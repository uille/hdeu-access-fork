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
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.DATE_SUBMITTED;
import static eu.europa.data.health.access.Constants.DESC;
import static eu.europa.data.health.access.Constants.FORM;
import static eu.europa.data.health.access.Constants.FORM_TRANSLATIONS;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.OPTIONS;
import static eu.europa.data.health.access.Constants.PENDING_FILE_TRANSLATIONS;
import static eu.europa.data.health.access.Constants.PROVIDED_JUSTIFICATION_EXCEPTION;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.REGEX;
import static eu.europa.data.health.access.Constants.SECTION;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_NUMBER;
import static eu.europa.data.health.access.Constants.SENDING;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.SUPPORTED_LANGUAGES;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;

import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.OperationStatus;
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

public class DataRequestApplicationManager extends CommonApplicationManager {

  private final MongoClient mongoClient;

  private DataRequestApplicationValidationManager dataRequestApplicationValidationManager;

  private DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager;

  private final AuditLogManager auditLogManager;

  private final NotificationManager notificationManager;

  /**
   * Section lock manager used to enforce write-time lock checks.
   * Injected after construction via {@link #setSectionLockManager(SectionLockManager)}.
   */
  private SectionLockManager sectionLockManager;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String dataRequestManagerClassName = this.getClass().getSimpleName() + " - ";

  /**
   * Data request application constructor
   */
  public DataRequestApplicationManager(DBHandler dbHandler, MongoClient mongoClient,
    BasketManager basketManager, HubStoreClient hubStoreClient, Vertx vertx,
    DataRequestApplicationValidationManager dataRequestApplicationValidationManager,
    DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager,
    AuditLogManager auditLogManager, NotificationManager notificationManager, HubTranslationClient hubTranslationClient,
    HubSearchClient hubSearchClient, AuthClient authClient) {
    super(dbHandler, basketManager, mongoClient, vertx, hubStoreClient, auditLogManager, notificationManager,
      hubTranslationClient, hubSearchClient, authClient);
    this.mongoClient = mongoClient;
    this.dataRequestApplicationValidationManager = dataRequestApplicationValidationManager;
    this.dataRequestApplicationBusinessValidationManager = dataRequestApplicationBusinessValidationManager;
    this.auditLogManager = auditLogManager;
    this.notificationManager = notificationManager;
  }

  /**
   * Injects the {@link SectionLockManager} for write-time lock enforcement.
   * Called from {@code MainVerticle} after construction.
   *
   * @param sectionLockManager the section lock manager instance
   */
  public void setSectionLockManager(SectionLockManager sectionLockManager) {
    this.sectionLockManager = sectionLockManager;
  }

