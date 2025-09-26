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

import static eu.europa.data.health.access.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

class HubStoreClientTest {

  private WebClient webClient;
  private HubStoreClient hubStoreClient;
  private HttpRequest<Buffer> httpRequest;
  private static final String API_URL = "http://example.com";
  private static final String API_TOKEN = "dummy-token";

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
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.sendMultipartForm(any(MultipartForm.class))).thenReturn(Future.succeededFuture(response));

    Future<JsonObject> result = hubStoreClient.uploadFile("parameterType", "parameterId", "test.txt", Buffer.buffer("test data"), "text/plain");

    assertTrue(result.succeeded());
    assertEquals("success", result.result().getString("status"));
  }

  @Test
  void testUploadFile_Failure() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(500);
    when(response.statusMessage()).thenReturn("Internal Server Error");

    when(webClient.postAbs(eq(API_URL + "/data"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.sendMultipartForm(any(MultipartForm.class)))
      .thenReturn(Future.succeededFuture(response));

    Future<JsonObject> result = hubStoreClient.uploadFile("parameterType", "parameterId", "test.txt", Buffer.buffer("test data"), "text/plain");

    assertTrue(result.failed());
    assertEquals("Failed to upload file: Internal Server Error", result.cause().getMessage());
  }

  @Test
  void testGetFile_Success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.bodyAsBuffer()).thenReturn(Buffer.buffer("file data"));

    when(webClient.getAbs(eq(API_URL + DATA))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq("id"), eq("fileId"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.getFile("fileId", "parameterType", "parameterId");

    assertTrue(result.succeeded());
    assertEquals("file data", result.result().bodyAsBuffer().toString());
  }

  @Test
  void testGetFile_Failure() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(404);
    when(response.statusMessage()).thenReturn("Not Found");

    when(webClient.getAbs(eq(API_URL + DATA))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq("id"), eq("fileId"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.getFile("fileId","parameterType", "parameterId");

    assertTrue(result.failed());
    assertEquals("Failed to download file: Not Found", result.cause().getMessage());
  }

  @Test
  void testDeleteFile_Success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(202);

    when(webClient.deleteAbs(eq(API_URL + "/data/fileId"))).thenReturn(httpRequest);
    when(httpRequest.putHeader(eq("X-API-Key"), eq(API_TOKEN))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.deleteFile("fileId", "parameterType", "parameterId");

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
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    // Call the method
    Future<HttpResponse<Buffer>> result = hubStoreClient.deleteFile("fileId", "parameterType", "parameterId");

    // Verify
    assertTrue(result.failed());
    assertEquals("Failed to delete file: Internal Server Error", result.cause().getMessage());
  }

  @Test
  void testDeleteFileByParameter_Success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(202);
    when(response.bodyAsString()).thenReturn("File deleted successfully");

    when(webClient.deleteAbs(eq(API_URL + DATA))).thenReturn(httpRequest);
    when(httpRequest.putHeader(eq("X-API-Key"), eq(API_TOKEN))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.deleteFileByParameter("parameterType", "parameterId");

    assertTrue(result.succeeded());
    assertEquals(202, result.result().statusCode());
    assertEquals("File deleted successfully", result.result().bodyAsString());

    verify(webClient).deleteAbs(eq(API_URL + DATA));
    verify(httpRequest).putHeader(eq("X-API-Key"), eq(API_TOKEN));
    verify(httpRequest).addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"));
    verify(httpRequest).addQueryParam(eq(PARAMETER_ID), eq("parameterId"));
    verify(httpRequest).send();
  }

  @Test
  void testDeleteFileByParameter_Failure() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(500);
    when(response.bodyAsString()).thenReturn("Internal Server Error");

    when(webClient.deleteAbs(eq(API_URL + DATA))).thenReturn(httpRequest);
    when(httpRequest.putHeader(eq("X-API-Key"), eq(API_TOKEN))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(eq(PARAMETER_ID), eq("parameterId"))).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> result = hubStoreClient.deleteFileByParameter("parameterType", "parameterId");

    assertTrue(result.failed());
    assertEquals("Failed to delete file: null", result.cause().getMessage());

    verify(webClient).deleteAbs(eq(API_URL + DATA));
    verify(httpRequest).putHeader(eq("X-API-Key"), eq(API_TOKEN));
    verify(httpRequest).addQueryParam(eq(PARAMETER_TYPE), eq("parameterType"));
    verify(httpRequest).addQueryParam(eq(PARAMETER_ID), eq("parameterId"));
    verify(httpRequest).send();
  }
  @Test
  void testGetFileAsBase64_success() {
    HttpResponse<Buffer> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.bodyAsBuffer()).thenReturn(Buffer.buffer("hello", String.valueOf(StandardCharsets.UTF_8)));
    when(response.getHeader(eq("Content-Disposition")))
      .thenReturn("attachment; filename*=UTF-8''my%20file.TXT");

    when(webClient.getAbs(eq("http://example.com" + DATA))).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(response));

    Future<JsonObject> fut =
      hubStoreClient.getFileAsBase64("file-1", "parameterType", "parameterId");

    assertTrue(fut.succeeded());
    JsonObject out = fut.result();
    assertEquals("file-1", out.getString("fileId"));
    assertEquals("my file.TXT", out.getString("fileName")); // decoded via decodeFilename
    assertEquals("txt", out.getString("format"));            // via extractFileType (lower-cased)
    assertEquals(Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8)),
      out.getString("base64Content"));
  }

  @Test
  void testGetFileAsBase64_underlyingGetFileFails() {
    HubStoreClient spyClient = spy(new HubStoreClient(webClient, API_URL, API_TOKEN));

    doReturn(Future.failedFuture("boom"))
      .when(spyClient).getFile(eq("bad-id"), anyString(), anyString());

    Future<JsonObject> fut = spyClient.getFileAsBase64("bad-id", "anyType", "anyId");

    assertTrue(fut.failed());
    assertEquals("boom", fut.cause().getMessage());
  }

  // ---------- decodeFilename (protected) ----------

  @Test
  void testDecodeFilename_variants() {
    // Same package → can call protected directly
    String h1 = "attachment; filename*=UTF-8''report%20final.PDF";
    assertEquals("report final.PDF", hubStoreClient.decodeFilename(h1));

    // Unicode percent-encoded
    String h2 = "attachment; filename*=UTF-8''%CE%A4%CE%AD%CF%83%CF%84.docx";
    assertEquals("Τέστ.docx", hubStoreClient.decodeFilename(h2));

    // Missing filename*= → fallback string
    String h3 = "attachment; filename=plain.docx";
    assertEquals("Error_decoding_filename", hubStoreClient.decodeFilename(h3));

    // Null / empty → fallback
    assertEquals("Error_decoding_filename", hubStoreClient.decodeFilename(null));
    assertEquals("Error_decoding_filename", hubStoreClient.decodeFilename(""));
  }

  // ---------- extractFileType (private, via reflection) ----------

  @Test
  void testExtractFileType_reflection() throws Exception {
    Method m = HubStoreClient.class.getDeclaredMethod("extractFileType", String.class);
    m.setAccessible(true);

    assertEquals("unknown", m.invoke(hubStoreClient, (Object) null));
    assertEquals("unknown", m.invoke(hubStoreClient, "nofile"));
    assertEquals("txt",     m.invoke(hubStoreClient, "a.TXT"));             // lower-cased
    assertEquals("gz",      m.invoke(hubStoreClient, "archive.tar.gz"));    // last segment
    assertEquals("hidden",  m.invoke(hubStoreClient, ".hidden"));           // substring after first dot
  }

  // ---------- getFilesAsBase64 ----------

  @Test
  void testGetFilesAsBase64_successMultiple() {
    HubStoreClient spyClient = spy(new HubStoreClient(webClient, API_URL, API_TOKEN));

    JsonObject j1 = new JsonObject().put("fileId", "id1");
    JsonObject j2 = new JsonObject().put("fileId", "id2");

    // IMPORTANT: doReturn(...).when(spy) — do NOT use when(...).thenReturn(...)
    doReturn(Future.succeededFuture(j1))
      .when(spyClient).getFileAsBase64(eq("id1"), anyString(), eq("app-1"));
    doReturn(Future.succeededFuture(j2))
      .when(spyClient).getFileAsBase64(eq("id2"), anyString(), eq("app-1"));

    Future<List<JsonObject>> fut = spyClient.getFilesAsBase64(List.of("id1", "id2"), "app-1");

    assertTrue(fut.succeeded());
    List<JsonObject> out = fut.result();
    assertEquals(List.of("id1", "id2"),
      out.stream().map(o -> o.getString("fileId")).toList());

    verify(spyClient).getFileAsBase64("id1", APPLICATIONID, "app-1");
    verify(spyClient).getFileAsBase64("id2", APPLICATIONID, "app-1");
  }

  @Test
  void testGetFilesAsBase64_partialFailureFailsAll() {
    HubStoreClient spyClient =
      spy(new HubStoreClient(webClient, API_URL, API_TOKEN));

    // IMPORTANT: use doReturn(...).when(spy) to avoid calling the real method
    doReturn(Future.succeededFuture(new JsonObject().put("fileId", "id1")))
      .when(spyClient).getFileAsBase64(eq("id1"), anyString(), anyString());
    doReturn(Future.failedFuture("oops"))
      .when(spyClient).getFileAsBase64(eq("id2"), anyString(), anyString());

    Future<List<JsonObject>> fut =
      spyClient.getFilesAsBase64(List.of("id1", "id2"), "app-1");

    assertTrue(fut.failed());
    assertEquals("oops", fut.cause().getMessage());
  }

}
