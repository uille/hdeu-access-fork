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

import eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager;
import eu.europa.data.health.access.validations.validators.DataRequestApplicationValidationManager;

public class SectionDTOBuilderFactory {

  // Private constructor to prevent instantiation
  SectionDTOBuilderFactory() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static Class<? extends SectionDTO> createSectionDTO(int sectionNumber, Class<?> clazz) {
    switch (sectionNumber) {
      case 1:
        return Section1DTO.class;
      case 2:
        return Section2DTO.class;
      case 3:
        return Section3DTO.class;
      case 4:
        return Section4DTO.class;
      case 5:
        if (DataAccessApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_access.Section5DTO.class;
        } else if (DataRequestApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_request.Section5DTO.class;
        } else {
          throw new IllegalArgumentException("Unknown class type for Section 5: " + clazz);
        }
      case 6:
        if (DataAccessApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_access.Section6DTO.class;
        } else if (DataRequestApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_request.Section6DTO.class;
        } else {
          throw new IllegalArgumentException("Unknown class type for Section 6: " + clazz);
        }
      case 7:
        if (DataAccessApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_access.Section7DTO.class;
        } else if (DataRequestApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_request.Section7DTO.class;
        } else {
          throw new IllegalArgumentException("Unknown class type for Section 7: " + clazz);
        }
      case 8:
        if (DataAccessApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_access.Section8DTO.class;
        } else if (DataRequestApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_request.Section8DTO.class;
        } else {
          throw new IllegalArgumentException("Unknown class type for Section 8: " + clazz);
        }
      case 9:
        if (DataAccessApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_access.Section9DTO.class;
        } else {
          throw new IllegalArgumentException("Invalid section number: " + sectionNumber);
        }
      case 10:
        if (DataAccessApplicationValidationManager.class.isAssignableFrom(clazz)) {
          return eu.europa.data.health.access.dto.sections.data_access.Section10DTO.class;
        } else {
          throw new IllegalArgumentException("Invalid section number: " + sectionNumber);
        }
      default:
        throw new IllegalArgumentException("Invalid section number: " + sectionNumber);
    }
  }
}
