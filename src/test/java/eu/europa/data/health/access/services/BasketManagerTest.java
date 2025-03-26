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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MongoDBContainer;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
 class BasketManagerTest {

  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";
  private static MongoDBContainer mongoDBContainer;
  private static MongoClient mongoClient;
  private static DBHandler dbHandler;
  private static BasketManager basketManager;

  @BeforeEach
  void setUp(VertxTestContext testContext) {
    Vertx vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:4.4.2");
    mongoDBContainer.start();

    // Configure MongoClient with the connection string
    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE));

    mongoClient = MongoClient.createShared(vertx, config);
    dbHandler = new DBHandler(mongoClient);
    basketManager = new BasketManager(dbHandler, mongoClient);

    testContext.completeNow();
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
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
  void testCreateOrUpdateBasketDataset(VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(
      new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
        .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b"));

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals("Basket dataset created", result);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCreateOrUpdateBasketDatasetTwice(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(
      new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
        .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b"));
    RoutingContext context2 = RoutingContextMocker.mockRoutingContext(
      new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
        .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b"));

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals("Basket dataset created", result);

        vertx.setTimer(500, timerId -> {

          basketManager.createOrUpdateBasketDataset(context2)
            .onSuccess(result2 -> {
              assertNotNull(result2);
              assertEquals("Operation successful", result2);
              testContext.completeNow();
            })
            .onFailure(testContext::failNow);
        });

      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindByPathParam(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(
      new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
        .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b"));

    when(context.pathParam("dataset_id")).thenReturn("7cdaaad8-1123-42c4-9a0b-a648a23d637b");

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals("Basket dataset created", result);

        vertx.setTimer(500, timerId -> {

          basketManager.findByPathParam(context, "BASKET", "dataset_id");
          vertx.setTimer(500, timerId2 -> {
            verify(context.response()).setStatusCode(201);
            testContext.completeNow();
          });
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteByPathParam(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(
      new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
        .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b"));

    when(context.pathParam("dataset_id")).thenReturn("7cdaaad8-1123-42c4-9a0b-a648a23d637b");

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals("Basket dataset created", result);
        vertx.setTimer(500, timerId -> {

          basketManager.deleteByPathParam(context, "BASKET", "dataset_id");

          vertx.setTimer(500, timerId2 -> {
            verify(context.response()).setStatusCode(204);
            testContext.completeNow();
          });
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteAll(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          assertEquals(201, response.getStatusCode(), "Expected status code to be 201");
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");

          when(context.pathParam("id")).thenReturn(createdId);
          basketManager.deleteAllBasketDatasets(context);

          vertx.setTimer(800, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            String statusMessage = response2.getStatusMessage();
            assertEquals(204, statusCode, "Expected status code to be 204");
            assertNotNull(statusMessage);
            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);


  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindById(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");

          when(context.pathParam("id")).thenReturn(createdId);
          basketManager.findById(context, "BASKET");

          vertx.setTimer(800, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            String statusMessage = response2.getStatusMessage();
            assertEquals(200, statusCode, "Expected status code to be 200");
            assertNotNull(statusMessage);
            JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
            assertEquals("7cdaaad8-1123-42c4-9a0b-a648a23d637b",
              foundResponse.getString("dataset_id"));
            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);

  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testGetBasketDatasetByDatasetId(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");
          assertNotNull(createdId);

          when(context.pathParam("dataset_id")).thenReturn("7cdaaad8-1123-42c4-9a0b-a648a23d637b");
          basketManager.getBasketDatasetByDatasetId(context);

          vertx.setTimer(800, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            String statusMessage = response2.getStatusMessage();
            assertEquals(200, statusCode, "Expected status code to be 200");
            assertNotNull(statusMessage);
            JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
            assertEquals("7cdaaad8-1123-42c4-9a0b-a648a23d637b",
              foundResponse.getString("dataset_id"));
            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);

  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testGetAllBasketDatasets(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");
          assertNotNull(createdId);

          when(context.request().getParam("page")).thenReturn("1");
          when(context.request().getParam("limit")).thenReturn("2");
          when(context.request().getParam("sortBy")).thenReturn("dataset_id");
          when(context.request().getParam("sortOrder")).thenReturn("asc");
          basketManager.getAllBasketDatasets(context);

          vertx.setTimer(800, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            String statusMessage = response2.getStatusMessage();
            assertEquals(200, statusCode, "Expected status code to be 200");
            assertNotNull(statusMessage);
            JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
            assertEquals("1", foundResponse.getJsonObject("metadata").getString("total_documents"));

            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);

  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testGetDistributionCount(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");
          assertNotNull(createdId);

          basketManager.getDistributionCount(context);

          vertx.setTimer(800, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            String statusMessage = response2.getStatusMessage();
            assertEquals(200, statusCode, "Expected status code to be 200");
            assertNotNull(statusMessage);
            JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
            assertEquals("1", foundResponse.getString("distribution_count"));
            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);

  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteBasketDatasetByDatasetId(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");
          assertNotNull(createdId);

          when(context.pathParam("dataset_id")).thenReturn("7cdaaad8-1123-42c4-9a0b-a648a23d637b");
          basketManager.deleteBasketDatasetByDatasetId(context);

          vertx.setTimer(800, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            assertEquals(204, statusCode, "Expected status code to be 200");

            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);

  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteBasketDatasetByDistributionId(Vertx vertx, VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("dataset_id", "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");
    JsonObject document2 = new JsonObject().put("dataset_id",
        "7cdaaad8-1123-42c4-9a0b-a648a23d637b")
      .put("distribution_id", "18054bf7-09a5-41be-9f5c-f465f6c07a1b");

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(document);

    basketManager.createOrUpdateBasketDataset(context)
      .onSuccess(result -> {

        vertx.setTimer(800, timerId -> {
          assertNotNull(result);
          assertEquals("Basket dataset created", result);
          HttpServerResponse response = context.response();
          JsonObject createdResponse = new JsonObject(response.getStatusMessage());
          String createdId = createdResponse.getString("id");
          assertNotNull(createdId);

          when(context.body().asJsonObject()).thenReturn(document2);
          basketManager.deleteDistributionByDistributionId(context);

          vertx.setTimer(1500, timerId2 -> {
            HttpServerResponse response2 = context.response();
            assertNotNull(response2, "Response should not be null");
            int statusCode = response2.getStatusCode();
            assertEquals(200, statusCode, "Expected status code to be 200");

            testContext.completeNow();
          });
        });

      })
      .onFailure(testContext::failNow);

  }

}
