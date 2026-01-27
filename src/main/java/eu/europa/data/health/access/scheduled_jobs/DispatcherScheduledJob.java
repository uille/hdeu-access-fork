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

import static eu.europa.data.health.access.Constants.COMPLETED;
import static eu.europa.data.health.access.Constants.CONFIGURATION_APPLICATION_TYPE;
import static eu.europa.data.health.access.Constants.CONFIGURATION_RETRIES;
import static eu.europa.data.health.access.Constants.CONFIGURATION_STATUS;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.FAILED;
import static eu.europa.data.health.access.Constants.IN_PROGRESS;
import static eu.europa.data.health.access.Constants.OPERATION;
import static eu.europa.data.health.access.Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
import static eu.europa.data.health.access.Constants.PENDING;

import eu.europa.data.health.access.DispatcherResponseStatus;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.CommonApplicationManager;
import eu.europa.data.health.access.services.DispatcherManager;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherScheduledJob extends CommonApplicationManager {

  public static final String CONFIGURATION_SYNC_PENDING = "configuration.syncPending";
  private final MongoClient mongoClient;
  private final DispatcherManager dispatcherManager;
  private final Logger log = LoggerFactory.getLogger(getClass());

  public DispatcherScheduledJob(DBHandler dbHandler, MongoClient mongoClient, DispatcherManager dispatcherManager, AuditLogManager auditLogManager) {
    super(dbHandler, mongoClient, auditLogManager);
    this.mongoClient = mongoClient;

    this.dispatcherManager = dispatcherManager;
  }


  public void sendApplicationsToDispatcher(final int dispatcherMaxRetries,
    DispatcherClient dispatcherClient, String applicationType) {

    findPendingDispatcherRecords(applicationType).onComplete(res -> {
      if (res.succeeded()) {
        List<JsonObject> dispatcherRecords = res.result();
        if (dispatcherRecords != null && !dispatcherRecords.isEmpty()) {
          log.info("{} Total number of dispatcher records for application type: {} found: {}",
            DISPATCHER_COLLECTION, applicationType, dispatcherRecords.size());
          sendApplicationsToDispatcher(dispatcherRecords, dispatcherMaxRetries, dispatcherClient);
        } else {
          log.debug(DISPATCHER_COLLECTION + " No dispatcher records found");
        }

      } else {
        log.error(DISPATCHER_COLLECTION
            + " Find pending dispatcher records was not successful, with error message: {}",
          res.cause().getMessage());
      }
    });
  }

  private Future<List<JsonObject>> findPendingDispatcherRecords(String collectionName) {
    Promise<List<JsonObject>> promise = Promise.promise();
    JsonObject queryForPendingDispatcherRecords;
    if (OUTGOING_APPLICATION_ACTIVITIES_COLLECTION.equals(collectionName)) {
      queryForPendingDispatcherRecords = new JsonObject().put(CONFIGURATION_SYNC_PENDING, "1");
      mongoClient.find(collectionName, queryForPendingDispatcherRecords, listAsyncResult -> {
        if (listAsyncResult.succeeded()) {
          log.debug("{} Find pending dispatcher records was successful", collectionName);
          promise.complete(listAsyncResult.result());
        } else {
          log.error(" Failed to find pending {} dispatcher records, with error message: {}",
            collectionName, listAsyncResult.cause().getMessage());
          promise.fail(listAsyncResult.cause());
        }
      });
    } else {
      // in case of create application
      queryForPendingDispatcherRecords = new JsonObject().put(CONFIGURATION_STATUS, PENDING)
        .put(CONFIGURATION_APPLICATION_TYPE, collectionName);
      mongoClient.find(DISPATCHER_COLLECTION, queryForPendingDispatcherRecords, listAsyncResult -> {
        if (listAsyncResult.succeeded()) {
          log.debug("{} Find pending dispatcher records was successful", DISPATCHER_COLLECTION);
          promise.complete(listAsyncResult.result());
        } else {
          log.error(" Failed to find pending {} dispatcher records, with error message: {}",
            DISPATCHER_COLLECTION, listAsyncResult.cause().getMessage());
          promise.fail(listAsyncResult.cause());
        }
      });
    }

    return promise.future();
  }

  private void sendApplicationsToDispatcher(List<JsonObject> dispatcherRecords,
    final int dispatcherMaxRetries, DispatcherClient dispatcherClient) {

    for (JsonObject dispatcherRecord : dispatcherRecords) {
      processDispatcherRecordWithLock(dispatcherRecord, dispatcherMaxRetries, dispatcherClient);
    }
  }

  private void processDispatcherRecordWithLock(JsonObject dispatcherRecord,
    final int dispatcherMaxRetries, DispatcherClient dispatcherClient) {

    String dispatcherRecordId = dispatcherRecord.getString("_id");

    // Try to acquire a lock on the task
    JsonObject query = new JsonObject().put("_id", dispatcherRecordId)
      .put(CONFIGURATION_STATUS, PENDING);
    JsonObject update = new JsonObject().put("$set",
      new JsonObject().put(CONFIGURATION_STATUS, IN_PROGRESS));

    mongoClient.findOneAndUpdate(DISPATCHER_COLLECTION, query, update, res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          log.info(DISPATCHER_COLLECTION + " Lock acquired for dispatcher record with id: {}",
            dispatcherRecordId);

          sendRecordToDispatcher(prepareJsonObjectForDispatch(dispatcherRecord),
            dispatcherClient).onComplete(actionRes -> {
            if (actionRes.succeeded()) {
              markDispatcherRecordAsCompleted(dispatcherRecord, actionRes.result());
            } else {
              log.error(DISPATCHER_COLLECTION
                  + " Send application to dispatcher failed for record with id: {}",
                dispatcherRecordId);
              retrySendApplicationToDispatcher(dispatcherRecordId, dispatcherRecord,
                dispatcherMaxRetries, actionRes.cause().getMessage(), DISPATCHER_COLLECTION);
            }
          });
        } else {
          log.error(
            DISPATCHER_COLLECTION + " Failed to acquire lock for dispatcher record with id: {}",
            dispatcherRecordId);
        }
      } else {
        log.error(DISPATCHER_COLLECTION
            + " Failed to find and update the dispatcher record, with error message: {}",
          res.cause().getMessage());
      }
    });
  }

  private JsonObject prepareJsonObjectForDispatch(JsonObject dispatcherRecord) {
    JsonObject dispatcherRecordCopy = dispatcherRecord.copy();
    dispatcherRecordCopy.remove(TRANSLATION);
    return dispatcherRecordCopy;
  }

  public Future<String> sendRecordToDispatcher(JsonObject dispatcherRecord,
    DispatcherClient dispatcherClient) {

    Promise<String> promise = Promise.promise();

    dispatcherClient.sendApplication(dispatcherRecord).onComplete(actionRes -> {
      if (actionRes.succeeded()) {
        log.info(DISPATCHER_COLLECTION + " Action performed on application");
        promise.complete(actionRes.result());
      } else {
        promise.fail(actionRes.cause().getMessage());
        log.error(actionRes.cause().getMessage());
      }
    });
    return promise.future();
  }

  public void markDispatcherRecordAsCompleted(JsonObject entry, String responseMessageId) {
    JsonObject query = new JsonObject().put("_id", entry.getString("_id"));
    JsonObject update;
    String collectionName;
    if (OperationStatus.CREATE.getLabel().equalsIgnoreCase(entry.getString(OPERATION))
      || OperationStatus.UPDATE.getLabel().equalsIgnoreCase(entry.getString(OPERATION))) {
      update = new JsonObject().put("$set", new JsonObject().put(CONFIGURATION_STATUS, COMPLETED)
        .put("configuration.completion_date", getCurrentTimestamp())
        .put("configuration.dispatcher_response", new JsonArray(responseMessageId)));
      collectionName = DISPATCHER_COLLECTION;
    } else {
      update = new JsonObject().put("$set", new JsonObject().put(CONFIGURATION_SYNC_PENDING, "0"));
      collectionName = OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
    }

    mongoClient.updateCollection(collectionName, query, update, res -> {
      if (res.succeeded()) {
        log.info(" {} Application dispatcher marked as completed {}", collectionName,
          entry.getString("_id"));
        if (DISPATCHER_COLLECTION.equals(collectionName)) {
          // Update application collections.
          updateDispatcherStatusInApplication(query, null);
        }
      } else {
        log.error(" Failed to  update {} dispatcher record id: {} as completed {}", collectionName,
          entry.getString("_id"), res.cause().getMessage());
      }
    });
  }

  public void retrySendApplicationToDispatcher(String dispatcherRecordId,
    JsonObject dispatcherRecord, int dispatcherMaxRetries, String error, String collectionName) {

    int retries = dispatcherRecord.getJsonObject("configuration").getInteger("retries", 0) + 1;
    boolean updateApplicationsStatus;

    JsonObject query = new JsonObject().put("_id", dispatcherRecordId);
    JsonObject update;

    if (retries > dispatcherMaxRetries) {
      if (DISPATCHER_COLLECTION.equals(collectionName)) {
        update = new JsonObject().put("$set", new JsonObject().put(CONFIGURATION_STATUS, FAILED));
      } else {
        update = new JsonObject().put("$set",
          new JsonObject().put(CONFIGURATION_SYNC_PENDING, "0"));
      }
      updateApplicationsStatus = true;
    } else {
      updateApplicationsStatus = false;

      update = new JsonObject().put("$set",
        new JsonObject().put(CONFIGURATION_STATUS, PENDING).put(CONFIGURATION_RETRIES, retries)
          .put("configuration.latest_retry_date", getCurrentTimestamp())
          .put("configuration.error_message", new JsonObject(error)));
    }

    mongoClient.updateCollection(collectionName, query, update, res -> {
      if (res.succeeded()) {
        log.info(" {} dispatcher record marked for retry: {}", collectionName, dispatcherRecordId);
        // The last try failed - update the application data sets status to fail.
        if (DISPATCHER_COLLECTION.equals(collectionName) && BooleanUtils.isTrue(
          updateApplicationsStatus)) {
          // Find the dispatcher record to get the application id.
          updateDispatcherStatusInApplication(query, DispatcherResponseStatus.FAILED);
        }
      } else {
        log.error(" Failed to update {} dispatcher record for retry, with error message {}",
          collectionName, res.cause().getMessage());
      }
    });
  }

  private void updateDispatcherStatusInApplication(JsonObject query,
    DispatcherResponseStatus specificStatus) {
    mongoClient.findOne(DISPATCHER_COLLECTION, query, null, asyncResult -> {
      if (asyncResult.result() != null) {
        dispatcherManager.updateDispatcherStatusInApplication(asyncResult.result(), specificStatus);
      }
    });
  }

  private String getCurrentTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }


  /**
   * Finds the pending dispatcher records and sends them to domibus.
   *
   * @param collection The collection to query.
   */
  public void sendOutgoingApplicationActivitiesToDispatcher(String collection,
    DispatcherClient dispatcherClient) {
    findPendingDispatcherRecords(collection).onComplete(ar -> {
      if (ar.succeeded()) {
        List<JsonObject> entries = ar.result();
        if (CollectionUtils.isNotEmpty(entries)) {
          log.debug("{} Total number found: {}", collection, entries.size());
          entries.forEach(entry -> sendRecordToDispatcher(entry, dispatcherClient).onSuccess(
            result -> markDispatcherRecordAsCompleted(entry, result)).onFailure(err -> {
            log.error("Failed to dispatch record {}", entry.getString("_id"), err);
            retrySendApplicationToDispatcher(entry.getString("_id"), entry, 10, err.getMessage(),
              OUTGOING_APPLICATION_ACTIVITIES_COLLECTION);
          }));


        } else {
          log.debug("{} No records found", collection);
        }
      } else {
        log.error("{} Failed to find pending messages, with error message: {}", collection,
          ar.cause().getMessage());
      }
    });
  }

  public void retrieveSentApplications(String collectionName, int retriesNumber) {

    findApplicationsWithLatestStatusSent(collectionName, retriesNumber)
      .onSuccess(applications -> {

        if (applications.isEmpty()) {
          log.debug("No applications found with a catalog in SENT status");
          return;
        }

        log.debug("Found {} applications with a SENT catalog for collection {}",
          applications.size(), collectionName);

        applications.forEach(application -> processSentApplication(application, collectionName, retriesNumber));
      })
      .onFailure(err ->
        log.error("Failed to retrieve SENT applications: {}", err.getMessage())
      );
  }


  private void processSentApplication(JsonObject application, String collectionName, int retriesNumber) {

    JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());

    catalogs.stream()
      .filter(o -> o instanceof JsonObject)
      .map(o -> (JsonObject) o)
      .filter(catalog -> getStatusWithSentAsLatest(catalog) != null)
      .forEach(catalog -> processCatalog(application, catalog, collectionName, retriesNumber));
  }

  private void processCatalog(JsonObject application, JsonObject catalog, String collectionName, int retriesNumber) {

    getDispatcherConfiguration(application, collectionName)
      .onSuccess(dispatcherResult -> handleDispatcherResponse(application, catalog, dispatcherResult, collectionName, retriesNumber))
      .onFailure(err -> log.debug("getDispatcherConfiguration failed with message: {}", err.getMessage()));
  }

  private Future<JsonObject> getDispatcherConfiguration(JsonObject application, String collectionName) {

    Promise<JsonObject> promise = Promise.promise();

    JsonObject query = new JsonObject()
      .put("application._id", application.getString("_id"))
      .put("configuration.application_type", collectionName);

    JsonObject projection = new JsonObject()
      .put("configuration", 1)
      .put("_id", 1);

    mongoClient.findOne(DISPATCHER_COLLECTION, query, projection, res -> {
      if (res.succeeded()) {
        if(res.result() != null){
          promise.complete(res.result());
        } else {
          promise.complete(new JsonObject().put("reason", "No application id found in dispatcher"));
        }
      } else {
        promise.fail(res.cause() != null ? res.cause() : new RuntimeException("Dispatcher configuration not found"));
      }
    });

    return promise.future();
  }

  private void handleDispatcherResponse(JsonObject application, JsonObject catalog, JsonObject dispatcherResult,
    String collectionName, int retriesNumber) {

    String catalogId = catalog.getString("catalog_id");
    String applicationId = application.getString("_id");

    if (isCatalogReceived(dispatcherResult, applicationId, catalogId)) {
      changeCatalogToSubmitted(application, catalogId, collectionName);
    } else {
      incrementRetryStatus(catalog, dispatcherResult, retriesNumber);
      saveApplication(application, collectionName);
    }
  }

  private boolean isCatalogReceived(JsonObject dispatcherResult, String applicationId, String catalogId) {

    JsonObject configuration = dispatcherResult.getJsonObject("configuration");
    if(configuration != null && configuration.containsKey("dispatcher_response")){
      JsonArray dispatcherResponseArray = configuration.getJsonArray("dispatcher_response", new JsonArray());

      for (Object dispatcherResponseObject : dispatcherResponseArray) {
        JsonObject dispatcherResponse = (JsonObject) dispatcherResponseObject;

        boolean applicationIdMatch = applicationId.equals(dispatcherResponse.getString("application_id"));
        boolean catalogIdMatch = dispatcherResponse.getJsonArray("catalog_ids").contains(catalogId);

        if (applicationIdMatch && catalogIdMatch) {
          return "domibus_national_received".equals(dispatcherResponse.getString("status"));
        }
      }
    }

    return false;
  }

  private void incrementRetryStatus(JsonObject catalog, JsonObject dispatcherResult, int retriesNumber) {

    JsonObject latestStatus = getStatusWithSentAsLatest(catalog);
    if (latestStatus == null) return;

    JsonObject dispatcherStatus = latestStatus.getJsonObject("dispatcher_status", new JsonObject());

    int retries = dispatcherStatus.getInteger("retries", 0);

    if (retries < retriesNumber) {
      retries++;
      dispatcherStatus.put("retries", retries);
      dispatcherStatus.put("dispatcher_response", dispatcherResult);
      latestStatus.put("dispatcher_status", dispatcherStatus);
    }
  }

  private Future<List<JsonObject>> findApplicationsWithLatestStatusSent(
    String collectionName,
    int maxRetries
  ) {

    Promise<List<JsonObject>> promise = Promise.promise();

    JsonObject queryForSentApplicationRecords = new JsonObject()
      .put("catalogs", new JsonObject()
        .put("$elemMatch", new JsonObject()
          .put("statuses", new JsonObject()
            .put("$elemMatch", new JsonObject()
              .put("status", "SENT")
              .put("latest", 1)
              .put("$or", new JsonArray()
                .add(new JsonObject()
                  .put("dispatcher_status", new JsonObject()
                    .put("$exists", false)))
                .add(new JsonObject()
                  .put("dispatcher_status.retries", new JsonObject()
                    .put("$exists", false)))
                .add(new JsonObject()
                  .put("dispatcher_status.retries", new JsonObject()
                    .put("$lt", maxRetries)))
              )
            )
          )
        )
      );

    mongoClient.find(collectionName, queryForSentApplicationRecords, listAsyncResult -> {
      if (listAsyncResult.succeeded()) {
        log.debug("Found pending applications with 'SENT' status");
        promise.complete(listAsyncResult.result());
      } else {
        log.error(" Failed to find pending applications with 'SENT' status, with error message: {}",
          listAsyncResult.cause().getMessage());
        promise.fail(listAsyncResult.cause());
      }
    });

    return promise.future();
  }

}
