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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Section10DTOTest {

  private final Validator validator;

  public Section10DTOTest() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testValidSection10DTO() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for valid Section10DTO.");
  }

  @Test
  void testNullAwareProcessingFee() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(null);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for null awareProcessingFee.");
  }

  @Test
  void testNullAwareChargeFee() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(null);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for null awareChargeFee.");
  }

  @Test
  void testNullAwareInformationCorrect() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(null);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for null awareInformationCorrect.");
  }

  @Test
  void testAwareProcessingFeeFalse() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(false);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for false awareProcessingFee.");
  }

  @Test
  void testAwareChargeFeeFalse() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(false);
    dto.setAwareInformationCorrect(true);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for false awareChargeFee.");
  }

  @Test
  void testAwareInformationCorrectFalse() {
    Section10DTO dto = new Section10DTO();
    dto.setAwareProcessingFee(true);
    dto.setAwareChargeFee(true);
    dto.setAwareInformationCorrect(false);

    Set<ConstraintViolation<Section10DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for false awareInformationCorrect.");
  }

  @Test
  void testGettersAndSetters() {
    Section10DTO dto = new Section10DTO();

    dto.setAwareProcessingFee(true);
    assertEquals(true, dto.getAwareProcessingFee(), "Expected awareProcessingFee to be true.");

    dto.setAwareChargeFee(true);
    assertEquals(true, dto.getAwareChargeFee(), "Expected awareChargeFee to be true.");

    dto.setAwareInformationCorrect(true);
    assertEquals(true, dto.getAwareInformationCorrect(), "Expected awareInformationCorrect to be true.");
  }

  @Test
  void testConstructorAndDefaultValues() {
    Section10DTO dto = new Section10DTO();
    assertNull(dto.getAwareProcessingFee(), "Expected awareProcessingFee to be null.");
    assertNull(dto.getAwareChargeFee(), "Expected awareChargeFee to be null.");
    assertNull(dto.getAwareInformationCorrect(), "Expected awareInformationCorrect to be null.");
  }
}
