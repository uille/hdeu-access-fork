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
import eu.europa.data.health.access.services.TranslationManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckEnumValueValidation implements ConstraintValidator<CheckEnumValue, Object> {

  private String sectionNumber;
  private String fieldName;
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void initialize(CheckEnumValue constraintAnnotation) {
    sectionNumber = constraintAnnotation.sectionNumber();
    fieldName = constraintAnnotation.fieldName();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      if (Objects.isNull(value)) {
        return true;
      }
      List<String> getAllValues = new ArrayList<>();
      if (value instanceof List) {
        List<SelectObjectDTO> list = (List<SelectObjectDTO>) value;
        getAllValues = list.stream().map(SelectObjectDTO::getKey).collect(Collectors.toList());
      } else {
        SelectObjectDTO obj = (SelectObjectDTO) value;
        getAllValues.add(obj.getKey());
      }
      if (CollectionUtils.isEmpty(getAllValues)) {
        return true;
      }
      TranslationManager translationManager = new TranslationManager();

      JsonObject enumValues = translationManager.getTranslation("en", sectionNumber);
      JsonObject fieldValues = enumValues.getJsonObject(fieldName);
      JsonArray items = fieldValues.getJsonArray("items");
      List<String> getAllKeys = extractKeys(items);
      return new HashSet<>(getAllKeys).containsAll(getAllValues);
    } catch (Exception e) {
      // log error or handle exception
      log.info(e.getMessage());
      return false;
    }
  }

  private List<String> extractKeys(JsonArray items) {
    return IntStream.range(0, items.size())
      .mapToObj(i -> items.getJsonObject(i).getString("key"))
      .toList();
  }

}
