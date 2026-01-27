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
import static eu.europa.data.health.access.Constants.COMPLETED;
import static eu.europa.data.health.access.Constants.DELETION_CAN_NOT_BE_EXECUTED;
import static eu.europa.data.health.access.Constants.FILE_NAME;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.PROVIDED_APPLICATION_ID;
import static eu.europa.data.health.access.Constants.RESULT_MESSAGE_PARAM;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.TRANSLATION;
import static eu.europa.data.health.access.Constants.WAS_NOT_FOUND_IN_THE_DATABASE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.CatalogStatus;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.PermitStatus;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.utils.ApplicationFormUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;


class CommonApplicationManagerStatusTest {

  private DBHandler dbHandler;
  private MongoClient mongoClient;
  private AuditLogManager auditLogManager;

  private RoutingContext ctx;
  private HttpServerResponse response;

  private TestManager manager;

  /** minimal concrete class so we can instantiate CommonApplicationManager */
  static class TestManager extends CommonApplicationManager {
    protected TestManager(DBHandler dbHandler, MongoClient mongoClient, AuditLogManager auditLogManager) {
      super(dbHandler, mongoClient, auditLogManager);
    }
    List<String> lastTargets;
    String lastSourceLang;
    String lastDocId;
    String lastAppId;
    String lastAppType;

    @Override
    protected Future<Set<String>> getTargetLanguagesFromDatasets(List<JsonObject> datasets) {
      return Future.succeededFuture(new HashSet<>(Set.of("FR", "DE")));
    }

    @Override
    protected void triggerDocumentTranslationRequest(
      String applicationId,
      String applicationType,
      List<String> targetLanguages,
      String sourceLang,
      String documentId
    ) {
      // capture arguments (so we can test handleDocTranslation easily)
      this.lastAppId = applicationId;
      this.lastAppType = applicationType;
      this.lastTargets = targetLanguages;
      this.lastSourceLang = sourceLang;
      this.lastDocId = documentId;
    }
  }

  @BeforeEach
  void setUp() {
    dbHandler = mock(DBHandler.class);
    mongoClient = mock(MongoClient.class);
    auditLogManager = mock(AuditLogManager.class);

    manager = new TestManager(dbHandler, mongoClient, auditLogManager);

    ctx = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);

    // chaining
    when(response.setStatusCode(anyInt())).thenReturn(response);

    // IMPORTANT: avoid end() overload ambiguity by stubbing the String overload
    when(response.end(anyString())).thenReturn(null);
  }

  // -------------------------------------------------------------------------
  // getLatestCatalogStatus(...)
  // -------------------------------------------------------------------------

  @Test
  void getLatestCatalogStatus_whenCatalogsNull_shouldReturnNull() {
    JsonObject app = new JsonObject(); // no "catalogs"
    assertNull(manager.getLatestCatalogStatus(app, "C1"));
  }

  @Test
  void getLatestCatalogStatus_whenCatalogIdNotFound_shouldReturnNull() {
    JsonObject app = new JsonObject().put("catalogs", new JsonArray()
      .add(new JsonObject().put("catalog_id", "OTHER")
        .put("statuses", new JsonArray().add(new JsonObject().put("latest", 1).put("status", "SENT"))))
    );

    assertNull(manager.getLatestCatalogStatus(app, "C1"));
  }

  @Test
  void getLatestCatalogStatus_whenStatusesNull_shouldReturnNull() {
    JsonObject app = new JsonObject().put("catalogs", new JsonArray()
      .add(new JsonObject().put("catalog_id", "C1")) // no statuses
    );

    assertNull(manager.getLatestCatalogStatus(app, "C1"));
  }

  @Test
  void getLatestCatalogStatus_whenLatestFound_shouldReturnStatusString() {
    JsonObject app = new JsonObject().put("catalogs", new JsonArray()
      .add(new JsonObject()
        .put("catalog_id", "C1")
        .put("statuses", new JsonArray()
          .add(new JsonObject().put("latest", 0).put("status", "DRAFT"))
          .add(new JsonObject().put("latest", 1).put("status", "SENT"))
        ))
    );

    assertEquals("SENT", manager.getLatestCatalogStatus(app, "C1"));
  }

  @Test
  void getLatestCatalogStatus_whenCatalogFoundButNoLatest_shouldReturnNull() {
    JsonObject app = new JsonObject().put("catalogs", new JsonArray()
      .add(new JsonObject()
        .put("catalog_id", "C1")
        .put("statuses", new JsonArray()
          .add(new JsonObject().put("latest", 0).put("status", "SENT"))
          .add(new JsonObject().put("latest", 0).put("status", "SUBMITTED"))
        ))
    );

    assertNull(manager.getLatestCatalogStatus(app, "C1"));
  }

  // -------------------------------------------------------------------------
  // private getLatestStatus(...) via reflection
  // -------------------------------------------------------------------------

  private String callGetLatestStatus(String fieldName, String fieldStatusName,
                                     JsonObject mongoApplication, String catalogId,
                                     String datasetId, RoutingContext context) {
    try {
      Method m = CommonApplicationManager.class.getDeclaredMethod(
        "getLatestStatus",
        String.class, String.class, JsonObject.class, String.class, String.class, RoutingContext.class
      );
      m.setAccessible(true);
      return (String) m.invoke(manager, fieldName, fieldStatusName, mongoApplication, catalogId, datasetId, context);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      // unwrap reflection InvocationTargetException etc
      Throwable t = e.getCause() != null ? e.getCause() : e;
      if (t instanceof RuntimeException rte) throw rte;
      throw new RuntimeException(t);
    }
  }

  @Test
  void getLatestStatus_whenDatasetsNullOrEmpty_shouldSet400AndThrow() {
    JsonObject appNoDatasets = new JsonObject(); // datasets missing

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
      callGetLatestStatus("decisions", "datasetStatus", appNoDatasets, "C1", "D1", ctx)
    );

    assertTrue(ex.getMessage().contains("datasets are null/empty"));

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end((String) argThat(s -> s.toString().contains("datasets are null/empty")));
  }

  @Test
  void getLatestStatus_whenDatasetMatchedButFieldArrayNull_shouldReturnNull() {
    JsonObject dataset = new JsonObject()
      .put("catalog_id", "C1")
      .put("dataset_id", "D1");
    // no "decisions" field -> should return null

    JsonObject app = new JsonObject().put("datasets", new JsonArray().add(dataset));

    String latest = callGetLatestStatus("decisions", "datasetStatus", app, "C1", "D1", ctx);
    assertNull(latest);

    // No error response should be written in this branch
    verify(response, never()).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }

  @Test
  void getLatestStatus_whenDatasetMatchedButNoLatestDecision_shouldSet400AndThrow() {
    JsonObject dataset = new JsonObject()
      .put("catalog_id", "C1")
      .put("dataset_id", "D1")
      .put("decisions", new JsonArray()
        .add(new JsonObject().put("latest", 0).put("datasetStatus", "SENT"))
        .add(new JsonObject().put("latest", 0).put("datasetStatus", "OTHER"))
      );

    JsonObject app = new JsonObject().put("datasets", new JsonArray().add(dataset));

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
      callGetLatestStatus("decisions", "datasetStatus", app, "C1", "D1", ctx)
    );

    assertTrue(ex.getMessage().contains("No latest decision found"));

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end((String) argThat(s -> s.toString().contains("No latest decision found")));
  }

  @Test
  void getLatestStatus_whenNoDatasetMatchesBothIds_shouldSet400AndThrow() {
    JsonObject dataset = new JsonObject()
      .put("catalog_id", "C1")
      .put("dataset_id", "OTHER")
      .put("decisions", new JsonArray().add(new JsonObject().put("latest", 1).put("datasetStatus", "X")));

    JsonObject app = new JsonObject().put("datasets", new JsonArray().add(dataset));

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
      callGetLatestStatus("decisions", "datasetStatus", app, "C1", "D1", ctx)
    );

    assertTrue(ex.getMessage().contains("No dataset found matching both catalog and dataset id"));

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end((String) argThat(s -> s.toString().contains("No dataset found matching both catalog and dataset id")));
  }

  @SuppressWarnings("unchecked")
  private static <T> T callPrivate(Object target, String method, Class<?>[] types, Object... args) {
    try {
      Class<?> c = target.getClass();
      Method m = null;

      while (c != null) {
        try {
          m = c.getDeclaredMethod(method, types);
          break;
        } catch (NoSuchMethodException ignore) {
          c = c.getSuperclass();
        }
      }

      if (m == null) {
        throw new NoSuchMethodException(
          "Method " + method + " not found in class hierarchy of " + target.getClass().getName()
        );
      }

      m.setAccessible(true);
      return (T) m.invoke(target, args);

    } catch (java.lang.reflect.InvocationTargetException ite) {
      // IMPORTANT: show the real exception thrown *inside* the private method
      Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
      throw new RuntimeException("Private method threw: " + method + " -> " + cause, cause);

    } catch (Exception e) {
      throw new RuntimeException("Failed calling private method: " + method, e);
    }
  }


  // -------------------------------------------------------------------------
