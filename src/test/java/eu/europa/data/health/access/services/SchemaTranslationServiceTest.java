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
package eu.europa.data.health.access.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaTranslationServiceTest {

  private SchemaTranslationService service;

  @BeforeEach
  void setUp() {
    service = new SchemaTranslationService();
  }

  @Test
  void testMergeExistingTranslations_mergesOldTranslations() {
    JsonObject oldPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject().put("en", "Public").put("fr", "Public FR"))
          .put("description", new JsonObject().put("en", "Open").put("de", "Offen"))));

    JsonObject newPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject().put("en", "Public"))   // same EN, missing FR
          .put("description", new JsonObject().put("en", "Open")))); // same EN, missing DE

    service.mergeExistingTranslations(newPayload, oldPayload, "en", Set.of("en", "fr", "de"));

    JsonObject level = newPayload.getJsonObject("datasetAccessLevels").getJsonObject("public");
    assertEquals("Public FR", level.getJsonObject("label").getString("fr"));
    assertEquals("Offen", level.getJsonObject("description").getString("de"));
  }

  @Test
  void testMergeExistingTranslations_doesNotMergeWhenEnglishChanged() {
    JsonObject oldPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject().put("en", "Public v1").put("fr", "Ancien FR"))));

    JsonObject newPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject().put("en", "Public v2")))); // changed EN

    service.mergeExistingTranslations(newPayload, oldPayload, "en", Set.of("en", "fr"));

    JsonObject label = newPayload.getJsonObject("datasetAccessLevels").getJsonObject("public")
      .getJsonObject("label");
    assertNull(label.getString("fr")); // should NOT merge, since EN changed
  }

  @Test
  void testMergeExistingTranslations_nestedInputFields() {
    JsonObject oldPayload = new JsonObject()
      .put("shapes", new JsonObject()
        .put("DatasetShape", new JsonObject()
          .put("properties", new JsonArray().add(new JsonObject()
            .put("path", "root/title")
            .put("input", new JsonObject()
              .put("label", new JsonObject().put("en", "Title").put("de", "Titel"))
              .put("tooltip", new JsonObject().put("en", "Info").put("fr", "Infos")))))));

    JsonObject newPayload = new JsonObject()
      .put("shapes", new JsonObject()
        .put("DatasetShape", new JsonObject()
          .put("properties", new JsonArray().add(new JsonObject()
            .put("path", "root/title")
            .put("input", new JsonObject()
              .put("label", new JsonObject().put("en", "Title"))     // same EN, missing DE
              .put("tooltip", new JsonObject().put("en", "Info"))))))); // same EN, missing FR

    service.mergeExistingTranslations(newPayload, oldPayload, "en", Set.of("en", "de", "fr"));

    JsonObject input = newPayload.getJsonObject("shapes")
      .getJsonObject("DatasetShape")
      .getJsonArray("properties").getJsonObject(0)
      .getJsonObject("input");

    assertEquals("Titel", input.getJsonObject("label").getString("de"));
    assertEquals("Infos", input.getJsonObject("tooltip").getString("fr"));
  }

  @Test
  void testBuildTranslatableDict_withDatasetAccessLevels() {
    JsonObject newPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject().put("en", "Public"))
          .put("description", new JsonObject().put("en", "Open access"))));

    JsonObject dict = service.buildTranslatableDict(newPayload, null, "en", Set.of("fr", "de"));

    assertTrue(dict.containsKey("datasetAccessLevels/public"));
    JsonObject subject = dict.getJsonObject("datasetAccessLevels/public");

    assertEquals("Public", subject.getJsonObject("label").getString("value"));
    assertEquals("en", subject.getJsonObject("label").getString("sourceLanguage"));

    JsonArray tl = subject.getJsonObject("label").getJsonArray("targetLanguages");
    assertEquals(2, tl.size());
    assertTrue(tl.contains("fr"));
    assertTrue(tl.contains("de"));
  }

  @Test
  void testBuildTranslatableDict_withShapesAndProperties() {
    JsonObject newPayload = new JsonObject()
      .put("shapes", new JsonObject()
        .put("DatasetShape", new JsonObject()
          .put("name", new JsonObject().put("en", "Dataset"))
          .put("properties", new JsonArray()
            .add(new JsonObject()
              .put("buttonLabel", new JsonObject().put("en", "Click Me"))
              .put("input", new JsonObject()
                .put("label", new JsonObject().put("en", "Label Text"))
                .put("tooltip", new JsonObject().put("en", "Tooltip text")))))));

    JsonObject dict = service.buildTranslatableDict(newPayload, null, "en", Set.of("fr"));

    assertTrue(dict.containsKey("shapes/DatasetShape"));
    assertTrue(dict.containsKey("shapes/DatasetShape/properties/0"));

    JsonObject prop = dict.getJsonObject("shapes/DatasetShape/properties/0");
    assertEquals("Click Me", prop.getJsonObject("buttonLabel").getString("value"));

    JsonObject input = dict.getJsonObject("shapes/DatasetShape/properties/0/input");
    assertEquals("Label Text", input.getJsonObject("label").getString("value"));
    assertEquals("Tooltip text", input.getJsonObject("tooltip").getString("value"));
  }

  @Test
  void testBuildTranslatableDict_withNestedProperties() {
    JsonObject newPayload = new JsonObject()
      .put("shapes", new JsonObject()
        .put("NestedShape", new JsonObject()
          .put("properties", new JsonArray()
            .add(new JsonObject()
              .put("nestedProperties", new JsonArray()
                .add(new JsonObject()
                  .put("buttonLabel", new JsonObject().put("en", "Deep Label"))))))));

    JsonObject dict = service.buildTranslatableDict(newPayload, null, "en", Set.of("es"));

    assertTrue(dict.containsKey("shapes/NestedShape/properties/0/nestedProperties/0"));
    assertEquals("Deep Label",
      dict.getJsonObject("shapes/NestedShape/properties/0/nestedProperties/0")
        .getJsonObject("buttonLabel").getString("value"));
  }

  @Test
  void testApplyTranslationsToSchema_updatesPayload() {
    JsonObject payload = new JsonObject()
      .put("shapes", new JsonObject()
        .put("DatasetShape", new JsonObject()
          .put("name", new JsonObject().put("en", "Dataset"))));

    JsonObject responseDict = new JsonObject()
      .put("shapes/DatasetShape", new JsonObject()
        .put("name", new JsonObject()
          .put("translations", new JsonObject().put("fr", "Jeu de données"))));

    service.applyTranslationsToSchema(payload, responseDict);

    JsonObject translatedName = payload.getJsonObject("shapes")
      .getJsonObject("DatasetShape")
      .getJsonObject("name");

    assertEquals("Dataset", translatedName.getString("en"));
    assertEquals("Jeu de données", translatedName.getString("fr"));
  }

  @Test
  void testBuildTranslatableDict_diff_whenEnglishUnchanged_requestsOnlyMissingTargets() {
    // old has EN + FR; new has same EN and FR already present -> only DE is missing
    JsonObject oldPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject()
            .put("en", "Public")
            .put("fr", "Public FR"))));

    JsonObject newPayload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject()
          .put("label", new JsonObject()
            .put("en", "Public")
            .put("fr", "Public FR"))));

    JsonObject dict = service.buildTranslatableDict(newPayload, oldPayload, "en",
      Set.of("en", "fr", "de"));

    assertTrue(dict.containsKey("datasetAccessLevels/public"));
    JsonObject subject = dict.getJsonObject("datasetAccessLevels/public");
    JsonObject label = subject.getJsonObject("label");

    assertEquals("Public", label.getString("value"));
    JsonArray tl = label.getJsonArray("targetLanguages");
    assertEquals(1, tl.size());
    assertTrue(tl.contains("de")); // only the missing one
    assertFalse(tl.contains("fr")); // FR already present, so not requested
  }

  @Test
  void testApplyTranslationsToSchema_ignoresNumericErrorCodes_stringAndNumber() {
    JsonObject payload = new JsonObject().put("shapes", new JsonObject().put("DatasetShape",
      new JsonObject().put("name", new JsonObject().put("en", "Dataset"))));

    // "fr" is a valid translation; "bg" and "da" are error codes (string and number)
    JsonObject responseDict = new JsonObject()
      .put("shapes/DatasetShape", new JsonObject()
        .put("name", new JsonObject()
          .put("translations", new JsonObject()
            .put("fr", "Jeu de données")
            .put("bg", "40000") // error code as STRING
            .put("da", -20000) // error code as NUMBER
          )
        )
      );

    service.applyTranslationsToSchema(payload, responseDict);

    JsonObject name = payload.getJsonObject("shapes").getJsonObject("DatasetShape")
      .getJsonObject("name");

    assertEquals("Dataset", name.getString("en")); // kept
    assertEquals("Jeu de données", name.getString("fr"));
    assertNull(name.getString("bg")); // ignored numeric-string error
    assertNull(name.getString("da")); // ignored numeric error
  }

  @Test
  void testApplyTranslationsToSchema_withEmptyResponseDict() {
    JsonObject payload = new JsonObject()
      .put("datasetAccessLevels", new JsonObject()
        .put("public", new JsonObject().put("label", new JsonObject().put("en", "Public"))));

    JsonObject responseDict = new JsonObject()
      .put("datasetAccessLevels/public", new JsonObject()
        .put("label", new JsonObject())); // missing translations

    service.applyTranslationsToSchema(payload, responseDict);

    // Should remain unchanged
    JsonObject label = payload.getJsonObject("datasetAccessLevels")
      .getJsonObject("public")
      .getJsonObject("label");
    assertEquals("Public", label.getString("en"));
    assertNull(label.getString("fr"));
  }

  @Test
  void testBuildTranslatableDict_withNullValues() {
    JsonObject newPayload = new JsonObject()
      .put("shapes", new JsonObject()
        .put("Shape1", new JsonObject()
          .put("properties", new JsonArray().add(new JsonObject()))));

    JsonObject dict = service.buildTranslatableDict(newPayload, null, "en", Set.of("fr"));
    assertTrue(dict.isEmpty());
  }
}
