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

import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

public class ConditionalNotNullValueValidator implements ConstraintValidator<ConditionalNotNullValue, Object> {

  private String field;
  private String dependentField;
  private String requiredValue;
  private String message;

  @Override
  public void initialize(ConditionalNotNullValue constraintAnnotation) {
    this.field = constraintAnnotation.field();
    this.dependentField = constraintAnnotation.dependentField();
    this.requiredValue = constraintAnnotation.requiredValue();
    this.message = constraintAnnotation.message();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      Field fieldToCheck = value.getClass().getDeclaredField(field);
      fieldToCheck.trySetAccessible();

      Object fieldValue = fieldToCheck.get(value);

      Field dependentFieldToCheck = value.getClass().getDeclaredField(dependentField);
      dependentFieldToCheck.trySetAccessible();

      Object dependentFieldValue = dependentFieldToCheck.get(value);

      if(fieldValue != null){
        SelectObjectDTO legalOrNaturalPerson = (SelectObjectDTO)fieldValue;
        if(requiredValue.equals(legalOrNaturalPerson.getKey()) && (dependentFieldValue == null || dependentFieldValue.toString().trim().isEmpty())){
            context.disableDefaultConstraintViolation();
            String errorMessage = message
              .replace("{field}", field)
              .replace("{dependentField}", dependentField)
              .replace("{requiredValue}", requiredValue);
            context.buildConstraintViolationWithTemplate(errorMessage)
              .addPropertyNode(dependentField)
              .addConstraintViolation();
            return false;
        }
      }
      return true; // Always valid if the condition is not met
    } catch (Exception e) {
      return false; // In case of any error, consider the object invalid
    }
  }
}
