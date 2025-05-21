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
package eu.europa.data.health.access.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogueStatusEnumTest {

  @Test
  void testEnumValues() {
    assertEquals("live", CatalogueStatusEnum.LIVE.getValue());
    assertEquals("test", CatalogueStatusEnum.TEST.getValue());
  }

  @Test
  void testEnumNames() {
    assertEquals("LIVE", CatalogueStatusEnum.LIVE.name());
    assertEquals("TEST", CatalogueStatusEnum.TEST.name());
  }

  @Test
  void testEnumOrdinal() {
    assertEquals(0, CatalogueStatusEnum.LIVE.ordinal());
    assertEquals(1, CatalogueStatusEnum.TEST.ordinal());
  }

  @Test
  void testEnumFromString() {
    CatalogueStatusEnum live = CatalogueStatusEnum.valueOf("LIVE");
    CatalogueStatusEnum test = CatalogueStatusEnum.valueOf("TEST");

    assertSame(CatalogueStatusEnum.LIVE, live);
    assertSame(CatalogueStatusEnum.TEST, test);
  }
}
