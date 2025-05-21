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

import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

public class AuditLogManagerTest {

  @InjectMocks
  private AuditLogManager auditLogManager;
  private  DBHandler dbHandler;
  private  AuthClient authClient;
  private RoutingContext routingContext;
  private HttpServerResponse response;
  private HttpServerRequest request;

  @BeforeEach
  void setUp() {
    authClient = mock(AuthClient.class);
    dbHandler = mock(DBHandler.class);
    auditLogManager = new AuditLogManager(dbHandler, authClient);

    routingContext = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    request = mock(HttpServerRequest.class);

    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);
    User mockUser = Mockito.mock(User.class);
    String ecasId = "n3817490";
    Mockito.when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    Mockito.when(routingContext.user().principal()).thenReturn(principal);
    Mockito.when(mockUser.get("preferred_username")).thenReturn(ecasId);
  }

  @Test
  void testInsertAuditLog_createsAuditLogEntryAndSavesIt() {
    String object = "TestObject";
    String objectId = "12345";
    String action = "CREATE";
    String description = "Created test object";
    boolean linkable = true;

    auditLogManager.insertAuditLog(object, objectId, action, description, linkable, routingContext);

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).create(eq(AUDIT_LOG_COLLECTION), captor.capture());

    JsonObject capturedLog = captor.getValue();

    assertEquals(object, capturedLog.getString("object"));
    assertEquals(objectId, capturedLog.getString("objectId"));
    assertEquals(action, capturedLog.getString("action"));
    assertEquals(description, capturedLog.getString("actionDescription"));
    assertEquals(linkable, capturedLog.getBoolean("linkable"));
    assertNotNull(capturedLog.getString("date"));
    assertNotNull(capturedLog.getString("userId"));
  }

  @Test
  void testExtractDistributionIds_validJson() {
    String jsonArray = "[{\"distribution_id\": \"dist1\"}, {\"distribution_id\": \"dist2\"}]";
    String result = auditLogManager.extractDistributionIds(jsonArray);
    assertEquals("dist1, dist2", result);
  }

  @Test
  void testExtractDistributionIds_emptyArray() {
    String jsonArray = "[]";
    String result = auditLogManager.extractDistributionIds(jsonArray);
    assertEquals("", result);
  }

  @Test
  void testExtractDistributionIds_missingField() {
    String jsonArray = "[{\"no_distribution_id\": \"x\"}]";
    String result = auditLogManager.extractDistributionIds(jsonArray);
    assertEquals("", result);
  }

  @Test
  void testExtractDistributionIds_invalidJson() {
    String jsonArray = "[{\"distribution_id\": \"dist1\", {\"distribution_id\": \"dist2\"}]";
    String result = auditLogManager.extractDistributionIds(jsonArray);
    assertEquals("", result);
  }

  @Test
  void testSubmitDatasetAuditLog_createAction() {
    JsonObject input = new JsonObject()
      .put("datasetId", "ds1")
      .put("datasetAction", "create")
      .put("ncp", "NCP-X");

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(input);

    AuditLogManager spyManager = Mockito.spy(auditLogManager);

    doNothing().when(spyManager).setUpMessageContextResponse(any(), any(), any());
    doReturn("Dataset Record with id: ds1 was created by NCP: NCP-X")
      .when(spyManager).prepareAuditLogActionDescription(any(), any(), any());

    doCallRealMethod().when(spyManager)
      .insertAuditLog(any(), any(), any(), any(), anyBoolean(), isNull());

    spyManager.submitDatasetAuditLog(routingContext);

    ArgumentCaptor<String> actionDescriptionCaptor = ArgumentCaptor.forClass(String.class);

    verify(spyManager).insertAuditLog(
      eq("Dataset Record"),
      eq("ds1"),
      eq("Dataset Record creation"),
      actionDescriptionCaptor.capture(),
      eq(true),
      isNull()
    );

    assertEquals("Dataset Record with id: ds1 was created by NCP: NCP-X",
      actionDescriptionCaptor.getValue()
    );

    verify(spyManager).setUpMessageContextResponse(
      eq(routingContext),
      eq(HttpResponseStatus.OK),
      eq("Audit Log record inserted successfully")
    );
  }


  @Test
  void testSubmitDatasetAuditLog_deleteAction() {
    JsonObject input = new JsonObject()
      .put("datasetId", "ds42")
      .put("datasetAction", "delete")
      .put("ncp", "NCP-DEL");

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(input);

    AuditLogManager spyManager = Mockito.spy(auditLogManager);

    doNothing().when(spyManager).insertAuditLog(any(), any(), any(), any(), anyBoolean(), isNull());
    doNothing().when(spyManager).setUpMessageContextResponse(any(), any(), any());
    when(dbHandler.updateLinkableFields(any(), any())).thenReturn(null); // FIX HERE

    spyManager.submitDatasetAuditLog(routingContext);

    verify(dbHandler).updateLinkableFields(eq(AUDIT_LOG_COLLECTION), eq("ds42"));

    verify(spyManager).insertAuditLog(
      eq("Dataset Record"),
      eq("ds42"),
      eq("Dataset Record deletion"),
      argThat(s -> s.contains("ds42") && s.contains("NCP-DEL")),
      eq(false),
      isNull()
    );
  }

  @Test
  void testGetQueryOptions_withSearchFieldsAndMatchingUsers() {
    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(ctx.request()).thenReturn(request);
    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);
    when(request.getParam("sortBy")).thenReturn(null);
    when(request.getParam("sortOrder")).thenReturn(null);

    JsonObject searchBody = new JsonObject()
      .put("objectId", "abc123")
      .put("logDateFrom", "2023-01-01")
      .put("logDateTo", "2023-12-31")
      .put("object", new JsonArray().add("Dataset"))
      .put("action", "delete")
      .put("actionDescription", "removed")
      .put("ecasId", "john.doe")
      .put("firstName", "John")
      .put("lastName", "Doe");

    when(ctx.body()).thenReturn(mock(RequestBody.class));
    when(ctx.body().asJsonObject()).thenReturn(searchBody);

    JsonArray users = new JsonArray().add(new JsonObject()
      .put("ecasId", "john.doe")
      .put("firstName", "John")
      .put("lastName", "Doe"));

    doAnswer(invocation -> {
      Promise<JsonArray> p = Promise.promise();
      p.complete(users);
      return p.future();
    }).when(authClient).getAllUsers(eq(ctx));

    AuditLogManager spyManager = Mockito.spy(auditLogManager);
    doNothing().when(spyManager).getAuditLogs(any(), any(), any(), any(), any(), any());

    spyManager.searchAuditLogs(ctx);

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);

    verify(spyManager).getAuditLogs(
      eq(ctx),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      queryCaptor.capture()
    );

    JsonObject capturedQuery = queryCaptor.getValue();

    assertNotNull(capturedQuery);
    assertTrue(capturedQuery.containsKey("objectId"));
    assertTrue(capturedQuery.containsKey("date"));
    assertTrue(capturedQuery.containsKey("object"));
    assertTrue(capturedQuery.containsKey("action"));
    assertTrue(capturedQuery.containsKey("actionDescription"));
    assertTrue(capturedQuery.containsKey("userId"));
    assertEquals("john.doe", capturedQuery.getJsonObject("userId").getJsonArray("$in").getString(0));
  }

  @Test
  void testGetQueryOptions_whenAuthClientFails() {
    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(ctx.request()).thenReturn(request);
    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);
    when(request.getParam("sortBy")).thenReturn(null);
    when(request.getParam("sortOrder")).thenReturn(null);

    JsonObject searchBody = new JsonObject().put("ecasId", "fail");

    when(ctx.body()).thenReturn(mock(RequestBody.class));
    when(ctx.body().asJsonObject()).thenReturn(searchBody);

    doAnswer(invocation -> {
      Promise<JsonArray> p = Promise.promise();
      p.fail("User service down");
      return p.future();
    }).when(authClient).getAllUsers(eq(ctx));

    AuditLogManager spyManager = Mockito.spy(auditLogManager);

    doNothing().when(spyManager).setUpMessageContextResponse(
      eq(ctx),
      eq(HttpResponseStatus.INTERNAL_SERVER_ERROR),
      eq("Could not construct query options")
    );

    spyManager.searchAuditLogs(ctx);

    verify(spyManager).setUpMessageContextResponse(
      eq(ctx),
      eq(HttpResponseStatus.INTERNAL_SERVER_ERROR),
      eq("Could not construct query options")
    );
  }


}
