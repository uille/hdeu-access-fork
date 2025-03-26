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
package eu.europa.data.health.access;

import static org.mockito.ArgumentMatchers.anyString;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Objects;
import org.mockito.Mockito;

public class RoutingContextMocker {

  public static RoutingContext mockRoutingContext(JsonObject body) {
    // Mock the RoutingContext
    RoutingContext context = Mockito.mock(RoutingContext.class);
    HttpServerResponse response = Mockito.mock(HttpServerResponse.class);
    // Mock the RequestBody
    RequestBody requestBody = Mockito.mock(RequestBody.class);
    HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
    Mockito.when(context.request()).thenReturn(request);
    // When context.body() is called, return the mocked RequestBody object
    Mockito.when(context.body()).thenReturn(requestBody);
    // When context.body().asString() is called on the requestBody, return the provided string
    // object
    Mockito.when(context.body().asString()).thenReturn(body.toString());
    // When asJsonObject() is called on the requestBody, return the provided JSON object
    Mockito.when(requestBody.asJsonObject()).thenReturn(body);
    // Mock HttpServerResponse's setStatusCode() and end() methods
    Mockito.when(response.setStatusCode(Mockito.anyInt())).thenAnswer(invocation -> {
      int statusCode = invocation.getArgument(0);
      Mockito.when(response.getStatusCode()).thenReturn(statusCode);
      return response;
    });
    // Mock end() to capture the message
    Mockito.when(response.end(anyString())).thenAnswer(invocation -> {
      String message = invocation.getArgument(0);
      Mockito.when(response.getStatusMessage()).thenReturn(message);
      return Future.succeededFuture();
    });
    // Mock the response object to return the mock response
    Mockito.when(context.response()).thenReturn(response);

    // Mock the User object from vertx-auth-common
    User mockUser = Mockito.mock(User.class);
    String ecasId = "1eadc727-9da3-44e9-80a0-867d291f3bf7";
    // Define rules
    Mockito.when(context.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("sub", ecasId);
    Mockito.when(context.user().principal()).thenReturn(principal);
    Mockito.when(mockUser.get("sub")).thenReturn(ecasId);

    return context;
  }

  public static RoutingContext mockRoutingContext(JsonObject body,
    Map<String, String> pathParams, boolean getResponse) {
    // Mock the RoutingContext
    RoutingContext context = Mockito.mock(RoutingContext.class);
    HttpServerResponse response = Mockito.mock(HttpServerResponse.class);
    // Mock the response object to return the mock response
    Mockito.when(context.response()).thenReturn(response);
    if (Objects.nonNull(body)) {
      // Mock the RequestBody
      RequestBody requestBody = Mockito.mock(RequestBody.class);
      // When context.body() is called, return the mocked RequestBody object
      Mockito.when(context.body()).thenReturn(requestBody);
      Mockito.when(requestBody.asJsonObject()).thenReturn(body);
    }
    // Mock path parameters
    if (pathParams != null) {
      pathParams.forEach((key, value) -> Mockito.when(context.pathParam(key)).thenReturn(value));
    } else {
      // Default behavior if no path parameters provided
      Mockito.when(context.pathParam(anyString())).thenReturn(null);
    }
    // Properly mock response behavior
    Mockito.when(context.response()).thenReturn(response);
    Mockito.when(response.putHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(response);
    //make a conflict with export services where they are no need those
    if (getResponse) {
      Mockito.when(response.setStatusCode(Mockito.anyInt())).thenReturn(response);
      Mockito.when(response.end(Mockito.anyString())).thenReturn(Future.succeededFuture());
    }
    Mockito.when(response.end(Mockito.any(Buffer.class))).thenReturn(Future.succeededFuture());

    return context;
  }

}
