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
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE;
import static eu.europa.data.health.access.Constants.CATALOG_IDS;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATA_PERMIT_ID;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_RESPONSE_STATUS;
import static eu.europa.data.health.access.Constants.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

import eu.europa.data.health.access.DispatcherResponseStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.mongo.UpdateOptions;
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
class DispatcherManagerTest {

  @Mock MongoClient mongoClient;
  @Mock DBHandler dbHandler;
  @Mock BasketManager basketManager;
  @Mock HubStoreClient hubStoreClient;
  @Mock Vertx vertx;
  @Mock AuditLogManager auditLogManager;
  @Mock HubTranslationClient hubTranslationClient;
  @Mock HubSearchClient hubSearchClient;
  @Mock AuthClient authClient;

  @Mock RoutingContext routingContext;
  @Mock HttpServerRequest request;
  @Mock HttpServerResponse response;
  @Mock RequestBody requestBody;

  DispatcherManager dispatcherManager;

  @BeforeEach
  void setUp() {
    dispatcherManager = Mockito.spy(new DispatcherManager(
      dbHandler,
      mongoClient,
      basketManager,
      hubStoreClient,
      vertx,
      auditLogManager,
      hubTranslationClient,
      hubSearchClient,
      authClient
    ));

    // IMPORTANT: shared stubs must be lenient to avoid UnnecessaryStubbingException
    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
    lenient().when(response.putHeader(anyString(), anyString())).thenReturn(response);
    lenient().when(response.end(anyString())).thenReturn(Future.succeededFuture());
    lenient().when(response.end()).thenReturn(Future.succeededFuture());
    lenient().when(routingContext.body()).thenReturn(requestBody);
  }

  // -------------------------------------------------------------------------
  // getDispatcherApplicationByIdAndVersion(...)
  // -------------------------------------------------------------------------

