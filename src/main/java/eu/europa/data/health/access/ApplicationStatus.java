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

import static eu.europa.data.health.access.services.AbstractManager.setUpMessageContextResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ApplicationStatus {

  DRAFT("DRAFT"),
  ON_GOING("ON-GOING"),
  COMPLETED("COMPLETED", Set.of(CatalogStatus.REJECTED, CatalogStatus.WITHDRAWN, CatalogStatus.DECISION_ISSUED));

  private final String status;
  private Set<ApplicationStatus> allowedNextStatuses = Set.of();
  private final Set<CatalogStatus> catalogStatuses;

  ApplicationStatus(String status) {
    this.status = status;
    this.catalogStatuses = Set.of();
  }

  ApplicationStatus(String status, Set<CatalogStatus> catalogStatuses) {
    this.status = status;
    this.catalogStatuses = catalogStatuses;
  }

  static {
    DRAFT.allowedNextStatuses = Set.of(
      ON_GOING
    );

    ON_GOING.allowedNextStatuses = Set.of(
      COMPLETED
    );
  }

  public static boolean isValidStatus(String statusStr) {
    for (ApplicationStatus status : ApplicationStatus.values()) {
      if (status.getStatus().equalsIgnoreCase(statusStr)) {
        return true;
      }
    }
    return false;
  }

  public static ApplicationStatus fromStatus(String statusStr) {
    for (ApplicationStatus status : ApplicationStatus.values()) {
      if (status.getStatus().equalsIgnoreCase(statusStr)) {
        return status;
      }
    }
    throw new IllegalArgumentException("No enum constant with status " + statusStr);
  }

  public boolean canChangeTo(ApplicationStatus next, JsonObject application, @NotNull RoutingContext context) {

    if(this.equals(next)) {
      return false;
    }

    if (!allowedNextStatuses.contains(next)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid application transition status from: " + this.getStatus() + " to: " + next.getStatus());
      throw new IllegalStateException("Invalid application transition from: " + this.getStatus() + " to: " + next.getStatus());
    }

    return allLatestStatusesAreInCompletedStatus(application.getJsonArray("catalogs"));

  }

  public boolean allLatestStatusesAreInCompletedStatus(JsonArray catalogs) {

    Set<String> completedCatalogStatusValues = ApplicationStatus.COMPLETED
      .getCatalogStatuses()
      .stream()
      .map(CatalogStatus::getLabel)
      .map(String::toUpperCase)
      .collect(Collectors.toSet());

    List<String> latestStatuses = new ArrayList<>();

    for (Object catalogObj : catalogs) {
      JsonObject catalog = (JsonObject) catalogObj;

      JsonArray statuses = catalog.getJsonArray("statuses");
      if (statuses == null || statuses.isEmpty()) {
        return false;
      }

      String latestStatus = null;

      for (Object statusObj : statuses) {
        JsonObject status = (JsonObject) statusObj;
        Integer latest = status.getInteger("latest", 0);

        if (latest == 1) {
          latestStatus = status.getString("status");
          break;
        }
      }

      if (latestStatus == null) {
        return false;
      }

      latestStatuses.add(latestStatus.toUpperCase());
    }

    return completedCatalogStatusValues.containsAll(latestStatuses);
  }

  public void validatePermitChange(@NotNull RoutingContext context){
    if (!this.equals(COMPLETED) && !this.equals(ON_GOING)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid application status, a permit status change can only come from application status: " + COMPLETED.getStatus());
      throw new IllegalStateException("Invalid application status, a permit status change can only come from application status: " + COMPLETED.getStatus());
    }
  }


}
