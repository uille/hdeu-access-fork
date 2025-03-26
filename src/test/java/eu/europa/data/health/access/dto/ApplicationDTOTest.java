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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApplicationDTOTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testDefaultConstructor() {
    ApplicationDTO dto = new ApplicationDTO();
    assertNull(dto.getId());
    assertNull(dto.getVersion());
    assertNull(dto.getTitle());
    assertNull(dto.getStatus());
    assertNull(dto.getDateCreated());
    assertNull(dto.getDateModified());
    assertNull(dto.getDatasets());
  }

  @Test
  void testSettersAndGetters() {
    ApplicationDTO dto = new ApplicationDTO();

    dto.setId("app123");
    dto.setVersion(1);
    dto.setTitle("Application Title");
    dto.setStatus(new ApplicationStatusDTO(ApplicationStatusEnum.DRAFT, new Date(), "user123"));
    dto.setDateCreated(new Date());
    dto.setDateModified(new Date());
    dto.setDatasets(List.of(new DatasetDTO("dataset1", "Dataset Title 1"),
      new DatasetDTO("dataset2", "Dataset Title 2")));

    assertEquals("app123", dto.getId());
    assertEquals(1, dto.getVersion());
    assertEquals("Application Title", dto.getTitle());
    assertNotNull(dto.getStatus());
    assertNotNull(dto.getDateCreated());
    assertNotNull(dto.getDateModified());
    assertNotNull(dto.getDatasets());
    assertEquals(2, dto.getDatasets().size());
  }

  @Test
  void testJsonSerialization() throws Exception {
    ApplicationDTO dto = new ApplicationDTO();
    dto.setId("app123");
    dto.setVersion(1);
    dto.setTitle("Application Title");
    dto.setStatus(new ApplicationStatusDTO(ApplicationStatusEnum.DRAFT, new Date(), "user123"));
    dto.setDateCreated(new Date(1683110400000L)); // Fixed date for testing
    dto.setDateModified(new Date(1683196800000L));
    dto.setDatasets(List.of(new DatasetDTO("dataset1", "Dataset Title 1")));

    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(dto);

    assertTrue(jsonString.contains("\"_id\":\"app123\""));
    assertTrue(jsonString.contains("\"title\":\"Application Title\""));
    assertTrue(jsonString.contains("\"status\":"));
    assertTrue(jsonString.contains("\"date_created\":1683110400000"));
    assertTrue(jsonString.contains("\"date_modified\":1683196800000"));
    assertTrue(jsonString.contains("\"datasets\":"));
  }

  @Test
  void testValidation_DatasetsNotNull() {
    ApplicationDTO dto = new ApplicationDTO();
    dto.setTitle("Valid Title");
    dto.setDatasets(null); // Invalid datasets

    Set<ConstraintViolation<ApplicationDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
      .anyMatch(v -> v.getPropertyPath().toString().equals("datasets") &&
        v.getMessage().contains("must not be null")));
  }

  @Test
  void testValidation_TitleNotBlank() {
    ApplicationDTO dto = new ApplicationDTO();
    dto.setTitle(""); // Invalid title
    dto.setDatasets(List.of(new DatasetDTO("dataset1", "Dataset Title 1")));

    Set<ConstraintViolation<ApplicationDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
      .anyMatch(v -> v.getPropertyPath().toString().equals("title") &&
        v.getMessage().contains("must not be blank")));
  }
}

