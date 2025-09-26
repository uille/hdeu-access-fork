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
package eu.europa.data.health.access.validations.business_validators;

import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager.buildCustomValidationErrors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.Section4BindSection3DTO;
import eu.europa.data.health.access.dto.sections.data_request.Section5BindToOtherSections;
import eu.europa.data.health.access.dto.sections.data_request.Section6BindToOtherSections;
import eu.europa.data.health.access.dto.sections.data_request.Section6CountryDTO;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.exceptions.GeneralException;
import eu.europa.data.health.access.validations.validators.AbstractSchemaValidator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

/**
 * DataRequestValidationManager class implementing all validations for the data request application
 * entity
 */
public class DataRequestApplicationBusinessValidationManager extends AbstractSchemaValidator {

  private final MongoClient mongoClient;

  public DataRequestApplicationBusinessValidationManager(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  public void businessValidateUpdate(RoutingContext context) {
    Integer sectionNumberFromUrl = Integer.valueOf(context.pathParam("section_number"));

    if(sectionNumberFromUrl == 1){
      section1Validation(context);
    } else if (sectionNumberFromUrl == 4) {
      section4Validation(context);
    } else if (sectionNumberFromUrl == 6) {
      section6Validation(context);
    } else if (sectionNumberFromUrl == 5) {
      section5Validation(context);
    } else {
      context.next();
    }
  }

  public void section1Validation(RoutingContext context){
    JsonObject sectionForm = context.body().asJsonObject();
    String applicationId = context.pathParam(APPLICATION_ID);
    JsonObject query = new JsonObject().put("_id", applicationId);

    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, query, null, ar -> {
      if (ar.succeeded() && !ar.result().isEmpty()) {
        JsonObject result = ar.result();

        // Check if the section and the field exist in the result
        if (result.containsKey("datasets")) {
          JsonArray datasets = result.getJsonArray("datasets", new JsonArray());
          Set<String> datasetIdsWithVars = new HashSet<>();

          for (int i = 0; i < datasets.size(); i++) {
            Object o = datasets.getValue(i);
            if (!(o instanceof JsonObject)) continue;
            JsonObject ds = (JsonObject) o;

            String dsId = ds.getString("dataset_id", ds.getString("id"));
            if (dsId == null) continue;

            boolean hasVars = false;

            // case A: dataset.variables
            JsonArray dsVars = ds.getJsonArray("variables");
            if (dsVars != null && !dsVars.isEmpty()) {
              hasVars = true;
            }

            // case B: dataset.distributions_sample[*].variables
            if (!hasVars) {
              JsonArray samples = ds.getJsonArray("distributions_sample");
              if (samples != null) {
                for (int s = 0; s < samples.size(); s++) {
                  Object so = samples.getValue(s);
                  if (!(so instanceof JsonObject)) continue;
                  JsonArray sampleVars = ((JsonObject) so).getJsonArray("variables");
                  if (sampleVars != null && !sampleVars.isEmpty()) {
                    hasVars = true;
                    break;
                  }
                }
              }
            }

            if (hasVars) {
              datasetIdsWithVars.add(dsId);
            }
          }

          // --- collect datasetIds provided in sectionForm ---
          JsonArray provided = sectionForm.getJsonArray("datasetVariables", new JsonArray());
          Set<String> providedIds = provided.stream()
            .filter(JsonObject.class::isInstance)
            .map(JsonObject.class::cast)
            .map(j -> j.getString("datasetId"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

          // --- find missing: DB says variables exist, but sectionForm has nothing for that dataset ---
          Set<String> missing = new HashSet<>(datasetIdsWithVars);
          missing.removeAll(providedIds);

          if (!missing.isEmpty()) {
            List<FieldError> errors = new ArrayList<>();
            for (String dsId : missing) {
              errors.add(new FieldError(
                  "NotNullVariables",
                  "Selected variable for the corresponding dataset should not be null",
                  dsId, null, null, null, null
                )
              );
            }

            context.put(ERRORS, new GeneralException(errors).getErrors());
          }

          context.next();
        } else {
          context.next();
        }
      }
    });
  }

  public boolean section4Validation(JsonObject form, RoutingContext context) {
    JsonObject sectionData = form.getJsonObject("section3");
    // Check if the field legalOrNaturalPerson exists
    if (sectionData.containsKey(LEGAL_OR_NATURAL_PERSON)
      && Objects.equals(sectionData.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString("key"), "a")) {
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Section4BindSection3DTO sectionDTO = objectMapper.readValue(
          form.getJsonObject("section4").toString(), Section4BindSection3DTO.class);

        Set<ConstraintViolation<Section4BindSection3DTO>> violations = validator.validate(
          sectionDTO);
        //if violations is not empty return false
        if (!violations.isEmpty()) {
          context.response().setStatusCode(403).end("Failed on section 4 business validation");
          return false;
        }
      } catch (JsonProcessingException e) {
        catchJsonException(context, e);
      }
    }
    return true;
  }

  public void section4Validation(RoutingContext context) {
    JsonObject requestApplicationId = new JsonObject().put("_id",
      context.pathParam(APPLICATION_ID));
    String sectionNumber = "section3";

    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, requestApplicationId, null, ar -> {
      if (ar.succeeded() && !ar.result().isEmpty()) {
        JsonObject result = ar.result();

        // Check if the section and the field exist in the result
        if (result.containsKey("form") && result.getJsonObject("form")
          .containsKey(sectionNumber)) {
          JsonObject sectionData = result.getJsonObject("form").getJsonObject(sectionNumber);
          // Check if the field legalOrNaturalPerson exists
          if (sectionData.containsKey(LEGAL_OR_NATURAL_PERSON)
            && Objects.equals(sectionData.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString("key"),
            "a")) {
            ObjectMapper objectMapper = new ObjectMapper();
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            try {
              Section4BindSection3DTO sectionDTO = objectMapper.readValue(
                context.body().asString(), Section4BindSection3DTO.class);

              // Validate SectionDTO
              Set<ConstraintViolation<Section4BindSection3DTO>> violations = validator.validate(
                sectionDTO);

              if (!violations.isEmpty()) {
                // Handle validation errors
                List<FieldError> errors = new ArrayList<>();
                var ctxErrors = context.get(ERRORS);
                if (Objects.nonNull(ctxErrors)) {
                  errors.addAll(context.get(ERRORS));
                }
                for (ConstraintViolation<Section4BindSection3DTO> violation : violations) {
                  String errorCode = violation.getConstraintDescriptor().getAnnotation()
                    .annotationType().getSimpleName();
                  String fieldName = violation.getPropertyPath().toString();
                  Object maxParam = violation.getConstraintDescriptor().getAttributes()
                    .get("max");
                  List<String> params = new ArrayList<>();
                  if (maxParam != null) {
                    params.add(maxParam.toString());
                    // Set value of field in context to null
                    context.body().asJsonObject().put(fieldName, null);
                  }
                  buildCustomValidationErrors(errors, errorCode, fieldName, params, violation);

                }
                context.put(ERRORS, new GeneralException(errors).getErrors());
                context.next();
              } else {
                context.next();
              }
            } catch (JsonProcessingException e) {
              catchJsonException(context, e);
            }
          } else {
            context.next();
          }
        } else {
          context.next();
        }
      }

    });
  }

