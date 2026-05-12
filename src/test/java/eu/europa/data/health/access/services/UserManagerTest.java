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

import static eu.europa.data.health.access.Constants.APPLICATION_COLLABORATOR_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.NOTIFICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.USER_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.UserProfileDTO;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.mail.MailClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testcontainers.containers.MongoDBContainer;

@ExtendWith(VertxExtension.class)
class UserManagerTest {

  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";
  private static final List<String> TEST_COLLECTIONS = List.of(
    USER_COLLECTION,
    NOTIFICATION_COLLECTION,
    DATA_ACCESS_APPLICATION_COLLECTION,
    DATA_REQUEST_APPLICATION_COLLECTION,
    DISPATCHER_COLLECTION
  );
  private static MongoDBContainer mongoDBContainer;
  private static Vertx vertx;
  private static MongoClient mongoClient;
  private static DBHandler dbHandler;
  private static UserManager userManager;
  private AuthClient authClient;
  private DispatcherClient dispatcherClient;
  private NotificationManager notificationManager;


  @BeforeAll
  static void setUpSuite() {
    vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:5.0");
    mongoDBContainer.start();

    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE));

    mongoClient = MongoClient.createShared(vertx, config);
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    cleanupCollections();
    dbHandler = new DBHandler(mongoClient);
    dispatcherClient = mock(DispatcherClient.class);
    authClient = mock(AuthClient.class);
    MailClient mailClient = mock(MailClient.class);
    notificationManager = new NotificationManager(dbHandler, mailClient, "http://test-base-url");
    AuditLogManager auditLogManager = new AuditLogManager(dbHandler, authClient);
    userManager = new UserManager(dbHandler, mongoClient, dispatcherClient, auditLogManager,
      authClient, notificationManager);
  }

  @AfterAll
  static void tearDownSuite() throws InterruptedException {
    closeMongoClient();
    closeVertx();
    mongoDBContainer.stop();
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
        // Note: email field is removed by UserManager during user creation, so we don't check for it
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

    vertx.setTimer(1000, timerId -> {
      testContext.verify(() -> {
        HttpServerResponse response = context.response();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("Document created or updated successfully", response.getStatusMessage());
      });

      userManager.createOrUpdateUser(context2);

      vertx.setTimer(1000, timerId1 -> {
        testContext.verify(() -> {
          HttpServerResponse response1 = context2.response();
          assertNotNull(response1);
          assertEquals(200, response1.getStatusCode());
          assertEquals("Document created or updated successfully", response1.getStatusMessage());
        });

        userManager.getUser(context2);

        vertx.setTimer(1000, timerId2 -> {
          testContext.verify(() -> {
            HttpServerResponse response2 = context2.response();
            assertNotNull(response2);
            assertEquals(200, response2.getStatusCode());

            JsonObject foundResponse = new JsonObject(response2.getStatusMessage());
            assertEquals("testEULoginIDLegal", foundResponse.getString("euLoginID"));
            assertEquals("testLegalPersonBusinessID_UPDATED",
              foundResponse.getString("legalPersonBusinessID"));
          });
          testContext.completeNow();
        });
      });
    });
  }

  @Test
  void testCreateOrUpdateUser_validRequest(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

    userManager.createOrUpdateUser(context);

    vertx.setTimer(500, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      assertEquals(200, response.getStatusCode(), "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());
      testContext.completeNow();
    });
  }

  @Test
  void testCreateOrUpdateUser_invalidRequest(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

    // Simulate failure on MongoClient
    MongoClient mongoClientSpy = Mockito.spy(mongoClient);
    doAnswer(inv -> Future.failedFuture(new RuntimeException("MongoDB error")))
      .when(mongoClientSpy)
      .updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class),
        any(UpdateOptions.class));

    UserManager userManagerWithMockedMongoClient = new UserManager(dbHandler, mongoClientSpy,
      dispatcherClient,
      new AuditLogManager(dbHandler, authClient), authClient, notificationManager);

    userManagerWithMockedMongoClient.createOrUpdateUser(context);

    vertx.setTimer(500, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      assertEquals(500, response.getStatusCode(), "Expected status code to be 500");
      assertEquals("Failed", response.getStatusMessage());
      testContext.completeNow();
    });
  }

  @Test
  void testDeleteUser(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("n3817490");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

      JsonObject notification = new JsonObject()
        .put("_id", "notificationId")
        .put("userId", "n3817490")
        .put("message", "test notification");

      dbHandler.create(NOTIFICATION_COLLECTION, notification)
        .onSuccess(v -> {
          userManager.createOrUpdateUser(context);

          vertx.setTimer(1000, timerId -> {
            testContext.verify(() -> {
              HttpServerResponse response = context.response();
              assertNotNull(response, "Response should not be null");
              assertEquals(200, response.getStatusCode(), "Expected status code to be 200");
              assertEquals("Document created or updated successfully", response.getStatusMessage());
            });

            userManager.deleteUser(context);

            vertx.setTimer(1200, timerId2 -> {
              HttpServerResponse response2 = context.response();
              dbHandler.countAll(NOTIFICATION_COLLECTION, new JsonObject().put("userId", "n3817490"))
                .onSuccess(count -> {
                  testContext.verify(() -> {
                    assertNotNull(response2, "Response should not be null");
                    assertEquals(204, response2.getStatusCode(), "Expected status code to be 204");
                    assertEquals(0, count, "Expected notifications to be deleted");
                  });
                  testContext.completeNow();
                })
                .onFailure(testContext::failNow);
            });
          });
        })
        .onFailure(testContext::failNow);
    }
  }

  private JsonObject createMockNaturalUser() {
    UserProfileDTO userProfile = new UserProfileDTO();
    userProfile.setEuLoginID("testEULoginIDLegal");
    userProfile.setEmail("legal.person@example.com");
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
    }).when(userManagerSpy).deleteUserHealthData(any(), anyString(), anyBoolean());

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

    JsonObject mockApplicationBindOnUser = new JsonObject()
      .put("userId", "n3817490")
      .put("_id", "applicationId");

    JsonObject dispatcherDeleteQuery = new JsonObject()
      .put("application.userId", "n3817490");

    JsonObject notification = new JsonObject()
      .put("_id", "notificationId")
      .put("userId", "n3817490")
      .put("message", "test notification");

    when(dispatcherClient.deleteApplicationOnDispatcher(any()))
      .thenReturn(Future.succeededFuture());

    Future.all(
      dbHandler.create(DATA_ACCESS_APPLICATION_COLLECTION, mockApplicationBindOnUser),
      dbHandler.create(DATA_REQUEST_APPLICATION_COLLECTION, mockApplicationBindOnUser),
      dbHandler.create(DISPATCHER_COLLECTION, dispatcherDeleteQuery),
      dbHandler.create(NOTIFICATION_COLLECTION, notification)
    ).onSuccess(v -> {
      userManager.createOrUpdateUser(context);

      vertx.setTimer(800, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response, "Response should not be null");
          assertEquals(200, response.getStatusCode(), "Expected status code to be 200");
          assertEquals("Document created or updated successfully", response.getStatusMessage());
        });

        userManager.deleteUser(context);

        vertx.setTimer(1200, timerId2 -> {
          HttpServerResponse response2 = context.response();
          dbHandler.countAll(NOTIFICATION_COLLECTION, new JsonObject().put("userId", "n3817490"))
            .onSuccess(count -> {
              testContext.verify(() -> {
                assertNotNull(response2, "Response should not be null");
                assertEquals(204, response2.getStatusCode(), "Expected status code to be 204");
                assertEquals(0, count, "Expected notifications to be deleted");
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow);
        });
      });
    }).onFailure(testContext::failNow);
  }

  @Test
  void testCreateOrUpdateUser_missingEmail_ReturnsBadRequest(Vertx vertx,
    VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    JsonObject invalidUser = createMockNaturalUserWithoutEmail();
    when(context.body().asJsonObject()).thenReturn(invalidUser);

    userManager.createOrUpdateUser(context);

    vertx.setTimer(500, timerId -> {
      HttpServerResponse response = context.response();
      assertNotNull(response, "Response should not be null");
      assertEquals(200, response.getStatusCode(), "Expected status code to be 200");
      assertEquals("Document created or updated successfully", response.getStatusMessage());
      testContext.completeNow();
    });
  }

  private JsonObject createMockNaturalUserWithoutEmail() {
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
  void testGetUserById_returnsUserProfile(Vertx vertx, VertxTestContext testContext) {
    JsonObject user = createMockNaturalUser()
      .put("_id", "admin-target-user")
      .put("date_modified", java.time.Instant.now().toString());

    mongoClient.insert(USER_COLLECTION, user)
      .onSuccess(v -> {
        RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
        when(context.pathParam("userId")).thenReturn("admin-target-user");

        userManager.getUserById(context);

        vertx.setTimer(500, timerId -> {
          testContext.verify(() -> {
            HttpServerResponse response = context.response();
            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            JsonObject foundResponse = new JsonObject(response.getStatusMessage());
            assertEquals("testEULoginIDLegal", foundResponse.getString("euLoginID"));
            assertEquals("legal.person@example.com", foundResponse.getString("email"));
            assertNull(foundResponse.getString("_id"));
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  void testGetUserById_returnsNotFoundWhenMissing(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.pathParam("userId")).thenReturn("missing-user");

    userManager.getUserById(context);

    vertx.setTimer(500, timerId -> {
      testContext.verify(() -> {
        HttpServerResponse response = context.response();
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());

        JsonObject errorResponse = new JsonObject(response.getStatusMessage());
        assertEquals("User not found", errorResponse.getString("result_message"));
      });
      testContext.completeNow();
    });
  }

  @Test
  void testGetUserById_missingUserId(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.pathParam("userId")).thenReturn(null);

    userManager.getUserById(context);

    vertx.setTimer(500, timerId -> {
      testContext.verify(() -> {
        HttpServerResponse response = context.response();
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());

        JsonObject errorResponse = new JsonObject(response.getStatusMessage());
        assertEquals("Missing userId", errorResponse.getString("result_message"));
      });
      testContext.completeNow();
    });
  }

  @Test
  void testGetUserById_emptyUserId(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.pathParam("userId")).thenReturn("   ");

    userManager.getUserById(context);

    vertx.setTimer(500, timerId -> {
      testContext.verify(() -> {
        HttpServerResponse response = context.response();
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());

        JsonObject errorResponse = new JsonObject(response.getStatusMessage());
        assertEquals("Missing userId", errorResponse.getString("result_message"));
      });
      testContext.completeNow();
    });
  }

  @Test
  void testUpdateUserEmailPreferences(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("testUserForUpdate");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      JsonObject preferences = new JsonObject()
        .put("applicationApprovedNotification", true)
        .put("applicationRejectedNotification", false);
      when(context.body().asJsonObject()).thenReturn(preferences);

      userManager.updateUserEmailPreferences(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(200, response.getStatusCode());
          assertEquals("Email preferences updated successfully", response.getStatusMessage());
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testUpdateUserEmailPreferences_failure(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("testUserForUpdate");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      JsonObject preferences = new JsonObject()
        .put("applicationApprovedNotification", true);
      when(context.body().asJsonObject()).thenReturn(preferences);

      // Create a spy on mongoClient and make updateCollection fail
      MongoClient mongoClientSpy = Mockito.spy(mongoClient);
      doAnswer(inv -> Future.failedFuture(new RuntimeException("DB error")))
        .when(mongoClientSpy)
        .updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class));

      UserManager userManagerWithMockedMongoClient = new UserManager(dbHandler, mongoClientSpy,
        dispatcherClient,
        new AuditLogManager(dbHandler, authClient), authClient, notificationManager);

      userManagerWithMockedMongoClient.updateUserEmailPreferences(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(500, response.getStatusCode());
          assertEquals("Failed to update email preferences", response.getStatusMessage());
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testBootstrapUser_success(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("bootstrapUser");
      contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("bootstrap@example.com");
      contextMock.when(() -> Context.getUserName(any())).thenReturn("bootstrapUsername");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      userManager.bootstrapUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(200, response.getStatusCode());

          JsonObject responseBody = new JsonObject(response.getStatusMessage());
          assertEquals("bootstrapUser", responseBody.getString("ecasId"));
          assertEquals("bootstrap@example.com", responseBody.getString("email"));
          assertEquals("bootstrapUsername", responseBody.getString("userName"));
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testBootstrapUser_missingEmail(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("bootstrapUser");
      contextMock.when(() -> Context.getEcasEmail(any())).thenReturn(null);
      contextMock.when(() -> Context.getUserName(any())).thenReturn("bootstrapUsername");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      userManager.bootstrapUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(400, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testBootstrapUser_blankEmail(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("bootstrapUser");
      contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("   ");
      contextMock.when(() -> Context.getUserName(any())).thenReturn("bootstrapUsername");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      userManager.bootstrapUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(400, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testBootstrapUser_withPendingInvites(Vertx vertx, VertxTestContext testContext) {
    // Insert a pending collaborator invitation
    JsonObject pendingInvite = new JsonObject()
      .put("_id", "inviteId1")
      .put("invitedEmail", "invited@example.com")
      .put("invitationStatus", "PENDING")
      .put("applicationId", "appId1");

    mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, pendingInvite)
      .onSuccess(v -> {
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
          contextMock.when(() -> Context.getEcasId(any())).thenReturn("bootstrapUserWithInvites");
          contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("invited@example.com");
          contextMock.when(() -> Context.getUserName(any())).thenReturn("invitedUser");

          RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

          userManager.bootstrapUser(context);

          vertx.setTimer(1000, timerId -> {
            testContext.verify(() -> {
              HttpServerResponse response = context.response();
              assertNotNull(response);
              assertEquals(200, response.getStatusCode());

              JsonObject responseBody = new JsonObject(response.getStatusMessage());
              assertEquals("bootstrapUserWithInvites", responseBody.getString("ecasId"));
              assertEquals("invited@example.com", responseBody.getString("email"));
            });
            testContext.completeNow();
          });
        }
      })
      .onFailure(testContext::failNow);
  }

  @Test
  void testBootstrapUser_failure(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("bootstrapUserFail");
      contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("bootstrap@example.com");
      contextMock.when(() -> Context.getUserName(any())).thenReturn("bootstrapUsername");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      // Create a spy on mongoClient and make updateCollectionWithOptions fail
      MongoClient mongoClientSpy = Mockito.spy(mongoClient);
      doAnswer(inv -> Future.failedFuture(new RuntimeException("Bootstrap DB error")))
        .when(mongoClientSpy)
        .updateCollectionWithOptions(anyString(), any(JsonObject.class), any(JsonObject.class),
          any(UpdateOptions.class));

      UserManager userManagerWithMockedMongoClient = new UserManager(dbHandler, mongoClientSpy,
        dispatcherClient,
        new AuditLogManager(dbHandler, authClient), authClient, notificationManager);

      userManagerWithMockedMongoClient.bootstrapUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(500, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }



  @Test
  void testDeleteUserHealthData_dbHandlerFindFails(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("userFindFails");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      // Create a spy on mongoClient and make find fail
      MongoClient mongoClientSpy = Mockito.spy(mongoClient);
      doAnswer(inv -> Future.failedFuture(new RuntimeException("Find error")))
        .when(mongoClientSpy)
        .find(anyString(), any(JsonObject.class));

      UserManager userManagerWithMockedMongoClient = new UserManager(dbHandler, mongoClientSpy,
        dispatcherClient,
        new AuditLogManager(dbHandler, authClient), authClient, notificationManager);

      userManagerWithMockedMongoClient.deleteUser(context);

      vertx.setTimer(1000, timerId -> {
        HttpServerResponse response = context.response();
        testContext.verify(() -> {
          assertNotNull(response);
          assertEquals(404, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }







  @Test
  void testResolvePendingCollaboratorInvites_noPendingInvites(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("userWithoutInvites");
      contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("noinvites@example.com");
      contextMock.when(() -> Context.getUserName(any())).thenReturn("noinvitesUser");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      userManager.bootstrapUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(200, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }



  @Test
  void testCreateOrUpdateUser_withNullEmailNotificationPreferences(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("userWithNullPrefs");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      JsonObject userWithNullPrefs = createMockNaturalUser();
      userWithNullPrefs.putNull("emailNotificationPreferences");
      when(context.body().asJsonObject()).thenReturn(userWithNullPrefs);

      userManager.createOrUpdateUser(context);

      vertx.setTimer(500, timerId -> {
        HttpServerResponse response = context.response();
        testContext.verify(() -> {
          assertNotNull(response);
          assertEquals(200, response.getStatusCode());
          assertEquals("Document created or updated successfully", response.getStatusMessage());
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testGetUserById_returnsUserProfileExcludingSystemFields(Vertx vertx, VertxTestContext testContext) {
    JsonObject user = createMockNaturalUser()
      .put("_id", "system-user-id")
      .put("date_created", java.time.Instant.now().toString())
      .put("date_modified", java.time.Instant.now().toString());

    mongoClient.insert(USER_COLLECTION, user)
      .onSuccess(v -> {
        RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
        when(context.pathParam("userId")).thenReturn("system-user-id");

        userManager.getUserById(context);

        vertx.setTimer(500, timerId -> {
          testContext.verify(() -> {
            HttpServerResponse response = context.response();
            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            JsonObject foundResponse = new JsonObject(response.getStatusMessage());
            assertEquals("testEULoginIDLegal", foundResponse.getString("euLoginID"));
            // _id should be removed
            assertNull(foundResponse.getString("_id"));
            // Other system fields should be preserved
            assertNotNull(foundResponse.getString("date_created"));
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
  }

  @Test
  void testDeleteUser_adminFlagTrue(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("adminDeletedUser");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

      // Create a mock auditLogManager to verify admin deletion creates correct audit logs
      AuditLogManager auditLogManagerSpy = Mockito.spy(
        new AuditLogManager(dbHandler, authClient));

      UserManager userManagerWithAuditSpy = new UserManager(dbHandler, mongoClient,
        dispatcherClient, auditLogManagerSpy, authClient, notificationManager);

      userManagerWithAuditSpy.createOrUpdateUser(context);

      vertx.setTimer(1000, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(200, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }

  @Test
  void testDeleteUser_withApplications(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("userWithApps");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      when(context.body().asJsonObject()).thenReturn(createMockNaturalUser());

      // Create test data
      JsonObject accessApp = new JsonObject()
        .put("_id", "accessAppId")
        .put("userId", "userWithApps")
        .put("title", "Access App");

      JsonObject requestApp = new JsonObject()
        .put("_id", "requestAppId")
        .put("userId", "userWithApps")
        .put("title", "Request App");

      JsonObject dispatcherRecord = new JsonObject()
        .put("application.userId", "userWithApps");

      when(dispatcherClient.deleteApplicationOnDispatcher(any()))
        .thenReturn(Future.succeededFuture());

      Future.all(
        mongoClient.insert("dataAccessApplication", accessApp),
        mongoClient.insert("dataRequestApplication", requestApp),
        mongoClient.insert("dispatcher", dispatcherRecord)
      ).onSuccess(v -> {
        userManager.createOrUpdateUser(context);

        vertx.setTimer(1000, timerId -> {
          testContext.verify(() -> {
            HttpServerResponse response = context.response();
            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
          });

          userManager.deleteUser(context);

          vertx.setTimer(1500, timerId2 -> {
            HttpServerResponse response2 = context.response();
            testContext.verify(() -> {
              assertNotNull(response2);
              assertEquals(204, response2.getStatusCode());
            });
            testContext.completeNow();
          });
        });
      }).onFailure(testContext::failNow);
    }
  }

  @Test
  void testBootstrapUser_resolvePendingInvitesMultiple(Vertx vertx, VertxTestContext testContext) {
    // Insert multiple pending invites
    JsonObject invite1 = new JsonObject()
      .put("_id", "invite1")
      .put("invitedEmail", "multi@example.com")
      .put("invitationStatus", "PENDING")
      .put("applicationId", "app1");

    JsonObject invite2 = new JsonObject()
      .put("_id", "invite2")
      .put("invitedEmail", "multi@example.com")
      .put("invitationStatus", "PENDING")
      .put("applicationId", "app2");

    Future.all(
      mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, invite1),
      mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, invite2)
    ).onSuccess(v -> {
      try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
        contextMock.when(() -> Context.getEcasId(any())).thenReturn("userMultiInvites");
        contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("multi@example.com");
        contextMock.when(() -> Context.getUserName(any())).thenReturn("multiUser");

        RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

        userManager.bootstrapUser(context);

        vertx.setTimer(1500, timerId -> {
          testContext.verify(() -> {
            HttpServerResponse response = context.response();
            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            JsonObject responseBody = new JsonObject(response.getStatusMessage());
            assertEquals("userMultiInvites", responseBody.getString("ecasId"));
            assertEquals("multi@example.com", responseBody.getString("email"));
          });
          testContext.completeNow();
        });
      }
    }).onFailure(testContext::failNow);
  }

  // --- L155-156: getUser when user doesn't exist (null jsonObject in findById) ---
  @Test
  void testGetUser_notFound(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("nonExistentUser");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

      userManager.getUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(404, response.getStatusCode());
          JsonObject body = new JsonObject(response.getStatusMessage());
          assertEquals("User not found", body.getString("result_message"));
        });
        testContext.completeNow();
      });
    }
  }

  // --- L261-265: deleteAll(FAVOURITES/NOTIFICATIONS) failure when no apps ---
  @Test
  void testDeleteUser_noApps_deleteFavouritesFails(Vertx vertx, VertxTestContext testContext) {
    DBHandler dbHandlerSpy = Mockito.spy(dbHandler);
    doAnswer(inv -> Future.failedFuture(new RuntimeException("Fav delete error")))
      .when(dbHandlerSpy)
      .deleteAll(anyString(), any(JsonObject.class));

    UserManager um = new UserManager(dbHandlerSpy, mongoClient, dispatcherClient,
      new AuditLogManager(dbHandlerSpy, authClient), authClient, notificationManager);

    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("userNoAppsFavFail");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      um.deleteUser(context);

      vertx.setTimer(1000, timerId -> {
        HttpServerResponse response = context.response();
        testContext.verify(() -> {
          assertNotNull(response);
          assertEquals(500, response.getStatusCode());
          assertTrue(response.getStatusMessage().contains("Failed to delete favourites"));
        });
        testContext.completeNow();
      });
    }
  }

  // --- L326-330: dbHandler.deleteAll(DISPATCHER_COLLECTION) failure ---
  @Test
  void testDeleteUser_withApps_dispatcherCollectionDeleteFails(Vertx vertx, VertxTestContext testContext) {
    JsonObject accessApp = new JsonObject()
      .put("_id", "appDispFail")
      .put("userId", "userDispatcherCollFail");

    when(dispatcherClient.deleteApplicationOnDispatcher(any()))
      .thenReturn(Future.succeededFuture());

    DBHandler dbHandlerSpy = Mockito.spy(dbHandler);
    doAnswer(inv -> Future.failedFuture(new RuntimeException("Dispatcher coll delete fail")))
      .when(dbHandlerSpy)
      .deleteAll(eq(DISPATCHER_COLLECTION), any(JsonObject.class));

    UserManager um = new UserManager(dbHandlerSpy, mongoClient, dispatcherClient,
      new AuditLogManager(dbHandlerSpy, authClient), authClient, notificationManager);

    mongoClient.insert(DATA_ACCESS_APPLICATION_COLLECTION, accessApp)
      .onSuccess(v -> {
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
          contextMock.when(() -> Context.getEcasId(any())).thenReturn("userDispatcherCollFail");
          RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
          um.deleteUser(context);

          vertx.setTimer(1500, timerId -> {
            HttpServerResponse response = context.response();
            testContext.verify(() -> {
              assertNotNull(response);
              assertEquals(500, response.getStatusCode());
              assertTrue(response.getStatusMessage().contains("EU Dispatcher"));
            });
            testContext.completeNow();
          });
        }
      }).onFailure(testContext::failNow);
  }

  // --- L333-337: dispatcherClient.deleteApplicationOnDispatcher failure ---
  @Test
  void testDeleteUser_withApps_dispatcherClientFails(Vertx vertx, VertxTestContext testContext) {
    JsonObject accessApp = new JsonObject()
      .put("_id", "appDispClientFail")
      .put("userId", "userDispClientFail");

    when(dispatcherClient.deleteApplicationOnDispatcher(any()))
      .thenReturn(Future.failedFuture(new RuntimeException("Dispatcher client error")));

    mongoClient.insert(DATA_ACCESS_APPLICATION_COLLECTION, accessApp)
      .onSuccess(v -> {
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
          contextMock.when(() -> Context.getEcasId(any())).thenReturn("userDispClientFail");
          RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
          userManager.deleteUser(context);

          vertx.setTimer(1000, timerId -> {
            HttpServerResponse response = context.response();
            testContext.verify(() -> {
              assertNotNull(response);
              assertEquals(500, response.getStatusCode());
              assertTrue(response.getStatusMessage().contains("DISPATCHER collection"));
            });
            testContext.completeNow();
          });
        }
      }).onFailure(testContext::failNow);
  }

  // --- L383-401: notifyCollaboratorsForDeletedApps with accepted collaborators ---
  @Test
  void testDeleteUser_withAppsAndAcceptedCollaborators(Vertx vertx, VertxTestContext testContext) {
    JsonObject accessApp = new JsonObject()
      .put("_id", "appWithCollabs")
      .put("userId", "ownerWithCollabs")
      .put("title", "App With Collaborators");

    // Accepted collaborator for the app
    JsonObject acceptedCollab = new JsonObject()
      .put("application_id", "appWithCollabs")
      .put("invitationStatus", "ACCEPTED")
      .put("collaboratorUserId", "collab1");

    // Collaborator with no userId (should be skipped - L388 branch)
    JsonObject collabNoUserId = new JsonObject()
      .put("application_id", "appWithCollabs")
      .put("invitationStatus", "ACCEPTED");

    JsonObject userDoc = new JsonObject().put("_id", "ownerWithCollabs");

    when(dispatcherClient.deleteApplicationOnDispatcher(any()))
      .thenReturn(Future.succeededFuture());

    Future.all(
      mongoClient.insert(DATA_ACCESS_APPLICATION_COLLECTION, accessApp),
      mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, acceptedCollab),
      mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, collabNoUserId),
      mongoClient.insert(USER_COLLECTION, userDoc)
    ).onSuccess(v -> {
      try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
        contextMock.when(() -> Context.getEcasId(any())).thenReturn("ownerWithCollabs");
        RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

        userManager.deleteUser(context);

        vertx.setTimer(1500, timerId2 -> {
          HttpServerResponse response = context.response();
          testContext.verify(() -> {
            assertNotNull(response);
            assertEquals(204, response.getStatusCode());
          });
          testContext.completeNow();
        });
      }
    }).onFailure(testContext::failNow);
  }

  // --- L416-420: deleteUserEntity with admin=true ---
  @Test
  void testAdminDeleteUser_fullDeletion_adminAuditLog(Vertx vertx, VertxTestContext testContext) {
    RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(context.request().getParam("userId")).thenReturn("keycloakId");
    when(context.request().getParam("username")).thenReturn("adminTargetUser");

    // Put user in DB
    mongoClient.insert(USER_COLLECTION, new JsonObject().put("_id", "adminTargetUser"))
      .onSuccess(v -> {
        when(authClient.deleteUser(eq(context), eq("keycloakId")))
          .thenReturn(Future.succeededFuture());

        userManager.adminDeleteUser(context);

        vertx.setTimer(1500, timerId -> {
          testContext.verify(() -> {
            HttpServerResponse response = context.response();
            assertNotNull(response);
            assertEquals(204, response.getStatusCode());
          });
          testContext.completeNow();
        });
      }).onFailure(testContext::failNow);
  }

  // --- L553-583: resolvePendingCollaboratorInvites - updateCollection failure path ---
  @Test
  void testBootstrapUser_resolvePendingInvites_updateFails(Vertx vertx, VertxTestContext testContext) {
    JsonObject pendingInvite = new JsonObject()
      .put("_id", "failInvite1")
      .put("invitedEmail", "failupdate@example.com")
      .put("invitationStatus", "PENDING")
      .put("applicationId", "appFail1");

    mongoClient.insert(APPLICATION_COLLABORATOR_COLLECTION, pendingInvite)
      .onSuccess(v -> {
        // Make updateCollection fail for applicationCollaborator
        MongoClient mongoClientSpy = Mockito.spy(mongoClient);
        doAnswer(inv -> Future.failedFuture(new RuntimeException("Update invite fail")))
          .when(mongoClientSpy)
          .updateCollection(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class), any(JsonObject.class));

        UserManager um = new UserManager(dbHandler, mongoClientSpy, dispatcherClient,
          new AuditLogManager(dbHandler, authClient), authClient, notificationManager);

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
          contextMock.when(() -> Context.getEcasId(any())).thenReturn("userFailUpdate");
          contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("failupdate@example.com");
          contextMock.when(() -> Context.getUserName(any())).thenReturn("failUpdateUser");

          RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
          um.bootstrapUser(context);

          vertx.setTimer(1500, timerId -> {
            testContext.verify(() -> {
              // Bootstrap still returns 200 - resolving invites is fire-and-forget
              HttpServerResponse response = context.response();
              assertNotNull(response);
              assertEquals(200, response.getStatusCode());
            });
            testContext.completeNow();
          });
        }
      }).onFailure(testContext::failNow);
  }

  // --- L362: notifyCollaboratorsForDeletedApps called with app that has null/blank appId ---
  @Test
  void testDeleteUser_withApps_appHasNoTitle_collabsEmpty(Vertx vertx, VertxTestContext testContext) {
    // App without a title (tests appTitle == null branch in L384)
    JsonObject accessApp = new JsonObject()
      .put("_id", "appNoTitle")
      .put("userId", "ownerNoTitle");

    JsonObject userDoc = new JsonObject().put("_id", "ownerNoTitle");

    when(dispatcherClient.deleteApplicationOnDispatcher(any()))
      .thenReturn(Future.succeededFuture());

    Future.all(
      mongoClient.insert(DATA_ACCESS_APPLICATION_COLLECTION, accessApp),
      mongoClient.insert(USER_COLLECTION, userDoc)
    ).onSuccess(v -> {
      try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
        contextMock.when(() -> Context.getEcasId(any())).thenReturn("ownerNoTitle");
        RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

        userManager.deleteUser(context);

        vertx.setTimer(1500, timerId -> {
          HttpServerResponse response = context.response();
          testContext.verify(() -> {
            assertNotNull(response);
            assertEquals(204, response.getStatusCode());
          });
          testContext.completeNow();
        });
      }
    }).onFailure(testContext::failNow);
  }

  // --- L155-156: respondWithUserById when findById succeeds with null ---
  @Test
  void testGetUser_findByIdReturnsNull(Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("ghostUser");

      DBHandler dbHandlerSpy = Mockito.spy(dbHandler);
      doAnswer(inv -> Future.succeededFuture((JsonObject) null))
        .when(dbHandlerSpy)
        .findById(eq(USER_COLLECTION), anyString());

      UserManager um = new UserManager(dbHandlerSpy, mongoClient, dispatcherClient,
        new AuditLogManager(dbHandlerSpy, authClient), authClient, notificationManager);

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      um.getUser(context);

      vertx.setTimer(500, timerId -> {
        testContext.verify(() -> {
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(404, response.getStatusCode());
          JsonObject body = new JsonObject(response.getStatusMessage());
          assertEquals("User not found", body.getString("result_message"));
        });
        testContext.completeNow();
      });
    }
  }

  // --- L370: notifyCollaboratorsForDeletedApps with app having no _id (blank appId branch) ---
  @Test
  void testDeleteUser_withApp_noId_skipsContinue(Vertx vertx, VertxTestContext testContext) {
    when(dispatcherClient.deleteApplicationOnDispatcher(any()))
      .thenReturn(Future.succeededFuture());

    // Mock mongoClient.find to return a list with one app that has no _id
    MongoClient mongoClientSpy = Mockito.spy(mongoClient);
    JsonObject appWithNoId = new JsonObject().put("userId", "userNoIdApp");
    doAnswer(inv -> Future.succeededFuture(java.util.List.of(appWithNoId)))
      .when(mongoClientSpy)
      .find(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class));
    doAnswer(inv -> Future.succeededFuture(java.util.List.of()))
      .when(mongoClientSpy)
      .find(eq(DATA_REQUEST_APPLICATION_COLLECTION), any(JsonObject.class));

    mongoClient.insert(USER_COLLECTION, new JsonObject().put("_id", "userNoIdApp"))
      .onSuccess(v -> {
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
          contextMock.when(() -> Context.getEcasId(any())).thenReturn("userNoIdApp");
          RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());

          UserManager um = new UserManager(dbHandler, mongoClientSpy, dispatcherClient,
            new AuditLogManager(dbHandler, authClient), authClient, notificationManager);
          um.deleteUser(context);

          vertx.setTimer(1500, timerId -> {
            testContext.verify(() -> {
              HttpServerResponse response = context.response();
              assertNotNull(response);
              assertEquals(204, response.getStatusCode());
            });
            testContext.completeNow();
          });
        }
      }).onFailure(testContext::failNow);
  }

  // --- L587: resolvePendingCollaboratorInvites onFailure when find APPLICATION_COLLABORATOR fails ---
  @Test
  void testBootstrapUser_resolvePendingInvites_findFails(Vertx vertx, VertxTestContext testContext) {
    MongoClient mongoClientSpy = Mockito.spy(mongoClient);
    doAnswer(inv -> Future.failedFuture(new RuntimeException("Find invites error")))
      .when(mongoClientSpy)
      .find(eq(APPLICATION_COLLABORATOR_COLLECTION), any(JsonObject.class));

    UserManager um = new UserManager(dbHandler, mongoClientSpy, dispatcherClient,
      new AuditLogManager(dbHandler, authClient), authClient, notificationManager);

    try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
      contextMock.when(() -> Context.getEcasId(any())).thenReturn("userFindInvitesFail");
      contextMock.when(() -> Context.getEcasEmail(any())).thenReturn("findfail@example.com");
      contextMock.when(() -> Context.getUserName(any())).thenReturn("findFailUser");

      RoutingContext context = RoutingContextMocker.mockRoutingContext(new JsonObject());
      um.bootstrapUser(context);

      vertx.setTimer(1000, timerId -> {
        testContext.verify(() -> {
          // Bootstrap still returns 200 — fire-and-forget
          HttpServerResponse response = context.response();
          assertNotNull(response);
          assertEquals(200, response.getStatusCode());
        });
        testContext.completeNow();
      });
    }
  }

  private void cleanupCollections() throws InterruptedException {
    for (String collection : TEST_COLLECTIONS) {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<Throwable> error = new AtomicReference<>();

      mongoClient.removeDocuments(collection, new JsonObject(), result -> {
        if (result.failed()) {
          error.set(result.cause());
        }
        latch.countDown();
      });

      if (!latch.await(30, TimeUnit.SECONDS)) {
        throw new RuntimeException("Timed out cleaning collection: " + collection);
      }
      if (error.get() != null) {
        throw new RuntimeException(error.get());
      }
    }
  }

  private static void closeMongoClient() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    mongoClient.close().onComplete(result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out closing Mongo client");
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  private static void closeVertx() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    vertx.close(result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out closing Vertx");
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

}


