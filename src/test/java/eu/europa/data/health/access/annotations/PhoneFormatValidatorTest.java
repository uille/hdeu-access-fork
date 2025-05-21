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
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PhoneFormatValidatorTest {

  private PhoneFormatValidator validator;
  private ConstraintValidatorContext context;
  private ConstraintViolationBuilder builder;

  @BeforeEach
  public void setUp() {
    validator = new PhoneFormatValidator();
    context = mock(ConstraintValidatorContext.class);
    builder = mock(ConstraintViolationBuilder.class);

    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    when(builder.addConstraintViolation()).thenReturn(context);
  }

  @Test
  void testValidPhoneNumber() {
    String phone = "(123) 456-7890";
    assertTrue(validator.isValid(phone, context));
  }

  @Test
  void testNullPhoneNumber() {
    assertTrue(validator.isValid(null, context));
  }

  @Test
  void testInvalidPhoneNumberFormat() {
    String phone = "invalid_phone";
    assertFalse(validator.isValid(phone, context));
  }

  @Test
  void testPhoneNumberStartingWithZero() {
    String phone = "0123-456-789";
    boolean result = validator.isValid(phone, context);

    assertFalse(result);
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("PhoneStartsWithZero");
    verify(builder).addConstraintViolation();
  }
}
