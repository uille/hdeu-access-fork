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

public enum AuditLogActionDescription {

  DATASET_RECORD_CREATION("Dataset Record with id: {} was created by NCP: {}"),
  DATASET_RECORD_UPDATE("Dataset Record with id: {} was updated by NCP: {}"),
  DATASET_RECORD_DELETION("Dataset Record with id: {} was deleted by NCP: {}"),

  USER_ACCOUNT_DELETED("User with EU Login ID: {} has deleted their account"),

  DATA_ACCESS_APPLICATION_CREATED("Data Access application {} : {} has been created"),
  DATA_ACCESS_APPLICATION_UPDATED("Data Access application {} : {} has been updated to status: {}"),
  DATA_ACCESS_APPLICATION_HDAB_UPDATED("Data Access application {} : {} has been updated to HDAB status: {} for catalog id: {}"),
  DATA_ACCESS_APPLICATION_HDAB_AND_DECISION_UPDATED("Data Access application {} : {} has been updated to HDAB status: {} for catalog id: {} and dataset status: {} for dataset id: {}"),
  DATA_ACCESS_APPLICATION_PERMIT_UPDATED("Data Access application {} : {} for catalog id: {} and dataset id: {} has been updated to permit status: {}"),

  DATA_ACCESS_APPLICATION_AND_CATALOG_UPDATED("Data Access application {} : {} has been updated to status: {} and catalog status: {} for catalog id: {}"),
  DATA_ACCESS_APPLICATION_DATASET_UPDATED("Data Access application {} : {} has been updated to dataset status: {} for dataset id: {}"),
  DATA_ACCESS_APPLICATION_SUBMITTED("Data Access application {} : {} has been submitted"),
  DATA_ACCESS_APPLICATION_RESUBMITTED("Data Access application {} : {} has been re-submitted"),
  DATA_ACCESS_APPLICATION_DELETED("Data Access application {} : {} has been deleted"),
  DATA_ACCESS_APPLICATION_DISCARDED("Data Access application {} : {} has been discarded by removing the last Dataset with id: {} and distribution(s): {}"),
  DATA_ACCESS_APPLICATION_DATASET_DELETED("Dataset with id: {} and distribution(s): {} for Data Access application {} : {} has been deleted"),
  DATA_ACCESS_APPLICATION_DISTRIBUTION_DELETED("Distribution with id: {} from dataset with id: {} for Data Access application {} : {} has been deleted"),
  DATA_ACCESS_APPLICATION_TRANSLATION_FAILED("Data Access application {} : {} translation failed: {}"),

  DATA_REQUEST_APPLICATION_CREATED("Data Request application {} : {} has been created"),
  DATA_REQUEST_APPLICATION_UPDATED("Data Request application {} : {} has been updated to status: {}"),
  DATA_REQUEST_APPLICATION_HDAB_UPDATED("Data Request application {} : {} has been updated to HDAB status: {} for catalog id: {}"),
  DATA_REQUEST_APPLICATION_HDAB_AND_DECISION_UPDATED("Data Request application {} : {} has been updated to HDAB status: {} for catalog id: {} and dataset status: {} for dataset id: {}"),

  DATA_REQUEST_APPLICATION_AND_CATALOG_UPDATED("Data Request application {} : {} has been updated to status: {} and catalog status: {} for catalog id: {}"),
  DATA_REQUEST_APPLICATION_DATASET_UPDATED("Data Request application {} : {} has been updated to dataset status: {} for dataset id: {}"),
  DATA_REQUEST_APPLICATION_SUBMITTED("Data Request application {} : {} has been submitted"),
  DATA_REQUEST_APPLICATION_RESUBMITTED("Data Request application {} : {} has been re-submitted"),
  DATA_REQUEST_APPLICATION_DELETED("Data Request application {} : {} has been deleted"),
  DATA_REQUEST_APPLICATION_DISCARDED("Data Request application {} : {} has been discarded by removing the last Dataset with id: {} and distribution(s): {}"),
  DATA_REQUEST_APPLICATION_DATASET_DELETED("Dataset with id: {} and distribution(s): {} for Data Request application {} : {} has been deleted"),
  DATA_REQUEST_APPLICATION_DISTRIBUTION_DELETED("Distribution with id: {} from dataset with id: {} for Data Request application {} : {} has been deleted"),
  DATA_REQUEST_APPLICATION_TRANSLATION_FAILED("Data Request application {} : {} translation failed: {}"),

  CATALOGUE_CREATED("Catalogue: {} : {} has been created"),
  CATALOGUE_UPDATED("Catalogue: {} : {} has been edited"),
  CATALOGUE_DELETED("Catalogue: {} : {} has been deleted"),

  USER_UPDATED("User: {} has been updated"),
  USER_DELETED("User: {} has been deleted");

  private final String description;

  AuditLogActionDescription(final String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return description;
  }

}
