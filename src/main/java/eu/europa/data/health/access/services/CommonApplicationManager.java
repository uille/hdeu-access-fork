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
import static eu.europa.data.health.access.Constants.APPLICATION_JSON;
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
import static eu.europa.data.health.access.Constants.DATE_DEADLINE;
import static eu.europa.data.health.access.Constants.DATE_DISPATCHED;
import static eu.europa.data.health.access.Constants.DATE_EXTENSION_DEADLINE;
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
import static eu.europa.data.health.access.Constants.REJECTION_FILE;
import static eu.europa.data.health.access.Constants.REJECTION_REASON;
import static eu.europa.data.health.access.Constants.RENEW_AMEND_FILE;
import static eu.europa.data.health.access.Constants.REVOCATION_FILE;
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
import static eu.europa.data.health.access.Constants.SUPPORTED_LANGUAGES_UPPER;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.VARIABLES;
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
import eu.europa.data.health.access.NotificationCode;
import eu.europa.data.health.access.NotificationKeys;
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
import io.vertx.ext.mongo.FindOptions;
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
import java.util.HashMap;
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
  private static final String CATALOG_LANGUAGES = "catalog_languages";
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
  private static final String TRANSLATION_FILES = "translation.files.";
  private static final String TRANSLATION_ERROR_TYPE = "translation.errorType";
  private static final String TRANSLATION_STATUS = "translation.status";
  public static final String TRANSLATION_PENDING_FILE_TRANSLATIONS = "translation.pendingFileTranslations";
  public static final String DATASETS_HDAB_NAME = "datasets.hdab.name";
  public static final String DATASETS_COUNTRY_LABEL = "datasets.country.label";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String KEY = "key";
  private static final String VALUE = "value";

  /**
   * @param selectedPathInApplication e.g. "form.section2.purposeForWhichDataWillBeUsed"
   * @param itemsPathInTranslations   e.g. "section2.purposeForWhichDataWillBeUsed.items"
   */
  private record KeyValueRemapSpec(String selectedPathInApplication,
                                   String itemsPathInTranslations) {

  }

  private static final List<String> ERROR_CODES_REQUIRED = Arrays.asList(
    "NotNull",
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

  private final BasketManager basketManager;
  private final MongoClient mongoClient;
  private final Vertx vertx;
  private final HubStoreClient hubStoreClient;
  private final AuditLogManager auditLogManager;
  private final NotificationManager notificationManager;
  private final HubTranslationClient hubTranslationClient;
  private final HubSearchClient hubSearchClient;
  private final AuthClient authClient;
  private final JsonObject config;
  private DecisionTranslationService decisionTranslationService;

  /**
   * WebSocket manager for broadcasting real-time events to connected clients.
   * Injected after construction via {@link #setLockWebSocketManager(LockWebSocketManager)}.
   * Used to broadcast {@code section_saved} events when a section is successfully persisted.
   */
  private LockWebSocketManager lockWebSocketManager;

  /**
   * Exposes the shared MongoClient to subclasses.
   * Used by {@link #buildUserAccessFilter} so that both
   * {@code DataAccessApplicationManager} and {@code DataRequestApplicationManager}
   * can reuse the collaborator-lookup logic without duplicating it.
   */
  protected MongoClient getMongoClient() {
    return mongoClient;
  }

  public void setDecisionTranslationService(DecisionTranslationService decisionTranslationService) {
    this.decisionTranslationService = decisionTranslationService;
  }

  protected DecisionTranslationService getDecisionTranslationService() {
    return decisionTranslationService;
  }

  protected HubStoreClient getHubStoreClient() {
    return hubStoreClient;
  }

  /**
   * Builds a MongoDB {@code $or} filter that matches applications visible to the given user:
   * <ol>
   *   <li>Applications owned by the user ({@code userId = ecasId}), AND</li>
   *   <li>Applications where the user is an <em>ACCEPTED</em> collaborator
   *       for the given {@code applicationType}.</li>
   * </ol>
   *
   * <p>This is the central place that enforces the "collaborators see shared apps in the list"
   * business rule.  Both {@code DataAccessApplicationManager.getQueryOptionsApplication} and
   * {@code DataRequestApplicationManager.getQueryOptionsApplication} delegate here so the logic
   * stays in one place and is easy to maintain.
   *
   * @param ecasId          the current user's ECAS ID (preferred_username)
   * @param applicationType the application type constant
   *                        ({@code DATA_ACCESS_APPLICATION} or {@code DATA_REQUEST_APPLICATION})
   * @return a {@code Future} that resolves to a {@code JsonObject} containing the
   *         {@code $or} clause.  The clause always includes the owner condition; the
   *         collaborator condition is added only when accepted collaborator entries exist.
   */
  protected Future<JsonObject> buildUserAccessFilter(String ecasId, String applicationType) {
    return buildUserAccessFilter(ecasId, applicationType, null);
  }

  protected Future<JsonObject> buildUserAccessFilter(String ecasId, String applicationType, JsonArray roleList) {
    LinkedHashSet<String> normalizedRoles = new LinkedHashSet<>();
    if (roleList != null) {
      roleList.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(StringUtils::trimToEmpty)
        .map(StringUtils::upperCase)
        .filter(role -> Constants.ROLE_OWNER.equals(role) || Constants.ROLE_EDITOR.equals(role))
        .forEach(normalizedRoles::add);
    }

    if (normalizedRoles.isEmpty()) {
      normalizedRoles.add(Constants.ROLE_OWNER);
      normalizedRoles.add(Constants.ROLE_EDITOR);
    }

    boolean includeOwner = normalizedRoles.contains(Constants.ROLE_OWNER);
    boolean includeEditor = normalizedRoles.contains(Constants.ROLE_EDITOR);

    if (!includeEditor) {
      return Future.succeededFuture(includeOwner
        ? new JsonObject().put(Constants.USER_ID, ecasId)
        : new JsonObject().put(Constants.ID, new JsonObject().put("$in", new JsonArray())));
    }

    JsonObject collabFilter = new JsonObject()
      .put(Constants.COLLABORATOR_USER_ID, ecasId)
      .put(Constants.INVITATION_STATUS, Constants.INVITATION_ACCEPTED)
      .put(Constants.APPLICATION_TYPE_FIELD, applicationType)
      .put(Constants.ROLE, Constants.ROLE_EDITOR);

    JsonObject fields = new JsonObject().put(Constants.APPLICATION_ID, 1);

    return mongoClient.findWithOptions(
        Constants.APPLICATION_COLLABORATOR_COLLECTION,
        collabFilter,
        new FindOptions().setFields(fields)
      )
      .map(collabDocs -> {
        JsonArray collabAppIds = new JsonArray();
        for (JsonObject doc : collabDocs) {
          String appId = doc.getString(Constants.APPLICATION_ID);
          if (StringUtils.isNotBlank(appId)) {
            collabAppIds.add(appId);
          }
        }

        if (!includeOwner) {
          return new JsonObject().put(Constants.ID, new JsonObject().put("$in", collabAppIds));
        }

        if (collabAppIds.isEmpty()) {
          return new JsonObject().put(Constants.USER_ID, ecasId);
        }

          return new JsonObject().put("$or", new JsonArray()
          .add(new JsonObject().put(Constants.USER_ID, ecasId))
          .add(new JsonObject().put(Constants.ID, new JsonObject().put("$in", collabAppIds))));
      });
  }

  /**
   * Fetches an application by ID allowing access for both the <strong>owner</strong> and any
   * <strong>accepted collaborator</strong>.
   *
   * <p>This replaces the inherited {@code findById} call (which restricts to owner only via
   * {@code userId = ecasId}) in {@code getApplicationById} overrides of both
   * {@code DataAccessApplicationManager} and {@code DataRequestApplicationManager}.
   *
   * @param context        the Vert.x routing context; path param {@code id} is the application ID
   * @param collectionName the MongoDB collection to search in
   * @param applicationType the application type constant used to scope the collaborator lookup
   */
  protected void findByIdForUserOrCollaborator(
      @NotNull RoutingContext context,
      String collectionName,
      String applicationType) {

    String applicationId = context.pathParam("id");
    String ecasId        = Context.getEcasId(context);

    // Build the $or filter: owner OR accepted collaborator
    buildUserAccessFilter(ecasId, applicationType)
      .compose(accessFilter -> {
        // Combine the $or with the specific _id
        JsonObject query = new JsonObject()
            .put(Constants.ID, applicationId)
            .mergeIn(accessFilter);
        return mongoClient.findOne(collectionName, query, null);
      })
      .onSuccess(doc -> {
        if (doc != null) {
          setUpContextResponse(context, HttpResponseStatus.OK, doc.encode());
        } else {
          setUpContextResponse(context, HttpResponseStatus.NOT_FOUND);
        }
      })
      .onFailure(err -> {
        log.error("findByIdForUserOrCollaborator failed for id='{}': {}", applicationId, err.getMessage(), err);
        setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      });
  }

  protected CommonApplicationManager(DBHandler dbHandler, BasketManager basketManager,
    MongoClient mongoClient, Vertx vertx, HubStoreClient hubStoreClient,
    AuditLogManager auditLogManager, NotificationManager notificationManager,
    HubTranslationClient hubTranslationClient, HubSearchClient hubSearchClient,
    AuthClient authClient) {
    super(dbHandler);
    this.basketManager = basketManager;
    this.mongoClient = mongoClient;
    this.vertx = vertx;
    this.hubStoreClient = hubStoreClient;
    this.auditLogManager = auditLogManager;
    this.notificationManager = notificationManager;
    this.hubTranslationClient = hubTranslationClient;
    this.hubSearchClient = hubSearchClient;
    this.authClient = authClient;
    this.config = new JsonObject();
  }

  public CommonApplicationManager(DBHandler dbHandler, BasketManager basketManager, MongoClient mongoClient, Vertx vertx, HubStoreClient hubStoreClient, AuditLogManager auditLogManager, NotificationManager notificationManager,
    HubTranslationClient hubTranslationClient, HubSearchClient hubSearchClient, AuthClient authClient, JsonObject config) {
    super(dbHandler);
    this.basketManager = basketManager;
    this.mongoClient = mongoClient;
    this.vertx = vertx;
    this.hubStoreClient = hubStoreClient;
    this.auditLogManager = auditLogManager;
    this.notificationManager = notificationManager;
    this.hubTranslationClient = hubTranslationClient;
    this.hubSearchClient = hubSearchClient;
    this.authClient = authClient;
    this.config = config;
  }

  protected CommonApplicationManager(DBHandler dbHandler, MongoClient mongoClient,
    AuditLogManager auditLogManager, NotificationManager notificationManager, JsonObject config) {
    super(dbHandler);
    this.mongoClient = mongoClient;
    this.basketManager = null;
    this.vertx = null;
    this.hubStoreClient = null;
    this.auditLogManager = auditLogManager;
    this.notificationManager = notificationManager;
    this.hubTranslationClient = null;
    this.hubSearchClient = null;
    this.authClient = null;
    this.config = config;
  }

  /**
   * Injects the {@link LockWebSocketManager} used to broadcast {@code section_saved}
   * events to all clients viewing the same application.
   * Called from {@code MainVerticle} after construction.
   *
   * @param lockWebSocketManager the shared WebSocket manager instance
   */
  public void setLockWebSocketManager(LockWebSocketManager lockWebSocketManager) {
    this.lockWebSocketManager = lockWebSocketManager;
  }

  /**
   * Builds the MongoDB projection used by the application list endpoints
   * ({@code getAllDataAccessApplications} / {@code getAllDataRequestApplications}).
   *
   * @return A new {@link JsonObject} projection with all fields required by the application list
   * views and the dataset basket.
   */
  protected JsonObject buildApplicationListProjection() {
    return new JsonObject()
      .put(ID, 1)
      .put(TITLE, 1)
      .put(STATUS_STATUS, 1)
      .put(TRANSLATION_STATUS, 1)
      .put("form.section2.projectLeader", 1)
      .put("form.section2.countryOfProjectLeader.value", 1)
      .put("form.section2.summaryOfTheProject", 1)
      .put("catalogs.catalog_id", 1)
      .put("catalogs.statuses.latest", 1)
      .put("catalogs.statuses.status", 1)
      .put("datasets.dataset_id", 1)
      .put("datasets.catalog_id", 1)
      .put("datasets.distributions.distribution_id", 1)
      .put("userId", 1);
  }

  protected void updateApplicationStatus(JsonObject mongoApplication, JsonObject status,
    RoutingContext context) {

    ApplicationStatus targetStatus = ApplicationStatus.fromStatus(
      status.getString(STATUS));

    // Get current application status.
    JsonObject currentStatusObj = mongoApplication.getJsonObject(STATUS);
    ApplicationStatus currentStatus = ApplicationStatus.fromStatus(
      currentStatusObj.getString("status"));

    if (currentStatus == targetStatus) {
      // If the status is the same as the current one, do not update the status history but just return.
      return;
    }
    // Update the status_history for the application.
    JsonArray previousStatuses = new JsonArray();
    if (currentStatusObj.getJsonArray(STATUS_HISTORY) != null) {
      previousStatuses = currentStatusObj.getJsonArray(STATUS_HISTORY);
      currentStatusObj.remove(STATUS_HISTORY);
    }
    // If the status to go is the Submitted keep the date submitted.
    if (ON_GOING.equals(targetStatus)) {
      mongoApplication.put(DATE_SUBMITTED, Instant.now());
    }
    previousStatuses.add(currentStatusObj);
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

    // Ensure all section stubs exist in the form from creation time,
    // so downstream consumers (e.g. hdnp) can always find every section.
    initializeFormSectionStubs(requestBody, collectionName);

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

  /**
   * Ensures all section stubs exist in the request body's "form" field before the application
   * document is first persisted. This guarantees downstream consumers (e.g. hdnp) always find
   * every section, even when the applicant never visits an optional section.
   *
   * <p>Stubs are minimal JSON objects containing only {@code sectionNumber}. Section 6 is
   * initialized as an empty array to match its expected type. Existing section data is never
   * overwritten.
   *
   * @param requestBody    the mutable application document being created
   * @param collectionName identifies the application type (DA = 10 sections, DR = 8 sections)
   */
  private void initializeFormSectionStubs(JsonObject requestBody, String collectionName) {
    JsonObject form = requestBody.getJsonObject("form");
    if (form == null) {
      form = new JsonObject();
      requestBody.put("form", form);
    }
    int maxSection = DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName) ? 10 : 8;
    for (int i = 1; i <= maxSection; i++) {
      String sectionKey = SECTION + i;
      if (!form.containsKey(sectionKey)) {
        if (i == 6) {
          form.put(sectionKey, new JsonArray());
        } else {
          form.put(sectionKey, new JsonObject().put("sectionNumber", i));
        }
      }
    }
  }

  protected void changeCatalogStatus(@NotNull RoutingContext context, String collectionName) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String catalogId = context.pathParam(CATALOG_ID);
    String catalogStatusString = context.body().asJsonObject().getString(STATUS);

    changeCatalogStatus(applicationId, catalogId, catalogStatusString, null, collectionName,
      context, true);
  }

  protected void changeCatalogStatus(String applicationId, String catalogId,
    String catalogStatusString, JsonObject statusPayload, String collectionName,
    @NotNull RoutingContext context, boolean comingFromUI) {

    CatalogStatus targetCatalogStatus = CatalogStatus.fromLabel(catalogStatusString, context);
    ApplicationStatus targetApplicationStatus = targetCatalogStatus.computeApplicationStatus();

    JsonObject applicationStatusObj = new JsonObject().put("status",
      targetApplicationStatus.getStatus());

    JsonObject query = new JsonObject().put(ID, applicationId);
    query.put(ID, applicationId);
    if (comingFromUI) {
      query.put(USER_ID, Context.getEcasId(context));
    }

    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {
          JsonObject application = ar.result();

          CatalogStatus currentCatalogStatus = CatalogStatus.fromLabel(getLatestCatalogStatus(application, catalogId), context);
          if(currentCatalogStatus.canChangeTo(targetCatalogStatus, context)) {
            String newDeadline = updateCatalogStatus(application, catalogId, targetCatalogStatus,
              statusPayload);
            if(CatalogStatus.PROCESSING_EXTENDED.equals(targetCatalogStatus) || CatalogStatus.PROCESSING.equals(targetCatalogStatus) ) {
              sendApplicationToOutgoingApplicationActivities(collectionName, applicationId,
                targetCatalogStatus, catalogId, null,null, null, null, null, newDeadline, OperationStatus.ACTIVITY_RESPONSE);
            }
          }

          ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(
            application.getJsonObject("status").getString("status"));
          if (currentApplicationStatus.canChangeTo(targetApplicationStatus, application, context)) {
            updateApplicationStatus(application, applicationStatusObj, context);
            String auditLogActionDescriptionForApplicationUpdate = createAuditLogActionDescriptionForApplicationUpdate(
              applicationId, application.getString("title"), targetApplicationStatus,
              collectionName);
            insertAuditLogForApplicationUpdate(auditLogActionDescriptionForApplicationUpdate,
              collectionName, applicationId, context);
          }

          applySpecialRules(application, targetCatalogStatus);

          replaceApplication(application, collectionName)
            .onSuccess(replaceResult -> {
              setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
              if(comingFromUI)
                sendApplicationToOutgoingApplicationActivities(collectionName, applicationId,
                  targetCatalogStatus, catalogId, null,null, null,null,null, null, OperationStatus.STATUS_UPDATE);

              if (statusPayload != null) {
                saveCatalogTranslationState(collectionName, application, catalogId)
                  .onFailure(err -> log.error(
                    "Failed to save catalog status translation state for application {} and catalog {}: {}",
                    applicationId,
                    catalogId,
                    err.getMessage(),
                    err));
              }

              //Add Notification
              addNotificationForChangeCatalogStatus(application, targetCatalogStatus, catalogId,
                collectionName);

              //Audit Log
              String auditLogActionDescription = createAuditLogActionDescriptionForChangeHDABStatus(
                targetCatalogStatus, collectionName, applicationId, application.getString("title"),
                catalogId);
              insertAuditLogForHDABUpdate(auditLogActionDescription, collectionName, applicationId,
                context);

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

  private void applySpecialRules(JsonObject application, CatalogStatus targetCatalogStatus) {

    if (CatalogStatus.ADDITIONAL_INFORMATION.equals(targetCatalogStatus)) {
      application.getJsonObject("translation")
        .put("status", TranslationStatus.FILES_COMPLETED.name());
      application.remove("form_translations");
    }
  }

  public void changePermitStatus(String applicationId, String catalogId, String datasetId,
    String permitStatusString, String reason, String renewAmendFile, String rejectionReason, String rejectionFile, String revocationFile, @NotNull RoutingContext context) {
    changePermitStatus(applicationId, catalogId, datasetId,
      permitStatusString, reason, null, renewAmendFile, rejectionReason, rejectionFile,  revocationFile,  null,
      null, null, null,
      context, true, null);
  }

  public void changePermitStatus(String applicationId, String catalogId, String datasetId,
    String permitStatusString, String reason, String permitFile, String renewAmendFile, String rejectionReason, String rejectionFile, String revocationFile,  String permitStartDate,
    String permitExpirationDate, String permitReferenceNumber, String permitContactDetails,
    @NotNull RoutingContext context, boolean comingFromUI, JsonObject translatedFields) {

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

          ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(
            application.getJsonObject("status").getString("status"));
          currentApplicationStatus.validatePermitChange(context);
          CatalogStatus currentCatalogStatus = CatalogStatus.fromLabel(
            getLatestCatalogStatus(application, catalogId), context);
          currentCatalogStatus.validatePermitChange(context);
          String latestCurrentDatasetStatus = getLatestDatasetStatus(application, catalogId,
            datasetId, context);
          DatasetStatus currentDatasetStatus = DatasetStatus.fromLabel(latestCurrentDatasetStatus,
            context);
          currentDatasetStatus.validatePermitChange(context);

          String latestPermitStatusString = getLatestPermitStatus(application, catalogId, datasetId,
            context);
          if (StringUtils.isNotEmpty(latestPermitStatusString)) {
            PermitStatus currentPermitStatus = PermitStatus.fromLabel(latestPermitStatusString,
              true, context);
            currentPermitStatus.canChangeTo(targetPermitStatus, permitExpirationDate, context);
          } else {
            targetPermitStatus.validateInitialization(comingFromUI, permitStartDate,
              permitExpirationDate, permitReferenceNumber, permitContactDetails, context);
          }

          updatePermitStatus(application, catalogId, datasetId, targetPermitStatus, permitFile, renewAmendFile,  rejectionReason, rejectionFile,  revocationFile,
            permitStartDate, permitExpirationDate, permitReferenceNumber, permitContactDetails,
            reason, translatedFields);

          replaceApplication(application, DATA_ACCESS_APPLICATION_COLLECTION)
            .onSuccess(replaceResult -> {
              setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());
              if (comingFromUI) {
                sendApplicationToOutgoingApplicationActivities(
                  DATA_ACCESS_APPLICATION_COLLECTION,
                  applicationId,
                  currentCatalogStatus,
                  catalogId,
                  targetPermitStatus.getLabel(),
                  datasetId,
                  reason,
                  renewAmendFile,
                  rejectionReason,
                  rejectionFile,
                  revocationFile,
                  null,
                  OperationStatus.PERMIT,
                  null);

              }

              if (comingFromUI || (translatedFields != null && !translatedFields.isEmpty())) {
                saveApplicationTranslationState(
                  DATA_ACCESS_APPLICATION_COLLECTION,
                  application,
                  catalogId,
                  datasetId,
                  OperationStatus.PERMIT)
                  .onFailure(err -> log.error(
                    "Failed to save permit translation state for application {} and dataset {}: {}",
                    applicationId,
                    datasetId,
                    err.getMessage(),
                    err));
              }

              //Add Notification
              addNotificationForChangePermitStatus(application, targetPermitStatus, datasetId);

              //Audit Log
              String auditLogActionDescription = createAuditLogActionDescriptionForChangePermitStatus(
                applicationId, application.getString("title"), targetPermitStatus, catalogId,
                datasetId);
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

  private String getLatestPermitStatus(JsonObject mongoApplication, String catalogId,
    String datasetId, @NotNull RoutingContext context) {
    return getLatestStatus("permits", "permit_status", mongoApplication, catalogId, datasetId,
      context);
  }

  private String getLatestDatasetStatus(JsonObject mongoApplication, String catalogId,
    String datasetId, @NotNull RoutingContext context) {
    return getLatestStatus("decisions", "datasetStatus", mongoApplication, catalogId, datasetId,
      context);
  }

  private String getLatestStatus(String fieldName, String fieldStatusName,
    JsonObject mongoApplication, String catalogId, String datasetId,
    @NotNull RoutingContext context) {

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
    String decisionFileBase64 = context.body().asJsonObject().getString("decision_file");
    String decisionReason = context.body().asJsonObject().getString("decision_reason");

    changeDatasetStatus(applicationId, catalogId, datasetId, datasetStatusString,
      decisionFileBase64,
      collectionName, context, true, decisionReason, null, null)
      .onFailure(err -> {
        log.error("changeDatasetStatus failed for application {}: {}", applicationId, err.getMessage());
        if (!context.response().ended()) {
          setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
      });
  }

  protected Future<Void> changeDatasetStatus(
    String applicationId,
    String catalogId,
    String datasetId,
    String datasetStatusString,
    String decisionFileBase64,
    String collectionName,
    @NotNull RoutingContext context,
    boolean comingFromUI,
    String decisionReason,
    String supportFileBase64,
    JsonObject translatedFields) {

    DatasetStatus targetDatasetStatus = DatasetStatus.fromLabel(datasetStatusString, context);
    CatalogStatus targetCatalogStatus = targetDatasetStatus.computeCatalogStatus();
    ApplicationStatus targetApplicationStatus = targetCatalogStatus.computeApplicationStatus();

    JsonObject applicationStatusObj = new JsonObject().put("status", targetApplicationStatus);

    JsonObject query = new JsonObject().put(ID, applicationId);
    if (comingFromUI) {
      query.put(USER_ID, Context.getEcasId(context));
    }

    // 🔥 Convert Mongo call to Future
    return mongoClient
      .findOne(collectionName, query, null)
      .compose(application -> {

        if (application == null) {
          setUpContextResponse(context, HttpResponseStatus.NOT_FOUND);
          return Future.failedFuture("Application not found");
        }

        String latestCurrentDatasetStatus = getLatestDatasetStatus(application, catalogId, datasetId, context);

        if (StringUtils.isEmpty(latestCurrentDatasetStatus)) {

          if (targetDatasetStatus.validateInitialization(context)) {
            updateDatasetStatus(application, catalogId, datasetId, targetDatasetStatus,
              decisionFileBase64, decisionReason, supportFileBase64, translatedFields);
          }

          CatalogStatus currentCatalogStatus = CatalogStatus.fromLabel(
            getLatestCatalogStatus(application, catalogId), context);

          if (currentCatalogStatus.canChangeTo(targetCatalogStatus, context)) {
            updateCatalogStatus(application, catalogId, targetCatalogStatus);

            insertAuditLogForDatasetUpdate(
              createAuditLogActionDescriptionForChangeHDABAndDecisionStatus(
                targetCatalogStatus, collectionName, applicationId,
                application.getString("title"), catalogId,
                latestCurrentDatasetStatus, targetDatasetStatus, datasetId),
              collectionName, applicationId, context);
          }

        } else {

          DatasetStatus currentDatasetStatus = DatasetStatus.fromLabel(latestCurrentDatasetStatus, context);

          if (currentDatasetStatus.canChangeTo(targetDatasetStatus, collectionName,
            decisionReason, context)) {

            updateDatasetStatus(application, catalogId, datasetId, targetDatasetStatus,
              decisionFileBase64, decisionReason, supportFileBase64, translatedFields);

            insertAuditLogForDatasetUpdate(
              createAuditLogActionDescriptionForChangeHDABAndDecisionStatus(
                targetCatalogStatus, collectionName, applicationId,
                application.getString("title"), catalogId,
                latestCurrentDatasetStatus, targetDatasetStatus, datasetId),
              collectionName, applicationId, context);
          }
        }

        ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(
          application.getJsonObject("status").getString("status"));

        if (currentApplicationStatus.canChangeTo(targetApplicationStatus, application, context)) {

          updateApplicationStatus(application, applicationStatusObj, context);

          insertAuditLogForApplicationUpdate(
            createAuditLogActionDescriptionForApplicationUpdate(
              applicationId, application.getString("title"),
              targetApplicationStatus, collectionName),
            collectionName, applicationId, context);
        }

        // 🔥 Persist changes
        return replaceApplication(application, collectionName)
          .onSuccess(replaceResult -> {
            setUpContextResponse(context, HttpResponseStatus.OK, replaceResult.encode());

            // side-effects (no response handling)
            if (comingFromUI) {
              sendApplicationToOutgoingApplicationActivities(
                collectionName, applicationId,
                targetCatalogStatus, catalogId,
                targetDatasetStatus.getLabel(), datasetId,
                decisionReason, null, null, null, null,
                decisionFileBase64, OperationStatus.DECISION,
                null, application);
            }

            addNotificationForChangeDatasetStatus(
              application, targetDatasetStatus, datasetId, collectionName);

            if (!comingFromUI && translatedFields != null && !translatedFields.isEmpty()) {
              saveApplicationTranslationState(
                collectionName, application, catalogId, datasetId, OperationStatus.DECISION)
                .onFailure(err -> log.error("Failed translation state: {}", err.getMessage()));
            }
          })
          .onFailure(err -> {
            log.error("Failed to persist dataset status change for application {}: {}", applicationId, err.getMessage());
            setUpContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
          })
          .mapEmpty();
      });
  }


  private void updateDatasetStatus(JsonObject application, String catalogId, String datasetId,
    DatasetStatus datasetStatus, String decisionFileBase64, String decisionReason,
    String supportFileBase64, JsonObject translatedFields) {
    JsonArray datasets = application.getJsonArray("datasets");
    updateDatasetStatus(datasets, catalogId, datasetId, datasetStatus, decisionFileBase64,
      decisionReason, supportFileBase64, translatedFields);
  }

  protected void updateDatasetStatus(JsonArray datasets, String catalogId, String datasetId,
    DatasetStatus datasetStatus, String decisionFileHubRepoId, String decisionReason,
    String supportFileBase64, JsonObject translatedFields) {

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
        if (StringUtils.isNotEmpty(decisionFileHubRepoId)) {
          decision.put("decision_file", decisionFileHubRepoId);
        }
        if (StringUtils.isNotEmpty(decisionReason)) {
          decision.put("decision_reason", decisionReason);
        }

        if (StringUtils.isNotEmpty(supportFileBase64)) {
          decision.put("support_file", supportFileBase64);
        }
        if (translatedFields != null && !translatedFields.isEmpty()) {
          decision.put("translatedFields", translatedFields);
        }
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

  protected String updateCatalogStatus(JsonObject application, String catalogId, CatalogStatus catalogStatus) {
    JsonArray catalogs = computeCatalogs(null, catalogId, catalogStatus, null, application);
    application.put("catalogs", catalogs);

    JsonObject catalog = findCatalogById(catalogs, catalogId);

    if (null != catalog && null != catalog.getJsonArray("statuses")) {
     return getLastJsonObject(catalog.getJsonArray("statuses"))
        .map(status -> status.getString(DATE_DEADLINE))
        .orElse(null);
    } else
      return null;
  }

  protected String updateCatalogStatus(JsonObject application, String catalogId,
    CatalogStatus catalogStatus, JsonObject statusPayload) {
    JsonArray catalogs = computeCatalogs(null, catalogId, catalogStatus, statusPayload,
      application);
    application.put("catalogs", catalogs);

    JsonObject catalog = findCatalogById(catalogs, catalogId);

    if (catalogStatus == CatalogStatus.PROCESSING) {
      if(null != catalog && null != catalog.getJsonArray("statuses")) {
        return getLastJsonObject(catalog.getJsonArray("statuses")).map(
          status -> status.getString(DATE_EXTENSION_DEADLINE)).orElse(null);
      } else {
        return null;
      }
    } else {
      if(null != catalog && null != catalog.getJsonArray("statuses")) {
        return getLastJsonObject(catalog.getJsonArray("statuses")).map(
          status -> status.getString(DATE_DEADLINE)).orElse(null);
      }
      else {
        return null;
      }
    }
  }

  private JsonArray createCatalogs(JsonArray datasets) {
    return computeCatalogs(datasets, null, CatalogStatus.DRAFT, null, null);
  }

  private JsonArray computeCatalogs(JsonArray datasets, String catalogId, CatalogStatus status,
    JsonObject statusPayload,
    JsonObject application) {

    JsonArray catalogs = new JsonArray();
    Set<String> catalogIds = new LinkedHashSet<>();

    //
    // CASE 1 — datasets NOT NULL → new application (initial creation) will create the catalogs
    //
    if (datasets != null && !datasets.isEmpty()) {

      Map<String, JsonArray> catalogLanguagesById = getCatalogLanguagesByCatalogId(datasets);
      catalogIds.addAll(catalogLanguagesById.keySet());

      JsonObject newStatusObj = new JsonObject()
        .put("status", status.getLabel())
        .put("date_received", Instant.now())
        .put("latest", 1);

      if(CatalogStatus.SUBMITTED.equals(status)) {
        String newDeadline =  computeCatalogDeadline(application, null,
          config.getInteger(Constants.EXPIRATION_DAYS), status,null);
        newStatusObj.put(DATE_DEADLINE, newDeadline);
      }

      catalogIds.forEach(id -> {
        JsonArray statuses = new JsonArray().add(newStatusObj.copy());
        catalogs.add(new JsonObject()
          .put("catalog_id", id)
          .put(CATALOG_LANGUAGES, catalogLanguagesById.getOrDefault(id, new JsonArray()).copy())
          .put("statuses", statuses));
      });

      return catalogs;

      //
      // CASE 2 — datasets NULL && catalogId NULL → change status for all catalogs ex. CatalogStatus.SUBMITTED
      //
    } else if (datasets == null && StringUtils.isEmpty(catalogId)) {

      JsonArray mongoDatasets = application.getJsonArray(DATASETS);
      if (mongoDatasets != null && !mongoDatasets.isEmpty()) {
        catalogIds.addAll(getCatalogLanguagesByCatalogId(mongoDatasets).keySet());
      } else {
        log.error("Application with id: {} has no datasets", application.getString("_id"));
        return new JsonArray();
      }

      Map<String, JsonArray> catalogLanguagesById = getCatalogLanguagesByCatalogId(mongoDatasets);
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
          .put(CATALOG_LANGUAGES, new JsonArray())
          .put("statuses", new JsonArray()));

        JsonArray statuses = catalog.getJsonArray("statuses", new JsonArray());

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

        catalog.put(
          CATALOG_LANGUAGES,
          catalogLanguagesById.getOrDefault(
            catId,
            catalog.getJsonArray(CATALOG_LANGUAGES, new JsonArray())
          ).copy()
        );
        catalog.put("statuses", statuses);
        catalogMap.put(catId, catalog);
      }

      catalogMap.values().forEach(catalogs::add);

      return catalogs;

      //
      // CASE 3 — datasets NULL && catalogId NOT NULL → update existing application catalog
      //
    } else if (datasets == null && StringUtils.isNotEmpty(catalogId)) {

      Map<String, JsonArray> catalogLanguagesById = getCatalogLanguagesByCatalogId(
        application.getJsonArray(DATASETS, new JsonArray()));
      JsonArray existingCatalogs = application.getJsonArray("catalogs", new JsonArray());
      JsonArray updatedCatalogs = new JsonArray();

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
        String latestDeadlineDate = null;
        String additionalInformationDate = null;
        // reset latest=0 for all previous statuses
        for (int i = 0; i < statuses.size(); i++) {
          if(statuses.getJsonObject(i).getString("status").equalsIgnoreCase(CatalogStatus.ADDITIONAL_INFORMATION.getLabel())) {
            additionalInformationDate = extractAdditionalInformationRequestingDate(
              statuses.getJsonObject(i).getJsonObject("statusPayload"));
          }
          if(statuses.getJsonObject(i).getString(DATE_DEADLINE) != null){
            latestDeadlineDate = statuses.getJsonObject(i).getString(DATE_DEADLINE);
          }
          statuses.getJsonObject(i).put("latest", 0);
        }

        // build new status
        JsonObject updatedStatus = new JsonObject()
          .put("status", status.getLabel())
          .put("date_received", Instant.now())
          .put("latest", 1);

        if(CatalogStatus.SUBMITTED.equals(status) || CatalogStatus.PROCESSING_EXTENDED.equals(status)) {
          String newDeadline= computeCatalogDeadline(application, latestDeadlineDate,
            config.getInteger(Constants.EXPIRATION_DAYS), status, additionalInformationDate);
          updatedStatus.put(DATE_DEADLINE, newDeadline);
        }
        else if(CatalogStatus.PROCESSING.equals(status)) {
          String newExtensionDeadline= computeCatalogExtensionDeadline(latestDeadlineDate,
            config.getInteger(Constants.EXPIRATION_DAYS));
          updatedStatus.put(DATE_EXTENSION_DEADLINE, newExtensionDeadline);
        }

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

        if (catalogLanguagesById.containsKey(cid)) {
          catalog.put(CATALOG_LANGUAGES, catalogLanguagesById.get(cid).copy());
        }

        catalog.put("statuses", statuses);
        updatedCatalogs.add(catalog);
      }

      return updatedCatalogs;
    } else {
      log.error("computeCatalogs operation failed for catalog id: {}", catalogId);
      return new JsonArray();
    }
  }

  private Map<String, JsonArray> getCatalogLanguagesByCatalogId(JsonArray datasets) {
    Map<String, LinkedHashSet<String>> uniqueLanguagesByCatalogId = new LinkedHashMap<>();

    if (datasets == null || datasets.isEmpty()) {
      return new LinkedHashMap<>();
    }

    for (Object datasetObj : datasets) {
      if (!(datasetObj instanceof JsonObject dataset)) {
        continue;
      }

      String datasetCatalogId = dataset.getString(CATALOG_ID);
      if (StringUtils.isEmpty(datasetCatalogId)) {
        continue;
      }

      LinkedHashSet<String> uniqueLanguages = uniqueLanguagesByCatalogId.computeIfAbsent(
        datasetCatalogId,
        ignored -> new LinkedHashSet<>()
      );

      JsonArray datasetCatalogLanguages = dataset.getJsonArray(CATALOG_LANGUAGES, new JsonArray());

      for (Object languageObj : datasetCatalogLanguages) {
        if (languageObj instanceof String languageId) {
          String normalizedLanguageId = StringUtils.trimToEmpty(languageId);
          if (StringUtils.isNotEmpty(normalizedLanguageId)) {
            uniqueLanguages.add(normalizedLanguageId);
          }
        } else if (languageObj instanceof JsonObject language) {
          // optional backward compatibility in case old data still exists
          String languageId = StringUtils.trimToEmpty(language.getString("id"));
          if (StringUtils.isNotEmpty(languageId)) {
            uniqueLanguages.add(languageId);
          }
        }
      }
    }

    Map<String, JsonArray> catalogLanguagesById = new LinkedHashMap<>();
    uniqueLanguagesByCatalogId.forEach((catalogKey, languages) -> {
      JsonArray catalogLanguages = new JsonArray();
      languages.forEach(catalogLanguages::add);
      catalogLanguagesById.put(catalogKey, catalogLanguages);
    });

    return catalogLanguagesById;
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

    // Set top-level variables from hub result onto the dataset
    JsonArray variables = result.getJsonArray(VARIABLES);
    if (variables != null && !variables.isEmpty()) {
      dataset.put(VARIABLES, variables);
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

            // UC 10.3 – notify all accepted collaborators that the draft was discarded
            notifyCollaboratorsOfDiscard(id, collectionName, document.getString(TITLE));

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

    log.debug("PostData called for applicationId: {}", applicationId);

    // Build an $or filter that matches the application if the caller is either
    // the owner (userId == ecasId) OR an ACCEPTED collaborator for this application.
    // Previously this used a hard-coded userId-only query, which silently rejected
    // every file upload attempt made by collaborators (returned NOT_FOUND instead of OK).
    buildUserAccessFilter(userId, collectionName)
      .onSuccess(accessFilter -> {
        JsonObject query = new JsonObject()
            .put(ID, applicationId)
            .mergeIn(accessFilter);

        // Search in database based on the provided application id and user access rights
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
      })
      .onFailure(err -> {
        log.error("PostData failed to build user access filter for userId={}, error={}", userId, err.getMessage());
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, err.getMessage());
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

  /**
   * Triggers a file/document translation request and persists translation metadata to enable retry
   * mechanism.
   * <p>
   * <b>Important Design Pattern:</b> This method creates the translation object in the database
   * BEFORE
   * making the translation request to the external service. This ensures that even if the
   * translation service is unavailable or the request fails, the translation metadata exists in the
   * database with PENDING status, allowing the
   * {@link eu.europa.data.health.access.scheduled_jobs.TranslationRetryScheduledJob} to discover
   * and retry the translation automatically.
   * </p>
   *
   * <h3>Execution Flow:</h3>
   * <ol>
   *   <li>Fetch file content as Base64 from hub-store service</li>
   *   <li>Build translation payload with source/target languages</li>
   *   <li><b>Create translation metadata in DB with PENDING status</b> (critical for retry mechanism)</li>
   *   <li>Make translation request to hub-translation service</li>
   *   <li>If request fails, translation object remains PENDING for scheduler to retry</li>
   * </ol>
   *
   * <h3>Translation Object Structure Created:</h3>
   * <pre>
   * {
   *   "translation": {
   *     "status": "FILES_PENDING",
   *     "createdAt": "2026-01-27T13:00:00Z",        // Used by scheduler to determine retry timing
   *     "sourceLanguage": "EN",
   *     "targetLanguages": ["DE", "FR"],
   *     "files": {
   *       "fileId": {
   *         "status": "PENDING",
   *         "attempts": 0,                           // Tracks retry attempts
   *         "createdAt": "2026-01-27T13:00:00Z"     // Independent timestamp per file
   *       }
   *     }
   *   }
   * }
   * </pre>
   *
   * <h3>Retry Mechanism:</h3>
   * The scheduler runs periodically (default: every 5 minutes) and queries for applications where:
   * <ul>
   *   <li><code>translation.files.{fileId}.status = PENDING</code></li>
   *   <li><code>translation.createdAt <= (now - retryInterval)</code></li>
   * </ul>
   * This allows automatic retries up to the configured maximum attempts (default: 3).
   *
   * @param applicationId   The unique identifier of the application
   * @param applicationType The MongoDB collection name (e.g., "data_access_applications")
   * @param targetLanguages List of target language codes to translate into (e.g., ["DE", "FR",
   *                        "ES"])
   * @param sourceLang      The source language code (e.g., "EN")
   * @param documentId      The file identifier to fetch from hub-store and translate
   * @see eu.europa.data.health.access.scheduled_jobs.TranslationRetryScheduledJob
   * @see #triggerFormTranslationRequest(String, JsonObject, String, String, List, JsonObject,
   * Boolean)
   */
  protected void triggerDocumentTranslationRequest(
    String applicationId,
    String applicationType,
    List<String> targetLanguages,
    String sourceLang,
    String documentId
  ) {
    triggerDocumentTranslationRequest(applicationId, applicationType, targetLanguages, sourceLang,
      documentId, false);
  }

  /**
   * Triggers a file/document translation request and persists translation metadata to enable
   * retry.
   *
   * @param preserveExistingTranslations When {@code true}, existing form translation data
   *                                     ({@code translation.form} and {@code form_translations})
   *                                     will NOT be unset. Use this when triggering file
   *                                     translations as part of a remaining-translations flow where
   *                                     form translations may already exist and must be preserved.
   */
  protected void triggerDocumentTranslationRequest(
    String applicationId,
    String applicationType,
    List<String> targetLanguages,
    String sourceLang,
    String documentId,
    boolean preserveExistingTranslations
  ) {
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

          // CRITICAL: Create translation metadata BEFORE making the request
          // Why? This ensures the object exists for retry mechanism even if the request fails.
          //
          // Without this pattern:
          //   - Translation service is down → request fails → no DB object created
          //   - Scheduler can't find pending translations → no retries happen
          //   - Translation never completes
          //
          // With this pattern:
          //   - Translation metadata created with PENDING status and attempts=0
          //   - Translation service is down → request fails → object remains PENDING
          //   - Scheduler finds it via translation.createdAt timestamp
          //   - Automatic retries happen every 5 minutes (configurable)
          //   - After 3 failed attempts (configurable), marked as FAILED with audit log
          JsonObject setFields = new JsonObject()
            .put(DATE_MODIFIED, Instant.now())
            .put(TRANSLATION_STATUS, TranslationStatus.FILES_PENDING.name())
            .put("translation.sourceLanguage", sourceLang)
            .put("translation.targetLanguages", new JsonArray(targetLanguages))
            // Translation-level createdAt: Used by scheduler instead of date_submitted/date_modified
            // This isolates translation timing from application updates
            .put("translation.createdAt", Instant.now().toString())
            .put(TRANSLATION_FILES + fileId, new JsonObject()
              .put("status", "PENDING")
              // attempts: Tracks retry count. Scheduler increments on each retry.
              // When attempts >= maxAttempts (default: 3), translation is marked FAILED
              .put("attempts", 0)
              // File-level createdAt: Independent timestamp per file for multi-file translations
              .put("createdAt", Instant.now().toString()));

          JsonObject translationUpdate = new JsonObject().put("$set", setFields);

          if (!preserveExistingTranslations) {
            // Remove form translation when switching from form to file translation
            translationUpdate.put(
              "$unset",
              new JsonObject().put("translation.form", true).put("form_translations", true)
            );
          }

          log.info(
            "TriggerDocumentTranslationRequest: Persisting translation metadata for applicationId={}",
            applicationId);

          // Step 1: Persist translation metadata to database
          mongoClient.updateCollection(applicationType,
              new JsonObject().put("_id", applicationId), translationUpdate)
            .onSuccess(v2 -> {
              log.info(
                "TriggerDocumentTranslationRequest: Translation metadata persisted successfully for applicationId={}",
                applicationId);

              // Step 2: Now make the translation request to external service
              // If this fails, the PENDING object remains in DB for scheduler to retry
              hubTranslationClient.requestFileTranslation(filePayload)
                .onSuccess(v -> {
                  log.info("File translation triggered for fileId={}, applicationId={}", fileId,
                    applicationId);
                  // Translation service accepted the request
                  // When translation completes, callback will update status to COMPLETED
                })
                .onFailure(err -> {
                  log.error(
                    "File translation request failed for fileId={}, applicationId={}, error={}",
                    fileId,
                    applicationId, err.getMessage(), err);
                  // Translation object already exists with PENDING status
                  // Scheduler will automatically retry based on translation.createdAt timestamp
                  // Log failure once for audit trail (prevents duplicate audit logs)
                  logTranslationFailureOnce(applicationId, applicationType,
                    "Failed to request file translation");
                });
            })
            .onFailure(err -> {
              log.error(
                "Failed to persist translation metadata for applicationId={}, error={}",
                applicationId, err.getMessage(), err);
              logTranslationFailureOnce(applicationId, applicationType,
                "Failed to persist translation metadata");
            });

        } catch (Exception ex) {
          log.error("Failed to build or send file translation payload for fileId={}, error={}",
            fileId, ex.getMessage(), ex);
          logTranslationFailureOnce(applicationId, applicationType,
            "Failed to build file translation payload");
        }

      })
      .onFailure(err -> {
        log.error("Failed to fetch Base64 files for applicationId={}, error={}", applicationId,
          err.getMessage(), err);
        logTranslationFailureOnce(applicationId, applicationType, "Failed to fetch Base64 files");
      });
  }

    /**
     * Delete file by id
     */
  public void getData(@NotNull RoutingContext context, String collectionName) {
    String applicationId = context.pathParam(APPLICATION_ID);

    JsonObject query = new JsonObject()
      .put(ID, applicationId);

    // Check if the application exists
    mongoClient.findOne(collectionName, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() != null) {
          // Proceed to get the file data
          getFileData(context, applicationId);
        } else {
          // Document does not exist
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.NOT_FOUND,
            APPLICATION_WITH_ID + applicationId + WAS_NOT_FOUND
          );
        }
      } else {
        // Error occurred during the lookup
        log.error(ERROR_CHECKING_DOCUMENT_EXISTENCE, ar.cause());

        setUpMessageContextResponse(
          context,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE
        );
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

    // Build an $or filter that matches the application if the caller is either
    // the owner (userId == ecasId) OR an ACCEPTED collaborator for this application.
    // Previously this used a hard-coded userId-only query, which silently rejected
    // every file deletion attempt made by collaborators (returned NOT_FOUND instead of OK).
    buildUserAccessFilter(userId, collectionName)
      .onSuccess(accessFilter -> {
        JsonObject query = new JsonObject()
            .put(ID, applicationId)
            .mergeIn(accessFilter);

        // Search in database based on the provided application id and user access rights
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
      })
      .onFailure(err -> {
        log.error("deleteData failed to build user access filter for userId={}, error={}", userId, err.getMessage());
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, err.getMessage());
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
      unset.put(TRANSLATION_FILES + fileId, true) // remove only this file
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
        .put(TRANSLATION_STATUS, status)
        .put("date_modified", Instant.now());

      update.put("$set", set);
    }
    unset.put("form_translations", true);

    update.put("$unset", unset);
    return update;
  }

  protected void createOrUpdateSection(@NotNull RoutingContext context, String collectionName) {

    String applicationId = context.pathParam(APPLICATION_ID);
    String ecasId        = Context.getEcasId(context);

    // Build an $or filter that matches the application if the caller is either
    // the owner (userId == ecasId) OR an ACCEPTED collaborator for this application.
    // Previously this used a hard-coded userId-only query, which silently rejected
    // every save attempt made by collaborators (returned NOT_FOUND instead of OK).
    buildUserAccessFilter(ecasId, collectionName)
      .onSuccess(accessFilter -> {
        JsonObject query = new JsonObject()
            .put(ID, applicationId)
            .mergeIn(accessFilter);

        // Search in database based on the provided application id and user access rights
        mongoClient.findOne(collectionName, query, null, ar -> {
          if (ar.succeeded()) {
            if (ar.result() != null) {

              JsonObject mongoApplication = ar.result();

          //Able to create/update an application only if status is not SUBMITTED
          if (!ON_GOING.getStatus()
            .equalsIgnoreCase(mongoApplication.getJsonObject(STATUS).getString(STATUS))
            || Objects.nonNull(mongoApplication.getJsonArray("additionalInformation"))) {

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

                    // Broadcast section_saved event to all connected collaborators
                    if (lockWebSocketManager != null) {
                      String sessionId = context.request().getHeader("X-Session-Id");
                      lockWebSocketManager.broadcastSectionSaved(
                        collectionName, applicationId, sectionNumber, ecasId, sessionId);
                    }
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
      })
      .onFailure(err -> {
        log.error("createOrUpdateSection: failed to build access filter for applicationId='{}' ecasId='{}': {}",
          applicationId, ecasId, err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Could not verify access rights for this application");
      });
  }

  JsonArray sameAsSection3Array(JsonArray sectionFormArray, JsonObject section3) {
    if (Objects.isNull(section3)) {
      return sectionFormArray;
    }

    JsonObject legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalOrNaturalPerson)) {
      return sectionFormArray;
    }

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
    if (Objects.isNull(section3)) {
      return sectionForm;
    }

    JsonObject legalOrNaturalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalOrNaturalPerson)) {
      return sectionForm;
    }

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

    if (Objects.isNull(section3)) {
      return;
    }

    switch (sectionNumber) {
      case 4:
        updateSection4(updateForm.getJsonObject("form.section4"), section3);
        break;
      case 5:
        updateSection5(updateForm.getJsonObject("form.section5"), section3);
        break;
      case 6:
        if (Boolean.TRUE.equals(isAccess)) {
          updateSection6(updateForm.getJsonArray("form.section6"), section3);
        }
        break;
      case 8:
        if (Boolean.TRUE.equals(isAccess)) {
          updateSection8(updateForm.getJsonObject("form.section8"), section3);
        }
        break;
      default:
        break;
    }
  }

  private void updateSection4(JsonObject section4, JsonObject section3) {
    if (Objects.isNull(section4)) {
      return;
    }

    boolean sameAsContactPerson = section4.getBoolean(SAME_AS_CONTACT_PERSON, false);

    if (!sameAsContactPerson) {
      return;
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) {
      return;
    }

    String key = legalPerson.getString("key");
    if ("a".equals(key)) {
      updateLegalPerson(section4, section3);
    } else if ("b".equals(key)) {
      updateNaturalPerson(section4, section3);
    }
  }

  private void updateSection5(JsonObject section5, JsonObject section3) {
    if (Objects.isNull(section5)) {
      return;
    }

    updatePerson(section5, section3, PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON,
      PERSON_RESPONSIBLE_NAME);

    updatePerson(section5, section3, PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, PERSON_RESEARCH_NAME);
  }

  private void updateSection8(JsonObject section8, JsonObject section3) {
    if (Objects.isNull(section8)) {
      return;
    }

    boolean sameAsContactPerson = section8.getBoolean(SAME_AS_CONTACT_PERSON, false);

    if (!sameAsContactPerson) {
      return;
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) {
      return;
    }

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
    if (!sameAsContactPerson) {
      return;
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    if (Objects.isNull(legalPerson)) {
      return;
    }

    String personType = legalPerson.getString("key");
    if ("a".equals(personType)) {
      section.put(nameKey, section3.getString(LEGAL_PERSON_NAME, ""));
    } else if ("b".equals(personType)) {
      section.put(nameKey, section3.getString(NATURAL_PERSON_NAME, ""));
    }
  }

  private void updateSection6(JsonArray section6, JsonObject section3) {
    if (Objects.isNull(section6)) {
      return;
    }

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
    if (Objects.isNull(legalPerson)) {
      return;
    }

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

  protected void sendApplicationForDispatch(JsonObject mongoApplication, String database,
    OperationStatus operation) {

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
    String applicationId, CatalogStatus catalogStatus, String catalogId, String reason, String renewAmendFile,  String rejectionReason, String rejectionFile, String revocationFile,
    String deadlineDate, OperationStatus operationType) {

    JsonObject outgoingApplicationActivity = createOutgoingApplicationActivity(collectionName,
      applicationId, catalogStatus, catalogId, reason, renewAmendFile, rejectionReason, rejectionFile, revocationFile, deadlineDate,operationType);
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

  public Future<Void> sendApplicationToOutgoingApplicationActivities(String collectionName,
    String applicationId, CatalogStatus catalogStatus, String catalogId, String status,
    String datasetId, String reason, String renewAmendFile, String rejectionReason, String rejectionFile, String revocationFile, String decisionFileBase64, OperationStatus operationStatus,
    String deadlineDate) {
    Promise<Void> promise = Promise.promise();

    JsonObject outgoingApplicationActivity = createOutgoingApplicationActivity(collectionName,
      applicationId, catalogStatus, catalogId, status, datasetId, operationStatus, reason, renewAmendFile, rejectionReason, rejectionFile,  revocationFile,
      decisionFileBase64, deadlineDate);
    mongoClient.save(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION, outgoingApplicationActivity,
      res -> {
        if (res.succeeded()) {
          log.debug(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Saved outgoing application activity with id: {}",
            res.result());
          promise.complete();
        } else {
          log.error(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Failed to save outgoing application activity, with error message: {}",
            res.cause().getMessage());
          promise.fail(res.cause());
        }
      });
    return promise.future();
  }

  /**
   * Overloaded version of {@link #sendApplicationToOutgoingApplicationActivities} that accepts the
   * full {@code application} document.
   *
   * <p>For a {@link OperationStatus#DECISION DECISION} operation the activity is <em>always</em>
   * dispatched immediately. Any {@code translatedFields} already stored on the application's latest
   * decision are embedded inside {@code decisions[0]} so the national dispatcher always receives
   * the most up-to-date translations.
   *
   * <p>Additionally, if the decision has translatable content (a non-blank {@code reason} or
   * {@code decisionFileBase64}) and a {@link DecisionTranslationService} is configured, a
   * translation state record is also saved so that an {@code ACTIVITY_TRANSLATION} outgoing
   * activity will be dispatched once translation completes.
   *
   * <p>For every other operation type the call is forwarded to
   * {@link #sendApplicationToOutgoingApplicationActivities(String, String, CatalogStatus, String, String, String, String, String, String, String, String, String, OperationStatus, String,JsonObject)}
   * which dispatches the activity immediately.
   *
   * @param application The full application document.
   */
  public Future<Void> sendApplicationToOutgoingApplicationActivities(
      String collectionName,
      String applicationId,
      CatalogStatus catalogStatus,
      String catalogId,
      String status,
      String datasetId,
      String reason,
      String renewAmendFile,
      String rejectionReason,
      String rejectionFile,
      String revocationFile,
      String decisionFileBase64,
      OperationStatus operationStatus,
      String deadlineDate,
      JsonObject application) {

    if (operationStatus == OperationStatus.DECISION && application != null) {
      // Always build and send the DECISION activity immediately.
      // Embed any translatedFields already stored on the decision so the dispatcher gets them.
      JsonObject outgoingActivity = createOutgoingApplicationActivity(collectionName,
          applicationId, catalogStatus, catalogId, status, datasetId, operationStatus, reason, renewAmendFile,rejectionReason, rejectionFile, revocationFile,
        decisionFileBase64, deadlineDate);


      Promise<Void> promise = Promise.promise();
      mongoClient.save(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION, outgoingActivity, res -> {
        if (res.succeeded()) {
          log.debug(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Saved outgoing decision activity, id: {}", res.result());
          promise.complete();
        } else {
          log.error(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION
              + " Failed to save outgoing decision activity, error: {}",
              res.cause().getMessage());
          promise.fail(res.cause());
        }
      });

      // Additionally, if there is translatable content, kick off translation so that an
      // ACTIVITY_TRANSLATION is dispatched with the translated reason / file when done.
      if ((StringUtils.isNotEmpty(reason) || StringUtils.isNotEmpty(decisionFileBase64))
          && decisionTranslationService != null) {
        log.debug(
            "DECISION with translatable content for application {} – also triggering translation",
            applicationId);
        saveApplicationTranslationState(collectionName, application, catalogId, datasetId,
            OperationStatus.DECISION)
            .onFailure(err -> log.error(
                "Failed to save decision translation state for application {} dataset {}: {}",
                applicationId, datasetId, err.getMessage(), err));
      }

      return promise.future();
    }

    // For all other operation types fall through to the immediate-dispatch variant.
    return sendApplicationToOutgoingApplicationActivities(collectionName, applicationId,
        catalogStatus, catalogId, status, datasetId, reason, renewAmendFile,rejectionReason, rejectionFile, revocationFile, decisionFileBase64, operationStatus,
        deadlineDate);
  }

  protected Future<Void> saveApplicationTranslationState(String collectionName,
    JsonObject application, String catalogId, String datasetId, OperationStatus operationStatus) {

    if (decisionTranslationService == null || application == null) {
      return Future.succeededFuture();
    }

    JsonObject dataset = findDatasetByCatalogAndDatasetId(application, catalogId, datasetId);
    if (dataset == null) {
      return Future.succeededFuture();
    }

    return switch (operationStatus) {
      case DECISION -> saveDecisionTranslationState(collectionName, application,
        catalogId, datasetId, dataset);
      case PERMIT -> savePermitTranslationState(collectionName, application,
        catalogId, datasetId, dataset);
      default -> Future.succeededFuture();
    };
  }

  protected Future<Void> saveCatalogTranslationState(String collectionName, JsonObject application,
    String catalogId) {

    if (decisionTranslationService == null || application == null) {
      return Future.succeededFuture();
    }

    JsonObject catalog = findCatalogById(application.getJsonArray("catalogs", new JsonArray()),
      catalogId);
    if (catalog == null) {
      return Future.succeededFuture();
    }

    JsonObject latestStatus = getLastJsonObject(catalog.getJsonArray("statuses", new JsonArray()))
      .orElse(null);
    if (latestStatus == null) {
      return Future.succeededFuture();
    }

    JsonObject statusPayload = latestStatus.getJsonObject("statusPayload");
    if (statusPayload == null) {
      return Future.succeededFuture();
    }

    return decisionTranslationService
      .saveStatusTranslation(
        collectionName,
        application.getString(ID),
        application.getString(APPLICATION_ID, application.getString(ID)),
        catalogId,
        statusPayload,
        application.getString(INPUT_LANGUAGE, "en")
      )
      .mapEmpty();
  }

  private Future<Void> saveDecisionTranslationState(String collectionName,
    JsonObject application, String catalogId, String datasetId, JsonObject dataset) {

    JsonArray decisions = dataset.getJsonArray("decisions", new JsonArray());
    int itemIndex = findLatestItemIndex(decisions);
    if (itemIndex < 0) {
      return Future.succeededFuture();
    }

    JsonObject decision = decisions.getJsonObject(itemIndex);

    return decisionTranslationService.saveDecisionTranslation(collectionName,
      application.getString(ID), application.getString(APPLICATION_ID, application.getString(ID)),
      datasetId, itemIndex, decision, catalogId, application.getString(INPUT_LANGUAGE, "en") ).mapEmpty();
  }

  private Future<Void> savePermitTranslationState(String collectionName,
    JsonObject application, String catalogId, String datasetId, JsonObject dataset) {

    JsonArray permits = dataset.getJsonArray("permits", new JsonArray());
    int itemIndex = findLatestItemIndex(permits);
    if (itemIndex < 0) {
      return Future.succeededFuture();
    }

    JsonObject permit = permits.getJsonObject(itemIndex);

    // Translate for ALL languages – catalog-language filtering is applied at sync time.
    return decisionTranslationService.savePermitTranslation(collectionName,
      application.getString(ID), application.getString(APPLICATION_ID, application.getString(ID)),
      datasetId, itemIndex, permit, catalogId, application.getString(INPUT_LANGUAGE, "en") ).mapEmpty();
  }

  private JsonObject findDatasetByCatalogAndDatasetId(JsonObject application, String catalogId,
    String datasetId) {
    JsonArray datasets = application.getJsonArray(DATASETS, new JsonArray());
    for (Object datasetObj : datasets) {
      JsonObject dataset = (JsonObject) datasetObj;
      if (catalogId.equals(dataset.getString(CATALOG_ID))
        && datasetId.equals(dataset.getString(DATASET_ID))) {
        return dataset;
      }
    }
    return null;
  }

  private int findLatestItemIndex(JsonArray items) {
    for (int i = items.size() - 1; i >= 0; i--) {
      if (items.getJsonObject(i).getInteger("latest", 0) == 1) {
        return i;
      }
    }
    return items.isEmpty() ? -1 : items.size() - 1;
  }

  private JsonObject createOutgoingApplicationActivity(String collectionName, String applicationId,
    CatalogStatus catalogStatus, String catalogId,String reason, String renewAmendFile, String rejectionReason, String rejectionFile, String revocationFile, String deadlineDate,
    OperationStatus operationType) {
    return createOutgoingApplicationActivity(collectionName, applicationId,
      catalogStatus, catalogId, null, null, operationType, reason, renewAmendFile, rejectionReason, rejectionFile, revocationFile, null,
      deadlineDate);
  }

  private JsonObject createOutgoingApplicationActivity(
    String collectionName,
    String applicationId,
    CatalogStatus catalogStatus,
    String catalogId,
    String status,
    String datasetId,
    OperationStatus operationStatus,
    String reason,
    String renewAmendFile,
    String rejectionReason,
    String rejectionFile,
    String revocationFile,
    String decisionFileBase64,
    String deadlineDate) {

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
        if(catalogStatus.getLabel().equals("SUBMITTED")) {
          payload.put(DATE_DEADLINE, deadlineDate);
        }
        break;
      case DECISION:
        JsonObject decision = new JsonObject()
          .put("decision", status)
          .put("dataset_id", datasetId);

        if (StringUtils.isNotEmpty(reason)) {
          decision.put("reason", reason);
        }

        if (StringUtils.isNotEmpty(decisionFileBase64)) {
          decision.put("decision_file", decisionFileBase64);
        }

        payload.put("decisions", new JsonArray().add(decision));
        break;
      case PERMIT:
        JsonObject permit = new JsonObject()
          .put("dataset_id", datasetId)
          .put("reason", reason)
          .put(RENEW_AMEND_FILE, renewAmendFile)
          .put(REJECTION_REASON, rejectionReason)
          .put(REJECTION_FILE, rejectionFile)
          .put(REVOCATION_FILE, revocationFile)
          .put("permit_status", status);
        payload.put("permits", new JsonArray().add(permit));
        break;
      case ACTIVITY_RESPONSE:
        JsonObject activityResponseJson = new JsonObject();
        if(catalogStatus.equals(CatalogStatus.PROCESSING_EXTENDED)) {
          activityResponseJson.put(DATE_DEADLINE,deadlineDate);
        }
        else if (catalogStatus.equals(CatalogStatus.PROCESSING)) {
          activityResponseJson.put(DATE_EXTENSION_DEADLINE,deadlineDate);
        }
        payload.put("activityResponse",activityResponseJson);
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
    JsonObject body = context.body().asJsonObject();

    String title = body.getString(TITLE);
    String newInputLang = body.getString(INPUT_LANGUAGE);

    JsonObject query = new JsonObject()
      .put(TITLE, title)
      .put(ID, new JsonObject().put("$ne", id))
      .put(USER_ID, Context.getEcasId(context));

    mongoClient.findOne(database, query, null, asyncResult -> {
      if (asyncResult.failed()) {
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          asyncResult.cause().getMessage());
        return;
      }

      if (asyncResult.result() != null) {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "applicationTitleExists");
        return;
      }

      JsonObject queryUpdate = new JsonObject()
        .put(ID, id)
        .put(STATUS_STATUS, new JsonObject()
          .put("$not", new JsonObject()
            .put("$regex", "^SUBMITTED$")
            .put("$options", "i")
          )
        );

      mongoClient.findOne(database, new JsonObject().put(ID, id), null, appRes -> {
        if (appRes.failed() || appRes.result() == null) {
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND, "applicationNotFound");
          return;
        }

        JsonObject app = appRes.result();

        // Define which key/value arrays should be retranslated when input language changes
        List<KeyValueRemapSpec> specs = List.of(
          new KeyValueRemapSpec(
            "form.section2.purposeForWhichDataWillBeUsed",
            "section2.purposeForWhichDataWillBeUsed.items"
          )
        );

        JsonObject setObj = new JsonObject()
          .put(TITLE, body.getValue(TITLE))
          .put(INPUT_LANGUAGE, body.getValue(INPUT_LANGUAGE));

        // Generic remap for all configured fields
        JsonObject remappedSet = buildRemappedFieldsSetObject(context.vertx(), app, newInputLang,
          specs);
        for (String k : remappedSet.fieldNames()) {
          setObj.put(k, remappedSet.getValue(k));
        }

        JsonObject update = new JsonObject().put("$set", setObj);

        mongoClient.findOneAndUpdate(database, queryUpdate, update, res -> {
          if (res.succeeded()) {
            setUpMessageContextResponse(context, HttpResponseStatus.OK,
              "Updated Title, Input Language and translated key/value selections for application with id: "
                + id);
          } else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              "Did not update Title/Input Language/translations in DB for application with id: "
                + id);
          }
        });
      });
    });
  }


  protected void getApplicationsHbad(@NotNull RoutingContext context, String database) {
    JsonObject query = new JsonObject()
      .put(USER_ID, Context.getEcasId(context));

    JsonObject fields = new JsonObject().put(DATASETS_HDAB_NAME, 1);

    // Query all documents and collect distinct HDAB names
    mongoClient.findWithOptions(database, query, new FindOptions().setFields(fields), asyncResult -> {
      if (asyncResult.succeeded()) {
        List<String> distinctHdabs = new ArrayList<>();

        asyncResult.result().forEach(doc -> {
          // Each document may contain "datasets" -> array
          JsonArray datasets = doc.getJsonArray("datasets");
          if (datasets != null) {
            for (int i = 0; i < datasets.size(); i++) {
              JsonObject dataset = datasets.getJsonObject(i);
              if (dataset == null) {
                continue;
              }

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
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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

    JsonObject fields = new JsonObject().put(DATASETS_COUNTRY_LABEL, 1);

    // Get all documents and extract distinct country labels
    mongoClient.findWithOptions(database, query, new FindOptions().setFields(fields), asyncResult -> {
      if (asyncResult.succeeded()) {
        List<String> distinctCountries = new ArrayList<>();

        asyncResult.result().forEach(doc -> {
          JsonArray datasets = doc.getJsonArray("datasets");
          if (datasets != null) {
            for (int i = 0; i < datasets.size(); i++) {
              JsonObject dataset = datasets.getJsonObject(i);
              if (dataset == null) {
                continue;
              }

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
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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

    JsonObject fields = new JsonObject().put(TITLE, 1);

    // Get all documents and extract distinct country labels
    mongoClient.findWithOptions(database, query, new FindOptions().setFields(fields), asyncResult -> {
      if (asyncResult.succeeded()) {
        List<String> applicationTitles = new ArrayList<>();

        asyncResult.result().forEach(doc -> {
          String title = doc.getString(TITLE);
          if (title != null) {
            applicationTitles.add(title);
          }
        });

        // Respond with array of unique country labels
        JsonArray resultArray = new JsonArray(applicationTitles);
        context.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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
        log.info("Rollback successful: Status reverted to DRAFT for application: {}",
          applicationId);
      } else {
        log.info("Rollback failed for application : {} with message: {}", applicationId,
          ar.cause().getMessage());
      }
    });
  }

  public Future<JsonObject> replaceApplication(JsonObject mongoApplication, String collectionName) {
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
          log.error("{} Failed to replace application with id: {}", collectionName,
            mongoApplication.getValue(ID), result.cause());
          promise.fail(result.cause());
        }
      });
    return promise.future();
  }

  protected Future<JsonObject> updateApplication(String id, JsonObject updateApplication,
    String collectionName) {
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
              if (deleteApplicationDataset(mongoApplication, requestDatasetId, context,
                collectionName)) {
                applicationModified.set(true);
              }
            });
            if (applicationModified.get()) {

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

  /**
   * Sends an {@link NotificationCode#APPLICATION_DISCARDED_BY_OWNER} notification to every
   * accepted collaborator of the given application.
   *
   * <p>Called after an application is definitively deleted (UC 10.3 – Discard Draft, and
   * UC 10.5 – Remove Last Dataset/Distribution when it causes the application to be deleted).
   * The notification is fire-and-forget: failures are only logged.
   *
   * @param applicationId   the (now deleted) application identifier
   * @param applicationType the application type collection name
   * @param applicationName the human-readable application title
   */
  protected void notifyCollaboratorsOfDiscard(
    String applicationId,
    String applicationType,
    String applicationName
  ) {
    if (StringUtils.isBlank(applicationId)) {
      return;
    }

    JsonObject query = new JsonObject()
      .put(Constants.APPLICATION_ID, applicationId)
      .put(Constants.INVITATION_STATUS, Constants.INVITATION_ACCEPTED);

    mongoClient.find(Constants.APPLICATION_COLLABORATOR_COLLECTION, query)
      .onSuccess(collaborators -> {
        if (collaborators == null || collaborators.isEmpty()) {
          return;
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(NotificationKeys.APPLICATION_NAME, applicationName != null ? applicationName : "");
        for (JsonObject collab : collaborators) {
          String userId = collab.getString(Constants.COLLABORATOR_USER_ID);
          if (StringUtils.isNotBlank(userId)) {
            notificationManager.createNotifications(
                NotificationCode.APPLICATION_DISCARDED_BY_OWNER,
                applicationId,
                applicationType,
                properties,
                userId)
              .onFailure(err ->
                log.error(
                  "Failed to send APPLICATION_DISCARDED_BY_OWNER notification for applicationId={} userId={}",
                  applicationId, userId, err)
              );
          }
        }
      })
      .onFailure(err ->
        log.error(
          "Failed to fetch collaborators for discard notification, applicationId={}",
          applicationId, err)
      );
  }

  boolean deleteApplicationDataset(JsonObject mongoApplication, String requestDatasetId,
    @NotNull RoutingContext context, String collectionName) {

    AuditLogActionDescription auditLogActionDescriptionDiscarded;
    AuditLogObject auditLogObject;
    AuditLogAction auditLogActionDiscarded;
    AuditLogActionDescription auditLogActionDescriptionDeleted;

    if (DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)) {
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

          // UC 10.5 – notify accepted collaborators that the application was discarded
          notifyCollaboratorsOfDiscard(
            mongoApplication.getString(ID),
            collectionName,
            mongoApplication.getString(TITLE));

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
            DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
              ? AuditLogAction.DATA_ACCESS_APPLICATION_DELETED.toString()
              : AuditLogAction.DATA_REQUEST_APPLICATION_DELETED.toString(),
            auditLogActionDescription, false, context);

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

    if (DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)) {
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

                // UC 10.5 – notify accepted collaborators that the application was discarded
                notifyCollaboratorsOfDiscard(
                  mongoApplication.getString(ID),
                  collectionName,
                  mongoApplication.getString(TITLE));

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

                // If dataset removed, remove section 6 too.
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

  protected void putDistributionsApplication(@NotNull RoutingContext context,
    String collectionName) {

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
      requestApplication -> futures.add(
        updateApplicationAsync(requestApplication, context, collectionName)));

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
                log.info(
                  "{} Distribution with id: {} was found in the Mongo database, so will not add it",
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

  private Future<JsonObject> findApplication(Object datasetQueryDocument, String userId,
    String collectionName) {

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
   * Extracts and returns the set of target languages from the given list of datasets by querying
   * their catalogues.
   * <p>
   * For each dataset, this method extracts the {@code catalog_id}, fetches the corresponding
   * catalogue via {@code hubSearchClient}, and collects all {@code language.id} values found in the
   * catalogue's "language" array. Language codes are returned in uppercase (e.g., "EN", "DE").
   * <p>
   * If no catalogue IDs are found, an empty set is returned immediately. If a catalogue is missing
   * or does not contain a "language" field, a warning is logged.
   *
   * @param datasets List of dataset JSON objects, each expected to contain a {@code catalog_id}
   *                 field.
   * @return A {@code Future<Set<String>>} that resolves with the set of unique target language
   * codes.
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
            log.warn("Catalogue does not contain language field: {}",
              catalogueWrapper.encodePrettily());
          }
        }

        return targetLanguages;
      });
  }

  /**
   * Triggers a form translation request and persists translation metadata to enable retry
   * mechanism.
   * <p>
   * <b>Important Design Pattern:</b> This method follows the same pattern as file translations -
   * creating the translation object in the database BEFORE making the translation request to the
   * external service. This ensures reliable retry mechanism even when the translation service is
   * unavailable.
   * </p>
   *
   * <h3>Why Create Translation Object Before Request?</h3>
   * <ul>
   *   <li><b>Service Availability:</b> Translation service may be down, experiencing DNS issues, or unreachable</li>
   *   <li><b>Network Issues:</b> Request may fail due to network problems</li>
   *   <li><b>Automatic Recovery:</b> Scheduler can retry without manual intervention</li>
   *   <li><b>Audit Trail:</b> All translation attempts are tracked in the database</li>
   * </ul>
   *
   * <h3>Execution Flow:</h3>
   * <ol>
   *   <li>Build form translation payload with all form fields and target languages</li>
   *   <li>Mark form as PENDING in TranslationResult object (includes createdAt, attempts=0)</li>
   *   <li><b>Persist translation metadata to DB with PENDING status</b> (enables retry)</li>
   *   <li>Make translation request to hub-translation service</li>
   *   <li>If request fails, translation remains PENDING for scheduler to retry</li>
   * </ol>
   *
   * <h3>Translation Object Structure Created:</h3>
   * <pre>
   * {
   *   "translation": {
   *     "status": "FORM_PENDING",
   *     "sourceLanguage": "EN",
   *     "targetLanguages": ["DE", "FR"],
   *     "form": {
   *       "status": "PENDING",
   *       "payload": { ...form fields... },
   *       "createdAt": "2026-01-27T13:00:00Z",    // Used by scheduler for retry timing
   *       "attempts": 0                            // Tracks retry count
   *     }
   *   }
   * }
   * </pre>
   *
   * <h3>Retry Mechanism:</h3>
   * The scheduler queries for applications where:
   * <ul>
   *   <li><code>translation.form.status = PENDING</code></li>
   *   <li><code>translation.form.createdAt <= (now - retryInterval)</code></li>
   * </ul>
   * Retries occur every 5 minutes (configurable) up to 3 attempts (configurable).
   * After max attempts, translation is marked FAILED with audit log.
   *
   * <h3>Difference from File Translation:</h3>
   * <ul>
   *   <li><b>Files:</b> translation.files.{fileId} nested object per file</li>
   *   <li><b>Form:</b> translation.form flat object (only one form per application)</li>
   *   <li><b>Both:</b> Use same retry mechanism and timing logic</li>
   * </ul>
   *
   * @param applicationId          The unique identifier of the application
   * @param form                   The original application form JSON to translate
   * @param applicationType        The MongoDB collection name (e.g., "data_access_applications")
   * @param sourceLang             The source language code (e.g., "EN")
   * @param targetLanguages        List of target language codes (e.g., ["DE", "FR", "ES"])
   * @param englishFormTranslation English form translation to preserve (if exists)
   * @param changeTargetLanguages  Whether to update target languages in translation object
   * @see eu.europa.data.health.access.scheduled_jobs.TranslationRetryScheduledJob
   * @see #triggerDocumentTranslationRequest(String, String, List, String, String)
   * @see eu.europa.data.health.access.hub_translation_client.TranslationResult
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

    // Mark form translation as PENDING with initial metadata
    // This creates: { status: "PENDING", payload: {...}, createdAt: "...", attempts: 0 }
    result.markFormPending(formPayload);

    // CRITICAL: Create translation metadata BEFORE making the request
    // This follows the same pattern as file translations - see triggerDocumentTranslationRequest()
    //
    // Why persist before request?
    //   1. Translation service may be unavailable (DNS resolution, service down, network issues)
    //   2. If we create object AFTER request, failed requests = no DB object = no retries
    //   3. With object persisted BEFORE, scheduler can discover and retry automatically
    //
    // Scheduler will query for:
    //   - translation.form.status = PENDING
    //   - translation.form.createdAt <= (now - retryInterval)
    //
    // This enables automatic retries every 5 minutes up to 3 attempts
    JsonObject update = result.toMongoUpdate(sourceLang, targetLanguages, englishFormTranslation,
      changeTargetLanguages);
    log.info(
      "TriggerFormTranslationRequest: Persisting translation metadata for applicationId={}, update={}",
      applicationId, update.encodePrettily());

    // Step 1: Persist translation metadata to database
    mongoClient.updateCollection(applicationType, new JsonObject().put("_id", applicationId),
        update)
      .onSuccess(v -> {
        log.info(
          "TriggerFormTranslationRequest: Translation metadata persisted successfully for applicationId={}",
          applicationId);

        // Step 2: Now make the translation request to external service
        // If this fails, the PENDING object remains in DB for scheduler to retry
        hubTranslationClient.requestTranslation(formPayload)
          .onSuccess(id -> {
            log.info("Form translation triggered successfully for applicationId={}", applicationId);
            // Translation service accepted the request
            // When translation completes, callback will update status to COMPLETED
          })
          .onFailure(err -> {
            log.error("Form translation request failed for applicationId={}, err={}", applicationId,
              err.getMessage());
            // Translation object already exists with PENDING status
            // Scheduler will automatically retry based on translation.form.createdAt timestamp
            // Log failure once for audit trail (prevents duplicate audit logs)
            logTranslationFailureOnce(applicationId, applicationType,
              "Failed to request form translation");
          });
      })
      .onFailure(err -> {
        log.error("Failed to persist translation metadata for applicationId={}, error={}",
          applicationId, err.getMessage(), err);
        logTranslationFailureOnce(applicationId, applicationType,
          "Failed to persist translation metadata");
      });
  }

  /**
   * Builds the translated forms for each target language and sets the overall translation status.
   *
   * <p>This method uses the translation dictionary to create deep copies of the original form with
   * translated values applied per language. It then compares the languages received against the
   * expected target languages to determine the translation completion status.
   *
   * <p>If any expected language is missing from the translations, the status is set to
   * {@link TranslationStatus#PARTIALLY_COMPLETED}; otherwise, it is set to
   * {@link TranslationStatus#COMPLETED}.
   *
   * @param originalForm  The original application form JSON object.
   * @param dict          Translation dictionary containing translated values organized by section
   *                      and field.
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
    JsonObject formTranslations = hubTranslationClient.buildFormTranslations(originalForm, dict,
      translatedFiles, applicationType, formTrans);

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
   * in the application. The translation status of the form is updated
   * {@code translation.form.status}
   *
   * @param application       The original application JsonObject to be updated.
   * @param translationUpdate The translation update JsonObject containing form translations and
   *                          status.
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
   * Updates an application's translation metadata and form_translations to reflect new translated
   * file versions.
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
   * @param uploadedFiles  A map of language codes to file metadata for the translated files. Each
   *                       value must include "fileId" and "fileName".
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

  protected Future<JsonObject> createApplicantDetails(@NotNull RoutingContext context) {

    Promise<JsonObject> promise = Promise.promise();

    JsonObject applicantDetails = new JsonObject();
    String ecasId = Context.getEcasId(context);
    applicantDetails.put("ecasId", ecasId);

    authClient.getAllUsers(context, true)
      .onSuccess(users -> {
        Optional<JsonObject> matchingUser = users.stream()
          .map(obj -> (JsonObject) obj)
          .filter(user -> ecasId.equals(user.getString("ecasId")))
          .findFirst();

        if (matchingUser.isPresent()) {
          applicantDetails.put("firstName", matchingUser.get().getString("firstName"));
          applicantDetails.put("lastName", matchingUser.get().getString("lastName"));
        }
        promise.complete(applicantDetails);
      })
      .onFailure(
        err -> promise.fail("Could not retrieve users, with error message: " + err.getMessage()));

    return promise.future();
  }

  protected void markTranslationError(String applicationId, String collectionName, String errorType,
    String fileId) {
    JsonObject query = new JsonObject().put(ID, applicationId);
    JsonObject setObject = new JsonObject()
      .put(TRANSLATION_STATUS, TranslationStatus.FAILED)
      .put(STATUS_STATUS, DRAFT)
      .put(TRANSLATION_ERROR_TYPE, errorType)
      .put(DATE_MODIFIED, Instant.now());

    if (fileId != null) {
      // Mark specific file as FAILED - only if it's still PENDING
      query.put(TRANSLATION_FILES + fileId + ".status", "PENDING");
      setObject.put(TRANSLATION_FILES + fileId + ".status", TranslationStatus.FAILED.name());
    } else {
      // Mark the form as FAILED
      setObject.put("translation.form.status", TranslationStatus.FAILED.name());
    }

    JsonObject update = new JsonObject().put("$set", setObject);

    mongoClient.findOneAndUpdate(collectionName, query, update, res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          log.info("Marked translation error [{}] for application [{}] in [{}]", errorType,
            applicationId, collectionName);
          logTranslationFailureOnce(applicationId, collectionName, errorType);
        } else {
          log.error(
            "Failed to mark translation error for application [{}] in [{}] - document not found or query did not match. Query: {}",
            applicationId, collectionName, query.encode());
        }
      } else {
        log.error("Failed to mark translation error for application [{}] in [{}]", applicationId,
          collectionName, res.cause());
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
      path = TRANSLATION_FILES + fileId + ".";
      // Ensure the file is still marked as PENDING, in case another attempt succeeded in the meantime
      query.put(TRANSLATION_FILES + fileId + ".status", TranslationStatus.PENDING.name());
    } else {
      path = "translation.form.";
    }

    // Set lastRetryAt timestamp for tracking retry attempts
    setObject.put(path + "lastRetryAt", Instant.now().toString());

    // Build the update to increment attempts by 1
    JsonObject update = new JsonObject()
      .put("$set", setObject)
      .put("$inc", new JsonObject().put(path + "attempts", 1));

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
   * Determines the audit action and object based on the collection type (Data Access or Data
   * Request), and logs a warning if the collection type is unknown.
   *
   * @param applicationId  the ID of the application whose translation failed
   * @param collectionName the collection where the application resides (e.g.,
   *                       DATA_ACCESS_APPLICATION_COLLECTION)
   * @param title          the application title, used in the audit log description
   * @param errorType      a description of the error that caused the translation failure
   */
  private void createTranslationFailedAuditLog(String applicationId, String collectionName,
    String title, String errorType) {
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
   * This method checks whether a translation failure audit log has already been created for the
   * current submission of the application by comparing the timestamp of the
   * "translation.logged_failure" flag with the application's "date_submitted". If no prior log
   * exists for the current submission, it creates a translation failure audit log and sets the
   * "translation.logged_failure" flag with the current timestamp.
   * <p>
   * This ensures that repeated failures for the same submission do not generate duplicate audit log
   * entries.
   *
   * @param applicationId  the ID of the application for which the translation failed
   * @param collectionName the MongoDB collection name where the application is stored
   * @param errorType      a descriptive string indicating the type or context of the failure
   */
  private void logTranslationFailureOnce(String applicationId, String collectionName,
    String errorType) {
    mongoClient.findOne(collectionName, new JsonObject().put(ID, applicationId), null)
      .onSuccess(application -> {
        if (application == null) {
          log.warn("Could not fetch application [{}] for audit log", applicationId);
          return;
        }

        JsonObject translation = application.getJsonObject(TRANSLATION, new JsonObject());
        String loggedFailureStr = translation.getString(LOGGED_FAILURE, null);
        String dateSubmittedStr = application.getString(DATE_SUBMITTED);

        if (dateSubmittedStr == null) {
          log.warn(
            "Application [{}] has no date_submitted field, cannot check for duplicate audit log",
            applicationId);
          // Continue to create the audit log even without date_submitted check
        } else {
          try {
            Instant dateSubmitted = Instant.parse(dateSubmittedStr);

            if (loggedFailureStr != null) {
              Instant loggedFailure = Instant.parse(loggedFailureStr);
              if (loggedFailure.isAfter(dateSubmitted)) {
                log.info("Skipping duplicate translation failed audit log for application [{}]",
                  applicationId);
                return;
              }
            }
          } catch (Exception e) {
            log.warn("Failed to parse date_submitted [{}] for application [{}]: {}",
              dateSubmittedStr, applicationId, e.getMessage());
            // Continue to create the audit log even if date parsing fails
          }
        }

        String title = application.getString(TITLE, "");
        createTranslationFailedAuditLog(applicationId, collectionName, title, errorType);

        JsonObject updateFlag = new JsonObject()
          .put("$set",
            new JsonObject().put("translation.logged_failure", Instant.now().toString()));

        mongoClient.updateCollection(collectionName, new JsonObject().put(ID, applicationId),
            updateFlag)
          .onSuccess(v -> log.info("Set translation.logged_failure flag for [{}]", applicationId))
          .onFailure(
            err -> log.error("Failed to set logged_failure flag for [{}]: {}", applicationId,
              err.getMessage()));
      })
      .onFailure(
        err -> log.error("Failed to fetch application [{}] for audit log", applicationId, err));
  }

  protected String createAuditLogActionDescriptionForChangeHDABStatus(
    CatalogStatus targetCatalogStatus, String collectionName, String applicationId,
    String applicationTitle, String catalogId) {

    return auditLogManager.prepareAuditLogActionDescription(
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_HDAB_UPDATED.toString()
        : AuditLogActionDescription.DATA_REQUEST_APPLICATION_HDAB_UPDATED.toString(),
      applicationId, applicationTitle, targetCatalogStatus.getLabel(), catalogId);
  }

  private String createAuditLogActionDescriptionForChangeHDABAndDecisionStatus(
    CatalogStatus targetCatalogStatus, String collectionName, String applicationId,
    String applicationTitle, String catalogId, String currentDatasetStatus,
    DatasetStatus targetDatasetStatus, String datasetId) {

    if (currentDatasetStatus == null) {
      return auditLogManager.prepareAuditLogActionDescription(
        DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
          ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_HDAB_AND_DECISION_UPDATED.toString()
          : AuditLogActionDescription.DATA_REQUEST_APPLICATION_HDAB_AND_DECISION_UPDATED.toString(),
        applicationId,
        applicationTitle, targetCatalogStatus.getLabel(), catalogId, targetDatasetStatus.getLabel(),
        datasetId);
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
    String applicationId, String applicationTitle, ApplicationStatus targetApplicationStatus,
    String collectionName) {

    return auditLogManager.prepareAuditLogActionDescription(
      DATA_ACCESS_APPLICATION_COLLECTION.equalsIgnoreCase(collectionName)
        ? AuditLogActionDescription.DATA_ACCESS_APPLICATION_UPDATED.toString()
        : AuditLogActionDescription.DATA_REQUEST_APPLICATION_UPDATED.toString(),
      applicationId,
      applicationTitle, targetApplicationStatus.getStatus());

  }

  public String createAuditLogActionDescriptionForChangePermitStatus(
    String applicationId, String applicationTitle, PermitStatus targetPermitStatus,
    String catalogId, String datasetId) {

    return auditLogManager.prepareAuditLogActionDescription(
      AuditLogActionDescription.DATA_ACCESS_APPLICATION_PERMIT_UPDATED.toString(), applicationId,
      applicationTitle, catalogId, datasetId, targetPermitStatus.getLabel());

  }

  protected void insertAuditLogForApplicationUpdate(String auditLogActionDescription,
    String collectionName, String applicationId, RoutingContext context) {

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

  protected void insertAuditLogForHDABUpdate(String auditLogActionDescription,
    String collectionName, String applicationId, RoutingContext context) {

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

  private void insertAuditLogForDatasetUpdate(String auditLogActionDescription,
    String collectionName, String applicationId, RoutingContext context) {

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

  public void insertAuditLogForPermitUpdate(String auditLogActionDescription, String applicationId,
    RoutingContext context) {

    auditLogManager.insertAuditLog(
      AuditLogObject.DATA_ACCESS_APPLICATION.toString(),
      applicationId,
      AuditLogAction.DATA_ACCESS_PERMIT_STATUS_UPDATED.toString(),
      auditLogActionDescription, true, context);
  }


  protected JsonObject getDefaultEnglishFormTranslation(String sourceLang,
    JsonObject mongoApplication) {

    JsonObject formTranslations = new JsonObject();

    if ("EN".equalsIgnoreCase(sourceLang)) {
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
    String renewAmendFile = context.body().asJsonObject().getString(RENEW_AMEND_FILE);
    String rejectionFile = context.body().asJsonObject().getString(REJECTION_FILE);
    String rejectionReason = context.body().asJsonObject().getString(REJECTION_REASON);
    String revocationFile = context.body().asJsonObject().getString(REVOCATION_FILE);

    changePermitStatus(applicationId, catalogId, datasetId, permitStatusString, comment, renewAmendFile, rejectionReason, rejectionFile,  revocationFile,  context);

  }

  public void updatePermitStatus(JsonObject application, String catalogId, String datasetId,
    PermitStatus permitStatus, String permitFile, String renewAmendFile, String rejectionReason, String rejectionFile, String revocationFile, String permitStartDate,
    String permitExpirationDate,
    String permitReferenceNumber, String permitContactDetails, String reason,
    JsonObject translatedFields) {
    JsonArray datasets = application.getJsonArray("datasets");
    modifyDatasetBasedOnDispatcherPermit(datasets, catalogId, datasetId, permitStatus, permitFile, renewAmendFile, rejectionReason, rejectionFile,  revocationFile,
      permitStartDate, permitExpirationDate, permitReferenceNumber, permitContactDetails, reason,
      translatedFields);
  }

  private void modifyDatasetBasedOnDispatcherPermit(
    JsonArray mongoDatasets,
    String catalogId,
    String datasetId,
    PermitStatus permitStatus,
    String permitFile,
    String renewAmendFile,
    String rejectionReason,
    String rejectionFile,
    String revocationFile,
    String permitStartDate,
    String permitExpirationDate,
    String permitReferenceNumber,
    String permitContactDetails,
    String reason, JsonObject translatedFields
  ) {

    for (Object mongoDatasetObj : mongoDatasets) {

      JsonObject mongoDataset = (JsonObject) mongoDatasetObj;

      String mongoCatalogId = mongoDataset.getString("catalog_id");
      String mongoDatasetId = mongoDataset.getString("dataset_id");
      JsonArray permits = mongoDataset.getJsonArray("permits");

      if (catalogId.equals(mongoCatalogId) && datasetId.equalsIgnoreCase(mongoDatasetId)) {

        String previousStartDate = null;
        String previousExpirationDate = null;
        String previousReferenceNumber = null;
        String previousContactDetails = null;

        if (permits == null) {
          permits = new JsonArray();
        } else {
          for (Object item : permits) {
            JsonObject existingPermit = (JsonObject) item;

            if (existingPermit.getInteger("latest", 0) == 1) {
              previousStartDate = existingPermit.getString("permit_start_date");
              previousExpirationDate = existingPermit.getString("permit_expiration_date");
              previousReferenceNumber = existingPermit.getString("permit_reference_number");
              previousContactDetails = existingPermit.getString("permit_contact_details");
            }

            existingPermit.put("latest", 0);
          }
        }

        JsonObject permit = new JsonObject();
        permit.put("permit_received", Instant.now());
        permit.put("latest", 1);

        if (permitFile != null) {
          permit.put("permit_file", permitFile);
        }
        if (renewAmendFile != null) {
          permit.put(RENEW_AMEND_FILE, renewAmendFile);
        }
        if (rejectionFile != null) {
          permit.put(REJECTION_FILE, rejectionFile);
        }
        if (revocationFile != null) {
          permit.put(REVOCATION_FILE, revocationFile);
        }
        if (rejectionReason != null) {
          permit.put(REJECTION_REASON, rejectionReason);
        }

        if (permitStatus != null) {
          permit.put("permit_status", permitStatus.getLabel());
        }

        if (reason != null) {
          permit.put("reason", reason);
        }

        if (translatedFields != null && !translatedFields.isEmpty()) {
          permit.put("translatedFields", translatedFields);
        }

        if (permitStatus == PermitStatus.GRANTED) {
          permit.put("permit_start_date", permitStartDate);
          permit.put("permit_reference_number", permitReferenceNumber);
          permit.put("permit_contact_details", permitContactDetails);
        }

        if (permitStatus == PermitStatus.RENEWED) {
          permit.put("permit_start_date", previousStartDate);
          permit.put("permit_reference_number", previousReferenceNumber);
          permit.put("permit_contact_details", previousContactDetails);
        }

        // 🔑 expiration date logic
        if (permitStatus == PermitStatus.GRANTED || permitStatus == PermitStatus.RENEWED) {
          if (permitExpirationDate != null) {
            // optionally validate/parse before storing
            permit.put("permit_expiration_date", permitExpirationDate);
          }
        } else {
          permit.put("permit_expiration_date", previousExpirationDate);
          permit.put("permit_start_date", previousStartDate);
          permit.put("permit_reference_number", previousReferenceNumber);
          permit.put("permit_contact_details", previousContactDetails);

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

  protected Future<Void> changeCatalogToSubmitted(
    JsonObject application,
    String catalogId,
    String collectionName
  ) {
    Promise<Void> promise = Promise.promise();
    String applicationId = application.getString("_id");

    // 1. Remove dispatcher_status from the SENT status entry
    JsonObject catalog = findCatalogById(application, catalogId);
    if (catalog != null) {
      removeDispatcherStatus(catalog);
    }

    // 2. Update catalog status to SUBMITTED
    String newDeadline = updateCatalogStatus(application, catalogId, CatalogStatus.SUBMITTED);
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
        catalogId,null,null, null, null,
        null, newDeadline, OperationStatus.STATUS_UPDATE);

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
      promise.complete();
    }).onFailure(err -> {
      log.error("Failed to save application in changeCatalogToSubmitted: {}", err.getMessage());
      promise.fail(err);
    });

    return promise.future();
  }

  protected Future<Void> changeCatalogToWithdrawn(
    JsonObject application,
    String catalogId,
    String collectionName
  ) {
    Promise<Void> promise = Promise.promise();
    String applicationId = application.getString("_id");

    updateCatalogStatus(application, catalogId, CatalogStatus.WITHDRAWN);

    ApplicationStatus currentApplicationStatus = ApplicationStatus.fromStatus(
      application.getJsonObject("status").getString("status"));
    if (ApplicationStatus.ON_GOING.equals(currentApplicationStatus)
      && currentApplicationStatus.allLatestStatusesAreInCompletedStatus(
      application.getJsonArray("catalogs", new JsonArray()))) {
      updateApplicationStatus(
        application,
        new JsonObject().put("status", ApplicationStatus.COMPLETED.getStatus()),
        null
      );

      insertAuditLogForApplicationUpdate(
        createAuditLogActionDescriptionForApplicationUpdate(
          applicationId,
          application.getString("title"),
          ApplicationStatus.COMPLETED,
          collectionName
        ),
        collectionName,
        applicationId,
        null
      );
    }

    saveApplication(application, collectionName).onSuccess(v -> {
      sendApplicationToOutgoingApplicationActivities(
        collectionName,
        applicationId,
        CatalogStatus.WITHDRAWN,
        catalogId,
        null,
        null,
        null,
        null,
        null,
        null,
        OperationStatus.STATUS_UPDATE
      );

      insertAuditLogForHDABUpdate(
        createAuditLogActionDescriptionForChangeHDABStatus(
          CatalogStatus.WITHDRAWN,
          collectionName,
          applicationId,
          application.getString("title"),
          catalogId
        ),
        collectionName,
        applicationId,
        null
      );
      promise.complete();
    }).onFailure(err -> {
      log.error("Failed to save application in changeCatalogToWithdrawn: {}",
        err.getMessage());
      promise.fail(err);
    });

    return promise.future();
  }

  protected String extractAdditionalInformationRequestingDate(JsonObject statusPayload) {
    if (statusPayload == null || statusPayload.isEmpty()) {
      return null;
    }

    String flatRequestingDate = statusPayload.getString("requestingDate");
    if (StringUtils.isNotBlank(flatRequestingDate)) {
      return flatRequestingDate;
    }

    return statusPayload.getJsonObject(ADDITIONAL_INFORMATION, new JsonObject())
      .getString("requestingDate");
  }

  private String computeCatalogDeadline(JsonObject application, String existingDeadlineStr,
    Integer expirationDays, CatalogStatus status, String additionInfoDateStr) {
    Instant newDeadline;
    if (application.getInteger("version", 1) > 1 && CatalogStatus.SUBMITTED.equals(status)) {
      if (existingDeadlineStr != null) {
        Instant existingDeadline = Instant.parse(existingDeadlineStr);
        long stuckInAdditionalInfoDays = 0;
        if (StringUtils.isNotEmpty(additionInfoDateStr)){
          Instant additionInfoDate = Instant.parse(additionInfoDateStr);
          stuckInAdditionalInfoDays = java.time.temporal.ChronoUnit.DAYS.between(additionInfoDate
            , Instant.now());
        }
        newDeadline = existingDeadline
          .plus(stuckInAdditionalInfoDays, java.time.temporal.ChronoUnit.DAYS);
      } else {
        newDeadline = Instant.now().plus(expirationDays, java.time.temporal.ChronoUnit.DAYS);
      }
    } else if (CatalogStatus.PROCESSING_EXTENDED.equals(status)) {
      Instant existingDeadline = Instant.parse(existingDeadlineStr);
      newDeadline = existingDeadline
        .plus(expirationDays, java.time.temporal.ChronoUnit.DAYS);
    } else {
      newDeadline = Instant.now().plus(expirationDays, java.time.temporal.ChronoUnit.DAYS);
    }
    return newDeadline.toString();
  }


  private String computeCatalogExtensionDeadline( String existingDeadlineStr,
    Integer expirationDays) {
    Instant existingDeadline = Instant.parse(existingDeadlineStr);
    return existingDeadline.plus(expirationDays, java.time.temporal.ChronoUnit.DAYS).toString();
  }



  private JsonObject findCatalogById(JsonObject app, String catalogId) {
    JsonArray catalogsArray = app.getJsonArray("catalogs", new JsonArray());
    return findCatalogById(catalogsArray, catalogId);
  }

  private JsonObject findCatalogById(JsonArray catalogs, String catalogId) {
    if (catalogs == null || catalogId == null) {
      return null;
    }
    for (Object catalogObject : catalogs) {
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

  protected void triggerRemainingTranslations(String applicationId, JsonObject mongoApplication,
    String collectionName) {
    // disable additional translations if needed
    boolean enableTranslation = Boolean.parseBoolean(config.getString(Constants.ENABLE_TRANSLATION, "false"));
    if (!enableTranslation) {
      return;
    }
    try {
      String sourceLang = mongoApplication
        .getString(INPUT_LANGUAGE, "EN")
        .toUpperCase(Locale.ENGLISH);

      JsonObject translation = mongoApplication.getJsonObject(TRANSLATION);
      JsonArray targetLanguages =
        translation != null ? translation.getJsonArray(TARGET_LANGUAGES) : null;
      if (targetLanguages == null) {
        log.warn(
          "No translation.targetLanguages found; skipping remaining-language translations for {}",
          applicationId);
        return;
      }

      // For file translations: all supported languages minus the source language.
      // Files must be translated into every supported language so that form translations
      // (which reference translated files) can find them regardless of target language.
      List<String> fileTranslationLanguages = SUPPORTED_LANGUAGES_UPPER.stream()
        .filter(lang -> !lang.equals(sourceLang))
        .toList();

      // For form translations: supported languages minus source AND already-targeted languages.
      // These are triggered later, after file translations complete.
      Set<String> excludedFormLanguages = targetLanguages
        .stream()
        .map(Object::toString)
        .map(s -> s.toUpperCase(Locale.ENGLISH))
        .collect(Collectors.toSet());
      excludedFormLanguages.add(sourceLang);

      List<String> remainingFormLanguages = SUPPORTED_LANGUAGES_UPPER.stream()
        .filter(lang -> !excludedFormLanguages.contains(lang))
        .toList();

      if (remainingFormLanguages.isEmpty()) {
        log.debug("No remaining form languages to translate for {}", applicationId);
        return;
      }

      JsonObject form = mongoApplication.getJsonObject("form");
      List<String> documentIds = ApplicationFormUtils.extractDocumentIds(form);

      if (!documentIds.isEmpty()) {
        // Set the pendingFileTranslations flag so that when file translations complete,
        // form translations are triggered automatically.
        updatePendingFileTranslationsFlag(collectionName, applicationId, true);

        // Trigger file translations first; form translations follow once all files are done.
        // preserveExistingTranslations=true ensures existing form_translations are not wiped.
        log.info(
          "Triggering remaining-language file translations for {} documents in application {} (fileLanguages={})",
          documentIds.size(), applicationId, fileTranslationLanguages.size()
        );
        for (String documentId : documentIds) {
          triggerDocumentTranslationRequest(
            applicationId,
            collectionName,
            fileTranslationLanguages,
            sourceLang,
            documentId,
            true
          );
        }
      } else {
        // No files to translate — skip straight to form translations.
        log.info(
          "No documents in form for {}; triggering remaining-language form translations directly (count={}).",
          applicationId, remainingFormLanguages.size()
        );
        updatePendingFileTranslationsFlag(collectionName, applicationId, false);
        triggerFormTranslationRequest(
          applicationId,
          form,
          collectionName,
          sourceLang,
          remainingFormLanguages,
          null,
          false
        );
      }

      log.info("triggerRemainingTranslations initiated for {}.", applicationId);
    } catch (Exception e) {
      log.warn("Failed to trigger remaining-language translations for {}: {}", applicationId,
        e.getMessage());
    }
  }

  /**
   * Triggers form translations for the remaining languages after file translations have completed.
   * <p>
   * Called from {@code processFileTranslationCallback} once all pending file translations finish
   * (i.e. when {@code translation.pendingFileTranslations} is {@code true} and overall status
   * reaches {@code FILES_COMPLETED} or {@code COMPLETED}).
   * <p>
   * The remaining form languages are all supported languages minus the source language and minus
   * any languages that were already targeted before the remaining-translations flow started.
   *
   * @param applicationId  The ID of the application.
   * @param application    The full application object (fresh from DB).
   * @param collectionName The MongoDB collection name.
   */
  protected void triggerRemainingFormTranslations(
    String applicationId,
    JsonObject application,
    String collectionName
  ) {
    try {
      String sourceLang = application.getString(INPUT_LANGUAGE, "EN").toUpperCase(Locale.ENGLISH);

      Set<String> excludedLanguages = application
        .getJsonObject(FORM_TRANSLATIONS, new JsonObject())
        .fieldNames()
        .stream()
        .map(s -> s.toUpperCase(Locale.ENGLISH))
        .collect(Collectors.toSet());
      excludedLanguages.add(sourceLang);

      List<String> remainingLanguages = SUPPORTED_LANGUAGES_UPPER.stream()
        .filter(lang -> !excludedLanguages.contains(lang))
        .toList();

      if (remainingLanguages.isEmpty()) {
        log.debug(
          "No remaining form languages to translate for {}; clearing pendingFileTranslations flag.",
          applicationId
        );
        updatePendingFileTranslationsFlag(collectionName, applicationId, false);
        return;
      }
      triggerFormTranslationRequest(
        applicationId,
        application.getJsonObject("form"),
        collectionName,
        sourceLang,
        remainingLanguages,
        null,
        false
      );

      log.info(
        "Triggering remaining-language form translations for {} (count={}) after file translations completed.",
        applicationId,
        remainingLanguages.size()
      );
    } catch (Exception e) {
      log.warn("Failed to trigger remaining form translations for {}:", applicationId, e);
    }
  }

  private void updatePendingFileTranslationsFlag(String collection, String applicationId,
    boolean value) {
    mongoClient.updateCollection(
      collection,
      new JsonObject().put("_id", applicationId),
      new JsonObject().put("$set",
        new JsonObject().put(TRANSLATION_PENDING_FILE_TRANSLATIONS, value))
    ).onFailure(err ->
      log.warn("Failed to update pendingFileTranslations flag for {}:", applicationId, err)
    );
  }

  protected void addNotificationsForMultipleCatalogsPerApplication(
    NotificationCode notificationCode, JsonObject mongoApplication, String applicationType) {

    JsonArray datasets = mongoApplication.getJsonArray("datasets");
    if (datasets == null) {
      return;
    }

    Set<String> processedCatalogIds = new HashSet<>();

    datasets.forEach(datasetObject -> {
      JsonObject dataset = (JsonObject) datasetObject;
      JsonObject hdab = dataset.getJsonObject("hdab");

      if (hdab == null) {
        return;
      }

      String catalogId = dataset.getString("catalog_id");

      if (catalogId == null || !processedCatalogIds.add(catalogId)) {
        return;
      }

      String hdabName = hdab.getString("name");
      Map<String, Object> properties = new HashMap<>();
      properties.put(NotificationKeys.APPLICATION_NAME, mongoApplication.getString(TITLE));
      properties.put(NotificationKeys.HDAB_NAME, hdabName);

      notificationManager.createNotifications(notificationCode, mongoApplication.getString(ID), applicationType, properties, mongoApplication.getString("userId"))
        .onFailure(err ->
          log.error(
            "Failed to create notification {} for catalogId={} applicationId={}",
            notificationCode,
            catalogId,
            mongoApplication.getString(ID),
            err
          )
      );
    });
  }

  private void addNotificationForChangeCatalogStatus(JsonObject application,
    CatalogStatus targetCatalogStatus,
    String catalogId,
    String applicationType) {

    Map<String, Object> properties = new HashMap<>();
    properties.put(NotificationKeys.APPLICATION_NAME, application.getString(TITLE));
    properties.put(NotificationKeys.HDAB_NAME, findHdabNameByCatalogId(application, catalogId));

    String applicationId = application.getString(ID);
    String userId = application.getString("userId");

    NotificationCode code = switch (targetCatalogStatus) {
      case REJECTED -> NotificationCode.CATALOG_REJECTED;
      case ADDITIONAL_INFORMATION -> NotificationCode.CATALOG_AWAIT_ADDITIONAL_INFORMATION;
      case PROCESSING -> NotificationCode.CATALOG_PROCESSING;
      case PROCESSING_EXTENDED -> NotificationCode.CATALOG_PROCESSING_EXTENDED;
      case WITHDRAWN -> NotificationCode.CATALOG_WITHDRAW;
      default -> null;
    };

    if (code == null) return;

    notificationManager
      .createNotifications(code, applicationId, applicationType, properties, userId)
      .onFailure(err ->
        log.error(
          "Failed to create notification {} for applicationId={}, userId={}, catalogId={}, targetStatus={}, applicationType={}",
          code,
          applicationId,
          userId,
          catalogId,
          targetCatalogStatus,
          applicationType,
          err
        )
      );
  }

  private void addNotificationForChangeDatasetStatus(JsonObject application,
    DatasetStatus targetDatasetStatus,
    String datasetId,
    String applicationType) {

    Map<String, Object> properties = new HashMap<>();
    properties.put(NotificationKeys.APPLICATION_NAME, application.getString(TITLE));
    properties.put(NotificationKeys.DATASET_ID, datasetId);

    // Find the dataset title from the datasets array using the datasetId
    JsonObject datasetTitle = findDatasetTitleByDatasetId(application, datasetId);
    if (datasetTitle != null) {
      properties.put("DATASET_TITLE", datasetTitle);
      properties.put(NotificationKeys.DATASET_NAME, extractDatasetName(datasetTitle, datasetId));
    } else {
      properties.put(NotificationKeys.DATASET_NAME, datasetId);
    }

    String applicationId = application.getString(ID);
    String userId = application.getString("userId");

    NotificationCode code = switch (targetDatasetStatus) {
      case POSITIVE_DECISION_ISSUED -> NotificationCode.DATASET_POSITIVE_DECISION_ISSUED;
      case NEGATIVE_DECISION_ISSUED -> NotificationCode.DATASET_NEGATIVE_DECISION_ISSUED;
      case APPEAL_REJECTED -> NotificationCode.DATASET_APPEAL_REJECTED;
      default -> null;
    };

    if (code == null) return;

    notificationManager
      .createNotifications(code, applicationId, applicationType, properties, userId)
      .onFailure(err ->
        log.error(
          "Failed to create notification {} for applicationId={}, userId={}, datasetId={}, targetDatasetStatus={}, applicationType={}",
          code,
          applicationId,
          userId,
          datasetId,
          targetDatasetStatus,
          applicationType,
          err
        )
      );
  }

  private void addNotificationForChangePermitStatus(JsonObject application,
    PermitStatus targetPermitStatus,
    String datasetId) {

    Map<String, Object> properties = new HashMap<>();
    properties.put(NotificationKeys.APPLICATION_NAME, application.getString(TITLE));
    properties.put(NotificationKeys.DATASET_ID, datasetId);

    JsonObject datasetTitle = findDatasetTitleByDatasetId(application, datasetId);
    if (datasetTitle != null) {
      properties.put("DATASET_TITLE", datasetTitle);
      properties.put(NotificationKeys.DATASET_NAME, extractDatasetName(datasetTitle, datasetId));
    } else {
      properties.put(NotificationKeys.DATASET_NAME, datasetId);
    }

    String applicationId = application.getString(ID);
    String userId = application.getString("userId");

    NotificationCode code = switch (targetPermitStatus) {
      case GRANTED -> NotificationCode.PERMIT_GRANTED;
      case RENEWED -> NotificationCode.PERMIT_RENEWED;
      case RENEWAL_REJECTED -> NotificationCode.PERMIT_RENEWAL_REJECTED;
      case AMENDED -> NotificationCode.PERMIT_AMENDED;
      case AMENDMENT_REJECTED -> NotificationCode.PERMIT_AMENDMENT_REJECTED;
      case EXPIRED -> NotificationCode.PERMIT_EXPIRED;
      case REVOKED -> NotificationCode.PERMIT_REVOKED;
      case APPEAL_APPROVED -> NotificationCode.PERMIT_APPEAL_APPROVED;
      case APPEAL_REJECTED -> NotificationCode.PERMIT_APPEAL_REJECTED;
      default -> null;
    };

    if (code == null) return;

    notificationManager
      .createNotifications(code, applicationId, DATA_ACCESS_APPLICATION_COLLECTION, properties, userId)
      .onFailure(err ->
        log.error(
          "Failed to create notification {} for applicationId={}, userId={}, datasetId={}, targetPermitStatus={}",
          code,
          applicationId,
          userId,
          datasetId,
          targetPermitStatus,
          err
        )
      );
  }

  private String findHdabNameByCatalogId(JsonObject application, String catalogId) {

    if (application == null || catalogId == null) {
      return null;
    }

    JsonArray datasets = application.getJsonArray("datasets", new JsonArray());
    if (datasets == null) {
      return null;
    }

    for (Object obj : datasets) {
      JsonObject dataset = (JsonObject) obj;

      if (catalogId.equals(dataset.getString("catalog_id"))) {

        JsonObject hdab = dataset.getJsonObject("hdab");
        if (hdab != null) {
          return hdab.getString("name");
        }

        return null; // dataset found but no hdab
      }
    }
    return null; // no dataset matched
  }

  private JsonObject findDatasetTitleByDatasetId(JsonObject application, String datasetId) {

    if (application == null || datasetId == null) {
      return null;
    }

    JsonArray datasets = application.getJsonArray("datasets", new JsonArray());
    if (datasets == null) {
      return null;
    }

    for (Object obj : datasets) {
      JsonObject dataset = (JsonObject) obj;

      if (datasetId.equals(dataset.getString("dataset_id"))) {
        return dataset.getJsonObject(TITLE); // returns the full title object with all translations
      }
    }
    return null; // no dataset matched
  }

  /**
   * Extracts a display name from a multilingual dataset title object. Prefers English ("en"), falls
   * back to the first available value, and ultimately to the datasetId if nothing is found.
   */
  private String extractDatasetName(JsonObject datasetTitle, String fallback) {
    if (datasetTitle == null || datasetTitle.isEmpty()) {
      return fallback;
    }
    String enTitle = datasetTitle.getString("en");
    if (enTitle != null && !enTitle.isBlank()) {
      return enTitle;
    }
    // Return the first non-blank value
    for (String key : datasetTitle.fieldNames()) {
      Object val = datasetTitle.getValue(key);
      if (val instanceof String s && !s.isBlank()) {
        return s;
      }
    }
    return fallback;
  }

  private String normalizeLang(String lang) {
    if (lang == null || lang.isBlank()) {
      return "en";
    }
    return lang.trim().toLowerCase();
  }

  private JsonArray getJsonArrayByPath(JsonObject root, String dottedPath, JsonArray defaultValue) {
    if (root == null || dottedPath == null || dottedPath.isBlank()) {
      return defaultValue;
    }

    Object current = root;
    String[] parts = dottedPath.split("\\.");

    for (String part : parts) {
      if (!(current instanceof JsonObject)) {
        return defaultValue;
      }
      current = ((JsonObject) current).getValue(part);
      if (current == null) {
        return defaultValue;
      }
    }

    return (current instanceof JsonArray) ? (JsonArray) current : defaultValue;
  }

  private Optional<JsonObject> getLastJsonObject(JsonArray array) {
    if (array == null || array.isEmpty()) {
      return Optional.empty();
    }

    Object lastItem = array.getValue(array.size() - 1);
    return lastItem instanceof JsonObject ? Optional.of((JsonObject) lastItem) : Optional.empty();
  }

  private void putJsonArrayByPath(JsonObject root, String dottedPath, JsonArray value) {
    // Creates intermediate objects if missing
    String[] parts = dottedPath.split("\\.");
    JsonObject current = root;

    for (int i = 0; i < parts.length - 1; i++) {
      String p = parts[i];
      JsonObject next = current.getJsonObject(p);
      if (next == null) {
        next = new JsonObject();
        current.put(p, next);
      }
      current = next;
    }
    current.put(parts[parts.length - 1], value);
  }

  private Map<String, String> buildKeyValueMapFromItems(JsonArray items) {
    Map<String, String> map = new HashMap<>();
    if (items == null) {
      return map;
    }

    for (int i = 0; i < items.size(); i++) {
      JsonObject it = items.getJsonObject(i);
      if (it == null) {
        continue;
      }

      String k = it.getString(KEY);
      String v = it.getString(VALUE);
      if (k != null && v != null) {
        map.put(k, v);
      }
    }
    return map;
  }

  private JsonArray remapSelectedKeyValueArray(JsonArray selected, Map<String, String> keyToValue) {
    JsonArray out = new JsonArray();
    if (selected == null) {
      return out;
    }

    for (int i = 0; i < selected.size(); i++) {
      JsonObject sel = selected.getJsonObject(i);
      if (sel == null) {
        continue;
      }

      String k = sel.getString(KEY);
      String oldV = sel.getString(VALUE);

      String newV = (k != null && keyToValue != null && keyToValue.containsKey(k))
        ? keyToValue.get(k)
        : oldV;

      out.add(new JsonObject().put(KEY, k).put(VALUE, newV));
    }
    return out;
  }

  private JsonObject loadTranslationsRoot(Vertx vertx, String lang) {
    // If your files are under src/main/resources/translations/*.json,
    // this relative path works in Vert.x when running from project root.
    // If not, adapt to your deployment packaging.
    String path = "translations/" + lang + ".json";
    String jsonStr = vertx.fileSystem().readFileBlocking(path).toString();
    return new JsonObject(jsonStr);
  }

  private JsonObject buildRemappedFieldsSetObject(
    Vertx vertx,
    JsonObject applicationDoc,
    String newInputLang,
    List<KeyValueRemapSpec> specs
  ) {
    JsonObject setObj = new JsonObject();

    Map<String, Map<String, String>> cacheByItemsPath = new HashMap<>();

    JsonObject translationsRoot;
    try {
      translationsRoot = loadTranslationsRoot(vertx, normalizeLang(newInputLang));
    } catch (Exception e) {
      // If translations missing, do nothing (keep existing values)
      return setObj;
    }

    for (KeyValueRemapSpec spec : specs) {
      // 1) selected array from application
      JsonArray selected = getJsonArrayByPath(applicationDoc, spec.selectedPathInApplication,
        new JsonArray());

      // 2) items array from translations, then map key->value (cached)
      Map<String, String> keyToValue = cacheByItemsPath.computeIfAbsent(
        spec.itemsPathInTranslations, p -> {
          JsonArray items = getJsonArrayByPath(translationsRoot, p, new JsonArray());
          return buildKeyValueMapFromItems(items);
        });

      // 3) remap selected and add to $set
      JsonArray remapped = remapSelectedKeyValueArray(selected, keyToValue);
      setObj.put(spec.selectedPathInApplication, remapped);
    }

    return setObj;
  }
}
