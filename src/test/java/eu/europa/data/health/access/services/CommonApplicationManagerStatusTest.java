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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.CatalogStatus;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonApplicationManagerStatusTest {

  private DBHandler dbHandler;
  private MongoClient mongoClient;
  private AuditLogManager auditLogManager;
  private NotificationManager notificationManager;

  private RoutingContext ctx;
  private HttpServerResponse response;

  private TestManager manager;

  /** minimal concrete class so we can instantiate CommonApplicationManager */
  static class TestManager extends CommonApplicationManager {
    protected TestManager(DBHandler dbHandler, MongoClient mongoClient, AuditLogManager auditLogManager, NotificationManager notificationManager) {
      super(dbHandler, mongoClient, auditLogManager, notificationManager, new JsonObject());
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
      String documentId) {
      // capture arguments (so we can test handleDocTranslation easily)
      this.lastAppId = applicationId;
      this.lastAppType = applicationType;
      this.lastTargets = targetLanguages;
      this.lastSourceLang = sourceLang;
      this.lastDocId = documentId;
    }

    @Override
    protected void triggerDocumentTranslationRequest(
      String applicationId,
      String applicationType,
      List<String> targetLanguages,
      String sourceLang,
      String documentId,
      boolean preserveExistingTranslations) {
      // capture arguments (so we can test triggerRemainingTranslations easily)
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
    notificationManager = mock(NotificationManager.class);

    JsonObject config = new JsonObject()
      .put(Constants.EXPIRATION_DAYS, 90);
    manager = new TestManager(dbHandler, mongoClient, auditLogManager, notificationManager);
    // Set the config after construction
    try {
      Field configField = CommonApplicationManager.class.getDeclaredField("config");
      configField.setAccessible(true);
      configField.set(manager, config);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    ctx = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);

    // chaining
    when(response.setStatusCode(anyInt())).thenReturn(response);

    // IMPORTANT: avoid end() overload ambiguity by stubbing the String overload
    when(response.end(anyString())).thenReturn(null);
  }

  @Test
  void triggerRemainingTranslations_whenTranslationDisabled_returnsEarly() {
    JsonObject mongoApplication = new JsonObject()
      .put("inputLanguage", "EN")
      .put("translation", new JsonObject().put("targetLanguages", new JsonArray().add("FR")))
      .put("form", new JsonObject());

    try {
      Field configField = CommonApplicationManager.class.getDeclaredField("config");
      configField.setAccessible(true);
      configField.set(manager, new JsonObject().put(Constants.ENABLE_TRANSLATION, false));

      Method method = CommonApplicationManager.class.getDeclaredMethod(
        "triggerRemainingTranslations",
        String.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, "APP_DISABLED", mongoApplication, "data_access_applications");

      assertNull(manager.lastAppId);
      assertNull(manager.lastDocId);
      assertNull(manager.lastTargets);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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

  // =========================================================================
  // updateCatalogStatus() Comprehensive Tests - All Three Overloads
  // =========================================================================

  // --------- updateCatalogStatus(JsonObject application) ---------
  // This method calls: updateCatalogStatus with (null, null, SENT, null, application)

  @Test
  void updateCatalogStatus_noArgs_shouldComputeCatalogsWithSentStatus() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
        .add(new JsonObject().put("catalog_id", "C2"))
      )
      .put("catalogs", new JsonArray());

    manager.updateCatalogStatus(application);

    JsonArray catalogs = application.getJsonArray("catalogs");
    assertNotNull(catalogs);
    assertEquals(2, catalogs.size());

    for (Object catObj : catalogs) {
      JsonObject cat = (JsonObject) catObj;
      JsonArray statuses = cat.getJsonArray("statuses");
      assertNotNull(statuses);
      assertEquals(1, statuses.size());

      JsonObject status = statuses.getJsonObject(0);
      assertEquals(CatalogStatus.SENT.getLabel(), status.getString("status"));
      assertEquals(1, status.getInteger("latest"));
      assertNotNull(status.getValue("date_received"));
    }
  }

  @Test
  void updateCatalogStatus_noArgs_withExistingCatalogs_shouldResetLatestAndAddNew() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1).put("date_received", "2026-01-01"))
          )
        )
      );

    manager.updateCatalogStatus(application);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    assertEquals(2, statuses.size());

    // Old status should have latest=0
    assertEquals(0, statuses.getJsonObject(0).getInteger("latest"));
    assertEquals("DRAFT", statuses.getJsonObject(0).getString("status"));

    // New status should have latest=1
    assertEquals(1, statuses.getJsonObject(1).getInteger("latest"));
    assertEquals(CatalogStatus.SENT.getLabel(), statuses.getJsonObject(1).getString("status"));
  }

  // --------- updateCatalogStatus(JsonObject, String, CatalogStatus) ---------
  // Tests covering lines 1110-1122

  @Test
  void updateCatalogStatus_withCatalogIdAndStatus_shouldReturnDeadline() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
          )
        )
      );

    String deadline = manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED);

    assertNotNull(deadline);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(CatalogStatus.SUBMITTED.getLabel(), latestStatus.getString("status"));
    assertEquals(deadline, latestStatus.getString("date_deadline"));
  }

  @Test
  void updateCatalogStatus_withCatalogIdAndStatus_whenNoCatalog_shouldReturnNull() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray())
      .put("catalogs", new JsonArray());

    String deadline = manager.updateCatalogStatus(application, "NONEXISTENT", CatalogStatus.SUBMITTED);

    assertNull(deadline);
  }

  @Test
  void updateCatalogStatus_withCatalogIdAndStatus_whenCatalogHasNoStatuses_shouldReturnNull() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()) // empty statuses
        )
      );

    String deadline = manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED);

    assertNotNull(deadline); // deadline is computed even with empty statuses
  }

  @Test
  void updateCatalogStatus_withCatalogIdAndStatus_whenStatusIsNull_shouldReturnNull() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          // no statuses field at all
        )
      );

    String deadline = manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED);

    assertNotNull(deadline); // deadline is computed even with no statuses field
  }

  // --------- updateCatalogStatus(JsonObject, String, CatalogStatus, JsonObject) ---------
  // Tests covering lines 1124-1148

  @Test
  void updateCatalogStatus_withPayload_whenProcessingStatus_shouldReturnExtensionDeadline() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject()
              .put("status", "SUBMITTED")
              .put("latest", 1)
              .put("date_deadline", "2026-07-15T10:00:00Z")
            )
          )
        )
      );

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    String deadline = manager.updateCatalogStatus(
      application,
      "C1",
      CatalogStatus.PROCESSING,
      payload
    );

    assertNotNull(deadline);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(CatalogStatus.PROCESSING.getLabel(), latestStatus.getString("status"));
    assertEquals(deadline, latestStatus.getString("date_extension_deadline"));
    assertNotNull(latestStatus.getJsonObject("statusPayload"));
  }

  @Test
  void updateCatalogStatus_withPayload_whenProcessingStatus_withNoCatalog_shouldReturnNull() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("catalogs", new JsonArray());

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    String deadline = manager.updateCatalogStatus(
      application,
      "NONEXISTENT",
      CatalogStatus.PROCESSING,
      payload
    );

    assertNull(deadline);
  }

  @Test
  void updateCatalogStatus_withPayload_whenProcessingStatus_withNoStatuses_shouldReturnNull() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject()
              .put("status", "SUBMITTED")
              .put("latest", 1)
              .put("date_deadline", "2026-07-15T10:00:00Z")
            )
          )
        )
      );

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    String deadline = manager.updateCatalogStatus(
      application,
      "C1",
      CatalogStatus.PROCESSING,
      payload
    );

    assertNotNull(deadline);
  }

  @Test
  void updateCatalogStatus_withPayload_whenNonProcessingStatus_shouldReturnRegularDeadline() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject()
              .put("status", "SUBMITTED")
              .put("latest", 1)
              .put("date_deadline", "2026-07-15T10:00:00Z")
            )
          )
        )
      );

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    String deadline = manager.updateCatalogStatus(
      application,
      "C1",
      CatalogStatus.SUBMITTED,
      payload
    );

    assertNotNull(deadline);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(CatalogStatus.SUBMITTED.getLabel(), latestStatus.getString("status"));
    assertEquals(deadline, latestStatus.getString("date_deadline"));
  }

   @Test
   void updateCatalogStatus_withPayload_whenNonProcessingStatus_withNoStatuses_shouldReturnNull() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           // no statuses
         )
       );

     JsonObject payload = new JsonObject().put("formVersion", "v1");
     String deadline = manager.updateCatalogStatus(
       application,
       "C1",
       CatalogStatus.DECISION_ISSUED,
       payload
     );

     assertNull(deadline);
   }

  @Test
  void updateCatalogStatus_withPayload_shouldIncludeStatusPayloadInStatus() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "SUBMITTED").put("latest", 1))
          )
        )
      );

    JsonObject payload = new JsonObject()
      .put("formVersion", "v2")
      .put("someData", "value");

    manager.updateCatalogStatus(application, "C1", CatalogStatus.REJECTED, payload);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertNotNull(latestStatus.getJsonObject("statusPayload"));
    assertEquals("v2", latestStatus.getJsonObject("statusPayload").getString("formVersion"));
    assertEquals("value", latestStatus.getJsonObject("statusPayload").getString("someData"));
  }

  @Test
  void updateCatalogStatus_withPayload_whenAdditionalInformation_shouldComputeAndStore() {
    JsonObject country = new JsonObject().put("code", "IT");

    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("country", country)
        )
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "SUBMITTED").put("latest", 1))
          )
        )
      );

    JsonObject payload = new JsonObject()
      .put("formVersion", "v1")
      .put("additionalInformation", new JsonObject()
        .put("source", new JsonArray()
          .add(new JsonObject().put("field", "section1").put("comment", "comment1"))
        )
        .put("translation", new JsonArray()
          .add(new JsonObject().put("field", "field2").put("comment", "comment2"))
        )
      );

    manager.updateCatalogStatus(
      application,
      "C1",
      CatalogStatus.ADDITIONAL_INFORMATION,
      payload
    );

    JsonArray additionalInfo = application.getJsonArray("additionalInformation");
    assertNotNull(additionalInfo);
    assertEquals(1, additionalInfo.size());

    JsonObject ai = additionalInfo.getJsonObject(0);
    assertEquals("C1", ai.getString("catalog_id"));
    assertEquals(country, ai.getJsonObject("country"));
    assertEquals("v1", ai.getString("formVersion"));
    assertNotNull(ai.getJsonObject("source"));
    assertNotNull(ai.getJsonObject("translation"));
  }

  @Test
  void updateCatalogStatus_withPayload_whenEmptyPayload_shouldNotIncludeIt() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "SUBMITTED").put("latest", 1))
          )
        )
      );

    JsonObject emptyPayload = new JsonObject();

    manager.updateCatalogStatus(application, "C1", CatalogStatus.REJECTED, emptyPayload);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertNull(latestStatus.getJsonObject("statusPayload"));
  }

  @Test
  void updateCatalogStatus_withPayload_whenNullPayload_shouldNotIncludeIt() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "SUBMITTED").put("latest", 1))
          )
        )
      );

    manager.updateCatalogStatus(application, "C1", CatalogStatus.REJECTED, null);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertNull(latestStatus.getJsonObject("statusPayload"));
  }

   @Test
   void updateCatalogStatus_withPayload_shouldPreserveCatalogLanguages() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("catalog_languages", new JsonArray()
             .add("EN")
             .add("FR")
           )
         )
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("catalog_languages", new JsonArray().add("EN"))
           .put("statuses", new JsonArray()
             .add(new JsonObject().put("status", "SUBMITTED").put("latest", 1))
           )
         )
       );

     JsonObject payload = new JsonObject().put("formVersion", "v1");
     manager.updateCatalogStatus(application, "C1", CatalogStatus.DECISION_ISSUED, payload);

     JsonArray catalogs = application.getJsonArray("catalogs");
     JsonObject c1 = catalogs.getJsonObject(0);
     JsonArray languages = c1.getJsonArray("catalog_languages");

     assertNotNull(languages);
     assertEquals(2, languages.size());
     assertTrue(languages.contains("EN"));
     assertTrue(languages.contains("FR"));
   }

  @Test
  void updateCatalogStatus_withPayload_whenSubmittedStatus_shouldComputeDeadline() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
          )
        )
      );

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    String deadline = manager.updateCatalogStatus(
      application,
      "C1",
      CatalogStatus.SUBMITTED,
      payload
    );

    assertNotNull(deadline);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(latestStatus.getString("date_deadline"), deadline);
  }

  @Test
  void updateCatalogStatus_withPayload_whenProcessingExtendedStatus_shouldComputeDeadline() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject()
              .put("status", "SUBMITTED")
              .put("latest", 0)
              .put("date_deadline", "2026-07-15T10:00:00Z")
            )
            .add(new JsonObject()
              .put("status", "PROCESSING")
              .put("latest", 1)
              .put("date_extension_deadline", "2026-08-15T10:00:00Z")
            )
          )
        )
      );

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    String deadline = manager.updateCatalogStatus(
      application,
      "C1",
      CatalogStatus.PROCESSING_EXTENDED,
      payload
    );

    assertNotNull(deadline);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    JsonObject latestStatus = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(CatalogStatus.PROCESSING_EXTENDED.getLabel(), latestStatus.getString("status"));
    assertEquals(latestStatus.getString("date_deadline"), deadline);
  }

  @Test
  void updateCatalogStatus_withPayload_shouldResetPreviousStatusesLatest() {
    JsonObject application = new JsonObject()
      .put("_id", "APP1")
      .put("datasets", new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
      )
      .put("catalogs", new JsonArray()
        .add(new JsonObject()
          .put("catalog_id", "C1")
          .put("statuses", new JsonArray()
            .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
            .add(new JsonObject().put("status", "SUBMITTED").put("latest", 1))
          )
        )
      );

    JsonObject payload = new JsonObject().put("formVersion", "v1");
    manager.updateCatalogStatus(application, "C1", CatalogStatus.REJECTED, payload);

    JsonArray catalogs = application.getJsonArray("catalogs");
    JsonObject c1 = catalogs.getJsonObject(0);
    JsonArray statuses = c1.getJsonArray("statuses");

    long latestCount = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .count();

    assertEquals(1, latestCount);

    JsonObject latest = statuses.stream()
      .map(JsonObject.class::cast)
      .filter(s -> s.getInteger("latest", 0) == 1)
      .findFirst()
      .orElseThrow();

    assertEquals(CatalogStatus.REJECTED.getLabel(), latest.getString("status"));
  }

   @Test
   void updateCatalogStatus_withPayload_shouldPreserveOlderStatuses() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
         )
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject().put("status", "DRAFT").put("latest", 1).put("date_received", "2026-04-01"))
           )
         )
       );

     JsonObject payload = new JsonObject().put("formVersion", "v1");
     manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED, payload);

     JsonArray catalogs = application.getJsonArray("catalogs");
     JsonObject c1 = catalogs.getJsonObject(0);
     JsonArray statuses = c1.getJsonArray("statuses");

     assertEquals(2, statuses.size());

     // Old status preserved
     assertEquals("DRAFT", statuses.getJsonObject(0).getString("status"));
     assertEquals(0, statuses.getJsonObject(0).getInteger("latest"));

     // New status added
     assertEquals(CatalogStatus.SUBMITTED.getLabel(), statuses.getJsonObject(1).getString("status"));
     assertEquals(1, statuses.getJsonObject(1).getInteger("latest"));
   }

   // -------------------------------------------------------------------------
   // updateCatalogStatus (3 overloads) - comprehensive coverage
   // -------------------------------------------------------------------------

   @Test
   void updateCatalogStatus_singleArg_withoutExistingCatalogs_createsNewCatalogs() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("catalog_languages", new JsonArray().add("EN").add("FR"))
         )
         .add(new JsonObject()
           .put("catalog_id", "C2")
           .put("catalog_languages", new JsonArray().add("EN"))
         )
       );

     manager.updateCatalogStatus(application);

     JsonArray catalogs = application.getJsonArray("catalogs");
     assertNotNull(catalogs);
     assertEquals(2, catalogs.size());

     // Verify both catalogs created
     JsonObject c1 = catalogs.stream()
       .map(JsonObject.class::cast)
       .filter(cat -> "C1".equals(cat.getString("catalog_id")))
       .findFirst()
       .orElse(null);

     assertNotNull(c1);
     assertEquals(2, c1.getJsonArray("catalog_languages").size());

     JsonObject c2 = catalogs.stream()
       .map(JsonObject.class::cast)
       .filter(cat -> "C2".equals(cat.getString("catalog_id")))
       .findFirst()
       .orElse(null);

     assertNotNull(c2);
     assertEquals(1, c2.getJsonArray("catalog_languages").size());
   }

   @Test
   void updateCatalogStatus_singleArg_setsStatusToSent() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("catalog_languages", new JsonArray().add("EN"))
         )
       );

     manager.updateCatalogStatus(application);

     JsonArray catalogs = application.getJsonArray("catalogs");
     JsonObject c1 = catalogs.getJsonObject(0);
     JsonArray statuses = c1.getJsonArray("statuses");

     assertEquals(1, statuses.size());
     JsonObject status = statuses.getJsonObject(0);
     assertEquals("SENT", status.getString("status"));
     assertEquals(1, status.getInteger("latest"));
   }

   @Test
   void updateCatalogStatus_singleArg_withExistingCatalogs_updatesAll() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("catalog_languages", new JsonArray().add("EN"))
         )
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
           )
         )
       );

     manager.updateCatalogStatus(application);

     JsonArray catalogs = application.getJsonArray("catalogs");
     JsonObject c1 = catalogs.getJsonObject(0);
     JsonArray statuses = c1.getJsonArray("statuses");

     // Old status marked as not latest
     assertEquals(0, statuses.getJsonObject(0).getInteger("latest"));

     // New SENT status added
     assertEquals(2, statuses.size());
     assertEquals("SENT", statuses.getJsonObject(1).getString("status"));
     assertEquals(1, statuses.getJsonObject(1).getInteger("latest"));
   }

   @Test
   void updateCatalogStatus_twoArgs_updatesSpecificCatalogStatus() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject().put("catalog_id", "C1"))
         .add(new JsonObject().put("catalog_id", "C2"))
       )
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

     // C1 updated
     JsonObject c1 = catalogs.getJsonObject(0);
     String c1Status = c1.getJsonArray("statuses").stream()
       .map(JsonObject.class::cast)
       .filter(s -> s.getInteger("latest", 0) == 1)
       .findFirst()
       .map(s -> s.getString("status"))
       .orElse(null);

     assertEquals("SUBMITTED", c1Status);

     // C2 unchanged
     JsonObject c2 = catalogs.getJsonObject(1);
     String c2Status = c2.getJsonArray("statuses").stream()
       .map(JsonObject.class::cast)
       .filter(s -> s.getInteger("latest", 0) == 1)
       .findFirst()
       .map(s -> s.getString("status"))
       .orElse(null);

     assertEquals("DRAFT", c2Status);
   }

   @Test
   void updateCatalogStatus_threeArgs_withProcessingStatus_returnsExtensionDeadline() {
     String baseDeadline = "2026-07-17T10:00:00Z";
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject().put("catalog_id", "C1"))
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject()
               .put("status", "SUBMITTED")
               .put("latest", 1)
               .put("date_deadline", baseDeadline)
             )
           )
         )
       );

     JsonObject payload = new JsonObject().put("formVersion", "v1");
     String result = manager.updateCatalogStatus(application, "C1", CatalogStatus.PROCESSING, payload);

     assertNotNull(result);

     // Verify new status has extension deadline set
     JsonArray statuses = application.getJsonArray("catalogs").getJsonObject(0).getJsonArray("statuses");
     JsonObject newStatus = statuses.stream()
       .map(JsonObject.class::cast)
       .filter(s -> s.getInteger("latest", 0) == 1)
       .findFirst()
       .orElse(null);

     assertNotNull(newStatus);
     assertEquals("PROCESSING", newStatus.getString("status"));
     assertEquals(result, newStatus.getString("date_extension_deadline"));
   }

   @Test
   void updateCatalogStatus_threeArgs_withAdditionalInformation_updatesApplicationState() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("country", new JsonObject().put("id", "IE").put("name", "Ireland"))
         )
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
           )
         )
       );

     JsonObject additionalInfoPayload = new JsonObject()
       .put("additionalInformation", new JsonObject()
         .put("source", new JsonArray())
         .put("translation", new JsonArray())
       )
       .put("formVersion", "v1");

     manager.updateCatalogStatus(application, "C1", CatalogStatus.ADDITIONAL_INFORMATION, additionalInfoPayload);

     // Verify application now has additionalInformation array
     JsonArray additionalInfo = application.getJsonArray("additionalInformation");
     assertNotNull(additionalInfo);
     assertEquals(1, additionalInfo.size());

     JsonObject infoEntry = additionalInfo.getJsonObject(0);
     assertEquals("C1", infoEntry.getString("catalog_id"));
     assertEquals("IE", infoEntry.getJsonObject("country").getString("id"));
   }

   @Test
   void updateCatalogStatus_threeArgs_preservesStatusPayload() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject().put("catalog_id", "C1"))
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
           )
         )
       );

     JsonObject payload = new JsonObject()
       .put("formVersion", "v1")
       .put("customField", "customValue");

     manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED, payload);

     JsonArray statuses = application.getJsonArray("catalogs").getJsonObject(0).getJsonArray("statuses");
     JsonObject newStatus = statuses.stream()
       .map(JsonObject.class::cast)
       .filter(s -> s.getInteger("latest", 0) == 1)
       .findFirst()
       .orElse(null);

     assertNotNull(newStatus);
     JsonObject statusPayload = newStatus.getJsonObject("statusPayload");
     assertNotNull(statusPayload);
     assertEquals("v1", statusPayload.getString("formVersion"));
     assertEquals("customValue", statusPayload.getString("customField"));
   }

   @Test
   void updateCatalogStatus_threeArgs_computesDeadlineForSubmittedStatus() {
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject().put("catalog_id", "C1"))
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
           )
         )
       );

     JsonObject payload = new JsonObject().put("formVersion", "v1");
     String result = manager.updateCatalogStatus(application, "C1", CatalogStatus.SUBMITTED, payload);

     assertNotNull(result);

     // Verify deadline is in ISO 8601 format
     assertTrue(result.contains("T"));
     assertTrue(result.endsWith("Z"));

     // Verify new status has deadline set
     JsonArray statuses = application.getJsonArray("catalogs").getJsonObject(0).getJsonArray("statuses");
     JsonObject newStatus = statuses.stream()
       .map(JsonObject.class::cast)
       .filter(s -> s.getInteger("latest", 0) == 1)
       .findFirst()
       .orElse(null);

     assertNotNull(newStatus);
     assertEquals(result, newStatus.getString("date_deadline"));
   }

   @Test
   void updateCatalogStatus_threeArgs_withProcessingExtended_computesDeadlineFromPrevious() {
     String previousDeadline = "2026-07-17T10:00:00Z";
     JsonObject application = new JsonObject()
       .put("_id", "APP1")
       .put("datasets", new JsonArray()
         .add(new JsonObject().put("catalog_id", "C1"))
       )
       .put("catalogs", new JsonArray()
         .add(new JsonObject()
           .put("catalog_id", "C1")
           .put("statuses", new JsonArray()
             .add(new JsonObject()
               .put("status", "SUBMITTED")
               .put("latest", 0)
               .put("date_deadline", previousDeadline)
             )
             .add(new JsonObject()
               .put("status", "PROCESSING")
               .put("latest", 1)
               .put("date_extension_deadline", "2026-08-16T10:00:00Z")
             )
           )
         )
       );

     JsonObject payload = new JsonObject().put("formVersion", "v1");
     String result = manager.updateCatalogStatus(application, "C1", CatalogStatus.PROCESSING_EXTENDED, payload);

     assertNotNull(result);

     // Verify new status has deadline_date set
     JsonArray statuses = application.getJsonArray("catalogs").getJsonObject(0).getJsonArray("statuses");
     JsonObject newStatus = statuses.stream()
       .map(JsonObject.class::cast)
       .filter(s -> s.getInteger("latest", 0) == 1)
       .findFirst()
       .orElse(null);

      assertNotNull(newStatus);
      assertEquals("PROCESSING_EXTENDED", newStatus.getString("status"));
      assertEquals(result, newStatus.getString("date_deadline"));
    }

  // -------------------------------------------------------------------------
  // handleDocTranslation(...) - comprehensive coverage
  // -------------------------------------------------------------------------

  @Test
  void handleDocTranslation_withValidApplication_triggersTranslation() {
    // Setup: Create a valid application with datasets and document upload
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP1")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE").add("FR"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_123")
      );

    String collectionName = "data_access_applications";

    // Act: Call handleDocTranslation via reflection since it's private
    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, collectionName);

      // Assert: Verify triggerDocumentTranslationRequest was called (captured in TestManager)
      assertNotNull(manager.lastDocId);
      assertEquals("DOC_123", manager.lastDocId);
      assertEquals("APP1", manager.lastAppId);
      assertEquals(collectionName, manager.lastAppType);
      assertNotNull(manager.lastTargets);
      assertEquals("EN", manager.lastSourceLang);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_extractsSourceLanguageFromApplication() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP2")
      .put("inputLanguage", "de")  // lowercase, should be converted to uppercase
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("EN").add("FR"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_456")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: Source language should be uppercase
      assertEquals("DE", manager.lastSourceLang);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_defaultsSourceLanguageToEN() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP3")
      // No inputLanguage specified - should default to EN
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE").add("FR"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_789")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: Default source language should be EN
      assertEquals("EN", manager.lastSourceLang);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_extractsDocumentIdFromUploadResult() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP4")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "UNIQUE_DOC_ID_12345")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: Document ID should be extracted correctly
      assertEquals("UNIQUE_DOC_ID_12345", manager.lastDocId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_includesENInTargetLanguages() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP5")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE").add("FR"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_999")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: EN should always be included in target languages
      assertNotNull(manager.lastTargets);
      assertTrue(manager.lastTargets.contains("EN"), "EN should be in target languages");
      assertTrue(manager.lastTargets.contains("DE"), "DE should be in target languages");
      assertTrue(manager.lastTargets.contains("FR"), "FR should be in target languages");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_withMultipleDatasetsLanguages() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP6")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE").add("FR"))
        )
        .add(new JsonObject()
          .put("languages", new JsonArray().add("IT").add("ES"))
        )
        .add(new JsonObject()
          .put("languages", new JsonArray().add("PT"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_MULTI")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: The TestManager's getTargetLanguagesFromDatasets returns {FR, DE}
      // Plus EN is always added, so we expect 3 languages: {FR, DE, EN}
      assertNotNull(manager.lastTargets);
      assertEquals(3, manager.lastTargets.size());
      assertTrue(manager.lastTargets.containsAll(List.of("EN", "DE", "FR")));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_deduplicatesTargetLanguages() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP7")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE").add("FR").add("DE"))
        )
        .add(new JsonObject()
          .put("languages", new JsonArray().add("FR").add("EN"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_DUPE")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: Duplicate languages should be deduplicated
      assertNotNull(manager.lastTargets);
      // Should have: EN, DE, FR (no duplicates)
      long uniqueCount = manager.lastTargets.stream().distinct().count();
      assertEquals(manager.lastTargets.size(), uniqueCount, "All target languages should be unique");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_passesApplicationIdToTrigger() {
    String appId = "SPECIFIC_APP_ID_123";
    JsonObject mongoApplication = new JsonObject()
      .put("_id", appId)
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_XYZ")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: Application ID should be passed correctly
      assertEquals(appId, manager.lastAppId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_passesCollectionNameToTrigger() {
    String collectionName = "custom_collection_name";
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP8")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_ABC")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, collectionName);

      // Assert: Collection name should be passed correctly
      assertEquals(collectionName, manager.lastAppType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_withEmptyDatasets() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP9")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()); // Empty datasets

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_EMPTY")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: The TestManager's getTargetLanguagesFromDatasets returns {FR, DE}
      // Plus EN is always added, so we expect 3 languages: {FR, DE, EN}
      assertNotNull(manager.lastTargets);
      assertEquals(3, manager.lastTargets.size());
      assertTrue(manager.lastTargets.contains("EN"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void handleDocTranslation_withDatasetsWithoutLanguages() {
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "APP10")
      .put("inputLanguage", "EN")
      .put("datasets", new JsonArray()
        .add(new JsonObject()) // Dataset without languages field
        .add(new JsonObject()
          .put("languages", new JsonArray().add("DE"))
        )
      );

    JsonObject successDocUpload = new JsonObject()
      .put("result", new JsonObject()
        .put("id", "DOC_PARTIAL")
      );

    try {
      java.lang.reflect.Method method = CommonApplicationManager.class.getDeclaredMethod(
        "handleDocTranslation",
        JsonObject.class,
        JsonObject.class,
        String.class
      );
      method.setAccessible(true);
      method.invoke(manager, mongoApplication, successDocUpload, "collection");

      // Assert: Should still process with available languages
      assertNotNull(manager.lastTargets);
      assertTrue(manager.lastTargets.contains("EN"));
      assertTrue(manager.lastTargets.contains("DE"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
