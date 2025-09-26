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

import static eu.europa.data.health.access.Constants.CONTROLLED_VOCABULARY_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ControlledVocabularyManagerTest {

  private static final String ACCESS_RIGHT_SCHEME = "http://publications.europa.eu/resource/authority/access-right";
  private static final String CHECKSUM_SCHEME = "http://spdx.org/rdf/terms";
  private static final String ROLE_CODE_SCHEME = "https://standards.iso.org/iso/19115/resources/Codelists/gml/CI_RoleCode.xml";

  private static final String LABEL = "label";
  private static final String VALUE = "value";

  @Mock
  private MongoClient mongo;
  @Mock
  private WebClient http;
  @Mock
  private HubTranslationClient hubTranslationClient;

  private ControlledVocabularyManager manager;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
    manager = new ControlledVocabularyManager(mongo, http, hubTranslationClient);
    when(hubTranslationClient.createCallbackJson()).thenReturn(new JsonObject().put("cb", true));
    when(hubTranslationClient.requestTranslation(any())).thenReturn(Future.succeededFuture());
  }

  @SuppressWarnings("unchecked")
  private void mockHttpGet(String url, int status, String body) {
    HttpRequest<Buffer> req = mock(HttpRequest.class);
    HttpResponse<Buffer> resp = mock(HttpResponse.class);

    when(http.getAbs(url)).thenReturn(req);
    doAnswer(inv -> {
      var handler = (Handler<AsyncResult<HttpResponse<Buffer>>>) inv.getArgument(
        0);
      when(resp.statusCode()).thenReturn(status);
      when(resp.bodyAsString()).thenReturn(body);
      when(resp.body()).thenReturn(Buffer.buffer(body, String.valueOf(StandardCharsets.UTF_8)));
      handler.handle(Future.succeededFuture(resp));
      return null;
    }).when(req).send(any());
  }

  private static JsonObject docWithItems(String source, JsonArray items) {
    return new JsonObject().put("source", source).put("items", items);
  }


  private static String readResource(String path) {
    try (InputStream in = Objects.requireNonNull(
      ControlledVocabularyManagerTest.class.getResourceAsStream(path),
      "Test resource missing: " + path)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String conceptRdf(String uri, String lang, String label) {
    return """
      <?xml version="1.0" encoding="utf-8" ?>
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
               xmlns:skos="http://www.w3.org/2004/02/skos/core#">
        <rdf:Description rdf:about="%s">
          <skos:prefLabel xml:lang="%s">%s</skos:prefLabel>
        </rdf:Description>
      </rdf:RDF>
      """.formatted(uri, lang, label);
  }

  private static List<String> extractTopConceptUris(String rdfContent) {
    // simple regex is fine for these fixtures
    Pattern p = Pattern.compile("skos:hasTopConcept\\s+rdf:resource=\"([^\"]+)\"");
    Matcher m = p.matcher(rdfContent);
    ArrayList<String> list = new ArrayList<>();
    while (m.find()) {
      list.add(m.group(1));
    }
    return list;
  }

  @Test
  void getBySource_cached_multilingual_triggersTranslation() {
    String source = "http://example.com/scheme";
    JsonArray items = new JsonArray()
      .add(new JsonObject()
        .put("value", "http://example.com/c1")
        .put("label", new JsonObject().put("en", "Foo"))) // multilingual label with only en
      .add(new JsonObject()
        .put("value", "http://example.com/c2")
        .put("label", "Plain String")); // string label (ignored)
    JsonObject cached = docWithItems(source, items);

    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(cached));

    // Return
    var fut = manager.getBySource(source);
    JsonArray out = fut.result();
    assertEquals(2, out.size());

    // Should have triggered a translation request (because at least one multilingual label has only "en")
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void getBySource_cached_stringLabels_noTranslation() {
    String source = "http://example.com/scheme2";
    JsonArray items = new JsonArray()
      .add(new JsonObject().put("value", "v1").put("label", "MD5"))
      .add(new JsonObject().put("value", "v2").put("label", "SHA-256"));
    JsonObject cached = docWithItems(source, items);

    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(cached));

    var fut = manager.getBySource(source);
    assertEquals(2, fut.result().size());

    // No multilingual labels -> no translation trigger
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void getBySource_buildFromChecksum_upserts_and_noTranslation() {
    // Not cached
    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Mock HTTP for checksum page (2 entries)
    String html = """
      <div id="d4e2129">
        <dl>
          <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_md5">MD5</a></dd>
          <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_sha256">SHA256</a></dd>
        </dl>
      </div>
      """;
    mockHttpGet(CHECKSUM_SCHEME, 200, html);

    // Upsert succeeds
    when(
      mongo.updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
        any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    var fut = manager.getBySource(CHECKSUM_SCHEME);
    JsonArray items = fut.result();

    // Built from HTML
    assertEquals(2, items.size());
    assertEquals("http://spdx.org/rdf/terms#checksumAlgorithm_md5",
      items.getJsonObject(0).getString("value"));
    assertEquals("MD5", items.getJsonObject(0).getString("label"));
    assertEquals("SHA-256", items.getJsonObject(1).getString("label")); // hyphen inserted

    // Check upsert happened
    verify(mongo).updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class), any(UpdateOptions.class));

    // All labels are strings -> no translation trigger
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void getBySource_buildFromRoleCode_upserts_and_triggersTranslation() {
    // Not cached
    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    String xml = """
      <Dictionary xmlns="http://www.opengis.net/gml/3.2" xmlns:gml="http://www.opengis.net/gml/3.2">
        <dictionaryEntry>
          <Definition gml:id="_1">
            <description>party that supplies the resource</description>
            <identifier>resourceProvider</identifier>
          </Definition>
        </dictionaryEntry>
      </Dictionary>
      """;
    mockHttpGet(ROLE_CODE_SCHEME, 200, xml);

    when(
      mongo.updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
        any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    var fut = manager.getBySource(ROLE_CODE_SCHEME);
    JsonArray items = fut.result();

    assertEquals(1, items.size());
    JsonObject item = items.getJsonObject(0);
    assertEquals(ROLE_CODE_SCHEME + "#resourceProvider", item.getString("value"));
    // label is multilingual JsonObject with "en"
    assertInstanceOf(JsonObject.class, item.getValue("label"));
    assertTrue(item.getJsonObject("label").containsKey("en"));
    assertTrue(item.getJsonObject("label").getString("en").startsWith("Resource Provider"));

    // Upsert called
    verify(mongo).updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class), any(UpdateOptions.class));
    // And translation triggered for missing languages
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void getBySource_buildFromRDF_accessRight_upserts_and_triggersTranslation() {
    // Not cached
    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Load the scheme RDF from resources
    String schemeRdf = readResource("/health-dcat-ap/access-rights.rdf");
    mockHttpGet(ACCESS_RIGHT_SCHEME, 200, schemeRdf);

    // The scheme contains these top concepts (from the provided RDF)
    List<String> conceptUris = List.of(
      ACCESS_RIGHT_SCHEME + "/SENSITIVE",
      ACCESS_RIGHT_SCHEME + "/NORMAL",
      ACCESS_RIGHT_SCHEME + "/OP_DATPRO",
      ACCESS_RIGHT_SCHEME + "/PUBLIC",
      ACCESS_RIGHT_SCHEME + "/NON_PUBLIC",
      ACCESS_RIGHT_SCHEME + "/RESTRICTED",
      ACCESS_RIGHT_SCHEME + "/CONFIDENTIAL"
    );

    // For each concept, mock a minimal RDF with English prefLabel
    for (String c : conceptUris) {
      String name = c.substring(c.lastIndexOf('/') + 1).replace('_', ' ');
      String label = "Label " + name; // arbitrary but deterministic
      mockHttpGet(c, 200, conceptRdf(c, "en", label));
    }

    // Upsert succeeds
    when(
      mongo.updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
        any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    // Act
    JsonArray items = manager.getBySource(ACCESS_RIGHT_SCHEME).result();

    // Assert items built for all concepts
    assertNotNull(items);
    assertEquals(conceptUris.size(), items.size());
    // Check a couple of entries are multilingual with "en"
    JsonObject first = items.getJsonObject(0);
    assertInstanceOf(JsonObject.class, first.getValue("label"));
    assertTrue(first.getJsonObject("label").containsKey("en"));

    // Upsert called once
    verify(mongo).updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION),
      any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class));

    // Since labels are multilingual (only "en"), there are missing supported langs -> translation triggered
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void getBySource_buildFromRDF_languageScheme_filters_and_triggersTranslation() {
    final String scheme = "http://publications.europa.eu/resource/authority/language";

    // Not cached
    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    // Scheme RDF from resources
    String schemeRdf = readResource("/health-dcat-ap/languages.rdf");
    mockHttpGet(scheme, 200, schemeRdf);

    // Mock concept fetch for all top concepts listed in the fixture (the code will filter internally)
    for (String c : extractTopConceptUris(schemeRdf)) {
      String seg = c.substring(c.lastIndexOf('/') + 1).replace('_', ' ');
      mockHttpGet(c, 200, conceptRdf(c, "el", "Γλώσσα " + seg));
    }

    when(
      mongo.updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
        any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    JsonArray items = manager.getBySource(scheme).result();

    assertNotNull(items);
    assertFalse(items.isEmpty(), "Expected at least one supported language concept");
    // labels should be multilingual objects (at least "el")
    for (int i = 0; i < items.size(); i++) {
      Object lbl = items.getJsonObject(i).getValue("label");
      assertInstanceOf(JsonObject.class, lbl, "language labels must be multilingual");
      assertTrue(((JsonObject) lbl).containsKey("el"));
    }

    verify(mongo).updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class), any(UpdateOptions.class));
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void getBySource_buildFromRDF_countryScheme_filters_and_triggersTranslation() {
    final String scheme = "http://publications.europa.eu/resource/authority/country";

    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    String schemeRdf = readResource("/health-dcat-ap/countries.rdf");
    mockHttpGet(scheme, 200, schemeRdf);

    for (String c : extractTopConceptUris(schemeRdf)) {
      String seg = c.substring(c.lastIndexOf('/') + 1).replace('_', ' ');
      mockHttpGet(c, 200, conceptRdf(c, "en", "Country " + seg));
    }

    when(
      mongo.updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
        any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    JsonArray items = manager.getBySource(scheme).result();

    assertNotNull(items);
    assertFalse(items.isEmpty(), "Expected at least one supported country concept");
    for (int i = 0; i < items.size(); i++) {
      Object lbl = items.getJsonObject(i).getValue("label");
      assertInstanceOf(JsonObject.class, lbl, "country labels must be multilingual");
      assertTrue(((JsonObject) lbl).containsKey("en"));
    }

    verify(mongo).updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class), any(UpdateOptions.class));
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void getBySource_buildFromRDF_fileType_nonMultilingual_noTranslation() {
    final String scheme = "http://publications.europa.eu/resource/authority/file-type";

    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    String schemeRdf = readResource("/health-dcat-ap/file-types.rdf");
    mockHttpGet(scheme, 200, schemeRdf);

    // All concept fetches return an English prefLabel
    for (String c : extractTopConceptUris(schemeRdf)) {
      String seg = c.substring(c.lastIndexOf('/') + 1).replace('_', ' ');
      mockHttpGet(c, 200, conceptRdf(c, "en", "FileType " + seg));
    }

    when(
      mongo.updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
        any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    JsonArray items = manager.getBySource(scheme).result();

    assertNotNull(items);
    assertFalse(items.isEmpty(), "Expected at least one file-type concept");
    // labels must be plain strings for file-type special case
    for (int i = 0; i < items.size(); i++) {
      assertInstanceOf(String.class, items.getJsonObject(i).getValue("label"),
        "file-type labels must be non-multilingual strings");
    }

    verify(mongo).updateCollectionWithOptions(eq(CONTROLLED_VOCABULARY_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class), any(UpdateOptions.class));
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void processTranslationCallback_mergesNewLangs_skipsErrors_and_persists() {
    String source = "http://example.com/scheme3";
    String value = "http://example.com/c1";

    // Existing doc with multilingual label having "en" only
    JsonObject existing = docWithItems(source, new JsonArray()
      .add(new JsonObject()
        .put("value", value)
        .put("label", new JsonObject().put("en", "Hello"))));

    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(existing));

    when(mongo.updateCollection(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
      any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    JsonObject dict = new JsonObject()
      .put(source, new JsonObject()
        .put(value, new JsonObject()
          .put("translations", new JsonObject()
            .put("fr", "Bonjour")
            .put("de", -3201)))); // numeric error -> must be skipped

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);

    manager.processTranslationCallback(source, dict);

    // Verify persisted patch contains "fr" but not "de"
    verify(mongo).updateCollection(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class),
      updateCaptor.capture());

    JsonObject update = updateCaptor.getValue();
    JsonObject set = update.getJsonObject("$set");
    assertNotNull(set);
    JsonArray items = set.getJsonArray("items");
    assertEquals(1, items.size());
    JsonObject label = items.getJsonObject(0).getJsonObject("label");
    assertEquals("Hello", label.getString("en"));
    assertEquals("Bonjour", label.getString("fr"));
    assertFalse(label.containsKey("de"));
  }

  @Test
  void processTranslationCallback_unknownSource_noPersist() {
    when(mongo.findOne(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(null));

    JsonObject dict = new JsonObject()
      .put("http://unknown", new JsonObject()
        .put("v", new JsonObject().put("translations", new JsonObject().put("fr", "x"))));

    manager.processTranslationCallback("http://unknown", dict);

    verify(mongo, never()).updateCollection(anyString(), any(JsonObject.class),
      any(JsonObject.class));
  }


  @Test
  void refreshBySource_emptyDoc_skipsUpdateAndTranslation() {
    // Given: buildFromChecksum will return an empty items list (no matching links)
    String html = """
    <div id="d4e2129">
      <dl>
        <!-- no <a title="..."> entries -->
      </dl>
    </div>
    """;
    mockHttpGet(CHECKSUM_SCHEME, 200, html);

    // And: existing doc (should be returned as-is)
    JsonObject existing = docWithItems(CHECKSUM_SCHEME, new JsonArray()
      .add(new JsonObject().put(VALUE, "v1").put(LABEL, "L1")));

    // When
    Future<Void> fut = manager.refreshBySource(CHECKSUM_SCHEME, existing);

    // Then
    assertTrue(fut.succeeded());
    // No upsert and no translation trigger
    verify(mongo, never()).updateCollectionWithOptions(anyString(), (JsonObject) any(), (JsonArray) any(), any(UpdateOptions.class));
    verify(hubTranslationClient, never()).requestTranslation(any());
  }
  @Test
  void refreshBySource_equivalentDoc_skipsUpdateAndTranslation() {
    // Given: buildFromChecksum produces a doc equivalent to 'existing'
    String html = """
    <div id="d4e2129">
      <dl>
        <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_md5">MD5</a></dd>
        <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_sha256">SHA256</a></dd>
      </dl>
    </div>
    """;
    mockHttpGet(CHECKSUM_SCHEME, 200, html);

    // The built labels will normalize to "MD5" and "SHA-256" (hyphen inserted by code)
    JsonArray existingItems = new JsonArray()
      .add(new JsonObject()
        .put(VALUE, "http://spdx.org/rdf/terms#checksumAlgorithm_md5")
        .put(LABEL, "MD5"))
      .add(new JsonObject()
        .put(VALUE, "http://spdx.org/rdf/terms#checksumAlgorithm_sha256")
        .put(LABEL, "SHA-256"));

    JsonObject existing = docWithItems(CHECKSUM_SCHEME, existingItems);

    // When
    Future<Void> fut = manager.refreshBySource(CHECKSUM_SCHEME, existing);

    // Then
    assertTrue(fut.succeeded());
    // Equivalent -> skip upsert
    verify(mongo, never()).updateCollectionWithOptions(anyString(), any(), (JsonObject) any(), any(UpdateOptions.class));
    // All labels are simple strings -> no translation either
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void refreshBySource_differentDoc_upserts_and_triggersTranslation() {
    // Given: Role Code build produces multilingual labels (JsonObject with "en")
    String xml = """
    <Dictionary xmlns="http://www.opengis.net/gml/3.2" xmlns:gml="http://www.opengis.net/gml/3.2">
      <dictionaryEntry>
        <Definition gml:id="_1">
          <description>party that supplies the resource</description>
          <identifier>resourceProvider</identifier>
        </Definition>
      </dictionaryEntry>
    </Dictionary>
    """;
    mockHttpGet(ROLE_CODE_SCHEME, 200, xml);

    // Existing differs (empty or different item), so update should occur
    JsonObject existing = docWithItems(ROLE_CODE_SCHEME, new JsonArray());

    when(mongo.updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    // When
    Future<Void> fut = manager.refreshBySource(ROLE_CODE_SCHEME, existing);

    // Then
    assertTrue(fut.succeeded());
    // Upsert called once
    verify(mongo, times(1)).updateCollectionWithOptions(
      anyString(),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class)
    );    // Role code produces multilingual labels -> translation should be triggered
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void refreshBySource_nullExisting_performsUpsert_and_triggersTranslation() {
    // Given: checksum page with one entry -> non-empty doc
    String html = """
    <div id="d4e2129">
      <dl>
        <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_md5">MD5</a></dd>
      </dl>
    </div>
    """;
    mockHttpGet(CHECKSUM_SCHEME, 200, html);

    when(mongo.updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    // When existing is null, we still upsert the fresh doc
    Future<Void> fut = manager.refreshBySource(CHECKSUM_SCHEME, null);

    // Then
    assertTrue(fut.succeeded());
    verify(mongo, times(1)).updateCollectionWithOptions(
      anyString(),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class)
    );    // For checksum scheme, labels are plain strings -> translation is NOT triggered
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void labelsEq_multilingual_equal_ignoreExtraLangs_noUpsert() {
    // RoleCode returns multilingual with "en"
    String xml = """
    <Dictionary xmlns="http://www.opengis.net/gml/3.2" xmlns:gml="http://www.opengis.net/gml/3.2">
      <dictionaryEntry>
        <Definition gml:id="_1">
          <description>party that supplies the resource</description>
          <identifier>resourceProvider</identifier>
        </Definition>
      </dictionaryEntry>
    </Dictionary>
    """;
    mockHttpGet(ROLE_CODE_SCHEME, 200, xml);

    // Existing has same "en" PLUS an extra "fr" -> should be considered equivalent
    JsonObject existingLabel = new JsonObject()
      .put("en", "Resource Provider – Party that supplies the resource")
      .put("fr", "Fournisseur de ressources"); // extra language should be ignored
    JsonArray existingItems = new JsonArray()
      .add(new JsonObject()
        .put("value", ROLE_CODE_SCHEME + "#resourceProvider")
        .put("label", existingLabel));
    JsonObject existing = docWithItems(ROLE_CODE_SCHEME, existingItems);

    manager.refreshBySource(ROLE_CODE_SCHEME, existing)
      .toCompletionStage().toCompletableFuture().join();

    verify(mongo, never()).updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class));

    // labels are multilingual; but since no update, translation is not re-triggered here
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void labelsEq_multilingual_mismatch_triggersUpsert() {
    String xml = """
    <Dictionary xmlns="http://www.opengis.net/gml/3.2" xmlns:gml="http://www.opengis.net/gml/3.2">
      <dictionaryEntry>
        <Definition gml:id="_1">
          <description>party that supplies the resource</description>
          <identifier>resourceProvider</identifier>
        </Definition>
      </dictionaryEntry>
    </Dictionary>
    """;
    mockHttpGet(ROLE_CODE_SCHEME, 200, xml);

    // Existing has different "en" (tiny change) -> not equivalent
    JsonObject existingLabel = new JsonObject()
      .put("en", "Resource Provider – party that supplies the resource"); // 'party' lowercased
    JsonArray existingItems = new JsonArray()
      .add(new JsonObject()
        .put("value", ROLE_CODE_SCHEME + "#resourceProvider")
        .put("label", existingLabel));
    JsonObject existing = docWithItems(ROLE_CODE_SCHEME, existingItems);

    when(mongo.updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    manager.refreshBySource(ROLE_CODE_SCHEME, existing)
      .toCompletionStage().toCompletableFuture().join();

    verify(mongo, times(1)).updateCollectionWithOptions(
      anyString(),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class)
    );
    // Role code uses multilingual labels -> translation should be triggered after update
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void labelsEq_string_equal_noUpsert() {
    String html = """
    <div id="d4e2129">
      <dl>
        <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_md5">MD5</a></dd>
        <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_sha256">SHA256</a></dd>
      </dl>
    </div>
    """;
    mockHttpGet(CHECKSUM_SCHEME, 200, html);

    // Existing matches normalized fresh labels ("MD5", "SHA-256")
    JsonArray existingItems = new JsonArray()
      .add(new JsonObject().put("value", "http://spdx.org/rdf/terms#checksumAlgorithm_md5").put("label", "MD5"))
      .add(new JsonObject().put("value", "http://spdx.org/rdf/terms#checksumAlgorithm_sha256").put("label", "SHA-256"));
    JsonObject existing = docWithItems(CHECKSUM_SCHEME, existingItems);

    manager.refreshBySource(CHECKSUM_SCHEME, existing)
      .toCompletionStage().toCompletableFuture().join();

    verify(mongo, never()).updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class));
    // checksum labels are plain strings -> no translation
    verify(hubTranslationClient, never()).requestTranslation(any());
  }

  @Test
  void labelsEq_string_mismatch_triggersUpsert() {
    String html = """
    <div id="d4e2129">
      <dl>
        <dd><a title="http://spdx.org/rdf/terms#checksumAlgorithm_md5">MD5</a></dd>
      </dl>
    </div>
    """;
    mockHttpGet(CHECKSUM_SCHEME, 200, html);

    // Existing label differs
    JsonArray existingItems = new JsonArray()
      .add(new JsonObject()
        .put("value", "http://spdx.org/rdf/terms#checksumAlgorithm_md5")
        .put("label", "MD5x"));
    JsonObject existing = docWithItems(CHECKSUM_SCHEME, existingItems);

    when(mongo.updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    manager.refreshBySource(CHECKSUM_SCHEME, existing)
      .toCompletionStage().toCompletableFuture().join();

    verify(mongo, times(1)).updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class));
    verify(hubTranslationClient, never()).requestTranslation(any()); // string labels -> no translation
  }

  @Test
  void labelsEq_mixedTypes_stringVsMultilingual_triggersUpsert() {
    // Fresh will be multilingual ("en")
    String xml = """
    <Dictionary xmlns="http://www.opengis.net/gml/3.2" xmlns:gml="http://www.opengis.net/gml/3.2">
      <dictionaryEntry>
        <Definition gml:id="_1">
          <description>party that supplies the resource</description>
          <identifier>resourceProvider</identifier>
        </Definition>
      </dictionaryEntry>
    </Dictionary>
    """;
    mockHttpGet(ROLE_CODE_SCHEME, 200, xml);

    // Existing has same value but a String label (not JsonObject)
    JsonArray existingItems = new JsonArray()
      .add(new JsonObject()
        .put("value", ROLE_CODE_SCHEME + "#resourceProvider")
        .put("label", "Resource Provider – Party that supplies the resource")); // String, not object
    JsonObject existing = docWithItems(ROLE_CODE_SCHEME, existingItems);

    when(mongo.updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class)))
      .thenReturn(Future.succeededFuture());

    manager.refreshBySource(ROLE_CODE_SCHEME, existing)
      .toCompletionStage().toCompletableFuture().join();

    verify(mongo, times(1)).updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class), any(UpdateOptions.class));
    verify(hubTranslationClient, atLeastOnce()).requestTranslation(any(JsonObject.class));
  }

  @Test
  void getAll_returnsDocuments_whenMongoSucceeds() {
    List<JsonObject> docs = List.of(
      new JsonObject().put("source", "s1").put("items", new JsonArray()),
      new JsonObject().put("source", "s2").put("items", new JsonArray())
    );

    when(mongo.find(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(docs));

    List<JsonObject> result = manager.getAll()
      .toCompletionStage().toCompletableFuture().join();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("s1", result.get(0).getString("source"));

    verify(mongo).find(eq(CONTROLLED_VOCABULARY_COLLECTION), eq(new JsonObject()));
  }

  @Test
  void getAll_failsFuture_whenMongoFails() {
    RuntimeException boom = new RuntimeException("db down");
    when(mongo.find(eq(CONTROLLED_VOCABULARY_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture(boom));

    CompletionException ex = assertThrows(CompletionException.class, () ->
      manager.getAll().toCompletionStage().toCompletableFuture().join()
    );

    assertTrue(ex.getCause() instanceof RuntimeException);
    assertEquals("db down", ex.getCause().getMessage());

    verify(mongo).find(eq(CONTROLLED_VOCABULARY_COLLECTION), eq(new JsonObject()));
  }

  @Test
  void findAnyPrefLabel_returnsFirstLiteral() throws Exception {
    // Build a tiny Jena model with a concept and two prefLabels in different langs
    Model model = ModelFactory.createDefaultModel();
    String conceptUri = "http://example.com/c1";
    Resource concept = model.createResource(conceptUri);
    concept.addProperty(SKOS.prefLabel, model.createLiteral("Erste Beschriftung", "de"));
    concept.addProperty(SKOS.prefLabel, model.createLiteral("Première étiquette", "fr"));

    // Reflectively call private method
    Method m = ControlledVocabularyManager.class.getDeclaredMethod("findAnyPrefLabel", Resource.class);
    m.setAccessible(true);
    String result = (String) m.invoke(manager, concept);

    assertTrue(
      result.equals("Erste Beschriftung") || result.equals("Première étiquette"),
      "Expected one of the available prefLabels, got: " + result
    );
  }

  @Test
  void findAnyPrefLabel_returnsNullWhenNoLiteral() throws Exception {
    Model model = ModelFactory.createDefaultModel();
    String conceptUri = "http://example.com/c2";
    Resource concept = model.createResource(conceptUri);
    // Add a non-literal object just to be sure it is ignored (subject->predicate->resource)
    concept.addProperty(SKOS.prefLabel, model.createResource("http://example.com/not-a-literal"));

    Method m = ControlledVocabularyManager.class.getDeclaredMethod("findAnyPrefLabel", Resource.class);
    m.setAccessible(true);
    String result = (String) m.invoke(manager, concept);

    assertNull(result);
  }

  @Test
  void safeHead_truncatesAt400AndNormalizesWhitespace() throws Exception {
    // Build a long string with newlines/tabs
    StringBuilder sb = new StringBuilder();
    sb.append("line1\nline2\tline3  line4 ");
    while (sb.length() < 450) {
      sb.append(" x ");
    }
    String longText = sb.toString();

    Buffer buf = Buffer.buffer(longText);

    Method m = ControlledVocabularyManager.class.getDeclaredMethod("safeHead", Buffer.class);
    m.setAccessible(true);
    String head = (String) m.invoke(manager, buf);

    // Assert length <= 400 and whitespace normalized to single spaces (no \n or \t)
    assertTrue(head.length() <= 400, "Expected head to be <= 400 chars");
    assertFalse(head.contains("\n"));
    assertFalse(head.contains("\t"));
    assertTrue(head.contains("line1")); // still begins with the original content
  }

  @Test
  void safeHead_returnsUnreadableOnException() throws Exception {
    // Mock a Buffer that throws when toString(Charset) is called
    Buffer evil = mock(Buffer.class);
    when(evil.toString(eq(StandardCharsets.UTF_8))).thenThrow(new RuntimeException("boom"));

    Method m = ControlledVocabularyManager.class.getDeclaredMethod("safeHead", Buffer.class);
    m.setAccessible(true);
    String head = (String) m.invoke(manager, evil);

    assertEquals("<unreadable>", head);
  }


}
