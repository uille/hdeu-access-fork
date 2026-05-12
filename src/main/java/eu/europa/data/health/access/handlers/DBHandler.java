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

import static org.apache.jena.http.auth.AuthEnv.LOG;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.serviceproxy.ServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.IterableUtils;
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



  private static final String CLASS_NAME = DBHandler.class.getSimpleName();
  private static final String MSG_FORMAT = "{} - {}";
  private static final String EXITING_FIND_BY_FIELD = "Exiting findByField method";
  private static final String ENTERED_FIND_BY_FIELD = "Entered findByField method";
  private static final String ENTERED_SAVE = "Entered save method";
  private static final String EXITING_SAVE = "Exiting save method";
  public static final String JSON_OBJECT_RETRIEVED_SUCCESSFULLY = "{} - JsonObject retrieved successfully";
  public static final String RETRIEVED_JSON_OBJECT_IS_EMPTY = "{} - Retrieved JsonObject is empty";
  public static final String FAILED_TO_RETRIEVE_JSON_OBJECT = "{} - Failed to retrieve JsonObject";
  public static final String SAVED_SUCCESSFULLY = "{} - Saved successfully: {}";
  public static final String FAILED_TO_SAVE = "{} - Failed to save: ";

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
    log.debug("{} Entered save method", dbHandlerClassName);
    Promise<JsonObject> promise = Promise.promise();
    //Remove the "id" field from the request body to prevent create a new field with it
    //wee need keep only the _id of mongoDb
    updateQuery.remove("id");
    mongoClient.updateCollection(collectionName, query, new JsonObject().put("$set", updateQuery)
      , asyncResult -> {
        if (asyncResult.succeeded()) {
          log.debug("{} Saved successfully: {}", dbHandlerClassName, asyncResult.result());
          promise.complete(JsonObject.mapFrom(asyncResult.result()));
        } else {
          log.error("{} Failed to save: {}", dbHandlerClassName, asyncResult.cause().toString());
          promise.fail(asyncResult.cause());
        }
      });
    log.debug("{} Exiting save method", dbHandlerClassName);

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

  public Future<List<JsonObject>> findEntities(
    String collectionName,
    Integer pageNumber,
    Integer pageLimit,
    String sortBy,
    String sortOrder,
    JsonObject queryOptions,
    JsonObject projection
  ) {
    log.debug("{} Entered findEntities method", dbHandlerClassName);

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

    // Apply projection if provided
    if (!IterableUtils.isEmpty(projection)) {
      options.setFields(projection);
    }

    mongoClient.findWithOptions(collectionName, queryOptions, options, listAsyncResult -> {
      if (listAsyncResult.succeeded()) {
        log.debug("{} Found documents: {}", dbHandlerClassName, listAsyncResult.result());
        promise.complete(listAsyncResult.result());
      } else {
        log.debug("{} Failed to find documents by queryOptions: {}", dbHandlerClassName,
          listAsyncResult.cause().toString());
        promise.fail(listAsyncResult.cause());
      }
    });
    log.debug("{} Exiting findEntities method", dbHandlerClassName);

    return promise.future();
  }

  public Future<List<JsonObject>> findDataPermitGrantedDatasetsPerApplication(
    String collectionName,
    Integer pageNumber,
    Integer pageLimit,
    String sortBy,
    String sortOrder,
    JsonObject queryOptions
  ) {
    Promise<List<JsonObject>> promise = Promise.promise();

    final int page  = (pageNumber == null || pageNumber < 1) ? 1 : pageNumber;
    final int limit = (pageLimit == null || pageLimit < 1) ? 5 : pageLimit;
    final int skip  = (page - 1) * limit;

    String effectiveSortKey =
      (sortBy == null || sortBy.isBlank() || "date_created".equalsIgnoreCase(sortBy))
        ? "date_created"
        : "date_modified";

    int sortDir = ("asc".equalsIgnoreCase(sortOrder)) ? 1 : -1;

    JsonArray pipeline = new JsonArray();

    // 1) Apply application-level filters (uses indexes)
    if (queryOptions != null && !queryOptions.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", queryOptions));
    }

    // 2) Unwind datasets (one row per dataset)
    pipeline.add(new JsonObject().put("$unwind", "$datasets"));

    // 3) Keep only datasets that have at least one GRANTED permit
    pipeline.add(new JsonObject().put("$match",
      new JsonObject().put("datasets.permits",
        new JsonObject().put("$elemMatch",
          new JsonObject().put("permit_status", "GRANTED")
        )
      )
    ));

    // 4) Project only required fields + filter ONLY granted permits
    pipeline.add(new JsonObject().put("$project", new JsonObject()
      .put("applicationId", "$_id")
      .put("applicationTitle", "$title")
      .put("applicationStatus", "$status.status")
      .put("hdab", "$datasets.hdab")
      .put("projectName", "$form.section2.projectName")
      .put("permits", new JsonObject().put("$filter", new JsonObject()
        .put("input", "$datasets.permits")
        .put("as", "p")
        .put("cond", new JsonObject().put("$eq", new JsonArray()
          .add("$$p.permit_status")
          .add("GRANTED")
        ))
      ))
      .put(effectiveSortKey, 1) // keep for sorting
    ));

    // 5) Sort + pagination
    pipeline
      .add(new JsonObject().put("$sort", new JsonObject().put(effectiveSortKey, sortDir)))
      .add(new JsonObject().put("$skip", skip))
      .add(new JsonObject().put("$limit", limit));

    // 6) Final projection (remove sort helper field)
    pipeline.add(new JsonObject().put("$project", new JsonObject()
      .put("applicationId", 1)
      .put("applicationTitle", 1)
      .put("applicationStatus", 1)
      .put("permits", 1)
      .put("hdab", 1)
      .put("projectName", 1)
    ));

    // 7) Execute aggregation
    ReadStream<JsonObject> stream = mongoClient.aggregate(collectionName, pipeline);
    List<JsonObject> items = new ArrayList<>();

    stream
      .handler(items::add)
      .exceptionHandler(promise::fail)
      .endHandler(v -> promise.complete(items));

    return promise.future();
  }

  public Future<List<JsonObject>> findDecisionIssuedDatasetsPerApplication(
    String collectionName,
    Integer pageNumber,
    Integer pageLimit,
    String sortBy,
    String sortOrder,
    JsonObject queryOptions,
    Boolean isPositive
  ) {
    Promise<List<JsonObject>> promise = Promise.promise();

    final int page  = (pageNumber == null || pageNumber < 1) ? 1 : pageNumber;
    final int limit = (pageLimit == null || pageLimit < 1) ? 5 : pageLimit;
    final int skip  = (page - 1) * limit;

    final String wantedStatus = Boolean.TRUE.equals(isPositive)
      ? "POSITIVE_DECISION_ISSUED"
      : "NEGATIVE_DECISION_ISSUED";

    final String effectiveSortBy =
      (sortBy == null || sortBy.isBlank()) ? "updatedAt" : sortBy;

    final int effectiveSortOrder =
      ("asc".equalsIgnoreCase(sortOrder)) ? 1 : -1;

    JsonArray pipeline = new JsonArray();

    // 1) Application-level filters
    if (queryOptions != null && !queryOptions.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", queryOptions));
    }

    // 2) Unwind datasets
    pipeline.add(new JsonObject().put("$unwind", "$datasets"));

    // 3) Keep only datasets with desired decision status
    pipeline.add(new JsonObject().put("$match", new JsonObject()
      .put("datasets.decisions", new JsonObject().put("$elemMatch",
        new JsonObject().put("datasetStatus", wantedStatus)
      ))
    ));

    // 4) Project + compute latestDecision using optimized $reduce
    pipeline.add(new JsonObject().put("$project", new JsonObject()
      .put("applicationId", "$_id")
      .put("applicationTitle", "$title")
      .put("applicationStatus", "$status.status")
      .put("hdab", "$datasets.hdab")
      .put(effectiveSortBy, 1)

      // Compute latestDecision directly (no temp arrays!)
      .put("latestDecision", new JsonObject().put("$reduce", new JsonObject()
        .put("input", "$datasets.decisions")
        .put("initialValue", new JsonObject())
        .put("in", new JsonObject().put("$cond", new JsonObject()
          .put("if", new JsonObject().put("$and", new JsonArray()
            // only consider matching status
            .add(new JsonObject().put("$eq", new JsonArray()
              .add("$$this.datasetStatus")
              .add(wantedStatus)
            ))
            // compare dates
            .add(new JsonObject().put("$gt", new JsonArray()
              .add("$$this.decision_date_received")
              .add("$$value.decision_date_received")
            ))
          ))
          .put("then", "$$this")
          .put("else", "$$value")
        ))
      ))
    ));

    // 5) Sort + pagination
    pipeline
      .add(new JsonObject().put("$sort", new JsonObject().put(effectiveSortBy, effectiveSortOrder)))
      .add(new JsonObject().put("$skip", skip))
      .add(new JsonObject().put("$limit", limit));

    // 6) Final projection
    pipeline.add(new JsonObject().put("$project", new JsonObject()
      .put("applicationId", 1)
      .put("applicationTitle", 1)
      .put("applicationStatus", 1)
      .put("hdab", 1)
      .put("latestDecision", 1)
    ));

    // 7) Execute
    ReadStream<JsonObject> stream = mongoClient.aggregate(collectionName, pipeline);
    List<JsonObject> items = new ArrayList<>();

    stream
      .handler(items::add)
      .exceptionHandler(promise::fail)
      .endHandler(v -> promise.complete(items));

    return promise.future();
  }

  public Future<List<JsonObject>> findDatasetsPerApplication(
    String collectionName,
    Integer pageNumber,
    Integer pageLimit,
    String sortBy,
    String sortOrder,
    JsonObject queryOptions
  ) {
    Promise<List<JsonObject>> promise = Promise.promise();

    final int page = (pageNumber == null || pageNumber < 1) ? 1 : pageNumber;
    final int limit = (pageLimit == null || pageLimit < 1) ? 5 : pageLimit;
    final int skip = (page - 1) * limit;

    JsonArray pipeline = new JsonArray();

    // 1) Pre-filter at application level (works also with $and)
    if (queryOptions != null && !queryOptions.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", queryOptions));
    }

    pipeline.add(new JsonObject().put("$unwind", "$datasets"));

    // ALWAYS: only datasets with decisions
    pipeline.add(new JsonObject().put("$match",
      new JsonObject().put("datasets.decisions",
        new JsonObject().put("$exists", true).put("$ne", null)
      )
    ));

    // decisionStatus filter (only if provided)
    JsonObject latestDecisionMatch = extractLatestDecisionMatch(queryOptions);
    if (latestDecisionMatch != null && !latestDecisionMatch.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", latestDecisionMatch));
    }

    // permitStatus filter (only if provided)
    JsonObject latestPermitMatch = extractLatestPermitMatch(queryOptions);
    if (latestPermitMatch != null && !latestPermitMatch.isEmpty()) {
      pipeline.add(new JsonObject().put("$match",
        new JsonObject().put("datasets.permits",
          new JsonObject().put("$exists", true).put("$ne", null)
        )
      ));
      pipeline.add(new JsonObject().put("$match", latestPermitMatch));
    }

    // 5) Project
    pipeline.add(new JsonObject().put("$project", new JsonObject()
      .put("applicationId", "$_id")
      .put("applicationTitle", "$title")
      .put("applicationStatus", "$status.status")
      .put("dateSubmitted", "$date_submitted")
      .put("dataset", "$datasets")
      .put("date_created", "$date_created")
      .put("date_modified", "$date_modified")
    ));

    String effectiveSortKey =
      (sortBy == null || sortBy.isBlank() || "date_created".equalsIgnoreCase(sortBy))
        ? "date_created"
        : "date_modified";

    int sortDir = ("asc".equalsIgnoreCase(sortOrder)) ? 1 : -1;

    pipeline.add(new JsonObject().put("$sort", new JsonObject().put(effectiveSortKey, sortDir)));
    pipeline.add(new JsonObject().put("$skip", skip));
    pipeline.add(new JsonObject().put("$limit", limit));

    ReadStream<JsonObject> stream = mongoClient.aggregate(collectionName, pipeline);
    List<JsonObject> items = new ArrayList<>();

    stream.handler(items::add)
      .exceptionHandler(promise::fail)
      .endHandler(v -> promise.complete(items));

    return promise.future();
  }

  private JsonObject extractLatestDecisionMatch(JsonObject queryOptions) {
    if (queryOptions == null) {
      return null;
    }

    // If you build queryOptions as {$and:[...]}
    JsonArray and = queryOptions.getJsonArray("$and");
    if (and != null) {
      for (int i = 0; i < and.size(); i++) {
        JsonObject clause = and.getJsonObject(i);
        if (clause == null) {
          continue;
        }

        if (clause.containsKey("datasets.decisions")) {
          // Re-apply on the single unwound dataset
          return new JsonObject().put("datasets.decisions", clause.getValue("datasets.decisions"));
        }
      }
      return null;
    }

    // fallback if flat
    if (queryOptions.containsKey("datasets.decisions")) {
      return new JsonObject().put("datasets.decisions",
        queryOptions.getValue("datasets.decisions"));
    }

    return null;
  }

  private JsonObject extractLatestPermitMatch(JsonObject queryOptions) {
    if (queryOptions == null) {
      return null;
    }

    JsonArray and = queryOptions.getJsonArray("$and");
    if (and != null) {
      for (int i = 0; i < and.size(); i++) {
        JsonObject clause = and.getJsonObject(i);
        if (clause == null) {
          continue;
        }

        if (clause.containsKey("datasets.permits")) {
          return new JsonObject().put("datasets.permits", clause.getValue("datasets.permits"));
        }
      }
      return null;
    }

    if (queryOptions.containsKey("datasets.permits")) {
      return new JsonObject().put("datasets.permits", queryOptions.getValue("datasets.permits"));
    }

    return null;
  }

  public Future<Integer> countDecisionIssuedDatasetsPerApplication(
    String collectionName,
    JsonObject queryOptions,
    Boolean isPositive
  ) {
    Promise<Integer> promise = Promise.promise();

    JsonArray allowedStatuses = new JsonArray();
    if (isPositive == null) {
      allowedStatuses.add("POSITIVE_DECISION_ISSUED");
      allowedStatuses.add("NEGATIVE_DECISION_ISSUED");
    } else if (Boolean.TRUE.equals(isPositive)) {
      allowedStatuses.add("POSITIVE_DECISION_ISSUED");
    } else {
      allowedStatuses.add("NEGATIVE_DECISION_ISSUED");
    }

    JsonArray pipeline = new JsonArray();

    if (queryOptions != null && !queryOptions.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", queryOptions));
    }

    pipeline.add(new JsonObject().put("$unwind", "$datasets"));

    pipeline.add(new JsonObject().put("$match",
      new JsonObject().put("datasets.decisions",
        new JsonObject().put("$exists", true).put("$ne", null)
      )
    ));

    // ✅ Any decision with allowed status
    pipeline.add(new JsonObject().put("$match",
      new JsonObject().put("datasets.decisions",
        new JsonObject().put("$elemMatch",
          new JsonObject().put("datasetStatus",
            new JsonObject().put("$in", allowedStatuses)
          )
        )
      )
    ));

    // ✅ IMPORTANT: removed latestDecision match

    pipeline.add(new JsonObject().put("$count", "count"));

    mongoClient.aggregate(collectionName, pipeline)
      .collect(Collectors.toList())
      .onSuccess(list -> {
        int count = list.isEmpty() ? 0 : list.get(0).getInteger("count", 0);
        promise.complete(count);
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  public Future<Integer> countDatasetsPerDataPermitGrantedDatasetApplication(
    String collectionName,
    JsonObject queryOptions
  ) {
    Promise<Integer> p = Promise.promise();

    JsonArray pipeline = new JsonArray();

    // 1) Application-level pre-filter
    if (queryOptions != null && !queryOptions.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", queryOptions));
    }

    // 2) One row per dataset
    pipeline.add(new JsonObject().put("$unwind", "$datasets"));

    // 3) Keep datasets that have ANY granted permit (NO latest)
    pipeline.add(new JsonObject().put("$match", new JsonObject()
      .put("datasets.permits", new JsonObject().put("$elemMatch", new JsonObject()
        .put("permit_status", "GRANTED")
      ))
    ));

    // 4) Count UNIQUE datasets (per application + dataset)
    pipeline.add(new JsonObject().put("$group", new JsonObject()
      .put("_id", new JsonObject()
          .put("applicationId", "$_id")
          // ✅ use the correct dataset id field from your docs:
          .put("datasetId", "$datasets.dataset_id")
        // if your DB uses "$datasets.id" keep that instead
      )
    ));

    // 5) Count
    pipeline.add(new JsonObject().put("$count", "count"));

    ReadStream<JsonObject> stream = mongoClient.aggregate(collectionName, pipeline);
    List<JsonObject> out = new ArrayList<>();

    stream
      .handler(out::add)
      .exceptionHandler(p::fail)
      .endHandler(v -> {
        int count = out.isEmpty() ? 0 : out.get(0).getInteger("count", 0);
        p.complete(count);
      });

    return p.future();
  }

  public Future<Integer> countDatasetsPerApplication(String collectionName,
    JsonObject queryOptions) {
    Promise<Integer> p = Promise.promise();

    // Split filters
    JsonObject appMatch = new JsonObject();
    JsonObject dsMatch = new JsonObject();
    if (queryOptions != null) {
      for (String key : queryOptions.fieldNames()) {
        Object v = queryOptions.getValue(key);
        if (key.startsWith("datasets.")) {
          dsMatch.put(key.substring("datasets.".length()), v);
        } else {
          appMatch.put(key, v);
        }
      }
    }

    JsonArray pipeline = new JsonArray();

    if (!appMatch.isEmpty()) {
      pipeline.add(new JsonObject().put("$match", appMatch));
    }

    pipeline.add(new JsonObject().put("$unwind", "$datasets"));

    // NEW: Count only datasets that have a decisions object
    pipeline.add(new JsonObject().put("$match",
      new JsonObject().put("datasets.decisions",
        new JsonObject()
          .put("$exists", true)
          .put("$ne", null)
      )
    ));

    if (!dsMatch.isEmpty()) {
      JsonObject q = new JsonObject();
      for (String k : dsMatch.fieldNames()) {
        q.put("datasets." + k, dsMatch.getValue(k));
      }
      pipeline.add(new JsonObject().put("$match", q));
    }

    pipeline.add(new JsonObject().put("$count", "count"));

    ReadStream<JsonObject> stream = mongoClient.aggregate(collectionName, pipeline);
    List<JsonObject> out = new ArrayList<>();

    stream.handler(out::add)
      .exceptionHandler(p::fail)
      .endHandler(v -> p.complete(out.isEmpty() ? 0 : out.get(0).getInteger("count", 0)));

    return p.future();
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

  public Future<Integer> countAll(String collectionName, JsonObject queryOptions) {
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

    log.debug("{} Entered updateLinkableFields method", dbHandlerClassName);

    JsonObject query = new JsonObject().put("objectId", objectId);
    JsonObject update = new JsonObject().put("$set", new JsonObject().put("linkable", false));

    Promise<Object> promise = Promise.promise();

    UpdateOptions options = new UpdateOptions().setMulti(true);
    mongoClient.updateCollectionWithOptions(collectionName, query, update, options, res -> {
      if (res.succeeded()) {
        log.debug("{} Linkable fields updated successfully", dbHandlerClassName);
        promise.complete();
      } else {
        log.error("{} Failed to update linkable fields, with error message: {}", dbHandlerClassName,
          res.cause().getMessage());
        promise.fail(res.cause());
      }
    });

    log.debug("{} Exiting updateLinkableFields method", dbHandlerClassName);
    return promise.future();
  }

  public Future<JsonObject> findOne(String collection, JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();

    mongoClient.findOne(collection, query, null, ar -> {
      if (ar.succeeded()) {
        promise.complete(ar.result());
      } else {
        promise.fail(ar.cause());
      }
    });

    return promise.future();
  }

  public Future<Object> updateMany(String collectionName, JsonObject query, JsonObject update) {
    Promise<Object> promise = Promise.promise();

    UpdateOptions options = new UpdateOptions().setMulti(true);
    mongoClient.updateCollectionWithOptions(collectionName, query, update, options, res -> {
      if (res.succeeded()) {
        promise.complete(res.result());
      } else {
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  /**
   * Update Document for Specific Collection
   *
   * @param collectionName the collection
   * @param updateQuery the query with the fields that they will going to update
   * change it to _id)
   */
  public Future<JsonObject> updateByField(String collectionName, JsonObject updateQuery,
    String fieldName, String fieldValue) {
    log.debug(MSG_FORMAT, CLASS_NAME, ENTERED_SAVE);
    //Define the query to find the document to update
    //on database level it is generated with name _id
    JsonObject query = new JsonObject().put(fieldName, fieldValue);
    Promise<JsonObject> promise = Promise.promise();
    //Remove the "id" field from the request body to prevent create a new field with it
    //wee need keep only the _id of mongoDb
    updateQuery.remove(fieldName);
    mongoClient.updateCollection(collectionName, query, new JsonObject().put("$set", updateQuery),
      asyncResult -> {
        if (asyncResult.succeeded()) {
          log.debug(SAVED_SUCCESSFULLY, CLASS_NAME, asyncResult.result());
          promise.complete(JsonObject.mapFrom(asyncResult.result()));
        } else {
          log.error(FAILED_TO_SAVE, CLASS_NAME, asyncResult.cause());
          promise.fail(asyncResult.cause());
        }
      });
    log.debug(MSG_FORMAT, CLASS_NAME, EXITING_SAVE);

    return promise.future();
  }


  private void logQuery(JsonObject providedQuery) {
    if (LOG.isDebugEnabled() && providedQuery != null) {
      log.debug(MSG_FORMAT, CLASS_NAME, providedQuery.encodePrettily());
    }
  }

  /**
   * Find a Document by field for the provided collection.
   *
   * @param collectionName the collection
   * @param jsonObject     the object to retrieve
   * @param exactMatch     if the match should be exact or work as a like
   */
  public Future<List<JsonObject>> findAllByField(String collectionName, JsonObject jsonObject,
    boolean exactMatch) {
    if (LOG.isDebugEnabled()) {
      log.debug(MSG_FORMAT, CLASS_NAME, ENTERED_FIND_BY_FIELD);
      logQuery(jsonObject);
    }

    JsonObject query = new JsonObject();
    if (exactMatch) {
      query = jsonObject;
    } else {
      Map.Entry<String, Object> firstEntry = jsonObject.stream().findFirst().orElse(null);

      if (firstEntry != null) {
        query.put(firstEntry.getKey(),
          new JsonObject().put("$regex", firstEntry.getValue()).put("$options", "i"));
      }
    }

    Promise<List<JsonObject>> promise = Promise.promise();
    mongoClient.find(collectionName, query, asyncResult -> {
      if (asyncResult.succeeded()) {
        if (asyncResult.result() != null && !asyncResult.result().isEmpty()) {
          log.debug(JSON_OBJECT_RETRIEVED_SUCCESSFULLY, CLASS_NAME);
          promise.complete(asyncResult.result());
        } else {
          log.debug(RETRIEVED_JSON_OBJECT_IS_EMPTY, CLASS_NAME);
          promise.fail(new ServiceException(404, HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        }
      } else {
        log.debug(FAILED_TO_RETRIEVE_JSON_OBJECT, CLASS_NAME);
        promise.fail(asyncResult.cause());
      }
    });
    log.debug(MSG_FORMAT, CLASS_NAME, EXITING_FIND_BY_FIELD);

    return promise.future();
  }

}
