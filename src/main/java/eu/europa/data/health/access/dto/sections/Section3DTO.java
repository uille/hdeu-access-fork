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
package eu.europa.data.health.access.dto.sections;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.EmailFormat;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.annotations.TranslatableField;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.NotBlankValidationGroup;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.PhoneFormatValidationGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

@RequiredFieldBasedOnObjectValue(
  objectName = "applyingForDataOnBehalfOfPublicSector",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "applyingForDataForCarryingOutTasks",
  message = "applyingForDataForCarryingOutTasks must be provided when applyingForDataOnBehalfOfPublicSector is 'Yes'"
)
// Natural person validations
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonAddress",
  message = "naturalPersonAddress must be provided when legalOrNaturalPerson is 'Natural person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonCity",
  message = "naturalPersonCity must be provided when legalOrNaturalPerson is 'Natural person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonEmail",
  message = "naturalPersonEmail must be provided when legalOrNaturalPerson is 'Natural person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonName",
  message = "naturalPersonName must be provided when legalOrNaturalPerson is 'Natural person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonZipCode",
  message = "naturalPersonZipCode must be provided when legalOrNaturalPerson is 'Natural person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "object",
  mandatoryField = "naturalPersonCountry",
  message = "naturalPersonCountry must be provided when legalOrNaturalPerson is 'Natural person'"
)

@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonEmail",
  message = "contactPersonEmail must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonName",
  message = "contactPersonName must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonRelationship",
  message = "contactPersonRelationship must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "legalPersonAddress",
  message = "legalPersonAddress must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "legalPersonCity",
  message = "legalPersonCity must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "legalPersonName",
  message = "legalPersonName must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "legalPersonZipCode",
  message = "legalPersonCity must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "object",
  mandatoryField = "legalPersonCountry",
  message = "legalPersonCountry must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "phone",
  mandatoryField = "contactPersonPhone",
  message = "contactPersonPhone must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "phone",
  mandatoryField = "naturalPersonPhone",
  message = "naturalPersonPhone must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonOrganisationName",
  message = "contactPersonOrganisationName must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonBusinessID",
  message = "contactPersonBusinessID must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonAffiliation",
  message = "naturalPersonAffiliation must be provided when legalOrNaturalPerson is 'Natural person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonJobTitle",
  message = "naturalPersonJobTitle must be provided when legalOrNaturalPerson is 'Natural person'"
)

@Data
@NoArgsConstructor
@GroupSequence({Section3DTO.class, NotBlankValidationGroup.class, PhoneFormatValidationGroup.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section3DTO extends SectionDTO {

  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO applyingForDataOnBehalfOfPublicSector;
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO applyingForDataForCarryingOutTasks;
  @NotNull
  @CheckEnumValue(fieldName = "legalOrNaturalPerson", sectionNumber = "section3")
  SelectObjectDTO legalOrNaturalPerson;
  @Size(max = 500)
  @TranslatableField
  String legalPersonName;
  @Size(max = 2000)
  @TranslatableField
  String legalPersonAddress;
  @Size(max = 50)
  String legalPersonZipCode;
  @Size(max = 500)
  @TranslatableField
  String legalPersonCity;
  @Valid
  SelectObjectDTO legalPersonCountry;
  @Size(max = 500)
  @TranslatableField
  String contactPersonName;
  @EmailFormat
  @Size(max = 500)
  String contactPersonEmail;
  @Valid
  PhoneNumberDTO contactPersonPhone;
  @Size(max = 250)
  @TranslatableField
  String contactPersonOrganisationName;
  @Size(max = 250)
  @TranslatableField
  String contactPersonBusinessID;
  @Size(max = 500)
  @TranslatableField
  String contactPersonRelationship;
  @Size(max = 2000)
  @TranslatableField
  String otherContactPerson;
  @Size(max = 500)
  @TranslatableField
  String naturalPersonName;
  @Size(max = 2000)
  @TranslatableField
  String naturalPersonAddress;
  @Size(max = 50)
  String naturalPersonZipCode;
  @Size(max = 500)
  @TranslatableField
  String naturalPersonCity;
  @Valid
  SelectObjectDTO naturalPersonCountry;
  @Size(max = 500)
  @TranslatableField
  String naturalPersonJobTitle;
  @Size(max = 500)
  @TranslatableField
  String naturalPersonAffiliation;
  @EmailFormat
  @Size(max = 500)
  String naturalPersonEmail;
  @Valid
  PhoneNumberDTO naturalPersonPhone;
  Date legalOrNaturalPersonProfileDataDate;
  Date contactPersonProfileDataDate;
}
