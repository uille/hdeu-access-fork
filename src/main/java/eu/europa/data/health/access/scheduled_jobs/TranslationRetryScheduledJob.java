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

/**
 * Scheduled job that automatically retries pending form and file translations.
 * <p>
 * <b>Purpose:</b> When translation requests fail due to service unavailability, network issues,
 * or other transient errors, this scheduler discovers pending translations and retries them
 * automatically without manual intervention.
 * </p>
 *
 * <h2>How It Works</h2>
 *
 * <h3>1. Translation Object Creation</h3>
 * When a translation is initiated (see {@link eu.europa.data.health.access.services.CommonApplicationManager}),
 * the translation metadata is persisted to the database BEFORE making the actual translation request:
 * <pre>
 * {
 *   "translation": {
 *     "status": "FILES_PENDING" | "FORM_PENDING",
 *     "createdAt": "2026-01-27T13:00:00Z",        // Used to determine retry timing
 *     "form": {
 *       "status": "PENDING",
 *       "createdAt": "2026-01-27T13:00:00Z",
 *       "attempts": 0
 *     },
 *     "files": {
 *       "fileId": {
 *         "status": "PENDING",
 *         "createdAt": "2026-01-27T13:00:00Z",
 *         "attempts": 0
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <h3>2. Scheduler Discovery</h3>
 * This job runs periodically (default: every 5 minutes) and queries for applications with:
 * <ul>
 *   <li>translation.status = FILES_PENDING or FORM_PENDING</li>
 *   <li>translation.form.createdAt <= (now - retryInterval) OR</li>
 *   <li>translation.createdAt <= (now - retryInterval)</li>
 * </ul>
 *
 * <b>Why use createdAt instead of date_submitted?</b>
 * <ul>
 *   <li>date_submitted only exists for SUBMITTED applications, not DRAFT</li>
 *   <li>date_modified changes on ANY application update, not just translation state</li>
 *   <li>translation.createdAt is isolated from application lifecycle</li>
 * </ul>
 *
 * <h3>3. Retry Logic</h3>
 * For each pending translation:
 * <ol>
 *   <li>Check if attempts >= maxAttempts (default: 3)
 *     <ul>
 *       <li>YES: Mark translation as FAILED, create audit log, stop retrying</li>
 *       <li>NO: Continue to step 2</li>
 *     </ul>
 *   </li>
 *   <li>Increment attempts counter and set lastRetryAt timestamp</li>
 *   <li>On first retry (attempts=0), set firstAttemptAt timestamp</li>
 *   <li>Fetch file content (for files) or use stored payload (for forms)</li>
 *   <li>Make translation request to hub-translation service</li>
 *   <li>If request succeeds: translation proceeds normally (callback updates status)</li>
 *   <li>If request fails: translation stays PENDING for next retry cycle</li>
 * </ol>
 *
 * <h3>4. Timing Example</h3>
 * <pre>
 * T=0:00  Translation created, request fails (service down)
 *         DB: { status: "PENDING", attempts: 0, createdAt: "13:00:00" }
 *
 * T=5:00  Scheduler finds it (createdAt <= now-5min)
 *         Retry #1: attempts → 1, firstAttemptAt set
 *         Request fails again
 *
 * T=10:00 Scheduler finds it again
 *         Retry #2: attempts → 2, lastRetryAt updated
 *         Request fails again
 *
 * T=15:00 Scheduler finds it again
 *         Retry #3: attempts → 3, lastRetryAt updated
 *         Request fails again
 *
 * T=20:00 Scheduler finds it again
 *         Check: attempts (3) >= maxAttempts (3)? YES
 *         Mark as FAILED, create audit log
 * </pre>
 *
 * <h3>5. Configuration</h3>
 * <ul>
 *   <li><b>Retry Interval:</b> How often scheduler runs (default: 5 minutes = 300,000ms)</li>
 *   <li><b>Max Attempts:</b> Maximum retry attempts before marking FAILED (default: 3)</li>
 * </ul>
 * Both are configurable via application properties or environment variables.
 *
 * <h3>6. File vs Form Translations</h3>
 * <table border="1">
 *   <tr>
 *     <th>Aspect</th>
 *     <th>Files</th>
 *     <th>Forms</th>
 *   </tr>
 *   <tr>
 *     <td>Structure</td>
 *     <td>translation.files.{fileId}</td>
 *     <td>translation.form</td>
 *   </tr>
 *   <tr>
 *     <td>Multiple</td>
 *     <td>Yes (one per file)</td>
 *     <td>No (one per application)</td>
 *   </tr>
 *   <tr>
 *     <td>Retry Timing</td>
 *     <td>translation.createdAt</td>
 *     <td>translation.form.createdAt</td>
 *   </tr>
 *   <tr>
 *     <td>Attempts Tracking</td>
 *     <td>Per file</td>
 *     <td>Single counter</td>
 *   </tr>
 * </table>
 *
 * @see eu.europa.data.health.access.services.CommonApplicationManager
 * @see eu.europa.data.health.access.MainVerticle
 */
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

  /**
   * Queries MongoDB for applications with pending translations that are ready to retry.
   * <p>
   * <b>Query Logic:</b> Finds applications where the translation was initiated before the
   * retry interval threshold, ensuring we don't retry immediately after creation.
   * </p>
   *
   * <h3>Why Translation-Level Timestamps?</h3>
   * <p>
   * This method uses <code>translation.createdAt</code> and <code>translation.form.createdAt</code>
   * instead of application-level fields like <code>date_submitted</code> or <code>date_modified</code>
   * for several critical reasons:
   * </p>
   *
   * <h4>1. Draft Applications Support</h4>
   * <ul>
   *   <li><code>date_submitted</code> only exists for SUBMITTED applications</li>
   *   <li>Users can translate forms/files while application is still in DRAFT status</li>
   *   <li>Without translation-level timestamp, DRAFT translations would never be found</li>
   * </ul>
   *
   * <h4>2. Isolation from Application Updates</h4>
   * <ul>
   *   <li><code>date_modified</code> changes whenever ANY application field is updated</li>
   *   <li>Example: User updates application title → date_modified changes → affects translation timing</li>
   *   <li>Translation retry timing should be independent of other application modifications</li>
   * </ul>
   *
   * <h4>3. Independent Timing Per Translation Type</h4>
   * <ul>
   *   <li>Form translations: Use <code>translation.form.createdAt</code></li>
   *   <li>File translations: Use <code>translation.createdAt</code></li>
   *   <li>Each can be retried independently with its own timing</li>
   * </ul>
   *
   * <h3>Query Structure:</h3>
   * <pre>
   * {
   *   "translation.status": { "$in": ["FILES_PENDING", "FORM_PENDING"] },
   *   "$or": [
   *     {
   *       // Form translation ready to retry
   *       "translation.form.status": "PENDING",
   *       "translation.form.createdAt": { "$lte": threshold }
   *     },
   *     {
   *       // File translation ready to retry
   *       "translation.files": { "$exists": true },
   *       "translation.createdAt": { "$lte": threshold }
   *     }
   *   ]
   * }
   * </pre>
   *
   * <h3>Threshold Calculation:</h3>
   * <code>threshold = now() - retryInterval</code>
   * <p>
   * For example, with retryInterval = 5 minutes:
   * <ul>
   *   <li>If translation was created at 13:00, threshold at 13:05 is 13:00</li>
   *   <li>Query finds translations where createdAt <= 13:00</li>
   *   <li>This ensures at least 5 minutes have passed before first retry</li>
   * </ul>
   *
   * @param applicationType The MongoDB collection name ("data_access_applications" or "data_request_applications")
   * @param retryInterval   The time in milliseconds to wait before retrying (e.g., 300000 = 5 minutes)
   * @return Future containing list of applications with pending translations ready to retry
   */
  private Future<List<JsonObject>> findPendingTranslationApplications(String applicationType, int retryInterval) {
    Instant threshold = Instant.now().minusMillis(retryInterval);

    // Find applications that have either FILES_PENDING or FORM_PENDING status
    // and were initiated before the threshold time.
    // Uses translation-level createdAt timestamp instead of application-level date_submitted/date_modified
    // to ensure timing is independent of other application updates.
    JsonObject query = new JsonObject()
            .put("translation.status", new JsonObject().put("$in", new JsonArray()
                .add(FILES_PENDING)
                .add(FORM_PENDING)))
            .put("$or", new JsonArray()
                // Form translation: check translation.form.createdAt
                .add(new JsonObject()
                    .put("translation.form.status", "PENDING")
                    .put("translation.form.createdAt", new JsonObject().put("$lte", threshold.toString())))
                // File translation: check translation.createdAt
                .add(new JsonObject()
                    .put("translation.files", new JsonObject().put("$exists", true))
                    .put("translation.createdAt", new JsonObject().put("$lte", threshold.toString()))));

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

    // Check if we've exceeded maximum retry attempts
    // If so, mark translation as FAILED and create audit log
    // This prevents infinite retry loops for permanently failing translations
    if (attemptCount >= maxAttempts) {
      log.warn("Skipping form translation retry for {} — exceeded max attempts ({}).", appId, maxAttempts);
      // If the form translation fails, mark the whole translation as failed and add audit log
      markTranslationError(applicationType, appId, "Form translation max retries exceeded", null);
      return;
    }

    // Make translation request to external service
    // If successful: Callback will update status to COMPLETED when translation finishes
    // If failed: Translation stays PENDING for next retry cycle
    hubTranslationClient.requestTranslation(payload)
      .onSuccess(res -> {
        log.info("Successfully retried form translation for {}", appId);
        // Clear any previous error flags on successful retry attempt
        JsonObject successUpdate = new JsonObject()
          .put("$unset", new JsonObject().put("translation.form.error", ""));
        mongoClient.updateCollection(applicationType, new JsonObject().put("_id", appId), successUpdate);
      })
      .onFailure(err -> log.error("Failed to retry form translation for {}: {}", appId, err.getMessage()));

    // Track retry attempts and timestamps
    // This happens regardless of request success/failure to accurately track retry count
    JsonObject setObj = new JsonObject()
      .put("translation.form.lastRetryAt", Instant.now().toString());

    // Set firstAttemptAt only on first attempt (when attempts will become 1)
    // This helps distinguish between initial creation time and first actual retry
    // Useful for debugging and understanding translation lifecycle
    if (attemptCount == 0) {
      setObj.put("translation.form.firstAttemptAt", Instant.now().toString());
    }

    // Increment attempts counter
    // This is checked on next scheduler run to determine if maxAttempts is exceeded
    JsonObject update = new JsonObject()
      .put("$inc", new JsonObject().put("translation.form.attempts", 1))
      .put("$set", setObj);
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

    // Iterate through all files in the translation object
    // Each file has independent retry tracking (status, attempts, timestamps)
    for (String fileId : filesMap.fieldNames()) {
      JsonObject fileMeta = filesMap.getJsonObject(fileId);
      // Skip files that don't exist or aren't in PENDING status
      // (e.g., already COMPLETED or FAILED files)
      if (fileMeta == null || !"PENDING".equals(fileMeta.getString("status"))) continue;

      int attempts = fileMeta.getInteger("attempts", 0);

      // Check if this specific file has exceeded maximum retry attempts
      // Each file in a multi-file translation is tracked independently
      if (attempts >= maxAttempts) {
        log.warn("Skipping file translation retry for fileId={}, appId={} — exceeded max attempts ({}).", fileId, appId, maxAttempts);

        // If any file translation fails permanently, mark the whole translation as failed
        // This ensures users are notified when even one file cannot be translated
        markTranslationError(applicationType, appId, "File translation max retries exceeded", fileId);
        continue;
      }

      // Track retry attempts and timestamps for this specific file
      // This happens BEFORE making the request to ensure accurate tracking
      JsonObject setObj = new JsonObject()
        .put(TRANSLATION_FILES + fileId + ".lastRetryAt", Instant.now().toString());

      // Set firstAttemptAt only on first attempt (when attempts will become 1)
      // This tracks when we first attempted to retry (different from createdAt)
      if (attempts == 0) {
        setObj.put(TRANSLATION_FILES + fileId + ".firstAttemptAt", Instant.now().toString());
      }

      // Increment the attempts counter for this file
      // On next scheduler run, this will be checked against maxAttempts
      JsonObject preRetryUpdate = new JsonObject()
        .put("$inc", new JsonObject().put(TRANSLATION_FILES + fileId + ".attempts", 1))
        .put("$set", setObj);

      log.info("Incrementing attempts for fileId={}, appId={}, currentAttempts={}, update={}",
        fileId, appId, attempts, preRetryUpdate.encode());

      mongoClient.updateCollection(applicationType, new JsonObject().put("_id", appId), preRetryUpdate)
        .onSuccess(result -> {
          log.info("Successfully incremented attempts for fileId={}, appId={}, updatedCount={}",
            fileId, appId, result.getDocModified());
        })
        .onFailure(err -> log.error("Failed to increment attempts for fileId={}, appId={}", fileId, appId, err));

      // Fetch file content as Base64 for translation
      // Why fetch on every retry instead of storing?
      //   - Files may be large (storing Base64 in translation object would bloat database)
      //   - File might have been updated since initial request
      //   - Hub-store is the source of truth for file content
      hubStoreClient.getFilesAsBase64(List.of(fileId), appId)
        .compose(files -> {
          if (files.isEmpty()) throw new RuntimeException("File not found for base64 fetch: " + fileId);
          JsonObject fileObj = files.get(0);

          // Extract target languages from translation object
          // These were set when translation was initially created
          JsonObject translation = app.getJsonObject(TRANSLATION, new JsonObject());
          List<String> targetLangs = translation.getJsonArray("targetLanguages", new JsonArray())
            .stream().map(Object::toString).toList();

          // Build translation payload with current file content and target languages
          JsonObject payload = hubTranslationClient.buildFileTranslationPayload(appId, applicationType, fileObj, targetLangs);
          return hubTranslationClient.requestFileTranslation(payload);
        })
        .onSuccess(v -> log.info("Retried file translation for fileId={}, appId={}", fileId, appId))
        .onFailure(err -> {
          log.error("Failed file translation retry for fileId={}, appId={}, err={}", fileId, appId, err.getMessage(), err);

          // Special handling for missing file content
          // If file was deleted or moved, mark this specific file as FAILED
          // instead of continuing to retry indefinitely
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
