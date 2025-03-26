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
import static eu.europa.data.health.access.Constants.AWARE_CHARGE_FEE;
import static eu.europa.data.health.access.Constants.AWARE_INFORMATION_CORRECT;
import static eu.europa.data.health.access.Constants.AWARE_PROCESSING_FEE;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.COUNTRY_CODE;
import static eu.europa.data.health.access.Constants.COUNTRY_ID;
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
import static eu.europa.data.health.access.Constants.NUMBER;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PHONE;
import static eu.europa.data.health.access.Constants.POSTAL_ADDRESS_LABEL;
import static eu.europa.data.health.access.Constants.PROVENANCE;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.PURPOSE_OF_USE;
import static eu.europa.data.health.access.Constants.REGIONS_SEEK_FOR_DATA;
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
import org.docx4j.toc.TocGenerator;
import org.docx4j.wml.BooleanDefaultTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentExportDataRequestManager extends DocumentExportUtility {

  private final TranslationManager translationManager;
  private final DBHandler dbHandler;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebClient client;
  private final JsonObject config;

  public DocumentExportDataRequestManager(TranslationManager translationManager, DBHandler dbHandler,
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
      form != null ? form.getJsonObject(SECTION_5) : null, aboutThisSection);
    section6Mappings(document, languageMap.getJsonObject("section6"),
      form != null ? form.getJsonArray("section6") : null, form != null ? form.getJsonObject(SECTION_5) : null, aboutThisSection, datasetsValues);
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

    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document, languageMap.getJsonObject("projectName").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, section.getString("projectName"), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject("projectLeader").getString(LABEL),
        true, false);
      addTextToNewParagraph(document, languageMap.getJsonObject("projectLeader").getString(TOOLTIP),
        false, true);
      addTextToNewParagraphWithTable(document, section.getString("projectLeader"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(COUNTRY_OF_PROJECT_LEADER).getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document,
        Objects.nonNull(section.getJsonObject(COUNTRY_OF_PROJECT_LEADER)) ?
          section.getJsonObject(COUNTRY_OF_PROJECT_LEADER).getString(VALUE) : null, false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED).getString(TOOLTIP), false,
        true);
      JsonArray purposeForWhichDataWillBeUsedList = section.getJsonArray(
        PURPOSE_FOR_WHICH_DATA_WILL_BE_USED);
      generatedJsonArrayValues(document, purposeForWhichDataWillBeUsedList, VALUE);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("theResearchFocusesOnTheFollowingObjectives").getString(LABEL),
        true, false);
      JsonArray theResearchFocusesOnTheFollowingObjectives = section.getJsonArray(
        "theResearchFocusesOnTheFollowingObjectives");
      generatedJsonArrayValues(document, theResearchFocusesOnTheFollowingObjectives, VALUE);

      if (Objects.nonNull(theResearchFocusesOnTheFollowingObjectives) && containsKey(
        theResearchFocusesOnTheFollowingObjectives, "i")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("theResearchFocusesOnTheFollowingObjectivesOther")
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          section.getString("theResearchFocusesOnTheFollowingObjectivesOther"), false,
          false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("areaOfResearch").getString(LABEL),
        true, false);
      JsonArray areaOfResearch = section.getJsonArray("areaOfResearch");
      generatedJsonArrayValues(document, areaOfResearch, VALUE);

      if (Objects.nonNull(areaOfResearch) && containsKey(areaOfResearch, "aj")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("areaOfResearchOther").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("areaOfResearchOther"), false,
          false);
      }

      boolean isDescriptionChecked =
        section.containsKey(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
          && section.getBoolean(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION, false);

      if (isDescriptionChecked) {
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
            .getString(LABEL), true, true);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("descriptionOfTheProject").getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject("descriptionOfTheProject").getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document, section.getString("descriptionOfTheProject"), false, false);
      } else {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("descriptionOfTheDataYouWillUse").getString(LABEL), true,
          false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject("descriptionOfTheDataYouWillUse").getString(TOOLTIP), false,
          true);
        addTextToNewParagraphWithTable(document,
          section.getString("descriptionOfTheDataYouWillUse"), false, false);

        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
            .getString(LABEL), true, false);
      }

      boolean isSummaryChecked =
        section.containsKey(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
          && section.getBoolean(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, false);

      if (isSummaryChecked) {
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
            .getString(LABEL), true, true);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(
              THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON)
            .getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON)
            .getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document,
          section.getString(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON), false,
          false);
      } else {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("summaryOfTheProject").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("summaryOfTheProject"), false,
          false);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
            .getString(LABEL), true, false);
      }
    }
  }

  private void section3Mappings(WordprocessingMLPackage document, JsonObject
    languageMap, JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section3h5", "section3Description",
      aboutThisSection);

    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("applyingForDataOnBehalfOfPublicSector").getString(LABEL),
        true, false);
      JsonObject applyingForDataOnBehalfOfPublicSector = section.getJsonObject(
        "applyingForDataOnBehalfOfPublicSector");
      addTextToNewParagraphWithTable(document, Objects.nonNull(applyingForDataOnBehalfOfPublicSector)
          ? applyingForDataOnBehalfOfPublicSector.getString(VALUE) : null,
        false, false);

      if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
        applyingForDataOnBehalfOfPublicSector.getString("key"))) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS).getString(LABEL),
          true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS).getString(TOOLTIP),
          false, true);
        JsonObject applyingForDataForCarryingOutTasks = section.getJsonObject(
          APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS);
        if (Objects.nonNull(applyingForDataForCarryingOutTasks)) {
          addTextToNewParagraphWithTable(document, applyingForDataForCarryingOutTasks.getString(VALUE),
            false, false);
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString(TOOLTIP), false, true);
      JsonObject legalPerson = section.getJsonObject(LEGAL_OR_NATURAL_PERSON);

      if (Objects.nonNull(legalPerson)) {
        //if is "a" is legal person else i b natural person
        if (Objects.equals(legalPerson.getString("key"), "a")) {
          addTextToNewParagraph(document, languageMap.getString("legalPerson"), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject(LEGAL_PERSON_NAME)
            .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString(LEGAL_PERSON_NAME), false, false);

          addTextToNewParagraph(document, languageMap.getString("postalAddress"), true, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("legalPersonAddress")
            .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("legalPersonAddress"), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("legalPersonZipCode")
            .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("legalPersonZipCode"), false, false);

          addTextToNewParagraph(document, languageMap.getJsonObject("legalPersonCity")
            .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("legalPersonCity"), false, false);

            addTextToNewParagraph(document, languageMap.getJsonObject(LEGAL_PERSON_COUNTRY)
            .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
            LEGAL_PERSON_COUNTRY)) ? section.getJsonObject(LEGAL_PERSON_COUNTRY)
            .getString(VALUE) : null, false, false);

          addTextToNewParagraph(document, languageMap.getString("contactPersonInformation"),
            true, false);
          addTextToNewParagraph(document,
            languageMap.getString("contactPersonInformationDescription"),
            false, true);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonName").getString(LABEL), true, false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonName").getString(TOOLTIP), false, true);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonName"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonJobTitle").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonJobTitle"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(CONTACT_PERSON_EMAIL).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString(CONTACT_PERSON_EMAIL), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(CONTACT_PERSON_PHONE).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            Objects.nonNull(section.getJsonObject(CONTACT_PERSON_PHONE)) ?
              "+" + section.getJsonObject(CONTACT_PERSON_PHONE).getString(COUNTRY_CODE) + " " +
                section.getJsonObject(CONTACT_PERSON_PHONE).getString(NUMBER) : null, false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonNameOfTheOrganisation").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonOrganisationName"), false,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonBusinessIdentifier").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonBusinessID"), false,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonOperatorIdentifier").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonOperatorID"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonAffiliation").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonAffiliation"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("contactPersonRelationship").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonRelationship"), false,
            false);
        }
        //natural person
        else {
          addTextToNewParagraph(document,
            legalPerson.getString(VALUE) + " " + getTranslateValueForKey(
              languageMap.getJsonObject(LEGAL_OR_NATURAL_PERSON),
              legalPerson.getString("key"), "info"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(LEGAL_PERSON_NAME).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("naturalPersonName"), false, false);
          addTextToNewParagraph(document, languageMap.getString("postalAddress"), true, false);

          addTextToNewParagraph(document, languageMap.getJsonObject(NATURAL_PERSON_ADDRESS)
            .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString(NATURAL_PERSON_ADDRESS), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("naturalPersonZipCode").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("naturalPersonZipCode"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("naturalPersonCity").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("naturalPersonCity"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(LEGAL_PERSON_COUNTRY).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            Objects.nonNull(section.getJsonObject("naturalPersonCountry")) ?
              section.getJsonObject("naturalPersonCountry").getString(VALUE) : null, false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(CONTACT_PERSON_EMAIL).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("naturalPersonEmail"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(CONTACT_PERSON_PHONE).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            Objects.nonNull(section.getJsonObject(NATURAL_PERSON_PHONE)) ? "+" +
              section.getJsonObject(NATURAL_PERSON_PHONE).getString(COUNTRY_CODE) + " " +
              section.getJsonObject(NATURAL_PERSON_PHONE).getString(NUMBER) : null, false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("naturalPersonJobTitle").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("naturalPersonJobTitle"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("naturalPersonAffiliation").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("naturalPersonAffiliation"), false,
            false);
        }
      }
    }
  }

  private void section4Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonObject section3, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section4h5", "section4Description",
      aboutThisSection);

    if (Objects.nonNull(section)) {
      boolean isDescriptionChecked =
        section.containsKey(SAME_AS_CONTACT_PERSON) && section.getBoolean(SAME_AS_CONTACT_PERSON,
          false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(SAME_AS_CONTACT_PERSON).getString(LABEL), true,
        isDescriptionChecked);

      if (isDescriptionChecked) {
        JsonObject legalPerson = Objects.nonNull(section3) ? section3.getJsonObject(
          LEGAL_OR_NATURAL_PERSON) : null;
        if (Objects.nonNull(legalPerson)) {
          if (Objects.equals(legalPerson.getString("key"), "a")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject(FULL_NAME).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, section3.getString(LEGAL_PERSON_NAME), false, false);
            addTextToNewParagraph(document, languageMap.getString(POSTAL_ADDRESS_LABEL), true,
              false);

            addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, section.getString(ADDRESS), false, false);

            addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              "+" +
                section3.getJsonObject(CONTACT_PERSON_PHONE).getString(COUNTRY_CODE) + " " +
                section3.getJsonObject(CONTACT_PERSON_PHONE).getString(NUMBER),
              false, false);

            addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, section3.getString(CONTACT_PERSON_EMAIL), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("operatorIdentifier").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, section.getString("operatorIdentifier"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("nameOfTheOrganisation").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, section.getString("nameOfTheOrganisation"), false,
              false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("businessIdentifierOrganization").getString(LABEL), true,
              false);
            addTextToNewParagraphWithTable(document, section.getString("businessIdentifierOrganization"),
              false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("vatNumber").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, section.getString("vatNumber"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("peppolCode").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, section.getString("peppolCode"), false, false);
          } else {
            addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, section3.getString("naturalPersonName"), false, false);

            addTextToNewParagraph(document, languageMap.getString(POSTAL_ADDRESS_LABEL), true,
              false);
            addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, section3.getString(NATURAL_PERSON_ADDRESS), false,
              false);

            addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              Objects.nonNull(section.getJsonObject(PHONE)) ? "+" +
                section.getJsonObject(PHONE).getString(COUNTRY_CODE) + " " +
                section.getJsonObject(PHONE).getString(NUMBER) : null, false, false);

            addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, section3.getString("naturalPersonEmail"), false, false);
          }
        }
      }
      else {
        addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document, section.getString(FULL_NAME), false, false);

        addTextToNewParagraph(document, languageMap.getString(POSTAL_ADDRESS_LABEL), true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document, section.getString(ADDRESS), false, false);

        addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document,
          Objects.nonNull(section.getJsonObject(PHONE)) ? "+" +
            section.getJsonObject(PHONE).getString(COUNTRY_CODE) + " " +
            section.getJsonObject(PHONE).getString(NUMBER) : null, false, false);

        addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString(EMAIL), false, false);
      }
      addTextToNewParagraph(document, languageMap.getJsonObject("invoiceType").getString(LABEL),
        true, false);
      JsonObject invoiceType = section.getJsonObject("invoiceType");
      addTextToNewParagraphWithTable(document, Objects.nonNull(invoiceType) ?
        invoiceType.getString(VALUE) : null, false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("invoiceReferenceNumber").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("invoiceReferenceNumber"), false, false);

      if (Objects.nonNull(invoiceType) && Objects.equals(invoiceType.getString("key"), "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("invoiceAddress").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("invoiceAddress"), false, false);
      }

      JsonObject legalPerson = Objects.nonNull(section3) ? section3.getJsonObject(
        LEGAL_OR_NATURAL_PERSON) : null;
      if (Objects.nonNull(legalPerson)) {
        if (Objects.equals(legalPerson.getString("key"), "a")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("operatorIdentifier").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("operatorIdentifier"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("nameOfTheOrganisation").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("nameOfTheOrganisation"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("businessIdentifierOrganization").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("businessIdentifierOrganization"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("vatNumber").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("vatNumber"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("peppolCode").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("peppolCode"), false, false);
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("isTheProjectFinanciallyCovered").getString(LABEL), true,
        false);
      JsonObject financiallyCovered = section.getJsonObject("isTheProjectFinanciallyCovered");
      if (Objects.nonNull(financiallyCovered)) {
        addTextToNewParagraphWithTable(document, financiallyCovered.getString(VALUE), false, false);
        JsonObject rangeOfAmountOfFinancing = section.getJsonObject("rangeOfAmountOfFinancing");
        if ("a".equals(financiallyCovered.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("rangeOfAmountOfFinancing").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            Objects.nonNull(rangeOfAmountOfFinancing) ? rangeOfAmountOfFinancing.getString(VALUE)
              : null, false, false);
        }
      }
    }
  }

  private void section5Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section5h5", "section5Description",
      aboutThisSection);

    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document, languageMap.getJsonObject(PURPOSE_OF_USE).getString(LABEL),
        true, false);

      JsonArray purposeOfUse = section.getJsonArray(PURPOSE_OF_USE);
      generatedJsonArrayValues(document, purposeOfUse, VALUE);

      if (Objects.nonNull(purposeOfUse) && containsSelectedValueWithoutSpecificValue(purposeOfUse, "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleLabel"),
          true, false);
        boolean isPersonResponsibleSameAsContactPerson =
          section.containsKey(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON) && section.getBoolean(
            PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, false);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON).getString(LABEL),
          true, isPersonResponsibleSameAsContactPerson);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResponsibleName"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResponsibleJobTitle"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_AFFILIATION).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString(PERSON_RESPONSIBLE_AFFILIATION),
          false, false);


      }

      if (Objects.nonNull(purposeOfUse) && containsKey(purposeOfUse, "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleForResearchLabel"),
          true, false);

        boolean isPersonResearchSameAsContactPerson =
          section.containsKey(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON) && section.getBoolean(
            PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, false);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON).getString(LABEL), true,
          isPersonResearchSameAsContactPerson);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResearchName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResearchName"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResearchJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResearchJobTitle"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_AFFILIATION).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("personResearchAffiliation"),
          false, false);
      }


    addTextToNewParagraph(document,
      languageMap.getJsonObject("whyAreTheDataRequested").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, section.getString("whyAreTheDataRequested"), false,
      false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whatIsTheAimAndTopicOfTheProject").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, section.getString("whatIsTheAimAndTopicOfTheProject"),
      false, false);


      if (Objects.nonNull(purposeOfUse) && containsKey(purposeOfUse, "b")){
        addTextToNewParagraph(document,
          languageMap.getJsonObject("legalBasis").getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject("legalBasis").getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document, section.getString("legalBasis"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("linkToTheSupportingLegalBasis").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("linkToTheSupportingLegalBasis"), false, false);
      }

      if (Objects.nonNull(purposeOfUse) && containsSelectedValueWithoutSpecificValue(purposeOfUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA).getString(LABEL), true,
          false);

        JsonArray summaryOfPlanForUsingTheDataDoc = section.getJsonArray(
          SUMMARY_OF_PLAN_FOR_USING_THE_DATA);
        generatedJsonArrayValues(document, summaryOfPlanForUsingTheDataDoc, NAME);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE).getString(LABEL),
          true, false);
        JsonObject summaryOfPlanForUsingTheDataLanguage = section.getJsonObject(
          SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE);
        if (Objects.nonNull(summaryOfPlanForUsingTheDataLanguage)) {
          addTextToNewParagraphWithTable(document,
            summaryOfPlanForUsingTheDataLanguage.getString(VALUE),
            false, false);
        }
      }

      if (Objects.nonNull(purposeOfUse) && containsKey(purposeOfUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN).getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN).getString(TOOLTIP), false,
          true);
        JsonArray summaryOfResearchPlanDoc = section.getJsonArray(SUMMARY_OF_RESEARCH_PLAN);
        generatedJsonArrayValues(document, summaryOfResearchPlanDoc, NAME);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN_LANGUAGE).getString(LABEL), true,
          false);
        JsonObject summaryOfResearchPlanLanguage = section.getJsonObject(
          SUMMARY_OF_RESEARCH_PLAN_LANGUAGE);
        if (Objects.nonNull(summaryOfResearchPlanLanguage)) {
          addTextToNewParagraphWithTable(document, summaryOfResearchPlanLanguage.getString(VALUE),
            false, false);
        }
      }
    }

  }

  private void section6Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonArray section6Array,JsonObject section5, String aboutThisSection, JsonArray datasets) {
    addSectionWithHeader(document, languageMap, "section6h5", "section6Description",
      aboutThisSection);

    if (Objects.nonNull(section6Array) && !section6Array.isEmpty()) {
      for (int i = 0; i < section6Array.size(); i++) {
        JsonArray catalogIds = section6Array.getJsonObject(i).getJsonArray("catalog_ids");
        JsonArray filterDatasets = filterDatasetsByCatalogIds(datasets, catalogIds);
        section60(document, languageMap, section6Array.getJsonObject(i), filterDatasets);
        section61(document, languageMap, section6Array.getJsonObject(i), section5, filterDatasets);
      }
    }
  }

  private void section60(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonArray datasets) {
    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document, section.getString(COUNTRY_ID), true, false);
      addHeaderToTheText(document,
        languageMap.getString("descriptionOfTheDataNeeded") + " " + section.getString(COUNTRY_ID),
        "Heading2");
      addTextToNewParagraph(document, languageMap.getString("applicationHDABsForSelectedCountry"),
        true, false);
      writeDatasetsValues(document, datasets, "hdab", NAME);
      addTextToNewParagraph(document, languageMap.getString("applicationDatasetsPerCountry"), true,
        false);
      writeDatasetsValues(document, datasets, TITLE, "en");
      addTextToNewParagraph(document, languageMap.getJsonObject("hdabContacts").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, section.getString("hdabContacts"), false, false);
    }
  }

  private void section61(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,JsonObject section5, JsonArray datasets) {
    if (Objects.nonNull(section)) {
      addHeaderToTheText(document,
        languageMap.getString("descriptionSection6_1") + " " + section.getString(COUNTRY_ID),
        "Heading3");

      addTextToNewParagraph(document,
        languageMap.getJsonObject("indicateTheSizeOfTheStudyCohort").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("indicateTheSizeOfTheStudyCohort"), false,
        false);

      JsonObject sizeOfTheStudyCohortEstimationOrExact = section.getJsonObject(
        "indicateTheSizeOfTheStudyCohortEstimationOrExact");
      if (Objects.nonNull(sizeOfTheStudyCohortEstimationOrExact)) {
        addTextToNewParagraphWithTable(document, sizeOfTheStudyCohortEstimationOrExact.getString(VALUE),
          false, false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("whyNeedStudyCohortOfThisSize").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("whyNeedStudyCohortOfThisSize"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(REGIONS_SEEK_FOR_DATA).getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString(REGIONS_SEEK_FOR_DATA), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(ETHICAL_REVIEW_INPUT).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ETHICAL_REVIEW_INPUT).getString(TOOLTIP), false, true);
      addTextToNewParagraphWithTable(document, section.getString(ETHICAL_REVIEW_INPUT), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(ETHICAL_REVIEW_ATTACHMENT).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ETHICAL_REVIEW_ATTACHMENT).getString(TOOLTIP), false, true);
      JsonArray additionalAttachment = section.getJsonArray(ETHICAL_REVIEW_ATTACHMENT);
      generatedJsonArrayValues(document, additionalAttachment, NAME);

      addTextToNewParagraph(document, languageMap.getString("dataHolders"), true, false);
      writeDatasetsValues(document, datasets, "publisher", NAME);
      addTextToNewParagraph(document, languageMap.getString("whichDatabasesDataExtracted"), true,
        false);
      writeDatasetsValues(document, datasets, PROVENANCE, LABEL);
      addTextToNewParagraph(document, languageMap.getString("whichDatasetDataExtracted"), true,
        false);
      writeDatasetsValues(document, datasets, TITLE, "en");

      addTextToNewParagraph(document,
        languageMap.getJsonObject("variablesToBeUsedInDataExtraction").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("variablesToBeUsedInDataExtraction"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("timePeriodOfDataExtraction").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("timePeriodOfDataExtraction"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("extractionMethod").getString(LABEL), true, false);
      JsonObject extractionMethod = section.getJsonObject("extractionMethod");
      if (Objects.nonNull(extractionMethod)) {
        addTextToNewParagraphWithTable(document, extractionMethod.getString(VALUE), false, false);
        String key2 = extractionMethod.getString("key");

        if (key2.equals("c")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("samplingMethod").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("samplingMethod"), false, false);
        }

        if (key2.equals("c") || key2.equals("a")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(SAMPLE_SIZE).getString(LABEL), true, false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(SAMPLE_SIZE).getString(TOOLTIP), false, true);
          addTextToNewParagraphWithTable(document, section.getString(SAMPLE_SIZE), false, false);
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject(INCLUSION_CRITERIA).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(INCLUSION_CRITERIA).getString(TOOLTIP), false, true);
      addTextToNewParagraphWithTable(document, section.getString(INCLUSION_CRITERIA), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("potentialExclusionCriteria").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("potentialExclusionCriteria"), false,
        false);

      JsonArray tabulationPlanArray = section.getJsonArray(
        TABULATION_PLAN_ARRAY);
      if (!tabulationPlanArray.isEmpty()) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(TABULATION_PLAN_ARRAY).getString(LABEL), true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject(TABULATION_PLAN_ARRAY).getString(TOOLTIP), false,
          true);

        for (Object item : tabulationPlanArray) {
          if (item instanceof JsonObject jsonItem) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationRegisteredToBeUsed").getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationRegisteredToBeUsed"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationPossibleStudyCohort").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationPossibleStudyCohort"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationInformationOfRequiredVariables").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationInformationOfRequiredVariables"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationFormationVariables").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationFormationVariables"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationDesiredDirection").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationDesiredDirection"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationOrderInWhichTable").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationOrderInWhichTable"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("tabulationAnyOtherRelevant").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("tabulationAnyOtherRelevant"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject(TABULATION_PLAN).getString(LABEL), true, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject(TABULATION_PLAN).getString(TOOLTIP), false, true);
            JsonObject tabulationPlan = jsonItem.getJsonObject(
              TABULATION_PLAN);
            if(Objects.nonNull(tabulationPlan)) {
              addTextToNewParagraphWithTable(document, tabulationPlan.getString(NAME), false,
                false);
            }
          }
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject(EXTRACTION_DESCRIPTION_AND_COMPILATION).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(EXTRACTION_DESCRIPTION_AND_COMPILATION).getString(TOOLTIP), false, true);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(DATA_EXTRACTION_FREQUENCY).getString(LABEL), true, false);
      JsonObject howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple = section.getJsonObject(
        HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE);
      if (Objects.nonNull(howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple)) {
        addTextToNewParagraphWithTable(document, howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple
            .getString(VALUE), false, false);
        if (Objects.equals(
          howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"), "b")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(LABEL), true,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(TOOLTIP), false,
            true);
          JsonObject needForDataExtractionEvery = section.getJsonObject(
            NEED_FOR_DATA_EXTRACTION_EVERY);

          if (Objects.nonNull(needForDataExtractionEvery)) {
            addTextToNewParagraphWithTable(document, needForDataExtractionEvery.getString(VALUE),
              false, false);
            if (Objects.equals(needForDataExtractionEvery.getString("key"), "d")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("needForDataExtractionEveryOther").getString(LABEL),
                true, false);
              addTextToNewParagraphWithTable(document,
                section.getString("needForDataExtractionEveryOther"),
                false, false);
            }
          }
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("needForDataExtractionEveryDescriptionDR").getString(LABEL), true,
        false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject("needForDataExtractionEveryDescriptionDR").getString(TOOLTIP), false,
        true);
      addTextToNewParagraphWithTable(document, section.getString("needForDataExtractionEveryDescription"),
        false, false);
      if(Objects.nonNull(section5)) {
        JsonArray purposeOfUse = section5.getJsonArray(PURPOSE_OF_USE);
        if ((Objects.nonNull(purposeOfUse) && containsKey(
          purposeOfUse, "b"))) {
          addTextToNewParagraph(document, languageMap.getJsonObject("whatIsTheFrequencyOfUpdates")
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("whatIsTheFrequencyOfUpdates"),
            false, false);
        }
      }
    }
  }

  private void section7Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section) {
    addHeaderToTheText(document, languageMap.getString("section9h5"), "Heading1");
    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_INFORMATION).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_INFORMATION).getString(TOOLTIP), false, true);
      addTextToNewParagraphWithTable(document, section.getString(
          ADDITIONAL_INFORMATION), false,
        false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_ATTACHMENT).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_ATTACHMENT).getString(TOOLTIP), false, true);
      JsonArray additionalAttachment = section.getJsonArray(
        ADDITIONAL_ATTACHMENT);
      generatedJsonArrayValues(document, additionalAttachment, NAME);
    }
  }

  private void section8Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section10h5", "section10Description",
      aboutThisSection);
    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document, languageMap.getString("submitYouApplicationText"), false,
        false);
      boolean acceptHealthDataBody =
        section.containsKey(ACCEPT_HEALTH_DATA_BODY) && section.getBoolean(ACCEPT_HEALTH_DATA_BODY,
          false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(ACCEPT_HEALTH_DATA_BODY).getString(LABEL), false,
        acceptHealthDataBody);
      boolean awareProcessingFee =
        section.containsKey(AWARE_PROCESSING_FEE) && section.getBoolean(AWARE_PROCESSING_FEE,
          false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(AWARE_PROCESSING_FEE).getString(LABEL), false,
        awareProcessingFee);
      boolean awareChargeFee =
        section.containsKey(AWARE_CHARGE_FEE) && section.getBoolean(AWARE_CHARGE_FEE, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(AWARE_CHARGE_FEE).getString(LABEL), false, awareChargeFee);
      boolean awareInformationCorrect =
        section.containsKey(AWARE_INFORMATION_CORRECT) && section.getBoolean(
          AWARE_INFORMATION_CORRECT, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(AWARE_INFORMATION_CORRECT).getString(LABEL), false,
        awareInformationCorrect);
    }
  }

}
