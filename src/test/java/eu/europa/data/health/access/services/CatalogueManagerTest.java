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

import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_repo_client.HubRepoClient;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@ExtendWith(VertxExtension.class)
class CatalogueManagerTest {

  @Mock
  private DBHandler dbHandler;
  @Mock
  private HubRepoClient hubRepoClient;
  @Mock
  private HubStoreClient hubStoreClient;
  @Mock
  private HubSearchClient hubSearchClient;
  @Mock
  private DispatcherClient dispatcherClient;
  @Mock
  private RoutingContext routingContext;
  @Mock
  private HttpResponse<Buffer> httpResponse;
  @Mock
  private HttpServerResponse response;
  @Mock
  private HttpServerRequest request;
  @Mock
  private AuditLogManager auditLogManager;

  private CatalogueManager catalogueManager;


  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    Vertx vertx = Vertx.vertx();
    catalogueManager = new CatalogueManager(dbHandler, hubRepoClient, hubStoreClient, dispatcherClient, vertx, auditLogManager);

    // Mock other behaviors
    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
  }

  @Test
  void testValidateUpdateCatalogue_withValidationErrors(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(List.of("missing field"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      assertTrue(responseBody.contains("Create catalogue not stored due to validation errors"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.validateUpdateCatalogue(routingContext);
  }

  @Test
  void testValidateUpdateCatalogue_withoutValidationErrors(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(null);

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      assertTrue(responseBody.contains("Create catalogue can be continue"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.validateUpdateCatalogue(routingContext);
  }

  @Test
  void testValidateUpdateCatalogue_failure_dueToValidationErrors(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(List.of("Invalid title"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);

      // Check that the correct error message is returned
      assertTrue(responseBody.contains("Create catalogue not stored due to validation errors"));
      testContext.completeNow();
      return null;
    });

    // Call the method
    catalogueManager.validateUpdateCatalogue(routingContext);
  }

  @Test
  void testValidateCreateCatalogue_noErrors(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(null);
    when(hubRepoClient.createCatalogue(routingContext)).thenReturn(Future.succeededFuture("Catalogue created"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).then(invocation -> {
      String message = invocation.getArgument(0);
      assertTrue(message.contains("Catalogue created"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.createCatalogue(routingContext);
  }

  @Test
  void testCreateCatalogue_withValidationErrors(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(List.of("validation failed"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).then(invocation -> {
      String message = invocation.getArgument(0);
      assertTrue(message.contains("validation errors"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.createCatalogue(routingContext);
  }

  @Test
  void testCreateCatalogue_failure_withJsonError(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(null);

    JsonObject errorJson = new JsonObject()
      .put("errorCode", 409)
      .put("errorDetails", "Conflict creating catalogue");

    when(hubRepoClient.createCatalogue(routingContext))
      .thenReturn(Future.failedFuture(errorJson.encode()));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).then(invocation -> {
      String message = invocation.getArgument(0);
      assertTrue(message.contains("Conflict creating catalogue"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.createCatalogue(routingContext);
  }

  @Test
  void testCreateCatalogue_failure_withUnknownError(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(null);
    when(hubRepoClient.createCatalogue(routingContext))
      .thenReturn(Future.failedFuture("unstructured error"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).then(invocation -> {
      String message = invocation.getArgument(0);
      assertTrue(message.contains("unknown error"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.createCatalogue(routingContext);
  }

  @Test
  void testUpdateCatalogue_success(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(null);

    when(hubRepoClient.createCatalogue(routingContext))
      .thenReturn(Future.succeededFuture("Catalogue created successfully"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      assertTrue(responseBody.contains("Catalogue created successfully"));
      testContext.completeNow();
      return null;
    });

    // Call the updateCatalogue method
    catalogueManager.updateCatalogue(routingContext);
  }

  @Test
  void testUpdateCatalogue_failure_dueToValidationErrors(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(List.of("Invalid data"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      assertTrue(responseBody.contains("Create catalogue not stored due to validation errors"));
      testContext.completeNow();
      return null;
    });

    // Call the updateCatalogue method
    catalogueManager.updateCatalogue(routingContext);
  }

  @Test
  void testGetAllCatalogue_success(VertxTestContext testContext) {
    when(hubRepoClient.getAllCatalogue(any())).thenReturn(Future.succeededFuture(new JsonObject()));

    catalogueManager.getAllCatalogues(routingContext);

    verify(hubRepoClient).getAllCatalogue(routingContext);
    testContext.completeNow();
  }

  @Test
  void testGetRDFFileMetadata_success(VertxTestContext testContext) {
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123");
    when(hubStoreClient.getFile(any(), any(), any()))
      .thenReturn(Future.succeededFuture(httpResponse));

    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
      .add("content-disposition", "attachment; filename=test.rdf")
      .add("filename", "test.rdf");
    when(httpResponse.headers()).thenReturn(headers);

    HttpServerResponse serverResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(serverResponse);
    when(serverResponse.putHeader(anyString(), anyString())).thenReturn(serverResponse);
    when(serverResponse.end(anyString())).thenReturn(null);

    catalogueManager.getRDFFileMetadata(routingContext);

    verify(hubStoreClient).getFile(null, CATALOGUE_ID, "123");
    verify(serverResponse).putHeader(CONTENT_TYPE, "application/json");
    testContext.completeNow();
  }

  @Test
  void testGetAllGeographicalCoverage_success(VertxTestContext testContext) {
    JsonObject mockResult = new JsonObject();
    mockResult.put("country1", "Country 1");
    mockResult.put("country2", "Country 2");

    when(hubRepoClient.getAllGeographicalCoverage())
      .thenReturn(Future.succeededFuture(mockResult));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      // Assert that the response body is the encoded mock result
      assertTrue(responseBody.contains("Country 1"));
      assertTrue(responseBody.contains("Country 2"));
      testContext.completeNow();
      return null;
    });

    // Call the method
    catalogueManager.getAllGeographicalCoverage(routingContext);
  }

  @Test
  void testGetAllGeographicalCoverage_failure(VertxTestContext testContext) {
    when(hubRepoClient.getAllGeographicalCoverage())
      .thenReturn(Future.failedFuture("No countries found."));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      // Assert the response contains the failure message
      assertTrue(responseBody.contains("No countries found."));
      testContext.completeNow();
      return null;
    });

    // Call the method
    catalogueManager.getAllGeographicalCoverage(routingContext);
  }

  @Test
  void testGetRDFFileData_failure(VertxTestContext testContext) {
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("123");
    when(hubStoreClient.getFile(any(), any(), any()))
      .thenReturn(Future.failedFuture("File not found"));

    catalogueManager.getRDFFileData(routingContext);

    verify(hubStoreClient).getFile(null, CATALOGUE_ID, "123");
    testContext.completeNow();
  }

  @Test
  void testDeleteCatalog_success(VertxTestContext testContext) {
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("catalogue-123");

    Future<Integer> dispatcherFuture = Future.succeededFuture(1);
    Future<String> hubRepoFuture = Future.succeededFuture("deleted");
    Future<HttpResponse<Buffer>> hubStoreFuture = Future.succeededFuture(httpResponse);

    when(dispatcherClient.deleteCatalogMappingByMongoId(routingContext)).thenReturn(dispatcherFuture);
    when(hubRepoClient.deleteCatalogue(routingContext)).thenReturn(hubRepoFuture);
    when(hubStoreClient.deleteFile(null, CATALOGUE_ID, "catalogue-123")).thenReturn(hubStoreFuture);
    when(hubRepoClient.getCatalogueTitle(routingContext)).thenReturn( Future.succeededFuture(new JsonObject()) );

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsString()).thenReturn("File deleted");

    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.end(anyString())).then(invocation -> {
      String json = invocation.getArgument(0);
      JsonObject result = new JsonObject(json);
      // Verify the content of the result
      assertEquals(1, result.getInteger("dispatcherCallResult"));
      assertEquals("deleted", result.getString("hubRepoCallResult"));
      assertEquals(200, result.getInteger("hubStoreCallResultStatus"));
      assertEquals("File deleted", result.getString("hubStoreCallResultMessage"));
      testContext.completeNow();
      return null;
    });

    // Call the method under test
    catalogueManager.deleteCatalog(routingContext);
  }

  @Test
  void testDeleteCatalog_failure_dispatcherCall(VertxTestContext testContext) {
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("catalogue-123");

    // Dispatcher fails
    Future<Integer> dispatcherFuture = Future.failedFuture("Dispatcher error");
    Future<String> hubRepoFuture = Future.succeededFuture("deleted");
    Future<HttpResponse<Buffer>> hubStoreFuture = Future.succeededFuture(httpResponse);

    when(dispatcherClient.deleteCatalogMappingByMongoId(routingContext)).thenReturn(dispatcherFuture);
    when(hubRepoClient.deleteCatalogue(routingContext)).thenReturn(hubRepoFuture);
    when(hubStoreClient.deleteFile(null, CATALOGUE_ID, "catalogue-123")).thenReturn(hubStoreFuture);
    when(hubRepoClient.getCatalogueTitle(routingContext)).thenReturn( Future.succeededFuture(new JsonObject()) );

    when(response.end(anyString())).then(invocation -> {
      String errorMessage = invocation.getArgument(0);
      assertTrue(errorMessage.contains("Dispatcher error"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.deleteCatalog(routingContext);
  }

  @Test
  void testDeleteCatalog_failure_hubRepoCall(VertxTestContext testContext) {
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("catalogue-123");

    Future<Integer> dispatcherFuture = Future.succeededFuture(1);
    Future<String> hubRepoFuture = Future.failedFuture("HubRepo error");
    Future<HttpResponse<Buffer>> hubStoreFuture = Future.succeededFuture(httpResponse);

    when(dispatcherClient.deleteCatalogMappingByMongoId(routingContext)).thenReturn(dispatcherFuture);
    when(hubRepoClient.deleteCatalogue(routingContext)).thenReturn(hubRepoFuture);
    when(hubStoreClient.deleteFile(null, CATALOGUE_ID, "catalogue-123")).thenReturn(hubStoreFuture);
    when(hubRepoClient.getCatalogueTitle(routingContext)).thenReturn( Future.succeededFuture(new JsonObject()) );

    when(response.setStatusCode(500)).thenReturn(response);
    when(response.end(anyString())).then(invocation -> {
      String errorMessage = invocation.getArgument(0);
      assertTrue(errorMessage.contains("HubRepo error"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.deleteCatalog(routingContext);
  }

  @Test
  void testDeleteCatalog_failure_hubStoreCall(VertxTestContext testContext) {
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("catalogue-123");

    Future<Integer> dispatcherFuture = Future.succeededFuture(1);
    Future<String> hubRepoFuture = Future.succeededFuture("deleted");
    Future<HttpResponse<Buffer>> hubStoreFuture = Future.failedFuture("HubStore error");

    when(dispatcherClient.deleteCatalogMappingByMongoId(routingContext)).thenReturn(dispatcherFuture);
    when(hubRepoClient.deleteCatalogue(routingContext)).thenReturn(hubRepoFuture);
    when(hubStoreClient.deleteFile(null, CATALOGUE_ID, "catalogue-123")).thenReturn(hubStoreFuture);
    when(hubRepoClient.getCatalogueTitle(routingContext)).thenReturn( Future.succeededFuture(new JsonObject()) );

    when(response.setStatusCode(500)).thenReturn(response);
    when(response.end(anyString())).then(invocation -> {
      String errorMessage = invocation.getArgument(0);
      assertTrue(errorMessage.contains("HubStore error"));
      testContext.completeNow();
      return null;
    });

    catalogueManager.deleteCatalog(routingContext);
  }

  @Test
  void testGetRDFFileDataForCatalogue_success(VertxTestContext testContext) {
    String catalogueId = "catalogue-123";
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn(catalogueId);

    // Mock file headers
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("catalogueId", "file-456");
    headers.add("filename", "file.rdf");

    // Mock file body
    Buffer body = Buffer.buffer("dummy RDF content");

    when(httpResponse.headers()).thenReturn(headers);
    when(httpResponse.body()).thenReturn(body);
    when(httpResponse.getHeader("Content-Disposition")).thenReturn("filename*=file.rdf");
    when(hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId)).thenReturn(Future.succeededFuture(httpResponse));

    JsonObject result = new JsonObject().put("data", new JsonObject());

    catalogueManager.getRDFFileDataForCatalogue(routingContext, result).onComplete(ar -> {
      assertTrue(ar.succeeded());
      JsonObject updatedResult = ar.result();
      JsonObject data = updatedResult.getJsonObject("data");

      // Check uploadRDF was set
      assertNotNull(data.getJsonObject("uploadRDF"));
      assertEquals("b", data.getJsonObject("uploadRDF").getString("key"));

      // Check rdfTemplate was set
      JsonObject rdfTemplate = data.getJsonObject("rdfTemplate");
      assertNotNull(rdfTemplate);
      assertEquals("file-456", rdfTemplate.getString("id"));
      assertEquals("file.rdf", rdfTemplate.getString("name"));
      testContext.completeNow();
    });
  }

  @Test
  void testGetRDFFileDataForCatalogue_failure(VertxTestContext testContext) {
    String catalogueId = "catalogue-123";
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn(catalogueId);

    when(hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId))
      .thenReturn(Future.failedFuture("Hub store unavailable"));

    JsonObject result = new JsonObject().put("data", new JsonObject());

    catalogueManager.getRDFFileDataForCatalogue(routingContext, result).onComplete(ar -> {
      assertTrue(ar.succeeded());
      JsonObject updatedResult = ar.result();
      JsonObject data = updatedResult.getJsonObject("data");

      // uploadRDF should still be present
      JsonObject uploadRDF = data.getJsonObject("uploadRDF");
      assertNotNull(uploadRDF);
      assertEquals("b", uploadRDF.getString("key"));

      // rdfTemplate should not be present due to failure
      assertNull(data.getJsonObject("rdfTemplate"));

      testContext.completeNow();
    });
  }

  @Test
  void testUploadRDFFile_failure_dueToValidationError(VertxTestContext testContext) {
    when(routingContext.get("errors")).thenReturn(List.of("Validation error"));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      // Assert that the error message contains "File upload not completed because of validation issues"
      assertTrue(responseBody.contains("File upload not completed because of validation issues"));
      testContext.completeNow();
      return null;
    });

    // Call the uploadRDFFile method
    catalogueManager.uploadRDFFile(routingContext);
  }

  @Test
  void testGetCatalogue_success(VertxTestContext testContext) {
    // Mock the hubRepoClient.getCatalogue call
    JsonObject catalogueResult = new JsonObject()
        .put("data", new JsonObject()
            .put("catalogue", new JsonObject()
                .put("id", "catalogue-123")
                .put("title", "Test Catalogue")));

    when(hubRepoClient.getCatalogue(routingContext)).thenReturn(Future.succeededFuture(catalogueResult));

    // Mock the getRDFFileDataForCatalogue call
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn("catalogue-123");

    // Mock the hubStoreClient.getFile call
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("catalogueId", "file-456");
    headers.add("filename", "file.rdf");

    Buffer body = Buffer.buffer("dummy RDF content");

    when(httpResponse.headers()).thenReturn(headers);
    when(httpResponse.body()).thenReturn(body);
    when(httpResponse.getHeader("Content-Disposition")).thenReturn("filename*=file.rdf");
    when(hubStoreClient.getFile(null, CATALOGUE_ID, "catalogue-123")).thenReturn(Future.succeededFuture(httpResponse));

    // Mock the response
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.end(anyString())).thenAnswer(invocation -> {
      String responseBody = invocation.getArgument(0);
      JsonObject result = new JsonObject(responseBody);

      // Verify the response contains the expected data
      assertNotNull(result.getJsonObject("data"));
      assertNotNull(result.getJsonObject("data").getJsonObject("catalogue"));
      assertEquals("catalogue-123", result.getJsonObject("data").getJsonObject("catalogue").getString("id"));
      assertEquals("Test Catalogue", result.getJsonObject("data").getJsonObject("catalogue").getString("title"));

      // Verify the RDF file data was added
      assertNotNull(result.getJsonObject("data").getJsonObject("uploadRDF"));
      assertNotNull(result.getJsonObject("data").getJsonObject("rdfTemplate"));

      testContext.completeNow();
      return null;
    });

    // Call the method
    catalogueManager.getCatalogue(routingContext);
  }

  @Test
  void testGetRDFFileData_success(VertxTestContext testContext) {
    String catalogueId = "catalogue-123";
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn(catalogueId);

    // Mock file headers and body
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Disposition", "attachment; filename=test.rdf");

    Buffer body = Buffer.buffer("dummy RDF content");

    when(httpResponse.headers()).thenReturn(headers);
    when(httpResponse.body()).thenReturn(body);
    when(httpResponse.bodyAsBuffer()).thenReturn(body);
    when(httpResponse.getHeader("Content-Disposition")).thenReturn("attachment; filename=test.rdf");
    when(hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId)).thenReturn(Future.succeededFuture(httpResponse));

    // Mock the response
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.end(any(Buffer.class))).thenAnswer(invocation -> {
      Buffer responseBody = invocation.getArgument(0);
      assertEquals("dummy RDF content", responseBody.toString());
      testContext.completeNow();
      return null;
    });

    // Call the method
    catalogueManager.getRDFFileData(routingContext);
  }

  @Test
  void testDecodeFilename() {
    // Test with filename*=UTF-8''test%20file.rdf
    String contentDisposition = "attachment; filename*=UTF-8''test%20file.rdf";
    String result = catalogueManager.decodeFilename(contentDisposition);
    assertEquals("test file.rdf", result);

    // Test with empty content disposition
    result = catalogueManager.decodeFilename("");
    assertEquals("Error_decoding_filename", result);

    // Test with content disposition without filename*=
    result = catalogueManager.decodeFilename("attachment; filename=test.rdf");
    assertEquals("Error_decoding_filename", result);
  }
}
