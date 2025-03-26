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
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.EmailFormat;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SectionDTO;
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
  baseFieldValue = "b",
  mandatoryFieldType = "string",
  mandatoryField = "naturalPersonAffiliation",
  message = "naturalPersonAffiliation must be provided when legalOrNaturalPerson is 'Natural person'"
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
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonOperatorID",
  message = "contactPersonOperatorID must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonJobTitle",
  message = "contactPersonJobTitle must be provided when legalOrNaturalPerson is 'Legal person'"
)
@RequiredFieldBasedOnObjectValue(
  objectName = "legalOrNaturalPerson",
  baseField = "key",
  baseFieldValue = "a",
  mandatoryFieldType = "string",
  mandatoryField = "contactPersonAffiliation",
  message = "contactPersonAffiliation must be provided when legalOrNaturalPerson is 'Legal person'"
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
  String legalPersonName;
  @Size(max = 2000)
  String legalPersonAddress;
  @Size(max = 50)
  String legalPersonZipCode;
  @Size(max = 500)
  String legalPersonCity;
  @Valid
  SelectObjectDTO legalPersonCountry;
  @Size(max = 500)
  String contactPersonName;
  @EmailFormat
  @Size(max = 500)
  String contactPersonEmail;
  @Valid
  PhoneNumberDTO contactPersonPhone;
  @Size(max = 250)
  String contactPersonOrganisationName;
  @Size(max = 250)
  String contactPersonBusinessID;
  @Size(max = 250)
  String contactPersonOperatorID;
  @Size(max = 500)
  String contactPersonJobTitle;
  @Size(max = 500)
  String contactPersonAffiliation;
  @Size(max = 500)
  String contactPersonRelationship;
  @Size(max = 500)
  String naturalPersonName;
  @Size(max = 2000)
  String naturalPersonAddress;
  @Size(max = 50)
  String naturalPersonZipCode;
  @Size(max = 500)
  String naturalPersonCity;
  @Valid
  SelectObjectDTO naturalPersonCountry;
  @EmailFormat
  @Size(max = 500)
  String naturalPersonEmail;
  @Valid
  PhoneNumberDTO naturalPersonPhone;
  @Size(max = 500)
  String naturalPersonJobTitle;
  @Size(max = 500)
  String naturalPersonAffiliation;
  Date legalOrNaturalPersonProfileDataDate;
  Date contactPersonProfileDataDate;
}