// Catalog computation / updateCatalogStatus / createCatalogs / additionalInformation
// -------------------------------------------------------------------------

  @Test
  void createCatalogs_whenDatasetsProvided_shouldCreateUniqueCatalogsWithInitialStatus() {
    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put("catalog_id", "C1"))
      .add(new JsonObject().put("catalog_id", "C2"))
      .add(new JsonObject().put("catalog_id", "C1")); // duplicate -> should be unique

    JsonArray catalogs = callPrivate(
      manager,
      "createCatalogs",
      new Class<?>[]{JsonArray.class},
      datasets
    );

    assertNotNull(catalogs);
    assertEquals(2, catalogs.size());

    // verify each catalog has statuses[0] with latest=1 and status=DRAFT
    for (int i = 0; i < catalogs.size(); i++) {
      JsonObject cat = catalogs.getJsonObject(i);
      assertTrue(Set.of("C1", "C2").contains(cat.getString("catalog_id")));

      JsonArray statuses = cat.getJsonArray("statuses");
      assertNotNull(statuses);
      assertEquals(1, statuses.size());

      JsonObject st = statuses.getJsonObject(0);
      assertEquals(1, st.getInteger("latest"));
      assertEquals(CatalogStatus.DRAFT.getLabel(), st.getString("status"));
      assertNotNull(st.getValue("date_received")); // Instant.now()
    }
  }

  @Test
  void updateCatalogStatus_whenNoCatalogId_shouldUpdateOrCreateAllCatalogsFromDatasets_case2() {
    // application datasets define which catalogs exist
    JsonObject application = new JsonObject()
      .put("_id", "A1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
        .add(new JsonObject().put("catalog_id", "C2"))
      )
      .put("catalogs", new JsonArray()
        // existing catalog C1 with 1 latest status
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
          )
        )
      );

    // calls computeCatalogs(null, null, SENT, null, application)
    manager.updateCatalogStatus(application);

    JsonArray catalogs = application.getJsonArray("catalogs");
    assertNotNull(catalogs);
    assertEquals(2, catalogs.size()); // C1 existing updated, C2 created

    // find C1 and ensure old latest reset + new latest added
    JsonObject c1 = catalogs.stream()
      .map(JsonObject.class::cast)
      .filter(c -> "C1".equals(c.getString("catalog_id")))
      .findFirst()
      .orElseThrow();

    JsonArray c1Statuses = c1.getJsonArray("statuses");
    assertTrue(c1Statuses.size() >= 2);

    long latestCount = c1Statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .count();

    assertEquals(1, latestCount);

    JsonObject latest = c1Statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(CatalogStatus.SENT.getLabel(), latest.getString("status"));
    assertNotNull(latest.getValue("date_received"));

    // C2 should exist with 1 latest status
    JsonObject c2 = catalogs.stream()
      .map(JsonObject.class::cast)
      .filter(c -> "C2".equals(c.getString("catalog_id")))
      .findFirst()
      .orElseThrow();

    JsonArray c2Statuses = c2.getJsonArray("statuses");
    assertEquals(1, c2Statuses.size());
    assertEquals(1, c2Statuses.getJsonObject(0).getInteger("latest"));
    assertEquals(CatalogStatus.SENT.getLabel(), c2Statuses.getJsonObject(0).getString("status"));
  }

  @Test
  void updateCatalogStatus_whenNoDatasets_shouldReturnEmptyCatalogs_case2() {
    JsonObject application = new JsonObject()
      .put("_id", "A1")
      .put("datasets", new JsonArray()); // empty -> triggers "has no datasets" branch

    manager.updateCatalogStatus(application);

    JsonArray catalogs = application.getJsonArray("catalogs");
    assertNotNull(catalogs);
    assertEquals(0, catalogs.size());
  }

  @Test
  void updateCatalogStatus_whenCatalogIdProvided_shouldUpdateOnlyThatCatalog_case3() {
    JsonObject application = new JsonObject()
      .put("_id", "A1")
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
          )
        )
        .add(new JsonObject()
          .put("catalog_id", "C2")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
          )
        )
      );

    manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED);

    JsonArray catalogs = application.getJsonArray("catalogs");
    assertEquals(2, catalogs.size());

    JsonObject c1 = catalogs.stream().map(JsonObject.class::cast)
      .filter(c -> "C1".equals(c.getString("catalog_id")))
      .findFirst().orElseThrow();

    JsonArray c1Statuses = c1.getJsonArray("statuses");
    assertTrue(c1Statuses.size() >= 2);

    JsonObject c1Latest = c1Statuses.stream().map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst().orElseThrow();

    assertEquals(CatalogStatus.SUBMITTED.getLabel(), c1Latest.getString("status"));

    // C2 must remain untouched (still has its original single latest status DRAFT)
    JsonObject c2 = catalogs.stream().map(JsonObject.class::cast)
      .filter(c -> "C2".equals(c.getString("catalog_id")))
      .findFirst().orElseThrow();

    JsonArray c2Statuses = c2.getJsonArray("statuses");
    assertEquals(1, c2Statuses.size());
    assertEquals(1, c2Statuses.getJsonObject(0).getInteger("latest"));
    assertEquals("DRAFT", c2Statuses.getJsonObject(0).getString("status"));
  }

  @Test
  void updateCatalogStatus_whenAdditionalInformationStatusAndPayload_shouldComputeAdditionalInformation_case3() {
    JsonObject country = new JsonObject().put("code", "GR");

    JsonObject application = new JsonObject()
      .put("_id", "A1")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("dataset_id", "D1")
          .put("country", country)
        )
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "SENT").put("latest", 1))
          )
        )
      );

    JsonObject payload = new JsonObject()
      .put("formVersion", "v1")
      .put("additionalInformation", new JsonObject()
        .put("source", new JsonArray()
          .add(new JsonObject().put("field", "section.0.question").put("comment", "c1"))
          .add(new JsonObject().put("field", "simple").put("comment", "c2"))
        )
        .put("translation", new JsonArray()
          .add(new JsonObject().put("field", "t.0.x").put("comment", "tc1"))
        )
      );

    manager.updateCatalogStatus(application, "C1", CatalogStatus.ADDITIONAL_INFORMATION, payload);

    // Status entry should include statusPayload and latest=1
    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latest = statuses.stream().map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst().orElseThrow();

    assertEquals(CatalogStatus.ADDITIONAL_INFORMATION.getLabel(), latest.getString("status"));
    assertNotNull(latest.getJsonObject("statusPayload"));
    assertEquals("v1", latest.getJsonObject("statusPayload").getString("formVersion"));

    // additionalInformation should be computed and attached to application
    JsonArray additionalInfo = application.getJsonArray("additionalInformation");
    assertNotNull(additionalInfo);
    assertEquals(1, additionalInfo.size());

    JsonObject ai = additionalInfo.getJsonObject(0);
    assertEquals("C1", ai.getString("catalog_id"));
    assertEquals(country, ai.getJsonObject("country"));
    assertEquals("v1", ai.getString("formVersion"));

    // expanded "source"
    JsonObject sourceExpanded = ai.getJsonObject("source");
    assertNotNull(sourceExpanded);

    // section.0.question -> section is array with index 0 object with question=c1
    JsonArray sectionArr = sourceExpanded.getJsonArray("section");
    assertNotNull(sectionArr);
    assertEquals("c1", sectionArr.getJsonObject(0).getString("question"));

    // simple -> "c2"
    assertEquals("c2", sourceExpanded.getString("simple"));

    // expanded "translation"
    JsonObject translationExpanded = ai.getJsonObject("translation");
    JsonArray tArr = translationExpanded.getJsonArray("t");
    assertEquals("tc1", tArr.getJsonObject(0).getString("x"));
  }

  @Test
  void expandAdditionalInformation_shouldBuildNestedObjectsAndArrays() {
    JsonArray flat = new JsonArray()
      .add(new JsonObject().put("field", "a.b").put("comment", "x"))
      .add(new JsonObject().put("field", "items.0.name").put("comment", "n0"))
      .add(new JsonObject().put("field", "items.1.name").put("comment", "n1"))
      .add(new JsonObject().put("field", "skip").put("comment", "")); // should be ignored

    JsonObject expanded = callPrivate(
      manager,
      "expandAdditionalInformation",
      new Class<?>[]{JsonArray.class},
      flat
    );

    assertEquals("x", expanded.getJsonObject("a").getString("b"));

    JsonArray items = expanded.getJsonArray("items");
    assertEquals("n0", items.getJsonObject(0).getString("name"));
    assertEquals("n1", items.getJsonObject(1).getString("name"));

    // "skip" should not exist because comment empty
    assertNull(expanded.getValue("skip"));
  }

  @Test
  void handleDocTranslation_whenLanguagesSucceeded_shouldCallTriggerWithUniqueTargetsPlusEN() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = spy(new TestManager(db, mongo, audit));

    JsonObject app = new JsonObject()
      .put("_id", "APP1")
      .put("inputLanguage", "fr") // should become "FR"
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1").put("country", new JsonObject().put("language", "FR")))
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject().put("id", "DOC123"));

    // call the private method
    callPrivate(
      testManager,
      "handleDocTranslation",
      new Class<?>[]{JsonObject.class, JsonObject.class, String.class},
      app,
      successDocUpload,
      Constants.DATA_ACCESS_APPLICATION_COLLECTION
    );

    // verify captured
    assertEquals("APP1", testManager.lastAppId);
    assertEquals(Constants.DATA_ACCESS_APPLICATION_COLLECTION, testManager.lastAppType);
    assertEquals("DOC123", testManager.lastDocId);
    assertEquals("FR", testManager.lastSourceLang);

    // should include EN + unique from getTargetLanguagesFromDatasets (FR, EL, FR -> FR, EL) + EN
    assertNotNull(testManager.lastTargets);
    // order not guaranteed (HashSet); assert content
    assertTrue(testManager.lastTargets.contains("EN"));
    assertTrue(testManager.lastTargets.contains("FR"));
  }

  @Test
  void handleDocTranslation_whenLanguagesFailed_shouldCallTriggerWithEmptyTargets() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = spy(new TestManager(db, mongo, audit));
    doReturn(Future.failedFuture("boom"))
      .when(testManager).getTargetLanguagesFromDatasets(anyList());

    JsonObject app = new JsonObject()
      .put("_id", "APP1")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray().add(new JsonObject()));

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject().put("id", "DOC123"));

    callPrivate(
      testManager,
      "handleDocTranslation",
      new Class<?>[]{JsonObject.class, JsonObject.class, String.class},
      app,
      successDocUpload,
      "col"
    );

    assertEquals("APP1", testManager.lastAppId);
    assertEquals("col", testManager.lastAppType);
    assertEquals("DOC123", testManager.lastDocId);
    assertEquals("EN", testManager.lastSourceLang);

    assertNotNull(testManager.lastTargets);
    assertEquals(0, testManager.lastTargets.size()); // fallback = List.of()
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Class<?> c = target.getClass();
      while (c != null) {
        try {
          var f = c.getDeclaredField(fieldName);
          f.setAccessible(true);
          f.set(target, value);
          return;
        } catch (NoSuchFieldException ignored) {
          c = c.getSuperclass();
        }
      }
      throw new NoSuchFieldException(fieldName);
    } catch (Exception e) {
      throw new RuntimeException("Failed setting field: " + fieldName, e);
    }
  }


  @Test
  void triggerDocumentTranslationRequest_whenHubStoreFails_shouldNotUpdateMongo() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    HubStoreClient hubStoreClient = mock(HubStoreClient.class);
    HubTranslationClient hubTranslationClient = mock(HubTranslationClient.class);

    setField(testManager, "hubStoreClient", hubStoreClient);
    setField(testManager, "hubTranslationClient", hubTranslationClient);
    setField(testManager, "mongoClient", mongo);

    when(hubStoreClient.getFileAsBase64(anyString(), anyString(), anyString()))
      .thenReturn(Future.failedFuture("nope"));

    testManager.triggerDocumentTranslationRequest("APP1", "col", List.of("EN"), "EN", "DOC123");

    verify(mongo, never()).updateCollection(anyString(), any(), (JsonArray) any());
    verify(hubTranslationClient, never()).requestFileTranslation(any());
  }

  @Test
  void buildFileRemovalUpdate_whenNoTranslation_shouldReturnEmptyUpdate() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    JsonObject app = new JsonObject(); // no "translation"
    JsonObject update = callPrivate(testManager, "buildFileRemovalUpdate",
      new Class<?>[]{JsonObject.class, String.class},
      app, "FILE1");

    assertTrue(update.isEmpty());
  }

  @Test
  void buildFileRemovalUpdate_whenFilesMissingOrNoFileId_shouldReturnEmptyUpdate() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    JsonObject app = new JsonObject()
      .put("translation", new JsonObject()
        .put("files", new JsonObject().put("OTHER", new JsonObject()))
      );

    JsonObject update = callPrivate(testManager, "buildFileRemovalUpdate",
      new Class<?>[]{JsonObject.class, String.class},
      app, "FILE1");

    assertTrue(update.isEmpty());
  }

  @Test
  void buildFileRemovalUpdate_whenRemovingLastFile_shouldUnsetEntireTranslation() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    JsonObject app = new JsonObject()
      .put("translation", new JsonObject()
        .put("files", new JsonObject()
          .put("FILE1", new JsonObject().put("status", "PENDING"))
        )
      );

    JsonObject update = callPrivate(testManager, "buildFileRemovalUpdate",
      new Class<?>[]{JsonObject.class, String.class},
      app, "FILE1");

    JsonObject unset = update.getJsonObject("$unset");
    assertNotNull(unset);
    assertTrue(unset.getBoolean("translation", false));        // remove entire translation
    assertTrue(unset.getBoolean("form_translations", false));  // always unsets form_translations
  }

  @Test
  void buildFileRemovalUpdate_whenRemovingOneOfManyFiles_shouldUnsetSpecificFileAndSetStatus() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    JsonObject app = new JsonObject()
      .put("translation", new JsonObject()
        .put("files", new JsonObject()
          .put("FILE1", new JsonObject().put("status", "PENDING"))
          .put("FILE2", new JsonObject().put("status", "PENDING"))
        )
      );

    JsonObject update = callPrivate(testManager, "buildFileRemovalUpdate",
      new Class<?>[]{JsonObject.class, String.class},
      app, "FILE1");

    JsonObject unset = update.getJsonObject("$unset");
    JsonObject set = update.getJsonObject("$set");

    assertNotNull(unset);
    assertTrue(unset.containsKey("translation.files.FILE1"));
    assertTrue(unset.containsKey("translation.form"));
    assertTrue(unset.containsKey("form_translations"));

    assertNotNull(set);
    assertNotNull(set.getString("translation.status"));      // value depends on ApplicationFormUtils; just ensure present
    assertNotNull(set.getValue("date_modified"));            // Instant.now()
  }

  @Test
  void updateNaturalPerson_shouldCopyFieldsIntoSection4() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    JsonObject section3 = new JsonObject()
      .put(Constants.NATURAL_PERSON_NAME, "John Doe")
      .put(Constants.NATURAL_PERSON_ADDRESS, "Addr")
      .put(Constants.NATURAL_PERSON_EMAIL, "a@b.com")
      .put(Constants.NATURAL_PERSON_PHONE, new JsonObject()
        .put("countryCode", "+30")
        .put("number", "123")
      );

    JsonObject section4 = new JsonObject();

    callPrivate(testManager, "updateNaturalPerson",
      new Class<?>[]{JsonObject.class, JsonObject.class},
      section4, section3);

    assertEquals("John Doe", section4.getString(Constants.FULL_NAME));
    assertEquals("Addr", section4.getString(Constants.ADDRESS));
    assertEquals("a@b.com", section4.getString(Constants.EMAIL));
    assertNotNull(section4.getValue(Constants.PHONE)); // whatever getPhoneNumber() returns
  }



  @Test
  void validateFileBeforeUpdate_dataRequest_section6_shouldNext() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    RequestBody body = mock(RequestBody.class);
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(resp);

