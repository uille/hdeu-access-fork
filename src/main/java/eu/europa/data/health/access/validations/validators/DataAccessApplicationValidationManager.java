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
package eu.europa.data.health.access.validations.validators;

import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.ERROR_CODE;
import static eu.europa.data.health.access.Constants.EXTRACTION_CRITERIA_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_2;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_3;
import static eu.europa.data.health.access.Constants.HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU_OTHER_OPTIONS;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.MANDATORY_FIELD;
import static eu.europa.data.health.access.Constants.MESSAGE;
import static eu.europa.data.health.access.Constants.MORE_INFO_ON_EXTRACTION_PERIODS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.ORDER_OF_CONTROLS_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.OTHER_PERMIT_APPLICATIONS;
import static eu.europa.data.health.access.Constants.PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM;
import static eu.europa.data.health.access.Constants.PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS;
import static eu.europa.data.health.access.Constants.RELATIONSHIP_TO_STUDY_COHORT;
import static eu.europa.data.health.access.Constants.RESULT_MESSAGE;
import static eu.europa.data.health.access.Constants.SPECIFY_OTHER;
import static eu.europa.data.health.access.Constants.SPECIFY_OTHER_RELATIVES;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.THE_PARSING_OF_JSON_FAILED_WITH_ERROR;
import static eu.europa.data.health.access.Constants.TIME_PERIOD_FOR_DATA_EXTRACTION;
import static eu.europa.data.health.access.Constants.TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES;
import static eu.europa.data.health.access.Constants.VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS;
import static eu.europa.data.health.access.Constants.VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS;
import static eu.europa.data.health.access.Constants.WHEN_DATA_NEEDED;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT;
import static eu.europa.data.health.access.Constants.WILL_CONTROLS_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_RELATIVES_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES;
import static eu.europa.data.health.access.Constants.WILL_THE_DATA_BE_TRANSFERRED;
import static eu.europa.data.health.access.Constants.OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW;
import static eu.europa.data.health.access.Constants.PROVIDED_JUSTIFICATION_EXCEPTION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.annotations.AlphanumericFormat;
import eu.europa.data.health.access.annotations.AttachmentValidation;
import eu.europa.data.health.access.annotations.DateHigherOrEqual;
import eu.europa.data.health.access.annotations.DateLowerOrEqual;
import eu.europa.data.health.access.annotations.EmailFormat;
import eu.europa.data.health.access.annotations.ListContainsOneMoreValueExpectASpecificValue;
import eu.europa.data.health.access.annotations.ListContainsOneMoreValueThanASpecificValue;
import eu.europa.data.health.access.annotations.ListContainsSpecificValue;
import eu.europa.data.health.access.annotations.ListNotContainsSpecificValue;
import eu.europa.data.health.access.annotations.ListObjectFieldsNotNull;
import eu.europa.data.health.access.annotations.ListOfAttachmentsValidation;
import eu.europa.data.health.access.annotations.PhoneFormat;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnAndOrObjectValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnTwoObjectValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnValue;
import eu.europa.data.health.access.annotations.Section8DataBeTransferred;
import eu.europa.data.health.access.annotations.Section8DataBeTransferredOpt48;
import eu.europa.data.health.access.dto.ApplicationCommonDTO;
import eu.europa.data.health.access.dto.sections.Section3DTO;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import eu.europa.data.health.access.dto.sections.SectionDTOBuilderFactory;
import eu.europa.data.health.access.dto.sections.data_access.Section6CountryDTO;
import eu.europa.data.health.access.dto.sections.data_access.Section6DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section7DTO;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.exceptions.GeneralException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jetbrains.annotations.NotNull;

/**
 * DataAccessValidationManager class implementing all validations for the data access application
 * entity
 */
public class DataAccessApplicationValidationManager extends AbstractSchemaValidator {

