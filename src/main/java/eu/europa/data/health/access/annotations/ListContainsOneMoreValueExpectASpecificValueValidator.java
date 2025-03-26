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

public class ListContainsOneMoreValueExpectASpecificValueValidator implements
  ConstraintValidator<ListContainsOneMoreValueExpectASpecificValue, Object> {

  private String listField;
  private String baseField;
  private String expectFieldValue;
  private String mandatoryField;

  @Override
  public void initialize(ListContainsOneMoreValueExpectASpecificValue constraintAnnotation) {
    this.listField = constraintAnnotation.listField();
    this.baseField = constraintAnnotation.baseField();
    this.expectFieldValue = constraintAnnotation.expectFieldValue();
    this.mandatoryField = constraintAnnotation.mandatoryField();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      List<?> list = (List<?>) PropertyUtils.getProperty(value, this.listField);
      if (list == null || list.isEmpty()) {
        return true; // List is empty, no validation needed
      }

      boolean mandatoryFieldValid = true;
      boolean listConditionMet = false;

      // Check if mandatoryField is valid (not null or blank)
      String mandatoryFieldValue = BeanUtils.getProperty(value, this.mandatoryField);

      if (list.size() == 1) {
        String baseFieldValue = (String) PropertyUtils.getProperty(list.get(0), this.baseField);

        if(Objects.equals(baseFieldValue, this.expectFieldValue)){
          return true;
        }
      }

      // Check if any item in the list has a baseField value not equal to 'e'
      for (Object obj : list) {
        String baseFieldValue = (String) PropertyUtils.getProperty(obj, this.baseField);

        // If baseFieldValue is not equal to "e", list condition is met
        if (!Objects.equals(baseFieldValue, this.expectFieldValue)) {
          mandatoryFieldValid = !(Objects.isNull(mandatoryFieldValue) || mandatoryFieldValue.isBlank());
          listConditionMet = true;
          break;
        }
      }

      // Only return true if both mandatoryFieldValid and listConditionMet are true
      return mandatoryFieldValid && listConditionMet;

    } catch (Exception e) {
      // log error or handle exception
      return false;
    }
  }

}

