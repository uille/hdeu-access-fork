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

import static eu.europa.data.health.access.Constants.BASKET_COLLECTION;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.USER_ID;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that implements all the CRUD operations for Basket.
 */
public class BasketManager extends AbstractManager {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final MongoClient mongoClient;

  public BasketManager(DBHandler dbHandler, MongoClient mongoClient) {
    super(dbHandler);
    this.mongoClient = mongoClient;
  }

  /**
   * Operation to create or update the content of a Basket Dataset
   *
   * @param context Contains the request body for the Basket Dataset
   */
  public Future<String> createOrUpdateBasketDataset(@NotNull RoutingContext context) {

    JsonObject body = context.body().asJsonObject();
    if (!validateBasketDataset(body)) {
      return Future.failedFuture("Invalid input data");
    }

    // Retrieve the dataset id
    JsonObject datasetIdToSearch = new JsonObject().put(DATASET_ID,
      context.body().asJsonObject().getValue(DATASET_ID));
    datasetIdToSearch.put(USER_ID, Context.getEcasId(context));

    // Check if the provided dataset already exists as a document in the Mongo db
    Promise<String> promise = Promise.promise();
    mongoClient.findOne(BASKET_COLLECTION, datasetIdToSearch, null, asyncResult -> {
      //Dataset exists in Mongo db
      if (asyncResult.succeeded()) {
        //Retrieve the stored dataset
        JsonObject dataset = asyncResult.result();

        if (dataset != null) {
          // Create the new json object for the distribution to be added
          JsonObject distributionToBeAdded = new JsonObject().put(DISTRIBUTION_ID,
            context.body().asJsonObject().getString(DISTRIBUTION_ID));
          // Update the dataset
          if (dataset.getJsonArray(DISTRIBUTIONS).contains(distributionToBeAdded)) {
            // Provided distribution already exists
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Provided distribution already exists");
          } else {
            dataset.put("publisher", context.get("publisher"));
            // Add the new distribution
            dataset.getJsonArray(DISTRIBUTIONS).add(distributionToBeAdded);
            // Update the object in Mongo db
            mongoClient.replaceDocuments(BASKET_COLLECTION, datasetIdToSearch, dataset,
              updateResult -> {
                if (updateResult.succeeded()) {
                  // Object updated successfully
                  setUpMessageContextResponse(context, HttpResponseStatus.CREATED,
                    "Document updated successfully");
                } else {
                  // Failed to update object
                  setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update document");
                }
              });
          }
          promise.complete("Operation successful");
        } else {
          createBasketDataset(context);
          super.createEntity(context, BASKET_COLLECTION);
          promise.complete("Basket dataset created");
        }
        // Dataset does not exist in the Mongo db, will create it
      } else {
        log.error("Failed to find dataset: " + asyncResult.cause().getMessage(),
          asyncResult.cause());
        promise.fail("Operation failed");
      }
    });

    return promise.future();
  }

  /**
   * Retrieve a Basket Dataset by Dataset id
   *
   * @param context Contains the path param with the Dataset id to retrieve
   */
  public void getBasketDatasetByDatasetId(@NotNull RoutingContext context) {
    super.findByPathParam(context, BASKET_COLLECTION, DATASET_ID);
  }

