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
package eu.europa.data.health.access.dto.sections.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.Section2DTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Section2DTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testValidSection2DTO() {
    Section2DTO dto = new Section2DTO();
    dto.setProjectName("Project A");
    dto.setProjectLeader("Leader A");
    dto.setCountryOfProjectLeader(new SelectObjectDTO("US", "United States"));
    dto.setPurposeForWhichDataWillBeUsed(List.of(new SelectObjectDTO("a", "a. value")));
    dto.setSummaryOfTheProject("Summary A");

    Set<ConstraintViolation<Section2DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors.");
  }

  @Test
  void testMissingMandatoryFields() {
    Section2DTO dto = new Section2DTO();
    dto.setProjectLeader("Leader A");

    Set<ConstraintViolation<Section2DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing fields.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
      "Expected 'must not be blank' validation error.");
  }

  @Test
  void testConditionalFieldValidation() {
    Section2DTO dto = new Section2DTO();
    dto.setProjectName("Project A");
    dto.setProjectLeader("Leader A");
    dto.setCountryOfProjectLeader(new SelectObjectDTO("US", "United States"));

    Set<ConstraintViolation<Section2DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for conditional fields.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("summaryOfTheProject must be provided")),
      "Expected validation message for missing summaryOfTheProject.");
  }

  @Test
  void testListContainsSpecificValueValidation() {
    Section2DTO dto = new Section2DTO();
    dto.setProjectName("Project A");
    dto.setProjectLeader("Leader A");
    dto.setCountryOfProjectLeader(new SelectObjectDTO("US", "United States"));

    Set<ConstraintViolation<Section2DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing other field.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("summaryOfTheProject must be provided when theNatureOfTheDataDoesNotLetYouProvideADescription is false")),
      "Expected validation message for missing other field.");
  }

  @Test
  void testFieldSizeValidation() {
    Section2DTO dto = new Section2DTO();
    dto.setProjectName("A".repeat(501));
    dto.setProjectLeader("Leader A");

    Set<ConstraintViolation<Section2DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for exceeding field size.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("size must be between")),
      "Expected size validation error.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    Section2DTO dto = new Section2DTO();
    dto.setProjectName("Project A");
    dto.setProjectLeader("Leader A");
    dto.setCountryOfProjectLeader(new SelectObjectDTO("US", "United States"));

    String json = objectMapper.writeValueAsString(dto);
    assertTrue(json.contains("\"projectName\":\"Project A\""));
    assertTrue(json.contains("\"projectLeader\":\"Leader A\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "projectName": "Project A",
                  "projectLeader": "Leader A",
                  "countryOfProjectLeader": {"key": "US", "value": "United States"}
                }
                """;

    Section2DTO dto = objectMapper.readValue(json, Section2DTO.class);

    assertNotNull(dto);
    assertEquals("Project A", dto.getProjectName());
    assertEquals("Leader A", dto.getProjectLeader());
    assertEquals("US", dto.getCountryOfProjectLeader().getKey());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "projectName": "Project A",
                  "projectLeader": "Leader A",
                  "unknownField": "value"
                }
                """;

    Section2DTO dto = objectMapper.readValue(json, Section2DTO.class);

    assertNotNull(dto);
    assertEquals("Project A", dto.getProjectName());
    assertEquals("Leader A", dto.getProjectLeader());
  }
}
