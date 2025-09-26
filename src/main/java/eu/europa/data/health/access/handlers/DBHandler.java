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
package eu.europa.data.health.access.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.serviceproxy.ServiceException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that provide access to the mongoDb for specific operations.
 */
public class DBHandler {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;
  private final String dbHandlerClassName = this.getClass().getSimpleName() + " - ";

  private static final String ENTERED_DELETE_BY_FIELD_MESSAGE = "DBHandler Entered deleteByField method";
  private static final String EXIT_DELETE_BY_FIELD_MESSAGE = "DBHandler Exiting deleteByField method";
  private static final String ENTERED_DELETE_BY_ID_MESSAGE = "DBHandler Entered deleteById method";
  private static final String EXIT_DELETE_BY_ID_MESSAGE = "DBHandler Exiting deleteById method";
  private static final String ENTERED_DELETE_ALL_MESSAGE = "DBHandler Entered deleteAll method";
  private static final String EXIT_DELETE_ALL_MESSAGE = "DBHandler Exiting deleteAll method";

  public DBHandler(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  /**
   * Creates a document for the provided collection
   *
   * @param collectionName the collection
   * @param jsonObject     the object to create
   */
  public Future<String> create(String collectionName, JsonObject jsonObject) {
    log.debug("{} Entered save method", dbHandlerClassName);

    Promise<String> promise = Promise.promise();
    mongoClient.save(collectionName, jsonObject, asyncResult -> {
      if (asyncResult.succeeded()) {
        log.debug("{} Saved successfully: {}", dbHandlerClassName, asyncResult.result());
        promise.complete(asyncResult.result());
      } else {
        log.debug("{} Failed to save: {}", dbHandlerClassName, asyncResult.cause().toString());
        promise.fail(asyncResult.cause());
      }
    });
    log.debug("{} Exiting save method", dbHandlerClassName);

    return promise.future();
  }

  /**
   * Update Document for Specific Collection
   *
   * @param collectionName the collection
   * @param updateQuery    the query with the fields that they will going to update
   * @param query          the id of the document to be updated and the user id (On json can you
   *                       send as field with name id, we change it to _id)
   */
  public Future<JsonObject> update(String collectionName, JsonObject updateQuery,
    JsonObject query) {
    log.info("{} Entered save method", dbHandlerClassName);
    Promise<JsonObject> promise = Promise.promise();
    //Remove the "id" field from the request body to prevent create a new field with it
    //wee need keep only the _id of mongoDb
    updateQuery.remove("id");
    mongoClient.updateCollection(collectionName, query, new JsonObject().put("$set", updateQuery)
      , asyncResult -> {
        if (asyncResult.succeeded()) {
          log.info("{} Saved successfully: {}", dbHandlerClassName, asyncResult.result());
          promise.complete(JsonObject.mapFrom(asyncResult.result()));
        } else {
          log.info("{} Failed to save: {}", dbHandlerClassName, asyncResult.cause().toString());
          promise.fail(asyncResult.cause());
        }
      });
    log.info("{} Exiting save method", dbHandlerClassName);

    return promise.future();
  }

  /**
   * Find a Document by field for the provided collection.
   *
   * @param collectionName the collection
   * @param jsonObject     the object to retrieve
   */
  public Future<JsonObject> findByField(String collectionName, JsonObject jsonObject) {
    log.debug("{} Entered findByField method", dbHandlerClassName);

    Promise<JsonObject> promise = Promise.promise();
    mongoClient.findOne(collectionName, jsonObject, null, asyncResult -> {
      if (asyncResult.succeeded()) {
        if (asyncResult.result() != null) {
          logAndPromiseSuccess(promise, asyncResult,
            dbHandlerClassName + " JsonObject retrieved successfully");
        } else {
          logAndPromiseFailWithNotFound(promise,
            dbHandlerClassName + " Retrieved JsonObject is empty");
        }
      } else {
        logAndPromiseFail(promise, asyncResult,
          dbHandlerClassName + " Failed to retrieve JsonObject");
      }
    });
    log.debug("{} Exiting findByField method", dbHandlerClassName);

    return promise.future();
  }

  /**
   * Delete a Document by field for the provided collection.
   *
   * @param collectionName the collection
   * @param jsonObject     the object to delete
   */
  public Future<JsonObject> deleteByField(String collectionName, JsonObject jsonObject) {
    log.debug("{}", ENTERED_DELETE_BY_FIELD_MESSAGE);

    Promise<JsonObject> promise = Promise.promise();
    mongoClient.findOneAndDelete(collectionName, jsonObject, asyncResult -> {
      if (asyncResult.succeeded()) {
        if (asyncResult.result() != null) {
          logAndPromiseSuccess(promise, asyncResult,
            dbHandlerClassName + " JsonObject deleted successfully");
        } else {
          logAndPromiseFailWithNotFound(promise, dbHandlerClassName + " JsonObject does not exist");
        }
      } else {
        logAndPromiseFail(promise, asyncResult,
          dbHandlerClassName + " Failed to delete JsonObject");
      }
    });
    log.debug("{}", EXIT_DELETE_BY_FIELD_MESSAGE);

    return promise.future();
  }

  private void logAndPromiseFail(Promise<JsonObject> promise, AsyncResult<JsonObject> result,
    String message) {
    if (log.isDebugEnabled()) {
      log.debug(message);
    }
    promise.fail(result.cause());
  }

  private void logAndPromiseFailWithNotFound(Promise<JsonObject> promise, String message) {
    if (log.isDebugEnabled()) {
      log.debug(message);
    }
    promise.fail(new ServiceException(404, HttpResponseStatus.NOT_FOUND.reasonPhrase()));
  }

  private void logAndPromiseSuccess(Promise<JsonObject> promise, AsyncResult<JsonObject> result,
    String message) {
    if (log.isDebugEnabled()) {
      log.debug(message);
    }
    promise.complete(result.result());
  }

  /**
   * Find a Document by id for the provided collection.
   *
   * @param collectionName the collection
   * @param id             the id to retrieve
   */
  public Future<JsonObject> findById(String collectionName, String id) {
    log.debug("Entered findByDatabaseId method of DBHandler");
    JsonObject query = new JsonObject().put("_id", id);
    return findByField(collectionName, query);
  }

  public Future<List<JsonObject>> findAll(String collectionName, Integer pageNumber,
    Integer pageLimit, String sortBy, String sortOrder, String status) {
    return findAll(collectionName, pageNumber, pageLimit, sortBy, sortOrder, status, null);
  }

  /**
   * Find all Documents for the provided collection.
   *
   * @param collectionName the collection
   */
  public Future<List<JsonObject>> findAll(String collectionName, Integer pageNumber,
    Integer pageLimit, String sortBy, String sortOrder, String status,
    String userId) {
    log.debug("{} Entered findAll method", dbHandlerClassName);

    Promise<List<JsonObject>> promise = Promise.promise();

    //Define pagination configuration, if provided
    FindOptions options = new FindOptions();
    if (pageNumber != null && pageLimit != null) {
      options.setSkip((pageNumber - 1) * pageLimit).setLimit(pageLimit);
    }

    // Define sort options
    if (sortBy != null && sortOrder != null) {
      options.setSort(new JsonObject().put(sortBy, sortOrder.equalsIgnoreCase("asc") ? 1 : -1));
    }

    // Define filter options
    JsonObject filter = new JsonObject();

    // Apply status filter
    JsonObject statusFilter = createStatusFilter(status);
    if (!statusFilter.isEmpty()) {
      filter.put("status.status", statusFilter);
    }
    if (StringUtils.isNotEmpty(userId)) {
      filter.put("userId", userId);
    }
    mongoClient.findWithOptions(collectionName, filter, options, listAsyncResult -> {
      if (listAsyncResult.succeeded()) {
        log.debug("{} Found documents: {}", dbHandlerClassName, listAsyncResult.result());
        promise.complete(listAsyncResult.result());
      } else {
        log.debug("{} Failed to find documents by catalog name: {}", dbHandlerClassName,
          listAsyncResult.cause().toString());
        promise.fail(listAsyncResult.cause());
      }
    });
    log.debug("{} Exiting findAll method", dbHandlerClassName);

    return promise.future();
  }

  public Future<List<JsonObject>> findAuditLogs(String collectionName, Integer pageNumber,
    Integer pageLimit, String sortBy, String sortOrder, JsonObject queryOptions) {
    log.debug("{} Entered findAuditLogs method", dbHandlerClassName);

    Promise<List<JsonObject>> promise = Promise.promise();

    //Define pagination configuration, if provided
    FindOptions options = new FindOptions();
    if (pageNumber != null && pageLimit != null) {
      options.setSkip((pageNumber - 1) * pageLimit).setLimit(pageLimit);
    }

    // Define sort options
    if (sortBy != null && sortOrder != null) {
      options.setSort(new JsonObject().put(sortBy, sortOrder.equalsIgnoreCase("asc") ? 1 : -1));
    }

    mongoClient.findWithOptions(collectionName, queryOptions, options, listAsyncResult -> {
      if (listAsyncResult.succeeded()) {
        log.debug("{} Found documents: {}", dbHandlerClassName, listAsyncResult.result());
        promise.complete(listAsyncResult.result());
      } else {
        log.debug("{} Failed to find documents by catalog name: {}", dbHandlerClassName,
          listAsyncResult.cause().toString());
        promise.fail(listAsyncResult.cause());
      }
    });
    log.debug("{} Exiting findAuditLogs method", dbHandlerClassName);

    return promise.future();
  }

  /**
   * Delete a Document by field for the provided collection.
   *
   * @param collectionName the collection
   */
  public Future<Integer> deleteById(String collectionName, JsonObject jsonObject) {

    log.debug("{}", ENTERED_DELETE_BY_ID_MESSAGE);
    Promise<Integer> promise = Promise.promise();

    mongoClient.removeDocument(collectionName, jsonObject, asyncResult -> {
      if (asyncResult.succeeded()) {
        log.debug("{} Deletion was successful", dbHandlerClassName);
        promise.complete(asyncResult.result().getRemovedCount() == 1 ? 204 : 404);
      } else {
        log.debug("{} Failed to delete", dbHandlerClassName);
        promise.fail(asyncResult.cause());
      }
    });

    log.debug("{}", EXIT_DELETE_BY_ID_MESSAGE);
    return promise.future();
  }

  /**
   * Delete all Documents for the provided collection.
   *
   * @param collectionName the collection
   */
  public Future<Integer> deleteAll(String collectionName, JsonObject jsonObject) {

    log.debug("{}", ENTERED_DELETE_ALL_MESSAGE);
    Promise<Integer> promise = Promise.promise();

    mongoClient.removeDocuments(collectionName, jsonObject, asyncResult -> {
      if (asyncResult.succeeded()) {
        log.debug("{} Deletion was successful", dbHandlerClassName);
        promise.complete(asyncResult.result().getRemovedCount() >= 1 ? 204 : 404);
      } else {
        log.debug("{} Failed to delete", dbHandlerClassName);
        promise.fail(asyncResult.cause());
      }
    });

    log.debug("{}", EXIT_DELETE_ALL_MESSAGE);
    return promise.future();
  }

  public Future<Integer> countAllDocuments(String collectionName, String status) {
    return countAllDocuments(collectionName, status, null);
  }

  /**
   * An operation to count all documents for the collection provided
   *
   * @param collectionName the collection
   */
  public Future<Integer> countAllDocuments(String collectionName, String status, String userId) {
    Promise<Integer> promise = Promise.promise();

    JsonObject filter = new JsonObject();
    // Handle multiple statuses
    // Apply status filter
    JsonObject statusFilter = createStatusFilter(status);
    if (!statusFilter.isEmpty()) {
      filter.put("status.status", statusFilter);
    }
    if (StringUtils.isNotEmpty(userId)) {
      filter.put("userId", userId);
    }
    // Use the countDocuments method to count all documents
    mongoClient.count(collectionName, filter, ar -> {
      if (ar.succeeded()) {
        Integer count = ar.result().intValue();
        promise.complete(count);
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  public Future<Integer> countAllAuditLogs(String collectionName, JsonObject queryOptions) {
    Promise<Integer> promise = Promise.promise();

    // Use the countDocuments method to count all documents
    mongoClient.count(collectionName, queryOptions, ar -> {
      if (ar.succeeded()) {
        Integer count = ar.result().intValue();
        promise.complete(count);
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  /**
   * Generic function in order to check if a document exists in a Mongo collection
   *
   * @param query      the query that will be executed in Mongo db
   * @param collection the mongo collection on which the query will be executed
   * @return a promise with 'true' if the document with the given query is found, 'false' in any
   * other case
   */
  public Promise<Boolean> checkDocumentExists(JsonObject query, String collection) {
    Promise<Boolean> promise = Promise.promise();

    mongoClient.count(collection, query, res -> {
      if (res.succeeded()) {
        long count = res.result();
        promise.complete(count > 0);
      } else {
        promise.fail(res.cause());
      }
    });

    return promise;
  }

  // Function to handle single or multiple statuses
  private JsonObject createStatusFilter(String status) {
    JsonObject statusFilter = new JsonObject();

    if (StringUtils.isNotEmpty(status)) {
      if (status.contains(",")) {
        // Multiple statuses, split by comma and use $in operator
        List<String> statusList = Arrays.asList(status.split(","));
        statusFilter.put("$in", new JsonArray(statusList));
      } else {
        // Single status, as before
        statusFilter.put("$eq", status);
      }
    }
    return statusFilter;
  }

  public Future<Object> updateLinkableFields(String collectionName, String objectId) {

    log.info("{} Entered updateLinkableFields method", dbHandlerClassName);

    JsonObject query = new JsonObject().put("objectId", objectId);
    JsonObject update = new JsonObject().put("$set", new JsonObject().put("linkable", false));

    Promise<Object> promise = Promise.promise();

    UpdateOptions options = new UpdateOptions().setMulti(true);
    mongoClient.updateCollectionWithOptions(collectionName, query, update, options, res -> {
      if (res.succeeded()) {
        log.info("{} Linkable fields updated successfully", dbHandlerClassName);
        promise.complete();
      } else {
        log.error("{} Failed to update linkable fields, with error message: {}", dbHandlerClassName,
          res.cause().getMessage());
        promise.fail(res.cause());
      }
    });

    log.info("{} Exiting updateLinkableFields method", dbHandlerClassName);
    return promise.future();
  }
}
