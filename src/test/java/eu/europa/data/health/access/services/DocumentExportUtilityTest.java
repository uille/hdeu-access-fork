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

import static eu.europa.data.health.access.Constants.ADDRESS;
import static eu.europa.data.health.access.Constants.COUNTRY_CODE;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.EXPORT_DATASET_EXTERNAL_URL;
import static eu.europa.data.health.access.Constants.EXTERNAL_DATASET_ID;
import static eu.europa.data.health.access.Constants.FULL_NAME;
import static eu.europa.data.health.access.Constants.INVOICE_TYPE;
import static eu.europa.data.health.access.Constants.IS_THE_PROJECT_FINANCIALLY_COVERED;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.NUMBER;
import static eu.europa.data.health.access.Constants.PHONE;
import static eu.europa.data.health.access.Constants.RESULT;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.VALUE;
import static eu.europa.data.health.access.Constants.VARIABLES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * Tests for DocumentExportUtility to maximize line/branch coverage.
 */
class DocumentExportUtilityTest {

  private WebClient webClient;
  private TranslationManager translationManager;
  private JsonObject config;

  private TestExportUtil util;

  @BeforeEach
  void setup() {
    webClient = mock(WebClient.class);
    translationManager = mock(TranslationManager.class);

    config = new JsonObject()
      .put(EXPORT_DATASET_EXTERNAL_URL, "https://example.org/datasets/");

    util = new TestExportUtil(config, webClient, translationManager);
  }

  /** Minimal concrete subclass for testing. */
  static class TestExportUtil extends DocumentExportUtility {
    TestExportUtil(JsonObject config, WebClient client, TranslationManager translationManager) {
      super(config, client, translationManager);
    }

    @Override
    protected void addFormData(WordprocessingMLPackage document, JsonObject languageMap, JsonObject form, JsonArray datasetsValues) {
      // minimal deterministic action to ensure it runs
      document.getMainDocumentPart().addParagraphOfText("ADD_FORM_DATA_CALLED");
    }
  }

  // ----------------------------
  // Helpers
  // ----------------------------

  private static WordprocessingMLPackage newDoc() throws Exception {
    return WordprocessingMLPackage.createPackage();
  }

  private static void ensureTemplateExists(String filename) throws Exception {
    // Maven default: target/test-classes is on the test classpath
    Path templatesDir = Paths.get("target", "test-classes", "templates");
    Files.createDirectories(templatesDir);

    Path templateFile = templatesDir.resolve(filename);
    if (!Files.exists(templateFile)) {
      WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
      pkg.getMainDocumentPart().addParagraphOfText("TEMPLATE");
      pkg.save(templateFile.toFile());
    }
  }

  private static RoutingContext mockRoutingContext(HttpServerResponse response) {
    RoutingContext ctx = mock(RoutingContext.class);
    when(ctx.response()).thenReturn(response);
    return ctx;
  }

  private static HttpServerResponse mockResponse() {
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
    when(resp.setStatusCode(anyInt())).thenReturn(resp);
    // no stubbing for end(...)
    return resp;
  }

