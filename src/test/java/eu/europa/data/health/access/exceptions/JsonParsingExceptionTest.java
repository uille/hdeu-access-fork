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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JsonParsingExceptionTest {

  @Test
  void testExceptionWithMessageAndCause() {
    String errorMessage = "Error parsing JSON";
    Throwable cause = new IllegalArgumentException("Invalid JSON format");

    JsonParsingException exception = new JsonParsingException(errorMessage, cause);

    // Verify the message is correctly set
    assertEquals(errorMessage, exception.getMessage());

    // Verify the cause is correctly set
    assertEquals(cause, exception.getCause());
  }

  @Test
  void testExceptionWithoutCause() {
    String errorMessage = "Error parsing JSON";

    JsonParsingException exception = new JsonParsingException(errorMessage, null);

    // Verify the message is correctly set
    assertEquals(errorMessage, exception.getMessage());

    // Verify the cause is null
    assertNull(exception.getCause());
  }

  @Test
  void testExceptionWithNullMessageAndCause() {
    JsonParsingException exception = new JsonParsingException(null, null);

    // Verify the message is null
    assertNull(exception.getMessage());

    // Verify the cause is null
    assertNull(exception.getCause());
  }

  @Test
  void testExceptionIsRuntimeException() {
    JsonParsingException exception = new JsonParsingException("Test", null);

    // Verify the exception is an instance of RuntimeException
    assertInstanceOf(RuntimeException.class, exception);
  }
}
