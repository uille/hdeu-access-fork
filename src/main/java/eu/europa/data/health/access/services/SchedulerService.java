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

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class SchedulerService extends AbstractManager {


  public SchedulerService(DBHandler dbHandler) {
    super(dbHandler);
  }


  /**
   * Creates a document for the Entity collection
   *
   * @param schedulerName the Scheduler Name
   */
  public Future<String> createNewRecordForScheduler( String schedulerName) {
    JsonObject newRecord = new JsonObject();
    newRecord.put(FIELD_SCHEDULER_NAME, schedulerName);
    newRecord.put("is_running", "0");
    return dbHandler.create(COLLECTION_SCHEDULERS_STATUS, newRecord);
  }


  public Future<JsonObject> updateSchedulerStatus(String schedulerName, String isRunning) {

    JsonObject updateRecord = new JsonObject().put("is_running", isRunning)
        .put("status_changed_at", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

    return dbHandler.updateByField(COLLECTION_SCHEDULERS_STATUS, updateRecord, FIELD_SCHEDULER_NAME,
        schedulerName);
  }

}
