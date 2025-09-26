package eu.europa.data.health.access.annotations;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.services.TranslationManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckEnumValueValidationTest {

  /**
   * Class: CheckEnumValueValidation
   * Method: isValid
   * Description: Validates if the keys of the given value(s) exist in the translation values extracted
   * from the TranslationManager, based on the sectionNumber and fieldName.
   */

  @Test
  void testIsValidWithNullValue() {
      CheckEnumValueValidation validator = new CheckEnumValueValidation();
      boolean result = validator.isValid(null, null);
      assertTrue(result, "Validation should return true for null value.");
  }

  @Test
  void testIsValidWithEmptyList() {
      CheckEnumValueValidation validator = new CheckEnumValueValidation();
      List<SelectObjectDTO> emptyList = new ArrayList<>();
      boolean result = validator.isValid(emptyList, null);
      assertTrue(result, "Validation should return true for an empty list.");
  }

  @Test
  void testIsValidWithNonMatchingKeySingleValue() {
      SelectObjectDTO selectObject = new SelectObjectDTO();
      selectObject.setKey("key3");

      JsonArray jsonArray = new JsonArray().add(new JsonObject().put("key", "key1"));
      setupMockTranslationManagerForValidator("section1", "field1", jsonArray);

      CheckEnumValueValidation validator = new CheckEnumValueValidation();
      validator.initialize(createMockAnnotation("section1", "field1"));

      boolean result = validator.isValid(selectObject, null);
      assertFalse(result, "Validation should return false for non-matching key in single value.");
  }

  @Test
  void testIsValidWithNonMatchingKeysInList() {
      List<SelectObjectDTO> selectObjectList = new ArrayList<>();
      SelectObjectDTO object1 = new SelectObjectDTO();
      object1.setKey("key1");
      SelectObjectDTO object2 = new SelectObjectDTO();
      object2.setKey("invalidKey");
      selectObjectList.add(object1);
      selectObjectList.add(object2);

      JsonArray jsonArray = new JsonArray()
              .add(new JsonObject().put("key", "key1"));

      setupMockTranslationManagerForValidator("section1", "field1", jsonArray);

      CheckEnumValueValidation validator = new CheckEnumValueValidation();
      validator.initialize(createMockAnnotation("section1", "field1"));

      boolean result = validator.isValid(selectObjectList, null);
      assertFalse(result, "Validation should return false for non-matching keys in list.");
  }

  @Test
  void testIsValidWithExceptionThrown() {
    CheckEnumValueValidation validator = new CheckEnumValueValidation();
    validator.initialize(createMockAnnotation("invalidSection", "field1"));

    boolean result = validator.isValid("invalidValue", null);
    assertFalse(result, "Validation should return false if an exception is thrown.");
  }

  private void setupMockTranslationManagerForValidator(String section, String field, JsonArray items) {
    TranslationManager translationManager = mock(TranslationManager.class);
    JsonObject enumValues = new JsonObject().put(field, new JsonObject().put("items", items));
    when(translationManager.getTranslation(Mockito.anyString(), Mockito.eq(section)))
      .thenReturn(enumValues);

    try {
      TranslationManager realTranslationManager = new TranslationManager();
      TranslationManager spyManager = Mockito.spy(realTranslationManager);
      Mockito.doReturn(enumValues).when(spyManager).getTranslation(Mockito.anyString(), Mockito.eq(section));
    } catch (Exception ignored) {
    }
  }

  private CheckEnumValue createMockAnnotation(String sectionNumber, String fieldName) {
    CheckEnumValue mockAnnotation = mock(CheckEnumValue.class);
    when(mockAnnotation.sectionNumber()).thenReturn(sectionNumber);
    when(mockAnnotation.fieldName()).thenReturn(fieldName);
    return mockAnnotation;
  }
}
