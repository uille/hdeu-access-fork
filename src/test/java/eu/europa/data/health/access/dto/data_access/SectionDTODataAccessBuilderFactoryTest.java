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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class SectionDTODataAccessBuilderFactoryTest {

  @Test
  void testCreateSectionDTO_ValidSectionNumbers() {
    // Test for valid section numbers and assert that the correct class is returned
    assertEquals(Section2DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(2));
    assertEquals(Section3DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(3));
    assertEquals(Section4DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(4));
    assertEquals(Section5DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(5));
    assertEquals(Section6DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(6));
    assertEquals(Section7DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(7));
    assertEquals(Section8DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(8));
    assertEquals(Section9DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(9));
    assertEquals(Section10DTO.class, SectionDTODataAccessBuilderFactory.createSectionDTO(10));
  }

  @Test
  void testCreateSectionDTO_InvalidSectionNumber() {
    // Test for invalid section numbers and assert that an IllegalArgumentException is thrown
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      SectionDTODataAccessBuilderFactory.createSectionDTO(1);
    });
    assertEquals("Invalid section number: 1", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      SectionDTODataAccessBuilderFactory.createSectionDTO(11);
    });
    assertEquals("Invalid section number: 11", exception.getMessage());
  }

  @Test
  void testConstructorNotInstantiable() {
    // Test that the constructor is private and the class cannot be instantiated
    try {
      new SectionDTODataAccessBuilderFactory();
      fail("Expected UnsupportedOperationException to be thrown");
    } catch (UnsupportedOperationException e) {
      assertEquals("This is a utility class and cannot be instantiated", e.getMessage());
    }
  }
}
