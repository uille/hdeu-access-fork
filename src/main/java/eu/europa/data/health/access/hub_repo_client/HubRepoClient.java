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
import static eu.europa.data.health.access.Constants.CATALOGUE_NAME;
import static eu.europa.data.health.access.Constants.DESCRIPTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.GEOGRAPHICAL_COVERAGE;
import static eu.europa.data.health.access.Constants.PUBLISHER_HOMEPAGE_URL;
import static eu.europa.data.health.access.Constants.PUBLISHER_NAME;
import static eu.europa.data.health.access.Constants.UPLOAD_RDF;
import static eu.europa.data.health.access.SchemaFiles.CATALOGUE_FORM_RDF;

import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AbstractManager;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.HubSearchClient;
import eu.europa.data.health.access.services.HubStoreClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hub repo client
 */
public class HubRepoClient extends AbstractManager {

  private final String url;
  private final String xApiKey;
  private final Logger log = LoggerFactory.getLogger(getClass());
  public static final String HUB_REPO_CLIENT = "HUB_REPO_CLIENT";
  public static final String CATALOGUE_REQUEST = "/catalogues";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_DETAILS = "errorDetails";

  WebClient vertxClient;
  private final DispatcherClient dispatcherClient;
  private final HubStoreClient hubStoreClient;
  private final HubSearchClient hubSearchClient;
  private final CountriesManager countriesManager;

  public HubRepoClient(DBHandler dbHandler, String url, String xApiKey, WebClient client, DispatcherClient dispatcherClient,
                       HubStoreClient hubStoreClient, HubSearchClient hubSearchClient, CountriesManager countriesManager) {
    super(dbHandler);
    this.url = url;
    this.xApiKey = xApiKey;
    this.vertxClient = client;
    this.dispatcherClient = dispatcherClient;
    this.hubStoreClient = hubStoreClient;
    this.hubSearchClient = hubSearchClient;
    this.countriesManager = countriesManager;
  }

  private void addXApiKey(MultiMap headers) {
    if (StringUtils.isNotEmpty(this.xApiKey)) {
      headers.add("X-API-Key", this.xApiKey);
    }
  }

