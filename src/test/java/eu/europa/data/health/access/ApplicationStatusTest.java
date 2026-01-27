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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ApplicationStatusTest {

  @Test
  void testGetStatus() {
    assertEquals("DRAFT", ApplicationStatus.DRAFT.getStatus());
    assertEquals("ON-GOING", ApplicationStatus.ON_GOING.getStatus());
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
    assertEquals(ApplicationStatus.DRAFT, ApplicationStatus.fromStatus("DRAFT"));
    assertEquals(ApplicationStatus.ON_GOING, ApplicationStatus.fromStatus("ON-GOING"));
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
    assertTrue(ApplicationStatus.isValidStatus("Draft"));
    assertEquals(ApplicationStatus.DRAFT, ApplicationStatus.fromStatus("Draft"));

    assertTrue(ApplicationStatus.isValidStatus("On-GoinG"));
    assertEquals(ApplicationStatus.ON_GOING, ApplicationStatus.fromStatus("ON-gOiNg"));

    assertTrue(ApplicationStatus.isValidStatus("WiThDrAwN"));
  }


  private RoutingContext ctx() {
    return mock(RoutingContext.class, RETURNS_DEEP_STUBS);
  }

  private JsonObject applicationWithCatalogLatestStatuses(String... latestCatalogStatuses) {
    JsonArray catalogs = new JsonArray();
    for (String latest : latestCatalogStatuses) {
      JsonObject catalog = new JsonObject()
        .put("statuses", new JsonArray()
          .add(new JsonObject().put("status", latest).put("latest", 1))
        );
      catalogs.add(catalog);
    }
    return new JsonObject().put("catalogs", catalogs);
  }

  private JsonObject applicationWithCatalogStatusesArray(JsonArray... statusArrays) {
    JsonArray catalogs = new JsonArray();
    for (JsonArray statuses : statusArrays) {
      catalogs.add(new JsonObject().put("statuses", statuses));
    }
    return new JsonObject().put("catalogs", catalogs);
  }

  @Test
  void canChangeTo_whenSameStatus_shouldReturnFalse() {
    boolean ok = ApplicationStatus.DRAFT.canChangeTo(
      ApplicationStatus.DRAFT,
      applicationWithCatalogLatestStatuses("REJECTED"),
      ctx()
    );
    assertFalse(ok);
  }

  @Test
  void canChangeTo_whenNotAllowed_shouldSetBadRequest_andThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        ApplicationStatus.DRAFT.canChangeTo(
          ApplicationStatus.COMPLETED,
          applicationWithCatalogLatestStatuses("REJECTED"),
          context
        )
      );

      assertTrue(ex.getMessage().contains("Invalid application transition"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid application transition status from:")
        )
      );
    }
  }

  @Test
  void canChangeTo_whenAllowedButCatalogsNotCompleted_shouldReturnFalse() {
    // DRAFT -> ON_GOING is allowed, but method returns allLatestStatusesAreInCompletedStatus(...)
    boolean ok = ApplicationStatus.DRAFT.canChangeTo(
      ApplicationStatus.ON_GOING,
      applicationWithCatalogLatestStatuses("SUBMITTED"),
      ctx()
    );
    assertFalse(ok);
  }

  @Test
  void canChangeTo_whenAllowedAndAllCatalogLatestStatusesAreCompleted_shouldReturnTrue() {
    boolean ok = ApplicationStatus.ON_GOING.canChangeTo(
      ApplicationStatus.COMPLETED,
      applicationWithCatalogLatestStatuses("REJECTED", "WITHDRAWN", "DECISION_ISSUED"),
      ctx()
    );
    assertTrue(ok);
  }

  @Test
  void allLatestStatusesAreInCompletedStatus_whenStatusesNullOrEmpty_shouldReturnFalse() {
    JsonObject appNullStatuses = applicationWithCatalogStatusesArray((JsonArray) null);
    assertFalse(ApplicationStatus.DRAFT.allLatestStatusesAreInCompletedStatus(appNullStatuses.getJsonArray("catalogs")));

    JsonObject appEmptyStatuses = applicationWithCatalogStatusesArray(new JsonArray());
    assertFalse(ApplicationStatus.DRAFT.allLatestStatusesAreInCompletedStatus(appEmptyStatuses.getJsonArray("catalogs")));
  }

  @Test
  void allLatestStatusesAreInCompletedStatus_whenNoLatestFlag_shouldReturnFalse() {
    JsonArray statuses = new JsonArray()
      .add(new JsonObject().put("status", "REJECTED").put("latest", 0))
      .add(new JsonObject().put("status", "WITHDRAWN").put("latest", 0));

    JsonObject app = applicationWithCatalogStatusesArray(statuses);

    assertFalse(ApplicationStatus.DRAFT.allLatestStatusesAreInCompletedStatus(app.getJsonArray("catalogs")));
  }

  @Test
  void allLatestStatusesAreInCompletedStatus_whenLatestIsNotCompleted_shouldReturnFalse() {
    JsonObject app = applicationWithCatalogLatestStatuses("SUBMITTED");
    assertFalse(ApplicationStatus.DRAFT.allLatestStatusesAreInCompletedStatus(app.getJsonArray("catalogs")));
  }

  @Test
  void validatePermitChange_whenDraft_shouldSetBadRequest_andThrow() {
    RoutingContext context = ctx();

    try (MockedStatic<eu.europa.data.health.access.services.AbstractManager> mocked =
           mockStatic(eu.europa.data.health.access.services.AbstractManager.class)) {

      IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        ApplicationStatus.DRAFT.validatePermitChange(context)
      );

      assertTrue(ex.getMessage().contains("Invalid application status"));

      mocked.verify(() ->
        eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse(
          eq(context),
          eq(HttpResponseStatus.BAD_REQUEST),
          contains("Invalid application status, a permit status change can only come from application status:")
        )
      );
    }
  }

  @Test
  void validatePermitChange_whenOnGoingOrCompleted_shouldNotThrow() {
    assertDoesNotThrow(() -> ApplicationStatus.ON_GOING.validatePermitChange(ctx()));
    assertDoesNotThrow(() -> ApplicationStatus.COMPLETED.validatePermitChange(ctx()));
  }
}
