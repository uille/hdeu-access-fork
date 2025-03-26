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
package eu.europa.data.health.access.dto.data_access;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.ListContainsOneMoreValueExpectASpecificValue;
import eu.europa.data.health.access.annotations.ListContainsOneMoreValueThanASpecificValue;
import eu.europa.data.health.access.annotations.ListContainsSpecificValue;
import eu.europa.data.health.access.annotations.ListOfAttachmentsValidation;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryField = "legalBasis",
  message = "legalBasis must be provided when purposeOfUse list has at least one key as: b"
)
@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryField = "linkToTheSupportingLegalBasis",
  message = "linkToTheSupportingLegalBasis must be provided when purposeOfUse list has at least one key as: b"
)
@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "personResearchName",
  message = "personResearchName must be provided when purposeOfUse list has at least one key as: e"
)
@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "personResearchJobTitle",
  message = "personResearchJobTitle must be provided when purposeOfUse list has at least one key as: e"
)
@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "personResearchAffiliation",
  message = "personResearchAffiliation must be provided when purposeOfUse list has at least one key as: e"
)
@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "summaryOfResearchPlan",
  message = "summaryOfResearchPlan must be provided when purposeOfUse list has at least one key as: e"
)
@ListContainsSpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "summaryOfResearchPlanLanguage",
  message = "summaryOfResearchPlanLanguage must be provided when purposeOfUse list has at least one key as: e"
)

@RequiredFieldBasedOnObjectValue(
  objectName = "formatOfTheElectronicHealthData",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "boolean",
  mandatoryField = "inOrderToApplyPseudonymisedDataA",
  message = "inOrderToApplyPseudonymisedDataA must be provided when formatOfTheElectronicHealthData list has at least one key as: b"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "formatOfTheElectronicHealthData",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "boolean",
  mandatoryField = "inOrderToApplyPseudonymisedDataB",
  message = "inOrderToApplyPseudonymisedDataB must be provided when formatOfTheElectronicHealthData list has at least one key as: b"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "formatOfTheElectronicHealthData",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "String",
  mandatoryField = "whyNeedForPseudonymisation",
  message = "whyNeedForPseudonymisation must be provided when formatOfTheElectronicHealthData list has at least one key as: b"
)

@ListContainsOneMoreValueThanASpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValues = {"a", "b", "c", "d", "f", "g", "h"}, // Provide multiple values here
  mandatoryField = "personResponsibleName",
  message = "personResponsibleName must be provided when purposeOfUse list has key other than e or has more than one values e included"
)
@ListContainsOneMoreValueThanASpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValues = {"a", "b", "c", "d", "f", "g", "h"}, // Provide multiple values here
  mandatoryField = "personResponsibleJobTitle",
  message = "personResponsibleJobTitle must be provided when purposeOfUse list has key other than e or has more than one values e included"
)
@ListContainsOneMoreValueThanASpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  baseFieldValues = {"a", "b", "c", "d", "f", "g", "h"}, // Provide multiple values here
  mandatoryField = "personResponsibleAffiliation",
  message = "personResponsibleAffiliation must be provided when purposeOfUse list has key other than e or has more than one values e included"
)

@ListContainsOneMoreValueExpectASpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  expectFieldValue = "e", //not contain Value
  baseFieldValue = "e", //not contain Value
  mandatoryField = "summaryOfPlanForUsingTheData",
  message = "summaryOfPlanForUsingTheData must be provided when purposeOfUse list has key other than e or has more than one values e included"
)

@ListContainsOneMoreValueExpectASpecificValue(
  listField = "purposeOfUse",
  baseField = "key",
  expectFieldValue = "e", //not contain Value
  baseFieldValue = "e", //not contain Value
  mandatoryField = "summaryOfPlanForUsingTheDataLanguage",
  message = "summaryOfPlanForUsingTheDataLanguage must be provided when purposeOfUse list has key other than e or has more than one values e included"
)

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section5DTO extends SectionDTO {

  @NotNull
  @NotEmpty
  @CheckEnumValue(fieldName = "purposeOfUse", sectionNumber = "section5")
  List<SelectObjectDTO> purposeOfUse;
  @NotBlank
  @Size(max = 1000)
  String whyTheDataIsNeeded;
  @NotBlank
  @Size(max = 1000)
  String whatIsTheAimAndTopicOfTheProject;
  @NotBlank
  @Size(max = 2000)
  String whichAreTheExpectedBenefits;
  @Size(max = 2000)
  String legalBasis;
  @Size(max = 500)
  String linkToTheSupportingLegalBasis;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls",
    "xlsx", "odt"}, maxSize = 5242880, fieldName = "summaryOfPlanForUsingTheData") // 5MB
  List<AttachmentDTO> summaryOfPlanForUsingTheData;
  @CheckEnumValue(fieldName = "summaryOfPlanForUsingTheDataLanguage", sectionNumber = "section5")
  SelectObjectDTO summaryOfPlanForUsingTheDataLanguage;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "summaryOfResearchPlan") // 5MB
  List<AttachmentDTO> summaryOfResearchPlan;
  @CheckEnumValue(fieldName = "summaryOfResearchPlanLanguage", sectionNumber = "section5")
  SelectObjectDTO summaryOfResearchPlanLanguage;
  @NotNull
  boolean personResponsibleSameAsContactPerson;

  @Size(max = 500)
  String personResponsibleName;
  @Size(max = 500)
  String personResponsibleJobTitle;
  @Size(max = 500)
  String personResponsibleAffiliation;

  @NotNull
  boolean personResearchSameAsContactPerson;
  @Size(max = 500)
  String personResearchName;
  @Size(max = 500)
  String personResearchJobTitle;
  @Size(max = 500)
  String personResearchAffiliation;
  @NotNull
  @CheckEnumValue(fieldName = "formatOfTheElectronicHealthData", sectionNumber = "section5")
  SelectObjectDTO formatOfTheElectronicHealthData;
  Boolean inOrderToApplyPseudonymisedDataA;
  Boolean inOrderToApplyPseudonymisedDataB;
  @Size(max = 2000)
  String whyNeedForPseudonymisation;
  Date personResponsibleProfileDataDate;
  Date personResearchProfileDataDate;
}
