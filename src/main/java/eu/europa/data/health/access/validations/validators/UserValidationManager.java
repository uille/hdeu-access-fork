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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.dto.UserProfileDTO;
import eu.europa.data.health.access.exceptions.UserProfileError;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class UserValidationManager extends AbstractSchemaValidator{

  public void validateCreateOrUpdateUser(RoutingContext context) {

    JsonObject requestBody = context.body().asJsonObject();

    UserProfileDTO userProfile = generateUserProfileDTO(requestBody, context);

    Set<ConstraintViolation<UserProfileDTO>> violations = validateUserProfile(userProfile);

    if (!violations.isEmpty()) {
      List<UserProfileError> errors = new ArrayList<>();
      for (ConstraintViolation<UserProfileDTO> violation : violations) {
        String code = violation.getConstraintDescriptor().getAnnotation()
          .annotationType().getSimpleName();
        String field = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object maxParam = violation.getConstraintDescriptor().getAttributes().get("max");
        List<String> params = new ArrayList<>();
        if (maxParam != null) {
          params.add(maxParam.toString());
        }
        UserProfileError error = new UserProfileError(code, message, field, params);
        errors.add(error);
      }

      JsonObject response = new JsonObject()
        .put("errors", errors);


    context.response()
      .setStatusCode(400)
      .end(response.encode());
    } else {
      context.next();
    }
  }

  private Set<ConstraintViolation<UserProfileDTO>> validateUserProfile(UserProfileDTO userProfile) {

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    return validator.validate(userProfile);
  }

  private UserProfileDTO generateUserProfileDTO(JsonObject jsonUser, RoutingContext context) {

    UserProfileDTO userProfileDTO = null;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      userProfileDTO = objectMapper.readValue(jsonUser.toString(), UserProfileDTO.class);
    } catch (JsonProcessingException e) {
      context.response().setStatusCode(500).end(new JsonObject().put("result_message",
        "The parsing of json failed with error: " + e.getMessage()).encode());
    }
    return userProfileDTO;
  }

  public void validateCreateOrUpdateUserJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.USER_SCHEMA_FILE.getValue());
  }

}



