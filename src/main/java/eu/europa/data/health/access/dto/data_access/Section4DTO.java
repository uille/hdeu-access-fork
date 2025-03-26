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
import eu.europa.data.health.access.annotations.EmailFormat;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.NotBlankValidationGroup;
import eu.europa.data.health.access.validation_groups.PhoneNumberGroups.PhoneFormatValidationGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;


  @RequiredFieldBasedOnObjectValue(
    objectName = "isTheProjectFinanciallyCovered",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "object",
    mandatoryField = "rangeOfAmountOfFinancing",
    message = "rangeOfAmountOfFinancing must be provided when isTheProjectFinanciallyCovered is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "invoiceType",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "string",
    mandatoryField = "invoiceAddress",
    message = "invoiceAddress must be provided when invoiceType is 'Electronic'"
  )

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@GroupSequence({Section4DTO.class, NotBlankValidationGroup.class, PhoneFormatValidationGroup.class})
public class Section4DTO extends SectionDTO {

  boolean sameAsContactPerson;
  @NotBlank
  @Size(max=500)
  String fullName;
  @NotBlank
  @Size(max=2000)
  String address;
  @NotNull
  @Valid
  PhoneNumberDTO phone;
  @NotBlank
  @EmailFormat
  @Size(max = 500)
  String email;
  @NotNull
  @CheckEnumValue(fieldName = "invoiceType" , sectionNumber = "section4")
  SelectObjectDTO invoiceType;
  @NotBlank
  @Size(max=250)
  String invoiceReferenceNumber;
  @Size(max=250)
  String invoiceAddress;
  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions" , sectionNumber = "commonFields")
  SelectObjectDTO isTheProjectFinanciallyCovered;
  @Valid
  @CheckEnumValue(fieldName = "rangeOfAmountOfFinancing" , sectionNumber = "section4")
  SelectObjectDTO rangeOfAmountOfFinancing;
  Date section4ProfileDataDate;
}
