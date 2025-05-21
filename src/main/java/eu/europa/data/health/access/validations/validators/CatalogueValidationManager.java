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

import static eu.europa.data.health.access.Constants.*;
import static eu.europa.data.health.access.Constants.FILE_UPLOAD_CATALOGUE_VALID_TYPES;
import static eu.europa.data.health.access.SchemaFiles.COUNTRIES_RDF;
import static eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager.buildCustomValidationErrors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.annotations.AttachmentValidation;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.dto.CatalogueDTO;
import eu.europa.data.health.access.dto.RDFCatalogueDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.exceptions.GeneralException;
import eu.europa.data.health.access.hub_repo_client.HubRepoClient;
import eu.europa.data.health.access.services.CountriesManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CatalogueValidationManager class implementing all validations for the Catalogue entity
 */
public class CatalogueValidationManager extends AbstractSchemaValidator{

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final HubRepoClient hubRepoClient;
  private final DispatcherClient dispatcherClient;
  private final Vertx vertx;
  private final CountriesManager countriesManager;

  public CatalogueValidationManager(HubRepoClient hubRepoClient, DispatcherClient dispatcherClient, Vertx vertx, CountriesManager countriesManager) {
    this.hubRepoClient = hubRepoClient;
    this.dispatcherClient = dispatcherClient;
    this.vertx = vertx;
    this.countriesManager = countriesManager;
  }

  /**
   * Validate update catalogue.
   * @param context The context provided.
   */
  public void validateUpdate(@NotNull RoutingContext context) {
    List<FieldError> errors = new ArrayList<>();
    hubRepoClient.existsCatalogueByDomibusIdOrCatalogueIdOrCountry(context).onSuccess(result -> {
      setUniqueFieldErrors(result, errors);
      validateCatalogueBothWays(context, errors);
    }).onFailure(cause -> {
      validateCatalogueBothWays(context, errors);
    });
  }

  /**
   * Generate catalogue dto file from context.
   * @param catalogueString Catalogue file in string.
   * @param context The provided context.
   * @return The generated catalogue dto object.
   */
  public CatalogueDTO generateCatalogueDTO(String catalogueString, RoutingContext context) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();

