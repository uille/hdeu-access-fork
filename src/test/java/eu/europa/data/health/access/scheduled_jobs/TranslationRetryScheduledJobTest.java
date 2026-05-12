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

import ch.qos.logback.classic.Level;
import eu.europa.data.health.access.TestAppender;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import eu.europa.data.health.access.services.DataRequestApplicationManager;
import eu.europa.data.health.access.services.HubStoreClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationRetryScheduledJobTest {

  private MongoClient mongoClient;
  private HubTranslationClient hubTranslationClient;
  private HubStoreClient hubStoreClient;
  private TranslationRetryScheduledJob translationRetryScheduledJob;
  private TestAppender testAppender;

  @BeforeEach
  void setUp() {

    testAppender = new TestAppender();
    Logger log = LoggerFactory.getLogger(TranslationRetryScheduledJob.class);
    testAppender.start();
    ((ch.qos.logback.classic.Logger) log).addAppender(testAppender);

    mongoClient = mock(MongoClient.class);
    hubTranslationClient = mock(HubTranslationClient.class);
    hubStoreClient = mock(HubStoreClient.class);
    DataAccessApplicationManager dataAccessApplicationManager = mock(
      DataAccessApplicationManager.class);
    DataRequestApplicationManager dataRequestApplicationManager = mock(
      DataRequestApplicationManager.class);
    translationRetryScheduledJob = new TranslationRetryScheduledJob(mongoClient,
      hubTranslationClient, hubStoreClient, dataAccessApplicationManager,
      dataRequestApplicationManager);
  }

  @Test
  void testRetryPendingTranslations_formRetrySuccess() {
    String appId = "app-123";
    String appType = "dataRequest";

    JsonObject form = new JsonObject().put("status", "PENDING")
      .put("payload", new JsonObject().put("field", "value")).put("attempts", 0);

    JsonObject translation = new JsonObject().put("form", form);
    JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

    // Mock Mongo find to return one application
    when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(List.of(app)));

    // Mock hub translation request
    when(hubTranslationClient.requestTranslation(any(JsonObject.class))).thenReturn(
      Future.succeededFuture());

    // Mock update calls
    when(mongoClient.updateCollection(eq(appType), any(JsonObject.class),
      any(JsonObject.class))).thenReturn(Future.succeededFuture());

    // Act
    translationRetryScheduledJob.retryPendingTranslations(appType, 3, 1000);

    // Assert: verify that hubTranslationClient was called with the payload
    verify(hubTranslationClient).requestTranslation(eq(form.getJsonObject("payload")));

    // Verify mongo update for attempts increment
    verify(mongoClient, atLeastOnce()).updateCollection(eq(appType), any(JsonObject.class),
      any(JsonObject.class));
  }

  @Test
  void testRetryPendingTranslations_retryPendingTranslations_fail() {
    String appId = "app-456";
    String appType = DATA_REQUEST_APPLICATION_COLLECTION;

    JsonObject form = new JsonObject().put("status", "PENDING")
      .put("payload", new JsonObject().put("field", "value")).put("attempts", 5);

    JsonObject translation = new JsonObject().put("form", form);
    JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

    when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(List.of(app)));

    // Act
    translationRetryScheduledJob.retryPendingTranslations(appType, 3, 1000);

    // Assert: requestTranslation should never be called
    verify(hubTranslationClient, never()).requestTranslation(any());

    boolean found = testAppender.getLogs().stream()
      .anyMatch(log -> log.getLevel() == Level.WARN &&
        log.getFormattedMessage().contains("exceeded max attempts"));

    assertTrue(found, "Expected WARN log with 'exceeded max attempts'");
  }


  @Test
  void testRetryPendingTranslations_formRetryExceeded() {
    String appId = "app-456";
    String appType = "dataRequest";
    String fileId = "file123";

    JsonObject form = new JsonObject().put("status", "PENDING")
      .put("payload", new JsonObject().put("field", "value"))
      .put("attempts", 5); // exceed maxAttempts

    JsonObject translation = new JsonObject().put("files", new JsonObject().put("form", form));
    JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

    when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(List.of(app)));

    when(mongoClient.updateCollection(eq(appType), any(JsonObject.class),
      any(JsonObject.class))).thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    JsonObject fileObj = new JsonObject().put("id", fileId).put("base64", "abc123");

    when(hubStoreClient.getFilesAsBase64(any(), any())).thenReturn(
      Future.succeededFuture(List.of(fileObj)));

    when(hubTranslationClient.requestFileTranslation(any())).thenReturn(Future.succeededFuture());

    // Act
    translationRetryScheduledJob.retryPendingTranslations(appType, 6, 1000);

    // Assert: requestTranslation should never be called
    verify(hubTranslationClient, never()).requestTranslation(any());

    // Assert: mongo update should mark FAILED
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).updateCollection(eq(appType), any(JsonObject.class),
      updateCaptor.capture());

    JsonObject update = updateCaptor.getValue();
    assertNotNull(update.getJsonObject("$set").getString("translation.files.form.lastRetryAt"));
  }


  @Test
  void testRetryPendingTranslations_maxAttemps() {
    String appId = "app-456";
    String appType = DATA_REQUEST_APPLICATION_COLLECTION;

    JsonObject form = new JsonObject().put("status", "PENDING")
      .put("payload", new JsonObject().put("field", "value"))
      .put("attempts", 6); // exceed maxAttempts

    JsonObject translation = new JsonObject().put("files", new JsonObject().put("form", form));
    JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

    when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any())).thenReturn(
      Future.succeededFuture(List.of(app)));

    // Act
    translationRetryScheduledJob.retryPendingTranslations(appType, 4, 1000);

    // Assert: requestTranslation should never be called
    verify(hubTranslationClient, never()).requestTranslation(any());

    boolean found = testAppender.getLogs().stream()
      .anyMatch(log -> log.getLevel() == Level.WARN &&
        log.getFormattedMessage().contains("exceeded max attempts"));

    assertTrue(found, "Expected WARN log with 'exceeded max attempts'");

  }


    @Test
    void testRetryPendingTranslations_fetchError() {
        String appType = "dataRequest";
        // Mock Mongo failure
        when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any()))
                .thenReturn(Future.failedFuture(new RuntimeException("DB error")));
        // Act
        translationRetryScheduledJob.retryPendingTranslations(appType, 3, 1000);

        // Assert: Ensure error is logged
        boolean found = testAppender.getLogs().stream()
                .anyMatch(log -> log.getLevel() == Level.ERROR &&
                        log.getFormattedMessage().contains("Failed to fetch applications for retry"));

        assertTrue(found, "Expected ERROR log with 'Failed to fetch applications for retry'");
    }

    @Test
    void testRetryPendingTranslations_fileTranslationMaxAttempts() {
        String appId = "app-789";
        String appType = DATA_REQUEST_APPLICATION_COLLECTION;
        String fileId = "file123";

        JsonObject file = new JsonObject().put("status", "PENDING")
                .put("attempts", 5); // Exceed maxAttempts

        JsonObject translation = new JsonObject().put("files", new JsonObject().put(fileId, file));
        JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

        when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any()))
                .thenReturn(Future.succeededFuture(List.of(app)));

        // Act
        translationRetryScheduledJob.retryPendingTranslations(appType, 4, 1000);

        // Assert: Ensure translation retry was skipped and warn logged
        verify(hubTranslationClient, never()).requestFileTranslation(any());

        boolean found = testAppender.getLogs().stream()
                .anyMatch(log -> log.getLevel() == Level.WARN &&
                        log.getFormattedMessage()
                                .contains("Skipping file translation retry for fileId=" + fileId + ", appId=" + appId));

        assertTrue(found, "Expected WARN log with 'Skipping file translation retry'");
    }

    @Test
    void testRetryPendingTranslations_formTranslationRetry() {
        String appId = "app-123";
        String appType = "dataRequest";

        JsonObject form = new JsonObject().put("status", "PENDING")
                .put("payload", new JsonObject().put("key", "value"))
                .put("attempts", 0);

        JsonObject translation = new JsonObject().put("form", form);
        JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

        when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any()))
                .thenReturn(Future.succeededFuture(List.of(app)));

        when(hubTranslationClient.requestTranslation(eq(form.getJsonObject("payload"))))
                .thenReturn(Future.succeededFuture());

        when(mongoClient.updateCollection(eq(appType), any(JsonObject.class), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        // Act
        translationRetryScheduledJob.retryPendingTranslations(appType, 3, 1000);

        // Assert: requestTranslation is called for form payload
        verify(hubTranslationClient).requestTranslation(eq(form.getJsonObject("payload")));

        // Assert: Update attempts and lastRetryAt is called
        verify(mongoClient, atLeastOnce()).updateCollection(eq(appType), any(JsonObject.class), any(JsonObject.class));
    }

    @Test
    void testRetryPendingTranslations_fileTranslationRetry() {
        String appId = "app-123";
        String appType = "dataRequest";
        String fileId = "file456";

        JsonObject file = new JsonObject().put("status", "PENDING")
                .put("attempts", 0);

        JsonObject translation = new JsonObject().put("files", new JsonObject().put(fileId, file));
        JsonObject app = new JsonObject().put("_id", appId).put("translation", translation);

        JsonObject fileObj = new JsonObject().put("id", fileId).put("base64", "abc123");
        JsonObject payload = new JsonObject().put("key", "value");

        when(mongoClient.findWithOptions(eq(appType), any(JsonObject.class), any()))
                .thenReturn(Future.succeededFuture(List.of(app)));

        when(hubStoreClient.getFilesAsBase64(any(), eq(appId)))
                .thenReturn(Future.succeededFuture(List.of(fileObj)));

        when(hubTranslationClient.buildFileTranslationPayload(eq(appId), eq(appType), eq(fileObj), any()))
                .thenReturn(payload);

        when(hubTranslationClient.requestFileTranslation(eq(payload)))
                .thenReturn(Future.succeededFuture());

        when(mongoClient.updateCollection(eq(appType), any(JsonObject.class), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

        // Act
        translationRetryScheduledJob.retryPendingTranslations(appType, 3, 1000);

        // Assert: Ensure file translation is attempted
        verify(hubStoreClient).getFilesAsBase64(any(), eq(appId));
        verify(hubTranslationClient).requestFileTranslation(eq(payload));

        // Assert: Update attempts and lastRetryAt is called
        verify(mongoClient, atLeastOnce()).updateCollection(eq(appType), any(JsonObject.class), any(JsonObject.class));
    }

}