  private static JsonObject minimalLanguageMap() {
    // Build only keys used by tested methods (section4Mappings, section60, variables table, etc.)
    JsonObject lm = new JsonObject();

    // generic labels needed
    lm.put("section4h5", "Section 4");
    lm.put("section4Description", "Section 4 description");

    lm.put("descriptionOfTheDataNeeded", "Description of the data needed:");
    lm.put("applicationHDABsForSelectedCountry", "HDABs:");
    lm.put("applicationDatasetsPerCountry", "Datasets:");

    // labels as JsonObjects: languageMap.getJsonObject(KEY).getString(LABEL)
    lm.put(SAME_AS_CONTACT_PERSON, new JsonObject().put(LABEL, "Same as contact person"));
    lm.put(FULL_NAME, new JsonObject().put(LABEL, "Full name"));
    lm.put(EMAIL, new JsonObject().put(LABEL, "Email"));
    lm.put(PHONE, new JsonObject().put(LABEL, "Phone"));
    lm.put(ADDRESS, new JsonObject().put(LABEL, "Address"));
    lm.put(INVOICE_TYPE, new JsonObject().put(LABEL, "Invoice type"));
    lm.put(IS_THE_PROJECT_FINANCIALLY_COVERED, new JsonObject().put(LABEL, "Financially covered?"));

    lm.put("nameOfTheOrganisation", new JsonObject().put(LABEL, "Organisation"));
    lm.put("businessIdentifierOrganization", new JsonObject().put(LABEL, "Business ID"));
    lm.put("vatNumber", new JsonObject().put(LABEL, "VAT"));
    lm.put("invoiceReferenceNumber", new JsonObject().put(LABEL, "Invoice ref"));
    lm.put("invoiceAddress", new JsonObject().put(LABEL, "Invoice address"));
    lm.put("operatorIdentifier", new JsonObject().put(LABEL, "Operator identifier"));
    lm.put("peppolCode", new JsonObject().put(LABEL, "Peppol code"));
    lm.put("rangeOfAmountOfFinancing", new JsonObject().put(LABEL, "Financing range"));

    lm.put("hdabContacts", new JsonObject().put(LABEL, "HDAB contacts"));
    lm.put("howWillTheDataFromDifferentSourcesBeLinked", new JsonObject().put(LABEL, "Data linkage"));

    // variables table headers
    lm.put(VARIABLES, new JsonObject().put(LABEL, "Variables"));
    lm.put("datatype", new JsonObject().put(LABEL, "Datatype"));
    lm.put("description", new JsonObject().put(LABEL, "Description"));

    return lm;
  }

  // ----------------------------
  // Small utility methods coverage
  // ----------------------------

  @Test
  void addTextToNewParagraph_shouldAddNothingWhenNull() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    int before = doc.getMainDocumentPart().getContent().size();

    util.addTextToNewParagraph(doc, null, true, true);

