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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;


import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractSchemaValidatorTest {

  private AbstractSchemaValidator validator;

  @BeforeEach
  void setUp() {
    validator = new AbstractSchemaValidator() {}; // Anonymous subclass to test abstract class
  }

  @Test
  void testGetFileFromResourceAsStream_validFile() {
    String fileName = "schema_validators/basketSchema.json";
    JsonObject result = validator.getFileFromResourceAsStream(fileName);
    assertNotNull(result);
    assertTrue(result.containsKey("type")); // Assuming your schema has a 'type' field
  }

  @Test
  void testGetFileFromResourceAsStream_invalidFile() {
    JsonObject result = validator.getFileFromResourceAsStream("nonexistent.json");
    assertNull(result);
  }

  @Test
  void testRequestBodyIsEmpty_nullBody() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.body()).thenReturn(null);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);
    assertTrue(validator.requestBodyIsEmpty(context));
    verify(context.response()).setStatusCode(400);
  }

  @Test
  void testRequestBodyIsEmpty_emptyBody() {
    RoutingContext context = mock(RoutingContext.class);
    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);
    when(body.isEmpty()).thenReturn(true);
    assertTrue(validator.requestBodyIsEmpty(context));
    verify(context.response()).setStatusCode(400);
  }

  @Test
  void testRequestBodyIsEmpty_validBody() {
    RoutingContext context = mock(RoutingContext.class);
    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.isEmpty()).thenReturn(false);
    assertFalse(validator.requestBodyIsEmpty(context));
  }

  @Test
  void testSetUpMessageContextResponse() {
    RoutingContext context = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);
    validator.setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, "Invalid input");

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(contains("Invalid input"));
  }

  @Test
  void testSetUpMessageContextResponseWithContent() {
    RoutingContext context = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);


    JsonArray content = new JsonArray().add("field1 missing").add("invalid format");
    validator.setUpMessageContextResponseWithContent(context, HttpResponseStatus.BAD_REQUEST, "Validation errors", content);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(contains("Validation errors"));
    verify(response).end(contains("field1 missing"));
  }



  @Test
  void testValidateJsonSchemaSuccess() {
    RoutingContext context = mock(RoutingContext.class);
    RequestBody body = mock(RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asString()).thenReturn("{\"name\": \"Test\"}");
    when(body.asJsonObject()).thenReturn(new JsonObject().put("name", "Test"));
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);
    String fileName = "basketSchema.json";

    validator.validateJsonSchema(context, fileName);
    verify(context.response(), times(1)).setStatusCode(anyInt());
  }

}

