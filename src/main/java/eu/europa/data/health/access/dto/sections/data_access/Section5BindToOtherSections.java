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
import eu.europa.data.health.access.annotations.*;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@ListContainsOneMoreValueThanASpecificValue(
  listField = "section2purposeForWhichDataWillBeUsed",
  baseField = "key",
  baseFieldValues = {"a", "b", "c", "d", "f", "g", "h"}, // Provide multiple values here
  mandatoryField = "personResponsibleName",
  message = "personResponsibleName must be provided when section2purposeForWhichDataWillBeUsed list has key other than e or has more than one values e included"
)

@ListContainsOneMoreValueExpectASpecificValue(
  listField = "section2purposeForWhichDataWillBeUsed",
  baseField = "key",
  expectFieldValue = "e", //not contain Value
  baseFieldValue = "e", //not contain Value
  mandatoryField = "summaryOfPlanForUsingTheData",
  message = "summaryOfPlanForUsingTheData must be provided when section2purposeForWhichDataWillBeUsed list has key other than e or has more than one values e included"
)

@ListContainsSpecificValue(
  listField = "section2purposeForWhichDataWillBeUsed",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryField = "legalBasis",
  message = "legalBasis must be provided when section2purposeForWhichDataWillBeUsed list has at least one key as: b"
)
@ListContainsSpecificValue(
  listField = "section2purposeForWhichDataWillBeUsed",
  baseField = "key",
  baseFieldValue = "b",
  mandatoryField = "linkToTheSupportingLegalBasis",
  message = "linkToTheSupportingLegalBasis must be provided when section2purposeForWhichDataWillBeUsed list has at least one key as: b"
)
@ListContainsSpecificValue(
  listField = "section2purposeForWhichDataWillBeUsed",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "personResearchName",
  message = "personResearchName must be provided when section2purposeForWhichDataWillBeUsed list has at least one key as: e"
)
@ListContainsSpecificValue(
  listField = "section2purposeForWhichDataWillBeUsed",
  baseField = "key",
  baseFieldValue = "e",
  mandatoryField = "summaryOfResearchPlan",
  message = "summaryOfResearchPlan must be provided when section2purposeForWhichDataWillBeUsed list has at least one key as: e"
)

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section5BindToOtherSections {

  List<SelectObjectDTO> section2purposeForWhichDataWillBeUsed;
  @Size(max = 2000)
  @TranslatableField
  String legalBasis;
  @Size(max = 500)
  @TranslatableField
  String linkToTheSupportingLegalBasis;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls",
    "xlsx", "odt"}, maxSize = 5242880, fieldName = "summaryOfPlanForUsingTheData") // 5MB
  List<AttachmentDTO> summaryOfPlanForUsingTheData;
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "summaryOfResearchPlan") // 5MB
  List<AttachmentDTO> summaryOfResearchPlan;
  @NotNull
  boolean personResponsibleSameAsContactPerson;

  @Size(max = 500)
  @TranslatableField
  String personResponsibleName;

  @NotNull
  boolean personResearchSameAsContactPerson;
  @Size(max = 500)
  @TranslatableField
  String personResearchName;
}
