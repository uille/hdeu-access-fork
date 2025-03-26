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
import static eu.europa.data.health.access.Constants.APPLICATION_AUDIT_LOG;
import static eu.europa.data.health.access.Constants.APPLICATION_CREATED;
import static eu.europa.data.health.access.Constants.APPLICATION_DELETED;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_WITH_ID;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_AFFILIATION;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_BUSINESS_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_JOB_TITLE;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_OPERATOR_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_ORGANISATION_NAME;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE;
import static eu.europa.data.health.access.Constants.DATE_ADDED;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.DATE_SUBMITTED;
import static eu.europa.data.health.access.Constants.DELETION_CAN_NOT_BE_EXECUTED;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.ERROR_CHECKING_DOCUMENT_EXISTENCE;
import static eu.europa.data.health.access.Constants.FORM;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_EMAIL;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_NAME;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_AFFILIATION;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_JOB_TITLE;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NOT_FOUND_FOR_USER_WITH_ID;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_JOB_TITLE;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_JOB_TITLE;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PROVIDED_APPLICATION_ID;
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
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION;
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
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.exceptions.JsonParsingException;
import eu.europa.data.health.access.handlers.DBHandler;
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
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class CommonApplicationManager extends AbstractManager {

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
    "ListContainsOneMoreValueExpectASpecificValue");

  private final DBHandler dbHandler;
  private final BasketManager basketManager;
  private final MongoClient mongoClient;
  private final Vertx vertx;
  private final HubStoreClient hubStoreClient;

  protected CommonApplicationManager(DBHandler dbHandler, BasketManager basketManager,
    MongoClient mongoClient, Vertx vertx, HubStoreClient hubStoreClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.basketManager = basketManager;
    this.mongoClient = mongoClient;
    this.vertx = vertx;
    this.hubStoreClient = hubStoreClient;
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
    status.put(USER_ID, getUserId(context));
    mongoApplication.put(STATUS, status);
  }

  protected void createApplication(RoutingContext context, String collectionName) {
    // Retrieve the request body
    JsonObject requestBody = context.body().asJsonObject();
    // Define the fields to be stored
    requestBody.put(DATE_CREATED, Instant.now());
    requestBody.put(DATE_MODIFIED, Instant.now());
    requestBody.put(USER_ID, getUserId(context));
    JsonObject status = new JsonObject();
    status.put(STATUS, DRAFT);
    status.put(DATE, Instant.now());
    status.put(USER_ID, getUserId(context));
    requestBody.put(STATUS, status);
    requestBody.put(VERSION, 1);
    JsonArray datasets = requestBody.getJsonArray(DATASETS);

    if (datasets != null) {
      for (Object datasetObj : datasets) {
        JsonObject dataset = (JsonObject) datasetObj;
        dataset.put(DATE_ADDED, Instant.now());
      }
    }
    super.createEntity(context, collectionName);
    String applicationId = context.body().asJsonObject().getString(ID);
    auditLog(status, applicationId, APPLICATION_CREATED, context);
    // For each dataset delete distributions from data basket
    deleteDistributionsFromBasket(datasets, context);
  }

  protected void auditLog(JsonObject status, String applicationId,
    String action, RoutingContext context) {
    JsonObject auditLog = new JsonObject(status.encode())
      .put(APPLICATION_ID, applicationId)
      .put(USER_ID, getUserId(context))
      .put("action", action);
    dbHandler.create(APPLICATION_AUDIT_LOG, auditLog);
  }

  protected void deleteDataAccessApplicationById(@NotNull RoutingContext context,
    String collectionName) {
    String id = context.pathParam("id");
    // First, retrieve the application
    JsonObject query = new JsonObject().put(ID, id);
    query.put(USER_ID, getUserId(context));
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
                hubStoreClient.deleteFile(fileId, id);
              }
            }
            // Log the application for auditing purposes
            auditLog(document.getJsonObject(STATUS), id, APPLICATION_DELETED, context);
            // Delete the application
            super.deleteEntityById(context, collectionName);
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
          getUserId(context)), distributionIdList);
    }
  }

  public void postData(@NotNull RoutingContext context, String collectionName) {

    String requestApplicationId = context.pathParam(APPLICATION_ID);
    String userId = getUserId(context);
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
            handleFileUpload(context, requestApplicationId);

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
    String userId = getUserId(context);
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
        if (StringUtils.isNotEmpty(foundValue) && StringUtils.equals(value, foundValue)) {
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
        if (StringUtils.isNotEmpty(datasetId) && StringUtils.equals(datasetId, id)) {
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

  private void handleFileUpload(RoutingContext context, String applicationId) {

    String contentType = context.request().getHeader(CONTENT_TYPE);

    // Get the uploaded file
    FileUpload fileUploaded = context.fileUploads().get(0);
    vertx.fileSystem().readFile(fileUploaded.uploadedFileName())
      .compose(
        fileBuffer -> hubStoreClient.uploadFile(applicationId, fileUploaded.fileName(),
          fileBuffer, contentType))
      .onSuccess(responseJson -> {
        context.response().putHeader(CONTENT_TYPE, "application/json");
        setUpContextResponse(context, HttpResponseStatus.CREATED, responseJson.encode());
      })
      .onFailure(
        err -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to upload file"));
  }

  protected void getFileData(RoutingContext context, final String applicationId) {
    String fileId = context.pathParam("data_id");

    // Get the uploaded file
    hubStoreClient.getFile(fileId, applicationId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          context.response()
            .putHeader(CONTENT_TYPE, "application/octet-stream")
            .putHeader("Content-Disposition", response.headers().get("content-disposition"))
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

    hubStoreClient.deleteFile(fileId, applicationId)
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
    String userId = getUserId(context);
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
    query.put(USER_ID, getUserId(context));
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
                (StringUtils.equals(sectionNumber, SECTION_6)) ? sameAsSection3Array(context.body().asJsonArray(), sectionNumber, section3)
                  : sameAsSection3Object(context.body().asJsonObject(), sectionNumber, section3));
            } else if (DATA_REQUEST_APPLICATION_COLLECTION.equals(collectionName)) {
              update.put(FORM + sectionNumber,
                (StringUtils.equals(sectionNumber, SECTION_6)) ? context.body().asJsonArray()
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

  private JsonArray sameAsSection3Array(JsonArray sectionFormArray, String sectionNumber, JsonObject section3) {
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

  private void populateSection4(JsonObject sectionForm, JsonObject section3, String personType) {
    if ("a".equals(personType)) {
      sectionForm.put("fullName", section3.getString(LEGAL_PERSON_NAME));
      sectionForm.put("address", section3.getString(LEGAL_PERSON_ADDRESS));
      sectionForm.put("phone", section3.getJsonObject(CONTACT_PERSON_PHONE));
      sectionForm.put("email", section3.getString(CONTACT_PERSON_EMAIL));
      sectionForm.put("operatorIdentifier", section3.getString(CONTACT_PERSON_OPERATOR_ID));
      sectionForm.put("nameOfTheOrganisation", section3.getString(CONTACT_PERSON_ORGANISATION_NAME));
      sectionForm.put("businessIdentifierOrganization", section3.getString(CONTACT_PERSON_BUSINESS_ID));
    } else if ("b".equals(personType)) {
      sectionForm.put("fullName", section3.getString(NATURAL_PERSON_NAME));
      sectionForm.put("address", section3.getString(NATURAL_PERSON_ADDRESS));
      sectionForm.put("phone", section3.getJsonObject(NATURAL_PERSON_PHONE));
      sectionForm.put("email", section3.getString(NATURAL_PERSON_EMAIL));
    }
  }

  private void populateSection5(JsonObject sectionForm, JsonObject section3, String personType) {
    String nameKey = "a".equals(personType) ? LEGAL_PERSON_NAME : NATURAL_PERSON_NAME;
    String jobTitleKey = "a".equals(personType) ? CONTACT_PERSON_JOB_TITLE : NATURAL_PERSON_JOB_TITLE;
    String affiliationKey = "a".equals(personType) ? CONTACT_PERSON_AFFILIATION : NATURAL_PERSON_AFFILIATION;

    if (isFieldTrue(sectionForm, PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON)) {
      sectionForm.put(PERSON_RESPONSIBLE_NAME, section3.getString(nameKey));
      sectionForm.put(PERSON_RESPONSIBLE_JOB_TITLE, section3.getString(jobTitleKey));
      sectionForm.put(PERSON_RESPONSIBLE_AFFILIATION, section3.getString(affiliationKey));
    }

    if (isFieldTrue(sectionForm, PERSON_RESEARCH_SAME_AS_CONTACT_PERSON)) {
      sectionForm.put(PERSON_RESEARCH_NAME, section3.getString(nameKey));
      sectionForm.put(PERSON_RESEARCH_JOB_TITLE, section3.getString(jobTitleKey));
      sectionForm.put(PERSON_RESEARCH_AFFILIATION, section3.getString(affiliationKey));
    }
  }

  private boolean isFieldTrue(JsonObject sectionForm, String fieldKey) {
    return sectionForm.containsKey(fieldKey) && sectionForm.getBoolean(fieldKey, false);
  }


  private void updateSameAsSection3(JsonObject mongoForm, JsonObject updateForm, Integer sectionNumber, Boolean isAccess) {
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

    updatePerson(section5, section3, PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, PERSON_RESPONSIBLE_NAME,
      PERSON_RESPONSIBLE_JOB_TITLE, PERSON_RESPONSIBLE_AFFILIATION);

    updatePerson(section5, section3, PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, PERSON_RESEARCH_NAME,
      PERSON_RESEARCH_JOB_TITLE, PERSON_RESEARCH_AFFILIATION);
  }

  private void updatePerson(JsonObject section, JsonObject section3, String key, String nameKey, String jobTitleKey, String affiliationKey) {
    boolean sameAsContactPerson = section.getBoolean(key, false);
    if (!sameAsContactPerson) return;

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) return;

    String personType = legalPerson.getString("key");
    if ("a".equals(personType)) {
      section.put(nameKey, section3.getString(LEGAL_PERSON_NAME, ""));
      section.put(jobTitleKey, section3.getString(CONTACT_PERSON_JOB_TITLE, ""));
      section.put(affiliationKey, section3.getString(CONTACT_PERSON_AFFILIATION, ""));
    } else if ("b".equals(personType)) {
      section.put(nameKey, section3.getString(NATURAL_PERSON_NAME, ""));
      section.put(jobTitleKey, section3.getString(NATURAL_PERSON_JOB_TITLE, ""));
      section.put(affiliationKey, section3.getString(NATURAL_PERSON_AFFILIATION, ""));
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
    section4.put("fullName", section3.getString(LEGAL_PERSON_NAME, ""));
    section4.put("address", section3.getString(LEGAL_PERSON_ADDRESS, ""));
    section4.put("phone", getPhoneNumber(section3, CONTACT_PERSON_PHONE));
    section4.put("email", section3.getString(CONTACT_PERSON_EMAIL, ""));
    section4.put("operatorIdentifier", section3.getString(CONTACT_PERSON_OPERATOR_ID, ""));
    section4.put("nameOfTheOrganisation", section3.getString(CONTACT_PERSON_ORGANISATION_NAME, ""));
    section4.put("businessIdentifierOrganization", section3.getString(CONTACT_PERSON_BUSINESS_ID, ""));
  }

  private void updateNaturalPerson(JsonObject section4, JsonObject section3) {
    section4.put("fullName", section3.getString(NATURAL_PERSON_NAME, ""));
    section4.put("address", section3.getString(NATURAL_PERSON_ADDRESS, ""));
    section4.put("phone", getPhoneNumber(section3, NATURAL_PERSON_PHONE));
    section4.put("email", section3.getString(NATURAL_PERSON_EMAIL, ""));
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
      (StringUtils.equals(sectionNumber, SECTION_6)) ? context.body().asJsonArray()
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

              validateNestedFileFor("ethicalReviewAttachment", context)
                .recover(err -> Future.succeededFuture())
                .compose(
                  res2 -> validateNestedFileFromArray("tabulationPlan", "tabulationPlanArray",
                    context))
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

    List<Future> futures = new ArrayList<>();
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
    CompositeFuture.all(futures)
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
    hubStoreClient.getFile(attachmentId, applicationId)
      .compose(result -> {
        String fileName = result.getHeader("filename");
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
    List<Future> futures = new ArrayList<>(); // Collect all futures

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
    CompositeFuture.all(futures)
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

    List<Future> futures = new ArrayList<>();
    List<JsonObject> objectsToRemove = new ArrayList<>();

    objectList.forEach(obj -> {
      JsonObject attachment = (JsonObject) obj;
      String attachmentId = attachment.getString("id");

      Future<Object> future = hubStoreClient.getFile(attachmentId, applicationId)
        .compose(result -> {
          String fileName = result.getHeader("filename");
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

    CompositeFuture.all(futures)
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

  protected void cleanSection2(JsonObject mongoApplicationForm) {
    JsonObject section2 = mongoApplicationForm.getJsonObject(SECTION_2);

    JsonArray theResearchFocusesOnTheFollowingObjectives = section2.getJsonArray(
      "theResearchFocusesOnTheFollowingObjectives");
    if (Objects.nonNull(theResearchFocusesOnTheFollowingObjectives) && notContainsKey(
      theResearchFocusesOnTheFollowingObjectives, "i")) {
      section2.remove("theResearchFocusesOnTheFollowingObjectivesOther");
    }

    JsonArray areaOfResearch = section2.getJsonArray("areaOfResearch");
    if (Objects.nonNull(areaOfResearch) && notContainsKey(areaOfResearch, "aj")) {
      section2.remove("areaOfResearchOther");
    }

    boolean isDescriptionChecked =
      section2.containsKey(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
        && section2.getBoolean(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION,
        false);

    if (!isDescriptionChecked) {
      section2.remove("descriptionOfTheProject");
    }else {
      section2.remove("descriptionOfTheDataYouWillUse");
    }

    boolean isSummaryChecked =
      section2.containsKey(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
        && section2.getBoolean(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY,
        false);

    if (!isSummaryChecked) {
      section2.remove(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON);
    }else {
      section2.remove("summaryOfTheProject");
    }

    // Explicitly put the modified section2 back into mongoApplication
    mongoApplicationForm.put(SECTION_2, section2);
  }

  protected void cleanSection3(JsonObject mongoApplicationForm) {
    JsonObject section3 = mongoApplicationForm.getJsonObject(SECTION_3);

    JsonObject applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
      APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    if (!"a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
      section3.remove(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS);
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    //if is "a" is legal person else i b natural person
    if (Objects.equals(legalPerson.getString("key"), "b")) {
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
      section3.remove(CONTACT_PERSON_OPERATOR_ID);
      section3.remove(CONTACT_PERSON_JOB_TITLE);
      section3.remove(CONTACT_PERSON_AFFILIATION);
      section3.remove("contactPersonRelationship");
    } else {
      section3.remove(NATURAL_PERSON_NAME);
      section3.remove(NATURAL_PERSON_ADDRESS);
      section3.remove("naturalPersonZipCode");
      section3.remove("naturalPersonCity");
      section3.remove("naturalPersonCountry");
      section3.remove(NATURAL_PERSON_EMAIL);
      section3.remove(NATURAL_PERSON_PHONE);
      section3.remove(NATURAL_PERSON_JOB_TITLE);
      section3.remove(NATURAL_PERSON_AFFILIATION);
    }

    // Explicitly put the modified section2 back into mongoApplication
    mongoApplicationForm.put(SECTION_3, section3);
  }

  protected void cleanSection4(JsonObject mongoApplicationForm) {
    JsonObject section3 = mongoApplicationForm.getJsonObject(SECTION_3);
    JsonObject section4 = mongoApplicationForm.getJsonObject(SECTION_4);

    JsonObject invoiceType = section4.getJsonObject("invoiceType");

    if (!Objects.equals(invoiceType.getString("key"), "b")) {
      section4.remove("invoiceAddress");
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.equals(legalPerson.getString("key"), "b")) {
      section4.remove("operatorIdentifier");
      section4.remove("nameOfTheOrganisation");
      section4.remove("businessIdentifierOrganization");
      section4.remove("vatNumber");
      section4.remove("peppolCode");
    }

    JsonObject isTheProjectFinanciallyCovered = section4.getJsonObject(
      "isTheProjectFinanciallyCovered");
    if (!"a".equals(isTheProjectFinanciallyCovered.getString("key"))) {
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

    JsonObject cronConfiguration = new JsonObject().put(Constants.APPLICATION_TYPE, database).put(STATUS, "pending").put("retries", 0);
    JsonObject dispatcherRecord = new JsonObject().put("configuration", cronConfiguration).put("application", mongoApplication);

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
      .put(USER_ID, getUserId(context));

    // Check if the provided title already exists as a document.
    mongoClient.findOne(database, query, null, asyncResult -> {
      // Title already exists.
      if (asyncResult.result() != null) {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "applicationTitleExists");
        // Update title
      } else {
        JsonObject queryUpdate = new JsonObject().put(ID, id);
        JsonObject update = new JsonObject().put("$set",
          new JsonObject().put(TITLE, context.body().asJsonObject().getValue(TITLE)));

        mongoClient.findOneAndUpdate(database, queryUpdate, update,
          res -> {
            if (res.succeeded()) {
              setUpMessageContextResponse(context, HttpResponseStatus.OK,
                "Title application updated successfully for application with id: " + id);
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
                "Title application did not updated in the DB level for application with id: " + id);
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

}
