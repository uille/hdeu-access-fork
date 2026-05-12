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
import static eu.europa.data.health.access.Constants.DATASET_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.NOTIFICATION_HISTORY_COLLECTION;
import static eu.europa.data.health.access.Constants.PERMIT_NOTIFICATION_TYPE;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.services.NotificationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.mongo.UpdateOptions;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationScheduledJobTest {

  @Mock
  private MongoClient mongoClient;

  @Mock
  private NotificationManager notificationManager;

  private NotificationScheduledJob notificationScheduledJob;

  @BeforeEach
  void setUp() {
    notificationScheduledJob = new NotificationScheduledJob(mongoClient, notificationManager, 30);

    lenient().when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.succeededFuture());

    lenient().doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
      when(updateResult.getDocMatched()).thenReturn(0L);
      handler.handle(Future.succeededFuture(updateResult));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(NOTIFICATION_HISTORY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class),
      any()
    );
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Existing permit tests
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processPermitsThatAreAboutToExpire_shouldUseWithRenewCode_whenRenewedPermitExistsInHistory() {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("GRANTED", 1, 3),
      permit("RENEWED", 0, 30)
    ))));

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED", "RENEWED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(result.succeeded());

    verify(notificationManager).createNotifications(
      eq(NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );
  }

  @Test
  void processPermitsThatAreAboutToExpire_shouldUseWithRenewCode_whenLatestPermitIsRenewed() {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("RENEWED", 1, 3),
      permit("GRANTED", 0, 30)
    ))));

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED", "RENEWED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(result.succeeded());

    verify(notificationManager).createNotifications(
      eq(NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );
  }

  @Test
  void processPermitsThatAreAboutToExpire_shouldKeepDefaultCode_whenPermitWasNeverRenewed() {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("GRANTED", 1, 3),
      permit("AMENDED", 0, 30)
    ))));

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED", "RENEWED", "AMENDED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(result.succeeded());

    ArgumentCaptor<NotificationCode> notificationCodeCaptor = ArgumentCaptor.forClass(NotificationCode.class);
    verify(notificationManager).createNotifications(
      notificationCodeCaptor.capture(),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );

    assertSame(NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING, notificationCodeCaptor.getValue());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processPermitsThatAreAboutToExpire – extra branches
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processPermitsThatAreAboutToExpire_noMatchingDatasets_noNotificationCreated() {
    // Permit already expired → not in window → no notification
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("GRANTED", 1, -5) // expired 5 days ago
    ))));

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      3,
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(result.succeeded());
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processPermitsThatAreAboutToExpire_emptyApplicationList_completesSuccessfully() {
    stubFindApplications(List.of());

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(result.succeeded());
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processPermitsThatAreAboutToExpire_mongoQueryFails_futureFailed() {
    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture("Mongo failure"));
      return null;
    }).when(mongoClient).findWithOptions(eq(DATA_ACCESS_APPLICATION_COLLECTION),
      any(JsonObject.class), any(FindOptions.class), any());

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(result.failed());
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processPermitsThatAreAboutToExpire_historyAlreadyExists_skipsNotification() {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("GRANTED", 1, 3)
    ))));

    // docMatched = 1 → history already exists → skip
    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      MongoClientUpdateResult result = mock(MongoClientUpdateResult.class);
      when(result.getDocMatched()).thenReturn(1L);
      handler.handle(Future.succeededFuture(result));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(NOTIFICATION_HISTORY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class),
      any()
    );

    Future<Void> future = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(future.succeeded());
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processPermitsThatAreAboutToExpire_upsertFails_futureCompletes() {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("GRANTED", 1, 3)
    ))));

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      handler.handle(Future.failedFuture("upsert error"));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(NOTIFICATION_HISTORY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class),
      any()
    );

    Future<Void> future = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING
    );

    assertTrue(future.failed());
  }

  @Test
  void processPermitsThatAreAboutToExpire_nonApproachingNotificationCode_staysDefault() {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("RENEWED", 1, 3)
    ))));

    NotificationCode otherCode = NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW;

    Future<Void> result = callProcessPermitsThatAreAboutToExpire(
      notificationScheduledJob,
      new Class<?>[] {Integer.class, List.class, NotificationCode.class},
      5,
      List.of("RENEWED"),
      otherCode
    );

    assertTrue(result.succeeded());

    verify(notificationManager).createNotifications(
      eq(otherCode),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // createScheduledNotifications – public entry point
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void createScheduledNotifications_nullDays_skipsJob() {
    notificationScheduledJob.createScheduledNotifications(
      null,
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      PERMIT_NOTIFICATION_TYPE
    );

    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void createScheduledNotifications_emptyDays_skipsJob() {
    notificationScheduledJob.createScheduledNotifications(
      Set.of(),
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      PERMIT_NOTIFICATION_TYPE
    );

    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void createScheduledNotifications_unknownType_logsAndDoesNothing() throws Exception {
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      "UNKNOWN_TYPE"
    );

    // give the CompositeFuture time to fire
    Thread.sleep(200);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void createScheduledNotifications_permitType_triggersPermitProcessing() throws Exception {
    stubFindApplications(List.of(applicationWithDataset(datasetWithPermits(
      permit("GRANTED", 1, 3)
    ))));
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("GRANTED"),
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      PERMIT_NOTIFICATION_TYPE
    );

    Thread.sleep(300);

    verify(notificationManager).createNotifications(
      eq(NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );
  }

  @Test
  void createScheduledNotifications_datasetType_triggersDatasetProcessing() throws Exception {
    Instant past = Instant.now().minus(Duration.ofDays(25));

    JsonObject matchingApp = new JsonObject()
      .put("_id", "app-1")
      .put("title", "App Title")
      .put("userId", "user-1")
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("dataset_id", "dataset-1")
        .put("decisions", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("datasetStatus", "APPROVED")
          .put("decision_date_received", past.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(matchingApp));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(10),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(400);

    verify(notificationManager).createNotifications(
      eq(NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );
  }

  @Test
  void createScheduledNotifications_catalogType_triggersCatalogProcessing() throws Exception {
    Instant past = Instant.now().minus(Duration.ofDays(25));

    JsonObject matchingApp = new JsonObject()
      .put("_id", "app-1")
      .put("title", "App Title")
      .put("userId", "user-1")
      .put("catalogs", new JsonArray().add(new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("statuses", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("status", "APPROVED")
          .put("date_received", past.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(matchingApp));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(10),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(400);

    verify(notificationManager).createNotifications(
      eq(NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING),
      eq("app-1"),
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      anyMap(),
      eq("user-1")
    );
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processDatasetsThatAreAboutToWithdraw – extra branches
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processDatasetsThatAreAboutToWithdraw_noDecisionsArray_skipsDataset() throws Exception {
    JsonObject appWithNoDecisions = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("dataset_id", "ds-1"))); // no "decisions" key

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(appWithNoDecisions));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processDatasetsThatAreAboutToWithdraw_decisionNotLatest_skipsDataset() throws Exception {
    Instant past = Instant.now().minus(Duration.ofDays(25));
    JsonObject appWithNonLatestDecision = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("dataset_id", "ds-1")
        .put("decisions", new JsonArray().add(new JsonObject()
          .put("latest", 0) // NOT latest
          .put("datasetStatus", "APPROVED")
          .put("decision_date_received", past.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(appWithNonLatestDecision));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processDatasetsThatAreAboutToWithdraw_decisionDateNotInWindow_skipsDataset() throws Exception {
    // Date received was only 1 day ago → withdrawal 30 days away, notify window 5d → outside window
    Instant recentDate = Instant.now().minus(Duration.ofDays(1));
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("dataset_id", "ds-1")
        .put("decisions", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("datasetStatus", "APPROVED")
          .put("decision_date_received", recentDate.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processDatasetsThatAreAboutToWithdraw_invalidDecisionDate_skipsDataset() throws Exception {
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("dataset_id", "ds-1")
        .put("decisions", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("datasetStatus", "APPROVED")
          .put("decision_date_received", "not-a-date")))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processDatasetsThatAreAboutToWithdraw_mongoFails_doesNotThrow() throws Exception {
    doAnswer(inv -> {
      Handler<AsyncResult<List<JsonObject>>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture("db error"));
      return null;
    }).when(mongoClient).findWithOptions(anyString(), any(), any(FindOptions.class), any());

    // cleanup is never reached when queries fail, so do not stub removeDocuments

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processDatasetsThatAreAboutToWithdraw_historyAlreadyExists_skipsNotification() throws Exception {
    Instant past = Instant.now().minus(Duration.ofDays(25));
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("dataset_id", "ds-1")
        .put("decisions", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("datasetStatus", "APPROVED")
          .put("decision_date_received", past.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    // Override: history exists (docMatched = 1) → skip
    doAnswer(inv -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = inv.getArgument(4);
      MongoClientUpdateResult r = mock(MongoClientUpdateResult.class);
      when(r.getDocMatched()).thenReturn(1L);
      handler.handle(Future.succeededFuture(r));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(NOTIFICATION_HISTORY_COLLECTION), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class), any());

    notificationScheduledJob.createScheduledNotifications(
      Set.of(10),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
      DATASET_NOTIFICATION_TYPE
    );

    Thread.sleep(400);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processCatalogsThatAreAboutToWithdraw – extra branches
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processCatalogsThatAreAboutToWithdraw_noStatusesArray_skipsApp() throws Exception {
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("catalogs", new JsonArray().add(new JsonObject()
        .put("catalog_id", "cat-1"))); // no "statuses"

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processCatalogsThatAreAboutToWithdraw_statusNotLatest_skips() throws Exception {
    Instant past = Instant.now().minus(Duration.ofDays(25));
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("catalogs", new JsonArray().add(new JsonObject()
        .put("catalog_id", "cat-1")
        .put("statuses", new JsonArray().add(new JsonObject()
          .put("latest", 0) // NOT latest
          .put("status", "APPROVED")
          .put("date_received", past.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processCatalogsThatAreAboutToWithdraw_invalidDateReceived_skips() throws Exception {
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("catalogs", new JsonArray().add(new JsonObject()
        .put("catalog_id", "cat-1")
        .put("statuses", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("status", "APPROVED")
          .put("date_received", "BAD_DATE")))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processCatalogsThatAreAboutToWithdraw_historyAlreadyExists_skipsNotification() throws Exception {
    Instant past = Instant.now().minus(Duration.ofDays(25));
    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put("title", "T")
      .put("userId", "u")
      .put("catalogs", new JsonArray().add(new JsonObject()
        .put("catalog_id", "cat-1")
        .put("statuses", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("status", "APPROVED")
          .put("date_received", past.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    doAnswer(inv -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = inv.getArgument(4);
      MongoClientUpdateResult r = mock(MongoClientUpdateResult.class);
      when(r.getDocMatched()).thenReturn(1L);
      handler.handle(Future.succeededFuture(r));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(NOTIFICATION_HISTORY_COLLECTION), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class), any());

    notificationScheduledJob.createScheduledNotifications(
      Set.of(10),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(400);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processCatalogsThatAreAboutToWithdraw_dateOutsideWindow_skips() throws Exception {
    Instant recentDate = Instant.now().minus(Duration.ofDays(1));
    JsonObject app = new JsonObject()
      .put("_id", "app-1").put("title", "T").put("userId", "u")
      .put("catalogs", new JsonArray().add(new JsonObject()
        .put("catalog_id", "cat-1")
        .put("statuses", new JsonArray().add(new JsonObject()
          .put("latest", 1)
          .put("status", "APPROVED")
          .put("date_received", recentDate.toString())))));

    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of(app));
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(300);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  @Test
  void processCatalogsThatAreAboutToWithdraw_emptyResults_completesSuccessfully() throws Exception {
    stubFindApplicationsForCollection(DATA_ACCESS_APPLICATION_COLLECTION, List.of());
    stubFindApplicationsForCollection(DATA_REQUEST_APPLICATION_COLLECTION, List.of());
    stubCleanup();

    notificationScheduledJob.createScheduledNotifications(
      Set.of(5),
      List.of("APPROVED"),
      NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
      CATALOG_NOTIFICATION_TYPE
    );

    Thread.sleep(200);
    verify(notificationManager, never()).createNotifications(any(), any(), any(), anyMap(), any());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private static <T> T callProcessPermitsThatAreAboutToExpire(Object target, Class<?>[] parameterTypes, Object... args) {
    try {
      Method method = target.getClass().getDeclaredMethod("processPermitsThatAreAboutToExpire", parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(target, args);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke private method processPermitsThatAreAboutToExpire", e);
    }
  }

  private void stubFindApplications(List<JsonObject> applications) {
    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(applications));
      return null;
    }).when(mongoClient).findWithOptions(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(FindOptions.class), any());
  }

  private void stubFindApplicationsForCollection(String collection, List<JsonObject> applications) {
    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(applications));
      return null;
    }).when(mongoClient).findWithOptions(eq(collection), any(JsonObject.class), any(FindOptions.class), any());
  }

  /** Stubs the removeDocuments call used by cleanupOldNotificationHistory */
  private void stubCleanup() {
    doAnswer(inv -> {
      Handler<AsyncResult<MongoClientDeleteResult>> handler = inv.getArgument(2);
      MongoClientDeleteResult dr = mock(MongoClientDeleteResult.class);
      when(dr.getRemovedCount()).thenReturn(0L);
      handler.handle(Future.succeededFuture(dr));
      return null;
    }).when(mongoClient).removeDocuments(eq(NOTIFICATION_HISTORY_COLLECTION), any(JsonObject.class), any());
  }

  private JsonObject applicationWithDataset(JsonObject dataset) {
    return new JsonObject()
      .put("_id", "app-1")
      .put("title", "Application A")
      .put("userId", "user-1")
      .put("datasets", new JsonArray().add(dataset));
  }

  private JsonObject datasetWithPermits(JsonObject... permits) {
    JsonArray permitArray = new JsonArray();
    for (JsonObject permit : permits) {
      permitArray.add(permit);
    }

    return new JsonObject()
      .put("dataset_id", "dataset-1")
      .put("permits", permitArray);
  }

  private JsonObject permit(String status, int latest, int expiresInDays) {
    return new JsonObject()
      .put("permit_status", status)
      .put("latest", latest)
      .put("permit_expiration_date", Instant.now().plus(Duration.ofDays(expiresInDays)).toString());
  }
}
