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
package eu.europa.data.health.access.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import org.apache.jena.http.auth.AuthEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;

@ExtendWith({ VertxExtension.class, MockitoExtension.class })
class DBHandlerTest {

  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";
  private static final String DB_HANDLER_COLLECTION = "dbHandlerCollection";
  private static final String ENTITY_COLLECTION = "entityCollection";
  private static final String APPLICATION_COLLECTION = "applicationCollection";
  private static final String AUDIT_LOG_COLLECTION = "auditLogCollection";
  private static final String UPDATE_COLLECTION = "updateCollection";
  private static final String LINKABLE_COLLECTION = "linkableCollection";
  private static final List<String> TEST_COLLECTIONS = List.of(
    DB_HANDLER_COLLECTION,
    ENTITY_COLLECTION,
    APPLICATION_COLLECTION,
    AUDIT_LOG_COLLECTION,
    UPDATE_COLLECTION,
    LINKABLE_COLLECTION
  );

  private static MongoDBContainer mongoDBContainer;
  private static MongoClient mongoClient;
  private static Vertx vertx;
  private static Logger dbHandlerLogger;
  private static Logger authEnvLogger;
  private static Level previousDbHandlerLogLevel;
  private static Level previousAuthEnvLogLevel;

  @Mock
  private MongoClient mockedMongoClient;

  @InjectMocks
  private DBHandler mockedHandler;

  private DBHandler dbHandler;

  @BeforeAll
  static void setUpSuite() {
    vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:5.0");
    mongoDBContainer.start();

    JsonObject config = new JsonObject().put(
      "connection_string",
      mongoDBContainer.getReplicaSetUrl(JUNIT_TEST_DATABASE)
    );

    mongoClient = MongoClient.createShared(vertx, config);

    // Set log level to DEBUG to cover branches that check if debug logging is enabled
    dbHandlerLogger = (Logger) LoggerFactory.getLogger(DBHandler.class);
    authEnvLogger = (Logger) LoggerFactory.getLogger(AuthEnv.class);
    previousDbHandlerLogLevel = dbHandlerLogger.getLevel();
    previousAuthEnvLogLevel = authEnvLogger.getLevel();
    dbHandlerLogger.setLevel(Level.DEBUG);
    authEnvLogger.setLevel(Level.DEBUG);
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    dbHandler = new DBHandler(mongoClient);
    cleanupCollections();
  }

