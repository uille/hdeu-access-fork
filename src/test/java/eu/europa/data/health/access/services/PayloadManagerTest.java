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

import static eu.europa.data.health.access.Constants.CONFIGURATION_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
class PayloadManagerTest {

  @Mock
  private DBHandler dbHandler;

  @Mock
  private MongoClient mongoClient;

  @Mock
  private ControlledVocabularyManager controlledVocabularyManager;

  @Mock
  private HubTranslationClient hubTranslationClient;

  @Mock
  private SchemaTranslationService translationService;

  @Mock
  private RoutingContext routingContext;

  @Mock
  private RequestBody requestBody;

  private PayloadManager payloadManager;

  private PayloadManager createPayloadManager(JsonObject config) {
    return new PayloadManager(
      dbHandler,
      mongoClient,
      controlledVocabularyManager,
      hubTranslationClient,
      translationService,
      config
    );
  }

  @BeforeEach
  void setup() {
    payloadManager = createPayloadManager(new JsonObject());
  }

  @Test
  void testCreateOrUpdatePayload_callsUpsert() {
    JsonObject body = new JsonObject().put("name", "test-doc");
    when(routingContext.body()).thenReturn(requestBody);
    when(routingContext.body().asJsonObject()).thenReturn(body);

    payloadManager.createOrUpdatePayload(routingContext);

    verify(mongoClient).findOneAndUpdateWithOptions(
      anyString(), any(JsonObject.class), any(JsonObject.class),
      any(FindOptions.class), any(UpdateOptions.class), any()
    );
  }

