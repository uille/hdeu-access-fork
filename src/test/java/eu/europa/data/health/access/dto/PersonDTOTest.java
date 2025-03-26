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

class PersonDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testValidPersonDTO() {
    PersonDTO person = new PersonDTO();
    person.setFullName("John Doe");
    person.setAffiliation("Some Organization");
    person.setEmailAddress("john.doe@example.com");

    Set violations = validator.validate(person);
    assertTrue(violations.isEmpty(), "Expected no validation errors.");
  }

  @Test
  void testMissingOptionalFields() {
    PersonDTO person = new PersonDTO();
    person.setFullName("John Doe");
    person.setEmailAddress("john.doe@example.com");

    Set violations = validator.validate(person);
    assertTrue(violations.isEmpty(), "Expected no validation errors for missing optional fields.");
  }

  @Test
  void testInvalidEmailAddress() {
    PersonDTO person = new PersonDTO();
    person.setFullName("John Doe");
    person.setAffiliation("Some Organization");
    person.setEmailAddress("invalid-email");

    Set<ConstraintViolation<PersonDTO>> violations = validator.validate(person);
    assertFalse(violations.isEmpty(), "Expected validation error for invalid email address.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Invalid email format")),
      "Expected validation message for invalid email address.");
  }

  @Test
  void testFieldSizeExceedingLimits() {
    PersonDTO person = new PersonDTO();
    person.setFullName("A".repeat(501)); // Exceeds @Size(max = 500)
    person.setAffiliation("B".repeat(501)); // Exceeds @Size(max = 500)
    person.setEmailAddress("C".repeat(490) + "@example.com"); // Exceeds @Size(max = 500)

    Set<ConstraintViolation<PersonDTO>> violations = validator.validate(person);
    assertFalse(violations.isEmpty(), "Expected validation errors for exceeding field sizes.");

    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("size must be between")),
      "Expected size validation message.");
  }

  @Test
  void testJsonSerialization() throws Exception {
    PersonDTO person = new PersonDTO();
    person.setFullName("Jane Doe");
    person.setAffiliation("Another Organization");
    person.setEmailAddress("jane.doe@example.com");

    String json = objectMapper.writeValueAsString(person);

    assertTrue(json.contains("\"fullName\":\"Jane Doe\""));
    assertTrue(json.contains("\"affiliation\":\"Another Organization\""));
    assertTrue(json.contains("\"emailAddress\":\"jane.doe@example.com\""));
  }

  @Test
  void testJsonDeserialization() throws Exception {
    String json = """
                {
                  "fullName": "Jane Doe",
                  "affiliation": "Another Organization",
                  "emailAddress": "jane.doe@example.com"
                }
                """;

    PersonDTO person = objectMapper.readValue(json, PersonDTO.class);

    assertNotNull(person);
    assertEquals("Jane Doe", person.getFullName());
    assertEquals("Another Organization", person.getAffiliation());
    assertEquals("jane.doe@example.com", person.getEmailAddress());
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = """
                {
                  "fullName": "John Doe",
                  "affiliation": "Some Organization",
                  "emailAddress": "john.doe@example.com",
                  "unknownField": "ignored"
                }
                """;

    PersonDTO person = objectMapper.readValue(json, PersonDTO.class);

    assertNotNull(person);
    assertEquals("John Doe", person.getFullName());
    assertEquals("Some Organization", person.getAffiliation());
    assertEquals("john.doe@example.com", person.getEmailAddress());
  }
}
