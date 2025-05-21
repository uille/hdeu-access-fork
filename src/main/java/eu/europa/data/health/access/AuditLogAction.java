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

public enum AuditLogAction {

  DATASET_RECORD_CREATION("Dataset Record creation"),
  DATASET_RECORD_UPDATE("Dataset Record update"),
  DATASET_RECORD_DELETION("Dataset Record deletion"),

  USER_ACCOUNT("User Account deletion"),

  DATA_ACCESS_APPLICATION_CREATED("Data Access application created"),
  DATA_ACCESS_APPLICATION_SUBMITTED("Data Access application submitted"),
  DATA_ACCESS_APPLICATION_DELETED("Data Access application deleted"),
  DATA_ACCESS_APPLICATION_DISCARDED("Data Access application discarded"),

  DATA_REQUEST_APPLICATION_CREATED("Data Request application created"),
  DATA_REQUEST_APPLICATION_SUBMITTED("Data Request application submitted"),
  DATA_REQUEST_APPLICATION_DELETED("Data Request application deleted"),
  DATA_REQUEST_APPLICATION_DISCARDED("Data Request application discarded"),

  DATASET_DELETED("Dataset deleted"),
  DISTRIBUTION_DELETED("Distribution deleted");

  private final String auditLogAction;

  AuditLogAction(final String auditLogAction) {
    this.auditLogAction = auditLogAction;
  }

  public String getString() {
    return auditLogAction;
  }

}
