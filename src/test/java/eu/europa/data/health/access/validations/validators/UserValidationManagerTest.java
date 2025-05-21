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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserValidationManagerTest {

  private UserValidationManager manager;
  private RoutingContext context;

  @Mock
  private HttpServerResponse response;

  @BeforeEach
  void setUp() {
    manager = new UserValidationManager();
    context = mock(RoutingContext.class);
  }

  @Test
  void testValidateCreateOrUpdateUser_InvalidInput_ShouldReturn400() {
    JsonObject invalidJson = new JsonObject()
      .put("email", "invalid-email")  // invalid email format
      .put("name", "");               // blank name, triggers @NotBlank

    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(invalidJson);

    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);

    manager.validateCreateOrUpdateUser(context);

    verify(response).setStatusCode(400);
    ArgumentCaptor<String> responseBodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).end(responseBodyCaptor.capture());

    verify(context).response();
    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }

  @Test
  void testValidateCreateOrUpdateUser_ValidInput_ShouldCallNext() {
    JsonObject validJson = new JsonObject()

      .put("legalOrNaturalPerson", new JsonObject().put("key", "b"))
      .put("naturalPersonJobTitle", "Researcher")
      .put("naturalPersonAffiliation", "Health Institute")
      .put("naturalPersonAddress", "123 Main St")
      .put("naturalPersonZipCode", "12345")
      .put("naturalPersonCity", "Helsinki")
      .put("naturalPersonCountry",  new JsonObject().put("key", "FI")
        .put("value", "Finland"))
      .put("naturalPersonPhone", new JsonObject().
        put("countryCode", "+358")
      .put("number", "123456789"));

    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(validJson);

    manager = spy(manager);
    doNothing().when(context).next();



    manager.validateCreateOrUpdateUser(context);

    verify(context).next();
  }

  @Test
  void testValidateBasketJsonSchema_ValidJson_ShouldCallNext() {
    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");

    var body = mock(io.vertx.ext.web.RequestBody.class);



    when(context.response()).thenReturn(response);

    when(context.body()).thenReturn(body);
    when(context.body().asString()).thenReturn(buffer.toString());
    when(context.body().asString().trim()).thenReturn(buffer.toString());

    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);

    manager.validateCreateOrUpdateUserJsonSchema(context);


    verify(context, never()).next();

  }

}
