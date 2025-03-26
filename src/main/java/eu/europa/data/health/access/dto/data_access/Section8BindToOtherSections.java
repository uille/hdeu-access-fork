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
import eu.europa.data.health.access.annotations.DateHigherOrEqual;
import eu.europa.data.health.access.annotations.ListContainsSpecificValue;
import eu.europa.data.health.access.annotations.ListOfAttachmentsValidation;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnAndOrObjectValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnTwoObjectValue;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

  @RequiredFieldBasedOnTwoObjectValue(
    objectName = "section7willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    objectName2 = "section3applyingForDataOnBehalfOfPublicSector",
    baseField2 = "key",
    baseFieldValue2 = "a",
    mandatoryFieldType = "objectList",
    mandatoryField = "legalBasisForProcessingCombinedApplicationData",
    message = "legalBasisForProcessingCombinedApplicationData must be provided when section 7 field #2 of T.01.10 = Yes and field #2 section T.01.3 = Yes"
  )
  @RequiredFieldBasedOnTwoObjectValue(
    objectName = "section7willDataBeCombinedWithDataObtained",
    baseField = "key",
    baseFieldValue = "a",
    objectName2 = "section3applyingForDataOnBehalfOfPublicSector",
    baseField2 = "key",
    baseFieldValue2 = "b",
    mandatoryFieldType = "objectList",
    mandatoryField = "legalBasisForProcessingCombinedData",
    message = "legalBasisForProcessingCombinedData must be provided when section 7 field #2 of T.01.10 = Yes and field #2 section T.01.3 = No"
  )
  @RequiredFieldBasedOnTwoObjectValue(
    objectName = "section5formatOfTheElectronicHealthData",
    baseField = "key",
    baseFieldValue = "b",
    objectName2 = "section3applyingForDataOnBehalfOfPublicSector",
    baseField2 = "key",
    baseFieldValue2 = "a",
    mandatoryFieldType = "objectList",
    mandatoryField = "europeanUnionInstitution",
    message = "europeanUnionInstitution must be provided when section 5 field #19 of T.01.5 = Pseudonymised and field #2 section T.01.3 = Yes"
  )

  @RequiredFieldBasedOnAndOrObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "a",
    objectNameWithOr = "section3legalOrNaturalPerson",
    baseFieldWithOr = "key",
    baseFieldValueWithOr = "b",
    objectNameWithAnd ="section5formatOfTheElectronicHealthData",
    baseFieldWithAnd = "key",
    baseFieldValueWithAnd = "b",
    mandatoryFieldType = "objectList",
    mandatoryField = "lawfulForProcessingPersonalData",
    message = "lawfulForProcessingPersonalData must be provided when section 5 field #19 of T.01.5 = Pseudonymised and field #2 section T.01.3 = Yes or field #4 = Natural person"
  )

  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "informationWhichDataCanAccess",
    message = "informationWhichDataCanAccess must be provided when section 3 (Applicant and contact person information) value = Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "frequencyOfAccessOrDataUpdates",
    message = "frequencyOfAccessOrDataUpdates must be provided when section 3 (Applicant and contact person information) value = Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "complyWithDataMinimisationPrincipleNotEUMember",
    message = "complyWithDataMinimisationPrincipleNotEUMember must be provided when section 3 (Applicant and contact person information) value = Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "string",
    mandatoryField = "estimatedStartDatesForDataProcessing",
    message = "estimatedStartDatesForDataProcessing must be provided when section 3 (Applicant and contact person information) value = Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "string",
    mandatoryField = "estimatedEndDatesForDataProcessing",
    message = "estimatedEndDatesForDataProcessing must be provided when section 3 (Applicant and contact person information) value = No"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "string",
    mandatoryField = "informationWhichDataCanAccess",
    message = "informationWhichDataCanAccess must be provided when section 3 (Applicant and contact person information) value = Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "string",
    mandatoryField = "complyWithDataMinimisationPrinciple",
    message = "complyWithDataMinimisationPrinciple must be provided when section 3 (Applicant and contact person information) value = No"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "objectList",
    mandatoryField = "legalBasisForProcessingPersonalData",
    message = "legalBasisForProcessingPersonalData must be provided when section 3 (Applicant and contact person information) value = No"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "section3applyingForDataOnBehalfOfPublicSector",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "objectList",
    mandatoryField = "legalBasisForProcessingApplicationData",
    message = "legalBasisForProcessingApplicationData must be provided when section 3 (Applicant and contact person information) value = Yes"
  )

  //For list requirements
  @ListContainsSpecificValue(
    listField = "legalBasisForProcessingCombinedData",
    baseField = "key",
    baseFieldValue = "c",
    mandatoryField = "otherLegalBasisForProcessingCombinedData",
    message = "otherLegalBasisForProcessingCombinedData must be provided when "
      + "legalBasisForProcessingCombinedData is 3. other"
  )
  @ListContainsSpecificValue(
    listField = "legalBasisForProcessingCombinedApplicationData",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryField = "otherLegalBasisForProcessingCombinedApplicationData",
    message = "otherLegalBasisForProcessingCombinedApplicationData must be provided when "
      + "legalBasisForProcessingCombinedApplicationData is 2. other"
  )
  @ListContainsSpecificValue(
    listField = "section2purposeForWhichDataWillBeUsed",
    baseField = "key",
    baseFieldValue = "e",
    mandatoryField = "attachResearchPermitIfRequired",
    message = "attachResearchPermitIfRequired must be provided when "
      + "from section 2 is selected the option e"
  )
  @ListContainsSpecificValue(
    listField = "legalBasisForProcessingApplicationData",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryField = "otherLegalBasisForProcessingApplicationData",
    message = "otherLegalBasisForProcessingApplicationData must be provided when "
      + "legalBasisForProcessingApplicationData is 2. other"
  )
  @ListContainsSpecificValue(
    listField = "legalBasisForProcessingPersonalData",
    baseField = "key",
    baseFieldValue = "c",
    mandatoryField = "otherLegalBasisForProcessingPersonalData",
    message = "otherLegalBasisForProcessingPersonalData must be provided when "
      + "legalBasisForProcessingPersonalData is 3. other"
  )

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@DateHigherOrEqual(field = "estimatedStartDatesForDataProcessing",includeToday= true, field2 = "",
  message = "Date must be in the future", errorCode = "mustBeHigherOfToday")
