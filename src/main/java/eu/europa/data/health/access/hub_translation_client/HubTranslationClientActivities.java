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

import static eu.europa.data.health.access.Constants.APPLICATION_JSON;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubTranslationClientActivities {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebClient client;
  private final String hubTranslationApiKey;
  private final String fileTranslationEndpoint;
  private final String textTranslationEndpoint;
  private final String portalApiKey;
  private final String portalEndpoint;

  public HubTranslationClientActivities(
      WebClient client,
      String hubTranslationApiKey,
      String hubTranslationEndpoint,
      String portalApiKey,
      String portalEndpoint
  ) {
    this.client = client;
    this.hubTranslationApiKey = hubTranslationApiKey;
    this.fileTranslationEndpoint = hubTranslationEndpoint.replace("/translation-service" ,
        "/file-translation-service");
    this.textTranslationEndpoint = hubTranslationEndpoint;
    this.portalApiKey = portalApiKey;
    this.portalEndpoint = portalEndpoint;
  }

  public Future<Void> requestTextTranslation(JsonObject payload) {
    return requestTranslation(textTranslationEndpoint, payload);
  }

  public Future<Void> requestFileTranslation(JsonObject payload) {
    return requestTranslation(fileTranslationEndpoint, payload);
  }

  /**
   * Sends a translation request to the Hub Translation API.
   *
   * @param url     The translation endpoint URL.
   * @param payload The JSON payload containing translation details.
   * @return A Future indicating the success or failure of the request.
   */
  private Future<Void> requestTranslation(String url, JsonObject payload) {
    Buffer buffer = payload.toBuffer();
    MultiMap headers = createHeader(String.valueOf(buffer.length()));
    return client
        .postAbs(url)
        .putHeaders(headers)
        .sendBuffer(buffer)
        .compose(res -> {
          int code = res.statusCode();
          if (code == 204) {
            log.debug("Translation request completed successfully");
            return Future.succeededFuture();
          } else {
            log.error("Translation request failed: HTTP {}", code);
            return Future.failedFuture("Translation request failed: " + res.statusMessage());
          }
        });
  }

  /**
   * Creates HTTP headers for the translation request.
   *
   * @param length The content length.
   * @return A MultiMap containing the HTTP headers.
   */
  private MultiMap createHeader(String length) {
    return MultiMap.caseInsensitiveMultiMap()
        .add(CONTENT_TYPE, APPLICATION_JSON)
        .add("Content-Length", length)
        .add("X-API-Key", this.hubTranslationApiKey);
  }

  /**
   * Creates the callback JSON object for the translation request payload.
   *
   * @return A JsonObject representing the callback configuration.
   */
  public JsonObject createCallbackJson() {
    return new JsonObject()
        .put("url", portalEndpoint)
        .put("method", "POST")
        .put("headers", new JsonObject().put("X-API-Key", this.portalApiKey));
  }
}
