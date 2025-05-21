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

import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class DateLowerOrEqualValidatorTest {

  private DateLowerOrEqualValidator validator;
  private ConstraintValidatorContext context;

  ConstraintValidatorContext.ConstraintViolationBuilder builder;
  ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

  static class TestDTO {
    String start;
    String end;

    TestDTO(String start, String end) {
      this.start = start;
      this.end = end;
    }
  }

  @BeforeEach
  void setUp() {
    validator = new DateLowerOrEqualValidator();
    context = mock(ConstraintValidatorContext.class);

    // Mock the full method chain: buildConstraintViolationWithTemplate → addPropertyNode → addConstraintViolation
    builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
  }

  @Test
  void shouldReturnTrue_whenStartDateBeforeEndDate() {
    validator.initialize(annotation("start", "end", false));

    TestDTO dto = new TestDTO("2020-01-01", "2020-12-31");
    assertTrue(validator.isValid(dto, context));
  }


  @Test
  void shouldReturnTrue_error() {
    validator.initialize(annotation("startFail", "endFail", false));

    TestDTO dto = new TestDTO("2020-01-01", "2020-12-31");
    assertFalse(validator.isValid(dto, context));
  }


  @Test
  void shouldReturnTrue_whenStartDateEqualsEndDate_andIncludeTodayTrue() {
    validator.initialize(annotation("start", "end", true));

    String today = LocalDate.now().toString();
    TestDTO dto = new TestDTO(today, today);
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnFalse_whenStartDateAfterEndDate_andIncludeTodayFalse() {

    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(builder);
    when(builder.addPropertyNode(any())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);

    validator.initialize(annotation("start", "end", false));

    TestDTO dto = new TestDTO("2024-01-01", "2023-01-01");
    assertFalse(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnFalse_whenField2Empty_andStartDateInFuture() {

    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(builder);
    when(builder.addPropertyNode(any())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);

    validator.initialize(annotation("start", "", false));

    String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    TestDTO dto = new TestDTO(tomorrow, null);
    assertFalse(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenField2Empty_andStartDateIsToday_andIncludeTodayTrue() {
    validator.initialize(annotation("start", "", true));

    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    TestDTO dto = new TestDTO(today, null);
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenStartDateIsBlank() {
    validator.initialize(annotation("start", "end", false));
    TestDTO dto = new TestDTO("", "2023-01-01");
    assertTrue(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnFalse_whenEndDateIsNull() {

    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(builder);
    when(builder.addPropertyNode(any())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);

    validator.initialize(annotation("start", "end", false));
    TestDTO dto = new TestDTO("2022-01-01", null);
    assertFalse(validator.isValid(dto, context));
  }

  @Test
  void shouldReturnTrue_whenStartFieldIsNull() {
    validator.initialize(annotation(null, "end", false));
    TestDTO dto = new TestDTO("2022-01-01", "2023-01-01");
    assertTrue(validator.isValid(dto, context));
  }

  // Helper to mock the annotation
  private DateLowerOrEqual annotation(String field, String field2, boolean includeToday) {
    return new DateLowerOrEqual() {
      @Override public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return DateLowerOrEqual.class;
      }
      @Override public String message() {
        return "Date is not valid";
      }
      @Override public Class<?>[] groups() {
        return new Class<?>[0];
      }
      @Override public Class<? extends jakarta.validation.Payload>[] payload() {
        return new Class[0];
      }
      @Override public String errorCode() {
        return "dateNotValid";
      }
      @Override public boolean includeToday() {
        return includeToday;
      }
      @Override public String field() {
        return field;
      }
      @Override public String field2() {
        return field2;
      }
    };
  }
}