@DateHigherOrEqual(field = "estimatedEndDatesForDataProcessing", field2 = "estimatedStartDatesForDataProcessing",
  message = "Date must be higher than starting day", errorCode = "mustBeHigherOfStartingDay")
public class Section8BindToOtherSections {

  //T.01.2 section 2 field
  List<SelectObjectDTO> section2purposeForWhichDataWillBeUsed;

  //T.01.3 section 3 field
  SelectObjectDTO section3applyingForDataOnBehalfOfPublicSector;
  SelectObjectDTO section3legalOrNaturalPerson;

  //T.01.10 section 7 field
  SelectObjectDTO section7willDataBeCombinedWithDataObtained;

  // T.01.5 section 5 field #19
  SelectObjectDTO section5formatOfTheElectronicHealthData;

  //field 7
  @Size(max = 2000)// validation 3 of T.01.3 (Applicant and contact person information) value = "Yes"
  String informationWhichDataCanAccess;
  //field 8
  //validation 3 of T.01.3 (Applicant and contact person information) value = "Yes"
  @Size(max = 2000)//validation 3 of T.01.3 (Applicant and contact person information) value = "Yes"
  String frequencyOfAccessOrDataUpdates;
  //field 9
  //validation 3 of T.01.3 (Applicant and contact person information) value = "No"
  String estimatedStartDatesForDataProcessing;
  //field 9
  //validation 3 of T.01.3 (Applicant and contact person information) value = "No"
  String estimatedEndDatesForDataProcessing;

  //field 18
  @Size(max = 500)//validation 3 of Visible if field #1 of T.01.3 value = "Yes"
    String complyWithDataMinimisationPrincipleNotEUMember;
  //field 19
  @Size(max = 500)//validation 3 of Visible if field #1 of T.01.3 value = "No"
    String complyWithDataMinimisationPrinciple;

  //field 26
  @Valid//visible when section 7 Visible if field #2 of T.01.10 = "Yes" and if field #2 section T.01.3 = "No"
  @CheckEnumValue(fieldName = "processingData", sectionNumber = "commonFields")
  List<SelectObjectDTO> legalBasisForProcessingCombinedData;
  @Size(max = 2000) //field 27
  String otherLegalBasisForProcessingCombinedData;
  //field 32
  @Valid//visible when section 7 Visible if field #2 of T.01.10 = "Yes" and if field #2 section T.01.3 = "Yes"
  @CheckEnumValue(fieldName = "legalBasisForProcessingData" , sectionNumber = "commonFields")
  List<SelectObjectDTO> legalBasisForProcessingCombinedApplicationData;
  @Size(max = 2000) //field 33
  String otherLegalBasisForProcessingCombinedApplicationData;

  // Field 23
  List<SelectObjectDTO> lawfulForProcessingPersonalData;
  // Field 28
  @CheckEnumValue(fieldName = "europeanUnionInstitution", sectionNumber = "section8")
  List<SelectObjectDTO> europeanUnionInstitution;


  //visible if on section 2
  //Option e - Scientific research related to health or care sectors.
  //Is selected as response for Question #5 of Section “Public information of the project “(T.01.2)
  @Valid
  @ListOfAttachmentsValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880, fieldName = "attachResearchPermitIfRequired") // 5MB
    List<AttachmentDTO> attachResearchPermitIfRequired;

  //field 24
  @Valid
  @CheckEnumValue(fieldName = "processingData", sectionNumber = "commonFields")
  List<SelectObjectDTO> legalBasisForProcessingPersonalData;
  @Size(max = 2000) //field 25
  String otherLegalBasisForProcessingPersonalData;
  @Valid//field 29
  @CheckEnumValue(fieldName = "legalBasisForProcessingData", sectionNumber = "commonFields")
  List<SelectObjectDTO> legalBasisForProcessingApplicationData;
  @Size(max = 2000) //field 30
  String otherLegalBasisForProcessingApplicationData;



}
