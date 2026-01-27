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
import static eu.europa.data.health.access.Constants.COUNTRY;
import static eu.europa.data.health.access.Constants.COUNTRY_OF_PROJECT_LEADER;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA;
import static eu.europa.data.health.access.Constants.FULL_NAME;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_2;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_3;
import static eu.europa.data.health.access.Constants.HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS;
import static eu.europa.data.health.access.Constants.HEADING_3;
import static eu.europa.data.health.access.Constants.HOW_IS_THE_STUDY_COHORT_FORMED;
import static eu.europa.data.health.access.Constants.HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.INCLUSION_CRITERIA;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS;
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
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY_DESCRIPTION;
import static eu.europa.data.health.access.Constants.OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW;
import static eu.europa.data.health.access.Constants.ORDER_OF_CONTROLS_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.ORDER_OF_RELATIVES_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.OTHER_PERMIT_APPLICATIONS;
import static eu.europa.data.health.access.Constants.PERMIT_DECISION_ATTACHMENT;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_A;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_B;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_C;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_D;
import static eu.europa.data.health.access.Constants.PROTECTION_AND_SECURITY_STATEMENTS_E;
import static eu.europa.data.health.access.Constants.PROVENANCE;
import static eu.europa.data.health.access.Constants.PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS;
import static eu.europa.data.health.access.Constants.PROVIDE_INFORMATION_LIST;
import static eu.europa.data.health.access.Constants.PUBLISHER;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.RESULT;
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
import static eu.europa.data.health.access.Constants.SUMMARY_OF_RESEARCH_PLAN;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentExportDataAccessManager extends DocumentExportUtility {


  private final DBHandler dbHandler;
  private final Logger log = LoggerFactory.getLogger(getClass());

  public DocumentExportDataAccessManager(TranslationManager translationManager, DBHandler dbHandler,
    WebClient client, JsonObject config) {
    super(config, client, translationManager);
    this.dbHandler = dbHandler;
  }

  public void exportApplicationDataAccessForm(RoutingContext context) {
    String lg = context.pathParam("lg");
    String id = context.pathParam("id");
    String type = context.pathParam("type");
    String tl = context.pathParam("tl");
    int version;
    try {
      version = Integer.parseInt(context.pathParam("version"));
    } catch (NumberFormatException e) {
      context.response()
        .setStatusCode(400)
        .end("Invalid application version parameter");
      return;
    }

    JsonObject accessQuery = new JsonObject()
      .put("_id", id)
      .put("version", version);

    dbHandler.findOne(DATA_ACCESS_APPLICATION_COLLECTION, accessQuery)
      .onSuccess(accessJsonObject -> {
        if(accessJsonObject != null) {
          processApplicationForExport(context, accessJsonObject, tl, lg, type);
        } else {
          findInDispatcherCollection(context, id, version, tl, lg, type);
        }
      })
      .onFailure(cause -> context.response().setStatusCode(500).end());
  }

  private void findInDispatcherCollection(RoutingContext context, String id, int version, String tl, String lg, String type) {
    JsonObject dispatcherQuery = new JsonObject()
      .put("application._id", id)
      .put("application.version", version);

    dbHandler.findOne(DISPATCHER_COLLECTION, dispatcherQuery)
      .onSuccess(dispatcherJsonObject -> {
        if (dispatcherJsonObject != null) {
          JsonObject dispatcherApplication = dispatcherJsonObject.getJsonObject("application");
          processApplicationForExport(context, dispatcherApplication, tl, lg, type);
        } else {
          context.response()
            .setStatusCode(404)
            .end("No application found with ID " + id + " and version " + version);
        }
      })
      .onFailure(cause -> context.response().setStatusCode(500).end("Database error"));
  }

  private void processApplicationForExport(RoutingContext context, JsonObject application,
    String tl, String lg, String type) {
    JsonObject form = tl.equals(application.getString("inputLanguage"))
      ? application.getJsonObject("form")
      : application.getJsonObject("form_translations").getJsonObject(tl);
    JsonArray datasets = application.getJsonArray("datasets", new JsonArray());
    getDatasetsValues(context, datasets, lg, form, type, "DataAccessForm.docx");
  }

  /**
   * Export application form for external use
   * @param context  RoutingContext
   */
  public void externalExportApplicationForm(RoutingContext context) {
    JsonObject jsonBody = context.body().asJsonObject();
    JsonArray datasets = jsonBody.getJsonArray("datasets");
    JsonObject form = jsonBody.getJsonObject("form");
    getDatasetsValues(context, datasets, "en", form, "docx", "DataAccessForm.docx");
  }

  @Override
  protected void addFormData(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject form, JsonArray datasetsValues) {
    // Implement mapping logic based on document template structure
    String aboutThisSection = languageMap.getJsonObject("commonFields")
      .getString("aboutThisSection");
    section1Mappings(document, languageMap.getJsonObject(SECTION_1),
      datasetsValues, aboutThisSection, form != null ? form.getJsonObject(SECTION_1) : null);
    section2Mappings(document, languageMap.getJsonObject(SECTION_2),
      form != null ? form.getJsonObject(SECTION_2) : null, aboutThisSection);
    section3Mappings(document, languageMap.getJsonObject(SECTION_3),
      form != null ? form.getJsonObject(SECTION_3) : null, aboutThisSection);
    section4Mappings(document, languageMap.getJsonObject(SECTION_4),
      form != null ? form.getJsonObject(SECTION_4) : null,
      form != null ? form.getJsonObject(SECTION_3) : null, aboutThisSection);
    section5Mappings(document, languageMap.getJsonObject(SECTION_5),
      form != null ? form.getJsonObject(SECTION_2) : null,
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
    JsonArray datasets, String aboutThisSection, JsonObject section1) {
    addSectionWithHeader(document, languageMap, "section1h5", "section1Description",
      aboutThisSection);
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationDatasets").getString(LABEL),
      true, false);
    writeDatasetsValuesWithVariables(document, datasets, TITLE, "en", section1, languageMap);
    addTextToNewParagraph(document,
      languageMap.getJsonObject("applicationCountries").getString(LABEL),
      true, false);
    writeDatasetsValues(document, datasets, COUNTRY, LABEL);
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
    writeDatasetsValues(document, datasets, PROVENANCE, "en");
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
    addTextToNewParagraphWithTable(document, getObjectValue(section, COUNTRY_OF_PROJECT_LEADER),
      false, false);

    // --- PURPOSE FOR WHICH DATA WILL BE USED ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED).getString(TOOLTIP), false,
      true);
    generatedJsonArrayValues(document, getArrayField(section, PURPOSE_FOR_WHICH_DATA_WILL_BE_USED),
      VALUE);

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
    addSectionWithHeader(document, languageMap, "section3h5", "section3Description",
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

  private void section5Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section2, JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section5h5", "section5Description",
      aboutThisSection);

    addTextToNewParagraph(document, languageMap.getJsonObject("purposeOfUse").getString(LABEL),
      true, false);

    JsonArray purposeOfUse = getArrayField(section2, "purposeForWhichDataWillBeUsed");
    generatedJsonArrayValues(document, purposeOfUse, VALUE);

    // --- PURPOSE OF USE ---
    if (section2 != null) {
      if (purposeOfUse != null && containsSelectedValueWithoutSpecificValue(purposeOfUse, "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleLabel"), true,
          false);

        boolean isPersonResponsibleSameAsContactPerson = isChecked(section,
          PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON).getString(LABEL),
          true, isPersonResponsibleSameAsContactPerson);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResponsibleName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResponsibleName"), false,
          false);
      }

      if (purposeOfUse != null && containsKey(purposeOfUse, "e")) {
        addTextToNewParagraph(document, languageMap.getString("personResponsibleForResearchLabel"),
          true, false);

        boolean isPersonResearchSameAsContactPerson = isChecked(section,
          PERSON_RESEARCH_SAME_AS_CONTACT_PERSON);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON).getString(LABEL),
          true, isPersonResearchSameAsContactPerson);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("personResearchName").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "personResearchName"), false,
          false);
      }
    }

    // --- PROJECT PURPOSE INFORMATION ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("whyTheDataIsNeeded").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whyTheDataIsNeeded"), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whatIsTheAimAndTopicOfTheProject").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whatIsTheAimAndTopicOfTheProject"),
      false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whichAreTheExpectedBenefits").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whichAreTheExpectedBenefits"),
      false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("describeApplicantsQualification").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "describeApplicantsQualification"),
      false, false);

    // --- CONDITIONAL BLOCK BASED ON PURPOSE OF USE ---
    if (section2 != null) {
      if (purposeOfUse != null && containsKey(purposeOfUse, "b")) {
        addTextToNewParagraph(document, languageMap.getJsonObject(LEGAL_BASIS).getString(LABEL),
          true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject(LEGAL_BASIS).getString(TOOLTIP),
          true, false);
        addTextToNewParagraphWithTable(document, getField(section, LEGAL_BASIS), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("linkToTheSupportingLegalBasis").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "linkToTheSupportingLegalBasis"),
          false, false);
      }

      if (purposeOfUse != null && containsSelectedValueWithoutSpecificValue(purposeOfUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA).getString(LABEL), true,
          false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_PLAN_FOR_USING_THE_DATA).getString(TOOLTIP), false,
          true);

        JsonArray summaryOfPlanForUsingTheDataDoc = getArrayField(section,
          SUMMARY_OF_PLAN_FOR_USING_THE_DATA);
        generatedJsonArrayValues(document, summaryOfPlanForUsingTheDataDoc, "name");
      }

      if (purposeOfUse != null && containsKey(purposeOfUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN).getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(SUMMARY_OF_RESEARCH_PLAN).getString(TOOLTIP), false, true);

        JsonArray summaryOfResearchPlanDoc = getArrayField(section, SUMMARY_OF_RESEARCH_PLAN);
        generatedJsonArrayValues(document, summaryOfResearchPlanDoc, "name");
      }
    }

    // --- FORMAT OF THE ELECTRONIC HEALTH DATA ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA).getString(LABEL), true,
      false);
    String formatOfTheElectronicHealthDataValue = getObjectValue(section,
      FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    addTextToNewParagraphWithTable(document, formatOfTheElectronicHealthDataValue, false, false);

    if (formatOfTheElectronicHealthDataValue != null) {
      JsonObject formatOfTheElectronicHealthData = getObject(section,
        FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
      if (isKey(formatOfTheElectronicHealthData, "b")) {
        addTextToNewParagraph(document, languageMap.getString("inOrderToApplyPseudonymisedData"),
          true, false);

        boolean isPseudonymisedDataA = isChecked(section, IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A).getString(LABEL), true,
          isPseudonymisedDataA);

        boolean isPseudonymisedDataB = isChecked(section, IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B).getString(LABEL), true,
          isPseudonymisedDataB);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("whyNeedForPseudonymisation").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "whyNeedForPseudonymisation"),
          false, false);
      }
    }
  }

  private void section6Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonArray section6Array, String aboutThisSection, JsonArray datasets) {
    addSectionWithHeader(document, languageMap, "section6h5", "section6Description",
      aboutThisSection);

    List<String> catalogIds = extractCatalogIds(datasets);

    for (String catalogId : catalogIds) {
      JsonArray singleCatalogIdArray = new JsonArray();
      singleCatalogIdArray.add(catalogId);

      JsonArray filterDatasets = filterDatasetsByCatalogIds(datasets, singleCatalogIdArray);

      JsonObject section6Object = findSection6ObjectByCatalogId(section6Array, catalogId);

      section60(document, languageMap, section6Object, filterDatasets);
      section61(document, languageMap, section6Object, filterDatasets);
      section62(document, languageMap, section6Object, filterDatasets);
      section63(document, languageMap, section6Object, filterDatasets);
    }
  }

  private void section61(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,
    JsonArray datasets) {
    addHeaderToTheText(document,
      languageMap.getString("descriptionSection6_1") + " " + datasets.getJsonObject(0)
        .getJsonObject(RESULT).getJsonObject(COUNTRY)
        .getString(LABEL, null),
      HEADING_3);

    // --- STUDY COHORT FORMATION ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(HOW_IS_THE_STUDY_COHORT_FORMED).getString(LABEL), true, false);
    JsonObject howStudyCohortFormed = getObject(section, HOW_IS_THE_STUDY_COHORT_FORMED);
    addTextToNewParagraphWithTable(document, getObjectValue(section, HOW_IS_THE_STUDY_COHORT_FORMED),
      false,
      false);

    if (isKey(howStudyCohortFormed, "b") || isKey(howStudyCohortFormed, "c")) {
      addTextToNewParagraph(document, languageMap.getJsonObject(
          HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS)
        .getString(LABEL), true, false);
      JsonObject formedBasedOnInfo = getObject(section,
        HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
      addTextToNewParagraphWithTable(document,
        (formedBasedOnInfo != null) ? formedBasedOnInfo.getString(VALUE, "") : "", false, false);
    }

    JsonObject formedBasedOnInfo = getObject(section,
      HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);

    addTextToNewParagraph(document, languageMap.getJsonObject(
        "hasTheCohortBeenBasedOnADataPermit")
      .getString(LABEL), true, false);
    JsonObject hasTheCohortBeenBasedOnADataPermit = getObject(section,
      "hasTheCohortBeenBasedOnADataPermit");
    addTextToNewParagraphWithTable(document,
      (hasTheCohortBeenBasedOnADataPermit != null) ? hasTheCohortBeenBasedOnADataPermit.getString(VALUE, "") : "", false, false);

    if ((isKey(howStudyCohortFormed, "b") || isKey(howStudyCohortFormed, "c")) && isKey(formedBasedOnInfo,
      "a")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("doesTheInformedConsentCoverTheRequestedRegistryExtractions")
          .getString(LABEL), true, false);
      JsonObject consentCovering = getObject(section,
        "doesTheInformedConsentCoverTheRequestedRegistryExtractions");
      if (consentCovering != null) {
        addTextToNewParagraphWithTable(document, consentCovering.getString(VALUE, ""), false,
          false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("consentAndInformationLetterAttachment").getString(LABEL), true,
        false);
      JsonArray consentLetterAttachment = getArrayField(section,
        "consentAndInformationLetterAttachment");
      generatedJsonArrayValues(document, consentLetterAttachment, "name");
    }
    if ((isKey(howStudyCohortFormed, "b") || isKey(howStudyCohortFormed, "c")) && isKey(hasTheCohortBeenBasedOnADataPermit,
      "a")) {
      boolean confirmPermitGranted = isChecked(section,
        CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT);
      addTextWithCheckboxToNewParagraph(document, languageMap.getJsonObject(
          CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT).getString(LABEL), false,
        confirmPermitGranted);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("ifTheGroupHasBeenFormedBasedOnPreviousIssued").getString(LABEL),
        true, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("previouslyIssuedPermitIssuer1").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitIssuer1"),
        false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("previouslyIssuedPermitDate1").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitDate1"),
        false, false);

      addTextToNewParagraph(document, languageMap.getString("validityPeriod1"), true, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodFrom1").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document,
        getField(section, "previouslyIssuedPermitValidityPeriodFrom1"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodTo1").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document,
        getField(section, "previouslyIssuedPermitValidityPeriodTo1"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("previouslyIssuedPermitNumber1").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitNumber1"),
        false, false);
    }

    if (isKey(howStudyCohortFormed, "b") && isKey(formedBasedOnInfo,
      "a")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(PERMIT_DECISION_ATTACHMENT).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(PERMIT_DECISION_ATTACHMENT).getString(TOOLTIP), false, true);
      JsonArray permitDecisionAttachment = getArrayField(section, PERMIT_DECISION_ATTACHMENT);
      generatedJsonArrayValues(document, permitDecisionAttachment, "name");
    }

    if (isKey(howStudyCohortFormed, "b") && formedBasedOnInfo != null && isKey(formedBasedOnInfo,
      "b")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("howTheStudyCohortWasObtained").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "howTheStudyCohortWasObtained"),
        false, false);
    }

    // --- INFORMATION PROVIDED TO SUBJECTS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS)
        .getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS)
        .getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getObjectValue(section,
        PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS), false,
      false);

    JsonObject infoProvidedSubjects = getObject(section,
      PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS);

    if (infoProvidedSubjects != null) {
      if (isKey(infoProvidedSubjects, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("howProvidedInformationOfTheDataUseToCorrespondingSubjects")
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          getField(section, "howProvidedInformationOfTheDataUseToCorrespondingSubjects"), false,
          false);
      } else {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects")
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          getField(section, "whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects"), false,
          false);
      }
    }

    // --- INFORMATION PROVIDER DETAILS ---
    if ((isKey(howStudyCohortFormed, "b") || isKey(howStudyCohortFormed, "c"))) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("whoWillDeliverTheInformationOnTheStudy").getString(LABEL), true,
        false);

      boolean sameAsContactProvider = isChecked(section,
        INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON);
      addTextWithCheckboxToNewParagraph(document,
        languageMap.getJsonObject(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON).getString(LABEL),
        false, sameAsContactProvider);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("informationProviderName").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "informationProviderName"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("informationProviderEmail").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "informationProviderEmail"), false,
        false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(INFORMATION_PROVIDER_PHONE).getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document,
        buildPhone(getObject(section, INFORMATION_PROVIDER_PHONE)), false, false);
    }

    // --- NEED OF WHOLE POPULATION ---
    if (isKey(howStudyCohortFormed, "d")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WHY_NEED_DATA_OF_A_WHOLE_POPULATION).getString(LABEL), true,
        false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(WHY_NEED_DATA_OF_A_WHOLE_POPULATION).getString(TOOLTIP), false,
        true);
      addTextToNewParagraphWithTable(document,
        getField(section, WHY_NEED_DATA_OF_A_WHOLE_POPULATION), false, false);
    }

    // --- STUDY COHORT SIZE ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("sizeOfTheStudyCohort").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "sizeOfTheStudyCohort"), false,
      false);

    JsonObject cohortSizeEstimation = getObject(section, "sizeOfTheStudyCohortEstimationOrExact");
    if (cohortSizeEstimation != null) {
      addTextToNewParagraphWithTable(document, cohortSizeEstimation.getString(VALUE, ""), false,
        false);
    }

    addTextToNewParagraph(document,
      languageMap.getJsonObject("whyNeedStudyCohortOfThisSize").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "whyNeedStudyCohortOfThisSize"),
      false, false);

    // --- ETHICAL REVIEW ATTACHMENT ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("ethicalReviewAttachment").getString(LABEL), true, false);
    JsonArray ethicalReviewAttachment = getArrayField(section, "ethicalReviewAttachment");
    generatedJsonArrayValues(document, ethicalReviewAttachment, "name");

    addTextToNewParagraph(document, languageMap.getString("dataHolders"), true, false);
    writeDatasetsValues(document, datasets, PUBLISHER, "name");
    addTextToNewParagraph(document, languageMap.getString("whichDatabasesDataExtracted"), true,
      false);
    writeDatasetsValues(document, datasets, PROVENANCE, "en");
    addTextToNewParagraph(document, languageMap.getString("whichDatasetDataExtracted"), true,
      false);
    writeDatasetsValues(document, datasets, TITLE, "en");

    // --- DATA EXTRACTION DETAILS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("variablesToBeUsedInDataExtractionAttachment").getString(LABEL),
      true, false);
    JsonArray variablesExtractionAttachment = getArrayField(section,
      "variablesToBeUsedInDataExtractionAttachment");
    generatedJsonArrayValues(document, variablesExtractionAttachment, "name");

    addTextToNewParagraph(document,
      languageMap.getJsonObject("timePeriodOfDataExtraction").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "timePeriodOfDataExtraction"), false,
      false);

    addTextToNewParagraph(document, languageMap.getJsonObject("extractionMethod").getString(LABEL),
      true, false);
    JsonObject extractionMethod = getObject(section, "extractionMethod");

    if (extractionMethod != null) {
      addTextToNewParagraphWithTable(document, extractionMethod.getString(VALUE, ""), false, false);
      String extractionKey = extractionMethod.getString("key", "");

      if ("c".equals(extractionKey)) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("samplingMethod").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "samplingMethod"), false, false);
      }

      if ("c".equals(extractionKey) || "a".equals(extractionKey)) {
        addTextToNewParagraph(document, languageMap.getJsonObject(SAMPLE_SIZE).getString(LABEL),
          true, false);
        addTextToNewParagraph(document, languageMap.getJsonObject(SAMPLE_SIZE).getString(TOOLTIP),
          false, true);
        addTextToNewParagraphWithTable(document, getField(section, SAMPLE_SIZE), false, false);
      }
    }

    addTextToNewParagraph(document, languageMap.getJsonObject(INCLUSION_CRITERIA).getString(LABEL),
      true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(INCLUSION_CRITERIA).getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getField(section, INCLUSION_CRITERIA), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("potentialExclusionCriteria").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "potentialExclusionCriteria"), false,
      false);

    addTextToNewParagraph(document, languageMap.getString("dataExtraction"), true, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject(HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED).getString(LABEL), true,
      false);
    JsonObject howOftenExtracted = getObject(section, HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED);
    addTextToNewParagraphWithTable(document,
      getObjectValue(section, HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED), false,
      false);

    if (isKey(howOftenExtracted, "b")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY).getString(TOOLTIP), false,
        true);

      JsonObject needExtractionEvery = getObject(section, NEED_FOR_DATA_EXTRACTION_EVERY);
      if (needExtractionEvery != null) {
        addTextToNewParagraphWithTable(document, needExtractionEvery.getString(VALUE, ""), false,
          false);

        if (isKey(needExtractionEvery, "d")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("needForDataExtractionEveryOther").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document,
            getField(section, "needForDataExtractionEveryOther"), false, false);
        }
      }
    }

    // --- NEED FOR DATA EXTRACTION EVERY DESCRIPTION ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY_DESCRIPTION).getString(LABEL), true,
      false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(NEED_FOR_DATA_EXTRACTION_EVERY_DESCRIPTION).getString(TOOLTIP), true,
      false);
    addTextToNewParagraphWithTable(document,
      getField(section, NEED_FOR_DATA_EXTRACTION_EVERY_DESCRIPTION), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("orderForDataExtraction").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "orderForDataExtraction"), false,
      false);
  }

  private void section62(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,
    JsonArray datasets) {
    addHeaderToTheText(document,
      languageMap.getString("definingExtractionCriteria") + " " + datasets.getJsonObject(0)
        .getJsonObject(RESULT).getJsonObject(COUNTRY)
        .getString(LABEL, null),
      HEADING_3);

    // --- CONTROLS EXTRACTION DETAILS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WILL_CONTROLS_BE_EXTRACTED).getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getObjectValue(section, WILL_CONTROLS_BE_EXTRACTED),
      false, false);

    JsonObject willControlsExtracted = getObject(section, WILL_CONTROLS_BE_EXTRACTED);
    if (isKey(willControlsExtracted, "a")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject(HAS_THE_GROUP_RELATIVES_FORMED_2).getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document,
        getObjectValue(section, HAS_THE_GROUP_RELATIVES_FORMED_2), false,
        false);

      JsonObject hasGroupRelativesFormed = getObject(section, HAS_THE_GROUP_RELATIVES_FORMED_2);
      if (hasGroupRelativesFormed != null) {
        if (isKey(hasGroupRelativesFormed, "a")) {
          addTextToNewParagraph(document, languageMap.getString("ifTheGroupHasBeenFormed"), true,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("previouslyIssuedPermitIssuer2").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitIssuer2"),
            false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("previouslyIssuedPermitDate2").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitDate2"),
            false, false);

          addTextToNewParagraph(document, languageMap.getString("validityPeriod2"), true, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodFrom2").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document,
            getField(section, "previouslyIssuedPermitValidityPeriodFrom2"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodTo2").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document,
            getField(section, "previouslyIssuedPermitValidityPeriodTo2"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("previouslyIssuedPermitNumber2").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitNumber2"),
            false, false);
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document,
        getObjectValue(section, WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS), false, false);

      JsonObject willSameDataExtracted = getObject(section,
        WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS);
      if (isKey(willSameDataExtracted, "b")) {
        addTextToNewParagraph(document, languageMap.getString("fromWhichDataHolderLabel"), true,
          false);
        writeDatasetsValues(document, datasets, PUBLISHER, "name");

        addTextToNewParagraph(document, languageMap.getString("fromWhichDatabaseLabel"), true,
          false);
        writeDatasetsValues(document, datasets, PROVENANCE, "en");

        addTextToNewParagraph(document, languageMap.getString("fromWhichDatasetLabel"), true,
          false);
        writeDatasetsValues(document, datasets, TITLE, "en");

        addTextToNewParagraph(document,
          languageMap.getJsonObject("variablesForDataExtractionAttachments").getString(LABEL), true, false);
        JsonArray variablesForDataExtractionAttachments = getArrayField(section, "variablesForDataExtractionAttachments");
        generatedJsonArrayValues(document, variablesForDataExtractionAttachments, "name");

        addTextToNewParagraph(document,
          languageMap.getJsonObject("timePeriodForDataExtraction").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "timePeriodForDataExtraction"),
          false, false);
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("extractionCriteriaForControls").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "extractionCriteriaForControls"),
        false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("sizeOfControlGroup").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "sizeOfControlGroup"), false,
        false);

      addTextToNewParagraphWithTable(document,
        getObjectValue(section, "sizeOfControlGroupEstimationOrExact"), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("controlsPerPersonInStudyCohort").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "controlsPerPersonInStudyCohort"),
        false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("exclusionCriteriaForControls").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "exclusionCriteriaForControls"),
        false, false);

      addTextToNewParagraph(document, languageMap.getString("dataExtractionAndOrderForControls"),
        true, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY)
          .getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document,
        getObjectValue(section, WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY), false, false);

      JsonObject willDataExtractedSimultaneously = getObject(section,
        WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY);

      if (isKey(willDataExtractedSimultaneously, "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(DATA_EXTRACTION_FREQUENCY).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getObjectValue(section, DATA_EXTRACTION_FREQUENCY),
          false, false);

        JsonObject extractionFrequency = getObject(section, DATA_EXTRACTION_FREQUENCY);
        if (isKey(extractionFrequency, "b")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("dataExtractionFrequencyMultipleTimesInformation")
              .getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document,
            getField(section, "dataExtractionFrequencyMultipleTimesInformation"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY).getString(LABEL), true,
            false);
          addTextToNewParagraph(document,
            languageMap.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY).getString(TOOLTIP), false,
            true);

          JsonObject dataNeedsExtractedEvery = getObject(section, DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          if (dataNeedsExtractedEvery != null) {
            addTextToNewParagraphWithTable(document, dataNeedsExtractedEvery.getString(VALUE, ""),
              false, false);

            if (isKey(dataNeedsExtractedEvery, "d")) {
              addTextToNewParagraph(document,
                languageMap.getJsonObject("specifyOther").getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document, getField(section, "specifyOther"), false,
                false);
            }
          }
        }

        addTextToNewParagraph(document,
          languageMap.getJsonObject(MORE_INFO_ON_EXTRACTION_PERIODS).getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, MORE_INFO_ON_EXTRACTION_PERIODS),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(ORDER_OF_CONTROLS_DATA_EXTRACTION).getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document,
          getField(section, ORDER_OF_CONTROLS_DATA_EXTRACTION), false, false);
      }
    }
  }

  private void section63(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section,
    JsonArray datasets) {
    addHeaderToTheText(document,
      languageMap.getString("definingTheExtractionCriteria") + " " + datasets.getJsonObject(0)
        .getJsonObject(RESULT).getJsonObject(COUNTRY)
        .getString(LABEL, null), HEADING_3);
    // --- RELATIVES EXTRACTION DETAILS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WILL_RELATIVES_BE_EXTRACTED).getString(LABEL), true, false);
    JsonObject willRelativesBeExtracted = getObject(section, WILL_RELATIVES_BE_EXTRACTED);
    addTextToNewParagraphWithTable(document, getObjectValue(section, WILL_RELATIVES_BE_EXTRACTED),
      false, false);

    if (willRelativesBeExtracted != null) {
      if (isKey(willRelativesBeExtracted, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(HAS_THE_GROUP_RELATIVES_FORMED_3).getString(LABEL), true, false);
        JsonObject hasGroupRelativesFormed3 = getObject(section, HAS_THE_GROUP_RELATIVES_FORMED_3);
        addTextToNewParagraphWithTable(document,
          getObjectValue(section, HAS_THE_GROUP_RELATIVES_FORMED_3), false,
          false);

        if (hasGroupRelativesFormed3 != null) {
          if (isKey(hasGroupRelativesFormed3, "a")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("ifTheGroupOfRelativesHasBeenFormed").getString(LABEL), true,
              false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("previouslyIssuedPermitIssuer3").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitIssuer3"),
              false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("previouslyIssuedPermitDate3").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitDate3"),
              false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodFrom3").getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              getField(section, "previouslyIssuedPermitValidityPeriodFrom3"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("previouslyIssuedPermitValidityPeriodTo3").getString(LABEL),
              true, false);
            addTextToNewParagraphWithTable(document,
              getField(section, "previouslyIssuedPermitValidityPeriodTo3"), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("previouslyIssuedPermitNumber3").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, getField(section, "previouslyIssuedPermitNumber3"),
              false, false);
          }
        }
        addTextToNewParagraph(document,
          languageMap.getJsonObject(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES).getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document,
          getObjectValue(section, WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES), false, false);

        JsonObject sameDataRelatives = getObject(section,
          WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES);
        if (isKey(sameDataRelatives, "b")) {
          addTextToNewParagraph(document, languageMap.getString("whichDataHoldersForRelatives"),
            true, false);
          writeDatasetsValues(document, datasets, PUBLISHER, "name");

          addTextToNewParagraph(document, languageMap.getString("whichDatabaseForRelatives"), true,
            false);
          writeDatasetsValues(document, datasets, PROVENANCE, "en");

          addTextToNewParagraph(document, languageMap.getString("whichDatasetsForRelatives"), true,
            false);
          writeDatasetsValues(document, datasets, TITLE, "en");


          addTextToNewParagraph(document,
            languageMap.getJsonObject("variablesForDataExtractionRelativesAttachments").getString(LABEL), true, false);
          JsonArray variablesForDataExtractionRelativesAttachments = getArrayField(section, "variablesForDataExtractionRelativesAttachments");
          generatedJsonArrayValues(document, variablesForDataExtractionRelativesAttachments, "name");

          addTextToNewParagraph(document,
            languageMap.getJsonObject("timePeriodForDataExtractionRelatives").getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document,
            getField(section, "timePeriodForDataExtractionRelatives"), false, false);
        }

        addTextToNewParagraph(document,
          languageMap.getJsonObject("relationshipToStudyCohort").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "relationshipToStudyCohort"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("sizeOfRelativesGroup").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "sizeOfRelativesGroup"), false,
          false);

        addTextToNewParagraphWithTable(document,
          getObjectValue(section, "sizeOfRelativesGroupEstimateOrExact"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY)
            .getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          getObjectValue(section, WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY), false,
          false);

        JsonObject willDataSimultaneouslyRelatives = getObject(section,
          WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY);
        if (isKey(willDataSimultaneouslyRelatives, "b")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject(DATA_EXTRACTION_FREQUENCY_RELATIVES).getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document,
            getObjectValue(section, DATA_EXTRACTION_FREQUENCY_RELATIVES), false, false);

          JsonObject extractionFrequencyRelatives = getObject(section,
            DATA_EXTRACTION_FREQUENCY_RELATIVES);
          if (isKey(extractionFrequencyRelatives, "b")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("dataExtractionFrequencyMultipleTimesInformationRelatives")
                .getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              getField(section, "dataExtractionFrequencyMultipleTimesInformationRelatives"), false,
              false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("needForDataExtractionEveryOptions").getString(LABEL), true,
              false);
            addTextToNewParagraph(document,
              languageMap.getJsonObject("needForDataExtractionEveryOptions").getString(TOOLTIP),
              false, true);

            JsonObject needEveryRelatives = getObject(section,
              DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
            if (needEveryRelatives != null) {
              addTextToNewParagraphWithTable(document, needEveryRelatives.getString(VALUE, ""),
                false, false);

              if (isKey(needEveryRelatives, "d")) {
                addTextToNewParagraph(document,
                  languageMap.getJsonObject("specifyOtherRelatives").getString(LABEL), true, false);
                addTextToNewParagraphWithTable(document, getField(section, "specifyOtherRelatives"),
                  false, false);
              }
            }
          }

          addTextToNewParagraph(document,
            languageMap.getJsonObject(MORE_INFO_ON_EXTRACTION_PERIODS_RELATIVES).getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document,
            getField(section, MORE_INFO_ON_EXTRACTION_PERIODS_RELATIVES), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject(ORDER_OF_RELATIVES_DATA_EXTRACTION).getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document,
            getField(section, ORDER_OF_RELATIVES_DATA_EXTRACTION), false, false);
        }
      }
    }
  }

  private void section7Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section7h5", "section7Description",
      aboutThisSection);

    // --- WILL DATA BE COMBINED WITH OTHER DATA ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED).getString(LABEL), true,
      false);

    String willDataBeCombinedWithDataObtainedValue = getObjectValue(section,
      WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);
    addTextToNewParagraphWithTable(document, willDataBeCombinedWithDataObtainedValue, false, false);

    if (willDataBeCombinedWithDataObtainedValue != null) {
      JsonObject willDataBeCombinedWithDataObtained = getObject(section,
        WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);
      if (isKey(willDataBeCombinedWithDataObtained, "a")) {
        addTextToNewParagraph(document, languageMap.getString("listOtherDataToBeCompined"), true,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("combinedCountries").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "combinedCountries"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("combinedDataHolders").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "combinedDataHolders"), false,
          false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("combinedDatabaseRegistries").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "combinedDatabaseRegistries"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("combinedDatasetsRegistries").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "combinedDatasetsRegistries"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject(PROVIDE_INFORMATION_LIST).getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(PROVIDE_INFORMATION_LIST).getString(TOOLTIP), false, true);

        JsonArray provideInformationList = getArrayField(section, PROVIDE_INFORMATION_LIST);
        if (provideInformationList != null && !provideInformationList.isEmpty()) {
          for (Object item : provideInformationList) {
            if (item instanceof JsonObject provideInformation) {
              addTextToNewParagraph(document, languageMap.getJsonObject("dataset").getString(LABEL),
                true, false);
              addTextToNewParagraphWithTable(document, provideInformation.getString("dataset", ""),
                false, false);

              addTextToNewParagraph(document,
                languageMap.getJsonObject("numberOfFiles").getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                provideInformation.getString("numberOfFiles", ""), false, false);

              addTextToNewParagraph(document,
                languageMap.getJsonObject("formatOfFiles").getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                provideInformation.getString("formatOfFiles", ""), false, false);

              addTextToNewParagraph(document,
                languageMap.getJsonObject("sizeOfFiles").getString(LABEL), true, false);
              addTextToNewParagraphWithTable(document,
                provideInformation.getString("sizeOfFiles", ""), false, false);

              addTextToNewParagraph(document, languageMap.getJsonObject("notes").getString(LABEL),
                true, false);
              addTextToNewParagraphWithTable(document, provideInformation.getString("notes", ""),
                false, false);
            }
          }
        }
      }

      addTextToNewParagraph(document, languageMap.getString("listAnyOtherDataPermit"), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject("otherPermits").getString(LABEL),
        true, false);
      JsonArray otherDataPermits = getArrayField(section, "otherDataPermits");

      if (otherDataPermits != null && !otherDataPermits.isEmpty()) {
        for (Object item : otherDataPermits) {
          if (item instanceof JsonObject otherDataPermit) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitIssuer").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document, otherDataPermit.getString("permitIssuer", ""),
              false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitStartDateOfIssue").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              otherDataPermit.getString("permitStartDateOfIssue", ""), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitEndDateOfIssue").getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              otherDataPermit.getString("permitEndDateOfIssue", ""), false, false);

            addTextToNewParagraph(document,
              languageMap.getJsonObject("permitIdentificationInformation").getString(LABEL), true,
              false);
            addTextToNewParagraphWithTable(document,
              otherDataPermit.getString("permitIdentificationInformation", ""), false, false);
          }
        }
      }

      addTextToNewParagraph(document,
        languageMap.getJsonObject("attachmentOfPermitDocuments").getString(LABEL), true, false);
      JsonArray attachmentOfPermitDocuments = getArrayField(section, "attachmentOfPermitDocuments");
      generatedJsonArrayValues(document, attachmentOfPermitDocuments, "name");

      addTextToNewParagraph(document,
        languageMap.getJsonObject(OTHER_PERMIT_APPLICATIONS).getString(LABEL), true, false);
      addTextToNewParagraph(document,
        languageMap.getJsonObject(OTHER_PERMIT_APPLICATIONS).getString(TOOLTIP), false, true);

      String otherPermitApplicationsValue = getObjectValue(section, OTHER_PERMIT_APPLICATIONS);
      addTextToNewParagraphWithTable(document, otherPermitApplicationsValue, false, false);

      if (otherPermitApplicationsValue != null) {
        JsonObject otherPermitApplications = getObject(section, OTHER_PERMIT_APPLICATIONS);
        if (isKey(otherPermitApplications, "a")) {
          addTextToNewParagraph(document, languageMap.getString("listTheOtherData"), true, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("dateOfSubmittingTheApplication").getString(LABEL), true,
            false);
          addTextToNewParagraphWithTable(document,
            getField(section, "dateOfSubmittingTheApplication"), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("applicationIssuer").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, getField(section, "applicationIssuer"), false,
            false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("identificationCode").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, getField(section, "identificationCode"), false,
            false);
        }
      }
    }
  }

  private void section8Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonObject section2, JsonObject section3, JsonObject section5,
    JsonObject section7, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section8h5", "section8Description",
      aboutThisSection);
    // --- TECHNICAL ENVIRONMENT & DATA NEEDS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("technicalRequirementsForEnvironment").getString(LABEL), true,
      false);
    addTextToNewParagraphWithTable(document,
      getField(section, "technicalRequirementsForEnvironment"), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("nameWebsiteAddressForEnvironment").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "nameWebsiteAddressForEnvironment"),
      false, false);

    addTextToNewParagraph(document, languageMap.getString("healthDataAccessBody"), false, false);

    // --- WHEN IS THE DATA NEEDED ---
    addTextToNewParagraph(document, languageMap.getJsonObject(WHEN_DATA_NEEDED).getString(LABEL),
      true, false);
    addTextToNewParagraph(document, languageMap.getJsonObject(WHEN_DATA_NEEDED).getString(TOOLTIP),
      false, true);

    JsonObject whenDataNeeded = getObject(section, WHEN_DATA_NEEDED);
    if (whenDataNeeded != null) {
      addTextToNewParagraphWithTable(document, whenDataNeeded.getString(VALUE, ""), false, false);

      if (isKey(whenDataNeeded, "b")) {
        addTextToNewParagraph(document, languageMap.getJsonObject("ifLaterWhen").getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document, getField(section, "ifLaterWhen"), false, false);
      }
    }

    // --- ADDITIONAL ACCESS OR PROCESSING INFORMATION ---
    JsonObject applyingForDataOnBehalfOfPublicSector =
      (section3 != null) ? getObject(section3, APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR) : null;

    if (applyingForDataOnBehalfOfPublicSector != null) {
      if (isKey(applyingForDataOnBehalfOfPublicSector, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("informationWhichDataCanAccess").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document, getField(section, "informationWhichDataCanAccess"),
          false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("frequencyOfAccessOrDataUpdates").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document,
          getField(section, "frequencyOfAccessOrDataUpdates"), false, false);

      } else if (isKey(applyingForDataOnBehalfOfPublicSector, "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("estimatedStartAndEnd").getString(LABEL), true, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("estimatedStartDatesForDataProcessing").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document,
          getField(section, "estimatedStartDatesForDataProcessing"), false, false);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("estimatedEndDatesForDataProcessing").getString(LABEL), true,
          false);
        addTextToNewParagraphWithTable(document,
          getField(section, "estimatedEndDatesForDataProcessing"), false, false);
      }
    }

    // --- STORAGE AFTER USE ---
    addTextToNewParagraph(document, languageMap.getString("ifStoreNeeded"), true, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("startPeriodOfInactiveDataStorage").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "startPeriodOfInactiveDataStorage"),
      false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("endPeriodOfInactiveDataStorage").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "endPeriodOfInactiveDataStorage"),
      false, false);

    // --- DATA TRANSFER ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED).getString(LABEL), true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED).getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document, getObjectValue(section, WILL_THE_DATA_BE_TRANSFERRED),
      false,
      false);

    JsonObject willTheDataBeTransferred = getObject(section, WILL_THE_DATA_BE_TRANSFERRED);
    // --- WILL THE DATA BE TRANSFERRED OUTSIDE EU ---
    if (willTheDataBeTransferred != null) {
      if (isKey(willTheDataBeTransferred, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("whichCountriesDataBeProcessed").getString(LABEL), true, false);
        JsonArray whichCountriesDataBeProcessed = getArrayField(section,
          "whichCountriesDataBeProcessed");
        generatedJsonArrayValues(document, whichCountriesDataBeProcessed, VALUE);

        addTextToNewParagraph(document,
          languageMap.getJsonObject("whyWillDataBeTransferredOutsideEU").getString(LABEL), true,
          false);

        boolean isArticle47 = isChecked(section,
          WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47)
            .getString(LABEL), true, isArticle47);

        if (isArticle47) {
          JsonArray optionsArticle47 = getArrayField(section,
            "whyWillDataBeTransferredOutsideEUArticle47Options");
          generatedJsonArrayValues(document, optionsArticle47, VALUE);
        }

        boolean isArticle48 = isChecked(section,
          WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48)
            .getString(LABEL), true, isArticle48);

        if (isArticle48) {
          boolean isArticle48a = isChecked(section,
            WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A)
              .getString(LABEL), true, isArticle48a);

          boolean isArticle48b = isChecked(section,
            WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B);
          addTextWithCheckboxToNewParagraph(document,
            languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
              .getString(LABEL), true, isArticle48b);

          if (isArticle48b) {
            JsonArray optionsArticle48b = getArrayField(section,
              "whyWillDataBeTransferredOutsideEUArticle48bOptions");
            generatedJsonArrayValues(document, optionsArticle48b, VALUE);

            boolean isArticle48c = isChecked(section,
              WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C);
            addTextWithCheckboxToNewParagraph(document,
              languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
                .getString(LABEL), true, isArticle48c);

            if (isArticle48c) {
              JsonObject reason48c = getObject(section,
                "whyWillDataBeTransferredOutsideEUArticle48cOpt");
              addTextToNewParagraphWithTable(document,
                (reason48c != null) ? reason48c.getString(VALUE, "") : "", false, false);
            }

            boolean isArticle48d = isChecked(section,
              WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D);
            addTextWithCheckboxToNewParagraph(document,
              languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D)
                .getString(LABEL), true, isArticle48d);

            boolean isArticle48e = isChecked(section,
              WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E);
            addTextWithCheckboxToNewParagraph(document,
              languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E)
                .getString(LABEL), true, isArticle48e);
          }
        }

        boolean isArticle49 = isChecked(section,
          WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49);
        addTextWithCheckboxToNewParagraph(document,
          languageMap.getJsonObject(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49)
            .getString(LABEL), true, isArticle49);

        JsonObject legalBasisOutsideEU = getObject(section,
          LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU)
            .getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU)
            .getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document,  getObjectValue(section,
            LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU), false,
          false);

        if (isKey(legalBasisOutsideEU, "c")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("legalBasisForTransferringTheDataOutsideEUOtherOptions")
              .getString(LABEL), true, false);
          JsonArray otherOptions = getArrayField(section,
            "legalBasisForTransferringTheDataOutsideEUOtherOptions");
          generatedJsonArrayValues(document, otherOptions, VALUE);
        }

        if (isKey(legalBasisOutsideEU, "b")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("safeguardsAreProvidedByReferringGDCP").getString(LABEL),
            true, false);
          JsonArray safeguards = getArrayField(section, "safeguardsAreProvidedByReferringGDCP");
          generatedJsonArrayValues(document, safeguards, VALUE);
        }
      }
    }

    // --- ORGANIZATION WHO CONTROLS DATA ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA).getString(LABEL),
      true, false);
    addTextToNewParagraph(document,
      languageMap.getJsonObject(WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA)
        .getString(TOOLTIP), false, true);
    addTextToNewParagraphWithTable(document,
      getField(section, WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA), false, false);

    // --- DATA MINIMISATION PRINCIPLE BASED ON PUBLIC SECTOR APPLICATION ---
    if (applyingForDataOnBehalfOfPublicSector != null) {
      if (isKey(applyingForDataOnBehalfOfPublicSector, "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE).getString(LABEL), true,
          false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE).getString(TOOLTIP),
          false, true);
        addTextToNewParagraphWithTable(document,
          getField(section, COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE), false, false);
      } else if (isKey(applyingForDataOnBehalfOfPublicSector, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER)
            .getString(LABEL), true, false);
        addTextToNewParagraph(document,
          languageMap.getJsonObject(COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER)
            .getString(TOOLTIP), false, true);
        addTextToNewParagraphWithTable(document,
          getField(section, COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER), false, false);
      }
    }

    addTextToNewParagraph(document,
      languageMap.getJsonObject(OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW).getString(LABEL),
      true, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject(OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW).getString(TOOLTIP),
      false, true);
    addTextToNewParagraphWithTable(document, getObjectValue(section, OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW),
      false,
      false);

    JsonObject mechanismProvidedInNationalLaw = getObject(section,OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW);
    if (isKey(mechanismProvidedInNationalLaw, "a")) {
      addTextToNewParagraph(document, languageMap.getJsonObject("providedJustificationException").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "providedJustificationException"), false, false);
    }

    // --- PROTECTION AND SECURITY STATEMENTS ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("protectionAndSecurityStatements").getString(LABEL), true, false);

    boolean statementA = isChecked(section, PROTECTION_AND_SECURITY_STATEMENTS_A);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_A).getString(LABEL), false,
      statementA);

    boolean statementB = isChecked(section, PROTECTION_AND_SECURITY_STATEMENTS_B);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_B).getString(LABEL), false,
      statementB);

    boolean statementC = isChecked(section, PROTECTION_AND_SECURITY_STATEMENTS_C);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_C).getString(LABEL), false,
      statementC);

    boolean statementD = isChecked(section, PROTECTION_AND_SECURITY_STATEMENTS_D);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_D).getString(LABEL), false,
      statementD);

    boolean statementE = isChecked(section, PROTECTION_AND_SECURITY_STATEMENTS_E);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(PROTECTION_AND_SECURITY_STATEMENTS_E).getString(LABEL), false,
      statementE);

    // --- PEOPLE WHO WILL PROCESS THE DATA ---
    JsonArray peopleWhoWillProcessData = getArrayField(section, "peopleWhoWillBeProcessingTheData");
    addTextToNewParagraph(document, languageMap.getString("onlyThePeopleMentioned"), false,
      false);

    boolean isSameAsContact = isChecked(section, SAME_AS_CONTACT_PERSON);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(SAME_AS_CONTACT_PERSON).getString(LABEL), true, isSameAsContact);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("listTheFullNames").getString(LABEL), true, false);

    if (peopleWhoWillProcessData != null && !peopleWhoWillProcessData.isEmpty()) {
      for (Object item : peopleWhoWillProcessData) {
        if (item instanceof JsonObject jsonItem) {
          addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL),
            true, false);
          addTextToNewParagraphWithTable(document, jsonItem.getString(FULL_NAME, ""), false, false);

          addTextToNewParagraph(document,
            languageMap.getJsonObject("emailAddress").getString(LABEL), true, false);
          addTextToNewParagraphWithTable(document, jsonItem.getString("emailAddress", ""), false,
            false);
        }
      }
    }

    // --- LAWFULNESS OF PROCESSING ---
    if (Boolean.TRUE.equals(displayLawfulnessOfProcessing(section, section3, section5, section7))
      || Boolean.TRUE.equals(displayLawfulForProcessingPersonalData(section3, section5))) {

      addTextToNewParagraph(document, languageMap.getString("lawfulnessOfProcessing"), false,
        false);

      if (Boolean.TRUE.equals(displayLawfulForProcessingPersonalData(section3, section5))) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("lawfulForProcessingPersonalData").getString(LABEL), true,
          false);
        JsonArray lawfulForProcessingPersonalData = getArrayField(section,
          "lawfulForProcessingPersonalData");
        generatedJsonArrayValues(document, lawfulForProcessingPersonalData, VALUE);
      }

      if (isKey(applyingForDataOnBehalfOfPublicSector, "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA).getString(LABEL),
          true, false);
        JsonArray legalBasisPersonalData = getArrayField(section,
          LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
        generatedJsonArrayValues(document, legalBasisPersonalData, VALUE);
      }

      JsonArray legalBasisPersonalDataCheck = getArrayField(section,
        LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA);
      if (legalBasisPersonalDataCheck != null && containsKey(legalBasisPersonalDataCheck, "c")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("otherLegalBasisForProcessingPersonalData").getString(LABEL),
          true, false);
        addTextToNewParagraphWithTable(document,
          getField(section, "otherLegalBasisForProcessingPersonalData"), false, false);
      }

      JsonObject combinedDataObtained =
        (section7 != null) ? getObject(section7, WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED) : null;

      if (isKey(combinedDataObtained, "a")
        && isKey(applyingForDataOnBehalfOfPublicSector, "b")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA).getString(LABEL),
          true, false);
        JsonArray legalBasisCombinedData = getArrayField(section,
          LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA);
        if (legalBasisCombinedData != null && !legalBasisCombinedData.isEmpty()) {
          generatedJsonArrayValues(document, legalBasisCombinedData, VALUE);
          if (containsKey(legalBasisCombinedData, "b")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("otherLegalBasisForProcessingCombinedData")
                .getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              getField(section, "otherLegalBasisForProcessingCombinedData"), false, false);
          }
        }
      }

      if (section5 != null) {
        JsonObject formatData = getObject(section5, FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
        if (isKey(formatData, "b")
          && isKey(
          applyingForDataOnBehalfOfPublicSector, "a")) {
          addTextToNewParagraph(document,
            languageMap.getJsonObject("europeanUnionInstitution").getString(LABEL), true, false);
          JsonArray europeanUnionInstitution = getArrayField(section, "europeanUnionInstitution");
          generatedJsonArrayValues(document, europeanUnionInstitution, VALUE);
        }
      }

      if (isKey(applyingForDataOnBehalfOfPublicSector, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA).getString(LABEL),
          true, false);
        JsonArray legalBasisApplicationData = getArrayField(section,
          LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA);
        generatedJsonArrayValues(document, legalBasisApplicationData, VALUE);
        if (legalBasisApplicationData != null && !legalBasisApplicationData.isEmpty()) {
          if (containsKey(legalBasisApplicationData, "b")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("otherLegalBasisForProcessingApplicationData")
                .getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              getField(section, "otherLegalBasisForProcessingApplicationData"), false, false);
          }
        }
      }

      if (isKey(combinedDataObtained, "a")
        && isKey(applyingForDataOnBehalfOfPublicSector, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject(LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA)
            .getString(LABEL), true, false);
        JsonArray legalBasisCombinedAppData = getArrayField(section,
          LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA);
        generatedJsonArrayValues(document, legalBasisCombinedAppData, VALUE);
        if (legalBasisCombinedAppData != null && !legalBasisCombinedAppData.isEmpty()) {
          if (containsKey(legalBasisCombinedAppData, "b")) {
            addTextToNewParagraph(document,
              languageMap.getJsonObject("otherLegalBasisForProcessingCombinedApplicationData")
                .getString(LABEL), true, false);
            addTextToNewParagraphWithTable(document,
              getField(section, "otherLegalBasisForProcessingCombinedApplicationData"), false,
              false);
          }
        }
      }
    }

    // --- RESEARCH PERMIT ATTACHMENT ---
    if (section2 != null) {
      JsonArray purposeDataUse = getArrayField(section2, PURPOSE_FOR_WHICH_DATA_WILL_BE_USED);
      if (purposeDataUse != null && containsKey(purposeDataUse, "e")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("attachResearchPermitIfRequired").getString(LABEL), true,
          false);
        JsonArray attachResearchPermit = getArrayField(section, "attachResearchPermitIfRequired");
        generatedJsonArrayValues(document, attachResearchPermit, NAME);
      }
    }
  }

  private void section9Mappings(WordprocessingMLPackage document, JsonObject languageMap,
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

  private void section10Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section10h5", "section10Description",
      aboutThisSection);

    // --- FINAL CONFIRMATION & AWARENESS ---
    addTextToNewParagraph(document, languageMap.getString("submitYouApplicationText"), false,
      false);

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
    if (Objects.nonNull(section5)) {
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
    if (Objects.nonNull(section5)) {
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

  public void downloadDataPermitDocument(RoutingContext context) {

    log.debug("Will download Data Permit Document");

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      InputStream templateInputStream = getClass().getResourceAsStream(
        "/templates/DataPermitTemplate.docx")) {

      if (templateInputStream == null) {
        log.error("Data Permit Template not found");
        context.response().setStatusCode(500)
          .end(new JsonObject().put("result_message", "Data Permit Template not found").encode());
        return;
      }

      WordprocessingMLPackage document = WordprocessingMLPackage.load(templateInputStream);
      document.save(outputStream);
      context.response().putHeader("Content-Length", String.valueOf(outputStream.size()))
        .putHeader("Content-Disposition",
          "attachment; filename=TEHDAS2_Task6.3_Data_permit_template_vC1.docx")
        .putHeader("Content-Type",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        .end(Buffer.buffer(outputStream.toByteArray()));
      log.info("Data Permit Document downloaded");

    } catch (Exception e) {
      log.error("Error was thrown during download of Data Permit Document, with error message: {}",
        e.getMessage());
      context.response().setStatusCode(500).end();
    }
  }
}
