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
import eu.europa.data.health.access.annotations.ListObjectFieldsNotNull;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnObjectValue;
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnValue;
import eu.europa.data.health.access.annotations.Section8DataBeTransferred;
import eu.europa.data.health.access.annotations.Section8DataBeTransferredOpt48;
import eu.europa.data.health.access.dto.PersonDTO;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;


  @RequiredFieldBasedOnObjectValue(
    objectName = "whenDataNeeded",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "string",
    mandatoryField = "ifLaterWhen",
    message = "ifLaterWhen must be provided when whenDataNeeded is Later, when?"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "willTheDataBeTransferred",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "objectList",
    mandatoryField = "whichCountriesDataBeProcessed",
    message = "whichCountriesDataBeProcessed must be provided when willTheDataBeTransferred is Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "willTheDataBeTransferred",
    baseField = "key",
    baseFieldValue = "a",
    mandatoryFieldType = "object",
    mandatoryField = "legalBasisForTransferringTheDataOutsideEU",
    message = "legalBasisForTransferringTheDataOutsideEU must be provided when willTheDataBeTransferred is Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "legalBasisForTransferringTheDataOutsideEU",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryFieldType = "objectList",
    mandatoryField = "safeguardsAreProvidedByReferringGDCP",
    message = "safeguardsAreProvidedByReferringGDCP must be provided when willTheDataBeTransferred is Yes"
  )
  @RequiredFieldBasedOnObjectValue(
    objectName = "legalBasisForTransferringTheDataOutsideEU",
    baseField = "key",
    baseFieldValue = "c",
    mandatoryFieldType = "objectList",
    mandatoryField = "legalBasisForTransferringTheDataOutsideEUOtherOptions",
    message =
      "legalBasisForTransferringTheDataOutsideEUOtherOptions at least one option be provided wehn "
        + "legalBasisForTransferringTheDataOutsideEU is 3. Other exceptional legal basis (in the absence of appropriate safeguards pursuant to article 46)"
  )


  @RequiredFieldBasedOnValue(
    baseField = "whyWillDataBeTransferredOutsideEUArticle47",
    baseFieldValue = "true",
    mandatoryField = "whyWillDataBeTransferredOutsideEUArticle47Options",
    message =
      "whyWillDataBeTransferredOutsideEUArticle47Options at least one option be provided when "
        + "whyWillDataBeTransferredOutsideEUArticle47 is selected"
  )
  @RequiredFieldBasedOnValue(
    baseField = "whyWillDataBeTransferredOutsideEUArticle48b",
    baseFieldValue = "true",
    mandatoryField = "whyWillDataBeTransferredOutsideEUArticle48bOptions",
    message = "whyWillDataBeTransferredOutsideEUArticle48bOptions at least one option be provided whyWillDataBeTransferredOutsideEUArticle48b is selected"
  )
  @RequiredFieldBasedOnValue(
    baseField = "whyWillDataBeTransferredOutsideEUArticle48c",
    baseFieldValue = "true",
    mandatoryField = "whyWillDataBeTransferredOutsideEUArticle48cOpt",
    message = "whyWillDataBeTransferredOutsideEUArticle48cOpt must be provided when whyWillDataBeTransferredOutsideEUArticle48c is selected"
  )

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Section8DataBeTransferred
@Section8DataBeTransferredOpt48
@DateHigherOrEqual(field = "ifLaterWhen", field2 = "", message = "Date must be in the future", errorCode = "mustBeHigherThanToday")
@DateHigherOrEqual(field = "startPeriodOfInactiveDataStorage",includeToday= true, field2 = "",
  message = "Date must be today or in the future", errorCode = "mustBeHigherOrEqualOfToday")
@DateHigherOrEqual(field = "endPeriodOfInactiveDataStorage", field2 = "startPeriodOfInactiveDataStorage",
  message = "Date must be higher than starting day", errorCode = "mustBeHigherOfStartingDay")
public class Section8DTO extends SectionDTO {

