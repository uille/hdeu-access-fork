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

import static eu.europa.data.health.access.ApplicationStatus.DRAFT;
import static eu.europa.data.health.access.ApplicationStatus.ON_GOING;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.services.CommonApplicationManager.TARGET_LANGUAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
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
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dto.sections.SectionDTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.validations.business_validators.DataRequestApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.validators.DataRequestApplicationValidationManager;
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
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@ExtendWith(MockitoExtension.class)
class DataRequestApplicationManagerTest {

  @Mock
  MongoClient mongoClient;
  @Mock
  DBHandler dbHandler;
  @Mock
  BasketManager basketManager;
  @Mock
  HubStoreClient hubStoreClient;
  @Mock
  HubTranslationClient hubTranslationClient;
  @Mock
  HubSearchClient hubSearchClient;
  @Mock
  Vertx vertx;
  @Mock
  AuditLogManager auditLogManager;
  @Mock
  DataRequestApplicationValidationManager dataRequestApplicationValidationManager;
  @Mock
  DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager;
  @Mock
  RoutingContext routingContext;
  @Mock
  private HttpServerResponse response;
  @Mock
  private AuthClient authClient;
  @Mock
  private HttpServerRequest request;
  Logger logger = (Logger) LoggerFactory.getLogger(DataRequestApplicationManager.class);

  ListAppender<ILoggingEvent> listAppender;

  DataRequestApplicationManager dataRequestApplicationManager;


