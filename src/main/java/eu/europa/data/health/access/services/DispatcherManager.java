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
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE;
import static eu.europa.data.health.access.Constants.CATALOG_IDS;
import static eu.europa.data.health.access.Constants.DATA_PERMIT_ID;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.STATUS;

import eu.europa.data.health.access.CatalogStatus;
import eu.europa.data.health.access.DispatcherResponseStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherManager extends CommonApplicationManager {

  private static final String APP_ID = "application._id";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;

  public DispatcherManager(DBHandler dbHandler, MongoClient mongoClient,
    BasketManager basketManager, HubStoreClient hubStoreClient, Vertx vertx,
    AuditLogManager auditLogManager, NotificationManager notificationManager,
    HubTranslationClient hubTranslationClient, HubSearchClient hubSearchClient,
    AuthClient authClient, JsonObject config) {
    super(dbHandler, basketManager, mongoClient, vertx, hubStoreClient, auditLogManager,
      notificationManager, hubTranslationClient, hubSearchClient, authClient, config);
    this.mongoClient = mongoClient;
  }

  public void getDispatcherApplicationByIdAndVersion(RoutingContext ctx) {
    String applicationId = ctx.pathParam("applicationId");
    String versionParam = ctx.pathParam("version");

    Integer version;
    try {
      version = Integer.valueOf(versionParam);
    } catch (NumberFormatException e) {
      ctx.response()
        .setStatusCode(400)
        .end(new JsonObject().put("error", "version must be an integer").encodePrettily());
      return;
    }

    findDispatcherApplicationByIdAndVersion(applicationId, version)
      .onSuccess(result -> {
        if (result == null) {
          ctx.response()
            .setStatusCode(404)
            .end(new JsonObject().put("error", "Application not found").encodePrettily());
        } else {
          ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(result.getJsonObject("application").encodePrettily());
        }
      })
      .onFailure(err -> {
        ctx.response()
          .setStatusCode(500)
          .end(new JsonObject().put("error", err.getMessage()).encodePrettily());
      });
  }

  public void getDispatcherApplicationById(RoutingContext ctx) {
    String applicationId = ctx.pathParam("applicationId");

    findDispatcherApplicationById(applicationId)
      .onSuccess(count -> {
        JsonObject responseJson = new JsonObject().put("count", count);

        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(responseJson.encodePrettily());
      })
      .onFailure(err -> {
        ctx.response()
          .setStatusCode(500)
          .end(new JsonObject().put("error", err.getMessage()).encodePrettily());
      });
  }

  public void updateDatasetDecisionFromDispatcher(@NotNull RoutingContext context) {
    JsonObject body = context.body().asJsonObject();
    String applicationId = body.getString("objectId");
    JsonObject payload = body.getJsonObject("payload");
    String catalogId = payload.getString("catalog_id");
    String collectionName = payload.getString("application_type");
    JsonArray decisions = payload.getJsonArray("decisions");
    JsonObject decision = decisions.getJsonObject(0);
    String datasetId = decision.getString("dataset_id");
    String datasetStatus = decision.getString("decision");
    String decisionReason = decision.getString("decision_reason");
    String decisionFileBase64 = decision.getString("decision_file");
    String supportFileBase64 = decision.getString("support_file");

    uploadDecisionFileIfPresent(decisionFileBase64, "decision_file.pdf", applicationId)
      .compose(decisionFileId -> {
        decision.put("decision_file", decisionFileId);

        return uploadDecisionFileIfPresent(supportFileBase64, "support_file.pdf", applicationId)
          .compose(supportFileId -> {
            decision.put("support_file", supportFileId);

            return createDecisionTranslatedFieldsReference(
              collectionName, applicationId, datasetId, decision)
              .compose(translatedFields ->
                super.changeDatasetStatus(
                  applicationId,
                  catalogId,
                  datasetId,
                  datasetStatus,
                  decisionFileId,
                  collectionName,
                  context,
                  false,
                  decisionReason,
                  supportFileId,
                  translatedFields
                )
              );
          });
      })
      .onSuccess(res -> {
        if (!context.response().ended()) {
          context.response().setStatusCode(200).end();
        }
      })
      .onFailure(err -> {
          if (!context.response().ended()) {
            setUpMessageContextResponse(
              context,
              HttpResponseStatus.INTERNAL_SERVER_ERROR,
              err.getMessage()
            );
          }});
  }

  /**
   * Uploads a base64-encoded file to HubStoreClient and returns the stored file ID.
   * Returns {@code null} (as a succeeded future) if the content is blank or HubStoreClient is unavailable.
   *
   * @param base64Content   Base64-encoded file content
   * @param fileName        Name to assign to the uploaded file
   * @param applicationId   Application ID used as the parameterId in the store
   * @return Future containing the file ID on success, or {@code null} if nothing to upload
   */
  protected Future<String> uploadDecisionFileIfPresent(String base64Content, String fileName,
    String applicationId) {
    if (StringUtils.isBlank(base64Content)) {
      return Future.succeededFuture(null);
    }
    HubStoreClient client = getHubStoreClient();
    if (client == null) {
      log.warn("HubStoreClient is not available, skipping file upload for: {}", fileName);
      return Future.succeededFuture(null);
    }
    try {
      byte[] fileBytes = Base64.getMimeDecoder().decode(base64Content);
      Buffer fileBuffer = Buffer.buffer(fileBytes);
      return client.uploadFile(APPLICATIONID, applicationId, fileName, fileBuffer, "application/pdf")
        .map(response -> response.getJsonObject("result").getString("id"))
        .onFailure(err -> log.error("Failed to upload file {} for application {}: {}",
          fileName, applicationId, err.getMessage()));
    } catch (Exception e) {
      log.error("Failed to decode/upload file {} for application {}: {}",
        fileName, applicationId, e.getMessage());
      return Future.failedFuture(e);
    }
  }


  public void updateStatusFromDispatcher(@NotNull RoutingContext context) {

    String applicationId = context.body().asJsonObject().getString("objectId");
    JsonObject payload = context.body().asJsonObject().getJsonObject("payload");
    String catalogId = payload.getString("catalog_id");
    String status = payload.getString("status");
    String collectionName = payload.getString("application_type");
    JsonObject statusPayload = payload.getJsonObject("statusPayload");

    createStatusPayloadTranslationState(collectionName, applicationId, catalogId, statusPayload)
      .onSuccess(translatedStatusPayload -> super.changeCatalogStatus(applicationId, catalogId,
        status, translatedStatusPayload, collectionName, context, false))
      .onFailure(err -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, err.getMessage()));
  }

  public void updateApplicationStatusFromDispatcher(@NotNull RoutingContext context) {

    JsonArray requestBody = context.body().asJsonArray();
    JsonArray dispatcherResponse = generateDispatcherResponse(requestBody);

    // Collect all catalog_ids from the batch to process them together after all
    // DISPATCHER_COLLECTION updates complete. This prevents a race condition where
    // concurrent calls to updateDispatcherStatusInApplication for the same application
    // read-modify-save the application document, causing lost updates.
    JsonArray allUpdatedCatalogIds = new JsonArray();
    List<Future<JsonObject>> dispatcherUpdates = new ArrayList<>();

    dispatcherResponse.forEach(rec -> {
      JsonObject dispatcherRecord = (JsonObject) rec;
      dispatcherRecord.put("latest_update_date", getCurrentTimestamp());

      JsonArray catalogIds = dispatcherRecord.getJsonArray(CATALOG_IDS);
      if (catalogIds != null) {
        catalogIds.forEach(allUpdatedCatalogIds::add);
      }

      JsonObject filter = new JsonObject().put("configuration.dispatcher_response",
        new JsonObject().put("$elemMatch",
          new JsonObject().put(APPLICATION_ID, dispatcherRecord.getString(APPLICATION_ID))
            .put(DATA_PERMIT_ID, dispatcherRecord.getString(DATA_PERMIT_ID))
            .put(CATALOG_IDS, new JsonObject().put("$all", catalogIds))));

      JsonObject update = new JsonObject().put("$set",
        new JsonObject().put("configuration.dispatcher_response.$", dispatcherRecord));

      Promise<JsonObject> updatePromise = Promise.promise();
      dispatcherUpdates.add(updatePromise.future());

      mongoClient.findOneAndUpdateWithOptions(DISPATCHER_COLLECTION, filter, update,
        new FindOptions(), new UpdateOptions().setReturningNewDocument(true), res -> {
          if (res.succeeded() && res.result() != null) {
            log.info(DISPATCHER_COLLECTION + " dispatcher_response record updated: {}",
              dispatcherRecord);
            updatePromise.complete(res.result());
          } else {
            // If no document was matched, perform an insert operation to add the new element to the array
            JsonObject insertFilter = new JsonObject().put("_id",
              new JsonObject().put("$exists", true)); // Match any document

            JsonObject insertUpdate = new JsonObject().put("$push",
              new JsonObject().put("configuration.dispatcher_response", dispatcherRecord));

            mongoClient.updateCollectionWithOptions(DISPATCHER_COLLECTION, insertFilter,
              insertUpdate, new UpdateOptions().setUpsert(true), insertRes -> {
                if (insertRes.succeeded()) {
                  log.info(DISPATCHER_COLLECTION + " dispatcher_response record inserted: {}",
                    dispatcherRecord);
                  updatePromise.complete(JsonObject.mapFrom(insertRes.result()));
                } else {
                  log.error(DISPATCHER_COLLECTION
                      + " dispatcher_response record failed with error message: {}",
                    insertRes.cause().getMessage());
                  updatePromise.fail(insertRes.cause());
                }
              });
          }
        });
    });

    // After all DISPATCHER_COLLECTION updates complete, call updateDispatcherStatusInApplication
    // ONCE with all catalog_ids combined. This avoids concurrent read-modify-save on the same
    // application document which would cause lost updates (only one catalog getting SUBMITTED).
    Future.all(new ArrayList<>(dispatcherUpdates))
      .onSuccess(cf -> {
        // Use the first successful result as the dispatcher document (all belong to the same application)
        JsonObject dispatcherDoc = null;
        for (int i = 0; i < cf.size(); i++) {
          JsonObject result = cf.resultAt(i);
          if (result != null && result.getJsonObject("application") != null) {
            dispatcherDoc = result;
            break;
          }
        }
        if (dispatcherDoc != null) {
          // Build a combined updatedRecord with all catalog_ids and a representative status
          String combinedStatus = dispatcherResponse.getJsonObject(0).getString(STATUS);
          JsonObject combinedUpdatedRecord = new JsonObject()
            .put(CATALOG_IDS, allUpdatedCatalogIds)
            .put(STATUS, combinedStatus);
          updateDispatcherStatusInApplication(dispatcherDoc, combinedUpdatedRecord);
        } else {
          // All records were newly inserted (upsert path) - fetch the full dispatcher document
          String applicationId = dispatcherResponse.getJsonObject(0).getString(APPLICATION_ID);
          JsonObject findFilter = new JsonObject().put(APP_ID, applicationId);
          mongoClient.findOne(DISPATCHER_COLLECTION, findFilter, null, findRes -> {
            if (findRes.succeeded() && findRes.result() != null) {
              String combinedStatus = dispatcherResponse.getJsonObject(0).getString(STATUS);
              JsonObject combinedUpdatedRecord = new JsonObject()
                .put(CATALOG_IDS, allUpdatedCatalogIds)
                .put(STATUS, combinedStatus);
              updateDispatcherStatusInApplication(findRes.result(), combinedUpdatedRecord);
            } else {
              log.error("Could not find dispatcher document for application: {}", applicationId);
            }
          });
        }
      })
      .onFailure(err ->
        log.error("Failed to complete all dispatcher_response updates: {}", err.getMessage())
      );

    setUpMessageContextResponse(context, HttpResponseStatus.OK, "Update procedure completed");
  }

  public void updateDispatcherStatusInApplication(
    JsonObject mongoDispatcher,
    JsonObject updatedRecord
  ) {
    JsonObject mongoApplicationForm = mongoDispatcher.getJsonObject("application");
    String applicationId = mongoApplicationForm.getString("_id");
    JsonObject applicationQuery = new JsonObject().put("_id", applicationId);
    String collectionName = mongoDispatcher.getJsonObject("configuration")
      .getString(APPLICATION_TYPE);
    mongoClient.findOne(collectionName, applicationQuery, null, asyncAppResult -> {
      if (asyncAppResult.result() != null) {
        JsonObject mongoApplication = asyncAppResult.result();
        JsonObject mongoConfiguration = mongoDispatcher.getJsonObject("configuration");
        JsonArray mongoDispatcherResponse = mongoConfiguration.getJsonArray(
          "dispatcher_response");

        // Only trigger updateSentCatalogs for the specific record that was just updated/inserted,
        // not for all dispatcher_response entries. This prevents duplicate SUBMITTED activities.
        if (updatedRecord != null) {
          String updatedStatus = updatedRecord.getString(STATUS);
          if (
            DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus().equals(
              updatedStatus
            )
          ) {
            JsonArray updatedCatalogIds = updatedRecord.getJsonArray(CATALOG_IDS);
            updateSentCatalogs(mongoApplication, updatedCatalogIds, collectionName);
          }
        } else if (mongoDispatcherResponse != null) {
          // Backward compatibility: when updatedRecord is null (called from DispatcherScheduledJob),
          // iterate all entries as before
          mongoDispatcherResponse.forEach(dispatcherResponseObj -> {
            if (dispatcherResponseObj instanceof JsonObject mongoDispatcherResponseObject) {
              JsonArray catalogIds = mongoDispatcherResponseObject.getJsonArray(CATALOG_IDS);
              String dispatcherResponseStatus = mongoDispatcherResponseObject.getString(STATUS);
              if (
                DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus().equals(
                  dispatcherResponseStatus
                )
              ) {
                updateSentCatalogs(mongoApplication, catalogIds, collectionName);
              }
            }
          });
        }
      } else {
        log.error(
          "{} Dispatcher response status failed to update for application's datasets. Application with id: {} not found",
          collectionName, applicationId);
      }
    });
  }

  /**
   * Updates the status of catalogs with latest status SENT to SUBMITTED.
   * Processes catalogs sequentially to avoid concurrent saves of the same application document
   * which could cause lost updates (only one catalog getting SUBMITTED persisted).
   *
   * @param application The application JSON object.
   * @param catalogIds  The catalog IDs to check and update.
   * @param collection  The collection where the application is stored.
   */
  private void updateSentCatalogs(
    @NotNull JsonObject application,
    @NotNull JsonArray catalogIds,
    String collection
  ) {
    JsonArray catalogs = application.getJsonArray("catalogs");
    if (IterableUtils.isEmpty(catalogs)) {
      return;
    }

    List<String> catalogsToSubmit = catalogIds
      .stream()
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .filter(catalogId ->
        CatalogStatus.SENT.getLabel().equals(getLatestCatalogStatus(application, catalogId))
      )
      .toList();

    // Chain changeCatalogToSubmitted calls sequentially so each save completes
    // before the next starts. This prevents out-of-order saves from overwriting each other.
    Future<Void> chain = Future.succeededFuture();
    for (String catalogId : catalogsToSubmit) {
      chain = chain.compose(v -> changeCatalogToSubmitted(application, catalogId, collection));
    }
  }

  public void updateApplication(JsonObject mongoApplication, JsonObject applicationQuery,
    String collectionName) {
    if (mongoApplication != null) {
      mongoClient.findOneAndReplace(collectionName, applicationQuery, mongoApplication,
        datasetUpdated -> {
          if (datasetUpdated.succeeded()) {
            log.info(
              "{} Dispatcher response status updated successfully for application's datasets",
              collectionName);
          } else {
            log.error("{} Dispatcher response status failed to update for application's datasets",
              collectionName);
          }
        });
    }
  }

  private JsonArray generateDispatcherResponse(JsonArray requestBody) {

    JsonArray dispatcherResults = new JsonArray();

    for (int i = 0; i < requestBody.size(); i++) {
      JsonObject jsonObject = requestBody.getJsonObject(i);

      String applicationId = jsonObject.getString(APPLICATION_ID);
      String dataPermitId = jsonObject.getString(DATA_PERMIT_ID);
      String status = jsonObject.getString(STATUS);
      JsonArray catalogIdsArray = jsonObject.getJsonArray(CATALOG_IDS);
      JsonObject dispatcherResult = new JsonObject().put(APPLICATION_ID, applicationId)
        .put(DATA_PERMIT_ID, dataPermitId).put(CATALOG_IDS, catalogIdsArray).put(STATUS, status);
      dispatcherResults.add(dispatcherResult);
    }
    return dispatcherResults;
  }

  private String getCurrentTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }

  public void updatePermitFromDispatcher(RoutingContext context) {

    String applicationId = context.body().asJsonObject().getString("objectId");
    JsonObject payload = context.body().asJsonObject().getJsonObject("payload");
    String catalogId = payload.getString("catalog_id");
    String collectionName = payload.getString("application_type");

    JsonArray dispatcherPermits = payload.getJsonArray("permits");
    dispatcherPermits.forEach(dispatcherPermitObj -> {

      JsonObject permitObject = (JsonObject) dispatcherPermitObj;

      String dispatcherDatasetId = permitObject.getString("dataset_id");
      String permitStatus = permitObject.getString("permit_status");
      String reason = permitObject.getString("reason");
      String rejectionReason = permitObject.getString("rejection_reason");
      String permitStartDate = permitObject.getString("permit_start_date");
      String permitExpirationDate = permitObject.getString("permit_expiration_date");
      String permitReferenceNumber = permitObject.getString("permit_reference_number");
      String permitContactDetails = permitObject.getString("permit_contact_details");

      // Upload all file fields and store IDs back on permitObject before passing downstream
      uploadDecisionFileIfPresent(permitObject.getString("permit_file"), "permit_file.pdf", applicationId)
        .compose(permitFileId -> {
          permitObject.put("permit_file", permitFileId);
          return uploadDecisionFileIfPresent(permitObject.getString("renew_amend_file"), "renew_amend_file.pdf", applicationId);
        })
        .compose(renewAmendFileId -> {
          permitObject.put("renew_amend_file", renewAmendFileId);
          return uploadDecisionFileIfPresent(permitObject.getString("rejection_file"), "rejection_file.pdf", applicationId);
        })
        .compose(rejectionFileId -> {
          permitObject.put("rejection_file", rejectionFileId);
          return uploadDecisionFileIfPresent(permitObject.getString("revocation_file"), "revocation_file.pdf", applicationId);
        })
        .compose(revocationFileId -> {
          permitObject.put("revocation_file", revocationFileId);
          return createPermitTranslatedFieldsReference(collectionName, applicationId,
            dispatcherDatasetId, permitObject);
        })
        .onSuccess(translatedFields -> changePermitStatus(applicationId, catalogId,
          dispatcherDatasetId, permitStatus, reason,
          permitObject.getString("permit_file"),
          permitObject.getString("renew_amend_file"),
          rejectionReason,
          permitObject.getString("rejection_file"),
          permitObject.getString("revocation_file"),
          permitStartDate, permitExpirationDate, permitReferenceNumber, permitContactDetails,
          context, false, translatedFields))
        .onFailure(err -> setUpMessageContextResponse(context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR, err.getMessage()));

    });
  }

  protected Future<JsonObject> createDecisionTranslatedFieldsReference(String collectionName,
    String applicationId, String datasetId, JsonObject decision) {
    if (getDecisionTranslationService() == null) {
      return Future.succeededFuture(decision.getJsonObject("translatedFields"));
    }

    boolean hasTranslatableContent = StringUtils.isNotBlank(decision.getString("decision_file"))
      || StringUtils.isNotBlank(decision.getString("support_file"))
      || StringUtils.isNotBlank(decision.getString("decision_reason"));

    return Future.succeededFuture(hasTranslatableContent
      ? getDecisionTranslationService().createTranslatedFieldsReference(UUID.randomUUID().toString())
      : decision.getJsonObject("translatedFields"));
  }

  protected Future<JsonObject> createPermitTranslatedFieldsReference(String collectionName,
    String applicationId, String datasetId, JsonObject permit) {
    if (getDecisionTranslationService() == null) {
      return Future.succeededFuture(permit.getJsonObject("translatedFields"));
    }

    boolean hasTranslatableContent = StringUtils.isNotBlank(permit.getString("permit_file"))
      || StringUtils.isNotBlank(permit.getString("permit_contact_details"));

    return Future.succeededFuture(hasTranslatableContent
      ? getDecisionTranslationService().createTranslatedFieldsReference(UUID.randomUUID().toString())
      : permit.getJsonObject("translatedFields"));
  }

  protected Future<JsonObject> createStatusPayloadTranslationState(String collectionName,
    String applicationId, String catalogId, JsonObject statusPayload) {
    if (statusPayload == null || getDecisionTranslationService() == null) {
      return Future.succeededFuture(statusPayload);
    }

    JsonObject payloadCopy = statusPayload.copy();
    boolean hasTranslatableContent = StringUtils.isNotBlank(payloadCopy.getString("reason"))
      || !payloadCopy.getJsonObject("additionalInformation", new JsonObject())
      .getJsonArray("source", new JsonArray()).isEmpty();

    if (hasTranslatableContent) {
      payloadCopy.put("translatedFields",
        getDecisionTranslationService().createTranslatedFieldsReference(UUID.randomUUID().toString()));
    }

    return Future.succeededFuture(payloadCopy);
  }

  private Future<JsonObject> findDispatcherApplicationByIdAndVersion(String applicationId, Integer version) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject query = new JsonObject()
      .put(APP_ID, applicationId)
      .put("application.version", version);

    mongoClient.findOne(DISPATCHER_COLLECTION, query, null)
      .onSuccess(promise::complete)
      .onFailure(promise::fail);

    return promise.future();
  }

  private Future<Integer> findDispatcherApplicationById(String applicationId) {
    Promise<Integer> promise = Promise.promise();

    JsonObject query = new JsonObject()
      .put(APP_ID, applicationId);

    mongoClient.count(DISPATCHER_COLLECTION, query)
      .onSuccess(count -> {promise.complete(count.intValue());})
      .onFailure(promise::fail);

    return promise.future();
  }

}
