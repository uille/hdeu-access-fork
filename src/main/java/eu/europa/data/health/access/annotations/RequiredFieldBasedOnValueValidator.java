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
import org.apache.commons.beanutils.BeanUtils;

public class RequiredFieldBasedOnValueValidator implements ConstraintValidator<RequiredFieldBasedOnValue, Object> {

  private String baseField;
  private String baseFieldValue;
  private String mandatoryField;
  private boolean isMandatory;

  @Override
  public void initialize(RequiredFieldBasedOnValue constraintAnnotation) {
    this.baseField = constraintAnnotation.baseField();
    this.baseFieldValue = constraintAnnotation.baseFieldValue();
    this.mandatoryField = constraintAnnotation.mandatoryField();
    this.isMandatory = constraintAnnotation.isMandatory();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {

      String baseFieldVal = BeanUtils.getProperty(value, this.baseField);
      String mandatoryFieldValue = BeanUtils.getProperty(value, this.mandatoryField);

      if(!this.isMandatory && mandatoryFieldValue == null)
        return true;

      if (this.baseFieldValue.equalsIgnoreCase(baseFieldVal)) {
        return mandatoryFieldValue != null && !mandatoryFieldValue.trim().isEmpty();
      } else if(baseFieldVal == null && this.baseFieldValue.equals("null")){
        return mandatoryFieldValue != null && !mandatoryFieldValue.trim().isEmpty();
      } else {
        return true;
      }
    } catch (Exception e) {
      // log error or handle exception
      return false;
    }
  }
}
