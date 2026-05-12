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

import static eu.europa.data.health.access.Constants.CATALOG_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DATASET_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.NOTIFICATION_HISTORY_COLLECTION;
import static eu.europa.data.health.access.Constants.PERMIT_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;

import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.PermitStatus;
import eu.europa.data.health.access.services.NotificationManager;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationScheduledJob {

  private static final String DATASETS_DATASET_ID = String.join(".", DATASETS, DATASET_ID);
  private static final String DATASETS_DECISIONS = String.join(".", DATASETS, "decisions");
  private static final String DATASETS_DECISIONS_LATEST = String.join(".", DATASETS_DECISIONS, "latest");
  private static final String DATASETS_DECISIONS_STATUS = String.join(".", DATASETS_DECISIONS, "datasetStatus");
  private static final String DATASETS_DECISIONS_DATE_RECEIVED = String.join(".", DATASETS_DECISIONS, "decision_date_received");
  private static final String DATASETS_PERMITS = String.join(".", DATASETS, "permits");
  private static final String DATASETS_PERMITS_LATEST = String.join(".", DATASETS_PERMITS, "latest");
  private static final String DATASETS_PERMITS_STATUS = String.join(".", DATASETS_PERMITS, "permit_status");
  private static final String DATASETS_PERMITS_EXPIRATION_DATE = String.join(".", DATASETS_PERMITS, "permit_expiration_date");
  private static final String CATALOGS = "catalogs";
  private static final String CATALOGS_ID = String.join(".", CATALOGS, "catalog_id");
  private static final String CATALOGS_STATUSES = String.join(".", CATALOGS, "statuses");
  private static final String CATALOGS_STATUSES_LATEST = String.join(".", CATALOGS_STATUSES, "latest");
  private static final String CATALOGS_STATUSES_STATUS = String.join(".", CATALOGS_STATUSES, "status");
  private static final String CATALOGS_STATUSES_DATE_RECEIVED = String.join(".", CATALOGS_STATUSES, "date_received");

  private final MongoClient mongoClient;
  private final NotificationManager notificationManager;
  private final Integer withdrawnTimePeriod;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public NotificationScheduledJob(MongoClient mongoClient, NotificationManager notificationManager, Integer withdrawnTimePeriod) {
    this.mongoClient = mongoClient;
    this.notificationManager = notificationManager;
    this.withdrawnTimePeriod = withdrawnTimePeriod;
  }

  public void createScheduledNotifications(
    Set<Integer> daysToDisplayNotifications,
    List<String> statuses,
    NotificationCode notificationCode,
    String notificationType) {

    log.info("Running notification job at {} for notification type: {}", LocalDateTime.now(), notificationType);

    if (daysToDisplayNotifications == null || daysToDisplayNotifications.isEmpty()) {
      log.error("No timeframes configured → skipping job");
      return;
    }

    List<Future> futures = new ArrayList<>();

    switch (notificationType) {
      case PERMIT_NOTIFICATION_TYPE -> {
        futures.addAll(
          daysToDisplayNotifications.stream()
            .map(day -> processPermitsThatAreAboutToExpire(day, statuses, notificationCode))
            .toList()
        );
      }

      case DATASET_NOTIFICATION_TYPE -> {
        // List of collections to process
        List<String> collections = List.of(
          DATA_ACCESS_APPLICATION_COLLECTION,
          DATA_REQUEST_APPLICATION_COLLECTION
        );

        futures.addAll(
          collections.stream()
            .flatMap(collection ->
              daysToDisplayNotifications.stream()
                .map(day -> processDatasetsThatAreAboutToWithdraw(collection, day, statuses, notificationCode))
            )
            .toList()
        );
      }

      case CATALOG_NOTIFICATION_TYPE -> {
        // List of collections to process
        List<String> collections = List.of(
            DATA_ACCESS_APPLICATION_COLLECTION,
            DATA_REQUEST_APPLICATION_COLLECTION
        );

        futures.addAll(
            collections.stream()
                .flatMap(collection ->
                    daysToDisplayNotifications.stream()
                        .map(day -> processCatalogsThatAreAboutToWithdraw(collection, day, statuses, notificationCode))
                )
                .toList()
        );
      }

      default -> {
        log.error("Notification type: {} didn't match any of the appropriate types", notificationType);
      }
    }

    CompositeFuture.all(futures)
      .compose(v -> cleanupOldNotificationHistory())
      .onSuccess(v -> log.info("Notification job completed"))
      .onFailure(err -> log.error("Notification job failed: ", err))
      .mapEmpty();
  }

  private Future<Void> processPermitsThatAreAboutToExpire(Integer dayToDisplayNotifications, List<String> statuses, NotificationCode notificationCode) {

    Instant now = Instant.now();
    Instant notifyAt = now.plus(Duration.ofDays(dayToDisplayNotifications));

    JsonObject query = createQueryForPermitsThatAreAboutToExpire(dayToDisplayNotifications, statuses);
    JsonObject projection = new JsonObject()
      .put(ID, 1)
      .put(USER_ID, 1)
      .put(TITLE, 1)
      .put(DATASETS_DATASET_ID, 1)
      .put(DATASETS_PERMITS_LATEST, 1)
      .put(DATASETS_PERMITS_STATUS, 1)
      .put(DATASETS_PERMITS_EXPIRATION_DATE, 1);
    FindOptions options = new FindOptions().setFields(projection);
    Promise<Void> promise = Promise.promise();

    mongoClient.findWithOptions(DATA_ACCESS_APPLICATION_COLLECTION, query, options, res -> {
      if (res.failed()) {
        promise.fail(res.cause());
        return;
      }

      List<JsonObject> applications = res.result();
      log.debug("Timeframe {}d → found {} applications", dayToDisplayNotifications, applications.size());

      List<Future> datasetFutures = applications.stream()
        .flatMap(app -> {
          String applicationId = app.getString("_id");

          return app.getJsonArray("datasets").stream()
            .map(obj -> (JsonObject) obj)
            .filter(dataset -> datasetHasExpiringPermit(dataset, statuses, now, notifyAt))
            .map(dataset -> {
              String datasetId = dataset.getString("dataset_id");
              String userId = app.getString("userId");
              NotificationCode datasetNotificationCode = resolvePermitExpirationNotificationCode(notificationCode, dataset);

              return upsertHistoryAndCreateNotification(applicationId, app.getString("title"), datasetId, userId, dayToDisplayNotifications, datasetNotificationCode, DATA_ACCESS_APPLICATION_COLLECTION);
            });
        })
        .collect(Collectors.toList());

      if (datasetFutures.isEmpty()) {
        promise.complete();
      } else {
        CompositeFuture.all(datasetFutures)
          .onComplete(ar -> {
            if (ar.succeeded()) {
              promise.complete();
            } else {
              promise.fail(ar.cause());
            }
          });
      }
    });

    return promise.future();
  }

  private Future<Void> processDatasetsThatAreAboutToWithdraw(
    String collection,
    Integer dayToDisplayNotifications,
    List<String> statuses,
    NotificationCode notificationCode) {

    Instant now = Instant.now();

    JsonObject query = createQueryForDatasetsThatAreAboutToWithdraw(statuses);
    JsonObject projection = new JsonObject()
      .put(ID, 1)
      .put(USER_ID, 1)
      .put(TITLE, 1)
      .put(DATASETS_DATASET_ID, 1)
      .put(DATASETS_DECISIONS_LATEST, 1)
      .put(DATASETS_DECISIONS_STATUS, 1)
      .put(DATASETS_DECISIONS_DATE_RECEIVED, 1);
    FindOptions options = new FindOptions().setFields(projection);
    Promise<Void> promise = Promise.promise();

    mongoClient.findWithOptions(collection, query, options, res -> {
      if (res.failed()) {
        promise.fail(res.cause());
        return;
      }

      List<JsonObject> applications = res.result();
      log.debug("[{}] Timeframe {}d → found {} applications",
        collection, dayToDisplayNotifications, applications.size());

      List<Future> datasetFutures = applications.stream()
        .flatMap(app -> {
          String applicationId = app.getString("_id");
          String userId = app.getString("userId");

          return app.getJsonArray("datasets").stream()
            .map(obj -> (JsonObject) obj)

            // Filter datasets by decision logic in Java
            .filter(dataset -> datasetMatchesWithdrawalWindow(
              dataset, statuses, now, dayToDisplayNotifications))

            .map(dataset -> {
              String datasetId = dataset.getString("dataset_id");

              return upsertHistoryAndCreateNotification(
                applicationId,
                app.getString("title"),
                datasetId,
                userId,
                dayToDisplayNotifications,
                notificationCode,
                collection
              );
            });
        })
        .collect(Collectors.toList());

      if (datasetFutures.isEmpty()) {
        promise.complete();
      } else {
        CompositeFuture.all(datasetFutures)
          .onSuccess(v -> promise.complete())
          .onFailure(promise::fail);
      }
    });

    return promise.future();
  }

  private boolean datasetMatchesWithdrawalWindow(
    JsonObject dataset,
    List<String> statuses,
    Instant now,
    Integer dayToDisplayNotifications) {

    if (dataset.getJsonArray("decisions") == null) return false;

    return dataset.getJsonArray("decisions").stream()
      .map(o -> (JsonObject) o)
      .anyMatch(decision -> {

        if (decision.getInteger("latest", 0) != 1) return false;
        if (!statuses.contains(decision.getString("datasetStatus"))) return false;
        if (!decision.containsKey("decision_date_received")) return false;

        try {
          Instant decisionDate = Instant.parse(decision.getString("decision_date_received"));
          Instant withdrawnDate = decisionDate.plus(Duration.ofDays(withdrawnTimePeriod));
          Instant notifyFrom = withdrawnDate.minus(Duration.ofDays(dayToDisplayNotifications));

          boolean shouldNotify = !now.isBefore(notifyFrom) && !now.isAfter(withdrawnDate);

          if (shouldNotify) {
            log.debug(
              "Dataset {} qualifies → withdrawnDate={}, notifyFrom={}",
              dataset.getString("dataset_id"),
              withdrawnDate,
              notifyFrom
            );
          }

          return shouldNotify;

        } catch (Exception e) {
          log.warn("Invalid decision_date_received: {}", decision.getString("decision_date_received"));
          return false;
        }
      });
  }

  private Future<Void> processCatalogsThatAreAboutToWithdraw(
      String collection,
      Integer dayToDisplayNotifications,
      List<String> statuses,
      NotificationCode notificationCode) {

    Instant now = Instant.now();

    JsonObject query = createQueryForCatalogsThatAreAboutToWithdraw(statuses);
    JsonObject projection = new JsonObject()
      .put(ID, 1)
      .put(USER_ID, 1)
      .put(TITLE, 1)
      .put(CATALOGS_ID, 1)
      .put(CATALOGS_STATUSES_LATEST, 1)
      .put(CATALOGS_STATUSES_STATUS, 1)
      .put(CATALOGS_STATUSES_DATE_RECEIVED, 1);
    FindOptions options = new FindOptions().setFields(projection);

    Promise<Void> promise = Promise.promise();

    mongoClient.findWithOptions(collection, query, options, res -> {
      if (res.failed()) {
        promise.fail(res.cause());
        return;
      }

      List<JsonObject> applications = res.result();

      List<Future> catalogFutures = applications.stream()
          .flatMap(app -> {

            String applicationId = app.getString("_id");
            String userId = app.getString("userId");

            return app.getJsonArray("catalogs").stream()
                .map(o -> (JsonObject) o)

                .filter(catalog -> catalogMatchesWithdrawalWindow(
                    catalog, statuses, now, dayToDisplayNotifications))

                .map(catalog -> {
                  String catalogId = catalog.getString("catalog_id");

                  return upsertHistoryAndCreateCatalogNotification(
                      applicationId,
                    app.getString("title"),
                      catalogId,
                      userId,
                      dayToDisplayNotifications,
                      collection,
                      notificationCode
                  );
                });
          })
          .collect(Collectors.toList());

      if (catalogFutures.isEmpty()) {
        promise.complete();
      } else {
        CompositeFuture.all(catalogFutures)
            .onSuccess(v -> promise.complete())
            .onFailure(promise::fail);
      }
    });

    return promise.future();
  }

  private JsonObject createQueryForCatalogsThatAreAboutToWithdraw(List<String> statuses) {
    return new JsonObject()
        .put("catalogs.statuses", new JsonObject()
            .put("$elemMatch", new JsonObject()
                .put("latest", 1)
                .put("status", new JsonObject().put("$in", statuses))
                .put("date_received", new JsonObject().put("$exists", true))
            )
        );
  }

  private boolean catalogMatchesWithdrawalWindow(
      JsonObject catalog,
      List<String> statuses,
      Instant now,
      Integer dayToDisplayNotifications) {

    if (catalog.getJsonArray("statuses") == null) return false;

    return catalog.getJsonArray("statuses").stream()
        .map(o -> (JsonObject) o)
        .anyMatch(statusObj -> {

          if (statusObj.getInteger("latest", 0) != 1) return false;
          if (!statuses.contains(statusObj.getString("status"))) return false;
          if (!statusObj.containsKey("date_received")) return false;

          try {
            Instant receivedDate =
                Instant.parse(statusObj.getString("date_received"));

            Instant withdrawnDate = receivedDate.plus(Duration.ofDays(withdrawnTimePeriod));
            Instant notifyFrom =
                withdrawnDate.minus(Duration.ofDays(dayToDisplayNotifications));

            boolean shouldNotify =
                !now.isBefore(notifyFrom) && !now.isAfter(withdrawnDate);

            if (shouldNotify) {
              log.debug(
                  "Catalog {} qualifies → withdrawnDate={}, notifyFrom={}",
                  catalog.getString("catalog_id"),
                  withdrawnDate,
                  notifyFrom
              );
            }

            return shouldNotify;

          } catch (Exception e) {
            log.warn("Invalid date_received: {}",
                statusObj.getString("date_received"));
            return false;
          }
        });
  }

  private Future<Void> upsertHistoryAndCreateCatalogNotification(
      String applicationId,
      String applicationTitle,
      String catalogId,
      String userId,
      Integer dayToDisplayNotifications,
      String collection,
      NotificationCode notificationCode) {

    Promise<Void> promise = Promise.promise();

    JsonObject historyQuery = new JsonObject()
        .put("applicationId", applicationId)
        .put("catalogId", catalogId)
        .put("notificationCode", notificationCode.getCode())
        .put("notificationTimeFrame", dayToDisplayNotifications + "d");

    JsonObject historyInsert = new JsonObject()
        .put("$setOnInsert", new JsonObject()
            .put("applicationId", applicationId)
            .put("catalogId", catalogId)
            .put("notificationCode", notificationCode.getCode())
            .put("notificationTimeFrame", dayToDisplayNotifications + "d")
            .put("date_created", Instant.now().toString())
        );

    mongoClient.updateCollectionWithOptions(
        NOTIFICATION_HISTORY_COLLECTION,
        historyQuery,
        historyInsert,
        new UpdateOptions().setUpsert(true),
        res -> {
          if (res.failed()) {
            promise.fail(res.cause());
            return;
          }

          boolean isNew = res.result().getDocMatched() == 0;

          if (!isNew) {
            log.debug("History exists → skipping notification (app={}, catalog={}, dayToDisplayNotifications={}d)",
                applicationId, catalogId, dayToDisplayNotifications);
            promise.complete();
            return;
          }

          log.debug("New history inserted → creating notification (app={}, catalog={}, dayToDisplayNotifications={}d)",
              applicationId, catalogId, dayToDisplayNotifications);

          handleCatalog(applicationId, applicationTitle, userId, dayToDisplayNotifications, collection, notificationCode)
              .onSuccess(v -> promise.complete())
              .onFailure(promise::fail);
        }
    );

    return promise.future();
  }

  /**
   * Helper to check if a permit's expiration date is within now and notifyAt
   */
  private boolean permitExpirationWithin(JsonObject permit, Instant now, Instant notifyAt) {
    if (!permit.containsKey("permit_expiration_date")) return false;
    Instant expiration = Instant.parse(permit.getString("permit_expiration_date"));
    return expiration.isAfter(now) && !expiration.isAfter(notifyAt);
  }

  private boolean datasetHasExpiringPermit(
    JsonObject dataset,
    List<String> statuses,
    Instant now,
    Instant notifyAt) {

    return dataset.getJsonArray("permits", new JsonArray()).stream()
      .map(o -> (JsonObject) o)
      .anyMatch(permit -> permit.getInteger("latest", 0) == 1
        && statuses.contains(permit.getString("permit_status"))
        && permitExpirationWithin(permit, now, notifyAt));
  }

  private NotificationCode resolvePermitExpirationNotificationCode(
    NotificationCode defaultNotificationCode,
    JsonObject dataset) {

    if (defaultNotificationCode != NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING) {
      return defaultNotificationCode;
    }

    return hasRenewedPermit(dataset)
      ? NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW
      : defaultNotificationCode;
  }

  private boolean hasRenewedPermit(JsonObject dataset) {
    return dataset.getJsonArray("permits", new JsonArray()).stream()
      .map(o -> (JsonObject) o)
      .anyMatch(permit -> PermitStatus.RENEWED.getLabel().equalsIgnoreCase(permit.getString("permit_status", "")));
  }

  /**
   * Handle individual dataset notification creation
   */
  private Future<Void> handleDataset(
    String applicationId,
    String applicationTitle,
    String datasetId,
    String userId,
    Integer dayToDisplayNotifications,
    NotificationCode notificationCode,
    String collection
  ) {

    Map<String, Object> properties = new HashMap<>();
    properties.put("DATASET_ID", datasetId);
    properties.put("APPLICATION_NAME", applicationTitle);
    properties.put("NOTIFICATION_DAY", dayToDisplayNotifications.toString());

    return notificationManager.createNotifications(
      notificationCode,
      applicationId,
      collection,
      properties,
      userId
    );
  }

  private Future<Void> handleCatalog(
    String applicationId,
    String applicationTitle,
    String userId,
    Integer dayToDisplayNotifications,
    String collection,
    NotificationCode notificationCode
  ) {

    Map<String, Object> properties = new HashMap<>();
    properties.put("APPLICATION_NAME", applicationTitle);
    properties.put("NOTIFICATION_DAY", dayToDisplayNotifications.toString());

    return notificationManager.createNotifications(
      notificationCode,
      applicationId,
      collection,
      properties,
      userId
    );
  }

  private JsonObject createQueryForPermitsThatAreAboutToExpire(
    Integer timeFrame,
    List<String> permitStatusesThatExpire) {

    Instant now = Instant.now();
    Instant notifyAt = now.plus(Duration.ofDays(timeFrame));

    return new JsonObject()
      .put("datasets", new JsonObject()
        .put("$elemMatch", new JsonObject()
          .put("permits", new JsonObject()
            .put("$elemMatch", new JsonObject()
              .put("latest", 1)
              .put("permit_status", new JsonObject().put("$in", permitStatusesThatExpire))
              .put("permit_expiration_date", new JsonObject()
                .put("$lte", notifyAt.toString())
                .put("$gt", now.toString())
              )
            )
          )
        )
      );
  }

  private JsonObject createQueryForDatasetsThatAreAboutToWithdraw(
    List<String> datasetStatusesThatWithdraw) {

    return new JsonObject()
      .put("datasets.decisions", new JsonObject()
        .put("$elemMatch", new JsonObject()
          .put("latest", 1)
          .put("datasetStatus", new JsonObject()
            .put("$in", datasetStatusesThatWithdraw))
          .put("decision_date_received", new JsonObject()
            .put("$exists", true))
        )
      );
  }


  private Future<Void> upsertHistoryAndCreateNotification(
    String applicationId,
    String applicationTitle,
    String datasetId,
    String userId,
    Integer dayToDisplayNotifications,
    NotificationCode notificationCode,
    String collection) {

    Promise<Void> promise = Promise.promise();

    JsonObject historyQuery = new JsonObject()
      .put("applicationId", applicationId)
      .put("datasetId", datasetId)
      .put("notificationCode", notificationCode.getCode())
      .put("notificationTimeFrame", dayToDisplayNotifications + "d");

    JsonObject historyInsert = new JsonObject()
      .put("$setOnInsert", new JsonObject()
        .put("applicationId", applicationId)
        .put("datasetId", datasetId)
        .put("notificationCode", notificationCode.getCode())
        .put("notificationTimeFrame", dayToDisplayNotifications + "d")
        .put("date_created", Instant.now().toString())
      );

    mongoClient.updateCollectionWithOptions(
      NOTIFICATION_HISTORY_COLLECTION,
      historyQuery,
      historyInsert,
      new UpdateOptions().setUpsert(true),
      res -> {
        if (res.failed()) {
          promise.fail(res.cause());
          return;
        }

        boolean isNew = res.result().getDocMatched() == 0;

        if (!isNew) {
          log.debug("History exists → skipping notification (app={}, dataset={}, dayToDisplayNotifications={}d)",
            applicationId, datasetId, dayToDisplayNotifications);
          promise.complete();
          return;
        }

        log.debug("New history inserted → creating notification (app={}, dataset={}, dayToDisplayNotifications={}d)",
          applicationId, datasetId, dayToDisplayNotifications);

        handleDataset(applicationId, applicationTitle, datasetId, userId, dayToDisplayNotifications, notificationCode, collection)
          .onSuccess(v -> promise.complete())
          .onFailure(promise::fail);
      }
    );

    return promise.future();
  }

  private Future<Void> cleanupOldNotificationHistory() {

    Promise<Void> promise = Promise.promise();

    Instant sixMonthsAgo = Instant.now().minus(Duration.ofDays(180));

    JsonObject deleteQuery = new JsonObject()
      .put("date_created", new JsonObject()
        .put("$lt", sixMonthsAgo.toString())
      );

    mongoClient.removeDocuments(
      NOTIFICATION_HISTORY_COLLECTION,
      deleteQuery,
      res -> {
        if (res.succeeded()) {
          log.info("Cleanup completed → removed {} old records",
            res.result().getRemovedCount());
          promise.complete();
        } else {
          promise.fail(res.cause());
        }
      }
    );

    return promise.future();
  }

}
