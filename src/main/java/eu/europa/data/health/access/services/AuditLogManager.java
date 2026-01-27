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

import static eu.europa.data.health.access.Constants.ACTION;
import static eu.europa.data.health.access.Constants.ACTION_DESCRIPTION;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.ECAS_ID;
import static eu.europa.data.health.access.Constants.OBJECT;
import static eu.europa.data.health.access.Constants.OBJECT_ID;
import static eu.europa.data.health.access.Constants.OPTIONS;
import static eu.europa.data.health.access.Constants.REGEX;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.USER_ID_PARAM;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogManager extends AbstractManager {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String auditLogManagerClassName = this.getClass().getSimpleName() + " - ";

  private final DBHandler dbHandler;

  private final AuthClient authClient;

  public AuditLogManager(DBHandler dbHandler, AuthClient authClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.authClient = authClient;
  }

  /**
   * Insert an audit log entry without object id (legacy overload).
   * <p>
   * If {@code context} is present, {@code USER_ID} is taken from it and we save immediately. If
   * {@code context} is absent, there is no {@code objectId} in this overload, so we save with empty
   * {@code USER_ID} (best-effort policy).
   */
  public void insertAuditLog(String object, String action, String actionDescription,
    RoutingContext context) {
    // This overload uses linkable=false (existing behavior)
    JsonObject entry = createAuditLogEntry(object, null, action, actionDescription, false, context);

    if (context != null) {
      // USER_ID already set via Context.getEcasId(context)
      dbHandler.create(AUDIT_LOG_COLLECTION, entry);
      return;
    }

    // No context and no objectId in this overload → nothing to resolve
    dbHandler.create(AUDIT_LOG_COLLECTION, entry);
  }

  /**
   * Insert an audit log entry for a specific object id (e.g., applications, datasets).
   * <p>
   * If {@code context} is present, {@code USER_ID} is taken from it and we save immediately. If
   * {@code context} is absent and the object is an application (data-access or data-request), we
   * first resolve {@code USER_ID} from the application document asynchronously and only then save.
   * Otherwise (non-application objects), we save as-is with empty {@code USER_ID}.
   */
  public void insertAuditLog(String object, String objectId, String action,
    String actionDescription, boolean linkable, RoutingContext context) {

    JsonObject entry = createAuditLogEntry(object, objectId, action, actionDescription, linkable,
      context);

    if (context != null) {
      // USER_ID already set by builder from the request context
      dbHandler.create(AUDIT_LOG_COLLECTION, entry);
      return;
    }

    // No context → for application objects we want to resolve userId from the application doc
    boolean isAppObject =
      AuditLogObject.DATA_ACCESS_APPLICATION.toString().equals(object) ||
        AuditLogObject.DATA_REQUEST_APPLICATION.toString().equals(object);

    if (isAppObject && StringUtils.isNotEmpty(objectId)) {
      String applicationCollection =
        AuditLogObject.DATA_ACCESS_APPLICATION.toString().equals(object)
          ? DATA_ACCESS_APPLICATION_COLLECTION
          : DATA_REQUEST_APPLICATION_COLLECTION;

      // Resolve user id asynchronously, then save (mirrors getUser() pattern)
      userIdFromApplicationObjectId(applicationCollection, objectId)
        .onSuccess(userId -> {
          entry.put(USER_ID, userId);
          dbHandler.create(AUDIT_LOG_COLLECTION, entry);
        })
        .onFailure(err -> {
          // Best-effort: log and save with empty user
          log.error("User lookup failed for applicationId {}: {}", objectId, err.getMessage());
          entry.put(USER_ID, "");
          dbHandler.create(AUDIT_LOG_COLLECTION, entry);
        });
      return;
    }

    // Not an application object (or no id) → save as-is (USER_ID may be "")
    dbHandler.create(AUDIT_LOG_COLLECTION, entry);
  }

  /**
   * Build an audit log entry JSON with static fields and optional user id from context.
   * <p>
   * This method does <b>not</b> perform any database calls. If a {@code context} is provided, it
   * extracts and sets {@code USER_ID}; otherwise it sets {@code USER_ID} to an empty string. For
   * the "hub-translation" path (no context + application object), the user id will be resolved
   * <b>before saving</b> inside
   * {@link #insertAuditLog(String, String, String, String, boolean, RoutingContext)}.
   *
   * @param object            The audit object (e.g., DATASET_RECORD, DATA_ACCESS_APPLICATION, ...)
   * @param applicationId     Object id (might be null for the overload that doesn’t supply one)
   * @param action            The audit action
   * @param actionDescription Human-readable description (templated string, already formatted)
   * @param linkable          Whether the log should be considered linkable
   * @param context           Routing context; when present, {@code USER_ID} is taken from it
   * @return JsonObject ready to be persisted (possibly with empty USER_ID)
   */
  private JsonObject createAuditLogEntry(String object, String applicationId, String action,
    String actionDescription, boolean linkable, RoutingContext context) {
    JsonObject auditLogEntry = new JsonObject();

    auditLogEntry.put("date", getCurrentISOTimestamp());
    auditLogEntry.put(OBJECT, object);

    if (StringUtils.isNotEmpty(applicationId)) {
      auditLogEntry.put(OBJECT_ID, applicationId);
    }

    auditLogEntry.put(ACTION, action);
    auditLogEntry.put(ACTION_DESCRIPTION, actionDescription);
    auditLogEntry.put("linkable", linkable);

    // If we have request context, we already know the user id; otherwise leave it empty for now.
    if (context != null) {
      auditLogEntry.put(USER_ID, Context.getEcasId(context));
    } else {
      auditLogEntry.put(USER_ID, "");
    }

    return auditLogEntry;
  }

  public String prepareAuditLogActionDescription(String template, Object... values) {
    StringBuilder formattedTemplate = new StringBuilder();
    Matcher matcher = Pattern.compile("\\{}").matcher(template);
    int index = 0;

    while (matcher.find()) {
      matcher.appendReplacement(formattedTemplate, "{" + index++ + "}");
    }
    matcher.appendTail(formattedTemplate);

    return MessageFormat.format(formattedTemplate.toString(), values);
  }

  public static String getCurrentISOTimestamp() {
    return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
  }

  public String extractDistributionIds(String jsonArrayString) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(jsonArrayString);

      // Stream through the array and extract "distribution_id" values
      return StreamSupport.stream(rootNode.spliterator(), false)
        .map(node -> node.get("distribution_id").asText())
        .collect(Collectors.joining(", "));

    } catch (Exception e) {
      log.error("Exception was thrown inside: extractDistributionIds, with error message: {}",
        e.getMessage());
      return ""; // Return empty string in case of failure
    }
  }

  public void submitDatasetAuditLog(@NotNull RoutingContext context) {

    JsonObject dispatcherAuditLog = context.body().asJsonObject();

    String datasetId = dispatcherAuditLog.getString("datasetId");
    String action = dispatcherAuditLog.getString("datasetAction");
    String ncp = dispatcherAuditLog.getString("ncp");
    String auditLogActionDescription;
    boolean linkable = true;

    switch (action) {
      case "create":
        action = AuditLogAction.DATASET_RECORD_CREATION.toString();
        auditLogActionDescription = AuditLogActionDescription.DATASET_RECORD_CREATION.toString();
        break;

      case "update":
        action = AuditLogAction.DATASET_RECORD_UPDATE.toString();
        auditLogActionDescription = AuditLogActionDescription.DATASET_RECORD_UPDATE.toString();
        break;

      //case "delete":
      default:
        action = AuditLogAction.DATASET_RECORD_DELETION.toString();
        auditLogActionDescription = AuditLogActionDescription.DATASET_RECORD_DELETION.toString();
        linkable = false;
        dbHandler.updateLinkableFields(AUDIT_LOG_COLLECTION, datasetId);
        break;
    }

    String auditLogActionDescriptionMessage = prepareAuditLogActionDescription(
      auditLogActionDescription, datasetId, ncp);

    insertAuditLog(AuditLogObject.DATASET_RECORD.toString(), datasetId, action,
      auditLogActionDescriptionMessage, linkable, null);

    setUpMessageContextResponse(context, HttpResponseStatus.OK,
      "Audit Log record inserted successfully");
  }

  public void searchAuditLogs(@NotNull RoutingContext context) {

    Integer page = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");
    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      page = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = page;
    Integer finalPageLimit = pageLimit;

    getQueryOptions(context).onSuccess(queryOptions -> {
      log.debug("{}Retrieved query options ", auditLogManagerClassName);
      super.getAuditLogs(context, AUDIT_LOG_COLLECTION, finalPage, finalPageLimit,
        StringUtils.isNotEmpty(sortByString) ? sortByString : null,
        StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : null, queryOptions);
    }).onFailure(err -> {
      log.error("{}Error building query options, will proceed without them",
        auditLogManagerClassName);
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Could not construct query options");
    });
  }

  private Future<JsonObject> getQueryOptions(@NotNull RoutingContext context) {

    Promise<JsonObject> promise = Promise.promise();

    JsonObject searchFields = context.body().asJsonObject();

    JsonObject query = new JsonObject();

    String objectId = searchFields.getString(OBJECT_ID);
    String logDateFrom = searchFields.getString("logDateFrom");
    String logDateTo = searchFields.getString("logDateTo");
    JsonArray objectList = searchFields.getJsonArray(OBJECT);
    String action = searchFields.getString(ACTION);
    String actionDescription = searchFields.getString(ACTION_DESCRIPTION);

    if (objectId != null && !objectId.isEmpty()) {
      query.put(OBJECT_ID, new JsonObject().put(REGEX, objectId).put(OPTIONS, "i"));
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    if (logDateFrom != null && !logDateFrom.isEmpty()) {
      String fromDate = LocalDate.parse(logDateFrom, formatter).format(formatter) + " 00:00:00";
      query.put("date", new JsonObject().put("$gte", fromDate));
    }

    if (logDateTo != null && !logDateTo.isEmpty()) {
      String toDate = LocalDate.parse(logDateTo, formatter).format(formatter) + " 23:59:59";
      if (query.containsKey("date")) {
        query.getJsonObject("date", new JsonObject()).put("$lte", toDate);
      } else {
        query.put("date", new JsonObject().put("$lte", toDate));
      }
    }

    if (objectList != null && !objectList.isEmpty()) {
      query.put(OBJECT, new JsonObject().put("$in", objectList));
    }
    if (action != null && !action.isEmpty()) {
      query.put(ACTION, new JsonObject().put(REGEX, action).put(OPTIONS, "i"));
    }
    if (actionDescription != null && !actionDescription.isEmpty()) {
      query.put(ACTION_DESCRIPTION,
        new JsonObject().put(REGEX, actionDescription).put(OPTIONS, "i"));
    }

    //Search Based on Ecas Id
    String ecasId = searchFields.getString(ECAS_ID);
    String firstName = searchFields.getString("firstName");
    String lastName = searchFields.getString("lastName");

    authClient.getAllUsers(context, false)
      .onSuccess(users -> {

        query.put("users", users);

        JsonArray ecasList = new JsonArray();

        boolean hasEcasId = StringUtils.isNotEmpty(ecasId);
        boolean hasFirstName = StringUtils.isNotEmpty(firstName);
        boolean hasLastName = StringUtils.isNotEmpty(lastName);

        int criteriaCount = (hasEcasId ? 1 : 0) + (hasFirstName ? 1 : 0) + (hasLastName ? 1 : 0);

        for (Object userObj : users) {
          JsonObject user = (JsonObject) userObj;

          boolean matches = hasEcasId || hasFirstName || hasLastName;

          if (matches) {
            if (hasEcasId && !Strings.CI.contains(user.getString("ecasId"), ecasId)) {
              matches = false;
            }
            if (hasFirstName && !Strings.CI.contains(user.getString("firstName"),
              firstName)) {
              matches = false;
            }

            if (hasLastName && !Strings.CI.contains(user.getString("lastName"),
              lastName)) {
              matches = false;
            }

          }

          if (matches) {
            ecasList.add(user.getString("ecasId"));
          }
        }

        if (ecasList.isEmpty()) {
          if (criteriaCount >= 1) {
            query.put(USER_ID, new JsonObject().put("$in", new JsonArray().add("NO_RESULT")));
          }
        } else {
          query.put(USER_ID, new JsonObject().put("$in", ecasList));
        }

        promise.complete(query);
      }).onFailure(err -> promise.fail("Could not get all users"));

    return promise.future();
  }

  /**
   * Resolve the application's owner userId from the given application collection and application
   * id.
   * <p>
   * This method is non-blocking and returns a Future that succeeds with the user id (or empty
   * string if not found). On failure, we log and succeed with an empty string to keep audit logging
   * best-effort.
   *
   * @param applicationCollection The Mongo collection to search (e.g.,
   *                              DATA_ACCESS_APPLICATION_COLLECTION)
   * @param applicationId         The application document id
   * @return Future that completes with the resolved user id or an empty string if not found/failing
   */
  private Future<String> userIdFromApplicationObjectId(String applicationCollection,
    String applicationId) {
    return dbHandler.findById(applicationCollection, applicationId)
      .map(json -> json != null ? json.getString(USER_ID_PARAM, "") : "")
      .recover(err -> {
        log.error("Could not retrieve userId for applicationId {}: {}", applicationId,
          err.getMessage());
        return Future.succeededFuture(""); // best-effort policy: save with empty user
      });
  }

}
