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
package eu.europa.data.health.access.manager;

import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION_ACCESS_SCHEDULER;
import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION_REQUEST_SCHEDULER;
import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION_SYNCHRONIZATION;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.WITHDRAW_ACCESS_SCHEDULER;
import static eu.europa.data.health.access.Constants.WITHDRAW_REQUEST_SCHEDULER;
import static eu.europa.data.health.access.Constants.WITHDRAW_SYNCHRONIZATION;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.scheduled_jobs.DispatcherScheduledJob;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.NotificationManager;
import eu.europa.data.health.access.services.SchedulerService;
import eu.europa.data.health.access.utils.TimeUtilities;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerManager {

  public static final String SCHEDULER = "Scheduler {} {}";
  private static final String NOT_RUNNING = "0";
  private static final String RUNNING = "1";
  private final MongoClient mongoClient;
  private final Logger log = LoggerFactory.getLogger(getClass());

  public SchedulerManager(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }
  /**
   * Schedules two daily jobs that scan MongoDB for applications/datasets/decisions eligible for
   * withdrawal and dispatches them for processing to dispatcher.
   *
   * Coordination notes:
   * - Uses a shared DispatcherScheduledJob that contains the actual job logic.
   * - Each scheduled timer targets a different collection and scheduler name.
   * - Duplicate execution protection is handled inside scheduleWithdrawForCollection(...).
   *
   * @param dispatcherManager business component that executes withdraw actions
   * @param auditLogManager auditing component for emitted events
   * @param schedulerService service that tracks scheduler state (RUNNING/NOT_RUNNING/EXPIRED)
   * @param config configuration (thresholds)
   * @param vertx Vert.x instance used to register timers
   * @param dbHandler low-level DB access helper
   */
  public void scheduleWithdrawApplicationJob(DispatcherManager dispatcherManager,
    AuditLogManager auditLogManager, SchedulerService schedulerService, JsonObject config,
    Vertx vertx, DBHandler dbHandler, NotificationManager notificationManager) {

    int withdrawThresholdDays = config.getInteger(Constants.WITHDRAW_THRESHOLD_DAYS) != null
      ? config.getInteger(Constants.WITHDRAW_THRESHOLD_DAYS)
      : Constants.WITHDRAWN_TIME_DEFAULT_PERIOD;
    LocalTime withdrawRunTime = resolveWithdrawRunTime(config);

    int additionalInfoWithdrawThresholdDays = config.getInteger(Constants.ADDITIONAL_INFO_WITHDRAW_THRESHOLD_DAYS) != null
      ? config.getInteger(Constants.ADDITIONAL_INFO_WITHDRAW_THRESHOLD_DAYS)
      : Constants.WITHDRAWN_TIME_DEFAULT_PERIOD;

    DispatcherScheduledJob dispatcherScheduledJob = new DispatcherScheduledJob(dbHandler,
      mongoClient, dispatcherManager, auditLogManager, notificationManager, config);

    scheduleDecisionWithdrawForCollection(vertx, schedulerService, dispatcherScheduledJob,
      withdrawThresholdDays, withdrawRunTime, WITHDRAW_ACCESS_SCHEDULER,
      DATA_ACCESS_APPLICATION_COLLECTION);

    scheduleDecisionWithdrawForCollection(vertx, schedulerService, dispatcherScheduledJob,
      withdrawThresholdDays, withdrawRunTime, WITHDRAW_REQUEST_SCHEDULER,
      DATA_REQUEST_APPLICATION_COLLECTION);

    scheduleAdditionalInformationWithdrawForCollection(vertx, schedulerService,
      dispatcherScheduledJob,
      additionalInfoWithdrawThresholdDays, withdrawRunTime, ADDITIONAL_INFORMATION_ACCESS_SCHEDULER,
      DATA_ACCESS_APPLICATION_COLLECTION);

    scheduleAdditionalInformationWithdrawForCollection(vertx, schedulerService,
      dispatcherScheduledJob,
      additionalInfoWithdrawThresholdDays, withdrawRunTime,
      ADDITIONAL_INFORMATION_REQUEST_SCHEDULER,
      DATA_REQUEST_APPLICATION_COLLECTION);

  }

  private void scheduleDecisionWithdrawForCollection(Vertx vertx,
    SchedulerService schedulerService,
    DispatcherScheduledJob dispatcherScheduledJob, int withdrawThresholdDays,
    LocalTime withdrawRunTime,
    String schedulerName, String collectionName) {

    scheduleNextWithdrawRun(vertx, schedulerService, dispatcherScheduledJob,
      withdrawThresholdDays, withdrawRunTime, schedulerName, collectionName);
  }

  private void scheduleAdditionalInformationWithdrawForCollection(Vertx vertx,
    SchedulerService schedulerService, DispatcherScheduledJob dispatcherScheduledJob,
    int expirationDays, LocalTime withdrawRunTime, String schedulerName,
    String collectionName) {

    scheduleNextAdditionalInformationWithdrawRun(vertx, schedulerService, dispatcherScheduledJob,
      expirationDays, withdrawRunTime, schedulerName, collectionName);
  }

  private void scheduleNextWithdrawRun(Vertx vertx, SchedulerService schedulerService,
    DispatcherScheduledJob dispatcherScheduledJob, int withdrawThresholdDays,
    LocalTime withdrawRunTime,
    String schedulerName, String collectionName) {

    long delay = millisUntilNextTime(withdrawRunTime);
    log.debug("{} scheduled for collection {} at {} (in {} ms)", schedulerName,
      collectionName, withdrawRunTime, delay);

    vertx.setTimer(delay,
      id -> runWithdrawForCollection(schedulerService, dispatcherScheduledJob,
        withdrawThresholdDays, schedulerName, collectionName)
        .onComplete(ar -> scheduleNextWithdrawRun(vertx, schedulerService,
          dispatcherScheduledJob, withdrawThresholdDays, withdrawRunTime, schedulerName,
          collectionName)));
  }

  private void scheduleNextAdditionalInformationWithdrawRun(Vertx vertx,
    SchedulerService schedulerService, DispatcherScheduledJob dispatcherScheduledJob,
    int expirationDays, LocalTime withdrawRunTime, String schedulerName,
    String collectionName) {

    long delay = millisUntilNextTime(withdrawRunTime);
    log.debug("{} scheduled for collection {} at {} (in {} ms)", schedulerName,
      collectionName, withdrawRunTime, delay);

    vertx.setTimer(delay,
      id -> runAdditionalInformationWithdrawForCollection(schedulerService,
        dispatcherScheduledJob, expirationDays, schedulerName, collectionName)
        .onComplete(ar -> scheduleNextAdditionalInformationWithdrawRun(vertx,
          schedulerService, dispatcherScheduledJob, expirationDays, withdrawRunTime,
          schedulerName, collectionName)));
  }

  private LocalTime resolveWithdrawRunTime(JsonObject config) {
    String configuredRunTime = config.getString(Constants.WITHDRAW_RUN_TIME);
    String valueToParse = configuredRunTime == null || configuredRunTime.isBlank()
      ? Constants.WITHDRAW_RUN_TIME_DEFAULT
      : configuredRunTime.trim();

    try {
      return LocalTime.parse(valueToParse);
    } catch (DateTimeParseException ex) {
      log.warn("Invalid {} value '{}'. Falling back to {}", Constants.WITHDRAW_RUN_TIME,
        configuredRunTime, Constants.WITHDRAW_RUN_TIME_DEFAULT);
      return LocalTime.parse(Constants.WITHDRAW_RUN_TIME_DEFAULT);
    }
  }

  private Future<Void> runWithdrawForCollection(SchedulerService schedulerService,
    DispatcherScheduledJob dispatcherScheduledJob, int withdrawThresholdDays,
    String schedulerName, String collectionName) {

    Promise<Void> promise = Promise.promise();

    isSchedulerRunningOrExpired(schedulerName, schedulerService).onSuccess(isRunning -> {

      if (Boolean.TRUE.equals(isRunning)) {
        log.debug("{} already running, skipping this cycle", schedulerName);
        promise.complete();
        return;
      }

      updateSchedulerState(schedulerName, schedulerService, RUNNING).compose(v -> {
        log.debug("{} launched for collection {}", WITHDRAW_SYNCHRONIZATION, collectionName);

        return dispatcherScheduledJob.sendWithDrawApplicationsToDispatcher(collectionName,
          withdrawThresholdDays);
      }).onComplete(ar -> updateSchedulerState(schedulerName, schedulerService, NOT_RUNNING)
        .onComplete(reset -> {
          if (ar.failed()) {
            log.error("{} failed for collection {}: {}", WITHDRAW_SYNCHRONIZATION,
              collectionName, ar.cause().getMessage(), ar.cause());
          }
          promise.complete();
        }));
    }).onFailure(err -> {
      log.error("Scheduler check failed for {}", schedulerName, err);
      promise.complete();
    });

    return promise.future();
  }

  private Future<Void> runAdditionalInformationWithdrawForCollection(
    SchedulerService schedulerService, DispatcherScheduledJob dispatcherScheduledJob,
    int expirationDays, String schedulerName, String collectionName) {

    Promise<Void> promise = Promise.promise();

    isSchedulerRunningOrExpired(schedulerName, schedulerService).onSuccess(isRunning -> {

      if (Boolean.TRUE.equals(isRunning)) {
        log.debug("{} already running, skipping this cycle", schedulerName);
        promise.complete();
        return;
      }

      updateSchedulerState(schedulerName, schedulerService, RUNNING).compose(v -> {
        log.debug("{} launched for collection {}", ADDITIONAL_INFORMATION_SYNCHRONIZATION,
          collectionName);

        return dispatcherScheduledJob.withdrawCatalogsWithExpiredAdditionalInformation(
          collectionName, expirationDays);
      }).onComplete(ar -> updateSchedulerState(schedulerName, schedulerService, NOT_RUNNING)
        .onComplete(reset -> {
          if (ar.failed()) {
            log.error("{} failed for collection {}: {}",
              ADDITIONAL_INFORMATION_SYNCHRONIZATION, collectionName,
              ar.cause().getMessage(), ar.cause());
          }
          promise.complete();
        }));
    }).onFailure(err -> {
      log.error("Scheduler check failed for {}", schedulerName, err);
      promise.complete();
    });

    return promise.future();
  }

  private long millisUntilNextTime(LocalTime targetTime) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextRun = now.toLocalDate().atTime(targetTime);

    if (!nextRun.isAfter(now)) {
      nextRun = nextRun.plusDays(1);
    }

    return Duration.between(now, nextRun).toMillis();
  }
  private Future<Boolean> isSchedulerRunningOrExpired(String schedulerName,
    SchedulerService schedulerService) {
    Promise<Boolean> promise = Promise.promise();

    schedulerService.findByField(Constants.FIELD_SCHEDULER_NAME, schedulerName,
      Constants.COLLECTION_SCHEDULERS_STATUS).onSuccess(result -> {
      String isRunning = result.get(0).getString("is_running");
      if ("1".equals(isRunning)) {
        boolean isSchedulerCrashed = !TimeUtilities.hasThirtyMinutesPassed(
          result.get(0).getInstant("status_changed_at"));
        promise.complete(isSchedulerCrashed);
      } else {
        promise.complete(false);
      }
    }).onFailure(throwable -> {
      if (throwable.getMessage().contains("Not Found")) {
        schedulerService.createNewRecordForScheduler(schedulerName).onSuccess(s -> {
          log.info(SCHEDULER, schedulerName, " created successfully");
          promise.complete(false); // scheduler was not running, we just created it

        }).onFailure(throwable1 -> {
          log.error(SCHEDULER, schedulerName, " creation failed", throwable1);
          promise.complete(false);
        });
      } else {
        log.error("Scheduler failed", throwable);
        promise.complete(false);
      }
    });

    return promise.future();
  }

  private Future<Void> updateSchedulerState(String schedulerName, SchedulerService schedulerService,
    String state) {
    Promise<Void> promise = Promise.promise();

    schedulerService.updateSchedulerStatus(schedulerName, state).onSuccess(result -> {
      log.debug(SCHEDULER, schedulerName, " updated successfully");
      promise.complete();
    }).onFailure(throwable -> {
      log.error(SCHEDULER, schedulerName, " update failed", throwable);
      promise.complete();
    });
    return promise.future();
  }

  /**
   * Schedules periodic dispatcher jobs for sending and retrieving applications.
   *
   * @param dispatcherManager business component for dispatcher operations
   * @param dispatcherClient client for dispatcher communication
   * @param auditLogManager auditing component
   * @param notificationManager notification component
   * @param schedulerService service that tracks scheduler state
   * @param config configuration (intervals/retries)
   * @param vertx Vert.x instance for periodic timers
   * @param dbHandler low-level DB access helper
   */
  public void scheduleDispatcherJobs(DispatcherManager dispatcherManager,
    eu.europa.data.health.access.dispatcher_client.DispatcherClient dispatcherClient,
    AuditLogManager auditLogManager, NotificationManager notificationManager,
    SchedulerService schedulerService, JsonObject config, Vertx vertx, DBHandler dbHandler) {

    // Data Access Application Dispatcher
    int dataAccessDispatcherInterval = Constants.DATA_ACCESS_DISPATCHER_DEFAULT_INTERVAL;
    if (config.getInteger(Constants.DATA_ACCESS_DISPATCHER_INTERVAL) != null) {
      dataAccessDispatcherInterval = config.getInteger(Constants.DATA_ACCESS_DISPATCHER_INTERVAL);
    }

    final int dataAccessDispatcherMaxRetries;
    if (config.getInteger(Constants.DATA_ACCESS_DISPATCHER_MAX_RETRIES) != null) {
      dataAccessDispatcherMaxRetries = config.getInteger(
        Constants.DATA_ACCESS_DISPATCHER_MAX_RETRIES);
    } else {
      dataAccessDispatcherMaxRetries = Constants.DATA_ACCESS_DISPATCHER_DEFAULT_MAX_RETRIES;
    }

    int dataAccessDispatcherForSentApplicationsInterval = Constants.DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_INTERVAL;
    if (config.getInteger(Constants.DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL)
      != null) {
      dataAccessDispatcherForSentApplicationsInterval = config.getInteger(
        Constants.DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL);
    }

    final int dataAccessDispatcherForSentApplicationsMaxRetries;
    if (config.getInteger(Constants.DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES)
      != null) {
      dataAccessDispatcherForSentApplicationsMaxRetries = config.getInteger(
        Constants.DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES);
    } else {
      dataAccessDispatcherForSentApplicationsMaxRetries = Constants.DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_MAX_RETRIES;
    }

    // Data Request Application Dispatcher
    int dataRequestDispatcherInterval = Constants.DATA_REQUEST_DISPATCHER_DEFAULT_INTERVAL;
    if (config.getInteger(Constants.DATA_REQUEST_DISPATCHER_INTERVAL) != null) {
      dataRequestDispatcherInterval = config.getInteger(Constants.DATA_REQUEST_DISPATCHER_INTERVAL);
    }

    final int dataRequestDispatcherMaxRetries;
    if (config.getInteger(Constants.DATA_REQUEST_DISPATCHER_MAX_RETRIES) != null) {
      dataRequestDispatcherMaxRetries = config.getInteger(
        Constants.DATA_REQUEST_DISPATCHER_MAX_RETRIES);
    } else {
      dataRequestDispatcherMaxRetries = Constants.DATA_REQUEST_DISPATCHER_DEFAULT_MAX_RETRIES;
    }

    int dataRequestDispatcherForSentApplicationsInterval = Constants.DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_INTERVAL;
    if (config.getInteger(Constants.DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL)
      != null) {
      dataRequestDispatcherForSentApplicationsInterval = config.getInteger(
        Constants.DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL);
    }

    final int dataRequestDispatcherForSentApplicationsMaxRetries;
    if (config.getInteger(Constants.DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES)
      != null) {
      dataRequestDispatcherForSentApplicationsMaxRetries = config.getInteger(
        Constants.DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES);
    } else {
      dataRequestDispatcherForSentApplicationsMaxRetries = Constants.DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_MAX_RETRIES;
    }

    DispatcherScheduledJob dispatcherScheduledJob = new DispatcherScheduledJob(dbHandler,
      mongoClient, dispatcherManager, auditLogManager, notificationManager,config);

    // Schedule DATA_ACCESS_DISPATCHER
    scheduleDispatcherForCollection(vertx, schedulerService, dispatcherScheduledJob,
      dataAccessDispatcherInterval, dataAccessDispatcherMaxRetries, "DATA_ACCESS_DISPATCHER",
      DATA_ACCESS_APPLICATION_COLLECTION, dispatcherClient, true);

    // Schedule DATA_ACCESS_SENT_STATUS_APPLICATIONS
    scheduleDispatcherForCollection(vertx, schedulerService, dispatcherScheduledJob,
      dataAccessDispatcherForSentApplicationsInterval,
      dataAccessDispatcherForSentApplicationsMaxRetries, "DATA_ACCESS_SENT_STATUS_APPLICATIONS",
      DATA_ACCESS_APPLICATION_COLLECTION, dispatcherClient, false);

    // Schedule DATA_REQUEST_DISPATCHER
    scheduleDispatcherForCollection(vertx, schedulerService, dispatcherScheduledJob,
      dataRequestDispatcherInterval, dataRequestDispatcherMaxRetries, "DATA_REQUEST_DISPATCHER",
      DATA_REQUEST_APPLICATION_COLLECTION, dispatcherClient, true);

    // Schedule DATA_REQUEST_SENT_STATUS_APPLICATIONS
    scheduleDispatcherForCollection(vertx, schedulerService, dispatcherScheduledJob,
      dataRequestDispatcherForSentApplicationsInterval,
      dataRequestDispatcherForSentApplicationsMaxRetries, "DATA_REQUEST_SENT_STATUS_APPLICATIONS",
      DATA_REQUEST_APPLICATION_COLLECTION, dispatcherClient, false);
  }

  private void scheduleDispatcherForCollection(Vertx vertx, SchedulerService schedulerService,
    DispatcherScheduledJob dispatcherScheduledJob, int interval, int maxRetries,
    String schedulerName, String collectionName,
    eu.europa.data.health.access.dispatcher_client.DispatcherClient dispatcherClient,
    boolean isSendJob) {

    vertx.setPeriodic(interval,
      id -> isSchedulerRunningOrExpired(schedulerName, schedulerService).onSuccess(isRunning -> {

        if (Boolean.TRUE.equals(isRunning)) {
          log.debug("{} already running, skipping this cycle", schedulerName);
          return;
        }

        updateSchedulerState(schedulerName, schedulerService, RUNNING).compose(v -> {
          log.debug("{} launched for collection {}", schedulerName, collectionName);

          if (isSendJob) {
            return dispatcherScheduledJob.sendApplicationsToDispatcher(maxRetries, dispatcherClient,
              collectionName);
          } else {
            return dispatcherScheduledJob.retrieveSentApplications(collectionName, maxRetries);
          }
        }).onComplete(ar -> updateSchedulerState(schedulerName, schedulerService, NOT_RUNNING));
      }).onFailure(
        err -> log.error("Scheduler check failed for {}: {}", schedulerName, err.getMessage())));
  }

}
