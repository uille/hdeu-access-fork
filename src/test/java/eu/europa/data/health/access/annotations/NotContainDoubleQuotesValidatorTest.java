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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotContainDoubleQuotesValidatorTest {

  private NotContainDoubleQuotesValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new NotContainDoubleQuotesValidator();
  }

  @Test
  void testNullValue() {
    assertTrue(validator.isValid(null, null)); // null is considered valid
  }

  @Test
  void testBlankValue() {
    assertTrue(validator.isValid("   ", null)); // blank (whitespace) is valid
  }

  @Test
  void testValidStringWithoutQuotes() {
    assertTrue(validator.isValid("Hello, world!", null));
  }

  @Test
  void testInvalidStringWithDoubleQuotes() {
    assertFalse(validator.isValid("This is a \"quoted\" word", null));
  }

  @Test
   void testOnlyDoubleQuote() {
    assertFalse(validator.isValid("\"", null));
  }
}

