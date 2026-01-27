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

import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE;
import static eu.europa.data.health.access.Constants.CATALOG_IDS;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATA_PERMIT_ID;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_RESPONSE_STATUS;
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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.collections4.IterableUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherManager extends CommonApplicationManager {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;

  public DispatcherManager(DBHandler dbHandler, MongoClient mongoClient,
    BasketManager basketManager, HubStoreClient hubStoreClient, Vertx vertx,
    AuditLogManager auditLogManager, HubTranslationClient hubTranslationClient,
    HubSearchClient hubSearchClient, AuthClient authClient) {
    super(dbHandler, basketManager, mongoClient, vertx, hubStoreClient, auditLogManager,
      hubTranslationClient, hubSearchClient, authClient);
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
    String decisionFileBase64 = decision.getString("decision_file");

    super.changeDatasetStatus(applicationId, catalogId, datasetId, datasetStatus, decisionFileBase64, collectionName, context, false);

  }


  public void updateStatusFromDispatcher(@NotNull RoutingContext context) {

    String applicationId = context.body().asJsonObject().getString("objectId");
    JsonObject payload = context.body().asJsonObject().getJsonObject("payload");
    String catalogId = payload.getString("catalog_id");
    String status = payload.getString("status");
    String collectionName = payload.getString("application_type");
    JsonObject statusPayload = payload.getJsonObject("statusPayload");

    super.changeCatalogStatus(applicationId, catalogId, status, statusPayload, collectionName, context, false);
  }

  public void updateApplicationStatusFromDispatcher(@NotNull RoutingContext context) {

    JsonArray requestBody = context.body().asJsonArray();
    JsonArray dispatcherResponse = generateDispatcherResponse(requestBody);

    dispatcherResponse.forEach(rec -> {
      JsonObject dispatcherRecord = (JsonObject) rec;
      dispatcherRecord.put("latest_update_date", getCurrentTimestamp());

      JsonArray catalogIds = dispatcherRecord.getJsonArray(CATALOG_IDS);
      JsonObject filter = new JsonObject().put("configuration.dispatcher_response",
        new JsonObject().put("$elemMatch",
          new JsonObject().put(APPLICATION_ID, dispatcherRecord.getString(APPLICATION_ID))
            .put(DATA_PERMIT_ID, dispatcherRecord.getString(DATA_PERMIT_ID))
            .put(CATALOG_IDS, new JsonObject().put("$all", catalogIds))));

      JsonObject update = new JsonObject().put("$set",
        new JsonObject().put("configuration.dispatcher_response.$", dispatcherRecord));
      mongoClient.findOneAndUpdateWithOptions(DISPATCHER_COLLECTION, filter, update,
        new FindOptions(), new UpdateOptions().setReturningNewDocument(true), res -> {
          if (res.succeeded() && res.result() != null) {
            // Update application collections.
            updateDispatcherStatusInApplication(res.result(), null);
            // If update succeeded and matched a document, the update is done
            log.info(DISPATCHER_COLLECTION + " dispatcher_response record updated: {}",
              dispatcherRecord);
          } else {
            // If no document was matched, perform an insert operation to add the new element to the array
            JsonObject insertFilter = new JsonObject().put("_id",
              new JsonObject().put("$exists", true)); // Match any document

            JsonObject insertUpdate = new JsonObject().put("$push",
              new JsonObject().put("configuration.dispatcher_response", dispatcherRecord));

            mongoClient.updateCollectionWithOptions(DISPATCHER_COLLECTION, insertFilter,
              insertUpdate, new UpdateOptions().setUpsert(true), insertRes -> {
                if (insertRes.succeeded()) {
                  // Update application collections.
                  updateDispatcherStatusInApplication(JsonObject.mapFrom(insertRes.result()), null);

                  log.info(DISPATCHER_COLLECTION + " dispatcher_response record inserted: {}",
                    dispatcherRecord);
                } else {
                  log.error(DISPATCHER_COLLECTION
                      + " dispatcher_response record failed with error message: {}",
                    insertRes.cause().getMessage());
                }
              });
          }
        });
    });
    setUpMessageContextResponse(context, HttpResponseStatus.OK, "Update procedure completed");
  }

  public void updateDispatcherStatusInApplication(JsonObject mongoDispatcher,
    DispatcherResponseStatus specificStatus) {
    JsonObject mongoApplicationForm = mongoDispatcher.getJsonObject("application");
    String applicationId = mongoApplicationForm.getString("_id");
    JsonObject applicationQuery = new JsonObject().put("_id", applicationId);
    String collectionName = mongoDispatcher.getJsonObject("configuration")
      .getString(APPLICATION_TYPE);
    mongoClient.findOne(collectionName, applicationQuery, null, asyncAppResult -> {
      if (asyncAppResult.result() != null) {
        JsonObject mongoApplication = asyncAppResult.result();
        if (specificStatus != null) {
          JsonArray mongoDatasets = mongoApplication.getJsonArray(DATASETS);
          if (mongoDatasets != null) {
            mongoDatasets.forEach(mongoDatasetsObj -> {
              if (mongoDatasetsObj instanceof JsonObject mongoDatasetObject) {
                mongoDatasetObject.put(DISPATCHER_RESPONSE_STATUS,
                  specificStatus.getDispatcherResponseStatus());
              }
            });
          }
        } else {
          JsonObject mongoConfiguration = mongoDispatcher.getJsonObject("configuration");
          JsonArray mongoDispatcherResponse = mongoConfiguration.getJsonArray(
            "dispatcher_response");

          if (mongoDispatcherResponse != null) {
            mongoDispatcherResponse.forEach(dispatcherResponseObj -> {
              if (dispatcherResponseObj instanceof JsonObject mongoDispatcherResponseObject) {
                JsonArray catalogIds = mongoDispatcherResponseObject.getJsonArray(CATALOG_IDS);
                String dispatcherResponseStatus = mongoDispatcherResponseObject.getString(STATUS);

                // Update dispatcherResponseStatus for datasets of applicationId with catalogId in catalogIds
                prepareDatasets(mongoApplication, catalogIds, dispatcherResponseStatus);

                // If status is DOMIBUS_NATIONAL_RECEIVED, update sent catalogs to submitted
                if (DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus()
                  .equals(dispatcherResponseStatus)) {
                  updateSentCatalogs(mongoApplication, catalogIds, collectionName);
                }
              }
            });
          }
        }
      } else {
        log.error(
          "{} Dispatcher response status failed to update for application's datasets. Application with id: {} not found",
          collectionName, applicationId);
      }
    });
  }

  private void prepareDatasets(JsonObject mongoApplication, JsonArray catalogIds,
    String dispatcherResponseStatus) {
    if (mongoApplication != null) {
      JsonArray mongoDatasets = mongoApplication.getJsonArray(DATASETS);
      if (mongoDatasets != null) {
        mongoDatasets.forEach(mongoDatasetsObj -> {
          if (mongoDatasetsObj instanceof JsonObject mongoDatasetObject) {
            String datasetCatalogId = mongoDatasetObject.getString("catalog_id");
            if (catalogIds != null && catalogIds.contains(datasetCatalogId)) {
              mongoDatasetObject.put(DISPATCHER_RESPONSE_STATUS, dispatcherResponseStatus);
            }
          }
        });
      }
    }
  }

  /**
   * Updates the status of catalogs with latest status SENT to SUBMITTED.
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

    catalogIds
      .stream()
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .forEach(catalogId -> {
        if (CatalogStatus.SENT.getLabel().equals(getLatestCatalogStatus(application, catalogId))) {
          changeCatalogToSubmitted(application, catalogId, collection);
        }
      });
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

    JsonArray dispatcherPermits = payload.getJsonArray("permits");
    dispatcherPermits.forEach(dispatcherPermitObj -> {

      JsonObject permitObject = (JsonObject) dispatcherPermitObj;

      String dispatcherDatasetId = permitObject.getString("dataset_id");
      String dispatcherPermitFile = permitObject.getString("permit_file");
      String permitStatus = permitObject.getString("permit_status");
      String reason = permitObject.getString("reason");

      changePermitStatus(applicationId, catalogId, dispatcherDatasetId, permitStatus, reason,
        dispatcherPermitFile, context, false);

    });
  }

  private Future<JsonObject> findDispatcherApplicationByIdAndVersion(String applicationId, Integer version) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject query = new JsonObject()
      .put("application._id", applicationId)
      .put("application.version", version);

    mongoClient.findOne(DISPATCHER_COLLECTION, query, null)
      .onSuccess(promise::complete)
      .onFailure(promise::fail);

    return promise.future();
  }

  private Future<Integer> findDispatcherApplicationById(String applicationId) {
    Promise<Integer> promise = Promise.promise();

    JsonObject query = new JsonObject()
      .put("application._id", applicationId);

    mongoClient.count(DISPATCHER_COLLECTION, query)
      .onSuccess(count -> {promise.complete(count.intValue());})
      .onFailure(promise::fail);

    return promise.future();
  }

}
