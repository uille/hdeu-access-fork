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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HubStoreClientTest {

  private WebClient webClient;
  private HubStoreClient hubStoreClient;
  private HttpRequest<Buffer> httpRequest;
  private static final String API_URL = "http://example.com";
  private static final String API_TOKEN = "dummy-token";
  private static final String APPLICATION_ID = "applicationId";

  @BeforeEach
  void setUp() {
    webClient = mock(WebClient.class);
    httpRequest = mock(HttpRequest.class);
    hubStoreClient = new HubStoreClient(webClient, API_URL, API_TOKEN);
  }

  @Test
  void testUploadFile_Success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(201);
    when(response.bodyAsJsonObject()).thenReturn(new JsonObject().put("status", "success"));

    when(webClient.postAbs(eq(API_URL + "/data"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.sendMultipartForm(any(MultipartForm.class)))
      .thenReturn(Future.succeededFuture(response));

    Future<JsonObject> result = hubStoreClient.uploadFile("appId", "test.txt", Buffer.buffer("test data"), "text/plain");

    assertTrue(result.succeeded());
    assertEquals("success", result.result().getString("status"));
  }

  @Test
  void testUploadFile_Failure() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(500);
    when(response.statusMessage()).thenReturn("Internal Server Error");

    when(webClient.postAbs(eq(API_URL + "/data"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.sendMultipartForm(any(MultipartForm.class)))
      .thenReturn(Future.succeededFuture(response));

    Future<JsonObject> result = hubStoreClient.uploadFile("appId", "test.txt", Buffer.buffer("test data"), "text/plain");

    assertTrue(result.failed());
    assertEquals("Failed to upload file: Internal Server Error", result.cause().getMessage());
  }

  @Test
  void testGetFile_Success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.bodyAsBuffer()).thenReturn(Buffer.buffer("file data"));

    when(webClient.getAbs(eq(API_URL + "/data/fileId"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(APPLICATION_ID), eq(APPLICATION_ID))).thenReturn(httpRequest);
    when(httpRequest.send())
      .thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.getFile("fileId", "applicationId");

    assertTrue(result.succeeded());
    assertEquals("file data", result.result().bodyAsBuffer().toString());
  }

  @Test
  void testGetFile_Failure() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(404);
    when(response.statusMessage()).thenReturn("Not Found");

    when(webClient.getAbs(eq(API_URL + "/data/fileId"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(APPLICATION_ID), eq("applicationId"))).thenReturn(httpRequest);
    when(httpRequest.send())
      .thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.getFile("fileId","applicationId");

    assertTrue(result.failed());
    assertEquals("Failed to download file: Not Found", result.cause().getMessage());
  }

  @Test
  void testDeleteFile_Success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(202);

    when(webClient.deleteAbs(eq(API_URL + "/data/fileId"))).thenReturn(httpRequest);
    when(httpRequest.putHeader(eq("X-API-Key"), eq(API_TOKEN))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(APPLICATION_ID), eq("applicationId"))).thenReturn(httpRequest);
    when(httpRequest.send())
      .thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.deleteFile("fileId", "applicationId");

    assertTrue(result.succeeded());
  }

  @Test
  void testDeleteFile_Failure() {
    // Mock HttpResponse
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(500);
    when(response.statusMessage()).thenReturn("Internal Server Error");

    // Mock WebClient and HttpRequest behavior
    when(webClient.deleteAbs(eq(API_URL + "/data/fileId"))).thenReturn(httpRequest);
    when(httpRequest.putHeader(eq("X-API-Key"), eq(API_TOKEN))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(APPLICATION_ID), eq("applicationId"))).thenReturn(httpRequest);
    when(httpRequest.send())
      .thenReturn(Future.succeededFuture(response));

    // Call the method
    Future<HttpResponse<Buffer>> result = hubStoreClient.deleteFile("fileId", "applicationId");

    // Verify
    assertTrue(result.failed());
    assertEquals("Failed to delete file: Internal Server Error", result.cause().getMessage());
  }
}
