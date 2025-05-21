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

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.services.DataAccessApplicationManager.extractField;
import static eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager.buildCustomValidationErrors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.data_access.Section4BindSection3DTO;
import eu.europa.data.health.access.dto.data_access.Section5BindToOtherSections;
import eu.europa.data.health.access.dto.data_access.Section8BindToOtherSections;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.exceptions.GeneralException;
import eu.europa.data.health.access.validations.validators.AbstractSchemaValidator;
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
import org.apache.commons.collections.CollectionUtils;

/**
 * DataAccessValidationManager class implementing all validations for the data access application
 * entity
 */
public class DataAccessApplicationBusinessValidationManager extends AbstractSchemaValidator {

  private final MongoClient mongoClient;

  public DataAccessApplicationBusinessValidationManager(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  public void businessValidateUpdate(RoutingContext context) {
    Integer sectionNumberFromUrl = Integer.valueOf(context.pathParam("section_number"));

    if (sectionNumberFromUrl == 4) {
      section4Validation(context);
    } else if (sectionNumberFromUrl == 8) {
      section8Validation(context);
    }else if (sectionNumberFromUrl == 5) {
      section5Validation(context);
    }
    else {
      context.next();
    }
  }

  public boolean section4Validation(JsonObject form, RoutingContext context) {
    JsonObject sectionData = form.getJsonObject(SECTION_3);
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
      context.pathParam("application_id"));
    String sectionNumber = SECTION_3;

    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, requestApplicationId, null, ar -> {
      if (ar.succeeded() && !ar.result().isEmpty()) {
          JsonObject result = ar.result();

          // Check if the section and the field exist in the result
          if (result.containsKey("form") && result.getJsonObject("form")
            .containsKey(sectionNumber)) {
            JsonObject sectionData = result.getJsonObject("form").getJsonObject(sectionNumber);
            // Check if the field legalOrNaturalPerson exists
            if (sectionData.containsKey(LEGAL_OR_NATURAL_PERSON)
              && Objects.equals(sectionData.getJsonObject(LEGAL_OR_NATURAL_PERSON).getString("key"), "a")) {
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

  public boolean section8Validation(JsonObject form, RoutingContext context) {

    boolean result = false;

    JsonObject sectionData2 = form.getJsonObject("section2");
    JsonObject sectionData3 = form.getJsonObject(SECTION_3);
    JsonObject sectionData5 = form.getJsonObject("section5");
    JsonObject sectionData7 = form.getJsonObject("section7");
    JsonObject sectionData8 = form.getJsonObject("section8");

    //T.01.2 section 2 field
    List<SelectObjectDTO> section2purposeForWhichDataWillBeUsed = new ArrayList<>();
    //T.01.3
    SelectObjectDTO section3applyingForDataOnBehalfOfPublicSector = null;
    SelectObjectDTO section3legalOrNaturalPerson = null;
    //T.01.10
    SelectObjectDTO section7willDataBeCombinedWithDataObtained = null;
    // T.01.5
    SelectObjectDTO section5formatOfTheElectronicHealthData = null;

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
    if (Objects.nonNull(sectionData3)) {
      String jsonValue = sectionData3.getString("applyingForDataOnBehalfOfPublicSector");
      if (Objects.nonNull(jsonValue)) {
        try {
          section3applyingForDataOnBehalfOfPublicSector = objectMapper.readValue(jsonValue,
            new TypeReference<SelectObjectDTO>() {
            });
        } catch (JsonProcessingException e) {
          catchJsonException(context, e);
        }
      }
      String jsonValue2 = sectionData3.getString(LEGAL_OR_NATURAL_PERSON);
      if (Objects.nonNull(jsonValue2)) {
        try {
          section3legalOrNaturalPerson = objectMapper.readValue(jsonValue2,
            new TypeReference<SelectObjectDTO>() {
            });
        } catch (JsonProcessingException e) {
          catchJsonException(context, e);
        }
      }
    }
    if (Objects.nonNull(sectionData7)) {
      String jsonValue = sectionData7.getString("willDataBeCombinedWithDataObtained");
      if (Objects.nonNull(jsonValue)) {
        try {
          section7willDataBeCombinedWithDataObtained = objectMapper.readValue(jsonValue,
            new TypeReference<SelectObjectDTO>() {
            });
        } catch (JsonProcessingException e) {
          catchJsonException(context, e);
        }
      }
    }
    if (Objects.nonNull(sectionData5)) {
      String jsonValue = sectionData5.getString("formatOfTheElectronicHealthData");
      if (Objects.nonNull(jsonValue)) {
        try {
          section5formatOfTheElectronicHealthData = objectMapper.readValue(jsonValue,
            new TypeReference<SelectObjectDTO>() {
            });
        } catch (JsonProcessingException e) {
          catchJsonException(context, e);
        }
      }
    }

    if (CollectionUtils.isNotEmpty(section2purposeForWhichDataWillBeUsed)
      || Objects.nonNull(section3applyingForDataOnBehalfOfPublicSector)
      || Objects.nonNull(section3legalOrNaturalPerson)
      || Objects.nonNull(section7willDataBeCombinedWithDataObtained)
      || Objects.nonNull(section5formatOfTheElectronicHealthData)) {
      ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      Validator validator = factory.getValidator();

      try {
        Section8BindToOtherSections sectionDTO =
          objectMapper.readValue(sectionData8.toString(),
            Section8BindToOtherSections.class);
        sectionDTO.setSection2purposeForWhichDataWillBeUsed(section2purposeForWhichDataWillBeUsed);
        sectionDTO.setSection3applyingForDataOnBehalfOfPublicSector(
          section3applyingForDataOnBehalfOfPublicSector);
        sectionDTO.setSection3legalOrNaturalPerson(section3legalOrNaturalPerson);
        sectionDTO.setSection7willDataBeCombinedWithDataObtained(
          section7willDataBeCombinedWithDataObtained);
        sectionDTO.setSection5formatOfTheElectronicHealthData(section5formatOfTheElectronicHealthData);

        // Validate SectionDTO
        Set<ConstraintViolation<Section8BindToOtherSections>> violations = validator.validate(
          sectionDTO);

        if (violations.isEmpty()) {
          result = true;
        } else {
          context.response().setStatusCode(403).end("Failed on section 8 business validation");
        }
      } catch (JsonProcessingException e) {
        catchJsonException(context, e);
      }
    }
    return result;
  }

  public void section8Validation(RoutingContext context){
    JsonObject requestApplicationId = new JsonObject().put("_id", context.pathParam("application_id"));
    String section2 = "section2";
    String section3 = SECTION_3;
    String section7 = "section7";
    String section5 = "section5";
    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, requestApplicationId, null,
      ar -> {
        if (ar.succeeded() && !ar.result().isEmpty()) {
            JsonObject result = ar.result();

            // Check if the section and the field exist in the result
            if (result.containsKey("form") && (result.getJsonObject("form").containsKey(section2)
              ||result.getJsonObject("form").containsKey(section3)
              || result.getJsonObject("form").containsKey(section7))) {
              JsonObject sectionData2 = result.getJsonObject("form").getJsonObject(section2);
              JsonObject sectionData3 = result.getJsonObject("form").getJsonObject(section3);
              JsonObject sectionData7 = result.getJsonObject("form").getJsonObject(section7);
              JsonObject sectionData5 = result.getJsonObject("form").getJsonObject(section5);
              //T.01.2 section 2 field
              List<SelectObjectDTO> section2purposeForWhichDataWillBeUsed = new ArrayList<>();
              //T.01.3
              SelectObjectDTO section3applyingForDataOnBehalfOfPublicSector = null;
              SelectObjectDTO section3legalOrNaturalPerson = null;
              //T.01.10
              SelectObjectDTO section7willDataBeCombinedWithDataObtained = null;
              // T.01.5
              SelectObjectDTO section5formatOfTheElectronicHealthData = null;
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
              if (Objects.nonNull(sectionData3)) {
                String jsonValue = sectionData3.getString("applyingForDataOnBehalfOfPublicSector");
                if (Objects.nonNull(jsonValue)) {
                  try {
                    section3applyingForDataOnBehalfOfPublicSector = objectMapper.readValue(jsonValue,
                      new TypeReference<SelectObjectDTO>() {
                      });
                  } catch (JsonProcessingException e) {
                    catchJsonException(context, e);
                  }
                }
                String jsonValue2 = sectionData3.getString(LEGAL_OR_NATURAL_PERSON);
                if (Objects.nonNull(jsonValue2)) {
                  try {
                    section3legalOrNaturalPerson = objectMapper.readValue(jsonValue2,
                      new TypeReference<SelectObjectDTO>() {
                      });
                  } catch (JsonProcessingException e) {
                    catchJsonException(context, e);
                  }
                }
              }
              if (Objects.nonNull(sectionData7)) {
                String jsonValue = sectionData7.getString("willDataBeCombinedWithDataObtained");
                if (Objects.nonNull(jsonValue)) {
                  try {
                    section7willDataBeCombinedWithDataObtained = objectMapper.readValue(jsonValue,
                      new TypeReference<SelectObjectDTO>() {
                      });
                  } catch (JsonProcessingException e) {
                    catchJsonException(context, e);
                  }
                }
              }

              if (Objects.nonNull(sectionData5)) {
                String jsonValue = sectionData5.getString("formatOfTheElectronicHealthData");
                if (Objects.nonNull(jsonValue)) {
                  try {
                    section5formatOfTheElectronicHealthData = objectMapper.readValue(jsonValue,
                      new TypeReference<SelectObjectDTO>() {
                      });
                  } catch (JsonProcessingException e) {
                    catchJsonException(context, e);
                  }
                }
              }

              if (CollectionUtils.isNotEmpty(section2purposeForWhichDataWillBeUsed)
                || Objects.nonNull(section3applyingForDataOnBehalfOfPublicSector)
                || Objects.nonNull(section3legalOrNaturalPerson)
                || Objects.nonNull(section7willDataBeCombinedWithDataObtained)
                || Objects.nonNull(section5formatOfTheElectronicHealthData)) {
                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                Validator validator = factory.getValidator();

                try {
                  Section8BindToOtherSections sectionDTO =
                    objectMapper.readValue(context.body().asString(),
                      Section8BindToOtherSections.class);
                  sectionDTO.setSection2purposeForWhichDataWillBeUsed(section2purposeForWhichDataWillBeUsed);
                  sectionDTO.setSection3applyingForDataOnBehalfOfPublicSector(
                    section3applyingForDataOnBehalfOfPublicSector);
                  sectionDTO.setSection3legalOrNaturalPerson(section3legalOrNaturalPerson);
                  sectionDTO.setSection7willDataBeCombinedWithDataObtained(
                    section7willDataBeCombinedWithDataObtained);
                  sectionDTO.setSection5formatOfTheElectronicHealthData(section5formatOfTheElectronicHealthData);

                  // Validate SectionDTO
                  Set<ConstraintViolation<Section8BindToOtherSections>> violations = validator.validate(
                    sectionDTO);

                  if (!violations.isEmpty()) {
                    // Handle validation errors
                    List<FieldError> errors = new ArrayList<>();
                    var ctxErrors = context.get(ERRORS);
                    if (Objects.nonNull(ctxErrors)) {
                      errors.addAll(context.get(ERRORS));
                    }
                    for (ConstraintViolation<Section8BindToOtherSections> violation : violations) {
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

  public void section5Validation(RoutingContext context){
    JsonObject requestApplicationId = new JsonObject().put("_id", context.pathParam("application_id"));
    String section2 = "section2";

    // Search in the database based on the provided application ID
    mongoClient.findOne(DATA_ACCESS_APPLICATION_COLLECTION, requestApplicationId, null,
      ar -> {
        if (ar.succeeded() && !ar.result().isEmpty()) {
          JsonObject result = ar.result();

          // Check if the section and the field exist in the result
          if (result.containsKey("form") && (result.getJsonObject("form").containsKey(section2))) {
            JsonObject sectionData2 = result.getJsonObject("form").getJsonObject(section2);
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
                sectionDTO.setSection2purposeForWhichDataWillBeUsed(section2purposeForWhichDataWillBeUsed);

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

  private void catchJsonException(RoutingContext context, JsonProcessingException e) {
    context.response().setStatusCode(500).end(new JsonObject().put("result_message",
      "The parsing of json failed with error: " + e.getMessage()).encode());
  }

  /**
   * Validate tha catalogs from datasets and section 6 are the same
   */
  public boolean validateCatalogs(JsonObject application, RoutingContext context) {
    String jsonString = application.toString();
    HashSet<String> section6CatalogIds = new HashSet<>();
    HashSet<String> datasetsCatalogIds = new HashSet<>();
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(jsonString);
      extractField(rootNode.get("datasets"), datasetsCatalogIds, "catalog_id");
      extractField(rootNode.get("form"), section6CatalogIds, "catalog_ids");
    } catch (JsonProcessingException e) {
      catchJsonException(context, e);
    }
    // Check if all datasetsCatalogIds are in section6CatalogIds and vice versa
    if (!section6CatalogIds.containsAll(datasetsCatalogIds)) {
      context.response().setStatusCode(403)
        .end("Not all dataset catalog IDs exist in section 6 catalog IDs.");
      return false;
    }
    if (!datasetsCatalogIds.containsAll(section6CatalogIds)) {
      context.response().setStatusCode(403)
        .end("Not all section 6 catalog IDs exist in dataset catalog IDs.");
      return false;
    }
    return true;
  }

}