      // Read JSON payload as a generic Map<String, Object>
      return objectMapper.readValue(catalogueString,  CatalogueDTO.class);

    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put(RESULT_MESSAGE,
        THE_PARSING_OF_JSON_FAILED_WITH_ERROR + e.getMessage()).encode());
    }
    return null;
  }

  /**
   * Validate the criteria send for the catalogue search.
   * @param context The criteria of the catalogue search.
   */
  public void validateSearchCatalogueJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.CATALOGUE_SEARCH.getValue());
  }
  private RDFCatalogueDTO generateRDFCatalogueDTO(String catalogueString, RoutingContext context) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();

      // Read JSON payload as a generic Map<String, Object>
      return objectMapper.readValue(catalogueString,  RDFCatalogueDTO.class);

    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put(RESULT_MESSAGE,
        THE_PARSING_OF_JSON_FAILED_WITH_ERROR + e.getMessage()).encode());
    }
    return null;
  }

  private void validateCatalogueBothWays(RoutingContext context, List<FieldError> errors) {
    JsonObject uploadRDF = context.body().asJsonObject()
      .getJsonObject("uploadRDF");

    if(Objects.nonNull(uploadRDF) && uploadRDF.getValue("key").equals("a")){
      validateUpdateWithRDF(context, errors);
    } else {
      validateUpdateCatalogue(context, errors);
    }
  }

  private void validateUpdateWithRDF(RoutingContext context, List<FieldError> errors){
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    // Generate a CatalogueDTO object from context
    RDFCatalogueDTO catalogueDTO;

    JsonObject catalogueForm = context.body().asJsonObject();
    context.put("body", catalogueForm.toString());
    catalogueDTO = generateRDFCatalogueDTO(catalogueForm.toString(),context);

    // Validate CatalogueDTO
    Set<ConstraintViolation<RDFCatalogueDTO>> violations = validator.validate(catalogueDTO);

    if (!violations.isEmpty()) {
      // Handle validation errors
      for (ConstraintViolation<RDFCatalogueDTO> violation : violations) {
        String errorCode = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();

        String fieldName = violation.getPropertyPath().toString();
        Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
        List<String> params = new ArrayList<>();
        if (maxParam != null) {
          params.add(maxParam.toString());
          // Set value of field in context to null
          context.body().asJsonObject().put(fieldName, null);
        }
        buildCustomValidationErrors(errors, errorCode, fieldName, params,
          violation);
        // Override the AttachmentValidation error message.
        if (errorCode.equals(AttachmentValidation.class.getSimpleName())) {
          errors.removeIf(error -> errorCode.equals(error.getCode()));
          // create the error object
          errors.add(new FieldError(
            "rdfAttachmentValidation",
            violation.getConstraintDescriptor()
              .getAttributes().get(MESSAGE).toString(),
            ((PathImpl) violation.getPropertyPath()).getLeafNode().getParent().getName(),
            null,
            null,
            null,
            null));
        }
      }
    }
    // Add the errors except dto object
    JsonObject fieldNameContent = context.body().asJsonObject()
      .getJsonObject("rdfTemplate");

    if(fieldNameContent == null){
      FieldError error = new FieldError("NotBlank", "NotBlank",
        "rdfTemplate", null, null, null, null);
      errors.add(error);
    }

    if(!errors.isEmpty()){
      context.put(ERRORS, new GeneralException(errors).getErrors());
    }

    context.next();
  }

  private void validateUpdateCatalogue(RoutingContext context, List<FieldError> errors){
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    // Generate a CatalogueDTO object from context
    CatalogueDTO catalogueDTO;

    JsonObject catalogueForm = context.body().asJsonObject();
    context.put("body", catalogueForm.toString());
    catalogueDTO = generateCatalogueDTO(catalogueForm.toString(),context);

    // Validate CatalogueDTO
    Set<ConstraintViolation<CatalogueDTO>> violations = validator.validate(catalogueDTO);

    if (!violations.isEmpty()) {
      // Handle validation errors
      for (ConstraintViolation<CatalogueDTO> violation : violations) {
        String errorCode = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();

        String fieldName = violation.getPropertyPath().toString();
        Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
        List<String> params = new ArrayList<>();
        if (maxParam != null) {
          params.add(maxParam.toString());
          // Set value of field in context to null
          context.body().asJsonObject().put(fieldName, null);
        }
        buildCustomValidationErrors(errors, errorCode, fieldName, params,
          violation);
      }
    }

    if(!errors.isEmpty()){
      context.put(ERRORS, new GeneralException(errors).getErrors());
    }

    context.next();
  }

  private void setUniqueFieldErrors(JsonObject result, List<FieldError> errors){
    if(result != null){
      String domibusId = result.getString(DOMIBUS_ID);
      String catalogueId = result.getString(CATALOGUE_ID);
      String geographicalCoverage = result.getString(GEOGRAPHICAL_COVERAGE);

      if(catalogueId != null && !catalogueId.isEmpty()){
        FieldError fieldError = new FieldError("catalogueIdExists", "Catalogue id exists", CATALOGUE_ID,
          null, null, null, null);
        errors.add(fieldError);
      }
      if(domibusId != null && !domibusId.isEmpty()){
        FieldError fieldError = new FieldError("domibusIdExists", "Domibus id exists", DOMIBUS_ID,
          null, null, null, null);
        errors.add(fieldError);
      }
      if(geographicalCoverage != null && !geographicalCoverage.isEmpty()){
        FieldError fieldError = new FieldError("geographicalCoverageExists", "Geographical coverage exists", GEOGRAPHICAL_COVERAGE,
          null, null, null, null);
        errors.add(fieldError);
      }
    }
  }

  public void validateCatalogMappingForDeletion(@NotNull RoutingContext context) {

    String catalogId = context.pathParam("catalogueId");
    if(StringUtils.isNotEmpty(catalogId)) {

      log.info("Entered validateCatalogMapping with catalog id: {}", catalogId);

      getCatalogMappingByCatalogId(catalogId, context)
        .onComplete(catalogMappingObject -> {
          if (catalogMappingObject.succeeded()) {

            JsonObject catalogMapping = catalogMappingObject.result();
            log.info("Catalog mapping retrieved: {}", catalogMapping.toString());

            String isProduction = catalogMapping.getString("isProduction");
            if("true".equals(isProduction)){
              log.info("Catalog mapping is in production");

              getDatasetsByCatalogId(catalogId)
                .onComplete(datasetsArray -> {
                  if (datasetsArray.succeeded()) {
                    JsonArray datasets = datasetsArray.result();
                    log.info("Datasets retrieved, with size: {}", datasets.size());
                    if(datasets.isEmpty()){
                      log.info("Datasets size is 0, you can proceed with deletion");
                      context.next();
                    } else {
                      log.error("Provided catalog contains datasets that need to be deleted");
                      context.response().setStatusCode(400).end(new JsonObject().put("errorMessage", "datasetsExist").encode());
                    }
                  } else {
                    log.error("Error was thrown during getDatasetsByCatalogId(), with message: {}", datasetsArray.cause().getMessage());
                    setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, datasetsArray.cause().getMessage());
                  }
                });

            } else {
              log.info("Catalog mapping is not in production");
              context.next();
            }

          } else {
            log.error("Error was thrown during getCatalogMappingByCatalogId(), with message: {}", catalogMappingObject.cause().getMessage());
            setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, catalogMappingObject.cause().getMessage());
          }
        })
        .onFailure(
          fail -> setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Error while trying to retrieve catalog mapping, with error message: "
              + fail.getMessage())
        );

    } else {
      log.error("Provided catalog is is null/empty");
      setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Provided catalog is is null/empty");
    }
  }

  private Future<JsonObject> getCatalogMappingByCatalogId(String catalogId, @NotNull RoutingContext context) {

    Promise<JsonObject> promise = Promise.promise();

    dispatcherClient.getCatalogMappingByCatalogId(catalogId)
      .onComplete(actionRes -> {
      if (actionRes.succeeded()) {
        log.info("Successfully retrieved catalog mapping");
        context.put("catalogMappingMongoId", actionRes.result().getString("_id"));
        promise.complete(actionRes.result());
      } else {
        log.error("Could not retrieve catalog mapping");
        promise.fail(actionRes.cause().getMessage());
      }
    });
    return promise.future();
  }

  private Future<JsonArray> getDatasetsByCatalogId(String catalogId) {

    Promise<JsonArray> promise = Promise.promise();

    hubRepoClient.getDatasetsByCatalogId(catalogId)
      .onComplete(actionRes -> {
        if (actionRes.succeeded()) {
          log.info("Successfully retrieved catalog mapping");
          promise.complete(actionRes.result());
        } else {
          log.error("Could not retrieve catalog mapping");
          promise.fail(actionRes.cause().getMessage());
        }
      });
    return promise.future();
  }

  public void validateFileCatalogue(RoutingContext context) {
    if (context.fileUploads().size() == 1) {
      FileUpload fileUploaded = context.fileUploads().get(0);
      String validFileList = FILE_UPLOAD_CATALOGUE_VALID_TYPES;
      if(!isValidFileType(fileUploaded.fileName(), validFileList)){
        setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
          "Uploaded file is not a valid type");
      }

      // Get the uploaded file
      vertx.fileSystem().readFile(fileUploaded.uploadedFileName())
        .onSuccess(fileBuffer -> {
          validateGeographicalCoverage(context, fileBuffer.toString())
            .onSuccess(result -> {
              if (!result.isEmpty()) {
                context.put(ERRORS, new GeneralException(result).getErrors());
              }
              context.next();
            })
            .onFailure(err -> {
              setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
                "Something went wrong on validation of Geographical Coverage");
            });
        })
        .onFailure(err -> {
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file");
        });
    } else if (context.fileUploads().isEmpty()) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "No file found to upload");
    } else {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Only 1 file allowed in upload");
    }
  }

  private Future<List<FieldError>> validateGeographicalCoverage(RoutingContext context, String rdfContent){
    Promise<List<FieldError>> promise = Promise.promise();
    List<FieldError> errors = new ArrayList<>();
    String currentGeographicalURL;
    if (context.queryParam("currentGeographicalURL") != null && !context.queryParam("currentGeographicalURL").isEmpty()) {
      currentGeographicalURL = context.queryParam("currentGeographicalURL").get(0);
    } else {
      currentGeographicalURL = "";
    }


    Pattern pattern = Pattern.compile("dct:spatial\\s+[^>]*?>");
    Matcher matcher = pattern.matcher(rdfContent);

    boolean matcherFound = matcher.find();
    // Matcher for json and jsonld files.
    if(!matcherFound){
      pattern = Pattern.compile("\"dct:spatial\"\\s*:\\s*\\{\\s*\"@id\"\\s*:\\s*\"([^\"]+)\"");
      matcher = pattern.matcher(rdfContent);
      matcherFound = matcher.find();
    }

    if (matcherFound) {
      String spatialLine = matcher.group();

      // Check if the geographical coverage exists already in the system.
      hubRepoClient.getAllGeographicalCoverage(context).onComplete(response ->{
        if(response.result() != null){
          JsonArray selectedObjectList = response.result().getJsonArray("geographicalCoverageList");
          for (Object selectedObjectObj : selectedObjectList) {
            SelectObjectDTO selectedObject = (SelectObjectDTO) selectedObjectObj;
            if(spatialLine.contains(selectedObject.getKey()) && !currentGeographicalURL.equals(selectedObject.getKey())){
              List<String> params = new ArrayList<>();
              params.add("country");
              params.add(selectedObject.getValue());
              FieldError error = new FieldError("geographicalCoverageExistsForCountry", "Geographical coverage id exists", eu.europa.data.health.access.Constants.RDF_TEMPLATE,
                null, params, null, null);
              errors.add(error);
            }
          }
        }

        // Check if the geographical coverage is a EU country.
        JsonArray countries = countriesManager.getCountriesJsonArray(getCountriesModel(),"en",COUNTRIES_EU_EEA);
        if(!countries.isEmpty()){
          boolean itIsEUCountry= false;

          if (spatialLine.contains("EUR")) {
            itIsEUCountry = true;
          } else {
            for (Object selectedObjectObj : countries) {
              JsonObject country =  (JsonObject) selectedObjectObj;
              String url = country.getString("country");
              String countryCode = url.substring(url.lastIndexOf('/') + 1);
              if( spatialLine.contains(countryCode)){
                itIsEUCountry = true;
                break;
              }
            }
          }
          if(!itIsEUCountry){
            FieldError error = new FieldError("geographicalCoverageNoEU", "Geographical coverage is not a EU country", RDF_TEMPLATE,
              null, null, null, null);
            errors.add(error);
          }
        }
        promise.complete(errors);
      }).onFailure(promise::fail);
    } else {
      promise.fail("The spatial did not excluded.");
    }
    return promise.future();
  }

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

  private boolean isValidFileType(String fileName, String validFileList) {
    return fileName.toLowerCase().matches(validFileList);
  }

}
