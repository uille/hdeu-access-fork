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
package eu.europa.data.health.access.validations.validators;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.europa.data.health.access.SchemaFiles;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DispatcherValidationManagerTest {

  private DispatcherValidationManager manager;
  private RoutingContext ctx;
  private HttpServerResponse response;

  @BeforeEach
  void setUp() {
    manager = spy(new DispatcherValidationManager());

    ctx = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);

    // IMPORTANT: prevent NPE inside validateJsonSchema(...)
    when(ctx.response()).thenReturn(response);

    // common Vert.x response chaining
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
  }

  @Test
  void validateDispatcherDecisionJsonSchema_shouldUseDecisionSchema() {
    manager.validateDispatcherDecisionJsonSchema(ctx);

    verify(manager).validateJsonSchema(
      same(ctx),
      eq(SchemaFiles.DISPATCHER_DECISION_SCHEMA_FILE.getValue())
    );
  }

  @Test
  void validateDispatcherStatusJsonSchema_shouldUseStatusSchema() {
    manager.validateDispatcherStatusJsonSchema(ctx);

    verify(manager).validateJsonSchema(
      same(ctx),
      eq(SchemaFiles.DISPATCHER_STATUS_SCHEMA_FILE.getValue())
    );
  }

  @Test
  void validateDispatcherPermitJsonSchema_shouldUsePermitSchema() {
    manager.validateDispatcherPermitJsonSchema(ctx);

    verify(manager).validateJsonSchema(
      same(ctx),
      eq(SchemaFiles.PERMIT_STATUS_SCHEMA_FILE.getValue())
    );
  }
}

