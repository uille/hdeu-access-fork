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
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.services.CommonApplicationManager.TARGET_LANGUAGES;
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
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.europa.data.health.access.auth_client.AuthClient;
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
      auditLogManager, hubTranslationClient, hubSearchClient, false, authClient
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

    spyManager.getAllDataRequestApplications(routingContext);

    verify(response).setStatusCode(200);
    verify(spyManager).getEntity(eq(routingContext), eq(DATA_REQUEST_APPLICATION_COLLECTION), eq(2),
      eq(10),
      eq("name"), eq("asc"), eq("PENDING"));
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
    verify(routingContext.response()).end(contains("wrong flow"));
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
    assertTrue(found);

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

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(app));
      return null;
    }).when(mongoClient).findOneAndReplace(anyString(), any(), any(JsonObject.class), any());

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any()))
      .thenReturn("desc");

    JsonObject fileJson = new JsonObject().put("fileId", fileId).put("fileName", "fileName");
    HashMap<String, JsonObject> hashMap = new HashMap<>();
    hashMap.put("EN", fileJson);

    // Act
    dataRequestApplicationManager.processFileTranslationCallback(appId, fileId,hashMap
      );

    // Assert
    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("was updated successfully"));
    assertTrue(found);
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
      any(JsonObject.class),anyString())).thenReturn(app);

    JsonObject fileJson = new JsonObject().put("fileId", fileId).put("fileName", "fileName");
    HashMap<String, JsonObject> hashMap = new HashMap<>();
    hashMap.put("EN", fileJson);

    dataRequestApplicationManager.processTranslationCallback(appId, app );

    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("Translation not yet complete"));
    assertTrue(found);
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
      any(JsonObject.class),anyString())).thenReturn(new JsonObject().put("EN", "test"));

    JsonObject fileJson = new JsonObject().put("fileId", fileId).put("fileName", "fileName");
    HashMap<String, JsonObject> hashMap = new HashMap<>();
    hashMap.put("EN", fileJson);

    dataRequestApplicationManager.processTranslationCallback(appId, app );

    boolean found = listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("Application updated with translations. Proceeding to dispatch."));
    assertTrue(found);
  }

}
