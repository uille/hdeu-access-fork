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

import static eu.europa.data.health.access.Constants.DATASET_ASSISTANT_COLLECTION;
import static eu.europa.data.health.access.Constants.PAYLOAD;
import static eu.europa.data.health.access.Constants.USER_NAME;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages Dataset Description Assistant (DDA) payloads per authenticated user.
 *
 * <p>Document shape in MongoDB (one document per user):
 * <pre>
 * {
 *   "_id": ObjectId(...),
 *   "userName": "local_dev",
 *   "payload": {
 *     "accessLevel": "public",
 *     "language": "en",
 *     "formData": { ... } // arbitrary object from the client
 *   },
 *   "createdAt": "2025-08-28T09:40:00Z",
 *   "updatedAt": "2025-08-28T09:42:31Z"
 * }
 * </pre>
 *
 * <p>POST /dda: upserts (create/update) "payload" for the authenticated user.
 * <br>GET  /dda: returns the saved document for the authenticated user (without _id).
 */
public class DdaDatasetManager extends AbstractManager {

  private static final String DATASET_DESCRIPTION_ASSISTANT_LANGUAGE = "datasetDescriptionAssistantLanguage";
  private static final String DATASET_DESCRIPTION_ASSISTANT_FORM_DATA = "datasetDescriptionAssistantFormData";
  private static final String DATASET_DESCRIPTION_ASSISTANT_ACCESS_LEVEL = "datasetDescriptionAssistantAccessLevel";

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;

  public DdaDatasetManager(DBHandler dbHandler, MongoClient mongoClient) {
    super(dbHandler);
    this.mongoClient = mongoClient;
  }

  /**
   * POST /dda
   * <p>Creates or updates the current user's DDA payload.
   *
   * <p>Accepted request body formats (all normalized to {@code payload}):
   * <ul>
   *   <li>{@code { "payload": { "accessLevel": "...", "language": "...", "formData": { ... } } }}</li>
   *   <li>{@code { "accessLevel": "...", "language": "...", "formData": { ... } }}</li>
   *   <li>{@code {
   *          "datasetDescriptionAssistantAccessLevel": "...",
   *          "datasetDescriptionAssistantLanguage": "...",
   *          "datasetDescriptionAssistantFormData": { ... }
   *        }}</li>
   * </ul>
   *
   * <p>Responses:
   * <ul>
   *   <li>201 Created — new document inserted</li>
   *   <li>200 OK — existing document updated</li>
   *   <li>401 Unauthorized — no authenticated user found</li>
   *   <li>400 Bad Request — missing or invalid JSON body</li>
   *   <li>500 Internal Server Error — persistence failure</li>
   * </ul>
   */
  public void createOrUpdateDatasetDescription(@NotNull RoutingContext context) {
    log.debug("Entered createOrUpdateDatasetDescription");

    final JsonObject body = context.body().asJsonObject();
    if (body == null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Missing JSON body");
      return;
    }

    // Resolve current username from the security context (Keycloak/JWT, etc.)
    final String userName = Context.getEcasId(context);
    if (userName == null || userName.isBlank()) {
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, "Unauthorized");
      return;
    }

    // Normalize whatever the client sent into a single "payload" object
    final JsonObject payload = extractPayload(body);

