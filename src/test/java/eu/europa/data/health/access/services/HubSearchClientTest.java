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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.TestAppender;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HubSearchClientTest {

  @Mock
  private WebClient mockClient;
  @Mock
  private HttpResponse<Buffer> mockResponse;
  @Mock
  private JsonObject mockConfig;
  @Mock
  private HttpRequest<Buffer> mockRequest;
  private HubSearchClient hubSearchClient;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockConfig.getString(Constants.HUB_SEARCH_DATA_URL)).thenReturn("http://example.com");
    hubSearchClient = new HubSearchClient(mockClient, mockConfig);
  }

  @Test
  void testGetDataset_Success() {
    String datasetId = "123";
    JsonObject expectedJson = new JsonObject().put("id", datasetId).put("name", "Dataset 123");

    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.bodyAsJsonObject()).thenReturn(expectedJson);

    Future<JsonObject> result = hubSearchClient.getDataset(datasetId);

    assertTrue(result.succeeded());
    assertEquals(expectedJson, result.result());
  }

  @Test
  void testGetDataset_NotFound() {
    String datasetId = "999";

    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(404);

    Future<JsonObject> result = hubSearchClient.getDataset(datasetId);

    assertTrue(result.succeeded());
    JsonObject expectedJson = new JsonObject().put("external_dataset_id", datasetId);
    assertEquals(expectedJson, result.result());
  }

  @Test
  void testGetDataset_Failure() {
    String datasetId = "123";

    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(500);
    when(mockResponse.statusMessage()).thenReturn("123");

    Future<JsonObject> result = hubSearchClient.getDataset(datasetId);

    assertTrue(result.failed());
    assertEquals("Failed to get dataset: 123", result.cause().getMessage());
  }

  @Test
  void testLogging_Success() {
    // Given
    String datasetId = "123";
    JsonObject expectedJson = new JsonObject().put("id", datasetId).put("name", "Dataset 123");

    // Mock the WebClient behavior
    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.bodyAsJsonObject()).thenReturn(expectedJson);

    TestAppender testAppender = new TestAppender();
    Logger log = LoggerFactory.getLogger(HubSearchClient.class);
    testAppender.start();
    ((ch.qos.logback.classic.Logger) log).addAppender(testAppender);

    hubSearchClient.getDataset(datasetId).result();

    List<String> logs = testAppender.getLogMessages();
    assertTrue(logs.stream().anyMatch(logMessage -> logMessage.contains("Dataset downloaded successfully")));
  }

  @Test
  void testGetAllCatalogues_success() {
    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.addQueryParam("filter", "catalogue")).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.bodyAsJsonObject()).thenReturn(new JsonObject().put("type", "catalogues"));

    JsonObject result = hubSearchClient.getAllCatalogues().result();

    assertEquals("catalogues", result.getString("type"));
  }

  @Test
  void testGetAllCatalogues_failure() {
    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.addQueryParam("filter", "catalogue")).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(503);
    when(mockResponse.statusMessage()).thenReturn("Service Unavailable");
    when(mockResponse.bodyAsString()).thenReturn("down");

    Future<JsonObject> future = hubSearchClient.getAllCatalogues();

    assertTrue(future.failed());
    assertTrue(future.cause().getMessage().contains("Failed to get catalogues: Service Unavailable"));
  }

  @Test
  void testGetCatalogue_success() {
    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.bodyAsJsonObject()).thenReturn(new JsonObject().put("id", "cat1"));

    JsonObject result = hubSearchClient.getCatalogue("cat1").result();

    assertEquals("cat1", result.getString("id"));
  }

  @Test
  void testGetCatalogue_notFound() {
    when(mockClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(404);
    JsonObject result = hubSearchClient.getCatalogue("cat404").result();

    assertEquals("cat404", result.getString("catalogue_id"));
  }

  @Test
  void testGetCatalogue_failure() {
    when(mockClient.getAbs(anyString())).thenReturn( mockRequest);
    when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(500);
    when(mockResponse.statusMessage()).thenReturn("Server Down");
    when(mockResponse.bodyAsString()).thenReturn("fail");

    Future<JsonObject> future = hubSearchClient.getCatalogue("catX");

    assertTrue(future.failed());
    assertTrue(future.cause().getMessage().contains("Failed to get catalogue: Server Down"));
  }

}
