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

import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;

class DispatcherManagerTest {

  @Mock
  MongoClient mongoClient;
  @Mock
  DBHandler dbHandler;
  @Mock
  RoutingContext routingContext;
  @Mock
  private HttpServerResponse response;
  @Mock
  private HttpServerRequest request;

  @InjectMocks
  DispatcherManager dispatcherManager;
  ch.qos.logback.classic.Logger logger = (Logger) LoggerFactory.getLogger(DispatcherManager.class);

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    dispatcherManager = new DispatcherManager(mongoClient, dbHandler);
    // Mock other behaviors
    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
  }

  @Test
  void testUpdateDatasetStatusFromDispatcher_success() {
    JsonObject payload = new JsonObject()
      .put("catalogId", "CAT123")
      .put("applicationType", "collectionName")
      .put("decisions", new JsonArray().add(new JsonObject()
        .put("publisher", "Pub1").put("decision", "APPROVED")));

    JsonObject contextBody = new JsonObject()
      .put("objectId", "app1")
      .put("payload", payload);

    when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(contextBody);

    JsonObject dataset = new JsonObject()
      .put("catalog_id", "CAT123")
      .put("dispatcher_response_status", "domibus_national_received")
      .put("publisher", new JsonObject().put("name", "Pub1"));

    JsonObject application = new JsonObject()
      .put("status", new JsonObject()
        .put("status", "SUBMITTED")
        .put("date", "2024-04-01T10:00:00Z")
        .put("userId", "test"))
      .put("datasets", new JsonArray().add(dataset));

    when(mongoClient.findOne(anyString(), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(application));
        return null;
      });

    when(mongoClient.findOneAndReplace(anyString(), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(application));
        return null;
      });

    dispatcherManager.updateDatasetStatusFromDispatcher(routingContext);
  }

  @Test
  void testUpdateApplicationStatusFromDispatcher_success() {
    JsonArray requestBody = new JsonArray()
      .add(new JsonObject()
        .put("applicationId", "app1")
        .put("dataPermitId", "dp1")
        .put("catalogIds", new JsonArray().add("CAT123"))
        .put("status", "RECEIVED"));

    when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(requestBody);

    when(mongoClient.updateCollection(any(), any(), (JsonObject) any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
        MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
        when(updateResult.getDocMatched()).thenReturn(1L);
        handler.handle(Future.succeededFuture(updateResult));
        return null;
      });

    JsonObject dispatcherResponse = new JsonObject()
      .put("catalogIds", new JsonArray().add("CAT123"))
      .put("status", "RECEIVED");

    JsonObject dispatcherConfig = new JsonObject()
      .put("dispatcher_response", new JsonArray().add(dispatcherResponse))
      .put("application_type", "collectionName");

    JsonObject dataset = new JsonObject()
      .put("catalog_id", "CAT123")
      .put("dispatcher_response_status", "NONE");

    JsonObject mongoApp = new JsonObject()
      .put("datasets", new JsonArray().add(dataset))
      .put("application", new JsonObject().put("_id", "app1"))
      .put("configuration", dispatcherConfig);

    when(mongoClient.findOne(eq(DISPATCHER_COLLECTION), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApp));
        return null;
      });

    when(mongoClient.findOne(eq("collectionName"), any(JsonObject.class), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApp));
        return null;
      });

    dispatcherManager.updateApplicationStatusFromDispatcher(routingContext);
  }

  @Test
  void testUpdateDispatcherStatusInApplication_statusPropagated() {
    JsonObject dispatcherResponse = new JsonObject()
      .put("catalogIds", new JsonArray().add("CAT123"))
      .put("status", "RECEIVED");

    JsonObject dispatcherConfig = new JsonObject()
      .put("dispatcher_response", new JsonArray().add(dispatcherResponse))
      .put("application_type", "collectionName");

    JsonObject dispatcher = new JsonObject()
      .put("application", new JsonObject().put("_id", "app1"))
      .put("configuration", dispatcherConfig);

    JsonObject dataset = new JsonObject()
      .put("catalog_id", "CAT123")
      .put("dispatcher_response_status", "NONE");

    JsonObject mongoApp = new JsonObject()
      .put("datasets", new JsonArray().add(dataset));

    when(mongoClient.findOne(eq("collectionName"), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApp));
        return null;
      });

    dispatcherManager.updateDispatcherStatusInApplication(dispatcher, null);
  }

  @Test
  void testUpdateApplication_replaceSuccess() {
    JsonObject app = new JsonObject().put("datasets", new JsonArray());
    JsonObject query = new JsonObject().put("_id", "app1");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOneAndReplace(any(), any(), any(), any());

    dispatcherManager.updateApplication(app, query, "collectionName");
  }


  @Test
  void testUpdateApplicationStatusFromDispatcher_failed() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    JsonArray requestBody = new JsonArray()
      .add(new JsonObject()
        .put("applicationId", "app1")
        .put("dataPermitId", "dp1")
        .put("catalogIds", new JsonArray().add("CAT123"))
        .put("status", "RECEIVED"));

    when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(requestBody);

    JsonObject dispatcherResponse = new JsonObject()
      .put("catalogIds", new JsonArray().add("CAT123"))
      .put("status", "RECEIVED");

    JsonObject dispatcherConfig = new JsonObject()
      .put("dispatcher_response", new JsonArray().add(dispatcherResponse))
      .put("application_type", "collectionName");

    JsonObject dataset = new JsonObject()
      .put("catalog_id", "CAT123")
      .put("dispatcher_response_status", "NONE");

    JsonObject mongoApp = new JsonObject()
      .put("datasets", new JsonArray().add(dataset))
      .put("application", new JsonObject().put("_id", "app1"))
      .put("configuration", dispatcherConfig);

    when(mongoClient.updateCollection(any(), any(), (JsonObject) any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
        MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
        when(updateResult.getDocMatched()).thenReturn(1L);
        handler.handle(Future.failedFuture("failed"));
        return null;
      });

    when(mongoClient.updateCollectionWithOptions(eq(DISPATCHER_COLLECTION), any(),
      (JsonObject) any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(4);
        handler.handle(Future.succeededFuture(mongoApp));
        return null;
      });

    when(mongoClient.findOne(eq("collectionName"), any(JsonObject.class), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApp));
        return null;
      });

    dispatcherManager.updateApplicationStatusFromDispatcher(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event ->
          event.getLevel() == Level.INFO &&
            event.getFormattedMessage().contains("dispatcher_response record inserted"));
      assertTrue(found);
    });
  }

}
