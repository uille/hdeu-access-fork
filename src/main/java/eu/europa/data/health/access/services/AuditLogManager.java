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
import static eu.europa.data.health.access.Constants.ECAS_ID;
import static eu.europa.data.health.access.Constants.OBJECT;
import static eu.europa.data.health.access.Constants.OBJECT_ID;
import static eu.europa.data.health.access.Constants.OPTIONS;
import static eu.europa.data.health.access.Constants.REGEX;
import static eu.europa.data.health.access.Constants.USER_ID;

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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogManager extends AbstractManager{

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String auditLogManagerClassName = this.getClass().getSimpleName() + " - ";

  private final DBHandler dbHandler;

  private final AuthClient authClient;

  public AuditLogManager(DBHandler dbHandler, AuthClient authClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.authClient = authClient;
  }

  protected void insertAuditLog(String object, String action, String actionDescription, RoutingContext context) {

    JsonObject auditLogEntry = createAuditLogEntry(object, null, action, actionDescription, false, context);

    dbHandler.create(AUDIT_LOG_COLLECTION, auditLogEntry);
  }

  protected void insertAuditLog(String object, String objectId, String action, String actionDescription, boolean linkable, RoutingContext context) {

    JsonObject auditLogEntry = createAuditLogEntry(object, objectId, action, actionDescription, linkable, context);

    dbHandler.create(AUDIT_LOG_COLLECTION, auditLogEntry);
  }

  private JsonObject createAuditLogEntry(String object, String applicationId, String action, String actionDescription, boolean linkable, RoutingContext context) {

    JsonObject auditLogEntry = new JsonObject();

    auditLogEntry.put("date", getCurrentISOTimestamp());
    auditLogEntry.put(OBJECT, object);
    if(StringUtils.isNotEmpty(applicationId))
      auditLogEntry.put(OBJECT_ID, applicationId);
    auditLogEntry.put(ACTION, action);
    auditLogEntry.put(ACTION_DESCRIPTION, actionDescription);
    if(null != context)
      auditLogEntry.put(USER_ID, Context.getEcasId(context));
    auditLogEntry.put("linkable", linkable);

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
      log.error("Exception was thrown inside: extractDistributionIds, with error message: {}", e.getMessage());
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
        action = AuditLogAction.DATASET_RECORD_CREATION.getString();
        auditLogActionDescription = AuditLogActionDescription.DATASET_RECORD_CREATION.getString();
        break;

      case "update":
        action = AuditLogAction.DATASET_RECORD_UPDATE.getString();
        auditLogActionDescription = AuditLogActionDescription.DATASET_RECORD_UPDATE.getString();
        break;

      //case "delete":
      default:
        action = AuditLogAction.DATASET_RECORD_DELETION.getString();
        auditLogActionDescription = AuditLogActionDescription.DATASET_RECORD_DELETION.getString();
        linkable = false;
        dbHandler.updateLinkableFields(AUDIT_LOG_COLLECTION, datasetId);
        break;
    }

    String auditLogActionDescriptionMessage = prepareAuditLogActionDescription(
      auditLogActionDescription, datasetId, ncp);

    insertAuditLog(AuditLogObject.DATASET_RECORD.getString(), datasetId, action,
      auditLogActionDescriptionMessage, linkable, null);

    setUpMessageContextResponse(context, HttpResponseStatus.OK,
      "Audit Log record inserted successfully");
  }

  public void searchAuditLogs(@NotNull RoutingContext context){

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
      log.info("{}Retrieved query options ", auditLogManagerClassName);
      super.getAuditLogs(context, finalPage, finalPageLimit,
        StringUtils.isNotEmpty(sortByString) ? sortByString : null,
        StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : null, queryOptions);
    }).onFailure(err -> {
      log.error("{}Error building query options, will proceed without them", auditLogManagerClassName);
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
      if(query.containsKey("date")){
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

      authClient.getAllUsers(context)
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

            if(matches){
              if(hasEcasId){
                if(!StringUtils.containsIgnoreCase(user.getString("ecasId"), ecasId)){
                  matches = false;
                }
              }
              if(hasFirstName){
                if(!StringUtils.containsIgnoreCase(user.getString("firstName"), firstName)){

                  matches = false;
                }
              }
              if(hasLastName){
                if(!StringUtils.containsIgnoreCase(user.getString("lastName"), lastName)){
                  matches = false;
                }
              }
            }

            if (matches) {
              ecasList.add(user.getString("ecasId"));
            }
          }


          if(ecasList.isEmpty()){
            if(criteriaCount >= 1){
              query.put(USER_ID, new JsonObject().put("$in", new JsonArray().add("NO_RESULT")));
            }
          } else {
            query.put(USER_ID, new JsonObject().put("$in", ecasList));
          }

          promise.complete(query);
        }).onFailure(err -> promise.fail("Could not get all users"));

    return promise.future();
  }

}
