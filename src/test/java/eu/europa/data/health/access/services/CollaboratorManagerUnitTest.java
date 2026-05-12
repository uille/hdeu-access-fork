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
import static eu.europa.data.health.access.Constants.APPLICATION_TITLE;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE_FIELD;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_ID;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_NAME;
import static eu.europa.data.health.access.Constants.CREATED_AT;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ADMIN;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INVITATION_ACCEPTED;
import static eu.europa.data.health.access.Constants.INVITATION_PENDING;
import static eu.europa.data.health.access.Constants.INVITATION_REJECTED;
import static eu.europa.data.health.access.Constants.INVITATION_STATUS;
import static eu.europa.data.health.access.Constants.INVITED_BY;
import static eu.europa.data.health.access.Constants.INVITED_EMAIL;
import static eu.europa.data.health.access.Constants.JUSTIFICATION;
import static eu.europa.data.health.access.Constants.OWNERSHIP_REQUEST_COLLECTION;
import static eu.europa.data.health.access.Constants.ROLE;
import static eu.europa.data.health.access.Constants.ROLE_EDITOR;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaboratorManagerUnitTest {

  private static final String APP_ID = "app-1";
  private static final String OWNER_ID = "owner-1";
  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String OWNER_NAME = "Owner User";
  private static final String EDITOR_ID = "editor-1";
  private static final String EDITOR_EMAIL = "editor@example.com";
  private static final String EDITOR_NAME = "Editor User";

  @Mock
  private DBHandler dbHandler;
  @Mock
  private MongoClient mongoClient;
  @Mock
  private AuditLogManager auditLogManager;
  @Mock
  private NotificationManager notificationManager;

  private CollaboratorManager manager;

  @BeforeEach
  void setUp() {
    manager = new CollaboratorManager(dbHandler, mongoClient, auditLogManager, notificationManager);
    lenient().when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.succeededFuture());
    lenient().when(notificationManager.sendExternalEmailNotification(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.succeededFuture());
  }

  private RoutingContext ctx(String userId, String email, JsonObject body) {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body == null ? new JsonObject() : body);
    if (body == null) {
      RequestBody requestBody = mock(RequestBody.class);
      when(ctx.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(null);
    }

    User user = mock(User.class);
    JsonObject principal = new JsonObject()
      .put("preferred_username", userId)
      .put("email", email)
      .put("given_name", "Test")
      .put("family_name", "User");
    lenient().when(user.principal()).thenReturn(principal);
    lenient().when(user.get("preferred_username")).thenReturn(userId);
    lenient().when(ctx.user()).thenReturn(user);
    lenient().when(ctx.pathParam("applicationId")).thenReturn(APP_ID);
    return ctx;
  }

  private RoutingContext adminCtx(JsonObject body) {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body == null ? new JsonObject() : body);
    if (body == null) {
      RequestBody requestBody = mock(RequestBody.class);
      when(ctx.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(null);
    }

    User user = mock(User.class);
    JsonObject principal = new JsonObject()
      .put("preferred_username", "admin-user")
      .put("email", "admin@example.com")
      .put("given_name", "Admin")
      .put("family_name", "User")
      .put("realm_access", new JsonObject().put("roles", new JsonArray().add(DATA_ADMIN)));
    lenient().when(user.principal()).thenReturn(principal);
    lenient().when(user.get("preferred_username")).thenReturn("admin-user");
    lenient().when(ctx.user()).thenReturn(user);
    lenient().when(ctx.pathParam("applicationId")).thenReturn(APP_ID);
    return ctx;
  }

  private RoutingContext collaboratorCtx(String userId, JsonObject body, String collaboratorId) {
    RoutingContext ctx = ctx(userId, userId + "@example.com", body);
    when(ctx.pathParam("collaboratorId")).thenReturn(collaboratorId);
    return ctx;
  }

  private RoutingContext requestCtx(String userId, JsonObject body, String requestId) {
    RoutingContext ctx = ctx(userId, userId + "@example.com", body);
    when(ctx.pathParam("requestId")).thenReturn(requestId);
    return ctx;
  }

  private JsonObject collaboratorDoc(String collaboratorId, String targetUserId, String status, String role) {
    return new JsonObject()
      .put(ID, collaboratorId)
      .put(APPLICATION_ID, APP_ID)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION)
      .put(COLLABORATOR_USER_ID, targetUserId)
      .put(COLLABORATOR_USER_NAME, EDITOR_NAME)
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(INVITATION_STATUS, status)
      .put(ROLE, role)
      .put(INVITED_BY, OWNER_ID);
  }

  private JsonObject ownershipRequest(String requestId) {
    return new JsonObject()
      .put(ID, requestId)
      .put(APPLICATION_ID, APP_ID)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION)
      .put(USER_ID, EDITOR_ID)
      .put(USER_NAME, EDITOR_NAME)
      .put(JUSTIFICATION, "Please")
      .put(CREATED_AT, "2026-01-01T00:00:00Z");
  }

  private MongoClientDeleteResult deleteResult() {
    return mock(MongoClientDeleteResult.class);
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
    Method method = CollaboratorManager.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(manager, args);
  }

  private <T> T await(Future<T> future) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<T> value = new AtomicReference<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    future.onComplete(ar -> {
      if (ar.succeeded()) {
        value.set(ar.result());
      } else {
        error.set(ar.cause());
      }
      latch.countDown();
    });
    assertTrue(latch.await(2, TimeUnit.SECONDS));
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
    return value.get();
  }

  private Throwable awaitFailure(Future<?> future) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    future.onComplete(ar -> {
      if (ar.failed()) {
        error.set(ar.cause());
      }
      latch.countDown();
    });
    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertNotNull(error.get());
    return error.get();
  }

  // ---------------------------------------------------------------------------
  // Public method coverage - negative and alternate branches
  // ---------------------------------------------------------------------------

  @Test
  void getCollaborators_missingApplicationId_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject());
    when(ctx.pathParam("applicationId")).thenReturn(null);

    manager.getCollaborators(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void getCollaborators_forbiddenForUnauthorisedUser() {
    RoutingContext ctx = ctx("outsider", "outsider@example.com", new JsonObject());
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.getCollaborators(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void getCollaborators_adminBypassesOwnershipCheck() {
    RoutingContext ctx = adminCtx(new JsonObject());
    when(mongoClient.findWithOptions(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(FindOptions.class)))
      .thenReturn(Future.succeededFuture(List.of(new JsonObject().put(INVITED_EMAIL, EDITOR_EMAIL))));
    when(
      mongoClient.count(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class))
    ).thenReturn(Future.succeededFuture(1L));

    manager.getCollaborators(ctx);

    assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
  }

  @Test
  void getCollaborators_authorizationFailure_returnsServerError() {
    RoutingContext ctx = ctx("outsider", "outsider@example.com", new JsonObject());
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("db failure"));

    manager.getCollaborators(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void getCollaborators_queryFailure_returnsServerError() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject());
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findWithOptions(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(FindOptions.class)))
      .thenReturn(Future.failedFuture("query failure"));

    manager.getCollaborators(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_nullBody_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, null);

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_missingFields_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject());

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_nonOwner_returnsForbidden() {
    RoutingContext ctx = ctx("outsider", "outsider@example.com", new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_ownerCheckFailure_returnsServerError() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("owner lookup failed"));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_duplicateCheckFailure_returnsServerError() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("duplicate check failed"));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_userLookupFailure_returnsServerError() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)), Future.succeededFuture(null));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("user lookup failed"));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_insertFailure_returnsServerError() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.succeededFuture(new JsonObject().put(TITLE, "Title")));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.insert(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("insert failed"));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_notFound_returns404() {
    RoutingContext ctx = collaboratorCtx(OWNER_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_lookupFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(OWNER_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_forbiddenForNonOwnerAndNotSelf() {
    RoutingContext ctx = collaboratorCtx("outsider", new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_selfRemoval_returnsNoContent() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)), Future.succeededFuture(null));
    MongoClientDeleteResult del440 = deleteResult();
    when(mongoClient.removeDocument(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(del440));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_deleteFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(OWNER_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)), Future.succeededFuture(null));
    when(mongoClient.removeDocument(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("remove failed"));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptCollaboratorInvite_notFound_returns404() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.acceptCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptCollaboratorInvite_forbidden_returns403() {
    RoutingContext ctx = collaboratorCtx("other-user", new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_PENDING, ROLE_EDITOR)));

    manager.acceptCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptCollaboratorInvite_nonPending_returnsConflict() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));

    manager.acceptCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.CONFLICT.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptCollaboratorInvite_updateFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_PENDING, ROLE_EDITOR)));
    when(mongoClient.updateCollection(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("update failed"));

    manager.acceptCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptCollaboratorInvite_lookupFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.acceptCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectCollaboratorInvite_notFound_returns404() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.rejectCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectCollaboratorInvite_forbidden_returns403() {
    RoutingContext ctx = collaboratorCtx("other-user", new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_PENDING, ROLE_EDITOR)));

    manager.rejectCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectCollaboratorInvite_nonPending_returnsConflict() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_REJECTED, ROLE_EDITOR)));

    manager.rejectCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.CONFLICT.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectCollaboratorInvite_updateFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_PENDING, ROLE_EDITOR)));
    when(mongoClient.updateCollection(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("update failed"));

    manager.rejectCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectCollaboratorInvite_lookupFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.rejectCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_nullBody_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, null);

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_missingTargetId_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject());

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_targetNotFound_returns404() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject().put("newOwnerCollaboratorId", "missing"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_targetFromDifferentApplication_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject().put("newOwnerCollaboratorId", "collab-2"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-2", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)
        .put(APPLICATION_ID, "other-app")));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_targetWithoutAcceptedInvite_returnsBadRequest() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject().put("newOwnerCollaboratorId", "collab-2"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-2", null, INVITATION_PENDING, ROLE_EDITOR)));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_nonOwner_returnsForbidden() {
    RoutingContext ctx = ctx("outsider", "outsider@example.com", new JsonObject().put("newOwnerCollaboratorId", "collab-2"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-2", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_ownerCheckFailure_returns500() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject().put("newOwnerCollaboratorId", "collab-2"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-2", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("owner lookup failed"));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_resolveOwnershipActorFailure_returns500() {
    RoutingContext ctx = adminCtx(new JsonObject().put("newOwnerCollaboratorId", "collab-2"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-2", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject()));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void transferOwnership_lookupFailure_returns500() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject().put("newOwnerCollaboratorId", "collab-2"));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.transferOwnership(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_nullBody_returnsBadRequest() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, null);

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_blankJustification_returnsBadRequest() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject().put(JUSTIFICATION, "  "));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_adminNotFound_returns404() {
    RoutingContext ctx = adminCtx(new JsonObject().put(JUSTIFICATION, "Need ownership"));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_duplicateRequest_returns409() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject().put(JUSTIFICATION, "Need ownership"));
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, "req-1")));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.CONFLICT.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_duplicateCheckFailure_returns500() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject().put(JUSTIFICATION, "Need ownership"));
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("duplicate lookup failed"));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_ambiguousAdminApplicationType_returns500() {
    RoutingContext ctx = adminCtx(new JsonObject().put(JUSTIFICATION, "Need ownership"));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_insertFailure_returns500() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject().put(JUSTIFICATION, "Need ownership"));
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.insert(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("insert failed"));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void getOwnershipRequests_adminNotFound_returns404() {
    RoutingContext ctx = adminCtx(new JsonObject());
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    manager.getOwnershipRequests(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void getOwnershipRequests_forbiddenForNonOwner() {
    RoutingContext ctx = ctx("outsider", "outsider@example.com", new JsonObject());
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    manager.getOwnershipRequests(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void getOwnershipRequests_ownerLookupFailure_returns500() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject());
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.getOwnershipRequests(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void getOwnershipRequests_queryFailure_returns500() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject());
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findWithOptions(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), any(FindOptions.class)))
      .thenReturn(Future.failedFuture("query failed"));

    manager.getOwnershipRequests(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_notFound_returns404() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_wrongApplication_returnsBadRequest() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1").put(APPLICATION_ID, "other-app")));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_forbiddenForNonOwner() {
    RoutingContext ctx = requestCtx("outsider", new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_staleCollaborator_returnsConflict() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    MongoClientDeleteResult del860 = deleteResult();
    when(mongoClient.removeDocument(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(del860));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.CONFLICT.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_ownerCheckFailure_returns500() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("owner lookup failed"));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_collaboratorLookupFailure_returns500() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("collaborator lookup failed"));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_lookupFailure_returns500() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_notFound_returns404() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_wrongApplication_returnsBadRequest() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1").put(APPLICATION_ID, "other-app")));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_forbiddenForNonOwner() {
    RoutingContext ctx = requestCtx("outsider", new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.FORBIDDEN.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_deleteFailure_returns500() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));
    when(mongoClient.removeDocument(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("delete failed"));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_ownerCheckFailure_returns500() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("owner lookup failed"));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_lookupFailure_returns500() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.failedFuture("lookup failed"));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void getMyCollaborativeApplications_success_returnsPayload() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject());
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));

    manager.getMyCollaborativeApplications(ctx);

    assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
  }

  @Test
  void getMyCollaborativeApplications_failure_returns500() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject());
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("find failed"));

    manager.getMyCollaborativeApplications(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  // ---------------------------------------------------------------------------
  // Private helper coverage via reflection
  // ---------------------------------------------------------------------------

  @Test
  void resolveCollection_andParseIntParam_coverAllBranches() throws Exception {
    assertEquals(DATA_ACCESS_APPLICATION_COLLECTION,
      invokePrivate("resolveCollection", new Class[]{String.class}, DATA_ACCESS_APPLICATION_COLLECTION));
    assertEquals(DATA_REQUEST_APPLICATION_COLLECTION,
      invokePrivate("resolveCollection", new Class[]{String.class}, DATA_REQUEST_APPLICATION_COLLECTION));
    assertNull(invokePrivate("resolveCollection", new Class[]{String.class}, "UNKNOWN"));

    assertEquals(5, ((Integer) invokePrivate("parseIntParam", new Class[]{String.class, int.class}, "5", 0)).intValue());
    assertEquals(7, ((Integer) invokePrivate("parseIntParam", new Class[]{String.class, int.class}, "", 7)).intValue());
    assertEquals(9, ((Integer) invokePrivate("parseIntParam", new Class[]{String.class, int.class}, "not-int", 9)).intValue());
  }

  @Test
  void buildCollaboratorListQuery_coversBlankAndSearch() throws Exception {
    JsonObject base = new JsonObject().put(APPLICATION_ID, APP_ID);
    JsonObject blank = await(invokePrivate("buildCollaboratorListQuery",
      new Class[]{JsonObject.class, String.class}, base, null));
    assertEquals(base, blank);

    JsonObject searched = await(invokePrivate("buildCollaboratorListQuery",
      new Class[]{JsonObject.class, String.class}, base, "papad"));
    assertNotNull(searched.getJsonArray("$or"));
    assertEquals(2, searched.getJsonArray("$or").size());
  }

  @Test
  void resolveAcceptedEditorCollaboration_coversNoneOneAndMultiple() throws Exception {
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of()));
    Future<JsonObject> noneFuture = invokePrivate("resolveAcceptedEditorCollaboration",
      new Class[]{String.class, String.class}, APP_ID, EDITOR_ID);
    assertNull(await(noneFuture));

    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));
    Future<JsonObject> oneFuture = invokePrivate("resolveAcceptedEditorCollaboration",
      new Class[]{String.class, String.class}, APP_ID, EDITOR_ID);
    assertEquals("collab-1", await(oneFuture).getString(ID));

    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(
        collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR),
        collaboratorDoc("collab-2", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));
    Future<JsonObject> manyFuture = invokePrivate("resolveAcceptedEditorCollaboration",
      new Class[]{String.class, String.class}, APP_ID, EDITOR_ID);
    assertTrue(awaitFailure(manyFuture).getMessage().contains("Multiple accepted editor records"));
  }

  @Test
  void resolveOwnedApplicationType_andResolveApplicationType_coverAllOutcomes() throws Exception {
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    Future<String> ownedAccess = invokePrivate("resolveOwnedApplicationType",
      new Class[]{String.class, String.class}, APP_ID, OWNER_ID);
    assertEquals(DATA_ACCESS_APPLICATION_COLLECTION, await(ownedAccess));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    Future<String> anyRequest = invokePrivate("resolveApplicationType",
      new Class[]{String.class}, APP_ID);
    assertEquals(DATA_REQUEST_APPLICATION_COLLECTION, await(anyRequest));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    Future<String> ambiguous = invokePrivate("resolveApplicationType",
      new Class[]{String.class}, APP_ID);
    assertTrue(awaitFailure(ambiguous).getMessage().contains("ambiguous"));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    Future<String> none = invokePrivate("resolveApplicationType",
      new Class[]{String.class}, APP_ID);
    assertNull(await(none));
  }

  @Test
  void getApplicationOwnerId_fetchApplicationTitle_andResolveUserIdentity_coverFallbacks() throws Exception {
    Future<String> unknownOwner = invokePrivate("getApplicationOwnerId",
      new Class[]{String.class, String.class}, APP_ID, "UNKNOWN");
    assertTrue(awaitFailure(unknownOwner).getMessage().contains("Unknown applicationType"));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.failedFuture("title lookup failed"));
    Future<String> owner = invokePrivate("getApplicationOwnerId",
      new Class[]{String.class, String.class}, APP_ID, DATA_ACCESS_APPLICATION_COLLECTION);
    assertEquals(OWNER_ID, await(owner));

    Future<String> titleUnknown = invokePrivate("fetchApplicationTitle",
      new Class[]{String.class, String.class}, APP_ID, "UNKNOWN");
    assertNull(await(titleUnknown));

    Future<String> titleFailure = invokePrivate("fetchApplicationTitle",
      new Class[]{String.class, String.class}, APP_ID, DATA_ACCESS_APPLICATION_COLLECTION);
    assertNull(await(titleFailure));

    Future<JsonObject> blankIdentity = invokePrivate("resolveUserIdentity",
      new Class[]{String.class, String.class, String.class}, "", "Fallback User", "fallback@example.com");
    assertEquals("Fallback User", await(blankIdentity).getString(USER_NAME));

    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_NAME, EDITOR_NAME).put(EMAIL, EDITOR_EMAIL)));
    Future<JsonObject> resolvedIdentity = invokePrivate("resolveUserIdentity",
      new Class[]{String.class, String.class, String.class}, EDITOR_ID, null, null);
    JsonObject resolved = await(resolvedIdentity);
    assertEquals(EDITOR_NAME, resolved.getString(USER_NAME));
    assertEquals(EDITOR_EMAIL, resolved.getString(EMAIL));
  }

  @Test
  void isOwnerHelpers_coverCollaboratorAndOwnerLookups() throws Exception {
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    Future<Boolean> collaboratorAccess = invokePrivate("isOwnerOrCollaborator",
      new Class[]{String.class, String.class}, APP_ID, EDITOR_ID);
    assertTrue(await(collaboratorAccess));

    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    Future<Boolean> ownerInAccess = invokePrivate("isOwnerInAnyCollection",
      new Class[]{String.class, String.class}, APP_ID, OWNER_ID);
    assertTrue(await(ownerInAccess));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    Future<Boolean> ownerInRequest = invokePrivate("isOwnerInAnyCollection",
      new Class[]{String.class, String.class}, APP_ID, OWNER_ID);
    assertTrue(await(ownerInRequest));

    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    Future<Boolean> noAccess = invokePrivate("isOwnerOrCollaborator",
      new Class[]{String.class, String.class}, APP_ID, "ghost");
    assertFalse(await(noAccess));
  }

  @Test
  void adminAndFormattingHelpers_coverAllBranches() throws Exception {
    assertTrue((Boolean) invokePrivate("containsAdminRole", new Class[]{Object.class}, new JsonArray().add(DATA_ADMIN)));
    assertTrue((Boolean) invokePrivate("containsAdminRole", new Class[]{Object.class}, List.of(DATA_ADMIN)));
    assertTrue((Boolean) invokePrivate("containsAdminRole", new Class[]{Object.class}, "data-admin other-role"));
    assertFalse((Boolean) invokePrivate("containsAdminRole", new Class[]{Object.class}, 42));

    RoutingContext noUserCtx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(noUserCtx.user()).thenReturn(null);
    assertFalse((Boolean) invokePrivate("isAdmin", new Class[]{RoutingContext.class}, noUserCtx));

    RoutingContext topLevelRoleCtx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject());
    JsonObject topPrincipal = new JsonObject().put("roles", new JsonArray().add(DATA_ADMIN));
    User topUser = mock(User.class);
    when(topUser.principal()).thenReturn(topPrincipal);
    when(topLevelRoleCtx.user()).thenReturn(topUser);
    assertTrue((Boolean) invokePrivate("isAdmin", new Class[]{RoutingContext.class}, topLevelRoleCtx));

    RoutingContext realmRoleCtx = adminCtx(new JsonObject());
    assertTrue((Boolean) invokePrivate("isAdmin", new Class[]{RoutingContext.class}, realmRoleCtx));

    assertEquals(APP_ID, invokePrivate("formatApplicationReference",
      new Class[]{String.class, String.class}, APP_ID, null));
    assertEquals(APP_ID + " – Title", invokePrivate("formatApplicationReference",
      new Class[]{String.class, String.class}, APP_ID, "Title"));
    assertEquals("User", invokePrivate("formatUserIdentity",
      new Class[]{String.class, String.class, String.class}, "User", null, "fallback"));
    assertEquals("User / user@example.com", invokePrivate("formatUserIdentity",
      new Class[]{String.class, String.class, String.class}, "User", "user@example.com", "fallback"));
  }

  @Test
  void resolveOwnershipActor_andExecuteOwnershipTransfer_coverFailureAndFallbacks() throws Exception {
    Future<JsonObject> blankOwner = invokePrivate("resolveOwnershipActor",
      new Class[]{String.class, String.class, String.class, String.class, String.class},
      APP_ID, DATA_ACCESS_APPLICATION_COLLECTION, "", OWNER_NAME, OWNER_EMAIL);
    assertTrue(awaitFailure(blankOwner).getMessage().contains("Application owner not found"));

    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject()
        .put(COLLABORATOR_USER_NAME, OWNER_NAME)
        .put(INVITED_EMAIL, OWNER_EMAIL)));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_NAME, "Fallback").put(EMAIL, "fallback@example.com")));
    Future<JsonObject> resolvedOwner = invokePrivate("resolveOwnershipActor",
      new Class[]{String.class, String.class, String.class, String.class, String.class},
      APP_ID, DATA_ACCESS_APPLICATION_COLLECTION, OWNER_ID, null, null);
    JsonObject owner = await(resolvedOwner);
    assertEquals(OWNER_NAME, owner.getString(USER_NAME));
    assertEquals(OWNER_EMAIL, owner.getString(EMAIL));

    Future<Void> unknownCollection = invokePrivate("executeOwnershipTransfer",
      new Class[]{String.class, String.class, String.class, String.class, String.class,
        String.class, JsonObject.class, RoutingContext.class, NotificationCode.class,
        String.class, String.class, String.class, String.class},
      APP_ID, "UNKNOWN", OWNER_ID, OWNER_NAME, OWNER_EMAIL, OWNER_ID,
      collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR),
      ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()),
      NotificationCode.COLLABORATION_OWNERSHIP_TRANSFERRED,
      OWNER_NAME, "Application", "Application updated", null);
    assertTrue(awaitFailure(unknownCollection).getMessage().contains("Unknown applicationType"));

    Future<Void> missingFields = invokePrivate("executeOwnershipTransfer",
      new Class[]{String.class, String.class, String.class, String.class, String.class,
        String.class, JsonObject.class, RoutingContext.class, NotificationCode.class,
        String.class, String.class, String.class, String.class},
      APP_ID, DATA_ACCESS_APPLICATION_COLLECTION, OWNER_ID, OWNER_NAME, OWNER_EMAIL, OWNER_ID,
      new JsonObject(), ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()),
      NotificationCode.COLLABORATION_OWNERSHIP_TRANSFERRED,
      OWNER_NAME, "Application", "Application updated", null);
    assertTrue(awaitFailure(missingFields).getMessage().contains("missing required fields"));
  }

  @Test
  void addCollaborator_adminSuccess_coversActorLabelAndExternalEmailFailure() {
    RoutingContext ctx = adminCtx(new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.succeededFuture(new JsonObject().put(TITLE, "Application title")));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.insert(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("new-id"));
    when(notificationManager.sendExternalEmailNotification(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.failedFuture("email failed"));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.CREATED.code(), ctx.response().getStatusCode());
  }

  @Test
  void addCollaborator_successWithLinkedUser_coversNotificationFailure() {
    RoutingContext ctx = ctx(OWNER_ID, OWNER_EMAIL, new JsonObject()
      .put(INVITED_EMAIL, EDITOR_EMAIL)
      .put(APPLICATION_TYPE_FIELD, DATA_ACCESS_APPLICATION_COLLECTION));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.succeededFuture(new JsonObject().put(TITLE, "Application title")));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject()
        .put(ID, EDITOR_ID)
        .put(USER_NAME, EDITOR_NAME)));
    when(mongoClient.insert(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("new-id"));
    when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.failedFuture("notification failed"));

    manager.addCollaborator(ctx);

    assertEquals(HttpResponseStatus.CREATED.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_ownerCheckFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(OWNER_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("owner lookup failed"));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_titleLookupFailure_returns500() {
    RoutingContext ctx = collaboratorCtx(OWNER_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.failedFuture("title lookup failed"));
    MongoClientDeleteResult delResult = deleteResult();
    when(mongoClient.removeDocument(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(delResult));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_selfRemoval_coversNotificationFailure() {
    RoutingContext ctx = collaboratorCtx(EDITOR_ID, new JsonObject(), "collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.succeededFuture(new JsonObject().put(TITLE, "Title")));
    MongoClientDeleteResult del1313 = deleteResult();
    when(mongoClient.removeDocument(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(del1313));
    when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.failedFuture("notification failed"));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
  }

  @Test
  void removeCollaborator_adminRemoval_coversActorLabelAndNotificationFailure() {
    RoutingContext ctx = adminCtx(new JsonObject());
    when(ctx.pathParam("collaboratorId")).thenReturn("collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.succeededFuture(new JsonObject().put(TITLE, "Title")));
    MongoClientDeleteResult del1332 = deleteResult();
    when(mongoClient.removeDocument(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(del1332));
    when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.failedFuture("notification failed"));

    manager.removeCollaborator(ctx);

    assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptCollaboratorInvite_adminSuccess_coversNotificationFailure() {
    RoutingContext ctx = adminCtx(new JsonObject());
    when(ctx.pathParam("collaboratorId")).thenReturn("collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_PENDING, ROLE_EDITOR)
        .put(APPLICATION_TITLE, "Title")));
    when(mongoClient.updateCollection(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mock(io.vertx.ext.mongo.MongoClientUpdateResult.class)));
    when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.failedFuture("notification failed"));

    manager.acceptCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectCollaboratorInvite_adminSuccess_coversNotificationFailure() {
    RoutingContext ctx = adminCtx(new JsonObject());
    when(ctx.pathParam("collaboratorId")).thenReturn("collab-1");
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_PENDING, ROLE_EDITOR)
        .put(APPLICATION_TITLE, "Title")));
    when(mongoClient.updateCollection(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mock(io.vertx.ext.mongo.MongoClientUpdateResult.class)));
    when(notificationManager.createNotifications(any(), anyString(), anyString(), anyMap(), anyString()))
      .thenReturn(Future.failedFuture("notification failed"));

    manager.rejectCollaboratorInvite(ctx);

    assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
  }

  @Test
  void requestOwnership_followUpPreparationFailure_stillReturnsCreated() {
    RoutingContext ctx = ctx(EDITOR_ID, EDITOR_EMAIL, new JsonObject().put(JUSTIFICATION, "Need ownership"));
    when(mongoClient.find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR))));
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.insert(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("req-new"));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("user lookup failed"));

    manager.requestOwnership(ctx);

    assertEquals(HttpResponseStatus.CREATED.code(), ctx.response().getStatusCode());
  }

  @Test
  void acceptOwnershipRequest_cleanupFailureRecovered_stillReturnsOk() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)),
        Future.succeededFuture(new JsonObject().put(TITLE, "Title")));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(collaboratorDoc("collab-1", EDITOR_ID, INVITATION_ACCEPTED, ROLE_EDITOR)));
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(COLLABORATOR_USER_NAME, OWNER_NAME).put(INVITED_EMAIL, OWNER_EMAIL)));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_NAME, OWNER_NAME).put(EMAIL, OWNER_EMAIL)),
        Future.succeededFuture(new JsonObject().put(USER_NAME, EDITOR_NAME).put(EMAIL, EDITOR_EMAIL)));
    io.vertx.ext.mongo.MongoClientUpdateResult mockUpdate1 = mock(io.vertx.ext.mongo.MongoClientUpdateResult.class);
    io.vertx.ext.mongo.MongoClientUpdateResult mockUpdate2 = mock(io.vertx.ext.mongo.MongoClientUpdateResult.class);
    when(mongoClient.updateCollection(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mockUpdate1),
        Future.succeededFuture(mockUpdate2));
    io.vertx.ext.mongo.MongoClientUpdateResult mockUpdate3 = mock(io.vertx.ext.mongo.MongoClientUpdateResult.class);
    when(mongoClient.updateCollectionWithOptions(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture(mockUpdate3));
    MongoClientDeleteResult del1412 = deleteResult();
    when(mongoClient.removeDocument(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(del1412));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mock(io.vertx.ext.mongo.MongoClientUpdateResult.class)));
    when(mongoClient.removeDocuments(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("cleanup failed"));

    manager.acceptOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode());
  }

  @Test
  void rejectOwnershipRequest_followUpPreparationFailure_stillReturnsNoContent() {
    RoutingContext ctx = requestCtx(OWNER_ID, new JsonObject(), "req-1");
    when(mongoClient.findOne(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(ownershipRequest("req-1")));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(USER_ID, OWNER_ID)));
    MongoClientDeleteResult del1431 = deleteResult();
    when(mongoClient.removeDocument(eq(OWNERSHIP_REQUEST_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(del1431));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("user lookup failed"));

    manager.rejectOwnershipRequest(ctx);

    assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode());
  }

  @Test
  void checkDuplicateCollaborator_andResolveCollaboratorUserByEmail_coverDirectHelpers() throws Exception {
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, "dup")), Future.succeededFuture(null));
    Future<Boolean> duplicate = invokePrivate("checkDuplicateCollaborator",
      new Class[]{String.class, String.class, String.class}, APP_ID, DATA_ACCESS_APPLICATION_COLLECTION, EDITOR_EMAIL);
    assertTrue(await(duplicate));
    Future<Boolean> notDuplicate = invokePrivate("checkDuplicateCollaborator",
      new Class[]{String.class, String.class, String.class}, APP_ID, DATA_ACCESS_APPLICATION_COLLECTION, EDITOR_EMAIL);
    assertFalse(await(notDuplicate));

    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, EDITOR_ID).put(USER_NAME, EDITOR_NAME)));
    Future<JsonObject> resolvedUser = invokePrivate("resolveCollaboratorUserByEmail",
      new Class[]{String.class}, EDITOR_EMAIL);
    assertEquals(EDITOR_ID, await(resolvedUser).getString(ID));
  }

  @Test
  void checkDuplicateCollaborator_usesCaseInsensitiveEmailMatch() throws Exception {
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    when(
      mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), queryCaptor.capture(), isNull())
    ).thenReturn(Future.succeededFuture(new JsonObject().put(ID, "dup")));

    Future<Boolean> result = invokePrivate(
      "checkDuplicateCollaborator",
      new Class[] {String.class, String.class, String.class},
      APP_ID,
      DATA_ACCESS_APPLICATION_COLLECTION,
      "Editor@Example.COM"
    );
    assertTrue(await(result));

    JsonObject capturedQuery = queryCaptor.getValue();
    JsonObject emailFilter = capturedQuery.getJsonObject(INVITED_EMAIL);
    assertNotNull(
      emailFilter,
      "invitedEmail should use a regex filter for case-insensitive matching"
    );
    assertEquals("i", emailFilter.getString("$options"));
    assertTrue(
      emailFilter.getString("$regex").contains("Editor@Example.COM"),
      "regex should contain the original email for anchored case-insensitive match"
    );
  }

  @Test
  void resolveCollaboratorUserByEmail_usesCaseInsensitiveEmailMatch() throws Exception {
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    when(
      mongoClient.findOne(eq(USER_COLLECTION), queryCaptor.capture(), any(JsonObject.class))
    ).thenReturn(
      Future.succeededFuture(new JsonObject().put(ID, EDITOR_ID).put(USER_NAME, EDITOR_NAME))
    );

    Future<JsonObject> resolved = invokePrivate(
      "resolveCollaboratorUserByEmail",
      new Class[] {String.class},
      "Editor@Example.COM"
    );
    assertEquals(EDITOR_ID, await(resolved).getString(ID));

    JsonObject capturedQuery = queryCaptor.getValue();
    JsonObject emailFilter = capturedQuery.getJsonObject(EMAIL);
    assertNotNull(emailFilter, "email should use a regex filter for case-insensitive matching");
    assertEquals("i", emailFilter.getString("$options"));
    assertTrue(
      emailFilter.getString("$regex").contains("Editor@Example.COM"),
      "regex should contain the original email for anchored case-insensitive match"
    );
  }

  @Test
  void resolveOwnedApplicationType_additionalBranches_and_isAdminPrincipalNull() throws Exception {
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    Future<String> requestOwned = invokePrivate("resolveOwnedApplicationType",
      new Class[]{String.class, String.class}, APP_ID, OWNER_ID);
    assertEquals(DATA_REQUEST_APPLICATION_COLLECTION, await(requestOwned));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject().put(ID, APP_ID)));
    Future<String> ambiguousOwned = invokePrivate("resolveOwnedApplicationType",
      new Class[]{String.class, String.class}, APP_ID, OWNER_ID);
    assertTrue(awaitFailure(ambiguousOwned).getMessage().contains("ambiguous"));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    Future<String> noneOwned = invokePrivate("resolveOwnedApplicationType",
      new Class[]{String.class, String.class}, APP_ID, OWNER_ID);
    assertNull(await(noneOwned));

    RoutingContext principalNullCtx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    User principalNullUser = mock(User.class);
    when(principalNullCtx.user()).thenReturn(principalNullUser);
    when(principalNullUser.principal()).thenReturn(null);
    assertFalse((Boolean) invokePrivate("isAdmin", new Class[]{RoutingContext.class}, principalNullCtx));
  }

  @Test
  void getApplicationOwnerId_andFetchApplicationTitle_coverNullDocs() throws Exception {
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null), Future.succeededFuture(null));

    Future<String> owner = invokePrivate("getApplicationOwnerId",
      new Class[]{String.class, String.class}, APP_ID, DATA_ACCESS_APPLICATION_COLLECTION);
    assertNull(await(owner));

    Future<String> title = invokePrivate("fetchApplicationTitle",
      new Class[]{String.class, String.class}, APP_ID, DATA_ACCESS_APPLICATION_COLLECTION);
    assertNull(await(title));
  }

  @Test
  void resolveOwnershipActor_fallsBackWhenNoStoredIdentityExists() throws Exception {
    when(mongoClient.findOne(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));
    when(mongoClient.findOne(eq(USER_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    Future<JsonObject> future = invokePrivate("resolveOwnershipActor",
      new Class[]{String.class, String.class, String.class, String.class, String.class},
      APP_ID, DATA_ACCESS_APPLICATION_COLLECTION, OWNER_ID, "Fallback Owner", "fallback@example.com");
    JsonObject resolved = await(future);
    assertEquals("Fallback Owner", resolved.getString(USER_NAME));
    assertEquals("fallback@example.com", resolved.getString(EMAIL));
  }
}
