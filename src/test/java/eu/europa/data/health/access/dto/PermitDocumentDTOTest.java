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

class PermitDocumentDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();


  @Test
  void testValidPermitDocumentDTO() {
    PermitDocumentDTO dto = new PermitDocumentDTO();
    dto.setPermitIssuer("Valid Issuer");
    dto.setPermitStartDateOfIssue("2023-01-01");
    dto.setPermitEndDateOfIssue("2023-12-31");
    dto.setPermitIdentificationInformation("ABC123");

    Set<ConstraintViolation<PermitDocumentDTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors.");
  }

  @Test
  void testPermitEndDateBeforeStartDate() {
    PermitDocumentDTO dto = new PermitDocumentDTO();
    dto.setPermitIssuer("Valid Issuer");
    dto.setPermitStartDateOfIssue("2023-12-31");
    dto.setPermitEndDateOfIssue("2023-01-01"); // Invalid
    dto.setPermitIdentificationInformation("ABC123");

    Set<ConstraintViolation<PermitDocumentDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for end date before start date.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Date must be higher than starting day")),
      "Expected message for end date being lower than start date.");
  }

  @Test
  void testPermitStartDateInFuture() {
    PermitDocumentDTO dto = new PermitDocumentDTO();
    dto.setPermitIssuer("Valid Issuer");
    dto.setPermitStartDateOfIssue("2099-01-01"); // Invalid, in the future
    dto.setPermitEndDateOfIssue("2099-12-31");
    dto.setPermitIdentificationInformation("ABC123");

    Set<ConstraintViolation<PermitDocumentDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for start date in the future.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Date must be lower than today")),
      "Expected message for start date being in the future.");
  }

  @Test
  void testInvalidPermitIdentificationInformation() {
    PermitDocumentDTO dto = new PermitDocumentDTO();
    dto.setPermitIssuer("Valid Issuer");
    dto.setPermitStartDateOfIssue("2023-01-01");
    dto.setPermitEndDateOfIssue("2023-12-31");
    dto.setPermitIdentificationInformation("Invalid#123"); // Invalid, contains non-alphanumeric

    Set<ConstraintViolation<PermitDocumentDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for invalid permit identification information.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("alphanumeric")),
      "Expected message for non-alphanumeric format.");
  }

  @Test
  void testFieldSizeValidation() {
    PermitDocumentDTO dto = new PermitDocumentDTO();
    dto.setPermitIssuer("A".repeat(501)); // Exceeds @Size(max = 500)
    dto.setPermitStartDateOfIssue("2023-01-01");
    dto.setPermitEndDateOfIssue("2023-12-31");
    dto.setPermitIdentificationInformation("ABC123");

    Set<ConstraintViolation<PermitDocumentDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for exceeding field size.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("size must be between")),
      "Expected size validation message.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    PermitDocumentDTO dto = new PermitDocumentDTO();
    dto.setPermitIssuer("Valid Issuer");
    dto.setPermitStartDateOfIssue("2023-01-01");
    dto.setPermitEndDateOfIssue("2023-12-31");
    dto.setPermitIdentificationInformation("ABC123");

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"permitIssuer\":\"Valid Issuer\""));
    assertTrue(json.contains("\"permitStartDateOfIssue\":\"2023-01-01\""));
    assertTrue(json.contains("\"permitEndDateOfIssue\":\"2023-12-31\""));
    assertTrue(json.contains("\"permitIdentificationInformation\":\"ABC123\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "permitIssuer": "Valid Issuer",
                  "permitStartDateOfIssue": "2023-01-01",
                  "permitEndDateOfIssue": "2023-12-31",
                  "permitIdentificationInformation": "ABC123"
                }
                """;

    PermitDocumentDTO dto = objectMapper.readValue(json, PermitDocumentDTO.class);

    assertNotNull(dto);
    assertEquals("Valid Issuer", dto.getPermitIssuer());
    assertEquals("2023-01-01", dto.getPermitStartDateOfIssue());
    assertEquals("2023-12-31", dto.getPermitEndDateOfIssue());
    assertEquals("ABC123", dto.getPermitIdentificationInformation());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "permitIssuer": "Valid Issuer",
                  "permitStartDateOfIssue": "2023-01-01",
                  "permitEndDateOfIssue": "2023-12-31",
                  "unknownField": "ignoredValue"
                }
                """;

    PermitDocumentDTO dto = objectMapper.readValue(json, PermitDocumentDTO.class);

    assertNotNull(dto);
    assertEquals("Valid Issuer", dto.getPermitIssuer());
    assertEquals("2023-01-01", dto.getPermitStartDateOfIssue());
    assertEquals("2023-12-31", dto.getPermitEndDateOfIssue());
  }
}
