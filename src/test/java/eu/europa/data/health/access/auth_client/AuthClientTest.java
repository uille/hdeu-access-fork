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

import static eu.europa.data.health.access.auth_client.AuthClient.filterUsersBySearchCriteria;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthClientTest {

  @Mock
  RoutingContext context;
  @Mock
  HttpServerRequest serverRequest;
  @Mock
  HttpServerResponse serverResponse;
  @Mock
  WebClient webClient;
  @Mock
  HttpRequest<Buffer> request;
  @Mock
  AsyncResult<HttpResponse<Buffer>> asyncResult;
  @Mock
  HttpResponse<Buffer> httpResponse;
  @Mock
  User user;
  @Mock
  RequestBody requestBody;

  private AuthClient authClient;

  private final String BASE_URL = "http://auth-service";

  private final String TOKEN = "dummy-token";

  @BeforeEach
  void setup() {
    authClient = new AuthClient(BASE_URL, webClient);

    JsonObject principal = new JsonObject().put("access_token", TOKEN);
    lenient().when(user.principal()).thenReturn(principal);
    lenient().when(context.user()).thenReturn(user);
    lenient().when(context.response()).thenReturn(serverResponse);
    lenient().when(serverResponse.setStatusCode(anyInt())).thenReturn(serverResponse);
    lenient().when(context.request()).thenReturn(serverRequest);
    lenient().when(context.body()).thenReturn(requestBody);
    lenient().when(webClient.getAbs(BASE_URL + "/users")).thenReturn(request);
    lenient().when(request.putHeaders(any())).thenReturn(request);
  }

  @Test
  void testCreateHeader() {
    MultiMap headers = authClient.createHeader(context);
    assertEquals("authToken=dummy-token", headers.get("Cookie"));
  }

  @Test
  void testGetAllUsers_successfulResponse() {
    JsonArray userArray = new JsonArray().add(new JsonObject()
      .put("username", "jdoe")
      .put("firstName", "John")
      .put("lastName", "Doe"));

    ArgumentCaptor<Handler<AsyncResult<HttpResponse<Buffer>>>> captor = ArgumentCaptor.forClass(
      Handler.class);

    when(webClient.getAbs(BASE_URL + "/users")).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);
    when(request.addQueryParam(any(),any())).thenReturn(request);

    Future<JsonArray> future = authClient.getAllUsers(context,true);

    verify(request).send(captor.capture());

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    HttpResponse<Buffer> httpResponse = mock(HttpResponse.class);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonArray()).thenReturn(userArray);

    captor.getValue().handle(asyncResult);

    future.onComplete(res -> {
      assertTrue(res.succeeded());
      JsonArray result = res.result();
      JsonObject user = result.getJsonObject(0);
      assertEquals("jdoe", user.getString("ecasId"));
    });
  }

  @Test
  void testGetAllUsers_failedStatus() {
    ArgumentCaptor<Handler<AsyncResult<HttpResponse<Buffer>>>> captor = ArgumentCaptor.forClass(
      Handler.class);
    when(webClient.getAbs(BASE_URL + "/users")).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);
    when(request.addQueryParam(any(),any())).thenReturn(request);


    Future<JsonArray> future = authClient.getAllUsers(context,true);

    verify(request).send(captor.capture());

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    HttpResponse<Buffer> httpResponse = mock(HttpResponse.class);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(500);

    captor.getValue().handle(asyncResult);

    future.onComplete(res -> {
      assertTrue(res.failed());
      assertTrue(res.cause().getMessage().contains("Failed to retrieve users"));
    });
  }

  @Test
  void testGetAllUsers_exceptionThrown() {
    ArgumentCaptor<Handler<AsyncResult<HttpResponse<Buffer>>>> captor = ArgumentCaptor.forClass(
      Handler.class);
    when(webClient.getAbs(BASE_URL + "/users")).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);
    when(request.addQueryParam(any(),any())).thenReturn(request);

    Future<JsonArray> future = authClient.getAllUsers(context,true);

    verify(request).send(captor.capture());

    Throwable throwable = new RuntimeException("Connection error");

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);

    captor.getValue().handle(asyncResult);

    future.onComplete(res -> {
      assertTrue(res.failed());
      assertEquals("Connection error", res.cause().getMessage());
    });
  }

  @Test
  void testSearchUsers_success_withPaginationAndSorting() {
    when(serverRequest.getParam("page")).thenReturn("1");
    when(serverRequest.getParam("limit")).thenReturn("2");
    when(serverRequest.getParam("sortBy")).thenReturn("firstName");
    when(serverRequest.getParam("sortOrder")).thenReturn("asc");
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());

    JsonArray usersArray = new JsonArray()
      .add(new JsonObject().put("username", "jsmith").put("firstName", "Jane").put("lastName", "Smith"))
      .add(new JsonObject().put("username", "adoe").put("firstName", "Alice").put("lastName", "Doe"))
      .add(new JsonObject().put("username", "bwayne").put("firstName", "Bruce").put("lastName", "Wayne"));

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonArray()).thenReturn(usersArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      handler.handle(asyncResult);
      return null;
    }).when(request).send(any());

    authClient.searchUsers(context);
    verify(serverResponse).setStatusCode(200);
    verify(serverResponse).end(argThat((ArgumentMatcher<String>) body -> body.contains("metadata")));
  }

  @Test
  void testSearchUsers_non200Status() {
    when(serverRequest.getParam(anyString())).thenReturn(null);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());
    when(httpResponse.statusCode()).thenReturn(403);
    when(httpResponse.bodyAsString()).thenReturn("Forbidden");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      handler.handle(asyncResult);
      return null;
    }).when(request).send(any());

    authClient.searchUsers(context);
    verify(serverResponse).setStatusCode(403);
    verify(serverResponse).end("Forbidden");
  }

  @Test
  void testSearchUsers_failure_fetchError() {
    Throwable failureCause = new RuntimeException("Network Error");

    when(serverRequest.getParam(anyString())).thenReturn(null);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(failureCause);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      handler.handle(asyncResult);
      return null;
    }).when(request).send(any());

    authClient.searchUsers(context);
    verify(serverResponse).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(serverResponse).end("Network Error");
  }

  @Test
  void testSearchUsers_dateSortingWithNulls() {
    when(serverRequest.getParam("sortBy")).thenReturn("createdTimestamp");
    when(serverRequest.getParam("sortOrder")).thenReturn("desc");
    when(serverRequest.getParam("page")).thenReturn(null);
    when(serverRequest.getParam("limit")).thenReturn(null);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());


    JsonArray usersArray = new JsonArray()
      .add(new JsonObject().put("username", "jdoe").put("createdTimestamp", "01/01/2022 12:00:00"))
      .add(new JsonObject().put("username", "adoe")) // Missing timestamp
      .add(new JsonObject().put("username", "bwayne").put("createdTimestamp", "01/03/2022 09:00:00"));

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonArray()).thenReturn(usersArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      handler.handle(asyncResult);
      return null;
    }).when(request).send(any());

    authClient.searchUsers(context);
    verify(serverResponse).setStatusCode(200);
    verify(serverResponse).end(argThat((ArgumentMatcher<String>) body -> body.contains("metadata")));
  }

  @Test
  void testSearchUsers_paginationOverflow() {
    when(serverRequest.getParam("page")).thenReturn("10");
    when(serverRequest.getParam("limit")).thenReturn("5");
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());

    JsonArray usersArray = new JsonArray()
      .add(new JsonObject().put("username", "user1"))
      .add(new JsonObject().put("username", "user2"));

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonArray()).thenReturn(usersArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      handler.handle(asyncResult);
      return null;
    }).when(request).send(any());

    authClient.searchUsers(context);
    verify(serverResponse).end(argThat((ArgumentMatcher<String>) body -> {
      JsonObject json = new JsonObject(body);
      JsonArray data = json.getJsonArray("data");
      JsonObject metadata = json.getJsonObject("metadata");
      return data.isEmpty()
        && metadata.getInteger("current_page") == 10
        && metadata.getInteger("page_size") == 5
        && metadata.getInteger("total_pages") == 1
        && metadata.getInteger("total_documents") == 2;
    }));
  }

  @Test
  void testSearchUsers_noPagingNoSorting() {
    when(serverRequest.getParam(anyString())).thenReturn(null);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject());

    JsonArray usersArray = new JsonArray()
      .add(new JsonObject().put("username", "zeta"))
      .add(new JsonObject().put("username", "alpha"));

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonArray()).thenReturn(usersArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      handler.handle(asyncResult);
      return null;
    }).when(request).send(any());

    authClient.searchUsers(context);
    verify(serverResponse).setStatusCode(200);
    verify(serverResponse).end(argThat((ArgumentMatcher<String>) body -> body.contains("total_documents")));
  }

  @Test
  void testCreatedTimestampInRange_shouldMatch() {
    JsonObject user = new JsonObject()
      .put("createdTimestamp", "2023-01-15T08:07:00Z")
      .put("username", "jdoe");

    JsonObject criteria = new JsonObject()
      .put("createdTimestampFrom", "2023-01-01")
      .put("createdTimestampTill", "2023-12-31");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertEquals(1, result.size());
  }

  @Test
  void testCreatedTimestampOutOfRange_shouldFilterOut() {
    JsonObject user = new JsonObject()
      .put("createdTimestamp", "2022-12-31");


    JsonObject criteria = new JsonObject()
      .put("createdTimestampFrom", "2023-01-01");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertTrue(result.isEmpty());
  }

  @Test
  void testLastLoginInRange_shouldMatch() {
    JsonObject user = new JsonObject()
      .put("lastLogin", "2023-01-01T00:00:00Z");

    JsonObject criteria = new JsonObject()
      .put("lastLoginFrom", "2023-01-01")
      .put("lastLoginTill", "2023-01-15");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertEquals(1, result.size());
  }

  @Test
  void testMissingLastLogin_shouldBeFilteredOutIfDateCriteriaPresent() {
    JsonObject user = new JsonObject(); // no lastLogin

    JsonObject criteria = new JsonObject()
      .put("lastLoginFrom", "2023-01-01");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertTrue(result.isEmpty());
  }

  @Test
  void testUsernameContains_shouldMatch() {
    JsonObject user = new JsonObject().put("username", "jdoe");
    JsonObject criteria = new JsonObject().put("username", "doe");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertEquals(1, result.size());
  }

  @Test
  void testUsernameDoesNotContain_shouldNotMatch() {
    JsonObject user = new JsonObject().put("username", "jdoe");
    JsonObject criteria = new JsonObject().put("username", "smith");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertTrue(result.isEmpty());
  }

  @Test
  void testRoleMatchesEcAdmin_shouldPassIfHasDataAdmin() {
    JsonObject user = new JsonObject()
      .put("username", "adminuser")
      .put("roles", "data-admin");

    JsonObject criteria = new JsonObject().put("roles", "ec admin");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertEquals(1, result.size());
  }

  @Test
  void testRoleDoesNotMatchEcAdmin_shouldFilterOut() {
    JsonObject user = new JsonObject()
      .put("username", "basicuser")
      .put("roles", "data-user");

    JsonObject criteria = new JsonObject().put("roles", "ec admin");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertTrue(result.isEmpty());
  }

  @Test
  void testEmptySearchFields_shouldReturnAll() {
    JsonObject user1 = new JsonObject().put("username", "u1");
    JsonObject user2 = new JsonObject().put("username", "u2");

    JsonObject criteria = new JsonObject();

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user1, user2), criteria);
    assertEquals(2, result.size());
  }

  @Test
  void testMalformedCreatedTimestamp_shouldBeFilteredOut() {
    JsonObject user = new JsonObject()
      .put("createdTimestamp", "invalid-date");

    JsonObject criteria = new JsonObject()
      .put("createdTimestampFrom", "2023-01-01");

    List<JsonObject> result = filterUsersBySearchCriteria(List.of(user), criteria);
    assertTrue(result.isEmpty());
  }

  @Test
  void testDeleteUser_successfulResponse() {
    String userId = "12345";
    String expectedUrl = BASE_URL + "/users/" + userId;

    HttpRequest<Buffer> deleteRequest = mock(HttpRequest.class);

    when(webClient.deleteAbs(expectedUrl)).thenReturn(deleteRequest);
    when(deleteRequest.putHeaders(any())).thenReturn(deleteRequest);

    ArgumentCaptor<Handler<AsyncResult<HttpResponse<Buffer>>>> captor = ArgumentCaptor.forClass(Handler.class);

    Future<Void> future = authClient.deleteUser(context, userId);
    verify(deleteRequest).send(captor.capture());

    // Simulate a successful DELETE with 204
    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    HttpResponse<Buffer> response = mock(HttpResponse.class);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(response);
    when(response.statusCode()).thenReturn(HttpResponseStatus.NO_CONTENT.code());

    captor.getValue().handle(asyncResult);

    future.onComplete(res -> {
      assertTrue(res.succeeded());
    });
  }

  @Test
  void testDeleteUser_missingUserId_shouldFail() {
    Future<Void> future = authClient.deleteUser(context, null);

    future.onComplete(res -> {
      assertTrue(res.failed());
      assertEquals("Missing required userId parameter", res.cause().getMessage());
    });
  }

  @Test
  void testDeleteUser_unsuccessfulStatus_shouldFail() {
    String userId = "99999";
    String expectedUrl = BASE_URL + "/users/" + userId;

    HttpRequest<Buffer> deleteRequest = mock(HttpRequest.class);

    when(webClient.deleteAbs(expectedUrl)).thenReturn(deleteRequest);
    when(deleteRequest.putHeaders(any())).thenReturn(deleteRequest);

    ArgumentCaptor<Handler<AsyncResult<HttpResponse<Buffer>>>> captor = ArgumentCaptor.forClass(Handler.class);

    Future<Void> future = authClient.deleteUser(context, userId);
    verify(deleteRequest).send(captor.capture());

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    HttpResponse<Buffer> response = mock(HttpResponse.class);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(response);
    when(response.statusCode()).thenReturn(500);
    when(response.bodyAsString()).thenReturn("Internal Server Error");

    captor.getValue().handle(asyncResult);

    future.onComplete(res -> {
      assertTrue(res.failed());
      assertTrue(res.cause().getMessage().contains("Failed to delete user"));
    });
  }
  @Test
  void testDeleteUser_requestFails_shouldFail() {
    String userId = "88888";
    String expectedUrl = BASE_URL + "/users/" + userId;

    HttpRequest<Buffer> deleteRequest = mock(HttpRequest.class);
    when(webClient.deleteAbs(expectedUrl)).thenReturn(deleteRequest);
    when(deleteRequest.putHeaders(any())).thenReturn(deleteRequest);

    ArgumentCaptor<Handler<AsyncResult<HttpResponse<Buffer>>>> captor = ArgumentCaptor.forClass(Handler.class);

    Future<Void> future = authClient.deleteUser(context, userId);
    verify(deleteRequest).send(captor.capture());

    Throwable error = new RuntimeException("Timeout");

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(error);

    captor.getValue().handle(asyncResult);

    future.onComplete(res -> {
      assertTrue(res.failed());
      assertEquals("Timeout", res.cause().getMessage());
    });
  }

  @Test
  void testUpdateUserRole_addRole_success() {
    String userId = "user123";
    String role = "data-admin";

    when(serverRequest.getParam("operation")).thenReturn("add");
    when(serverRequest.getParam("role")).thenReturn(role);
    when(serverRequest.getParam("userId")).thenReturn(userId);

    HttpRequest<Buffer> postRequest = mock(HttpRequest.class);
    when(webClient.postAbs(BASE_URL + "/users/" + userId + "/roles/" + role)).thenReturn(postRequest);
    when(postRequest.putHeaders(any())).thenReturn(postRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      when(httpResponse.statusCode()).thenReturn(200);
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(httpResponse);
      handler.handle(asyncResult);
      return null;
    }).when(postRequest).send(any());

    authClient.updateUserRole(context);

    verify(serverResponse).setStatusCode(200);
    verify(serverResponse).end("Role added successfully");
  }

  @Test
  void testUpdateUserRole_missingParameters_shouldFail() {
    when(serverRequest.getParam("operation")).thenReturn(null);
    when(serverRequest.getParam("role")).thenReturn("role");
    when(serverRequest.getParam("userId")).thenReturn("id");

    authClient.updateUserRole(context);

    verify(serverResponse).setStatusCode(400);
    verify(serverResponse).end("Missing required parameter: operation, role, or userId");
  }

  @Test
  void testUpdateUserRole_invalidOperation_shouldFail() {
    when(serverRequest.getParam("operation")).thenReturn("edit");
    when(serverRequest.getParam("role")).thenReturn("data-admin");
    when(serverRequest.getParam("userId")).thenReturn("user123");

    authClient.updateUserRole(context);

    verify(serverResponse).setStatusCode(400);
    verify(serverResponse).end("Invalid operation. Must be 'add' or 'remove'");
  }

  @Test
  void testUpdateUserRole_addRole_serverError() {
    String userId = "user123";
    String role = "data-admin";

    when(serverRequest.getParam("operation")).thenReturn("add");
    when(serverRequest.getParam("role")).thenReturn(role);
    when(serverRequest.getParam("userId")).thenReturn(userId);

    HttpRequest<Buffer> postRequest = mock(HttpRequest.class);
    when(webClient.postAbs(BASE_URL + "/users/" + userId + "/roles/" + role)).thenReturn(postRequest);
    when(postRequest.putHeaders(any())).thenReturn(postRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      when(httpResponse.statusCode()).thenReturn(500);
      when(httpResponse.bodyAsString()).thenReturn("Internal Error");
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(httpResponse);
      handler.handle(asyncResult);
      return null;
    }).when(postRequest).send(any());

    authClient.updateUserRole(context);

    verify(serverResponse).setStatusCode(500);
    verify(serverResponse).end(contains("Failed to add role"));
  }

  @Test
  void testUpdateUserRole_removeRole_exceptionThrown() {
    String userId = "user123";
    String role = "data-admin";

    when(serverRequest.getParam("operation")).thenReturn("remove");
    when(serverRequest.getParam("role")).thenReturn(role);
    when(serverRequest.getParam("userId")).thenReturn(userId);

    HttpRequest<Buffer> deleteRequest = mock(HttpRequest.class);
    when(webClient.deleteAbs(BASE_URL + "/users/" + userId + "/roles/" + role)).thenReturn(deleteRequest);
    when(deleteRequest.putHeaders(any())).thenReturn(deleteRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
      when(asyncResult.succeeded()).thenReturn(false);
      when(asyncResult.cause()).thenReturn(new RuntimeException("Timeout"));
      handler.handle(asyncResult);
      return null;
    }).when(deleteRequest).send(any());

    authClient.updateUserRole(context);

    verify(serverResponse).setStatusCode(500);
    verify(serverResponse).end(contains("Error when removing role: Timeout"));
  }

  @Test
  void testValidateUsersSearchFilters_noValidationErrors() {
    JsonObject body = new JsonObject()
      .put("username", "jdoe")
      .put("firstName", "John")
      .put("lastName", "Doe");

    when(requestBody.asJsonObject()).thenReturn(body);

    authClient.validateUsersSearchFilters(context);

    verify(context).next();
    verify(serverResponse, never()).end(anyString());
  }

  @Test
  void testValidateUsersSearchFilters_usernameTooLong() {
    String longUsername = "x".repeat(51);
    JsonObject body = new JsonObject().put("username", longUsername);

    when(requestBody.asJsonObject()).thenReturn(body);

    authClient.validateUsersSearchFilters(context);

    verify(serverResponse).setStatusCode(400);
    verify(serverResponse).end(argThat((String bodyStr) -> bodyStr.contains("username")));
  }

  @Test
  void testValidateUsersSearchFilters_firstNameTooLong() {
    String longFirstName = "x".repeat(51);
    JsonObject body = new JsonObject().put("firstName", longFirstName);

    when(requestBody.asJsonObject()).thenReturn(body);

    authClient.validateUsersSearchFilters(context);

    verify(serverResponse).setStatusCode(400);
    verify(serverResponse).end(argThat((String bodyStr) -> bodyStr.contains("firstName")));
  }


  @Test
  void testValidateUsersSearchFilters_lastNameTooLong() {
    String longLastName = "x".repeat(51);
    JsonObject body = new JsonObject().put("lastName", longLastName);

    when(requestBody.asJsonObject()).thenReturn(body);

    authClient.validateUsersSearchFilters(context);

    verify(serverResponse).setStatusCode(400);
    verify(serverResponse).end(argThat((String bodyStr) -> bodyStr.contains("lastName")));
  }

  @Test
  void testValidateUsersSearchFilters_multipleFieldsTooLong() {
    String longString = "x".repeat(60);
    JsonObject body = new JsonObject()
      .put("username", longString)
      .put("firstName", longString)
      .put("lastName", longString);

    when(requestBody.asJsonObject()).thenReturn(body);

    authClient.validateUsersSearchFilters(context);

    verify(serverResponse).setStatusCode(400);
    verify(serverResponse).end(argThat((String bodyStr) ->
      bodyStr.contains("username") &&
        bodyStr.contains("firstName") &&
        bodyStr.contains("lastName")
    ));
  }

  @Test
  void testValidateUsersSearchFilters_emptyFields_shouldPass() {
    JsonObject body = new JsonObject()
      .put("username", "")
      .put("firstName", "")
      .put("lastName", "");

    when(requestBody.asJsonObject()).thenReturn(body);

    authClient.validateUsersSearchFilters(context);

    verify(context).next();
    verify(serverResponse, never()).end(anyString());
  }

}
