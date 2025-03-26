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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FileDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testGetterSetter() {
    FileDTO fileDTO = new FileDTO();

    fileDTO.setLastModified(1609459200000.0);  // Example timestamp (e.g., 2021-01-01 00:00:00)
    fileDTO.setName("fileName.txt");
    fileDTO.setWebkitRelativePath("/files/fileName.txt");

    assertEquals(1609459200000.0, fileDTO.getLastModified());
    assertEquals("fileName.txt", fileDTO.getName());
    assertEquals("/files/fileName.txt", fileDTO.getWebkitRelativePath());
  }

  @Test
  void testIgnoreUnknownProperties() throws Exception {
    String json = "{\"lastModified\":1609459200000.0, \"name\":\"fileName.txt\", \"webkitRelativePath\":\"/files/fileName.txt\", \"extraField\":\"extraValue\"}";

    // Deserialize from JSON with an extra field
    FileDTO fileDTO = objectMapper.readValue(json, FileDTO.class);

    assertEquals(1609459200000.0, fileDTO.getLastModified());
    assertEquals("fileName.txt", fileDTO.getName());
    assertEquals("/files/fileName.txt", fileDTO.getWebkitRelativePath());

    // We should not get an exception and extraField should be ignored
  }

  @Test
  void testSerialization() throws Exception {
    FileDTO fileDTO = new FileDTO();
    fileDTO.setLastModified(1609459200000.0);
    fileDTO.setName("fileName.txt");
    fileDTO.setWebkitRelativePath("/files/fileName.txt");

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(fileDTO);

    assertTrue(json.contains("\"lastModified\":1.6094592E12"));
    assertTrue(json.contains("\"name\":\"fileName.txt\""));
    assertTrue(json.contains("\"webkitRelativePath\":\"/files/fileName.txt\""));
  }

  @Test
  void testDeserialization() throws Exception {
    String json = "{\"lastModified\":1609459200000.0, \"name\":\"fileName.txt\", \"webkitRelativePath\":\"/files/fileName.txt\"}";

    // Deserialize from JSON
    FileDTO fileDTO = objectMapper.readValue(json, FileDTO.class);

    assertEquals(1609459200000.0, fileDTO.getLastModified());
    assertEquals("fileName.txt", fileDTO.getName());
    assertEquals("/files/fileName.txt", fileDTO.getWebkitRelativePath());
  }

  @Test
  void testNullValues() throws Exception {
    String json = "{\"lastModified\":null, \"name\":null, \"webkitRelativePath\":null}";

    // Deserialize from JSON with null values
    FileDTO fileDTO = objectMapper.readValue(json, FileDTO.class);

    assertNull(fileDTO.getLastModified());
    assertNull(fileDTO.getName());
    assertNull(fileDTO.getWebkitRelativePath());
  }

  @Test
  void testMissingFields() throws Exception {
    String json = "{\"name\":\"fileName.txt\"}";  // Missing lastModified and webkitRelativePath

    // Deserialize from JSON
    FileDTO fileDTO = objectMapper.readValue(json, FileDTO.class);

    assertNull(fileDTO.getLastModified());  // Missing field, should be null
    assertEquals("fileName.txt", fileDTO.getName());
    assertNull(fileDTO.getWebkitRelativePath());  // Missing field, should be null
  }

  @Test
  void testInvalidJson() {
    String invalidJson = "{\"lastModified\":\"invalid_value\", \"name\":\"fileName.txt\", \"webkitRelativePath\":\"/files/fileName.txt\"}";

    assertThrows(com.fasterxml.jackson.databind.exc.MismatchedInputException.class, () -> {
      objectMapper.readValue(invalidJson, FileDTO.class);
    });
  }
}
