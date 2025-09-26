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

import eu.europa.data.health.access.TranslationStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationFormUtils {

  private static final Logger log = LoggerFactory.getLogger(ApplicationFormUtils.class);

  public static List<String> extractDocumentIds(JsonObject form) {
    Set<String> ids = new HashSet<>();
    extractIdsRecursive(form, ids);
    return new ArrayList<>(ids);
  }

  private static void extractIdsRecursive(Object obj, Set<String> ids) {
    if (obj instanceof JsonObject jsonObject) {
      if (jsonObject.containsKey("id") && jsonObject.getValue("id") instanceof String) {
        // AttachmentDTO structure has name and size properties
        if (jsonObject.containsKey("name") && jsonObject.containsKey("size")) {
          ids.add(jsonObject.getString("id"));
        }
      }
      for (String key : jsonObject.fieldNames()) {
        extractIdsRecursive(jsonObject.getValue(key), ids);
      }
    } else if (obj instanceof JsonArray jsonArray) {
      for (Object element : jsonArray) {
        extractIdsRecursive(element, ids);
      }
    }
  }

  /**
   * Determines whether the received translations match the expected target languages for a form or
   * file translation.
   * <p>
   * This method compares the set of languages for which translations have been received against the
   * expected list of target languages. It normalizes both sets to uppercase and returns: -
   * {@code COMPLETED} if all expected languages are present. - {@code PARTIALLY_COMPLETED} if some
   * or all expected languages are missing.
   * <p>
   * A warning is logged if the expected list is null or empty, or if any expected languages are
   * missing.
   *
   * @param receivedLangsRaw A collection of received language codes (e.g., from a callback
   *                         payload).
   * @param expectedLangsRaw A collection of expected language codes (e.g., defined in the
   *                         request).
   * @param typeLabel        A label indicating the context ("form", "file", etc.) for logging
   *                         purposes.
   * @param applicationId    The application ID associated with this translation check.
   * @return {@code COMPLETED} if all expected translations are received,
   * {@code PARTIALLY_COMPLETED} otherwise.
   */
  public static TranslationStatus determineTranslationStatus(
    Collection<String> receivedLangsRaw,
    Collection<String> expectedLangsRaw,
    String typeLabel,
    String applicationId
  ) {
    if (expectedLangsRaw == null || expectedLangsRaw.isEmpty()) {
      log.warn(
        "No targetLanguages provided for application {}, cannot validate translation completeness",
        applicationId);
      return TranslationStatus.FAILED;
    }

    Set<String> receivedLangs = receivedLangsRaw.stream()
      .filter(Objects::nonNull)
      .map(String::toUpperCase)
      .collect(Collectors.toSet());

    Set<String> expectedLangs = expectedLangsRaw.stream()
      .filter(Objects::nonNull)
      .map(String::toUpperCase)
      .collect(Collectors.toSet());

    if (!receivedLangs.containsAll(expectedLangs)) {
      Set<String> missing = new HashSet<>(expectedLangs);
      missing.removeAll(receivedLangs);
      log.warn("Some expected {} translations are missing for application {}: {}", typeLabel,
        applicationId, missing);
      return TranslationStatus.PENDING;
    }

    return TranslationStatus.COMPLETED;
  }

  /**
   * Evaluates the overall translation status of an application by checking both form and file
   * translation statuses.
   * <p>
   * The method reads the "status" field inside the "form" object and inside each entry under
   * "files". It returns: - {@code COMPLETED} if all form and file statuses are COMPLETED. -
   * {@code PARTIALLY_COMPLETED} if any status is PARTIALLY_COMPLETED. - {@code PENDING} otherwise
   * (including when the translation object is null or any status is still pending).
   *
   * @param translation The full translation object containing "form" and "files" status
   *                    information.
   * @return The overall {@link TranslationStatus} of the application.
   */
  public static TranslationStatus determineOverallTranslationStatus(JsonObject translation) {
    if (translation == null) {
      return TranslationStatus.PENDING;
    }

    TranslationStatus formStatus = TranslationStatus.fromStatus(
      translation.getJsonObject("form", new JsonObject()).getString("status", "PENDING"));

    Map<String, TranslationStatus> fileStatuses = new HashMap<>();
    JsonObject files = translation.getJsonObject("files", new JsonObject());

    for (String fileId : files.fieldNames()) {
      JsonObject fileEntry = files.getJsonObject(fileId);
      String statusStr = fileEntry != null ? fileEntry.getString("status", "PENDING") : "PENDING";
      fileStatuses.put(fileId, TranslationStatus.fromStatus(statusStr));
    }

    List<TranslationStatus> allStatuses = new ArrayList<>();
    allStatuses.add(formStatus);
    allStatuses.addAll(fileStatuses.values());

    if (allStatuses.stream().anyMatch(s -> s == TranslationStatus.FAILED)) {
      return TranslationStatus.FAILED;
    }

    if (allStatuses.stream().allMatch(s -> s == TranslationStatus.COMPLETED)) {
      return TranslationStatus.COMPLETED;
    }

    return TranslationStatus.PENDING;
  }

  /**
   * Recursively traverses a translated form section to locate and replace any file reference
   * matching the given original file ID with a new translated file reference.
   * <p>
   * This method handles both direct file references (i.e., JsonObjects with an "id" field) and
   * lists of such references (JsonArrays). If the original file is found, it is replaced in-place
   * with a new JsonObject that contains the translated file ID and name, while preserving the
   * original file's "size" field if present.
   *
   * @param formSection    The root JsonObject of a translated form section (e.g.,
   *                       form_translations.en).
   * @param originalFileId The file ID of the original file to be replaced.
   * @param newFileId      The file ID of the translated file that will replace the original.
   * @param newFileName    The file name of the translated file.
   */
  public static void updateTranslatedFileReferencesInForm(
    JsonObject formSection,
    String originalFileId,
    String newFileId,
    String newFileName
  ) {
    for (String key : formSection.fieldNames()) {
      Object value = formSection.getValue(key);

      if (value instanceof JsonObject valueObj) {
        // If it's a direct file reference, replace it if it matches
        if (originalFileId.equals(valueObj.getString("id"))) {
          JsonObject newFileRef = new JsonObject()
            .put("id", newFileId)
            .put("name", newFileName);
          if (valueObj.containsKey("size")) {
            newFileRef.put("size", valueObj.getInteger("size"));
          }
          log.info("Replacing original fileId {} with translated fileId {} in key '{}' (object)",
            originalFileId, newFileId, key);
          formSection.put(key, newFileRef);
          continue;
        }
        // Otherwise recurse
        updateTranslatedFileReferencesInForm(valueObj, originalFileId, newFileId, newFileName);
      } else if (value instanceof JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
          Object element = array.getValue(i);

          if (element instanceof JsonObject fileObj) {
            if (originalFileId.equals(fileObj.getString("id"))) {
              JsonObject newFileRef = new JsonObject()
                .put("id", newFileId)
                .put("name", newFileName);
              if (fileObj.containsKey("size")) {
                newFileRef.put("size", fileObj.getInteger("size"));
              }
              log.info(
                "Replacing original fileId {} with translated fileId {} in array at key '{}' [index {}]",
                originalFileId, newFileId, key, i);
              array.set(i, newFileRef); // Replace in-place
            } else {
              // Recurse in nested structures within the array element
              updateTranslatedFileReferencesInForm(fileObj, originalFileId, newFileId, newFileName);
            }
          }
        }
      }
    }
  }

}
