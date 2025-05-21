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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TranslationsTest {

  @Test
  void testGetDefaultReturnsEnglish() {
    assertEquals("en", Translations.getDefault());
  }

  @Test
  void testExistsReturnsTrueForExistingLanguage() {
    assertTrue(Translations.exists("en"));
    assertTrue(Translations.exists("EN")); // case-insensitive check
  }

  @Test
  void testExistsReturnsFalseForNonExistingLanguage() {
    assertFalse(Translations.exists("de"));
    assertFalse(Translations.exists("fr"));
  }

  @Test
  void testEnumValuesTranslation() {
    assertEquals("en", Translations.DEFAULT.getTranslation());
    assertEquals("en", Translations.ENGLISH.getTranslation());
  }
}
