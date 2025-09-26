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
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.ERROR_CHECKING_DOCUMENT_EXISTENCE;
import static eu.europa.data.health.access.Constants.FILENAME_ASTERISK_EQUALS;
import static eu.europa.data.health.access.Constants.FILE_ID;
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
import static eu.europa.data.health.access.scheduled_jobs.DispatcherScheduledJob.DISPATCHER_COLLECTION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.Context;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class CommonApplicationManager extends AbstractManager {

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
  public static final String LOGGED_FAILURE="logged_failure";
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
    MongoClient mongoClient, Vertx vertx, HubStoreClient hubStoreClient, AuditLogManager auditLogManager,
                                     HubTranslationClient hubTranslationClient, HubSearchClient hubSearchClient, AuthClient authClient) {
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

  protected void performStatusUpdate(JsonObject mongoApplication, JsonObject status,
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
    if (ApplicationStatus.SUBMITTED.equals(receivedStatus)) {
      mongoApplication.put(DATE_SUBMITTED, Instant.now());
    }
    previousStatuses.add(currentStatus);
    // Set the status_history to the new status.
    status.put(STATUS_HISTORY, previousStatuses);
    status.put(DATE, Instant.now());
    if (context != null) {
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
            context.body().asJsonObject().getString(ID), context.body().asJsonObject().getString(TITLE));
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
    JsonObject country = result.getJsonObject("country");
    if (country != null) {
      if (country.containsKey("id")) {
        Object idValue = country.remove("id");
        country.put("country_id", idValue);
      }
      dataset.put("country", country);
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
          if (!ApplicationStatus.SUBMITTED.getStatus()
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
    JsonObject mongoForm = mongoApplication.getJsonObject("form");
    if (Objects.isNull(mongoForm)) {
      return;
    }
    JsonArray mongoSection6 = mongoForm.getJsonArray(SECTION_6);
    if (Objects.isNull(mongoSection6)) {
      return;
    }

    Iterator<Object> section6Iterator = mongoSection6.iterator();
    while (section6Iterator.hasNext()) {
      Object section6Obj = section6Iterator.next();
      if (section6Obj instanceof JsonObject jsonObject) {
        // Check if catalogId contains in the catalog_ids
        JsonArray mongoCatalogIds = jsonObject.getJsonArray("catalog_ids");
        if (mongoCatalogIds.contains(catalogId)) {
          section6Iterator.remove();
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

    String requestApplicationId = context.pathParam(APPLICATION_ID);
    String userId = Context.getEcasId(context);
    JsonObject query = new JsonObject().put(ID, requestApplicationId).put(USER_ID, userId);

    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to create/update an application only if status is not SUBMITTED
          if (!ApplicationStatus.SUBMITTED.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            // Proceed if the application exists
            handleFileUpload(context, APPLICATIONID, requestApplicationId, hubStoreClient, vertx);

          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Posting data can not be executed in a submitted application");
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
    log.info("{} Provided dataset can not be converted to JsonObject", collectionName );
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

  protected void deleteFile(RoutingContext context, String applicationId) {
    String fileId = context.pathParam("data_id");

    hubStoreClient.deleteFile(fileId, APPLICATIONID, applicationId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          setUpMessageContextResponse(context, HttpResponseStatus.OK,
            "File with id: " + fileId + " was deleted successfully");
        } else {
          // Handle file deletion failure
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error deleting file: " + ar.cause().getMessage());
        }
      });
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
          if (!ApplicationStatus.SUBMITTED.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            // Proceed to delete the file
            deleteFile(context, applicationId);

          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Deletion of data can not be executed in a submitted application");
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

  protected void createOrUpdateSection(@NotNull RoutingContext context, String collectionName) {

    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to create/update an application only if status is not SUBMITTED
          if (!ApplicationStatus.SUBMITTED.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

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

            if(Objects.nonNull(mongoApplication
              .getJsonObject("form")) && Objects.nonNull(mongoApplication
              .getJsonObject("form").getJsonObject("section3"))){
              section3 = mongoApplication
                .getJsonObject("form").getJsonObject("section3");
            }

            JsonObject update = new JsonObject();

            if(DATA_ACCESS_APPLICATION_COLLECTION.equals(collectionName)){
              update.put(FORM + sectionNumber,
                (Strings.CI.equals(sectionNumber, SECTION_6)) ? sameAsSection3Array(context.body().asJsonArray(), section3)
                  : sameAsSection3Object(context.body().asJsonObject(), sectionNumber, section3));
            } else if (DATA_REQUEST_APPLICATION_COLLECTION.equals(collectionName)) {
              update.put(FORM + sectionNumber,
                (Strings.CI.equals(sectionNumber, SECTION_6)) ? context.body().asJsonArray()
                  : sameAsSection3Object(context.body().asJsonObject(), sectionNumber, section3));

            }

            update.put(DATE_MODIFIED, Instant.now());

            JsonObject mongoForm = mongoApplication.getJsonObject("form");
            if(Objects.nonNull(mongoForm)){
              updateSameAsSection3(mongoForm, update, Integer.valueOf(context.pathParam(SECTION_NUMBER)),
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
    if (Objects.isNull(section3)) return sectionFormArray;

    JsonObject legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalOrNaturalPerson)) return sectionFormArray;

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

  private JsonObject sameAsSection3Object(JsonObject sectionForm, String sectionNumber, JsonObject section3) {
    if (Objects.isNull(section3)) return sectionForm;

    JsonObject legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalOrNaturalPerson)) return sectionForm;

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
      sectionForm.put(NAME_OF_THE_ORGANISATION, section3.getString(CONTACT_PERSON_ORGANISATION_NAME));
      sectionForm.put(BUSINESS_IDENTIFIER_ORGANIZATION, section3.getString(CONTACT_PERSON_BUSINESS_ID));
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


   void updateSameAsSection3(JsonObject mongoForm, JsonObject updateForm, Integer sectionNumber, Boolean isAccess) {
    JsonObject section3 = mongoForm.getJsonObject(SECTION_3);

    if (Objects.isNull(section3)) return;

    switch (sectionNumber) {
      case 4:
        updateSection4(updateForm.getJsonObject("form.section4"), section3);
        break;
      case 5:
        updateSection5(updateForm.getJsonObject("form.section5"), section3);
        break;
      case 6:
        if (Boolean.TRUE.equals(isAccess)) updateSection6(updateForm.getJsonArray("form.section6"), section3);
        break;
      case 8:
        if (Boolean.TRUE.equals(isAccess)) updateSection8(updateForm.getJsonObject("form.section8"), section3);
        break;
      default:
        break;
    }
  }

  private void updateSection4(JsonObject section4, JsonObject section3) {
    if (Objects.isNull(section4)) return;

    boolean sameAsContactPerson = section4.getBoolean(SAME_AS_CONTACT_PERSON, false);

    if (!sameAsContactPerson) return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) return;

    String key = legalPerson.getString("key");
    if ("a".equals(key)) {
      updateLegalPerson(section4, section3);
    } else if ("b".equals(key)) {
      updateNaturalPerson(section4, section3);
    }
  }

  private void updateSection5(JsonObject section5, JsonObject section3) {
    if (Objects.isNull(section5)) return;

    updatePerson(section5, section3, PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, PERSON_RESPONSIBLE_NAME);

    updatePerson(section5, section3, PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, PERSON_RESEARCH_NAME);
  }

  private void updateSection8(JsonObject section8, JsonObject section3) {
    if (Objects.isNull(section8)) return;

    boolean sameAsContactPerson = section8.getBoolean(SAME_AS_CONTACT_PERSON, false);

    if (!sameAsContactPerson) return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) return;

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
    if (!sameAsContactPerson) return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) return;

    String personType = legalPerson.getString("key");
    if ("a".equals(personType)) {
      section.put(nameKey, section3.getString(LEGAL_PERSON_NAME, ""));
    } else if ("b".equals(personType)) {
      section.put(nameKey, section3.getString(NATURAL_PERSON_NAME, ""));
    }
  }

  private void updateSection6(JsonArray section6, JsonObject section3) {
    if (Objects.isNull(section6)) return;

    for (int i = 0; i < section6.size(); i++) {
      JsonObject country = section6.getJsonObject(i);
      boolean informationProviderSameAsContactPerson = country.getBoolean(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, false);

      if (informationProviderSameAsContactPerson) {
        updateCountryInformationProvider(country, section3);
      }
    }
  }

  private void updateCountryInformationProvider(JsonObject country, JsonObject section3) {
    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) return;

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
    section4.put(NAME_OF_THE_ORGANISATION, section3.getString(CONTACT_PERSON_ORGANISATION_NAME, ""));
    section4.put(BUSINESS_IDENTIFIER_ORGANIZATION, section3.getString(CONTACT_PERSON_BUSINESS_ID, ""));
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
                .compose(res2 -> validateNestedFileFor("variablesForDataExtractionRelativesAttachments", context))
                .compose(res2 -> validateNestedFileFor("variablesForDataExtractionAttachments", context))
                .compose(res2 -> validateNestedFileFor("variablesToBeUsedInDataExtractionAttachment", context))
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
      validateFile(fieldNameContentList, context.pathParam(APPLICATION_ID), context.body().asJsonObject(), fieldName)
        .onComplete(result -> {
          if(fieldNameContentList.isEmpty()){
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

  private Future<Void> validateFile(@NotNull JsonArray objectList, String applicationId, JsonObject object, @NotNull String fieldName) {
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
        if(objectList.isEmpty()){
          object.remove(fieldName);
        }
        promise.complete();
      });

    return promise.future();
  }

  protected String decodeFilename(String contentDisposition){
    if (StringUtils.isNotEmpty(contentDisposition) && contentDisposition.contains(FILENAME_ASTERISK_EQUALS)) {
      String part = contentDisposition.substring(contentDisposition.indexOf(FILENAME_ASTERISK_EQUALS) + FILENAME_ASTERISK_EQUALS.length()).trim();
      if (part.startsWith("UTF-8''")) {
        part = part.substring("UTF-8''".length());
      }
      return URLDecoder.decode(part, StandardCharsets.UTF_8);
    }
    return "Error_decoding_filename";
  }

  protected void cleanSection2(JsonObject mongoApplicationForm) {
    JsonObject section2 = mongoApplicationForm.getJsonObject(SECTION_2);
    if(Objects.isNull(section2)){
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
    if(Objects.isNull(section3)){
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
    if(Objects.isNull(section4)){
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

  protected void sendApplicationForDispatch(JsonObject mongoApplication, String database) {
    mongoApplication.put(DATE_DISPATCHED, Instant.now());
    // Clone the application and remove the 'translation' object
    JsonObject applicationForDispatch = mongoApplication.copy();

    JsonArray targetLanguages = applicationForDispatch.getJsonObject(TRANSLATION).getJsonArray(
      TARGET_LANGUAGES);

    applicationForDispatch.remove(TRANSLATION);

    JsonObject cronConfiguration = new JsonObject().put(Constants.APPLICATION_TYPE, database).put(STATUS, "pending").put("retries", 0);
    JsonObject dispatcherRecord = new JsonObject().put("configuration", cronConfiguration).put(
      "application", applicationForDispatch).put(TARGET_LANGUAGES,targetLanguages);

    mongoClient.save(DISPATCHER_COLLECTION, dispatcherRecord, res -> {
      if (res.succeeded()) {
        log.info(DISPATCHER_COLLECTION + " Saved dispatcher application with id: {}",
          res.result());
      } else {
        log.error(DISPATCHER_COLLECTION + " Failed to save dispatcher application, with error message: {}", res.cause().getMessage());
      }
    });
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
          .put("status.status", new JsonObject()
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

  protected void rollbackStatusToDraft(String applicationId, String database) {
    JsonObject query = new JsonObject().put(ID, applicationId);
    JsonObject rollbackStatus = new JsonObject().put("$set",
      new JsonObject().put("status.status", "DRAFT"));

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
          log.info("{} Application with id: {} was updated successfully", collectionName,
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

  /**
   * Delete a list of datasets for the Data Request Application collection.
   */
  public void deleteDatasets(@NotNull RoutingContext context, String collectionName) {

    // Get the application id
    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to delete an application only if status is not SUBMITTED
          if (!ApplicationStatus.SUBMITTED.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            JsonArray requestContent = context.body().asJsonArray();
            AtomicBoolean applicationModified = new AtomicBoolean(false);
            requestContent.forEach(requestDataset -> {
              String requestDatasetId = ((JsonObject) requestDataset).getString(DATASET_ID);
              if(deleteApplicationDataset(mongoApplication, requestDatasetId, context, collectionName))
                applicationModified.set(true);
            });
            if(applicationModified.get()) {
              replaceApplication(mongoApplication, collectionName);
              setUpMessageContextResponse(context, HttpResponseStatus.OK,
                "Delete operation/s completed");
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

          //Able to delete an application only if status is not SUBMITTED
          if (!ApplicationStatus.SUBMITTED.getStatus()
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
            replaceApplication(mongoApplication, collectionName);
            setUpMessageContextResponse(context, HttpResponseStatus.OK,
              "Delete operation/s completed");

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
          replaceApplication(mongoApplication, collectionName);
        }

        // Delete datasets and distributions received.
        deleteDistributionsFromBasket(requestDatasets, context);

      } else {
        log.info(collectionName
            + " Application with id: {} was not found in the Mongo database",
          ((JsonObject) requestApplication).getString(APPLICATION_ID));
      }

      promise.complete(updatedApplication);
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
  protected void triggerTranslationRequest(
    String applicationId,
    JsonObject form,
    String applicationType,
    String sourceLang,
    List<String> targetLanguages) {

    TranslationResult result = new TranslationResult();

    // Extract document IDs from form
    List<String> documentIds = ApplicationFormUtils.extractDocumentIds(form);
    log.info("Triggering translation for applicationId={}, documents={}", applicationId, documentIds);

    // Trigger form translation request
    JsonObject formPayload = hubTranslationClient.buildTranslationRequestPayload(
      applicationId, form, applicationType, sourceLang, targetLanguages);

    // Record that translation was initiated
    result.markFormPending(formPayload);
    for (String documentId : documentIds) {
      result.addFileStatus(documentId, TranslationStatus.PENDING);
    }

    Future<Void> formTranslationFuture = hubTranslationClient.requestTranslation(formPayload)
      .onSuccess(id -> log.info("Form translation triggered successfully for applicationId={}", applicationId))
      .onFailure(err -> {
        log.error("Form translation request failed for applicationId={}, err={}", applicationId, err.getMessage());
        result.markFormFailure(err);
      });

    // Fetch files as Base64 and trigger separate translation requests per file
    Future<List<JsonObject>> filesBase64Future = hubStoreClient.getFilesAsBase64(documentIds, applicationId)
      .onFailure(err -> {
        log.error("Failed to fetch Base64 files for applicationId={}, error={}", applicationId, err.getMessage(), err);
        result.markFilesFailure(err, documentIds);
      });

    Future<Void> filesTranslationFuture = filesBase64Future.compose(filesBase64 -> {
      log.info("Fetched {} files for Base64 translation for applicationId={}", filesBase64.size(), applicationId);

      // For each file Base64 JSON, trigger the translation request for the file
      List<Future> fileTranslationFutures = new ArrayList<>();

      for (JsonObject fileObj : filesBase64) {
        final String fileId = fileObj.getString(FILE_ID);
        try {
          JsonObject filePayload = hubTranslationClient.buildFileTranslationPayload(applicationId, applicationType, fileObj, targetLanguages);

          Future<Void> fileTranslationFuture = hubTranslationClient.requestFileTranslation(filePayload)
            .onSuccess(v -> log.info("File translation triggered for fileId={}, applicationId={}", fileId, applicationId))
            .onFailure(err -> {
              log.error("File translation failed for fileId={}, applicationId={}, error={}", fileId, applicationId, err.getMessage(), err);
              result.addFileStatus(fileId, TranslationStatus.FAILED, err);
            });

          fileTranslationFutures.add(fileTranslationFuture);
        } catch (Exception ex) {
          log.error("Failed to build or send file translation payload for fileId={}, error={}", fileId, ex.getMessage(), ex);
          result.addFileStatus(fileId, TranslationStatus.FAILED, ex);
        }
      }
      return CompositeFuture.all(fileTranslationFutures).mapEmpty();
    });

    // Combine form and file translation flows
    CompositeFuture.all(formTranslationFuture, filesTranslationFuture)
      .onSuccess(v -> log.info("Triggered form and file translations successfully for application {}", applicationId))
      .onFailure(err -> log.error("One or more translation requests failed for applicationId={}, error={}", applicationId, err.getMessage(), err));

    JsonObject update = result.toMongoUpdate();
    log.info("Persisting translation metadata for applicationId={}, update={}", applicationId, update.encodePrettily());

    mongoClient.updateCollection(applicationType, new JsonObject().put("_id", applicationId), update)
      .onSuccess(v -> {
        log.info("Translation metadata persisted successfully for applicationId={}", applicationId);

        // Unique Audit log for failure
        if (result.isFailed()) {
          logTranslationFailureOnce(applicationId, applicationType, "Translation failed");
        }
      })
      .onFailure(err -> log.error("Failed to persist translation metadata for applicationId={}, error={}", applicationId, err.getMessage(), err));
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
    String applicationType
  ) {
    JsonObject formTranslations = hubTranslationClient.buildFormTranslations(originalForm, dict, translatedFiles, applicationType);

    Set<String> receivedLangs = formTranslations.fieldNames();

    TranslationStatus formStatus = ApplicationFormUtils.determineTranslationStatus(
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
    for (Map.Entry<String, JsonObject> entry : uploadedFiles.entrySet()) {
      String lang = entry.getKey();
      JsonObject fileInfo = entry.getValue();

      translatedFilesObject.put(lang, new JsonObject()
        .put(FILE_ID, fileInfo.getString(FILE_ID))
        .put("fileName", fileInfo.getString("fileName"))
      );
    }

    Set<String> expectedLangs = translation
      .getJsonArray(TARGET_LANGUAGES, new JsonArray())
      .stream()
      .map(Object::toString)
      .collect(Collectors.toSet());

    Set<String> uploadedLangs = uploadedFiles.keySet();

    TranslationStatus fileStatus = ApplicationFormUtils.determineTranslationStatus(
      uploadedLangs, expectedLangs, "file", applicationId
    );

    JsonObject fileTranslationEntry = new JsonObject()
      .put("translatedFiles", translatedFilesObject)
      .put(STATUS, fileStatus);

    translationMeta.put(originalFileId, fileTranslationEntry);
    translation.put("files", translationMeta);
    application.put(TRANSLATION, translation);

    // Update form_translations with new file references
    JsonObject formTranslations = application.getJsonObject(FORM_TRANSLATIONS, new JsonObject());

    for (Map.Entry<String, JsonObject> entry : uploadedFiles.entrySet()) {
      String lang = entry.getKey();
      JsonObject fileInfo = entry.getValue();

      JsonObject formTranslation = formTranslations.getJsonObject(lang);
      if (formTranslation != null) {
        ApplicationFormUtils.updateTranslatedFileReferencesInForm(
          formTranslation,
          originalFileId,
          fileInfo.getString(FILE_ID),
          fileInfo.getString("fileName")
        );
      }
    }

    application.put(FORM_TRANSLATIONS, formTranslations);
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
      .put("status.status", DRAFT)
      .put("translation.errorType", errorType)
      .put(DATE_MODIFIED, Instant.now());

    if (fileId != null) {
      // Mark specific file as FAILED
      setObject.put("translation.files." + fileId + ".status", TranslationStatus.FAILED.name());
    } else {
      // Mark the form as FAILED
      setObject.put("translation.form.status", TranslationStatus.FAILED.name());
    }

    JsonObject update = new JsonObject().put("$set", setObject);

    mongoClient.findOneAndUpdate(collectionName, query, update, res -> {
      if (res.succeeded()) {
        log.info("Marked translation error [{}] for application [{}] in [{}]", errorType, applicationId, collectionName);

        logTranslationFailureOnce(applicationId, collectionName, errorType);
      } else {
        log.error("Failed to mark translation error for application [{}] in [{}]", applicationId, collectionName, res.cause());
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
}
