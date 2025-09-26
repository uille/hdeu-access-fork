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
package eu.europa.data.health.access.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.json.JsonObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class TranslationUtilsTest {

  @Test
  void testConstructor_isPrivate_andThrows() throws Exception {
    Constructor<TranslationUtils> constructor = TranslationUtils.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

    constructor.setAccessible(true); // allow reflective invocation

    InvocationTargetException ex =
      assertThrows(InvocationTargetException.class, constructor::newInstance,
        "Invoking the private constructor should throw");

    assertInstanceOf(UnsupportedOperationException.class, ex.getTargetException(),
      "Constructor should throw UnsupportedOperationException");
    assertEquals("Utility class", ex.getTargetException().getMessage());
  }

  @Test
  void testAddTranslations_addsOnlyValidNonNumericStrings() {
    JsonObject multilingual = new JsonObject();

    JsonObject translations = new JsonObject()
      // valid strings (should be added)
      .put("en", "Hello")
      .put("es", "Hola")
      .put("nonnumeric", "12a") // not numeric → add
      .put("doubleDash", "--5") // not numeric after single-dash stripping → add
      // numeric error codes (should be skipped)
      .put("numInt", 404) // Number → isNumericErrorCode = true
      .put("numStr", "007") // numeric string → true
      .put("negStr", "-5") // negative numeric string → true
      .put("spacedNeg", "  -42 ") // spaces + negative → true
      // blanks & non-String, non-Number (should not be added)
      .put("blank", "   ") // not numeric but StringUtils.isNotBlank = false → skip
      .put("obj", new JsonObject().put("a", 1)) // isNumericErrorCode=false, but not String → skip
      .put("nullVal", null); // null → skip

    boolean added = TranslationUtils.addTranslations(multilingual, translations);

    assertTrue(added, "At least one valid translation should be added");
    // Only the valid, non-numeric, non-blank Strings should be present
    assertEquals(4, multilingual.size());
    assertEquals("Hello", multilingual.getString("en"));
    assertEquals("Hola", multilingual.getString("es"));
    assertEquals("12a", multilingual.getString("nonnumeric"));
    assertEquals("--5", multilingual.getString("doubleDash"));

    // Ensure skipped keys are indeed absent
    assertFalse(multilingual.containsKey("numInt"));
    assertFalse(multilingual.containsKey("numStr"));
    assertFalse(multilingual.containsKey("negStr"));
    assertFalse(multilingual.containsKey("spacedNeg"));
    assertFalse(multilingual.containsKey("blank"));
    assertFalse(multilingual.containsKey("obj"));
    assertFalse(multilingual.containsKey("nullVal"));
  }

  @Test
  void testAddTranslations_returnsFalse_whenNothingAdded() {
    JsonObject multilingual = new JsonObject();

    JsonObject translations = new JsonObject()
      .put("onlyNumInt", 500)
      .put("onlyNumStr", "12345")
      .put("onlyNegNumStr", "-99")
      .put("onlyBlank", "   ")
      .put("onlyNonString", Boolean.TRUE)
      .put("onlyNull", null);

    boolean added = TranslationUtils.addTranslations(multilingual, translations);

    assertFalse(added, "No valid text translations → should return false");
    assertTrue(multilingual.isEmpty(), "Multilingual object should remain empty");
  }

  @Test
  void testAddTranslations_overwritesExistingLanguageValue_andReturnsTrue() {
    JsonObject multilingual = new JsonObject().put("en", "Old");
    JsonObject translations = new JsonObject().put("en", "New"); // valid non-numeric string

    boolean added = TranslationUtils.addTranslations(multilingual, translations);

    assertTrue(added, "A valid update should return true");
    assertEquals("New", multilingual.getString("en"), "Existing value should be overwritten");
  }

  @Test
  void testAddTranslations_nonStringNonNumber_path_exercisesFalseReturnOfIsNumericErrorCode() {
    JsonObject multilingual = new JsonObject();
    // Raw is neither Number nor String → isNumericErrorCode returns false,
    // but since it's not a String, addTranslations won't put it.
    JsonObject translations = new JsonObject().put("complex", new JsonObject().put("x", 1));

    boolean added = TranslationUtils.addTranslations(multilingual, translations);

    assertFalse(added, "No String value to add → should be false");
    assertFalse(multilingual.containsKey("complex"));
  }
}