  public void section5Validation(RoutingContext context) {
    // Search in the database based on the provided application ID
    JsonObject requestApplicationId = new JsonObject().put("_id",
      context.pathParam(APPLICATION_ID));

    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, requestApplicationId, null,
      ar -> {
        if (ar.succeeded() && !ar.result().isEmpty()) {
          JsonObject result = ar.result();

          // Check if the section and the field exist in the result
          if (result.containsKey("form") && (result.getJsonObject("form").containsKey(SECTION_2))) {
            JsonObject sectionData2 = result.getJsonObject("form").getJsonObject(SECTION_2);
            //T.01.2 section 2 field
            List<SelectObjectDTO> section2purposeForWhichDataWillBeUsed = new ArrayList<>();

            ObjectMapper objectMapper = new ObjectMapper();
            if (Objects.nonNull(sectionData2)) {
              String jsonValue = sectionData2.getString("purposeForWhichDataWillBeUsed");
              if (Objects.nonNull(jsonValue)) {
                try {
                  section2purposeForWhichDataWillBeUsed = objectMapper.readValue(jsonValue,
                    new TypeReference<List<SelectObjectDTO>>() {
                    });
                } catch (JsonProcessingException e) {
                  catchJsonException(context, e);
                }
              }
            }

            if (CollectionUtils.isNotEmpty(section2purposeForWhichDataWillBeUsed)) {
              ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
              Validator validator = factory.getValidator();

              try {
                Section5BindToOtherSections sectionDTO =
                  objectMapper.readValue(context.body().asString(),
                    Section5BindToOtherSections.class);
                sectionDTO.setSection2purposeForWhichDataWillBeUsed(
                  section2purposeForWhichDataWillBeUsed);

                // Validate SectionDTO
                Set<ConstraintViolation<Section5BindToOtherSections>> violations = validator.validate(
                  sectionDTO);

                if (!violations.isEmpty()) {
                  // Handle validation errors
                  List<FieldError> errors = new ArrayList<>();
                  var ctxErrors = context.get(ERRORS);
                  if (Objects.nonNull(ctxErrors)) {
                    errors.addAll(context.get(ERRORS));
                  }
                  for (ConstraintViolation<Section5BindToOtherSections> violation : violations) {
                    String errorCode = violation.getConstraintDescriptor()
                      .getAnnotation().annotationType().getSimpleName();
                    String fieldName = violation.getPropertyPath().toString();
                    Object maxParam = violation.getConstraintDescriptor().getAttributes()
                      .get("max");
                    List<String> params = new ArrayList<>();
                    if (maxParam != null) {
                      params.add(maxParam.toString());
                      // Set value of field in context to null
                      context.body().asJsonObject().put(fieldName, null);
                    }
                    buildCustomValidationErrors(errors, errorCode, fieldName, params,
                      violation);
                  }
                  context.put(ERRORS, new GeneralException(errors).getErrors());
                  context.next();
                } else {
                  context.next();
                }
              } catch (JsonProcessingException e) {
                catchJsonException(context, e);
              }
            } else {
              context.next();
            }
          } else {
            context.next();
          }
        }
      });
  }

  public void section6Validation(RoutingContext context) {
    JsonObject requestApplicationId = new JsonObject().put("_id",
      context.pathParam(APPLICATION_ID));

    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_REQUEST_APPLICATION_COLLECTION, requestApplicationId, null, ar -> {
      if (ar.succeeded() && !ar.result().isEmpty()) {
        JsonObject result = ar.result();

        // Check if the section and the field exist in the result
        if (result.containsKey("form") && result.getJsonObject("form")
          .containsKey(SECTION_2)) {

          JsonObject sectionData2 = result.getJsonObject("form").getJsonObject(SECTION_2);

          // Check if the field purposeForWhichDataWillBeUsed in section2 exists
          if (Objects.nonNull(sectionData2) && sectionData2.containsKey("purposeForWhichDataWillBeUsed")) {

            ObjectMapper objectMapper = new ObjectMapper();
            List<SelectObjectDTO> section5purposeOfUse = new ArrayList<>();
            String jsonValue = sectionData2.getString("purposeForWhichDataWillBeUsed");

            if (Objects.nonNull(jsonValue)) {
              try {
                section5purposeOfUse = objectMapper.readValue(jsonValue,
                  new TypeReference<>() {
                  });
              } catch (JsonProcessingException e) {
                catchJsonException(context, e);
              }
            }

            if (CollectionUtils.isNotEmpty(section5purposeOfUse)) {
              ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
              Validator validator = factory.getValidator();

              try {
                List<Section6CountryDTO> section6Countries = objectMapper.readValue(
                  context.body().asJsonArray().toString(),
                  new TypeReference<>() {
                  }
                );

                List<SelectObjectDTO> finalSection5purposeOfUse = section5purposeOfUse;
                section6Countries.forEach(sectionCountry -> {
                  Section6BindToOtherSections sectionDTO = new Section6BindToOtherSections();
                  sectionDTO.setWhatIsTheFrequencyOfUpdates(
                    sectionCountry.getWhatIsTheFrequencyOfUpdates());
                  sectionDTO.setSection2purposeOfUse(finalSection5purposeOfUse);

                  Set<ConstraintViolation<Section6BindToOtherSections>> violations = validator.validate(
                    sectionDTO);

                  // Validate SectionDTO
                  if (!violations.isEmpty()) {
                    // Handle validation errors
                    List<FieldError> errors = new ArrayList<>();
                    var ctxErrors = context.get(ERRORS);
                    if (Objects.nonNull(ctxErrors)) {
                      errors.addAll(context.get(ERRORS));
                    }
                    for (ConstraintViolation<Section6BindToOtherSections> violation : violations) {
                      String errorCode = violation.getConstraintDescriptor()
                        .getAnnotation().annotationType().getSimpleName();
                      String fieldName = violation.getPropertyPath().toString();
                      List<String> params = new ArrayList<>();
                      Object maxParam = violation.getConstraintDescriptor().getAttributes()
                        .get("max");
                      if (maxParam != null) {
                        params.add(maxParam.toString());
                      }

                      buildCustomValidationErrors(errors, errorCode, fieldName, params,
                        violation, sectionCountry.getCountryId());
                    }
                    context.put(ERRORS, new GeneralException(errors).getErrors());
                  }
                });
                context.next();
              } catch (JsonProcessingException e) {
                catchJsonException(context, e);
              }
            } else {
              context.next();
            }
          } else {
            context.next();
          }
        } else {
          context.next();
        }
      }
    });
  }

  private void catchJsonException(RoutingContext context, JsonProcessingException e) {
    context.response().setStatusCode(500).end(new JsonObject().put("result_message",
      "The parsing of json failed with error: " + e.getMessage()).encode());
  }
}
