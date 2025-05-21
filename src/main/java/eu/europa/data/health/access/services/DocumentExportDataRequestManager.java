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
import static eu.europa.data.health.access.Constants.COUNTRY_OF_PROJECT_LEADER;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.ETHICAL_REVIEW_ATTACHMENT;
import static eu.europa.data.health.access.Constants.ETHICAL_REVIEW_INPUT;
import static eu.europa.data.health.access.Constants.EXTRACTION_DESCRIPTION_AND_COMPILATION;
import static eu.europa.data.health.access.Constants.FULL_NAME;
import static eu.europa.data.health.access.Constants.HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE;
import static eu.europa.data.health.access.Constants.INCLUSION_CRITERIA;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PHONE;
import static eu.europa.data.health.access.Constants.PROVENANCE;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.PURPOSE_OF_USE;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SAMPLE_SIZE;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_PLAN_FOR_USING_THE_DATA;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_RESEARCH_PLAN;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_RESEARCH_PLAN_LANGUAGE;
import static eu.europa.data.health.access.Constants.TABULATION_PLAN;
import static eu.europa.data.health.access.Constants.TABULATION_PLAN_ARRAY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.TOOLTIP;
import static eu.europa.data.health.access.Constants.VALUE;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart;
import org.docx4j.toc.TocGenerator;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentExportDataRequestManager extends DocumentExportUtility {

  private final TranslationManager translationManager;
  private final DBHandler dbHandler;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebClient client;
  private final JsonObject config;

  public DocumentExportDataRequestManager(TranslationManager translationManager,
    DBHandler dbHandler,
    WebClient client, JsonObject config) {
    super(config);
    this.translationManager = translationManager;
    this.dbHandler = dbHandler;
    this.client = client;
    this.config = config;
  }

  public void exportApplicationDataRequestForm(RoutingContext context) {
    String lg = context.pathParam("lg");
    String id = context.pathParam("id");
    String type = context.pathParam("type");

    dbHandler.findById(DATA_REQUEST_APPLICATION_COLLECTION, id).onSuccess(jsonObject -> {
      JsonObject form = jsonObject.getJsonObject("form");
      JsonArray datasets = jsonObject.getJsonArray("datasets");
      getDatasetsValues(context, datasets, lg, form, type);
    }).onFailure(cause -> context.response().setStatusCode(500).end());

  }

  private void getDatasetsValues(RoutingContext context, JsonArray datasets, String lg,
    JsonObject form, String type) {
    HubSearchClient hubSearchClient = new HubSearchClient(client, config);
    JsonArray hubSearchDataSetValues = new JsonArray();
    List<Future<JsonObject>> futures = new ArrayList<>();
    //Collect all futures for the dataset retrievals
    for (int i = 0; i < datasets.size(); i++) {
      String datasetId = datasets.getJsonObject(i).getString("dataset_id");
      Future<JsonObject> future = hubSearchClient.getDataset(datasetId)
        .onSuccess(hubSearchDataSetValues::add)
        .onFailure(cause -> log.info(cause.getMessage()));
      futures.add(future);
    }
    //Wait until all dataset retrievals are completed
    Future.all(futures).onSuccess(result -> {
      if (result.succeeded()) {
        export(context, lg, form, hubSearchDataSetValues, type);
      } else {
        log.error("One or more dataset retrievals failed.");
        context.response().setStatusCode(500).end();
      }
    });
  }

  public void externalExportApplicationForm(RoutingContext context) {
    JsonObject jsonBody = context.body().asJsonObject();
    JsonArray datasets = jsonBody.getJsonArray("datasets");
    JsonObject form = jsonBody.getJsonObject("form");
    getDatasetsValues(context, datasets, "en", form, "docx");
  }

  private void export(RoutingContext context, String lg, JsonObject form, JsonArray datasetsValues,
    String type) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      InputStream templateInputStream = getClass().getResourceAsStream(
        "/templates/DataRequestForm.docx")) {
      if (templateInputStream == null) {
        throw new IllegalArgumentException("Template file not found!");
      }
      WordprocessingMLPackage document = WordprocessingMLPackage.load(templateInputStream);
      JsonObject languageMap = translationManager.getTranslationByLanguish(lg);
      //add values
      addFormData(document, languageMap, form, datasetsValues);

      //generate table of context
      generateTableOfContext(document);

      document.getMainDocumentPart()
        .getDocumentSettingsPart().getJaxbElement()
        .setUpdateFields(new BooleanDefaultTrue());

      FieldUpdater fieldUpdater = new FieldUpdater(document);
      fieldUpdater.update(true);

      // update document settings to avoid field updates MS Word prompt
      // 1) Get the settings part (creates one if absent)
      DocumentSettingsPart dsp = document.getMainDocumentPart().getDocumentSettingsPart();
      // 2) Grab or build its JAXB object
      CTSettings settings = dsp.getJaxbElement();
      if (settings == null) {
        settings = new CTSettings();
      }
      // 3) Insert <w:updateFields w:val="false"/>
      BooleanDefaultTrue b = new BooleanDefaultTrue();
      b.setVal(false);
      settings.setUpdateFields(b);
      // 4) Write it back to document settings
      dsp.setJaxbElement(settings);

      document.save(outputStream);
      if ("pdf".equalsIgnoreCase(type)) {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        // Convert DOCX to PDF logic here
        context.response().putHeader("Content-Disposition", "attachment; filename=Form.pdf")
          .putHeader("Content-Type", "application/pdf")
          .end(Buffer.buffer(pdfOutputStream.toByteArray()));
      } else if ("docx".equalsIgnoreCase(type)) {
        context.response().putHeader("Content-Length", String.valueOf(outputStream.size()))
          .putHeader("Content-Disposition", "attachment; filename=DataRequestForm.docx")
          .putHeader("Content-Type",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
          .end(Buffer.buffer(outputStream.toByteArray()));
      } else {
        context.response().setStatusCode(400).end("Unsupported export type. Use 'pdf' or 'docx'.");
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      context.response().setStatusCode(500).end();
    }
  }

  private void generateTableOfContext(WordprocessingMLPackage document) {
    try {
      TocGenerator tocGenerator = new TocGenerator(document);
      tocGenerator.updateToc(false, true);
    } catch (Exception e) {
      log.debug(e.getMessage());
    }
  }

  private void addFormData(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject form, JsonArray datasetsValues) {
    // Implement mapping logic based on document template structure
    String aboutThisSection = languageMap.getJsonObject("commonFields")
      .getString("aboutThisSection");
    section1Mappings(document, languageMap.getJsonObject("section1"),
      datasetsValues, aboutThisSection);
    section2Mappings(document, languageMap.getJsonObject("section2"),
      form != null ? form.getJsonObject("section2") : null, aboutThisSection);
    section3Mappings(document, languageMap.getJsonObject(SECTION_3),
      form != null ? form.getJsonObject(SECTION_3) : null, aboutThisSection);
    section4Mappings(document, languageMap.getJsonObject("section4"),
      form != null ? form.getJsonObject("section4") : null,
      form != null ? form.getJsonObject(SECTION_3) : null, aboutThisSection);
    section5Mappings(document, languageMap.getJsonObject(SECTION_5),
      form != null ? form.getJsonObject("section2") : null,
      form != null ? form.getJsonObject(SECTION_5) : null, aboutThisSection);
    section6Mappings(document, languageMap.getJsonObject("section6"),
      form != null ? form.getJsonArray("section6") : null,
      form != null ? form.getJsonObject("section2") : null, aboutThisSection, datasetsValues);
    section7Mappings(document, languageMap.getJsonObject("section9"),
      form != null ? form.getJsonObject("section7") : null);
    section8Mappings(document, languageMap.getJsonObject("section10"),
      form != null ? form.getJsonObject("section8") : null, aboutThisSection);

  }

  private void section1Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonArray datasets, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section1h5", "section1Description",
      aboutThisSection);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationDatasets").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, TITLE, "en");
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationCountries").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, "country", LABEL);
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationDataHolders").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, "publisher", NAME);
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationHDABs").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, "hdab", NAME);
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationDatabases").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, PROVENANCE, LABEL);
  }

  private void section2Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section2h5", "section2Description",
      aboutThisSection);

    // --- PROJECT NAME ---
    addTextToNewParagraph(document, languageMap.getJsonObject("projectName").getString(LABEL), true,
      false);
    addTextToNewParagraphWithTable(document, getField(section, "projectName"), false, false);

    // --- PROJECT LEADER ---
    addTextToNewParagraph(document, languageMap.getJsonObject("projectLeader").getString(LABEL),
      true, false);
    addTextToNewParagraph(document, languageMap.getJsonObject("projectLeader").getString(TOOLTIP),
      false, true);
    addTextToNewParagraphWithTable(document, getField(section, "projectLeader"), false, false);

    // --- COUNTRY OF PROJECT LEADER ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(COUNTRY_OF_PROJECT_LEADER).getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getObjectValue(section,COUNTRY_OF_PROJECT_LEADER),
      false, false);

    // --- PURPOSE FOR WHICH DATA WILL BE USED ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED).getString(TOOLTIP), false,
      true);
    generatedJsonArrayValues(document, getArrayField(section, PURPOSE_FOR_WHICH_DATA_WILL_BE_USED),
      VALUE);

    // --- DESCRIPTION OF THE PROJECT ---
    if (isChecked(section, THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)) {
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
          .getString(LABEL),
        true, true);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("descriptionOfTheProject").getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject("descriptionOfTheProject").getString(TOOLTIP), false, true);
      addTextToNewParagraphWithTable(document, getField(section, "descriptionOfTheProject"), false,
        false);
    } else {
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
          .getString(LABEL),
        true, false);
    }

    // --- SUMMARY OF THE PROJECT ---
    if (isChecked(section, THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)) {
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
          .getString(LABEL),
        true, true);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(
          THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON).getString(LABEL),
        true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(
          THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON).getString(TOOLTIP),
        false, true);
      addTextToNewParagraphWithTable(document,
        getField(section, THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON),
        false, false);
    } else {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("summaryOfTheProject").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "summaryOfTheProject"), false,
        false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
          .getString(LABEL),
        true, false);
    }
  }

  private void section3Mappings(WordprocessingMLPackage document, JsonObject
    languageMap, JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section3h5", "section3DescriptionRequest",
      aboutThisSection);

    // --- APPLYING FOR DATA ON BEHALF OF PUBLIC SECTOR ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR).getString(LABEL),
      true, false);
    JsonObject applyingForDataOnBehalf = getObject(section,
      APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    addTextToNewParagraphWithTable(document,
      getObjectValue(section, APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR), false, false);

    if (isKey(applyingForDataOnBehalf, "a")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS).getString(LABEL), true,
        false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS).getString(TOOLTIP),
        false, true);
      addTextToNewParagraphWithTable(document,
        getObjectValue(section, APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS), false, false);
    }

    // --- LEGAL OR NATURAL PERSON ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getObjectValue(section, LEGAL_OR_NATURAL_PERSON),
      false, false);

    JsonObject legalOrNaturalPerson = getObject(section, LEGAL_OR_NATURAL_PERSON);
    if (legalOrNaturalPerson != null) {
      if (isKey(legalOrNaturalPerson, "a")) {
        // --- LEGAL PERSON ---
        addTextToNewParagraph(document, languageMap.getString("legalPerson"), true, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_PERSON_NAME).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "legalPersonName"), false,
          false);

        addTextToNewParagraph(document, languageMap.getString("postalAddress"), true, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("legalPersonAddress").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "legalPersonAddress"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("legalPersonZipCode").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "legalPersonZipCode"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("legalPersonCity").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "legalPersonCity"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_PERSON_COUNTRY).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getObjectValue(section, LEGAL_PERSON_COUNTRY),
          false, false);

        addTextToNewParagraph(document, languageMap.getString("contactPersonInformation"), true,
          false);
        addTextToNewParagraph(document,
          languageMap.getString("contactPersonInformationDescription"), false, true);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonName"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(CONTACT_PERSON_EMAIL).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonEmail"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(CONTACT_PERSON_PHONE).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          buildPhone(getObject(section, CONTACT_PERSON_PHONE)), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonJobTitle"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonAffiliation").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonAffiliation"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonRelationship").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonRelationship"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonNameOfTheOrganisation").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonOrganisationName"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonBusinessIdentifier").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonBusinessID"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("contactPersonOperatorIdentifier").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, getField(section, "contactPersonOperatorID"),
          false, false);
      } else {
        // --- NATURAL PERSON ---
        addTextToNewParagraph(document, languageMap.getString("naturalPerson"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_PERSON_NAME).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "naturalPersonName"), false,
          false);

        addTextToNewParagraph(document, languageMap.getString("postalAddress"), true, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(NATURAL_PERSON_ADDRESS).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, NATURAL_PERSON_ADDRESS), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("naturalPersonZipCode").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "naturalPersonZipCode"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("naturalPersonCity").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "naturalPersonCity"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_PERSON_COUNTRY).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getObjectValue(section, "naturalPersonCountry"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(CONTACT_PERSON_EMAIL).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "naturalPersonEmail"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(CONTACT_PERSON_PHONE).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          buildPhone(getObject(section, NATURAL_PERSON_PHONE)), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("naturalPersonJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "naturalPersonJobTitle"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("naturalPersonAffiliation").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "naturalPersonAffiliation"),
          false, false);
      }
    }
  }

  private void section4Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonObject section3, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section4h5", "section4Description",
      aboutThisSection);

    boolean isDescriptionChecked = isChecked(section, SAME_AS_CONTACT_PERSON);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(SAME_AS_CONTACT_PERSON).getString(LABEL), true,
      isDescriptionChecked);

    JsonObject legalPerson = this.getObject(section3, LEGAL_OR_NATURAL_PERSON);

    if (Objects.nonNull(legalPerson) && Objects.equals(legalPerson.getString("key"), "a")) {
      addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, FULL_NAME), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, EMAIL),
        false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document,
        buildPhone(getObject(section, PHONE)), false, false);

      // --- ORGANIZATION INFORMATION ---
      addTextToNewParagraph(document,
        languageMap.getJsonObject("nameOfTheOrganisation").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "nameOfTheOrganisation"), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, ADDRESS), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("businessIdentifierOrganization").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "businessIdentifierOrganization"),
        false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject("vatNumber").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, "vatNumber"), false, false);
    } else {
      // --- CONTACT INFORMATION ---
      addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, FULL_NAME), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, EMAIL), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, buildPhone(getObject(section, PHONE)), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, ADDRESS), false, false);
    }

    // --- INVOICE INFORMATION ---
    addTextToNewParagraph(document, languageMap.getJsonObject("invoiceType").getString(LABEL), true,
      false);
    JsonObject invoiceType = getObject(section, "invoiceType");
    addTextToNewParagraphWithTable(document, getObjectValue(section, "invoiceType"), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("invoiceReferenceNumber").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "invoiceReferenceNumber"), false,
      false);

    if (isKey(invoiceType, "b")) {
      addTextToNewParagraph(document, languageMap.getJsonObject("invoiceAddress").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, "invoiceAddress"), false, false);
    }

    // --- OPERATOR IDENTIFIER (ONLY FOR LEGAL PERSONS) ---
    if (isKey(legalPerson, "a")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("operatorIdentifier").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "operatorIdentifier"), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject("peppolCode").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, "peppolCode"), false, false);
    }

    // --- FINANCIAL COVERAGE ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("isTheProjectFinanciallyCovered").getString(LABEL), true, false);
    String financiallyCoveredValue = getObjectValue(section, "isTheProjectFinanciallyCovered");
    addTextToNewParagraphWithTable(document, financiallyCoveredValue, false, false);

    if (financiallyCoveredValue != null) {
      JsonObject financiallyCovered = getObject(section, "isTheProjectFinanciallyCovered");
      if (isKey(financiallyCovered, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("rangeOfAmountOfFinancing").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          getObjectValue(section, "rangeOfAmountOfFinancing"), false, false);
      }
    }
  }


  private void section5Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section2, JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section5h5", "section5Description",
      aboutThisSection);

    addTextToNewParagraph(document, languageMap.getJsonObject("purposeOfUse").getString(LABEL),
      true, false);

    JsonArray purposeOfUse = getArrayField(section2, "purposeForWhichDataWillBeUsed");
    generatedJsonArrayValues(document, purposeOfUse, VALUE);

    // --- PURPOSE OF USE AND RESPONSIBLE PERSONS ---
    if (section2 != null) {
      if (purposeOfUse != null && containsSelectedValueWithoutSpecificValue(purposeOfUse, "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleLabel"), true,
          false);

        boolean isPersonResponsibleSameAsContact = isChecked(section,
          PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON).getString(LABEL),
          true, isPersonResponsibleSameAsContact);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResponsibleName"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResponsibleJobTitle"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_AFFILIATION).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, PERSON_RESPONSIBLE_AFFILIATION),
          false, false);
      }

      if (purposeOfUse != null && containsKey(purposeOfUse, "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleForResearchLabel"),
          true, false);

        boolean isPersonResearchSameAsContact = isChecked(section,
          PERSON_RESEARCH_SAME_AS_CONTACT_PERSON);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON).getString(LABEL), true,
          isPersonResearchSameAsContact);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResearchName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResearchName"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResearchJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResearchJobTitle"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_AFFILIATION).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResearchAffiliation"),
          false, false);
      }
    }

    // --- WHY DATA IS REQUESTED, AIM, BENEFITS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("whyAreTheDataRequested").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whyAreTheDataRequested"), false,
      false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whatIsTheAimAndTopicOfTheProject").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whatIsTheAimAndTopicOfTheProject"),
      false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whichAreTheExpectedBenefits").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whichAreTheExpectedBenefits"),
      false, false);

    // --- LEGAL BASIS AND PLANS ---
    if (section2 != null) {
      if (purposeOfUse != null && containsKey(purposeOfUse, "b")) {
        addTextToNewParagraph(document, languageMap.getJsonObject("legalBasis").getString(LABEL),
          true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject("legalBasis").getString(TOOLTIP),
          false, true);
        addTextToNewParagraphWithTable(document, getField(section, "legalBasis"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("linkToTheSupportingLegalBasis").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "linkToTheSupportingLegalBasis"),
          false, false);
      }

      if (purposeOfUse != null && containsSelectedValueWithoutSpecificValue(purposeOfUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA).getString(LABEL), true,
          false);

        JsonArray summaryPlanData = getArrayField(section, SUMMARY_OF_PLAN_FOR_USING_THE_DATA);
        generatedJsonArrayValues(document, summaryPlanData, NAME);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE).getString(LABEL),
          true, false);
        JsonObject summaryPlanLanguage = getObject(section,
          SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE);
        if (summaryPlanLanguage != null) {
          addTextToNewParagraphWithTable(document, summaryPlanLanguage.getString(VALUE, ""), false,
            false);
        }
      }

      if (purposeOfUse != null && containsKey(purposeOfUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN).getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN).getString(TOOLTIP), false, true);

        JsonArray researchPlanDoc = getArrayField(section, SUMMARY_OF_RESEARCH_PLAN);
        generatedJsonArrayValues(document, researchPlanDoc, NAME);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN_LANGUAGE).getString(LABEL), true,
          false);
        JsonObject researchPlanLanguage = getObject(section, SUMMARY_OF_RESEARCH_PLAN_LANGUAGE);
        if (researchPlanLanguage != null) {
          addTextToNewParagraphWithTable(document, researchPlanLanguage.getString(VALUE, ""), false,
            false);
        }
      }
    }
  }

  private void section6Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonArray section6Array, JsonObject section2, String aboutThisSection, JsonArray datasets) {
    addSectionWithHeader(document, languageMap, "section6h5", "section6Description",
      aboutThisSection);

    List<String> catalogIds = extractCatalogIds(datasets);

    for (String catalogId : catalogIds) {

      JsonArray singleCatalogIdArray = new JsonArray();
      singleCatalogIdArray.add(catalogId);

      JsonArray filterDatasets = filterDatasetsByCatalogIds(datasets, singleCatalogIdArray);

      JsonObject section6Object = findSection6ObjectByCatalogId(section6Array, catalogId);

      section60(document, languageMap, section6Object, filterDatasets);
      section61(document, languageMap, section6Object, section2, filterDatasets);
    }

  }

  private void section60(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonArray datasets) {
    addTextToNewParagraph(document,
      datasets.getJsonObject(0).getJsonObject("result").getJsonObject("country")
        .getString("label", null), true, false);
    addHeaderToTheText(document,
      languageMap.getString("descriptionOfTheDataNeeded") + " " + datasets.getJsonObject(0)
        .getJsonObject("result").getJsonObject("country")
        .getString("label", null),
      "Heading2");
    addTextToNewParagraph(document, languageMap.getString("applicationHDABsForSelectedCountry"),
      true, false);
    writeDatasetsValues(document, datasets, "hdab", NAME);
    addTextToNewParagraph(document, languageMap.getString("applicationDatasetsPerCountry"), true,
      false);
    writeDatasetsValues(document, datasets, TITLE, "en");

    // --- HDAB CONTACTS AND DATA LINKAGE ---
    addTextToNewParagraph(document, languageMap.getJsonObject("hdabContacts").getString(LABEL),
      true, false);
    addTextToNewParagraphWithTable(document, getField(section, "hdabContacts"), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("howWillTheDataFromDifferentSourcesBeLinked").getString(LABEL),
      true, false);
    addTextToNewParagraphWithTable(document,
      getField(section, "howWillTheDataFromDifferentSourcesBeLinked"), false, false);
  }

  private void section61(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonObject section2, JsonArray datasets) {
    addHeaderToTheText(document,
      languageMap.getString("descriptionSection6_1") + " " + datasets.getJsonObject(0)
        .getJsonObject("result").getJsonObject("country")
        .getString("label", null),
      "Heading3");

    // --- STUDY COHORT SIZE AND ETHICAL REVIEW ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("indicateTheSizeOfTheStudyCohort").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "indicateTheSizeOfTheStudyCohort"),
      false, false);

    JsonObject cohortSizeEstimation = getObject(section,
      "indicateTheSizeOfTheStudyCohortEstimationOrExact");
    if (cohortSizeEstimation != null) {
      addTextToNewParagraphWithTable(document, cohortSizeEstimation.getString(VALUE, ""), false,
        false);
    }

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whyNeedStudyCohortOfThisSize").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whyNeedStudyCohortOfThisSize"),
      false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject(ETHICAL_REVIEW_INPUT).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(ETHICAL_REVIEW_INPUT).getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getField(section, ETHICAL_REVIEW_INPUT), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject(ETHICAL_REVIEW_ATTACHMENT).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(ETHICAL_REVIEW_ATTACHMENT).getString(TOOLTIP), false, true);

    JsonArray ethicalReviewAttachment = getArrayField(section, ETHICAL_REVIEW_ATTACHMENT);
    generatedJsonArrayValues(document, ethicalReviewAttachment, NAME);

    addTextToNewParagraph(document, languageMap.getString("dataHolders"), true, false);
    writeDatasetsValues(document, datasets, "publisher", NAME);
    addTextToNewParagraph(document, languageMap.getString("whichDatabasesDataExtracted"), true,
      false);
    writeDatasetsValues(document, datasets, PROVENANCE, LABEL);
    addTextToNewParagraph(document, languageMap.getString("whichDatasetDataExtracted"), true,
      false);
    writeDatasetsValues(document, datasets, TITLE, "en");

    // --- DATA EXTRACTION AND SAMPLING METHOD ---
    addTextToNewParagraph(document, languageMap.getJsonObject("variablesToBeUsedInDataExtractionAttachment").getString(LABEL), true, false);
    JsonArray variablesToBeUsed = getArrayField(section, "variablesToBeUsedInDataExtractionAttachment");
    generatedJsonArrayValues(document, variablesToBeUsed, "name");

    addTextToNewParagraph(document, languageMap.getJsonObject("timePeriodOfDataExtraction").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "timePeriodOfDataExtraction"), false, false);

    addTextToNewParagraph(document, languageMap.getJsonObject("extractionMethod").getString(LABEL), true, false);
    JsonObject extractionMethod = getObject(section, "extractionMethod");

    if (extractionMethod != null) {
      addTextToNewParagraphWithTable(document, extractionMethod.getString(VALUE, ""), false, false);
      String key = extractionMethod.getString("key", "");

      if ("c".equals(key)) {
        addTextToNewParagraph(document, languageMap.getJsonObject("samplingMethod").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "samplingMethod"), false, false);
      }

      if ("c".equals(key) || "a".equals(key)) {
        addTextToNewParagraph(document, languageMap.getJsonObject(SAMPLE_SIZE).getString(LABEL), true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject(SAMPLE_SIZE).getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document, getField(section, SAMPLE_SIZE), false, false);
      }
    }

    addTextToNewParagraph(document, languageMap.getJsonObject(INCLUSION_CRITERIA).getString(LABEL), true, false);
    addTextToNewParagraph(document, languageMap.getJsonObject(INCLUSION_CRITERIA).getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getField(section, INCLUSION_CRITERIA), false, false);

    addTextToNewParagraph(document, languageMap.getJsonObject("potentialExclusionCriteria").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "potentialExclusionCriteria"), false, false);

    // --- TABULATION PLAN ---
    addTextToNewParagraph(document, languageMap.getJsonObject(TABULATION_PLAN_ARRAY).getString(LABEL), true, false);
    addTextToNewParagraph(document, languageMap.getJsonObject(TABULATION_PLAN_ARRAY).getString(TOOLTIP), false, true);

    JsonArray tabulationPlanArray = getArrayField(section, TABULATION_PLAN_ARRAY);
    if (tabulationPlanArray != null && !tabulationPlanArray.isEmpty()) {
      for (Object item : tabulationPlanArray) {
        if (item instanceof JsonObject tabulationItem) {
          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationRegisteredToBeUsed").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationRegisteredToBeUsed", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationPossibleStudyCohort").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationPossibleStudyCohort", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationInformationOfRequiredVariables").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationInformationOfRequiredVariables", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationFormationVariables").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationFormationVariables", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationDesiredDirection").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationDesiredDirection", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationOrderInWhichTable").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationOrderInWhichTable", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("tabulationAnyOtherRelevant").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, tabulationItem.getString("tabulationAnyOtherRelevant", ""), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject(TABULATION_PLAN).getString(LABEL), true, false);
          addTextToNewParagraph(document, languageMap.getJsonObject(TABULATION_PLAN).getString(TOOLTIP), false, true);

          JsonObject tabulationPlan = tabulationItem.getJsonObject(TABULATION_PLAN);
          if (tabulationPlan != null) {
            addTextToNewParagraphWithTable(document, tabulationPlan.getString(NAME, ""), false, false);
          }
        }
      }
    }

    // --- EXTRACTION DESCRIPTION AND FREQUENCY ---
    addTextToNewParagraph(document, languageMap.getJsonObject(EXTRACTION_DESCRIPTION_AND_COMPILATION).getString(LABEL), true, false);
    addTextToNewParagraph(document, languageMap.getJsonObject(EXTRACTION_DESCRIPTION_AND_COMPILATION).getString(TOOLTIP), false, true);

    addTextToNewParagraph(document, languageMap.getJsonObject(DATA_EXTRACTION_FREQUENCY).getString(LABEL), true, false);
    JsonObject howOftenExtraction = getObject(section, HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE);
    addTextToNewParagraphWithTable(document, getObjectValue(section, HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE), false, false);

    if (howOftenExtraction != null) {
      if (isKey(howOftenExtraction, "b")) {
        addTextToNewParagraph(document, languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(LABEL), true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document, getObjectValue(section, NEED_FOR_DATA_EXTRACTION_EVERY), false, false);

        JsonObject needExtractionEvery = getObject(section, NEED_FOR_DATA_EXTRACTION_EVERY);
        if (needExtractionEvery != null) {

          if (isKey(needExtractionEvery, "d")) {
            addTextToNewParagraph(document, languageMap.getJsonObject("needForDataExtractionEveryOther").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, getField(section, "needForDataExtractionEveryOther"), false, false);
          }
        }
      }
    }

    addTextToNewParagraph(document, languageMap.getJsonObject("needForDataExtractionEveryDescriptionDR").getString(LABEL), true, false);
    addTextToNewParagraph(document, languageMap.getJsonObject("needForDataExtractionEveryDescriptionDR").getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getField(section, "needForDataExtractionEveryDescription"), false, false);

    // --- FREQUENCY OF UPDATES (IF APPLICABLE) ---
    if (section2 != null) {
      JsonArray purposeOfUse = getArrayField(section2, "purposeForWhichDataWillBeUsed");
      if (purposeOfUse != null && containsKey(purposeOfUse, "b")) {
        addTextToNewParagraph(document, languageMap.getJsonObject("whatIsTheFrequencyOfUpdates").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "whatIsTheFrequencyOfUpdates"), false, false);
      }
    }
  }

  private void section7Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section) {
    addHeaderToTheText(document, languageMap.getString("section9h5"), "Heading1");
    // --- ADDITIONAL INFORMATION ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(ADDITIONAL_INFORMATION).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(ADDITIONAL_INFORMATION).getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getField(section, ADDITIONAL_INFORMATION), false,
      false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject(ADDITIONAL_ATTACHMENT).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(ADDITIONAL_ATTACHMENT).getString(TOOLTIP), false, true);

    JsonArray additionalAttachment = getArrayField(section, ADDITIONAL_ATTACHMENT);
    generatedJsonArrayValues(document, additionalAttachment, "name");

  }

  private void section8Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section10h5", "section10Description",
      aboutThisSection);
    // --- APPLICATION SUBMISSION AND ACCEPTANCE ---
    addTextToNewParagraph(document, languageMap.getString("submitYouApplicationText"), false,
      false);

    boolean acceptHealthDataBody = isChecked(section, ACCEPT_HEALTH_DATA_BODY);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(ACCEPT_HEALTH_DATA_BODY).getString(LABEL), false,
      acceptHealthDataBody);

    boolean awareProcessingFee = isChecked(section, AWARE_PROCESSING_FEE);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(AWARE_PROCESSING_FEE).getString(LABEL), false, awareProcessingFee);

    boolean awareChargeFee = isChecked(section, AWARE_CHARGE_FEE);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(AWARE_CHARGE_FEE).getString(LABEL), false, awareChargeFee);

    boolean awareInformationCorrect = isChecked(section, AWARE_INFORMATION_CORRECT);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(AWARE_INFORMATION_CORRECT).getString(LABEL), false,
      awareInformationCorrect);
  }
}

