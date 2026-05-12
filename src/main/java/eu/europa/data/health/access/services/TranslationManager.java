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

import static eu.europa.data.health.access.Constants.APPLICATION_COLLABORATOR_COLLECTION;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.COLLABORATOR_USER_ID;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.INVITATION_ACCEPTED;
import static eu.europa.data.health.access.Constants.INVITATION_STATUS;
import static eu.europa.data.health.access.Constants.JSON;
import static eu.europa.data.health.access.Constants.JSON_FILE_FOR;
import static eu.europa.data.health.access.Constants.NOT_FOUND;
import static eu.europa.data.health.access.Constants.TRANSLATIONS;
import static eu.europa.data.health.access.Constants.TRIPLE_LOG_ABBREVIATION;
import static eu.europa.data.health.access.Constants.USER_ID_PARAM;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.Translations;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import org.apache.jena.iri.impl.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationManager {

  private final DBHandler dbHandler;

  /**
   * Optional MongoClient used to verify collaborator access in
   * {@link #getApplicationTranslation(RoutingContext)}.
   * May be {@code null} when constructed without it (e.g. in validation helpers).
   */
  private final MongoClient mongoClient;

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Full constructor — preferred in production.
   * Enables the collaborator-aware translation lookup.
   */
  public TranslationManager(DBHandler dbHandler, MongoClient mongoClient) {
    this.dbHandler = dbHandler;
    this.mongoClient = mongoClient;
  }

  /** Legacy constructor kept for backward compatibility (no collaborator check). */
  public TranslationManager(DBHandler dbHandler) {
    this.dbHandler = dbHandler;
    this.mongoClient = null;
  }

  /** No-arg constructor used by validation helpers that only need static translation files. */
  public TranslationManager() {
    this.dbHandler = null;
    this.mongoClient = null;
  }

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

  /**
   * Returns the {@code translation} sub-document of an application identified by
   * {@code applicationId}.
   *
   * <p><strong>Collaborator fix:</strong> the original implementation queried by
   * {@code {_id, userId}}, which always produced a 404 for collaborators because the
   * stored {@code userId} is the <em>owner's</em> ID.  The corrected flow is:
   * <ol>
   *   <li>If a {@link MongoClient} is available, look up the caller in
   *       {@code APPLICATION_COLLABORATOR_COLLECTION} to check whether they hold an
   *       ACCEPTED invitation for this application.</li>
   *   <li>If they are a collaborator the query is issued by {@code _id} only (no
   *       {@code userId} filter).</li>
   *   <li>If they are not a collaborator (i.e. they must be the owner) the original
   *       {@code {_id, userId}} query is used so that unauthorised callers still
   *       receive 404.</li>
   *   <li>When no {@link MongoClient} is available (legacy constructor) the method
   *       falls back to the owner-only query.</li>
   * </ol>
   */
  public void getApplicationTranslation(RoutingContext context) {
    String applicationId = context.pathParam("applicationId");
    String ecasId        = Context.getEcasId(context);

    if (mongoClient == null) {
      // No MongoClient injected — fall back to owner-only lookup (legacy behaviour).
      doTranslationLookup(applicationId, ecasId, true, context);
      return;
    }

    // Check whether the caller is an accepted collaborator for this specific application.
    JsonObject collabFilter = new JsonObject()
        .put(APPLICATION_ID,       applicationId)
        .put(COLLABORATOR_USER_ID, ecasId)
        .put(INVITATION_STATUS,    INVITATION_ACCEPTED);

    mongoClient.findOne(APPLICATION_COLLABORATOR_COLLECTION, collabFilter, null)
        .onSuccess(collabDoc -> {
          // collabDoc != null  → accepted collaborator  → skip userId filter
          // collabDoc == null  → not a collaborator      → apply userId filter (owner check)
          boolean requireOwner = (collabDoc == null);
          doTranslationLookup(applicationId, ecasId, requireOwner, context);
        })
        .onFailure(err -> {
          log.error("Failed to check collaborator status for translation lookup "
              + "(applicationId='{}', ecasId='{}'): {}", applicationId, ecasId, err.getMessage(), err);
          // On unexpected DB error fall back to the safe owner-only path.
          doTranslationLookup(applicationId, ecasId, true, context);
        });
  }

  /**
   * Performs the actual application lookup and returns the translation sub-document.
   *
   * @param applicationId the application to look up
   * @param ecasId        the caller's ECAS ID
   * @param requireOwner  when {@code true} adds {@code userId = ecasId} to the query so
   *                      only the owner can read the document; when {@code false} the
   *                      query uses only {@code _id} (collaborators are already validated
   *                      by the caller).
   * @param context       the Vert.x routing context used to write the HTTP response
   */
  private void doTranslationLookup(String applicationId, String ecasId,
      boolean requireOwner, RoutingContext context) {

    JsonObject query = new JsonObject().put("_id", applicationId);
    if (requireOwner) {
      query.put(USER_ID_PARAM, ecasId);
    }

    assert dbHandler != null;
    dbHandler.findByField(DATA_ACCESS_APPLICATION_COLLECTION, query)
      .onSuccess(accessJsonObject -> returnTranslation(accessJsonObject, context))
      .onFailure(errorAccess -> {
        int accessErrorCode = ((ServiceException) errorAccess).failureCode();
        if (404 == accessErrorCode) {
          dbHandler.findByField(DATA_REQUEST_APPLICATION_COLLECTION, query)
            .onSuccess(requestJsonObject -> returnTranslation(requestJsonObject, context))
            .onFailure(errorRequest -> {
              int requestErrorCode = ((ServiceException) errorRequest).failureCode();
              if (404 == requestErrorCode) {
                context.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                  .end("Application not found");
              } else {
                context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                  .end(errorAccess.getMessage());
              }
            });
        } else {
          context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .end(errorAccess.getMessage());
        }
      });
  }

  private void returnTranslation(JsonObject applicationObject, @NotNull RoutingContext context){
    JsonObject translation = new JsonObject();
    if (Objects.nonNull(applicationObject.getJsonObject("translation"))) {
      translation = applicationObject.getJsonObject("translation");
      context.response().setStatusCode(HttpResponseStatus.OK.code()).end(translation.encode());
    } else {
      translation.put("message", "Translation not found");
      context.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code())
        .end(translation.encode());
    }
  }

}
