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
package eu.europa.data.health.access.services;


import static eu.europa.data.health.access.SchemaFiles.COUNTRIES_RDF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.SchemaFiles;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class CountriesManagerTest {

  private CountriesManager countriesManager;


  @BeforeEach
  void setUp() {
    countriesManager = new CountriesManager();
  }

  @Test
  void testGetCountries_AllCountries_RealFile() {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "filter", Constants.COUNTRIES_ALL
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null,
      pathParams, true);
    assertEquals("en", routingContext.pathParam("lg"));
    assertEquals(Constants.COUNTRIES_ALL, routingContext.pathParam("filter"));

    countriesManager.getCountries(routingContext);

    verify(routingContext.response()).setStatusCode(200);
    verify(routingContext.response()).end(anyString());
  }

  @Test
  void testGetCountriesNoEuCountries_RealFile() {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "filter", Constants.COUNTRIES_NO_EU
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null,
      pathParams, true);
    assertEquals("en", routingContext.pathParam("lg"));
    assertEquals(Constants.COUNTRIES_NO_EU, routingContext.pathParam("filter"));

    countriesManager.getCountries(routingContext);

    verify(routingContext.response()).setStatusCode(200);
    verify(routingContext.response()).end(anyString());
  }

  @Test
  void testGetCountriesEuEeaCountries_RealFile() {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "filter", Constants.COUNTRIES_EU_EEA
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null,
      pathParams, true);
    assertEquals("en", routingContext.pathParam("lg"));
    assertEquals(Constants.COUNTRIES_EU_EEA, routingContext.pathParam("filter"));

    countriesManager.getCountries(routingContext);

    verify(routingContext.response()).setStatusCode(200);
    verify(routingContext.response()).end(anyString());
  }

  @Test
  void testGetCountriesInvalidQuery() {
    Map<String, String> pathParams = Map.of(
      "lg", "en",
      "filter", "COUNTRIES_INVALID"//Use an invalid filter
    );

    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(null, pathParams, true);

    assertEquals("en", routingContext.pathParam("lg"));
    assertEquals("COUNTRIES_INVALID", routingContext.pathParam("filter"));

    countriesManager.getCountries(routingContext);

    verify(routingContext.response()).setStatusCode(400);
    verify(routingContext.response()).end("Invalid filter query: COUNTRIES_INVALID");
  }

  @Test
  void testGetCountriesJsonArray_allCountries_returnsResults() {
    Model model = getCountriesModel();
    JsonArray result = countriesManager.getCountriesJsonArray(model, "en", Constants.COUNTRIES_ALL);

    assertNotNull(result);
    assertFalse(result.isEmpty(), "Should return countries for filter 'all'");
    assertTrue(result.getJsonObject(0).containsKey("name"));
    assertTrue(result.getJsonObject(0).containsKey("country"));
  }

  @Test
  void testGetCountriesJsonArray_noEu_returnsResults() {
    Model model = getCountriesModel();
    JsonArray result = countriesManager.getCountriesJsonArray(model, "en", Constants.COUNTRIES_NO_EU);

    assertNotNull(result);
    assertFalse(result.isEmpty(), "Should return non-EU countries");
  }

  @Test
  void testGetCountriesJsonArray_euEea_returnsResultsIncludingLIE_NOR_ISL() {
    Model model = getCountriesModel();
    JsonArray result = countriesManager.getCountriesJsonArray(model, "en", Constants.COUNTRIES_EU_EEA);

    assertNotNull(result);
    assertFalse(result.isEmpty(), "Should return EU and EEA countries");

    List<String> expectedEEA = List.of("LIE", "NOR", "ISL");
    List<String> returnedCodes = result.stream()
      .map(obj -> ((JsonObject) obj).getString("country"))
      .map(uri -> uri.substring(uri.lastIndexOf('/') + 1))
      .toList();

    assertTrue(returnedCodes.containsAll(expectedEEA), "Should include EEA countries");
  }

  @Test
  void testGetCountriesJsonArray_invalidFilter_returnsEmpty() {
    Model model = getCountriesModel();
    JsonArray result = countriesManager.getCountriesJsonArray(model, "en", "INVALID_FILTER");

    assertNotNull(result);
    assertTrue(result.isEmpty(), "Invalid filter should return empty result");
  }

  @Test
  void testGetCountryJsonByCode_validCountry_returnsCorrectCountry() {
    JsonObject result = countriesManager.getCountryJsonByCountryCode("FRA");

    assertNotNull(result);
    assertEquals("France", result.getString("name"));
    assertTrue(result.getString("country").endsWith("/FRA"));
  }

  @Test
  void testGetCountryJsonByCode_invalidCountry_returnsNull() {
    JsonObject result = countriesManager.getCountryJsonByCountryCode("INVALID");

    assertNull(result, "Invalid country code should return null");
  }

  /**
   * Helper method to load the RDF model from the real RDF resource file.
   * Assumes COUNTRIES_RDF.getValue() returns a valid file path or stream.
   */
  private Model getCountriesModel() {
    try (InputStream rdfFile = SchemaFiles.class.getClassLoader()
      .getResourceAsStream(COUNTRIES_RDF.getValue())) {
      Model model = ModelFactory.createDefaultModel();
      RDFDataMgr.read(model, Objects.requireNonNull(rdfFile), Lang.RDFXML);
      return model;
    } catch (
      IOException e) {
      return null;
    }
  }

}
