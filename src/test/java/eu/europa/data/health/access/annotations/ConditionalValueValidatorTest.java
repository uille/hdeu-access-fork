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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConditionalValueValidatorTest {

  private ConditionalValueValidator validator;
  private ConstraintValidatorContext context;


  @Data
  @AllArgsConstructor
  static class TestDTO {
    private SelectObjectDTO legalStatus;
    private String taxNumber;
  }

  @BeforeEach
  void setUp() {
    validator = new ConditionalValueValidator();
    context = mock(ConstraintValidatorContext.class);

    ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(builder);
    when(builder.addPropertyNode(any())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);

    // Simulate annotation values via a fake annotation
    validator.initialize(new ConditionalValue() {
      @Override
      public String field() { return "legalStatus"; }

      @Override
      public String dependentField() { return "taxNumber"; }

      @Override
      public String requiredValue() { return "LEGAL"; }

      @Override
      public String message() {
        return "Invalid because {field} must be {requiredValue} when {dependentField} is not null.";
      }

      @Override public Class<?>[] groups() { return new Class<?>[0]; }
      @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ConditionalValue.class; }
      @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
    });
  }

  @Test
  void shouldReturnTrue_whenDependentFieldIsNull() {
    TestDTO dto = new TestDTO(new SelectObjectDTO("NATURAL", "Natural Person"), null);
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenFieldHasCorrectRequiredValue() {
    TestDTO dto = new TestDTO(new SelectObjectDTO("LEGAL", "Legal Person"), "123456789");
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnFalse_whenFieldDoesNotHaveRequiredValueAndDependentFieldIsPresent() {
    TestDTO dto = new TestDTO(new SelectObjectDTO("NATURAL", "Natural Person"), "123456789");
    assertFalse(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnFalse_whenFieldThrowsException() {
    // Create object with missing field (to force exception)
    Object dto = new Object();
    assertFalse(validator.isValid(dto, context));
  }
}

