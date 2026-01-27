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
public enum PermitStatus {
  GRANTED("GRANTED"),
  RENEWAL_REQUESTED("RENEWAL_REQUESTED"),
  RENEWED("RENEWED"),
  RENEWAL_REJECTED("RENEWAL_REJECTED"),
  EXPIRED("EXPIRED"),
  AMENDMENT_REQUESTED("AMENDMENT_REQUESTED"),
  AMENDED("AMENDED"),
  AMENDMENT_REJECTED("AMENDMENT_REJECTED"),
  REVOKED("REVOKED"),
  REVOKE_APPEALED("REVOKE_APPEALED"),
  APPEAL_APPROVED("APPEAL_APPROVED"),
  APPEAL_REJECTED("APPEAL_REJECTED");

  /**
   * -- GETTER -- Returns the label shown to end users.
   */
  private final String label;
  private Set<PermitStatus> allowedNextStatuses = Set.of();

  PermitStatus(String label) {
    this.label = label;
  }

  static {
    GRANTED.allowedNextStatuses = Set.of(
      RENEWAL_REQUESTED,
      AMENDMENT_REQUESTED,
      EXPIRED,
      REVOKED
    );

    RENEWAL_REQUESTED.allowedNextStatuses = Set.of(
      RENEWED,
      RENEWAL_REJECTED
    );

    RENEWED.allowedNextStatuses = Set.of(
      AMENDMENT_REQUESTED,
      REVOKED,
      EXPIRED
    );

    RENEWAL_REJECTED.allowedNextStatuses = Set.of(
      AMENDMENT_REQUESTED,
      REVOKED,
      EXPIRED
    );

    AMENDMENT_REQUESTED.allowedNextStatuses = Set.of(
      AMENDED,
      AMENDMENT_REJECTED
    );

    AMENDED.allowedNextStatuses = Set.of(
      RENEWAL_REQUESTED,
      AMENDMENT_REQUESTED,
      REVOKED,
      EXPIRED
    );

    AMENDMENT_REJECTED.allowedNextStatuses = Set.of(
      RENEWAL_REQUESTED,
      REVOKED,
      EXPIRED
    );

    REVOKED.allowedNextStatuses = Set.of(
      REVOKE_APPEALED
    );

    REVOKE_APPEALED.allowedNextStatuses = Set.of(
      APPEAL_APPROVED,
      APPEAL_REJECTED
    );

    APPEAL_APPROVED.allowedNextStatuses = Set.of(
      GRANTED
    );

  }

  /**
   * Convert a label to the matching enum constant.
   */
  public static PermitStatus fromLabel(String label, boolean nullable, @NotNull RoutingContext context) {

    if (StringUtils.isEmpty(label)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Provided catalog status value is null/empty");
      throw new RuntimeException("Provided catalog status value is null/empty");
    }

    for (PermitStatus status : PermitStatus.values()) {
      if (status.label.equalsIgnoreCase(label.trim())) {
        return status;
      }
    }
    if(nullable)
      return null;
    else {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Provided permit status value: " + label + " is not a valid permit status");
      throw new RuntimeException("Provided permit status value: " + label + " is not a valid permit status");
    }
  }

  public boolean canChangeTo(PermitStatus next, @NotNull RoutingContext context) {

    if(this.equals(next)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid permit transition status from: " + this.getLabel() + " to: " + next.getLabel());
      throw new IllegalStateException("Invalid permit transition from: " + this.getLabel() + " to: " + next.getLabel());
    }

    if (!allowedNextStatuses.contains(next)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid permit transition status from: " + this.getLabel() + " to: " + next.getLabel());
      throw new IllegalStateException("Invalid permit transition from: " + this.getLabel() + " to: " + next.getLabel());
    }
    return true;
  }

  public void validateInitialization(@NotNull RoutingContext context) {

    if (this.equals(GRANTED)) {
      return;
    }

    setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
      "Invalid initialization of permit status: " + this.getLabel());
    throw new IllegalStateException("Invalid initialization of permit status: " + this.getLabel());
  }
}
