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

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import org.apache.jena.iri.impl.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationManager {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Method where you give the languish and the section of the form, and you retrieve the
   * translations for this section
   */
  public void getTranslationForSection(RoutingContext context) {
    //to languish that we need return the tranlsations
    String lg = context.pathParam("lg");
    //which query we need to create
    String sectionParam = context.pathParam("section");
    context.response().putHeader("content-type", "application/json");
    try (InputStream inputStream = Main.class.getResourceAsStream(
      TRANSLATIONS + lg + JSON)) {
      if (inputStream == null) {
        log.info(TRIPLE_LOG_ABBREVIATION, JSON_FILE_FOR, lg, NOT_FOUND);
        context.response().setStatusCode(404).end("Translation file for languish: " + lg + NOT_FOUND);
        return;
      }
      JsonObject jsonObject = loadJsonFromStream(inputStream);
      if (Objects.nonNull(jsonObject)) {
        JsonObject section = jsonObject.getJsonObject(sectionParam);
        //check for wrong value of sections
        if(Objects.isNull(section)){
          context.response().setStatusCode(404).end("Not section found: " + sectionParam);
          return;
        }
        //include also the common field in every section
        JsonObject commonField = jsonObject.getJsonObject("commonFields");
        JsonObject combinedSection = new JsonObject();
        combinedSection.mergeIn(section);
        combinedSection.mergeIn(commonField);
        context.response().setStatusCode(200).end(combinedSection.encode());
      } else {
        context.response().setStatusCode(404).end("Translation file is empty.");
      }
    } catch (IOException e) {
      log.info(JSON_FILE_FOR + lg + NOT_FOUND, e);
      context.response().setStatusCode(500).end(e.getMessage());
    }
  }

  /**
   * Get section translation
   */
  public JsonObject getTranslation(String lg, String sectionNumber){
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
