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
package eu.europa.data.health.access.annotations;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UUIDFormatValidatorTest {

  private UUIDFormatValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  public void setUp() {
    validator = new UUIDFormatValidator();
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  void testValidUUID() {
    String validUUID = "123e4567-e89b-12d3-a456-426614174000";
    assertTrue(validator.isValid(validUUID, context));
  }

  @Test
  void testInvalidUUID() {
    String invalidUUID = "invalid-uuid";
    assertFalse(validator.isValid(invalidUUID, context));
  }

  @Test
  void testNullUUID() {
    assertTrue(validator.isValid(null, context), "Null should be valid (use @NotNull to enforce non-null)");
  }

  @Test
  void testUUIDWithWrongLength() {
    String tooShortUUID = "1234";
    assertFalse(validator.isValid(tooShortUUID, context));
  }

  @Test
  void testUUIDWithUpperCaseLetters() {
    String validUppercaseUUID = "123E4567-E89B-12D3-A456-426614174000";
    assertTrue(validator.isValid(validUppercaseUUID, context));
  }
  @Test
  void testUUIDWithExtraCharacters() {
    String uuidWithExtraCharacters = "123e4567-e89b-12d3-a456-426614174000extra";
    assertFalse(validator.isValid(uuidWithExtraCharacters, context), "UUID with extra characters should be invalid");
  }

  @Test
  void testUUIDWithMissingHyphens() {
    String uuidWithMissingHyphens = "123e4567e89b12d3a456426614174000";
    assertFalse(validator.isValid(uuidWithMissingHyphens, context), "UUID with missing hyphens should be invalid");
  }

  @Test
  void testUUIDWithOnlySpaces() {
    String uuidWithSpaces = "    ";
    assertFalse(validator.isValid(uuidWithSpaces, context), "UUID with only spaces should be invalid");
  }

  @Test
  void testEmptyString() {
    String emptyString = "";
    assertFalse(validator.isValid(emptyString, context), "Empty string should be invalid");
  }
}
