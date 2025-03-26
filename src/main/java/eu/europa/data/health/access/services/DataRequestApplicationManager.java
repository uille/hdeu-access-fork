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

import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_STATUS_CHANGED;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.DELETION_CAN_NOT_BE_EXECUTED;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.FORM;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_JOB_TITLE;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_JOB_TITLE;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PROVIDED_APPLICATION_ID;
import static eu.europa.data.health.access.Constants.SECTION;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_NUMBER;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.VERSION;
import static eu.europa.data.health.access.Constants.WAS_NOT_FOUND_IN_THE_DATABASE;

import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.Sections;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.validations.business_validators.DataRequestApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.validators.DataRequestApplicationValidationManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRequestApplicationManager extends CommonApplicationManager {

  private final MongoClient mongoClient;

  private final DBHandler dbHandler;

  private DataRequestApplicationValidationManager dataRequestApplicationValidationManager;

  private DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager;

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Data request application constructor
   */
  public DataRequestApplicationManager(DBHandler dbHandler, MongoClient mongoClient,
    BasketManager basketManager, HubStoreClient hubStoreClient, Vertx vertx,
    DataRequestApplicationValidationManager dataRequestApplicationValidationManager,
    DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager) {
    super(dbHandler, basketManager, mongoClient, vertx, hubStoreClient);
    this.dbHandler = dbHandler;
    this.mongoClient = mongoClient;
    this.dataRequestApplicationValidationManager = dataRequestApplicationValidationManager;
    this.dataRequestApplicationBusinessValidationManager = dataRequestApplicationBusinessValidationManager;
  }

  /**
   * Get a Document by field ID for the Data Request Application collection.
   */
  public void getApplicationById(RoutingContext context) {
    super.findById(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  /**
   * Create a Data Request Application
   */
  public void createApplication(RoutingContext context) {
    super.createApplication(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  /**
   * Get all Documents for the Data Request Application collection.
   */
  public void getAllDataRequestApplications(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");
    String status = context.request().getParam(STATUS);
    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    super.getEntity(context, DATA_REQUEST_APPLICATION_COLLECTION, pageSize, pageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc",
      status);
  }

  /**
   * Update a Document for the Data Request Application collection
   */
  public void updateApplication(@NotNull RoutingContext context) {
    super.updateEntity(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  /**
   * Update a Document for the Data Request Application collection
   */
  public void patchApplication(@NotNull RoutingContext context) {
    super.patchEntity(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  /**
   * Delete a Document by field ID for the Data Request Application collection.
   */
  public void deleteDataRequestApplicationById(@NotNull RoutingContext context) {
    deleteDataAccessApplicationById(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  /**
   * Delete all Documents for the Data Request Application collection.
   */
  public void deleteAllDataRequestApplication(@NotNull RoutingContext context) {
    super.deleteAllEntities(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void putDistributionsApplication(@NotNull RoutingContext context) {

    // A json array that will contain all the json objects<application_id, title> for the applications that have been updated
    JsonArray updatedApplications = new JsonArray();

    // A composition of all the asynchronous operations in order to perform one by one the updates of the applications
    CompositeFuture.all(updateApplicationsAsync(context))
      .onComplete(ar -> {
        if (ar.succeeded()) {
          log.info(DATA_REQUEST_APPLICATION_COLLECTION
            + "All update application operations have been completed successfully");

          // Access the results of each operation
          for (Object updatedApplication : ar.result().list()) {

            if (updatedApplication != null && !((JsonObject) updatedApplication).isEmpty()) {
              updatedApplications.add(updatedApplication);
            } else {
              log.info(DATA_REQUEST_APPLICATION_COLLECTION
                + "Application returned was null");
            }
          }
        } else {
          log.info(DATA_REQUEST_APPLICATION_COLLECTION
            + "Some operations have failed");
        }

        // Setup the context response containing all the updated applications
        if (updatedApplications.isEmpty()) {
          setUpContextResponse(context, HttpResponseStatus.NO_CONTENT);
        } else {
          setUpContextResponse(context, HttpResponseStatus.OK, updatedApplications.encode());
        }

      });
  }

  /**
   * Delete a list of datasets for the Data Request Application collection.
   */
  public void deleteDatasets(@NotNull RoutingContext context) {

    // Get the application id
    JsonObject query = new JsonObject().put("_id", context.pathParam(APPLICATION_ID));
    query.put(USER_ID, getUserId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {

          JsonObject mongoApplication = ar.result();

          //Able to delete an application only if status is not SUBMITTED
          if (!ApplicationStatus.SUBMITTED.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))) {

            JsonArray requestContent = context.body().asJsonArray();
            AtomicBoolean applicationModified = new AtomicBoolean(false);
            requestContent.forEach(requestDataset -> {
              String requestDatasetId = ((JsonObject) requestDataset).getString(
                DATASET_ID);
              if(deleteApplicationDataset(mongoApplication, requestDatasetId))
                applicationModified.set(true);
            });
            if(applicationModified.get()) {
              replaceApplication(mongoApplication);
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

  /**
   * Delete a list of distributions for the Data Request Application collection.
   */
  public void deleteDistributions(@NotNull RoutingContext context) {

    // Get the application id
    JsonObject query = new JsonObject().put("_id", context.pathParam(APPLICATION_ID));
    query.put(USER_ID, getUserId(context));
    // Search in database based on the provided application id and user Id
    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, query, null, ar -> {
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
                requestDistributionId);

            });
            replaceApplication(mongoApplication);
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

  private boolean deleteApplicationDataset(JsonObject mongoApplication,
    String requestDatasetId) {
    // Loop every dataset to check if provided dataset id is contained
    JsonArray mongoDatasets = mongoApplication.getJsonArray(DATASETS);
    boolean modified = false;

    Iterator<Object> datasetIterator = mongoDatasets.iterator();
    while (datasetIterator.hasNext()) {
      Object datasetObj = datasetIterator.next();
      if (datasetObj instanceof JsonObject mongoDataset && mongoDataset.getString(DATASET_ID)
        .equals(requestDatasetId)) {

          if (mongoDatasets.size() == 1) {
            JsonObject deleteById = new JsonObject().put("_id",
              mongoApplication.getValue("_id"));
            dbHandler.deleteById(DATA_REQUEST_APPLICATION_COLLECTION, deleteById);
            return false;
          } else {
            datasetIterator.remove();
            modified = true;

            // If dataset removed, delete section6 too.
            deleteSection6ByDataset(mongoApplication, mongoDataset.getString("catalog_id"));
          }
      }
    }
    return modified;
  }

  private void deleteApplicationDatasetDistribution(JsonObject mongoApplication,
    String requestDatasetId, String requestDistributionId) {
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
                    JsonObject deleteById = new JsonObject().put("_id",
                      mongoApplication.getValue("_id"));
                    dbHandler.deleteById(DATA_REQUEST_APPLICATION_COLLECTION, deleteById);
                    return;
                  } else {
                    datasetIterator.remove();
                    mongoApplication.put(DATE_MODIFIED, Instant.now());

                    // If  dataset removed, remove section 6 too.
                    deleteSection6ByDataset(mongoApplication, mongoDataset.getString("catalog_id"));
                  }
                } else {
                  distributionIterator.remove();
                  mongoApplication.put(DATE_MODIFIED, Instant.now());
                }

            }
          }

      }
    }
  }

  public void createOrUpdateSection(@NotNull RoutingContext context) {
    createOrUpdateSection(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void getSection(@NotNull RoutingContext context) {

    JsonObject query = new JsonObject().put("_id", context.pathParam(APPLICATION_ID));
    query.put(USER_ID, getUserId(context));
    String sectionNumber = SECTION + context.pathParam(SECTION_NUMBER);

    JsonObject sectionToRetrieve = new JsonObject().put(FORM + sectionNumber, 1);
    FindOptions options = new FindOptions().setFields(sectionToRetrieve);

    // Search in database based on the provided application id
    mongoClient.findWithOptions(DATA_REQUEST_APPLICATION_COLLECTION, query, options, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null && ar.result().get(0) != null
          && ar.result().get(0).getJsonObject("form") != null && !ar.result().get(0)
          .getJsonObject("form").isEmpty()) {

          List<JsonObject> responseList = ar.result();
          Object mongoSection = ((JsonObject) responseList.get(0).getValue("form")).getValue(
            sectionNumber);

          setUpContextResponse(context, HttpResponseStatus.OK, mongoSection.toString());
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "No information found for provided input");
        }
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          ar.cause().getMessage());
      }
    });
  }

  public void changeStatus(@NotNull RoutingContext context) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String targetStatus = context.body().asJsonObject().getString(STATUS);

    if ("SUBMITTED".equalsIgnoreCase(targetStatus)) {

      JsonObject query = new JsonObject().put(ID, applicationId);
      query.put(USER_ID, getUserId(context));
      query.put("status.status", "DRAFT");

      JsonObject submittingStatus = new JsonObject().put("$set",
        new JsonObject().put("status.status", "SUBMITTING"));

      mongoClient.findOneAndUpdate(DATA_REQUEST_APPLICATION_COLLECTION, query, submittingStatus,
        updateResult -> {
          if (updateResult.succeeded()) {
            if (updateResult.result() != null) {

              JsonObject mongoApplication = updateResult.result();
              JsonObject status = context.body().asJsonObject();

              //validate status change
              if (!canChangeStatus(status, mongoApplication, context)) {
                rollbackStatusToDraft(applicationId, DATA_REQUEST_APPLICATION_COLLECTION);
                return;
              }

              super.performStatusUpdate(mongoApplication, status, context);
              performCleanUpForm(mongoApplication);

              // Replace application and return result
              replaceApplication(mongoApplication).onSuccess(replaceResult -> {
                auditLog(context.body().asJsonObject(), applicationId, APPLICATION_STATUS_CHANGED,
                  context);

                // If status is submitted then we need to add the application to the dispatcher table
                ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
                  mongoApplication.getJsonObject(STATUS).getString(STATUS));
                if (ApplicationStatus.SUBMITTED.equals(requestStatus))
                  sendApplicationForDispatch(mongoApplication, DATA_REQUEST_APPLICATION_COLLECTION);

                setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
              }).onFailure(fail -> {
                rollbackStatusToDraft(applicationId, DATA_REQUEST_APPLICATION_COLLECTION);
                setUpMessageContextResponse(context,
                  HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage());
              });
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
                "Application not found");
            }
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Application status can not be changed");
          }
        });
    } else {
      setUpContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Wrong status provided");
    }
  }

  private boolean performBusinessValidation(JsonObject form, RoutingContext context) {
    return dataRequestApplicationBusinessValidationManager.section4Validation(form, context);
  }

  private boolean performDTOValidation(JsonObject mongoApplication, RoutingContext context) {

    // First check all required sections are saved into the database
    JsonObject form = mongoApplication.getJsonObject("form");
    boolean allSectionsAreSaved = validateKeys(form.getMap());

    if (allSectionsAreSaved) {

      //Retrieve the sections saved in database
      JsonObject databaseSections = mongoApplication.getJsonObject("form");

      for (Sections section : Sections.getFilteredSections()) {

        JsonObject databaseSection = new JsonObject();
        JsonArray databaseSection6 = new JsonArray();
        if (!section.getSection().equalsIgnoreCase(SECTION_6)) {
          databaseSection = databaseSections.getJsonObject(section.getSection());
        } else {
          databaseSection6 = databaseSections.getJsonArray(section.getSection());
        }

        // Since section 7 is not mandatory, if it is not provided then no validations should be
        // executed
        if (Sections.SECTION_7.equals(section) && databaseSection == null) {
          return true;
        }

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        SectionDTO sectionDTO = dataRequestApplicationValidationManager.generateSectionDTO(
          section.getSectionNumber(),
          section.getSection().equals(SECTION_6) ? databaseSection6.toString()
            : databaseSection.toString(), context);

        // Validate SectionDTO
        Set<ConstraintViolation<SectionDTO>> violations = validator.validate(sectionDTO);

        if (!violations.isEmpty()) {
          setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
            "Section errors exist at section: " + section.getSectionNumber()
              + ", you need to fix them before proceeding to form submission");
          return false;
        }
      }
      return true;

    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
        "You need to save all required sections before proceeding to form submission");
    }
    return false;
  }

  private boolean validateKeys(Map<String, Object> map) {
    List<String> mandatorySections = Arrays.asList(SECTION_2,SECTION_3, SECTION_4, SECTION_5,
      SECTION_6, "section8");

    for (String key : mandatorySections) {
      if (!map.containsKey(key)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check that the request status from FE is valid enum and also check that submitted can be
   * performed when application are on statuses draft or withdrawn. And withdrawn can be performed
   * only when application is on status submitted
   */
  private boolean canChangeStatus(JsonObject requestStatusObject, JsonObject mongoApplication,
    RoutingContext context) {

    String statusStr = requestStatusObject.getString(STATUS);
    if (!ApplicationStatus.isValid(statusStr)) {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN, "Status cannot be "
        + "updated: wrong status value");
      return false;
    }

    ApplicationStatus requestStatus = ApplicationStatus.fromStatus(statusStr);
    ApplicationStatus mongoStatus = ApplicationStatus.fromStatus(
      mongoApplication.getJsonObject(STATUS).getString(STATUS));
    Map<ApplicationStatus, Set<ApplicationStatus>> allowStatuses = Map.of(
      ApplicationStatus.SUBMITTED, EnumSet.of(ApplicationStatus.DRAFT, ApplicationStatus.WITHDRAWN),
      ApplicationStatus.WITHDRAWN, EnumSet.of(ApplicationStatus.SUBMITTED)
    );
    boolean statusChangeAllowed = allowStatuses.getOrDefault(requestStatus, Collections.emptySet())
      .contains(mongoStatus);

    if (statusChangeAllowed) {
      // In case the status is 'submitted' we need to validate all the sections before updating it
      if (ApplicationStatus.SUBMITTED.equals(requestStatus)) {
        // Perform the DTO validations
        statusChangeAllowed = performDTOValidation(mongoApplication, context);
        // If DTO's validations passed successfully, then we need to perform the business validations
        if (statusChangeAllowed) {
          statusChangeAllowed = performBusinessValidation(mongoApplication.getJsonObject("form"),
            context);
        }
      }
    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN, "Status cannot be "
        + "updated: wrong flow");
      return false;
    }

    return statusChangeAllowed;
  }

  private void performCleanUpForm(JsonObject mongoApplication) {
    cleanSection2(mongoApplication.getJsonObject("form"));
    cleanSection3(mongoApplication.getJsonObject("form"));
    cleanSection4(mongoApplication.getJsonObject("form"));
    cleanSection5(mongoApplication.getJsonObject("form"));
    cleanSection6(mongoApplication.getJsonObject("form"));
  }

  private void cleanSection5(JsonObject mongoApplicationForm) {
    JsonObject section5 = mongoApplicationForm.getJsonObject(SECTION_5);

    JsonArray purposeOfUse = section5.getJsonArray("purposeOfUse");

    if (notContainsKey(purposeOfUse, "b")) {
      section5.remove("legalBasis");
      section5.remove("linkToTheSupportingLegalBasis");
    }

    if (notContainsSelectedValueWithoutSpecificValue(purposeOfUse)) {
      section5.remove("summaryOfPlanForUsingTheData");
      section5.remove(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON);
      section5.remove(PERSON_RESPONSIBLE_NAME);
      section5.remove(PERSON_RESPONSIBLE_JOB_TITLE);
      section5.remove(PERSON_RESPONSIBLE_AFFILIATION);
    }

    if (notContainsKey(purposeOfUse, "e")) {
      section5.remove("summaryOfResearchPlan");
      section5.remove(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON);
      section5.remove(PERSON_RESEARCH_NAME);
      section5.remove(PERSON_RESEARCH_JOB_TITLE);
      section5.remove(PERSON_RESEARCH_AFFILIATION);
    }

    mongoApplicationForm.put(SECTION_5, section5);
  }

  private void cleanSection6(JsonObject mongoApplicationForm) {
    JsonArray section6Array = mongoApplicationForm.getJsonArray(SECTION_6);

    for (int i = 0; i < section6Array.size(); i++) {


      //SECTION 6_1
      JsonObject section = section6Array.getJsonObject(i);

      JsonObject extractionMethod = section.getJsonObject("extractionMethod");
      String extractionMethodKey2 = extractionMethod.getString("key");

      if (!extractionMethodKey2.equals("c")) {
        section.remove("samplingMethod");
      }

      if (!extractionMethodKey2.equals("c") && !extractionMethodKey2.equals("a")) {
        section.remove("sampleSize");
      }

      JsonObject howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple = section.getJsonObject(
        "howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple");
      if (!Objects.equals(howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"), "b")) {
        section.remove(NEED_FOR_DATA_EXTRACTION_EVERY);
      }

      JsonObject needForDataExtractionEvery = section.getJsonObject(
        NEED_FOR_DATA_EXTRACTION_EVERY);
      if (!Objects.equals(howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"), "b")
        || !Objects.equals(needForDataExtractionEvery.getString("key"), "d")) {
        section.remove("needForDataExtractionEveryOther");
      }
    }

    mongoApplicationForm.put(SECTION_6, section6Array);
  }

  private boolean notContainsSelectedValueWithoutSpecificValue(JsonArray jsonArray) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");

      if (!key.equals("e")) {
        return false;
      }
    }
    return true;
  }

  private List<Future> updateApplicationsAsync(@NotNull RoutingContext context) {

    //Retrieve all applications from the request
    JsonArray requestApplications = context.body().asJsonObject().getJsonArray("applications");
    //Define the array of Futures that need to be returned
    List<Future> futures = new ArrayList<>();

    // For every application in the request, invoke the asynchronous call to perform the update of the application
    requestApplications.forEach(requestApplication -> futures.add(updateApplicationAsync(requestApplication, context)));

    return futures;
  }

  private Future<JsonObject> updateApplicationAsync(Object requestApplication,
    @NotNull RoutingContext context) {

    Promise<JsonObject> promise = Promise.promise();

    // A map of application <application_id, title> that will store the application that is being updated
    JsonObject updatedApplication = new JsonObject();
    //Retrieve the application from Mongo Database
    Future<JsonObject> mongoApplicationFuture = findApplication(requestApplication,
      getUserId(context));
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
            JsonArray mongoDistributions = getDistributions(mongoDatasets, DATASET_ID,
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
                updateDatasetDateAdded(mongoDatasets, requestDatasetIdValue, DATA_REQUEST_APPLICATION_COLLECTION);
                //Add the application id and the title to the response map
                updatedApplication.put("_id", mongoApplication.getString("_id"));
                updatedApplication.put(VERSION, mongoApplication.getString(VERSION));
                updatedApplication.put(TITLE, mongoApplication.getString(TITLE));
              } else {
                // Distribution is found in Mongo database
                log.info(DATA_REQUEST_APPLICATION_COLLECTION
                    + " Distribution with id: {} was found in the Mongo database, so will not add it",
                  requestDistributionIdValue);
              }
            });
          } else {
            // The provided dataset does not exist, so will have to add it with all of its distributions
            // prepare Dataset to be added
            mongoDatasets.add(prepareDataset(requestDataset, DATA_REQUEST_APPLICATION_COLLECTION));
            //Add the application id and the title to the response map
            updatedApplication.put("_id", mongoApplication.getString("_id"));
            updatedApplication.put(VERSION, mongoApplication.getString(VERSION));
            updatedApplication.put(TITLE, mongoApplication.getString(TITLE));
          }

        });
        //Update the application to Mongo database only if changes need to be applied
        if (!updatedApplication.isEmpty()) {
          replaceApplication(mongoApplication);
        }

        // Delete datasets and distributions received.
        deleteDistributionsFromBasket(requestDatasets, context);

      } else {
        log.info(DATA_REQUEST_APPLICATION_COLLECTION
            + " Application with id: {} was not found in the Mongo database",
          ((JsonObject) requestApplication).getString(APPLICATION_ID));
      }

      promise.complete(updatedApplication);
    });

    return promise.future();
  }

  private Future<JsonObject> replaceApplication(JsonObject mongoApplication) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject query = new JsonObject().put("_id", mongoApplication.getValue("_id"));
    mongoApplication.put(DATE_MODIFIED, Instant.now());
    mongoClient.findOneAndReplace(DATA_REQUEST_APPLICATION_COLLECTION, query, mongoApplication,
      result -> {
        if (result.succeeded()) {
          log.info(DATA_REQUEST_APPLICATION_COLLECTION
              + " Application with id: {} was updated successfully",
            mongoApplication.getValue("_id"));
          promise.complete(result.result());
        } else {
          log.info(
            DATA_REQUEST_APPLICATION_COLLECTION + " Failed to replace application with id: {}",
            mongoApplication.getValue("_id"));
          promise.fail(result.cause());
        }
      });
    return promise.future();
  }

  private JsonArray getDistributions(JsonArray jsonArray, String field, String value) {
    for (Object element : jsonArray) {
      if (element instanceof JsonObject jsonObject) {
        String foundValue = jsonObject.getString(field);
        if (StringUtils.isNotEmpty(foundValue) && StringUtils.equals(value, foundValue)) {
          return jsonObject.getJsonArray(DISTRIBUTIONS);
        }
      }
    }
    return null;
  }

  private Future<JsonObject> findApplication(Object datasetQueryDocument, String userId) {

    JsonObject applicationQueryDocument = new JsonObject();
    applicationQueryDocument.put("_id",
      ((JsonObject) datasetQueryDocument).getString(APPLICATION_ID));
    applicationQueryDocument.put(USER_ID, userId);

    Promise<JsonObject> promise = Promise.promise();

    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, applicationQueryDocument, null, ar -> {
      if (ar.succeeded()) {
        JsonObject application = ar.result();
        promise.complete(application);
      } else {
        promise.fail(ar.cause());
      }
    });

    return promise.future();
  }

  public void postData(@NotNull RoutingContext context) {
    postData(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }
  public void getData(@NotNull RoutingContext context) {
    getData(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }
  public void deleteData(@NotNull RoutingContext context) {
    deleteData(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void validateFileBeforeUpdate(@NotNull RoutingContext context) {
    validateFileBeforeUpdate(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void changeTitle(@NotNull RoutingContext context) {
    changeTitle(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

}
