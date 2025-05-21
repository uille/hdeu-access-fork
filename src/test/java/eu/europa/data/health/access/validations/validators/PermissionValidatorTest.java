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

import ch.qos.logback.classic.Level;
import io.piveau.security.PiveauAuth;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.MultiMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.mockito.Mockito.*;

class PermissionValidatorTest {

  private RoutingContext context;
  private User user;

  @BeforeEach
  void setup() {
    context = mock(RoutingContext.class);
    user = mock(User.class);
    MultiMap queryParams = mock(MultiMap.class);

    when(context.user()).thenReturn(user);
    when(context.queryParams()).thenReturn(queryParams);
    when(queryParams.get("catalog")).thenReturn("test-catalog");

    Logger mockLogger = LoggerFactory.getLogger(PermissionValidator.class);
    ((ch.qos.logback.classic.Logger) mockLogger).setLevel(Level.TRACE);


    // mock a simple principal
    when(user.principal()).thenReturn(io.vertx.core.json.JsonObject.of("roles", List.of("read")));
  }

  @Test
  void shouldProceed_whenUserHasAllRoles() {
    List<String> scopes = List.of("read", "write");

    try (MockedStatic<PiveauAuth> authMock = Mockito.mockStatic(PiveauAuth.class)) {
      for (String scope : scopes) {
        authMock.when(() -> PiveauAuth.userHasRole(user, scope)).thenReturn(true);
      }

      PermissionValidator validator = new PermissionValidator(scopes);
      validator.handle(context);

      verify(context).next();
    }
  }

  @Test
  void shouldFail_whenUserHasNoPermissionOrRole() {
    List<String> scopes = List.of("admin");

    try (MockedStatic<PiveauAuth> authMock = Mockito.mockStatic(PiveauAuth.class)) {
      authMock.when(() -> PiveauAuth.userHasRole(user, "admin")).thenReturn(false);
      authMock.when(() -> PiveauAuth.userHasPermission(user, "test-catalog", "admin")).thenReturn(false);

      PermissionValidator validator = new PermissionValidator(scopes);
      validator.handle(context);

      verify(context).fail(403);
    }
  }

  @Test
  void shouldProceed_whenUserHasAllPermissions() {
    List<String> scopes = List.of("read", "write");

    try (MockedStatic<PiveauAuth> authMock = Mockito.mockStatic(PiveauAuth.class)) {
      for (String scope : scopes) {
        authMock.when(() -> PiveauAuth.userHasRole(user, scope)).thenReturn(false);
        authMock.when(() -> PiveauAuth.userHasPermission(user, "test-catalog", scope)).thenReturn(true);
      }

      PermissionValidator validator = new PermissionValidator(scopes);
      validator.handle(context);

      verify(context).next();
    }
  }
}

