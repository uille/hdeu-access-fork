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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AttachmentDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testGetterSetter() {
    FileDTO fileDTO = new FileDTO();
    fileDTO.setLastModified(1609459200000.0);  // Example timestamp (e.g., 2021-01-01 00:00:00)
    fileDTO.setName("fileName.txt");
    fileDTO.setWebkitRelativePath("/files/fileName.txt");

    AttachmentDTO attachmentDTO = new AttachmentDTO();
    attachmentDTO.setName("AttachmentName");
    attachmentDTO.setId("12345");
    attachmentDTO.setFile(fileDTO);
    attachmentDTO.setSize(25.5);

    assertEquals("AttachmentName", attachmentDTO.getName());
    assertEquals("12345", attachmentDTO.getId());
    assertEquals(fileDTO, attachmentDTO.getFile());
    assertEquals(25.5, attachmentDTO.getSize());
  }

  @Test
  void testIgnoreUnknownProperties() throws Exception {
    String json = "{\"name\":\"AttachmentName\", \"id\":\"12345\", \"file\":{\"filePath\":\"path/to/file\", \"fileContentType\":\"application/pdf\"}, \"size\":25.5, \"extraField\":\"extra_value\"}";

    // Deserialize from JSON with an extra field
    AttachmentDTO attachmentDTO = objectMapper.readValue(json, AttachmentDTO.class);

    assertEquals("AttachmentName", attachmentDTO.getName());
    assertEquals("12345", attachmentDTO.getId());
    assertNotNull(attachmentDTO.getFile());
    assertEquals(25.5, attachmentDTO.getSize());

    // We should not get an exception, and extra_field should be ignored
  }

  @Test
  void testSerialization() throws Exception {
    FileDTO fileDTO = new FileDTO();
    fileDTO.setLastModified(1609459200000.0);
    fileDTO.setName("fileName.txt");
    fileDTO.setWebkitRelativePath("/files/fileName.txt");

    AttachmentDTO attachmentDTO = new AttachmentDTO();
    attachmentDTO.setName("AttachmentName");
    attachmentDTO.setId("12345");
    attachmentDTO.setFile(fileDTO);
    attachmentDTO.setSize(25.5);

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(attachmentDTO);

    assertTrue(json.contains("\"name\":\"AttachmentName\""));
    assertTrue(json.contains("\"id\":\"12345\""));
    assertTrue(json.contains("\"file\":{\"lastModified\":1.6094592E12,\"name\":\"fileName.txt\",\"webkitRelativePath\":\"/files/fileName.txt\"}"));
    assertTrue(json.contains("\"size\":25.5"));
  }

  @Test
  void testDeserialization() throws Exception {
    String json = "{\"name\":\"AttachmentName\", \"id\":\"12345\", \"file\":{\"filePath\":\"path/to/file\",\"fileContentType\":\"application/pdf\"}, \"size\":25.5}";

    // Deserialize from JSON
    AttachmentDTO attachmentDTO = objectMapper.readValue(json, AttachmentDTO.class);

    assertEquals("AttachmentName", attachmentDTO.getName());
    assertEquals("12345", attachmentDTO.getId());
    assertNotNull(attachmentDTO.getFile());
    assertEquals(25.5, attachmentDTO.getSize());
  }

  @Test
  void testNullFileAndSize() throws Exception {
    String json = "{\"name\":\"AttachmentName\", \"id\":\"12345\", \"file\":null, \"size\":null}";

    // Deserialize from JSON with null file and size
    AttachmentDTO attachmentDTO = objectMapper.readValue(json, AttachmentDTO.class);

    assertEquals("AttachmentName", attachmentDTO.getName());
    assertEquals("12345", attachmentDTO.getId());
    assertNull(attachmentDTO.getFile());  // null file
    assertNull(attachmentDTO.getSize());  // null size
  }

  @Test
  void testMissingFields() throws Exception {
    String json = "{\"name\":\"AttachmentName\", \"id\":\"12345\"}";  // Missing file and size

    // Deserialize from JSON
    AttachmentDTO attachmentDTO = objectMapper.readValue(json, AttachmentDTO.class);

    assertEquals("AttachmentName", attachmentDTO.getName());
    assertEquals("12345", attachmentDTO.getId());
    assertNull(attachmentDTO.getFile());  // Missing file, should be null
    assertNull(attachmentDTO.getSize());  // Missing size, should be null
  }

  @Test
  void testInvalidJson() {
    String invalidJson = "{\"name\":\"AttachmentName\", \"id\":\"12345\", \"size\":\"invalid_value\"}";  // invalid value for size

    assertThrows(com.fasterxml.jackson.databind.exc.MismatchedInputException.class, () -> {
      objectMapper.readValue(invalidJson, AttachmentDTO.class);
    });
  }
}
