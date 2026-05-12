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

import static eu.europa.data.health.access.Constants.ACCEPT_HEALTH_DATA_BODY;
import static eu.europa.data.health.access.Constants.ADDITIONAL_ATTACHMENT;
import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION;
import static eu.europa.data.health.access.Constants.ADDRESS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.AWARE_CHARGE_FEE;
import static eu.europa.data.health.access.Constants.AWARE_INFORMATION_CORRECT;
import static eu.europa.data.health.access.Constants.AWARE_PROCESSING_FEE;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.COUNTRY_CODE;
import static eu.europa.data.health.access.Constants.COUNTRY_OF_PROJECT_LEADER;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.EXPORT_DATASET_EXTERNAL_URL;
import static eu.europa.data.health.access.Constants.EXTRACTION_DESCRIPTION_AND_COMPILATION;
import static eu.europa.data.health.access.Constants.FULL_NAME;
import static eu.europa.data.health.access.Constants.HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE;
import static eu.europa.data.health.access.Constants.INCLUSION_CRITERIA;
import static eu.europa.data.health.access.Constants.INVOICE_TYPE;
import static eu.europa.data.health.access.Constants.IS_THE_PROJECT_FINANCIALLY_COVERED;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.NUMBER;
import static eu.europa.data.health.access.Constants.OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PHONE;
import static eu.europa.data.health.access.Constants.PROJECT_LEADER;
import static eu.europa.data.health.access.Constants.PROVENANCE;
import static eu.europa.data.health.access.Constants.PUBLISHER;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.RESULT;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SAMPLE_SIZE;
import static eu.europa.data.health.access.Constants.SECTION_1;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_PLAN_FOR_USING_THE_DATA;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_RESEARCH_PLAN;
import static eu.europa.data.health.access.Constants.TABULATION_PLAN;
import static eu.europa.data.health.access.Constants.TABULATION_PLAN_ARRAY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.TOOLTIP;
import static eu.europa.data.health.access.Constants.VALUE;
import static eu.europa.data.health.access.Constants.VARIABLES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentExportDataRequestManagerTest {

  private TranslationManager translationManager;
  private DBHandler dbHandler;
  private WebClient webClient;
  private JsonObject config;

  private TestableManager manager; // used for routing tests

  @BeforeEach
  void setup() {
    translationManager = mock(TranslationManager.class);
    dbHandler = mock(DBHandler.class);
    webClient = mock(WebClient.class);
    config = new JsonObject().put(EXPORT_DATASET_EXTERNAL_URL, "https://example/");

    manager = new TestableManager(translationManager, dbHandler, webClient, config);
  }

  /**
   * Testable subclass that prevents real export logic (docx/template/hub calls).
   * We only capture arguments passed to getDatasetsValues(...).
   */
  static class TestableManager extends DocumentExportDataRequestManager {
    RoutingContext capturedCtx;
    JsonArray capturedDatasets;
    String capturedLg;
    JsonObject capturedForm;
    String capturedType;
    String capturedFilename;

    TestableManager(TranslationManager translationManager, DBHandler dbHandler, WebClient client, JsonObject config) {
      super(translationManager, dbHandler, client, config);
    }

    @Override
    protected void getDatasetsValues(RoutingContext context, JsonArray datasets, String lg,
                                     JsonObject form, String type, String filename) {
      this.capturedCtx = context;
      this.capturedDatasets = datasets;
      this.capturedLg = lg;
      this.capturedForm = form;
      this.capturedType = type;
      this.capturedFilename = filename;
      // do not call super
    }
  }

  // -----------------------------------------------------------------------
  // RoutingContext/Response helpers (safe across Vert.x signatures)
  // -----------------------------------------------------------------------

  private static HttpServerResponse mockResponse() {
    HttpServerResponse resp = mock(HttpServerResponse.class);
    when(resp.setStatusCode(anyInt())).thenReturn(resp);
    when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
    // don't stub end(...) (signature differs across Vert.x versions)
    return resp;
  }

  private static RoutingContext mockContextWithResponse(HttpServerResponse resp) {
    RoutingContext ctx = mock(RoutingContext.class);
    when(ctx.response()).thenReturn(resp);
    return ctx;
  }

  // -----------------------------------------------------------------------
  // 1) SAFE routing/DB fallback tests (won't fail / no IO)
  // -----------------------------------------------------------------------

  @Test
  void exportApplicationDataRequestForm_invalidVersion_shouldReturn400_andStop() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("app-1");
    when(ctx.pathParam("type")).thenReturn("docx");
    when(ctx.pathParam("tl")).thenReturn("en");
    when(ctx.pathParam("version")).thenReturn("NOT_A_NUMBER");

    manager.exportApplicationDataRequestForm(ctx);

    verify(resp).setStatusCode(400);
    verify(resp).end(contains("Invalid application version"));
    verifyNoInteractions(dbHandler);
    assertNull(manager.capturedFilename);
  }

  @Test
  void exportApplicationDataRequestForm_dbFindOneSuccess_applicationFound_shouldProcess_andUseFormWhenTlMatchesInputLanguage() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("app-1");
    when(ctx.pathParam("type")).thenReturn("docx");
    when(ctx.pathParam("tl")).thenReturn("en");
    when(ctx.pathParam("version")).thenReturn("2");

    JsonObject form = new JsonObject().put("x", "y");
    JsonArray datasets = new JsonArray().add(new JsonObject().put("dataset_id", "ds-1"));

    JsonObject application = new JsonObject()
      .put("_id", "app-1")
      .put("version", 2)
      .put("inputLanguage", "en")
      .put("form", form)
      .put("datasets", datasets);

    when(dbHandler.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(application));

    manager.exportApplicationDataRequestForm(ctx);

    assertSame(ctx, manager.capturedCtx);
    assertEquals("en", manager.capturedLg);
    assertEquals("docx", manager.capturedType);
    assertEquals("DataRequestForm.docx", manager.capturedFilename);
    assertSame(form, manager.capturedForm);
    assertSame(datasets, manager.capturedDatasets);
  }

  @Test
  void exportApplicationDataRequestForm_dbFindOneSuccess_applicationFound_shouldUseTranslatedFormWhenTlDiffers() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("app-1");
    when(ctx.pathParam("type")).thenReturn("pdf");
    when(ctx.pathParam("tl")).thenReturn("fr");
    when(ctx.pathParam("version")).thenReturn("3");

    JsonObject baseForm = new JsonObject().put("base", true);
    JsonObject frForm = new JsonObject().put("lang", "fr");
    JsonObject formTranslations = new JsonObject().put("fr", frForm);

    JsonObject application = new JsonObject()
      .put("_id", "app-1")
      .put("version", 3)
      .put("inputLanguage", "en")
      .put("form", baseForm)
      .put("form_translations", formTranslations)
      .put("datasets", new JsonArray());

    when(dbHandler.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(application));

    manager.exportApplicationDataRequestForm(ctx);

    assertSame(frForm, manager.capturedForm);
    assertEquals("pdf", manager.capturedType);
  }

  @Test
  void exportApplicationDataRequestForm_dbFindOneSuccess_applicationNull_shouldFallbackToDispatcher_found() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("app-9");
    when(ctx.pathParam("type")).thenReturn("docx");
    when(ctx.pathParam("tl")).thenReturn("en");
    when(ctx.pathParam("version")).thenReturn("1");

    when(dbHandler.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    JsonObject dispatcherApplication = new JsonObject()
      .put("_id", "app-9")
      .put("version", 1)
      .put("inputLanguage", "en")
      .put("form", new JsonObject().put("from", "dispatcher"))
      .put("datasets", new JsonArray());

    JsonObject dispatcherDoc = new JsonObject().put("application", dispatcherApplication);

    when(dbHandler.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(dispatcherDoc));

    manager.exportApplicationDataRequestForm(ctx);

    assertNotNull(manager.capturedForm);
    assertEquals("dispatcher", manager.capturedForm.getString("from"));
    assertEquals("DataRequestForm.docx", manager.capturedFilename);
  }

  @Test
  void exportApplicationDataRequestForm_dbFindOneSuccess_applicationNull_shouldFallbackToDispatcher_notFound_shouldReturn404() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("missing");
    when(ctx.pathParam("type")).thenReturn("docx");
    when(ctx.pathParam("tl")).thenReturn("en");
    when(ctx.pathParam("version")).thenReturn("7");

    when(dbHandler.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    when(dbHandler.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    manager.exportApplicationDataRequestForm(ctx);

    verify(resp).setStatusCode(404);
    verify(resp).end(contains("No application found"));
    assertNull(manager.capturedFilename);
  }

  @Test
  void exportApplicationDataRequestForm_dbFindOneFailure_shouldReturn500() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("app-1");
    when(ctx.pathParam("type")).thenReturn("docx");
    when(ctx.pathParam("tl")).thenReturn("en");
    when(ctx.pathParam("version")).thenReturn("1");

    when(dbHandler.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("boom"));

    manager.exportApplicationDataRequestForm(ctx);

    verify(resp).setStatusCode(500);
    verify(resp).end();
    assertNull(manager.capturedFilename);
  }

  @Test
  void exportApplicationDataRequestForm_dispatcherFindFailure_shouldReturn500_withMessage() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    when(ctx.pathParam("lg")).thenReturn("en");
    when(ctx.pathParam("id")).thenReturn("app-1");
    when(ctx.pathParam("type")).thenReturn("docx");
    when(ctx.pathParam("tl")).thenReturn("en");
    when(ctx.pathParam("version")).thenReturn("1");

    when(dbHandler.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(null));

    when(dbHandler.findOne(eq(DISPATCHER_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.failedFuture("db down"));

    manager.exportApplicationDataRequestForm(ctx);

    verify(resp).setStatusCode(500);
    verify(resp).end(contains("Database error"));
    assertNull(manager.capturedFilename);
  }

  @Test
  void externalExportApplicationForm_shouldReadBodyAndCallGetDatasetsValues() {
    HttpServerResponse resp = mockResponse();
    RoutingContext ctx = mockContextWithResponse(resp);

    RequestBody body = mock(RequestBody.class);
    when(ctx.body()).thenReturn(body);

    JsonArray datasets = new JsonArray().add(new JsonObject().put("dataset_id", "ds-1"));
    JsonObject form = new JsonObject().put("k", "v");
    JsonObject payload = new JsonObject().put("datasets", datasets).put("form", form);

    when(body.asJsonObject()).thenReturn(payload);

    manager.externalExportApplicationForm(ctx);

    assertSame(ctx, manager.capturedCtx);
    assertSame(datasets, manager.capturedDatasets);
    assertSame(form, manager.capturedForm);
    assertEquals("en", manager.capturedLg);
    assertEquals("docx", manager.capturedType);
    assertEquals("DataRequestForm.docx", manager.capturedFilename);
  }

  // -----------------------------------------------------------------------
  // 2) addFormData + sections coverage (direct call, real doc object, no IO)
  // -----------------------------------------------------------------------

  @Test
  void addFormData_shouldCoverAllSections_legalPerson_summaryChecked_true_andSection61_tabulationPlanArray() throws Exception {
    DocumentExportDataRequestManager real =
      new DocumentExportDataRequestManager(translationManager, dbHandler, webClient, config);

    WordprocessingMLPackage doc = WordprocessingMLPackage.createPackage();
    JsonObject languageMap = buildLanguageMap();
    JsonObject form = buildForm_legalPerson_summaryChecked_true();
    JsonArray datasetsValues = buildDatasetsValues_forCatalog("cat-1", "Greece");

    int before = doc.getMainDocumentPart().getContent().size();

    assertDoesNotThrow(() -> real.addFormData(doc, languageMap, form, datasetsValues));

    int after = doc.getMainDocumentPart().getContent().size();
    assertTrue(after > before);
    String documentXml = doc.getMainDocumentPart().getXML();
    assertTrue(documentXml.contains("If there are multiple contact persons in your project, please name them here along with information on their affiliations."));
    assertTrue(documentXml.contains("Additional contacts for the project"));
  }

  @Test
  void addFormData_shouldCoverAllSections_naturalPerson_summaryChecked_false() throws Exception {
    DocumentExportDataRequestManager real =
      new DocumentExportDataRequestManager(translationManager, dbHandler, webClient, config);

    WordprocessingMLPackage doc = WordprocessingMLPackage.createPackage();
    JsonObject languageMap = buildLanguageMap();
    JsonObject form = buildForm_naturalPerson_summaryChecked_false();
    JsonArray datasetsValues = buildDatasetsValues_forCatalog("cat-1", "Greece");

    int before = doc.getMainDocumentPart().getContent().size();

    assertDoesNotThrow(() -> real.addFormData(doc, languageMap, form, datasetsValues));

    int after = doc.getMainDocumentPart().getContent().size();
    assertTrue(after > before);
  }

  // -----------------------------------------------------------------------
  // Builders for languageMap, form, datasetsValues
  // -----------------------------------------------------------------------

  private static JsonObject buildLanguageMap() {
    JsonObject lm = new JsonObject();
    lm.put("commonFields", new JsonObject().put("aboutThisSection", "About this section"));

    lm.put("section1", buildSection1Language());
    lm.put(SECTION_2, buildSection2Language());
    lm.put(SECTION_3, buildSection3Language());
    lm.put("section4", buildSection4Language());
    lm.put(SECTION_5, buildSection5Language());
    lm.put("section6", buildSection6Language());
    lm.put("section9", buildSection7Language());
    lm.put("section10", buildSection8Language());

    return lm;
  }

  private static JsonObject buildSection1Language() {
    JsonObject s = new JsonObject();
    s.put("section1h5", "Section 1");
    s.put("section1Description", "Section 1 description");
    s.put("applicationDatasets", label("Datasets"));
    s.put("applicationCountries", label("Countries"));
    s.put("applicationDataHolders", label("Data holders"));
    s.put("applicationHDABs", label("HDABs"));
    s.put("applicationDatabases", label("Databases"));

    s.put(VARIABLES, label("Variables"));
    s.put("datatype", label("Datatype"));
    s.put("description", label("Description"));
    return s;
  }

  private static JsonObject buildSection2Language() {
    JsonObject s = new JsonObject();
    s.put("section2h5", "Section 2");
    s.put("section2Description", "Section 2 description");
    s.put("projectName", label("Project name"));
    s.put(PROJECT_LEADER, labelTooltip("Project leader", "Project leader tooltip"));
    s.put(COUNTRY_OF_PROJECT_LEADER, label("Country of project leader"));
    s.put(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED, labelTooltip("Purpose", "Purpose tooltip"));

    s.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, label("Cannot provide summary"));
    s.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON, labelTooltip("Reason", "Reason tooltip"));
    s.put("summaryOfTheProject", label("Summary of the project"));
    return s;
  }

  private static JsonObject buildSection3Language() {
    JsonObject s = new JsonObject();
    s.put("section3h5", "Section 3");
    s.put("section3DescriptionRequest", "Section 3 description");

    s.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR, label("Applying on behalf"));
    s.put(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS, labelTooltip("Tasks", "Tasks tooltip"));
    s.put(LEGAL_OR_NATURAL_PERSON, labelTooltip("Legal or natural", "Legal/natural tooltip"));

    s.put("legalPerson", "Legal person");
    s.put("naturalPerson", "Natural person");
    s.put("postalAddress", "Postal address");
    s.put("contactPersonInformation", "Contact person information");
    s.put("contactPersonInformationDescription", "Contact person info description");

    s.put(LEGAL_PERSON_NAME, label("Name"));
    s.put("legalPersonAddress", label("Address"));
    s.put("legalPersonZipCode", label("Zip"));
    s.put("legalPersonCity", label("City"));
    s.put(LEGAL_PERSON_COUNTRY, label("Country"));

    s.put("contactPersonName", label("Contact person name"));
    s.put(CONTACT_PERSON_EMAIL, label("Contact email"));
    s.put(CONTACT_PERSON_PHONE, label("Contact phone"));
    s.put("contactPersonRelationship", label("Relationship"));
    s.put("otherContactPerson", label("If there are multiple contact persons in your project, please name them here along with information on their affiliations."));
    s.put("contactPersonNameOfTheOrganisation", label("Organisation"));
    s.put("contactPersonBusinessIdentifier", label("Business identifier"));

    s.put(NATURAL_PERSON_ADDRESS, label("Natural person address"));
    s.put("naturalPersonZipCode", label("Natural person zip"));
    s.put("naturalPersonCity", label("Natural person city"));
    s.put("naturalPersonJobTitle", label("Job title"));
    s.put("naturalPersonAffiliation", label("Affiliation"));
    return s;
  }

  private static JsonObject buildSection4Language() {
    JsonObject s = new JsonObject();
    s.put("section4h5", "Section 4");
    s.put("section4Description", "Section 4 description");

    s.put(SAME_AS_CONTACT_PERSON, label("Same as contact person"));
    s.put(FULL_NAME, label("Full name"));
    s.put(EMAIL, label("Email"));
    s.put(PHONE, label("Phone"));
    s.put(ADDRESS, label("Address"));

    s.put(INVOICE_TYPE, label("Invoice type"));
    s.put(IS_THE_PROJECT_FINANCIALLY_COVERED, label("Financially covered"));

    s.put("nameOfTheOrganisation", label("Organisation"));
    s.put("businessIdentifierOrganization", label("Business ID"));
    s.put("vatNumber", label("VAT"));
    s.put("invoiceReferenceNumber", label("Invoice reference"));
    s.put("invoiceAddress", label("Invoice address"));
    s.put("operatorIdentifier", label("Operator identifier"));
    s.put("peppolCode", label("Peppol code"));
    s.put("rangeOfAmountOfFinancing", label("Financing range"));
    return s;
  }

  private static JsonObject buildSection5Language() {
    JsonObject s = new JsonObject();
    s.put("section5h5", "Section 5");
    s.put("section5Description", "Section 5 description");

    s.put("purposeOfUse", label("Purpose of use"));
    s.put(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, label("Responsible same as contact"));
    s.put(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, label("Research same as contact"));

    s.put("personResponsibleName", label("Responsible name"));
    s.put("personResearchName", label("Research name"));

    s.put("whyAreTheData_requested_placeholder", ""); // harmless
    s.put("whyAreTheDataRequested", label("Why requested"));
    s.put("whatIsTheAimAndTopicOfTheProject", label("Aim and topic"));
    s.put("whichAreTheExpectedBenefits", label("Expected benefits"));
    s.put("describeApplicantsQualification", label("Qualifications"));

    s.put(LEGAL_BASIS, labelTooltip("Legal basis", "Legal basis tooltip"));
    s.put("linkToTheSupportingLegalBasis", label("Link to legal basis"));

    s.put(SUMMARY_OF_PLAN_FOR_USING_THE_DATA, labelTooltip("Plan summary", "Plan summary tooltip"));
    s.put(SUMMARY_OF_RESEARCH_PLAN, labelTooltip("Research summary", "Research summary tooltip"));

    s.put("personResponsibleLabel", "Person responsible label");
    s.put("personResponsibleForResearchLabel", "Person responsible for research label");
    return s;
  }

  private static JsonObject buildSection6Language() {
    JsonObject s = new JsonObject();
    s.put("section6h5", "Section 6");
    s.put("section6Description", "Section 6 description");

    s.put("descriptionOfTheDataNeeded", "Description of the data needed:");
    s.put("applicationHDABsForSelectedCountry", "HDABs for selected country");
    s.put("applicationDatasetsPerCountry", "Datasets per country");
    s.put("hdabContacts", label("HDAB contacts"));
    s.put("howWillTheDataFromDifferentSourcesBeLinked", label("How linked"));

    s.put("descriptionSection6_1", "Section 6.1 for");

    s.put("indicateTheSizeOfTheStudyCohort", label("Cohort size"));
    s.put("whyNeedStudyCohortOfThisSize", label("Why that size"));

    s.put("dataHolders", "Data holders");
    s.put("whichDatabasesDataExtracted", "Which databases");
    s.put("whichDatasetDataExtracted", "Which datasets");

    s.put("variablesToBeUsedInDataExtractionAttachment", label("Variables attachment"));
    s.put("timePeriodOfDataExtraction", label("Time period"));
    s.put("extractionMethod", label("Extraction method"));
    s.put("samplingMethod", label("Sampling method"));

    s.put(SAMPLE_SIZE, labelTooltip("Sample size", "Sample size tooltip"));
    s.put(INCLUSION_CRITERIA, labelTooltip("Inclusion criteria", "Inclusion tooltip"));
    s.put("potentialExclusionCriteria", label("Exclusion criteria"));

    // IMPORTANT: include BOTH keys (constant and literal) to be safe
    s.put(TABULATION_PLAN_ARRAY, labelTooltip("Tabulation plan array", "Tabulation array tooltip"));
    s.put("tabulationPlanArray", labelTooltip("Tabulation plan array", "Tabulation array tooltip"));

    s.put("tabulationRegisteredToBeUsed", label("Registered to be used"));
    s.put("tabulationPossibleStudyCohort", label("Possible cohort"));
    s.put("tabulationInformationOfRequiredVariables", label("Required variables info"));
    s.put("tabulationFormationVariables", label("Formation variables"));
    s.put("tabulationDesiredDirection", label("Desired direction"));
    s.put("tabulationOrderInWhichTable", label("Order"));
    s.put("tabulationAnyOtherRelevant", label("Any other relevant"));

    s.put(TABULATION_PLAN, labelTooltip("Tabulation plan", "Tabulation plan tooltip"));

    s.put(EXTRACTION_DESCRIPTION_AND_COMPILATION, labelTooltip("Extraction description", "Extraction description tooltip"));
    s.put(DATA_EXTRACTION_FREQUENCY, label("Extraction frequency"));

    s.put(OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW, labelTooltip("Opt out", "Opt out tooltip"));
    s.put("providedJustificationException", label("Justification exception"));

    s.put(NEED_FOR_DATA_EXTRACTION_EVERY, labelTooltip("Need extraction every", "Need extraction tooltip"));
    s.put("needForDataExtractionEveryOther", label("Other frequency"));

    s.put("needForDataExtractionEveryDescriptionDR", labelTooltip("Extraction description DR", "Extraction desc tooltip"));
    s.put("whatIsTheFrequencyOfUpdates", label("Frequency of updates"));
    return s;
  }

  private static JsonObject buildSection7Language() {
    JsonObject s = new JsonObject();
    s.put("section9h5", "Section 9");
    s.put(ADDITIONAL_INFORMATION, labelTooltip("Additional info", "Additional info tooltip"));
    s.put(ADDITIONAL_ATTACHMENT, labelTooltip("Additional attachment", "Additional attachment tooltip"));
    return s;
  }

  private static JsonObject buildSection8Language() {
    JsonObject s = new JsonObject();
    s.put("section10h5", "Section 10");
    s.put("section10Description", "Section 10 description");
    s.put("submitYouApplicationText", "Submit your application text");

    s.put(ACCEPT_HEALTH_DATA_BODY, label("Accept health data body"));
    s.put(AWARE_PROCESSING_FEE, label("Aware processing fee"));
    s.put(AWARE_CHARGE_FEE, label("Aware charge fee"));
    s.put(AWARE_INFORMATION_CORRECT, label("Aware information correct"));
    return s;
  }

  private static JsonObject buildForm_legalPerson_summaryChecked_true() {
    JsonObject form = new JsonObject();

    // SECTION_1 (variables selection)
    form.put(SECTION_1, new JsonObject()
      .put("datasetVariables", new JsonArray()
        .add(new JsonObject().put("datasetId", "ds-1").put("name", "varA"))));

    // SECTION_2 (summary checkbox TRUE path)
    form.put(SECTION_2, new JsonObject()
      .put("projectName", "Project X")
      .put(PROJECT_LEADER, "Leader")
      .put(COUNTRY_OF_PROJECT_LEADER, new JsonObject().put("key", "gr").put(VALUE, "Greece"))
      .put(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED, new JsonArray()
        .add(new JsonObject().put(VALUE, "b"))
        .add(new JsonObject().put(VALUE, "e")))
      // used later by section5/61
      .put("purposeForWhichDataWillBeUsed", new JsonArray()
        .add(new JsonObject().put("key", "b"))
        .add(new JsonObject().put("key", "e")))
      .put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, true)
      .put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON, "Cannot disclose"));

    // SECTION_3 (legal person + applying on behalf "a")
    form.put(SECTION_3, new JsonObject()
      .put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR, new JsonObject().put("key", "a").put(VALUE, "Yes"))
      .put(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS, new JsonObject().put("key", "x").put(VALUE, "Tasks desc"))
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "a").put(VALUE, "Legal"))
      .put("legalPersonName", "Org Ltd")
      .put("legalPersonAddress", "Addr 1")
      .put("legalPersonZipCode", "12345")
      .put("legalPersonCity", "City")
      .put(LEGAL_PERSON_COUNTRY, new JsonObject().put(VALUE, "Greece"))
      .put("contactPersonName", "Contact")
      .put("contactPersonEmail", "c@example.org")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put(COUNTRY_CODE, "30").put(NUMBER, "210"))
      .put("contactPersonRelationship", "Rel")
      .put("otherContactPerson", "Additional contacts for the project")
      .put("contactPersonOrganisationName", "Org")
      .put("contactPersonBusinessID", "BID"));

    // section4 (used by parent section4Mappings)
    form.put("section4", new JsonObject()
      .put(SAME_AS_CONTACT_PERSON, false)
      .put(FULL_NAME, "Billing Name")
      .put(EMAIL, "bill@example.org")
      .put(PHONE, new JsonObject().put(COUNTRY_CODE, "30").put(NUMBER, "111"))
      .put(ADDRESS, "Billing address")
      .put(INVOICE_TYPE, new JsonObject().put("key", "b").put(VALUE, "Invoice B"))
      .put("invoiceReferenceNumber", "INV-1")
      .put("invoiceAddress", "Invoice address")
      .put(IS_THE_PROJECT_FINANCIALLY_COVERED, new JsonObject().put("key", "a").put(VALUE, "YES"))
      .put("rangeOfAmountOfFinancing", new JsonObject().put(VALUE, "10-20")));

    // SECTION_5 (purpose-of-use dependent blocks)
    form.put(SECTION_5, new JsonObject()
      .put(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, true)
      .put("personResponsibleName", "Responsible")
      .put(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, false)
      .put("personResearchName", "Researcher")
      .put("whyAreTheDataRequested", "Because")
      .put("whatIsTheAimAndTopicOfTheProject", "Aim")
      .put("whichAreTheExpectedBenefits", "Benefits")
      .put("describeApplicantsQualification", "Qualification")
      .put(LEGAL_BASIS, "Legal basis text")
      .put("linkToTheSupportingLegalBasis", "http://legal")
      .put(SUMMARY_OF_PLAN_FOR_USING_THE_DATA, new JsonArray().add(new JsonObject().put(NAME, "Plan1")))
      .put(SUMMARY_OF_RESEARCH_PLAN, new JsonArray().add(new JsonObject().put(NAME, "Res1"))));

    // section6 array (must match catalog id "cat-1")
    form.put("section6", new JsonArray().add(buildSection6Object("cat-1")));

    // section7 + section8
    form.put("section7", new JsonObject()
      .put(ADDITIONAL_INFORMATION, "More info")
      .put(ADDITIONAL_ATTACHMENT, new JsonArray().add(new JsonObject().put("name", "att.txt"))));

    form.put("section8", new JsonObject()
      .put(ACCEPT_HEALTH_DATA_BODY, true)
      .put(AWARE_PROCESSING_FEE, true)
      .put(AWARE_CHARGE_FEE, false)
      .put(AWARE_INFORMATION_CORRECT, true));

    return form;
  }

  private static JsonObject buildForm_naturalPerson_summaryChecked_false() {
    JsonObject form = new JsonObject();

    form.put(SECTION_1, new JsonObject()); // safe (no datasetVariables)

    // summary checkbox FALSE branch
    form.put(SECTION_2, new JsonObject()
      .put("projectName", "Project Y")
      .put(PROJECT_LEADER, "Leader2")
      .put(COUNTRY_OF_PROJECT_LEADER, new JsonObject().put(VALUE, "Greece"))
      .put(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED, new JsonArray().add(new JsonObject().put(VALUE, "e")))
      .put("purposeForWhichDataWillBeUsed", new JsonArray().add(new JsonObject().put("key", "e")))
      .put("summaryOfTheProject", "Short summary")
      .put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, false));

    // SECTION_3 natural person
    form.put(SECTION_3, new JsonObject()
      .put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR, new JsonObject().put("key", "b").put(VALUE, "No"))
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "b").put(VALUE, "Natural"))
      .put("naturalPersonName", "John Natural")
      .put(NATURAL_PERSON_ADDRESS, "Nat addr")
      .put("naturalPersonZipCode", "99999")
      .put("naturalPersonCity", "Nat city")
      .put("naturalPersonCountry", new JsonObject().put(VALUE, "Greece"))
      .put("naturalPersonEmail", "n@example.org")
      .put(NATURAL_PERSON_PHONE, new JsonObject().put(COUNTRY_CODE, "30").put(NUMBER, "222"))
      .put("naturalPersonJobTitle", "Job")
      .put("naturalPersonAffiliation", "Aff"));

    // keep other sections present (minimal) to avoid NPE
    form.put("section4", new JsonObject().put(INVOICE_TYPE, new JsonObject().put("key", "a").put(VALUE, "A")));
    form.put(SECTION_5, new JsonObject());
    form.put("section6", new JsonArray().add(buildSection6Object("cat-1")));
    form.put("section7", new JsonObject());
    form.put("section8", new JsonObject());

    return form;
  }

  private static JsonObject buildSection6Object(String catalogId) {
    // Drives many branches in section61, including tabulationPlanArray
    JsonObject section6 = new JsonObject()
      .put("catalog_ids", new JsonArray().add(catalogId))
      .put("indicateTheSizeOfTheStudyCohort", "1000")
      .put("indicateTheSizeOfTheStudyCohortEstimationOrExact", new JsonObject().put(VALUE, "Exact"))
      .put("whyNeedStudyCohortOfThisSize", "Because reasons")
      .put("variablesToBeUsedInDataExtractionAttachment", new JsonArray().add(new JsonObject().put("name", "var1")))
      .put("timePeriodOfDataExtraction", "2010-2020")
      // extractionMethod key "c" => sampling + sample size blocks
      .put("extractionMethod", new JsonObject().put("key", "c").put(VALUE, "Sampling"))
      .put("samplingMethod", "Random")
      .put(SAMPLE_SIZE, "250")
      .put(INCLUSION_CRITERIA, "Include all")
      .put("potentialExclusionCriteria", "Exclude none")
      // opt-out mechanism key "a" => justification exception
      .put(OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW, new JsonObject().put("key", "a").put(VALUE, "Yes"))
      .put("providedJustificationException", "Justification text")
      // howOften key "b" => NEED_FOR_DATA_EXTRACTION_EVERY block
      .put(HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE, new JsonObject().put("key", "b").put(VALUE, "Multiple"))
      .put(NEED_FOR_DATA_EXTRACTION_EVERY, new JsonObject().put("key", "d").put(VALUE, "Other"))
      .put("needForDataExtractionEveryOther", "Every 3 weeks")
      .put("needForDataExtractionEveryDescription", "Description");

    // Tabulation plan array with one item + nested TABULATION_PLAN object
    JsonArray tabulation = new JsonArray().add(
      new JsonObject()
        .put("tabulationRegisteredToBeUsed", "Reg")
        .put("tabulationPossibleStudyCohort", "Cohort")
        .put("tabulationInformationOfRequiredVariables", "Vars")
        .put("tabulationFormationVariables", "Form")
        .put("tabulationDesiredDirection", "Dir")
        .put("tabulationOrderInWhichTable", "Order")
        .put("tabulationAnyOtherRelevant", "Other")
        .put(TABULATION_PLAN, new JsonObject().put(NAME, "PlanName"))
    );

    // IMPORTANT: set BOTH keys to be safe
    section6.put(TABULATION_PLAN_ARRAY, tabulation);
    section6.put("tabulationPlanArray", tabulation);

    return section6;
  }

  private static JsonArray buildDatasetsValues_forCatalog(String catalogId, String countryLabel) {
    JsonObject dataset = new JsonObject()
      .put(RESULT, new JsonObject()
        .put("id", "ds-1")
        .put("catalog", new JsonObject().put("id", catalogId))
        .put("country", new JsonObject().put("label", countryLabel))
        .put(TITLE, new JsonObject().put("en", "Dataset title"))
        .put(PUBLISHER, new JsonObject().put("name", "Publisher"))
        .put("publisher", new JsonObject().put(NAME, "Publisher")) // section61 uses "publisher"/NAME
        .put(PROVENANCE, new JsonObject().put("en", "Database"))
        .put("hdab", new JsonObject().put("name", "HDAB name"))
        .put("distributions_sample", new JsonArray().add(
          new JsonObject().put(VARIABLES, new JsonArray().add(
            new JsonObject()
              .put("name", "varA")
              .put("datatype", "string")
              .put("description", new JsonObject().put("en", "desc en"))
          ))
        ))
      );

    return new JsonArray().add(dataset);
  }

  private static JsonObject label(String label) {
    return new JsonObject().put(LABEL, label);
  }

  private static JsonObject labelTooltip(String label, String tooltip) {
    return new JsonObject().put(LABEL, label).put(TOOLTIP, tooltip);
  }
}