  /**
   * A method validating that the json object inside the request body complies to the data access
   * application json schema
   *
   * @param context The routing context of the request
   */
  public void validateDataAccessApplicationJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.DATA_ACCESS_APPLICATION_SCHEMA_FILE.getValue());
  }

  public void validatePutDistributionsApplicationJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.DATA_ACCESS_APPLICATION_UPDATE_SCHEMA_FILE.getValue());
  }

  public void validateDeleteDatasetsJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context,
      SchemaFiles.DATA_ACCESS_APPLICATION_DELETE_DATASETS_SCHEMA_FILE.getValue());
  }

  public void validateDeleteDistributionsJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context,
      SchemaFiles.DATA_ACCESS_APPLICATION_DELETE_DISTRIBUTIONS_SCHEMA_FILE.getValue());
  }

  public void validateChangeStatusJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.DATA_ACCESS_APPLICATION_CHANGE_STATUS_FILE.getValue());
  }

  public void validateChangeTitleJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.DATA_ACCESS_APPLICATION_CHANGE_TITLE_FILE.getValue());
  }

  public void validateUpdate(RoutingContext context) {

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    // Extract sectionNumber field from url as path variable
    Integer sectionNumberFromUrl = Integer.valueOf(context.pathParam("section_number"));

    // Generate a SectionDTO object based on the extracted section number and context
    SectionDTO sectionDTO;
    if (sectionNumberFromUrl.equals(6)) {
      JsonArray sectionForm = context.body().asJsonArray();
      performCleanUpForm6(sectionForm);
      sectionDTO = generateSectionDTO(sectionNumberFromUrl, context,
        sectionForm.toString());
    } else {
      JsonObject sectionForm = context.body().asJsonObject();
      performCleanUpForm(sectionNumberFromUrl, sectionForm);
      context.put("body", sectionForm.toString());
      sectionDTO = generateSectionDTO(sectionNumberFromUrl, context,
        sectionForm.toString());
    }

    // Validate SectionDTO
    Set<ConstraintViolation<SectionDTO>> violations = validator.validate(sectionDTO);

    //special handling for section 3 phones (as they are mandatory and conditional at the same time)
    if (sectionNumberFromUrl.equals(3) && sectionDTO instanceof Section3DTO section3DTO) {
      // Access the legalOrNaturalPerson property
      String legalOrNaturalPerson = Objects.isNull(section3DTO.getLegalOrNaturalPerson()) ? null
        : section3DTO.getLegalOrNaturalPerson().getKey();
      if (Objects.nonNull(legalOrNaturalPerson) && legalOrNaturalPerson.equals(
        "b")) {
        // remove contactPersonPhone violations as they do not exist for "Natural Person"
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith(CONTACT_PERSON_PHONE));
      } else if (Objects.nonNull(legalOrNaturalPerson) && legalOrNaturalPerson.equals(
        "a")) {
        // remove naturalPersonPhone violations as they do not exist for "Legal Person"
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith(NATURAL_PERSON_PHONE));
      } else if (Objects.isNull(legalOrNaturalPerson)) {
        // remove both contactPersonPhone and naturalPersonPhone violations as none of "Natural person"
        // or "Legal person" is selected
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith(CONTACT_PERSON_PHONE));
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith(NATURAL_PERSON_PHONE));
      }
    }
    if (sectionNumberFromUrl.equals(7) && sectionDTO instanceof Section7DTO section7DTO) {
      // Access the legalOrNaturalPerson property
      String willDataBeCombinedWithDataObtained =
        Objects.isNull(section7DTO.getWillDataBeCombinedWithDataObtained()) ? null
          : section7DTO.getWillDataBeCombinedWithDataObtained().getKey();
      if (Objects.nonNull(willDataBeCombinedWithDataObtained)
        && willDataBeCombinedWithDataObtained.equals(
        "b")) {
        // remove contactPersonPhone violations as they do not exist for "Natural Person"
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith("provideInformationList"));
      }
    }
    if (!violations.isEmpty()) {
      // Handle validation errors
      List<FieldError> errors = new ArrayList<>();
      for (ConstraintViolation<SectionDTO> violation : violations) {
        String errorCode = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();

        if (sectionNumberFromUrl == 6) {
          String fieldName = ((PathImpl) violation.getPropertyPath()).getLeafNode().getName();
          if (errorCode.equals(PhoneFormat.class.getSimpleName()) ||
            (errorCode.equals(NotBlank.class.getSimpleName()) && fieldName.equals("number"))) {
            fieldName = ((PathImpl) violation.getPropertyPath()).getLeafNode().getParent()
              .getName();
          }
          Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
          List<String> params = new ArrayList<>();
          if (maxParam != null) {
            params.add(maxParam.toString());
          }
          buildCustomValidationErrors(errors, errorCode, fieldName, params,
            violation, getCountryId(violation));

        } else {
          String fieldName = violation.getPropertyPath().toString();
          Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
          List<String> params = new ArrayList<>();
          if (maxParam != null) {
            params.add(maxParam.toString());
            // Set value of field in context to null
            context.body().asJsonObject().put(fieldName, null);
          }
          buildCustomValidationErrors(errors, errorCode, fieldName, params,
            violation);
        }
      }
      context.put(ERRORS, new GeneralException(errors).getErrors());
    }
    context.next();
  }

  private void performCleanUpForm(Integer sectionNumberFromUrl, JsonObject mongoApplication) {
    if (sectionNumberFromUrl == null) {
      throw new IllegalArgumentException("Section number cannot be null");
    }

    switch (sectionNumberFromUrl) {
      case 2:
        cleanSection2(mongoApplication);
        break;
      case 3:
        cleanSection3(mongoApplication);
        break;
      case 4:
        cleanSection4(mongoApplication);
        break;
      case 5:
        cleanSection5(mongoApplication);
        break;
      case 7:
        cleanSection7(mongoApplication);
        break;
      case 8:
        cleanSection8(mongoApplication);
        break;
      default:
        break;
    }
  }

  private void performCleanUpForm6(JsonArray mongoApplication) {
    cleanSection6(mongoApplication);
  }

  private void cleanSection2(JsonObject section2) {
    boolean isSummaryChecked =
      section2.containsKey(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY)
        && section2.getBoolean(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY,
        false);

    if (!isSummaryChecked) {
      section2.remove(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON);
    } else {
      section2.remove("summaryOfTheProject");
    }
  }

  private void cleanSection3(JsonObject section3) {
    JsonObject applyingForDataOnBehalfOfPublicSector = section3.getJsonObject(
      APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR);
    if (Objects.nonNull(applyingForDataOnBehalfOfPublicSector) && !"a".equals(
      applyingForDataOnBehalfOfPublicSector.getString("key"))) {
      section3.remove(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS);
    }

    JsonObject legalPerson = section3.getJsonObject(LEGAL_OR_NATURAL_PERSON);
    //if is "a" is legal person else i b natural person
    if (Objects.nonNull(legalPerson) && Objects.equals(legalPerson.getString("key"), "b")) {
      section3.remove(LEGAL_PERSON_NAME);
      section3.remove("legalPersonAddress");
      section3.remove("legalPersonZipCode");
      section3.remove("legalPersonCity");
      section3.remove(LEGAL_PERSON_COUNTRY);
      section3.remove("contactPersonName");
      section3.remove(CONTACT_PERSON_EMAIL);
      section3.remove(CONTACT_PERSON_PHONE);
      section3.remove("contactPersonOrganisationName");
      section3.remove("contactPersonBusinessID");
      section3.remove("contactPersonRelationship");
    } else {
      section3.remove("naturalPersonName");
      section3.remove(NATURAL_PERSON_ADDRESS);
      section3.remove("naturalPersonZipCode");
      section3.remove("naturalPersonCity");
      section3.remove("naturalPersonCountry");
      section3.remove("naturalPersonEmail");
      section3.remove(NATURAL_PERSON_PHONE);
      section3.remove("naturalPersonJobTitle");
      section3.remove("naturalPersonAffiliation");
    }
  }

  private void cleanSection4(JsonObject section4) {
    JsonObject invoiceType = section4.getJsonObject("invoiceType");

    if (Objects.nonNull(invoiceType) && !Objects.equals(invoiceType.getString("key"), "b")) {
      section4.remove("invoiceAddress");
    }

    JsonObject isTheProjectFinanciallyCovered = section4.getJsonObject(
      "isTheProjectFinanciallyCovered");
    if (Objects.nonNull(isTheProjectFinanciallyCovered) && !"a".equals(
      isTheProjectFinanciallyCovered.getString("key"))) {
      section4.remove("rangeOfAmountOfFinancing");
    }
  }

  private void cleanSection5(JsonObject section5) {
    JsonObject formatOfTheElectronicHealthData = section5.getJsonObject(
      FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA);
    if (Objects.nonNull(formatOfTheElectronicHealthData) && !Objects.equals(
      formatOfTheElectronicHealthData.getString("key"), "b")) {
      section5.remove(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A);
      section5.remove(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B);
      section5.remove("whyNeedForPseudonymisation");
    }
  }

  private void cleanSection6(JsonArray section6Array) {
    for (int i = 0; i < section6Array.size(); i++) {

      //SECTION 6_1
      JsonObject section = section6Array.getJsonObject(i);
      JsonObject howIsTheStudyCohortFormed = section.getJsonObject("howIsTheStudyCohortFormed");

      if (Objects.nonNull(howIsTheStudyCohortFormed) &&
        (!Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(howIsTheStudyCohortFormed.getString("key"), "c"))) {
        section.remove(HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
      }

      JsonObject hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants = section.getJsonObject(
        HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS);
      if ((Objects.nonNull(howIsTheStudyCohortFormed) && (!Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(howIsTheStudyCohortFormed.getString("key"), "c"))) || (
        Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
          && !Objects.equals(
          hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString("key"),
          "a"))) {
        section.remove("doesTheInformedConsentCoverTheRequestedRegistryExtractions");
        section.remove("consentAndInformationLetterAttachment");
      }

      if ((Objects.nonNull(howIsTheStudyCohortFormed) && (!Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "c")))) {
        section.remove("confirmThatDataPermitHasBeenGrantedForTheResearchProject");
        section.remove("permitDecisionAttachment");
        section.remove("previouslyIssuedPermitIssuer1");
        section.remove("previouslyIssuedPermitDate1");
        section.remove("previouslyIssuedPermitValidityPeriodFrom1");
        section.remove("previouslyIssuedPermitValidityPeriodTo1");
        section.remove("previouslyIssuedPermitNumber1");
      }

      if ((Objects.nonNull(howIsTheStudyCohortFormed) && !Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "b")) || (
        Objects.nonNull(hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants)
          && !Objects.equals(
          hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants.getString("key"),
          "b"))) {
        section.remove("howTheStudyCohortWasObtained");
      }

      JsonObject providedInformationOfTheDataUseToCorrespondingSubjects = section.getJsonObject(
        PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS);

      if (Objects.nonNull(providedInformationOfTheDataUseToCorrespondingSubjects)
        && !Objects.equals(
        providedInformationOfTheDataUseToCorrespondingSubjects.getString("key"),
        "a")) {
        section.remove("howProvidedInformationOfTheDataUseToCorrespondingSubjects");
      }

      if (Objects.nonNull(providedInformationOfTheDataUseToCorrespondingSubjects)
        && !Objects.equals(providedInformationOfTheDataUseToCorrespondingSubjects.getString("key"),
        "b")) {
        section.remove("whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects");
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && (
        !Objects.equals(howIsTheStudyCohortFormed.getString("key"), "b") && !Objects.equals(
          howIsTheStudyCohortFormed.getString("key"), "c"))) {
        section.remove(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON);
        section.remove("informationProviderName");
        section.remove("informationProviderEmail");
        section.remove("informationProviderPhone");
      }

      if (Objects.nonNull(howIsTheStudyCohortFormed) && !Objects.equals(
        howIsTheStudyCohortFormed.getString("key"), "d")) {
        section.remove("whyNeedDataOfaWholePopulation");
      }

      JsonObject extractionMethod = section.getJsonObject("extractionMethod");

      if (Objects.nonNull(extractionMethod) && !extractionMethod.getString("key").equals("c")) {
        section.remove("samplingMethod");
      }

      if (Objects.nonNull(extractionMethod) && (!extractionMethod.getString("key").equals("c")
        && !extractionMethod.getString("key")
        .equals("a"))) {
        section.remove("sampleSize");
      }

      JsonObject howOftenDoesTheDataNeedToBeExtracted = section.getJsonObject(
        "howOftenDoesTheDataNeedToBeExtracted");
      if (Objects.nonNull(howOftenDoesTheDataNeedToBeExtracted) && !Objects.equals(
        howOftenDoesTheDataNeedToBeExtracted.getString("key"), "b")) {
        section.remove(NEED_FOR_DATA_EXTRACTION_EVERY);
      }

      JsonObject needForDataExtractionEvery = section.getJsonObject(
        NEED_FOR_DATA_EXTRACTION_EVERY);
      if ((Objects.nonNull(howOftenDoesTheDataNeedToBeExtracted) && !Objects.equals(
        howOftenDoesTheDataNeedToBeExtracted.getString("key"), "b"))
        || (Objects.nonNull(needForDataExtractionEvery) && !Objects.equals(
        needForDataExtractionEvery.getString("key"), "d"))) {
        section.remove("needForDataExtractionEveryOther");
      }

      //SECTION 6_2

      JsonObject willControlsBeExtracted = section.getJsonObject(WILL_CONTROLS_BE_EXTRACTED);
      if (Objects.nonNull(willControlsBeExtracted) && !Objects.equals(
        willControlsBeExtracted.getString("key"), "a")) {
        section.remove(HAS_THE_GROUP_RELATIVES_FORMED_2);
        section.remove("previouslyIssuedPermitDate2");
        section.remove("previouslyIssuedPermitValidityPeriodFrom2");
        section.remove("previouslyIssuedPermitValidityPeriodTo2");
        section.remove("previouslyIssuedPermitNumber2");
        section.remove("previouslyIssuedPermitIssuer2");
        section.remove("previouslyIssuedPermitDate2");
        section.remove("previouslyIssuedPermitValidityPeriodFrom2");
        section.remove("previouslyIssuedPermitValidityPeriodTo2");
        section.remove("previouslyIssuedPermitNumber2");
        section.remove(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS);
        section.remove(VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS);
        section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION);
        section.remove(EXTRACTION_CRITERIA_FOR_CONTROLS);
        section.remove("sizeOfControlGroup");
        section.remove("sizeOfControlGroupEstimationOrExact");
        section.remove("controlsPerPersonInStudyCohort");
        section.remove("exclusionCriteriaForControls");
        section.remove(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY);
        section.remove(DATA_EXTRACTION_FREQUENCY);
        section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION);
        section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
        section.remove(SPECIFY_OTHER);
        section.remove(MORE_INFO_ON_EXTRACTION_PERIODS);
        section.remove(ORDER_OF_CONTROLS_DATA_EXTRACTION);
      } else {
        JsonObject hasTheGroupRelativesFormed2 = section.getJsonObject(
          HAS_THE_GROUP_RELATIVES_FORMED_2);

        if (Objects.nonNull(hasTheGroupRelativesFormed2) && !Objects.equals(
          hasTheGroupRelativesFormed2.getString("key"),
          "a")) {
          section.remove("previouslyIssuedPermitIssuer2");
          section.remove("previouslyIssuedPermitDate2");
          section.remove("previouslyIssuedPermitValidityPeriodFrom2");
          section.remove("previouslyIssuedPermitValidityPeriodTo2");
          section.remove("previouslyIssuedPermitNumber2");
        }

        JsonObject willSameDataBeExtractedForControls = section.getJsonObject(
          WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS);
        if (Objects.nonNull(willSameDataBeExtractedForControls) && !Objects.equals(
          willSameDataBeExtractedForControls.getString("key"), "b")) {
          section.remove(VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS);
          section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION);
        }

        JsonObject willDataForControlsBeExtractedSimultaneously =
          section.getJsonObject(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY);
        if (Objects.nonNull(willDataForControlsBeExtractedSimultaneously) && !Objects.equals(
          willDataForControlsBeExtractedSimultaneously.getString("key"), "b")) {

          section.remove(DATA_EXTRACTION_FREQUENCY);
          section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION);
          section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          section.remove(SPECIFY_OTHER);
          section.remove(MORE_INFO_ON_EXTRACTION_PERIODS);
          section.remove(ORDER_OF_CONTROLS_DATA_EXTRACTION);
        } else {
          JsonObject dataExtractionFrequency =
            section.getJsonObject(DATA_EXTRACTION_FREQUENCY);
          if (Objects.nonNull(dataExtractionFrequency) && !Objects.equals(
            dataExtractionFrequency.getString("key"), "b")) {
            section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION);
            section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          }

          JsonObject dataNeedsToBeExtractedEvery =
            section.getJsonObject(DATA_NEEDS_TO_BE_EXTRACTED_EVERY);
          if (Objects.nonNull(dataNeedsToBeExtractedEvery) && (!Objects.equals(
            dataExtractionFrequency.getString("key"), "b") ||
            !Objects.equals(dataNeedsToBeExtractedEvery.getString("key"), "d"))) {
            section.remove(SPECIFY_OTHER);
          }
        }
      }

      //SECTION 6_3
      JsonObject willRelativesBeExtracted = section.getJsonObject(WILL_RELATIVES_BE_EXTRACTED);

      if (Objects.nonNull(willRelativesBeExtracted) && !Objects.equals(
        willRelativesBeExtracted.getString("key"),
        "a")) {
        section.remove(HAS_THE_GROUP_RELATIVES_FORMED_3);
        section.remove("previouslyIssuedPermitIssuer3");
        section.remove("previouslyIssuedPermitDate3");
        section.remove("previouslyIssuedPermitValidityPeriodFrom3");
        section.remove("previouslyIssuedPermitValidityPeriodTo3");
        section.remove("previouslyIssuedPermitNumber3");
        section.remove(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES);
        section.remove(VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS);
        section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES);
        section.remove(RELATIONSHIP_TO_STUDY_COHORT);
        section.remove("sizeOfRelativesGroup");
        section.remove("sizeOfRelativesGroupEstimateOrExact");
        section.remove(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY);
        section.remove(DATA_EXTRACTION_FREQUENCY_RELATIVES);
        section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES);
        section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
        section.remove(SPECIFY_OTHER_RELATIVES);
        section.remove("moreInfoOnExtractionPeriodsRelatives");
        section.remove("orderOfRelativesDataExtraction");
      } else {
        JsonObject hasTheGroupRelativesFormed3 = section.getJsonObject(
          HAS_THE_GROUP_RELATIVES_FORMED_3);

        if (Objects.nonNull(hasTheGroupRelativesFormed3) && !Objects.equals(
          hasTheGroupRelativesFormed3.getString("key"),
          "a")) {
          section.remove("previouslyIssuedPermitIssuer3");
          section.remove("previouslyIssuedPermitDate3");
          section.remove("previouslyIssuedPermitValidityPeriodFrom3");
          section.remove("previouslyIssuedPermitValidityPeriodTo3");
          section.remove("previouslyIssuedPermitNumber3");
        }

        JsonObject willSameDataBeExtractedForRelatives = section.getJsonObject(
          WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES);
        if (Objects.nonNull(willSameDataBeExtractedForRelatives) && !Objects.equals(
          willSameDataBeExtractedForRelatives.getString("key"),
          "b")) {
          section.remove(VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS);
          section.remove(TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES);
        }

        JsonObject willDataForRelativesBeExtractedSimultaneously = section.getJsonObject(
          WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY);
        if (Objects.nonNull(willDataForRelativesBeExtractedSimultaneously) && !Objects.equals(
          willDataForRelativesBeExtractedSimultaneously.getString("key"),
          "b")) {
          section.remove(DATA_EXTRACTION_FREQUENCY_RELATIVES);
          section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES);
          section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
          section.remove(SPECIFY_OTHER_RELATIVES);
        } else {
          JsonObject dataExtractionFrequencyRelatives = section.getJsonObject(
            DATA_EXTRACTION_FREQUENCY_RELATIVES);
          if (Objects.nonNull(dataExtractionFrequencyRelatives) && !Objects.equals(
            dataExtractionFrequencyRelatives.getString("key"), "b")) {
            section.remove(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES);
            section.remove(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
            section.remove(SPECIFY_OTHER_RELATIVES);
          } else {
            JsonObject dataNeedsToBeExtractedEveryRelatives = section.getJsonObject(
              DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES);
            if (Objects.nonNull(dataNeedsToBeExtractedEveryRelatives) && !Objects.equals(
              dataNeedsToBeExtractedEveryRelatives.getString("key"), "d")) {
              section.remove(SPECIFY_OTHER_RELATIVES);
            }
          }
        }
      }
    }
  }

  private void cleanSection7(JsonObject section7) {
    JsonObject willDataBeCombinedWithDataObtained = section7.getJsonObject(
      WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED);

    if (Objects.nonNull(willDataBeCombinedWithDataObtained) && !Objects.equals(
      willDataBeCombinedWithDataObtained.getString("key"), "a")) {
      section7.remove("combinedCountries");
      section7.remove("combinedDataHolders");
      section7.remove("combinedDatabaseRegistries");
      section7.remove("combinedDatasetsRegistries");
      section7.remove("provideInformationList");
    }

    JsonObject otherPermitApplications = section7.getJsonObject(OTHER_PERMIT_APPLICATIONS);
    if (Objects.nonNull(otherPermitApplications) && !Objects.equals(
      otherPermitApplications.getString("key"), "a")) {
      section7.remove("dateOfSubmittingTheApplication");
      section7.remove("applicationIssuer");
      section7.remove("identificationCode");
    }
  }

  private void cleanSection8(JsonObject section8) {
    JsonObject whenDataNeeded = section8.getJsonObject(WHEN_DATA_NEEDED);
    if (Objects.nonNull(whenDataNeeded) && !Objects.equals(whenDataNeeded.getString("key"), "b")) {
      section8.remove("ifLaterWhen");
    }

    JsonObject willTheDataBeTransferred = section8.getJsonObject(WILL_THE_DATA_BE_TRANSFERRED);
    if (Objects.nonNull(willTheDataBeTransferred) && !Objects.equals(
      willTheDataBeTransferred.getString("key"), "a")) {
      section8.remove("whichCountriesDataBeProcessed");
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47);
      section8.remove("whyWillDataBeTransferredOutsideEUArticle47Options");
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48);
      section8.remove("whyWillDataBeTransferredOutsideEUArticle48a");
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B);
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C);
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
      section8.remove("whyWillDataBeTransferredOutsideEUArticle48d");
      section8.remove("whyWillDataBeTransferredOutsideEUArticle48e");
      section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
      section8.remove("whyWillDataBeTransferredOutsideEUArticle49");
      section8.remove(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);
      section8.remove(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU_OTHER_OPTIONS);
      section8.remove("safeguardsAreProvidedByReferringGDCP");
    } else {
      boolean isWhyWillDataBeTransferredOutsideEUArticle47 =
        section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47)
          && section8.getBoolean(
          WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47, false);
      if (!isWhyWillDataBeTransferredOutsideEUArticle47) {
        section8.remove("whyWillDataBeTransferredOutsideEUArticle47Options");
      }

      boolean isWhyWillDataBeTransferredOutsideEUArticle48 =
        section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48)
          && section8.getBoolean(
          WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, false);

      if (!isWhyWillDataBeTransferredOutsideEUArticle48) {
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48a");
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B);
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C);
        section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48d");
        section8.remove("whyWillDataBeTransferredOutsideEUArticle48e");
      } else {
        boolean isWhyWillDataBeTransferredOutsideEUArticle48b =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B)
            && section8.getBoolean(
            WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48b) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT);
        }

        boolean isWhyWillDataBeTransferredOutsideEUArticle48C =
          section8.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C)
            && section8.getBoolean(
            WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false);

        if (!isWhyWillDataBeTransferredOutsideEUArticle48C) {
          section8.remove(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT);
        }
      }
      JsonObject legalBasisForTransferringTheDataOutsideEU = section8.getJsonObject(
        LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU);
      if (Objects.nonNull(legalBasisForTransferringTheDataOutsideEU) && !Objects.equals(
        legalBasisForTransferringTheDataOutsideEU.getString("key"), "c")) {
        section8.remove(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU_OTHER_OPTIONS);
      } else if (Objects.isNull(legalBasisForTransferringTheDataOutsideEU)) {
        section8.remove(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU_OTHER_OPTIONS);
      }
    }
    JsonObject isOptedOutOfTheMechanismProvidedInNationalLaw = section8.getJsonObject(
      OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW);
    if (Objects.nonNull(isOptedOutOfTheMechanismProvidedInNationalLaw) &&
      !"a".equals(isOptedOutOfTheMechanismProvidedInNationalLaw.getString("key"))) {
      section8.remove(PROVIDED_JUSTIFICATION_EXCEPTION);
    }
  }


  private String getCountryId(ConstraintViolation<SectionDTO> violation) {

    // Define the regex pattern to match "countries" followed by a number inside square brackets
    String regex = "countries\\[(\\d+)\\]";

    // Compile the pattern
    Pattern pattern = Pattern.compile(regex);

    // Create a matcher to find the number
    Matcher matcher = pattern.matcher(violation.getPropertyPath().toString());

    // Find and extract the number
    if (matcher.find()) {
      Integer countryNumber = Integer.valueOf(matcher.group(1));
      return ((Section6CountryDTO) ((java.util.ArrayList) ((Section6DTO) violation.getRootBean()).getCountries()).get(
        countryNumber)).getCountryId();
    } else {
      return null;
    }
  }

  public SectionDTO generateSectionDTO(Integer sectionNumberFromUrl, String sectionString,
    RoutingContext context) {

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      // Generate the sectionDTO class based on the provided Section Number
      Class<? extends SectionDTO> sectionDTOClass = SectionDTOBuilderFactory.createSectionDTO(
        sectionNumberFromUrl, DataAccessApplicationValidationManager.class);

      SectionDTO sectionDTO;
      if (sectionNumberFromUrl == 6) {
        List<Section6CountryDTO> countries = objectMapper.readValue(sectionString,
          new TypeReference<>() {
          });
        Section6DTO section6DTO = new Section6DTO();
        section6DTO.setCountries(countries);
        sectionDTO = section6DTO;
      } else {
        // Read JSON payload as a generic Map<String, Object>
        sectionDTO = objectMapper.readValue(sectionString, sectionDTOClass);
      }

      return sectionDTO;

    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put(RESULT_MESSAGE,
        THE_PARSING_OF_JSON_FAILED_WITH_ERROR + e.getMessage()).encode());
    }
    return null;
  }

  private SectionDTO generateSectionDTO(Integer sectionNumberFromUrl, RoutingContext context,
    String contextAsString) {
    return generateSectionDTO(sectionNumberFromUrl, contextAsString, context);
  }

  public static void buildCustomValidationErrors(List<FieldError> errors, String errorCode,
    String fieldName, List<String> params, ConstraintViolation<?> violation) {
    buildCustomValidationErrors(errors, errorCode, fieldName, params, violation, null);
  }

  public static void buildCustomValidationErrors(List<FieldError> errors, String errorCode,
    String fieldName, List<String> params, ConstraintViolation<?> violation, String countryId) {

    PathImpl listField = ((PathImpl) violation.getPropertyPath());
    Integer index = StringUtils.startsWith(listField.toString(), "countries[") ? null
      : listField.getLeafNode().getIndex();
    String nameFieldForList = listField.getLeafNode().getParent().getName();

    Map<String, Object> attributes = violation.getConstraintDescriptor()
      .getAttributes();

    if (errorCode.equals(ListObjectFieldsNotNull.class.getSimpleName())
      || errorCode.equals(ListOfAttachmentsValidation.class.getSimpleName())) {
      // create the error object
      errors.add(new FieldError(
        errorCode,
        violation.getConstraintDescriptor().getAttributes().get(MESSAGE).toString(),
        listField.getLeafNode().getName(),
        listField.getLeafNode().getParent().getName(),
        null,
        countryId,
        countryId != null ? getSection6Subsection(
          ((PathImpl) violation.getPropertyPath()).getLeafNode().getName()) : null));
    } else if (errorCode.equals(ListContainsSpecificValue.class.getSimpleName())
      || errorCode.equals(RequiredFieldBasedOnObjectValue.class.getSimpleName())
      || errorCode.equals(RequiredFieldBasedOnTwoObjectValue.class.getSimpleName())
      || errorCode.equals(RequiredFieldBasedOnAndOrObjectValue.class.getSimpleName())
      || errorCode.equals(ListContainsOneMoreValueThanASpecificValue.class.getSimpleName())
      || errorCode.equals(ListContainsOneMoreValueExpectASpecificValue.class.getSimpleName())) {
      // create the error object
      errors.add(new FieldError(
        errorCode,
        attributes.get(MESSAGE).toString(),
        index == null ? attributes.get(MANDATORY_FIELD).toString() : nameFieldForList,
        index != null ? index.toString() : null,
        null,
        countryId,
        countryId != null ? getSection6Subsection(attributes.get(MANDATORY_FIELD).toString())
          : null));
    } else if (errorCode.equals(RequiredFieldBasedOnValue.class.getSimpleName())) {
      // create the error object
      errors.add(new FieldError(
        errorCode,
        attributes.get(MESSAGE).toString(),
        attributes.get(MANDATORY_FIELD).toString(),
        null,
        null,
        countryId,
        countryId != null ? getSection6Subsection(attributes.get(MANDATORY_FIELD).toString())
          : null));
    } else if (errorCode.equals(Section8DataBeTransferredOpt48.class.getSimpleName())
      || errorCode.equals(Section8DataBeTransferred.class.getSimpleName())) {
      // create the error object
      errors.add(new FieldError(
        attributes.get(ERROR_CODE).toString(),
        attributes.get(MESSAGE).toString(),
        index == null ? fieldName : nameFieldForList, index != null ? index.toString() : null,
        null,
        countryId,
        countryId != null ? getSection6Subsection(fieldName) : null));
    } else if (errorCode.equals(AttachmentValidation.class.getSimpleName())) {
      // create the error object
      errors.add(new FieldError(
        errorCode,
        violation.getConstraintDescriptor()
          .getAttributes().get(MESSAGE).toString(),
        ((PathImpl) violation.getPropertyPath()).getLeafNode().getParent().getName(),
        null,
        null,
        countryId,
        countryId != null ? getSection6Subsection(
          ((PathImpl) violation.getPropertyPath()).getLeafNode().getName()) : null));
    } else if (errorCode.equals(ListNotContainsSpecificValue.class.getSimpleName())) {
      // create the error object
      errors.add(new FieldError(
        attributes.get(ERROR_CODE).toString(),
        attributes.get(MESSAGE).toString(),
        Objects.nonNull(fieldName) && !fieldName.isBlank() ? fieldName
          : attributes.get(MANDATORY_FIELD).toString(),
        null,
        null,
        countryId,
        countryId != null ? getSection6Subsection(fieldName) : null));
    } else if (errorCode.equals(DateHigherOrEqual.class.getSimpleName())
      || errorCode.equals(DateLowerOrEqual.class.getSimpleName())) {
      // create the error object
      if (index != null) {
        // property with validation error exists in a list
        errors.add(new FieldError(
          attributes.get(ERROR_CODE).toString(),
          attributes.get(MESSAGE).toString(),
          ((PathImpl) violation.getPropertyPath()).getLeafNode().getName(), index.toString(),
          null,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      } else {
        errors.add(new FieldError(
          attributes.get(ERROR_CODE).toString(),
          attributes.get(MESSAGE).toString(),
          fieldName,
          null,
          null,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      }
    } else if (errorCode.equals(EmailFormat.class.getSimpleName())) {
      // create the error object
      if (index != null) {
        // property with validation error exists in a list
        errors.add(new FieldError(
          attributes.get(ERROR_CODE) != null ? attributes.get(ERROR_CODE).toString() : errorCode,
          attributes.get(MESSAGE).toString(),
          ((PathImpl) violation.getPropertyPath()).getLeafNode().getName(),
          index.toString(),
          null,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      } else {
        errors.add(new FieldError(
          attributes.get(ERROR_CODE) != null ? attributes.get(ERROR_CODE).toString() : errorCode,
          attributes.get(MESSAGE).toString(),
          fieldName,
          null,
          null,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      }
    } else if (errorCode.equals("Size")) {
      // create the error object
      if (index != null) {
        // property with validation error exists in a list
        errors.add(new FieldError(
          attributes.get(ERROR_CODE) != null ? attributes.get(ERROR_CODE).toString() : errorCode,
          attributes.get(MESSAGE).toString(),
          ((PathImpl) violation.getPropertyPath()).getLeafNode().getName(),
          index.toString(),
          params,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      } else {
        errors.add(new FieldError(
          attributes.get(ERROR_CODE) != null ? attributes.get(ERROR_CODE).toString() : errorCode,
          attributes.get(MESSAGE).toString(),
          fieldName,
          null,
          params,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      }
    } else if (errorCode.equals(AlphanumericFormat.class.getSimpleName()) && index != null) {
      errors.add(new FieldError(
        errorCode,
        errorCode,
        ((PathImpl) violation.getPropertyPath()).getLeafNode().getName(), index.toString(),
        params,
        countryId,
        countryId != null ? getSection6Subsection(fieldName) : null));
    } else if (errorCode.equals(PhoneFormat.class.getSimpleName()) && violation.getMessage()
      .equals("PhoneStartsWithZero")) {
      // Handle the custom error for phone numbers starting with 0
      errors.add(new FieldError(
        violation.getMessage(),
        violation.getMessage(),
        index == null ? fieldName : nameFieldForList,
        index != null ? index.toString() : null,
        params,
        countryId,
        countryId != null ? getSection6Subsection(fieldName) : null
      ));
    } else {
      // create the error object
      // For default case the filed name for a list is the parent field name.
      errors.add(new FieldError(
        errorCode,
        errorCode,
        index == null ? fieldName : nameFieldForList,
        index != null ? index.toString() : null, params, countryId,
        countryId != null ? getSection6Subsection(fieldName) : null));
    }
  }

  static String getSection6Subsection(String fieldName) {
    if(fieldName != null && fieldName.equals("catalogIds")){
      return null;
    }
    if (fieldName != null && (fieldName.equals("country_id")
      || fieldName.equals("hdabContacts")
      || fieldName.equals("howWillTheDataFromDifferentSourcesBeLinked"))) {
      return "6.0";
    } else if (fieldName != null && (fieldName.equals(WILL_CONTROLS_BE_EXTRACTED)
      || fieldName.equals("hasTheGroupRelativesFormed2")
      || fieldName.equals(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS)
      || fieldName.equals("dataHoldersForControls")
      || fieldName.equals("databasesForControls")
      || fieldName.equals("datasetsForControls")
      || fieldName.equals(VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS)
      || fieldName.equals(TIME_PERIOD_FOR_DATA_EXTRACTION)
      || fieldName.equals(EXTRACTION_CRITERIA_FOR_CONTROLS)
      || fieldName.equals("sizeOfControlGroup")
      || fieldName.equals("sizeOfControlGroupEstimationOrExact")
      || fieldName.equals("controlsPerPersonInStudyCohort")
      || fieldName.equals("exclusionCriteriaForControls")
      || fieldName.equals("previouslyIssuedPermitIssuer2")
      || fieldName.equals("previouslyIssuedPermitDate2")
      || fieldName.equals(PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM)
      || fieldName.equals("previouslyIssuedPermitValidityPeriodTo2")
      || fieldName.equals("previouslyIssuedPermitNumber2")
      || fieldName.equals(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY)
      || fieldName.equals(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION)
      || fieldName.equals(DATA_EXTRACTION_FREQUENCY)
      || fieldName.equals(DATA_NEEDS_TO_BE_EXTRACTED_EVERY)
      || fieldName.equals(SPECIFY_OTHER)
      || fieldName.equals(MORE_INFO_ON_EXTRACTION_PERIODS)
      || fieldName.equals(ORDER_OF_CONTROLS_DATA_EXTRACTION)
      || fieldName.equals("extractionPhases"))) {
      return "6.2";
    } else if (fieldName != null && (fieldName.equals(WILL_RELATIVES_BE_EXTRACTED)
      || fieldName.equals(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES)
      || fieldName.equals("dataHoldersForRelatives")
      || fieldName.equals("databasesForRelatives")
      || fieldName.equals("datasetsForRelatives")
      || fieldName.equals(VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS)
      || fieldName.equals(TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES)
      || fieldName.equals(RELATIONSHIP_TO_STUDY_COHORT)
      || fieldName.equals("sizeOfRelativesGroup")
      || fieldName.equals("sizeOfRelativesGroupEstimateOrExact")
      || fieldName.equals(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY)
      || fieldName.equals(DATA_EXTRACTION_FREQUENCY_RELATIVES)
      || fieldName.equals(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES)
      || fieldName.equals(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES)
      || fieldName.equals(SPECIFY_OTHER_RELATIVES)
      || fieldName.equals("moreInfoOnExtractionPeriodsRelatives")
      || fieldName.equals("orderOfRelativesDataExtraction")
      || fieldName.equals("extractionPhasesRelatives")
      || fieldName.equals("hasTheGroupRelativesFormed3")
      || fieldName.equals("previouslyIssuedPermitIssuer3")
      || fieldName.equals("previouslyIssuedPermitDate3")
      || fieldName.equals("previouslyIssuedPermitValidityPeriodFrom3")
      || fieldName.equals("previouslyIssuedPermitValidityPeriodTo3")
      || fieldName.equals("previouslyIssuedPermitNumber3"))) {
      return "6.3";
    } else {
      return "6.1";
    }
  }

  public void validateUpdateTitle(RoutingContext context) {
    JsonObject requestBody = context.body().asJsonObject();

    ApplicationCommonDTO applicationCommonDTO = generateDataAccessApplicationCommonDTO(
      requestBody, context);

    Set<ConstraintViolation<ApplicationCommonDTO>> violations = validateDataAccessApplicationCommonDTO(
      applicationCommonDTO);

    if (!violations.isEmpty()) {
      List<FieldError> errors = new ArrayList<>();
      for (ConstraintViolation<ApplicationCommonDTO> violation : violations) {
        String code = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();
        String field = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
        List<String> params = new ArrayList<>();
        if (maxParam != null) {
          params.add(maxParam.toString());
        }
        FieldError error = new FieldError(code, message, field, null, params, null, null);
        errors.add(error);
      }

      JsonObject response = new JsonObject()
        .put(ERRORS, errors);

      context.response()
        .setStatusCode(400)
        .end(response.encode());
    } else {
      context.next();
    }
  }

  private ApplicationCommonDTO generateDataAccessApplicationCommonDTO(JsonObject jsonUser,
    RoutingContext context) {

    ApplicationCommonDTO applicationCommonDTO = null;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      applicationCommonDTO = objectMapper.readValue(jsonUser.toString(),
        ApplicationCommonDTO.class);
    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put(RESULT_MESSAGE,
        THE_PARSING_OF_JSON_FAILED_WITH_ERROR + e.getMessage()).encode());
    }
    return applicationCommonDTO;
  }

  private Set<ConstraintViolation<ApplicationCommonDTO>> validateDataAccessApplicationCommonDTO(
    ApplicationCommonDTO applicationCommonDTO) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    return validator.validate(applicationCommonDTO);
  }
}
