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

import static eu.europa.data.health.access.Constants.FAVOURITES_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
class FavouriteManagerTest {

  @Mock
  private MongoClient mongoClient;

  @Mock
  private HubSearchClient hubSearchClient;

  @Mock
  private RoutingContext routingContext;

  @Mock
  private HttpServerRequest request;

  @Mock
  private HttpServerResponse response;

  @Mock
  private RequestBody requestBody;

  @Mock
  private User user;

  @Mock
  private JsonObject principal;

  @Mock
  private MongoClientDeleteResult deleteResult;

  @InjectMocks
  private FavouriteManager favouriteManager;

  @BeforeEach
  void setup() {
    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    // Mock user context for Context.getEcasId()
    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    when(principal.getString("preferred_username")).thenReturn("testUser123");
  }

  @Test
  void testGetFavourites_noFilters_returnsAllFavourites() {
    // Arrange
    when(request.getParam("type")).thenReturn(null);

    List<JsonObject> favourites = new ArrayList<>();
    favourites.add(
      new JsonObject()
        .put("_id", "fav1")
        .put("userId", "testUser123")
        .put("type", "dataset")
        .put("objectId", "dataset123")
        .put("createdAt", "2024-01-01T10:00:00Z")
        .put("updatedAt", "2024-01-01T10:00:00Z")
    );
    favourites.add(
      new JsonObject()
        .put("_id", "fav2")
        .put("userId", "testUser123")
        .put("type", "distribution")
        .put("objectId", "dist456")
        .put("createdAt", "2024-01-02T10:00:00Z")
        .put("updatedAt", "2024-01-02T10:00:00Z")
    );

    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(favourites)
    );

    // Act
    favouriteManager.getFavourites(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).find(eq(FAVOURITES_COLLECTION), queryCaptor.capture());

