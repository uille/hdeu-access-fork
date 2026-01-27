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
import io.vertx.ext.web.RoutingContext;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Getter
public enum CatalogStatus {

  DRAFT("DRAFT"),
  SENT("SENT"),
  SUBMITTED("SUBMITTED"),
  PRE_SCREEN("PRE_SCREEN"),
  PROCESSING("PROCESSING"),
  PROCESSING_EXTENDED("PROCESSING_EXTENDED"),
  ADDITIONAL_INFORMATION("ADDITIONAL_INFORMATION"),
  REJECTED("REJECTED"),
  WITHDRAWN("WITHDRAWN"),
  DECISION_ISSUED("DECISION_ISSUED");

  /**
   * -- GETTER --
   *  Returns the human-friendly label shown to end users.
   */
  private final String label;
  private Set<CatalogStatus> allowedNextStatuses = Set.of();

  CatalogStatus(String label) {
    this.label = label;
  }

  static {
    DRAFT.allowedNextStatuses = Set.of(
      SENT
    );

    SENT.allowedNextStatuses = Set.of(
      SUBMITTED
    );

    SUBMITTED.allowedNextStatuses = Set.of(
      PRE_SCREEN,
      PROCESSING,
      WITHDRAWN
    );

    PRE_SCREEN.allowedNextStatuses = Set.of(
      PROCESSING,
      ADDITIONAL_INFORMATION,
      REJECTED,
      WITHDRAWN
    );

    PROCESSING.allowedNextStatuses = Set.of(
      PROCESSING_EXTENDED,
      ADDITIONAL_INFORMATION,
      DECISION_ISSUED,
      WITHDRAWN
    );

    PROCESSING_EXTENDED.allowedNextStatuses = Set.of(
      ADDITIONAL_INFORMATION,
      DECISION_ISSUED,
      WITHDRAWN
    );

    ADDITIONAL_INFORMATION.allowedNextStatuses = Set.of(
      SUBMITTED,
      WITHDRAWN
    );

  }

  /**
   * Convert a *label* (from frontend) to the matching enum constant.
   *
   * Example:
   *  "Rejected as incomplete" -> CatalogStatus.REJECTED
   */
  public static CatalogStatus fromLabel(String label, @NotNull RoutingContext context) {
    if (StringUtils.isEmpty(label)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Provided catalog status value is null/empty");
      throw new RuntimeException("Provided catalog status value is null/empty");
    }

    for (CatalogStatus status : CatalogStatus.values()) {
      if (status.label.equalsIgnoreCase(label.trim())) {
        return status;
      }
    }
    setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
      "Provided catalog status value: " + label + " is not a valid catalog status");
    throw new RuntimeException("Provided catalog status value: " + label + " is not a valid catalog status");
  }

  public ApplicationStatus computeApplicationStatus() {
    return switch (this) {
      case DRAFT -> ApplicationStatus.DRAFT;
      case SENT, SUBMITTED, PRE_SCREEN, PROCESSING, PROCESSING_EXTENDED, ADDITIONAL_INFORMATION ->
        ApplicationStatus.ON_GOING;
      case REJECTED, WITHDRAWN, DECISION_ISSUED -> ApplicationStatus.COMPLETED;
    };
  }

  public boolean canChangeTo(CatalogStatus next, @NotNull RoutingContext context) {

    if(this.equals(DECISION_ISSUED) && next.equals(DECISION_ISSUED)) {
      return false;
    }

    if(this.equals(next)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid catalog transition status from: " + this.getLabel() + " to: " + next.getLabel());
      throw new IllegalStateException("Invalid catalog transition from: " + this.getLabel() + " to: " + next.getLabel());
    }

    if (!allowedNextStatuses.contains(next)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid catalog transition status from: " + this.getLabel() + " to: " + next.getLabel());
      throw new IllegalStateException("Invalid catalog transition from: " + this.getLabel() + " to: " + next.getLabel());
    }
    return true;
  }

  public void validatePermitChange(@NotNull RoutingContext context){
    if(!this.equals(DECISION_ISSUED)){
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid catalog status, a permit status change can only come from catalog status: " + DECISION_ISSUED.getLabel());
      throw new IllegalStateException("Invalid catalog status, a permit status change can only come from catalog status: " + DECISION_ISSUED.getLabel());
    }
  }

}
