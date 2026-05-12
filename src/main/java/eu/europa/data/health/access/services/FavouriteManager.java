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

import static eu.europa.data.health.access.Constants.CONTENT;
import static eu.europa.data.health.access.Constants.CREATED_AT;
import static eu.europa.data.health.access.Constants.FAVOURITES_COLLECTION;
import static eu.europa.data.health.access.Constants.OBJECT_ID;
import static eu.europa.data.health.access.Constants.PARENT_ID;
import static eu.europa.data.health.access.Constants.TYPE;
import static eu.europa.data.health.access.Constants.UNAUTHORIZED;
import static eu.europa.data.health.access.Constants.UPDATED_AT;
import static eu.europa.data.health.access.Constants.USER_ID;

import eu.europa.data.health.access.Context;
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
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.CompositeFuture;
import java.util.ArrayList;
import java.util.List;


/**
 * Class that implements all the CRUD operations for Favourites.
 */
public class FavouriteManager extends AbstractManager {

  private static final String DUPLICATE = "DUPLICATE";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;
  private final HubSearchClient hubSearchClient;

  public FavouriteManager(DBHandler dbHandler, MongoClient mongoClient, HubSearchClient hubSearchClient) {
    super(dbHandler);
    this.mongoClient = mongoClient;
    this.hubSearchClient = hubSearchClient;
  }

  /**
   * Retrieve all favourites with optional filtering by type.
   *
   * @param context The routing context with optional query parameters
   */
  public void getFavourites(@NotNull RoutingContext context) {
    log.debug("Entered getFavourites method");

    // Resolve current user ID from the security context
    final String userId = Context.getEcasId(context);
    if (StringUtils.isBlank(userId)) {
      log.warn("Unauthorized access attempt to getFavourites");
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, UNAUTHORIZED);
      return;
    }

    // Build the query filters
    JsonObject query = new JsonObject().put(USER_ID, userId);

    // Add type filter if provided
    final String type = context.request().getParam(TYPE);
    if (StringUtils.isNotEmpty(type)) {
      query.put(TYPE, type);
    }

    // Fetch favourites from the database
    mongoClient
      .find(FAVOURITES_COLLECTION, query)
      .onSuccess(favourites -> {
        JsonArray response = new JsonArray(favourites);
        setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
      })
      .onFailure(cause -> {
        log.error("Failed to retrieve favourites:", cause);
        setUpMessageContextResponse(
          context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to retrieve favourites"
        );
      });

