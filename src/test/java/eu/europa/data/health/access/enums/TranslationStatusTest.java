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
package eu.europa.data.health.access.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TranslationStatusTest {

  // -------------------------------------------------------------------------
  // getStatus()
  // -------------------------------------------------------------------------
  @Test
  void getStatus_returnsExpectedStringForEachConstant() {
    assertEquals("completed", TranslationStatus.COMPLETED.getStatus());
    assertEquals("failed", TranslationStatus.FAILED.getStatus());
    assertEquals("pending", TranslationStatus.PENDING.getStatus());
    assertEquals("waiting", TranslationStatus.WAITING.getStatus());
  }

  @Test
  void values_containsExactlyFourConstants() {
    assertEquals(4, TranslationStatus.values().length);
  }

  // -------------------------------------------------------------------------
  // isValidStatus()
  // -------------------------------------------------------------------------
  @Test
  void isValidStatus_acceptsAllKnownValues_lowercase() {
    assertTrue(TranslationStatus.isValidStatus("completed"));
    assertTrue(TranslationStatus.isValidStatus("failed"));
    assertTrue(TranslationStatus.isValidStatus("pending"));
    assertTrue(TranslationStatus.isValidStatus("waiting"));
  }

  @Test
  void isValidStatus_acceptsAllKnownValues_uppercase() {
    assertTrue(TranslationStatus.isValidStatus("COMPLETED"));
    assertTrue(TranslationStatus.isValidStatus("FAILED"));
    assertTrue(TranslationStatus.isValidStatus("PENDING"));
    assertTrue(TranslationStatus.isValidStatus("WAITING"));
  }

  @Test
  void isValidStatus_acceptsAllKnownValues_mixedCase() {
    assertTrue(TranslationStatus.isValidStatus("Completed"));
    assertTrue(TranslationStatus.isValidStatus("FaIlEd"));
    assertTrue(TranslationStatus.isValidStatus("PeNdInG"));
    assertTrue(TranslationStatus.isValidStatus("WaItInG"));
  }

  @Test
  void isValidStatus_rejectsUnknownValues() {
    assertFalse(TranslationStatus.isValidStatus("done"));
    assertFalse(TranslationStatus.isValidStatus("processing"));
    assertFalse(TranslationStatus.isValidStatus("partially_completed"));
    assertFalse(TranslationStatus.isValidStatus("not_started"));
  }

  @Test
  void isValidStatus_rejectsEmptyString() {
    assertFalse(TranslationStatus.isValidStatus(""));
  }

  @Test
  void isValidStatus_rejectsStringsWithLeadingOrTrailingSpaces() {
    assertFalse(TranslationStatus.isValidStatus(" pending "));
    assertFalse(TranslationStatus.isValidStatus(" completed"));
    assertFalse(TranslationStatus.isValidStatus("failed "));
  }

  @Test
  void isValidStatus_rejectsNull() {
    assertFalse(TranslationStatus.isValidStatus(null));
  }

  // -------------------------------------------------------------------------
  // fromStatus()
  // -------------------------------------------------------------------------
  @Test
  void fromStatus_returnsCorrectEnum_lowercase() {
    assertEquals(TranslationStatus.COMPLETED, TranslationStatus.fromStatus("completed"));
    assertEquals(TranslationStatus.FAILED, TranslationStatus.fromStatus("failed"));
    assertEquals(TranslationStatus.PENDING, TranslationStatus.fromStatus("pending"));
    assertEquals(TranslationStatus.WAITING, TranslationStatus.fromStatus("waiting"));
  }

  @Test
  void fromStatus_returnsCorrectEnum_uppercase() {
    assertEquals(TranslationStatus.COMPLETED, TranslationStatus.fromStatus("COMPLETED"));
    assertEquals(TranslationStatus.FAILED, TranslationStatus.fromStatus("FAILED"));
    assertEquals(TranslationStatus.PENDING, TranslationStatus.fromStatus("PENDING"));
    assertEquals(TranslationStatus.WAITING, TranslationStatus.fromStatus("WAITING"));
  }

  @Test
  void fromStatus_returnsCorrectEnum_mixedCase() {
    assertEquals(TranslationStatus.COMPLETED, TranslationStatus.fromStatus("Completed"));
    assertEquals(TranslationStatus.FAILED, TranslationStatus.fromStatus("FaIlEd"));
    assertEquals(TranslationStatus.PENDING, TranslationStatus.fromStatus("PeNdInG"));
    assertEquals(TranslationStatus.WAITING, TranslationStatus.fromStatus("WaItInG"));
  }

  @Test
  void fromStatus_throwsIllegalArgumentException_forUnknownValue() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
      TranslationStatus.fromStatus("unknown")
    );
    assertTrue(ex.getMessage().contains("unknown"));
  }

  @Test
  void fromStatus_throwsIllegalArgumentException_forEmptyString() {
    assertThrows(IllegalArgumentException.class, () -> TranslationStatus.fromStatus(""));
  }

  @Test
  void fromStatus_throwsIllegalArgumentException_forNull() {
    assertThrows(IllegalArgumentException.class, () -> TranslationStatus.fromStatus(null));
  }
}
