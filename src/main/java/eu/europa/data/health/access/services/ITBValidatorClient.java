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

import static eu.europa.data.health.access.Constants.CONTENT_TYPE;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ITBValidatorClient class provides methods to interact with ITB SHACL Validator.
 */
public class ITBValidatorClient {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebClient client;
  private final String itbValidatorEndpoint;
  private static final String APPLICATION_JSON = "application/json";
  private static final String STRUCTURAL_ERROR_STR = "An error occurred while reading the provided content";

  public ITBValidatorClient(WebClient client, String itbValidatorEndpoint) {
    this.client = client;
    this.itbValidatorEndpoint = itbValidatorEndpoint;
  }

  /**
   * Validates a dataset by sending it to the ITB SHACL Validator.
   *
   * @param context The routing context containing the request data.
   */
  public void validateDataset(RoutingContext context) {
    log.debug("Validating dataset...");

    String rdf = context.body().asString();
    String accessLevel = context.request().getParam("accessLevel");
    client.postAbs(itbValidatorEndpoint)
      .timeout(10000) // Set a 10 seconds timeout for the request
      .sendJsonObject(createRequestBody(rdf, accessLevel), ar -> {
        // Fail the future if the request failed
        if (ar.failed()) {
          Throwable cause = ar.cause();
          log.error("Failed to communicate with ITB validation service", cause);
          context.response().setStatusCode(500).end(getItbFailedResponseMessage(cause));
          return;
        }

        var response = ar.result();
        String responseBody = response.bodyAsString() != null ? response.bodyAsString().trim() : "";

        // Return a 500 error if the response status code is not 200
        // and the response body does not contain the structural error string
        if (response.statusCode() != 200 && !responseBody.contains(STRUCTURAL_ERROR_STR)) {
          String message = "Status %d: %s".formatted(response.statusCode(),
            response.statusMessage());
          if (!StringUtils.isEmpty(responseBody)) {
            message += ", " + responseBody;
          }
          log.error("Unexpected error during dataset validation with {}", message);
          context.response().setStatusCode(500).end(message);
          return;
        }

        try {
          // Parse the response body as a JSON object
          JsonObject responseJson = response.bodyAsJsonObject();

          // Handle the case of structural errors where ITB returns a 500 status code and a response
          // body with a message property starting with the structural error string
          if (
            response.statusCode() == 500 &&
              responseJson.getString("message", "").trim().startsWith(STRUCTURAL_ERROR_STR)
          ) {
            context.response()
              .putHeader(CONTENT_TYPE, APPLICATION_JSON)
              .end(
                createResponse(
                  responseJson.getString("timestamp"),
                  responseJson.getString("message").trim()
                ).encodePrettily()
              );
            return;
          }

          // If the response is valid, return it with a 200 status code
          context.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(responseJson.encodePrettily());
        } catch (DecodeException e) {
          // If parsing fails, return a 500 error with the response body
          log.error("Failed to parse response from validation service: {}", responseBody, e);
          String errorMessage = "Failed to parse response from validation service: " + responseBody;
          context.response().setStatusCode(500).end(errorMessage);
        }
      });
  }

  /**
   * Creates the JSON body for the validation request.
   *
   * @param rdf The RDF content to be validated.
   * @param accessLevel The access level for the validation (public, restricted, non-public).
   * @return The JSON body as a JsonObject.
   */
  private JsonObject createRequestBody(String rdf, String accessLevel) {
    return new JsonObject()
      .put("contentToValidate", Base64.getEncoder().encodeToString(rdf.getBytes()))
      .put("embeddingMethod", "BASE64")
      .put("contentSyntax", "text/turtle")
      .put("validationType", accessLevel)
      .put("reportSyntax", APPLICATION_JSON);
  }

  /**
   * Creates a JSON response object in the same format as the regular ITB reports.
   *
   * @param timestamp The timestamp of the error occurrence, formatted as a string.
   * @param message   The error message to include in the response.
   * @return The JSON response object.
   */
  private JsonObject createResponse(String timestamp, String message) {
    return new JsonObject()
      .put("date", timestamp)
      .put("result", "FAILURE")
      .put(
        "reports",
        new JsonObject()
          .put("error", new JsonArray().add(new JsonObject().put("description", message))
          )
      );
  }

  /**
   * Returns an error message based on the type of error that occurred while communicating with the
   * ITB validation service.
   *
   * @param cause The cause of the error.
   * @return The error message.
   */
  @NotNull
  private static String getItbFailedResponseMessage(Throwable cause) {
    String errorMessage;

    // Determine the error type
    if (cause instanceof java.util.concurrent.TimeoutException) {
      errorMessage = "Validation service timed out: " + cause.getMessage();
    } else if (cause instanceof java.net.ConnectException
      || cause instanceof java.net.UnknownHostException) {
      errorMessage = "Validation service unreachable: " + cause.getMessage();
    } else if (cause != null) {
      errorMessage =
        "Unexpected error while communicating with validation service: " + cause.getMessage();
    } else {
      errorMessage = "Unknown error while communicating with validation service";
    }
    return errorMessage;
  }
}
