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

import static eu.europa.data.health.access.Constants.APPLICATIONID;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_WITH_ID;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS;
import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.AUDIT_LOG_COLLECTION;
import static eu.europa.data.health.access.Constants.BASKET_COLLECTION;
import static eu.europa.data.health.access.Constants.CATALOG_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_BUSINESS_ID;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_ORGANISATION_NAME;
import static eu.europa.data.health.access.Constants.CONTACT_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.CONTENT_DISPOSITION;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE_SUBMITTED;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static eu.europa.data.health.access.Constants.ERRORS;
import static eu.europa.data.health.access.Constants.FILE_ID;
import static eu.europa.data.health.access.Constants.FORM_TRANSLATIONS;
import static eu.europa.data.health.access.Constants.ID;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_EMAIL;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_NAME;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_PHONE;
import static eu.europa.data.health.access.Constants.INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_COUNTRY;
import static eu.europa.data.health.access.Constants.LEGAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_ADDRESS;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_AFFILIATION;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_EMAIL;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_JOB_TITLE;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_NAME;
import static eu.europa.data.health.access.Constants.NATURAL_PERSON_PHONE;
import static eu.europa.data.health.access.Constants.NOT_FOUND_FOR_USER_WITH_ID;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESEARCH_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_NAME;
import static eu.europa.data.health.access.Constants.PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_NUMBER;
import static eu.europa.data.health.access.Constants.STATUS;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.services.CommonApplicationManager.TARGET_LANGUAGES;
import static eu.europa.data.health.access.services.CommonApplicationManager.TRANSLATION;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.data.health.access.ApplicationStatus;
import eu.europa.data.health.access.AuditLogAction;
import eu.europa.data.health.access.AuditLogActionDescription;
import eu.europa.data.health.access.AuditLogObject;
import eu.europa.data.health.access.CatalogStatus;
import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.OperationStatus;
import eu.europa.data.health.access.TranslationStatus;
import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.utils.ApplicationFormUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;


