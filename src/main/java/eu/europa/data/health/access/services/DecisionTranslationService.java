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
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.INPUT_LANGUAGE;
import static eu.europa.data.health.access.Constants.OBJECT_ID;
import static eu.europa.data.health.access.Constants.OPERATION;
import static eu.europa.data.health.access.Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
import static eu.europa.data.health.access.Constants.PENDING;
import static eu.europa.data.health.access.Constants.SUPPORTED_LANGUAGES;
import static eu.europa.data.health.access.Constants.TARGET_LANGUAGES;
import static eu.europa.data.health.access.Constants.TRANSLATION;
import static eu.europa.data.health.access.Constants.TRANSLATIONS_STATE_COLLECTION;

import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.enums.TranslationStatus;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClientActivities;
import eu.europa.data.health.access.utils.TranslationUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for translating fields in decisions and permits into all
 * {@link eu.europa.data.health.access.Constants#SUPPORTED_LANGUAGES}.
 *
 * <p>File fields ({@code decision_file}, {@code support_file}, {@code permit_file}) are sent
 * via the <em>file-translation-service</em> endpoint using the {@code documentToTranslateBase64}
 * format.
 *
 * <p>Text fields ({@code decision_reason}, {@code permit_contact_details}) are sent
 * via the <em>translation-service</em> endpoint using the {@code value} format.
 *
 * <p>Each MongoDB translation record stored in
 * {@value eu.europa.data.health.access.Constants#TRANSLATIONS_STATE_COLLECTION} holds:
 * <ul>
 *   <li>{@code applicationPortalId} – portal {@code _id} of the application</li>
 *   <li>{@code datasetId} – dataset this decision/permit belongs to</li>
 *   <li>{@code itemIndex} – position in the dataset's {@code decisions}/{@code permits} array</li>
 *   <li>{@code itemType} – {@code "decision"} or {@code "permit"}</li>
 *   <li>{@code fileFields} – array of file-field descriptors ({@code field, fileContent, format, fileName})</li>
 *   <li>{@code textFields} – array of text-field descriptors ({@code field, value})</li>
 *   <li>{@code translations} – nested object {@code {lang -> {field -> translatedValue}}}</li>
 *   <li>{@code translationStatus} – PENDING, WAITING, COMPLETED or FAILED</li>
 *   <li>{@code fileRetries} – number of times file translation has been attempted</li>
 *   <li>{@code textRetries} – number of times text translation has been attempted</li>
 *   <li>{@code syncPending} – whether the record is pending sync back to the application document</li>
 * </ul>
 */
public record DecisionTranslationService(
  MongoClient mongoClient,
  HubTranslationClientActivities hubTranslationClient,
  DispatcherClient dispatcherClient,
  HubStoreClient hubStoreClient,
  boolean enableTranslation
) {

  // Field names used in decisions
  public static final String DECISION_FILE = "decision_file";
  public static final String SUPPORT_FILE = "support_file";
  public static final String DECISION_REASON = "decision_reason";

  // Field names used in permits
  public static final String PERMIT_FILE = "permit_file";
  public static final String PERMIT_CONTACT_DETAILS = "permit_contact_details";

  // Item type identifiers
  public static final String TYPE_DECISION = "decision";
  public static final String TYPE_PERMIT = "permit";
  public static final String TYPE_STATUS = "status";

  // Document field keys (MongoDB record structure)
  private static final String APPLICATION_PORTAL_ID = "applicationPortalId";
  private static final String APPLICATION_ID = "applicationId";
  private static final String DATASET_ID = "dataset_id";
  private static final String ITEM_INDEX = "itemIndex";
  private static final String ITEM_TYPE = "itemType";
  private static final String FILE_FIELDS = "fileFields";
  private static final String TEXT_FIELDS = "textFields";
  private static final String FIELD = "field";
  private static final String FILE_HUB_REPO_ID = "file_hub_repo_id";
  private static final String FORMAT = "format";
  private static final String FILE_NAME = "fileName";
  private static final String VALUE = "value";
  private static final String TRANSLATIONS = "translations";
  private static final String TRANSLATION_STATUS = "translationStatus";
  private static final String FILE_RETRIES = "fileRetries";
  private static final String TEXT_RETRIES = "textRetries";
  private static final int MAX_RETRIES = 5;
  private static final String SYNC_PENDING = "syncPending";
  private static final String DATASETS = "datasets";
  private static final String DECISIONS = "decisions";
  private static final String PERMITS = "permits";
  private static final String PAYLOAD = "payload";
  private static final String TRANSLATED_FIELDS = "translatedFields";
  private static final String CATALOG_ID = "catalogId";
  private static final String TRANSLATION_ERRORS = "translationErrors";
  private static final String LANGUAGE = "language";
  private static final String ERROR_CODE = "errorCode";
  private static final String CALLBACK_TYPE = "callbackType";
  private static final String RECORDED_AT = "recordedAt";
  private static final String TRANSLATION_STATE_ID = "translationStateId";
  private static final String DISPATCHER_STATUS = "dispatcherStatus";
  private static final String CATALOG_LANGUAGES = "catalogLanguages";
  private static final String SOURCE_LANGUAGE = "sourceLanguage";
  private static final String TARGET_LANGUAGE = "targetLanguage";
  private static final String ADDITIONAL_INFORMATION = "additionalInformation";
  private static final Logger log = LoggerFactory.getLogger(DecisionTranslationService.class);

  // ─────────────────────────────────────────────────────────────────────────────
  // Public API
  // ─────────────────────────────────────────────────────────────────────────────


  /**
   * Saves a translation record for a newly issued decision with catalog-specific languages. For
   * activities (DATA_REQUEST_APPLICATION_COLLECTION and DATA_ACCESS_APPLICATION_COLLECTION), this
   * method should be used to restrict translations to catalog_languages only.
   *
   * @param applicationPortalId The portal {@code _id} of the application.
   * @param applicationId       The national dispatcher application ID.
   * @param datasetId           The dataset identifier the decision belongs to.
   * @param itemIndex           Index of this decision inside the dataset's {@code decisions}
   *                            array.
   * @param decision            The decision JSON object (as stored in the database).
   * @param catalogId           The catalog ID this decision belongs to.
   * @return A Future containing the inserted document ID, or {@code null} if nothing to translate.
   */
  public Future<String> saveDecisionTranslation(
    @NotNull String collectionName,
    @NotNull String applicationPortalId,
    @NotNull String applicationId,
    @NotNull String datasetId,
    int itemIndex,
    @NotNull JsonObject decision,
    String catalogId,
    String inputLanguage
  ) {
    JsonArray fileFields = new JsonArray();
    JsonArray textFields = new JsonArray();
    addTextField(textFields, decision, DECISION_REASON);

    return addFileField(fileFields, decision, DECISION_FILE, applicationPortalId)
      .compose(v -> addFileField(fileFields, decision, SUPPORT_FILE, applicationPortalId))
      .compose(v -> {
        if (fileFields.isEmpty() && textFields.isEmpty()) {
          log.debug("No translatable fields in decision for dataset {} (app {})",
            datasetId, applicationPortalId);
          return Future.succeededFuture(null);
        }
        JsonObject doc = buildRecord(
          collectionName, applicationPortalId, applicationId, datasetId, itemIndex,
          TYPE_DECISION, fileFields, textFields, catalogId);

        if (!enableTranslation) {
          doc.put(TARGET_LANGUAGE, inputLanguage);
        }
        applyExistingStateId(doc, decision.getJsonObject(TRANSLATED_FIELDS));
        return insertRecord(doc);
      });
  }


  /**
   * Saves a translation record for a newly issued/updated permit with catalog-specific languages.
   * For activities (DATA_REQUEST_APPLICATION_COLLECTION and DATA_ACCESS_APPLICATION_COLLECTION),
   * this method should be used to restrict translations to catalog_languages only.
   *
   * @param applicationPortalId The portal {@code _id} of the application.
   * @param applicationId       The national dispatcher application ID.
   * @param datasetId           The dataset identifier the permit belongs to.
   * @param itemIndex           Index of this permit inside the dataset's {@code permits} array.
   * @param permit              The permit JSON object (as stored in the database).
   * @param catalogId           The catalog ID this permit belongs to.
   * @return A Future containing the inserted document ID, or {@code null} if nothing to translate.
   */
  public Future<String> savePermitTranslation(
    @NotNull String collectionName,
    @NotNull String applicationPortalId,
    @NotNull String applicationId,
    @NotNull String datasetId,
    int itemIndex,
    @NotNull JsonObject permit, String catalogId, String inputLanguage
  ) {
    JsonArray fileFields = new JsonArray();
    JsonArray textFields = new JsonArray();
    addTextField(textFields, permit, PERMIT_CONTACT_DETAILS);

    return addFileField(fileFields, permit, PERMIT_FILE, applicationPortalId)
      .compose(v -> {
        if (fileFields.isEmpty() && textFields.isEmpty()) {
          log.debug("No translatable fields in permit for dataset {} (app {})",
            datasetId, applicationPortalId);
          return Future.succeededFuture(null);
        }
        JsonObject doc = buildRecord(
          collectionName, applicationPortalId, applicationId, datasetId, itemIndex,
          TYPE_PERMIT, fileFields, textFields, catalogId);

        if (!enableTranslation) {
          doc.put(TARGET_LANGUAGE, inputLanguage);
        }

        applyExistingStateId(doc, permit.getJsonObject(TRANSLATED_FIELDS));
        return insertRecord(doc);
      });
  }

  public Future<String> saveStatusTranslation(
    @NotNull String collectionName,
    @NotNull String applicationPortalId,
    @NotNull String applicationId,
    @NotNull String catalogId,
    @Nullable JsonObject statusPayload,
    @NotNull String inputLanguage
  ) {
    JsonArray textFields = new JsonArray();
    addStatusTextFields(textFields, statusPayload);

    if (textFields.isEmpty()) {
      log.debug("No translatable fields in catalog status payload for catalog {} (app {})",
        catalogId, applicationPortalId);
      return Future.succeededFuture(null);
    }

    JsonObject doc = buildRecord(
      collectionName, applicationPortalId, applicationId, null, -1,
      TYPE_STATUS, new JsonArray(), textFields, catalogId
    )
      .put(CATALOG_ID, catalogId)
      .put(TARGET_LANGUAGE, inputLanguage);
    applyExistingStateId(doc,
      statusPayload == null ? null : statusPayload.getJsonObject(TRANSLATED_FIELDS));
    return insertRecord(doc);
  }

  public JsonObject createTranslatedFieldsReference(@Nullable String stateId) {
    if (StringUtils.isBlank(stateId)) {
      return null;
    }
    return new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, stateId);
  }

  /**
   * Fetches all PENDING decision-translation records and dispatches them to the translation
   * services. File fields go to the file-translation endpoint; text fields go to the text endpoint.
   * Records with nothing left to translate are immediately marked COMPLETED.
   *
   * @return A Future that completes when all pending records have been processed.
   */
  public Future<Void> triggerPendingTranslations() {
    JsonObject stateQuery = new JsonObject()
      .put(TRANSLATION_STATUS, TranslationStatus.PENDING.getStatus())
      .put(OPERATION, new JsonObject().put("$in", new JsonArray()
        .add(OperationStatus.DECISION.getLabel())
        .add(OperationStatus.PERMIT.getLabel())
        .add(OperationStatus.STATUS_UPDATE.getLabel())));

    return Future.all(List.of(
        findRecordsByQuery(DATA_ACCESS_APPLICATION_COLLECTION, stateQuery),
        findRecordsByQuery(DATA_REQUEST_APPLICATION_COLLECTION, stateQuery)))
      .map(cf -> {
        List<JsonObject> docs = new ArrayList<>();
        docs.addAll(cf.resultAt(0));
        docs.addAll(cf.resultAt(1));
        return docs;
      })
      .compose(this::processPendingTranslationDocs)
      .onFailure(err -> log.error("Failed to fetch pending decision translation records", err))
      .mapEmpty();
  }

  private Future<Void> processPendingTranslationDocs(List<JsonObject> docs) {
    if (docs.isEmpty()) {
      log.debug("No pending decision translation records found");
      return Future.succeededFuture();
    }

    log.info("Found {} pending decision translation record(s)", docs.size());
    List<Future<Void>> futures = new ArrayList<>();
    for (JsonObject doc : docs) {
      futures.add(processRecordForTranslation(doc));
    }
    return Future.all(new ArrayList<>(futures)).mapEmpty();
  }

  /**
   * Processes a translation callback (from either the file or text endpoint). Accumulates
   * translated values per language per field into the {@code translations} object, then advances
   * the record status.
   *
   * @param id   The {@code _id} of the decision-translation record.
   * @param dict The dictionary returned by the translation service.
   */
  public void processTranslationCallback(@NotNull String id, @NotNull JsonObject dict) {
    findStateRecord(id)
      .compose(doc -> {
        if (doc == null) {
          log.warn("Decision translation record {} not found for callback", id);
          return Future.failedFuture("Record not found: " + id);
        }

        JsonArray fileFields = extractFileFields(doc);
        JsonArray textFields = extractTextFields(doc);
        JsonObject section = extractCallbackSection(dict, fileFields, textFields);

        if (section.isEmpty()) {
          log.warn("No matching callback section found for decision translation record {}", id);
          return Future.failedFuture("Unsupported translation callback payload");
        }

        // translations is a nested object: lang -> field -> translatedValue
        JsonObject translations = doc.getJsonObject(TRANSLATIONS, new JsonObject());
        String applicationPortalId = doc.getString(APPLICATION_PORTAL_ID,
          doc.getString(OBJECT_ID));

        // Determine whether this callback came from the file or text endpoint by checking
        // whether any key in the section matches a file-field name.
        Set<String> fileFieldNames = fileFields.stream()
          .map(JsonObject.class::cast)
          .map(e -> e.getString(FIELD))
          .filter(StringUtils::isNotEmpty)
          .collect(Collectors.toSet());
        boolean isFileCallback = section.fieldNames().stream()
          .anyMatch(fileFieldNames::contains);
        JsonArray callbackErrors = extractTranslationErrors(section, isFileCallback);

        // Chain both merge operations; file fields may trigger async HubStore uploads
        return mergeTranslationsFromSection(section, fileFields, translations, applicationPortalId)
          .compose(ignored -> mergeTranslationsFromSection(section, textFields, translations,
            applicationPortalId))
          .compose(ignored -> {

            int totalFields = fileFields.size() + textFields.size();
            String targetLanguage = doc.getString(TARGET_LANGUAGE);
            int langCount = StringUtils.isNotBlank(targetLanguage) ? 1 : SUPPORTED_LANGUAGES.size();
            int totalExpected = totalFields * langCount;
            int translatedCount = countTranslatedEntries(translations);

            int fileRetries = doc.getInteger(FILE_RETRIES, 0);
            int textRetries = doc.getInteger(TEXT_RETRIES, 0);

            // Increment only the counter for the callback type that just arrived
            if (isFileCallback) {
              fileRetries++;
            } else {
              textRetries++;
            }

            String translationStatus;
            if (translatedCount >= totalExpected) {
              translationStatus = TranslationStatus.COMPLETED.getStatus();
              log.info("All translations completed for decision record {}", id);
            } else if (fileRetries >= MAX_RETRIES || textRetries >= MAX_RETRIES) {
              translationStatus = TranslationStatus.FAILED.getStatus();
              log.warn(
                "Translation failed for record {} (fileRetries={}, textRetries={}, {}/{} pairs)",
                id, fileRetries, textRetries, translatedCount, totalExpected);
            } else {
              translationStatus = TranslationStatus.PENDING.getStatus();
              log.warn(
                "Partial translation for record {}: {}/{} pairs (fileRetries={}, textRetries={})",
                id, translatedCount, totalExpected, fileRetries, textRetries);
            }

            final int updatedFileRetries = fileRetries;
            final int updatedTextRetries = textRetries;
            final String updatedTranslationStatus = translationStatus;

            return updateStateRecord(id, state -> {
              state.put(TRANSLATIONS, translations);
              state.put(TRANSLATION_STATUS, updatedTranslationStatus);
              state.put(FILE_RETRIES, updatedFileRetries);
              state.put(TEXT_RETRIES, updatedTextRetries);
              JsonArray stateErrors = state.getJsonArray(TRANSLATION_ERRORS, new JsonArray());
              callbackErrors.forEach(stateErrors::add);
              state.put(TRANSLATION_ERRORS, stateErrors);
            }).compose(v -> {
              if (TranslationStatus.COMPLETED.getStatus().equals(updatedTranslationStatus)
                || TranslationStatus.FAILED.getStatus().equals(updatedTranslationStatus)) {
                doc.put(TRANSLATIONS, translations);
                doc.put(TRANSLATION_STATUS, updatedTranslationStatus);
                doc.put(FILE_RETRIES, updatedFileRetries);
                doc.put(TEXT_RETRIES, updatedTextRetries);
                doc.put(TRANSLATION_ERRORS,
                  appendErrors(doc.getJsonArray(TRANSLATION_ERRORS, new JsonArray()),
                    callbackErrors));
                return syncRecord(doc);
              }
              return Future.succeededFuture();
            });
          });
      })
      .onFailure(err ->
        log.error("Failed to process translation callback for decision record {}", id, err)
      );
  }

  /**
   * Syncs completed (or permanently failed) records back into the application documents. Writes
   * {@code translatedFields: { lang: { field: value, ... }, ... }} onto the target item.
   *
   * @return A Future that completes when all pending syncs are done.
   */
  public Future<Void> syncTranslations() {
    return Future.succeededFuture();
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Translation dispatch
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Dispatches a pending record to the appropriate translation endpoint(s). File fields →
   * file-translation-service; text fields → translation-service. Both requests are sent
   * concurrently. Status advances to WAITING when both succeed.
   */
  private Future<Void> processRecordForTranslation(JsonObject doc) {
    try {
      String id = doc.getString("_id");
      String applicationPortalId = doc.getString(OBJECT_ID, doc.getString(APPLICATION_PORTAL_ID));
      JsonArray fileFields = extractFileFields(doc);
      JsonArray textFields = extractTextFields(doc);

      JsonArray catalogLanguages = doc.getJsonArray(CATALOG_LANGUAGES);
      String storedTargetLanguage = doc.getString(TARGET_LANGUAGE);

      Set<String> inputLangSet = StringUtils.isNotBlank(storedTargetLanguage)
        ? Set.of(doc.getString(TARGET_LANGUAGE).toLowerCase(Locale.ENGLISH))
        : new HashSet<>();
      Set<String> targetLanguages = enableTranslation ? SUPPORTED_LANGUAGES : inputLangSet;
      Set<String> fullyTranslatedFileFields = getFullyTranslatedFields(doc, fileFields,
        targetLanguages);
      Set<String> fullyTranslatedTextFields = getFullyTranslatedFields(doc, textFields,
        targetLanguages);

      JsonArray targetLangs = StringUtils.isNotBlank(storedTargetLanguage)
        ? new JsonArray().add(storedTargetLanguage)
        : buildTargetLanguagesArray(catalogLanguages);
      String sourceLanguage = doc.getString(SOURCE_LANGUAGE, "LD");

      // Build the file dict asynchronously (fetches content from HubStore per file ID)
      return buildFileDictForUntranslated(
        fileFields, fullyTranslatedFileFields, targetLangs, sourceLanguage, applicationPortalId)
        .compose(fileDict -> {
          List<Future<Void>> requests = new ArrayList<>();

          // --- File fields ---
          if (!fileDict.isEmpty()) {
            JsonObject fileRequest = new JsonObject()
              .put("id", TRANSLATIONS_STATE_COLLECTION + "-file-" + UUID.randomUUID())
              .put("data",
                new JsonObject().put("id", id).put("type", TRANSLATIONS_STATE_COLLECTION))
              .put("dict", new JsonObject().put("file", fileDict))
              .put("callback", hubTranslationClient.createCallbackJson());
            requests.add(
              hubTranslationClient.requestFileTranslation(fileRequest)
                .onFailure(err ->
                  log.error("Failed to trigger file translation for record {}", id, err))
            );
          }

          // --- Text fields ---
          JsonObject textDict = buildTextDictForUntranslated(
            textFields, fullyTranslatedTextFields, targetLangs, sourceLanguage);
          if (!textDict.isEmpty()) {
            JsonObject textRequest = new JsonObject()
              .put("id", TRANSLATIONS_STATE_COLLECTION + "-text-" + UUID.randomUUID())
              .put("data",
                new JsonObject().put("id", id).put("type", TRANSLATIONS_STATE_COLLECTION))
              .put("dict", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, textDict))
              .put("callback", hubTranslationClient.createCallbackJson());
            requests.add(
              hubTranslationClient.requestTextTranslation(textRequest)
                .onFailure(err ->
                  log.error("Failed to trigger text translation for record {}", id, err))
            );
          }

          if (requests.isEmpty()) {
            log.warn("No fields left to translate in record {}, marking as completed", id);
            return updateTranslationStatus(id, TranslationStatus.COMPLETED);
          }

          return Future.all(requests)
            .compose(v -> {
              log.info("Triggered translation request(s) for decision record {}", id);
              return updateTranslationStatus(id, TranslationStatus.WAITING);
            })
            .mapEmpty();
        });
    } catch (Exception e) {
      log.error("Error while processing decision translation record for translation", e);
      return Future.failedFuture(e);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Sync
  // ─────────────────────────────────────────────────────────────────────────────

  private Future<Void> syncRecord(JsonObject translationDoc) {
    try {
      String id = translationDoc.getString("_id");
      String applicationId = extractApplicationObjectId(translationDoc);
      String collectionName = extractCollectionName(translationDoc);
      String datasetId = extractDatasetId(translationDoc);
      String catalogId = extractCatalogId(translationDoc);
      int itemIndex = translationDoc.getInteger(ITEM_INDEX, -1);
      String itemType = extractItemType(translationDoc);

      boolean isStatusTranslation = TYPE_STATUS.equals(itemType);
      if ((!isStatusTranslation && StringUtils.isAnyEmpty(id, applicationId, collectionName,
        datasetId, itemType))
        || (isStatusTranslation && StringUtils.isAnyEmpty(id, applicationId, collectionName,
        catalogId, itemType))) {
        log.warn("Missing required fields in decision translation record {}", id);
        return Future.failedFuture("Missing required fields");
      }

      String translationStatus = translationDoc.getString(TRANSLATION_STATUS);
      boolean translationFailed = TranslationStatus.FAILED.getStatus()
        .equalsIgnoreCase(translationStatus);

      if (translationFailed) {
        log.warn("Translation failed for decision record {}, syncing without translated fields",
          id);
        return clearTranslationPlaceholder(collectionName, applicationId, itemType, catalogId,
          datasetId, itemIndex, id)
          .compose(v -> dispatchTranslationToDispatcher(id, null));
      }

      JsonObject translations = translationDoc.getJsonObject(TRANSLATIONS, new JsonObject());
      if (translations.isEmpty()) {
        log.warn("No translations to sync for record {}, syncing without translated fields", id);
        return clearTranslationPlaceholder(collectionName, applicationId, itemType, catalogId,
          datasetId, itemIndex, id)
          .compose(v -> dispatchTranslationToDispatcher(id, null));
      }

      JsonObject appQuery = new JsonObject().put("_id", applicationId);

      return mongoClient.findOne(collectionName, appQuery, null)
        .compose(application -> {
          if (application == null) {
            log.warn("Application {} not found for decision sync", applicationId);
            return Future.failedFuture("Application not found: " + applicationId);
          }

          if (TYPE_STATUS.equals(itemType)) {
            JsonObject targetStatus = findTargetCatalogStatusObject(application, catalogId, id);

            if (targetStatus == null) {
              return Future.failedFuture("Target status not found");
            }

            String statusValue = targetStatus.getString("status");

            // --- ADDITIONAL_INFORMATION FLOW ---
            if ("ADDITIONAL_INFORMATION".equals(statusValue)) {

              JsonArray computedAdditionalInfo =
                computeAdditionalInformation(application, catalogId, translations);

              // Safely get latest element
              JsonObject latestAdditional = computedAdditionalInfo.stream()
                .map(JsonObject.class::cast)
                .reduce((first, second) -> second)
                .orElse(new JsonObject());

              JsonObject update = new JsonObject()
                .put("$set", new JsonObject()
                  .put(ADDITIONAL_INFORMATION, computedAdditionalInfo));

              JsonObject modifiedStatus = targetStatus.copy();

              return mongoClient.updateCollection(collectionName, appQuery, update)
                .map(res -> buildStatusActivityTranslationRecord(
                  applicationId,
                  collectionName,
                  catalogId,
                  modifiedStatus,
                  latestAdditional,
                  id
                ))
                .onFailure(Throwable::printStackTrace);
            }

            // --- DEFAULT FLOW ---
            return syncCatalogStatusTranslation(application, collectionName, appQuery, catalogId,
              id, translations)
              .map(status -> buildStatusActivityTranslationRecord(
                applicationId,
                collectionName,
                catalogId,
                status,
                translations,
                id
              ));
          }

          JsonArray datasets = application.getJsonArray(DATASETS, new JsonArray());
          JsonObject targetDataset = datasets.stream()
            .map(JsonObject.class::cast)
            .filter(ds -> StringUtils.isNotBlank(datasetId)
              && datasetId.equals(ds.getString("dataset_id")))
            .findFirst()
            .orElse(null);

          if (targetDataset == null) {
            log.warn("Dataset {} not found in application {} for decision sync",
              datasetId, applicationId);
            return Future.failedFuture("Dataset not found: " + datasetId);
          }

          final JsonObject dispatchTranslations;
          JsonArray catalogLanguages = retrieveCatalogLanguages(application, catalogId);

          if (catalogLanguages != null && !catalogLanguages.isEmpty()) {
            dispatchTranslations = filterTranslationsForApplication(application,
              translations, catalogLanguages);
          } else {
            dispatchTranslations = translations;
          }

          String arrayKey = TYPE_DECISION.equals(itemType) ? DECISIONS : PERMITS;
          JsonArray items = targetDataset.getJsonArray(arrayKey, new JsonArray());
          JsonObject targetItem = findTargetItem(items, itemIndex, id);
          if (targetItem == null) {
            log.warn("Unable to resolve target item for {} in dataset {}", arrayKey, datasetId);
            return Future.failedFuture("Target item not found");
          }

          // Merge into any existing translatedFields on the item
          JsonObject translatedFields = targetItem.getJsonObject(TRANSLATED_FIELDS,
            new JsonObject());
          translatedFields.remove(TRANSLATIONS_STATE_COLLECTION);
          for (String lang : dispatchTranslations.fieldNames()) {
            JsonObject langTranslations = dispatchTranslations.getJsonObject(lang,
              new JsonObject());
            JsonObject existing = translatedFields.getJsonObject(lang, new JsonObject());
            for (String fieldName : langTranslations.fieldNames()) {
              existing.put(fieldName, langTranslations.getValue(fieldName));
            }
            translatedFields.put(lang, existing);
          }
          targetItem.put(TRANSLATED_FIELDS, translatedFields);

          String targetCatalogId = targetDataset.getString("catalog_id", catalogId);
          String currentCatalogStatus = getLatestCatalogStatus(application, targetCatalogId);
          JsonObject activityRecord = buildActivityTranslationRecord(applicationId,
            collectionName, targetCatalogId, currentCatalogStatus, itemType, targetItem,
            dispatchTranslations, id, datasetId);

          return mongoClient
            .updateCollection(collectionName, appQuery,
              new JsonObject().put("$set", new JsonObject().put(DATASETS, datasets)))
            .map(activityRecord);
        })
        .compose(activityRecord -> dispatchTranslationToDispatcher(id, activityRecord))
        .onFailure(err ->
          log.error("Failed to sync decision translation record {} to application {}",
            id, applicationId, err)
        )
        .mapEmpty();
    } catch (Exception e) {
      log.error("Error while syncing decision translation record", e);
      return Future.failedFuture(e);
    }
  }

  private JsonArray retrieveCatalogLanguages(JsonObject application, String catalogId) {
    return application.getJsonArray("catalogs", new JsonArray()).stream()
      .map(JsonObject.class::cast)
      .filter(c -> catalogId.equals(c.getString("catalog_id")))
      .findFirst()
      .map(c -> c.getJsonArray("catalog_languages"))
      .orElse(null);


  }

  /**
   * Sends the stored dispatcher payload to the national dispatcher and marks the record as synced.
   *
   * @param id               The translation record ID (for logging).
   * @param dispatcherRecord The dispatcher record built from the stored translation activity.
   * @return A Future that completes when the dispatcher call and sync-pending update are done.
   */
  private Future<Void> dispatchTranslationToDispatcher(String id, JsonObject dispatcherRecord) {
    if (dispatcherRecord == null || dispatcherRecord.isEmpty()) {
      log.debug("No ACTIVITY_TRANSLATION payload for record {} – syncing only", id);
      return updateSyncPending(id);
    }

    return saveOutgoingActivityTranslation(dispatcherRecord)
      .onSuccess(v -> log.info("Successfully saved translated activity record {}", id))
      .onFailure(err ->
        log.error("Failed to save translated activity record {}", id, err)
      );
  }

  private Future<Void> saveOutgoingActivityTranslation(JsonObject activityRecord) {
    Promise<Void> promise = Promise.promise();
    mongoClient.save(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION, activityRecord, ar -> {
      if (ar.succeeded()) {
        promise.complete();
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  private JsonObject buildActivityTranslationRecord(String applicationId,
    String collectionName, String catalogId, String catalogStatus, String itemType,
    JsonObject item, JsonObject translatedFields, String translationStateId,
    @Nullable String datasetId) {
    if (item == null || translatedFields == null || translatedFields.isEmpty()) {
      return null;
    }

    JsonObject payload = new JsonObject()
      .put(APPLICATION_TYPE, collectionName)
      .put("catalog_id", catalogId)
      .put("status", catalogStatus);

    // Include datasetId if provided (for dispatcher forwarding)
    if (StringUtils.isNotBlank(datasetId)) {
      item.put(DATASET_ID, datasetId);
    }

    JsonObject itemCopy = item.copy();
    itemCopy.put(TRANSLATED_FIELDS, translatedFields.copy());
    if (TYPE_DECISION.equals(itemType)) {
      itemCopy.remove("decision_file");
      itemCopy.remove("decision_reason");
      itemCopy.remove("latest");
      String datasetStatus = itemCopy.getString("datasetStatus");
      itemCopy.put("decision", datasetStatus);
      itemCopy.remove("datasetStatus");

      payload.put(DECISIONS, new JsonArray().add(itemCopy));
    } else if (TYPE_PERMIT.equals(itemType)) {
      payload.put(PERMITS, new JsonArray().add(itemCopy));
    } else {
      return null;
    }

    return buildOutgoingActivityTranslation(applicationId, payload, translationStateId);
  }

  private JsonObject buildOutgoingActivityTranslation(String applicationId, JsonObject payload,
    String translationStateId) {
    JsonObject configuration = new JsonObject()
      .put(SYNC_PENDING, "1")
      .put(TRANSLATION_STATE_ID, translationStateId);
    return new JsonObject()
      .put(OBJECT_ID, applicationId)
      .put("type", "application")
      .put(OPERATION, OperationStatus.ACTIVITY_TRANSLATION.getLabel())
      .put(PAYLOAD, payload)
      .put("configuration", configuration);
  }

  private JsonArray extractTranslationErrors(JsonObject section, boolean isFileCallback) {
    JsonArray errors = new JsonArray();
    for (String fieldName : section.fieldNames()) {
      JsonObject field = section.getJsonObject(fieldName, new JsonObject());
      JsonObject fieldTranslations = field.getJsonObject(TRANSLATIONS, new JsonObject());
      for (String lang : fieldTranslations.fieldNames()) {
        Object rawValue = fieldTranslations.getValue(lang);
        if (!TranslationUtils.isNumericErrorCode(rawValue)) {
          continue;
        }

        Integer code = rawValue instanceof Number number
          ? number.intValue()
          : Integer.parseInt(String.valueOf(rawValue).trim());
        errors.add(new JsonObject()
          .put(FIELD, fieldName)
          .put(LANGUAGE, lang)
          .put(ERROR_CODE, code)
          .put(CALLBACK_TYPE, isFileCallback ? "file" : "text")
          .put(RECORDED_AT, Instant.now().toString()));
      }
    }
    return errors;
  }

  private JsonArray appendErrors(JsonArray existingErrors, JsonArray newErrors) {
    JsonArray merged = existingErrors == null ? new JsonArray() : existingErrors.copy();
    if (newErrors != null) {
      newErrors.forEach(merged::add);
    }
    return merged;
  }

  private JsonObject extractCallbackSection(JsonObject dict, JsonArray fileFields,
    JsonArray textFields) {
    JsonObject translationStateSection = dict.getJsonObject(TRANSLATIONS_STATE_COLLECTION,
      new JsonObject());
    if (!translationStateSection.isEmpty()) {
      return translationStateSection;
    }

    JsonObject fileSection = dict.getJsonObject("file", new JsonObject());
    if (matchesKnownFields(fileSection, fileFields)) {
      return fileSection;
    }

    JsonObject textSection = dict.getJsonObject("text", new JsonObject());
    if (matchesKnownFields(textSection, textFields)) {
      return textSection;
    }

    if (matchesKnownFields(dict, fileFields) || matchesKnownFields(dict, textFields)) {
      return dict;
    }

    return new JsonObject();
  }

  private boolean matchesKnownFields(JsonObject section, JsonArray fields) {
    if (section == null || section.isEmpty() || fields == null || fields.isEmpty()) {
      return false;
    }

    Set<String> knownFields = fields.stream()
      .map(JsonObject.class::cast)
      .map(entry -> entry.getString(FIELD))
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.toSet());

    return section.fieldNames().stream().anyMatch(knownFields::contains);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Translation accumulation helpers
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Merges translated values from a callback section into the translations map. For fields whose
   * name contains "file", the translated value (expected to be base64-encoded) is uploaded to
   * HubStore and the returned file ID is stored instead of the raw content.
   *
   * @param section             The callback section keyed by field name.
   * @param fields              Field descriptor array (each entry has {@code field} key).
   * @param translations        Mutable translations map to merge into
   *                            ({@code lang -> field -> value}).
   * @param applicationPortalId Portal application ID used as parameterId for HubStore uploads.
   * @return A Future that completes when all merges (including any async uploads) are done.
   */
  private Future<Void> mergeTranslationsFromSection(
    JsonObject section, JsonArray fields, JsonObject translations, String applicationPortalId
  ) {
    List<Future<Void>> futures = new ArrayList<>();

    fields.stream()
      .map(JsonObject.class::cast)
      .forEach(entry -> {
        String fieldName = entry.getString(FIELD);
        JsonObject fieldResult = section.getJsonObject(fieldName, new JsonObject());
        JsonObject fieldTranslations = fieldResult.getJsonObject(TRANSLATIONS, new JsonObject());

        for (String lang : fieldTranslations.fieldNames()) {
          Object translatedValue = fieldTranslations.getValue(lang);
          if (translatedValue == null || TranslationUtils.isNumericErrorCode(translatedValue)) {
            continue;
          }

          if (fieldName.contains("file")
            && hubStoreClient != null
            && StringUtils.isNotBlank(applicationPortalId)
            && translatedValue instanceof String translatedString
            && StringUtils.isNotBlank(translatedString)) {
            // Attempt to decode as base64 and upload to HubStore
            final String capturedLang = lang;
            Future<Void> uploadFuture;
            try {
              byte[] fileBytes = Base64.getMimeDecoder().decode(translatedString);
              if (fileBytes.length > 64) {
                Buffer fileBuffer = Buffer.buffer(fileBytes);
                String uploadFileName = fieldName + "_" + capturedLang + ".pdf";
                uploadFuture = hubStoreClient
                  .uploadFile(APPLICATIONID, applicationPortalId, uploadFileName,
                    fileBuffer, "application/pdf")
                  .compose(response -> {
                    String fileId = response.getJsonObject("result", new JsonObject())
                      .getString("id");
                    if (StringUtils.isNotBlank(fileId)) {
                      JsonObject langObj = translations.getJsonObject(capturedLang,
                        new JsonObject());
                      langObj.put(fieldName, fileId);
                      translations.put(capturedLang, langObj);
                    } else {
                      log.warn("HubStore returned no file ID for field {} lang {}",
                        fieldName, capturedLang);
                      // Fall back to storing value as-is
                      JsonObject langObj = translations.getJsonObject(capturedLang,
                        new JsonObject());
                      langObj.put(fieldName, translatedString);
                      translations.put(capturedLang, langObj);
                    }
                    return Future.<Void>succeededFuture();
                  })
                  .recover(err -> {
                    log.warn(
                      "Failed to upload translated file for field {} lang {} (app {}), "
                        + "storing value as-is: {}",
                      fieldName, capturedLang, applicationPortalId, err.getMessage());
                    JsonObject langObj = translations.getJsonObject(capturedLang,
                      new JsonObject());
                    langObj.put(fieldName, translatedString);
                    translations.put(capturedLang, langObj);
                    return Future.succeededFuture();
                  });
              } else {
                // Too small to be a real file — treat as plain value (e.g. already an ID)
                JsonObject langObj = translations.getJsonObject(lang, new JsonObject());
                langObj.put(fieldName, translatedValue);
                translations.put(lang, langObj);
                uploadFuture = Future.succeededFuture();
              }
            } catch (Exception ignored) {
              // Not valid base64 — store as-is (may already be a HubStore file ID)
              JsonObject langObj = translations.getJsonObject(lang, new JsonObject());
              langObj.put(fieldName, translatedValue);
              translations.put(lang, langObj);
              uploadFuture = Future.succeededFuture();
            }
            futures.add(uploadFuture);
          } else {
            // Non-file field — store translated text directly
            JsonObject langObj = translations.getJsonObject(lang, new JsonObject());
            langObj.put(fieldName, translatedValue);
            translations.put(lang, langObj);
          }
        }
      });

    return futures.isEmpty()
      ? Future.succeededFuture()
      : Future.all(futures).mapEmpty();
  }

  /**
   * Counts the total number of (lang, field) translation pairs already accumulated.
   */
  private int countTranslatedEntries(JsonObject translations) {
    int count = 0;
    for (String lang : translations.fieldNames()) {
      count += translations.getJsonObject(lang, new JsonObject()).size();
    }
    return count;
  }

  /**
   * Returns the set of field names from {@code fields} that already have translations for
   * <em>all</em> supported languages.
   */
  private Set<String> getFullyTranslatedFields(JsonObject doc, JsonArray fields,
    Set<String> languages) {
    JsonObject translations = doc.getJsonObject(TRANSLATIONS, new JsonObject());
    String targetLanguage = doc.getString(TARGET_LANGUAGE);
    Collection<String> expectedLangs = StringUtils.isNotBlank(targetLanguage)
      ? List.of(targetLanguage.toLowerCase(Locale.ENGLISH))
      : languages;
    Set<String> fullyDone = new HashSet<>();
    fields.stream()
      .map(JsonObject.class::cast)
      .map(e -> e.getString(FIELD))
      .filter(StringUtils::isNotEmpty)
      .forEach(fieldName -> {
        long coveredLangs = expectedLangs.stream()
          .filter(lang -> {
            JsonObject langObj = translations.getJsonObject(lang, new JsonObject());
            return langObj.containsKey(fieldName);
          })
          .count();
        if (coveredLangs >= expectedLangs.size()) {
          fullyDone.add(fieldName);
        }
      });
    return fullyDone;
  }

  private JsonArray extractFileFields(JsonObject doc) {
    JsonArray existing = doc.getJsonArray(FILE_FIELDS);
    if (existing != null) {
      return existing;
    }

    JsonArray fields = new JsonArray();
    JsonObject payload = doc.getJsonObject(PAYLOAD, new JsonObject());
    String itemType = extractItemType(doc);
    JsonObject item = extractPayloadItem(payload, itemType);
    if (item == null) {
      return fields;
    }

    if (TYPE_DECISION.equals(itemType)) {
      addFileFieldSync(fields, item, DECISION_FILE);
      addFileFieldSync(fields, item, SUPPORT_FILE);
    } else if (TYPE_PERMIT.equals(itemType)) {
      addFileFieldSync(fields, item, PERMIT_FILE);
    }
    return fields;
  }

  private JsonArray extractTextFields(JsonObject doc) {
    JsonArray existing = doc.getJsonArray(TEXT_FIELDS);
    if (existing != null) {
      return existing;
    }

    JsonArray fields = new JsonArray();
    JsonObject payload = doc.getJsonObject(PAYLOAD, new JsonObject());
    String itemType = extractItemType(doc);
    JsonObject item = extractPayloadItem(payload, itemType);
    if (item == null) {
      return fields;
    }

    if (TYPE_DECISION.equals(itemType)) {
      addTextField(fields, item, DECISION_REASON);
    } else if (TYPE_PERMIT.equals(itemType)) {
      addTextField(fields, item, PERMIT_CONTACT_DETAILS);
    }
    return fields;
  }

  private String extractApplicationObjectId(JsonObject doc) {
    return doc.getString(OBJECT_ID, doc.getString(APPLICATION_PORTAL_ID));
  }

  private String extractCollectionName(JsonObject doc) {
    return doc.getString(APPLICATION_TYPE);
  }

  private String extractDatasetId(JsonObject doc) {
    if (doc.containsKey(DATASET_ID)) {
      return doc.getString(DATASET_ID);
    }
    JsonObject payload = doc.getJsonObject(PAYLOAD, new JsonObject());
    String itemType = extractItemType(doc);
    JsonObject item = extractPayloadItem(payload, itemType);
    return item != null ? item.getString("dataset_id") : null;
  }

  private String extractCatalogId(JsonObject doc) {
    if (doc.containsKey(CATALOG_ID)) {
      return doc.getString(CATALOG_ID);
    }
    JsonObject payload = doc.getJsonObject(PAYLOAD, new JsonObject());
    return payload.getString("catalog_id");
  }

  private String extractItemType(JsonObject doc) {
    if (doc.containsKey(ITEM_TYPE)) {
      return doc.getString(ITEM_TYPE);
    }
    String operation = doc.getString(OPERATION);
    if (OperationStatus.DECISION.getLabel().equalsIgnoreCase(operation)) {
      return TYPE_DECISION;
    }
    if (OperationStatus.PERMIT.getLabel().equalsIgnoreCase(operation)) {
      return TYPE_PERMIT;
    }
    if (OperationStatus.STATUS_UPDATE.getLabel().equalsIgnoreCase(operation)) {
      return TYPE_STATUS;
    }
    return null;
  }

  private JsonObject extractPayloadItem(JsonObject payload, String itemType) {
    if (payload == null || StringUtils.isBlank(itemType)) {
      return null;
    }
    JsonArray items = TYPE_DECISION.equals(itemType)
      ? payload.getJsonArray(DECISIONS, new JsonArray())
      : payload.getJsonArray(PERMITS, new JsonArray());
    return items.isEmpty() ? null : items.getJsonObject(0);
  }

  private JsonObject buildStatusActivityTranslationRecord(String applicationId,
    String collectionName, String catalogId, JsonObject status, JsonObject translatedFields,
    String translationStateId) {
    if (status == null || translatedFields == null || translatedFields.isEmpty()) {
      return null;
    }

    JsonObject statusPayload = status.getJsonObject("statusPayload", new JsonObject()).copy();
    if (status.getString("status").equals("ADDITIONAL_INFORMATION")) {
      statusPayload.put(ADDITIONAL_INFORMATION, translatedFields);
    } else {
      statusPayload.put(TRANSLATED_FIELDS, translatedFields.copy());
    }

    JsonObject payload = new JsonObject()
      .put(APPLICATION_TYPE, collectionName)
      .put("catalog_id", catalogId)
      .put(CATALOG_ID, catalogId)
      .put("status", status.getString("status"))
      .put("statusPayload", statusPayload);

    return buildOutgoingActivityTranslation(applicationId, payload, translationStateId);
  }

  private JsonObject findTargetItem(JsonArray items, int itemIndex, String translationStateId) {
    JsonObject targetByStateId = findTargetItemByTranslationStateId(items, translationStateId);
    if (targetByStateId != null) {
      return targetByStateId;
    }

    if (itemIndex >= 0 && itemIndex < items.size()) {
      return items.getJsonObject(itemIndex);
    }

    for (int i = items.size() - 1; i >= 0; i--) {
      JsonObject item = items.getJsonObject(i);
      if (item.getInteger("latest", 0) == 1) {
        return item;
      }
    }

    return items.isEmpty() ? null : items.getJsonObject(items.size() - 1);
  }

  private JsonObject findTargetItemByTranslationStateId(JsonArray items,
    String translationStateId) {
    if (StringUtils.isBlank(translationStateId)) {
      return null;
    }

    for (Object itemObj : items) {
      JsonObject item = (JsonObject) itemObj;
      JsonObject translatedFields = item.getJsonObject(TRANSLATED_FIELDS, new JsonObject());
      if (translationStateId.equals(translatedFields.getString(TRANSLATIONS_STATE_COLLECTION))) {
        return item;
      }
    }
    return null;
  }

  private Future<JsonObject> syncCatalogStatusTranslation(JsonObject application,
    String collectionName,
    JsonObject appQuery, String catalogId, String translationStateId, JsonObject translations) {
    JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());
    JsonObject targetCatalog = catalogs.stream()
      .map(JsonObject.class::cast)
      .filter(catalog -> catalogId.equals(catalog.getString("catalog_id")))
      .findFirst()
      .orElse(null);

    if (targetCatalog == null) {
      return Future.failedFuture("Catalog not found: " + catalogId);
    }

    JsonArray statuses = targetCatalog.getJsonArray("statuses", new JsonArray());
    JsonObject targetStatus = findTargetStatusByTranslationStateId(statuses, translationStateId);
    if (targetStatus == null) {
      targetStatus = findLatestStatus(statuses);
    }
    if (targetStatus == null) {
      return Future.failedFuture("Catalog status not found: " + catalogId);
    }

    JsonObject statusPayload = targetStatus.getJsonObject("statusPayload", new JsonObject());
    JsonObject translatedFields = statusPayload.getJsonObject(TRANSLATED_FIELDS, new JsonObject());
    translatedFields.remove(TRANSLATIONS_STATE_COLLECTION);
    for (String lang : translations.fieldNames()) {
      JsonObject langTranslations = translations.getJsonObject(lang, new JsonObject());
      JsonObject existing = translatedFields.getJsonObject(lang, new JsonObject());
      for (String fieldName : langTranslations.fieldNames()) {
        existing.put(fieldName, langTranslations.getValue(fieldName));
      }
      translatedFields.put(lang, existing);
    }
    statusPayload.put(TRANSLATED_FIELDS, translatedFields);
    targetStatus.put("statusPayload", statusPayload);

    return mongoClient.updateCollection(collectionName,
        appQuery, new JsonObject().put("$set", new JsonObject().put("catalogs", catalogs)))
      .map(targetStatus);
  }

  private JsonObject filterTranslationsForApplication(JsonObject application,
    JsonObject translations, @Nullable JsonArray catalogLanguages) {
    JsonArray targetLanguages = application.getJsonObject(TRANSLATION, new JsonObject())
      .getJsonArray(TARGET_LANGUAGES, new JsonArray());

    // For activities with catalog_languages, use those instead of application target languages
    if (catalogLanguages != null && !catalogLanguages.isEmpty()) {
      targetLanguages = catalogLanguages;
    } else if (targetLanguages.isEmpty()) {
      return translations.copy();
    }

    Set<String> allowedLanguages = targetLanguages.stream()
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .map(lang -> lang.toLowerCase(Locale.ENGLISH))
      .collect(Collectors.toSet());

    JsonObject filtered = new JsonObject();
    for (String lang : translations.fieldNames()) {
      if (allowedLanguages.contains(lang.toLowerCase(Locale.ENGLISH))) {
        filtered.put(lang, translations.getJsonObject(lang, new JsonObject()).copy());
      }
    }

    String applicationLanguage = application.getString(INPUT_LANGUAGE, null);
    if (applicationLanguage != null && !filtered.containsKey(applicationLanguage)) {
      filtered.put(applicationLanguage,
        translations.getJsonObject(applicationLanguage, new JsonObject()).copy());
    }
    return filtered;
  }

  private JsonObject findTargetStatusByTranslationStateId(JsonArray statuses,
    String translationStateId) {
    if (StringUtils.isBlank(translationStateId)) {
      return null;
    }

    for (Object statusObj : statuses) {
      JsonObject status = (JsonObject) statusObj;
      JsonObject statusPayload = status.getJsonObject("statusPayload", new JsonObject());
      JsonObject translatedFields = statusPayload.getJsonObject(TRANSLATED_FIELDS,
        new JsonObject());
      if (translationStateId.equals(translatedFields.getString(TRANSLATIONS_STATE_COLLECTION))) {
        return status;
      }
    }
    return null;
  }

  private JsonObject findLatestStatus(JsonArray statuses) {
    for (int i = statuses.size() - 1; i >= 0; i--) {
      JsonObject status = statuses.getJsonObject(i);
      if (status.getInteger("latest", 0) == 1) {
        return status;
      }
    }
    return statuses.isEmpty() ? null : statuses.getJsonObject(statuses.size() - 1);
  }

  private String getLatestCatalogStatus(JsonObject application, String catalogId) {
    JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());
    for (Object catalogObj : catalogs) {
      JsonObject catalog = (JsonObject) catalogObj;
      if (!catalogId.equals(catalog.getString("catalog_id"))) {
        continue;
      }
      JsonObject latestStatus = findLatestStatus(catalog.getJsonArray("statuses", new JsonArray()));
      return latestStatus == null ? null : latestStatus.getString("status");
    }
    return null;
  }

  /**
   * Finds the target catalog status object for a given catalog, first by translationStateId, then
   * by latest flag.
   */
  private JsonObject findTargetCatalogStatusObject(JsonObject application, String catalogId,
    String translationStateId) {
    JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());
    JsonObject targetCatalog = catalogs.stream()
      .map(JsonObject.class::cast)
      .filter(c -> catalogId.equals(c.getString("catalog_id")))
      .findFirst()
      .orElse(null);

    if (targetCatalog == null) {
      return null;
    }

    JsonArray statuses = targetCatalog.getJsonArray("statuses", new JsonArray());
    JsonObject targetStatus = findTargetStatusByTranslationStateId(statuses, translationStateId);
    if (targetStatus == null) {
      targetStatus = findLatestStatus(statuses);
    }
    return targetStatus;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Dict builders
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Builds the file-translation dict for fields not yet fully translated. Each file entry holds a
   * HubStore file ID in {@code file_hub_repo_id}; the actual Base64 content is fetched
   * asynchronously via {@link HubStoreClient#getFileAsBase64}. Fields whose retrieval fails are
   * skipped with a warning (not fatal).
   *
   * @param fileFields          file-field descriptors from the translation state record
   * @param alreadyDone         field names whose translations are already complete
   * @param targetLangs         target language codes
   * @param sourceLanguage      source language code
   * @param applicationPortalId portal application ID used as parameterId when retrieving files
   * @return a Future containing the populated dict (may be empty if all retrievals fail)
   */
  private Future<JsonObject> buildFileDictForUntranslated(
    JsonArray fileFields, Set<String> alreadyDone, JsonArray targetLangs,
    String sourceLanguage, String applicationPortalId
  ) {
    List<JsonObject> pending = fileFields.stream()
      .map(JsonObject.class::cast)
      .filter(e -> StringUtils.isNotEmpty(e.getString(FIELD))
        && StringUtils.isNotEmpty(e.getString(FILE_HUB_REPO_ID))
        && !alreadyDone.contains(e.getString(FIELD)))
      .collect(Collectors.toList());

    if (pending.isEmpty()) {
      return Future.succeededFuture(new JsonObject());
    }

    JsonObject dict = new JsonObject();
    List<Future<Void>> futures = new ArrayList<>();

    for (JsonObject e : pending) {
      String fieldName = e.getString(FIELD);
      String fileId = e.getString(FILE_HUB_REPO_ID);
      String defaultFormat = e.getString(FORMAT, "pdf");
      String defaultFileName = e.getString(FILE_NAME, fieldName + "." + defaultFormat);

      Future<Void> f = hubStoreClient.getFileAsBase64(fileId, APPLICATIONID, applicationPortalId)
        .recover(err -> {
          log.error("Failed to retrieve file {} for field {} (app {}): {}",
            fileId, fieldName, applicationPortalId, err.getMessage());
          return Future.succeededFuture(null);
        })
        .compose(result -> {
          if (result != null) {
            String base64Content = result.getString("base64Content");
            String format = result.getString(FORMAT, defaultFormat);
            String fileName = result.getString(FILE_NAME, defaultFileName);
            if (StringUtils.isNotBlank(base64Content)) {
              dict.put(fieldName, new JsonObject()
                .put("documentToTranslateBase64", new JsonObject()
                  .put("content", base64Content)
                  .put(FORMAT, format)
                  .put(FILE_NAME, fileName))
                .put("sourceLanguage", sourceLanguage)
                .put("targetLanguages", targetLangs));
            }
          }
          return Future.succeededFuture();
        })
        .mapEmpty();
      futures.add(f);
    }

    return Future.all(new ArrayList<>(futures)).map(dict);
  }

  /**
   * Builds the text-translation dict for text fields not yet fully translated. Uses {@code value}
   * format, targeting specified languages with configurable source language.
   */
  private JsonObject buildTextDictForUntranslated(
    JsonArray textFields, Set<String> alreadyDone, JsonArray targetLangs, String sourceLanguage
  ) {
    JsonObject dict = new JsonObject();
    textFields.stream()
      .map(JsonObject.class::cast)
      .filter(e -> StringUtils.isNoneEmpty(e.getString(FIELD), e.getString(VALUE))
        && !alreadyDone.contains(e.getString(FIELD)))
      .forEach(e -> {
        String fieldName = e.getString(FIELD);
        dict.put(fieldName, new JsonObject()
          .put(VALUE, e.getString(VALUE))
          .put("sourceLanguage", sourceLanguage)
          .put("targetLanguages", targetLangs));
      });
    return dict;
  }

  /**
   * Builds a JsonArray of target language codes. If catalogLanguages is provided and not empty,
   * uses those languages (for activities); otherwise uses all supported languages.
   */
  private JsonArray buildTargetLanguagesArray(@Nullable JsonArray catalogLanguages) {
    JsonArray arr = new JsonArray();
    if (catalogLanguages != null && catalogLanguages.size() > 0) {
      // For activities with catalog_languages, use only those languages
      catalogLanguages.forEach(arr::add);
    } else {
      // Otherwise, use all supported languages
      SUPPORTED_LANGUAGES.forEach(arr::add);
    }
    return arr;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Field collectors
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Appends a file-field descriptor to {@code fields}.
   * <p>If {@code fieldName} contains {@code "file"} and a {@link HubStoreClient} is available,
   * the value is treated as raw Base64 file content: it is uploaded to HubStore and the returned
   * file ID is stored under {@code file_hub_repo_id}. If the value cannot be decoded as a
   * meaningful file (e.g. it is already a file ID), it is stored as-is so it can be retrieved later
   * by {@link #buildFileDictForUntranslated}.
   *
   * @param fields              accumulating array of file-field descriptors
   * @param source              JSON object holding the field value
   * @param fieldName           name of the field
   * @param applicationPortalId portal application ID used as the parameterId when uploading
   * @return a Future that completes when the descriptor has been added (or skipped)
   */
  private Future<Void> addFileField(JsonArray fields, JsonObject source, String fieldName,
    String applicationPortalId) {
    String value = source.getString(fieldName);
    if (StringUtils.isBlank(value)) {
      return Future.succeededFuture();
    }

    if (fieldName.contains("file") && hubStoreClient != null
      && StringUtils.isNotBlank(applicationPortalId)) {
      try {
        byte[] fileBytes = Base64.getMimeDecoder().decode(value);
        // A real file will have many more bytes than a simple ID string
        if (fileBytes.length > 64) {
          Buffer fileBuffer = Buffer.buffer(fileBytes);
          return hubStoreClient.uploadFile(APPLICATIONID, applicationPortalId,
              fieldName + ".pdf", fileBuffer, "application/pdf")
            .map(response -> response.getJsonObject("result").getString("id"))
            .recover(err -> {
              log.warn("Failed to upload file for field {} (app {}), storing value as-is: {}",
                fieldName, applicationPortalId, err.getMessage());
              return Future.succeededFuture(value);
            })
            .compose(fileId -> {
              if (StringUtils.isNotBlank(fileId)) {
                fields.add(new JsonObject()
                  .put(FIELD, fieldName)
                  .put(FILE_HUB_REPO_ID, fileId)
                  .put(FORMAT, "pdf")
                  .put(FILE_NAME, fieldName + ".pdf"));
              }
              return Future.succeededFuture();
            });
        }
      } catch (Exception ignored) {
        // Not valid Base64 — value is already a HubStore file ID, fall through
      }
    }

    // Value is an existing HubStore file ID (or hubStoreClient is unavailable)
    fields.add(new JsonObject()
      .put(FIELD, fieldName)
      .put(FILE_HUB_REPO_ID, value)
      .put(FORMAT, "pdf")
      .put(FILE_NAME, fieldName + ".pdf"));
    return Future.succeededFuture();
  }

  /**
   * Synchronous fallback used only in the {@link #extractFileFields} dead-code path. Stores the
   * value as-is without attempting any HubStore upload.
   */
  private void addFileFieldSync(JsonArray fields, JsonObject source, String fieldName) {
    String value = source.getString(fieldName);
    if (StringUtils.isBlank(value)) {
      return;
    }
    fields.add(new JsonObject()
      .put(FIELD, fieldName)
      .put(FILE_HUB_REPO_ID, value)
      .put(FORMAT, "pdf")
      .put(FILE_NAME, fieldName + ".pdf"));
  }

  /**
   * Appends a text field descriptor to {@code fields} if the source value is non-blank.
   */
  private void addTextField(JsonArray fields, JsonObject source, String fieldName) {
    String value = source.getString(fieldName);
    if (StringUtils.isBlank(value)) {
      return;
    }
    fields.add(new JsonObject()
      .put(FIELD, fieldName)
      .put(VALUE, value));
  }

  private void addStatusTextFields(JsonArray fields, @Nullable JsonObject statusPayload) {
    if (statusPayload == null) {
      return;
    }

    addTextField(fields, statusPayload, "reason");

    JsonArray sourceEntries = statusPayload.getJsonObject("additionalInformation", new JsonObject())
      .getJsonArray("source", new JsonArray());

    for (Object entryObj : sourceEntries) {
      if (!(entryObj instanceof JsonObject entry)) {
        continue;
      }

      String fieldName = entry.getString("field");
      String value = entry.getString("comment");
      if (StringUtils.isNoneBlank(fieldName, value)) {
        fields.add(new JsonObject().put(FIELD, fieldName).put(VALUE, value));
      }
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // MongoDB helpers
  // ─────────────────────────────────────────────────────────────────────────────


  private JsonObject buildRecord(
    String collectionName,
    String applicationPortalId,
    String applicationId,
    String datasetId,
    int itemIndex,
    String itemType,
    JsonArray fileFields,
    JsonArray textFields,
    String catalogId
  ) {
    String operation = TYPE_DECISION.equals(itemType)
      ? OperationStatus.DECISION.getLabel()
      : TYPE_STATUS.equals(itemType)
        ? OperationStatus.STATUS_UPDATE.getLabel()
        : OperationStatus.PERMIT.getLabel();

    return new JsonObject()
      .put("_id", UUID.randomUUID().toString())
      .put(OBJECT_ID, applicationPortalId)
      .put("type", "application")
      .put(OPERATION, operation)
      .put(APPLICATION_TYPE, collectionName)
      .put(APPLICATION_PORTAL_ID, applicationPortalId)
      .put(APPLICATION_ID, applicationId)
      .put(DATASET_ID, datasetId)
      .put(CATALOG_ID, catalogId)
      .put(ITEM_INDEX, itemIndex)
      .put(ITEM_TYPE, itemType)
      .put(FILE_FIELDS, fileFields)
      .put(TEXT_FIELDS, textFields)
      .put(TRANSLATIONS, new JsonObject())
      .put(TRANSLATION_ERRORS, new JsonArray())
      .put(TRANSLATION_STATUS, TranslationStatus.PENDING.getStatus())
      .put(FILE_RETRIES, 0)
      .put(TEXT_RETRIES, 0)
      .put(SYNC_PENDING, "1")
      .put("configuration", new JsonObject()
        .put("syncPending", "1")
        .put(DISPATCHER_STATUS, PENDING));
  }

  private Future<String> insertRecord(JsonObject doc) {
    String recordId = doc.getString("_id");
    String collectionName = doc.getString(APPLICATION_TYPE);
    String applicationPortalId = doc.getString(APPLICATION_PORTAL_ID);

    if (StringUtils.isAnyBlank(recordId, collectionName, applicationPortalId)) {
      return Future.failedFuture(
        "Missing required fields while saving decision translation record");
    }

    return mongoClient.save(TRANSLATIONS_STATE_COLLECTION, doc)
      .map(recordId)
      .onSuccess(id -> log.debug("Decision translation record inserted with id {}", id))
      .onFailure(err -> log.error("Failed to insert decision translation record", err));
  }

  private Future<Void> updateTranslationStatus(String id, TranslationStatus status) {
    return updateStateRecord(id, state -> state.put(TRANSLATION_STATUS, status.getStatus()))
      .onSuccess(v ->
        log.debug("Updated translation status to {} for record {}", status.getStatus(), id))
      .onFailure(err ->
        log.error("Failed to update translation status for decision record {}", id, err))
      .mapEmpty();
  }

  private Future<Void> updateSyncPending(String id) {
    return updateStateRecord(id, state -> {
      state.put(SYNC_PENDING, "0");
      JsonObject configuration = state.getJsonObject("configuration", new JsonObject());
      configuration.put(SYNC_PENDING, "0");
      state.put("configuration", configuration);
    })
      .onSuccess(v -> log.debug("Marked translation state {} as synced", id))
      .onFailure(err ->
        log.error("Failed to mark translation state as synced for decision record {}", id,
          err))
      .mapEmpty();
  }

  private void applyExistingStateId(JsonObject doc, @Nullable JsonObject translatedFields) {
    if (translatedFields == null) {
      return;
    }
    String existingStateId = translatedFields.getString(TRANSLATIONS_STATE_COLLECTION);
    if (StringUtils.isNotBlank(existingStateId)) {
      doc.put("_id", existingStateId);
    }
  }

  private Future<List<JsonObject>> findRecordsByQuery(String collectionName,
    JsonObject stateQuery) {
    JsonObject query = stateQuery.copy().put(APPLICATION_TYPE, collectionName);
    return mongoClient.find(TRANSLATIONS_STATE_COLLECTION, query);
  }

  private Future<Void> clearTranslationPlaceholder(String collectionName, String applicationId,
    String itemType, String catalogId, String datasetId, int itemIndex, String translationStateId) {
    JsonObject appQuery = new JsonObject().put("_id", applicationId);
    return mongoClient.findOne(collectionName, appQuery, null).compose(application -> {
      if (application == null) {
        return Future.succeededFuture();
      }

      if (TYPE_STATUS.equals(itemType)) {
        JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());
        JsonObject targetCatalog = catalogs.stream()
          .map(JsonObject.class::cast)
          .filter(catalog -> catalogId.equals(catalog.getString("catalog_id")))
          .findFirst()
          .orElse(null);
        if (targetCatalog == null) {
          return Future.succeededFuture();
        }

        JsonObject targetStatus = findTargetStatusByTranslationStateId(
          targetCatalog.getJsonArray("statuses", new JsonArray()), translationStateId);
        if (targetStatus != null) {
          JsonObject statusPayload = targetStatus.getJsonObject("statusPayload", new JsonObject());
          JsonObject translatedFields = statusPayload.getJsonObject(TRANSLATED_FIELDS,
            new JsonObject());
          translatedFields.remove(TRANSLATIONS_STATE_COLLECTION);
          if (translatedFields.isEmpty()) {
            statusPayload.remove(TRANSLATED_FIELDS);
          } else {
            statusPayload.put(TRANSLATED_FIELDS, translatedFields);
          }
          targetStatus.put("statusPayload", statusPayload);
          return mongoClient.updateCollection(collectionName, appQuery,
            new JsonObject().put("$set", new JsonObject().put("catalogs", catalogs))).mapEmpty();
        }

        return Future.succeededFuture();
      }

      JsonArray datasets = application.getJsonArray(DATASETS, new JsonArray());
      JsonObject targetDataset = datasets.stream()
        .map(JsonObject.class::cast)
        .filter(ds -> StringUtils.isNotBlank(datasetId)
          && datasetId.equals(ds.getString("dataset_id")))
        .findFirst()
        .orElse(null);
      if (targetDataset == null) {
        return Future.succeededFuture();
      }

      JsonArray items = targetDataset.getJsonArray(
        TYPE_DECISION.equals(itemType) ? DECISIONS : PERMITS,
        new JsonArray());
      JsonObject targetItem = findTargetItem(items, itemIndex, translationStateId);
      if (targetItem == null) {
        return Future.succeededFuture();
      }

      JsonObject translatedFields = targetItem.getJsonObject(TRANSLATED_FIELDS, new JsonObject());
      translatedFields.remove(TRANSLATIONS_STATE_COLLECTION);
      if (translatedFields.isEmpty()) {
        targetItem.remove(TRANSLATED_FIELDS);
      } else {
        targetItem.put(TRANSLATED_FIELDS, translatedFields);
      }

      return mongoClient.updateCollection(collectionName, appQuery,
        new JsonObject().put("$set", new JsonObject().put(DATASETS, datasets))).mapEmpty();
    });
  }

  private Future<JsonObject> findStateRecord(String stateId) {
    return mongoClient.findOne(TRANSLATIONS_STATE_COLLECTION, new JsonObject().put("_id", stateId),
      null);
  }

  private Future<Void> updateStateRecord(String stateId,
    java.util.function.Consumer<JsonObject> mutator) {
    return findStateRecord(stateId).compose(state -> {
      if (state == null) {
        return Future.failedFuture("Translation state not found: " + stateId);
      }

      mutator.accept(state);
      JsonObject updateDoc = state.copy();
      updateDoc.remove("_id");

      return mongoClient.updateCollection(TRANSLATIONS_STATE_COLLECTION,
          new JsonObject().put("_id", stateId),
          new JsonObject().put("$set", updateDoc))
        .mapEmpty();
    });
  }


  @NotNull
  private JsonArray computeAdditionalInformation(
    @NotNull JsonObject application,
    @NotNull String catalogId,
    @NotNull JsonObject filteredTranslations
  ) {
    // filteredTranslations has one language key → extract its value for flat (language-agnostic) storage
    JsonObject singleLangTranslations = filteredTranslations.fieldNames().isEmpty()
      ? new JsonObject()
      : filteredTranslations.getJsonObject(
        filteredTranslations.fieldNames().iterator().next(),
        new JsonObject()
      );

    return application
      .getJsonArray(ADDITIONAL_INFORMATION, new JsonArray())
      .stream()
      .map(JsonObject.class::cast)
      .filter(info -> catalogId.equalsIgnoreCase(info.getString("catalog_id")))
      .map(info ->
        info.put("translation", expandAdditionalInformationFromObject(singleLangTranslations))
      )
      .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
  }


  /**
   * Retrieves an existing child of {@code parent} with the given {@code key} and {@code type}, or
   * creates and puts a new instance using {@code supplier} when absent or of a different type.
   */
  @SuppressWarnings("unchecked")
  private <T> T getOrCreate(JsonObject parent, String key, Class<T> type,
    java.util.function.Supplier<T> supplier) {
    Object existing = parent.getValue(key);
    if (type.isInstance(existing)) {
      return (T) existing;
    }
    T created = supplier.get();
    parent.put(key, created);
    return created;
  }

  /**
   * Expands a flattened JsonArray of additional information into a nested JsonObject.
   *
   * @return A nested JsonObject representing the expanded additional information.
   */
  private JsonObject expandAdditionalInformationFromObject(@NotNull JsonObject input) {
    JsonObject root = new JsonObject();

    input.forEach(entry -> {
      String field = entry.getKey();
      String comment = (String) entry.getValue();

      String[] parts = field.split("\\.");
      JsonObject current = root;
      boolean skipNext = false;

      for (int i = 0; i < parts.length; i++) {
        if (skipNext) {
          skipNext = false;
          continue;
        }

        String part = parts[i];

        if (i == parts.length - 1) {
          current.put(part, comment);
        } else if (i + 1 < parts.length && parts[i + 1].matches("\\d+")) {
          int index = Integer.parseInt(parts[i + 1]);

          JsonArray array = getOrCreate(current, part, JsonArray.class, JsonArray::new);
          while (array.size() <= index) {
            array.add(new JsonObject());
          }

          current = array.getJsonObject(index);
          skipNext = true;
        } else {
          current = getOrCreate(current, part, JsonObject.class, JsonObject::new);
        }
      }
    });

    return root;
  }

}
