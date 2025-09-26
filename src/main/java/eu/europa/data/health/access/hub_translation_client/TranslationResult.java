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

import eu.europa.data.health.access.TranslationStatus;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;


public class TranslationResult {
  @Getter
  private JsonObject formStatus;
  @Getter
  private final Map<String, JsonObject> fileStatuses = new HashMap<>();
  @Getter
  private boolean failed = false;

  public void markFormPending(JsonObject payload) {
    this.formStatus = new JsonObject()
      .put("status", TranslationStatus.PENDING.name())
      .put("payload", payload);
  }

  public void markFormSuccess() {
    if (this.formStatus == null) {
      this.formStatus = new JsonObject();
    }
    this.formStatus.put("status", TranslationStatus.COMPLETED.name());
  }

  public void markFormFailure(Throwable err) {
    this.failed = true;
    if (this.formStatus == null) {
      this.formStatus = new JsonObject();
    }
    this.formStatus
      .put("status", TranslationStatus.FAILED.name())
      .put("error", err.getMessage());
  }

  public void addFileStatus(String fileId, TranslationStatus status, Throwable error) {
    if (fileId == null) return;

    JsonObject existing = this.fileStatuses.getOrDefault(fileId, new JsonObject());
    existing.put("status", status.name());

    if (error != null) {
      existing.put("error", error.getMessage());
      this.failed = true;
    }

    this.fileStatuses.put(fileId, existing);
  }

  public void addFileStatus(String fileId, TranslationStatus status) {
    addFileStatus(fileId, status, null);
  }

  public void markFilesFailure(Throwable err, List<String> fileIds) {
    this.failed = true;
    for (String fileId : fileIds) {
      if (fileId != null) {
        this.fileStatuses.put(fileId, new JsonObject()
          .put("status", TranslationStatus.FAILED.name())
          .put("error", err.getMessage()));
      }
    }
  }

  public JsonObject toMongoUpdate() {
    JsonObject update = new JsonObject();
    JsonObject setObject = new JsonObject();

    if (formStatus != null) {
      setObject.put("translation.form", formStatus);
    }

    if (!fileStatuses.isEmpty()) {
      JsonObject filesObject = new JsonObject();
      for (Map.Entry<String, JsonObject> entry : fileStatuses.entrySet()) {
        filesObject.put(entry.getKey(), entry.getValue());
      }
      setObject.put("translation.files", filesObject);
    }

    setObject.put("translation.status", failed ? TranslationStatus.FAILED.name() : TranslationStatus.PENDING.name());

    update.put("$set", setObject);
    return update;
  }

}
