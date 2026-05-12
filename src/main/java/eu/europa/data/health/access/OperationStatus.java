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

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum OperationStatus {

  STATUS_UPDATE("statusUpdate"),
  DECISION("decision"),
  CREATE("create"),
  UPDATE("update"),
  PERMIT("permit"),
  ACTIVITY_RESPONSE("activityResponse"),
  ACTIVITY_TRANSLATION("activityTranslation");


  /**
   * -- GETTER --
   *  Returns the human-friendly label shown to end users.
   */
  private final String label;

  OperationStatus(String label) {
    this.label = label;
  }

  /**
   * Convert a *label* (from frontend) to the matching enum constant.
   *
   * Example:
   *  "Rejected as incomplete" -> OperationStatus.REJECTED
   */
  public static OperationStatus fromLabel(String label) {
    if (StringUtils.isEmpty(label)) {
      return null;
    }

    for (OperationStatus status : OperationStatus.values()) {
      if (status.label.equalsIgnoreCase(label.trim())) {
        return status;
      }
    }
    return null;
  }
}
