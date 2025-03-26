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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SectionsTest {

  @Test
  void testGetSection() {
    assertEquals("section2", Sections.SECTION_2.getSection());
    assertEquals("section3", Sections.SECTION_3.getSection());
  }

  @Test
  void testGetSectionNumber() {
    assertEquals(2, Sections.SECTION_2.getSectionNumber());
    assertEquals(3, Sections.SECTION_3.getSectionNumber());
  }

  @Test
  void testIsValidSection() {
    assertTrue(Sections.isValidSection("section2"));
    assertTrue(Sections.isValidSection("Section3")); // Case-insensitive check
    assertFalse(Sections.isValidSection("invalidSection"));
  }

  @Test
  void testGetFilteredSections() {
    List<Sections> filteredSections = Sections.getFilteredSections();

    // Ensure SECTION_9 and SECTION_10 are excluded
    assertFalse(filteredSections.contains(Sections.SECTION_9));
    assertFalse(filteredSections.contains(Sections.SECTION_10));

    // Ensure other sections are included
    assertTrue(filteredSections.contains(Sections.SECTION_2));
    assertTrue(filteredSections.contains(Sections.SECTION_3));
    assertTrue(filteredSections.contains(Sections.SECTION_4));
  }

  @Test
  void testEnumValues() {
    // Verify the total number of sections
    Sections[] sections = Sections.values();
    assertEquals(9, sections.length);

    // Verify each section
    assertEquals(Sections.SECTION_2, Sections.valueOf("SECTION_2"));
    assertEquals(Sections.SECTION_3, Sections.valueOf("SECTION_3"));
    assertEquals(Sections.SECTION_4, Sections.valueOf("SECTION_4"));
    assertEquals(Sections.SECTION_5, Sections.valueOf("SECTION_5"));
    assertEquals(Sections.SECTION_6, Sections.valueOf("SECTION_6"));
    assertEquals(Sections.SECTION_7, Sections.valueOf("SECTION_7"));
    assertEquals(Sections.SECTION_8, Sections.valueOf("SECTION_8"));
    assertEquals(Sections.SECTION_9, Sections.valueOf("SECTION_9"));
    assertEquals(Sections.SECTION_10, Sections.valueOf("SECTION_10"));
  }
}
