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

import static eu.europa.data.health.access.Constants.APPLICATION_COLLABORATOR_COLLECTION;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_ID;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_NAME;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.INVITATION_PENDING;
import static eu.europa.data.health.access.Constants.INVITATION_STATUS;
import static eu.europa.data.health.access.Constants.INVITED_EMAIL;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.mail.MailClient;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Integration tests for {@link UserManager#bootstrapUser(RoutingContext)}.
 */
@ExtendWith(VertxExtension.class)
class UserBootstrapTest {

  private static final String TEST_DB = "bootstrapTestDb";
  private static final String TEST_USER = "testUser1";
  private static final String TEST_EMAIL = "testuser@example.com";
  private static final String TEST_GIVEN_NAME = "Maria";
  private static final String TEST_FAMILY_NAME = "Papadopoulou";
  private static final String TEST_DISPLAY_NAME = TEST_GIVEN_NAME + " " + TEST_FAMILY_NAME;

  private static MongoDBContainer mongoDBContainer;
  private static Vertx vertx;
  private static MongoClient mongoClient;
  private UserManager userManager;

  @BeforeAll
  static void setUpSuite() {
    vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:5.0");
    mongoDBContainer.start();

    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(TEST_DB));

    mongoClient = MongoClient.createShared(vertx, config);
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    cleanupCollection(USER_COLLECTION);
    cleanupCollection(APPLICATION_COLLABORATOR_COLLECTION);
    DBHandler dbHandler = new DBHandler(mongoClient);
    AuditLogManager auditLogManager = new AuditLogManager(dbHandler, mock(AuthClient.class));
    MailClient mailClient = mock(MailClient.class);
    NotificationManager notificationManager = new NotificationManager(dbHandler, mailClient, "http://test-base-url");
    userManager = new UserManager(
      dbHandler,
      mongoClient,
      mock(DispatcherClient.class),
      auditLogManager,
      mock(AuthClient.class),
      notificationManager
    );
  }

  @AfterAll
  static void tearDownSuite() throws InterruptedException {
    closeMongoClient();
    closeVertx();
    mongoDBContainer.stop();
  }

  private RoutingContext mockContextWithEmail() {
    return mockContextWithPrincipal(new JsonObject()
      .put("preferred_username", TEST_USER)
      .put("email", TEST_EMAIL));
  }

  private RoutingContext mockContextWithEmailAndNames() {
    return mockContextWithPrincipal(new JsonObject()
      .put("preferred_username", TEST_USER)
      .put("email", TEST_EMAIL)
      .put("given_name", TEST_GIVEN_NAME)
      .put("family_name", TEST_FAMILY_NAME));
  }

  private RoutingContext mockContextWithoutEmail() {
    return mockContextWithPrincipal(new JsonObject().put("preferred_username", TEST_USER));
  }

  private RoutingContext mockContextWithPrincipal(JsonObject principal) {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(principal);
    when(mockUser.get("preferred_username"))
      .thenReturn(principal.getString("preferred_username"));
    when(ctx.user()).thenReturn(mockUser);
    return ctx;
  }

  @Test
  @DisplayName("bootstrapUser: creates new USER document with email and userName on first login")
  void bootstrapUser_createsUserWithEmail(Vertx vertx, VertxTestContext testContext) {
    userManager.bootstrapUser(mockContextWithEmailAndNames());

    vertx.setTimer(600, id ->
      mongoClient.find(USER_COLLECTION, new JsonObject().put("_id", TEST_USER))
        .onSuccess(results -> {
          testContext.verify(() -> {
            assertEquals(1, results.size());
            assertEquals(TEST_EMAIL, results.get(0).getString(EMAIL));
            assertEquals(TEST_DISPLAY_NAME, results.get(0).getString(USER_NAME));
          });
          testContext.completeNow();
        })
        .onFailure(testContext::failNow));
  }

  @Test
  @DisplayName("bootstrapUser: updates existing USER document with email and fallback userName")
  void bootstrapUser_updatesExistingUserWithEmail(Vertx vertx, VertxTestContext testContext) {
    mongoClient.insert(USER_COLLECTION, new JsonObject().put("_id", TEST_USER))
      .onSuccess(v -> {
        userManager.bootstrapUser(mockContextWithEmail());

        vertx.setTimer(600, id ->
          mongoClient.find(USER_COLLECTION, new JsonObject().put("_id", TEST_USER))
            .onSuccess(results -> {
              testContext.verify(() -> {
                assertEquals(1, results.size());
                assertEquals(TEST_EMAIL, results.get(0).getString(EMAIL));
                assertEquals(TEST_USER, results.get(0).getString(USER_NAME));
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("bootstrapUser: returns 400 when token has no email claim")
  void bootstrapUser_returns400_whenNoEmailClaim(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = mockContextWithoutEmail();
    userManager.bootstrapUser(ctx);

    vertx.setTimer(400, id -> {
      testContext.verify(() -> assertEquals(400, ctx.response().getStatusCode()));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("bootstrapUser: resolves pending collaborator invites with userId and collaboratorUserName")
  void bootstrapUser_resolvesPendingCollaboratorInvites(Vertx vertx, VertxTestContext testContext) {
    JsonObject pendingInvite = new JsonObject()
      .put(INVITED_EMAIL, TEST_EMAIL)
      .put(INVITATION_STATUS, INVITATION_PENDING)
      .put("applicationType", "DATA_ACCESS_APPLICATION")
      .put("applicationId", "app-123");

    mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, pendingInvite)
      .onSuccess(v -> {
        userManager.bootstrapUser(mockContextWithEmailAndNames());

        vertx.setTimer(800, id ->
          mongoClient.find(APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put(INVITED_EMAIL, TEST_EMAIL))
            .onSuccess(results -> {
              testContext.verify(() -> {
                assertNotNull(results);
                assertEquals(1, results.size());
                assertEquals(TEST_USER,
                  results.get(0).getString(COLLABORATOR_USER_ID));
                assertEquals(TEST_DISPLAY_NAME,
                  results.get(0).getString(COLLABORATOR_USER_NAME),
                  "Pending invite should be backfilled with collaboratorUserName");
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  private void cleanupCollection(String collection) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    mongoClient.removeDocuments(collection, new JsonObject(), result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out cleaning collection: " + collection);
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  private static void closeMongoClient() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    mongoClient.close().onComplete(result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out closing Mongo client");
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  private static void closeVertx() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    vertx.close(result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out closing Vertx");
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }
}
