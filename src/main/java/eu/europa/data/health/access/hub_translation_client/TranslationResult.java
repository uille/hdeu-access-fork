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

import static eu.europa.data.health.access.Constants.ERROR;
import static eu.europa.data.health.access.Constants.STATUS;

import eu.europa.data.health.access.TranslationStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class TranslationResult {
  @Getter
  private JsonObject formStatus;
  @Getter
  private final Map<String, JsonObject> fileStatuses = new HashMap<>();
  @Getter
  private boolean failed = false;

  /**
   * Marks the form translation as PENDING and stores the translation payload.
   * <p>
   * <b>Important:</b> This initializes the form translation metadata with retry-enabling fields:
   * <ul>
   *   <li><b>status:</b> Set to PENDING to indicate translation is awaiting processing</li>
   *   <li><b>payload:</b> Stores the complete translation request payload for retry attempts</li>
   *   <li><b>createdAt:</b> Timestamp used by scheduler to determine retry timing (independent of application dates)</li>
   *   <li><b>attempts:</b> Initialized to 0, incremented on each retry, checked against maxAttempts</li>
   * </ul>
   * </p>
   *
   * <h3>Why Initialize These Fields?</h3>
   * <ul>
   *   <li><b>createdAt:</b> Scheduler uses this instead of date_submitted/date_modified to isolate translation timing from application updates</li>
   *   <li><b>attempts:</b> Enables retry limit tracking - when attempts >= maxAttempts (default: 3), translation is marked FAILED</li>
   *   <li><b>payload:</b> Stored for retry attempts - scheduler can resend same payload without rebuilding</li>
   * </ul>
   *
   * <h3>Resulting Database Structure:</h3>
   * <pre>
   * {
   *   "translation": {
   *     "form": {
   *       "status": "PENDING",
   *       "payload": { ...form translation request... },
   *       "createdAt": "2026-01-27T13:00:00Z",
   *       "attempts": 0
   *     }
   *   }
   * }
   * </pre>
   *
   * @param payload The complete translation request payload to store for retries
   * @see eu.europa.data.health.access.scheduled_jobs.TranslationRetryScheduledJob
   */
  public void markFormPending(JsonObject payload) {
    this.formStatus = new JsonObject()
      .put(STATUS, TranslationStatus.PENDING.name())
      .put("payload", payload)
      .put("createdAt", Instant.now().toString())
      .put("attempts", 0);  // Initialize for retry tracking
  }

  public void markFormSuccess() {
    if (this.formStatus == null) {
      this.formStatus = new JsonObject();
    }
    this.formStatus.put(STATUS, TranslationStatus.COMPLETED.name());
  }

  public void markFormFailure(Throwable err) {
    this.failed = true;
    if (this.formStatus == null) {
      this.formStatus = new JsonObject();
    }
    this.formStatus
      .put(STATUS, TranslationStatus.FAILED.name())
      .put(ERROR, err.getMessage());
  }

  public void addFileStatus(String fileId, TranslationStatus status, Throwable error) {
    if (fileId == null) return;

    JsonObject existing = this.fileStatuses.getOrDefault(fileId, new JsonObject());
    existing.put(STATUS, status.name());

    if (error != null) {
      existing.put(ERROR, error.getMessage());
      this.failed = true;
    }

    this.fileStatuses.put(fileId, existing);
  }

  public void markFilesFailure(Throwable err, List<String> fileIds) {
    this.failed = true;
    for (String fileId : fileIds) {
      if (fileId != null) {
       markFileFailure(err, fileId);
      }
    }
  }

  public void markFileFailure(Throwable err, String fileId) {
    this.fileStatuses.put(fileId, new JsonObject()
      .put(STATUS, TranslationStatus.FAILED.name())
      .put(ERROR, err.getMessage()));
  }

  public JsonObject toMongoUpdate(String sourceLang, List<String> targetLanguages, JsonObject englishFormTranslation, Boolean changeTargetLanguages) {
    JsonObject update = new JsonObject();
    JsonObject setObject = new JsonObject();

    if (formStatus != null) {
      setObject.put("translation.form", formStatus);
    }

    if(englishFormTranslation != null && !englishFormTranslation.isEmpty()) {
      setObject.put("form_translations", englishFormTranslation);
    }

    if (!fileStatuses.isEmpty()) {
      JsonObject filesObject = new JsonObject();
      for (Map.Entry<String, JsonObject> entry : fileStatuses.entrySet()) {
        filesObject.put(entry.getKey(), entry.getValue());
      }
      setObject.put("translation.files", filesObject);
    }

    if(changeTargetLanguages){
      setObject.put("translation.status", failed ? TranslationStatus.FAILED.name() : TranslationStatus.FORM_PENDING.name());
    }

    if(StringUtils.isNotEmpty(sourceLang))
      setObject.put("translation.sourceLanguage", sourceLang);

    if(targetLanguages != null && !targetLanguages.isEmpty() && changeTargetLanguages) {
      setObject.put("translation.targetLanguages", new JsonArray(targetLanguages));
    }

    update.put("$set", setObject);
    return update;
  }

}
