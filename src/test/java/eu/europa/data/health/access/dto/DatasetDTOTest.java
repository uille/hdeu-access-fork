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
import org.junit.jupiter.api.Test;

class DatasetDTOTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testNoArgsConstructor() {
    DatasetDTO datasetDTO = new DatasetDTO();
    assertNull(datasetDTO.getCatalogId());
    assertNull(datasetDTO.getDatasetId());
  }

  @Test
  void testCatalogIdNotBlankValidation() {
    DatasetDTO datasetDTO = new DatasetDTO();
    datasetDTO.setCatalogId("");
    datasetDTO.setDatasetId("123");

    var violations = validator.validate(datasetDTO);
    assertFalse(violations.isEmpty(), "CatalogId should not be blank");
  }

  @Test
  void testDatasetIdNotBlankValidation() {
    DatasetDTO datasetDTO = new DatasetDTO();
    datasetDTO.setCatalogId("456");
    datasetDTO.setDatasetId("");

    var violations = validator.validate(datasetDTO);
    assertFalse(violations.isEmpty(), "DatasetId should not be blank");
  }

  @Test
  void testValidDatasetDTO() {
    DatasetDTO datasetDTO = new DatasetDTO();
    datasetDTO.setCatalogId("456");
    datasetDTO.setDatasetId("123");

    var violations = validator.validate(datasetDTO);
    assertTrue(violations.isEmpty(), "Validation should pass for valid datasetDTO");
  }

  @Test
  void testSerialization() throws Exception {
    DatasetDTO datasetDTO = new DatasetDTO();
    datasetDTO.setCatalogId("456");
    datasetDTO.setDatasetId("123");

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(datasetDTO);
    assertTrue(json.contains("\"catalog_id\":\"456\""));
    assertTrue(json.contains("\"dataset_id\":\"123\""));
  }

  @Test
  void testDeserialization() throws Exception {
    String json = "{\"catalog_id\":\"456\", \"dataset_id\":\"123\"}";

    // Deserialize from JSON
    DatasetDTO datasetDTO = objectMapper.readValue(json, DatasetDTO.class);
    assertEquals("456", datasetDTO.getCatalogId());
    assertEquals("123", datasetDTO.getDatasetId());
  }

  @Test
  void testIgnoreUnknownProperties() throws Exception {
    String json = "{\"catalog_id\":\"456\", \"dataset_id\":\"123\", \"extra_field\":\"value\"}";

    // Deserialize from JSON
    DatasetDTO datasetDTO = objectMapper.readValue(json, DatasetDTO.class);

    assertEquals("456", datasetDTO.getCatalogId());
    assertEquals("123", datasetDTO.getDatasetId());
  }
}
