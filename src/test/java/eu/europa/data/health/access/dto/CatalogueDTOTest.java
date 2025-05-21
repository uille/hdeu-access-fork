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

class CatalogueDTOTest {

  @Test
  void testCompareByField_stringField() {
    CatalogueDTO dto1 = new CatalogueDTO();
    dto1.setCatalogueName("Alpha");

    CatalogueDTO dto2 = new CatalogueDTO();
    dto2.setCatalogueName("Beta");

    int result = dto1.compareByField("catalogueName", dto2);
    assertTrue(result < 0, "Expected Alpha < Beta");
  }

  @Test
  void testCompareByField_selectObjectField() {
    SelectObjectDTO geo1 = new SelectObjectDTO("GR", "Greece");
    SelectObjectDTO geo2 = new SelectObjectDTO("CY", "Cyprus");

    CatalogueDTO dto1 = new CatalogueDTO();
    dto1.setGeographicalCoverage(geo1);

    CatalogueDTO dto2 = new CatalogueDTO();
    dto2.setGeographicalCoverage(geo2);

    int result = dto1.compareByField("geographicalCoverage", dto2);
    assertTrue(result > 0, "Expected GR > CY lexically");
  }

  @Test
  void testCompareByField_nullValues() {
    CatalogueDTO dto1 = new CatalogueDTO();
    CatalogueDTO dto2 = new CatalogueDTO();

    dto1.setCatalogueName(null);
    dto2.setCatalogueName(null);

    int result = dto1.compareByField("catalogueName", dto2);
    assertEquals(0, result, "Expected nulls to be equal");
  }

  @Test
  void testCompareByField_invalidField() {
    CatalogueDTO dto1 = new CatalogueDTO();
    CatalogueDTO dto2 = new CatalogueDTO();

    Exception exception = assertThrows(RuntimeException.class, () ->
      dto1.compareByField("invalidField", dto2)
    );

    assertTrue(exception.getMessage().contains("Error in comparison"));
  }


  @Test
  void testCompareByField_selectObjectValue() {
    CatalogueDTO dto1 = new CatalogueDTO();
    CatalogueDTO dto2 = new CatalogueDTO();
    SelectObjectDTO  selectObjectDTO = new SelectObjectDTO ();
    SelectObjectDTO  selectObjectDTO1 = new SelectObjectDTO ();

    selectObjectDTO.setValue(null);
    selectObjectDTO1.setValue(null);

    dto1.setGeographicalCoverage(selectObjectDTO);
    dto2.setGeographicalCoverage(selectObjectDTO1);

    int result = dto1.compareByField("catalogueName", dto2);
    assertEquals(0, result, "Expected nulls to be equal");
  }



}
