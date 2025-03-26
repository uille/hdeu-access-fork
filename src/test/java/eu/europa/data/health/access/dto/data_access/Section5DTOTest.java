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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Section5DTOTest {

  private Validator validator;

  @BeforeEach
  public void setup() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void testLegalBasisRequiredWhenPurposeOfUseContainsB() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem = new SelectObjectDTO("b", "value");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem));  // contains "b"
    dto.setLegalBasis(null);  // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when legalBasis is null for purposeOfUse containing 'b'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("legalBasis must be provided")), "Expected validation message for 'legalBasis'");
  }

  @Test
  void testLinkToTheSupportingLegalBasisRequiredWhenPurposeOfUseContainsB() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem = new SelectObjectDTO("b", "value");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem));  // contains "b"
    dto.setLinkToTheSupportingLegalBasis(null);  // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when linkToTheSupportingLegalBasis is null for purposeOfUse containing 'b'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("linkToTheSupportingLegalBasis must be provided")), "Expected validation message for 'linkToTheSupportingLegalBasis'");
  }

  @Test
  void testPersonResearchNameRequiredWhenPurposeOfUseContainsE() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem = new SelectObjectDTO("e", "value");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem));  // contains "e"
    dto.setPersonResearchName(null);  // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when personResearchName is null for purposeOfUse containing 'e'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("personResearchName must be provided")), "Expected validation message for 'personResearchName'");
  }

  @Test
  void testInOrderToApplyPseudonymisedDataARequiredWhenFormatOfTheElectronicHealthDataIsB() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO formatOfTheElectronicHealthDataItem = new SelectObjectDTO("b", "value");
    dto.setFormatOfTheElectronicHealthData(formatOfTheElectronicHealthDataItem);
    dto.setInOrderToApplyPseudonymisedDataA(null); // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when inOrderToApplyPseudonymisedDataA is null for formatOfTheElectronicHealthData containing 'b'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("inOrderToApplyPseudonymisedDataA must be provided")), "Expected validation message for 'inOrderToApplyPseudonymisedDataA'");
  }

  @Test
  void testPersonResponsibleNameRequiredWhenPurposeOfUseContainsMoreThanOneValue() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem1 = new SelectObjectDTO("a", "value_a");
    SelectObjectDTO purposeOfUseItem2 = new SelectObjectDTO("e", "value_e");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem1, purposeOfUseItem2)); // contains more than one value
    dto.setPersonResponsibleName(null); // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when personResponsibleName is null for purposeOfUse containing more than one value");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("personResponsibleName must be provided")), "Expected validation message for 'personResponsibleName'");
  }

  @Test
  void testPersonResponsibleNameRequiredWhenPurposeOfUseDoesNotContainE() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem = new SelectObjectDTO("a", "value");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem)); // does not contain "e"
    dto.setPersonResponsibleName(null); // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when personResponsibleName is null for purposeOfUse not containing 'e'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("personResponsibleName must be provided")), "Expected validation message for 'personResponsibleName'");
  }

  @Test
  void testSummaryOfPlanForUsingTheDataRequiredWhenPurposeOfUseDoesNotContainE() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem = new SelectObjectDTO("a", "value");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem)); // does not contain "e"
    dto.setSummaryOfPlanForUsingTheData(null); // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when summaryOfPlanForUsingTheData is null for purposeOfUse not containing 'e'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("summaryOfPlanForUsingTheData must be provided")), "Expected validation message for 'summaryOfPlanForUsingTheData'");
  }

  @Test
  void testSummaryOfPlanForUsingTheDataLanguageRequiredWhenPurposeOfUseDoesNotContainE() {
    Section5DTO dto = new Section5DTO();
    SelectObjectDTO purposeOfUseItem = new SelectObjectDTO("a", "value");
    dto.setPurposeOfUse(Arrays.asList(purposeOfUseItem)); // does not contain "e"
    dto.setSummaryOfPlanForUsingTheDataLanguage(null); // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when summaryOfPlanForUsingTheDataLanguage is null for purposeOfUse not containing 'e'");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("summaryOfPlanForUsingTheDataLanguage must be provided")), "Expected validation message for 'summaryOfPlanForUsingTheDataLanguage'");
  }

  @Test
  void testFormatOfTheElectronicHealthDataRequired() {
    Section5DTO dto = new Section5DTO();
    dto.setFormatOfTheElectronicHealthData(null); // should trigger validation error

    var violations = validator.validate(dto);

    assertFalse(violations.isEmpty(), "Validation should fail when formatOfTheElectronicHealthData is null");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be null")), "Expected validation message for 'formatOfTheElectronicHealthData'");
  }
}

