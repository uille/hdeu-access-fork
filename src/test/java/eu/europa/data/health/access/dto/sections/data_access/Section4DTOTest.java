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
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.Section4DTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Section4DTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testValidSection4DTO() {
    Section4DTO dto = new Section4DTO();
    dto.setFullName("John Doe");
    dto.setAddress("123 Main St");
    dto.setPhone(new PhoneNumberDTO("1", "1234567890"));
    dto.setEmail("john.doe@example.com");
    dto.setInvoiceType(new SelectObjectDTO("a", "Paper"));
    dto.setInvoiceReferenceNumber("INV12345");
    dto.setIsTheProjectFinanciallyCovered(new SelectObjectDTO("a", "Yes"));
    dto.setRangeOfAmountOfFinancing(new SelectObjectDTO("a", "40"));

    Set<ConstraintViolation<Section4DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for valid DTO.");
  }

  @Test
  void testMissingMandatoryFields() {
    Section4DTO dto = new Section4DTO();

    Set<ConstraintViolation<Section4DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing mandatory fields.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
      "Expected 'must not be blank' validation error.");
  }

  @Test
  void testConditionalFieldValidationInvoiceAddress() {
    Section4DTO dto = new Section4DTO();
    dto.setFullName("John Doe");
    dto.setAddress("123 Main St");
    dto.setPhone(new PhoneNumberDTO("1", "1234567890"));
    dto.setEmail("john.doe@example.com");
    dto.setInvoiceType(new SelectObjectDTO("b", "Electronic")); // Requires invoiceAddress
    dto.setInvoiceReferenceNumber("INV12345");

    Set<ConstraintViolation<Section4DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing invoiceAddress.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("invoiceAddress must be provided")),
      "Expected validation error for missing invoiceAddress.");
  }

  @Test
  void testConditionalFieldValidationRangeOfAmountOfFinancing() {
    Section4DTO dto = new Section4DTO();
    dto.setFullName("John Doe");
    dto.setAddress("123 Main St");
    dto.setPhone(new PhoneNumberDTO("1", "1234567890"));
    dto.setEmail("john.doe@example.com");
    dto.setInvoiceType(new SelectObjectDTO("a", "Paper"));
    dto.setInvoiceReferenceNumber("INV12345");
    dto.setIsTheProjectFinanciallyCovered(new SelectObjectDTO("a", "Yes")); // Requires rangeOfAmountOfFinancing

    Set<ConstraintViolation<Section4DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing rangeOfAmountOfFinancing.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("rangeOfAmountOfFinancing must be provided")),
      "Expected validation error for missing rangeOfAmountOfFinancing.");
  }

  @Test
  void testInvalidEmailFormat() {
    Section4DTO dto = new Section4DTO();
    dto.setFullName("John Doe");
    dto.setAddress("123 Main St");
    dto.setPhone(new PhoneNumberDTO("1", "1234567890"));
    dto.setEmail("invalid-email"); // Invalid email format
    dto.setInvoiceType(new SelectObjectDTO("a", "Paper"));
    dto.setInvoiceReferenceNumber("INV12345");
    dto.setIsTheProjectFinanciallyCovered(new SelectObjectDTO("a", "Yes"));
    dto.setRangeOfAmountOfFinancing(new SelectObjectDTO("1000-5000", "1,000 to 5,000"));

    Set<ConstraintViolation<Section4DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for invalid email format.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("email format")),
      "Expected validation error for invalid email format.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    Section4DTO dto = new Section4DTO();
    dto.setFullName("John Doe");
    dto.setAddress("123 Main St");
    dto.setPhone(new PhoneNumberDTO("1", "1234567890"));
    dto.setEmail("john.doe@example.com");
    dto.setInvoiceType(new SelectObjectDTO("a", "Paper"));
    dto.setInvoiceReferenceNumber("INV12345");

    String json = objectMapper.writeValueAsString(dto);
    assertTrue(json.contains("\"fullName\":\"John Doe\""));
    assertTrue(json.contains("\"address\":\"123 Main St\""));
    assertTrue(json.contains("\"email\":\"john.doe@example.com\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "fullName": "John Doe",
                  "address": "123 Main St",
                  "phone": {"countryCode": "1", "number": "1234567890"},
                  "email": "john.doe@example.com",
                  "invoiceType": {"key": "a", "value": "Paper"},
                  "invoiceReferenceNumber": "INV12345"
                }
                """;

    Section4DTO dto = objectMapper.readValue(json, Section4DTO.class);

    assertNotNull(dto);
    assertEquals("John Doe", dto.getFullName());
    assertEquals("123 Main St", dto.getAddress());
    assertEquals("1", dto.getPhone().getCountryCode());
    assertEquals("1234567890", dto.getPhone().getNumber());
    assertEquals("a", dto.getInvoiceType().getKey());
    assertEquals("INV12345", dto.getInvoiceReferenceNumber());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "fullName": "John Doe",
                  "address": "123 Main St",
                  "unknownField": "unknownValue"
                }
                """;

    Section4DTO dto = objectMapper.readValue(json, Section4DTO.class);

    assertNotNull(dto);
    assertEquals("John Doe", dto.getFullName());
    assertEquals("123 Main St", dto.getAddress());
  }
}
