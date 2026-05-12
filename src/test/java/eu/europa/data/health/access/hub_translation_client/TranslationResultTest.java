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
package eu.europa.data.health.access.hub_translation_client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.europa.data.health.access.TranslationStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TranslationResultTest {

  @Test
  void testMarkFormPending_setsPendingPayloadAndRetryMetadata() {
    TranslationResult result = new TranslationResult();
    JsonObject payload = new JsonObject().put("field", "value");

    result.markFormPending(payload);

    JsonObject form = result
      .toMongoUpdate(null, null, null, true)
      .getJsonObject("$set")
      .getJsonObject("translation.form");

    assertEquals(TranslationStatus.PENDING.name(), form.getString("status"));
    assertEquals(payload, form.getJsonObject("payload"));
    assertEquals(0, form.getInteger("attempts"));
    assertNotNull(form.getString("createdAt"));
    assertEquals(
      form.getString("createdAt"),
      Instant.parse(form.getString("createdAt")).toString()
    );
  }

  @Test
  void testMarkFormSuccessWithoutPending() {
    TranslationResult result = new TranslationResult();

    result.markFormSuccess();

    JsonObject form = result.toMongoUpdate(null, null, null, true)
      .getJsonObject("$set").getJsonObject("translation.form");

    assertEquals(TranslationStatus.COMPLETED.name(), form.getString("status"));
  }

  @Test
  void testMarkFormFailure_setsFailedFlagAndError() {
    TranslationResult result = new TranslationResult();
    Throwable error = new RuntimeException("Form error");

    result.markFormFailure(error);

    JsonObject form = result.toMongoUpdate(null, null, null, true)
      .getJsonObject("$set").getJsonObject("translation.form");

    assertEquals(TranslationStatus.FAILED.name(), form.getString("status"));
    assertEquals("Form error", form.getString("error"));
    assertEquals(TranslationStatus.FAILED.name(),
      result.toMongoUpdate(null, null, null, true).getJsonObject("$set").getString("translation.status"));
  }

  @Test
  void testAddFileStatus_withErrorSetsFailed() {
    TranslationResult result = new TranslationResult();

    result.addFileStatus("file1", TranslationStatus.FAILED, new RuntimeException("File failed"));

    JsonObject files = result.toMongoUpdate(null, null, null, true)
      .getJsonObject("$set").getJsonObject("translation.files");

    assertEquals(TranslationStatus.FAILED.name(),
      files.getJsonObject("file1").getString("status"));
    assertEquals("File failed", files.getJsonObject("file1").getString("error"));
    assertEquals(TranslationStatus.FAILED.name(),
      result.toMongoUpdate(null, null, null, true).getJsonObject("$set").getString("translation.status"));
  }

  @Test
  void testMarkFilesFailure_multipleFiles() {
    TranslationResult result = new TranslationResult();
    Throwable error = new RuntimeException("Batch failure");

    result.markFilesFailure(error, List.of("f1", "f2"));

    JsonObject files = result.toMongoUpdate(null, null, null, true)
      .getJsonObject("$set").getJsonObject("translation.files");

    assertEquals(TranslationStatus.FAILED.name(),
      files.getJsonObject("f1").getString("status"));
    assertEquals("Batch failure", files.getJsonObject("f2").getString("error"));
  }

  @Test
  void testToMongoUpdate_setsFormTranslationsSourceAndTargetLanguages() {
    TranslationResult result = new TranslationResult();
    JsonObject englishFormTranslation = new JsonObject().put("title", "Hello world");
    List<String> targetLanguages = List.of("fr", "de");

    JsonObject setObject = result
      .toMongoUpdate("en", targetLanguages, englishFormTranslation, true)
      .getJsonObject("$set");

    assertEquals(englishFormTranslation, setObject.getJsonObject("form_translations"));
    assertEquals("en", setObject.getString("translation.sourceLanguage"));
    assertEquals(
      new JsonArray(targetLanguages),
      setObject.getJsonArray("translation.targetLanguages")
    );
  }
}