  @Test
  void getDispatcherApplicationByIdAndVersion_whenVersionNotInteger_shouldReturn400() {
    when(routingContext.pathParam("applicationId")).thenReturn("A1");
    when(routingContext.pathParam("version")).thenReturn("x");

    dispatcherManager.getDispatcherApplicationByIdAndVersion(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end(contains("version must be an integer"));
    verify(mongoClient, never()).findOne(anyString(), any(JsonObject.class), any());
  }

  @Test
  void getDispatcherApplicationByIdAndVersion_whenNotFound_shouldReturn404() {
    when(routingContext.pathParam("applicationId")).thenReturn("A1");
    when(routingContext.pathParam("version")).thenReturn("1");

    when(mongoClient.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    dispatcherManager.getDispatcherApplicationByIdAndVersion(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(404);
      verify(response).end(contains("Application not found"));
    });
  }

  @Test
  void getDispatcherApplicationByIdAndVersion_whenFound_shouldReturnApplicationJson() {
    when(routingContext.pathParam("applicationId")).thenReturn("A1");
    when(routingContext.pathParam("version")).thenReturn("2");

    JsonObject dispatcherDoc = new JsonObject()
      .put("application", new JsonObject().put("_id", "A1").put("version", 2).put("title", "T"));

    when(mongoClient.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(dispatcherDoc));

    dispatcherManager.getDispatcherApplicationByIdAndVersion(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).putHeader("Content-Type", "application/json");
      verify(response).end(contains("\"_id\""));
      verify(response).end(contains("\"A1\""));
      verify(response).end(contains("\"version\""));
    });
  }

  @Test
  void getDispatcherApplicationByIdAndVersion_whenMongoFails_shouldReturn500() {
    when(routingContext.pathParam("applicationId")).thenReturn("A1");
    when(routingContext.pathParam("version")).thenReturn("2");

    when(mongoClient.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("DB down"));

    dispatcherManager.getDispatcherApplicationByIdAndVersion(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("DB down"));
    });
  }

  // -------------------------------------------------------------------------
  // getDispatcherApplicationById(...)
  // -------------------------------------------------------------------------

  @Test
  void getDispatcherApplicationById_whenCountSucceeds_shouldReturnCountJson() {
    when(routingContext.pathParam("applicationId")).thenReturn("A1");

    when(mongoClient.count(eq(DISPATCHER_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(5L));

    dispatcherManager.getDispatcherApplicationById(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).putHeader("Content-Type", "application/json");
      verify(response).end(contains("\"count\""));
      verify(response).end(contains("5"));
    });
  }

  @Test
  void getDispatcherApplicationById_whenCountFails_shouldReturn500() {
    when(routingContext.pathParam("applicationId")).thenReturn("A1");

    when(mongoClient.count(eq(DISPATCHER_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("count failed"));

    dispatcherManager.getDispatcherApplicationById(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("count failed"));
    });
  }

  // -------------------------------------------------------------------------
  // updateDatasetDecisionFromDispatcher(...) + updateStatusFromDispatcher(...)
  // (delegation-only tests)
  // -------------------------------------------------------------------------

  @Test
  void updateDatasetDecisionFromDispatcher_shouldDelegateToChangeDatasetStatus() {
    JsonObject payload = new JsonObject()
      .put("catalog_id", "C1")
      .put("application_type", "myCollection")
      .put("decisions", new JsonArray().add(new JsonObject()
        .put("dataset_id", "D1")
        .put("decision", "POSITIVE_DECISION_ISSUED")
        .put("decision_file", "base64")
      ));

    JsonObject body = new JsonObject()
      .put("objectId", "A1")
      .put("payload", payload);

    when(requestBody.asJsonObject()).thenReturn(body);

    doNothing().when(dispatcherManager).changeDatasetStatus(
      anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyBoolean()
    );

    dispatcherManager.updateDatasetDecisionFromDispatcher(routingContext);

    verify(dispatcherManager).changeDatasetStatus(
      "A1",
      "C1",
      "D1",
      "POSITIVE_DECISION_ISSUED",
      "base64",
      "myCollection",
      routingContext,
      false
    );
  }

  @Test
  void updateStatusFromDispatcher_shouldDelegateToChangeCatalogStatus() {
    JsonObject payload = new JsonObject()
      .put("catalog_id", "C1")
      .put("status", "PROCESSING")
      .put("application_type", "myCollection")
      .put("statusPayload", new JsonObject().put("x", 1));

    JsonObject body = new JsonObject()
      .put("objectId", "A1")
      .put("payload", payload);

    when(requestBody.asJsonObject()).thenReturn(body);

    doNothing().when(dispatcherManager).changeCatalogStatus(
      anyString(), anyString(), anyString(), any(JsonObject.class), anyString(), any(), anyBoolean()
    );

    dispatcherManager.updateStatusFromDispatcher(routingContext);

    verify(dispatcherManager).changeCatalogStatus(
      "A1",
      "C1",
      "PROCESSING",
      new JsonObject().put("x", 1),
      "myCollection",
      routingContext,
      false
    );
  }

  // -------------------------------------------------------------------------
  // updateApplicationStatusFromDispatcher(...)
  // Covers: generateDispatcherResponse + timestamp + update/insert branch
  // -------------------------------------------------------------------------

  @Test
  void updateApplicationStatusFromDispatcher_whenFindOneAndUpdateSucceeds_shouldCallUpdateDispatcherStatusAndReturn200() {
    JsonArray req = new JsonArray().add(new JsonObject()
      .put(APPLICATION_ID, "A1")
      .put(DATA_PERMIT_ID, "P1")
      .put(STATUS, DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus())
      .put(CATALOG_IDS, new JsonArray().add("C1"))
    );
    when(requestBody.asJsonArray()).thenReturn(req);

    doNothing().when(dispatcherManager).updateDispatcherStatusInApplication(any(JsonObject.class), isNull());

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(5);

      JsonObject dispatcherDoc = new JsonObject()
        .put("application", new JsonObject().put("_id", "A1"))
        .put("configuration", new JsonObject().put(APPLICATION_TYPE, "myCollection"));
      h.handle(Future.succeededFuture(dispatcherDoc));
      return null;
    }).when(mongoClient).findOneAndUpdateWithOptions(
      eq(DISPATCHER_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(FindOptions.class),
      any(UpdateOptions.class),
      any()
    );

    dispatcherManager.updateApplicationStatusFromDispatcher(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Update procedure completed"));

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
      verify(dispatcherManager).updateDispatcherStatusInApplication(any(JsonObject.class), isNull())
    );

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).findOneAndUpdateWithOptions(
      eq(DISPATCHER_COLLECTION),
      any(JsonObject.class),
      updateCaptor.capture(),
      any(FindOptions.class),
      any(UpdateOptions.class),
      any()
    );

    String updateJson = updateCaptor.getValue().encode();
    assertTrue(updateJson.contains("latest_update_date"));
    assertTrue(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}").matcher(updateJson).find());
  }

  @Test
  void updateApplicationStatusFromDispatcher_whenFindOneAndUpdateReturnsNull_shouldUpsertAndCallUpdateDispatcherStatus() {
    JsonArray req = new JsonArray().add(new JsonObject()
      .put(APPLICATION_ID, "A1")
      .put(DATA_PERMIT_ID, "P1")
      .put(STATUS, "some_status")
      .put(CATALOG_IDS, new JsonArray().add("C1"))
    );
    when(requestBody.asJsonArray()).thenReturn(req);

    doNothing().when(dispatcherManager).updateDispatcherStatusInApplication(any(JsonObject.class), isNull());

    // First call: no match (result null)
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(5);
      h.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOneAndUpdateWithOptions(
      eq(DISPATCHER_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(FindOptions.class),
      any(UpdateOptions.class),
      any()
    );

    // Insert/upsert call succeeds
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<MongoClientUpdateResult>> h = inv.getArgument(4);
      h.handle(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(DISPATCHER_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class),
      any()
    );

    dispatcherManager.updateApplicationStatusFromDispatcher(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Update procedure completed"));

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(mongoClient).updateCollectionWithOptions(
        eq(DISPATCHER_COLLECTION),
        any(JsonObject.class),
        any(JsonObject.class),
        any(UpdateOptions.class),
        any()
      );
      verify(dispatcherManager).updateDispatcherStatusInApplication(any(JsonObject.class), isNull());
    });
  }

  // -------------------------------------------------------------------------
  // updateDispatcherStatusInApplication(...) - specificStatus branch
  // -------------------------------------------------------------------------

  @Test
  void updateDispatcherStatusInApplication_whenSpecificStatusProvided_shouldSetStatusForAllDatasets() {
    JsonObject mongoDispatcher = new JsonObject()
      .put("application", new JsonObject().put("_id", "A1"))
      .put("configuration", new JsonObject().put(APPLICATION_TYPE, "myCollection"));

    JsonObject app = new JsonObject()
      .put("_id", "A1")
      .put(DATASETS, new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
        .add(new JsonObject().put("catalog_id", "C2"))
      );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOne(eq("myCollection"), any(JsonObject.class), isNull(), any());

    dispatcherManager.updateDispatcherStatusInApplication(
      mongoDispatcher,
      DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      JsonArray ds = app.getJsonArray(DATASETS);
      assertEquals(
        DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus(),
        ds.getJsonObject(0).getString(DISPATCHER_RESPONSE_STATUS)
      );
      assertEquals(
        DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus(),
        ds.getJsonObject(1).getString(DISPATCHER_RESPONSE_STATUS)
      );
    });
  }

  // -------------------------------------------------------------------------
  // updateDispatcherStatusInApplication(...) - dispatcher_response loop (prepareDatasets)
  // -------------------------------------------------------------------------

  @Test
  void updateDispatcherStatusInApplication_whenDispatcherResponseProvided_shouldSetStatusForMatchingCatalogDatasets() {
    JsonArray catalogIds = new JsonArray().add("C1");
    JsonObject dispatcherResponseObj = new JsonObject()
      .put(CATALOG_IDS, catalogIds)
      .put(STATUS, "some_status");

    JsonObject mongoDispatcher = new JsonObject()
      .put("application", new JsonObject().put("_id", "A1"))
      .put("configuration", new JsonObject()
        .put(APPLICATION_TYPE, "myCollection")
        .put("dispatcher_response", new JsonArray().add(dispatcherResponseObj))
      );

    JsonObject app = new JsonObject()
      .put("_id", "A1")
      .put(DATASETS, new JsonArray()
        .add(new JsonObject().put("catalog_id", "C1"))
        .add(new JsonObject().put("catalog_id", "C999"))
      )
      .put("catalogs", new JsonArray()); // keep safe for any downstream access

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOne(eq("myCollection"), any(JsonObject.class), isNull(), any());

    dispatcherManager.updateDispatcherStatusInApplication(mongoDispatcher, null);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      JsonArray ds = app.getJsonArray(DATASETS);
      assertEquals("some_status", ds.getJsonObject(0).getString(DISPATCHER_RESPONSE_STATUS));
      assertEquals(null, ds.getJsonObject(1).getString(DISPATCHER_RESPONSE_STATUS));
    });
  }

  // -------------------------------------------------------------------------
  // updateApplication(...)
  // -------------------------------------------------------------------------

  @Test
  void updateApplication_whenReplaceCalled_shouldInvokeMongoFindOneAndReplace() {
    JsonObject app = new JsonObject().put("_id", "A1");
    JsonObject q = new JsonObject().put("_id", "A1");

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOneAndReplace(eq("myCollection"), eq(q), eq(app), any());

    dispatcherManager.updateApplication(app, q, "myCollection");

    verify(mongoClient).findOneAndReplace(eq("myCollection"), eq(q), eq(app), any());
  }

  @Test
  void updateApplication_whenApplicationNull_shouldDoNothing() {
    dispatcherManager.updateApplication((JsonObject) null, new JsonObject().put("_id", "A1"), "myCollection");
    verify(mongoClient, never()).findOneAndReplace(anyString(), any(), any(), any());
  }

  // -------------------------------------------------------------------------
  // updatePermitFromDispatcher(...) delegation
  // -------------------------------------------------------------------------

  @Test
  void updatePermitFromDispatcher_shouldDelegateToChangePermitStatusForEachPermit() {
    JsonArray permits = new JsonArray()
      .add(new JsonObject()
        .put("dataset_id", "D1")
        .put("permit_file", "b64")
        .put("permit_status", "GRANTED")
        .put("reason", "ok")
      );

    JsonObject payload = new JsonObject()
      .put("catalog_id", "C1")
      .put("permits", permits);

    JsonObject body = new JsonObject()
      .put("objectId", "A1")
      .put("payload", payload);

    when(requestBody.asJsonObject()).thenReturn(body);

    doNothing().when(dispatcherManager).changePermitStatus(
      anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyBoolean()
    );

    dispatcherManager.updatePermitFromDispatcher(routingContext);

    verify(dispatcherManager).changePermitStatus(
      "A1", "C1", "D1", "GRANTED",
      "ok", "b64", routingContext,
      false
    );
  }
}


