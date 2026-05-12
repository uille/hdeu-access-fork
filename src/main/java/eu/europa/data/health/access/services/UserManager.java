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
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_ID;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_NAME;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.FAVOURITES_COLLECTION;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INVITATION_ACCEPTED;
import static eu.europa.data.health.access.Constants.INVITATION_PENDING;
import static eu.europa.data.health.access.Constants.INVITATION_STATUS;
import static eu.europa.data.health.access.Constants.INVITED_EMAIL;
import static eu.europa.data.health.access.Constants.NOTIFICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.USER_NAME;

import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.NotificationKeys;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager extends AbstractManager {

  public static final String ERROR_WHILE_DELETING_USER_FROM_KEYCLOAK = "Error while deleting user from keycloak";
  private final MongoClient mongoClient;
  private final DispatcherClient dispatcherClient;
  private final AuditLogManager auditLogManager;
  private final AuthClient authClient;
  private final NotificationManager notificationManager;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public UserManager(DBHandler dbHandler, MongoClient mongoClient,
    DispatcherClient dispatcherClient, AuditLogManager auditLogManager, AuthClient authClient,
    NotificationManager notificationManager) {
    super(dbHandler);
    this.mongoClient = mongoClient;
    this.dispatcherClient = dispatcherClient;
    this.auditLogManager = auditLogManager;
    this.authClient = authClient;
    this.notificationManager = notificationManager;
  }

  public void createOrUpdateUser(@NotNull RoutingContext context) {

    JsonObject requestBody = context.body().asJsonObject();
    String ecasId = Context.getEcasId(context);

    createOrUpdate(requestBody, ecasId).onComplete(ar -> {
      if (ar.succeeded()) {
        context.response().setStatusCode(200).end("Document created or updated successfully");
      } else {
        context.response().setStatusCode(500).end("Failed");
      }
    });
  }

  private Future<Void> createOrUpdate(JsonObject object, String ecasId) {

    JsonObject query = new JsonObject().put("_id", ecasId);
    // Do not replace system fields
    object.remove("_id");
    object.remove("email");
    object.remove(USER_NAME);
    object.remove("lastLoginAt");
    object.remove("date_created");

    // Prevent preferences accidental deletion via null
    if (object.containsKey("emailNotificationPreferences") &&
      object.getValue("emailNotificationPreferences") == null) {
      object.remove("emailNotificationPreferences");
    }

    object.put("date_modified", Instant.now().toString());

    JsonObject update = new JsonObject()
      .put("$set", object)
      .put("$setOnInsert", new JsonObject()
        .put("_id", ecasId)
        .put("date_created", Instant.now().toString())
      );

    UpdateOptions options = new UpdateOptions().setUpsert(true);

    return mongoClient
      .updateCollectionWithOptions(USER_COLLECTION, query, update, options)
      .mapEmpty();
  }


  public void getUser(@NotNull RoutingContext context) {

    String ecasId = Context.getEcasId(context);
    respondWithUserById(context, ecasId);

  }

  public void getUserById(@NotNull RoutingContext context) {
    String userId = context.pathParam("userId");
    if (userId == null || userId.isBlank()) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Missing userId");
      return;
    }

    respondWithUserById(context, userId);
  }

  private void respondWithUserById(@NotNull RoutingContext context, String userId) {
    dbHandler.findById(USER_COLLECTION, userId)
      .onSuccess(jsonObject -> {
        if (jsonObject == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND, "User not found");
          return;
        }

        jsonObject.remove("_id");
        setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode());
      })
      .onFailure(cause ->
        setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND, "User not found")
      );
  }

  /**
   * Update user email notification preferences
   */
  public void updateUserEmailPreferences(@NotNull RoutingContext context) {
    JsonObject requestBody = context.body().asJsonObject();
    String ecasId = Context.getEcasId(context);

    updateEmailPreferences(requestBody, ecasId).onComplete(ar -> {
      if (ar.succeeded()) {
        context.response().setStatusCode(200).end("Email preferences updated successfully");
      } else {
        context.response().setStatusCode(500).end("Failed to update email preferences");
      }
    });
  }

  private Future<Void> updateEmailPreferences(JsonObject emailPreferences, String ecasId) {
    JsonObject query = new JsonObject().put("_id", ecasId);
    JsonObject update = new JsonObject()
      .put("$set", new JsonObject()
        .put("emailNotificationPreferences", emailPreferences)
        .put("date_modified", Instant.now()));

    return mongoClient.updateCollection(USER_COLLECTION, query, update)
      .mapEmpty();
  }

  /**
   * Delete user info and all associated entities based on userId
   */
  public void deleteUser(@NotNull RoutingContext context) {

    // Get the ecasId and userId from the context
    String userId = Context.getEcasId(context);
    deleteUserHealthData(context, userId, false);
  }

  /**
   * Administrator panel,  delete user info and all associated entities based on userId
   */
  public void adminDeleteUser(@NotNull RoutingContext context) {
    // Get the username (used as id in the mongo collection)
    String userId = context.request().getParam("userId");
    String username = context.request().getParam("username");
    if (userId == null || username == null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Missing userId or username");
      return;
    }
    // Delete user from Keycloak
    authClient.deleteUser(context, userId)
      .onSuccess(v -> {
        // Continue with further actions (e.g., delete user from health data)
        deleteUserHealthData(context, username, true);
      })
      .onFailure(
        err -> {
          log.error(ERROR_WHILE_DELETING_USER_FROM_KEYCLOAK , err);
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error while deleting user from keycloak: " + err.getMessage());
        });
  }

  /**
   * Delete user info and all associated entities based on userId
   */
  void deleteUserHealthData(@NotNull RoutingContext context, String userId, boolean admin) {
    JsonObject queryUserDocument = new JsonObject().put("_id", userId);
    // Query to find documents with the given userId
    JsonObject queryUserId = new JsonObject().put("userId", userId);
    JsonObject queryFavourites = new JsonObject().put(USER_ID, userId);
    JsonObject queryNotifications = new JsonObject().put(USER_ID, userId);

    // Find all access and request applications for this user
    Future<List<JsonObject>> accessApplicationsQuery = mongoClient.find(
      DATA_ACCESS_APPLICATION_COLLECTION, queryUserId);
    Future<List<JsonObject>> requestApplicationQuery = mongoClient.find(
      DATA_REQUEST_APPLICATION_COLLECTION, queryUserId);

    CompositeFuture combined = Future.join(accessApplicationsQuery, requestApplicationQuery);
    combined.onComplete(result -> {
        if (result.succeeded()) {
          // Retrieve results
          List<JsonObject> accessApplicationResults = accessApplicationsQuery.result();
          List<JsonObject> requestApplicationResults = requestApplicationQuery.result();

          if (accessApplicationResults.isEmpty() && requestApplicationResults.isEmpty()) {
            // No applications found for this user, delete favourites and notifications then proceed to delete the user
            Future.join(
                dbHandler.deleteAll(FAVOURITES_COLLECTION, queryFavourites),
                dbHandler.deleteAll(NOTIFICATION_COLLECTION, queryNotifications)
              )
              .onSuccess(v -> deleteUserEntity(context, queryUserDocument, admin))
              .onFailure(cause ->
                setUpMessageContextResponse(
                  context,
                  HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to delete favourites or notifications"
                )
              );
          } else {
            //Prepare an object of 2 arrays (access/request applications) for sending them to dispatcher
            List<String> accessIds = accessApplicationResults.stream()
              .map(app -> app.getString("_id")).toList();
            List<String> requestIds = requestApplicationResults.stream()
              .map(app -> app.getString("_id")).toList();
            JsonObject applications = new JsonObject();
            if (!accessIds.isEmpty()) {
              applications.put("accessApplications", new JsonArray(accessIds));
            }
            if (!requestIds.isEmpty()) {
              applications.put("requestApplications", new JsonArray(requestIds));
            }

            // Delete applications on Dispatcher
            dispatcherClient.deleteApplicationOnDispatcher(applications)
              .onSuccess(dispatcherClientResult -> {

                // UC 4.3 – notify accepted collaborators of every owned application
                // that is about to be deleted because the owner's profile is deleted.
                // Fire-and-forget; failures are logged but do not block the delete flow.
                notifyCollaboratorsForDeletedApps(accessApplicationResults, DATA_ACCESS_APPLICATION_COLLECTION);
                notifyCollaboratorsForDeletedApps(requestApplicationResults, DATA_REQUEST_APPLICATION_COLLECTION);

                // Delete documents from DISPATCHER collection based on userId
                JsonObject dispatcherDeleteQuery = new JsonObject()
                  .put("application.userId", userId);

                dbHandler.deleteAll(DISPATCHER_COLLECTION, dispatcherDeleteQuery)
                  .onSuccess(dispatcherDeleteResult -> {

                    // Delete documents from DATA_ACCESS_APPLICATION_COLLECTION and DATA_REQUEST_APPLICATION
                    Future<Integer> accessApplicationsDeleteQuery = dbHandler.deleteAll(
                      DATA_ACCESS_APPLICATION_COLLECTION, queryUserId);
                    Future<Integer> requestApplicationsDeleteQuery = dbHandler.deleteAll(
                      DATA_REQUEST_APPLICATION_COLLECTION, queryUserId);
                    Future<Integer> favouritesDeleteQuery = dbHandler.deleteAll(
                      FAVOURITES_COLLECTION,
                      queryFavourites
                    );
                    Future<Integer> notificationsDeleteQuery = dbHandler.deleteAll(
                      NOTIFICATION_COLLECTION,
                      queryNotifications
                    );

                    CompositeFuture deleteQuery = Future.join(
                      accessApplicationsDeleteQuery,
                      requestApplicationsDeleteQuery,
                      favouritesDeleteQuery,
                      notificationsDeleteQuery
                    );
                    deleteQuery
                      .onSuccess(deleteQueryResult -> deleteUserEntity(context, queryUserDocument,
                        admin))
                      .onFailure(cause -> setUpMessageContextResponse(context,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete applications, favourites or notifications"));
                  }).onFailure(
                    cause -> {
                      log.error("Failed to delete EU Dispatcher records", cause);
                      setUpMessageContextResponse(context,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete EU Dispatcher records");
                    });
              }).onFailure(
                cause -> {
                  log.error("Failed to delete DISPATCHER collection records", cause);
                  setUpMessageContextResponse(context,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete DISPATCHER collection records");
                });
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Error while retrieving applications from the database:, " + result.cause()
              .getMessage());
        }
      });
  }

  /**
   * For each application in the list, queries {@code APPLICATION_COLLABORATOR} for accepted
   * collaborators and sends them an {@link NotificationCode#APPLICATION_DISCARDED_BY_OWNER}
   * notification (UC 4.3 – owner's profile deleted, so all owned applications are deleted).
   *
   * <p>Fire-and-forget: failures are only logged and do not block the delete flow.
   *
   * @param applications     the list of application documents owned by the deleted user
   * @param applicationType  the collection name ({@code DATA_ACCESS_APPLICATION} etc.)
   */
  private void notifyCollaboratorsForDeletedApps(
    List<JsonObject> applications,
    String applicationType
  ) {
    if (applications == null || applications.isEmpty()) {
      return;
    }

    for (JsonObject app : applications) {
      String appId = app.getString(ID);
      String appTitle = app.getString(TITLE);

      if (StringUtils.isBlank(appId)) {
        continue;
      }

      JsonObject query = new JsonObject()
        .put(APPLICATION_ID, appId)
        .put(INVITATION_STATUS, INVITATION_ACCEPTED);

      mongoClient.find(APPLICATION_COLLABORATOR_COLLECTION, query)
        .onSuccess(collaborators -> {
          if (collaborators == null || collaborators.isEmpty()) {
            return;
          }

          Map<String, Object> properties = new HashMap<>();
          properties.put(NotificationKeys.APPLICATION_NAME, appTitle != null ? appTitle : "");

          for (JsonObject collab : collaborators) {
            String collaboratorUserId = collab.getString(COLLABORATOR_USER_ID);
            if (StringUtils.isNotBlank(collaboratorUserId)) {
              notificationManager.createNotifications(
                  NotificationCode.APPLICATION_DISCARDED_BY_OWNER,
                  appId,
                  applicationType,
                  properties,
                  collaboratorUserId)
                .onFailure(err ->
                  log.error(
                    "Failed to send APPLICATION_DISCARDED_BY_OWNER notification for applicationId={} userId={}",
                    appId, collaboratorUserId, err)
                );
            }
          }
        })
        .onFailure(err ->
          log.error(
            "Failed to fetch collaborators for discard notification on profile delete, applicationId={}",
            appId, err)
        );
    }
  }

  private void deleteUserEntity(@NotNull RoutingContext context, JsonObject object, boolean admin) {
    dbHandler.deleteByField(USER_COLLECTION, object)
      .onComplete(result -> {
        //Audit Log
        if(admin){
          String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
            AuditLogActionDescription.USER_DELETED.toString(), object.getString(ID));
          auditLogManager.insertAuditLog(AuditLogObject.USER_ROLE_MANAGEMENT.toString(),
            AuditLogAction.USER_DELETED.toString(),
            auditLogActionDescription, context);
        } else {
          String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
            AuditLogActionDescription.USER_ACCOUNT_DELETED.toString(), object.getString(ID));
          auditLogManager.insertAuditLog(AuditLogObject.USER_ACCOUNT.toString(),
            AuditLogAction.USER_ACCOUNT.toString(),
            auditLogActionDescription, context);
        }
      })
      .onSuccess(jsonObject -> setUpContextResponse(context, HttpResponseStatus.NO_CONTENT))
      .onFailure(cause -> setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
        "User not found"));
  }

  // ---------------------------------------------------------------------------
  // Bootstrap endpoint — called once per login from the frontend
  // ---------------------------------------------------------------------------

  /**
   * Bootstrap the authenticated user record in MongoDB.
   *
   * <p>This endpoint is called by the frontend immediately after authentication.
   * It ensures that every authenticated user has a persisted {@code USER} document
   * that includes their {@code email} claim from the Keycloak JWT token and their
   * derived {@code userName} from the authenticated principal.
   *
   * <p>Logic:
   * <ol>
   *   <li>Reads {@code preferred_username}, {@code email}, and name claims from the JWT principal
   *       (no Keycloak admin lookup required).</li>
   *   <li>If email is absent in the token, rejects with 400 — Keycloak must be
   *       configured to include the {@code email} claim via a mapper/client scope.</li>
   *   <li>Upserts the {@code USER} document:
   *       <ul>
   *         <li>Creates the document if it does not exist.</li>
   *         <li>Sets {@code email} and {@code userName} from the current token/context.</li>
   *         <li>Updates {@code lastLoginAt} on every call.</li>
   *       </ul>
   *   </li>
   *   <li>After a successful upsert, resolves any pending email-based collaborator
   *       invitations that match this user's email address.</li>
   * </ol>
   *
   * @param context the Vert.x routing context carrying the authenticated user principal
   */
  public void bootstrapUser(@NotNull RoutingContext context) {
    // -- 1. Extract claims from JWT (no Keycloak admin call needed) ----------
    String ecasId = Context.getEcasId(context);
    String email = Context.getEcasEmail(context);
    String userName = Context.getUserName(context);

    if (email == null || email.isBlank()) {
      log.warn("Bootstrap rejected for user '{}': token contains no email claim. "
        + "Configure a Keycloak mapper to include the 'email' claim in the token.", ecasId);
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Token does not contain an email claim. "
          + "Please contact the administrator to configure the Keycloak email mapper.");
      return;
    }

    // -- 2. Upsert USER document with email and lastLoginAt -------------------
    JsonObject query      = new JsonObject().put(ID, ecasId);
    Instant now           = Instant.now();

    // $set is applied on every upsert (create OR update)
    JsonObject setFields  = new JsonObject()
        .put(EMAIL, email)
        .put(USER_NAME, userName)
        .put("lastLoginAt", now.toString());

    // $setOnInsert is applied only when a new document is created
    JsonObject setOnInsertFields = new JsonObject()
        .put(ID, ecasId)
        .put("date_created", now.toString());

    JsonObject updateDoc = new JsonObject()
        .put("$set", setFields)
        .put("$setOnInsert", setOnInsertFields);

    UpdateOptions upsertOptions = new UpdateOptions().setUpsert(true);

    mongoClient.updateCollectionWithOptions(USER_COLLECTION, query, updateDoc, upsertOptions)
      .onSuccess(result -> {
        log.info("Bootstrap: USER record upserted for ecasId='{}' email='{}'", ecasId, email);

        // -- 3. Resolve pending email invitations for this user ---------------
        resolvePendingCollaboratorInvites(ecasId, email, userName);

        // Return the bootstrapped user info
        JsonObject responseBody = new JsonObject()
            .put("ecasId", ecasId)
            .put("email", email)
            .put(USER_NAME, userName);
        setUpContextResponse(context, HttpResponseStatus.OK, responseBody.encode());
      })
      .onFailure(err -> {
        log.error("Bootstrap: failed to upsert USER for ecasId='{}': {}", ecasId, err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to bootstrap user: " + err.getMessage());
      });
  }

  /**
   * Resolves pending collaborator invites by email.
   *
   * <p>When a user was invited by email before they had a USER record, the invite
   * is stored as {@code invitationStatus=PENDING} with {@code invitedEmail} set
   * and {@code collaboratorUserId=null}.  On login bootstrap, we link those
   * invites to the real userId so that the collaboration is properly tracked.
   *
   * <p>This method is fire-and-forget: failures are logged but do NOT affect
   * the HTTP response of the bootstrap endpoint.
   *
   * @param ecasId the logged-in user's ECAS ID (= MongoDB {@code _id} in USER)
   * @param email  the email address from the JWT claim
   * @param userName the current auth-derived user name to persist on collaborator docs
   */
  private void resolvePendingCollaboratorInvites(String ecasId, String email, String userName) {
    // Find all PENDING invites addressed to this email where userId is not yet set.
    // NOTE: Vert.x 4 UpdateOptions does NOT have setMulti() — it was removed.
    // We find all matching documents first, then update each individually by _id.
    JsonObject pendingFilter = new JsonObject()
        .put(INVITED_EMAIL, email)
        .put(INVITATION_STATUS, INVITATION_PENDING)
        .put(COLLABORATOR_USER_ID, new JsonObject().put("$exists", false));

    mongoClient.find(APPLICATION_COLLABORATOR_COLLECTION, pendingFilter)
      .onSuccess(docs -> {
        if (docs == null || docs.isEmpty()) {
          log.debug("Bootstrap: no pending invites to resolve for email='{}'", email);
          return;
        }

        log.info("Bootstrap: resolving {} pending collaborator invite(s) for email='{}'",
            docs.size(), email);

        // Update each document individually (Vert.x 4 has no multi-update API)
        List<Future<?>> updates = new java.util.ArrayList<>();
        for (JsonObject doc : docs) {
          String docId = doc.getString(ID);
          if (docId == null) continue;

          JsonObject filter = new JsonObject().put(ID, docId);
          JsonObject update = new JsonObject()
              .put("$set", new JsonObject()
                .put(COLLABORATOR_USER_ID, ecasId)
                .put(COLLABORATOR_USER_NAME, userName));

          updates.add(
            mongoClient.updateCollection(APPLICATION_COLLABORATOR_COLLECTION, filter, update)
              .onFailure(err -> log.warn(
                "Bootstrap: failed to resolve invite '{}' for email='{}': {}",
                docId, email, err.getMessage()))
          );
        }

        // Wait for all updates and log completion
        Future.all(updates)
          .onSuccess(v -> log.info(
              "Bootstrap: finished resolving {} pending invite(s) for email='{}'",
              docs.size(), email))
          .onFailure(err -> log.warn(
              "Bootstrap: some invite resolutions failed for email='{}': {}",
              email, err.getMessage()));
      })
      .onFailure(err -> log.warn(
          "Bootstrap: failed to query pending collaborator invites for email='{}': {}",
          email, err.getMessage()));
  }

}
