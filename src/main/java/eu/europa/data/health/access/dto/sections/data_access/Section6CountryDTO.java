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
package eu.europa.data.health.access.dto.sections.data_access;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.data.health.access.annotations.*;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.NotBlankValidationGroup;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.PhoneFormatValidationGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor

// Starting section 6.1
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants",
  message = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants must be provided when howIsTheStudyCohortFormed is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "c",
  mandatoryFieldType = "object",
  mandatoryField = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants",
  message = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants must be provided when howIsTheStudyCohortFormed is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "doesTheInformedConsentCoverTheRequestedRegistryExtractions",
  message = "doesTheInformedConsentCoverTheRequestedRegistryExtractions must be provided when hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "listOfAttachment",
  mandatoryField = "consentAndInformationLetterAttachment",
  message = "consentAndInformationLetterAttachment must be provided when hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants is 'Yes'"
)
@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "boolean",
  mandatoryField = "confirmThatDataPermitHasBeenGrantedForTheResearchProject",
  message = "confirmThatDataPermitHasBeenGrantedForTheResearchProject must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)
@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "listOfAttachment",
  mandatoryField = "permitDecisionAttachment",
  message = "permitDecisionAttachment must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "howTheStudyCohortWasObtained",
  message = "howTheStudyCohortWasObtained must be provided when hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "providedInformationOfTheDataUseToCorrespondingSubjects",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "howProvidedInformationOfTheDataUseToCorrespondingSubjects",
  message = "howProvidedInformationOfTheDataUseToCorrespondingSubjects must be provided when providedInformationOfTheDataUseToCorrespondingSubjects is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "providedInformationOfTheDataUseToCorrespondingSubjects",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects",
  message = "whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects must be provided when providedInformationOfTheDataUseToCorrespondingSubjects is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "informationProviderName",
  message = "informationProviderName must be provided when informationProviderName is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "c",
  mandatoryFieldType = "string",
  mandatoryField = "informationProviderName",
  message = "informationProviderName must be provided when informationProviderName is '3'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "informationProviderEmail",
  message = "informationProviderEmail must be provided when informationProviderEmail is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "c",
  mandatoryFieldType = "string",
  mandatoryField = "informationProviderEmail",
  message = "informationProviderEmail must be provided when informationProviderEmail is '3'"
)
//has problem
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "phone",
  mandatoryField = "informationProviderPhone",
  message = "informationProviderPhone must be provided when informationProviderPhone is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "c",
  mandatoryFieldType = "phone",
  mandatoryField = "informationProviderPhone",
  message = "informationProviderPhone must be provided when informationProviderPhone is '3'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "d",
  mandatoryFieldType = "string",
  mandatoryField = "whyNeedDataOfaWholePopulation",
  message = "whyNeedDataOfaWholePopulation must be provided when whyNeedDataOfaWholePopulation is '4'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "howOftenDoesTheDataNeedToBeExtracted",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "needForDataExtractionEvery",
  message = "needForDataExtractionEvery must be provided when howOftenDoesTheDataNeedToBeExtracted is 'multiple times'"
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
//Starting section 6.2
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "willSameDataBeExtractedForControls",
  message = "willSameDataBeExtractedForControls must be provided when willControlsBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willSameDataBeExtractedForControls",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "timePeriodForDataExtraction",
  message = "timePeriodForDataExtraction must be provided when willSameDataBeExtractedForControls is 'No'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "extractionCriteriaForControls",
  message = "extractionCriteriaForControls must be provided when willControlsBeExtracted is 'No'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "sizeOfControlGroup",
  message = "sizeOfControlGroup must be provided when willControlsBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "sizeOfControlGroupEstimationOrExact",
  message = "sizeOfControlGroupEstimationOrExact must be provided when willControlsBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "controlsPerPersonInStudyCohort",
  message = "controlsPerPersonInStudyCohort must be provided when willControlsBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "willDataForControlsBeExtractedSimultaneously",
  message = "willDataForControlsBeExtractedSimultaneously must be provided when willControlsBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willDataForControlsBeExtractedSimultaneously",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "dataExtractionFrequency",
  message = "dataExtractionFrequency must be provided when willDataForControlsBeExtractedSimultaneously is 'No'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "dataExtractionFrequency",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "dataExtractionFrequencyMultipleTimesInformation",
  message = "dataExtractionFrequencyMultipleTimesInformation must be provided when howIsTheStudyCohortFormed is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "dataExtractionFrequency",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "dataNeedsToBeExtractedEvery",
  message = "dataNeedsToBeExtractedEvery must be provided when howIsTheStudyCohortFormed is '2'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "dataNeedsToBeExtractedEvery",
  baseField = "key",
  baseFieldValue = "d",
  mandatoryFieldType = "string",
  mandatoryField = "specifyOther",
  message = "specifyOther must be provided when dataNeedsToBeExtractedEvery is 'Other'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willDataForControlsBeExtractedSimultaneously",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "moreInfoOnExtractionPeriods",
  message = "moreInfoOnExtractionPeriods must be provided when willDataForControlsBeExtractedSimultaneously is 'No'"
)
// Section 6.3
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "willSameDataBeExtractedForRelatives",
  message = "willSameDataBeExtractedForRelatives must be provided when willRelativesBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willSameDataBeExtractedForRelatives",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "timePeriodForDataExtractionRelatives",
  message = "timePeriodForDataExtractionRelatives must be provided when willSameDataBeExtractedForRelatives is 'No'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "relationshipToStudyCohort",
  message = "relationshipToStudyCohort must be provided when willRelativesBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "sizeOfRelativesGroup",
  message = "sizeOfRelativesGroup must be provided when willRelativesBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "sizeOfRelativesGroupEstimateOrExact",
  message = "sizeOfRelativesGroupEstimateOrExact must be provided when willRelativesBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "willDataForRelativesBeExtractedSimultaneously",
  message = "willDataForRelativesBeExtractedSimultaneously must be provided when willRelativesBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willDataForRelativesBeExtractedSimultaneously",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "dataExtractionFrequencyRelatives",
  message = "dataExtractionFrequencyRelatives must be provided when willDataForRelativesBeExtractedSimultaneously is 'No'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "dataExtractionFrequencyRelatives",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "dataNeedsToBeExtractedEveryRelatives",
  message = "dataNeedsToBeExtractedEveryRelatives must be provided when willDataForRelativesBeExtractedSimultaneously is 'Multiple times'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "dataExtractionFrequencyRelatives",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "dataExtractionFrequencyMultipleTimesInformationRelatives",
  message = "dataExtractionFrequencyMultipleTimesInformationRelatives must be provided when willDataForRelativesBeExtractedSimultaneously is 'Multiple times'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "dataNeedsToBeExtractedEveryRelatives",
  baseField = "key",
  baseFieldValue = "d",
  mandatoryFieldType = "string",
  mandatoryField = "specifyOtherRelatives",
  message = "specifyOtherRelatives must be provided when willDataForRelativesBeExtractedSimultaneously is 'Multiple times'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "moreInfoOnExtractionPeriodsRelatives",
  message = "moreInfoOnExtractionPeriodsRelatives must be provided when willRelativesBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willRelativesBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "hasTheGroupRelativesFormed3",
  message = "hasTheGroupRelativesFormed3 must be provided when hasTheGroupRelativesFormed3 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed3",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitIssuer3",
  message = "previouslyIssuedPermitIssuer3 must be provided when hasTheGroupRelativesFormed3 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed3",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitDate3",
  message = "previouslyIssuedPermitDate3 must be provided when hasTheGroupRelativesFormed3 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed3",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitValidityPeriodFrom3",
  message = "previouslyIssuedPermitValidityPeriodFrom3 must be provided when hasTheGroupRelativesFormed3 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed3",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitValidityPeriodTo3",
  message = "previouslyIssuedPermitValidityPeriodTo3 must be provided when hasTheGroupRelativesFormed3 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed3",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitNumber3",
  message = "previouslyIssuedPermitNumber3 must be provided when hasTheGroupRelativesFormed3 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "willControlsBeExtracted",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "hasTheGroupRelativesFormed2",
  message = "hasTheGroupRelativesFormed2 must be provided when willControlsBeExtracted is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed2",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitIssuer2",
  message = "previouslyIssuedPermitIssuer2 must be provided when hasTheGroupRelativesFormed2 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed2",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitDate2",
  message = "previouslyIssuedPermitDate2 must be provided when hasTheGroupRelativesFormed2 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed2",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitValidityPeriodFrom2",
  message = "previouslyIssuedPermitValidityPeriodFrom2 must be provided when hasTheGroupRelativesFormed2 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed2",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitValidityPeriodTo2",
  message = "previouslyIssuedPermitValidityPeriodTo2 must be provided when hasTheGroupRelativesFormed2 is 'Yes'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "hasTheGroupRelativesFormed2",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitNumber2",
  message = "previouslyIssuedPermitNumber2 must be provided when hasTheGroupRelativesFormed2 is 'Yes'"
)

