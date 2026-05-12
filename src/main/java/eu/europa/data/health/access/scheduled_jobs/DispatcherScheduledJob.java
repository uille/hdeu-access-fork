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
import static eu.europa.data.health.access.Constants.TRANSLATIONS_STATE_COLLECTION;

import eu.europa.data.health.access.CatalogStatus;
import eu.europa.data.health.access.DatasetStatus;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.CommonApplicationManager;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.NotificationManager;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherScheduledJob extends CommonApplicationManager {

  public static final String CONFIGURATION_SYNC_PENDING = "configuration.syncPending";
  public static final String DECISION_DATE_RECEIVED = "decision_date_received";
  public static final String DATASET_STATUS = "datasetStatus";
  private final MongoClient mongoClient;
  private final DispatcherManager dispatcherManager;
  private final Logger log = LoggerFactory.getLogger(getClass());

  public DispatcherScheduledJob(DBHandler dbHandler, MongoClient mongoClient,
    DispatcherManager dispatcherManager, AuditLogManager auditLogManager,
    NotificationManager notificationManager, JsonObject config) {
    super(dbHandler, mongoClient, auditLogManager, notificationManager, config);
    this.mongoClient = mongoClient;

    this.dispatcherManager = dispatcherManager;
  }


  public Future<Void> sendApplicationsToDispatcher(final int dispatcherMaxRetries,
    DispatcherClient dispatcherClient, String applicationType) {
    Promise<Void> promise = Promise.promise();

    findPendingDispatcherRecords(applicationType).onComplete(res -> {
      if (res.succeeded()) {
        List<JsonObject> dispatcherRecords = res.result();
        if (dispatcherRecords != null && !dispatcherRecords.isEmpty()) {
          log.info("{} Total number of dispatcher records for application type: {} found: {}",
            DISPATCHER_COLLECTION, applicationType, dispatcherRecords.size());
          sendApplicationsToDispatcher(dispatcherRecords, dispatcherMaxRetries, dispatcherClient)
            .onComplete(ar -> {
              if (ar.succeeded()) {
                promise.complete();
              } else {
                promise.fail(ar.cause());
              }
            });
        } else {
          log.debug(DISPATCHER_COLLECTION + " No dispatcher records found");
          promise.complete();
        }

      } else {
        log.error(DISPATCHER_COLLECTION
            + " Find pending dispatcher records was not successful, with error message: {}",
          res.cause().getMessage());
        promise.fail(res.cause());
      }
    });
    return promise.future();
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

  private Future<Void> sendApplicationsToDispatcher(List<JsonObject> dispatcherRecords,
    final int dispatcherMaxRetries, DispatcherClient dispatcherClient) {

    List<Future<Void>> allOperations = new ArrayList<>();
    for (JsonObject dispatcherRecord : dispatcherRecords) {
      allOperations.add(processDispatcherRecordWithLock(dispatcherRecord, dispatcherMaxRetries, dispatcherClient));
    }

    return Future.all(allOperations).mapEmpty();
  }

  private Future<Void> processDispatcherRecordWithLock(JsonObject dispatcherRecord,
    final int dispatcherMaxRetries, DispatcherClient dispatcherClient) {

    Promise<Void> promise = Promise.promise();
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
              markDispatcherRecordAsCompleted(dispatcherRecord, actionRes.result())
                .onComplete(ar -> {
                  if (ar.succeeded()) {
                    promise.complete();
                  } else {
                    promise.fail(ar.cause());
                  }
                });
            } else {
              log.error(DISPATCHER_COLLECTION
                  + " Send application to dispatcher failed for record with id: {}",
                dispatcherRecordId);
              retrySendApplicationToDispatcher(dispatcherRecordId, dispatcherRecord,
                dispatcherMaxRetries, actionRes.cause().getMessage(), DISPATCHER_COLLECTION)
                .onComplete(ar -> {
                  if (ar.succeeded()) {
                    promise.complete();
                  } else {
                    promise.fail(ar.cause());
                  }
                });
            }
          });
        } else {
          log.error(
            DISPATCHER_COLLECTION + " Failed to acquire lock for dispatcher record with id: {}",
            dispatcherRecordId);
          promise.complete();
        }
      } else {
        log.error(DISPATCHER_COLLECTION
            + " Failed to find and update the dispatcher record, with error message: {}",
          res.cause().getMessage());
        promise.fail(res.cause());
      }
    });

    return promise.future();
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

  public Future<Void> markDispatcherRecordAsCompleted(JsonObject entry, String responseMessageId) {
    Promise<Void> promise = Promise.promise();
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
        Future<Void> postUpdate = Future.succeededFuture();
        if (OUTGOING_APPLICATION_ACTIVITIES_COLLECTION.equals(collectionName)
          && OperationStatus.ACTIVITY_TRANSLATION.getLabel().equalsIgnoreCase(entry.getString(OPERATION))) {
          postUpdate = markTranslationStateDispatcherCompleted(entry);
        }
        if (DISPATCHER_COLLECTION.equals(collectionName)) {
          // Update application collections.
          updateDispatcherStatusInApplication(query);
        }
        postUpdate.onSuccess(v -> promise.complete()).onFailure(promise::fail);
      } else {
        log.error(" Failed to  update {} dispatcher record id: {} as completed {}", collectionName,
          entry.getString("_id"), res.cause().getMessage());
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  public Future<Void> retrySendApplicationToDispatcher(String dispatcherRecordId,
    JsonObject dispatcherRecord, int dispatcherMaxRetries, String error, String collectionName) {
    Promise<Void> promise = Promise.promise();

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
        Future<Void> postUpdate = Future.succeededFuture();
        if (OUTGOING_APPLICATION_ACTIVITIES_COLLECTION.equals(collectionName)
          && updateApplicationsStatus
          && OperationStatus.ACTIVITY_TRANSLATION.getLabel().equalsIgnoreCase(dispatcherRecord.getString(OPERATION))) {
          postUpdate = markTranslationStateDispatcherFailed(dispatcherRecord, error);
        }
        postUpdate.onSuccess(v -> promise.complete()).onFailure(promise::fail);
      } else {
        log.error(" Failed to update {} dispatcher record for retry, with error message {}",
          collectionName, res.cause().getMessage());
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  private void updateDispatcherStatusInApplication(JsonObject query) {
    mongoClient.findOne(DISPATCHER_COLLECTION, query, null, asyncResult -> {
      if (asyncResult.result() != null) {
        dispatcherManager.updateDispatcherStatusInApplication(asyncResult.result(), null);
      }
    });
  }

  private String getCurrentTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }

  private Future<Void> markTranslationStateDispatcherCompleted(JsonObject entry) {
    String translationStateId = entry.getJsonObject("configuration", new JsonObject())
      .getString("translationStateId");
    if (translationStateId == null) {
      return Future.succeededFuture();
    }

    JsonObject set = new JsonObject()
      .put("configuration.dispatcherStatus", COMPLETED)
      .put("configuration.syncPending", "0")
      .put("syncPending", "0");
    return mongoClient.updateCollection(TRANSLATIONS_STATE_COLLECTION,
      new JsonObject().put("_id", translationStateId), new JsonObject().put("$set", set))
      .mapEmpty();
  }

  private Future<Void> markTranslationStateDispatcherFailed(JsonObject entry, String error) {
    String translationStateId = entry.getJsonObject("configuration", new JsonObject())
      .getString("translationStateId");
    if (translationStateId == null) {
      return Future.succeededFuture();
    }

    JsonObject set = new JsonObject()
      .put("configuration.dispatcherStatus", FAILED)
      .put("configuration.error_message", error);
    return mongoClient.updateCollection(TRANSLATIONS_STATE_COLLECTION,
      new JsonObject().put("_id", translationStateId), new JsonObject().put("$set", set))
      .mapEmpty();
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

  public Future<Void> retrieveSentApplications(String collectionName, int retriesNumber) {
    Promise<Void> promise = Promise.promise();

    findApplicationsWithLatestStatusSent(collectionName, retriesNumber)
      .onSuccess(applications -> {

        if (applications.isEmpty()) {
          log.debug("No applications found with a catalog in SENT status");
          promise.complete();
          return;
        }

        log.debug("Found {} applications with a SENT catalog for collection {}",
          applications.size(), collectionName);

        List<Future<Void>> allOperations = new ArrayList<>();
        for (JsonObject application : applications) {
          allOperations.add(processSentApplication(application, collectionName, retriesNumber));
        }

        Future.all(allOperations).onComplete(ar -> {
          if (ar.succeeded()) {
            promise.complete();
          } else {
            promise.fail(ar.cause());
          }
        });
      })
      .onFailure(err -> {
        log.error("Failed to retrieve SENT applications: {}", err.getMessage());
        promise.fail(err);
      });

    return promise.future();
  }


  private Future<Void> processSentApplication(JsonObject application, String collectionName, int retriesNumber) {
    Promise<Void> promise = Promise.promise();
    JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());

    List<Future<Void>> catalogOperations = new ArrayList<>();
    catalogs.stream()
      .filter(o -> o instanceof JsonObject)
      .map(o -> (JsonObject) o)
      .filter(catalog -> getStatusWithSentAsLatest(catalog) != null)
      .forEach(catalog -> catalogOperations.add(processCatalog(application, catalog, collectionName, retriesNumber)));

    if (catalogOperations.isEmpty()) {
      promise.complete();
    } else {
      Future.all(catalogOperations).onComplete(ar -> {
        if (ar.succeeded()) {
          promise.complete();
        } else {
          promise.fail(ar.cause());
        }
      });
    }

    return promise.future();
  }

  private Future<Void> processCatalog(JsonObject application, JsonObject catalog, String collectionName, int retriesNumber) {
    Promise<Void> promise = Promise.promise();

    getDispatcherConfiguration(application, collectionName)
      .onSuccess(dispatcherResult -> handleDispatcherResponse(application, catalog, dispatcherResult, collectionName, retriesNumber)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            promise.complete();
          } else {
            promise.fail(ar.cause());
          }
        }))
      .onFailure(err -> {
        log.debug("getDispatcherConfiguration failed with message: {}", err.getMessage());
        promise.complete();
      });

    return promise.future();
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

  private Future<Void> handleDispatcherResponse(JsonObject application, JsonObject catalog, JsonObject dispatcherResult,
    String collectionName, int retriesNumber) {
    Promise<Void> promise = Promise.promise();

    String catalogId = catalog.getString("catalog_id");
    String applicationId = application.getString("_id");

    if (isCatalogReceived(dispatcherResult, applicationId, catalogId)) {
      changeCatalogToSubmitted(application, catalogId, collectionName)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            promise.complete();
          } else {
            promise.fail(ar.cause());
          }
        });
    } else {
      incrementRetryStatus(catalog, dispatcherResult, retriesNumber);
      saveApplication(application, collectionName)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            promise.complete();
          } else {
            promise.fail(ar.cause());
          }
        });
    }

    return promise.future();
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

  public Future<Void> sendWithDrawApplicationsToDispatcher(String applicationType,
    Integer withdrawThresholdDays) {
    Promise<Void> promise = Promise.promise();
    Instant nowMinusThresholdDays = Instant.now().minus(withdrawThresholdDays, ChronoUnit.DAYS);

    findApplicationsWithDecisionsToWithdraw(applicationType, nowMinusThresholdDays).onFailure(promise::fail)
      .onSuccess(applications -> {

        if (applications == null || applications.isEmpty()) {
          log.info("No pending decision for withdraw");
          promise.complete();
          return;
        }
        List<Future<Void>> allOperations = new ArrayList<>();

        for (JsonObject application : applications) {
          String applicationId = application.getString("_id");
          JsonArray datasets = application.getJsonArray("datasets");
          List<JsonObject> withdrawnDatasets = new ArrayList<>();
          Set<String> affectedCatalogIds = new LinkedHashSet<>();

          for (Object o : datasets) {
            JsonObject dataset = (JsonObject) o;
            JsonArray decisions = dataset.getJsonArray("decisions");

            if (!hasDecisionOlderThanThresholdDays(decisions, nowMinusThresholdDays)) {
              continue;
            }

            String datasetId = dataset.getString("dataset_id");
            String catalogId = dataset.getString("catalog_id");

            updateDatasetStatus(datasets, catalogId, datasetId, DatasetStatus.WITHDRAWN, null,
              null, null, null);
            affectedCatalogIds.add(catalogId);
            withdrawnDatasets.add(new JsonObject()
              .put("catalog_id", catalogId)
              .put("dataset_id", datasetId));
          }

          if (withdrawnDatasets.isEmpty()) {
            continue;
          }

          for (String catalogId : affectedCatalogIds) {
            if (areAllDatasetsWithdrawnForCatalog(datasets, catalogId)) {
              updateCatalogStatus(application, catalogId, CatalogStatus.WITHDRAWN);
            }
          }

          Future<Void> operation = replaceApplication(application, applicationType).compose(v -> {
            List<Future<Void>> outgoingActivities = new ArrayList<>();
            for (JsonObject withdrawnDataset : withdrawnDatasets) {
              outgoingActivities.add(sendApplicationToOutgoingApplicationActivities(applicationType,
                applicationId, CatalogStatus.DECISION_ISSUED,
                withdrawnDataset.getString("catalog_id"), DatasetStatus.WITHDRAWN.getLabel(),
                withdrawnDataset.getString("dataset_id"), null, null,null,null,null,null,
                OperationStatus.DECISION, null));
            }
            return Future.all(outgoingActivities).mapEmpty();
          });

          allOperations.add(operation);
        }

        if (allOperations.isEmpty()) {
          // No datasets qualified
          promise.complete();
          return;
        }

        Future.all(allOperations).onSuccess(v -> promise.complete()).onFailure(err -> {
          log.error("Failed to process withdraw applications", err);
          promise.fail(err);
        });
      });

    return promise.future();
  }

  public Future<Void> withdrawCatalogsWithExpiredAdditionalInformation(String applicationType,
    Integer expirationDays) {
    Promise<Void> promise = Promise.promise();
    Instant requestingDateThreshold = Instant.now().minus(expirationDays, ChronoUnit.DAYS);

    findApplicationsWithLatestAdditionalInformation(applicationType)
      .onFailure(promise::fail)
      .onSuccess(applications -> {
        if (applications == null || applications.isEmpty()) {
          log.info("No applications found with catalogs in ADDITIONAL_INFORMATION status");
          promise.complete();
          return;
        }

        List<Future<Void>> allOperations = new ArrayList<>();

        for (JsonObject application : applications) {
          List<String> expiredCatalogIds = findExpiredAdditionalInformationCatalogIds(application,
            requestingDateThreshold);

          if (expiredCatalogIds.isEmpty()) {
            continue;
          }

          Future<Void> appOperation = Future.succeededFuture();
          for (String catalogId : expiredCatalogIds) {
            appOperation = appOperation.compose(v -> changeCatalogToWithdrawn(application,
              catalogId, applicationType));
          }
          allOperations.add(appOperation);
        }

        if (allOperations.isEmpty()) {
          promise.complete();
          return;
        }

        Future.all(allOperations).onSuccess(v -> promise.complete()).onFailure(err -> {
          log.error("Failed to auto-withdraw catalogs awaiting additional information", err);
          promise.fail(err);
        });
      });

    return promise.future();
  }

  private Future<List<JsonObject>> findApplicationsWithLatestAdditionalInformation(
    String applicationType) {
    Promise<List<JsonObject>> promise = Promise.promise();

    JsonObject query = new JsonObject().put("catalogs",
      new JsonObject().put("$elemMatch", new JsonObject().put("statuses",
        new JsonObject().put("$elemMatch", new JsonObject()
          .put("status", CatalogStatus.ADDITIONAL_INFORMATION.getLabel())
          .put("latest", 1)))));

    mongoClient.find(applicationType, query).onFailure(err -> {
      log.error("Failed to find applications with ADDITIONAL_INFORMATION catalogs", err);
      promise.fail(err);
    }).onSuccess(result -> {
      log.info("Found {} {} application(s) with ADDITIONAL_INFORMATION catalogs", result.size(),
        applicationType);
      promise.complete(result);
    });

    return promise.future();
  }

  private List<String> findExpiredAdditionalInformationCatalogIds(JsonObject application,
    Instant requestingDateThreshold) {
    List<String> expiredCatalogIds = new ArrayList<>();
    JsonArray catalogs = application.getJsonArray("catalogs", new JsonArray());

    for (Object catalogObj : catalogs) {
      JsonObject catalog = (JsonObject) catalogObj;
      JsonObject latestStatus = getLatestCatalogStatus(catalog);
      if (latestStatus == null || !CatalogStatus.ADDITIONAL_INFORMATION.getLabel()
        .equalsIgnoreCase(latestStatus.getString("status"))) {
        continue;
      }

      String requestingDate = extractAdditionalInformationRequestingDate(
        latestStatus.getJsonObject("statusPayload"));
      if (requestingDate == null) {
        continue;
      }

      try {
        if (Instant.parse(requestingDate).isBefore(requestingDateThreshold)) {
          expiredCatalogIds.add(catalog.getString("catalog_id"));
        }
      } catch (Exception ex) {
        log.warn("Skipping catalog {} in application {} due to invalid requestingDate '{}'",
          catalog.getString("catalog_id"), application.getString("_id"), requestingDate);
      }
    }

    return expiredCatalogIds;
  }

  private JsonObject getLatestCatalogStatus(JsonObject catalog) {
    JsonArray statuses = catalog.getJsonArray("statuses", new JsonArray());

    for (Object statusObj : statuses) {
      JsonObject status = (JsonObject) statusObj;
      if (status.getInteger("latest", 0) == 1) {
        return status;
      }
    }

    return null;
  }

  private boolean areAllDatasetsWithdrawnForCatalog(JsonArray datasets, String catalogId) {
    boolean catalogFound = false;

    for (Object datasetObj : datasets) {
      JsonObject dataset = (JsonObject) datasetObj;
      if (!catalogId.equals(dataset.getString("catalog_id"))) {
        continue;
      }

      catalogFound = true;
      if (!isDatasetWithdrawn(dataset)) {
        return false;
      }
    }

    return catalogFound;
  }

  private boolean isDatasetWithdrawn(JsonObject dataset) {
    JsonArray decisions = dataset.getJsonArray("decisions", new JsonArray());

    for (Object decisionObj : decisions) {
      JsonObject decision = (JsonObject) decisionObj;
      if (decision.getInteger("latest", 0) != 1) {
        continue;
      }

      Object datasetStatus = decision.getValue(DATASET_STATUS);
      return DatasetStatus.WITHDRAWN.equals(datasetStatus)
        || DatasetStatus.WITHDRAWN.getLabel().equals(String.valueOf(datasetStatus));
    }

    return false;
  }


  private boolean hasDecisionOlderThanThresholdDays(JsonArray decisions, Instant nowMinusThresholdDays) {
    if (decisions == null || decisions.isEmpty()) {
      return false;
    }

    return decisions.stream().map(o -> (JsonObject) o).anyMatch(
      d -> d.getInteger("latest", 0) == 1
        && ("NEGATIVE_DECISION_ISSUED".equals(d.getString(DATASET_STATUS))
          || "POSITIVE_DECISION_ISSUED".equals(d.getString(DATASET_STATUS)))
        && d.getString(DECISION_DATE_RECEIVED) != null
        && Instant.parse(d.getString(DECISION_DATE_RECEIVED)).isBefore(nowMinusThresholdDays));
  }


  private Future<List<JsonObject>> findApplicationsWithDecisionsToWithdraw(String applicationType,
    Instant nowMinusThresholdDays) {
    Promise<List<JsonObject>> promise = Promise.promise();

    String iso = DateTimeFormatter.ISO_INSTANT.format(nowMinusThresholdDays);

    JsonObject query = new JsonObject().put("datasets",
      new JsonObject().put("$elemMatch", new JsonObject()   // any dataset in datasets array
        .put("decisions",
          new JsonObject().put("$elemMatch", new JsonObject()  // any decision in decisions array
            .put("latest", 1).put(DATASET_STATUS, new JsonObject().put("$in",
              new JsonArray().add("NEGATIVE_DECISION_ISSUED").add("POSITIVE_DECISION_ISSUED")))
            .put(DECISION_DATE_RECEIVED, new JsonObject().put("$lt", iso))))));

    mongoClient.find(applicationType, query).onFailure(event -> {
      log.error("Failed to find applications", event);
      promise.fail(event);

    }).onSuccess(list -> {
      log.info("Found {} {} With Decisions To Withdraw", list.size(), applicationType);
      promise.complete(list);
    });
    return promise.future();
  }

}
