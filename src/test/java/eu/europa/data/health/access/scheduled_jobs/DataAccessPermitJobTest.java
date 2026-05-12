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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.PermitStatus;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessPermitJobTest {

  @Mock
  private MongoClient mongoClient;

  @Mock
  private DataAccessApplicationManager dataAccessApplicationManager;

  @InjectMocks
  private DataAccessPermitJob dataAccessPermitJob;

  @Test
  void expireOldPermits_shouldProcessOnlyExpiredLatestPermitsAndTriggerPostActions() {
    JsonObject application = applicationWithDatasets(
      datasetWithPermits(
        "catalog-1",
        "dataset-1",
        permit("granted", 1, -3),
        permit("GRANTED", 1, 3),
        permit("GRANTED", 0, -5)
      )
    );

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture(List.of(application)));
      return null;
    })
      .when(mongoClient)
      .find(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any());

    when(
      dataAccessApplicationManager.replaceApplication(
        application,
        DATA_ACCESS_APPLICATION_COLLECTION
      )
    ).thenReturn(Future.succeededFuture(application));
    when(
      dataAccessApplicationManager.createAuditLogActionDescriptionForChangePermitStatus(
        "app-1",
        "Application Title",
        PermitStatus.EXPIRED,
        "catalog-1",
        "dataset-1"
      )
    ).thenReturn("permit expired");

    dataAccessPermitJob.expireOldPermits(List.of("GRANTED"));

    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).find(eq(DATA_ACCESS_APPLICATION_COLLECTION), queryCaptor.capture(), any());

    JsonObject query = queryCaptor.getValue();
    JsonObject elemMatch = query.getJsonObject("datasets.permits").getJsonObject("$elemMatch");
    assertEquals(
      List.of("GRANTED"),
      elemMatch.getJsonObject("permit_status").getJsonArray("$in").getList()
    );
    assertEquals(1, elemMatch.getInteger("latest"));
    assertTrue(elemMatch.getJsonObject("permit_expiration_date").containsKey("$lt"));

    verify(dataAccessApplicationManager).updatePermitStatus(
      eq(application),
      eq("catalog-1"),
      eq("dataset-1"),
      eq(PermitStatus.EXPIRED),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      anyString(),
      isNull(),
      isNull(),
      eq("Expiration limit reached"),
      isNull(),
      isNull()
    );
    verify(dataAccessApplicationManager).replaceApplication(
      application,
      DATA_ACCESS_APPLICATION_COLLECTION
    );
    verify(dataAccessApplicationManager).sendApplicationToOutgoingApplicationActivities(
      eq(DATA_ACCESS_APPLICATION_COLLECTION),
      eq("app-1"),
      eq(DECISION_ISSUED),
      eq("catalog-1"),
      eq(PermitStatus.EXPIRED.getLabel()),
      eq("dataset-1"),
      eq("Expiration limit reached"),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      eq(OperationStatus.PERMIT),
      isNull()
    );
    verify(dataAccessApplicationManager).createAuditLogActionDescriptionForChangePermitStatus(
      "app-1",
      "Application Title",
      PermitStatus.EXPIRED,
      "catalog-1",
      "dataset-1"
    );
    verify(dataAccessApplicationManager).insertAuditLogForPermitUpdate(
      "permit expired",
      "app-1",
      null
    );
  }

  @Test
  void expireOldPermits_shouldSkipApplicationsWithoutMatchingPermits() {
    JsonObject application = applicationWithDatasets(
      datasetWithPermits(
        "catalog-1",
        "dataset-1",
        permit("GRANTED", 1, 5),
        permit("GRANTED", 0, -2),
        permit("REVOKED", 1, -2)
      )
    );

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture(List.of(application)));
      return null;
    })
      .when(mongoClient)
      .find(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any());

    dataAccessPermitJob.expireOldPermits(List.of("GRANTED"));

    verify(dataAccessApplicationManager, never()).updatePermitStatus(
      any(),
      anyString(),
      anyString(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      anyString(),
      any(),
      any(),
      anyString(),
      any(),
      any()
    );
    verify(dataAccessApplicationManager, never()).replaceApplication(
      any(JsonObject.class),
      anyString()
    );
    verify(dataAccessApplicationManager, never()).sendApplicationToOutgoingApplicationActivities(
      anyString(),
      anyString(),
      any(),
      anyString(),
      anyString(),
      anyString(),
      anyString(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
    verify(dataAccessApplicationManager, never()).insertAuditLogForPermitUpdate(
      anyString(),
      anyString(),
      any()
    );
  }

  @Test
  void expireOldPermits_shouldStopWhenFindFails() {
    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture("Database unavailable"));
      return null;
    })
      .when(mongoClient)
      .find(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any());

    dataAccessPermitJob.expireOldPermits(List.of("GRANTED"));

    verify(dataAccessApplicationManager, never()).updatePermitStatus(
      any(),
      anyString(),
      anyString(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      anyString(),
      any(),
      any(),
      anyString(),
      any(),
      any()
    );
    verify(dataAccessApplicationManager, never()).replaceApplication(
      any(JsonObject.class),
      anyString()
    );
    verify(dataAccessApplicationManager, never()).sendApplicationToOutgoingApplicationActivities(
      anyString(),
      anyString(),
      any(),
      anyString(),
      anyString(),
      anyString(),
      anyString(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
    verify(dataAccessApplicationManager, never()).insertAuditLogForPermitUpdate(
      anyString(),
      anyString(),
      any()
    );
  }

  @Test
  void expireOldPermits_shouldNotRunPostActionsWhenReplaceFails() {
    JsonObject application = applicationWithDatasets(
      datasetWithPermits("catalog-1", "dataset-1", permit("GRANTED", 1, -1))
    );

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture(List.of(application)));
      return null;
    })
      .when(mongoClient)
      .find(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any());

    when(
      dataAccessApplicationManager.replaceApplication(
        application,
        DATA_ACCESS_APPLICATION_COLLECTION
      )
    ).thenReturn(Future.failedFuture("replace failed"));

    dataAccessPermitJob.expireOldPermits(List.of("GRANTED"));

    verify(dataAccessApplicationManager, times(1)).updatePermitStatus(
      eq(application),
      eq("catalog-1"),
      eq("dataset-1"),
      eq(PermitStatus.EXPIRED),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      anyString(),
      isNull(),
      isNull(),
      eq("Expiration limit reached"),
      isNull(),
      isNull()
    );
    verify(dataAccessApplicationManager).replaceApplication(
      application,
      DATA_ACCESS_APPLICATION_COLLECTION
    );
    verify(dataAccessApplicationManager, never()).sendApplicationToOutgoingApplicationActivities(
      anyString(),
      anyString(),
      any(),
      anyString(),
      anyString(),
      anyString(),
      anyString(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
    verify(
      dataAccessApplicationManager,
      never()
    ).createAuditLogActionDescriptionForChangePermitStatus(
      anyString(),
      anyString(),
      any(),
      anyString(),
      anyString()
    );
    verify(dataAccessApplicationManager, never()).insertAuditLogForPermitUpdate(
      anyString(),
      anyString(),
      any()
    );
  }

  private JsonObject applicationWithDatasets(JsonObject... datasets) {
    JsonArray datasetArray = new JsonArray();
    for (JsonObject dataset : datasets) {
      datasetArray.add(dataset);
    }

    return new JsonObject()
      .put("_id", "app-1")
      .put("title", "Application Title")
      .put("datasets", datasetArray);
  }

  private JsonObject datasetWithPermits(String catalogId, String datasetId, JsonObject... permits) {
    JsonArray permitArray = new JsonArray();
    for (JsonObject permit : permits) {
      permitArray.add(permit);
    }

    return new JsonObject()
      .put("catalog_id", catalogId)
      .put("dataset_id", datasetId)
      .put("permits", permitArray);
  }

  private JsonObject permit(String status, int latest, int expiresInDays) {
    return new JsonObject()
      .put("permit_status", status)
      .put("latest", latest)
      .put("permit_expiration_date", Instant.now().plus(Duration.ofDays(expiresInDays)).toString());
  }
}
