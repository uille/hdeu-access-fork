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
package eu.europa.data.health.access.dispatcher_client;

import static eu.europa.data.health.access.Constants.APPLICATION;
import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.STATUS;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.dto.CatalogueStatusEnum;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DispatcherClient {

  private final String url;
  private final String xApiKey;

  WebClient vertxClient;

  public DispatcherClient(String url, String xApiKey, WebClient client) {
    this.url = url;
    this.xApiKey = xApiKey;
    this.vertxClient = client;
  }


  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String DISPATCHER_CLIENT = "DISPATCHER_CLIENT";
  public static final String DATA_PERMIT_CREATE_APPLICATION_REQUEST = "/data-permit/create/application";
  private static final String DATA_PERMIT_DELETE_APPLICATIONS_REQUEST = "/data-permit/delete";
  private static final String CATALOGUE_MAP_REQUEST ="/catalogs";
  private static final String CATALOGUE_MAP_REQUEST_BY_CATALOG_ID ="/catalog/catalogId/";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_DETAILS = "errorDetails";

  private void addXApiKey(MultiMap headers) {

    if (StringUtils.isNotEmpty(this.xApiKey)) {
      headers.add("X-API-Key", this.xApiKey);
    }
  }

  public MultiMap createHeader(Buffer buffer) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("content-type", "application/json");
    headers.add("content-length", String.valueOf(buffer.length()));
    addXApiKey(headers);
    return headers;
  }

  private MultiMap createHeaderWithoutType(){
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    addXApiKey(headers);
    return headers;
  }

  /**
   * Create a catalogue map.
   * @param context The data to create the catalogue map.
   * @return The created catalogue map.
   */
  public Future<String> createCatalogueMap(RoutingContext context){
    Promise<String> promise = Promise.promise();

    // JSON object to send in the POST request body
    JsonObject jsonObject = new JsonObject()
      .put("catalogIds", new JsonArray().add(context.body().asJsonObject().getString(CATALOGUE_ID)))
      .put("recipient", context.body().asJsonObject().getString(DOMIBUS_ID))
      .put("isProduction", CatalogueStatusEnum.LIVE.getValue().
        equals(context.body().asJsonObject().getJsonObject(STATUS).getString("key")));

    Buffer buffer = jsonObject.toBuffer();
    MultiMap headers = createHeader(buffer);

    vertxClient.postAbs(this.url + CATALOGUE_MAP_REQUEST)
      .putHeaders(headers).sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.CREATED.code() == ar.result().statusCode()) {
            log.info(DISPATCHER_CLIENT
                + " Successfully create the map for catalogue: {} to dispatcher",
              context.body().asJsonObject().getString(CATALOGUE_ID));
            promise.complete(Jsoup.parse(ar.result().bodyAsString()).text());
          } else {
            log.error(DISPATCHER_CLIENT
                + " Dispatcher failed to create catalogue: {}, with error code: {} and message: {}",
              context.body().asJsonObject().getString(CATALOGUE_ID),
              extractedErrorCode(ar), extractedErrorMessage(ar));
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          }
        } else {
          log.error(DISPATCHER_CLIENT
              + " Failed failed to create catalogue: {} to dispatcher, with error message: {}",
            context.body().asJsonObject().getString(CATALOGUE_ID),
            extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });
    return promise.future();
  }

  /**
   * Update catalogue map.
   * @param context The data to update the catalogue map.
   * @return The updated catalogue map.
   */
  public Future<String> updateCatalogueMap(@NotNull RoutingContext context) {
    Promise<String> promise = Promise.promise();
    String catalogMappingMongoId = context.get("catalogMappingMongoId");
    log.info("Will try to update catalog mapping with mongo id: {}", catalogMappingMongoId);

    // JSON object to send in the POST request body
    JsonObject jsonObject = new JsonObject()
      .put("catalogIds", new JsonArray().add(context.body().asJsonObject().getString(CATALOGUE_ID)))
      .put("recipient", context.body().asJsonObject().getString(DOMIBUS_ID))
      .put("isProduction", CatalogueStatusEnum.LIVE.getValue().
        equals(context.body().asJsonObject().getJsonObject(STATUS).getString("key")));

    Buffer buffer = jsonObject.toBuffer();
    MultiMap headers = createHeader(buffer);
    vertxClient.putAbs(this.url + CATALOGUE_MAP_REQUEST + "/" + catalogMappingMongoId)
      .putHeaders(headers).sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.OK.code() == ar.result().statusCode()) {
            log.info(DISPATCHER_CLIENT
                + " Successfully updated the map for catalogue: {} to dispatcher",
              context.body().asJsonObject().getString(CATALOGUE_ID));
            promise.complete(Jsoup.parse(ar.result().bodyAsString()).text());
          } else {
            log.error(DISPATCHER_CLIENT
                + " Dispatcher failed to update catalogue: {}, with error code: {} and message: {}",
              context.body().asJsonObject().getString(CATALOGUE_ID),
              extractedErrorCode(ar), extractedErrorMessage(ar));
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          }
        } else {
          log.error(DISPATCHER_CLIENT
              + " Failed failed to update catalogue: {} to dispatcher, with error message: {}",
            context.body().asJsonObject().getString(CATALOGUE_ID),
            extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });
    return promise.future();
  }

  /**
   * Get all the catalogue map.
   * @return The list of catalogue map.
   */
  public Future<String> getAllCatalogueMap(){
    Promise<String> promise = Promise.promise();
    MultiMap headers = createHeaderWithoutType();

    vertxClient.getAbs(this.url + CATALOGUE_MAP_REQUEST)
      .putHeaders(headers)
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          if(ar.result().statusCode() == 404){
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          } else {
            log.info(DISPATCHER_CLIENT + " Successfully got catalogue map list");
            promise.complete(Jsoup.parse(ar.result().body().toString()).text());
          }
        } else {
          log.error(DISPATCHER_CLIENT + " Failed to get catalogue map list with error {}", extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });

    return promise.future();
  }

  public Future<String> sendApplication(JsonObject dispatcherRecord) {

    Promise<String> promise = Promise.promise();

    // JSON object to send in the POST request body
    JsonObject jsonObject = new JsonObject()
      .put("application_id", dispatcherRecord.getJsonObject(APPLICATION).getString("_id"))
      .put(STATUS, dispatcherRecord.getJsonObject(APPLICATION).getJsonObject(STATUS).getValue(STATUS))
      .put("datasets", dispatcherRecord.getJsonObject(APPLICATION).getJsonArray("datasets"))
      .put("form", dispatcherRecord.getJsonObject(APPLICATION).getJsonObject("form"))
      .put("title", dispatcherRecord.getJsonObject(APPLICATION).getString("title"))
      .put(Constants.APPLICATION_TYPE, dispatcherRecord.getJsonObject("configuration").getString(Constants.APPLICATION_TYPE));

    Buffer buffer = jsonObject.toBuffer();
    MultiMap headers = createHeader(buffer);

    vertxClient.postAbs(this.url + DATA_PERMIT_CREATE_APPLICATION_REQUEST)
      .putHeaders(headers).sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.CREATED.code() == ar.result().statusCode()) {
            log.info(DISPATCHER_CLIENT
                + " Successfully sent application: {} to dispatcher",
              dispatcherRecord.getJsonObject(APPLICATION).getString("_id"));
            promise.complete(Jsoup.parse(ar.result().bodyAsString()).text());
          } else {
            log.error(DISPATCHER_CLIENT
                + " Dispatcher failed to process application: {}, with error code: {} and message: {}",
              dispatcherRecord.getJsonObject(APPLICATION).getString("_id"),
              extractedErrorCode(ar), extractedErrorMessage(ar));
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          }
        } else {
          log.error(DISPATCHER_CLIENT
              + " Failed to sent application: {} to dispatcher, with error message: {}",
            dispatcherRecord.getJsonObject(APPLICATION).getString("_id"),
            extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });
    return promise.future();
  }

  /**
   * On errors when the dispatcher it is down we do not have a result, so we need get the error from
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
   * Delete all applications on dispatcher for a specific applicationIds

   */
  public Future<String> deleteApplicationOnDispatcher(JsonObject applications) {

    Promise<String> promise = Promise.promise();
    // JSON object to send in the DELETE request body
    Buffer buffer = applications.toBuffer();
    MultiMap headers = createHeader(buffer);
    vertxClient.deleteAbs(this.url + DATA_PERMIT_DELETE_APPLICATIONS_REQUEST).putHeaders(headers)
      .sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.NO_CONTENT.code() == ar.result().statusCode()) {
            log.info(DISPATCHER_CLIENT
                + " successfully delete applications: {} from dispatcher",
              applications);
            promise.complete();
          } else {
            log.error(DISPATCHER_CLIENT
                + " failed to delete applications: {}, with error code: {} and message: {}",
              applications, extractedErrorCode(ar), extractedErrorMessage(ar));
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
          }
        } else {
          log.error(DISPATCHER_CLIENT
              + " failed to delete application: {} to dispatcher, with error message: {}",
            applications, ar.cause().getMessage());
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, extractedErrorMessage(ar)).encode());
        }
      });
    return promise.future();
  }

  public Future<JsonObject> getCatalogMappingByCatalogId(String catalogId) {

    MultiMap headers = createHeaderWithoutType();

    return vertxClient.getAbs(this.url + CATALOGUE_MAP_REQUEST_BY_CATALOG_ID + catalogId)
      .putHeaders(headers)
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Catalog mapping was found for: catalog id={}", catalogId);
          return Future.succeededFuture(response.bodyAsJsonObject());

        } else {
          log.error("Failed to get catalog mapping for catalog id: {}", catalogId);
          return Future.failedFuture(
            "Failed to get catalog mapping for catalog id: " + catalogId + " with response message: " + response.statusMessage());
        }
      });
  }


  public Future<Integer> deleteCatalogMappingByMongoId(@NotNull RoutingContext context) {

    MultiMap headers = createHeaderWithoutType();
    String catalogMappingMongoId = context.get("catalogMappingMongoId");
    log.info("Will try to delete catalog mapping with mongo id: {}", catalogMappingMongoId);

    return vertxClient.deleteAbs(this.url + CATALOGUE_MAP_REQUEST + "/" + catalogMappingMongoId)
      .putHeaders(headers)
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Catalog mapping was found and deleted for: catalog mapping with mongo id: {}", catalogMappingMongoId);
          return Future.succeededFuture(response.statusCode());
        } else if (response.statusCode() == 404) {
          log.info("Catalog mapping was not found for: catalog mapping with mongo id: {}", catalogMappingMongoId);
          return Future.succeededFuture(response.statusCode());
        } else {
          log.error("Failed to delete catalog mapping for catalog id: {}", catalogMappingMongoId);
          return Future.failedFuture(
            "Failed to delete catalog mapping for catalog id: " + catalogMappingMongoId + " with response message: " + response.statusMessage());
        }
      }).onFailure(failure ->
        log.error("Failed to delete catalog mapping for catalog id: {} with error: {}",
          catalogMappingMongoId, failure.getMessage()));
  }

}

