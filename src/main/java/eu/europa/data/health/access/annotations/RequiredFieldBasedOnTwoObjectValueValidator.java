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

import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequiredFieldBasedOnTwoObjectValueValidator implements
  ConstraintValidator<RequiredFieldBasedOnTwoObjectValue, Object> {
  private String objectName;
  private String baseField;
  private String objectName2;
  private String baseField2;
  private String mandatoryFieldType;
  private String baseFieldValue;
  private String baseFieldValue2;
  private String mandatoryField;
  private final Logger log = LoggerFactory.getLogger(getClass());


  @Override
  public void initialize(RequiredFieldBasedOnTwoObjectValue constraintAnnotation) {
    this.objectName = constraintAnnotation.objectName();
    this.objectName2 = constraintAnnotation.objectName2();
    this.baseField = constraintAnnotation.baseField();
    this.baseField2 = constraintAnnotation.baseField2();
    this.baseFieldValue = constraintAnnotation.baseFieldValue();
    this.baseFieldValue2 = constraintAnnotation.baseFieldValue2();
    this.mandatoryField = constraintAnnotation.mandatoryField();
    this.mandatoryFieldType = constraintAnnotation.mandatoryFieldType();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      Object obj = PropertyUtils.getProperty(value, this.objectName);
      Object obj2 = PropertyUtils.getProperty(value, this.objectName2);
      if(Objects.isNull(obj) || Objects.isNull(obj2)){
        return true;
      }
      String baseFieldVal = (String) PropertyUtils.getProperty(obj, this.baseField);
      String baseFieldVal2 = (String) PropertyUtils.getProperty(obj2, this.baseField2);
      if (Objects.equals(this.baseFieldValue, baseFieldVal) && Objects.equals(this.baseFieldValue2, baseFieldVal2)) {
        if(mandatoryFieldType.equals("boolean")){
          return BooleanUtils.isTrue((boolean)PropertyUtils.getProperty(value, this.mandatoryField));
        }else if(mandatoryFieldType.equals("object")) {
          SelectObjectDTO mandatoryFieldObject = (SelectObjectDTO) PropertyUtils.getProperty(value, this.mandatoryField);
          return !Objects.isNull(mandatoryFieldObject) && !mandatoryFieldObject.getKey().isEmpty();
        }else if (mandatoryFieldType.equals("objectList")){
          List<SelectObjectDTO> list = (List<SelectObjectDTO>) PropertyUtils.getProperty(value, this.mandatoryField);
          List<String> getAllKeyValues = list.stream().map(SelectObjectDTO::getKey).toList();
          return CollectionUtils.isNotEmpty(getAllKeyValues);
        }else if(mandatoryFieldType.equals("attachment")) {
          AttachmentDTO mandatoryFieldObject = (AttachmentDTO) PropertyUtils.getProperty(value,
            this.mandatoryField);
          return !Objects.isNull(mandatoryFieldObject);
        }
        else if(mandatoryFieldType.equals("phone")) {
          PhoneNumberDTO mandatoryFieldObject = (PhoneNumberDTO) PropertyUtils.getProperty(value,
            this.mandatoryField);
          return !Objects.isNull(mandatoryFieldObject);
        }
        else {
          String mandatoryFieldValue = (String) PropertyUtils.getProperty(value, this.mandatoryField);
          return !Objects.isNull(mandatoryFieldValue) && !mandatoryFieldValue.trim().isEmpty();
        }
      }
      return true;
    } catch (Exception e) {
      log.info(e.getMessage());
      return false;
    }
  }
}
