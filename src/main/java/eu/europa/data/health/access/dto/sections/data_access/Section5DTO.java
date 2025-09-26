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
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.annotations.TranslatableField;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section5DTO extends SectionDTO {
  @NotBlank
  @Size(max = 1000)
  @TranslatableField
  String whyTheDataIsNeeded;
  @NotBlank
  @Size(max = 1000)
  @TranslatableField
  String whatIsTheAimAndTopicOfTheProject;
  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String whichAreTheExpectedBenefits;

  @NotBlank
  @Size(max = 2000)
  @TranslatableField
  String describeApplicantsQualification;

  @NotNull
  @CheckEnumValue(fieldName = "formatOfTheElectronicHealthData", sectionNumber = "section5")
  SelectObjectDTO formatOfTheElectronicHealthData;
  Boolean inOrderToApplyPseudonymisedDataA;
  Boolean inOrderToApplyPseudonymisedDataB;
  @Size(max = 2000)
  @TranslatableField
  String whyNeedForPseudonymisation;
  Date personResponsibleProfileDataDate;
  Date personResearchProfileDataDate;
}
