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

public enum SchemaFiles {
  BASKET_SCHEMA_FILE ("basketSchema.json"),
  DATA_ACCESS_APPLICATION_SCHEMA_FILE ("dataAccessApplicationSchema.json"),
  DATA_ACCESS_APPLICATION_DELETE_DATASETS_SCHEMA_FILE ("dataAccessApplicationDeleteDatasetsSchema.json"),
  DATA_ACCESS_APPLICATION_DELETE_DISTRIBUTIONS_SCHEMA_FILE ("dataAccessApplicationDeleteDistributionsSchema.json"),
  DATA_ACCESS_APPLICATION_UPDATE_SCHEMA_FILE("dataAccessApplicationUpdateSchema.json"),
  DATA_ACCESS_APPLICATION_CHANGE_STATUS_FILE("dataAccessApplicationChangeStatusSchema.json"),
  DATA_ACCESS_APPLICATION_CHANGE_TITLE_FILE("dataAccessApplicationChangeTitleSchema.json"),
  AUDIT_LOG_FOR_DATASET_RECORD("auditLogForDatasetRecord.json"),
  AUDIT_LOG_FOR_SEARCH("auditLogForSearch.json"),
  SCHEMA_FILE_PATH ("schema_validators/"),
  CLASSPATH_SCHEMA_FILE_PATH ("classpath:/schemaValidators/"),
  COUNTRIES_RDF("countries/countries-skos.rdf"),
  USER_SCHEMA_FILE ("userSchema.json"),
  EMAIL_PREFERENCES_SCHEMA_FILE ("emailPreferencesSchema.json"),
  DISPATCHER_DECISION_SCHEMA_FILE("dispatcherDecisionSchema.json"),
  DISPATCHER_STATUS_SCHEMA_FILE("dispatcherStatusSchema.json"),
  PERMIT_STATUS_SCHEMA_FILE("dispatcherPermitSchema.json"),
  CATALOGUE_FORM_RDF("catalogues/catalogue-form.rdf"),
  CATALOGUE_SEARCH("catalogueForSearch.json");

  private final String fileName;

  SchemaFiles(final String fileName) {
    this.fileName = fileName;
  }

  public String getValue() {
    return fileName;
  }
}
