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
import java.util.List;
import java.util.Objects;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

public class ListContainsOneMoreValueThanASpecificValueValidator implements
  ConstraintValidator<ListContainsOneMoreValueThanASpecificValue, Object> {


  private String listField;
  private String baseField;
  private String baseFieldValue;
  private String[] baseFieldValues; // Changed to array to accept multiple values
  private String mandatoryField;

  @Override
  public void initialize(ListContainsOneMoreValueThanASpecificValue constraintAnnotation) {
    this.listField = constraintAnnotation.listField();
    this.baseField = constraintAnnotation.baseField();
    this.baseFieldValues = constraintAnnotation.baseFieldValues(); // Updated to array
    this.mandatoryField = constraintAnnotation.mandatoryField();
    this.baseFieldValue = constraintAnnotation.baseFieldValue();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      List<?> list = (List<?>) PropertyUtils.getProperty(value, this.listField);

      boolean conditionMet = false;
      boolean mandatoryFieldValid = true;

      if (list == null || list.isEmpty()) {
        return true;
      }

      for (Object obj : list) {
        String baseFieldVal = (String) PropertyUtils.getProperty(obj, this.baseField);

        // Check if baseFieldValue is in baseFieldValues array or a single value
        if ((!Objects.isNull(baseFieldValues) && containsBaseFieldValue(baseFieldVal))
        || (!Objects.isNull(this.baseFieldValue) && !this.baseFieldValue.isEmpty()
          && Objects.equals(this.baseFieldValue, baseFieldVal))) {
          conditionMet = true;
          String mandatoryFieldValue = BeanUtils.getProperty(value, this.mandatoryField);
          if (Objects.isNull(mandatoryFieldValue) || mandatoryFieldValue.isBlank()) {
            mandatoryFieldValid = false;
            break;
          }
        }
      }

      if (conditionMet) {
        return mandatoryFieldValid;
      }

      return true;
    } catch (Exception e) {
      // log error or handle exception
      return false;
    }
  }

  // Helper method to check if baseFieldValue is in baseFieldValues array
  private boolean containsBaseFieldValue(String baseFieldValue) {
    for (String value : this.baseFieldValues) {
      if (Objects.equals(value, baseFieldValue)) {
        return true;
      }
    }
    return false;
  }

}

