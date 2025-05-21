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
package eu.europa.data.health.access.validations.validators;

import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.UPLOAD_RDF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.dto.CatalogueDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.hub_repo_client.HubRepoClient;
import eu.europa.data.health.access.services.CountriesManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class CatalogueValidationManagerTest {

  @Mock
  private HubRepoClient hubRepoClient;
  @Mock
  private DispatcherClient dispatcherClient;
  @Mock
  private Vertx vertx;

  private CountriesManager countriesManager;
  @Mock
  private RoutingContext routingContext;
  @Mock
  private HttpServerResponse httpResponse;
  @Mock
  private HttpServerRequest httpServerRequest;
  @Mock

  private HttpRequest<Buffer> httpRequest;

  @Mock
  private FileUpload fileUpload;
  @Mock
  private HttpServerResponse response;
  @Mock
  private WebClient webClient;


  private CatalogueValidationManager manager;

  @BeforeEach
  void setUp() {
    countriesManager = new CountriesManager();
    manager = new CatalogueValidationManager(hubRepoClient, dispatcherClient, vertx,
      countriesManager);
  }

  @Test
  void testGenerateCatalogueDTO_validJson_returnsDTO() {
    String json = "{\"catalogueId\":\"test-id\"}";
    RoutingContext ctx = mock(RoutingContext.class);
    CatalogueDTO dto = manager.generateCatalogueDTO(json, ctx);
    assertNotNull(dto);
    assertEquals("test-id", dto.getCatalogueId());
  }

  @Test
  void testGenerateCatalogueDTO_invalidJson_returnsNullAndSetsError() {
    String invalidJson = "{ invalid json }";
    when(routingContext.response()).thenReturn(httpResponse);
    when(httpResponse.setStatusCode(anyInt())).thenReturn(httpResponse);
    when(httpResponse.end(anyString())).thenReturn(Future.succeededFuture());

    CatalogueDTO result = manager.generateCatalogueDTO(invalidJson, routingContext);
    assertNull(result);
    verify(httpResponse).setStatusCode(500);
  }

  @Test
  void testValidateSearchCatalogueJsonSchema_invokesValidator() {
    RoutingContext context = mock(RoutingContext.class);
    RequestBody requestBody = Mockito.mock(RequestBody.class);

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(context.response()).thenReturn(response);

    when(context.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);

    when(context.body()).thenReturn(requestBody);
    manager.validateSearchCatalogueJsonSchema(context);
    verify(context, times(2)).response();
  }


  @Test
  void testValidateFileCatalogue_noFiles_setsErrorResponse() {
    when(routingContext.fileUploads()).thenReturn(Collections.emptyList());
    when(routingContext.response()).thenReturn(httpResponse);
    when(httpResponse.setStatusCode(anyInt())).thenReturn(httpResponse);
    when(httpResponse.end(anyString())).thenReturn(Future.succeededFuture());

    manager.validateFileCatalogue(routingContext);

    verify(httpResponse).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(httpResponse).end(contains("No file found to upload"));
  }


  @Test
  void testValidateFileCatalogue() {
    FileSystem fileSystem = mock(FileSystem.class);
    String fileContent = "<dct:spatial rdf:resource=\"http://example.com/place\">\n";
    String fileName = "file.rdf";
    Buffer fileBuffer = Buffer.buffer(fileContent);

    JsonObject mockResult = new JsonObject();
    JsonArray jsonArray = new JsonArray();

    SelectObjectDTO selectObjectDTO = new SelectObjectDTO();
    selectObjectDTO.setKey("dct:spatial rdf:resource=\"http://example.com/place\">");
    selectObjectDTO.setValue("AFI");

    jsonArray.add(selectObjectDTO);
    mockResult.put("geographicalCoverageList", jsonArray);

    when(hubRepoClient.getAllGeographicalCoverage(routingContext)).thenReturn(
      Future.succeededFuture(mockResult));

    when(routingContext.fileUploads()).thenReturn(Collections.singletonList(fileUpload));
    when(fileUpload.uploadedFileName()).thenReturn(fileName);
    when(fileUpload.fileName()).thenReturn(fileName);

    when(vertx.fileSystem()).thenReturn(fileSystem);
    when(vertx.fileSystem().readFile(anyString())).thenReturn(Future.succeededFuture(fileBuffer));

    manager.validateFileCatalogue(routingContext);

    verify(httpResponse, never()).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }


  @Test
  void testValidateFileCatalogue_not_match() {
    FileSystem fileSystem = mock(FileSystem.class);
    String fileContent = "error";
    String fileName = "file.xml";
    Buffer fileBuffer = Buffer.buffer(fileContent);

    JsonObject mockResult = new JsonObject();
    JsonArray jsonArray = new JsonArray();

    SelectObjectDTO selectObjectDTO = new SelectObjectDTO();
    selectObjectDTO.setKey("dct:spatial rdf:resource=\"http://example.com/place\">");
    selectObjectDTO.setValue("AFI");

    jsonArray.add(selectObjectDTO);
    mockResult.put("geographicalCoverageList", jsonArray);

    when(routingContext.fileUploads()).thenReturn(Collections.singletonList(fileUpload));
    when(fileUpload.uploadedFileName()).thenReturn(fileName);
    when(fileUpload.fileName()).thenReturn(fileName);

    when(vertx.fileSystem()).thenReturn(fileSystem);
    when(vertx.fileSystem().readFile(anyString())).thenReturn(Future.succeededFuture(fileBuffer));

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    manager.validateFileCatalogue(routingContext);

    verify(httpResponse, never()).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }


  @Test
  void testValidateFileCatalogue_invalid_file() {
    FileSystem fileSystem = mock(FileSystem.class);
    String fileContent = "<dct:spatial rdf:resource=\"http://example.com/place\">\n";
    String fileName = "file.test";
    Buffer fileBuffer = Buffer.buffer(fileContent);

    JsonObject mockResult = new JsonObject();
    JsonArray jsonArray = new JsonArray();

    SelectObjectDTO selectObjectDTO = new SelectObjectDTO();
    selectObjectDTO.setKey("dct:spatial rdf:resource=\"http://example.com/place\">");
    selectObjectDTO.setValue("AFI");

    jsonArray.add(selectObjectDTO);
    mockResult.put("geographicalCoverageList", jsonArray);

    when(hubRepoClient.getAllGeographicalCoverage(routingContext)).thenReturn(
      Future.succeededFuture(mockResult));

    when(routingContext.fileUploads()).thenReturn(Collections.singletonList(fileUpload));
    when(fileUpload.uploadedFileName()).thenReturn(fileName);
    when(fileUpload.fileName()).thenReturn(fileName);

    when(vertx.fileSystem()).thenReturn(fileSystem);
    when(vertx.fileSystem().readFile(anyString())).thenReturn(Future.succeededFuture(fileBuffer));

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    manager.validateFileCatalogue(routingContext);

    verify(httpResponse, never()).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }


  @Test
  void testValidateUpdate_onSuccess() {
    RoutingContext context = mock(RoutingContext.class);

    JsonObject mockResult = new JsonObject();

    JsonObject requestBody = new JsonObject().put(CATALOGUE_ID, "test-cat-id")
      .put(DOMIBUS_ID, "dom-id").put(UPLOAD_RDF, new JsonObject().put("key", "b"))
      .put("geographicalCoverage", new JsonObject().put("key", "coverage"));

    when(context.body()).thenReturn(mock(RequestBody.class));
    when(context.body().asJsonObject()).thenReturn(requestBody);

    // Mock dependencies
    when(hubRepoClient.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(context)).thenReturn(
      Future.succeededFuture(mockResult));

    CatalogueValidationManager managerSpy = spy(
      new CatalogueValidationManager(hubRepoClient, dispatcherClient, vertx, countriesManager));

    managerSpy.validateUpdate(context);
  }


  @Test
  void testValidateUpdateRdf_onSuccess() {
    RoutingContext context = mock(RoutingContext.class);

    JsonObject requestBody = new JsonObject().put(CATALOGUE_ID, "test-cat-id")
      .put(DOMIBUS_ID, "dom-id").put(UPLOAD_RDF, new JsonObject().put("key", "a"))
      .put("geographicalCoverage", new JsonObject().put("key", "coverage"));

    when(context.body()).thenReturn(mock(RequestBody.class));
    when(context.body().asJsonObject()).thenReturn(requestBody);

    when(hubRepoClient.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(context)).thenReturn(
      Future.succeededFuture(requestBody));

    CatalogueValidationManager managerSpy = spy(
      new CatalogueValidationManager(hubRepoClient, dispatcherClient, vertx, countriesManager));

    managerSpy.validateUpdate(context);

  }


  @Test
  void testValidateCatalogMappingForDeletion_allConditionsMet_shouldCallNext() {
    String catalogId = "test-catalog-id";
    when(routingContext.pathParam("catalogueId")).thenReturn(catalogId);
    when(routingContext.put(anyString(), any())).thenReturn(routingContext);

    JsonObject catalogMapping = new JsonObject().put("isProduction", "true");
    JsonArray datasets = new JsonArray(); // empty means deletion allowed

    when(dispatcherClient.getCatalogMappingByCatalogId(catalogId)).thenReturn(
      Future.succeededFuture(catalogMapping));

    when(hubRepoClient.getDatasetsByCatalogId(catalogId)).thenReturn(
      Future.succeededFuture(datasets));

    doNothing().when(routingContext).next();

    manager.validateCatalogMappingForDeletion(routingContext);

    verify(routingContext, timeout(1000)).next();
  }


  @Test
  void testValidateCatalogMappingForDeletion_Production_false_shouldCallNext() {
    String catalogId = "test-catalog-id";
    when(routingContext.pathParam("catalogueId")).thenReturn(catalogId);
    when(routingContext.put(anyString(), any())).thenReturn(routingContext);

    JsonObject catalogMapping = new JsonObject().put("isProduction", "false");

    when(dispatcherClient.getCatalogMappingByCatalogId(catalogId)).thenReturn(
      Future.succeededFuture(catalogMapping));


    doNothing().when(routingContext).next();

    manager.validateCatalogMappingForDeletion(routingContext);

    verify(routingContext, timeout(1000)).next();
  }


  @Test
  void testValidateCatalogMappingForDeletion_datasetsExist_shouldReturn400() {
    String catalogId = "test-catalog-id";

    when(routingContext.pathParam("catalogueId")).thenReturn(catalogId);
    when(routingContext.put(anyString(), any())).thenReturn(routingContext);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    JsonObject catalogMapping = new JsonObject().put("isProduction", "true");
    JsonArray datasets = new JsonArray().add(new JsonObject()); // Non-empty, so deletion must fail

    when(dispatcherClient.getCatalogMappingByCatalogId(catalogId)).thenReturn(
      Future.succeededFuture(catalogMapping));

    when(hubRepoClient.getDatasetsByCatalogId(catalogId)).thenReturn(
      Future.succeededFuture(datasets));

    manager.validateCatalogMappingForDeletion(routingContext);

    verify(response, timeout(1000)).setStatusCode(400);
  }


}
