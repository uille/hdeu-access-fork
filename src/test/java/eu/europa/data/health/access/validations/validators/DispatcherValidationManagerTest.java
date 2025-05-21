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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class DispatcherValidationManagerTest {


  @BeforeEach
  void setUp() {
    manager = new DispatcherValidationManager();
  }

  private DispatcherValidationManager manager;


  @Mock
  private HttpServerResponse response;

  @Test
  void testValidateSearchCatalogueJsonSchema_invokesValidator() {
    RoutingContext context = mock(RoutingContext.class);
    RequestBody requestBody = Mockito.mock(RequestBody.class);

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(context.response()).thenReturn(response);

    when(context.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);

    when(context.body()).thenReturn(requestBody);
    manager.validateDispatcherDecisionJsonSchema(context);
    verify(context, times(2)).response();
  }




}
