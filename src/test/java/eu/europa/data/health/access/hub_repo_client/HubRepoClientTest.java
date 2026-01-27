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
package eu.europa.data.health.access.hub_repo_client;

import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.CATALOGUE_LANGUAGE;
import static eu.europa.data.health.access.Constants.CATALOGUE_NAME;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.PUBLISHER_HOMEPAGE_URL;
import static eu.europa.data.health.access.Constants.PUBLISHER_NAME;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.UPLOAD_RDF;
import static eu.europa.data.health.access.hub_repo_client.HubRepoClient.ERROR_CODE;
import static eu.europa.data.health.access.hub_repo_client.HubRepoClient.ERROR_DETAILS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import eu.europa.data.health.access.TestAppender;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.HubSearchClient;
import eu.europa.data.health.access.services.HubStoreClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;

@ExtendWith(MockitoExtension.class)
class HubRepoClientTest {

  private static final String API_URL = "http://example.com";
  private static final String API_KEY = "dummy-key";
  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";
  @Mock
  private WebClient webClient;
  @InjectMocks
  private HubRepoClient hubRepoClient;
  @Mock
  private DispatcherClient dispatcherClient;
  @Mock
  private HubStoreClient hubStoreClient;
  @Mock
  private RoutingContext routingContext;
  @Mock
  private HttpRequest<Buffer> httpRequest;
  private TestAppender testAppender;
  @Mock
  private HttpResponse<Buffer> httpResponse;
  @Mock
  private HubSearchClient hubSearchClient;
  @Mock
  private AuditLogManager auditLogManager;
  @Mock
  private AuthClient authClient;


  private static @NotNull String getJsonString() {
    return """
      {
      "data": [
        {
          "catalogIds": ["catalogueId1", "catalogueId2"],
          "recipient": "Recipient1",
          "isProduction": true
        },
        {
          "catalogIds": ["catalogueId3"],
          "recipient": "Recipient2",
          "isProduction": false
        }
      ]
      }
      """;
  }

