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

class DatasetStatusTest {

    @Test
    void testCanChangeTo_ValidTransition() {
        RoutingContext mockContext = mock(RoutingContext.class);
        DatasetStatus currentStatus = DatasetStatus.POSITIVE_DECISION_ISSUED;
        DatasetStatus nextStatus = DatasetStatus.POSITIVE_DECISION_ACCEPTED;

        assertTrue(currentStatus.canChangeTo(nextStatus, "someCollectionName", mockContext));
    }


    @Test
    void testCanChangeTo_ValidTransitionForSpecialCase() {
        RoutingContext mockContext = mock(RoutingContext.class);
        DatasetStatus currentStatus = DatasetStatus.APPEAL_APPROVED;
        DatasetStatus nextStatus = DatasetStatus.POSITIVE_DECISION_ISSUED;

        assertTrue(currentStatus.canChangeTo(nextStatus, Constants.DATA_REQUEST_APPLICATION_COLLECTION, mockContext));
    }


  private RoutingContext ctx() {
    return mock(RoutingContext.class, RETURNS_DEEP_STUBS);
  }

  // ---------- fromLabel() ----------

  @Test
  void testFromLabel_validLabel_caseAndTrim() {
    RoutingContext context = ctx();
    assertEquals(DatasetStatus.POSITIVE_DECISION_ISSUED, DatasetStatus.fromLabel("POSITIVE_DECISION_ISSUED", context));
    assertEquals(DatasetStatus.POSITIVE_DECISION_ISSUED, DatasetStatus.fromLabel("positive_decision_issued", context));
    assertEquals(DatasetStatus.POSITIVE_DECISION_ISSUED, DatasetStatus.fromLabel("  POSITIVE_DECISION_ISSUED  ", context));
  }

  @Test
  void testFromLabel_emptyOrNull_shouldThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      RuntimeException ex1 = assertThrows(RuntimeException.class, () -> DatasetStatus.fromLabel(null, context));
      assertEquals("Provided dataset status value is null/empty", ex1.getMessage());

      RuntimeException ex2 = assertThrows(RuntimeException.class, () -> DatasetStatus.fromLabel("", context));
      assertEquals("Provided dataset status value is null/empty", ex2.getMessage());

      mocked.verify(() ->
          eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
            eq(context),
            eq(HttpResponseStatus.BAD_REQUEST),
            eq("Provided dataset status value is null/empty")
          ),
        atLeastOnce()
      );
    }
  }

  @Test
  void testFromLabel_invalid_shouldThrow() {
    RoutingContext context = ctx();
    String label = "UNKNOWN";

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      RuntimeException ex = assertThrows(RuntimeException.class, () -> DatasetStatus.fromLabel(label, context));
      assertEquals("Provided dataset status value: " + label + " is not a valid dataset status", ex.getMessage());

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          eq("Provided dataset status value: " + label + " is not a valid dataset status")
        )
      );
    }
  }

  // ---------- computeCatalogStatus() ----------

  @Test
  void testComputeCatalogStatus_alwaysDecisionIssued() {
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.POSITIVE_DECISION_ISSUED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.POSITIVE_DECISION_ACCEPTED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.POSITIVE_DECISION_REJECTED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.NEGATIVE_DECISION_ISSUED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.NEGATIVE_DECISION_ACCEPTED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.NEGATIVE_DECISION_APPEALED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.APPEAL_APPROVED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.APPEAL_REJECTED.computeCatalogStatus());
    assertEquals(CatalogStatus.DECISION_ISSUED, DatasetStatus.WITHDRAWN.computeCatalogStatus());
  }

  // ---------- canChangeTo() invalid branch ----------

  @Test
  void testCanChangeTo_invalidTransition_shouldSetBadRequest_andThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        DatasetStatus.POSITIVE_DECISION_ISSUED.canChangeTo(
          DatasetStatus.APPEAL_APPROVED,
          "someCollectionName",
          context
        )
      );

      assertTrue(ex.getMessage().contains("Invalid dataset transition"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid dataset transition status from:")
        )
      );
    }
  }

  @Test
  void testCanChangeTo_allowedTransition_shouldReturnTrue() {
    RoutingContext context = ctx();
    assertTrue(DatasetStatus.NEGATIVE_DECISION_ISSUED.canChangeTo(
      DatasetStatus.NEGATIVE_DECISION_APPEALED,
      "any",
      context
    ));
  }

  // ---------- validateInitialization() ----------

  @Test
  void testValidateInitialization_whenIssued_shouldReturnTrue() {
    assertTrue(DatasetStatus.POSITIVE_DECISION_ISSUED.validateInitialization(ctx()));
    assertTrue(DatasetStatus.NEGATIVE_DECISION_ISSUED.validateInitialization(ctx()));
  }

  @Test
  void testValidateInitialization_whenNotIssued_shouldThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        DatasetStatus.POSITIVE_DECISION_ACCEPTED.validateInitialization(context)
      );

      assertTrue(ex.getMessage().contains("Invalid initialization of decision status"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid initialization of decision status:")
        )
      );
    }
  }

  // ---------- validatePermitChange() ----------

  @Test
  void testValidatePermitChange_whenAllowed_shouldNotThrow() {
    assertDoesNotThrow(() -> DatasetStatus.POSITIVE_DECISION_ACCEPTED.validatePermitChange(ctx()));
    assertDoesNotThrow(() -> DatasetStatus.APPEAL_APPROVED.validatePermitChange(ctx()));
  }

  @Test
  void testValidatePermitChange_whenNotAllowed_shouldSetBadRequest_andThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        DatasetStatus.NEGATIVE_DECISION_ACCEPTED.validatePermitChange(context)
      );

      // Note: code throws "Invalid catalog status..." in exception message, while response says dataset status.
      assertTrue(ex.getMessage().contains("Invalid catalog status"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid dataset status, a permit status change can only come from catalog status:")
        )
      );
    }
  }
}
