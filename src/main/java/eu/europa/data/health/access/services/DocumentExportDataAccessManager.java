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

import static eu.europa.data.health.access.Constants.ADDITIONAL_ATTACHMENT;
import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION;
import static eu.europa.data.health.access.Constants.ADDRESS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.AWARE_CHARGE_FEE;
import static eu.europa.data.health.access.Constants.AWARE_INFORMATION_CORRECT;
import static eu.europa.data.health.access.Constants.AWARE_PROCESSING_FEE;
import static eu.europa.data.health.access.Constants.COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE;
import static eu.europa.data.health.access.Constants.COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER;
import static eu.europa.data.health.access.Constants.CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.COUNTRY_CODE;
import static eu.europa.data.health.access.Constants.COUNTRY_ID;
import static eu.europa.data.health.access.Constants.COUNTRY_OF_PROJECT_LEADER;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES;
import static eu.europa.data.health.access.Constants.DETAILS_OF_HOW_THE_STUDY_HAS_BEEN_FORMED;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA;
import static eu.europa.data.health.access.Constants.FULL_NAME;
import static eu.europa.data.health.access.Constants.HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS;
import static eu.europa.data.health.access.Constants.HEADING_3;
import static eu.europa.data.health.access.Constants.INCLUSION_CRITERIA;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.MORE_INFO_ON_EXTRACTION_PERIODS;
import static eu.europa.data.health.access.Constants.MORE_INFO_ON_EXTRACTION_PERIODS_RELATIVES;
import static eu.europa.data.health.access.Constants.NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.NUMBER;
import static eu.europa.data.health.access.Constants.ORDER_OF_CONTROLS_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.ORDER_OF_RELATIVES_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.OTHER_PERMIT_APPLICATIONS;
import static eu.europa.data.health.access.Constants.PERMIT_DECISION_ATTACHMENT;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PHONE;
import static eu.europa.data.health.access.Constants.POSTAL_ADDRESS_LABEL;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_A;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_B;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_C;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_D;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_E;
import static eu.europa.data.health.access.Constants.PROVENANCE;
import static eu.europa.data.health.access.Constants.PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS;
import static eu.europa.data.health.access.Constants.PUBLISHER;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SAMPLE_SIZE;
import static eu.europa.data.health.access.Constants.SECTION_1;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_7;
import static eu.europa.data.health.access.Constants.SECTION_8;
import static eu.europa.data.health.access.Constants.SECTION_9;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_PLAN_FOR_USING_THE_DATA;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_RESEARCH_PLAN;
import static eu.europa.data.health.access.Constants.SUMMARY_OF_RESEARCH_PLAN_LANGUAGE;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.TOOLTIP;
import static eu.europa.data.health.access.Constants.VALUE;
import static eu.europa.data.health.access.Constants.WHEN_DATA_NEEDED;
import static eu.europa.data.health.access.Constants.WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA;
import static eu.europa.data.health.access.Constants.WHY_NEED_DATA_OF_A_WHOLE_POPULATION;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49;
import static eu.europa.data.health.access.Constants.WILL_CONTROLS_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_RELATIVES_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES;
import static eu.europa.data.health.access.Constants.WILL_THE_DATA_BE_TRANSFERRED;

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

public class DocumentExportDataAccessManager extends DocumentExportUtility {



  private final TranslationManager translationManager;
  private final DBHandler dbHandler;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebClient client;
  private final JsonObject config;

  public DocumentExportDataAccessManager(TranslationManager translationManager, DBHandler dbHandler,
    WebClient client, JsonObject config) {
    super(config);
    this.translationManager = translationManager;
    this.dbHandler = dbHandler;
    this.client = client;
    this.config = config;
  }

  public void exportApplicationDataAccessForm(RoutingContext context) {
    String lg = context.pathParam("lg");
    String id = context.pathParam("id");
    String type = context.pathParam("type");

    dbHandler.findById(DATA_ACCESS_APPLICATION_COLLECTION, id).onSuccess(jsonObject -> {
      JsonObject form = jsonObject.getJsonObject("form");
      JsonArray datasets = jsonObject.getJsonArray("datasets");
      getDatasetsValues(context, datasets, lg, form, type);
    }).onFailure(cause -> context.response().setStatusCode(500).end());

  }

  public void externalExportApplicationForm(RoutingContext context) {
    JsonObject jsonBody = context.body().asJsonObject();
    JsonArray datasets = jsonBody.getJsonArray("datasets");
    JsonObject form = jsonBody.getJsonObject("form");
    getDatasetsValues(context, datasets, "en", form, "docx");
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

  private void export(RoutingContext context, String lg, JsonObject form, JsonArray datasetsValues,
    String type) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      InputStream templateInputStream = getClass().getResourceAsStream(
        "/templates/DataAccessForm.docx")) {
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
          .putHeader("Content-Disposition", "attachment; filename=DataAccessForm.docx")
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
    section1Mappings(document, languageMap.getJsonObject(SECTION_1),
      datasetsValues, aboutThisSection);
    section2Mappings(document, languageMap.getJsonObject(SECTION_2),
      form != null ? form.getJsonObject(SECTION_2) : null, aboutThisSection);
    section3Mappings(document, languageMap.getJsonObject(SECTION_3),
      form != null ? form.getJsonObject(SECTION_3) : null, aboutThisSection);
    section4Mappings(document, languageMap.getJsonObject(SECTION_4),
      form != null ? form.getJsonObject(SECTION_4) : null,
      form != null ? form.getJsonObject(SECTION_3) : null, aboutThisSection);
    section5Mappings(document, languageMap.getJsonObject(SECTION_5),
      form != null ? form.getJsonObject(SECTION_5) : null, aboutThisSection);
    section6Mappings(document, languageMap.getJsonObject(SECTION_6),
      form != null ? form.getJsonArray(SECTION_6) : null, aboutThisSection, datasetsValues);
    section7Mappings(document, languageMap.getJsonObject(SECTION_7),
      form != null ? form.getJsonObject(SECTION_7) : null, aboutThisSection);
    section8Mappings(document, languageMap.getJsonObject(SECTION_8),
      form != null ? form.getJsonObject(SECTION_8) : null,
      form != null ? form.getJsonObject(SECTION_2) : null,
      form != null ? form.getJsonObject(SECTION_3) : null,
      form != null ? form.getJsonObject(SECTION_5) : null,
      form != null ? form.getJsonObject(SECTION_7) : null, aboutThisSection);
    section9Mappings(document, languageMap.getJsonObject(SECTION_9),
      form != null ? form.getJsonObject(SECTION_9) : null);
    section10Mappings(document, languageMap.getJsonObject("section10"),
      form != null ? form.getJsonObject("section10") : null, aboutThisSection);
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
    writeDatasetsValues(document, datasets, PUBLISHER, "name");
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationHDABs").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, "hdab", "name");
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
        languageMap.getJsonObject(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR).getString(LABEL),
        true, false);
      JsonObject applyingForDataOnBehalfOfPublicSector = section.getJsonObject(
        APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
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
          addTextToNewParagraphWithTable(document, section.getString("contactPersonName"), false, false);

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
            languageMap.getJsonObject("contactPersonJobTitle").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("contactPersonJobTitle"), false, false);

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
      addTextToNewParagraph(document, languageMap.getJsonObject("purposeOfUse").getString(LABEL),
        true, false);