  private MultiMap createHeader(String input) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("content-type", detectRdfLang(input));
    headers.add("content-length", String.valueOf(input.length()));
    addXApiKey(headers);
    return headers;
  }

  /**
   * Detect RDF serialization format by peeking at the very start
   * of the trimmed content and a few simple markers.
   *
   * @param rdfContent  the raw text of the RDF document
   * @return            the detected RDF MIME type
   */
  private String detectRdfLang(String rdfContent) {
    // Remove leading/trailing whitespace and lowercase once
    String text  = rdfContent.trim();
    String lower = text.toLowerCase();

    // 1) RDF/XML: starts with "<?xml"
    if (lower.startsWith("<?xml")) {
      return "application/rdf+xml";
    }

    // 2) JSON-LD: starts with "{"
    if (text.startsWith("{")) {
      return "application/ld+json";
    }

    // 3) Notation3 rules: looks for "=>" or N3-only keywords
    if (lower.contains("=>")
      || lower.contains("@forsome")
      || lower.contains("@forall")
      || lower.contains("@keywords")) {
      return "text/n3; charset=utf-8";
    }

    // 4) Bare "PREFIX " (no '@') → N3
    if (lower.startsWith("prefix ")) {
      return "text/n3; charset=utf-8";
    }

    // 5) "@prefix " → Turtle
    if (lower.startsWith("@prefix ")) {
      return "text/turtle";
    }

    // 6) Turtle shorthand: " a " for rdf:type anywhere
    if (lower.contains(" a ")) {
      return "text/turtle";
    }

    // 7) Fallback → N-Triples
    return "application/n-triples";
  }

  private MultiMap createHeaderWithoutType(){
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    addXApiKey(headers);
    return headers;
  }

  /**
   * Create catalogue with the given fields
   * @param context The fields given from form
   * @return The id of the catalogue created
   */
  public Future<String> createCatalogue(@NotNull RoutingContext context) {
    String catalogueId = context.body().asJsonObject().getString(CATALOGUE_ID);
    Promise<String> promise = Promise.promise();

    processRDF(context, catalogueId).onSuccess(catalogue ->{
      MultiMap headers = createHeader(catalogue);

      vertxClient.putAbs(this.url + CATALOGUE_REQUEST + "/" + catalogueId)
        .putHeaders(headers)
        .sendBuffer(Buffer.buffer(catalogue), ar -> {
          if (ar.succeeded()) {
            // Create action performed.
            if (HttpResponseStatus.CREATED.code() == ar.result().statusCode()) {
              log.info(HUB_REPO_CLIENT + " Successfully put catalogue with id: {}", catalogueId);

              // Create the catalogue map under dispatcher.
              dispatcherClient.createCatalogueMap(context).onComplete(actionRes -> {
                if (actionRes.succeeded()) {
                  log.info(DISPATCHER_COLLECTION
                    +" Created catalogue map with id: {}", catalogueId);
                  promise.complete(actionRes.result());
                } else {
                  promise.fail(actionRes.cause().getMessage());
                }});
            } else if (HttpResponseStatus.NO_CONTENT.code() == ar.result().statusCode()) {
              // Update action performed.
              log.info(HUB_REPO_CLIENT + " Successfully updated catalogue with id: {}", catalogueId);

              dispatcherClient.getCatalogMappingByCatalogId(catalogueId)
                .onComplete(getRes -> {
                  if (getRes.succeeded()) {
                    log.info("Successfully retrieved catalog mapping");
                    context.put("catalogMappingMongoId", getRes.result().getString("_id"));
                    // Update the catalogue map under dispatcher.
                    dispatcherClient.updateCatalogueMap(context).onComplete(actionRes -> {
                      if (actionRes.succeeded()) {
                        log.info(DISPATCHER_COLLECTION
                          +" Update catalogue map with id: {}", catalogueId);
                        promise.complete(actionRes.result());
                      } else {
                        promise.fail(actionRes.cause().getMessage());
                      }});
                  } else {
                    log.error("Could not retrieve catalog mapping");
                    promise.fail(getRes.cause().getMessage());
                  }
                });
            }
            else {
              log.error(HUB_REPO_CLIENT +
                  " Hub repo failed to put catalogue with id: {}, error code: {}, message: {}",
                catalogueId, extractedErrorCode(ar), extractedErrorMessage(ar));
              promise.fail(new JsonObject()
                .put(ERROR_CODE, extractedErrorCode(ar))
                .put(ERROR_DETAILS, extractedErrorMessage(ar))
                .encode());
            }
          } else {
            log.error(HUB_REPO_CLIENT +
                " Failed to put catalogue: {} to hub repo, error message: {}",
              catalogueId, extractedErrorMessage(ar));
            promise.fail(new JsonObject()
              .put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar))
              .encode());
          }
        });
    }).onFailure(cause -> {
        log.error(HUB_REPO_CLIENT + " The RDF file not provided or not valid.", cause);
        promise.fail(cause.getMessage());
      }
    );
    return promise.future();
  }

  public Future<JsonObject> getAllGeographicalCoverage(@NotNull RoutingContext context){
    MultiMap headers = createHeaderWithoutType();
    Promise<JsonObject> promise = Promise.promise();

    vertxClient.getAbs(this.url + CATALOGUE_REQUEST)
      .putHeaders(headers)
      .addQueryParam("valueType", "metadata")
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          log.info(HUB_REPO_CLIENT + " Successfully got all catalogues");

          JsonObject catalogueList = CatalogueMapper.mapGeographicalCoverageList(ar.result().body().toJsonObject(),countriesManager);
          promise.complete(catalogueList);
        } else {
          log.error(HUB_REPO_CLIENT +
            " Failed to get catalogues from hub repo for the geographical coverage," +
            " error message: {}", extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });

    return promise.future();
  }

  /**
   * Get all catalogue
   * @param context The filter criteria
   * @return The list of catalogues filtered by the criteria
   */
  public Future<JsonObject> getAllCatalogue(@NotNull RoutingContext context) {
    long startTime;
    MultiMap headers = createHeaderWithoutType();
    Promise<JsonObject> promise = Promise.promise();
    // TODO to be removed it is for performance investigation.
    startTime = System.currentTimeMillis();
    log.warn((HUB_REPO_CLIENT+ "------------start of get all catalogues"));
    vertxClient.getAbs(this.url + CATALOGUE_REQUEST)
      .putHeaders(headers)
      .addQueryParam("valueType", "metadata")
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          log.info(HUB_REPO_CLIENT + " Successfully got all catalogues");
          // TODO to be removed it is for performance investigation.
          long test = System.currentTimeMillis();
          log.info((HUB_REPO_CLIENT+ "------------get all catalogues from hub repo: " + (test- startTime)));

          String pageString = context.request().getParam("page");
          String limitString = context.request().getParam("limit");

          String sortByString = context.request().getParam("sortBy");
          String sortOrderString = context.request().getParam("sortOrder");

          dispatcherClient.getAllCatalogueMap().onComplete(actionRes -> {
            if (actionRes.succeeded()) {
              // TODO to be removed it is for performance investigation.
              long test2 = System.currentTimeMillis();
              log.info((HUB_REPO_CLIENT+ "------------get all catalogues map from dispatcher: " + (test2- startTime)));

              CatalogueMapper.map(ar.result().body().toJsonObject(), new JsonObject(actionRes.result()), context, countriesManager, hubSearchClient)
                .onSuccess(catalogueList -> {

                  if(catalogueList == null){
                    promise.complete(new JsonObject());
                  }

                  JsonObject response =
                    prepareGetEntityResponse(catalogueList.getJsonArray("catalogueList").getList(),
                      Integer.parseInt(pageString) , catalogueList.getInteger("sizeOfCatalogueList") ,
                      Integer.parseInt(pageString) , Integer.parseInt(limitString) , sortByString,
                      sortOrderString, null);
                  // TODO to be removed it is for performance investigation.
                  long test3 = System.currentTimeMillis();
                  log.info((HUB_REPO_CLIENT+ "------------finished all catalogues fetch: " + (test3-startTime)));
                  promise.complete(response);

                })
                .onFailure(err -> {
                  context.response().setStatusCode(500).end("Failed to map catalogues.");
                });

            } else {
              promise.fail(actionRes.cause().getMessage());
            }});
        } else {
          log.error(HUB_REPO_CLIENT +
            " Failed to get catalogues from hub repo, error message: {}", extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });

    return promise.future();
  }


  /**
   * Get catalogue or a list of catalogue.
   * @param context Provided id.
   * @return The list of catalogues if id provided or the catalogue with id provided.
   */
  public Future<JsonObject> getCatalogue(@NotNull RoutingContext context) {
    MultiMap headers = createHeaderWithoutType();
    Promise<JsonObject> promise = Promise.promise();
    String catalogueId =  (context.pathParam(CATALOGUE_ID) == null) ?
      context.body().asJsonObject().getString(CATALOGUE_ID):
      context.pathParam(CATALOGUE_ID);

    vertxClient.getAbs(this.url + CATALOGUE_REQUEST + "/" + catalogueId)
      .putHeaders(headers)
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          if(ar.result().statusCode() == 404){
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          } else {
            dispatcherClient.getAllCatalogueMap().onComplete(actionRes -> {
              if (actionRes.succeeded()) {
                JsonObject catalogue = CatalogueMapper.mapCatalogue(ar.result().body().toJsonObject()
                  ,new JsonObject(actionRes.result()), countriesManager, catalogueId);

                if(catalogue == null){
                  promise.complete(new JsonObject());
                  return;
                }
                JsonObject response = new JsonObject();
                response.put("data", catalogue);
                promise.complete(response);
                log.info(HUB_REPO_CLIENT + " Successfully got catalogue with id: {}", catalogueId);
              } else {
                promise.fail(actionRes.cause().getMessage());
              }});
          }
        } else {
          log.error(HUB_REPO_CLIENT +
              " Failed to get catalogue: {} from hub repo, error message: {}",
            catalogueId, extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });

    return promise.future();
  }



  public Future<JsonObject> existsCatalogueByDomibusIdOrCatalogueIdOrCountry(RoutingContext context) {
    MultiMap headers = createHeaderWithoutType();
    Promise<JsonObject> promise = Promise.promise();

    // Get catalogue id for query param if exists for validate api call
    // or from path param in case of update api call.
    String catalogueId = context.queryParams().get(CATALOGUE_ID) != null
      ? context.queryParams().get(CATALOGUE_ID)
      : context.pathParam(CATALOGUE_ID);

    vertxClient.getAbs(this.url + CATALOGUE_REQUEST)
      .putHeaders(headers)
      .addQueryParam("valueType", "metadata")
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          log.info(HUB_REPO_CLIENT + " Successfully got all catalogues");

          dispatcherClient.getAllCatalogueMap().onComplete(actionRes -> {
            if (actionRes.succeeded()) {
              getUniqueFieldsFromContext(context).onComplete(uniqueFieldsResponse ->{
                if (uniqueFieldsResponse.succeeded()) {
                  promise.complete( CatalogueMapper.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(ar.result().body().toJsonObject()
                    ,new JsonObject(actionRes.result()), countriesManager, uniqueFieldsResponse.result(), catalogueId));
                } else {
                  promise.fail(uniqueFieldsResponse.cause().getMessage());
                }
              });
            } else {
              promise.fail(actionRes.cause().getMessage());
            }});
        } else {
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });

    return promise.future();
  }


  private Future<JsonObject> getUniqueFieldsFromContext(RoutingContext context) {
    String catalogueId = context.body().asJsonObject().getString(CATALOGUE_ID);

    JsonObject uniqueFields = new JsonObject();
    uniqueFields.put(CATALOGUE_ID,catalogueId);
    uniqueFields.put(DOMIBUS_ID, context.body().asJsonObject().getString(DOMIBUS_ID));

    Promise<JsonObject> promise = Promise.promise();

    // Get the value of the radio button to see which value is selected.
    JsonObject uploadRDF = context.body().asJsonObject().getJsonObject(UPLOAD_RDF);
    String selectedValue = null;

    if (uploadRDF != null) {
      selectedValue = uploadRDF.getString("key");
    }

    if(context.body().asJsonObject().getJsonObject("geographicalCoverage") != null && "b".equals(selectedValue)){
      uniqueFields.put(GEOGRAPHICAL_COVERAGE,context.body().asJsonObject().getJsonObject("geographicalCoverage").getString("key"));
    }
    promise.complete(uniqueFields);

    return promise.future();
  }

  /**
   * Delete catalogue by id
   * @param context The catalogue id to delete
   * @return The id of the catalogue deleted
   */
  public Future<String> deleteCatalogue(@NotNull RoutingContext context) {
    MultiMap headers = createHeaderWithoutType();
    Promise<String> promise = Promise.promise();
    String catalogueId = context.pathParam(CATALOGUE_ID);

    vertxClient.deleteAbs(this.url + CATALOGUE_REQUEST + "/" + catalogueId)
      .putHeaders(headers)
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.NO_CONTENT.code() == ar.result().statusCode()) {
            log.info(HUB_REPO_CLIENT + " Successfully delete catalogue with id: {}", catalogueId);
            promise.complete(catalogueId);
          } else {
            log.error(HUB_REPO_CLIENT +
                " Failed to delete catalogue: {} from hub repo, error message: {}",
              catalogueId, extractedErrorMessage(ar));
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          }
        } else {
          log.error(HUB_REPO_CLIENT +
              "  Failed to delete catalogue: {} to hub repo, error message: {}",
            catalogueId, extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });
    return promise.future();
  }

  /**
   * On errors when the hub repo it is down we do not have a result, so we need get the error from
   * the cause
   */
  private String extractedErrorMessage(AsyncResult<HttpResponse<Buffer>> ar) {
    String errorMessage;

    if (Objects.nonNull(ar.cause())) {
      errorMessage = ar.cause().getMessage();
    } else if (Objects.nonNull(ar.result().bodyAsString())) {
      errorMessage = ar.result().bodyAsString();
    } else {
      errorMessage = ar.result().statusMessage();
    }

    return Jsoup.parse(errorMessage).text();
  }

  private Integer extractedErrorCode(AsyncResult<HttpResponse<Buffer>> ar) {
    return Objects.nonNull(ar.result()) ? ar.result().statusCode() : 500;
  }

  /**
   * Process RDF text
   * @param context The retrieved context
   * @param catalogueId The catalogue id
   * @return The rdf file content
   */
  private Future<String> processRDF(RoutingContext context, String catalogueId) {
    Promise<String> promise = Promise.promise();

    // Get the value of the radio button to see which value is selected.
    JsonObject uploadRDF = context.body().asJsonObject().getJsonObject(UPLOAD_RDF);
    String selectedValue = null;

    // Check if a file uploaded.
    JsonObject fieldNameContent = context.body().asJsonObject()
      .getJsonObject("rdfTemplate");

    if (uploadRDF != null) {
      selectedValue = uploadRDF.getString("key");
    }

    if(fieldNameContent != null && "a".equals(selectedValue)){
       getCatalogueFile(catalogueId, context).onSuccess(promise::complete);
    } else if ("b".equals(selectedValue)) {
        try (InputStream rdfFile = getResourcePath(CATALOGUE_FORM_RDF.getValue())) {
          // Read RDF file as a string
          String rdfContent = new String(rdfFile.readAllBytes(), StandardCharsets.UTF_8);

          // Replace placeholders with actual values
          rdfContent = rdfContent.replace("{catalogueID}", catalogueId)
            .replace("{title}", context.body().asJsonObject().getString(CATALOGUE_NAME))
            .replace("{description}", context.body().asJsonObject().getString(DESCRIPTION))
            .replace("{lang}", "eng")
            .replace("{countryISO}", context.body().asJsonObject().getJsonObject("geographicalCoverage").getString("key"))
            .replace("{homepageURL}", context.body().asJsonObject().getString(PUBLISHER_HOMEPAGE_URL))
            .replace("{name}", context.body().asJsonObject().getString(PUBLISHER_NAME));

          // Upload the produced rdf file to hub store.
          uploadTheRDFFile(context, rdfContent, context.body().asJsonObject().getString(CATALOGUE_NAME));
          promise.complete(rdfContent);
        } catch (IOException e) {
          log.info("RDF file not found.", e);
          promise.fail("RDF file not found.");
        }
    } else {
      promise.fail("RDF file not provided or not valid.");
    }
    return promise.future();
  }

  private Future<String> getCatalogueFile(String catalogueId, RoutingContext context) {
    Promise<String> promise = Promise.promise();

    // Get the uploaded file asynchronously
    hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          promise.complete(response.bodyAsString());  // Return the file content
        } else {
          // Handle file retrieval failure
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error retrieving file: " + ar.cause().getMessage());
          promise.fail(ar.cause());
        }
      });

    return promise.future();
  }

  private InputStream getResourcePath(String relativePath) {
    return SchemaFiles.class.getClassLoader().getResourceAsStream(relativePath);
  }

  private void uploadTheRDFFile(RoutingContext context, String rdfContent, String catalogueName){
    hubStoreClient.uploadFile(CATALOGUE_ID, context.body().asJsonObject().getString(CATALOGUE_ID),
      catalogueName + ".rdf", Buffer.buffer(rdfContent), "text/plain");
  }

  public Future<JsonArray> getDatasetsByCatalogId(String catalogId) {

    MultiMap headers = createHeaderWithoutType();

    return vertxClient.getAbs(this.url + CATALOGUE_REQUEST + "/" + catalogId + "/datasets")
      .putHeaders(headers)
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Datasets were retrieved for catalog id={}", catalogId);
          return Future.succeededFuture(response.bodyAsJsonArray());

        } else {
          log.error("Failed to get datasets for catalog id: {}", catalogId);
          return Future.failedFuture(
            "Failed to get datasets for catalog id: {} with response message: {}" + catalogId
              + response.statusMessage());
        }
      });
  }
}
