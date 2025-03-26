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
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ListObjectFieldsNotNullValidator implements ConstraintValidator<ListObjectFieldsNotNull, List<?>> {

  private String[] fields;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private boolean isOptional;


  @Override
  public void initialize(ListObjectFieldsNotNull constraintAnnotation) {
    this.fields = constraintAnnotation.fields();
    this.isOptional = constraintAnnotation.isOptional();
  }

  @Override
  public boolean isValid(List<?> value, ConstraintValidatorContext context) {
    // Handle optional validation
    if (value == null || value.isEmpty()) {
      return isOptional;
    }

    boolean isValid = true;

    for (int i = 0; i < value.size(); i++) {
      Object obj = value.get(i);
      try {
        for (String field : fields) {
          Object fieldValue = PropertyUtils.getProperty(obj, field);
          if (fieldValue == null || StringUtils.isEmpty(fieldValue.toString())) {
            isValid = false;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
              .addPropertyNode(Integer.toString(i)).addPropertyNode(field)
              .addConstraintViolation();
          }
        }
      } catch (Exception e) {
        // log error or handle exception
        log.info(e.getMessage());
        isValid = false;
      }
    }
    return isValid;
  }

}
