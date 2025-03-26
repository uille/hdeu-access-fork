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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListContainsSpecificValueValidator implements
  ConstraintValidator<ListContainsSpecificValue, Object> {


  private String listField;
  private String baseField;
  private String baseFieldValue;
  private String mandatoryField;
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void initialize(ListContainsSpecificValue constraintAnnotation) {
    this.listField = constraintAnnotation.listField();
    this.baseField = constraintAnnotation.baseField();
    this.baseFieldValue = constraintAnnotation.baseFieldValue();
    this.mandatoryField = constraintAnnotation.mandatoryField();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      List<?> list = (List<?>) PropertyUtils.getProperty(value, this.listField);
      if(list == null || list.isEmpty()) {
        return true;
      }
      for (Object obj : list) {
        String baseFieldVal = (String) PropertyUtils.getProperty(obj, this.baseField);
        if (Objects.equals(this.baseFieldValue, baseFieldVal)) {
          String mandatoryFieldValue = BeanUtils.getProperty(value, this.mandatoryField);
          if (Objects.isNull(mandatoryFieldValue) || mandatoryFieldValue.trim().isEmpty()) {
            return false;
          }
        }
      }
    } catch (Exception e) {
      // log error or handle exception
      log.info(e.getMessage());
      return false;
    }
    return true;
  }

}
