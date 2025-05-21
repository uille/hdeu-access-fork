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

import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.UserProfileDTO;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.containers.MongoDBContainer;

@ExtendWith(VertxExtension.class)
class UserManagerTest {

  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";
  private static MongoDBContainer mongoDBContainer;
  private static MongoClient mongoClient;
  private static DBHandler dbHandler;
  private static UserManager userManager;
  private AuthClient authClient;
  private DispatcherClient dispatcherClient;


  @BeforeEach
  void setUp(VertxTestContext testContext) {
    Vertx vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:4.4.2");
    mongoDBContainer.start();

    // Configure MongoClient with the connection string
    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE));

    mongoClient = MongoClient.createShared(vertx, config);
    dbHandler = new DBHandler(mongoClient);
    dispatcherClient = mock(DispatcherClient.class);
    authClient = mock(AuthClient.class);
    AuditLogManager auditLogManager = new AuditLogManager(dbHandler, authClient);
    userManager = new UserManager(dbHandler, mongoClient, dispatcherClient, auditLogManager,
      authClient);

    testContext.completeNow();
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
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
  void testCreateUser(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

    userManager.createOrUpdateUser(context);

    vertx.setTimer(800, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      int statusCode = response.getStatusCode();
      assertEquals(200, statusCode, "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());
      testContext.completeNow();
    });
  }

  @Test
  void testFindUser(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

    userManager.createOrUpdateUser(context);

    vertx.setTimer(800, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      int statusCode = response.getStatusCode();
      assertEquals(200, statusCode, "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());

      userManager.getUser(context);

      vertx.setTimer(800, timerId2 -> {
        HttpServerResponse response2 = context.response();
        assertNotNull(response2, "Response should not be null");
        int statusCode2 = response2.getStatusCode();
        assertEquals(200, statusCode2, "Expected status code to be 200");
        JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
        assertEquals("testEULoginIDLegal", foundResponse.getString("euLoginID"));
        testContext.completeNow();
      });
    });
  }

  @Test
  void testUpdateAndFindUser(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());
    RoutingContext context2 = RoutingContextMocker.mockRoutingContext(new JsonObject());
    JsonObject mockedUserForUpdate = createMockNaturalUser();
    mockedUserForUpdate.put("legalPersonBusinessID", "testLegalPersonBusinessID_UPDATED");
    when(context2.body().asJsonObject()).thenReturn(mockedUserForUpdate);

    userManager.createOrUpdateUser(context);

    vertx.setTimer(800, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      int statusCode = response.getStatusCode();
      assertEquals(200, statusCode, "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());

      userManager.createOrUpdateUser(context2);

      vertx.setTimer(800, timerId1 -> {

        HttpServerResponse response1 = context2.response();
        assertNotNull(response1, "Response should not be null");
        int statusCode1 = response1.getStatusCode();
        assertEquals(200, statusCode1, "Expected status code to be 200");
        assertEquals("Document created or updated successfully", response1.getStatusMessage());

        userManager.getUser(context2);

        vertx.setTimer(800, timerId2 -> {
          HttpServerResponse response2 = context2.response();
          assertNotNull(response2, "Response should not be null");
          int statusCode2 = response2.getStatusCode();
          assertEquals(200, statusCode2, "Expected status code to be 200");
          JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
          assertEquals("testEULoginIDLegal", foundResponse.getString("euLoginID"));
          assertEquals("testLegalPersonBusinessID_UPDATED",
            foundResponse.getString("legalPersonBusinessID"));
          testContext.completeNow();
        });
      });
    });
  }

  @Test
  void testDeleteUser(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

    userManager.createOrUpdateUser(context);

    vertx.setTimer(800, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      int statusCode = response.getStatusCode();
      assertEquals(200, statusCode, "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());

      userManager.deleteUser(context);

      vertx.setTimer(800, timerId2 -> {
        HttpServerResponse response2 = context.response();
        assertNotNull(response2, "Response should not be null");
        int statusCode2 = response2.getStatusCode();
        assertEquals(204, statusCode2, "Expected status code to be 204");
        testContext.completeNow();
      });
    });
  }

  private JsonObject createMockNaturalUser() {
    UserProfileDTO userProfile = new UserProfileDTO();
    userProfile.setEuLoginID("testEULoginIDLegal");
    userProfile.setLegalOrNaturalPerson(new SelectObjectDTO() {{
      setKey("a");
      setValue("Legal person");
    }});
    userProfile.setLegalPersonBusinessID("testLegalPersonBusinessID");
    userProfile.setLegalPersonAddress("testLegalPersonAddress");
    userProfile.setLegalPersonZipCode("testLegalPersonZipCode");
    userProfile.setLegalPersonCity("testLegalPersonCity");
    userProfile.setNaturalPersonAddress("asd");
    userProfile.setLegalPersonCountry(new SelectObjectDTO() {{
      setKey("http://publications.europa.eu/resource/authority/country/CZE");
      setValue("Czechia");
    }});
    userProfile.setLegalPersonPhone(new PhoneNumberDTO() {{
      setCountryCode("30");
      setNumber("6971234567");
    }});

    return JsonObject.mapFrom(userProfile);

  }

  @Test
  void testAdminDeleteUser_successfulDeletion(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.request().getParam("userId")).thenReturn("123");
    when(context.request().getParam("username")).thenReturn("testuser");

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);
    when(response.end()).thenReturn(null);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);

    // Mock AuthClient behavior
    when(authClient.deleteUser(eq(context), eq("123"))).thenReturn(Future.succeededFuture());

    UserManager userManagerSpy = Mockito.spy(userManager);
    doAnswer(inv -> {
      context.response().setStatusCode(204).end();
      return null;
    }).when(userManagerSpy).deleteUserHealthData(eq(context), eq("testuser"));

    userManagerSpy.adminDeleteUser(context);

    vertx.setTimer(500, timerId -> {
      verify(response).setStatusCode(204);
      testContext.completeNow();
    });
  }

  @Test
  void testAdminDeleteUser_missingParams(VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.request().getParam("userId")).thenReturn(null);
    when(context.request().getParam("username")).thenReturn(null);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);

    userManager.adminDeleteUser(context);

    verify(response).setStatusCode(400);
    verify(response).end(contains("Missing userId or username"));
    testContext.completeNow();
  }

  @Test
  void testAdminDeleteUser_keycloakDeletionFails(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.request().getParam("userId")).thenReturn("123");
    when(context.request().getParam("username")).thenReturn("testuser");

    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);

    // Simulate a failure in AuthClient's deleteUser call
    Throwable simulatedError = new RuntimeException("Keycloak unavailable");
    when(authClient.deleteUser(eq(context), eq("123")))
      .thenReturn(Future.failedFuture(simulatedError));

    userManager.adminDeleteUser(context);

    vertx.setTimer(500, id -> {
      verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      verify(response).end(
        contains("Error while deleting user from keycloak: Keycloak unavailable"));
      testContext.completeNow();
    });
  }

  @Test
  void testDeleteUser4(Vertx vertx, VertxTestContext testContext) {

    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

    JsonObject mockApplicationBindOnUser = new JsonObject();
    mockApplicationBindOnUser.put("userId", "n3817490");
    mockApplicationBindOnUser.put("_id", "applicationId");
    dbHandler.create(DATA_ACCESS_APPLICATION_COLLECTION, mockApplicationBindOnUser);
    dbHandler.create(DATA_REQUEST_APPLICATION_COLLECTION, mockApplicationBindOnUser);
    JsonObject dispatcherDeleteQuery = new JsonObject()
      .put("application.userId", "n3817490");
    dbHandler.create(DISPATCHER_COLLECTION, dispatcherDeleteQuery);
    userManager.createOrUpdateUser(context);

    vertx.setTimer(800, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      int statusCode = response.getStatusCode();
      assertEquals(200, statusCode, "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());

      when(dispatcherClient.deleteApplicationOnDispatcher(any())).thenReturn(
        Future.succeededFuture());

      userManager.deleteUser(context);

      vertx.setTimer(1200, timerId2 -> {
        HttpServerResponse response2 = context.response();
        assertNotNull(response2, "Response should not be null");
        int statusCode2 = response2.getStatusCode();
        assertEquals(204, statusCode2, "Expected status code to be 204");
        testContext.completeNow();
      });
    });
  }

}
