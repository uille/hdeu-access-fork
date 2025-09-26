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

import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.GEOGRAPHICAL_COVERAGE;

import eu.europa.data.health.access.dto.CatalogueDTO;
import eu.europa.data.health.access.dto.CatalogueStatusEnum;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.HubSearchClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CatalogueMapper {

  public static final String VALUE = "@value";

  public static Future<JsonObject> map(JsonObject catalogues, JsonObject catalogueMapList,
    RoutingContext context, CountriesManager countriesManager, HubSearchClient hubSearchClient) {

    Promise<JsonObject> promise = Promise.promise();

    List<CatalogueDTO> catalogueDTOs = new ArrayList<>();

    JsonArray graph = catalogues.getJsonArray("@graph");
    if (graph == null) {
      return Future.succeededFuture(null);
    }
    JsonObject firstElement = graph.getJsonObject(0);

    if (firstElement.getJsonArray("@graph") != null) {
      // JsonObject is a list
      for (int i = 0; i < graph.size(); i++) {
        catalogueDTOs.add(
          mapToCatalogueList(graph.getJsonObject(i), catalogueMapList, countriesManager));
      }
    } else {
      catalogueDTOs.add(mapToCatalogueList(catalogues, catalogueMapList, countriesManager));
    }

    //Add external code here
    hubSearchClient.getAllCatalogues().onSuccess(response -> {
      Map<String, Integer> catalogueIdsWithCountsMap = getCatalogueCounts(
        response.getJsonObject("result").getJsonArray("results"));

      // Update DTOs with count values
      catalogueDTOs.forEach(dto -> {
        Integer count = catalogueIdsWithCountsMap.get(dto.getCatalogueId());
        if (count != null) {
          dto.setDatasets(count);
        }
      });

      List<CatalogueDTO> filteredList = filterCatalogueList(context, catalogueDTOs);
      List<CatalogueDTO> finalList = filterCatalogueListPageAndItems(context, filteredList);

      JsonObject result = new JsonObject()
        .put("catalogueList", convertDtoListToJsonObject(finalList))
        .put("sizeOfCatalogueList", filteredList != null ? filteredList.size() : 0);

      promise.complete(result);

    }).onFailure(cause -> {
      context.response().setStatusCode(500).end("No catalogues found from hub search.");
      promise.fail(cause);
    });

    return promise.future();
  }

  private static @NotNull Map<String, Integer> getCatalogueCounts(JsonArray cataloguesFromHubSearch) {
    Map<String, Integer> catalogueCounts = new HashMap<>();
    for (Object obj : cataloguesFromHubSearch) {
      if (obj instanceof JsonObject cat) {
        String id = cat.getString("id");
        Integer count = cat.getInteger("count", 0);
        if (id != null) {
          catalogueCounts.put(id, count);
        }
      }
    }
    return catalogueCounts;
  }

  public static JsonObject mapCatalogue(JsonObject catalogue, JsonObject catalogueMapList
    , CountriesManager countriesManager, String catalogueId) {
    CatalogueDTO catalogueDTO;

    JsonArray graph = catalogue.getJsonArray("@graph");
    if (graph == null) {
      return null;
    }

    catalogueDTO = mapToCatalogue(catalogue, catalogueMapList, countriesManager, catalogueId);

    return convertDtoToJsonObject(catalogueDTO);
  }

  public static CatalogueDTO mapToCatalogue(JsonObject jsonObject, JsonObject catalogueMapList,
    CountriesManager countriesManager, String catalogueId) {
    return commonMapToCatalogue(jsonObject, catalogueMapList,
      countriesManager, catalogueId, false);
  }

  public static CatalogueDTO mapToCatalogueList(JsonObject jsonObject, JsonObject catalogueMapList,
    CountriesManager countriesManager) {
    return commonMapToCatalogue(jsonObject, catalogueMapList,
      countriesManager, null, true);
  }

  public static String handleDates(JsonObject item, String fieldName, boolean findMostRecent) {
    Object field = item.getValue(fieldName);

    if (field instanceof JsonObject) {
      JsonObject fieldObject = item.getJsonObject(fieldName);
      if(fieldObject != null){
        return fieldObject.getString(VALUE);
      }
    } else if (field instanceof JsonArray) {
      JsonArray fieldArray = item.getJsonArray(fieldName);
      List<String> valueItems = new ArrayList<>();
      for (int i = 0; i < fieldArray.size(); i++) {
        JsonObject entry = fieldArray.getJsonObject(i);
        String value = entry.getString(VALUE);
        if(value != null){
          valueItems.add(value);
        }
      }
      return findDate(valueItems, findMostRecent);
    }
    return null;
  }

  private static String findDate(List<String> dateStrings, boolean findMostRecent) {
    if (dateStrings == null || dateStrings.isEmpty()) {
      return null;
    }

    OffsetDateTime bestMatch = null;

    for (String dateStr : dateStrings) {
      try {
        OffsetDateTime currentDate = OffsetDateTime.parse(dateStr);
        if (bestMatch == null ||
          (findMostRecent && currentDate.isAfter(bestMatch)) ||
          (!findMostRecent && currentDate.isBefore(bestMatch))) {
          bestMatch = currentDate;
        }
      } catch (DateTimeParseException ignored) {
        // Ignore invalid dates and continue
      }
    }

    return bestMatch != null ? bestMatch.toString() : null;
  }

  public static void dispatcherMapToCatalogue(CatalogueDTO dto, JsonObject catalogueMapList) {
    JsonArray data = catalogueMapList.getJsonArray("data");
    for (Object obj : data) {
      if (obj instanceof JsonObject jsonObj) {
        JsonArray catalogueIds = jsonObj.getJsonArray("catalogIds");
        SelectObjectDTO status = new SelectObjectDTO();
        if (catalogueIds.contains(dto.getCatalogueId())) {
          dto.setDomibusId(jsonObj.getString("recipient"));
          Boolean isProduction = jsonObj.getBoolean("isProduction");
          status.setKey(isProduction ? "live" : "test");
          status.setValue(isProduction ? CatalogueStatusEnum.LIVE.getValue()
            :  CatalogueStatusEnum.TEST.getValue());
          dto.setStatus(status);
        }
      }
    }
  }

  public static List<CatalogueDTO> filterCatalogueList(RoutingContext context,
    List<CatalogueDTO> catalogueDTOList) {
    if(catalogueDTOList == null || catalogueDTOList.isEmpty()) {
      return null;
    }

    String sortByString = context.request().getParam("sortBy");
    String sortOrderString = context.request().getParam("sortOrder");

    JsonObject searchFields = context.body().asJsonObject();
    List<String> geoCoverageList = convertJsonArrayToListString(searchFields.getJsonArray("geoCoverageKeys"));
    List<String>  statusList = convertJsonArrayToListString(searchFields.getJsonArray("status"));

    return catalogueDTOList.stream()
      .filter(catalogue -> {
        boolean matchGeo = true;
        boolean matchStatus = true;

        if (geoCoverageList != null && !geoCoverageList.isEmpty() && catalogue.getGeographicalCoverage() != null) {
          matchGeo = geoCoverageList.contains(catalogue.getGeographicalCoverage().getKey());
        }

        if (statusList != null && !statusList.isEmpty() && catalogue.getStatus() != null) {
          matchStatus = statusList.contains(catalogue.getStatus().getKey().equals("test") ? CatalogueStatusEnum.TEST.getValue() : CatalogueStatusEnum.LIVE.getValue());
        }

        // Combine filters with AND logic
        return matchGeo && matchStatus;
      })
      .sorted((c1, c2) -> {
        if (sortByString == null || sortOrderString == null) {
          return 0;
        }
        try {
          int comparisonResult = c1.compareByField(sortByString, c2);
          return "desc".equalsIgnoreCase(sortOrderString) ? -comparisonResult : comparisonResult;
        } catch (Exception e) {
          throw new RuntimeException("Error during sorting", e);
        }
      })
      .collect(Collectors.toList());
  }

  private static List<CatalogueDTO> filterCatalogueListPageAndItems(RoutingContext context,
                                                                                       List<CatalogueDTO> catalogueDTOList) {
    if(catalogueDTOList == null || catalogueDTOList.isEmpty()) {
      return null;
    }

    String pageString = context.request().getParam("page");
    String limitString = context.request().getParam("limit");

    return catalogueDTOList.stream()
      .skip((long) (Integer.parseInt(pageString) - 1) * Integer.parseInt(limitString))
      .limit(Integer.parseInt(limitString))
      .collect(Collectors.toList());
  }

  private static List<String> convertJsonArrayToListString(JsonArray jsonArray){
    if(jsonArray == null) {
      return null;
    }
    List<String> list = new ArrayList<>();

    for (Object obj : jsonArray) {
      if (obj instanceof String) {
        list.add((String) obj);
      }
    }
    return list;
  }

  public static List<JsonObject> convertDtoListToJsonObject(List<CatalogueDTO> catalogueList) {
    List<JsonObject> catalogueDTOList = new ArrayList<>();

    if(catalogueList == null || catalogueList.isEmpty()) {
      return catalogueDTOList;
    }

    for (CatalogueDTO dto : catalogueList) {
      catalogueDTOList.add(convertDtoToJsonObject(dto));
    }
    return catalogueDTOList;
  }

  public static JsonObject convertDtoToJsonObject(CatalogueDTO dto) {
    return new JsonObject()
      .put("catalogueId", dto.getCatalogueId())
      .put("catalogueName", dto.getCatalogueName())
      .put("description", dto.getDescription())
      .put("description", dto.getDescription())
      .put("geographicalCoverage", dto.getGeographicalCoverage())
      .put("language", dto.getLanguage())
      .put("publisherHomepageURL", dto.getPublisherHomepageURL())
      .put("publisherName", dto.getPublisherName())
      .put("createdDate", dto.getCreatedDate())
      .put("modifiedDate", dto.getModifiedDate())
      .put("domibusId", dto.getDomibusId())
      .put("status", dto.getStatus())
      .put("datasets", dto.getDatasets());
  }

  public static JsonObject mapGeographicalCoverageList(JsonObject catalogues, CountriesManager countriesManager ){
      List<SelectObjectDTO> geographicalCoverageList = new ArrayList<>();

      JsonArray graph = catalogues.getJsonArray("@graph");
      if (graph == null) {
        return null;
      }
      JsonObject firstElement = graph.getJsonObject(0);

      if (firstElement.getJsonArray("@graph") != null) {
        // JsonObject is a list
        for (int i = 0; i < graph.size(); i++) {
          SelectObjectDTO country = mapToGeographicalCoverageList(graph.getJsonObject(i), countriesManager);
          if(country != null){
            geographicalCoverageList.add(country);
          }
        }
      } else {
        SelectObjectDTO country = mapToGeographicalCoverageList(catalogues, countriesManager);
        if(country != null){
          geographicalCoverageList.add(country);
        }

      }

      return new JsonObject().put("geographicalCoverageList", geographicalCoverageList);
  }

  private static SelectObjectDTO mapToGeographicalCoverageList(JsonObject catalogue,
                                                           CountriesManager countriesManager){
    // Extract the @graph array
    JsonArray graph = catalogue.getJsonArray("@graph");

    for (int i = 0; i < graph.size(); i++) {
      JsonObject item = graph.getJsonObject(i);

      // Check if "dct:spatial" exists before calling the function
      if (item.getJsonObject("dct:spatial") != null) {
        return mapToGeographicalCoverage(item, countriesManager);
      }
    }
    return null;
  }

  private static SelectObjectDTO mapToGeographicalCoverage(JsonObject item,
                                                           CountriesManager countriesManager) {
    if (item.getJsonObject("dct:spatial") != null) {
      SelectObjectDTO geographicalCoverage = new SelectObjectDTO();
      JsonObject countryObject = new JsonObject();

      String url = item.getJsonObject("dct:spatial").getString("@id");
      String countryCode = url.substring(url.lastIndexOf('/') + 1);
      if (countryCode.equals("EUR")) {
        countryObject.put("country", url);
        countryObject.put("name", "European Union");
      } else {
        countryObject = getCountryJsonObjectFromRdf(countryCode, countriesManager);
      }

      if(countryObject != null){
        geographicalCoverage.setKey(countryObject.getString("country"));
        geographicalCoverage.setValue( countryObject.getString("name") );
        return geographicalCoverage;
      }
      return null;
    }
    return null;
  }

  private static String extractUUIDFromURL(String url) {
    Pattern pattern = Pattern.compile(
      "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");
    Matcher matcher = pattern.matcher(url);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static JsonObject getCountryJsonObjectFromRdf(String countryCode,
    CountriesManager countriesManager) {
    return countriesManager.getCountryJsonByCountryCode(countryCode);
  }

  public static JsonObject existsCatalogueByDomibusIdOrCatalogueIdOrCountry(JsonObject catalogues,
                                                                         JsonObject catalogueMapList,
                                                                         CountriesManager countriesManager,
                                                                         JsonObject uniqueFieldsDTO, String catalogueIdToUpdate) {
      List<CatalogueDTO> catalogueDTOs = new ArrayList<>();

      JsonArray graph = catalogues.getJsonArray("@graph");
      if (graph == null) {
        return null;
      }
      JsonObject firstElement = graph.getJsonObject(0);

      if (firstElement.getJsonArray("@graph") != null) {
        // JsonObject is a list
        for (int i = 0; i < graph.size(); i++) {
          CatalogueDTO catalogue = mapToCatalogueList(graph.getJsonObject(i), catalogueMapList, countriesManager);
          // Exclude the catalogue that currently updated from the validation
          if(catalogueIdToUpdate == null || !catalogueIdToUpdate.equals(catalogue.getCatalogueId())){
            catalogueDTOs.add(catalogue);
          }
        }
      } else {
        CatalogueDTO catalogue = mapToCatalogueList(catalogues, catalogueMapList, countriesManager);
        // Exclude the catalogue that currently updated from the validation
        if(catalogueIdToUpdate == null || !catalogueIdToUpdate.equals(catalogue.getCatalogueId())){
          catalogueDTOs.add(catalogue);
        }
      }
      return findInTheList(catalogueDTOs, uniqueFieldsDTO);
  }

  private static JsonObject findInTheList(List<CatalogueDTO> list, JsonObject catalogue){
    JsonObject fieldWithValidation = new JsonObject();
    boolean atLeastOneError = false;
    if (list != null) {
      if (list.stream().anyMatch(item ->
        item.getDomibusId() != null && item.getDomibusId().equals(catalogue.getString(DOMIBUS_ID)))) {
        fieldWithValidation.put(DOMIBUS_ID, catalogue.getString(DOMIBUS_ID));
        atLeastOneError = true;
      }

      if(list.stream().anyMatch(item ->
        item.getCatalogueId() != null && item.getCatalogueId().equals(catalogue.getString(CATALOGUE_ID)))){
        fieldWithValidation.put(CATALOGUE_ID, catalogue.getString(CATALOGUE_ID));
        atLeastOneError = true;
      }
    }

    String url = catalogue.getString(GEOGRAPHICAL_COVERAGE);
      if (url != null && list != null && list.stream().anyMatch(item ->
        item.getGeographicalCoverage() != null &&
          item.getGeographicalCoverage().getKey() != null &&
          Objects.equals(item.getGeographicalCoverage().getKey(), url))) {
        fieldWithValidation.put("geographicalCoverage", url);
        atLeastOneError = true;
      }

    return  atLeastOneError ? fieldWithValidation: null;
  }

  public static CatalogueDTO commonMapToCatalogue(JsonObject jsonObject, JsonObject catalogueMapList,
    CountriesManager countriesManager, String catalogueId, boolean list) {
    CatalogueDTO dto = new CatalogueDTO();

    // Extract the @graph array
    JsonArray graph = jsonObject.getJsonArray("@graph");

    for (int i = 0; i < graph.size(); i++) {
      JsonObject item = graph.getJsonObject(i);

      // Set catalogue values
      if (item.getJsonObject("dct:title") != null) {
        dto.setCatalogueName(item.getJsonObject("dct:title").getString(VALUE));
      }

      // Set catalogue values
      if (item.getJsonObject("dct:description") != null) {
        dto.setDescription(item.getJsonObject("dct:description").getString(VALUE));
      }

      // Set geographical coverage
      SelectObjectDTO geographicalCoverage = mapToGeographicalCoverage(item, countriesManager);
      if(geographicalCoverage != null){
        dto.setGeographicalCoverage(geographicalCoverage);
      }

      // Set language
      String language = item.getJsonObject("dct:language") != null ?
        item.getJsonObject("dct:language").getString("@id") : null;
      if (language != null) {
        dto.setLanguage(language.substring(language.lastIndexOf('/') + 1));
      }

      // Set publisher homepage
      if (item.getJsonObject("foaf:homepage") != null) {
        dto.setPublisherHomepageURL(item.getJsonObject("foaf:homepage").getString("@id"));
      }

      // Set publisher name
      if (item.getString("foaf:name") != null) {
        dto.setPublisherName(item.getString("foaf:name"));
      }

      dto.setCreatedDate(dto.getCreatedDate() == null ? handleDates(item, "dct:issued", false) : dto.getModifiedDate());

      dto.setModifiedDate(dto.getModifiedDate() == null ? handleDates(item, "dct:modified", true) : dto.getModifiedDate());
    }

    if (list) {
      // Extract UUID from @id
      if (jsonObject.getString("@id") != null) {
        dto.setCatalogueId(extractUUIDFromURL(jsonObject.getString("@id")));

        dispatcherMapToCatalogue(dto, catalogueMapList);
      }
    } else {
      dto.setCatalogueId(catalogueId);
      dispatcherMapToCatalogue(dto, catalogueMapList);
    }

    return dto;
  }
}
