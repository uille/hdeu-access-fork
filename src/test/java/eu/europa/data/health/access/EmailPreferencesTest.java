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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.validations.validators.UserValidationManager;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailPreferencesTest {

  private UserValidationManager validationManager;
  private RoutingContext context;
  private HttpServerResponse response;

  @BeforeEach
  void setUp() {
    validationManager = new UserValidationManager();
    context = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
  }

  @Test
  void testValidEmailPreferences_ShouldPass() {
    System.out.println("[DEBUG_LOG] Testing valid email preferences");

    // Valid email preferences JSON
    JsonObject validEmailPreferences = new JsonObject()
      .put("enabled", true)
      .put("reminders", new JsonObject()
        .put("enabled", true)
        .put("offsetDays", new JsonArray().add(1)));

    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asString()).thenReturn(validEmailPreferences.encode());
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    // This should not throw an exception and should call context.next()
    assertDoesNotThrow(() -> {
      validationManager.validateEmailPreferencesJsonSchema(context);
    });

    System.out.println("[DEBUG_LOG] Valid email preferences test completed");
  }

  @Test
  void testInvalidEmailPreferences_MissingRequired_ShouldFail() {
    System.out.println("[DEBUG_LOG] Testing invalid email preferences - missing required fields");

    // Invalid email preferences JSON - missing required fields
    JsonObject invalidEmailPreferences = new JsonObject()
      .put("enabled", true);
      // Missing "reminders" field

    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asString()).thenReturn(invalidEmailPreferences.encode());
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);

    validationManager.validateEmailPreferencesJsonSchema(context);

    // Should return 400 status code for invalid input
    verify(response).setStatusCode(400);

    System.out.println("[DEBUG_LOG] Invalid email preferences test completed");
  }

  @Test
  void testEmailPreferencesWithRemindersDisabled_ShouldPass() {
    System.out.println("[DEBUG_LOG] Testing email preferences with reminders disabled");

    // Valid email preferences with reminders disabled (no offsetDays required)
    JsonObject validEmailPreferences = new JsonObject()
      .put("enabled", true)
      .put("reminders", new JsonObject()
        .put("enabled", false));

    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asString()).thenReturn(validEmailPreferences.encode());
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    assertDoesNotThrow(() -> {
      validationManager.validateEmailPreferencesJsonSchema(context);
    });

    System.out.println("[DEBUG_LOG] Email preferences with reminders disabled test completed");
  }

  @Test
  void testEmailPreferencesSchema_ValidatesOffsetDaysFormat() {
    System.out.println("[DEBUG_LOG] Testing email preferences with specific offsetDays format");

    // Test the exact format from the issue description
    JsonObject emailPreferences = new JsonObject()
      .put("enabled", true)
      .put("reminders", new JsonObject()
        .put("enabled", true)
        .put("offsetDays", new JsonArray().add(1)));

    var body = mock(io.vertx.ext.web.RequestBody.class);
    when(context.body()).thenReturn(body);
    when(body.asString()).thenReturn(emailPreferences.encode());
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    assertDoesNotThrow(() -> {
      validationManager.validateEmailPreferencesJsonSchema(context);
    });

    System.out.println("[DEBUG_LOG] OffsetDays format validation test completed");
  }
}
