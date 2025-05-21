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
package eu.europa.data.health.access.dto.data_request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;


class SectionDTODataRequestBuilderFactoryTest {

  @Test
  void shouldReturnCorrectSectionDTOClass_givenValidSectionNumber() {
    assertEquals(Section2DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(2));
    assertEquals(Section3DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(3));
    assertEquals(Section4DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(4));
    assertEquals(Section5DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(5));
    assertEquals(Section6DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(6));
    assertEquals(Section7DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(7));
    assertEquals(Section8DTO.class, SectionDTODataRequestBuilderFactory.createSectionDTO(8));

  }

  @Test
  void shouldThrowIllegalArgumentException_givenInvalidSectionNumber() {
    IllegalArgumentException ex1 = assertThrows(
      IllegalArgumentException.class,
      () -> SectionDTODataRequestBuilderFactory.createSectionDTO(1)
    );
    assertEquals("Invalid section number: 1", ex1.getMessage());

    IllegalArgumentException ex2 = assertThrows(
      IllegalArgumentException.class,
      () -> SectionDTODataRequestBuilderFactory.createSectionDTO(9)
    );
    assertEquals("Invalid section number: 9", ex2.getMessage());
  }

}
