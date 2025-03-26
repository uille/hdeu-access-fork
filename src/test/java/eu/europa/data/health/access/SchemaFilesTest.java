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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SchemaFilesTest {

  @Test
  void testGetValue() {
    // Verify the file names for each enum constant
    assertEquals("basketSchema.json", SchemaFiles.BASKET_SCHEMA_FILE.getValue());
    assertEquals("dataAccessApplicationSchema.json", SchemaFiles.DATA_ACCESS_APPLICATION_SCHEMA_FILE.getValue());
    assertEquals("dataAccessApplicationDeleteDatasetsSchema.json", SchemaFiles.DATA_ACCESS_APPLICATION_DELETE_DATASETS_SCHEMA_FILE.getValue());
    assertEquals("dataAccessApplicationDeleteDistributionsSchema.json", SchemaFiles.DATA_ACCESS_APPLICATION_DELETE_DISTRIBUTIONS_SCHEMA_FILE.getValue());
    assertEquals("dataAccessApplicationUpdateSchema.json", SchemaFiles.DATA_ACCESS_APPLICATION_UPDATE_SCHEMA_FILE.getValue());
    assertEquals("dataAccessApplicationChangeStatusSchema.json", SchemaFiles.DATA_ACCESS_APPLICATION_CHANGE_STATUS_FILE.getValue());
    assertEquals("dataAccessApplicationChangeTitleSchema.json", SchemaFiles.DATA_ACCESS_APPLICATION_CHANGE_TITLE_FILE.getValue());
    assertEquals("schema_validators/", SchemaFiles.SCHEMA_FILE_PATH.getValue());
    assertEquals("classpath:/schemaValidators/", SchemaFiles.CLASSPATH_SCHEMA_FILE_PATH.getValue());
    assertEquals("countries/countries-skos.rdf", SchemaFiles.COUNTRIES_RDF.getValue());
  }

  @Test
  void testEnumValues() {
    // Verify the number of constants in the enum
    SchemaFiles[] values = SchemaFiles.values();
    assertEquals(11, values.length);

    // Verify each constant is present
    assertEquals(SchemaFiles.BASKET_SCHEMA_FILE, SchemaFiles.valueOf("BASKET_SCHEMA_FILE"));
    assertEquals(SchemaFiles.DATA_ACCESS_APPLICATION_SCHEMA_FILE, SchemaFiles.valueOf("DATA_ACCESS_APPLICATION_SCHEMA_FILE"));
    assertEquals(SchemaFiles.DATA_ACCESS_APPLICATION_DELETE_DATASETS_SCHEMA_FILE, SchemaFiles.valueOf("DATA_ACCESS_APPLICATION_DELETE_DATASETS_SCHEMA_FILE"));
    assertEquals(SchemaFiles.DATA_ACCESS_APPLICATION_DELETE_DISTRIBUTIONS_SCHEMA_FILE, SchemaFiles.valueOf("DATA_ACCESS_APPLICATION_DELETE_DISTRIBUTIONS_SCHEMA_FILE"));
    assertEquals(SchemaFiles.DATA_ACCESS_APPLICATION_UPDATE_SCHEMA_FILE, SchemaFiles.valueOf("DATA_ACCESS_APPLICATION_UPDATE_SCHEMA_FILE"));
    assertEquals(SchemaFiles.DATA_ACCESS_APPLICATION_CHANGE_STATUS_FILE, SchemaFiles.valueOf("DATA_ACCESS_APPLICATION_CHANGE_STATUS_FILE"));
    assertEquals(SchemaFiles.DATA_ACCESS_APPLICATION_CHANGE_TITLE_FILE, SchemaFiles.valueOf("DATA_ACCESS_APPLICATION_CHANGE_TITLE_FILE"));
    assertEquals(SchemaFiles.SCHEMA_FILE_PATH, SchemaFiles.valueOf("SCHEMA_FILE_PATH"));
    assertEquals(SchemaFiles.CLASSPATH_SCHEMA_FILE_PATH, SchemaFiles.valueOf("CLASSPATH_SCHEMA_FILE_PATH"));
    assertEquals(SchemaFiles.COUNTRIES_RDF, SchemaFiles.valueOf("COUNTRIES_RDF"));
  }
}
