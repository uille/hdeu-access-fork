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

import eu.europa.data.health.access.dto.sections.data_request.Section5DTO;
import eu.europa.data.health.access.dto.sections.data_request.Section6DTO;
import eu.europa.data.health.access.dto.sections.data_request.Section7DTO;
import eu.europa.data.health.access.dto.sections.data_request.Section8DTO;
import eu.europa.data.health.access.validations.validators.DataRequestApplicationValidationManager;
import org.junit.jupiter.api.Test;


class SectionDTODataRequestBuilderFactoryTest {

  @Test
  void shouldReturnCorrectSectionDTOClass_givenValidSectionNumber() {
    assertEquals(Section2DTO.class, SectionDTOBuilderFactory.createSectionDTO(2, DataRequestApplicationValidationManager.class));
    assertEquals(Section3DTO.class, SectionDTOBuilderFactory.createSectionDTO(3, DataRequestApplicationValidationManager.class));
    assertEquals(Section4DTO.class, SectionDTOBuilderFactory.createSectionDTO(4, DataRequestApplicationValidationManager.class));
    assertEquals(Section5DTO.class, SectionDTOBuilderFactory.createSectionDTO(5, DataRequestApplicationValidationManager.class));
    assertEquals(Section6DTO.class, SectionDTOBuilderFactory.createSectionDTO(6, DataRequestApplicationValidationManager.class));
    assertEquals(Section7DTO.class, SectionDTOBuilderFactory.createSectionDTO(7, DataRequestApplicationValidationManager.class));
    assertEquals(Section8DTO.class, SectionDTOBuilderFactory.createSectionDTO(8, DataRequestApplicationValidationManager.class));

  }

  @Test
  void shouldThrowIllegalArgumentException_givenInvalidSectionNumber() {
    IllegalArgumentException ex1 = assertThrows(
      IllegalArgumentException.class,
      () -> SectionDTOBuilderFactory.createSectionDTO(11,
        DataRequestApplicationValidationManager.class)
    );
    assertEquals("Invalid section number: 11", ex1.getMessage());

    IllegalArgumentException ex2 = assertThrows(
      IllegalArgumentException.class,
      () -> SectionDTOBuilderFactory.createSectionDTO(12,
        DataRequestApplicationValidationManager.class)
    );
    assertEquals("Invalid section number: 12", ex2.getMessage());
  }

}
