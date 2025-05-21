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
import static org.mockito.Mockito.mock;

class URLFormatValidatorTest {

  private URLFormatValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new URLFormatValidator();
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  void testValidHttpUrl() {
    assertTrue(validator.isValid("http://example.com", context));
  }

  @Test
  void testValidHttpsUrl() {
    assertTrue(validator.isValid("https://example.com/path?query=value", context));
  }

  @Test
  void testValidFtpUrl() {
    assertTrue(validator.isValid("ftp://ftp.example.com/resource", context));
  }

  @Test
  void testInvalidUrl_missingProtocol() {
    assertFalse(validator.isValid("www.example.com", context));
  }

  @Test
  void testInvalidUrl_justText() {
    assertFalse(validator.isValid("not_a_url", context));
  }

  @Test
  void testInvalidUrl_badScheme() {
    assertFalse(validator.isValid("abcd://example.com", context));
  }

  @Test
  void testNullUrl_isValid() {
    assertTrue(validator.isValid(null, context));
  }
}
