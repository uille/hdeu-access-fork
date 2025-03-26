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

import static eu.europa.data.health.access.UtilityServiceTest.getStringJsonObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentExportDataRequestManagerTest {

  @Mock
  private DBHandler dbHandler;
  @Mock
  private WebClient webClient;
  @Mock
  private HttpRequest<Buffer> httpRequest;
  @Mock
  private HttpResponse<Buffer> httpResponse;
  private JsonObject config;

  @InjectMocks
  private DocumentExportDataRequestManager documentExportDataRequestManager;

  @BeforeEach
  void setUp() {
    TranslationManager translationManager = new TranslationManager();
    config = new JsonObject().put("HUB_SEARCH_DATA_URL", "http://mocked-url");
    documentExportDataRequestManager = new DocumentExportDataRequestManager(translationManager,
      dbHandler, webClient,
      config);
  }

  @Test
  void exportApplicationForm() {
    JsonObject application = new JsonObject(getStringJsonObject("full_data_request"));
    Future<JsonObject> future = Future.succeededFuture(application);
    when(dbHandler.findById("DATA_REQUEST_APPLICATION", "applicationId")).thenReturn(future);

    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "id", "applicationId",
      "type", "docx"
    );
    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null,
      pathParams, false);
    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals("applicationId", routingContext.pathParam("id"));
    Assertions.assertEquals("docx", routingContext.pathParam("type"));


    // Mock WebClient and HttpRequest behavior for the dataset fetch
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(404);

    documentExportDataRequestManager.exportApplicationDataRequestForm(routingContext);
    verify(routingContext.response()).end(any(Buffer.class));
    verify(dbHandler, times(1)).findById("DATA_REQUEST_APPLICATION", "applicationId");

  }

  @Test
  void externalExportApplicationForm() {
    JsonObject application = new JsonObject(getStringJsonObject("full_data_request"));
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

    // Mock WebClient and HttpRequest behavior for the dataset fetch
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(404);

    documentExportDataRequestManager.externalExportApplicationForm(routingContext);
    verify(routingContext.response()).end(any(Buffer.class));

  }

}
