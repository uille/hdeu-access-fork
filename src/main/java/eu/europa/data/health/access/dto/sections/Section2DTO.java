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
import eu.europa.data.health.access.annotations.RequiredFieldBasedOnValue;
import eu.europa.data.health.access.annotations.TranslatableField;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

@RequiredFieldBasedOnValue(
  baseField = "theNatureOfTheProjectDoesNotLetYouProvideASummary",
  baseFieldValue = "true",
  mandatoryField = "theNatureOfTheProjectDoesNotLetYouProvideASummaryReason",
  message = "theNatureOfTheProjectDoesNotLetYouProvideASummaryReason must be provided when theNatureOfTheProjectDoesNotLetYouProvideASummary is true"
)
@RequiredFieldBasedOnValue(
  baseField = "theNatureOfTheProjectDoesNotLetYouProvideASummary",
  baseFieldValue = "null",
  mandatoryField = "summaryOfTheProject",
  message = "summaryOfTheProject must be provided when theNatureOfTheDataDoesNotLetYouProvideADescription is false"
)

@JsonIgnoreProperties(ignoreUnknown = true)
public class Section2DTO extends SectionDTO {

  @NotBlank
  @Size(max = 500)
  @TranslatableField
  String projectName;
  @NotBlank
  @Size(max = 500)
  String projectLeader;
  @NotNull
  @Valid
  SelectObjectDTO countryOfProjectLeader;
  @NotNull
  @NotEmpty
  @Valid
  @CheckEnumValue(fieldName = "purposeForWhichDataWillBeUsed", sectionNumber = "section2")
  List<SelectObjectDTO> purposeForWhichDataWillBeUsed;
  @Size(max = 2000)
  @TranslatableField
  String summaryOfTheProject;
  Boolean theNatureOfTheProjectDoesNotLetYouProvideASummary;
  @Size(max = 2000)
  @TranslatableField
  String theNatureOfTheProjectDoesNotLetYouProvideASummaryReason;

}
