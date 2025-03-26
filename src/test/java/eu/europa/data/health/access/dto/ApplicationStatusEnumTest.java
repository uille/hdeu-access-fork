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
package eu.europa.data.health.access.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ApplicationStatusEnumTest {

  @Test
  void testEnumValues() {
    ApplicationStatusEnum[] values = ApplicationStatusEnum.values();

    // Verify that the enum values are as expected
    assertEquals(3, values.length, "The enum should have 3 values.");
    assertEquals(ApplicationStatusEnum.DRAFT, values[0]);
    assertEquals(ApplicationStatusEnum.SUBMITTED, values[1]);
    assertEquals(ApplicationStatusEnum.WITHDRAWN, values[2]);
  }

  @Test
  void testEnumOrdinal() {
    // Verify the ordinal positions of each enum value
    assertEquals(0, ApplicationStatusEnum.DRAFT.ordinal());
    assertEquals(1, ApplicationStatusEnum.SUBMITTED.ordinal());
    assertEquals(2, ApplicationStatusEnum.WITHDRAWN.ordinal());
  }

  @Test
  void testEnumName() {
    // Verify that the name of each enum value is correct
    assertEquals("DRAFT", ApplicationStatusEnum.DRAFT.name());
    assertEquals("SUBMITTED", ApplicationStatusEnum.SUBMITTED.name());
    assertEquals("WITHDRAWN", ApplicationStatusEnum.WITHDRAWN.name());
  }

  @Test
  void testInvalidEnumValue() {
    // Test with an invalid string that doesn't correspond to any enum value
    String invalidValue = "INVALID";

    // Verify that an exception is thrown when trying to convert an invalid string to an enum
    assertThrows(IllegalArgumentException.class, () -> {
      ApplicationStatusEnum.valueOf(invalidValue);
    }, "An exception should be thrown when an invalid enum string is provided.");
  }

  @Test
  void testValidEnumValueFromString() {
    // Test valid strings for each enum value
    assertEquals(ApplicationStatusEnum.DRAFT, ApplicationStatusEnum.valueOf("DRAFT"));
    assertEquals(ApplicationStatusEnum.SUBMITTED, ApplicationStatusEnum.valueOf("SUBMITTED"));
    assertEquals(ApplicationStatusEnum.WITHDRAWN, ApplicationStatusEnum.valueOf("WITHDRAWN"));
  }

  @Test
  void testEnumToString() {
    // Test the string representation of each enum value
    assertEquals("DRAFT", ApplicationStatusEnum.DRAFT.toString());
    assertEquals("SUBMITTED", ApplicationStatusEnum.SUBMITTED.toString());
    assertEquals("WITHDRAWN", ApplicationStatusEnum.WITHDRAWN.toString());
  }
}