  @AfterAll
  static void tearDownSuite() throws InterruptedException {
    closeMongoClient();
    closeVertx();
    dbHandlerLogger.setLevel(previousDbHandlerLogLevel);
    authEnvLogger.setLevel(previousAuthEnvLogLevel);
    mongoDBContainer.stop();
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCreateDocument(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testCreateDocumentKey")
      .put("value", "testCreateDocumentValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        return dbHandler.findById(DB_HANDLER_COLLECTION, id);
      })
      .onSuccess(result -> {
        assertEquals("testCreateDocumentKey", result.getString("key"));
        assertEquals("testCreateDocumentValue", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCreateDocumentWithPredefinedId(VertxTestContext testContext) {
    String predefinedId = "66f2848812955e42f75812b0";
    JsonObject document = new JsonObject()
      .put("_id", predefinedId)
      .put("key", "testCreateDocumentKey")
      .put("value", "testCreateDocumentValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNull(id);
        return dbHandler.findById(DB_HANDLER_COLLECTION, predefinedId);
      })
      .onSuccess(result -> {
        assertEquals(predefinedId, result.getString("_id"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateDocument(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testUpdateDocumentKey")
      .put("value", "testUpdateDocumentValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        JsonObject query = new JsonObject().put("_id", id);
        JsonObject updateDocument = new JsonObject()
          .put("key", "testUpdateDocumentKeyUpdated")
          .put("value", "testUpdateDocumentValueUpdated");
        return dbHandler
          .update(DB_HANDLER_COLLECTION, updateDocument, query)
          .compose(result ->
            dbHandler
              .findById(DB_HANDLER_COLLECTION, id)
              .map(found -> {
                assertEquals("1", result.getString("docMatched"));
                assertEquals("1", result.getString("docModified"));
                return found;
              })
          );
      })
      .onSuccess(result -> {
        assertEquals("testUpdateDocumentKeyUpdated", result.getString("key"));
        assertEquals("testUpdateDocumentValueUpdated", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailUpdateDocumentThatDoesNotExist(VertxTestContext testContext) {
    JsonObject updateDocument = new JsonObject()
      .put("key", "testUpdateDocumentKeyUpdated")
      .put("value", "testUpdateDocumentValueUpdated");

    dbHandler
      .update(
        DB_HANDLER_COLLECTION,
        updateDocument,
        new JsonObject().put("_id", "66f2b92dd764b50aab4f7360")
      )
      .onSuccess(result -> {
        assertEquals("0", result.getString("docMatched"));
        assertEquals("0", result.getString("docModified"));
        assertNull(result.getString("docUpsertedId"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDocumentByField(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testFindDocumentByFieldKey")
      .put("value", "testFindDocumentByFieldValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id ->
        dbHandler.findByField(
          DB_HANDLER_COLLECTION,
          new JsonObject().put("key", "testFindDocumentByFieldKey")
        )
      )
      .onSuccess(result -> {
        assertEquals("testFindDocumentByFieldKey", result.getString("key"));
        assertEquals("testFindDocumentByFieldValue", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailFindDocumentByFieldThatDoesNotExist(VertxTestContext testContext) {
    dbHandler
      .findByField(
        DB_HANDLER_COLLECTION,
        new JsonObject().put("key", "testFailFindDocumentByFieldThatDoesNotExistKey")
      )
      .onSuccess(result ->
        testContext.failNow(new AssertionError("Expected failure but got: " + result))
      )
      .onFailure(throwable -> assertNotFound(throwable, testContext));
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteDocumentByField(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testDeleteDocumentByFieldKey")
      .put("value", "testDeleteDocumentByFieldValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id ->
        dbHandler.deleteByField(
          DB_HANDLER_COLLECTION,
          new JsonObject().put("key", "testDeleteDocumentByFieldKey")
        )
      )
      .onSuccess(result -> {
        assertNotNull(result.getString("_id"));
        assertEquals("testDeleteDocumentByFieldKey", result.getString("key"));
        assertEquals("testDeleteDocumentByFieldValue", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailDeleteDocumentByFieldThatDoesNotExist(VertxTestContext testContext) {
    dbHandler
      .deleteByField(DB_HANDLER_COLLECTION, new JsonObject().put("key", "missing"))
      .onSuccess(result ->
        testContext.failNow(new AssertionError("Expected failure but got: " + result))
      )
      .onFailure(throwable -> assertNotFound(throwable, testContext));
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDocumentById(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testFindDocumentByIdKey")
      .put("value", "testFindDocumentByIdValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id -> dbHandler.findById(DB_HANDLER_COLLECTION, id))
      .onSuccess(result -> {
        assertEquals("testFindDocumentByIdKey", result.getString("key"));
        assertEquals("testFindDocumentByIdValue", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailFindDocumentByIdThatDoesNotExist(VertxTestContext testContext) {
    dbHandler
      .findById(DB_HANDLER_COLLECTION, "66f2b92dd764b50aab4f7360")
      .onSuccess(result ->
        testContext.failNow(new AssertionError("Expected failure but got: " + result))
      )
      .onFailure(throwable -> assertNotFound(throwable, testContext));
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithoutPagination(VertxTestContext testContext) {
    JsonObject draftStatus = new JsonObject().put("status", "draft");

    insertDocuments(
      DB_HANDLER_COLLECTION,
      new JsonObject().put("key", "key1").put("status", draftStatus).put("userId", "user-1"),
      new JsonObject().put("key", "key2").put("status", draftStatus).put("userId", "user-1")
    )
      .compose(v ->
        dbHandler.findAll(DB_HANDLER_COLLECTION, null, null, null, null, null, "user-1")
      )
      .onSuccess(allDocuments -> {
        assertEquals(2, allDocuments.size());
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithoutUserIdAndMultipleStatuses(VertxTestContext testContext) {
    insertDocuments(
      DB_HANDLER_COLLECTION,
      documentWithStatus("key1", "draft", null),
      documentWithStatus("key2", "submitted", null),
      documentWithStatus("key3", "archived", null)
    )
      .compose(v ->
        dbHandler.findAll(DB_HANDLER_COLLECTION, null, null, "key", "asc", "draft,submitted")
      )
      .onSuccess(allDocuments -> {
        assertEquals(2, allDocuments.size());
        assertEquals("key1", allDocuments.get(0).getString("key"));
        assertEquals("key2", allDocuments.get(1).getString("key"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithPaginationAndSortingAsc(VertxTestContext testContext) {
    insertDocuments(
      DB_HANDLER_COLLECTION,
      documentWithStatus("testCountAllDocumentsKey1", "draft", "user-1"),
      documentWithStatus("testCountAllDocumentsKey2", "draft", "user-1"),
      documentWithStatus("testCountAllDocumentsKey3", "draft", "user-1")
    )
      .compose(v -> dbHandler.findAll(DB_HANDLER_COLLECTION, 1, 2, "key", "asc", "draft", "user-1"))
      .onSuccess(allDocuments -> {
        assertEquals(2, allDocuments.size());
        assertEquals("testCountAllDocumentsKey1", allDocuments.get(0).getString("key"));
        assertEquals("testCountAllDocumentsKey2", allDocuments.get(1).getString("key"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindEntitiesWithProjectionAndSort(VertxTestContext testContext) {
    insertDocuments(
      ENTITY_COLLECTION,
      new JsonObject().put("name", "beta").put("category", "A").put("secret", "x"),
      new JsonObject().put("name", "alpha").put("category", "A").put("secret", "y"),
      new JsonObject().put("name", "gamma").put("category", "B").put("secret", "z")
    )
      .compose(v ->
        dbHandler.findEntities(
          ENTITY_COLLECTION,
          1,
          2,
          "name",
          "asc",
          new JsonObject().put("category", "A"),
          new JsonObject().put("name", 1).put("_id", 0)
        )
      )
      .onSuccess(result -> {
        assertEquals(2, result.size());
        assertEquals("alpha", result.get(0).getString("name"));
        assertNull(result.get(0).getString("secret"));
        assertEquals("beta", result.get(1).getString("name"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDataPermitGrantedDatasetsPerApplication(VertxTestContext testContext) {
    insertDocuments(
      APPLICATION_COLLECTION,
      grantedPermitApplication("app-1", "App One", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z"),
      grantedPermitApplication("app-2", "App Two", "2024-02-01T00:00:00Z", "2024-02-02T00:00:00Z"),
      nonGrantedPermitApplication("app-3", "App Three")
    )
      .compose(v ->
        dbHandler.findDataPermitGrantedDatasetsPerApplication(
          APPLICATION_COLLECTION,
          1,
          10,
          "date_created",
          "asc",
          new JsonObject().put("owner", "owner-1")
        )
      )
      .onSuccess(result -> {
        assertEquals(2, result.size());
        assertEquals("App One", result.get(0).getString("applicationTitle"));
        assertEquals(1, result.get(0).getJsonArray("permits").size());
        assertEquals(
          "GRANTED",
          result.get(0).getJsonArray("permits").getJsonObject(0).getString("permit_status")
        );
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDecisionIssuedDatasetsPerApplication(VertxTestContext testContext) {
    insertDocuments(
      APPLICATION_COLLECTION,
      decisionApplication(
        "app-1",
        "positive",
        "POSITIVE_DECISION_ISSUED",
        "2024-01-11T00:00:00Z",
        "2024-01-10T00:00:00Z"
      ),
      decisionApplication(
        "app-2",
        "negative",
        "NEGATIVE_DECISION_ISSUED",
        "2024-01-12T00:00:00Z",
        "2024-01-10T00:00:00Z"
      )
    )
      .compose(v ->
        dbHandler.findDecisionIssuedDatasetsPerApplication(
          APPLICATION_COLLECTION,
          1,
          10,
          "updatedAt",
          "desc",
          new JsonObject().put("owner", "owner-1"),
          Boolean.TRUE
        )
      )
      .onSuccess(result -> {
        assertEquals(1, result.size());
        assertEquals("positive", result.get(0).getString("hdab"));
        assertEquals(
          "POSITIVE_DECISION_ISSUED",
          result.get(0).getJsonObject("latestDecision").getString("datasetStatus")
        );
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDatasetsPerApplicationWithAndFilters(VertxTestContext testContext) {
    insertDocuments(
      APPLICATION_COLLECTION,
      datasetApplication(
        "app-1",
        "Application One",
        "dataset-1",
        "allowed-hdab",
        "POSITIVE_DECISION_ISSUED",
        "GRANTED"
      ),
      datasetApplication(
        "app-2",
        "Application Two",
        "dataset-2",
        "other-hdab",
        "NEGATIVE_DECISION_ISSUED",
        "PENDING"
      )
    )
      .compose(v ->
        dbHandler.findDatasetsPerApplication(
          APPLICATION_COLLECTION,
          1,
          10,
          "date_modified",
          "asc",
          new JsonObject().put(
            "$and",
            new JsonArray()
              .add(new JsonObject().put("owner", "owner-1"))
              .add(
                new JsonObject().put(
                  "datasets.decisions",
                  new JsonObject().put(
                    "$elemMatch",
                    new JsonObject().put("datasetStatus", "POSITIVE_DECISION_ISSUED")
                  )
                )
              )
              .add(
                new JsonObject().put(
                  "datasets.permits",
                  new JsonObject().put(
                    "$elemMatch",
                    new JsonObject().put("permit_status", "GRANTED")
                  )
                )
              )
          )
        )
      )
      .onSuccess(result -> {
        assertEquals(1, result.size());
        assertEquals("Application One", result.get(0).getString("applicationTitle"));
        assertEquals("allowed-hdab", result.get(0).getJsonObject("dataset").getString("hdab"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteById(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testDeleteByIdKey")
      .put("value", "testDeleteByIdValue");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id -> dbHandler.deleteById(DB_HANDLER_COLLECTION, new JsonObject().put("_id", id)))
      .onSuccess(result -> {
        assertEquals(204, result);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailDeleteByIdThatDoesNotExist(VertxTestContext testContext) {
    dbHandler
      .deleteById(DB_HANDLER_COLLECTION, new JsonObject().put("_id", "66f2b92dd764b50aab4f7360"))
      .onSuccess(result -> {
        assertEquals(404, result);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailDeleteAllButNoneExists(VertxTestContext testContext) {
    dbHandler
      .deleteAll(DB_HANDLER_COLLECTION, new JsonObject().put("value", "sameValue"))
      .onSuccess(result -> {
        assertEquals(404, result);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountAllDocuments(VertxTestContext testContext) {
    insertDocuments(
      DB_HANDLER_COLLECTION,
      documentWithStatus("key1", "draft", "user-1"),
      documentWithStatus("key2", "draft", "user-1")
    )
      .compose(v -> dbHandler.countAllDocuments(DB_HANDLER_COLLECTION, "draft", "user-1"))
      .onSuccess(count -> {
        assertEquals(2, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountAllDocumentsWithoutUserId(VertxTestContext testContext) {
    insertDocuments(
      DB_HANDLER_COLLECTION,
      documentWithStatus("key1", "draft", null),
      documentWithStatus("key2", "submitted", null),
      documentWithStatus("key3", "archived", null)
    )
      .compose(v -> dbHandler.countAllDocuments(DB_HANDLER_COLLECTION, "draft,submitted"))
      .onSuccess(count -> {
        assertEquals(2, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailCountAllDocumentsButNoneExists(VertxTestContext testContext) {
    dbHandler
      .countAllDocuments(DB_HANDLER_COLLECTION, "draft", "user-1")
      .onSuccess(count -> {
        assertEquals(0, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountAll(VertxTestContext testContext) {
    insertDocuments(
      ENTITY_COLLECTION,
      new JsonObject().put("category", "A"),
      new JsonObject().put("category", "A"),
      new JsonObject().put("category", "B")
    )
      .compose(v -> dbHandler.countAll(ENTITY_COLLECTION, new JsonObject().put("category", "A")))
      .onSuccess(count -> {
        assertEquals(2, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCheckDocumentExists(VertxTestContext testContext) {
    JsonObject document = new JsonObject()
      .put("key", "testCheckDocumentExistsKey")
      .put("value", "testCheckDocumentExistsValue")
      .put("userId", "user-1");

    dbHandler
      .create(DB_HANDLER_COLLECTION, document)
      .compose(id ->
        dbHandler
          .checkDocumentExists(
            new JsonObject().put("_id", id).put("userId", "user-1"),
            DB_HANDLER_COLLECTION
          )
          .future()
      )
      .onSuccess(exists -> {
        assertTrue(exists);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailCheckDocumentExistsButDoesNotExist(VertxTestContext testContext) {
    dbHandler
      .checkDocumentExists(
        new JsonObject().put("_id", "66f2b92dd764b50aab4f7360").put("userId", "user-1"),
        DB_HANDLER_COLLECTION
      )
      .future()
      .onSuccess(exists -> {
        assertFalse(exists);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateLinkableFieldsSuccess(VertxTestContext testContext) {
    insertDocuments(
      LINKABLE_COLLECTION,
      new JsonObject().put("objectId", "12345").put("linkable", true),
      new JsonObject().put("objectId", "12345").put("linkable", true),
      new JsonObject().put("objectId", "other").put("linkable", true)
    )
      .compose(v -> dbHandler.updateLinkableFields(LINKABLE_COLLECTION, "12345"))
      .compose(v ->
        dbHandler.findAllByField(
          LINKABLE_COLLECTION,
          new JsonObject().put("objectId", "12345"),
          true
        )
      )
      .onSuccess(result -> {
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(doc -> doc.getBoolean("linkable")));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindOne(VertxTestContext testContext) {
    JsonObject document = new JsonObject().put("externalId", "one-1").put("value", "expected");

    dbHandler
      .create(ENTITY_COLLECTION, document)
      .compose(id ->
        dbHandler.findOne(ENTITY_COLLECTION, new JsonObject().put("externalId", "one-1"))
      )
      .onSuccess(result -> {
        assertEquals("expected", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateMany(VertxTestContext testContext) {
    insertDocuments(
      UPDATE_COLLECTION,
      new JsonObject().put("groupId", "g-1").put("flag", true),
      new JsonObject().put("groupId", "g-1").put("flag", true),
      new JsonObject().put("groupId", "g-2").put("flag", true)
    )
      .compose(v ->
        dbHandler.updateMany(
          UPDATE_COLLECTION,
          new JsonObject().put("groupId", "g-1"),
          new JsonObject().put("$set", new JsonObject().put("flag", false))
        )
      )
      .compose(v ->
        dbHandler.findAllByField(UPDATE_COLLECTION, new JsonObject().put("groupId", "g-1"), true)
      )
      .onSuccess(result -> {
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(doc -> doc.getBoolean("flag")));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateByField(VertxTestContext testContext) {
    JsonObject document = new JsonObject().put("externalId", "field-1").put("name", "before");

    dbHandler
      .create(UPDATE_COLLECTION, document)
      .compose(id ->
        dbHandler.updateByField(
          UPDATE_COLLECTION,
          new JsonObject().put("externalId", "field-1").put("name", "after"),
          "externalId",
          "field-1"
        )
      )
      .compose(result ->
        dbHandler.findOne(UPDATE_COLLECTION, new JsonObject().put("externalId", "field-1"))
      )
      .onSuccess(result -> {
        assertEquals("after", result.getString("name"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllByFieldExactMatch(VertxTestContext testContext) {
    insertDocuments(
      ENTITY_COLLECTION,
      new JsonObject().put("name", "Alpha").put("category", "A"),
      new JsonObject().put("name", "Alphabet").put("category", "B")
    )
      .compose(v ->
        dbHandler.findAllByField(ENTITY_COLLECTION, new JsonObject().put("name", "Alpha"), true)
      )
      .onSuccess(result -> {
        assertEquals(1, result.size());
        assertEquals("Alpha", result.get(0).getString("name"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllByFieldLikeMatch(VertxTestContext testContext) {
    insertDocuments(
      ENTITY_COLLECTION,
      new JsonObject().put("name", "Alpha").put("category", "A"),
      new JsonObject().put("name", "alphabet").put("category", "B")
    )
      .compose(v ->
        dbHandler.findAllByField(ENTITY_COLLECTION, new JsonObject().put("name", "alp"), false)
      )
      .onSuccess(result -> {
        assertEquals(2, result.size());
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountDecisionIssuedDatasetsPerApplication(VertxTestContext testContext) {
    insertDocuments(
      APPLICATION_COLLECTION,
      decisionApplication(
        "app-1",
        "positive",
        "POSITIVE_DECISION_ISSUED",
        "2024-01-11T00:00:00Z",
        "2024-01-10T00:00:00Z"
      ),
      decisionApplication(
        "app-2",
        "negative",
        "NEGATIVE_DECISION_ISSUED",
        "2024-01-12T00:00:00Z",
        "2024-01-10T00:00:00Z"
      )
    )
      .compose(v ->
        dbHandler.countDecisionIssuedDatasetsPerApplication(
          APPLICATION_COLLECTION,
          new JsonObject().put("owner", "owner-1"),
          null
        )
      )
      .onSuccess(count -> {
        assertEquals(2, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountDatasetsPerDataPermitGrantedDatasetApplication(VertxTestContext testContext) {
    insertDocuments(
      APPLICATION_COLLECTION,
      grantedPermitApplication("app-1", "App One", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z"),
      grantedPermitApplication("app-2", "App Two", "2024-02-01T00:00:00Z", "2024-02-02T00:00:00Z")
    )
      .compose(v ->
        dbHandler.countDatasetsPerDataPermitGrantedDatasetApplication(
          APPLICATION_COLLECTION,
          new JsonObject().put("owner", "owner-1")
        )
      )
      .onSuccess(count -> {
        assertEquals(2, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountDatasetsPerApplication(VertxTestContext testContext) {
    insertDocuments(
      APPLICATION_COLLECTION,
      datasetApplication(
        "app-1",
        "Application One",
        "dataset-1",
        "allowed-hdab",
        "POSITIVE_DECISION_ISSUED",
        "GRANTED"
      ),
      datasetApplication(
        "app-2",
        "Application Two",
        "dataset-2",
        "other-hdab",
        "NEGATIVE_DECISION_ISSUED",
        "PENDING"
      )
    )
      .compose(v ->
        dbHandler.countDatasetsPerApplication(
          APPLICATION_COLLECTION,
          new JsonObject().put("title", "Application One").put("datasets.hdab", "allowed-hdab")
        )
      )
      .onSuccess(count -> {
        assertEquals(1, count);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAuditLogs(VertxTestContext testContext) {
    insertDocuments(
      AUDIT_LOG_COLLECTION,
      new JsonObject().put("action", "create").put("timestamp", 2).put("entity", "A"),
      new JsonObject().put("action", "update").put("timestamp", 3).put("entity", "A"),
      new JsonObject().put("action", "delete").put("timestamp", 1).put("entity", "A")
    )
      .compose(v ->
        dbHandler.findAuditLogs(
          AUDIT_LOG_COLLECTION,
          1,
          2,
          "timestamp",
          "desc",
          new JsonObject().put("entity", "A")
        )
      )
      .onSuccess(result -> {
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).getInteger("timestamp"));
        assertEquals(2, result.get(1).getInteger("timestamp"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  void testCreateFailurePropagates() {
    RuntimeException boom = new RuntimeException("save failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .save(anyString(), any(JsonObject.class), any());

    Future<String> result = mockedHandler.create(DB_HANDLER_COLLECTION, new JsonObject());

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testUpdateFailurePropagates() {
    RuntimeException boom = new RuntimeException("update failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class), any());

    Future<JsonObject> result = mockedHandler.update(
      DB_HANDLER_COLLECTION,
      new JsonObject(),
      new JsonObject()
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindAllFailurePropagates() {
    RuntimeException boom = new RuntimeException("findAll failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .findWithOptions(anyString(), any(JsonObject.class), any(FindOptions.class), any());

    Future<List<JsonObject>> result = mockedHandler.findAll(
      DB_HANDLER_COLLECTION,
      1,
      10,
      "key",
      "asc",
      "draft"
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindEntitiesFailurePropagates() {
    RuntimeException boom = new RuntimeException("findEntities failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .findWithOptions(anyString(), any(JsonObject.class), any(FindOptions.class), any());

    Future<List<JsonObject>> result = mockedHandler.findEntities(
      ENTITY_COLLECTION,
      1,
      10,
      "name",
      "asc",
      new JsonObject(),
      new JsonObject().put("name", 1)
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindDataPermitGrantedDatasetsPerApplicationFailurePropagates() {
    TestReadStream stream = new TestReadStream();

    when(mockedMongoClient.aggregate(eq(APPLICATION_COLLECTION), any(JsonArray.class))).thenReturn(
      stream
    );

    Future<List<JsonObject>> result = mockedHandler.findDataPermitGrantedDatasetsPerApplication(
      APPLICATION_COLLECTION,
      1,
      10,
      "date_created",
      "asc",
      new JsonObject()
    );

    RuntimeException boom = new RuntimeException("aggregate failed");
    stream.fail(boom);

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindDecisionIssuedDatasetsPerApplicationFailurePropagates() {
    TestReadStream stream = new TestReadStream();

    when(mockedMongoClient.aggregate(eq(APPLICATION_COLLECTION), any(JsonArray.class))).thenReturn(
      stream
    );

    Future<List<JsonObject>> result = mockedHandler.findDecisionIssuedDatasetsPerApplication(
      APPLICATION_COLLECTION,
      1,
      10,
      "updatedAt",
      "desc",
      new JsonObject(),
      Boolean.TRUE
    );

    RuntimeException boom = new RuntimeException("aggregate failed");
    stream.fail(boom);

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindDatasetsPerApplicationFailurePropagates() {
    TestReadStream stream = new TestReadStream();

    when(mockedMongoClient.aggregate(eq(APPLICATION_COLLECTION), any(JsonArray.class))).thenReturn(
      stream
    );

    Future<List<JsonObject>> result = mockedHandler.findDatasetsPerApplication(
      APPLICATION_COLLECTION,
      1,
      10,
      "date_modified",
      "asc",
      new JsonObject()
    );

    RuntimeException boom = new RuntimeException("aggregate failed");
    stream.fail(boom);

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testCountDecisionIssuedDatasetsPerApplicationFailurePropagates() {
    @SuppressWarnings("unchecked")
    ReadStream<JsonObject> aggregateStream = mock(ReadStream.class);
    RuntimeException boom = new RuntimeException("count failed");

    when(mockedMongoClient.aggregate(eq(APPLICATION_COLLECTION), any(JsonArray.class))).thenReturn(
      aggregateStream
    );
    when(aggregateStream.collect(any(Collector.class))).thenReturn(Future.failedFuture(boom));

    Future<Integer> result = mockedHandler.countDecisionIssuedDatasetsPerApplication(
      APPLICATION_COLLECTION,
      new JsonObject(),
      Boolean.TRUE
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testCountDatasetsPerDataPermitGrantedDatasetApplicationFailurePropagates() {
    TestReadStream stream = new TestReadStream();

    when(mockedMongoClient.aggregate(eq(APPLICATION_COLLECTION), any(JsonArray.class))).thenReturn(
      stream
    );

    Future<Integer> result = mockedHandler.countDatasetsPerDataPermitGrantedDatasetApplication(
      APPLICATION_COLLECTION,
      new JsonObject()
    );

    RuntimeException boom = new RuntimeException("count failed");
    stream.fail(boom);

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testCountDatasetsPerApplicationFailurePropagates() {
    TestReadStream stream = new TestReadStream();

    when(mockedMongoClient.aggregate(eq(APPLICATION_COLLECTION), any(JsonArray.class))).thenReturn(
      stream
    );

    Future<Integer> result = mockedHandler.countDatasetsPerApplication(
      APPLICATION_COLLECTION,
      new JsonObject().put("datasets.hdab", "allowed-hdab")
    );

    RuntimeException boom = new RuntimeException("count failed");
    stream.fail(boom);

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindAuditLogsFailurePropagates() {
    RuntimeException boom = new RuntimeException("audit failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .findWithOptions(anyString(), any(JsonObject.class), any(FindOptions.class), any());

    Future<List<JsonObject>> result = mockedHandler.findAuditLogs(
      AUDIT_LOG_COLLECTION,
      1,
      10,
      "timestamp",
      "desc",
      new JsonObject()
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testCountAllDocumentsFailurePropagates() {
    RuntimeException boom = new RuntimeException("countAllDocuments failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<Long>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .count(anyString(), any(JsonObject.class), any());

    Future<Integer> result = mockedHandler.countAllDocuments(DB_HANDLER_COLLECTION, "draft");

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testCountAllFailurePropagates() {
    RuntimeException boom = new RuntimeException("countAll failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<Long>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .count(anyString(), any(JsonObject.class), any());

    Future<Integer> result = mockedHandler.countAll(ENTITY_COLLECTION, new JsonObject());

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindOneFailurePropagates() {
    RuntimeException boom = new RuntimeException("findOne failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .findOne(anyString(), any(JsonObject.class), any(), any());

    Future<JsonObject> result = mockedHandler.findOne(ENTITY_COLLECTION, new JsonObject());

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindByFieldFailurePropagates() {
    RuntimeException boom = new RuntimeException("findByField failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .findOne(anyString(), any(JsonObject.class), any(), any());

    Future<JsonObject> result = mockedHandler.findByField(
      DB_HANDLER_COLLECTION,
      new JsonObject().put("key", "value")
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testDeleteByFieldFailurePropagates() {
    RuntimeException boom = new RuntimeException("deleteByField failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .findOneAndDelete(anyString(), any(JsonObject.class), any());

    Future<JsonObject> result = mockedHandler.deleteByField(
      DB_HANDLER_COLLECTION,
      new JsonObject().put("key", "value")
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testDeleteByIdFailurePropagates() {
    RuntimeException boom = new RuntimeException("deleteById failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<io.vertx.ext.mongo.MongoClientDeleteResult>> handler =
        invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .removeDocument(anyString(), any(JsonObject.class), any());

    Future<Integer> result = mockedHandler.deleteById(
      DB_HANDLER_COLLECTION,
      new JsonObject().put("_id", "missing")
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testDeleteAllFailurePropagates() {
    RuntimeException boom = new RuntimeException("deleteAll failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<io.vertx.ext.mongo.MongoClientDeleteResult>> handler =
        invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .removeDocuments(anyString(), any(JsonObject.class), any());

    Future<Integer> result = mockedHandler.deleteAll(
      DB_HANDLER_COLLECTION,
      new JsonObject().put("key", "value")
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testCheckDocumentExistsFailurePropagates() {
    RuntimeException boom = new RuntimeException("checkDocumentExists failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<Long>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .count(anyString(), any(JsonObject.class), any());

    Future<Boolean> result = mockedHandler
      .checkDocumentExists(new JsonObject().put("_id", "missing"), DB_HANDLER_COLLECTION)
      .future();

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testUpdateManyFailurePropagates() {
    RuntimeException boom = new RuntimeException("updateMany failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .updateCollectionWithOptions(
        anyString(),
        any(JsonObject.class),
        any(JsonObject.class),
        any(UpdateOptions.class),
        any()
      );

    Future<Object> result = mockedHandler.updateMany(
      UPDATE_COLLECTION,
      new JsonObject(),
      new JsonObject()
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testUpdateByFieldFailurePropagates() {
    RuntimeException boom = new RuntimeException("updateByField failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class), any());

    Future<JsonObject> result = mockedHandler.updateByField(
      UPDATE_COLLECTION,
      new JsonObject().put("externalId", "field-1"),
      "externalId",
      "field-1"
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindAllByFieldFailurePropagates() {
    RuntimeException boom = new RuntimeException("findAllByField failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .find(anyString(), any(JsonObject.class), any());

    Future<List<JsonObject>> result = mockedHandler.findAllByField(
      ENTITY_COLLECTION,
      new JsonObject().put("name", "Alpha"),
      true
    );

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testUpdateLinkableFieldsFailurePropagates() {
    RuntimeException boom = new RuntimeException("updateLinkableFields failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4);
      handler.handle(Future.failedFuture(boom));
      return null;
    })
      .when(mockedMongoClient)
      .updateCollectionWithOptions(
        anyString(),
        any(JsonObject.class),
        any(JsonObject.class),
        any(UpdateOptions.class),
        any()
      );

    Future<Object> result = mockedHandler.updateLinkableFields(LINKABLE_COLLECTION, "12345");

    assertTrue(result.failed());
    assertSame(boom, result.cause());
  }

  @Test
  void testFindAllByFieldBuildsRegexQueryWhenExactMatchIsFalse() {
    List<JsonObject> documents = List.of(new JsonObject().put("name", "Alpha"));
    ArgumentCaptor<JsonObject> queryCaptor = ArgumentCaptor.forClass(JsonObject.class);

    doAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture(documents));
      return null;
    })
      .when(mockedMongoClient)
      .find(anyString(), any(JsonObject.class), any());

    Future<List<JsonObject>> result = mockedHandler.findAllByField(
      ENTITY_COLLECTION,
      new JsonObject().put("name", "alp"),
      false
    );

    assertTrue(result.succeeded());
    verify(mockedMongoClient).find(eq(ENTITY_COLLECTION), queryCaptor.capture(), any());
    assertEquals("alp", queryCaptor.getValue().getJsonObject("name").getString("$regex"));
    assertEquals("i", queryCaptor.getValue().getJsonObject("name").getString("$options"));
  }

  private static void closeMongoClient() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    mongoClient
      .close()
      .onComplete(result -> {
        if (result.failed()) {
          error.set(result.cause());
        }
        latch.countDown();
      });

    assertTrue(latch.await(30, TimeUnit.SECONDS));
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  private static void closeVertx() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    vertx
      .close()
      .onComplete(result -> {
        if (result.failed()) {
          error.set(result.cause());
        }
        latch.countDown();
      });

    assertTrue(latch.await(30, TimeUnit.SECONDS));
    if (error.get() != null) {
      throw new RuntimeException(error.get());
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

      assertTrue(latch.await(30, TimeUnit.SECONDS));
      if (error.get() != null) {
        throw new RuntimeException(error.get());
      }
    }
  }

  private Future<Void> insertDocuments(String collection, JsonObject... documents) {
    Future<Void> chain = Future.succeededFuture();
    for (JsonObject document : documents) {
      chain = chain.compose(v -> dbHandler.create(collection, document).mapEmpty());
    }
    return chain;
  }

  private JsonObject documentWithStatus(String key, String status, String userId) {
    JsonObject document = new JsonObject()
      .put("key", key)
      .put("value", key + "Value")
      .put("status", new JsonObject().put("status", status));
    if (userId != null) {
      document.put("userId", userId);
    }
    return document;
  }

  private JsonObject grantedPermitApplication(
    String id,
    String title,
    String createdAt,
    String modifiedAt
  ) {
    return new JsonObject()
      .put("_id", id)
      .put("title", title)
      .put("owner", "owner-1")
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put("date_created", createdAt)
      .put("date_modified", modifiedAt)
      .put(
        "form",
        new JsonObject().put("section2", new JsonObject().put("projectName", title + " Project"))
      )
      .put(
        "datasets",
        new JsonArray().add(
          new JsonObject()
            .put("dataset_id", id + "-dataset")
            .put("hdab", title.toLowerCase())
            .put(
              "permits",
              new JsonArray()
                .add(
                  new JsonObject().put("permit_status", "GRANTED").put("permit_id", id + "-permit")
                )
                .add(
                  new JsonObject().put("permit_status", "PENDING").put("permit_id", id + "-pending")
                )
            )
        )
      );
  }

  private JsonObject nonGrantedPermitApplication(String id, String title) {
    return new JsonObject()
      .put("_id", id)
      .put("title", title)
      .put("owner", "owner-1")
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put("date_created", "2024-03-01T00:00:00Z")
      .put("date_modified", "2024-03-02T00:00:00Z")
      .put(
        "form",
        new JsonObject().put("section2", new JsonObject().put("projectName", title + " Project"))
      )
      .put(
        "datasets",
        new JsonArray().add(
          new JsonObject()
            .put("dataset_id", id + "-dataset")
            .put("hdab", title.toLowerCase())
            .put("permits", new JsonArray().add(new JsonObject().put("permit_status", "PENDING")))
        )
      );
  }

  private JsonObject decisionApplication(
    String id,
    String hdab,
    String status,
    String newestDecisionDate,
    String olderDecisionDate
  ) {
    return new JsonObject()
      .put("_id", id)
      .put("title", "Decision " + id)
      .put("owner", "owner-1")
      .put("updatedAt", newestDecisionDate)
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put(
        "datasets",
        new JsonArray().add(
          new JsonObject()
            .put("dataset_id", id + "-dataset")
            .put("hdab", hdab)
            .put(
              "decisions",
              new JsonArray()
                .add(
                  new JsonObject()
                    .put("datasetStatus", status)
                    .put("decision_date_received", olderDecisionDate)
                )
                .add(
                  new JsonObject()
                    .put("datasetStatus", status)
                    .put("decision_date_received", newestDecisionDate)
                )
            )
        )
      );
  }

  private JsonObject datasetApplication(
    String id,
    String title,
    String datasetId,
    String hdab,
    String decisionStatus,
    String permitStatus
  ) {
    return new JsonObject()
      .put("_id", id)
      .put("title", title)
      .put("owner", "owner-1")
      .put("status", new JsonObject().put("status", "SUBMITTED"))
      .put("date_submitted", "2024-01-01T00:00:00Z")
      .put("date_created", "2024-01-01T00:00:00Z")
      .put("date_modified", "2024-01-02T00:00:00Z")
      .put(
        "datasets",
        new JsonArray().add(
          new JsonObject()
            .put("dataset_id", datasetId)
            .put("hdab", hdab)
            .put(
              "decisions",
              new JsonArray().add(
                new JsonObject()
                  .put("datasetStatus", decisionStatus)
                  .put("decision_date_received", "2024-01-02T00:00:00Z")
              )
            )
            .put(
              "permits",
              new JsonArray().add(
                new JsonObject()
                  .put("permit_status", permitStatus)
                  .put("permit_id", datasetId + "-permit")
              )
            )
        )
      );
  }

  private void assertNotFound(Throwable throwable, VertxTestContext testContext) {
    if (throwable instanceof ServiceException exception) {
      assertEquals(404, exception.failureCode());
      assertEquals(HttpResponseStatus.NOT_FOUND.reasonPhrase(), exception.getMessage());
      testContext.completeNow();
      return;
    }
    testContext.failNow(new AssertionError("Expected ServiceException but got: " + throwable));
  }

  private static final class TestReadStream implements ReadStream<JsonObject> {

    private Handler<Throwable> exceptionHandler;

    @Override
    public ReadStream<JsonObject> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public ReadStream<JsonObject> handler(Handler<JsonObject> handler) {
      return this;
    }

    @Override
    public ReadStream<JsonObject> pause() {
      return this;
    }

    @Override
    public ReadStream<JsonObject> resume() {
      return this;
    }

    @Override
    public ReadStream<JsonObject> fetch(long amount) {
      return this;
    }

    @Override
    public ReadStream<JsonObject> endHandler(Handler<Void> handler) {
      return this;
    }

    void fail(Throwable throwable) {
      if (exceptionHandler != null) {
        exceptionHandler.handle(throwable);
      }
    }
  }
}
