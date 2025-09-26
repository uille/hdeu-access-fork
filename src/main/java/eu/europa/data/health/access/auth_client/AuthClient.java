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
package eu.europa.data.health.access.auth_client;

import static eu.europa.data.health.access.Constants.DATA_ADMIN;
import static eu.europa.data.health.access.Constants.MAX_ALLOWED_CHARS_50;
import static eu.europa.data.health.access.Constants.USER_ID;

import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.services.AuditLogManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthClient {

  public static final String USERNAME = "username";
  public static final String FIRST_NAME = "firstName";
  public static final String LAST_NAME = "lastName";
  public static final String ROLES = "roles";
  public static final String CREATED_TIMESTAMP = "createdTimestamp";
  public static final String LAST_LOGIN = "lastLogin";
  public static final String ERROR_WHEN_RETRIEVING_USERS = " Error when retrieving users";
  private final String url;
  WebClient vertxClient;
  @Setter
  private AuditLogManager auditLogManager;

  public AuthClient(String url, WebClient client) {
    this.url = url;
    this.vertxClient = client;
  }

  private final Logger log = LoggerFactory.getLogger(getClass());
  public static final String AUTH_CLIENT = "AUTH_CLIENT";
  public static final String SEARCH_USER_REQUEST = "/users";

  /**
   * Creates the headers for the request to the auth client.
   *
   * @param context the routing context
   * @return the headers
   */
  public MultiMap createHeader(RoutingContext context) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Cookie", "authToken=" + context.user().principal().getString("access_token"));
    return headers;
  }

  /**
   * Retrieves all users from the auth client.
   *
   * @param context the routing context
   * @param includeUserDetails Whether to include detailed user information such as roles, login events, and metadata.
   * @return a future containing the list of users
   */
  public Future<JsonArray> getAllUsers(RoutingContext context, boolean includeUserDetails) {
    Promise<JsonArray> promise = Promise.promise();

    MultiMap headers = createHeader(context);

    vertxClient.getAbs(this.url + SEARCH_USER_REQUEST)
      .putHeaders(headers)
      .addQueryParam("includeUserDetails", String.valueOf(includeUserDetails))
      .send(ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.OK.code() == ar.result().statusCode()) {
            log.info(AUTH_CLIENT + " Successfully retrieved users");

            JsonArray users = new JsonArray();
            ar.result().bodyAsJsonArray().forEach(user -> {
                JsonObject userObj = (JsonObject) user;
                JsonObject userToStore = new JsonObject();
                userToStore.put("ecasId", userObj.getString(USERNAME));
                userToStore.put(FIRST_NAME, userObj.getString(FIRST_NAME));
                userToStore.put(LAST_NAME, userObj.getString(LAST_NAME));
                users.add(userToStore);
              }
            );
            promise.complete(users);
          } else {
            log.error(AUTH_CLIENT + " Failed to retrieve users");
            promise.fail("Failed to retrieve users with status: " + ar.result().statusCode());
          }
        } else {
          log.error(AUTH_CLIENT + ERROR_WHEN_RETRIEVING_USERS, ar.cause());
          promise.fail(ar.cause());
        }
      });
    return promise.future();
  }

  /**
   * Searches for users based on the provided criteria.
   *
   * @param context the routing context
   */
  public void searchUsers(@NotNull RoutingContext context) {
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

    // Create final copies for use inside lambdas.
    final Integer finalPage = page;
    final Integer finalPageLimit = pageLimit;

    // Get search criteria from the request body.
    JsonObject searchFields = context.body().asJsonObject();

    MultiMap headers = createHeader(context);

    vertxClient.getAbs(this.url + SEARCH_USER_REQUEST)
      .putHeaders(headers)
      .send(ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.OK.code() == ar.result().statusCode()) {
            log.info(AUTH_CLIENT + " Successfully retrieved users");

            // Convert retrieved users to a list of JsonObject.
            List<JsonObject> allUsers = buildUsersListFromKeycloakResponse(
              ar.result().bodyAsJsonArray());

            int totalDocuments = allUsers.size();

            List<JsonObject> filteredUsers = filterUsersBySearchCriteria(allUsers,
              searchFields);

            int totalFiltered = filteredUsers.size();

            // Apply sorting if sortBy is provided.
            if (StringUtils.isNotEmpty(sortByString)) {
              final String sortBy = sortByString;
              final boolean ascending = !("desc".equalsIgnoreCase(sortOrderString));
              // Date formatter for the date fields (e.g. "MM/dd/yyyy HH:mm:ss")
              filteredUsers.sort((u1, u2) -> {
                // Check if the sortBy field is one of the date fields.
                if (CREATED_TIMESTAMP.equals(sortBy) || LAST_LOGIN.equals(sortBy)) {
                  String s1 = u1.getString(sortBy);
                  String s2 = u2.getString(sortBy);
                  Instant d1 = null;
                  Instant d2 = null;
                  try {
                    if (s1 != null && !s1.isEmpty()) {
                      d1 = Instant.parse(s1);
                    }
                  } catch (Exception e) {
                    // Parsing error; treat as null.
                  }
                  try {
                    if (s2 != null && !s2.isEmpty()) {
                      d2 =Instant.parse(s2);
                    }
                  } catch (Exception e) {
                    // Parsing error; treat as null.
                  }
                  // Place null dates last.
                  if (d1 == null && d2 == null) {
                    return 0;
                  } else if (d1 == null) {
                    return 1;
                  } else if (d2 == null) {
                    return -1;
                  } else {
                    int cmp = d1.compareTo(d2);
                    return ascending ? cmp : -cmp;
                  }
                } else {
                  // For non-date fields, perform a case-insensitive string comparison.
                  String val1 = u1.getString(sortBy);
                  String val2 = u2.getString(sortBy);
                  if (val1 == null && val2 == null) {
                    return 0;
                  } else if (val1 == null) {
                    return 1;
                  } else if (val2 == null) {
                    return -1;
                  }
                  int cmp = val1.compareToIgnoreCase(val2);
                  return ascending ? cmp : -cmp;
                }
              });
            }

            // Apply paging using finalPage and finalPageLimit.
            List<JsonObject> pagedUsers;
            if (finalPage != null && finalPageLimit != null) {
              int fromIndex = (finalPage - 1) * finalPageLimit;
              int toIndex = Math.min(fromIndex + finalPageLimit, filteredUsers.size());
              if (fromIndex >= filteredUsers.size()) {
                pagedUsers = new ArrayList<>();
              } else {
                pagedUsers = filteredUsers.subList(fromIndex, toIndex);
              }
            } else {
              pagedUsers = filteredUsers;
            }

            // Build the JSON response.
            JsonArray responseData = new JsonArray();
            pagedUsers.forEach(responseData::add);

            JsonObject metadata = new JsonObject();
            metadata.put("total_documents", totalDocuments);
            metadata.put("total_documents_filtered", totalFiltered);
            int pageSize = (finalPageLimit != null ? finalPageLimit : totalFiltered);
            metadata.put("page_size", pageSize);
            int currentPage = (finalPage != null ? finalPage : 1);
            metadata.put("current_page", currentPage);
            int totalPages = (int) Math.ceil((double) totalFiltered / pageSize);
            metadata.put("total_pages", totalPages);

            if (StringUtils.isNotEmpty(sortByString)) {
              metadata.put("sort_by", sortByString);
              metadata.put("sort_order", sortOrderString);
            }

            JsonObject response = new JsonObject();
            response.put("data", responseData);
            response.put("metadata", metadata);

            context.response().setStatusCode(HttpResponseStatus.OK.code())
              .end(response.encodePrettily());
          } else {
            log.error(AUTH_CLIENT + " Failed to retrieve users");
            context.response().setStatusCode(ar.result().statusCode())
              .end(ar.result().bodyAsString());
          }
        } else {
          log.error(AUTH_CLIENT + ERROR_WHEN_RETRIEVING_USERS, ar.cause());
          context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .end(ar.cause().getMessage());
        }
      });
  }

  @NotNull
  static List<JsonObject> filterUsersBySearchCriteria(List<JsonObject> allUsers,
    JsonObject searchFields) {
    DateTimeFormatter searchFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return allUsers.stream().filter(user -> {
      // --- Handle createdTimestamp range if provided ---
      String createdFrom = searchFields.getString("createdTimestampFrom", "").trim();
      String createdTill = searchFields.getString("createdTimestampTill", "").trim();
      if (StringUtils.isNotEmpty(createdFrom) || StringUtils.isNotEmpty(createdTill)) {
        String userCreated = user.getString(CREATED_TIMESTAMP, "");
        if (StringUtils.isEmpty(userCreated)) {
          return false;
        }
        try {
          Instant userInstant = Instant.parse(userCreated);
          LocalDate userDate = userInstant.atZone(ZoneOffset.UTC).toLocalDate();
          if (StringUtils.isNotEmpty(createdFrom)) {
            LocalDate fromDate = LocalDate.parse(createdFrom, searchFormatter);

            if (userDate.isBefore(fromDate)) {
              return false;
            }
          }
          if (StringUtils.isNotEmpty(createdTill)) {
            LocalDate tillDate = LocalDate.parse(createdTill, searchFormatter);
            if (userDate.isAfter(tillDate)) {
              return false;
            }
          }
        } catch (DateTimeParseException e) {
          return false;
        }
      }

      // --- Handle lastLogin range if provided ---
      String lastLoginFrom = searchFields.getString("lastLoginFrom", "").trim();
      String lastLoginTill = searchFields.getString("lastLoginTill", "").trim();
      if (StringUtils.isNotEmpty(lastLoginFrom) || StringUtils.isNotEmpty(lastLoginTill)) {
        String userLastLogin = user.getString(LAST_LOGIN, "");
        if (StringUtils.isEmpty(userLastLogin)) {
          return false;
        }
        try {
          Instant userInstant = Instant.parse(userLastLogin);
          LocalDate userDate = userInstant.atZone(ZoneOffset.UTC).toLocalDate();
          if (StringUtils.isNotEmpty(lastLoginFrom)) {
            LocalDate fromDate = LocalDate.parse(lastLoginFrom, searchFormatter);
            if (userDate.isBefore(fromDate)) {
              return false;
            }
          }
          if (StringUtils.isNotEmpty(lastLoginTill)) {
            LocalDate tillDate = LocalDate.parse(lastLoginTill, searchFormatter);
            if (userDate.isAfter(tillDate)) {
              return false;
            }
          }
        } catch (DateTimeParseException e) {
          return false;
        }
      }

      // --- Process other search criteria ---
      for (String key : searchFields.fieldNames()) {
        // Skip date range fields; they're already processed.
        if ("createdTimestampFrom".equals(key) || "createdTimestampTill".equals(key) ||
          "lastLoginFrom".equals(key) || "lastLoginTill".equals(key)) {
          continue;
        }

        // check search criteria for roles
        // TODO the roles check is simple for now as all users have data-user role by default
        // (part of composite role default-roles-healthdata)
        // so the only applicable role is data-admin as a search criterion, this should be improved in the future.
        if (ROLES.equals(key) && searchFields.getString(key, "").toLowerCase()
          .contains("ec admin")) {
          if (!user.getValue(ROLES).toString().contains(DATA_ADMIN)) {
            return false;
          }
        } else {
          String searchValue = searchFields.getString(key, "").toLowerCase();
          if (StringUtils.isNotEmpty(searchValue)) {
            String fieldValue = user.getString(key, "").toLowerCase();
            if (!fieldValue.contains(searchValue)) {
              return false;
            }
          }
        }
      }
      return true;
    }).collect(Collectors.toList());
  }

  /**
   * Deletes a user from the auth client.
   *
   * @param context the routing context
   * @param userId  the user ID to delete
   * @return a future indicating the result of the operation
   */
  public Future<Void> deleteUser(RoutingContext context, String userId) {
    Promise<Void> promise = Promise.promise();

    if (userId == null || userId.isEmpty()) {
      promise.fail("Missing required userId parameter");
      return promise.future();
    }

    // Retrieve headers (e.g., authorization)
    MultiMap headers = createHeader(context);

    // Construct the URL for deleting the user.
    String deleteUserUrl = this.url + "/users/" + userId;

    // Send DELETE request to the health data auth module.
    vertxClient.deleteAbs(deleteUserUrl)
      .putHeaders(headers)
      .send(ar -> {
        if (ar.succeeded()) {
          int statusCode = ar.result().statusCode();
          if (statusCode == HttpResponseStatus.NO_CONTENT.code()
            || statusCode == HttpResponseStatus.OK.code()) {
            log.info("User deleted successfully, userId: {}", userId);
            promise.complete();
          } else {
            String errorMsg =
              "Failed to delete user, status code: " + statusCode + " " + ar.result()
                .bodyAsString();
            log.error(errorMsg);
            promise.fail(errorMsg);
          }
        } else {
          log.error("Error when deleting user", ar.cause());
          promise.fail(ar.cause());
        }
      });

    return promise.future();
  }

  /**
   * Updates the role of a user in the auth client.
   *
   * @param context the routing context
   */
  public void updateUserRole(RoutingContext context) {
    // Retrieve the parameters from the path.
    String operation = context.request().getParam("operation");
    String role = context.request().getParam("role");
    String userId = context.request().getParam(USER_ID);

    // Validate required parameters.
    if (operation == null || operation.isEmpty() ||
      role == null || role.isEmpty() ||
      userId == null || userId.isEmpty()) {
      context.response()
        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
        .end("Missing required parameter: operation, role, or userId");
      return;
    }

    // Retrieve headers (e.g., including the authorization token)
    MultiMap headers = createHeader(context);

    String targetUrl = this.url + "/users/" + userId + "/roles/" + role;

    // Check the operation and invoke the corresponding HTTP method.
    if ("add".equalsIgnoreCase(operation)) {
      // Use POST to add the role.
      vertxClient.postAbs(targetUrl)
        .putHeaders(headers)
        .send(ar -> {
          if (ar.succeeded()) {
            int statusCode = ar.result().statusCode();
            if (statusCode == HttpResponseStatus.OK.code() ||
              statusCode == HttpResponseStatus.CREATED.code() ||
              statusCode == HttpResponseStatus.NO_CONTENT.code()) {

              //Audit Log
              getEcasIdFromAllUsers(context, userId)
                .onSuccess(ecasId -> {
                  String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                    AuditLogActionDescription.USER_UPDATED.toString(), ecasId);
                  auditLogManager.insertAuditLog(AuditLogObject.USER_ROLE_MANAGEMENT.toString(),
                    AuditLogAction.USER_UPDATED.toString(),
                    auditLogActionDescription, context);
                });

              context.response()
                .setStatusCode(HttpResponseStatus.OK.code())
                .end("Role added successfully");
            } else {
              String errorMsg = "Failed to add role, status code: " + statusCode +
                " " + ar.result().bodyAsString();
              context.response()
                .setStatusCode(statusCode)
                .end(errorMsg);
            }
          } else {
            context.response()
              .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
              .end("Error when adding role: " + ar.cause().getMessage());
          }
        });
    } else if ("remove".equalsIgnoreCase(operation)) {
      // Use DELETE to remove the role.
      vertxClient.deleteAbs(targetUrl)
        .putHeaders(headers)
        .send(ar -> {
          if (ar.succeeded()) {
            int statusCode = ar.result().statusCode();
            if (statusCode == HttpResponseStatus.OK.code() ||
              statusCode == HttpResponseStatus.NO_CONTENT.code()) {

              //Audit Log
              getEcasIdFromAllUsers(context, userId)
                .onSuccess(ecasId -> {
                  String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
                    AuditLogActionDescription.USER_UPDATED.toString(), ecasId);
                  auditLogManager.insertAuditLog(AuditLogObject.USER_ROLE_MANAGEMENT.toString(),
                    AuditLogAction.USER_UPDATED.toString(),
                    auditLogActionDescription, context);
                });

              context.response()
                .setStatusCode(HttpResponseStatus.OK.code())
                .end("Role removed successfully");
            } else {
              String errorMsg = "Failed to remove role, status code: " + statusCode +
                " " + ar.result().bodyAsString();
              context.response()
                .setStatusCode(statusCode)
                .end(errorMsg);
            }
          } else {
            context.response()
              .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
              .end("Error when removing role: " + ar.cause().getMessage());
          }
        });
    } else {
      context.response()
        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
        .end("Invalid operation. Must be 'add' or 'remove'");
    }
  }


  public void validateUsersSearchFilters(@NotNull RoutingContext context){

    log.info("Will validate filters of Users");

    JsonObject searchFields = context.body().asJsonObject();

    JsonArray errors = new JsonArray();


    String username = searchFields.getString(USERNAME);
    if(StringUtils.isNotEmpty(username) && username.length() > MAX_ALLOWED_CHARS_50){
        log.info("Adding error for ecasId with max chars: {}", MAX_ALLOWED_CHARS_50);
        errors.add(getSizeErrorMessage(USERNAME, MAX_ALLOWED_CHARS_50));
      }

    String firstName = searchFields.getString(FIRST_NAME);
    if(StringUtils.isNotEmpty(firstName) && firstName.length() > MAX_ALLOWED_CHARS_50){
        log.info("Adding error for firstName with max chars: {}", MAX_ALLOWED_CHARS_50);
        errors.add(getSizeErrorMessage(FIRST_NAME, MAX_ALLOWED_CHARS_50));
      }

    String lastName = searchFields.getString(LAST_NAME);
    if(StringUtils.isNotEmpty(lastName) && lastName.length() > MAX_ALLOWED_CHARS_50){
        log.info("Adding error for lastName with max chars: {}", MAX_ALLOWED_CHARS_50);
        errors.add(getSizeErrorMessage(LAST_NAME, MAX_ALLOWED_CHARS_50));
      }

    if (errors.isEmpty()) {
      log.info("No errors exist");
      context.next();
    } else {
      log.error("Audit Log search could not proceed due to validation errors");
      setUpMessageContextResponseWithContent(context,
        errors);
    }
  }

  protected void setUpMessageContextResponseWithContent(RoutingContext context,
    JsonArray content) {
    context.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put("result_message",
      "Users search could not proceed due to validation errors").put("errors", content).encode());
  }


  private JsonObject getSizeErrorMessage(String fieldName, int size){

    JsonArray params = new JsonArray();
    params.add(String.valueOf(size));

    return new JsonObject()
      .put("code", "Size")
      .put("errorMessage", "Field: " + fieldName + " cannot contain more than: " + size + " characters.")
      .put("field", fieldName)
      .put("params", params);
  }

  /**
   * Builds a list of users from the Keycloak response.
   *
   * @param usersArray the JSON array of users
   * @return a list of JSON objects representing users
   */
  @NotNull
  private List<JsonObject> buildUsersListFromKeycloakResponse(JsonArray usersArray) {
    List<JsonObject> allUsers = new ArrayList<>();
    for (Object user : usersArray) {
      JsonObject userObj = (JsonObject) user;
      JsonObject userToStore = new JsonObject();
      userToStore.put(USER_ID, userObj.getString("id"));
      userToStore.put(USERNAME, userObj.getString(USERNAME));
      userToStore.put("deletePath", userObj.getString("deletePath"));
      userToStore.put(FIRST_NAME, userObj.getString(FIRST_NAME));
      userToStore.put(LAST_NAME, userObj.getString(LAST_NAME));
      userToStore.put("email", userObj.getString("email"));
      userToStore.put(CREATED_TIMESTAMP, userObj.getString(CREATED_TIMESTAMP));
      userToStore.put(LAST_LOGIN, userObj.getString(LAST_LOGIN));
      userToStore.put(ROLES, userObj.getString(ROLES));
      // last admin flag, default false
      userToStore.put("isLastAdmin", false);

      allUsers.add(userToStore);
    }

    // Count how many have the data-admin role
    long dataAdminCount = allUsers.stream()
      .filter(u -> {
        String roles = u.getString(ROLES);
        return roles != null && roles.contains(DATA_ADMIN);
      })
      .count();

    // If exactly one, mark that one as lastAdmin=true
    if (dataAdminCount == 1) {
      allUsers.stream()
        .filter(u -> {
          String roles = u.getString(ROLES);
          return roles != null && roles.contains(DATA_ADMIN);
        })
        .findFirst()
        .ifPresent(u -> u.put("isLastAdmin", true));
    }

    return allUsers;
  }

  private Future<String> getEcasIdFromAllUsers(@NotNull RoutingContext context, String userId) {

    Promise<String> promise = Promise.promise();

    MultiMap headers = createHeader(context);

    vertxClient.getAbs(this.url + SEARCH_USER_REQUEST)
      .putHeaders(headers)
      .send(ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.OK.code() == ar.result().statusCode()) {
            log.info(AUTH_CLIENT + " Successfully retrieved users from getEcasIdFromAllUsers");

            // Convert retrieved users to a list of JsonObject.
            List<JsonObject> allUsers = buildUsersListFromKeycloakResponse(
              ar.result().bodyAsJsonArray());
            Optional<String> ecasId = allUsers.stream()
              .filter(user -> userId.equals(user.getString(USER_ID)))
              .map(user -> user.getString(USERNAME).trim())
              .findFirst();

            promise.complete(ecasId.orElse(""));
          } else {
            log.error(AUTH_CLIENT + " Failed to retrieve users from getEcasIdFromAllUsers");
            promise.fail("Failed to retrieve users with status: " + ar.result().statusCode());
          }
        } else {
          log.error(AUTH_CLIENT + ERROR_WHEN_RETRIEVING_USERS, ar.cause());
          promise.fail(ar.cause());
        }
      });
    return promise.future();
  }
}
