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


import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ITBValidatorClient.validateDataset
 * Uses JUnit 5 and Mockito.
 */
 class ITBValidatorClientTest {

  private WebClient webClient;
  private HttpRequest<Buffer> httpRequest;
  private HttpResponse<Buffer> httpResponse;

  private RoutingContext routingContext;
  private RequestBody requestBody;
  private HttpServerRequest serverRequest;
  private HttpServerResponse serverResponse;

  private ITBValidatorClient client;

  private static final String ENDPOINT = "https://itb.example/validate";

  // Captured output from the response
  private Integer capturedStatusCode;
  private String capturedResponseBody;

  @BeforeEach
  void setUp() {
    webClient = mock(WebClient.class);
    httpRequest = mock(HttpRequest.class);
    httpResponse = mock(HttpResponse.class);

    routingContext = mock(RoutingContext.class);
    requestBody = mock(RequestBody.class);
    serverRequest = mock(HttpServerRequest.class);
    serverResponse = mock(HttpServerResponse.class);

    // Default request body and param
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asString()).thenReturn("ttl-content-here");
    when(routingContext.request()).thenReturn(serverRequest);
    when(serverRequest.getParam("accessLevel")).thenReturn("public");

    // Response: capture status and body
    capturedStatusCode = null;
    capturedResponseBody = null;

    when(routingContext.response()).thenReturn(serverResponse);

    // putHeader just returns response for chaining
    when(serverResponse.putHeader(anyString(), anyString())).thenReturn(serverResponse);

    // setStatusCode: capture
    when(serverResponse.setStatusCode(anyInt())).thenAnswer(inv -> {
      capturedStatusCode = inv.getArgument(0, Integer.class);
      return serverResponse;
    });

    // end(String): capture
    doAnswer(inv -> {
      capturedResponseBody = inv.getArgument(0, String.class);
      return null;
    }).when(serverResponse).end(anyString());

    // WebClient pipeline
    when(webClient.postAbs(eq(ENDPOINT))).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);

    client = new ITBValidatorClient(webClient, ENDPOINT);
  }

  /**
   * Helper: triggers the handler passed to sendJsonObject with a succeeded AsyncResult.
   */
  @SuppressWarnings("unchecked")
  private void completeRequestWithResponse(JsonObject expectedBodySent, HttpResponse<Buffer> resp) {
    doAnswer(invocation -> {
      JsonObject body = invocation.getArgument(0);
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);

      // Validate that the body we send is as expected if provided
      if (expectedBodySent != null) {
        assertEquals(expectedBodySent.getString("embeddingMethod"), body.getString("embeddingMethod"));
        assertEquals(expectedBodySent.getString("contentSyntax"), body.getString("contentSyntax"));
        assertEquals(expectedBodySent.getString("validationType"), body.getString("validationType"));
        assertEquals(expectedBodySent.getString("reportSyntax"), body.getString("reportSyntax"));

        // Verify BASE64 content matches source RDF
        String sentB64 = body.getString("contentToValidate");
        String decoded = new String(Base64.getDecoder().decode(sentB64));
        assertEquals("ttl-content-here", decoded);
      }

      handler.handle(new AsyncResult<>() {
        @Override public HttpResponse<Buffer> result() { return resp; }
        @Override public Throwable cause() { return null; }
        @Override public boolean succeeded() { return true; }
        @Override public boolean failed() { return false; }
      });
      return null;
    }).when(httpRequest).sendJsonObject(any(JsonObject.class), any());
  }

  /**
   * Helper: triggers the handler passed to sendJsonObject with a failed AsyncResult.
   */
  @SuppressWarnings("unchecked")
  private void failRequestWith(Throwable error) {
    doAnswer(invocation -> {
      Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
      handler.handle(new AsyncResult<>() {
        @Override public HttpResponse<Buffer> result() { return null; }
        @Override public Throwable cause() { return error; }
        @Override public boolean succeeded() { return false; }
        @Override public boolean failed() { return true; }
      });
      return null;
    }).when(httpRequest).sendJsonObject(any(JsonObject.class), any());
  }

  @Test
  void validateDataset_success200_returnsBodyAndJsonHeader() {
    // Prepare a valid JSON body
    JsonObject body = new JsonObject().put("ok", true);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.statusMessage()).thenReturn("OK");
    when(httpResponse.bodyAsString()).thenReturn(body.encode());
    when(httpResponse.bodyAsJsonObject()).thenReturn(body);

    // Expect specific request JSON parts
    JsonObject expectedSent = new JsonObject()
      .put("embeddingMethod", "BASE64")
      .put("contentSyntax", "text/turtle")
      .put("validationType", "public")
      .put("reportSyntax", "application/json");

    completeRequestWithResponse(expectedSent, httpResponse);

    // Invoke
    client.validateDataset(routingContext);

    // Assert: header and body
    verify(serverResponse).putHeader(eq("Content-Type"), eq("application/json"));
    assertNull(capturedStatusCode, "Should not set error status for success");
    assertEquals(body.encodePrettily(), capturedResponseBody);
  }

  @Test
  void validateDataset_structuralError500_returnsNormalizedFailureReport() {
    // Simulate ITB returning 500 with a body containing the structural error message
    String timestamp = Instant.now().toString();
    String message = "An error occurred while reading the provided content: bad chars";
    JsonObject itbError = new JsonObject()
      .put("timestamp", timestamp)
      .put("message", message);

    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.statusMessage()).thenReturn("Internal Server Error");
    when(httpResponse.bodyAsString()).thenReturn(itbError.encode());
    when(httpResponse.bodyAsJsonObject()).thenReturn(itbError);

    completeRequestWithResponse(null, httpResponse);

    client.validateDataset(routingContext);

    verify(serverResponse).putHeader(eq("Content-Type"), eq("application/json"));
    // Body should be in the normalized structure produced by createResponse(...)
    assertNotNull(capturedResponseBody);
    JsonObject returned = new JsonObject(capturedResponseBody);
    assertEquals(timestamp, returned.getString("date"));
    assertEquals("FAILURE", returned.getString("result"));
    JsonArray errors = returned.getJsonObject("reports").getJsonArray("error");
    assertEquals(1, errors.size());
    assertEquals(message, errors.getJsonObject(0).getString("description"));
  }

  @Test
  void validateDataset_non200WithoutStructural_returns500WithStatusAndBody() {
    when(httpResponse.statusCode()).thenReturn(400);
    when(httpResponse.statusMessage()).thenReturn("Bad Request");
    when(httpResponse.bodyAsString()).thenReturn("Oops not structural");
    // bodyAsJsonObject would not be used in this branch
    completeRequestWithResponse(null, httpResponse);

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertEquals("Status 400: Bad Request, Oops not structural", capturedResponseBody);
  }

  @Test
  void validateDataset_transportTimeout_returns500WithTimeoutMsg() {
    failRequestWith(new TimeoutException("took too long"));

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertTrue(capturedResponseBody.startsWith("Validation service timed out:"));
  }

  @Test
  void validateDataset_transportUnreachable_returns500WithUnreachableMsg() {
    failRequestWith(new ConnectException("connection refused"));

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertTrue(capturedResponseBody.startsWith("Validation service unreachable:"));
  }

  @Test
  void validateDataset_unknownHost_returns500WithUnreachableMsg() {
    failRequestWith(new UnknownHostException("no such host"));

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertTrue(capturedResponseBody.startsWith("Validation service unreachable:"));
  }

  @Test
  void validateDataset_transportUnexpectedError_returns500WithUnexpectedMsg() {
    failRequestWith(new RuntimeException("boom"));

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertEquals(
      "Unexpected error while communicating with validation service: boom",
      capturedResponseBody
    );
  }

  @Test
  void validateDataset_transportFailedWithNullCause_returns500WithUnknownMsg() {
    failRequestWith(null);

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertEquals(
      "Unknown error while communicating with validation service",
      capturedResponseBody
    );
  }

  @Test
  void validateDataset_parseFailure_returns500WithBodyEcho() {
    String nonJson = "this is not json";
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.statusMessage()).thenReturn("OK");
    when(httpResponse.bodyAsString()).thenReturn(nonJson);
    // bodyAsJsonObject throws DecodeException
    when(httpResponse.bodyAsJsonObject()).thenThrow(new DecodeException("boom"));

    completeRequestWithResponse(null, httpResponse);

    client.validateDataset(routingContext);

    assertEquals(500, capturedStatusCode);
    assertTrue(capturedResponseBody.startsWith("Failed to parse response from validation service: "));
    assertTrue(capturedResponseBody.contains(nonJson));
  }

  @Test
  void validateDataset_sendsExpectedRequestPayload() {
    // Create a minimal valid JSON response so the call completes normally
    JsonObject body = new JsonObject().put("ok", true);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.statusMessage()).thenReturn("OK");
    when(httpResponse.bodyAsString()).thenReturn(body.encode());
    when(httpResponse.bodyAsJsonObject()).thenReturn(body);

    // Capture and verify the sent JsonObject inside helper
    JsonObject expectedSent = new JsonObject()
      .put("embeddingMethod", "BASE64")
      .put("contentSyntax", "text/turtle")
      .put("validationType", "public")
      .put("reportSyntax", "application/json");

    completeRequestWithResponse(expectedSent, httpResponse);

    client.validateDataset(routingContext);

    // Also verify we set a timeout on the request
    verify(httpRequest).timeout(eq(10_000L));
  }
}
