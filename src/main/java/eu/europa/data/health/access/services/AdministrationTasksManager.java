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

import static eu.europa.data.health.access.Constants.APPLICATION_COLLABORATOR_COLLECTION;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE_FIELD;
import static eu.europa.data.health.access.Constants.CONFIGURATION_RETRIES;
import static eu.europa.data.health.access.Constants.CONFIGURATION_STATUS;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_TIME_FORMAT;
import static eu.europa.data.health.access.Constants.DESC;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.FAILED;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.LIMIT;
import static eu.europa.data.health.access.Constants.OWNERSHIP_REQUEST_COLLECTION;
import static eu.europa.data.health.access.Constants.PAGE;
import static eu.europa.data.health.access.Constants.PAGINATION_LIMIT;
import static eu.europa.data.health.access.Constants.PENDING;
import static eu.europa.data.health.access.Constants.ROLE;
import static eu.europa.data.health.access.Constants.ROLE_OWNER;
import static eu.europa.data.health.access.Constants.SORT_BY;
import static eu.europa.data.health.access.Constants.SORT_ORDER;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.TITLE;

import eu.europa.data.health.access.dto.SearchCriteriaDto;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdministrationTasksManager extends AbstractManager{

  public static final String DISPATCHER_RECORD_WITH_ID = "Dispatcher record with id: ";
  private static final String SEARCH_TERM = "searchTerm";
  private static final String APPLICATION_TYPE = "applicationType";
  private static final String OWNERSHIP_FILTER = "ownershipFilter";
  private static final String HAS_OWNERSHIP_REQUESTS = "hasOwnershipRequests";
  private static final String HAS_COLLABORATORS = "hasCollaborators";
  private static final String APPLICATION_COLLECTION = "applicationCollection";
  private static final String REGEX_OPERATOR = "$regex";
  private static final String OPTIONS_OPERATOR = "$options";
  private static final String CASE_INSENSITIVE = "i";
  private static final String FILTER_ALL = "all";
  private static final String FILTER_PENDING = "pending";
  private static final String FILTER_NO_PENDING = "no-pending";
  private static final String DATA_ACCESS = "DATA_ACCESS";
  private static final String DATA_REQUEST = "DATA_REQUEST";
  private static final String STATUS_FIELD = "status.status";
  private static final int INVALID_PAGINATION_VALUE = -1;
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final MongoClient mongoClient;

  public AdministrationTasksManager(DBHandler dbHandler, MongoClient mongoClient) {
    super(dbHandler);
    this.mongoClient = mongoClient;
  }


  public void retrySubmitAction(@NotNull RoutingContext context){

    String applicationId = context.pathParam(APPLICATION_ID);
    JsonObject query = new JsonObject().put("application._id", applicationId);
    mongoClient.findOne(DISPATCHER_COLLECTION, query, null)
      .onSuccess(result -> {

        if(result == null){
          log.error(
            DISPATCHER_COLLECTION + " Dispatcher record with id: {} not found", applicationId);
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            DISPATCHER_RECORD_WITH_ID + applicationId + " not found");
        } else {
          JsonObject configuration = result.getJsonObject("configuration");
          String status = configuration.getString("status");
          if(Strings.CI.equals(status, FAILED)){

            String dispatcherRecordId = result.getString("_id");

            JsonObject resultObjectId = new JsonObject().put("_id", dispatcherRecordId);
            JsonObject update = new JsonObject().put("$set", new JsonObject()
              .put(CONFIGURATION_STATUS, PENDING)
              .put(CONFIGURATION_RETRIES, 0)
              .put("configuration.resubmission_date", getCurrentTimestamp()));

            mongoClient.updateCollection(DISPATCHER_COLLECTION, resultObjectId, update, res -> {
              if (res.succeeded()) {
                log.info(DISPATCHER_COLLECTION
                  + " Application dispatcher record : {} resubmitted", dispatcherRecordId);
                setUpMessageContextResponse(context, HttpResponseStatus.OK,
                  DISPATCHER_RECORD_WITH_ID + applicationId + " has been resubmitted");
              } else {
                log.error(DISPATCHER_COLLECTION
                  + " Failed to update application dispatcher record for retry, with error message {}", res.cause().getMessage());
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Internal server error occurred with message:" + res.cause().getMessage());
              }
            });

          } else {
            log.info(DISPATCHER_COLLECTION
              + "Provided application {} is not in failed status", applicationId);
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              DISPATCHER_RECORD_WITH_ID + applicationId + " must be in failed status");
          }
        }
      })
      .onFailure(fail -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage()));
  }

  public void getAllDataAccessApplications(RoutingContext context){
    getAllDataApplications(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getAllDataRequestApplications(@NotNull RoutingContext context) {
    getAllDataApplications(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  public void searchAllApplications(@NotNull RoutingContext context) {
    String pageString = context.request().getParam(PAGE);
    String limitString = context.request().getParam(LIMIT);
    Integer pageNumber = parsePaginationParam(pageString);
    Integer pageLimit = parsePaginationParam(limitString);

    if (isInvalidPagination(pageNumber, pageLimit)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Both page and limit must be positive integers when pagination is requested");
      return;
    }

    String sortBy = StringUtils.isNotBlank(context.request().getParam(SORT_BY))
      ? context.request().getParam(SORT_BY)
      : DATE_CREATED;
    String sortOrder = StringUtils.isNotBlank(context.request().getParam(SORT_ORDER))
      ? context.request().getParam(SORT_ORDER)
      : DESC;
    String status = context.request().getParam(STATUS);

    JsonObject searchPayload = getSearchPayload(context);
    String searchValue = getSearchValue(context, searchPayload);
    String ownershipFilter = resolveOwnershipFilter(searchPayload.getString(OWNERSHIP_FILTER));

    if (ownershipFilter == null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "ownershipFilter must be one of: all, pending, no-pending");
      return;
    }

    JsonObject accessQueryOptions = buildSearchQuery(
      status,
      searchValue,
      DATA_ACCESS_APPLICATION_COLLECTION
    );
    JsonObject requestQueryOptions = buildSearchQuery(
      status,
      searchValue,
      DATA_REQUEST_APPLICATION_COLLECTION
    );
    Integer effectiveLimit = pageLimit != null ? Math.min(pageLimit, PAGINATION_LIMIT) : null;
    boolean paginatedRequest = pageNumber != null && effectiveLimit != null;

    Future<List<JsonObject>> accessApplicationsFuture = fetchApplications(
      DATA_ACCESS_APPLICATION_COLLECTION,
      DATA_ACCESS,
      accessQueryOptions
    );
    Future<List<JsonObject>> requestApplicationsFuture = fetchApplications(
      DATA_REQUEST_APPLICATION_COLLECTION,
      DATA_REQUEST,
      requestQueryOptions
    );
    Future<Integer> totalDocumentsFuture = paginatedRequest
      ? Future.all(
          dbHandler.countAll(DATA_ACCESS_APPLICATION_COLLECTION, new JsonObject()),
          dbHandler.countAll(DATA_REQUEST_APPLICATION_COLLECTION, new JsonObject()))
        .map(results -> castCount(results.resultAt(0)) + castCount(results.resultAt(1)))
      : Future.succeededFuture(null);

    Future.all(accessApplicationsFuture, requestApplicationsFuture, totalDocumentsFuture)
      .compose(results -> {
        List<JsonObject> applications = mergeApplications(
          castApplications(results.resultAt(0)),
          castApplications(results.resultAt(1)));
        Integer totalDocuments = castCount(results.resultAt(2));

        return Future.all(
          enrichApplicationsWithOwnershipRequests(applications),
          enrichApplicationsWithCollaborators(applications)
        ).map(ignored -> new Object[] { applications, totalDocuments });
      })
      .onSuccess(payload -> {
        List<JsonObject> applications = castApplications(payload[0]);
        Integer totalDocuments = castCount(payload[1]);

        List<JsonObject> filteredApplications = applyOwnershipFilter(applications, ownershipFilter);
        sortApplications(filteredApplications, sortBy, sortOrder);

        int totalFilteredDocuments = filteredApplications.size();
        List<JsonObject> responseData = sanitizeApplications(
          paginateApplications(filteredApplications, pageNumber, effectiveLimit));

        JsonObject response = paginatedRequest
          ? prepareGetEntityResponse(responseData,
          new SearchCriteriaDto(totalDocuments, totalFilteredDocuments, pageNumber,
            effectiveLimit, sortBy, sortOrder, status))
          : prepareGetEntityResponse(responseData,
          new SearchCriteriaDto(null, null, null, null, sortBy, sortOrder, status));

        enrichMetadata(response, searchValue, ownershipFilter);
        setUpContextResponse(context, HttpResponseStatus.OK, response.encode());
      })
      .onFailure(error -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, error.getMessage()));
  }

  private void getAllDataApplications(@NotNull RoutingContext context, String collection){
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam(PAGE);
    String limitString = context.request().getParam(LIMIT);
    String sortByString = context.request().getParam(SORT_BY);
    String sortOrderString = context.request().getParam(SORT_ORDER);
    String status = context.request().getParam(STATUS);

    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    super.getAllRecordsByAdmin(context, collection, pageSize, pageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : DESC,
      status);
  }

  private JsonObject getSearchPayload(RoutingContext context) {
    RequestBody requestBody = context.body();
    if (requestBody == null || requestBody.asJsonObject() == null) {
      return new JsonObject();
    }
    return requestBody.asJsonObject();
  }

  private String getSearchValue(RoutingContext context, JsonObject searchPayload) {
    String searchValue = searchPayload.getString(SEARCH_TERM);
    if (StringUtils.isBlank(searchValue)) {
      searchValue = searchPayload.getString(TITLE);
    }
    if (StringUtils.isBlank(searchValue)) {
      searchValue = context.request().getParam(SEARCH_TERM);
    }
    return StringUtils.trimToNull(searchValue);
  }

  private String resolveOwnershipFilter(String requestedOwnershipFilter) {
    String normalizedFilter = StringUtils.trimToNull(requestedOwnershipFilter);
    if (normalizedFilter == null) {
      return FILTER_ALL;
    }

    normalizedFilter = normalizedFilter.toLowerCase(Locale.ROOT);
    if (FILTER_ALL.equals(normalizedFilter)
      || FILTER_PENDING.equals(normalizedFilter)
      || FILTER_NO_PENDING.equals(normalizedFilter)) {
      return normalizedFilter;
    }
    return null;
  }

  private JsonObject buildSearchQuery(String status, String searchValue, String collectionName) {
    JsonObject queryOptions = new JsonObject();
    if (StringUtils.isNotBlank(searchValue) && !matchesCollection(searchValue, collectionName)) {
      String escapedValue = escapeForMongoRegex(searchValue);
      if (searchValue.length() <= 3) {
        JsonArray orFilters = new JsonArray()
          .add(
            new JsonObject().put(
              TITLE,
              new JsonObject()
                .put(REGEX_OPERATOR, escapedValue)
                .put(OPTIONS_OPERATOR, CASE_INSENSITIVE)
            )
          )
          .add(
            new JsonObject().put(
              ID,
              new JsonObject()
                .put(REGEX_OPERATOR, escapedValue + "$")
                .put(OPTIONS_OPERATOR, CASE_INSENSITIVE)
            )
          );
        queryOptions.put("$or", orFilters);
      } else {
        queryOptions.put(
          TITLE,
          new JsonObject().put(REGEX_OPERATOR, escapedValue).put(OPTIONS_OPERATOR, CASE_INSENSITIVE)
        );
      }
    }

    JsonObject statusFilter = createStatusFilter(status);
    if (!statusFilter.isEmpty()) {
      queryOptions.put(STATUS_FIELD, statusFilter);
    }

    return queryOptions;
  }

  /**
   * Returns {@code true} when the user-supplied search value, after being
   * uppercased and having spaces replaced by underscores, is a substring of
   * the given collection name. This lets users type the application type
   * label they see in the UI (e.g. "Data Access") and have the corresponding
   * collection returned without a title/reference-number filter.
   */
  private boolean matchesCollection(String searchValue, String collectionName) {
    if (StringUtils.isBlank(searchValue) || StringUtils.isBlank(collectionName)) {
      return false;
    }
    String normalizedSearch = searchValue.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    return collectionName.contains(normalizedSearch);
  }

  /**
   * Escapes regex metacharacters so the user's raw search value is treated as
   * a literal string by MongoDB's {@code $regex} operator. Avoids using
   * {@link java.util.regex.Pattern#quote(String)} so the resulting expression
   * stays portable with DocumentDB (which does not support every PCRE
   * extension Java's regex engine recognises).
   */
  private String escapeForMongoRegex(String value) {
    return value.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
  }

  private JsonObject createStatusFilter(String status) {
    JsonObject statusFilter = new JsonObject();
    if (StringUtils.isNotBlank(status)) {
      if (status.contains(",")) {
        statusFilter.put("$in", new JsonArray(Arrays.asList(status.split(","))));
      } else {
        statusFilter.put("$eq", status);
      }
    }
    return statusFilter;
  }

  private Future<List<JsonObject>> fetchApplications(String collection, String applicationType,
    JsonObject queryOptions) {

    JsonObject filterOptionFields = new JsonObject()
      .put("title", 1)
      .put(STATUS_FIELD, 1)
      .put("userId", 1)
      .put(DATE_CREATED, 1);

    return dbHandler.findEntities(collection, null, null, null, null, queryOptions, filterOptionFields)
      .map(results -> {
        for (JsonObject result : results) {
          result.put(APPLICATION_TYPE, applicationType);
          result.put(APPLICATION_COLLECTION, collection);
        }
        return results;
      });
  }

  private List<JsonObject> mergeApplications(List<JsonObject> accessApplications,
    List<JsonObject> requestApplications) {
    List<JsonObject> mergedApplications = new ArrayList<>(
      accessApplications.size() + requestApplications.size());
    mergedApplications.addAll(accessApplications);
    mergedApplications.addAll(requestApplications);
    return mergedApplications;
  }

  private void sortApplications(List<JsonObject> applications, String sortBy, String sortOrder) {
    String normalizedSortBy = normalizeSortBy(sortBy);
    applications.sort((left, right) -> {
      int comparison = compareValues(getNestedValue(left, normalizedSortBy),
        getNestedValue(right, normalizedSortBy));
      if (!Strings.CI.equals(sortOrder, "asc")) {
        comparison = comparison * -1;
      }
      if (comparison == 0) {
        comparison = compareValues(getNestedValue(left, TITLE), getNestedValue(right, TITLE));
      }
      if (comparison == 0) {
        comparison = compareValues(getNestedValue(left, ID), getNestedValue(right, ID));
      }
      return comparison;
    });
  }

  private String normalizeSortBy(String sortBy) {
    return Strings.CI.equals(sortBy, STATUS) ? STATUS_FIELD : sortBy;
  }

  private Object getNestedValue(JsonObject jsonObject, String fieldPath) {
    Object currentValue = jsonObject;
    for (String field : fieldPath.split("\\.")) {
      if (!(currentValue instanceof JsonObject)) {
        return null;
      }
      currentValue = ((JsonObject) currentValue).getValue(field);
    }
    return currentValue;
  }

  private int compareValues(Object leftValue, Object rightValue) {
    if (leftValue == null && rightValue == null) {
      return 0;
    }
    if (leftValue == null) {
      return 1;
    }
    if (rightValue == null) {
      return -1;
    }
    return leftValue.toString().compareToIgnoreCase(rightValue.toString());
  }

  private List<JsonObject> paginateApplications(List<JsonObject> applications, Integer pageNumber,
    Integer pageLimit) {
    if (pageNumber == null || pageLimit == null) {
      return applications;
    }

    int fromIndex = (pageNumber - 1) * pageLimit;
    if (fromIndex >= applications.size()) {
      return new ArrayList<>();
    }

    int toIndex = Math.min(fromIndex + pageLimit, applications.size());
    return new ArrayList<>(applications.subList(fromIndex, toIndex));
  }

  private Future<List<JsonObject>> enrichApplicationsWithOwnershipRequests(
    List<JsonObject> applications) {
    if (applications.isEmpty()) {
      return Future.succeededFuture(applications);
    }

    LinkedHashSet<String> applicationIds = new LinkedHashSet<>();
    JsonArray applicationCollections = new JsonArray()
      .add(DATA_ACCESS_APPLICATION_COLLECTION)
      .add(DATA_REQUEST_APPLICATION_COLLECTION);

    for (JsonObject application : applications) {
      String applicationId = application.getString(ID);
      if (StringUtils.isNotBlank(applicationId)) {
        applicationIds.add(applicationId);
      }
    }

    JsonObject ownershipQuery = new JsonObject()
      .put(APPLICATION_ID, new JsonObject().put("$in", new JsonArray(new ArrayList<>(applicationIds))))
      .put(APPLICATION_TYPE_FIELD, new JsonObject().put("$in", applicationCollections));

    return mongoClient.find(OWNERSHIP_REQUEST_COLLECTION, ownershipQuery)
      .map(ownershipRequests -> {
        Set<String> applicationsWithOwnershipRequests = new java.util.HashSet<>();
        for (JsonObject ownershipRequest : ownershipRequests) {
          applicationsWithOwnershipRequests.add(toOwnershipKey(
            ownershipRequest.getString(APPLICATION_TYPE_FIELD),
            ownershipRequest.getString(APPLICATION_ID)));
        }

        for (JsonObject application : applications) {
          boolean hasOwnershipRequests = applicationsWithOwnershipRequests.contains(
            toOwnershipKey(application.getString(APPLICATION_COLLECTION),
              application.getString(ID)));
          application.put(HAS_OWNERSHIP_REQUESTS, hasOwnershipRequests);
        }
        return applications;
      });
  }

  private String toOwnershipKey(String applicationCollection, String applicationId) {
    return StringUtils.defaultString(applicationCollection) + ":" + StringUtils.defaultString(applicationId);
  }

  private Future<List<JsonObject>> enrichApplicationsWithCollaborators(
    List<JsonObject> applications
  ) {
    if (applications.isEmpty()) {
      return Future.succeededFuture(applications);
    }

    LinkedHashSet<String> applicationIds = new LinkedHashSet<>();
    for (JsonObject application : applications) {
      String applicationId = application.getString(ID);
      if (StringUtils.isNotBlank(applicationId)) {
        applicationIds.add(applicationId);
      }
    }

    if (applicationIds.isEmpty()) {
      for (JsonObject application : applications) {
        application.put(HAS_COLLABORATORS, false);
      }
      return Future.succeededFuture(applications);
    }

    JsonObject collaboratorQuery = new JsonObject()
      .put(
        APPLICATION_ID,
        new JsonObject().put("$in", new JsonArray(new ArrayList<>(applicationIds)))
      )
      .put(ROLE, new JsonObject().put("$ne", ROLE_OWNER));

    return mongoClient
      .find(APPLICATION_COLLABORATOR_COLLECTION, collaboratorQuery)
      .map(collaborators -> {
        Set<String> applicationsWithCollaborators = new java.util.HashSet<>();
        for (JsonObject collaborator : collaborators) {
          applicationsWithCollaborators.add(
            toOwnershipKey(
              collaborator.getString(APPLICATION_TYPE_FIELD),
              collaborator.getString(APPLICATION_ID)
            )
          );
        }

        for (JsonObject application : applications) {
          boolean hasCollaborators = applicationsWithCollaborators.contains(
            toOwnershipKey(application.getString(APPLICATION_COLLECTION), application.getString(ID))
          );
          application.put(HAS_COLLABORATORS, hasCollaborators);
        }
        return applications;
      });
  }

  private List<JsonObject> applyOwnershipFilter(List<JsonObject> applications, String ownershipFilter) {
    if (FILTER_ALL.equals(ownershipFilter)) {
      return applications;
    }

    List<JsonObject> filteredApplications = new ArrayList<>();
    boolean expectedOwnershipState = FILTER_PENDING.equals(ownershipFilter);
    for (JsonObject application : applications) {
      if (application.getBoolean(HAS_OWNERSHIP_REQUESTS, false) == expectedOwnershipState) {
        filteredApplications.add(application);
      }
    }
    return filteredApplications;
  }

  private List<JsonObject> sanitizeApplications(List<JsonObject> applications) {
    for (JsonObject application : applications) {
      application.remove(APPLICATION_COLLECTION);
    }
    return applications;
  }

  private void enrichMetadata(JsonObject response, String searchValue, String ownershipFilter) {
    JsonObject metadata = response.getJsonObject("metadata");
    if (metadata == null) {
      metadata = new JsonObject();
      response.put("metadata", metadata);
    }

    if (StringUtils.isNotBlank(searchValue)) {
      metadata.put(SEARCH_TERM, searchValue);
    }
    metadata.put(OWNERSHIP_FILTER, ownershipFilter);
  }

  private Integer parsePaginationParam(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      log.warn("Invalid pagination parameter received: {}", value);
      return INVALID_PAGINATION_VALUE;
    }
  }

  private boolean isInvalidPagination(Integer pageNumber, Integer pageLimit) {
    return (pageNumber == null) != (pageLimit == null)
      || (pageNumber != null && pageNumber < 1)
      || (pageLimit != null && pageLimit < 1);
  }

  @SuppressWarnings("unchecked")
  private List<JsonObject> castApplications(Object result) {
    return (List<JsonObject>) result;
  }

  private Integer castCount(Object result) {
    return (Integer) result;
  }

  private String getCurrentTimestamp(){
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    return now.format(formatter);
  }
}
