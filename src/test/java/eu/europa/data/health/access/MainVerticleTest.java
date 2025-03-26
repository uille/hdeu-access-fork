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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.containers.MongoDBContainer;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  static MongoDBContainer mongoDBContainer;
  static String mongoUri;
  private MainVerticle mainVerticle;
  private Promise<Void> startPromise;

  @BeforeAll
  static void startMongoDB() {
    // Start MongoDB container
    mongoDBContainer = new MongoDBContainer("mongo:4.4.2")
      .withNetworkAliases("mongodb")
      .withExposedPorts(27017)
      .withCommand("--replSet rs0");

    mongoDBContainer.start();

    mongoUri = "mongodb://" + mongoDBContainer.getHost() + ":" + mongoDBContainer.getMappedPort(27017);
    System.out.println("MongoDB URI: " + mongoUri);
  }

  @BeforeEach
  void setUp() {
    mainVerticle = Mockito.spy(new MainVerticle());
    startPromise = Promise.promise();
  }

  @AfterAll
  static void stopMongoDB() {
    // Stop the container after all tests
    mongoDBContainer.stop();
  }


  //TODO need to fix
  //@Test
  void testVerticleDeployment(Vertx vertx, VertxTestContext testContext) {
    JsonObject testConfig = new JsonObject()
        .put(Constants.ENV_MONGO_DB_URI, mongoUri)
        .put(Constants.ENV_MONGO_DB, "test-db")
        .put(Constants.PIVEAU_STORE_API_KEY, "test-PIVEAU_STORE_API_KEY")
        .put(Constants.DISPATCHER_URL, "test-DISPATCHER_URL")
        .put(Constants.DISPATCHER_X_API_KEY, "test-DISPATCHER_X_API_KEY")
        .put(Constants.HUB_STORE_UPLOAD_DATA_URL, "test-HUB_STORE_UPLOAD_DATA_URL")
        .put(Constants.BEARER_AUTH, "test-BEARER_AUTH")
        .put(Constants.EXPORT_DATA_ACCESS_API_KEY, "test-EXPORT_DATA_ACCESS_API_KEY")
        .put(Constants.ENV_HEALTH_DATA_ACCESS_API_KEY, "test-ENV_HEALTH_DATA_ACCESS_API_KEY");

    vertx.deployVerticle(new MainVerticle(), new DeploymentOptions().setConfig(testConfig),
      testContext.succeeding(id -> {
        assertFalse(vertx.deploymentIDs().isEmpty(),
          "Expected at least one verticle to be deployed.");
        testContext.completeNow();
      }));
  }

