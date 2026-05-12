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
package eu.europa.data.health.access.dto.sections.data_request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.data.health.access.annotations.*;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.TabulationPlanDTO;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.NotBlankValidationGroup;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.PhoneFormatValidationGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor


@RequiredFieldBasedOnObjectValue(
    objectName = "howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "object",
    mandatoryField = "needForDataExtractionEvery",
    message = "needForDataExtractionEvery must be provided when needForDataExtractionEvery is 'multiple times'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "optOutOfTheMechanismProvidedInTheNationalLaw",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "providedJustificationException",
  message = "providedJustificationException must be provided when optOutOfTheMechanismProvidedInTheNationalLaw is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
    objectName = "needForDataExtractionEvery",
    baseField = "key",
    baseFieldValue = "d",
    mandatoryFieldType = "string",
    mandatoryField = "needForDataExtractionEveryOther",
    message = "needForDataExtractionEveryOther must be provided when needForDataExtractionEvery is 'other'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "needForDataExtractionEvery",
  baseField = "key",
  baseFieldValue = "d",
  mandatoryFieldType = "string",
  mandatoryField = "needForDataExtractionEveryDescription",
  message = "needForDataExtractionEveryDescription must be provided when needForDataExtractionEvery is 'other'"
)
@RequiredFieldBasedOnObjectValue(
    objectName = "extractionMethod",
    baseField = "key",
    baseFieldValue = "c",
    mandatoryFieldType = "string",
    mandatoryField = "samplingMethod",
    message = "samplingMethod must be provided when extractionMethod is 'other sample'"
)
@RequiredFieldBasedOnObjectValue(
    objectName = "extractionMethod",
    baseField = "key",
    baseFieldValue = "c",
    mandatoryFieldType = "string",
    mandatoryField = "sampleSize",
    message = "sampleSize must be provided when extractionMethod is 'other sample'"
)
@RequiredFieldBasedOnObjectValue(
    objectName = "extractionMethod",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "sampleSize",
    message = "sampleSize must be provided when extractionMethod is 'other sample'"
)

@GroupSequence({Section6CountryDTO.class, NotBlankValidationGroup.class,
    PhoneFormatValidationGroup.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section6CountryDTO extends SectionDTO {

  // Starting section 6.0
  @Getter
  @NotBlank
  @JsonProperty("country_id")
  String countryId;
  @NotEmpty
  @JsonProperty("catalog_ids")
  List<String> catalogIds;
  @Size(max = 2000)
  @TranslatableField
  String hdabContacts;
  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String howWillTheDataFromDifferentSourcesBeLinked;

  // Starting section 6.1
  @NotNull
  @NotBlank
  @Size(max = 250)
  @TranslatableField
  String indicateTheSizeOfTheStudyCohort;

  @NotNull
  @CheckEnumValue(fieldName = "indicateTheSizeOfTheStudyCohortEstimationOrExact", sectionNumber = "section6")
  SelectObjectDTO indicateTheSizeOfTheStudyCohortEstimationOrExact;

  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String whyNeedStudyCohortOfThisSize;

  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
      "odt"}, maxSize = 5242880, fieldName = "variablesToBeUsedInDataExtractionAttachment") // 5MB
  List<AttachmentDTO>  variablesToBeUsedInDataExtractionAttachment;

  @NotBlank
  @Size(max = 500)
  @TranslatableField
  String timePeriodOfDataExtraction;

  @NotNull
  @CheckEnumValue(fieldName = "extractionMethod", sectionNumber = "section6")
  SelectObjectDTO extractionMethod;

  @Size(max = 2000)
  @TranslatableField
  String samplingMethod;

  @Size(max = 250)
  @TranslatableField
  String sampleSize;

  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String inclusionCriteria;

  @Size(max = 2000)
  @TranslatableField
  String potentialExclusionCriteria;

  @NotNull
  @CheckEnumValue(fieldName = "onceOrMultipleTimes", sectionNumber = "commonFields")
  SelectObjectDTO howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple;

  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO optOutOfTheMechanismProvidedInTheNationalLaw;

  @TranslatableField
  @Size(max = 2000)
  String providedJustificationException;

  @CheckEnumValue(fieldName = "everyWhen", sectionNumber = "commonFields")
  SelectObjectDTO needForDataExtractionEvery;

  @Size(max = 2000)
  @TranslatableField
  String needForDataExtractionEveryOther;

  @Size(max = 2000)
  @TranslatableField
  String needForDataExtractionEveryDescription;

  @TranslatableField
  String whatIsTheFrequencyOfUpdates;

  @ListObjectFieldsNotNull(fields = {"tabulationRegisteredToBeUsed",
      "tabulationPossibleStudyCohort", "tabulationInformationOfRequiredVariables",
      "tabulationFormationVariables", "tabulationDesiredDirection", "tabulationOrderInWhichTable",
      "tabulationAnyOtherRelevant",
      "tabulationPlan"}, message = "Fields in the list must not be null", isOptional = false)
  List<TabulationPlanDTO> tabulationPlanArray;
}
