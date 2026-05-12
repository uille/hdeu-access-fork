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
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE_FIELD;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_ID;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_NAME;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.INVITATION_ACCEPTED;
import static eu.europa.data.health.access.Constants.INVITATION_PENDING;
import static eu.europa.data.health.access.Constants.INVITATION_STATUS;
import static eu.europa.data.health.access.Constants.INVITED_BY;
import static eu.europa.data.health.access.Constants.INVITED_EMAIL;
import static eu.europa.data.health.access.Constants.JUSTIFICATION;
import static eu.europa.data.health.access.Constants.NOTIFICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.OWNERSHIP_REQUEST_COLLECTION;
import static eu.europa.data.health.access.Constants.ROLE;
import static eu.europa.data.health.access.Constants.ROLE_EDITOR;
import static eu.europa.data.health.access.Constants.ROLE_OWNER;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.NotificationKeys;
import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.mail.MailClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
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
 * Integration tests for {@link CollaboratorManager}.
 */
@ExtendWith(VertxExtension.class)
class CollaboratorManagerTest {

  private static final String TEST_DB = "collaboratorTestDb";
  private static final String OWNER_ID = "ownerUser";
  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String OWNER_NAME = "Owner User";
  private static final String INVITED_USER = "invitedUser";
  private static final String INVITED_EMAIL_VAL = "invited@example.com";
  private static final String INVITED_USER_NAME = "Maria Papadopoulou";
  private static final String APP_ID = "app-test-123";
  private static final String APP_TYPE = DATA_ACCESS_APPLICATION_COLLECTION;

  private static final int MAX_WAIT_ATTEMPTS = 20;
  private static final long WAIT_DELAY_MS = 200L;

  private static final List<String> TEST_COLLECTIONS = List.of(
    APP_TYPE,
    APPLICATION_COLLABORATOR_COLLECTION,
    USER_COLLECTION,
    OWNERSHIP_REQUEST_COLLECTION,
    NOTIFICATION_COLLECTION,
    AUDIT_LOG_COLLECTION
  );

  private static MongoDBContainer mongoDBContainer;
  private static Vertx vertx;
  private static MongoClient mongoClient;
  private CollaboratorManager collaboratorManager;

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
    cleanupCollections();
    DBHandler dbHandler = new DBHandler(mongoClient);
    AuditLogManager auditLogManager = new AuditLogManager(dbHandler, mock(AuthClient.class));
    MailClient mailClient = mock(MailClient.class);
    when(mailClient.sendMail(org.mockito.ArgumentMatchers.any()))
      .thenReturn(Future.succeededFuture());

    NotificationManager notificationManager = new NotificationManager(dbHandler, mailClient, "http://test");

