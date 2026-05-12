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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.scheduled_jobs.DataAccessPermitJob;
import eu.europa.data.health.access.scheduled_jobs.DispatcherScheduledJob;
import eu.europa.data.health.access.scheduled_jobs.NotificationScheduledJob;
import eu.europa.data.health.access.scheduled_jobs.TranslationRetryScheduledJob;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.ControlledVocabularyManager;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import eu.europa.data.health.access.services.DecisionTranslationService;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.LockWebSocketManager;
import eu.europa.data.health.access.services.NotificationManager;
import eu.europa.data.health.access.services.SectionLockManager;
import io.piveau.security.PiveauAuth;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  private MainVerticle mainVerticle;
  private TestAppender testAppender;
  private AutoCloseable mocks;

  @Mock
  private MongoClient mongo;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    mainVerticle = new MainVerticle();
    testAppender = new TestAppender();
    testAppender.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
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

    spyVerticle.config = new JsonObject()
      .put(Constants.ENV_MONGO_DB_URI, "mongodb://localhost:27017")
      .put(Constants.ENV_MONGO_DB, "testdb");

    when(mongo.runCommand(any(), any())).thenReturn(
      Future.succeededFuture(new JsonObject().put("ok", 1))
    );
    when(mongo.createIndexWithOptions(any(), any(), any(IndexOptions.class))).thenReturn(
      Future.succeededFuture()
    );
    when(mongo.dropIndex(any(), any())).thenReturn(
      Future.succeededFuture()
    );

    doReturn(mongo).when(spyVerticle).createMongoClient(any());

    spyVerticle
      .setupMongoDBConnection()
      .onComplete(ar -> {
        assertTrue(ar.succeeded());
        assertNotNull(ar.result());
        verify(mongo).runCommand(eq("ping"), any());
        testContext.completeNow();
      });
  }

  @Test
  void testSetupMongoDBConnection_failure(Vertx vertx, VertxTestContext testContext) {
    MainVerticle spyVerticle = Mockito.spy(new MainVerticle());
    spyVerticle.init(vertx, vertx.getOrCreateContext());

    spyVerticle.config = new JsonObject()
      .put(Constants.ENV_MONGO_DB_URI, "mongodb://invalidhost")
      .put(Constants.ENV_MONGO_DB, "testdb");

    when(mongo.runCommand(any(), any())).thenReturn(
      Future.failedFuture(new RuntimeException("Connection refused"))
    );

    doReturn(mongo).when(spyVerticle).createMongoClient(any());

    spyVerticle
      .setupMongoDBConnection()
      .onComplete(ar -> {
        assertTrue(ar.failed());
        assertEquals("Connection refused", ar.cause().getMessage());
        verify(mongo).runCommand(eq("ping"), any());
        testContext.completeNow();
      });
  }

  @Test
  void testStart_withMockedSetupMongoAndPiveauAuth(Vertx vertx, VertxTestContext testContext) throws Exception {
    PiveauAuth mockedPiveauAuth = mock(PiveauAuth.class);
    when(mockedPiveauAuth.authHandler()).thenReturn(RoutingContext::next);
    when(mongo.createIndexWithOptions(any(), any(), any(IndexOptions.class))).thenReturn(
      Future.succeededFuture()
    );
    when(mongo.dropIndex(any(), any())).thenReturn(
      Future.succeededFuture()
    );

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
                .put("oauth2", new JsonObject())))
            .put(Constants.WITHDRAW_RUN_TIME, "06:00")
            .put(Constants.WITHDRAW_THRESHOLD_DAYS, 28);
          return Future.succeededFuture();
        }
      }

      MainVerticle testVerticle = new TestMainVerticle();
      testVerticle.init(vertx, vertx.getOrCreateContext());
      testVerticle.mongoClient = mongo;

      Promise<Void> startPromise = Promise.promise();
      testVerticle.start(startPromise);

      startPromise.future().onComplete(ar -> {
        assertTrue(ar.succeeded());
        testContext.completeNow();
      });
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  void testHandleLockWebSocket_rejectsUnexpectedPath(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    ServerWebSocket webSocket = mock(ServerWebSocket.class);
    LockWebSocketManager wsManager = mock(LockWebSocketManager.class);
    SectionLockManager lockManager = mock(SectionLockManager.class);

    when(webSocket.path()).thenReturn("/api/ws/invalid");

    verticle.handleLockWebSocket(webSocket, wsManager, lockManager);

    verify(webSocket).reject(404);
    verify(webSocket, never()).accept();
    verify(wsManager, never()).register(any(), any(), any());
    verify(lockManager, never()).releaseLocksForSession(any(), any(), any());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testHandleLockWebSocket_rejectsUnknownApplicationType(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    ServerWebSocket webSocket = mock(ServerWebSocket.class);
    LockWebSocketManager wsManager = mock(LockWebSocketManager.class);
    SectionLockManager lockManager = mock(SectionLockManager.class);

    when(webSocket.path()).thenReturn("/api/ws/applications/unknown/123/locks");

    verticle.handleLockWebSocket(webSocket, wsManager, lockManager);

    verify(webSocket).reject(400);
    verify(webSocket, never()).accept();
    verify(wsManager, never()).register(any(), any(), any());
    verify(lockManager, never()).releaseLocksForSession(any(), any(), any());
  }

  @Test
  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  void testHandleLockWebSocket_registersAndCleansUpOnlyOnce(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    ServerWebSocket webSocket = mock(ServerWebSocket.class);
    LockWebSocketManager wsManager = mock(LockWebSocketManager.class);
    SectionLockManager lockManager = mock(SectionLockManager.class);

    when(webSocket.path()).thenReturn(
      "/api/ws/applications/" +
        Constants.DATA_ACCESS_APPLICATION_COLLECTION +
        "/application-1/locks"
    );
    when(webSocket.query()).thenReturn("sessionId=session-123&other=value");
    when(webSocket.closeHandler(any())).thenReturn(webSocket);
    when(webSocket.exceptionHandler(any())).thenReturn(webSocket);
    when(webSocket.textMessageHandler(any())).thenReturn(webSocket);

    verticle.handleLockWebSocket(webSocket, wsManager, lockManager);

    verify(webSocket).accept();
    verify(wsManager).register(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "application-1",
      webSocket
    );

    ArgumentCaptor<Handler> closeCaptor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(webSocket).closeHandler(closeCaptor.capture());
    verify(webSocket).exceptionHandler(exceptionCaptor.capture());

    closeCaptor.getValue().handle(null);
    exceptionCaptor.getValue().handle(new RuntimeException("socket failure"));

    verify(wsManager).unregister(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "application-1",
      webSocket
    );
    verify(lockManager).releaseLocksForSession(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "application-1",
      "session-123"
    );
    verify(wsManager, times(1)).unregister(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "application-1",
      webSocket
    );
    verify(lockManager, times(1)).releaseLocksForSession(
      Constants.DATA_ACCESS_APPLICATION_COLLECTION,
      "application-1",
      "session-123"
    );
  }

  @Test
  void testGetApiKeys_returnsConfiguredKey() {
    mainVerticle.config = new JsonObject().put(Constants.ENV_HEALTH_DATA_ACCESS_API_KEY, "api-key");

    Map<String, List<String>> apiKeys = mainVerticle.getApiKeys(
      Constants.ENV_HEALTH_DATA_ACCESS_API_KEY
    );

    assertEquals(Map.of("api-key", List.of("*")), apiKeys);
  }

  @Test
  void testParseDaysToSet_handlesConfiguredAndInvalidValues() {
    assertEquals(Set.of(1, 2, 15), mainVerticle.parseDaysToSet("[1, 2,15]"));
    assertTrue(mainVerticle.parseDaysToSet("bad-value").isEmpty());
  }

  @Test
  void testParseArrayOfStringsFromConstantsOrConfig_prefersConfigValues() {
    mainVerticle.config = new JsonObject().put("test.values", " one, two ,, three ");

    List<String> values = mainVerticle.parseArrayOfStringsFromConstantsOrConfig(
      "test.values",
      "default"
    );

    assertEquals(List.of("one", "two", "three"), values);
  }

  @Test
  void testFlushExpiredIntrospectionCacheEntries_removesOnlyExpiredEntries() {
    mainVerticle.introspectionCache().put("expired", System.currentTimeMillis() - 1);
    mainVerticle.introspectionCache().put("valid", System.currentTimeMillis() + 10_000);

    mainVerticle.flushExpiredIntrospectionCacheEntries();

    assertFalse(mainVerticle.introspectionCache().containsKey("expired"));
    assertTrue(mainVerticle.introspectionCache().containsKey("valid"));
  }

  @Test
  void testHandleLockWebSocketPing_cancelsClosedSocketTimer() {
    MainVerticle verticle = new MainVerticle();
    Vertx mockedVertx = mock(Vertx.class);
    verticle.init(mockedVertx, mock(Context.class));
    ServerWebSocket webSocket = mock(ServerWebSocket.class);
    when(webSocket.isClosed()).thenReturn(true);

    verticle.handleLockWebSocketPing(webSocket, "type", "id", 42L);

    verify(mockedVertx).cancelTimer(42L);
    verify(webSocket, never()).writePing(any(Buffer.class));
  }

  @Test
  void testHandleLockWebSocketPing_sendsPingForOpenSocket() {
    MainVerticle verticle = new MainVerticle();
    Vertx mockedVertx = mock(Vertx.class);
    verticle.init(mockedVertx, mock(Context.class));
    ServerWebSocket webSocket = mock(ServerWebSocket.class);
    when(webSocket.isClosed()).thenReturn(false);

    verticle.handleLockWebSocketPing(webSocket, "type", "id", 42L);

    verify(webSocket).writePing(any(Buffer.class));
    verify(mockedVertx, never()).cancelTimer(any(Long.class));
  }

  @Test
  void testSendOutgoingApplicationActivities_callsDispatcherJob() {
    DispatcherScheduledJob scheduledJob = mock(DispatcherScheduledJob.class);
    eu.europa.data.health.access.dispatcher_client.DispatcherClient dispatcherClient = mock(
      eu.europa.data.health.access.dispatcher_client.DispatcherClient.class
    );

    mainVerticle.sendOutgoingApplicationActivities(scheduledJob, dispatcherClient, 15L);

    verify(scheduledJob).sendOutgoingApplicationActivitiesToDispatcher(
      Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION,
      dispatcherClient
    );
  }

  @Test
  void testTriggerDecisionTranslationJob_runsAndResetsFlag() {
    DecisionTranslationService decisionTranslationService = mock(DecisionTranslationService.class);
    AtomicBoolean isRunning = new AtomicBoolean(false);
    Promise<Void> promise = Promise.promise();
    when(decisionTranslationService.triggerPendingTranslations()).thenReturn(promise.future());

    mainVerticle.triggerDecisionTranslationJob(decisionTranslationService, isRunning);
    assertTrue(isRunning.get());

    promise.complete();

    assertFalse(isRunning.get());
    verify(decisionTranslationService).triggerPendingTranslations();
  }

  @Test
  void testHandlePostRequestDataFailure_writesUploadFailedResponse() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.statusCode()).thenReturn(400);
    when(context.failure()).thenReturn(new RuntimeException("upload failed"));
    when(response.ended()).thenReturn(false);
    when(response.setStatusCode(400)).thenReturn(response);

    mainVerticle.handlePostRequestDataFailure(context);

    verify(response).setStatusCode(400);
    verify(response).end("Upload failed");
  }

  @Test
  void testHandlePostRequestDataFailure_skipsResponseWhenAlreadyEnded() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.statusCode()).thenReturn(-1);
    when(context.failure()).thenReturn(null);
    when(response.ended()).thenReturn(true);

    mainVerticle.handlePostRequestDataFailure(context);

    verify(response, never()).setStatusCode(anyInt());
    verify(response, never()).end(anyString());
  }

  @Test
  void testCreateAuthTokenHandler_withoutCookie_callsNext() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(null);
    when(request.absoluteURI()).thenReturn("http://localhost/no-cookie");

    mainVerticle.createAuthTokenHandler(new JsonObject(), mock(PiveauAuth.class)).handle(context);

    verify(context).next();
  }

  @Test
  void testCreateAuthTokenHandler_whenAuthenticationFails_returnsUnauthorized() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    PiveauAuth piveauAuth = mock(PiveauAuth.class);
    RuntimeException error = new RuntimeException("auth failed");
    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(Cookie.cookie("authToken", "token"));
    when(request.absoluteURI()).thenReturn("http://localhost/auth-fail");
    when(piveauAuth.authenticate("token")).thenReturn(Future.failedFuture(error));

    mainVerticle.createAuthTokenHandler(minimalAuthConfig(), piveauAuth).handle(context);

    verify(context).fail(401, error);
  }

  @Test
  void testCreateAuthTokenHandler_withCachedToken_skipsIntrospection(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    WebClient webClient = mock(WebClient.class);
    PiveauAuth piveauAuth = mock(PiveauAuth.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    User user = mock(User.class);
    HeadersMultiMap headers = new HeadersMultiMap();

    verticle.setWebClient(webClient);
    verticle.config = new JsonObject().put(Constants.ENV_KEYCLOAK_INTROSPECT_PATH, "/introspect");
    verticle.introspectionCache().put("token", System.currentTimeMillis() + 10_000);

    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(Cookie.cookie("authToken", "token"));
    when(request.absoluteURI()).thenReturn("http://localhost/cache-hit");
    when(request.headers()).thenReturn(headers);
    when(user.principal()).thenReturn(new JsonObject().put("preferred_username", "ecas-user"));
    when(piveauAuth.authenticate("token")).thenReturn(Future.succeededFuture(user));

    verticle.createAuthTokenHandler(minimalAuthConfig(), piveauAuth).handle(context);

    assertEquals("Bearer token", headers.get("Authorization"));
    verify(context).setUser(user);
    verify(context).next();
    verify(webClient, never()).postAbs(anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCreateAuthTokenHandler_withActiveIntrospection_setsUserAndCachesToken(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    WebClient webClient = mock(WebClient.class);
    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    HttpResponse<Buffer> httpResponse = mock(HttpResponse.class);
    io.vertx.core.AsyncResult<HttpResponse<Buffer>> asyncResult = mock(
      io.vertx.core.AsyncResult.class
    );
    PiveauAuth piveauAuth = mock(PiveauAuth.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    User user = mock(User.class);
    HeadersMultiMap headers = new HeadersMultiMap();

    verticle.setWebClient(webClient);
    verticle.config = new JsonObject().put(Constants.ENV_KEYCLOAK_INTROSPECT_PATH, "/introspect");

    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(Cookie.cookie("authToken", "token"));
    when(request.absoluteURI()).thenReturn("http://localhost/introspect");
    when(request.headers()).thenReturn(headers);
    when(user.principal()).thenReturn(new JsonObject().put("preferred_username", "ecas-user"));
    when(piveauAuth.authenticate("token")).thenReturn(Future.succeededFuture(user));
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonObject()).thenReturn(new JsonObject().put("active", true));

    Mockito.doAnswer(invocation -> {
        Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
        handler.handle(asyncResult);
        return null;
      })
      .when(httpRequest)
      .sendForm(
        any(io.vertx.core.MultiMap.class),
        Mockito.<Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>>>any()
      );

    verticle.createAuthTokenHandler(minimalAuthConfig(), piveauAuth).handle(context);

    verify(context).setUser(user);
    verify(context).next();
    assertEquals("Bearer token", headers.get("Authorization"));
    assertTrue(verticle.introspectionCache().containsKey("token"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCreateAuthTokenHandler_withInactiveIntrospection_returnsUnauthorized(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    WebClient webClient = mock(WebClient.class);
    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    HttpResponse<Buffer> httpResponse = mock(HttpResponse.class);
    io.vertx.core.AsyncResult<HttpResponse<Buffer>> asyncResult = mock(
      io.vertx.core.AsyncResult.class
    );
    PiveauAuth piveauAuth = mock(PiveauAuth.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    User user = mock(User.class);

    verticle.setWebClient(webClient);
    verticle.config = new JsonObject().put(Constants.ENV_KEYCLOAK_INTROSPECT_PATH, "/introspect");
    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(Cookie.cookie("authToken", "token"));
    when(request.absoluteURI()).thenReturn("http://localhost/inactive");
    when(request.headers()).thenReturn(new HeadersMultiMap());
    when(user.principal()).thenReturn(new JsonObject().put("preferred_username", "ecas-user"));
    when(piveauAuth.authenticate("token")).thenReturn(Future.succeededFuture(user));
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonObject()).thenReturn(new JsonObject().put("active", false));

    Mockito.doAnswer(invocation -> {
        Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
        handler.handle(asyncResult);
        return null;
      })
      .when(httpRequest)
      .sendForm(
        any(io.vertx.core.MultiMap.class),
        Mockito.<Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>>>any()
      );

    verticle.createAuthTokenHandler(minimalAuthConfig(), piveauAuth).handle(context);

    verify(context).fail(401);
    assertFalse(verticle.introspectionCache().containsKey("token"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCreateAuthTokenHandler_withNon200Introspection_returnsUnauthorized(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    WebClient webClient = mock(WebClient.class);
    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    HttpResponse<Buffer> httpResponse = mock(HttpResponse.class);
    io.vertx.core.AsyncResult<HttpResponse<Buffer>> asyncResult = mock(
      io.vertx.core.AsyncResult.class
    );
    PiveauAuth piveauAuth = mock(PiveauAuth.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    User user = mock(User.class);

    verticle.setWebClient(webClient);
    verticle.config = new JsonObject().put(Constants.ENV_KEYCLOAK_INTROSPECT_PATH, "/introspect");

    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(Cookie.cookie("authToken", "token"));
    when(request.absoluteURI()).thenReturn("http://localhost/non-200");
    when(request.headers()).thenReturn(new HeadersMultiMap());
    when(user.principal()).thenReturn(new JsonObject().put("preferred_username", "ecas-user"));
    when(piveauAuth.authenticate("token")).thenReturn(Future.succeededFuture(user));
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.bodyAsString()).thenReturn("failure");

    Mockito.doAnswer(invocation -> {
        Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
        handler.handle(asyncResult);
        return null;
      })
      .when(httpRequest)
      .sendForm(
        any(io.vertx.core.MultiMap.class),
        Mockito.<Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>>>any()
      );

    verticle.createAuthTokenHandler(minimalAuthConfig(), piveauAuth).handle(context);

    verify(context).fail(401);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCreateAuthTokenHandler_whenIntrospectionRequestFails_returnsUnauthorized(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    WebClient webClient = mock(WebClient.class);
    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    io.vertx.core.AsyncResult<HttpResponse<Buffer>> asyncResult = mock(
      io.vertx.core.AsyncResult.class
    );
    PiveauAuth piveauAuth = mock(PiveauAuth.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    User user = mock(User.class);
    RuntimeException error = new RuntimeException("introspection failed");

    verticle.setWebClient(webClient);
    verticle.config = new JsonObject().put(Constants.ENV_KEYCLOAK_INTROSPECT_PATH, "/introspect");

    when(context.request()).thenReturn(request);
    when(request.getCookie("authToken")).thenReturn(Cookie.cookie("authToken", "token"));
    when(request.absoluteURI()).thenReturn("http://localhost/request-fail");
    when(request.headers()).thenReturn(new HeadersMultiMap());
    when(user.principal()).thenReturn(new JsonObject().put("preferred_username", "ecas-user"));
    when(piveauAuth.authenticate("token")).thenReturn(Future.succeededFuture(user));
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(error);

    Mockito.doAnswer(invocation -> {
        Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
        handler.handle(asyncResult);
        return null;
      })
      .when(httpRequest)
      .sendForm(
        any(io.vertx.core.MultiMap.class),
        Mockito.<Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>>>any()
      );

    verticle.createAuthTokenHandler(minimalAuthConfig(), piveauAuth).handle(context);

    verify(context).fail(401);
  }

  @Test
  void testHandleErrors_additionalBranches(Vertx vertx, VertxTestContext testContext) {
    MainVerticle verticle = new MainVerticle();
    Router router = Router.router(vertx);
    verticle.handleExceptions(router);

    ServiceException notFoundDebug = mock(ServiceException.class);
    when(notFoundDebug.getDebugInfo()).thenReturn(new JsonObject().put("id", "missing"));
    when(notFoundDebug.getMessage()).thenReturn("Not found");

    ServiceException serverErrorDebug = mock(ServiceException.class);
    when(serverErrorDebug.getDebugInfo()).thenReturn(new JsonObject().put("error", "debug"));
    when(serverErrorDebug.getMessage()).thenReturn("Server debug message");

    ServiceException serverErrorMessage = mock(ServiceException.class);
    when(serverErrorMessage.getDebugInfo()).thenReturn(null);
    when(serverErrorMessage.getMessage()).thenReturn("Server message");

    router
      .route("/redirection-service")
      .handler(ctx -> ctx.fail(304, new ServiceException(304, "Redirect body")));
    router
      .route("/client-error-service")
      .handler(ctx -> ctx.fail(400, new ServiceException(400, "Client service")));
    router.route("/client-error-empty").handler(ctx -> ctx.fail(400));
    router.route("/not-found-debug").handler(ctx -> ctx.fail(404, notFoundDebug));
    router
      .route("/not-found-generic")
      .handler(ctx -> ctx.fail(404, new Throwable("Not found generic")));
    router
      .route("/unsupported-service")
      .handler(ctx -> ctx.fail(415, new ServiceException(415, "Unsupported body")));
    router
      .route("/unprocessable-generic")
      .handler(ctx -> ctx.fail(422, new Throwable("unprocessable")));
    router.route("/server-error-debug").handler(ctx -> ctx.fail(500, serverErrorDebug));
    router.route("/server-error-message").handler(ctx -> ctx.fail(500, serverErrorMessage));
    router.route("/server-error-generic").handler(ctx -> ctx.fail(500, new Throwable("generic")));

    List<ResponseExpectation> expectations = List.of(
      new ResponseExpectation("/redirection-service", 304, ""),
      new ResponseExpectation("/client-error-service", 400, "Client service"),
      new ResponseExpectation("/client-error-empty", 400, ""),
      new ResponseExpectation("/not-found-debug", 404, "id"),
      new ResponseExpectation("/not-found-generic", 404, "Not found generic"),
      new ResponseExpectation("/unsupported-service", 415, "Unsupported body"),
      new ResponseExpectation("/unprocessable-generic", 422, ""),
      new ResponseExpectation("/server-error-debug", 500, "error"),
      new ResponseExpectation("/server-error-message", 500, "Server message"),
      new ResponseExpectation("/server-error-generic", 500, "")
    );

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(
        0,
        testContext.succeeding(server -> {
          HttpClient client = vertx.createHttpClient();
          AtomicInteger remaining = new AtomicInteger(expectations.size());

          for (ResponseExpectation expectation : expectations) {
            client
              .request(HttpMethod.GET, server.actualPort(), "localhost", expectation.path())
              .compose(HttpClientRequest::send)
              .onSuccess(response ->
                response
                  .body()
                  .onSuccess(body ->
                    testContext.verify(() -> {
                      assertEquals(expectation.statusCode(), response.statusCode());
                      assertTrue(body.toString().contains(expectation.bodyFragment()));
                      if (remaining.decrementAndGet() == 0) {
                        server.close();
                        testContext.completeNow();
                      }
                    })
                  )
                  .onFailure(err -> {
                    server.close();
                    testContext.failNow(err);
                  })
              )
              .onFailure(err -> {
                server.close();
                testContext.failNow(err);
              });
          }
        })
      );
  }

  @Test
  void testLoadConfig_whenConfigFileMissing_logsWarningAndLoadsEnvConfig(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    class MissingConfigVerticle extends MainVerticle {

      @Override
      String getConfigPath() {
        return "conf/missing-test.properties";
      }
    }

    Logger logger = (Logger) LoggerFactory.getLogger(MissingConfigVerticle.class);
    logger.setLevel(Level.DEBUG);
    logger.addAppender(testAppender);

    MainVerticle verticle = new MissingConfigVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());

    verticle
      .loadConfig()
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
          return;
        }
        testContext.verify(() ->
          assertTrue(
            testAppender
              .getLogs()
              .stream()
              .anyMatch(log -> log.getFormattedMessage().contains("Configuration file not found"))
          )
        );
        testContext.completeNow();
      });
  }

  @Test
  void testLoadConfig_whenRetrieverFails_returnsFailure(Vertx vertx) {
    ConfigRetriever retriever = mock(ConfigRetriever.class);

    class FailingConfigVerticle extends MainVerticle {

      @Override
      ConfigRetriever createConfigRetriever(io.vertx.config.ConfigRetrieverOptions options) {
        return retriever;
      }
    }

    MainVerticle verticle = new FailingConfigVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());

    Mockito.doAnswer(invocation -> {
        Handler<io.vertx.core.AsyncResult<JsonObject>> handler = invocation.getArgument(0);
        handler.handle(Future.failedFuture("config-failed"));
        return null;
      })
      .when(retriever)
      .getConfig(any());

    Future<Void> result = verticle.loadConfig();

    assertTrue(result.failed());
    assertTrue(result.cause().getMessage().contains("Failed to load config"));
  }

  @Test
  void testSetupMongoDBConnection_whenMongoClientBecomesNull_returnsFailure(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    MainVerticle spyVerticle = Mockito.spy(new MainVerticle());
    Promise<JsonObject> pingPromise = Promise.promise();
    MongoClient mockedMongoClient = mock(MongoClient.class);

    spyVerticle.init(vertx, vertx.getOrCreateContext());
    spyVerticle.config = new JsonObject()
      .put(Constants.ENV_MONGO_DB_URI, "mongodb://localhost:27017")
      .put(Constants.ENV_MONGO_DB, "testdb");

    when(mockedMongoClient.runCommand(any(), any())).thenReturn(pingPromise.future());
    doReturn(mockedMongoClient).when(spyVerticle).createMongoClient(any());

    Future<DBHandler> result = spyVerticle.setupMongoDBConnection();
    spyVerticle.mongoClient = null;
    pingPromise.complete(new JsonObject().put("ok", 1));

    result.onComplete(ar -> {
      assertTrue(ar.failed());
      assertEquals("No DB-Connection.", ar.cause().getMessage());
      testContext.completeNow();
    });
  }

  @Test
  void testCreateMongoClient_returnsClient(Vertx vertx) {
    MainVerticle verticle = new MainVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());

    MongoClient client = verticle.createMongoClient(
      new JsonObject().put("connection_string", "mongodb://localhost:27017").put("db_name", "test")
    );

    assertNotNull(client);
    client.close();
  }

  @Test
  void testGetCountriesModel_whenStreamCloseFails_returnsNull() {
    class CountriesModelVerticle extends MainVerticle {

      @Override
      InputStream openCountriesRdfStream() throws IOException {
        throw new IOException("open failed");
      }
    }

    assertNull(new CountriesModelVerticle().getCountriesModel());
  }

  @Test
  void testGetCountries_returnsJsonArray() {
    CountriesManager countriesManager = mock(CountriesManager.class);
    Model countriesModel = mock(Model.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    JsonArray countries = new JsonArray().add(new JsonObject().put("code", "BE"));

    mainVerticle.setCountriesManager(countriesManager);
    mainVerticle.setCountriesModel(countriesModel);
    when(context.pathParam("lg")).thenReturn("en");
    when(context.pathParam("filter")).thenReturn("euCountry");
    when(context.response()).thenReturn(response);
    when(countriesManager.getCountriesJsonArray(countriesModel, "en", "euCountry")).thenReturn(
      countries
    );
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(200)).thenReturn(response);

    mainVerticle.getCountries(context);

    verify(response).end(countries.encode());
  }

  @Test
  void testGetCountriesTranslation_whenModelMissing_returnsBadRequest() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    mainVerticle.setCountriesModel(null);
    when(context.pathParam("lg")).thenReturn("en");
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);

    mainVerticle.getCountriesTranslation(context);

    verify(response).end("Countries model is null.");
  }

  @Test
  void testGetCountriesTranslation_returnsJsonArray() {
    CountriesManager countriesManager = mock(CountriesManager.class);
    Model countriesModel = mock(Model.class);
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    JsonArray countries = new JsonArray().add(new JsonObject().put("code", "BE"));

    mainVerticle.setCountriesManager(countriesManager);
    mainVerticle.setCountriesModel(countriesModel);
    when(context.pathParam("lg")).thenReturn("fr");
    when(context.response()).thenReturn(response);
    when(countriesManager.getCountriesJsonArray(countriesModel, "fr", "euCountry")).thenReturn(
      countries
    );
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(200)).thenReturn(response);

    mainVerticle.getCountriesTranslation(context);

    verify(response).end(countries.encode());
  }

  @Test
  void testCreateCollaborationIndexes_recoversFromAllFailures() {
    mainVerticle.mongoClient = mongo;
    when(mongo.dropIndex(anyString(), anyString())).thenReturn(Future.failedFuture("drop failed"));
    when(mongo.createIndexWithOptions(anyString(), any(), any(IndexOptions.class))).thenReturn(
      Future.failedFuture("user index failed"),
      Future.failedFuture("email index failed")
    );

    assertTrue(mainVerticle.createCollaborationIndexes().succeeded());
  }

  @Test
  void testCreateOwnershipRequestIndexes_recoversFromFailure() {
    mainVerticle.mongoClient = mongo;
    when(mongo.createIndexWithOptions(anyString(), any(), any(IndexOptions.class))).thenReturn(
      Future.failedFuture("ownership index failed")
    );

    assertTrue(mainVerticle.createOwnershipRequestIndexes().succeeded());
  }

  @Test
  void testCreateSectionLockIndexes_recoversFromAllFailures() {
    mainVerticle.mongoClient = mongo;
    when(mongo.createIndexWithOptions(anyString(), any(), any(IndexOptions.class))).thenReturn(
      Future.failedFuture("unique failed"),
      Future.failedFuture("ttl failed")
    );

    assertTrue(mainVerticle.createSectionLockIndexes().succeeded());
  }

  @Test
  void testCreateApplicationIndexes_recoversFromAllFailures() {
    mainVerticle.mongoClient = mongo;
    when(mongo.createIndexWithOptions(anyString(), any(), any(IndexOptions.class))).thenReturn(
      Future.failedFuture("date-created failed"),
      Future.failedFuture("date-modified failed"),
      Future.failedFuture("status-date-created failed")
    );

    assertTrue(
      mainVerticle
        .createApplicationIndexes(Constants.DATA_ACCESS_APPLICATION_COLLECTION)
        .succeeded()
    );
  }

  @Test
  void testSetUpSchedulersOutgoingApplicationActivities_whenEnabled_triggersPeriodicWork(
    Vertx vertx
  ) throws InterruptedException {
    class SchedulerVerticle extends MainVerticle {

      private final CountDownLatch latch = new CountDownLatch(1);

      @Override
      void sendOutgoingApplicationActivities(
        DispatcherScheduledJob dispatcherScheduledJob,
        eu.europa.data.health.access.dispatcher_client.DispatcherClient dispatcherClient,
        long dispatcherInterval
      ) {
        latch.countDown();
      }
    }

    SchedulerVerticle verticle = new SchedulerVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    verticle.dbHandler = mock(DBHandler.class);
    verticle.mongoClient = mongo;
    verticle.config = new JsonObject()
      .put(Constants.OUTGOING_APPLICATION_ACTIVITIES_ENABLE, Constants.SCHEDULER_ENABLED)
      .put(Constants.OUTGOING_APPLICATION_ACTIVITIES_INTERVAL, 1);

    verticle.setUpSchedulersOutgoingApplicationActivities(
      mock(DispatcherManager.class),
      mock(eu.europa.data.health.access.dispatcher_client.DispatcherClient.class),
      mock(AuditLogManager.class),
      mock(NotificationManager.class)
    );

    assertTrue(verticle.latch.await(1, TimeUnit.SECONDS));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRunInitialPermitExpirationAndScheduleRepeats_runsImmediatelyAndOnPeriodicTick() {
    MainVerticle verticle = new MainVerticle();
    Vertx mockedVertx = mock(Vertx.class);
    DataAccessPermitJob permitJob = mock(DataAccessPermitJob.class);
    ArgumentCaptor<Handler<Long>> periodicHandler = ArgumentCaptor.forClass(Handler.class);
    List<String> statuses = List.of("GRANTED");

    verticle.init(mockedVertx, mock(Context.class));
    when(mockedVertx.setPeriodic(eq(77L), periodicHandler.capture())).thenReturn(11L);

    verticle.runInitialPermitExpirationAndScheduleRepeats(permitJob, statuses, 77L);
    periodicHandler.getValue().handle(11L);

    verify(permitJob, times(2)).expireOldPermits(statuses);
  }

  @Test
  void testRetryPendingTranslations_callsBothCollections() {
    TranslationRetryScheduledJob retryJob = mock(TranslationRetryScheduledJob.class);

    mainVerticle.retryPendingTranslations(retryJob, 5, 10);

    verify(retryJob).retryPendingTranslations(Constants.DATA_REQUEST_APPLICATION_COLLECTION, 5, 10);
    verify(retryJob).retryPendingTranslations(Constants.DATA_ACCESS_APPLICATION_COLLECTION, 5, 10);
  }

  @Test
  void testHandleControlledVocabularyDocuments_refreshesOnlyNonBlankSources() {
    ControlledVocabularyManager cvManager = mock(ControlledVocabularyManager.class);
    JsonObject validDoc = new JsonObject().put("source", "https://example.com/cv");
    JsonObject blankDoc = new JsonObject().put("source", " ");
    when(cvManager.refreshBySource("https://example.com/cv", validDoc)).thenReturn(
      Future.succeededFuture()
    );

    mainVerticle.handleControlledVocabularyDocuments(cvManager, List.of(validDoc, blankDoc));

    verify(cvManager).refreshBySource("https://example.com/cv", validDoc);
    verify(cvManager, never()).refreshBySource(anyString(), eq(blankDoc));
  }

  @Test
  void testExecuteControlledVocabularyRefresh_whenSuccessful_reschedulesNextRun() {
    MainVerticle verticle = new MainVerticle();
    Vertx mockedVertx = mock(Vertx.class);
    ControlledVocabularyManager cvManager = mock(ControlledVocabularyManager.class);

    verticle.init(mockedVertx, mock(Context.class));
    verticle.config = new JsonObject().put(
      Constants.CONTROLLED_VOCABULARY_REFRESH_INTERVAL,
      60_000L
    );
    when(cvManager.getAll()).thenReturn(Future.succeededFuture(List.of()));
    when(mockedVertx.setTimer(any(Long.class), any())).thenReturn(1L);

    verticle.executeControlledVocabularyRefresh(cvManager);

    verify(cvManager).getAll();
    verify(mockedVertx).setTimer(eq(60_000L), any());
  }

  @Test
  void testExecuteControlledVocabularyRefresh_whenLookupFails_reschedulesNextRun() {
    MainVerticle verticle = new MainVerticle();
    Vertx mockedVertx = mock(Vertx.class);
    ControlledVocabularyManager cvManager = mock(ControlledVocabularyManager.class);

    verticle.init(mockedVertx, mock(Context.class));
    verticle.config = new JsonObject().put(
      Constants.CONTROLLED_VOCABULARY_REFRESH_INTERVAL,
      60_000L
    );
    when(cvManager.getAll()).thenReturn(Future.failedFuture("lookup failed"));
    when(mockedVertx.setTimer(any(Long.class), any())).thenReturn(1L);

    verticle.executeControlledVocabularyRefresh(cvManager);

    verify(cvManager).getAll();
    verify(mockedVertx).setTimer(eq(60_000L), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testScheduleRecurringNotifications_runsImmediatelyAndOnPeriodicTick() {
    MainVerticle verticle = new MainVerticle();
    Vertx mockedVertx = mock(Vertx.class);
    NotificationScheduledJob notificationJob = mock(NotificationScheduledJob.class);
    ArgumentCaptor<Handler<Long>> periodicHandler = ArgumentCaptor.forClass(Handler.class);
    Set<Integer> days = Set.of(1, 7);
    List<String> statuses = List.of("GRANTED");

    verticle.init(mockedVertx, mock(Context.class));
    when(mockedVertx.setPeriodic(eq(88L), periodicHandler.capture())).thenReturn(12L);

    verticle.scheduleRecurringNotifications(
      notificationJob,
      days,
      statuses,
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      Constants.PERMIT_NOTIFICATION_TYPE,
      88L
    );
    periodicHandler.getValue().handle(12L);

    verify(notificationJob, times(2)).createScheduledNotifications(
      days,
      statuses,
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      Constants.PERMIT_NOTIFICATION_TYPE
    );
  }

  @Test
  void testScheduleJobForDataAccessPermitApplication_usesConfiguredStatuses(Vertx vertx)
    throws InterruptedException {
    class PermitSchedulerVerticle extends MainVerticle {

      private final CountDownLatch latch = new CountDownLatch(1);
      private List<String> capturedStatuses;
      private long capturedInterval;

      @Override
      long millisUntilNextMidnight() {
        return 1;
      }

      @Override
      void runInitialPermitExpirationAndScheduleRepeats(
        DataAccessPermitJob dataAccessPermitJob,
        List<String> permitStatusesThatExpire,
        long dataAccessPermitInterval
      ) {
        capturedStatuses = permitStatusesThatExpire;
        capturedInterval = dataAccessPermitInterval;
        latch.countDown();
      }
    }

    PermitSchedulerVerticle verticle = new PermitSchedulerVerticle();
    verticle.init(vertx, vertx.getOrCreateContext());
    verticle.config = new JsonObject()
      .put(Constants.PERMIT_STATUSES_THAT_EXPIRE_LABEL, "GRANTED, RENEWED")
      .put(Constants.DATA_ACCESS_PERMIT_INTERVAL, 77L);

    verticle.scheduleJobForDataAccessPermitApplication(
      mongo,
      mock(DataAccessApplicationManager.class)
    );

    assertTrue(verticle.latch.await(1, TimeUnit.SECONDS));
    assertEquals(List.of("GRANTED", "RENEWED"), verticle.capturedStatuses);
    assertEquals(77L, verticle.capturedInterval);
  }

  private JsonObject minimalAuthConfig() {
    return new JsonObject()
      .put("clientId", "client-id")
      .put("clientSecret", "client-secret")
      .put(
        "tokenServerConfig",
        new JsonObject().put("keycloak", new JsonObject().put("serverUrl", "http://localhost"))
      );
  }

  private record ResponseExpectation(String path, int statusCode, String bodyFragment) {}
}