@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitIssuer1",
  message = "previouslyIssuedPermitIssuer1 must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)
@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitDate1",
  message = "previouslyIssuedPermitDate1 must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)
@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitValidityPeriodFrom1",
  message = "previouslyIssuedPermitValidityPeriodFrom1 must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)
@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitValidityPeriodTo1",
  message = "previouslyIssuedPermitValidityPeriodTo1 must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)
@RequiredFieldBasedOnAndOrObjectValue(
  objectName = "howIsTheStudyCohortFormed",
  baseField = "key",
  baseFieldValue = "b",
  objectNameWithOr = "howIsTheStudyCohortFormed",
  baseFieldWithOr = "key",
  baseFieldValueWithOr = "c",
  objectNameWithAnd ="hasTheCohortBeenBasedOnADataPermit",
  baseFieldWithAnd = "key",
  baseFieldValueWithAnd = "a",
  mandatoryFieldType = "string",
  mandatoryField = "previouslyIssuedPermitNumber1",
  message = "previouslyIssuedPermitNumber1 must be provided when howIsTheStudyCohortFormed is 'b' or 'c' and hasTheCohortBeenBasedOnADataPermit is 'yes'"
)


@DateHigherOrEqual(field = "previouslyIssuedPermitValidityPeriodTo1", field2 = "previouslyIssuedPermitValidityPeriodFrom1",
  message = "Date must be higher than starting day", errorCode = "mustBeHigherOfStartingDay")