  /**
   * Retrieve all Basket Datasets
   *
   * @param context Contains the RoutingContext
   */
  public void getAllBasketDatasets(@NotNull RoutingContext context) {

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

    super.getEntity(context, BASKET_COLLECTION, pageSize, pageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : "date_added",
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc", null);
  }

  /**
   * Retrieve the distribution count contained in all datasets
   *
   * @param context Contains the RoutingContext
   */
  public void getDistributionCount(@NotNull RoutingContext context) {

    Promise<List<JsonObject>> promise = Promise.promise();
    FindOptions options = new FindOptions();
    JsonObject query = new JsonObject().put(USER_ID, Context.getEcasId(context));
    mongoClient.findWithOptions(BASKET_COLLECTION, query, options, listAsyncResult -> {
      if (listAsyncResult.succeeded()) {
        JsonObject response = new JsonObject().put("distribution_count",
          countDistributions(listAsyncResult.result()));
        promise.complete(listAsyncResult.result());
        setUpContextResponse(context, HttpResponseStatus.OK,
          response.encode());
      } else {
        promise.fail(listAsyncResult.cause());
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to retrieve the distribution counter");
      }
    });

    promise.future();
  }

  /**
   * Delete a Basket Dataset by providing the Dataset id
   *
   * @param context Contains the path param with the Dataset id to delete
   */
  public void deleteBasketDatasetByDatasetId(@NotNull RoutingContext context) {
    super.deleteByPathParam(context, BASKET_COLLECTION, DATASET_ID);
  }

  /**
   * Delete all Basket Datasets contained in the Mongo db
   *
   * @param context Contains the RoutingContext
   */
  public void deleteAllBasketDatasets(@NotNull RoutingContext context) {
    super.deleteAllEntities(context, BASKET_COLLECTION);
  }

  /**
   * Deletes a Distribution from an existing Basket Dataset. If the provided distribution is the
   * last one existing in the Basket Dataset, then the Dataset gets deleted also.
   *
   * @param context Contains the request body for the Distribution to delete
   */
  public Future<String> deleteDistributionByDistributionId(@NotNull RoutingContext context) {

    // Retrieve the dataset id
    JsonObject datasetIdToSearch = new JsonObject().put(DATASET_ID,
      context.body().asJsonObject().getValue(DATASET_ID));
    datasetIdToSearch.put(USER_ID, Context.getEcasId(context));

    // Check if the provided dataset already exists as a document in the Mongo db
    Promise<String> promise = Promise.promise();
    mongoClient.findOne(BASKET_COLLECTION, datasetIdToSearch, null, searchResult -> {
      //Dataset exists in Mongo db
      if (searchResult.result() != null) {
        //Retrieve the stored dataset
        JsonObject dataset = searchResult.result();
        // Create the new json object for the distribution to be removed
        JsonObject distributionToBeRemoved = new JsonObject().put(DISTRIBUTION_ID,
          context.body().asJsonObject().getString(DISTRIBUTION_ID));
        // Retrieve all distributions
        JsonArray distributions = dataset.getJsonArray(DISTRIBUTIONS);
        // Check if found dataset from Mongo db contains the provided distribution
        if (distributions.contains(distributionToBeRemoved)) {

          //Remove the provided distribution from the dataset
          distributions.remove(distributionToBeRemoved);

          // Check if dataset does not contain any other distributions. In case it does, delete the dataset
          if (distributions.isEmpty()) {
            mongoClient.findOneAndDelete(BASKET_COLLECTION, datasetIdToSearch,
              asyncResult -> {
                if (asyncResult.succeeded()) {
                  // Dataset deleted successfully
                  setUpMessageContextResponse(context, HttpResponseStatus.OK,
                    "Dataset deleted successfully");
                } else {
                  // Failed to delete dataset
                  setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete dataset");
                }
              });

          } else {
            // Remove the distribution from the dataset
            mongoClient.replaceDocuments(BASKET_COLLECTION, datasetIdToSearch, dataset,
              updateResult -> {
                if (updateResult.succeeded()) {
                  // Dataset updated successfully
                  setUpMessageContextResponse(context, HttpResponseStatus.OK,
                    "Distribution deleted successfully");
                } else {
                  // Failed to update dataset
                  setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete distribution");
                }
              });
          }

          // Provided distribution is not included in the dataset
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            "Dataset does not contain the provided distribution");
        }

        //Dataset does not exist in the Mongo db
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "Provided dataset was not found");
      }
    });
    return promise.future();
  }

  /**
   * Delete distributions by distribution id list
   *
   * @param datasetId          The dataset id
   * @param distributionIdList The distribution id list
   */
  void deleteDistributionsByDistributionIdList(JsonObject datasetId,
    ArrayList<JsonObject> distributionIdList) {
    // Check if the provided dataset already exists as a document in the Mongo db
    mongoClient.findOne(BASKET_COLLECTION, datasetId, null, searchResult -> {
      // Dataset exists in Mongo db
      if (searchResult.result() != null) {
        //Retrieve the stored dataset
        JsonObject dataset = searchResult.result();

        // Retrieve all distributions
        JsonArray distributions = dataset.getJsonArray(DISTRIBUTIONS);

        //Remove the provided distribution from the dataset
        for (JsonObject id : distributionIdList) {
          distributions.remove(id);
        }

        // Check if dataset does not contain any other distributions. In case it does, delete the dataset
        if (distributions.isEmpty()) {
          mongoClient.findOneAndDelete(BASKET_COLLECTION, datasetId);
        } else {
          // Remove the distribution from the dataset
          mongoClient.replaceDocuments(BASKET_COLLECTION, datasetId, dataset);
        }
      }
    });
  }

  private boolean validateBasketDataset(JsonObject body) {
    return body.containsKey(DATASET_ID) && body.containsKey(DISTRIBUTION_ID) &&
      body.getString(DATASET_ID) != null && body.getString(DISTRIBUTION_ID) != null;
  }

  /**
   * Creates a new Basket Dataset and stores it in the RoutingContext
   *
   * @param context Contains the request body for the Basket Dataset.
   */
  private void createBasketDataset(RoutingContext context) {

    // Retrieve the request body
    JsonObject requestBody = context.body().asJsonObject();

    requestBody.put("publisher", context.get("publisher"));

    // Define the fields to be stored for a Basket Dataset
    JsonObject distribution = new JsonObject().put(DISTRIBUTION_ID,
      requestBody.getString(DISTRIBUTION_ID));
    JsonArray distributions = new JsonArray();
    distributions.add(distribution);

    requestBody.remove(DISTRIBUTION_ID);
    requestBody.put(USER_ID, Context.getEcasId(context));
    requestBody.put("date_added", Instant.now());
    requestBody.put(DISTRIBUTIONS, distributions);
  }

  private Integer countDistributions(List<JsonObject> datasetList) {

    Integer distributionCounter = 0;
    for (JsonObject dataset : datasetList) {
      distributionCounter += dataset.getJsonArray(DISTRIBUTIONS).size();
    }
    return distributionCounter;
  }
}
