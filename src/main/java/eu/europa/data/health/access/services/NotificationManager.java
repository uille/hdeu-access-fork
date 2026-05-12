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
import static eu.europa.data.health.access.NotificationKeys.USER_NAME_FIELD;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.mail.ApplicationNotificationModel;
import eu.europa.data.health.access.mail.CollaborationNotificationModel;
import eu.europa.data.health.access.mail.DataPermitNotificationModel;
import eu.europa.data.health.access.mail.MailClient;
import eu.europa.data.health.access.mail.MailPayloadFactory;
import eu.europa.data.health.access.mail.MailTemplateType;
import eu.europa.data.health.access.mail.ReminderNotificationModel;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationManager extends AbstractManager {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String notificationManagerClassName = this.getClass().getSimpleName() + " - ";

  private final DBHandler dbHandler;
  private final MailClient mailClient;

  private final String applicationsBaseUrl;

  private record UserNotificationInfo(
    String email,
    String fullName,
    JsonObject preferences
  ) {}

  public NotificationManager(DBHandler dbHandler, MailClient mailClient, String applicationsBaseUrl) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.mailClient = mailClient;
    this.applicationsBaseUrl = applicationsBaseUrl;
  }

  /**
   * Creates an in-app notification and dispatches an email notification (if allowed by user
   * preferences), generating a {@code correlationId} used to trace the notification lifecycle
   * across services — from DB persistence through email dispatch to the mail service.
   *
   * @param notificationCode the type of notification to create
   * @param applicationId    the related application identifier
   * @param applicationType  the type of application
   * @param properties       notification-specific properties
   * @param userId           the recipient user identifier
   */
  public Future<Void> createNotifications(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    String userId
  ) {
    return createNotifications(notificationCode, applicationId, applicationType, properties, userId, UUID.randomUUID().toString());
  }

  /**
   * Internal overload that propagates an existing {@code correlationId} through notification
   * persistence and email delivery, ensuring both operations share the same trace context.
   */
  private Future<Void> createNotifications(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    String userId,
    String correlationId
  ) {

    Future<String> dbFuture = createApplicationNotification(
      notificationCode, applicationId, applicationType, properties, userId, correlationId
    );

    Future<Void> emailFuture = handleEmailNotification(
      notificationCode, applicationId, applicationType, properties, userId, correlationId
    );

    return CompositeFuture.all(dbFuture, emailFuture).mapEmpty();
  }

  /**
   * Sends an email notification directly to an external recipient (not yet registered in the
   * system), generating a {@code correlationId} to trace the email flow across services.
   *
   * @param notificationCode the type of notification to send
   * @param applicationId    the related application identifier
   * @param applicationType  the type of application
   * @param properties       notification-specific properties
   * @param email            the external recipient email address
   */
  public Future<Void> sendExternalEmailNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    String email
  ) {
    return sendExternalEmailNotification(notificationCode, applicationId, applicationType, properties, email, UUID.randomUUID().toString());
  }

  /**
   * Internal overload that preserves an existing {@code correlationId} for external email
   * dispatch, maintaining correlation context through to the mail service handoff.
   */
  private Future<Void> sendExternalEmailNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    String email,
    String correlationId
  ) {

    if (email == null || email.isBlank()) {
      log.warn("Skipping email notification {} correlationId={} because email is missing. properties={}",
        notificationCode, correlationId, properties);
      return Future.succeededFuture();
    }

    // Create a UserNotificationInfo with only email
    UserNotificationInfo externalUser = new UserNotificationInfo(email, null, null);

    return createEmailNotification(notificationCode, applicationId, applicationType, properties, externalUser, correlationId)
      .onFailure(err ->
        log.error(
          "Failed to send external email notification {} correlationId={} to email={} properties={}",
          notificationCode, correlationId, email, properties, err
        ));
  }

  private Future<Void> handleEmailNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    String userId,
    String correlationId
  ) {

    return getUserNotificationInfo(userId)
      .compose(user -> {

        if (user.email() == null) {
          log.warn("Skipping email notification {} because email is missing for user {}",
            notificationCode, userId);
          return Future.succeededFuture();
        }

        boolean shouldSend = shouldSendEmail(notificationCode, user.preferences(), properties);

        if (!shouldSend) {
          log.debug("Skipping email notification {} due to user preferences (userId={})",
            notificationCode, userId);
          return Future.succeededFuture();
        }

        return createEmailNotification(notificationCode, applicationId, applicationType, properties, user, correlationId);
      });
  }

  public Future<String> createApplicationNotification(NotificationCode notificationCode, String applicationId, String applicationType, Map<String, Object> properties, String userId) {
    return createApplicationNotification(notificationCode, applicationId, applicationType, properties, userId, UUID.randomUUID().toString());
  }

  public Future<String> createApplicationNotification(NotificationCode notificationCode, String applicationId, String applicationType, Map<String, Object> properties, String userId, String correlationId) {
    JsonObject entry = createApplicationNotificationEntry(notificationCode, applicationId, applicationType, properties, userId);
    return dbHandler.create(NOTIFICATION_COLLECTION, entry)
      .onSuccess(notificationId -> {
        if (correlationId != null) {
          log.info("[NOTIFICATION-CREATED] correlationId={} notificationId={} applicationId={} notificationCode={} userId={}",
            correlationId, notificationId, applicationId, notificationCode, userId);
        }
      });
  }

  private JsonObject createApplicationNotificationEntry(NotificationCode notificationCode, String applicationId, String applicationType, Map<String, Object> properties, String userId) {
    JsonObject entry = new JsonObject();
    entry.put("applicationId", applicationId);
    entry.put("notificationType", "APPLICATION");
    entry.put("applicationType", applicationType);
    entry.put("notificationCode", notificationCode.getCode());
    entry.put("dateCreated", Instant.now().toString());
    entry.put("displayed", false);
    entry.put(USER_ID, userId);
    entry.put("notificationProperties", properties);

    return entry;
  }

  private Future<Void> createEmailNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    UserNotificationInfo user,
    String correlationId
  ) {

    switch (notificationCode) {

      case APPLICATION_SUBMIT,
           APPLICATION_RESUBMIT,
           CATALOG_REJECTED,
           CATALOG_AWAIT_ADDITIONAL_INFORMATION,
           CATALOG_PROCESSING,
           CATALOG_PROCESSING_EXTENDED,
           CATALOG_WITHDRAW -> {
        return handleApplicationNotification(notificationCode, applicationId, applicationType, properties, user, correlationId);
      }

      case DATASET_POSITIVE_DECISION_ISSUED,
           DATASET_NEGATIVE_DECISION_ISSUED,
           DATASET_APPEAL_REJECTED,
           PERMIT_GRANTED,
           PERMIT_RENEWED,
           PERMIT_RENEWAL_REJECTED,
           PERMIT_AMENDED,
           PERMIT_AMENDMENT_REJECTED,
           PERMIT_EXPIRED,
           PERMIT_REVOKED,
           PERMIT_APPEAL_APPROVED,
           PERMIT_APPEAL_REJECTED -> {
        return handleDataPermitNotification(notificationCode, applicationId, applicationType, properties, user, correlationId);
      }

      case SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
           SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW,
           SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
           SCHEDULED_CATALOG_WITHDRAWN_APPROACHING -> {
        return handleReminderNotification(notificationCode, applicationId, applicationType, properties, user, correlationId);
      }

      case COLLABORATION_INVITE_SENT,
           COLLABORATOR_REMOVED_BY_OWNER,
           COLLABORATOR_SELF_REMOVED,
           COLLABORATION_OWNERSHIP_TRANSFERRED,
           COLLABORATION_INVITE_ACCEPTED,
           COLLABORATION_INVITE_REJECTED,
           COLLABORATION_OWNERSHIP_REQUESTED,
           COLLABORATION_OWNERSHIP_REQUEST_APPROVED,
           COLLABORATION_OWNERSHIP_REQUEST_REJECTED,
           COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN,
           APPLICATION_DISCARDED_BY_OWNER -> {
        return handleCollaborationNotification(notificationCode, applicationId, applicationType, properties, user, correlationId);
      }

      default -> {
        log.warn("No email notification implemented for {}", notificationCode);
        return Future.succeededFuture();
      }
    }
  }

  private Future<Void> handleApplicationNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    UserNotificationInfo user,
    String correlationId
  ) {
    String applicationName = getProperty(properties, APPLICATION_NAME);
    String hdabName = getProperty(properties, HDAB_NAME);
    String email = user.email();
    String fullName = user.fullName();

    // Build application link
    String applicationLink = buildApplicationLink(applicationId, applicationType, applicationName, notificationCode);

    String message;

    switch (notificationCode) {
      case APPLICATION_SUBMIT -> message = String.format(
        "Application <strong>%s</strong> has been submitted to HDAB <strong>%s</strong>.",
        applicationLink, hdabName
      );
      case APPLICATION_RESUBMIT -> message = String.format(
        "Application <strong>%s</strong> has been resubmitted with additional information to HDAB <strong>%s</strong>.",
        applicationLink, hdabName
      );
      case CATALOG_REJECTED -> message = String.format(
        "Application <strong>%s</strong> has been rejected as Incomplete by HDAB <strong>%s</strong>.",
        applicationLink, hdabName
      );
      case CATALOG_AWAIT_ADDITIONAL_INFORMATION -> message = String.format(
        "Application <strong>%s</strong> has been returned by HDAB <strong>%s</strong> for additional information.",
        applicationLink, hdabName
      );
      case CATALOG_PROCESSING -> message = String.format(
        "Application <strong>%s</strong> was accepted and is being processed by HDAB <strong>%s</strong>.",
        applicationLink, hdabName
      );
      case CATALOG_PROCESSING_EXTENDED -> message = String.format(
        "Processing of Application <strong>%s</strong> has been extended by HDAB <strong>%s</strong>.",
        applicationLink, hdabName
      );
      case CATALOG_WITHDRAW -> message = String.format(
        "Application <strong>%s</strong> for HDAB <strong>%s</strong> has been withdrawn by the system.",
        applicationLink, hdabName
      );
      default -> {
        log.warn("Unhandled application notification: {}", notificationCode);
        return Future.failedFuture("Unhandled notification: " + notificationCode);
      }
    }

    ApplicationNotificationModel model = new ApplicationNotificationModel(message, fullName);

    JsonObject payload = MailPayloadFactory.applicationNotification(email, model, null);

    return sendEmail(payload, notificationCode, applicationId, correlationId);
  }

  private Future<Void> handleDataPermitNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    UserNotificationInfo user,
    String correlationId
  ) {

    String datasetName = getProperty(properties, DATASET_NAME);
    String applicationName = getProperty(properties, APPLICATION_NAME);
    String email = user.email();
    String fullName = user.fullName();

    // Build application link
    String applicationLink = buildApplicationLink(applicationId, applicationType, applicationName, notificationCode);

    String message;
    String subject = null;

    switch (notificationCode) {

      case DATASET_POSITIVE_DECISION_ISSUED -> {
        message = String.format(
          "Positive Decision issued for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );
        subject = MailTemplateType.DATASET_DECISION_SUBJECT;
      }

      case DATASET_NEGATIVE_DECISION_ISSUED -> {
        message = String.format(
          "Negative Decision issued for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );
        subject = MailTemplateType.DATASET_DECISION_SUBJECT;
      }

      case DATASET_APPEAL_REJECTED -> {
        message = String.format(
          "Appeal rejected for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );
        subject = MailTemplateType.DATASET_DECISION_SUBJECT;
      }

      case PERMIT_GRANTED ->
        message = String.format(
          "Data Permit granted for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );

      case PERMIT_RENEWED ->
        message = String.format(
          "Data Permit renewed for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );

      case PERMIT_RENEWAL_REJECTED ->
        message = String.format(
          "Data Permit renewal rejected for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );

      case PERMIT_AMENDED ->
        message = String.format(
          "Data Permit amended for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );

      case PERMIT_AMENDMENT_REJECTED ->
        message = String.format(
          "Data Permit amendment rejected for Dataset <strong>%s</strong> in Application <strong>%s</strong>.",
          datasetName, applicationLink
        );

      case PERMIT_EXPIRED ->
        message = String.format(
          "Data Permit <strong>%s</strong> for Application <strong>%s</strong> has expired.",
          datasetName, applicationLink
        );

      case PERMIT_REVOKED ->
        message = String.format(
          "Data Permit <strong>%s</strong> for Application <strong>%s</strong> has been revoked.",
          datasetName, applicationLink
        );

      case PERMIT_APPEAL_APPROVED ->
        message = String.format(
          "Appeal on revoked Data Permit for Dataset <strong>%s</strong> in Application <strong>%s</strong> has been approved.",
          datasetName, applicationLink
        );

      case PERMIT_APPEAL_REJECTED ->
        message = String.format(
          "Appeal on revoked Data Permit for Dataset <strong>%s</strong> in Application <strong>%s</strong> has been rejected.",
          datasetName, applicationLink
        );

      default -> {
        log.warn("Unhandled data permit notification: {}", notificationCode);
        return Future.failedFuture("Unhandled notification: " + notificationCode);
      }
    }

    // CTA (button URL/text) not currently provided for data permit notifications
    DataPermitNotificationModel model =
      new DataPermitNotificationModel(
        message,
        fullName,
        null, // ctaUrl
        null // ctaText
      );

    JsonObject payload = MailPayloadFactory.dataPermitNotification(email, model, subject);

    return sendEmail(payload, notificationCode, applicationId, correlationId);
  }

  private Future<Void> handleReminderNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    UserNotificationInfo user,
    String correlationId
  ) {

    String applicationName = getProperty(properties, APPLICATION_NAME);
    String datasetName = getProperty(properties, DATASET_NAME);
    String daysRemaining = getProperty(properties, NOTIFICATION_DAY);

    String email = user.email();
    String fullName = user.fullName();

    // Build application link
    String applicationLink = buildApplicationLink(applicationId, applicationType, applicationName, notificationCode);

    String message;

    switch (notificationCode) {

      case SCHEDULED_PERMIT_EXPIRATION_APPROACHING ->
        message = String.format(
          "Data Permit <strong>%s</strong> for Application <strong>%s</strong> is approaching expiration. Please submit a renewal request if needed.",
          datasetName, applicationLink
        );

      case SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW ->
        message = String.format(
          "Data Permit <strong>%s</strong> for Application <strong>%s</strong> is approaching expiration.",
          datasetName, applicationName
        );

      case SCHEDULED_DATASET_WITHDRAWN_APPROACHING ->
        message = String.format(
          "Application <strong>%s</strong> requires action. It will be automatically withdrawn in <strong>%s days</strong> if no information is provided.",
          applicationLink, daysRemaining
        );

      case SCHEDULED_CATALOG_WITHDRAWN_APPROACHING ->
        message = String.format(
          "A decision is awaiting your action for Application <strong>%s</strong>. It will be automatically withdrawn in <strong>%s days</strong> if no response is submitted.",
          applicationLink, daysRemaining
        );

      default -> {
        log.warn("Unhandled reminder notification: {}", notificationCode);
        return Future.failedFuture("Unhandled notification: " + notificationCode);
      }
    }

    // CTA (button URL/text) not currently provided for reminder notifications
    ReminderNotificationModel model =
      new ReminderNotificationModel(
        message,
        fullName,
        null, // ctaUrl
        null  // ctaText
      );

    JsonObject payload = MailPayloadFactory.reminderNotification(email, model, null);

    return sendEmail(payload, notificationCode, applicationId, correlationId);
  }

  private Future<Void> handleCollaborationNotification(
    NotificationCode notificationCode,
    String applicationId,
    String applicationType,
    Map<String, Object> properties,
    UserNotificationInfo user,
    String correlationId
  ) {

    String applicationName = getProperty(properties, APPLICATION_NAME);
    String userName = getProperty(properties, USER_NAME);

    String email = user.email();
    String fullName = user.fullName();

    // Build application link
    String applicationLink = buildApplicationLink(applicationId, applicationType, applicationName, notificationCode);

    String message;

    switch (notificationCode) {

      case COLLABORATION_INVITE_SENT ->
        message = String.format(
          "You have been invited to collaborate on Health Data Application <strong>%s</strong> with the role Editor. Please accept or reject the invitation.",
          applicationLink
        );

      case COLLABORATOR_REMOVED_BY_OWNER ->
        message = String.format(
          "You have been removed from Health Data Application <strong>%s</strong>. You no longer have access to this application.",
          applicationName // plain text (no link) since user no longer has access
        );

      case COLLABORATOR_SELF_REMOVED ->
        message = String.format(
          "<strong>%s</strong> has left Health Data Application <strong>%s</strong>.",
          userName, applicationLink
        );

      case COLLABORATION_OWNERSHIP_TRANSFERRED ->
        message = String.format(
          "Ownership of Health Data Application <strong>%s</strong> has been transferred to you. You are now the Owner of this application.",
          applicationLink
        );

      case COLLABORATION_INVITE_ACCEPTED ->
        message = String.format(
          "<strong>%s</strong> has accepted your invitation to collaborate on Health Data Application <strong>%s</strong>.",
          userName, applicationLink
        );

      case COLLABORATION_INVITE_REJECTED ->
        message = String.format(
          "<strong>%s</strong> has rejected your invitation to collaborate on Health Data Application <strong>%s</strong>.",
          userName, applicationLink
        );

      case COLLABORATION_OWNERSHIP_REQUESTED ->
        message = String.format(
          "Transfer of the ownership of Health Data Application <strong>%s</strong> has been requested.",
          applicationLink
        );

      case COLLABORATION_OWNERSHIP_REQUEST_APPROVED ->
        message = String.format(
          "Transfer of the ownership of Health Data Application <strong>%s</strong> has been approved by <strong>%s</strong>.",
          applicationLink, userName
        );

      case COLLABORATION_OWNERSHIP_REQUEST_REJECTED ->
        message = String.format(
          "Transfer of the ownership of Health Data Application <strong>%s</strong> has been rejected by <strong>%s</strong>.",
          applicationLink, userName
        );

      case COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN ->
        message = String.format(
          "Ownership of Health Data Application <strong>%s</strong> has been transferred to another collaborator by the administrator. You are now editor of this application.",
          applicationLink
        );

      case APPLICATION_DISCARDED_BY_OWNER ->
        message = String.format(
          "The Health Data Application <strong>%s</strong> has been discarded by the owner.",
          applicationName // plain text – no link since the application no longer exists
        );

      default -> {
        log.warn("Unhandled collaboration notification: {}", notificationCode);
        return Future.failedFuture("Unhandled notification: " + notificationCode);
      }
    }

    CollaborationNotificationModel model = new CollaborationNotificationModel(
      message,
      fullName,
      null, // primaryCtaText
      null, // primaryCtaUrl
      null, // secondaryCtaText
      null  // secondaryCtaUrl
    );

    JsonObject payload = MailPayloadFactory.collaborationNotification(email, model, null);

    return sendEmail(payload, notificationCode, applicationId, correlationId);
  }

  private Future<Void> sendEmail(JsonObject payload, NotificationCode notificationCode, String applicationId, String correlationId) {

    log.info("[EMAIL-DISPATCH] correlationId={} notificationCode={} recipient={} applicationId={}",
      correlationId, notificationCode, payload.getString("to"), applicationId);

    if (correlationId != null) {
      payload.put("metadata", new JsonObject().put("correlationId", correlationId));
    }

    return mailClient.sendMail(payload, correlationId)
      .onFailure(err ->
        log.error("Failed to send email notification for {}", notificationCode, err)
      );
  }

  private String getProperty(Map<String, Object> properties, String key) {
    Object value = properties.get(key);

    if (value == null) {
      log.warn("Missing notification property: {} | properties={}", key, properties);
      return "";
    }

    return value.toString();
  }

  public void getAllNotifications(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;

    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");

    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    String userId = Context.getEcasId(context);

    JsonObject queryOptions = new JsonObject();
    if (StringUtils.isNotEmpty(userId)) {
      queryOptions.put(USER_ID, userId);
    }

    super.getEntities(
      context,
      NOTIFICATION_COLLECTION,
      finalPage,
      finalPageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : "dateCreated",
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc",
      queryOptions
    );
  }

  public void readNotifications(@NotNull RoutingContext context) {

    JsonObject body = context.body().asJsonObject();
    if (body == null || body.getJsonArray("notificationIds") == null) {
      context.response().setStatusCode(400).end("notificationIds is required");
      return;
    }

    List<String> notificationIds = body
      .getJsonArray("notificationIds")
      .stream()
      .map(Object::toString)
      .toList();

    if (notificationIds.isEmpty()) {
      context.response().setStatusCode(400).end("notificationIds cannot be empty");
      return;
    }

    // Query: find notifications by IDs
    JsonObject query = new JsonObject()
      .put("_id", new JsonObject().put("$in", notificationIds));

    // Update: set displayed = true
    JsonObject update = new JsonObject()
      .put("$set", new JsonObject().put("displayed", true));

    dbHandler.updateMany(NOTIFICATION_COLLECTION, query, update)
      .onSuccess(res ->
        context.response().setStatusCode(200).end()
      )
      .onFailure(err -> {
        log.error("{}Failed to mark notifications as read", notificationManagerClassName, err);
        context.response().setStatusCode(500).end(err.getMessage());
      });
  }

  public void getUnreadNotificationCount(@NotNull RoutingContext context) {
    String userId = Context.getEcasId(context);

    JsonObject query = new JsonObject()
      .put(USER_ID, userId)
      .put("displayed", false);

    dbHandler.countAll(NOTIFICATION_COLLECTION, query)
      .onSuccess(count ->
        context.response()
          .setStatusCode(200)
          .end(new JsonObject()
            .put("unread_notification_count", count)
            .encode())
      )
      .onFailure(err ->
        context.response()
          .setStatusCode(500)
          .end(err.getMessage())
      );
  }

  public void deleteNotifications(@NotNull RoutingContext context) {
    String userId = Context.getEcasId(context);

    if (StringUtils.isEmpty(userId)) {
      context.response().setStatusCode(401).end();
      return;
    }

    JsonObject body = context.body().asJsonObject();
    JsonObject query = new JsonObject().put(USER_ID, userId);

    //If we have ids put them in the query, otherwise delete all
    if (body != null && body.getJsonArray("notificationIds") != null) {
      JsonArray ids = body.getJsonArray("notificationIds");

      if (!ids.isEmpty()) {
        query.put("_id", new JsonObject().put("$in", ids));
      }
    }

    dbHandler.deleteAll(NOTIFICATION_COLLECTION, query)
      .onSuccess(code ->
        context.response().setStatusCode(204).end()
      )
      .onFailure(err ->
        context.response()
          .setStatusCode(500)
          .end(err.getMessage())
      );
  }

  /**
   * Retrieves user notification info (email, full name, preferences) for a given user ID.
   *
   * <p>Full name is taken from {@code fullName} if present, otherwise composed from
   * {@code firstName} and {@code lastName}. Missing values may be {@code null}.
   *
   * <p>If no user is found, returns an instance with all fields {@code null}.
   *
   * @param userId the user ID
   * @return a Future containing the user's notification info
   */
  private Future<UserNotificationInfo> getUserNotificationInfo(String userId) {

    return dbHandler.findById(USER_COLLECTION, userId)
      .map(user -> {
        if (user == null) {
          return new UserNotificationInfo(null, null, null);
        }

        String email = user.getString(EMAIL);

        String fullName = user.getString(USER_NAME_FIELD);
        JsonObject preferences = user.getJsonObject(EMAIL_NOTIFICATION_PREFERENCES);

        return new UserNotificationInfo(email, fullName, preferences);
      });
  }

  /**
   * Determines whether an email should be sent based on user preferences
   * and notification properties.
   * <p>
   * Reminder notifications are additionally filtered by the configured
   * reminder offset days.
   */
  private boolean shouldSendEmail(
    NotificationCode notificationCode,
    JsonObject preferences,
    Map<String, Object> properties) {

    // No preferences stored, allow by default
    if (preferences == null) {
      return true;
    }

    // Global email notifications disabled
    if (!preferences.getBoolean(ENABLED, true)) {
      return false;
    }

    // If not a reminder notification then allowed
    if (!notificationCode.isReminder()) {
      return true;
    }

    JsonObject reminderPrefs = preferences.getJsonObject(REMINDERS);

    if (reminderPrefs == null) {
      return true;
    }

    if (!reminderPrefs.getBoolean(ENABLED, true)) {
      return false;
    }

    return isReminderOffsetAllowed(reminderPrefs, properties);
  }

  private boolean isReminderOffsetAllowed(
    JsonObject reminderPrefs,
    Map<String, Object> properties) {

    String dayValue = properties.get(NOTIFICATION_DAY).toString();

    if (dayValue == null) {
      return true;
    }

    int offset = Integer.parseInt(dayValue);

    JsonArray allowedOffsets = reminderPrefs.getJsonArray(OFFSET_DAYS);

    if (allowedOffsets == null) {
      return true;
    }

    log.debug("Reminder offset check: requested={}, allowed={}", offset, allowedOffsets);

    for (Object value : allowedOffsets) {
      if (value instanceof Number number) {
        if (number.intValue() == offset) {
          return true;
        }
      }
    }

    return false;
  }

        private String buildApplicationLink(String applicationId, String applicationType, String applicationName, NotificationCode notificationCode) {
            String base = applicationsBaseUrl.endsWith("/") ? applicationsBaseUrl : applicationsBaseUrl + "/";
            if (notificationCode == NotificationCode.COLLABORATION_INVITE_SENT) {
                String url = base + "myApplicantSpace";
                String safeName = StringEscapeUtils.escapeHtml4(applicationName);
                return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", url, safeName);
            } else {
                String path;
                switch (applicationType) {
                    case DATA_ACCESS_APPLICATION_COLLECTION ->
                        path = "applications/data-access-details";
                    case DATA_REQUEST_APPLICATION_COLLECTION ->
                        path = "applications/data-request-details";
                    default -> {
                        log.warn("Unknown applicationType: {}", applicationType);
                        return String.format("<a href=\"%smyApplicantSpace\" target=\"_blank\">%s</a>", base, applicationName);
                    }
                }
                String url = base + path + "/" + applicationId;
                String safeName = StringEscapeUtils.escapeHtml4(applicationName);
                return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", url, safeName);
            }
        }

}
