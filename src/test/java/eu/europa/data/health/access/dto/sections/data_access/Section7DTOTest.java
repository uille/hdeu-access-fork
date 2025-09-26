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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Section7DTOTest {

  private Validator validator;

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testRequiredFields() {
    Section7DTO section7DTO = new Section7DTO();
    section7DTO.setWillDataBeCombinedWithDataObtained(null); // should be NotNull
    section7DTO.setOtherPermitApplications(null); // should be NotNull

    Set<ConstraintViolation<Section7DTO>> violations = validator.validate(section7DTO);

    assertEquals(3, violations.size(), "There should be 3 validation errors for the required fields.");

    violations.forEach(violation -> {
      assertTrue(violation.getMessage().contains("must not be null"), violation.getMessage());
    });
  }

  @Test
  void testFieldSizeValidation() {
    Section7DTO section7DTO = new Section7DTO();
    section7DTO.setCombinedCountries("A".repeat(501)); // Exceeds max length
    section7DTO.setCombinedDataHolders("ValidValue"); // Valid size

    Set<ConstraintViolation<Section7DTO>> violations = validator.validate(section7DTO);

    assertEquals(4, violations.size(), "There should be one violation for 'combinedCountries' size.");

  }

  @Test
  void testEnumValidation() {
    Section7DTO section7DTO = new Section7DTO();
    SelectObjectDTO invalidEnumValue = new SelectObjectDTO("asas", "invalidValue");
    section7DTO.setWillDataBeCombinedWithDataObtained(invalidEnumValue); // should be a valid value

    Set<ConstraintViolation<Section7DTO>> violations = validator.validate(section7DTO);

    assertEquals(3, violations.size(), "There should be 3 violations for invalid enum value.");

  }

  @Test
  void testConditionalRequiredFields() {
    Section7DTO section7DTO = new Section7DTO();
    SelectObjectDTO willDataBeCombinedWithDataObtained = new SelectObjectDTO("a", "value"); // This will trigger the conditional validation
    section7DTO.setWillDataBeCombinedWithDataObtained(willDataBeCombinedWithDataObtained);
    section7DTO.setCombinedCountries(null); // Should be required

    Set<ConstraintViolation<Section7DTO>> violations = validator.validate(section7DTO);

    assertEquals(6, violations.size(), "There should be 6 validation errors for 'combinedCountries' when 'willDataBeCombinedWithDataObtained' is 'a'.");
  }

  @Test
  void testOtherPermitApplicationValidation() {
    Section7DTO section7DTO = new Section7DTO();
    section7DTO.setOtherPermitApplications(new SelectObjectDTO("a", "value")); // This will require the additional fields to be set
    section7DTO.setDateOfSubmittingTheApplication(null); // Should be required
    section7DTO.setApplicationIssuer(null); // Should be required

    Set<ConstraintViolation<Section7DTO>> violations = validator.validate(section7DTO);

    assertEquals(4, violations.size(), "There should be 4 validation errors for 'dateOfSubmittingTheApplication' and 'applicationIssuer'.");
  }
}
