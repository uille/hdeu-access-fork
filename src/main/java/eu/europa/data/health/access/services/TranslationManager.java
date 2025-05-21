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

import static eu.europa.data.health.access.Constants.JSON;
import static eu.europa.data.health.access.Constants.JSON_FILE_FOR;
import static eu.europa.data.health.access.Constants.NOT_FOUND;
import static eu.europa.data.health.access.Constants.TRANSLATIONS;
import static eu.europa.data.health.access.Constants.TRIPLE_LOG_ABBREVIATION;

import eu.europa.data.health.access.Translations;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.jena.iri.impl.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class TranslationManager {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Method where you give the languish and the section of the form, and you retrieve the
   * translations for this section
   */
  public void getTranslationForSection(RoutingContext context) {
    //to languish that we need return the translations
    String lg = context.pathParam("lg");

    //which query we need to create
    String sectionParam = context.pathParam("section");
    context.response().putHeader("content-type", "application/json");

    JsonObject jsonObject;

    try (InputStream inputStream = getTranslationFileStream(lg)) {
      if (inputStream == null) {
        log.info(TRIPLE_LOG_ABBREVIATION, JSON_FILE_FOR, lg, NOT_FOUND,
          " Will assign default language: ", Translations.getDefault());
        lg = Translations.getDefault();

        try (InputStream defaultStream = getTranslationFileStream(lg)) {
          if (defaultStream == null) {
            log.error("Default translation file for language '{}' not found.", lg);
            context.response().setStatusCode(500)
              .end("Translation file for default language: " + lg + NOT_FOUND);
            return;
          }
          jsonObject = loadJsonFromStream(defaultStream);
        }
      } else {
        jsonObject = loadJsonFromStream(inputStream);
      }
    } catch (IOException e) {
      log.error("Error reading translation file for language '{}': {}", lg, e.getMessage(), e);
      context.response().setStatusCode(500).end("Error loading translation file.");
      return;
    }

    // Handle case where JSON file is empty or invalid
    if (Objects.isNull(jsonObject)) {
      context.response().setStatusCode(404).end("Translation file is empty.");
      return;
    }

    JsonObject section = jsonObject.getJsonObject(sectionParam);
    //check for wrong value of sections
    if (Objects.isNull(section)) {
      context.response().setStatusCode(404).end("Section not found for: " + sectionParam);
      return;
    }
    //include also the common field in every section
    JsonObject commonField = jsonObject.getJsonObject("commonFields");
    JsonObject combinedSection = new JsonObject();
    combinedSection.mergeIn(section);
    combinedSection.mergeIn(commonField);
    context.response().setStatusCode(200).end(combinedSection.encode());
  }

   InputStream getTranslationFileStream(String language) {
    return Main.class.getResourceAsStream(TRANSLATIONS + language + JSON);
  }

  /**
   * Get section translation
   */
  public JsonObject getTranslation(String lg, String sectionNumber) {
    try (InputStream inputStream = Main.class.getResourceAsStream(
      TRANSLATIONS + lg + JSON)) {
      if (inputStream == null) {
        log.info(TRIPLE_LOG_ABBREVIATION, JSON_FILE_FOR, lg, NOT_FOUND);
        return null;
      }
      JsonObject jsonObject = loadJsonFromStream(inputStream);
      if (Objects.nonNull(jsonObject)) {
        return jsonObject.getJsonObject(sectionNumber);
      } else {
        log.info("Translation json file is empty.");
      }
    } catch (IOException e) {
      log.info(JSON_FILE_FOR + lg + NOT_FOUND, e);
    }
    return null;
  }

  /**
   * Load json from an input stream
   *
   * @return JsonObject
   */
  public JsonObject loadJsonFromStream(InputStream inputStream) {
    if (Objects.isNull(inputStream)) {
      return null;
    }
    try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
      String content = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : null;
      return (content == null || content.isBlank()) ? null : new JsonObject(content);
    } catch (Exception e) {
      log.error("Failed to load JSON from stream", e);
      return null;
    }
  }

  /**
   * Get all translations for specific languish
   */
  public JsonObject getTranslationByLanguish(String lg) throws IOException {
    try (InputStream inputStream = Main.class.getResourceAsStream(TRANSLATIONS + lg + JSON)) {
      if (inputStream == null) {
        log.info(TRIPLE_LOG_ABBREVIATION, JSON_FILE_FOR, lg, NOT_FOUND);
      }
      return loadJsonFromStream(inputStream);
    }
  }

}
