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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;

class ContextTest {

  @Test void testGetEcasId() {
    // Mocks
    RoutingContext routingContext = mock(RoutingContext.class);
    User user = mock(User.class);
    JsonObject principal = mock(JsonObject.class);

    // Behavior setup
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    when(principal.getString("preferred_username")).thenReturn("ecas_user123");

    // Call and assert
    String result = Context.getEcasId(routingContext);
    assertEquals("ecas_user123", result);
  }

  @Test void testGetUserNameReturnsGivenAndFamilyName() {
    RoutingContext routingContext = mock(RoutingContext.class);
    User user = mock(User.class);
    JsonObject principal = new JsonObject()
      .put("given_name", "Maria")
      .put("family_name", "Papadopoulou")
      .put("preferred_username", "mpapa");

    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);

    String result = Context.getUserName(routingContext);
    assertEquals("Maria Papadopoulou", result);
  }

  @Test void testGetUserNameFallsBackToSingleAvailableNamePart() {
    RoutingContext routingContext = mock(RoutingContext.class);
    User user = mock(User.class);
    JsonObject principal = new JsonObject()
      .put("given_name", "Maria")
      .put("family_name", "   ")
      .put("preferred_username", "mpapa");

    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);

    String result = Context.getUserName(routingContext);
    assertEquals("Maria", result);
  }

  @Test void testGetUserNameFallsBackToPreferredUsername() {
    RoutingContext routingContext = mock(RoutingContext.class);
    User user = mock(User.class);
    JsonObject principal = new JsonObject()
      .put("given_name", "   ")
      .put("family_name", null)
      .put("preferred_username", "ecas_user123");

    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);

    String result = Context.getUserName(routingContext);
    assertEquals("ecas_user123", result);
  }
}
