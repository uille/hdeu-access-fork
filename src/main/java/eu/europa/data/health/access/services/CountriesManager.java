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

import static eu.europa.data.health.access.Constants.COUNTRIES_ALL;
import static eu.europa.data.health.access.Constants.COUNTRIES_EU_EEA;
import static eu.europa.data.health.access.Constants.COUNTRIES_NO_EU;
import static eu.europa.data.health.access.Constants.COUNTRY;
import static eu.europa.data.health.access.Constants.COUNTRY_AT_DEPRECATED;
import static eu.europa.data.health.access.Constants.COUNTRY_EU;
import static eu.europa.data.health.access.Constants.COUNTRY_SKOS_NOTATION;
import static eu.europa.data.health.access.Constants.COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME;
import static eu.europa.data.health.access.Constants.FILTER_BOUND_DEPRECATED;
import static eu.europa.data.health.access.Constants.FILTER_LANG_COUNTRY_NAME;
import static eu.europa.data.health.access.Constants.NOTATION;
import static eu.europa.data.health.access.Constants.OPTIONAL;
import static eu.europa.data.health.access.Constants.PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY;
import static eu.europa.data.health.access.Constants.PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE;
import static eu.europa.data.health.access.Constants.SELECT_COUNTRY_NAME_COUNTRY_NOTATION_WHERE;
import static eu.europa.data.health.access.Constants.SELECT_COUNTRY_NAME_COUNTRY_WHERE;
import static eu.europa.data.health.access.SchemaFiles.COUNTRIES_RDF;

import eu.europa.data.health.access.SchemaFiles;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service for get the countries
 */
public class CountriesManager {

  private final Logger log = LoggerFactory.getLogger(getClass());


  /**
   * Method where read rdf countries file and filter them by languish and request filter (all, noEE=
   * no european countries, euEEA = europe and 3 extra countries LIE, NOR, ISL)
   */
  public void getCountries(RoutingContext context) {
    //to languish that we need return the country
    String lg = context.pathParam("lg");
    //which query we need to create
    String filter = context.pathParam("filter");
    try (InputStream rdfFile = getResourcePath(COUNTRIES_RDF.getValue())) {
      Model model = ModelFactory.createDefaultModel();
      RDFDataMgr.read(model, rdfFile, Lang.RDFXML);
      JsonArray countriesList = new JsonArray();
      String sparqlQuery = "";

      switch (filter) {
        case COUNTRIES_ALL:
          sparqlQuery =
            PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
              PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
              SELECT_COUNTRY_NAME_COUNTRY_WHERE +
              COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
              OPTIONAL +
              COUNTRY_AT_DEPRECATED +
              "  } " +
              OPTIONAL +
              "    ?country skos:topConceptOf ?topConcept . " +
              "  } " +
              FILTER_LANG_COUNTRY_NAME + lg + "') " +
              FILTER_BOUND_DEPRECATED +
              "  FILTER (?topConcept = <http://publications.europa.eu/resource/authority/country/0001>) "
              +
              "}";
          break;
        case COUNTRIES_NO_EU:
          sparqlQuery =
            PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
              PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
              SELECT_COUNTRY_NAME_COUNTRY_WHERE +
              COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
              OPTIONAL +
              COUNTRY_AT_DEPRECATED +
              "  } " +
              OPTIONAL +
              "    ?country at:protocol.ordering ?orderingValue . " +
              "  } " +
              OPTIONAL +
              "    ?country skos:topConceptOf ?topConcept . " +
              "  } " +
              FILTER_LANG_COUNTRY_NAME + lg + "') " +
              FILTER_BOUND_DEPRECATED +
              "  FILTER (!bound(?orderingValue))" +
              "  FILTER (?country NOT IN (" +
              " <http://publications.europa.eu/resource/authority/country/LIE>," +
              " <http://publications.europa.eu/resource/authority/country/NOR>, " +
              " <http://publications.europa.eu/resource/authority/country/ISL>)) " +
              "  FILTER (?topConcept = <http://publications.europa.eu/resource/authority/country/0001>) "
              +
              "}";
          break;
        case COUNTRIES_EU_EEA:
          sparqlQuery =
            PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
              PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
              SELECT_COUNTRY_NAME_COUNTRY_WHERE +
              COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
              OPTIONAL +
              COUNTRY_AT_DEPRECATED +
              "  } " +
              OPTIONAL +
              "    ?country at:protocol.ordering ?orderingValue . " +
              "  } " +
              FILTER_LANG_COUNTRY_NAME + lg + "') " +
              FILTER_BOUND_DEPRECATED +
              "  FILTER (bound(?orderingValue))" +
              "}";
          //an extra query for get the EEA countries where they are not include on EU
          //(Liechtenstein - (EEA), Norway - (EEA), Iceland - (EEA)
          String sparqlQueryEEA =
            PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
              PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
              SELECT_COUNTRY_NAME_COUNTRY_WHERE +
              COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
              OPTIONAL +
              COUNTRY_AT_DEPRECATED +
              "  } " +
              "  FILTER (?country IN (" +
              " <http://publications.europa.eu/resource/authority/country/LIE>," +
              " <http://publications.europa.eu/resource/authority/country/NOR>, " +
              " <http://publications.europa.eu/resource/authority/country/ISL>)) " +
              FILTER_LANG_COUNTRY_NAME + lg + "') " +
              FILTER_BOUND_DEPRECATED +
              "}";
          Query queryEEA = QueryFactory.create(sparqlQueryEEA);
          getResults(queryEEA, model, countriesList);
          break;
        default:
          log.error("Invalid filter query: {}", filter);
          context.response().setStatusCode(400).end("Invalid filter query: " + filter);
          return;
      }
      Query query = QueryFactory.create(sparqlQuery);
      getResults(query, model, countriesList);
      // Sort the JsonArray by the "name" field in ascending order
      JsonArray sortedCountries = countriesList.stream()
        .sorted(Comparator.comparing(o -> ((JsonObject) o).getString("name")))
        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

      context.response().putHeader("content-type", "application/json");
      context.response().setStatusCode(200).end(sortedCountries.encode());
    } catch (IOException e) {
      log.info("RDF file not found.", e);
      context.response().setStatusCode(404).end("RDF file not found.");
    }

  }

