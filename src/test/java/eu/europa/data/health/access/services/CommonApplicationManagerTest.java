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

import static eu.europa.data.health.access.Constants.APPLICATIONID;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.BASKET_COLLECTION;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_AFFILIATION;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_BUSINESS_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_JOB_TITLE;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_OPERATOR_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_ORGANISATION_NAME;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_EMAIL;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_NAME;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_AFFILIATION;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_JOB_TITLE;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_JOB_TITLE;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_AFFILIATION;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_JOB_TITLE;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_NUMBER;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class CommonApplicationManagerTest {

  @Mock
  private MongoClient mongoClient;
  private RoutingContext routingContext;
  @Mock
  private HubStoreClient hubStoreClient;
  @Mock
  private AuditLogManager auditLogManager;
  @Mock
  private DBHandler dbHandler;
  @Mock
  private BasketManager basketManager;
  @Mock
  private Vertx vertx;
  @Mock
  private HttpServerResponse response;
  @Mock
  private HttpServerRequest request;
  @Captor
  private ArgumentCaptor<JsonObject> jsonCaptor;

  private CommonApplicationManager commonApplicationManager;
  private final String ecasId = "n3817490";

  @BeforeEach
  void setUp() {
    commonApplicationManager = new CommonApplicationManager(dbHandler, basketManager, mongoClient,
      vertx, hubStoreClient, auditLogManager) {
    };
    routingContext = mock(RoutingContext.class);

    User mockUser = Mockito.mock(User.class);
    lenient().when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    lenient().when(routingContext.user().principal()).thenReturn(principal);

    // Mock other behaviors
    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
    lenient().when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    RequestBody requestBody = mock(RequestBody.class);
    lenient().when(routingContext.body()).thenReturn(requestBody);

  }


  @Test
  void performStatusUpdate_updatesStatusAndHistoryCorrectly() {

    JsonObject mongoApplication = new JsonObject().put("status",
      new JsonObject().put("status", "DRAFT"));
    JsonObject newStatus = new JsonObject().put("status", "SUBMITTED");

    commonApplicationManager.performStatusUpdate(mongoApplication, newStatus, routingContext);

    JsonObject updatedStatus = mongoApplication.getJsonObject("status");
    assertEquals("SUBMITTED", updatedStatus.getString("status"));
    assertNotNull(updatedStatus.getJsonArray("status_history"));
    assertEquals("DRAFT",
      updatedStatus.getJsonArray("status_history").getJsonObject(0).getString("status"));
  }

  @Test
  void deleteApplicationById_deletesApplicationWhenNotSubmitted() {
    JsonObject form = new JsonObject();
    form.put("id", "app123");
    JsonObject application = new JsonObject()
      .put("form", form)
      .put("status", new JsonObject().put("status", "DRAFT"));
    when(routingContext.pathParam("id")).thenReturn("applicationId");
    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(application));
      return null;
    }).when(mongoClient).findOne(anyString(), any(), any(), any());

    when(dbHandler.deleteById(eq("testCollection"), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteApplicationById(routingContext, "testCollection");

    verify(dbHandler, times(1)).deleteById(eq("testCollection"), any(JsonObject.class));
  }

  @Test
  void deleteApplicationById_failsToDeleteWhenSubmitted() {
    JsonObject application = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));
    when(routingContext.pathParam("id")).thenReturn("app123");

    JsonObject expectedQuery = new JsonObject()
      .put("_id", "app123")
      .put("userId", "n3817490");
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), eq(expectedQuery), isNull(),
      any())).thenAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(application));
      return null;
    });

    commonApplicationManager.deleteApplicationById(routingContext,
      DATA_REQUEST_APPLICATION_COLLECTION);

    verify(mongoClient, never()).removeDocument(eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(JsonObject.class), any());
  }

  @Test
  void getData_retrievesFileWhenApplicationExists() {
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");
    when(routingContext.pathParam("data_id")).thenReturn("data_id");

    Promise<Boolean> mockPromise = Promise.promise();
    mockPromise.complete(true);
    when(dbHandler.checkDocumentExists(any(), any())).thenReturn(mockPromise);

    HttpResponse<Buffer> mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap()
      .add("content-disposition", "attachment; filename=testfile.txt"));
    when(mockHttpResponse.bodyAsBuffer()).thenReturn(Buffer.buffer("file content"));

    when(hubStoreClient.getFile(eq("data_id"), eq("applicationId"), eq("app123"))).thenReturn(
      Future.succeededFuture(mockHttpResponse));

    HttpServerResponse mockResponse = Mockito.mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.end(any(Buffer.class))).thenReturn(Future.succeededFuture());

    commonApplicationManager.getData(routingContext, "testCollection");

    verify(mockResponse, times(1)).putHeader("Content-Type", "application/octet-stream");
    verify(mockResponse, times(1)).putHeader("Content-Disposition",
      "attachment; filename=testfile.txt");
    verify(mockResponse, times(1)).end(Buffer.buffer("file content"));
  }

  @Test
  void testGetFileData() {
    RoutingContext routingContext = mock(RoutingContext.class);
    when(routingContext.pathParam("data_id")).thenReturn("file123");

    HttpResponse<Buffer> mockResponse = mock(HttpResponse.class);
    when(hubStoreClient.getFile(anyString(), anyString(), anyString()))
      .thenReturn(Future.succeededFuture(mockResponse));

    MultiMap mockHeaders = mock(MultiMap.class);
    when(mockResponse.headers()).thenReturn(mockHeaders);
    when(mockHeaders.get("content-disposition")).thenReturn("attachment; filename=file123");

    Buffer mockBuffer = Buffer.buffer("File content here");
    when(mockResponse.bodyAsBuffer()).thenReturn(mockBuffer);

    HttpServerResponse responce = Mockito.mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(responce);
    when(responce.putHeader(anyString(), anyString())).thenReturn(responce);
    when(responce.end(any(Buffer.class))).thenReturn(Future.succeededFuture());

    commonApplicationManager.getFileData(routingContext, "app123");

    verify(routingContext.response()).putHeader(eq("Content-Type"), eq("application/octet-stream"));
    verify(routingContext.response()).putHeader(eq("Content-Disposition"),
      eq("attachment; filename=file123"));
    verify(routingContext.response()).end(eq(mockBuffer));
  }

  @Test
  void createApplication_savesApplicationWithValidData() {
    User mockUser = Mockito.mock(User.class);
    Mockito.when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    Mockito.when(routingContext.user().principal()).thenReturn(principal);
    JsonObject body = new JsonObject()
      .put(DATASETS, new JsonArray().add(
        new JsonObject()
          .put("dataset_id", "ds1")
          .put("distributions", new JsonArray().add(
            new JsonObject()
              .put(DISTRIBUTION_ID, "dist1")
              .put("publisher", new JsonObject().put("name", "Test Publisher"))))
      ))
      .put("id", "appId")
      .put("title", "Test App Title");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    when(mongoClient.findOne(eq(BASKET_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put("publisher", "Test Publisher")));

    when(dbHandler.create(eq("testCollection"), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("newEntityId"));

    lenient().doNothing().when(auditLogManager)
      .insertAuditLog(anyString(), any(), anyString(), any(), anyBoolean(), any());

    commonApplicationManager.createApplication(routingContext, "testCollection");

    verify(mongoClient, times(1)).findOne(eq(BASKET_COLLECTION), any(JsonObject.class), isNull());
    verify(dbHandler, times(1)).create(eq("testCollection"), any(JsonObject.class));

    verify(auditLogManager, times(1))
      .insertAuditLog(eq("Data Request Application"), any(), eq("Data Request application created"),
        any(), eq(true), eq(routingContext));

  }

  @Test
  void testDeleteSection6ByDataset_withNullForm() {
    JsonObject mongoApp = new JsonObject();
    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");
    assertNull(mongoApp.getJsonObject("form"));
  }

  @Test
  void testDeleteSection6ByDataset_withNullSection6() {
    JsonObject mongoApp = new JsonObject().put("form", new JsonObject());
    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");
    assertNull(mongoApp.getJsonObject("form").getJsonArray("section6"));
  }

  @Test
  void testDeleteSection6ByDataset_withNoMatchingCatalogId() {
    JsonArray section6 = new JsonArray().add(
      new JsonObject().put("catalog_ids", new JsonArray().add("catalog-999")));
    JsonObject form = new JsonObject().put("section6", section6);
    JsonObject mongoApp = new JsonObject().put("form", form);

    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");

    JsonArray resultSection6 = mongoApp.getJsonObject("form").getJsonArray("section6");
    assertEquals(1, resultSection6.size());
  }

  @Test
  void testDeleteSection6ByDataset_withMatchingCatalogId() {
    JsonArray section6 = new JsonArray()
      .add(new JsonObject().put("catalog_ids", new JsonArray().add("catalog-123")))
      .add(new JsonObject().put("catalog_ids", new JsonArray().add("catalog-456")));

    JsonObject form = new JsonObject().put("section6", section6);
    JsonObject mongoApp = new JsonObject().put("form", form);

    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");

    JsonArray resultSection6 = mongoApp.getJsonObject("form").getJsonArray("section6");
    assertEquals(1, resultSection6.size());
    assertTrue(resultSection6.getJsonObject(0).getJsonArray("catalog_ids").contains("catalog-456"));
  }

  @Test
  void testPostData_success_callsHandleFileUpload() {
    String applicationId = "app-123";
    String userId = "n3817490";
    String collectionName = "testCollection";

    JsonObject statusObject = new JsonObject().put("status", "DRAFT");
    JsonObject application = new JsonObject().put("status", statusObject);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    JsonObject expectedQuery = new JsonObject()
      .put(ID, applicationId)
      .put(USER_ID, userId);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(application));
      return null;
    }).when(mongoClient).findOne(eq(collectionName), eq(expectedQuery), isNull(), any());

    CommonApplicationManager managerSpy = spy(commonApplicationManager);

    doNothing().when(managerSpy).handleFileUpload(
      any(), anyString(), anyString(), any(), any()
    );

    managerSpy.postData(routingContext, collectionName);

    verify(managerSpy).handleFileUpload(routingContext, "applicationId", applicationId,
      hubStoreClient, vertx);
  }

  @Test
  void testHasElementWithValue_returnsTrue_whenValueExists() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "match"))
      .add(new JsonObject().put("key", "other"));

    boolean result = commonApplicationManager.hasElementWithValue(array, "key", "match");

    assertTrue(result);
  }

  @Test
  void testHasElementWithValue_returnsFalse_whenValueDoesNotExist() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "nope"))
      .add(new JsonObject().put("key", "nothing"));

    boolean result = commonApplicationManager.hasElementWithValue(array, "key", "match");

    assertFalse(result);
  }

  @Test
  void testUpdateDatasetDateAdded_callsPrepareDataset_whenMatchingIdExists() {
    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put("dataset_id", "ds1"))
      .add(new JsonObject().put("dataset_id", "ds2"));

    CommonApplicationManager manager = spy(
      new CommonApplicationManager(null, null, null, null, null, null) {
      });

    manager.updateDatasetDateAdded(datasets, "ds2", "collectionName");

    verify(manager).prepareDataset(datasets.getJsonObject(1), "collectionName");
  }

  @Test
  void testPrepareDataset_withJsonObject_setsDateAdded() {
    JsonObject input = new JsonObject().put("name", "dataset-1");

    JsonObject result = commonApplicationManager.prepareDataset(input, "testCollection");

    assertNotNull(result);
    assertEquals("dataset-1", result.getString("name"));
    assertNotNull(result.getInstant("date_added"));
  }

  @Test
  void testPrepareDataset_withInvalidObject_returnsNull() {
    Object invalidInput = "this is not a JsonObject";

    JsonObject result = commonApplicationManager.prepareDataset(invalidInput, "testCollection");

    assertNull(result);
  }

  @Test
  void prepareDataset_validJsonObject_addsDateAdded() {
    JsonObject dataset = new JsonObject().put("key", "value");

    JsonObject result = commonApplicationManager.prepareDataset(dataset, "testCollection");

    assertNotNull(result);
    assertEquals("value", result.getString("key"));
    assertNotNull(result.getInstant("date_added"));
  }

  @Test
  void prepareDataset_invalidObject_returnsNull() {
    Object dataset = 42;

    JsonObject result = commonApplicationManager.prepareDataset(dataset, "testCollection");

    assertNull(result);
  }

  @Test
  void deleteFile_success_setsOkResponse() {
    String applicationId = "app-123";
    String fileId = "file-456";

    when(routingContext.pathParam("data_id")).thenReturn(fileId);

    when(hubStoreClient.deleteFile(fileId, "applicationId", applicationId))
      .thenReturn(Future.succeededFuture());

    // Call the method
    commonApplicationManager.deleteFile(routingContext, applicationId);
    verify(response).setStatusCode(eq(200));
    verify(response).end(contains("\"File with id: " + fileId + " was deleted successfully"));
  }


  @Test
  void deleteFile_failure_setsInternalServerErrorResponse() {
    String applicationId = "app-123";
    String fileId = "file-456";
    String errorMessage = "Error deleting file";

    when(routingContext.pathParam("data_id")).thenReturn(fileId);

    when(hubStoreClient.deleteFile(fileId, "applicationId", applicationId))
      .thenReturn(Future.failedFuture(new RuntimeException(errorMessage)));

    commonApplicationManager.deleteFile(routingContext, applicationId);
    verify(response).setStatusCode(eq(500));
    verify(response).end(contains("Error deleting file: " + errorMessage));
  }

  @Test
  void deleteData_success_deletesFile() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject mongoApplication = new JsonObject().put("status",
      new JsonObject().put("status", "DRAFT"));
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    String fileId = "file-456";
    when(routingContext.pathParam("data_id")).thenReturn(fileId);
    when(hubStoreClient.deleteFile(fileId, "applicationId", applicationId))
      .thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("File with id: " + fileId + " was deleted successfully"));
  }

  @Test
  void deleteData_applicationNotFound_setsNotFoundResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(null));
        return null;
      });

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(contains("Application with id: " + applicationId + " was not found"));
  }


  @Test
  void deleteData_submissionStatus_setsBadRequestResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(
      contains("Deletion of data can not be executed in a submitted application"));
  }

  @Test
  void deleteData_failure_setsInternalServerErrorResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.failedFuture(new RuntimeException("Database error")));
        return null;
      });

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("Database error"));
  }

  @Test
  void requestCreateOrUpdateSection_success_section_3() {
    createOrUpdateSection_success_updates("3", DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void accessCreateOrUpdateSection_success_section_3() {
    createOrUpdateSection_success_updates("3", DATA_ACCESS_APPLICATION_COLLECTION);
  }

  @Test
  void requestCreateOrUpdateSection_success_section_4() {
    createOrUpdateSection_success_updates("4", DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void accessCreateOrUpdateSection_success_section_5() {
    createOrUpdateSection_success_updates("5", DATA_ACCESS_APPLICATION_COLLECTION);
  }

  void createOrUpdateSection_success_updates(String sectionNumber, String collectionName) {
    String applicationId = "app-123";

    JsonObject section3 = new JsonObject();
    section3.put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "b"));
    section3.put(NATURAL_PERSON_NAME, "name");
    section3.put(NATURAL_PERSON_ADDRESS, "address");
    section3.put(NATURAL_PERSON_PHONE, new JsonObject());
    section3.put(NATURAL_PERSON_EMAIL, "email");
    JsonObject mongoApplication = new JsonObject()
      .put("form", new JsonObject().put("section3", section3))
      .put("status", new JsonObject().put("status", "DRAFT"));

    JsonObject query = new JsonObject()
      .put(ID, applicationId)
      .put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn(sectionNumber);

    when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(new JsonObject());
    when(routingContext.get(ERRORS)).thenReturn(null);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    when(mongoClient.updateCollection(eq(collectionName), eq(query), any(JsonObject.class), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
        MongoClientUpdateResult result = mock(MongoClientUpdateResult.class);
        when(result.getDocMatched()).thenReturn(1L);
        when(result.getDocModified()).thenReturn(1L);
        handler.handle(Future.succeededFuture(result));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains(
      "Section section" + sectionNumber + " has been stored in application with id: "
        + applicationId));
  }

  @Test
  void createOrUpdateSection_applicationNotFound_setsNotFoundResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";

    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(null));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(contains("Application with id: " + applicationId + " was not found"));
  }

  @Test
  void createOrUpdateSection_statusSubmitted_setsBadRequest() {
    String applicationId = "app-123";
    String collectionName = "testCollection";

    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));

    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(
      contains("Creation or update of a section can not be executed in a submitted application"));
  }

  @Test
  void createOrUpdateSection_failure_setsInternalServerErrorResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";

    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.failedFuture(new RuntimeException("DB down")));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("DB down"));
  }

  @Test
  void sameAsSection3Array_fillsFieldsCorrectly_forLegalPerson() {
    JsonArray sectionFormArray = new JsonArray()
      .add(new JsonObject()
        .put(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, true));

    JsonObject section3 = new JsonObject()
      .put(LEGAL_PERSON_NAME, "Company X")
      .put(CONTACT_PERSON_EMAIL, "contact@companyx.com")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("prefix", "+32").put("number", "123456"))
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "a"));

    JsonArray result = commonApplicationManager.sameAsSection3Array(sectionFormArray, section3);
    JsonObject resultItem = result.getJsonObject(0);

    assertEquals("Company X", resultItem.getString(INFORMATION_PROVIDER_NAME));
    assertEquals("contact@companyx.com", resultItem.getString(INFORMATION_PROVIDER_EMAIL));
    assertEquals("+32", resultItem.getJsonObject(INFORMATION_PROVIDER_PHONE).getString("prefix"));
    assertEquals("123456",
      resultItem.getJsonObject(INFORMATION_PROVIDER_PHONE).getString("number"));
  }

  @Test
  void populateSection4_setsFieldsCorrectly_forLegalPerson() {
    JsonObject sectionForm = new JsonObject();
    JsonObject section3 = new JsonObject()
      .put(LEGAL_PERSON_NAME, "Company X")
      .put(LEGAL_PERSON_ADDRESS, "Legal Address")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("prefix", "+32").put("number", "123456"))
      .put(CONTACT_PERSON_EMAIL, "contact@companyx.com")
      .put(CONTACT_PERSON_OPERATOR_ID, "OP123")
      .put(CONTACT_PERSON_ORGANISATION_NAME, "OrgName")
      .put(CONTACT_PERSON_BUSINESS_ID, "BIZ789");

    commonApplicationManager.populateSection4(sectionForm, section3, "a");

    assertEquals("Company X", sectionForm.getString("fullName"));
    assertEquals("Legal Address", sectionForm.getString("address"));
    assertEquals("contact@companyx.com", sectionForm.getString("email"));
    assertEquals("OP123", sectionForm.getString("operatorIdentifier"));
    assertEquals("OrgName", sectionForm.getString("nameOfTheOrganisation"));
    assertEquals("BIZ789", sectionForm.getString("businessIdentifierOrganization"));

    JsonObject phone = sectionForm.getJsonObject("phone");
    assertEquals("+32", phone.getString("prefix"));
    assertEquals("123456", phone.getString("number"));
  }

  @Test
  void populateSection4_setsFieldsCorrectly_forNaturalPerson() {
    JsonObject sectionForm = new JsonObject();
    JsonObject section3 = new JsonObject()
      .put(NATURAL_PERSON_NAME, "John Doe")
      .put(NATURAL_PERSON_ADDRESS, "Natural Address")
      .put(NATURAL_PERSON_PHONE, new JsonObject().put("prefix", "+44").put("number", "987654"))
      .put(NATURAL_PERSON_EMAIL, "john.doe@example.com");

    commonApplicationManager.populateSection4(sectionForm, section3, "b");

    assertEquals("John Doe", sectionForm.getString("fullName"));
    assertEquals("Natural Address", sectionForm.getString("address"));
    assertEquals("john.doe@example.com", sectionForm.getString("email"));

    JsonObject phone = sectionForm.getJsonObject("phone");
    assertEquals("+44", phone.getString("prefix"));
    assertEquals("987654", phone.getString("number"));
  }

  @Test
  void populateSection5_setsFieldsCorrectly_forLegalPerson_whenBothFlagsTrue() {
    JsonObject sectionForm = new JsonObject()
      .put(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, true)
      .put(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, true);

    JsonObject section3 = new JsonObject()
      .put(LEGAL_PERSON_NAME, "Jane Smith")
      .put(CONTACT_PERSON_JOB_TITLE, "Legal Advisor")
      .put(CONTACT_PERSON_AFFILIATION, "Legal Dept");

    commonApplicationManager.populateSection5(sectionForm, section3, "a");

    // Responsible person fields
    assertEquals("Jane Smith", sectionForm.getString(PERSON_RESPONSIBLE_NAME));
    assertEquals("Legal Advisor", sectionForm.getString(PERSON_RESPONSIBLE_JOB_TITLE));
    assertEquals("Legal Dept", sectionForm.getString(PERSON_RESPONSIBLE_AFFILIATION));

    // Research person fields
    assertEquals("Jane Smith", sectionForm.getString(PERSON_RESEARCH_NAME));
    assertEquals("Legal Advisor", sectionForm.getString(PERSON_RESEARCH_JOB_TITLE));
    assertEquals("Legal Dept", sectionForm.getString(PERSON_RESEARCH_AFFILIATION));
  }

  @Test
  void updateSameAsSection3_updatesSection4_forLegalPerson() {
    JsonObject legalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, legalPerson)
      .put(LEGAL_PERSON_NAME, "Legal Name")
      .put(CONTACT_PERSON_EMAIL, "legal@example.com")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("countryCode", "+1").put("number", "123456"));

    JsonObject mongoForm = new JsonObject().put(SECTION_3, section3);

    JsonObject section4 = new JsonObject().put(SAME_AS_CONTACT_PERSON, true);
    JsonObject updateForm = new JsonObject().put("form.section4", section4);

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 4, true);

    assertEquals("Legal Name", section4.getString("fullName"));
    assertEquals("legal@example.com", section4.getString("email"));
  }

  @Test
  void updateSameAsSection3_updatesSection5_forNaturalPerson() {
    JsonObject legalPerson = new JsonObject().put("key", "b");
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, legalPerson)
      .put(NATURAL_PERSON_NAME, "Natural Name")
      .put(NATURAL_PERSON_JOB_TITLE, "Researcher")
      .put(NATURAL_PERSON_AFFILIATION, "BioDept");

    JsonObject mongoForm = new JsonObject().put(SECTION_3, section3);

    JsonObject section5 = new JsonObject()
      .put(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, true)
      .put(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, true);

    JsonObject updateForm = new JsonObject().put("form.section5", section5);

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 5, true);

    assertEquals("Natural Name", section5.getString(PERSON_RESPONSIBLE_NAME));
    assertEquals("Researcher", section5.getString(PERSON_RESPONSIBLE_JOB_TITLE));
    assertEquals("BioDept", section5.getString(PERSON_RESPONSIBLE_AFFILIATION));

    assertEquals("Natural Name", section5.getString(PERSON_RESEARCH_NAME));
  }

  @Test
  void updateSameAsSection3_updatesSection6_forLegalPerson() {
    JsonObject legalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, legalPerson)
      .put(LEGAL_PERSON_NAME, "Legal Provider")
      .put(CONTACT_PERSON_EMAIL, "info@legal.com")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("countryCode", "+33").put("number", "7890"));

    JsonObject mongoForm = new JsonObject().put(SECTION_3, section3);

    JsonObject country = new JsonObject().put(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, true);
    JsonArray section6 = new JsonArray().add(country);
    JsonObject updateForm = new JsonObject().put("form.section6", section6);

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 6, true);

    JsonObject updatedCountry = section6.getJsonObject(0);
    assertEquals("Legal Provider", updatedCountry.getString("informationProviderName"));
    assertEquals("info@legal.com", updatedCountry.getString("informationProviderEmail"));
    assertEquals("+33",
      updatedCountry.getJsonObject("informationProviderPhone").getString("countryCode"));
  }

  @Test
  void updateSameAsSection3_updatesSection8_forSameAsContactPerson() {
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "b"))
      .put("naturalPersonName", "John Doe")
      .put("naturalPersonAffiliation", "University A")
      .put("naturalPersonEmail", "john.doe@university.com");

    JsonObject mongoForm = new JsonObject().put("section3", section3);

    JsonObject section8 = new JsonObject().put("sameAsContactPerson", true);
    JsonObject updateForm = new JsonObject().put("form.section8", section8);

    // When
    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 8, true);

    JsonArray peopleArray = section8.getJsonArray("peopleWhoWillBeProcessingTheData");
    assertNotNull(peopleArray);
    assertEquals(1, peopleArray.size());

    JsonObject person = peopleArray.getJsonObject(0);
    assertEquals("John Doe", person.getString("fullName"));
    assertEquals("University A", person.getString("affiliation"));
    assertEquals("john.doe@university.com", person.getString("emailAddress"));
  }

  @Test
  void updateSameAsSection3_doesNothing_whenSection3IsNull() {
    JsonObject mongoForm = new JsonObject();
    JsonObject updateForm = new JsonObject().put("form.section4", new JsonObject());

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 4, true);

    assertTrue(true);
  }

  @Test
  void getSectionErrors_shouldStoreSection_whenNoCriticalErrors_andSectionNot6() {
    JsonObject body = new JsonObject().put("field", "value");

    when(routingContext.get(ERRORS)).thenReturn(List.of());
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("4");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("1234");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section section4 has been stored"));
  }

  @Test
  void getSectionErrors_shouldStoreSection6_whenNoCriticalErrors_andBodyIsArray() {
    JsonArray bodyArray = new JsonArray().add(new JsonObject().put("field", "value"));

    when(routingContext.get(ERRORS)).thenReturn(List.of());
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("6");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("5678");
    when(routingContext.body().asJsonArray()).thenReturn(bodyArray);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section section6 has been stored"));
  }

  @Test
  void getSectionErrors_shouldReturnBadRequest_whenCriticalErrorsExist() {
    FieldError criticalError = new FieldError("someField", "NON_REQUIRED_CODE");

    when(routingContext.get(ERRORS)).thenReturn(List.of(criticalError));

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(contains("Section not stored due to validation errors"));
  }

  @Test
  void getSectionErrors_shouldStoreSection_whenOnlyRequiredFieldErrors() {
    FieldError requiredError = new FieldError("field", "REQUIRED");

    when(routingContext.get(ERRORS)).thenReturn(List.of(requiredError));

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end(contains("Section not stored due to validation errors"));
  }

  @Test
  void getSectionErrors_shouldStoreSection_whenNoErrorsListPresent() {
    when(routingContext.get(ERRORS)).thenReturn(null);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("3");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("1234");

    JsonObject body = new JsonObject().put("someKey", "someValue");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section section3 has been stored"));
  }

  @Test
  void getSectionErrors_shouldHandleUnexpectedSectionNumberGracefully() {
    when(routingContext.get(ERRORS)).thenReturn(null);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("foo");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("1234");

    JsonObject body = new JsonObject().put("field", "val");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section sectionfoo has been stored"));
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection6_DARA_ACCESS() {
    JsonArray mockBody = new JsonArray();
    JsonObject mockBodyObject = new JsonObject()
      .put("id", "file1");
    mockBody.add(mockBodyObject.put("consentAndInformationLetterAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    mockBody.add(mockBodyObject.put("permitDecisionAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    mockBody.add(mockBodyObject.put("ethicalReviewAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    mockBody.add(mockBodyObject.put("variablesToBeUsedInDataExtractionAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    validateFileBeforeUpdate_shouldValidateFileForSection("6",
      DATA_ACCESS_APPLICATION_COLLECTION, null, mockBody);
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection8_DATA_ACCESS() {
    Map<String, Object> fields = Map.of(
      "attachResearchPermitIfRequired", new JsonArray().add(new JsonObject()
        .put("id", "file1").put("name", "file1.txt")));
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("8",
      DATA_ACCESS_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection9_DATA_ACCESS() {
    Map<String, Object> fields = Map.of(
      "additionalAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt")));
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("9",
      DATA_ACCESS_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection5_REQUEST_ACCESS() {
    Map<String, Object> fields = Map.of(
      "summaryOfPlanForUsingTheData",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt")),
      "summaryOfResearchPlan",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))
    );
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("5",
      DATA_REQUEST_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection5_DATA_REQUEST() {
    Map<String, Object> fields = Map.of(
      "summaryOfPlanForUsingTheData",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt")),
      "summaryOfResearchPlan",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))
    );
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("5", DATA_REQUEST_APPLICATION_COLLECTION,
      mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection6_REQUEST_ACCESS() {
    JsonObject nestedFile = new JsonObject()
      .put("tabulationPlanArray",
        new JsonArray().add(new JsonObject()
          .put("tabulationPlan", new JsonObject().put("id", "tabulationPlanId"))
          .put("id", "file1")
          .put("name", "file1.txt")));
    JsonArray nestedArray = new JsonArray().add(nestedFile);
    JsonObject arrayItem = new JsonObject()
      .put("tabulationPlanArray", nestedArray);
    validateFileBeforeUpdate_shouldValidateFileForSection("6",
      DATA_REQUEST_APPLICATION_COLLECTION, arrayItem, nestedArray);
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection7_REQUEST_ACCESS() {
    Map<String, Object> fields = Map.of(
      "attachmentOfPermitDocuments",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))
    );
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("7",
      DATA_REQUEST_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  void validateFileBeforeUpdate_shouldValidateFileForSection(String sectionNumber,
    String collectionName, JsonObject mockBody, JsonArray mockBodyArray) {
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn(sectionNumber);
    lenient().when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");

    lenient().when(routingContext.body().asJsonObject()).thenReturn(mockBody);
    lenient().when(routingContext.body().asJsonArray()).thenReturn(mockBodyArray);

    HttpResponse<Buffer> mockHttpResponse = mock(HttpResponse.class);
    lenient().when(mockHttpResponse.getHeader("filename")).thenReturn("file1.txt");

    lenient().when(hubStoreClient.getFile(eq("tabulationPlanId"), eq(APPLICATIONID), eq("app123")))
      .thenReturn(Future.succeededFuture(mockHttpResponse));

    commonApplicationManager = spy(commonApplicationManager);

    commonApplicationManager.validateFileBeforeUpdate(routingContext,
      collectionName);

    verify(routingContext, times(1)).next();
  }

  @Test
  void cleanSection2_shouldRemoveOtherFieldWhenTheResearchFocusesOnTheFollowingObjectivesDoesNotContainI() {
    JsonObject section2 = new JsonObject();
    JsonArray objectivesArray = new JsonArray().add(new JsonObject().put("key", "a"));
    section2.put("theResearchFocusesOnTheFollowingObjectives", objectivesArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    boolean containsKey = section2.containsKey("theResearchFocusesOnTheFollowingObjectivesOther");
    assertFalse(containsKey, "theResearchFocusesOnTheFollowingObjectivesOther should be removed");
  }

  @Test
  void cleanSection2_shouldNotRemoveOtherFieldWhenTheResearchFocusesOnTheFollowingObjectivesContainsI() {
    JsonObject section2 = new JsonObject();
    JsonArray objectivesArray = new JsonArray().add(new JsonObject().put("key", "i"));
    section2.put("theResearchFocusesOnTheFollowingObjectives", objectivesArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertTrue(section2.containsKey("theResearchFocusesOnTheFollowingObjectives"));
  }

  @Test
  void cleanSection2_shouldRemoveAreaOfResearchOtherFieldWhenAreaOfResearchDoesNotContainAj() {
    JsonObject section2 = new JsonObject();
    JsonArray areaOfResearchArray = new JsonArray().add(new JsonObject().put("key", "b"));
    section2.put("areaOfResearch", areaOfResearchArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(section2.containsKey("areaOfResearchOther"));
  }

  @Test
  void cleanSection2_shouldNotRemoveAreaOfResearchOtherFieldWhenAreaOfResearchContainsAj() {
    JsonObject section2 = new JsonObject();
    JsonArray areaOfResearchArray = new JsonArray().add(new JsonObject().put("key", "aj"));
    section2.put("areaOfResearch", areaOfResearchArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertTrue(section2.containsKey("areaOfResearch"));
  }

  @Test
  void cleanSection2_shouldRemoveDescriptionOfTheProjectIfDescriptionNotChecked() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION, false);
    section2.put("descriptionOfTheProject", "Some description");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(section2.containsKey("descriptionOfTheProject"));
  }

  @Test
  void cleanSection2_shouldRemoveDescriptionOfTheDataYouWillUseIfDescriptionChecked() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION, false);
    section2.put("descriptionOfTheProject", "Some description");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(section2.containsKey("descriptionOfTheProject"));
  }

  @Test
  void cleanSection2_shouldRemoveSummaryOfTheProjectIfSummaryNotChecked() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, false);
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON,
      "Some reason");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(section2.containsKey("Some reason"));
  }

  @Test
  void cleanSection2_shouldRemoveSummaryOfTheProjectReasonIfSummaryChecked() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, true);
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON,
      "Some reason");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(
      section2.containsKey("THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON"));
  }

  @Test
  void cleanSection2_shouldUpdateSection2InMongoApplicationForm() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION, false);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertTrue(mongoApplicationForm.containsKey("section2"));
    assertEquals(section2, mongoApplicationForm.getJsonObject("section2"));
  }

  @Test
  void cleanSection3_shouldRemoveApplyingForDataForCarryingOutTasksIfKeyNotA() {
    JsonObject section3 = new JsonObject();
    JsonObject applyingForDataOnBehalfOfPublicSector = new JsonObject();
    applyingForDataOnBehalfOfPublicSector.put("key", "b");
    section3.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
      applyingForDataOnBehalfOfPublicSector);

    JsonObject legalPerson = new JsonObject();
    section3.put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS));
  }

  @Test
  void cleanSection3_shouldNotRemoveApplyingForDataForCarryingOutTasksIfKeyIsA() {
    JsonObject section3 = new JsonObject();
    JsonObject applyingForDataOnBehalfOfPublicSector = new JsonObject();
    applyingForDataOnBehalfOfPublicSector.put("key", "b");
    section3.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
      applyingForDataOnBehalfOfPublicSector);
    section3.put(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS, "Some text");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS));
  }

  @Test
  void cleanSection3_shouldRemoveLegalPersonFieldsIfKeyIsB() {
    JsonObject section3 = new JsonObject();
    JsonObject legalPerson = new JsonObject();
    legalPerson.put("key", "b");
    section3.put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(LEGAL_PERSON_NAME));
    assertFalse(section3.containsKey(LEGAL_PERSON_ADDRESS));
    assertFalse(section3.containsKey("legalPersonZipCode"));
    assertFalse(section3.containsKey("legalPersonCity"));
    assertFalse(section3.containsKey(LEGAL_PERSON_COUNTRY));
    assertFalse(section3.containsKey("contactPersonName"));
    assertFalse(section3.containsKey(CONTACT_PERSON_EMAIL));
    assertFalse(section3.containsKey(CONTACT_PERSON_PHONE));
    assertFalse(section3.containsKey(CONTACT_PERSON_ORGANISATION_NAME));
    assertFalse(section3.containsKey(CONTACT_PERSON_BUSINESS_ID));
    assertFalse(section3.containsKey(CONTACT_PERSON_OPERATOR_ID));
    assertFalse(section3.containsKey(CONTACT_PERSON_JOB_TITLE));
    assertFalse(section3.containsKey(CONTACT_PERSON_AFFILIATION));
    assertFalse(section3.containsKey("contactPersonRelationship"));
  }

  @Test
  void cleanSection3_shouldRemoveNaturalPersonFieldsIfKeyIsA() {
    JsonObject section3 = new JsonObject();
    JsonObject legalPerson = new JsonObject();
    legalPerson.put("key", "a");
    section3.put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(NATURAL_PERSON_NAME));
    assertFalse(section3.containsKey(NATURAL_PERSON_ADDRESS));
    assertFalse(section3.containsKey("naturalPersonZipCode"));
    assertFalse(section3.containsKey("naturalPersonCity"));
    assertFalse(section3.containsKey("naturalPersonCountry"));
    assertFalse(section3.containsKey(NATURAL_PERSON_EMAIL));
    assertFalse(section3.containsKey(NATURAL_PERSON_PHONE));
    assertFalse(section3.containsKey(NATURAL_PERSON_JOB_TITLE));
    assertFalse(section3.containsKey(NATURAL_PERSON_AFFILIATION));
  }

  @Test
  void cleanSection3_shouldPutModifiedSection3BackIntoMongoApplicationForm() {
    JsonObject section3 = new JsonObject();
    JsonObject applyingForDataOnBehalfOfPublicSector = new JsonObject();
    applyingForDataOnBehalfOfPublicSector.put("key", "b");
    section3.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
      applyingForDataOnBehalfOfPublicSector);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertTrue(mongoApplicationForm.containsKey(SECTION_3));
    assertEquals(section3, mongoApplicationForm.getJsonObject(SECTION_3));
  }

  @Test
  void cleanSection4_shouldRemoveInvoiceAddressIfInvoiceTypeNotB() {
    JsonObject invoiceType = new JsonObject().put("key", "a");
    JsonObject section4 = new JsonObject()
      .put("invoiceType", invoiceType)
      .put("invoiceAddress", "some address");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    assertFalse(mongoApplicationForm.getJsonObject(SECTION_4).containsKey("invoiceAddress"));
  }

  @Test
  void cleanSection4_shouldNotRemoveInvoiceAddressIfInvoiceTypeIsB() {
    JsonObject invoiceType = new JsonObject().put("key", "b");
    JsonObject section4 = new JsonObject()
      .put("invoiceType", invoiceType)
      .put("invoiceAddress", "some address");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    assertTrue(mongoApplicationForm.getJsonObject(SECTION_4).containsKey("invoiceAddress"));
  }

  @Test
  void cleanSection4_shouldRemoveOrgFieldsIfLegalPersonIsB() {
    JsonObject legalPerson = new JsonObject().put("key", "b");
    JsonObject section3 = new JsonObject().put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject section4 = new JsonObject()
      .put("operatorIdentifier", "op")
      .put("nameOfTheOrganisation", "org")
      .put("businessIdentifierOrganization", "biz")
      .put("vatNumber", "vat")
      .put("peppolCode", "peppol");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_3, section3)
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    JsonObject cleanedSection4 = mongoApplicationForm.getJsonObject(SECTION_4);
    assertFalse(cleanedSection4.containsKey("operatorIdentifier"));
    assertFalse(cleanedSection4.containsKey("nameOfTheOrganisation"));
    assertFalse(cleanedSection4.containsKey("businessIdentifierOrganization"));
    assertFalse(cleanedSection4.containsKey("vatNumber"));
    assertFalse(cleanedSection4.containsKey("peppolCode"));
  }

  @Test
  void cleanSection4_shouldRemoveRangeOfAmountOfFinancingIfCoverageKeyNotA() {
    JsonObject isCovered = new JsonObject().put("key", "b");
    JsonObject section4 = new JsonObject()
      .put("isTheProjectFinanciallyCovered", isCovered)
      .put("rangeOfAmountOfFinancing", "10k-20k");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    assertFalse(
      mongoApplicationForm.getJsonObject(SECTION_4).containsKey("rangeOfAmountOfFinancing"));
  }

  @Test
  void notContainsKey_shouldReturnTrueWhenKeyIsNotPresent() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "a"))
      .add(new JsonObject().put("key", "b"));
    boolean result = commonApplicationManager.notContainsKey(array, "c");
    assertTrue(result);
  }

  @Test
  void notContainsKey_shouldReturnFalseWhenKeyIsPresent() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "x"))
      .add(new JsonObject().put("key", "target"))
      .add(new JsonObject().put("key", "z"));
    boolean result = commonApplicationManager.notContainsKey(array, "target");
    assertFalse(result);
  }

  @Test
  void notContainsKey_shouldReturnTrueWhenArrayIsEmpty() {
    JsonArray array = new JsonArray();
    boolean result = commonApplicationManager.notContainsKey(array, "anything");
    assertTrue(result);
  }

  @Test
  void notContainsKey_shouldReturnTrueWhenKeyFieldIsMissing() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("notKey", "value1"))
      .add(new JsonObject().put("anotherField", "value2"));
    boolean result = commonApplicationManager.notContainsKey(array, "target");
    assertTrue(result);
  }

  @Test
  void sendApplicationForDispatch_shouldCallMongoClientSaveWithCorrectData() {
    JsonObject mockApplication = new JsonObject().put("appId", "123");
    String database = "testDatabase";

    doAnswer(invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(2);
      AsyncResult<String> successResult = mock(AsyncResult.class);
      when(successResult.succeeded()).thenReturn(true);
      when(successResult.result()).thenReturn("generated-id-xyz");
      handler.handle(successResult);
      return null;
    }).when(mongoClient).save(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any());

    commonApplicationManager.sendApplicationForDispatch(mockApplication, database);

    verify(mongoClient).save(eq(DISPATCHER_COLLECTION), jsonCaptor.capture(), any());
    JsonObject savedDoc = jsonCaptor.getValue();

    assertNotNull(savedDoc);
    assertTrue(savedDoc.containsKey("configuration"));
    assertEquals(database,
      savedDoc.getJsonObject("configuration").getString(Constants.APPLICATION_TYPE));
    assertEquals("pending", savedDoc.getJsonObject("configuration").getString("status"));
    assertEquals(0, savedDoc.getJsonObject("configuration").getInteger("retries"));
    assertEquals(mockApplication, savedDoc.getJsonObject("application"));
  }

  @Test
  void sendApplicationForDispatch_shouldLogErrorWhenSaveFails() {

    JsonObject mockApplication = new JsonObject().put("appId", "123");
    String database = "testDatabase";

    doAnswer(invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(2);
      AsyncResult<String> failedResult = mock(AsyncResult.class);
      when(failedResult.succeeded()).thenReturn(false);
      when(failedResult.cause()).thenReturn(new RuntimeException("DB write failed"));
      handler.handle(failedResult);
      return null;
    }).when(mongoClient).save(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any());

    commonApplicationManager.sendApplicationForDispatch(mockApplication, database);

    verify(mongoClient).save(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any());
  }

  @Test
  void changeTitle_shouldRespondBadRequestIfTitleAlreadyExists() {
    String id = "abc123";
    String title = "TestTitle";

    JsonObject requestBody = new JsonObject().put("title", title);
    JsonObject expectedQuery = new JsonObject()
      .put(TITLE, title)
      .put(ID, new JsonObject().put("$ne", id))
      .put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(id);
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.result()).thenReturn(new JsonObject());
      handler.handle(result);
      return null;
    }).when(mongoClient).findOne(eq("testDB"), eq(expectedQuery), isNull(), any());

    commonApplicationManager.changeTitle(routingContext, "testDB");

    verify(routingContext).response();
    verify(routingContext.response()).setStatusCode(400);
  }

  @Test
  void changeTitle_shouldUpdateTitleIfNotExistsAndUpdateSucceeds() {
    String id = "abc123";
    String title = "NewTitle";

    JsonObject requestBody = new JsonObject().put("title", title);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(id);

    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.result()).thenReturn(null);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOne(any(), any(), any(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> updateResult = mock(AsyncResult.class);
      when(updateResult.succeeded()).thenReturn(true);
      handler.handle(updateResult);
      return null;
    }).when(mongoClient).findOneAndUpdate(any(), any(), any(), any());

    commonApplicationManager.changeTitle(routingContext, "testDB");

    verify(mongoClient).findOneAndUpdate(any(), any(), any(), any());
    verify(routingContext).response();
    verify(routingContext.response()).setStatusCode(200);
  }

  @Test
  void changeTitle_shouldReturnBadRequestWhenUpdateFails() {
    String id = "abc123";
    String title = "NewTitle";

    JsonObject requestBody = new JsonObject().put("title", title);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(id);
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.result()).thenReturn(null);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOne(any(), any(), any(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> updateResult = mock(AsyncResult.class);
      when(updateResult.succeeded()).thenReturn(false);
      handler.handle(updateResult);
      return null;
    }).when(mongoClient).findOneAndUpdate(any(), any(), any(), any());

    commonApplicationManager.changeTitle(routingContext, "testDB");

    verify(mongoClient).findOneAndUpdate(any(), any(), any(), any());
    verify(routingContext).response();
    verify(routingContext.response()).setStatusCode(400);
  }

  @Test
  void rollbackStatusToDraft_shouldLogSuccessWhenUpdateSucceeds() {
    String applicationId = "app123";
    String database = "testDB";

    doAnswer(invocation -> {
      Handler<AsyncResult<Void>> handler = invocation.getArgument(3);
      AsyncResult<Void> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(true);
      handler.handle(result);
      return null;
    }).when(mongoClient).updateCollection(
      eq(database), any(JsonObject.class), any(JsonObject.class), any(Handler.class));

    commonApplicationManager.rollbackStatusToDraft(applicationId, database);

    verify(mongoClient).updateCollection(
      eq(database),
      eq(new JsonObject().put("_id", applicationId)),
      eq(new JsonObject().put("$set", new JsonObject().put("status.status", "DRAFT"))),
      any());
  }

  @Test
  void rollbackStatusToDraft_shouldLogFailureWhenUpdateFails() {
    String applicationId = "app123";
    String database = "testDB";

    doAnswer(invocation -> {
      Handler<AsyncResult<Void>> handler = invocation.getArgument(3);

      AsyncResult<Void> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(false);
      when(result.cause()).thenReturn(new RuntimeException("Update failed"));

      handler.handle(result);

      return null;
    }).when(mongoClient).updateCollection(
      eq(database), any(JsonObject.class), any(JsonObject.class), any(Handler.class));

    commonApplicationManager.rollbackStatusToDraft(applicationId, database);

    verify(mongoClient).updateCollection(
      eq(database),
      eq(new JsonObject().put("_id", applicationId)),
      eq(new JsonObject().put("$set", new JsonObject().put("status.status", "DRAFT"))),
      any(Handler.class));
  }

  @Test
  void replaceApplication_shouldCompleteFutureWhenUpdateSucceeds() {
    String collection = "testCollection";
    String id = "app123";
    JsonObject mongoApplication = new JsonObject().put(ID, id);

    JsonObject expectedResult = new JsonObject()
      .put(ID, id);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(true);
      when(result.result()).thenReturn(expectedResult);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOneAndReplace(any(), any(), any(), any());

    Future<JsonObject> future = commonApplicationManager.replaceApplication(mongoApplication,
      collection);

    assertTrue(future.succeeded());
    assertEquals(expectedResult, future.result());

    verify(mongoClient).findOneAndReplace(
      eq(collection),
      eq(new JsonObject().put(ID, id)),
      argThat(doc -> doc.getString(ID).equals(id) && doc.containsKey("date_modified")
      ), any());
  }

  @Test
  void replaceApplication_shouldFailFutureWhenUpdateFails() {
    String collection = "testCollection";
    String id = "app123";
    JsonObject mongoApplication = new JsonObject().put(ID, id);

    RuntimeException cause = new RuntimeException("Replace failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(false);
      when(result.cause()).thenReturn(cause);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOneAndReplace(any(), any(), any(), any());

    Future<JsonObject> future = commonApplicationManager.replaceApplication(mongoApplication,
      collection);

    assertTrue(future.failed());
    assertEquals(cause, future.cause());

    verify(mongoClient).findOneAndReplace(
      eq(collection),
      eq(new JsonObject().put(ID, id)),
      argThat(doc -> doc.getString(ID).equals(id) && doc.containsKey("date_modified")
      ), any());
  }


}
