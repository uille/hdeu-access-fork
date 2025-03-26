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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SectionDTOTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testDefaultConstructorAndAccessors() {
    SectionDTO sectionDTO = new SectionDTO();
    sectionDTO.setSectionNumber(10);

    assertEquals(10, sectionDTO.getSectionNumber());
  }

  @Test
  void testJsonSerialization() throws Exception {
    SectionDTO sectionDTO = new SectionDTO();
    sectionDTO.setSectionNumber(5);

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(sectionDTO);

    // Validate JSON output
    assertTrue(json.contains("\"sectionNumber\":5"));
  }

  @Test
  void testIgnoreUnknownFields() throws Exception {
    String json = "{\"sectionNumber\":7, \"extraField\":\"value\"}";

    // Deserialize from JSON with unknown field
    SectionDTO sectionDTO = objectMapper.readValue(json, SectionDTO.class);

    // Validate deserialized object
    assertNotNull(sectionDTO);
    assertEquals(7, sectionDTO.getSectionNumber());
  }

  @Test
  void testMissingFieldsInJson() throws Exception {
    String json = "{}"; // No sectionNumber field

    // Deserialize from JSON
    SectionDTO sectionDTO = objectMapper.readValue(json, SectionDTO.class);

    // Validate object
    assertNotNull(sectionDTO);
    assertNull(sectionDTO.getSectionNumber());
  }

  @Test
  void testNullSectionNumber() {
    SectionDTO sectionDTO = new SectionDTO();
    sectionDTO.setSectionNumber(null);

    assertNull(sectionDTO.getSectionNumber());
  }

  @Test
  void testNegativeSectionNumber() {
    SectionDTO sectionDTO = new SectionDTO();
    sectionDTO.setSectionNumber(-1);

    assertEquals(-1, sectionDTO.getSectionNumber());
  }
}