// keep chaining methods
    when(resp.setStatusCode(anyInt())).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);

// IMPORTANT: end(...) returns Future<Void> in Vert.x 4, so stub with Future
    when(resp.end(ArgumentMatchers.<String>any()))
      .thenReturn(Future.succeededFuture());

// also stub no-arg end() if needed
    when(resp.end())
      .thenReturn(Future.succeededFuture());
    JsonObject payload = new JsonObject()
      .put("tabulationPlanArray", new JsonArray()
        .add(new JsonObject().put("tabulationPlan", new JsonObject().put("id", "file-1")))
      );

    when(body.asJsonObject()).thenReturn(payload);

    testManager.validateFileBeforeUpdate(routingContext, Constants.DATA_REQUEST_APPLICATION_COLLECTION);

    verify(routingContext, timeout(500)).next();

  }


  @Test
  void validateFileBeforeUpdate_defaultCollection_shouldJustNext() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    RoutingContext routingContext = mock(RoutingContext.class);
    when(routingContext.pathParam(Constants.SECTION_NUMBER)).thenReturn("99");

    testManager.validateFileBeforeUpdate(routingContext, "unknown_collection");

    verify(routingContext).next();
  }

  @Test
  void validateFileBeforeUpdate_dataAccess_section5_shouldCallNext() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    when(routingContext.pathParam(Constants.SECTION_NUMBER)).thenReturn("5");

    // Provide body + json so validateFileFor(...) doesn't NPE
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);

    // Put the keys the validator is likely to look for.
    // If it expects base64 string, keep them as strings. If it expects JsonObject, adjust.
    JsonObject payload = new JsonObject()
      .put("summaryOfPlanForUsingTheData", "dummyBase64OrId")
      .put("summaryOfResearchPlan", "dummyBase64OrId");

    when(body.asJsonObject()).thenReturn(payload);

    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(resp);