  /**
   * Method to get countries from a preloaded Apache Jena model and filter them by language and
   * scope
   *
   * @param model  the rdf mode
   * @param lang   the selected language
   * @param filter the selected scope to filter by (all, noEE, euEEA)
   * @return a json array with the results based on the provided scope: 1) all = all countries 2)
   * noEE = non eu countries 3) euEEA = European Economic Area (EEA), eu and 3 extra countries (LIE,
   * NOR, ISL) 4) euCountry = get European Union country
   */
  public JsonArray getCountriesJsonArray(Model model, String lang, String filter) {
    JsonArray countriesList = new JsonArray();
    String sparqlQuery = "";

    switch (filter) {
      case COUNTRIES_ALL:
        sparqlQuery =
          PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
            PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
            SELECT_COUNTRY_NAME_COUNTRY_NOTATION_WHERE +
            COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
            COUNTRY_SKOS_NOTATION +
            OPTIONAL +
            COUNTRY_AT_DEPRECATED +
            "  } " +
            OPTIONAL +
            "    ?country skos:topConceptOf ?topConcept . " +
            "  } " +
            FILTER_LANG_COUNTRY_NAME + lang + "') " +
            FILTER_BOUND_DEPRECATED +
            "  FILTER (?topConcept = <http://publications.europa.eu/resource/authority/country/0001>) "
            +
            "}";
        break;
      case COUNTRIES_NO_EU:
        sparqlQuery =
          PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
            PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
            SELECT_COUNTRY_NAME_COUNTRY_WHERE +
            COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
            OPTIONAL +
            COUNTRY_AT_DEPRECATED +
            "  } " +
            OPTIONAL +
            "    ?country at:protocol.ordering ?orderingValue . " +
            "  } " +
            OPTIONAL +
            "    ?country skos:topConceptOf ?topConcept . " +
            "  } " +
            FILTER_LANG_COUNTRY_NAME + lang + "') " +
            FILTER_BOUND_DEPRECATED +
            "  FILTER (!bound(?orderingValue))" +
            "  FILTER (?country NOT IN (" +
            " <http://publications.europa.eu/resource/authority/country/LIE>," +
            " <http://publications.europa.eu/resource/authority/country/NOR>, " +
            " <http://publications.europa.eu/resource/authority/country/ISL>)) " +
            "  FILTER (?topConcept = <http://publications.europa.eu/resource/authority/country/0001>) "
            +
            "}";
        break;
      case COUNTRIES_EU_EEA:
        sparqlQuery =
          PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
            PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
            SELECT_COUNTRY_NAME_COUNTRY_WHERE +
            COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
            OPTIONAL +
            COUNTRY_AT_DEPRECATED +
            "  } " +
            OPTIONAL +
            "    ?country at:protocol.ordering ?orderingValue . " +
            "  } " +
            FILTER_LANG_COUNTRY_NAME + lang + "') " +
            FILTER_BOUND_DEPRECATED +
            "  FILTER (bound(?orderingValue))" +
            "}";
        //an extra query for get the EEA countries where they are not include on EU
        //(Liechtenstein - (EEA), Norway - (EEA), Iceland - (EEA)
        String sparqlQueryEEA =
          PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
            PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
            SELECT_COUNTRY_NAME_COUNTRY_WHERE +
            COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
            OPTIONAL +
            COUNTRY_AT_DEPRECATED +
            "  } " +
            "  FILTER (?country IN (" +
            " <http://publications.europa.eu/resource/authority/country/LIE>," +
            " <http://publications.europa.eu/resource/authority/country/NOR>, " +
            " <http://publications.europa.eu/resource/authority/country/ISL>)) " +
            FILTER_LANG_COUNTRY_NAME + lang + "') " +
            FILTER_BOUND_DEPRECATED +
            "}";
        Query queryEEA = QueryFactory.create(sparqlQueryEEA);
        getResults(queryEEA, model, countriesList);
        break;
      case COUNTRY_EU:
        sparqlQuery =
          PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
            PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
            SELECT_COUNTRY_NAME_COUNTRY_NOTATION_WHERE +
            COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
            COUNTRY_SKOS_NOTATION +
            OPTIONAL +
            COUNTRY_AT_DEPRECATED +
            "  } " +
            "  FILTER (?country IN (" +
            " <http://publications.europa.eu/resource/authority/country/EUR>)) " +
            FILTER_LANG_COUNTRY_NAME + lang + "') " +
            FILTER_BOUND_DEPRECATED +
            "}";
        break;
      default:
        log.error("Invalid filter query: {}", filter);
        return countriesList;
    }
    Query query = QueryFactory.create(sparqlQuery);
    getResults(query, model, countriesList);
    // Sort the JsonArray by the "name" field in ascending order
    return countriesList.stream()
      .sorted(Comparator.comparing(o -> ((JsonObject) o).getString("name")))
      .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
  }

