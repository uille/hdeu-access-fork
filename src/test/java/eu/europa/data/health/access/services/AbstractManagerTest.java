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

import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.containers.MongoDBContainer;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
class AbstractManagerTest {

  private static MongoDBContainer mongoDBContainer;
  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";

  private static MongoClient mongoClient;
  private static DBHandler dbHandler;
  static AbstractManager abstractManager;

  public static final String USER_ID = "n3817490";

  private static final String ABSTRACT_MANAGER_COLLECTION = "abstractManagerCollection";


  @BeforeAll
  static void setUp(VertxTestContext testContext) {
    Vertx vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:4.4.2");
    mongoDBContainer.start();

    // Configure MongoClient with the connection string
    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE));

    mongoClient = MongoClient.createShared(vertx, config);
    dbHandler = new DBHandler(mongoClient);

    abstractManager = new AbstractManager(dbHandler) {
    };

    testContext.completeNow();
  }

  @AfterAll
  static void tearDown(VertxTestContext testContext) {
    try {
      mongoClient.close().onComplete(v -> {
        mongoDBContainer.stop();
        testContext.completeNow();
      });
    } catch (Exception e) {
      testContext.failNow(e);
    }
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCreateEntity(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject().put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue"));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    //Since createEntity() is void, we can't use onSuccess, so we wait for the async response
    //Query the database after calling createEntity to check if the document was inserted
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");
      dbHandler.findById(ABSTRACT_MANAGER_COLLECTION, docId)
        .onSuccess(result -> {
          assertNotNull(result);
          assertNotNull(result.getString("_id"));
          assertEquals(docId, result.getString("_id"));
          assertEquals("abstractManagerTestKey", result.getString("key"));
          assertEquals("abstractManagerTestValue", result.getString("value"));
          testContext.completeNow();
        })
        .onFailure(testContext::failNow);
    });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateEntity(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      // Perform the update
      JsonObject updatedDocument = new JsonObject()
        .put("key", "abstractManagerTestKeyUpdated")
        .put("value", "abstractManagerTestValueUpdate")
        .put("id", docId);
      RoutingContext updateContext = RoutingContextMocker.mockRoutingContext(updatedDocument);
      abstractManager.updateEntity(updateContext, ABSTRACT_MANAGER_COLLECTION);

      vertx.setTimer(1000, timerId2 -> dbHandler.findById(ABSTRACT_MANAGER_COLLECTION, docId)
        .onSuccess(result -> {
          assertNotNull(result);
          assertNotNull(result.getString("_id"));
          assertEquals(docId, result.getString("_id"));
          assertEquals("abstractManagerTestKeyUpdated", result.getString("key"));
          assertEquals("abstractManagerTestValueUpdate", result.getString("value"));
          testContext.completeNow();
        })
        .onFailure(testContext::failNow));
    });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testPatchEntity(Vertx vertx, VertxTestContext testContext) {
    JsonObject initialDocument = new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID);
    dbHandler.create(ABSTRACT_MANAGER_COLLECTION, initialDocument).onSuccess( docId -> {

      // Perform the update
      JsonObject updatedDocument = new JsonObject()
        .put("key", "abstractManagerTestKeyPatched")
        .put("value", "abstractManagerTestValuePatched");

      RoutingContext updateContext = RoutingContextMocker.mockRoutingContext(updatedDocument);
      // Mock the path parameter retrieval
      when(updateContext.pathParam("id")).thenReturn(docId);
      abstractManager.patchEntity(updateContext, ABSTRACT_MANAGER_COLLECTION);

      vertx.setTimer(500, updateContextTimerId -> dbHandler.findById(ABSTRACT_MANAGER_COLLECTION,
          docId).onSuccess(result -> {
            assertNotNull(result);
            assertNotNull(result.getString("_id"));
            assertEquals(docId, result.getString("_id"));
            assertEquals("abstractManagerTestKeyPatched", result.getString("key"));
            assertEquals("abstractManagerTestValuePatched", result.getString("value"));
            testContext.completeNow();
          })
          .onFailure(testContext::failNow));
    });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testGetEntity(Vertx vertx, VertxTestContext testContext) {
    createEntities(10);
    vertx.setTimer(500, timerId -> {
      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject().put("userId", USER_ID));
      Integer pageNumber = 1;
      Integer pageLimit = 10;

      abstractManager.getEntity(context, ABSTRACT_MANAGER_COLLECTION, pageNumber, pageLimit, null, null, null);

      // Wait for the async code to finish
      vertx.setTimer(1000, timerId2 -> {
        String message = context.response().getStatusMessage();
        assertEquals(HttpResponseStatus.OK.code(), context.response().getStatusCode());
        assertTrue(message.contains("data"),"It should contains the content item");
        testContext.completeNow();
      });
    });
  }

  @Test
  void testFindById(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      RoutingContext findContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(findContext.pathParam("id")).thenReturn(docId);
      abstractManager.findById(findContext, ABSTRACT_MANAGER_COLLECTION);
      assertNotNull(findContext.body());
      vertx.setTimer(1000, findTimerId -> {
        String message = findContext.response().getStatusMessage();
        assertNotNull(message, "Item should not be null");
        assertTrue(message.contains(docId), "Response should contain the created document");
        testContext.completeNow();
      });
    });
  }

  @Test
  void testFindByPathParam(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      RoutingContext findContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(findContext.pathParam("pathParam")).thenReturn("value");
      abstractManager.findByPathParam(findContext, ABSTRACT_MANAGER_COLLECTION, "abstractManagerTestValue");
      vertx.setTimer(1000, timerIdFind -> {
        String message = findContext.response().getStatusMessage();
        assertEquals(HttpResponseStatus.OK.code(), findContext.response().getStatusCode());
        assertTrue(message.contains("abstractManagerTestValue"),"It should contains the content item");
        testContext.completeNow();
      });
    });
  }

  @Test
  void testDeleteByPathParam(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      RoutingContext deleteContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(deleteContext.pathParam("pathParam")).thenReturn("value");
      abstractManager.deleteByPathParam(deleteContext, ABSTRACT_MANAGER_COLLECTION, "abstractManagerTestValue");
      vertx.setTimer(1000, timerIdFind -> {
        assertEquals(HttpResponseStatus.NO_CONTENT.code(), deleteContext.response().getStatusCode());
        testContext.completeNow();
        });
    });
  }

  @Test
  void testDeleteById(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
      .put("key", "abstractManagerTestKey").put("value", "abstractManagerTestValue")
      .put("userId", USER_ID));
    abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
    assertNotNull(context.body());
    vertx.setTimer(500, timerId -> {
      JsonObject body = context.body().asJsonObject();

      String docId = body.getString("_id");

      RoutingContext deleteContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(deleteContext.pathParam("id")).thenReturn(docId);
      abstractManager.deleteEntityById(deleteContext, ABSTRACT_MANAGER_COLLECTION);
      vertx.setTimer(1000, timerIdFind -> {
        assertEquals(HttpResponseStatus.OK.code(), deleteContext.response().getStatusCode());
        testContext.completeNow();
      });
    });
  }

  @Test
  void testDeleteAllEntities(Vertx vertx, VertxTestContext testContext) {
    createEntities(11);
    vertx.setTimer(500, timerId -> {
      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject().put("userId", USER_ID));
      abstractManager.deleteAllEntities(context, ABSTRACT_MANAGER_COLLECTION);
      vertx.setTimer(1000, timerIdFind -> {
        assertEquals(HttpResponseStatus.NO_CONTENT.code(), context.response().getStatusCode());
        testContext.completeNow();
      });
    });
  }

  private void createEntities(Integer numberOfEntities){
    for(int i = 0; i < numberOfEntities; i++){
      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject()
        .put("title", "abstractManagerTestKey"+i)
        .put("value", "abstractManagerTestValue"+i)
        .put("userId", USER_ID));
      abstractManager.createEntity(context, ABSTRACT_MANAGER_COLLECTION);
      assertNotNull(context.body());
    }
  }

  @Test
  void testGetAuditLogs_withUserEnrichment(Vertx vertx, VertxTestContext testContext) {
    JsonObject userLog = new JsonObject()
      .put("userId", "ecas_test_1")
      .put("object", "Project")
      .put("action", "UPDATE");

    JsonObject systemLog = new JsonObject()
      .put("userId", "ecas_test_2")
      .put("object", "Dataset Record")
      .put("action", "DELETE");

    // Insert logs
    mongoClient.insert(AUDIT_LOG_COLLECTION, userLog)
      .compose(id -> mongoClient.insert(AUDIT_LOG_COLLECTION, systemLog))
      .onSuccess(res -> {

        JsonArray users = new JsonArray()
          .add(new JsonObject()
            .put("ecasId", "ecas_test_1")
            .put("firstName", "John")
            .put("lastName", "Doe"))
          .add(new JsonObject()
            .put("ecasId", "ecas_test_2")
            .put("firstName", "Jane")
            .put("lastName", "Smith"));

        JsonObject queryOptions = new JsonObject().put("users", users);

        RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(new JsonObject());

        abstractManager.getAuditLogs(routingContext, 1, 10, "timestamp", "desc", queryOptions);

        vertx.setTimer(500, updateContextTimerId -> {
          verify(routingContext.response()).setStatusCode(200);
          verify(routingContext.response()).end(anyString());
          testContext.completeNow();
        });
      }).onFailure(testContext::failNow);
  }

  @Test
  void testHandleFileUpload() {
    Vertx vertxMock = mock(Vertx.class);
    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
    HubStoreClient hubStoreClient = mock(HubStoreClient.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    FileUpload fileUpload = mock(FileUpload.class);
    FileSystem fileSystem = mock(FileSystem.class);

    String contentType = "text/plain";
    String parentName = "parent";
    String parentId = "123";
    String fileName = "test.txt";
    String fileContent = "Test file content";
    Buffer fileBuffer = Buffer.buffer(fileContent);

    when(routingContext.request()).thenReturn(request);
    when(routingContext.fileUploads()).thenReturn(Collections.singletonList(fileUpload));
    when(fileUpload.uploadedFileName()).thenReturn(fileName);
    when(fileUpload.fileName()).thenReturn(fileName);
    when(request.getHeader("Content-Type")).thenReturn(contentType);

    when(vertxMock.fileSystem()).thenReturn(fileSystem);
    when(fileSystem.readFile(fileName)).thenReturn(Future.succeededFuture(fileBuffer));
    Mockito.when(response.putHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(response);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    JsonObject mockResponse = new JsonObject().put("status", "ok").put("fileName", fileName);
    when(hubStoreClient.uploadFile(any(), any(), any(), any(), any())).thenReturn(Future.succeededFuture(mockResponse));

    abstractManager.handleFileUpload(routingContext, parentName, parentId, hubStoreClient, vertxMock);

    verify(response).putHeader("Content-Type", "application/json");
    verify(response).setStatusCode(201);
    verify(response).end(mockResponse.encode());
  }

  @Test
  void testHandleFileUpload_shouldFailOnFileReadError() {
    Vertx vertxMock = mock(Vertx.class);
    RoutingContext routingContext = RoutingContextMocker.mockRoutingContext(new JsonObject());
    HubStoreClient hubStoreClient = mock(HubStoreClient.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    FileUpload fileUpload = mock(FileUpload.class);
    FileSystem fileSystem = mock(FileSystem.class);

    String contentType = "text/plain";
    String parentName = "parent";
    String parentId = "123";
    String fileName = "test.txt";

    when(routingContext.request()).thenReturn(request);
    when(routingContext.fileUploads()).thenReturn(Collections.singletonList(fileUpload));
    when(fileUpload.uploadedFileName()).thenReturn(fileName);
    when(fileUpload.fileName()).thenReturn(fileName);
    when(request.getHeader("Content-Type")).thenReturn(contentType);
    when(vertxMock.fileSystem()).thenReturn(fileSystem);

    when(fileSystem.readFile(fileName)).thenReturn(Future.failedFuture(new RuntimeException("Disk error")));

    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.response()).thenReturn(response);

    abstractManager.handleFileUpload(routingContext, parentName, parentId, hubStoreClient, vertxMock);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("Failed to upload file"));
  }


  @Test
  void testDeleteAllEntities_shouldReturn404OnNotFoundMessage()
    throws NoSuchFieldException, IllegalAccessException {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    HttpServerRequest request = mock(HttpServerRequest.class);

    when(context.request()).thenReturn(request);
    when(context.response()).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());
    Mockito.when(response.setStatusCode(Mockito.anyInt())).thenReturn(response);

    User mockUser = Mockito.mock(User.class);
    String ecasId = "n3817490";
    // Define rules
    Mockito.when(context.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    Mockito.when(context.user().principal()).thenReturn(principal);
    Mockito.when(mockUser.get("preferred_username")).thenReturn(ecasId);

    // Setup mock DB handler
    DBHandler mockDbHandler = mock(DBHandler.class);
    RuntimeException notFoundEx = new RuntimeException(HttpResponseStatus.NOT_FOUND.reasonPhrase());
    when(mockDbHandler.deleteAll(eq("collectionName"), any()))
      .thenReturn(Future.failedFuture(notFoundEx));
    Field dbField = AbstractManager.class.getDeclaredField("dbHandler");
    dbField.setAccessible(true);
    dbField.set(abstractManager, mockDbHandler);

    abstractManager.deleteAllEntities(context, "collectionName");

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(contains("Not Found"));
  }

}
