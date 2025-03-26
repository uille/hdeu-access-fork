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
package eu.europa.data.health.access;

import java.util.Arrays;
import java.util.List;

public enum Sections {
  SECTION_2("section2", 2),
  SECTION_3("section3", 3),
  SECTION_4("section4", 4),
  SECTION_5("section5", 5),
  SECTION_6("section6", 6),
  SECTION_7("section7", 7),
  SECTION_8("section8", 8),
  SECTION_9("section9", 9),
  SECTION_10("section10", 10);

  private final String section;
  private final Integer sectionNumber;

  Sections(final String section, Integer sectionNumber) {
    this.section = section;
    this.sectionNumber = sectionNumber;
  }

  public String getSection() {
    return section;
  }

  public Integer getSectionNumber() {
    return sectionNumber;
  }

  public static boolean isValidSection(String section) {
    for (Sections sectionObj : Sections.values()) {
      if (sectionObj.getSection().equalsIgnoreCase(section)) {
        return true;
      }
    }
    return false;
  }

  // New method to exclude specific sections for data request
  public static List<Sections> getFilteredSections() {
    return Arrays.stream(Sections.values())
      .filter(section -> section != SECTION_9 && section != SECTION_10) // Exclude SECTION_9 and SECTION_10
      .toList();
  }

}
