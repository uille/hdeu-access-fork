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

import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.ERRORS_PARAM;
import static eu.europa.data.health.access.Constants.RESULT_MESSAGE_PARAM;
import static eu.europa.data.health.access.Constants.USER_ID_PARAM;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;


/**
 * Abstract Class that implements all the CRUD operations for Entity.
 */
public abstract class AbstractManager {

  private final DBHandler dbHandler;

  protected AbstractManager(DBHandler dbHandler) {
    this.dbHandler = dbHandler;
  }

  /**
   * Creates a document for the Entity collection
   *
   * @param context        the body that it is to update
   * @param collectionName the Collection Name
   */
  public void createEntity(@NotNull RoutingContext context, String collectionName) {
    dbHandler.create(collectionName, context.body().asJsonObject()).onSuccess(
        id -> setUpMessageContextResponseWithId(context, id))
      .onFailure(cause -> setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR));
  }

  /**
   * Update Document from specific Collection
   *
   * @param context        the body that it is to update
   * @param collectionName the Collection Name
   */
  public void updateEntity(@NotNull RoutingContext context, String collectionName) {
    JsonObject updateQuery = context.body().asJsonObject();
    updateApplication(context, collectionName, updateQuery.getString("id"), updateQuery);
  }

  /**
   * Patch specific fields of Document from specific Collection
   *
   * @param context        the body that it is to update
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
        userId)
      .onSuccess(list -> {
        if (pageNumber != null && effectiveLimit != null) {
          dbHandler.countAllDocuments(collectionNameString, null, userId)
            .compose(pageCount ->
              dbHandler.countAllDocuments(collectionNameString, status, userId)
                .map(pageCountFilter -> prepareGetEntityResponse(list, pageCount, pageCountFilter,
                  pageNumber, effectiveLimit, sortBy, sortOrder, status))
            )
            .onSuccess(response -> {
              setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
            })
            .onFailure(err -> {
              setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                new JsonObject().put("error", err.getMessage()).encode());
            });
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
   * Get Audit logs
   */
  public void getAuditLogs(@NotNull RoutingContext context, Integer pageNumber, Integer pageLimit,
    String sortBy, String sortOrder, JsonObject queryOptions) {

    JsonArray users;
    if(queryOptions.containsKey("users")){
      users = queryOptions.getJsonArray("users");
      queryOptions.remove("users");
    } else {
      users = null;
    }

    final Integer effectiveLimit;
    if (pageLimit != null) {
      effectiveLimit = Math.min(pageLimit, Constants.PAGINATION_LIMIT);
    } else {
      effectiveLimit = null;
    }

    dbHandler.findAuditLogs(AUDIT_LOG_COLLECTION, pageNumber, effectiveLimit, sortBy, sortOrder, queryOptions)
      .onSuccess(list -> {

        list.forEach(auditLog -> {
          if(!"Dataset Record".equalsIgnoreCase(auditLog.getString("object"))){
            String auditLogUserId = auditLog.getString("userId");
            if(users != null){
              users.forEach(userObj -> {
                JsonObject user = (JsonObject) userObj;
                if(auditLogUserId.equalsIgnoreCase(user.getString("ecasId"))){
                  auditLog.put("name", user.getString("firstName") + " " + user.getString("lastName") + " (" +  user.getString("ecasId") + ")");
                }
              });
            }
          } else {
            auditLog.put("name", "System");
          }
        });

        if (pageNumber != null && effectiveLimit != null) {
          dbHandler.countAllAuditLogs(AUDIT_LOG_COLLECTION, queryOptions)
            .compose(pageCount ->
              dbHandler.countAllAuditLogs(AUDIT_LOG_COLLECTION, queryOptions)
                .map(pageCountFilter -> prepareGetEntityResponse(list, pageCount, pageCountFilter,
                  pageNumber, effectiveLimit, sortBy, sortOrder, null))
            )
            .onSuccess(response -> {
              setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
            })
            .onFailure(err -> {
              setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                new JsonObject().put("error", err.getMessage()).encode());
            });
        } else {
          // No pagination involved
          JsonObject response = prepareGetEntityResponse(list, sortBy, sortOrder, null);
          setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
        }
      }).onFailure(
        cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          cause.getMessage()));
  }

  /**
   * find by id and user id
   */
  public void findById(@NotNull RoutingContext context, String collectionName) {
    JsonObject query = new JsonObject().put("_id", context.pathParam("id"));
    query.put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.findByField(collectionName, query).onSuccess(
        jsonObject -> setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode()))
      .onFailure(cause ->
      {
        int errorCode = ((ServiceException) cause).failureCode();
        setUpContextResponse(context, HttpResponseStatus.valueOf(errorCode));
      });
  }

  /**
   * Retrieve a document from a collection by providing the path parameter variable
   *
   * @param context        The RoutingContext
   * @param collectionName The collection name to retrieve the object from
   * @param pathParam      The field value to retrieve the object from the collection
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
   * @param context        The RoutingContext
   * @param collectionName The collection name to delete the object from
   * @param pathParam      The field value to delete the object from the collection
   */
  public void deleteByPathParam(@NotNull RoutingContext context, String collectionName,
    String pathParam) {
    JsonObject query = new JsonObject().put(pathParam, context.pathParam(pathParam));
    query.put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.deleteByField(collectionName, query).onSuccess(
        jsonObject -> setUpContextResponse(context, HttpResponseStatus.NO_CONTENT))
      .onFailure(
        cause -> setUpMessageContextResponse(context, HttpResponseStatus.OK, cause.getMessage()));
  }

  /**
   * Delete a Document by field for the Entity collection.
   *
   * @param context        The RoutingContext
   * @param collectionName The collection name to delete the object from
   */
  public void deleteEntityById(@NotNull RoutingContext context, String collectionName) {
    String id = context.pathParam("id");
    JsonObject query = new JsonObject().put("_id", id);

    dbHandler.deleteById(collectionName, query).onSuccess(
        event -> setUpContextResponse(context, HttpResponseStatus.OK, successJson(id)))
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
   * @param context        The RoutingContext
   * @param collectionName The collection name to delete the objects from
   */
  public void deleteAllEntities(@NotNull RoutingContext context, String collectionName) {
    //In the future, we will pass the clientId
    JsonObject queryDocument = new JsonObject().put(USER_ID_PARAM, Context.getEcasId(context));
    dbHandler.deleteAll(collectionName, queryDocument)
      .onSuccess(
        event -> setUpContextResponse(context, HttpResponseStatus.NO_CONTENT))
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
      jsonObject.put("error", error);
    }

    return jsonObject.encodePrettily();
  }

  private JsonObject prepareGetEntityResponse(List<JsonObject> data, String sortBy,
    String sortOrder, String status) {
    return prepareGetEntityResponse(data, null, null, null, null, sortBy, sortOrder, status);
  }

   public JsonObject prepareGetEntityResponse(List<JsonObject> data, Integer pageCount,
    Integer pageCountFilter, Integer pageNumber, Integer pageLimit, String sortBy, String sortOrder,
    String status) {

    JsonObject metadata = new JsonObject();
    if (pageNumber != null && pageLimit != null) {
      //Calculate the number of pages based on the page limit
      int numberOfPages = (int) Math.ceil(((double) pageCountFilter / pageLimit));
      metadata.put("total_documents", pageCount);
      metadata.put("total_documents_filtered", pageCountFilter);
      metadata.put("page_size", pageLimit);
      metadata.put("current_page", pageNumber);
      metadata.put("total_pages", numberOfPages);
    }

    if(sortBy != null && sortOrder != null) {
      metadata.put("sort_by", sortBy);
      metadata.put("sort_order", sortOrder);
    }

    if (StringUtils.isNotEmpty(status)) {
      metadata.put("status", status);
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
          dbHandler.countAllDocuments(collectionNameString, null)
            .compose(pageCount ->
              dbHandler.countAllDocuments(collectionNameString, status)
                .map(pageCountFilter -> prepareGetEntityResponse(list, pageCount, pageCountFilter,
                  pageNumber, effectiveLimit, sortBy, sortOrder, status))
            )
            .onSuccess(response -> {
              setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
            })
            .onFailure(err -> {
              setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                new JsonObject().put("error", err.getMessage()).encode());
            });
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

  protected void setUpMessageContextResponse(RoutingContext context, HttpResponseStatus status,
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

  private String getValidationErrors(RoutingContext context) {
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

  protected void setUpContextResponse(RoutingContext context, HttpResponseStatus status,
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
    vertx.fileSystem().readFile(fileUploaded.uploadedFileName())
      .compose(
        fileBuffer -> hubStoreClient.uploadFile(parentName, parentId, fileUploaded.fileName(),
          fileBuffer, contentType))
      .onSuccess(responseJson -> {
        context.response().putHeader(CONTENT_TYPE, "application/json");
        setUpContextResponse(context, HttpResponseStatus.CREATED, responseJson.encode());
      })
      .onFailure(
        err -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to upload file"));
  }
}
