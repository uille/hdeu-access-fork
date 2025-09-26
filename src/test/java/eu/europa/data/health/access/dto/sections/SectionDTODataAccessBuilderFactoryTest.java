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
package eu.europa.data.health.access.dto.sections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import eu.europa.data.health.access.dto.sections.data_access.Section10DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section5DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section6DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section7DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section8DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section9DTO;
import eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager;
import org.junit.jupiter.api.Test;

class SectionDTODataAccessBuilderFactoryTest {

  @Test
  void testCreateSectionDTO_ValidSectionNumbers() {
    // Test for valid section numbers and assert that the correct class is returned
    assertEquals(Section2DTO.class, SectionDTOBuilderFactory.createSectionDTO(2, DataAccessApplicationValidationManager.class));
    assertEquals(Section3DTO.class, SectionDTOBuilderFactory.createSectionDTO(3, DataAccessApplicationValidationManager.class));
    assertEquals(Section4DTO.class, SectionDTOBuilderFactory.createSectionDTO(4, DataAccessApplicationValidationManager.class));
    assertEquals(Section5DTO.class, SectionDTOBuilderFactory.createSectionDTO(5, DataAccessApplicationValidationManager.class));
    assertEquals(Section6DTO.class, SectionDTOBuilderFactory.createSectionDTO(6, DataAccessApplicationValidationManager.class));
    assertEquals(Section7DTO.class, SectionDTOBuilderFactory.createSectionDTO(7, DataAccessApplicationValidationManager.class));
    assertEquals(Section8DTO.class, SectionDTOBuilderFactory.createSectionDTO(8, DataAccessApplicationValidationManager.class));
    assertEquals(Section9DTO.class, SectionDTOBuilderFactory.createSectionDTO(9, DataAccessApplicationValidationManager.class));
    assertEquals(Section10DTO.class, SectionDTOBuilderFactory.createSectionDTO(10, DataAccessApplicationValidationManager.class));
  }

  @Test
  void testCreateSectionDTO_InvalidSectionNumber() {
    // Test for invalid section numbers and assert that an IllegalArgumentException is thrown
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      SectionDTOBuilderFactory.createSectionDTO(0, DataAccessApplicationValidationManager.class);
    });
    assertEquals("Invalid section number: 0", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      SectionDTOBuilderFactory.createSectionDTO(11, DataAccessApplicationValidationManager.class);
    });
    assertEquals("Invalid section number: 11", exception.getMessage());
  }

  @Test
  void testConstructorNotInstantiable() {
    // Test that the constructor is private and the class cannot be instantiated
    try {
      new SectionDTOBuilderFactory();
      fail("Expected UnsupportedOperationException to be thrown");
    } catch (UnsupportedOperationException e) {
      assertEquals("This is a utility class and cannot be instantiated", e.getMessage());
    }
  }
}
