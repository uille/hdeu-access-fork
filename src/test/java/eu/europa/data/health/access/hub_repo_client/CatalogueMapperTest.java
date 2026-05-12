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
package eu.europa.data.health.access.hub_repo_client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dto.CatalogueDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.HubSearchClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CatalogueMapperTest {

  @Mock
  private RoutingContext routingContext;
  @Mock
  private CountriesManager countriesManager;
  @Mock
  private HubSearchClient hubSearchClient;
  @Mock
  private RequestBody requestBody;

  private JsonObject catalogue;
  private JsonObject catalogueMapList;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    String catalogueId = "4507af72-fb13-4cde-ad71-f7b715a44262";
    JsonObject spatial = new JsonObject().put("@id", "http://example.com/location/EUR");
    JsonArray topGraph = new JsonArray();
    JsonArray nestedGraph = new JsonArray();
    nestedGraph.add(new JsonObject().put("dct:title", new JsonObject().put("@value", "Catalogue "
      + "Title")));
    nestedGraph.add(new JsonObject().put("dct:spatial", spatial));
    JsonObject firstElement = new JsonObject();
    firstElement.put("@graph", nestedGraph);
    firstElement.put("@id", "4507af72-fb13-4cde-ad71-f7b715a44262");
    firstElement.put("dct:title", new JsonObject().put("@value", "Catalogue Title"));
    firstElement.put("dct:description", new JsonObject().put("@value", "Catalogue Title"));
    firstElement.put("foaf:homepage", new JsonObject().put("@value", "Catalogue Title"));
    firstElement.put("foaf:name", new JsonObject().put("@value", "Catalogue Title"));
    firstElement.put("dct:issued", new JsonObject().put("@value", "12"));
    firstElement.put("dct:modified", new JsonObject().put("@value", "12"));
    firstElement.put("foaf:homepage", new JsonObject().put("@value", "Catalogue Title"));



    topGraph.add(0, firstElement);

    catalogue = new JsonObject().put("@graph", topGraph);

    JsonArray catalogueIds = new JsonArray();
    catalogueIds.add(catalogueId);
    catalogueMapList = new JsonObject().put("data", new JsonArray().add(
      new JsonObject()
        .put("domibusId", "123")
        .put("catalogueId", catalogueId)
        .put("geographicalCoverage", "EU")
        .put("catalogIds", catalogueIds)
        .put("recipient", "recipientEU")
        .put("isProduction", true)
    ));

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(
      new JsonObject().put("geoCoverageKeys", List.of("EU")));

    when(routingContext.request()).thenReturn(mock(io.vertx.core.http.HttpServerRequest.class));
    when(routingContext.request().getParam("page")).thenReturn("1");
    when(routingContext.request().getParam("limit")).thenReturn("10");
  }

  @Test
  void testMap_success() {
    JsonArray resultsArray = new JsonArray()
      .add(new JsonObject().put("id", 1).put("name", "Item A"))
      .add(new JsonObject().put("id", 2).put("name", "Item B"));

    JsonObject response = new JsonObject()
      .put("result", new JsonObject().put("results", resultsArray));

    when(hubSearchClient.getAllCatalogues()).thenReturn( Future.succeededFuture(response));

    CatalogueMapper.map(catalogue, catalogueMapList, routingContext,
      countriesManager, hubSearchClient)
      .onSuccess(result -> {
        assertNotNull(result);
        assertTrue(result.containsKey("catalogueList"));
        assertTrue(result.containsKey("sizeOfCatalogueList"));
    });
  }


  @Test
  void testMapCatalogue_success() {
    String catalogueId = "123";
    JsonObject result = CatalogueMapper.mapCatalogue(catalogue, catalogueMapList,
      countriesManager, catalogueId);

    assertNotNull(result);
    assertTrue(result.containsKey("catalogueId"));
  }

  @Test
  void testMapToCatalogueList_success() {
    CatalogueDTO result = CatalogueMapper.mapToCatalogueList(catalogue, catalogueMapList,
      countriesManager);

    assertNotNull(result);
    assertEquals("Catalogue Title", result.getCatalogueName());
  }

  @Test
  void testMapToCatalogueList_withStringTitleAndDescription() {
    JsonObject item = new JsonObject()
      .put("dct:title", "Catalogue Title")
      .put("dct:description", "Catalogue Description");
    JsonObject catalogueWithStringValues = new JsonObject().put(
      "@graph",
      new JsonArray().add(item)
    );

    CatalogueDTO result = CatalogueMapper.mapToCatalogueList(
      catalogueWithStringValues,
      catalogueMapList,
      countriesManager
    );

    assertNotNull(result);
    assertEquals("Catalogue Title", result.getCatalogueName());
    assertEquals("Catalogue Description", result.getDescription());
  }

  @Test
  void testDispatcherMapToCatalogue_success() {
    CatalogueDTO catalogueDTO = new CatalogueDTO();
    catalogueDTO.setCatalogueId("4507af72-fb13-4cde-ad71-f7b715a44262");

    CatalogueMapper.dispatcherMapToCatalogue(catalogueDTO, catalogueMapList);

    assertNotNull(catalogueDTO.getDomibusId());
    assertNotNull(catalogueDTO.getStatus());
  }

  @Test
  void testFilterCatalogueList_withGeoCoverageFilter() {
    when(routingContext.request().getParam("sortBy")).thenReturn("catalogueName");
    when(routingContext.request().getParam("sortOrder")).thenReturn("desc");

    JsonObject searchFields = new JsonObject();
    searchFields.put("geoCoverageKeys", List.of("EU"));
    RequestBody bodyMock = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(bodyMock);
    when(bodyMock.asJsonObject()).thenReturn(
      new JsonObject().put("geoCoverageKeys", new JsonArray().add("EU"))
        .put("status", new JsonArray().add("test"))
    );
    CatalogueDTO catalogueDTO = new CatalogueDTO();
    catalogueDTO.setStatus(new SelectObjectDTO("test","test"));


    List<CatalogueDTO> catalogueList = List.of(catalogueDTO,catalogueDTO);
    List<CatalogueDTO> filteredList = CatalogueMapper.filterCatalogueList(routingContext,
      catalogueList);

    assertNotNull(filteredList);
  }

  @Test
  void testFilterCatalogueList_withGeoCoverageFilter2() {
    when(routingContext.request().getParam("sortBy")).thenReturn("catalogueName");
    when(routingContext.request().getParam("sortOrder")).thenReturn("desc");

    JsonObject searchFields = new JsonObject();
    searchFields.put("geoCoverageKeys", List.of("EU"));
    RequestBody bodyMock = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(bodyMock);
    when(bodyMock.asJsonObject()).thenReturn(
      new JsonObject().put("geoCoverageKeys", new JsonArray().add("EU")));

    List<CatalogueDTO> filteredList = CatalogueMapper.filterCatalogueList(routingContext,
      null);

    assertNull(filteredList);
  }

  @Test
  void testFilterCatalogueList_withGeoCoverageFilter3() {
    when(routingContext.request().getParam("sortBy")).thenReturn(null);
    when(routingContext.request().getParam("sortOrder")).thenReturn("desc");

    JsonObject searchFields = new JsonObject();
    searchFields.put("geoCoverageKeys", List.of("EU"));
    RequestBody bodyMock = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(bodyMock);
    when(bodyMock.asJsonObject()).thenReturn(
      new JsonObject().put("geoCoverageKeys", new JsonArray().add("EU"))
        .put("status", new JsonArray().add("test"))
    );
    CatalogueDTO catalogueDTO = new CatalogueDTO();
    catalogueDTO.setStatus(new SelectObjectDTO("test","test"));


    List<CatalogueDTO> catalogueList = List.of(catalogueDTO,catalogueDTO);
    List<CatalogueDTO> filteredList = CatalogueMapper.filterCatalogueList(routingContext,
      catalogueList);

    assertNotNull(filteredList);
  }


  @Test
  void testExistsCatalogueByDomibusIdOrCatalogueIdOrCountry() {
    JsonObject uniqueFieldsDTO = new JsonObject().put("domibusId", "123")
      .put("catalogueId", "4507af72-fb13-4cde-ad71-f7b715a44262")
      .put("geographicalCoverage", "EU");

    JsonObject result = CatalogueMapper.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(catalogue,
      catalogueMapList, countriesManager, uniqueFieldsDTO, "4507af72-fb13-4cde-ad71-f7b715a44262");

    assertNull(result);
  }

  @Test
  void testExistsCatalogueByDomibusIdOrCatalogueIdOrCountry_withCatalogueIdMatch() {
      JsonObject uniqueFieldsDTO = new JsonObject().put("catalogueId", "4507af72-fb13-4cde-ad71-f7b715a44262");

      JsonObject result = CatalogueMapper.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(catalogue,
              catalogueMapList, countriesManager, uniqueFieldsDTO, null);

      assertNotNull(result);
      assertEquals("4507af72-fb13-4cde-ad71-f7b715a44262", result.getString("catalogueId"));
  }

  @Test
  void testExistsCatalogueByDomibusIdOrCatalogueIdOrCountry_noMatch() {
      JsonObject uniqueFieldsDTO = new JsonObject().put("domibusId", "non-existing-id")
              .put("catalogueId", "non-existing-id")
              .put("geographicalCoverage", "non-existing-coverage");

      JsonObject result = CatalogueMapper.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(catalogue,
              catalogueMapList, countriesManager, uniqueFieldsDTO, null);

      assertNull(result);
  }

  @Test
  void testMapGeographicalCoverageList_success() {
    JsonObject result = CatalogueMapper.mapGeographicalCoverageList(catalogue, countriesManager);

    assertNotNull(result);
    assertTrue(result.containsKey("geographicalCoverageList"));

    JsonArray geoList = result.getJsonArray("geographicalCoverageList");
    assertEquals(1, geoList.size());

    assertTrue(geoList.getString(0)
      .contains("SelectObjectDTO(key=http://example.com/location/EUR, value=European Union)"));
  }

  @Test
  void testHandleDates_singleJsonObject_returnsDateValue() {
    String expectedDate = OffsetDateTime.now().toString();
    JsonObject item = new JsonObject()
      .put("dct:issued", new JsonObject().put("@value", expectedDate));

    String result = CatalogueMapper.handleDates(item, "dct:issued", true);

    assertEquals(expectedDate, result);
  }

  @Test
  void testHandleDates_jsonArray_returnsMostRecentDate() {
    OffsetDateTime oldDate = OffsetDateTime.now().minusDays(5);
    OffsetDateTime recentDate = OffsetDateTime.now();

    JsonArray array = new JsonArray()
      .add(new JsonObject().put("@value", oldDate.toString()))
      .add(new JsonObject().put("@value", recentDate.toString()));

    JsonObject item = new JsonObject().put("dct:issued", array);

    String result = CatalogueMapper.handleDates(item, "dct:issued", true);
    assertEquals(recentDate.toString(), result);
  }

  @Test
  void testHandleDates_jsonArray_returnsOldestDate() {
    OffsetDateTime oldDate = OffsetDateTime.now().minusDays(5);
    OffsetDateTime recentDate = OffsetDateTime.now();

    JsonArray array = new JsonArray()
      .add(new JsonObject().put("@value", recentDate.toString()))
      .add(new JsonObject().put("@value", oldDate.toString()));

    JsonObject item = new JsonObject().put("dct:issued", array);

    String result = CatalogueMapper.handleDates(item, "dct:issued", false);
    assertEquals(oldDate.toString(), result);
  }

  @Test
  void testHandleDates_jsonArray_withInvalidDate_ignoresInvalid() {
    OffsetDateTime validDate = OffsetDateTime.now();
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("@value", "invalid-date"))
      .add(new JsonObject().put("@value", validDate.toString()));

    JsonObject item = new JsonObject().put("dct:issued", array);

    String result = CatalogueMapper.handleDates(item, "dct:issued", true);
    assertEquals(validDate.toString(), result);
  }

  @Test
  void testHandleDates_nullField_returnsNull() {
    JsonObject item = new JsonObject();
    String result = CatalogueMapper.handleDates(item, "dct:issued", true);
    assertNull(result);
  }

  @Test
  void testHandleDates_emptyArray_returnsNull() {
    JsonObject item = new JsonObject().put("dct:issued", new JsonArray());
    String result = CatalogueMapper.handleDates(item, "dct:issued", true);
    assertNull(result);
  }

}
