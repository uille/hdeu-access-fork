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


import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.DispatcherManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testcontainers.shaded.org.awaitility.Awaitility;

class DispatcherScheduledJobAdditionalTest {

  private DBHandler dbHandler;
  private MongoClient mongoClient;
  private DispatcherManager dispatcherManager;
  private AuditLogManager auditLogManager;

  private DispatcherScheduledJob job;

  @BeforeEach
  void setUp() {
    dbHandler = mock(DBHandler.class);
    mongoClient = mock(MongoClient.class);
    dispatcherManager = mock(DispatcherManager.class);
    auditLogManager = mock(AuditLogManager.class);

    job = new DispatcherScheduledJob(dbHandler, mongoClient, dispatcherManager, auditLogManager);
  }

  // -------------------------------------------------------------------------
  // Reflection helper (for private methods)
  // -------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private static <T> T callPrivate(Object target, String method, Class<?>[] types, Object... args) {
    try {
      Method m = findMethod(target.getClass(), method, types);
      m.setAccessible(true);
      return (T) m.invoke(target, args);
    } catch (Exception e) {
      throw new RuntimeException("Failed calling private method: " + method, e);
    }
  }

  private static Method findMethod(Class<?> clazz, String name, Class<?>[] types) throws NoSuchMethodException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredMethod(name, types);
      } catch (NoSuchMethodException ignored) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchMethodException(clazz.getName() + "." + name);
  }
  static class TestableDispatcherScheduledJob extends DispatcherScheduledJob {

    final AtomicReference<JsonObject> replacedApp = new AtomicReference<>();
    final AtomicReference<String> replacedCollection = new AtomicReference<>();
    final AtomicInteger changeCatalogToSubmittedCalls = new AtomicInteger();

    TestableDispatcherScheduledJob(DBHandler dbHandler, MongoClient mongoClient,
                                   DispatcherManager dispatcherManager, AuditLogManager auditLogManager) {
      super(dbHandler, mongoClient, dispatcherManager, auditLogManager);
    }

    // saveApplication(...) calls replaceApplication(...) → we observe persistence via this override
    @Override
    protected Future<JsonObject> replaceApplication(JsonObject app, String collectionName) {
      replacedApp.set(app);
      replacedCollection.set(collectionName);
      return Future.succeededFuture(app);
    }

    // handleDispatcherResponse(received) calls changeCatalogToSubmitted(...)
    @Override
    protected void changeCatalogToSubmitted(JsonObject application, String catalogId, String collectionName) {
      changeCatalogToSubmittedCalls.incrementAndGet();
    }
  }
  // -------------------------------------------------------------------------
  // 1) getCurrentTimestamp()  (private)
  // -------------------------------------------------------------------------

  @Test
  void getCurrentTimestamp_shouldReturnFormattedTimestamp() {
    String ts = callPrivate(job, "getCurrentTimestamp", new Class<?>[] {});
    assertNotNull(ts);
    assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", ts));
  }

  // -------------------------------------------------------------------------
  // 2) findApplicationsWithLatestStatusSent(...) (private)
  //    - cover succeeded + failed branches and query creation
  // -------------------------------------------------------------------------

  @Test
  void findApplicationsWithLatestStatusSent_whenMongoFindSucceeds_shouldReturnList_andQueryContainsSent() {
    List<JsonObject> apps = List.of(new JsonObject().put("_id", "A1"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.succeededFuture(apps));
      return null;
    }).when(mongoClient).find(eq("myCollection"), any(JsonObject.class), any());

    Future<List<JsonObject>> fut = callPrivate(
      job,
      "findApplicationsWithLatestStatusSent",
      new Class<?>[]{String.class, int.class},
      "myCollection",
      10
    );
    assertTrue(fut.succeeded());
    assertEquals(apps, fut.result());

    ArgumentCaptor<JsonObject> queryCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).find(eq("myCollection"), queryCap.capture(), any());

    String q = queryCap.getValue().encode();
    assertTrue(q.contains("\"SENT\""));
    assertTrue(q.contains("\"latest\""));
  }

  @Test
  void findApplicationsWithLatestStatusSent_whenMongoFindFails_shouldFailFuture() {
    RuntimeException boom = new RuntimeException("boom");

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.failedFuture(boom));
      return null;
    }).when(mongoClient).find(eq("myCollection"), any(JsonObject.class), any());

    Future<List<JsonObject>> fut = callPrivate(
      job,
      "findApplicationsWithLatestStatusSent",
      new Class<?>[]{String.class, int.class},
      "myCollection",
      10
    );

    assertTrue(fut.failed());
    assertSame(boom, fut.cause());
  }

  // -------------------------------------------------------------------------
  // 3) processSentApplication(...) (private)
  //    Cover the "no SENT latest" path (filters run, but never reaches getDispatcherConfiguration)
  // -------------------------------------------------------------------------

  @Test
  void processSentApplication_whenNoCatalogHasLatestSent_shouldNotTryToQueryDispatcherConfig() {
    JsonObject catalog1 = new JsonObject()
      .put("catalog_id", "C1")
      .put("statuses", new JsonArray()
        .add(new JsonObject().put("latest", 1).put("status", "DRAFT"))
      );

    JsonObject catalog2 = new JsonObject()
      .put("catalog_id", "C2")
      .put("statuses", new JsonArray()
        .add(new JsonObject().put("latest", 0).put("status", "SENT"))
        .add(new JsonObject().put("latest", 1).put("status", "SUBMITTED"))
      );

    JsonObject application = new JsonObject()
      .put("_id", "A1")
      .put("catalogs", new JsonArray().add(catalog1).add(catalog2));

    callPrivate(
      job,
      "processSentApplication",
      new Class<?>[]{JsonObject.class, String.class, int.class},
      application, "myCollection", 10
    );

    verify(mongoClient, never()).findOne(anyString(), any(JsonObject.class), any(JsonObject.class), any());
  }

  // -------------------------------------------------------------------------
  // 4) getStatusWithSentAsLatest(...) (private)
  // -------------------------------------------------------------------------

  @Test
  void getStatusWithSentAsLatest_shouldReturnMatchingStatusOrNull() {
    JsonObject sentLatest = new JsonObject().put("latest", 1).put("status", "SENT");
    JsonObject catalog = new JsonObject().put("statuses", new JsonArray().add(sentLatest));

    JsonObject found = callPrivate(job, "getStatusWithSentAsLatest",
      new Class<?>[]{JsonObject.class}, catalog);


    assertNotNull(found);
    assertEquals(1, found.getInteger("latest"));
    assertEquals("SENT", found.getString("status"));

    JsonObject noSent = new JsonObject().put("statuses",
      new JsonArray().add(new JsonObject().put("latest", 1).put("status", "DRAFT")));

    assertNull(callPrivate(job, "getStatusWithSentAsLatest", new Class<?>[]{JsonObject.class}, noSent));
  }

  // -------------------------------------------------------------------------
  // 5) isCatalogReceived(...) (private)
  // -------------------------------------------------------------------------

  @Test
  void isCatalogReceived_shouldReturnTrueOnlyWhenIdsMatchAndStatusIsReceived() {
    JsonObject dispatcherResult = new JsonObject().put("configuration", new JsonObject()
      .put("dispatcher_response", new JsonArray()
        .add(new JsonObject()
          .put("application_id", "A1")
          .put("catalog_ids", new JsonArray().add("C1"))
          .put("status", "domibus_national_received")
        )
      )
    );

    boolean ok = callPrivate(job, "isCatalogReceived",
      new Class<?>[]{JsonObject.class, String.class, String.class},
      dispatcherResult, "A1", "C1");

    assertTrue(ok);

    boolean no = callPrivate(job, "isCatalogReceived",
      new Class<?>[]{JsonObject.class, String.class, String.class},
      dispatcherResult, "A1", "C999");

    assertFalse(no);
  }

  // -------------------------------------------------------------------------
  // 6) incrementRetryStatus(...) + removeDispatcherStatus(...) + findCatalogById(...)
  // -------------------------------------------------------------------------

  @Test
  void incrementRetryStatus_shouldIncrementAndStoreDispatcherResponse_untilLimit() {
    JsonObject latestSent = new JsonObject()
      .put("latest", 1)
      .put("status", "SENT")
      .put("dispatcher_status", new JsonObject().put("retries", 0));

    JsonObject catalog = new JsonObject().put("statuses", new JsonArray().add(latestSent));
    JsonObject dispatcherResult = new JsonObject().put("some", "response");

    callPrivate(job, "incrementRetryStatus",
      new Class<?>[]{JsonObject.class, JsonObject.class, int.class},
      catalog, dispatcherResult, 2);

    JsonObject ds = latestSent.getJsonObject("dispatcher_status");
    assertEquals(1, ds.getInteger("retries"));
    assertEquals(dispatcherResult, ds.getJsonObject("dispatcher_response"));

    ds.put("retries", 2);
    callPrivate(job, "incrementRetryStatus",
      new Class<?>[]{JsonObject.class, JsonObject.class, int.class},
      catalog, new JsonObject().put("x", 1), 2);

    assertEquals(2, latestSent.getJsonObject("dispatcher_status").getInteger("retries"));
  }

  @Test
  void removeDispatcherStatus_shouldRemoveFromLatestSentStatus() {
    JsonObject latestSent = new JsonObject()
      .put("latest", 1)
      .put("status", "SENT")
      .put("dispatcher_status", new JsonObject().put("retries", 1));

    JsonObject catalog = new JsonObject().put("statuses", new JsonArray().add(latestSent));

    callPrivate(job, "removeDispatcherStatus", new Class<?>[]{JsonObject.class}, catalog);

    assertFalse(latestSent.containsKey("dispatcher_status"));
  }

  @Test
  void findCatalogById_shouldReturnCatalogOrNull() {
    JsonObject c1 = new JsonObject().put("catalog_id", "C1");
    JsonObject c2 = new JsonObject().put("catalog_id", "C2");
    JsonObject app = new JsonObject().put("catalogs", new JsonArray().add(c1).add(c2));

    JsonObject found = callPrivate(job, "findCatalogById",
      new Class<?>[]{JsonObject.class, String.class}, app, "C2");
    assertEquals(c2, found);

    JsonObject missing = callPrivate(job, "findCatalogById",
      new Class<?>[]{JsonObject.class, String.class}, app, "CX");
    assertNull(missing);
  }
  private static void awaitAssert(Runnable assertion) {
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(assertion::run);
  }


  @Test
  void retrieveSentApplications_whenFindSucceedsWithEmptyList_shouldNotCallProcessSentApplication() {
    DispatcherScheduledJob spy = Mockito.spy(job);

    // Make private findApplicationsWithLatestStatusSent(...) return empty list
    // We do it by stubbing mongoClient.find(...) used inside that method.
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.succeededFuture(List.of()));
      return null;
    }).when(mongoClient).find(eq("myCollection"), any(JsonObject.class), any());

    spy.retrieveSentApplications("myCollection", 3);

    // Async handler runs; ensure mongo find called, but nothing further happens (no dispatcher lookup)
    awaitAssert(() -> verify(mongoClient).find(eq("myCollection"), any(JsonObject.class), any()));
    verify(mongoClient, never()).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());
  }

  @Test
  void retrieveSentApplications_whenFindFails_shouldLogErrorBranchAndNotCallDispatcher() {
    DispatcherScheduledJob spy = Mockito.spy(job);

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.failedFuture(new RuntimeException("boom")));
      return null;
    }).when(mongoClient).find(eq("myCollection"), any(JsonObject.class), any());

    spy.retrieveSentApplications("myCollection", 3);

    awaitAssert(() -> verify(mongoClient).find(eq("myCollection"), any(JsonObject.class), any()));
    verify(mongoClient, never()).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());
  }


  @Test
  void retrieveSentApplications_whenNoApplicationsFound_shouldNotProcessAnything() {
    TestableDispatcherScheduledJob testJob =
      new TestableDispatcherScheduledJob(dbHandler, mongoClient, dispatcherManager, auditLogManager);

    // findApplicationsWithLatestStatusSent(...) uses mongoClient.find(...)
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.succeededFuture(List.of()));
      return null;
    }).when(mongoClient).find(eq("myCollection"), any(JsonObject.class), any());

    testJob.retrieveSentApplications("myCollection", 2);

    // Should NOT even try dispatcher configuration lookup
    verify(mongoClient, never()).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());

    // No save attempted
    assertNull(testJob.replacedApp.get());
  }

  @Test
  void retrieveSentApplications_whenFindFails_shouldNotQueryDispatcher() {
    TestableDispatcherScheduledJob testJob =
      new TestableDispatcherScheduledJob(dbHandler, mongoClient, dispatcherManager, auditLogManager);

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.failedFuture(new RuntimeException("boom")));
      return null;
    }).when(mongoClient).find(eq("myCollection"), any(JsonObject.class), any());

    testJob.retrieveSentApplications("myCollection", 2);

    verify(mongoClient, never()).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());
    assertNull(testJob.replacedApp.get());
  }


  @Test
  void processCatalog_whenGetDispatcherConfigurationFails_shouldHitFailureBranch() {
    // Directly invoke private processCatalog(...) via reflection to cover onFailure branch.
    JsonObject sentLatest = new JsonObject().put("latest", 1).put("status", "SENT");
    JsonObject catalog = new JsonObject()
      .put("catalog_id", "C1")
      .put("statuses", new JsonArray().add(sentLatest));

    JsonObject app = new JsonObject().put("_id", "A1");

    // mongoClient.findOne used by getDispatcherConfiguration -> fail it
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.failedFuture(new RuntimeException("nope")));
      return null;
    }).when(mongoClient).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());

    callPrivate(job, "processCatalog",
      new Class<?>[]{JsonObject.class, JsonObject.class, String.class, int.class},
      app, catalog, "myCollection", 3);

    awaitAssert(() -> verify(mongoClient).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any()));
  }

  @Test
  void getDispatcherConfiguration_whenFindOneSucceedsWithNullResult_shouldReturnReasonObject() {
    JsonObject app = new JsonObject().put("_id", "A1");

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());

    Future<JsonObject> fut = callPrivate(job, "getDispatcherConfiguration",
      new Class<?>[]{JsonObject.class, String.class}, app, "myCollection");

    assertTrue(fut.succeeded());
    assertEquals("No application id found in dispatcher", fut.result().getString("reason"));
  }

  @Test
  void getDispatcherConfiguration_whenFindOneFailsWithCauseNull_shouldFailWithDefaultRuntimeException() {
    JsonObject app = new JsonObject().put("_id", "A1");

    // Force asyncResult.failed(), asyncResult.cause() == null
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(new AsyncResult<>() {
        @Override public JsonObject result() { return null; }
        @Override public Throwable cause() { return null; }
        @Override public boolean succeeded() { return false; }
        @Override public boolean failed() { return true; }
      });
      return null;
    }).when(mongoClient).findOne(eq(DISPATCHER_COLLECTION), any(), any(), any());

    Future<JsonObject> fut = callPrivate(job, "getDispatcherConfiguration",
      new Class<?>[]{JsonObject.class, String.class}, app, "myCollection");

    assertTrue(fut.failed());
    assertNotNull(fut.cause());
    assertEquals("Dispatcher configuration not found", fut.cause().getMessage());
  }

  @Test
  void handleDispatcherResponse_whenCatalogReceived_shouldChangeCatalogToSubmitted() {
    TestableDispatcherScheduledJob testJob =
      new TestableDispatcherScheduledJob(dbHandler, mongoClient, dispatcherManager, auditLogManager);

    JsonObject catalog = new JsonObject().put("catalog_id", "C1");

    JsonObject application = new JsonObject().put("_id", "A1");

    JsonObject dispatcherResult = new JsonObject()
      .put("configuration", new JsonObject()
        .put("dispatcher_response", new JsonArray()
          .add(new JsonObject()
            .put("application_id", "A1")
            .put("catalog_ids", new JsonArray().add("C1"))
            .put("status", "domibus_national_received")
          )
        )
      );

    callPrivate(
      testJob,
      "handleDispatcherResponse",
      new Class<?>[]{JsonObject.class, JsonObject.class, JsonObject.class, String.class, int.class},
      application, catalog, dispatcherResult, "myCollection", 2
    );

    assertEquals(1, testJob.changeCatalogToSubmittedCalls.get());
  }



}
