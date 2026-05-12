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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.vertx.ext.web.RoutingContext;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermitStatusTest {

  // Use deep stubs so AbstractManager.setUpMessageContextResponse(context, ...)
  // can safely do things like context.response().setStatusCode(...).end(...)
  private RoutingContext ctx() {
    return mock(RoutingContext.class, RETURNS_DEEP_STUBS);
  }

  @Test
  void staticInitialization_shouldSetAllowedNextStatuses() {
    assertEquals(
      Set.of(
        PermitStatus.RENEWAL_REQUESTED,
        PermitStatus.AMENDMENT_REQUESTED,
        PermitStatus.EXPIRED,
        PermitStatus.REVOKED
      ),
      PermitStatus.GRANTED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.RENEWED, PermitStatus.RENEWAL_REJECTED),
      PermitStatus.RENEWAL_REQUESTED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.AMENDMENT_REQUESTED, PermitStatus.REVOKED, PermitStatus.EXPIRED),
      PermitStatus.RENEWED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.AMENDMENT_REQUESTED, PermitStatus.REVOKED, PermitStatus.EXPIRED),
      PermitStatus.RENEWAL_REJECTED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.AMENDED, PermitStatus.AMENDMENT_REJECTED),
      PermitStatus.AMENDMENT_REQUESTED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(
        PermitStatus.RENEWAL_REQUESTED,
        PermitStatus.AMENDMENT_REQUESTED,
        PermitStatus.REVOKED,
        PermitStatus.EXPIRED
      ),
      PermitStatus.AMENDED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.RENEWAL_REQUESTED, PermitStatus.REVOKED, PermitStatus.EXPIRED),
      PermitStatus.AMENDMENT_REJECTED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.REVOKE_APPEALED),
      PermitStatus.REVOKED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.APPEAL_APPROVED, PermitStatus.APPEAL_REJECTED),
      PermitStatus.REVOKE_APPEALED.getAllowedNextStatuses()
    );

    assertEquals(
      Set.of(PermitStatus.GRANTED),
      PermitStatus.APPEAL_APPROVED.getAllowedNextStatuses()
    );

    // Not assigned in static block => default empty set
    assertEquals(Set.of(), PermitStatus.APPEAL_REJECTED.getAllowedNextStatuses());
    assertEquals(Set.of(), PermitStatus.EXPIRED.getAllowedNextStatuses());
  }

  @Test
  void fromLabel_whenEmpty_shouldThrow() {
    RuntimeException ex1 = assertThrows(
      RuntimeException.class,
      () -> PermitStatus.fromLabel("", false, ctx())
    );
    assertEquals("Provided catalog status value is null/empty", ex1.getMessage());

    RuntimeException ex2 = assertThrows(
      RuntimeException.class,
      () -> PermitStatus.fromLabel(null, false, ctx())
    );
    assertEquals("Provided catalog status value is null/empty", ex2.getMessage());
  }

  @Test
  void fromLabel_whenValid_shouldReturnEnum_ignoringCaseAndTrim() {
    RoutingContext context = ctx();

    assertEquals(PermitStatus.GRANTED, PermitStatus.fromLabel(" granted ", false, context));
    assertEquals(PermitStatus.RENEWAL_REQUESTED, PermitStatus.fromLabel("RENEWAL_REQUESTED", false, context));
    assertEquals(PermitStatus.REVOKE_APPEALED, PermitStatus.fromLabel("revoke_appealed", false, context));
  }

  @Test
  void fromLabel_whenInvalid_andNullableTrue_shouldReturnNull() {
    PermitStatus result = PermitStatus.fromLabel("NOT_A_STATUS", true, ctx());
    assertNull(result);
  }

  @Test
  void fromLabel_whenInvalid_andNullableFalse_shouldThrow() {
    String label = "NOT_A_STATUS";
    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () -> PermitStatus.fromLabel(label, false, ctx())
    );
    assertEquals("Provided permit status value: " + label + " is not a valid permit status", ex.getMessage());
  }

  @Test
  void canChangeTo_whenSameStatus_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.GRANTED.canChangeTo(PermitStatus.GRANTED,"2026-02-06T11:40:32.487397566Z", ctx())
    );
    assertTrue(ex.getMessage().contains("Invalid permit transition from:"));
  }

  @Test
  void canChangeTo_whenNotAllowed_shouldThrow() {
    // Example: GRANTED cannot go directly to RENEWED
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.GRANTED.canChangeTo(PermitStatus.RENEWED,"2026-02-06T11:40:32.487397566Z", ctx())
    );
    assertTrue(ex.getMessage().contains("Invalid permit transition from:"));
  }

  @Test
  void canChangeTo_whenAllowed_shouldReturnTrue() {
    assertTrue(PermitStatus.GRANTED.canChangeTo(PermitStatus.RENEWAL_REQUESTED,"2096-02-06T11:40:32.487397566Z", ctx()));
  }

  @Test
  void canChangeTo_whenRenewedWithFutureExpiration_shouldReturnTrue() {
    assertTrue(
      PermitStatus.RENEWAL_REQUESTED.canChangeTo(
        PermitStatus.RENEWED,
        "2096-02-06T11:40:32.487397566Z",
        ctx()
      )
    );
  }

  @Test
  void canChangeTo_whenRenewedWithEmptyExpiration_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.RENEWAL_REQUESTED.canChangeTo(PermitStatus.RENEWED, "", ctx())
    );
    assertTrue(ex.getMessage().contains("permit expiration date should not be empty"));
  }

  @Test
  void canChangeTo_whenRenewedWithPastExpiration_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.RENEWAL_REQUESTED.canChangeTo(
        PermitStatus.RENEWED,
        "2020-02-06T11:40:32.487397566Z",
        ctx()
      )
    );
    assertTrue(ex.getMessage().contains("permit expiration date should be a future date"));
  }

  @Test
  void validateInitialization_whenGranted_shouldPass() {
    assertDoesNotThrow(() -> PermitStatus.GRANTED.validateInitialization(false, "2096-02-06T11:40:32.487397566Z", "2096-02-07T11:40:32.487397566Z", "REF-123", "permit contact info", ctx()));
  }

  @Test
  void validateInitialization_whenNotGranted_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.AMENDED.validateInitialization(false, "2096-02-06T11:40:32.487397566Z", "2096-02-07T11:40:32.487397566Z", "REF-123", "permit contact info", ctx())
    );
    assertTrue(ex.getMessage().contains("Invalid initialization of permit status:"));
  }

  @Test
  void validateInitialization_whenGrantedMissingMandatoryFields_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.GRANTED.validateInitialization(
        false,
        "2096-02-06T11:40:32.487397566Z",
        "",
        "REF-123",
        "permit contact info",
        ctx()
      )
    );
    assertTrue(ex.getMessage().contains("mandatory field/s are null/empty"));
  }

  @Test
  void validateInitialization_whenGrantedWithPastExpiration_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.GRANTED.validateInitialization(
        false,
        "2096-02-06T11:40:32.487397566Z",
        "2020-02-07T11:40:32.487397566Z",
        "REF-123",
        "permit contact info",
        ctx()
      )
    );
    assertTrue(ex.getMessage().contains("permit expiration date should be a future date"));
  }

  @Test
  void validateInitialization_whenGrantedWithExpirationBeforeStart_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.GRANTED.validateInitialization(
        false,
        "2096-02-07T11:40:32.487397566Z",
        "2096-02-06T11:40:32.487397566Z",
        "REF-123",
        "permit contact info",
        ctx()
      )
    );
    assertTrue(ex.getMessage().contains("permit expiration date should be after permit start date"));
  }

  @Test
  void validateInitialization_whenComingFromUiEvenIfGranted_shouldThrow() {
    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> PermitStatus.GRANTED.validateInitialization(
        true,
        "2096-02-06T11:40:32.487397566Z",
        "2096-02-07T11:40:32.487397566Z",
        "REF-123",
        "permit contact info",
        ctx()
      )
    );
    assertTrue(ex.getMessage().contains("Invalid initialization of permit status: GRANTED"));
  }
}
