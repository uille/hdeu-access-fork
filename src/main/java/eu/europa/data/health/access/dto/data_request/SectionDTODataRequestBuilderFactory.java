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

import eu.europa.data.health.access.dto.SectionDTO;

public class SectionDTODataRequestBuilderFactory {

  // Private constructor to prevent instantiation
  private SectionDTODataRequestBuilderFactory() {
    throw new UnsupportedOperationException("SectionDTODataRequestBuilderFactory class should not be instantiated");
  }

  public static Class<? extends SectionDTO> createSectionDTO(int sectionNumber) {
    return switch (sectionNumber) {
      case 2 -> Section2DTO.class;
      case 3 -> Section3DTO.class;
      case 4 -> Section4DTO.class;
      case 5 -> Section5DTO.class;
      case 6 -> Section6DTO.class;
      case 7 -> Section7DTO.class;
      case 8 -> Section8DTO.class;
      default -> throw new IllegalArgumentException("Invalid section number: " + sectionNumber);
    };
  }
}

