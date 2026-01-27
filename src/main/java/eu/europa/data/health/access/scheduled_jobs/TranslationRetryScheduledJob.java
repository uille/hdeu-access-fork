/*
 * Copyright (C) 2024 European Union
 *  __
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the “Licence”);
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an “AS IS” basis, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */
package eu.europa.data.health.access.scheduled_jobs;

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.TRANSLATION;
import static eu.europa.data.health.access.TranslationStatus.FAILED;
import static eu.europa.data.health.access.TranslationStatus.FILES_PENDING;
import static eu.europa.data.health.access.TranslationStatus.FORM_PENDING;

import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import eu.europa.data.health.access.services.DataRequestApplicationManager;
import eu.europa.data.health.access.services.HubStoreClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationRetryScheduledJob {

  public static final String TRANSLATION_FILES = "translation.files.";

  private final MongoClient mongoClient;
  private final HubTranslationClient hubTranslationClient;
  private final HubStoreClient hubStoreClient;
  private final DataAccessApplicationManager daaManager;
  private final DataRequestApplicationManager draManager;
  private static final Logger log = LoggerFactory.getLogger(TranslationRetryScheduledJob.class);

  public TranslationRetryScheduledJob(
    MongoClient mongoClient,
    HubTranslationClient hubTranslationClient,
    HubStoreClient hubStoreClient,
    DataAccessApplicationManager daaManager,
    DataRequestApplicationManager draManager) {
    this.mongoClient = mongoClient;
    this.hubTranslationClient = hubTranslationClient;
    this.hubStoreClient = hubStoreClient;
    this.daaManager = daaManager;
    this.draManager = draManager;
  }

  public void retryPendingTranslations(String applicationType, int maxAttempts, int retryInterval) {
    findPendingTranslationApplications(applicationType, retryInterval).onSuccess(applications -> {
      for (JsonObject app : applications) {
        retryTranslationIfNeeded(app, applicationType, maxAttempts);
      }
    }).onFailure(err -> log.error("Failed to fetch applications for retry", err));
  }

  private Future<List<JsonObject>> findPendingTranslationApplications(String applicationType, int retryInterval) {
    Instant threshold = Instant.now().minusMillis(retryInterval);

    // Find applications that have either FILES_PENDING or FORM_PENDING status
    // and were submitted before the threshold time
    JsonObject query = new JsonObject()
            .put("translation.status", new JsonObject().put("$in", new JsonArray()
                .add(FILES_PENDING)
                .add(FORM_PENDING)))
            .put("date_submitted", new JsonObject().put("$lte", threshold.toString()));

    FindOptions options = new FindOptions();

    Promise<List<JsonObject>> promise = Promise.promise();
    mongoClient.findWithOptions(applicationType, query, options)
      .onSuccess(promise::complete)
      .onFailure(promise::fail);
    return promise.future();
  }

  private void retryTranslationIfNeeded(JsonObject app, String applicationType, int maxAttempts) {
    String appId = app.getString("_id");
    JsonObject translation = app.getJsonObject(TRANSLATION);

    // Form translation retry
    JsonObject form = translation != null ? translation.getJsonObject("form") : null;
    JsonObject payload = form != null ? form.getJsonObject("payload") : null;

    if (payload != null && "PENDING".equals(form.getString("status"))) {
      retryFormTranslation(appId, applicationType, form, payload, maxAttempts);
    }

    // File translation retries
    JsonObject files = translation != null ? translation.getJsonObject("files") : null;
    if (files != null && !files.isEmpty()) {
      retryPendingFileTranslations(app, applicationType, maxAttempts);
    }
  }

  /**
   * Attempts to retry the form translation for a given application.
   * <p>
   * - Checks if the retry attempts have exceeded the maximum allowed.
   *   - If exceeded, marks the form and overall translation status as FAILED.
   * - If not exceeded, sends a translation request again.
   * - On success, clears any previous form translation error.
   * - Always increments the retry attempt count and records the retry timestamp.
   *
   * @param appId the ID of the application
   * @param applicationType the collection/type of the application
   * @param form the form JSON containing translation metadata
   * @param payload the translation payload to resend
   * @param maxAttempts maximum allowed retry attempts
   */
  private void retryFormTranslation(String appId, String applicationType, JsonObject form, JsonObject payload, int maxAttempts) {
    Integer attemptCount = form.getInteger("attempts", 0);
    if (attemptCount >= maxAttempts) {
      log.warn("Skipping form translation retry for {} — exceeded max attempts ({}).", appId, maxAttempts);
      // If the form translation fails, mark the whole translation as failed and add audit log
      markTranslationError(applicationType, appId, "Form translation max retries exceeded", null);
      return;
    }

    hubTranslationClient.requestTranslation(payload)
      .onSuccess(res -> {
        log.info("Successfully retried form translation for {}", appId);
        JsonObject successUpdate = new JsonObject()
          .put("$unset", new JsonObject().put("translation.form.error", ""));
        mongoClient.updateCollection(applicationType, new JsonObject().put("_id", appId), successUpdate);
      })
      .onFailure(err -> log.error("Failed to retry form translation for {}: {}", appId, err.getMessage()));

    // Increment the retry attempts
    JsonObject update = new JsonObject()
      .put("$inc", new JsonObject().put("translation.form.attempts", 1))
      .put("$set", new JsonObject().put("translation.form.lastRetryAt", Instant.now().toString()));
    mongoClient.updateCollection(applicationType, new JsonObject().put("_id", appId), update)
      .onFailure(err -> log.error("Failed to increment form translation attempts for {}", appId, err));
  }

  /**
   * Retries translation for all files in an application that have a translation status of PENDING.
   * <p>
   * For each pending file translation:
   * - Checks if the retry attempts have exceeded the maximum allowed.
   * - If exceeded, marks that file translation and the overall translation status as FAILED.
   * - Otherwise, increments the retry attempt count and sets the retry timestamp.
   * - Fetches the file content as base64 and requests a file translation through the translation service.
   * - Logs success or failure of the retry request.
   * - On failure due to missing base64 content, marks the file translation as FAILED.
   *
   * @param app             the full application JSON document
   * @param applicationType the collection/type name of the application
   * @param maxAttempts     maximum retry attempts allowed per file
   */
  private void retryPendingFileTranslations(JsonObject app, String applicationType, int maxAttempts) {
    String appId = app.getString("_id");
    JsonObject filesMap = app.getJsonObject(TRANSLATION, new JsonObject())
      .getJsonObject("files", new JsonObject());

    for (String fileId : filesMap.fieldNames()) {
      JsonObject fileMeta = filesMap.getJsonObject(fileId);
      if (fileMeta == null || !"PENDING".equals(fileMeta.getString("status"))) continue;

      int attempts = fileMeta.getInteger("attempts", 0);
      if (attempts >= maxAttempts) {
        log.warn("Skipping file translation retry for fileId={}, appId={} — exceeded max attempts ({}).", fileId, appId, maxAttempts);

        // If any file translation fails, mark the whole translation as failed and add audit log
        markTranslationError(applicationType, appId, "File translation max retries exceeded", fileId);
        continue;
      }

      // Increment the retry attempts
      JsonObject preRetryUpdate = new JsonObject()
        .put("$inc", new JsonObject().put(TRANSLATION_FILES + fileId + ".attempts", 1))
        .put("$set", new JsonObject().put(TRANSLATION_FILES + fileId + ".lastRetryAt", Instant.now().toString()));

      log.info("Incrementing attempts for fileId={}, appId={}, currentAttempts={}, update={}",
        fileId, appId, attempts, preRetryUpdate.encode());

      mongoClient.updateCollection(applicationType, new JsonObject().put("_id", appId), preRetryUpdate)
        .onSuccess(result -> {
          log.info("Successfully incremented attempts for fileId={}, appId={}, updatedCount={}",
            fileId, appId, result.getDocModified());
        })
        .onFailure(err -> log.error("Failed to increment attempts for fileId={}, appId={}", fileId, appId, err));

      hubStoreClient.getFilesAsBase64(List.of(fileId), appId)
        .compose(files -> {
          if (files.isEmpty()) throw new RuntimeException("File not found for base64 fetch: " + fileId);
          JsonObject fileObj = files.get(0);

          JsonObject translation = app.getJsonObject(TRANSLATION, new JsonObject());
          List<String> targetLangs = translation.getJsonArray("targetLanguages", new JsonArray())
            .stream().map(Object::toString).toList();

          JsonObject payload = hubTranslationClient.buildFileTranslationPayload(appId, applicationType, fileObj, targetLangs);
          return hubTranslationClient.requestFileTranslation(payload);
        })
        .onSuccess(v -> log.info("Retried file translation for fileId={}, appId={}", fileId, appId))
        .onFailure(err -> {
          log.error("Failed file translation retry for fileId={}, appId={}, err={}", fileId, appId, err.getMessage(), err);

          if (err.getMessage().contains("base64")) {
            JsonObject failUpdate = new JsonObject()
              .put("$set", new JsonObject().put(TRANSLATION_FILES + fileId + ".status", FAILED));
            mongoClient.updateCollection(applicationType, new JsonObject().put("_id", appId), failUpdate)
              .onFailure(e -> log.error("Failed to mark file as FAILED for fileId={}, appId={}, err={}", fileId, appId, e.getMessage()));
          }
        });
    }
  }

  private void markTranslationError(
    String applicationType,
    String appId,
    String errorType,
    String fileId
  ) {
    if (DATA_ACCESS_APPLICATION_COLLECTION.equals(applicationType)) {
      daaManager.markTranslationError(appId, errorType, fileId);
    } else if (DATA_REQUEST_APPLICATION_COLLECTION.equals(applicationType)) {
      draManager.markTranslationError(appId, errorType, fileId);
    } else {
      throw new IllegalArgumentException("Unknown application type: " + applicationType);
    }
  }

}