  @NotBlank
  @Size(max = 2000)
  String technicalRequirementsForEnvironment;
  @Size(max = 2000)
  String nameWebsiteAddressForEnvironment;
  @NotNull
  @CheckEnumValue(fieldName = "whenDataNeeded", sectionNumber = "section8")
  SelectObjectDTO whenDataNeeded;
  //Date selected > current date
  String ifLaterWhen;

  //Start date >= Current date
  String startPeriodOfInactiveDataStorage;
  String endPeriodOfInactiveDataStorage;

  @NotNull
  @CheckEnumValue(fieldName = "yesOrNoOptions", sectionNumber = "commonFields")
  SelectObjectDTO willTheDataBeTransferred;

  @Valid//Visible if field willTheDataBeTransferred #11 of T.01.11 = "Yes"
  List<SelectObjectDTO> whichCountriesDataBeProcessed;

  boolean whyWillDataBeTransferredOutsideEUArticle47;
  @Valid//if whyWillDataBeTransferredOutsideEUArticle47=true
  @CheckEnumValue(fieldName = "whyWillDataBeTransferredOutsideEUArticle47Options", sectionNumber = "section8")
  List<SelectObjectDTO> whyWillDataBeTransferredOutsideEUArticle47Options;
  boolean whyWillDataBeTransferredOutsideEUArticle48;
  boolean whyWillDataBeTransferredOutsideEUArticle48a;
  boolean whyWillDataBeTransferredOutsideEUArticle48b;
  @Valid//if whyWillDataBeTransferredOutsideEUArticle48b=true
  @CheckEnumValue(fieldName = "whyWillDataBeTransferredOutsideEUArticle48bOptions", sectionNumber = "section8")
  List<SelectObjectDTO> whyWillDataBeTransferredOutsideEUArticle48bOptions;
  boolean whyWillDataBeTransferredOutsideEUArticle48c;
  @CheckEnumValue(fieldName = "whyWillDataBeTransferredOutsideEUArticle48cOpt", sectionNumber = "section8")
  SelectObjectDTO whyWillDataBeTransferredOutsideEUArticle48cOpt;
  boolean whyWillDataBeTransferredOutsideEUArticle48d;
  boolean whyWillDataBeTransferredOutsideEUArticle48e;
  boolean whyWillDataBeTransferredOutsideEUArticle49;

  //field 14
  @CheckEnumValue(fieldName = "legalBasisForTransferringTheDataOutsideEU", sectionNumber = "section8")
  SelectObjectDTO legalBasisForTransferringTheDataOutsideEU;
  @Valid//required when legalBasisForTransferringTheDataOutsideEU select option 2, //field 15
  @CheckEnumValue(fieldName = "safeguardsAreProvidedByReferringGDCP", sectionNumber = "section8")
  List<SelectObjectDTO> safeguardsAreProvidedByReferringGDCP;
  @Valid//required when legalBasisForTransferringTheDataOutsideEU select option 3, //field 16
  @CheckEnumValue(fieldName = "legalBasisForTransferringTheDataOutsideEUOtherOptions", sectionNumber = "section8")
  List<SelectObjectDTO> legalBasisForTransferringTheDataOutsideEUOtherOptions;
  @NotBlank
  @Size(max = 500)//field 17
  String whichOrganizationWillBeTheControllerOfData;

  //Go to Section8BindToOtherSections for the next fields 18 and 19

  //Fields 20 all the booleans
  @AssertTrue
  boolean protectionAndSecurityStatementsA;
  @AssertTrue
  boolean protectionAndSecurityStatementsB;
  @AssertTrue
  boolean protectionAndSecurityStatementsC;
  @AssertTrue
  boolean protectionAndSecurityStatementsD;
  @AssertTrue
  boolean protectionAndSecurityStatementsE;

  //field 22
  @Valid
  @ListObjectFieldsNotNull(fields = {"fullName", "affiliation", "emailAddress"}, isOptional = false)
  List<PersonDTO> peopleWhoWillBeProcessingTheData;



}
