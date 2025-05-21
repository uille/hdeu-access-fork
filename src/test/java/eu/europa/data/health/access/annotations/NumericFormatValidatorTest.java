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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumericFormatValidatorTest {

  private NumericFormatValidator validator;

  @BeforeEach
  void setup() {
    validator = new NumericFormatValidator();
  }

  @Test
  void shouldReturnTrue_whenValueIsNull() {
    assertTrue(validator.isValid(null, null));
  }

  @Test
  void shouldReturnTrue_whenValueIsValidNumeric() {
    assertTrue(validator.isValid("123456", null));
  }

  @Test
  void shouldReturnFalse_whenValueContainsNonDigitCharacters() {
    assertFalse(validator.isValid("123a56", null));
    assertFalse(validator.isValid("12 345", null));
    assertFalse(validator.isValid("12-345", null));
  }

  @Test
  void shouldReturnFalse_whenValueIsOnlySpaces() {
    assertFalse(validator.isValid("   ", null));
  }

  @Test
  void shouldReturnFalse_whenValueHasLeadingSpaces() {
    assertFalse(validator.isValid(" 123", null));
  }

  @Test
  void shouldReturnFalse_whenValueHasTrailingSpaces() {
    assertFalse(validator.isValid("123 ", null));
  }
}
