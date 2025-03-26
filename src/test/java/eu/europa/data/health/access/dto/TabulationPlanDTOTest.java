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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TabulationPlanDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testDefaultConstructorAndAccessors() {
    TabulationPlanDTO dto = new TabulationPlanDTO();
    dto.setCountryId("123");
    dto.setListIndex(1);
    dto.setTabulationRegisteredToBeUsed("Study cohort info");

    assertEquals("123", dto.getCountryId());
    assertEquals(1, dto.getListIndex());
    assertEquals("Study cohort info", dto.getTabulationRegisteredToBeUsed());
  }


  @Test
  void testFieldSizeValidation() {
    TabulationPlanDTO dto = new TabulationPlanDTO();
    dto.setTabulationRegisteredToBeUsed("A".repeat(501)); // Exceeds @Size(max = 500)
    dto.setTabulationInformationOfRequiredVariables("B".repeat(1001)); // Exceeds @Size(max = 1000)

    // Validate the object
    Set<ConstraintViolation<TabulationPlanDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for field size constraints.");

    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("size must be between")),
      "Expected size validation errors.");
  }

  @Test
  void testAttachmentValidation() {
    TabulationPlanDTO dto = new TabulationPlanDTO();
    AttachmentDTO invalidAttachment = new AttachmentDTO();
    invalidAttachment.setName("invalid.txt"); // Invalid extension
    invalidAttachment.setSize(6_000_000.0); // Exceeds maxSize = 5MB
    dto.setTabulationPlan(invalidAttachment);

    // Validate the object
    Set<ConstraintViolation<TabulationPlanDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for attachment constraints.");

    assertTrue(violations.stream().anyMatch(v ->
        v.getMessage().contains("Invalid file extension: txt")),
      "Expected validation error for attachment extension.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    TabulationPlanDTO dto = new TabulationPlanDTO();
    dto.setCountryId("789");
    dto.setListIndex(3);
    dto.setTabulationRegisteredToBeUsed("Registered cohort");

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"country_id\":\"789\""));
    assertTrue(json.contains("\"list_index\":3"));
    assertTrue(json.contains("\"tabulationRegisteredToBeUsed\":\"Registered cohort\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "country_id": "789",
                  "list_index": 3,
                  "tabulationRegisteredToBeUsed": "Registered cohort"
                }
                """;

    TabulationPlanDTO dto = objectMapper.readValue(json, TabulationPlanDTO.class);

    assertNotNull(dto);
    assertEquals("789", dto.getCountryId());
    assertEquals(3, dto.getListIndex());
    assertEquals("Registered cohort", dto.getTabulationRegisteredToBeUsed());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "country_id": "123",
                  "list_index": 4,
                  "extra_field": "ignored"
                }
                """;

    TabulationPlanDTO dto = objectMapper.readValue(json, TabulationPlanDTO.class);

    assertNotNull(dto);
    assertEquals("123", dto.getCountryId());
    assertEquals(4, dto.getListIndex());
  }

  @Test
  void testMissingFieldsValidation() {
    TabulationPlanDTO dto = new TabulationPlanDTO();

    // Validate the object
    Set violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for missing optional fields.");
  }
}

