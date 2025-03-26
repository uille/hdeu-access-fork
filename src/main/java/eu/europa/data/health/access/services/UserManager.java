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

import static eu.europa.data.health.access.Constants.APPLICATION_AUDIT_LOG;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_DELETED;

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

public class UserManager extends AbstractManager {

  private final MongoClient mongoClient;

  private final DBHandler dbHandler;

  private final DispatcherClient dispatcherClient;

  public UserManager(DBHandler dbHandler, MongoClient mongoClient,
    DispatcherClient dispatcherClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.mongoClient = mongoClient;

    this.dispatcherClient = dispatcherClient;
  }

  public void createOrUpdateUser(@NotNull RoutingContext context) {

    JsonObject requestBody = context.body().asJsonObject();
    String ecasId = context.user().get("sub");

    createOrUpdate(USER_COLLECTION, requestBody, ecasId).onComplete(ar -> {
      if (ar.succeeded()) {
        context.response().setStatusCode(200).end("Document created or updated successfully");
      } else {
        context.response().setStatusCode(500).end("Failed");
      }
    });
  }

  private Future<Void> createOrUpdate(String collection, JsonObject object, String ecasId) {

    JsonObject query = new JsonObject().put("_id", ecasId);
    object.put("_id", ecasId);
    object.put("date_modified", Instant.now());

    UpdateOptions options = new UpdateOptions().setUpsert(true);

    return mongoClient.replaceDocumentsWithOptions(collection, query, object, options)
      .mapEmpty();
  }


  public void getUser(@NotNull RoutingContext context) {

    String ecasId = context.user().get("sub");

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
   * Delete user info and all associate entities of him base of userId
   */
  public void deleteUser(@NotNull RoutingContext context) {

    // Get the ecasId and userId from the context
    String userId = context.user().get("sub");
    JsonObject queryUserDocument = new JsonObject().put("_id", userId);
    // Query to find documents with the given userId
    JsonObject queryUserId = new JsonObject().put("userId", userId);

    // Find all access and request applications for this user
    Future<List<JsonObject>> accessApplicationsQuery = mongoClient.find(DATA_ACCESS_APPLICATION_COLLECTION, queryUserId);
    Future<List<JsonObject>> requestApplicationQuery = mongoClient.find(DATA_REQUEST_APPLICATION_COLLECTION, queryUserId);

    CompositeFuture combined = Future.join(accessApplicationsQuery, requestApplicationQuery);
    combined
      .onComplete(result -> {
        if (result.succeeded()) {
          // Retrieve results
          List<JsonObject> accessApplicationResults = accessApplicationsQuery.result();
          List<JsonObject> requestApplicationResults = requestApplicationQuery.result();

          if(accessApplicationResults.isEmpty() && requestApplicationResults.isEmpty()) {
            // No applications found for this user, proceed to delete the user
            deleteUserEntity(context, queryUserDocument);
          } else {
            //Prepare an object of 2 arrays (access/request applications) for sending them to dispatcher
            List<String> accessIds = accessApplicationResults.stream()
              .map(app -> app.getString("_id")).toList();
            List<String> requestIds = requestApplicationResults.stream()
              .map(app -> app.getString("_id")).toList();
            JsonObject applications = new JsonObject();
            if(!accessIds.isEmpty()) {
              applications.put("accessApplications", new JsonArray(accessIds));
            }
            if(!requestIds.isEmpty()) {
              applications.put("requestApplications", new JsonArray(requestIds));
            }

            // Delete applications on Dispatcher
            dispatcherClient.deleteApplicationOnDispatcher(applications).onSuccess(dispatcherClientResult -> {

              // Delete documents from DISPATCHER collection based on userId
              JsonObject dispatcherDeleteQuery = new JsonObject()
                .put("application.userId", userId);

              dbHandler.deleteAll(DISPATCHER_COLLECTION, dispatcherDeleteQuery)
                .onSuccess(dispatcherDeleteResult -> {

                  // Delete documents from DATA_ACCESS_APPLICATION_COLLECTION and DATA_REQUEST_APPLICATION
                  Future<Integer> accessApplicationsDeleteQuery = dbHandler.deleteAll(DATA_ACCESS_APPLICATION_COLLECTION, queryUserId);
                  Future<Integer> requestApplicationsDeleteQuery = dbHandler.deleteAll(DATA_REQUEST_APPLICATION_COLLECTION, queryUserId);

                  CompositeFuture deleteQuery = Future.join(accessApplicationsDeleteQuery, requestApplicationsDeleteQuery);
                  deleteQuery
                    .onComplete(deleteQueryResult -> {
                      if (result.succeeded()) {

                        // Delete the user
                        deleteUserEntity(context, queryUserDocument);

                      }}).onFailure(cause -> setUpMessageContextResponse(context,
                      HttpResponseStatus.INTERNAL_SERVER_ERROR,
                      "Failed to delete applications"));
                }).onFailure(
                  cause -> setUpMessageContextResponse(context,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete dispatcher records"));
            }).onFailure(
                cause -> setUpMessageContextResponse(context,
                  HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Failed to delete dispatcher records"));
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Error while retrieving applications from the database:, " + result.cause().getMessage());
        }
      });
  }

  private void deleteUserEntity(@NotNull RoutingContext context, JsonObject object) {
    dbHandler.deleteByField(USER_COLLECTION, object)
      .onSuccess(jsonObject -> {
        //add auditlog
        auditLog(context, USER_DELETED);
        setUpContextResponse(context, HttpResponseStatus.NO_CONTENT);
      }).onFailure(cause -> setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
        "User not found"));
  }

  private void auditLog(RoutingContext context, String action) {
    JsonObject auditLog = new JsonObject()
      .put("userId", getUserId(context))
      .put("action", action);
    dbHandler.create(APPLICATION_AUDIT_LOG, auditLog);
  }

}