  /**
   * Find a country by the country code.
   *
   * @param countryCode The country code of the country to receive.
   * @return The country with the given country code.
   */
  public JsonObject getCountryJsonByCountryCode(String countryCode) {
    try (InputStream rdfFile = getResourcePath(COUNTRIES_RDF.getValue())) {
      Model model = ModelFactory.createDefaultModel();
      RDFDataMgr.read(model, rdfFile, Lang.RDFXML);

      String sparqlQuery =
        PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE +
          PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY +
          SELECT_COUNTRY_NAME_COUNTRY_WHERE +
          // First, select the country from the concept scheme using skos:hasTopConcept
          " <http://publications.europa.eu/resource/authority/country/0001> skos:hasTopConcept ?country . "
          +
          COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME +
          OPTIONAL +
          COUNTRY_AT_DEPRECATED +
          "  } " +
          OPTIONAL +
          "    ?country skos:topConceptOf ?topConcept . " +
          "  } " +
          FILTER_LANG_COUNTRY_NAME + "en" + "') " +
          FILTER_BOUND_DEPRECATED +
          // Then, filter the country by its code extracted from its URI
          " FILTER (STRAFTER(STR(?country), 'http://publications.europa.eu/resource/authority/country/') = '"
          + countryCode + "') " +
          "}";

      Query queryEEA = QueryFactory.create(sparqlQuery);
      return getCountry(queryEEA, model, countryCode);
    } catch (Exception e) {
      log.info(e.getMessage());
      return null;
    }
  }

  private JsonObject getCountry(Query query, Model model, String countryCode) {
    try (QueryExecution qExec = QueryExecutionFactory.create(query, model)) {
      ResultSet results = qExec.execSelect();

      while (results.hasNext()) {
        QuerySolution solution = results.nextSolution();
        String countryName = solution.getLiteral("countryName").getString();
        String country = solution.getResource(COUNTRY).getURI();
        if (country.substring(country.lastIndexOf('/') + 1).equals(countryCode)) {
          return new JsonObject().put("name", countryName).put(COUNTRY, country);
        }
      }
    } catch (Exception e) {
      log.info(e.getMessage());
    }
    return null;
  }

  private void getResults(Query query, Model model, JsonArray countriesList) {
    try (QueryExecution qExec = QueryExecutionFactory.create(query, model)) {
      ResultSet results = qExec.execSelect();

      while (results.hasNext()) {
        QuerySolution solution = results.nextSolution();
        String countryName = solution.getLiteral("countryName").getString();
        String country = solution.getResource(COUNTRY).getURI();

        String isoCode = null;
        if (solution.contains(NOTATION) && solution.get(NOTATION).isLiteral()) {
          isoCode = solution.getLiteral(NOTATION).getString();
        }

        JsonObject countryJson = new JsonObject()
          .put("name", countryName)
          .put(COUNTRY, country);

        if (isoCode != null) {
          countryJson.put("isoCode", isoCode);
        }

        countriesList.add(countryJson);
      }
    } catch (Exception e) {
      log.info(e.getMessage());
    }
  }

  private InputStream getResourcePath(String relativePath) {
    return SchemaFiles.class.getClassLoader().getResourceAsStream(relativePath);
  }

}
