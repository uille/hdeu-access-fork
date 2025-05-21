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

import eu.europa.data.health.access.services.HubSearchClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasketValidationManagerTest {

  @Mock
  HubSearchClient hubSearchClient;
  private BasketValidationManager validationManager;
  private RoutingContext context;
  private RequestBody body;
  @Mock
  private HttpServerResponse response;


  @BeforeEach
  void setUp() {
    context = mock(RoutingContext.class);
    body = mock(RequestBody.class);

    validationManager = new BasketValidationManager(hubSearchClient);
  }

  @Test
  void testValidateDatasetAndDistribution_Success() {
    String datasetId = "dataset-123";
    String distributionId = "dist-456";

    JsonObject inputBody = new JsonObject().put("datasetId", datasetId)
        .put("distributionId", distributionId);
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(inputBody);

    JsonObject publisher = new JsonObject().put("name", "My Publisher");
    JsonArray distributions = new JsonArray().add(new JsonObject().put("id", distributionId));
    JsonObject datasetResult = new JsonObject().put("publisher", publisher)
        .put("distributions", distributions);
    JsonObject dataset = new JsonObject().put("result", datasetResult);

    when(hubSearchClient.getDataset(any())).thenReturn(Future.succeededFuture(dataset));
    BasketValidationManager spyManager = Mockito.spy(validationManager);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);
    spyManager.validateDatasetAndDistributionExist(context);

    verify(context).put("publisher", "My Publisher");
  }


  @Test
  void testValidateDatasetAndDistribution_PublisherNotFound() {
    String datasetId = "dataset-123";
    String distributionId = "dist-456";

    JsonObject inputBody = new JsonObject().put("datasetId", datasetId)
        .put("distributionId", distributionId);
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(inputBody);

    JsonObject publisher = new JsonObject().put("name", "My Publisher");
    JsonArray distributions = new JsonArray().add(new JsonObject().put("id", distributionId));
    JsonObject datasetResult = new JsonObject().put("NotFound", publisher)
        .put("distributions", distributions);
    JsonObject dataset = new JsonObject().put("result", datasetResult);

    when(hubSearchClient.getDataset(any())).thenReturn(Future.succeededFuture(dataset));
    BasketValidationManager spyManager = Mockito.spy(validationManager);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);
    spyManager.validateDatasetAndDistributionExist(context);

    verify(context, times(2)).response();
    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }


  @Test
  void testValidateDatasetAndDistribution_ResultNotFound() {
    String datasetId = "dataset-123";
    String distributionId = "dist-456";

    JsonObject inputBody = new JsonObject().put("datasetId", datasetId)
        .put("distributionId", distributionId);
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(inputBody);

    JsonObject publisher = new JsonObject().put("name", "My Publisher");
    JsonArray distributions = new JsonArray().add(new JsonObject().put("id", distributionId));
    JsonObject datasetResult = new JsonObject().put("publisher", publisher)
        .put("distributions", distributions);
    JsonObject dataset = new JsonObject().put("NotFound", datasetResult);

    when(hubSearchClient.getDataset(any())).thenReturn(Future.succeededFuture(dataset));
    BasketValidationManager spyManager = Mockito.spy(validationManager);
    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);
    spyManager.validateDatasetAndDistributionExist(context);

    verify(context, times(2)).response();
    verify(response).setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
  }


  @Test
  void testValidateDatasetAndDistribution_DistributionNotFound() {
    String datasetId = "dataset-123";
    String distributionId = "dist-456";

    JsonObject inputBody = new JsonObject().put("datasetId", datasetId)
        .put("distributionId", distributionId);
    when(context.body()).thenReturn(body);
    when(body.asJsonObject()).thenReturn(inputBody);

    JsonObject publisher = new JsonObject().put("name", "My Publisher");
    JsonArray distributions = new JsonArray(); // empty
    JsonObject datasetResult = new JsonObject().put("publisher", publisher)
        .put("distributions", distributions);
    JsonObject dataset = new JsonObject().put("result", datasetResult);

    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);

    when(hubSearchClient.getDataset(any())).thenReturn(Future.succeededFuture(dataset));

    BasketValidationManager spyManager = spy(new BasketValidationManager(hubSearchClient));
    spyManager.validateDatasetAndDistributionExist(context);

    verify(context, never()).next();
  }

  @Test
  void testValidateDatasetAndDistribution_RequestFails() {
    String datasetId = "dataset-123";

    JsonObject inputBody = new JsonObject().put("datasetId", datasetId)
        .put("distributionId", "dist-456");

    when(body.asJsonObject()).thenReturn(inputBody);
    when(context.body()).thenReturn(body);

    when(hubSearchClient.getDataset(any())).thenReturn(Future.failedFuture("Something went wrong"));

    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);

    BasketValidationManager spyManager = spy(new BasketValidationManager(hubSearchClient));

    spyManager.validateDatasetAndDistributionExist(context);

    verify(context, never()).next();
  }


  @Test
  void testValidateBasketJsonSchema_ValidJson_ShouldCallNext() {
    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");

    when(context.response()).thenReturn(response);

    when(context.body()).thenReturn(body);
    when(context.body().asString()).thenReturn(buffer.toString());
    when(context.body().asString().trim()).thenReturn(buffer.toString());

    when(context.response()).thenReturn(response);
    when(context.response().setStatusCode(anyInt())).thenReturn(response);

    BasketValidationManager spyManager = spy(new BasketValidationManager(hubSearchClient));

    doReturn(false).when(spyManager).requestBodyIsEmpty(context);

    spyManager.validateBasketJsonSchema(context);

    verify(context, never()).next();

  }


}

