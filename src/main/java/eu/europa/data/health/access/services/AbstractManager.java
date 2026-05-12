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

import static eu.europa.data.health.access.Constants.APPLICATIONID;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.ERRORS_PARAM;
import static eu.europa.data.health.access.Constants.RESULT_MESSAGE_PARAM;
import static eu.europa.data.health.access.Constants.USER_ID_PARAM;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.dto.SearchCriteriaDto;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract Class that implements all the CRUD operations for Entity.
 */
public abstract class AbstractManager {

  private static final Logger log = LoggerFactory.getLogger(AbstractManager.class);

  /** Matches a MongoDB ObjectId — exactly 24 lower/upper-case hex characters. */
  private static final Pattern HUB_STORE_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");

  /** Returns {@code true} only when {@code value} looks like a HubStore / MongoDB ObjectId. */
  private static boolean isHubStoreFileId(String value) {
    return value != null && HUB_STORE_ID_PATTERN.matcher(value).matches();
  }

  public static final String USERS = "users";
  public static final String ERROR = "error";
  final DBHandler dbHandler;

  protected AbstractManager(DBHandler dbHandler) {
    this.dbHandler = dbHandler;
  }

  /**
   * Creates a document for the Entity collection
   *
   * @param context the body that it is to update
   * @param collectionName the Collection Name
   */
  public void createEntity(@NotNull RoutingContext context, String collectionName) {
    dbHandler.create(collectionName, context.body().asJsonObject())
      .onSuccess(id -> setUpMessageContextResponseWithId(context, id))
      .onFailure(cause -> setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR));
  }

  /**
   * Update Document from specific Collection
   *
   * @param context the body that it is to update
   * @param collectionName the Collection Name
   */
  public void updateEntity(@NotNull RoutingContext context, String collectionName) {
    JsonObject updateQuery = context.body().asJsonObject();
    updateApplication(context, collectionName, updateQuery.getString("id"), updateQuery);
  }

  /**
   * Patch specific fields of Document from specific Collection
   *
   * @param context the body that it is to update
   * @param collectionName the Collection Name
   */
  public void patchEntity(@NotNull RoutingContext context, String collectionName) {
    JsonObject updateQuery = context.body().asJsonObject();
    updateApplication(context, collectionName, context.pathParam("id"), updateQuery);
  }

  private void updateApplication(@NotNull RoutingContext context, String collectionName, String id,
    JsonObject updateQuery) {
    //Define the query to find the document to update
    //on database level it is generated with name _id
    JsonObject query = new JsonObject().put("_id", id);
    query.put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.update(collectionName, updateQuery, query).onSuccess(
        jsonObject -> setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode()))
      .onFailure(cause -> setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR));
  }

  /**
   * Get all Documents for the Entity collection.
   */
  public void getEntity(@NotNull RoutingContext context, String collectionNameString,
    Integer pageNumber, Integer pageLimit, String sortBy, String sortOrder, String status) {

    String userId = Context.getEcasId(context);

    final Integer effectiveLimit;
    if (pageLimit != null) {
      effectiveLimit = Math.min(pageLimit, Constants.PAGINATION_LIMIT);
    } else {
      effectiveLimit = null;
    }

    dbHandler.findAll(collectionNameString, pageNumber, effectiveLimit, sortBy, sortOrder, status,
      userId).onSuccess(list -> {
      if (pageNumber != null && effectiveLimit != null) {
        dbHandler.countAllDocuments(collectionNameString, null, userId)
          .compose(pageCount ->
            dbHandler.countAllDocuments(collectionNameString, status, userId).map(
              pageCountFilter -> prepareGetEntityResponse(list,
                new SearchCriteriaDto(pageCount, pageCountFilter,
                  pageNumber, effectiveLimit, sortBy,sortOrder, status)))
          ).onSuccess(response ->
            setUpContextResponse(context, HttpResponseStatus.OK, response.encode())
          ).onFailure(err ->
            setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            new JsonObject().put(ERROR, err.getMessage()).encode()));
      } else {
        // No pagination involved
        JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, status);
        setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
      }
    }).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        cause.getMessage()));
  }

  /**
   * Get Entities
   */
  public void getEntities(
    @NotNull RoutingContext context,
    String collectionName,
    Integer pageNumber,
    Integer pageLimit,
    String sortBy,
    String sortOrder,
    JsonObject queryOptions
  ) {
    getEntities(
      context,
      collectionName,
      pageNumber,
      pageLimit,
      sortBy,
      sortOrder,
      queryOptions,
      null
    );
  }

  /**
   * Get Entities with projection
   */
  public void getEntities(
    @NotNull RoutingContext context,
    String collectionName,
    Integer pageNumber,
    Integer pageLimit,
    String sortBy,
    String sortOrder,
    JsonObject queryOptions,
    JsonObject projection
  ) {
    Integer effectiveLimit =
      (pageLimit != null) ? Math.min(pageLimit, Constants.PAGINATION_LIMIT) : null;

    dbHandler
      .findEntities(
        collectionName,
        pageNumber,
        effectiveLimit,
        sortBy,
        sortOrder,
        queryOptions,
        projection
      )
      .onSuccess(list ->
        buildResponseWithPagination(
          context,
          collectionName,
          pageNumber,
          sortBy,
          sortOrder,
          queryOptions,
          list,
          effectiveLimit
        )
      )
      .onFailure(cause ->
        setUpMessageContextResponse(
          context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          cause.getMessage()
        )
      );
  }

  /**
   * Get Datasets
   */
  public void getDatasets(@NotNull RoutingContext context, String collectionName, Integer pageNumber, Integer pageLimit,
    String sortBy, String sortOrder, JsonObject queryOptions) {

    Integer effectiveLimit =
      (pageLimit != null) ? Math.min(pageLimit, Constants.PAGINATION_LIMIT) : null;

    dbHandler.findDatasetsPerApplication(collectionName, pageNumber, effectiveLimit, sortBy, sortOrder,
      queryOptions).onSuccess(list -> {
      buildResponseWithPaginationPerDataset(context, collectionName, pageNumber, sortBy, sortOrder, queryOptions,
        list, effectiveLimit);
    }).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        cause.getMessage()));
  }

  /**
   * Get Datasets with HubStore file enrichment.
   * For every decision's {@code translatedFields}, fields whose name contains "file" are resolved
   * from HubStore (the stored value is a file ID) and replaced with the base64-encoded content
   * returned by {@code getFileAsBase64}.
   */
  public void getDatasets(@NotNull RoutingContext context, String collectionName, Integer pageNumber, Integer pageLimit,
    String sortBy, String sortOrder, JsonObject queryOptions, HubStoreClient hubStoreClient) {

    Integer effectiveLimit =
      (pageLimit != null) ? Math.min(pageLimit, Constants.PAGINATION_LIMIT) : null;

    dbHandler.findDatasetsPerApplication(collectionName, pageNumber, effectiveLimit, sortBy, sortOrder, queryOptions)
      .compose(list ->
        enrichTranslatedFileFields(list, hubStoreClient)
          .recover(err -> {
            // Enrichment errors must never block the response — degrade gracefully
            log.error("File enrichment failed for collection {}, returning unenriched data: {}",
              collectionName, err.getMessage() != null ? err.getMessage() : err.getClass().getSimpleName());
            return Future.succeededFuture();
          })
          .map(v -> list))
      .onSuccess(list ->
        buildResponseWithPaginationPerDataset(context, collectionName, pageNumber, sortBy, sortOrder, queryOptions,
          list, effectiveLimit))
      .onFailure(cause -> {
        String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        log.error("Failed to retrieve datasets for collection {}: {}", collectionName, cause);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, msg);
      });
  }

  /**
   * For each item in the list, finds {@code translatedFields} inside every decision,
   * and for every field whose name contains "file" replaces the stored HubStore file ID with the
   * base64-encoded file content fetched from HubStore.
   * Failures on individual files are logged and silently skipped so the response is never blocked.
   */
  private Future<Void> enrichTranslatedFileFields(List<JsonObject> items, HubStoreClient hubStoreClient) {
    if (hubStoreClient == null || items == null || items.isEmpty()) {
      return Future.succeededFuture();
    }
    try {
      List<Future<Void>> futures = new ArrayList<>();

      for (JsonObject item : items) {
        String applicationId = item.getString("applicationId");
        log.info("Enriching translated file fields for {}", applicationId);
        if (StringUtils.isBlank(applicationId)) {
          continue;
        }

        JsonObject dataset = item.getJsonObject("dataset");
        if (dataset == null) {
          continue;
        }

        JsonArray decisions = dataset.getJsonArray("decisions");
        if (decisions != null && !decisions.isEmpty()) {
          enrichFileFieldsInItems(decisions, applicationId, futures, hubStoreClient);
        }

        JsonArray permits = dataset.getJsonArray("permits");
        if (permits != null && !permits.isEmpty()) {
          enrichFileFieldsInItems(permits, applicationId, futures, hubStoreClient);
        }
      }

      return futures.isEmpty() ? Future.succeededFuture() : Future.all(futures).mapEmpty();

    } catch (Exception e) {
      // Convert any synchronous exception (e.g. ClassCastException from malformed DB data)
      // into a failed Future so the .recover() in the caller can handle it gracefully.
      log.warn("Unexpected exception during file enrichment (will return unenriched data): {}",
        e.getMessage() != null ? e.getMessage() : e.getClass().getName(), e);
      return Future.failedFuture(e);
    }
  }

  /**
   * Iterates over a JSON array of items (decisions or permits), and for every field whose name
   * contains "file" — both at the top level of each item AND inside {@code translatedFields} —
   * replaces the stored HubStore file ID with the base64-encoded content fetched from HubStore.
   * Populates {@code futures} in-place.
   */
  private void enrichFileFieldsInItems(JsonArray items, String applicationId,
    List<Future<Void>> futures, HubStoreClient hubStoreClient) {

    for (int i = 0; i < items.size(); i++) {
      JsonObject item;
      try {
        item = items.getJsonObject(i);  // throws ClassCastException if element is a raw String
      } catch (ClassCastException e) {
        log.warn("decisions/permits[{}] is not a JsonObject — skipping ({})", i, e.getMessage());
        continue;
      }
      if (item == null) {
        continue;
      }

      // ── 1. Top-level file fields (e.g. permit_file, decision_file, support_file) ──
      for (String fieldName : new ArrayList<>(item.fieldNames())) {
        if (!fieldName.contains("file")) {
          continue;
        }
        Object value = item.getValue(fieldName);
        if (!(value instanceof String fileId) || !isHubStoreFileId(fileId)) {
          continue;
        }
        futures.add(fetchAndReplaceFileField(item, fieldName, fileId, applicationId, hubStoreClient));
      }

      // ── 2. translatedFields: { lang -> { fieldName -> fileId } } ──
      JsonObject translatedFields;
      try {
        translatedFields = item.getJsonObject("translatedFields");
      } catch (ClassCastException e) {
        log.warn("translatedFields is not a JsonObject in item — skipping ({})", e.getMessage());
        continue;
      }
      if (translatedFields == null || translatedFields.isEmpty()) {
        continue;
      }

      for (String lang : translatedFields.fieldNames()) {
        JsonObject langFields;
        try {
          langFields = translatedFields.getJsonObject(lang);
        } catch (ClassCastException e) {
          log.warn("translatedFields['{}'] is not a JsonObject — skipping ({})", lang, e.getMessage());
          continue;
        }
        if (langFields == null) {
          continue;
        }

        for (String fieldName : new ArrayList<>(langFields.fieldNames())) {
          if (!fieldName.contains("file")) {
            continue;
          }
          String fileId = langFields.getString(fieldName);
          if (!isHubStoreFileId(fileId)) {
            continue;
          }
          futures.add(fetchAndReplaceFileField(langFields, fieldName, fileId, applicationId, hubStoreClient));
        }
      }
    }
  }

  /**
   * Fetches a file from HubStore by ID and replaces the value in {@code target} in-place.
   * On failure the original value is kept and a warning is logged.
   */
  private Future<Void> fetchAndReplaceFileField(JsonObject target, String fieldName,
    String fileId, String applicationId, HubStoreClient hubStoreClient) {

    return hubStoreClient
      .getFileAsBase64(fileId, APPLICATIONID, applicationId)
      .map(response -> {
        String base64Content = response.getString("base64Content");
        if (StringUtils.isNotBlank(base64Content)) {
          target.put(fieldName, base64Content);
        }
        return (Void) null;
      })
      .recover(err -> {
        log.warn("Failed to retrieve file {} for field {} (app {}): {}",
          fileId, fieldName, applicationId, err.getMessage());
        return Future.succeededFuture();
      });
  }

  public void getDecisionIssuedDatasets(@NotNull RoutingContext context, String collectionName, Integer pageNumber, Integer pageLimit,
    String sortBy, String sortOrder, JsonObject queryOptions, Boolean isPositiveDecision) {

    Integer effectiveLimit =
      (pageLimit != null) ? Math.min(pageLimit, Constants.PAGINATION_LIMIT) : null;

    dbHandler.findDecisionIssuedDatasetsPerApplication(collectionName, pageNumber, effectiveLimit, sortBy, sortOrder,
      queryOptions, isPositiveDecision).onSuccess(list -> {
      buildResponseWithPaginationPerDecisionIssuedDataset(context, collectionName, pageNumber, sortBy, sortOrder, queryOptions,
        list, effectiveLimit, isPositiveDecision);
    }).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        cause.getMessage()));
  }

  private void buildResponseWithPaginationPerDecisionIssuedDataset(@NotNull RoutingContext context, String collectionName,
    Integer pageNumber, String sortBy, String sortOrder, JsonObject queryOptions,
    List<JsonObject> list, Integer effectiveLimit, Boolean isPositiveDecision) {

    if (pageNumber != null && effectiveLimit != null) {
      dbHandler.countDecisionIssuedDatasetsPerApplication(collectionName, queryOptions, isPositiveDecision)
        .map(totalDatasets ->
          prepareGetEntityResponse(
            list,
            new SearchCriteriaDto(
              totalDatasets,           // total count (datasets) for pagination
              totalDatasets,           // or a second metric if your DTO expects it
              pageNumber,
              effectiveLimit,
              sortBy,
              sortOrder,
              null
            )
          )
        )
        .onSuccess(response ->
          setUpContextResponse(context, HttpResponseStatus.OK, response.encode()))
        .onFailure(err ->
          setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            new JsonObject().put("error", err.getMessage()).encode()));
    } else {
      JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, null);
      setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
    }
  }


  public void getDataPermitGrantedDatasets(@NotNull RoutingContext context, String collectionName, Integer pageNumber, Integer pageLimit,
    String sortBy, String sortOrder, JsonObject queryOptions) {

    Integer effectiveLimit =
      (pageLimit != null) ? Math.min(pageLimit, Constants.PAGINATION_LIMIT) : null;

    dbHandler.findDataPermitGrantedDatasetsPerApplication(collectionName, pageNumber, effectiveLimit, sortBy, sortOrder,
      queryOptions).onSuccess(list -> {
      buildResponseWithPaginationPerDataPermitGrantedDataset(context, collectionName, pageNumber, sortBy, sortOrder, queryOptions,
        list, effectiveLimit);
    }).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        cause.getMessage()));
  }

  private void buildResponseWithPaginationPerDataPermitGrantedDataset(@NotNull RoutingContext context, String collectionName,
    Integer pageNumber, String sortBy, String sortOrder, JsonObject queryOptions,
    List<JsonObject> list, Integer effectiveLimit) {

    if (pageNumber != null && effectiveLimit != null) {
      dbHandler.countDatasetsPerDataPermitGrantedDatasetApplication(collectionName, queryOptions)
        .map(totalDatasets ->
          prepareGetEntityResponse(
            list,
            new SearchCriteriaDto(
              totalDatasets,           // total count (datasets) for pagination
              totalDatasets,           // or a second metric if your DTO expects it
              pageNumber,
              effectiveLimit,
              sortBy,
              sortOrder,
              null
            )
          )
        )
        .onSuccess(response ->
          setUpContextResponse(context, HttpResponseStatus.OK, response.encode()))
        .onFailure(err ->
          setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            new JsonObject().put("error", err.getMessage()).encode()));
    } else {
      JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, null);
      setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
    }
  }

  private void buildResponseWithPaginationPerDataset(@NotNull RoutingContext context, String collectionName,
    Integer pageNumber, String sortBy, String sortOrder, JsonObject queryOptions,
    List<JsonObject> list, Integer effectiveLimit) {

    if (pageNumber != null && effectiveLimit != null) {
      dbHandler.countDatasetsPerApplication(collectionName, queryOptions)
        .map(totalDatasets ->
          prepareGetEntityResponse(
            list,
            new SearchCriteriaDto(
              totalDatasets,           // total count (datasets) for pagination
              totalDatasets,           // or a second metric if your DTO expects it
              pageNumber,
              effectiveLimit,
              sortBy,
              sortOrder,
              null
            )
          )
        )
        .onSuccess(response ->
          setUpContextResponse(context, HttpResponseStatus.OK, response.encode()))
        .onFailure(err -> {
          setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            new JsonObject().put("error", err.getMessage()).encode());
          log.error("Failed to build response with pagination for collection {}: {}",
            collectionName,
            err.getMessage());
          });
    } else {
      JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, null);
      setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
    }
  }

  /**
   * Get Audit logs
   */
  public void getAuditLogs(@NotNull RoutingContext context, String collectionName, Integer pageNumber, Integer pageLimit,
    String sortBy, String sortOrder, JsonObject queryOptions) {

    JsonArray users = queryOptions.containsKey(USERS) ? queryOptions.getJsonArray(USERS) : null;
    queryOptions.remove(USERS);

    Integer effectiveLimit =
      (pageLimit != null) ? Math.min(pageLimit, Constants.PAGINATION_LIMIT) : null;

    dbHandler.findAuditLogs(AUDIT_LOG_COLLECTION, pageNumber, effectiveLimit, sortBy, sortOrder,
      queryOptions).onSuccess(list -> {
      enrichAuditLogNames(list, users);
      buildResponseWithPagination(context,collectionName, pageNumber, sortBy, sortOrder, queryOptions,
        list, effectiveLimit);
    }).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        cause.getMessage()));
  }

  private void enrichAuditLogNames(List<JsonObject> auditLogs, JsonArray users) {
    auditLogs.forEach(auditLog -> {
      if (!"Dataset Record".equalsIgnoreCase(auditLog.getString("object"))) {
        String auditLogUserId = auditLog.getString("userId");
        if(StringUtils.isNotEmpty(auditLogUserId)) {
          if (users != null) {
            users.forEach(userObj -> {
              JsonObject user = (JsonObject) userObj;
              if (auditLogUserId.equalsIgnoreCase(user.getString("ecasId"))) {
                String name = String.format("%s %s (%s)", user.getString("firstName", ""),
                  user.getString("lastName", ""), user.getString("ecasId", ""));
                auditLog.put("name", name);
              }
            });
          }
        } else {
          auditLog.put("name", "-");
        }
      } else {
        auditLog.put("name", "System");
      }
    });
  }

  private void buildResponseWithPagination(@NotNull RoutingContext context, String collectionName,
    Integer pageNumber, String sortBy, String sortOrder, JsonObject queryOptions,
    List<JsonObject> list, Integer effectiveLimit) {

    if (pageNumber != null && effectiveLimit != null) {
      dbHandler.countAll(collectionName, queryOptions).compose(
          pageCount -> dbHandler.countAll(collectionName,
              queryOptions) // Suggest separate method for filtered count
            .map(pageCountFilter -> prepareGetEntityResponse(list,
              new SearchCriteriaDto(pageCount, pageCountFilter, pageNumber, effectiveLimit, sortBy,
                sortOrder, null)))).onSuccess(
          response -> setUpContextResponse(context, HttpResponseStatus.OK, response.encode()))
        .onFailure(err -> setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          new JsonObject().put(ERROR, err.getMessage()).encode()));
    } else {
      JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, null);
      setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
    }
  }

  /**
   * find by id and user id
   */
  public void findById(@NotNull RoutingContext context, String collectionName) {
    JsonObject query = new JsonObject().put("_id", context.pathParam("id"));
    query.put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.findByField(collectionName, query).onSuccess(
        jsonObject -> setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode()))
      .onFailure(cause -> {
        int errorCode = ((ServiceException) cause).failureCode();
        setUpContextResponse(context, HttpResponseStatus.valueOf(errorCode));
      });
  }

  /**
   * find by id without security and additional information in the object
   */
  public void findByIdWithoutSecurity(@NotNull RoutingContext context, String collectionName) {

    // Query: match by ID and require ON-GOING application status
    JsonObject query = new JsonObject()
      .put("_id", context.pathParam("id"));

    dbHandler.findByField(collectionName, query)
      .onSuccess(jsonObject -> {
        if (jsonObject != null) {

          if (jsonObject.containsKey("additionalInformation")) {
            jsonObject.remove("additionalInformation");
          }
        }

        setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode());
      })
      .onFailure(cause -> {
        int errorCode = (cause instanceof ServiceException)
          ? ((ServiceException) cause).failureCode()
          : HttpResponseStatus.INTERNAL_SERVER_ERROR.code();

        setUpContextResponse(context, HttpResponseStatus.valueOf(errorCode));
      });
  }

  /**
   * Retrieve a document from a collection by providing the path parameter variable
   *
   * @param context The RoutingContext
   * @param collectionName The collection name to retrieve the object from
   * @param pathParam The field value to retrieve the object from the collection
   */
  public void findByPathParam(@NotNull RoutingContext context, String collectionName,
    String pathParam) {
    JsonObject query = new JsonObject().put(pathParam, context.pathParam(pathParam));
    query.put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.findByField(collectionName, query).onSuccess(
        jsonObject -> setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode()))
      .onFailure(
        cause -> setUpMessageContextResponse(context, HttpResponseStatus.OK, cause.getMessage()));
  }

  /**
   * Delete a document from a collection by providing the path parameter variable
   *
   * @param context The RoutingContext
   * @param collectionName The collection name to delete the object from
   * @param pathParam The field value to delete the object from the collection
   */
  public void deleteByPathParam(@NotNull RoutingContext context, String collectionName,
    String pathParam) {
    JsonObject query = new JsonObject().put(pathParam, context.pathParam(pathParam));
    query.put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.deleteByField(collectionName, query)
      .onSuccess(jsonObject -> setUpContextResponse(context, HttpResponseStatus.NO_CONTENT))
      .onFailure(
        cause -> setUpMessageContextResponse(context, HttpResponseStatus.OK, cause.getMessage()));
  }

  /**
   * Delete a Document by field for the Entity collection.
   *
   * @param context The RoutingContext
   * @param collectionName The collection name to delete the object from
   */
  public void deleteEntityById(@NotNull RoutingContext context, String collectionName) {
    String id = context.pathParam("id");
    JsonObject query = new JsonObject().put("_id", id);

    dbHandler.deleteById(collectionName, query)
      .onSuccess(event -> setUpContextResponse(context, HttpResponseStatus.OK, successJson(id)))
      .onFailure(cause -> {
        if (cause instanceof ServiceException se) {
          setUpContextResponse(context, HttpResponseStatus.valueOf(se.failureCode()),
            failureJson(se.getMessage(), se.failureCode()));
        } else {
          if (cause.getMessage() != null) {
            int code =
              cause.getMessage().equals(HttpResponseStatus.NOT_FOUND.reasonPhrase()) ? 404 : 500;
            setUpContextResponse(context, HttpResponseStatus.valueOf(code),
              failureJson(cause.getLocalizedMessage(), code));
          } else {
            context.fail(500);
          }
        }
      });
  }

  /**
   * Delete all Document for the Entity collection.
   *
   * @param context The RoutingContext
   * @param collectionName The collection name to delete the objects from
   */
  public void deleteAllEntities(@NotNull RoutingContext context, String collectionName) {
    //In the future, we will pass the clientId
    JsonObject queryDocument = new JsonObject().put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.deleteAll(collectionName, queryDocument)
      .onSuccess(event -> setUpContextResponse(context, HttpResponseStatus.NO_CONTENT))
      .onFailure(cause -> {
        if (cause instanceof ServiceException se) {
          setUpContextResponse(context, HttpResponseStatus.valueOf(se.failureCode()),
            failureJson(se.getMessage(), se.failureCode()));
        } else {
          if (cause.getMessage() != null) {
            int code =
              cause.getMessage().equals(HttpResponseStatus.NOT_FOUND.reasonPhrase()) ? 404 : 500;
            setUpContextResponse(context, HttpResponseStatus.valueOf(code),
              failureJson(cause.getLocalizedMessage(), code));
          } else {
            context.fail(500);
          }
        }
      });
  }

  private String successJson(String id) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.put("success", true);
    JsonObject result = new JsonObject();
    result.put("id", id);
    jsonObject.put("result", result);

    return jsonObject.encodePrettily();
  }

  private String failureJson(String error, int code) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.put("success", false);

    if (!error.isEmpty()) {
      jsonObject.put("code", code);
      jsonObject.put(ERROR, error);
    }

    return jsonObject.encodePrettily();
  }

  private JsonObject prepareGetEntityResponse(List<JsonObject> data, String sortBy,
    String sortOrder, String status) {
    return prepareGetEntityResponse(data,
      new SearchCriteriaDto(null, null, null, null, sortBy, sortOrder, status));

  }

  public JsonObject prepareGetEntityResponse(List<JsonObject> data,
    SearchCriteriaDto searchCriteriaDto) {

    JsonObject metadata = new JsonObject();
    if (searchCriteriaDto.getPageNumber() != null && searchCriteriaDto.getPageLimit() != null) {
      //Calculate the number of pages based on the page limit
      int numberOfPages = (int) Math.ceil(
        ((double) searchCriteriaDto.getPageCountFilter() / searchCriteriaDto.getPageLimit()));
      metadata.put("total_documents", searchCriteriaDto.getPageCount());
      metadata.put("total_documents_filtered", searchCriteriaDto.getPageCountFilter());
      metadata.put("page_size", searchCriteriaDto.getPageLimit());
      metadata.put("current_page", searchCriteriaDto.getPageNumber());
      metadata.put("total_pages", numberOfPages);
    }

    if (searchCriteriaDto.getSortBy() != null && searchCriteriaDto.getSortOrder() != null) {
      metadata.put("sort_by", searchCriteriaDto.getSortBy());
      metadata.put("sort_order", searchCriteriaDto.getSortOrder());
    }

    if (StringUtils.isNotEmpty(searchCriteriaDto.getStatus())) {
      metadata.put("status", searchCriteriaDto.getStatus());
    }

    JsonObject response = new JsonObject();
    response.put("data", data);
    response.put("metadata", metadata);

    return response;
  }

  public void getAllRecordsByAdmin(@NotNull RoutingContext context, String collectionNameString,
    Integer pageNumber, Integer pageLimit, String sortBy, String sortOrder, String status) {

    final Integer effectiveLimit;
    if (pageLimit != null) {
      effectiveLimit = Math.min(pageLimit, Constants.PAGINATION_LIMIT);
    } else {
      effectiveLimit = null;
    }

    dbHandler.findAll(collectionNameString, pageNumber, effectiveLimit, sortBy, sortOrder, status)
      .onSuccess(list -> {
        if (pageNumber != null && effectiveLimit != null) {
          dbHandler.countAllDocuments(collectionNameString, null).compose(
              pageCount -> dbHandler.countAllDocuments(collectionNameString, status).map(
                pageCountFilter -> prepareGetEntityResponse(list,
                  new SearchCriteriaDto(pageCount, pageCountFilter, pageNumber, effectiveLimit,
                    sortBy, sortOrder, status)))).onSuccess(
              response -> setUpContextResponse(context, HttpResponseStatus.OK, response.encode()))
            .onFailure(
              err -> setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                new JsonObject().put(ERROR, err.getMessage()).encode()));
        } else {
          // No pagination involved
          JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, status);
          setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
        }
      }).onFailure(
        cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          cause.getMessage()));
  }

  protected void setUpMessageContextResponseWithId(RoutingContext context, String id) {
    setUpContextResponse(context, HttpResponseStatus.CREATED,
      new JsonObject().put(RESULT_MESSAGE_PARAM, "Document created successfully").put("id", id)
        .encode());
  }

  static public void setUpMessageContextResponse(RoutingContext context, HttpResponseStatus status,
    String message) {
    if (context.get(ERRORS_PARAM) != null) {
      JsonObject response = new JsonObject();
      response.put(RESULT_MESSAGE_PARAM, message);
      response.put(ERRORS_PARAM,
        new JsonArray(Objects.requireNonNull(getValidationErrors(context))));
      setUpContextResponse(context, status, response.encode());
    } else {
      setUpContextResponse(context, status,
        new JsonObject().put(RESULT_MESSAGE_PARAM, message).encode());
    }
  }

  static private String getValidationErrors(RoutingContext context) {
    Object errors = context.get(ERRORS_PARAM);
    ObjectMapper objectMapper = new ObjectMapper();
    if (errors == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(context.get(ERRORS_PARAM));
    } catch (Exception e) {
      return null;
    }
  }

  static protected void setUpContextResponse(RoutingContext context, HttpResponseStatus status,
    String message) {
    context.response().setStatusCode(status.code()).end(message);
  }

  protected void setUpContextResponse(RoutingContext context, HttpResponseStatus status) {
    context.response().setStatusCode(status.code()).end();
  }

  public void handleFileUpload(RoutingContext context, String parentName, String parentId,
    HubStoreClient hubStoreClient, Vertx vertx) {

    String contentType = context.request().getHeader(CONTENT_TYPE);

    // Get the uploaded file
    FileUpload fileUploaded = context.fileUploads().get(0);
    vertx.fileSystem().readFile(fileUploaded.uploadedFileName()).compose(
      fileBuffer -> hubStoreClient.uploadFile(parentName, parentId, fileUploaded.fileName(),
        fileBuffer, contentType)).onSuccess(responseJson -> {
      context.response().putHeader(CONTENT_TYPE, "application/json");
      setUpContextResponse(context, HttpResponseStatus.CREATED, responseJson.encode());
    }).onFailure(
      err -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Failed to upload file"));
  }

  public Future<JsonObject> handleDocUpload(RoutingContext context, String parentName, String parentId,
    HubStoreClient hubStoreClient, Vertx vertx) {
    Promise<JsonObject> promise = Promise.promise();
    String contentType = context.request().getHeader(CONTENT_TYPE);

    // Get the uploaded file
    FileUpload fileUploaded = context.fileUploads().get(0);
    vertx.fileSystem().readFile(fileUploaded.uploadedFileName()).compose(
      fileBuffer -> hubStoreClient.uploadFile(parentName, parentId, fileUploaded.fileName(),
        fileBuffer, contentType)).onSuccess(responseJson -> {
      context.response().putHeader(CONTENT_TYPE, "application/json");
      setUpContextResponse(context, HttpResponseStatus.CREATED, responseJson.encode());
      promise.complete(responseJson);
    }).onFailure(err -> {
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Failed to upload file");
      promise.fail(err.getMessage());
    });
    return promise.future();
  }

  /**
   * Retrieve a document from a collection by providing the path parameter variable
   *
   * @param fieldName The fieldName of the object
   * @param value The value of the field of the object
   * @param collectionName The collection name to retrieve the object from
   */
  public Future<List<JsonObject>> findByField(String fieldName, String value,
    String collectionName) {
    JsonObject object = new JsonObject().put(fieldName, value);
    return dbHandler.findAllByField(collectionName, object, true);
  }

}
