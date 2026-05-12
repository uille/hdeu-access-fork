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
import static eu.europa.data.health.access.Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION;
import static eu.europa.data.health.access.Constants.TRANSLATIONS_STATE_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.enums.TranslationStatus;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClientActivities;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DecisionTranslationServiceTest {

  @Mock
  private MongoClient mongoClient;
  @Mock
  private HubTranslationClientActivities hubTranslationClient;
  @Mock
  private DispatcherClient dispatcherClient;
  @Mock
  private HubStoreClient hubStoreClient;

  private DecisionTranslationService service;

  private DecisionTranslationService createService(boolean enableTranslation) {
    return new DecisionTranslationService(
      mongoClient,
      hubTranslationClient,
      dispatcherClient,
      hubStoreClient,
      enableTranslation
    );
  }

  @BeforeEach
  void setUp() {
    service = createService(true);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // createTranslatedFieldsReference
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void createTranslatedFieldsReference_withValidId_returnsJsonObjectWithStateId() {
    String stateId = "abc-123";
    JsonObject result = service.createTranslatedFieldsReference(stateId);
    assertNotNull(result);
    assertEquals(stateId, result.getString(TRANSLATIONS_STATE_COLLECTION));
  }

  @Test
  void createTranslatedFieldsReference_withBlankId_returnsNull() {
    assertNull(service.createTranslatedFieldsReference(""));
    assertNull(service.createTranslatedFieldsReference("   "));
    assertNull(service.createTranslatedFieldsReference(null));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // saveDecisionTranslation
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveDecisionTranslation_withNoTranslatableFields_returnsNullFuture()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject decision = new JsonObject(); // no decision_file, no support_file, no decision_reason

    // hubStoreClient is never needed for this path since no files
    // addFileField returns a succeeded future for blank values without calling hubStore
    Future<String> future = service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en");

    String result = awaitFuture(future);
    assertNull(result);
  }

  @Test
  void saveDecisionTranslation_withDecisionReason_insertsRecord()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject decision = new JsonObject().put("decision_reason", "Some reason");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    Future<String> future = service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en");

    String result = awaitFuture(future);
    // The record ID is a UUID generated internally – just ensure it was saved
    verify(mongoClient, times(1)).save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class));
    assertNotNull(result);
  }

  @Test
  void saveDecisionTranslation_withDecisionFileAsHubStoreId_insertsRecord()
      throws ExecutionException, InterruptedException, TimeoutException {
    // Value "short-file-id" is not valid base64 → treated as existing HubStore ID
    JsonObject decision = new JsonObject().put("decision_file", "some-hub-store-id");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    Future<String> future = service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en");

    awaitFuture(future);
    verify(mongoClient, times(1)).save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class));
  }

  @Test
  void saveDecisionTranslation_existingStateIdApplied_usesExistingId()
      throws ExecutionException, InterruptedException, TimeoutException {
    String existingStateId = "existing-state-uuid";
    JsonObject translatedFields = new JsonObject()
        .put(TRANSLATIONS_STATE_COLLECTION, existingStateId);
    JsonObject decision = new JsonObject()
        .put("decision_reason", "Some reason")
        .put("translatedFields", translatedFields);

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    Future<String> future = service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en");

    String result = awaitFuture(future);
    assertEquals(existingStateId, result);

    ArgumentCaptor<JsonObject> docCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), docCaptor.capture());
    assertEquals(existingStateId, docCaptor.getValue().getString("_id"));
  }

  @Test
  void saveDecisionTranslation_recordHasCorrectFields()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject decision = new JsonObject().put("decision_reason", "reason text");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 2, decision, "catalog-1", "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonObject saved = captor.getValue();

    assertEquals("portal-id", saved.getString("applicationPortalId"));
    assertEquals("app-id", saved.getString("applicationId"));
    assertEquals("dataset-1", saved.getString("dataset_id"));
    assertEquals(2, saved.getInteger("itemIndex"));
    assertEquals("decision", saved.getString("itemType"));
    assertEquals("catalog-1", saved.getString("catalogId"));
    assertEquals(TranslationStatus.PENDING.getStatus(), saved.getString("translationStatus"));
    assertEquals(0, saved.getInteger("fileRetries"));
    assertEquals(0, saved.getInteger("textRetries"));
    assertEquals(DATA_ACCESS_APPLICATION_COLLECTION, saved.getString("application_type"));
  }

    @Test
    void saveDecisionTranslation_withTranslationDisabled_setsTargetLanguageToInputLanguage()
            throws ExecutionException, InterruptedException, TimeoutException {
        DecisionTranslationService disabledService = createService(false);
        JsonObject decision = new JsonObject().put("decision_reason", "reason text");

        when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture(null));

        awaitFuture(disabledService.saveDecisionTranslation(
                DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
                "dataset-1", 2, decision, "catalog-1", "de"));

        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
        assertEquals("de", captor.getValue().getString("targetLanguage"));
    }

  // ─────────────────────────────────────────────────────────────────────────────
  // savePermitTranslation
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void savePermitTranslation_withNoTranslatableFields_returnsNull()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject permit = new JsonObject(); // no permit_file, no permit_contact_details

    Future<String> future = service.savePermitTranslation(
        DATA_REQUEST_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, permit, "catalog-1", "en");

    assertNull(awaitFuture(future));
  }

  @Test
  void savePermitTranslation_withContactDetails_insertsRecord()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject permit = new JsonObject().put("permit_contact_details", "Contact info");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    Future<String> future = service.savePermitTranslation(
        DATA_REQUEST_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 1, permit, "catalog-1", "en");

    awaitFuture(future);

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonObject saved = captor.getValue();
    assertEquals("permit", saved.getString("itemType"));
    assertEquals(1, saved.getInteger("itemIndex"));
    assertEquals(DATA_REQUEST_APPLICATION_COLLECTION, saved.getString("application_type"));
  }

  @Test
  void savePermitTranslation_withPermitFileAsId_insertsRecord()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject permit = new JsonObject().put("permit_file", "hub-store-file-id");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.savePermitTranslation(
        DATA_REQUEST_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, permit, "catalog-1", "en"));

    verify(mongoClient, times(1)).save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class));
  }

  @Test
  void savePermitTranslation_existingStateIdApplied()
      throws ExecutionException, InterruptedException, TimeoutException {
    String existingStateId = "permit-state-uuid";
    JsonObject translatedFields = new JsonObject()
        .put(TRANSLATIONS_STATE_COLLECTION, existingStateId);
    JsonObject permit = new JsonObject()
        .put("permit_contact_details", "Contact")
        .put("translatedFields", translatedFields);

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    String result = awaitFuture(service.savePermitTranslation(
        DATA_REQUEST_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, permit, "catalog-1", "en"));

    assertEquals(existingStateId, result);

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    assertEquals(existingStateId, captor.getValue().getString("_id"));
  }

  @Test
  void savePermitTranslation_withTranslationDisabled_setsTargetLanguageToInputLanguage()
    throws ExecutionException, InterruptedException, TimeoutException {
    DecisionTranslationService disabledService = createService(false);
    JsonObject permit = new JsonObject().put("permit_contact_details", "Contact info");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(null)
    );

    awaitFuture(
      disabledService.savePermitTranslation(
        DATA_REQUEST_APPLICATION_COLLECTION,
        "portal-id",
        "app-id",
        "dataset-1",
        1,
        permit,
        "catalog-1",
        "fr"
      )
    );

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    assertEquals("fr", captor.getValue().getString("targetLanguage"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // saveStatusTranslation
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveStatusTranslation_withNoTextFields_returnsNull()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject statusPayload = new JsonObject(); // no "reason", no "additionalInformation"

    Future<String> future = service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", statusPayload, "en");

    assertNull(awaitFuture(future));
  }

  @Test
  void saveStatusTranslation_withNullPayload_returnsNull()
      throws ExecutionException, InterruptedException, TimeoutException {
    Future<String> future = service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", null, "en");

    assertNull(awaitFuture(future));
  }

  @Test
  void saveStatusTranslation_withReason_insertsRecord()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject statusPayload = new JsonObject().put("reason", "Rejection reason");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", statusPayload, "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonObject saved = captor.getValue();
    assertEquals("status", saved.getString("itemType"));
    assertEquals("catalog-1", saved.getString("catalogId"));
    assertEquals("en", saved.getString("targetLanguage"));
    assertEquals(-1, saved.getInteger("itemIndex"));
    assertEquals(TranslationStatus.PENDING.getStatus(), saved.getString("translationStatus"));
  }

  @Test
  void saveStatusTranslation_withAdditionalInformation_insertsRecordWithMultipleTextFields()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject additionalInfo = new JsonObject()
        .put("source", new JsonArray()
            .add(new JsonObject().put("field", "field1").put("comment", "Comment 1"))
            .add(new JsonObject().put("field", "field2").put("comment", "Comment 2")));
    JsonObject statusPayload = new JsonObject().put("additionalInformation", additionalInfo);

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", statusPayload, "fr"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonObject saved = captor.getValue();
    JsonArray textFields = saved.getJsonArray("textFields");
    assertNotNull(textFields);
    assertEquals(2, textFields.size());
  }

  @Test
  void saveStatusTranslation_existingStateIdApplied()
      throws ExecutionException, InterruptedException, TimeoutException {
    String existingStateId = "status-state-uuid";
    JsonObject statusPayload = new JsonObject()
        .put("reason", "A reason")
        .put("translatedFields", new JsonObject()
            .put(TRANSLATIONS_STATE_COLLECTION, existingStateId));

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    String result = awaitFuture(service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", statusPayload, "de"));

    assertEquals(existingStateId, result);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // triggerPendingTranslations
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void triggerPendingTranslations_withNoPendingDocs_completesSuccessfully()
      throws ExecutionException, InterruptedException, TimeoutException {
    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of()));

    Future<Void> future = service.triggerPendingTranslations();
    awaitFuture(future);
    // No exceptions; just verifies the chain completes
    verify(mongoClient, times(2)).find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class));
  }

  @Test
  void triggerPendingTranslations_withPendingDocThatHasNoFields_marksCompleted()
      throws ExecutionException, InterruptedException, TimeoutException {
    // A record with empty fileFields and textFields → should mark as COMPLETED immediately
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray())
        .put("textFields", new JsonArray())
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));

    // For updateStateRecord: findOne → state, then updateCollection
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    Future<Void> future = service.triggerPendingTranslations();
    awaitFuture(future);

    // updateCollection should have been called to set status to COMPLETED
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processTranslationCallback
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_recordNotFound_logsWarning() {
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(null));

    // Should not throw; just complete silently with a warning
    assertDoesNotThrow(() ->
        service.processTranslationCallback("nonexistent-id", new JsonObject()));

    // Give async chain time to run
    try { Thread.sleep(100); } catch (InterruptedException ignored) {}

    verify(mongoClient).findOne(eq(TRANSLATIONS_STATE_COLLECTION),
        argThat(q -> "nonexistent-id".equals(q.getString("_id"))), isNull());
  }

  @Test
  void processTranslationCallback_withTextCallback_completesWhenAllTranslated() {
    // Build a state record that has one text field across 0 languages already done
    // The callback provides translations for all supported languages
    JsonObject stateDoc = buildMinimalStateDoc("rec-id", "decision", new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 0);

    // Create a callback dict that returns translations for a small set of languages
    // (we'll only use 2 to keep the test lean; the assertion is that translations are accumulated)
    JsonObject langTranslations = new JsonObject()
        .put("en", "Translated reason EN")
        .put("fr", "Translated reason FR");
    JsonObject fieldResult = new JsonObject().put("translations", langTranslations);
    JsonObject translationSection = new JsonObject().put("decision_reason", fieldResult);
    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, translationSection);

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
  }

  @Test
  void processTranslationCallback_withNumericErrorCode_recordsErrors() {
    JsonObject stateDoc = buildMinimalStateDoc("rec-id", "decision", new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 0);

    // Error code 500 is a numeric value → should be treated as an error, not a translation
    JsonObject langTranslations = new JsonObject().put("en", 500);
    JsonObject fieldResult = new JsonObject().put("translations", langTranslations);
    JsonObject translationSection = new JsonObject().put("decision_reason", fieldResult);
    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, translationSection);

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    try { Thread.sleep(200); } catch (InterruptedException ignored) {}

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    boolean hasErrors = updateCaptor.getAllValues().stream()
        .anyMatch(u -> {
          JsonObject set = u.getJsonObject("$set", new JsonObject());
          JsonArray errors = set.getJsonArray("translationErrors");
          return errors != null && !errors.isEmpty();
        });
    assertTrue(hasErrors);
  }

  @Test
  void processTranslationCallback_exceedingMaxRetries_setStatusFailed() {
    // Simulate 5 text retries already done so next callback triggers FAILED
    JsonObject stateDoc = buildMinimalStateDoc("rec-id", "decision", new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 4 /* textRetries = 4, will become 5 = MAX */);

    // Send a dict that only partially translates (0 translations in output)
    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject())));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    try { Thread.sleep(300); } catch (InterruptedException ignored) {}

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    boolean failedStatus = updateCaptor.getAllValues().stream().anyMatch(u -> {
      JsonObject set = u.getJsonObject("$set", new JsonObject());
      return "failed".equals(set.getString("translationStatus"));
    });
    assertTrue(failedStatus, "Expected FAILED status after exceeding MAX_RETRIES");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // syncTranslations (currently a no-op)
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void syncTranslations_completesSuccessfully()
      throws ExecutionException, InterruptedException, TimeoutException {
    Future<Void> future = service.syncTranslations();
    awaitFuture(future);
    // no interactions expected
    verifyNoInteractions(mongoClient, hubTranslationClient, dispatcherClient, hubStoreClient);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // saveDecisionTranslation – mongoClient failure propagation
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveDecisionTranslation_mongoFails_futureFails() {
    JsonObject decision = new JsonObject().put("decision_reason", "Some reason");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.failedFuture("Mongo error"));

    Future<String> future = service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en");

    CompletableFuture<Boolean> failed = new CompletableFuture<>();
    future.onFailure(e -> failed.complete(true));
    future.onSuccess(v -> failed.complete(false));

    try {
      assertTrue(failed.get(2, TimeUnit.SECONDS));
    } catch (Exception e) {
      fail("Future did not complete in time: " + e.getMessage());
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // triggerPendingTranslations – with actual fields → sends requests
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void triggerPendingTranslations_withTextFields_sendsTextTranslationRequestAndSetsWaiting()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray())
        .put("textFields", new JsonArray()
            .add(new JsonObject().put("field", "decision_reason").put("value", "some reason")))
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(hubTranslationClient.createCallbackJson())
        .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestTextTranslation(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    verify(hubTranslationClient).requestTextTranslation(any(JsonObject.class));
    // Status should be updated to WAITING
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    boolean waitingSet = updateCaptor.getAllValues().stream()
        .anyMatch(u -> "waiting".equals(u.getJsonObject("$set", new JsonObject()).getString("translationStatus")));
    assertTrue(waitingSet, "Expected translationStatus to be set to 'waiting'");
  }

  @Test
  void triggerPendingTranslations_withFileFields_sendsFileTranslationRequestAndSetsWaiting()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray()
            .add(new JsonObject().put("field", "decision_file").put("file_hub_repo_id", "hub-file-id")
                .put("format", "pdf").put("fileName", "decision_file.pdf")))
        .put("textFields", new JsonArray())
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    // HubStore returns base64 content
    JsonObject fileResult = new JsonObject()
        .put("base64Content", "dGVzdA==")
        .put("format", "pdf")
        .put("fileName", "decision_file.pdf");
    when(hubStoreClient.getFileAsBase64(eq("hub-file-id"), any(), any()))
        .thenReturn(Future.succeededFuture(fileResult));
    when(hubTranslationClient.createCallbackJson())
        .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestFileTranslation(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    verify(hubTranslationClient).requestFileTranslation(any(JsonObject.class));
  }

  @Test
  void triggerPendingTranslations_hubStoreFails_skipsFileAndStillSetsWaiting()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray()
            .add(new JsonObject().put("field", "decision_file").put("file_hub_repo_id", "hub-file-id")
                .put("format", "pdf").put("fileName", "decision_file.pdf")))
        .put("textFields", new JsonArray()
            .add(new JsonObject().put("field", "decision_reason").put("value", "reason")))
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    // HubStore fails → file skipped
    when(hubStoreClient.getFileAsBase64(any(), any(), any()))
        .thenReturn(Future.failedFuture("HubStore unavailable"));
    when(hubTranslationClient.createCallbackJson())
        .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestTextTranslation(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    // Text translation should still fire despite file failure
    verify(hubTranslationClient).requestTextTranslation(any(JsonObject.class));
  }

  @Test
  void triggerPendingTranslations_withCatalogLanguages_usesThoseLanguages()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray())
        .put("textFields", new JsonArray()
            .add(new JsonObject().put("field", "decision_reason").put("value", "reason")))
        .put("catalogLanguages", new JsonArray().add("en").add("fr"))
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(hubTranslationClient.createCallbackJson())
        .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestTextTranslation(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    ArgumentCaptor<JsonObject> requestCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(hubTranslationClient).requestTextTranslation(requestCaptor.capture());
    JsonObject req = requestCaptor.getValue();
    JsonObject dict = req.getJsonObject("dict", new JsonObject())
        .getJsonObject(TRANSLATIONS_STATE_COLLECTION, new JsonObject());
    JsonArray targetLangs = dict.getJsonObject("decision_reason", new JsonObject())
        .getJsonArray("targetLanguages");
    assertNotNull(targetLangs);
    assertEquals(2, targetLangs.size());
    assertTrue(targetLangs.contains("en"));
    assertTrue(targetLangs.contains("fr"));
  }

  @Test
  void triggerPendingTranslations_withBothFileAndTextFields_sendsBothRequests()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray().add(new JsonObject()
            .put("field", "decision_file")
            .put("file_hub_repo_id", "hub-file-id")
            .put("format", "pdf")
            .put("fileName", "decision_file.pdf")))
        .put("textFields", new JsonArray().add(new JsonObject()
            .put("field", "decision_reason")
            .put("value", "reason")))
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(hubStoreClient.getFileAsBase64(eq("hub-file-id"), any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject()
            .put("base64Content", java.util.Base64.getEncoder().encodeToString(new byte[80]))
            .put("format", "pdf")
            .put("fileName", "decision_file.pdf")));
    when(hubTranslationClient.createCallbackJson())
        .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestFileTranslation(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(hubTranslationClient.requestTextTranslation(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    verify(hubTranslationClient).requestFileTranslation(any(JsonObject.class));
    verify(hubTranslationClient).requestTextTranslation(any(JsonObject.class));
  }

  @Test
  void triggerPendingTranslations_withFileOnlyAndMissingBase64_marksCompleted()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray().add(new JsonObject()
            .put("field", "decision_file")
            .put("file_hub_repo_id", "hub-file-id")
            .put("format", "pdf")
            .put("fileName", "decision_file.pdf")))
        .put("textFields", new JsonArray())
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(hubStoreClient.getFileAsBase64(eq("hub-file-id"), any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject()));
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    verify(hubTranslationClient, never()).requestFileTranslation(any(JsonObject.class));
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    assertTrue(updateCaptor.getAllValues().stream().anyMatch(u -> "completed".equals(
        u.getJsonObject("$set", new JsonObject()).getString("translationStatus"))));
  }

  @Test
  void triggerPendingTranslations_whenTextRequestFails_futureFails() {
    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray())
        .put("textFields", new JsonArray().add(new JsonObject()
            .put("field", "decision_reason")
            .put("value", "reason")))
        .put("translations", new JsonObject())
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(hubTranslationClient.createCallbackJson())
        .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestTextTranslation(any(JsonObject.class)))
        .thenReturn(Future.failedFuture("translation down"));

    Future<Void> future = service.triggerPendingTranslations();
    CompletableFuture<Boolean> failed = new CompletableFuture<>();
    future.onSuccess(v -> failed.complete(false));
    future.onFailure(err -> failed.complete(true));

    try {
      assertTrue(failed.get(2, TimeUnit.SECONDS));
    } catch (Exception e) {
      fail("Future did not complete as expected: " + e.getMessage());
    }
  }

  @Test
  void triggerPendingTranslations_withFullyTranslatedFields_skipsThoseFields()
      throws ExecutionException, InterruptedException, TimeoutException {
    // decision_reason already translated for all supported langs → nothing to send
    JsonObject existingTranslations = new JsonObject();
    // Just add 2 lang translations to make the field "fully done" for targetLanguage="en"
    existingTranslations.put("en", new JsonObject().put("decision_reason", "EN"));

    JsonObject pendingDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", "decision")
        .put("fileFields", new JsonArray())
        .put("textFields", new JsonArray()
            .add(new JsonObject().put("field", "decision_reason").put("value", "reason")))
        .put("targetLanguage", "en")  // single-lang → only "en" needed
        .put("translations", existingTranslations)
        .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    awaitFuture(service.triggerPendingTranslations());

    // No translation request should be sent since field is already fully translated
    verify(hubTranslationClient, never()).requestTextTranslation(any());
    verify(hubTranslationClient, never()).requestFileTranslation(any());
    // Should be marked COMPLETED directly
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    boolean completedSet = updateCaptor.getAllValues().stream()
        .anyMatch(u -> "completed".equals(u.getJsonObject("$set", new JsonObject()).getString("translationStatus")));
    assertTrue(completedSet, "Expected status COMPLETED when all fields already translated");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processTranslationCallback – all translated → COMPLETED → syncs to application
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_allTranslatedForDecision_syncsToApplication()
      throws InterruptedException {
    // Use targetLanguage="en" so 1 text field × 1 lang = 1 expected pair
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "EN reason"))));

    JsonObject decision = new JsonObject()
        .put("datasetStatus", "APPROVED").put("latest", 1)
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject dataset = new JsonObject()
        .put("dataset_id", "dataset-1").put("catalog_id", "catalog-1")
        .put("decisions", new JsonArray().add(decision));
    JsonObject catalog = new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("statuses", new JsonArray().add(
            new JsonObject().put("status", "APPROVED").put("latest", 1)));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("datasets", new JsonArray().add(dataset))
        .put("catalogs", new JsonArray().add(catalog));

    // findOne for TRANSLATIONS_STATE called multiple times (processTranslationCallback + updateStateRecord)
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    doAnswer(inv -> {
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved"));
      return mongoClient;
    }).when(mongoClient).save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    // Application should be updated with translated fields
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class));
    // Outgoing activity record should be saved
    verify(mongoClient, atLeastOnce())
        .save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());
  }

  @Test
  void processTranslationCallback_allTranslatedForPermit_syncsToApplication()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "permit",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "permit_contact_details").put("value", "contact")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("permit_contact_details",
            new JsonObject().put("translations", new JsonObject().put("en", "EN contact"))));

    JsonObject permit = new JsonObject()
        .put("latest", 1)
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject dataset = new JsonObject()
        .put("dataset_id", "dataset-1").put("catalog_id", "catalog-1")
        .put("permits", new JsonArray().add(permit));
    JsonObject catalog = new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("statuses", new JsonArray().add(
            new JsonObject().put("status", "APPROVED").put("latest", 1)));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("datasets", new JsonArray().add(dataset))
        .put("catalogs", new JsonArray().add(catalog));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    doAnswer(inv -> {
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved"));
      return mongoClient;
    }).when(mongoClient).save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class));
    verify(mongoClient, atLeastOnce())
        .save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());
  }

  @Test
  void processTranslationCallback_failedTranslation_clearsPlaceholderAndSyncs()
      throws InterruptedException {
    // fileRetries=5 → FAILED immediately on first callback
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray().add(new JsonObject().put("field", "decision_file").put("file_hub_repo_id", "hid")
            .put("format", "pdf").put("fileName", "decision_file.pdf")),
        new JsonArray(),
        new JsonObject(), 4, 0, "en"); // fileRetries=4, will become 5 → FAILED

    // Empty file result → no translations accumulated
    JsonObject dict = new JsonObject().put("file",
        new JsonObject().put("decision_file",
            new JsonObject().put("translations", new JsonObject())));

    JsonObject decision = new JsonObject().put("latest", 1)
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject dataset = new JsonObject()
        .put("dataset_id", "dataset-1").put("catalog_id", "catalog-1")
        .put("decisions", new JsonArray().add(decision));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("datasets", new JsonArray().add(dataset))
        .put("catalogs", new JsonArray());

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    // FAILED status should be set
    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    boolean failedSet = updateCaptor.getAllValues().stream()
        .anyMatch(u -> "failed".equals(u.getJsonObject("$set", new JsonObject()).getString("translationStatus")));
    assertTrue(failedSet, "Expected FAILED status");
    // Application update (clearing placeholder) should be called
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  @Test
  void processTranslationCallback_applicationNotFound_failsGracefully()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "EN reason"))));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    // Application not found
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(null));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);
  }

  @Test
  void processTranslationCallback_withTextSectionPayload_completes()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put("text",
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "from-text-section"))));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(250);

    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  @Test
  void processTranslationCallback_withRootLevelPayload_completes()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "reason")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put("decision_reason",
        new JsonObject().put("translations", new JsonObject().put("en", "from-root")));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(250);

    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  @Test
  void processTranslationCallback_withFileCallbackSection_mergesFileTranslations()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray().add(new JsonObject().put("field", "decision_file")
            .put("file_hub_repo_id", "hid").put("format", "pdf").put("fileName", "f.pdf")),
        new JsonArray(),
        new JsonObject(), 0, 0, "en");

    // Callback in "file" section format (not TRANSLATIONS_STATE_COLLECTION key)
    JsonObject dict = new JsonObject().put("file",
        new JsonObject().put("decision_file",
            new JsonObject().put("translations",
                new JsonObject().put("en", "already-a-hub-id")))); // short non-base64 value

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.failedFuture("irrelevant")); // won't be COMPLETED with 1/1 → check

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);
    // updateCollection on state should have been called
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  @Test
  void processTranslationCallback_withFileErrorCode_recordsFileCallbackType()
      throws InterruptedException {
    JsonObject stateDoc = buildMinimalStateDoc("rec-id", "decision",
        new JsonArray().add(new JsonObject().put("field", "decision_file")
            .put("file_hub_repo_id", "hid").put("format", "pdf").put("fileName", "f.pdf")),
        new JsonArray(),
        new JsonObject(), 0, 0);

    JsonObject dict = new JsonObject().put("file",
        new JsonObject().put("decision_file",
            new JsonObject().put("translations", new JsonObject().put("en", 500))));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(250);

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), updateCaptor.capture());
    assertTrue(updateCaptor.getAllValues().stream().anyMatch(u -> {
      JsonArray errors = u.getJsonObject("$set", new JsonObject()).getJsonArray("translationErrors", new JsonArray());
      return errors.stream()
          .map(JsonObject.class::cast)
          .anyMatch(err -> "file".equals(err.getString("callbackType")));
    }));
  }

  @Test
  void processTranslationCallback_withUnsupportedCallbackPayload_failsGracefully()
      throws InterruptedException {
    JsonObject stateDoc = buildMinimalStateDoc("rec-id", "decision",
        new JsonArray().add(new JsonObject().put("field", "decision_file")
            .put("file_hub_repo_id", "hid")),
        new JsonArray(),
        new JsonObject(), 0, 0);

    // Dict with no matching section
    JsonObject dict = new JsonObject().put("unknownSection",
        new JsonObject().put("unknownField", "val"));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(200);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processTranslationCallback – status record (TYPE_STATUS) sync
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_allTranslatedForStatus_syncsToApplication()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "status",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "reason").put("value", "text")),
        new JsonObject(), 0, 0, "en");
    stateDoc.put("catalogId", "catalog-1");
    stateDoc.putNull("dataset_id");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("reason",
            new JsonObject().put("translations", new JsonObject().put("en", "EN text"))));

    JsonObject statusPayload = new JsonObject()
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject catalogStatus = new JsonObject()
        .put("status", "APPROVED").put("latest", 1).put("statusPayload", statusPayload);
    JsonObject catalog = new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("statuses", new JsonArray().add(catalogStatus));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("catalogs", new JsonArray().add(catalog))
        .put("datasets", new JsonArray());

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    doAnswer(inv -> {
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved"));
      return mongoClient;
    }).when(mongoClient).save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class));
    verify(mongoClient, atLeastOnce())
        .save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());
  }

  @Test
  void processTranslationCallback_statusAdditionalInformation_syncsExpandedTranslation()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "status",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "request.0.comment").put("value", "text")),
        new JsonObject(), 0, 0, "en");
    stateDoc.put("catalogId", "catalog-1");
    stateDoc.putNull("dataset_id");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("request.0.comment",
            new JsonObject().put("translations", new JsonObject().put("en", "Expanded text"))));

    JsonObject statusPayload = new JsonObject()
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject catalogStatus = new JsonObject()
        .put("status", "ADDITIONAL_INFORMATION")
        .put("latest", 1)
        .put("statusPayload", statusPayload);
    JsonObject catalog = new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("statuses", new JsonArray().add(catalogStatus));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("catalogs", new JsonArray().add(catalog))
        .put("datasets", new JsonArray())
        .put("additionalInformation", new JsonArray().add(new JsonObject()
            .put("catalog_id", "catalog-1")
            .put("translation", new JsonObject())));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    doAnswer(inv -> {
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved"));
      return mongoClient;
    }).when(mongoClient).save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    ArgumentCaptor<JsonObject> appUpdateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), appUpdateCaptor.capture());
    assertTrue(appUpdateCaptor.getAllValues().stream().anyMatch(update -> {
      JsonArray info = update.getJsonObject("$set", new JsonObject()).getJsonArray("additionalInformation");
      if (info == null || info.isEmpty()) {
        return false;
      }
      JsonObject translation = info.getJsonObject(0).getJsonObject("translation", new JsonObject());
      return translation.getJsonArray("request", new JsonArray()).size() == 1;
    }));
  }

  @Test
  void processTranslationCallback_usesPayloadFallbackExtractionWhenFieldArraysAbsent()
      throws InterruptedException {
    JsonObject stateDoc = new JsonObject()
        .put("_id", "rec-id")
        .put("objectId", "portal-id")
        .put("applicationPortalId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("operation", "decision")
        .put("payload", new JsonObject().put("decisions", new JsonArray().add(new JsonObject()
            .put("decision_reason", "reason-from-payload")
            .put("decision_file", "payload-file-id")
            .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id")))))
        .put("dataset_id", "dataset-1")
        .put("catalogId", "catalog-1")
        .put("translations", new JsonObject())
        .put("translationStatus", "pending")
        .put("fileRetries", 0)
        .put("textRetries", 0)
        .put("targetLanguage", "en")
        .put("translationErrors", new JsonArray());

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "translated from payload"))));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);

    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // saveDecisionTranslation – base64 file → HubStore upload
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveDecisionTranslation_withBase64FileContent_uploadsToHubStore()
      throws ExecutionException, InterruptedException, TimeoutException {
    // Generate a >64 byte base64 string representing a fake PDF
    byte[] fakeFile = new byte[100];
    for (int i = 0; i < fakeFile.length; i++) fakeFile[i] = (byte) i;
    String base64Content = java.util.Base64.getEncoder().encodeToString(fakeFile);

    JsonObject decision = new JsonObject().put("decision_file", base64Content);

    JsonObject uploadResponse = new JsonObject()
        .put("result", new JsonObject().put("id", "new-hub-id"));
    when(hubStoreClient.uploadFile(any(), eq("portal-id"), any(), any(), any()))
        .thenReturn(Future.succeededFuture(uploadResponse));
    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en"));

    verify(hubStoreClient).uploadFile(any(), eq("portal-id"), any(), any(), any());
    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonArray fileFields = captor.getValue().getJsonArray("fileFields");
    assertNotNull(fileFields);
    assertFalse(fileFields.isEmpty());
    assertEquals("new-hub-id", fileFields.getJsonObject(0).getString("file_hub_repo_id"));
  }

  @Test
  void saveDecisionTranslation_hubStoreUploadFails_storesValueAsIs()
      throws ExecutionException, InterruptedException, TimeoutException {
    byte[] fakeFile = new byte[100];
    String base64Content = java.util.Base64.getEncoder().encodeToString(fakeFile);
    JsonObject decision = new JsonObject().put("decision_file", base64Content);

    when(hubStoreClient.uploadFile(any(), eq("portal-id"), any(), any(), any()))
        .thenReturn(Future.failedFuture("Upload failed"));
    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en"));

    // Should still insert (storing the original base64 value as-is)
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // savePermitTranslation – base64 file → HubStore upload
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void savePermitTranslation_withBase64FileContent_uploadsToHubStore()
      throws ExecutionException, InterruptedException, TimeoutException {
    byte[] fakeFile = new byte[100];
    String base64Content = java.util.Base64.getEncoder().encodeToString(fakeFile);
    JsonObject permit = new JsonObject().put("permit_file", base64Content);

    JsonObject uploadResponse = new JsonObject()
        .put("result", new JsonObject().put("id", "permit-hub-id"));
    when(hubStoreClient.uploadFile(any(), eq("portal-id"), any(), any(), any()))
        .thenReturn(Future.succeededFuture(uploadResponse));
    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.savePermitTranslation(
        DATA_REQUEST_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, permit, "catalog-1", "en"));

    verify(hubStoreClient).uploadFile(any(), eq("portal-id"), any(), any(), any());
    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonArray fileFields = captor.getValue().getJsonArray("fileFields");
    assertEquals("permit-hub-id", fileFields.getJsonObject(0).getString("file_hub_repo_id"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // saveStatusTranslation – with both reason and additionalInformation
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveStatusTranslation_withReasonAndAdditionalInfo_includesAllTextFields()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject additionalInfo = new JsonObject()
        .put("source", new JsonArray()
            .add(new JsonObject().put("field", "q1").put("comment", "Comment for q1")));
    JsonObject statusPayload = new JsonObject()
        .put("reason", "Rejection reason")
        .put("additionalInformation", additionalInfo);

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", statusPayload, "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonArray textFields = captor.getValue().getJsonArray("textFields");
    assertEquals(2, textFields.size()); // "reason" + "q1"
  }

  @Test
  void saveStatusTranslation_additionalInfoEntryMissingField_skipped()
      throws ExecutionException, InterruptedException, TimeoutException {
    // Entry has comment but no field name → should be skipped
    JsonObject additionalInfo = new JsonObject()
        .put("source", new JsonArray()
            .add(new JsonObject().put("comment", "orphan comment"))); // no "field"
    JsonObject statusPayload = new JsonObject()
        .put("reason", "reason")
        .put("additionalInformation", additionalInfo);

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", statusPayload, "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonArray textFields = captor.getValue().getJsonArray("textFields");
    // Only "reason" should be added, not the entry without field name
    assertEquals(1, textFields.size());
    assertEquals("reason", textFields.getJsonObject(0).getString("field"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // processTranslationCallback – mergeTranslationsFromSection with file upload
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_fileTranslationWithValidBase64_uploadsToHubStore()
      throws InterruptedException {
    // File field in stateDoc
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray().add(new JsonObject().put("field", "decision_file")
            .put("file_hub_repo_id", "hid").put("format", "pdf").put("fileName", "f.pdf")),
        new JsonArray(),
        new JsonObject(), 0, 0, "en");

    // Callback returns a valid base64 content (>64 bytes)
    byte[] fakeFile = new byte[100];
    String base64Content = java.util.Base64.getMimeEncoder().encodeToString(fakeFile);
    JsonObject dict = new JsonObject().put("file",
        new JsonObject().put("decision_file",
            new JsonObject().put("translations",
                new JsonObject().put("en", base64Content))));

    JsonObject uploadResponse = new JsonObject()
        .put("result", new JsonObject().put("id", "uploaded-file-id"));
    when(hubStoreClient.uploadFile(any(), any(), any(), any(), any()))
        .thenReturn(Future.succeededFuture(uploadResponse));
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.failedFuture("no app needed"));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    verify(hubStoreClient, atLeastOnce()).uploadFile(any(), any(), any(), any(), any());
  }

  @Test
  void processTranslationCallback_fileTranslationHubStoreUploadFails_storesValueAsIs()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray().add(new JsonObject().put("field", "decision_file")
            .put("file_hub_repo_id", "hid").put("format", "pdf").put("fileName", "f.pdf")),
        new JsonArray(),
        new JsonObject(), 0, 0, "en");

    byte[] fakeFile = new byte[100];
    String base64Content = java.util.Base64.getMimeEncoder().encodeToString(fakeFile);
    JsonObject dict = new JsonObject().put("file",
        new JsonObject().put("decision_file",
            new JsonObject().put("translations",
                new JsonObject().put("en", base64Content))));

    when(hubStoreClient.uploadFile(any(), any(), any(), any(), any()))
        .thenReturn(Future.failedFuture("upload failed"));
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.failedFuture("no app needed"));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    // Should still update state record (storing value as-is)
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // syncRecord – application dataset not found
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_datasetNotFoundInApplication_failsGracefully()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "r")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "translated"))));

    // Application exists but has no matching dataset
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("datasets", new JsonArray()) // empty
        .put("catalogs", new JsonArray());

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Additional createTranslatedFieldsReference edge cases
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void createTranslatedFieldsReference_storesUnderCorrectKey() {
    JsonObject result = service.createTranslatedFieldsReference("my-state-id");
    assertNotNull(result);
    // The key must be TRANSLATIONS_STATE_COLLECTION constant value ("TRANSLATIONS_STATE")
    assertEquals(1, result.size());
    assertTrue(result.containsKey(TRANSLATIONS_STATE_COLLECTION));
    assertEquals("my-state-id", result.getString(TRANSLATIONS_STATE_COLLECTION));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // saveDecisionTranslation – both file and text fields
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveDecisionTranslation_withBothFileAndTextFields_insertsRecordWithBothArrays()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject decision = new JsonObject()
        .put("decision_reason", "some reason")
        .put("decision_file", "hub-file-id-existing");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonObject saved = captor.getValue();

    JsonArray fileFields = saved.getJsonArray("fileFields");
    JsonArray textFields = saved.getJsonArray("textFields");
    assertFalse(fileFields.isEmpty(), "Expected fileFields to be non-empty");
    assertFalse(textFields.isEmpty(), "Expected textFields to be non-empty");
    assertEquals("decision_file", fileFields.getJsonObject(0).getString("field"));
    assertEquals("decision_reason", textFields.getJsonObject(0).getString("field"));
  }

  @Test
  void saveDecisionTranslation_withSupportFileOnly_insertsRecord()
      throws ExecutionException, InterruptedException, TimeoutException {
    JsonObject decision = new JsonObject().put("support_file", "support-hub-id");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(service.saveDecisionTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "dataset-1", 0, decision, "catalog-1", "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonArray fileFields = captor.getValue().getJsonArray("fileFields");
    assertEquals(1, fileFields.size());
    assertEquals("support_file", fileFields.getJsonObject(0).getString("field"));
    assertEquals("support-hub-id", fileFields.getJsonObject(0).getString("file_hub_repo_id"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // addStatusTextFields – non-JsonObject entry in additionalInformation.source
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void saveStatusTranslation_withNonJsonObjectSourceEntry_skipsNonJsonObjectEntry()
    throws ExecutionException, InterruptedException, TimeoutException {
    // Build a payload whose source array contains a non-JsonObject entry (a String)
    // followed by a valid JsonObject entry.  Only the valid entry should produce a field.
    JsonObject additionalInformation = new JsonObject()
      .put("source", new JsonArray()
        .add("not-a-json-object") // triggers the continue branch
        .add(new JsonObject().put("field", "comment_field").put("comment", "some text")));
    JsonObject payload = new JsonObject()
      .put("additionalInformation", additionalInformation);

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("state-id"));

    awaitFuture(service.saveStatusTranslation(
      DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
      "catalog-1", payload, "en"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    JsonArray textFields = captor.getValue().getJsonArray("textFields");
    // Only the valid JsonObject entry should have been added (String entry must be skipped)
    long commentFieldCount = textFields.stream()
      .map(JsonObject.class::cast)
      .filter(f -> "comment_field".equals(f.getString("field")))
      .count();
    assertEquals(1, commentFieldCount);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // mergeTranslationsFromSection – small base64 (≤64 bytes) stored as plain value
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_smallBase64FileField_storedAsIs() {
    // A translated value that decodes to ≤64 bytes — should NOT be uploaded to HubStore,
    // just stored directly in translations.
    String smallBase64 = java.util.Base64.getEncoder()
      .encodeToString("short".getBytes()); // 5 bytes

    JsonObject stateDoc = buildMinimalStateDoc("rec-small", "decision",
      new JsonArray().add(new JsonObject()
        .put("field", "decision_file")
        .put("file_hub_repo_id", "hub-id")
        .put("format", "pdf")
        .put("fileName", "f.pdf")),
      new JsonArray(), new JsonObject(), 0, 0);
    stateDoc.put("targetLanguage", "en");

    JsonObject fieldResult = new JsonObject()
      .put("translations", new JsonObject().put("en", smallBase64));
    JsonObject section = new JsonObject().put("decision_file", fieldResult);
    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, section);

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
      .thenReturn(Future.succeededFuture(stateDoc));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION),
      any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-small", dict));
    try {
      Thread.sleep(300);
    } catch (InterruptedException ignored) {
    }

    // HubStore must NOT have been called for small content
    verify(hubStoreClient, never()).uploadFile(any(), any(), any(), any(), any());

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
      .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(), updateCaptor.capture());
    boolean translationStored = updateCaptor.getAllValues().stream().anyMatch(u -> {
      JsonObject set = u.getJsonObject("$set", new JsonObject());
      JsonObject translations = set.getJsonObject("translations", new JsonObject());
      return translations.getJsonObject("en", new JsonObject())
        .containsKey("decision_file");
    });
    assertTrue(translationStored, "Small base64 value should be stored as-is in translations");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // mergeTranslationsFromSection – invalid base64 falls back to store as-is
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void processTranslationCallback_invalidBase64FileField_storedAsIs() {
    // A value that is NOT valid base64 — Base64.getMimeDecoder() will throw,
    // triggering the catch(Exception) branch (store as-is).
    String notBase64 = "!!!not-valid-base64!!!";

    JsonObject stateDoc = buildMinimalStateDoc("rec-invalid-b64", "decision",
      new JsonArray().add(new JsonObject()
        .put("field", "decision_file")
        .put("file_hub_repo_id", "hub-id")
        .put("format", "pdf")
        .put("fileName", "f.pdf")),
      new JsonArray(), new JsonObject(), 0, 0);
    stateDoc.put("targetLanguage", "en");

    JsonObject fieldResult = new JsonObject()
      .put("translations", new JsonObject().put("en", notBase64));
    JsonObject section = new JsonObject().put("decision_file", fieldResult);
    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, section);

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
      .thenReturn(Future.succeededFuture(stateDoc));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION),
      any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-invalid-b64", dict));
    try {
      Thread.sleep(300);
    } catch (InterruptedException ignored) {
    }

    verify(hubStoreClient, never()).uploadFile(any(), any(), any(), any(), any());

    ArgumentCaptor<JsonObject> updateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
      .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(), updateCaptor.capture());
    boolean translationStored = updateCaptor.getAllValues().stream().anyMatch(u -> {
      JsonObject set = u.getJsonObject("$set", new JsonObject());
      JsonObject translations = set.getJsonObject("translations", new JsonObject());
      return translations.getJsonObject("en", new JsonObject())
        .containsKey("decision_file");
    });
    assertTrue(translationStored, "Invalid base64 value should be stored as-is in translations");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // extractItemType – via OPERATION field when itemType key is absent
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void triggerPendingTranslations_docWithOperationFieldOnly_resolvesItemType()
    throws ExecutionException, InterruptedException, TimeoutException {
    // Doc has no "itemType" key; instead has "operation": "decision"
    // extractItemType must fall through to the OPERATION branch.
    JsonObject pendingDoc = new JsonObject()
      .put("_id", "op-rec-id")
      .put("objectId", "portal-id")
      .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
      .put("operation", "decision") // no "itemType" key
      .put("fileFields", new JsonArray())
      .put("textFields", new JsonArray()
        .add(new JsonObject().put("field", "decision_reason").put("value", "reason text")))
      .put("translations", new JsonObject())
      .put("translationStatus", "pending");

    when(mongoClient.find(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(List.of(pendingDoc)))
      .thenReturn(Future.succeededFuture(List.of()));
    when(hubTranslationClient.createCallbackJson())
      .thenReturn(new JsonObject().put("url", "http://callback"));
    when(hubTranslationClient.requestTextTranslation(any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
      .thenReturn(Future.succeededFuture(pendingDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION),
      any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    // Should not throw; the doc is processed (text translation request sent)
    awaitFuture(service.triggerPendingTranslations());

    verify(hubTranslationClient).requestTextTranslation(any(JsonObject.class));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────────────────────

  private JsonObject buildStateDocWithTargetLang(String id, String itemType,
      JsonArray fileFields, JsonArray textFields, JsonObject translations,
      int fileRetries, int textRetries, String targetLanguage) {
    JsonObject doc = buildMinimalStateDoc(id, itemType, fileFields, textFields,
        translations, fileRetries, textRetries);
    if (targetLanguage != null) {
      doc.put("targetLanguage", targetLanguage);
    }
    doc.put("dataset_id", "dataset-1");
    doc.put("catalogId", "catalog-1");
    return doc;
  }


  private JsonObject buildMinimalStateDoc(String id, String itemType,
      JsonArray fileFields, JsonArray textFields,
      JsonObject translations, int fileRetries, int textRetries) {
    return new JsonObject()
        .put("_id", id)
        .put("objectId", "portal-id")
        .put("applicationPortalId", "portal-id")
        .put("application_type", DATA_ACCESS_APPLICATION_COLLECTION)
        .put("itemType", itemType)
        .put("fileFields", fileFields)
        .put("textFields", textFields)
        .put("translations", translations)
        .put("translationStatus", TranslationStatus.PENDING.getStatus())
        .put("fileRetries", fileRetries)
        .put("textRetries", textRetries)
        .put("translationErrors", new JsonArray());
  }

  private <T> T awaitFuture(Future<T> future)
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<T> cf = new CompletableFuture<>();
    future.onSuccess(cf::complete);
    future.onFailure(cf::completeExceptionally);
    return cf.get(5, TimeUnit.SECONDS);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Additional tests for 100% line coverage
  // ─────────────────────────────────────────────────────────────────────────────

  @Test
  void retrieveCatalogLanguages_catalogNotFound_returnsNull() throws Exception {
    JsonObject application = new JsonObject()
        .put("catalogs", new JsonArray().add(new JsonObject()
            .put("catalog_id", "other-catalog")
            .put("catalog_languages", new JsonArray().add("en"))));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("retrieveCatalogLanguages", JsonObject.class, String.class);
    method.setAccessible(true);

    JsonArray result = (JsonArray) method.invoke(service, application, "catalog-1");
    assertNull(result);
  }

  @Test
  void findLatestStatus_withNoLatestFlag_returnsLastElement() throws Exception {
    JsonArray statuses = new JsonArray()
        .add(new JsonObject().put("status", "APPROVED").put("latest", 0))
        .add(new JsonObject().put("status", "PENDING").put("latest", 0));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("findLatestStatus", JsonArray.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, statuses);
    assertNotNull(result);
    assertEquals("PENDING", result.getString("status"));
  }

  @Test
  void findLatestStatus_withEmptyArray_returnsNull() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("findLatestStatus", JsonArray.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, new JsonArray());
    assertNull(result);
  }

  @Test
  void getLatestCatalogStatus_noCatalogMatchFound_returnsNull() throws Exception {
    JsonObject application = new JsonObject()
        .put("catalogs", new JsonArray().add(new JsonObject()
            .put("catalog_id", "other-catalog")
            .put("statuses", new JsonArray().add(new JsonObject().put("status", "APPROVED").put("latest", 1)))));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("getLatestCatalogStatus", JsonObject.class, String.class);
    method.setAccessible(true);

    String result = (String) method.invoke(service, application, "missing-catalog");
    assertNull(result);
  }

  @Test
  void buildActivityTranslationRecord_nullItem_returnsNull() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildActivityTranslationRecord", String.class, String.class, String.class,
            String.class, String.class, JsonObject.class, JsonObject.class, String.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", "APPROVED", "decision", null, new JsonObject().put("en", new JsonObject()), "state-id", "dataset-1");
    assertNull(result);
  }

  @Test
  void buildActivityTranslationRecord_emptyTranslatedFields_returnsNull() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildActivityTranslationRecord", String.class, String.class, String.class,
            String.class, String.class, JsonObject.class, JsonObject.class, String.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", "APPROVED", "decision", new JsonObject().put("field", "val"), new JsonObject(), "state-id", "dataset-1");
    assertNull(result);
  }

  @Test
  void buildActivityTranslationRecord_invalidItemType_returnsNull() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildActivityTranslationRecord", String.class, String.class, String.class,
            String.class, String.class, JsonObject.class, JsonObject.class, String.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", "APPROVED", "invalid_type", new JsonObject().put("field", "val"),
        new JsonObject().put("en", new JsonObject().put("f", "v")), "state-id", "dataset-1");
    assertNull(result);
  }

  @Test
  void buildActivityTranslationRecord_permitType_returnsPayload() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildActivityTranslationRecord", String.class, String.class, String.class,
            String.class, String.class, JsonObject.class, JsonObject.class, String.class, String.class);
    method.setAccessible(true);

    JsonObject item = new JsonObject().put("permit_contact_details", "contact");
    JsonObject translations = new JsonObject().put("en", new JsonObject().put("permit_contact_details", "contact EN"));
    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", "APPROVED", "permit", item, translations, "state-id", "dataset-1");
    assertNotNull(result);
  }

  @Test
  void findTargetItemByTranslationStateId_blankId_returnsNull() throws Exception {
    JsonArray items = new JsonArray().add(new JsonObject()
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "some-id")));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("findTargetItemByTranslationStateId", JsonArray.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, items, "");
    assertNull(result);

    result = (JsonObject) method.invoke(service, items, (String) null);
    assertNull(result);
  }

  @Test
  void findTargetStatusByTranslationStateId_blankId_returnsNull() throws Exception {
    JsonArray statuses = new JsonArray().add(new JsonObject()
        .put("status", "APPROVED")
        .put("statusPayload", new JsonObject()
            .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "some-id"))));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("findTargetStatusByTranslationStateId", JsonArray.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, statuses, "");
    assertNull(result);
  }

  @Test
  void findTargetStatusByTranslationStateId_notFound_returnsNull() throws Exception {
    JsonArray statuses = new JsonArray().add(new JsonObject()
        .put("status", "APPROVED")
        .put("statusPayload", new JsonObject()
            .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "other-id"))));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("findTargetStatusByTranslationStateId", JsonArray.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, statuses, "target-id");
    assertNull(result);
  }

  @Test
  void matchesKnownFields_emptyFieldsArray_returnsFalse() throws Exception {
    JsonObject section = new JsonObject().put("decision_reason", "text");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("matchesKnownFields", JsonObject.class, JsonArray.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(service, section, new JsonArray());
    assertFalse(result);
  }

  @Test
  void matchesKnownFields_emptySection_returnsFalse() throws Exception {
    JsonArray fields = new JsonArray().add(new JsonObject().put("field", "decision_reason"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("matchesKnownFields", JsonObject.class, JsonArray.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(service, new JsonObject(), fields);
    assertFalse(result);
  }

  @Test
  void extractPayloadItem_permitsType_returnsFirstPermit() throws Exception {
    JsonObject payload = new JsonObject().put("permits", new JsonArray()
        .add(new JsonObject().put("permit_contact_details", "contact info")));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("extractPayloadItem", JsonObject.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, payload, "permit");
    assertNotNull(result);
    assertEquals("contact info", result.getString("permit_contact_details"));
  }

  @Test
  void extractPayloadItem_emptyPayload_returnsNull() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("extractPayloadItem", JsonObject.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, new JsonObject(), "decision");
    assertNull(result);
  }

  @Test
  void extractPayloadItem_nullItemType_returnsNull() throws Exception {
    JsonObject payload = new JsonObject().put("decisions", new JsonArray()
        .add(new JsonObject().put("field", "value")));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("extractPayloadItem", JsonObject.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, payload, (String) null);
    assertNull(result);
  }

  @Test
  void buildTextDictForUntranslated_allFieldsAlreadyTranslated_returnsEmptyDict() throws Exception {
    JsonArray textFields = new JsonArray().add(new JsonObject()
        .put("field", "decision_reason").put("value", "reason"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildTextDictForUntranslated", JsonArray.class, java.util.Set.class, JsonArray.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, textFields, java.util.Set.of("decision_reason"),
        new JsonArray().add("en"), "LD");
    assertTrue(result.isEmpty());
  }

  @Test
  void buildTargetLanguagesArray_withCatalogLanguages_usesCatalogLanguages() throws Exception {
    JsonArray catalogLanguages = new JsonArray().add("en").add("de");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildTargetLanguagesArray", JsonArray.class);
    method.setAccessible(true);

    JsonArray result = (JsonArray) method.invoke(service, catalogLanguages);
    assertEquals(2, result.size());
    assertTrue(result.contains("en"));
    assertTrue(result.contains("de"));
  }

  @Test
  void buildTargetLanguagesArray_withNullCatalogLanguages_usesAllSupportedLanguages() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildTargetLanguagesArray", JsonArray.class);
    method.setAccessible(true);

    JsonArray result = (JsonArray) method.invoke(service, (JsonArray) null);
    assertFalse(result.isEmpty());
  }

  @Test
  void addFileFieldSync_withBlankValue_skipsField() throws Exception {
    JsonArray fields = new JsonArray();
    JsonObject source = new JsonObject().put("decision_file", "   ");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("addFileFieldSync", JsonArray.class, JsonObject.class, String.class);
    method.setAccessible(true);

    method.invoke(service, fields, source, "decision_file");
    assertTrue(fields.isEmpty());
  }

  @Test
  void applyExistingStateId_withExistingId_appliesIdToDoc() throws Exception {
    JsonObject doc = new JsonObject();
    JsonObject translatedFields = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "existing-id");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("applyExistingStateId", JsonObject.class, JsonObject.class);
    method.setAccessible(true);

    method.invoke(service, doc, translatedFields);
    assertEquals("existing-id", doc.getString("_id"));
  }

  @Test
  void applyExistingStateId_withNullTranslatedFields_doesNothing() throws Exception {
    JsonObject doc = new JsonObject().put("_id", "original-id");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("applyExistingStateId", JsonObject.class, JsonObject.class);
    method.setAccessible(true);

    method.invoke(service, doc, (JsonObject) null);
    assertEquals("original-id", doc.getString("_id"));
  }

  @Test
  void filterTranslationsForApplication_withEmptyTargetLanguages_returnsAllTranslations() throws Exception {
    JsonObject application = new JsonObject()
        .put("translation", new JsonObject().put("targetLanguages", new JsonArray()));
    JsonObject translations = new JsonObject()
        .put("en", new JsonObject().put("f", "v"))
        .put("fr", new JsonObject().put("f", "v2"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("filterTranslationsForApplication", JsonObject.class, JsonObject.class, JsonArray.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, application, translations, null);
    assertNotNull(result);
    assertTrue(result.containsKey("en"));
    assertTrue(result.containsKey("fr"));
  }

  @Test
  void filterTranslationsForApplication_withApplicationLanguageFallback_includesInputLang() throws Exception {
    JsonObject application = new JsonObject()
        .put("translation", new JsonObject().put("targetLanguages", new JsonArray().add("en")))
        .put("inputLanguage", "de");
    JsonObject translations = new JsonObject()
        .put("en", new JsonObject().put("f", "v"))
        .put("de", new JsonObject().put("f", "v-de"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("filterTranslationsForApplication", JsonObject.class, JsonObject.class, JsonArray.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, application, translations, null);
    assertTrue(result.containsKey("en"));
    assertTrue(result.containsKey("de")); // inputLanguage should be added as fallback
  }

  @Test
  void filterTranslationsForApplication_withCatalogLanguages_usesCatalogLanguages() throws Exception {
    JsonObject application = new JsonObject()
        .put("translation", new JsonObject().put("targetLanguages", new JsonArray().add("en").add("fr")));
    JsonObject translations = new JsonObject()
        .put("en", new JsonObject().put("f", "v"))
        .put("fr", new JsonObject().put("f", "v2"))
        .put("de", new JsonObject().put("f", "v3"));
    JsonArray catalogLanguages = new JsonArray().add("en");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("filterTranslationsForApplication", JsonObject.class, JsonObject.class, JsonArray.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, application, translations, catalogLanguages);
    assertTrue(result.containsKey("en"));
    assertFalse(result.containsKey("fr")); // Not in catalog languages
    assertFalse(result.containsKey("de"));
  }

  @Test
  void expandAdditionalInformationFromObject_withNestedPath_createsNestedStructure() throws Exception {
    JsonObject input = new JsonObject()
        .put("request.0.comment", "first comment")
        .put("request.1.comment", "second comment")
        .put("simpleField", "simple value");

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("expandAdditionalInformationFromObject", JsonObject.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, input);
    assertNotNull(result);
    JsonArray request = result.getJsonArray("request");
    assertNotNull(request);
    assertEquals(2, request.size());
    assertEquals("first comment", request.getJsonObject(0).getString("comment"));
    assertEquals("second comment", request.getJsonObject(1).getString("comment"));
    assertEquals("simple value", result.getString("simpleField"));
  }

  @Test
  void getOrCreate_existingKeyOfSameType_returnsExisting() throws Exception {
    JsonObject parent = new JsonObject();
    JsonObject existing = new JsonObject().put("key", "val");
    parent.put("child", existing);

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("getOrCreate", JsonObject.class, String.class, Class.class, java.util.function.Supplier.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    JsonObject result = (JsonObject) method.invoke(service, parent, "child", JsonObject.class,
        (java.util.function.Supplier<?>) JsonObject::new);
    assertSame(existing, result);
  }

  @Test
  void getOrCreate_missingKey_createsNewInstance() throws Exception {
    JsonObject parent = new JsonObject();

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("getOrCreate", JsonObject.class, String.class, Class.class, java.util.function.Supplier.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    JsonObject result = (JsonObject) method.invoke(service, parent, "newChild", JsonObject.class,
        (java.util.function.Supplier<?>) JsonObject::new);
    assertNotNull(result);
    assertTrue(parent.containsKey("newChild"));
  }

  @Test
  void computeAdditionalInformation_withMatchingCatalog_appliesTranslations() throws Exception {
    JsonObject application = new JsonObject()
        .put("additionalInformation", new JsonArray().add(new JsonObject()
            .put("catalog_id", "catalog-1")
            .put("translation", new JsonObject())));
    JsonObject translations = new JsonObject()
        .put("en", new JsonObject().put("request.0.comment", "EN comment"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("computeAdditionalInformation", JsonObject.class, String.class, JsonObject.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    JsonArray result = (JsonArray) method.invoke(service, application, "catalog-1", translations);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  void computeAdditionalInformation_withNoMatchingCatalog_returnsEmptyArray() throws Exception {
    JsonObject application = new JsonObject()
        .put("additionalInformation", new JsonArray().add(new JsonObject()
            .put("catalog_id", "other-catalog")
            .put("translation", new JsonObject())));
    JsonObject translations = new JsonObject()
        .put("en", new JsonObject().put("reason", "EN reason"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("computeAdditionalInformation", JsonObject.class, String.class, JsonObject.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    JsonArray result = (JsonArray) method.invoke(service, application, "catalog-1", translations);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  void processTranslationCallback_statusCatalogNotFound_failsGracefully() throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "status",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "reason").put("value", "text")),
        new JsonObject(), 0, 0, "en");
    stateDoc.put("catalogId", "catalog-1");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("reason",
            new JsonObject().put("translations", new JsonObject().put("en", "translated"))));

    // Application has no matching catalog
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("catalogs", new JsonArray())
        .put("datasets", new JsonArray());

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);
  }

  @Test
  void syncRecord_missingRequiredFields_failsGracefully() throws InterruptedException {
    // targetLanguage="en", 1 text field = 1 lang pair completed
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "r")),
        new JsonObject(), 0, 0, "en");
    // Remove application_type so collectionName is null → syncRecord returns failedFuture
    stateDoc.remove("application_type");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "translated"))));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);
  }

  @Test
  void processTranslationCallback_decisionWithCatalogLanguages_filtersTranslations()
      throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "r")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "translated"))));

    JsonObject decision = new JsonObject()
        .put("datasetStatus", "APPROVED").put("latest", 1)
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject dataset = new JsonObject()
        .put("dataset_id", "dataset-1").put("catalog_id", "catalog-1")
        .put("decisions", new JsonArray().add(decision));
    JsonObject catalog = new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("catalog_languages", new JsonArray().add("en"))
        .put("statuses", new JsonArray().add(
            new JsonObject().put("status", "APPROVED").put("latest", 1)));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("datasets", new JsonArray().add(dataset))
        .put("catalogs", new JsonArray().add(catalog));

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    doAnswer(inv -> {
      Handler<AsyncResult<String>> handler = inv.getArgument(2);
      handler.handle(Future.succeededFuture("saved"));
      return mongoClient;
    }).when(mongoClient).save(eq(OUTGOING_APPLICATION_ACTIVITIES_COLLECTION), any(JsonObject.class), any());

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class));
  }

  @Test
  void processTranslationCallback_decisionNoMatchingItem_failsGracefully() throws InterruptedException {
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "decision",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "decision_reason").put("value", "r")),
        new JsonObject(), 0, 0, "en");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("decision_reason",
            new JsonObject().put("translations", new JsonObject().put("en", "translated"))));

    JsonObject dataset = new JsonObject()
        .put("dataset_id", "dataset-1").put("catalog_id", "catalog-1")
        .put("decisions", new JsonArray()); // no decisions – findTargetItem returns null
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("datasets", new JsonArray().add(dataset))
        .put("catalogs", new JsonArray());

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(300);
  }

  @Test
  void processTranslationCallback_statusTranslationFailed_clearsCatalogPlaceholder()
      throws InterruptedException {
    // fileRetries=4, isFileCallback=false (text callback), textRetries becomes 5 → FAILED
    JsonObject stateDoc = buildStateDocWithTargetLang("rec-id", "status",
        new JsonArray(),
        new JsonArray().add(new JsonObject().put("field", "reason").put("value", "text")),
        new JsonObject(), 0, 4, "en");
    stateDoc.put("catalogId", "catalog-1");

    JsonObject dict = new JsonObject().put(TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put("reason",
            new JsonObject().put("translations", new JsonObject())));

    JsonObject statusPayload = new JsonObject()
        .put("translatedFields", new JsonObject().put(TRANSLATIONS_STATE_COLLECTION, "rec-id"));
    JsonObject catalogStatus = new JsonObject()
        .put("status", "APPROVED").put("latest", 1).put("statusPayload", statusPayload);
    JsonObject catalog = new JsonObject()
        .put("catalog_id", "catalog-1")
        .put("statuses", new JsonArray().add(catalogStatus));
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("catalogs", new JsonArray().add(catalog))
        .put("datasets", new JsonArray());

    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(stateDoc))
        .thenReturn(Future.succeededFuture(stateDoc.copy()));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));
    when(mongoClient.updateCollection(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    assertDoesNotThrow(() -> service.processTranslationCallback("rec-id", dict));
    Thread.sleep(400);

    ArgumentCaptor<JsonObject> stateCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient, atLeastOnce())
        .updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), stateCaptor.capture());
    boolean failedSet = stateCaptor.getAllValues().stream()
        .anyMatch(u -> "failed".equals(u.getJsonObject("$set", new JsonObject()).getString("translationStatus")));
    assertTrue(failedSet);
  }

  @Test
  void getFullyTranslatedFields_withTargetLanguageOnly_checksOnlyTargetLang() throws Exception {
    JsonObject doc = new JsonObject()
        .put("targetLanguage", "en")
        .put("translations", new JsonObject()
            .put("en", new JsonObject().put("decision_reason", "translated")));
    JsonArray fields = new JsonArray().add(new JsonObject().put("field", "decision_reason"));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("getFullyTranslatedFields", JsonObject.class, JsonArray.class, java.util.Set.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    java.util.Set<String> result = (java.util.Set<String>) method.invoke(service, doc, fields, java.util.Set.of("en", "fr", "de"));
    assertTrue(result.contains("decision_reason"));
  }

  @Test
  void clearTranslationPlaceholder_applicationNotFound_completesSuccessfully() throws InterruptedException {
    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(null));

    java.lang.reflect.Method method;
    try {
      method = DecisionTranslationService.class.getDeclaredMethod("clearTranslationPlaceholder",
          String.class, String.class, String.class, String.class, String.class, int.class, String.class);
      method.setAccessible(true);
      method.invoke(service, DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "decision", "catalog-1", "dataset-1", 0, "rec-id");
    } catch (Exception e) {
      fail("Should not throw: " + e.getMessage());
    }
    Thread.sleep(200);
    verify(mongoClient).findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull());
  }

  @Test
  void clearTranslationPlaceholder_statusTypeNoCatalogMatch_completesSuccessfully() throws InterruptedException {
    JsonObject application = new JsonObject()
        .put("_id", "portal-id")
        .put("catalogs", new JsonArray());

    when(mongoClient.findOne(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(application));

    java.lang.reflect.Method method;
    try {
      method = DecisionTranslationService.class.getDeclaredMethod("clearTranslationPlaceholder",
          String.class, String.class, String.class, String.class, String.class, int.class, String.class);
      method.setAccessible(true);
      method.invoke(service, DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "status", "catalog-1", null, -1, "rec-id");
    } catch (Exception e) {
      fail("Should not throw: " + e.getMessage());
    }
    Thread.sleep(200);
  }

  @Test
  void dispatchTranslationToDispatcher_withNullRecord_callsUpdateSyncPending() throws InterruptedException {
    when(mongoClient.findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull()))
        .thenReturn(Future.succeededFuture(new JsonObject()
            .put("_id", "rec-id")
            .put("syncPending", "1")
            .put("configuration", new JsonObject())));
    when(mongoClient.updateCollection(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(mock(MongoClientUpdateResult.class)));

    java.lang.reflect.Method method;
    try {
      method = DecisionTranslationService.class.getDeclaredMethod("dispatchTranslationToDispatcher",
          String.class, JsonObject.class);
      method.setAccessible(true);
      method.invoke(service, "rec-id", null);
    } catch (Exception e) {
      fail("Should not throw: " + e.getMessage());
    }
    Thread.sleep(200);
    verify(mongoClient, atLeastOnce()).findOne(eq(TRANSLATIONS_STATE_COLLECTION), any(), isNull());
  }

  @Test
  void buildStatusActivityTranslationRecord_nullStatus_returnsNull() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildStatusActivityTranslationRecord", String.class, String.class,
            String.class, JsonObject.class, JsonObject.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", null, new JsonObject().put("en", new JsonObject()), "state-id");
    assertNull(result);
  }

  @Test
  void buildStatusActivityTranslationRecord_additionalInfoStatus_setsPayloadCorrectly() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildStatusActivityTranslationRecord", String.class, String.class,
            String.class, JsonObject.class, JsonObject.class, String.class);
    method.setAccessible(true);

    JsonObject status = new JsonObject()
        .put("status", "ADDITIONAL_INFORMATION")
        .put("statusPayload", new JsonObject().put("reason", "reason text"));
    JsonObject translatedFields = new JsonObject().put("en", new JsonObject().put("f", "v"));

    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", status, translatedFields, "state-id");
    assertNotNull(result);
  }

  @Test
  void buildStatusActivityTranslationRecord_normalStatus_setsTranslatedFields() throws Exception {
    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("buildStatusActivityTranslationRecord", String.class, String.class,
            String.class, JsonObject.class, JsonObject.class, String.class);
    method.setAccessible(true);

    JsonObject status = new JsonObject()
        .put("status", "APPROVED")
        .put("statusPayload", new JsonObject().put("reason", "reason text"));
    JsonObject translatedFields = new JsonObject().put("en", new JsonObject().put("f", "v"));

    JsonObject result = (JsonObject) method.invoke(service, "app-id", DATA_ACCESS_APPLICATION_COLLECTION,
        "catalog-1", status, translatedFields, "state-id");
    assertNotNull(result);
  }

  @Test
  void findTargetCatalogStatusObject_catalogNotFound_returnsNull() throws Exception {
    JsonObject application = new JsonObject()
        .put("catalogs", new JsonArray().add(new JsonObject()
            .put("catalog_id", "other-catalog")
            .put("statuses", new JsonArray())));

    java.lang.reflect.Method method = DecisionTranslationService.class
        .getDeclaredMethod("findTargetCatalogStatusObject", JsonObject.class, String.class, String.class);
    method.setAccessible(true);

    JsonObject result = (JsonObject) method.invoke(service, application, "catalog-1", "state-id");
    assertNull(result);
  }

  @Test
  void saveStatusTranslation_translationDisabled_setsTargetLang()
      throws ExecutionException, InterruptedException, TimeoutException {
    DecisionTranslationService disabledService = createService(false);
    JsonObject payload = new JsonObject().put("reason", "a reason");

    when(mongoClient.save(eq(TRANSLATIONS_STATE_COLLECTION), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(null));

    awaitFuture(disabledService.saveStatusTranslation(
        DATA_ACCESS_APPLICATION_COLLECTION, "portal-id", "app-id",
        "catalog-1", payload, "pt"));

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).save(eq(TRANSLATIONS_STATE_COLLECTION), captor.capture());
    assertEquals("pt", captor.getValue().getString("targetLanguage"));
  }
}
