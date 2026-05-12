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

import static eu.europa.data.health.access.Constants.COLLECTION_SCHEDULERS_STATUS;
import static eu.europa.data.health.access.Constants.FIELD_SCHEDULER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

  private static final String SCHEDULER_NAME = "withdrawScheduler";

  @Mock
  private DBHandler dbHandler;

  @Captor
  private ArgumentCaptor<JsonObject> jsonCaptor;

  @InjectMocks
  private SchedulerService schedulerService;

  @Test
  void createNewRecordForScheduler_createsExpectedDocument() {
    when(dbHandler.create(eq(COLLECTION_SCHEDULERS_STATUS), jsonCaptor.capture())).thenReturn(
      Future.succeededFuture("scheduler-record-id")
    );

    Future<String> result = schedulerService.createNewRecordForScheduler(SCHEDULER_NAME);

    assertTrue(result.succeeded());
    assertEquals("scheduler-record-id", result.result());

    JsonObject createdRecord = jsonCaptor.getValue();
    assertEquals(SCHEDULER_NAME, createdRecord.getString(FIELD_SCHEDULER_NAME));
    assertEquals("0", createdRecord.getString("is_running"));

    verify(dbHandler).create((COLLECTION_SCHEDULERS_STATUS), (createdRecord));
  }

  @Test
  void updateSchedulerStatus_updatesRunningFlagAndTimestamp() {
    JsonObject updatedRecord = new JsonObject()
      .put(FIELD_SCHEDULER_NAME, SCHEDULER_NAME)
      .put("is_running", "1")
      .put("status_changed_at", Instant.now().toString());

    when(
      dbHandler.updateByField(
        eq(COLLECTION_SCHEDULERS_STATUS),
        jsonCaptor.capture(),
        eq(FIELD_SCHEDULER_NAME),
        eq(SCHEDULER_NAME)
      )
    ).thenReturn(Future.succeededFuture(updatedRecord));

    Future<JsonObject> result = schedulerService.updateSchedulerStatus(SCHEDULER_NAME, "1");

    assertTrue(result.succeeded());
    assertEquals(updatedRecord, result.result());

    JsonObject updateRecord = jsonCaptor.getValue();
    assertEquals("1", updateRecord.getString("is_running"));
    assertNotNull(updateRecord.getString("status_changed_at"));
    assertNotNull(Instant.parse(updateRecord.getString("status_changed_at")));

    verify(dbHandler).updateByField(
      (COLLECTION_SCHEDULERS_STATUS),
      (updateRecord),
      (FIELD_SCHEDULER_NAME),
      (SCHEDULER_NAME)
    );
  }
}
