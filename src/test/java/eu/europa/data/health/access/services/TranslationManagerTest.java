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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import eu.europa.data.health.access.RoutingContextMocker;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class TranslationManagerTest {


  private TranslationManager translationManager;

  @BeforeEach
  void setUp() {
    translationManager = new TranslationManager();
  }

  @Test
  void testGetTranslationSuccess() {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "section", "section1"
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null, pathParams, true);

    translationManager.getTranslationForSection(routingContext);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(routingContext.response()).setStatusCode(200);
    verify(routingContext.response()).end(captor.capture());
    JsonObject actualResponse = new JsonObject(captor.getValue());
    assertEquals("Selecting Project Sources", actualResponse.getString("section1h5"));
  }

  @Test
  void testGetTranslationForSectionFileNotFound() {
    Map<String, String> pathParams = Map.of(
      "lg", "gr",
      "section", "section1"
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null, pathParams, true);


    translationManager.getTranslationForSection(routingContext);

    verify(routingContext.response()).setStatusCode(404);
    verify(routingContext.response()).end("Translation file for languish: gr not found.");
  }

  @Test
  void testGetTranslationForSectionEmptyFile() {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "section", "wrongSection"
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null, pathParams, true);


    translationManager.getTranslationForSection(routingContext);

    verify(routingContext.response()).setStatusCode(404);
    verify(routingContext.response()).end("Not section found: wrongSection");
  }

  @Test
  void testGetTranslationFileNotFound() {
    JsonObject result = translationManager.getTranslation("gr", "section1");

    assertNull(result);
  }

  @Test
  void testGetTranslationByLanguishSuccess() throws Exception {

    JsonObject result = translationManager.getTranslationByLanguish("en");

    assertNotNull(result);
    assertEquals("Selecting Project Sources", result.getJsonObject("section1").getString("section1h5"));
  }

  @Test
  void testGetTranslationByLanguishFailed() throws Exception {

    JsonObject result = translationManager.getTranslationByLanguish("wrongLanguish");

    assertNull(result);
  }

  @Test
  void testLoadJsonFromStreamEmptyStream() {
    InputStream inputStream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));

    JsonObject result = translationManager.loadJsonFromStream(inputStream);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetTranslationForSection_IOException() throws IOException {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "section", "section1"
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null, pathParams, true);

    TranslationManager translationManagerSpy = spy(translationManager);
    doAnswer(invocation -> {throw new IOException("Simulated IOException");
    }).when(translationManagerSpy).loadJsonFromStream(any(InputStream.class));

    translationManagerSpy.getTranslationForSection(routingContext);

    verify(routingContext.response()).setStatusCode(500);
    verify(routingContext.response()).end("Simulated IOException");
  }

}
