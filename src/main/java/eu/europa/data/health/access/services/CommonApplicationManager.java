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

import static eu.europa.data.health.access.ApplicationStatus.DRAFT;
import static eu.europa.data.health.access.ApplicationStatus.ON_GOING;
import static eu.europa.data.health.access.Constants.APPLICATIONID;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_WITH_ID;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.BASKET_COLLECTION;
import static eu.europa.data.health.access.Constants.CATALOG_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_BUSINESS_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_ORGANISATION_NAME;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.CONTENT_DISPOSITION;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE;
import static eu.europa.data.health.access.Constants.DATE_ADDED;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_DISPATCHED;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.DATE_SUBMITTED;
import static eu.europa.data.health.access.Constants.DELETION_CAN_NOT_BE_EXECUTED;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.ERROR_CHECKING_DOCUMENT_EXISTENCE;
import static eu.europa.data.health.access.Constants.FILENAME_ASTERISK_EQUALS;
import static eu.europa.data.health.access.Constants.FILE_ID;
import static eu.europa.data.health.access.Constants.FILE_NAME;
import static eu.europa.data.health.access.Constants.FORM;
import static eu.europa.data.health.access.Constants.FORM_TRANSLATIONS;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_EMAIL;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_NAME;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.INPUT_LANGUAGE;
import static eu.europa.data.health.access.Constants.INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NOT_FOUND_FOR_USER_WITH_ID;
import static eu.europa.data.health.access.Constants.OPERATION;
import static eu.europa.data.health.access.Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PROVIDED_APPLICATION_ID;
import static eu.europa.data.health.access.Constants.PUBLISHER;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SECTION;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_NUMBER;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.STATUS_HISTORY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.VERSION;
import static eu.europa.data.health.access.Constants.WAS_NOT_FOUND;
import static eu.europa.data.health.access.Constants.WAS_NOT_FOUND_IN_THE_DATABASE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.CatalogStatus;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.DatasetStatus;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.PermitStatus;
import eu.europa.data.health.access.TranslationStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.exceptions.JsonParsingException;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.hub_translation_client.TranslationResult;
import eu.europa.data.health.access.utils.ApplicationFormUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class CommonApplicationManager extends AbstractManager {

  private static final String ADDITIONAL_INFORMATION = "additionalInformation";
  private static final String COMMENT = "comment";
  private static final String COUNTRY = "country";
  public static final String FULL_NAME = "fullName";
  public static final String ADDRESS = "address";
  public static final String PHONE = "phone";
  public static final String EMAIL = "email";
  public static final String OPERATOR_IDENTIFIER = "operatorIdentifier";
  public static final String NAME_OF_THE_ORGANISATION = "nameOfTheOrganisation";
  public static final String BUSINESS_IDENTIFIER_ORGANIZATION = "businessIdentifierOrganization";
  public static final String PEOPLE_WHO_WILL_BE_PROCESSING_THE_DATA = "peopleWhoWillBeProcessingTheData";
  public static final String TARGET_LANGUAGES = "targetLanguages";
  public static final String TRANSLATION = "translation";
  public static final String LOGGED_FAILURE = "logged_failure";
  public static final String STATUS_STATUS = "status.status";
  public static final String TRANSLATION_ERROR_TYPE = "translation.errorType";
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final List<String> ERROR_CODES_REQUIRED = Arrays.asList("NotNull",
    "NotBlank",
    "NotEmpty",
    "AssertTrue",
    "ListContainsSpecificValue",
    "ListNotContainsSpecificValue",
    "ListContainsOneMoreValueThanASpecificValue",
    "RequiredFieldBasedOnObjectValue",
    "RequiredFieldBasedOnTwoObjectValue",
    "RequiredFieldBasedOnAndOrObjectValue",
    "ListObjectFieldsNotNull",
    "requiredOneFieldSelected",
    "RequiredFieldBasedOnValue",
    "ListContainsOneMoreValueExpectASpecificValue",
    "NotNullVariables");

  private final DBHandler dbHandler;
  private final BasketManager basketManager;
  private final MongoClient mongoClient;
  private final Vertx vertx;
  private final HubStoreClient hubStoreClient;
  private final AuditLogManager auditLogManager;
  private final HubTranslationClient hubTranslationClient;
  private final HubSearchClient hubSearchClient;
  private final AuthClient authClient;

  protected CommonApplicationManager(DBHandler dbHandler, BasketManager basketManager,
    MongoClient mongoClient, Vertx vertx, HubStoreClient hubStoreClient,
    AuditLogManager auditLogManager,
    HubTranslationClient hubTranslationClient, HubSearchClient hubSearchClient,
    AuthClient authClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.basketManager = basketManager;
    this.mongoClient = mongoClient;
    this.vertx = vertx;
    this.hubStoreClient = hubStoreClient;
    this.auditLogManager = auditLogManager;
    this.hubTranslationClient = hubTranslationClient;
    this.hubSearchClient = hubSearchClient;
    this.authClient = authClient;
  }

  protected CommonApplicationManager(DBHandler dbHandler, MongoClient mongoClient, AuditLogManager auditLogManager) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.mongoClient = mongoClient;
    this.basketManager = null;
    this.vertx = null;
    this.hubStoreClient = null;
    this.auditLogManager = auditLogManager;
    this.hubTranslationClient = null;
    this.hubSearchClient = null;
    this.authClient = null;
  }

  protected void updateApplicationStatus(JsonObject mongoApplication, JsonObject status,
    RoutingContext context) {
    // Get current application status.
    JsonObject currentStatus = mongoApplication.getJsonObject(STATUS);
    // Update the status_history for the application.
    JsonArray previousStatuses = new JsonArray();
    if (currentStatus.getJsonArray(STATUS_HISTORY) != null) {
      previousStatuses = currentStatus.getJsonArray(STATUS_HISTORY);
      currentStatus.remove(STATUS_HISTORY);
    }
    // If the status to go is the Submitted keep the date submitted.
    ApplicationStatus receivedStatus = ApplicationStatus.fromStatus(
      status.getString(STATUS));
    if (ON_GOING.equals(receivedStatus)) {
      mongoApplication.put(DATE_SUBMITTED, Instant.now());
    }
    previousStatuses.add(currentStatus);
    // Set the status_history to the new status.
    status.put(STATUS_HISTORY, previousStatuses);
    status.put(DATE, Instant.now());
    if (context != null && Context.getEcasId(context) != null) {
      status.put(USER_ID, Context.getEcasId(context));
    } else {
      status.put(USER_ID, "system");
    }
    mongoApplication.put(STATUS, status);
  }

  protected void createApplication(RoutingContext context, String collectionName) {
    // Retrieve the request body
    JsonObject requestBody = context.body().asJsonObject();
    // Define the fields to be stored
    requestBody.put(DATE_CREATED, Instant.now());
    requestBody.put(DATE_MODIFIED, Instant.now());
    requestBody.put(USER_ID, Context.getEcasId(context));
    JsonObject status = new JsonObject();
    status.put(STATUS, DRAFT);
    status.put(DATE, Instant.now());
    status.put(USER_ID, Context.getEcasId(context));
    requestBody.put(STATUS, status);
    requestBody.put(VERSION, 1);
    JsonArray datasets = requestBody.getJsonArray(DATASETS);

    if (datasets != null && !datasets.isEmpty()) {

      requestBody.put("catalogs", createCatalogs(datasets));

      List<Future<Void>> futures = new ArrayList<>();
      for (Object datasetObj : datasets) {

        JsonObject dataset = (JsonObject) datasetObj;
        dataset.put(DATE_ADDED, Instant.now());

        JsonObject publisherRetrieveQuery = new JsonObject()
          .put(DATASET_ID, dataset.getString(DATASET_ID))
          .put(USER_ID, Context.getEcasId(context));

        futures.add(
          mongoClient.findOne(BASKET_COLLECTION, publisherRetrieveQuery, null)
            .onSuccess(result -> {
              if (result != null) {
                dataset.put(PUBLISHER,
                  new JsonObject().put("name", result.getString(PUBLISHER)));
              }
            })
            .mapEmpty()
        );

        futures.add(
          hubSearchClient.getDataset(dataset.getString(DATASET_ID))
            .onSuccess(hubSearchResult -> processHubSearchResult(dataset, hubSearchResult))
            .mapEmpty()
        );

      }

      // Wait for all async queries to finish before proceeding
      Future.all(futures).onComplete(ar -> {
        if (ar.succeeded()) {
          super.createEntity(context, collectionName);

          //Audit Log
          String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
            DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
              ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_CREATED.toString()
              : AuditLogActionDescription.DATA_REQUEST_APPLICATION_CREATED.toString(),
            context.body().asJsonObject().getString(ID),
            context.body().asJsonObject().getString(TITLE));
          auditLogManager.insertAuditLog(
            DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
              ? AuditLogObject.DATA_ACCESS_APPLICATION.toString()
              : AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
            context.body().asJsonObject().getString(ID),
            DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
              ? AuditLogAction.DATA_ACCESS_APPLICATION_CREATED.toString()
              : AuditLogAction.DATA_REQUEST_APPLICATION_CREATED.toString(),
            auditLogActionDescription, true, context);

          // For each dataset delete distributions from data basket
          deleteDistributionsFromBasket(datasets, context);
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            ar.cause().getMessage());
        }
      });
    }
  }

  protected void changeCatalogStatus(@NotNull RoutingContext context, String collectionName) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String catalogId = context.pathParam(CATALOG_ID);
    String catalogStatusString = context.body().asJsonObject().getString(STATUS);

    changeCatalogStatus(applicationId, catalogId, catalogStatusString, null, collectionName, context, true);
  }

  protected void changeCatalogStatus(String applicationId, String catalogId, String catalogStatusString, JsonObject statusPayload, String collectionName, @NotNull RoutingContext context, boolean comingFromUI) {

    CatalogStatus targetCatalogStatus = CatalogStatus.fromLabel(catalogStatusString, context);
    ApplicationStatus targetApplicationStatus = targetCatalogStatus.computeApplicationStatus();

    JsonObject applicationStatusObj = new JsonObject().put("status",
      targetApplicationStatus.getStatus());

    JsonObject query = new JsonObject().put(ID, applicationId);
    query.put(ID, applicationId);
    if (comingFromUI){
      query.put(USER_ID, Context.getEcasId(context));
    }

    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {
          JsonObject application = ar.result();

          CatalogStatus currentCatalogStatus = CatalogStatus.fromLabel(getLatestCatalogStatus(application, catalogId), context);
          if(currentCatalogStatus.canChangeTo(targetCatalogStatus, context))
            updateCatalogStatus(application, catalogId, targetCatalogStatus, statusPayload);

          ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(application.getJsonObject("status").getString("status"));
          if(currentApplicationStatus.canChangeTo(targetApplicationStatus, application, context)){
            updateApplicationStatus(application, applicationStatusObj, context);
            String auditLogActionDescriptionForApplicationUpdate = createAuditLogActionDescriptionForApplicationUpdate(applicationId, application.getString("title"), targetApplicationStatus, collectionName);
            insertAuditLogForApplicationUpdate(auditLogActionDescriptionForApplicationUpdate, collectionName, applicationId, context);
          }

          applySpecialRules(application, targetCatalogStatus);

          replaceApplication(application, collectionName)
            .onSuccess(replaceResult -> {
              setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
              if(comingFromUI)
                sendApplicationToOutgoingApplicationActivities(collectionName, applicationId, targetCatalogStatus, catalogId, null);

              //Audit Log
              String auditLogActionDescription = createAuditLogActionDescriptionForChangeHDABStatus(targetCatalogStatus, collectionName, applicationId, application.getString("title"), catalogId);
              insertAuditLogForHDABUpdate(auditLogActionDescription, collectionName, applicationId, context);

            }).onFailure(fail -> {
              setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                fail.getMessage());
            });
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Application not found");
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Error occurred: " + ar.cause().getMessage());
      }
    });
  }

  private void applySpecialRules(JsonObject application, CatalogStatus targetCatalogStatus){

    if(CatalogStatus.ADDITIONAL_INFORMATION.equals(targetCatalogStatus)){
      application.getJsonObject("translation").put("status", TranslationStatus.FILES_COMPLETED.name());
      application.remove("form_translations");
    }
  }

  public void changePermitStatus(String applicationId, String catalogId, String datasetId,
    String permitStatusString, String reason, String permitFile,
    @NotNull RoutingContext context, boolean comingFromUI) {

    PermitStatus targetPermitStatus = PermitStatus.fromLabel(permitStatusString, false, context);

    JsonObject query = new JsonObject().put(ID, applicationId);
    query.put(ID, applicationId);
    if (comingFromUI) {
      query.put(USER_ID, Context.getEcasId(context));
    }

    mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {
          JsonObject application = ar.result();

          ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(application.getJsonObject("status").getString("status"));
          currentApplicationStatus.validatePermitChange(context);
          CatalogStatus currentCatalogStatus = CatalogStatus.fromLabel(getLatestCatalogStatus(application, catalogId), context);
          currentCatalogStatus.validatePermitChange(context);
          String latestCurrentDatasetStatus = getLatestDatasetStatus(application, catalogId, datasetId, context);
          DatasetStatus currentDatasetStatus = DatasetStatus.fromLabel(latestCurrentDatasetStatus, context);
          currentDatasetStatus.validatePermitChange(context);

          String latestPermitStatusString = getLatestPermitStatus(application, catalogId, datasetId, context);
          if(StringUtils.isNotEmpty(latestPermitStatusString)){
            PermitStatus currentPermitStatus = PermitStatus.fromLabel(latestPermitStatusString, true, context);
            currentPermitStatus.canChangeTo(targetPermitStatus, context);
          } else {
            targetPermitStatus.validateInitialization(context);
          }

          updatePermitStatus(application, catalogId, datasetId, targetPermitStatus, permitFile, reason);

          replaceApplication(application, DATA_ACCESS_APPLICATION_COLLECTION)
            .onSuccess(replaceResult -> {
            setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
            if(comingFromUI)
              sendApplicationToOutgoingApplicationActivities(DATA_ACCESS_APPLICATION_COLLECTION, applicationId,
                currentCatalogStatus, catalogId, targetPermitStatus.getLabel(), datasetId, reason, OperationStatus.PERMIT);

            //Audit Log
            String auditLogActionDescription = createAuditLogActionDescriptionForChangePermitStatus(applicationId, application.getString("title"), targetPermitStatus, catalogId, datasetId);
            insertAuditLogForPermitUpdate(auditLogActionDescription, applicationId, context);

          }).onFailure(fail -> {
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              fail.getMessage());
          });
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Data Access Application not found");
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Error occurred: " + ar.cause().getMessage());
      }
    });
  }

  protected String getLatestCatalogStatus(JsonObject mongoApplication, String catalogId) {
    JsonArray catalogs = mongoApplication.getJsonArray("catalogs");
    if (catalogs == null) {
      return null;
    }

    for (int i = 0; i < catalogs.size(); i++) {
      JsonObject catalog = catalogs.getJsonObject(i);

      // Skip if this is not the catalog we want
      if (!catalogId.equals(catalog.getString("catalog_id"))) {
        continue;
      }

      JsonArray statuses = catalog.getJsonArray("statuses");
      if (statuses == null) {
        return null;
      }

      for (int j = 0; j < statuses.size(); j++) {
        JsonObject statusObj = statuses.getJsonObject(j);

        if (statusObj.getInteger("latest", 0) == 1) {
          return statusObj.getString("status");
        }
      }

      // Catalog found, but no 'latest = 1'
      return null;
    }

    // Catalog ID not found
    return null;
  }

  private String getLatestPermitStatus(JsonObject mongoApplication, String catalogId, String datasetId, @NotNull RoutingContext context) {
    return getLatestStatus("permits", "permit_status", mongoApplication, catalogId, datasetId, context);
  }
  private String getLatestDatasetStatus(JsonObject mongoApplication, String catalogId, String datasetId, @NotNull RoutingContext context) {
    return getLatestStatus("decisions", "datasetStatus", mongoApplication, catalogId, datasetId, context);
  }

  private String getLatestStatus(String fieldName, String fieldStatusName, JsonObject mongoApplication, String catalogId, String datasetId, @NotNull RoutingContext context) {

    JsonArray datasets = mongoApplication.getJsonArray("datasets");
    if (datasets == null || datasets.isEmpty()) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Application's datasets are null/empty");
      throw new RuntimeException("Application's datasets are null/empty");
    }

    for (int i = 0; i < datasets.size(); i++) {
      JsonObject dataset = datasets.getJsonObject(i);

      // Check both dataset_id and catalog_id
      if (!datasetId.equals(dataset.getString("dataset_id"))) {
        continue;
      }
      if (!catalogId.equals(dataset.getString("catalog_id"))) {
        continue;
      }

      JsonArray decisions = dataset.getJsonArray(fieldName);
      if (decisions == null) {
        return null;
      }

      for (int j = 0; j < decisions.size(); j++) {
        JsonObject decision = decisions.getJsonObject(j);

        if (decision.getInteger("latest", 0) == 1) {
          return decision.getString(fieldStatusName);
        }
      }

      // Dataset matched but no "latest" decision found
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "No latest decision found");
      throw new RuntimeException("No latest decision found");
    }

    // No dataset matching both IDs found
    setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
      "No dataset found matching both catalog and dataset id");
    throw new RuntimeException("No dataset found matching both catalog and dataset id");
  }

  protected void changeDatasetStatus(@NotNull RoutingContext context, String collectionName) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String catalogId = context.pathParam(CATALOG_ID);
    String datasetId = context.pathParam(DATASET_ID);
    String datasetStatusString = context.body().asJsonObject().getString(STATUS);

    changeDatasetStatus(applicationId, catalogId, datasetId, datasetStatusString, null, collectionName, context, true);
  }

  protected void changeDatasetStatus(String applicationId, String catalogId, String datasetId, String datasetStatusString, String decisionFileBase64, String collectionName, @NotNull RoutingContext context, boolean comingFromUI) {

    DatasetStatus targetDatasetStatus = DatasetStatus.fromLabel(datasetStatusString, context);
    CatalogStatus targetCatalogStatus = targetDatasetStatus.computeCatalogStatus();
    ApplicationStatus targetApplicationStatus = targetCatalogStatus.computeApplicationStatus();

    JsonObject applicationStatusObj = new JsonObject().put("status",
      targetApplicationStatus);

    JsonObject query = new JsonObject().put(ID, applicationId);
    query.put(ID, applicationId);
    if (comingFromUI){
      query.put(USER_ID, Context.getEcasId(context));
    }

    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {
          JsonObject application = ar.result();

          String latestCurrentDatasetStatus = getLatestDatasetStatus(application, catalogId, datasetId, context);
          if(StringUtils.isEmpty(latestCurrentDatasetStatus)) {

            if(targetDatasetStatus.validateInitialization(context))
              updateDatasetStatus(application, catalogId, datasetId, targetDatasetStatus, decisionFileBase64);

            CatalogStatus currentCatalogStatus = CatalogStatus.fromLabel(getLatestCatalogStatus(application, catalogId), context);
            if(currentCatalogStatus.canChangeTo(targetCatalogStatus, context)){
              updateCatalogStatus(application, catalogId, targetCatalogStatus);
              String auditLogActionDescriptionForHDABAndDecisionUpdate = createAuditLogActionDescriptionForChangeHDABAndDecisionStatus(targetCatalogStatus, collectionName, applicationId,  application.getString("title"), catalogId, latestCurrentDatasetStatus, targetDatasetStatus, datasetId);
              insertAuditLogForDatasetUpdate(auditLogActionDescriptionForHDABAndDecisionUpdate, collectionName, applicationId, context);
            }

            ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(application.getJsonObject("status").getString("status"));
            if(currentApplicationStatus.canChangeTo(targetApplicationStatus, application, context)){
              updateApplicationStatus(application, applicationStatusObj, context);
              String auditLogActionDescriptionForApplicationUpdate = createAuditLogActionDescriptionForApplicationUpdate(applicationId, application.getString("title"), targetApplicationStatus, collectionName);
              insertAuditLogForApplicationUpdate(auditLogActionDescriptionForApplicationUpdate, collectionName, applicationId, context);
            }

          } else {

            DatasetStatus currentDatasetStatus = DatasetStatus.fromLabel(latestCurrentDatasetStatus, context);
            if(currentDatasetStatus.canChangeTo(targetDatasetStatus, collectionName, context)){
              updateDatasetStatus(application, catalogId, datasetId, targetDatasetStatus, decisionFileBase64);
              String auditLogActionDescriptionForHDABAndDecisionUpdate = createAuditLogActionDescriptionForChangeHDABAndDecisionStatus(targetCatalogStatus, collectionName, applicationId,  application.getString("title"), catalogId, latestCurrentDatasetStatus, targetDatasetStatus, datasetId);
              insertAuditLogForDatasetUpdate(auditLogActionDescriptionForHDABAndDecisionUpdate, collectionName, applicationId, context);
            }

            ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(application.getJsonObject("status").getString("status"));
            if(currentApplicationStatus.canChangeTo(targetApplicationStatus, application, context)){
              updateApplicationStatus(application, applicationStatusObj, context);
              String auditLogActionDescriptionForApplicationUpdate = createAuditLogActionDescriptionForApplicationUpdate(applicationId, application.getString("title"), targetApplicationStatus, collectionName);
              insertAuditLogForApplicationUpdate(auditLogActionDescriptionForApplicationUpdate, collectionName, applicationId, context);
            }
          }

          replaceApplication(application, collectionName)
            .onSuccess(replaceResult -> {
              setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
              if(comingFromUI)
                sendApplicationToOutgoingApplicationActivities(collectionName, applicationId,
                  targetCatalogStatus, catalogId, targetDatasetStatus.getLabel(), datasetId, null, OperationStatus.DECISION);

            }).onFailure(fail -> {
              setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                fail.getMessage());
            });
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "Application not found");
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Error occurred: " + ar.cause().getMessage());
      }
    });
  }

  private void updateDatasetStatus(JsonObject application, String catalogId, String datasetId,
    DatasetStatus datasetStatus, String decisionFileBase64) {
    JsonArray datasets = application.getJsonArray("datasets");
    updateDatasetStatus(datasets, catalogId, datasetId, datasetStatus, decisionFileBase64);
  }

  protected void updateDatasetStatus(JsonArray datasets, String catalogId, String datasetId,
    DatasetStatus datasetStatus, String decisionFileBase64) {

    for (Object datasetObj : datasets) {

      JsonObject dataset = (JsonObject) datasetObj;
      String mongoCatalogId = dataset.getString("catalog_id");
      String mongoDatasetId = dataset.getString("dataset_id");
      JsonArray decisions = dataset.getJsonArray("decisions");

      if (catalogId.equals(mongoCatalogId) && datasetId.equals(mongoDatasetId)) {

        if (decisions == null) {
          decisions = new JsonArray();
        } else {
          decisions.forEach(item -> {
            JsonObject decision = (JsonObject) item;
            decision.put("latest", 0);
          });
        }

        JsonObject decision = new JsonObject();
        decision.put("datasetStatus", datasetStatus);
        if(StringUtils.isNotEmpty(decisionFileBase64))
          decision.put("decision_file", decisionFileBase64);
        decision.put("decision_date_received", Instant.now());
        decision.put("latest", 1);

        decisions.add(decision);
        dataset.put("decisions", decisions);
      }
    }
  }

  protected void updateCatalogStatus(JsonObject application) {
    application.put("catalogs", computeCatalogs(null, null, CatalogStatus.SENT, null, application));
  }

  protected void updateCatalogStatus(JsonObject application, String catalogId, CatalogStatus catalogStatus) {
    application.put("catalogs", computeCatalogs(null, catalogId, catalogStatus, null, application));
  }

  protected void updateCatalogStatus(JsonObject application, String catalogId, CatalogStatus catalogStatus, JsonObject statusPayload) {
    application.put("catalogs", computeCatalogs(null, catalogId, catalogStatus, statusPayload, application));
  }

  private JsonArray createCatalogs(JsonArray datasets) {
    return computeCatalogs(datasets, null, CatalogStatus.DRAFT, null, null);
  }

  private JsonArray computeCatalogs(JsonArray datasets, String catalogId, CatalogStatus status, JsonObject statusPayload,
    JsonObject application) {

    JsonArray catalogs = new JsonArray();
    Set<String> catalogIds = new LinkedHashSet<>();

    //
    // CASE 1 — datasets NOT NULL → new application (initial creation) will create the catalogs
    //
    if (datasets != null && !datasets.isEmpty()) {

      for (Object datasetObj : datasets) {
        JsonObject dataset = (JsonObject) datasetObj;
        String datasetCatalogId = dataset.getString(CATALOG_ID);
        if (StringUtils.isNotEmpty(datasetCatalogId)) {
          catalogIds.add(datasetCatalogId);
        }
      }

      JsonObject newStatusObj = new JsonObject()
        .put("status", status.getLabel())
        .put("date_received", Instant.now())
        .put("latest", 1);

      catalogIds.forEach(id -> {
        JsonArray statuses = new JsonArray().add(newStatusObj.copy());
        catalogs.add(new JsonObject()
          .put("catalog_id", id)
          .put("statuses", statuses));
      });

      return catalogs;

    //
    // CASE 2 — datasets NULL && catalogId NULL → change status for all catalogs ex. CatalogStatus.SUBMITTED
    //
    } else if(datasets == null && StringUtils.isEmpty(catalogId)) {

      JsonArray mongoDatasets = application.getJsonArray(DATASETS);
      if (mongoDatasets != null && !mongoDatasets.isEmpty()) {
        for (Object datasetObj : mongoDatasets) {
          JsonObject dataset = (JsonObject) datasetObj;
          if (StringUtils.isNotEmpty(dataset.getString(CATALOG_ID))) {
            catalogIds.add(dataset.getString(CATALOG_ID));
          }
        }
      } else {
        log.error("Application with id: {} has no datasets", application.getString("_id"));
        return new JsonArray();
      }

      JsonArray existingCatalogs = application.getJsonArray("catalogs", new JsonArray());
      Map<String, JsonObject> catalogMap = new LinkedHashMap<>();

      // index existing catalogs
      for (Object obj : existingCatalogs) {
        JsonObject catalog = (JsonObject) obj;
        catalogMap.put(catalog.getString("catalog_id"), catalog.copy());
      }

      // update/create catalogs
      for (String catId : catalogIds) {

        JsonObject catalog = catalogMap.getOrDefault(catId, new JsonObject()
          .put("catalog_id", catId)
          .put("statuses", new JsonArray()));

        JsonArray statuses = catalog.getJsonArray("statuses");

        // Reset existing statuses: latest=0
        for (int i = 0; i < statuses.size(); i++) {
          statuses.getJsonObject(i).put("latest", 0);
        }

        // Add new status with latest=1
        JsonObject updatedStatus = new JsonObject()
          .put("status", status.getLabel())
          .put("date_received", Instant.now())
          .put("latest", 1);

        statuses.add(updatedStatus);

        catalog.put("statuses", statuses);
        catalogMap.put(catId, catalog);
      }

      catalogMap.values().forEach(catalogs::add);

      return catalogs;


    //
    // CASE 3 — datasets NULL && catalogId NOT NULL → update existing application catalog
    //
    } else if(datasets == null && StringUtils.isNotEmpty(catalogId)) {

      JsonArray existingCatalogs = application.getJsonArray("catalogs", new JsonArray());
      JsonArray updatedCatalogs = new JsonArray(); // final result

      for (Object obj : existingCatalogs) {
        JsonObject catalog = ((JsonObject) obj).copy();
        String cid = catalog.getString("catalog_id");

        // if this is NOT the catalog we want, simply keep it untouched
        if (!catalogId.equals(cid)) {
          updatedCatalogs.add(catalog);
          continue;
        }

        // --- This IS the catalog to update ---
        JsonArray statuses = catalog.getJsonArray("statuses", new JsonArray());

        // reset latest=0 for all previous statuses
        for (int i = 0; i < statuses.size(); i++) {
          statuses.getJsonObject(i).put("latest", 0);
        }

        // build new status
        JsonObject updatedStatus = new JsonObject()
          .put("status", status.getLabel())
          .put("date_received", Instant.now())
          .put("latest", 1);

        if (statusPayload != null && !statusPayload.isEmpty()) {
          updatedStatus.put("statusPayload", statusPayload);

          // Handle ADDITIONAL_INFORMATION status
          if (CatalogStatus.ADDITIONAL_INFORMATION.equals(status)) {
            application.put(
                ADDITIONAL_INFORMATION,
                computeAdditionalInformation(application, catalogId, statusPayload)
            );
          }
        }

        statuses.add(updatedStatus);
        catalog.put("statuses", statuses);

        updatedCatalogs.add(catalog);
      }

      // return the full updated list, including all catalogs
      return updatedCatalogs;
    } else {
      log.error("computeCatalogs operation failed for catalog id: {}", catalogId);
      return new JsonArray();
    }
  }

  /**
   * Computes the additionalInformation entry for a given catalog and updates the application's
   * additionalInformation array accordingly.
   *
   * @param application   The application JsonObject.
   * @param catalogId     The catalog ID for which to compute additional information.
   * @param statusPayload The status payload containing additional information data.
   * @return An updated JsonArray of additionalInformation for the application.
   */
  @NotNull
  private JsonArray computeAdditionalInformation(
      @NotNull JsonObject application,
      @NotNull String catalogId,
      @NotNull JsonObject statusPayload
  ) {
    // Find the country associated with the catalog to include in additionalInformation
    JsonObject country = application
        .getJsonArray("datasets")
        .stream()
        .map(JsonObject.class::cast)
        .filter(ds -> catalogId.equalsIgnoreCase(ds.getString("catalog_id")))
        .findFirst()
        .orElse(new JsonObject())
        .getJsonObject(COUNTRY, new JsonObject());

    // Extract source and translation arrays from statusPayload
    JsonArray source = statusPayload
        .getJsonObject(ADDITIONAL_INFORMATION, new JsonObject())
        .getJsonArray("source", new JsonArray());
    JsonArray translation = statusPayload
        .getJsonObject(ADDITIONAL_INFORMATION, new JsonObject())
        .getJsonArray(TRANSLATION, new JsonArray());

    // Prepare additionalInformation entry
    JsonObject additionalInformation = new JsonObject()
        .put("catalog_id", catalogId)
        .put(COUNTRY, country)
        .put("formVersion", statusPayload.getString("formVersion"))
        .put("source", expandAdditionalInformation(source))
        .put(TRANSLATION, expandAdditionalInformation(translation));

    // Filter out existing entries for this catalog_id and then add the new one
    return application
        .getJsonArray(ADDITIONAL_INFORMATION, new JsonArray())
        .stream()
        .map(JsonObject.class::cast)
        .filter(info -> !catalogId.equalsIgnoreCase(info.getString("catalog_id")))
        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll)
        .add(additionalInformation);
  }

  /**
   * Expands a flattened JsonArray of additional information into a nested JsonObject.
   *
   * @param additionalInformation The flattened additional information array.
   * @return A nested JsonObject representing the expanded additional information.
   */
  private JsonObject expandAdditionalInformation(@NotNull JsonArray additionalInformation) {
    JsonObject root = new JsonObject();
    additionalInformation
      .stream()
      .map(JsonObject.class::cast)
      .filter(entry -> StringUtils.isNoneEmpty(entry.getString("field"), entry.getString(COMMENT)))
      .forEach(entry -> {
        String field = entry.getString("field");
        String comment = entry.getString(COMMENT);
        String[] parts = field.split("\\.");
        JsonObject current = root;
        boolean skipNext = false;

        for (int i = 0; i < parts.length; i++) {
          if (skipNext) {
            skipNext = false;
            continue;
          }

          String part = parts[i];

          if (i == parts.length - 1) {
            current.put(part, comment); // Last segment → put string value
          } else if (StringUtils.isNumeric(parts[i + 1])) {
            int index = Integer.parseInt(parts[i + 1]);

            // Create or retrieve JsonArray and ensure it has enough elements
            JsonArray array = getOrCreate(current, part, JsonArray.class, JsonArray::new);
            while (array.size() <= index) {
              array.add(new JsonObject());
            }

            current = array.getJsonObject(index); // Move into the object inside the array
            skipNext = true; // Skip the array index segment
          } else {
            current = getOrCreate(current, part, JsonObject.class, JsonObject::new);
          }
        }
      });
    return root;
  }

  /**
   * Retrieves a value from a JsonObject by key, or creates and inserts it if it doesn't exist.
   *
   * @param object   The JsonObject to retrieve from.
   * @param key      The key to look for.
   * @param type     The expected type of the value.
   * @param supplier A supplier to create the value if it doesn't exist.
   * @param <T>      The type of the value.
   * @return The existing or newly created value.
   */
  private <T> T getOrCreate(JsonObject object, String key, Class<T> type, Supplier<T> supplier) {
    Object raw = object.getValue(key);
    if (raw == null) {
      T value = supplier.get();
      object.put(key, value);
      return value;
    }
    return type.cast(raw);
  }

  private void processHubSearchResult(JsonObject dataset, JsonObject hubSearchResult) {
    if (hubSearchResult == null) {
      return;
    }

    JsonObject result = hubSearchResult.getJsonObject("result");
    if (result == null) {
      return;
    }

    // Set dataset title
    JsonObject titles = result.getJsonObject(TITLE);
    if (titles != null) {
      dataset.put(TITLE, titles);
    }

    // Set dataset country
    JsonObject country = result.getJsonObject(COUNTRY);
    if (country != null) {
      if (country.containsKey("id")) {
        Object idValue = country.remove("id");
        country.put("country_id", idValue);
      }
      dataset.put(COUNTRY, country);
    }

    // Set dataset publisher
    JsonObject publisher = result.getJsonObject(PUBLISHER);
    if (publisher != null) {
      dataset.put(PUBLISHER, publisher);
    }

    // Set dataset hdab
    JsonObject hdab = result.getJsonObject("hdab");
    if (hdab != null) {
      dataset.put("hdab", hdab);
    }

    // Set dataset provenance
    JsonObject provenance = result.getJsonObject("provenance");
    if (provenance != null) {
      dataset.put("provenance", provenance);
    }

    // --- Distributions: update titles where IDs match ---
    JsonArray datasetDistributions = dataset.getJsonArray(DISTRIBUTIONS);
    JsonArray hubSearchDistributions = result.getJsonArray(DISTRIBUTIONS);

    if (datasetDistributions != null && !datasetDistributions.isEmpty()
      && hubSearchDistributions != null && !hubSearchDistributions.isEmpty()) {

      // Build a quick lookup map for hub search distributions
      Map<String, JsonObject> distributionMap = hubSearchDistributions.stream()
        .filter(JsonObject.class::isInstance)
        .map(JsonObject.class::cast)
        .collect(Collectors.toMap(
          dist -> dist.getString("id"),
          dist -> dist
        ));

      // Update titles where IDs match
      for (int i = 0; i < datasetDistributions.size(); i++) {
        JsonObject distribution = datasetDistributions.getJsonObject(i);
        String distId = distribution.getString("distribution_id");
        if (distId != null && distributionMap.containsKey(distId)) {
          JsonObject hubDist = distributionMap.get(distId);
          distribution.put(TITLE, hubDist.getJsonObject(TITLE));
        }
      }
    }

    // --- Samples: keep only sample id and variables ---
    // Copies result.distributions_sample into dataset.distributions_sample,
    // but each element contains only: { "id": "<sampleId>", "variables": [...] }
    JsonArray hubSamples = result.getJsonArray("distributions_sample");
    if (hubSamples != null && !hubSamples.isEmpty()) {
      JsonArray reducedSamples = new JsonArray();

      for (int i = 0; i < hubSamples.size(); i++) {
        Object item = hubSamples.getValue(i);
        if (!(item instanceof JsonObject)) {
          continue;
        }
        JsonObject sample = (JsonObject) item;

        String sampleId = sample.getString("id");
        // variables might be absent; keep it only if present (or use empty array if you prefer)
        JsonArray variables = sample.getJsonArray("variables");

        JsonObject out = new JsonObject();
        if (sampleId != null) {
          out.put(DATASET_ID, sampleId);          // keep the sample id
        }
        if (variables != null) {
          out.put("variables", variables);  // keep the variables array
        }

        // Only add if we have at least one of the fields
        if (!out.isEmpty()) {
          reducedSamples.add(out);
        }
      }

      if (!reducedSamples.isEmpty()) {
        dataset.put("distributions_sample", reducedSamples);
      }
    }
  }

  protected void deleteApplicationById(@NotNull RoutingContext context,
    String collectionName) {
    String id = context.pathParam("id");
    // First, retrieve the application
    JsonObject query = new JsonObject().put(ID, id);
    query.put(USER_ID, Context.getEcasId(context));
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        JsonObject document = ar.result();
        if (Objects.nonNull(document)) {

          //Able to delete an application only if status is not SUBMITTED
          if (!ON_GOING.getStatus()
            .equalsIgnoreCase(document.getJsonObject(STATUS).getString(STATUS))) {

            List<String> fileIds = new ArrayList<>();
            JsonObject form = document.getJsonObject("form");
            if (Objects.nonNull(form)) {
              String jsonString = form.toString();
              ObjectMapper objectMapper = new ObjectMapper();
              try {
                JsonNode rootNode = objectMapper.readTree(jsonString);
                extractIds(rootNode, fileIds);
              } catch (JsonProcessingException e) {
                throw new JsonParsingException("Error parsing JSON input", e);
              }
              for (String fileId : fileIds) {
                hubStoreClient.deleteFile(fileId, APPLICATIONID, id);
              }
            }

            super.deleteEntityById(context, collectionName);

            //Audit Log
            dbHandler.updateLinkableFields(AUDIT_LOG_COLLECTION, id);
            String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
              DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
                ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_DELETED.toString()
                : AuditLogActionDescription.DATA_REQUEST_APPLICATION_DELETED.toString(),
              id, document.getString(TITLE));
            auditLogManager.insertAuditLog(
              DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
                ? AuditLogObject.DATA_ACCESS_APPLICATION.toString()
                : AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
              id,
              DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
                ? AuditLogAction.DATA_ACCESS_APPLICATION_DELETED.toString()
                : AuditLogAction.DATA_REQUEST_APPLICATION_DELETED.toString(),
              auditLogActionDescription, false, context);

          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              DELETION_CAN_NOT_BE_EXECUTED);
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            PROVIDED_APPLICATION_ID + id + WAS_NOT_FOUND_IN_THE_DATABASE);
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
          ar.cause().getMessage());
      }
    });
  }

  protected static void extractIds(JsonNode node, List<String> ids) {
    if (node.isObject()) {
      Iterator<String> fieldNames = node.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        JsonNode childNode = node.get(fieldName);
        if (fieldName.equals("id") && childNode.isTextual()) {
          ids.add(childNode.asText());
        } else {
          extractIds(childNode, ids);
        }
      }
    } else if (node.isArray()) {
      for (JsonNode arrayItem : node) {
        extractIds(arrayItem, ids);
      }
    }
  }

  protected void deleteSection6ByDataset(JsonObject mongoApplication, String catalogId) {
    if (mongoApplication == null || catalogId == null || catalogId.isBlank()) {
      return;
    }

    // 1) If catalogId exists in >= 1 dataset -> DO NOT delete section6
    JsonArray datasets = mongoApplication.getJsonArray("datasets");
    if (datasets != null) {
      for (Object dsObj : datasets) {
        if (dsObj instanceof JsonObject ds) {
          String dsCatalogId = ds.getString("catalog_id");
          if (catalogId.equals(dsCatalogId)) {
            return; // keep section6
          }
        }
      }
    }

    // 2) Otherwise (catalogId not present in any dataset) -> delete section6 entries related to that catalogId
    JsonObject mongoForm = mongoApplication.getJsonObject("form");
    if (mongoForm == null) {
      return;
    }

    JsonArray mongoSection6 = mongoForm.getJsonArray(SECTION_6);
    if (mongoSection6 == null || mongoSection6.isEmpty()) {
      return;
    }

    Iterator<Object> it = mongoSection6.iterator();
    while (it.hasNext()) {
      Object section6Obj = it.next();
      if (section6Obj instanceof JsonObject jsonObject) {
        JsonArray mongoCatalogIds = jsonObject.getJsonArray("catalog_ids");
        if (mongoCatalogIds != null && mongoCatalogIds.contains(catalogId)) {
          it.remove();
        }
      }
    }
  }


  protected void deleteDistributionsFromBasket(JsonArray datasets, RoutingContext context) {
    for (Object datasetObj : datasets) {
      JsonObject dataset = (JsonObject) datasetObj;
      JsonArray distributions = dataset.getJsonArray(DISTRIBUTIONS);
      ArrayList<JsonObject> distributionIdList = new ArrayList<>();
      for (Object distributionObj : distributions) {
        JsonObject distribution = (JsonObject) distributionObj;
        distributionIdList.add(
          new JsonObject().put(DISTRIBUTION_ID, distribution.getString(DISTRIBUTION_ID)));
      }
      basketManager.deleteDistributionsByDistributionIdList(
        new JsonObject().put(DATASET_ID, dataset.getString(DATASET_ID)).put(USER_ID,
          Context.getEcasId(context)), distributionIdList);
    }
  }

  public void postData(@NotNull RoutingContext context, String collectionName) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String userId = Context.getEcasId(context);
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, userId);

    log.debug("PostData called for applicationId: {}", applicationId);

    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        log.debug("PostData found the application: {}", applicationId);

        if (ar.result() != null) {
          log.debug("PostData application is not empty");

          JsonObject mongoApplication = ar.result();

          if (ON_GOING.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))
            || DRAFT.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            // Proceed if the application exists
            handleDocUpload(context, APPLICATIONID, applicationId, hubStoreClient, vertx)
              .onSuccess(successDocUpload -> {
                log.info("Successfully uploaded the file document");
                handleDocTranslation(mongoApplication, successDocUpload, collectionName);

              })
              .onFailure(errorDocUpload -> {
                log.info("PostData Failed to handleDocUpload with error message: {}",
                  errorDocUpload.getMessage());
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Could not upload the file, error message: " + errorDocUpload.getMessage());
              });
          } else {
            log.info("PostData application is in submitted status and cannot proceed");
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Posting data can not be executed in a completed application");
          }
        } else {
          log.info("PostData application is empty");
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            APPLICATION_WITH_ID + context.pathParam(APPLICATION_ID)
              + WAS_NOT_FOUND);
        }
      } else {
        log.info("PostData findOne query was not successful");
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          ar.cause().getMessage());
      }
    });
  }

  private void handleDocTranslation(JsonObject mongoApplication, JsonObject successDocUpload,
    String collectionName) {
    // Translate the file
    String documentId = successDocUpload.getJsonObject("result").getString("id");
    log.info("HandleDocTranslation called for id: {}", documentId);

    // Fetch source language
    String sourceLang = mongoApplication
      .getString("inputLanguage", "EN")
      .toUpperCase(Locale.ENGLISH);

    // Extract datasets as List<JsonObject>
    List<JsonObject> datasets = mongoApplication.getJsonArray("datasets").stream()
      .filter(obj -> obj instanceof JsonObject)
      .map(obj -> (JsonObject) obj)
      .toList();

    log.info("HandleDocTranslation will getTargetLanguagesFromDatasets");
    getTargetLanguagesFromDatasets(datasets).onComplete(langResult -> {

      log.info("HandleDocTranslation completed getTargetLanguagesFromDatasets");
      List<String> targetLanguages;
      if (langResult.succeeded()) {
        log.info("HandleDocTranslation langResult succeeded");
        Set<String> uniqueLanguages = new HashSet<>(langResult.result());
        // English is included in the translation languages by default
        uniqueLanguages.add("EN");
        targetLanguages = new ArrayList<>(uniqueLanguages);

      } else {
        log.info("HandleDocTranslation langResult did not succeed");
        // If failed to get target languages, fallback to empty list (no translation)
        targetLanguages = List.of();
      }

      log.info(
        "HandleDocTranslation will triggerDocumentTranslationRequest for application id: {} and document id: {}",
        mongoApplication.getString(ID), documentId);
      triggerDocumentTranslationRequest(mongoApplication.getString(ID),
        collectionName, targetLanguages, sourceLang, documentId);
    });
  }

  protected void triggerDocumentTranslationRequest(
    String applicationId,
    String applicationType,
    List<String> targetLanguages,
    String sourceLang,
    String documentId) {

    log.info("Triggering translation for applicationId={}, documentId={}", applicationId,
      documentId);

    // Fetch files as Base64 and trigger separate translation requests per file
    hubStoreClient.getFileAsBase64(documentId, APPLICATIONID, applicationId)
      .onSuccess(filesBase64 -> {
        log.info("Fetched: {} file for Base64 translation for applicationId={}", filesBase64.size(),
          applicationId);
        final String fileId = filesBase64.getString(FILE_ID);
        try {
          JsonObject filePayload = hubTranslationClient.buildFileTranslationPayload(applicationId,
            applicationType, filesBase64, targetLanguages);

          hubTranslationClient.requestFileTranslation(filePayload)
            .onSuccess(v -> {
              log.info("File translation triggered for fileId={}, applicationId={}", fileId,
                applicationId);

              JsonObject translationUpdate = new JsonObject()
                .put("$set", new JsonObject()
                  .put(DATE_MODIFIED, Instant.now())
                  .put("translation.status", TranslationStatus.FILES_PENDING.name())
                  .put("translation.sourceLanguage", sourceLang)
                  .put("translation.targetLanguages", new JsonArray(targetLanguages))
                  .put("translation.files." + fileId, new JsonObject().put("status", "PENDING")))
                .put("$unset", new JsonObject()
                  .put("translation.form", true)
                  .put("form_translations", true)
                );

              log.info(
                "TriggerDocumentTranslationRequest: Persisting translation metadata for applicationId={}",
                applicationId);

              mongoClient.updateCollection(applicationType,
                  new JsonObject().put("_id", applicationId), translationUpdate)
                .onSuccess(v2 -> {
                  log.info(
                    "TriggerDocumentTranslationRequest: Translation metadata persisted successfully for applicationId={}",
                    applicationId);
                })
                .onFailure(err -> log.error(
                  "Failed to persist translation metadata for applicationId={}, error={}",
                  applicationId, err.getMessage(), err));
            })
            .onFailure(err -> {
              log.error("File translation failed for fileId={}, applicationId={}, error={}", fileId,
                applicationId, err.getMessage(), err);
              logTranslationFailureOnce(applicationId, applicationType,
                "Failed to request file translation");
            });

        } catch (Exception ex) {
          log.error("Failed to build or send file translation payload for fileId={}, error={}",
            fileId, ex.getMessage(), ex);
        }

      })
      .onFailure(err -> {
        log.error("Failed to fetch Base64 files for applicationId={}, error={}", applicationId,
          err.getMessage(), err);
        logTranslationFailureOnce(applicationId, applicationType, "Failed to fetch Base64 files");
      });
  }

  /**
   * Get file by id
   */
  public void getData(@NotNull RoutingContext context, String collectionName) {
    String applicationId = context.pathParam(APPLICATION_ID);
    String userId = Context.getEcasId(context);
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, userId);

    // Check if the application exists
    dbHandler.checkDocumentExists(query, collectionName)
      .future()
      .onComplete(existenceCheck -> {
        if (existenceCheck.succeeded()) {
          boolean applicationExists = existenceCheck.result();
          if (applicationExists) {
            // Proceed to get the file data
            getFileData(context, applicationId);
          } else {
            // Document does not exist
            setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
              APPLICATION_WITH_ID + applicationId + NOT_FOUND_FOR_USER_WITH_ID + userId);
          }
        } else {
          // Error occurred during the existence check
          log.error(ERROR_CHECKING_DOCUMENT_EXISTENCE, existenceCheck.cause());
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE);
        }
      });
  }

  protected boolean hasElementWithValue(JsonArray jsonArray, String field, String value) {
    for (Object element : jsonArray) {
      if (element instanceof JsonObject jsonObject) {
        String foundValue = jsonObject.getString(field);
        if (StringUtils.isNotEmpty(foundValue) && Strings.CI.equals(value, foundValue)) {
          return true;
        }
      }
    }
    return false;
  }

  protected void updateDatasetDateAdded(JsonArray datasets, String id, String collectionName) {
    for (Object element : datasets) {
      if (element instanceof JsonObject dataset) {
        String datasetId = dataset.getString(DATASET_ID);
        if (StringUtils.isNotEmpty(datasetId) && Strings.CI.equals(datasetId, id)) {
          prepareDataset(dataset, collectionName);
        }
      }
    }
  }

  protected JsonObject prepareDataset(Object dataset, String collectionName) {
    if (dataset instanceof JsonObject datasetJson) {
      datasetJson.put(DATE_ADDED, Instant.now());
      return datasetJson;
    }
    log.info("{} Provided dataset can not be converted to JsonObject", collectionName);
    return null;
  }

  protected void getFileData(RoutingContext context, final String applicationId) {
    String fileId = context.pathParam("data_id");

    // Get the uploaded file
    hubStoreClient.getFile(fileId, APPLICATIONID, applicationId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          context.response()
            .putHeader(CONTENT_TYPE, "application/octet-stream")
            .putHeader(CONTENT_DISPOSITION, response.headers().get(CONTENT_DISPOSITION))
            .end(response.bodyAsBuffer());
        } else {
          // Handle file retrieval failure
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error retrieving file: " + ar.cause().getMessage());
        }
      });
  }

  protected Future<Void> deleteFile(RoutingContext context, String applicationId) {

    Promise<Void> promise = Promise.promise();

    String fileId = context.pathParam("data_id");

    hubStoreClient.deleteFile(fileId, APPLICATIONID, applicationId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          setUpMessageContextResponse(context, HttpResponseStatus.OK,
            "File with id: " + fileId + " was deleted successfully");
          promise.complete();
        } else {
          // Handle file deletion failure
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error deleting file: " + ar.cause().getMessage());
          promise.fail(ar.cause());
        }
      });
    return promise.future();
  }

  /**
   * Delete file by id
   */
  public void deleteData(@NotNull RoutingContext context, String collectionName) {
    String applicationId = context.pathParam(APPLICATION_ID);
    String userId = Context.getEcasId(context);
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, userId);

    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to create/update an application only if status is not SUBMITTED
          if (ON_GOING.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))
            || DRAFT.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            // Proceed to delete the file
            deleteFile(context, applicationId)
              .onSuccess(res -> {
                JsonObject updateDoc = buildFileRemovalUpdate(mongoApplication,
                  context.pathParam("data_id"));
                if (updateDoc.isEmpty()) {
                  log.warn("No update necessary for application {}", applicationId);
                  return;
                }
                updateApplication(applicationId, updateDoc, collectionName)
                  .onSuccess(
                    updated -> log.info("deleteData for Application: {} updated after file removal",
                      applicationId))
                  .onFailure(err -> log.error("deleteData Failed to update application {}: {}",
                    applicationId, err.getMessage()));
              });
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Deletion of data can not be executed in a completed application");
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            APPLICATION_WITH_ID + context.pathParam(APPLICATION_ID)
              + WAS_NOT_FOUND);
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          ar.cause().getMessage());
      }
    });
  }

  private JsonObject buildFileRemovalUpdate(JsonObject mongoApplication, String fileId) {
    JsonObject update = new JsonObject();
    JsonObject translation = mongoApplication.getJsonObject("translation");
    if (translation == null) {
      return update;
    }

    JsonObject files = translation.getJsonObject("files");
    if (files == null || !files.containsKey(fileId)) {
      return update;
    }

    // Simulate removal locally only to decide what else to unset
    files.remove(fileId);

    // Build $unset to remove the specific file field
    JsonObject unset = new JsonObject();

    // If all files are removed → also remove entire translation object
    if (files.isEmpty()) {
      unset.put("translation", true);
    } else {
      unset.put("translation.files." + fileId, true) // remove only this file
        .put("translation.form", true);
      // Otherwise, recalculate translation.status
      TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
        new JsonObject().put("files", files)
      );

      String status = overallStatus.name();
      if (TranslationStatus.FORM_PENDING.name().equalsIgnoreCase(status)
        || TranslationStatus.COMPLETED.name().equalsIgnoreCase(status)) {
        status = TranslationStatus.FILES_COMPLETED.name();
      }
      if (TranslationStatus.FILES_COMPLETED.name().equalsIgnoreCase(status)
        || TranslationStatus.COMPLETED.name().equalsIgnoreCase(status)) {
        unset.put(TRANSLATION_ERROR_TYPE, true);
      }

      JsonObject set = new JsonObject()
        .put("translation.status", status)
        .put("date_modified", Instant.now());

      update.put("$set", set);
    }
    unset.put("form_translations", true);

    update.put("$unset", unset);
    return update;
  }

  protected void createOrUpdateSection(@NotNull RoutingContext context, String collectionName) {

    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to create/update an application only if status is not SUBMITTED
          if (!ON_GOING.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS)) || Objects.nonNull(mongoApplication.getJsonArray("additionalInformation"))) {

            //stop if there are validation errors other than mandatory field missing
            if (context.get(ERRORS) != null) {
              List<FieldError> errors = context.get(ERRORS);

              for (FieldError error : errors) {
                if (!ERROR_CODES_REQUIRED.contains(error.getCode())) {
                  setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
                    "Section not stored due to validation errors");
                  return;
                }
              }
            }

            String sectionNumber = SECTION + context.pathParam(SECTION_NUMBER);

            // Extracting section3
            JsonObject section3 = null;

            if (Objects.nonNull(mongoApplication
              .getJsonObject("form")) && Objects.nonNull(mongoApplication
              .getJsonObject("form").getJsonObject("section3"))) {
              section3 = mongoApplication
                .getJsonObject("form").getJsonObject("section3");
            }

            JsonObject update = new JsonObject();

            if (DATA_ACCESS_APPLICATION_COLLECTION.equals(collectionName)) {
              update.put(FORM + sectionNumber,
                (Strings.CI.equals(sectionNumber, SECTION_6)) ? sameAsSection3Array(
                  context.body().asJsonArray(), section3)
                  : sameAsSection3Object(context.body().asJsonObject(), sectionNumber, section3));
            } else if (DATA_REQUEST_APPLICATION_COLLECTION.equals(collectionName)) {
              update.put(FORM + sectionNumber,
                (Strings.CI.equals(sectionNumber, SECTION_6)) ? context.body().asJsonArray()
                  : sameAsSection3Object(context.body().asJsonObject(), sectionNumber, section3));

            }

            update.put(DATE_MODIFIED, Instant.now());

            JsonObject mongoForm = mongoApplication.getJsonObject("form");
            if (Objects.nonNull(mongoForm)) {
              updateSameAsSection3(mongoForm, update,
                Integer.valueOf(context.pathParam(SECTION_NUMBER)),
                DATA_ACCESS_APPLICATION_COLLECTION.equals(collectionName));
            }

            JsonObject form = new JsonObject();
            form.put("$set", update);

            mongoClient.updateCollection(collectionName, query, form, res -> {
              if (res.succeeded()) {
                MongoClientUpdateResult result = res.result();
                if (result.getDocMatched() == 1) {
                  if (result.getDocModified() == 1) {
                    setUpMessageContextResponse(context, HttpResponseStatus.OK,
                      "Section " + sectionNumber + " has been stored in application with id: "
                        + context.pathParam(APPLICATION_ID));
                  } else {
                    setUpMessageContextResponse(context, HttpResponseStatus.OK,
                      "No changes contained in the already existing " + sectionNumber);
                  }
                } else {
                  setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
                    APPLICATION_WITH_ID + context.pathParam(APPLICATION_ID)
                      + WAS_NOT_FOUND);
                }

              } else {
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  res.cause().getMessage());
              }
            });
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Creation or update of a section can not be executed in a submitted application");
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            APPLICATION_WITH_ID + context.pathParam(APPLICATION_ID)
              + WAS_NOT_FOUND);
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          ar.cause().getMessage());
      }
    });
  }

  JsonArray sameAsSection3Array(JsonArray sectionFormArray, JsonObject section3) {
    if (Objects.isNull(section3))
      return sectionFormArray;

    JsonObject legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalOrNaturalPerson))
      return sectionFormArray;

    String personType = legalOrNaturalPerson.getString("key");

    for (int i = 0; i < sectionFormArray.size(); i++) {
      JsonObject sectionItem = sectionFormArray.getJsonObject(i);
      String name = "a".equals(personType) ? LEGAL_PERSON_NAME : NATURAL_PERSON_NAME;
      String email = "a".equals(personType) ? CONTACT_PERSON_EMAIL : NATURAL_PERSON_EMAIL;
      String phone = "a".equals(personType) ? CONTACT_PERSON_PHONE : NATURAL_PERSON_PHONE;

      if (isFieldTrue(sectionItem, INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON)) {
        sectionItem.put(INFORMATION_PROVIDER_NAME, section3.getString(name));
        sectionItem.put(INFORMATION_PROVIDER_EMAIL, section3.getString(email));
        sectionItem.put(INFORMATION_PROVIDER_PHONE, section3.getJsonObject(phone));
      }

      // Update the array if needed
      sectionFormArray.set(i, sectionItem);
    }

    return sectionFormArray;
  }

  private JsonObject sameAsSection3Object(JsonObject sectionForm, String sectionNumber,
    JsonObject section3) {
    if (Objects.isNull(section3))
      return sectionForm;

    JsonObject legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalOrNaturalPerson))
      return sectionForm;

    String personType = legalOrNaturalPerson.getString("key");

    switch (sectionNumber) {
      case SECTION_4:
        populateSection4(sectionForm, section3, personType);
        break;
      case SECTION_5:
        populateSection5(sectionForm, section3, personType);
        break;
      default:
        break;
    }

    return sectionForm;
  }

  void populateSection4(JsonObject sectionForm, JsonObject section3, String personType) {
    if (isFieldTrue(sectionForm, "sameAsContactPerson")) {
      if ("a".equals(personType)) {
        sectionForm.put(FULL_NAME, section3.getString(LEGAL_PERSON_NAME));
        sectionForm.put(ADDRESS, section3.getString(LEGAL_PERSON_ADDRESS));
        sectionForm.put(PHONE, section3.getJsonObject(CONTACT_PERSON_PHONE));
        sectionForm.put(EMAIL, section3.getString(CONTACT_PERSON_EMAIL));
        sectionForm.put(NAME_OF_THE_ORGANISATION,
          section3.getString(CONTACT_PERSON_ORGANISATION_NAME));
        sectionForm.put(BUSINESS_IDENTIFIER_ORGANIZATION,
          section3.getString(CONTACT_PERSON_BUSINESS_ID));
      } else if ("b".equals(personType)) {
        sectionForm.put(FULL_NAME, section3.getString(NATURAL_PERSON_NAME));
        sectionForm.put(ADDRESS, section3.getString(NATURAL_PERSON_ADDRESS));
        sectionForm.put(PHONE, section3.getJsonObject(NATURAL_PERSON_PHONE));
        sectionForm.put(EMAIL, section3.getString(NATURAL_PERSON_EMAIL));
      }
    }
  }

  void populateSection5(JsonObject sectionForm, JsonObject section3, String personType) {
    boolean isLegalPerson = "a".equals(personType);

    String nameKey = isLegalPerson ? LEGAL_PERSON_NAME : NATURAL_PERSON_NAME;
    String nameValue = section3.getString(nameKey);

    if (isFieldTrue(sectionForm, PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON)) {
      sectionForm.put(PERSON_RESPONSIBLE_NAME, nameValue);
    }

    if (isFieldTrue(sectionForm, PERSON_RESEARCH_SAME_AS_CONTACT_PERSON)) {
      sectionForm.put(PERSON_RESEARCH_NAME, nameValue);
    }
  }

  private boolean isFieldTrue(JsonObject sectionForm, String fieldKey) {
    return sectionForm.containsKey(fieldKey) && sectionForm.getBoolean(fieldKey, false);
  }


  void updateSameAsSection3(JsonObject mongoForm, JsonObject updateForm, Integer sectionNumber,
    Boolean isAccess) {
    JsonObject section3 = mongoForm.getJsonObject(SECTION_3);

    if (Objects.isNull(section3))
      return;

    switch (sectionNumber) {
      case 4:
        updateSection4(updateForm.getJsonObject("form.section4"), section3);
        break;
      case 5:
        updateSection5(updateForm.getJsonObject("form.section5"), section3);
        break;
      case 6:
        if (Boolean.TRUE.equals(isAccess))
          updateSection6(updateForm.getJsonArray("form.section6"), section3);
        break;
      case 8:
        if (Boolean.TRUE.equals(isAccess))
          updateSection8(updateForm.getJsonObject("form.section8"), section3);
        break;
      default:
        break;
    }
  }

  private void updateSection4(JsonObject section4, JsonObject section3) {
    if (Objects.isNull(section4))
      return;

    boolean sameAsContactPerson = section4.getBoolean(SAME_AS_CONTACT_PERSON, false);

    if (!sameAsContactPerson)
      return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson))
      return;

    String key = legalPerson.getString("key");
    if ("a".equals(key)) {
      updateLegalPerson(section4, section3);
    } else if ("b".equals(key)) {
      updateNaturalPerson(section4, section3);
    }
  }

  private void updateSection5(JsonObject section5, JsonObject section3) {
    if (Objects.isNull(section5))
      return;

    updatePerson(section5, section3, PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON,
      PERSON_RESPONSIBLE_NAME);

    updatePerson(section5, section3, PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, PERSON_RESEARCH_NAME);
  }

  private void updateSection8(JsonObject section8, JsonObject section3) {
    if (Objects.isNull(section8))
      return;

    boolean sameAsContactPerson = section8.getBoolean(SAME_AS_CONTACT_PERSON, false);

    if (!sameAsContactPerson)
      return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson))
      return;

    if (section8.getJsonArray(PEOPLE_WHO_WILL_BE_PROCESSING_THE_DATA) == null) {
      section8.put(PEOPLE_WHO_WILL_BE_PROCESSING_THE_DATA, new JsonArray());
    }

    boolean isNatural = "b".equals(legalPerson.getString("key"));

    String fullName = isNatural
      ? section3.getString("naturalPersonName", "")
      : section3.getString("contactPersonName", "");

    String emailAddress = isNatural
      ? section3.getString("naturalPersonEmail", "")
      : section3.getString("contactPersonEmail", "");

    // Create JsonObject for person (you can also use your PersonDTO and convert it)
    JsonObject personJson = new JsonObject()
      .put(FULL_NAME, fullName != null ? fullName : "")
      .put("emailAddress", emailAddress != null ? emailAddress : "");

    // Get or initialize the array
    JsonArray peopleArray = section8.getJsonArray(PEOPLE_WHO_WILL_BE_PROCESSING_THE_DATA);
    if (peopleArray == null) {
      peopleArray = new JsonArray();
      section8.put(PEOPLE_WHO_WILL_BE_PROCESSING_THE_DATA, peopleArray);
    }

    // Add or replace the first person
    if (peopleArray.isEmpty()) {
      peopleArray.add(personJson);
    } else {
      peopleArray.set(0, personJson);
    }
  }

  private void updatePerson(JsonObject section, JsonObject section3, String key, String nameKey) {
    boolean sameAsContactPerson = section.getBoolean(key, false);
    if (!sameAsContactPerson)
      return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson))
      return;

    String personType = legalPerson.getString("key");
    if ("a".equals(personType)) {
      section.put(nameKey, section3.getString(LEGAL_PERSON_NAME, ""));
    } else if ("b".equals(personType)) {
      section.put(nameKey, section3.getString(NATURAL_PERSON_NAME, ""));
    }
  }

  private void updateSection6(JsonArray section6, JsonObject section3) {
    if (Objects.isNull(section6))
      return;

    for (int i = 0; i < section6.size(); i++) {
      JsonObject country = section6.getJsonObject(i);
      boolean informationProviderSameAsContactPerson = country.getBoolean(
        INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, false);

      if (informationProviderSameAsContactPerson) {
        updateCountryInformationProvider(country, section3);
      }
    }
  }

  private void updateCountryInformationProvider(JsonObject country, JsonObject section3) {
    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson))
      return;

    String key = legalPerson.getString("key");
    if ("a".equals(key)) {
      country.put("informationProviderName", section3.getString(LEGAL_PERSON_NAME, ""));
      country.put("informationProviderPhone", getPhoneNumber(section3, CONTACT_PERSON_PHONE));
      country.put("informationProviderEmail", section3.getString(CONTACT_PERSON_EMAIL, ""));
    } else if ("b".equals(key)) {
      country.put("informationProviderName", section3.getString(NATURAL_PERSON_NAME, ""));
      country.put("informationProviderPhone", getPhoneNumber(section3, NATURAL_PERSON_PHONE));
      country.put("informationProviderEmail", section3.getString(NATURAL_PERSON_EMAIL, ""));
    }
  }

  private JsonObject getPhoneNumber(JsonObject section3, String phoneKey) {
    return section3.containsKey(phoneKey) && section3.getValue(phoneKey) instanceof JsonObject
      ? section3.getJsonObject(phoneKey)
      : new JsonObject().put("countryCode", "").put("number", "");
  }

  private void updateLegalPerson(JsonObject section4, JsonObject section3) {
    section4.put(FULL_NAME, section3.getString(LEGAL_PERSON_NAME, ""));
    section4.put(ADDRESS, section3.getString(LEGAL_PERSON_ADDRESS, ""));
    section4.put(PHONE, getPhoneNumber(section3, CONTACT_PERSON_PHONE));
    section4.put(EMAIL, section3.getString(CONTACT_PERSON_EMAIL, ""));
    section4.put(NAME_OF_THE_ORGANISATION,
      section3.getString(CONTACT_PERSON_ORGANISATION_NAME, ""));
    section4.put(BUSINESS_IDENTIFIER_ORGANIZATION,
      section3.getString(CONTACT_PERSON_BUSINESS_ID, ""));
  }

  private void updateNaturalPerson(JsonObject section4, JsonObject section3) {
    section4.put(FULL_NAME, section3.getString(NATURAL_PERSON_NAME, ""));
    section4.put(ADDRESS, section3.getString(NATURAL_PERSON_ADDRESS, ""));
    section4.put(PHONE, getPhoneNumber(section3, NATURAL_PERSON_PHONE));
    section4.put(EMAIL, section3.getString(NATURAL_PERSON_EMAIL, ""));
  }

  public void getSectionErrors(@NotNull RoutingContext context) {
    //stop if there are validation errors other than mandatory field missing
    if (context.get(ERRORS) != null) {
      List<FieldError> errors = context.get(ERRORS);

      for (FieldError error : errors) {
        if (!ERROR_CODES_REQUIRED.contains(error.getCode())) {
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            "Section not stored due to validation errors");
          return;
        }
      }
    }
    String sectionNumber = SECTION + context.pathParam(SECTION_NUMBER);

    JsonObject update = new JsonObject();
    update.put(FORM + sectionNumber,
      (Strings.CI.equals(sectionNumber, SECTION_6)) ? context.body().asJsonArray()
        : context.body().asJsonObject());
    update.put(DATE_MODIFIED, Instant.now());
    JsonObject form = new JsonObject();
    form.put("$set", update);

    setUpMessageContextResponse(context, HttpResponseStatus.OK,
      "Section " + sectionNumber + " has been stored in application with id: " + context.pathParam(
        APPLICATION_ID));
  }

  public void validateFileBeforeUpdate(@NotNull RoutingContext context, String collectionName) {

    String sectionNumber = context.pathParam(SECTION_NUMBER);
    try {
      switch (collectionName) {
        case DATA_ACCESS_APPLICATION_COLLECTION:
          switch (sectionNumber) {
            case "5": {
              validateFileFor("summaryOfPlanForUsingTheData", context)
                .recover(err -> Future.succeededFuture())
                .compose(res1 -> validateFileFor("summaryOfResearchPlan", context))
                .onComplete(end -> context.next());
              break;
            }
            case "6": {
              validateNestedFileFor("consentAndInformationLetterAttachment", context)
                .recover(err -> Future.succeededFuture())
                .compose(res1 -> validateNestedFileFor("permitDecisionAttachment", context)
                  .recover(err -> Future.succeededFuture()))
                .compose(res2 -> validateNestedFileFor("ethicalReviewAttachment", context))
                .compose(
                  res2 -> validateNestedFileFor("variablesForDataExtractionRelativesAttachments",
                    context))
                .compose(
                  res2 -> validateNestedFileFor("variablesForDataExtractionAttachments", context))
                .compose(
                  res2 -> validateNestedFileFor("variablesToBeUsedInDataExtractionAttachment",
                    context))
                .onComplete(end -> context.next());
              break;
            }
            case "7": {
              validateFileFor("attachmentOfPermitDocuments", context)
                .onComplete(end -> context.next());
              break;
            }
            case "8": {
              validateFileFor("attachResearchPermitIfRequired", context)
                .onComplete(end -> context.next());
              break;
            }
            case "9": {
              validateFileFor("additionalAttachment", context)
                .onComplete(end -> context.next());
              break;
            }
            default: {
              context.next();
              break;
            }
          }
          break;
        case DATA_REQUEST_APPLICATION_COLLECTION:
          switch (sectionNumber) {
            case "5": {
              validateFileFor("summaryOfPlanForUsingTheData", context)
                .recover(err -> Future.succeededFuture())
                .compose(res1 -> validateFileFor("summaryOfResearchPlan", context))
                .onComplete(end -> context.next());
              break;
            }
            case "6": {

              validateNestedFileFromArray("tabulationPlan", "tabulationPlanArray",
                context)
                .recover(err -> Future.succeededFuture())
                .onComplete(end -> context.next());

              break;
            }
            case "7": {
              validateFileFor("additionalAttachment", context)
                .onComplete(end -> context.next());
              break;
            }
            default: {
              context.next();
              break;
            }
          }
          break;
        default: {
          context.next();
          break;
        }
      }
    } catch (Exception e) {
      log.error("Error at validateFileBeforeSubmit with message: {}", e.getMessage());
      context.next();
    }
  }

  private Future<Void> validateNestedFileFromArray(@NotNull String fieldName,
    @NotNull String arrayName, @NotNull RoutingContext context) {

    Promise<Void> promise = Promise.promise();

    List<Future<Void>> futures = new ArrayList<>();
    JsonArray objectList = context.body().asJsonArray();
    objectList.forEach(obj -> {
      JsonObject object = (JsonObject) obj;
      if (object.containsKey(arrayName)) {
        JsonArray arrayList = object.getJsonArray(arrayName);
        if (arrayList != null && !arrayList.isEmpty()) {
          arrayList.forEach(arrayObj -> {
            JsonObject arrayObject = (JsonObject) arrayObj;
            if (arrayObject.containsKey(fieldName)) {
              futures.add(validateNestedSingleFileFor(fieldName, arrayObject,
                context.pathParam(APPLICATION_ID)));
            }
          });
        }
      }
    });
    // Wait for all Futures to complete
    Future.all(futures)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          promise.complete(); // Complete the promise successfully
        } else {
          promise.fail(ar.cause()); // Fail the promise with the first encountered error
        }
      });

    return promise.future();
  }

  private Future<Void> validateNestedFileFor(@NotNull String fieldName,
    @NotNull RoutingContext context) {
    JsonArray objectList = context.body().asJsonArray();
    return validateNestedFileFor(fieldName, objectList, context.pathParam(APPLICATION_ID));
  }

  private Future<Void> validateNestedSingleFileFor(@NotNull String fieldName,
    @NotNull JsonObject object, String applicationId) {

    Promise<Void> promise = Promise.promise();

    JsonObject attachment = object.getJsonObject(fieldName);
    String attachmentId = attachment.getString("id");
    hubStoreClient.getFile(attachmentId, APPLICATIONID, applicationId)
      .compose(result -> {
        String fileName = decodeFilename(result.getHeader(CONTENT_DISPOSITION));
        if (!fileName.equalsIgnoreCase(attachment.getString("name"))) {
          object.remove(fieldName);
        }
        promise.complete();
        return Future.succeededFuture();
      })
      .recover(err -> {
        // Failure handling: Add the attachment to the removal list
        object.remove(fieldName);
        promise.complete();
        return Future.succeededFuture();
      });

    return promise.future();
  }

  private Future<Void> validateNestedFileFor(@NotNull String fieldName,
    @NotNull JsonArray objectList, String applicationId) {

    Promise<Void> promise = Promise.promise();
    List<Future<Void>> futures = new ArrayList<>(); // Collect all futures

    objectList.forEach(obj -> {
      JsonObject object = (JsonObject) obj;
      if (object.containsKey(fieldName)) {
        JsonArray list = object.getJsonArray(fieldName);
        if (list != null && !list.isEmpty()) {
          // Add the validateFile Future to the list
          futures.add(validateFile(list, applicationId, object, fieldName));
        } else {
          object.remove(fieldName);
        }
      }
    });

    // Wait for all Futures to complete
    Future.all(futures)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          promise.complete(); // Complete the promise successfully
        } else {
          promise.fail(ar.cause()); // Fail the promise with the first encountered error
        }
      });

    return promise.future();
  }

  private Future<Void> validateFileFor(@NotNull String fieldName, @NotNull RoutingContext context) {

    Promise<Void> promise = Promise.promise();

    JsonArray fieldNameContentList = context.body().asJsonObject()
      .getJsonArray(fieldName);

    if (fieldNameContentList != null && !fieldNameContentList.isEmpty()) {
      validateFile(fieldNameContentList, context.pathParam(APPLICATION_ID),
        context.body().asJsonObject(), fieldName)
        .onComplete(result -> {
          if (fieldNameContentList.isEmpty()) {
            context.body().asJsonObject().remove(fieldName);
          }
          promise.complete();
        });
    } else {
      context.body().asJsonObject().remove(fieldName);
      promise.complete();
    }
    return promise.future();
  }

  private Future<Void> validateFile(@NotNull JsonArray objectList, String applicationId,
    JsonObject object, @NotNull String fieldName) {
    Promise<Void> promise = Promise.promise();

    List<Future<Object>> futures = new ArrayList<>();
    List<JsonObject> objectsToRemove = new ArrayList<>();

    objectList.forEach(obj -> {
      JsonObject attachment = (JsonObject) obj;
      String attachmentId = attachment.getString("id");

      Future<Object> future = hubStoreClient.getFile(attachmentId, APPLICATIONID, applicationId)
        .compose(result -> {

          String fileName = decodeFilename(result.getHeader(CONTENT_DISPOSITION));

          if (!fileName.equalsIgnoreCase(attachment.getString("name"))) {
            objectsToRemove.add(attachment);
          }
          return Future.succeededFuture();
        })
        .recover(err -> {
          // Failure handling: Add the attachment to the removal list
          objectsToRemove.add(attachment);
          return Future.succeededFuture();
        });

      futures.add(future);
    });

    Future.all(futures)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          // Remove collected objects after all operations are complete
          objectsToRemove.forEach(objectList::remove);
        }
        if (objectList.isEmpty()) {
          object.remove(fieldName);
        }
        promise.complete();
      });

    return promise.future();
  }

  protected String decodeFilename(String contentDisposition) {
    if (StringUtils.isNotEmpty(contentDisposition) && contentDisposition.contains(
      FILENAME_ASTERISK_EQUALS)) {
      String part = contentDisposition.substring(
          contentDisposition.indexOf(FILENAME_ASTERISK_EQUALS) + FILENAME_ASTERISK_EQUALS.length())
        .trim();
      if (part.startsWith("UTF-8''")) {
        part = part.substring("UTF-8''".length());
      }
      return URLDecoder.decode(part, StandardCharsets.UTF_8);
    }
    return "Error_decoding_filename";
  }

  protected void cleanSection2(JsonObject mongoApplicationForm) {
    JsonObject section2 = mongoApplicationForm.getJsonObject(SECTION_2);
    if (Objects.isNull(section2)) {
      return;
    }

    boolean isSummaryChecked =
      section2.containsKey(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
        && section2.getBoolean(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY,
        false);

    if (!isSummaryChecked) {
      section2.remove(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON);
    } else {
      section2.remove("summaryOfTheProject");
    }

    // Explicitly put the modified section2 back into mongoApplication
    mongoApplicationForm.put(SECTION_2, section2);
  }

  protected void cleanSection3(JsonObject mongoApplicationForm) {
    JsonObject section3 = mongoApplicationForm.getJsonObject(SECTION_3);
    if (Objects.isNull(section3)) {
      return;
    }

    JsonObject applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
      APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && !"a".equals(
      applyingForDataOnBehalfOfPublicSector.getString("key"))) {
      section3.remove(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS);
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    //if is "a" is legal person else i b natural person
    if (Objects.nonNull(legalPerson) &&
      Objects.equals(legalPerson.getString("key"), "b")) {
      section3.remove(LEGAL_PERSON_NAME);
      section3.remove(LEGAL_PERSON_ADDRESS);
      section3.remove("legalPersonZipCode");
      section3.remove("legalPersonCity");
      section3.remove(LEGAL_PERSON_COUNTRY);
      section3.remove("contactPersonName");
      section3.remove(CONTACT_PERSON_EMAIL);
      section3.remove(CONTACT_PERSON_PHONE);
      section3.remove(CONTACT_PERSON_ORGANISATION_NAME);
      section3.remove(CONTACT_PERSON_BUSINESS_ID);
      section3.remove("contactPersonRelationship");
    } else {
      section3.remove(NATURAL_PERSON_NAME);
      section3.remove(NATURAL_PERSON_ADDRESS);
      section3.remove("naturalPersonZipCode");
      section3.remove("naturalPersonCity");
      section3.remove("naturalPersonCountry");
      section3.remove(NATURAL_PERSON_EMAIL);
      section3.remove(NATURAL_PERSON_PHONE);
    }

    // Explicitly put the modified section2 back into mongoApplication
    mongoApplicationForm.put(SECTION_3, section3);
  }

  protected void cleanSection4(JsonObject mongoApplicationForm) {
    JsonObject section3 = mongoApplicationForm.getJsonObject(SECTION_3);
    JsonObject section4 = mongoApplicationForm.getJsonObject(SECTION_4);
    if (Objects.isNull(section4)) {
      return;
    }

    JsonObject invoiceType = section4.getJsonObject("invoiceType");

    if (Objects.nonNull(invoiceType) &&
      !Objects.equals(invoiceType.getString("key"), "b")) {
      section4.remove("invoiceAddress");
    }

    JsonObject legalPerson = Objects.nonNull(section3) ?
      section3.getJsonObject(LEGAL_OR_NATURAL_PERSON) : null;
    if (Objects.nonNull(legalPerson) &&
      Objects.equals(legalPerson.getString("key"), "b")) {
      section4.remove(OPERATOR_IDENTIFIER);
      section4.remove(NAME_OF_THE_ORGANISATION);
      section4.remove(BUSINESS_IDENTIFIER_ORGANIZATION);
      section4.remove("vatNumber");
      section4.remove("peppolCode");
    }

    JsonObject isTheProjectFinanciallyCovered = section4.getJsonObject(
      "isTheProjectFinanciallyCovered");
    if (Objects.nonNull(isTheProjectFinanciallyCovered) &&
      !"a".equals(isTheProjectFinanciallyCovered.getString("key"))) {
      section4.remove("rangeOfAmountOfFinancing");
    }
    mongoApplicationForm.put(SECTION_4, section4);
  }

  //Method to check if the JsonArray doesn't contain an object with a specific key value
  protected boolean notContainsKey(JsonArray jsonArray, String keyToCheck) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");
      if (keyToCheck.equals(key)) {
        return false;
      }
    }
    return true;
  }

  protected void sendApplicationForDispatch(JsonObject mongoApplication, String database, OperationStatus operation) {

    mongoApplication.put(DATE_DISPATCHED, Instant.now());
    // Clone the application
    JsonObject applicationForDispatch = mongoApplication.copy();

    JsonArray targetLanguages = applicationForDispatch.getJsonObject(TRANSLATION).getJsonArray(
      TARGET_LANGUAGES);

    JsonObject cronConfiguration = new JsonObject().put(Constants.APPLICATION_TYPE, database)
      .put(STATUS, "pending").put("retries", 0);
    JsonObject dispatcherRecord = new JsonObject()
      .put("configuration", cronConfiguration)
      .put("application", applicationForDispatch)
      .put(TARGET_LANGUAGES, targetLanguages)
      .put(OPERATION, operation.getLabel());

    mongoClient.save(DISPATCHER_COLLECTION, dispatcherRecord, res -> {
      if (res.succeeded()) {
        log.info(DISPATCHER_COLLECTION + " Saved dispatcher application with id: {}",
          res.result());
      } else {
        log.error(
          DISPATCHER_COLLECTION + " Failed to save dispatcher application, with error message: {}",
          res.cause().getMessage());
      }
    });
  }

  protected void sendApplicationToOutgoingApplicationActivities(String collectionName,
    String applicationId, CatalogStatus catalogStatus, String catalogId, String reason) {

    JsonObject outgoingApplicationActivity = createOutgoingApplicationActivity(collectionName,
      applicationId, catalogStatus, catalogId, reason);
    mongoClient.save(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION, outgoingApplicationActivity,
      res -> {
        if (res.succeeded()) {
          log.info(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Saved outgoing application activity with id: {}",
            res.result());
        } else {
          log.error(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Failed to save outgoing application activity, with error message: {}",
            res.cause().getMessage());
        }
      });
  }

  protected void sendApplicationToOutgoingApplicationActivities(String collectionName,
    String applicationId, CatalogStatus catalogStatus, String catalogId, String status,
    String datasetId, String reason, OperationStatus operationStatus) {

    JsonObject outgoingApplicationActivity = createOutgoingApplicationActivity(collectionName,
      applicationId, catalogStatus, catalogId, status, datasetId, operationStatus, reason);
    mongoClient.save(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION, outgoingApplicationActivity,
      res -> {
        if (res.succeeded()) {
          log.info(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Saved outgoing application activity with id: {}",
            res.result());
        } else {
          log.error(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Failed to save outgoing application activity, with error message: {}",
            res.cause().getMessage());
        }
      });
  }

  private JsonObject createOutgoingApplicationActivity(String collectionName, String applicationId,
    CatalogStatus catalogStatus, String catalogId,String reason) {
    return createOutgoingApplicationActivity(collectionName, applicationId,
      catalogStatus, catalogId, null, null, OperationStatus.STATUS_UPDATE, reason);
  }

  private JsonObject createOutgoingApplicationActivity(
    String collectionName,
    String applicationId,
    CatalogStatus catalogStatus,
    String catalogId,
    String status,
    String datasetId,
    OperationStatus operationStatus,
    String reason) {

    JsonObject activity = new JsonObject()
      .put("objectId", applicationId)
      .put("type", "application")
      .put("operation", operationStatus.getLabel());

    JsonObject payload = new JsonObject()
      .put("application_type", collectionName)
      .put("catalog_id", catalogId)
      .put("status", catalogStatus.getLabel());

    switch (operationStatus) {
      case STATUS_UPDATE:
        // Nothing extra, payload already filled
        break;
      case DECISION:
        JsonObject decision = new JsonObject()
          .put("decision", status)
          .put("dataset_id", datasetId);
        payload.put("decisions", new JsonArray().add(decision));
        break;
      case PERMIT:
        JsonObject permit = new JsonObject()
          .put("dataset_id", datasetId)
          .put("reason", reason)
          .put("permit_status", status);
        payload.put("permits", new JsonArray().add(permit));
        break;
      default:
        throw new IllegalArgumentException("Unsupported operation: " + operationStatus);
    }

    activity.put("payload", payload);

    activity.put("configuration", new JsonObject()
      .put("syncPending", "1"));

    return activity;
  }

  protected void changeTitle(@NotNull RoutingContext context, String database) {
    String id = context.pathParam(APPLICATION_ID);
    String title = context.body().asJsonObject().getString(TITLE);

    // Retrieve the title of application.
    JsonObject query = new JsonObject()
      .put(TITLE, title)  // Match the title
      .put(ID, new JsonObject().put("$ne", id))
      .put(USER_ID, Context.getEcasId(context));

    // Check if the provided title already exists as a document.
    mongoClient.findOne(database, query, null, asyncResult -> {
      // Title already exists.
      if (asyncResult.result() != null) {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "applicationTitleExists");
        // Update
      } else {
        JsonObject queryUpdate = new JsonObject()
          .put(ID, id)
          .put(STATUS_STATUS, new JsonObject()
              .put("$not", new JsonObject()
                .put("$regex", "^SUBMITTED$")
                .put("$options", "i")
              )
          );
        JsonObject update = new JsonObject().put("$set", new JsonObject()
          .put(TITLE, context.body().asJsonObject().getValue(TITLE))
          .put(INPUT_LANGUAGE, context.body().asJsonObject().getValue(INPUT_LANGUAGE)));

        mongoClient.findOneAndUpdate(database, queryUpdate, update,
          res -> {
            if (res.succeeded()) {
              setUpMessageContextResponse(context, HttpResponseStatus.OK,
                "Updated Title and Input Language successfully for application with id: " + id);
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
                "Did not update Title and Input Language in the DB level for application with id: " + id);
            }
          });
      }
    });
  }

  protected void getApplicationsHbad(@NotNull RoutingContext context, String database) {
    JsonObject query = new JsonObject()
      .put(USER_ID, Context.getEcasId(context));

    // Query all documents and collect distinct HDAB names
    mongoClient.find(database, query, asyncResult -> {
      if (asyncResult.succeeded()) {
        List<String> distinctHdabs = new ArrayList<>();

        asyncResult.result().forEach(doc -> {
          // Each document may contain "datasets" -> array
          JsonArray datasets = doc.getJsonArray("datasets");
          if (datasets != null) {
            for (int i = 0; i < datasets.size(); i++) {
              JsonObject dataset = datasets.getJsonObject(i);
              if (dataset == null) continue;

              // Each dataset may have "hdab" -> object with "name"
              JsonObject hdab = dataset.getJsonObject("hdab");
              if (hdab != null) {
                String name = hdab.getString("name");
                if (name != null && !name.isBlank() && !distinctHdabs.contains(name)) {
                  distinctHdabs.add(name);
                }
              }
            }
          }
        });

        // Return as a JSON array response
        JsonArray resultArray = new JsonArray(distinctHdabs);
        context.response()
          .putHeader("Content-Type", "application/json")
          .end(resultArray.encodePrettily());

      } else {
        context.response()
          .setStatusCode(500)
          .end(new JsonObject().put("error", asyncResult.cause().getMessage()).encodePrettily());
      }
    });
  }

  protected void getApplicationsCountries(@NotNull RoutingContext context, String database) {
    JsonObject query = new JsonObject()
      .put(USER_ID, Context.getEcasId(context));

    // Get all documents and extract distinct country labels
    mongoClient.find(database, query, asyncResult -> {
      if (asyncResult.succeeded()) {
        List<String> distinctCountries = new ArrayList<>();

        asyncResult.result().forEach(doc -> {
          JsonArray datasets = doc.getJsonArray("datasets");
          if (datasets != null) {
            for (int i = 0; i < datasets.size(); i++) {
              JsonObject dataset = datasets.getJsonObject(i);
              if (dataset == null) continue;

              JsonObject country = dataset.getJsonObject(COUNTRY);
              if (country != null) {
                String label = country.getString("label");
                if (label != null && !label.isBlank() && !distinctCountries.contains(label)) {
                  distinctCountries.add(label);
                }
              }
            }
          }
        });

        // Respond with array of unique country labels
        JsonArray resultArray = new JsonArray(distinctCountries);
        context.response()
          .putHeader("Content-Type", "application/json")
          .end(resultArray.encodePrettily());

      } else {
        context.response()
          .setStatusCode(500)
          .end(new JsonObject().put("error", asyncResult.cause().getMessage()).encodePrettily());
      }
    });
  }

  protected void getApplicationTitles(@NotNull RoutingContext context, String database) {
    JsonObject query = new JsonObject()
      .put(USER_ID, Context.getEcasId(context));

    // Get all documents and extract distinct country labels
    mongoClient.find(database, query, asyncResult -> {
      if (asyncResult.succeeded()) {
        List<String> applicationTitles = new ArrayList<>();

        asyncResult.result().forEach(doc -> {
          String title = doc.getString("title");
          if (title != null) {
            applicationTitles.add(title);
          }
        });

        // Respond with array of unique country labels
        JsonArray resultArray = new JsonArray(applicationTitles);
        context.response()
          .putHeader("Content-Type", "application/json")
          .end(resultArray.encodePrettily());

      } else {
        context.response()
          .setStatusCode(500)
          .end(new JsonObject().put("error", asyncResult.cause().getMessage()).encodePrettily());
      }
    });
  }

  protected void rollbackStatus(String applicationId, ApplicationStatus status, String database) {
    JsonObject query = new JsonObject().put(ID, applicationId);
    JsonObject rollbackStatus = new JsonObject().put("$set",
      new JsonObject().put(STATUS_STATUS, status.getStatus()));

    mongoClient.updateCollection(database, query, rollbackStatus, ar -> {
      if (ar.succeeded()) {
        log.info("Rollback successful: Status reverted to DRAFT for application: {}", applicationId);
      } else {
        log.info("Rollback failed for application : {} with message: {}", applicationId, ar.cause().getMessage());
      }
    });
  }

  protected Future<JsonObject> replaceApplication(JsonObject mongoApplication, String collectionName) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject query = new JsonObject().put(ID, mongoApplication.getValue(ID));
    mongoApplication.put(DATE_MODIFIED, Instant.now());
    mongoClient.findOneAndReplace(collectionName, query, mongoApplication,
      result -> {
        if (result.succeeded()) {
          log.info("{} Application with id: {} was replaced successfully", collectionName,
            mongoApplication.getValue(ID));
          promise.complete(result.result());
        } else {
          log.info("{} Failed to replace application with id: {}", collectionName,
            mongoApplication.getValue(ID));
          promise.fail(result.cause());
        }
      });
    return promise.future();
  }

  protected Future<JsonObject> updateApplication(String id, JsonObject updateApplication, String collectionName) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject query = new JsonObject().put(ID, id);
    mongoClient.findOneAndUpdate(collectionName, query, updateApplication,
      result -> {
        if (result.succeeded()) {
          log.info("{} Application with id: {} was updated successfully", collectionName,
            updateApplication.getValue(ID));
          promise.complete(result.result());
        } else {
          log.info("{} Failed to update application with id: {}", collectionName,
            updateApplication.getValue(ID));
          promise.fail(result.cause());
        }
      });
    return promise.future();
  }

  public void deleteDatasets(@NotNull RoutingContext context, String collectionName) {

    // Get the application id
    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to delete an application only if in DRAFT status
          if (DRAFT.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))
          || ON_GOING.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            JsonArray requestContent = context.body().asJsonArray();
            AtomicBoolean applicationModified = new AtomicBoolean(false);
            requestContent.forEach(requestDataset -> {
              String requestDatasetId = ((JsonObject) requestDataset).getString(DATASET_ID);
              if(deleteApplicationDataset(mongoApplication, requestDatasetId, context, collectionName))
                applicationModified.set(true);
            });
            if(applicationModified.get()) {

              // Remove catalogs with no datasets left
              cleanupEmptyCatalogs(mongoApplication);

              updateTargetLanguages(mongoApplication)
                .onSuccess(v -> {
                  replaceApplication(mongoApplication, collectionName)
                    .onSuccess(v2 ->
                      setUpMessageContextResponse(context,
                        HttpResponseStatus.OK,
                        "Delete operation/s completed")
                    )
                    .onFailure(err ->
                      setUpMessageContextResponse(context,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "replaceApplication failed with error message: " + err.getMessage())
                    );
                })
                .onFailure(err ->
                  setUpMessageContextResponse(context,
                  HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "updateTargetLanguages failed with error message: " + err.getMessage())
                );

            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.OK,
                "No Delete operations required");
            }
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              DELETION_CAN_NOT_BE_EXECUTED);
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            PROVIDED_APPLICATION_ID + context.pathParam(APPLICATION_ID)
              + WAS_NOT_FOUND_IN_THE_DATABASE);
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          ar.cause().getMessage());
      }
    });
  }

  private void cleanupEmptyCatalogs(JsonObject mongoApplication) {
    JsonArray datasets = mongoApplication.getJsonArray(DATASETS, new JsonArray());
    Set<String> remainingCatalogIds = datasets.stream()
      .map(o -> ((JsonObject) o).getString(CATALOG_ID))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    JsonArray catalogs = mongoApplication.getJsonArray("catalogs", new JsonArray());
    Iterator<Object> catalogIterator = catalogs.iterator();

    while (catalogIterator.hasNext()) {
      JsonObject catalog = (JsonObject) catalogIterator.next();
      String catalogId = catalog.getString("catalog_id");
      if (!remainingCatalogIds.contains(catalogId)) {
        catalogIterator.remove();
      }
    }
  }

  protected Future<Void> updateTargetLanguages(JsonObject mongoApplication) {

    if (mongoApplication.containsKey("translation")
      && mongoApplication.getJsonObject("translation").containsKey("targetLanguages")) {

      // Extract datasets as List<JsonObject>
      List<JsonObject> datasets = mongoApplication.getJsonArray("datasets").stream()
        .filter(obj -> obj instanceof JsonObject)
        .map(obj -> (JsonObject) obj)
        .toList();

      return getTargetLanguagesFromDatasets(datasets)
        .onSuccess(languages -> {
          Set<String> uniqueLanguages = new HashSet<>(languages);
          // English is included by default
          uniqueLanguages.add("EN");

          mongoApplication
            .getJsonObject("translation")
            .put("targetLanguages", new JsonArray(new ArrayList<>(uniqueLanguages)));
        })
        .onFailure(err -> {
          // Fallback to empty list on failure
          mongoApplication
            .getJsonObject("translation")
            .put("targetLanguages", new JsonArray());
        })
        .mapEmpty();

    } else {
      // No action required
      return Future.succeededFuture();
    }
  }

  boolean deleteApplicationDataset(JsonObject mongoApplication, String requestDatasetId,
    @NotNull RoutingContext context, String collectionName) {


    AuditLogActionDescription auditLogActionDescriptionDiscarded;
    AuditLogObject auditLogObject;
    AuditLogAction auditLogActionDiscarded;
    AuditLogActionDescription auditLogActionDescriptionDeleted;

    if(DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)) {
      auditLogActionDescriptionDiscarded = AuditLogActionDescription.DATA_ACCESS_APPLICATION_DISCARDED;
      auditLogObject = AuditLogObject.DATA_ACCESS_APPLICATION;
      auditLogActionDiscarded = AuditLogAction.DATA_ACCESS_APPLICATION_DISCARDED;
      auditLogActionDescriptionDeleted = AuditLogActionDescription.DATA_ACCESS_APPLICATION_DATASET_DELETED;
    } else {
      auditLogActionDescriptionDiscarded = AuditLogActionDescription.DATA_REQUEST_APPLICATION_DISCARDED;
      auditLogObject = AuditLogObject.DATA_REQUEST_APPLICATION;
      auditLogActionDiscarded = AuditLogAction.DATA_REQUEST_APPLICATION_DISCARDED;
      auditLogActionDescriptionDeleted = AuditLogActionDescription.DATA_REQUEST_APPLICATION_DATASET_DELETED;
    }


    // Loop every dataset to check if provided dataset id is contained
    JsonArray mongoDatasets = mongoApplication.getJsonArray(DATASETS);
    boolean modified = false;

    Iterator<Object> datasetIterator = mongoDatasets.iterator();
    while (datasetIterator.hasNext()) {
      Object datasetObj = datasetIterator.next();
      if (datasetObj instanceof JsonObject mongoDataset && mongoDataset.getString(DATASET_ID)
        .equals(requestDatasetId)) {
        if (mongoDatasets.size() == 1) {
          JsonObject deleteById = new JsonObject().put(ID, mongoApplication.getValue(ID));
          dbHandler.deleteById(collectionName, deleteById);

          //Audit Log
          dbHandler.updateLinkableFields(AUDIT_LOG_COLLECTION, mongoApplication.getString(ID));
          JsonObject datasetObjJson = (JsonObject) datasetObj;
          JsonArray distributions = datasetObjJson.getJsonArray(DISTRIBUTIONS);
          String distributionsString = auditLogManager.extractDistributionIds(
            distributions.toString());
          String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
            auditLogActionDescriptionDiscarded.toString(),
            mongoApplication.getString(ID), mongoApplication.getString(TITLE),
            datasetObjJson.getString(DATASET_ID), distributionsString);
          auditLogManager.insertAuditLog(auditLogObject.toString(),
            mongoApplication.getString(ID),
            auditLogActionDiscarded.toString(), auditLogActionDescription, false, context);

          return false;
        } else {
          datasetIterator.remove();
          modified = true;

          // If dataset removed, delete section6 too.
          deleteSection6ByDataset(mongoApplication, mongoDataset.getString(CATALOG_ID));

          //Audit Log
          JsonObject datasetObjJson = (JsonObject) datasetObj;
          JsonArray distributions = datasetObjJson.getJsonArray(DISTRIBUTIONS);
          String distributionsString = auditLogManager.extractDistributionIds(
            distributions.toString());
          String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
            auditLogActionDescriptionDeleted.toString(),
            datasetObjJson.getString(DATASET_ID), distributionsString,
            mongoApplication.getString(ID), mongoApplication.getString(TITLE));
          auditLogManager.insertAuditLog(auditLogObject.toString(),
            mongoApplication.getString(ID), AuditLogAction.DATASET_DELETED.toString(),
            auditLogActionDescription, true, context);
        }
      }
    }
    return modified;
  }

  /**
   * Delete a list of distributions for the Data Access Application collection.
   */
  public void deleteDistributions(@NotNull RoutingContext context, String collectionName) {

    // Get the application id
    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          if (DRAFT.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))
          || ON_GOING.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            JsonArray requestContent = context.body().asJsonArray();
            requestContent.forEach(datasetDistributionCombo -> {
              String requestDatasetId = ((JsonObject) datasetDistributionCombo).getString(
                DATASET_ID);
              String requestDistributionId = ((JsonObject) datasetDistributionCombo).getString(
                DISTRIBUTION_ID);
              deleteApplicationDatasetDistribution(mongoApplication, requestDatasetId,
                requestDistributionId, context, collectionName);

            });
            cleanupEmptyCatalogs(mongoApplication);

            updateTargetLanguages(mongoApplication)
              .onSuccess(v -> {
                replaceApplication(mongoApplication, collectionName)
                  .onSuccess(v2 ->
                    setUpMessageContextResponse(context,
                      HttpResponseStatus.OK,
                      "Delete operation/s completed")
                  )
                  .onFailure(err ->
                    setUpMessageContextResponse(context,
                      HttpResponseStatus.INTERNAL_SERVER_ERROR,
                      "replaceApplication failed with error message: " + err.getMessage())
                  );
              })
              .onFailure(err ->
                setUpMessageContextResponse(context,
                  HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "updateTargetLanguages failed with error message: " + err.getMessage())
              );
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              DELETION_CAN_NOT_BE_EXECUTED);
          }
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            PROVIDED_APPLICATION_ID + context.pathParam(APPLICATION_ID)
              + WAS_NOT_FOUND_IN_THE_DATABASE);
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          ar.cause().getMessage());
      }
    });
  }

  void deleteApplicationDatasetDistribution(JsonObject mongoApplication, String requestDatasetId,
    String requestDistributionId, @NotNull RoutingContext context, String collectionName) {


    AuditLogActionDescription auditLogActionDescriptionDiscarded;
    AuditLogObject auditLogObject;
    AuditLogAction auditLogActionDiscarded;
    AuditLogActionDescription auditLogActionDescriptionDeleted;
    AuditLogActionDescription auditLogActionDescriptionDistributionDeleted;

    if(DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)) {
      auditLogActionDescriptionDiscarded = AuditLogActionDescription.DATA_ACCESS_APPLICATION_DISCARDED;
      auditLogObject = AuditLogObject.DATA_ACCESS_APPLICATION;
      auditLogActionDiscarded = AuditLogAction.DATA_ACCESS_APPLICATION_DISCARDED;
      auditLogActionDescriptionDeleted = AuditLogActionDescription.DATA_ACCESS_APPLICATION_DATASET_DELETED;
      auditLogActionDescriptionDistributionDeleted = AuditLogActionDescription.DATA_ACCESS_APPLICATION_DISTRIBUTION_DELETED;

    } else {
      auditLogActionDescriptionDiscarded = AuditLogActionDescription.DATA_REQUEST_APPLICATION_DISCARDED;
      auditLogObject = AuditLogObject.DATA_REQUEST_APPLICATION;
      auditLogActionDiscarded = AuditLogAction.DATA_REQUEST_APPLICATION_DISCARDED;
      auditLogActionDescriptionDeleted = AuditLogActionDescription.DATA_REQUEST_APPLICATION_DATASET_DELETED;
      auditLogActionDescriptionDistributionDeleted = AuditLogActionDescription.DATA_REQUEST_APPLICATION_DISTRIBUTION_DELETED;
    }

    // Loop every dataset to check if provided dataset id is contained
    JsonArray mongoDatasets = mongoApplication.getJsonArray(DATASETS);

    Iterator<Object> datasetIterator = mongoDatasets.iterator();
    while (datasetIterator.hasNext()) {
      Object datasetObj = datasetIterator.next();
      if (datasetObj instanceof JsonObject mongoDataset && mongoDataset.getString(DATASET_ID)
        .equals(requestDatasetId)) {
        JsonArray mongoDistributions = mongoDataset.getJsonArray(DISTRIBUTIONS);
        Iterator<Object> distributionIterator = mongoDistributions.iterator();
        while (distributionIterator.hasNext()) {
          Object distributionObj = distributionIterator.next();
          if (distributionObj instanceof JsonObject mongoDistribution
            && mongoDistribution.getString(DISTRIBUTION_ID).equals(requestDistributionId)) {
            if (mongoDistributions.size() == 1) {
              if (mongoDatasets.size() == 1) {
                JsonObject deleteById = new JsonObject().put(ID, mongoApplication.getValue(ID));
                dbHandler.deleteById(collectionName, deleteById);

                //Audit Log
                dbHandler.updateLinkableFields(AUDIT_LOG_COLLECTION,
                  mongoApplication.getString(ID));
                String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                  auditLogActionDescriptionDiscarded.toString(),
                  mongoApplication.getString(ID), mongoApplication.getString(TITLE),
                  requestDatasetId, requestDistributionId);
                auditLogManager.insertAuditLog(auditLogObject.toString(),
                  mongoApplication.getString(ID),
                  auditLogActionDiscarded.toString(),
                  auditLogActionDescription, false, context);

                return;
              } else {
                datasetIterator.remove();
                mongoApplication.put(DATE_MODIFIED, Instant.now());

                // If  dataset removed, remove section 6 too.
                deleteSection6ByDataset(mongoApplication, mongoDataset.getString(CATALOG_ID));

                //Audit Log
                String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                  auditLogActionDescriptionDeleted.toString(),
                  requestDatasetId, requestDistributionId, mongoApplication.getString(ID),
                  mongoApplication.getString(TITLE));
                auditLogManager.insertAuditLog(auditLogObject.toString(),
                  mongoApplication.getString(ID), AuditLogAction.DATASET_DELETED.toString(),
                  auditLogActionDescription, true, context);
              }
            } else {
              distributionIterator.remove();
              mongoApplication.put(DATE_MODIFIED, Instant.now());

              //Audit Log
              String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                auditLogActionDescriptionDistributionDeleted.toString(),
                requestDistributionId, requestDatasetId, mongoApplication.getString(ID),
                mongoApplication.getString(TITLE));
              auditLogManager.insertAuditLog(auditLogObject.toString(),
                mongoApplication.getString(ID), AuditLogAction.DISTRIBUTION_DELETED.toString(),
                auditLogActionDescription, true, context);
            }
          }
        }
      }
    }
  }

  JsonArray getDistributions(JsonArray jsonArray, String value) {
    for (Object element : jsonArray) {
      if (element instanceof JsonObject jsonObject) {
        String foundValue = jsonObject.getString(DATASET_ID);
        if (StringUtils.isNotEmpty(foundValue) && Strings.CI.equals(value, foundValue)) {
          return jsonObject.getJsonArray(DISTRIBUTIONS);
        }
      }
    }
    return null;
  }

  protected void putDistributionsApplication(@NotNull RoutingContext context, String collectionName) {

    // Retrieve all update futures for each application
    List<Future> updateFutures = updateApplicationsAsync(context, collectionName);

    // Wait until all application updates are done
    CompositeFuture.all(updateFutures).onComplete(ar -> {

      JsonArray updatedApplications = new JsonArray();

      if (ar.failed()) {
        log.error("Some update operations failed", ar.cause());
        context.fail(ar.cause());
        return;
      }

      // Collect all non-empty updated applications
      for (Object result : ar.result().list()) {
        JsonObject updatedApplication = (JsonObject) result;
        if (updatedApplication != null && !updatedApplication.isEmpty()) {
          updatedApplications.add(updatedApplication);
        }
      }

      if (updatedApplications.isEmpty()) {
        setUpContextResponse(context, HttpResponseStatus.NO_CONTENT);
      } else {
        setUpContextResponse(context, HttpResponseStatus.OK, updatedApplications.encode());
      }
    });
  }

  List<Future> updateApplicationsAsync(@NotNull RoutingContext context, String collectionName) {

    //Retrieve all applications from the request
    JsonArray requestApplications = context.body().asJsonObject().getJsonArray("applications");
    //Define the array of Futures that need to be returned
    List<Future> futures = new ArrayList<>();

    // For every application in the request, invoke the asynchronous call to perform the update of the application
    requestApplications.forEach(
      requestApplication -> futures.add(updateApplicationAsync(requestApplication, context, collectionName)));

    return futures;
  }

  private Future<JsonObject> updateApplicationAsync(Object requestApplication,
    @NotNull RoutingContext context, String collectionName) {

    Promise<JsonObject> promise = Promise.promise();

    // A map of application <application_id, title> that will store the application that is being updated
    JsonObject updatedApplication = new JsonObject();
    //Retrieve the application from Mongo Database
    Future<JsonObject> mongoApplicationFuture = findApplication(requestApplication,
      Context.getEcasId(context), collectionName);
    //Process the result asynchronously
    mongoApplicationFuture.onComplete(ar -> {
      if (ar.succeeded() && ar.result() != null) {

        // Get the application document from the database
        JsonObject mongoApplication = ar.result();

        //Loop for every dataset
        JsonArray requestDatasets = context.body().asJsonObject().getJsonArray(DATASETS);

        requestDatasets.forEach(requestDataset -> {

          JsonArray mongoDatasets = mongoApplication.getJsonArray(DATASETS);
          String requestDatasetIdValue = ((JsonObject) requestDataset).getString(DATASET_ID);
          if (hasElementWithValue(mongoDatasets, DATASET_ID, requestDatasetIdValue)) {

            // Get the distributions for the specific dataset
            JsonArray mongoDistributions = getDistributions(mongoDatasets,
              requestDatasetIdValue);

            // The provided dataset exists, so we have to parse the distributions
            JsonArray requestDistributions = ((JsonObject) requestDataset).getJsonArray(
              DISTRIBUTIONS);
            requestDistributions.forEach(requestDistribution -> {

              String requestDistributionIdValue = ((JsonObject) requestDistribution).getString(
                DISTRIBUTION_ID);
              if (!hasElementWithValue(mongoDistributions, DISTRIBUTION_ID,
                requestDistributionIdValue)) {
                // The provided distribution does not exist, so will have to add it
                //Prepare the distribution
                mongoDistributions.add(requestDistribution);
                // Update the date_added of the dataset
                updateDatasetDateAdded(mongoDatasets, requestDatasetIdValue,
                  collectionName);
                //Add the application id and the title to the response map
                updatedApplication.put(ID, mongoApplication.getString(ID));
                updatedApplication.put(VERSION, mongoApplication.getString(VERSION));
                updatedApplication.put(TITLE, mongoApplication.getString(TITLE));
              } else {
                // Distribution is found in Mongo database
                log.info("{} Distribution with id: {} was found in the Mongo database, so will not add it",
                  collectionName, requestDistributionIdValue);
              }
            });
          } else {
            // The provided dataset does not exist, so will have to add it with all of its distributions
            // prepare Dataset to be added
            mongoDatasets.add(prepareDataset(requestDataset, collectionName));
            //Add the application id and the title to the response map
            updatedApplication.put(ID, mongoApplication.getString(ID));
            updatedApplication.put(VERSION, mongoApplication.getString(VERSION));
            updatedApplication.put(TITLE, mongoApplication.getString(TITLE));
          }

        });
        //Update the application to Mongo database only if changes need to be applied
        if (!updatedApplication.isEmpty()) {

          mongoApplication.put("catalogs", createCatalogs(mongoApplication.getJsonArray(DATASETS)));

          // 🆕 Ensure targetLanguages are updated BEFORE the single replace
          updateTargetLanguages(mongoApplication)
            .compose(v -> replaceApplication(mongoApplication, collectionName))
            .onSuccess(v -> {
              deleteDistributionsFromBasket(requestDatasets, context);
              promise.complete(updatedApplication);
            })
            .onFailure(promise::fail);

        } else {
          deleteDistributionsFromBasket(requestDatasets, context);
          promise.complete(updatedApplication);
        }

      } else {
        log.info(collectionName
            + " Application with id: {} was not found in the Mongo database",
          ((JsonObject) requestApplication).getString(APPLICATION_ID));
        promise.fail("Application was not found in the Mongo database");
      }

    });

    return promise.future();
  }

  private Future<JsonObject> findApplication(Object datasetQueryDocument, String userId, String collectionName) {

    JsonObject applicationQueryDocument = new JsonObject();
    applicationQueryDocument.put(ID, ((JsonObject) datasetQueryDocument).getString(APPLICATION_ID));
    applicationQueryDocument.put(USER_ID, userId);

    Promise<JsonObject> promise = Promise.promise();

    mongoClient.findOne(collectionName, applicationQueryDocument, null, ar -> {
      if (ar.succeeded()) {
        JsonObject application = ar.result();
        promise.complete(application);
      } else {
        promise.fail(ar.cause());
      }
    });

    return promise.future();
  }

  /**
   * Extracts and returns the set of target languages from the given list of datasets by querying their catalogues.
   * <p>
   * For each dataset, this method extracts the {@code catalog_id}, fetches the corresponding catalogue via
   * {@code hubSearchClient}, and collects all {@code language.id} values found in the catalogue's "language" array.
   * Language codes are returned in uppercase (e.g., "EN", "DE").
   * <p>
   * If no catalogue IDs are found, an empty set is returned immediately.
   * If a catalogue is missing or does not contain a "language" field, a warning is logged.
   *
   * @param datasets List of dataset JSON objects, each expected to contain a {@code catalog_id} field.
   * @return A {@code Future<Set<String>>} that resolves with the set of unique target language codes.
   */
  protected Future<Set<String>> getTargetLanguagesFromDatasets(List<JsonObject> datasets) {
    Set<String> uniqueCatalogueIds = datasets.stream()
      .map(dataset -> dataset.getString(CATALOG_ID))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    if (uniqueCatalogueIds.isEmpty()) {
      log.warn("No catalogue IDs found in datasets.");
      return Future.succeededFuture(Collections.emptySet());
    }

    List<Future> fetchFutures = new ArrayList<>();

    for (String catalogueId : uniqueCatalogueIds) {
      fetchFutures.add(hubSearchClient.getCatalogue(catalogueId));
    }

    return CompositeFuture.all(fetchFutures)
      .map(cf -> {
        Set<String> targetLanguages = new HashSet<>();

        for (int i = 0; i < uniqueCatalogueIds.size(); i++) {
          JsonObject catalogueWrapper = cf.resultAt(i);
          JsonObject catalogue = catalogueWrapper.getJsonObject("result");

          if (catalogue != null && catalogue.containsKey("language")) {
            JsonArray languageArray = catalogue.getJsonArray("language");
            for (int j = 0; j < languageArray.size(); j++) {
              JsonObject langObj = languageArray.getJsonObject(j);
              if (langObj.containsKey("id")) {
                targetLanguages.add(langObj.getString("id").toUpperCase(Locale.ENGLISH));
              }
            }
          } else {
            log.warn("Catalogue does not contain language field: {}", catalogueWrapper.encodePrettily());
          }
        }

        return targetLanguages;
      });
  }

  /**
   * Triggers translation of both the form content and attached documents for a given application.
   *
   * <p>This method coordinates translation requests to the hub-translation service. It performs the following steps:
   * <ol>
   *   <li>Extracts document IDs from the form.</li>
   *   <li>Builds and sends a translation request for the form content.</li>
   *   <li>Fetches attached documents as Base64 via {@code hubStoreClient}.</li>
   *   <li>For each document, builds and sends a separate translation request.</li>
   *   <li>Tracks translation status in a {@link TranslationResult} object and persists it in MongoDB.</li>
   * </ol>
   *
   * <p>All form and file translation requests are executed asynchronously. Failures in either path
   * are recorded in the {@link TranslationResult} object. If any translation fails, a single audit log
   * is created for the application using {@link #logTranslationFailureOnce(String, String, String)}, ensuring
   * duplicates are avoided. The translation metadata is persisted even if some requests fail.
   *
   * @param applicationId   Unique ID of the application being translated.
   * @param form            The original application form JSON.
   * @param applicationType MongoDB collection name (e.g., "DATA_REQUEST_APPLICATION").
   * @param sourceLang      Language code of the original content (e.g., "EN").
   * @param targetLanguages List of target language codes to translate into (e.g., ["DE", "FR"]).
   */
  protected void triggerFormTranslationRequest(
    String applicationId,
    JsonObject form,
    String applicationType,
    String sourceLang,
    List<String> targetLanguages,
    JsonObject englishFormTranslation,
    Boolean changeTargetLanguages) {

    TranslationResult result = new TranslationResult();

    // Trigger form translation request
    JsonObject formPayload = hubTranslationClient.buildTranslationRequestPayload(
      applicationId, form, applicationType, sourceLang, targetLanguages);

    // Record that translation was initiated
    result.markFormPending(formPayload);

    hubTranslationClient.requestTranslation(formPayload)
      .onSuccess(id -> {
        log.info("Form translation triggered successfully for applicationId={}", applicationId);
        JsonObject update = result.toMongoUpdate(sourceLang, targetLanguages, englishFormTranslation, changeTargetLanguages);
        log.info("TriggerFormTranslationRequest: Persisting translation metadata for applicationId={}, update={}", applicationId, update.encodePrettily());

        mongoClient.updateCollection(applicationType, new JsonObject().put("_id", applicationId), update)
          .onSuccess(v -> {
            log.info("TriggerFormTranslationRequest: Translation metadata persisted successfully for applicationId={}", applicationId);

            // Unique Audit log for failure
            if (result.isFailed()) {
              logTranslationFailureOnce(applicationId, applicationType, "Translation failed");
            }
          })
          .onFailure(err -> log.error("Failed to persist translation metadata for applicationId={}, error={}", applicationId, err.getMessage(), err));
      })
      .onFailure(err -> {
        log.error("Form translation request failed for applicationId={}, err={}", applicationId, err.getMessage());
        result.markFormFailure(err);
      });
  }

  /**
   * Builds the translated forms for each target language and sets the overall translation status.
   *
   * <p>This method uses the translation dictionary to create deep copies of the original form with
   * translated values applied per language. It then compares the languages received against the expected
   * target languages to determine the translation completion status.
   *
   * <p>If any expected language is missing from the translations, the status is set to
   * {@link TranslationStatus#PARTIALLY_COMPLETED}; otherwise, it is set to
   * {@link TranslationStatus#COMPLETED}.
   *
   * @param originalForm  The original application form JSON object.
   * @param dict          Translation dictionary containing translated values organized by section and field.
   * @param expectedLangs List of expected target language codes (case-insensitive).
   * @param applicationId The ID of the application (used for logging missing languages).
   * @return JsonObject containing:
   * <ul>
   *   <li>{@code form_translations}: a JsonObject mapping language codes to translated forms</li>
   *   <li>{@code status}: a {@link TranslationStatus} indicating completeness of the translations</li>
   * </ul>
   */
  protected JsonObject buildFormTranslationsAndSetStatus(
    JsonObject originalForm,
    JsonObject dict,
    List<String> expectedLangs,
    String applicationId,
    JsonObject translatedFiles,
    String applicationType,
    JsonObject formTrans
  ) {
    JsonObject formTranslations = hubTranslationClient.buildFormTranslations(originalForm, dict, translatedFiles, applicationType, formTrans);

    Set<String> receivedLangs = formTranslations.fieldNames();

    String formStatus = ApplicationFormUtils.determineTranslationStatus(
      receivedLangs, expectedLangs, "form", applicationId
    );

    return new JsonObject()
      .put(FORM_TRANSLATIONS, formTranslations)
      .put(STATUS, formStatus);
  }

  /**
   * Applies translation updates to the application JSON object.
   *
   * <p>If the update contains {@code form_translations}, they replace the existing translations
   * in the application. The translation status of the form is updated {@code translation.form.status}
   *
   * @param application       The original application JsonObject to be updated.
   * @param translationUpdate The translation update JsonObject containing form translations and status.
   */
  protected void applyTranslationUpdate(JsonObject application, JsonObject translationUpdate) {
    if (translationUpdate.containsKey(FORM_TRANSLATIONS)) {
      application.put(FORM_TRANSLATIONS, translationUpdate.getJsonObject(FORM_TRANSLATIONS));
    }

    String status = translationUpdate.getString(STATUS);

    JsonObject translation = application.getJsonObject(TRANSLATION, new JsonObject());
    JsonObject form = translation.getJsonObject("form", new JsonObject());

    if (status != null) {
      form.put(STATUS, status);
      translation.put("form", form);
      application.put(TRANSLATION, translation);
    }
  }

  /**
   * Updates an application's translation metadata and form_translations to reflect new translated file versions.
   * <p>
   * This method performs two key tasks:
   * <ol>
   *   <li>Updates the <code>translation.files.{originalFileId}</code> entry with translated file metadata
   *       (i.e., new file ID and name for each language), and sets the appropriate translation status
   *       based on whether all expected target languages are covered.</li>
   *   <li>Recursively traverses each translated form (in <code>form_translations</code>) to find and replace
   *       references to the original file ID with the corresponding translated file reference.</li>
   * </ol>
   *
   * @param application    The full application object to update.
   * @param applicationId  The ID of the application (used for logging/debugging).
   * @param originalFileId The ID of the file that was translated.
   * @param uploadedFiles  A map of language codes to file metadata for the translated files.
   *                       Each value must include "fileId" and "fileName".
   */
  protected void applyFileTranslationUpdates(
    JsonObject application,
    String applicationId,
    String originalFileId,
    Map<String, JsonObject> uploadedFiles
  ) {
    // Update "translation.files.{fileId}" metadata
    JsonObject translation = application.getJsonObject(TRANSLATION, new JsonObject());
    JsonObject translationMeta = translation.getJsonObject("files", new JsonObject());

    JsonObject translatedFilesObject = new JsonObject();
    for (Entry<String, JsonObject> entry : uploadedFiles.entrySet()) {
      String lang = entry.getKey();
      JsonObject fileInfo = entry.getValue();

      translatedFilesObject.put(lang, new JsonObject()
        .put(FILE_ID, fileInfo.getString(FILE_ID))
        .put(FILE_NAME, fileInfo.getString(FILE_NAME))
      );
    }

    Set<String> expectedLangs = translation
      .getJsonArray(TARGET_LANGUAGES, new JsonArray())
      .stream()
      .map(Object::toString)
      .collect(Collectors.toSet());

    Set<String> uploadedLangs = uploadedFiles.keySet();

    String fileStatus = ApplicationFormUtils.determineTranslationStatus(
      uploadedLangs, expectedLangs, "file", applicationId
    );

    JsonObject fileTranslationEntry = new JsonObject()
      .put("translatedFiles", translatedFilesObject)
      .put(STATUS, fileStatus);

    translationMeta.put(originalFileId, fileTranslationEntry);
    translation.put("files", translationMeta);
    application.put(TRANSLATION, translation);

  }

  protected Future<JsonObject> createApplicantDetails(@NotNull RoutingContext context){

    Promise<JsonObject> promise = Promise.promise();

    JsonObject applicantDetails = new JsonObject();
    String ecasId = Context.getEcasId(context);
    applicantDetails.put("ecasId", ecasId);

    authClient.getAllUsers(context,true)
      .onSuccess(users -> {
        Optional<JsonObject> matchingUser = users.stream()
          .map(obj -> (JsonObject) obj)
          .filter(user -> ecasId.equals(user.getString("ecasId")))
          .findFirst();

        if(matchingUser.isPresent()){
          applicantDetails.put("firstName", matchingUser.get().getString("firstName"));
          applicantDetails.put("lastName", matchingUser.get().getString("lastName"));
        }
        promise.complete(applicantDetails);
      })
      .onFailure(err -> promise.fail("Could not retrieve users, with error message: " + err.getMessage()));

    return promise.future();
  }

  protected void markTranslationError(String applicationId, String collectionName, String errorType, String fileId) {
    JsonObject query = new JsonObject().put(ID, applicationId);
    JsonObject setObject = new JsonObject()
      .put("translation.status", TranslationStatus.FAILED)
      .put(STATUS_STATUS, DRAFT)
      .put(TRANSLATION_ERROR_TYPE, errorType)
      .put(DATE_MODIFIED, Instant.now());

    if (fileId != null) {
      // Mark specific file as FAILED - only if it's still PENDING
      query.put("translation.files." + fileId + ".status", "PENDING");
      setObject.put("translation.files." + fileId + ".status", TranslationStatus.FAILED.name());
    } else {
      // Mark the form as FAILED
      setObject.put("translation.form.status", TranslationStatus.FAILED.name());
    }

    JsonObject update = new JsonObject().put("$set", setObject);

    mongoClient.findOneAndUpdate(collectionName, query, update, res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          log.info("Marked translation error [{}] for application [{}] in [{}]", errorType, applicationId, collectionName);
          logTranslationFailureOnce(applicationId, collectionName, errorType);
        } else {
          log.error("Failed to mark translation error for application [{}] in [{}] - document not found or query did not match. Query: {}",
            applicationId, collectionName, query.encode());
        }
      } else {
        log.error("Failed to mark translation error for application [{}] in [{}]", applicationId, collectionName, res.cause());
      }
    });
  }

  /**
   * Increments the translation attempts counter for either the form or a specific file of an
   * application.
   *
   * @param applicationId  The ID of the application.
   * @param collectionName The collection where the application resides.
   * @param errorType      The error returned by eTranslation.
   * @param fileId         The ID of the file for which to increment attempts, or null to increment
   *                       the form attempts.
   */
  protected void incrementTranslationAttempts(
    String applicationId,
    String collectionName,
    String errorType,
    String fileId
  ) {
    JsonObject query = new JsonObject().put(ID, applicationId);
    JsonObject setObject = new JsonObject()
      .put(TRANSLATION_ERROR_TYPE, errorType)
      .put(DATE_MODIFIED, Instant.now());

    String path;
    if (fileId != null) {
      path = "translation.files." + fileId + ".";
      // Ensure the file is still marked as PENDING, in case another attempt succeeded in the meantime
      query.put("translation.files." + fileId + ".status", TranslationStatus.PENDING.name());
    } else {
      path = "translation.form.";
    }

    // Build the update to set attempts to 0 if null, then increment by 1
    setObject
      .put(path + "attempts", new JsonObject().put("$ifNull", 0))
      .put(path + "lastRetryAt", Instant.now().toString());

    JsonObject update = new JsonObject()
      .put("$set", setObject)
      .put("$inc", new JsonObject().put(path, 1));

    mongoClient.findOneAndUpdate(collectionName, query, update, res -> {
      if (res.succeeded()) {
        log.info(
          "Incremented translation attempts for application [{}] in [{}]",
          applicationId,
          collectionName
        );
      } else {
        log.error(
          "Failed to increment translation attempts for application [{}] in [{}]",
          applicationId,
          collectionName,
          res.cause()
        );
      }
    });
  }

  /**
   * Inserts an audit log entry indicating that a translation has failed for the given application.
   * Determines the audit action and object based on the collection type (Data Access or Data Request),
   * and logs a warning if the collection type is unknown.
   *
   * @param applicationId the ID of the application whose translation failed
   * @param collectionName the collection where the application resides (e.g., DATA_ACCESS_APPLICATION_COLLECTION)
   * @param title the application title, used in the audit log description
   * @param errorType a description of the error that caused the translation failure
   */
  private void createTranslationFailedAuditLog(String applicationId, String collectionName, String title, String errorType) {
    String auditDescription;
    String auditAction;
    String auditObject;

    if (DATA_REQUEST_APPLICATION_COLLECTION.equals(collectionName)) {
      auditDescription = auditLogManager.prepareAuditLogActionDescription(
        AuditLogActionDescription.DATA_REQUEST_APPLICATION_TRANSLATION_FAILED.toString(),
        applicationId, title, errorType
      );
      auditAction = AuditLogAction.DATA_REQUEST_APPLICATION_TRANSLATION_FAILED.toString();
      auditObject = AuditLogObject.DATA_REQUEST_APPLICATION.toString();
    } else if (DATA_ACCESS_APPLICATION_COLLECTION.equals(collectionName)) {
      auditDescription = auditLogManager.prepareAuditLogActionDescription(
        AuditLogActionDescription.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString(),
        applicationId, title, errorType
      );
      auditAction = AuditLogAction.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString();
      auditObject = AuditLogObject.DATA_ACCESS_APPLICATION.toString();
    } else {
      log.warn("Unknown collection [{}] for translation failed audit log", collectionName);
      return;
    }

    auditLogManager.insertAuditLog(
      auditObject,
      applicationId,
      auditAction,
      auditDescription,
      false,
      null
    );
  }

  /**
   * Logs a translation failure for an application only once per submission.
   * <p>
   * This method checks whether a translation failure audit log has already been created
   * for the current submission of the application by comparing the timestamp of the
   * "translation.logged_failure" flag with the application's "date_submitted".
   * If no prior log exists for the current submission, it creates a translation failure
   * audit log and sets the "translation.logged_failure" flag with the current timestamp.
   * <p>
   * This ensures that repeated failures for the same submission do not generate
   * duplicate audit log entries.
   *
   * @param applicationId  the ID of the application for which the translation failed
   * @param collectionName the MongoDB collection name where the application is stored
   * @param errorType      a descriptive string indicating the type or context of the failure
   */
  private void logTranslationFailureOnce(String applicationId, String collectionName, String errorType) {
    mongoClient.findOne(collectionName, new JsonObject().put(ID, applicationId), null)
      .onSuccess(application -> {
        if (application == null) {
          log.warn("Could not fetch application [{}] for audit log", applicationId);
          return;
        }

        JsonObject translation = application.getJsonObject(TRANSLATION, new JsonObject());
        String loggedFailureStr = translation.getString(LOGGED_FAILURE, null);
        Instant dateSubmitted = Instant.parse(application.getString(DATE_SUBMITTED));

        if (loggedFailureStr != null) {
          Instant loggedFailure = Instant.parse(loggedFailureStr);
          if (loggedFailure.isAfter(dateSubmitted)) {
            log.info("Skipping duplicate translation failed audit log for application [{}]", applicationId);
            return;
          }
        }

        String title = application.getString(TITLE, "");
        createTranslationFailedAuditLog(applicationId, collectionName, title, errorType);

        JsonObject updateFlag = new JsonObject()
          .put("$set", new JsonObject().put("translation.logged_failure", Instant.now().toString()));

        mongoClient.updateCollection(collectionName, new JsonObject().put(ID, applicationId), updateFlag)
          .onSuccess(v -> log.info("Set translation.logged_failure flag for [{}]", applicationId))
          .onFailure(err -> log.error("Failed to set logged_failure flag for [{}]: {}", applicationId, err.getMessage()));
      })
      .onFailure(err -> log.error("Failed to fetch application [{}] for audit log", applicationId, err));
  }

  protected String createAuditLogActionDescriptionForChangeHDABStatus(CatalogStatus targetCatalogStatus, String collectionName, String applicationId,
    String applicationTitle, String catalogId){

      return auditLogManager.prepareAuditLogActionDescription(
        DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
          ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_HDAB_UPDATED.toString()
          : AuditLogActionDescription.DATA_REQUEST_APPLICATION_HDAB_UPDATED.toString(),
        applicationId, applicationTitle, targetCatalogStatus.getLabel(), catalogId);
  }

  private String createAuditLogActionDescriptionForChangeHDABAndDecisionStatus(CatalogStatus targetCatalogStatus, String collectionName, String applicationId,
    String applicationTitle, String catalogId, String currentDatasetStatus, DatasetStatus targetDatasetStatus, String datasetId){

    if (currentDatasetStatus == null) {
      return auditLogManager.prepareAuditLogActionDescription(
        DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
          ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_HDAB_AND_DECISION_UPDATED.toString()
          : AuditLogActionDescription.DATA_REQUEST_APPLICATION_HDAB_AND_DECISION_UPDATED.toString(),
        applicationId,
        applicationTitle, targetCatalogStatus.getLabel(), catalogId, targetDatasetStatus.getLabel(), datasetId);
    } else {
      return auditLogManager.prepareAuditLogActionDescription(
        DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
          ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_DATASET_UPDATED.toString()
          : AuditLogActionDescription.DATA_REQUEST_APPLICATION_DATASET_UPDATED.toString(),
        applicationId,
        applicationTitle, targetCatalogStatus.getLabel(), datasetId);
    }
  }

  protected String createAuditLogActionDescriptionForApplicationUpdate(
    String applicationId, String applicationTitle, ApplicationStatus targetApplicationStatus, String collectionName) {

    return auditLogManager.prepareAuditLogActionDescription(
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_UPDATED.toString()
        : AuditLogActionDescription.DATA_REQUEST_APPLICATION_UPDATED.toString(),
      applicationId,
      applicationTitle, targetApplicationStatus.getStatus());

  }

  private String createAuditLogActionDescriptionForChangePermitStatus(
    String applicationId, String applicationTitle, PermitStatus targetPermitStatus,
    String catalogId, String datasetId) {

    return auditLogManager.prepareAuditLogActionDescription(
      AuditLogActionDescription.DATA_ACCESS_APPLICATION_PERMIT_UPDATED.toString(), applicationId,
      applicationTitle, catalogId, datasetId, targetPermitStatus.getLabel());

  }

  protected void insertAuditLogForApplicationUpdate(String auditLogActionDescription, String collectionName, String applicationId, RoutingContext context){

    auditLogManager.insertAuditLog(
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogObject.DATA_ACCESS_APPLICATION.toString()
        : AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
      applicationId,
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogAction.DATA_ACCESS_APPLICATION_UPDATED.toString()
        : AuditLogAction.DATA_REQUEST_APPLICATION_UPDATED.toString(),
      auditLogActionDescription, true, context);
  }

  protected void insertAuditLogForHDABUpdate(String auditLogActionDescription, String collectionName, String applicationId, RoutingContext context){

    auditLogManager.insertAuditLog(
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogObject.DATA_ACCESS_APPLICATION.toString()
        : AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
      applicationId,
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogAction.DATA_ACCESS_HDAB_STATUS_UPDATED.toString()
        : AuditLogAction.DATA_REQUEST_HDAB_STATUS_UPDATED.toString(),
      auditLogActionDescription, true, context);
  }

  private void insertAuditLogForDatasetUpdate(String auditLogActionDescription, String collectionName, String applicationId, RoutingContext context){

    auditLogManager.insertAuditLog(
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogObject.DATA_ACCESS_APPLICATION.toString()
        : AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
      applicationId,
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogAction.DATA_ACCESS_DATASET_STATUS_UPDATED.toString()
        : AuditLogAction.DATA_REQUEST_DATASET_STATUS_UPDATED.toString(),
      auditLogActionDescription, true, context);
  }

  private void insertAuditLogForPermitUpdate(String auditLogActionDescription, String applicationId, RoutingContext context){

    auditLogManager.insertAuditLog(
      AuditLogObject.DATA_ACCESS_APPLICATION.toString(),
      applicationId,
      AuditLogAction.DATA_ACCESS_PERMIT_STATUS_UPDATED.toString(),
      auditLogActionDescription, true, context);
  }



  protected JsonObject getDefaultEnglishFormTranslation(String sourceLang, JsonObject mongoApplication){

    JsonObject formTranslations = new JsonObject();

    if("EN".equalsIgnoreCase(sourceLang)) {
      JsonObject form = mongoApplication.getJsonObject("form").copy();
      JsonObject translatedFiles = mongoApplication.getJsonObject(TRANSLATION, new JsonObject())
        .getJsonObject("files", new JsonObject());
      if (translatedFiles != null) {
        for (String originalFileId : translatedFiles.fieldNames()) {
          JsonObject fileEntry = translatedFiles.getJsonObject(originalFileId);

          JsonObject translatedPerLang = fileEntry.getJsonObject("translatedFiles");
          if (translatedPerLang != null && translatedPerLang.containsKey("en")) {
            JsonObject translatedFile = translatedPerLang.getJsonObject("en");
            String newFileId = translatedFile.getString("fileId");
            String newFileName = translatedFile.getString(FILE_NAME);

            ApplicationFormUtils.updateTranslatedFileReferencesInForm(
              form, originalFileId, newFileId, newFileName
            );
          }
        }
      }
      formTranslations.put("en", form);
    }

    return formTranslations;
  }

  public void changePermitStatus(@NotNull RoutingContext context) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String catalogId = context.pathParam(CATALOG_ID);
    String datasetId = context.pathParam(DATASET_ID);
    String permitStatusString = context.body().asJsonObject().getString(STATUS);
    String comment = context.body().asJsonObject().getString(COMMENT);

    changePermitStatus(applicationId, catalogId, datasetId, permitStatusString, comment,
      null, context, true);

  }

  private void updatePermitStatus(JsonObject application, String catalogId, String datasetId,
    PermitStatus permitStatus, String permitFile, String reason) {
    JsonArray datasets = application.getJsonArray("datasets");
    modifyDatasetBasedOnDispatcherPermit(datasets, catalogId, datasetId, permitStatus, permitFile,
      reason);
  }

  private void modifyDatasetBasedOnDispatcherPermit(JsonArray mongoDatasets, String catalogId,
    String datasetId, PermitStatus permitStatus, String permitFile, String reason) {

    for (Object mongoDatasetObj : mongoDatasets) {

      JsonObject mongoDataset = (JsonObject) mongoDatasetObj;

      String mongoCatalogId = mongoDataset.getString("catalog_id");
      String mongoDatasetId = mongoDataset.getString("dataset_id");
      JsonArray permits = mongoDataset.getJsonArray("permits");

      if (catalogId.equals(mongoCatalogId) && datasetId.equalsIgnoreCase(mongoDatasetId)) {

        if (permits == null) {
          permits = new JsonArray();
        } else {
          permits.forEach(item -> {
            JsonObject permit = (JsonObject) item;
            permit.put("latest", 0);
          });
        }

        JsonObject permit = new JsonObject();
        permit.put("permit_received", Instant.now());
        permit.put("latest", 1);

        if (permitFile != null) {
          permit.put("permit_file", permitFile);
        }

        if (permitStatus != null) {
          permit.put("permit_status", permitStatus.getLabel());
        }

        if (reason != null) {
          permit.put("reason", reason);
        }

        permits.add(permit);
        mongoDataset.put("permits", permits);

      }
    }
  }

  protected boolean hasAtLeastOneAdditionalInformationAsLatest(JsonArray catalogs) {
    if (catalogs == null || catalogs.isEmpty()) {
      return false;
    }

    for (Object catalogObj : catalogs) {
      if (!(catalogObj instanceof JsonObject)) {
        continue;
      }

      JsonObject catalog = (JsonObject) catalogObj;
      JsonArray statuses = catalog.getJsonArray("statuses");

      if (statuses == null || statuses.isEmpty()) {
        continue;
      }

      for (Object statusObj : statuses) {
        if (!(statusObj instanceof JsonObject)) {
          continue;
        }

        JsonObject statusEntry = (JsonObject) statusObj;

        int latest = statusEntry.getInteger("latest", 0);
        String status = statusEntry.getString("status");

        if (latest == 1 && "ADDITIONAL_INFORMATION".equals(status)) {
          // Found a catalog where the latest status is ADDITIONAL_INFORMATION
          return true;
        }
      }
    }

    // No catalog matched
    return false;
  }

  protected void changeCatalogToSubmitted(
    JsonObject application,
    String catalogId,
    String collectionName
  ) {
    String applicationId = application.getString("_id");

    // 1. Remove dispatcher_status from the SENT status entry
    JsonObject catalog = findCatalogById(application, catalogId);
    if (catalog != null) {
      removeDispatcherStatus(catalog);
    }

    // 2. Update catalog status to SUBMITTED
    updateCatalogStatus(application, catalogId, CatalogStatus.SUBMITTED);

    // 3. Promote application
    updateApplicationStatus(
      application,
      new JsonObject().put("status", ApplicationStatus.ON_GOING.getStatus()),
      null
    );

    insertAuditLogForApplicationUpdate(
      createAuditLogActionDescriptionForApplicationUpdate(
        applicationId,
        application.getString("title"),
        ApplicationStatus.ON_GOING,
        collectionName
      ),
      collectionName,
      applicationId,
      null
    );

    saveApplication(application, collectionName).onSuccess(v -> {
      sendApplicationToOutgoingApplicationActivities(
        collectionName,
        applicationId,
        CatalogStatus.SUBMITTED,
        catalogId,
        null
      );

      insertAuditLogForHDABUpdate(
        createAuditLogActionDescriptionForChangeHDABStatus(
          CatalogStatus.SUBMITTED,
          collectionName,
          applicationId,
          application.getString("title"),
          catalogId
        ),
        collectionName,
        applicationId,
        null
      );
    });
  }

  private JsonObject findCatalogById(JsonObject app, String catalogId) {
    JsonArray catalogsArray = app.getJsonArray("catalogs", new JsonArray());
    for (Object catalogObject : catalogsArray) {
      JsonObject catalog = (JsonObject) catalogObject;
      if (catalogId.equals(catalog.getString("catalog_id"))) {
        return catalog;
      }
    }
    return null;
  }

  private void removeDispatcherStatus(JsonObject catalog) {
    JsonObject latest = getStatusWithSentAsLatest(catalog);
    if (latest != null) {
      latest.remove("dispatcher_status");
    }
  }

  protected JsonObject getStatusWithSentAsLatest(JsonObject catalog) {
    JsonArray statuses = catalog.getJsonArray("statuses", new JsonArray());

    for (Object o : statuses) {
      JsonObject status = (JsonObject) o;
      if (
        status.getInteger("latest", 0) == 1 &&
          CatalogStatus.SENT.getLabel().equalsIgnoreCase(status.getString("status"))
      ) {
        return status;
      }
    }
    return null;
  }

  protected Future<Object> saveApplication(JsonObject app, String collectionName) {
    return replaceApplication(app, collectionName)
      .mapEmpty()
      .onFailure(err ->
        log.error("Failed saving application {}: {}", app.getString("_id"), err.getMessage())
      );
  }

  protected void triggerRemainingTranslations(String applicationId, JsonObject mongoApplication, String collectionName) {
    try {
      List<String> ALL_LANGUAGES = Arrays.asList(
        "BG","FR","NL","CS","GA","PL","DA","HR","PT","DE","HU","RO","EL","IT","SK",
        "EN","LT","SL","ES","LV","FI","ET","MT","SV","IS","NB","NN"
      );

      String sourceLang = mongoApplication.getString("inputLanguage", "EN")
        .toUpperCase(Locale.ENGLISH);

      JsonObject translation = mongoApplication.getJsonObject("translation");
      JsonArray tl = translation != null ? translation.getJsonArray("targetLanguages") : null;
      if (tl == null) {
        log.warn("No translation.targetLanguages found; skipping remaining-language translations for {}", applicationId);
        return;
      }

      List<String> targetLanguages = tl.stream()
        .map(Object::toString)
        .map(s -> s.toUpperCase(Locale.ENGLISH))
        .collect(Collectors.toList());

      targetLanguages.remove(sourceLang);

      List<String> remainingLanguages = ALL_LANGUAGES.stream()
        .filter(lang -> !lang.equals(sourceLang))
        .filter(lang -> !targetLanguages.contains(lang))
        .toList();

      if (remainingLanguages.isEmpty()) {
        log.debug("No remaining languages to translate for {}", applicationId);
        return;
      }

      triggerFormTranslationRequest(
        applicationId,
        mongoApplication.getJsonObject("form"),
        collectionName,
        sourceLang,
        remainingLanguages,
        null,
        false
      );

      log.info("Triggered remaining-language translations for {} (count={})", applicationId, remainingLanguages.size());

    } catch (Exception e) {
      log.warn("Failed to trigger remaining-language translations for {}: {}", applicationId, e.getMessage());
    }
  }

}
