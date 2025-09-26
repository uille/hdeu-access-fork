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
package eu.europa.data.health.access.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.data.health.access.annotations.AttachmentValidation;
import eu.europa.data.health.access.annotations.TranslatableField;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabulationPlanDTO {

  //These variables are used to create violation errors to send them into the UI
  @JsonProperty("country_id")
  String countryId;
  @JsonProperty("list_index")
  Integer listIndex;

  @Size(max = 500)
  @TranslatableField
  String tabulationRegisteredToBeUsed;
  @Size(max = 500)
  @TranslatableField
  String tabulationPossibleStudyCohort;
  @Size(max = 1000)
  @TranslatableField
  String tabulationInformationOfRequiredVariables;
  @Size(max = 1000)
  @TranslatableField
  String tabulationFormationVariables;
  @Size(max = 1000)
  @TranslatableField
  String tabulationDesiredDirection;
  @Size(max = 1000)
  @TranslatableField
  String tabulationOrderInWhichTable;
  @Size(max = 1000)
  @TranslatableField
  String tabulationAnyOtherRelevant;
  @AttachmentValidation(allowedExtensions = {"doc", "docx", "pdf", "xls", "xlsx",
    "odt"}, maxSize = 5242880) // 5MB
  AttachmentDTO tabulationPlan;
}
