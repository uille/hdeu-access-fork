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
package eu.europa.data.health.access.dto.data_request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.ListObjectFieldsNotNull;
import eu.europa.data.health.access.annotations.ListOfAttachmentsValidation;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.TabulationPlanDTO;
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
    objectName = "needForDataExtractionEvery",
    baseField = "key",
    baseFieldValue = "d",
    mandatoryFieldType = "string",
    mandatoryField = "needForDataExtractionEveryOther",
    message = "needForDataExtractionEveryOther must be provided when needForDataExtractionEvery is 'other'"
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
  String hdabContacts;

  // Starting section 6.1
  @NotNull
  @NotBlank
  @Size(max = 250)
  String indicateTheSizeOfTheStudyCohort;

  @NotNull
  @CheckEnumValue(fieldName = "indicateTheSizeOfTheStudyCohortEstimationOrExact", sectionNumber = "section6")
  SelectObjectDTO indicateTheSizeOfTheStudyCohortEstimationOrExact;

  @NotBlank
  @Size(max = 2000)
  String whyNeedStudyCohortOfThisSize;

  @Size(max = 2000)
  String regionsSeekForData;

  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "ethicalReviewAttachment") // 5MB
  List<AttachmentDTO> ethicalReviewAttachment;

  @Size(max = 2000)
  String ethicalReviewInput;

  @Size(max = 2000)
  String variablesToBeUsedInDataExtraction;

  @NotBlank
  @Size(max = 500)
  String timePeriodOfDataExtraction;

  @NotNull
  @CheckEnumValue(fieldName = "extractionMethod", sectionNumber = "section6")
  SelectObjectDTO extractionMethod;

  @Size(max = 2000)
  String samplingMethod;

  @Size(max = 250)
  String sampleSize;

  @NotBlank
  @Size(max = 2000)
  String inclusionCriteria;

  @Size(max = 2000)
  String potentialExclusionCriteria;

  @NotNull
  @CheckEnumValue(fieldName = "onceOrMultipleTimes", sectionNumber = "commonFields")
  SelectObjectDTO howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple;

  @CheckEnumValue(fieldName = "everyWhen", sectionNumber = "commonFields")
  SelectObjectDTO needForDataExtractionEvery;

  @Size(max = 2000)
  String needForDataExtractionEveryOther;

  @NotBlank
  @Size(max = 2000)
  String needForDataExtractionEveryDescription;

  String whatIsTheFrequencyOfUpdates;

  @ListObjectFieldsNotNull(fields = {"tabulationRegisteredToBeUsed",
    "tabulationPossibleStudyCohort", "tabulationInformationOfRequiredVariables",
    "tabulationFormationVariables", "tabulationDesiredDirection", "tabulationOrderInWhichTable",
    "tabulationAnyOtherRelevant",
    "tabulationPlan"}, message = "Fields in the list must not be null", isOptional = false)
  List<TabulationPlanDTO> tabulationPlanArray;
}
