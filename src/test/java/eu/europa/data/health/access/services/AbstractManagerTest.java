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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MongoDBContainer;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
class AbstractManagerTest {

  private static MongoDBContainer mongoDBContainer;
  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";

  private static MongoClient mongoClient;
  private static DBHandler dbHandler;
  private static AbstractManager abstractManager;

  public static final String USER_ID = "1eadc727-9da3-44e9-80a0-867d291f3bf7";

  private static final String ABSTRACT_MANAGER_COLLECTION = "abstractManagerCollection";

  @BeforeAll
  static void setUp(VertxTestContext testContext) {
    Vertx vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:4.4.2");
    mongoDBContainer.start();

    // Configure MongoClient with the connection string
    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE));

    mongoClient = MongoClient.createShared(vertx, config);
    dbHandler = new DBHandler(mongoClient);

    abstractManager = new AbstractManager(dbHandler) {
    };

    testContext.completeNow();
  }

  @AfterAll
  static void tearDown(VertxTestContext testContext) {
    try {
      mongoClient.close().onComplete(v -> {
        mongoDBContainer.stop();
        testContext.completeNow();
      });
    } catch (Exception e) {
      testContext.failNow(e);
    }
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCreateEntity(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject().put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue"));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    //Since createEntity() is void, we can't use onSuccess, so we wait for the async response
    //Query the database after calling createEntity to check if the document was inserted
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");
      dbHandler.findById(ABSTRACT_MANAGER_COLLECTION, docId)
        .onSuccess(result -> {
          assertNotNull(result);
          assertNotNull(result.getString("_id"));
          assertEquals(docId, result.getString("_id"));
          assertEquals("abstractManagerTestKey", result.getString("key"));
          assertEquals("abstractManagerTestValue", result.getString("value"));
          testContext.completeNow();
        })
        .onFailure(testContext::failNow);
    });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateEntity(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      // Perform the update
      JsonObject updatedDocument = new JsonObject()
        .put("key", "abstractManagerTestKeyUpdated")
        .put("value", "abstractManagerTestValueUpdate")
        .put("id", docId);
      RoutingContext updateContext = RoutingContextMocker.mockRoutingContext(updatedDocument);
      abstractManager.updateEntity(updateContext, ABSTRACT_MANAGER_COLLECTION);

      vertx.setTimer(1000, timerId2 -> dbHandler.findById(ABSTRACT_MANAGER_COLLECTION, docId)
        .onSuccess(result -> {
          assertNotNull(result);
          assertNotNull(result.getString("_id"));
          assertEquals(docId, result.getString("_id"));
          assertEquals("abstractManagerTestKeyUpdated", result.getString("key"));
          assertEquals("abstractManagerTestValueUpdate", result.getString("value"));
          testContext.completeNow();
        })
        .onFailure(testContext::failNow));
    });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testPatchEntity(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      // Perform the update
      JsonObject updatedDocument = new JsonObject()
        .put("key", "abstractManagerTestKeyPatched")
        .put("value", "abstractManagerTestValuePatched");

      RoutingContext updateContext = RoutingContextMocker.mockRoutingContext(updatedDocument);
      // Mock the path parameter retrieval
      when(updateContext.pathParam("id")).thenReturn(docId);
      abstractManager.patchEntity(updateContext, ABSTRACT_MANAGER_COLLECTION);

      vertx.setTimer(1000, updateContextTimerId -> dbHandler.findById(ABSTRACT_MANAGER_COLLECTION, docId)
          .onSuccess(result -> {
            assertNotNull(result);
            assertNotNull(result.getString("_id"));
            assertEquals(docId, result.getString("_id"));
            assertEquals("abstractManagerTestKeyPatched", result.getString("key"));
            assertEquals("abstractManagerTestValuePatched", result.getString("value"));
            testContext.completeNow();
          })
          .onFailure(testContext::failNow));
    });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testGetEntity(Vertx vertx, VertxTestContext testContext) {
    createEntities(10);
    vertx.setTimer(500, timerId -> {
      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject().put("userId", USER_ID));
      Integer pageNumber = 1;
      Integer pageLimit = 10;

      abstractManager.getEntity(context, ABSTRACT_MANAGER_COLLECTION, pageNumber, pageLimit, null, null, null);

      // Wait for the async code to finish
      vertx.setTimer(1000, timerId2 -> {
        String message = context.response().getStatusMessage();
        assertEquals(HttpResponseStatus.OK.code(), context.response().getStatusCode());
        assertTrue(message.contains("data"),"It should contains the content item");
        testContext.completeNow();
      });
    });
  }

  @Test
  void testFindById(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      RoutingContext findContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(findContext.pathParam("id")).thenReturn(docId);
      abstractManager.findById(findContext, ABSTRACT_MANAGER_COLLECTION);
      assertNotNull(findContext.body());
      vertx.setTimer(1000, findTimerId -> {
        String message = findContext.response().getStatusMessage();
        assertNotNull(message, "Item should not be null");
        assertTrue(message.contains(docId), "Response should contain the created document");
        testContext.completeNow();
      });
    });
  }

  @Test
  void testFindByPathParam(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      RoutingContext findContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(findContext.pathParam("pathParam")).thenReturn("value");
      abstractManager.findByPathParam(findContext, ABSTRACT_MANAGER_COLLECTION, "abstractManagerTestValue");
      vertx.setTimer(1000, timerIdFind -> {
        String message = findContext.response().getStatusMessage();
        assertEquals(HttpResponseStatus.OK.code(), findContext.response().getStatusCode());
        assertTrue(message.contains("abstractManagerTestValue"),"It should contains the content item");
        testContext.completeNow();
      });
    });
  }

  @Test
  void testDeleteByPathParam(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      RoutingContext deleteContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(deleteContext.pathParam("pathParam")).thenReturn("value");
      abstractManager.deleteByPathParam(deleteContext, ABSTRACT_MANAGER_COLLECTION, "abstractManagerTestValue");
      vertx.setTimer(1000, timerIdFind -> {
        assertEquals(HttpResponseStatus.NO_CONTENT.code(), deleteContext.response().getStatusCode());
        testContext.completeNow();
        });
    });
  }

  @Test
  void testDeleteById(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      RoutingContext deleteContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(deleteContext.pathParam("id")).thenReturn(docId);
      abstractManager.deleteEntityById(deleteContext, ABSTRACT_MANAGER_COLLECTION);
      vertx.setTimer(1000, timerIdFind -> {
        assertEquals(HttpResponseStatus.OK.code(), deleteContext.response().getStatusCode());
        testContext.completeNow();
      });
    });
  }

  @Test
  void testDeleteAllEntities(Vertx vertx, VertxTestContext testContext) {
    createEntities(11);
    vertx.setTimer(500, timerId -> {
      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject().put("userId", USER_ID));
      abstractManager.deleteAllEntities(context, ABSTRACT_MANAGER_COLLECTION);
      vertx.setTimer(1000, timerIdFind -> {
        assertEquals(HttpResponseStatus.NO_CONTENT.code(), context.response().getStatusCode());
        testContext.completeNow();
      });
    });
  }

  private void createEntities(Integer numberOfEntities){
    for(int i = 0; i < numberOfEntities; i++){
      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
        .put("title", "abstractManagerTestKey"+i)
        .put("value", "abstractManagerTestValue"+i)
        .put("userId", USER_ID));
      abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
      assertNotNull(context.body());
    }
  }
}
