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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class SelectObjectDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testValidSelectObjectDTO() {
    SelectObjectDTO dto = new SelectObjectDTO();
    dto.setKey("key1");
    dto.setValue("value1");

    // Validate the object
    var violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors.");
  }

  @Test
  void testMissingKey() {
    SelectObjectDTO dto = new SelectObjectDTO();
    dto.setKey(""); // Invalid, blank key
    dto.setValue("value1");

    // Validate the object
    var violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for missing key.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
      "Expected 'must not be blank' error for key.");
  }

  @Test
  void testMissingValue() {
    SelectObjectDTO dto = new SelectObjectDTO();
    dto.setKey("key1");
    dto.setValue(""); // Invalid, blank value

    // Validate the object
    var violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for missing value.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
      "Expected 'must not be blank' error for value.");
  }

  @Test
  void testMissingKeyAndValue() {
    SelectObjectDTO dto = new SelectObjectDTO();
    dto.setKey(""); // Invalid, blank key
    dto.setValue(""); // Invalid, blank value

    // Validate the object
    var violations = validator.validate(dto);
    assertEquals(2, violations.size(), "Expected two validation errors for missing key and value.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    SelectObjectDTO dto = new SelectObjectDTO();
    dto.setKey("key1");
    dto.setValue("value1");

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(dto);

    // Validate JSON output
    assertTrue(json.contains("\"key\":\"key1\""));
    assertTrue(json.contains("\"value\":\"value1\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = "{\"key\":\"key1\", \"value\":\"value1\"}";

    // Deserialize from JSON
    SelectObjectDTO dto = objectMapper.readValue(json, SelectObjectDTO.class);

    // Validate deserialized object
    assertNotNull(dto);
    assertEquals("key1", dto.getKey());
    assertEquals("value1", dto.getValue());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = "{\"key\":\"key1\", \"value\":\"value1\", \"extraField\":\"extraValue\"}";

    // Deserialize from JSON with unknown field
    SelectObjectDTO dto = objectMapper.readValue(json, SelectObjectDTO.class);

    // Validate deserialized object
    assertNotNull(dto);
    assertEquals("key1", dto.getKey());
    assertEquals("value1", dto.getValue());
  }

  @Test
  void testMissingFieldsInJson() throws Exception {
    String json = "{\"key\":\"key1\"}"; // Missing value

    // Deserialize from JSON
    SelectObjectDTO dto = objectMapper.readValue(json, SelectObjectDTO.class);

    // Validate object
    assertNotNull(dto);
    assertEquals("key1", dto.getKey());
    assertNull(dto.getValue());
  }
}
