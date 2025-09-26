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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DdaDatasetManagerTest {

  User user;
  JsonObject principal;
  @InjectMocks
  DdaDatasetManager manager;
  @Mock
  private DBHandler dbHandler;
  @Mock
  private MongoClient mongoClient;
  @Mock
  private RoutingContext routingContext;
  @Mock
  private RequestBody requestBody;
  @Mock
  private HttpServerResponse response;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    manager = new DdaDatasetManager(dbHandler, mongoClient);

    when(routingContext.response()).thenReturn(response);
    user = mock(User.class);
    principal = new JsonObject().put("preferred_username", "testUser");

  }


  @Test
  void testCreateOrUpdateDatasetDescription_unauthorized() {
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(new JsonObject().put("payload", new JsonObject()));
    // Simulate Context.getEcasId returning null
    principal = new JsonObject().put("preferred_username", "");
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);

    when(response.setStatusCode(anyInt())).thenReturn(response);
    manager.createOrUpdateDatasetDescription(routingContext);

    verify(response).setStatusCode(401);
    verify(response).end(contains("Unauthorized"));
  }

  @Test
  void testCreateOrUpdateDatasetDescription_successUpdate() {
    when(routingContext.body()).thenReturn(requestBody);
    JsonObject payload = new JsonObject().put("datasetDescriptionAssistantAccessLevel", "public")
      .put("datasetDescriptionAssistantLanguage", "en")
      .put("datasetDescriptionAssistantFormData", new JsonObject().put("title", "demo"))
      .put("accessLevel", "test");

    when(requestBody.asJsonObject()).thenReturn(payload);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.setStatusCode(anyInt()).putHeader(anyString(), anyString())).thenReturn(response);

    when(mongoClient.updateCollectionWithOptions(anyString(), any(), (JsonObject) any(), any(),
      any())).thenAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
      handler.handle(Future.succeededFuture(updateResult));
      return null;
    });
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    manager.createOrUpdateDatasetDescription(routingContext);

    verify(response).setStatusCode(200);
    verify(response).end(contains("testUser"));

  }

  @Test
  void testCreateOrUpdateDatasetDescription_successUpdate2() {
    when(routingContext.body()).thenReturn(requestBody);
    JsonObject payload = new JsonObject().put("datasetDescriptionAssistantAccessLevel", "public")
      .put("datasetDescriptionAssistantLanguage", "en")
      .put("datasetDescriptionAssistantFormData", new JsonObject().put("title", "demo"));

    when(requestBody.asJsonObject()).thenReturn(payload);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.setStatusCode(anyInt()).putHeader(anyString(), anyString())).thenReturn(response);

    when(mongoClient.updateCollectionWithOptions(anyString(), any(), (JsonObject) any(), any(),
      any())).thenAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
      handler.handle(Future.succeededFuture(updateResult));
      return null;
    });
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    manager.createOrUpdateDatasetDescription(routingContext);

    verify(response).setStatusCode(200);
    verify(response).end(contains("testUser"));

  }


  @Test
  void testGetDatasetDescription_noContent() {

    // Simulate mongoClient returning null doc
    when(mongoClient.findOne(anyString(), any(), isNull())).thenReturn(
      Future.succeededFuture(null));
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    manager.getDatasetDescription(routingContext);

    verify(response).setStatusCode(204);
    verify(response).end();

  }

  @Test
  void testGetDatasetDescription_success() {

    JsonObject doc = new JsonObject().put("userName", "testUser")
      .put("payload", new JsonObject().put("language", "en")).put("_id", "mongoId");

    when(mongoClient.findOne(anyString(), any(), isNull())).thenReturn(
     Future.succeededFuture(doc));
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);

    manager.getDatasetDescription(routingContext);

    verify(response).setStatusCode(200);
    verify(response).end(contains("language"));
    verify(response, never()).end(contains("_id")); // ensure _id removed

  }
}
