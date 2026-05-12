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
package eu.europa.data.health.access.manager;

import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION_ACCESS_SCHEDULER;
import static eu.europa.data.health.access.Constants.ADDITIONAL_INFORMATION_REQUEST_SCHEDULER;
import static eu.europa.data.health.access.Constants.ADDITIONAL_INFO_WITHDRAW_THRESHOLD_DAYS;
import static eu.europa.data.health.access.Constants.COLLECTION_SCHEDULERS_STATUS;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_DISPATCHER_DEFAULT_INTERVAL;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_DISPATCHER_INTERVAL;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_DISPATCHER_DEFAULT_INTERVAL;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_DISPATCHER_INTERVAL;
import static eu.europa.data.health.access.Constants.FIELD_SCHEDULER_NAME;
import static eu.europa.data.health.access.Constants.WITHDRAW_ACCESS_SCHEDULER;
import static eu.europa.data.health.access.Constants.WITHDRAW_REQUEST_SCHEDULER;
import static eu.europa.data.health.access.Constants.WITHDRAW_RUN_TIME;
import static eu.europa.data.health.access.Constants.WITHDRAW_THRESHOLD_DAYS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.NotificationManager;
import eu.europa.data.health.access.services.SchedulerService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SchedulerManagerTest {

  @Mock
  MongoClient mongoClient;

  @Mock
  Vertx vertx;

  @Mock
  SchedulerService schedulerService;

  @Mock
  DispatcherManager dispatcherManager;

  @Mock
  AuditLogManager auditLogManager;

  @Mock
  NotificationManager notificationManager;

  @Mock
  DBHandler dbHandler;

  @Mock
  DispatcherClient dispatcherClient;

  @InjectMocks
  private SchedulerManager schedulerManager;

  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  /** Returns a config with sensible defaults for withdraw-job tests. */
  private JsonObject baseWithdrawConfig() {
    return new JsonObject()
      .put(WITHDRAW_RUN_TIME, "06:00")
      .put(WITHDRAW_THRESHOLD_DAYS, 28)
      .put(ADDITIONAL_INFO_WITHDRAW_THRESHOLD_DAYS, 28);
  }

  /** Stubs the 2-arg Future-based mongoClient.find() (used by withdraw / additional-info jobs). */
  private void mockEmptyMongoFindFuture() {
    when(mongoClient.find(anyString(), any(JsonObject.class))).thenReturn(
      Future.succeededFuture(Collections.emptyList())
    );
  }

  /**
   * Stubs the 3-arg callback-based mongoClient.find() (used by dispatcher send / retrieve-sent
   * jobs).
   */
  private void mockEmptyMongoFindCallback() {
    doAnswer(inv -> {
      Handler<AsyncResult<List<JsonObject>>> h = inv.getArgument(2);
      h.handle(Future.succeededFuture(Collections.emptyList()));
      return null;
    })
      .when(mongoClient)
      .find(anyString(), any(JsonObject.class), any());
  }

  /** Returns a scheduler record that represents "not running". */
  private JsonObject notRunningRecord() {
    return new JsonObject()
      .put("is_running", "0")
      .put("status_changed_at", Instant.now().toString());
  }

  /**
   * Returns a scheduler record that represents "recently started / actively running" (< 30 min
   * ago).
   */
  private JsonObject activelyRunningRecord() {
    return new JsonObject()
      .put("is_running", "1")
      .put("status_changed_at", Instant.now().toString());
  }

  /**
   * Returns a scheduler record where is_running=1 but the timestamp is > 30 min ago, meaning the
   * scheduler crashed.
   */
  private JsonObject crashedRecord() {
    return new JsonObject()
      .put("is_running", "1")
      .put("status_changed_at", Instant.now().minusSeconds(35 * 60).toString());
  }

  // ── scheduleWithdrawApplicationJob — timer registration ───────────────────

  @Test
  void testScheduleWithdrawApplicationJob_registersExactlyFourTimers() {
    when(vertx.setTimer(anyLong(), any())).thenReturn(1L);

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    // Two withdraw timers + two additional-information timers
    verify(vertx, times(4)).setTimer(anyLong(), any());
  }

  @Test
  void testScheduleWithdrawApplicationJob_withEmptyConfig_usesDefaultsAndStillRegistersTimers() {
    when(vertx.setTimer(anyLong(), any())).thenReturn(1L);

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler,
      notificationManager
    );

    verify(vertx, times(4)).setTimer(anyLong(), any());
  }

  @Test
  void testScheduleWithdrawApplicationJob_withInvalidRunTime_fallsBackToDefaultAndRegistersTimers() {
    when(vertx.setTimer(anyLong(), any())).thenReturn(1L);
    JsonObject config = new JsonObject().put(WITHDRAW_RUN_TIME, "not-a-time");

    assertDoesNotThrow(() ->
      schedulerManager.scheduleWithdrawApplicationJob(
        dispatcherManager,
        auditLogManager,
        schedulerService,
        config,
        vertx,
        dbHandler,
        notificationManager
      )
    );

    verify(vertx, times(4)).setTimer(anyLong(), any());
  }

  @Test
  void testScheduleWithdrawApplicationJob_withBlankRunTime_fallsBackToDefaultAndRegistersTimers() {
    when(vertx.setTimer(anyLong(), any())).thenReturn(1L);
    JsonObject config = new JsonObject().put(WITHDRAW_RUN_TIME, "   ");

    assertDoesNotThrow(() ->
      schedulerManager.scheduleWithdrawApplicationJob(
        dispatcherManager,
        auditLogManager,
        schedulerService,
        config,
        vertx,
        dbHandler,
        notificationManager
      )
    );

    verify(vertx, times(4)).setTimer(anyLong(), any());
  }

  // ── scheduleWithdrawApplicationJob — timer fires (scheduler state logic) ──

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerNotRunning_proceedsAndSetsRunningState() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    // Fire the first registered timer
    captor.getAllValues().get(0).handle(1L);

    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerNotRunning_resetsStateAfterJobCompletes() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(0).handle(1L);

    // State must be set to RUNNING (1) and then back to NOT_RUNNING (0)
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("0"));
  }

  /**
   * Covers the {@code if (Boolean.TRUE.equals(isRunning))} skip branch for every withdraw timer:
   * <ul>
   *   <li>index 0 – WITHDRAW_ACCESS_SCHEDULER          (runWithdrawForCollection)</li>
   *   <li>index 2 – ADDITIONAL_INFORMATION_ACCESS_SCHEDULER  (runAdditionalInformationWithdrawForCollection)</li>
   *   <li>index 3 – ADDITIONAL_INFORMATION_REQUEST_SCHEDULER (runAdditionalInformationWithdrawForCollection)</li>
   * </ul>
   */
  @ParameterizedTest(name = "timer index {0} – scheduler actively running → job skipped")
  @ValueSource(ints = { 0, 2, 3 })
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerAlreadyRunning_skipsJob(int timerIndex) {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(activelyRunningRecord()))
    );

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(timerIndex).handle(1L);

    // Job is skipped → no state update at all
    verify(schedulerService, never()).updateSchedulerStatus(anyString(), anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerCrashed_treatsItAsNotRunningAndProceeds() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    // is_running=1 but timestamp is > 30 min old → treated as crashed / not running
    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(crashedRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(0).handle(1L);

    // The job ran even though is_running was "1" (it was considered crashed)
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerRecordNotFound_createsNewRecord() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Not Found")
    );
    when(schedulerService.createNewRecordForScheduler(anyString())).thenReturn(
      Future.succeededFuture("new-record-id")
    );
    // After "Not Found" the promise resolves with false → job still runs → these must be stubbed
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(0).handle(1L);

    verify(schedulerService, atLeastOnce()).createNewRecordForScheduler(anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerRecordCreationFails_completesGracefully() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Not Found")
    );
    when(schedulerService.createNewRecordForScheduler(anyString())).thenReturn(
      Future.failedFuture("Creation failed")
    );
    // Even when creation fails the promise resolves with false → job still runs → stub required
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(0).handle(1L));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_whenSchedulerCheckFailsWithNonNotFoundError_completesGracefully() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Unexpected DB error")
    );
    // isSchedulerRunningOrExpired always completes(false) even on generic errors → job runs
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(0).handle(1L));
    // A generic (non-"Not Found") error must NOT trigger record creation
    verify(schedulerService, never()).createNewRecordForScheduler(anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWithdrawJob_updateStateFailure_doesNotPreventReset() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    // First call (RUNNING) fails; second call (NOT_RUNNING reset) also succeeds
    when(schedulerService.updateSchedulerStatus(anyString(), eq("1"))).thenReturn(
      Future.failedFuture("DB write error")
    );
    when(schedulerService.updateSchedulerStatus(anyString(), eq("0"))).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(0).handle(1L));
  }

  // ── runAdditionalInformationWithdrawForCollection — timer fires ───────────
  //
  // scheduleWithdrawApplicationJob registers 4 timers in this order:
  //   [0] WITHDRAW_ACCESS_SCHEDULER         → runWithdrawForCollection
  //   [1] WITHDRAW_REQUEST_SCHEDULER        → runWithdrawForCollection
  //   [2] ADDITIONAL_INFORMATION_ACCESS_SCHEDULER  → runAdditionalInformationWithdrawForCollection
  //   [3] ADDITIONAL_INFORMATION_REQUEST_SCHEDULER → runAdditionalInformationWithdrawForCollection

  @Test
  @SuppressWarnings("unchecked")
  void testAdditionalInfoWithdrawJob_whenSchedulerNotRunning_setsRunningState() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    // withdrawCatalogsWithExpiredAdditionalInformation uses 2-arg Future-based find
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(2).handle(1L);

    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAdditionalInfoWithdrawJob_whenSchedulerNotRunning_resetsStateAfterCompletion() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(2).handle(1L);

    // Both RUNNING and NOT_RUNNING transitions must occur
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("0"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAdditionalInfoWithdrawJob_whenWithdrawFails_stillResetsStateToNotRunning() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    // Fail the underlying mongo query so withdrawCatalogsWithExpiredAdditionalInformation fails
    when(mongoClient.find(anyString(), any(JsonObject.class))).thenReturn(
      Future.failedFuture("DB error during additional-info withdraw")
    );

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(2).handle(1L));

    // Even on job failure the state must be reset to NOT_RUNNING
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("0"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAdditionalInfoWithdrawJob_whenSchedulerCrashed_treatsAsNotRunningAndProceeds() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    // is_running=1 but > 30 min ago → considered crashed
    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(crashedRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(2).handle(1L);

    // Crashed scheduler (> 30 min) is treated as not running → job proceeds
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAdditionalInfoWithdrawJob_whenSchedulerNotFound_createsRecordAndRunsJob() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Not Found")
    );
    when(schedulerService.createNewRecordForScheduler(anyString())).thenReturn(
      Future.succeededFuture("new-id")
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    captor.getAllValues().get(2).handle(1L);

    verify(schedulerService, atLeastOnce()).createNewRecordForScheduler(anyString());
    // After creation, isRunning = false → job still runs
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAdditionalInfoWithdrawJob_whenUpdateRunningStateFails_stillResetsState() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setTimer(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    // RUNNING update fails, NOT_RUNNING reset still attempted
    when(schedulerService.updateSchedulerStatus(anyString(), eq("1"))).thenReturn(
      Future.failedFuture("DB write error")
    );
    when(schedulerService.updateSchedulerStatus(anyString(), eq("0"))).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindFuture();

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(2).handle(1L));
  }

  // ── scheduleDispatcherJobs — periodic timer registration ──────────────────

  @Test
  void testScheduleDispatcherJobs_registersExactlyFourPeriodicTimers() {
    when(vertx.setPeriodic(anyLong(), any())).thenReturn(1L);

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    verify(vertx, times(4)).setPeriodic(anyLong(), any());
  }

  @Test
  void testScheduleDispatcherJobs_withCustomIntervals_usesCustomValuesForDataAccessTimers() {
    ArgumentCaptor<Long> intervalCaptor = ArgumentCaptor.forClass(Long.class);
    when(vertx.setPeriodic(intervalCaptor.capture(), any())).thenReturn(1L);

    JsonObject config = new JsonObject()
      .put(DATA_ACCESS_DISPATCHER_INTERVAL, 60_000)
      .put(DATA_REQUEST_DISPATCHER_INTERVAL, 90_000);

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      config,
      vertx,
      dbHandler
    );

    List<Long> intervals = intervalCaptor.getAllValues();
    assertTrue(intervals.contains(60_000L), "Custom DATA_ACCESS_DISPATCHER_INTERVAL must be used");
    assertTrue(intervals.contains(90_000L), "Custom DATA_REQUEST_DISPATCHER_INTERVAL must be used");
  }

  @Test
  void testScheduleDispatcherJobs_withDefaultConfig_usesBuiltInDefaultIntervals() {
    ArgumentCaptor<Long> intervalCaptor = ArgumentCaptor.forClass(Long.class);
    when(vertx.setPeriodic(intervalCaptor.capture(), any())).thenReturn(1L);

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    List<Long> intervals = intervalCaptor.getAllValues();
    assertTrue(intervals.contains((long) DATA_ACCESS_DISPATCHER_DEFAULT_INTERVAL));
    assertTrue(intervals.contains((long) DATA_REQUEST_DISPATCHER_DEFAULT_INTERVAL));
  }

  // ── scheduleDispatcherJobs — periodic timer fires (scheduler state logic) ─

  @Test
  @SuppressWarnings("unchecked")
  void testDispatcherJob_whenSchedulerNotRunning_proceedsAndSetsRunningState() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindCallback();

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    captor.getAllValues().get(0).handle(1L);

    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDispatcherJob_whenSchedulerActivelyRunning_skipsJob() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(activelyRunningRecord()))
    );

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    captor.getAllValues().get(0).handle(1L);

    verify(schedulerService, never()).updateSchedulerStatus(anyString(), anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDispatcherJob_whenSchedulerRecordNotFound_createsNewRecord() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Not Found")
    );
    when(schedulerService.createNewRecordForScheduler(anyString())).thenReturn(
      Future.succeededFuture("new-record-id")
    );
    // After "Not Found" the promise resolves with false → job still runs → these must be stubbed
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindCallback();

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    captor.getAllValues().get(0).handle(1L);

    verify(schedulerService, atLeastOnce()).createNewRecordForScheduler(anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDispatcherJob_whenSchedulerNotFound_createFails_completeGracefully() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Not Found")
    );
    when(schedulerService.createNewRecordForScheduler(anyString())).thenReturn(
      Future.failedFuture("Creation failed")
    );
    // Even when creation fails the promise resolves with false → job still runs → stub required
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindCallback();

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(0).handle(1L));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDispatcherJob_whenSchedulerCheckFailsWithGenericError_logsErrorAndSkipsJob() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.failedFuture("Some unexpected DB error")
    );
    // isSchedulerRunningOrExpired always completes(false) even on generic errors → job runs
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindCallback();

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    assertDoesNotThrow(() -> captor.getAllValues().get(0).handle(1L));
    // A generic (non-"Not Found") error must NOT trigger record creation
    verify(schedulerService, never()).createNewRecordForScheduler(anyString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDispatcherJob_resetsStateAfterJobCompletes() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(schedulerService.findByField(anyString(), anyString(), anyString())).thenReturn(
      Future.succeededFuture(List.of(notRunningRecord()))
    );
    when(schedulerService.updateSchedulerStatus(anyString(), anyString())).thenReturn(
      Future.succeededFuture(new JsonObject())
    );
    mockEmptyMongoFindCallback();

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    captor.getAllValues().get(0).handle(1L);

    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("1"));
    verify(schedulerService, atLeastOnce()).updateSchedulerStatus(anyString(), eq("0"));
  }

  // ── All four dispatcher scheduler names are used ──────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void testScheduleDispatcherJobs_allFourSchedulerNamesAreRegistered() {
    ArgumentCaptor<Handler<Long>> captor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<String> schedulerNameCaptor = ArgumentCaptor.forClass(String.class);
    when(vertx.setPeriodic(anyLong(), captor.capture())).thenReturn(1L);

    when(
      schedulerService.findByField(
        eq(FIELD_SCHEDULER_NAME),
        schedulerNameCaptor.capture(),
        eq(COLLECTION_SCHEDULERS_STATUS)
      )
    ).thenReturn(Future.succeededFuture(List.of(activelyRunningRecord())));

    schedulerManager.scheduleDispatcherJobs(
      dispatcherManager,
      dispatcherClient,
      auditLogManager,
      notificationManager,
      schedulerService,
      new JsonObject(),
      vertx,
      dbHandler
    );

    // Fire all four periodic handlers to populate captors
    for (Handler<Long> handler : captor.getAllValues()) {
      handler.handle(1L);
    }

    List<String> names = schedulerNameCaptor.getAllValues();
    assertTrue(names.contains("DATA_ACCESS_DISPATCHER"));
    assertTrue(names.contains("DATA_ACCESS_SENT_STATUS_APPLICATIONS"));
    assertTrue(names.contains("DATA_REQUEST_DISPATCHER"));
    assertTrue(names.contains("DATA_REQUEST_SENT_STATUS_APPLICATIONS"));
  }

  // ── Withdraw scheduler name coverage ──────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void testScheduleWithdrawApplicationJob_allFourSchedulerNamesAreRegistered() {
    ArgumentCaptor<Handler<Long>> timerCaptor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    when(vertx.setTimer(anyLong(), timerCaptor.capture())).thenReturn(1L);

    when(
      schedulerService.findByField(
        eq(FIELD_SCHEDULER_NAME),
        nameCaptor.capture(),
        eq(COLLECTION_SCHEDULERS_STATUS)
      )
    ).thenReturn(Future.succeededFuture(List.of(activelyRunningRecord())));

    schedulerManager.scheduleWithdrawApplicationJob(
      dispatcherManager,
      auditLogManager,
      schedulerService,
      baseWithdrawConfig(),
      vertx,
      dbHandler,
      notificationManager
    );

    for (Handler<Long> handler : timerCaptor.getAllValues()) {
      handler.handle(1L);
    }

    List<String> names = nameCaptor.getAllValues();
    assertTrue(names.contains(WITHDRAW_ACCESS_SCHEDULER));
    assertTrue(names.contains(WITHDRAW_REQUEST_SCHEDULER));
    assertTrue(names.contains(ADDITIONAL_INFORMATION_ACCESS_SCHEDULER));
    assertTrue(names.contains(ADDITIONAL_INFORMATION_REQUEST_SCHEDULER));
  }
}
