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

import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.CONTENT_DISPOSITION;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.FILENAME;
import static eu.europa.data.health.access.Constants.FILENAME_ASTERISK_EQUALS;

import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_repo_client.HubRepoClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Class that implements all the CRUD operations for Catalogue.
 */
@Slf4j
public class CatalogueManager extends AbstractManager {

  private final HubRepoClient hubRepoClient;

  private final HubStoreClient hubStoreClient;

  private final DispatcherClient dispatcherClient;

  private final AuditLogManager auditLogManager;

  private final Vertx vertx;

  public CatalogueManager(DBHandler dbHandler, HubRepoClient hubRepoClient,
    HubStoreClient hubStoreClient, DispatcherClient dispatcherClient, Vertx vertx,  AuditLogManager auditLogManager) {
    super(dbHandler);
    this.hubRepoClient = hubRepoClient;
    this.hubStoreClient = hubStoreClient;
    this.dispatcherClient = dispatcherClient;
    this.vertx = vertx;
    this.auditLogManager = auditLogManager;
  }


  public void validateUpdateCatalogue(@NotNull RoutingContext context) {
    // Stop if there are validation errors.
    if (context.get(ERRORS) != null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Create catalogue not stored due to validation errors");
      return;
    }

    setUpMessageContextResponse(context, HttpResponseStatus.OK,
      "Create catalogue can be continue");
  }

