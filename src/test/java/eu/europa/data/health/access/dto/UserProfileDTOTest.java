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

class UserProfileDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testNaturalPersonValidation() {
    UserProfileDTO dto = new UserProfileDTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("b", "Natural person"));
    dto.setNaturalPersonJobTitle("Engineer");
    dto.setNaturalPersonAffiliation("Company A");
    dto.setNaturalPersonAddress("123 Street, City");
    dto.setNaturalPersonZipCode("12345");
    dto.setNaturalPersonCity("Some City");
    dto.setNaturalPersonCountry(new SelectObjectDTO("US", "United States"));
    dto.setNaturalPersonPhone(new PhoneNumberDTO("1", "1234567890"));

    Set violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for valid natural person fields.");
  }

  @Test
  void testMissingNaturalPersonFields() {
    UserProfileDTO dto = new UserProfileDTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("b", "Natural person"));

    Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing natural person fields.");

    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("naturalPersonJobTitle")),
      "Expected validation error for missing naturalPersonJobTitle.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("naturalPersonAffiliation")),
      "Expected validation error for missing naturalPersonAffiliation.");
  }

  @Test
  void testLegalPersonValidation() {
    UserProfileDTO dto = new UserProfileDTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("a", "Legal person"));
    dto.setLegalPersonOrganisationName("Organization A");
    dto.setLegalPersonBusinessID("BUS123");
    dto.setLegalPersonOperatorID("OP123");
    dto.setLegalPersonJobTitle("CEO");
    dto.setLegalPersonAffiliation("Affiliate B");
    dto.setLegalPersonAddress("456 Avenue, City");
    dto.setLegalPersonZipCode("54321");
    dto.setLegalPersonCity("Another City");
    dto.setLegalPersonCountry(new SelectObjectDTO("CA", "Canada"));
    dto.setLegalPersonPhone(new PhoneNumberDTO("1", "6987654321"));

    Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for valid legal person fields.");
  }

  @Test
  void testMissingLegalPersonFields() {
    UserProfileDTO dto = new UserProfileDTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("a", "Legal person"));

    Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for missing legal person fields.");

    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("legalPersonOrganisationName")),
      "Expected validation error for missing legalPersonOrganisationName.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("legalPersonBusinessID")),
      "Expected validation error for missing legalPersonBusinessID.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    UserProfileDTO dto = new UserProfileDTO();
    dto.setLegalOrNaturalPerson(new SelectObjectDTO("b", "Natural person"));
    dto.setNaturalPersonJobTitle("Engineer");

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"legalOrNaturalPerson\":{\"key\":\"b\""));
    assertTrue(json.contains("\"naturalPersonJobTitle\":\"Engineer\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "euLoginID": "user123",
                  "legalOrNaturalPerson": {"key": "b", "value": "Natural person"},
                  "naturalPersonJobTitle": "Engineer"
                }
                """;

    UserProfileDTO dto = objectMapper.readValue(json, UserProfileDTO.class);

    assertNotNull(dto);
    assertEquals("user123", dto.getEuLoginID());
    assertEquals("b", dto.getLegalOrNaturalPerson().getKey());
    assertEquals("Engineer", dto.getNaturalPersonJobTitle());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "euLoginID": "user123",
                  "legalOrNaturalPerson": {"key": "a", "value": "Legal person"},
                  "extraField": "extraValue"
                }
                """;

    UserProfileDTO dto = objectMapper.readValue(json, UserProfileDTO.class);

    assertNotNull(dto);
    assertEquals("user123", dto.getEuLoginID());
    assertEquals("a", dto.getLegalOrNaturalPerson().getKey());
  }
}