  @BeforeEach
  void setUp() {
    Logger mockLogger = LoggerFactory.getLogger(HubRepoClient.class);
    testAppender = new TestAppender();
    testAppender.start();
    ((ch.qos.logback.classic.Logger) mockLogger).addAppender(testAppender);

    Vertx vertx = Vertx.vertx();
    MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.2");
    mongoDBContainer.start();
    // Configure MongoClient with the connection string
    JsonObject config = new JsonObject().put("connection_string",
      mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE));
    MongoClient mongoClient = MongoClient.createShared(vertx, config);
    DBHandler dbHandler = new DBHandler(mongoClient);
    CountriesManager countriesManager = new CountriesManager();
    hubRepoClient = new HubRepoClient(dbHandler, API_URL, API_KEY, webClient, dispatcherClient,
      hubStoreClient,hubSearchClient,countriesManager, auditLogManager);
  }

  @Test
  void testCreateCatalogueSuccess() {
    JsonObject country = new JsonObject();
    country.put("key", "http://publications.europa.eu/resource/authority/country/AZE");
    country.put("value", "Azerbaijan");

    JsonObject testRDF = new JsonObject().put("key", "a");
    JsonObject bodyJson = new JsonObject().put("rdfTemplate",
        new JsonObject().put("key", "rdfTemplateValue")).put(UPLOAD_RDF, testRDF)
      .put("title", "test title").put("description", "description test").put("country", country)
      .put("homepageURL", "www.homepageURLtest.com").put("name", "name test")
      .put(CATALOGUE_NAME, "test catalogue")
      .put("geographicalCoverage", new JsonObject().put("key", "keyValue"))
      .put(PUBLISHER_HOMEPAGE_URL, "www.publisherURLtest.com")
      .put(PUBLISHER_NAME, "publisher name test").put(CATALOGUE_ID, "catalogueId")
      .put(STATUS, new JsonObject().put("key", "keyValue"));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(bodyJson);

    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());

    when(webClient.putAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    when(dispatcherClient.createCatalogueMap(routingContext)).thenReturn(
      Future.succeededFuture(getJsonString()));

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    when(httpResponse.bodyAsString()).thenReturn("test");

    when(hubStoreClient.getFile(any(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(httpResponse));

    Future<String> result = hubRepoClient.createCatalogue(routingContext);

    assertTrue(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(
      logMessages.stream().anyMatch(msg -> msg.contains("Successfully put catalogue with id")));
  }

  @Test
  void testCreateCatalogueSuccess2() {
    JsonObject country = new JsonObject();
    country.put("key", "http://publications.europa.eu/resource/authority/country/AZE");
    country.put("value", "Azerbaijan");

    JsonObject testRDF = new JsonObject().put("key", "b");
    JsonObject bodyJson = new JsonObject().put("rdfTemplate",
        new JsonObject().put("key", "rdfTemplateValue")).put(UPLOAD_RDF, testRDF)
      .put("title", "test title").put("description", "description test").put("country", country)
      .put("homepageURL", "www.homepageURLtest.com").put("name", "name test")
      .put(CATALOGUE_NAME, "test catalogue")
      .put(CATALOGUE_LANGUAGE, "en")
      .put("geographicalCoverage", new JsonObject().put("key", "keyValue"))
      .put(PUBLISHER_HOMEPAGE_URL, "www.publisherURLtest.com")
      .put(PUBLISHER_NAME, "publisher name test").put(CATALOGUE_ID, "catalogueId")
      .put(STATUS, new JsonObject().put("key", "keyValue"));

    JsonObject successResponse = new JsonObject().put("message", "Catalogue created");

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(bodyJson);

    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());

    when(webClient.putAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NO_CONTENT.code());

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    when(dispatcherClient.getCatalogMappingByCatalogId(anyString())).thenReturn(
      Future.succeededFuture(successResponse));

    when(dispatcherClient.updateCatalogueMap(any())).thenReturn(
      Future.succeededFuture(getJsonString()));

    when(hubStoreClient.uploadFile(anyString(), anyString(), anyString(), any(),
      anyString())).thenReturn(Future.succeededFuture(bodyJson));

    Future<String> result = hubRepoClient.createCatalogue(routingContext);

    assertTrue(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Update catalogue map with id")));
  }

  @Test
  void testCreateCatalogueSuccess3() {
    JsonObject country = new JsonObject();
    country.put("key", "http://publications.europa.eu/resource/authority/country/AZE");
    country.put("value", "Azerbaijan");

    JsonObject testRDF = new JsonObject().put("key", "b");
    JsonObject bodyJson = new JsonObject().put("rdfTemplate",
        new JsonObject().put("key", "rdfTemplateValue")).put(UPLOAD_RDF, testRDF)
      .put("title", "test title").put("description", "description test").put("country", country)
      .put("homepageURL", "www.homepageURLtest.com").put("name", "name test")
      .put(CATALOGUE_NAME, "test catalogue")
      .put(CATALOGUE_LANGUAGE, "en")
      .put("geographicalCoverage", new JsonObject().put("key", "keyValue"))
      .put(PUBLISHER_HOMEPAGE_URL, "www.publisherURLtest.com")
      .put(PUBLISHER_NAME, "publisher name test").put(CATALOGUE_ID, "catalogueId")
      .put(STATUS, new JsonObject().put("key", "keyValue"));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(bodyJson);

    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());

    when(webClient.putAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NO_CONTENT.code());

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    when(dispatcherClient.getCatalogMappingByCatalogId(anyString())).thenReturn(
      Future.failedFuture(new RuntimeException()));

    when(hubStoreClient.uploadFile(anyString(), anyString(), anyString(), any(),
      anyString())).thenReturn(Future.succeededFuture(bodyJson));

    Future<String> result = hubRepoClient.createCatalogue(routingContext);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(
      logMessages.stream().anyMatch(msg -> msg.contains("Could not retrieve catalog mapping")));
  }


  @Test
  void testCreateCatalogueSuccess4() {
    JsonObject country = new JsonObject();
    country.put("key", "http://publications.europa.eu/resource/authority/country/AZE");
    country.put("value", "Azerbaijan");

    JsonObject testRDF = new JsonObject().put("key", "b");
    JsonObject bodyJson = new JsonObject().put("rdfTemplate",
        new JsonObject().put("key", "rdfTemplateValue")).put(UPLOAD_RDF, testRDF)
      .put("title", "test title").put("description", "description test").put("country", country)
      .put("homepageURL", "www.homepageURLtest.com").put("name", "name test")
      .put(CATALOGUE_NAME, "test catalogue")
      .put(CATALOGUE_LANGUAGE, "en")
      .put("geographicalCoverage", new JsonObject().put("key", "keyValue"))
      .put(PUBLISHER_HOMEPAGE_URL, "www.publisherURLtest.com")
      .put(PUBLISHER_NAME, "publisher name test").put(CATALOGUE_ID, "catalogueId")
      .put(STATUS, new JsonObject().put("key", "keyValue"));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(bodyJson);

    when(webClient.putAbs(anyString())).thenReturn(httpRequest);

    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      handler.handle(Future.failedFuture(new RuntimeException("Test exception")));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    when(hubStoreClient.uploadFile(anyString(), anyString(), anyString(), any(),
      anyString())).thenReturn(Future.succeededFuture(bodyJson));

    Future<String> result = hubRepoClient.createCatalogue(routingContext);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains(
      "HUB_REPO_CLIENT Failed to put catalogue: catalogueId to hub repo, error message: Test exception")));
  }


  @Test
  void testCreateCatalogueSuccess5() {
    JsonObject country = new JsonObject();
    country.put("key", "http://publications.europa.eu/resource/authority/country/AZE");
    country.put("value", "Azerbaijan");

    JsonObject testRDF = new JsonObject().put("key", "b");
    JsonObject bodyJson = new JsonObject().put("rdfTemplate",
        new JsonObject().put("key", "rdfTemplateValue")).put(UPLOAD_RDF, testRDF)
      .put("title", "test title").put("description", "description test").put("country", country)
      .put("homepageURL", "www.homepageURLtest.com").put("name", "name test")
      .put(CATALOGUE_NAME, "test catalogue")
      .put(CATALOGUE_LANGUAGE, "en")
      .put("geographicalCoverage", new JsonObject().put("key", "keyValue"))
      .put(PUBLISHER_HOMEPAGE_URL, "www.publisherURLtest.com")
      .put(PUBLISHER_NAME, "publisher name test").put(CATALOGUE_ID, "catalogueId")
      .put(STATUS, new JsonObject().put("key", "keyValue"));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(bodyJson);

    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.CREATED.code());

    when(webClient.putAbs(anyString())).thenReturn(httpRequest);

    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NOT_FOUND.code());
      when(httpResponse.statusMessage()).thenReturn("Error");

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(any(Buffer.class), any());

    when(hubStoreClient.uploadFile(anyString(), anyString(), anyString(), any(),
      anyString())).thenReturn(Future.succeededFuture(bodyJson));

    Future<String> result = hubRepoClient.createCatalogue(routingContext);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream()
      .anyMatch(msg -> msg.contains("Hub repo failed to put catalogue with id")));
  }


  @Test
  void testCreateCatalogueFailure_Due_to_RDF_not_Valid() {
    JsonObject country = new JsonObject();
    country.put("key", "http://publications.europa.eu/resource/authority/country/AZE");
    country.put("value", "Azerbaijan");

    JsonObject bodyJson = new JsonObject().put("title", "test title")
      .put("description", "description test").put("country", country)
      .put("homepageURL", "www.homepageURLtest.com").put("name", "name test")
      .put(CATALOGUE_NAME, "test catalogue")
      .put("geographicalCoverage", new JsonObject().put("key", "keyValue"))
      .put(PUBLISHER_HOMEPAGE_URL, "www.publisherURLtest.com")
      .put(PUBLISHER_NAME, "publisher name test").put(CATALOGUE_ID, "catalogueId");

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(bodyJson);

    hubRepoClient.createCatalogue(routingContext);

    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream()
      .anyMatch(msg -> msg.contains("The RDF file not provided or not valid.")));
  }

  @Test
  void testGetCatalogueSuccess() {
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NO_CONTENT.code());

      // Create the catalogueJson (including @graph)
      JsonObject catalogueJson = new JsonObject();
      JsonArray graphArray = new JsonArray();
      graphArray.add(new JsonObject().put("name", "Item 1"));
      graphArray.add(new JsonObject().put("name", "Item 2"));
      catalogueJson.put("@graph", graphArray);

      // Create the catalogueMapList with 'data' array (this is your mock data for dispatcherMapToCatalogue)
      JsonArray dataArray = new JsonArray();
      JsonObject dataObject1 = new JsonObject().put("catalogIds",
          new JsonArray().add("catalogueId1")).put("recipient", "Recipient1")
        .put("isProduction", true);
      JsonObject dataObject2 = new JsonObject().put("catalogIds",
          new JsonArray().add("catalogueId2")).put("recipient", "Recipient2")
        .put("isProduction", false);

      dataArray.add(dataObject1);
      dataArray.add(dataObject2);

      // Add the catalogueMapList data to the catalogueJson as part of the response
      catalogueJson.put("data", dataArray);

      // Convert the JsonObject to a string
      String responseBody = catalogueJson.encodePrettily();

      Buffer buffer = Buffer.buffer(responseBody);
      when(httpResponse.body()).thenReturn(buffer);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123_id");

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);

    String jsonString = getJsonString();

    when(dispatcherClient.getAllCatalogueMap()).thenReturn(Future.succeededFuture(jsonString));

    Future<JsonObject> result = hubRepoClient.getCatalogue(routingContext);

    assertTrue(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(
      logMessages.stream().anyMatch(msg -> msg.contains("Successfully got catalogue with id:")));
  }

  @Test
  void testGetCatalogueFailure() {
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      handler.handle(Future.failedFuture("fail"));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123_id");

    Future<JsonObject> result = hubRepoClient.getCatalogue(routingContext);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Failed to get catalogue:")));
  }


  @Test
  void testGetCatalogueFailure2() {
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NOT_FOUND.code());

      // Create the catalogueJson (including @graph)
      JsonObject catalogueJson = new JsonObject();
      JsonArray graphArray = new JsonArray();
      graphArray.add(new JsonObject().put("name", "Item 1"));
      graphArray.add(new JsonObject().put("name", "Item 2"));
      catalogueJson.put("@graph", graphArray);

      // Create the catalogueMapList with 'data' array (this is your mock data for dispatcherMapToCatalogue)
      JsonArray dataArray = new JsonArray();
      JsonObject dataObject1 = new JsonObject().put("catalogIds",
          new JsonArray().add("catalogueId1")).put("recipient", "Recipient1")
        .put("isProduction", true);
      JsonObject dataObject2 = new JsonObject().put("catalogIds",
          new JsonArray().add("catalogueId2")).put("recipient", "Recipient2")
        .put("isProduction", false);

      dataArray.add(dataObject1);
      dataArray.add(dataObject2);

      // Add the catalogueMapList data to the catalogueJson as part of the response
      catalogueJson.put("data", dataArray);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123_id");

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);

    when(httpResponse.statusMessage()).thenReturn("Error");

    Future<JsonObject> result = hubRepoClient.getCatalogue(routingContext);

    assertTrue(result.failed());
  }

  @Test
  void testDeleteCatalogueSuccess() {
    when(webClient.deleteAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NO_CONTENT.code());

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123_id");

    Future<String> result = hubRepoClient.deleteCatalogue(routingContext);

    assertTrue(result.succeeded());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(
      logMessages.stream().anyMatch(msg -> msg.contains("Successfully delete catalogue with id:")));
  }

  @Test
  void testDeleteCatalogueSuccess2() {
    when(webClient.deleteAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.NOT_FOUND.code());
      when(httpResponse.bodyAsString()).thenReturn("Error");

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123_id");

    Future<String> result = hubRepoClient.deleteCatalogue(routingContext);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Failed to delete catalogue:")));
  }

  @Test
  void testDeleteCatalogueFailure() {
    when(webClient.deleteAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      handler.handle(Future.failedFuture("fail"));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123_id");

    Future<String> result = hubRepoClient.deleteCatalogue(routingContext);

    assertTrue(result.failed());
    List<String> logMessages = testAppender.getLogMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Failed to delete catalogue:")));
  }

  @Test
  void testGetAllGeographicalCoverageSuccess() {
    RoutingContext context = mock(RoutingContext.class);

    // Mock response JSON
    JsonObject responseBody = new JsonObject().put("data", new JsonArray());
    when(httpResponse.body()).thenReturn(Buffer.buffer(responseBody.encode()));

    // Mock WebClient and request
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    // Mock catalogue mapper
    JsonObject expectedMapped = new JsonObject().put("mapped", true);
    try (MockedStatic<CatalogueMapper> mapperMock = Mockito.mockStatic(CatalogueMapper.class)) {
      mapperMock.when(() -> CatalogueMapper.mapGeographicalCoverageList(any(), any()))
        .thenReturn(expectedMapped);

      Future<JsonObject> resultFuture = hubRepoClient.getAllGeographicalCoverage();

      resultFuture.onComplete(ar -> {
        assertTrue(ar.succeeded());
        assertEquals(expectedMapped, ar.result());
      });
    }
  }

  @Test
  void testGetAllGeographicalCoverageFailure() {
    RoutingContext context = mock(RoutingContext.class);

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.failedFuture("Simulated failure"));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    Future<JsonObject> resultFuture = hubRepoClient.getAllGeographicalCoverage();

    resultFuture.onComplete(ar -> {
      assertTrue(ar.failed());
      JsonObject error = new JsonObject(ar.cause().getMessage());
      assertTrue(error.containsKey(ERROR_CODE));
      assertTrue(error.containsKey(ERROR_DETAILS));
    });
  }

  @Test
  void testGetAllCatalogueSuccess() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(context.request()).thenReturn(request);
    when(request.getParam("page")).thenReturn("1");
    when(request.getParam("limit")).thenReturn("10");
    when(request.getParam("sortBy")).thenReturn("name");
    when(request.getParam("sortOrder")).thenReturn("asc");

    JsonObject catalogueJson = new JsonObject().put("catalogueList",
      new JsonArray().add(new JsonObject())).put("sizeOfCatalogueList", 1);
    // Mock response from hub repo

    when(httpResponse.body()).thenReturn(Buffer.buffer(new JsonObject().encode()));
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    // Mock static method CatalogueMapper.map
    try (MockedStatic<CatalogueMapper> mapperMock = Mockito.mockStatic(CatalogueMapper.class)) {
      mapperMock.when(() -> CatalogueMapper.map(any(), any(), any(), any(),any()))
        .thenReturn(Future.succeededFuture(catalogueJson));

      // Mock prepareGetEntityResponse (if not static, spy and override it)
      JsonObject expectedResponse = new JsonObject().put("result", "success");
      HubRepoClient spyClient = Mockito.spy(hubRepoClient);
      doReturn(expectedResponse).when(spyClient)
        .prepareGetEntityResponse(anyList(), any());

      when(dispatcherClient.getAllCatalogueMap()).thenReturn(
        Future.succeededFuture(getJsonString()));
      Future<JsonObject> resultFuture = spyClient.getAllCatalogue(context);

      resultFuture.onComplete(ar -> {
        assertTrue(ar.succeeded());
        assertEquals(expectedResponse, ar.result());
      });
    }
  }

  @Test
  void testGetAllCatalogueFailsWhenHttpFails() {
    RoutingContext context = mock(RoutingContext.class);

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.failedFuture("Hub repo error"));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    Future<JsonObject> result = hubRepoClient.getAllCatalogue(context);
    result.onComplete(ar -> {
      assertTrue(ar.failed());
      String msg = ar.cause().getMessage();
      assertTrue(msg.contains(ERROR_CODE) || msg.contains("Hub repo error"));
    });
  }

  @Test
  void testExistsCatalogueByDomibusIdOrCatalogueIdOrCountrySuccess() {
    RoutingContext context = mock(RoutingContext.class);
    when(context.queryParams()).thenReturn(
      MultiMap.caseInsensitiveMultiMap().add("catalogueId", "test-cat-id"));

    JsonObject requestBody = new JsonObject().put(CATALOGUE_ID, "test-cat-id")
      .put(DOMIBUS_ID, "dom-id").put(UPLOAD_RDF, new JsonObject().put("key", "b"))
      .put("geographicalCoverage", new JsonObject().put("key", "coverage"));

    when(context.body()).thenReturn(mock(RequestBody.class));
    when(context.body().asJsonObject()).thenReturn(requestBody);

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(any(), any())).thenReturn(httpRequest);

    when(httpResponse.body()).thenReturn(Buffer.buffer(new JsonObject().encode()));

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendBuffer(eq(null), any());

    JsonObject expectedResult = new JsonObject().put("exists", true);

    try (MockedStatic<CatalogueMapper> mapperMock = Mockito.mockStatic(CatalogueMapper.class)) {
      mapperMock.when(
        () -> CatalogueMapper.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(any(), any(), any(),
          any(), anyString())).thenReturn(expectedResult);

      when(dispatcherClient.getAllCatalogueMap()).thenReturn(
        Future.succeededFuture(getJsonString()));

      hubRepoClient.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(context).onComplete(ar -> {
        assertTrue(ar.succeeded());
        assertEquals(expectedResult, ar.result());
      });
    }
  }

  @Test
  void testExistsCatalogueFailsWhenDispatcherFails() {
    RoutingContext context = mock(RoutingContext.class);
    MultiMap queryParams = MultiMap.caseInsensitiveMultiMap().add("catalogueId", "test-cat-id");
    when(context.queryParams()).thenReturn(queryParams);

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);

    hubRepoClient.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(context).onComplete(ar -> {
      assertTrue(ar.failed());
      assertEquals("{\"errorCode\":500,\"errorDetails\":\"Dispatcher failed\"}",
        ar.cause().getMessage());
    });
  }

  @Test
  void testGetDatasetsByCatalogIdSuccess() {
    String catalogId = "test-catalog-id";

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);

    JsonArray mockDatasetArray = new JsonArray().add(new JsonObject().put("id", "ds1"));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonArray()).thenReturn(mockDatasetArray);

    Promise<HttpResponse<Buffer>> responsePromise = Promise.promise();
    when(httpRequest.send()).thenReturn(responsePromise.future());

    Future<JsonArray> future = hubRepoClient.getDatasetsByCatalogId(catalogId);

    responsePromise.complete(httpResponse);

    future.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(mockDatasetArray, ar.result());
    });
  }


  @Test
  void testGetDatasetsByCatalogIdFailure() {
    String catalogId = "test-catalog-id";

    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);

    when(httpResponse.statusCode()).thenReturn(404);

    Promise<HttpResponse<Buffer>> responsePromise = Promise.promise();
    when(httpRequest.send()).thenReturn(responsePromise.future());

    Future<JsonArray> future = hubRepoClient.getDatasetsByCatalogId(catalogId);

    responsePromise.complete(httpResponse);

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      assertTrue(future.failed());
      assertTrue(future.cause().getMessage().contains("Failed to get datasets"));
    });

  }

}

