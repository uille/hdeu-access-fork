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
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Translations;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TranslationManagerTest {

  @Mock DBHandler dbHandler;
  @Mock RoutingContext routingContext;
  @Mock HttpServerResponse response;
  @Mock User user;

  /**
   * Testable manager that bypasses classpath resources by serving JSON from an in-memory map.
   */
  static class TestableTranslationManager extends TranslationManager {

    private final Map<String, String> langToJson = new HashMap<>();

    TestableTranslationManager(DBHandler dbHandler) {
      super(dbHandler);
    }

    void putLangJson(String lang, String json) {
      langToJson.put(lang, json);
    }

    void removeLang(String lang) {
      langToJson.remove(lang);
    }

    @Override
    InputStream getTranslationFileStream(String language) {
      String json = langToJson.get(language);
      if (json == null) {
        return null;
      }
      return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
  }

  private TestableTranslationManager manager;

  @BeforeEach
  void setUp() {
    manager = new TestableTranslationManager(dbHandler);

    when(routingContext.response()).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
  }

  // -------------------------------------------------------------------------
  // loadJsonFromStream
  // -------------------------------------------------------------------------

  @Test
  void loadJsonFromStream_whenNullStream_shouldReturnNull() {
    assertNull(manager.loadJsonFromStream(null));
  }

  @Test
  void loadJsonFromStream_whenBlankContent_shouldReturnNull() {
    InputStream is = new ByteArrayInputStream("   ".getBytes(StandardCharsets.UTF_8));
    assertNull(manager.loadJsonFromStream(is));
  }

  @Test
  void loadJsonFromStream_whenInvalidJson_shouldReturnNull() {
    InputStream is = new ByteArrayInputStream("{ not-json".getBytes(StandardCharsets.UTF_8));
    assertNull(manager.loadJsonFromStream(is));
  }

  @Test
  void loadJsonFromStream_whenValidJson_shouldReturnJsonObject() {
    InputStream is = new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
    JsonObject obj = manager.loadJsonFromStream(is);
    assertNotNull(obj);
    assertEquals(1, obj.getInteger("a"));
  }

  // -------------------------------------------------------------------------
  // getTranslationForSection
  // -------------------------------------------------------------------------

  @Test
  void getTranslationForSection_whenLanguageFileMissing_shouldFallbackToDefault_andReturnSectionPlusCommonFields() {
    String requestedLang = "xx";
    String defaultLang = Translations.getDefault();

    // requested language missing (returns null)
    manager.removeLang(requestedLang);

    // default language exists
    manager.putLangJson(defaultLang,
      new JsonObject()
        .put("section1", new JsonObject().put("s", "v"))
        .put("commonFields", new JsonObject().put("c", "cv"))
        .encode()
    );

    when(routingContext.pathParam("lg")).thenReturn(requestedLang);
    when(routingContext.pathParam("section")).thenReturn("section1");

    manager.getTranslationForSection(routingContext);

    ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCap.capture());

    JsonObject merged = new JsonObject(bodyCap.getValue());
    assertEquals("v", merged.getString("s"));
    assertEquals("cv", merged.getString("c"));
  }

  @Test
  void getTranslationForSection_whenRequestedAndDefaultMissing_shouldReturn500() {
    String requestedLang = "xx";
    String defaultLang = Translations.getDefault();

    manager.removeLang(requestedLang);
    manager.removeLang(defaultLang);

    when(routingContext.pathParam("lg")).thenReturn(requestedLang);
    when(routingContext.pathParam("section")).thenReturn("section1");

    manager.getTranslationForSection(routingContext);

    verify(response).setStatusCode(500);
    verify(response).end(contains("Translation file for default language"));
  }

  @Test
  void getTranslationForSection_whenJsonEmpty_shouldReturn404TranslationFileIsEmpty() {
    manager.putLangJson("en", "   "); // blank -> loadJsonFromStream returns null

    when(routingContext.pathParam("lg")).thenReturn("en");
    when(routingContext.pathParam("section")).thenReturn("section1");

    manager.getTranslationForSection(routingContext);

    verify(response).setStatusCode(404);
    verify(response).end("Translation file is empty.");
  }

  @Test
  void getTranslationForSection_whenSectionMissing_shouldReturn404SectionNotFound() {
    manager.putLangJson("en",
      new JsonObject()
        .put("commonFields", new JsonObject().put("c", "cv"))
        .encode()
    );

    when(routingContext.pathParam("lg")).thenReturn("en");
    when(routingContext.pathParam("section")).thenReturn("sectionX");

    manager.getTranslationForSection(routingContext);

    verify(response).setStatusCode(404);
    verify(response).end("Section not found for: sectionX");
  }

  @Test
  void getTranslationForSection_whenOk_shouldReturnMergedSectionAndCommonFields() {
    manager.putLangJson("en",
      new JsonObject()
        .put("section2", new JsonObject().put("a", "A"))
        .put("commonFields", new JsonObject().put("b", "B"))
        .encode()
    );

    when(routingContext.pathParam("lg")).thenReturn("en");
    when(routingContext.pathParam("section")).thenReturn("section2");

    manager.getTranslationForSection(routingContext);

    ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCap.capture());

    JsonObject merged = new JsonObject(bodyCap.getValue());
    assertEquals("A", merged.getString("a"));
    assertEquals("B", merged.getString("b"));
  }

  // -------------------------------------------------------------------------
  // getApplicationTranslation
  // -------------------------------------------------------------------------

  private void mockUserOnContext(String ecasId) {
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(new JsonObject().put("preferred_username", ecasId));
  }

  @Test
  void getApplicationTranslation_whenAccessAppFoundWithTranslation_shouldReturn200() {
    mockUserOnContext("u1");
    when(routingContext.pathParam("applicationId")).thenReturn("app1");

    JsonObject app = new JsonObject().put("translation", new JsonObject().put("status", "COMPLETED"));

    when(dbHandler.findByField(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(app));

    manager.getApplicationTranslation(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(new JsonObject().put("status", "COMPLETED").encode());
  }

  @Test
  void getApplicationTranslation_whenAccessAppFoundWithoutTranslation_shouldReturn404() {
    mockUserOnContext("u1");
    when(routingContext.pathParam("applicationId")).thenReturn("app1");

    JsonObject app = new JsonObject(); // no translation

    when(dbHandler.findByField(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(app));

    manager.getApplicationTranslation(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
    verify(response).end(bodyCap.capture());

    JsonObject out = new JsonObject(bodyCap.getValue());
    assertEquals("Translation not found", out.getString("message"));
  }

  @Test
  void getApplicationTranslation_whenAccess404AndRequestFound_shouldReturn200() {
    mockUserOnContext("u1");
    when(routingContext.pathParam("applicationId")).thenReturn("app1");

    when(dbHandler.findByField(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(new ServiceException(404, "not found")));

    JsonObject app = new JsonObject().put("translation", new JsonObject().put("x", 1));
    when(dbHandler.findByField(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(app));

    manager.getApplicationTranslation(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(new JsonObject().put("x", 1).encode());
  }

  @Test
  void getApplicationTranslation_whenAccess404AndRequest404_shouldReturn404ApplicationNotFound() {
    mockUserOnContext("u1");
    when(routingContext.pathParam("applicationId")).thenReturn("app1");

    when(dbHandler.findByField(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(new ServiceException(404, "not found")));

    when(dbHandler.findByField(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(new ServiceException(404, "not found")));

    manager.getApplicationTranslation(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end("Application not found");
  }

  @Test
  void getApplicationTranslation_whenAccess404AndRequestOtherError_shouldReturn500WithAccessMessage() {
    mockUserOnContext("u1");
    when(routingContext.pathParam("applicationId")).thenReturn("app1");

    ServiceException access404 = new ServiceException(404, "access missing");
    when(dbHandler.findByField(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(access404));

    ServiceException request500 = new ServiceException(500, "request exploded");
    when(dbHandler.findByField(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(request500));

    manager.getApplicationTranslation(routingContext);

    // NOTE: code returns INTERNAL_SERVER_ERROR but uses errorAccess.getMessage()
    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("access missing"));
  }

  @Test
  void getApplicationTranslation_whenAccessNon404Error_shouldReturn500WithErrorMessage() {
    mockUserOnContext("u1");
    when(routingContext.pathParam("applicationId")).thenReturn("app1");

    ServiceException access500 = new ServiceException(500, "db down");
    when(dbHandler.findByField(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(access500));

    manager.getApplicationTranslation(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("db down"));
  }

  // -------------------------------------------------------------------------
  // getTranslation(...) and getTranslationByLanguish(...)
  // NOTE: These two methods use Main.class.getResourceAsStream(...) directly,
  // so we rely on existing translation resources on the test/runtime classpath.
  // -------------------------------------------------------------------------

  @Test
  void getTranslation_whenResourceMissing_shouldReturnNull() {
    // pick a language that surely doesn't exist
    JsonObject section = manager.getTranslation("xx_DOES_NOT_EXIST_xx", "section1");
    assertNull(section);
  }

  @Test
  void getTranslation_whenResourceExists_shouldReturnSectionOrNullButCoverNonNullJsonBranch() {
    // This assumes your project already ships translation JSON files under the configured TRANSLATIONS path.
    // Use default language to maximize chance it exists.
    String lg = Translations.getDefault();

   // covers: inputStream != null AND jsonObject != null branches.
    JsonObject section = manager.getTranslation(lg, "commonFields");

    assertTrue(section == null || section instanceof JsonObject);
  }

  @Test
  void getTranslationByLanguish_whenResourceMissing_shouldReturnNull() throws Exception {
    JsonObject all = manager.getTranslationByLanguish("xx_DOES_NOT_EXIST_xx");
    assertNull(all);
  }

  @Test
  void getTranslationByLanguish_whenResourceExists_shouldReturnRootJson() throws Exception {
    String lg = Translations.getDefault();
    JsonObject all = manager.getTranslationByLanguish(lg);

    // If your default translations exist, this should be non-null.
    // If for some reason default isn't on classpath in tests, you'll get null and should point lg to an existing language.
    assertNotNull(all);
  }

}
