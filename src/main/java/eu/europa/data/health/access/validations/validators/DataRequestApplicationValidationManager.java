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
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.ERROR_CODE;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.MANDATORY_FIELD;
import static eu.europa.data.health.access.Constants.MESSAGE;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.PARSING_FAILED;
import static eu.europa.data.health.access.Constants.RESULT_MESSAGE;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.annotations.AttachmentValidation;
import eu.europa.data.health.access.annotations.DateHigherOrEqual;
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
import eu.europa.data.health.access.dto.ApplicationCommonDTO;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.TabulationPlanDTO;
import eu.europa.data.health.access.dto.data_request.Section3DTO;
import eu.europa.data.health.access.dto.data_request.Section6CountryDTO;
import eu.europa.data.health.access.dto.data_request.Section6DTO;
import eu.europa.data.health.access.dto.data_request.SectionDTODataRequestBuilderFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jetbrains.annotations.NotNull;

/**
 * DataRequestValidationManager class implementing all validations for the data request application
 * entity
 */
public class DataRequestApplicationValidationManager extends AbstractSchemaValidator {

  /**
   * A method validating that the json object inside the request body complies to the data request
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

    // Initialize the validator factory and validator for constraint validations
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    // Extract sectionNumber field from URL as a path variable
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

    // Validate the SectionDTO object
    Set<ConstraintViolation<SectionDTO>> violations = validator.validate(sectionDTO);
    // Use AtomicReference to track validation errors for TabulationPlanDTO (used dynamically in Section 6)
    AtomicReference<Set<ConstraintViolation<TabulationPlanDTO>>> violationsForTabulationPlan =
      new AtomicReference<>(new HashSet<>());

    // Special handling for Section 6, which has a dynamic list (countries and their tabulation plans)
    if (sectionNumberFromUrl.equals(6) && sectionDTO instanceof Section6DTO section) {
      section.getCountries().forEach(country -> {
        // Retrieve the list of TabulationPlanDTO objects for each country
        List<TabulationPlanDTO> tabulationPlanArray = country.getTabulationPlanArray();
        if (tabulationPlanArray != null) {
          // Validate each TabulationPlanDTO in the list
          for (int i = 0; i < tabulationPlanArray.size(); i++) {
            TabulationPlanDTO tabulationPlan = tabulationPlanArray.get(i);
            // Add additional metadata to the TabulationPlanDTO for validation context
            tabulationPlan.setCountryId(country.getCountryId());
            tabulationPlan.setListIndex(i);

            // Validate the TabulationPlanDTO and accumulate violations
            Set<ConstraintViolation<TabulationPlanDTO>> violationsTabulation =
              validator.validate(tabulationPlan);
            violationsForTabulationPlan.get().addAll(violationsTabulation);
          }
        }
      });
    }

    // Special handling for Section 3, which has conditional mandatory fields (phones)
    if (sectionNumberFromUrl.equals(3) && sectionDTO instanceof Section3DTO section3DTO) {
      // Retrieve the value of legalOrNaturalPerson to determine conditional logic
      String legalOrNaturalPerson = Objects.isNull(section3DTO.getLegalOrNaturalPerson()) ? null
        : section3DTO.getLegalOrNaturalPerson().getKey();

      // Remove specific phone field violations based on the type of person
      if (Objects.nonNull(legalOrNaturalPerson) && legalOrNaturalPerson.equals("b")) {
        // "Natural Person": Remove contactPersonPhone violations
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith("contactPersonPhone"));
      } else if (Objects.nonNull(legalOrNaturalPerson) && legalOrNaturalPerson.equals("a")) {
        // "Legal Person": Remove naturalPersonPhone violations
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith("naturalPersonPhone"));
      } else if (Objects.isNull(legalOrNaturalPerson)) {
        // Neither "Natural" nor "Legal Person": Remove both phone field violations
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith("contactPersonPhone"));
        violations.removeIf(
          violation -> violation.getPropertyPath().toString().startsWith("naturalPersonPhone"));
      }
    }

    // List to collect custom error objects
    List<FieldError> errors = new ArrayList<>();

    // Handle general validation errors (for the main SectionDTO)
    if (!violations.isEmpty()) {
      for (ConstraintViolation<SectionDTO> violation : violations) {
        // Extract the error code based on the constraint annotation type
        String errorCode = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();

        if (sectionNumberFromUrl == 6) {
          // For Section 6, dynamically resolve field names and additional parameters
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
          // Build custom validation error messages for Section 6
          buildCustomValidationErrors(errors, errorCode, fieldName, params,
            violation, getCountryId(violation), false);

        } else {
          // General error handling for other sections
          String fieldName = violation.getPropertyPath().toString();
          Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
          List<String> params = new ArrayList<>();
          if (maxParam != null) {
            params.add(maxParam.toString());
            // Set the field value to null in the context body for invalid fields
            context.body().asJsonObject().put(fieldName, null);
          }
          buildCustomValidationErrors(errors, errorCode, fieldName, params,
            violation);
        }
      }
      // Store the collected errors in the context
      context.put(ERRORS, new GeneralException(errors).getErrors());
    }

    // Handle validation errors for TabulationPlanDTOs (specific to Section 6)
    if (!violationsForTabulationPlan.get().isEmpty()) {
      for (ConstraintViolation<TabulationPlanDTO> violationDemo : violationsForTabulationPlan.get()) {
        // Extract error code and other details for TabulationPlanDTO violations
        String errorCode = violationDemo.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();

        if (sectionNumberFromUrl == 6) {
          String fieldName = ((PathImpl) violationDemo.getPropertyPath()).getLeafNode().getName();
          Object maxParam = violationDemo.getConstraintDescriptor().getAttributes().get("max");
          List<String> params = new ArrayList<>();
          if (maxParam != null) {
            params.add(maxParam.toString());
          }
          // Build custom validation error messages for TabulationPlanDTOs
          buildCustomValidationErrors(errors, errorCode, fieldName, params,
            violationDemo, (violationDemo.getRootBean()).getCountryId(), true);
        }
      }
      // Store the collected errors for TabulationPlanDTOs in the context
      context.put(ERRORS, new GeneralException(errors).getErrors());
    }

    // Proceed to the next handler in the routing context
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
      default:
        break;
    }
  }

  private void performCleanUpForm6(JsonArray mongoApplication) {
    cleanSection6(mongoApplication);
  }

  private void cleanSection2(JsonObject section2) {

    JsonArray theResearchFocusesOnTheFollowingObjectives = section2.getJsonArray(
      "theResearchFocusesOnTheFollowingObjectives");
    if (Objects.nonNull(theResearchFocusesOnTheFollowingObjectives) && notContainsKey(
      theResearchFocusesOnTheFollowingObjectives, "i")) {
      section2.remove("theResearchFocusesOnTheFollowingObjectivesOther");
    }

    JsonArray areaOfResearch = section2.getJsonArray("areaOfResearch");
    if (Objects.nonNull(areaOfResearch) && notContainsKey(areaOfResearch, "aj")) {
      section2.remove("areaOfResearchOther");
    }

    boolean isDescriptionChecked =
      section2.containsKey(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION)
        && section2.getBoolean(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION,
        false);

    if (!isDescriptionChecked) {
      section2.remove("descriptionOfTheProject");
    } else {
      section2.remove("descriptionOfTheDataYouWillUse");
    }

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
      section3.remove("contactPersonOperatorID");
      section3.remove("contactPersonJobTitle");
      section3.remove("contactPersonAffiliation");
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
    JsonArray purposeOfUse = section5.getJsonArray("purposeOfUse");
    if (Objects.nonNull(purposeOfUse) && notContainsKey(purposeOfUse, "b")) {
      section5.remove("legalBasis");
      section5.remove("linkToTheSupportingLegalBasis");
    }

    if (Objects.nonNull(purposeOfUse) && notContainsSelectedValueWithoutSpecificValue(
      purposeOfUse)) {
      section5.remove("summaryOfPlanForUsingTheData");
      section5.remove("summaryOfPlanForUsingTheDataLanguage");
      section5.remove("personResponsibleSameAsContactPerson");
      section5.remove("personResponsibleName");
      section5.remove("personResponsibleJobTitle");
      section5.remove("personResponsibleAffiliation");
    }

    if (Objects.nonNull(purposeOfUse) && notContainsKey(purposeOfUse, "e")) {
      section5.remove("summaryOfResearchPlan");
      section5.remove("summaryOfResearchPlanLanguage");
      section5.remove("personResearchSameAsContactPerson");
      section5.remove("personResearchName");
      section5.remove("personResearchJobTitle");
      section5.remove("personResearchAffiliation");
    }
  }

  private void cleanSection6(JsonArray section6Array) {

    for (int i = 0; i < section6Array.size(); i++) {

      //SECTION 6_1
      JsonObject section = section6Array.getJsonObject(i);

      JsonObject extractionMethod = section.getJsonObject("extractionMethod");

      if (Objects.nonNull(extractionMethod) && !extractionMethod.getString("key").equals("c")) {
        section.remove("samplingMethod");
      }

      if (Objects.nonNull(extractionMethod) && (!extractionMethod.getString("key").equals("c")
        && !extractionMethod.getString("key").equals("a"))) {
        section.remove("sampleSize");
      }

      JsonObject howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple = section.getJsonObject(
        "howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple");
      if (Objects.nonNull(howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple) && !Objects.equals(
        howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"),
        "b")) {
        section.remove(NEED_FOR_DATA_EXTRACTION_EVERY);
      }

      JsonObject needForDataExtractionEvery = section.getJsonObject(
        NEED_FOR_DATA_EXTRACTION_EVERY);
      if (Objects.nonNull(
        howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple) && !Objects.equals(
        howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple.getString("key"), "b")
        || ((Objects.nonNull(needForDataExtractionEvery) && !Objects.equals(
        needForDataExtractionEvery.getString("key"), "d")))) {
        section.remove("needForDataExtractionEveryOther");
      }
    }
  }

  //Method to check if the JsonArray doesn't contain an object with a specific key value
  private boolean notContainsKey(JsonArray jsonArray, String keyToCheck) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");
      if (keyToCheck.equals(key)) {
        return false;
      }
    }
    return true;
  }

  private boolean notContainsSelectedValueWithoutSpecificValue(JsonArray jsonArray) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");

      if (!key.equals("e")) {
        return false;
      }
    }
    return true;
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
      return ((Section6CountryDTO) ((ArrayList) ((Section6DTO) violation.getRootBean()).getCountries()).get(
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
      Class<? extends SectionDTO> sectionDTOClass = SectionDTODataRequestBuilderFactory.createSectionDTO(
        sectionNumberFromUrl);

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
        PARSING_FAILED + e.getMessage()).encode());
    }
    return null;
  }

  private SectionDTO generateSectionDTO(Integer sectionNumberFromUrl, RoutingContext context,
    String contextAsString) {
    return generateSectionDTO(sectionNumberFromUrl, contextAsString, context);
  }

  public static <T> void buildCustomValidationErrors(List<FieldError> errors, String errorCode,
    String fieldName, List<String> params, ConstraintViolation<T> violation) {
    buildCustomValidationErrors(errors, errorCode, fieldName, params, violation, null, false);
  }

  public static <T> void buildCustomValidationErrors(List<FieldError> errors, String errorCode,
    String fieldName, List<String> params, ConstraintViolation<T> violation, String countryId,
    Boolean hasTabulation) {

    PathImpl listField = ((PathImpl) violation.getPropertyPath());
    Integer index = StringUtils.startsWith(listField.toString(), "countries[") ? null
      : listField.getLeafNode().getIndex();
    String nameFieldForList = listField.getLeafNode().getParent().getName();

    // These fields are used in Section 6 to identify the specific row in the dynamic
    //list where the validation error occurred.
    Integer listIndexAsString = null;
    if (Boolean.TRUE.equals(hasTabulation)) {
      listIndexAsString =
        Objects.nonNull((violation.getRootBean()))
          ? ((TabulationPlanDTO) violation.getRootBean()).getListIndex() : null;
    }

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
    }
    else if (errorCode.equals(RequiredFieldBasedOnValue.class.getSimpleName())) {
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
    } else if (errorCode.equals(DateHigherOrEqual.class.getSimpleName())) {
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
          Objects.nonNull(listIndexAsString) ? listIndexAsString.toString() : null,
          params,
          countryId,
          countryId != null ? getSection6Subsection(fieldName) : null));
      }
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
      errors.add(new FieldError(
        errorCode,
        errorCode,
        index == null ? fieldName : nameFieldForList,
        index != null ? index.toString() : null, params, countryId,
        countryId != null ? getSection6Subsection(fieldName) : null));
    }
  }

  static String getSection6Subsection(String fieldName) {
    if (fieldName != null && (fieldName.equals("country_id")
      || fieldName.equals("hdabContacts"))) {
      return "6.0";
    } else if (fieldName != null && (fieldName.equals("willControlsBeExtracted")
      || fieldName.equals("willSameDataBeExtractedForControls")
      || fieldName.equals("regionForControlsDataExtraction")
      || fieldName.equals("dataHoldersForControls")
      || fieldName.equals("databasesForControls")
      || fieldName.equals("datasetsForControls")
      || fieldName.equals("variablesForDataExtraction")
      || fieldName.equals("timePeriodForDataExtraction")
      || fieldName.equals("extractionCriteriaForControls")
      || fieldName.equals("sizeOfControlGroup")
      || fieldName.equals("sizeOfControlGroupEstimationOrExact")
      || fieldName.equals("controlsPerPersonInStudyCohort")
      || fieldName.equals("inclusionCriteriaForControls")
      || fieldName.equals("exclusionCriteriaForControls")
      || fieldName.equals("previouslyIssuedPermitIssuer")
      || fieldName.equals("previouslyIssuedPermitDate")
      || fieldName.equals("previouslyIssuedPermitValidityPeriodFrom")
      || fieldName.equals("previouslyIssuedPermitValidityPeriodTo")
      || fieldName.equals("previouslyIssuedPermitNumber")
      || fieldName.equals("willDataForControlsBeExtractedSimultaneously")
      || fieldName.equals("dataExtractionFrequencyMultipleTimesInformation")
      || fieldName.equals("dataExtractionFrequency")
      || fieldName.equals("dataNeedsToBeExtractedEvery")
      || fieldName.equals("specifyOther")
      || fieldName.equals("moreInfoOnExtractionPeriods")
      || fieldName.equals("orderOfControlsDataExtraction")
      || fieldName.equals("extractionPhases"))) {
      return "6.2";
    } else if (fieldName != null && (fieldName.equals("willRelativesBeExtracted")
      || fieldName.equals("willSameDataBeExtractedForRelatives")
      || fieldName.equals("dataHoldersForRelatives")
      || fieldName.equals("databasesForRelatives")
      || fieldName.equals("datasetsForRelatives")
      || fieldName.equals("variablesForDataExtractionRelatives")
      || fieldName.equals("timePeriodForDataExtractionRelatives")
      || fieldName.equals("relationshipToStudyCohort")
      || fieldName.equals("previouslyIssuedPermitIssuerRelatives")
      || fieldName.equals("sizeOfRelativesGroup")
      || fieldName.equals("sizeOfRelativesGroupEstimateOrExact")
      || fieldName.equals("willDataForRelativesBeExtractedSimultaneously")
      || fieldName.equals("dataExtractionFrequencyRelatives")
      || fieldName.equals("dataNeedsToBeExtractedEveryRelatives")
      || fieldName.equals("dataExtractionFrequencyMultipleTimesInformationRelatives")
      || fieldName.equals("specifyOtherRelatives")
      || fieldName.equals("moreInfoOnExtractionPeriodsRelatives")
      || fieldName.equals("orderOfRelativesDataExtraction")
      || fieldName.equals("extractionPhasesRelatives"))) {
      return "6.3";
    } else {
      return "6.1";
    }
  }

  public void validateUpdateTitle(RoutingContext context) {
    JsonObject requestBody = context.body().asJsonObject();

    ApplicationCommonDTO dataRequestApplicationCommonDTO = generateDataRequestApplicationCommonDTO(
      requestBody, context);

    Set<ConstraintViolation<ApplicationCommonDTO>> violations = validateDataRequestApplicationCommonDTO(
      dataRequestApplicationCommonDTO);

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

  private ApplicationCommonDTO generateDataRequestApplicationCommonDTO(JsonObject jsonUser,
    RoutingContext context) {

    ApplicationCommonDTO dataRequestApplicationCommonDTO = null;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      dataRequestApplicationCommonDTO = objectMapper.readValue(jsonUser.toString(),
        ApplicationCommonDTO.class);
    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put(RESULT_MESSAGE,
        PARSING_FAILED + e.getMessage()).encode());
    }
    return dataRequestApplicationCommonDTO;
  }

  private Set<ConstraintViolation<ApplicationCommonDTO>> validateDataRequestApplicationCommonDTO(
    ApplicationCommonDTO dataRequestApplicationCommonDTO) {

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    return validator.validate(
      dataRequestApplicationCommonDTO);
  }

}