    collaboratorManager = new CollaboratorManager(
      dbHandler, mongoClient, auditLogManager, notificationManager);
  }

  @AfterAll
  static void tearDownSuite() throws InterruptedException {
    closeMongoClient();
    closeVertx();
    mongoDBContainer.stop();
  }

  private Future<String> insertOwner() {
    JsonObject appDoc = new JsonObject()
      .put("_id", APP_ID)
      .put(USER_ID, OWNER_ID);

    JsonObject ownerCollaborator = new JsonObject()
      .put(APPLICATION_ID, APP_ID)
      .put(APPLICATION_TYPE_FIELD, APP_TYPE)
      .put(COLLABORATOR_USER_ID, OWNER_ID)
      .put(COLLABORATOR_USER_NAME, OWNER_NAME)
      .put(INVITED_EMAIL, OWNER_EMAIL)
      .put(ROLE, ROLE_OWNER)
      .put(INVITATION_STATUS, INVITATION_ACCEPTED)
      .put(INVITED_BY, OWNER_ID);

    return mongoClient.insert(APP_TYPE, appDoc)
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, ownerCollaborator));
  }

  private Future<String> insertInvitedUserRecord() {
    return mongoClient.insert(USER_COLLECTION,
      new JsonObject()
        .put("_id", INVITED_USER)
        .put(EMAIL, INVITED_EMAIL_VAL)
        .put(USER_NAME, INVITED_USER_NAME));
  }

  private String emailForUser(String userId) {
    if (OWNER_ID.equals(userId)) {
      return OWNER_EMAIL;
    }
    if (INVITED_USER.equals(userId)) {
      return INVITED_EMAIL_VAL;
    }
    return userId + "@example.com";
  }

  private RoutingContext ctxFor(String userId, JsonObject body) {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);

    JsonObject principal = new JsonObject()
      .put("preferred_username", userId)
      .put("email", emailForUser(userId))
      .put("given_name", "Test")
      .put("family_name", "User");

    when(mockUser.principal()).thenReturn(principal);
    when(mockUser.get("preferred_username")).thenReturn(userId);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("applicationId")).thenReturn(APP_ID);
    return ctx;
  }

  private RoutingContext ctxForAdmin(JsonObject body) {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);

    JsonObject principal = new JsonObject()
      .put("preferred_username", "admin-user")
      .put("email", "admin@example.com")
      .put("given_name", "Admin")
      .put("family_name", "User")
      .put("realm_access", new JsonObject().put("roles", new JsonArray().add("data-admin")));

    when(mockUser.principal()).thenReturn(principal);
    when(mockUser.get("preferred_username")).thenReturn("admin-user");
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("applicationId")).thenReturn(APP_ID);
    return ctx;
  }

  private RoutingContext ctxFor(String userId, JsonObject body, String collaboratorId) {
    RoutingContext ctx = ctxFor(userId, body);
    when(ctx.pathParam("collaboratorId")).thenReturn(collaboratorId);
    return ctx;
  }

  private RoutingContext ctxForCollaboratorList(
    String userId,
    String searchTerm,
    String sortBy,
    String sortDirection
  ) {
    RoutingContext ctx = ctxFor(userId, new JsonObject());
    when(ctx.request().getParam("search")).thenReturn(searchTerm);
    when(ctx.request().getParam("sortBy")).thenReturn(sortBy);
    when(ctx.request().getParam("sortDirection")).thenReturn(sortDirection);
    return ctx;
  }

  private RoutingContext ctxForRequestId(String userId, JsonObject body, String requestId) {
    RoutingContext ctx = ctxFor(userId, body);
    when(ctx.pathParam("requestId")).thenReturn(requestId);
    return ctx;
  }

  /**
   * Polls a collection until the expected document count is reached.
   * Useful for fire-and-forget side effects such as notifications/audit logs.
   */
  private Future<List<JsonObject>> waitForDocuments(
    Vertx vertx,
    String collection,
    JsonObject query,
    int expectedCount
  ) {
    return waitForDocuments(vertx, collection, query, expectedCount, 0);
  }

  private Future<List<JsonObject>> waitForDocuments(
    Vertx vertx,
    String collection,
    JsonObject query,
    int expectedCount,
    int attempt
  ) {
    Promise<List<JsonObject>> promise = Promise.promise();

    mongoClient.find(collection, query)
      .onSuccess(results -> {
        if (results.size() == expectedCount) {
          promise.complete(results);
          return;
        }

        if (attempt >= MAX_WAIT_ATTEMPTS) {
          promise.fail(new AssertionError(
            "Expected " + expectedCount + " documents in collection '" + collection
              + "' but found " + results.size() + " for query: " + query.encode()
          ));
          return;
        }

        vertx.setTimer(WAIT_DELAY_MS, id ->
          waitForDocuments(vertx, collection, query, expectedCount, attempt + 1)
            .onComplete(promise)
        );
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  private void cleanupCollections() throws InterruptedException {
    for (String collection : TEST_COLLECTIONS) {
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

  // ---------------------------------------------------------------------------
  // List tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("getCollaborators: searches linked collaborators by stored collaboratorUserName")
  void getCollaborators_searchesByStoredCollaboratorUserName(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, INVITED_USER)
        .put(COLLABORATOR_USER_NAME, INVITED_USER_NAME)
        .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(v -> {
        RoutingContext ctx = ctxForCollaboratorList(OWNER_ID, "papad", null, null);
        collaboratorManager.getCollaborators(ctx);

        vertx.setTimer(600, id -> {
          testContext.verify(() -> {
            assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
            JsonObject body = new JsonObject(ctx.response().getStatusMessage());
            JsonArray results = body.getJsonArray("items");
            assertEquals(1, results.size());
            assertEquals(1L, body.getLong("total").longValue());
            assertEquals(
              INVITED_USER_NAME,
              results.getJsonObject(0).getString(COLLABORATOR_USER_NAME)
            );
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("getCollaborators: sorts collaborators by stored collaboratorUserName")
  void getCollaborators_sortsByStoredCollaboratorUserName(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, "user-b")
        .put(COLLABORATOR_USER_NAME, "Zeta User")
        .put(INVITED_EMAIL, "zeta@example.com")
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, "user-a")
        .put(COLLABORATOR_USER_NAME, "Alpha User")
        .put(INVITED_EMAIL, "alpha@example.com")
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(v -> {
        RoutingContext ctx = ctxForCollaboratorList(OWNER_ID, null, COLLABORATOR_USER_NAME, "asc");
        collaboratorManager.getCollaborators(ctx);

        vertx.setTimer(600, id -> {
          testContext.verify(() -> {
            assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
            JsonObject body = new JsonObject(ctx.response().getStatusMessage());
            JsonArray results = body.getJsonArray("items");
            assertEquals(3L, body.getLong("total").longValue());
            assertEquals("Alpha User", results.getJsonObject(0).getString(COLLABORATOR_USER_NAME));
            assertEquals("Owner User", results.getJsonObject(1).getString(COLLABORATOR_USER_NAME));
            assertEquals("Zeta User", results.getJsonObject(2).getString(COLLABORATOR_USER_NAME));
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("getCollaborators: keeps searching pending invites by email")
  void getCollaborators_searchesPendingInviteByEmail(Vertx vertx, VertxTestContext testContext) {
    insertOwner()
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(INVITED_EMAIL, "pending.user@example.com")
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_PENDING)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(v -> {
        RoutingContext ctx = ctxForCollaboratorList(OWNER_ID, "pending.user", null, null);
        collaboratorManager.getCollaborators(ctx);

        vertx.setTimer(600, id -> {
          testContext.verify(() -> {
            assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
            JsonObject body = new JsonObject(ctx.response().getStatusMessage());
            JsonArray results = body.getJsonArray("items");
            assertEquals(1, results.size());
            assertEquals(1L, body.getLong("total").longValue());
            assertEquals(
              "pending.user@example.com",
              results.getJsonObject(0).getString(INVITED_EMAIL)
            );
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  // ---------------------------------------------------------------------------
  // Invite tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("addCollaborator: links userId immediately when USER.email exists")
  void addCollaborator_linksUserId_whenUserEmailExists(Vertx vertx, VertxTestContext testContext) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .onSuccess(v -> {
        JsonObject body = new JsonObject()
          .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
          .put(APPLICATION_TYPE_FIELD, APP_TYPE);

        collaboratorManager.addCollaborator(ctxFor(OWNER_ID, body));

        vertx.setTimer(700, id ->
          mongoClient.find(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put(INVITED_EMAIL, INVITED_EMAIL_VAL).put(ROLE, ROLE_EDITOR)
            )
            .onSuccess(results -> {
              testContext.verify(() -> {
                assertNotNull(results);
                assertEquals(1, results.size(), "One invite should exist");
                assertEquals(
                  INVITED_USER,
                  results.get(0).getString(COLLABORATOR_USER_ID),
                  "collaboratorUserId should be resolved immediately"
                );
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("addCollaborator: stores collaboratorUserId and collaboratorUserName when USER exists")
  void addCollaborator_storesCollaboratorUserName_whenUserExists(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .onSuccess(v -> {
        JsonObject body = new JsonObject()
          .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
          .put(APPLICATION_TYPE_FIELD, APP_TYPE);

        collaboratorManager.addCollaborator(ctxFor(OWNER_ID, body));

        vertx.setTimer(700, id ->
          mongoClient.find(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put(INVITED_EMAIL, INVITED_EMAIL_VAL).put(ROLE, ROLE_EDITOR)
            )
            .onSuccess(results -> {
              testContext.verify(() -> {
                assertEquals(1, results.size(), "One invite should exist");
                JsonObject collaborator = results.get(0);
                assertEquals(INVITED_USER, collaborator.getString(COLLABORATOR_USER_ID));
                assertEquals(
                  INVITED_USER_NAME,
                  collaborator.getString(COLLABORATOR_USER_NAME),
                  "collaboratorUserName should be stored from USER.userName"
                );
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("addCollaborator: creates PENDING invite when user not in USER collection")
  void addCollaborator_createsPendingInvite_whenUserUnknown(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .onSuccess(v -> {
        JsonObject body = new JsonObject()
          .put(INVITED_EMAIL, "unknown@example.com")
          .put(APPLICATION_TYPE_FIELD, APP_TYPE);

        collaboratorManager.addCollaborator(ctxFor(OWNER_ID, body));

        vertx.setTimer(700, id ->
          mongoClient.find(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put(INVITED_EMAIL, "unknown@example.com")
            )
            .onSuccess(results -> {
              testContext.verify(() -> {
                assertEquals(1, results.size());
                assertEquals(INVITATION_PENDING, results.get(0).getString(INVITATION_STATUS));
                assertNull(
                  results.get(0).getString(COLLABORATOR_USER_NAME),
                  "pending invites should not invent collaboratorUserName"
                );
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("addCollaborator: returns 409 with exact message for duplicate invite")
  void addCollaborator_returns409_forDuplicateInvite(Vertx vertx, VertxTestContext testContext) {
    insertOwner()
      .onSuccess(v -> {
        JsonObject body = new JsonObject()
          .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
          .put(APPLICATION_TYPE_FIELD, APP_TYPE);

        collaboratorManager.addCollaborator(ctxFor(OWNER_ID, body));

        vertx.setTimer(600, id -> {
          RoutingContext duplicateCtx = ctxFor(OWNER_ID, body.copy());
          collaboratorManager.addCollaborator(duplicateCtx);

          vertx.setTimer(600, id2 -> {
            testContext.verify(() ->
              assertEquals(
                HttpResponseStatus.CONFLICT.code(),
                duplicateCtx.response().getStatusCode()
              )
            );
            testContext.completeNow();
          });
        });
      })
      .onFailure(testContext::failNow);
  }

  // ---------------------------------------------------------------------------
  // Accept / Reject tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("acceptCollaboratorInvite: sets invitationStatus to ACCEPTED")
  void acceptCollaboratorInvite_setsAccepted(Vertx vertx, VertxTestContext testContext) {
    JsonObject invite = new JsonObject()
      .put(APPLICATION_ID, APP_ID)
      .put(APPLICATION_TYPE_FIELD, APP_TYPE)
      .put(COLLABORATOR_USER_ID, INVITED_USER)
      .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
      .put(ROLE, ROLE_EDITOR)
      .put(INVITATION_STATUS, INVITATION_PENDING)
      .put(INVITED_BY, OWNER_ID);

    mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, invite)
      .onSuccess(docId -> {
        collaboratorManager.acceptCollaboratorInvite(ctxFor(INVITED_USER, new JsonObject(), docId));

        vertx.setTimer(600, id ->
          mongoClient.findOne(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put("_id", docId),
              null
            )
            .onSuccess(doc -> {
              testContext.verify(() ->
                assertEquals(INVITATION_ACCEPTED, doc.getString(INVITATION_STATUS))
              );
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("rejectCollaboratorInvite: sets invitationStatus to REJECTED")
  void rejectCollaboratorInvite_setsRejected(Vertx vertx, VertxTestContext testContext) {
    JsonObject invite = new JsonObject()
      .put(APPLICATION_ID, APP_ID)
      .put(APPLICATION_TYPE_FIELD, APP_TYPE)
      .put(COLLABORATOR_USER_ID, INVITED_USER)
      .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
      .put(ROLE, ROLE_EDITOR)
      .put(INVITATION_STATUS, INVITATION_PENDING)
      .put(INVITED_BY, OWNER_ID);

    mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, invite)
      .onSuccess(docId -> {
        collaboratorManager.rejectCollaboratorInvite(ctxFor(INVITED_USER, new JsonObject(), docId));

        vertx.setTimer(600, id ->
          mongoClient.findOne(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put("_id", docId),
              null
            )
            .onSuccess(doc -> {
              testContext.verify(() ->
                assertEquals("REJECTED", doc.getString(INVITATION_STATUS))
              );
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("removeCollaborator: fetches application title for notification properties")
  void removeCollaborator_fetchesApplicationTitleForNotification(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .compose(v -> mongoClient.updateCollection(
        APP_TYPE,
        new JsonObject().put("_id", APP_ID),
        new JsonObject().put("$set", new JsonObject().put("title", "Fetched app title"))
      ))
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, INVITED_USER)
        .put(COLLABORATOR_USER_NAME, INVITED_USER_NAME)
        .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(collaboratorId -> {
        RoutingContext ctx = ctxFor(OWNER_ID, new JsonObject(), collaboratorId);
        collaboratorManager.removeCollaborator(ctx);

        vertx.setTimer(700, id ->
          mongoClient.findOne(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put("_id", collaboratorId),
              null
            )
            .compose(removedDoc -> {
              testContext.verify(() -> assertNull(removedDoc));
              return waitForDocuments(
                vertx,
                NOTIFICATION_COLLECTION,
                new JsonObject()
                  .put("applicationId", APP_ID)
                  .put(USER_ID, INVITED_USER)
                  .put("notificationCode",
                    NotificationCode.COLLABORATOR_REMOVED_BY_OWNER.getCode()),
                1
              );
            })
            .onSuccess(notifications -> {
              testContext.verify(() -> {
                assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
                JsonObject props = notifications.get(0).getJsonObject("notificationProperties");
                assertNotNull(props);
                assertEquals("Fetched app title", props.getString(NotificationKeys.APPLICATION_NAME));
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("transferOwnership: updates application userId, keeps former owner as EDITOR, and removes new owner collaborator row")
  void transferOwnership_updatesApplicationUserIdAndRemovesNewOwnerCollaborator(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .compose(v -> mongoClient.updateCollection(
        APP_TYPE,
        new JsonObject().put("_id", APP_ID),
        new JsonObject().put("$set", new JsonObject().put("title", "Fetched app title"))
      ))
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, INVITED_USER)
        .put(COLLABORATOR_USER_NAME, INVITED_USER_NAME)
        .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(newOwnerCollaboratorId -> {
        RoutingContext ctx = ctxFor(
          OWNER_ID,
          new JsonObject().put("newOwnerCollaboratorId", newOwnerCollaboratorId)
        );
        collaboratorManager.transferOwnership(ctx);

        vertx.setTimer(800, id ->
          mongoClient.find(
              APPLICATION_COLLABORATOR_COLLECTION,
              new JsonObject().put(APPLICATION_ID, APP_ID)
            )
            .compose(collaborators -> {
              testContext.verify(() -> {
                assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
                assertEquals(
                  1,
                  collaborators.size(),
                  "Only the former owner should remain in collaborators after transfer"
                );
                JsonObject remainingCollaborator = collaborators.get(0);
                assertEquals(OWNER_ID, remainingCollaborator.getString(COLLABORATOR_USER_ID));
                assertEquals(ROLE_EDITOR, remainingCollaborator.getString(ROLE));
              });

              return waitForDocuments(
                vertx,
                NOTIFICATION_COLLECTION,
                new JsonObject()
                  .put("applicationId", APP_ID)
                  .put(USER_ID, INVITED_USER)
                  .put("notificationCode",
                    NotificationCode.COLLABORATION_OWNERSHIP_TRANSFERRED.getCode()),
                1
              );
            })
            .compose(notifications -> {
              testContext.verify(() -> {
                JsonObject props = notifications.get(0).getJsonObject("notificationProperties");
                assertNotNull(props);
                assertEquals("app-test-123 – Fetched app title", props.getString(NotificationKeys.APPLICATION_NAME));
              });
              return mongoClient.findOne(APP_TYPE, new JsonObject().put("_id", APP_ID), null);
            })
            .onSuccess(applicationDoc -> {
              testContext.verify(() -> {
                assertNotNull(applicationDoc);
                assertEquals(
                  INVITED_USER,
                  applicationDoc.getString(USER_ID),
                  "Application userId should be updated to the new owner"
                );
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("transferOwnership: admin action notifies former owner with admin transfer notification")
  void transferOwnership_byAdmin_createsFormerOwnerAdminNotification(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, INVITED_USER)
        .put(COLLABORATOR_USER_NAME, INVITED_USER_NAME)
        .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(newOwnerCollaboratorId -> {
        RoutingContext ctx = ctxForAdmin(
          new JsonObject().put("newOwnerCollaboratorId", newOwnerCollaboratorId)
        );
        collaboratorManager.transferOwnership(ctx);

        waitForDocuments(
          vertx,
          NOTIFICATION_COLLECTION,
          new JsonObject()
            .put("applicationId", APP_ID)
            .put(USER_ID, OWNER_ID)
            .put("notificationCode",
              NotificationCode.COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN.getCode()),
          1
        )
          .onSuccess(notifications -> {
            testContext.verify(() -> {
              assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
              assertEquals(1, notifications.size());
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow);
      })
      .onFailure(testContext::failNow);
  }

  // ---------------------------------------------------------------------------
  // Ownership request tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("requestOwnership: creates ownership request for accepted editor")
  void requestOwnership_createsOwnershipRequest_forAcceptedEditor(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, INVITED_USER)
        .put(COLLABORATOR_USER_NAME, INVITED_USER_NAME)
        .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .onSuccess(v -> {
        RoutingContext ctx = ctxFor(
          INVITED_USER,
          new JsonObject().put(JUSTIFICATION, "Need to manage the application ongoing")
        );
        collaboratorManager.requestOwnership(ctx);

        vertx.setTimer(700, id ->
          mongoClient.find(
              OWNERSHIP_REQUEST_COLLECTION,
              new JsonObject().put(APPLICATION_ID, APP_ID).put(USER_ID, INVITED_USER)
            )
            .compose(results -> {
              testContext.verify(() -> {
                assertEquals(1, results.size());
                assertEquals(
                  "Need to manage the application ongoing",
                  results.get(0).getString(JUSTIFICATION)
                );
                assertEquals(HttpResponseStatus.CREATED.code(), ctx.response().getStatusCode());
              });

              return waitForDocuments(
                vertx,
                NOTIFICATION_COLLECTION,
                new JsonObject()
                  .put("applicationId", APP_ID)
                  .put(USER_ID, OWNER_ID)
                  .put("notificationCode",
                    NotificationCode.COLLABORATION_OWNERSHIP_REQUESTED.getCode()),
                1
              );
            })
            .compose(notifications -> {
              testContext.verify(() -> assertEquals(1, notifications.size()));
              return waitForDocuments(
                vertx,
                AUDIT_LOG_COLLECTION,
                new JsonObject().put("action", "Request Ownership"),
                1
              );
            })
            .onSuccess(auditLogs -> {
              testContext.verify(() -> {
                assertEquals(1, auditLogs.size());
                assertEquals(
                  "Ownership request submitted for Application " + APP_ID
                    + " by user Maria Papadopoulou / invited@example.com.",
                  auditLogs.get(0).getString("actionDescription")
                );
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("requestOwnership: rejects non-editor users")
  void requestOwnership_rejectsNonEditors(Vertx vertx, VertxTestContext testContext) {
    insertOwner()
      .onSuccess(v -> {
        RoutingContext ctx = ctxFor(
          INVITED_USER,
          new JsonObject().put(JUSTIFICATION, "Please transfer ownership")
        );
        collaboratorManager.requestOwnership(ctx);

        vertx.setTimer(400, id -> {
          testContext.verify(() ->
            assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode())
          );
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("getOwnershipRequests: owner sees pending requests")
  void getOwnershipRequests_returnsRequests_forOwner(Vertx vertx, VertxTestContext testContext) {
    insertOwner()
      .compose(v -> mongoClient.insert(OWNERSHIP_REQUEST_COLLECTION, new JsonObject()
        .put("_id", "req-1")
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(USER_ID, INVITED_USER)
        .put(USER_NAME, INVITED_USER_NAME)
        .put(JUSTIFICATION, "Justification")
        .put("createdAt", "2026-03-23T10:00:00Z")))
      .onSuccess(v -> {
        RoutingContext ctx = ctxFor(OWNER_ID, new JsonObject());
        collaboratorManager.getOwnershipRequests(ctx);

        vertx.setTimer(500, id -> {
          testContext.verify(() -> {
            assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
            JsonArray results = new JsonArray(ctx.response().getStatusMessage());
            assertEquals(1, results.size());
            assertEquals("Justification", results.getJsonObject(0).getString(JUSTIFICATION));
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("acceptOwnershipRequest: transfers ownership and removes request records")
  void acceptOwnershipRequest_transfersOwnership_andRemovesRequests(
    Vertx vertx,
    VertxTestContext testContext
  ) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .compose(v -> mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, new JsonObject()
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(COLLABORATOR_USER_ID, INVITED_USER)
        .put(COLLABORATOR_USER_NAME, INVITED_USER_NAME)
        .put(INVITED_EMAIL, INVITED_EMAIL_VAL)
        .put(ROLE, ROLE_EDITOR)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED)
        .put(INVITED_BY, OWNER_ID)))
      .compose(v -> mongoClient.insert(OWNERSHIP_REQUEST_COLLECTION, new JsonObject()
        .put("_id", "req-accept")
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(USER_ID, INVITED_USER)
        .put(USER_NAME, INVITED_USER_NAME)
        .put(JUSTIFICATION, "Please make me owner")
        .put("createdAt", "2026-03-23T10:00:00Z")))
      .onSuccess(v -> {
        RoutingContext ctx = ctxForRequestId(OWNER_ID, new JsonObject(), "req-accept");
        collaboratorManager.acceptOwnershipRequest(ctx);

        vertx.setTimer(900, id ->
          mongoClient.find(
              OWNERSHIP_REQUEST_COLLECTION,
              new JsonObject().put(APPLICATION_ID, APP_ID)
            )
            .compose(requests -> {
              testContext.verify(() -> {
                assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
                assertEquals(0, requests.size(), "All ownership requests should be cleaned up");
              });
              return mongoClient.find(
                APPLICATION_COLLABORATOR_COLLECTION,
                new JsonObject().put(APPLICATION_ID, APP_ID)
              );
            })
            .compose(collaborators -> {
              testContext.verify(() -> {
                assertEquals(1, collaborators.size());
                assertEquals(OWNER_ID, collaborators.get(0).getString(COLLABORATOR_USER_ID));
                assertEquals(ROLE_EDITOR, collaborators.get(0).getString(ROLE));
              });

              return waitForDocuments(
                vertx,
                NOTIFICATION_COLLECTION,
                new JsonObject()
                  .put("applicationId", APP_ID)
                  .put(USER_ID, INVITED_USER)
                  .put("notificationCode",
                    NotificationCode.COLLABORATION_OWNERSHIP_REQUEST_APPROVED.getCode()),
                1
              );
            })
            .compose(notifications -> {
              testContext.verify(() -> assertEquals(1, notifications.size()));
              return waitForDocuments(
                vertx,
                AUDIT_LOG_COLLECTION,
                new JsonObject().put("action", "Ownership transfer approved"),
                1
              );
            })
            .compose(auditLogs -> {
              testContext.verify(() -> {
                assertEquals(1, auditLogs.size());
                assertEquals(
                  "Ownership request transfer for Application " + APP_ID
                    + " submitted by Maria Papadopoulou / invited@example.com has been approved by Owner User / owner@example.com.",
                  auditLogs.get(0).getString("actionDescription")
                );
              });
              return mongoClient.findOne(APP_TYPE, new JsonObject().put("_id", APP_ID), null);
            })
            .onSuccess(applicationDoc -> {
              testContext.verify(() ->
                assertEquals(INVITED_USER, applicationDoc.getString(USER_ID))
              );
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("rejectOwnershipRequest: deletes the request and keeps ownership unchanged")
  void rejectOwnershipRequest_deletesRequest_only(Vertx vertx, VertxTestContext testContext) {
    insertOwner()
      .compose(v -> insertInvitedUserRecord())
      .compose(v -> mongoClient.insert(OWNERSHIP_REQUEST_COLLECTION, new JsonObject()
        .put("_id", "req-reject")
        .put(APPLICATION_ID, APP_ID)
        .put(APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(USER_ID, INVITED_USER)
        .put(USER_NAME, INVITED_USER_NAME)
        .put(JUSTIFICATION, "Please make me owner")
        .put("createdAt", "2026-03-23T10:00:00Z")))
      .onSuccess(v -> {
        RoutingContext ctx = ctxForRequestId(OWNER_ID, new JsonObject(), "req-reject");
        collaboratorManager.rejectOwnershipRequest(ctx);

        vertx.setTimer(700, id ->
          mongoClient.findOne(
              OWNERSHIP_REQUEST_COLLECTION,
              new JsonObject().put("_id", "req-reject"),
              null
            )
            .compose(requestDoc -> {
              testContext.verify(() -> {
                assertNull(requestDoc);
                assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
              });

              return waitForDocuments(
                vertx,
                NOTIFICATION_COLLECTION,
                new JsonObject()
                  .put("applicationId", APP_ID)
                  .put(USER_ID, INVITED_USER)
                  .put("notificationCode",
                    NotificationCode.COLLABORATION_OWNERSHIP_REQUEST_REJECTED.getCode()),
                1
              );
            })
            .compose(notifications -> {
              testContext.verify(() -> assertEquals(1, notifications.size()));
              return waitForDocuments(
                vertx,
                AUDIT_LOG_COLLECTION,
                new JsonObject().put("action", "Ownership transfer rejected"),
                1
              );
            })
            .compose(auditLogs -> {
              testContext.verify(() -> {
                assertEquals(1, auditLogs.size());
                assertEquals(
                  "Ownership request transfer for Application " + APP_ID
                    + " submitted by Maria Papadopoulou / invited@example.com has been rejected by Test User / owner@example.com.",
                  auditLogs.get(0).getString("actionDescription")
                );
              });
              return mongoClient.findOne(APP_TYPE, new JsonObject().put("_id", APP_ID), null);
            })
            .onSuccess(applicationDoc -> {
              testContext.verify(() ->
                assertEquals(OWNER_ID, applicationDoc.getString(USER_ID))
              );
              testContext.completeNow();
            })
            .onFailure(testContext::failNow));
      })
      .onFailure(testContext::failNow);
  }
}
