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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.RoutingContextMocker;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals(Constants.COUNTRIES_ALL, routingContext.pathParam("filter"));

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
    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals(Constants.COUNTRIES_NO_EU, routingContext.pathParam("filter"));

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
    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals(Constants.COUNTRIES_EU_EEA, routingContext.pathParam("filter"));

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

    Assertions.assertEquals("en", routingContext.pathParam("lg"));
    Assertions.assertEquals("COUNTRIES_INVALID", routingContext.pathParam("filter"));

    countriesManager.getCountries(routingContext);

    verify(routingContext.response()).setStatusCode(400);
    verify(routingContext.response()).end("Invalid filter query: COUNTRIES_INVALID");
  }

}
