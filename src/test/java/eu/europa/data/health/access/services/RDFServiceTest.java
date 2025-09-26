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

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.RequestBody;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpHeaders;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RDFServiceTest {

  private RDFService service;

  private RoutingContext ctx;
  private RequestBody requestBody;
  private HttpServerResponse response;

  private Integer capturedStatus;
  private String capturedBody;

  @BeforeEach
  void setUp() {
    service = new RDFService();

    ctx = mock(RoutingContext.class);
    requestBody = mock(RequestBody.class);
    response = mock(HttpServerResponse.class);

    when(ctx.body()).thenReturn(requestBody);
    when(ctx.response()).thenReturn(response);

    // Chainable putHeader
    when(response.putHeader(any(CharSequence.class), any(CharSequence.class)))
      .thenReturn(response);

    // Capture status
    capturedStatus = null;
    when(response.setStatusCode(anyInt())).thenAnswer(inv -> {
      capturedStatus = inv.getArgument(0);
      return response;
    });

    // Capture body
    doAnswer(inv -> {
      capturedBody = inv.getArgument(0);
      return null;
    }).when(response).end(anyString());
  }

  @Test
  void convertRDF_success_Turtle_to_RdfXml() {
    // Valid Turtle
    String ttl = """
      @prefix ex: <http://example.org/> .
      ex:s ex:p "v" .
      """;
    when(requestBody.asString()).thenReturn(ttl);

    service.convertRDF(ctx, Lang.TURTLE, Lang.RDFXML);

    // Default 200 (no explicit status set)
    assertNull(capturedStatus);

    // Content-Type header set to RDF/XML
    verify(response).putHeader(eq(HttpHeaders.CONTENT_TYPE),
      eq(Lang.RDFXML.getContentType().getContentTypeStr()));

    // Parse the RDF/XML response and assert the triple exists
    assertNotNull(capturedBody);
    org.apache.jena.rdf.model.Model model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
    org.apache.jena.riot.RDFDataMgr.read(model, new java.io.StringReader(capturedBody), null, Lang.RDFXML);

    assertTrue(model.contains(
      model.createResource("http://example.org/s"),
      model.createProperty("http://example.org/p"),
      model.createLiteral("v")
    ));
  }

  @Test
  void convertRDF_emptyPayload_returns400() {
    when(requestBody.asString()).thenReturn(""); // blank
    service.convertRDF(ctx, Lang.TURTLE, Lang.RDFXML);

    assertEquals(400, capturedStatus);
    assertEquals("Empty Turtle payload", capturedBody);
  }

  @Test
  void convertRDF_invalidSyntax_returns400WithMessage() {
    // Intentionally invalid Turtle so Jena throws RiotException
    when(requestBody.asString()).thenReturn("this is not valid turtle at all");

    service.convertRDF(ctx, Lang.TURTLE, Lang.RDFXML);

    assertEquals(400, capturedStatus);
    assertNotNull(capturedBody);
    assertTrue(capturedBody.startsWith("Invalid syntax: "));
  }

  @Test
  void convertRDF_nullOutputFormat_treatedAsSyntaxError_returns400() {
    when(requestBody.asString()).thenReturn("""
      @prefix ex: <http://example.org/> .
      ex:s ex:p "v" .
  """);

    // This causes Jena to throw a Riot* exception -> your code returns 400 with "Invalid syntax: ..."
    service.convertRDF(ctx, org.apache.jena.riot.Lang.TURTLE, /* outputFormat */ null);

    assertEquals(400, capturedStatus);
    assertNotNull(capturedBody);
    assertTrue(capturedBody.startsWith("Invalid syntax: "));
    // Be tolerant to Jena’s wording across versions:
    assertTrue(capturedBody.contains("No output format"), "Expected 'No output format' in message");
  }

  @Test
  void convertRDF_unexpectedNonRiotError_logsErrorAndReturns500() {
    // Capture ERROR log
    Logger logger = (Logger) LoggerFactory.getLogger(RDFService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    when(requestBody.asString()).thenReturn("""
      @prefix ex: <http://example.org/> .
      ex:s ex:p "v" .
  """);

    // Force a NON-Riot exception inside the try block after successful parse/write
    when(response.putHeader(any(CharSequence.class), any(CharSequence.class)))
      .thenThrow(new RuntimeException("boom"));

    // Use valid formats so Jena does NOT throw Riot
    service.convertRDF(ctx, org.apache.jena.riot.Lang.TURTLE, org.apache.jena.riot.Lang.RDFXML);

    assertEquals(500, capturedStatus);
    assertEquals("Conversion error", capturedBody);

    boolean sawError = appender.list.stream().anyMatch(e ->
      e.getLevel() == Level.ERROR &&
        e.getFormattedMessage().startsWith("Failed to convert RDF"));
    logger.detachAppender(appender);
    assertTrue(sawError, "Expected ERROR log 'Failed to convert RDF' was not emitted");
  }

  @Test
  void convertRDF_unexpectedError_logsErrorAndReturns500() {
    // Attach a Logback appender to assert the ERROR log
    ch.qos.logback.classic.Logger logger =
      (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RDFService.class);
    ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
      new ch.qos.logback.core.read.ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    // Valid payload so read/write would normally succeed
    when(requestBody.asString()).thenReturn("""
      @prefix ex: <http://example.org/> .
      ex:s ex:p "v" .
      """);

    // FORCE a non-Riot exception inside the try block
    // (overrides the default stub from setUp)
    when(response.putHeader(any(CharSequence.class), any(CharSequence.class)))
      .thenThrow(new RuntimeException("boom"));

    // Use valid formats so Jena doesn't throw a RiotException
    service.convertRDF(ctx, org.apache.jena.riot.Lang.TURTLE, org.apache.jena.riot.Lang.RDFXML);

    // Assert the 500 branch
    assertEquals(500, capturedStatus);
    assertEquals("Conversion error", capturedBody);

    // Assert the ERROR log was emitted
    boolean sawError = appender.list.stream().anyMatch(e ->
      e.getLevel() == ch.qos.logback.classic.Level.ERROR &&
        e.getFormattedMessage().startsWith("Failed to convert RDF"));
    logger.detachAppender(appender);
    assertTrue(sawError, "Expected ERROR log 'Failed to convert RDF' was not emitted");
  }
}
