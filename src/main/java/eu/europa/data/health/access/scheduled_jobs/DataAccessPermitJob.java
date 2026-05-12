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
package eu.europa.data.health.access.scheduled_jobs;

import static eu.europa.data.health.access.CatalogStatus.DECISION_ISSUED;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;

import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.PermitStatus;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataAccessPermitJob {

  private final MongoClient mongoClient;
  private final DataAccessApplicationManager dataAccessApplicationManager;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public DataAccessPermitJob(MongoClient mongoClient,
    DataAccessApplicationManager dataAccessApplicationManager) {
    this.mongoClient = mongoClient;
    this.dataAccessApplicationManager = dataAccessApplicationManager;
  }

  public void expireOldPermits(List<String> permitStatusesThatExpire) {

    log.info("Running job to expire old permits at: {}", LocalDateTime.now());

    JsonObject query = createQuery(permitStatusesThatExpire);

    mongoClient.find(DATA_ACCESS_APPLICATION_COLLECTION, query, ar -> {

      if (ar.failed()) {
        log.error("Failed to fetch applications", ar.cause());
      } else {
        log.info("Found {} applications to process with at least one expired permit", ar.result().size());

        for (JsonObject application : ar.result()) {
          processApplication(application, permitStatusesThatExpire);
        }
      }
    });
  }

  private void processApplication(JsonObject application, List<String> permitStatusesThatExpire) {

    JsonArray datasets = application.getJsonArray("datasets", new JsonArray());
    JsonArray datasetCatalogPairs = new JsonArray();

    for (Object datasetObj : datasets) {
      JsonObject dataset = (JsonObject) datasetObj;
      JsonArray permits = dataset.getJsonArray("permits", new JsonArray());

      for (Object permitObj : permits) {
        JsonObject permit = (JsonObject) permitObj;

        Instant permitExpirationDate = Instant.parse(permit.getString("permit_expiration_date"));
        String status = permit.getString("permit_status", "");
        int latest = permit.getInteger("latest", 0);

        if (permitExpirationDate.isBefore(Instant.now())
          && (permitStatusesThatExpire.stream().anyMatch(s -> s.equalsIgnoreCase(status)))
          && latest == 1) {

          datasetCatalogPairs.add(new JsonObject()
            .put("dataset_id", dataset.getString("dataset_id"))
            .put("catalog_id", dataset.getString("catalog_id")));
        }
      }
    }

    if (datasetCatalogPairs.isEmpty()) {
      log.info("No permits to expire for application {}", application.getString("_id"));
      return;
    }

    log.info("Processing application ID: {} with {} dataset/catalog pairs",
      application.getString("_id"), datasetCatalogPairs.size());

    for (Object obj : datasetCatalogPairs) {
      JsonObject pair = (JsonObject) obj;
      String catalogId = pair.getString("catalog_id");
      String datasetId = pair.getString("dataset_id");

      log.info("Updating dataset_id: {}, catalog_id: {} for application {}",
        datasetId, catalogId, application.getString("_id"));

      dataAccessApplicationManager.updatePermitStatus(application, catalogId, datasetId,
        PermitStatus.EXPIRED, null, null,null,null, null, Instant.now().toString(), null, null, "Expiration limit"
          + " reached",null, null);
    }

    dataAccessApplicationManager.replaceApplication(application, DATA_ACCESS_APPLICATION_COLLECTION)
      .onSuccess(replaceResult -> postUpdateActions(application, datasetCatalogPairs))
      .onFailure(fail -> log.error("Failed to replace application: {} ", fail.getMessage(), fail.getCause()));
  }

  private void postUpdateActions(JsonObject application, JsonArray datasetCatalogPairs) {

    for (Object obj : datasetCatalogPairs) {
      JsonObject pair = (JsonObject) obj;
      String catalogId = pair.getString("catalog_id");
      String datasetId = pair.getString("dataset_id");

      dataAccessApplicationManager.sendApplicationToOutgoingApplicationActivities(
        DATA_ACCESS_APPLICATION_COLLECTION,
        application.getString("_id"),
        DECISION_ISSUED,
        catalogId,
        PermitStatus.EXPIRED.getLabel(),
        datasetId,
        "Expiration limit reached",
        null,null,null,null,null,
        OperationStatus.PERMIT, null);

      String auditLogActionDescription =
        dataAccessApplicationManager.createAuditLogActionDescriptionForChangePermitStatus(
          application.getString("_id"),
          application.getString("title"),
          PermitStatus.EXPIRED,
          catalogId,
          datasetId
        );

      dataAccessApplicationManager.insertAuditLogForPermitUpdate(
        auditLogActionDescription, application.getString("_id"), null
      );
    }
  }

  private JsonObject createQuery(List<String> permitStatusesThatExpire){

    Instant time = Instant.now();

    log.info("Will expire permits: {}", permitStatusesThatExpire.toString());

    return new JsonObject()
      .put("datasets.permits", new JsonObject().put("$elemMatch", new JsonObject()
        .put("permit_expiration_date", new JsonObject().put("$lt", time.toString()))
        .put("permit_status", new JsonObject().put("$in", permitStatusesThatExpire))
        .put("latest", 1)
      ));
  }
}
