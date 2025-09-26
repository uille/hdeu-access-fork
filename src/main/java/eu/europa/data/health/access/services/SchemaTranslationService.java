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

import static eu.europa.data.health.access.utils.TranslationUtils.addTranslations;
import static java.util.Objects.nonNull;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class SchemaTranslationService {

  private static final String DATASET_ACCESS_LEVELS = "datasetAccessLevels";
  private static final String DESCRIPTION = "description";
  private static final String INPUT = "input";
  private static final String LABEL = "label";
  private static final String NESTED_PROPERTIES = "nestedProperties";
  private static final String PROPERTIES = "properties";
  private static final String SHAPES = "shapes";

  // Keys we care about in properties
  private static final String[] PROPERTY_TOP_LEVEL_KEYS = new String[]{"buttonLabel"};
  private static final String[] INPUT_KEYS = new String[]{
    LABEL, "tooltip", "usageNote", "placeholder", "checkboxLabel"
  };

  /**
   * Merges existing translations from oldPayload into newPayload where the source language text is
   * unchanged and the new payload lacks those translations.
   *
   * @param newPayload The new schema payload to update (mutated in place).
   * @param oldPayload The old schema payload to copy translations from.
   * @param source     The source language code (e.g., "en").
   * @param target     The set of target language codes (e.g., {"el", "de", "es"}).
   */
  public void mergeExistingTranslations(
    JsonObject newPayload,
    JsonObject oldPayload,
    String source,
    Set<String> target
  ) {
    if (newPayload == null || oldPayload == null) {
      return;
    }

    // Make sure source is not in target
    Set<String> targetCopy = new LinkedHashSet<>(target);
    targetCopy.remove(source);

    // datasetAccessLevels
    JsonObject newLevels = newPayload.getJsonObject(DATASET_ACCESS_LEVELS);
    JsonObject oldLevels = oldPayload.getJsonObject(DATASET_ACCESS_LEVELS);
    if (newLevels != null && oldLevels != null) {
      for (String key : newLevels.fieldNames()) {
        JsonObject newLevel = newLevels.getJsonObject(key);
        JsonObject oldLevel = oldLevels.getJsonObject(key);
        if (newLevel == null || oldLevel == null) {
          continue;
        }

        mergeWithOldTranslations(newLevel.getJsonObject(LABEL), oldLevel.getJsonObject(LABEL),
          source, targetCopy);
        mergeWithOldTranslations(newLevel.getJsonObject(DESCRIPTION),
          oldLevel.getJsonObject(DESCRIPTION), source, targetCopy);
      }
    }

    // shapes
    JsonObject newShapes = newPayload.getJsonObject(SHAPES);
    JsonObject oldShapes = oldPayload.getJsonObject(SHAPES);
    if (newShapes != null && oldShapes != null) {
      for (String key : newShapes.fieldNames()) {
        JsonObject newShape = newShapes.getJsonObject(key);
        JsonObject oldShape = oldShapes.getJsonObject(key);
        if (newShape == null || oldShape == null) {
          continue;
        }

        // shape name
        mergeWithOldTranslations(newShape.getJsonObject("name"), oldShape.getJsonObject("name"),
          source, targetCopy);

        // properties (match by path; recurse nestedProperties)
        JsonArray newProps = newShape.getJsonArray(PROPERTIES);
        JsonArray oldProps = oldShape.getJsonArray(PROPERTIES);
        if (newProps != null && oldProps != null) {
          mergeWithOldProps(newProps, oldProps, source, targetCopy);
        }
      }
    }
  }

  /**
   * Recursively merges translations from oldProps into newProps.
   *
   * @param newProps The new properties array to update (mutated in place).
   * @param oldProps The old properties array to copy translations from.
   * @param source   The source language code (e.g., "en").
   * @param target   The set of target language codes (e.g., {"el", "de", "es"}).
   */
  private void mergeWithOldProps(
    JsonArray newProps,
    JsonArray oldProps,
    String source,
    Set<String> target
  ) {
    for (int i = 0; i < newProps.size(); i++) {
      JsonObject newProp = newProps.getJsonObject(i);
      if (newProp == null) {
        continue;
      }

      // Find matching old prop by "path"
      JsonObject oldProp = oldProps.stream()
        .filter(
          o -> o instanceof JsonObject jo &&
            Objects.equals(jo.getString("path"), newProp.getString("path")))
        .map(o -> (JsonObject) o)
        .findFirst()
        .orElse(null);

      if (oldProp == null) {
        continue;
      }

      // property-level fields
      for (String key : PROPERTY_TOP_LEVEL_KEYS) {
        mergeWithOldTranslations(newProp.getJsonObject(key), oldProp.getJsonObject(key), source,
          target);
      }

      // input.*
      JsonObject newInput = newProp.getJsonObject(INPUT);
      JsonObject oldInput = oldProp.getJsonObject(INPUT);
      if (newInput != null && oldInput != null) {
        for (String key : INPUT_KEYS) {
          mergeWithOldTranslations(newInput.getJsonObject(key), oldInput.getJsonObject(key), source,
            target);
        }
      }

      // nestedProperties
      JsonArray newNested = newProp.getJsonArray(NESTED_PROPERTIES);
      JsonArray oldNested = oldProp.getJsonArray(NESTED_PROPERTIES);
      if (newNested != null && oldNested != null) {
        mergeWithOldProps(newNested, oldNested, source, target);
      }
    }
  }

  /**
   * Merges translations from oldObj into newObj when source is unchanged and newObj lacks those
   * translations.
   *
   * @param newObj The new multilingual JsonObject to update (mutated in place).
   * @param oldObj The old multilingual JsonObject to copy translations from.
   * @param source The source language code (e.g., "en").
   * @param target The set of target language codes (e.g., {"el", "de", "es"}).
   */
  private void mergeWithOldTranslations(
    JsonObject newObj,
    JsonObject oldObj,
    String source,
    Set<String> target
  ) {
    if (newObj == null || oldObj == null) {
      return;
    }

    String newValue = newObj.getString(source);
    String oldValue = oldObj.getString(source);
    if (!Objects.equals(newValue, oldValue) || StringUtils.isBlank(newValue)) {
      return;
    }

    for (String lang : target) {
      if (StringUtils.isNotBlank(newObj.getString(lang))) {
        continue;
      }

      String prev = oldObj.getString(lang);
      if (StringUtils.isNotBlank(prev)) {
        newObj.put(lang, prev);
      }
    }
  }

  /**
   * Builds the "dict" object expected by hub-translation from the full schema payload.
   *
   * @param newPayload The "payload" JsonObject from the new schema document.
   * @param oldPayload The "payload" JsonObject from the old schema document.
   * @param source     The source language code (e.g., "en").
   * @param target     The set of target language codes (e.g., {"el", "de", "es"}).
   * @return A JsonObject structured as subject -> property -> { value, sourceLanguage,
   * targetLanguages }
   */
  public JsonObject buildTranslatableDict(JsonObject newPayload, JsonObject oldPayload,
    String source, Set<String> target) {
    JsonObject dict = new JsonObject();

    // Make sure source is not in target
    Set<String> targetCopy = new LinkedHashSet<>(target);
    targetCopy.remove(source);

    // datasetAccessLevels.*.(label|description)
    JsonObject newLevels = newPayload.getJsonObject(DATASET_ACCESS_LEVELS);
    JsonObject oldLevels =
      oldPayload != null ? oldPayload.getJsonObject(DATASET_ACCESS_LEVELS) : new JsonObject();
    if (newLevels != null) {
      for (String key : newLevels.fieldNames()) {
        JsonObject newLevel = newLevels.getJsonObject(key);
        if (newLevel == null) {
          continue;
        }
        JsonObject oldLevel = oldLevels.getJsonObject(key, new JsonObject());

        String base = "datasetAccessLevels/" + key;
        putIfNeeded(dict, base, LABEL, newLevel.getJsonObject(LABEL), oldLevel.getJsonObject(LABEL),
          source, targetCopy);
        putIfNeeded(dict, base, DESCRIPTION, newLevel.getJsonObject(DESCRIPTION),
          oldLevel.getJsonObject(DESCRIPTION), source, targetCopy);
      }
    }

    // shapes.*.name and shapes.*.properties[...] (with recursion on nestedProperties)
    JsonObject newShapes = newPayload.getJsonObject(SHAPES);
    JsonObject oldShapes =
      oldPayload != null ? oldPayload.getJsonObject(SHAPES) : new JsonObject();

    if (newShapes != null) {
      for (String key : newShapes.fieldNames()) {
        JsonObject newShape = newShapes.getJsonObject(key);
        if (newShape == null) {
          continue;
        }
        JsonObject oldShape = oldShapes.getJsonObject(key, new JsonObject());

        String base = "shapes/" + key;
        putIfNeeded(dict, base, "name", newShape.getJsonObject("name"),
          oldShape.getJsonObject("name"), source, targetCopy);

        JsonArray newProps = newShape.getJsonArray(PROPERTIES);
        if (newProps != null) {
          JsonArray oldProps = oldShape.getJsonArray(PROPERTIES, new JsonArray());
          walkProperties(dict, newProps, oldProps, base + "/properties", source, targetCopy);
        }
      }
    }

    return dict;
  }

  /**
   * Recursively walks the properties array, extracting multilingual fields.
   *
   * @param dict     The dict object to populate.
   * @param newProps The new properties array.
   * @param oldProps The old properties array.
   * @param base     The base path for the current properties (e.g., "shapes/Shape1/properties").
   * @param source   The source language code.
   * @param target   The set of target language codes.
   */
  private void walkProperties(JsonObject dict, JsonArray newProps, JsonArray oldProps, String base,
    String source, Set<String> target) {
    for (int i = 0; i < newProps.size(); i++) {
      JsonObject newProp = newProps.getJsonObject(i);
      if (newProp == null) {
        continue;
      }
      JsonObject oldProp = oldProps.stream()
        .filter(
          o -> o instanceof JsonObject jo &&
            Objects.equals(jo.getString("path"), (newProp.getString("path")))
        )
        .map(o -> (JsonObject) o)
        .findFirst()
        .orElse(new JsonObject());

      String propBase = base + "/" + i;

      for (String key : PROPERTY_TOP_LEVEL_KEYS) {
        putIfNeeded(dict, propBase, key, newProp.getJsonObject(key), oldProp.getJsonObject(key),
          source, target);
      }

      JsonObject newInput = newProp.getJsonObject(INPUT);
      if (newInput != null) {
        JsonObject oldInput = oldProp.getJsonObject(INPUT, new JsonObject());
        for (String key : INPUT_KEYS) {
          putIfNeeded(dict, propBase + "/input", key, newInput.getJsonObject(key),
            oldInput.getJsonObject(key), source, target);
        }
      }

      JsonArray newNested = newProp.getJsonArray(NESTED_PROPERTIES);
      if (newNested != null) {
        JsonArray oldNested = oldProp.getJsonArray(NESTED_PROPERTIES, new JsonArray());
        walkProperties(dict, newNested, oldNested, propBase + "/nestedProperties", source, target);
      }
    }
  }

  /**
   * Adds the leaf to dict if it's a multilingual object containing { "en": "..." }
   *
   * @param dict            The dict object to populate.
   * @param base            The base path for the subject (e.g., "shapes/Shape1/properties/0").
   * @param key             The property key (e.g., "label", "description", "input.label").
   * @param newMultilingual The new multilingual JsonObject (e.g., { "en": "..." }).
   * @param oldMultilingual The old multilingual JsonObject (can be null).
   * @param source          The source language code.
   * @param target          The set of target language codes.
   */
  private void putIfNeeded(JsonObject dict, String base, String key,
    JsonObject newMultilingual, JsonObject oldMultilingual, String source, Set<String> target) {

    if (newMultilingual == null) {
      return;
    }

    String newValue = newMultilingual.getString(source);
    if (StringUtils.isBlank(newValue)) {
      return;
    }
    String oldValue = oldMultilingual != null ? oldMultilingual.getString(source) : null;

    // If the source text changed, then we need translation for all target languages,
    // otherwise only for those not already present.
    Set<String> needed = new LinkedHashSet<>();
    if (!newValue.equals(oldValue)) {
      needed.addAll(target);
    } else {
      for (String lang : target) {
        if (!newMultilingual.containsKey(lang)) {
          needed.add(lang);
        }
      }
    }
    if (needed.isEmpty()) {
      return;
    }

    JsonObject subject = dict.getJsonObject(base);
    if (subject == null) {
      subject = new JsonObject();
      dict.put(base, subject);
    }

    subject
      .put(key, new JsonObject()
        .put("value", newValue)
        .put("sourceLanguage", source)
        .put("targetLanguages", new JsonArray(new ArrayList<>(needed)))
      );
  }

  /**
   * Merges translation response dict back into the original schema payload (mutates the payload).
   * Response structure mirrors the request "dict": subject -> property -> { value, sourceLanguage,
   * translations{ lang: text } }
   *
   * @param payload The original schema payload to update.
   * @param dict    The translation response dict.
   */
  public void applyTranslationsToSchema(JsonObject payload, JsonObject dict) {
    if (dict == null) {
      return;
    }

    for (String subject : dict.fieldNames()) {
      JsonObject props = dict.getJsonObject(subject);
      if (props == null) {
        continue;
      }

      for (String key : props.fieldNames()) {
        JsonObject entry = props.getJsonObject(key, new JsonObject());
        JsonObject translations = entry.getJsonObject("translations");
        if (translations == null || translations.isEmpty()) {
          continue;
        }

        // Resolve where this property lives in the schema.
        // Subject is a pointer-like path (e.g., "shapes/dcatap:Dataset_Shape/properties/0/input").
        // Property key indicates the final multilingual object name at that location (e.g., "label"
        // or "tooltip").
        Optional<JsonObject> multilingualOpt = resolveMultilingualObject(payload, subject, key);
        multilingualOpt.ifPresent(multilingual -> addTranslations(multilingual, translations));
      }
    }
  }

  /**
   * Resolves the multilingual JsonObject at the given subject path and property key.
   *
   * @param root    The root payload JsonObject.
   * @param subject The subject path (e.g., "shapes/dcatap:Dataset_Shape/properties/0/input").
   * @param key     The property key (e.g., "label", "tooltip").
   * @return An Optional containing the multilingual JsonObject if found, or empty if not found.
   */
  private Optional<JsonObject> resolveMultilingualObject(JsonObject root, String subject,
    String key) {
    // Navigate by "/" segments; indices apply for arrays
    String[] segments = subject.split("/");
    Object current = root;
    for (String segment : segments) {
      if (segment.isEmpty()) {
        continue;
      }

      if (current instanceof JsonObject jo) {
        // If current is an object, segment should be a field name
        current = jo.getValue(segment);
        if (current == null) {
          return Optional.empty();
        }
      } else if (current instanceof JsonArray ja) {
        // If current is an array, segment must be an integer index
        try {
          int idx = Integer.parseInt(segment);
          if (idx < 0 || idx >= ja.size()) {
            return Optional.empty();
          }
          current = ja.getValue(idx);
        } catch (NumberFormatException nfe) {
          return Optional.empty();
        }
      } else {
        return Optional.empty();
      }
    }

    // Now current should be the parent JsonObject that contains the multilingual field named key
    if (current instanceof JsonObject jo) {
      JsonObject multilingual = jo.getJsonObject(key);
      if (nonNull(multilingual)) {
        return Optional.of(multilingual);
      }
    }
    return Optional.empty();
  }
}
