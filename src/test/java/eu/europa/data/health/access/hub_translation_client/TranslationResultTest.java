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
import static org.junit.jupiter.api.Assertions.assertFalse;

import eu.europa.data.health.access.TranslationStatus;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class TranslationResultTest {

  @Test
  void testMarkFormPending() {
    TranslationResult result = new TranslationResult();
    JsonObject payload = new JsonObject().put("field", "value");

    result.markFormPending(payload);

    JsonObject update = result.toMongoUpdate();
    JsonObject form = update.getJsonObject("$set").getJsonObject("translation.form");

    assertEquals(TranslationStatus.PENDING.name(), form.getString("status"));
    assertEquals("value", form.getJsonObject("payload").getString("field"));
    assertEquals(TranslationStatus.PENDING.name(),
      update.getJsonObject("$set").getString("translation.status"));
  }

  @Test
  void testMarkFormSuccessWithoutPending() {
    TranslationResult result = new TranslationResult();

    result.markFormSuccess();

    JsonObject form = result.toMongoUpdate()
      .getJsonObject("$set").getJsonObject("translation.form");

    assertEquals(TranslationStatus.COMPLETED.name(), form.getString("status"));
  }

  @Test
  void testMarkFormFailure_setsFailedFlagAndError() {
    TranslationResult result = new TranslationResult();
    Throwable error = new RuntimeException("Form error");

    result.markFormFailure(error);

    JsonObject form = result.toMongoUpdate()
      .getJsonObject("$set").getJsonObject("translation.form");

    assertEquals(TranslationStatus.FAILED.name(), form.getString("status"));
    assertEquals("Form error", form.getString("error"));
    assertEquals(TranslationStatus.FAILED.name(),
      result.toMongoUpdate().getJsonObject("$set").getString("translation.status"));
  }

  @Test
  void testAddFileStatus_successCase() {
    TranslationResult result = new TranslationResult();

    result.addFileStatus("file1", TranslationStatus.COMPLETED);

    JsonObject files = result.toMongoUpdate()
      .getJsonObject("$set").getJsonObject("translation.files");

    assertEquals(TranslationStatus.COMPLETED.name(),
      files.getJsonObject("file1").getString("status"));
  }

  @Test
  void testAddFileStatus_withErrorSetsFailed() {
    TranslationResult result = new TranslationResult();

    result.addFileStatus("file1", TranslationStatus.FAILED, new RuntimeException("File failed"));

    JsonObject files = result.toMongoUpdate()
      .getJsonObject("$set").getJsonObject("translation.files");

    assertEquals(TranslationStatus.FAILED.name(),
      files.getJsonObject("file1").getString("status"));
    assertEquals("File failed", files.getJsonObject("file1").getString("error"));
    assertEquals(TranslationStatus.FAILED.name(),
      result.toMongoUpdate().getJsonObject("$set").getString("translation.status"));
  }

  @Test
  void testMarkFilesFailure_multipleFiles() {
    TranslationResult result = new TranslationResult();
    Throwable error = new RuntimeException("Batch failure");

    result.markFilesFailure(error, List.of("f1", "f2"));

    JsonObject files = result.toMongoUpdate()
      .getJsonObject("$set").getJsonObject("translation.files");

    assertEquals(TranslationStatus.FAILED.name(),
      files.getJsonObject("f1").getString("status"));
    assertEquals("Batch failure", files.getJsonObject("f2").getString("error"));
  }

  @Test
  void testAddFileStatus_nullFileIdIgnored() {
    TranslationResult result = new TranslationResult();

    result.addFileStatus(null, TranslationStatus.PENDING);

    JsonObject update = result.toMongoUpdate();
    assertFalse(update.getJsonObject("$set").containsKey("translation.files"));
  }

  @Test
  void testToMongoUpdate_emptyResult_defaultsToPending() {
    TranslationResult result = new TranslationResult();

    JsonObject update = result.toMongoUpdate();

    assertEquals(TranslationStatus.PENDING.name(),
      update.getJsonObject("$set").getString("translation.status"));
    assertFalse(update.getJsonObject("$set").containsKey("translation.form"));
    assertFalse(update.getJsonObject("$set").containsKey("translation.files"));
  }
}
