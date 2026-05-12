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

import static eu.europa.data.health.access.Constants.APPLICATION_COLLABORATOR_COLLECTION;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE_FIELD;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.LIMIT;
import static eu.europa.data.health.access.Constants.OWNERSHIP_REQUEST_COLLECTION;
import static eu.europa.data.health.access.Constants.PAGE;
import static eu.europa.data.health.access.Constants.SORT_BY;
import static eu.europa.data.health.access.Constants.SORT_ORDER;
import static eu.europa.data.health.access.Constants.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

class AdministrationTasksManagerTest {

  @InjectMocks
  private AdministrationTasksManager manager;
  private MongoClient mongoClient;
  private DBHandler dbHandler;
  private RoutingContext routingContext;
  private HttpServerResponse response;
  private HttpServerRequest request;
  private RequestBody requestBody;

  @BeforeEach
  void setUp() {
    mongoClient = mock(MongoClient.class);
    dbHandler = mock(DBHandler.class);
    manager = new AdministrationTasksManager(dbHandler, mongoClient);

    routingContext = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    request = mock(HttpServerRequest.class);
    requestBody = mock(RequestBody.class);

    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);
  }

  @Test
  void testRetrySubmitAction_dispatcherRecordNotFound() {
    when(routingContext.pathParam("applicationId")).thenReturn("app-123");
    when(mongoClient.findOne(anyString(), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.retrySubmitAction(routingContext);
    verify(routingContext).response();
  }

  @Test
  void testRetrySubmitAction_statusNotFailed() {
    when(routingContext.pathParam("applicationId")).thenReturn("app-123");
    JsonObject record = new JsonObject().put("_id", "12345")
      .put("configuration", new JsonObject().put("status", "COMPLETED"));

    when(mongoClient.findOne(anyString(), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(record));

    manager.retrySubmitAction(routingContext);
    verify(routingContext).response();
  }

  @Test
  void testRetrySubmitAction_updateCollectionFails() {
    String appId = "app-123";
    String dispatcherId = "dispatcher-456";

    JsonObject jsonObject = new JsonObject()
      .put("_id", dispatcherId)
      .put("configuration", new JsonObject().put("status", "FAILED"));

    when(routingContext.pathParam("applicationId")).thenReturn(appId);
    when(mongoClient.findOne(anyString(), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(jsonObject));

    HttpServerResponse httpResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(httpResponse);
    when(httpResponse.putHeader(anyString(), anyString())).thenReturn(httpResponse);
    when(httpResponse.setStatusCode(anyInt())).thenReturn(httpResponse);

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      Throwable updateError = new RuntimeException("Update failed");
      handler.handle(Future.failedFuture(updateError));
      return null;
    }).when(mongoClient).updateCollection(
      anyString(), any(JsonObject.class), any(JsonObject.class), any());

    manager.retrySubmitAction(routingContext);

    verify(httpResponse).setStatusCode(eq(500));
    verify(httpResponse).end(contains("Internal server error occurred with message:Update failed"));
  }

  @Test
  void testRetrySubmitAction_updateSuccess() {
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app-123");
    JsonObject rec = new JsonObject().put("_id", "12345")
      .put("configuration", new JsonObject().put("status", "FAILED"));

    when(mongoClient.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(rec));

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
      return null;
    }).when(mongoClient)
      .updateCollection(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any(JsonObject.class),
        any());

    manager.retrySubmitAction(routingContext);

    verify(response).setStatusCode(200);
    verify(response).end(contains("has been resubmitted"));
    verify(routingContext).response();
  }

  @Test
  void testGetAllDataRequestApplications_withDefaults() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(request.getParam(SORT_BY)).thenReturn(null);
    when(request.getParam(SORT_ORDER)).thenReturn(null);
    when(request.getParam(STATUS)).thenReturn(null);

    List<JsonObject> mockResultList = new ArrayList<>();

    when(dbHandler.findAll(
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      anyString(),
      anyString(),
      isNull()
    )).thenReturn(Future.succeededFuture(mockResultList));

    when(dbHandler.countAllDocuments(eq(DATA_REQUEST_APPLICATION_COLLECTION), isNull()))
      .thenReturn(Future.succeededFuture(10));
    when(dbHandler.countAllDocuments(eq(DATA_REQUEST_APPLICATION_COLLECTION), eq("PENDING")))
      .thenReturn(Future.succeededFuture(5));

    manager.getAllDataRequestApplications(routingContext);
  }

  @Test
  void testGetAllDataAccessApplications_withPagination() {
    when(request.getParam(PAGE)).thenReturn("1");
    when(request.getParam(LIMIT)).thenReturn("1");
    when(request.getParam(SORT_BY)).thenReturn("status");
    when(request.getParam(SORT_ORDER)).thenReturn("asc");
    when(request.getParam(STATUS)).thenReturn("PENDING");


    List<JsonObject> mockResultList = new ArrayList<>();

    when(dbHandler.findAll(
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      eq(1),
      eq(1),
      eq("status"),
      eq("asc"),
      eq("PENDING")
    )).thenReturn(Future.succeededFuture(mockResultList));

    when(dbHandler.countAllDocuments(eq(DATA_REQUEST_APPLICATION_COLLECTION), isNull()))
      .thenReturn(Future.succeededFuture(10));
    when(dbHandler.countAllDocuments(eq(DATA_REQUEST_APPLICATION_COLLECTION), eq("PENDING")))
      .thenReturn(Future.succeededFuture(5));

    manager.getAllDataRequestApplications(routingContext);
  }

  @Test
  void testGetAllDataAccessApplications_withQueryParams() {
    when(request.getParam(PAGE)).thenReturn("1");
    when(request.getParam(LIMIT)).thenReturn("10");
    when(request.getParam(SORT_BY)).thenReturn("status");
    when(request.getParam(SORT_ORDER)).thenReturn("asc");
    when(request.getParam(STATUS)).thenReturn("APPROVED");

    List<JsonObject> mockResultList = new ArrayList<>();

    when(dbHandler.findAll(
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      eq(1),
      eq(10),
      eq("status"),
      eq("asc"),
      eq("APPROVED")
    )).thenReturn(Future.succeededFuture(mockResultList));

    when(dbHandler.countAllDocuments(eq(DATA_ACCESS_APPLICATION_COLLECTION), isNull()))
      .thenReturn(Future.succeededFuture(10));
    when(dbHandler.countAllDocuments(eq(DATA_ACCESS_APPLICATION_COLLECTION), eq("APPROVED")))
      .thenReturn(Future.succeededFuture(5));

    manager.getAllDataAccessApplications(routingContext);

  }

  @Test
  void testSearchAllApplications_withPaginationAndSearchTerm() {
    when(request.getParam(PAGE)).thenReturn("1");
    when(request.getParam(LIMIT)).thenReturn("2");
    when(request.getParam(SORT_BY)).thenReturn("title");
    when(request.getParam(SORT_ORDER)).thenReturn("asc");
    when(request.getParam(STATUS)).thenReturn("PENDING");

    JsonObject searchPayload = new JsonObject()
      .put("searchTerm", "beta");
    when(requestBody.asJsonObject()).thenReturn(searchPayload);

    List<JsonObject> accessApplications = List.of(
      new JsonObject().put("_id", "da-1").put("title", "Beta Access")
        .put("date_created", "2025-01-02")
        .put("status", new JsonObject().put("status", "PENDING"))
    );
    List<JsonObject> requestApplications = List.of(
      new JsonObject().put("_id", "dr-1").put("title", "Beta Request")
        .put("date_created", "2025-01-01")
        .put("status", new JsonObject().put("status", "PENDING"))
    );

    when(dbHandler.findEntities(eq(DATA_ACCESS_APPLICATION_COLLECTION), isNull(), isNull(), isNull(),
      isNull(), any(JsonObject.class), any(JsonObject.class))).thenReturn(Future.succeededFuture(accessApplications));
    when(dbHandler.findEntities(eq(DATA_REQUEST_APPLICATION_COLLECTION), isNull(), isNull(), isNull(),
      isNull(), any(JsonObject.class), any(JsonObject.class))).thenReturn(Future.succeededFuture(requestApplications));

    when(dbHandler.countAll(eq(DATA_ACCESS_APPLICATION_COLLECTION),
      argThat(query -> query != null && query.isEmpty()))).thenReturn(Future.succeededFuture(4));
    when(dbHandler.countAll(eq(DATA_REQUEST_APPLICATION_COLLECTION),
      argThat(query -> query != null && query.isEmpty()))).thenReturn(Future.succeededFuture(6));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(
        new JsonObject().put(APPLICATION_ID, "da-1").put(APPLICATION_TYPE_FIELD,
          DATA_ACCESS_APPLICATION_COLLECTION)
      )));
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(eq(DATA_ACCESS_APPLICATION_COLLECTION), isNull(), isNull(),
      isNull(), isNull(), queryCaptor.capture(), any(JsonObject.class));

    JsonObject query = queryCaptor.getValue();
    assertNull(query.getJsonArray("$or"));
    assertEquals("beta", query.getJsonObject("title").getString("$regex"));
    assertEquals("i", query.getJsonObject("title").getString("$options"));
    assertEquals("PENDING", query.getJsonObject("status.status").getString("$eq"));

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(responseCaptor.capture());

    JsonObject responseJson = new JsonObject(responseCaptor.getValue());
    JsonArray data = responseJson.getJsonArray("data");
    JsonObject metadata = responseJson.getJsonObject("metadata");

    assertEquals(2, data.size());
    assertEquals("DATA_ACCESS", data.getJsonObject(0).getString("applicationType"));
    assertEquals(true, data.getJsonObject(0).getBoolean("hasOwnershipRequests"));
    assertEquals("DATA_REQUEST", data.getJsonObject(1).getString("applicationType"));
    assertEquals(false, data.getJsonObject(1).getBoolean("hasOwnershipRequests"));
    assertEquals(10, metadata.getInteger("total_documents"));
    assertEquals(2, metadata.getInteger("total_documents_filtered"));
    assertEquals("beta", metadata.getString("searchTerm"));
    assertEquals("all", metadata.getString("ownershipFilter"));
    assertEquals("title", metadata.getString("sort_by"));
    assertEquals("asc", metadata.getString("sort_order"));
    assertEquals(1, metadata.getInteger("current_page"));
    assertEquals(2, metadata.getInteger("page_size"));
    assertEquals(1, metadata.getInteger("total_pages"));
    assertEquals("PENDING", metadata.getString("status"));
  }

  @Test
  void testSearchAllApplications_withPaginationAndOwnershipFilter() {
    when(request.getParam(PAGE)).thenReturn("1");
    when(request.getParam(LIMIT)).thenReturn("2");
    when(request.getParam(SORT_BY)).thenReturn("title");
    when(request.getParam(SORT_ORDER)).thenReturn("asc");
    when(request.getParam(STATUS)).thenReturn("PENDING");

    JsonObject searchPayload = new JsonObject()
      .put("searchTerm", "beta")
      .put("ownershipFilter", "pending");
    when(requestBody.asJsonObject()).thenReturn(searchPayload);

    List<JsonObject> accessApplications = List.of(
      new JsonObject().put("_id", "da-1").put("title", "Beta Access")
        .put("date_created", "2025-01-02")
        .put("status", new JsonObject().put("status", "PENDING"))
    );
    List<JsonObject> requestApplications = List.of(
      new JsonObject().put("_id", "dr-1").put("title", "Beta Request")
        .put("date_created", "2025-01-01")
        .put("status", new JsonObject().put("status", "PENDING"))
    );

    when(dbHandler.findEntities(eq(DATA_ACCESS_APPLICATION_COLLECTION), isNull(), isNull(), isNull(),
      isNull(), any(JsonObject.class), any(JsonObject.class))).thenReturn(Future.succeededFuture(accessApplications));
    when(dbHandler.findEntities(eq(DATA_REQUEST_APPLICATION_COLLECTION), isNull(), isNull(), isNull(),
      isNull(), any(JsonObject.class), any(JsonObject.class))).thenReturn(Future.succeededFuture(requestApplications));

    when(dbHandler.countAll(eq(DATA_ACCESS_APPLICATION_COLLECTION),
      argThat(query -> query != null && query.isEmpty()))).thenReturn(Future.succeededFuture(4));
    when(dbHandler.countAll(eq(DATA_REQUEST_APPLICATION_COLLECTION),
      argThat(query -> query != null && query.isEmpty()))).thenReturn(Future.succeededFuture(6));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(
        new JsonObject().put(APPLICATION_ID, "da-1").put(APPLICATION_TYPE_FIELD,
          DATA_ACCESS_APPLICATION_COLLECTION),
        new JsonObject().put(APPLICATION_ID, "dr-1").put(APPLICATION_TYPE_FIELD,
          DATA_REQUEST_APPLICATION_COLLECTION)
      )));
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(eq(DATA_ACCESS_APPLICATION_COLLECTION), isNull(), isNull(),
      isNull(), isNull(), queryCaptor.capture(), any(JsonObject.class));

    JsonObject query = queryCaptor.getValue();
    assertNull(query.getJsonArray("$or"));
    assertEquals("beta", query.getJsonObject("title").getString("$regex"));
    assertEquals("i", query.getJsonObject("title").getString("$options"));
    assertEquals("PENDING", query.getJsonObject("status.status").getString("$eq"));

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(responseCaptor.capture());

    JsonObject responseJson = new JsonObject(responseCaptor.getValue());
    JsonArray data = responseJson.getJsonArray("data");
    JsonObject metadata = responseJson.getJsonObject("metadata");

    assertEquals(2, data.size());
    assertEquals("DATA_ACCESS", data.getJsonObject(0).getString("applicationType"));
    assertEquals(true, data.getJsonObject(0).getBoolean("hasOwnershipRequests"));
    assertEquals("DATA_REQUEST", data.getJsonObject(1).getString("applicationType"));
    assertEquals(true, data.getJsonObject(1).getBoolean("hasOwnershipRequests"));
    assertEquals(10, metadata.getInteger("total_documents"));
    assertEquals(2, metadata.getInteger("total_documents_filtered"));
    assertEquals("beta", metadata.getString("searchTerm"));
    assertEquals("pending", metadata.getString("ownershipFilter"));
    assertEquals("title", metadata.getString("sort_by"));
    assertEquals("asc", metadata.getString("sort_order"));
    assertEquals(1, metadata.getInteger("current_page"));
    assertEquals(2, metadata.getInteger("page_size"));
    assertEquals(1, metadata.getInteger("total_pages"));
    assertEquals("PENDING", metadata.getString("status"));
  }

  @Test
  void testSearchAllApplications_withNoPendingOwnershipFilter() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(request.getParam(SORT_BY)).thenReturn(null);
    when(request.getParam(SORT_ORDER)).thenReturn(null);
    when(request.getParam(STATUS)).thenReturn(null);

    JsonObject searchPayload = new JsonObject()
      .put("title", "request")
      .put("ownershipFilter", "no-pending");
    when(requestBody.asJsonObject()).thenReturn(searchPayload);

    List<JsonObject> accessApplications = List.of(
      new JsonObject().put("_id", "da-1").put("title", "Request Access")
        .put("date_created", "2025-02-02")
        .put("status", new JsonObject().put("status", "APPROVED"))
    );
    List<JsonObject> requestApplications = List.of(
      new JsonObject().put("_id", "dr-1").put("title", "Request Application")
        .put("date_created", "2025-02-01")
        .put("status", new JsonObject().put("status", "APPROVED"))
    );

    when(dbHandler.findEntities(eq(DATA_ACCESS_APPLICATION_COLLECTION), isNull(), isNull(), isNull(),
      isNull(), any(JsonObject.class), any(JsonObject.class))).thenReturn(Future.succeededFuture(accessApplications));
    when(dbHandler.findEntities(eq(DATA_REQUEST_APPLICATION_COLLECTION), isNull(), isNull(), isNull(),
      isNull(), any(JsonObject.class), any(JsonObject.class))).thenReturn(Future.succeededFuture(requestApplications));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(
        new JsonObject().put(APPLICATION_ID, "da-1").put(APPLICATION_TYPE_FIELD,
          DATA_ACCESS_APPLICATION_COLLECTION)
      )));
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> requestQueryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      requestQueryCaptor.capture(),
      any(JsonObject.class)
    );
    // searchTerm "request" matches DATA_REQUEST_APPLICATION collection name -> no title/_id filter
    assertNull(requestQueryCaptor.getValue().getJsonArray("$or"));

    ArgumentCaptor<JsonObject> accessQueryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      accessQueryCaptor.capture(),
      any(JsonObject.class)
    );
    // searchTerm "request" does not match DATA_ACCESS_APPLICATION -> title-only filter (>3 chars)
    JsonObject accessQuery = accessQueryCaptor.getValue();
    assertNull(accessQuery.getJsonArray("$or"));
    assertEquals("request", accessQuery.getJsonObject("title").getString("$regex"));
    assertEquals("i", accessQuery.getJsonObject("title").getString("$options"));

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(responseCaptor.capture());

    JsonObject responseJson = new JsonObject(responseCaptor.getValue());
    JsonArray data = responseJson.getJsonArray("data");
    JsonObject metadata = responseJson.getJsonObject("metadata");

    assertEquals(1, data.size());
    assertEquals("DATA_REQUEST", data.getJsonObject(0).getString("applicationType"));
    assertEquals(false, data.getJsonObject(0).getBoolean("hasOwnershipRequests"));
    assertEquals(false, data.getJsonObject(0).getBoolean("hasCollaborators"));
    assertEquals("request", metadata.getString("searchTerm"));
    assertEquals("no-pending", metadata.getString("ownershipFilter"));
    verify(dbHandler, never()).countAll(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class));
    verify(dbHandler, never()).countAll(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class));
  }

  @Test
  void testSearchAllApplications_withInvalidOwnershipFilterReturnsBadRequest() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject().put("ownershipFilter", "invalid"));

    manager.searchAllApplications(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end(contains("ownershipFilter must be one of"));
    verify(dbHandler, never()).findEntities(anyString(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSearchAllApplications_withInvalidPaginationReturnsBadRequest() {
    when(request.getParam(PAGE)).thenReturn("1");
    when(request.getParam(LIMIT)).thenReturn(null);

    manager.searchAllApplications(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end(contains("Both page and limit must be positive integers"));
    verify(dbHandler, never()).findEntities(anyString(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSearchAllApplications_searchByReferenceNumber() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(request.getParam(SORT_BY)).thenReturn(null);
    when(request.getParam(SORT_ORDER)).thenReturn(null);
    when(request.getParam(STATUS)).thenReturn(null);

    JsonObject searchPayload = new JsonObject().put("searchTerm", "abc");
    when(requestBody.asJsonObject()).thenReturn(searchPayload);

    when(
      dbHandler.findEntities(
        eq(DATA_ACCESS_APPLICATION_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));
    when(
      dbHandler.findEntities(
        eq(DATA_REQUEST_APPLICATION_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(new ArrayList<>())
    );
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      queryCaptor.capture(),
      any(JsonObject.class)
    );

    JsonArray orFilters = queryCaptor.getValue().getJsonArray("$or");
    assertEquals("abc", orFilters.getJsonObject(0).getJsonObject("title").getString("$regex"));
    // reference-number match: regex anchored at end of _id
    assertEquals("abc$", orFilters.getJsonObject(1).getJsonObject("_id").getString("$regex"));
    assertEquals("i", orFilters.getJsonObject(1).getJsonObject("_id").getString("$options"));
  }

  @Test
  void testSearchAllApplications_searchTermWithRegexMetacharactersIsEscaped() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(request.getParam(STATUS)).thenReturn(null);

    JsonObject searchPayload = new JsonObject().put("searchTerm", "a.b*c");
    when(requestBody.asJsonObject()).thenReturn(searchPayload);

    when(
      dbHandler.findEntities(
        anyString(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(new ArrayList<>())
    );
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      queryCaptor.capture(),
      any(JsonObject.class)
    );
    // "a.b*c" is 5 chars > 3 -> title-only filter, no _id suffix
    JsonObject query = queryCaptor.getValue();
    assertNull(query.getJsonArray("$or"));
    assertEquals("a\\.b\\*c", query.getJsonObject("title").getString("$regex"));
    assertEquals("i", query.getJsonObject("title").getString("$options"));
  }

  @Test
  void testSearchAllApplications_searchMatchesCollectionName() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(request.getParam(STATUS)).thenReturn(null);

    // user types the application type label as it appears in the UI (with a space).
    JsonObject searchPayload = new JsonObject().put("searchTerm", "Data Access");
    when(requestBody.asJsonObject()).thenReturn(searchPayload);

    List<JsonObject> accessApplications = List.of(
      new JsonObject()
        .put("_id", "da-1")
        .put("title", "Anything")
        .put("date_created", "2025-03-01")
        .put("status", new JsonObject().put("status", "DRAFT"))
    );
    when(
      dbHandler.findEntities(
        eq(DATA_ACCESS_APPLICATION_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(accessApplications));
    when(
      dbHandler.findEntities(
        eq(DATA_REQUEST_APPLICATION_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(new ArrayList<>())
    );
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(new ArrayList<>()));

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> accessQueryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      accessQueryCaptor.capture(),
      any(JsonObject.class)
    );
    // "Data Access" -> "DATA_ACCESS" is contained in DATA_ACCESS_APPLICATION -> no $or filter for this collection
    assertNull(accessQueryCaptor.getValue().getJsonArray("$or"));

    ArgumentCaptor<JsonObject> requestQueryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).findEntities(
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      requestQueryCaptor.capture(),
      any(JsonObject.class)
    );
    // The OTHER collection filters by title only ("Data Access" is 10 chars > 3, no _id suffix)
    JsonObject requestQuery = requestQueryCaptor.getValue();
    assertNull(requestQuery.getJsonArray("$or"));
    assertEquals("Data Access", requestQuery.getJsonObject("title").getString("$regex"));
    assertEquals("i", requestQuery.getJsonObject("title").getString("$options"));

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(responseCaptor.capture());
    JsonArray data = new JsonObject(responseCaptor.getValue()).getJsonArray("data");
    assertEquals(1, data.size());
    assertEquals("DATA_ACCESS", data.getJsonObject(0).getString("applicationType"));
  }

  @Test
  void testSearchAllApplications_enrichesHasCollaborators() {
    when(request.getParam(PAGE)).thenReturn(null);
    when(request.getParam(LIMIT)).thenReturn(null);
    when(request.getParam(STATUS)).thenReturn(null);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());

    List<JsonObject> accessApplications = List.of(
      new JsonObject()
        .put("_id", "da-1")
        .put("title", "With collaborators")
        .put("date_created", "2025-04-02")
        .put("status", new JsonObject().put("status", "DRAFT"))
    );
    List<JsonObject> requestApplications = List.of(
      new JsonObject()
        .put("_id", "dr-1")
        .put("title", "Without collaborators")
        .put("date_created", "2025-04-01")
        .put("status", new JsonObject().put("status", "DRAFT"))
    );
    when(
      dbHandler.findEntities(
        eq(DATA_ACCESS_APPLICATION_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(accessApplications));
    when(
      dbHandler.findEntities(
        eq(DATA_REQUEST_APPLICATION_COLLECTION),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(JsonObject.class),
        any(JsonObject.class)
      )
    ).thenReturn(Future.succeededFuture(requestApplications));
    when(mongoClient.find(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(new ArrayList<>())
    );
    when(
      mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(
      Future.succeededFuture(
        List.of(
          new JsonObject()
            .put(APPLICATION_ID, "da-1")
            .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION)
            .put("role", "EDITOR")
        )
      )
    );

    manager.searchAllApplications(routingContext);

    ArgumentCaptor<JsonObject> collaboratorQueryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).find(
      eq(APPLICATION_COLLABORATOR_COLLECTION),
      collaboratorQueryCaptor.capture()
    );
    JsonObject collaboratorQuery = collaboratorQueryCaptor.getValue();
    assertEquals("OWNER", collaboratorQuery.getJsonObject("role").getString("$ne"));
    assertEquals(2, collaboratorQuery.getJsonObject(APPLICATION_ID).getJsonArray("$in").size());

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(responseCaptor.capture());
    JsonArray data = new JsonObject(responseCaptor.getValue()).getJsonArray("data");
    assertEquals(2, data.size());
    JsonObject accessApp = data.getJsonObject(0).getString("_id").equals("da-1")
      ? data.getJsonObject(0)
      : data.getJsonObject(1);
    JsonObject requestApp = data.getJsonObject(0).getString("_id").equals("dr-1")
      ? data.getJsonObject(0)
      : data.getJsonObject(1);
    assertEquals(true, accessApp.getBoolean("hasCollaborators"));
    assertEquals(false, requestApp.getBoolean("hasCollaborators"));
  }
}
