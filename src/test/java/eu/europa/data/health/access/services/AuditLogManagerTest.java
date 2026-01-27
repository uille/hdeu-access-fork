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

import static eu.europa.data.health.access.Constants.ACTION;
import static eu.europa.data.health.access.Constants.ACTION_DESCRIPTION;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.ECAS_ID;
import static eu.europa.data.health.access.Constants.OBJECT;
import static eu.europa.data.health.access.Constants.OBJECT_ID;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.USER_ID_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@ExtendWith(MockitoExtension.class)
class AuditLogManagerTest {

  @Mock
  DBHandler dbHandler;

  @Mock
  AuthClient authClient;

  @Mock
  RoutingContext routingContext;

  @Mock
  HttpServerRequest request;

  @Mock
  HttpServerResponse response;

  AuditLogManager auditLogManager;

  @BeforeEach
  void setUp() {
    auditLogManager = Mockito.spy(new AuditLogManager(dbHandler, authClient));

    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
    lenient().when(response.end(anyString())).thenReturn(Future.succeededFuture());
  }

  private void addUserOnContext() {
    io.vertx.ext.auth.User mockUser = mock(io.vertx.ext.auth.User.class);
    when(routingContext.user()).thenReturn(mockUser);

    JsonObject principal = new JsonObject().put("preferred_username", "n3817490");
    when(mockUser.principal()).thenReturn(principal);
  }

  // -------------------------------------------------------------------------
  // prepareAuditLogActionDescription + getCurrentISOTimestamp + extractDistributionIds
  // -------------------------------------------------------------------------

  @Test
  void prepareAuditLogActionDescription_shouldReplaceCurlyPlaceholdersAndFormat() {
    String template = "Hello {} you have {} items";
    String out = auditLogManager.prepareAuditLogActionDescription(template, "John", 3);
    assertEquals("Hello John you have 3 items", out);
  }

