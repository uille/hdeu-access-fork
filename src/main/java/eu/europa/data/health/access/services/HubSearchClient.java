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

import eu.europa.data.health.access.Constants;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The HubSearchClient class provides methods to interact with a hub search API for get datasets.
 */
public class HubSearchClient {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final WebClient client;
  private  final JsonObject config;


  public HubSearchClient(WebClient client, JsonObject config) {
    this.client = client;
    this.config = config;
  }

  /**
   * Get the dataset by id
   * @param datasetId the id of the dataset
   * @return the document payload
   */
  public Future<JsonObject> getDataset(String datasetId) {

    if(log.isInfoEnabled()){
      log.info("Get dataset from hub search: datasetId={}, url={}", datasetId,
        config.getString(Constants.HUB_SEARCH_DATA_URL) + "/datasets/" + datasetId);
    }

    return client.getAbs(config.getString(Constants.HUB_SEARCH_DATA_URL) + "/datasets/" + datasetId)
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Dataset downloaded successfully: datasetId={}", datasetId);
          return Future.succeededFuture(response.bodyAsJsonObject());
        }
        else if (response.statusCode() == 404) {
          log.warn("Dataset not found: datasetId={}", datasetId);
          return Future.succeededFuture(new JsonObject().put("external_dataset_id", datasetId));
        } else {
          log.error("Failed to get dataset: datasetId={}, status={}, body={}", datasetId,
            response.statusCode(), response.bodyAsString());
          return Future.failedFuture("Failed to get dataset: " + response.statusMessage());
        }
      });
  }


  public Future<JsonObject> getAllCatalogues() {

    if(log.isInfoEnabled()){
      log.info("Get all catalogues from hub search: url={}",
        config.getString(Constants.HUB_SEARCH_DATA_URL));
    }

    return client.getAbs(config.getString(Constants.HUB_SEARCH_DATA_URL) + "/search")
      .addQueryParam("filter", "catalogue")
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Catalogues retrieved successfully");
          return Future.succeededFuture(response.bodyAsJsonObject());
        }
        else {
          log.error("Failed to get catalogues, status={}, body={}",
            response.statusCode(), response.bodyAsString());
          return Future.failedFuture("Failed to get catalogues: " + response.statusMessage());
        }
      });
  }

  public Future<JsonObject> getCatalogue(String catalogueId) {
    String url = config.getString(Constants.HUB_SEARCH_DATA_URL) + "/catalogues/" + catalogueId;

    if (log.isInfoEnabled()) {
      log.info("Get catalogue from hub search: catalogueId={}, url={}", catalogueId, url);
    }

    return client.getAbs(url)
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("Catalogue downloaded successfully: catalogueId={}", catalogueId);
          return Future.succeededFuture(response.bodyAsJsonObject());
        } else if (response.statusCode() == 404) {
          log.warn("Catalogue not found: catalogueId={}", catalogueId);
          return Future.succeededFuture(new JsonObject().put("catalogue_id", catalogueId));
        } else {
          log.error("Failed to get catalogue: catalogueId={}, status={}, body={}", catalogueId,
            response.statusCode(), response.bodyAsString());
          return Future.failedFuture("Failed to get catalogue: " + response.statusMessage());
        }
      });
  }

}
