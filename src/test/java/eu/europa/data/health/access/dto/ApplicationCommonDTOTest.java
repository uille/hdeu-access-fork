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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApplicationCommonDTOTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testDefaultConstructor() {
    ApplicationCommonDTO dto = new ApplicationCommonDTO();
    assertNull(dto.getId());
    assertNull(dto.getVersion());
    assertNull(dto.getTitle());
  }

  @Test
  void testSettersAndGetters() {
    ApplicationCommonDTO dto = new ApplicationCommonDTO();
    dto.setId("12345");
    dto.setVersion(1);
    dto.setTitle("Test Title");

    assertEquals("12345", dto.getId());
    assertEquals(1, dto.getVersion());
    assertEquals("Test Title", dto.getTitle());
  }

  @Test
  void testJsonSerialization() throws Exception {
    ApplicationCommonDTO dto = new ApplicationCommonDTO();
    dto.setId("12345");
    dto.setVersion(1);
    dto.setTitle("Test Title");

    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(dto);

    assertTrue(jsonString.contains("\"_id\":\"12345\""));
    assertTrue(jsonString.contains("\"version\":1"));
    assertTrue(jsonString.contains("\"title\":\"Test Title\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String jsonString = "{\"_id\":\"12345\",\"version\":1,\"title\":\"Test Title\"}";

    ObjectMapper mapper = new ObjectMapper();
    ApplicationCommonDTO dto = mapper.readValue(jsonString, ApplicationCommonDTO.class);

    assertEquals("12345", dto.getId());
    assertEquals(1, dto.getVersion());
    assertEquals("Test Title", dto.getTitle());
  }

  @Test
  void testValidation_TitleNotBlank() {
    ApplicationCommonDTO dto = new ApplicationCommonDTO();
    dto.setTitle(""); // Invalid title

    Set<ConstraintViolation<ApplicationCommonDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
      .anyMatch(v -> v.getPropertyPath().toString().equals("title") &&
        v.getMessage().contains("must not be blank")));
  }

  @Test
  void testValidation_TitleMaxSize() {
    ApplicationCommonDTO dto = new ApplicationCommonDTO();
    dto.setTitle("A".repeat(501)); // Exceeding max size

    Set<ConstraintViolation<ApplicationCommonDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
      .anyMatch(v -> v.getPropertyPath().toString().equals("title") &&
        v.getMessage().contains("size must be between")));
  }
}

