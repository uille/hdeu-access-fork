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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CatalogStatusTest {

    @Test
    void testFromLabel_validLabel() {
        RoutingContext context = Mockito.mock(RoutingContext.class);
        CatalogStatus result = CatalogStatus.fromLabel("DRAFT", context);
        assertNotNull(result);
        assertEquals(CatalogStatus.DRAFT, result);
    }

  @Test
  void testFromLabel_validLabelWithDifferentCase() {
    RoutingContext context = Mockito.mock(RoutingContext.class);
    CatalogStatus result = CatalogStatus.fromLabel("draft", context);
    assertNotNull(result);
    assertEquals(CatalogStatus.DRAFT, result);
  }

  @Test
  void testFromLabel_validLabelWithExtraSpaces() {
    RoutingContext context = Mockito.mock(RoutingContext.class);
    CatalogStatus result = CatalogStatus.fromLabel("  DRAFT  ", context);
    assertNotNull(result);
    assertEquals(CatalogStatus.DRAFT, result);
  }

  private RoutingContext ctx() {
    return mock(RoutingContext.class, RETURNS_DEEP_STUBS);
  }

  // ---------- fromLabel() error paths ----------

  @Test
  void testFromLabel_nullOrEmpty_shouldThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      RuntimeException ex1 = assertThrows(RuntimeException.class,
        () -> CatalogStatus.fromLabel(null, context));
      assertEquals("Provided catalog status value is null/empty", ex1.getMessage());

      RuntimeException ex2 = assertThrows(RuntimeException.class,
        () -> CatalogStatus.fromLabel("", context));
      assertEquals("Provided catalog status value is null/empty", ex2.getMessage());

      mocked.verify(() ->
          eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
            eq(context),
            eq(HttpResponseStatus.BAD_REQUEST),
            eq("Provided catalog status value is null/empty")
          ),
        atLeastOnce()
      );
    }
  }

  @Test
  void testFromLabel_invalidLabel_shouldThrow() {
    RoutingContext context = ctx();
    String label = "UNKNOWN";

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      RuntimeException ex = assertThrows(RuntimeException.class,
        () -> CatalogStatus.fromLabel(label, context));

      assertEquals(
        "Provided catalog status value: " + label + " is not a valid catalog status",
        ex.getMessage()
      );

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          eq("Provided catalog status value: " + label + " is not a valid catalog status")
        )
      );
    }
  }

  // ---------- computeApplicationStatus() ----------

  @Test
  void computeApplicationStatus_shouldMapCorrectly() {
    assertEquals(ApplicationStatus.DRAFT, CatalogStatus.DRAFT.computeApplicationStatus());

    assertEquals(ApplicationStatus.ON_GOING, CatalogStatus.SENT.computeApplicationStatus());
    assertEquals(ApplicationStatus.ON_GOING, CatalogStatus.SUBMITTED.computeApplicationStatus());
    assertEquals(ApplicationStatus.ON_GOING, CatalogStatus.PRE_SCREEN.computeApplicationStatus());
    assertEquals(ApplicationStatus.ON_GOING, CatalogStatus.PROCESSING.computeApplicationStatus());
    assertEquals(ApplicationStatus.ON_GOING, CatalogStatus.PROCESSING_EXTENDED.computeApplicationStatus());
    assertEquals(ApplicationStatus.ON_GOING, CatalogStatus.ADDITIONAL_INFORMATION.computeApplicationStatus());

    assertEquals(ApplicationStatus.COMPLETED, CatalogStatus.REJECTED.computeApplicationStatus());
    assertEquals(ApplicationStatus.COMPLETED, CatalogStatus.WITHDRAWN.computeApplicationStatus());
    assertEquals(ApplicationStatus.COMPLETED, CatalogStatus.DECISION_ISSUED.computeApplicationStatus());
  }

  // ---------- canChangeTo() ----------

  @Test
  void canChangeTo_whenDecisionIssuedToDecisionIssued_shouldReturnFalse() {
    assertFalse(CatalogStatus.DECISION_ISSUED.canChangeTo(
      CatalogStatus.DECISION_ISSUED, ctx()));
  }

  @Test
  void canChangeTo_whenSameStatus_shouldThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        CatalogStatus.DRAFT.canChangeTo(CatalogStatus.DRAFT, context)
      );

      assertTrue(ex.getMessage().contains("Invalid catalog transition"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid catalog transition status from:")
        )
      );
    }
  }

  @Test
  void canChangeTo_whenNotAllowed_shouldThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        CatalogStatus.DRAFT.canChangeTo(CatalogStatus.PROCESSING, context)
      );

      assertTrue(ex.getMessage().contains("Invalid catalog transition"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid catalog transition status from:")
        )
      );
    }
  }

  @Test
  void canChangeTo_whenAllowed_shouldReturnTrue() {
    assertTrue(CatalogStatus.DRAFT.canChangeTo(CatalogStatus.SENT, ctx()));
    assertTrue(CatalogStatus.SENT.canChangeTo(CatalogStatus.SUBMITTED, ctx()));
    assertTrue(CatalogStatus.SUBMITTED.canChangeTo(CatalogStatus.PROCESSING, ctx()));
  }

  // ---------- validatePermitChange() ----------

  @Test
  void validatePermitChange_whenNotDecisionIssued_shouldThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        CatalogStatus.DRAFT.validatePermitChange(context)
      );

      assertTrue(ex.getMessage().contains("Invalid catalog status"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid catalog status, a permit status change can only come from catalog status:")
        )
      );
    }
  }

  @Test
  void validatePermitChange_whenDecisionIssued_shouldNotThrow() {
    assertDoesNotThrow(() -> CatalogStatus.DECISION_ISSUED.validatePermitChange(ctx()));
  }
}
