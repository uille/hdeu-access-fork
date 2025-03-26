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
package eu.europa.data.health.access.validations.validators;

import static eu.europa.data.health.access.Constants.BASKET_COLLECTION;
import static eu.europa.data.health.access.Constants.CATALOG_ID;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.FILE_UPLOAD_VALID_TYPES;
import static eu.europa.data.health.access.Constants.RESULT_MESSAGE;
import static eu.europa.data.health.access.Constants.THE_PARSING_OF_JSON_FAILED_WITH_ERROR;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.dto.ApplicationDTO;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AbstractManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommonApplicationValidationManager extends AbstractManager {

  private final DBHandler dbHandler;
  private final MongoClient mongoClient;

  public CommonApplicationValidationManager(DBHandler dbHandler, MongoClient mongoClient) {
    super(dbHandler);
    this.dbHandler = dbHandler;
    this.mongoClient = mongoClient;
  }

  public void validateCreate(RoutingContext context, String collectionName) {
    JsonObject requestBody = context.body().asJsonObject();

    ApplicationDTO applicationDTO = generateCommonApplicationDTO(
      requestBody, context);

    Set<ConstraintViolation<ApplicationDTO>> violations = validateCommonApplicationDTO(
      applicationDTO);

    if (!violations.isEmpty()) {
      List<FieldError> errors = new ArrayList<>();
      for (ConstraintViolation<ApplicationDTO> violation : violations) {
        String code = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();
        String field = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
        List<String> params = new ArrayList<>();
        if (maxParam != null) {
          params.add(maxParam.toString());
        }
        FieldError error = new FieldError(code, message, field, null, params, null, null);
        errors.add(error);
      }

      JsonObject response = new JsonObject()
        .put(ERRORS, errors);

      context.response()
        .setStatusCode(400)
        .end(response.encode());
    } else {

      validateTitle(context, collectionName)
        .compose(result -> retainOnlyValidDatasetsAndDistributions(context))
        .onSuccess(v -> context.next())
        .onFailure(err -> context.fail(500, err.getCause()));
    }
  }

  private Future<Void> validateTitle(RoutingContext context, String collectionName) {

    Promise<Void> promise = Promise.promise();

    // Retrieve the title of application.
    JsonObject titleReceived = new JsonObject().put(TITLE,
      context.body().asJsonObject().getValue(TITLE));
    titleReceived.put(USER_ID, getUserId(context));

    mongoClient.findOne(collectionName, titleReceived, null, asyncResult -> {
      if (asyncResult.result() != null) {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "applicationTitleExists");
        promise.fail(asyncResult.cause());
      } else {
        promise.complete();
      }
    });
    return promise.future();
  }

  private ApplicationDTO generateCommonApplicationDTO(JsonObject jsonUser,
    RoutingContext context) {

    ApplicationDTO applicationDTO = null;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      applicationDTO = objectMapper.readValue(jsonUser.toString(),
        ApplicationDTO.class);
    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put(RESULT_MESSAGE,
        THE_PARSING_OF_JSON_FAILED_WITH_ERROR + e.getMessage()).encode());
    }
    return applicationDTO;
  }

  private Set<ConstraintViolation<ApplicationDTO>> validateCommonApplicationDTO(
    ApplicationDTO applicationDTO) {

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    return validator.validate(applicationDTO);
  }

  private Future<Void> retainOnlyValidDatasetsAndDistributions(RoutingContext context) {
    Promise<Void> promise = Promise.promise();
    JsonArray requestDatasets = context.body().asJsonObject().getJsonArray(DATASETS);

    fetchMongoDatasets(context)
      .onSuccess(mongoDatasets -> {
        JsonArray updatedDatasets = filterValidDatasets(requestDatasets, mongoDatasets);

        if (!updatedDatasets.isEmpty()) {
          context.body().asJsonObject().put(DATASETS, updatedDatasets);
          promise.complete();
        } else {
          handleError(context, HttpResponseStatus.BAD_REQUEST,
            "Provided datasets/distributions are not valid", promise);
        }
      })
      .onFailure(cause ->
        handleError(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Could not retrieve records from Basket collection", promise, cause));

    return promise.future();
  }

  private JsonArray filterValidDatasets(JsonArray requestDatasets, List<JsonObject> mongoDatasets) {
    JsonArray updatedDatasets = new JsonArray();

    requestDatasets.forEach(requestDatasetObj -> {
      JsonObject requestDataset = (JsonObject) requestDatasetObj;
      String requestDatasetId = requestDataset.getString(DATASET_ID);

      mongoDatasets.stream()
        .filter(mongoDataset -> requestDatasetId.equals(mongoDataset.getString(DATASET_ID)))
        .findFirst()
        .ifPresent(mongoDataset -> {
          JsonArray validDistributions = filterValidDistributions(
            requestDataset.getJsonArray(DISTRIBUTIONS),
            mongoDataset.getJsonArray(DISTRIBUTIONS)
          );

          if (!validDistributions.isEmpty()) {
            JsonObject datasetToStore = new JsonObject()
              .put(DATASET_ID, requestDatasetId)
              .put(CATALOG_ID, requestDataset.getString(CATALOG_ID))
              .put(DISTRIBUTIONS, validDistributions);

            updatedDatasets.add(datasetToStore);
          }
        });
    });

    return updatedDatasets;
  }

  private JsonArray filterValidDistributions(JsonArray requestDistributions,
    JsonArray mongoDistributions) {
    JsonArray validDistributions = new JsonArray();

    requestDistributions.forEach(requestDistributionObj -> {
      JsonObject requestDistribution = (JsonObject) requestDistributionObj;
      String requestDistributionId = requestDistribution.getString(DISTRIBUTION_ID);

      boolean isValid = mongoDistributions.stream()
        .map(mongoDistributionObj -> ((JsonObject) mongoDistributionObj).getString(DISTRIBUTION_ID))
        .anyMatch(requestDistributionId::equals);

      if (isValid) {
        validDistributions.add(requestDistribution);
      }
    });

    return validDistributions;
  }

  private void handleError(RoutingContext context, HttpResponseStatus status, String message,
    Promise<Void> promise) {
    setUpMessageContextResponse(context, status, message);
    promise.fail(message);
  }

  private void handleError(RoutingContext context, HttpResponseStatus status, String message,
    Promise<Void> promise, Throwable cause) {
    setUpMessageContextResponse(context, status, message);
    promise.fail(cause);
  }


  public Future<List<JsonObject>> fetchMongoDatasets(RoutingContext context) {

    Promise<List<JsonObject>> promise = Promise.promise();
    String userId = context.user().principal().getString("sub");

    dbHandler.findAll(BASKET_COLLECTION, null, null, null, null, null, userId)
      .onComplete(result -> {
        if (result.succeeded()) {
          promise.complete(result.result());
        } else {
          promise.fail(result.cause());
        }
      });

    return promise.future();
  }

  public void validateFile(RoutingContext context) {
    if (context.fileUploads().size() == 1) {
      FileUpload fileUploaded = context.fileUploads().get(0);
      if(isValidFileType(fileUploaded.fileName())){
        context.next();
      } else {
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "Uploaded file is not a valid type");
      }
    } else if (context.fileUploads().isEmpty()) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "No file found to upload");
    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Only 1 file allowed in upload");
    }
  }

  private boolean isValidFileType(String fileName) {
    return fileName.toLowerCase().matches(FILE_UPLOAD_VALID_TYPES);
  }
}
