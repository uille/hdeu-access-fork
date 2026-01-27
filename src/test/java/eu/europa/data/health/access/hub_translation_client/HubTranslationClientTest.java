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
package eu.europa.data.health.access.hub_translation_client;

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dto.sections.Section2DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section6DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section7DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section8DTO;
import eu.europa.data.health.access.services.TranslationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class HubTranslationClientTest {

  private WebClient webClient;
  private TranslationManager translationManager;
  private HubTranslationClient client;

  @BeforeEach
  void setup() {
    webClient = mock(WebClient.class);
    translationManager = mock(TranslationManager.class);

    client = new HubTranslationClient(
      "http://translation-service/form",
      "http://translation-service/file",
      "http://callback-service",
      "api-key-123",
      "access-key-456",
      webClient,
      translationManager
    );
  }

  @Test
  void testCreateHeader_includesApiKeyAndLength() {
    Buffer buffer = Buffer.buffer("{\"test\":true}");
    MultiMap headers = client.createHeader(buffer);

    assertEquals("application/json", headers.get("content-type"));
    assertEquals(String.valueOf(buffer.length()), headers.get("content-length"));
    assertEquals("api-key-123", headers.get("X-API-Key"));
  }

  @Test
  void testCreateHeader_withApiKey() {
      Buffer buffer = Buffer.buffer("data");
      MultiMap headers = client.createHeader(buffer);

      assertEquals("application/json", headers.get("content-type"));
      assertEquals(String.valueOf(buffer.length()), headers.get("content-length"));
      assertTrue(headers.contains("X-API-Key"));
      assertEquals("api-key-123", headers.get("X-API-Key"));
  }

  @Test
  void testCreateHeader_withoutApiKey() {
      // Create a new client instance with an empty API key
      HubTranslationClient clientWithoutApiKey = new HubTranslationClient(
              "http://translation-service/form",
              "http://translation-service/file",
              "http://callback-service",
              "",
              "access-key-456",
              webClient,
              translationManager
      );

      Buffer buffer = Buffer.buffer("data");
      MultiMap headers = clientWithoutApiKey.createHeader(buffer);

      assertEquals("application/json", headers.get("content-type"));
      assertEquals(String.valueOf(buffer.length()), headers.get("content-length"));
      assertFalse(headers.contains("X-API-Key"));
  }

  @Test
  void testCreateHeader_withEmptyBuffer() {
      Buffer buffer = Buffer.buffer("");
      MultiMap headers = client.createHeader(buffer);

      assertEquals("application/json", headers.get("content-type"));
      assertEquals("0", headers.get("content-length"));
      assertEquals("api-key-123", headers.get("X-API-Key"));
  }

  @Test
  void testBuildTranslationRequestPayloadArray_success() {
    JsonObject dict = new JsonObject()
      .put("section8", new JsonArray().add(new JsonObject().put("technicalRequirementsForEnvironment", "hello")));

    JsonObject payload = client.buildTranslationRequestPayload(
      "app-123", dict, DATA_ACCESS_APPLICATION_COLLECTION, "en", List.of("de", "fr"));

    assertEquals("app-123", payload.getJsonObject("data").getString("applicationId"));
    assertEquals(DATA_ACCESS_APPLICATION_COLLECTION, payload.getJsonObject("data").getString("applicationType"));
    assertTrue(payload.getJsonObject("dict").containsKey("section8"));
    assertEquals(List.of("de", "fr"), payload.getJsonObject("data").getJsonArray("targetLanguages").getList());
  }


  @Test
  void testBuildTranslationRequestPayload_success() {

    JsonObject dict = new JsonObject()
      .put("section8", new JsonObject().put("technicalRequirementsForEnvironment", "hello"));

    JsonObject payload = client.buildTranslationRequestPayload(
      "app-123", dict, DATA_ACCESS_APPLICATION_COLLECTION, "en", List.of("de", "fr"));

    assertEquals("app-123", payload.getJsonObject("data").getString("applicationId"));
    assertEquals(DATA_ACCESS_APPLICATION_COLLECTION, payload.getJsonObject("data").getString("applicationType"));
    assertTrue(payload.getJsonObject("dict").containsKey("section8"));
    assertEquals(List.of("de", "fr"), payload.getJsonObject("data").getJsonArray("targetLanguages").getList());
  }

  @Test
  void testBuildTranslationRequestPayloadJsonObject_success() {

    JsonObject dict = new JsonObject()
      .put("section8", new JsonObject().put("technicalRequirementsForEnvironment" , "test")
        .put("nameWebsiteAddressForEnvironment", "test"));

    JsonObject payload = client.buildTranslationRequestPayload(
      "app-123", dict, DATA_ACCESS_APPLICATION_COLLECTION, "en", List.of("de", "fr"));

    assertEquals("app-123", payload.getJsonObject("data").getString("applicationId"));
    assertEquals(DATA_ACCESS_APPLICATION_COLLECTION, payload.getJsonObject("data").getString("applicationType"));
    assertTrue(payload.getJsonObject("dict").containsKey("section8"));
    assertEquals(List.of("de", "fr"), payload.getJsonObject("data").getJsonArray("targetLanguages").getList());
  }

  @Test
  void testBuildFileTranslationPayload_containsExpectedFields() {
    JsonObject fileObj = new JsonObject()
      .put("fileId", "file-1")
      .put("fileName", "doc.pdf")
      .put("format", "application/pdf")
      .put("base64Content", "dGVzdA==");

    JsonObject payload = client.buildFileTranslationPayload(
      "app-321", DATA_ACCESS_APPLICATION_COLLECTION, fileObj, List.of("fr"));

    JsonObject dict = payload.getJsonObject("dict").getJsonObject("file");
    assertNotNull(dict.getJsonObject("file-1"));
    assertEquals("doc.pdf", dict.getJsonObject("file-1")
      .getJsonObject("documentToTranslateBase64").getString("fileName"));
  }

  @Test
  void testBuildFormTranslations_appliesTranslation() {
    JsonObject form = new JsonObject().put("section2",
      new JsonObject().put("status", "original"));

    JsonObject dict = new JsonObject()
      .put("section2", new JsonObject()
        .put("status.1", new JsonObject()
          .put("translations", new JsonObject().put("FR", "traduit"))));

    JsonObject translatedForms = client.buildFormTranslations(form, dict,
      new JsonObject().put("status" ,
        new JsonObject().put("status", "COMPLETED").
          put("translatedFiles" , new JsonObject()
            .put("FR", new JsonObject())
            .put("fileName", "test")
            .put("fileId", "test")
          )),
      DATA_ACCESS_APPLICATION_COLLECTION, new JsonObject());

    assertEquals("original",
      translatedForms.getJsonObject("FR").getJsonObject("section2").getString("status"));
  }


  @Test
  void testBuildFormTranslationsJsonObject_appliesTranslation() {
    JsonObject form = new JsonObject().put("section1",
      new JsonObject().put("status" , new JsonObject().put("status", "original")));

    JsonObject dict = new JsonObject()
      .put("section1", new JsonObject()
        .put("status.1", new JsonObject()
          .put("translations", new JsonObject().put("FR", "traduit"))));

    JsonObject translatedForms = client.buildFormTranslations(form, dict,
      new JsonObject().put("status" ,
        new JsonObject().put("status", "COMPLETED").
          put("translatedFiles" , new JsonObject()
            .put("FR", new JsonObject())
            .put("fileName", "test")
            .put("fileId", "test")
          )),
      DATA_ACCESS_APPLICATION_COLLECTION, new JsonObject());

    assertEquals("original",
      translatedForms.getJsonObject("FR").getJsonObject("section1").getJsonObject("status").getString("status"));
  }


  @Test
  void testBuildFormTranslationsJsonArray_appliesTranslation() {
    JsonObject form = new JsonObject().put("section3",
      new JsonArray().add(new JsonObject().put("status", "original").put("status2", "original2")));

    JsonObject dict = new JsonObject()
      .put("section3", new JsonObject()
        .put("0.1", new JsonObject()
          .put("translations", new JsonObject().put("FR", "traduit"))));

    JsonObject translatedForms = client.buildFormTranslations(form, dict,
      new JsonObject().put("status" ,
        new JsonObject().put("status", "COMPLETED").
          put("translatedFiles" , new JsonObject()
            .put("FR", new JsonObject())
            .put("fileName", "test")
            .put("fileId", "test")
          )),
      DATA_REQUEST_APPLICATION_COLLECTION, new JsonObject());

    assertEquals("original",
      translatedForms.getJsonObject("FR").getJsonArray("section3").getJsonObject(0).getString(
        "status"));
  }


  @Test
  void testBuildFormTranslationsJsonArray_error() {
    JsonObject form = new JsonObject().put("section",
      new JsonArray().add(new JsonObject().put("status", "original").put("status2", "original2")));

    JsonObject dict = new JsonObject().put("section", new JsonObject().put("0.1",
      new JsonObject().put("translations", new JsonObject().put("FR", "traduit"))));

    assertThrows(IllegalArgumentException.class, () -> client.buildFormTranslations(form, dict,
      new JsonObject().put("status", new JsonObject().put("status", "COMPLETED")
        .put("translatedFiles", new JsonObject().put("FR", new JsonObject()).put("fileName", "test")
          .put("fileId", "test"))), "error", new JsonObject()));
  }


  @Test
  void testInjectStaticOptionLabels_replacesValue() {
    JsonObject sectionNode = new JsonObject().put("choice", new JsonObject().put("key", "yes").put("value", "Yes"));

    when(translationManager.getTranslation("fr", "section2"))
      .thenReturn(new JsonObject().put("choice", new JsonObject().put("items", null)));

    client.injectStaticOptionLabels(sectionNode, "fr", "section2", Section2DTO.class);

    assertEquals("Yes", sectionNode.getJsonObject("choice").getString("value"));
  }

  @Test
  void testInjectStaticOptionLabels2_replacesValue() {
    JsonObject sectionNode = new JsonObject().put("purposeForWhichDataWillBeUsed", new JsonObject().put("key", "yes").put("value", "Yes"));

    when(translationManager.getTranslation("fr", "section2"))
      .thenReturn(new JsonObject().put("choice", new JsonObject().put("items", null)));

    client.injectStaticOptionLabels(sectionNode, "fr", "section2", Section2DTO.class);

    assertEquals("Yes", sectionNode.getJsonObject("purposeForWhichDataWillBeUsed").getString("value"));
  }


  @Test
  void testInjectStaticOptionLabels3_replacesValue() {
    JsonObject sectionNode = new JsonObject().put("choice",
      new JsonObject().put("keyless", "yes").put("value", "Yes"));

    JsonArray translatedItems = new JsonArray().add(new JsonObject().put("key", "yes").put("value", "Oui"));

    when(translationManager.getTranslation("fr", "section2"))
      .thenReturn(new JsonObject().put("choice", new JsonObject().put("items", translatedItems)));

    client.injectStaticOptionLabels(sectionNode, "fr", "section2", Section8DTO.class);

    assertEquals("Yes", sectionNode.getJsonObject("choice").getString("value"));
  }



  @Test
  void testInjectStaticOptionLabelsJsonArray_replacesValue() {
    JsonObject sectionNode = new JsonObject().put("choice",
      new JsonArray().add(new JsonObject().put("key", "yes").put("value", "Yes")));

    JsonArray translatedItems = new JsonArray().add(new JsonObject().put("key", "yes").put("value", "Oui"));

    when(translationManager.getTranslation("fr", "section2"))
      .thenReturn(new JsonObject().put("choice", new JsonObject().put("items", translatedItems)));

    client.injectStaticOptionLabels(sectionNode, "fr", "section2", Section7DTO.class);

    assertEquals("Oui", sectionNode.getJsonArray("choice").getJsonObject(0).getString("value"));
  }


  @Test
  void testInjectStaticOptionLabelsJsonArrayEmpty_replacesValue() {
    JsonArray sectionNode = new JsonArray().add(new JsonObject().put("Oui", "yes"));

    JsonArray translatedItems = new JsonArray().add(new JsonObject().put("key", "yes").put("value", "Oui"));

    when(translationManager.getTranslation("fr", "section2"))
      .thenReturn(new JsonObject().put("choice", new JsonObject().put("items", translatedItems)));

    client.injectStaticOptionLabels(sectionNode, "fr", "section2", Section6DTO.class);

    assertEquals("{\"Oui\":\"yes\"}", sectionNode.getString(0));
  }

  @Test
  void testRequestTranslation_successfulResponse() {
    JsonObject payload = new JsonObject().put("test", true);

    var request = mock(HttpRequest.class);
    when(webClient.postAbs(anyString())).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);

    doAnswer(invocation -> {
      var handler = invocation.getArgument(1, io.vertx.core.Handler.class);
      HttpResponse<Buffer> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(200);
      when(response.bodyAsString()).thenReturn("OK");

      handler.handle((AsyncResult) Future.succeededFuture(response));
      return null;
    }).when(request).sendBuffer(any(), any());

    Future<Void> future = client.requestTranslation(payload);

    assertTrue(future.succeeded());
  }

  @Test
  void testRequestFileTranslation_failureResponse() {
    JsonObject payload = new JsonObject().put("test", true);

    var request = mock(HttpRequest.class);
    when(webClient.postAbs(anyString())).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);

    doAnswer(invocation -> {
      var handler = invocation.getArgument(1, io.vertx.core.Handler.class);
      HttpResponse<Buffer> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(500);
      when(response.bodyAsString()).thenReturn("Internal Error");

      handler.handle((AsyncResult) Future.succeededFuture(response));
      return null;
    }).when(request).sendBuffer(any(), any());

    Future<Void> future = client.requestFileTranslation(payload);

    assertTrue(future.failed());
    assertTrue(future.cause().getMessage().contains("errorCode"));
  }


  @Test
  void testRequestFileTranslation_Error() {
    JsonObject payload = new JsonObject().put("test", true);

    var request = mock(HttpRequest.class);
    when(webClient.postAbs(anyString())).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);

    doAnswer(invocation -> {
      var handler = invocation.getArgument(1, io.vertx.core.Handler.class);
      HttpResponse<Buffer> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(500);
      when(response.bodyAsString()).thenReturn("Internal Error");

      handler.handle((AsyncResult) Future.failedFuture(new Exception()));
      return null;
    }).when(request).sendBuffer(any(), any());

    Future<Void> future = client.requestFileTranslation(payload);

    assertTrue(future.failed());
    assertTrue(future.cause().getMessage().contains("errorCode"));
  }

}