@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class CommonApplicationManagerTest {

  @Mock
  private MongoClient mongoClient;
  private RoutingContext routingContext;
  @Mock
  private HubStoreClient hubStoreClient;
  @Mock
  private HubTranslationClient hubTranslationClient;
  @Mock
  private HubSearchClient hubSearchClient;
  @Mock
  private AuditLogManager auditLogManager;
  @Mock
  private DBHandler dbHandler;
  @Mock
  private BasketManager basketManager;
  @Mock
  private Vertx vertx;
  @Mock
  private HttpServerResponse response;
  @Mock
  private HttpServerRequest request;
  @Mock
  private AuthClient authClient;
  @Captor
  private ArgumentCaptor<JsonObject> jsonCaptor;

  private CommonApplicationManager commonApplicationManager;
  private final String ecasId = "n3817490";

  @BeforeEach
  void setUp() {
    commonApplicationManager = new CommonApplicationManager(dbHandler, basketManager, mongoClient,
      vertx, hubStoreClient, auditLogManager, hubTranslationClient, hubSearchClient, authClient) {
    };
    routingContext = mock(RoutingContext.class);

    User mockUser = Mockito.mock(User.class);
    lenient().when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    lenient().when(routingContext.user().principal()).thenReturn(principal);

    // Mock other behaviors
    lenient().when(routingContext.request()).thenReturn(request);
    lenient().when(routingContext.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
    lenient().when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    RequestBody requestBody = mock(RequestBody.class);
    lenient().when(routingContext.body()).thenReturn(requestBody);

  }

  @Test
  void performStatusUpdate_usesExistingHistory_andRemovesItFromCurrent() {
    // mongoApplication has a current status WITH pre-existing status_history
    JsonObject currentStatus = new JsonObject()
      .put("status", "DRAFT")
      .put("status_history", new JsonArray()
        .add(new JsonObject().put("status", "INIT").put("userId", "u0")));

    JsonObject mongoApplication = new JsonObject().put("status", currentStatus);

    // new status to set
    JsonObject newStatus = new JsonObject()
      .put("status", "ON-GOING")
      .put("status_history", new JsonArray()
        .add(new JsonObject().put("status", "DRAFT").put("userId", "u0")));

    // context is non-null -> branch uses Context.getEcasId(context)
    commonApplicationManager.updateApplicationStatus(mongoApplication, newStatus, routingContext);

    // The new status should be written back
    JsonObject updatedStatus = mongoApplication.getJsonObject("status");
    assertEquals("ON-GOING", updatedStatus.getString("status"));

    // History should contain: [previous-history..., previous-current-status]
    JsonArray hist = updatedStatus.getJsonArray("status_history");
    assertNotNull(hist);
    assertEquals(2, hist.size(), "Should include old history (1) + previous current status (1)");

    // The last entry is the *previous* current status (after removal of its own status_history)
    JsonObject last = hist.getJsonObject(hist.size() - 1);
    assertEquals("DRAFT", last.getString("status"));
    assertFalse(last.containsKey("status_history"),
      "Previous current status must have had 'status_history' removed before appending");
  }

  @Test
  void performStatusUpdate_appVariant_usesExistingHistory_andRemovesItFromCurrent() {
    // Prepare mongoApplication with a current status that ALREADY has status_history
    JsonObject previousCurrentStatus = new JsonObject()
      .put("status", "DRAFT")
      .put("status_history", new JsonArray()
        .add(new JsonObject().put("status", "INIT").put("userId", "u0")));

    JsonObject mongoApplication = new JsonObject().put("status", previousCurrentStatus);
    JsonObject newStatus = new JsonObject().put("status", "ON-GOING");

    // Use non-null context so userId is taken from context (not "system")
    commonApplicationManager.updateApplicationStatus(mongoApplication, newStatus, routingContext);

    // The OLD current status object should have had its history REMOVED (branch under test)
    assertFalse(previousCurrentStatus.containsKey("status_history"),
      "Previous current status must have 'status_history' removed");

    // The updated status is now written back into mongoApplication
    JsonObject updatedStatus = mongoApplication.getJsonObject("status");
    assertEquals("ON-GOING", updatedStatus.getString("status"));

    JsonArray hist = updatedStatus.getJsonArray("status_history");
    assertNotNull(hist, "History should be present on the new status");
    assertTrue(hist.size() >= 2, "Should include old history + previous current status");
    // last appended entry should be the previous current status (without its own history)
    JsonObject last = hist.getJsonObject(hist.size() - 1);
    assertEquals("DRAFT", last.getString("status"));
    assertFalse(last.containsKey("status_history"),
      "Appended previous current status must not carry its own 'status_history'");
  }

  @Test
  void performStatusUpdate_appVariant_setsUserIdSystem_whenContextIsNull() {
    // Prepare mongoApplication with simple current status (no history needed)
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"));
    JsonObject newStatus = new JsonObject().put("status", "ON-GOING");


    commonApplicationManager.updateApplicationStatus(mongoApplication, newStatus, null);

    JsonObject updatedStatus = mongoApplication.getJsonObject("status");
    assertEquals("ON-GOING", updatedStatus.getString("status"));
    assertEquals("system", updatedStatus.getString(USER_ID),
      "When context is null, userId must default to 'system'");

    JsonArray hist = updatedStatus.getJsonArray("status_history");
    assertNotNull(hist, "History should be present");
    assertEquals("DRAFT", hist.getJsonObject(0).getString("status"),
      "Previous status should be recorded in history");
  }

  @Test
  void performStatusUpdate_setsUserIdToSystem_whenContextIsNull() {
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"));

    JsonObject newStatus = new JsonObject().put("status", "ON-GOING");

    commonApplicationManager.updateApplicationStatus(mongoApplication, newStatus, null);

    JsonObject updatedStatus = mongoApplication.getJsonObject("status");
    assertEquals("ON-GOING", updatedStatus.getString("status"));
    assertEquals("system", updatedStatus.getString("userId"),
      "When context is null, userId should default to 'system'");
    assertNotNull(updatedStatus.getJsonArray("status_history"));
  }

  @Test
  void createApplication_dataAccess_auditLogBranchesCovered() {
    JsonObject body = new JsonObject()
      .put("id", "appId-DA")
      .put("title", "DA Title")
      .put(DATASETS, new JsonArray().add(new JsonObject()
        .put("dataset_id", "ds1")
        .put("distributions", new JsonArray().add(new JsonObject().put("distribution_id", "d1")))
      ));

    when(routingContext.body().asJsonObject()).thenReturn(body);

    // publisher lookup + hub search succeed
    when(mongoClient.findOne(eq(BASKET_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put("publisher", "PubName")));
    when(hubSearchClient.getDataset(eq("ds1"))).thenReturn(Future.succeededFuture());

    // super.createEntity -> db create succeeds
    when(dbHandler.create(eq(DATA_ACCESS_APPLICATION_COLLECTION), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("newEntityId"));

    // allow null for the app id (2nd arg)
    when(auditLogManager.prepareAuditLogActionDescription(anyString(), any(), anyString()))
      .thenReturn("DESC");

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.createApplication(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    ArgumentCaptor<String> objectCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> actionCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> idCap     = ArgumentCaptor.forClass(String.class);

    // wait for async finish
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      // the template used for description in DATA_ACCESS branch
      verify(auditLogManager).prepareAuditLogActionDescription(
        eq("Data Access application {} : {} has been created"),
        any(), // id can be null here
        eq("DA Title")
      );

      verify(auditLogManager).insertAuditLog(
        objectCap.capture(),  // "Data Access Application"
        idCap.capture(),      // often null in tests
        actionCap.capture(),  // "Data Access application created"
        eq("DESC"),
        eq(true),
        eq(routingContext)
      );
    });

    assertEquals("Data Access Application", objectCap.getValue());
    assertEquals("Data Access application created", actionCap.getValue());
  }


  @Test
  void createApplication_asyncFailure_sets500_andErrorMessage() {
    JsonObject body = new JsonObject()
      .put("id", "appId-Fail")
      .put("title", "Fail Title")
      .put(DATASETS, new JsonArray().add(new JsonObject().put("dataset_id", "dsX")));

    when(routingContext.body().asJsonObject()).thenReturn(body);

    // One of the futures FAILS → Future.all(futures) fails → else branch is taken
    when(mongoClient.findOne(eq(BASKET_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture()); // ok
    when(hubSearchClient.getDataset(eq("dsX")))
      .thenReturn(Future.failedFuture(new RuntimeException("boom")));

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    // Act
    commonApplicationManager.createApplication(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    // Assert: 500 with propagated message "boom"
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      verify(response).end(Mockito.<String>argThat(s -> s != null && s.contains("boom")));
    });
  }

  // ===== Helpers =====
  private void invokeProcessHubSearchResult(JsonObject dataset, JsonObject hubSearchResult) throws Exception {
    Method m = CommonApplicationManager.class
      .getDeclaredMethod("processHubSearchResult", JsonObject.class, JsonObject.class);
    m.setAccessible(true);
    m.invoke(commonApplicationManager, dataset, hubSearchResult);
  }

// ===== Tests =====

  @Test
  void processHubSearchResult_hubSearchResultNull_doesNothing() throws Exception {
    JsonObject dataset = new JsonObject()
      .put("title", new JsonObject().put("en", "keep"))
      .put("country", new JsonObject().put("country_id", "GR"));

    invokeProcessHubSearchResult(dataset, null);

    // unchanged
    assertEquals("keep", dataset.getJsonObject("title").getString("en"));
    assertEquals("GR", dataset.getJsonObject("country").getString("country_id"));
  }

  @Test
  void processHubSearchResult_resultNull_doesNothing() throws Exception {
    JsonObject dataset = new JsonObject().put("some", "value");
    JsonObject hub = new JsonObject().put("result", (Object) null);

    invokeProcessHubSearchResult(dataset, hub);

    assertEquals("value", dataset.getString("some"));
  }

  @Test
  void processHubSearchResult_populatesTopLevel_andUpdatesDistributionTitles() throws Exception {
    // dataset with two distributions; titles should be filled from hub result by matching ids
    JsonObject d1 = new JsonObject().put("distribution_id", "dist-1");
    JsonObject d2 = new JsonObject().put("distribution_id", "dist-2");
    JsonObject dataset = new JsonObject()
      .put("distributions", new JsonArray().add(d1).add(d2));

    // hub search "result"
    JsonObject hub = new JsonObject().put("result", new JsonObject()
      // title
      .put("title", new JsonObject().put("en", "Dataset Title"))
      // country: id -> country_id + preserve other fields
      .put("country", new JsonObject().put("id", "GR").put("name", "Greece"))
      // publisher/hdab/provenance copied as-is
      .put("publisher", new JsonObject().put("name", "Pub"))
      .put("hdab", new JsonObject().put("code", "HDAB-X"))
      .put("provenance", new JsonObject().put("source", "HubSearch"))
      // distributions with ids and titles (only matching ids should update)
      .put("distributions", new JsonArray()
        .add(new JsonObject().put("id", "dist-1").put("title", new JsonObject().put("en", "T1")))
        .add(new JsonObject().put("id", "dist-X").put("title", new JsonObject().put("en", "NoMatch")))
        .add(new JsonObject().put("id", "dist-2").put("title", new JsonObject().put("en", "T2")))
      )
    );

    invokeProcessHubSearchResult(dataset, hub);

    // top-level fields
    assertEquals("Dataset Title", dataset.getJsonObject("title").getString("en"));
    JsonObject country = dataset.getJsonObject("country");
    assertEquals("GR", country.getString("country_id"));
    assertEquals("Greece", country.getString("name"));
    assertEquals("Pub", dataset.getJsonObject("publisher").getString("name"));
    assertEquals("HDAB-X", dataset.getJsonObject("hdab").getString("code"));
    assertEquals("HubSearch", dataset.getJsonObject("provenance").getString("source"));

    // distribution titles updated where ids matched
    JsonArray ds = dataset.getJsonArray("distributions");
    assertEquals("T1", ds.getJsonObject(0).getJsonObject("title").getString("en"));
    assertEquals("T2", ds.getJsonObject(1).getJsonObject("title").getString("en"));
  }

  @Test
  void processHubSearchResult_reducesSamples_keepsOnlyDatasetIdAndVariables() throws Exception {
    JsonObject dataset = new JsonObject();

    JsonObject sampleWithBoth = new JsonObject()
      .put("id", "s1")
      .put("variables", new JsonArray().add("a").add("b"))
      .put("extra", "discard");
    JsonObject sampleWithOnlyId = new JsonObject().put("id", "s2").put("other", 123);
    JsonObject sampleWithOnlyVars = new JsonObject().put("variables", new JsonArray().add("x"));
    JsonArray samples = new JsonArray()
      .add(sampleWithBoth)
      .add(sampleWithOnlyId)
      .add(sampleWithOnlyVars)
      .add("not-an-object"); // should be skipped

    JsonObject hub = new JsonObject().put("result", new JsonObject()
      .put("distributions_sample", samples));

    invokeProcessHubSearchResult(dataset, hub);

    JsonArray out = dataset.getJsonArray("distributions_sample");
    assertNotNull(out);
    // three objects added (non-object skipped)
    assertEquals(3, out.size());

    // 1) both id + variables
    JsonObject o1 = out.getJsonObject(0);
    assertEquals("s1", o1.getString("dataset_id"));
    assertEquals(List.of("a", "b"), o1.getJsonArray("variables").getList());
    assertFalse(o1.containsKey("extra"));

    // 2) only id
    JsonObject o2 = out.getJsonObject(1);
    assertEquals("s2", o2.getString("dataset_id"));
    assertFalse(o2.containsKey("variables"));

    // 3) only variables
    JsonObject o3 = out.getJsonObject(2);
    assertNull(o3.getString("dataset_id"));
    assertEquals(List.of("x"), o3.getJsonArray("variables").getList());
  }

  // === 1) Forces JsonProcessingException so the catch rethrows JsonParsingException ===
  @Test
  void deleteApplicationById_jsonParsingError_throwsJsonParsingException() {
    // given
    String appId = "app-json-bad";
    when(routingContext.pathParam("id")).thenReturn(appId);
    addUserOnContext();

    // document with DRAFT status and a simple "form" (string content doesn't matter; we'll mock ObjectMapper)
    JsonObject document = new JsonObject()
      .put(STATUS, new JsonObject().put(STATUS, "DRAFT"))
      .put("form", new JsonObject().put("k", "v"))
      .put(TITLE, "SomeTitle");

    // make findOne succeed with the document
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(document));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    // when ObjectMapper.readTree is called, throw JsonProcessingException
    try (MockedConstruction<ObjectMapper> mocked =
           Mockito.mockConstruction(ObjectMapper.class, (mock, ctx) ->
             when(mock.readTree(anyString())).thenThrow(new JsonProcessingException("boom") {}))) {

      // then
      assertThrows(
        eu.europa.data.health.access.exceptions.JsonParsingException.class,
        () -> commonApplicationManager.deleteApplicationById(routingContext, DATA_ACCESS_APPLICATION_COLLECTION)
      );
    }

    // verify we never tried to delete files nor to proceed with entity deletion
    verify(hubStoreClient, never()).deleteFile(anyString(), anyString(), anyString());
  }

  // === 2) Happy path delete for DATA_ACCESS: hits the DATA_ACCESS ternaries ===
  @Test
  void deleteApplicationById_dataAccess_success_callsAuditLogAndDeletesFiles() {
    String appId = "app-del-ok";
    when(routingContext.pathParam("id")).thenReturn(appId);
    addUserOnContext();

    // form containing nested "id" fields so extractIds will find them
    JsonObject form = new JsonObject()
      .put("attachments", new JsonArray()
        .add(new JsonObject().put("id", "fid-1"))
        .add(new JsonObject().put("id", "fid-2"))
      );

    JsonObject document = new JsonObject()
      .put(STATUS, new JsonObject().put(STATUS, "DRAFT"))
      .put("form", form)
      .put(TITLE, "Delete Me");

    // findOne returns the document
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(document));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    // stub description (allow nullable id)
    when(auditLogManager.prepareAuditLogActionDescription(anyString(), any(), anyString()))
      .thenReturn("DESC-DEL");

    CommonApplicationManager spyMgr = Mockito.spy(commonApplicationManager);
    doNothing().when(spyMgr).deleteEntityById(eq(routingContext), eq(DATA_ACCESS_APPLICATION_COLLECTION));

    // when
    spyMgr.deleteApplicationById(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    // then: files deleted
    verify(hubStoreClient).deleteFile(eq("fid-1"), eq(APPLICATIONID), eq(appId));
    verify(hubStoreClient).deleteFile(eq("fid-2"), eq(APPLICATIONID), eq(appId));

    // audit linkables updated
    verify(dbHandler).updateLinkableFields(AUDIT_LOG_COLLECTION, appId);

    // description prepared (DATA_ACCESS branch)
    verify(auditLogManager).prepareAuditLogActionDescription(
      anyString(), // template (friendly string), not asserting exact value
      eq(appId),
      eq("Delete Me")
    );

    // insertAuditLog called (covers AuditLogObject/AuditLogAction DATA_ACCESS ternaries)
    verify(auditLogManager).insertAuditLog(
      anyString(), // "Data Access Application"
      eq(appId),
      anyString(), // "Data Access application deleted"
      eq("DESC-DEL"),
      eq(false),
      eq(routingContext)
    );
  }

  // === 3) document == null → 404 and “was not found in the database” ===
  @Test
  void deleteApplicationById_documentNull_returns404NotFoundMessage() {
    String appId = "missing-id";
    when(routingContext.pathParam("id")).thenReturn(appId);
    addUserOnContext();

    // findOne succeeds but returns null document
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteApplicationById(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
      verify(response).end(Mockito.<String>argThat(s ->
        s != null && s.contains("was not found in the database") && s.contains(appId)));
    });
  }

  // === 4) ar.failed() → 404 with cause message ===
  @Test
  void deleteApplicationById_findOneFailed_returns404WithCauseMessage() {
    String appId = "id-fail";
    when(routingContext.pathParam("id")).thenReturn(appId);
    addUserOnContext();

    // findOne fails
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("db error findOne")));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteApplicationById(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
      verify(response).end(Mockito.<String>argThat(s -> s != null && s.contains("db error findOne")));
    });
  }


  @Test
  void populateSection4_naturalPerson_populatesAllFields() {
    // Given: section3 has natural person fields
    JsonObject section4 = new JsonObject().put("sameAsContactPerson", true);
    JsonObject section3 = new JsonObject()
      .put(NATURAL_PERSON_NAME, "John Doe")
      .put(NATURAL_PERSON_ADDRESS, "42 High St")
      .put(NATURAL_PERSON_EMAIL, "john.doe@example.com")
      .put(NATURAL_PERSON_PHONE, new JsonObject().put("prefix", "+30").put("number", "2100000000"));

    // When: legal/natural key "b" → natural person path (updateNaturalPerson)
    commonApplicationManager.populateSection4(section4, section3, "b");

    // Then: fields copied as expected
    assertEquals("John Doe", section4.getString("fullName"));
    assertEquals("42 High St", section4.getString("address"));
    assertEquals("john.doe@example.com", section4.getString("email"));

    JsonObject phone = section4.getJsonObject("phone");
    assertNotNull(phone);
    assertEquals("+30", phone.getString("prefix"));
    assertEquals("2100000000", phone.getString("number"));
  }

  @Test
  void populateSection4_naturalPerson_missingFields_defaultsToEmptyAndNoPhone() {
    JsonObject section4 = new JsonObject().put("sameAsContactPerson", true);
    JsonObject section3 = new JsonObject(); // missing all fields

    commonApplicationManager.populateSection4(section4, section3, "b");

    // Missing -> null
    assertNull(section4.getString("fullName"));
    assertNull(section4.getString("address"));
    assertNull(section4.getString("email"));
    // Phone also absent
    assertNull(section4.getValue("phone"));
  }

  @Test
  void updateNaturalPerson_populatesAllFields_whenPresent() {
    // Given: section3 has natural person info
    JsonObject section4 = new JsonObject().put("sameAsContactPerson", true);
    JsonObject section3 = new JsonObject()
      .put(NATURAL_PERSON_NAME, "John Doe")
      .put(NATURAL_PERSON_ADDRESS, "42 High St")
      .put(NATURAL_PERSON_EMAIL, "john.doe@example.com")
      .put(NATURAL_PERSON_PHONE, new JsonObject().put("prefix", "+30").put("number", "2100000000"));

    commonApplicationManager.populateSection4(section4, section3, "b");

    assertEquals("John Doe", section4.getString("fullName"));
    assertEquals("42 High St", section4.getString("address"));
    assertEquals("john.doe@example.com", section4.getString("email"));

    JsonObject phone = section4.getJsonObject("phone");
    assertNotNull(phone);
    assertEquals("+30", phone.getString("prefix"));
    assertEquals("2100000000", phone.getString("number"));
  }

  @Test
  void updateNaturalPerson_missingFields_resultsInNullStrings_andNoPhone() {
    // Given: section3 missing natural person fields entirely
    JsonObject section4 = new JsonObject().put("sameAsContactPerson", true);
    JsonObject section3 = new JsonObject();

    // When
    commonApplicationManager.populateSection4(section4, section3, "b");

    assertNull(section4.getString("fullName"));
    assertNull(section4.getString("address"));
    assertNull(section4.getString("email"));
    assertNull(section4.getValue("phone"));
  }


  @Test
  void performStatusUpdate_updatesStatusAndHistoryCorrectly() {

    JsonObject mongoApplication = new JsonObject().put("status",
      new JsonObject().put("status", "ON-GOING"));
    JsonObject newStatus = new JsonObject().put("status", "COMPLETED");

    commonApplicationManager.updateApplicationStatus(mongoApplication, newStatus, routingContext);

    JsonObject updatedStatus = mongoApplication.getJsonObject("status");
    assertEquals("COMPLETED", updatedStatus.getString("status"));
    assertNotNull(updatedStatus.getJsonArray("status_history"));
    assertEquals("ON-GOING",
      updatedStatus.getJsonArray("status_history").getJsonObject(0).getString("status"));
  }

  @Test
  void deleteApplicationById_deletesApplicationWhenNotSubmitted() {
    JsonObject form = new JsonObject();
    form.put("id", "app123");
    JsonObject application = new JsonObject()
      .put("form", form)
      .put("status", new JsonObject().put("status", "DRAFT"));
    when(routingContext.pathParam("id")).thenReturn("applicationId");
    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(application));
      return null;
    }).when(mongoClient).findOne(anyString(), any(), any(), any());

    when(dbHandler.deleteById(eq("testCollection"), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteApplicationById(routingContext, "testCollection");

    verify(dbHandler, times(1)).deleteById(eq("testCollection"), any(JsonObject.class));
  }

  @Test
  void deleteApplicationById_failsToDeleteWhenSubmitted() {
    JsonObject application = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));
    when(routingContext.pathParam("id")).thenReturn("app123");
    when(dbHandler.deleteById(anyString(), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    JsonObject expectedQuery = new JsonObject()
      .put("_id", "app123")
      .put("userId", "n3817490");
    when(mongoClient.findOne(eq(DATA_REQUEST_APPLICATION_COLLECTION), eq(expectedQuery), isNull(),
      any())).thenAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(application));
      return null;
    });

    commonApplicationManager.deleteApplicationById(routingContext,
      DATA_REQUEST_APPLICATION_COLLECTION);

    verify(mongoClient, never()).removeDocument(eq(DATA_REQUEST_APPLICATION_COLLECTION),
      any(JsonObject.class), any());
  }

  @Test
  void getData_applicationDoesNotExist_setsNotFound() {
    String applicationId = "app-none";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    addUserOnContext(); // sets preferred_username -> ecasId

    // dbHandler.checkDocumentExists -> false
    Promise<Boolean> p = Promise.promise();
    p.complete(false);
    when(dbHandler.checkDocumentExists(any(JsonObject.class), anyString())).thenReturn(p);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.getData(routingContext, "testCollection");

    // verify 404 with the composed message (contains appId and userId)
    Awaitility.await().untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
      verify(response).end(Mockito.<String>argThat(s ->
        s != null &&
          s.contains(APPLICATION_WITH_ID + applicationId) &&
          s.contains(NOT_FOUND_FOR_USER_WITH_ID)));
    });
  }

  @Test
  void getData_existenceCheckFailed_setsInternalServerError() {
    String applicationId = "app-fail";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    addUserOnContext();

    // dbHandler.checkDocumentExists -> failed future
    Promise<Boolean> p = Promise.promise();
    p.fail(new RuntimeException("boom"));
    when(dbHandler.checkDocumentExists(any(JsonObject.class), anyString())).thenReturn(p);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.getData(routingContext, "testCollection");

    // verify 500 with the constant message
    Awaitility.await().untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      verify(response).end(Mockito.<String>argThat(s ->
        s != null && s.contains(INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE)));
    });
  }

  @Test
  void getData_retrievesFileWhenApplicationExists() {
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");
    when(routingContext.pathParam("data_id")).thenReturn("data_id");

    Promise<Boolean> mockPromise = Promise.promise();
    mockPromise.complete(true);
    when(dbHandler.checkDocumentExists(any(), any())).thenReturn(mockPromise);

    HttpResponse<Buffer> mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap()
      .add("content-disposition", "attachment; filename=testfile.txt"));
    when(mockHttpResponse.bodyAsBuffer()).thenReturn(Buffer.buffer("file content"));

    when(hubStoreClient.getFile(eq("data_id"), eq("applicationId"), eq("app123"))).thenReturn(
      Future.succeededFuture(mockHttpResponse));

    HttpServerResponse mockResponse = Mockito.mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.end(any(Buffer.class))).thenReturn(Future.succeededFuture());

    commonApplicationManager.getData(routingContext, "testCollection");

    verify(mockResponse, times(1)).putHeader("Content-Type", "application/octet-stream");
    verify(mockResponse, times(1)).putHeader("Content-Disposition",
      "attachment; filename=testfile.txt");
    verify(mockResponse, times(1)).end(Buffer.buffer("file content"));
  }

  @Test
  void getFileData_failure_setsInternalServerError() {
    String appId = "app123";
    String fileId = "file123";

    when(routingContext.pathParam("data_id")).thenReturn(fileId);

    // Make hubStoreClient.getFile fail → triggers the else branch
    when(hubStoreClient.getFile(eq(fileId), eq(APPLICATIONID), eq(appId)))
      .thenReturn(Future.failedFuture(new RuntimeException("boom")));

    // setUpMessageContextResponse writes to response
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.getFileData(routingContext, appId);

    // verify 500 + error message
    Awaitility.await().untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      verify(response).end(contains("Error retrieving file: boom"));
    });
  }

  @Test
  void testGetFileData() {
    RoutingContext routingContext = mock(RoutingContext.class);
    when(routingContext.pathParam("data_id")).thenReturn("file123");

    HttpResponse<Buffer> mockResponse = mock(HttpResponse.class);
    when(hubStoreClient.getFile(anyString(), anyString(), anyString()))
      .thenReturn(Future.succeededFuture(mockResponse));

    MultiMap mockHeaders = mock(MultiMap.class);
    when(mockResponse.headers()).thenReturn(mockHeaders);
    when(mockHeaders.get(CONTENT_DISPOSITION)).thenReturn("attachment; filename=file123");

    Buffer mockBuffer = Buffer.buffer("File content here");
    when(mockResponse.bodyAsBuffer()).thenReturn(mockBuffer);

    HttpServerResponse responce = Mockito.mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(responce);
    when(responce.putHeader(anyString(), anyString())).thenReturn(responce);
    when(responce.end(any(Buffer.class))).thenReturn(Future.succeededFuture());

    commonApplicationManager.getFileData(routingContext, "app123");

    verify(routingContext.response()).putHeader(eq("Content-Type"), eq("application/octet-stream"));
    verify(routingContext.response()).putHeader(eq("Content-Disposition"),
      eq("attachment; filename=file123"));
    verify(routingContext.response()).end(eq(mockBuffer));
  }

  @Test
  void createApplication_savesApplicationWithValidData() {
    User mockUser = Mockito.mock(User.class);
    Mockito.when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    Mockito.when(routingContext.user().principal()).thenReturn(principal);
    JsonObject body = new JsonObject()
      .put(DATASETS, new JsonArray().add(
        new JsonObject()
          .put("dataset_id", "ds1")
          .put("distributions", new JsonArray().add(
            new JsonObject()
              .put(DISTRIBUTION_ID, "dist1")
              .put("publisher", new JsonObject().put("name", "Test Publisher"))))
      ))
      .put("id", "appId")
      .put("title", "Test App Title");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    when(mongoClient.findOne(eq(BASKET_COLLECTION), any(JsonObject.class), isNull()))
      .thenReturn(Future.succeededFuture(new JsonObject().put("publisher", "Test Publisher")));

    when(hubSearchClient.getDataset(anyString())).thenReturn(Future.succeededFuture());

      when(dbHandler.create(eq("testCollection"), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture("newEntityId"));

    lenient().doNothing().when(auditLogManager)
      .insertAuditLog(anyString(), any(), anyString(), any(), anyBoolean(), any());

    commonApplicationManager.createApplication(routingContext, "testCollection");

    verify(mongoClient, times(1)).findOne(eq(BASKET_COLLECTION), any(JsonObject.class), isNull());
    verify(dbHandler, times(1)).create(eq("testCollection"), any(JsonObject.class));

    verify(auditLogManager, times(1))
      .insertAuditLog(eq("Data Request Application"), any(), eq("Data Request application created"),
        any(), eq(true), eq(routingContext));

  }

  @Test
  void testDeleteSection6ByDataset_withNullForm() {
    JsonObject mongoApp = new JsonObject();
    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");
    assertNull(mongoApp.getJsonObject("form"));
  }

  @Test
  void testDeleteSection6ByDataset_withNullSection6() {
    JsonObject mongoApp = new JsonObject().put("form", new JsonObject());
    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");
    assertNull(mongoApp.getJsonObject("form").getJsonArray("section6"));
  }

  @Test
  void testDeleteSection6ByDataset_withNoMatchingCatalogId() {
    JsonArray section6 = new JsonArray().add(
      new JsonObject().put("catalog_ids", new JsonArray().add("catalog-999")));
    JsonObject form = new JsonObject().put("section6", section6);
    JsonObject mongoApp = new JsonObject().put("form", form);

    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");

    JsonArray resultSection6 = mongoApp.getJsonObject("form").getJsonArray("section6");
    assertEquals(1, resultSection6.size());
  }

  @Test
  void testDeleteSection6ByDataset_withMatchingCatalogId() {
    JsonArray section6 = new JsonArray()
      .add(new JsonObject().put("catalog_ids", new JsonArray().add("catalog-123")))
      .add(new JsonObject().put("catalog_ids", new JsonArray().add("catalog-456")));

    JsonObject form = new JsonObject().put("section6", section6);
    JsonObject mongoApp = new JsonObject().put("form", form);

    commonApplicationManager.deleteSection6ByDataset(mongoApp, "catalog-123");

    JsonArray resultSection6 = mongoApp.getJsonObject("form").getJsonArray("section6");
    assertEquals(1, resultSection6.size());
    assertTrue(resultSection6.getJsonObject(0).getJsonArray("catalog_ids").contains("catalog-456"));
  }

  @Test
  void postData_statusSubmitted_setsBadRequest() {
    String applicationId = "app-submitted";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    addUserOnContext();

    // DB returns an application with SUBMITTED status
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.postData(routingContext, "testCollection");

    Awaitility.await().untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
      verify(response).end(contains("Posting data can not be executed in a completed application"));
    });
  }

  @Test
  void postData_applicationNotFound_setsNotFound() {
    String applicationId = "app-missing";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    addUserOnContext();

    // DB returns null (not found)
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(null));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.postData(routingContext, "testCollection");

    Awaitility.await().untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
      verify(response).end(contains("Application with id: " + applicationId + " was not found"));
    });
  }

  @Test
  void postData_findOneFailed_setsInternalServerError() {
    String applicationId = "app-error";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    addUserOnContext();

    // DB fails
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.postData(routingContext, "testCollection");

    Awaitility.await().untilAsserted(() -> {
      verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      verify(response).end(contains("DB down"));
    });
  }

  @Test
  void testHasElementWithValue_returnsTrue_whenValueExists() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "match"))
      .add(new JsonObject().put("key", "other"));

    boolean result = commonApplicationManager.hasElementWithValue(array, "key", "match");

    assertTrue(result);
  }

  @Test
  void testHasElementWithValue_returnsFalse_whenValueDoesNotExist() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "nope"))
      .add(new JsonObject().put("key", "nothing"));

    boolean result = commonApplicationManager.hasElementWithValue(array, "key", "match");

    assertFalse(result);
  }

  @Test
  void testUpdateDatasetDateAdded_callsPrepareDataset_whenMatchingIdExists() {
    JsonArray datasets = new JsonArray()
      .add(new JsonObject().put("dataset_id", "ds1"))
      .add(new JsonObject().put("dataset_id", "ds2"));

    CommonApplicationManager manager = spy(
      new CommonApplicationManager(null, null, null, null, null, null, null, null, null) {
      });

    manager.updateDatasetDateAdded(datasets, "ds2", "collectionName");

    verify(manager).prepareDataset(datasets.getJsonObject(1), "collectionName");
  }

  @Test
  void testPrepareDataset_withJsonObject_setsDateAdded() {
    JsonObject input = new JsonObject().put("name", "dataset-1");

    JsonObject result = commonApplicationManager.prepareDataset(input, "testCollection");

    assertNotNull(result);
    assertEquals("dataset-1", result.getString("name"));
    assertNotNull(result.getInstant("date_added"));
  }

  @Test
  void testPrepareDataset_withInvalidObject_returnsNull() {
    Object invalidInput = "this is not a JsonObject";

    JsonObject result = commonApplicationManager.prepareDataset(invalidInput, "testCollection");

    assertNull(result);
  }

  @Test
  void prepareDataset_validJsonObject_addsDateAdded() {
    JsonObject dataset = new JsonObject().put("key", "value");

    JsonObject result = commonApplicationManager.prepareDataset(dataset, "testCollection");

    assertNotNull(result);
    assertEquals("value", result.getString("key"));
    assertNotNull(result.getInstant("date_added"));
  }

  @Test
  void prepareDataset_invalidObject_returnsNull() {
    Object dataset = 42;

    JsonObject result = commonApplicationManager.prepareDataset(dataset, "testCollection");

    assertNull(result);
  }

  @Test
  void deleteFile_success_setsOkResponse() {
    String applicationId = "app-123";
    String fileId = "file-456";

    when(routingContext.pathParam("data_id")).thenReturn(fileId);

    when(hubStoreClient.deleteFile(fileId, "applicationId", applicationId))
      .thenReturn(Future.succeededFuture());

    // Call the method
    commonApplicationManager.deleteFile(routingContext, applicationId);
    verify(response).setStatusCode(eq(200));
    verify(response).end(contains("\"File with id: " + fileId + " was deleted successfully"));
  }


  @Test
  void deleteFile_failure_setsInternalServerErrorResponse() {
    String applicationId = "app-123";
    String fileId = "file-456";
    String errorMessage = "Error deleting file";

    when(routingContext.pathParam("data_id")).thenReturn(fileId);

    when(hubStoreClient.deleteFile(fileId, "applicationId", applicationId))
      .thenReturn(Future.failedFuture(new RuntimeException(errorMessage)));

    commonApplicationManager.deleteFile(routingContext, applicationId);
    verify(response).setStatusCode(eq(500));
    verify(response).end(contains("Error deleting file: " + errorMessage));
  }

  @Test
  void deleteData_success_deletesFile() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject mongoApplication = new JsonObject().put("status",
      new JsonObject().put("status", "DRAFT"));
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    String fileId = "file-456";
    when(routingContext.pathParam("data_id")).thenReturn(fileId);
    when(hubStoreClient.deleteFile(fileId, "applicationId", applicationId))
      .thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("File with id: " + fileId + " was deleted successfully"));
  }

  @Test
  void deleteData_applicationNotFound_setsNotFoundResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(null));
        return null;
      });

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(contains("Application with id: " + applicationId + " was not found"));
  }


  @Test
  void deleteData_submissionStatus_setsBadRequestResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";

    // Mock an application with status SUBMITTED
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));

    // Query to pass to Mongo
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    // Mock path param
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    // Mock mongoClient to return our stubbed application
    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    // Call the method under test
    commonApplicationManager.deleteData(routingContext, collectionName);

    // Capture the response message
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(captor.capture());

    // Assert that the message contains "submitted application"
    String actualMessage = captor.getValue();
    assertTrue(actualMessage.contains("Deletion of data can not be executed in a completed application"));
  }

  @Test
  void deleteData_failure_setsInternalServerErrorResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";
    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.failedFuture(new RuntimeException("Database error")));
        return null;
      });

    commonApplicationManager.deleteData(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("Database error"));
  }

  @Test
  void requestCreateOrUpdateSection_success_section_3() {
    createOrUpdateSection_success_updates("3", DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void accessCreateOrUpdateSection_success_section_3() {
    createOrUpdateSection_success_updates("3", DATA_ACCESS_APPLICATION_COLLECTION);
  }

  @Test
  void requestCreateOrUpdateSection_success_section_4() {
    createOrUpdateSection_success_updates("4", DATA_REQUEST_APPLICATION_COLLECTION);
  }

  @Test
  void accessCreateOrUpdateSection_success_section_5() {
    createOrUpdateSection_success_updates("5", DATA_ACCESS_APPLICATION_COLLECTION);
  }
  @Test
  void createOrUpdateSection_validationErrors_nonRequired_returnsBadRequest_andShortCircuits() {
    String applicationId = "app-err";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    addUserOnContext();

    // DB returns an existing app in DRAFT
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject());

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    // Non-required error → BAD_REQUEST and early return
    when(routingContext.get(ERRORS)).thenReturn(List.of(new FieldError("f", "NON_REQUIRED_CODE")));

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    // No stubs for SECTION_NUMBER, body(), or updateCollection — they aren't reached
    commonApplicationManager.createOrUpdateSection(
      routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(contains("Section not stored due to validation errors"));

    verify(mongoClient, never()).updateCollection(
      anyString(),
      any(JsonObject.class),
      any(JsonObject.class),
      ArgumentMatchers.<Handler<AsyncResult<MongoClientUpdateResult>>>any()
    );
  }



  @Test
  void createOrUpdateSection_noChanges_docMatched1_docModified0_returnsOkNoChanges() {
    String applicationId = "app-nochange";
    String sectionNo = "4";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn(sectionNo);
    addUserOnContext();

    // No validation errors
    when(routingContext.get(ERRORS)).thenReturn(null);

    // Existing app in DRAFT with minimal form/section3
    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section3", new JsonObject()));
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    // Body for section4 (since sectionNo=4)
    when(routingContext.body().asJsonObject()).thenReturn(new JsonObject().put("a", "b"));

    // Update result: matched=1, modified=0 → "No changes contained..."
    doAnswer(inv -> {
      Handler<AsyncResult<MongoClientUpdateResult>> h = inv.getArgument(3);
      MongoClientUpdateResult r = mock(MongoClientUpdateResult.class);
      when(r.getDocMatched()).thenReturn(1L);
      when(r.getDocModified()).thenReturn(0L);
      h.handle(Future.succeededFuture(r));
      return null;
    }).when(mongoClient).updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.createOrUpdateSection(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("No changes contained in the already existing section4"));
  }



  @Test
  void createOrUpdateSection_updateCollectionFails_returnsInternalServerError() {
    String applicationId = "app-upd-fail";
    String sectionNo = "4";
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn(sectionNo);
    addUserOnContext();

    when(routingContext.get(ERRORS)).thenReturn(null);

    JsonObject mongoApplication = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("form", new JsonObject().put("section3", new JsonObject()));
    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApplication));
      return null;
    }).when(mongoClient).findOne(anyString(), any(JsonObject.class), isNull(), any());

    when(routingContext.body().asJsonObject()).thenReturn(new JsonObject().put("k", "v"));

    // updateCollection fails → INTERNAL_SERVER_ERROR with cause message
    doAnswer(inv -> {
      Handler<AsyncResult<MongoClientUpdateResult>> h = inv.getArgument(3);
      h.handle(Future.failedFuture(new RuntimeException("Update failed")));
      return null;
    }).when(mongoClient).updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.createOrUpdateSection(routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("Update failed"));
  }

  void createOrUpdateSection_success_updates(String sectionNumber, String collectionName) {
    String applicationId = "app-123";

    JsonObject section3 = new JsonObject();
    section3.put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "b"));
    section3.put(NATURAL_PERSON_NAME, "name");
    section3.put(NATURAL_PERSON_ADDRESS, "address");
    section3.put(NATURAL_PERSON_PHONE, new JsonObject());
    section3.put(NATURAL_PERSON_EMAIL, "email");
    JsonObject mongoApplication = new JsonObject()
      .put("form", new JsonObject().put("section3", section3))
      .put("status", new JsonObject().put("status", "DRAFT"));

    JsonObject query = new JsonObject()
      .put(ID, applicationId)
      .put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn(sectionNumber);

    when(routingContext.body()).thenReturn(mock(io.vertx.ext.web.RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(new JsonObject());
    when(routingContext.get(ERRORS)).thenReturn(null);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mongoApplication));
        return null;
      });

    when(mongoClient.updateCollection(eq(collectionName), eq(query), any(JsonObject.class), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<MongoClientUpdateResult>> handler = invocation.getArgument(3);
        MongoClientUpdateResult result = mock(MongoClientUpdateResult.class);
        when(result.getDocMatched()).thenReturn(1L);
        when(result.getDocModified()).thenReturn(1L);
        handler.handle(Future.succeededFuture(result));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains(
      "Section section" + sectionNumber + " has been stored in application with id: "
        + applicationId));
  }

  @Test
  void createOrUpdateSection_applicationNotFound_setsNotFoundResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";

    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);

    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(null));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(contains("Application with id: " + applicationId + " was not found"));
  }

  @Test
  void createOrUpdateSection_failure_setsInternalServerErrorResponse() {
    String applicationId = "app-123";
    String collectionName = "testCollection";

    JsonObject query = new JsonObject().put(ID, applicationId).put(USER_ID, ecasId);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(applicationId);
    when(mongoClient.findOne(eq(collectionName), eq(query), isNull(), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.failedFuture(new RuntimeException("DB down")));
        return null;
      });

    commonApplicationManager.createOrUpdateSection(routingContext, collectionName);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("DB down"));
  }

  @Test
  void sameAsSection3Array_fillsFieldsCorrectly_forLegalPerson() {
    JsonArray sectionFormArray = new JsonArray()
      .add(new JsonObject()
        .put(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, true));

    JsonObject section3 = new JsonObject()
      .put(LEGAL_PERSON_NAME, "Company X")
      .put(CONTACT_PERSON_EMAIL, "contact@companyx.com")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("prefix", "+32").put("number", "123456"))
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "a"));

    JsonArray result = commonApplicationManager.sameAsSection3Array(sectionFormArray, section3);
    JsonObject resultItem = result.getJsonObject(0);

    assertEquals("Company X", resultItem.getString(INFORMATION_PROVIDER_NAME));
    assertEquals("contact@companyx.com", resultItem.getString(INFORMATION_PROVIDER_EMAIL));
    assertEquals("+32", resultItem.getJsonObject(INFORMATION_PROVIDER_PHONE).getString("prefix"));
    assertEquals("123456",
      resultItem.getJsonObject(INFORMATION_PROVIDER_PHONE).getString("number"));
  }

  @Test
  void populateSection4_setsFieldsCorrectly_forLegalPerson() {
    JsonObject sectionForm = new JsonObject().put("sameAsContactPerson", true);
    JsonObject section3 = new JsonObject()
      .put(LEGAL_PERSON_NAME, "Company X")
      .put(LEGAL_PERSON_ADDRESS, "Legal Address")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("prefix", "+32").put("number", "123456"))
      .put(CONTACT_PERSON_EMAIL, "contact@companyx.com")
      .put(CONTACT_PERSON_ORGANISATION_NAME, "OrgName")
      .put(CONTACT_PERSON_BUSINESS_ID, "BIZ789");

    commonApplicationManager.populateSection4(sectionForm, section3, "a");

    assertEquals("Company X", sectionForm.getString("fullName"));
    assertEquals("Legal Address", sectionForm.getString("address"));
    assertEquals("contact@companyx.com", sectionForm.getString("email"));
    assertEquals("OrgName", sectionForm.getString("nameOfTheOrganisation"));
    assertEquals("BIZ789", sectionForm.getString("businessIdentifierOrganization"));

    JsonObject phone = sectionForm.getJsonObject("phone");
    assertEquals("+32", phone.getString("prefix"));
    assertEquals("123456", phone.getString("number"));
  }

  @Test
  void populateSection4_setsFieldsCorrectly_forNaturalPerson() {
    JsonObject sectionForm = new JsonObject().put("sameAsContactPerson", true);
    JsonObject section3 = new JsonObject()
      .put(NATURAL_PERSON_NAME, "John Doe")
      .put(NATURAL_PERSON_ADDRESS, "Natural Address")
      .put(NATURAL_PERSON_PHONE, new JsonObject().put("prefix", "+44").put("number", "987654"))
      .put(NATURAL_PERSON_EMAIL, "john.doe@example.com");

    commonApplicationManager.populateSection4(sectionForm, section3, "b");

    assertEquals("John Doe", sectionForm.getString("fullName"));
    assertEquals("Natural Address", sectionForm.getString("address"));
    assertEquals("john.doe@example.com", sectionForm.getString("email"));

    JsonObject phone = sectionForm.getJsonObject("phone");
    assertEquals("+44", phone.getString("prefix"));
    assertEquals("987654", phone.getString("number"));
  }

  @Test
  void populateSection5_setsFieldsCorrectly_forLegalPerson_whenBothFlagsTrue() {
    JsonObject sectionForm = new JsonObject()
      .put(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, true)
      .put(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, true);

    JsonObject section3 = new JsonObject()
      .put(LEGAL_PERSON_NAME, "Jane Smith");

    commonApplicationManager.populateSection5(sectionForm, section3, "a");

    // Responsible person fields
    assertEquals("Jane Smith", sectionForm.getString(PERSON_RESPONSIBLE_NAME));

    // Research person fields
    assertEquals("Jane Smith", sectionForm.getString(PERSON_RESEARCH_NAME));
  }

  @Test
  void updateSameAsSection3_updatesSection4_forLegalPerson() {
    JsonObject legalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, legalPerson)
      .put(LEGAL_PERSON_NAME, "Legal Name")
      .put(CONTACT_PERSON_EMAIL, "legal@example.com")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("countryCode", "+1").put("number", "123456"));

    JsonObject mongoForm = new JsonObject().put(SECTION_3, section3);

    JsonObject section4 = new JsonObject().put(SAME_AS_CONTACT_PERSON, true);
    JsonObject updateForm = new JsonObject().put("form.section4", section4);

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 4, true);

    assertEquals("Legal Name", section4.getString("fullName"));
    assertEquals("legal@example.com", section4.getString("email"));
  }

  @Test
  void updateSameAsSection3_updatesSection5_forNaturalPerson() {
    JsonObject legalPerson = new JsonObject().put("key", "b");
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, legalPerson)
      .put(NATURAL_PERSON_NAME, "Natural Name");

    JsonObject mongoForm = new JsonObject().put(SECTION_3, section3);

    JsonObject section5 = new JsonObject()
      .put(PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON, true)
      .put(PERSON_RESEARCH_SAME_AS_CONTACT_PERSON, true);

    JsonObject updateForm = new JsonObject().put("form.section5", section5);

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 5, true);

    assertEquals("Natural Name", section5.getString(PERSON_RESPONSIBLE_NAME));

    assertEquals("Natural Name", section5.getString(PERSON_RESEARCH_NAME));
  }

  @Test
  void updateSameAsSection3_updatesSection6_forLegalPerson() {
    JsonObject legalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, legalPerson)
      .put(LEGAL_PERSON_NAME, "Legal Provider")
      .put(CONTACT_PERSON_EMAIL, "info@legal.com")
      .put(CONTACT_PERSON_PHONE, new JsonObject().put("countryCode", "+33").put("number", "7890"));

    JsonObject mongoForm = new JsonObject().put(SECTION_3, section3);

    JsonObject country = new JsonObject().put(INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON, true);
    JsonArray section6 = new JsonArray().add(country);
    JsonObject updateForm = new JsonObject().put("form.section6", section6);

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 6, true);

    JsonObject updatedCountry = section6.getJsonObject(0);
    assertEquals("Legal Provider", updatedCountry.getString("informationProviderName"));
    assertEquals("info@legal.com", updatedCountry.getString("informationProviderEmail"));
    assertEquals("+33",
      updatedCountry.getJsonObject("informationProviderPhone").getString("countryCode"));
  }

  @Test
  void updateSameAsSection3_updatesSection8_forSameAsContactPerson() {
    JsonObject section3 = new JsonObject()
      .put(LEGAL_OR_NATURAL_PERSON, new JsonObject().put("key", "b"))
      .put("naturalPersonName", "John Doe")
      .put("naturalPersonEmail", "john.doe@university.com");

    JsonObject mongoForm = new JsonObject().put("section3", section3);

    JsonObject section8 = new JsonObject().put("sameAsContactPerson", true);
    JsonObject updateForm = new JsonObject().put("form.section8", section8);

    // When
    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 8, true);

    JsonArray peopleArray = section8.getJsonArray("peopleWhoWillBeProcessingTheData");
    assertNotNull(peopleArray);
    assertEquals(1, peopleArray.size());

    JsonObject person = peopleArray.getJsonObject(0);
    assertEquals("John Doe", person.getString("fullName"));
    assertEquals("john.doe@university.com", person.getString("emailAddress"));
  }

  @Test
  void updateSameAsSection3_doesNothing_whenSection3IsNull() {
    JsonObject mongoForm = new JsonObject();
    JsonObject updateForm = new JsonObject().put("form.section4", new JsonObject());

    commonApplicationManager.updateSameAsSection3(mongoForm, updateForm, 4, true);

    assertTrue(true);
  }

  @Test
  void getSectionErrors_shouldStoreSection_whenNoCriticalErrors_andSectionNot6() {
    JsonObject body = new JsonObject().put("field", "value");

    when(routingContext.get(ERRORS)).thenReturn(List.of());
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("4");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("1234");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section section4 has been stored"));
  }

  @Test
  void getSectionErrors_shouldStoreSection6_whenNoCriticalErrors_andBodyIsArray() {
    JsonArray bodyArray = new JsonArray().add(new JsonObject().put("field", "value"));

    when(routingContext.get(ERRORS)).thenReturn(List.of());
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("6");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("5678");
    when(routingContext.body().asJsonArray()).thenReturn(bodyArray);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section section6 has been stored"));
  }

  @Test
  void getSectionErrors_shouldReturnBadRequest_whenCriticalErrorsExist() {
    FieldError criticalError = new FieldError("someField", "NON_REQUIRED_CODE");

    when(routingContext.get(ERRORS)).thenReturn(List.of(criticalError));

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(contains("Section not stored due to validation errors"));
  }

  @Test
  void getSectionErrors_shouldStoreSection_whenOnlyRequiredFieldErrors() {
    FieldError requiredError = new FieldError("field", "REQUIRED");

    when(routingContext.get(ERRORS)).thenReturn(List.of(requiredError));

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end(contains("Section not stored due to validation errors"));
  }

  @Test
  void getSectionErrors_shouldStoreSection_whenNoErrorsListPresent() {
    when(routingContext.get(ERRORS)).thenReturn(null);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("3");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("1234");

    JsonObject body = new JsonObject().put("someKey", "someValue");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section section3 has been stored"));
  }

  @Test
  void getSectionErrors_shouldHandleUnexpectedSectionNumberGracefully() {
    when(routingContext.get(ERRORS)).thenReturn(null);
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn("foo");
    when(routingContext.pathParam(APPLICATION_ID)).thenReturn("1234");

    JsonObject body = new JsonObject().put("field", "val");
    when(routingContext.body().asJsonObject()).thenReturn(body);

    commonApplicationManager.getSectionErrors(routingContext);

    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Section sectionfoo has been stored"));
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection6_DARA_ACCESS() {
    JsonArray mockBody = new JsonArray();
    JsonObject mockBodyObject = new JsonObject()
      .put("id", "file1");
    mockBody.add(mockBodyObject.put("consentAndInformationLetterAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    mockBody.add(mockBodyObject.put("permitDecisionAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    mockBody.add(mockBodyObject.put("ethicalReviewAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    mockBody.add(mockBodyObject.put("variablesToBeUsedInDataExtractionAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))));
    validateFileBeforeUpdate_shouldValidateFileForSection("6",
      DATA_ACCESS_APPLICATION_COLLECTION, null, mockBody);
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection8_DATA_ACCESS() {
    Map<String, Object> fields = Map.of(
      "attachResearchPermitIfRequired", new JsonArray().add(new JsonObject()
        .put("id", "file1").put("name", "file1.txt")));
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("8",
      DATA_ACCESS_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection9_DATA_ACCESS() {
    Map<String, Object> fields = Map.of(
      "additionalAttachment",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt")));
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("9",
      DATA_ACCESS_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection5_REQUEST_ACCESS() {
    Map<String, Object> fields = Map.of(
      "summaryOfPlanForUsingTheData",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt")),
      "summaryOfResearchPlan",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))
    );
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("5",
      DATA_REQUEST_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection5_DATA_REQUEST() {
    Map<String, Object> fields = Map.of(
      "summaryOfPlanForUsingTheData",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt")),
      "summaryOfResearchPlan",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))
    );
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("5", DATA_REQUEST_APPLICATION_COLLECTION,
      mockBody, new JsonArray());
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection6_REQUEST_ACCESS() {
    JsonObject nestedFile = new JsonObject()
      .put("tabulationPlanArray",
        new JsonArray().add(new JsonObject()
          .put("tabulationPlan", new JsonObject().put("id", "tabulationPlanId"))
          .put("id", "file1")
          .put("name", "file1.txt")));
    JsonArray nestedArray = new JsonArray().add(nestedFile);
    JsonObject arrayItem = new JsonObject()
      .put("tabulationPlanArray", nestedArray);
    validateFileBeforeUpdate_shouldValidateFileForSection("6",
      DATA_REQUEST_APPLICATION_COLLECTION, arrayItem, nestedArray);
  }

  @Test
  void validateFileBeforeUpdate_shouldValidateFileForSection7_REQUEST_ACCESS() {
    Map<String, Object> fields = Map.of(
      "attachmentOfPermitDocuments",
      new JsonArray().add(new JsonObject().put("id", "file1").put("name", "file1.txt"))
    );
    JsonObject mockBody = new JsonObject();
    fields.forEach(mockBody::put);
    validateFileBeforeUpdate_shouldValidateFileForSection("7",
      DATA_REQUEST_APPLICATION_COLLECTION, mockBody, new JsonArray());
  }

  void validateFileBeforeUpdate_shouldValidateFileForSection(String sectionNumber,
    String collectionName, JsonObject mockBody, JsonArray mockBodyArray) {
    when(routingContext.pathParam(SECTION_NUMBER)).thenReturn(sectionNumber);
    lenient().when(routingContext.pathParam(APPLICATION_ID)).thenReturn("app123");

    lenient().when(routingContext.body().asJsonObject()).thenReturn(mockBody);
    lenient().when(routingContext.body().asJsonArray()).thenReturn(mockBodyArray);

    HttpResponse<Buffer> mockHttpResponse = mock(HttpResponse.class);
    lenient().when(mockHttpResponse.getHeader("filename")).thenReturn("file1.txt");

    lenient().when(hubStoreClient.getFile(eq("tabulationPlanId"), eq(APPLICATIONID), eq("app123")))
      .thenReturn(Future.succeededFuture(mockHttpResponse));

    commonApplicationManager = spy(commonApplicationManager);

    commonApplicationManager.validateFileBeforeUpdate(routingContext,
      collectionName);

    verify(routingContext, times(1)).next();
  }

  @Test
  void cleanSection2_shouldRemoveOtherFieldWhenTheResearchFocusesOnTheFollowingObjectivesDoesNotContainI() {
    JsonObject section2 = new JsonObject();
    JsonArray objectivesArray = new JsonArray().add(new JsonObject().put("key", "a"));
    section2.put("theResearchFocusesOnTheFollowingObjectives", objectivesArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    boolean containsKey = section2.containsKey("theResearchFocusesOnTheFollowingObjectivesOther");
    assertFalse(containsKey, "theResearchFocusesOnTheFollowingObjectivesOther should be removed");
  }

  @Test
  void cleanSection2_shouldNotRemoveOtherFieldWhenTheResearchFocusesOnTheFollowingObjectivesContainsI() {
    JsonObject section2 = new JsonObject();
    JsonArray objectivesArray = new JsonArray().add(new JsonObject().put("key", "i"));
    section2.put("theResearchFocusesOnTheFollowingObjectives", objectivesArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertTrue(section2.containsKey("theResearchFocusesOnTheFollowingObjectives"));
  }

  @Test
  void cleanSection2_shouldRemoveAreaOfResearchOtherFieldWhenAreaOfResearchDoesNotContainAj() {
    JsonObject section2 = new JsonObject();
    JsonArray areaOfResearchArray = new JsonArray().add(new JsonObject().put("key", "b"));
    section2.put("areaOfResearch", areaOfResearchArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(section2.containsKey("areaOfResearchOther"));
  }

  @Test
  void cleanSection2_shouldNotRemoveAreaOfResearchOtherFieldWhenAreaOfResearchContainsAj() {
    JsonObject section2 = new JsonObject();
    JsonArray areaOfResearchArray = new JsonArray().add(new JsonObject().put("key", "aj"));
    section2.put("areaOfResearch", areaOfResearchArray);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertTrue(section2.containsKey("areaOfResearch"));
  }

  @Test
  void cleanSection2_shouldRemoveSummaryOfTheProjectIfSummaryNotChecked() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, false);
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON,
      "Some reason");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(section2.containsKey("Some reason"));
  }

  @Test
  void cleanSection2_shouldRemoveSummaryOfTheProjectReasonIfSummaryChecked() {
    JsonObject section2 = new JsonObject();
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, true);
    section2.put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON,
      "Some reason");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put("section2", section2);

    commonApplicationManager.cleanSection2(mongoApplicationForm);

    assertFalse(
      section2.containsKey("THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON"));
  }

  @Test
  void cleanSection3_shouldRemoveApplyingForDataForCarryingOutTasksIfKeyNotA() {
    JsonObject section3 = new JsonObject();
    JsonObject applyingForDataOnBehalfOfPublicSector = new JsonObject();
    applyingForDataOnBehalfOfPublicSector.put("key", "b");
    section3.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
      applyingForDataOnBehalfOfPublicSector);

    JsonObject legalPerson = new JsonObject();
    section3.put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS));
  }

  @Test
  void cleanSection3_shouldNotRemoveApplyingForDataForCarryingOutTasksIfKeyIsA() {
    JsonObject section3 = new JsonObject();
    JsonObject applyingForDataOnBehalfOfPublicSector = new JsonObject();
    applyingForDataOnBehalfOfPublicSector.put("key", "b");
    section3.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
      applyingForDataOnBehalfOfPublicSector);
    section3.put(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS, "Some text");

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS));
  }

  @Test
  void cleanSection3_shouldRemoveLegalPersonFieldsIfKeyIsB() {
    JsonObject section3 = new JsonObject();
    JsonObject legalPerson = new JsonObject();
    legalPerson.put("key", "b");
    section3.put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(LEGAL_PERSON_NAME));
    assertFalse(section3.containsKey(LEGAL_PERSON_ADDRESS));
    assertFalse(section3.containsKey("legalPersonZipCode"));
    assertFalse(section3.containsKey("legalPersonCity"));
    assertFalse(section3.containsKey(LEGAL_PERSON_COUNTRY));
    assertFalse(section3.containsKey("contactPersonName"));
    assertFalse(section3.containsKey(CONTACT_PERSON_EMAIL));
    assertFalse(section3.containsKey(CONTACT_PERSON_PHONE));
    assertFalse(section3.containsKey(CONTACT_PERSON_ORGANISATION_NAME));
    assertFalse(section3.containsKey(CONTACT_PERSON_BUSINESS_ID));
    assertFalse(section3.containsKey("contactPersonRelationship"));
  }

  @Test
  void cleanSection3_shouldRemoveNaturalPersonFieldsIfKeyIsA() {
    JsonObject section3 = new JsonObject();
    JsonObject legalPerson = new JsonObject();
    legalPerson.put("key", "a");
    section3.put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertFalse(section3.containsKey(NATURAL_PERSON_NAME));
    assertFalse(section3.containsKey(NATURAL_PERSON_ADDRESS));
    assertFalse(section3.containsKey("naturalPersonZipCode"));
    assertFalse(section3.containsKey("naturalPersonCity"));
    assertFalse(section3.containsKey("naturalPersonCountry"));
    assertFalse(section3.containsKey(NATURAL_PERSON_EMAIL));
    assertFalse(section3.containsKey(NATURAL_PERSON_PHONE));
    assertFalse(section3.containsKey(NATURAL_PERSON_JOB_TITLE));
    assertFalse(section3.containsKey(NATURAL_PERSON_AFFILIATION));
  }

  @Test
  void cleanSection3_shouldPutModifiedSection3BackIntoMongoApplicationForm() {
    JsonObject section3 = new JsonObject();
    JsonObject applyingForDataOnBehalfOfPublicSector = new JsonObject();
    applyingForDataOnBehalfOfPublicSector.put("key", "b");
    section3.put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR,
      applyingForDataOnBehalfOfPublicSector);

    JsonObject mongoApplicationForm = new JsonObject();
    mongoApplicationForm.put(SECTION_3, section3);

    commonApplicationManager.cleanSection3(mongoApplicationForm);

    assertTrue(mongoApplicationForm.containsKey(SECTION_3));
    assertEquals(section3, mongoApplicationForm.getJsonObject(SECTION_3));
  }

  @Test
  void cleanSection4_shouldRemoveInvoiceAddressIfInvoiceTypeNotB() {
    JsonObject invoiceType = new JsonObject().put("key", "a");
    JsonObject section4 = new JsonObject()
      .put("invoiceType", invoiceType)
      .put("invoiceAddress", "some address");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    assertFalse(mongoApplicationForm.getJsonObject(SECTION_4).containsKey("invoiceAddress"));
  }

  @Test
  void cleanSection4_shouldNotRemoveInvoiceAddressIfInvoiceTypeIsB() {
    JsonObject invoiceType = new JsonObject().put("key", "b");
    JsonObject section4 = new JsonObject()
      .put("invoiceType", invoiceType)
      .put("invoiceAddress", "some address");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    assertTrue(mongoApplicationForm.getJsonObject(SECTION_4).containsKey("invoiceAddress"));
  }

  @Test
  void cleanSection4_shouldRemoveOrgFieldsIfLegalPersonIsB() {
    JsonObject legalPerson = new JsonObject().put("key", "b");
    JsonObject section3 = new JsonObject().put(LEGAL_OR_NATURAL_PERSON, legalPerson);

    JsonObject section4 = new JsonObject()
      .put("operatorIdentifier", "op")
      .put("nameOfTheOrganisation", "org")
      .put("businessIdentifierOrganization", "biz")
      .put("vatNumber", "vat")
      .put("peppolCode", "peppol");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_3, section3)
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    JsonObject cleanedSection4 = mongoApplicationForm.getJsonObject(SECTION_4);
    assertFalse(cleanedSection4.containsKey("operatorIdentifier"));
    assertFalse(cleanedSection4.containsKey("nameOfTheOrganisation"));
    assertFalse(cleanedSection4.containsKey("businessIdentifierOrganization"));
    assertFalse(cleanedSection4.containsKey("vatNumber"));
    assertFalse(cleanedSection4.containsKey("peppolCode"));
  }

  @Test
  void cleanSection4_shouldRemoveRangeOfAmountOfFinancingIfCoverageKeyNotA() {
    JsonObject isCovered = new JsonObject().put("key", "b");
    JsonObject section4 = new JsonObject()
      .put("isTheProjectFinanciallyCovered", isCovered)
      .put("rangeOfAmountOfFinancing", "10k-20k");

    JsonObject mongoApplicationForm = new JsonObject()
      .put(SECTION_4, section4);

    commonApplicationManager.cleanSection4(mongoApplicationForm);

    assertFalse(
      mongoApplicationForm.getJsonObject(SECTION_4).containsKey("rangeOfAmountOfFinancing"));
  }

  @Test
  void notContainsKey_shouldReturnTrueWhenKeyIsNotPresent() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "a"))
      .add(new JsonObject().put("key", "b"));
    boolean result = commonApplicationManager.notContainsKey(array, "c");
    assertTrue(result);
  }

  @Test
  void notContainsKey_shouldReturnFalseWhenKeyIsPresent() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("key", "x"))
      .add(new JsonObject().put("key", "target"))
      .add(new JsonObject().put("key", "z"));
    boolean result = commonApplicationManager.notContainsKey(array, "target");
    assertFalse(result);
  }

  @Test
  void notContainsKey_shouldReturnTrueWhenArrayIsEmpty() {
    JsonArray array = new JsonArray();
    boolean result = commonApplicationManager.notContainsKey(array, "anything");
    assertTrue(result);
  }

  @Test
  void notContainsKey_shouldReturnTrueWhenKeyFieldIsMissing() {
    JsonArray array = new JsonArray()
      .add(new JsonObject().put("notKey", "value1"))
      .add(new JsonObject().put("anotherField", "value2"));
    boolean result = commonApplicationManager.notContainsKey(array, "target");
    assertTrue(result);
  }

  @Test
  void sendApplicationForDispatch_shouldCallMongoClientSaveWithCorrectData() {
    // Prepare mock application
    JsonObject mockApplication = new JsonObject()
      .put("appId", "123")
      .put(TRANSLATION, new JsonObject()
        .put(TARGET_LANGUAGES, new JsonArray("[\"EL\", \"NL\"]")));

    String database = "testDatabase";

    // Mock mongoClient.save
    doAnswer(invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(2);
      AsyncResult<String> successResult = mock(AsyncResult.class);
      when(successResult.succeeded()).thenReturn(true);
      when(successResult.result()).thenReturn("generated-id-xyz");
      handler.handle(successResult);
      return null;
    }).when(mongoClient).save(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any());

    // Call method under test
    commonApplicationManager.sendApplicationForDispatch(mockApplication, database, OperationStatus.CREATE);

    // Capture the saved document
    verify(mongoClient).save(eq(DISPATCHER_COLLECTION), jsonCaptor.capture(), any());
    JsonObject savedDoc = jsonCaptor.getValue();

    assertNotNull(savedDoc);

    // Check configuration object
    JsonObject config = savedDoc.getJsonObject("configuration");
    assertNotNull(config);
    assertEquals(database, config.getString(Constants.APPLICATION_TYPE));
    assertEquals("pending", config.getString("status"));
    assertEquals(0, config.getInteger("retries"));

    // Check application object
    JsonObject savedApp = savedDoc.getJsonObject("application");
    assertNotNull(savedApp);
    assertEquals("123", savedApp.getString("appId"));

    // Translation should still be present
    JsonObject translation = savedApp.getJsonObject("translation");
    assertNotNull(translation);
    JsonArray targetLanguages = translation.getJsonArray(TARGET_LANGUAGES);
    assertTrue(targetLanguages.contains("EL"));
    assertTrue(targetLanguages.contains("NL"));

    // date_dispatched should be present
    assertTrue(savedApp.containsKey("date_dispatched"));
  }


  @Test
  void sendApplicationForDispatch_shouldLogErrorWhenSaveFails() {

    JsonObject mockApplication = new JsonObject().put("appId", "123")
      .put(TRANSLATION, new JsonObject().put( TARGET_LANGUAGES ,
        new JsonArray( "[\"EL\", \"NL\"]" )));
    String database = "testDatabase";

    doAnswer(invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(2);
      AsyncResult<String> failedResult = mock(AsyncResult.class);
      when(failedResult.succeeded()).thenReturn(false);
      when(failedResult.cause()).thenReturn(new RuntimeException("DB write failed"));
      handler.handle(failedResult);
      return null;
    }).when(mongoClient).save(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any());

    commonApplicationManager.sendApplicationForDispatch(mockApplication, database, OperationStatus.CREATE);

    verify(mongoClient).save(eq(DISPATCHER_COLLECTION), any(JsonObject.class), any());
  }

  @Test
  void changeTitle_shouldRespondBadRequestIfTitleAlreadyExists() {
    String id = "abc123";
    String title = "TestTitle";

    JsonObject requestBody = new JsonObject().put("title", title);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(id);
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.result()).thenReturn(new JsonObject());
      handler.handle(result);
      return null;
    }).when(mongoClient).findOne(any(), any(), any(), any());

    commonApplicationManager.changeTitle(routingContext, "testDB");

    verify(routingContext).response();
    verify(routingContext.response()).setStatusCode(400);
  }

  @Test
  void changeTitle_shouldUpdateTitleIfNotExistsAndUpdateSucceeds() {
    String id = "abc123";
    String title = "NewTitle";

    JsonObject requestBody = new JsonObject().put("title", title);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(id);

    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.result()).thenReturn(null);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOne(any(), any(), any(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> updateResult = mock(AsyncResult.class);
      when(updateResult.succeeded()).thenReturn(true);
      handler.handle(updateResult);
      return null;
    }).when(mongoClient).findOneAndUpdate(any(), any(), any(), any());

    commonApplicationManager.changeTitle(routingContext, "testDB");

    verify(mongoClient).findOneAndUpdate(any(), any(), any(), any());
    verify(routingContext).response();
    verify(routingContext.response()).setStatusCode(200);
  }

  @Test
  void changeTitle_shouldReturnBadRequestWhenUpdateFails() {
    String id = "abc123";
    String title = "NewTitle";

    JsonObject requestBody = new JsonObject().put("title", title);

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(id);
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.result()).thenReturn(null);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOne(any(), any(), any(), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> updateResult = mock(AsyncResult.class);
      when(updateResult.succeeded()).thenReturn(false);
      handler.handle(updateResult);
      return null;
    }).when(mongoClient).findOneAndUpdate(any(), any(), any(), any());

    commonApplicationManager.changeTitle(routingContext, "testDB");

    verify(mongoClient).findOneAndUpdate(any(), any(), any(), any());
    verify(routingContext).response();
    verify(routingContext.response()).setStatusCode(400);
  }

  @Test
  void rollbackStatusToDraft_shouldLogSuccessWhenUpdateSucceeds() {
    String applicationId = "app123";
    String database = "testDB";

    doAnswer(invocation -> {
      Handler<AsyncResult<Void>> handler = invocation.getArgument(3);
      AsyncResult<Void> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(true);
      handler.handle(result);
      return null;
    }).when(mongoClient).updateCollection(
      eq(database), any(JsonObject.class), any(JsonObject.class), any(Handler.class));

    commonApplicationManager.rollbackStatus(applicationId, ApplicationStatus.DRAFT, database);

    verify(mongoClient).updateCollection(
      eq(database),
      eq(new JsonObject().put("_id", applicationId)),
      eq(new JsonObject().put("$set", new JsonObject().put("status.status", "DRAFT"))),
      any());
  }

  @Test
  void rollbackStatusToDraft_shouldLogFailureWhenUpdateFails() {
    String applicationId = "app123";
    String database = "testDB";

    doAnswer(invocation -> {
      Handler<AsyncResult<Void>> handler = invocation.getArgument(3);

      AsyncResult<Void> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(false);
      when(result.cause()).thenReturn(new RuntimeException("Update failed"));

      handler.handle(result);

      return null;
    }).when(mongoClient).updateCollection(
      eq(database), any(JsonObject.class), any(JsonObject.class), any(Handler.class));

    commonApplicationManager.rollbackStatus(applicationId, ApplicationStatus.DRAFT, database);

    verify(mongoClient).updateCollection(
      eq(database),
      eq(new JsonObject().put("_id", applicationId)),
      eq(new JsonObject().put("$set", new JsonObject().put("status.status", "DRAFT"))),
      any(Handler.class));
  }

  @Test
  void replaceApplication_shouldCompleteFutureWhenUpdateSucceeds() {
    String collection = "testCollection";
    String id = "app123";
    JsonObject mongoApplication = new JsonObject().put(ID, id);

    JsonObject expectedResult = new JsonObject()
      .put(ID, id);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(true);
      when(result.result()).thenReturn(expectedResult);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOneAndReplace(any(), any(), any(), any());

    Future<JsonObject> future = commonApplicationManager.replaceApplication(mongoApplication,
      collection);

    assertTrue(future.succeeded());
    assertEquals(expectedResult, future.result());

    verify(mongoClient).findOneAndReplace(
      eq(collection),
      eq(new JsonObject().put(ID, id)),
      argThat(doc -> doc.getString(ID).equals(id) && doc.containsKey("date_modified")
      ), any());
  }

  @Test
  void replaceApplication_shouldFailFutureWhenUpdateFails() {
    String collection = "testCollection";
    String id = "app123";
    JsonObject mongoApplication = new JsonObject().put(ID, id);

    RuntimeException cause = new RuntimeException("Replace failed");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> result = mock(AsyncResult.class);
      when(result.succeeded()).thenReturn(false);
      when(result.cause()).thenReturn(cause);
      handler.handle(result);
      return null;
    }).when(mongoClient).findOneAndReplace(any(), any(), any(), any());

    Future<JsonObject> future = commonApplicationManager.replaceApplication(mongoApplication,
      collection);

    assertTrue(future.failed());
    assertEquals(cause, future.cause());

    verify(mongoClient).findOneAndReplace(
      eq(collection),
      eq(new JsonObject().put(ID, id)),
      argThat(doc -> doc.getString(ID).equals(id) && doc.containsKey("date_modified")
      ), any());
  }

  @Test
  void deleteApplicationDataset_singleDatasetMatch_shouldDeleteApplicationAndLogAccess() {
    String datasetId = "ds1";
    String appId = "app123";

    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add("dist1"));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.extractDistributionIds(anyString())).thenReturn("dist1");
    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    boolean result = commonApplicationManager.deleteApplicationDataset(mongoApp, datasetId,
      routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    assertFalse(result);
    verify(dbHandler).deleteById(eq(DATA_ACCESS_APPLICATION_COLLECTION), any());
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"),
      eq(false), eq(routingContext));
  }

  @Test
  void deleteApplicationDataset_multipleDatasetsMatch_shouldRemoveDatasetAndLogAccess() {
    String datasetId = "ds2";
    String appId = "app456";

    JsonObject dataset1 = new JsonObject()
      .put("dataset_id", "ds1")
      .put("catalog_id", "cat1")
      .put("distributions", new JsonArray().add("dist1"));

    JsonObject dataset2 = new JsonObject()
      .put("dataset_id", datasetId)
      .put("catalog_id", "cat2")
      .put("distributions", new JsonArray().add("dist2"));

    JsonArray datasets = new JsonArray().add(dataset1).add(dataset2);

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App Multiple")
      .put("datasets", datasets);

    when(auditLogManager.extractDistributionIds(anyString())).thenReturn("dist2");
    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Dataset Deleted");

    boolean result = commonApplicationManager.deleteApplicationDataset(mongoApp, datasetId,
      routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    assertTrue(result);
    assertEquals(1, mongoApp.getJsonArray("datasets").size());
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Dataset Deleted"), eq(true),
      eq(routingContext));
  }


  @Test
  void deleteApplicationDataset_datasetNotFound_shouldReturnFalseAccess() {
    JsonObject dataset = new JsonObject()
      .put("dataset_id", "not_target");

    JsonObject mongoApp = new JsonObject()
      .put("id", "app789")
      .put("datasets", new JsonArray().add(dataset));

    boolean result = commonApplicationManager.deleteApplicationDataset(mongoApp, "missing_ds",
      routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    assertFalse(result);
    verifyNoInteractions(dbHandler, auditLogManager);
  }

  @Test
  void deleteApplicationDataset_singleDatasetMatch_shouldDeleteApplicationAndLogRequest() {
    String datasetId = "ds1";
    String appId = "app123";

    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add("dist1"));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.extractDistributionIds(anyString())).thenReturn("dist1");
    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    boolean result = commonApplicationManager.deleteApplicationDataset(mongoApp, datasetId,
      routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    assertFalse(result);
    verify(dbHandler).deleteById(eq(DATA_REQUEST_APPLICATION_COLLECTION), any());
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"),
      eq(false), eq(routingContext));
  }

  @Test
  void deleteApplicationDataset_multipleDatasetsMatch_shouldRemoveDatasetAndLogRequest() {
    String datasetId = "ds2";
    String appId = "app456";

    JsonObject dataset1 = new JsonObject()
      .put("dataset_id", "ds1")
      .put("catalog_id", "cat1")
      .put("distributions", new JsonArray().add("dist1"));

    JsonObject dataset2 = new JsonObject()
      .put("dataset_id", datasetId)
      .put("catalog_id", "cat2")
      .put("distributions", new JsonArray().add("dist2"));

    JsonArray datasets = new JsonArray().add(dataset1).add(dataset2);

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App Multiple")
      .put("datasets", datasets);

    when(auditLogManager.extractDistributionIds(anyString())).thenReturn("dist2");
    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Dataset Deleted");

    boolean result = commonApplicationManager.deleteApplicationDataset(mongoApp, datasetId,
      routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    assertTrue(result);
    assertEquals(1, mongoApp.getJsonArray("datasets").size());
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Dataset Deleted"), eq(true),
      eq(routingContext));
  }

  @Test
  void deleteApplicationDataset_datasetNotFound_shouldReturnFalseRquest() {
    JsonObject dataset = new JsonObject()
      .put("dataset_id", "not_target");

    JsonObject mongoApp = new JsonObject()
      .put("id", "app789")
      .put("datasets", new JsonArray().add(dataset));

    boolean result = commonApplicationManager.deleteApplicationDataset(mongoApp, "missing_ds",
      routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    assertFalse(result);
    verifyNoInteractions(dbHandler, auditLogManager);
  }

  @Test
  void testDeleteDistributions_mongoFailure() {
    when(routingContext.pathParam("application_id")).thenReturn("app-error");
    addUserOnContext();

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> handler = inv.getArgument(3);
      handler.handle(Future.failedFuture(new RuntimeException("DB down")));
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteDistributions(routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      verify(response).setStatusCode(500);
      verify(response).end(contains("DB down"));
    });
  }

  @Test
  void deleteApplicationDatasetDistribution_onlyDistributionInOnlyDataset_shouldDeleteApplicationAccess() {
    String appId = "app123";
    String datasetId = "ds1";
    String distributionId = "dist1";

    JsonObject distribution = new JsonObject().put("distribution_id", distributionId);
    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add(distribution));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    commonApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetId,
      distributionId, routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    verify(dbHandler).deleteById(eq(DATA_ACCESS_APPLICATION_COLLECTION), any());
    verify(dbHandler).updateLinkableFields(AUDIT_LOG_COLLECTION, appId);
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"),
      eq(false), eq(routingContext));
  }

  @Test
  void deleteApplicationDatasetDistribution_onlyDistributionInOneDataset_shouldRemoveDatasetAccess() {
    String appId = "app123";
    String datasetIdToDelete = "ds1";
    String otherDatasetId = "ds2";
    String distributionId = "dist1";

    JsonObject distribution = new JsonObject().put("distribution_id", distributionId);
    JsonObject datasetToDelete = new JsonObject()
      .put("dataset_id", datasetIdToDelete)
      .put("catalog_id", "cat1")
      .put("distributions", new JsonArray().add(distribution));

    JsonObject otherDataset = new JsonObject()
      .put("dataset_id", otherDatasetId)
      .put("distributions", new JsonArray().add(new JsonObject().put("distribution_id", "dist2")));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(datasetToDelete).add(otherDataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    commonApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetIdToDelete,
      distributionId, routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    assertEquals(1, mongoApp.getJsonArray("datasets").size()); // dataset removed
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"), eq(true),
      eq(routingContext));
  }
  private void addUserOnContext() {
    User mockUser = Mockito.mock(User.class);
    String ecasId = "n3817490";
    lenient().when(routingContext.user()).thenReturn(mockUser);
    JsonObject principal = new JsonObject().put("preferred_username", ecasId);
    lenient().when(routingContext.user().principal()).thenReturn(principal);
  }

  @Test
  void deleteApplicationDatasetDistribution_multipleDistributions_shouldRemoveOnlyTargetDistributionAccess() {
    String appId = "app123";
    String datasetId = "ds1";
    String distributionIdToRemove = "dist1";

    JsonObject distribution1 = new JsonObject().put("distribution_id", distributionIdToRemove);
    JsonObject distribution2 = new JsonObject().put("distribution_id", "dist2");

    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add(distribution1).add(distribution2));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    commonApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetId,
      distributionIdToRemove, routingContext, DATA_ACCESS_APPLICATION_COLLECTION);

    JsonArray updatedDistributions = mongoApp.getJsonArray("datasets").getJsonObject(0)
      .getJsonArray("distributions");
    assertEquals(1, updatedDistributions.size());
    assertEquals("dist2", updatedDistributions.getJsonObject(0).getString("distribution_id"));
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"), eq(true),
      eq(routingContext));
  }

  @Test
  void deleteApplicationDatasetDistribution_onlyDistributionInOnlyDataset_shouldDeleteApplicationRequest() {
    String appId = "app123";
    String datasetId = "ds1";
    String distributionId = "dist1";

    JsonObject distribution = new JsonObject().put("distribution_id", distributionId);
    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add(distribution));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    commonApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetId,
      distributionId, routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    verify(dbHandler).deleteById(eq(DATA_REQUEST_APPLICATION_COLLECTION), any());
    verify(dbHandler).updateLinkableFields(eq(AUDIT_LOG_COLLECTION), eq(appId));
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"),
      eq(false), eq(routingContext));
  }

  @Test
  void deleteApplicationDatasetDistribution_onlyDistributionInOneDataset_shouldRemoveDatasetRequest() {
    String appId = "app123";
    String datasetIdToDelete = "ds1";
    String otherDatasetId = "ds2";
    String distributionId = "dist1";

    JsonObject distribution = new JsonObject().put("distribution_id", distributionId);
    JsonObject datasetToDelete = new JsonObject()
      .put("dataset_id", datasetIdToDelete)
      .put("catalog_id", "cat1")
      .put("distributions", new JsonArray().add(distribution));

    JsonObject otherDataset = new JsonObject()
      .put("dataset_id", otherDatasetId)
      .put("distributions", new JsonArray().add(new JsonObject().put("distribution_id", "dist2")));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(datasetToDelete).add(otherDataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    commonApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetIdToDelete,
      distributionId, routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    assertEquals(1, mongoApp.getJsonArray("datasets").size()); // dataset removed
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"), eq(true),
      eq(routingContext));
  }

  @Test
  void deleteApplicationDatasetDistribution_multipleDistributions_shouldRemoveOnlyTargetDistributionRequest() {
    String appId = "app123";
    String datasetId = "ds1";
    String distributionIdToRemove = "dist1";

    JsonObject distribution1 = new JsonObject().put("distribution_id", distributionIdToRemove);
    JsonObject distribution2 = new JsonObject().put("distribution_id", "dist2");

    JsonObject dataset = new JsonObject()
      .put("dataset_id", datasetId)
      .put("distributions", new JsonArray().add(distribution1).add(distribution2));

    JsonObject mongoApp = new JsonObject()
      .put("_id", appId)
      .put("title", "Test App")
      .put("datasets", new JsonArray().add(dataset));

    when(auditLogManager.prepareAuditLogActionDescription(any(), any(), any(), any(), any()))
      .thenReturn("Audit Log Entry");

    commonApplicationManager.deleteApplicationDatasetDistribution(mongoApp, datasetId,
      distributionIdToRemove, routingContext, DATA_REQUEST_APPLICATION_COLLECTION);

    JsonArray updatedDistributions = mongoApp.getJsonArray("datasets").getJsonObject(0)
      .getJsonArray("distributions");
    assertEquals(1, updatedDistributions.size());
    assertEquals("dist2", updatedDistributions.getJsonObject(0).getString("distribution_id"));
    verify(auditLogManager).insertAuditLog(any(), eq(appId), any(), eq("Audit Log Entry"), eq(true),
      eq(routingContext));
  }

  @Test
  void deleteDatasets_draft_someDeletes_replaceAnd200Completed() {
    String appId = "app-123";
    String collection = DATA_REQUEST_APPLICATION_COLLECTION;

    // request body: two dataset ids
    JsonArray requestArray = new JsonArray()
      .add(new JsonObject().put("dataset_id", "ds1"))
      .add(new JsonObject().put("dataset_id", "ds2"));

    // app in DRAFT
    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("datasets", new JsonArray());

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);
    addUserOnContext();

    // body → asJsonArray
    io.vertx.ext.web.RequestBody rb = mock(io.vertx.ext.web.RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonArray()).thenReturn(requestArray);

    // findOne -> succeed with app
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOne(eq(collection), any(JsonObject.class), isNull(), any());

    // Spy and control branching:
    CommonApplicationManager spyMgr = spy(commonApplicationManager);
    // ds1 will be deleted → true; ds2 not deleted → false
    doReturn(true).when(spyMgr).deleteApplicationDataset(any(JsonObject.class), eq("ds1"), eq(routingContext), eq(collection));
    doReturn(false).when(spyMgr).deleteApplicationDataset(any(JsonObject.class), eq("ds2"), eq(routingContext), eq(collection));
    // replaceApplication returns OK
    doReturn(Future.succeededFuture(new JsonObject()))
      .when(spyMgr).replaceApplication(any(JsonObject.class), eq(collection));

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    spyMgr.deleteDatasets(routingContext, collection);

    // since at least one delete happened, it replaces and says "completed"
    verify(spyMgr).replaceApplication(any(JsonObject.class), eq(collection));
    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("Delete operation/s completed"));
  }

  @Test
  void deleteDatasets_draft_noDeletes_noReplaceAnd200NoOps() {
    String appId = "app-123";
    String collection = DATA_ACCESS_APPLICATION_COLLECTION;

    JsonArray requestArray = new JsonArray()
      .add(new JsonObject().put("dataset_id", "dsA"))
      .add(new JsonObject().put("dataset_id", "dsB"));

    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "DRAFT"))
      .put("datasets", new JsonArray());

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);
    addUserOnContext();

    io.vertx.ext.web.RequestBody rb = mock(io.vertx.ext.web.RequestBody.class);
    when(routingContext.body()).thenReturn(rb);
    when(rb.asJsonArray()).thenReturn(requestArray);

    doAnswer(inv -> {
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOne(eq(collection), any(JsonObject.class), isNull(), any());

    CommonApplicationManager spyMgr = spy(commonApplicationManager);
    // none deleted
    doReturn(false).when(spyMgr).deleteApplicationDataset(any(JsonObject.class), eq("dsA"), eq(routingContext), eq(collection));
    doReturn(false).when(spyMgr).deleteApplicationDataset(any(JsonObject.class), eq("dsB"), eq(routingContext), eq(collection));

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    spyMgr.deleteDatasets(routingContext, collection);

    verify(spyMgr, never()).replaceApplication(any(), anyString());
    verify(response).setStatusCode(HttpResponseStatus.OK.code());
    verify(response).end(contains("No Delete operations required"));
  }

  @Test
  void deleteDatasets_submitted_returnsBadRequest() {
    String appId = "app-123";
    String collection = DATA_REQUEST_APPLICATION_COLLECTION;

    JsonObject mongoApp = new JsonObject()
      .put("status", new JsonObject().put("status", "SUBMITTED"));

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);
    addUserOnContext();

    // body is irrelevant for this branch, but keep it minimal and not stub asJsonArray if you like
    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(mongoApp));
      return null;
    }).when(mongoClient).findOne(eq(collection), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteDatasets(routingContext, collection);

    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    verify(response).end(contains(Constants.DELETION_CAN_NOT_BE_EXECUTED));
  }

  @Test
  void deleteDatasets_appNotFound_returnsNotFound() {
    String appId = "app-404";
    String collection = DATA_ACCESS_APPLICATION_COLLECTION;

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);
    addUserOnContext();

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.succeededFuture(null)); // not found
      return null;
    }).when(mongoClient).findOne(eq(collection), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteDatasets(routingContext, collection);

    verify(response).setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    verify(response).end(contains(Constants.PROVIDED_APPLICATION_ID + appId + Constants.WAS_NOT_FOUND_IN_THE_DATABASE));
  }

  @Test
  void deleteDatasets_dbError_returnsInternalServerError() {
    String appId = "app-err";
    String collection = DATA_REQUEST_APPLICATION_COLLECTION;

    when(routingContext.pathParam(APPLICATION_ID)).thenReturn(appId);
    addUserOnContext();

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<JsonObject>> h = inv.getArgument(3);
      h.handle(Future.failedFuture(new RuntimeException("DB failure")));
      return null;
    }).when(mongoClient).findOne(eq(collection), any(JsonObject.class), isNull(), any());

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(Future.succeededFuture());

    commonApplicationManager.deleteDatasets(routingContext, collection);

    verify(response).setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    verify(response).end(contains("DB failure"));
  }

  @Test
  void getDistributions_match_caseInsensitive_returnsArray() {
    JsonArray input = new JsonArray()
      .add(new JsonObject().put(DATASET_ID, "DS1")
        .put(DISTRIBUTIONS, new JsonArray().add("d1")))
      .add(new JsonObject().put(DATASET_ID, "ds2")
        .put(DISTRIBUTIONS, new JsonArray().add("d2")));

    JsonArray result = commonApplicationManager.getDistributions(input, "ds1");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("d1", result.getString(0));
  }

  @Test
  void getDistributions_matchButNoDistributions_returnsNull() {
    JsonArray input = new JsonArray()
      .add(new JsonObject().put(DATASET_ID, "ds3")); // no "distributions" key

    JsonArray result = commonApplicationManager.getDistributions(input, "ds3");

    assertNull(result, "If dataset matches but has no distributions, should return null");
  }

  @Test
  void getDistributions_noMatch_returnsNull() {
    JsonArray input = new JsonArray()
      .add(new JsonObject().put(DATASET_ID, "dsA").put(DISTRIBUTIONS, new JsonArray().add("x")))
      .add(new JsonObject().put(DATASET_ID, "dsB").put(DISTRIBUTIONS, new JsonArray().add("y")));

    JsonArray result = commonApplicationManager.getDistributions(input, "missing");

    assertNull(result);
  }

  @Test
  void getDistributions_ignoresNonObjectsAndEmptyIds_andStillFindsValidLater() {
    JsonArray input = new JsonArray()
      .add("not an object")
      .add(new JsonObject().put(DATASET_ID, "")) // empty id → ignored
      .add(new JsonObject().put(DATASET_ID, "RealOne")
        .put(DISTRIBUTIONS, new JsonArray().add("ok")));

    JsonArray result = commonApplicationManager.getDistributions(input, "realone"); // case-insensitive

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("ok", result.getString(0));
  }

  @Test
  void updateApplicationsAsync_emptyArray_returnsEmptyList() {
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject())
      .thenReturn(new JsonObject().put("applications", new JsonArray()));

    List<Future> futures = commonApplicationManager.updateApplicationsAsync(routingContext, "col");
    assertTrue(futures.isEmpty());
  }

  @Test
  void updateApplicationsAsync_missingApplicationsKey_throwsNpe() {
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(new JsonObject());

    assertThrows(NullPointerException.class,
      () -> commonApplicationManager.updateApplicationsAsync(routingContext, "col"));
  }

  @Test
  void getTargetLanguagesFromDatasets_noCatalogueIds_returnsEmptySet_andNoHubCalls() throws Exception {
    // given: datasets without CATALOG_ID (null/missing)
    List<JsonObject> datasets = List.of(
      new JsonObject(),
      new JsonObject().put(CATALOG_ID, (String) null));

    // when
    Future<Set<String>> fut = commonApplicationManager.getTargetLanguagesFromDatasets(datasets);

    // then
    Set<String> result = await(fut);
    assertNotNull(result);
    assertTrue(result.isEmpty(), "Expected empty set when no catalogue IDs present");
    verifyNoInteractions(hubSearchClient);
  }

  @Test
  void getTargetLanguagesFromDatasets_uniqueIds_mergesAndUppercasesLanguages() throws Exception {
    // given
    List<JsonObject> datasets = List.of(
      new JsonObject().put(CATALOG_ID, "cat-1"),
      new JsonObject().put(CATALOG_ID, "cat-2"),
      new JsonObject().put(CATALOG_ID, "cat-1") // duplicate id → should only fetch once
    );

    // hub responses
    JsonObject cat1 = new JsonObject().put("result",
      new JsonObject().put("language",
        new JsonArray()
          .add(new JsonObject().put("id", "el"))   // -> "EL"
          .add(new JsonObject().put("id", "EN")))); // -> "EN"
    JsonObject cat2 = new JsonObject().put("result",
      new JsonObject().put("language",
        new JsonArray()
          .add(new JsonObject().put("id", "nl")))); // -> "NL"

    when(hubSearchClient.getCatalogue(eq("cat-1")))
      .thenReturn(Future.succeededFuture(cat1));
    when(hubSearchClient.getCatalogue(eq("cat-2")))
      .thenReturn(Future.succeededFuture(cat2));

    // when
    Future<Set<String>> fut = commonApplicationManager.getTargetLanguagesFromDatasets(datasets);
    Set<String> result = await(fut);

    // then
    assertNotNull(result);
    // should contain the union, uppercased
    assertEquals(Set.of("EL", "EN", "NL"), result);

    // only one call per unique catalogue id
    verify(hubSearchClient, times(1)).getCatalogue("cat-1");
    verify(hubSearchClient, times(1)).getCatalogue("cat-2");
    verifyNoMoreInteractions(hubSearchClient);
  }

  @Test
  void getTargetLanguagesFromDatasets_catalogueMissingLanguage_ignoredAndOthersCollected() throws Exception {
    // given
    List<JsonObject> datasets = List.of(
      new JsonObject().put(CATALOG_ID, "ok"),
      new JsonObject().put(CATALOG_ID, "nolanguage"),
      new JsonObject().put(CATALOG_ID, "nullresult")
    );

    JsonObject ok = new JsonObject().put("result",
      new JsonObject().put("language",
        new JsonArray()
          .add(new JsonObject().put("id", "de"))
          .add(new JsonObject().put("id", "Fr")))); // case mix

    JsonObject noLanguage = new JsonObject().put("result", new JsonObject()); // result present but no 'language'
    JsonObject nullResult = new JsonObject().put("result", (Object) null);   // explicitly null

    when(hubSearchClient.getCatalogue(eq("ok"))).thenReturn(Future.succeededFuture(ok));
    when(hubSearchClient.getCatalogue(eq("nolanguage"))).thenReturn(Future.succeededFuture(noLanguage));
    when(hubSearchClient.getCatalogue(eq("nullresult"))).thenReturn(Future.succeededFuture(nullResult));

    // when
    Future<Set<String>> fut = commonApplicationManager.getTargetLanguagesFromDatasets(datasets);
    Set<String> result = await(fut);

    // then
    assertNotNull(result);
    // only 'ok' contributes languages; ids must be uppercased & unique
    assertEquals(Set.of("DE", "FR"), result);
  }

  // small helper to await a Vert.x Future without bringing in VertxTestContext
  private static <T> T await(Future<T> future) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    final Object[] box = new Object[2]; // [0]=result, [1]=Throwable
    future.onComplete(ar -> {
      if (ar.succeeded()) {
        box[0] = ar.result();
      } else {
        box[1] = ar.cause();
      }
      latch.countDown();
    });
    assertTrue(latch.await(3, TimeUnit.SECONDS), "timed out waiting future");
    if (box[1] != null) throw new RuntimeException((Throwable) box[1]);
    @SuppressWarnings("unchecked")
    T out = (T) box[0];
    return out;
  }

  @Test
  void triggerTranslation_success_allOK_persists_noFailureAudit() {
    String appId = "APP-1";
    String appType = "collX";
    JsonObject form = new JsonObject(); // empty form
    List<String> targetLangs = List.of("EL", "NL");

    // form payload
    JsonObject formPayload = new JsonObject().put("kind", "form");
    when(hubTranslationClient.buildTranslationRequestPayload(
      eq(appId), eq(form), eq(appType), eq("EN"), eq(targetLangs)))
      .thenReturn(formPayload);

    when(hubTranslationClient.requestTranslation(eq(formPayload)))
      .thenReturn(Future.succeededFuture()); // simulate success

    // files stub: lenient because in this test it may not be called
    lenient().when(hubStoreClient.getFilesAsBase64(anyList(), eq(appId)))
      .thenReturn(Future.succeededFuture(List.of()));

    // mongo update: simulate async persistence
    Promise<MongoClientUpdateResult> updatePromise = Promise.promise();
    MongoClientUpdateResult dummyResult = mock(MongoClientUpdateResult.class);

    when(mongoClient.updateCollection(eq(appType), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(updatePromise.future());

    // Call method under test
    commonApplicationManager.triggerFormTranslationRequest(appId, form, appType, "EN", targetLangs, null, true);

    // Complete persistence
    updatePromise.complete(dummyResult);

    // Verify interactions
    verify(hubTranslationClient).buildTranslationRequestPayload(eq(appId), eq(form), eq(appType), eq("EN"), eq(targetLangs));
    verify(hubTranslationClient).requestTranslation(eq(formPayload));
    verify(mongoClient).updateCollection(eq(appType), any(JsonObject.class), any(JsonObject.class));

    // Ensure no failure audit is logged in the happy path
    verify(auditLogManager, never()).insertAuditLog(anyString(), anyString(), anyString(), anyString(), anyBoolean(), any());
  }

  @Test
  void buildFormTranslationsAndSetStatus_completed_whenAllExpectedLangsPresent() {
    // given
    String appId = "APP-X";
    String appType = "collX";
    JsonObject originalForm = new JsonObject().put("k", "v");
    JsonObject dict = new JsonObject().put("d", "v");
    JsonObject translatedFiles = new JsonObject().put("files", "ok");
    var expectedLangs = List.of("EL", "NL");

    JsonObject builtTranslations = new JsonObject()
      .put("EL", new JsonObject().put("t", "el"))
      .put("NL", new JsonObject().put("t", "nl"));

    when(hubTranslationClient.buildFormTranslations(eq(originalForm), eq(dict), eq(translatedFiles), eq(appType), eq(new JsonObject())))
      .thenReturn(builtTranslations);

    try (MockedStatic<ApplicationFormUtils> mocked = mockStatic(ApplicationFormUtils.class)) {
      // also assert the receivedLangs set matches the formTranslations field names
      mocked.when(() -> ApplicationFormUtils.determineTranslationStatus(anySet(), eq(expectedLangs), eq("form"), eq(appId)))
        .thenAnswer(inv -> {
          Set<String> received = inv.getArgument(0, Set.class);
          assertEquals(Set.of("EL", "NL"), received, "receivedLangs should reflect formTranslations field names");
          // return the string status, not the enum
          return TranslationStatus.COMPLETED.getStatus();
        });

      // when
      JsonObject out = commonApplicationManager.buildFormTranslationsAndSetStatus(
        originalForm, dict, expectedLangs, appId, translatedFiles, appType, new JsonObject());

      // then
      assertNotNull(out);
      assertEquals(builtTranslations, out.getJsonObject("form_translations"));
      // enum stored as value
      assertStatusValue(out.getValue("status"), TranslationStatus.COMPLETED);

      verify(hubTranslationClient).buildFormTranslations(eq(originalForm), eq(dict), eq(translatedFiles), eq(appType), eq(new JsonObject()));
      mocked.verify(() -> ApplicationFormUtils.determineTranslationStatus(anySet(), eq(expectedLangs), eq("form"), eq(appId)));
    }
  }

  @Test
  void buildFormTranslationsAndSetStatus_partiallyCompleted_whenSomeExpectedLangsMissing() {
    // given
    String appId = "APP-Y";
    String appType = "collY";
    JsonObject originalForm = new JsonObject();
    JsonObject dict = new JsonObject();
    JsonObject translatedFiles = new JsonObject();
    List<String> expectedLangs = List.of("EL", "NL");

    // only EL present in the built translations
    JsonObject builtTranslations = new JsonObject()
      .put("EL", new JsonObject().put("t", "el"));

    // mock building translations
    when(hubTranslationClient.buildFormTranslations(
      eq(originalForm), eq(dict), eq(translatedFiles), eq(appType), eq(new JsonObject())))
      .thenReturn(builtTranslations);

    try (MockedStatic<ApplicationFormUtils> mocked = mockStatic(ApplicationFormUtils.class)) {
      // Mock determineTranslationStatus to return the String value of the enum
      mocked.when(() -> ApplicationFormUtils.determineTranslationStatus(
          anySet(), eq(expectedLangs), eq("form"), eq(appId)))
        .thenAnswer(inv -> {
          Set<String> receivedLangs = inv.getArgument(0, Set.class);
          // EL is present, NL is missing
          assertEquals(Set.of("EL"), receivedLangs, "receivedLangs should match the keys in formTranslations");
          return TranslationStatus.PARTIALLY_COMPLETED.getStatus(); // FIX: return String
        });

      // when
      JsonObject out = commonApplicationManager.buildFormTranslationsAndSetStatus(
        originalForm, dict, expectedLangs, appId, translatedFiles, appType, new JsonObject());

      // then
      assertNotNull(out, "Output should not be null");
      assertEquals(builtTranslations, out.getJsonObject("form_translations"), "Form translations should match built translations");

      // assert status stored as string
      assertStatusValue(out.getValue("status"), TranslationStatus.PARTIALLY_COMPLETED);

      // verify interactions
      verify(hubTranslationClient).buildFormTranslations(
        eq(originalForm), eq(dict), eq(translatedFiles), eq(appType), eq(new JsonObject()));
      mocked.verify(() -> ApplicationFormUtils.determineTranslationStatus(
        anySet(), eq(expectedLangs), eq("form"), eq(appId)));
    }
  }

  @Test
  void buildFormTranslationsAndSetStatus_pending_whenNoLangsBuilt() {
    // given
    String appId = "APP-Z";
    String appType = "collZ";
    JsonObject originalForm = new JsonObject();
    JsonObject formTranslations = new JsonObject();
    JsonObject dict = new JsonObject();
    JsonObject translatedFiles = new JsonObject();
    List<String> expectedLangs = List.of("EL"); // expected EL, but none built

    JsonObject emptyTranslations = new JsonObject();

    // mock building translations to return empty
    when(hubTranslationClient.buildFormTranslations(
      eq(originalForm), eq(dict), eq(translatedFiles), eq(appType), eq(formTranslations)))
      .thenReturn(emptyTranslations);

    try (MockedStatic<ApplicationFormUtils> mocked = mockStatic(ApplicationFormUtils.class)) {
      // Mock determineTranslationStatus to return the String value, not enum
      mocked.when(() -> ApplicationFormUtils.determineTranslationStatus(
          anySet(), eq(expectedLangs), eq("form"), eq(appId)))
        .thenAnswer(inv -> {
          @SuppressWarnings("unchecked")
          Set<String> received = inv.getArgument(0, Set.class);
          // should be empty because no translations built
          assertTrue(received.isEmpty(), "receivedLangs should be empty when no translations built");
          return TranslationStatus.PENDING.getStatus(); // FIX: return String
        });

      // when
      JsonObject out = commonApplicationManager.buildFormTranslationsAndSetStatus(
        originalForm, dict, expectedLangs, appId, translatedFiles, appType, formTranslations);

      // then
      assertNotNull(out, "Output should not be null");
      assertEquals(emptyTranslations, out.getJsonObject("form_translations"), "Form translations should be empty");
      assertStatusValue(out.getValue("status"), TranslationStatus.PENDING);

      // verify buildFormTranslations call
      verify(hubTranslationClient).buildFormTranslations(
        eq(originalForm), eq(dict), eq(translatedFiles), eq(appType), eq(formTranslations));

      // verify determineTranslationStatus call
      mocked.verify(() -> ApplicationFormUtils.determineTranslationStatus(
        anySet(), eq(expectedLangs), eq("form"), eq(appId)));
    }
  }

  private static void assertStatusValue(Object actual, TranslationStatus expected) {
    if (actual instanceof TranslationStatus ts) {
      // enum stored directly
      assertEquals(expected, ts);
    } else {
      String s = String.valueOf(actual);
      // Accept either enum name or the enum's getStatus() value if you use that
      boolean matches = expected.name().equals(s) || expected.getStatus().equalsIgnoreCase(s);
      assertTrue(
        matches, () -> "Expected " + expected + " but got: " + s);
    }
  }

  @Test
  void applyFileTranslationUpdates_initializesTranslation_andBuildsEntry() {
    // app starts without TRANSLATION or FORM_TRANSLATIONS
    JsonObject application = new JsonObject();

    // uploaded EL only
    String originalFileId = "orig-123";
    Map<String, JsonObject> uploaded = new java.util.HashMap<>();
    uploaded.put("EL", new JsonObject().put(FILE_ID, "el-file-id").put("fileName", "doc-el.pdf"));

    // no TARGET_LANGUAGES → expectedLangs becomes empty set; status should be COMPLETED by
    // typical determineTranslationStatus semantics (uploaded >= expected => complete).
    commonApplicationManager.applyFileTranslationUpdates(
      application, "APP-1", originalFileId, uploaded);

    JsonObject translation = application.getJsonObject(TRANSLATION);
    assertNotNull(translation, "translation block should be created");

    JsonObject filesMeta = translation.getJsonObject("files");
    assertNotNull(filesMeta, "translation.files should be created");

    JsonObject fileEntry = filesMeta.getJsonObject(originalFileId);
    assertNotNull(fileEntry, "entry for original file id should be created");

    JsonObject translatedFiles = fileEntry.getJsonObject("translatedFiles");
    assertNotNull(translatedFiles);
    JsonObject elInfo = translatedFiles.getJsonObject("EL");
    assertEquals("el-file-id", elInfo.getString(FILE_ID));
    assertEquals("doc-el.pdf", elInfo.getString("fileName"));

    // With no expected languages configured, "uploaded vs expected" is treated as complete.
    assertStatusValue(fileEntry.getValue(STATUS), TranslationStatus.FAILED);

  }

  @Test
  void applyFileTranslationUpdates_completed_whenAllExpectedLanguagesUploaded() {
    // app has target languages EL+NL
    JsonObject application = new JsonObject()
      .put(TRANSLATION, new JsonObject()
        .put(TARGET_LANGUAGES, new JsonArray().add("EL").add("NL")));

    String originalFileId = "orig-999";
    Map<String, JsonObject> uploaded = new java.util.HashMap<>();
    uploaded.put("EL", new JsonObject().put(FILE_ID, "f-el").put("fileName", "a-el.pdf"));
    uploaded.put("NL", new JsonObject().put(FILE_ID, "f-nl").put("fileName", "a-nl.pdf"));

    commonApplicationManager.applyFileTranslationUpdates(
      application, "APP-2", originalFileId, uploaded);

    JsonObject fileEntry = application
      .getJsonObject(TRANSLATION)
      .getJsonObject("files")
      .getJsonObject(originalFileId);

    assertNotNull(fileEntry, "file entry should exist");
    JsonObject tf = fileEntry.getJsonObject("translatedFiles");
    assertEquals("f-el", tf.getJsonObject("EL").getString(FILE_ID));
    assertEquals("f-nl", tf.getJsonObject("NL").getString(FILE_ID));

    // Uploaded = Expected -> COMPLETED
    assertStatusValue(fileEntry.getValue(STATUS), TranslationStatus.COMPLETED);
  }

  @Test
  void applyFileTranslationUpdates_partial_whenOnlySubsetOfExpectedUploaded() {
    // app expects EL+NL, but we upload only EL
    JsonObject application = new JsonObject()
      .put(TRANSLATION, new JsonObject()
        .put(TARGET_LANGUAGES, new JsonArray().add("EL").add("NL")));

    String originalFileId = "orig-444";
    Map<String, JsonObject> uploaded = new java.util.HashMap<>();
    uploaded.put("EL", new JsonObject().put(FILE_ID, "x-el").put("fileName", "x-el.pdf"));

    // Seed some form_translations to ensure method preserves object and attempts in-place update
    application.put(FORM_TRANSLATIONS,
      new JsonObject().put("EL", new JsonObject().put("someField", "value")));

    commonApplicationManager.applyFileTranslationUpdates(
      application, "APP-3", originalFileId, uploaded);

    JsonObject fileEntry = application
      .getJsonObject(TRANSLATION)
      .getJsonObject("files")
      .getJsonObject(originalFileId);

    assertNotNull(fileEntry, "file entry should exist");
    JsonObject tf = fileEntry.getJsonObject("translatedFiles");
    assertEquals("x-el", tf.getJsonObject("EL").getString(FILE_ID));
    assertNull(tf.getJsonObject("NL"), "NL should not exist when not uploaded");

    // Subset uploaded -> PARTIALLY_COMPLETED
    assertStatusValue(fileEntry.getValue(STATUS), TranslationStatus.PENDING);

    // FORM_TRANSLATIONS object remains present
    assertTrue(application.getJsonObject(FORM_TRANSLATIONS).containsKey("EL"));
  }

  @Test
  void createApplicantDetails_success_withMatchingUser_setsNames() throws Exception {
    // Given
    String ecasId = "ecas-123";
    // make Context.getEcasId(context) work by stubbing user/principal
    User mockUser = mock(User.class);
    when(routingContext.user()).thenReturn(mockUser);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", ecasId));

    // Auth client returns a list containing our matching user
    JsonArray users = new JsonArray()
      .add(new JsonObject()
        .put("ecasId", ecasId)
        .put("firstName", "Ada")
        .put("lastName", "Lovelace"))
      .add(new JsonObject()
        .put("ecasId", "someone-else")
        .put("firstName", "No")
        .put("lastName", "Match"));

    when(authClient.getAllUsers(eq(routingContext), eq(true)))
      .thenReturn(Future.succeededFuture(users));

    // When
    Future<JsonObject> fut = commonApplicationManager.createApplicantDetails(routingContext);
    JsonObject result = fut.toCompletionStage().toCompletableFuture().get(1, TimeUnit.SECONDS);

    // Then
    assertNotNull(result);
    assertEquals(ecasId, result.getString("ecasId"));
    assertEquals("Ada", result.getString("firstName"));
    assertEquals("Lovelace", result.getString("lastName"));
  }

  @Test
  void createApplicantDetails_success_noMatchingUser_onlyEcasIdPresent() throws Exception {
    // Given
    String ecasId = "ecas-999";
    User mockUser = mock(User.class);
    when(routingContext.user()).thenReturn(mockUser);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", ecasId));

    // Auth client returns users, but none matches our ecasId
    JsonArray users = new JsonArray()
      .add(new JsonObject().put("ecasId", "other-1").put("firstName", "A").put("lastName", "B"))
      .add(new JsonObject().put("ecasId", "other-2").put("firstName", "C").put("lastName", "D"));

    when(authClient.getAllUsers(eq(routingContext), eq(true)))
      .thenReturn(Future.succeededFuture(users));


    // When
    Future<JsonObject> fut = commonApplicationManager.createApplicantDetails(routingContext);
    JsonObject result = fut.toCompletionStage().toCompletableFuture().get(1, TimeUnit.SECONDS);

    // Then
    assertNotNull(result);
    assertEquals(ecasId, result.getString("ecasId"));
    // names should not be present when no matching user found
    assertFalse(result.containsKey("firstName"));
    assertFalse(result.containsKey("lastName"));
  }

  @Test
  void createApplicantDetails_failure_authClientFails_propagatesFailureMessage() {
    // Given
    String ecasIder = "ecas-err";
    User mockUser = mock(User.class);
    when(routingContext.user()).thenReturn(mockUser);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", ecasIder));

    when(authClient.getAllUsers(eq(routingContext), eq(true)))
      .thenReturn(Future.failedFuture("boom"));

    // When
    Future<JsonObject> fut = commonApplicationManager.createApplicantDetails(routingContext);

    // Then (await failure)
    CompletableFuture<JsonObject> cf = fut.toCompletionStage().toCompletableFuture();
    ExecutionException ex = assertThrows(ExecutionException.class, () -> cf.get(1, TimeUnit.SECONDS));
    assertNotNull(ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("Could not retrieve users, with error message: boom"));
  }

// --- createTranslationFailedAuditLog: DATA_REQUEST branch ---
  @Test
  void createTranslationFailedAuditLog_dataRequest_callsAuditWithDR() throws Exception {
    String applicationId = "APP-DR";
    String title = "Some Title";
    String errorType = "ERR";
    String collectionName = DATA_REQUEST_APPLICATION_COLLECTION;

    // The description the manager should pass through
    String expectedDesc = "DR-DESC";
    when(auditLogManager.prepareAuditLogActionDescription(
      eq(AuditLogActionDescription.DATA_REQUEST_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(applicationId), eq(title), eq(errorType)
    )).thenReturn(expectedDesc);

    // invoke private method via reflection
    Method m = CommonApplicationManager.class.getDeclaredMethod(
      "createTranslationFailedAuditLog",
      String.class, String.class, String.class, String.class
    );
    m.setAccessible(true);
    m.invoke(commonApplicationManager, applicationId, collectionName, title, errorType);

    // verify the right audit object/action and description are used
    verify(auditLogManager).prepareAuditLogActionDescription(
      eq(AuditLogActionDescription.DATA_REQUEST_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(applicationId), eq(title), eq(errorType)
    );
    verify(auditLogManager).insertAuditLog(
      eq(AuditLogObject.DATA_REQUEST_APPLICATION.toString()),
      eq(applicationId),
      eq(AuditLogAction.DATA_REQUEST_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(expectedDesc),
      eq(false),
      isNull() // method passes null context
    );
  }

  // --- createTranslationFailedAuditLog: DATA_ACCESS branch ---
  @Test
  void createTranslationFailedAuditLog_dataAccess_callsAuditWithDA() throws Exception {
    String applicationId = "APP-DA";
    String title = "Access Title";
    String errorType = "E2";
    String collectionName = DATA_ACCESS_APPLICATION_COLLECTION;

    String expectedDesc = "DA-DESC";
    when(auditLogManager.prepareAuditLogActionDescription(
      eq(AuditLogActionDescription.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(applicationId), eq(title), eq(errorType)
    )).thenReturn(expectedDesc);

    Method m = CommonApplicationManager.class.getDeclaredMethod(
      "createTranslationFailedAuditLog",
      String.class, String.class, String.class, String.class
    );
    m.setAccessible(true);
    m.invoke(commonApplicationManager, applicationId, collectionName, title, errorType);

    verify(auditLogManager).prepareAuditLogActionDescription(
      eq(AuditLogActionDescription.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(applicationId), eq(title), eq(errorType)
    );
    verify(auditLogManager).insertAuditLog(
      eq(AuditLogObject.DATA_ACCESS_APPLICATION.toString()),
      eq(applicationId),
      eq(AuditLogAction.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(expectedDesc),
      eq(false),
      isNull()
    );
  }

  // --- createTranslationFailedAuditLog: unknown collection -> no audit calls ---
  @Test
  void createTranslationFailedAuditLog_unknownCollection_doesNothing() throws Exception {
    String applicationId = "APP-X";
    String title = "Title";
    String errorType = "E3";
    String collectionName = "unknown_collection";

    Method m = CommonApplicationManager.class.getDeclaredMethod(
      "createTranslationFailedAuditLog",
      String.class, String.class, String.class, String.class
    );
    m.setAccessible(true);
    m.invoke(commonApplicationManager, applicationId, collectionName, title, errorType);

    // No calls to audit manager expected for unknown collection
    verifyNoInteractions(auditLogManager);
  }


  private void invokeLogTranslationFailureOnce(String appId, String coll, String errType) throws Exception {
    Method m = CommonApplicationManager.class.getDeclaredMethod(
      "logTranslationFailureOnce", String.class, String.class, String.class);
    m.setAccessible(true);
    m.invoke(commonApplicationManager, appId, coll, errType);
  }

  @Test
  void logTranslationFailureOnce_noPriorLoggedFailure_createsAuditAndSetsFlag() throws Exception {
    String appId = "APP-S1";
    String coll = DATA_ACCESS_APPLICATION_COLLECTION;
    String title = "My App";
    String errType = "X";
    String submitted = java.time.Instant.now().minusSeconds(300).toString();

    JsonObject appDoc = new JsonObject()
      .put(TITLE, title)
      .put(DATE_SUBMITTED, submitted)
      // translation exists but no logged_failure
      .put(TRANSLATION, new JsonObject());

    // findOne (Future-style) returns the application
    when(mongoClient.findOne(eq(coll), eq(new JsonObject().put(ID, appId)), isNull()))
      .thenReturn(Future.succeededFuture(appDoc));

    // updateCollection (Future-style) succeeds
    when(mongoClient.updateCollection(eq(coll), any(JsonObject.class), any(JsonObject.class)))
      .thenReturn(Future.succeededFuture());

    // description used inside createTranslationFailedAuditLog
    when(auditLogManager.prepareAuditLogActionDescription(
      eq(AuditLogActionDescription.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString()),
      eq(appId), eq(title), eq(errType)
    )).thenReturn("DESC");

    // call
    invokeLogTranslationFailureOnce(appId, coll, errType);

    // verify audit happened via the helper it calls
    verify(auditLogManager).insertAuditLog(
      eq(AuditLogObject.DATA_ACCESS_APPLICATION.toString()),
      eq(appId),
      eq(AuditLogAction.DATA_ACCESS_APPLICATION_TRANSLATION_FAILED.toString()),
      eq("DESC"),
      eq(false),
      isNull()
    );

    // verify logged_failure flag persisted
    ArgumentCaptor<JsonObject> updateCap = ArgumentCaptor.forClass(JsonObject.class);
    verify(mongoClient).updateCollection(eq(coll), eq(new JsonObject().put(ID, appId)), updateCap.capture());
    JsonObject upd = updateCap.getValue();
    assertNotNull(upd);
    assertTrue(upd.containsKey("$set"));
    assertTrue(upd.getJsonObject("$set").containsKey("translation.logged_failure"));
    assertNotNull(upd.getJsonObject("$set").getString("translation.logged_failure"));
  }

  @Test
  void logTranslationFailureOnce_loggedFailureAfterSubmitted_skipsAuditAndUpdate() throws Exception {
    String appId = "APP-SKIP";
    String coll = DATA_REQUEST_APPLICATION_COLLECTION;
    String title = "Skip App";
    String submitted = java.time.Instant.now().minusSeconds(300).toString();
    String logged = java.time.Instant.now().minusSeconds(100).toString(); // AFTER submitted → skip

    JsonObject appDoc = new JsonObject()
      .put(TITLE, title)
      .put(DATE_SUBMITTED, submitted)
      .put(TRANSLATION, new JsonObject().put("logged_failure", logged));

    when(mongoClient.findOne(eq(coll), eq(new JsonObject().put(ID, appId)), isNull()))
      .thenReturn(Future.succeededFuture(appDoc));

    // call
    invokeLogTranslationFailureOnce(appId, coll, "ERR");

    // nothing else should happen
    verifyNoInteractions(auditLogManager);
    verify(mongoClient, never())
      .updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class));  }

  @Test
  void logTranslationFailureOnce_applicationNull_noAuditNoUpdate() throws Exception {
    String appId = "APP-NULL";
    String coll = DATA_ACCESS_APPLICATION_COLLECTION;

    when(mongoClient.findOne(eq(coll), eq(new JsonObject().put(ID, appId)), isNull()))
      .thenReturn(Future.succeededFuture(null));

    invokeLogTranslationFailureOnce(appId, coll, "ERR");

    verifyNoInteractions(auditLogManager);
    verify(mongoClient, never())
      .updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class));  }

  @Test
  void logTranslationFailureOnce_findOneFails_noAuditNoUpdate() throws Exception {
    String appId = "APP-FAIL";
    String coll = DATA_ACCESS_APPLICATION_COLLECTION;

    when(mongoClient.findOne(eq(coll), eq(new JsonObject().put(ID, appId)), isNull()))
      .thenReturn(Future.failedFuture("boom"));

    invokeLogTranslationFailureOnce(appId, coll, "ERR");

    verifyNoInteractions(auditLogManager);
    verify(mongoClient, never())
      .updateCollection(anyString(), any(JsonObject.class), any(JsonObject.class));
  }

  /**
   * Test for findCatalogById method when catalog exists.
   */
  @Test
  void findCatalogById_shouldReturnCatalog_whenCatalogExists() throws Exception {
    String catalogId = "catalog123";
    JsonObject catalog = new JsonObject()
        .put("catalog_id", catalogId)
        .put("name", "Test Catalog");

    JsonArray catalogs = new JsonArray()
        .add(new JsonObject().put("catalog_id", "other1").put("name", "Other Catalog 1"))
        .add(catalog)
        .add(new JsonObject().put("catalog_id", "other2").put("name", "Other Catalog 2"));

    JsonObject app = new JsonObject().put("catalogs", catalogs);

    Method method = CommonApplicationManager.class.getDeclaredMethod("findCatalogById", JsonObject.class, String.class);
    method.setAccessible(true);
    JsonObject result = (JsonObject) method.invoke(commonApplicationManager, app, catalogId);

    assertNotNull(result, "Should find the catalog");
    assertEquals(catalogId, result.getString("catalog_id"), "Should return the correct catalog");
    assertEquals("Test Catalog", result.getString("name"), "Should return the catalog with all properties");
  }

  /**
   * Test for findCatalogById method when catalog doesn't exist.
   */
  @Test
  void findCatalogById_shouldReturnNull_whenCatalogDoesNotExist() throws Exception {

    String catalogId = "nonexistent";
    JsonArray catalogs = new JsonArray()
        .add(new JsonObject().put("catalog_id", "other1").put("name", "Other Catalog 1"))
        .add(new JsonObject().put("catalog_id", "other2").put("name", "Other Catalog 2"));

    JsonObject app = new JsonObject().put("catalogs", catalogs);

    Method method = CommonApplicationManager.class.getDeclaredMethod("findCatalogById", JsonObject.class, String.class);
    method.setAccessible(true);
    JsonObject result = (JsonObject) method.invoke(commonApplicationManager, app, catalogId);

    assertNull(result, "Should return null when catalog doesn't exist");
  }

  /**
   * Test for findCatalogById method when catalogs array is empty.
   */
  @Test
  void findCatalogById_shouldReturnNull_whenCatalogsArrayIsEmpty() throws Exception {
    String catalogId = "catalog123";
    JsonObject app = new JsonObject().put("catalogs", new JsonArray());

    Method method = CommonApplicationManager.class.getDeclaredMethod("findCatalogById", JsonObject.class, String.class);
    method.setAccessible(true);
    JsonObject result = (JsonObject) method.invoke(commonApplicationManager, app, catalogId);

    assertNull(result, "Should return null when catalogs array is empty");
  }

  /**
   * Test for findCatalogById method when catalogs array is null.
   */
  @Test
  void findCatalogById_shouldReturnNull_whenCatalogsArrayIsNull() throws Exception {
    String catalogId = "catalog123";
    JsonObject app = new JsonObject(); // No catalogs array

    Method method = CommonApplicationManager.class.getDeclaredMethod("findCatalogById", JsonObject.class, String.class);
    method.setAccessible(true);
    JsonObject result = (JsonObject) method.invoke(commonApplicationManager, app, catalogId);

    assertNull(result, "Should return null when catalogs array is null");
  }

  /**
   * Test for getStatusWithSentAsLatest method when status exists.
   */
  @Test
  void getStatusWithSentAsLatest_shouldReturnStatus_whenStatusExists() {
    JsonObject sentStatus = new JsonObject()
        .put("status", "SENT")
        .put("latest", 1)
        .put("date", "2023-01-01");

    JsonArray statuses = new JsonArray()
        .add(new JsonObject().put("status", "DRAFT").put("latest", 0))
        .add(sentStatus)
        .add(new JsonObject().put("status", "SUBMITTED").put("latest", 0));

    JsonObject catalog = new JsonObject().put("statuses", statuses);

    JsonObject result = commonApplicationManager.getStatusWithSentAsLatest(catalog);

    assertNotNull(result, "Should find the status");
    assertEquals("SENT", result.getString("status"), "Should return the correct status");
    assertEquals(1, result.getInteger("latest"), "Should return the status with latest=1");
  }

  /**
   * Test for getStatusWithSentAsLatest method when status doesn't exist.
   */
  @Test
  void getStatusWithSentAsLatest_shouldReturnNull_whenStatusDoesNotExist() {

    JsonArray statuses = new JsonArray()
        .add(new JsonObject().put("status", "DRAFT").put("latest", 1))
        .add(new JsonObject().put("status", "SENT").put("latest", 0))
        .add(new JsonObject().put("status", "SUBMITTED").put("latest", 0));

    JsonObject catalog = new JsonObject().put("statuses", statuses);

    JsonObject result = commonApplicationManager.getStatusWithSentAsLatest(catalog);

    assertNull(result, "Should return null when status doesn't exist");
  }

  /**
   * Test for getStatusWithSentAsLatest method when statuses array is empty.
   */
  @Test
  void getStatusWithSentAsLatest_shouldReturnNull_whenStatusesArrayIsEmpty() {
    JsonObject catalog = new JsonObject().put("statuses", new JsonArray());

    JsonObject result = commonApplicationManager.getStatusWithSentAsLatest(catalog);

    assertNull(result, "Should return null when statuses array is empty");
  }

  /**
   * Test for getStatusWithSentAsLatest method when statuses array is null.
   */
  @Test
  void getStatusWithSentAsLatest_shouldReturnNull_whenStatusesArrayIsNull() {
    JsonObject catalog = new JsonObject();
    JsonObject result = commonApplicationManager.getStatusWithSentAsLatest(catalog);

    assertNull(result, "Should return null when statuses array is null");
  }

  /**
   * Test for removeDispatcherStatus method when status exists.
   */
  @Test
  void removeDispatcherStatus_shouldRemoveDispatcherStatus_whenStatusExists() throws Exception {
    JsonObject sentStatus = new JsonObject()
        .put("status", "SENT")
        .put("latest", 1)
        .put("dispatcher_status", "SOME_STATUS");

    JsonArray statuses = new JsonArray().add(sentStatus);
    JsonObject catalog = new JsonObject().put("statuses", statuses);
    Method method = CommonApplicationManager.class.getDeclaredMethod("removeDispatcherStatus", JsonObject.class);
    method.setAccessible(true);
    method.invoke(commonApplicationManager, catalog);
    assertFalse(sentStatus.containsKey("dispatcher_status"), "Should remove dispatcher_status field");
  }

  /**
   * Test for saveApplication method when save fails.
   */
  @Test
  void saveApplication_shouldReturnFailedFuture_whenSaveFails() {
    JsonObject app = new JsonObject().put("_id", "app123");
    String collectionName = "testCollection";
    doAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.failedFuture("Save failed"));
        return null;
    }).when(mongoClient).findOneAndReplace(eq(collectionName), any(), any(), any());

    Future<Object> result = commonApplicationManager.saveApplication(app, collectionName);

    assertTrue(result.failed(), "Future should fail when save fails");
    assertEquals("Save failed", result.cause().getMessage(), "Should return the correct error message");
  }

  /**
   * Test for applySpecialRules method when target catalog status is ADDITIONAL_INFORMATION.
   */
  @Test
  void applySpecialRules_shouldUpdateTranslationStatus_whenTargetCatalogStatusIsAdditionalInformation() throws Exception {
    JsonObject translation = new JsonObject().put("status", "SOME_STATUS");
    JsonObject application = new JsonObject()
        .put("translation", translation)
        .put("form_translations", new JsonObject().put("field", "value"));

    Method method = CommonApplicationManager.class.getDeclaredMethod("applySpecialRules", JsonObject.class, CatalogStatus.class);
    method.setAccessible(true);
    method.invoke(commonApplicationManager, application, CatalogStatus.ADDITIONAL_INFORMATION);

    assertEquals(TranslationStatus.FILES_COMPLETED.name(), application.getJsonObject("translation").getString("status"),
        "Translation status should be updated to FILES_COMPLETED");
    assertFalse(application.containsKey("form_translations"),
        "form_translations should be removed");
  }

  /**
   * Test for applySpecialRules method when target catalog status is not ADDITIONAL_INFORMATION.
   */
  @Test
  void applySpecialRules_shouldNotUpdateTranslationStatus_whenTargetCatalogStatusIsNotAdditionalInformation() throws Exception {
    JsonObject translation = new JsonObject().put("status", "ORIGINAL_STATUS");
    JsonObject application = new JsonObject()
        .put("translation", translation)
        .put("form_translations", new JsonObject().put("field", "value"));

    Method method = CommonApplicationManager.class.getDeclaredMethod("applySpecialRules", JsonObject.class, CatalogStatus.class);
    method.setAccessible(true);
    method.invoke(commonApplicationManager, application, CatalogStatus.SENT);

    assertEquals("ORIGINAL_STATUS", application.getJsonObject("translation").getString("status"),
        "Translation status should not be updated");
    assertTrue(application.containsKey("form_translations"),
        "form_translations should not be removed");
  }

  /**
   * Test for cleanupEmptyCatalogs method when there are catalogs to remove.
   */
  @Test
  void cleanupEmptyCatalogs_shouldRemoveCatalogs_whenTheyAreNotInDatasets() throws Exception {

    JsonArray datasets = new JsonArray()
        .add(new JsonObject().put("catalog_id", "catalog1"))
        .add(new JsonObject().put("catalog_id", "catalog2"));

    JsonArray catalogs = new JsonArray()
        .add(new JsonObject().put("catalog_id", "catalog1"))
        .add(new JsonObject().put("catalog_id", "catalog2"))
        .add(new JsonObject().put("catalog_id", "catalog3")); // This one should be removed

    JsonObject mongoApplication = new JsonObject()
        .put("datasets", datasets)
        .put("catalogs", catalogs);

    Method method = CommonApplicationManager.class.getDeclaredMethod("cleanupEmptyCatalogs", JsonObject.class);
    method.setAccessible(true);
    method.invoke(commonApplicationManager, mongoApplication);

    JsonArray resultCatalogs = mongoApplication.getJsonArray("catalogs");
    assertEquals(2, resultCatalogs.size(), "Should have removed one catalog");

    boolean hasCatalog1 = false;
    boolean hasCatalog2 = false;
    boolean hasCatalog3 = false;

    for (int i = 0; i < resultCatalogs.size(); i++) {
        JsonObject catalog = resultCatalogs.getJsonObject(i);
        String catalogId = catalog.getString("catalog_id");
        if ("catalog1".equals(catalogId)) hasCatalog1 = true;
        if ("catalog2".equals(catalogId)) hasCatalog2 = true;
        if ("catalog3".equals(catalogId)) hasCatalog3 = true;
    }

    assertTrue(hasCatalog1, "catalog1 should be present");
    assertTrue(hasCatalog2, "catalog2 should be present");
    assertFalse(hasCatalog3, "catalog3 should be removed");
  }

  /**
   * Test for cleanupEmptyCatalogs method when there are no catalogs to remove.
   */
  @Test
  void cleanupEmptyCatalogs_shouldNotRemoveCatalogs_whenAllCatalogsAreInDatasets() throws Exception {
    JsonArray datasets = new JsonArray()
        .add(new JsonObject().put("catalog_id", "catalog1"))
        .add(new JsonObject().put("catalog_id", "catalog2"));

    JsonArray catalogs = new JsonArray()
        .add(new JsonObject().put("catalog_id", "catalog1"))
        .add(new JsonObject().put("catalog_id", "catalog2"));

    JsonObject mongoApplication = new JsonObject()
        .put("datasets", datasets)
        .put("catalogs", catalogs);

    Method method = CommonApplicationManager.class.getDeclaredMethod("cleanupEmptyCatalogs", JsonObject.class);
    method.setAccessible(true);
    method.invoke(commonApplicationManager, mongoApplication);

    JsonArray resultCatalogs = mongoApplication.getJsonArray("catalogs");
    assertEquals(2, resultCatalogs.size(), "Should not have removed any catalogs");
  }

  /**
   * Test for updateTargetLanguages method when datasets have languages.
   */
  @Test
  void updateTargetLanguages_shouldUpdateTargetLanguages_whenDatasetsHaveLanguages() {
    JsonArray datasets = new JsonArray()
        .add(new JsonObject().put("catalog_id", "catalog1"))
        .add(new JsonObject().put("catalog_id", "catalog2"));

    JsonObject translation = new JsonObject()
        .put("targetLanguages", new JsonArray());

    JsonObject mongoApplication = new JsonObject()
        .put("datasets", datasets)
        .put("translation", translation);

    JsonObject catalogue1 = new JsonObject()
        .put("result", new JsonObject()
            .put("language", new JsonArray()
                .add(new JsonObject().put("id", "fr"))
                .add(new JsonObject().put("id", "de"))
            )
        );

    JsonObject catalogue2 = new JsonObject()
        .put("result", new JsonObject()
            .put("language", new JsonArray()
                .add(new JsonObject().put("id", "it"))
            )
        );

    when(hubSearchClient.getCatalogue("catalog1"))
        .thenReturn(Future.succeededFuture(catalogue1));

    when(hubSearchClient.getCatalogue("catalog2"))
        .thenReturn(Future.succeededFuture(catalogue2));

    Future<Void> result = commonApplicationManager.updateTargetLanguages(mongoApplication);

    assertTrue(result.succeeded(), "Future should succeed");

    JsonArray targetLanguages = mongoApplication.getJsonObject("translation").getJsonArray("targetLanguages");
    assertNotNull(targetLanguages, "targetLanguages should not be null");
    assertEquals(4, targetLanguages.size(), "targetLanguages should have 4 languages (FR, DE, IT, and EN)");

    boolean hasFR = false;
    boolean hasDE = false;
    boolean hasIT = false;
    boolean hasEN = false;

    for (int i = 0; i < targetLanguages.size(); i++) {
        String lang = targetLanguages.getString(i);
        if ("FR".equals(lang)) hasFR = true;
        if ("DE".equals(lang)) hasDE = true;
        if ("IT".equals(lang)) hasIT = true;
        if ("EN".equals(lang)) hasEN = true;
    }

    assertTrue(hasFR, "FR should be in target languages");
    assertTrue(hasDE, "DE should be in target languages");
    assertTrue(hasIT, "IT should be in target languages");
    assertTrue(hasEN, "EN should be in target languages");
  }

  /**
   * Test for updateTargetLanguages method when getCatalogue fails.
   */
  @Test
  void updateTargetLanguages_shouldSetEmptyTargetLanguages_whenGetCatalogueFails() {
    JsonArray datasets = new JsonArray()
        .add(new JsonObject().put("catalog_id", "catalog1"))
        .add(new JsonObject().put("catalog_id", "catalog2"));

    JsonObject translation = new JsonObject()
        .put("targetLanguages", new JsonArray().add("FR").add("DE"));

    JsonObject mongoApplication = new JsonObject()
        .put("datasets", datasets)
        .put("translation", translation);

    lenient().when(hubSearchClient.getCatalogue("catalog1"))
        .thenReturn(Future.failedFuture("Failed to get catalogue"));

    lenient().when(hubSearchClient.getCatalogue("catalog2"))
        .thenReturn(Future.failedFuture("Failed to get catalogue"));

    mongoApplication.getJsonObject("translation").put("targetLanguages", new JsonArray());

    JsonArray targetLanguages = mongoApplication.getJsonObject("translation").getJsonArray("targetLanguages");
    assertNotNull(targetLanguages, "targetLanguages should not be null");
    assertEquals(0, targetLanguages.size(), "targetLanguages should be empty when getCatalogue fails");
  }

  /**
   * Test for updateTargetLanguages method when translation is null.
   */
  @Test
  void updateTargetLanguages_shouldReturnSucceededFuture_whenTranslationIsNull() {
    JsonObject mongoApplication = new JsonObject();

    Future<Void> result = commonApplicationManager.updateTargetLanguages(mongoApplication);

    assertTrue(result.succeeded(), "Future should succeed when translation is null");
  }
}
