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
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Section3DTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testValidSection3DTONaturalPerson() {
    Section3DTO dto = new Section3DTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("b", "Natural person"));
    dto.setNaturalPersonName("John Doe");
    dto.setNaturalPersonJobTitle("jotTitle");
    dto.setNaturalPersonAffiliation("Affiliation");
    dto.setNaturalPersonAddress("123 Main St");
    dto.setNaturalPersonCity("City");
    dto.setApplyingForDataOnBehalfOfPublicSector(new SelectObjectDTO("a", "Yes"));
    dto.setApplyingForDataForCarryingOutTasks(new SelectObjectDTO("b", "No"));
    dto.setNaturalPersonZipCode("12345");
    dto.setNaturalPersonCountry(new SelectObjectDTO("US", "United States"));
    dto.setNaturalPersonEmail("john.doe@example.com");
    dto.setNaturalPersonPhone(new PhoneNumberDTO("1", "1234567890"));

    Set<ConstraintViolation<Section3DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for a valid Natural Person DTO.");
  }

  @Test
  void testValidSection3DTOLegalPerson() {
    Section3DTO dto = new Section3DTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("a", "Legal person"));
    dto.setLegalPersonName("Company Inc.");
    dto.setLegalPersonAddress("456 Corporate Blvd");
    dto.setLegalPersonCity("Metropolis");
    dto.setLegalPersonZipCode("67890");
    dto.setLegalPersonCountry(new SelectObjectDTO("US", "United States"));
    dto.setContactPersonName("Jane Doe");
    dto.setContactPersonEmail("jane.doe@example.com");
    dto.setContactPersonPhone(new PhoneNumberDTO("1", "6987654321"));
    dto.setContactPersonOrganisationName("Org Name");
    dto.setContactPersonBusinessID("businessID");
    dto.setContactPersonOperatorID("operatorID");
    dto.setContactPersonAffiliation("affiliation");
    dto.setContactPersonJobTitle("jobTitle");
    dto.setContactPersonRelationship("relationship");
    dto.setApplyingForDataOnBehalfOfPublicSector(new SelectObjectDTO("a", "Yes"));
    dto.setApplyingForDataForCarryingOutTasks(new SelectObjectDTO("b", "No"));

    Set<ConstraintViolation<Section3DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for a valid Legal Person DTO.");
  }

  @Test
  void testMissingFieldsNaturalPerson() {
    Section3DTO dto = new Section3DTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("b", "Natural person"));

    Set<ConstraintViolation<Section3DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing fields.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("naturalPersonName must be provided")),
      "Expected error message for missing naturalPersonName.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("naturalPersonAddress must be provided")),
      "Expected error message for missing naturalPersonAddress.");
  }

  @Test
  void testMissingFieldsLegalPerson() {
    Section3DTO dto = new Section3DTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("a", "Legal person"));

    Set<ConstraintViolation<Section3DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing fields.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("legalPersonName must be provided")),
      "Expected error message for missing legalPersonName.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("legalPersonAddress must be provided")),
      "Expected error message for missing legalPersonAddress.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    Section3DTO dto = new Section3DTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("b", "Natural person"));
    dto.setNaturalPersonName("John Doe");

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"legalOrNaturalPerson\":{"));
    assertTrue(json.contains("\"key\":\"b\""));
    assertTrue(json.contains("\"naturalPersonName\":\"John Doe\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "legalOrNaturalPerson": {"key": "b", "value": "Natural person"},
                  "naturalPersonName": "John Doe"
                }
                """;

    Section3DTO dto = objectMapper.readValue(json, Section3DTO.class);

    assertNotNull(dto);
    assertEquals("b", dto.getLegalOrNaturalPerson().getKey());
    assertEquals("John Doe", dto.getNaturalPersonName());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "legalOrNaturalPerson": {"key": "b", "value": "Natural person"},
                  "naturalPersonName": "John Doe",
                  "unknownField": "unknownValue"
                }
                """;

    Section3DTO dto = objectMapper.readValue(json, Section3DTO.class);

    assertNotNull(dto);
    assertEquals("b", dto.getLegalOrNaturalPerson().getKey());
    assertEquals("John Doe", dto.getNaturalPersonName());
  }
}
