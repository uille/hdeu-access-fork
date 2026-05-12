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
package eu.europa.data.health.access.scheduled_jobs;

import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.NotificationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

class DispatcherScheduledJobTest {

  @Mock
  DBHandler dbHandler;

  @Mock
  MongoClient mongoClient;

  @Mock
  DispatcherManager dispatcherManager;

  @Mock
  DispatcherClient dispatcherClient;

  @Mock
  AuditLogManager auditLogManager;

  @Mock
  NotificationManager notificationManager;

  @InjectMocks
  DispatcherScheduledJob job;
  private final Logger logger = (Logger) LoggerFactory.getLogger(DispatcherScheduledJob.class);

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    job = new DispatcherScheduledJob(dbHandler, mongoClient, dispatcherManager, auditLogManager,
      notificationManager, new JsonObject());
  }

  @Test
  void testSendApplicationsToDispatcher_withResults() {
    String applicationType = "testType";
    List<JsonObject> records = List.of(new JsonObject().put("_id", "123"));

    when(mongoClient.find(anyString(), any(JsonObject.class), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture(records));
      return null;
    });

    job.sendApplicationsToDispatcher(3, dispatcherClient, applicationType)
      .onComplete(ar -> assertTrue(ar.succeeded()));

    verify(mongoClient, times(1)).find(anyString(), any(JsonObject.class), any());

  }

  @Test
  void testSendApplicationsToDispatcher_withEmptyResults() {
    String applicationType = "testType";

    when(mongoClient.find(anyString(), any(JsonObject.class), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture(Collections.emptyList()));
      return null;
    });

    job.sendApplicationsToDispatcher(3, dispatcherClient, applicationType)
      .onComplete(ar -> assertTrue(ar.succeeded()));
    verify(mongoClient, times(1)).find(anyString(), any(JsonObject.class), any());
  }

  @Test
  void testSendApplicationsToDispatcher_withFailResults() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    String applicationType = "testType";

    when(mongoClient.find(anyString(), any(JsonObject.class), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture("error"));
      return null;
    });

    job.sendApplicationsToDispatcher(3, dispatcherClient, applicationType)
      .onComplete(ar -> assertTrue(ar.failed()));
    verify(mongoClient, times(1)).find(anyString(), any(JsonObject.class), any());

    List<ILoggingEvent> logsList = listAppender.list;
    assertFalse(logsList.isEmpty(), "No logs captured");

    boolean foundError1 = false;
    boolean foundError2 = false;

    for (ILoggingEvent event : logsList) {
      if (event.getFormattedMessage().contains("Failed to find pending DISPATCHER dispatcher records")) {
        foundError1 = true;
      }
      if (event.getFormattedMessage().contains("Find pending dispatcher records was not successful")) {
        foundError2 = true;
      }
    }
    assertTrue(foundError1, "Error log for 'Failed to find pending DISPATCHER dispatcher records' not found");
    assertTrue(foundError2, "Error log for 'Find pending dispatcher records was not successful' not found");
  }

  @Test
  void testSendRecordToDispatcher_success() {
    JsonObject dispatcherRecord = new JsonObject().put("_id", "app1");
    when(dispatcherClient.sendApplication(any())).thenReturn(Future.succeededFuture("response-id"));

    Future<String> future = job.sendRecordToDispatcher(dispatcherRecord, dispatcherClient);
    assertTrue(future.succeeded());
    assertEquals("response-id", future.result());
  }

  @Test
  void testSendRecordToDispatcher_failure() {
    JsonObject dispatcherRecord = new JsonObject().put("_id", "app1");
    when(dispatcherClient.sendApplication(any())).thenReturn(Future.failedFuture("error"));

    Future<String> future = job.sendRecordToDispatcher(dispatcherRecord, dispatcherClient);
    assertTrue(future.failed());
    assertEquals("error", future.cause().getMessage());
  }

  @Test
  void testMarkDispatcherRecordAsCompleted_triggersUpdate() {
    JsonObject updatedResult = new JsonObject().put("_id", "app1")
      .put("operation", "CREATE")
      .put("configuration", new JsonObject().put("applicationType", "collectionName"));

    when(mongoClient.updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class),
      any(Handler.class)
    )).thenAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
      when(updateResult.getDocMatched()).thenReturn(1L);
      handler.handle(Future.succeededFuture(updateResult));
      return null;
    });

    when(mongoClient.findOne(anyString(), any(), isNull(), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(updatedResult));
      return null;
    });

    job.markDispatcherRecordAsCompleted(updatedResult, "[\"message-id\"]")
      .onComplete(ar -> assertTrue(ar.succeeded()));
    verify(mongoClient).updateCollection(eq("DISPATCHER"), any(JsonObject.class),
      any(JsonObject.class), any(Handler.class)
    );
  }

  @Test
  void testRetrySendApplicationToDispatcher_shouldFail() {
    JsonObject dispatcherRecord = new JsonObject()
      .put("_id", "app1")
      .put("configuration", new JsonObject().put("retries", 3));

    when(mongoClient.updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class),
      any(Handler.class)
    )).thenAnswer(invocation -> {
      Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
      MongoClientUpdateResult updateResult = mock(MongoClientUpdateResult.class);
      when(updateResult.getDocMatched()).thenReturn(1L);
      handler.handle(Future.succeededFuture(updateResult));
      return null;
    });

    job.retrySendApplicationToDispatcher("app1", dispatcherRecord, 2, "some error",
      DISPATCHER_COLLECTION)
      .onComplete(ar -> assertTrue(ar.succeeded()));
  }

  @Test
  void testSendApplicationsToDispatcher_acquiresLockAndCompletes() {
    String applicationType = "testType";
    List<JsonObject> records = List.of(new JsonObject().put("_id", "123"));

    JsonObject dispatcherRecord = new JsonObject()
      .put("_id", "123")
      .put("configuration", new JsonObject().put("applicationType", applicationType));

    DispatcherScheduledJob spyJob = spy(job);

    when(mongoClient.find(anyString(), any(JsonObject.class), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
        handler.handle(Future.succeededFuture(records));
        return null;
      });

    when(mongoClient.findOneAndUpdate(anyString(), any(), any(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(dispatcherRecord));
        return null;
      });

    doReturn(Future.succeededFuture("[\"mocked-msg-id\"]")).when(spyJob)
      .sendRecordToDispatcher(any(JsonObject.class), any(DispatcherClient.class));

    spyJob.sendApplicationsToDispatcher(3, dispatcherClient, applicationType)
      .onComplete(ar -> assertTrue(ar.succeeded()));

    verify(mongoClient, times(1)).find(anyString(), any(JsonObject.class), any());
    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mongoClient).findOneAndUpdate(eq("DISPATCHER"), any(), any(), any());
    });
  }

}