// keep chaining methods
    when(resp.setStatusCode(anyInt())).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);

// IMPORTANT: end(...) returns Future<Void> in Vert.x 4, so stub with Future
    when(resp.end(ArgumentMatchers.<String>any()))
      .thenReturn(Future.succeededFuture());

// also stub no-arg end() if needed
    when(resp.end())
      .thenReturn(Future.succeededFuture());

    testManager.validateFileBeforeUpdate(routingContext, Constants.DATA_ACCESS_APPLICATION_COLLECTION);

    verify(routingContext, timeout(500)).next();
  }

  private static void setPrivateField(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getSuperclass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void triggerDocumentTranslationRequest_whenFetchBase64Fails_shouldNotPersistMetadataOrRequestTranslation() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = spy(new TestManager(db, mongo, audit));

    HubStoreClient hubStore = mock(HubStoreClient.class);
    HubTranslationClient hubTranslation = mock(HubTranslationClient.class);

    setPrivateField(testManager, "hubStoreClient", hubStore);
    setPrivateField(testManager, "hubTranslationClient", hubTranslation);
    setPrivateField(testManager, "mongoClient", mongo); // only if mongoClient is private in the base class

    when(hubStore.getFileAsBase64(anyString(), anyString(), anyString()))
      .thenReturn(Future.failedFuture("boom"));

    testManager.triggerDocumentTranslationRequest("app-1", "col", List.of("FR"), "EN", "doc-1");


    verifyNoInteractions(hubTranslation);
    verify(mongo, never()).updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class));
  }


  @Test
  void decodeFilename_whenFilenameStarUtf8_shouldDecode() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    String header = "attachment; filename*=UTF-8''some%20file%20name.pdf";
    String decoded = testManager.decodeFilename(header);

    assertEquals("some file name.pdf", decoded);
  }

  @Test
  void decodeFilename_whenMissingOrNoStar_shouldReturnError() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    assertEquals("Error_decoding_filename", testManager.decodeFilename(null));
    assertEquals("Error_decoding_filename", testManager.decodeFilename("attachment; filename=\"x.pdf\""));
  }

  // ------------------------------------------------------------
// sendApplicationToOutgoingApplicationActivities + payload switch
// ------------------------------------------------------------

  @Test
  void sendApplicationToOutgoingApplicationActivities_statusUpdate_shouldSaveActivityWithSyncPending() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved-id"));
      return null;
    }).when(mongo).save(eq(Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    testManager.sendApplicationToOutgoingApplicationActivities(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "app-1",
      CatalogStatus.SENT,
      "cat-1",
      "reason-x"
    );

    ArgumentCaptor<JsonObject> docCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongo).save(eq(Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), docCap.capture(), any());

    JsonObject activity = docCap.getValue();
    assertEquals("app-1", activity.getString("objectId"));
    assertEquals("application", activity.getString("type"));
    assertEquals(OperationStatus.STATUS_UPDATE.getLabel(), activity.getString("operation"));

    JsonObject payload = activity.getJsonObject("payload");
    assertEquals(Constants.DATA_ACCESS_APPLICATION_COLLECTION, payload.getString("application_type"));
    assertEquals("cat-1", payload.getString("catalog_id"));
    assertEquals(CatalogStatus.SENT.getLabel(), payload.getString("status"));

    JsonObject config = activity.getJsonObject("configuration");
    assertEquals("1", config.getString("syncPending"));
  }

  @Test
  void sendApplicationToOutgoingApplicationActivities_decision_shouldSaveActivityWithDecisionsArray() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved-id"));
      return null;
    }).when(mongo).save(eq(Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    testManager.sendApplicationToOutgoingApplicationActivities(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "app-1",
      CatalogStatus.SUBMITTED,
      "cat-1",
      "APPROVED",
      "ds-1",
      null,
      OperationStatus.DECISION
    );

    ArgumentCaptor<JsonObject> docCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongo).save(eq(Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), docCap.capture(), any());

    JsonObject payload = docCap.getValue().getJsonObject("payload");
    JsonArray decisions = payload.getJsonArray("decisions");
    assertNotNull(decisions);
    assertEquals(1, decisions.size());

    JsonObject d0 = decisions.getJsonObject(0);
    assertEquals("APPROVED", d0.getString("decision"));
    assertEquals("ds-1", d0.getString("dataset_id"));
  }

  @Test
  void sendApplicationToOutgoingApplicationActivities_permit_shouldSaveActivityWithPermitsArray() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved-id"));
      return null;
    }).when(mongo).save(eq(Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    testManager.sendApplicationToOutgoingApplicationActivities(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "app-1",
      CatalogStatus.SUBMITTED,
      "cat-1",
      "GRANTED",
      "ds-9",
      "because",
      OperationStatus.PERMIT
    );

    ArgumentCaptor<JsonObject> docCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongo).save(eq(Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), docCap.capture(), any());

    JsonObject payload = docCap.getValue().getJsonObject("payload");
    JsonArray permits = payload.getJsonArray("permits");
    assertNotNull(permits);
    assertEquals(1, permits.size());

    JsonObject p0 = permits.getJsonObject(0);
    assertEquals("ds-9", p0.getString("dataset_id"));
    assertEquals("because", p0.getString("reason"));
    assertEquals("GRANTED", p0.getString("permit_status"));
  }

