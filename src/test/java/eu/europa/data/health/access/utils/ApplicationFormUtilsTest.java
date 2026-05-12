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
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.data.health.access.TranslationStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationFormUtilsTest {

  @Test
  void testExtractDocumentIds_simple() {
    JsonObject form = new JsonObject()
      .put("attachments", new JsonArray()
        .add(new JsonObject()
          .put("id", "file1")
          .put("name", "doc1.pdf")
          .put("size", 123))
        .add(new JsonObject()
          .put("id", "file2")
          .put("name", "doc2.pdf")
          .put("size", 456)));

    List<String> ids = ApplicationFormUtils.extractDocumentIds(form);

    assertEquals(2, ids.size());
    assertTrue(ids.contains("file1"));
    assertTrue(ids.contains("file2"));
  }

//  @Test
//  void testDetermineTranslationStatus_completed() {
//    List<String> received = Arrays.asList("en", "fr");
//    List<String> expected = Arrays.asList("EN", "FR");
//
//    TranslationStatus status = ApplicationFormUtils.determineTranslationStatus(
//      received, expected, "form", "app1");
//
//    assertEquals(TranslationStatus.COMPLETED, status);
//  }

//  @Test
//  void testDetermineTranslationStatus_partiallyCompleted_missingLang() {
//    List<String> received = Arrays.asList("en");
//    List<String> expected = Arrays.asList("EN", "FR");
//
//    TranslationStatus status = ApplicationFormUtils.determineTranslationStatus(
//      received, expected, "file", "app2");
//
//    assertEquals(TranslationStatus.PARTIALLY_COMPLETED, status);
//  }

//  @Test
//  void testDetermineTranslationStatus_noExpectedLangs() {
//    List<String> received = Arrays.asList("en");
//    List<String> expected = List.of();
//
//    TranslationStatus status = ApplicationFormUtils.determineTranslationStatus(
//      received, expected, "form", "app3");
//
//    assertEquals(TranslationStatus.PARTIALLY_COMPLETED, status);
//  }

  @Test
  void testDetermineOverallTranslationStatus_allCompleted() {
    JsonObject translation = new JsonObject()
      .put("form", new JsonObject().put("status", "COMPLETED"))
      .put("files", new JsonObject()
        .put("f1", new JsonObject().put("status", "COMPLETED"))
        .put("f2", new JsonObject().put("status", "COMPLETED")));

    TranslationStatus status = ApplicationFormUtils.determineOverallTranslationStatus(translation);

    assertEquals(TranslationStatus.COMPLETED, status);
  }

//  @Test
//  void testDetermineOverallTranslationStatus_pending() {
//    JsonObject translation = new JsonObject()
//      .put("form", new JsonObject().put("status", "PENDING"))
//      .put("files", new JsonObject()
//        .put("f1", new JsonObject().put("status", "PENDING")));
//
//    TranslationStatus status = ApplicationFormUtils.determineOverallTranslationStatus(translation);
//
//    assertEquals(TranslationStatus.PENDING, status);
//  }

  @Test
  void testUpdateTranslatedFileReferencesInForm_objectReplacement() {
    JsonObject formSection = new JsonObject()
      .put("doc", new JsonObject()
        .put("id", "orig1")
        .put("name", "old.docx")
        .put("size", 500));

    ApplicationFormUtils.updateTranslatedFileReferencesInForm(
      formSection, "orig1", "new1", "new.docx");

    JsonObject updated = formSection.getJsonObject("doc");
    assertEquals("new1", updated.getString("id"));
    assertEquals("new.docx", updated.getString("name"));
    assertEquals(500, updated.getInteger("size"));
  }

  @Test
  void testUpdateTranslatedFileReferencesInForm_arrayReplacement() {
    JsonObject formSection = new JsonObject()
      .put("docs", new JsonArray()
        .add(new JsonObject()
          .put("id", "orig1")
          .put("name", "old.docx")
          .put("size", 200))
        .add(new JsonObject()
          .put("id", "orig2")
          .put("name", "other.docx")
          .put("size", 300)));

    ApplicationFormUtils.updateTranslatedFileReferencesInForm(
      formSection, "orig1", "new1", "new.docx");

    JsonArray docs = formSection.getJsonArray("docs");
    JsonObject updated = docs.getJsonObject(0);
    assertEquals("new1", updated.getString("id"));
    assertEquals("new.docx", updated.getString("name"));
    assertEquals(200, updated.getInteger("size"));
  }

}
