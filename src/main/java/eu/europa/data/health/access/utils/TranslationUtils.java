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

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class TranslationUtils {

  private TranslationUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Adds valid translations from the translations object into a multilingual object.
   *
   * @param multilingual The multilingual JsonObject to update (mutated in place).
   * @param translations The translations JsonObject containing lang -> text mappings.
   * @return True if at least one translation was added, false otherwise.
   */
  public static boolean addTranslations(JsonObject multilingual, JsonObject translations) {
    boolean added = false;
    for (String lang : translations.fieldNames()) {
      Object raw = translations.getValue(lang);
      if (raw == null || isNumericErrorCode(raw)) {
        continue;
      }

      if (raw instanceof String str && StringUtils.isNotBlank(str)) {
        multilingual.put(lang, str);
        added = true;
      }
    }
    return added;
  }

  /**
   * Evaluates if the given raw value is a numeric error code (Number or String representing an
   * integer, possibly negative). Such values should not be added as translations.
   *
   * @param raw The value to check.
   * @return True if the value is a numeric error code, false otherwise.
   */
  public static boolean isNumericErrorCode(Object raw) {
    if (raw instanceof Number) {
      return true;
    }
    if (raw instanceof String str) {
      String trimmed = str.trim();
      if (trimmed.startsWith("-")) {
        trimmed = trimmed.substring(1);
      }
      return StringUtils.isNumeric(trimmed); // digits-only after optional leading '-'
    }
    return false;
  }

}
