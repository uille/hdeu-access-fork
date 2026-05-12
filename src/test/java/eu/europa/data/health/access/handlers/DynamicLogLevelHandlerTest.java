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
package eu.europa.data.health.access.handlers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

class DynamicLogLevelHandlerTest {

  private final DynamicLogLevelHandler handler = new DynamicLogLevelHandler();

  private RoutingContext ctxWithParams(String loggerName, String level) {
    RoutingContext ctx = mock(RoutingContext.class, Answers.RETURNS_DEEP_STUBS);

    HttpServerRequest req = mock(HttpServerRequest.class);
    HttpServerResponse resp = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(req);
    when(ctx.response()).thenReturn(resp);

    when(req.getParam("logger")).thenReturn(loggerName);
    when(req.getParam("level")).thenReturn(level);

    // Make fluent response calls safe
    when(resp.setStatusCode(anyInt())).thenReturn(resp);
    when(resp.end(anyString())).thenReturn(null);
    when(resp.end()).thenReturn(null);

    return ctx;
  }

  @Test
  void handle_whenMissingLoggerOrLevel_shouldReturn400() {
    RoutingContext ctx = ctxWithParams(null, "INFO");

    handler.handle(ctx);

    verify(ctx.request()).getParam("logger");
    verify(ctx.request()).getParam("level");

    verify(ctx.response()).setStatusCode(400);
    verify(ctx.response()).end("Missing 'logger' or 'level' parameter");

    verifyNoMoreInteractions(ctx.request());
  }

  @Test
  void handle_whenInvalidLogLevel_shouldReturn400() {
    RoutingContext ctx = ctxWithParams("com.acme.Test", "NOPE");

    LoggerContext loggerContext = mock(LoggerContext.class);
    ch.qos.logback.classic.Logger logger = mock(ch.qos.logback.classic.Logger.class);

    when(loggerContext.getLogger("com.acme.Test")).thenReturn(logger);

    try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
      mocked.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);

      handler.handle(ctx);

      verify(logger, never()).setLevel(any());
      verify(ctx.response()).setStatusCode(400);
      verify(ctx.response()).end("Invalid log level: NOPE");
    }
  }

  @Test
  void handle_whenValidLogLevel_shouldSetLevel_andReturn200WithMessage() {
    RoutingContext ctx = ctxWithParams("com.acme.Test", "debug");

    LoggerContext loggerContext = mock(LoggerContext.class);
    ch.qos.logback.classic.Logger logger = mock(ch.qos.logback.classic.Logger.class);

    when(loggerContext.getLogger("com.acme.Test")).thenReturn(logger);

    try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
      mocked.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);

      handler.handle(ctx);

      verify(logger).setLevel(Level.DEBUG);
      verify(ctx.response(), never()).setStatusCode(anyInt());
      verify(ctx.response()).end("Log level for com.acme.Test changed to debug");
    }
  }

  @Test
  void handle_whenLoggerNotFound_shouldReturn404() {
    RoutingContext ctx = ctxWithParams("missing.logger", "INFO");

    LoggerContext loggerContext = mock(LoggerContext.class);
    when(loggerContext.getLogger("missing.logger")).thenReturn(null);

    try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
      mocked.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);

      handler.handle(ctx);

      verify(ctx.response()).setStatusCode(404);
      verify(ctx.response()).end("Logger not found: missing.logger");
    }
  }

  @Test
  void handle_whenExceptionOccurs_shouldReturn500() {
    RoutingContext ctx = ctxWithParams("com.acme.Test", "INFO");

    try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
      mocked.when(LoggerFactory::getILoggerFactory).thenThrow(new RuntimeException("boom"));

      handler.handle(ctx);

      verify(ctx.response()).setStatusCode(500);
      verify(ctx.response()).end("Error changing log level: boom");
    }
  }
}
