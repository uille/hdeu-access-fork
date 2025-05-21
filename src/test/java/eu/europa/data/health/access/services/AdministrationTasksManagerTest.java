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
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.LIMIT;
import static eu.europa.data.health.access.Constants.PAGE;
import static eu.europa.data.health.access.Constants.SORT_BY;
import static eu.europa.data.health.access.Constants.SORT_ORDER;
import static eu.europa.data.health.access.Constants.STATUS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;


public class AdministrationTasksManagerTest {

  @InjectMocks
  private AdministrationTasksManager manager;
  private MongoClient mongoClient;
  private DBHandler dbHandler;
  private RoutingContext routingContext;
  private HttpServerResponse response;
  private HttpServerRequest request;

  @BeforeEach
  void setUp() {
    mongoClient = mock(MongoClient.class);
    dbHandler = mock(DBHandler.class);
    manager = new AdministrationTasksManager(dbHandler, mongoClient);

    routingContext = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    request = mock(HttpServerRequest.class);

    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
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

    JsonObject record = new JsonObject()
      .put("_id", dispatcherId)
      .put("configuration", new JsonObject().put("status", "FAILED"));

    when(routingContext.pathParam("applicationId")).thenReturn(appId);
    when(mongoClient.findOne(anyString(), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(record));

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      Throwable updateError = new RuntimeException("Update failed");
      handler.handle(Future.failedFuture(updateError));
      return null;
    }).when(mongoClient).updateCollection(
      anyString(), any(JsonObject.class), any(JsonObject.class), any());

    manager.retrySubmitAction(routingContext);

    verify(response).setStatusCode(eq(500));
    verify(response).end(contains("Internal server error occurred with message:Update failed"));
  }

  @Test
  void testRetrySubmitAction_updateSuccess() {
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app-123");
    JsonObject record = new JsonObject().put("_id", "12345")
      .put("configuration", new JsonObject().put("status", "FAILED"));

    when(mongoClient.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(record));

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

}
