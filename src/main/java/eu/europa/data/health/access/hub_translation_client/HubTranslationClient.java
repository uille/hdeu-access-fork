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

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.data.health.access.TranslationStatus;
import eu.europa.data.health.access.annotations.CheckEnumValue;
import eu.europa.data.health.access.annotations.TranslatableField;
import eu.europa.data.health.access.dto.sections.Section1DTO;
import eu.europa.data.health.access.dto.sections.Section2DTO;
import eu.europa.data.health.access.dto.sections.Section3DTO;
import eu.europa.data.health.access.dto.sections.Section4DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section10DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section5BindToOtherSections;
import eu.europa.data.health.access.dto.sections.data_access.Section5DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section6CountryDTO;
import eu.europa.data.health.access.dto.sections.data_access.Section7DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section8BindToOtherSections;
import eu.europa.data.health.access.dto.sections.data_access.Section8DTO;
import eu.europa.data.health.access.dto.sections.data_access.Section9DTO;
import eu.europa.data.health.access.services.TranslationManager;
import eu.europa.data.health.access.utils.ApplicationFormUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hub translation client
 */
public class HubTranslationClient {

  private final String translationUrl;
  private final String translationFileUrl;
  private final String callbackUrl;
  private final String xTranslationApiKey;
  private final String xAccessApiKey;
  private final TranslationManager translationManager;
  private static final Map<String, List<Class<?>>> SECTION_DTO_MAP_REQUEST = Map.of(
    "section1", List.of(Section1DTO.class),
    "section2", List.of(Section2DTO.class),
    "section3", List.of(Section3DTO.class),
    "section4", List.of(Section4DTO.class),
    "section5", List.of(eu.europa.data.health.access.dto.sections.data_request.Section5DTO.class,
      eu.europa.data.health.access.dto.sections.data_request.Section5BindToOtherSections.class),
    "section6",
    List.of(eu.europa.data.health.access.dto.sections.data_request.Section6CountryDTO.class,
      eu.europa.data.health.access.dto.sections.data_request.Section6BindToOtherSections.class),
    "section7", List.of(eu.europa.data.health.access.dto.sections.data_request.Section7DTO.class),
    "section8", List.of(eu.europa.data.health.access.dto.sections.data_request.Section8DTO.class)
  );

  private static final Map<String, List<Class<?>>> SECTION_DTO_MAP_ACCESS = Map.of(
    "section1", List.of(Section1DTO.class),
    "section2", List.of(Section2DTO.class),
    "section3", List.of(Section3DTO.class),
    "section4", List.of(Section4DTO.class),
    "section5", List.of(Section5DTO.class, Section5BindToOtherSections.class),
    "section6", List.of(Section6CountryDTO.class),
    "section7", List.of(Section7DTO.class),
    "section8", List.of(Section8DTO.class, Section8BindToOtherSections.class),
    "section9", List.of(Section9DTO.class),
    "section10", List.of(Section10DTO.class)
  );

  private static final Map<Class<?>, Map<String, EnumRef>> ENUM_MAP_CACHE = new ConcurrentHashMap<>();

  WebClient vertxClient;

  public HubTranslationClient(String translationUrl,
    String translationFileUrl,
    String callbackUrl,
    String xTranslationApiKey,
    String xAccessApiKey,
    WebClient client,
    TranslationManager translationManager) {
    this.translationUrl = translationUrl;
    this.translationFileUrl = translationFileUrl;
    this.callbackUrl = callbackUrl;
    this.xTranslationApiKey = xTranslationApiKey;
    this.xAccessApiKey = xAccessApiKey;
    this.vertxClient = client;
    this.translationManager = translationManager;
  }

  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String HUB_TRANSLATION_CLIENT = "HUB_TRANSLATION_CLIENT";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_DETAILS = "errorDetails";

