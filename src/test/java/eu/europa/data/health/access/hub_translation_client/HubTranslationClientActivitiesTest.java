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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HubTranslationClientActivitiesTest {

  private static final String HUB_TRANSLATION_ENDPOINT =
    "http://translation-service/translation-service";
  private static final String FILE_TRANSLATION_ENDPOINT =
    "http://file-translation-service/file-translation-service";
  private static final String HUB_TRANSLATION_API_KEY = "hub-api-key";
  private static final String PORTAL_API_KEY = "portal-api-key";
  private static final String PORTAL_ENDPOINT = "http://portal-service/callback";

  @Mock
  private WebClient webClient;

  @Mock
  private HttpRequest<Buffer> httpRequest;

  @Mock
  private HttpResponse<Buffer> httpResponse;

  private HubTranslationClientActivities activities;

  @BeforeEach
  void setUp() {
    activities = new HubTranslationClientActivities(
      webClient,
      HUB_TRANSLATION_API_KEY,
      HUB_TRANSLATION_ENDPOINT,
      PORTAL_API_KEY,
      PORTAL_ENDPOINT
    );
  }

  @Test
  void createCallbackJson_containsExpectedPortalCallbackConfiguration() {
    JsonObject callbackJson = activities.createCallbackJson();

    assertEquals(PORTAL_ENDPOINT, callbackJson.getString("url"));
    assertEquals("POST", callbackJson.getString("method"));
    assertEquals(PORTAL_API_KEY, callbackJson.getJsonObject("headers").getString("X-API-Key"));
  }

  @Test
  void requestTextTranslation_postsPayloadWithHeadersToTextEndpoint() {
    JsonObject payload = new JsonObject().put("text", "hello");
    Buffer expectedBuffer = payload.toBuffer();

    when(webClient.postAbs(HUB_TRANSLATION_ENDPOINT)).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    when(httpRequest.sendBuffer(any(Buffer.class))).thenReturn(
      Future.succeededFuture(httpResponse)
    );
    when(httpResponse.statusCode()).thenReturn(204);

    Future<Void> result = activities.requestTextTranslation(payload);

    assertTrue(result.succeeded());
    ArgumentCaptor<MultiMap> headersCaptor = ArgumentCaptor.forClass(MultiMap.class);
    ArgumentCaptor<Buffer> bodyCaptor = ArgumentCaptor.forClass(Buffer.class);
    verify(webClient).postAbs(HUB_TRANSLATION_ENDPOINT);
    verify(httpRequest).putHeaders(headersCaptor.capture());
    verify(httpRequest).sendBuffer(bodyCaptor.capture());
    assertEquals(expectedBuffer.toString(), bodyCaptor.getValue().toString());
    assertEquals("application/json", headersCaptor.getValue().get("Content-Type"));
    assertEquals(
      String.valueOf(expectedBuffer.length()),
      headersCaptor.getValue().get("Content-Length")
    );
    assertEquals(HUB_TRANSLATION_API_KEY, headersCaptor.getValue().get("X-API-Key"));
  }

  @Test
  void requestFileTranslation_usesDerivedFileTranslationEndpoint() {
    JsonObject payload = new JsonObject().put("fileId", "file-1");

    when(webClient.postAbs(FILE_TRANSLATION_ENDPOINT)).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    when(httpRequest.sendBuffer(any(Buffer.class))).thenReturn(
      Future.succeededFuture(httpResponse)
    );
    when(httpResponse.statusCode()).thenReturn(204);

    Future<Void> result = activities.requestFileTranslation(payload);

    assertTrue(result.succeeded());
    verify(webClient).postAbs(FILE_TRANSLATION_ENDPOINT);
  }

  @Test
  void requestTranslation_returnsFailedFutureWhenResponseStatusIsNotNoContent() {
    JsonObject payload = new JsonObject().put("text", "hello");

    when(webClient.postAbs(HUB_TRANSLATION_ENDPOINT)).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any(MultiMap.class))).thenReturn(httpRequest);
    when(httpRequest.sendBuffer(any(Buffer.class))).thenReturn(
      Future.succeededFuture(httpResponse)
    );
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.statusMessage()).thenReturn("Internal Server Error");

    Future<Void> result = activities.requestTextTranslation(payload);

    assertTrue(result.failed());
    assertEquals("Translation request failed: Internal Server Error", result.cause().getMessage());
  }
}
