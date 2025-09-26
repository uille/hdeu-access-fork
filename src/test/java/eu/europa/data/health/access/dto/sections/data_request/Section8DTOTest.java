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
package eu.europa.data.health.access.dto.sections.data_request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Section8DTOTest {

  private static Validator validator;

  @BeforeAll
  static void setupValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void shouldPassValidation_whenAllFieldsAreTrue() {
    Section8DTO dto = new Section8DTO();
    dto.setAcceptHealthDataBody(true);
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation violations");
  }

  @Test
  void shouldFailValidation_whenAnyFieldIsNull() {
    Section8DTO dto = new Section8DTO(); // all fields are null

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertEquals(4, violations.size(), "Expected 4 violations for null fields");
  }

  @Test
  void shouldFailValidation_whenAnyFieldIsFalse() {
    Section8DTO dto = new Section8DTO();
    dto.setAcceptHealthDataBody(false);
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("acceptHealthDataBody")));
  }
}
