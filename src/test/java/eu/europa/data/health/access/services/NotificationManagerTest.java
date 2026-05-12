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

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.NOTIFICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.NotificationKeys.APPLICATION_NAME;
import static eu.europa.data.health.access.NotificationKeys.DATASET_NAME;
import static eu.europa.data.health.access.NotificationKeys.EMAIL_NOTIFICATION_PREFERENCES;
import static eu.europa.data.health.access.NotificationKeys.ENABLED;
import static eu.europa.data.health.access.NotificationKeys.HDAB_NAME;
import static eu.europa.data.health.access.NotificationKeys.NOTIFICATION_DAY;
import static eu.europa.data.health.access.NotificationKeys.OFFSET_DAYS;
import static eu.europa.data.health.access.NotificationKeys.REMINDERS;
import static eu.europa.data.health.access.NotificationKeys.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.mail.MailClient;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationManagerTest {

  @Mock
  private DBHandler dbHandler;

  @Mock
  private MailClient mailClient;

  @Mock
  private RoutingContext routingContext;

  @Mock
  private RequestBody requestBody;

  @Mock
  private HttpServerResponse response;

  @Mock
  private HttpServerRequest request;

  private NotificationManager notificationManager;

  @BeforeEach
  void setUp() {
    notificationManager = new NotificationManager(dbHandler, mailClient, "http://test-base-url");
  }

  @Test
  void createApplicationNotification_shouldCreateDbEntry() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    Future<String> result = notificationManager.createApplicationNotification(
      NotificationCode.APPLICATION_SUBMIT,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());
    assertEquals("notification-id", result.result());

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(dbHandler).create(eq(NOTIFICATION_COLLECTION), captor.capture());

    JsonObject saved = captor.getValue();
    assertEquals("app-1", saved.getString("applicationId"));
    assertEquals("APPLICATION", saved.getString("notificationType"));
    assertEquals("DATA_ACCESS", saved.getString("applicationType"));
    assertEquals(NotificationCode.APPLICATION_SUBMIT.getCode(), saved.getString("notificationCode"));
    assertEquals(false, saved.getBoolean("displayed"));
    assertEquals("user-1", saved.getString(USER_ID));

    JsonObject notificationProperties = saved.getJsonObject("notificationProperties");
    assertEquals("App A", notificationProperties.getString(APPLICATION_NAME));
    assertEquals("HDAB X", notificationProperties.getString(HDAB_NAME));
  }

  @Test
  void createNotifications_shouldCreateDbAndSendEmail_whenUserHasEmailAndPreferencesAllow() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, new JsonObject().put(ENABLED, true));

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    when(mailClient.sendMail(any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT,
      "app-1",
      DATA_ACCESS_APPLICATION_COLLECTION,
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());

    verify(dbHandler).create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class));
    verify(dbHandler).findById(USER_COLLECTION, "user-1");
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldCreateDbButSkipEmail_whenUserHasNoEmail() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    JsonObject user = new JsonObject();

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());

    verify(dbHandler).create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class));
    verify(dbHandler).findById(USER_COLLECTION, "user-1");
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSkipEmail_whenGlobalEmailDisabled() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    JsonObject prefs = new JsonObject().put(ENABLED, false);
    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSkipReminderEmail_whenOffsetNotAllowed() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(DATASET_NAME, "Dataset A");
    properties.put(NOTIFICATION_DAY, "5");

    JsonObject reminderPrefs = new JsonObject()
      .put(ENABLED, true)
      .put(OFFSET_DAYS, new JsonArray().add(10).add(30));

    JsonObject prefs = new JsonObject()
      .put(ENABLED, true)
      .put(REMINDERS, reminderPrefs);

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSendReminderEmail_whenOffsetAllowed() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(DATASET_NAME, "Dataset A");
    properties.put(NOTIFICATION_DAY, "10");

    JsonObject reminderPrefs = new JsonObject()
      .put(ENABLED, true)
      .put(OFFSET_DAYS, new JsonArray().add(10).add(30));

    JsonObject prefs = new JsonObject()
      .put(ENABLED, true)
      .put(REMINDERS, reminderPrefs);

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    when(mailClient.sendMail(any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSendWithRenewReminderEmail_whenOffsetAllowed() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(DATASET_NAME, "Dataset A");
    properties.put(NOTIFICATION_DAY, "10");

    JsonObject reminderPrefs = new JsonObject()
      .put(ENABLED, true)
      .put(OFFSET_DAYS, new JsonArray().add(10).add(30));

    JsonObject prefs = new JsonObject()
      .put(ENABLED, true)
      .put(REMINDERS, reminderPrefs);

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    when(mailClient.sendMail(any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());

    ArgumentCaptor<JsonObject> payloadCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mailClient).sendMail(payloadCaptor.capture(), any());

    assertEquals(
      "Data Permit <strong>Dataset A</strong> for Application <strong>App A</strong> is approaching expiration.",
      payloadCaptor.getValue().getJsonObject("templateModel").getString("message")
    );
  }

  @Test
  void createNotifications_shouldSkipWithRenewReminderEmail_whenOffsetNotAllowed() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(DATASET_NAME, "Dataset A");
    properties.put(NOTIFICATION_DAY, "5");

    JsonObject reminderPrefs = new JsonObject()
      .put(ENABLED, true)
      .put(OFFSET_DAYS, new JsonArray().add(10).add(30));

    JsonObject prefs = new JsonObject()
      .put(ENABLED, true)
      .put(REMINDERS, reminderPrefs);

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("notification-id"));

    when(dbHandler.findById(USER_COLLECTION, "user-1"))
      .thenReturn(Future.succeededFuture(user));

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW,
      "app-1",
      "DATA_ACCESS",
      properties,
      "user-1"
    );

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void sendExternalEmailNotification_shouldSkipWhenEmailBlank() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    Future<Void> result = notificationManager.sendExternalEmailNotification(
      NotificationCode.APPLICATION_SUBMIT,
      "app-id-123",
      DATA_REQUEST_APPLICATION_COLLECTION,
      properties,
      "   "
    );

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void sendExternalEmailNotification_shouldSendWhenEmailProvided() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    when(mailClient.sendMail(any(JsonObject.class), any()))
      .thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.sendExternalEmailNotification(
      NotificationCode.APPLICATION_SUBMIT,
      "app-id-123",
      DATA_REQUEST_APPLICATION_COLLECTION,
      properties,
      "external@example.com"
    );

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void readNotifications_shouldReturn400_whenNotificationIdsMissing() {
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);

    notificationManager.readNotifications(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end("notificationIds is required");
    verify(dbHandler, never()).updateMany(any(), any(), any());
  }

  @Test
  void readNotifications_shouldReturn400_whenNotificationIdsEmpty() {
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject().put("notificationIds", new JsonArray()));
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);

    notificationManager.readNotifications(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end("notificationIds cannot be empty");
    verify(dbHandler, never()).updateMany(any(), any(), any());
  }

  @Test
  void readNotifications_shouldMarkAsRead_whenIdsProvided() {
    JsonArray ids = new JsonArray().add("id-1").add("id-2");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject().put("notificationIds", ids));
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(200)).thenReturn(response);

    when(dbHandler.updateMany(eq(NOTIFICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(new JsonObject()));

    notificationManager.readNotifications(routingContext);

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);

    verify(dbHandler).updateMany(
      eq(NOTIFICATION_COLLECTION),
      queryCaptor.capture(),
      updateCaptor.capture()
    );

    JsonObject query = queryCaptor.getValue();
    JsonObject update = updateCaptor.getValue();

    assertEquals(ids, query.getJsonObject("_id").getJsonArray("$in"));
    assertTrue(update.getJsonObject("$set").getBoolean("displayed"));

    verify(response).setStatusCode(200);
    verify(response).end();
  }

  @Test
  void getUnreadNotificationCount_shouldReturnCount() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(200)).thenReturn(response);

      when(dbHandler.countAll(
        eq(NOTIFICATION_COLLECTION),
        eq(new JsonObject().put(USER_ID, "user-1").put("displayed", false))
      )).thenReturn(Future.succeededFuture(7));

      notificationManager.getUnreadNotificationCount(routingContext);

      verify(response).setStatusCode(200);
      verify(response).end("{\"unread_notification_count\":7}");
    }
  }

  @Test
  void deleteNotifications_shouldReturn401_whenUserMissing() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("");

      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(401)).thenReturn(response);

      notificationManager.deleteNotifications(routingContext);

      verify(response).setStatusCode(401);
      verify(response).end();
      verify(dbHandler, never()).deleteAll(any(), any());
    }
  }

  @Test
  void deleteNotifications_shouldDeleteSpecificNotifications_whenIdsProvided() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      JsonArray ids = new JsonArray().add("id-1").add("id-2");

      when(routingContext.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(new JsonObject().put("notificationIds", ids));
      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(204)).thenReturn(response);

      doReturn(Future.succeededFuture(2))
        .when(dbHandler)
        .deleteAll(eq(NOTIFICATION_COLLECTION), any(JsonObject.class));

      notificationManager.deleteNotifications(routingContext);

      ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
      verify(dbHandler).deleteAll(eq(NOTIFICATION_COLLECTION), queryCaptor.capture());

      JsonObject query = queryCaptor.getValue();
      assertEquals("user-1", query.getString(USER_ID));
      assertEquals(ids, query.getJsonObject("_id").getJsonArray("$in"));

      verify(response).setStatusCode(204);
      verify(response).end();
    }
  }

  @Test
  void deleteNotifications_shouldDeleteAllForUser_whenNoIdsProvided() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(new JsonObject());
      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(204)).thenReturn(response);

      when(dbHandler.deleteAll(eq(NOTIFICATION_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(5));

      notificationManager.deleteNotifications(routingContext);

      ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
      verify(dbHandler).deleteAll(eq(NOTIFICATION_COLLECTION), queryCaptor.capture());

      JsonObject query = queryCaptor.getValue();
      assertEquals("user-1", query.getString(USER_ID));
      assertFalse(query.containsKey("_id"));

      verify(response).setStatusCode(204);
      verify(response).end();
    }
  }

  @Test
  void sendExternalEmailNotification_shouldSkipWhenEmailNull() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(APPLICATION_NAME, "App A");
    properties.put(HDAB_NAME, "HDAB X");

    Future<Void> result = notificationManager.sendExternalEmailNotification(
      NotificationCode.APPLICATION_SUBMIT, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
      properties, null);

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationCode.class, names = {
    "APPLICATION_RESUBMIT", "CATALOG_REJECTED", "CATALOG_AWAIT_ADDITIONAL_INFORMATION",
    "CATALOG_PROCESSING", "CATALOG_PROCESSING_EXTENDED", "CATALOG_WITHDRAW"
  })
  void createNotifications_applicationNotificationVariants_sendEmail(NotificationCode code) {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "My App");
    props.put(HDAB_NAME, "HDAB Y");

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, new JsonObject().put(ENABLED, true));

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      code, "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationCode.class, names = {
    "DATASET_POSITIVE_DECISION_ISSUED", "DATASET_NEGATIVE_DECISION_ISSUED",
    "DATASET_APPEAL_REJECTED", "PERMIT_GRANTED", "PERMIT_RENEWED",
    "PERMIT_RENEWAL_REJECTED", "PERMIT_AMENDED", "PERMIT_AMENDMENT_REJECTED",
    "PERMIT_EXPIRED", "PERMIT_REVOKED", "PERMIT_APPEAL_APPROVED", "PERMIT_APPEAL_REJECTED"
  })
  void createNotifications_dataPermitVariants_sendEmail(NotificationCode code) {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "My App");
    props.put(DATASET_NAME, "Dataset X");

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, new JsonObject().put(ENABLED, true));

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      code, "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationCode.class, names = {
    "SCHEDULED_DATASET_WITHDRAWN_APPROACHING", "SCHEDULED_CATALOG_WITHDRAWN_APPROACHING"
  })
  void createNotifications_withdrawalReminderVariants_sendEmail(NotificationCode code) {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "My App");
    props.put(DATASET_NAME, "Dataset X");
    props.put(NOTIFICATION_DAY, "10");

    JsonObject reminderPrefs = new JsonObject().put(ENABLED, true)
      .put(OFFSET_DAYS, new JsonArray().add(10));
    JsonObject prefs = new JsonObject().put(ENABLED, true).put(REMINDERS, reminderPrefs);
    JsonObject user = new JsonObject().put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      code, "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationCode.class, names = {
    "COLLABORATION_INVITE_SENT", "COLLABORATOR_REMOVED_BY_OWNER", "COLLABORATOR_SELF_REMOVED",
    "COLLABORATION_OWNERSHIP_TRANSFERRED", "COLLABORATION_INVITE_ACCEPTED",
    "COLLABORATION_INVITE_REJECTED", "COLLABORATION_OWNERSHIP_REQUESTED",
    "COLLABORATION_OWNERSHIP_REQUEST_APPROVED", "COLLABORATION_OWNERSHIP_REQUEST_REJECTED",
    "COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN", "APPLICATION_DISCARDED_BY_OWNER"
  })
  void createNotifications_collaborationVariants_sendEmail(NotificationCode code) {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "My App");
    props.put(USER_NAME, "John Doe");

    JsonObject user = new JsonObject()
      .put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, new JsonObject().put(ENABLED, true));

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      code, "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSendEmail_whenUserHasNoPreferences() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(HDAB_NAME, "HDAB X");

    // User exists with email but no emailNotificationPreferences field
    JsonObject user = new JsonObject().put(EMAIL, "user@example.com");

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT, "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props,
      "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSendReminderEmail_whenReminderPrefsNull() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(DATASET_NAME, "Dataset A");
    props.put(NOTIFICATION_DAY, "10");

    // Global enabled=true but no reminders key → reminderPrefs is null → return true
    JsonObject prefs = new JsonObject().put(ENABLED, true); // no REMINDERS key
    JsonObject user = new JsonObject().put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSkipReminderEmail_whenRemindersDisabled() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(DATASET_NAME, "Dataset A");
    props.put(NOTIFICATION_DAY, "10");

    JsonObject reminderPrefs = new JsonObject().put(ENABLED, false);
    JsonObject prefs = new JsonObject().put(ENABLED, true).put(REMINDERS, reminderPrefs);
    JsonObject user = new JsonObject().put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSendReminderEmail_whenOffsetDaysNull() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(DATASET_NAME, "Dataset A");
    props.put(NOTIFICATION_DAY, "10");

    // reminders enabled but no offsetDays → allowedOffsets is null → return true
    JsonObject reminderPrefs = new JsonObject().put(ENABLED, true); // no OFFSET_DAYS
    JsonObject prefs = new JsonObject().put(ENABLED, true).put(REMINDERS, reminderPrefs);
    JsonObject user = new JsonObject().put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, prefs);

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
      "app-1", DATA_ACCESS_APPLICATION_COLLECTION, props, "u1");

    assertTrue(result.succeeded());
    verify(mailClient).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_shouldSkipEmail_whenUserNotFoundInDb() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(HDAB_NAME, "HDAB X");

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "unknown")).thenReturn(Future.succeededFuture(null));

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT, "app-1", DATA_ACCESS_APPLICATION_COLLECTION,
      props, "unknown");

    assertTrue(result.succeeded());
    verify(mailClient, never()).sendMail(any(JsonObject.class), any());
  }

  @Test
  void createNotifications_unknownApplicationType_usesMyApplicantSpaceFallback() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(HDAB_NAME, "HDAB X");

    JsonObject user = new JsonObject().put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, new JsonObject().put(ENABLED, true));

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT, "app-1", "UNKNOWN_TYPE", props, "u1");

    assertTrue(result.succeeded());
    ArgumentCaptor<JsonObject> payloadCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mailClient).sendMail(payloadCaptor.capture(), any());
    String message = payloadCaptor.getValue().getJsonObject("templateModel").getString("message");
    assertTrue(message.contains("myApplicantSpace"), "Expected fallback URL with myApplicantSpace");
  }

  @Test
  void sendExternalEmailNotification_baseUrlWithTrailingSlash_noDoubleSlash() {
    NotificationManager managerWithSlash =
      new NotificationManager(dbHandler, mailClient, "http://base-url/");

    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "App A");
    props.put(HDAB_NAME, "HDAB X");

    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    Future<Void> result = managerWithSlash.sendExternalEmailNotification(
      NotificationCode.APPLICATION_SUBMIT, "app-1",
      DATA_ACCESS_APPLICATION_COLLECTION, props, "ext@example.com");

    assertTrue(result.succeeded());
    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mailClient).sendMail(captor.capture(), any());
    String message = captor.getValue().getJsonObject("templateModel").getString("message");
    assertFalse(message.contains("//applications"), "URL must not have double slash");
  }

  @Test
  void createNotifications_dataRequestApplicationType_buildsCorrectLink() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_NAME, "DRA App");
    props.put(HDAB_NAME, "HDAB Z");

    JsonObject user = new JsonObject().put(EMAIL, "user@example.com")
      .put(EMAIL_NOTIFICATION_PREFERENCES, new JsonObject().put(ENABLED, true));

    when(dbHandler.create(eq(NOTIFICATION_COLLECTION), any())).thenReturn(
      Future.succeededFuture("id"));
    when(dbHandler.findById(USER_COLLECTION, "u1")).thenReturn(Future.succeededFuture(user));
    when(mailClient.sendMail(any(JsonObject.class), any())).thenReturn(Future.succeededFuture());

    notificationManager.createNotifications(
      NotificationCode.APPLICATION_SUBMIT, "app-1",
      DATA_REQUEST_APPLICATION_COLLECTION, props, "u1");

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mailClient).sendMail(captor.capture(), any());
    String message = captor.getValue().getJsonObject("templateModel").getString("message");
    assertTrue(message.contains("data-request-details"), "Expected data-request-details in link");
  }

  @Test
  void readNotifications_shouldReturn400_whenBodyNull() {
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(null);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);

    notificationManager.readNotifications(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end("notificationIds is required");
    verify(dbHandler, never()).updateMany(any(), any(), any());
  }

  @Test
  void readNotifications_shouldReturn500_whenDbFails() {
    JsonArray ids = new JsonArray().add("id-1");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject().put("notificationIds", ids));
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(500)).thenReturn(response);

    when(dbHandler.updateMany(eq(NOTIFICATION_COLLECTION), any(), any()))
      .thenReturn(Future.failedFuture("DB error"));

    notificationManager.readNotifications(routingContext);

    verify(response).setStatusCode(500);
    verify(response).end("DB error");
  }

  @Test
  void getUnreadNotificationCount_shouldReturn500_whenDbFails() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(500)).thenReturn(response);
      when(dbHandler.countAll(eq(NOTIFICATION_COLLECTION), any()))
        .thenReturn(Future.failedFuture("DB error"));

      notificationManager.getUnreadNotificationCount(routingContext);

      verify(response).setStatusCode(500);
      verify(response).end("DB error");
    }
  }

  @Test
  void deleteNotifications_shouldDeleteAllForUser_whenBodyNull() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(null);
      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(204)).thenReturn(response);
      when(dbHandler.deleteAll(eq(NOTIFICATION_COLLECTION), any()))
        .thenReturn(Future.succeededFuture(1));

      notificationManager.deleteNotifications(routingContext);

      ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
      verify(dbHandler).deleteAll(eq(NOTIFICATION_COLLECTION), queryCaptor.capture());
      assertEquals("user-1", queryCaptor.getValue().getString(USER_ID));
      assertFalse(queryCaptor.getValue().containsKey("_id"));
    }
  }

  @Test
  void deleteNotifications_shouldDeleteAllForUser_whenIdsArrayEmpty() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(
        new JsonObject().put("notificationIds", new JsonArray()));
      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(204)).thenReturn(response);
      when(dbHandler.deleteAll(eq(NOTIFICATION_COLLECTION), any()))
        .thenReturn(Future.succeededFuture(3));

      notificationManager.deleteNotifications(routingContext);

      ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
      verify(dbHandler).deleteAll(eq(NOTIFICATION_COLLECTION), queryCaptor.capture());
      assertFalse(queryCaptor.getValue().containsKey("_id"),
        "Empty ids array should not add _id filter");
    }
  }

  @Test
  void deleteNotifications_shouldReturn500_whenDbFails() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.body()).thenReturn(requestBody);
      when(requestBody.asJsonObject()).thenReturn(new JsonObject());
      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(500)).thenReturn(response);
      when(dbHandler.deleteAll(eq(NOTIFICATION_COLLECTION), any()))
        .thenReturn(Future.failedFuture("DB error"));

      notificationManager.deleteNotifications(routingContext);

      verify(response).setStatusCode(500);
      verify(response).end("DB error");
    }
  }

  @Test
  void getAllNotifications_withAllParams_invokesDbFind() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.request()).thenReturn(request);
      when(request.getParam("page")).thenReturn("1");
      when(request.getParam("limit")).thenReturn("10");
      when(request.getParam("sortBy")).thenReturn("dateCreated");
      when(request.getParam("sortOrder")).thenReturn("asc");

      when(dbHandler.findEntities(
        eq(NOTIFICATION_COLLECTION), eq(1), eq(10),
        eq("dateCreated"), eq("asc"), any(), any()))
        .thenReturn(Future.succeededFuture(List.of()));

      // buildResponseWithPagination calls dbHandler.countAll for total count
      when(dbHandler.countAll(eq(NOTIFICATION_COLLECTION), any()))
        .thenReturn(Future.succeededFuture(0));
      when(routingContext.response()).thenReturn(response);
      when(response.setStatusCode(200)).thenReturn(response);

      notificationManager.getAllNotifications(routingContext);

      verify(dbHandler).findEntities(
        eq(NOTIFICATION_COLLECTION), eq(1), eq(10),
        eq("dateCreated"), eq("asc"), any(), any());
    }
  }

  @Test
  void getAllNotifications_withoutPageLimit_invokesDbFindWithNullPagination() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(routingContext)).thenReturn("user-1");

      when(routingContext.request()).thenReturn(request);
      when(request.getParam("page")).thenReturn(null);
      when(request.getParam("limit")).thenReturn(null);
      when(request.getParam("sortBy")).thenReturn(null);
      when(request.getParam("sortOrder")).thenReturn(null);

      when(dbHandler.findEntities(
        eq(NOTIFICATION_COLLECTION), eq(null), eq(null),
        eq("dateCreated"), eq("desc"), any(), any()))
        .thenReturn(Future.succeededFuture(List.of()));

      // Downstream response building is async and may or may not be reached in the test;
      // use lenient to avoid UnnecessaryStubbingException.
      org.mockito.Mockito.lenient()
        .when(dbHandler.countAll(eq(NOTIFICATION_COLLECTION), any()))
        .thenReturn(Future.succeededFuture(0));
      org.mockito.Mockito.lenient().when(routingContext.response()).thenReturn(response);
      org.mockito.Mockito.lenient().when(response.setStatusCode(200)).thenReturn(response);

      notificationManager.getAllNotifications(routingContext);

      verify(dbHandler).findEntities(
        eq(NOTIFICATION_COLLECTION), eq(null), eq(null),
        eq("dateCreated"), eq("desc"), any(), any());
    }
  }
}



