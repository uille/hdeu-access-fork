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
package eu.europa.data.health.access.mail;

import static eu.europa.data.health.access.mail.MailClient.ERROR_CODE_REQUEST_FAILURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class MailClientTest {

  @Mock
  private WebClient webClient;

  @Mock
  private HttpRequest<Buffer> httpRequest;

  @Mock
  private HttpResponse<Buffer> httpResponse;

  private MailClient mailClient;

  @BeforeEach
  void setUp() {
    mailClient = new MailClient(
      "http://mail-service",
      "secret-key",
      webClient
    );

    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);
  }

  @Test
  void sendMail_whenStatus200_shouldComplete(VertxTestContext testContext) {

    when(httpResponse.statusCode()).thenReturn(200);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject().put("to", "alice@test.com"))
      .onComplete(testContext.succeeding(v -> {
        verify(webClient).postAbs("http://mail-service" + MailClient.MAIL_DATA_ENDPOINT);
        verify(httpRequest).putHeaders(any());
        verify(httpRequest).putHeaders(argThat(headers ->
          "secret-key".equals(headers.get("X-API-Key"))
        ));
        testContext.completeNow();
      }));
  }

  @Test
  void sendMail_whenStatus500_shouldFail(VertxTestContext testContext) {

    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.bodyAsString()).thenReturn("Server error");

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject().put("to", "alice@test.com"))
      .onComplete(testContext.failing(err -> {

        // Fails with JSON string
        JsonObject errorJson = new JsonObject(err.getMessage());

        assertEquals(500, errorJson.getInteger(MailClient.ERROR_CODE));
        assertEquals("Server error", errorJson.getString(MailClient.ERROR_DETAILS));

        testContext.completeNow();
      }));
  }

  @Test
  void sendMail_whenRequestFails_shouldFail(VertxTestContext testContext) {

    RuntimeException networkError =
      new RuntimeException("Connection refused");

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.failedFuture(networkError));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject())
      .onComplete(testContext.failing(err -> {

        JsonObject errorJson = new JsonObject(err.getMessage());

        assertEquals(ERROR_CODE_REQUEST_FAILURE, errorJson.getInteger(MailClient.ERROR_CODE));
        assertEquals("Connection refused",
          errorJson.getString(MailClient.ERROR_DETAILS));

        testContext.completeNow();
      }));
  }

  @Test
  void sendMail_whenApiKeyEmpty_shouldNotAddHeader(VertxTestContext testContext) {

    mailClient = new MailClient(
      "http://mail-service",
      "", // empty API key
      webClient
    );

    when(httpResponse.statusCode()).thenReturn(200);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject())
      .onComplete(testContext.succeeding(v -> {

        verify(httpRequest).putHeaders(argThat(headers ->
          headers.get("X-API-Key") == null
        ));

        testContext.completeNow();
      }));
  }

  @Test
  void sendMail_whenStatus201_shouldComplete(VertxTestContext testContext) {

    when(httpResponse.statusCode()).thenReturn(201);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject().put("to", "alice@test.com"))
      .onComplete(testContext.succeeding(v -> {
        verify(webClient)
          .postAbs("http://mail-service" + MailClient.MAIL_DATA_ENDPOINT);
        testContext.completeNow();
      }));
  }

  @Test
  void sendMail_whenStatus400_shouldFail(VertxTestContext testContext) {

    when(httpResponse.statusCode()).thenReturn(400);
    when(httpResponse.bodyAsString()).thenReturn("Bad Request");

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject().put("to", "alice@test.com"))
      .onComplete(testContext.failing(err -> {

        JsonObject errorJson = new JsonObject(err.getMessage());

        assertEquals(400,
          errorJson.getInteger(MailClient.ERROR_CODE));
        assertEquals("Bad Request",
          errorJson.getString(MailClient.ERROR_DETAILS));

        testContext.completeNow();
      }));
  }

  @Test
  void sendMail_whenStatus204_shouldComplete(VertxTestContext testContext) {

    when(httpResponse.statusCode()).thenReturn(204);

    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler =
        invocation.getArgument(1);

      handler.handle(Future.succeededFuture(httpResponse));
      return null;
    }).when(httpRequest).sendMultipartForm(any(), any());

    mailClient.sendMail(new JsonObject().put("to", "alice@test.com"))
      .onComplete(testContext.succeeding(v -> {
        verify(webClient)
          .postAbs("http://mail-service" + MailClient.MAIL_DATA_ENDPOINT);
        testContext.completeNow();
      }));
  }

}