    upsertUserPayload(userName, payload, context);
  }

  /**
   * GET /dda
   * <p>Returns the DDA document for the authenticated user.
   *
   * <p>Responses:
   * <ul>
   *   <li>200 OK — document found (without MongoDB "_id")</li>
   *   <li>204 No Content — user has no saved dataset yet</li>
   *   <li>401 Unauthorized — no authenticated user found</li>
   *   <li>500 Internal Server Error — retrieval failure</li>
   * </ul>
   */
  public void getDatasetDescription(@NotNull RoutingContext context) {
    log.debug("Entered getDatasetDescription");

    final String userName = Context.getEcasId(context);
    if (userName == null || userName.isBlank()) {
      setUpMessageContextResponse(context, HttpResponseStatus.UNAUTHORIZED, "Unauthorized");
      return;
    }

    final JsonObject query = new JsonObject().put(USER_NAME, userName);

    mongoClient.findOne(DATASET_ASSISTANT_COLLECTION, query, null)
      .onSuccess(doc -> {
        if (doc == null) {
          // Nothing saved yet for this user
          context.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
          return;
        }
        // Remove internal MongoDB id before returning
        doc.remove("_id");
        setUpContextResponse(context, HttpResponseStatus.OK, doc.encode());
      })
      .onFailure(err -> {
        log.error("Failed to retrieve DDA payload for user {}: {}", userName, err.getMessage());
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve dataset");
      });
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Upserts the {@code payload} for the given {@code userName}.
   * <p>Uses:
   * <ul>
   *   <li>{@code $set} to replace {@code payload} and update {@code updatedAt}</li>
   *   <li>{@code $setOnInsert} to set {@code userName} and {@code createdAt} only on first insert</li>
   * </ul>
   * This ensures idempotent updates without modifying immutable fields.
   */
  private void upsertUserPayload(String userName, JsonObject payload, RoutingContext context) {
    log.debug("Entered upsertUserPayload for {}", userName);

    final JsonObject query = new JsonObject().put(USER_NAME, userName);
    final String now = Instant.now().toString();

    final JsonObject update = new JsonObject()
      .put("$set", new JsonObject()
        .put(PAYLOAD, payload)
        .put("updatedAt", now)
      )
      .put("$setOnInsert", new JsonObject()
        .put(USER_NAME, userName)
        .put("createdAt", now)
      );

    final UpdateOptions options = new UpdateOptions().setUpsert(true);

    mongoClient.updateCollectionWithOptions(DATASET_ASSISTANT_COLLECTION, query, update, options, res -> {
      if (res.succeeded()) {
        // Vert.x UpdateResult: if an upsert happened, getDocUpsertedId() is non-null
        final var ur = res.result();
        final boolean created = ur != null && ur.getDocUpsertedId() != null;
        final int status = created ? HttpResponseStatus.CREATED.code() : HttpResponseStatus.OK.code();

        // Minimal response body (no _id)
        final JsonObject response = new JsonObject()
          .put(USER_NAME, userName)
          .put(PAYLOAD, payload)
          .put(created ? "createdAt" : "updatedAt", now);

        context.response()
          .setStatusCode(status)
          .putHeader("Content-Type", "application/json")
          .end(response.encode());
      } else {
        log.error("Save dataset for user {} failed: {}", userName, res.cause().getMessage());
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Save dataset failed");
      }
    });
  }

  /**
   * Normalizes the client body into a single "payload" object with keys:
   * {@code accessLevel}, {@code language}, and {@code formData}.
   *
   * <p>Accepts any of the following shapes:
   * <ul>
   *   <li>{@code { "payload": { ... } }}</li>
   *   <li>{@code { "accessLevel": "...", "language": "...", "formData": { ... } }}</li>
   *   <li>{@code {
   *          "datasetDescriptionAssistantAccessLevel": "...",
   *          "datasetDescriptionAssistantLanguage": "...",
   *          "datasetDescriptionAssistantFormData": { ... } | "..."  // stringified JSON also handled
   *        }}</li>
   * </ul>
   */
  private JsonObject extractPayload(JsonObject body) {
    // Case A: already nested under "payload"
    if (body.containsKey(PAYLOAD) && body.getValue(PAYLOAD) instanceof JsonObject) {
      return body.getJsonObject(PAYLOAD);
    }

    // Case B: compact keys at the root
    if (body.containsKey("accessLevel") || body.containsKey("language") || body.containsKey("formData")) {
      return new JsonObject()
        .put(DATASET_DESCRIPTION_ASSISTANT_ACCESS_LEVEL, body.getValue("accessLevel"))
        .put(DATASET_DESCRIPTION_ASSISTANT_LANGUAGE, body.getValue("language"))
        .put(DATASET_DESCRIPTION_ASSISTANT_FORM_DATA, body.getValue("formData"));
    }

    // Case C: current frontend keys (with defensive parsing for formData if stringified)
    Object fd = body.getValue(DATASET_DESCRIPTION_ASSISTANT_FORM_DATA);
    if (fd instanceof String) {
      try {
        fd = new JsonObject((String) fd);
      } catch (Exception ignored) {
        // keep as string if not valid JSON
      }
    }

    return new JsonObject()
      .put(DATASET_DESCRIPTION_ASSISTANT_ACCESS_LEVEL, body.getValue(DATASET_DESCRIPTION_ASSISTANT_ACCESS_LEVEL))
      .put(DATASET_DESCRIPTION_ASSISTANT_LANGUAGE, body.getValue(DATASET_DESCRIPTION_ASSISTANT_LANGUAGE))
      .put(DATASET_DESCRIPTION_ASSISTANT_FORM_DATA, fd);
  }
}
