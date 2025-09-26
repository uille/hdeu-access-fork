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
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.ConditionalNotNullValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

  //Natural Person Validations
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonJobTitle", requiredValue = "b", message = "naturalPersonJobTitle field must only be provided when legalOrNaturalPerson value is: 'Natural person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonAffiliation", requiredValue = "b", message = "naturalPersonAffiliation field must only be provided when legalOrNaturalPerson value is: 'Natural person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonAddress", requiredValue = "b", message = "naturalPersonAddress field must only be provided when legalOrNaturalPerson value is: 'Natural person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonZipCode", requiredValue = "b", message = "naturalPersonZipCode field must only be provided when legalOrNaturalPerson value is: 'Natural person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonCity", requiredValue = "b", message = "naturalPersonCity field must only be provided when legalOrNaturalPerson value is: 'Natural person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonCountry", requiredValue = "b", message = "naturalPersonCountry field must only be provided when legalOrNaturalPerson value is: 'Natural person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "naturalPersonPhone", requiredValue = "b", message = "naturalPersonPhone field must only be provided when legalOrNaturalPerson value is: 'Natural person'")

  //Legal Person Validations
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonOrganisationName", requiredValue = "a", message = "legalPersonOrganisationName field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonOrganisationName", requiredValue = "a", message = "legalPersonOrganisationName field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonBusinessID", requiredValue = "a", message = "legalPersonBusinessID field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonOperatorID", requiredValue = "a", message = "legalPersonOperatorID field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonAddress", requiredValue = "a", message = "legalPersonAddress field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonZipCode", requiredValue = "a", message = "legalPersonZipCode field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonCity", requiredValue = "a", message = "legalPersonCity field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonCountry", requiredValue = "a", message = "legalPersonCountry field must only be provided when legalOrNaturalPerson value is: 'Legal person'")
  @ConditionalNotNullValue(field = "legalOrNaturalPerson", dependentField = "legalPersonPhone", requiredValue = "a", message = "legalPersonPhone is mandatory when legalOrNaturalPerson value is: 'Legal person'")

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDTO {

  String euLoginID;

  @NotNull
  @CheckEnumValue(fieldName = "legalOrNaturalPerson", sectionNumber = "section3")
  SelectObjectDTO legalOrNaturalPerson;

  //Natural Person properties

  @Size(max = 500)
  String naturalPersonJobTitle;

  @Size(max = 500)
  String naturalPersonAffiliation;

  @Size(max = 2000)
  String naturalPersonAddress;

  @Size(max = 50)
  String naturalPersonZipCode;

  @Size(max = 500)
  String naturalPersonCity;

  @Valid
  SelectObjectDTO naturalPersonCountry;

  @Valid
  PhoneNumberDTO naturalPersonPhone;

  // Legal Person properties

  @Size(max = 250)
  String legalPersonOrganisationName;

  @Size(max = 250)
  String legalPersonBusinessID;

  @Size(max = 250)
  String legalPersonOperatorID;

  @Size(max = 2000)
  String legalPersonAddress;

  @Size(max = 50)
  String legalPersonZipCode;

  @Size(max = 500)
  String legalPersonCity;

  @Valid
  SelectObjectDTO legalPersonCountry;

  @Valid
  PhoneNumberDTO legalPersonPhone;

  @JsonProperty("date_modified")
  String dateModified;
}