// ------------------------------------------------------------
// getApplicationsHbad / getApplicationsCountries / getApplicationTitles
// ------------------------------------------------------------

  @Test
  void getApplicationsHbad_whenMongoFindSucceeds_shouldReturnDistinctHdabs() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
    when(resp.setStatusCode(anyInt())).thenReturn(resp);

    // make Context.getEcasId(context) return something usable in your project:
    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    List<JsonObject> docs = List.of(
      new JsonObject().put("datasets", new JsonArray()
        .add(new JsonObject().put("hdab", new JsonObject().put("name", "HDAB A")))
        .add(new JsonObject().put("hdab", new JsonObject().put("name", "HDAB B")))
      ),
      new JsonObject().put("datasets", new JsonArray()
        .add(new JsonObject().put("hdab", new JsonObject().put("name", "HDAB A"))) // duplicate
      )
    );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<List<JsonObject>>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture(docs));
      return null;
    }).when(mongo).find(eq("db"), any(JsonObject.class), any());

    testManager.getApplicationsHbad(routingContext, "db");

    verify(resp).putHeader("Content-Type", "application/json");
    verify(resp).end((String) argThat(s -> s.toString().contains("HDAB A") && s.toString().contains("HDAB B")));
  }

  @Test
  void getApplicationsCountries_whenMongoFindSucceeds_shouldReturnDistinctCountryLabels() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
    when(resp.setStatusCode(anyInt())).thenReturn(resp);

    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    List<JsonObject> docs = List.of(
      new JsonObject().put("datasets", new JsonArray()
        .add(new JsonObject().put(Constants.COUNTRY, new JsonObject().put("label", "Greece")))
        .add(new JsonObject().put(Constants.COUNTRY, new JsonObject().put("label", "France")))
      ),
      new JsonObject().put("datasets", new JsonArray()
        .add(new JsonObject().put(Constants.COUNTRY, new JsonObject().put("label", "Greece")))
      )
    );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<List<JsonObject>>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture(docs));
      return null;
    }).when(mongo).find(eq("db"), any(JsonObject.class), any());

    testManager.getApplicationsCountries(routingContext, "db");

    verify(resp).putHeader("Content-Type", "application/json");
    verify(resp).end((String) argThat(s -> s.toString().contains("Greece") && s.toString().contains("France")));
  }

  @Test
  void getApplicationTitles_whenMongoFindSucceeds_shouldReturnTitles() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
    when(resp.setStatusCode(anyInt())).thenReturn(resp);

    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    List<JsonObject> docs = List.of(
      new JsonObject().put("title", "A"),
      new JsonObject().put("title", "B"),
      new JsonObject().put("title", (String) null) // should be skipped
    );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<List<JsonObject>>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture(docs));
      return null;
    }).when(mongo).find(eq("db"), any(JsonObject.class), any());

    testManager.getApplicationTitles(routingContext, "db");

    verify(resp).putHeader("Content-Type", "application/json");
    verify(resp).end((String) argThat(s -> s.toString().contains("A") && s.toString().contains("B") && !s.toString().contains("null")));
  }

  @Test
  void getApplicationsHbad_whenMongoFindFails_shouldReturn500WithErrorJson() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
    when(resp.setStatusCode(anyInt())).thenReturn(resp);

    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<List<JsonObject>>> handler = inv.getArgument(2);
      handler.handle(Future.failedFuture("db down"));
      return null;
    }).when(mongo).find(eq("db"), any(JsonObject.class), any());

    testManager.getApplicationsHbad(routingContext, "db");

    verify(resp).setStatusCode(500);
    verify(resp).end((String) argThat(s -> s.toString().contains("\"error\"") && s.toString().contains("db down")));
  }

  @Test
  void updateApplication_whenMongoSucceeds_shouldCompleteWithResult() {
    JsonObject updateDoc = new JsonObject().put("$set", new JsonObject().put("x", 1));
    JsonObject mongoResult = new JsonObject().put("_id", "app-1").put("x", 1);

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoResult));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq("col"), any(JsonObject.class), eq(updateDoc), any());

    Future<JsonObject> fut = manager.updateApplication("app-1", updateDoc, "col");

    assertTrue(fut.succeeded());
    assertEquals(mongoResult, fut.result());

    verify(mongoClient).findOneAndUpdate(
      eq("col"),
      argThat(q -> "app-1".equals(q.getString(ID))),
      eq(updateDoc),
      any()
    );
  }

  @Test
  void updateApplication_whenMongoFails_shouldFailFuture() {
    JsonObject updateDoc = new JsonObject().put("$set", new JsonObject().put("x", 1));
    RuntimeException boom = new RuntimeException("boom");

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.failedFuture(boom));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq("col"), any(JsonObject.class), eq(updateDoc), any());

    Future<JsonObject> fut = manager.updateApplication("app-1", updateDoc, "col");

    assertTrue(fut.failed());
    assertSame(boom, fut.cause());
  }

  // ----------------------------------------------------------------------------
  // deleteDistributions(...)
  // ----------------------------------------------------------------------------

  @Test
  void deleteDistributions_whenFindOneFails_shouldReturn500() {

    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    when(ctx.pathParam(Constants.APPLICATION_ID)).thenReturn("app-1");

    when(mongoClient.findOne(eq("col"), any(JsonObject.class), isNull(), any()))
      .thenAnswer(inv -> {
        @SuppressWarnings("unchecked")
        Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
        h.handle(Future.failedFuture("db down"));
        return null;
      });

    manager.deleteDistributions(ctx, "col");

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(argThat((String s) ->
      s.contains("db down") || s.contains(RESULT_MESSAGE_PARAM)
    ));
  }

  @Test
  void deleteDistributions_whenNotFound_shouldReturn404() {
    // avoid NPE in Context.getEcasId(ctx)
    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    when(ctx.pathParam(Constants.APPLICATION_ID)).thenReturn("app-1");

    when(mongoClient.findOne(eq("col"), any(JsonObject.class), isNull(), any()))
      .thenAnswer(inv -> {
        @SuppressWarnings("unchecked")
        Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
        h.handle(Future.succeededFuture(null)); // not found
        return null;
      });

    manager.deleteDistributions(ctx, "col");

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(argThat((String s) ->
      s.contains(PROVIDED_APPLICATION_ID)
        && s.contains("app-1")
        && s.contains(WAS_NOT_FOUND_IN_THE_DATABASE)
    ));
  }


  @Test
  void deleteDistributions_whenStatusNotDraftOrOngoing_shouldReturn400() {
    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    // make sure pathParam exists (your code uses APPLICATION_ID)
    when(ctx.pathParam(APPLICATION_ID)).thenReturn("app-1");

    // (only needed if your response mock is fluent; safe to keep)
    when(response.setStatusCode(anyInt())).thenReturn(response);

    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put(STATUS, new JsonObject().put(STATUS, COMPLETED)); // not allowed

    when(mongoClient.findOne(eq("col"), any(JsonObject.class), isNull(), any()))
      .thenAnswer(inv -> {
        @SuppressWarnings("unchecked")
        Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
        h.handle(Future.succeededFuture(app));
        return null;
      });

    manager.deleteDistributions(ctx, "col");

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(argThat((String s) -> s.contains(DELETION_CAN_NOT_BE_EXECUTED)));
  }


  // ----------------------------------------------------------------------------
  // putDistributionsApplication(...)
  // ----------------------------------------------------------------------------

  @Test
  void putDistributionsApplication_whenAnyUpdateFails_shouldContextFail() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = spy(new TestManager(db, mongo, audit));

    // (recommended) if your code calls ctx.response() anywhere later, keep these in place
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    doReturn(List.of(Future.failedFuture("nope")))
      .when(testManager).updateApplicationsAsync(ctx, "col");

    testManager.putDistributionsApplication(ctx, "col");

    verify(ctx, timeout(500)).fail(any(Throwable.class));

    verify(response, never()).setStatusCode(HttpResponseStatus.OK.code());
  }


  @Test
  void putDistributionsApplication_whenNoUpdatedApplications_shouldReturn204() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = spy(new TestManager(db, mongo, audit));

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse serverResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(serverResponse);
    when(serverResponse.setStatusCode(anyInt())).thenReturn(serverResponse);

    // CompositeFuture.all succeeds, but only empty results -> updatedApplications remains empty -> 204
    List<Future> updateFutures = List.of(Future.succeededFuture(new JsonObject())); // empty => ignored
    doReturn(updateFutures).when(testManager).updateApplicationsAsync(routingContext, "col");

    testManager.putDistributionsApplication(routingContext, "col");

    verify(serverResponse, timeout(500)).setStatusCode(HttpResponseStatus.NO_CONTENT.code());
    verify(serverResponse, timeout(500)).end();
  }

  @Test
  void putDistributionsApplication_whenUpdatesSucceed_shouldFetchUpdateLanguagesReplaceAndReturn200() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = spy(new TestManager(db, mongo, audit)); // <-- MUST be a spy

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse serverResponse = mock(HttpServerResponse.class);

    when(routingContext.response()).thenReturn(serverResponse);
    when(serverResponse.setStatusCode(anyInt())).thenReturn(serverResponse);

    // avoid NPE in Context.getEcasId(ctx)
    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    JsonObject upd = new JsonObject().put("_id", "app-1").put("changed", true);

    doReturn(List.of(Future.succeededFuture(upd)))
      .when(testManager).updateApplicationsAsync(routingContext, "col");

    // mongo findOne returns full doc
    JsonObject fullDoc = new JsonObject()
      .put("_id", "app-1")
      .put("datasets", new JsonArray())
      .put("catalogs", new JsonArray());

    when(mongo.findOne(eq("col"), any(JsonObject.class), isNull(), any()))
      .thenAnswer(inv -> {
        @SuppressWarnings("unchecked")
        Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
        h.handle(Future.succeededFuture(fullDoc));
        return null;
      });

    doReturn(Future.succeededFuture())
      .when(testManager).updateTargetLanguages(fullDoc);

    doReturn(Future.succeededFuture(new JsonObject().put("_id", "app-1")))
      .when(testManager).replaceApplication(fullDoc, "col");

    testManager.putDistributionsApplication(routingContext, "col");

    verify(mongo).findOne(
      eq("col"),
      argThat(q -> "app-1".equals(q.getString("_id"))),
      isNull(),
      any()
    );

    verify(testManager).updateTargetLanguages(fullDoc);
    verify(testManager).replaceApplication(fullDoc, "col");

    verify(serverResponse).setStatusCode(HttpResponseStatus.OK.code());
    verify(serverResponse).end(argThat((String s) -> {
      JsonArray arr = new JsonArray(s);
      return arr.size() == 1 && "app-1".equals(arr.getJsonObject(0).getString("_id"));
    }));
  }

  @Test
  void getDefaultEnglishFormTranslation_whenSourceLangNotEN_shouldReturnEmpty() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonObject app = new JsonObject()
      .put("form", new JsonObject().put("field", "value"));

    JsonObject result = testManager.getDefaultEnglishFormTranslation("FR", app);

    assertNotNull(result);
    assertTrue(result.isEmpty(), "Expected empty translations when sourceLang != EN");
  }

  @Test
  void getDefaultEnglishFormTranslation_whenSourceLangEN_andTranslatedFilesContainEN_shouldUpdateReferencesAndReturnEnForm() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonObject form = new JsonObject()
      .put("someAttachment", new JsonObject().put("fileId", "orig-1").put("fileName", "orig.pdf"));

    JsonObject app = new JsonObject()
      .put("form", form)
      .put(TRANSLATION, new JsonObject()
        .put("files", new JsonObject()
          .put("orig-1", new JsonObject()
            .put("translatedFiles", new JsonObject()
              .put("en", new JsonObject()
                .put("fileId", "new-1")
                .put(FILE_NAME, "new.pdf")
              )
            )
          )
        )
      );

    // Static method mock: we only need to verify it was called with expected args
    try (MockedStatic<ApplicationFormUtils> mocked = mockStatic(ApplicationFormUtils.class)) {

      JsonObject result = testManager.getDefaultEnglishFormTranslation("EN", app);

      assertNotNull(result);
      assertTrue(result.containsKey("en"));
      JsonObject returnedForm = result.getJsonObject("en");
      assertNotNull(returnedForm);

      // ensure copy() was used: returned form object is not same reference
      assertNotSame(form, returnedForm);

      mocked.verify(() -> ApplicationFormUtils.updateTranslatedFileReferencesInForm(
        any(JsonObject.class),
        eq("orig-1"),
        eq("new-1"),
        eq("new.pdf")
      ), times(1));
    }
  }

  @Test
  void getDefaultEnglishFormTranslation_whenSourceLangEN_andNoTranslationFiles_shouldReturnEnFormWithoutCallingUtil() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonObject form = new JsonObject().put("a", 1);
    JsonObject app = new JsonObject()
      .put("form", form);
    // no TRANSLATION at all -> default new JsonObject -> "files" default new JsonObject

    try (MockedStatic<ApplicationFormUtils> mocked = mockStatic(ApplicationFormUtils.class)) {

      JsonObject result = testManager.getDefaultEnglishFormTranslation("EN", app);

      assertTrue(result.containsKey("en"));
      JsonObject returnedForm = result.getJsonObject("en");
      assertEquals(1, returnedForm.getInteger("a"));
      assertNotSame(form, returnedForm);

      mocked.verifyNoInteractions();
    }
  }


  @Test
  void modifyDatasetBasedOnDispatcherPermit_whenPermitsNull_shouldCreateAndAddPermit() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonObject ds = new JsonObject()
      .put("catalog_id", "cat-1")
      .put("dataset_id", "ds-1");
    JsonArray datasets = new JsonArray().add(ds);

    // reflect private method
    callPrivate(
      testManager,
      "modifyDatasetBasedOnDispatcherPermit",
      new Class<?>[]{JsonArray.class, String.class, String.class, PermitStatus.class, String.class, String.class},
      datasets, "cat-1", "ds-1", PermitStatus.GRANTED, "fileBase64", "reason-1"
    );

    JsonArray permits = ds.getJsonArray("permits");
    assertNotNull(permits);
    assertEquals(1, permits.size());

    JsonObject p = permits.getJsonObject(0);
    assertEquals(1, p.getInteger("latest"));
    assertNotNull(p.getValue("permit_received")); // Instant
    assertEquals("fileBase64", p.getString("permit_file"));
    assertEquals(PermitStatus.GRANTED.getLabel(), p.getString("permit_status"));
    assertEquals("reason-1", p.getString("reason"));
  }

  @Test
  void modifyDatasetBasedOnDispatcherPermit_whenPermitsExist_shouldResetLatestAndAppendNew() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonArray permits = new JsonArray()
      .add(new JsonObject().put("latest", 1).put("permit_status", "OLD"))
      .add(new JsonObject().put("latest", 0).put("permit_status", "OLDER"));

    JsonObject ds = new JsonObject()
      .put("catalog_id", "cat-1")
      .put("dataset_id", "ds-1")
      .put("permits", permits);

    JsonArray datasets = new JsonArray().add(ds);

    callPrivate(
      testManager,
      "modifyDatasetBasedOnDispatcherPermit",
      new Class<?>[]{JsonArray.class, String.class, String.class, PermitStatus.class, String.class, String.class},
      datasets, "cat-1", "ds-1", PermitStatus.RENEWAL_REJECTED, null, null
    );

    JsonArray updated = ds.getJsonArray("permits");
    assertEquals(3, updated.size());

    // old ones must be latest=0
    assertEquals(0, updated.getJsonObject(0).getInteger("latest"));
    assertEquals(0, updated.getJsonObject(1).getInteger("latest"));

    JsonObject newest = updated.getJsonObject(2);
    assertEquals(1, newest.getInteger("latest"));
    assertEquals(PermitStatus.RENEWAL_REJECTED.getLabel(), newest.getString("permit_status"));
    assertFalse(newest.containsKey("permit_file"));
    assertFalse(newest.containsKey("reason"));
  }

  @Test
  void hasAtLeastOneAdditionalInformationAsLatest_whenNullOrEmpty_shouldReturnFalse() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    assertFalse(testManager.hasAtLeastOneAdditionalInformationAsLatest(null));
    assertFalse(testManager.hasAtLeastOneAdditionalInformationAsLatest(new JsonArray()));
  }

  @Test
  void hasAtLeastOneAdditionalInformationAsLatest_whenLatestAdditionalInformationExists_shouldReturnTrue() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonArray catalogs = new JsonArray()
      .add(new JsonObject().put("catalog_id", "c1").put("statuses", new JsonArray()
        .add(new JsonObject().put("status", "SENT").put("latest", 1))
      ))
      .add(new JsonObject().put("catalog_id", "c2").put("statuses", new JsonArray()
        .add(new JsonObject().put("status", "ADDITIONAL_INFORMATION").put("latest", 1))
      ));

    assertTrue(testManager.hasAtLeastOneAdditionalInformationAsLatest(catalogs));
  }

  @Test
  void hasAtLeastOneAdditionalInformationAsLatest_whenOnlyNonLatestAdditionalInformation_shouldReturnFalse() {
    TestManager testManager = new TestManager(dbHandler, mongoClient, auditLogManager);

    JsonArray catalogs = new JsonArray()
      .add(new JsonObject().put("catalog_id", "c1").put("statuses", new JsonArray()
        .add(new JsonObject().put("status", "ADDITIONAL_INFORMATION").put("latest", 0))
        .add(new JsonObject().put("status", "SENT").put("latest", 1))
      ));

    assertFalse(testManager.hasAtLeastOneAdditionalInformationAsLatest(catalogs));
  }

  @Test
  void changeCatalogToSubmitted_whenSaveSucceeds_shouldUpdateAndSendOutgoingAndAudit() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    JsonObject sentStatus = new JsonObject()
      .put("status", CatalogStatus.SENT.getLabel())
      .put("latest", 1)
      .put("dispatcher_status", "SOMETHING"); // expected removed

    JsonObject catalog = new JsonObject()
      .put("catalog_id", "cat-1")
      .put("statuses", new JsonArray().add(sentStatus));

    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put("title", "T1")
      .put("status", new JsonObject().put("status", ApplicationStatus.DRAFT.getStatus()))
      .put("datasets", new JsonArray().add(new JsonObject()
        .put("catalog_id", "cat-1")
        .put("dataset_id", "ds-1")
      ))
      .put("catalogs", new JsonArray().add(catalog));

    // Make updateApplicationStatus predictable (and avoid ctx usage)
    doAnswer(inv -> {
      JsonObject application = inv.getArgument(0);
      JsonObject statusObj = inv.getArgument(1);
      application.put("status", statusObj);
      return null;
    }).when(testManager).updateApplicationStatus(eq(app), any(JsonObject.class), any());

    // Avoid audit side effects
    doNothing().when(testManager).insertAuditLogForApplicationUpdate(anyString(), anyString(), anyString(), any());
    doNothing().when(testManager).insertAuditLogForHDABUpdate(anyString(), anyString(), anyString(), any());

    // save succeeds -> triggers outgoing + hdab audit
    doReturn(Future.succeededFuture(new JsonObject().put("ok", true)))
      .when(testManager).saveApplication(app, "col");

    doNothing().when(testManager).sendApplicationToOutgoingApplicationActivities(
      eq("col"), eq("app-1"), eq(CatalogStatus.SUBMITTED), eq("cat-1"), isNull()
    );

    testManager.changeCatalogToSubmitted(app, "cat-1", "col");

    // status promoted
    assertEquals(ApplicationStatus.ON_GOING.getStatus(), app.getJsonObject("status").getString("status"));

    // catalogs updated to SUBMITTED latest=1 somewhere
    JsonObject updatedCatalog = app.getJsonArray("catalogs").getJsonObject(0);
    JsonArray statuses = updatedCatalog.getJsonArray("statuses");
    assertNotNull(statuses);
    assertTrue(
      statuses.stream()
        .map(JsonObject.class::cast)
        .anyMatch(s -> CatalogStatus.SUBMITTED.getLabel().equals(s.getString("status")) && s.getInteger("latest", 0) == 1),
      "Expected SUBMITTED latest=1 in catalog statuses"
    );

    // dispatcher_status removed (best-effort; only if your private removeDispatcherStatus does it)
    JsonObject latestEntry = statuses.stream().map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst().orElse(new JsonObject());
    assertFalse(latestEntry.containsKey("dispatcher_status"));

    verify(testManager).saveApplication(app, "col");
    verify(testManager).sendApplicationToOutgoingApplicationActivities("col", "app-1", CatalogStatus.SUBMITTED, "cat-1", null);
    verify(testManager).insertAuditLogForHDABUpdate(
      nullable(String.class),
      eq("col"),
      eq("app-1"),
      isNull()
    );
  }

  @Test
  void changePermitStatus_shouldNotThrow_whenUserPresent() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = new TestManager(db, mongo, audit);

    RoutingContext routingContext = mock(RoutingContext.class);
    RequestBody body = mock(RequestBody.class);

    io.vertx.ext.auth.User user = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("ecasId", "ecas-1"));

    when(routingContext.pathParam(Constants.APPLICATION_ID)).thenReturn("app-1");
    when(routingContext.pathParam(Constants.CATALOG_ID)).thenReturn("cat-1");
    when(routingContext.pathParam(Constants.DATASET_ID)).thenReturn("ds-1");

    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject()
      .put(Constants.STATUS, "GRANTED"));

    assertDoesNotThrow(() -> testManager.changePermitStatus(routingContext));
  }

  @Test
  void triggerRemainingTranslations_whenTargetLanguagesMissing_shouldReturnWithoutTriggering() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put("inputLanguage", "EN")
      .put("form", new JsonObject().put("x", 1))
      // translation present but targetLanguages missing
      .put("translation", new JsonObject());

    testManager.triggerRemainingTranslations("app-1", app, "DATA_REQUEST_APPLICATION");

    verify(testManager, never()).triggerFormTranslationRequest(
      anyString(), any(JsonObject.class), anyString(), anyString(), anyList(), any(), anyBoolean()
    );
  }

  @Test
  void triggerRemainingTranslations_whenNoRemainingLanguages_shouldReturnWithoutTriggering() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    // If targetLanguages includes everything except source, remaining becomes empty
    // We include all the ALL_LANGUAGES except EN (source)
    JsonArray allExceptEn = new JsonArray()
      .add("BG").add("FR").add("NL").add("CS").add("GA").add("PL").add("DA").add("HR").add("PT")
      .add("DE").add("HU").add("RO").add("EL").add("IT").add("SK").add("LT").add("SL").add("ES")
      .add("LV").add("FI").add("ET").add("MT").add("SV").add("IS").add("NB").add("NN");

    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put("inputLanguage", "EN")
      .put("form", new JsonObject().put("x", 1))
      .put("translation", new JsonObject().put("targetLanguages", allExceptEn));

    testManager.triggerRemainingTranslations("app-1", app, "DATA_REQUEST_APPLICATION");

    verify(testManager, never()).triggerFormTranslationRequest(
      anyString(), any(JsonObject.class), anyString(), anyString(), anyList(), any(), anyBoolean()
    );
  }

  @Test
  void triggerRemainingTranslations_whenRemainingLanguagesExist_shouldTriggerFormTranslationRequestWithComputedList() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    // source = EN, targetLanguages already contains FR + de (mixed case), plus EN (will be removed)
    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put("inputLanguage", "en")
      .put("form", new JsonObject().put("field", "value"))
      .put("translation", new JsonObject()
        .put("targetLanguages", new JsonArray().add("fr").add("DE").add("EN"))
      );

    doNothing().when(testManager).triggerFormTranslationRequest(
      anyString(), any(JsonObject.class), anyString(), anyString(), anyList(), isNull(), eq(false)
    );

    testManager.triggerRemainingTranslations("app-1", app, "DATA_REQUEST_APPLICATION");

    verify(testManager).triggerFormTranslationRequest(
      eq("app-1"),
      eq(app.getJsonObject("form")),
      eq(Constants.DATA_REQUEST_APPLICATION_COLLECTION),
      eq("EN"),
      argThat((List<String> langs) -> {
        if (langs == null || langs.isEmpty()) return false;
        // must not contain source
        if (langs.contains("EN")) return false;
        // must not contain already targeted
        if (langs.contains("FR") || langs.contains("DE")) return false;
        // should contain at least one known language from list
        return langs.contains("BG") || langs.contains("NL") || langs.contains("IT") || langs.contains("ES");
      }),
      isNull(),
      eq(false)
    );
  }

  @Test
  void triggerRemainingTranslations_whenTriggerThrows_shouldBeCaughtAndNotPropagate() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    CommonApplicationManager testManager = spy(new TestManager(db, mongo, audit));

    JsonObject app = new JsonObject()
      .put("_id", "app-1")
      .put("inputLanguage", "EN")
      .put("form", new JsonObject().put("field", "value"))
      .put("translation", new JsonObject()
        .put("targetLanguages", new JsonArray().add("FR")) // remaining exists
      );

    doThrow(new RuntimeException("boom")).when(testManager).triggerFormTranslationRequest(
      anyString(), any(JsonObject.class), anyString(), anyString(), anyList(), any(), anyBoolean()
    );

    assertDoesNotThrow(() -> testManager.triggerRemainingTranslations("app-1", app, "DATA_REQUEST_APPLICATION"));
  }

  @Test
  void incrementTranslationAttempts_whenFileIdProvided_shouldUsePendingGuardAndIncFilePath() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    // drive callback to succeed (just to execute handler)
    when(mongo.findOneAndUpdate(eq("col"), any(JsonObject.class), any(JsonObject.class), any()))
      .thenAnswer(inv -> {
        Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
        h.handle(Future.succeededFuture(new JsonObject()));
        return null;
      });

    testManager.incrementTranslationAttempts("app-1", "col", "SOME_ERROR", "file-9");

    verify(mongo).findOneAndUpdate(
      eq("col"),
      argThat((JsonObject q) -> {
        // base id
        if (!"app-1".equals(q.getString(Constants.ID))) return false;
        // pending guard for file
        String guardKey = "translation.files.file-9.status";
        return "PENDING".equals(q.getString(guardKey));
      }),
      argThat((JsonObject upd) -> {
        JsonObject set = upd.getJsonObject("$set");
        JsonObject inc = upd.getJsonObject("$inc");
        if (set == null || inc == null) return false;

        // error type + date_modified exist
        if (!"SOME_ERROR".equals(set.getString("translation.errorType"))) return false;
        if (set.getValue(Constants.DATE_MODIFIED) == null) return false;

        String prefix = "translation.files.file-9.";

        // attempts uses $ifNull
        Object attemptsObj = set.getValue(prefix + "attempts");
        if (!(attemptsObj instanceof JsonObject)) return false;
        if (!((JsonObject) attemptsObj).containsKey("$ifNull")) return false;

        // lastRetryAt exists and is a string
        if (set.getString(prefix + "lastRetryAt") == null) return false;

        // inc path is prefix (note: code does put(path, 1))
        Integer incVal = inc.getInteger(prefix);
        return incVal != null && incVal == 1;
      }),
      any()
    );
  }

  @Test
  void incrementTranslationAttempts_whenNoFileId_shouldIncFormPathAndNoPendingGuard() {
    DBHandler db = mock(DBHandler.class);
    MongoClient mongo = mock(MongoClient.class);
    AuditLogManager audit = mock(AuditLogManager.class);

    TestManager testManager = new TestManager(db, mongo, audit);

    when(mongo.findOneAndUpdate(eq("col"), any(JsonObject.class), any(JsonObject.class), any()))
      .thenAnswer(inv -> {
        Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
        h.handle(Future.succeededFuture(new JsonObject()));
        return null;
      });

    testManager.incrementTranslationAttempts("app-2", "col", "OTHER_ERROR", null);

    verify(mongo).findOneAndUpdate(
      eq("col"),
      argThat((JsonObject q) -> {
        if (!"app-2".equals(q.getString(Constants.ID))) return false;
        // ensure no pending guard key present (we just check it doesn't contain "translation.files.")
        return q.fieldNames().stream().noneMatch(k -> k.startsWith("translation.files."));
      }),
      argThat((JsonObject upd) -> {
        JsonObject set = upd.getJsonObject("$set");
        JsonObject inc = upd.getJsonObject("$inc");
        if (set == null || inc == null) return false;

        if (!"OTHER_ERROR".equals(set.getString("translation.errorType"))) return false;
        if (set.getValue(Constants.DATE_MODIFIED) == null) return false;

        String prefix = "translation.form.";

        Object attemptsObj = set.getValue(prefix + "attempts");
        if (!(attemptsObj instanceof JsonObject)) return false;
        if (!((JsonObject) attemptsObj).containsKey("$ifNull")) return false;

        if (set.getString(prefix + "lastRetryAt") == null) return false;

        Integer incVal = inc.getInteger(prefix);
        return incVal != null && incVal == 1;
      }),
      any()
    );
  }





}
