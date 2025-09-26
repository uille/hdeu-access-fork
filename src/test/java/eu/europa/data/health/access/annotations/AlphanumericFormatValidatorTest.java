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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphanumericFormatValidatorTest {

    @Test
    void testIsValid_withNullValue_shouldReturnTrue() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid(null, null);
        assertTrue(result, "Validator should return true for null value");
    }

    @Test
    void testIsValid_withEmptyString_shouldReturnTrue() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("", null);
        assertTrue(result, "Validator should return true for empty string");
    }

    @Test
    void testIsValid_withValidAlphanumericString_shouldReturnTrue() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("abc123", null);
        assertTrue(result, "Validator should return true for valid alphanumeric string");
    }

    @Test
    void testIsValid_withValidString_withAllowedCharacters_shouldReturnTrue() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("abc_123.test", null);
        assertTrue(result, "Validator should return true for valid string with allowed characters (_, .)");
    }

    @Test
    void testIsValid_withInvalidStringContainingSpecialCharacters_shouldReturnFalse() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("abc123!@#", null);
        assertFalse(result, "Validator should return false for string containing invalid special characters");
    }

    @Test
    void testIsValid_withWhitespace_shouldReturnFalse() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("abc 123", null);
        assertFalse(result, "Validator should return false for string containing whitespace");
    }

    @Test
    void testIsValid_withOnlySpecialCharacters_shouldReturnFalse() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("!@#$%^&*", null);
        assertFalse(result, "Validator should return false for string containing only special characters");
    }

    @Test
    void testIsValid_withMixedValidAndInvalidCharacters_shouldReturnFalse() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("abc_123$", null);
        assertFalse(result, "Validator should return false for string containing mixed valid and invalid characters");
    }

    @Test
    void testIsValid_withUppercaseAndLowercaseLetters_shouldReturnTrue() {
        AlphanumericFormatValidator validator = new AlphanumericFormatValidator();
        boolean result = validator.isValid("AbC123", null);
        assertTrue(result, "Validator should return true for string with uppercase and lowercase letters");
    }
}
