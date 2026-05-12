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
package eu.europa.data.health.access.dispatcher_client;

import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.UtilityServiceTest.getStringJsonObject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.TestAppender;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DispatcherClientTest {

  private WebClient webClient;
  @InjectMocks
  private DispatcherClient dispatcherClient;
  private HttpRequest<Buffer> httpRequest;
  private static final String API_URL = "http://example.com";
  private static final String API_KEY = "dummy-key";
  private TestAppender testAppender;
  @Mock
  private HttpResponse<Buffer> httpResponse;
  @Mock
  RequestBody requestBody;

  @BeforeEach
  void setUp() {
    Logger mockLogger = LoggerFactory.getLogger(DispatcherClient.class);
    testAppender = new TestAppender();
    testAppender.start();
    ((ch.qos.logback.classic.Logger) mockLogger).addAppender(testAppender);
    webClient = mock(WebClient.class);
    httpRequest = mock(HttpRequest.class);
    dispatcherClient = new DispatcherClient(API_URL, API_KEY, webClient);
  }

  @Test
  void testSendApplicationSuccess() {
    JsonObject dispatcherRecord = new JsonObject(
      getStringJsonObject("full_data_application_dispatcher"));

    JsonObject successResponse = new JsonObject().put("message", "Application processed");

    HttpResponse<Buffer> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());
    when(mockResponse.bodyAsString()).thenReturn(successResponse.encode());

    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(mockResponse.statusCode()).thenReturn(201);
      when(mockResponse.bodyAsString()).thenReturn("success");

      handler.handle(Future.succeededFuture(mockResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    Future<String> result = dispatcherClient.sendApplication(dispatcherRecord);

    assertTrue(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream()
      .anyMatch(msg -> msg.contains("Successfully sent DISPATCHER")));
  }

  @Test
  void testSendApplicationDispatcherFailure() {
    JsonObject dispatcherRecord = new JsonObject(
      getStringJsonObject("full_data_application_dispatcher"));

    JsonObject successResponse = new JsonObject().put("message", "Application processed");

    HttpResponse<Buffer> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());
    when(mockResponse.bodyAsString()).thenReturn(successResponse.encode());

    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(mockResponse.statusCode()).thenReturn(401);
      when(mockResponse.bodyAsString()).thenReturn("success");

      handler.handle(Future.succeededFuture(mockResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    Future<String> result = dispatcherClient.sendApplication(dispatcherRecord);

    assertFalse(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream()
      .anyMatch(msg -> msg.contains("Dispatcher failed to process record with id")
        && msg.contains("error code: 401")));
  }

  @Test
  void testSendApplicationFailure() {
    JsonObject dispatcherRecord = new JsonObject(
      getStringJsonObject("full_data_application_dispatcher"));

    HttpResponse<Buffer> response = mock(HttpResponse.class);

    when(webClient.postAbs(eq(API_URL + DispatcherClient.DATA_PERMIT_CREATE_APPLICATION_REQUEST)))
      .thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      handler.handle(Future.failedFuture("Simulated failure"));

      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    dispatcherClient.sendApplication(dispatcherRecord);

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Failed to sent application:")));
  }

  @Test
  void testDeleteApplicationOnDispatcherSuccess() {
    List<String> applicationIds = List.of("appId1", "appId2");

    when(webClient.deleteAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      HttpResponse<Buffer> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(HttpResponseStatus.NO_CONTENT.code());

      handler.handle(Future.succeededFuture(response));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());
    JsonObject applications = new JsonObject()
      .put("accessApplications", new JsonArray(applicationIds));
    Future<String> result = dispatcherClient.deleteApplicationOnDispatcher(applications);

    assertTrue(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("DISPATCHER_CLIENT successfully "
      + "delete applications:")));
  }

  @Test
  void testDeleteApplicationOnDispatcherFailure() {
    List<String> applicationIds = List.of("appId1", "appId2");

    when(webClient.deleteAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      HttpResponse<Buffer> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(HttpResponseStatus.BAD_REQUEST.code());
      when(response.bodyAsString()).thenReturn("success");

      handler.handle(Future.succeededFuture(response));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());
    JsonObject applications = new JsonObject()
      .put("accessApplications", new JsonArray(applicationIds));
    Future<String> result = dispatcherClient.deleteApplicationOnDispatcher(applications);

    assertFalse(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("DISPATCHER_CLIENT failed to "
      + "delete applications:")));
  }

  @Test
  void testDeleteApplicationOnDispatcherFailureResponse() {
    List<String> applicationIds = List.of("appId1", "appId2");

    when(webClient.deleteAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      HttpResponse<Buffer> response = mock(HttpResponse.class);

      handler.handle(Future.failedFuture("fail"));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());
    JsonObject applications = new JsonObject()
      .put("accessApplications", new JsonArray(applicationIds));
    Future<String> result = dispatcherClient.deleteApplicationOnDispatcher(applications);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains(
      "DISPATCHER_CLIENT failed to delete application: {\"accessApplications\":[\"appId1\",\"appId2\"]} to dispatcher, with error message: fail")));
  }

  @Test
  void testGetCatalogMappingByCatalogId_success() {
    String catalogId = "catalog123";
    JsonObject expectedResponse = new JsonObject().put("id", catalogId).put("mapping", "value");

    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.bodyAsJsonObject()).thenReturn(expectedResponse);

    Future<HttpResponse<Buffer>> futureResponse = Future.succeededFuture(response);

    when(webClient.getAbs(eq(API_URL + "/catalog/catalogId/" + catalogId)))
      .thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(futureResponse);

    Future<JsonObject> future = dispatcherClient.getCatalogMappingByCatalogId(catalogId);

    assertTrue(future.succeeded());
    assertEquals(expectedResponse, future.result());

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Catalog mapping was found for")));
  }

  @Test
  void testGetCatalogMappingByCatalogId_notFound() {
    String catalogId = "nonexistent";

    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(404);
    when(response.statusMessage()).thenReturn("Not Found");

    Future<HttpResponse<Buffer>> futureResponse = Future.succeededFuture(response);

    when(webClient.getAbs(eq(API_URL + "/catalog/catalogId/" + catalogId)))
      .thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(futureResponse);

    Future<JsonObject> future = dispatcherClient.getCatalogMappingByCatalogId(catalogId);

    assertTrue(future.failed());
    assertTrue(future.cause().getMessage().contains("Failed to get catalog mapping"));

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Failed to get catalog mapping")));
  }

  @Test
  void testGetCatalogMappingByCatalogId_requestFailure() {
    String catalogId = "catalog-crash";

    when(webClient.getAbs(eq(API_URL + "/catalog/catalogId/" + catalogId)))
      .thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(
      Future.failedFuture("Connection timeout")); // ✅ Correct mocking

    Future<JsonObject> future = dispatcherClient.getCatalogMappingByCatalogId(catalogId);

    assertTrue(future.failed());
    assertTrue(future.cause().getMessage().contains("Connection timeout"));
  }

  @Test
  void testDeleteCatalogMappingByMongoId_success() {
    String mongoId = "mongo123";
    String fullUrl = API_URL + "/catalogs/" + mongoId;

    RoutingContext context = mock(RoutingContext.class);
    when(context.get("catalogMappingMongoId")).thenReturn(mongoId);

    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);

    when(webClient.deleteAbs(eq(fullUrl))).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<Integer> result = dispatcherClient.deleteCatalogMappingByMongoId(context);

    assertTrue(result.succeeded());
    assertEquals(200, result.result());

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream()
      .anyMatch(msg -> msg.contains("deleted for: catalog mapping with mongo id")));
  }


  @Test
  void testDeleteCatalogMappingByMongoId_notFound() {
    String mongoId = "notExist123";
    RoutingContext context = mock(RoutingContext.class);
    when(context.get("catalogMappingMongoId")).thenReturn(mongoId);
    String fullUrl = API_URL + "/catalogs/" + mongoId;

    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(404);

    when(webClient.deleteAbs(eq(fullUrl))).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<Integer> result = dispatcherClient.deleteCatalogMappingByMongoId(context);

    assertTrue(result.succeeded());
    assertEquals(404, result.result());

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("was not found for")));
  }

  @Test
  void testDeleteCatalogMappingByMongoId_failure() {
    String mongoId = "mongoFail";
    RoutingContext context = mock(RoutingContext.class);
    when(context.get("catalogMappingMongoId")).thenReturn(mongoId);
    String fullUrl = API_URL + "/catalogs/" + mongoId;

    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(500);
    when(response.statusMessage()).thenReturn("Internal Server Error");

    when(webClient.deleteAbs(eq(fullUrl))).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<Integer> result = dispatcherClient.deleteCatalogMappingByMongoId(context);

    assertTrue(result.failed());
    assertTrue(result.cause().getMessage().contains("Failed to delete catalog mapping"));

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(
      logMessages.stream().anyMatch(msg -> msg.contains("Failed to delete catalog mapping")));
  }

  @Test
  void testDeleteCatalogMappingByMongoId_sendFailure() {
    String mongoId = "mongoError";
    RoutingContext context = mock(RoutingContext.class);
    when(context.get("catalogMappingMongoId")).thenReturn(mongoId);
    String fullUrl = API_URL + "/catalogs/" + mongoId;

    when(webClient.deleteAbs(eq(fullUrl))).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.failedFuture("Connection refused"));

    Future<Integer> result = dispatcherClient.deleteCatalogMappingByMongoId(context);

    assertTrue(result.failed(), "Expected the future to fail");
    assertTrue(result.cause().getMessage().contains("Connection refused"),
      "Expected 'Connection refused' in the error message");

    List<String> logMessages = testAppender.getLogMessages();
    logMessages.forEach(System.out::println);
    assertTrue(
      logMessages.stream().anyMatch(msg -> msg.contains("Failed to delete catalog mapping")));
  }


  @Test
  void testGetAllCatalogueMapSuccess() {
    // given
    String body = "<html><body>Catalogue Map OK</body></html>";
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(Buffer.buffer(body));

    // mock WebClient behavior
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    doAnswer(invocation -> {
      io.vertx.core.Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(io.vertx.core.Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(), any());

    // when
    Future<String> result = dispatcherClient.getAllCatalogueMap();

    // then
    assertTrue(result.succeeded());
    assertEquals("Catalogue Map OK", result.result());
  }

  @Test
  void testGetAllCatalogueMapNotFound() {
    // given
    when(httpResponse.statusCode()).thenReturn(404);
    when(httpResponse.statusMessage()).thenReturn("error");

    // mock WebClient behavior
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    doAnswer(invocation -> {
      io.vertx.core.Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(io.vertx.core.Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(), any());

    // when
    Future<String> result = dispatcherClient.getAllCatalogueMap();

    // then
    assertTrue(result.failed());
    String error = result.cause().getMessage();
    assertTrue(error.contains("errorCode"));
    assertTrue(error.contains("errorDetails"));
  }

  @Test
  void testGetAllCatalogueMapFailure() {
    // given
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    doAnswer(invocation -> {
      io.vertx.core.Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(io.vertx.core.Future.failedFuture("connection refused"));
      return null;
    }).when(httpRequest).sendBuffer(any(), any());

    // when
    Future<String> result = dispatcherClient.getAllCatalogueMap();

    // then
    assertTrue(result.failed());
    assertTrue(result.cause().getMessage().contains("errorCode"));
  }


  @Test
  void testCreateCatalogueMapSuccess() {
    // given
    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());
    RoutingContext context = mock(RoutingContext.class);
    when( context.body()).thenReturn(requestBody);
    when( context.body(). asJsonObject()).thenReturn(new JsonObject().put(STATUS,
      new JsonObject().put("key", "value")));

    when(httpResponse.bodyAsString()).thenReturn("error");


    //    context.body().asJsonObject()

    // Mock WebClient behavior
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    doAnswer(invocation -> {
      io.vertx.core.Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    // when
    Future<String> result = dispatcherClient.createCatalogueMap(context);

    // then
    assertTrue(result.succeeded());
  }

  @Test
  void testCreateCatalogueMapFailureStatus() {
    // given: server responds with non-CREATED status
    when(httpResponse.statusCode()).thenReturn(400);
    when(httpResponse.statusMessage()).thenReturn("error");
    RoutingContext context = mock(RoutingContext.class);
    when( context.body()).thenReturn(requestBody);

    when( context.body(). asJsonObject()).thenReturn(new JsonObject().put(STATUS,
      new JsonObject().put("key", "value")));

    // Mock WebClient behavior
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    doAnswer(invocation -> {
      io.vertx.core.Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    // when
    Future<String> result = dispatcherClient.createCatalogueMap(context);

    // then
    assertTrue(result.failed());
  }

  @Test
  void testCreateCatalogueMapFailureClientError() {
    // given: client error (network failure)
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    doAnswer(invocation -> {
      io.vertx.core.Handler<io.vertx.core.AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.failedFuture("connection refused"));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());
    RoutingContext context = mock(RoutingContext.class);
    when( context.body()).thenReturn(requestBody);
    when( context.body(). asJsonObject()).thenReturn(new JsonObject().put(STATUS,
      new JsonObject().put("key", "value")));

    // when
    Future<String> result = dispatcherClient.createCatalogueMap(context);

    // then
    // assert
    assertTrue(result.failed());
    assertThat(result.cause().getMessage(), CoreMatchers.containsString("connection refused"));
  }

}
