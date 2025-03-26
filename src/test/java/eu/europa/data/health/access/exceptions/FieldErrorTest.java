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
package eu.europa.data.health.access.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class FieldErrorTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testFieldErrorGetterAndSetter() {
    FieldError fieldError = new FieldError(
      "E001",
      "Invalid input",
      "fieldName",
      "1",
      List.of("param1", "param2"),
      "US",
      "section6SubSectionValue"
    );

    assertEquals("E001", fieldError.getCode());
    assertEquals("Invalid input", fieldError.getErrorMessage());
    assertEquals("fieldName", fieldError.getField());
    assertEquals("1", fieldError.getListIndex());
    assertEquals(List.of("param1", "param2"), fieldError.getParams());
    assertEquals("US", fieldError.getCountryId());
    assertEquals("section6SubSectionValue", fieldError.getSection6SubSection());
  }

  @Test
  void testFieldErrorJsonSerialization() throws JsonProcessingException {
    FieldError fieldError = new FieldError(
      "E002",
      "Field is required",
      "fieldName2",
      null,
      List.of("paramA", "paramB"),
      "UK",
      null
    );

    String json = objectMapper.writeValueAsString(fieldError);
    assertTrue(json.contains("\"code\":\"E002\""));
    assertTrue(json.contains("\"errorMessage\":\"Field is required\""));
    assertTrue(json.contains("\"field\":\"fieldName2\""));
    assertTrue(json.contains("\"params\":[\"paramA\",\"paramB\"]"));
    assertTrue(json.contains("\"countryId\":\"UK\""));
    assertTrue(json.contains("\"listIndex\""));
    assertTrue(json.contains("\"section6SubSection\""));
  }

}