  /**
   * Operation to create or update the content of a catalogue
   *
   * @param context Contains the request body for the catalogue
   */
  public void createCatalogue(@NotNull RoutingContext context) {
    // Stop if there are validation errors.
    if (context.get(ERRORS) != null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Create catalogue not stored due to validation errors");
      return;
    }

    hubRepoClient.createCatalogue(context).onSuccess(result -> setUpMessageContextResponse(context, HttpResponseStatus.OK, result)).onFailure(cause -> {
      if (context.get(ERRORS) != null) {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "Catalogue was not created due to validation issue.");
      } else {
        String message = cause.getMessage();

        try {
          JsonObject json = new JsonObject(message);
          int errorCode = json.getInteger("errorCode");
          setUpMessageContextResponse(context, HttpResponseStatus.valueOf(errorCode),
            json.getString("errorDetails"));
        } catch (Exception e) {
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Catalogue was not created due to unknown error.");
        }
      }
    });
  }

  public void updateCatalogue(@NotNull RoutingContext context) {
    createCatalogue(context);
  }

  public void getAllCatalogues(@NotNull RoutingContext context) {
    hubRepoClient.getAllCatalogue(context).onSuccess(result -> setUpContextResponse(context, HttpResponseStatus.OK, result.encode())).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "No catalogues found."));
  }

  public void getAllGeographicalCoverage(@NotNull RoutingContext context) {
    hubRepoClient.getAllGeographicalCoverage(context).onSuccess(result -> setUpContextResponse(context, HttpResponseStatus.OK, result.encode())).onFailure(
      cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
        "No countries found."));
  }

  /**
   * Get catalogue or all catalogue list if id is not provided.
   *
   * @param context The id of catalogue if provided.
   */
  public void getCatalogue(@NotNull RoutingContext context) {
    hubRepoClient.getCatalogue(context).compose(result ->
        getRDFFileDataForCatalogue(context, result)
      ).onSuccess(
        updatedResult -> setUpContextResponse(context, HttpResponseStatus.OK, updatedResult.encode()
        )).
      onFailure(
        cause -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Catalogue not found."));
  }

  /**
   * Delete a catalogue by id.
   *
   * @param context The id of catalogue.
   */
  public void deleteCatalog(@NotNull RoutingContext context) {

    getCatalogueTitleValue(context)
      .recover(err -> Future.succeededFuture("")) // ignore errors, return empty string
      .compose(title -> {

        Future<Integer> dispatcherCall = dispatcherClient.deleteCatalogMappingByMongoId(context);
        Future<String> hubRepoCall = hubRepoClient.deleteCatalogue(context);
        Future<HttpResponse<Buffer>> hubStoreCall = hubStoreClient.deleteFile(null, CATALOGUE_ID, context.pathParam(CATALOGUE_ID));

        return CompositeFuture.all(dispatcherCall, hubRepoCall, hubStoreCall)
          .map(cf -> new DeleteResults(
            title,
            dispatcherCall.result(),
            hubRepoCall.result(),
            hubStoreCall.result()
          ));
      })
      .onSuccess(results -> {
        String title = results.title;
        Integer dispatcherCallResult = results.dispatcherResult;
        String hubRepoCallResult = results.hubRepoResult;
        HttpResponse<Buffer> hubStoreCallResult = results.hubStoreResult;

        int hubStoreStatus = (hubStoreCallResult != null) ? hubStoreCallResult.statusCode() : -1;
        String hubStoreMessage =
          (hubStoreCallResult != null) ? hubStoreCallResult.bodyAsString() : "Unknown";

        //Audit Log
        String auditLogActionDescription = auditLogManager.prepareAuditLogActionDescription(
          AuditLogActionDescription.CATALOGUE_DELETED.toString(), context.pathParam(CATALOGUE_ID), title);
        auditLogManager.insertAuditLog(AuditLogObject.CATALOGUE.toString(), context.pathParam(CATALOGUE_ID),
          AuditLogAction.CATALOGUE_DELETED.toString(), auditLogActionDescription, true, context);

        JsonObject finalResult = new JsonObject()
          .put("dispatcherCallResult", dispatcherCallResult)
          .put("hubRepoCallResult", hubRepoCallResult)
          .put("hubStoreCallResultStatus", hubStoreStatus)
          .put("hubStoreCallResultMessage", hubStoreMessage);

        context.response()
          .putHeader("Content-Type", "application/json")
          .setStatusCode(200)
          .end(finalResult.encodePrettily());
      })
      .onFailure(err -> context.response()
        .setStatusCode(500)
        .end("An error occurred: " + err.getMessage())
      );
  }

  private Future<String> getCatalogueTitleValue(RoutingContext context) {
    return hubRepoClient.getCatalogueTitle(context)
      .map(json -> {
        JsonArray graph = json.getJsonArray("@graph");
        if (graph == null || graph.isEmpty()) return "";

        JsonObject catalogue = graph.getJsonObject(0);
        JsonObject titleObj = catalogue.getJsonObject("dct:title");
        if (titleObj != null && "en".equals(titleObj.getString("@language"))) {
          return titleObj.getString("@value", "");
        }
        return "";
      })
      .recover(err -> Future.succeededFuture("")); // Treat errors as empty title
  }

  /**
   * The rdf file is uploaded in the hup-store.
   *
   * @param context The given rdf file.
   */
  public void uploadRDFFile(@NotNull RoutingContext context) {
    // Stop if there are validation errors.
    if (context.get(ERRORS) != null) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "File upload not completed because of validation issues");
      return;
    }
    handleFileUpload(context, CATALOGUE_ID, context.pathParam(CATALOGUE_ID), hubStoreClient, vertx);
  }

  public Future<JsonObject> getRDFFileDataForCatalogue(@NotNull RoutingContext context,
    JsonObject result) {
    Promise<JsonObject> promise = Promise.promise();
    String catalogueId = context.pathParam(CATALOGUE_ID);

    hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId).onComplete(ar -> {
      // Set default "uploadRDF" regardless of outcome
      SelectObjectDTO uploadRDF = new SelectObjectDTO();
      uploadRDF.setKey("b");
      uploadRDF.setValue("Define Catalogue By filling details?");
      result.getJsonObject("data").put("uploadRDF", JsonObject.mapFrom(uploadRDF));

      if (ar.succeeded()) {
        var response = ar.result();

        // Create and populate the AttachmentDTO
        AttachmentDTO rdfTemplate = new AttachmentDTO();
        //TODO wrong id this is the file id no the catalogue id. To be corrected
        rdfTemplate.setId(response.headers().get(CATALOGUE_ID));
        rdfTemplate.setName(decodeFilename(response.getHeader(CONTENT_DISPOSITION)));
        rdfTemplate.setSize((double) response.body().length());

        result.getJsonObject("data").put("rdfTemplate", JsonObject.mapFrom(rdfTemplate));
      }

      // Complete the promise with the updated result
      promise.complete(result);
    });


    return promise.future();
  }

  /**
   * Get the metadata of rdf file
   *
   * @param context The given catalogue id to find the file metadata
   */
  public void getRDFFileMetadata(@NotNull RoutingContext context) {
    String catalogueId = context.pathParam(CATALOGUE_ID);

    // Get the uploaded file
    hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();

          context.response()
            .putHeader(CONTENT_TYPE, "application/json")
            .end(Json.encodePrettily(Map.of(
              CONTENT_DISPOSITION, response.headers().get(CONTENT_DISPOSITION),
              FILENAME, decodeFilename(response.getHeader(CONTENT_DISPOSITION))
            )));
        } else {
          // Handle file retrieval failure
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error retrieving file: " + ar.cause().getMessage());
        }
      });
  }

  /**
   * Get the rdf file
   *
   * @param context The given catalogue id to find the file.
   */
  public void getRDFFileData(@NotNull RoutingContext context) {
    String catalogueId = context.pathParam(CATALOGUE_ID);

    // Get the uploaded file
    hubStoreClient.getFile(null, CATALOGUE_ID, catalogueId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          context.response()
            .putHeader(CONTENT_TYPE, "application/octet-stream")
            .putHeader(CONTENT_DISPOSITION, response.headers().get(CONTENT_DISPOSITION))
            .putHeader(FILENAME, decodeFilename(response.getHeader(CONTENT_DISPOSITION)))
            .end(response.bodyAsBuffer());
        } else {
          // Handle file retrieval failure
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error retrieving file: " + ar.cause().getMessage());
        }
      });
  }

  protected String decodeFilename(String contentDisposition){
    if (StringUtils.isNotEmpty(contentDisposition) && contentDisposition.contains(FILENAME_ASTERISK_EQUALS)) {
      String part = contentDisposition.substring(contentDisposition.indexOf(FILENAME_ASTERISK_EQUALS) + FILENAME_ASTERISK_EQUALS.length()).trim();
      if (part.startsWith("UTF-8''")) {
        part = part.substring("UTF-8''".length());
      }
      return URLDecoder.decode(part, StandardCharsets.UTF_8);
    }
    return "Error_decoding_filename";
  }

  public record DeleteResults(String title, Integer dispatcherResult, String hubRepoResult,
                              HttpResponse<Buffer> hubStoreResult) {}
}
