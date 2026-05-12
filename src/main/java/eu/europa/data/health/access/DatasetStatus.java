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
public enum DatasetStatus {

  POSITIVE_DECISION_ISSUED("POSITIVE_DECISION_ISSUED"),
  POSITIVE_DECISION_ACCEPTED("POSITIVE_DECISION_ACCEPTED"),
  POSITIVE_DECISION_REJECTED("POSITIVE_DECISION_REJECTED"),
  NEGATIVE_DECISION_ISSUED("NEGATIVE_DECISION_ISSUED"),
  NEGATIVE_DECISION_ACCEPTED("NEGATIVE_DECISION_ACCEPTED"),
  NEGATIVE_DECISION_APPEALED("NEGATIVE_DECISION_APPEALED"),
  APPEAL_REJECTED("APPEAL_REJECTED"),
  WITHDRAWN("WITHDRAWN");

  /**
   * -- GETTER --
   *  Returns the human-friendly label shown to end users.
   */
  private final String label;
  private Set<DatasetStatus> allowedNextStatuses = Set.of();

  DatasetStatus(String label) {
    this.label = label;
  }

  static {
    POSITIVE_DECISION_ISSUED.allowedNextStatuses = Set.of(
      POSITIVE_DECISION_ACCEPTED,
      POSITIVE_DECISION_REJECTED,
      WITHDRAWN
    );

    NEGATIVE_DECISION_ISSUED.allowedNextStatuses = Set.of(
      NEGATIVE_DECISION_ACCEPTED,
      NEGATIVE_DECISION_APPEALED,
      WITHDRAWN
    );

    NEGATIVE_DECISION_APPEALED.allowedNextStatuses = Set.of(
      APPEAL_REJECTED,
      POSITIVE_DECISION_ISSUED
    );
  }

  /**
   * Convert a *label* (from frontend) to the matching enum constant.
   *
   * Example:
   *  "Rejected as incomplete" -> DatasetStatus.REJECTED
   */
  public static DatasetStatus fromLabel(String label, @NotNull RoutingContext context) {
    if (StringUtils.isEmpty(label)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Provided dataset status value is null/empty");
      throw new RuntimeException("Provided dataset status value is null/empty");
    }

    for (DatasetStatus status : DatasetStatus.values()) {
      if (status.label.equalsIgnoreCase(label.trim())) {
        return status;
      }
    }
    setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
      "Provided dataset status value: " + label + " is not a valid dataset status");
    throw new RuntimeException("Provided dataset status value: " + label + " is not a valid dataset status");
  }

  public CatalogStatus computeCatalogStatus() {
    return switch (this) {
      case POSITIVE_DECISION_ISSUED, POSITIVE_DECISION_ACCEPTED, POSITIVE_DECISION_REJECTED, NEGATIVE_DECISION_ISSUED, NEGATIVE_DECISION_ACCEPTED, NEGATIVE_DECISION_APPEALED, APPEAL_REJECTED, WITHDRAWN -> CatalogStatus.DECISION_ISSUED;
    };
  }

  public boolean canChangeTo(DatasetStatus next, String collectionName, String decisionReason, @NotNull RoutingContext context) {

    if(next.equals(POSITIVE_DECISION_REJECTED) || next.equals(NEGATIVE_DECISION_APPEALED)) {
      if(StringUtils.isEmpty(decisionReason)) {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "A decision reason must be provided when changing dataset status to: " + next.getLabel());
        throw new IllegalStateException("A decision reason must be provided when changing dataset status to: " + next.getLabel());
      }
    }

    if (allowedNextStatuses.contains(next)) {
      return true;
    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid dataset transition status from: " + this.getLabel() + " to: " + next.getLabel());
      throw new IllegalStateException("Invalid dataset transition from: " + this.getLabel() + " to: " + next.getLabel());
    }
  }

  public boolean validateInitialization(@NotNull RoutingContext context) {
    if (this.equals(POSITIVE_DECISION_ISSUED) || this.equals(NEGATIVE_DECISION_ISSUED)) {
      return true;
    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid initialization of decision status: " + this.getLabel());
      throw new IllegalStateException("Invalid initialization of decision status: " + this.getLabel());
    }
  }

  public void validatePermitChange(@NotNull RoutingContext context){
    if (!this.equals(POSITIVE_DECISION_ACCEPTED)){
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid dataset status, a permit status change can only come from catalog status: " + POSITIVE_DECISION_ACCEPTED.getLabel());
      throw new IllegalStateException("Invalid catalog status, a permit status change can only come from catalog status: " + POSITIVE_DECISION_ACCEPTED.getLabel());
    }
  }
}
