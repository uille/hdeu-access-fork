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
import static eu.europa.data.health.access.Constants.APPROVED;
import static eu.europa.data.health.access.Constants.CATALOG_IDS;
import static eu.europa.data.health.access.Constants.DATA_PERMIT_ID;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.PUBLISHER;
import static eu.europa.data.health.access.Constants.REJECTED;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.STATUS_HISTORY;
import static eu.europa.data.health.access.Constants.SUBMITTED;

import eu.europa.data.health.access.DispatcherResponseStatus;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherManager extends AbstractManager {


  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;

  public DispatcherManager(MongoClient mongoClient, DBHandler dbHandler) {
    super(dbHandler);
    this.mongoClient = mongoClient;
  }

  public void updateDatasetStatusFromDispatcher(@NotNull RoutingContext context) {

    String applicationId = context.body().asJsonObject().getString("objectId");
    JsonObject payload = context.body().asJsonObject().getJsonObject("payload");
    String catalogId = payload.getString("catalogId");
    String collectionName = payload.getString("applicationType");

    JsonObject query = new JsonObject().put("_id", applicationId);
    mongoClient.findOne(collectionName, query, null, res -> {
      if (res.succeeded()) {
        if (res.result() != null) {

          JsonObject mongoApplication = res.result();

          String mongoApplicationStatus = mongoApplication.getJsonObject("status")
            .getString("status");
          if (SUBMITTED.equalsIgnoreCase(mongoApplicationStatus)) {

            JsonArray mongoDatasets = mongoApplication.getJsonArray("datasets");
            if (mongoDatasets != null && !mongoDatasets.isEmpty()) {

              AtomicBoolean atLeastOneApproved = new AtomicBoolean(false);
              AtomicBoolean atLeastOneRejected = new AtomicBoolean(false);

              JsonArray dispatcherDecisions = payload.getJsonArray("decisions");
              dispatcherDecisions.forEach(dispatcherDecisionObj -> {

                JsonObject decisionObject = (JsonObject) dispatcherDecisionObj;
                String dispatcherPublisher = decisionObject.getString(PUBLISHER);
                String dispatcherDecision = decisionObject.getString("decision");

                modifyPublishersBasedOnDispatcherDecisions(mongoDatasets, dispatcherPublisher, dispatcherDecision, catalogId, atLeastOneApproved, atLeastOneRejected);
              });

              String applicationStatus = null;
              if(atLeastOneApproved.get()){
                applicationStatus = "APPROVED";
              } else if (atLeastOneRejected.get()){
                applicationStatus = "REJECTED";
              }
              if(applicationStatus != null){

                updateApplicationStatus(mongoApplication, applicationStatus, context);

                replaceApplication(mongoApplication, collectionName)
                  .onSuccess(replaceResult -> setUpMessageContextResponse(context, HttpResponseStatus.OK, "Application status updated successfully"))
                  .onFailure(fail -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage()));
              } else {
                setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
                  "No application status update performed");
              }
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
                "Application does not contain any datasets");
            }
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Application must be in submitted status");
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Application with application id: " + applicationId + " was not found");
        }
      } else {
        setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }
    });
  }

  private void modifyPublishersBasedOnDispatcherDecisions(JsonArray mongoDatasets,String dispatcherPublisher, String dispatcherDecision, String catalogId, AtomicBoolean atLeastOneApproved, AtomicBoolean atLeastOneRejected) {
    for (Object mongoDatasetObj : mongoDatasets) {

      JsonObject mongoDataset = (JsonObject) mongoDatasetObj;
      String mongoDispatcherResponseStatus = mongoDataset.getString(
        "dispatcher_response_status");

      if ("domibus_national_received".equalsIgnoreCase(mongoDispatcherResponseStatus)) {

        String mongoCatalogId = mongoDataset.getString("catalog_id");
        JsonObject mongoPublisher = mongoDataset.getJsonObject("publisher");
        String mongoPublisherName = mongoPublisher.getString("name");

        if (catalogId.equals(mongoCatalogId) &&
          dispatcherPublisher.equalsIgnoreCase(mongoPublisherName)) {

          mongoPublisher.put("decision", dispatcherDecision);
          mongoPublisher.put("decision_date_received", Instant.now());


          if (APPROVED.equalsIgnoreCase(dispatcherDecision)) {
            atLeastOneApproved.set(true);
          }
          if (REJECTED.equalsIgnoreCase(dispatcherDecision)) {
            atLeastOneRejected.set(true);
          }
        }
      } else {
        log.debug(
          "Dataset with id: {} does not have proper dispatcher_response_status", mongoDataset.getString("dataset_id"));
      }
    }
  }

  private void updateApplicationStatus(JsonObject mongoApplication, String applicationStatus, RoutingContext context) {
    // Get current application status.
    JsonObject currentStatus = mongoApplication.getJsonObject(STATUS);

    JsonObject newHistoryStatus = new JsonObject();
    newHistoryStatus.put("status", currentStatus.getString("status"));
    newHistoryStatus.put("date", currentStatus.getString("date"));
    newHistoryStatus.put("userId", currentStatus.getString("userId"));

    // Update the status_history for the application.
    JsonArray statusHistory = new JsonArray();
    if (currentStatus.getJsonArray(STATUS_HISTORY) != null) {
      statusHistory = currentStatus.getJsonArray(STATUS_HISTORY);
    }
    statusHistory.add(newHistoryStatus);

    JsonObject newStatus = new JsonObject();
    newStatus.put("status", applicationStatus);
    newStatus.put("date", Instant.now());
    newStatus.put("userId", "DISPATCHER");
    newStatus.put("status_history", statusHistory);

    mongoApplication.put("status", newStatus);
  }


  public void updateApplicationStatusFromDispatcher(@NotNull RoutingContext context) {

    JsonArray requestBody = context.body().asJsonArray();
    JsonArray dispatcherResponse = generateDispatcherResponse(requestBody);

    dispatcherResponse.forEach(rec -> {
      JsonObject dispatcherRecord = (JsonObject) rec;
      dispatcherRecord.put("latest_update_date", getCurrentTimestamp());

      JsonArray catalogIds = dispatcherRecord.getJsonArray(CATALOG_IDS);
      JsonObject filter = new JsonObject()
        .put("configuration.dispatcher_response", new JsonObject()
          .put("$elemMatch", new JsonObject()
            .put(APPLICATION_ID, dispatcherRecord.getString(APPLICATION_ID))
            .put(DATA_PERMIT_ID, dispatcherRecord.getString(DATA_PERMIT_ID))
            .put(CATALOG_IDS, new JsonObject().put("$all", catalogIds))
          )
        );

      JsonObject update = new JsonObject()
        .put("$set", new JsonObject()
          .put("configuration.dispatcher_response.$", dispatcherRecord)
        );
      mongoClient.findOneAndUpdateWithOptions(
        DISPATCHER_COLLECTION,
        filter,
        update,
        new FindOptions(),
        new UpdateOptions().setReturningNewDocument(true),
        res -> {
          if (res.succeeded() && res.result() != null) {
            // Update application collections.
            updateDispatcherStatusInApplication(res.result(), null);
            // If update succeeded and matched a document, the update is done
            log.info(DISPATCHER_COLLECTION + " dispatcher_response record updated: {}",
              dispatcherRecord);
          } else {
            // If no document was matched, perform an insert operation to add the new element to the array
            JsonObject insertFilter = new JsonObject()
              .put("_id", new JsonObject().put("$exists", true)); // Match any document

            JsonObject insertUpdate = new JsonObject()
              .put("$push", new JsonObject()
                .put("configuration.dispatcher_response", dispatcherRecord)
              );

            mongoClient.updateCollectionWithOptions(DISPATCHER_COLLECTION, insertFilter, insertUpdate,
              new UpdateOptions().setUpsert(true), insertRes -> {
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
              }
            );
          }
        }
      );
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
          JsonArray mongoDatasets = mongoApplication.getJsonArray("datasets");
          if (mongoDatasets != null) {
            mongoDatasets.forEach(mongoDatasetsObj -> {
              if (mongoDatasetsObj instanceof JsonObject mongoDatasetObject) {
                mongoDatasetObject.put("dispatcher_response_status",
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

              }
            });
          }
        }
        updateApplication(mongoApplication, applicationQuery, collectionName);
      }
    });
  }

  private void prepareDatasets(JsonObject mongoApplication, JsonArray catalogIds,
    String dispatcherResponseStatus) {
    if (mongoApplication != null) {
      JsonArray mongoDatasets = mongoApplication.getJsonArray("datasets");
      if (mongoDatasets != null) {
        mongoDatasets.forEach(mongoDatasetsObj -> {
          if (mongoDatasetsObj instanceof JsonObject mongoDatasetObject) {
            String datasetCatalogId = mongoDatasetObject.getString("catalog_id");
            if (catalogIds != null && catalogIds.contains(datasetCatalogId)) {
              mongoDatasetObject.put("dispatcher_response_status", dispatcherResponseStatus);
            }
          }
        });
      }
    }
  }

  public void updateApplication(JsonObject mongoApplication, JsonObject applicationQuery,
    String collectionName) {
    if (mongoApplication != null) {
      mongoClient.findOneAndReplace(collectionName, applicationQuery, mongoApplication,
        datasetUpdated -> {
          if (datasetUpdated.succeeded()) {
            log.info(DISPATCHER_COLLECTION
              + " Dispatcher response status updated successfully for application's datasets");
          } else {
            log.info(DISPATCHER_COLLECTION
              + " Dispatcher response status failed to update for application's datasets");
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

  private Future<JsonObject> replaceApplication(JsonObject mongoApplication, String collectionName) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject query = new JsonObject().put(ID, mongoApplication.getValue(ID));
    mongoApplication.put(DATE_MODIFIED, Instant.now());
    mongoClient.findOneAndReplace(collectionName, query, mongoApplication,
      result -> {
        if (result.succeeded()) {
          log.info("{} Application with id: {} was updated successfully", collectionName,
            mongoApplication.getValue(ID));
          promise.complete(result.result());
        } else {
          log.info("{} Failed to replace application with id: {}", collectionName,
            mongoApplication.getValue(ID));
          promise.fail(result.cause());
        }
      });
    return promise.future();
  }

}
