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

import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES1;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES;
import static eu.europa.data.health.access.Constants.FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_2;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_3;
import static eu.europa.data.health.access.Constants.HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_EMAIL;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_NAME;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A;
import static eu.europa.data.health.access.Constants.IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA;
import static eu.europa.data.health.access.Constants.LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.OTHER_PERMIT_APPLICATIONS;
import static eu.europa.data.health.access.Constants.PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS;
import static eu.europa.data.health.access.Constants.PURPOSE_FOR_WHICH_DATA_WILL_BE_USED;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_7;
import static eu.europa.data.health.access.Constants.SECTION_8;
import static eu.europa.data.health.access.Constants.SPECIFY_OTHER;
import static eu.europa.data.health.access.Constants.SPECIFY_OTHER_RELATIVES;
import static eu.europa.data.health.access.Constants.WHEN_DATA_NEEDED;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT;
import static eu.europa.data.health.access.Constants.WILL_CONTROLS_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_RELATIVES_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.WILL_THE_DATA_BE_TRANSFERRED;
import static eu.europa.data.health.access.UtilityServiceTest.getStringJsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.Sections;
import eu.europa.data.health.access.dto.SectionDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.data_access.Section7DTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.validations.business_validators.DataAccessApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class DataAccessApplicationManagerTest {

  @Mock
  MongoClient mongoClient;
  @Mock
  DBHandler dbHandler;
  @Mock
  BasketManager basketManager;
  @Mock
  HubStoreClient hubStoreClient;
  @Mock
  Vertx vertx;
  @Mock
  AuditLogManager auditLogManager;
  @Mock
  DataAccessApplicationValidationManager dataAccessApplicationValidationManager;
  @Mock
  DataAccessApplicationBusinessValidationManager dataAccessApplicationBusinessValidationManager;
  @Mock
  RoutingContext routingContext;
  @Mock
  private HttpServerResponse response;
  @Mock
  private HttpServerRequest request;
  Logger logger = (Logger) LoggerFactory.getLogger(DataAccessApplicationManager.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  @InjectMocks
  DataAccessApplicationManager dataAccessApplicationManager;

  @BeforeEach
  void setUp() {
    dataAccessApplicationManager = new DataAccessApplicationManager(
      dbHandler, mongoClient, basketManager, hubStoreClient,
      vertx, dataAccessApplicationValidationManager, dataAccessApplicationBusinessValidationManager,
      auditLogManager
    );

    when(routingContext.request()).thenReturn(request);
    // Mock other behaviors
    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
  }

  private void addUserOnContext() {
    User mockUser = Mockito.mock(User.class);
    String ecasId = "n3817490";
    lenient().when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    lenient().when(routingContext.user().principal()).thenReturn(principal);
  }

  @Test
  void testDeleteDatasets_datasetDeletedWhenStatusNotSubmitted() {
    addUserOnContext();
    String appId = "app1";
    String datasetId = "ds1";

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(
      new JsonArray().add(new JsonObject().put("datasetId", datasetId)));

    JsonObject dataset = new JsonObject().put("dataset_id", datasetId)
      .put("distributions", new JsonArray());
    JsonObject application = new JsonObject()
      .put("id", appId)
      .put("title", "Test Title")
      .put("datasets", new JsonArray().add(dataset))
      .put("status", new JsonObject().put("status", "DRAFT"));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(application));
        return null;
      });

    dataAccessApplicationManager.deleteDatasets(routingContext);

    verify(response).setStatusCode(200);
    verify(routingContext).response();
  }

  @Test
  void testDeleteDatasets_blockedWhenStatusSubmitted() {
    addUserOnContext();
    String appId = "app2";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);

    JsonObject appWithSubmittedStatus = new JsonObject()
      .put("id", appId)
      .put("status", new JsonObject().put("status", "SUBMITTED"));

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(appWithSubmittedStatus));
        return null;
      });

    dataAccessApplicationManager.deleteDatasets(routingContext);

    verify(dbHandler, never()).deleteById(any(), any());
    verify(auditLogManager, never()).insertAuditLog(any(), any(), any(), any(), anyBoolean(),
      any());
  }

  @Test
  void testDeleteDatasets_appNotFound() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("not_found_id");

    when(mongoClient.findOne(any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(null));
        return null;
      });

    dataAccessApplicationManager.deleteDatasets(routingContext);

    verify(dbHandler, never()).deleteById(any(), any());
  }


  @Test
  void testDeleteDatasets_mongoError() {
    addUserOnContext();
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app3");

    when(mongoClient.findOne(any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.failedFuture("Database error"));
        return null;
      });

    dataAccessApplicationManager.deleteDatasets(routingContext);

    verify(dbHandler, never()).deleteById(any(), any());
  }

  @Test
  void deleteApplicationDataset_singleDatasetMatch_shouldDeleteApplicationAndLog() {
    String datasetId = "ds1";
    String appId = "app123";

    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add("dist1"));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.extractDistributionIds(anyString())).thenReturn("dist1");
    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    boolean result = dataAccessApplicationManager.deleteApplicationDataset(mongoApp, datasetId,
      routingContext);

    assertFalse(result);
    verify(dbHandler).deleteById(eq(DATA_ACCESS_APPLICATION_COLLECTION), any());
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"),
      eq(false), eq(routingContext));
  }

  @Test
  void deleteApplicationDataset_multipleDatasetsMatch_shouldRemoveDatasetAndLog() {
    String datasetId = "ds2";
    String appId = "app456";

    JsonObject dataset1 = new JsonObject()
      .put("dataset_id", "ds1")
      .put("catalog_id", "cat1")
      .put("distributions", new JsonArray().add("dist1"));

    JsonObject dataset2 = new JsonObject()
      .put("dataset_id", datasetId)
      .put("catalog_id", "cat2")
      .put("distributions", new JsonArray().add("dist2"));

    JsonArray datasets = new JsonArray().add(dataset1).add(dataset2);

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App Multiple")
      .put("datasets", datasets);

    when(auditLogManager.extractDistributionIds(anyString())).thenReturn("dist2");
    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Dataset Deleted");

    boolean result = dataAccessApplicationManager.deleteApplicationDataset(mongoApp, datasetId,
      routingContext);

    assertTrue(result);
    assertEquals(1, mongoApp.getJsonArray("datasets").size());
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Dataset Deleted"), eq(true),
      eq(routingContext));
  }

  @Test
  void deleteApplicationDataset_datasetNotFound_shouldReturnFalse() {
    JsonObject dataset = new JsonObject()
      .put("dataset_id", "not_target");

    JsonObject mongoApp = new JsonObject()
      .put("id", "app789")
      .put("datasets", new JsonArray().add(dataset));

    boolean result = dataAccessApplicationManager.deleteApplicationDataset(mongoApp, "missing_ds",
      routingContext);

    assertFalse(result);
    verifyNoInteractions(dbHandler, auditLogManager);
  }

  @Test
  void testDeleteDistributions_successfulDeletion() {
    String appId = "app-123";
    when(routingContext.pathParam("application_id")).thenReturn(appId);
    when(routingContext.body()).thenReturn(mock(RequestBody.class));

    JsonArray bodyArray = new JsonArray().add(
      new JsonObject()
        .put("dataset_id", "ds1")
        .put("distribution_id", "dist1")
    );

    when(routingContext.body().asJsonArray()).thenReturn(bodyArray);

    addUserOnContext();
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("datasets", new JsonArray());

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.deleteDistributions(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(200);
      verify(response).end(contains("Delete operation/s completed"));
    });
  }

  @Test
  void testDeleteDistributions_submittedStatus() {
    when(routingContext.pathParam("application_id")).thenReturn("app-1");
    addUserOnContext();
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.deleteDistributions(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(400);
      verify(response).end(contains("Deletion can not be executed"));
    });
  }

  @Test
  void testDeleteDistributions_applicationNotFound() {
    when(routingContext.pathParam("application_id")).thenReturn("missing-id");
    addUserOnContext();
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.deleteDistributions(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(404);
      verify(response).end(contains("was not found in the database"));
    });
  }

  @Test
  void testDeleteDistributions_mongoFailure() {
    when(routingContext.pathParam("application_id")).thenReturn("app-error");
    addUserOnContext();

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.deleteDistributions(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("DB down"));
    });
  }

  @Test
  void deleteApplicationDatasetDistribution_onlyDistributionInOnlyDataset_shouldDeleteApplication() {
    String appId = "app123";
    String datasetId = "ds1";
    String distributionId = "dist1";

    JsonObject distribution = new JsonObject().put("distribution_id", distributionId);
    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add(distribution));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    dataAccessApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetId,
      distributionId, routingContext);

    verify(dbHandler).deleteById(eq(DATA_ACCESS_APPLICATION_COLLECTION), any());
    verify(dbHandler).updateLinkableFields(eq(AUDIT_LOG_COLLECTION), eq(appId));
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"),
      eq(false), eq(routingContext));
  }

  @Test
  void deleteApplicationDatasetDistribution_onlyDistributionInOneDataset_shouldRemoveDataset() {
    String appId = "app123";
    String datasetIdToDelete = "ds1";
    String otherDatasetId = "ds2";
    String distributionId = "dist1";

    JsonObject distribution = new JsonObject().put("distribution_id", distributionId);
    JsonObject datasetToDelete = new JsonObject()
      .put("dataset_id", datasetIdToDelete)
      .put("catalog_id", "cat1")
      .put("distributions", new JsonArray().add(distribution));

    JsonObject otherDataset = new JsonObject()
      .put("dataset_id", otherDatasetId)
      .put("distributions", new JsonArray().add(new JsonObject().put("distribution_id", "dist2")));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(datasetToDelete).add(otherDataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    dataAccessApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetIdToDelete,
      distributionId, routingContext);

    assertEquals(1, mongoApp.getJsonArray("datasets").size()); // dataset removed
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"), eq(true),
      eq(routingContext));
  }

  @Test
  void deleteApplicationDatasetDistribution_multipleDistributions_shouldRemoveOnlyTargetDistribution() {
    String appId = "app123";
    String datasetId = "ds1";
    String distributionIdToRemove = "dist1";

    JsonObject distribution1 = new JsonObject().put("distribution_id", distributionIdToRemove);
    JsonObject distribution2 = new JsonObject().put("distribution_id", "dist2");

    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add(distribution1).add(distribution2));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    dataAccessApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetId,
      distributionIdToRemove, routingContext);

    JsonArray updatedDistributions = mongoApp.getJsonArray("datasets").getJsonObject(0)
      .getJsonArray("distributions");
    assertEquals(1, updatedDistributions.size());
    assertEquals("dist2", updatedDistributions.getJsonObject(0).getString("distribution_id"));
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"), eq(true),
      eq(routingContext));
  }

  @Test
  void testCreateOrUpdateSection() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).createOrUpdateSection(any(), anyString());
    spyManager.createOrUpdateSection(routingContext);
    verify(spyManager).createOrUpdateSection(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testGetSection_successful() {
    when(routingContext.pathParam("application_id")).thenReturn("app123");
    when(routingContext.pathParam("section_number")).thenReturn("1");
    addUserOnContext();

    JsonObject sectionData = new JsonObject().put("fieldA", "valueA");
    JsonObject form = new JsonObject().put("section1", sectionData);
    JsonObject mongoDocument = new JsonObject().put("form", form);

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(List.of(mongoDocument)));
      return null;
    }).when(mongoClient).findWithOptions(any(), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.getSection(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(200);
      verify(response).end(contains("fieldA"));
    });
  }

  @Test
  void testGetSection_notFound() {
    when(routingContext.pathParam("application_id")).thenReturn("app123");
    when(routingContext.pathParam("section_number")).thenReturn("1");
    addUserOnContext();

    JsonObject mongoDocument = new JsonObject();

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(List.of(mongoDocument)));
      return null;
    }).when(mongoClient).findWithOptions(any(), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.getSection(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(404);
      verify(response).end(contains("No information found for provided input"));
    });
  }

  @Test
  void testGetSection_mongoFailure() {
    when(routingContext.pathParam("application_id")).thenReturn("app123");
    when(routingContext.pathParam("section_number")).thenReturn("1");
    addUserOnContext();
    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findWithOptions(any(), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataAccessApplicationManager.getSection(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("DB down"));
    });
  }


  @Test
  void testGetApplicationById() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).findById(any(), anyString());
    spyManager.getApplicationById(routingContext);
    verify(spyManager).findById(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testCreateApplication() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(201).end("mocked");
      return null;
    }).when(spyManager).createApplication(any(), anyString());
    spyManager.createApplication(routingContext);
    verify(spyManager).createApplication(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(201);
  }

  @Test
  void testGetAllDataAccessApplicationsDelegatesToGetEntityWithCorrectParams() {
    String page = "2";
    String limit = "10";
    String sortBy = "name";
    String sortOrder = "asc";
    String status = "PENDING";

    when(request.getParam("page")).thenReturn(page);
    when(request.getParam("limit")).thenReturn(limit);
    when(request.getParam("sortBy")).thenReturn(sortBy);
    when(request.getParam("sortOrder")).thenReturn(sortOrder);
    when(request.getParam("status")).thenReturn(status);

    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).getEntity(routingContext, DATA_ACCESS_APPLICATION_COLLECTION, 2, 10, sortBy
      , sortOrder, status);

    spyManager.getAllDataAccessApplications(routingContext);

    verify(response).setStatusCode(200);
    verify(spyManager).getEntity(eq(routingContext), eq(DATA_ACCESS_APPLICATION_COLLECTION), eq(2),
      eq(10),
      eq("name"), eq("asc"), eq("PENDING"));
  }

  @Test
  void testUpdateApplication() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).updateEntity(any(), anyString());
    spyManager.updateApplication(routingContext);
    verify(spyManager).updateEntity(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testPatchApplication() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).patchEntity(any(), anyString());
    spyManager.patchApplication(routingContext);
    verify(spyManager).patchEntity(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteDataAccessApplicationById() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).deleteApplicationById(any(), any());
    spyManager.deleteDataAccessApplicationById(routingContext);
    verify(spyManager).deleteApplicationById(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteAllDataAccessApplication() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).deleteAllEntities(any(), any());
    spyManager.deleteAllDataAccessApplication(routingContext);
    verify(spyManager).deleteAllEntities(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testPutDistributionsApplication_success() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    JsonObject requestApplication = new JsonObject()
      .put("application_id", "app1");

    JsonObject requestDistribution = new JsonObject()
      .put("distribution_id", "dist1");

    JsonObject requestDataset = new JsonObject()
      .put("dataset_id", "ds1")
      .put("distributions", new JsonArray().add(requestDistribution));

    JsonObject requestBody = new JsonObject()
      .put("applications", new JsonArray().add(requestApplication))
      .put("datasets", new JsonArray().add(requestDataset));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    JsonObject mongoApplication = new JsonObject()
      .put("_id", "app1")
      .put("title", "Test Title")
      .put("version", "v1")
      .put("datasets", new JsonArray());

    addUserOnContext();

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(anyString(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    dataAccessApplicationManager.putDistributionsApplication(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event ->
          event.getLevel() == Level.INFO &&
            event.getFormattedMessage()
              .contains("All update application operations have been completed successfully"));
      assertTrue(found);
    });
  }

  @Test
  void testPutDistributionsApplication_applicationReturnedIsNull() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    JsonObject requestApplication = new JsonObject()
      .put("application_id", "app1");

    JsonObject requestDistribution = new JsonObject()
      .put("distribution_id", "dist1");

    JsonObject requestDataset = new JsonObject()
      .put("dataset_id", "ds1")
      .put("distributions", new JsonArray().add(requestDistribution));

    JsonObject requestBody = new JsonObject()
      .put("applications", new JsonArray().add(requestApplication))
      .put("datasets", new JsonArray().add(requestDataset));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    addUserOnContext();

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOne(anyString(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    dataAccessApplicationManager.putDistributionsApplication(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event -> event.getFormattedMessage().contains("Application returned was null"));
      assertTrue(found);
    });
  }

  @Test
  void testPutDistributionsApplication_someOperationsFailed() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);

    Future<Object> failedFuture = Future.failedFuture("Some failure");
    List<Future> futures = Collections.singletonList(failedFuture);
    doReturn(futures).when(spyManager).updateApplicationsAsync(any());

    spyManager.putDistributionsApplication(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event -> event.getFormattedMessage().contains("Some operations have failed"));
      assertTrue(found);
    });
  }

  @Test
  void testExtractField_objectNodeWithMatchingField() throws Exception {
    String json = "{\"id\": \"123\", \"name\": \"Alice\"}";
    JsonNode node = mapper.readTree(json);
    Set<String> ids = new HashSet<>();
    dataAccessApplicationManager.extractField(node, ids, "id");
    assertEquals(Set.of("123"), ids);
  }

  @Test
  void testExtractField_nestedObjectWithMatchingField() throws Exception {
    String json = "{\"user\": {\"id\": \"abc123\"}, \"meta\": {\"id\": \"xyz456\"}}";
    JsonNode node = mapper.readTree(json);
    Set<String> ids = new HashSet<>();
    dataAccessApplicationManager.extractField(node, ids, "id");
    assertEquals(Set.of("abc123", "xyz456"), ids);
  }

  @Test
  void testExtractField_arrayOfObjectsWithMatchingField() throws Exception {
    String json = "[{\"id\": \"1\"}, {\"id\": \"2\"}, {\"name\": \"no-id\"}]";
    JsonNode node = mapper.readTree(json);
    Set<String> ids = new HashSet<>();
    dataAccessApplicationManager.extractField(node, ids, "id");
    assertEquals(Set.of("1", "2"), ids);
  }

  @Test
  void testExtractField_mixedTypesWithMatchingField() throws Exception {
    String json = "{ \"items\": [ {\"id\": \"a\"}, {\"id\": \"b\"} ], \"extra\": {\"id\": \"c\"} }";
    JsonNode node = mapper.readTree(json);
    Set<String> ids = new HashSet<>();
    dataAccessApplicationManager.extractField(node, ids, "id");
    assertEquals(Set.of("a", "b", "c"), ids);
  }

  @Test
  void testExtractField_fieldValueIsArray() throws Exception {
    String json = "{ \"id\": [\"x1\", \"x2\"] }";
    JsonNode node = mapper.readTree(json);
    Set<String> ids = new HashSet<>();
    dataAccessApplicationManager.extractField(node, ids, "id");
    assertEquals(Set.of("x1", "x2"), ids);
  }

  @Test
  void testExtractField_noMatchingField() throws Exception {
    String json = "{\"name\": \"No match\"}";
    JsonNode node = mapper.readTree(json);
    Set<String> ids = new HashSet<>();
    dataAccessApplicationManager.extractField(node, ids, "id");
    assertTrue(ids.isEmpty());
  }


  @Test
  void testPostData() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).postData(any(), anyString());
    spyManager.postData(routingContext);
    verify(spyManager).postData(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testGetData() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).getData(any(), anyString());
    spyManager.getData(routingContext);
    verify(spyManager).getData(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteData() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).deleteData(any(), anyString());
    spyManager.deleteData(routingContext);
    verify(spyManager).deleteData(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testValidateFileBeforeUpdate() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).validateFileBeforeUpdate(any(), anyString());
    spyManager.validateFileBeforeUpdate(routingContext);
    verify(spyManager).validateFileBeforeUpdate(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testChangeTitle() {
    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).changeTitle(any(), anyString());
    spyManager.changeTitle(routingContext);
    verify(spyManager).changeTitle(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testChangeStatus_successfulFlow() {
    when(routingContext.pathParam("application_id")).thenReturn("app123");

    JsonObject bodyJson = new JsonObject().put("status", "SUBMITTED");
    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(bodyJson);
    addUserOnContext();

    JsonObject mongoApp = new JsonObject()
      .put("title", "Test App")
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataAccessApplicationManager spyManager = Mockito.spy(dataAccessApplicationManager);

    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());
    doReturn(Future.succeededFuture(mongoApp)).when(spyManager).replaceApplication(any());
    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).sendApplicationForDispatch(any(), any());
    doNothing().when(auditLogManager)
      .insertAuditLog(any(), any(), any(), any(), anyBoolean(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(any(), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    spyManager.changeStatus(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(200);
      verify(response).end(contains("Test App"));
    });
  }

  @Test
  void testCanChangeStatus_invalidStatus() {
    JsonObject requestStatus = new JsonObject().put("status", "INVALID");
    JsonObject mongoApp = new JsonObject().put("status", new JsonObject().put("status", "DRAFT"));

    boolean result = dataAccessApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
    verify(routingContext.response()).setStatusCode(HttpResponseStatus.FORBIDDEN.code());
  }

  @Test
  void testCanChangeStatus_disallowedFlow() {
    JsonObject requestStatus = new JsonObject().put("status", "SUBMITTED");
    JsonObject mongoApp = new JsonObject().put("status",
      new JsonObject().put("status", "SUBMITTED"));

    boolean result = dataAccessApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
    verify(routingContext.response()).setStatusCode(HttpResponseStatus.FORBIDDEN.code());
    verify(routingContext.response()).end(contains("wrong flow"));
  }

  @Test
  void testCanChangeStatus_dtoValidationFails() {
    JsonObject requestStatus = new JsonObject().put("status", "SUBMITTED");
    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject());

    boolean result = dataAccessApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
  }

  @Test
  void testCanChangeStatus_businessValidationFails() {
    JsonObject requestStatus = new JsonObject().put("status", "SUBMITTED");
    JsonObject form = new JsonObject();
    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", form);

    boolean result = dataAccessApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
  }

  @Test
  void testCanChangeStatus_success() {
    JsonObject requestStatus = new JsonObject().put("status", "SUBMITTED");

    JsonObject application = new JsonObject(getStringJsonObject("full_data_application"));
    JsonObject form = application.getJsonObject("form");
    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", form);

    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_2.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(new SectionDTO());
    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_3.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(new SectionDTO());
    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_4.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(new SectionDTO());
    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_5.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(new SectionDTO());
    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_6.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(new SectionDTO());
    Section7DTO mockDTO7 = new Section7DTO();
    mockDTO7.setWillDataBeCombinedWithDataObtained(new SelectObjectDTO("b", "value"));
    mockDTO7.setOtherPermitApplications(new SelectObjectDTO("b", "value"));
    mockDTO7.setProvideInformationList(null);
    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_7.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(mockDTO7);
    when(dataAccessApplicationValidationManager.generateSectionDTO(
      eq(Sections.SECTION_8.getSectionNumber()), anyString(), eq(routingContext)
    )).thenReturn(new SectionDTO());

    when(dataAccessApplicationBusinessValidationManager.section4Validation(any(), any()))
      .thenReturn(true);
    when(dataAccessApplicationBusinessValidationManager.section8Validation(any(), any()))
      .thenReturn(true);
    when(dataAccessApplicationBusinessValidationManager.validateCatalogs(any(), any()))
      .thenReturn(true);
    boolean result = dataAccessApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);
    assertTrue(result);
  }

  @Test
  void cleanSection5_whenKeyIsNotB_shouldRemoveFields() {
    JsonObject formatOfEhd = new JsonObject().put("key", "a");
    JsonObject section5 = new JsonObject()
      .put(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA, formatOfEhd)
      .put(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A, "valueA")
      .put(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B, "valueB")
      .put("whyNeedForPseudonymisation", "reason");

    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", new JsonObject().put(SECTION_5, section5));

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject updatedSection5 = applicationForm.getJsonObject("form").getJsonObject(SECTION_5);

    assertFalse(updatedSection5.containsKey(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A));
    assertFalse(updatedSection5.containsKey(IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B));
    assertFalse(updatedSection5.containsKey("whyNeedForPseudonymisation"));
    assertEquals("a",
      updatedSection5.getJsonObject(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA).getString("key"));
  }

  @Test
  void cleanSection6_shouldRemoveMostFieldsWhenConditionsNotMet() {
    JsonObject section = new JsonObject()
      .put("howIsTheStudyCohortFormed", new JsonObject().put("key", "a"))
      .put(HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS,
        new JsonObject().put("key", "b"))
      .put("doesTheInformedConsentCoverTheRequestedRegistryExtractions", "some value")
      .put("consentAndInformationLetterAttachment", "some value")
      .put("confirmThatDataPermitHasBeenGrantedForTheResearchProject", "some value")
      .put("permitDecisionAttachment", "some value")
      .put("previouslyIssuedPermitIssuer1", "issuer")
      .put("howTheStudyCohortWasObtained", "some value")
      .put("samplingMethod", "some value")
      .put("sampleSize", "some value")
      .put(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, "yes")
      .put(INFORMATION_PROVIDER_NAME, "name")
      .put(INFORMATION_PROVIDER_EMAIL, "email")
      .put(INFORMATION_PROVIDER_PHONE, "phone")
      .put("whyNeedDataOfaWholePopulation", "reason")
      .put("extractionMethod", new JsonObject().put("key", "b"))
      .put("howOftenDoesTheDataNeedToBeExtracted", new JsonObject().put("key", "a"))
      .put(DATA_EXTRACTION_FREQUENCY, new JsonObject().put("key", "b"))
      .put(NEED_FOR_DATA_EXTRACTION_EVERY, new JsonObject().put("key", "d"))
      .put("needForDataExtractionEveryOther", "value")
      .put(HAS_THE_GROUP_RELATIVES_FORMED_2, new JsonObject().put("key", "b"))
      .put(WILL_CONTROLS_BE_EXTRACTED, new JsonObject().put("key", "b"))
      .put(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS, new JsonObject().put("key", "a"))
      .put(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION, "value")
      .put(DATA_NEEDS_TO_BE_EXTRACTED_EVERY, new JsonObject().put("key", "d"))
      .put(SPECIFY_OTHER, "value")
      .put("moreInfoOnExtractionPeriods", "value")
      .put("orderOfControlsDataExtraction", "value")
      .put(HAS_THE_GROUP_RELATIVES_FORMED_3, new JsonObject().put("key", "b"))
      .put(WILL_RELATIVES_BE_EXTRACTED, new JsonObject().put("key", "b"))
      .put("willSameDataBeExtractedForRelatives", new JsonObject().put("key", "a"))
      .put("willDataForRelativesBeExtractedSimultaneously", new JsonObject().put("key", "a"))
      .put(DATA_EXTRACTION_FREQUENCY_RELATIVES, new JsonObject().put("key", "b"))
      .put(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES1, "value")
      .put(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES, new JsonObject().put("key", "d"))
      .put(SPECIFY_OTHER_RELATIVES, "value")
      .put(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS,
        new JsonObject().put("key", "a"))
      .put(HAS_THE_GROUP_RELATIVES_FORMED_2, new JsonObject().put("key", "a"));

    JsonArray section6Array = new JsonArray().add(section);
    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", new JsonObject().put(SECTION_6, section6Array));

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection =
      applicationForm.getJsonObject("form").getJsonArray(SECTION_6).getJsonObject(0);

    assertFalse(
      cleanedSection.containsKey("doesTheInformedConsentCoverTheRequestedRegistryExtractions"));
    assertFalse(cleanedSection.containsKey("consentAndInformationLetterAttachment"));
    assertFalse(
      cleanedSection.containsKey("confirmThatDataPermitHasBeenGrantedForTheResearchProject"));
    assertFalse(cleanedSection.containsKey("permitDecisionAttachment"));
    assertFalse(cleanedSection.containsKey("howTheStudyCohortWasObtained"));
    assertFalse(cleanedSection.containsKey("samplingMethod"));
    assertFalse(cleanedSection.containsKey("sampleSize"));
    assertFalse(cleanedSection.containsKey("whyNeedDataOfaWholePopulation"));
    assertFalse(cleanedSection.containsKey("needForDataExtractionEveryOther"));
    assertFalse(cleanedSection.containsKey(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION));
    assertFalse(cleanedSection.containsKey(SPECIFY_OTHER));
    assertFalse(cleanedSection.containsKey(SPECIFY_OTHER_RELATIVES));
  }

  @Test
  void cleanSection6_shouldRetainFieldsWhenConditionsMet() {
    JsonObject section = new JsonObject()
      .put("howIsTheStudyCohortFormed", new JsonObject().put("key", "b"))
      .put(HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS,
        new JsonObject().put("key", "a"))
      .put("doesTheInformedConsentCoverTheRequestedRegistryExtractions", "some value")
      .put("consentAndInformationLetterAttachment", "some value")
      .put("confirmThatDataPermitHasBeenGrantedForTheResearchProject", "some value")
      .put("permitDecisionAttachment", "some value")
      .put("howTheStudyCohortWasObtained", "some value")
      .put(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, "yes")
      .put(INFORMATION_PROVIDER_NAME, "name")
      .put(INFORMATION_PROVIDER_EMAIL, "email")
      .put(INFORMATION_PROVIDER_PHONE, "phone")
      .put("extractionMethod", new JsonObject().put("key", "c"))
      .put("samplingMethod", "simple")
      .put("sampleSize", "1000")
      .put("howOftenDoesTheDataNeedToBeExtracted", new JsonObject().put("key", "b"))
      .put(NEED_FOR_DATA_EXTRACTION_EVERY, new JsonObject().put("key", "d"))
      .put("needForDataExtractionEveryOther", "monthly")
      .put(WILL_CONTROLS_BE_EXTRACTED, new JsonObject().put("key", "a"))
      .put(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS, new JsonObject().put("key", "b"))
      .put("regionForControlsDataExtraction", "region")
      .put("variablesForDataExtraction", "variables")
      .put("timePeriodForDataExtraction", "2020-2023")
      .put("extractionCriteriaForControls", "criteria")
      .put(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY, new JsonObject().put("key", "b"))
      .put(DATA_EXTRACTION_FREQUENCY, new JsonObject().put("key", "b"))
      .put(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION, "yes")
      .put(DATA_NEEDS_TO_BE_EXTRACTED_EVERY, new JsonObject().put("key", "d"))
      .put(SPECIFY_OTHER, "value")
      .put(HAS_THE_GROUP_RELATIVES_FORMED_3, new JsonObject().put("key", "a"))
      .put("previouslyIssuedPermitIssuer3", "issuer")
      .put(WILL_RELATIVES_BE_EXTRACTED, new JsonObject().put("key", "a"))
      .put("willSameDataBeExtractedForRelatives", new JsonObject().put("key", "b"))
      .put("variablesForDataExtractionRelatives", "variables")
      .put("timePeriodForDataExtractionRelatives", "2021-2022")
      .put("relationshipToStudyCohort", "spouse")
      .put("willDataForRelativesBeExtractedSimultaneously", new JsonObject().put("key", "b"))
      .put(DATA_EXTRACTION_FREQUENCY_RELATIVES, new JsonObject().put("key", "b"))
      .put(DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES1, "yes")
      .put(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES, new JsonObject().put("key", "d"))
      .put(SPECIFY_OTHER_RELATIVES, "other")
      .put(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS,
        new JsonObject().put("key", "a"))
      .put(HAS_THE_GROUP_RELATIVES_FORMED_2, new JsonObject().put("key", "a"));

    JsonArray section6Array = new JsonArray().add(section);
    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", new JsonObject().put(SECTION_6, section6Array));

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection =
      applicationForm.getJsonObject("form").getJsonArray(SECTION_6).getJsonObject(0);

    assertTrue(
      cleanedSection.containsKey("doesTheInformedConsentCoverTheRequestedRegistryExtractions"));
    assertTrue(cleanedSection.containsKey("samplingMethod"));
    assertTrue(cleanedSection.containsKey("sampleSize"));
    assertTrue(cleanedSection.containsKey("needForDataExtractionEveryOther"));
    assertTrue(cleanedSection.containsKey(SPECIFY_OTHER));
    assertTrue(cleanedSection.containsKey("regionForControlsDataExtraction"));
    assertTrue(cleanedSection.containsKey(SPECIFY_OTHER_RELATIVES));
  }

  @Test
  void cleanSection7_shouldCleanUpSectionBasedOnConditions() {
    JsonObject section7 = new JsonObject()
      .put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED, new JsonObject().put("key", "b"))
      .put("combinedCountries", "country")
      .put("combinedDataHolders", "holder")
      .put("combinedDatabaseRegistries", "registries")
      .put("combinedDatasetsRegistries", "datasets")
      .put("provideInformationList", "info")

      .put(OTHER_PERMIT_APPLICATIONS, new JsonObject().put("key", "b"))
      .put("dateOfSubmittingTheApplication", "2023-01-01")
      .put("applicationIssuer", "issuer")
      .put("identificationCode", "ID123");

    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", new JsonObject().put(SECTION_7, section7));

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection = applicationForm.getJsonObject("form").getJsonObject(SECTION_7);

    assertFalse(cleanedSection.containsKey("combinedCountries"));
    assertFalse(cleanedSection.containsKey("combinedDataHolders"));
    assertFalse(cleanedSection.containsKey("combinedDatabaseRegistries"));
    assertFalse(cleanedSection.containsKey("combinedDatasetsRegistries"));
    assertFalse(cleanedSection.containsKey("provideInformationList"));

    assertFalse(cleanedSection.containsKey("dateOfSubmittingTheApplication"));
    assertFalse(cleanedSection.containsKey("applicationIssuer"));
    assertFalse(cleanedSection.containsKey("identificationCode"));
  }

  @Test
  void cleanSection8_shouldRemoveIfLaterWhen_WhenWhenDataNeededKeyIsNotB() {
    JsonObject section8 = new JsonObject()
      .put(WHEN_DATA_NEEDED, new JsonObject().put("key", "a"))
      .put("ifLaterWhen", "some date")
      .put(WILL_THE_DATA_BE_TRANSFERRED, new JsonObject().put("key", "a"))
      .put(LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA, new JsonArray())
      .put(LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA, new JsonArray())
      .put(LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA, new JsonArray())
      .put(LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA, new JsonArray());

    JsonObject form = new JsonObject()
      .put(SECTION_2, new JsonObject())
      .put(SECTION_3, new JsonObject().put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
        new JsonObject().put("key", "a")))
      .put(SECTION_5, new JsonObject())
      .put(SECTION_7, new JsonObject().put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED,
        new JsonObject().put("key", "a")))
      .put(SECTION_8, section8);

    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", form);

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection = applicationForm.getJsonObject("form").getJsonObject(SECTION_8);

    assertFalse(cleanedSection.containsKey("ifLaterWhen"));
  }

  @Test
  void cleanSection8_shouldRemoveFieldsBasedOnPublicSectorKey() {
    JsonObject section8 = new JsonObject()
      .put("estimatedStartAndEnd", "value")
      .put("estimatedStartDatesForDataProcessing", "value")
      .put("estimatedEndDatesForDataProcessing", "value")
      .put("informationWhichDataCanAccess", "value")
      .put("frequencyOfAccessOrDataUpdates", "value")
      .put(WILL_THE_DATA_BE_TRANSFERRED, new JsonObject().put("key", "a"));


    JsonObject form = new JsonObject()
      .put(SECTION_2, new JsonObject())
      .put(SECTION_3, new JsonObject().put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
        new JsonObject().put("key", "a")))
      .put(SECTION_5, new JsonObject())
      .put(SECTION_7, new JsonObject().put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED,
        new JsonObject().put("key", "a")))
      .put(SECTION_8, section8);

    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", form);

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection = applicationForm.getJsonObject("form").getJsonObject(SECTION_8);
    assertFalse(cleanedSection.containsKey("estimatedStartAndEnd"));
    assertFalse(cleanedSection.containsKey("estimatedStartDatesForDataProcessing"));
    assertFalse(cleanedSection.containsKey("estimatedEndDatesForDataProcessing"));
    assertTrue(cleanedSection.containsKey("informationWhichDataCanAccess"));
    assertTrue(cleanedSection.containsKey("frequencyOfAccessOrDataUpdates"));
  }

  @Test
  void cleanSection8_shouldRemoveTransferFields_WhenTransferKeyIsNotA() {
    JsonObject section8 = new JsonObject()
      .put(WILL_THE_DATA_BE_TRANSFERRED, new JsonObject().put("key", "c"))
      .put("whichCountriesDataBeProcessed", "data")
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47, false)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, false)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, false)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C, false)
      .put(LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU, new JsonObject().put("key", "a"));

    JsonObject form = new JsonObject()
      .put(SECTION_2, new JsonObject())
      .put(SECTION_3, new JsonObject().put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
        new JsonObject().put("key", "a")))
      .put(SECTION_5, new JsonObject().put(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA,
        new JsonObject().put("key", "a")))
      .put(SECTION_7, new JsonObject().put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED,
        new JsonObject().put("key", "b")))
      .put(SECTION_8, section8);

    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", form);

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection = applicationForm.getJsonObject("form").getJsonObject(SECTION_8);
    assertFalse(cleanedSection.containsKey("whichCountriesDataBeProcessed"));
    assertFalse(cleanedSection.containsKey("whyWillDataBeTransferredOutsideEUArticle47Options"));
    assertFalse(cleanedSection.containsKey("whyWillDataBeTransferredOutsideEUArticle48a"));
    assertFalse(
      cleanedSection.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT));
    assertFalse(
      cleanedSection.containsKey(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT));
    assertFalse(
      cleanedSection.containsKey("legalBasisForTransferringTheDataOutsideEUOtherOptions"));
    assertFalse(cleanedSection.containsKey("safeguardsAreProvidedByReferringGDCP"));
    assertFalse(cleanedSection.containsKey("europeanUnionInstitution"));
  }

  @Test
  void cleanSection8_shouldRemoveAttachResearchPermitIfPurposeNotE() {
    JsonArray purposeArray = new JsonArray()
      .add(new JsonObject().put("key", "a"))
      .add(new JsonObject().put("key", "c"));

    JsonObject section2 = new JsonObject()
      .put(PURPOSE_FOR_WHICH_DATA_WILL_BE_USED, purposeArray);

    JsonObject section8 = new JsonObject()
      .put("attachResearchPermitIfRequired", "permit")
      .put(WILL_THE_DATA_BE_TRANSFERRED, new JsonObject().put("key", "a"));


    JsonObject form = new JsonObject()
      .put(SECTION_2, section2)
      .put(SECTION_3, new JsonObject().put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
        new JsonObject().put("key", "b")))
      .put(SECTION_5, new JsonObject())
      .put(SECTION_7, new JsonObject().put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED,
        new JsonObject().put("key", "a")))
      .put(SECTION_8, section8);

    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", form);

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection = applicationForm.getJsonObject("form").getJsonObject(SECTION_8);
    assertFalse(cleanedSection.containsKey("attachResearchPermitIfRequired"));
  }

  @Test
  void cleanSection8_shouldRemoveLawfulnessFields_WhenConditionsNotMet() {
    JsonObject section3 = new JsonObject()
      .put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR, new JsonObject().put("key", "c"))
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "a"));

    JsonObject section5 = new JsonObject()
      .put(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA, new JsonObject().put("key", "a"));

    JsonObject section7 = new JsonObject()
      .put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED, new JsonObject().put("key", "a"));

    JsonObject section8 = new JsonObject()
      .put("lawfulForProcessingPersonalData", true)
      .put(LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA,
        new JsonArray().add(new JsonObject().put("key", "a")))
      .put("otherLegalBasisForProcessingPersonalData", "some other basis")
      .put(LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA,
        new JsonArray().add(new JsonObject().put("key", "a")))
      .put("otherLegalBasisForProcessingCombinedData", "other")
      .put("europeanUnionInstitution", "EU Inst")
      .put(LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA,
        new JsonArray().add(new JsonObject().put("key", "a")))
      .put("otherLegalBasisForProcessingApplicationData", "other")
      .put(LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA,
        new JsonArray().add(new JsonObject().put("key", "a")))
      .put("otherLegalBasisForProcessingCombinedApplicationData", "other");

    JsonObject form = new JsonObject()
      .put(SECTION_2, new JsonObject())
      .put(SECTION_3, section3)
      .put(SECTION_5, section5)
      .put(SECTION_7, section7)
      .put(SECTION_8, section8);

    JsonObject applicationForm = new JsonObject().put("form", form);

    dataAccessApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection8 = applicationForm.getJsonObject("form").getJsonObject(SECTION_8);

    assertFalse(cleanedSection8.containsKey(LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA));
    assertFalse(cleanedSection8.containsKey("otherLegalBasisForProcessingPersonalData"));
    assertFalse(cleanedSection8.containsKey(LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA));
    assertFalse(cleanedSection8.containsKey("otherLegalBasisForProcessingCombinedData"));
    assertFalse(cleanedSection8.containsKey("europeanUnionInstitution"));
    assertFalse(cleanedSection8.containsKey(LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA));
    assertFalse(cleanedSection8.containsKey("otherLegalBasisForProcessingApplicationData"));
    assertFalse(cleanedSection8.containsKey(LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA));
    assertFalse(cleanedSection8.containsKey("otherLegalBasisForProcessingCombinedApplicationData"));
  }

}

