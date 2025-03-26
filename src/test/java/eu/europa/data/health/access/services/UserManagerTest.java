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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.UserProfileDTO;
import eu.europa.data.health.access.handlers.DBHandler;
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
    DispatcherClient dispatcherClient = Mockito.mock(DispatcherClient.class);
    userManager = new UserManager(dbHandler, mongoClient, dispatcherClient);

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
          assertEquals("testLegalPersonBusinessID_UPDATED", foundResponse.getString("legalPersonBusinessID"));
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

}
