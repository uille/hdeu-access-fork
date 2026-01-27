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
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE;
import static eu.europa.data.health.access.Constants.COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.EXTRACTION_CRITERIA_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.FORM;
import static eu.europa.data.health.access.Constants.FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_2;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_3;
import static eu.europa.data.health.access.Constants.HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.MORE_INFO_ON_EXTRACTION_PERIODS;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.OPTIONS;
import static eu.europa.data.health.access.Constants.ORDER_OF_CONTROLS_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.OTHER_PERMIT_APPLICATIONS;
import static eu.europa.data.health.access.Constants.PREVIOUSLY_ISSUED_PERMIT_DATE_2;
import static eu.europa.data.health.access.Constants.PREVIOUSLY_ISSUED_PERMIT_NUMBER_2;
import static eu.europa.data.health.access.Constants.PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM_2;
import static eu.europa.data.health.access.Constants.PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_TO_2;
import static eu.europa.data.health.access.Constants.PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.REGEX;
import static eu.europa.data.health.access.Constants.RELATIONSHIP_TO_STUDY_COHORT;
import static eu.europa.data.health.access.Constants.SECTION;
import static eu.europa.data.health.access.Constants.SECTION_10;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_7;
import static eu.europa.data.health.access.Constants.SECTION_8;
import static eu.europa.data.health.access.Constants.SECTION_NUMBER;
import static eu.europa.data.health.access.Constants.SENDING;
import static eu.europa.data.health.access.Constants.SPECIFY_OTHER;
import static eu.europa.data.health.access.Constants.SPECIFY_OTHER_RELATIVES;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.TIME_PERIOD_FOR_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS;
import static eu.europa.data.health.access.Constants.VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS;
import static eu.europa.data.health.access.Constants.WHEN_DATA_NEEDED;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT;
import static eu.europa.data.health.access.Constants.WILL_CONTROLS_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_RELATIVES_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES;
import static eu.europa.data.health.access.Constants.WILL_THE_DATA_BE_TRANSFERRED;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.Sections;
import eu.europa.data.health.access.TranslationStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import eu.europa.data.health.access.dto.sections.data_access.Section7DTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.utils.ApplicationFormUtils;
import eu.europa.data.health.access.validations.business_validators.DataAccessApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataAccessApplicationManager extends CommonApplicationManager {

  private final MongoClient mongoClient;

  private final DataAccessApplicationValidationManager dataAccessApplicationValidationManager;

  private final DataAccessApplicationBusinessValidationManager dataAccessApplicationBusinessValidationManager;

  private final AuditLogManager auditLogManager;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String dataAccessManagerClassName = this.getClass().getSimpleName() + " - ";

  /**
   * Data access application constructor
   */
  public DataAccessApplicationManager(DBHandler dbHandler, MongoClient mongoClient,
    BasketManager basketManager, HubStoreClient hubStoreClient, Vertx vertx,
    DataAccessApplicationValidationManager dataAccessApplicationValidationManager,
    DataAccessApplicationBusinessValidationManager dataAccessApplicationBusinessValidationManager,
                                      AuditLogManager auditLogManager, HubTranslationClient hubTranslationClient, HubSearchClient hubSearchClient,
                                      AuthClient authClient) {
    super(dbHandler, basketManager, mongoClient, vertx, hubStoreClient, auditLogManager, hubTranslationClient, hubSearchClient, authClient);
    this.mongoClient = mongoClient;
    this.dataAccessApplicationValidationManager = dataAccessApplicationValidationManager;
    this.dataAccessApplicationBusinessValidationManager = dataAccessApplicationBusinessValidationManager;
    this.auditLogManager = auditLogManager;
  }

  /**
   * Get a Document by field ID for the Data Access Application collection.
   */
  public void getApplicationById(RoutingContext context) {
    super.findById(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Get a Document by field ID for the Data Access Application collection.
   */
  public void getEveryDataAccessApplicationById(RoutingContext context) {
    super.findByIdWithoutSecurity(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Create a Data Access Application
   */
  public void createApplication(RoutingContext context) {
    super.createApplication(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Get all Documents for the Data Access Application collection.
   */
  public void getAllDataAccessApplications(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");
    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    getQueryOptionsApplication(context).onSuccess(queryOptions -> {
      log.debug("{}Retrieved query options ", dataAccessManagerClassName);
      super.getApplications(context, DATA_ACCESS_APPLICATION_COLLECTION, finalPage, finalPageLimit,
        StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
        StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc", queryOptions);
    }).onFailure(err -> {
      log.error("{}Error building query options, will proceed without them",
        dataAccessManagerClassName);
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Could not construct query options");
    });
  }

  /**
   * Get all Ongoing Data Access Application collection.
   */
  public void getAllOnGoingDataAccessApplications(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");

    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    JsonObject searchFields = context.body().asJsonObject();
    if (searchFields == null) {
      searchFields = new JsonObject();
    }

    String title = searchFields.getString("title");

    JsonObject queryOptions = new JsonObject();

    if (StringUtils.isNotBlank(title)) {
      queryOptions.put("title",
        new JsonObject()
          .put("$regex", title)
          .put("$options", "i")
      );
    }
    queryOptions.put("status.status",
      new JsonObject().put("$in",
        new JsonArray().add("ON-GOING").add("COMPLETED")
      )
    );

    log.debug("{}Using query options for ongoing applications: {}",
      dataAccessManagerClassName, queryOptions.encode());

    super.getApplications(
      context,
      DATA_ACCESS_APPLICATION_COLLECTION,
      finalPage,
      finalPageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc",
      queryOptions
    );
  }

  public void getAllDecisionIssuedDataAccessApplications(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");

    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    JsonObject searchFields = context.body().asJsonObject();
    if (searchFields == null) {
      searchFields = new JsonObject();
    }

    String title = searchFields.getString("title");
    String decisionType = searchFields.getString("decisionType"); // NEW

    JsonObject queryOptions = new JsonObject();

    // Title filter
    if (StringUtils.isNotBlank(title)) {
      queryOptions.put("title",
        new JsonObject()
          .put("$regex", title)
          .put("$options", "i")
      );
    }

    // NEW — Decide whether we want positive or negative decisions
    Boolean isPositiveDecision = null;

    if (StringUtils.isNotBlank(decisionType)) {
      switch (decisionType) {
        case "POSITIVE_DECISION_ISSUED":
          isPositiveDecision = true;
          break;
        case "NEGATIVE_DECISION_ISSUED":
          isPositiveDecision = false;
          break;
        default:
          // unknown value, ignore or log warning
          log.warn("Unknown decisionType '{}'", decisionType);
          break;
      }
    }

    log.debug("{}Using query options for ongoing applications: {}",
      dataAccessManagerClassName, queryOptions.encode());

    super.getDecisionIssuedDatasets(
      context,
      DATA_ACCESS_APPLICATION_COLLECTION,
      finalPage,
      finalPageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc",
      queryOptions,
      isPositiveDecision
    );
  }

  public void getAllDataPermitIssuedApplications(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");

    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    JsonObject searchFields = context.body().asJsonObject();
    if (searchFields == null) {
      searchFields = new JsonObject();
    }

    String title = searchFields.getString("title");

    JsonObject queryOptions = new JsonObject();

    if (StringUtils.isNotBlank(title)) {
      queryOptions.put("title",
        new JsonObject()
          .put("$regex", title)
          .put("$options", "i")
      );
    }

    log.debug("{}Using query options for ongoing applications: {}",
      dataAccessManagerClassName, queryOptions.encode());

    super.getDataPermitGrantedDatasets(context, DATA_ACCESS_APPLICATION_COLLECTION, finalPage,
      finalPageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc", queryOptions);
  }


  /**
   * Get all Documents for the Data Access Application collection.
   */
  public void getAllDataAccessDatasets(@NotNull RoutingContext context) {
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");
    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");
    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    getQueryOptionsDataset(context).onSuccess(queryOptions -> {
      log.debug("{}Retrieved query options ", dataAccessManagerClassName);
      super.getDatasets(context, DATA_ACCESS_APPLICATION_COLLECTION, finalPage, finalPageLimit,
        StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
        StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc", queryOptions);
    }).onFailure(err -> {
      log.error("{}Error building query options, will proceed without them",
        dataAccessManagerClassName);
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Could not construct query options");
    });
  }

  /**
   * Update a Document for the Data Access Application collection
   */
  public void updateApplication(@NotNull RoutingContext context) {
    super.updateEntity(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Delete a Document by field ID for the Data Access Application collection.
   */
  public void deleteDataAccessApplicationById(@NotNull RoutingContext context) {
    deleteApplicationById(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Delete all Documents for the Data Access Application collection.
   */
  public void deleteAllDataAccessApplication(@NotNull RoutingContext context) {
    super.deleteAllEntities(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void putDistributionsApplication(@NotNull RoutingContext context) {
    putDistributionsApplication(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Delete a list of datasets for the Data Access Application collection.
   */
  public void deleteDatasets(@NotNull RoutingContext context) {
    deleteDatasets(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Delete a list of distributions for the Data Access Application collection.
   */
  public void deleteDistributions(@NotNull RoutingContext context) {
    deleteDistributions(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void createOrUpdateSection(@NotNull RoutingContext context) {
    createOrUpdateSection(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getSection(@NotNull RoutingContext context) {

    JsonObject query = new JsonObject().put(ID, context.pathParam(APPLICATION_ID));
    query.put(USER_ID, Context.getEcasId(context));
    String sectionNumber = SECTION + context.pathParam(SECTION_NUMBER);

    JsonObject sectionToRetrieve = new JsonObject().put(FORM + sectionNumber, 1);
    FindOptions options = new FindOptions().setFields(sectionToRetrieve);

    // Search in database based on the provided application id
    mongoClient.findWithOptions(DATA_ACCESS_APPLICATION_COLLECTION, query, options, ar -> {
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

  public void changeCatalogStatus(@NotNull RoutingContext context) {
    changeCatalogStatus(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void changeDatasetStatus(@NotNull RoutingContext context) {
    changeDatasetStatus(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void changeStatus(@NotNull RoutingContext context) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String targetStatus = context.body().asJsonObject().getString(STATUS);

    if ("EDIT".equalsIgnoreCase(targetStatus)) {

      JsonObject query = new JsonObject().put(ID, applicationId);
      query.put(USER_ID, Context.getEcasId(context));
      query.put("status.status", new JsonObject().put("$in",
        new JsonArray()
          .add(DRAFT.getStatus())
          .add(ON_GOING.getStatus())
      ));
      query.put("translation.status", new JsonObject()
        .put("$in", new JsonArray()
          .add(TranslationStatus.FORM_PENDING.name())
          .add(TranslationStatus.COMPLETED.name())));

      JsonObject newStatus = new JsonObject()
        .put("$set", new JsonObject()
          .put("translation.status", TranslationStatus.FILES_COMPLETED.name()))
        .put("$unset", new JsonObject()
          .put("translation.form", true)
          .put("form_translations", true));

      mongoClient.findOneAndUpdate(DATA_ACCESS_APPLICATION_COLLECTION, query, newStatus,
        updateResult -> {
          if (updateResult.succeeded()) {
            if (updateResult.result() != null) {
              //JsonObject mongoApplication = updateResult.result();
              setUpMessageContextResponse(context, HttpResponseStatus.OK,
                "Changed to Edit");
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
                "Application not found");
            }
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Error occurred: " + updateResult.cause().getMessage());
          }
        });

    } else if ("TRANSLATE".equalsIgnoreCase(targetStatus)) {

      JsonObject query = new JsonObject().put(ID, applicationId);
      query.put(USER_ID, Context.getEcasId(context));
      query.put("status.status", new JsonObject().put("$in",
        new JsonArray()
          .add(DRAFT.getStatus())
          .add(ON_GOING.getStatus())
      ));
      query.put("translation.status", TranslationStatus.FILES_COMPLETED.name());

      JsonObject translatingStatus = new JsonObject().put("$set",
        new JsonObject().put("translation.status", TranslationStatus.FORM_PENDING.name()));

      mongoClient.findOneAndUpdate(DATA_ACCESS_APPLICATION_COLLECTION, query, translatingStatus,
        updateResult -> {
          if (updateResult.succeeded()) {
            if (updateResult.result() != null) {

              JsonObject mongoApplication = updateResult.result();

              // Fetch source language
              String sourceLang = mongoApplication.getString("inputLanguage", "EN")
                .toUpperCase(Locale.ENGLISH);

              JsonArray tl = mongoApplication.getJsonObject("translation")
                .getJsonArray("targetLanguages");
              ArrayList<String> targetLanguages = tl.stream()
                .map(Object::toString)
                .collect(Collectors.toCollection(ArrayList::new));

              boolean hasOtherLanguages = targetLanguages.stream()
                .anyMatch(lang -> !lang.equalsIgnoreCase(sourceLang));
              targetLanguages.remove(sourceLang);

              JsonObject formTranslations = getDefaultEnglishFormTranslation(sourceLang,
                mongoApplication);

              if (hasOtherLanguages) {

                triggerFormTranslationRequest(applicationId,
                  mongoApplication.getJsonObject("form"), DATA_ACCESS_APPLICATION_COLLECTION,
                  sourceLang, targetLanguages, formTranslations, true);

                setUpMessageContextResponse(context, HttpResponseStatus.OK,
                  "Form translation initiated");
              } else {
                log.debug("No translation required");
                JsonObject translationUpdate = new JsonObject()
                  .put("$set", new JsonObject()
                    .put(DATE_MODIFIED, Instant.now())
                    .put("translation.status", TranslationStatus.COMPLETED.name())
                    .put("translation.sourceLanguage", sourceLang)
                    .put("translation.targetLanguages", targetLanguages)
                    .put("form_translations", formTranslations)
                  );
                mongoClient.updateCollection(
                  DATA_ACCESS_APPLICATION_COLLECTION,
                  new JsonObject().put("_id", mongoApplication.getString(ID)),
                  translationUpdate,
                  ar -> {
                    if (ar.succeeded()) {
                      setUpMessageContextResponse(context, HttpResponseStatus.OK, "No translation required");
                    } else {
                      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "Failed to complete translation update: " + ar.cause().getMessage());
                    }
                  }
                );
              }
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
                "Application not found");
            }
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Error occurred: " + updateResult.cause().getMessage());
          }
        });

    } else if ("SUBMITTED".equalsIgnoreCase(targetStatus)) {

      JsonObject query = new JsonObject()
        .put("translation.status", "COMPLETED")
        .put("$or", new JsonArray()
          .add(new JsonObject()
            .put(ID, applicationId)
            .put(USER_ID, Context.getEcasId(context))
            .put("status.status", DRAFT.getStatus()))
          .add(new JsonObject()
            .put(ID, applicationId)
            .put("status.status", ON_GOING.getStatus())
            .put("date_submitted", new JsonObject().put("$exists", true)))
        );

      JsonObject submittingStatus = new JsonObject().put("$set",
        new JsonObject().put("status.status", SENDING));

      mongoClient.findOneAndUpdate(DATA_ACCESS_APPLICATION_COLLECTION, query, submittingStatus,
        updateResult -> {
          if (updateResult.succeeded()) {
            if (updateResult.result() != null) {

              JsonObject mongoApplication = updateResult.result();

              if (mongoApplication.containsKey("date_submitted")
                && hasAtLeastOneAdditionalInformationAsLatest(
                mongoApplication.getJsonArray("catalogs"))) {

                JsonObject status = new JsonObject().put("status", ON_GOING.getStatus());

                //validate status change
                if (!canChangeStatus(status, mongoApplication, context)) {
                  rollbackStatus(applicationId, ON_GOING, DATA_ACCESS_APPLICATION_COLLECTION);
                  return;
                }

                super.updateApplicationStatus(mongoApplication, status, context);
                performCleanUpForm(mongoApplication);
                updateCatalogStatus(mongoApplication);
                int currentVersion = mongoApplication.getInteger("version") + 1;
                mongoApplication.put("version", currentVersion);

                if (currentVersion > 1) {
                  // 1) Read target languages from mongoApplication.translation.targetLanguages
                  JsonObject translation = mongoApplication.getJsonObject("translation");
                  JsonArray tl = translation != null ? translation.getJsonArray("targetLanguages") : null;

                  if (tl != null && !tl.isEmpty()) {
                    // Normalize targets to lowercase because form_translations keys are usually "en", "fr", ...
                    Set<String> targetLangsLower = tl.stream()
                      .map(Object::toString)
                      .map(s -> s.trim().toLowerCase(Locale.ROOT))
                      .collect(Collectors.toSet());

                    // 2) Read current form_translations
                    JsonObject formTranslations = mongoApplication.getJsonObject("form_translations");

                    if (formTranslations != null && !formTranslations.isEmpty()) {
                      JsonObject filtered = new JsonObject();

                      // Keep only langs that exist in targetLanguages
                      for (String lang : targetLangsLower) {
                        JsonObject langObj = formTranslations.getJsonObject(lang);
                        if (langObj != null) {
                          filtered.put(lang, langObj);
                        }
                      }

                      // 3) Replace form_translations with filtered
                      mongoApplication.put("form_translations", filtered);
                    }
                  }
                }

                // Replace application and return result
                replaceApplication(mongoApplication).onSuccess(replaceResult -> {

                  // If status is submitted then we need to add the application to the dispatcher table
                  ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
                    mongoApplication.getJsonObject(STATUS).getString(STATUS));
                  if (ON_GOING.equals(requestStatus)) {
                    sendApplicationForDispatch(mongoApplication,
                      DATA_ACCESS_APPLICATION_COLLECTION, OperationStatus.UPDATE);

                    // trigger extra translations (all eu languages, for registry page)
                    triggerRemainingTranslations(applicationId, mongoApplication, DATA_ACCESS_APPLICATION_COLLECTION);

                    removeAdditionalInformationFromDataAccessCollection(applicationId)
                      .onFailure(err -> log.warn("Failed to unset additional_information for {}: {}", applicationId, err.getMessage()));

                    //Audit Log
                    String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                      AuditLogActionDescription.DATA_ACCESS_APPLICATION_SUBMITTED.toString(),
                      applicationId, mongoApplication.getString(TITLE));
                    auditLogManager.insertAuditLog(
                      AuditLogObject.DATA_ACCESS_APPLICATION.toString(),
                      applicationId,
                      AuditLogAction.DATA_ACCESS_APPLICATION_SUBMITTED.toString(),
                      auditLogActionDescription, true, context);
                  }
                  setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
                }).onFailure(fail -> {
                  rollbackStatus(applicationId, ON_GOING, DATA_ACCESS_APPLICATION_COLLECTION);
                  setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    fail.getMessage());
                });

              } else {
                JsonObject status = new JsonObject().put("status", ON_GOING.getStatus());

                //validate status change
                if (!canChangeStatus(status, mongoApplication, context)) {
                  rollbackStatus(applicationId, ApplicationStatus.DRAFT,
                    DATA_ACCESS_APPLICATION_COLLECTION);
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

                    super.updateApplicationStatus(mongoApplication, status, context);
                    performCleanUpForm(mongoApplication);

                    updateCatalogStatus(mongoApplication);

                    // Replace application and return result
                    replaceApplication(mongoApplication).onSuccess(replaceResult -> {

                      // If status is submitted then we need to add the application to the dispatcher table
                      ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
                        mongoApplication.getJsonObject(STATUS).getString(STATUS));
                      if (ON_GOING.equals(requestStatus)) {
                        sendApplicationForDispatch(mongoApplication,
                          DATA_ACCESS_APPLICATION_COLLECTION, OperationStatus.CREATE);

                        // trigger extra translations (all eu languages, for registry page)
                        triggerRemainingTranslations(applicationId, mongoApplication, DATA_ACCESS_APPLICATION_COLLECTION);

                        removeAdditionalInformationFromDataAccessCollection(applicationId)
                          .onFailure(err -> log.warn("Failed to unset additional_information for {}: {}", applicationId, err.getMessage()));
                        //Audit Log
                        String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                          AuditLogActionDescription.DATA_ACCESS_APPLICATION_SUBMITTED.toString(),
                          applicationId, mongoApplication.getString(TITLE));
                        auditLogManager.insertAuditLog(
                          AuditLogObject.DATA_ACCESS_APPLICATION.toString(),
                          applicationId,
                          AuditLogAction.DATA_ACCESS_APPLICATION_SUBMITTED.toString(),
                          auditLogActionDescription, true, context);
                      }
                      setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
                    }).onFailure(fail -> {
                      rollbackStatus(applicationId, ApplicationStatus.DRAFT,
                        DATA_ACCESS_APPLICATION_COLLECTION);
                      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        fail.getMessage());
                    });
                  });
              }
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

  private Future<Void> removeAdditionalInformationFromDataAccessCollection(String applicationId) {
    Promise<Void> p = Promise.promise();

    JsonObject query = new JsonObject().put("_id", applicationId);

    JsonObject update = new JsonObject().put("$unset", new JsonObject().put("additionalInformation", ""));

    mongoClient.findOneAndUpdate(DATA_ACCESS_APPLICATION_COLLECTION, query, update, ar -> {
      if (ar.succeeded()) p.complete();
      else p.fail(ar.cause());
    });

    return p.future();
  }

  private Future<JsonObject> getQueryOptionsApplication(@NotNull RoutingContext context) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject searchFields = context.body().asJsonObject();

    String title = searchFields.getString("title");
    JsonArray statusList = searchFields.getJsonArray("status");
    JsonArray hdabList = searchFields.getJsonArray("hdab");
    JsonArray geographicalList = searchFields.getJsonArray("geographicalList");

    // Collect AND clauses
    JsonObject query = new JsonObject();

    // title (top-level string) -> case-insensitive regex
    if (title != null && !title.isBlank()) {
      query.put("title", new JsonObject().put(REGEX, title).put(OPTIONS, "i"));
      ;
    }

    if (statusList != null && !statusList.isEmpty()) {
      query.put("status.status",
        new JsonObject().put("$in", statusList));
    }

    if (hdabList != null && !hdabList.isEmpty()) {
      query.put("datasets.hdab.name",
        new JsonObject().put("$in", hdabList));
    }

    if (geographicalList != null && !geographicalList.isEmpty()) {
      query.put("datasets.country.label",
        new JsonObject().put("$in", geographicalList));
    }

    String userId = Context.getEcasId(context);

    if (StringUtils.isNotEmpty(userId)) {
      query.put("userId", userId);
    }

    promise.complete(query);
    return promise.future();
  }

  private Future<JsonObject> getQueryOptionsDataset(@NotNull RoutingContext context) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject searchFields = context.body().asJsonObject();
    if (searchFields == null) {
      searchFields = new JsonObject(); // <-- avoid NPE
    }

    String title = searchFields.getString("title");
    JsonArray datasetStatusList = searchFields.getJsonArray("datasetStatus");
    JsonArray permitStatusList = searchFields.getJsonArray("permitStatus");
    JsonArray hdabList = searchFields.getJsonArray("hdab");
    JsonArray applicationTitle = searchFields.getJsonArray("applicationTitle");

    JsonObject query = new JsonObject();
    JsonArray andClauses = new JsonArray();

    // title (regex)
    if (title != null && !title.isBlank()) {
      andClauses.add(new JsonObject()
        .put("title", new JsonObject().put("$regex", title).put("$options", "i")));
    }

    // applicationTitle (title IN [...])
    if (applicationTitle != null && !applicationTitle.isEmpty()) {
      andClauses.add(new JsonObject()
        .put("title", new JsonObject().put("$in", applicationTitle)));
    }

    // dataset-level latest decision status
    if (datasetStatusList != null && !datasetStatusList.isEmpty()) {
      andClauses.add(new JsonObject().put(
        "datasets.decisions",
        new JsonObject().put("$elemMatch", new JsonObject()
          .put("latest", 1)
          .put("datasetStatus", new JsonObject().put("$in", datasetStatusList))
        )
      ));
    }

    // dataset-level latest decision status
    if (permitStatusList != null && !permitStatusList.isEmpty()) {
      andClauses.add(new JsonObject().put(
        "datasets.permits",
        new JsonObject().put("$elemMatch", new JsonObject()
          .put("latest", 1)
          .put("permit_status", new JsonObject().put("$in", permitStatusList))
        )
      ));
    }

    // dataset-level HDAB name (IMPORTANT: keep datasets. prefix)
    if (hdabList != null && !hdabList.isEmpty()) {
      andClauses.add(new JsonObject()
        .put("datasets.hdab.name", new JsonObject().put("$in", hdabList)));
    }

    // user constraint (application-level)
    String userId = Context.getEcasId(context);
    if (StringUtils.isNotEmpty(userId)) {
      andClauses.add(new JsonObject().put("userId", userId));
    }

    // Collapse ANDs
    if (!andClauses.isEmpty()) {
      query.put("$and", andClauses);
    }

    promise.complete(query);
    return promise.future();
  }

  private boolean performBusinessValidation(JsonObject form, RoutingContext context) {

    return dataAccessApplicationBusinessValidationManager.section4Validation(form, context)
      && dataAccessApplicationBusinessValidationManager.section8Validation(form, context);
  }

  private boolean performDTOValidation(JsonObject mongoApplication, RoutingContext context) {

    // First check all required sections are saved into the database
    JsonObject form = mongoApplication.getJsonObject("form");
    boolean allSectionsAreSaved = validateKeys(form.getMap());

    if (allSectionsAreSaved) {

      //Retrieve the sections saved in database
      JsonObject databaseSections = mongoApplication.getJsonObject("form");

      for (Sections section : Sections.values()) {

        JsonObject databaseSection = new JsonObject();
        JsonArray databaseSection6 = new JsonArray();
        if (!section.getSection().equalsIgnoreCase(SECTION_6)) {
          databaseSection = databaseSections.getJsonObject(section.getSection());
        } else {
          databaseSection6 = databaseSections.getJsonArray(section.getSection());
        }

        // Since section 9 is not mandatory, if it is not provided then no validations should be executed
        if (Sections.SECTION_9.equals(section) && databaseSection == null) {
          return true;
        }

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        SectionDTO sectionDTO = dataAccessApplicationValidationManager.generateSectionDTO(
          section.getSectionNumber(),
          section.getSection().equals(SECTION_6) ? databaseSection6.toString()
            : databaseSection.toString(), context);

        // Validate SectionDTO
        Set<ConstraintViolation<SectionDTO>> violations = validator.validate(sectionDTO);

        // provideInformationList is a dynamic list which is required based on condition
        // remove violation from provideInformationList when the list is null
        if (section.getSection().equals(SECTION_7)
          && ((Section7DTO) sectionDTO).getProvideInformationList() == null) {
          violations.removeIf(violation -> violation.getPropertyPath().toString()
            .startsWith("provideInformationList"));
        }

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
      SECTION_6, SECTION_7, SECTION_8, SECTION_10);

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
    if (!ApplicationStatus.isValidStatus(statusStr)) {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
        "Status cannot be " + "updated: wrong status value");
      return false;
    }

    ApplicationStatus requestStatus = ApplicationStatus.fromStatus(statusStr);

    if (ON_GOING.equals(requestStatus)) {
      if (performDTOValidation(mongoApplication, context)) {
        if (performBusinessValidation(mongoApplication.getJsonObject("form"), context))
          return dataAccessApplicationBusinessValidationManager.validateCatalogs(mongoApplication, context);
      }
    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN,
        "Status cannot be updated: wrong flow");
    }

    return false;
  }

  void performCleanUpForm(JsonObject mongoApplication) {
    cleanSection2(mongoApplication.getJsonObject("form"));
    cleanSection3(mongoApplication.getJsonObject("form"));
    cleanSection4(mongoApplication.getJsonObject("form"));
    cleanSection5(mongoApplication.getJsonObject("form"));
    cleanSection6(mongoApplication.getJsonObject("form"));
    cleanSection7(mongoApplication.getJsonObject("form"));
    cleanSection8(mongoApplication.getJsonObject("form"));
  }

  private void cleanSection5(JsonObject mongoApplicationForm) {
    JsonObject section5 = mongoApplicationForm.getJsonObject(SECTION_5);
    if (Objects.isNull(section5)) {
      return;
    }
    JsonObject formatOfTheElectronicHealthData = section5.getJsonObject(
      FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    if (Objects.nonNull(formatOfTheElectronicHealthData) && !Objects.equals(
      formatOfTheElectronicHealthData.getString("key"), "b")) {
      section5.remove(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A);
      section5.remove(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B);
      section5.remove("whyNeedForPseudonymisation");
    }

    mongoApplicationForm.put(SECTION_5, section5);
  }

  private void cleanSection6(JsonObject mongoApplicationForm) {
    JsonArray section6Array = mongoApplicationForm.getJsonArray(SECTION_6);
    if (Objects.isNull(section6Array) || section6Array.isEmpty()) {
      return;
    }

    for (int i = 0; i < section6Array.size(); i++) {

      //SECTION 6_1
      JsonObject section = section6Array.getJsonObject(i);
      JsonObject howIsTheStudyCohortFormed = section.getJsonObject("howIsTheStudyCohortFormed");

      if (Objects.nonNull(howIsTheStudyCohortFormed) &&
        (!Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(
          howIsTheStudyCohortFormed.getString("key"), "c"))) {
        section.remove(HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
      }

      JsonObject hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants = section.getJsonObject(
        HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
      if ((Objects.nonNull(howIsTheStudyCohortFormed) && (
        !Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(
          howIsTheStudyCohortFormed.getString("key"), "c"))) || (
        Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
          && !Objects.equals(
          hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString("key"),
          "a"))) {
        section.remove("doesTheInformedConsentCoverTheRequestedRegistryExtractions");
        section.remove("consentAndInformationLetterAttachment");
      }

      if ((Objects.nonNull(howIsTheStudyCohortFormed) && (
        !Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(
          howIsTheStudyCohortFormed.getString("key"), "c")))) {
        section.remove("confirmThatDataPermitHasBeenGrantedForTheResearchProject");
        section.remove("permitDecisionAttachment");
        section.remove("previouslyIssuedPermitIssuer1");
        section.remove("previouslyIssuedPermitDate1");
        section.remove("previouslyIssuedPermitValidityPeriodFrom1");
        section.remove("previouslyIssuedPermitValidityPeriodTo1");
        section.remove("previouslyIssuedPermitNumber1");
      }

      if ((Objects.nonNull(howIsTheStudyCohortFormed) && !Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "b")) || (
        Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
          && !Objects.equals(
          hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString("key"),
          "b"))) {
        section.remove("howTheStudyCohortWasObtained");
      }

      JsonObject providedInformationOfTheDataUseToCorrespondingSubjects = section.getJsonObject(
        PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS);

      if (Objects.nonNull(providedInformationOfTheDataUseToCorrespondingSubjects)
        && !Objects.equals(
        providedInformationOfTheDataUseToCorrespondingSubjects.getString("key"),
        "a")) {
        section.remove("howProvidedInformationOfTheDataUseToCorrespondingSubjects");
      }

      if (Objects.nonNull(providedInformationOfTheDataUseToCorrespondingSubjects)
        && !Objects.equals(providedInformationOfTheDataUseToCorrespondingSubjects.getString("key"),
        "b")) {
        section.remove("whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects");
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && (
        !Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(
          howIsTheStudyCohortFormed.getString("key"), "c"))) {
        section.remove(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON);
        section.remove("informationProviderName");
        section.remove("informationProviderEmail");
        section.remove("informationProviderPhone");
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && !Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "d")) {
        section.remove("whyNeedDataOfaWholePopulation");
      }

      JsonObject extractionMethod = section.getJsonObject("extractionMethod");

      if (Objects.nonNull(extractionMethod) && !extractionMethod.getString("key").equals("c")) {
        section.remove("samplingMethod");
      }

      if (Objects.nonNull(extractionMethod) && (!extractionMethod.getString("key").equals("c")
        && !extractionMethod.getString("key")
        .equals("a"))) {
        section.remove("sampleSize");
      }

      JsonObject howOftenDoesTheDataNeedToBeExtracted = section.getJsonObject(
        "howOftenDoesTheDataNeedToBeExtracted");
      if (Objects.nonNull(howOftenDoesTheDataNeedToBeExtracted) && !Objects.equals(
        howOftenDoesTheDataNeedToBeExtracted.getString("key"), "b")) {
        section.remove(NEED_FOR_DATA_EXTRACTION_EVERY);
      }

      JsonObject needForDataExtractionEvery = section.getJsonObject(
        NEED_FOR_DATA_EXTRACTION_EVERY);
      if ((Objects.nonNull(howOftenDoesTheDataNeedToBeExtracted) && !Objects.equals(
        howOftenDoesTheDataNeedToBeExtracted.getString("key"), "b"))
        || (Objects.nonNull(needForDataExtractionEvery) && !Objects.equals(
        needForDataExtractionEvery.getString("key"), "d"))) {
        section.remove("needForDataExtractionEveryOther");
      }

      //SECTION 6_2

      JsonObject willControlsBeExtracted = section.getJsonObject(WILL_CONTROLS_BE_EXTRACTED);
      if (Objects.nonNull(willControlsBeExtracted) && !Objects.equals(
        willControlsBeExtracted.getString("key"), "a")) {
        section.remove(HAS_THE_GROUP_RELATIVES_FORMED_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_DATE_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_TO_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_NUMBER_2);
        section.remove("previouslyIssuedPermitIssuer2");
        section.remove(PREVIOUSLY_ISSUED_PERMIT_DATE_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_TO_2);
        section.remove(PREVIOUSLY_ISSUED_PERMIT_NUMBER_2);
        section.remove(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS);
        section.remove(VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS);
        section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION);
        section.remove(EXTRACTION_CRITERIA_FOR_CONTROLS);
        section.remove("sizeOfControlGroup");
        section.remove("sizeOfControlGroupEstimationOrExact");
        section.remove("controlsPerPersonInStudyCohort");
        section.remove("exclusionCriteriaForControls");
        section.remove(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY);
        section.remove(DATA_EXTRACTION_FREQUENCY);
        section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION);
        section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
        section.remove(SPECIFY_OTHER);
        section.remove(MORE_INFO_ON_EXTRACTION_PERIODS);
        section.remove(ORDER_OF_CONTROLS_DATA_EXTRACTION);
      } else {
        JsonObject hasTheGroupRelativesFormed2 = section.getJsonObject(
          HAS_THE_GROUP_RELATIVES_FORMED_2);

        if (Objects.nonNull(hasTheGroupRelativesFormed2) && !Objects.equals(
          hasTheGroupRelativesFormed2.getString("key"),
          "a")) {
          section.remove("previouslyIssuedPermitIssuer2");
          section.remove(PREVIOUSLY_ISSUED_PERMIT_DATE_2);
          section.remove(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM_2);
          section.remove(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_TO_2);
          section.remove(PREVIOUSLY_ISSUED_PERMIT_NUMBER_2);
        }

        JsonObject willSameDataBeExtractedForControls = section.getJsonObject(
          WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS);
        if (Objects.nonNull(willSameDataBeExtractedForControls) && !Objects.equals(
          willSameDataBeExtractedForControls.getString("key"), "b")) {
          section.remove(VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS);
          section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION);
        }

        JsonObject willDataForControlsBeExtractedSimultaneously =
          section.getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY);
        if (Objects.nonNull(willDataForControlsBeExtractedSimultaneously) && !Objects.equals(
          willDataForControlsBeExtractedSimultaneously.getString("key"), "b")) {

          section.remove(DATA_EXTRACTION_FREQUENCY);
          section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION);
          section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          section.remove(SPECIFY_OTHER);
          section.remove(MORE_INFO_ON_EXTRACTION_PERIODS);
          section.remove(ORDER_OF_CONTROLS_DATA_EXTRACTION);
        } else {
          JsonObject dataExtractionFrequency =
            section.getJsonObject(DATA_EXTRACTION_FREQUENCY);
          if (Objects.nonNull(dataExtractionFrequency) && !Objects.equals(
            dataExtractionFrequency.getString("key"), "b")) {
            section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION);
            section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          }

          JsonObject dataNeedsToBeExtractedEvery =
            section.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          if (Objects.nonNull(dataNeedsToBeExtractedEvery) && (!Objects.equals(
            dataExtractionFrequency.getString("key"), "b") ||
            !Objects.equals(dataNeedsToBeExtractedEvery.getString("key"), "d"))) {
            section.remove(SPECIFY_OTHER);
          }
        }
      }

      //SECTION 6_3
      JsonObject willRelativesBeExtracted = section.getJsonObject(WILL_RELATIVES_BE_EXTRACTED);

      if (Objects.nonNull(willRelativesBeExtracted) && !Objects.equals(
        willRelativesBeExtracted.getString("key"),
        "a")) {
        section.remove(HAS_THE_GROUP_RELATIVES_FORMED_3);
        section.remove("previouslyIssuedPermitIssuer3");
        section.remove("previouslyIssuedPermitDate3");
        section.remove("previouslyIssuedPermitValidityPeriodFrom3");
        section.remove("previouslyIssuedPermitValidityPeriodTo3");
        section.remove("previouslyIssuedPermitNumber3");
        section.remove(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES);
        section.remove(VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS);
        section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES);
        section.remove(RELATIONSHIP_TO_STUDY_COHORT);
        section.remove("sizeOfRelativesGroup");
        section.remove("sizeOfRelativesGroupEstimateOrExact");
        section.remove(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY);
        section.remove(DATA_EXTRACTION_FREQUENCY_RELATIVES);
        section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES);
        section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
        section.remove(SPECIFY_OTHER_RELATIVES);
        section.remove("moreInfoOnExtractionPeriodsRelatives");
        section.remove("orderOfRelativesDataExtraction");
      } else {
        JsonObject hasTheGroupRelativesFormed3 = section.getJsonObject(
          HAS_THE_GROUP_RELATIVES_FORMED_3);

        if (Objects.nonNull(hasTheGroupRelativesFormed3) && !Objects.equals(
          hasTheGroupRelativesFormed3.getString("key"),
          "a")) {
          section.remove("previouslyIssuedPermitIssuer3");
          section.remove("previouslyIssuedPermitDate3");
          section.remove("previouslyIssuedPermitValidityPeriodFrom3");
          section.remove("previouslyIssuedPermitValidityPeriodTo3");
          section.remove("previouslyIssuedPermitNumber3");
        }

        JsonObject willSameDataBeExtractedForRelatives = section.getJsonObject(
          WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES);
        if (Objects.nonNull(willSameDataBeExtractedForRelatives) && !Objects.equals(
          willSameDataBeExtractedForRelatives.getString("key"),
          "b")) {
          section.remove(VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS);
          section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES);
        }

        JsonObject willDataForRelativesBeExtractedSimultaneously = section.getJsonObject(
          WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY);
        if (Objects.nonNull(willDataForRelativesBeExtractedSimultaneously) && !Objects.equals(
          willDataForRelativesBeExtractedSimultaneously.getString("key"),
          "b")) {
          section.remove(DATA_EXTRACTION_FREQUENCY_RELATIVES);
          section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES);
          section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
          section.remove(SPECIFY_OTHER_RELATIVES);
        } else {
          JsonObject dataExtractionFrequencyRelatives = section.getJsonObject(
            DATA_EXTRACTION_FREQUENCY_RELATIVES);
          if (Objects.nonNull(dataExtractionFrequencyRelatives) && !Objects.equals(
            dataExtractionFrequencyRelatives.getString("key"), "b")) {
            section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES);
            section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
            section.remove(SPECIFY_OTHER_RELATIVES);
          } else {
            JsonObject dataNeedsToBeExtractedEveryRelatives = section.getJsonObject(
              DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
            if (Objects.nonNull(dataNeedsToBeExtractedEveryRelatives) && !Objects.equals(
              dataNeedsToBeExtractedEveryRelatives.getString("key"), "d")) {
              section.remove(SPECIFY_OTHER_RELATIVES);
            }
          }
        }
      }
    }

    mongoApplicationForm.put(SECTION_6, section6Array);
  }


  private void cleanSection7(JsonObject mongoApplicationForm) {
    JsonObject section7 = mongoApplicationForm.getJsonObject(SECTION_7);
    if (Objects.isNull(section7)) {
      return;
    }

    JsonObject willDataBeCombinedWithDataObtained = section7.getJsonObject(
      WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);

    if (!Objects.equals(willDataBeCombinedWithDataObtained.getString("key"), "a")) {
      section7.remove("combinedCountries");
      section7.remove("combinedDataHolders");
      section7.remove("combinedDatabaseRegistries");
      section7.remove("combinedDatasetsRegistries");
      section7.remove("provideInformationList");
    }

    JsonObject otherPermitApplications = section7.getJsonObject(OTHER_PERMIT_APPLICATIONS);
    if (Objects.nonNull(otherPermitApplications) && !Objects.equals(
      otherPermitApplications.getString("key"), "a")) {
      section7.remove("dateOfSubmittingTheApplication");
      section7.remove("applicationIssuer");
      section7.remove("identificationCode");
    }

    mongoApplicationForm.put(SECTION_7, section7);
  }

  private void cleanSection8(JsonObject mongoApplicationForm) {
    JsonObject section2 = mongoApplicationForm.getJsonObject(SECTION_2);
    JsonObject section3 = mongoApplicationForm.getJsonObject(SECTION_3);
    JsonObject section5 = mongoApplicationForm.getJsonObject(SECTION_5);
    JsonObject section7 = mongoApplicationForm.getJsonObject(SECTION_7);
    JsonObject section8 = mongoApplicationForm.getJsonObject(SECTION_8);
    if (Objects.isNull(section8)) {
      return;
    }

    JsonObject whenDataNeeded = section8.getJsonObject(WHEN_DATA_NEEDED);
    if (Objects.nonNull(whenDataNeeded) && !Objects.equals(whenDataNeeded.getString("key"), "b")) {
      section8.remove("ifLaterWhen");
    }

    JsonObject applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
      APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    if ("b".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
      section8.remove("informationWhichDataCanAccess");
      section8.remove("frequencyOfAccessOrDataUpdates");
    } else if ("a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
      section8.remove("estimatedStartAndEnd");
      section8.remove("estimatedStartDatesForDataProcessing");
      section8.remove("estimatedEndDatesForDataProcessing");
    }

    JsonObject willTheDataBeTransferred = section8.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED);
    if (Objects.nonNull(willTheDataBeTransferred) && !Objects.equals(
      willTheDataBeTransferred.getString("key"), "a")) {
      section8.remove("whichCountriesDataBeProcessed");
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47);

      boolean isWhyWillDataBeTransferredOutsideEUArticle47 =
        section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47)
          && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47, false);
      if (!isWhyWillDataBeTransferredOutsideEUArticle47) {
        section8.remove("whyWillDataBeTransferredOutsideEUArticle47Options");
      }

      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48);

      boolean isWhyWillDataBeTransferredOutsideEUArticle48 =
        section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48)
          && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, false);

      if (!isWhyWillDataBeTransferredOutsideEUArticle48) {
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48a");
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B);

        boolean isWhyWillDataBeTransferredOutsideEUArticle48b =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48b) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
        }
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C);

        boolean isWhyWillDataBeTransferredOutsideEUArticle48C =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48C) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
        }
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48d");
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48e");
      } else {
        boolean isWhyWillDataBeTransferredOutsideEUArticle48b =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48b) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
        }

        boolean isWhyWillDataBeTransferredOutsideEUArticle48C =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48C) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
        }
      }
      section8.remove("whyWillDataBeTransferredOutsideEUArticle49");
      section8.remove(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);

      JsonObject legalBasisForTransferringTheDataOutsideEU = section8.getJsonObject(
        LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);

      if (Objects.nonNull(legalBasisForTransferringTheDataOutsideEU) && !Objects.equals(
        legalBasisForTransferringTheDataOutsideEU.getString("key"), "c")) {
        section8.remove("legalBasisForTransferringTheDataOutsideEUOtherOptions");
      }
      if (Objects.nonNull(legalBasisForTransferringTheDataOutsideEU) && !Objects.equals(
        legalBasisForTransferringTheDataOutsideEU.getString("key"), "b")) {
        section8.remove("safeguardsAreProvidedByReferringGDCP");
      }

      if ("a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        section8.remove(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE);
      } else if ("b".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        section8.remove(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER);
      }
    } else {
      boolean isWhyWillDataBeTransferredOutsideEUArticle47 =
        section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47)
          && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47, false);
      if (!isWhyWillDataBeTransferredOutsideEUArticle47) {
        section8.remove("whyWillDataBeTransferredOutsideEUArticle47Options");
      }

      boolean isWhyWillDataBeTransferredOutsideEUArticle48 =
        section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48)
          && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, false);

      if (!isWhyWillDataBeTransferredOutsideEUArticle48) {
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48a");
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B);

        boolean isWhyWillDataBeTransferredOutsideEUArticle48b =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48b) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
        }
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C);

        boolean isWhyWillDataBeTransferredOutsideEUArticle48C =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48C) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
        }
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48d");
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48e");
      } else {
        boolean isWhyWillDataBeTransferredOutsideEUArticle48b =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48b) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
        }

        boolean isWhyWillDataBeTransferredOutsideEUArticle48C =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
            && section8.getBoolean(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48C) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
        }
      }

      JsonObject legalBasisForTransferringTheDataOutsideEU = section8.getJsonObject(
        LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);

      if (Objects.nonNull(legalBasisForTransferringTheDataOutsideEU) && !Objects.equals(
        legalBasisForTransferringTheDataOutsideEU.getString("key"), "c")) {
        section8.remove("legalBasisForTransferringTheDataOutsideEUOtherOptions");
      }
      if (Objects.nonNull(legalBasisForTransferringTheDataOutsideEU) && !Objects.equals(
        legalBasisForTransferringTheDataOutsideEU.getString("key"), "b")) {
        section8.remove("safeguardsAreProvidedByReferringGDCP");
      }

      if ("a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        section8.remove(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE);
      } else if ("b".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        section8.remove(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER);
      }
    }

    if (!(this.displayLawfulnessOfProcessing(section8, section3, section5, section7)
      || displayLawfulForProcessingPersonalData(section3, section5))) {

      if (Boolean.TRUE.equals(displayLawfulForProcessingPersonalData(section3, section5))) {
        section8.remove("lawfulForProcessingPersonalData");
      }

      JsonArray legalBasisForProcessingPersonalData = section8.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
      if (Objects.nonNull(legalBasisForProcessingPersonalData) && (
        notContainsKey(legalBasisForProcessingPersonalData, "c") || !"b".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key")))) {
        section8.remove("otherLegalBasisForProcessingPersonalData");
      }

      if (!"b".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        section8.remove(LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
      }

      JsonObject willDataBeCombinedWithDataObtained = section7.getJsonObject(
        WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);

      if (!"a".equals(willDataBeCombinedWithDataObtained.getString("key")) || !"b".equals(
        applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        JsonArray legalBasisForProcessingCombinedData = section8.getJsonArray(
          LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA);
        if (Objects.nonNull(legalBasisForProcessingCombinedData) && notContainsKey(
          legalBasisForProcessingCombinedData, "b")) {
          section8.remove("otherLegalBasisForProcessingCombinedData");
        }
        section8.remove(LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA);
      }

      if (Objects.nonNull(section5)) {
        JsonObject formatOfTheElectronicHealthData = section5.getJsonObject(
          FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
        if (!"b".equals(formatOfTheElectronicHealthData.getString("key")) || (!"a".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key")))) {
          section8.remove("europeanUnionInstitution");
        }
      }

      if (!"a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        JsonArray legalBasisForProcessingApplicationData = section8.getJsonArray(
          LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA);
        if (Objects.nonNull(legalBasisForProcessingApplicationData) && notContainsKey(
          legalBasisForProcessingApplicationData, "b")) {
          section8.remove("otherLegalBasisForProcessingApplicationData");
        }
        section8.remove(LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA);
      }

      if (!"a".equals(willDataBeCombinedWithDataObtained.getString("key")) || !"a".equals(
        applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        JsonArray legalBasisForProcessingCombinedApplicationData = section8.getJsonArray(
          LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA);
        if (notContainsKey(legalBasisForProcessingCombinedApplicationData, "b")) {
          section8.remove("otherLegalBasisForProcessingCombinedApplicationData");
        }
        section8.remove(LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA);
      }
    }

    JsonArray purposeForWhichDataWillBeUsed = section2.getJsonArray(
      PURPOSE_FOR_WHICH_DATA_WILL_BE_USED);
    if (Objects.nonNull(purposeForWhichDataWillBeUsed) && notContainsKey(
      purposeForWhichDataWillBeUsed, "e")) {
      section8.remove("attachResearchPermitIfRequired");
    }

    mongoApplicationForm.put(SECTION_8, section8);
  }

  private boolean containsKey(JsonArray jsonArray, String keyToCheck) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");
      if (keyToCheck.equals(key)) {
        return true;
      }
    }
    return false;
  }

  private Boolean displayLawfulnessOfProcessing(JsonObject section, JsonObject section3,
    JsonObject section5, JsonObject section7) {
    JsonObject applyingForDataOnBehalfOfPublicSector = null;
    JsonArray legalBasisForProcessingPersonalData = null;
    JsonObject willDataBeCombinedWithDataObtained = null;
    JsonArray legalBasisForProcessingCombinedData = null;
    JsonObject formatOfTheElectronicHealthData = null;
    JsonArray legalBasisForProcessingApplicationData = null;
    JsonArray legalBasisForProcessingCombinedApplicationData = null;

    if (Objects.nonNull(section)) {
      legalBasisForProcessingPersonalData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
      legalBasisForProcessingCombinedData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA);

      legalBasisForProcessingApplicationData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA);
      legalBasisForProcessingCombinedApplicationData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA);
    }
    if (Objects.nonNull(section3)) {
      applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
        APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    }
    if (Objects.nonNull(section5)) {
      formatOfTheElectronicHealthData = section5.getJsonObject(
        FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    }
    if (Objects.nonNull(section7)) {
      willDataBeCombinedWithDataObtained = section7.getJsonObject(
        WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);
    }

    return ((Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
      applyingForDataOnBehalfOfPublicSector.getString("key")) || (
      Objects.nonNull(legalBasisForProcessingPersonalData) && containsKey(
        legalBasisForProcessingPersonalData, "b"))) || (
      Objects.nonNull(willDataBeCombinedWithDataObtained) && "a".equals(
        willDataBeCombinedWithDataObtained.getString("key")) && (
        Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key"))) || (
        Objects.nonNull(legalBasisForProcessingCombinedData) && containsKey(
          legalBasisForProcessingCombinedData, "c")) || (
        Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key")) && (
          Objects.nonNull(formatOfTheElectronicHealthData) && "a".equals(
            formatOfTheElectronicHealthData.getString("key"))) || (
          (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
            applyingForDataOnBehalfOfPublicSector.getString("key"))) || (
            Objects.nonNull(legalBasisForProcessingApplicationData) && containsKey(
              legalBasisForProcessingApplicationData, "b")) || (
            Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
              applyingForDataOnBehalfOfPublicSector.getString("key")) && (
              Objects.nonNull(willDataBeCombinedWithDataObtained) && "a".equals(
                willDataBeCombinedWithDataObtained.getString("key"))) || (
              Objects.nonNull(legalBasisForProcessingCombinedApplicationData) && containsKey(
                legalBasisForProcessingCombinedApplicationData, "b")))))));
  }

  private Boolean displayLawfulForProcessingPersonalData(JsonObject section3, JsonObject section5) {
    JsonObject applyingForDataOnBehalfOfPublicSector = null;
    JsonObject formatOfTheElectronicHealthData = null;
    JsonObject legalOrNaturalPerson = null;

    if (Objects.nonNull(section3)) {
      applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
        APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
      legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    }
    if (Objects.nonNull(section5)) {
      formatOfTheElectronicHealthData = section5.getJsonObject(
        FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    }

    return ((Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
      applyingForDataOnBehalfOfPublicSector.getString("key")) || (
      Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
        legalOrNaturalPerson.getString("key")))) && (
      Objects.nonNull(formatOfTheElectronicHealthData) && "b".equals(
        formatOfTheElectronicHealthData.getString("key"))));
  }

  Future<JsonObject> replaceApplication(JsonObject mongoApplication) {
    return replaceApplication(mongoApplication, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  /**
   * Get all values from an optional field, from a specific Json node
   */
  public static void extractField(JsonNode node, Set<String> ids, String field) {
    if (node.isObject()) {
      processObjectNode(node, ids, field);
    } else if (node.isArray()) {
      processArrayNode(node, ids, field);
    }
  }

  private static void processObjectNode(JsonNode node, Set<String> ids, String field) {
    Iterator<String> fieldNames = node.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode childNode = node.get(fieldName);
      if (fieldName.equals(field)) {
        collectIdsFromNode(childNode, ids);
      } else {
        extractField(childNode, ids, field);
      }
    }
  }

  private static void processArrayNode(JsonNode node, Set<String> ids, String field) {
    for (JsonNode arrayItem : node) {
      extractField(arrayItem, ids, field);
    }
  }

  private static void collectIdsFromNode(JsonNode node, Set<String> ids) {
    if (node.isTextual()) {
      ids.add(node.asText());
    } else if (node.isArray()) {
      for (JsonNode idNode : node) {
        if (idNode.isTextual()) {
          ids.add(idNode.asText());
        }
      }
    }
  }

  public void postData(@NotNull RoutingContext context) {
    postData(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getData(@NotNull RoutingContext context) {
    getData(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void deleteData(@NotNull RoutingContext context) {
    deleteData(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void validateFileBeforeUpdate(@NotNull RoutingContext context) {
    validateFileBeforeUpdate(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void changeTitle(@NotNull RoutingContext context) {
    changeTitle(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getDataAccessApplicationsHbad(@NotNull RoutingContext context) {
    getApplicationsHbad(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getDataAccessApplicationsCountries(@NotNull RoutingContext context) {
    getApplicationsCountries(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getDataAccessApplicationTitles(@NotNull RoutingContext context) {
    getApplicationTitles(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void processTranslationCallback(String applicationId, JsonObject dict) {
    JsonObject query = new JsonObject().put(ID, applicationId);

    mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, query, null, res -> {
      if (res.succeeded() && res.result() != null) {
        JsonObject application = res.result();
        JsonObject originalForm = application.getJsonObject("form");
        JsonObject translatedFiles = application.getJsonObject(TRANSLATION, new JsonObject())
          .getJsonObject("files", new JsonObject());
        JsonObject formTranslations = application.getJsonObject("form_translations",
          new JsonObject());
        List<String> targetLangs = application.getJsonObject(TRANSLATION, new JsonObject())
          .getJsonArray("targetLanguages", new JsonArray())
          .stream().map(Object::toString).toList();

        JsonObject translationUpdate = super.buildFormTranslationsAndSetStatus(
          originalForm, dict, targetLangs, applicationId, translatedFiles,
          DATA_ACCESS_APPLICATION_COLLECTION, formTranslations
        );

        applyTranslationUpdate(application, translationUpdate);

        TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
          application.getJsonObject(TRANSLATION, new JsonObject())
        );
        application.getJsonObject(TRANSLATION).put(STATUS, overallStatus);

        if (TranslationStatus.COMPLETED.equals(overallStatus)) {
          application.getJsonObject(TRANSLATION).remove("errorType");
        }

        replaceApplication(application)
          .onSuccess(replaceResult -> {
            log.info("Application updated with translations. Proceeding to dispatch.");
          }).onFailure(err -> log.error("Failed to replace application with translations", err));

      } else {
        log.error("Could not find application to apply translations: {}", applicationId);
      }
    });
  }

  public void processFileTranslationCallback(String applicationId, String originalFileId,
    Map<String, JsonObject> uploadedFiles) {
    JsonObject query = new JsonObject().put(ID, applicationId);

    mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, query, null, res -> {
      if (res.succeeded() && res.result() != null) {
        JsonObject application = res.result();

        boolean fileIdExists = Optional.ofNullable(application)
          .map(m -> m.getJsonObject("translation"))
          .map(t -> t.getJsonObject("files"))
          .map(f -> f.containsKey(originalFileId))
          .orElse(false);
        if (!fileIdExists) {
          return;
        }

        applyFileTranslationUpdates(application, applicationId, originalFileId, uploadedFiles);

        TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
          application.getJsonObject(TRANSLATION, new JsonObject())
        );
        application.getJsonObject(TRANSLATION).put(STATUS, overallStatus);

        if (TranslationStatus.FILES_COMPLETED.equals(overallStatus)) {
          application.getJsonObject(TRANSLATION).remove("errorType");
        }
        log.info("File Translation completed for application id: {} and file id : {}",
          applicationId, originalFileId);

        replaceApplication(application)
          .onSuccess(replaceResult -> {
            log.info("Application [{}] updated with translated file references for [{}]",
              applicationId, originalFileId);
          }).onFailure(
            err -> log.error("Failed to replace application with file translation updates", err));

      } else {
        log.error("Could not find application [{}] to update file translations", applicationId);
      }
    });
  }

  public void markTranslationError(String applicationId, String errorType, String fileId) {
    super.markTranslationError(applicationId, DATA_ACCESS_APPLICATION_COLLECTION, errorType,
      fileId);
  }

  public void incrementTranslationAttempts(String applicationId, String errorType, String fileId) {
    super.incrementTranslationAttempts(
      applicationId,
      DATA_ACCESS_APPLICATION_COLLECTION,
      errorType,
      fileId
    );
  }

}
