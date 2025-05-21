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

import eu.europa.data.health.access.SchemaFiles;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractSchemaValidator class implementing all methods that can be used from the different Validation Managers
 */
public class AbstractSchemaValidator {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String CLASS_NAME = "AbstractSchemaValidator - ";

  /**
   * A class that retrieves any file from the resources folder
   * @param fileName The name of the file to be retrieved
   * @return The content of the file to be retrieved as a JsonObject
   */
  public JsonObject getFileFromResourceAsStream(String fileName) {

    // The class loader that loaded the class
    ClassLoader classLoader = AbstractSchemaValidator.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    // the stream holding the file content
    if (inputStream == null) {
      log.error("{} inputStream is null", CLASS_NAME);
    } else {
      try {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return new JsonObject(text);
      } catch (IOException e) {
        log.error("{} Failed to retrieve file: {} with error message: {}", CLASS_NAME, fileName, e.getMessage());
      }
    }
    return null;
  }

  /**
   * A method that checks if the request body of a RoutingContext is null/empty or not
   * @param context @param context The routing context of the request
   * @return True: if the request body is null/empty. False if it contains any json value.
   */
  public boolean requestBodyIsEmpty(RoutingContext context){

    if (context.body() == null || context.body().isEmpty()) {
      context.response()
        .setStatusCode(400)
        .end("request body cannot be null/empty");
      return true;
    }
    return false;
  }

  public void validateJsonSchema(@NotNull RoutingContext context, String jsonSchema){

    // Validate that the RoutingContext does not contain a null/empty json body
    if(requestBodyIsEmpty(context))
      return;

    // Validate that the json body complies with the json schema of the item provided

    boolean isJsonArray = context.body().asString().trim().startsWith("[");

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri(
      SchemaFiles.CLASSPATH_SCHEMA_FILE_PATH.getValue()));
    JsonSchema schema = JsonSchema.of(getFileFromResourceAsStream(
      SchemaFiles.SCHEMA_FILE_PATH.getValue() +jsonSchema));
    if(schema == null){
      log.error("{} json schema was not retrieved", CLASS_NAME);
      context.response()
        .setStatusCode(400)
        .end("json schema was not retrieved");
    }
    // Perform the validation
    OutputUnit result = repository.validator(schema).validate(isJsonArray? context.body().asJsonArray() : context.body().asJsonObject());
    if (Boolean.TRUE.equals(result.getValid())) {
      // Successful validation, continue to the next handler
      log.debug("{} Validation was successful", CLASS_NAME);
      context.next();
    } else {
      // Validation failed
      log.error("{} Validation was not successful, with error message: {}", CLASS_NAME, result.getError());
      context.response()
        .setStatusCode(400)
        .end("Json schema validation for item failed");
    }
  }

  protected void setUpMessageContextResponse(RoutingContext context, HttpResponseStatus status,
    String message) {
    context.response().setStatusCode(status.code()).end(new JsonObject().put("result_message", message).encode());
  }
  protected void setUpMessageContextResponseWithContent(RoutingContext context, HttpResponseStatus status,
    String message, JsonArray content) {
    context.response().setStatusCode(status.code()).end(new JsonObject().put("result_message", message).put("errors", content).encode());
  }
}
