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

public class RequiredFieldBasedOnAndOrObjectValueValidator implements
  ConstraintValidator<RequiredFieldBasedOnAndOrObjectValue, Object> {
  private String objectName;
  private String baseField;
  private String objectNameWithOr;
  private String baseFieldWithOr;
  private String objectNameWithAnd;
  private String baseFieldWithAnd;
  private String mandatoryFieldType;
  private String baseFieldValue;
  private String baseFieldValueWithOr;
  private String baseFieldValueWithAnd;
  private String mandatoryField;
  private final Logger log = LoggerFactory.getLogger(getClass());


  @Override
  public void initialize(RequiredFieldBasedOnAndOrObjectValue constraintAnnotation) {
    this.objectName = constraintAnnotation.objectName();
    this.objectNameWithOr = constraintAnnotation.objectNameWithOr();
    this.objectNameWithAnd = constraintAnnotation.objectNameWithAnd();
    this.baseField = constraintAnnotation.baseField();
    this.baseFieldWithOr = constraintAnnotation.baseFieldWithOr();
    this.baseFieldWithAnd = constraintAnnotation.baseFieldWithAnd();
    this.baseFieldValue = constraintAnnotation.baseFieldValue();
    this.baseFieldValueWithOr = constraintAnnotation.baseFieldValueWithOr();
    this.baseFieldValueWithAnd = constraintAnnotation.baseFieldValueWithAnd();
    this.mandatoryField = constraintAnnotation.mandatoryField();
    this.mandatoryFieldType = constraintAnnotation.mandatoryFieldType();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      Object obj = PropertyUtils.getProperty(value, this.objectName);
      Object objWithOr = PropertyUtils.getProperty(value, this.objectNameWithOr);
      Object objWithAnd = PropertyUtils.getProperty(value, this.objectNameWithAnd);
      if((Objects.isNull(obj) && Objects.isNull(objWithOr)) || Objects.isNull(objWithAnd)){
        return true;
      }
      String baseFieldVal = "";
      if(obj != null){
        baseFieldVal = (String) PropertyUtils.getProperty(obj, this.baseField);
      }
      String baseFieldValWithOr = "";
      if(objWithOr != null){
        baseFieldValWithOr = (String) PropertyUtils.getProperty(objWithOr, this.baseFieldWithOr);
      }
      String baseFieldValWithAnd = (String) PropertyUtils.getProperty(objWithAnd, this.baseFieldWithAnd);
      if ((Objects.equals(this.baseFieldValue, baseFieldVal) || Objects.equals(this.baseFieldValueWithOr, baseFieldValWithOr))
        && Objects.equals(this.baseFieldValueWithAnd, baseFieldValWithAnd)) {
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
        else if(mandatoryFieldType.equals("listOfAttachment")) {
          List<AttachmentDTO> mandatoryFieldObject = (List<AttachmentDTO>) PropertyUtils.getProperty(value,
            this.mandatoryField);
          return CollectionUtils.isNotEmpty(mandatoryFieldObject);
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
