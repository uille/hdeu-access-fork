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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Constants;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranslationCallbackManagerTest {

  @Mock
  private DataRequestApplicationManager dataRequestManager;

  @Mock
  private DataAccessApplicationManager dataAccessManager;

  @Mock
  private ControlledVocabularyManager controlledVocabularyManager;

  @Mock
  private PayloadManager payloadManager;

  @Mock
  private HubStoreClient hubStoreClient;

  @Mock
  private RoutingContext context;

  @Mock
  private HttpServerResponse response;

  @Mock
  private RequestBody requestBody;

  @InjectMocks
  private TranslationCallbackManager manager;

  @BeforeEach
  void setUp() {
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());
  }

  @Test
  void testHandleTranslationCallback_missingFields_returns400() {
    JsonObject body = new JsonObject().put("data", new JsonObject().put("applicationId", ""));

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(response).setStatusCode(400);
    verify(response).end("Missing required fields");
  }

  @Test
  void testHandleTranslationCallback_formTranslation_dataRequest() {
    JsonObject dict = new JsonObject().put("field", new JsonObject());
    JsonObject data = new JsonObject().put("applicationId", "app123")
      .put("applicationType", Constants.DATA_REQUEST_APPLICATION_COLLECTION);
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(dataRequestManager).processTranslationCallback("app123", dict);
    verify(response).setStatusCode(200);
  }

  @Test
  void testHandleTranslationCallback_formTranslation_dataAccess() {
    JsonObject dict = new JsonObject().put("field", new JsonObject());

    JsonObject data = new JsonObject().put("applicationId", "app456")
      .put("applicationType", Constants.DATA_ACCESS_APPLICATION_COLLECTION);
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(dataAccessManager).processTranslationCallback("app456", dict);
    verify(response).setStatusCode(200);
  }

  @Test
  void testHandleTranslationCallback_formTranslation_controlledVocabulary() {
    JsonObject dict = new JsonObject().put("field", new JsonObject());
    JsonObject data = new JsonObject()
      .put("applicationId", "vocab123")
      .put("applicationType", Constants.CONTROLLED_VOCABULARY_COLLECTION);
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(controlledVocabularyManager).processTranslationCallback("vocab123", dict);
    verify(response).setStatusCode(200);
  }

  @Test
  void testHandleTranslationCallback_formTranslation_configuration() {
    JsonObject dict = new JsonObject().put("field", new JsonObject());
    JsonObject data = new JsonObject()
      .put("applicationId", "app456")
      .put("applicationType", Constants.CONFIGURATION_COLLECTION)
      .put("documentName", "config123");
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(payloadManager).processTranslationCallback("config123", dict);
    verify(response).setStatusCode(200);
  }

  @Test
  void testHandleTranslationCallback_formTranslation_configuration_nullDocumentName() {
    JsonObject dict = new JsonObject().put("field", new JsonObject());
    JsonObject data = new JsonObject()
      .put("applicationId", "app456")
      .put("applicationType", Constants.CONFIGURATION_COLLECTION);
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(payloadManager, times(0)).processTranslationCallback(anyString(), any(JsonObject.class));
    verify(response).setStatusCode(400);
  }

  @Test
  void testHandleTranslationCallback_formTranslation_other() {
    String appId = "app789";
    String fileId = "file123";
    String base64 = Base64.getEncoder().encodeToString("dummy content".getBytes());

    JsonObject dict = new JsonObject().put("file", new JsonObject().put(fileId,
      new JsonObject().put("documentToTranslateBase64",
          new JsonObject().put("fileName", "doc.pdf").put("format", "pdf"))
        .put("translations", new JsonObject().put("en", base64))));

    JsonObject data = new JsonObject().put("applicationId", appId).put("applicationType", "other");

    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    JsonObject uploadResult = new JsonObject().put("result",
      new JsonObject().put("id", "newFileId"));
    when(hubStoreClient.uploadFile(anyString(), eq(appId), anyString(), any(Buffer.class),
      eq("pdf"))).thenReturn(Future.succeededFuture(uploadResult));

    manager.handleTranslationCallback(context);

    verify(response).setStatusCode(400);
  }

  @Test
  void testHandleTranslationCallback_unknownApplicationType_returns400() {
    JsonObject dict = new JsonObject();
    JsonObject data = new JsonObject().put("applicationId", "app999")
      .put("applicationType", "unknownType");
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(response).setStatusCode(400);
    verify(response).end("Unknown application type");
  }

    @Test
    void testHandleTranslationCallback_missingDict_returns400() {
        JsonObject data = new JsonObject().put("applicationId", "app999").put("applicationType", "unknownType");
        JsonObject body = new JsonObject().put("data", data);

        when(context.body()).thenReturn(requestBody);
        when(requestBody.asJsonObject()).thenReturn(body);

        manager.handleTranslationCallback(context);

        verify(response).setStatusCode(400);
        verify(response).end("Missing required fields");
    }

  @Test
  void testHandleTranslationCallback_translationError_dataRequestApplication() {
    JsonObject dict = new JsonObject().put(
      "file",
      new JsonObject().put(
        "fileId",
        new JsonObject().put("translations", new JsonObject().put("en", -1))
      )
    );
    JsonObject data = new JsonObject()
      .put("applicationId", "app123")
      .put("applicationType", Constants.DATA_REQUEST_APPLICATION_COLLECTION);
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(dataRequestManager).incrementTranslationAttempts(
      "app123",
      "Translation error code(s): [-1]",
      "fileId"
    );
    verify(response).setStatusCode(200);
    verify(response).end("Translation error handled");
  }

  @Test
  void testHandleTranslationCallback_translationError_dataAccessApplication() {
    JsonObject dict = new JsonObject().put(
      "file",
      new JsonObject().put(
        "fileId",
        new JsonObject().put("translations", new JsonObject().put("en", -1))
      )
    );
    JsonObject data = new JsonObject()
      .put("applicationId", "app456")
      .put("applicationType", Constants.DATA_ACCESS_APPLICATION_COLLECTION);
    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    manager.handleTranslationCallback(context);

    verify(dataAccessManager).incrementTranslationAttempts(
      "app456",
      "Translation error code(s): [-1]",
      "fileId"
    );
    verify(response).setStatusCode(200);
    verify(response).end("Translation error handled");
  }

  @Test
  void testHandleTranslationCallback_fileTranslation_success() {
    String appId = "app789";
    String fileId = "file123";
    String base64 = Base64.getEncoder().encodeToString("dummy content".getBytes());

    JsonObject dict = new JsonObject().put("file", new JsonObject().put(fileId,
      new JsonObject().put("documentToTranslateBase64",
          new JsonObject().put("fileName", "doc.pdf").put("format", "pdf"))
        .put("translations", new JsonObject().put("en", base64))));

    JsonObject data = new JsonObject().put("applicationId", appId)
      .put("applicationType", Constants.DATA_REQUEST_APPLICATION_COLLECTION);

    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    JsonObject uploadResult = new JsonObject().put("result",
      new JsonObject().put("id", "newFileId"));
    when(hubStoreClient.uploadFile(anyString(), eq(appId), anyString(), any(Buffer.class),
      eq("pdf"))).thenReturn(Future.succeededFuture(uploadResult));

    manager.handleTranslationCallback(context);

    verify(hubStoreClient).uploadFile(anyString(), eq(appId), contains("_en.pdf"), any(),
      eq("pdf"));
    verify(dataRequestManager).processFileTranslationCallback(eq(appId), eq(fileId),
      any(Map.class));
    verify(response).setStatusCode(200);
  }

  @Test
  void testHandleTranslationCallbackAccess_fileTranslation_success() {
    String appId = "app789";
    String fileId = "file123";
    String base64 = Base64.getEncoder().encodeToString("dummy content".getBytes());

    JsonObject dict = new JsonObject().put("file", new JsonObject().put(fileId,
      new JsonObject().put("documentToTranslateBase64",
          new JsonObject().put("fileName", "doc.pdf").put("format", "pdf"))
        .put("translations", new JsonObject().put("en", base64))));

    JsonObject data = new JsonObject().put("applicationId", appId)
      .put("applicationType", Constants.DATA_ACCESS_APPLICATION_COLLECTION);

    JsonObject body = new JsonObject().put("data", data).put("dict", dict);

    when(context.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    JsonObject uploadResult = new JsonObject().put("result",
      new JsonObject().put("id", "newFileId"));
    when(hubStoreClient.uploadFile(anyString(), eq(appId), anyString(), any(Buffer.class),
      eq("pdf"))).thenReturn(Future.succeededFuture(uploadResult));

    manager.handleTranslationCallback(context);

    verify(hubStoreClient).uploadFile(anyString(), eq(appId), contains("_en.pdf"), any(),
      eq("pdf"));
    verify(response).setStatusCode(200);
  }
}
