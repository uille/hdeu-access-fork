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
package eu.europa.data.health.access.services;

import static eu.europa.data.health.access.Constants.APPLICATIONID;
import static eu.europa.data.health.access.Constants.CONFIGURATION_COLLECTION;
import static eu.europa.data.health.access.Constants.CONTROLLED_VOCABULARY_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.TRANSLATIONS_STATE_COLLECTION;
import static eu.europa.data.health.access.Constants.UNKNOWN_APPLICATION_TYPE;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationCallbackManager {

  private static final Logger log = LoggerFactory.getLogger(TranslationCallbackManager.class);
  private static final String CALLBACK_RECEIVED = "Callback received";
  private static final String MISSING_REQUIRED_FIELDS = "Missing required fields";
  private static final int TIMEOUT_ERROR_CODE = 41003;
  private static final String TRANSLATIONS = "translations";

  private final DataRequestApplicationManager dataRequestManager;
  private final DataAccessApplicationManager dataAccessManager;
  private final ControlledVocabularyManager controlledVocabularyManager;
  private final PayloadManager payloadManager;
  private final HubStoreClient hubStoreClient;
  private final DecisionTranslationService decisionTranslationService;

  public TranslationCallbackManager(DataRequestApplicationManager dataRequestManager,
    DataAccessApplicationManager dataAccessManager,
    ControlledVocabularyManager controlledVocabularyManager,
    PayloadManager payloadManager,
    HubStoreClient hubStoreClient,
    DecisionTranslationService decisionTranslationService) {
    this.dataRequestManager = dataRequestManager;
    this.dataAccessManager = dataAccessManager;
    this.controlledVocabularyManager = controlledVocabularyManager;
    this.payloadManager = payloadManager;
    this.hubStoreClient = hubStoreClient;
    this.decisionTranslationService = decisionTranslationService;
  }

  /**
   * Handles the translation callback received via HTTP POST.
   * <p>
   * This method processes both:
   * <ul>
   *   <li><b>File translation callbacks</b>: When the payload contains a translated file section,
   *       it uploads the translated files, updates MongoDB metadata, and rewrites references in form translations.</li>
   *   <li><b>Form translation callbacks</b>: When the payload contains translated form content,
   *       it delegates processing to the appropriate application manager.</li>
   * </ul>
   * <p>
   * Validates required fields in the JSON payload:
   * - {@code id} (application ID)
   * - {@code data} object containing {@code applicationType} and {@code targetLanguages}
   * - {@code dict} with the translation data
   *
   * <p>Based on the {@code applicationType}, delegates processing to the appropriate manager.
   *
   * @param context The Vert.x {@link RoutingContext} representing the HTTP request/response context.
   *                Responds with:
   *                <ul>
   *                  <li><code>200 OK</code> if callback is processed successfully</li>
   *                  <li><code>400 Bad Request</code> if required fields are missing or invalid</li>
   *                  <li><code>500 Internal Server Error</code> if an error occurs during processing</li>
   *                </ul>
   */
  public void handleTranslationCallback(RoutingContext context) {
    log.debug("Handling translation callback");

    JsonObject body = context.body().asJsonObject();
    JsonObject data = body.getJsonObject("data");
    JsonObject dict = body.getJsonObject("dict");

    if (data == null || dict == null) {
      log.warn("Missing required fields in callback");
      context.response().setStatusCode(400).end(MISSING_REQUIRED_FIELDS);
      return;
    }

    String targetId = data.getString("id", data.getString("applicationId"));
    String targetType = data.getString("type", data.getString("applicationType"));

    if (targetId == null || targetType == null) {
      log.warn("Missing required fields in callback");
      context.response().setStatusCode(400).end(MISSING_REQUIRED_FIELDS);
      return;
    }

    boolean isTranslationStateCallback = TRANSLATIONS_STATE_COLLECTION.equals(targetType);

    // Check for errors in payload
    Set<Integer> errorCodes = getAllTranslationErrorCodes(dict);
    boolean hasTimeoutError = errorCodes.remove(TIMEOUT_ERROR_CODE);
    if (!errorCodes.isEmpty() && !isTranslationStateCallback) {
      log.warn("Detected translation errors {} in callback for application {}", errorCodes,
        targetId);

      String errorMessage = "Translation error code(s): " + errorCodes;

      // Determine if this is a file translation callback
      JsonObject fileSection = dict.getJsonObject("file");
      String fileId = null;
      if (fileSection != null && !fileSection.isEmpty()) {
        fileId = fileSection.fieldNames().iterator().next();
      }

      switch (targetType) {
        case DATA_REQUEST_APPLICATION_COLLECTION ->
          dataRequestManager.incrementTranslationAttempts(targetId, errorMessage, fileId);
        case DATA_ACCESS_APPLICATION_COLLECTION ->
          dataAccessManager.incrementTranslationAttempts(targetId, errorMessage, fileId);
        case CONTROLLED_VOCABULARY_COLLECTION ->
          controlledVocabularyManager.processTranslationCallback(targetId, dict);
        case CONFIGURATION_COLLECTION -> {
          String documentName = data.getString("documentName");
          if (documentName == null) {
            log.warn("Missing documentName in configuration translation error callback");
            context.response().setStatusCode(400).end(MISSING_REQUIRED_FIELDS);
            return;
          }
          payloadManager.processTranslationCallback(documentName, dict);
        }
        default -> {
          log.warn("Unknown application type while handling translation error: {}", targetType);
          context.response().setStatusCode(400).end(UNKNOWN_APPLICATION_TYPE);
          return;
        }
      }
      context.response().setStatusCode(200).end("Translation error handled");
      return;
    } else if (
      hasTimeoutError &&
        (targetType.equals(DATA_REQUEST_APPLICATION_COLLECTION) ||
          targetType.equals(DATA_ACCESS_APPLICATION_COLLECTION))
    ) {
      // Ignore timeout errors for applications in order to retry translating them later
      log.warn("Detected translation timeout in callback for application {}", targetId);
      context.response().setStatusCode(200).end("Translation timeout handled");
      return;
    }

    if (isTranslationStateCallback) {
      decisionTranslationService.processTranslationCallback(targetId, dict);
      context.response().setStatusCode(200).end(CALLBACK_RECEIVED);
      return;
    }

    JsonObject fileSection = dict.getJsonObject("file");
    boolean isFileTranslation = fileSection != null && !fileSection.isEmpty();

    if (isFileTranslation) {
      // Extract the single fileId from the file section (first and only key)
      String fileId = fileSection.fieldNames().iterator().next();

      handleFileTranslationCallback(targetId, fileId, dict)
              .onSuccess(translatedFiles -> {
                log.debug("Translation callback successful for file {}", fileId);

                // Now update MongoDB metadata with uploaded file references
                switch (targetType) {
                  case DATA_REQUEST_APPLICATION_COLLECTION -> {
                    dataRequestManager.processFileTranslationCallback(targetId, fileId, translatedFiles);
                    context.response().setStatusCode(200).end("File callback processed");
                  }
                  case DATA_ACCESS_APPLICATION_COLLECTION -> {
                    dataAccessManager.processFileTranslationCallback(targetId, fileId, translatedFiles);
                    context.response().setStatusCode(200).end("File callback processed");
                  }
                  default -> {
                    log.warn("Unknown application type while handling file translation callback: {}", targetType);
                    context.response().setStatusCode(400).end(UNKNOWN_APPLICATION_TYPE);
                  }
                }
              })
              .onFailure(err -> {
                log.error("Error processing file callback: {}", err.getMessage());
                context.response().setStatusCode(500).end("Error processing file callback");
              });

      return;
    }

    switch (targetType) {
      case DATA_REQUEST_APPLICATION_COLLECTION -> {
        dataRequestManager.processTranslationCallback(targetId, dict);
        context.response().setStatusCode(200).end(CALLBACK_RECEIVED);
      }
      case DATA_ACCESS_APPLICATION_COLLECTION -> {
        dataAccessManager.processTranslationCallback(targetId, dict);
        context.response().setStatusCode(200).end(CALLBACK_RECEIVED);
      }
      case CONTROLLED_VOCABULARY_COLLECTION -> {
        controlledVocabularyManager.processTranslationCallback(targetId, dict);
        context.response().setStatusCode(200).end(CALLBACK_RECEIVED);
      }
      case CONFIGURATION_COLLECTION -> {
        String documentName = data.getString("documentName");
        if (documentName == null) {
          log.warn("Missing documentName in configuration translation callback");
          context.response().setStatusCode(400).end(MISSING_REQUIRED_FIELDS);
          return;
        }
        payloadManager.processTranslationCallback(documentName, dict);
        context.response().setStatusCode(200).end(CALLBACK_RECEIVED);
      }
      default -> {
        log.warn("Unknown application type while handling form translation callback: {}", targetType);
        context.response().setStatusCode(400).end(UNKNOWN_APPLICATION_TYPE);
      }
    }
  }

  /**
   * Handles the processing of a file translation callback by decoding, uploading translated files,
   * and returning a mapping of language codes to the uploaded file metadata.
   * <p>
   * This method:
   * <ul>
   *   <li>Validates the structure of the callback payload (<code>dict</code>), ensuring it includes
   *       base64-encoded file content for each target language.</li>
   *   <li>Decodes each translated file from base64 and uploads it to the file storage service
   *       (<code>hubStoreClient</code>).</li>
   *   <li>Collects the resulting file IDs and names in a map keyed by language.</li>
   * </ul>
   *
   * @param applicationId   The ID of the application to associate the uploaded files with.
   * @param originalFileId  The ID of the original file that was translated.
   * @param dict            The callback dictionary payload containing translations under
   *                        <code>dict.file.{fileId}.translations</code>.
   *
   * @return A future containing a map where each key is a language code and each value is a JsonObject
   *         with <code>fileId</code> and <code>fileName</code> of the uploaded translated file.
   *         If any upload fails or required fields are missing, the future will fail.
   */
  private Future<Map<String, JsonObject>> handleFileTranslationCallback(
          String applicationId,
          String originalFileId,
          JsonObject dict
  ) {
    Promise<Map<String, JsonObject>> promise = Promise.promise();

    JsonObject fileSection = dict.getJsonObject("file");
    if (fileSection == null || !fileSection.containsKey(originalFileId)) {
      return Future.failedFuture("Missing file section for fileId: " + originalFileId);
    }

    JsonObject file = fileSection.getJsonObject(originalFileId);
    if (file == null || !file.containsKey(TRANSLATIONS)) {
      return Future.failedFuture("Missing file translations for fileId: " + originalFileId);
    }

    JsonObject translations = file.getJsonObject(TRANSLATIONS);
    JsonObject docMeta = file.getJsonObject("documentToTranslateBase64");

    if (docMeta == null) {
      return Future.failedFuture("Missing document metadata");
    }

    String format = docMeta.getString("format");
    String originalFileName = docMeta.getString("fileName");

    Map<String, JsonObject> uploadedFilesByLang = new HashMap<>();
    List<Future> uploadFutures = new ArrayList<>();

    for (String lang : translations.fieldNames()) {
      String base64 = translations.getString(lang);
      String newFileName = buildTranslatedFileName(originalFileName, lang);

      Buffer fileBuffer = Buffer.buffer(Base64.getDecoder().decode(base64));

      Future<JsonObject> uploadFuture = hubStoreClient.uploadFile(
              APPLICATIONID,
              applicationId,
              newFileName,
              fileBuffer,
              format
      ).onSuccess(uploadResult -> {
        JsonObject result = uploadResult.getJsonObject("result");
        String newFileId = result.getString("id");
        log.info("Uploaded translated file [{}] -> new ID: {}", lang, newFileId);

        uploadedFilesByLang.put(lang, new JsonObject()
                .put("fileId", newFileId)
                .put("fileName", newFileName));
      });

      uploadFutures.add(uploadFuture);
    }

    CompositeFuture.all(uploadFutures).onComplete(ar -> {
      if (ar.succeeded()) {
        log.info("All translated files uploaded for fileId={}", originalFileId);
        promise.complete(uploadedFilesByLang);
      } else {
        promise.fail("Failed to upload one or more translated files");
      }
    });

    return promise.future();
  }

  private String buildTranslatedFileName(String original, String lang) {
    if (original == null || lang == null) {
      throw new IllegalArgumentException("Original filename and language must not be null");
    }

    int dotIdx = original.lastIndexOf('.');
    if (dotIdx == -1) {
      return original + "_" + lang;
    }
    return original.substring(0, dotIdx) + "_" + lang + original.substring(dotIdx);
  }

  private Set<Integer> getAllTranslationErrorCodes(JsonObject dict) {
    Set<Integer> errorCodes = new HashSet<>();
    for (String sectionKey : dict.fieldNames()) {
      JsonObject section = dict.getJsonObject(sectionKey, new JsonObject());
      for (String fieldKey : section.fieldNames()) {
        JsonObject field = section.getJsonObject(fieldKey, new JsonObject());
        JsonObject translations = field.getJsonObject(TRANSLATIONS, new JsonObject());
        for (String lang : translations.fieldNames()) {
          Object value = translations.getValue(lang);
          if (value instanceof Number number) {
            errorCodes.add(number.intValue()); // collect error code
          }
        }
      }
    }
    return errorCodes;
  }
}