      JsonArray purposeOfUse = section.getJsonArray("purposeOfUse");
      generatedJsonArrayValues(document, purposeOfUse, VALUE);

      if (Objects.nonNull(purposeOfUse) && containsSelectedValueWithoutSpecificValue(purposeOfUse,
        "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleLabel"), true,
          false);

        boolean isPersonResponsibleSameAsContactPerson =
          section.containsKey(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON) && section.getBoolean(
            PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, false);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON).getString(LABEL),
          true, isPersonResponsibleSameAsContactPerson);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResponsibleName"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResponsibleJobTitle"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_AFFILIATION).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString(PERSON_RESPONSIBLE_AFFILIATION), false,
          false);
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
        addTextToNewParagraphWithTable(document, section.getString("personResearchName"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResearchJobTitle").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("personResearchJobTitle"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_AFFILIATION).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("personResearchAffiliation"), false,
          false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("whyTheDataIsNeeded").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("whyTheDataIsNeeded"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("whatIsTheAimAndTopicOfTheProject").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("whatIsTheAimAndTopicOfTheProject"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("whichAreTheExpectedBenefits").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("whichAreTheExpectedBenefits"), false,
        false);

      if (Objects.nonNull(purposeOfUse) && containsKey(purposeOfUse, "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("legalBasis").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("legalBasis"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("linkToTheSupportingLegalBasis").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("linkToTheSupportingLegalBasis"), false,
          false);
      }

      if (Objects.nonNull(purposeOfUse) && containsSelectedValueWithoutSpecificValue(purposeOfUse,
        "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA).getString(LABEL), true,
          false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA).getString(TOOLTIP), false,
          true);
        JsonArray summaryOfPlanForUsingTheDataDoc = section.getJsonArray(
          SUMMARY_OF_PLAN_FOR_USING_THE_DATA);
        generatedJsonArrayValues(document, summaryOfPlanForUsingTheDataDoc, "name");

        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE).getString(LABEL),
          true, false);
        JsonObject summaryOfPlanForUsingTheDataLanguage = section.getJsonObject(
          SUMMARY_OF_PLAN_FOR_USING_THE_DATA_LANGUAGE);
        if (Objects.nonNull(summaryOfPlanForUsingTheDataLanguage)) {
          addTextToNewParagraphWithTable(document, summaryOfPlanForUsingTheDataLanguage.getString(VALUE),
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
        generatedJsonArrayValues(document, summaryOfResearchPlanDoc, "name");

        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN_LANGUAGE).getString(LABEL), true,
          false);
        JsonObject summaryOfResearchPlanLanguage = section.getJsonObject(
          SUMMARY_OF_RESEARCH_PLAN_LANGUAGE);
        if (Objects.nonNull(summaryOfResearchPlanLanguage)) {
          addTextToNewParagraphWithTable(document, summaryOfResearchPlanLanguage.getString(VALUE), false,
            false);
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA).getString(LABEL), true,
        false);
      JsonObject formatOfTheElectronicHealthData = section.getJsonObject(
        FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);

      if (Objects.nonNull(formatOfTheElectronicHealthData)) {
        addTextToNewParagraphWithTable(document, formatOfTheElectronicHealthData.getString(VALUE), false,
          false);
        if (Objects.equals(formatOfTheElectronicHealthData.getString("key"), "b")) {
          addTextToNewParagraph(document, languageMap.getString("inOrderToApplyPseudonymisedData"),
            true, false);

          boolean isInOrderToApplyPseudonymisedDataA =
            section.containsKey(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A) && section.getBoolean(
              IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A, false);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A).getString(LABEL),
            true, isInOrderToApplyPseudonymisedDataA);

          boolean isInOrderToApplyPseudonymisedDataB =
            section.containsKey(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B) && section.getBoolean(
              IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B, false);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B).getString(LABEL),
            true, isInOrderToApplyPseudonymisedDataB);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("whyNeedForPseudonymisation").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("whyNeedForPseudonymisation"), false,
            false);
        }
      }
    }
  }

  private void section6Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonArray section6Array, String aboutThisSection, JsonArray datasets) {
    addSectionWithHeader(document, languageMap, "section6h5", "section6Description",
      aboutThisSection);

    if (Objects.nonNull(section6Array) && !section6Array.isEmpty()) {
      for (int i = 0; i < section6Array.size(); i++) {
        JsonArray catalogIds = section6Array.getJsonObject(i).getJsonArray("catalog_ids");
        JsonArray filterDatasets = filterDatasetsByCatalogIds(datasets, catalogIds);
        section60(document, languageMap, section6Array.getJsonObject(i), filterDatasets);
        section61(document, languageMap, section6Array.getJsonObject(i), filterDatasets);
        section62(document, languageMap, section6Array.getJsonObject(i), filterDatasets);
        section63(document, languageMap, section6Array.getJsonObject(i), filterDatasets);
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
      writeDatasetsValues(document, datasets, "hdab", "name");
      addTextToNewParagraph(document, languageMap.getString("applicationDatasetsPerCountry"), true,
        false);
      writeDatasetsValues(document, datasets, TITLE, "en");
      addTextToNewParagraph(document, languageMap.getJsonObject("hdabContacts").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, section.getString("hdabContacts"), false, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject("howWillTheDataFromDifferentSourcesBeLinked").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document,
        section.getString("howWillTheDataFromDifferentSourcesBeLinked"), false, false);
    }
  }

  private void section61(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,
    JsonArray datasets) {
    if (Objects.nonNull(section)) {
      addHeaderToTheText(document,
        languageMap.getString("descriptionSection6_1") + " " + section.getString(COUNTRY_ID),
        HEADING_3);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("howIsTheStudyCohortFormed").getString(LABEL), true, false);
      JsonObject howIsTheStudyCohortFormed = section.getJsonObject("howIsTheStudyCohortFormed");


      if (Objects.nonNull(howIsTheStudyCohortFormed)) {
        addTextToNewParagraphWithTable(document, howIsTheStudyCohortFormed.getString(VALUE), false, false);

        if ((Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b")
          || Objects.equals(howIsTheStudyCohortFormed.getString("key"), "c"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(
                DETAILS_OF_HOW_THE_STUDY_HAS_BEEN_FORMED)
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            section.getString(DETAILS_OF_HOW_THE_STUDY_HAS_BEEN_FORMED),
            false, false);
        }

        if (Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b")) {
          addTextToNewParagraphWithTable(document,
            section.getString("detailsOnHowAndWhatLegalDocumentsTheStudyCohortHasBeenFormed"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(
                HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS)
              .getString(LABEL), true, false);
          JsonObject hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants = section.getJsonObject(
            HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
          addTextToNewParagraphWithTable(document,
            Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
              ? hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString(VALUE)
              : null,
            false, false);
        }
      }

      JsonObject hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants = section.getJsonObject(
        HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
      if (Objects.nonNull(howIsTheStudyCohortFormed) && Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "b") && (
        Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
          && Objects.equals(
          hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString("key"),
          "a"))) {
        addTextToNewParagraph(document, languageMap.getJsonObject(
              "doesTheInformedConsentCoverTheRequestedRegistryExtractions")
            .getString(LABEL), true, false);
        JsonObject doesTheInformedConsentCoverTheRequestedRegistryExtractions = section.getJsonObject(
          "doesTheInformedConsentCoverTheRequestedRegistryExtractions");
        if (Objects.nonNull(doesTheInformedConsentCoverTheRequestedRegistryExtractions)) {
          addTextToNewParagraphWithTable(document,
            doesTheInformedConsentCoverTheRequestedRegistryExtractions.getString(VALUE), false,
            false);
        }

        addTextToNewParagraph(document,
          languageMap.getJsonObject(
              "consentAndInformationLetterAttachment")
            .getString(LABEL), true, false);
        JsonArray consentAndInformationLetterAttachment = section.getJsonArray(
          "consentAndInformationLetterAttachment");
        generatedJsonArrayValues(document, consentAndInformationLetterAttachment, "name");

        boolean confirmThatDataPermitHasBeenGrantedForTheResearchProject =
          section.containsKey(CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT)
            && section.getBoolean(
            CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT, false);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(
              CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT)
            .getString(LABEL), false,
          confirmThatDataPermitHasBeenGrantedForTheResearchProject);

        addTextToNewParagraph(document, languageMap.getJsonObject("idOfTheDataPermit")
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("idOfTheDataPermit"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERMIT_DECISION_ATTACHMENT).getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(PERMIT_DECISION_ATTACHMENT).getString(TOOLTIP), false, true);
        JsonArray permitDecisionAttachment = section.getJsonArray(
          PERMIT_DECISION_ATTACHMENT);
        generatedJsonArrayValues(document, permitDecisionAttachment, "name");
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "b") &&
        (Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
          && Objects.equals(
          hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString("key"),
          "b"))) {
        addTextToNewParagraph(document, languageMap.getJsonObject("howTheStudyCohortWasObtained")
          .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          section.getString("howTheStudyCohortWasObtained"), false, false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS)
          .getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS)
          .getString(TOOLTIP), false, true);
      JsonObject providedInformationOfTheDataUseToCorrespondingSubjects = section.getJsonObject(
        PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS);

      if (Objects.nonNull(providedInformationOfTheDataUseToCorrespondingSubjects)) {
        addTextToNewParagraphWithTable(document,
          providedInformationOfTheDataUseToCorrespondingSubjects.getString(VALUE), false, false);
        if (Objects.equals(providedInformationOfTheDataUseToCorrespondingSubjects.getString("key"),
          "a")) {addTextToNewParagraph(document,
            languageMap.getJsonObject("howProvidedInformationOfTheDataUseToCorrespondingSubjects")
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            section.getString("howProvidedInformationOfTheDataUseToCorrespondingSubjects"), false,
            false);
        } else {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(
                "whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects")
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            section.getString("whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects"),
            false, false);
        }
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && (
        Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") || Objects.equals(
          howIsTheStudyCohortFormed.getString("key"), "c"))) {
        addTextToNewParagraph(document, languageMap.getJsonObject(
              "whoWillDeliverTheInformationOnTheStudy")
            .getString(LABEL), true, false);

        boolean informationProviderSameAsContactPerson =
          section.containsKey(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON) && section.getBoolean(
            INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, false);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON).getString(LABEL),
          false, informationProviderSameAsContactPerson);

        addTextToNewParagraph(document, languageMap.getJsonObject("informationProviderName")
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("informationProviderName"),
          false, false);

        addTextToNewParagraph(document, languageMap.getJsonObject(
              "informationProviderEmail").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("informationProviderEmail"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(INFORMATION_PROVIDER_PHONE).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          Objects.nonNull(section.getJsonObject(INFORMATION_PROVIDER_PHONE)) ?
            "+" +
              section.getJsonObject(INFORMATION_PROVIDER_PHONE).getString(COUNTRY_CODE) + " " +
              section.getJsonObject(INFORMATION_PROVIDER_PHONE).getString(NUMBER) : null,
          false, false);
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "d")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(
              WHY_NEED_DATA_OF_A_WHOLE_POPULATION)
            .getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(
              WHY_NEED_DATA_OF_A_WHOLE_POPULATION)
            .getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document,
          section.getString(WHY_NEED_DATA_OF_A_WHOLE_POPULATION),
          false, false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("sizeOfTheStudyCohort").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("sizeOfTheStudyCohort"), false, false);

      JsonObject sizeOfTheStudyCohortEstimationOrExact = section.getJsonObject(
        "sizeOfTheStudyCohortEstimationOrExact");
      if (Objects.nonNull(sizeOfTheStudyCohortEstimationOrExact)) {
        addTextToNewParagraphWithTable(document, sizeOfTheStudyCohortEstimationOrExact.getString(VALUE),
          false, false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("whyNeedStudyCohortOfThisSize").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("whyNeedStudyCohortOfThisSize"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("regionsSeekForData").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("regionsSeekForData"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("ethicalReviewAttachment").getString(LABEL), true, false);
      JsonArray ethicalReviewAttachment = section.getJsonArray("ethicalReviewAttachment");
      generatedJsonArrayValues(document, ethicalReviewAttachment, "name");

      addTextToNewParagraph(document, languageMap.getString("dataHolders"), true, false);
      writeDatasetsValues(document, datasets, PUBLISHER, "name");
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

      addTextToNewParagraph(document, languageMap.getString("dataExtraction"), true, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("howOftenDoesTheDataNeedToBeExtracted").getString(LABEL), true,
        false);
      JsonObject howOftenDoesTheDataNeedToBeExtracted = section.getJsonObject(
        "howOftenDoesTheDataNeedToBeExtracted");

      if (Objects.nonNull(howOftenDoesTheDataNeedToBeExtracted)) {
        addTextToNewParagraphWithTable(document, howOftenDoesTheDataNeedToBeExtracted.getString(VALUE),
          false, false);
        if (Objects.equals(howOftenDoesTheDataNeedToBeExtracted.getString("key"), "b")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(LABEL), true,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(TOOLTIP), false,
            true);
          JsonObject needForDataExtractionEvery = section.getJsonObject(
            NEED_FOR_DATA_EXTRACTION_EVERY);

          if (Objects.nonNull(needForDataExtractionEvery)) {
            addTextToNewParagraphWithTable(document, needForDataExtractionEvery.getString(VALUE), false,
              false);
            if (Objects.equals(needForDataExtractionEvery.getString("key"), "d")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("needForDataExtractionEveryOther").getString(LABEL),
                true, false);
              addTextToNewParagraphWithTable(document, section.getString("needForDataExtractionEveryOther"),
                false, false);
            }
          }
        }
      }
      addTextToNewParagraph(document,
        languageMap.getJsonObject("needForDataExtractionEveryDescription").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("needForDataExtractionEveryDescription"),
        false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("orderForDataExtraction").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, section.getString("orderForDataExtraction"), false, false);
    }
  }

  private void section62(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,
    JsonArray datasets) {
    if (Objects.nonNull(section)) {
      addHeaderToTheText(document,
        languageMap.getString("definingExtractionCriteria") + " " + section.getString(COUNTRY_ID),
        HEADING_3);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_CONTROLS_BE_EXTRACTED).getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document,
        Objects.nonNull(section.getJsonObject(WILL_CONTROLS_BE_EXTRACTED)) ? section.getJsonObject(
          WILL_CONTROLS_BE_EXTRACTED).getString(VALUE) : null, false, false);

      if (Objects.nonNull(section.getJsonObject(WILL_CONTROLS_BE_EXTRACTED))
        && Objects.equals(section.getJsonObject(WILL_CONTROLS_BE_EXTRACTED).getString("key"),
        "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS).getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
            WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS)) ?
            section.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS).getString(VALUE) : null,
          false, false);
        if (Objects.nonNull(section.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS))
          && Objects.equals(
          section.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS).getString("key"),
          "b")) {

          addTextToNewParagraph(document,
            languageMap.getJsonObject("regionForControlsDataExtraction").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("regionForControlsDataExtraction"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getString("fromWhichDataHolderLabel"), true, false);
          writeDatasetsValues(document, datasets, PUBLISHER, "name");
          addTextToNewParagraph(document,
            languageMap.getString("fromWhichDatabaseLabel"), true, false);
          writeDatasetsValues(document, datasets, PROVENANCE, LABEL);
          addTextToNewParagraph(document,
            languageMap.getString("fromWhichDatasetLabel"), true, false);
          writeDatasetsValues(document, datasets, TITLE, "en");

          addTextToNewParagraph(document,
            languageMap.getJsonObject("variablesForDataExtraction").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("variablesForDataExtraction"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("timePeriodForDataExtraction").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("timePeriodForDataExtraction"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("extractionCriteriaForControls").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("extractionCriteriaForControls"), false,
            false);
        }

        addTextToNewParagraph(document,
          languageMap.getJsonObject("sizeOfControlGroup").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("sizeOfControlGroup"), false, false);

        addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
            "sizeOfControlGroupEstimationOrExact")) ?
            section.getJsonObject("sizeOfControlGroupEstimationOrExact").getString(VALUE) : null,
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("controlsPerPersonInStudyCohort").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("controlsPerPersonInStudyCohort"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("inclusionCriteriaForControls").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("inclusionCriteriaForControls"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("exclusionCriteriaForControls").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("exclusionCriteriaForControls"), false,
          false);

        addTextToNewParagraph(document, languageMap.getString("ifTheGroupHasBeenFormed"), true,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("previouslyIssuedPermitIssuer").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("previouslyIssuedPermitIssuer"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("previouslyIssuedPermitDate").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, section.getString("previouslyIssuedPermitDate"), false,
          false);

        addTextToNewParagraph(document, languageMap.getString("validityPeriod"), true, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodFrom").getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document,
          section.getString("previouslyIssuedPermitValidityPeriodFrom"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodTo").getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document, section.getString("previouslyIssuedPermitValidityPeriodTo"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("previouslyIssuedPermitNumber").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document, section.getString("previouslyIssuedPermitNumber"), false,
          false);

        addTextToNewParagraph(document, languageMap.getString("dataExtractionAndOrderForControls"),
          true, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY)
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
          WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY)) ?
          section.getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY).getString(VALUE)
          : null, false, false);

        if (Objects.nonNull(section
          .getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY)) && Objects.equals(
            section.getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY)
              .getString("key"), "b")) {

          addTextToNewParagraph(document,
            languageMap.getJsonObject(DATA_EXTRACTION_FREQUENCY).getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            Objects.nonNull(section.getJsonObject(DATA_EXTRACTION_FREQUENCY)) ?
              section.getJsonObject(DATA_EXTRACTION_FREQUENCY).getString(VALUE) : null, false,
            false);

          if (Objects.nonNull(section.getJsonObject(
            DATA_EXTRACTION_FREQUENCY)) && Objects.equals(section.getJsonObject(
            DATA_EXTRACTION_FREQUENCY).getString("key"), "b")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("dataExtractionFrequencyMultipleTimesInformation")
                .getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              section.getString("dataExtractionFrequencyMultipleTimesInformation"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY)
                .getString(LABEL), true, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY)
                .getString(TOOLTIP), false, true);
            JsonObject dataNeedsToBeExtractedEvery = section.getJsonObject(
              DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
            if (Objects.nonNull(dataNeedsToBeExtractedEvery)) {
              addTextToNewParagraphWithTable(document, dataNeedsToBeExtractedEvery.getString(VALUE), false,
                false);
              if (Objects.equals(dataNeedsToBeExtractedEvery.getString("key"), "d")) {
                addTextToNewParagraph(document,
                  languageMap.getJsonObject("specifyOther").getString(LABEL), true, false);
                addTextToNewParagraphWithTable(document, section.getString("specifyOther"), false, false);
              }
            }
          }
          addTextToNewParagraph(document,
            languageMap.getJsonObject(MORE_INFO_ON_EXTRACTION_PERIODS).getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString(MORE_INFO_ON_EXTRACTION_PERIODS), false,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(ORDER_OF_CONTROLS_DATA_EXTRACTION).getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString(ORDER_OF_CONTROLS_DATA_EXTRACTION),
            false, false);
        }
      }
    }
  }

  private void section63(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,
    JsonArray datasets) {
    if (Objects.nonNull(section)) {
      addHeaderToTheText(document,
        languageMap.getString("definingTheExtractionCriteria") + " " + section.getString(
          COUNTRY_ID), HEADING_3);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_RELATIVES_BE_EXTRACTED).getString(LABEL), true, false);
      JsonObject willRelativesBeExtracted = section.getJsonObject(WILL_RELATIVES_BE_EXTRACTED);
      if (Objects.nonNull(willRelativesBeExtracted)) {
        addTextToNewParagraphWithTable(document, willRelativesBeExtracted.getString(VALUE), false, false);
        if (Objects.equals(section.getJsonObject(WILL_RELATIVES_BE_EXTRACTED).getString("key"),
          "a")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES).getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
              WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES)) ?
              section.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES).getString(VALUE)
              : null, false, false);
          if (Objects.nonNull(section.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES))
            && Objects.equals(
            section.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES).getString("key"),
            "b")) {
            addTextToNewParagraph(document, languageMap.getString("whichDataHoldersForRelatives"),
              true, false);
            writeDatasetsValues(document, datasets, PUBLISHER, "name");
            addTextToNewParagraph(document, languageMap.getString("whichDatabaseForRelatives"),
              true, false);
            writeDatasetsValues(document, datasets, PROVENANCE, LABEL);
            addTextToNewParagraph(document, languageMap.getString("whichDatasetsForRelatives"),
              true, false);
            writeDatasetsValues(document, datasets, TITLE, "en");
            addTextToNewParagraph(document,
              languageMap.getJsonObject("variablesForDataExtractionRelatives").getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              section.getString("variablesForDataExtractionRelatives"),
              false, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("timePeriodForDataExtractionRelatives").getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              section.getString("timePeriodForDataExtractionRelatives"),
              false, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("relationshipToStudyCohort").getString(LABEL), true,
              false);
            addTextToNewParagraphWithTable(document, section.getString("relationshipToStudyCohort"), false,
              false);
          }

          addTextToNewParagraph(document,
            languageMap.getJsonObject("previouslyIssuedPermitIssuerRelatives").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document,
            section.getString("previouslyIssuedPermitIssuerRelatives"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("sizeOfRelativesGroup").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("sizeOfRelativesGroup"), false, false);
          addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
              "sizeOfRelativesGroupEstimateOrExact")) ?
              section.getJsonObject("sizeOfRelativesGroupEstimateOrExact").getString(VALUE) : null,
            false, false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY)
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
              WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY)) ?
              section.getJsonObject(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY)
                .getString(VALUE) : null, false, false);
          if (
            Objects.nonNull(
              section.getJsonObject(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY))
              && Objects.equals(
              section.getJsonObject(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY)
                .getString("key"), "b")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject(DATA_EXTRACTION_FREQUENCY_RELATIVES).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
                DATA_EXTRACTION_FREQUENCY_RELATIVES)) ?
                section.getJsonObject(DATA_EXTRACTION_FREQUENCY_RELATIVES).getString(VALUE) : null,
              false, false);
            if (Objects.nonNull(section.getJsonObject(DATA_EXTRACTION_FREQUENCY_RELATIVES))
              && Objects.equals(
              section.getJsonObject(DATA_EXTRACTION_FREQUENCY_RELATIVES).getString("key"), "b")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("dataExtractionFrequencyMultipleTimesInformationRelatives")
                  .getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                section.getString("dataExtractionFrequencyMultipleTimesInformationRelatives"),
                false, false);
              addTextToNewParagraph(document,
                languageMap.getJsonObject("needForDataExtractionEveryOptions").getString(LABEL),
                true, false);
              addTextToNewParagraph(document,
                languageMap.getJsonObject("needForDataExtractionEveryOptions").getString(TOOLTIP),
                false, true);
              addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
                  DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES)) ?
                  section.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES).getString(VALUE)
                  : null, false, false);
              if (Objects.nonNull(section.getJsonObject(
                DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES)) && Objects.equals(
                section.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES).getString("key"),
                "d")) {
                addTextToNewParagraph(document,
                  languageMap.getJsonObject("specifyOtherRelatives").getString(LABEL), true,
                  false);
                addTextToNewParagraphWithTable(document, section.getString("specifyOtherRelatives"), false,
                  false);
              }
            }
          }
          addTextToNewParagraph(document,
            languageMap.getJsonObject(MORE_INFO_ON_EXTRACTION_PERIODS_RELATIVES).getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, section.getString(MORE_INFO_ON_EXTRACTION_PERIODS_RELATIVES),
            false, false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(ORDER_OF_RELATIVES_DATA_EXTRACTION).getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString(ORDER_OF_RELATIVES_DATA_EXTRACTION),
            false, false);
        }
      }
    }
  }

  private void section7Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section7h5", "section7Description",
      aboutThisSection);

    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED).getString(LABEL), true,
        false);

      JsonObject willDataBeCombinedWithDataObtained = section.getJsonObject(
        WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);
      if (Objects.nonNull(willDataBeCombinedWithDataObtained)) {
        addTextToNewParagraphWithTable(document, willDataBeCombinedWithDataObtained.getString(VALUE),
          false, false);
        if (Objects.equals(willDataBeCombinedWithDataObtained.getString("key"), "a")) {
          addTextToNewParagraph(document, languageMap.getString("listOtherDataToBeCompined"), true,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject("combinedCountries").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("combinedCountries"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("combinedDataHolders").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("combinedDataHolders"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("combinedDatabaseRegistries").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("combinedDatabaseRegistries"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("combinedDatasetsRegistries").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("combinedDatasetsRegistries"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("providedInformationOnDataCombined").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("providedInformationOnDataCombined"),
            false, false);
        }
      }
      addTextToNewParagraph(document, languageMap.getString("listAnyOtherDataPermit"), false,
        false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject("otherPermits").getString(LABEL), true, false);
      JsonArray otherDataPermits = section.getJsonArray("otherDataPermits");
      if (Objects.nonNull(otherDataPermits) && !otherDataPermits.isEmpty()) {
        for (Object item : otherDataPermits) {
          if (item instanceof JsonObject otherDataPermit) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitIssuer").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, otherDataPermit.getString("permitIssuer"), false,
              false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitStartDateOfIssue").getString(LABEL), true,
              false);
            addTextToNewParagraphWithTable(document, otherDataPermit.getString("permitStartDateOfIssue"),
              false, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitEndDateOfIssue").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, otherDataPermit.getString("permitEndDateOfIssue"),
              false, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitIdentificationInformation").getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              otherDataPermit.getString("permitIdentificationInformation"), false, false);
          }
        }
      }
      addTextToNewParagraph(document,
        languageMap.getJsonObject("attachmentOfPermitDocuments").getString(LABEL), true, false);
      JsonArray attachmentOfPermitDocuments = section.getJsonArray("attachmentOfPermitDocuments");
      generatedJsonArrayValues(document, attachmentOfPermitDocuments, "name");

      addTextToNewParagraph(document,
        languageMap.getJsonObject(OTHER_PERMIT_APPLICATIONS).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(OTHER_PERMIT_APPLICATIONS).getString(TOOLTIP), false, true);
      JsonObject otherPermitApplications = section.getJsonObject(OTHER_PERMIT_APPLICATIONS);
      if (Objects.nonNull(otherPermitApplications)) {
        addTextToNewParagraphWithTable(document, otherPermitApplications.getString(VALUE), false, false);
        if (Objects.equals(otherPermitApplications.getString("key"), "a")) {
          addTextToNewParagraph(document, languageMap.getString("listTheOtherData"), true,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject("dateOfSubmittingTheApplication").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("dateOfSubmittingTheApplication"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("applicationIssuer").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("applicationIssuer"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("identificationCode").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("identificationCode"), false, false);
        }
      }
    }
  }

  private void section8Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonObject section2, JsonObject section3, JsonObject section5,
    JsonObject section7, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section8h5", "section8Description",
      aboutThisSection);
    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("technicalRequirementsForEnvironment").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("technicalRequirementsForEnvironment"),
        false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("nameWebsiteAddressForEnvironment").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("nameWebsiteAddressForEnvironment"), false,
        false);

      addTextToNewParagraph(document, languageMap.getString("healthDataAccessBody"), false, false);
      addTextToNewParagraphWithTable(document, section.getString("section8Description"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(WHEN_DATA_NEEDED).getString(LABEL),
        true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WHEN_DATA_NEEDED).getString(TOOLTIP), false, true);
      JsonObject whenDataNeeded = section.getJsonObject(WHEN_DATA_NEEDED);
      if (Objects.nonNull(whenDataNeeded)) {
        addTextToNewParagraphWithTable(document, whenDataNeeded.getString(VALUE), false, false);
        if (Objects.equals(whenDataNeeded.getString("key"), "b")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("ifLaterWhen").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, section.getString("ifLaterWhen"), false, false);
        }
      }

      JsonObject applyingForDataOnBehalfOfPublicSector =
        Objects.nonNull(section3) ? section3.getJsonObject(
          APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR) : null;
      if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector)) {
        if ("a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("informationWhichDataCanAccess").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("informationWhichDataCanAccess"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("frequencyOfAccessOrDataUpdates").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document, section.getString("frequencyOfAccessOrDataUpdates"),
            false, false);

        } else if ("b".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("estimatedStartAndEnd").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, section.getString("estimatedStartAndEnd"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("estimatedStartDatesForDataProcessing").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, section.getString("estimatedStartDatesForDataProcessing"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("estimatedEndDatesForDataProcessing").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, section.getString("estimatedEndDatesForDataProcessing"),
            false, false);
        }
      }

      addTextToNewParagraph(document, languageMap.getString("ifStoreNeeded"), true, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("startPeriodOfInactiveDataStorage").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("startPeriodOfInactiveDataStorage"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("endPeriodOfInactiveDataStorage").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, section.getString("endPeriodOfInactiveDataStorage"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED).getString(TOOLTIP), false, true);

      JsonObject willTheDataBeTransferred = section.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED);
      //if yes
      if (Objects.nonNull(willTheDataBeTransferred)) {
        addTextToNewParagraphWithTable(document, willTheDataBeTransferred.getString(VALUE), false, false);
        if (Objects.equals(willTheDataBeTransferred.getString("key"), "a")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("whichCountriesDataBeProcessed").getString(LABEL), true,
            false);
          JsonArray whichCountriesDataBeProcessed = section.getJsonArray(
            "whichCountriesDataBeProcessed");
          generatedJsonArrayValues(document, whichCountriesDataBeProcessed, VALUE);
          addTextToNewParagraph(document,
            languageMap.getJsonObject("whyWillDataBeTransferredOutsideEU").getString(LABEL), true,
            false);
          //article 47
          boolean isWhyWillDataBeTransferredOutsideEUArticle47 =
            section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47)
              && section.getBoolean(
              WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47, false);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47)
              .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle47);

          if (isWhyWillDataBeTransferredOutsideEUArticle47) {
            JsonArray whyWillDataBeTransferredOutsideEUArticle47Options = section.getJsonArray(
              "whyWillDataBeTransferredOutsideEUArticle47Options");
            generatedJsonArrayValues(document, whyWillDataBeTransferredOutsideEUArticle47Options,
              VALUE);
          }

          //article 48
          boolean isWhyWillDataBeTransferredOutsideEUArticle48 =
            section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48)
              && section.getBoolean(
              WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, false);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48)
              .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle48);

          if (isWhyWillDataBeTransferredOutsideEUArticle48) {
            boolean isWhyWillDataBeTransferredOutsideEUArticle48a =
              section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A)
                && section.getBoolean(
                WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A, false);
            addTextWithCheckboxToNewParagraph(document,
              languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A)
                .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle48a);

            boolean isWhyWillDataBeTransferredOutsideEUArticle48b =
              section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
                && section.getBoolean(
                WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false);
            addTextWithCheckboxToNewParagraph(document,
              languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
                .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle48b);
            if (isWhyWillDataBeTransferredOutsideEUArticle48b) {
              JsonArray whyWillDataBeTransferredOutsideEUArticle48bOptions = section.getJsonArray(
                "whyWillDataBeTransferredOutsideEUArticle48bOptions");
              generatedJsonArrayValues(document, whyWillDataBeTransferredOutsideEUArticle48bOptions,
                VALUE);

              boolean isWhyWillDataBeTransferredOutsideEUArticle48c =
                section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
                  && section.getBoolean(
                  WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false);
              addTextWithCheckboxToNewParagraph(document,
                languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
                  .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle48c);
              if (isWhyWillDataBeTransferredOutsideEUArticle48c) {
                addTextToNewParagraphWithTable(document, Objects.nonNull(section.getJsonObject(
                  "whyWillDataBeTransferredOutsideEUArticle48cOpt")) ?
                  section.getJsonObject("whyWillDataBeTransferredOutsideEUArticle48cOpt")
                    .getString(VALUE) : null, false, false);
              }

              boolean isWhyWillDataBeTransferredOutsideEUArticle48d =
                section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D)
                  && section.getBoolean(
                  WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D, false);
              addTextWithCheckboxToNewParagraph(document,
                languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D)
                  .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle48d);

              boolean isWhyWillDataBeTransferredOutsideEUArticle48e =
                section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E)
                  && section.getBoolean(
                  WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E, false);
              addTextWithCheckboxToNewParagraph(document,
                languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E)
                  .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle48e);
            }
          }

          //article 9
          boolean isWhyWillDataBeTransferredOutsideEUArticle49 =
            section.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49)
              && section.getBoolean(
              WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49, false);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49)
              .getString(LABEL), true, isWhyWillDataBeTransferredOutsideEUArticle49);

          JsonObject legalBasisForTransferringTheDataOutsideEU = section.getJsonObject(
            LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);
          if (Objects.nonNull(legalBasisForTransferringTheDataOutsideEU)) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU)
                .getString(LABEL), true, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU)
                .getString(TOOLTIP), false, true);
            addTextToNewParagraphWithTable(document,
              legalBasisForTransferringTheDataOutsideEU.getString(VALUE), false, false);
            if (Objects.equals(legalBasisForTransferringTheDataOutsideEU.getString("key"), "c")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("legalBasisForTransferringTheDataOutsideEUOtherOptions")
                  .getString(LABEL), true, false);
              JsonArray legalBasisForTransferringTheDataOutsideEUOtherOptions = section.getJsonArray(
                "legalBasisForTransferringTheDataOutsideEUOtherOptions");
              generatedJsonArrayValues(document,
                legalBasisForTransferringTheDataOutsideEUOtherOptions, VALUE);
            }
            if (Objects.equals(legalBasisForTransferringTheDataOutsideEU.getString("key"), "b")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("safeguardsAreProvidedByReferringGDCP")
                  .getString(LABEL),
                true, false);
              JsonArray safeguardsAreProvidedByReferringGDCP = section.getJsonArray(
                "safeguardsAreProvidedByReferringGDCP");
              generatedJsonArrayValues(document, safeguardsAreProvidedByReferringGDCP, VALUE);
            }
          }
        }
      }
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA)
          .getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA)
          .getString(TOOLTIP), false, true);
      addTextToNewParagraphWithTable(document,
        section.getString(WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA),
        false, false);

      if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector)) {
        if ("b".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE)
              .getString(LABEL), true, false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE)
              .getString(TOOLTIP), false, true);
          addTextToNewParagraphWithTable(document,
            section.getString(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE), false, false);
        } else if ("a".equals(applyingForDataOnBehalfOfPublicSector.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER)
              .getString(LABEL), true, false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER)
              .getString(TOOLTIP), false, true);
          addTextToNewParagraphWithTable(document,
            section.getString(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER), false, false);
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("protectionAndSecurityStatements").getString(LABEL), true,
        false);
      boolean protectionAndSecurityStatementsA =
        section.containsKey(PROTECTION_AND_SECURITY_STATEMENTS_A) && section.getBoolean(
          PROTECTION_AND_SECURITY_STATEMENTS_A, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_A).getString(LABEL), false,
        protectionAndSecurityStatementsA);
      boolean protectionAndSecurityStatementsB =
        section.containsKey(PROTECTION_AND_SECURITY_STATEMENTS_B) && section.getBoolean(
          PROTECTION_AND_SECURITY_STATEMENTS_B, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_B).getString(LABEL), false,
        protectionAndSecurityStatementsB);
      boolean protectionAndSecurityStatementsC =
        section.containsKey(PROTECTION_AND_SECURITY_STATEMENTS_C) && section.getBoolean(
          PROTECTION_AND_SECURITY_STATEMENTS_C, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_C).getString(LABEL), false,
        protectionAndSecurityStatementsC);
      boolean protectionAndSecurityStatementsD =
        section.containsKey(PROTECTION_AND_SECURITY_STATEMENTS_D) && section.getBoolean(
          PROTECTION_AND_SECURITY_STATEMENTS_D, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_D).getString(LABEL), false,
        protectionAndSecurityStatementsD);
      boolean protectionAndSecurityStatementsE =
        section.containsKey(PROTECTION_AND_SECURITY_STATEMENTS_E) && section.getBoolean(
          PROTECTION_AND_SECURITY_STATEMENTS_E, false);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_E).getString(LABEL), false,
        protectionAndSecurityStatementsE);

      JsonArray peopleWhoWillBeProcessingTheData = section.getJsonArray(
        "peopleWhoWillBeProcessingTheData");
      if (Objects.nonNull(peopleWhoWillBeProcessingTheData) && !peopleWhoWillBeProcessingTheData.isEmpty()) {addTextToNewParagraph(document, languageMap.getString("onlyThePeopleMentioned"), false,
          false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject("listTheFullNames").getString(LABEL), true, false);

        for (Object item : peopleWhoWillBeProcessingTheData) {
          if (item instanceof JsonObject jsonItem) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject(FULL_NAME).getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString(FULL_NAME), false, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("affiliation").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("affiliation"), false, false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("emailAddress").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, jsonItem.getString("emailAddress"), false, false);
          }
        }
      }

      if (Boolean.TRUE.equals(this.displayLawfulnessOfProcessing(section, section3, section5, section7))
        || Boolean.TRUE.equals(displayLawfulForProcessingPersonalData(section3, section5))) {
        addTextToNewParagraph(document, languageMap.getString("lawfulnessOfProcessing"), false,
          false);

        if (Boolean.TRUE.equals(displayLawfulForProcessingPersonalData(section3, section5))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("lawfulForProcessingPersonalData").getString(LABEL), true,
            false);
          JsonArray lawfulForProcessingPersonalData = section.getJsonArray(
            "lawfulForProcessingPersonalData");
          generatedJsonArrayValues(document, lawfulForProcessingPersonalData, VALUE);
        }
        if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA).getString(LABEL),
            true, false);
          JsonArray legalBasisForProcessingPersonalData = section.getJsonArray(
            LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
          generatedJsonArrayValues(document, legalBasisForProcessingPersonalData, VALUE);
        }

        JsonArray legalBasisForProcessingPersonalData = section.getJsonArray(
          LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
        if (Objects.nonNull(legalBasisForProcessingPersonalData) && containsKey(
          legalBasisForProcessingPersonalData, "c")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("otherLegalBasisForProcessingPersonalData")
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            section.getString("otherLegalBasisForProcessingPersonalData"), false, false);
        }

        JsonObject willDataBeCombinedWithDataObtained = section7.getJsonObject(
          WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);
        if ((Objects.nonNull(willDataBeCombinedWithDataObtained) && "a".equals(
          willDataBeCombinedWithDataObtained.getString("key")))
          && (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key")))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA).getString(LABEL),
            true, false);
          JsonArray legalBasisForProcessingCombinedData = section.getJsonArray(
            LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA);
          if (Objects.nonNull(legalBasisForProcessingCombinedData)
            && !legalBasisForProcessingCombinedData.isEmpty()) {
            generatedJsonArrayValues(document, legalBasisForProcessingCombinedData, VALUE);
            if (containsKey(legalBasisForProcessingCombinedData, "b")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("otherLegalBasisForProcessingCombinedData")
                  .getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                section.getString("otherLegalBasisForProcessingCombinedData"), false, false);
            }
          }
        }

        if(Objects.nonNull(section5)){
          JsonObject formatOfTheElectronicHealthData = section5.getJsonObject(
            FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
          if ((Objects.nonNull(formatOfTheElectronicHealthData) && "b".equals(
            formatOfTheElectronicHealthData.getString("key"))) && (
            Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
              applyingForDataOnBehalfOfPublicSector.getString("key")))) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("europeanUnionInstitution").getString(LABEL),
              true, false);
            JsonArray europeanUnionInstitution = section.getJsonArray("europeanUnionInstitution");
            generatedJsonArrayValues(document, europeanUnionInstitution, VALUE);
          }
        }

        if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA).getString(LABEL),
            true, false);
          JsonArray legalBasisForProcessingApplicationData = section.getJsonArray(
            LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA);
          if (Objects.nonNull(legalBasisForProcessingApplicationData)
            && !legalBasisForProcessingApplicationData.isEmpty()) {
            generatedJsonArrayValues(document, legalBasisForProcessingApplicationData, VALUE);
            if (containsKey(legalBasisForProcessingApplicationData, "b")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("otherLegalBasisForProcessingApplicationData")
                  .getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                section.getString("otherLegalBasisForProcessingApplicationData"), false, false);
            }
          }
        }

        if ((Objects.nonNull(willDataBeCombinedWithDataObtained) && "a".equals(
          willDataBeCombinedWithDataObtained.getString("key"))) &&
          (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
            applyingForDataOnBehalfOfPublicSector.getString("key")))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA)
              .getString(LABEL),
            true, false);
          JsonArray legalBasisForProcessingCombinedApplicationData = section.getJsonArray(
            LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA);
          if (Objects.nonNull(legalBasisForProcessingCombinedApplicationData)
            && !legalBasisForProcessingCombinedApplicationData.isEmpty()) {
            generatedJsonArrayValues(document, legalBasisForProcessingCombinedApplicationData,
              VALUE);
            if (containsKey(legalBasisForProcessingCombinedApplicationData, "b")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("otherLegalBasisForProcessingCombinedApplicationData")
                  .getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                section.getString("otherLegalBasisForProcessingCombinedApplicationData"), false,
                false);
            }
          }
        }
      }

      if(Objects.nonNull(section2)) {
        JsonArray purposeForWhichDataWillBeUsed = section2.getJsonArray(
          PURPOSE_FOR_WHICH_DATA_WILL_BE_USED);
        if ((Objects.nonNull(purposeForWhichDataWillBeUsed) && containsKey(
          purposeForWhichDataWillBeUsed, "e"))) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("attachResearchPermitIfRequired")
              .getString(LABEL),
            true, false);
          JsonArray attachResearchPermitIfRequiredDoc = section.getJsonArray(
            "attachResearchPermitIfRequired");
          generatedJsonArrayValues(document, attachResearchPermitIfRequiredDoc, NAME);
        }
      }
    }
  }

  private void section9Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section) {
    addHeaderToTheText(document, languageMap.getString("section9h5"), "Heading1");
    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_INFORMATION).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_INFORMATION).getString(TOOLTIP), false, true);
      addTextToNewParagraphWithTable(document, section.getString(
          ADDITIONAL_INFORMATION), false, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_ATTACHMENT).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(ADDITIONAL_ATTACHMENT).getString(TOOLTIP), false, true);
      JsonArray additionalAttachment = section.getJsonArray(
        ADDITIONAL_ATTACHMENT);
      generatedJsonArrayValues(document, additionalAttachment, "name");
    }
  }

  private void section10Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section10h5", "section10Description",
      aboutThisSection);

    if (Objects.nonNull(section)) {
      addTextToNewParagraph(document, languageMap.getString("submitYouApplicationText"), false,
        false);
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

  private Boolean displayLawfulnessOfProcessing(JsonObject section, JsonObject section3,
    JsonObject section5, JsonObject section7) {
    JsonObject applyingForDataOnBehalfOfPublicSector = null;
    JsonArray legalBasisForProcessingPersonalData = null;
    JsonObject willDataBeCombinedWithDataObtained = null;
    JsonArray legalBasisForProcessingCombinedData = null;
    JsonObject formatOfTheElectronicHealthData = null;
    JsonArray legalBasisForProcessingApplicationData = null;
    JsonArray legalBasisForProcessingCombinedApplicationData = null;

    if (Objects.nonNull(section)) {
      legalBasisForProcessingPersonalData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
      legalBasisForProcessingCombinedData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA);

      legalBasisForProcessingApplicationData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA);
      legalBasisForProcessingCombinedApplicationData = section.getJsonArray(
        LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA);
    }
    if (Objects.nonNull(section3)) {
      applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
        APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    }
    if(Objects.nonNull(section5)){
      formatOfTheElectronicHealthData = section5.getJsonObject(
        FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    }
    if (Objects.nonNull(section7)) {
      willDataBeCombinedWithDataObtained = section7.getJsonObject(
        WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);
    }

    return ((Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
      applyingForDataOnBehalfOfPublicSector.getString("key")) ||
      (Objects.nonNull(legalBasisForProcessingPersonalData) && containsKey(
        legalBasisForProcessingPersonalData, "b"))) ||
      (Objects.nonNull(willDataBeCombinedWithDataObtained) && "a".equals(
        willDataBeCombinedWithDataObtained.getString("key")) && (
        Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "b".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key"))) ||
        (Objects.nonNull(legalBasisForProcessingCombinedData) && containsKey(
          legalBasisForProcessingCombinedData, "c")) ||
        (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
          applyingForDataOnBehalfOfPublicSector.getString("key")) &&
          (Objects.nonNull(formatOfTheElectronicHealthData) && "a".equals(
            formatOfTheElectronicHealthData.getString("key"))) ||
          ((Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
            applyingForDataOnBehalfOfPublicSector.getString("key"))) ||
            (Objects.nonNull(legalBasisForProcessingApplicationData) && containsKey(
              legalBasisForProcessingApplicationData, "b")) ||
            (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
              applyingForDataOnBehalfOfPublicSector.getString("key")) && (
              Objects.nonNull(willDataBeCombinedWithDataObtained) && "a".equals(
                willDataBeCombinedWithDataObtained.getString("key"))) ||
              (Objects.nonNull(legalBasisForProcessingCombinedApplicationData) && containsKey(
                legalBasisForProcessingCombinedApplicationData, "b")))))));
  }

  private Boolean displayLawfulForProcessingPersonalData(JsonObject section3,
    JsonObject section5) {
    JsonObject applyingForDataOnBehalfOfPublicSector = null;
    JsonObject formatOfTheElectronicHealthData = null;
    JsonObject legalOrNaturalPerson = null;

    if (Objects.nonNull(section3)) {
      applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
        APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
      legalOrNaturalPerson = section3.getJsonObject(
        LEGAL_OR_NATURAL_PERSON);
    }
    if (Objects.nonNull(section5)){
      formatOfTheElectronicHealthData = section5.getJsonObject(
        FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    }

    return ((Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && "a".equals(
      applyingForDataOnBehalfOfPublicSector.getString("key"))
      || (Objects.nonNull(legalOrNaturalPerson) && "b".equals(
      legalOrNaturalPerson.getString("key")))) && (Objects.nonNull(formatOfTheElectronicHealthData)
      &&
      "b".equals(formatOfTheElectronicHealthData.getString("key"))));
  }

}
