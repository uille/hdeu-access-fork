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

import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;

import eu.europa.data.health.access.SchemaFiles;
import eu.europa.data.health.access.services.HubSearchClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.jetbrains.annotations.NotNull;

/**
 * BasketValidationManager class implementing all validations for the Basket entity
 */
public class BasketValidationManager extends AbstractSchemaValidator {

  private final WebClient client;
  private final JsonObject config;

  public BasketValidationManager(
    WebClient client, JsonObject config) {
    this.client = client;
    this.config = config;
  }
  /**
   * A method validating that the json object inside the request body complies to the basket json
   * schema
   *
   * @param context The routing context of the request
   */
  public void validateBasketJsonSchema(@NotNull RoutingContext context) {
    validateJsonSchema(context, SchemaFiles.BASKET_SCHEMA_FILE.getValue());
  }

  public void validateDatasetAndDistributionExist(@NotNull RoutingContext context){

    String datasetId = context.body().asJsonObject().getString(DATASET_ID);
    String distributionId = context.body().asJsonObject().getString(DISTRIBUTION_ID);

    HubSearchClient hubSearchClient = new HubSearchClient(client, config);
    hubSearchClient.getDataset(datasetId)
      .onComplete(result -> {
        if (result.succeeded()) {
          JsonObject response = result.result();
          if(response.containsKey("result")){
            JsonArray distributions = response.getJsonObject("result").getJsonArray("distributions");
            boolean foundDistributionId = distributions.stream()
              .filter(distribution -> distribution instanceof JsonObject)
              .map(distribution -> (JsonObject) distribution)
              .anyMatch(distributionJson -> distributionJson.getString("id").equals(distributionId));
            if(foundDistributionId){
              context.next();
            } else {
              setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, String.format("Distribution id: %s not found", distributionId));
            }
          }else {
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST, String.format("Dataset id: %s not found", datasetId));
          }

        } else {
          setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, result.cause().getMessage());
        }
      });
  }
}
