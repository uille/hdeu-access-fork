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

import static eu.europa.data.health.access.Constants.MAX_ALLOWED_CHARS_36;
import static eu.europa.data.health.access.Constants.MAX_ALLOWED_CHARS_50;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.services.HubSearchClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogValidationManager extends AbstractSchemaValidator{

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final HubSearchClient hubSearchClient;

  private long auditLogSearchInterval = Constants.DEFAULT_AUDIT_LOG_DATASET_SEARCH_INTERVAL;
  private int auditLogSearchMaxRetries = Constants.DEFAULT_AUDIT_LOG_DATASET_MAX_RETRIES;

  public AuditLogValidationManager(
    JsonObject config, HubSearchClient hubSearchClient) {
    this.hubSearchClient = hubSearchClient;

    if (config.getLong(Constants.AUDIT_LOG_DATASET_SEARCH_INTERVAL) != null) {
      auditLogSearchInterval = config.getInteger(Constants.AUDIT_LOG_DATASET_SEARCH_INTERVAL);
    }
    if (config.getInteger(Constants.AUDIT_LOG_DATASET_MAX_RETRIES) != null) {
      auditLogSearchMaxRetries = config.getInteger(Constants.AUDIT_LOG_DATASET_MAX_RETRIES);
    }
  }

  public void validateSubmitAuditLogJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context,
      SchemaFiles.AUDIT_LOG_FOR_DATASET_RECORD.getValue());
  }

  public void validateSearchAuditLogJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context,
      SchemaFiles.AUDIT_LOG_FOR_SEARCH.getValue());
  }

  public void validateDatasetExists(@NotNull RoutingContext context) {

    if(!"delete".equalsIgnoreCase(context.body().asJsonObject().getString("datasetAction"))){

      String datasetId = context.body().asJsonObject().getString("datasetId");
      log.info("Will validate that dataset with id: {} exists in HubSearch", datasetId);
      // Start retry logic
      attemptDatasetValidation(context, hubSearchClient, datasetId, 0);

    } else {
      log.info("No dataset validations performed for delete actions");
      context.next();
    }
  }

  private void attemptDatasetValidation(RoutingContext context, HubSearchClient hubSearchClient,
    String datasetId, int attempt) {
    log.info("Dataset validation attempt {} for datasetId: {}", attempt + 1, datasetId);

    hubSearchClient.getDataset(datasetId)
      .onComplete(result -> {
        if (result.succeeded()) {
          JsonObject response = result.result();
          if (response.containsKey("result")) {
            log.info("Dataset exists.");
            context.next();
            return;
          }
        }

        // If no result or API error, decide whether to retry
        if (attempt < 9) { // 10 attempts total (0 to 9)
          log.warn("Dataset not found, will retry again... (Attempt {}/{})", attempt + 1, auditLogSearchMaxRetries);
          context.vertx().setTimer(auditLogSearchInterval, id ->
            attemptDatasetValidation(context, hubSearchClient, datasetId, attempt + 1));
        } else {
          // Final attempt failed
          log.error("Dataset validation failed after {} attempts for datasetId: {}", auditLogSearchMaxRetries, datasetId);
          setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
            String.format("Dataset id: %s not found after multiple attempts", datasetId));
        }
      });
  }

  public void validateFilters(@NotNull RoutingContext context) {
    log.info("Validating filters for Audit Logs");

    JsonObject searchFields = context.body().asJsonObject();
    JsonArray errors = new JsonArray();

    // Validate string fields with max 50 characters
    validateFieldLength("action", searchFields, MAX_ALLOWED_CHARS_50, errors);
    validateFieldLength("actionDescription", searchFields, MAX_ALLOWED_CHARS_50, errors);
    validateFieldLength("ecasId", searchFields, MAX_ALLOWED_CHARS_50, errors);
    validateFieldLength("firstName", searchFields, MAX_ALLOWED_CHARS_50, errors);
    validateFieldLength("lastName", searchFields, MAX_ALLOWED_CHARS_50, errors);

    // Validate objectId with max 36 characters
    validateFieldLength("objectId", searchFields, MAX_ALLOWED_CHARS_36, errors);

    // Validate date range
    validateDateRange(searchFields.getString("logDateFrom"), searchFields.getString("logDateTo"), errors);

    // Final response
    if (errors.isEmpty()) {
      log.info("Filter validation passed");
      context.next();
    } else {
      if (log.isErrorEnabled()) {
        log.error("Audit Log search validation failed with errors: {}", errors.encodePrettily());
      }
      setUpMessageContextResponseWithContent(context, HttpResponseStatus.BAD_REQUEST,
        "Audit Log search could not proceed due to validation errors", errors);
    }
  }

  private void validateFieldLength(String fieldName, JsonObject fields, int maxLength, JsonArray errors) {
    String value = fields.getString(fieldName);
    if (StringUtils.isNotEmpty(value) && value.length() > maxLength) {
      log.info("Validation error: '{}' exceeds max length of {}", fieldName, maxLength);
      errors.add(getSizeErrorMessage(fieldName, maxLength));
    }
  }

  private void validateDateRange(String fromStr, String toStr, JsonArray errors) {
    if (StringUtils.isNotEmpty(fromStr) && StringUtils.isNotEmpty(toStr)) {
      try {
        LocalDate from = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate to = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
        if (from.isAfter(to)) {
          log.info("Date validation error: logDateFrom {} is after logDateTo {}", from, to);
          errors.add(getDateErrorMessage());
        }
      } catch (DateTimeParseException e) {
        log.warn("Invalid date format provided: from='{}', to='{}'", fromStr, toStr);
        errors.add(getDateErrorMessage()); // Optionally add a separate error message for format
      }
    }
  }

  private JsonObject getSizeErrorMessage(String fieldName, int size){

    JsonArray params = new JsonArray();
    params.add(String.valueOf(size));

    return new JsonObject()
      .put("code", "Size")
      .put("errorMessage", "Field: " + fieldName + " cannot contain more than: " + size + " characters.")
      .put("field", fieldName)
      .put("params", params);
  }

  private JsonObject getDateErrorMessage(){

    return new JsonObject()
      .put("code", "mustBeHigherOfStartingDay")
      .put("errorMessage", "Field: logDateTo must be higher than field: logDateFrom")
      .put("field", "logDateFrom");
  }


}