//  @Test
//  void testStartServer_Success() {
//    // Mock configuration
//    JsonObject mockConfig = new JsonObject();
//    mockConfig.put(Constants.ENV_PIVEAU_ACCESS_AUTH_CONFIG, new JsonObject());
//    verticle.config = mockConfig;
//
//    // Mock PiveauAuth creation
//    PiveauAuth mockPiveauAuth = mock(PiveauAuth.class);
//    doReturn(Future.succeededFuture(mockPiveauAuth))
//      .when(PiveauAuth.class);
//
//    // Mock RouterBuilder
//    RouterBuilder mockRouterBuilder = mock(RouterBuilder.class);
//    doReturn(Future.succeededFuture(mockRouterBuilder))
//      .when(RouterBuilder.class);
//
//    // Mock HTTP server
//    HttpServer mockHttpServer = mock(HttpServer.class);
//    doReturn(mockHttpServer).when(vertx).createHttpServer(any());
//    doReturn(Future.succeededFuture()).when(mockHttpServer).listen();
//
//    // Call the method under test
//    Future<Void> result = verticle.startServer();
//
//    // Verify interactions and assertions
//    assertTrue(result.succeeded(), "Server should start successfully");
//    verify(mockRouterBuilder, times(1)).createRouter();
//    verify(mockHttpServer, times(1)).listen();
//  }
//
//  @Test
//  void testStartServer_PiveauAuthFailure() {
//    // Mock failure in PiveauAuth creation
//    doReturn(Future.failedFuture(new RuntimeException("PiveauAuth failure")))
//      .when(PiveauAuth.class);
//
//    // Call the method under test
//    Future<Void> result = verticle.startServer();
//
//    // Assertions
//    assertTrue(result.failed(), "Promise should fail when PiveauAuth creation fails");
//    assertEquals("PiveauAuth failure", result.cause().getMessage());
//  }
//
//  @Test
//  void testStartServer_RouterBuilderFailure() {
//    // Mock PiveauAuth creation
//    PiveauAuth mockPiveauAuth = mock(PiveauAuth.class);
//    doReturn(Future.succeededFuture(mockPiveauAuth))
//      .when(PiveauAuth.class);
//
//    // Mock failure in RouterBuilder
//    doReturn(Future.failedFuture(new RuntimeException("RouterBuilder failure")))
//      .when(RouterBuilder.class);
//
//    // Call the method under test
//    Future<Void> result = verticle.startServer();
//
//    // Assertions
//    assertTrue(result.failed(), "Promise should fail when RouterBuilder initialization fails");
//    assertEquals("RouterBuilder failure", result.cause().getMessage());
//  }
//
//  @Test
//  void testStartServer_HttpServerFailure() {
//    // Mock PiveauAuth creation
//    PiveauAuth mockPiveauAuth = mock(PiveauAuth.class);
//    doReturn(Future.succeededFuture(mockPiveauAuth))
//      .when(PiveauAuth.class);
//
//    // Mock RouterBuilder
//    RouterBuilder mockRouterBuilder = mock(RouterBuilder.class);
//    doReturn(Future.succeededFuture(mockRouterBuilder))
//      .when(RouterBuilder.class);
//
//    // Mock HTTP server startup failure
//    HttpServer mockHttpServer = mock(HttpServer.class);
//    doReturn(mockHttpServer).when(vertx).createHttpServer(any());
//    doReturn(Future.failedFuture(new RuntimeException("HTTP server failure")))
//      .when(mockHttpServer).listen();
//
//    // Call the method under test
//    Future<Void> result = verticle.startServer();
//
//    // Assertions
//    assertTrue(result.failed(), "Promise should fail when HTTP server startup fails");
//    assertEquals("HTTP server failure", result.cause().getMessage());
//  }

  @Test
  void testStart_Success() throws Exception {
    doReturn(Future.succeededFuture()).when(mainVerticle).loadConfig();
    doReturn(Future.succeededFuture()).when(mainVerticle).setupMongoDBConnection();
    doReturn(Future.succeededFuture()).when(mainVerticle).startServer();

    mainVerticle.start(startPromise);

    verify(mainVerticle).loadConfig();
    verify(mainVerticle).setupMongoDBConnection();
    verify(mainVerticle).startServer();
    assertTrue(startPromise.future().succeeded());
  }

  @Test
  void testStart_FailureInLoadConfig() throws Exception {
    doReturn(Future.failedFuture(new RuntimeException("Config load failed"))).when(mainVerticle)
      .loadConfig();

    mainVerticle.start(startPromise);

    verify(mainVerticle).loadConfig();
    verify(mainVerticle, never()).setupMongoDBConnection();
    verify(mainVerticle, never()).startServer();
    assertTrue(startPromise.future().failed());
    assertEquals("Config load failed", startPromise.future().cause().getMessage());
  }

  @Test
  void testStart_FailureInSetupMongoDBConnection() throws Exception {
    doReturn(Future.succeededFuture()).when(mainVerticle).loadConfig();
    doReturn(Future.failedFuture(new RuntimeException("MongoDB connection failed")))
      .when(mainVerticle).setupMongoDBConnection();

    mainVerticle.start(startPromise);

    verify(mainVerticle).loadConfig();
    verify(mainVerticle).setupMongoDBConnection();
    verify(mainVerticle, never()).startServer();
    assertTrue(startPromise.future().failed());
    assertEquals("MongoDB connection failed", startPromise.future().cause().getMessage());
  }

  @Test
  void testStart_FailureInStartServer() throws Exception {
    doReturn(Future.succeededFuture()).when(mainVerticle).loadConfig();
    doReturn(Future.succeededFuture()).when(mainVerticle).setupMongoDBConnection();
    doReturn(Future.failedFuture(new RuntimeException("Server failed to start"))).when(mainVerticle)
      .startServer();

    mainVerticle.start(startPromise);

    verify(mainVerticle).loadConfig();
    verify(mainVerticle).setupMongoDBConnection();
    verify(mainVerticle).startServer();
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

}
