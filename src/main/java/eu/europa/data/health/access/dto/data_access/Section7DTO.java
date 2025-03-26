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
import eu.europa.data.health.access.annotations.AlphanumericFormat;
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.ListObjectFieldsNotNull;
import eu.europa.data.health.access.annotations.ListOfAttachmentsValidation;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.PermitDocumentDTO;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

  @RequiredFieldBasedOnObjectValue(
    objectName = "willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "combinedCountries",
    message = "combinedCountries must be provided when willDataBeCombinedWithDataObtained is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "combinedDataHolders",
    message = "combinedDataHolders must be provided when willDataBeCombinedWithDataObtained is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "combinedDatabaseRegistries",
    message = "combinedDatabaseRegistries must be provided when willDataBeCombinedWithDataObtained is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "combinedDatasetsRegistries",
    message = "combinedDatasetsRegistries must be provided when willDataBeCombinedWithDataObtained is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "providedInformationOnDataCombined",
    message = "providedInformationOnDataCombined must be provided when willDataBeCombinedWithDataObtained is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "otherPermitApplications",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "dateOfSubmittingTheApplication",
    message = "dateOfSubmittingTheApplication must be provided when otherPermitApplications is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "otherPermitApplications",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "applicationIssuer",
    message = "applicationIssuer must be provided when otherPermitApplications is 'Yes'"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "otherPermitApplications",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "identificationCode",
    message = "identificationCode must be provided when otherPermitApplications is 'Yes'"
  )

@JsonIgnoreProperties(ignoreUnknown = true)
public class Section7DTO extends SectionDTO {

  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions" , sectionNumber = "commonFields")
  SelectObjectDTO willDataBeCombinedWithDataObtained;
  @Size(max = 500)
  String combinedCountries;
  @Size(max = 500)
  String combinedDataHolders;
  @Size(max = 500)
  String combinedDatabaseRegistries;
  @Size(max = 500)
  String combinedDatasetsRegistries;
  @Size(max = 500)
  String providedInformationOnDataCombined;
  @ListOfAttachmentsValidation(allowedExtensions = { "doc", "docx", "pdf", "xls", "xlsx", "odt" }, maxSize = 5242880, fieldName = "attachmentOfPermitDocuments") // 5MB
    List<AttachmentDTO> attachmentOfPermitDocuments;
  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions" , sectionNumber = "commonFields")
  SelectObjectDTO otherPermitApplications;
  String dateOfSubmittingTheApplication;
  @Size(max = 500)
  String applicationIssuer;
  @AlphanumericFormat
  @Size(max = 500)
  String identificationCode;
  @Valid
  @ListObjectFieldsNotNull(fields = { "permitIssuer", "permitStartDateOfIssue", "permitEndDateOfIssue", "permitIdentificationInformation" }, message = "Fields in the list must not be null", isOptional=true)
  List<PermitDocumentDTO> otherDataPermits;
}
