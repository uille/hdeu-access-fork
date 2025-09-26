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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ListNotContainsSpecificValueValidatorTest {


  @Mock
  private ConstraintValidatorContext context;

  private ListNotContainsSpecificValueValidator validator;

  private ConstraintValidatorContext.ConstraintViolationBuilder builder;

  private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;


  @BeforeEach
  void setup() {
    validator = new ListNotContainsSpecificValueValidator();

    validator.initialize(new ListNotContainsSpecificValue() {
      @Override public String listField() { return "items"; }
      @Override public String baseField() { return "type"; }
      @Override public String baseFieldValue() { return "EXPECTED"; }
      @Override public String mandatoryField() { return "comment"; }
      @Override public String message() { return "Comment required when type is not 'EXPECTED'"; }
      @Override public String errorCode() { return "missingComment"; }
      @Override public Class<?>[] groups() { return new Class[0]; }
      @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
      @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ListNotContainsSpecificValue.class; }
    });

     builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);


  }

  @Test
  void shouldReturnTrue_whenListIsEmpty() {
    ContainerDTO dto = new ContainerDTO(Collections.emptyList(), null);
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenListFieldIsNull() {
    ContainerDTO dto = new ContainerDTO(null, null);
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenListContainsExpectedValue() {
    ContainerDTO dto = new ContainerDTO(List.of(new ListItemDTO("EXPECTED")), null);
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenListDoesNotContainExpectedValue_butMandatoryFieldIsPresent() {
    ContainerDTO dto = new ContainerDTO(List.of(new ListItemDTO("OTHER")), "Some comment");
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnFalse_whenListDoesNotContainExpectedValue_andMandatoryFieldIsEmpty() {

    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);

    ContainerDTO dto = new ContainerDTO(List.of(new ListItemDTO("OTHER")), "");
    assertFalse(validator.isValid(dto, context));
    verify(context).buildConstraintViolationWithTemplate(contains("missingComment"));
    verify(context.buildConstraintViolationWithTemplate(anyString())).addPropertyNode("comment");
  }

  @Test
  void shouldReturnFalse_whenReflectionFails() {
    Object invalidObject = new Object(); // No fields will match
    assertFalse(validator.isValid(invalidObject, context));
  }

  @Test
  void shouldReturnFalse_whenReflectionThrowsAnException() {
    class BrokenObject {
      public Object getItems() throws Exception {
        throw new Exception("Simulated reflection exception");
      }
    }
    BrokenObject invalidObject = new BrokenObject();
    assertFalse(validator.isValid(invalidObject, context));
  }
}
