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
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.OPERATION;
import static eu.europa.data.health.access.Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
import static eu.europa.data.health.access.Constants.STATUS;

import eu.europa.data.health.access.CommonClientUtils;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.OperationStatus;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherClient {

  public static final String INPUT_LANGUAGE = "inputLanguage";
  public static final String TITLE = "title";
  public static final String DISPATCHER_CLIENT = "DISPATCHER_CLIENT";
  public static final String DATA_PERMIT_CREATE_APPLICATION_REQUEST = "/data-permit/create/application";
  public static final String OUTGOING_APPLICATION_ACTIVITY_REQUEST = "/application/activity";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_DETAILS = "errorDetails";
  private static final String DATA_PERMIT_DELETE_APPLICATIONS_REQUEST = "/data-permit/delete";
  private static final String CATALOGUE_MAP_REQUEST = "/catalogs";
  private static final String CATALOGUE_MAP_REQUEST_BY_CATALOG_ID = "/catalog/catalogId/";
  private final String url;
  private final String xApiKey;
  private final Logger log = LoggerFactory.getLogger(getClass());
  WebClient vertxClient;

  public DispatcherClient(String url, String xApiKey, WebClient client) {
    this.url = url;
    this.xApiKey = xApiKey;
    this.vertxClient = client;
  }

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

  private MultiMap createHeaderWithoutType() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    addXApiKey(headers);
    return headers;
  }

  /**
   * Create a catalogue map.
   *
   * @param context The data to create the catalogue map.
   * @return The created catalogue map.
   */
  public Future<String> createCatalogueMap(RoutingContext context) {
    Promise<String> promise = Promise.promise();

    Buffer buffer = createCatalogueJsonObject(context).toBuffer();
    MultiMap headers = createHeader(buffer);

    vertxClient.postAbs(this.url + CATALOGUE_MAP_REQUEST).putHeaders(headers)
      .sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.CREATED.code() == ar.result().statusCode()) {
            logAndPromiseSuccess(promise, context, ar,
              DISPATCHER_CLIENT + " Successfully created the map for catalogue: {} to dispatcher");
          } else {
            logAndPromiseFail(promise, context, ar, DISPATCHER_CLIENT
              + " Dispatcher failed to create catalogue: {}, with error code: {} and message: {}");
          }
        } else {
          logAndPromiseFail(promise, context, ar, DISPATCHER_CLIENT
            + " Failed to create catalogue: {} to dispatcher, with error message: {}");
        }
      });
    return promise.future();
  }

  /**
   * Update catalogue map.
   *
   * @param context The data to update the catalogue map.
   * @return The updated catalogue map.
   */
  public Future<String> updateCatalogueMap(@NotNull RoutingContext context) {
    Promise<String> promise = Promise.promise();
    String catalogMappingMongoId = context.get("catalogMappingMongoId");
    log.info("Will try to update catalog mapping with mongo id: {}", catalogMappingMongoId);

    Buffer buffer = createCatalogueJsonObject(context).toBuffer();
    MultiMap headers = createHeader(buffer);
    vertxClient.putAbs(this.url + CATALOGUE_MAP_REQUEST + "/" + catalogMappingMongoId)
      .putHeaders(headers).sendBuffer(buffer, ar -> {
        if (ar.succeeded()) {
          if (HttpResponseStatus.OK.code() == ar.result().statusCode()) {
            logAndPromiseSuccess(promise, context, ar,
              DISPATCHER_CLIENT + " Successfully updated the map for catalogue: {} to dispatcher");
          } else {
            logAndPromiseFail(promise, context, ar, DISPATCHER_CLIENT
              + " Dispatcher failed to update catalogue: {}, with error code: {} and message: {}");
          }
        } else {
          logAndPromiseFail(promise, context, ar, DISPATCHER_CLIENT
            + " Failed failed to update catalogue: {} to dispatcher, with error message: {}");
        }
      });
    return promise.future();
  }

  private void logAndPromiseFail(Promise<String> promise, @NotNull RoutingContext context,
    AsyncResult<HttpResponse<Buffer>> result, String message) {

    if (log.isErrorEnabled()) {
      log.error(message, context.body().asJsonObject().getString(CATALOGUE_ID),
        CommonClientUtils.extractedErrorMessage(result));
    }

    promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(result))
      .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(result)).encode());
  }

  private void logAndPromiseSuccess(Promise<String> promise, @NotNull RoutingContext context,
    AsyncResult<HttpResponse<Buffer>> result, String message) {

    if (log.isInfoEnabled()) {
      log.info(message, context.body().asJsonObject().getString(CATALOGUE_ID));
    }

    promise.complete(Jsoup.parse(result.result().bodyAsString()).text());
  }

  private JsonObject createCatalogueJsonObject(@NotNull RoutingContext context) {
    return new JsonObject().put("catalogIds",
        new JsonArray().add(context.body().asJsonObject().getString(CATALOGUE_ID)))
      .put("recipient", context.body().asJsonObject().getString(DOMIBUS_ID)).put("isProduction",
        CatalogueStatusEnum.LIVE.getValue()
          .equals(context.body().asJsonObject().getJsonObject(STATUS).getString("key")));
  }

  /**
   * Get all the catalogue map.
   *
   * @return The list of catalogue map.
   */
  public Future<String> getAllCatalogueMap() {
    Promise<String> promise = Promise.promise();
    MultiMap headers = createHeaderWithoutType();

    vertxClient.getAbs(this.url + CATALOGUE_MAP_REQUEST).putHeaders(headers)
      .sendBuffer(null, ar -> {
        if (ar.succeeded()) {
          if (ar.result().statusCode() == 404) {
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(ar)).encode());
          } else {
            log.info(DISPATCHER_CLIENT + " Successfully got catalogue map list");
            promise.complete(Jsoup.parse(ar.result().body().toString()).text());
          }
        } else {
          log.error(DISPATCHER_CLIENT + " Failed to get catalogue map list with error {}",
            CommonClientUtils.extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(ar)).encode());
        }
      });

    return promise.future();
  }

  public Future<String> sendApplication(JsonObject dispatcherRecord) {

    Promise<String> promise = Promise.promise();

    // JSON object to send in the POST request body
    JsonObject outgoingJsonObject = new JsonObject();
    String path;
    String operation = dispatcherRecord.getString(OPERATION);
    String recordId;
    String collectionName;

    if (OperationStatus.DECISION.getLabel().equals(operation) ||
      OperationStatus.STATUS_UPDATE.getLabel().equals(dispatcherRecord.getString(OPERATION)) ||
      OperationStatus.PERMIT.getLabel().equals(dispatcherRecord.getString(OPERATION))
      || OperationStatus.ACTIVITY_RESPONSE.getLabel().equals(dispatcherRecord.getString(OPERATION))
      || OperationStatus.ACTIVITY_TRANSLATION.getLabel().equals(dispatcherRecord.getString(OPERATION))
      ) {

      recordId = dispatcherRecord.getString("_id");

      outgoingJsonObject = dispatcherRecord.copy();
      outgoingJsonObject.remove("configuration");
      path = OUTGOING_APPLICATION_ACTIVITY_REQUEST;
      collectionName = OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
    } else {
      // Create Application
      JsonObject application = dispatcherRecord.getJsonObject(APPLICATION);
      recordId = application.getString("_id");
      outgoingJsonObject.put("application_id", application.getString("_id"))
        .put(STATUS, application.getJsonObject(STATUS).getValue(STATUS)).put("datasets",
          filterDatasetLanguages(application.getJsonArray("datasets"),
            application.getString(INPUT_LANGUAGE), dispatcherRecord.getJsonArray("targetLanguages")))
        .put("version", application.getInteger("version"))
        .put("form", application.getJsonObject("form")).put(TITLE, application.getString(TITLE))
        .put(INPUT_LANGUAGE, application.getString(INPUT_LANGUAGE))
        .put("dateSubmitted", application.getString("date_submitted"))
        .put(Constants.APPLICATION_TYPE,
          dispatcherRecord.getJsonObject("configuration").getString(Constants.APPLICATION_TYPE));

      JsonObject formTranslations = application.getJsonObject("form_translations");
      if (formTranslations != null) {
        outgoingJsonObject.put("form_translations", formTranslations);
      }
      path = DATA_PERMIT_CREATE_APPLICATION_REQUEST;
      collectionName = DISPATCHER_COLLECTION;
    }

    Buffer buffer = outgoingJsonObject.toBuffer();
    MultiMap headers = createHeader(buffer);

    vertxClient.postAbs(this.url + path).putHeaders(headers).sendBuffer(buffer, ar -> {
      if (ar.succeeded()) {
        if (HttpResponseStatus.CREATED.code() == ar.result().statusCode()) {
          log.info(DISPATCHER_CLIENT + " Successfully sent {} with {} to dispatcher",
            collectionName, recordId);
          promise.complete(Jsoup.parse(ar.result().bodyAsString()).text());
        } else {
          log.error(DISPATCHER_CLIENT
              + " Dispatcher failed to process record with id {}, with error code: {} and "
              + "message: {}", recordId, extractedErrorCode(ar),
            CommonClientUtils.extractedErrorMessage(ar));
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(ar)).encode());
        }
      } else {
        log.error(DISPATCHER_CLIENT
            + " Failed to sent application: {} to dispatcher, with error message: {}", recordId,
          CommonClientUtils.extractedErrorMessage(ar));
        promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
          .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(ar)).encode());
      }
    });
    return promise.future();
  }


  private JsonArray filterDatasetLanguages(JsonArray datasets, String inputLanguage,
    JsonArray targetLanguages) {

    if (StringUtils.isNotEmpty(inputLanguage)) {
      targetLanguages.add(inputLanguage);
    }

    datasets.forEach(dataset -> {
      filterLanguages(((JsonObject) dataset).getJsonObject(TITLE), targetLanguages);
      filterLanguages(((JsonObject) dataset).getJsonObject("provenance"), targetLanguages);
      ((JsonObject) dataset).getJsonArray("distributions").forEach(
        distribution -> filterLanguages(((JsonObject) distribution).getJsonObject(TITLE),
          targetLanguages));
    });
    return datasets;
  }

  private void filterLanguages(JsonObject source, JsonArray targetLanguages) {

    if (source != null && !source.isEmpty()) {
      // Normalize target language codes (to lowercase for case-insensitive match)
      Set<String> allowed = targetLanguages.stream().map(Object::toString)
        .map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

      // Iterate safely and remove disallowed keys
      // removes the key from the source
      source.fieldNames().removeIf(key -> !allowed.contains(key.toLowerCase(Locale.ROOT)));
    }
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
            log.info(DISPATCHER_CLIENT + " successfully delete applications: {} from dispatcher",
              applications);
            promise.complete();
          } else {
            log.error(DISPATCHER_CLIENT
                + " failed to delete applications: {}, with error code: {} and message: {}",
              applications, extractedErrorCode(ar), CommonClientUtils.extractedErrorMessage(ar));
            promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
              .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(ar)).encode());
          }
        } else {
          log.error(DISPATCHER_CLIENT
              + " failed to delete application: {} to dispatcher, with error message: {}",
            applications, ar.cause().getMessage());
          promise.fail(new JsonObject().put(ERROR_CODE, extractedErrorCode(ar))
            .put(ERROR_DETAILS, CommonClientUtils.extractedErrorMessage(ar)).encode());
        }
      });
    return promise.future();
  }

  public Future<JsonObject> getCatalogMappingByCatalogId(String catalogId) {

    MultiMap headers = createHeaderWithoutType();

    return vertxClient.getAbs(this.url + CATALOGUE_MAP_REQUEST_BY_CATALOG_ID + catalogId)
      .putHeaders(headers).send().compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Catalog mapping was found for: catalog id={}", catalogId);
          return Future.succeededFuture(response.bodyAsJsonObject());

        } else {
          log.error("Failed to get catalog mapping for catalog id: {}", catalogId);
          return Future.failedFuture("Failed to get catalog mapping for catalog id: " + catalogId
            + " with response message: " + response.statusMessage());
        }
      });
  }


  public Future<Integer> deleteCatalogMappingByMongoId(@NotNull RoutingContext context) {

    MultiMap headers = createHeaderWithoutType();
    String catalogMappingMongoId = context.get("catalogMappingMongoId");
    log.info("Will try to delete catalog mapping with mongo id: {}", catalogMappingMongoId);

    return vertxClient.deleteAbs(this.url + CATALOGUE_MAP_REQUEST + "/" + catalogMappingMongoId)
      .putHeaders(headers).send().compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Catalog mapping was found and deleted for: catalog mapping with mongo id: {}",
            catalogMappingMongoId);
          return Future.succeededFuture(response.statusCode());
        } else if (response.statusCode() == 404) {
          log.info("Catalog mapping was not found for: catalog mapping with mongo id: {}",
            catalogMappingMongoId);
          return Future.succeededFuture(response.statusCode());
        } else {
          log.error("Failed to delete catalog mapping for catalog id: {}", catalogMappingMongoId);
          return Future.failedFuture(
            "Failed to delete catalog mapping for catalog id: " + catalogMappingMongoId
              + " with response message: " + response.statusMessage());
        }
      }).onFailure(
        failure -> log.error("Failed to delete catalog mapping for catalog id: {} with error: {}",
          catalogMappingMongoId, failure.getMessage()));
  }

}