  @Test
  void getCurrentISOTimestamp_shouldReturnIsoInstant() {
    String ts = AuditLogManager.getCurrentISOTimestamp();
    assertNotNull(ts);
    // ISO-8601 instant format ends with 'Z'
    assertTrue(ts.endsWith("Z"));
    // Rough sanity: yyyy-mm-ddThh:mm:ss...
    assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2}T.*Z", ts));
  }

  @Test
  void extractDistributionIds_whenValidJsonArray_shouldJoinIds() {
    String json = "[{\"distribution_id\":\"d1\"},{\"distribution_id\":\"d2\"}]";
    String out = auditLogManager.extractDistributionIds(json);
    assertEquals("d1, d2", out);
  }

  @Test
  void extractDistributionIds_whenInvalidJson_shouldReturnEmptyString() {
    String out = auditLogManager.extractDistributionIds("not-json");
    assertEquals("", out);
  }

  // -------------------------------------------------------------------------
  // insertAuditLog overload (legacy) - context null and non-null
  // -------------------------------------------------------------------------

  @Test
  void insertAuditLog_legacyOverload_withContext_shouldCreateEntry() {
    addUserOnContext(); // <-- ADD THIS

    auditLogManager.insertAuditLog(
      "DATASET_RECORD",
      "ACTION",
      "DESC",
      routingContext
    );

    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), any(JsonObject.class));
  }

  @Test
  void insertAuditLog_legacyOverload_withoutContext_shouldCreateEntry() {
    auditLogManager.insertAuditLog("OBJ", "ACTION_X", "desc", null);

    ArgumentCaptor<JsonObject> entryCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), entryCap.capture());

    JsonObject entry = entryCap.getValue();
    assertEquals("OBJ", entry.getString(OBJECT));
    assertEquals("ACTION_X", entry.getString(ACTION));
    assertEquals("desc", entry.getString(ACTION_DESCRIPTION));
    assertEquals("", entry.getString(USER_ID));
  }

  // -------------------------------------------------------------------------
  // insertAuditLog (objectId overload) - application objects without context
  //   - resolves USER_ID from application doc, then saves
  //   - failure path saves with empty USER_ID
  // -------------------------------------------------------------------------

  @Test
  void insertAuditLog_applicationObject_withoutContext_shouldResolveUserIdAndSave() {
    String appId = "app-1";
    JsonObject appDoc = new JsonObject().put(USER_ID_PARAM, "ecas123");

    when(dbHandler.findById(DATA_REQUEST_APPLICATION_COLLECTION, appId))
      .thenReturn(Future.succeededFuture(appDoc));

    auditLogManager.insertAuditLog(
      AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
      appId,
      "SOME_ACTION",
      "some desc",
      true,
      null
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> entryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), entryCap.capture());

      JsonObject entry = entryCap.getValue();
      assertEquals(appId, entry.getString(OBJECT_ID));
      assertEquals("ecas123", entry.getString(USER_ID));
      assertTrue(entry.getBoolean("linkable"));
    });
  }

  @Test
  void insertAuditLog_applicationObject_withoutContext_whenLookupFails_shouldSaveWithEmptyUser() {
    String appId = "app-2";

    when(dbHandler.findById(DATA_ACCESS_APPLICATION_COLLECTION, appId))
      .thenReturn(Future.failedFuture("boom"));

    auditLogManager.insertAuditLog(
      AuditLogObject.DATA_ACCESS_APPLICATION.toString(),
      appId,
      "SOME_ACTION",
      "some desc",
      true,
      null
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> entryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), entryCap.capture());

      JsonObject entry = entryCap.getValue();
      assertEquals(appId, entry.getString(OBJECT_ID));
      assertEquals("", entry.getString(USER_ID));
    });
  }

  @Test
  void insertAuditLog_nonApplicationObject_withoutContext_shouldSaveWithoutLookup() {
    auditLogManager.insertAuditLog(
      AuditLogObject.DATASET_RECORD.toString(),
      "ds-1",
      "A",
      "desc",
      true,
      null
    );

    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), any(JsonObject.class));
    verify(dbHandler, never()).findById(anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // submitDatasetAuditLog - create/update/default paths (+ updateLinkableFields in default)
  // -------------------------------------------------------------------------

  @Test
  void submitDatasetAuditLog_whenCreate_shouldInsertAuditLogAndReturnOk() {
    JsonObject body = new JsonObject()
      .put("datasetId", "ds1")
      .put("datasetAction", "create")
      .put("ncp", "AT");

    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(body);

    auditLogManager.submitDatasetAuditLog(routingContext);

    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), any(JsonObject.class));
    verify(dbHandler, never()).updateLinkableFields(anyString(), anyString());

    verify(response).setStatusCode(200);
    verify(response).end(contains("Audit Log record inserted successfully"));
  }

  @Test
  void submitDatasetAuditLog_whenUpdate_shouldInsertAuditLogAndReturnOk() {
    JsonObject body = new JsonObject()
      .put("datasetId", "ds2")
      .put("datasetAction", "update")
      .put("ncp", "DE");

    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(body);

    auditLogManager.submitDatasetAuditLog(routingContext);

    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), any(JsonObject.class));
    verify(dbHandler, never()).updateLinkableFields(anyString(), anyString());

    verify(response).setStatusCode(200);
    verify(response).end(contains("Audit Log record inserted successfully"));
  }

  @Test
  void submitDatasetAuditLog_whenDefaultDeletion_shouldUpdateLinkableFieldsAndInsertNonLinkableAuditLog() {
    JsonObject body = new JsonObject()
      .put("datasetId", "ds3")
      .put("datasetAction", "delete") // hits default
      .put("ncp", "FR");

    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(body);

    auditLogManager.submitDatasetAuditLog(routingContext);

    verify(dbHandler).updateLinkableFields(AUDIT_LOG_COLLECTION, "ds3");

    ArgumentCaptor<JsonObject> entryCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), entryCap.capture());

    assertFalse(entryCap.getValue().getBoolean("linkable"));

    verify(response).setStatusCode(200);
    verify(response).end(contains("Audit Log record inserted successfully"));
  }

  // -------------------------------------------------------------------------
  // searchAuditLogs - success + failure (query building uses authClient.getAllUsers)
  // -------------------------------------------------------------------------

  @Test
  void searchAuditLogs_whenQueryOptionsBuilt_shouldDelegateToGetAuditLogs() {
    // request params
    when(request.getParam("page")).thenReturn("2");
    when(request.getParam("limit")).thenReturn("10");
    when(request.getParam("sortBy")).thenReturn("date");
    when(request.getParam("sortOrder")).thenReturn("desc");

    // body
    JsonObject body = new JsonObject()
      .put(OBJECT_ID, "abc")
      .put(ACTION, "CREATE")
      .put(ACTION_DESCRIPTION, "hello")
      .put(ECAS_ID, "n381") // filters users
      .put("firstName", "")
      .put("lastName", "");

    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(body);

    // users from auth
    JsonArray users = new JsonArray()
      .add(new JsonObject().put("ecasId", "n3817490").put("firstName", "John").put("lastName", "Doe"))
      .add(new JsonObject().put("ecasId", "x999").put("firstName", "Alice").put("lastName", "Smith"));

    when(authClient.getAllUsers(routingContext, false))
      .thenReturn(Future.succeededFuture(users));

    // Intercept inherited getAuditLogs call
    doAnswer(inv -> null).when(auditLogManager).getAuditLogs(
      any(), anyString(), any(), any(), any(), any(), any()
    );

    auditLogManager.searchAuditLogs(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> queryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(auditLogManager).getAuditLogs(
        eq(routingContext),
        eq(AUDIT_LOG_COLLECTION),
        eq(2),
        eq(10),
        eq("date"),
        eq("desc"),
        queryCap.capture()
      );

      JsonObject q = queryCap.getValue();
      assertTrue(q.containsKey("users")); // put in queryOptions
      assertTrue(q.containsKey(USER_ID)); // derived from auth filtering
      assertTrue(q.containsKey(OBJECT_ID));
      assertTrue(q.containsKey(ACTION));
      assertTrue(q.containsKey(ACTION_DESCRIPTION));
    });
  }

  @Test
  void searchAuditLogs_whenAuthClientFails_shouldReturn500() {
    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);

    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(new JsonObject());

    when(authClient.getAllUsers(routingContext, false))
      .thenReturn(Future.failedFuture("boom"));

    auditLogManager.searchAuditLogs(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("Could not construct query options"));
      verify(auditLogManager, never()).getAuditLogs(any(), anyString(), any(), any(), any(), any(), any());
    });
  }

  @Test
  void searchAuditLogs_whenNoUserCriteriaMatch_shouldUseNoResultSentinel() {
    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);

    // criteria present but do not match any user -> USER_ID IN ["NO_RESULT"]
    JsonObject body = new JsonObject()
      .put(ECAS_ID, "nope")
      .put("firstName", "nope")
      .put("lastName", "nope");
    when(rb.asJsonObject()).thenReturn(body);

    JsonArray users = new JsonArray()
      .add(new JsonObject().put("ecasId", "aaa").put("firstName", "John").put("lastName", "Doe"));
    when(authClient.getAllUsers(routingContext, false))
      .thenReturn(Future.succeededFuture(users));

    doAnswer(inv -> null).when(auditLogManager).getAuditLogs(
      any(), anyString(), any(), any(), any(), any(), any()
    );

    auditLogManager.searchAuditLogs(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> queryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(auditLogManager).getAuditLogs(
        eq(routingContext),
        eq(AUDIT_LOG_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        queryCap.capture()
      );

      JsonObject q = queryCap.getValue();
      JsonObject userIdClause = q.getJsonObject(USER_ID);
      assertNotNull(userIdClause);
      JsonArray in = userIdClause.getJsonArray("$in");
      assertNotNull(in);
      assertEquals("NO_RESULT", in.getString(0));
    });
  }

  @Test
  void searchAuditLogs_whenNoCriteriaProvided_shouldNotForceNoResultUserId() {
    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(new JsonObject()); // no ECAS/first/last

    JsonArray users = new JsonArray()
      .add(new JsonObject().put("ecasId", "aaa").put("firstName", "John").put("lastName", "Doe"));
    when(authClient.getAllUsers(routingContext, false))
      .thenReturn(Future.succeededFuture(users));

    doAnswer(inv -> null).when(auditLogManager).getAuditLogs(
      any(), anyString(), any(), any(), any(), any(), any()
    );

    auditLogManager.searchAuditLogs(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> queryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(auditLogManager).getAuditLogs(
        eq(routingContext),
        eq(AUDIT_LOG_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        queryCap.capture()
      );

      JsonObject q = queryCap.getValue();
      // No criteria => USER_ID should be "$in" of ALL users or absent?
      // Code adds USER_ID for any users list; but does NOT add "NO_RESULT".
      JsonObject userIdClause = q.getJsonObject(USER_ID);
      assertNotNull(userIdClause);
      JsonArray in = userIdClause.getJsonArray("$in");
      assertNotNull(in);
      assertTrue(in.contains("aaa"));
      assertFalse(in.contains("NO_RESULT"));
    });
  }

  @Test
  void searchAuditLogs_whenLogDateFromOnly_shouldPutGteOnDate() {
    // body with logDateFrom only
    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);

    JsonObject body = new JsonObject()
      .put("logDateFrom", "2025-01-10"); // yyyy-MM-dd
    when(rb.asJsonObject()).thenReturn(body);

    // auth users success (required for getQueryOptions to complete)
    JsonArray users = new JsonArray()
      .add(new JsonObject().put("ecasId", "u1").put("firstName", "A").put("lastName", "B"));
    when(authClient.getAllUsers(routingContext, false))
      .thenReturn(Future.succeededFuture(users));

    // intercept inherited call
    doAnswer(inv -> null).when(auditLogManager).getAuditLogs(
      any(), anyString(), any(), any(), any(), any(), any()
    );

    auditLogManager.searchAuditLogs(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> queryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(auditLogManager).getAuditLogs(
        eq(routingContext),
        eq(AUDIT_LOG_COLLECTION),
        isNull(), isNull(), isNull(), isNull(),
        queryCap.capture()
      );

      JsonObject q = queryCap.getValue();
      assertNotNull(q);

      JsonObject date = q.getJsonObject("date");
      assertNotNull(date);
      assertEquals("2025-01-10 00:00:00", date.getString("$gte"));
      assertFalse(date.containsKey("$lte"));
    });
  }

  @Test
  void searchAuditLogs_whenLogDateFromAndTo_andObjectList_andAction_shouldBuildAllClauses() {
    RequestBody rb = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(rb);

    JsonArray objectList = new JsonArray().add("DATASET_RECORD").add("DATA_REQUEST_APPLICATION");

    JsonObject body = new JsonObject()
      .put("logDateFrom", "2025-01-10")
      .put("logDateTo", "2025-01-12")
      .put(OBJECT, objectList)
      .put(ACTION, "CREATE"); // triggers action REGEX clause
    when(rb.asJsonObject()).thenReturn(body);

    JsonArray users = new JsonArray()
      .add(new JsonObject().put("ecasId", "u1").put("firstName", "A").put("lastName", "B"));
    when(authClient.getAllUsers(routingContext, false))
      .thenReturn(Future.succeededFuture(users));

    doAnswer(inv -> null).when(auditLogManager).getAuditLogs(
      any(), anyString(), any(), any(), any(), any(), any()
    );

    auditLogManager.searchAuditLogs(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ArgumentCaptor<JsonObject> queryCap = ArgumentCaptor.forClass(JsonObject.class);
      verify(auditLogManager).getAuditLogs(
        eq(routingContext),
        eq(AUDIT_LOG_COLLECTION),
        isNull(), isNull(), isNull(), isNull(),
        queryCap.capture()
      );

      JsonObject q = queryCap.getValue();
      assertNotNull(q);

      // Date clause merges $gte and $lte
      JsonObject date = q.getJsonObject("date");
      assertNotNull(date);
      assertEquals("2025-01-10 00:00:00", date.getString("$gte"));
      assertEquals("2025-01-12 23:59:59", date.getString("$lte"));

      // object list clause
      JsonObject objClause = q.getJsonObject(OBJECT);
      assertNotNull(objClause);
      assertEquals(objectList, objClause.getJsonArray("$in"));

      // action regex clause
      JsonObject actionClause = q.getJsonObject(ACTION);
      assertNotNull(actionClause);
      assertEquals("CREATE", actionClause.getString("regex")); // REGEX constant value is "regex"
      assertEquals("i", actionClause.getString("options"));    // OPTIONS constant value is "options"
    });
  }

}