    JsonObject capturedQuery = queryCaptor.getValue();
    assertTrue(capturedQuery.containsKey("userId"));
    assertNull(capturedQuery.getString("type"));
    assertNull(capturedQuery.getString("objectId"));
    verify(response).setStatusCode(200);
  }

  @Test
  void testGetFavourites_withTypeFilter_returnsFilteredFavourites() {
    // Arrange
    when(request.getParam("type")).thenReturn("dataset");

    List<JsonObject> favourites = new ArrayList<>();
    favourites.add(
      new JsonObject()
        .put("_id", "fav1")
        .put("userId", "testUser123")
        .put("type", "dataset")
        .put("objectId", "dataset123")
        .put("createdAt", "2024-01-01T10:00:00Z")
        .put("updatedAt", "2024-01-01T10:00:00Z")
    );

    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(favourites)
    );

    // Act
    favouriteManager.getFavourites(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).find(eq(FAVOURITES_COLLECTION), queryCaptor.capture());

    JsonObject capturedQuery = queryCaptor.getValue();
    assertEquals("dataset", capturedQuery.getString("type"));
    verify(response).setStatusCode(200);
  }

  @Test
  void testGetFavourites_missingUserId_returnsUnauthorized() {
    // Arrange
    when(principal.getString("preferred_username")).thenReturn(null);

    // Act
    favouriteManager.getFavourites(routingContext);

    // Assert
    verify(response).setStatusCode(401);
  }

  @Test
  void testGetFavourites_databaseFailure_returnsInternalServerError() {
    // Arrange
    when(request.getParam("userId")).thenReturn(null);
    when(request.getParam("type")).thenReturn(null);

    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.failedFuture("Database connection error")
    );

    // Act
    favouriteManager.getFavourites(routingContext);

    // Assert
    verify(mongoClient).find(eq(FAVOURITES_COLLECTION), any(JsonObject.class));
    verify(response).setStatusCode(500);
  }

  @Test
  void testCreateFavourite_validRequest_returnsCreated() {
    // Arrange
    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    String generatedId = "generatedId123";
    when(mongoClient.save(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(generatedId)
    );

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> favouriteCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(FAVOURITES_COLLECTION), favouriteCaptor.capture());

    JsonObject capturedFavourite = favouriteCaptor.getValue();
    assertEquals("testUser123", capturedFavourite.getString("userId"));
    assertEquals("dataset", capturedFavourite.getString("type"));
    assertEquals("dataset456", capturedFavourite.getString("objectId"));
    assertNotNull(capturedFavourite.getString("createdAt"));

    verify(response).setStatusCode(201);
  }

  @Test
  void testCreateFavourite_validSparqlQueryWithContent_returnsCreated() {
    // Arrange
    JsonObject content = new JsonObject()
      .put("label", "My SPARQL Query")
      .put("query", "SELECT * WHERE { ?s ?p ?o } LIMIT 10");

    JsonObject requestJson = new JsonObject().put("type", "sparqlQuery").put("content", content);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    String generatedId = "generatedId456";
    when(mongoClient.save(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(generatedId)
    );

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> favouriteCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(FAVOURITES_COLLECTION), favouriteCaptor.capture());

    JsonObject capturedFavourite = favouriteCaptor.getValue();
    assertEquals("testUser123", capturedFavourite.getString("userId"));
    assertEquals("sparqlQuery", capturedFavourite.getString("type"));
    assertNotNull(capturedFavourite.getString("createdAt"));
    assertNotNull(capturedFavourite.getJsonObject("content"));
    assertEquals("My SPARQL Query", capturedFavourite.getJsonObject("content").getString("label"));
    assertEquals(
      "SELECT * WHERE { ?s ?p ?o } LIMIT 10",
      capturedFavourite.getJsonObject("content").getString("query")
    );

    verify(response).setStatusCode(201);
  }

  @Test
  void testCreateFavourite_distributionWithParentId_returnsCreated() {
    // Arrange
    JsonObject requestJson = new JsonObject()
      .put("type", "distribution")
      .put("objectId", "dist123")
      .put("parentId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    String generatedId = "generatedIdABC";
    when(mongoClient.save(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(generatedId)
    );

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> favouriteCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(FAVOURITES_COLLECTION), favouriteCaptor.capture());

    JsonObject capturedFavourite = favouriteCaptor.getValue();
    assertEquals("testUser123", capturedFavourite.getString("userId"));
    assertEquals("distribution", capturedFavourite.getString("type"));
    assertEquals("dist123", capturedFavourite.getString("objectId"));
    assertEquals("dataset456", capturedFavourite.getString("parentId"));
    assertNotNull(capturedFavourite.getString("createdAt"));

    verify(response).setStatusCode(201);
  }

  @Test
  void testCreateFavourite_missingUserId_returnsUnauthorized() {
    // Arrange
    when(principal.getString("preferred_username")).thenReturn(null);

    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    verify(response).setStatusCode(401);
  }

  @Test
  void testCreateFavourite_databaseFailure_returnsInternalServerError() {
    // Arrange
    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dist789");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    when(mongoClient.save(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.failedFuture("Database connection error")
    );

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    verify(mongoClient).save(eq(FAVOURITES_COLLECTION), any(JsonObject.class));
    verify(response).setStatusCode(500);
  }

  @Test
  void testCreateFavourite_duplicateDistribution_returnsConflict() {
    // Arrange
    JsonObject requestJson = new JsonObject()
      .put("type", "distribution")
      .put("objectId", "dist123")
      .put("parentId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    JsonObject existingFavourite = new JsonObject()
      .put("_id", "existingId")
      .put("userId", "testUser123")
      .put("type", "distribution")
      .put("objectId", "dist123")
      .put("parentId", "dataset456");

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(existingFavourite)
    );

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    verify(mongoClient).findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any());
    verify(response).setStatusCode(409);
  }

  @Test
  void testCreateFavourite_duplicateDatasetFilter_returnsConflict() {
    // Arrange
    JsonObject content = new JsonObject()
      .put("label", "My Filter")
      .put("filters", new JsonObject());

    JsonObject requestJson = new JsonObject().put("type", "datasetFilter").put("content", content);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    JsonObject existingFavourite = new JsonObject()
      .put("_id", "existingId")
      .put("userId", "testUser123")
      .put("type", "datasetFilter")
      .put("content", content);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(existingFavourite)
    );

    // Act
    favouriteManager.createFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).findOne(eq(FAVOURITES_COLLECTION), queryCaptor.capture(), any());

    JsonObject capturedQuery = queryCaptor.getValue();
    assertEquals("testUser123", capturedQuery.getString("userId"));
    assertEquals("datasetFilter", capturedQuery.getString("type"));
    assertEquals("My Filter", capturedQuery.getString("content.label"));
    verify(response).setStatusCode(409);
  }

  @Test
  void testDeleteFavourite_validRequest_returnsNoContent() {
    // Arrange
    when(routingContext.pathParam("id")).thenReturn("507f1f77bcf86cd799439011");

    when(mongoClient.removeDocument(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(deleteResult)
    );
    when(deleteResult.getRemovedCount()).thenReturn(1L);

    // Act
    favouriteManager.deleteFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).removeDocument(eq(FAVOURITES_COLLECTION), queryCaptor.capture());

    JsonObject capturedQuery = queryCaptor.getValue();
    assertTrue(capturedQuery.containsKey("_id"));
    assertEquals("testUser123", capturedQuery.getString("userId"));

    verify(response).setStatusCode(204);
  }

  @Test
  void testDeleteFavourite_invalidId_returnsBadRequest() {
    // Arrange
    when(routingContext.pathParam("id")).thenReturn("invalid-id-format");

    // Act
    favouriteManager.deleteFavourite(routingContext);

    // Assert
    verify(response).setStatusCode(400);
  }

  @Test
  void testDeleteFavourite_missingUserId_returnsUnauthorized() {
    // Arrange
    when(principal.getString("preferred_username")).thenReturn(null);
    when(routingContext.pathParam("id")).thenReturn("507f1f77bcf86cd799439011");

    // Act
    favouriteManager.deleteFavourite(routingContext);

    // Assert
    verify(response).setStatusCode(401);
  }

  @Test
  void testDeleteFavourite_notFound_returnsNotFound() {
    // Arrange
    when(routingContext.pathParam("id")).thenReturn("507f1f77bcf86cd799439011");

    when(mongoClient.removeDocument(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(deleteResult)
    );
    when(deleteResult.getRemovedCount()).thenReturn(0L);

    // Act
    favouriteManager.deleteFavourite(routingContext);

    // Assert
    verify(mongoClient).removeDocument(eq(FAVOURITES_COLLECTION), any(JsonObject.class));
    verify(response).setStatusCode(404);
  }

  @Test
  void testDeleteFavourite_databaseFailure_returnsInternalServerError() {
    // Arrange
    when(routingContext.pathParam("id")).thenReturn("507f1f77bcf86cd799439011");

    when(mongoClient.removeDocument(eq(FAVOURITES_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.failedFuture("Database connection error")
    );

    // Act
    favouriteManager.deleteFavourite(routingContext);

    // Assert
    verify(mongoClient).removeDocument(eq(FAVOURITES_COLLECTION), any(JsonObject.class));
    verify(response).setStatusCode(500);
  }

  @Test
  void testUpdateFavourite_validSparqlQueryWithContent_returnsOk() {
    // Arrange
    String favouriteId = "507f1f77bcf86cd799439011";
    when(routingContext.pathParam("id")).thenReturn(favouriteId);

    JsonObject content = new JsonObject()
      .put("label", "Updated SPARQL Query")
      .put("query", "SELECT * WHERE { ?s ?p ?o } LIMIT 20");

    JsonObject requestJson = new JsonObject().put("type", "sparqlQuery").put("content", content);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    JsonObject updatedFavourite = new JsonObject()
      .put("_id", favouriteId)
      .put("userId", "testUser123")
      .put("type", "sparqlQuery")
      .put("content", content)
      .put("createdAt", "2024-01-01T10:00:00Z")
      .put("updatedAt", "2024-01-15T12:00:00Z");

    when(
      mongoClient.findOneAndUpdateWithOptions(
        eq(FAVOURITES_COLLECTION),
        any(JsonObject.class),
        any(JsonObject.class),
        any(),
        any()
      )
    ).thenReturn(Future.succeededFuture(updatedFavourite));

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).findOneAndUpdateWithOptions(
      eq(FAVOURITES_COLLECTION),
      any(JsonObject.class),
      updateCaptor.capture(),
      any(),
      any()
    );

    JsonObject capturedUpdate = updateCaptor.getValue();
    assertTrue(capturedUpdate.containsKey("$set"));
    JsonObject setFields = capturedUpdate.getJsonObject("$set");
    assertNull(setFields.getString("type"));
    assertNotNull(setFields.getString("updatedAt"));
    assertNotNull(setFields.getJsonObject("content"));
    assertEquals("Updated SPARQL Query", setFields.getJsonObject("content").getString("label"));
    assertEquals(
      "SELECT * WHERE { ?s ?p ?o } LIMIT 20",
      setFields.getJsonObject("content").getString("query")
    );

    verify(response).setStatusCode(200);
  }

  @Test
  void testUpdateFavourite_invalidId_returnsBadRequest() {
    // Arrange
    when(routingContext.pathParam("id")).thenReturn("invalid-id-format");

    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    verify(response).setStatusCode(400);
  }

  @Test
  void testUpdateFavourite_missingUserId_returnsUnauthorized() {
    // Arrange
    when(routingContext.pathParam("id")).thenReturn("507f1f77bcf86cd799439011");
    when(principal.getString("preferred_username")).thenReturn(null);

    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    verify(response).setStatusCode(401);
  }

  @Test
  void testUpdateFavourite_notFound_returnsNotFound() {
    // Arrange
    String favouriteId = "507f1f77bcf86cd799439011";
    when(routingContext.pathParam("id")).thenReturn(favouriteId);

    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    when(
      mongoClient.findOneAndUpdateWithOptions(
        eq(FAVOURITES_COLLECTION),
        any(JsonObject.class),
        any(JsonObject.class),
        any(),
        any()
      )
    ).thenReturn(Future.succeededFuture(null));

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    verify(mongoClient).findOneAndUpdateWithOptions(
      eq(FAVOURITES_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(),
      any()
    );
    verify(response).setStatusCode(404);
  }

  @Test
  void testUpdateFavourite_databaseFailure_returnsInternalServerError() {
    // Arrange
    String favouriteId = "507f1f77bcf86cd799439011";
    when(routingContext.pathParam("id")).thenReturn(favouriteId);

    JsonObject requestJson = new JsonObject().put("type", "dataset").put("objectId", "dataset456");

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(null)
    );

    when(
      mongoClient.findOneAndUpdateWithOptions(
        eq(FAVOURITES_COLLECTION),
        any(JsonObject.class),
        any(JsonObject.class),
        any(),
        any()
      )
    ).thenReturn(Future.failedFuture("Database connection error"));

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    verify(mongoClient).findOneAndUpdateWithOptions(
      eq(FAVOURITES_COLLECTION),
      any(JsonObject.class),
      any(JsonObject.class),
      any(),
      any()
    );
    verify(response).setStatusCode(500);
  }

  @Test
  void testUpdateFavourite_duplicateSparqlQuery_returnsConflict() {
    // Arrange
    String favouriteId = "507f1f77bcf86cd799439011";
    when(routingContext.pathParam("id")).thenReturn(favouriteId);

    JsonObject content = new JsonObject()
      .put("label", "My SPARQL Query")
      .put("query", "SELECT * WHERE { ?s ?p ?o } LIMIT 10");

    JsonObject requestJson = new JsonObject().put("type", "sparqlQuery").put("content", content);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    JsonObject existingFavourite = new JsonObject()
      .put("_id", "507f1f77bcf86cd799439022")
      .put("userId", "testUser123")
      .put("type", "sparqlQuery")
      .put("content", content);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(existingFavourite)
    );

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).findOne(eq(FAVOURITES_COLLECTION), queryCaptor.capture(), any());

    JsonObject capturedQuery = queryCaptor.getValue();
    assertEquals("testUser123", capturedQuery.getString("userId"));
    assertEquals("sparqlQuery", capturedQuery.getString("type"));
    assertEquals("My SPARQL Query", capturedQuery.getString("content.label"));
    assertEquals(favouriteId, capturedQuery.getJsonObject("_id").getString("$ne"));
    verify(response).setStatusCode(409);
  }

  @Test
  void testUpdateFavourite_duplicateDatasetFilter_returnsConflict() {
    // Arrange
    String favouriteId = "507f1f77bcf86cd799439011";
    when(routingContext.pathParam("id")).thenReturn(favouriteId);

    JsonObject content = new JsonObject()
      .put("label", "My Filter")
      .put("filters", new JsonObject());

    JsonObject requestJson = new JsonObject().put("type", "datasetFilter").put("content", content);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(requestJson);

    JsonObject existingFavourite = new JsonObject()
      .put("_id", "507f1f77bcf86cd799439022")
      .put("userId", "testUser123")
      .put("type", "datasetFilter")
      .put("content", content);

    when(mongoClient.findOne(eq(FAVOURITES_COLLECTION), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(existingFavourite)
    );

    // Act
    favouriteManager.updateFavourite(routingContext);

    // Assert
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).findOne(eq(FAVOURITES_COLLECTION), queryCaptor.capture(), any());

    JsonObject capturedQuery = queryCaptor.getValue();
    assertEquals("testUser123", capturedQuery.getString("userId"));
    assertEquals("datasetFilter", capturedQuery.getString("type"));
    assertEquals("My Filter", capturedQuery.getString("content.label"));
    assertEquals(favouriteId, capturedQuery.getJsonObject("_id").getString("$ne"));
    verify(response).setStatusCode(409);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // getStaleFavourites
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void testGetStaleFavourites_missingUserId_returnsUnauthorized() {
    when(principal.getString("preferred_username")).thenReturn(null);
    when(request.getParam("type")).thenReturn("dataset");

    favouriteManager.getStaleFavourites(routingContext);

    verify(response).setStatusCode(401);
  }

  @Test
  void testGetStaleFavourites_invalidType_returnsBadRequest() {
    when(request.getParam("type")).thenReturn("sparqlQuery");

    favouriteManager.getStaleFavourites(routingContext);

    verify(response).setStatusCode(400);
  }

  @Test
  void testGetStaleFavourites_nullType_returnsBadRequest() {
    when(request.getParam("type")).thenReturn(null);

    favouriteManager.getStaleFavourites(routingContext);

    verify(response).setStatusCode(400);
  }

  @Test
  void testGetStaleFavourites_mongoFindFails_returnsInternalServerError() {
    when(request.getParam("type")).thenReturn("dataset");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.failedFuture("db error"));

    favouriteManager.getStaleFavourites(routingContext);

    verify(response).setStatusCode(500);
  }

  @Test
  void testGetStaleFavourites_emptyFavouritesList_returnsEmptyArray() {
    when(request.getParam("type")).thenReturn("dataset");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of()));

    favouriteManager.getStaleFavourites(routingContext);

    verify(response).setStatusCode(200);
  }

  // ─── dataset path ──────────────────────────────────────────────────────────

  @Test
  void testGetStaleFavourites_datasetType_datasetNotFound_returnsFavouriteAsStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav1").put("userId", "testUser123")
        .put("type", "dataset").put("objectId", "ds-123");

    when(request.getParam("type")).thenReturn("dataset");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // hub-search returns a response without "result" → dataset is gone → stale
    when(hubSearchClient.getDataset(eq("ds-123")))
        .thenReturn(Future.succeededFuture(new JsonObject()));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(1, stale.size());
    assertEquals("fav1", stale.getJsonObject(0).getString("_id"));
  }

  @Test
  void testGetStaleFavourites_datasetType_datasetFound_notStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav1").put("userId", "testUser123")
        .put("type", "dataset").put("objectId", "ds-123");

    when(request.getParam("type")).thenReturn("dataset");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // hub-search returns a response with "result" → dataset exists → not stale
    when(hubSearchClient.getDataset(eq("ds-123")))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", new JsonObject())));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(0, stale.size());
  }

  @Test
  void testGetStaleFavourites_datasetType_multipleEntries_onlyStaleReturned() {
    JsonObject fav1 = new JsonObject()
        .put("_id", "fav1").put("userId", "testUser123")
        .put("type", "dataset").put("objectId", "ds-gone");
    JsonObject fav2 = new JsonObject()
        .put("_id", "fav2").put("userId", "testUser123")
        .put("type", "dataset").put("objectId", "ds-alive");

    when(request.getParam("type")).thenReturn("dataset");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav1, fav2)));
    when(hubSearchClient.getDataset(eq("ds-gone")))
        .thenReturn(Future.succeededFuture(new JsonObject()));
    when(hubSearchClient.getDataset(eq("ds-alive")))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", new JsonObject())));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(1, stale.size());
    assertEquals("fav1", stale.getJsonObject(0).getString("_id"));
  }

  // ─── distribution path ─────────────────────────────────────────────────────

  @Test
  void testGetStaleFavourites_distributionType_parentNotFound_returnsFavouriteAsStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav3").put("userId", "testUser123")
        .put("type", "distribution").put("parentId", "ds-gone").put("objectId", "dist-1");

    when(request.getParam("type")).thenReturn("distribution");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // parent dataset not found → no "result" key
    when(hubSearchClient.getDataset(eq("ds-gone")))
        .thenReturn(Future.succeededFuture(new JsonObject()));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(1, stale.size());
    assertEquals("fav3", stale.getJsonObject(0).getString("_id"));
  }

  @Test
  void testGetStaleFavourites_distributionType_parentExistsDistributionFound_notStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav4").put("userId", "testUser123")
        .put("type", "distribution").put("parentId", "ds-alive").put("objectId", "dist-1");

    when(request.getParam("type")).thenReturn("distribution");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // parent dataset found AND contains distribution dist-1
    JsonObject result = new JsonObject()
        .put("distributions", new JsonArray()
            .add(new JsonObject().put("id", "dist-1")));
    when(hubSearchClient.getDataset(eq("ds-alive")))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", result)));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(0, stale.size());
  }

  @Test
  void testGetStaleFavourites_distributionType_parentExistsDistributionMissing_returnsStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav5").put("userId", "testUser123")
        .put("type", "distribution").put("parentId", "ds-alive").put("objectId", "dist-missing");

    when(request.getParam("type")).thenReturn("distribution");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // parent dataset found but has different distributions – dist-missing is absent
    JsonObject result = new JsonObject()
        .put("distributions", new JsonArray()
            .add(new JsonObject().put("id", "dist-other")));
    when(hubSearchClient.getDataset(eq("ds-alive")))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", result)));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(1, stale.size());
    assertEquals("fav5", stale.getJsonObject(0).getString("_id"));
  }

  @Test
  void testGetStaleFavourites_distributionType_parentExistsEmptyDistributions_returnsStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav6").put("userId", "testUser123")
        .put("type", "distribution").put("parentId", "ds-alive").put("objectId", "dist-1");

    when(request.getParam("type")).thenReturn("distribution");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // parent dataset found but distributions array is empty
    JsonObject result = new JsonObject().put("distributions", new JsonArray());
    when(hubSearchClient.getDataset(eq("ds-alive")))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", result)));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(1, stale.size());
  }

  @Test
  void testGetStaleFavourites_distributionType_parentExistsNoDistributionsKey_returnsStale() {
    JsonObject fav = new JsonObject()
        .put("_id", "fav7").put("userId", "testUser123")
        .put("type", "distribution").put("parentId", "ds-alive").put("objectId", "dist-1");

    when(request.getParam("type")).thenReturn("distribution");
    when(mongoClient.find(eq(FAVOURITES_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(fav)));
    // parent dataset found but "result" has no distributions key at all
    when(hubSearchClient.getDataset(eq("ds-alive")))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", new JsonObject())));

    favouriteManager.getStaleFavourites(routingContext);

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(200);
    verify(response).end(bodyCaptor.capture());
    JsonArray stale = new JsonArray(bodyCaptor.getValue());
    assertEquals(1, stale.size());
  }
}
