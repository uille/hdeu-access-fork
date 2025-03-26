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
package eu.europa.data.health.access.dto.data_access;

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

  class Section4BindSection3DTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    void testValidSection4BindSection3DTO() {
      Section4BindSection3DTO dto = new Section4BindSection3DTO();
      dto.setOperatorIdentifier("Operator123");
      dto.setNameOfTheOrganisation("Org Name");
      dto.setBusinessIdentifierOrganization("12345");
      dto.setVatNumber("VAT12345");
      dto.setPeppolCode("PEPPOL123");

      Set<ConstraintViolation<Section4BindSection3DTO>> violations = validator.validate(dto);
      assertTrue(violations.isEmpty(), "Expected no validation errors for valid DTO.");
    }

    @Test
    void testMissingMandatoryFields() {
      Section4BindSection3DTO dto = new Section4BindSection3DTO();

      Set<ConstraintViolation<Section4BindSection3DTO>> violations = validator.validate(dto);
      assertFalse(violations.isEmpty(), "Expected validation errors for missing mandatory fields.");
      assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
        "Expected 'must not be blank' validation error.");
    }

    @Test
    void testExceedingFieldSizes() {
      Section4BindSection3DTO dto = new Section4BindSection3DTO();
      dto.setOperatorIdentifier("A".repeat(251));
      dto.setNameOfTheOrganisation("B".repeat(251));
      dto.setBusinessIdentifierOrganization("C".repeat(251));
      dto.setVatNumber("D".repeat(501));
      dto.setPeppolCode("E".repeat(251));

      Set<ConstraintViolation<Section4BindSection3DTO>> violations = validator.validate(dto);
      assertFalse(violations.isEmpty(), "Expected validation errors for exceeding field sizes.");
      assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("size must be between")),
        "Expected size validation errors.");
    }

    @Test
    void testInvalidVatNumberFormat() {
      Section4BindSection3DTO dto = new Section4BindSection3DTO();
      dto.setOperatorIdentifier("Operator123");
      dto.setNameOfTheOrganisation("Org Name");
      dto.setBusinessIdentifierOrganization("12345");
      dto.setVatNumber("Invalid#VAT123");

      Set<ConstraintViolation<Section4BindSection3DTO>> violations = validator.validate(dto);
      assertFalse(violations.isEmpty(), "Expected validation errors for invalid VAT number format.");
      assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("alphanumeric")),
        "Expected validation error for non-alphanumeric VAT number.");
    }

    @Test
    void testJsonSerialization() throws Exception {
      Section4BindSection3DTO dto = new Section4BindSection3DTO();
      dto.setOperatorIdentifier("Operator123");
      dto.setNameOfTheOrganisation("Org Name");
      dto.setBusinessIdentifierOrganization("12345");
      dto.setVatNumber("VAT12345");
      dto.setPeppolCode("PEPPOL123");

      String json = objectMapper.writeValueAsString(dto);
      assertTrue(json.contains("\"operatorIdentifier\":\"Operator123\""));
      assertTrue(json.contains("\"nameOfTheOrganisation\":\"Org Name\""));
      assertTrue(json.contains("\"businessIdentifierOrganization\":\"12345\""));
      assertTrue(json.contains("\"vatNumber\":\"VAT12345\""));
      assertTrue(json.contains("\"peppolCode\":\"PEPPOL123\""));
    }

    // Test 6: Validate JSON Deserialization
    @Test
    void testJsonDeserialization() throws Exception {
      String json = """
                {
                  "operatorIdentifier": "Operator123",
                  "nameOfTheOrganisation": "Org Name",
                  "businessIdentifierOrganization": "12345",
                  "vatNumber": "VAT12345",
                  "peppolCode": "PEPPOL123"
                }
                """;

      Section4BindSection3DTO dto = objectMapper.readValue(json, Section4BindSection3DTO.class);

      assertNotNull(dto);
      assertEquals("Operator123", dto.getOperatorIdentifier());
      assertEquals("Org Name", dto.getNameOfTheOrganisation());
      assertEquals("12345", dto.getBusinessIdentifierOrganization());
      assertEquals("VAT12345", dto.getVatNumber());
      assertEquals("PEPPOL123", dto.getPeppolCode());
    }

    // Test 7: Ignore Unknown Fields
    @Test
    void testIgnoreUnknownFields() throws Exception {
      String json = """
                {
                  "operatorIdentifier": "Operator123",
                  "nameOfTheOrganisation": "Org Name",
                  "businessIdentifierOrganization": "12345",
                  "vatNumber": "VAT12345",
                  "peppolCode": "PEPPOL123",
                  "unknownField": "unknownValue"
                }
                """;

      Section4BindSection3DTO dto = objectMapper.readValue(json, Section4BindSection3DTO.class);

      assertNotNull(dto);
      assertEquals("Operator123", dto.getOperatorIdentifier());
      assertEquals("Org Name", dto.getNameOfTheOrganisation());
      assertEquals("12345", dto.getBusinessIdentifierOrganization());
      assertEquals("VAT12345", dto.getVatNumber());
      assertEquals("PEPPOL123", dto.getPeppolCode());
    }
  }
