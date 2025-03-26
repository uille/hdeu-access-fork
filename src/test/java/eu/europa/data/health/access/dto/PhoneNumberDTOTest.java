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
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class PhoneNumberDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void testValidPhoneNumberDTO() {
    PhoneNumberDTO phoneNumberDTO = new PhoneNumberDTO();
    phoneNumberDTO.setCountryCode("US");
    phoneNumberDTO.setNumber("6976969896");

    // Validate the object
    var violations = validator.validate(phoneNumberDTO);
    assertTrue(violations.isEmpty(), "Expected no validation errors.");
  }

  @Test
  void testMissingCountryCode() {
    PhoneNumberDTO phoneNumberDTO = new PhoneNumberDTO();
    phoneNumberDTO.setCountryCode(""); // Invalid, blank countryCode
    phoneNumberDTO.setNumber("+1234567890");

    // Validate the object
    var violations = validator.validate(phoneNumberDTO);
    assertFalse(violations.isEmpty(), "Expected validation error for missing countryCode.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
      "Expected 'must not be blank' error for countryCode.");
  }

  @Test
  void testMissingPhoneNumber() {
    PhoneNumberDTO phoneNumberDTO = new PhoneNumberDTO();
    phoneNumberDTO.setCountryCode("US");
    phoneNumberDTO.setNumber(""); // Invalid, blank number

    // Validate the object
    var violations = validator.validate(phoneNumberDTO);
    assertFalse(violations.isEmpty(), "Expected validation error for missing number.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")),
      "Expected 'must not be blank' error for number.");
  }

  @Test
  void testInvalidPhoneNumberFormat() {
    PhoneNumberDTO phoneNumberDTO = new PhoneNumberDTO();
    phoneNumberDTO.setCountryCode("US");
    phoneNumberDTO.setNumber("123asdasd45"); // Invalid, wrong phone format

    // Validate the object (PhoneFormat group should catch this)
    var violations = validator.validate(phoneNumberDTO);
    assertFalse(violations.isEmpty(), "Expected validation error for phone number format.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessageTemplate().contains("Invalid phone format")),
      "Expected 'Invalid phone format' error for number.");
  }

  @Test
  void testPhoneNumberDTOSerialization() throws Exception {
    PhoneNumberDTO phoneNumberDTO = new PhoneNumberDTO();
    phoneNumberDTO.setCountryCode("US");
    phoneNumberDTO.setNumber("+1234567890");

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(phoneNumberDTO);

    // Check if the serialized JSON contains the correct fields
    assertTrue(json.contains("\"countryCode\":\"US\""));
    assertTrue(json.contains("\"number\":\"+1234567890\""));
  }

  @Test
  void testPhoneNumberDTODeserialization() throws Exception {
    String json = "{\"countryCode\":\"US\", \"number\": \"+1234567890\"}";

    // Deserialize from JSON
    PhoneNumberDTO phoneNumberDTO = objectMapper.readValue(json, PhoneNumberDTO.class);

    // Check if the fields were correctly deserialized
    assertEquals("US", phoneNumberDTO.getCountryCode());
    assertEquals("+1234567890", phoneNumberDTO.getNumber());
  }

  @Test
  void testIgnoreUnknownFieldsInJson() throws Exception {
    String json = "{\"countryCode\":\"US\", \"number\": \"+1234567890\", \"extraField\":\"value\"}";

    // Deserialize from JSON with extra field
    PhoneNumberDTO phoneNumberDTO = objectMapper.readValue(json, PhoneNumberDTO.class);

    // Check if the extra field is ignored and the object is still correctly deserialized
    assertEquals("US", phoneNumberDTO.getCountryCode());
    assertEquals("+1234567890", phoneNumberDTO.getNumber());
  }

  @Test
  void testMissingFieldsInJson() throws Exception {
    String json = "{\"countryCode\":\"US\"}"; // Missing number

    // Deserialize from JSON
    PhoneNumberDTO phoneNumberDTO = objectMapper.readValue(json, PhoneNumberDTO.class);

    // Check if missing fields result in null (for the number field)
    assertEquals("US", phoneNumberDTO.getCountryCode());
    assertNull(phoneNumberDTO.getNumber());
  }
}
