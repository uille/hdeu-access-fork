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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.LoggerFactory;

public class DynamicLogLevelHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext routingContext) {
    String loggerName = routingContext.request().getParam("logger");
    String level = routingContext.request().getParam("level");

    if (loggerName == null || level == null) {
      routingContext.response().setStatusCode(400).end("Missing 'logger' or 'level' parameter");
      return;
    }

    try {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);

      if (logger != null) {
        if (!isValidLogLevel(level.toUpperCase())) {
          routingContext.response().setStatusCode(400).end("Invalid log level: " + level);
          return;
        }
        Level newLevel = Level.valueOf(level.toUpperCase());
        logger.setLevel(newLevel);
        routingContext.response().end("Log level for " + loggerName + " changed to " + level);
      } else {
        routingContext.response().setStatusCode(404).end("Logger not found: " + loggerName);
      }
    } catch (Exception e) {
      routingContext.response().setStatusCode(500)
        .end("Error changing log level: " + e.getMessage());
    }
  }

  private boolean isValidLogLevel(String level) {
    return level.equals("TRACE") || level.equals("DEBUG") || level.equals("INFO") || level.equals(
      "WARN") || level.equals("ERROR") || level.equals("OFF");
  }
}
