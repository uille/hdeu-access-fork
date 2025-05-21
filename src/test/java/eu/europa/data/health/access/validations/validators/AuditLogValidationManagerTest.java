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
package eu.europa.data.health.access.validations.validators;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.services.HubSearchClient;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.RequestBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogValidationManagerTest {

  @Mock
  RoutingContext context;

  @Mock
  RequestBody body;

  @Mock
  Vertx vertx;

  @Mock
  HubSearchClient hubSearchClient;

  JsonObject config;
  AuditLogValidationManager manager;

  @BeforeEach
  void setUp() {
    config = new JsonObject().put(Constants.AUDIT_LOG_DATASET_SEARCH_INTERVAL, 1L)
      .put(Constants.AUDIT_LOG_DATASET_MAX_RETRIES, 3);
    manager = new AuditLogValidationManager(config, hubSearchClient);
  }

  @Test
  void shouldSkipValidationForDeleteAction() {
    JsonObject deleteBody = new JsonObject().put("datasetAction", "delete");
    when(body.asJsonObject()).thenReturn(deleteBody);
    when(context.body()).thenReturn(body);

    manager.validateDatasetExists(context);

    verify(context).next(); // skipped due to "delete"
  }

  @Test
  void shouldValidateDatasetExistsSuccessfully() {
    JsonObject validBody = new JsonObject().put("datasetId", "ds123");
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(validBody);

    JsonObject result = new JsonObject().put("result", new JsonObject());
    Promise<JsonObject> promise = Promise.promise();
    promise.complete(result);
    when(hubSearchClient.getDataset("ds123")).thenReturn(promise.future());

    manager.validateDatasetExists(context);

    verify(context).next();
  }

  @Test
  void shouldTriggerRetryOnDatasetNotFound() {
    JsonObject requestBody = new JsonObject().put("datasetId", "missingId");
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(requestBody);
    when(context.vertx()).thenReturn(vertx);

    Promise<JsonObject> failedPromise = Promise.promise();
    failedPromise.fail("not found");
    when(hubSearchClient.getDataset("missingId")).thenReturn(failedPromise.future());

    AuditLogValidationManager spyManager = Mockito.spy(manager);

    spyManager.validateDatasetExists(context);

    verify(hubSearchClient).getDataset("missingId");
    verify(vertx, atLeastOnce()).setTimer(anyLong(), any());
  }


  @Test
  void shouldPassValidationWhenAllFieldsValid() {
    when(context.body()).thenReturn(body);
    JsonObject validJson = new JsonObject().put("action", "view")
      .put("actionDescription", "description").put("ecasId", "ecas123").put("firstName", "John")
      .put("lastName", "Doe").put("objectId", "obj-123").put("logDateFrom", "2024-01-01")
      .put("logDateTo", "2024-12-31");

    when(body.asJsonObject()).thenReturn(validJson);

    manager.validateFilters(context);

    verify(context).next();
  }

  @Test
  void shouldFailValidationWhenFieldsExceedLimit() {
    when(context.body()).thenReturn(body);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);
    JsonObject invalidJson = new JsonObject().put("action", "a".repeat(51))
      .put("actionDescription", "b".repeat(51)).put("ecasId", "c".repeat(51))
      .put("firstName", "d".repeat(51)).put("lastName", "e".repeat(51))
      .put("objectId", "f".repeat(37));

    when(body.asJsonObject()).thenReturn(invalidJson);

    manager.validateFilters(context);

    verify(context, never()).next();
  }

  @Test
  void shouldFailValidationWhenDateFromAfterDateTo() {
    when(context.body()).thenReturn(body);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(400)).thenReturn(response);
    JsonObject invalidDateJson = new JsonObject().put("logDateFrom", "2024-12-31")
      .put("logDateTo", "2024-01-01");

    when(body.asJsonObject()).thenReturn(invalidDateJson);

    manager.validateFilters(context);

    verify(context, never()).next();
  }


}
