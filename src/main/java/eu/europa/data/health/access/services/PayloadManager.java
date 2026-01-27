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

import static eu.europa.data.health.access.Constants.CONFIGURATION_COLLECTION;
import static eu.europa.data.health.access.Constants.NAME;
import static eu.europa.data.health.access.Constants.SUPPORTED_LANGUAGES;

import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadManager extends AbstractManager{

  private static final String PAYLOAD = "payload";
  private static final String SCHEMA_DOC_NAME = "health-dcat-ap-schema";
  private static final String SCHEMA_RESOURCE_CLASSPATH = "health-dcat-ap/health-dcat-ap-schema.json";
  private static final String SCHEMA_RESOURCE_FS_PATH = "src/main/resources/health-dcat-ap/health-dcat-ap-schema.json";
  private static final String SOURCE_LANG = "en";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;
  private final ControlledVocabularyManager controlledVocabularyManager;
  private final HubTranslationClient hubTranslationClient;
  private final SchemaTranslationService translationService;

  public PayloadManager(DBHandler dbHandler, MongoClient mongoClient,
    ControlledVocabularyManager controlledVocabularyManager,
    HubTranslationClient hubTranslationClient,
    SchemaTranslationService translationService) {
    super(dbHandler);
    this.mongoClient = mongoClient;
    this.controlledVocabularyManager = controlledVocabularyManager;
    this.hubTranslationClient = hubTranslationClient;
    this.translationService = translationService;
  }

  public void createOrUpdatePayload(@NotNull RoutingContext context){

    log.info("Entered createOrUpdatePayload");
    JsonObject body = context.body().asJsonObject();

    String name = body.getString(NAME);

    upsertByName(mongoClient, name, body, context);
  }

  public void upsertByName(MongoClient mongoClient, String name, JsonObject upsertData, @NotNull  RoutingContext context) {

    log.info("Entered upsertByName");
    JsonObject query = new JsonObject().put(NAME, name);

    // Return only the _id after update to keep the response light
    FindOptions findOptions = new FindOptions().setFields(new JsonObject().put("_id", 1));

    // Set upsert to true to insert if not found, and return the new document
    UpdateOptions updateOptions = new UpdateOptions().setUpsert(true).setReturningNewDocument(true);

    // Perform a simple upsert for all documents except the schema document
    if (!SCHEMA_DOC_NAME.equals(name)) {
      JsonObject upsert = new JsonObject().put("$set", upsertData);
      mongoClient.findOneAndUpdateWithOptions(
        CONFIGURATION_COLLECTION, query, upsert, findOptions, updateOptions,
        res -> handleUpsertResponse(context, res, name)
      );
      return;
    }

    // Find the existing document to compare old and new payloads and trigger the translation
    mongoClient.findOne(CONFIGURATION_COLLECTION, query, null, ar -> {
      JsonObject oldPayload;
      if (ar.succeeded()) {
        JsonObject prev = ar.result();
        oldPayload = prev != null ? prev.getJsonObject(PAYLOAD) : null;
      } else {
        oldPayload = null;
        log.warn("Could not load previous schema document for diff: {}", ar.cause().getMessage());
      }

      // Merge old translations into the new payload if the english text is the same and the
      // translations are missing
      JsonObject newPayload = upsertData.getJsonObject(PAYLOAD);
      if (newPayload != null && oldPayload != null) {
        translationService.mergeExistingTranslations(
          newPayload, oldPayload, SOURCE_LANG, SUPPORTED_LANGUAGES
        );
      }

      JsonObject upsert = new JsonObject().put("$set", upsertData);

      mongoClient.findOneAndUpdateWithOptions(CONFIGURATION_COLLECTION, query, upsert, findOptions,
        updateOptions, res -> {
          handleUpsertResponse(context, res, name);
          if (!res.succeeded()) {
            return;
          }
          if (newPayload == null) {
            log.warn("No payload found for doc '{}'; skipping translation.", name);
            return;
          }

          String id = res.result().getString("_id");
          triggerSchemaTranslation(name, newPayload, oldPayload, id);
        });
    });
  }

  /**
   * Trigger asynchronous translation of the Health DCAT-AP schema payload into supported languages.
   * The translation service will call back with the results, which we will then merge back into the
   * original document.
   *
   * @param name       The name of the configuration document (should be "health-dcat-ap-schema").
   * @param newPayload The new schema payload to translate.
   * @param oldPayload The previous schema payload to compare for changes (can be null).
   * @param id         The MongoDB document ID of the configuration document.
   */
  private void triggerSchemaTranslation(String name, JsonObject newPayload, JsonObject oldPayload,
    String id) {
    try {
      JsonObject dict = translationService.buildTranslatableDict(newPayload, oldPayload,
        SOURCE_LANG, SUPPORTED_LANGUAGES);

      if (dict.isEmpty()) {
        log.info("No new or changed text found in schema; skipping translation.");
        return;
      }

      // Prepare data block to help us find/update the right Mongo document when callback arrives
      JsonObject data = new JsonObject()
        .put("applicationId", id)
        .put("applicationType", CONFIGURATION_COLLECTION)
        .put("documentName", name);

      JsonObject request = new JsonObject()
        .put("id", CONFIGURATION_COLLECTION + "-" + UUID.randomUUID())
        .put("data", data)
        .put("dict", dict)
        .put("callback", hubTranslationClient.createCallbackJson());

      hubTranslationClient.requestTranslation(request)
        .onSuccess(v -> log.info("Health DCAT-AP schema translation triggered."))
        .onFailure(err -> log.error("Failed to trigger Health DCAT-AP schema translation", err));
    } catch (Exception e) {
      log.error("Error while building/sending Health DCAT-AP schema translation request", e);
    }
  }

  private void handleUpsertResponse(@NotNull RoutingContext context,
    AsyncResult<@Nullable JsonObject> response, String name) {
    if (response.succeeded()) {
      String id = response.result().getString("_id");
      log.info("Upsert on record with name: {} and _id: {} has been completed", name, id);
      setUpMessageContextResponse(context, HttpResponseStatus.OK,
        "Upsert on record with name: " + name + " has been completed");
    } else {
      log.error("Upsert on record with name: {} failed with error message: {}", name,
        response.cause().getMessage());
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "Upsert on record with name: " + name + " has failed");
    }
  }

  public void retrievePayload(@NotNull RoutingContext context){

    log.debug("Entered retrievePayload");

    String nameValue = context.request().getParam(NAME);

    findByFieldWithValue(CONFIGURATION_COLLECTION, NAME, nameValue)
      .onSuccess(
        jsonObject -> {
          jsonObject.remove("_id");
          setUpContextResponse(context, HttpResponseStatus.OK, jsonObject.encode());
        })
      .onFailure(cause -> setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
        "Payload with name: " + nameValue + " not found"));
  }

  public Future<JsonObject> findByFieldWithValue(String collectionName, String fieldName, String fieldValue) {

    log.debug("Entered findByFieldWithValue method");

    Promise<JsonObject> promise = Promise.promise();

    JsonObject query = new JsonObject().put(fieldName, fieldValue);

    mongoClient.findOne(collectionName, query, null)
      .onSuccess(result -> {
        boolean isSchema = CONFIGURATION_COLLECTION.equals(collectionName)
          && NAME.equals(fieldName)
          && SCHEMA_DOC_NAME.equals(fieldValue);
        if (result != null) {
          log.debug("Retrieved record from findByFieldWithValue method with field name: {} and value: {}", fieldName, fieldValue);
          if (!isSchema) {
            promise.complete(result);
            return;
          }

          // Update the schema with controlled vocabulary options before returning
          addOptionsToSelectInputs(result.copy())
            .onSuccess(promise::complete)
            .onFailure(err -> {
              log.error("Adding options to schema select inputs failed:", err);
              promise.complete(result); // Return the original if options addition fails
            });
          return;
        }

        if (!isSchema) {
          log.debug(
            "No record found from findByFieldWithValue method with field name: {} and value: {}",
            fieldName, fieldValue);
          promise.fail("Not found");
          return;
        }

        // Fallback for the health-dcat-ap-schema: Load from file and upsert
        JsonObject schemaDoc = loadSchemaFromResource();
        if (schemaDoc == null) {
          log.warn("Schema fallback load failed (resource not found or unreadable).");
          promise.fail("Not found");
          return;
        }

        // Update with controlled vocabulary options before returning
        addOptionsToSelectInputs(schemaDoc.copy())
          .onSuccess(promise::complete)
          .onFailure(err -> {
            log.error("Adding options to fallback schema select inputs failed :", err);
            promise.complete(schemaDoc); // Return the fallback if options addition fails
          });

        // Upsert in the background for future requests
        JsonObject upsert = new JsonObject().put("$set", schemaDoc);
        UpdateOptions options = new UpdateOptions().setUpsert(true);
        mongoClient.updateCollectionWithOptions(CONFIGURATION_COLLECTION, query, upsert, options,
          ar -> {
            if (ar.succeeded()) {
              log.info("Schema document '{}' was missing; loaded from file and saved into Mongo.",
                SCHEMA_DOC_NAME);
            } else {
              log.error("Failed to upsert schema document from file", ar.cause());
            }
          });
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Adds options to all the select inputs of the given schema document that specify a source by
   * fetching the relevant controlled vocabulary entries and injecting them into the schema.
   *
   * @param doc The schema document to process.
   * @return A future that completes with the updated document if successful, or the original
   * otherwise.
   */
  private Future<JsonObject> addOptionsToSelectInputs(JsonObject doc) {
    JsonObject payload = doc.getJsonObject(PAYLOAD);
    if (payload == null) {
      return Future.succeededFuture(doc);
    }

    JsonObject shapes = payload.getJsonObject("shapes");
    if (shapes == null || shapes.isEmpty()) {
      return Future.succeededFuture(doc);
    }

    // Collect unique sources across all select inputs
    Set<String> sources = new LinkedHashSet<>();
    for (String key : shapes.fieldNames()) {
      JsonObject shape = shapes.getJsonObject(key, new JsonObject());
      collectSourcesFromProps(shape.getJsonArray("properties"), sources);
    }
    if (sources.isEmpty()) {
      log.info("No controlled vocabulary sources found in schema.");
      return Future.succeededFuture(doc);
    }

    // Fetch options once per source. Use a list to preserve order for mapping later.
    List<String> sourceList = new ArrayList<>(sources);
    List<Future<JsonArray>> fetches = new ArrayList<>();
    for (String src : sourceList) {
      // Recover individual fetch failures to empty lists to avoid failing the entire operation
      fetches.add(
        controlledVocabularyManager.getBySource(src)
          .onFailure(e -> log.error("Failed to fetch controlled vocabulary for source {}:", src, e))
          .recover(e -> Future.succeededFuture(new JsonArray()))
      );
    }

    Promise<JsonObject> promise = Promise.promise();
    Future.all(fetches).onComplete(ar -> {
      // Build source -> options map
      Map<String, JsonArray> optionsBySource = new HashMap<>();
      for (int i = 0; i < sourceList.size(); i++) {
        JsonArray arr = ar.result().resultAt(i);
        optionsBySource.put(sourceList.get(i), arr != null ? arr : new JsonArray());
      }

      // Apply options to all select inputs
      for (String key : shapes.fieldNames()) {
        JsonObject shape = shapes.getJsonObject(key, new JsonObject());
        applyOptionsToProps(shape.getJsonArray("properties"), optionsBySource);
      }

      promise.complete(doc);
    });

    return promise.future();
  }

  /**
   * Recursively collects all unique controlled vocabulary sources from select inputs in the given
   * properties array.
   *
   * @param props   The properties array to scan.
   * @param sources The set to collect unique sources into.
   */
  private void collectSourcesFromProps(JsonArray props, Set<String> sources) {
    if (props == null) {
      return;
    }
    for (int i = 0; i < props.size(); i++) {
      JsonObject prop = props.getJsonObject(i);
      if (prop == null) {
        continue;
      }

      JsonObject input = prop.getJsonObject("input");
      if (input != null) {
        String type = input.getString("type");
        String source = input.getString("source");
        if ("select".equalsIgnoreCase(type) && StringUtils.isNotBlank(source)) {
          sources.add(source);
        }
      }

      // Recurse into nested properties
      collectSourcesFromProps(prop.getJsonArray("nestedProperties"), sources);
    }
  }

  /**
   * Recursively applies fetched options to select inputs in the given properties array based on
   * their source.
   *
   * @param props           The properties array to process.
   * @param optionsBySource A map of source identifiers to their corresponding options arrays.
   */
  private void applyOptionsToProps(JsonArray props, Map<String, JsonArray> optionsBySource) {
    if (props == null) {
      return;
    }
    for (int i = 0; i < props.size(); i++) {
      JsonObject prop = props.getJsonObject(i);
      if (prop == null) {
        continue;
      }

      JsonObject input = prop.getJsonObject("input");
      if (input != null) {
        String type = input.getString("type");
        String source = input.getString("source");
        if ("select".equalsIgnoreCase(type) && StringUtils.isNotBlank(source)) {
          JsonArray options = optionsBySource.get(source);
          if (options != null) {
            // overwrite with fetched options (copy to avoid shared mutation)
            input.put("options", options.copy());
          }
        }
      }

      // Recurse into nested properties
      applyOptionsToProps(prop.getJsonArray("nestedProperties"), optionsBySource);
    }
  }

  /**
   * Loads the Health DCAT-AP schema JSON from resources or filesystem.
   *
   * @return A document shaped as { "name": "health-dcat-ap-schema", "payload": { ... } }.
   */
  private JsonObject loadSchemaFromResource() {
    try {
      // Try classpath (works when packaged in a jar)
      try (InputStream is = getClass().getClassLoader()
        .getResourceAsStream(SCHEMA_RESOURCE_CLASSPATH)) {
        if (is != null) {
          String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          return new JsonObject(json);
        }
      }

      // Fallback to filesystem
      Path path = Paths.get(SCHEMA_RESOURCE_FS_PATH);
      if (Files.exists(path)) {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        return new JsonObject(json);
      }

      log.warn("health-dcat-ap-schema file not found in classpath or filesystem ({} or {}).",
        SCHEMA_RESOURCE_CLASSPATH, SCHEMA_RESOURCE_FS_PATH);
      return null;
    } catch (Exception e) {
      log.error("Failed to load health-dcat-ap-schema from resource", e);
      return null;
    }
  }

  /**
   * Process the translation callback by merging the translated values back into the original schema
   * payload and updating the MongoDB document.
   *
   * @param name The name of the configuration document to update.
   * @param dict The translation dictionary received from the callback.
   */
  public void processTranslationCallback(String name, JsonObject dict) {
    JsonObject query = new JsonObject().put(NAME, name);
    mongoClient.findOne(CONFIGURATION_COLLECTION, query, null)
      .compose(doc -> {
        if (doc == null) {
          log.warn("Received translation callback for unknown config '{}'", name);
          return Future.failedFuture("Config not found: " + name);
        }
        JsonObject payload = doc.getJsonObject(PAYLOAD, new JsonObject());

        // Apply translations onto the payload (recursive merge)
        translationService.applyTranslationsToSchema(payload, dict);

        JsonObject update = new JsonObject().put("$set", new JsonObject().put(PAYLOAD, payload));
        return mongoClient.updateCollection(CONFIGURATION_COLLECTION, query, update);
      })
      .onFailure(err -> log.error("Failed to process translation callback for '{}':", name, err));
  }
}
