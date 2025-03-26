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
package eu.europa.data.health.access.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UserProfileErrorTest {

  @Test
  void testAllArgsConstructor() {
    // Arrange
    String code = "ERR001";
    String errorMessage = "Invalid user input";
    String field = "username";
    List<String> params = List.of("param1", "param2");

    // Act
    UserProfileError userProfileError = new UserProfileError(code, errorMessage, field, params);

    // Assert
    assertEquals(code, userProfileError.getCode());
    assertEquals(errorMessage, userProfileError.getErrorMessage());
    assertEquals(field, userProfileError.getField());
    assertEquals(params, userProfileError.getParams());
  }

  @Test
  void testSettersAndGetters() {
    // Arrange
    UserProfileError userProfileError = new UserProfileError(null, null, null, null);
    String code = "ERR002";
    String errorMessage = "Missing required field";
    String field = "email";
    List<String> params = List.of("paramA", "paramB");

    // Act
    userProfileError.setCode(code);
    userProfileError.setErrorMessage(errorMessage);
    userProfileError.setField(field);
    userProfileError.setParams(params);

    // Assert
    assertEquals(code, userProfileError.getCode());
    assertEquals(errorMessage, userProfileError.getErrorMessage());
    assertEquals(field, userProfileError.getField());
    assertEquals(params, userProfileError.getParams());
  }

  @Test
  void testNullParams() {
    // Arrange
    String code = "ERR003";
    String errorMessage = "No parameters provided";
    String field = "phone";

    // Act
    UserProfileError userProfileError = new UserProfileError(code, errorMessage, field, null);

    // Assert
    assertEquals(code, userProfileError.getCode());
    assertEquals(errorMessage, userProfileError.getErrorMessage());
    assertEquals(field, userProfileError.getField());
    assertNull(userProfileError.getParams());
  }

  @Test
  void testEmptyParams() {
    // Arrange
    String code = "ERR004";
    String errorMessage = "Empty parameters";
    String field = "address";
    List<String> params = List.of();

    // Act
    UserProfileError userProfileError = new UserProfileError(code, errorMessage, field, params);

    // Assert
    assertEquals(code, userProfileError.getCode());
    assertEquals(errorMessage, userProfileError.getErrorMessage());
    assertEquals(field, userProfileError.getField());
    assertTrue(userProfileError.getParams().isEmpty());
  }
}

