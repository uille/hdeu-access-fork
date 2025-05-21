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

import static eu.europa.data.health.access.Constants.BASKET_COLLECTION;
import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.DATASETS;
import static eu.europa.data.health.access.Constants.DATASET_ID;
import static eu.europa.data.health.access.Constants.DISTRIBUTIONS;
import static eu.europa.data.health.access.Constants.DISTRIBUTION_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import io.vertx.ext.web.FileUpload;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.handlers.DBHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;

import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CommonApplicationValidationManagerTest {

  @Mock
  DBHandler dbHandler;
  @Mock
  MongoClient mongoClient;
  @Mock
  RoutingContext routingContext;
  @Mock
  HttpServerResponse response;
  @Mock
  RequestBody requestBody;
  @InjectMocks
  CommonApplicationValidationManager manager;

  @BeforeEach
  void setUp() {
    manager = new CommonApplicationValidationManager(dbHandler, mongoClient);
  }

  @Test
  void validateCreate_validDTO_noTitleConflict_shouldProceed()   {
    String userId = "test-user";
    JsonArray jsonArrayDistributions = new JsonArray();
    jsonArrayDistributions.add(new JsonObject().put(DISTRIBUTION_ID, userId));

    JsonArray jsonArrayDatasets = new JsonArray();
    jsonArrayDatasets.add(new JsonObject().put(DISTRIBUTION_ID, userId).put(DATASET_ID, userId)
      .put(DISTRIBUTIONS, jsonArrayDistributions));

    JsonObject requestBodyJson = new JsonObject().put("title", "My Application Title")
      .put(DATASETS, jsonArrayDatasets).put(DATASET_ID, userId)
      .put(DISTRIBUTIONS, jsonArrayDistributions);

    // Setup mocks
    when(routingContext.body()).thenReturn(requestBody);
    when(routingContext.body().asJsonObject()).thenReturn(requestBodyJson);

    User user = mock(User.class);
    JsonObject principal = mock(JsonObject.class);

    when(mongoClient.findOne(anyString(), any(), any(), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    });

    List<JsonObject> mockResultList = new ArrayList<>();
    mockResultList.add(requestBodyJson);

    when(routingContext.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    when(principal.getString("preferred_username")).thenReturn("test-user@example.com");

    when(dbHandler.findAll(eq(BASKET_COLLECTION), any(), any(), any(), any(), any(),
      anyString())).thenReturn(Future.succeededFuture(mockResultList));

    // Run
    manager.validateCreate(routingContext, "application-collection");

    // Verify
    verify(routingContext, timeout(1000)).next();
  }


  @Test
  void validateCreate_validDTO_noTitleConflict_withViolations() {
    String userId = "test-user";
    JsonArray jsonArrayDatasets = new JsonArray();
    jsonArrayDatasets.add(new JsonObject().put(DISTRIBUTION_ID, userId).put(DATASET_ID, userId));

    JsonObject requestBodyJson = new JsonObject().put(DATASETS, jsonArrayDatasets)
      .put("datasetId", userId);

    // Setup mocks
    when(routingContext.body()).thenReturn(requestBody);
    when(routingContext.body().asJsonObject()).thenReturn(requestBodyJson);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    // Run
    manager.validateCreate(routingContext, "application-collection");

    //     Verify
    verify(routingContext, times(2)).body();

  }


  @Test
  void validateFile_validSingleFile_shouldCallNext() {
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.fileName()).thenReturn("file.pdf");
    List<FileUpload> fileList = new ArrayList<>();
    fileList.add(fileUpload);

    when(routingContext.fileUploads()).thenReturn(fileList);
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn(null); // uses FILE_UPLOAD_VALID_TYPES

    doNothing().when(routingContext).next();

    manager.validateFile(routingContext);

    verify(routingContext).next();
  }


  @Test
  void validateFile_validSingleFile_invalidFile() {
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.fileName()).thenReturn("file.rdf");
    List<FileUpload> fileList = new ArrayList<>();
    fileList.add(fileUpload);

    when(routingContext.fileUploads()).thenReturn(fileList);
    when(routingContext.pathParam(CATALOGUE_ID)).thenReturn(null); // uses FILE_UPLOAD_VALID_TYPES

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    manager.validateFile(routingContext);

    verify(routingContext).response();
  }


  @Test
  void validateFile_validSingleFile_noFiles() {
    List<FileUpload> fileList = new ArrayList<>();

    when(routingContext.fileUploads()).thenReturn(fileList);

    when(routingContext.response()).thenReturn(response);

    when(response.setStatusCode(anyInt())).thenReturn(response);

    manager.validateFile(routingContext);

    verify(routingContext).response();
  }

  @Test
  void validateFile_validSingleFile_multipleFile() {
    FileUpload fileUpload = mock(FileUpload.class);
    List<FileUpload> fileList = new ArrayList<>();
    fileList.add(fileUpload);
    fileList.add(fileUpload);

    when(routingContext.fileUploads()).thenReturn(fileList);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    manager.validateFile(routingContext);

    verify(routingContext).response();
  }


}
