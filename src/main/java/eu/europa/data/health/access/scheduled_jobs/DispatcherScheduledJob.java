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
import static eu.europa.data.health.access.Constants.FAILED;
import static eu.europa.data.health.access.Constants.IN_PROGRESS;
import static eu.europa.data.health.access.Constants.PENDING;

import eu.europa.data.health.access.DispatcherResponseStatus;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.services.DispatcherManager;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherScheduledJob {

  private final MongoClient mongoClient;
  private final DispatcherManager dispatcherManager;
  public static final String DISPATCHER_COLLECTION = "DISPATCHER";
  private final Logger log = LoggerFactory.getLogger(getClass());

  public DispatcherScheduledJob(MongoClient mongoClient, DispatcherManager dispatcherManager) {
    this.mongoClient = mongoClient;
    this.dispatcherManager = dispatcherManager;
  }


  public void sendApplicationsToDispatcher(final int dispatcherMaxRetries, DispatcherClient dispatcherClient, String applicationType) {

    findPendingDispatcherRecords(applicationType).onComplete(res -> {
      if (res.succeeded()) {
        List<JsonObject> dispatcherRecords = res.result();
        if(dispatcherRecords != null && !dispatcherRecords.isEmpty()){
          log.info("{} Total number of dispatcher records for application type: {} found: {}", DISPATCHER_COLLECTION, applicationType, dispatcherRecords.size());
          sendApplicationsToDispatcher(dispatcherRecords, dispatcherMaxRetries, dispatcherClient);
        } else {
          log.info(DISPATCHER_COLLECTION
          + " No dispatcher records found");
        }

      } else {
        log.error(DISPATCHER_COLLECTION
          + " Find pending dispatcher records was not successful, with error message: {}", res.cause().getMessage());
      }
    });
  }



  private Future<List<JsonObject>> findPendingDispatcherRecords(String applicationType) {
    Promise<List<JsonObject>> promise = Promise.promise();

    JsonObject queryForPendingDispatcherRecords = new JsonObject().put(CONFIGURATION_STATUS, PENDING).put(CONFIGURATION_APPLICATION_TYPE, applicationType);
    mongoClient.find(DISPATCHER_COLLECTION, queryForPendingDispatcherRecords, listAsyncResult -> {
      if (listAsyncResult.succeeded()) {
        log.info(DISPATCHER_COLLECTION
          + " Find pending dispatcher records was successful");
        promise.complete(listAsyncResult.result());
      } else {
        log.error(DISPATCHER_COLLECTION
          + " Failed to find pending dispatcher records, with error message: {}", listAsyncResult.cause().getMessage());
        promise.fail(listAsyncResult.cause());
      }
    });

    return promise.future();
  }

  private void sendApplicationsToDispatcher(List<JsonObject> dispatcherRecords, final int dispatcherMaxRetries, DispatcherClient dispatcherClient) {

        for (JsonObject dispatcherRecord : dispatcherRecords) {
          processDispatcherRecordWithLock(dispatcherRecord, dispatcherMaxRetries, dispatcherClient);
        }
  }

  private void processDispatcherRecordWithLock(JsonObject dispatcherRecord, final int dispatcherMaxRetries, DispatcherClient dispatcherClient) {

    String dispatcherRecordId = dispatcherRecord.getString("_id");

    // Try to acquire a lock on the task
    JsonObject query = new JsonObject().put("_id", dispatcherRecordId).put(CONFIGURATION_STATUS, PENDING);
    JsonObject update = new JsonObject().put("$set", new JsonObject().put(CONFIGURATION_STATUS, IN_PROGRESS));

    mongoClient.findOneAndUpdate(DISPATCHER_COLLECTION, query, update, res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          log.info(DISPATCHER_COLLECTION
            + " Lock acquired for dispatcher record with id: {}", dispatcherRecordId);
          sendRecordToDispatcher(dispatcherRecord, dispatcherClient).onComplete(actionRes -> {
            if (actionRes.succeeded()) {
              markDispatcherRecordAsCompleted(dispatcherRecordId, actionRes.result());
            } else {
              log.error(DISPATCHER_COLLECTION
                + " Send application to dispatcher failed for record with id: {}", dispatcherRecordId);
              retrySendApplicationToDispatcher(dispatcherRecordId, dispatcherRecord, dispatcherMaxRetries, actionRes.cause().getMessage());
            }
          });
        } else {
          log.error(DISPATCHER_COLLECTION
            + " Failed to acquire lock for dispatcher record with id: {}", dispatcherRecordId);
        }
      } else {
        log.error(DISPATCHER_COLLECTION
          + " Failed to find and update the dispatcher record, with error message: {}", res.cause().getMessage());
      }
    });
  }

  public Future<String> sendRecordToDispatcher(JsonObject dispatcherRecord, DispatcherClient dispatcherClient) {

    Promise<String> promise = Promise.promise();

    dispatcherClient.sendApplication(dispatcherRecord).onComplete(actionRes -> {
      if (actionRes.succeeded()) {
        log.info(DISPATCHER_COLLECTION
          +" Action performed on application");
        promise.complete(actionRes.result());
      } else {
        promise.fail(actionRes.cause().getMessage());
      }
    });
    return promise.future();
  }

  public void markDispatcherRecordAsCompleted(String dispatcherRecordId, String responseMessageId) {
    JsonObject query = new JsonObject().put("_id", dispatcherRecordId);
    JsonObject update = new JsonObject().put("$set", new JsonObject().put(CONFIGURATION_STATUS, COMPLETED).put("configuration.completion_date", getCurrentTimestamp()).put("configuration.dispatcher_response", new JsonArray(responseMessageId)));

    mongoClient.updateCollection(DISPATCHER_COLLECTION, query, update, res -> {
      if (res.succeeded()) {
        log.info(DISPATCHER_COLLECTION
          + " Application dispatcher marked as completed {}", dispatcherRecordId);

        // Update application collections.
        updateDispatcherStatusInApplication(query, null);
      } else {
        log.error(DISPATCHER_COLLECTION
          + " Failed to  update dispatcher record id: {} as completed {}", dispatcherRecordId, res.cause().getMessage());
      }
    });
  }

  public void retrySendApplicationToDispatcher(String dispatcherRecordId,
    JsonObject dispatcherRecord, int dispatcherMaxRetries, String error) {

    int retries = dispatcherRecord.getJsonObject("configuration").getInteger("retries", 0) + 1;
    boolean updateApplicationsStatus;

    JsonObject query = new JsonObject().put("_id", dispatcherRecordId);
    JsonObject update;

    if (retries > dispatcherMaxRetries) {
      update = new JsonObject().put("$set", new JsonObject().put(CONFIGURATION_STATUS, FAILED));
      updateApplicationsStatus = true;
    } else {
      updateApplicationsStatus = false;

      update = new JsonObject().put("$set", new JsonObject()
        .put(CONFIGURATION_STATUS, PENDING)
        .put(CONFIGURATION_RETRIES, retries)
        .put("configuration.latest_retry_date", getCurrentTimestamp())
        .put("configuration.error_message", new JsonObject(error)));
    }

    mongoClient.updateCollection(DISPATCHER_COLLECTION, query, update, res -> {
      if (res.succeeded()) {
        log.info(DISPATCHER_COLLECTION
          + " Application dispatcher record marked for retry: {}", dispatcherRecordId);
        // The last try failed - update the application data sets status to fail.
        if (BooleanUtils.isTrue(updateApplicationsStatus)) {
          // Find the dispatcher record to get the application id.
          updateDispatcherStatusInApplication(query, DispatcherResponseStatus.FAILED);
        }
      } else {
        log.error(DISPATCHER_COLLECTION
            + " Failed to update application dispatcher record for retry, with error message {}", res.cause().getMessage());
      }
    });
  }

  private void updateDispatcherStatusInApplication(JsonObject query, DispatcherResponseStatus specificStatus){
    mongoClient.findOne(DISPATCHER_COLLECTION, query, null, asyncResult -> {
      if (asyncResult.result() != null) {
        dispatcherManager.updateDispatcherStatusInApplication(asyncResult.result(), specificStatus);
      }
    });
  }

  private String getCurrentTimestamp(){
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }
}