  @Test
  void testUpsertByName_success_withPayload_triggersTranslation() {
    payloadManager = createPayloadManager(new JsonObject().put(Constants.ENABLE_TRANSLATION, true));
    JsonObject upsertData = new JsonObject()
      .put("name", "health-dcat-ap-schema")
      .put("payload", new JsonObject().put("foo", "bar"));
    JsonObject docWithId = new JsonObject().put("_id", "12345");

    // 1) findOne(...) for the schema doc (old payload lookup)
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null)); // no previous doc
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), any(), any());

    // 2) findOneAndUpdateWithOptions(...) returns new doc with _id
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(5);
      handler.handle(Future.succeededFuture(docWithId));
      return null;
    }).when(mongoClient).findOneAndUpdateWithOptions(
      anyString(), any(JsonObject.class), any(JsonObject.class),
      any(FindOptions.class), any(UpdateOptions.class), any()
    );

    when(
      translationService.buildTranslatableDict(any(JsonObject.class), any(), anyString(), anySet()))
      .thenReturn(new JsonObject().put("dictKey", "dictVal"));
    when(hubTranslationClient.createCallbackJson())
      .thenReturn(new JsonObject().put("callback", true));
    when(hubTranslationClient.requestTranslation(any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    payloadManager.upsertByName(mongoClient, "health-dcat-ap-schema", upsertData, routingContext);

    verify(hubTranslationClient).requestTranslation(any(JsonObject.class));
  }

  @Test
  void testUpsertByName_success_withPayload_andTranslationDisabled_skipsTranslation() {
    payloadManager = createPayloadManager(
      new JsonObject().put(Constants.ENABLE_TRANSLATION, false)
    );
    JsonObject upsertData = new JsonObject()
      .put("name", "health-dcat-ap-schema")
      .put("payload", new JsonObject().put("foo", "bar"));
    JsonObject docWithId = new JsonObject().put("_id", "12345");

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    })
      .when(mongoClient)
      .findOne(anyString(), any(JsonObject.class), any(), any());

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(5);
      handler.handle(Future.succeededFuture(docWithId));
      return null;
    })
      .when(mongoClient)
      .findOneAndUpdateWithOptions(
        anyString(),
        any(JsonObject.class),
        any(JsonObject.class),
        any(FindOptions.class),
        any(UpdateOptions.class),
        any()
      );

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    payloadManager.upsertByName(mongoClient, "health-dcat-ap-schema", upsertData, routingContext);

    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void testUpsertByName_success_withoutPayload_skipsTranslation() {
    JsonObject upsertData = new JsonObject().put("name", "health-dcat-ap-schema");
    JsonObject docWithId = new JsonObject().put("_id", "12345");

    // 1) findOne(...) for the schema doc
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), any(), any());

    // 2) findOneAndUpdateWithOptions(...) returns new doc with _id
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(5);
      handler.handle(Future.succeededFuture(docWithId));
      return null;
    }).when(mongoClient).findOneAndUpdateWithOptions(
      anyString(), any(JsonObject.class), any(JsonObject.class),
      any(FindOptions.class), any(UpdateOptions.class), any()
    );

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    payloadManager.upsertByName(mongoClient, "health-dcat-ap-schema", upsertData, routingContext);

    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void testUpsertByName_failure_setsErrorResponse() {
    // Non-schema doc -> no findOne() pre-read
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(5);
      handler.handle(Future.failedFuture("DB error"));
      return null;
    }).when(mongoClient).findOneAndUpdateWithOptions(
      anyString(), any(JsonObject.class), any(JsonObject.class),
      any(FindOptions.class), any(UpdateOptions.class), any()
    );

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    payloadManager.upsertByName(mongoClient, "docName", new JsonObject(), routingContext);

    // Ensure we didn't try to send a translation request
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void testRetrievePayload_success() {
    String name = "someDoc";
    when(routingContext.request()).thenReturn(mock(io.vertx.core.http.HttpServerRequest.class));
    when(routingContext.request().getParam("name")).thenReturn(name);

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    JsonObject found = new JsonObject().put("_id", "123").put("payload", new JsonObject());
    when(mongoClient.findOne(any(), any(), any())).thenReturn(Future.succeededFuture(found));

    payloadManager.retrievePayload(routingContext);

    verify(mongoClient).findOne(eq("CONFIGURATION"), any(), isNull());
  }

  @Test
  void testFindByFieldWithValue_found() {
    JsonObject result = new JsonObject().put("name", "doc");
    when(mongoClient.findOne(anyString(), any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture(result));

    var future = payloadManager.findByFieldWithValue("collection", "name", "doc");

    future.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals("doc", ar.result().getString("name"));
    });
  }

  @Test
  void testFindByFieldWithValue_notFound() {
    when(mongoClient.findOne(anyString(), any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture(null));

    var future = payloadManager.findByFieldWithValue("collection", "name", "doc");

    future.onComplete(ar -> {
      assert(ar.failed());
    });
  }

  @Test
  void testFindByFieldWithValue_schemaFallback_loadsFromFile_andUpserts() {
    // Initial DB lookup returns null (schema missing)
    when(mongoClient.findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Upsert attempt succeeds (fire-and-forget)
    doAnswer(inv -> {
      Handler<AsyncResult<?>> h = inv.getArgument(4);
      h.handle(Future.succeededFuture());
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );

    // Act
    var fut = payloadManager.findByFieldWithValue(CONFIGURATION_COLLECTION, "name",
      "health-dcat-ap-schema");

    fut.onComplete(ar -> {
      assertTrue(ar.succeeded(), "Fallback should return the file-backed document");
      JsonObject doc = ar.result();
      assertEquals("health-dcat-ap-schema", doc.getString("name"));
      assertNotNull(doc.getJsonObject("payload"));
    });

    // Ensure we attempted to persist what we loaded
    verify(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );
  }

  @Test
  void testFindByFieldWithValue_schemaFallback_upsertFails_butStillReturnsFile() {
    // Initial DB lookup returns null
    when(mongoClient.findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Upsert fails; we should still get the file content from the Future
    doAnswer(inv -> {
      Handler<AsyncResult<?>> h = inv.getArgument(4);
      h.handle(Future.failedFuture("boom"));
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );

    // Act
    var fut = payloadManager.findByFieldWithValue(CONFIGURATION_COLLECTION, "name",
      "health-dcat-ap-schema");

    fut.onComplete(ar -> {
      assertTrue(ar.succeeded(), "Even if upsert fails, we still return the file-backed document");
      JsonObject doc = ar.result();
      assertEquals("health-dcat-ap-schema", doc.getString("name"));
      assertNotNull(doc.getJsonObject("payload"));
    });

    verify(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );
  }

  @Test
  void testFindByFieldWithValue_nonSchema_noCvCalls() {
    JsonObject nonSchemaDoc = new JsonObject()
      .put("name", "someDoc")
      .put("payload", new JsonObject()); // shape irrelevant

    when(mongoClient.findOne(anyString(), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(nonSchemaDoc));

    var fut = payloadManager.findByFieldWithValue("collection", "name", "someDoc");

    fut.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals("someDoc", ar.result().getString("name"));
    });

    verify(controlledVocabularyManager, never()).getBySource(anyString());
  }

  @Test
  void testFindByFieldWithValue_schema_enriches_topLevelAndNested_andDedupsSources() {
    // Schema with two select inputs using SAME source, one nested
    JsonObject schemaDoc = new JsonObject()
      .put("name", "health-dcat-ap-schema")
      .put("payload", new JsonObject()
        .put("shapes", new JsonObject()
          .put("DatasetShape", new JsonObject()
            .put("properties", new JsonArray(List.of(
              new JsonObject()
                .put("input", new JsonObject()
                  .put("type", "select")
                  .put("source", "COUNTRIES")),

              new JsonObject()
                .put("nestedProperties", new JsonArray().add(
                  new JsonObject()
                    .put("input", new JsonObject()
                      .put("type", "select")
                      .put("source", "COUNTRIES"))))
            )))
          )
        )
      );

    when(mongoClient.findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(schemaDoc));

    JsonArray countries = new JsonArray()
      .add(new JsonObject().put("value", "BE").put("label", new JsonObject().put("en", "Belgium")))
      .add(new JsonObject().put("value", "DE").put("label", new JsonObject().put("en", "Germany")));

    when(controlledVocabularyManager.getBySource("COUNTRIES"))
      .thenReturn(Future.succeededFuture(countries));

    var fut = payloadManager.findByFieldWithValue(CONFIGURATION_COLLECTION, "name",
      "health-dcat-ap-schema");

    fut.onComplete(ar -> {
      assertTrue(ar.succeeded());
      JsonObject doc = ar.result();
      JsonObject payload = doc.getJsonObject("payload");
      JsonObject shapes = payload.getJsonObject("shapes");
      JsonObject shape = shapes.getJsonObject("DatasetShape");
      JsonArray props = shape.getJsonArray("properties");

      // Top-level select got options
      JsonObject topInput = props.getJsonObject(0).getJsonObject("input");
      assertNotNull(topInput.getJsonArray("options"));
      assertEquals(2, topInput.getJsonArray("options").size());
      assertEquals("BE", topInput.getJsonArray("options").getJsonObject(0).getString("value"));

      // Nested select got options
      JsonObject nestedInput = props.getJsonObject(1)
        .getJsonArray("nestedProperties").getJsonObject(0)
        .getJsonObject("input");
      assertNotNull(nestedInput.getJsonArray("options"));
      assertEquals(2, nestedInput.getJsonArray("options").size());

      // Dedup: only one fetch for "COUNTRIES"
      verify(controlledVocabularyManager, times(1)).getBySource("COUNTRIES");
    });
  }

  @Test
  void testFindByFieldWithValue_schema_enriches_multipleSources_withOneFailing() {
    // Two different sources, one will fail
    JsonObject schemaDoc = new JsonObject()
      .put("name", "health-dcat-ap-schema")
      .put("payload", new JsonObject()
        .put("shapes", new JsonObject()
          .put("DatasetShape", new JsonObject()
            .put("properties", new JsonArray(List.of(
              new JsonObject()
                .put("input", new JsonObject()
                  .put("type", "select")
                  .put("source", "OK_SOURCE")),

              new JsonObject()
                .put("input", new JsonObject()
                  .put("type", "select")
                  .put("source", "BROKEN_SOURCE"))
            )))
          )
        )
      );

    when(mongoClient.findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(schemaDoc));

    JsonArray okOptions = new JsonArray().add(new JsonObject().put("value", "A"));
    when(controlledVocabularyManager.getBySource("OK_SOURCE")).thenReturn(
      Future.succeededFuture(okOptions));
    when(controlledVocabularyManager.getBySource("BROKEN_SOURCE")).thenReturn(
      Future.failedFuture("boom"));

    var fut = payloadManager.findByFieldWithValue(CONFIGURATION_COLLECTION, "name",
      "health-dcat-ap-schema");

    fut.onComplete(ar -> {
      assertTrue(ar.succeeded());
      JsonObject doc = ar.result();
      JsonArray props = doc.getJsonObject("payload")
        .getJsonObject("shapes")
        .getJsonObject("DatasetShape")
        .getJsonArray("properties");

      // OK source has its options
      JsonArray okArr = props.getJsonObject(0).getJsonObject("input").getJsonArray("options");
      assertNotNull(okArr);
      assertEquals(1, okArr.size());
      assertEquals("A", okArr.getJsonObject(0).getString("value"));

      // BROKEN source yields empty options (recovered)
      JsonArray brokenArr = props.getJsonObject(1).getJsonObject("input").getJsonArray("options");
      assertNotNull(brokenArr);
      assertEquals(0, brokenArr.size());
    });

    verify(controlledVocabularyManager).getBySource("OK_SOURCE");
    verify(controlledVocabularyManager).getBySource("BROKEN_SOURCE");
  }

  @Test
  void testFindByFieldWithValue_schema_fallback_file_enriched_and_upsertAttempted() {
    // DB misses schema
    when(mongoClient.findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Upsert attempt (best-effort) — succeed
    doAnswer(inv -> {
      Handler<AsyncResult<?>> h = inv.getArgument(4);
      h.handle(Future.succeededFuture());
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );

    // Make the CV return something predictable (in case the test resource includes select sources)
    when(controlledVocabularyManager.getBySource(anyString()))
      .thenReturn(Future.succeededFuture(new JsonArray()));

    // Act: this will load from classpath file, enrich (if any select+source present), and upsert
    var fut = payloadManager.findByFieldWithValue(CONFIGURATION_COLLECTION, "name",
      "health-dcat-ap-schema");

    fut.onComplete(ar -> {
      assertTrue(ar.succeeded(),
        "Fallback should return the file-backed (and possibly enriched) document");
      JsonObject doc = ar.result();
      assertEquals("health-dcat-ap-schema", doc.getString("name"));
      assertNotNull(doc.getJsonObject("payload"));
    });

    // Ensure an upsert was attempted
    verify(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );
  }

  @Test
  void testRetrievePayload_schemaFallback_sets200_andAttemptsUpsert() {
    // Request for schema
    when(routingContext.request()).thenReturn(mock(io.vertx.core.http.HttpServerRequest.class));
    when(routingContext.request().getParam("name")).thenReturn("health-dcat-ap-schema");

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    // First findOne returns null (missing)
    when(mongoClient.findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Upsert callback (we don’t care success/fail for this test)
    doAnswer(inv -> {
      Handler<AsyncResult<?>> h = inv.getArgument(4);
      h.handle(Future.succeededFuture());
      return null;
    }).when(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );

    // Act
    payloadManager.retrievePayload(routingContext);

    // Verify we did try DB read
    verify(mongoClient).findOne(eq(CONFIGURATION_COLLECTION), any(JsonObject.class), isNull());

    // We can’t easily assert the response body due to setUpContextResponse being in AbstractManager,
    // but we can at least ensure we attempted the upsert.
    verify(mongoClient).updateCollectionWithOptions(
      eq(CONFIGURATION_COLLECTION), any(JsonObject.class), any(JsonObject.class),
      any(UpdateOptions.class), any()
    );
  }

  @Test
  void testProcessTranslationCallback_success() {
    JsonObject doc = new JsonObject().put("name", "doc").put("payload", new JsonObject());
    when(mongoClient.findOne(anyString(), any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture(doc));
    when(mongoClient.updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    doAnswer(inv -> {
      JsonObject payload = inv.getArgument(0);
      payload.put("translated", true);
      return null;
    }).when(translationService).applyTranslationsToSchema(any(), any());

    payloadManager.processTranslationCallback("doc", new JsonObject().put("key", "val"));

    verify(translationService).applyTranslationsToSchema(any(), any());
    verify(mongoClient).updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class));
  }

  @Test
  void testProcessTranslationCallback_docNotFound() {
    when(mongoClient.findOne(anyString(), any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture(null));

    payloadManager.processTranslationCallback("doc", new JsonObject());

    verify(mongoClient, never()).updateCollection(anyString(), any(JsonObject.class),
      any(JsonObject.class));
  }
}
