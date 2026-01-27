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
package eu.europa.data.health.access.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


class DBHandlerAdditionalTest {

  private MongoClient mongoClient;
  private Object dbHandler; // keep Object to avoid compile breaks if your DBHandler package differs

  @BeforeEach
  void setUp() {
    mongoClient = mock(MongoClient.class);
    dbHandler = createDbHandler(mongoClient);
  }


  private Object createDbHandler(MongoClient client) {
    try {
      // Change this to your actual fully-qualified DBHandler class if needed
      Class<?> cls = Class.forName("eu.europa.data.health.access.handlers.DBHandler");
      Object instance;

      // Try common constructors first; otherwise fall back to no-args + reflection field set.
      Optional<?> ctorInstance =
        Arrays.stream(cls.getDeclaredConstructors())
          .filter(c -> c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(MongoClient.class))
          .findFirst()
          .map(c -> {
            try {
              c.setAccessible(true);
              return c.newInstance(client);
            } catch (Exception e) {
              return null;
            }
          });

      if (ctorInstance.isPresent() && ctorInstance.get() != null) {
        instance = ctorInstance.get();
      } else {
        instance = cls.getDeclaredConstructor().newInstance();
        setField(instance, "mongoClient", client);
      }
      return instance;
    } catch (Exception e) {
      throw new RuntimeException("Adapt createDbHandler(...) to your DBHandler constructors/fields", e);
    }
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Cannot set field '" + fieldName + "'. Adapt helper.", e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Future<T> invokeFuture(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Object out = dbHandler.getClass().getMethod(methodName, paramTypes).invoke(dbHandler, args);
      return (Future<T>) out;
    } catch (Exception e) {
      throw new RuntimeException("Invoke failed for method: " + methodName, e);
    }
  }

  // A controllable ReadStream for aggregate(...) codepaths that use handler/exception/endHandler
  private static final class TestReadStream implements ReadStream<JsonObject> {
    private Handler<JsonObject> dataHandler;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> endHandler;

    @Override
    public ReadStream<JsonObject> handler(Handler<JsonObject> handler) {
      this.dataHandler = handler;
      return this;
    }

    @Override
    public ReadStream<JsonObject> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public ReadStream<JsonObject> endHandler(Handler<Void> endHandler) {
      this.endHandler = endHandler;
      return this;
    }

    // Not used in DBHandler methods shown, but required by interface in some vertx versions
    @Override public ReadStream<JsonObject> pause() { return this; }
    @Override public ReadStream<JsonObject> resume() { return this; }
    @Override public ReadStream<JsonObject> fetch(long amount) { return this; }

    void emit(JsonObject item) {
      if (dataHandler != null) dataHandler.handle(item);
    }

    void fail(Throwable t) {
      if (exceptionHandler != null) exceptionHandler.handle(t);
    }

    void end() {
      if (endHandler != null) endHandler.handle(null);
    }
  }

  // ------------------------------------------------------------
  // Tests: findApplications / findAuditLogs (findWithOptions)
  // ------------------------------------------------------------

  @Test
  void findApplications_success_shouldApplyPaginationAndSort_andReturnList() {
    List<JsonObject> resultList = List.of(new JsonObject().put("id", 1));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(resultList));
      return null;
    }).when(mongoClient).findWithOptions(eq("apps"), any(JsonObject.class), any(FindOptions.class), any());

    Future<List<JsonObject>> fut = invokeFuture(
      "findApplications",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "apps", 2, 10, "date_created", "asc", new JsonObject().put("x", "y")
    );

    assertTrue(fut.succeeded());
    assertEquals(resultList, fut.result());

    ArgumentCaptor<FindOptions> optionsCap = ArgumentCaptor.forClass(FindOptions.class);
    verify(mongoClient).findWithOptions(eq("apps"), any(JsonObject.class), optionsCap.capture(), any());
    FindOptions options = optionsCap.getValue();

    assertEquals(10, options.getLimit());
    assertEquals((2 - 1) * 10, options.getSkip());
    assertEquals(new JsonObject().put("date_created", 1), options.getSort());
  }

  @Test
  void findApplications_failure_shouldFailFuture() {
    RuntimeException boom = new RuntimeException("boom");

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(3);
      h.handle(Future.failedFuture(boom));
      return null;
    }).when(mongoClient).findWithOptions(eq("apps"), any(JsonObject.class), any(FindOptions.class), any());

    Future<List<JsonObject>> fut = invokeFuture(
      "findApplications",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "apps", null, null, null, null, new JsonObject()
    );

    assertTrue(fut.failed());
    assertSame(boom, fut.cause());
  }

  @Test
  void findAuditLogs_success_shouldApplySortDesc_andReturnList() {
    List<JsonObject> resultList = List.of(new JsonObject().put("id", "a"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<io.vertx.core.AsyncResult<List<JsonObject>>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(resultList));
      return null;
    }).when(mongoClient).findWithOptions(eq("audit"), any(JsonObject.class), any(FindOptions.class), any());

    Future<List<JsonObject>> fut = invokeFuture(
      "findAuditLogs",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "audit", 1, 5, "ts", "DESC", new JsonObject()
    );

    assertTrue(fut.succeeded());
    assertEquals(resultList, fut.result());

    ArgumentCaptor<FindOptions> optionsCap = ArgumentCaptor.forClass(FindOptions.class);
    verify(mongoClient).findWithOptions(eq("audit"), any(JsonObject.class), optionsCap.capture(), any());
    assertEquals(new JsonObject().put("ts", -1), optionsCap.getValue().getSort());
  }

  // ------------------------------------------------------------
  // Tests: findDataPermitGrantedDatasetsPerApplication (aggregate stream handlers)
  // ------------------------------------------------------------

  @Test
  void findDataPermitGrantedDatasetsPerApplication_defaults_shouldBuildPipeline_andReturnEmittedItems() {
    TestReadStream stream = new TestReadStream();

    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    when(mongoClient.aggregate(eq("apps"), pipelineCap.capture())).thenReturn(stream);

    Future<List<JsonObject>> fut = invokeFuture(
      "findDataPermitGrantedDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "apps",
      null, null,  // defaults page=1 limit=5
      "date_created", "desc",
      null          // queryOptions null => no initial $match
    );

    // Emit two items then end
    JsonObject a = new JsonObject().put("k", 1);
    JsonObject b = new JsonObject().put("k", 2);
    stream.emit(a);
    stream.emit(b);
    stream.end();

    assertTrue(fut.succeeded());
    assertEquals(List.of(a, b), fut.result());

    JsonArray pipeline = pipelineCap.getValue();

    // Should NOT have initial $match when queryOptions null
    assertFalse(pipeline.stream().anyMatch(o -> ((JsonObject) o).containsKey("$match") && ((JsonObject) o).getJsonObject("$match") == null));

    // Should have $sort + $skip + $limit at the end
    JsonObject lastSort = (JsonObject) pipeline.getJsonObject(pipeline.size() - 3).getValue("$sort");
    JsonObject lastSkip = pipeline.getJsonObject(pipeline.size() - 2);
    JsonObject lastLimit = pipeline.getJsonObject(pipeline.size() - 1);

    assertEquals(new JsonObject().put("date_created", -1), lastSort);
    assertEquals(0, lastSkip.getInteger("$skip"));
    assertEquals(5, lastLimit.getInteger("$limit"));
  }

  @Test
  void findDataPermitGrantedDatasetsPerApplication_exceptionHandler_shouldFailFuture() {
    TestReadStream stream = new TestReadStream();
    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(stream);

    Future<List<JsonObject>> fut = invokeFuture(
      "findDataPermitGrantedDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "apps", 1, 5, null, null, new JsonObject().put("title", "x")
    );

    RuntimeException boom = new RuntimeException("boom");
    stream.fail(boom);

    assertTrue(fut.failed());
    assertSame(boom, fut.cause());
  }

  // ------------------------------------------------------------
  // Tests: findDecisionIssuedDatasetsPerApplication (aggregate(...).collect(...))
  // ------------------------------------------------------------

  @Test
  void findDecisionIssuedDatasetsPerApplication_positive_defaultSort_shouldBuildWantedStatus_andReturnList() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggStream = mock(ReadStream.class);

    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(aggStream);

    List<JsonObject> out = List.of(new JsonObject().put("x", 1));
    when(aggStream.collect(any(Collector.class))).thenReturn(Future.succeededFuture(out));

    Future<List<JsonObject>> fut = invokeFuture(
      "findDecisionIssuedDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class, Boolean.class},
      "apps", null, null, null, null, null, Boolean.TRUE
    );

    assertTrue(fut.succeeded());
    assertEquals(out, fut.result());

    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    verify(mongoClient).aggregate(eq("apps"), pipelineCap.capture());
    JsonArray pipeline = pipelineCap.getValue();

    // wantedStatus should be POSITIVE_DECISION_ISSUED in an $elemMatch
    String json = pipeline.encode();
    assertTrue(json.contains("POSITIVE_DECISION_ISSUED"));

    // default sort is updatedAt desc (-1)
    assertTrue(json.contains("\"$sort\""));
    assertTrue(json.contains("\"updatedAt\""));
  }

  @Test
  void findDecisionIssuedDatasetsPerApplication_negative_customSortAsc_shouldUseProvidedSort() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggStream = mock(ReadStream.class);

    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    when(mongoClient.aggregate(eq("apps"), pipelineCap.capture())).thenReturn(aggStream);

    when(aggStream.collect(any(Collector.class))).thenReturn(Future.succeededFuture(Collections.emptyList()));

    Future<List<JsonObject>> fut = invokeFuture(
      "findDecisionIssuedDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class, Boolean.class},
      "apps", 2, 7, "dataset.updatedAt", "asc", new JsonObject().put("owner", "me"), Boolean.FALSE
    );

    assertTrue(fut.succeeded());
    JsonArray pipeline = pipelineCap.getValue();
    String json = pipeline.encode();

    assertTrue(json.contains("NEGATIVE_DECISION_ISSUED"));
    assertTrue(json.contains("\"dataset.updatedAt\""));
    assertTrue(json.contains(":1")); // asc
    assertTrue(json.contains("\"$skip\""));
    assertTrue(json.contains("\"$limit\""));
  }

  @Test
  void findDecisionIssuedDatasetsPerApplication_onFailure_shouldFailFuture() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggStream = mock(ReadStream.class);

    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(aggStream);
    RuntimeException boom = new RuntimeException("boom");
    when(aggStream.collect(any(Collector.class))).thenReturn(Future.failedFuture(boom));

    Future<List<JsonObject>> fut = invokeFuture(
      "findDecisionIssuedDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class, Boolean.class},
      "apps", 1, 5, null, null, null, Boolean.TRUE
    );

    assertTrue(fut.failed());
    assertSame(boom, fut.cause());
  }

  // ------------------------------------------------------------
  // Tests: findDatasetsPerApplication (exercise $and logic + permit match paths)
  // ------------------------------------------------------------

  @Test
  void findDatasetsPerApplication_withAndClauses_shouldAddDecisionMatch_andPermitMatch() {
    TestReadStream stream = new TestReadStream();

    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    when(mongoClient.aggregate(eq("apps"), pipelineCap.capture())).thenReturn(stream);

    // $and with a null clause hits if clause = null continue
    // plus datasets.decisions and datasets.permits clauses (hits both extractLatest* branches)
    JsonArray and = new JsonArray()
      .add((JsonObject) null)
      .add(new JsonObject().put("datasets.decisions", new JsonObject().put("$elemMatch", new JsonObject().put("datasetStatus", "X"))))
      .add(new JsonObject().put("datasets.permits", new JsonObject().put("$elemMatch", new JsonObject().put("permit_status", "GRANTED"))));

    JsonObject queryOptions = new JsonObject().put("$and", and);

    Future<List<JsonObject>> fut = invokeFuture(
      "findDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "apps", 1, 5, "date_modified", "asc", queryOptions
    );

    JsonObject item = new JsonObject().put("ok", true);
    stream.emit(item);
    stream.end();

    assertTrue(fut.succeeded());
    assertEquals(List.of(item), fut.result());

    String json = pipelineCap.getValue().encode();

    // Should include the ALWAYS decisions existence match
    assertTrue(json.contains("\"datasets.decisions\""));
    assertTrue(json.contains("\"$exists\""));

    // Should include extracted decisions match from $and
    assertTrue(json.contains("datasetStatus"));

    // Should include permit existence match + extracted permit match
    assertTrue(json.contains("\"datasets.permits\""));
    assertTrue(json.contains("permit_status"));

    // Sort key: date_modified (since sortBy provided and not blank and not date_created)
    assertTrue(json.contains("\"$sort\""));
    assertTrue(json.contains("\"date_modified\""));
    assertTrue(json.contains(":1")); // asc
  }

  @Test
  void findDatasetsPerApplication_defaultSort_shouldUseDateCreated_desc() {
    TestReadStream stream = new TestReadStream();
    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    when(mongoClient.aggregate(eq("apps"), pipelineCap.capture())).thenReturn(stream);

    Future<List<JsonObject>> fut = invokeFuture(
      "findDatasetsPerApplication",
      new Class<?>[]{String.class, Integer.class, Integer.class, String.class, String.class, JsonObject.class},
      "apps", null, null, null, null, new JsonObject()
    );

    stream.end();
    assertTrue(fut.succeeded());

    String json = pipelineCap.getValue().encode();
    assertTrue(json.contains("\"date_created\""));
    assertTrue(json.contains(":-1")); // default desc
  }

  // ------------------------------------------------------------
  // Tests: countDecisionIssuedDatasetsPerApplication
  // ------------------------------------------------------------

  @Test
  void countDecisionIssuedDatasetsPerApplication_isPositiveNull_emptyResult_shouldReturn0() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggStream = mock(ReadStream.class);

    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    when(mongoClient.aggregate(eq("apps"), pipelineCap.capture())).thenReturn(aggStream);
    when(aggStream.collect(any(Collector.class))).thenReturn(Future.succeededFuture(Collections.emptyList()));

    Future<Integer> fut = invokeFuture(
      "countDecisionIssuedDatasetsPerApplication",
      new Class<?>[]{String.class, JsonObject.class, Boolean.class},
      "apps", new JsonObject().put("title", "x"), null
    );

    assertTrue(fut.succeeded());
    assertEquals(0, fut.result());

    // Allowed statuses should include both positive & negative in pipeline
    String json = pipelineCap.getValue().encode();
    assertTrue(json.contains("POSITIVE_DECISION_ISSUED"));
    assertTrue(json.contains("NEGATIVE_DECISION_ISSUED"));
  }

  @Test
  void countDecisionIssuedDatasetsPerApplication_isPositiveTrue_shouldReturnCountFromFirstDoc() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggStream = mock(ReadStream.class);

    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(aggStream);
    when(aggStream.collect(any(Collector.class)))
      .thenReturn(Future.succeededFuture(List.of(new JsonObject().put("count", 42))));

    Future<Integer> fut = invokeFuture(
      "countDecisionIssuedDatasetsPerApplication",
      new Class<?>[]{String.class, JsonObject.class, Boolean.class},
      "apps", null, Boolean.TRUE
    );

    assertTrue(fut.succeeded());
    assertEquals(42, fut.result());
  }

  @Test
  void countDecisionIssuedDatasetsPerApplication_onFailure_shouldFail() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggStream = mock(ReadStream.class);

    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(aggStream);
    RuntimeException boom = new RuntimeException("boom");
    when(aggStream.collect(any(Collector.class))).thenReturn(Future.failedFuture(boom));

    Future<Integer> fut = invokeFuture(
      "countDecisionIssuedDatasetsPerApplication",
      new Class<?>[]{String.class, JsonObject.class, Boolean.class},
      "apps", null, Boolean.FALSE
    );

    assertTrue(fut.failed());
    assertSame(boom, fut.cause());
  }

  // ------------------------------------------------------------
  // Tests: countDatasetsPerDataPermitGrantedDatasetApplication (stream-based)
  // ------------------------------------------------------------

  @Test
  void countDatasetsPerDataPermitGrantedDatasetApplication_emptyAggregation_shouldReturn0() {
    TestReadStream stream = new TestReadStream();
    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(stream);

    Future<Integer> fut = invokeFuture(
      "countDatasetsPerDataPermitGrantedDatasetApplication",
      new Class<?>[]{String.class, JsonObject.class},
      "apps", null
    );

    stream.end();

    assertTrue(fut.succeeded());
    assertEquals(0, fut.result());
  }

  @Test
  void countDatasetsPerDataPermitGrantedDatasetApplication_withCountDoc_shouldReturnCount() {
    TestReadStream stream = new TestReadStream();
    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(stream);

    Future<Integer> fut = invokeFuture(
      "countDatasetsPerDataPermitGrantedDatasetApplication",
      new Class<?>[]{String.class, JsonObject.class},
      "apps", new JsonObject().put("owner", "me")
    );

    stream.emit(new JsonObject().put("count", 9));
    stream.end();

    assertTrue(fut.succeeded());
    assertEquals(9, fut.result());
  }

  // ------------------------------------------------------------
  // Tests: countDatasetsPerApplication (split appMatch vs dsMatch)
  // ------------------------------------------------------------

  @Test
  void countDatasetsPerApplication_shouldSplitAppAndDatasetFilters_andReturnCount() {
    TestReadStream stream = new TestReadStream();

    ArgumentCaptor<JsonArray> pipelineCap = ArgumentCaptor.forClass(JsonArray.class);
    when(mongoClient.aggregate(eq("apps"), pipelineCap.capture())).thenReturn(stream);

    // one app-level filter + one dataset-level filter
    JsonObject query = new JsonObject()
      .put("title", "T")
      .put("datasets.hdab", "X");

    Future<Integer> fut = invokeFuture(
      "countDatasetsPerApplication",
      new Class<?>[]{String.class, JsonObject.class},
      "apps", query
    );

    stream.emit(new JsonObject().put("count", 3));
    stream.end();

    assertTrue(fut.succeeded());
    assertEquals(3, fut.result());

    String json = pipelineCap.getValue().encode();

    // app match should include "title"
    assertTrue(json.contains("\"$match\""));
    assertTrue(json.contains("\"title\""));

    // dataset match should include "datasets.hdab"
    assertTrue(json.contains("\"datasets.hdab\""));

    // always match datasets.decisions exists
    assertTrue(json.contains("\"datasets.decisions\""));
    assertTrue(json.contains("\"$exists\""));
  }

  @Test
  void countDatasetsPerApplication_whenQueryNull_shouldStillWork_andReturn0IfEmpty() {
    TestReadStream stream = new TestReadStream();
    when(mongoClient.aggregate(eq("apps"), any(JsonArray.class))).thenReturn(stream);

    Future<Integer> fut = invokeFuture(
      "countDatasetsPerApplication",
      new Class<?>[]{String.class, JsonObject.class},
      "apps", null
    );

    stream.end();
    assertTrue(fut.succeeded());
    assertEquals(0, fut.result());
  }
}
