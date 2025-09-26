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
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.FORM;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.PROVIDED_JUSTIFICATION_EXCEPTION;
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

import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.Sections;
import eu.europa.data.health.access.TranslationStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.utils.ApplicationFormUtils;
import eu.europa.data.health.access.validations.business_validators.DataRequestApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.validators.DataRequestApplicationValidationManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRequestApplicationManager extends CommonApplicationManager {

  private final MongoClient mongoClient;

  private DataRequestApplicationValidationManager dataRequestApplicationValidationManager;

  private DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager;

  private final AuditLogManager auditLogManager;

  private final boolean translate;

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Data request application constructor
   */
  public DataRequestApplicationManager(DBHandler dbHandler, MongoClient mongoClient,
    BasketManager basketManager, HubStoreClient hubStoreClient, Vertx vertx,
    DataRequestApplicationValidationManager dataRequestApplicationValidationManager,
    DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager,
    AuditLogManager auditLogManager, HubTranslationClient hubTranslationClient,
    HubSearchClient hubSearchClient,
    boolean translate, AuthClient authClient) {
    super(dbHandler, basketManager, mongoClient, vertx, hubStoreClient, auditLogManager,
      hubTranslationClient, hubSearchClient, authClient);
    this.mongoClient = mongoClient;
    this.dataRequestApplicationValidationManager = dataRequestApplicationValidationManager;
    this.dataRequestApplicationBusinessValidationManager = dataRequestApplicationBusinessValidationManager;
    this.auditLogManager = auditLogManager;
    this.translate = translate;
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
   * Delete a Document by field ID for the Data Request Application collection.
   */
  public void deleteDataRequestApplicationById(@NotNull RoutingContext context) {
    deleteApplicationById(context, DATA_REQUEST_APPLICATION_COLLECTION);
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
    CompositeFuture.all(updateApplicationsAsync(context, DATA_REQUEST_APPLICATION_COLLECTION))
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
    deleteDatasets(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  /**
   * Delete a list of distributions for the Data Request Application collection.
   */
  public void deleteDistributions(@NotNull RoutingContext context) {
    deleteDistributions(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void createOrUpdateSection(@NotNull RoutingContext context) {
    createOrUpdateSection(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void getSection(@NotNull RoutingContext context) {

    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
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
      query.put(USER_ID, Context.getEcasId(context));
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

              createApplicantDetails(context)
                .recover(err -> {
                  log.error("Could not update applicant details: {}", err.getMessage());
                  return Future.succeededFuture(new JsonObject());
                })
                .onSuccess(applicantDetailsResult -> {
                  if (!applicantDetailsResult.isEmpty()) {
                    mongoApplication.put("applicantDetails", applicantDetailsResult);
                  }

                  // Fetch source language
                  String sourceLang = mongoApplication
                    .getString("inputLanguage", "EN")
                    .toUpperCase(Locale.ENGLISH);

                  // Extract datasets as List<JsonObject>
                  List<JsonObject> datasets = mongoApplication.getJsonArray("datasets").stream()
                    .filter(obj -> obj instanceof JsonObject)
                    .map(obj -> (JsonObject) obj)
                    .toList();

                  // Async fetch of target languages from datasets
                  getTargetLanguagesFromDatasets(datasets).onComplete(langResult -> {
                    List<String> targetLanguages;
                    boolean translationNeeded;

                    if (langResult.succeeded()) {
                      Set<String> uniqueLanguages = new HashSet<>(langResult.result());
                      // English is included in the translation languages by default
                      uniqueLanguages.add("EN");
                      targetLanguages = new ArrayList<>(uniqueLanguages);

                      // Remove the source language if present
                      targetLanguages.removeIf(lang -> lang.equalsIgnoreCase(sourceLang));

                      // Check if translation is needed (any language remains)
                      translationNeeded = !targetLanguages.isEmpty();
                    } else {
                      translationNeeded = false;
                      // If failed to get target languages, fallback to empty list (no translation)
                      targetLanguages = List.of();
                    }

                    JsonObject translation = mongoApplication.getJsonObject(TRANSLATION,
                      new JsonObject());
                    translation.put(STATUS, translationNeeded ? TranslationStatus.PENDING
                      : TranslationStatus.NOT_REQUIRED);
                    translation.put("sourceLanguage", sourceLang);
                    translation.put("targetLanguages", new JsonArray(targetLanguages));
                    mongoApplication.put(TRANSLATION, translation);

                    super.performStatusUpdate(mongoApplication, status, context);
                    performCleanUpForm(mongoApplication);

                    // Replace application and return result
                    replaceApplication(mongoApplication).onSuccess(replaceResult -> {

                      if (translate && translationNeeded) {
                        triggerTranslationRequest(
                          applicationId,
                          mongoApplication.getJsonObject("form"),
                          DATA_REQUEST_APPLICATION_COLLECTION,
                          sourceLang,
                          targetLanguages
                        );

                        setUpContextResponse(context, HttpResponseStatus.OK,
                          replaceResult.encode());
                        return;
                      }

                      // If status is submitted then we need to add the application to the dispatcher table
                      ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
                        mongoApplication.getJsonObject(STATUS).getString(STATUS));
                      if (ApplicationStatus.SUBMITTED.equals(requestStatus)) {
                        sendApplicationForDispatch(mongoApplication,
                          DATA_REQUEST_APPLICATION_COLLECTION);

                        //Audit Log
                        String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                          AuditLogActionDescription.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
                          applicationId, mongoApplication.getString(TITLE));
                        auditLogManager.insertAuditLog(
                          AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
                          mongoApplication.getString(ID),
                          AuditLogAction.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
                          auditLogActionDescription, true, context);
                      }
                      setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
                    }).onFailure(fail -> {
                      rollbackStatusToDraft(applicationId, DATA_REQUEST_APPLICATION_COLLECTION);
                      setUpMessageContextResponse(context,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage());
                    });
                  });

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
    List<String> mandatorySections = Arrays.asList(SECTION_2, SECTION_3, SECTION_4, SECTION_5,
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
  boolean canChangeStatus(JsonObject requestStatusObject, JsonObject mongoApplication,
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

  void performCleanUpForm(JsonObject mongoApplication) {
    cleanSection2(mongoApplication.getJsonObject("form"));
    cleanSection3(mongoApplication.getJsonObject("form"));
    cleanSection4(mongoApplication.getJsonObject("form"));
    cleanSection6(mongoApplication.getJsonObject("form"));
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
      if (!Objects.equals(howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"),
        "b")) {
        section.remove(NEED_FOR_DATA_EXTRACTION_EVERY);
      }

      JsonObject needForDataExtractionEvery = section.getJsonObject(
        NEED_FOR_DATA_EXTRACTION_EVERY);
      if (!Objects.equals(howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"), "b")
        || !Objects.equals(needForDataExtractionEvery.getString("key"), "d")) {
        section.remove("needForDataExtractionEveryOther");
      }

      JsonObject optOutOfTheMechanismProvidedInTheNationalLaw = section.getJsonObject(
        "optOutOfTheMechanismProvidedInTheNationalLaw");
      if (!Objects.equals(optOutOfTheMechanismProvidedInTheNationalLaw.getString("key"),
        "a")) {
        section.remove(PROVIDED_JUSTIFICATION_EXCEPTION);
      }

    }

    mongoApplicationForm.put(SECTION_6, section6Array);
  }

  Future<JsonObject> replaceApplication(JsonObject mongoApplication) {
    return replaceApplication(mongoApplication, DATA_REQUEST_APPLICATION_COLLECTION);
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

  public void processTranslationCallback(String applicationId, JsonObject dict) {
    JsonObject query = new JsonObject().put(ID, applicationId);

    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, query, null, res -> {
      if (res.succeeded() && res.result() != null) {
        JsonObject application = res.result();
        JsonObject originalForm = application.getJsonObject("form");
        JsonObject translatedFiles = application.getJsonObject(TRANSLATION, new JsonObject())
          .getJsonObject("files", new JsonObject());
        List<String> targetLangs = application.getJsonObject(TRANSLATION, new JsonObject())
          .getJsonArray("targetLanguages", new JsonArray())
          .stream().map(Object::toString).toList();

        JsonObject translationUpdate = super.buildFormTranslationsAndSetStatus(
          originalForm, dict, targetLangs, applicationId, translatedFiles, DATA_REQUEST_APPLICATION_COLLECTION
        );
        applyTranslationUpdate(application, translationUpdate);

        TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
          application.getJsonObject(TRANSLATION, new JsonObject())
        );
        application.getJsonObject(TRANSLATION).put(STATUS, overallStatus);

        if (overallStatus != TranslationStatus.COMPLETED) {
          log.info(
            "Translation not yet complete for application [{}], skipping dispatch (status = {})",
            applicationId, overallStatus);
          replaceApplication(application).onSuccess(r -> {
          }).onFailure(err -> log.error("Failed to replace", err));
          return;
        }

        replaceApplication(application).onSuccess(replaceResult -> {
          log.info("Application updated with translations. Proceeding to dispatch.");

          ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
            application.getJsonObject(STATUS).getString(STATUS));

          if (ApplicationStatus.SUBMITTED.equals(requestStatus)) {
            sendApplicationForDispatch(application, DATA_REQUEST_APPLICATION_COLLECTION);

            String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
              AuditLogActionDescription.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
              applicationId, application.getString(TITLE));
            auditLogManager.insertAuditLog(AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
              application.getString(ID),
              AuditLogAction.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
              auditLogActionDescription, true, null);
          }
        }).onFailure(err -> log.error("Failed to replace application with translations", err));
      } else {
        log.error("Could not find application to apply translations: {}", applicationId);
      }
    });
  }

  public void processFileTranslationCallback(String applicationId, String originalFileId,
    Map<String, JsonObject> uploadedFiles) {
    JsonObject query = new JsonObject().put(ID, applicationId);

    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, query, null, res -> {
      if (res.succeeded() && res.result() != null) {
        JsonObject application = res.result();

        applyFileTranslationUpdates(application, applicationId, originalFileId, uploadedFiles);

        TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
          application.getJsonObject(TRANSLATION, new JsonObject())
        );
        application.getJsonObject(TRANSLATION).put(STATUS, overallStatus);

        if (overallStatus != TranslationStatus.COMPLETED) {
          log.info(
            "Translation not yet complete for application [{}], skipping dispatch (status = {})",
            applicationId, overallStatus);
          replaceApplication(application).onSuccess(r -> {
          }).onFailure(err -> log.error("Failed to replace", err));
          return;
        }

        replaceApplication(application).onSuccess(replaceResult -> {
          log.info("Application [{}] updated with translated file references for [{}]",
            applicationId, originalFileId);

          ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
            application.getJsonObject(STATUS).getString(STATUS));

          if (ApplicationStatus.SUBMITTED.equals(requestStatus)) {
            sendApplicationForDispatch(application, DATA_REQUEST_APPLICATION_COLLECTION);

            String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
              AuditLogActionDescription.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
              applicationId, application.getString(TITLE));

            auditLogManager.insertAuditLog(AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
              application.getString(ID),
              AuditLogAction.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
              auditLogActionDescription, true, null);
          }
        }).onFailure(
          err -> log.error("Failed to replace application with file translation updates", err));

      } else {
        log.error("Could not find application [{}] to update file translations", applicationId);
      }
    });
  }

  public void markTranslationError(String applicationId, String errorType, String fileId) {
    super.markTranslationError(applicationId, DATA_REQUEST_APPLICATION_COLLECTION, errorType, fileId);
  }

}
