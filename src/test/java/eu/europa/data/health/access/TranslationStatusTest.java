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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TranslationStatusTest {

//  @Test
//  void getStatus_returnsExpectedStrings() {
//    assertEquals("pending", TranslationStatus.PENDING.getStatus());
//    assertEquals("completed", TranslationStatus.COMPLETED.getStatus());
//    assertEquals("partially_completed", TranslationStatus.PARTIALLY_COMPLETED.getStatus());
//    assertEquals("not_required", TranslationStatus.NOT_REQUIRED.getStatus());
//    assertEquals("failed", TranslationStatus.FAILED.getStatus());
//  }

  @Test
  void isValidStatus_acceptsKnownValues_caseInsensitive() {
    // lower
    assertTrue(TranslationStatus.isValidStatus("pending"));
    assertTrue(TranslationStatus.isValidStatus("completed"));
    assertTrue(TranslationStatus.isValidStatus("partially_completed"));
    assertTrue(TranslationStatus.isValidStatus("not_started"));
    assertTrue(TranslationStatus.isValidStatus("failed"));
    // upper
    assertTrue(TranslationStatus.isValidStatus("PENDING"));
    assertTrue(TranslationStatus.isValidStatus("COMPLETED"));
    assertTrue(TranslationStatus.isValidStatus("PARTIALLY_COMPLETED"));
    assertTrue(TranslationStatus.isValidStatus("NOT_STARTED"));
    assertTrue(TranslationStatus.isValidStatus("FAILED"));
    // mixed
    assertTrue(TranslationStatus.isValidStatus("PeNdInG"));
    assertTrue(TranslationStatus.isValidStatus("PaRtIaLlY_CoMpLeTeD"));
  }

  @Test
  void isValidStatus_rejectsUnknownEmptyAndNull() {
    assertFalse(TranslationStatus.isValidStatus("done"));
    assertFalse(TranslationStatus.isValidStatus("partially-completed")); // hyphen != underscore
    assertFalse(TranslationStatus.isValidStatus(" pending "));           // spaces not trimmed
    assertFalse(TranslationStatus.isValidStatus(""));
    assertFalse(TranslationStatus.isValidStatus(null));                  // equalsIgnoreCase handles null -> false
  }

//  @Test
//  void fromStatus_returnsEnum_caseInsensitive() {
//    assertEquals(TranslationStatus.PENDING, TranslationStatus.fromStatus("pending"));
//    assertEquals(TranslationStatus.COMPLETED, TranslationStatus.fromStatus("COMPLETED"));
//    assertEquals(TranslationStatus.PARTIALLY_COMPLETED, TranslationStatus.fromStatus("Partially_Completed"));
//    assertEquals(TranslationStatus.NOT_REQUIRED, TranslationStatus.fromStatus("NOT_REQUIRED"));
//    assertEquals(TranslationStatus.FAILED, TranslationStatus.fromStatus("FaIlEd"));
//  }

  @Test
  void fromStatus_throwsForInvalidOrNull() {
    assertThrows(IllegalArgumentException.class, () -> TranslationStatus.fromStatus("unknown"));
    assertThrows(IllegalArgumentException.class, () -> TranslationStatus.fromStatus(""));
    assertThrows(IllegalArgumentException.class, () -> TranslationStatus.fromStatus(null));
  }
}