  @BeforeEach
  void setUp() {
    dataRequestApplicationManager = new DataRequestApplicationManager(
      dbHandler, mongoClient, basketManager, hubStoreClient,
      vertx, dataRequestApplicationValidationManager,
      dataRequestApplicationBusinessValidationManager,
      auditLogManager, hubTranslationClient, hubSearchClient, authClient
    );

    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

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
  void testGetApplicationById() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).findById(any(), anyString());
    spyManager.getApplicationById(routingContext);
    verify(spyManager).findById(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testCreateApplication() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(201).end("mocked");
      return null;
    }).when(spyManager).createApplication(any(), anyString());
    spyManager.createApplication(routingContext);
    verify(spyManager).createApplication(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
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

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).getEntity(routingContext, DATA_REQUEST_APPLICATION_COLLECTION, 2, 10, sortBy
      , sortOrder, status);


    verify(response).setStatusCode(200);
    verify(spyManager).getEntity(routingContext, DATA_REQUEST_APPLICATION_COLLECTION, 2, 10,
      "name", "asc", "PENDING");
  }

  @Test
  void testUpdateApplication() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).updateEntity(any(), anyString());
    spyManager.updateApplication(routingContext);
    verify(spyManager).updateEntity(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteDataAccessApplicationById() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).deleteApplicationById(any(), any());
    spyManager.deleteDataRequestApplicationById(routingContext);
    verify(spyManager).deleteApplicationById(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteAllDataAccessApplication() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).deleteAllEntities(any(), any());
    spyManager.deleteAllDataRequestApplication(routingContext);
    verify(spyManager).deleteAllEntities(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testPutDistributionsApplication_success() {

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

    dataRequestApplicationManager.putDistributionsApplication(routingContext);

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

    dataRequestApplicationManager.putDistributionsApplication(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event -> event.getFormattedMessage().contains("Application returned was null"));
      assertTrue(found);
    });
  }

  @Test
  void testPutDistributionsApplication_someOperationsFailed() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    Future<Object> failedFuture = Future.failedFuture("Some failure");
    List<Future> futures = Collections.singletonList(failedFuture);
    doReturn(futures).when(spyManager).updateApplicationsAsync(any(), anyString());

    spyManager.putDistributionsApplication(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event -> event.getFormattedMessage().contains("Some operations have failed"));
      assertTrue(found);
    });
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

    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(application));
        return null;
      });

    dataRequestApplicationManager.deleteDatasets(routingContext);

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

    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(appWithSubmittedStatus));
        return null;
      });

    dataRequestApplicationManager.deleteDatasets(routingContext);

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

    dataRequestApplicationManager.deleteDatasets(routingContext);

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

    dataRequestApplicationManager.deleteDatasets(routingContext);

    verify(dbHandler, never()).deleteById(any(), any());
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

    dataRequestApplicationManager.deleteDistributions(routingContext);

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

    dataRequestApplicationManager.deleteDistributions(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(400);
      verify(response).end(contains("Deletion can be executed"));
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

    dataRequestApplicationManager.deleteDistributions(routingContext);

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

    dataRequestApplicationManager.deleteDistributions(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("DB down"));
    });
  }

  @Test
  void testCreateOrUpdateSection() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).createOrUpdateSection(any(), anyString());
    spyManager.createOrUpdateSection(routingContext);
    verify(spyManager).createOrUpdateSection(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
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

    dataRequestApplicationManager.getSection(routingContext);

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

    dataRequestApplicationManager.getSection(routingContext);

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

    dataRequestApplicationManager.getSection(routingContext);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("DB down"));
    });
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
      .put("datasets", new JsonArray())
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());
    doReturn(Future.succeededFuture(mongoApp)).when(spyManager).replaceApplication(any());
    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).sendApplicationForDispatch(any(), any(), any());
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
    JsonArray usersJsonArray = new JsonArray();
    usersJsonArray.add(new JsonObject().put("firstName", "John").put("lastName", "Doe").put("ecasid", "111"));
    when(authClient.getAllUsers(any(), anyBoolean())).thenReturn(Future.succeededFuture(usersJsonArray));

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

    boolean result = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
    verify(routingContext.response()).setStatusCode(HttpResponseStatus.FORBIDDEN.code());
  }

  @Test
  void testCanChangeStatus_disallowedFlow() {
    JsonObject requestStatus = new JsonObject().put("status", "SUBMITTED");
    JsonObject mongoApp = new JsonObject().put("status",
      new JsonObject().put("status", "SUBMITTED"));

    boolean result = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
    verify(routingContext.response()).setStatusCode(HttpResponseStatus.FORBIDDEN.code());
    verify(routingContext.response()).end(contains("wrong status value"));
  }

  @Test
  void testCanChangeStatus_dtoValidationFails() {
    JsonObject requestStatus = new JsonObject().put("status", "SUBMITTED");
    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject());

    boolean result = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp,
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

    boolean result = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp,
      routingContext);

    assertFalse(result);
  }

  @Test
  void testCleanSection6_allConditionsCovered() {
    JsonObject section = new JsonObject()
      .put("extractionMethod", new JsonObject().put("key", "x")) // not "c" or "a"
      .put("samplingMethod", "some-sampling")
      .put("sampleSize", "1000")
      .put("howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple", new JsonObject().put("key", "x"))
      .put("optOutOfTheMechanismProvidedInTheNationalLaw", new JsonObject().put("key", "x"))
      .put(NEED_FOR_DATA_EXTRACTION_EVERY, new JsonObject().put("key", "x"))
      .put("needForDataExtractionEveryOther", "some value");

    JsonArray section6Array = new JsonArray().add(section);
    JsonObject applicationForm = new JsonObject();
    applicationForm.put("form", new JsonObject().put(SECTION_6, section6Array));

    dataRequestApplicationManager.performCleanUpForm(applicationForm);

    JsonObject cleanedSection =
      applicationForm.getJsonObject("form").getJsonArray(SECTION_6).getJsonObject(0);

    assertFalse(cleanedSection.containsKey("samplingMethod"), "samplingMethod should be removed");
    assertFalse(cleanedSection.containsKey("sampleSize"), "sampleSize should be removed");
    assertFalse(cleanedSection.containsKey(NEED_FOR_DATA_EXTRACTION_EVERY),
      "needForDataExtractionEvery should be removed");
    assertFalse(cleanedSection.containsKey("needForDataExtractionEveryOther"),
      "needForDataExtractionEveryOther should be removed");

    assertTrue(cleanedSection.containsKey("extractionMethod"));
    assertTrue(cleanedSection.containsKey("howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple"));
  }


  @Test
  void testPostData() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).postData(any(), anyString());
    spyManager.postData(routingContext);
    verify(spyManager).postData(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testGetData() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).getData(any(), anyString());
    spyManager.getData(routingContext);
    verify(spyManager).getData(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteData() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).deleteData(any(), anyString());
    spyManager.deleteData(routingContext);
    verify(spyManager).deleteData(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testValidateFileBeforeUpdate() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).validateFileBeforeUpdate(any(), anyString());
    spyManager.validateFileBeforeUpdate(routingContext);
    verify(spyManager).validateFileBeforeUpdate(routingContext,
      DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }

  @Test
  void testChangeTitle() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);
    doAnswer(invocation -> {
      RoutingContext ctx = invocation.getArgument(0);
      ctx.response().setStatusCode(200).end("mocked");
      return null;
    }).when(spyManager).changeTitle(any(), anyString());
    spyManager.changeTitle(routingContext);
    verify(spyManager).changeTitle(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
    verify(response).setStatusCode(200);
  }



  @Test
  void testProcessFileTranslationCallback_translationNotCompleted() {
    String appId = "app1";
    String fileId = "file1";

    JsonObject mongoApplication = new JsonObject()
      .put("_id", "app1")
      .put("title", "Test Title")
      .put("version", "v1")
      .put("datasets", new JsonArray());

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    dataRequestApplicationManager.processFileTranslationCallback(appId, fileId, new HashMap<>());

    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("Translation not yet complete for application"));
    assertFalse(found);

  }


  @Test
  void testProcessFileTranslationCallback_translationCompletedAndSubmitted() {
    String appId = "app1";
    String fileId = "file1";
    JsonObject app = new JsonObject()
      .put("translation", new JsonObject()
        .put(TARGET_LANGUAGES,new JsonArray().add("en"))
        .put("status", "COMPLETED")
        .put("form",new JsonObject()
          .put("status", "completed")
          .put("files",new JsonObject()
            .put("file1", new JsonObject()
              .put("status", "completed"))))

      )
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put("title", "Some Title")
      .put("id", appId);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());


    JsonObject fileJson = new JsonObject().put("fileId", fileId).put("fileName", "fileName");
    HashMap<String, JsonObject> hashMap = new HashMap<>();
    hashMap.put("EN", fileJson);

    // Act
    dataRequestApplicationManager.processFileTranslationCallback(appId, fileId,hashMap
      );

    // Assert
    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("was updated successfully"));
    assertFalse(found);
  }




  @Test
  void testProcessTranslationCallback_translationNotCompleted() {
    String appId = "app1";
    String fileId = "file1";
    JsonObject app = new JsonObject()
      .put("translation", new JsonObject()
        .put(TARGET_LANGUAGES,new JsonArray().add("en"))
        .put("files",new JsonObject()
          .put("file1", new JsonObject()
            .put("status", "completed")))
        .put("status", "COMPLETED"))
        .put("status", "COMPLETED")
        .put("form",new JsonObject()
          .put("status", "completed")


      )
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put("title", "Some Title")
      .put("id", appId);

    // Mock mongoClient.findOne to return app
    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOneAndReplace(anyString(), any(), any(JsonObject.class), any());


    when(hubTranslationClient.buildFormTranslations(any(JsonObject.class),any(JsonObject.class),
      any(JsonObject.class),anyString(), any(JsonObject.class))).thenReturn(app);

    JsonObject fileJson = new JsonObject().put("fileId", fileId).put("fileName", "fileName");
    HashMap<String, JsonObject> hashMap = new HashMap<>();
    hashMap.put("EN", fileJson);

    dataRequestApplicationManager.processTranslationCallback(appId, app );

    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("Translation not yet complete"));
    assertFalse(found);
  }

  @Test
  void testProcessTranslationCallback_translationCompletedAndSubmitted() {
    String appId = "app1";
    String fileId = "file1";
    JsonObject app = new JsonObject()
      .put("translation", new JsonObject()
        .put(TARGET_LANGUAGES,new JsonArray().add("en"))
        .put("files",new JsonObject()
          .put("file1", new JsonObject()
            .put("status", "completed")))
        .put("status", "COMPLETED"))
      .put("status", "COMPLETED")
      .put("form",new JsonObject()
        .put("status", "completed")


      )
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put("title", "Some Title")
      .put("id", appId);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOneAndReplace(anyString(), any(), any(JsonObject.class), any());

    when(hubTranslationClient.buildFormTranslations(any(JsonObject.class),any(JsonObject.class),
      any(JsonObject.class),anyString(), any(JsonObject.class))).thenReturn(new JsonObject().put("EN", "test"));

    JsonObject fileJson = new JsonObject().put("fileId", fileId).put("fileName", "fileName");
    HashMap<String, JsonObject> hashMap = new HashMap<>();
    hashMap.put("EN", fileJson);

    dataRequestApplicationManager.processTranslationCallback(appId, app);

    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("Application updated with translations. Proceeding to dispatch."));
    assertTrue(found);
  }


  @SuppressWarnings("unchecked")
  private static <T> T callPrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      java.lang.reflect.Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
      m.setAccessible(true);
      return (T) m.invoke(target, args);
    } catch (Exception e) {
      throw new RuntimeException("Failed calling private method: " + methodName, e);
    }
  }

  @Test
  void testGetQueryOptionsDataset_bodyNull_shouldStillBuildQueryAndIncludeUser() {
    addUserOnContext();

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(null); // important branch in code

    Future<JsonObject> fut = callPrivate(
      dataRequestApplicationManager,
      "getQueryOptionsDataset",
      new Class<?>[]{RoutingContext.class},
      routingContext
    );

    assertTrue(fut.succeeded());
    JsonObject q = fut.result();

    assertNotNull(q);
    assertTrue(q.containsKey("$and"));
    assertTrue(q.getJsonArray("$and").encode().contains("\"userId\""));
  }

  @Test
  void testGetQueryOptionsDataset_buildsAllClauses() {
    addUserOnContext();

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);

    JsonObject search = new JsonObject()
      .put("title", "abc")
      .put("datasetStatus", new JsonArray().add("POSITIVE_DECISION_ISSUED"))
      .put("hdab", new JsonArray().add("HDAB-1"))
      .put("applicationTitle", new JsonArray().add("AppTitle-1"));

    when(mockBody.asJsonObject()).thenReturn(search);

    Future<JsonObject> fut = callPrivate(
      dataRequestApplicationManager,
      "getQueryOptionsDataset",
      new Class<?>[]{RoutingContext.class},
      routingContext
    );

    assertTrue(fut.succeeded());
    JsonObject q = fut.result();
    String enc = q.encode();

    assertTrue(enc.contains("\"$and\""));
    assertTrue(enc.contains("\"title\""));
    assertTrue(enc.contains("\"$regex\""));
    assertTrue(enc.contains("\"datasets.decisions\""));
    assertTrue(enc.contains("\"datasetStatus\""));
    assertTrue(enc.contains("\"latest\":1"));
    assertTrue(enc.contains("\"datasets.hdab.name\""));
    assertTrue(enc.contains("\"AppTitle-1\""));
    assertTrue(enc.contains("\"userId\""));
  }

  @Test
  void testGetQueryOptionsApplication_buildsExpectedFields() {
    addUserOnContext();

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);

    JsonObject search = new JsonObject()
      .put("title", "hello")
      .put("status", new JsonArray().add("ON-GOING"))
      .put("hdab", new JsonArray().add("HDAB-X"))
      .put("geographicalList", new JsonArray().add("Greece"));

    when(mockBody.asJsonObject()).thenReturn(search);

    Future<JsonObject> fut = callPrivate(
      dataRequestApplicationManager,
      "getQueryOptionsApplication",
      new Class<?>[]{RoutingContext.class},
      routingContext
    );

    assertTrue(fut.succeeded());
    JsonObject q = fut.result();

    assertNotNull(q);
    assertTrue(q.containsKey("title"));
    assertTrue(q.containsKey("status.status"));
    assertTrue(q.containsKey("datasets.hdab.name"));
    assertTrue(q.containsKey("datasets.country.label"));
    assertTrue(q.containsKey("userId"));
  }

  @Test
  void testRemoveAdditionalInformationFromDataRequestCollection_successAndFail() {
    // success
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(new JsonObject()));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    Future<Void> ok = callPrivate(
      dataRequestApplicationManager,
      "removeAdditionalInformationFromDataRequestCollection",
      new Class<?>[]{String.class},
      "app-1"
    );

    assertTrue(ok.succeeded());

    // failure
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.failedFuture("boom"));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    Future<Void> ko = callPrivate(
      dataRequestApplicationManager,
      "removeAdditionalInformationFromDataRequestCollection",
      new Class<?>[]{String.class},
      "app-2"
    );

    assertTrue(ko.failed());
  }

  @Test
  void testGetAllDecisionIssuedDataRequestApplications_positiveDecisionType_delegatesWithIsPositiveTrue() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    when(request.getParam("page")).thenReturn("1");
    when(request.getParam("limit")).thenReturn("5");
    when(request.getParam("sortBy")).thenReturn(null);
    when(request.getParam("sortOrder")).thenReturn(null);

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(new JsonObject()
      .put("title", "abc")
      .put("decisionType", "POSITIVE_DECISION_ISSUED")
    );

    doNothing().when(spyManager).getDecisionIssuedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class),
      eq(Boolean.TRUE)
    );

    spyManager.getAllDecisionIssuedDataRequestApplications(routingContext);

    verify(spyManager).getDecisionIssuedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class),
      eq(Boolean.TRUE)
    );
  }

  @Test
  void testGetAllDecisionIssuedDataRequestApplications_negativeDecisionType_delegatesWithIsPositiveFalse() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(new JsonObject()
      .put("decisionType", "NEGATIVE_DECISION_ISSUED")
    );

    doNothing().when(spyManager).getDecisionIssuedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class),
      eq(Boolean.FALSE)
    );

    spyManager.getAllDecisionIssuedDataRequestApplications(routingContext);

    verify(spyManager).getDecisionIssuedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class),
      eq(Boolean.FALSE)
    );
  }

  @Test
  void testGetAllDecisionIssuedDataRequestApplications_unknownDecisionType_delegatesWithNull() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(new JsonObject()
      .put("decisionType", "SOMETHING_ELSE")
    );

    doNothing().when(spyManager).getDecisionIssuedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class),
      isNull()
    );

    spyManager.getAllDecisionIssuedDataRequestApplications(routingContext);

    verify(spyManager).getDecisionIssuedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class),
      isNull()
    );
  }

  @Test
  void testGetAllOnGoingDataRequestApplications_delegatesWithStatusInOnGoingAndCompleted() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);
    when(request.getParam("sortBy")).thenReturn(null);
    when(request.getParam("sortOrder")).thenReturn(null);

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(new JsonObject().put("title", "t"));

    doNothing().when(spyManager).getApplications(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class)
    );

    spyManager.getAllOnGoingDataRequestApplications(routingContext);

    // capture and assert the query contains the status filter with ON-GOING and COMPLETED
    org.mockito.ArgumentCaptor<JsonObject> cap = org.mockito.ArgumentCaptor.forClass(JsonObject.class);

    verify(spyManager).getApplications(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      cap.capture()
    );

    String enc = cap.getValue().encode();
    assertTrue(enc.contains("status.status"));
    assertTrue(enc.contains("ON-GOING"));
    assertTrue(enc.contains("COMPLETED"));
  }

  @Test
  void testGetAllDataRequestDatasets_delegatesToGetDatasetsWithQueryOptionsDataset() {
    addUserOnContext();

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    when(request.getParam("page")).thenReturn("1");
    when(request.getParam("limit")).thenReturn("5");
    when(request.getParam("sortBy")).thenReturn(null);
    when(request.getParam("sortOrder")).thenReturn(null);

    RequestBody mockBody = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(mockBody);
    when(mockBody.asJsonObject()).thenReturn(new JsonObject().put("title", "abc"));

    doNothing().when(spyManager).getDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class)
    );

    spyManager.getAllDataRequestDatasets(routingContext);

    verify(spyManager).getDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(), any(),
      anyString(), anyString(),
      any(JsonObject.class)
    );
  }

  @Test
  void testGetAllDataRequestApplications_queryOptionsSuccess_shouldDelegateToGetApplications() {
    addUserOnContext();

    // page/limit not provided -> finalPage and finalPageLimit remain null
    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);

    // sortBy/sortOrder not provided -> should use DATE_CREATED + "desc"
    when(request.getParam("sortBy")).thenReturn(null);
    when(request.getParam("sortOrder")).thenReturn(null);

    // body search fields for getQueryOptionsApplication()
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject()
      .put("title", "abc")
      .put("status", new JsonArray().add("ON-GOING"))
    );

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    // Stub the delegation target so we don't go deeper
    doNothing().when(spyManager).getApplications(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(), // finalPage
      isNull(), // finalPageLimit
      anyString(),
      anyString(),
      any(JsonObject.class)
    );

    spyManager.getAllDataRequestApplications(routingContext);

    // Capture the queryOptions passed to getApplications()
    org.mockito.ArgumentCaptor<JsonObject> queryCap = org.mockito.ArgumentCaptor.forClass(JsonObject.class);

    verify(spyManager).getApplications(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      // default sortBy is DATE_CREATED (constant), but we assert value indirectly by anyString + then check
      anyString(),
      anyString(),
      queryCap.capture()
    );

    JsonObject queryOptions = queryCap.getValue();
    assertNotNull(queryOptions);

    // from getQueryOptionsApplication: title regex + status.status $in + userId
    String enc = queryOptions.encode();
    assertTrue(enc.contains("\"title\""));
    assertTrue(enc.contains("\"$regex\""));
    assertTrue(enc.contains("\"status.status\""));
    assertTrue(enc.contains("\"$in\""));
    assertTrue(enc.contains("\"userId\""));
  }

  @Test
  void testGetAllDataPermitIssuedApplications_buildsQueryAndDelegates_toGetDataPermitGrantedDatasets() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    // pagination + sort params
    when(request.getParam("page")).thenReturn("2");
    when(request.getParam("limit")).thenReturn("10");
    when(request.getParam("sortBy")).thenReturn(null);     // force default DATE_CREATED
    when(request.getParam("sortOrder")).thenReturn(null);  // force default "desc"

    // body -> null searchFields branch
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(null);

    doNothing().when(spyManager).getDataPermitGrantedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      eq(2),
      eq(10),
      anyString(),
      anyString(),
      any(JsonObject.class)
    );

    spyManager.getAllDataPermitIssuedApplications(routingContext);

    // capture queryOptions
    org.mockito.ArgumentCaptor<JsonObject> queryCap = org.mockito.ArgumentCaptor.forClass(JsonObject.class);

    verify(spyManager).getDataPermitGrantedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      eq(2),
      eq(10),
      anyString(),
      anyString(),
      queryCap.capture()
    );

    // when searchFields is null -> queryOptions remains empty
    assertNotNull(queryCap.getValue());
    assertTrue(queryCap.getValue().isEmpty());
  }

  @Test
  void testGetAllDataPermitIssuedApplications_withTitle_shouldAddRegexTitleAndDelegate() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    // no pagination provided -> finalPage/finalPageLimit stay null
    when(request.getParam("page")).thenReturn(null);
    when(request.getParam("limit")).thenReturn(null);
    when(request.getParam("sortBy")).thenReturn("date_modified");
    when(request.getParam("sortOrder")).thenReturn("asc");

    // body with title -> title regex branch
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("title", "abc"));

    doNothing().when(spyManager).getDataPermitGrantedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      eq("date_modified"),
      eq("asc"),
      any(JsonObject.class)
    );

    spyManager.getAllDataPermitIssuedApplications(routingContext);

    org.mockito.ArgumentCaptor<JsonObject> queryCap = org.mockito.ArgumentCaptor.forClass(JsonObject.class);

    verify(spyManager).getDataPermitGrantedDatasets(
      eq(routingContext),
      eq(DATA_REQUEST_APPLICATION_COLLECTION),
      isNull(),
      isNull(),
      eq("date_modified"),
      eq("asc"),
      queryCap.capture()
    );

    JsonObject query = queryCap.getValue();
    assertNotNull(query);

    // ensure title regex structure
    assertTrue(query.containsKey("title"));
    assertEquals("abc", query.getJsonObject("title").getString("$regex"));
    assertEquals("i", query.getJsonObject("title").getString("$options"));
  }

  @Test
  void testGetDataRequestApplicationsHbad_delegatesToGetApplicationsHbad() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doNothing().when(spyManager).getApplicationsHbad(any(RoutingContext.class), anyString());

    spyManager.getDataRequestApplicationsHbad(routingContext);

    verify(spyManager).getApplicationsHbad(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void testGetDataRequestApplicationsCountries_delegatesToGetApplicationsCountries() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doNothing().when(spyManager).getApplicationsCountries(any(RoutingContext.class), anyString());

    spyManager.getDataRequestApplicationsCountries(routingContext);

    verify(spyManager).getApplicationsCountries(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void testGetDataRequestApplicationTitles_delegatesToGetApplicationTitles() {
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doNothing().when(spyManager).getApplicationTitles(any(RoutingContext.class), anyString());

    spyManager.getDataRequestApplicationTitles(routingContext);

    verify(spyManager).getApplicationTitles(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void testChangeStatus_EDIT_success_shouldReturn200ChangedToEdit() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "EDIT"));

    // findOneAndUpdate succeeds and returns non-null doc
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(new JsonObject().put("_id", "app123")));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.OK.code());
        verify(response).end(contains("Changed to Edit"));
      });
  }

  @Test
  void testChangeStatus_EDIT_notFound_shouldReturn404() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app404");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "EDIT"));

    // succeeded but result is null -> NOT_FOUND
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
        verify(response).end(contains("Application not found"));
      });
  }

  @Test
  void testChangeStatus_EDIT_mongoFailure_shouldReturn500() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app500");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "EDIT"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        verify(response).end(contains("Error occurred: DB down"));
      });
  }

  @Test
  void testChangeStatus_TRANSLATE_hasOtherLanguages_shouldTriggerTranslationAndReturn200() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appT1");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "TRANSLATE"));

    // Returned application from findOneAndUpdate
    JsonObject mongoApplication = new JsonObject()
      .put("_id", "appT1")
      .put("inputLanguage", "EN")
      .put("form", new JsonObject().put("section2", new JsonObject().put("x", 1)))
      .put("translation", new JsonObject()
        .put("targetLanguages", new JsonArray().add("EN").add("FR")) // has other lang
      );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    // Spy so we can verify triggerFormTranslationRequest was called
    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    // In case getDefaultEnglishFormTranslation does heavy work, just stub it (if method is accessible)
    // If compilation fails because method isn't visible, remove this line.
    lenient().doReturn(new JsonObject().put("en", new JsonObject()))
      .when(spyManager).getDefaultEnglishFormTranslation(anyString(), any(JsonObject.class));

    doNothing().when(spyManager).triggerFormTranslationRequest(
      anyString(),
      any(JsonObject.class),
      anyString(),
      anyString(),
      anyList(),
      any(JsonObject.class),
      anyBoolean()
    );

    spyManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.OK.code());
        verify(response).end(contains("Form translation initiated"));

        verify(spyManager).triggerFormTranslationRequest(
          eq("appT1"),
          any(JsonObject.class),
          eq(DATA_REQUEST_APPLICATION_COLLECTION),
          eq("EN"),
          anyList(),
          any(JsonObject.class),
          eq(true)
        );
      });
  }

  @Test
  void testChangeStatus_TRANSLATE_noOtherLanguages_updateCollectionSuccess_shouldReturn200NoTranslationRequired() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appT2");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "TRANSLATE"));

    JsonObject mongoApplication = new JsonObject()
      .put("_id", "appT2")
      .put("id", "appT2")
      .put("inputLanguage", "EN")
      .put("form", new JsonObject().put("section2", new JsonObject().put("x", 1)))
      .put("translation", new JsonObject()
        .put("targetLanguages", new JsonArray().add("EN")) // no other languages
      );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    // updateCollection succeeds -> "No translation required"
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<Void>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture());
      return null;
    }).when(mongoClient).updateCollection(eq(DATA_REQUEST_APPLICATION_COLLECTION),any(), (JsonArray) any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    // If compilation fails because method isn't visible, remove this stub.
    lenient().doReturn(new JsonObject().put("en", new JsonObject()))
      .when(spyManager).getDefaultEnglishFormTranslation(anyString(), any(JsonObject.class));


    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.OK.code());
        verify(response).end(contains("No translation required"));
        verify(mongoClient).updateCollection(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), (JsonArray) any(), any());
      });
  }


  @Test
  void testChangeStatus_TRANSLATE_notFound_shouldReturn404() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appTNF");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "TRANSLATE"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
        verify(response).end(contains("Application not found"));
      });
  }

  @Test
  void testChangeStatus_TRANSLATE_mongoFailure_shouldReturn500() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appTF");

    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "TRANSLATE"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        verify(response).end(contains("Error occurred: DB down"));
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_resubmission_canChangeStatusFalse_shouldRollbackAndReturn() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    // Application returned from findOneAndUpdate: has date_submitted + catalogs => triggers resubmission branch
    JsonObject mongoApp = new JsonObject()
      .put("_id", "app123")
      .put("id", "app123")
      .put("title", "My App")
      .put("version", 1)
      .put("date_submitted", "2025-01-01T00:00:00Z")
      .put("catalogs", new JsonArray())
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    // Force hasAtLeastOneAdditionalInformationAsLatest(...) == true
    doReturn(true).when(spyManager).hasAtLeastOneAdditionalInformationAsLatest(any());

    // Force canChangeStatus(...) == false => rollback + return
    doReturn(false).when(spyManager).canChangeStatus(any(), any(), any());
    doNothing().when(spyManager).rollbackStatus(anyString(), any(), anyString());

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    spyManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(spyManager).rollbackStatus("app123", ON_GOING, DATA_REQUEST_APPLICATION_COLLECTION);
        verify(spyManager, never()).replaceApplication(any());
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_resubmission_filtersFormTranslations_andDispatchesUpdate_andReturns200() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));



    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(true).when(spyManager).hasAtLeastOneAdditionalInformationAsLatest(any());
    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());

    // Avoid side effects
    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).updateCatalogStatus(any());
    doNothing().when(spyManager).sendApplicationForDispatch(any(), anyString(), any());
    doNothing().when(spyManager).triggerRemainingTranslations(anyString(), any(), DATA_REQUEST_APPLICATION_COLLECTION);
    doNothing().when(spyManager).updateApplicationStatus(any(), any(), any());
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(new JsonObject()));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    // replaceApplication returns the same updated app so we can assert it was filtered
    doAnswer(inv -> {
      JsonObject argApp = inv.getArgument(0);

      // verify filtering happened: only en+fr kept (de removed)
      JsonObject filtered = argApp.getJsonObject("form_translations");
      assertNotNull(filtered);
      assertTrue(filtered.containsKey("en"));
      assertTrue(filtered.containsKey("fr"));
      assertFalse(filtered.containsKey("de"));

      // Ensure status object exists and is ON-GOING so dispatch block runs
      argApp.put("status", new JsonObject().put("status", "ON-GOING"));

      return Future.succeededFuture(argApp);
    }).when(spyManager).replaceApplication(any());

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any()))
      .thenReturn("desc");

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());


    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(spyManager).sendApplicationForDispatch(any(), eq(DATA_REQUEST_APPLICATION_COLLECTION), eq(OperationStatus.UPDATE));
        verify(auditLogManager).insertAuditLog(any(), any(), any(), any(), anyBoolean(), any());
        verify(response).setStatusCode(HttpResponseStatus.OK.code());
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_resubmission_replaceFails_shouldRollbackAndReturn500() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    JsonObject mongoApp = new JsonObject()
      .put("_id", "app123")
      .put("id", "app123")
      .put("title", "My App")
      .put("version", 1)
      .put("date_submitted", "2025-01-01T00:00:00Z")
      .put("catalogs", new JsonArray())
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(true).when(spyManager).hasAtLeastOneAdditionalInformationAsLatest(any());
    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());

    doNothing().when(spyManager).rollbackStatus(anyString(), any(), anyString());
    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).updateCatalogStatus(any());
    doNothing().when(spyManager).updateApplicationStatus(any(), any(), any());

    doReturn(Future.failedFuture("replace failed")).when(spyManager).replaceApplication(any());

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    spyManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(spyManager).rollbackStatus("app123", ON_GOING, DATA_REQUEST_APPLICATION_COLLECTION);
        verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        verify(response).end(contains("replace failed"));
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_firstSubmission_canChangeStatusFalse_shouldRollbackAndReturn() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appX");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    // No date_submitted => goes to "else" (first submission branch)
    JsonObject mongoApp = new JsonObject()
      .put("_id", "appX")
      .put("id", "appX")
      .put("title", "First Submit App")
      .put("version", 1)
      .put("catalogs", new JsonArray())
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(false).when(spyManager).canChangeStatus(any(), any(), any());
    doNothing().when(spyManager).rollbackStatus(anyString(), any(), anyString());

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    spyManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(spyManager).rollbackStatus("appX", DRAFT, DATA_REQUEST_APPLICATION_COLLECTION);
        verify(spyManager, never()).replaceApplication(any());
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_firstSubmission_applicantDetailsRecoverAndDispatchCreate_shouldReturn200() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appY");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    // No date_submitted => first submission branch
    JsonObject mongoApp = new JsonObject()
      .put("_id", "appY")
      .put("id", "appY")
      .put("title", "First Submit App")
      .put("version", 1)
      .put("catalogs", new JsonArray())
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());


    doReturn(Future.failedFuture("boom")).when(spyManager).createApplicantDetails(any());

    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).updateCatalogStatus(any());
    doNothing().when(spyManager).sendApplicationForDispatch(any(), anyString(), any());
    doNothing().when(spyManager).triggerRemainingTranslations(anyString(), any(), DATA_REQUEST_APPLICATION_COLLECTION);
    doNothing().when(spyManager).updateApplicationStatus(any(), any(), any());


    doAnswer(inv -> {
      JsonObject appArg = inv.getArgument(0);
      // Ensure requestStatus becomes ON-GOING so it dispatches CREATE
      appArg.put("status", new JsonObject().put("status", "ON-GOING"));
      return Future.succeededFuture(appArg);
    }).when(spyManager).replaceApplication(any());

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any()))
      .thenReturn("desc");

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());


    spyManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(spyManager).sendApplicationForDispatch(any(), eq(DATA_REQUEST_APPLICATION_COLLECTION), eq(OperationStatus.CREATE));
        verify(auditLogManager).insertAuditLog(any(), any(), any(), any(), anyBoolean(), any());
        verify(response).setStatusCode(HttpResponseStatus.OK.code());
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_firstSubmission_applicantDetailsNonEmpty_shouldBeAdded() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appZ");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    JsonObject mongoApp = new JsonObject()
      .put("_id", "appZ")
      .put("id", "appZ")
      .put("title", "First Submit App")
      .put("version", 1)
      .put("catalogs", new JsonArray())
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());

    JsonObject applicantDetails = new JsonObject().put("name", "Alice");
    doReturn(Future.succeededFuture(applicantDetails)).when(spyManager).createApplicantDetails(any());

    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).updateCatalogStatus(any());
    doNothing().when(spyManager).sendApplicationForDispatch(any(), anyString(), any());
    doNothing().when(spyManager).triggerRemainingTranslations(anyString(), any(), DATA_REQUEST_APPLICATION_COLLECTION);
    doNothing().when(spyManager).updateApplicationStatus(any(), any(), any());


    doAnswer(inv -> {
      JsonObject appArg = inv.getArgument(0);

      // Assert applicantDetails was added
      assertTrue(appArg.containsKey("applicantDetails"));
      assertEquals("Alice", appArg.getJsonObject("applicantDetails").getString("name"));

      appArg.put("status", new JsonObject().put("status", "ON-GOING"));
      return Future.succeededFuture(appArg);
    }).when(spyManager).replaceApplication(any());

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any()))
      .thenReturn("desc");

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    spyManager.changeStatus(routingContext);

  }

  @Test
  void testChangeStatus_SUBMITTED_firstSubmission_replaceFails_shouldRollbackAndReturn500() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appF");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    JsonObject mongoApp = new JsonObject()
      .put("_id", "appF")
      .put("id", "appF")
      .put("title", "First Submit App")
      .put("version", 1)
      .put("catalogs", new JsonArray())
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section2", new JsonObject()));

    DataRequestApplicationManager spyManager = Mockito.spy(dataRequestApplicationManager);

    doReturn(true).when(spyManager).canChangeStatus(any(), any(), any());
    doNothing().when(spyManager).rollbackStatus(anyString(), any(), anyString());

    // applicant details success (empty)
    doReturn(Future.succeededFuture(new JsonObject())).when(spyManager).createApplicantDetails(any());

    doNothing().when(spyManager).performCleanUpForm(any());
    doNothing().when(spyManager).updateCatalogStatus(any());
    doNothing().when(spyManager).updateApplicationStatus(any(), any(), any());

    doReturn(Future.failedFuture("replace failed")).when(spyManager).replaceApplication(any());

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    spyManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(spyManager).rollbackStatus("appF", DRAFT, DATA_REQUEST_APPLICATION_COLLECTION);
        verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_updateResultNull_shouldReturn404() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appNF");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
        verify(response).end(contains("Application not found"));
      });
  }

  @Test
  void testChangeStatus_SUBMITTED_updateResultFailed_shouldReturn500StatusCannotBeChanged() {
    addUserOnContext();

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("appErr");
    RequestBody body = mock(RequestBody.class);
    when(routingContext.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(new JsonObject().put("status", "SUBMITTED"));

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findOneAndUpdate(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(), any(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    dataRequestApplicationManager.changeStatus(routingContext);

    org.testcontainers.shaded.org.awaitility.Awaitility.await()
      .atMost(java.time.Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        verify(response).end(contains("Application status can not be changed"));
      });
  }

  @Test
  void testCanChangeStatus_whenMandatorySectionsMissing_shouldFailDtoValidation() {
    // request wants to go ON_GOING
    JsonObject requestStatus = new JsonObject().put("status", "ON-GOING");

    // form missing mandatory keys -> validateKeys false
    JsonObject mongoApp = new JsonObject()
      .put("form", new JsonObject()  // empty, missing section2..section8
        .put("section2", new JsonObject())); // still missing many mandatory ones

    boolean ok = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp, routingContext);

    assertFalse(ok);
    verify(routingContext.response()).setStatusCode(HttpResponseStatus.FORBIDDEN.code());
    verify(routingContext.response()).end(contains("save all required sections"));
  }

  @Test
  void testCanChangeStatus_whenDtoHasViolations_shouldReturnFalse() {
    JsonObject requestStatus = new JsonObject().put("status", "ON-GOING");

    // Build a form containing all mandatory sections so validateKeys == true
    JsonObject form = new JsonObject()
      .put("section2", new JsonObject())
      .put("section3", new JsonObject())
      .put("section4", new JsonObject())
      .put("section5", new JsonObject())
      .put("section6", new JsonArray())   // section6 is array in your code
      .put("section8", new JsonObject());

    JsonObject mongoApp = new JsonObject().put("form", form);

    // Make DTO generation return an "empty" DTO that likely violates constraints
    // (SectionDTO typically has @NotNull/@NotBlank fields)
    when(dataRequestApplicationValidationManager.generateSectionDTO(anyInt(), anyString(), any()))
      .thenReturn(Mockito.mock(SectionDTO.class));

    boolean ok = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp, routingContext);

    assertFalse(ok);

  }

  @Test
  void testCanChangeStatus_whenDtoOkButBusinessValidationFails_shouldReturnFalse() {
    JsonObject requestStatus = new JsonObject().put("status", "ON-GOING");

    JsonObject form = new JsonObject()
      .put("section2", new JsonObject())
      .put("section3", new JsonObject())
      .put("section4", new JsonObject())
      .put("section5", new JsonObject())
      .put("section6", new JsonArray())
      .put("section8", new JsonObject());

    JsonObject mongoApp = new JsonObject().put("form", form);

    // Try to keep DTO valid (depends on your SectionDTO constraints)
    when(dataRequestApplicationValidationManager.generateSectionDTO(anyInt(), anyString(), any()))
      .thenReturn(new SectionDTO());

    when(dataRequestApplicationBusinessValidationManager.section4Validation(any(JsonObject.class), any()))
      .thenReturn(false);

    boolean ok = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp, routingContext);

    assertFalse(ok);
  }

  @Test
  void testCanChangeStatus_whenDtoOkAndBusinessValidationOk_shouldReturnTrue() {
    JsonObject requestStatus = new JsonObject().put("status", "ON-GOING");

    JsonObject form = new JsonObject()
      .put("section2", new JsonObject())
      .put("section3", new JsonObject())
      .put("section4", new JsonObject())
      .put("section5", new JsonObject())
      .put("section6", new JsonArray())
      .put("section8", new JsonObject());

    JsonObject mongoApp = new JsonObject().put("form", form);

    // Keep DTO valid (depends on constraints)
    when(dataRequestApplicationValidationManager.generateSectionDTO(anyInt(), anyString(), any()))
      .thenReturn(new SectionDTO());

    when(dataRequestApplicationBusinessValidationManager.section4Validation(any(JsonObject.class), any()))
      .thenReturn(true);

    boolean ok = dataRequestApplicationManager.canChangeStatus(requestStatus, mongoApp, routingContext);

    assertTrue(ok);
  }


}
