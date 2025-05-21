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

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.UtilityServiceTest.getStringJsonObject;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class DocumentExportDataAccessManagerTest {

  @Mock
  private DBHandler dbHandler;
  @Mock
  private WebClient webClient;
  @Mock
  private HttpRequest<Buffer> httpRequest;
  @Mock
  private HttpResponse<Buffer> httpResponse;
  @Mock
  HttpServerResponse serverResponse;
  private JsonObject config;
  RoutingContext routingContext;

  @InjectMocks
  private DocumentExportDataAccessManager documentExportManager;

  @BeforeEach
  void setUp() {
    TranslationManager translationManager = new TranslationManager();
    config = new JsonObject().put("HUB_SEARCH_DATA_URL", "http://mocked-url");
    documentExportManager = new DocumentExportDataAccessManager(translationManager, dbHandler,
      webClient, config);
    routingContext = mock(RoutingContext.class);
    lenient().when(routingContext.pathParam("lg")).thenReturn("en");
    lenient().when(routingContext.pathParam("id")).thenReturn("applicationId");
    lenient().when(routingContext.pathParam("type")).thenReturn("docx");
    lenient().when(routingContext.response()).thenReturn(serverResponse);
    lenient().when(serverResponse.putHeader(anyString(), anyString())).thenReturn(serverResponse);
  }

  @Test
  void exportApplicationForm_other_cases() {
    JsonObject application = new JsonObject(getStringJsonObject(
      "full_data_application_other_cases_sample"));
    exportApplicationForm(application,"docx");
  }


  @Test
  void exportApplicationForm_other_cases2() {
    lenient().when(routingContext.pathParam("type")).thenReturn("pdf");
    JsonObject application = new JsonObject(getStringJsonObject(
      "full_data_application_other_cases_sample_2"));
    exportApplicationForm(application,"pdf");
  }

  @Test
  void exportApplicationForm_some_cases() {
    JsonObject application = new JsonObject(getStringJsonObject("full_data_application"));
    exportApplicationForm(application,"docx");
  }

  void exportApplicationForm(JsonObject application,String type) {
    Future<JsonObject> future = Future.succeededFuture(application);
    when(dbHandler.findById(DATA_ACCESS_APPLICATION_COLLECTION, "applicationId")).thenReturn(
      future);

    // Build the mock JSON response body
    JsonObject mockResponseBody = new JsonObject();
    JsonArray mockResultsArray = new JsonArray();
    JsonObject provenance = new JsonObject()
      .put("label", "Provenance");
    mockResultsArray.add(provenance);
    mockResponseBody.put("provenance", mockResultsArray);
    mockResponseBody.put("title", new JsonObject().put("en", "title datasheet"));
    mockResponseBody.put("country", new JsonObject().put("label", "GR"));
    mockResponseBody.put("publisher", new JsonObject().put("name", "publisher name"));
    mockResponseBody.put("hdab", new JsonObject().put("name", "hdab name"));
    mockResponseBody.put("catalog", new JsonObject()
      .put("id", "726200a0-7940-4446-b5f8-a62e6cc00b1b"));

    JsonObject mockResultsBody = new JsonObject()
      .put("result", mockResponseBody);
    when(webClient.getAbs(config.getString(Constants.HUB_SEARCH_DATA_URL) + "/datasets/"
      + "8ddbd740-0c51-4a65-8ae8-0f03bb6fef5e")).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonObject()).thenReturn(mockResultsBody);

    documentExportManager.exportApplicationDataAccessForm(routingContext);

    if(type.equals("pdf")) {
      verify(serverResponse).putHeader("Content-Type", "application/pdf");
      verify(serverResponse).putHeader(eq("Content-Disposition"), contains("attachment; filename=Form.pdf"));
    }
    else{
    verify(serverResponse).putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    verify(serverResponse).putHeader(eq("Content-Disposition"), contains("filename=DataAccessForm.docx"));
    }
    verify(dbHandler, times(1)).findById(DATA_ACCESS_APPLICATION_COLLECTION, "applicationId");

  }

  @Test
  void externalExportApplicationForm() {
    JsonObject application = new JsonObject(getStringJsonObject("full_data_application"));
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "id", "applicationId",
      "type", "pdf"
    );
    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(application,
      pathParams, false);
    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals("applicationId", routingContext.pathParam("id"));
    Assertions.assertEquals("pdf", routingContext.pathParam("type"));

    // Build the mock JSON response body
    JsonObject mockResponseBody = new JsonObject();
    JsonArray mockResultsArray = new JsonArray();
    JsonObject provenance = new JsonObject()
      .put("label", "Provenance");
    mockResultsArray.add(provenance);
    mockResponseBody.put("provenance", mockResultsArray);
    mockResponseBody.put("title", new JsonObject().put("en", "title datasheet"));
    mockResponseBody.put("country", new JsonObject().put("label", "GR"));
    mockResponseBody.put("publisher", new JsonObject().put("name", "publisher name"));
    mockResponseBody.put("hdab", new JsonObject().put("name", "hdab name"));
    mockResponseBody.put("catalog", new JsonObject()
      .put("id", "726200a0-7940-4446-b5f8-a62e6cc00b1b"));

    JsonObject mockResultsBody = new JsonObject()
      .put("result", mockResponseBody);
    // Mock behavior for web client call
    when(webClient.getAbs(config.getString(Constants.HUB_SEARCH_DATA_URL) + "/datasets/"
      + "8ddbd740-0c51-4a65-8ae8-0f03bb6fef5e")).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonObject()).thenReturn(mockResultsBody);

    documentExportManager.externalExportApplicationForm(routingContext);
    verify(routingContext.response()).end(any(Buffer.class));

  }


  @Test
  void externalExportApplicationForm2() {
    JsonObject application = new JsonObject(getStringJsonObject("full_data_application_2"));
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "id", "applicationId",
      "type", "docx"
    );
    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(application,
      pathParams, false);
    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals("applicationId", routingContext.pathParam("id"));
    Assertions.assertEquals("docx", routingContext.pathParam("type"));

    // Build the mock JSON response body
    JsonObject mockResponseBody = new JsonObject();
    JsonArray mockResultsArray = new JsonArray();
    JsonObject provenance = new JsonObject()
      .put("label", "Provenance");
    mockResultsArray.add(provenance);
    mockResponseBody.put("provenance", mockResultsArray);
    mockResponseBody.put("title", new JsonObject().put("en", "title datasheet"));
    mockResponseBody.put("country", new JsonObject().put("label", "GR"));
    mockResponseBody.put("publisher", new JsonObject().put("name", "publisher name"));
    mockResponseBody.put("hdab", new JsonObject().put("name", "hdab name"));
    mockResponseBody.put("catalog", new JsonObject()
      .put("id", "726200a0-7940-4446-b5f8-a62e6cc00b1b"));

    JsonObject mockResultsBody = new JsonObject()
      .put("result", mockResponseBody);
    // Mock behavior for web client call
    when(webClient.getAbs(config.getString(Constants.HUB_SEARCH_DATA_URL) + "/datasets/"
      + "8ddbd740-0c51-4a65-8ae8-0f03bb6fef5e")).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonObject()).thenReturn(mockResultsBody);

    documentExportManager.externalExportApplicationForm(routingContext);
    verify(routingContext.response()).end(any(Buffer.class));

  }


  @Test
  void testDownloadDataPermitDocument_success() {
    DocumentExportDataAccessManager manager = new DocumentExportDataAccessManager(
      new TranslationManager(), dbHandler, webClient, config
    );

    manager.downloadDataPermitDocument(routingContext);

    ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
    verify(serverResponse).putHeader("Content-Disposition",
      "attachment; filename=TEHDAS2_Task6.3_Data_permit_template_vC1.docx");
    verify(serverResponse).putHeader("Content-Type",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    verify(serverResponse).end(bufferCaptor.capture());

    Buffer returnedBuffer = bufferCaptor.getValue();
    assertNotNull(returnedBuffer);
    assertTrue(returnedBuffer.length() > 0, "Buffer should not be empty");
  }


  @Test
  void testDownloadDataPermitDocument_fail() {
    DocumentExportDataAccessManager manager = new DocumentExportDataAccessManager(
      new TranslationManager(), dbHandler, webClient, config
    );

    when(routingContext.response().putHeader(any(),anyString())).thenThrow(new RuntimeException());
    when(routingContext.response().setStatusCode(500)).thenReturn(serverResponse);

    manager.downloadDataPermitDocument(routingContext);

    verify(serverResponse).setStatusCode(500);

  }

}