    int after = doc.getMainDocumentPart().getContent().size();
    assertEquals(before, after);
  }

  @Test
  void addTextToNewParagraph_shouldAddParagraphAndRunProps() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    int before = doc.getMainDocumentPart().getContent().size();

    util.addTextToNewParagraph(doc, "Hello", true, true);

    int after = doc.getMainDocumentPart().getContent().size();
    assertTrue(after > before);
  }

  @Test
  void addTextToNewParagraphWithTable_shouldAddNothingWhenNull() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    int before = doc.getMainDocumentPart().getContent().size();

    util.addTextToNewParagraphWithTable(doc, null, true, true);

    int after = doc.getMainDocumentPart().getContent().size();
    assertEquals(before, after);
  }

  @Test
  void addTextToNewParagraphWithTable_shouldAddTable() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    int before = doc.getMainDocumentPart().getContent().size();

    util.addTextToNewParagraphWithTable(doc, "Cell text", true, false);

    int after = doc.getMainDocumentPart().getContent().size();
    assertTrue(after > before);
  }

  @Test
  void generatedJsonArrayValues_shouldCreateEmptyTableWhenArrayNullOrEmpty() throws Exception {
    WordprocessingMLPackage doc1 = newDoc();
    util.generatedJsonArrayValues(doc1, null, "x");

    WordprocessingMLPackage doc2 = newDoc();
    util.generatedJsonArrayValues(doc2, new JsonArray(), "x");

    assertTrue(doc1.getMainDocumentPart().getContent().size() > 0);
    assertTrue(doc2.getMainDocumentPart().getContent().size() > 0);
  }

  @Test
  void generatedJsonArrayValues_shouldCreateRowsForItemsAndHandleNullText() throws Exception {
    WordprocessingMLPackage doc = newDoc();

    JsonArray arr = new JsonArray()
      .add(new JsonObject().put("name", "A"))
      .add(new JsonObject()); // missing "name" => null => empty paragraph branch

    util.generatedJsonArrayValues(doc, arr, "name");

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  @Test
  void containsKey_and_containsSelectedValueWithoutSpecificValue_shouldWork() {
    JsonArray arr = new JsonArray()
      .add(new JsonObject().put("key", "a"))
      .add(new JsonObject().put("key", "b"));

    assertTrue(util.containsKey(arr, "a"));
    assertFalse(util.containsKey(arr, "x"));

    assertTrue(util.containsSelectedValueWithoutSpecificValue(arr, "a")); // b exists
    assertFalse(util.containsSelectedValueWithoutSpecificValue(new JsonArray().add(new JsonObject().put("key", "a")), "a"));
  }

  @Test
  void filterDatasetsByCatalogIds_shouldIncludeExternalIdAndMatchingCatalog() {
    JsonArray catalogs = new JsonArray().add("cat-1");

    JsonObject external = new JsonObject().put(EXTERNAL_DATASET_ID, "EXT-1");

    JsonObject normalMatch = new JsonObject()
      .put(RESULT, new JsonObject()
        .put("catalog", new JsonObject().put("id", "cat-1")));

    JsonObject normalNoMatch = new JsonObject()
      .put(RESULT, new JsonObject()
        .put("catalog", new JsonObject().put("id", "cat-X")));

    JsonArray datasets = new JsonArray().add(external).add(normalMatch).add(normalNoMatch);

    JsonArray filtered = util.filterDatasetsByCatalogIds(datasets, catalogs);

    assertEquals(2, filtered.size());
  }

  @Test
  void safeFieldHelpers_shouldCoverNullAndPresentCases() {
    JsonObject sec = new JsonObject()
      .put("a", "A")
      .put("arr", new JsonArray().add(1))
      .put("flag", true)
      .put("obj", new JsonObject().put(VALUE, "V"))
      .put("keyObj", new JsonObject().put("key", "k"));

    assertEquals("A", util.getField(sec, "a"));
    assertEquals("", util.getField(null, "a"));
    assertEquals("", util.getField(new JsonObject(), "a"));

    assertNotNull(util.getArrayField(sec, "arr"));
    assertNull(util.getArrayField(null, "arr"));
    assertNull(util.getArrayField(new JsonObject(), "arr"));

    assertTrue(util.isChecked(sec, "flag"));
    assertFalse(util.isChecked(null, "flag"));

    assertNotNull(util.getObject(sec, "obj"));
    assertNull(util.getObject(null, "obj"));

    assertEquals("V", util.getObjectValue(sec, "obj"));
    assertEquals("", util.getObjectValue(sec, "missing"));

    assertTrue(util.isKey(sec.getJsonObject("keyObj"), "k"));
    assertFalse(util.isKey(sec.getJsonObject("keyObj"), "x"));
    assertFalse(util.isKey(null, "k"));

    assertEquals("+30 210", util.buildPhone(new JsonObject().put(COUNTRY_CODE, "30").put(NUMBER, "210")));
    assertEquals("", util.buildPhone(null));
  }

  @Test
  void extractCatalogIds_shouldReturnEmptyForNullOrEmpty() {
    assertTrue(DocumentExportUtility.extractCatalogIds(null).isEmpty());
    assertTrue(DocumentExportUtility.extractCatalogIds(new JsonArray()).isEmpty());
  }

  @Test
  void extractCatalogIds_shouldExtractOnlyNonNullCatalogIds() {
    JsonArray hubDatasets = new JsonArray()
      .add(new JsonObject().put(RESULT, new JsonObject().put("catalog", new JsonObject().put("id", "c1"))))
      .add(new JsonObject().put(RESULT, new JsonObject().put("catalog", new JsonObject()))) // null id
      .add(new JsonObject()); // missing

    assertEquals(1, DocumentExportUtility.extractCatalogIds(hubDatasets).size());
    assertEquals("c1", DocumentExportUtility.extractCatalogIds(hubDatasets).get(0));
  }

  @Test
  void findSection6ObjectByCatalogId_shouldHandleNullsAndMatches() {
    assertNull(DocumentExportUtility.findSection6ObjectByCatalogId(null, "c"));
    assertNull(DocumentExportUtility.findSection6ObjectByCatalogId(new JsonArray(), "c"));
    assertNull(DocumentExportUtility.findSection6ObjectByCatalogId(new JsonArray().add(new JsonObject()), null));

    JsonObject s1 = new JsonObject().put("catalog_ids", new JsonArray().add("c1").add("c2"));
    JsonObject s2 = new JsonObject().put("catalog_ids", new JsonArray().add("x"));

    JsonArray section6 = new JsonArray().add(s1).add(s2);

    assertSame(s1, DocumentExportUtility.findSection6ObjectByCatalogId(section6, "c2"));
    assertNull(DocumentExportUtility.findSection6ObjectByCatalogId(section6, "nope"));
  }

  @Test
  void generateTableOfContext_shouldNotThrowEvenIfNullDocument() {
    // This forces TocGenerator to blow up; method must catch and not throw
    assertDoesNotThrow(() -> util.generateTableOfContext(null));
  }

  // ----------------------------
  // section4Mappings / section60 coverage
  // ----------------------------

  @Test
  void section4Mappings_shouldCoverLegalPersonBranch_invoiceB_operator_and_financingA() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    JsonObject languageMap = minimalLanguageMap();

    JsonObject section = new JsonObject()
      .put(SAME_AS_CONTACT_PERSON, true)
      .put(FULL_NAME, "John Doe")
      .put(EMAIL, "john@doe.eu")
      .put(PHONE, new JsonObject().put(COUNTRY_CODE, "30").put(NUMBER, "210"))
      .put("nameOfTheOrganisation", "Org")
      .put(ADDRESS, "Addr")
      .put("businessIdentifierOrganization", "BIZ")
      .put("vatNumber", "VAT")
      .put(INVOICE_TYPE, new JsonObject().put("key", "b").put(VALUE, "B"))
      .put("invoiceReferenceNumber", "INV-REF")
      .put("invoiceAddress", "INV-ADDR")
      .put("operatorIdentifier", "OPID")
      .put("peppolCode", "PEP")
      .put(IS_THE_PROJECT_FINANCIALLY_COVERED, new JsonObject().put("key", "a").put(VALUE, "YES"))
      .put("rangeOfAmountOfFinancing", new JsonObject().put(VALUE, "10-20"));

    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "a")); // legal person => big branch

    util.section4Mappings(doc, languageMap, section, section3, "ABOUT");

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  @Test
  void section4Mappings_shouldCoverNaturalPersonBranch_and_invoiceNotB_and_financingNotA() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    JsonObject languageMap = minimalLanguageMap();

    JsonObject section = new JsonObject()
      .put(SAME_AS_CONTACT_PERSON, false)
      .put(FULL_NAME, "Jane")
      .put(EMAIL, "jane@x.eu")
      .put(PHONE, new JsonObject().put(COUNTRY_CODE, "1").put(NUMBER, "555"))
      .put(ADDRESS, "Addr2")
      .put(INVOICE_TYPE, new JsonObject().put("key", "a").put(VALUE, "A")) // not "b"
      .put("invoiceReferenceNumber", "REF")
      .put(IS_THE_PROJECT_FINANCIALLY_COVERED, new JsonObject().put("key", "b").put(VALUE, "NO"));

    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "b")); // natural person => else branch

    util.section4Mappings(doc, languageMap, section, section3, "ABOUT");

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  @Test
  void section60_shouldWriteDatasetInfoAndFields() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    JsonObject languageMap = minimalLanguageMap();

    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put(RESULT, new JsonObject()
        .put("country", new JsonObject().put("label", "Greece"))
        .put("hdab", new JsonObject().put("name", "HDAB Name"))
        .put(TITLE, new JsonObject().put("en", "Dataset Title"))
      ));

    JsonObject section = new JsonObject()
      .put("hdabContacts", "Contacts")
      .put("howWillTheDataFromDifferentSourcesBeLinked", "Linked by X");

    util.section60(doc, languageMap, section, datasets);

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  // ----------------------------
  // writeDatasetsValues / writeDatasetsValuesWithVariables coverage
  // ----------------------------

  @Test
  void writeDatasetsValues_shouldHandleExternalAndNormal() throws Exception {
    WordprocessingMLPackage doc = newDoc();

    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put(EXTERNAL_DATASET_ID, "EXT-7"))
      .add(new JsonObject().put(RESULT, new JsonObject().put("hdab", new JsonObject().put("name", "HDAB-1"))));

    util.writeDatasetsValues(doc, datasets, "hdab", "name");

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  @Test
  void writeDatasetsValuesWithVariables_shouldCreateVariablesTable_and_checkboxes() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    JsonObject languageMap = minimalLanguageMap();

    // Section1 chooses one variable
    JsonObject section1 = new JsonObject()
      .put("datasetVariables", new JsonArray()
        .add(new JsonObject().put("datasetId", "ds-1").put("name", "varA")));

    // Distributions sample with variables (including duplicate name to hit distinct logic)
    JsonArray distributionsSample = new JsonArray()
      .add(new JsonObject().put(VARIABLES, new JsonArray()
        .add(new JsonObject()
          .put("name", "varA")
          .put("datatype", "string")
          .put("description", new JsonObject().put("en", "desc-en").put("el", "desc-el")))
        .add(new JsonObject()
          .put("name", "varA")
          .put("datatype", "string")
          .put("description", new JsonObject().put("en", "dup")))));

    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put(RESULT, new JsonObject()
        .put("id", "ds-1")
        .put(TITLE, new JsonObject().put("en", "Dataset A"))
        .put("distributions_sample", distributionsSample)));

    // property1=TITLE, property2="en" => value exists => branch enters variables table
    util.writeDatasetsValuesWithVariables(doc, datasets, TITLE, "en", section1, languageMap);

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  @Test
  void writeDatasetsValuesWithVariables_shouldSkipVariablesTableWhenNoDistributionsOrNoVars() throws Exception {
    WordprocessingMLPackage doc = newDoc();
    JsonObject languageMap = minimalLanguageMap();

    JsonObject section1 = new JsonObject(); // no selections

    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put(RESULT, new JsonObject()
        .put("id", "ds-1")
        .put(TITLE, new JsonObject().put("en", "Dataset A"))
        .put("distributions_sample", new JsonArray())));

    util.writeDatasetsValuesWithVariables(doc, datasets, TITLE, "en", section1, languageMap);

    assertTrue(doc.getMainDocumentPart().getContent().size() > 0);
  }

  // ----------------------------
  // getTranslateValueForKey coverage
  // ----------------------------

  @Test
  void getTranslateValueForKey_shouldReturnValueOrNull() {
    JsonObject translations = new JsonObject().put("items", new JsonArray()
      .add(new JsonObject().put("key", "k1").put("label", "L1").put("value", "V1"))
      .add(new JsonObject().put("key", "k2").put("label", "L2").put("value", "V2"))
    );

    assertEquals("L2", util.getTranslateValueForKey(translations, "k2", "label"));
    assertNull(util.getTranslateValueForKey(translations, "nope", "label"));
  }

  // ----------------------------
  // getDatasetsValues + export coverage (docx/pdf/unsupported + exception)
  // ----------------------------

  @Test
  void getDatasetsValues_shouldExportDocx_success() throws Exception {
    ensureTemplateExists("test-template.docx");

    JsonObject languageMap = minimalLanguageMap();
    when(translationManager.getTranslationByLanguish("en")).thenReturn(languageMap);

    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockRoutingContext(resp);

    // Mock HubSearchClient construction so dataset retrieval is controlled
    try (MockedConstruction<HubSearchClient> mocked = Mockito.mockConstruction(
      HubSearchClient.class,
      (mock, context) -> when(mock.getDataset(anyString()))
        .thenReturn(Future.succeededFuture(new JsonObject().put(RESULT, new JsonObject().put("id", "dsX"))))
    )) {
      JsonArray datasets = new JsonArray()
        .add(new JsonObject().put("dataset_id", "ds-1"))
        .add(new JsonObject().put("dataset_id", "ds-2"));

      util.getDatasetsValues(ctx, datasets, "en", new JsonObject().put("form", true), "docx", "test-template.docx");

      // verify response was written
      verify(resp, atLeastOnce()).putHeader(eq("Content-Type"), contains("wordprocessingml"));
      verify(resp, atLeastOnce()).end(any(Buffer.class));

      // also ensures loop ran (two getDataset calls)
      HubSearchClient constructed = mocked.constructed().get(0);
      verify(constructed, times(2)).getDataset(anyString());
    }
  }

  @Test
  void getDatasetsValues_shouldExportPdf_branch() throws Exception {
    ensureTemplateExists("test-template.docx");

    JsonObject languageMap = minimalLanguageMap();
    when(translationManager.getTranslationByLanguish("en")).thenReturn(languageMap);

    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockRoutingContext(resp);

    // empty datasets => no getDataset calls, still goes through export path
    try (MockedConstruction<HubSearchClient> mocked = Mockito.mockConstruction(
      HubSearchClient.class,
      (mock, context) -> when(mock.getDataset(anyString()))
        .thenReturn(Future.succeededFuture(new JsonObject()))
    )) {
      util.getDatasetsValues(ctx, new JsonArray(), "en", new JsonObject(), "pdf", "test-template.docx");

      verify(resp, atLeastOnce()).putHeader("Content-Type", "application/pdf");
      verify(resp, atLeastOnce()).end(any(Buffer.class));
    }
  }

  @Test
  void getDatasetsValues_shouldReturn400_onUnsupportedType() throws Exception {
    ensureTemplateExists("test-template.docx");

    JsonObject languageMap = minimalLanguageMap();
    when(translationManager.getTranslationByLanguish("en")).thenReturn(languageMap);

    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockRoutingContext(resp);

    try (MockedConstruction<HubSearchClient> mocked = Mockito.mockConstruction(
      HubSearchClient.class,
      (mock, context) -> when(mock.getDataset(anyString()))
        .thenReturn(Future.succeededFuture(new JsonObject()))
    )) {
      util.getDatasetsValues(ctx, new JsonArray(), "en", new JsonObject(), "zip", "test-template.docx");

      verify(resp).setStatusCode(400);
      verify(resp).end(contains("Unsupported export type"));
    }
  }

  @Test
  void getDatasetsValues_shouldReturn500_ifTemplateMissing() throws IOException {
    // no template created => export throws => catches => 500
    JsonObject languageMap = minimalLanguageMap();
    when(translationManager.getTranslationByLanguish("en")).thenReturn(languageMap);

    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockRoutingContext(resp);

    try (MockedConstruction<HubSearchClient> mocked = Mockito.mockConstruction(
      HubSearchClient.class,
      (mock, context) -> when(mock.getDataset(anyString()))
        .thenReturn(Future.succeededFuture(new JsonObject()))
    )) {
      util.getDatasetsValues(ctx, new JsonArray(), "en", new JsonObject(), "docx", "missing-template.docx");

      verify(resp).setStatusCode(500);
      verify(resp).end();
    }
  }
}