  public MultiMap createHeader(Buffer buffer) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("content-type", "application/json");
    headers.add("content-length", String.valueOf(buffer.length()));
    if (StringUtils.isNotEmpty(this.xTranslationApiKey)) {
      headers.add("X-API-Key", this.xTranslationApiKey);
    }
    return headers;
  }

  private JsonObject createHeaderJson() {
    JsonObject headers = new JsonObject();
    if (StringUtils.isNotEmpty(this.xAccessApiKey)) {
      headers.put("X-API-Key", this.xAccessApiKey);
    }
    return headers;
  }

  public JsonObject createCallbackJson() {
    return new JsonObject()
      .put("url", callbackUrl)
      .put("method", "POST")
      .put("headers", createHeaderJson());
  }

  /**
   * Builds a translation request payload to be sent to the hub-translation service.
   * <p>
   * Based on the application type, selects the appropriate DTO mapping to extract and structure the
   * translatable fields from the input form JSON. Constructs a payload that includes: - The
   * structured translatable dictionary - Metadata such as application ID, type, default and target
   * languages - A callback configuration that tells the translation service where to POST results
   *
   * @param applicationId   Unique identifier of the application being translated.
   * @param dict            Original application form data as a JSON object.
   * @param applicationType Type of the application (e.g., "DATA_REQUEST_APPLICATION".
   * @param sourceLang      Source language code (e.g., "en").
   * @param targetLangs     List of target language codes (e.g., ["el", "de"]).
   * @return A complete translation request payload as a JsonObject, ready to send to
   * hub-translation.
   * @throws IllegalArgumentException if the applicationType is not supported.
   */
  public JsonObject buildTranslationRequestPayload(String applicationId, JsonObject dict,
    String applicationType, String sourceLang, List<String> targetLangs) {
    Map<String, List<Class<?>>> sectionDtoMap;

    switch (applicationType) {
      case DATA_REQUEST_APPLICATION_COLLECTION -> sectionDtoMap = SECTION_DTO_MAP_REQUEST;
      case DATA_ACCESS_APPLICATION_COLLECTION -> sectionDtoMap = SECTION_DTO_MAP_ACCESS;
      default -> {
        log.warn("Unknown application type: {}", applicationType);
        throw new IllegalArgumentException("Unsupported application type: " + applicationType);
      }
    }

    JsonObject translatablePayload = buildTranslatablePayload(dict, sectionDtoMap, sourceLang,
      targetLangs);

    JsonObject callback = createCallbackJson();

    JsonObject data = new JsonObject()
      .put("applicationId", applicationId)
      .put("applicationType", applicationType)
      .put("defaultLanguage", "EN")
      .put("targetLanguages", targetLangs);

    return new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("data", data)
      .put("dict", translatablePayload)
      .put("callback", callback);
  }

  /**
   * Extracts and enriches translatable fields from the form JSON using DTO field annotations.
   * <p>
   * For each section, it finds fields annotated with {@code @TranslatableField} and wraps their
   * values as:
   * <p>
   * { "value": "...", "sourceLanguage": "...", "targetLanguages": [...] }
   *
   * @param formJson      The full form data (e.g. with section1, section2, etc.).
   * @param sectionDtoMap Map of section names to a list of DTO classes defining translatable
   *                      fields.
   * @param sourceLang    Source language code (e.g., "en").
   * @param targetLangs   List of target language codes.
   * @return JsonObject with only the translatable fields, properly wrapped for hub-translation.
   */
  private JsonObject buildTranslatablePayload(JsonObject formJson,
    Map<String, List<Class<?>>> sectionDtoMap,
    String sourceLang,
    List<String> targetLangs) {
    JsonObject result = new JsonObject();

    for (String sectionKey : formJson.fieldNames()) {
      Object sectionValue = formJson.getValue(sectionKey);
      List<Class<?>> dtoClasses = sectionDtoMap.get(sectionKey);
      if (dtoClasses == null || dtoClasses.isEmpty()) {
        continue;
      }

      JsonObject sectionTranslations = new JsonObject();

      // Handle section as array (e.g., section6)
      if (sectionValue instanceof JsonArray sectionArray) {
        for (int i = 0; i < sectionArray.size(); i++) {
          JsonObject arrayItem = sectionArray.getJsonObject(i);
          String prefix = i + ""; // e.g., "0"

          for (Class<?> dtoClass : dtoClasses) {
            JsonObject itemTranslations = buildFlatTranslatableEntries(
              arrayItem, dtoClass, sourceLang, targetLangs, prefix
            );
            sectionTranslations.mergeIn(itemTranslations);
          }
        }

        // Handle section as object (e.g., section5)
      } else if (sectionValue instanceof JsonObject sectionObj) {
        for (Class<?> dtoClass : dtoClasses) {
          JsonObject itemTranslations = buildFlatTranslatableEntries(
            sectionObj, dtoClass, sourceLang, targetLangs, null
          );
          sectionTranslations.mergeIn(itemTranslations);
        }
      }

      if (!sectionTranslations.isEmpty()) {
        result.put(sectionKey, sectionTranslations);
      }
    }

    return result;
  }

  /**
   * Recursively builds a flattened map of translatable entries from a JsonObject form section,
   * using DTO metadata and field annotations.
   * <p>
   * For every field annotated with `@TranslatableField`, a translation entry is created in the
   * returned JsonObject with key paths like `"fieldName"` or `"0.subArrayField.1.fieldName"` and
   * corresponding values including source/target languages and the text value.
   * <p>
   * - Nested objects are recursively traversed based on the DTO class's structure. - Arrays are
   * flattened with index-based keys. - Non-translatable fields and nulls are ignored.
   *
   * @param jsonNode    The JSON structure (usually a form section) to extract translatable entries
   *                    from.
   * @param dtoClass    The DTO class describing the expected structure and annotations of the input
   *                    JSON.
   * @param sourceLang  The source language code (e.g., "IT").
   * @param targetLangs A list of target language codes to include (e.g., ["EN", "FR"]).
   * @param pathPrefix  A string prefix used to track the current path when recursing (nullable for
   *                    root).
   * @return A flattened JsonObject containing translation entries for each translatable field.
   */
  private JsonObject buildFlatTranslatableEntries(JsonObject jsonNode,
    Class<?> dtoClass,
    String sourceLang,
    List<String> targetLangs,
    String pathPrefix) {
    JsonObject result = new JsonObject();

    for (Field field : dtoClass.getDeclaredFields()) {
      field.setAccessible(true);

      String jsonFieldName = field.getName();
      JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
      if (jsonProp != null && jsonProp.value() != null && !jsonProp.value().isEmpty()) {
        jsonFieldName = jsonProp.value();
      }

      Object fieldValue = jsonNode.getValue(jsonFieldName);
      if (fieldValue == null) {
        continue;
      }

      // Construct key with optional prefix
      String dictKey = (pathPrefix != null) ? pathPrefix + "." + jsonFieldName : jsonFieldName;

      // Direct translatable string
      if (field.isAnnotationPresent(TranslatableField.class) && fieldValue instanceof String strVal
        && !strVal.isBlank()) {
        result.put(dictKey, new JsonObject()
          .put("value", strVal)
          .put("sourceLanguage", sourceLang)
          .put("targetLanguages", new JsonArray(targetLangs)));
      }

      // Nested object
      else if (fieldValue instanceof JsonObject childObj) {
        JsonObject childTranslations = buildFlatTranslatableEntries(
          childObj, field.getType(), sourceLang, targetLangs, dictKey
        );
        result.mergeIn(childTranslations);
      }

      // Nested array
      else if (fieldValue instanceof JsonArray array) {
        Class<?> elementType = getListGenericType(field);
        if (elementType == null) {
          continue;
        }

        for (int i = 0; i < array.size(); i++) {
          Object element = array.getValue(i);
          String elementPrefix = dictKey + "." + i;

          if (element instanceof JsonObject elemObj) {
            JsonObject elemTranslations = buildFlatTranslatableEntries(
              elemObj, elementType, sourceLang, targetLangs, elementPrefix
            );
            result.mergeIn(elemTranslations);
          } else if (element instanceof String strElem && field.isAnnotationPresent(
            TranslatableField.class)) {
            result.put(elementPrefix, new JsonObject()
              .put("value", strElem)
              .put("sourceLanguage", sourceLang)
              .put("targetLanguages", new JsonArray(targetLangs)));
          }
        }
      }
    }

    return result;
  }

  /**
   * Utility to find generic List element type of a field, if it is a List.
   */
  private Class<?> getListGenericType(Field field) {
    Type genericType = field.getGenericType();
    if (genericType instanceof ParameterizedType pType) {
      Type[] typeArgs = pType.getActualTypeArguments();
      if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> clazz) {
        return clazz;
      }
    }
    return null;
  }

  /**
   * Builds a map of translated forms for each target language using the original form and
   * translation dictionary.
   *
   * <p>This method takes the user-submitted {@code originalForm} and produces a translated copy
   * for each target language found in the {@code translationDict}. The resulting translated forms
   * preserve the same structure as the original, but with:
   *
   * <ul>
   *   <li>Field values replaced with translations from the dictionary (where available)</li>
   *   <li>Static option fields (e.g. yes/no enums) localized via DTO-based dictionary injection</li>
   *   <li>File references swapped for translated file metadata when available</li>
   * </ul>
   *
   * <p>The method ensures that only known application types are supported and that translations
   * are applied consistently across sections, arrays, and nested objects.
   *
   * @param originalForm    The original form submitted by the user, before any translation.
   * @param translationDict A dictionary containing translations for specific fields, structured by
   *                        section and path.
   * @param translatedFiles A map of original file IDs to their translated file metadata per
   *                        language.
   * @param applicationType The type of application (used to resolve the correct section DTO
   *                        mappings).
   * @return A JsonObject mapping each target language code (e.g., {@code "EN"}) to its fully
   * translated form.
   * @throws IllegalArgumentException if the {@code applicationType} is unknown.
   */
  public JsonObject buildFormTranslations(
    JsonObject originalForm,
    JsonObject translationDict,
    JsonObject translatedFiles,
    String applicationType
  ) {
    JsonObject formTranslations = new JsonObject();
    Set<String> targetLanguages = new HashSet<>();

    // Collect all target languages from the dictionary
    for (String section : translationDict.fieldNames()) {
      JsonObject fields = translationDict.getJsonObject(section);
      for (String dictKey : fields.fieldNames()) {
        JsonObject fieldData = fields.getJsonObject(dictKey);
        JsonObject translations = fieldData.getJsonObject("translations");
        if (translations != null) {
          targetLanguages.addAll(translations.fieldNames());
        }
      }
    }

    // Build one translated form per language
    for (String lang : targetLanguages) {
      JsonObject translatedForm = originalForm.copy();

      // Apply dictionary-based translations
      for (String section : translationDict.fieldNames()) {
        JsonObject sectionDict = translationDict.getJsonObject(section);
        Object sectionNode = translatedForm.getValue(section);
        if (sectionNode == null) {
          continue;
        }

        for (String path : sectionDict.fieldNames()) {
          JsonObject fieldData = sectionDict.getJsonObject(path);
          JsonObject translations = fieldData.getJsonObject("translations");
          if (translations == null || !translations.containsKey(lang)) {
            continue;
          }

          String translatedValue = translations.getString(lang);
          applyTranslationAtPath(sectionNode, path, translatedValue);
        }

        translatedForm.put(section, sectionNode);
      }

      // Inject static option labels (e.g. Yes/No, enums) using DTO metadata
      Map<String, List<Class<?>>> sectionDtoMap;
      switch (applicationType) {
        case DATA_REQUEST_APPLICATION_COLLECTION -> sectionDtoMap = SECTION_DTO_MAP_REQUEST;
        case DATA_ACCESS_APPLICATION_COLLECTION -> sectionDtoMap = SECTION_DTO_MAP_ACCESS;
        default -> {
          log.warn("Unknown application type when building Form translations: {}", applicationType);
          throw new IllegalArgumentException("Unsupported application type: " + applicationType);
        }
      }
      for (String section : translatedForm.fieldNames()) {
        List<Class<?>> dtoClasses = sectionDtoMap.get(section);
        Object sectionNode = translatedForm.getValue(section);
        if (sectionNode == null) {
          continue;
        }
        if (sectionNode instanceof JsonObject || sectionNode instanceof JsonArray) {
          for (Class<?> dtoClass : dtoClasses) {
            injectStaticOptionLabels(sectionNode, lang.toLowerCase(Locale.ROOT), section,
              dtoClass);
          }
        }
      }

      // Replace file references with translated file metadata
      if (translatedFiles != null) {
        for (String originalFileId : translatedFiles.fieldNames()) {
          JsonObject fileEntry = translatedFiles.getJsonObject(originalFileId);
          if (!TranslationStatus.COMPLETED.name().equals(fileEntry.getString("status"))) {
            continue;
          }

          JsonObject translatedPerLang = fileEntry.getJsonObject("translatedFiles");
          if (translatedPerLang != null && translatedPerLang.containsKey(lang)) {
            JsonObject translatedFile = translatedPerLang.getJsonObject(lang);
            String newFileId = translatedFile.getString("fileId");
            String newFileName = translatedFile.getString("fileName");

            ApplicationFormUtils.updateTranslatedFileReferencesInForm(
              translatedForm, originalFileId, newFileId, newFileName
            );
          }
        }
      }

      formTranslations.put(lang, translatedForm);
    }

    return formTranslations;
  }

  /**
   * Applies a translated string to a nested field path inside a given section node.
   * <p>
   * This method navigates through a structure of nested JsonObjects and JsonArrays based on the
   * dot-delimited `path` (e.g., "0.tabulationPlanArray.1.tabulationFormationVariables") and sets
   * the translated value at the final key.
   *
   * @param sectionNode     The root object of a section (could be a JsonObject or JsonArray).
   * @param path            A dot-separated path indicating where the translated value should be
   *                        placed.
   * @param translatedValue The translated string to insert into the form structure.
   */
  private static void applyTranslationAtPath(Object sectionNode, String path,
    String translatedValue) {
    String[] parts = path.split("\\.");
    Object current = sectionNode;

    for (int i = 0; i < parts.length - 1; i++) {
      String part = parts[i];

      if (current instanceof JsonArray arr && isInteger(part)) {
        int index = Integer.parseInt(part);
        if (index >= arr.size()) {
          return;
        }
        current = arr.getValue(index);
      } else if (current instanceof JsonObject obj) {
        current = obj.getValue(part);
      } else {
        return;
      }
    }

    String finalKey = parts[parts.length - 1];
    if (current instanceof JsonObject target) {
      target.put(finalKey, translatedValue);
    }
  }

  private static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Represents a reference to a predefined set of enum options within a specific form section.
   *
   * <p>This is resolved from the {@link CheckEnumValue} annotation on DTO fields and is later
   * used to inject localized option labels into translated forms.
   *
   * @param section   the logical section name (from {@code @CheckEnumValue.sectionNumber()}), e.g.
   *                  "commonFields"
   * @param fieldName the enum option field name (from {@code @CheckEnumValue.fieldName()}), e.g.
   *                  "yesOrNoOptions"
   */
  private record EnumRef(String section, String fieldName) {

  }

  /**
   * Builds (and caches) a mapping of JSON field names to their associated enum reference metadata,
   * based on {@link CheckEnumValue} annotations declared in the given DTO class.
   *
   * <p>The mapping allows lookup of which enum option set a particular form field belongs to,
   * so that localized labels can later be injected (e.g., Yes/No options, predefined categories).
   * The result is cached per DTO class in {@code ENUM_MAP_CACHE} to avoid repeated reflection.
   *
   * <p>Field name resolution prioritizes the Jackson {@link JsonProperty} value (if present),
   * otherwise falling back to the raw Java field name.
   *
   * @param dtoClass the DTO class to scan for {@link CheckEnumValue} annotations
   * @return a map of JSON field name → {@link EnumRef}, where {@link EnumRef} contains:
   * <ul>
   *   <li>{@code section}: the section key (from the annotation's {@code sectionNumber})</li>
   *   <li>{@code fieldName}: the logical enum field name (from the annotation's {@code fieldName})</li>
   * </ul>
   * Returns an empty map if {@code dtoClass} is {@code null} or contains no annotated fields.
   */
  private static Map<String, EnumRef> buildEnumMappings(Class<?> dtoClass) {
    if (dtoClass == null) {
      return Collections.emptyMap();
    }
    return ENUM_MAP_CACHE.computeIfAbsent(dtoClass, cls -> {
      Map<String, EnumRef> m = new HashMap<>();
      for (Field f : cls.getDeclaredFields()) {
        CheckEnumValue ann = f.getAnnotation(CheckEnumValue.class);
        if (ann == null) {
          continue;
        }

        // Resolve JSON field name (prefer @JsonProperty over raw Java field name)
        String jsonFieldName = f.getName();
        JsonProperty jsonProp = f.getAnnotation(JsonProperty.class);
        if (jsonProp != null && jsonProp.value() != null && !jsonProp.value().isEmpty()) {
          jsonFieldName = jsonProp.value();
        }

        m.put(jsonFieldName, new EnumRef(ann.sectionNumber(), ann.fieldName()));
      }
      return m;
    });
  }

  /**
   * Resolves the translated option items for a given field in the form.
   *
   * <p>The lookup follows a two-step strategy:
   * <ol>
   *   <li><b>Section-specific lookup:</b> Attempts to find translated items directly under
   *       the given section's dictionary (e.g. {@code section3.applyingForDataOnBehalfOfPublicSector.items}).</li>
   *   <li><b>Fallback via @CheckEnumValue:</b> If no section-specific items are found, consults the
   *       {@link EnumRef} mapping built from {@code @CheckEnumValue} annotations. This allows the method
   *       to fall back to shared/static enumerations, such as {@code commonFields.yesOrNoOptions.items}.</li>
   * </ol>
   *
   * @param lang                      the target language code (e.g. "en", "el")
   * @param fieldName                 the JSON field name being translated
   * @param staticSectionTranslations the dictionary for the current section in the given language
   * @param enumMap                   mapping of field names to {@link EnumRef} from the DTO class
   * @return the translated items array, or {@code null} if no translation is available
   */
  private JsonArray findTranslatedOptionItems(
    String lang,
    String fieldName,
    JsonObject staticSectionTranslations,
    Map<String, EnumRef> enumMap
  ) {
    // 1) Section-specific: sectionN.fieldName.items
    JsonArray items = staticSectionTranslations
      .getJsonObject(fieldName, new JsonObject())
      .getJsonArray("items");
    if (items != null) {
      return items;
    }

    // 2) Fallback via @CheckEnumValue (e.g., commonFields.yesOrNoOptions.items)
    EnumRef ref = enumMap.get(fieldName);
    if (ref == null) {
      // fallback: check if any EnumRef.fieldName matches the JSON fieldName
      ref = enumMap.values().stream()
        .filter(r -> r.fieldName.equals(fieldName))
        .findFirst()
        .orElse(null);
    }

    if (ref != null) {
      JsonObject refDict = translationManager.getTranslation(lang, ref.section);
      if (refDict != null) {
        return refDict
          .getJsonObject(ref.fieldName, new JsonObject())
          .getJsonArray("items");
      }
    }
    // DEBUG log to show which field cannot be mapped
    return null;
  }

  /**
   * Recursively injects translated static option labels into a given section node of a form, based
   * on the provided language, section name, and DTO class.
   *
   * <p>This method traverses the provided {@code sectionNode}, which may be a
   * {@link io.vertx.core.json.JsonObject} or {@link io.vertx.core.json.JsonArray}, and replaces the
   * "value" (and optionally "info") fields of option objects with their corresponding translations.
   * Translations are resolved from:</p>
   *
   * <ol>
   *   <li><b>Section-specific dictionary entries</b> (e.g. {@code section3.someField.items}).</li>
   *   <li><b>DTO-driven enum fallbacks</b> via {@link CheckEnumValue}, which map the field to
   *       another dictionary section/field (e.g. {@code commonFields.yesOrNoOptions.items}).</li>
   * </ol>
   *
   * <p>Three structures are supported:</p>
   * <ul>
   *   <li><b>Case 1:</b> A single option object with {@code key}/{@code value}.</li>
   *   <li><b>Case 2:</b> An array of option objects with {@code key} fields.</li>
   *   <li><b>Case 3:</b> Nested objects/arrays, which are processed recursively.</li>
   * </ul>
   *
   * @param sectionNode the current section node to process; may be a {@code JsonObject} or
   *                    {@code JsonArray}.
   * @param lang        the language code (e.g., "en", "fr") for which translations should be
   *                    applied.
   * @param sectionName the form section identifier used to retrieve the correct translation
   *                    dictionary.
   * @param dtoClass    the List of DTO classes used to build enum mappings for fallback
   *                    resolution.
   */
  public void injectStaticOptionLabels(Object sectionNode, String lang, String sectionName,
    Class<?> dtoClass) {
    if (sectionNode == null) {
      return;
    }

    JsonObject staticSectionTranslations = translationManager.getTranslation(lang, sectionName);
    if (staticSectionTranslations == null) {
      return;
    }

    // Build enum mapping once per call
    Map<String, EnumRef> enumMap = buildEnumMappings(dtoClass);

    if (sectionNode instanceof JsonObject section) {
      // Iterate each field in the object
      for (String fieldName : section.fieldNames()) {
        Object value = section.getValue(fieldName);

        // Case 1: Single option object with key/value
        if (value instanceof JsonObject optionObj &&
          optionObj.containsKey("key") && optionObj.containsKey("value")) {

          JsonArray translatedItems = findTranslatedOptionItems(
            lang, fieldName, staticSectionTranslations, enumMap);

          if (translatedItems != null) {
            String key = optionObj.getString("key");
            for (int i = 0; i < translatedItems.size(); i++) {
              JsonObject translatedItem = translatedItems.getJsonObject(i);
              if (key != null && key.equals(translatedItem.getString("key"))) {
                optionObj.put("value", translatedItem.getString("value"));
                if (translatedItem.containsKey("info")) {
                  optionObj.put("info", translatedItem.getString("info"));
                }
                break;
              }
            }
          }
        }

        // Case 2: Array of option objects
        else if (value instanceof JsonArray array &&
          !array.isEmpty() &&
          array.getValue(0) instanceof JsonObject) {

          JsonArray translatedItems = findTranslatedOptionItems(
            lang, fieldName, staticSectionTranslations, enumMap);

          if (translatedItems != null) {
            for (int i = 0; i < array.size(); i++) {
              JsonObject item = array.getJsonObject(i);
              if (!item.containsKey("key")) {
                continue;
              }

              String key = item.getString("key");

              for (int j = 0; j < translatedItems.size(); j++) {
                JsonObject translatedItem = translatedItems.getJsonObject(j);
                if (key != null && key.equals(translatedItem.getString("key"))) {
                  item.put("value", translatedItem.getString("value"));
                  if (translatedItem.containsKey("info")) {
                    item.put("info", translatedItem.getString("info"));
                  }
                  break;
                }
              }
            }
          }
        }

        // Case 3: Recurse for nested object/array
        else if (value instanceof JsonObject || value instanceof JsonArray) {
          injectStaticOptionLabels(value, lang, sectionName, dtoClass);
        }
      }
    } else if (sectionNode instanceof JsonArray array) {
      for (int i = 0; i < array.size(); i++) {
        Object item = array.getValue(i);
        if (item instanceof JsonObject || item instanceof JsonArray) {
          injectStaticOptionLabels(item, lang, sectionName, dtoClass);
        }
      }
    }
  }

  /**
   * Builds a payload to request translation of a single file via the hub-translation service.
   * <p>
   * Prepares a structured JSON object that includes: - Base64-encoded content of the file - Format
   * and filename metadata - Source and target languages, Source language is set to "LD" which is
   * used for auto-detect - Callback configuration for receiving the translated result
   * <p>
   * The resulting payload matches the expected structure for file translation requests as defined
   * by the hub-translation API, including nested fields under `dict.files.file`.
   *
   * @param applicationId   ID of the application the file is associated with.
   * @param applicationType Type of the application (e.g., "DATA_REQUEST_APPLICATION").
   * @param fileObj         JSON object representing the file, including keys: {@code fileId},
   *                        {@code fileName}, {@code format}, and {@code base64Content}.
   * @param targetLanguages List of target language codes for translation.
   * @return JsonObject formatted as a file translation request payload for hub-translation.
   */
  public JsonObject buildFileTranslationPayload(
    String applicationId,
    String applicationType,
    JsonObject fileObj,
    List<String> targetLanguages
  ) {
    final String fileId = fileObj.getString("fileId");
    final String fileName = fileObj.getString("fileName");
    final String format = fileObj.getString("format", "application/pdf");
    final String base64Content = fileObj.getString("base64Content");
    final String autoDetectLang = "LD";

    JsonObject documentToTranslate = new JsonObject()
      .put("content", base64Content)
      .put("format", format)
      .put("fileName", fileName);

    JsonObject file = new JsonObject()
      .put("documentToTranslateBase64", documentToTranslate)
      .put("sourceLanguage", autoDetectLang)
      .put("targetLanguages", targetLanguages);

    // Two levels of nesting are expected from hub-translation
    JsonObject dict = new JsonObject()
      .put("file", new JsonObject()
        .put(fileId, file));

    JsonObject callback = createCallbackJson();

    JsonObject data = new JsonObject()
      .put("applicationId", applicationId)
      .put("applicationType", applicationType)
      .put("defaultLanguage", autoDetectLang)
      .put("targetLanguages", targetLanguages);

    return new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("data", data)
      .put("dict", dict)
      .put("callback", callback);
  }

  /**
   * Sends an HTTP POST request with the given translation payload to the specified URL.
   * <p>
   * Converts the JSON payload to a buffer, sets appropriate headers, and sends the request
   * asynchronously using Vert.x WebClient. Resolves the returned {@code Future<Void>} based on the
   * success or failure of the HTTP response.
   * <p>
   * On success (HTTP 200 OK or 204 No Content), the future is completed. On failure (non-2xx status
   * or network error), the future is failed with a JSON-encoded error object containing
   * {@code errorCode} and {@code errorDetails}.
   *
   * @param url         Absolute URL to which the translation request should be sent.
   * @param payload     JSON payload to send in the request body.
   * @param requestType A descriptive label used for logging (e.g., "FormTranslation",
   *                    "FileTranslation").
   * @return A {@code Future<Void>} that completes when the request is successfully sent and
   * acknowledged, or fails if an error occurs during transmission or response handling.
   */
  private Future<Void> sendTranslationRequest(String url, JsonObject payload, String requestType) {
    Promise<Void> promise = Promise.promise();

    Buffer buffer = payload.toBuffer();
    MultiMap headers = createHeader(buffer);

    vertxClient.postAbs(url)
      .putHeaders(headers)
      .sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          int statusCode = ar.result().statusCode();
          if (statusCode == HttpResponseStatus.OK.code()
            || statusCode == HttpResponseStatus.NO_CONTENT.code()) {
            log.info("{} Successfully requested {} with status: {}", HUB_TRANSLATION_CLIENT,
              requestType, statusCode);
            promise.complete();
          } else {
            log.error("{} {} request failed with status: {}, message: {}",
              HUB_TRANSLATION_CLIENT, requestType, statusCode, ar.result().bodyAsString());
            promise.fail(new JsonObject()
              .put(ERROR_CODE, statusCode)
              .put(ERROR_DETAILS, ar.result().bodyAsString()).encode());
          }
        } else {
          log.error("{} Failed to request {}: {}", HUB_TRANSLATION_CLIENT, requestType,
            ar.cause().getMessage());
          promise.fail(new JsonObject()
            .put(ERROR_CODE, "request_failure")
            .put(ERROR_DETAILS, ar.cause().getMessage()).encode());
        }
      });

    return promise.future();
  }

  public Future<Void> requestTranslation(JsonObject translationRequestPayload) {
    return sendTranslationRequest(translationUrl, translationRequestPayload, "form translation");
  }

  public Future<Void> requestFileTranslation(JsonObject fileTranslationPayload) {
    return sendTranslationRequest(translationFileUrl, fileTranslationPayload, "file translation");
  }

}
