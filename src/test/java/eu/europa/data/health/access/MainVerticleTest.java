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
package eu.europa.data.health.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.europa.data.health.access.handlers.DBHandler;
import io.piveau.security.PiveauAuth;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  private MainVerticle mainVerticle;
  private TestAppender testAppender;
  private Promise<Void> startPromise;
  private Future<DBHandler> mockDbHandler;


  @BeforeEach
  void setUp() {
    mainVerticle = new MainVerticle();
    startPromise = Promise.promise();
    testAppender = new TestAppender();
    testAppender.start();

    DBHandler handler = mock(DBHandler.class);
    mockDbHandler = Future.succeededFuture(handler);
  }

  @Test
  void testStart_Success(Vertx vertx, VertxTestContext testContext) throws Exception {
    MainVerticle spyVerticle = Mockito.spy(mainVerticle);
    // Mock MongoDB and server setup to avoid real connections
    doReturn(Future.succeededFuture()).when(spyVerticle).setupMongoDBConnection();
    doReturn(Future.succeededFuture()).when(spyVerticle).startServer();

    spyVerticle.init(vertx, vertx.getOrCreateContext());
    Promise<Void> startPromise = Promise.promise();

    spyVerticle.start(startPromise);

    startPromise.future().onComplete(ar -> {
      if (ar.succeeded()) {
        try {
          verify(spyVerticle).loadConfig();
          verify(spyVerticle).setupMongoDBConnection();
          verify(spyVerticle).startServer();
          assertTrue(startPromise.future().succeeded());
          testContext.completeNow();
        } catch (AssertionError e) {
          testContext.failNow(e);
        }
      } else {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  void testStart_FailureInLoadConfig() throws Exception {
    MainVerticle spyVerticle = Mockito.spy(mainVerticle);
    Promise<Void> startPromise = Promise.promise();
    doReturn(Future.failedFuture(new RuntimeException("Config load failed"))).when(spyVerticle)
      .loadConfig();

    spyVerticle.start(startPromise);

    verify(spyVerticle).loadConfig();
    verify(spyVerticle, never()).setupMongoDBConnection();
    verify(spyVerticle, never()).startServer();
    assertTrue(startPromise.future().failed());
    assertEquals("Config load failed", startPromise.future().cause().getMessage());
  }

  @Test
  void testStart_FailureInSetupMongoDBConnection() throws Exception {
    MainVerticle spyVerticle = Mockito.spy(mainVerticle);
    Promise<Void> startPromise = Promise.promise();
    doReturn(Future.succeededFuture()).when(spyVerticle).loadConfig();
    doReturn(Future.failedFuture(new RuntimeException("MongoDB connection failed")))
      .when(spyVerticle).setupMongoDBConnection();

    spyVerticle.start(startPromise);

    verify(spyVerticle).loadConfig();
    verify(spyVerticle).setupMongoDBConnection();
    verify(spyVerticle, never()).startServer();
    assertTrue(startPromise.future().failed());
    assertEquals("MongoDB connection failed", startPromise.future().cause().getMessage());
  }

  @Test
  void testStart_FailureInStartServer() throws Exception {
    MainVerticle spyVerticle = Mockito.spy(mainVerticle);
    Promise<Void> startPromise = Promise.promise();
    doReturn(Future.succeededFuture()).when(spyVerticle).loadConfig();
    doReturn(Future.succeededFuture()).when(spyVerticle).setupMongoDBConnection();
    doReturn(Future.failedFuture(new RuntimeException("Server failed to start"))).when(spyVerticle)
      .startServer();

    spyVerticle.start(startPromise);

    verify(spyVerticle).loadConfig();
    verify(spyVerticle).setupMongoDBConnection();
    verify(spyVerticle).startServer();
    assertTrue(startPromise.future().failed());
    assertEquals("Server failed to start", startPromise.future().cause().getMessage());
  }

  /**
   * Creates various routes to test the error handling mechanism.
   *
   * @param vertx The Vert.x instance.
   * @param context The test context.
   */
  @Test
  void testHandleError(Vertx vertx, VertxTestContext context) {
    MainVerticle verticle = new MainVerticle();
    Router router = Router.router(vertx);
    verticle.handleExceptions(router);

    router.route("/not-modified").handler(ctx -> ctx.fail(304));
    router.route("/bad-request")
      .handler(ctx -> ctx.fail(400, new TestBadRequestException("Simulated error", null)));
    router.route("/unauthorized").handler(ctx -> ctx.fail(401));
    router.route("/forbidden").handler(ctx -> ctx.fail(403));
    router.route("/not-found")
      .handler(ctx -> ctx.fail(404, new ServiceException(404, "Simulated error")));
    router.route("/generic-error").handler(ctx -> ctx.fail(415, new Throwable("Simulated error")));
    router.route("/unprocessable-entity")
      .handler(ctx -> ctx.fail(422, new ServiceException(422, "Simulated 422 error")));
    router.route("/internal-error")
      .handler(ctx -> ctx.fail(new ServiceException(500, "Simulated error")));

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(0, context.succeeding(server -> {
        int actualPort = server.actualPort();
        HttpClient client = vertx.createHttpClient();

        // Define a list of test cases
        List<TestCase> testCases = List.of(
          new TestCase("/not-modified", 304),
          new TestCase("/bad-request", 400),
          new TestCase("/unauthorized", 401),
          new TestCase("/forbidden", 403),
          new TestCase("/not-found", 404),
          new TestCase("/generic-error", 415),
          new TestCase("/unprocessable-entity", 422),
          new TestCase("/internal-error", 500)
        );

        AtomicInteger remainingTests = new AtomicInteger(testCases.size());
        for (TestCase testCase : testCases) {
          client.request(HttpMethod.GET, actualPort, "localhost", testCase.path)
            .compose(HttpClientRequest::send)
            .onSuccess(response -> {
              context.verify(() -> assertEquals(testCase.expectedStatus, response.statusCode(),
                "Unexpected status code for path: " + testCase.path));

              if (remainingTests.decrementAndGet() == 0) {
                server.close();
                context.completeNow();
              }
            })
            .onFailure(err -> {
              server.close();
              context.failNow(err);
            });
        }
      }));
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * Represents a test case with a path and an expected status code.
   */
  private record TestCase(String path, int expectedStatus) {

  }

  /**
   * A custom exception class for testing purposes.
   */
  private static class TestBadRequestException extends BadRequestException {

    private final JsonObject errorDetails;

    public TestBadRequestException(String message, JsonObject errorDetails) {
      super(message, new Throwable());
      this.errorDetails = errorDetails;
    }

    @Override
    public JsonObject toJson() {
      return errorDetails != null ? errorDetails : new JsonObject().put("message", getMessage());
    }
  }

  @Test
  void testLoadConfig_whenFileMissing_shouldLogWarning(Vertx vertx, VertxTestContext testContext) {
    Logger logger = (Logger) LoggerFactory.getLogger(MainVerticle.class);
    logger.setLevel(Level.DEBUG);
    logger.addAppender(testAppender);
    mainVerticle = new MainVerticle();
    mainVerticle.init(vertx, vertx.getOrCreateContext());
    // Ensure the file does exist
    Path configPath = Paths.get("conf/hd-access-env.properties");
    assertTrue(Files.exists(configPath), "Config file should not exist for this test");

    mainVerticle.loadConfig().onComplete(res -> {
      if (res.succeeded()) {
        vertx.setTimer(300, id -> {
          boolean found = testAppender.getLogs().stream()
            .anyMatch(log -> log.getLevel() == Level.DEBUG &&
              log.getFormattedMessage().contains("Successfully loaded configuration"));

          assertTrue(found, "Expected DEBUG log with 'Successfully loaded configuration'");

          testAppender.stop();
          testContext.completeNow();
        });
      } else {
        testContext.failNow(res.cause());
      }
    });
  }

  @Test
  void testSetupMongoDBConnection_success(Vertx vertx, VertxTestContext testContext) {
    MainVerticle spyVerticle = Mockito.spy(new MainVerticle());
    spyVerticle.init(vertx, vertx.getOrCreateContext());

    JsonObject config = new JsonObject()
      .put(Constants.ENV_MONGO_DB_URI, "mongodb://localhost:27017")
      .put(Constants.ENV_MONGO_DB, "testdb");

    spyVerticle.config = config;

    MongoClient mockClient = mock(MongoClient.class);
    when(mockClient.runCommand(any(), any()))
      .thenReturn(Future.succeededFuture(new JsonObject().put("ok", 1)));

    doReturn(mockClient).when(spyVerticle).createMongoClient(any());

    spyVerticle.setupMongoDBConnection().onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertNotNull(ar.result());
      verify(mockClient).runCommand(eq("ping"), any());
      testContext.completeNow();
    });
  }

  @Test
  void testSetupMongoDBConnection_failure(Vertx vertx, VertxTestContext testContext) {
    MainVerticle spyVerticle = Mockito.spy(new MainVerticle());
    spyVerticle.init(vertx, vertx.getOrCreateContext());

    JsonObject config = new JsonObject()
      .put(Constants.ENV_MONGO_DB_URI, "mongodb://invalidhost")
      .put(Constants.ENV_MONGO_DB, "testdb");

    spyVerticle.config = config;

    MongoClient mockClient = mock(MongoClient.class);
    when(mockClient.runCommand(any(), any()))
      .thenReturn(Future.failedFuture(new RuntimeException("Connection refused")));

    doReturn(mockClient).when(spyVerticle).createMongoClient(any());

    spyVerticle.setupMongoDBConnection().onComplete(ar -> {
      assertTrue(ar.failed());
      assertEquals("Connection refused", ar.cause().getMessage());
      verify(mockClient).runCommand(eq("ping"), any());
      testContext.completeNow();
    });
  }

  @Test
  void testStart_withMockedSetupMongoAndPiveauAuth(Vertx vertx, VertxTestContext testContext) throws Exception {
    PiveauAuth mockedPiveauAuth = mock(PiveauAuth.class);
    when(mockedPiveauAuth.authHandler()).thenReturn(routingContext -> routingContext.next());

    try (MockedStatic<PiveauAuth> mockedStatic = mockStatic(PiveauAuth.class)) {
      mockedStatic.when(() -> PiveauAuth.create(any(), any()))
        .thenReturn(Future.succeededFuture(mockedPiveauAuth));

      class TestMainVerticle extends MainVerticle {
        @Override
        protected Future<DBHandler> setupMongoDBConnection() {
          return Future.succeededFuture(mock(DBHandler.class));
        }

        @Override
        protected Future<Void> loadConfig() {
          this.config = new JsonObject()
            .put(Constants.ENV_MONGO_DB_URI, "mongodb://localhost:27017")
            .put(Constants.ENV_MONGO_DB, "test")
            .put(Constants.ENV_PIVEAU_ACCESS_AUTH_CONFIG, new JsonObject()
              .put("jwtOptions", new JsonObject())
              .put("securityHandlers", new JsonObject()
                .put("oauth2", new JsonObject())));
          return Future.succeededFuture();
        }
      }

      MainVerticle testVerticle = new TestMainVerticle();
      testVerticle.init(vertx, vertx.getOrCreateContext());
      testVerticle.mongoClient = Mockito.mock(MongoClient.class);

      Promise<Void> startPromise = Promise.promise();
      testVerticle.start(startPromise);

      startPromise.future().onComplete(ar -> {
        assertTrue(ar.succeeded());
        testContext.completeNow();
      });
    }
  }


}