  /**
   * Fetches a Data Request Application by ID.
   *
   * <p>Unlike the default {@code findById} (which restricts to owner only),
   * this method also allows access for <strong>accepted collaborators</strong>
   * so that the application form loads correctly after a collaboration invite is accepted.
   */
  public void getApplicationById(RoutingContext context) {
    findByIdForUserOrCollaborator(context,
        DATA_REQUEST_APPLICATION_COLLECTION,
        Constants.DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void getEveryDataRequestApplicationById(RoutingContext context) {
    super.findByIdWithoutSecurity(context, DATA_REQUEST_APPLICATION_COLLECTION);
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
    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    Integer finalPage = pageSize;
    Integer finalPageLimit = pageLimit;

    getQueryOptionsApplication(context).onSuccess(queryOptions -> {
      log.debug("{}Retrieved query options ", dataRequestManagerClassName);
      super.getEntities(
        context,
        DATA_REQUEST_APPLICATION_COLLECTION,
        finalPage,
        finalPageLimit,
        StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
        StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : DESC,
        queryOptions,
        buildApplicationListProjection()
      );
    }).onFailure(err -> {
      log.error("{}Error building query options, will proceed without them",
        dataRequestManagerClassName);
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Could not construct query options");
    });
  }

  public void getAllDecisionIssuedDataRequestApplications(@NotNull RoutingContext context) {
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
      dataRequestManagerClassName, queryOptions.encode());

    super.getDecisionIssuedDatasets(
      context,
      DATA_REQUEST_APPLICATION_COLLECTION,
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
      dataRequestManagerClassName, queryOptions.encode());

    super.getDataPermitGrantedDatasets(context, DATA_REQUEST_APPLICATION_COLLECTION, finalPage,
      finalPageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc", queryOptions);
  }

  public void getAllOnGoingDataRequestApplications(@NotNull RoutingContext context) {
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
    queryOptions.put(STATUS_STATUS,
      new JsonObject().put("$in",
        new JsonArray().add("ON-GOING").add("COMPLETED")
      )
    );

    log.debug("{}Using query options for ongoing applications: {}",
      dataRequestManagerClassName, queryOptions.encode());

    // Projection: only the fields consumed by the registry "Submitted" list view
    JsonObject projection = new JsonObject()
      .put(ID, 1)
      .put(TITLE, 1)
      .put(STATUS, 1)
      .put(DATE_SUBMITTED, 1)
      .put("applicantDetails.firstName", 1)
      .put("applicantDetails.lastName", 1)
      .put(FORM + String.join(".", SECTION_2, PURPOSE_FOR_WHICH_DATA_WILL_BE_USED), 1)
      .put("form.section2.summaryOfTheProject", 1);

    for (String lang : SUPPORTED_LANGUAGES) {
      projection
        .put(
          String.join(".", FORM_TRANSLATIONS, lang, SECTION_2, PURPOSE_FOR_WHICH_DATA_WILL_BE_USED),
          1
        )
        .put(String.join(".", FORM_TRANSLATIONS, lang, SECTION_2, "summaryOfTheProject"), 1);
    }

    super.getEntities(
      context,
      DATA_REQUEST_APPLICATION_COLLECTION,
      finalPage,
      finalPageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc",
      queryOptions,
      projection
    );
  }

  /**
   * Get all Documents for the Data Access Application collection.
   */
  public void getAllDataRequestDatasets(@NotNull RoutingContext context) {
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
      log.debug("{}Retrieved query options ", dataRequestManagerClassName);
      super.getDatasets(context, DATA_REQUEST_APPLICATION_COLLECTION, finalPage, finalPageLimit,
        StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
        StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : "desc", queryOptions,
        getHubStoreClient());
    }).onFailure(err -> {
      log.error("{}Error building query options, will proceed without them",
        dataRequestManagerClassName);
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Could not construct query options");
    });
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

  public void getDataRequestApplicationsHbad(@NotNull RoutingContext context) {
    getApplicationsHbad(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void getDataRequestApplicationsCountries(@NotNull RoutingContext context) {
    getApplicationsCountries(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void getDataRequestApplicationTitles(@NotNull RoutingContext context) {
    getApplicationTitles(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void putDistributionsApplication(@NotNull RoutingContext context) {
    putDistributionsApplication(context, DATA_REQUEST_APPLICATION_COLLECTION);
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

  /**
   * Creates or updates a section of a Data Request Application.
   *
   * <p><strong>Lock enforcement:</strong> If a {@link SectionLockManager} is configured,
   * verifies that the caller holds a valid section lock before persisting changes.
   * Returns {@code 409 Conflict} if the lock is not held or has expired.
   *
   * @param context the Vert.x routing context
   */
  public void createOrUpdateSection(@NotNull RoutingContext context) {
    if (sectionLockManager != null) {
      String applicationId = context.pathParam(APPLICATION_ID);
      String sectionId     = SECTION + context.pathParam(SECTION_NUMBER);
      String ecasId        = Context.getEcasId(context);
      // Read the per-tab session ID sent by the frontend in the X-Session-Id header.
      // This must match the sessionId used when acquiring the lock.
      String sessionId     = context.request().getHeader("X-Session-Id");

      sectionLockManager.assertLockHeld(
          Constants.DATA_REQUEST_APPLICATION_COLLECTION, applicationId, sectionId, sessionId)
        .onSuccess(v ->
          createOrUpdateSection(context, DATA_REQUEST_APPLICATION_COLLECTION))
        .onFailure(err -> {
          log.warn("[DRA] Write rejected — lock not held: applicationId='{}' section='{}' user='{}': {}",
            applicationId, sectionId, ecasId, err.getMessage());
          setUpMessageContextResponse(context,
            io.netty.handler.codec.http.HttpResponseStatus.CONFLICT,
            err.getMessage());
        });
    } else {
      createOrUpdateSection(context, DATA_REQUEST_APPLICATION_COLLECTION);
    }
  }

  public void getSection(@NotNull RoutingContext context) {
    String applicationId = context.pathParam(APPLICATION_ID);
    String ecasId        = Context.getEcasId(context);
    String sectionNumber = SECTION + context.pathParam(SECTION_NUMBER);

    JsonObject sectionToRetrieve = new JsonObject().put(FORM + sectionNumber, 1);
    FindOptions options = new FindOptions().setFields(sectionToRetrieve);

    // Allow both owner and accepted collaborators to read sections
    buildUserAccessFilter(ecasId, Constants.DATA_REQUEST_APPLICATION_COLLECTION)
      .compose(accessFilter -> {
        JsonObject query = new JsonObject()
            .put(ID, applicationId)
            .mergeIn(accessFilter);
        return mongoClient.findWithOptions(DATA_REQUEST_APPLICATION_COLLECTION, query, options);
      })
      .onSuccess(ar -> {
        if (ar != null && !ar.isEmpty() && ar.get(0) != null
          && ar.get(0).getJsonObject("form") != null && !ar.get(0).getJsonObject("form").isEmpty()) {
          Object mongoSection = ar.get(0).getJsonObject("form").getValue(sectionNumber);
          setUpContextResponse(context, HttpResponseStatus.OK, mongoSection.toString());
        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            "No information found for provided input");
        }
      })
      .onFailure(err -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, err.getMessage()));
  }

  public void changeCatalogStatus(@NotNull RoutingContext context) {
    changeCatalogStatus(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void changeDatasetStatus(@NotNull RoutingContext context) {
    changeDatasetStatus(context, DATA_REQUEST_APPLICATION_COLLECTION);
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

      mongoClient.findOneAndUpdate(DATA_REQUEST_APPLICATION_COLLECTION, query, newStatus,
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

      mongoClient.findOneAndUpdate(DATA_REQUEST_APPLICATION_COLLECTION, query, translatingStatus,
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
                  mongoApplication.getJsonObject("form"), DATA_REQUEST_APPLICATION_COLLECTION,
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
                  DATA_REQUEST_APPLICATION_COLLECTION,
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
            .put("status.status", ApplicationStatus.ON_GOING.getStatus())
            .put("date_submitted", new JsonObject().put("$exists", true)))
        );

      JsonObject submittingStatus = new JsonObject().put("$set",
        new JsonObject().put("status.status", SENDING));

      mongoClient.findOneAndUpdate(DATA_REQUEST_APPLICATION_COLLECTION, query, submittingStatus,
        updateResult -> {
          if (updateResult.succeeded()) {
            if (updateResult.result() != null) {

              JsonObject mongoApplication = updateResult.result();

              if (mongoApplication.containsKey("date_submitted")
                && hasAtLeastOneAdditionalInformationAsLatest(
                mongoApplication.getJsonArray("catalogs"))) {

                JsonObject status = new JsonObject().put("status",
                  ApplicationStatus.ON_GOING.getStatus());
                //validate status change
                if (!canChangeStatus(status, mongoApplication, context)) {
                  rollbackStatus(applicationId, ON_GOING, DATA_REQUEST_APPLICATION_COLLECTION);
                  return;
                }

                super.updateApplicationStatus(mongoApplication, status, context);
                performCleanUpForm(mongoApplication);
                updateCatalogStatus(mongoApplication);
                int currentVersion = mongoApplication.getInteger("version") + 1;
                mongoApplication.put("version", currentVersion);
                mongoApplication.put("date_submitted", Instant.now());

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

                replaceApplication(mongoApplication).onSuccess(replaceResult -> {

                  // If status is submitted then we need to add the application to the dispatcher table
                  ApplicationStatus requestStatus = ApplicationStatus.fromStatus(
                    mongoApplication.getJsonObject(STATUS).getString(STATUS));
                  if (ApplicationStatus.ON_GOING.equals(requestStatus)) {

                    sendApplicationForDispatch(mongoApplication,
                      DATA_REQUEST_APPLICATION_COLLECTION, OperationStatus.UPDATE);

                    // trigger extra translations (all eu languages, for registry page)
                    triggerRemainingTranslations(applicationId, mongoApplication, DATA_REQUEST_APPLICATION_COLLECTION);

                    removeAdditionalInformationFromDataRequestCollection(applicationId)
                      .onFailure(err -> log.warn("Failed to unset additional_information for {}: {}", applicationId, err.getMessage()));

                    //Add Notification
                    addNotificationsForMultipleCatalogsPerApplication(NotificationCode.APPLICATION_RESUBMIT, mongoApplication, DATA_REQUEST_APPLICATION_COLLECTION);

                    //Audit Log
                    String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                      AuditLogActionDescription.DATA_REQUEST_APPLICATION_RESUBMITTED.toString(),
                      applicationId, mongoApplication.getString(TITLE));
                    auditLogManager.insertAuditLog(
                      AuditLogObject.DATA_REQUEST_APPLICATION.toString(),
                      mongoApplication.getString(ID),
                      AuditLogAction.DATA_REQUEST_APPLICATION_SUBMITTED.toString(),
                      auditLogActionDescription, true, context);
                  }
                  setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
                }).onFailure(fail -> {
                  rollbackStatus(applicationId, ON_GOING, DATA_REQUEST_APPLICATION_COLLECTION);
                  setUpMessageContextResponse(context,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage());
                });

              } else {
                JsonObject status = new JsonObject().put("status",
                  ApplicationStatus.ON_GOING.getStatus());

                //validate status change
                if (!canChangeStatus(status, mongoApplication, context)) {
                  rollbackStatus(applicationId, DRAFT, DATA_REQUEST_APPLICATION_COLLECTION);
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
                          DATA_REQUEST_APPLICATION_COLLECTION, OperationStatus.CREATE);

                        // trigger extra translations (all eu languages, for registry page)
                       triggerRemainingTranslations(applicationId, mongoApplication, DATA_REQUEST_APPLICATION_COLLECTION);

                        removeAdditionalInformationFromDataRequestCollection(applicationId)
                          .onFailure(err -> log.warn("Failed to unset additional_information for {}: {}", applicationId, err.getMessage()));

                        //Add Notification
                        addNotificationsForMultipleCatalogsPerApplication(NotificationCode.APPLICATION_SUBMIT, mongoApplication, DATA_REQUEST_APPLICATION_COLLECTION);

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
                      rollbackStatus(applicationId, DRAFT, DATA_REQUEST_APPLICATION_COLLECTION);
                      setUpMessageContextResponse(context,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage());
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

  private Future<Void> removeAdditionalInformationFromDataRequestCollection(String applicationId) {
    Promise<Void> p = Promise.promise();

    JsonObject query = new JsonObject().put("_id", applicationId);

    JsonObject update = new JsonObject().put("$unset", new JsonObject().put("additionalInformation", ""));

    mongoClient.findOneAndUpdate(DATA_REQUEST_APPLICATION_COLLECTION, query, update, ar -> {
      if (ar.succeeded()) p.complete();
      else p.fail(ar.cause());
    });

    return p.future();
  }

  private Future<JsonObject> getQueryOptionsDataset(@NotNull RoutingContext context) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject searchFields = context.body().asJsonObject();
    if (searchFields == null) {
      searchFields = new JsonObject(); // <-- avoid NPE
    }

    String title = searchFields.getString("title");
    JsonArray datasetStatusList = searchFields.getJsonArray("datasetStatus");
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

  /**
   * Builds the MongoDB query options for the "My Data Request Applications" list endpoint.
   *
   * <p>The user-access filter is an {@code $or} that covers two cases:
   * <ol>
   *   <li>The user is the <strong>owner</strong> ({@code userId = ecasId}).</li>
   *   <li>The user is an <strong>accepted collaborator</strong> on the application
   *       (looked up from {@code APPLICATION_COLLABORATOR} collection).</li>
   * </ol>
   *
   * <p>This ensures that after a collaborator accepts an invitation, the shared
   * application appears in their "My Data Request Applications" list.
   *
   * @param context the Vert.x routing context (provides auth + filter params)
   * @return a {@link Future} resolving to the complete Mongo query {@link JsonObject}
   */
  private Future<JsonObject> getQueryOptionsApplication(@NotNull RoutingContext context) {
    JsonObject searchFields = context.body().asJsonObject();
    if (searchFields == null) searchFields = new JsonObject();

    String title               = searchFields.getString("title");
    JsonArray statusList       = searchFields.getJsonArray("status");
    JsonArray hdabList         = searchFields.getJsonArray("hdab");
    JsonArray geographicalList = searchFields.getJsonArray("geographicalList");
    JsonArray roleList = searchFields.getJsonArray("role");

    final String titleFinal           = title;
    final JsonArray statusListFinal   = statusList;
    final JsonArray hdabListFinal     = hdabList;
    final JsonArray geoListFinal      = geographicalList;

    String userId = Context.getEcasId(context);

    return buildUserAccessFilter(userId, Constants.DATA_REQUEST_APPLICATION_COLLECTION, roleList)
        .map(userAccessFilter -> {
          JsonObject query = new JsonObject();

          if (titleFinal != null && !titleFinal.isBlank()) {
            query.put("title", new JsonObject().put(REGEX, titleFinal).put(OPTIONS, "i"));
          }
          if (statusListFinal != null && !statusListFinal.isEmpty()) {
            query.put("status.status", new JsonObject().put("$in", statusListFinal));
          }
          if (hdabListFinal != null && !hdabListFinal.isEmpty()) {
            query.put("datasets.hdab.name", new JsonObject().put("$in", hdabListFinal));
          }
          if (geoListFinal != null && !geoListFinal.isEmpty()) {
            query.put("datasets.country.label", new JsonObject().put("$in", geoListFinal));
          }

          if (StringUtils.isNotEmpty(userId)) {
            query.mergeIn(userAccessFilter);
          }

          return query;
        });
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
    if (!ApplicationStatus.isValidStatus(statusStr)) {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN, "Status cannot be "
        + "updated: wrong status value");
      return false;
    }

    ApplicationStatus requestStatus = ApplicationStatus.fromStatus(statusStr);

      if (ON_GOING.equals(requestStatus)) {
        if(performDTOValidation(mongoApplication, context)){
          if(performBusinessValidation(mongoApplication.getJsonObject("form"), context)){
            //TODO: we need to add here something like: return dataAccessApplicationBusinessValidationManager.validateCatalogs(mongoApplication, context);
            return true;
          }
        }

    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.FORBIDDEN, "Status cannot be "
        + "updated: wrong flow");
    }

    return false;
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
        JsonObject formTranslations = application.getJsonObject("form_translations",
          new JsonObject());
        List<String> targetLangs = application.getJsonObject(TRANSLATION, new JsonObject())
          .getJsonArray("targetLanguages", new JsonArray())
          .stream().map(Object::toString).toList();

        JsonObject translationUpdate = super.buildFormTranslationsAndSetStatus(
          originalForm, dict, targetLangs, applicationId, translatedFiles,
          DATA_REQUEST_APPLICATION_COLLECTION, formTranslations
        );
        applyTranslationUpdate(application, translationUpdate);

        TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
          application.getJsonObject(TRANSLATION, new JsonObject())
        );
        application.getJsonObject(TRANSLATION).put(STATUS, overallStatus);

        if (TranslationStatus.COMPLETED.equals(overallStatus)) {
          application.getJsonObject(TRANSLATION).remove("errorType");
        }

        boolean pendingFileTranslations = application
          .getJsonObject(TRANSLATION, new JsonObject())
          .getBoolean(PENDING_FILE_TRANSLATIONS, false);

        // Clear the flag — form translation is the final step of the remaining-translations flow.
        if (pendingFileTranslations) {
          application.getJsonObject(TRANSLATION).put(PENDING_FILE_TRANSLATIONS, false);
        }

        replaceApplication(application)
          .onSuccess(replaceResult -> {
            log.info("Application updated with translations. Proceeding to dispatch.");
          })
          .onFailure(err -> log.error("Failed to replace application with translations", err));

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

        boolean fileIdExists = Optional.ofNullable(application)
          .map(m -> m.getJsonObject("translation")).map(t -> t.getJsonObject("files"))
          .map(f -> f.containsKey(originalFileId)).orElse(false);
        if (!fileIdExists) {
          return;
        }

        applyFileTranslationUpdates(application, applicationId, originalFileId, uploadedFiles);

        TranslationStatus overallStatus = ApplicationFormUtils.determineOverallTranslationStatus(
          application.getJsonObject(TRANSLATION, new JsonObject()));
        application.getJsonObject(TRANSLATION).put(STATUS, overallStatus);

        if (TranslationStatus.FILES_COMPLETED.equals(overallStatus)) {
          application.getJsonObject(TRANSLATION).remove("errorType");
        }

        boolean pendingFileTranslations = application
          .getJsonObject(TRANSLATION, new JsonObject())
          .getBoolean(PENDING_FILE_TRANSLATIONS, false);

        log.info("File Translation completed for application id: {} and file id : {}",
          applicationId, originalFileId);

        replaceApplication(application)
          .onSuccess(replaceResult -> {
            log.info("Application [{}] updated with translated file references for [{}]",
              applicationId, originalFileId);
            // When all files are done, and we are in the remaining-translations flow,
            // trigger form translations now that translated files are available.
            if (
              pendingFileTranslations &&
                (TranslationStatus.FILES_COMPLETED.equals(overallStatus) ||
                  TranslationStatus.COMPLETED.equals(overallStatus))
            ) {
              log.info(
                "All file translations complete for {} (pendingFileTranslations=true); triggering remaining form translations.",
                applicationId
              );
              triggerRemainingFormTranslations(
                applicationId,
                application,
                DATA_REQUEST_APPLICATION_COLLECTION
              );
            }
          }).onFailure(
            err -> log.error("Failed to replace application with file translation updates", err));

      } else {
        log.error("Could not find application [{}] to update file translations", applicationId);
      }
    });
  }

  public void markTranslationError(String applicationId, String errorType, String fileId) {
    super.markTranslationError(applicationId, DATA_REQUEST_APPLICATION_COLLECTION, errorType,
      fileId);
  }

  public void incrementTranslationAttempts(String applicationId, String errorType, String fileId) {
    super.incrementTranslationAttempts(
      applicationId,
      DATA_REQUEST_APPLICATION_COLLECTION,
      errorType,
      fileId
    );
  }
}
