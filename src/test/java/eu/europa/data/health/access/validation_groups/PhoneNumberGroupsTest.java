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
package eu.europa.data.health.access.validation_groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.data.health.access.dto.PhoneNumberDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

class PhoneNumberGroupsTest {

  static class PhoneDTO {
    @NotBlank(groups = PhoneNumberGroups.NotBlankValidationGroup.class)
    @Pattern(regexp = "^\\+?[0-9 ]+$", groups = PhoneNumberGroups.PhoneFormatValidationGroup.class)
    String phone;

    public PhoneDTO(String phone) {
      this.phone = phone;
    }
  }

  private Validator validator;

  @BeforeEach
  void setup() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void shouldFailNotBlankGroupValidation() {
    PhoneDTO dto = new PhoneDTO(""); // blank phone
    Set<ConstraintViolation<PhoneDTO>> violations = validator.validate(dto, PhoneNumberGroups.NotBlankValidationGroup.class);

    assertFalse(violations.isEmpty());
    assertEquals("must not be blank", violations.iterator().next().getMessage());
  }

  @Test
  void shouldFailPhoneFormatGroupValidation() {
    PhoneDTO dto = new PhoneDTO("abc123"); // invalid format
    Set<ConstraintViolation<PhoneDTO>> violations = validator.validate(dto, PhoneNumberGroups.PhoneFormatValidationGroup.class);

    assertFalse(violations.isEmpty());
    assertTrue(violations.iterator().next().getMessage().contains("must match"));
  }

  @Test
  void shouldPassAllGroupsValidation() {
    PhoneDTO dto = new PhoneDTO("+123456 789");
    Set<ConstraintViolation<PhoneDTO>> violationsNotBlank = validator.validate(dto, PhoneNumberGroups.NotBlankValidationGroup.class);
    Set<ConstraintViolation<PhoneDTO>> violationsFormat = validator.validate(dto, PhoneNumberGroups.PhoneFormatValidationGroup.class);

    assertTrue(violationsNotBlank.isEmpty());
    assertTrue(violationsFormat.isEmpty());
  }


  @Test
  void testNotBlankGroupValidationFailsOnBlank() {
    PhoneNumberDTO dto = new PhoneNumberDTO("  ", "  "); // blank but not null

    Set<ConstraintViolation<PhoneNumberDTO>> violations =
      validator.validate(dto, PhoneNumberGroups.NotBlankValidationGroup.class);

    assertFalse(violations.isEmpty());
    assertEquals("must not be blank", violations.iterator().next().getMessage());
  }

  @Test
  void testPhoneFormatGroupValidationFailsOnBadFormat() {
    PhoneNumberDTO dto = new PhoneNumberDTO("111" ,"abc123");

    Set<ConstraintViolation<PhoneNumberDTO>> violations =
      validator.validate(dto, PhoneNumberGroups.PhoneFormatValidationGroup.class);

    assertFalse(violations.isEmpty());
    assertTrue(violations.iterator().next().getMessage().contains("Invalid phone format"));
  }

  @Test
  void testBothGroupsPass() {
    PhoneNumberDTO dto = new PhoneNumberDTO("+491","23456789");

    Set<ConstraintViolation<PhoneNumberDTO>> violations =
      validator.validate(dto, PhoneNumberGroups.NotBlankValidationGroup.class, PhoneNumberGroups.PhoneFormatValidationGroup.class);

    assertTrue(violations.isEmpty());
  }








}