@DateHigherOrEqual(field = "previouslyIssuedPermitValidityPeriodTo2", field2 = "previouslyIssuedPermitValidityPeriodFrom2",
  message = "Date must be higher than starting day", errorCode = "mustBeHigherOfStartingDay")
@DateHigherOrEqual(field = "previouslyIssuedPermitValidityPeriodTo3", field2 = "previouslyIssuedPermitValidityPeriodFrom3",
  message = "Date must be higher than starting day", errorCode = "mustBeHigherOfStartingDay")

@GroupSequence({Section6CountryDTO.class, NotBlankValidationGroup.class,
  PhoneFormatValidationGroup.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section6CountryDTO extends SectionDTO {

  // Starting section 6.0
  @Getter
  @NotBlank
  @JsonProperty("country_id")
  String countryId;
  @JsonProperty("catalog_ids")
  @NotEmpty
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
  @CheckEnumValue(fieldName = "howIsTheStudyCohortFormed", sectionNumber = "section6")
  SelectObjectDTO howIsTheStudyCohortFormed;

  @Size(max = 2000)
  @TranslatableField
  String detailsOnHowAndWhatLegalDocumentsTheStudyCohortHasBeenFormed;

  //field 3
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants;

  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO doesTheInformedConsentCoverTheRequestedRegistryExtractions;
  //field 5
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "consentAndInformationLetterAttachment") // 5MB
    List<AttachmentDTO> consentAndInformationLetterAttachment;

  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO hasTheCohortBeenBasedOnADataPermit;

  Boolean confirmThatDataPermitHasBeenGrantedForTheResearchProject;

  @Size(max = 500)
  @TranslatableField
  String previouslyIssuedPermitIssuer1;
  String previouslyIssuedPermitDate1;
  String previouslyIssuedPermitValidityPeriodFrom1;
  String previouslyIssuedPermitValidityPeriodTo1;
  @AlphanumericFormat
  @Size(max = 500)
  @TranslatableField
  String previouslyIssuedPermitNumber1;

  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx", "odt"},
    maxSize = 5242880, fieldName = "permitDecisionAttachment") // 5MB
  List<AttachmentDTO> permitDecisionAttachment;
  @Size(max = 2000) //field 9
  @TranslatableField
  String howTheStudyCohortWasObtained;
  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO providedInformationOfTheDataUseToCorrespondingSubjects;
  @Size(max = 500)
  @TranslatableField
  String howProvidedInformationOfTheDataUseToCorrespondingSubjects;
  @Size(max = 500)
  @TranslatableField
  String whyNotProvidedInformationOfTheDataUseToCorrespondingSubjects;
  @Size(max = 500)
  @TranslatableField
  String informationProviderName;
  @EmailFormat
  @Size(max = 500)
  String informationProviderEmail;
  @Valid
  PhoneNumberDTO informationProviderPhone;
  @Size(max = 2000)
  @TranslatableField
  String whyNeedDataOfaWholePopulation;
  @NotBlank
  @Size(max = 250)
  @TranslatableField
  String sizeOfTheStudyCohort;
  @NotNull
  @CheckEnumValue(fieldName = "sizeOfTheStudyCohortEstimationOrExact", sectionNumber = "section6")
  SelectObjectDTO sizeOfTheStudyCohortEstimationOrExact;
  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String whyNeedStudyCohortOfThisSize;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "ethicalReviewAttachment") // 5MB
  List<AttachmentDTO> ethicalReviewAttachment;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "variablesToBeUsedInDataExtractionAttachment") // 5MB
  List<AttachmentDTO> variablesToBeUsedInDataExtractionAttachment;
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
  SelectObjectDTO howOftenDoesTheDataNeedToBeExtracted;
  @CheckEnumValue(fieldName = "everyWhen", sectionNumber = "commonFields")
  SelectObjectDTO needForDataExtractionEvery;
  @Size(max = 2000)
  @TranslatableField
  String needForDataExtractionEveryOther;
  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String needForDataExtractionEveryDescription;
  @Size(max = 2000)
  @TranslatableField
  String orderForDataExtraction;

  //Starting Section 6.2
  @NotNull //Field 17 on T.0.1.8 - 6.2
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willControlsBeExtracted;

  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO hasTheGroupRelativesFormed2;

  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willSameDataBeExtractedForControls;

  List<String> dataHoldersForControls;

  List<String> databasesForControls;

  List<String> datasetsForControls;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "variablesForDataExtractionAttachments") // 5MB
  List<AttachmentDTO> variablesForDataExtractionAttachments;
  @Size(max = 500) //field 10
  @TranslatableField
  String timePeriodForDataExtraction;
  @Size(max = 500) //field 9
  @TranslatableField
  String extractionCriteriaForControls;
  @Size(max = 500)
  @TranslatableField
  String sizeOfControlGroup;

  @CheckEnumValue(fieldName = "sizeOfControlGroupEstimationOrExact", sectionNumber = "section6")
  SelectObjectDTO sizeOfControlGroupEstimationOrExact;
  @Size(max = 2000)
  @TranslatableField
  String controlsPerPersonInStudyCohort;
  @Size(max = 2000) // Field 15
  @TranslatableField
  String exclusionCriteriaForControls;
  @Size(max = 500)
  @TranslatableField
  String previouslyIssuedPermitIssuer2;
  String previouslyIssuedPermitDate2;

  String previouslyIssuedPermitValidityPeriodFrom2;
  String previouslyIssuedPermitValidityPeriodTo2;

  @AlphanumericFormat
  @Size(max = 500)
  @TranslatableField
  String previouslyIssuedPermitNumber2;

  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willDataForControlsBeExtractedSimultaneously;

  //field 18
  @CheckEnumValue(fieldName = "onceOrMultipleTimes", sectionNumber = "commonFields")
  SelectObjectDTO dataExtractionFrequency;

  @Size(max = 500)// field 18.1
  @TranslatableField
  String dataExtractionFrequencyMultipleTimesInformation;

  // field 20
  @CheckEnumValue(fieldName = "everyWhen", sectionNumber = "commonFields")
  SelectObjectDTO dataNeedsToBeExtractedEvery;

  @Size(max = 2000) //field 21
  @TranslatableField
  String specifyOther;
  @Size(max = 2000) //field 22
  @TranslatableField
  String moreInfoOnExtractionPeriods;
  @Size(max = 2000) //field 23
  @TranslatableField
  String orderOfControlsDataExtraction;

  //Starting Section 6.3
  @NotNull //field 1
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willRelativesBeExtracted;

  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO hasTheGroupRelativesFormed3;

  @Size(max = 500)
  @TranslatableField
  String previouslyIssuedPermitIssuer3;
  String previouslyIssuedPermitDate3;

  String previouslyIssuedPermitValidityPeriodFrom3;
  String previouslyIssuedPermitValidityPeriodTo3;

  @AlphanumericFormat
  @Size(max = 500)
  @TranslatableField
  String previouslyIssuedPermitNumber3;

  //field 2
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willSameDataBeExtractedForRelatives;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "variablesForDataExtractionRelativesAttachments") // 5MB
  List<AttachmentDTO> variablesForDataExtractionRelativesAttachments;
  @Size(max = 2000) //field 7
  @TranslatableField
  String timePeriodForDataExtractionRelatives;
  @Size(max = 2000) //field 8
  @TranslatableField
  String relationshipToStudyCohort;

  @Size(max = 500)// field 10
  @TranslatableField
  String sizeOfRelativesGroup;
  //field 11
  @CheckEnumValue(fieldName = "sizeOfRelativesGroupEstimateOrExact", sectionNumber = "section6")
  SelectObjectDTO sizeOfRelativesGroupEstimateOrExact;
  //field 12
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willDataForRelativesBeExtractedSimultaneously;
  //field 13
  @CheckEnumValue(fieldName = "onceOrMultipleTimes", sectionNumber = "commonFields")
  SelectObjectDTO dataExtractionFrequencyRelatives;
  //field 13.1
  @CheckEnumValue(fieldName = "everyWhen", sectionNumber = "commonFields")
  SelectObjectDTO dataNeedsToBeExtractedEveryRelatives;
  //field 14
  @Size(max = 500)
  @TranslatableField
  String dataExtractionFrequencyMultipleTimesInformationRelatives;
  @Size(max = 2000) //field 15
  @TranslatableField
  String specifyOtherRelatives;
  @Size(max = 500) //field 16
  @TranslatableField
  String moreInfoOnExtractionPeriodsRelatives;
  @Size(max = 500) //field 17
  @TranslatableField
  String orderOfRelativesDataExtraction;

  Date personProfileDataDate;
}
