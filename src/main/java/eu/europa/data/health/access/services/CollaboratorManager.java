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

import static eu.europa.data.health.access.Constants.ALREADY_COLLABORATOR_MESSAGE;
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
import static eu.europa.data.health.access.Constants.OPTIONS;
import static eu.europa.data.health.access.Constants.OWNERSHIP_REQUEST_COLLECTION;
import static eu.europa.data.health.access.Constants.REGEX;
import static eu.europa.data.health.access.Constants.ROLE;
import static eu.europa.data.health.access.Constants.ROLE_EDITOR;
import static eu.europa.data.health.access.Constants.ROLE_OWNER;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.USER_NAME;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.NotificationKeys;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class that manages collaboration on applications.
 *
 * <p>Collaboration allows an application owner to invite other users (by email)
 * to edit specific applications.  Invites are stored in
 * {@code APPLICATION_COLLABORATOR} and linked to a real {@code USER._id}
 * (= ecasId) as soon as the invited user logs in via the bootstrap endpoint.
 *
 * <h3>Key design decisions</h3>
 * <ul>
 *   <li>No Keycloak lookup is required - user info is read from the JWT or the
 *       {@code USER} MongoDB collection.</li>
 *   <li>Duplicate invite detection uses the exact error message
 *       {@value eu.europa.data.health.access.Constants#ALREADY_COLLABORATOR_MESSAGE}.</li>
 *   <li>Only the application <em>owner</em> (userId == application.userId) can
 *       invite, remove, or transfer ownership.</li>
 * </ul>
 */
public class CollaboratorManager extends AbstractManager {

  private static final Logger log = LoggerFactory.getLogger(CollaboratorManager.class);

  /** Pagination default for collaborator listings. */
  private static final int DEFAULT_PAGE_SIZE = 5;
  private static final String AUDIT_OBJECT_APPLICATION_OWNERSHIP = "Application Ownership";
  private static final String AUDIT_ACTION_REQUEST_OWNERSHIP = "Request Ownership";
  private static final String AUDIT_ACTION_OWNERSHIP_TRANSFER_APPROVED = "Ownership transfer approved";
  private static final String AUDIT_ACTION_OWNERSHIP_TRANSFER_REJECTED = "Ownership transfer rejected";

  private final MongoClient mongoClient;
  private final AuditLogManager auditLogManager;
  private final NotificationManager notificationManager;

  /**
   * Constructs a new {@code CollaboratorManager}.
   *
   * @param dbHandler           shared DB handler for low-level operations
   * @param mongoClient         raw Mongo client used for advanced queries
   * @param auditLogManager     service for writing audit log entries
   * @param notificationManager service for creating notifications
   */
  public CollaboratorManager(DBHandler dbHandler, MongoClient mongoClient,
    AuditLogManager auditLogManager, NotificationManager notificationManager) {
    super(dbHandler);
    this.mongoClient = mongoClient;
    this.auditLogManager = auditLogManager;
    this.notificationManager = notificationManager;
  }

  // ---------------------------------------------------------------------------
  // GET /application/{applicationId}/collaborators
  // ---------------------------------------------------------------------------

  /**
   * Returns all collaborator entries for the given application.
   *
   * <p>Accessible by the owner and any accepted collaborator of the application.
   * Pagination is supported via {@code page} and {@code limit} query parameters.
   *
   * @param context the Vert.x routing context; path param {@code applicationId} required
   */
  public void getCollaborators(@NotNull RoutingContext context) {
    String applicationId = context.pathParam("applicationId");
    String ecasId = Context.getEcasId(context);
    boolean adminAction = isAdmin(context);

    if (StringUtils.isBlank(applicationId)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Missing applicationId");
      return;
    }

    Future<Boolean> authorization = adminAction
      ? Future.succeededFuture(true)
      : isOwnerOrCollaborator(applicationId, ecasId);

    // Verify the caller is either owner or accepted collaborator of this application,
    // unless they are an administrator.
    authorization
      .onSuccess(authorized -> {
        if (!authorized) {
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.FORBIDDEN,
            "You are not authorised to view collaborators for this application."
          );
          return;
        }

        JsonObject query = new JsonObject().put(APPLICATION_ID, applicationId);

        String searchTerm = context.request().getParam("search");

        int page = parseIntParam(context.request().getParam("page"), 0);
        int limit = parseIntParam(context.request().getParam("limit"), DEFAULT_PAGE_SIZE);

        FindOptions options = new FindOptions().setSkip(page * limit).setLimit(limit);

        String sortBy = context.request().getParam("sortBy");
        String sortDirection = context.request().getParam("sortDirection");
        Set<String> allowedSortFields = Set.of(INVITED_EMAIL, COLLABORATOR_USER_NAME, ROLE);
        if (StringUtils.isNotBlank(sortBy) && allowedSortFields.contains(sortBy)) {
          int direction = "desc".equalsIgnoreCase(sortDirection) ? -1 : 1;
          options.setSort(new JsonObject().put(sortBy, direction));
        }

        final int finalPage = page;
        final int finalLimit = limit;
        buildCollaboratorListQuery(query, searchTerm)
          .compose(resolvedQuery ->
            mongoClient
              .findWithOptions(APPLICATION_COLLABORATOR_COLLECTION, resolvedQuery, options)
              .compose(results ->
                mongoClient
                  .count(APPLICATION_COLLABORATOR_COLLECTION, resolvedQuery)
                  .map(total ->
                    new JsonObject()
                      .put("items", new JsonArray(results))
                      .put("total", total)
                      .put("page", finalPage)
                      .put("limit", finalLimit)
                  )
              )
          )
          .onSuccess(response ->
            setUpContextResponse(context, HttpResponseStatus.OK, response.encode())
          )
          .onFailure(err -> {
            log.error(
              "getCollaborators failed for applicationId='{}': {}",
              applicationId,
              err.getMessage(),
              err
            );
            setUpMessageContextResponse(
              context,
              HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to retrieve collaborators"
            );
          });
      })
      .onFailure(err -> {
        log.error("getCollaborators auth check failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(
          context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to validate access"
        );
      });
  }

  // ---------------------------------------------------------------------------
  // POST /application/{applicationId}/collaborators
  // ---------------------------------------------------------------------------

  /**
   * Invites a user as a collaborator by email.
   *
   * <p>The caller must be the application owner.  The method:
   * <ol>
   *   <li>Checks for duplicate invites / existing collaborations.</li>
   *   <li>Looks up the invited email in {@code USER} to resolve the userId.</li>
   *   <li>Creates a {@code PENDING} collaborator entry.</li>
   *   <li>Sends an email notification to the invitee.</li>
   *   <li>Writes an audit log entry.</li>
   * </ol>
   *
   * <p>Request body (JSON):
   * <pre>{@code
   * {
   *   "invitedEmail": "user@example.com",
   *   "applicationType": "DATA_ACCESS_APPLICATION"
   * }
   * }</pre>
   *
   * @param context the Vert.x routing context; path param {@code applicationId} required
   */
  public void addCollaborator(@NotNull RoutingContext context) {
    String applicationId = context.pathParam("applicationId");
    String callerEcasId = Context.getEcasId(context);
    String callerUserName = Context.getUserName(context);
    boolean adminAction = isAdmin(context);
    JsonObject body = context.body().asJsonObject();

    if (body == null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Request body is required");
      return;
    }

    String invitedEmail = body.getString(INVITED_EMAIL);
    String applicationType = body.getString(APPLICATION_TYPE_FIELD);

    // -- Validate input -------------------------------------------------------
    if (StringUtils.isBlank(invitedEmail) || StringUtils.isBlank(applicationType)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Fields 'invitedEmail' and 'applicationType' are required");
      return;
    }

    log.info("[COLLAB-INVITE] applicationId={} applicationType={} invitedEmail={} inviter={}",
      applicationId, applicationType, invitedEmail, callerEcasId);

    // -- Verify caller is the owner of this application -----------------------
    getApplicationOwnerId(applicationId, applicationType)
      .onSuccess(ownerUserId -> {
        boolean isOwner = callerEcasId.equals(ownerUserId);
        if (!isOwner && !adminAction) {
          setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
            "Only the application owner or an administrator can invite collaborators.");
          return;
        }

        // -- Check for duplicate ------------------------------------------
        checkDuplicateCollaborator(applicationId, applicationType, invitedEmail)
          .onSuccess(isDuplicate -> {
            if (isDuplicate) {
              setUpMessageContextResponse(context, HttpResponseStatus.CONFLICT,
                ALREADY_COLLABORATOR_MESSAGE);
              return;
            }

            // -- Look up invited user in USER collection by email -----------
            resolveCollaboratorUserByEmail(invitedEmail)
              .compose(resolvedUser ->
                // -- Fetch the application title to store alongside the invite --
                fetchApplicationTitle(applicationId, applicationType)
                  .map(title -> new Object[]{resolvedUser, title})
              )
              .onSuccess(pair -> {
                JsonObject resolvedUser = (JsonObject) pair[0];
                String resolvedUserId = resolvedUser != null ? resolvedUser.getString(ID) : null;
                String collaboratorUserName = resolvedUser != null
                  ? StringUtils.firstNonBlank(
                      resolvedUser.getString(USER_NAME),
                      resolvedUser.getString(ID),
                      invitedEmail)
                  : null;
                String appTitle = (String) pair[1];

                // Build the collaborator document
                String docId = new ObjectId().toHexString();
                JsonObject collaboratorDoc = new JsonObject()
                  .put(ID, docId)
                  .put(APPLICATION_ID, applicationId)
                  .put(APPLICATION_TYPE_FIELD, applicationType)
                  .put(INVITED_EMAIL, invitedEmail)
                  .put(ROLE, ROLE_EDITOR)
                  .put(INVITATION_STATUS, INVITATION_PENDING)
                  .put(INVITED_BY, callerEcasId)
                  .put("createdAt", Instant.now().toString());

                // Store the application title if resolved
                if (appTitle != null) {
                  collaboratorDoc.put(APPLICATION_TITLE, appTitle);
                }

                // If the user already has a record, link immediately
                if (resolvedUserId != null) {
                  collaboratorDoc.put(COLLABORATOR_USER_ID, resolvedUserId);
                  collaboratorDoc.put(COLLABORATOR_USER_NAME, collaboratorUserName);
                }

                mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, collaboratorDoc)
                  .onSuccess(insertedId -> {
                    log.info("Collaborator invited: applicationId='{}' email='{}' by='{}'",
                      applicationId, invitedEmail, callerEcasId);

                    // Audit log
                    String actorLabel = adminAction && !isOwner
                      ? "Administrator " + callerUserName
                      : "Owner " + callerUserName;
                    auditLogManager.insertAuditLog(
                      "Application",
                      "Application updated",
                      "Collaboration invitation was sent to " + invitedEmail
                        + " for Application " + applicationId
                        + " with role Editor by " + actorLabel + ".",
                      context
                    );

                    Map<String, Object> properties = new HashMap<>();
                    properties.put(NotificationKeys.APPLICATION_NAME, appTitle);
                    properties.put(NotificationKeys.USER_NAME, callerUserName);

                    if (resolvedUserId != null) {
                      notificationManager.createNotifications(
                          NotificationCode.COLLABORATION_INVITE_SENT,
                          applicationId,
                          applicationType,
                          properties,
                          resolvedUserId
                        )
                        .onFailure(err ->
                          log.error(
                            "Failed to create collaboration invite notification for applicationId={} applicationType={} userId={}",
                            applicationId,
                            applicationType,
                            resolvedUserId,
                            err
                          )
                        );
                    } else {
                      notificationManager.sendExternalEmailNotification(
                          NotificationCode.COLLABORATION_INVITE_SENT,
                          applicationId,
                          applicationType,
                          properties,
                          invitedEmail
                        )
                        .onFailure(err ->
                          log.error(
                            "Failed to send collaboration invite email for applicationId={} applicationType={} email={}",
                            applicationId,
                            applicationType,
                            invitedEmail,
                            err
                          )
                        );
                    }

                    setUpContextResponse(context, HttpResponseStatus.CREATED,
                      new JsonObject().put(ID, docId).encode());
                  })
                  .onFailure(err -> {
                    log.error("addCollaborator insert failed: {}", err.getMessage(), err);
                    setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                      "Failed to create collaborator entry");
                  });
              })
              .onFailure(err -> {
                log.error("addCollaborator user/title lookup failed: {}", err.getMessage(), err);
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to resolve invited user");
              });
          })
          .onFailure(err -> {
            log.error("addCollaborator duplicate check failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to check for duplicate collaborator");
          });
      })
      .onFailure(err -> {
        log.error("addCollaborator owner check failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to validate ownership");
      });
  }

  // ---------------------------------------------------------------------------
  // DELETE /application/{applicationId}/collaborators/{collaboratorId}
  // ---------------------------------------------------------------------------

  /**
   * Removes a collaborator entry by its document ID.
   *
   * <p>Only the application owner can remove collaborators.
   * A collaborator can also remove themselves (leave the application).
   *
   * @param context the Vert.x routing context; path params {@code applicationId}
   *                and {@code collaboratorId} required
   */
  public void removeCollaborator(@NotNull RoutingContext context) {
    String applicationId   = context.pathParam("applicationId");
    String collaboratorId  = context.pathParam("collaboratorId");
    String callerEcasId    = Context.getEcasId(context);
    String callerUsername  = Context.getUserName(context);
    boolean adminAction    = isAdmin(context);

    // Fetch the collaborator document first to check permissions
    mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION,
        new JsonObject().put(ID, collaboratorId), null)
      .onSuccess(doc -> {
        if (doc == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Collaborator entry not found");
          return;
        }

        String applicationType = doc.getString(APPLICATION_TYPE_FIELD);
        String targetUserId = doc.getString(COLLABORATOR_USER_ID);

        // Allow removal if caller is the owner OR if caller is the collaborator themselves (leave)
        getApplicationOwnerId(applicationId, applicationType)
          .onSuccess(ownerUserId -> {
            boolean isOwner = callerEcasId.equals(ownerUserId);
            boolean isSelf = callerEcasId.equals(targetUserId);

            if (!isOwner && !isSelf && !adminAction) {
              setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
                "Only the application owner, an administrator, or the collaborator themselves can remove this entry.");
              return;
            }

            fetchApplicationTitle(applicationId, applicationType)
              .onSuccess(appTitle -> mongoClient.removeDocument(APPLICATION_COLLABORATOR_COLLECTION,
                  new JsonObject().put(ID, collaboratorId))
                .onSuccess(res -> {
                  log.info("Collaborator removed: collaboratorId='{}' by='{}'",
                    collaboratorId, callerEcasId);

                  Map<String, Object> properties = new HashMap<>();
                  properties.put(NotificationKeys.APPLICATION_NAME, appTitle);

                  if (isSelf && !adminAction) {
                    // Collaborator left voluntarily
                    if (StringUtils.isNotBlank(ownerUserId) && !callerEcasId.equals(ownerUserId)) {
                      properties.put(NotificationKeys.USER_NAME, callerUsername);
                      notificationManager.createNotifications(
                          NotificationCode.COLLABORATOR_SELF_REMOVED,
                          applicationId,
                          applicationType,
                          properties,
                          ownerUserId)
                        .onFailure(err ->
                          log.error(
                            "Failed to send {} notification for applicationId={} collaboratorId={}",
                            NotificationCode.COLLABORATOR_SELF_REMOVED, applicationId,
                            collaboratorId, err)
                        );
                    }
                    auditLogManager.insertAuditLog(
                      "Application",
                      "Application updated",
                      "Collaborator " + callerUsername
                        + " left Application " + applicationId + ".",
                      context
                    );
                  } else {
                    // Owner or admin removed a collaborator
                    if (StringUtils.isNotBlank(targetUserId)) {
                      notificationManager.createNotifications(
                          NotificationCode.COLLABORATOR_REMOVED_BY_OWNER,
                          applicationId,
                          applicationType,
                          properties,
                          targetUserId)
                        .onFailure(err ->
                          log.error(
                            "Failed to send {} notification for applicationId={} collaboratorId={}",
                            NotificationCode.COLLABORATOR_REMOVED_BY_OWNER, applicationId,
                            collaboratorId, err)
                        );
                    }
                    String actorLabel = adminAction && !isOwner
                      ? "Administrator " + callerUsername
                      : "Owner";
                    auditLogManager.insertAuditLog(
                      "Application",
                      "Application updated",
                      "Collaborator " + targetUserId
                        + " was removed from Application " + applicationId + " by "
                        + actorLabel + ".",
                      context
                    );
                  }

                  setUpContextResponse(context, HttpResponseStatus.NO_CONTENT);
                })
                .onFailure(err -> {
                  log.error("removeCollaborator delete failed: {}", err.getMessage(), err);
                  setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to remove collaborator");
                }))
              .onFailure(err -> {
                log.error("removeCollaborator title lookup failed: {}", err.getMessage(), err);
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to resolve application title");
              });
          })
          .onFailure(err -> {
            log.error("removeCollaborator owner check failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to validate ownership");
          });
      })
      .onFailure(err -> {
        log.error("removeCollaborator lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to find collaborator entry");
      });
  }

  // ---------------------------------------------------------------------------
  // PUT /application/{applicationId}/collaborators/{collaboratorId}/accept
  // ---------------------------------------------------------------------------

  /**
   * Accepts a pending collaboration invitation.
   *
   * <p>Only the invited user (matching {@code collaboratorUserId} or email) can accept.
   *
   * @param context the Vert.x routing context
   */
  public void acceptCollaboratorInvite(@NotNull RoutingContext context) {
    String collaboratorId = context.pathParam("collaboratorId");
    String callerEcasId   = Context.getEcasId(context);
    String callerUsername   = Context.getUserName(context);
    boolean adminAction = isAdmin(context);

    mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION,
        new JsonObject().put(ID, collaboratorId), null)
      .onSuccess(doc -> {
        if (doc == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Collaborator invite not found");
          return;
        }

        // Verify the caller is the invited user
        String storedUserId = doc.getString(COLLABORATOR_USER_ID);
        String applicationId = doc.getString(APPLICATION_ID);
        String applicationTitle = doc.getString(APPLICATION_TITLE);
        String acceptedBy = StringUtils.defaultIfBlank(callerUsername, doc.getString(INVITED_EMAIL));

        if (!adminAction && !callerEcasId.equals(storedUserId)) {
          setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
            "Only the invited user or an administrator can accept this invitation.");
          return;
        }

        if (!INVITATION_PENDING.equals(doc.getString(INVITATION_STATUS))) {
          setUpMessageContextResponse(context, HttpResponseStatus.CONFLICT,
            "Invitation is not in PENDING state.");
          return;
        }

        JsonObject update = new JsonObject()
          .put("$set", new JsonObject()
            .put(INVITATION_STATUS, INVITATION_ACCEPTED)
            .put("acceptedAt", Instant.now().toString()));

        mongoClient.updateCollection(APPLICATION_COLLABORATOR_COLLECTION,
            new JsonObject().put(ID, collaboratorId), update)
          .onSuccess(res -> {
            log.info("Collaborator invite accepted: collaboratorId='{}' by='{}'",
              collaboratorId, callerEcasId);

            HashMap<String, Object> properties = new HashMap<>();
            properties.put(NotificationKeys.APPLICATION_NAME, applicationTitle);
            properties.put(NotificationKeys.USER_NAME, callerUsername);

            notificationManager.createNotifications(
              NotificationCode.COLLABORATION_INVITE_ACCEPTED,
              applicationId,
              doc.getString(APPLICATION_TYPE_FIELD),
              properties,
              doc.getString(INVITED_BY))
              .onFailure(err ->
              log.error("Failed to send COLLABORATION_INVITE_ACCEPTED notification for collaboratorId={}",
                collaboratorId, err)
            );

            auditLogManager.insertAuditLog(
              "Application",
              "Application updated",
              "Collaboration invitation for Application " + applicationId
                + " was accepted by " + acceptedBy + ".",
              context
            );

            setUpContextResponse(context, HttpResponseStatus.OK,
              new JsonObject().put(INVITATION_STATUS, INVITATION_ACCEPTED).encode());
          })
          .onFailure(err -> {
            log.error("acceptCollaboratorInvite update failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to accept invitation");
          });
      })
      .onFailure(err -> {
        log.error("acceptCollaboratorInvite lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to find collaborator invite");
      });
  }

  // ---------------------------------------------------------------------------
  // PUT /application/{applicationId}/collaborators/{collaboratorId}/reject
  // ---------------------------------------------------------------------------

  /**
   * Rejects a pending collaboration invitation.
   *
   * <p>Only the invited user can reject their own pending invitation.
   *
   * @param context the Vert.x routing context
   */
  public void rejectCollaboratorInvite(@NotNull RoutingContext context) {
    String collaboratorId = context.pathParam("collaboratorId");
    String callerEcasId   = Context.getEcasId(context);
    String callerUsername    = Context.getUserName(context);
    boolean adminAction = isAdmin(context);

    mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION,
        new JsonObject().put(ID, collaboratorId), null)
      .onSuccess(doc -> {
        if (doc == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Collaborator invite not found");
          return;
        }

        String storedUserId = doc.getString(COLLABORATOR_USER_ID);
        String applicationId = doc.getString(APPLICATION_ID);
        String applicationTitle = doc.getString(APPLICATION_TITLE);
        String rejectedBy = StringUtils.defaultIfBlank(callerUsername, doc.getString(INVITED_EMAIL));

        if (!adminAction && !callerEcasId.equals(storedUserId)) {
          setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
            "Only the invited user or an administrator can reject this invitation.");
          return;
        }

        if (!INVITATION_PENDING.equals(doc.getString(INVITATION_STATUS))) {
          setUpMessageContextResponse(context, HttpResponseStatus.CONFLICT,
            "Invitation is not in PENDING state.");
          return;
        }

        JsonObject update = new JsonObject()
          .put("$set", new JsonObject()
            .put(INVITATION_STATUS, INVITATION_REJECTED)
            .put("rejectedAt", Instant.now().toString()));

        mongoClient.updateCollection(APPLICATION_COLLABORATOR_COLLECTION,
            new JsonObject().put(ID, collaboratorId), update)
          .onSuccess(res -> {
            log.info("Collaborator invite rejected: collaboratorId='{}' by='{}'",
              collaboratorId, callerEcasId);

            HashMap<String, Object> properties = new HashMap<>();
            properties.put(NotificationKeys.APPLICATION_NAME, applicationTitle);
            properties.put(NotificationKeys.USER_NAME, callerUsername);

            notificationManager.createNotifications(
                NotificationCode.COLLABORATION_INVITE_REJECTED,
                applicationId,
                doc.getString(APPLICATION_TYPE_FIELD),
                properties,
                doc.getString(INVITED_BY))
              .onFailure(err ->
                log.error(
                  "Failed to send COLLABORATION_INVITE_REJECTED notification for collaboratorId={}",
                  collaboratorId, err)
              );

            auditLogManager.insertAuditLog(
              "Application",
              "Application updated",
              "Collaboration invitation for Application " + applicationId
                + " was rejected by " + rejectedBy + ".",
              context
            );

            setUpContextResponse(context, HttpResponseStatus.OK,
              new JsonObject().put(INVITATION_STATUS, INVITATION_REJECTED).encode());
          })
          .onFailure(err -> {
            log.error("rejectCollaboratorInvite update failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to reject invitation");
          });
      })
      .onFailure(err -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to find collaborator invite"));
  }

  // ---------------------------------------------------------------------------
  // POST /application/{applicationId}/collaborators/transfer
  // ---------------------------------------------------------------------------

  /**
   * Transfers application ownership to another collaborator.
   *
   * <p>Only the current owner can perform ownership transfer.  The transfer is atomic:
   * the current owner becomes an EDITOR, the application {@code userId} is changed to the
   * target user, and the target collaborator row is removed from
   * {@code APPLICATION_COLLABORATOR}.
   *
   * <p>Request body (JSON):
   * <pre>{@code { "newOwnerCollaboratorId": "<collaborator doc _id>" }}</pre>
   *
   * @param context the Vert.x routing context
   */
  public void transferOwnership(@NotNull RoutingContext context) {
    String applicationId   = context.pathParam("applicationId");
    String callerEcasId    = Context.getEcasId(context);
    String callerUserName  = Context.getUserName(context);
    String callerEmail     = Context.getEcasEmail(context);
    boolean adminAction    = isAdmin(context);
    JsonObject body        = context.body().asJsonObject();

    if (body == null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Request body is required");
      return;
    }

    String newOwnerCollaboratorId = body.getString("newOwnerCollaboratorId");
    if (StringUtils.isBlank(newOwnerCollaboratorId)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Field 'newOwnerCollaboratorId' is required");
      return;
    }

    mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION,
        new JsonObject().put(ID, newOwnerCollaboratorId), null)
      .onSuccess(newOwnerDoc -> {
        if (newOwnerDoc == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Target collaborator not found");
          return;
        }

        String targetApplicationId = newOwnerDoc.getString(APPLICATION_ID);
        String applicationType     = newOwnerDoc.getString(APPLICATION_TYPE_FIELD);
        String newOwnerUserId      = newOwnerDoc.getString(COLLABORATOR_USER_ID);
        String invitationStatus    = newOwnerDoc.getString(INVITATION_STATUS);

        if (!applicationId.equals(targetApplicationId)) {
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            "Target collaborator does not belong to this application.");
          return;
        }

        if (StringUtils.isBlank(newOwnerUserId) || !INVITATION_ACCEPTED.equals(invitationStatus)) {
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            "Cannot transfer ownership to a user who has not yet accepted the invitation.");
          return;
        }

        getApplicationOwnerId(applicationId, applicationType)
          .onSuccess(ownerUserId -> {
            boolean isOwner = callerEcasId.equals(ownerUserId);
            if (!isOwner && !adminAction) {
              setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
                "Only the current owner or an administrator can transfer ownership.");
              return;
            }

            resolveOwnershipActor(applicationId, applicationType, ownerUserId,
                isOwner ? callerUserName : null,
                isOwner ? callerEmail : null)
              .compose(ownerActor -> executeOwnershipTransfer(applicationId, applicationType,
                ownerUserId,
                ownerActor.getString(USER_NAME),
                ownerActor.getString(EMAIL),
                callerEcasId,
                newOwnerDoc,
                context,
                NotificationCode.COLLABORATION_OWNERSHIP_TRANSFERRED,
                null,
                "Application",
                "Application updated",
                null))
              .onSuccess(v -> setUpContextResponse(context, HttpResponseStatus.OK,
                new JsonObject().put("message", "Ownership transferred successfully").encode()))
              .onFailure(err -> {
                log.error("transferOwnership update failed: {}", err.getMessage(), err);
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to transfer ownership");
              });
          })
          .onFailure(err -> {
            log.error("transferOwnership owner check failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to validate ownership");
          });
      })
      .onFailure(err -> {
        log.error("transferOwnership lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to find target collaborator");
      });
  }

  // ---------------------------------------------------------------------------
  // POST /application/{applicationId}/collaborators/request-ownership
  // ---------------------------------------------------------------------------

  /**
   * Requests ownership transfer for an application.
   *
   * <p>Only accepted editors can request ownership. The request is stored in
   * {@code OWNERSHIP_REQUEST_COLLECTION} and must be accepted by the current owner.
   *
   * <p>Request body (JSON):
   * <pre>{@code
   * {
   *   "justification": "I hereby request ownership transfer because..."
   * }
   * }</pre>
   *
   * @param context the Vert.x routing context
   */
  public void requestOwnership(@NotNull RoutingContext context) {
    String applicationId  = context.pathParam("applicationId");
    String callerEcasId   = Context.getEcasId(context);
    String callerUserName = StringUtils.firstNonBlank(Context.getUserName(context), callerEcasId);
    String callerEmail    = Context.getEcasEmail(context);
    boolean adminAction   = isAdmin(context);
    JsonObject body       = context.body().asJsonObject();

    if (body == null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Request body is required");
      return;
    }

    String justification = StringUtils.trimToNull(body.getString(JUSTIFICATION));
    if (StringUtils.isBlank(justification)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Field 'justification' is required");
      return;
    }

    Future<String> applicationTypeFuture = adminAction
      ? resolveApplicationType(applicationId)
      : resolveAcceptedEditorCollaboration(applicationId, callerEcasId)
        .map(editorDoc -> editorDoc != null ? editorDoc.getString(APPLICATION_TYPE_FIELD) : null);

    applicationTypeFuture
      .onSuccess(applicationType -> {
        if (StringUtils.isBlank(applicationType)) {
          setUpMessageContextResponse(context,
            adminAction ? HttpResponseStatus.NOT_FOUND : HttpResponseStatus.FORBIDDEN,
            adminAction
              ? "Application not found."
              : "Only accepted editors can request ownership.");
          return;
        }

        JsonObject duplicateQuery = new JsonObject()
          .put(APPLICATION_ID, applicationId)
          .put(APPLICATION_TYPE_FIELD, applicationType)
          .put(USER_ID, callerEcasId);

        mongoClient.findOne(OWNERSHIP_REQUEST_COLLECTION, duplicateQuery, null)
          .onSuccess(existingRequest -> {
            if (existingRequest != null) {
              setUpMessageContextResponse(context, HttpResponseStatus.CONFLICT,
                "An ownership request is already pending for this user and application.");
              return;
            }

            String requestId = new ObjectId().toHexString();
            JsonObject requestDoc = new JsonObject()
              .put(ID, requestId)
              .put(APPLICATION_ID, applicationId)
              .put(APPLICATION_TYPE_FIELD, applicationType)
              .put(USER_ID, callerEcasId)
              .put(USER_NAME, callerUserName)
              .put(JUSTIFICATION, justification)
              .put(CREATED_AT, Instant.now().toString());

            mongoClient.insert(OWNERSHIP_REQUEST_COLLECTION, requestDoc)
              .onSuccess(v -> {
                Future.all(
                    resolveUserIdentity(callerEcasId, callerUserName, callerEmail),
                    fetchApplicationTitle(applicationId, applicationType),
                    getApplicationOwnerId(applicationId, applicationType))
                  .onSuccess(payload -> {
                    JsonObject requesterIdentity = payload.resultAt(0);
                    String applicationTitle = payload.resultAt(1);
                    String ownerUserId = payload.resultAt(2);
                    String applicationLabel = formatApplicationReference(applicationId,
                      applicationTitle);

                    if (StringUtils.isNotBlank(ownerUserId)) {
                      Map<String, Object> properties = new HashMap<>();
                      properties.put(NotificationKeys.APPLICATION_NAME, applicationLabel);
                      notificationManager.createNotifications(
                          NotificationCode.COLLABORATION_OWNERSHIP_REQUESTED,
                          applicationId,
                          applicationType,
                          properties,
                          ownerUserId)
                        .onFailure(err -> log.error(
                          "Failed to send COLLABORATION_OWNERSHIP_REQUESTED notification for applicationId={} owner={}",
                          applicationId, ownerUserId, err));
                    }

                    auditLogManager.insertAuditLog(
                      AUDIT_OBJECT_APPLICATION_OWNERSHIP,
                      AUDIT_ACTION_REQUEST_OWNERSHIP,
                      "Ownership request submitted for Application " + applicationId
                        + " by user " + formatUserIdentity(
                        requesterIdentity.getString(USER_NAME),
                        requesterIdentity.getString(EMAIL),
                        callerEcasId) + ".",
                      context
                    );
                  })
                  .onFailure(err -> log.warn(
                    "requestOwnership follow-up preparation failed for applicationId={}: {}",
                    applicationId, err.getMessage()));

                setUpContextResponse(context, HttpResponseStatus.CREATED,
                  new JsonObject().put(ID, requestId).encode());
              })
              .onFailure(err -> {
                log.error("requestOwnership insert failed: {}", err.getMessage(), err);
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to create ownership request");
              });
          })
          .onFailure(err -> {
            log.error("requestOwnership duplicate check failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to validate ownership request");
          });
      })
      .onFailure(err -> {
        log.error("requestOwnership editor lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to validate collaborator role");
      });
  }

  // ---------------------------------------------------------------------------
  // GET /application/{applicationId}/collaborators/ownership-requests
  // ---------------------------------------------------------------------------

  /**
   * Returns all ownership requests for the given application.
   *
   * <p>Accessible by the owner of the application.
   * Pagination is supported via {@code page} and {@code limit} query parameters.
   *
   * @param context the Vert.x routing context; path param {@code applicationId} required
   */
  public void getOwnershipRequests(@NotNull RoutingContext context) {
    String applicationId = context.pathParam("applicationId");
    String callerEcasId  = Context.getEcasId(context);
    boolean adminAction  = isAdmin(context);

    Future<String> applicationTypeFuture = adminAction
      ? resolveApplicationType(applicationId)
      : resolveOwnedApplicationType(applicationId, callerEcasId);

    applicationTypeFuture
      .onSuccess(applicationType -> {
        if (StringUtils.isBlank(applicationType)) {
          setUpMessageContextResponse(context,
            adminAction ? HttpResponseStatus.NOT_FOUND : HttpResponseStatus.FORBIDDEN,
            adminAction
              ? "Application not found."
              : "Only the current owner can view ownership requests.");
          return;
        }

        FindOptions options = new FindOptions()
          .setSort(new JsonObject().put(CREATED_AT, -1));

        mongoClient.findWithOptions(OWNERSHIP_REQUEST_COLLECTION,
            new JsonObject()
              .put(APPLICATION_ID, applicationId)
              .put(APPLICATION_TYPE_FIELD, applicationType),
            options)
          .onSuccess(results -> setUpContextResponse(context, HttpResponseStatus.OK,
            new JsonArray(results).encode()))
          .onFailure(err -> {
            log.error("getOwnershipRequests query failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to retrieve ownership requests");
          });
      })
      .onFailure(err -> {
        log.error("getOwnershipRequests owner lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to validate ownership");
      });
  }

  // ---------------------------------------------------------------------------
  // PUT /application/{applicationId}/collaborators/ownership-requests/{requestId}/accept
  // ---------------------------------------------------------------------------

  /**
   * Accepts an ownership transfer request.
   *
   * <p>Only the current owner can accept ownership requests. Upon acceptance, the
   * ownership is transferred to the requester, and the request is removed from the
   * system.
   *
   * @param context the Vert.x routing context
   */
  public void acceptOwnershipRequest(@NotNull RoutingContext context) {
    String applicationId  = context.pathParam("applicationId");
    String requestId      = context.pathParam("requestId");
    String callerEcasId   = Context.getEcasId(context);
    String callerUserName = Context.getUserName(context);
    String callerEmail    = Context.getEcasEmail(context);
    boolean adminAction   = isAdmin(context);

    mongoClient.findOne(OWNERSHIP_REQUEST_COLLECTION, new JsonObject().put(ID, requestId), null)
      .onSuccess(requestDoc -> {
        if (requestDoc == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Ownership request not found");
          return;
        }

        if (!applicationId.equals(requestDoc.getString(APPLICATION_ID))) {
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            "Ownership request does not belong to this application.");
          return;
        }

        String applicationType = requestDoc.getString(APPLICATION_TYPE_FIELD);
        String requestedUserId = requestDoc.getString(USER_ID);

        getApplicationOwnerId(applicationId, applicationType)
          .onSuccess(ownerUserId -> {
            boolean isOwner = callerEcasId.equals(ownerUserId);
            if (!isOwner && !adminAction) {
              setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
                "Only the current owner or an administrator can accept ownership requests.");
              return;
            }

            JsonObject collaboratorFilter = new JsonObject()
              .put(APPLICATION_ID, applicationId)
              .put(APPLICATION_TYPE_FIELD, applicationType)
              .put(COLLABORATOR_USER_ID, requestedUserId)
              .put(ROLE, ROLE_EDITOR)
              .put(INVITATION_STATUS, INVITATION_ACCEPTED);

            mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION, collaboratorFilter, null)
              .onSuccess(newOwnerDoc -> {
                if (newOwnerDoc == null) {
                  mongoClient.removeDocument(OWNERSHIP_REQUEST_COLLECTION,
                      new JsonObject().put(ID, requestId))
                    .onComplete(removeResult -> {
                      if (removeResult.failed()) {
                        log.warn("acceptOwnershipRequest: failed to remove stale ownership request" +
                          " requestId={}: {}", requestId, removeResult.cause().getMessage());
                      }
                      setUpMessageContextResponse(context, HttpResponseStatus.CONFLICT,
                        "The requester is no longer an accepted editor on this application.");
                    });
                  return;
                }

                resolveOwnershipActor(applicationId, applicationType, ownerUserId,
                    isOwner ? callerUserName : null,
                    isOwner ? callerEmail : null)
                  .compose(ownerActor -> resolveUserIdentity(requestedUserId,
                      requestDoc.getString(USER_NAME), null)
                    .compose(requesterIdentity -> {
                      String approverName = isOwner
                        ? ownerActor.getString(USER_NAME)
                        : StringUtils.firstNonBlank(callerUserName, callerEcasId);
                      String approverEmail = isOwner
                        ? ownerActor.getString(EMAIL)
                        : callerEmail;
                      String auditDescription = "Ownership request transfer for Application "
                        + applicationId + " submitted by "
                        + formatUserIdentity(requesterIdentity.getString(USER_NAME),
                        requesterIdentity.getString(EMAIL), requestedUserId)
                        + " has been approved by "
                        + formatUserIdentity(approverName, approverEmail, callerEcasId) + ".";

                      return executeOwnershipTransfer(applicationId, applicationType,
                        ownerUserId,
                        ownerActor.getString(USER_NAME),
                        ownerActor.getString(EMAIL),
                        callerEcasId,
                        newOwnerDoc,
                        context,
                        NotificationCode.COLLABORATION_OWNERSHIP_REQUEST_APPROVED,
                        approverName,
                        AUDIT_OBJECT_APPLICATION_OWNERSHIP,
                        AUDIT_ACTION_OWNERSHIP_TRANSFER_APPROVED,
                        auditDescription);
                    }))
                  .compose(v -> mongoClient.removeDocuments(OWNERSHIP_REQUEST_COLLECTION,
                    new JsonObject()
                      .put(APPLICATION_ID, applicationId)
                      .put(APPLICATION_TYPE_FIELD, applicationType))
                    .onFailure(err ->
                      log.warn("acceptOwnershipRequest cleanup failed for applicationId={}: {}",
                        applicationId, err.getMessage()))
                    .recover(err -> Future.succeededFuture()))
                  .onSuccess(v -> setUpContextResponse(context, HttpResponseStatus.OK,
                    new JsonObject().put("message", "Ownership transferred successfully").encode()))
                  .onFailure(err -> {
                    log.error("acceptOwnershipRequest failed: {}", err.getMessage(), err);
                    setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                      "Failed to accept ownership request");
                  });
              })
              .onFailure(err -> {
                log.error("acceptOwnershipRequest collaborator lookup failed: {}", err.getMessage(), err);
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to validate requester collaboration");
              });
          })
          .onFailure(err -> {
            log.error("acceptOwnershipRequest owner check failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to validate ownership");
          });
      })
      .onFailure(err -> {
        log.error("acceptOwnershipRequest lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to find ownership request");
      });
  }

  // ---------------------------------------------------------------------------
  // PUT /application/{applicationId}/collaborators/ownership-requests/{requestId}/reject
  // ---------------------------------------------------------------------------

  /**
   * Rejects an ownership transfer request.
   *
   * <p>Only the current owner can reject ownership requests.
   *
   * @param context the Vert.x routing context
   */
  public void rejectOwnershipRequest(@NotNull RoutingContext context) {
    String applicationId = context.pathParam("applicationId");
    String requestId     = context.pathParam("requestId");
    String callerEcasId  = Context.getEcasId(context);
    String callerUserName = StringUtils.firstNonBlank(Context.getUserName(context), callerEcasId);
    String callerEmail = Context.getEcasEmail(context);
    boolean adminAction  = isAdmin(context);

    mongoClient.findOne(OWNERSHIP_REQUEST_COLLECTION, new JsonObject().put(ID, requestId), null)
      .onSuccess(requestDoc -> {
        if (requestDoc == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Ownership request not found");
          return;
        }

        if (!applicationId.equals(requestDoc.getString(APPLICATION_ID))) {
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            "Ownership request does not belong to this application.");
          return;
        }

        String applicationType = requestDoc.getString(APPLICATION_TYPE_FIELD);
        String requestedUserId = requestDoc.getString(USER_ID);
        getApplicationOwnerId(applicationId, applicationType)
          .onSuccess(ownerUserId -> {
            if (!callerEcasId.equals(ownerUserId) && !adminAction) {
              setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
                "Only the current owner or an administrator can reject ownership requests.");
              return;
            }

            mongoClient.removeDocument(OWNERSHIP_REQUEST_COLLECTION, new JsonObject().put(ID, requestId))
              .onSuccess(v -> {
                Future.all(
                    resolveUserIdentity(requestedUserId, requestDoc.getString(USER_NAME), null),
                    fetchApplicationTitle(applicationId, applicationType))
                  .onSuccess(payload -> {
                    JsonObject requesterIdentity = payload.resultAt(0);
                    String applicationTitle = payload.resultAt(1);
                    String applicationLabel = formatApplicationReference(applicationId,
                      applicationTitle);

                    Map<String, Object> properties = new HashMap<>();
                    properties.put(NotificationKeys.APPLICATION_NAME, applicationLabel);
                    properties.put(NotificationKeys.USER_NAME, callerUserName);
                    notificationManager.createNotifications(
                        NotificationCode.COLLABORATION_OWNERSHIP_REQUEST_REJECTED,
                        applicationId,
                        applicationType,
                        properties,
                        requestedUserId)
                      .onFailure(err -> log.error(
                        "Failed to send COLLABORATION_OWNERSHIP_REQUEST_REJECTED notification for applicationId={} requester={}",
                        applicationId, requestedUserId, err));

                    auditLogManager.insertAuditLog(
                      AUDIT_OBJECT_APPLICATION_OWNERSHIP,
                      AUDIT_ACTION_OWNERSHIP_TRANSFER_REJECTED,
                      "Ownership request transfer for Application " + applicationId
                        + " submitted by " + formatUserIdentity(
                        requesterIdentity.getString(USER_NAME),
                        requesterIdentity.getString(EMAIL), requestedUserId)
                        + " has been rejected by "
                        + formatUserIdentity(callerUserName, callerEmail, callerEcasId) + ".",
                      context
                    );
                  })
                  .onFailure(err -> log.warn(
                    "rejectOwnershipRequest follow-up preparation failed for applicationId={}: {}",
                    applicationId, err.getMessage()));

                setUpContextResponse(context, HttpResponseStatus.NO_CONTENT);
              })
              .onFailure(err -> {
                log.error("rejectOwnershipRequest delete failed: {}", err.getMessage(), err);
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to reject ownership request");
              });
          })
          .onFailure(err -> {
            log.error("rejectOwnershipRequest owner check failed: {}", err.getMessage(), err);
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to validate ownership");
          });
      })
      .onFailure(err -> {
        log.error("rejectOwnershipRequest lookup failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to find ownership request");
      });
  }

  // ---------------------------------------------------------------------------
  // GET /collaborations/mine
  // ---------------------------------------------------------------------------

  /**
   * Returns all applications on which the authenticated user is a collaborator.
   *
   * <p>Returns only entries with {@code invitationStatus=ACCEPTED} or {@code PENDING}.
   *
   * @param context the Vert.x routing context
   */
  public void getMyCollaborativeApplications(@NotNull RoutingContext context) {
    String callerEcasId = Context.getEcasId(context);

    JsonObject query = new JsonObject()
      .put(COLLABORATOR_USER_ID, callerEcasId)
      .put(INVITATION_STATUS, new JsonObject()
        .put("$in", new JsonArray()
          .add(INVITATION_ACCEPTED)
          .add(INVITATION_PENDING)));

    mongoClient.find(APPLICATION_COLLABORATOR_COLLECTION, query)
      .onSuccess(results -> setUpContextResponse(context, HttpResponseStatus.OK,
        new JsonArray(results).encode()))
      .onFailure(err -> {
        log.error("getMyCollaborativeApplications failed for ecasId='{}': {}",
          callerEcasId, err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to retrieve collaborative applications");
      });
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  /**
   * Returns the owner {@code userId} of the specified application.
   *
   * <p>Ownership is determined by the {@code userId} field on the application document
   * (set at creation time). No Keycloak lookup is needed.
   *
   * @param applicationId   the application document {@code _id}
   * @param applicationType either {@code DATA_ACCESS_APPLICATION} or {@code DATA_REQUEST_APPLICATION}
   * @return a {@code Future<String>} that resolves to the owner userId, or {@code null} if absent
   */
  private Future<String> getApplicationOwnerId(String applicationId, String applicationType) {
    String collection = resolveCollection(applicationType);
    if (collection == null) {
      return Future.failedFuture("Unknown applicationType: " + applicationType);
    }

    return mongoClient.findOne(collection,
        new JsonObject().put(ID, applicationId), new JsonObject().put(USER_ID, 1))
      .map(doc -> doc != null ? doc.getString(USER_ID) : null);
  }

  /**
   * Checks whether the caller is either the owner OR an accepted collaborator of the application.
   *
   * @param applicationId the application ID
   * @param ecasId        the caller's ECAS ID
   * @return a {@code Future<Boolean>} resolving to {@code true} if authorized
   */
  private Future<Boolean> isOwnerOrCollaborator(String applicationId, String ecasId) {
    // Check collaborator collection - owner entries may not be listed there,
    // so we also attempt to find any application where userId == ecasId.
    JsonObject collaboratorQuery = new JsonObject()
      .put(APPLICATION_ID, applicationId)
      .put(COLLABORATOR_USER_ID, ecasId)
      .put(INVITATION_STATUS, INVITATION_ACCEPTED);

    return mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION, collaboratorQuery, null)
      .compose(colDoc -> {
        if (colDoc != null) {
          return Future.succeededFuture(true);
        }
        // Check both application collections for ownership
        return isOwnerInAnyCollection(applicationId, ecasId);
      });
  }

  /**
   * Returns {@code true} if {@code ecasId} is the owner in any application collection.
   */
  private Future<Boolean> isOwnerInAnyCollection(String applicationId, String ecasId) {
    JsonObject ownerQuery = new JsonObject()
      .put(ID, applicationId)
      .put(USER_ID, ecasId);

    return mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, ownerQuery, null)
      .compose(doc -> {
        if (doc != null) {
          return Future.succeededFuture(true);
        }
        return mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, ownerQuery, null)
          .map(d -> d != null);
      });
  }

  /**
   * Checks whether an active (PENDING or ACCEPTED) collaborator entry already exists for the given
   * {@code invitedEmail} on the application.
   *
   * @param applicationId   the application ID
   * @param applicationType the application type
   * @param invitedEmail    the email being checked
   * @return {@code Future<Boolean>} resolving to {@code true} if a duplicate exists
   */
  private Future<Boolean> checkDuplicateCollaborator(String applicationId, String applicationType,
    String invitedEmail) {
    JsonObject query = new JsonObject()
      .put(APPLICATION_ID, applicationId)
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(
        INVITED_EMAIL,
        new JsonObject().put(REGEX, "^" + Pattern.quote(invitedEmail) + "$").put(OPTIONS, "i")
      )
      .put(
        INVITATION_STATUS,
        new JsonObject().put(
          "$in",
          new JsonArray().add(INVITATION_PENDING).add(INVITATION_ACCEPTED)
        )
      );

    return mongoClient
      .findOne(APPLICATION_COLLABORATOR_COLLECTION, query, null)
      .map(Objects::nonNull);
  }

  /**
   * Looks up the invited USER document fields needed to link a collaborator invite immediately.
   *
   * <p>Returns {@code null} if no matching user is found - in that case the
   * invite is stored as a "pending by email" entry and resolved later when the user logs in and
   * calls the bootstrap endpoint.
   *
   * @param email the email address to look up
   * @return a {@code Future} with the projected USER document, or {@code null} if not found
   */
  private Future<JsonObject> resolveCollaboratorUserByEmail(String email) {
    return mongoClient.findOne(
      USER_COLLECTION,
      new JsonObject().put(
        EMAIL,
        new JsonObject().put(REGEX, "^" + Pattern.quote(email) + "$").put(OPTIONS, "i")
      ),
      new JsonObject().put(ID, 1).put(USER_NAME, 1)
    );
  }

  /**
   * Fetches the {@code title} field of the specified application from MongoDB.
   *
   * <p>Only the {@code title} field is projected to keep the query lightweight.
   * Returns {@code null} if the application is not found or has no title set.
   *
   * @param applicationId   the application document {@code _id}
   * @param applicationType either {@code DATA_ACCESS_APPLICATION} or
   *                        {@code DATA_REQUEST_APPLICATION}
   * @return a {@code Future<String>} containing the title, or {@code null} if unavailable
   */
  private Future<String> fetchApplicationTitle(String applicationId, String applicationType) {
    String collection = resolveCollection(applicationType);
    if (collection == null) {
      return Future.succeededFuture(null);
    }
    return mongoClient.findOne(
        collection,
        new JsonObject().put(ID, applicationId),
        new JsonObject().put(TITLE, 1))
      .map(doc -> doc != null ? doc.getString(TITLE) : null)
      .otherwise((String) null); // non-critical – swallow any error and continue
  }

  /**
   * Resolves the MongoDB collection name for the given application type.
   *
   * @param applicationType the application type string
   * @return the collection name, or {@code null} if unknown
   */
  private String resolveCollection(String applicationType) {
    if (DATA_ACCESS_APPLICATION_COLLECTION.equals(applicationType)) {
      return DATA_ACCESS_APPLICATION_COLLECTION;
    } else if (DATA_REQUEST_APPLICATION_COLLECTION.equals(applicationType)) {
      return DATA_REQUEST_APPLICATION_COLLECTION;
    }
    return null;
  }

  /**
   * Safely parses an integer query parameter, returning a default if the value is null or invalid.
   */
  private int parseIntParam(String value, int defaultValue) {
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Builds the query for retrieving collaborators using stored collaborator fields.
   *
   * @param baseQuery the base query object to modify
   * @param searchTerm the search term (email or username) to match
   * @return a {@code Future<JsonObject>} containing the modified query
   */
  private Future<JsonObject> buildCollaboratorListQuery(JsonObject baseQuery, String searchTerm) {
    if (StringUtils.isBlank(searchTerm)) {
      return Future.succeededFuture(baseQuery);
    }

    JsonObject regexFilter = new JsonObject().put(REGEX, searchTerm).put(OPTIONS, "i");

    return Future.succeededFuture(baseQuery.copy()
      .put("$or", new JsonArray()
        .add(new JsonObject().put(INVITED_EMAIL, regexFilter))
        .add(new JsonObject().put(COLLABORATOR_USER_NAME, regexFilter))));
  }

  private Future<JsonObject> resolveAcceptedEditorCollaboration(String applicationId,
    String collaboratorUserId) {
    JsonObject query = new JsonObject()
      .put(APPLICATION_ID, applicationId)
      .put(COLLABORATOR_USER_ID, collaboratorUserId)
      .put(ROLE, ROLE_EDITOR)
      .put(INVITATION_STATUS, INVITATION_ACCEPTED);

    return mongoClient.find(APPLICATION_COLLABORATOR_COLLECTION, query)
      .compose(matches -> {
        if (matches == null || matches.isEmpty()) {
          return Future.succeededFuture(null);
        }
        if (matches.size() > 1) {
          return Future.failedFuture(
            "Multiple accepted editor records found for the same application and user");
        }
        return Future.succeededFuture(matches.get(0));
      });
  }

  private Future<String> resolveOwnedApplicationType(String applicationId, String ownerUserId) {
    JsonObject ownerQuery = new JsonObject()
      .put(ID, applicationId)
      .put(USER_ID, ownerUserId);

    return mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, ownerQuery,
        new JsonObject().put(ID, 1))
      .compose(accessDoc -> mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, ownerQuery,
          new JsonObject().put(ID, 1))
        .compose(requestDoc -> {
          if (accessDoc != null && requestDoc != null) {
            return Future.failedFuture(
              "Application id exists in multiple collections; applicationType is ambiguous");
          }
          if (accessDoc != null) {
            return Future.succeededFuture(DATA_ACCESS_APPLICATION_COLLECTION);
          }
          if (requestDoc != null) {
            return Future.succeededFuture(DATA_REQUEST_APPLICATION_COLLECTION);
          }
          return Future.succeededFuture(null);
        }));
  }

  private Future<String> resolveApplicationType(String applicationId) {
    JsonObject query = new JsonObject().put(ID, applicationId);

    return mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, query,
        new JsonObject().put(ID, 1))
      .compose(accessDoc -> mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, query,
          new JsonObject().put(ID, 1))
        .compose(requestDoc -> {
          if (accessDoc != null && requestDoc != null) {
            return Future.failedFuture(
              "Application id exists in multiple collections; applicationType is ambiguous");
          }
          if (accessDoc != null) {
            return Future.succeededFuture(DATA_ACCESS_APPLICATION_COLLECTION);
          }
          if (requestDoc != null) {
            return Future.succeededFuture(DATA_REQUEST_APPLICATION_COLLECTION);
          }
          return Future.succeededFuture(null);
        }));
  }

  private Future<JsonObject> resolveOwnershipActor(String applicationId, String applicationType,
    String ownerUserId, String fallbackUserName, String fallbackEmail) {
    if (StringUtils.isBlank(ownerUserId)) {
      return Future.failedFuture("Application owner not found");
    }

    JsonObject ownerCollaboratorQuery = new JsonObject()
      .put(APPLICATION_ID, applicationId)
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(COLLABORATOR_USER_ID, ownerUserId)
      .put(ROLE, ROLE_OWNER);

    return mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION, ownerCollaboratorQuery,
        new JsonObject()
          .put(COLLABORATOR_USER_NAME, 1)
          .put(INVITED_EMAIL, 1))
      .compose(ownerCollaborator -> mongoClient.findOne(USER_COLLECTION,
          new JsonObject().put(ID, ownerUserId),
          new JsonObject().put(USER_NAME, 1).put(EMAIL, 1))
        .map(ownerUser -> new JsonObject()
          .put(USER_ID, ownerUserId)
          .put(USER_NAME, StringUtils.firstNonBlank(
            ownerCollaborator != null ? ownerCollaborator.getString(COLLABORATOR_USER_NAME) : null,
            ownerUser != null ? ownerUser.getString(USER_NAME) : null,
            fallbackUserName,
            ownerUserId))
          .put(EMAIL, StringUtils.firstNonBlank(
            ownerCollaborator != null ? ownerCollaborator.getString(INVITED_EMAIL) : null,
            ownerUser != null ? ownerUser.getString(EMAIL) : null,
            fallbackEmail))));
  }

  private boolean isAdmin(RoutingContext context) {
    if (context.user() == null || context.user().principal() == null) {
      return false;
    }

    JsonObject principal = context.user().principal();
    if (containsAdminRole(principal.getValue("roles"))) {
      return true;
    }

    JsonObject realmAccess = principal.getJsonObject("realm_access");
    return realmAccess != null && containsAdminRole(realmAccess.getValue("roles"));
  }

  private boolean containsAdminRole(Object roles) {
    if (roles instanceof JsonArray jsonRoles) {
      return jsonRoles.contains(DATA_ADMIN);
    }
    if (roles instanceof Iterable<?> iterableRoles) {
      for (Object role : iterableRoles) {
        if (DATA_ADMIN.equals(String.valueOf(role))) {
          return true;
        }
      }
    }
    if (roles instanceof String rolesString) {
      return rolesString.contains(DATA_ADMIN);
    }
    return false;
  }

  private Future<Void> executeOwnershipTransfer(String applicationId, String applicationType,
    String currentOwnerUserId, String currentOwnerUserName, String currentOwnerEmail,
    String actingUserId, JsonObject newOwnerDoc,
    RoutingContext context,
    NotificationCode newOwnerNotificationCode,
    String actingUserName,
    String auditObject,
    String auditAction,
    String auditDescriptionOverride) {
    String collection = resolveCollection(applicationType);
    if (collection == null) {
      return Future.failedFuture("Unknown applicationType: " + applicationType);
    }

    String newOwnerCollaboratorId = newOwnerDoc.getString(ID);
    String newOwnerUserId = newOwnerDoc.getString(COLLABORATOR_USER_ID);
    if (StringUtils.isBlank(newOwnerCollaboratorId) || StringUtils.isBlank(newOwnerUserId)) {
      return Future.failedFuture("Target collaborator is missing required fields");
    }

    String safeCurrentOwnerUserName = StringUtils.firstNonBlank(currentOwnerUserName, currentOwnerUserId);

    JsonObject demoteFilter = new JsonObject()
      .put(APPLICATION_ID, applicationId)
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(COLLABORATOR_USER_ID, currentOwnerUserId)
      .put(ROLE, ROLE_OWNER);

    JsonObject demoteUpdate = new JsonObject()
      .put("$set", new JsonObject().put(ROLE, ROLE_EDITOR));

    JsonObject formerOwnerFilter = new JsonObject()
      .put(APPLICATION_ID, applicationId)
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(COLLABORATOR_USER_ID, currentOwnerUserId);

    JsonObject formerOwnerSet = new JsonObject()
      .put(ROLE, ROLE_EDITOR)
      .put(INVITATION_STATUS, INVITATION_ACCEPTED)
      .put(COLLABORATOR_USER_NAME, safeCurrentOwnerUserName);
    if (StringUtils.isNotBlank(currentOwnerEmail)) {
      formerOwnerSet.put(INVITED_EMAIL, currentOwnerEmail);
    }

    JsonObject formerOwnerUpdate = new JsonObject()
      .put("$set", formerOwnerSet)
      .put("$setOnInsert", new JsonObject()
        .put(ID, new ObjectId().toHexString())
        .put(APPLICATION_ID, applicationId)
        .put(APPLICATION_TYPE_FIELD, applicationType)
        .put(COLLABORATOR_USER_ID, currentOwnerUserId)
        .put(INVITED_BY, actingUserId)
        .put(CREATED_AT, Instant.now().toString()));

    JsonObject removeNewOwnerFilter = new JsonObject()
      .put(ID, newOwnerCollaboratorId)
      .put(APPLICATION_ID, applicationId)
      .put(APPLICATION_TYPE_FIELD, applicationType);

    JsonObject applicationOwnerUpdate = new JsonObject()
      .put("$set", new JsonObject().put(USER_ID, newOwnerUserId));

    return mongoClient.updateCollection(APPLICATION_COLLABORATOR_COLLECTION, demoteFilter, demoteUpdate)
      .compose(res -> mongoClient.updateCollectionWithOptions(APPLICATION_COLLABORATOR_COLLECTION,
        formerOwnerFilter, formerOwnerUpdate, new UpdateOptions().setUpsert(true)))
      .compose(res -> mongoClient.removeDocument(APPLICATION_COLLABORATOR_COLLECTION,
        removeNewOwnerFilter))
      .compose(res -> mongoClient.updateCollection(collection,
        new JsonObject().put(ID, applicationId), applicationOwnerUpdate))
      .compose(res -> fetchApplicationTitle(applicationId, applicationType).map(appTitle -> {
        String applicationLabel = formatApplicationReference(applicationId, appTitle);
        Map<String, Object> properties = new HashMap<>();
        properties.put(NotificationKeys.APPLICATION_NAME, applicationLabel);
        if (StringUtils.isNotBlank(actingUserName)) {
          properties.put(NotificationKeys.USER_NAME, actingUserName);
        }

        notificationManager.createNotifications(
            newOwnerNotificationCode,
            applicationId,
            applicationType,
            properties,
            newOwnerUserId)
          .onFailure(err ->
            log.error(
              "Failed to send {} notification for applicationId={} newOwner={}",
              newOwnerNotificationCode, applicationId, newOwnerUserId, err)
           );

        if (!Objects.equals(actingUserId, currentOwnerUserId)) {
          Map<String, Object> formerOwnerProperties = new HashMap<>();
          formerOwnerProperties.put(NotificationKeys.APPLICATION_NAME, applicationLabel);
          notificationManager.createNotifications(
              NotificationCode.COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN,
              applicationId,
              applicationType,
              formerOwnerProperties,
              currentOwnerUserId)
            .onFailure(err ->
              log.error(
                "Failed to send COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN notification for applicationId={} formerOwner={}",
                applicationId, currentOwnerUserId, err)
            );
        }

        String auditDescription = StringUtils.defaultIfBlank(auditDescriptionOverride,
          "Ownership transferred: applicationId=" + applicationId
            + " from=" + currentOwnerUserId + " to=" + newOwnerUserId
            + (!Objects.equals(actingUserId, currentOwnerUserId)
             ? " actedBy=" + actingUserId : ""));

        auditLogManager.insertAuditLog(
          StringUtils.defaultIfBlank(auditObject, "Application"),
          StringUtils.defaultIfBlank(auditAction, "Application updated"),
          auditDescription,
          context
        );

        log.info("Ownership transferred: applicationId='{}' from='{}' to='{}' actedBy='{}'",
          applicationId, currentOwnerUserId, newOwnerUserId, actingUserId);
        return null;
      }));
  }

  private String formatApplicationReference(String applicationId, String applicationTitle) {
    return StringUtils.isBlank(applicationTitle)
      ? applicationId
      : applicationId + " – " + applicationTitle;
  }

  private Future<JsonObject> resolveUserIdentity(String userId, String fallbackUserName,
    String fallbackEmail) {
    if (StringUtils.isBlank(userId)) {
      return Future.succeededFuture(new JsonObject()
        .put(USER_NAME, fallbackUserName)
        .put(EMAIL, fallbackEmail));
    }

    return mongoClient.findOne(USER_COLLECTION,
        new JsonObject().put(ID, userId),
        new JsonObject().put(USER_NAME, 1).put(EMAIL, 1))
      .map(userDoc -> new JsonObject()
        .put(USER_NAME, StringUtils.firstNonBlank(
          userDoc != null ? userDoc.getString(USER_NAME) : null,
          fallbackUserName,
          userId))
        .put(EMAIL, StringUtils.firstNonBlank(
          userDoc != null ? userDoc.getString(EMAIL) : null,
          fallbackEmail)));
  }

  private String formatUserIdentity(String userName, String email, String fallbackUserId) {
    String resolvedUserName = StringUtils.firstNonBlank(userName, fallbackUserId);
    if (StringUtils.isBlank(email)) {
      return resolvedUserName;
    }
    return resolvedUserName + " / " + email;
  }
}
