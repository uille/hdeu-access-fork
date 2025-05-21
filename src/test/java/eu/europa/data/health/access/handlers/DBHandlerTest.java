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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
class DBHandlerTest {

  private static MongoDBContainer mongoDBContainer;
  private static MongoClient mongoClient;
  private static DBHandler dbHandler;

  private static final String JUNIT_TEST_DATABASE = "junitTestDatabase";
  private static final String DB_HANDLER_COLLECTION = "dbHandlerCollection";
  private boolean skipCleanup = false;

  Logger logger = (Logger) LoggerFactory.getLogger(DBHandler.class);


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

    testContext.completeNow();
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    if (skipCleanup) {
      testContext.completeNow();
      return; // skip teardown logic
    }
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
  void testCreateDocument(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testCreateDocumentKey").put("value", "testCreateDocumentValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .onSuccess(id -> {
        assertNotNull(id);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailCreateDocumentWithPredefinedId(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("_id", "66f2848812955e42f75812b0").put("key", "testCreateDocumentKey").put("value", "testCreateDocumentValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .onSuccess(id -> {
        assertNull(id);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }



  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testUpdateDocument(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testUpdateDocumentKey").put("value", "testUpdateDocumentValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .onSuccess(id -> {
        assertNotNull(id);
        JsonObject updateDocument = new JsonObject().put("key", "testUpdateDocumentKeyUpdated").put("value", "testUpdateDocumentValueUpdated");
        dbHandler.update(DB_HANDLER_COLLECTION, updateDocument, document)
          .onSuccess(res -> {
            assertNotNull(res.encode());
            assertEquals("1", res.getString("docMatched"));
            assertEquals("1", res.getString("docModified"));
            assertNull(res.getString("docUpsertedId"));
            testContext.completeNow();
          })
          .onFailure(testContext::failNow);
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailUpdateDocumentThatDoesNotExist(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testUpdateDocumentKey").put("value", "testUpdateDocumentValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .onSuccess(id -> {
        assertNotNull(id);
        JsonObject updateDocument = new JsonObject().put("key", "testUpdateDocumentKeyUpdated").put("value", "testUpdateDocumentValueUpdated");
        document.put("_id", "66f2b92dd764b50aab4f7360");
        dbHandler.update(DB_HANDLER_COLLECTION, updateDocument, document)
          .onSuccess(res -> {
            assertNotNull(res.encode());
            assertEquals("0", res.getString("docMatched"));
            assertEquals("0", res.getString("docModified"));
            assertNull(res.getString("docUpsertedId"));
            testContext.completeNow();
          })
          .onFailure(testContext::failNow);
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDocumentByField(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testFindDocumentByFieldKey")
      .put("value", "testFindDocumentByFieldValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .onSuccess(id -> {
        assertNotNull(id);
        JsonObject documentToFindByField = new JsonObject().put("key",
          "testFindDocumentByFieldKey");
        dbHandler.findByField(DB_HANDLER_COLLECTION, documentToFindByField)
          .onSuccess(result -> {
            assertNotNull(result);
            assertEquals("testFindDocumentByFieldKey", result.getString("key"));
            assertEquals("testFindDocumentByFieldValue", result.getString("value"));
            testContext.completeNow();
          });
      }).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailFindDocumentByFieldThatDoesNotExist(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testFindDocumentByFieldKey").put("value", "testFindDocumentByFieldValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        JsonObject documentToFindByField = new JsonObject().put("key", "testFailFindDocumentByFieldThatDoesNotExistKey");
        return dbHandler.findByField(DB_HANDLER_COLLECTION, documentToFindByField);
      })
      .onFailure(throwable -> {
        if (throwable instanceof ServiceException) {
          ServiceException exception = (ServiceException) throwable;
          assertEquals(404, exception.failureCode());
          assertEquals(HttpResponseStatus.NOT_FOUND.reasonPhrase(), exception.getMessage());
          testContext.completeNow();
        } else {
          testContext.failNow(new AssertionError("Expected ServiceException but got: " + throwable));
        }
      });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteDocumentByField(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testDeleteDocumentByFieldKey").put("value", "testDeleteDocumentByFieldValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        JsonObject documentToDeleteByField = new JsonObject().put("key", "testDeleteDocumentByFieldKey");
        return dbHandler.deleteByField(DB_HANDLER_COLLECTION, documentToDeleteByField);
      })
      .onSuccess(result -> {
        assertNotNull(result);
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

    JsonObject document = new JsonObject().put("key", "testDeleteDocumentByFieldKey").put("value", "testDeleteDocumentByFieldValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        JsonObject documentToDeleteByField = new JsonObject().put("key", "testFailDeleteDocumentByFieldThatDoesNotExistKey");
        return dbHandler.deleteByField(DB_HANDLER_COLLECTION, documentToDeleteByField);
      })
      .onFailure(throwable -> {
        if (throwable instanceof ServiceException) {
          ServiceException exception = (ServiceException) throwable;
          assertEquals(404, exception.failureCode());
          assertEquals(HttpResponseStatus.NOT_FOUND.reasonPhrase(), exception.getMessage());
          testContext.completeNow();
        } else {
          testContext.failNow(new AssertionError("Expected ServiceException but got: " + throwable));
        }
      });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindDocumentById(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testFindDocumentByIdKey").put("value", "testFindDocumentByIdValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        return dbHandler.findById(DB_HANDLER_COLLECTION, id);
      })
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals("testFindDocumentByIdKey", result.getString("key"));
        assertEquals("testFindDocumentByIdValue", result.getString("value"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailFindDocumentByIdThatDoesNotExist(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testFindDocumentByIdKey").put("value", "testFindDocumentByIdValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        return dbHandler.findById(DB_HANDLER_COLLECTION, "66f2b92dd764b50aab4f7360");
      })
      .onFailure(throwable -> {
        if (throwable instanceof ServiceException) {
          ServiceException exception = (ServiceException) throwable;
          assertEquals(404, exception.failureCode());
          assertEquals(HttpResponseStatus.NOT_FOUND.reasonPhrase(), exception.getMessage());
          testContext.completeNow();
        } else {
          testContext.failNow(new AssertionError("Expected ServiceException but got: " + throwable));
        }
      });
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithoutPagination(VertxTestContext testContext) {

    JsonObject draftStatus = new JsonObject().put("status", "draft");
    JsonObject document1 = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2 = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");

    dbHandler.create(DB_HANDLER_COLLECTION, document1)
      .compose(result1 -> {
        assertNotNull(result1);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2);
      })
      .compose(result2 -> {
        assertNotNull(result2);
        return dbHandler.findAll(DB_HANDLER_COLLECTION, null, null, null, null, null, "1eadc727-9da3-44e9-80a0-867d291f3bf7");
      })
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(2, allDocuments.size());
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithPagination(VertxTestContext testContext) {

    JsonObject draftStatus = new JsonObject().put("status", "draft");
    JsonObject document1 = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2 = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document3 = new JsonObject().put("key", "testCountAllDocumentsKey3").put("value", "testCountAllDocumentsValue3").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");

    dbHandler.create(DB_HANDLER_COLLECTION, document1)
      .compose(result1 -> {
        assertNotNull(result1);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2);
      })
      .compose(result2 -> {
        assertNotNull(result2);
        return dbHandler.create(DB_HANDLER_COLLECTION, document3);
      })
      .compose(result3 -> {
        assertNotNull(result3);
        return dbHandler.findAll(DB_HANDLER_COLLECTION, 1, 2, null, null, null, "1eadc727-9da3-44e9-80a0-867d291f3bf7");
      })
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(2, allDocuments.size());
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithPaginationAndSortingAsc(VertxTestContext testContext) {

    JsonObject draftStatus = new JsonObject().put("status", "draft");
    JsonObject otherStatus = new JsonObject().put("status", "other");
    JsonObject document1 = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2 = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document3 = new JsonObject().put("key", "testCountAllDocumentsKey3").put("value", "testCountAllDocumentsValue3").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document1OtherStatus = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", otherStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2OtherStatus = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", otherStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");

    dbHandler.create(DB_HANDLER_COLLECTION, document1)
      .compose(result1 -> {
        assertNotNull(result1);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2);
      })
      .compose(result2 -> {
        assertNotNull(result2);
        return dbHandler.create(DB_HANDLER_COLLECTION, document3);
      })
      .compose(result3 -> {
        assertNotNull(result3);
        return dbHandler.create(DB_HANDLER_COLLECTION, document1OtherStatus);
      })
      .compose(result4 -> {
        assertNotNull(result4);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2OtherStatus);
      })
      .compose(result5 -> {
        assertNotNull(result5);
        return dbHandler.findAll(DB_HANDLER_COLLECTION, 1, 2, "key", "asc", "draft", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
      })
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(2, allDocuments.size());
        assertEquals("testCountAllDocumentsKey1", allDocuments.get(0).getString("key"));
        assertEquals("testCountAllDocumentsKey2", allDocuments.get(1).getString("key"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFindAllWithPaginationAndSortingDesc(VertxTestContext testContext) {

    JsonObject draftStatus = new JsonObject().put("status", "draft");
    JsonObject otherStatus = new JsonObject().put("status", "other");
    JsonObject document1 = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2 = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document3 = new JsonObject().put("key", "testCountAllDocumentsKey3").put("value", "testCountAllDocumentsValue3").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document1OtherStatus = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", otherStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2OtherStatus = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", otherStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");

    dbHandler.create(DB_HANDLER_COLLECTION, document1)
      .compose(result1 -> {
        assertNotNull(result1);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2);
      })
      .compose(result2 -> {
        assertNotNull(result2);
        return dbHandler.create(DB_HANDLER_COLLECTION, document3);
      })
      .compose(result3 -> {
        assertNotNull(result3);
        return dbHandler.create(DB_HANDLER_COLLECTION, document1OtherStatus);
      })
      .compose(result4 -> {
        assertNotNull(result4);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2OtherStatus);
      })
      .compose(result5 -> {
        assertNotNull(result5);
        return dbHandler.findAll(DB_HANDLER_COLLECTION, 1, 2, "key", "desc", "draft", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
      })
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(2, allDocuments.size());
        assertEquals("testCountAllDocumentsKey3", allDocuments.get(0).getString("key"));
        assertEquals("testCountAllDocumentsKey2", allDocuments.get(1).getString("key"));
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testDeleteById(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testDeleteByIdKey").put("value", "testDeleteByIdValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        return dbHandler.deleteById(DB_HANDLER_COLLECTION, new JsonObject().put("_id", id));
      })
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals(204, result);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailDeleteByIdThatDoesNotExist(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testDeleteByIdKey").put("value", "testDeleteByIdValue");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        return dbHandler.deleteById(DB_HANDLER_COLLECTION, new JsonObject().put("_id", "66f2b92dd764b50aab4f7360"));
      })
      .onSuccess(result -> {
        assertNotNull(result);
        assertEquals(404, result);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailDeleteAllButNoneExists(VertxTestContext testContext) {

    dbHandler.deleteAll(DB_HANDLER_COLLECTION, new JsonObject().put("value", "sameValue"))
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(404, allDocuments);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCountAllDocuments(VertxTestContext testContext) {

    JsonObject draftStatus = new JsonObject().put("status", "draft");
    JsonObject document1 = new JsonObject().put("key", "testCountAllDocumentsKey1").put("value", "testCountAllDocumentsValue1").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
    JsonObject document2 = new JsonObject().put("key", "testCountAllDocumentsKey2").put("value", "testCountAllDocumentsValue2").put("status", draftStatus).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");

    dbHandler.create(DB_HANDLER_COLLECTION, document1)
      .compose(result1 -> {
        assertNotNull(result1);
        return dbHandler.create(DB_HANDLER_COLLECTION, document2);
      })
      .compose(result2 -> {
        assertNotNull(result2);
        return dbHandler.countAllDocuments(DB_HANDLER_COLLECTION, "draft", "1eadc727-9da3-44e9-80a0-867d291f3bf7");
      })
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(2, allDocuments);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailCountAllDocumentsButNoneExists(VertxTestContext testContext) {

    dbHandler.countAllDocuments(DB_HANDLER_COLLECTION, "draft", "1eadc727-9da3-44e9-80a0-867d291f3bf7")
      .onSuccess(allDocuments -> {
        assertNotNull(allDocuments);
        assertEquals(0, allDocuments);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testCheckDocumentExists(VertxTestContext testContext) {

    JsonObject document = new JsonObject().put("key", "testCheckDocumentExistsKey").put("value", "testCheckDocumentExistsValue").put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7");

    dbHandler.create(DB_HANDLER_COLLECTION, document)
      .compose(id -> {
        assertNotNull(id);
        return dbHandler.checkDocumentExists(new JsonObject().put("_id", id).put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7"), DB_HANDLER_COLLECTION).future();
      })
      .onSuccess(documentExists -> {
        assertNotNull(documentExists);
        assertEquals(true, documentExists);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testFailCheckDocumentExistsButDoesNotExist(VertxTestContext testContext) {

    dbHandler.checkDocumentExists(new JsonObject().put("_id", "66f2b92dd764b50aab4f7360").put("userId", "1eadc727-9da3-44e9-80a0-867d291f3bf7"), DB_HANDLER_COLLECTION).future()
      .onSuccess(documentExists -> {
        assertNotNull(documentExists);
        assertEquals(false, documentExists);
        testContext.completeNow();
      })
      .onFailure(testContext::failNow);
  }


  @Test
  void testUpdateLinkableFieldsSuccess() {
    skipCleanup = true;

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // Arrange
    String collection = "testCollection";
    String objectId = "12345";

    // Act
    Future<Object> result = dbHandler.updateLinkableFields(collection, objectId);


    Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event ->
          event.getLevel() == Level.INFO &&
            event.getFormattedMessage().contains("Linkable fields updated successfully"));
      assertTrue(found);
      assertTrue(result.isComplete());

    });

  }


  @Test
  void testUpdateLinkableFieldsFailure() {
    skipCleanup = true;

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // Arrange
    String collection = "testCollection";
    String objectId = "12345";

    mongoClient = mock(MongoClient.class);
    dbHandler = new DBHandler(mongoClient);

        when(mongoClient.updateCollectionWithOptions(
      anyString(),
      any(JsonObject.class),
      any(JsonObject.class),
      any(UpdateOptions.class),
      any()
    ))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(4); // use correct index
        handler.handle(Future.failedFuture("error")); // simulate failure
        return null;
      });

    // Act
    Future<Object> result = dbHandler.updateLinkableFields(collection, objectId);


    Awaitility.await().atMost(Duration.ofSeconds(4)).untilAsserted(() -> {
      boolean found = listAppender.list.stream()
        .anyMatch(event ->
          event.getLevel() == Level.ERROR &&
            event.getFormattedMessage().contains("Failed"));
      assertTrue(found);
      assertTrue(result.isComplete());

    });

  }



}
