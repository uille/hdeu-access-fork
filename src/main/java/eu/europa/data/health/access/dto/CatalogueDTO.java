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
package eu.europa.data.health.access.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.data.health.access.annotations.NotContainDoubleQuotes;
import eu.europa.data.health.access.annotations.URLFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogueDTO extends RDFCatalogueDTO{
  @NotBlank
  @Size(max = 50)
  @NotContainDoubleQuotes
  String catalogueName;
  @NotBlank
  @Size(max = 250)
  @NotContainDoubleQuotes
  String description;
  @NotNull
  @Valid
  SelectObjectDTO geographicalCoverage;
  @NotBlank
  @Size(max = 250)
  @URLFormat
  @NotContainDoubleQuotes
  String publisherHomepageURL;
  @NotBlank
  @Size(max = 50)
  @NotContainDoubleQuotes
  String publisherName;
  // Fields for the get.
  String createdDate;
  String modifiedDate;
  Integer datasets=0;

  public int compareByField(String fieldName, Object otherObject) {
    try {
      // Look for the getter method in the current class or superclass.
      Method getterMethod = this.getClass().getMethod("get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
      Object value1 = getterMethod.invoke(this);
      Object value2 = getterMethod.invoke(otherObject);

      // If the field is of type SelectObjectDTO, compare by value.
      if (value1 instanceof SelectObjectDTO selectObjectValue1 && value2 instanceof SelectObjectDTO selectObjectValue2) {
        String valueStr1 = selectObjectValue1.getValue();
        String valueStr2 = selectObjectValue2.getValue();

        if (valueStr1 == null && valueStr2 == null) return 0;
        if (valueStr1 == null) return -1;
        if (valueStr2 == null) return 1;

        return valueStr1.compareTo(valueStr2);
      }

      // Compare the values if they are Comparable
      if (value1 instanceof Comparable && value2 instanceof Comparable) {
        return ((Comparable) value1).compareTo(value2);
      }
      return 0;
    } catch (Exception e) {
      throw new RuntimeException("Error in comparison: " + e.getMessage());
    }
  }
}