    log.debug("Exiting getFavourites method");
  }

  /**
   * Create a new favourite entry.
   *
   * @param context The routing context with the request body.
   */
  public void createFavourite(@NotNull RoutingContext context) {
    log.debug("Entered createFavourite method");

    // Resolve current user ID from the security context
    final String userId = Context.getEcasId(context);
    if (StringUtils.isBlank(userId)) {
      log.warn("Unauthorized access attempt to createFavourite");
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, UNAUTHORIZED);
      return;
    }

    JsonObject requestBody = context.body().asJsonObject();
    String type = requestBody.getString(TYPE);
    boolean isDataset = "dataset".equals(type);
    boolean isDistribution = "distribution".equals(type);
    boolean isSparqlQuery = "sparqlQuery".equals(type);
    boolean isDatasetFilter = "datasetFilter".equals(type);
    String timestamp = Instant.now().toString();

    // Prepare the favourite document to be saved and the uniqueness query
    JsonObject favourite = new JsonObject()
      .put(USER_ID, userId)
      .put(TYPE, type)
      .put(CREATED_AT, timestamp)
      .put(UPDATED_AT, timestamp);
    JsonObject query = new JsonObject().put(USER_ID, userId).put(TYPE, type);
    JsonObject projection = new JsonObject().put("_id", 1);

    // Add objectId if type is dataset or distribution
    if (isDataset || isDistribution) {
      favourite.put(OBJECT_ID, requestBody.getString(OBJECT_ID));
      query.put(OBJECT_ID, requestBody.getString(OBJECT_ID));
    }

    // Add parentId if type is distribution
    if (isDistribution) {
      favourite.put(PARENT_ID, requestBody.getString(PARENT_ID));
      query.put(PARENT_ID, requestBody.getString(PARENT_ID));
    }

    // Persist content for all types that may carry snapshot/display metadata.
    // For dataset/distribution favourites this is optional and is used by the UI
    // to show a readable warning when the real entry later disappears.
    JsonObject content = requestBody.getJsonObject(CONTENT);
    if (content != null && !content.isEmpty()) {
      favourite.put(CONTENT, content);
    }

    // Only SPARQL query and dataset-filter favourites use content.label for uniqueness.
    if (isSparqlQuery || isDatasetFilter) {
      JsonObject contentForUniqueness = requestBody.getJsonObject(CONTENT, new JsonObject());
      query.put("content.label", contentForUniqueness.getString("label"));
    }

    // Check uniqueness, then save the favourite to the database and respond with the created document
    mongoClient
      .findOne(FAVOURITES_COLLECTION, query, projection)
      .compose(existing -> {
        if (existing != null) {
          return Future.failedFuture(DUPLICATE);
        }
        return mongoClient.save(FAVOURITES_COLLECTION, favourite);
      })
      .onSuccess(id -> {
        favourite.put("_id", id);
        setUpContextResponse(context, HttpResponseStatus.CREATED, favourite.encode());
      })
      .onFailure(cause -> {
        if (DUPLICATE.equals(cause.getMessage())) {
          log.warn("Duplicate favourite attempt: userId={}, type={}", userId, type);
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.CONFLICT,
            "Favourite already exists"
          );
        } else {
          log.error("Failed to create favourite:", cause);
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Failed to create favourite"
          );
        }
      });

    log.debug("Exiting createFavourite method");
  }
  /**
   * Retrieve stale favourites for the authenticated user.
   * A favourite is considered stale when it still exists in the favourites
   * collection but the underlying dataset/distribution can no longer be
   * resolved in the current system data.
   * Supported types:
   *   - dataset
   *   - distribution
   */
  public void getStaleFavourites(@NotNull RoutingContext context) {
    log.debug("Entered getStaleFavourites method");

    final String userId = Context.getEcasId(context);
    if (StringUtils.isBlank(userId)) {
      log.warn("Unauthorized access attempt to getStaleFavourites");
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, UNAUTHORIZED);
      return;
    }

    final String type = context.request().getParam(TYPE);
    final boolean isDataset = "dataset".equals(type);
    final boolean isDistribution = "distribution".equals(type);

    if (!isDataset && !isDistribution) {
      setUpMessageContextResponse(
        context,
        HttpResponseStatus.BAD_REQUEST,
        "type must be either 'dataset' or 'distribution'"
      );
      return;
    }

    JsonObject query = new JsonObject()
      .put(USER_ID, userId)
      .put(TYPE, type);

    mongoClient.find(FAVOURITES_COLLECTION, query)
      .compose(favourites -> {
        List<Future> checks = new ArrayList<>();

        for (JsonObject favourite : favourites) {
          checks.add(isDataset
            ? resolveStaleDatasetFavourite(favourite)
            : resolveStaleDistributionFavourite(favourite));
        }

        return CompositeFuture.all(checks).map(composite -> {
          JsonArray stale = new JsonArray();

          for (int i = 0; i < checks.size(); i++) {
            JsonObject staleFavourite = composite.resultAt(i);
            if (staleFavourite != null) {
              stale.add(staleFavourite);
            }
          }

          return stale;
        });
      })
      .onSuccess(stale -> setUpContextResponse(context, HttpResponseStatus.OK, stale.encode()))
      .onFailure(cause -> {
        log.error("Failed to retrieve stale favourites:", cause);
        setUpMessageContextResponse(
          context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to retrieve stale favourites"
        );
      });

    log.debug("Exiting getStaleFavourites method");
  }

  /**
   * Dataset favourite is stale when the dataset can no longer be fetched from hub-search.
   */
  private Future<JsonObject> resolveStaleDatasetFavourite(JsonObject favourite) {
    String datasetId = favourite.getString(OBJECT_ID);

    return hubSearchClient.getDataset(datasetId)
      .map(response -> response.containsKey("result") ? null : favourite);
  }

  /**
   * Distribution favourite is stale when:
   *   1. its parent dataset no longer exists, or
   *   2. the parent dataset exists but no longer contains that distribution id.
   */
  private Future<JsonObject> resolveStaleDistributionFavourite(JsonObject favourite) {
    String datasetId = favourite.getString(PARENT_ID);
    String distributionId = favourite.getString(OBJECT_ID);

    return hubSearchClient.getDataset(datasetId)
      .map(response -> {
        if (!response.containsKey("result")) {
          return favourite;
        }

        JsonObject result = response.getJsonObject("result", new JsonObject());
        JsonArray distributions = result.getJsonArray("distributions", new JsonArray());

        for (int i = 0; i < distributions.size(); i++) {
          JsonObject distribution = distributions.getJsonObject(i);
          if (distribution != null && distributionId.equals(distribution.getString("id"))) {
            return null;
          }
        }

        return favourite;
      });
  }
  /**
   * Update an existing favourite entry by id (only for sparqlQuery and datasetFilter types).
   *
   * @param context The routing context with the favourite id in the path parameter and the request
   *                body.
   */
  public void updateFavourite(@NotNull RoutingContext context) {
    log.debug("Entered updateFavourite method");

    // Validate that the ID is a valid ObjectId
    final String id = context.pathParam("id");
    if (!ObjectId.isValid(id)) {
      log.warn("Invalid id format: {}", id);
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Invalid id format");
      return;
    }

    // Resolve current user ID from the security context
    final String userId = Context.getEcasId(context);
    if (StringUtils.isBlank(userId)) {
      log.warn("Unauthorized access attempt to updateFavourite");
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, UNAUTHORIZED);
      return;
    }

    JsonObject requestBody = context.body().asJsonObject();
    JsonObject content = requestBody.getJsonObject(CONTENT, new JsonObject());
    String type = requestBody.getString(TYPE);

    // Build uniqueness query to check if another favourite with the same type and content label
    // exists for the same user, excluding the current favourite being updated
    JsonObject query = new JsonObject()
      .put(USER_ID, userId)
      .put(TYPE, type)
      .put("content.label", content.getString("label"))
      .put("_id", new JsonObject().put("$ne", id));
    JsonObject projection = new JsonObject().put("_id", 1);

    // Check uniqueness, then update the favourite in the database
    mongoClient
      .findOne(FAVOURITES_COLLECTION, query, projection)
      .compose(existing -> {
        if (existing != null) {
          return Future.failedFuture(DUPLICATE);
        }

        // Prepare the update document with $set operator
        JsonObject updateFields = new JsonObject()
          .put(UPDATED_AT, Instant.now().toString())
          .put(CONTENT, content);
        JsonObject update = new JsonObject().put("$set", updateFields);

        // Build the query to ensure that only the owner can update their favourite
        JsonObject updateQuery = new JsonObject().put("_id", id).put(USER_ID, userId);

        // Configure options to return the updated document
        FindOptions findOptions = new FindOptions();
        UpdateOptions updateOptions = new UpdateOptions().setReturningNewDocument(true);

        return mongoClient.findOneAndUpdateWithOptions(
          FAVOURITES_COLLECTION,
          updateQuery,
          update,
          findOptions,
          updateOptions
        );
      })
      .onSuccess(result -> {
        if (result == null) {
          log.warn("Favourite not found to be updated: id={}", id);
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND, "Favourite not found");
        } else {
          log.debug("Favourite updated successfully: id={}", id);
          setUpContextResponse(context, HttpResponseStatus.OK, result.encode());
        }
      })
      .onFailure(cause -> {
        if (DUPLICATE.equals(cause.getMessage())) {
          log.warn("Duplicate favourite attempt on update: userId={}, type={}", userId, type);
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.CONFLICT,
            "Favourite already exists"
          );
        } else {
          log.error("Failed to update favourite:", cause);
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Failed to update favourite"
          );
        }
      });

    log.debug("Exiting updateFavourite method");
  }

  /**
   * Delete a favourite entry by id and userId.
   *
   * @param context The routing context with the favourite id in the path parameter.
   */
  public void deleteFavourite(@NotNull RoutingContext context) {
    log.debug("Entered deleteFavourite method");

    // Validate that the ID is a valid ObjectId
    final String id = context.pathParam("id");
    if (!ObjectId.isValid(id)) {
      log.warn("Invalid id format: {}", id);
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Invalid id format");
      return;
    }

    // Resolve current user ID from the security context
    final String userId = Context.getEcasId(context);
    if (StringUtils.isBlank(userId)) {
      log.warn("Unauthorized access attempt to deleteFavourite");
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, UNAUTHORIZED);
      return;
    }

    // Build the query to ensure that only the owner can delete their favourite
    JsonObject query = new JsonObject().put("_id", id).put(USER_ID, userId);

    // Delete the favourite from the database
    mongoClient
      .removeDocument(FAVOURITES_COLLECTION, query)
      .onSuccess(result -> {
        if (result.getRemovedCount() == 0) {
          log.warn("Favourite not found to be deleted: id={}", id);
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND, "Favourite not found");
        } else {
          log.debug("Favourite deleted successfully: id={}", id);
          setUpContextResponse(context, HttpResponseStatus.NO_CONTENT, "");
        }
      })
      .onFailure(cause -> {
        log.error("Failed to delete favourite:", cause);
        setUpMessageContextResponse(
          context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to delete favourite"
        );
      });

    log.debug("Exiting deleteFavourite method");
  }
}
