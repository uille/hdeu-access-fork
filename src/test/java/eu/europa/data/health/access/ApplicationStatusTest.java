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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationStatusTest {

  @Test
  void testGetStatus() {
    assertEquals("draft", ApplicationStatus.DRAFT.getStatus());
    assertEquals("submitted", ApplicationStatus.SUBMITTED.getStatus());
    assertEquals("withdrawn", ApplicationStatus.WITHDRAWN.getStatus());
  }

  @Test
  void testIsValid() {
    assertTrue(ApplicationStatus.isValid("DRAFT"));
    assertTrue(ApplicationStatus.isValid("submitted"));
    assertTrue(ApplicationStatus.isValid("WITHDRAWN"));
    assertFalse(ApplicationStatus.isValid("unknown"));
    assertFalse(ApplicationStatus.isValid(null));
  }

  @Test
  void testIsValidStatus() {
    assertTrue(ApplicationStatus.isValidStatus("draft"));
    assertTrue(ApplicationStatus.isValidStatus("submitted"));
    assertTrue(ApplicationStatus.isValidStatus("withdrawn"));
    assertFalse(ApplicationStatus.isValidStatus("unknown"));
    assertFalse(ApplicationStatus.isValidStatus(null));
  }

  @Test
  void testFromStatus() {
    assertEquals(ApplicationStatus.DRAFT, ApplicationStatus.fromStatus("draft"));
    assertEquals(ApplicationStatus.SUBMITTED, ApplicationStatus.fromStatus("submitted"));
    assertEquals(ApplicationStatus.WITHDRAWN, ApplicationStatus.fromStatus("withdrawn"));
  }

  @Test
  void testFromStatusThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ApplicationStatus.fromStatus("unknown");
    });
    assertEquals("No enum constant with status unknown", exception.getMessage());
  }

  @Test
  void testCaseInsensitiveMatching() {
    assertTrue(ApplicationStatus.isValid("Draft"));
    assertEquals(ApplicationStatus.DRAFT, ApplicationStatus.fromStatus("Draft"));

    assertTrue(ApplicationStatus.isValid("SUBMITTED"));
    assertEquals(ApplicationStatus.SUBMITTED, ApplicationStatus.fromStatus("SUBMITTED"));

    assertTrue(ApplicationStatus.isValid("WiThDrAwN"));
    assertEquals(ApplicationStatus.WITHDRAWN, ApplicationStatus.fromStatus("WiThDrAwN"));
  }
}
