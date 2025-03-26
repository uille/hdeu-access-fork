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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class DateLowerOrEqualValidator implements
  ConstraintValidator<DateLowerOrEqual, Object> {

  private boolean includeToday;
  private String field;
  private String field2;

  @Override
  public void initialize(DateLowerOrEqual constraintAnnotation) {
    this.includeToday = constraintAnnotation.includeToday();
    this.field = constraintAnnotation.field();
    this.field2 = constraintAnnotation.field2();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      if (Objects.isNull(field)) {
        return true;
      }
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      Field startField = value.getClass().getDeclaredField(field);
      startField.trySetAccessible();

      String startDate = (String) startField.get(value);
      if (StringUtils.isBlank(startDate)) {
        return true;
      }
      LocalDate start = LocalDate.parse(startDate, dateFormatter);
      String endDate;
      LocalDate end = LocalDate.now();
      if (!(Objects.isNull(field2) || field2.isEmpty())) {
        Field endField = value.getClass().getDeclaredField(field2);
        endField.trySetAccessible();
        endDate = (String) endField.get(value);
        //if the field do not have a value then it is not valid
        if (Objects.isNull(endDate) || endDate.isBlank()) {
          context.disableDefaultConstraintViolation();
          context.buildConstraintViolationWithTemplate(
              context.getDefaultConstraintMessageTemplate())
            .addPropertyNode(field)
            .addConstraintViolation();
          return false;
        }
        end = LocalDate.parse(endDate, dateFormatter);
      }
      boolean isValid = includeToday ? !end.isBefore(start) : end.isAfter(start);
      if (BooleanUtils.isFalse(isValid)) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
          .addPropertyNode(field)
          .addConstraintViolation();
      }
      return isValid;
    } catch (NoSuchFieldException | IllegalAccessException | DateTimeParseException e) {
      return false;
    }
  }
}
