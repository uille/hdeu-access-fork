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
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;

import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Context;
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
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager extends AbstractManager {

  public static final String ERROR_WHILE_DELETING_USER_FROM_KEYCLOAK = "Error while deleting user from keycloak";
  private final MongoClient mongoClient;

  private final DBHandler dbHandler;

  private final DispatcherClient dispatcherClient;

  private final AuditLogManager auditLogManager;

  private final AuthClient authClient;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public UserManager(DBHandler dbHandler, MongoClient mongoClient,
    DispatcherClient dispatcherClient, AuditLogManager auditLogManager, AuthClient authClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.mongoClient = mongoClient;
    this.dispatcherClient = dispatcherClient;
    this.auditLogManager = auditLogManager;
    this.authClient = authClient;
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
    object.put("_id", ecasId);
    object.put("date_modified", Instant.now());

    UpdateOptions options = new UpdateOptions().setUpsert(true);

    return mongoClient.replaceDocumentsWithOptions(USER_COLLECTION, query, object, options)
      .mapEmpty();
  }


  public void getUser(@NotNull RoutingContext context) {

    String ecasId = Context.getEcasId(context);

    dbHandler.findById(USER_COLLECTION, ecasId)
      .onSuccess(
        jsonObject -> {
          jsonObject.remove("_id");
          setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode());
        })
      .onFailure(cause -> setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
        "User not found"));

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
            // No applications found for this user, proceed to delete the user
            deleteUserEntity(context, queryUserDocument, admin);
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

                    CompositeFuture deleteQuery = Future.join(accessApplicationsDeleteQuery,
                      requestApplicationsDeleteQuery);
                    deleteQuery
                      .onComplete(deleteQueryResult -> {
                        if (result.succeeded()) {

                          // Delete the user
                          deleteUserEntity(context, queryUserDocument, admin);

                        }
                      }).onFailure(cause -> setUpMessageContextResponse(context,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete applications"));
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

}
