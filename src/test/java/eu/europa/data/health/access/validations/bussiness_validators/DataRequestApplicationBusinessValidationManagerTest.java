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
package eu.europa.data.health.access.validations.bussiness_validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.data_access.Section5BindToOtherSections;
import eu.europa.data.health.access.exceptions.FieldError;
import eu.europa.data.health.access.validations.business_validators.DataAccessApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.business_validators.DataRequestApplicationBusinessValidationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DataRequestApplicationBusinessValidationManagerTest {

  private DataRequestApplicationBusinessValidationManager validationManager;
  private MongoClient mongoClient;

  @Mock
  private RoutingContext mockContext;

  @Mock
  private HttpServerResponse response;

  @BeforeEach
  void setup() {
    mongoClient = mock(MongoClient.class);
    validationManager = new DataRequestApplicationBusinessValidationManager(mongoClient);
  }


  @Test
  void testSection1ValidationSucceedsWhenNoViolations_hasVars() {
    // Valid DTO with required field
    JsonObject legalOrNaturalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject().put("legalOrNaturalPerson", legalOrNaturalPerson);
    JsonObject section4 = new JsonObject().put("vatNumber", "test")
      .put("operatorIdentifier", "test").put("nameOfTheOrganisation", "test")
      .put("businessIdentifierOrganization", "test");

    JsonObject sectionForm = new JsonObject()
      .put("datasetVariables", new JsonArray()
        .add(new JsonObject()
          .put("datasetId", "DS1")
          .put("selected", new JsonArray().add("V1"))
        )
      );

    RequestBody requestBody = Mockito.mock(RequestBody.class);
    when(mockContext.body()).thenReturn(requestBody);
    when(mockContext.body().asJsonObject()).thenReturn(sectionForm);

    JsonArray jsonArrayDatasets = new JsonArray()
        .add(new JsonObject()
          .put("id", "DS1")
          .put("variables", new JsonArray()
            .add(new JsonObject().put("varId", "V1").put("name", "Age"))
            .add(new JsonObject().put("varId", "V2").put("name", "Gender"))
          )
        );

    JsonObject dbResult = new JsonObject()
      .put("datasets", jsonArrayDatasets)
      .put("sectionForm", sectionForm);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(dbResult);
      handler.handle(asyncResult);
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

     validationManager.section1Validation( mockContext);

  }



  @Test
  void testSection1ValidationSucceedsWhenNoViolations() {
    // Valid DTO with required field
    JsonObject legalOrNaturalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject().put("legalOrNaturalPerson", legalOrNaturalPerson);
    JsonObject section4 = new JsonObject().put("vatNumber", "test")
      .put("operatorIdentifier", "test").put("nameOfTheOrganisation", "test")
      .put("businessIdentifierOrganization", "test");

    JsonObject sectionForm = new JsonObject()
      .put("datasetVariables", new JsonArray()
        .add(new JsonObject()
          .put("datasetId", "DS1")
          .put("selected", new JsonArray().add("V1"))
        )
      );

    RequestBody requestBody = Mockito.mock(RequestBody.class);
    when(mockContext.body()).thenReturn(requestBody);
    when(mockContext.body().asJsonObject()).thenReturn(sectionForm);

    JsonArray jsonArrayDatasets = new JsonArray()
      .add(new JsonObject()
        .put("id", "DS2")
        .put("distributions_sample", new JsonArray()
          .add(new JsonObject().put("variables", new JsonArray()
          .add(new JsonObject().put("varId", "V1").put("name", "Age"))
          .add(new JsonObject().put("varId", "V2").put("name", "Gender"))
        )
      )));

    JsonObject dbResult = new JsonObject()
      .put("datasets", jsonArrayDatasets)
      .put("sectionForm", sectionForm);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(dbResult);
      handler.handle(asyncResult);
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    validationManager.section1Validation( mockContext);

  }

  @Test
  void testSection4ValidationFailsDueToConstraintViolation() {
    JsonObject legalOrNaturalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject().put("legalOrNaturalPerson", legalOrNaturalPerson);
    JsonObject section4 = new JsonObject();

    JsonObject form = new JsonObject().put("section3", section3).put("section4", section4);

    when(mockContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.end(anyString())).thenReturn(null);

    // Execute
    boolean result = validationManager.section4Validation(form, mockContext);

    // Verify
    verify(response).setStatusCode(403);
    verify(response).end(contains("Failed on section 4"));
    assertFalse(result);
  }

  @Test
  void testSection4ValidationSucceedsWhenNoViolations() {
    // Valid DTO with required field
    JsonObject legalOrNaturalPerson = new JsonObject().put("key", "a");
    JsonObject section3 = new JsonObject().put("legalOrNaturalPerson", legalOrNaturalPerson);
    JsonObject section4 = new JsonObject().put("vatNumber", "test")
      .put("operatorIdentifier", "test").put("nameOfTheOrganisation", "test")
      .put("businessIdentifierOrganization", "test");

    JsonObject form = new JsonObject().put("section3", section3).put("section4", section4);

    boolean result = validationManager.section4Validation(form, mockContext);
    assertTrue(result);
  }


  @Test
  void testSection4ValidationWithNoConstraintViolations() {
    // Arrange
    RoutingContext context = mock(RoutingContext.class);

    RequestBody requestBody = Mockito.mock(RequestBody.class);

    String applicationId = "test-app-id";
    when(context.pathParam("application_id")).thenReturn(applicationId);
    when(context.body()).thenReturn(requestBody);

    JsonObject section4BindSection3Json = new JsonObject().put("operatorIdentifier", "OP123456")
      .put("nameOfTheOrganisation", "Health Data Services Ltd.")
      .put("businessIdentifierOrganization", "BIZ-998877").put("vatNumber", "VAT998877ABC")
      .put("peppolCode", "PEPPOL-001122");

    when(context.body().asString()).thenReturn(section4BindSection3Json.toString());

    // Simulate Mongo DB result with legalOrNaturalPerson.key = "a"
    JsonObject dbForm = new JsonObject().put("section3",
      new JsonObject().put("legalOrNaturalPerson", new JsonObject().put("key", "a")));

    JsonObject dbResult = new JsonObject().put("form", dbForm);

    // Create a spy or mock of the validation manager with injected mock MongoClient
    DataAccessApplicationBusinessValidationManager manager = new DataAccessApplicationBusinessValidationManager(
      mongoClient);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(dbResult);
      handler.handle(asyncResult);
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    // Act
    manager.section4Validation(context);
    verify(context).next();

  }


  @Test
  void testSection4ValidationWithConstraintViolations() {
    // Arrange
    RoutingContext context = mock(RoutingContext.class);
    when(context.pathParam("section_number")).thenReturn("4");
    RequestBody requestBody = Mockito.mock(RequestBody.class);

    String applicationId = "test-app-id";
    when(context.pathParam("application_id")).thenReturn(applicationId);
    when(context.body()).thenReturn(requestBody);

    JsonObject section4BindSection3Json = new JsonObject().put("operatorIdentifier", "OP123456")
      .put("nameOfTheOrganisation", "Health Data Services Ltd.");

    when(context.body().asString()).thenReturn(section4BindSection3Json.toString());

    // Simulate Mongo DB result with legalOrNaturalPerson.key = "a"
    JsonObject dbForm = new JsonObject().put("section3",
      new JsonObject().put("legalOrNaturalPerson", new JsonObject().put("key", "a")));

    JsonObject dbResult = new JsonObject().put("form", dbForm);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(dbResult);
      handler.handle(asyncResult);
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    validationManager.businessValidateUpdate(context);

    verify(context).next();

  }


  @Test
  void testSection6Validation_Error() {
    RoutingContext context = mock(RoutingContext.class);
    RequestBody requestBody = Mockito.mock(RequestBody.class);

    when(context.pathParam("application_id")).thenReturn("app-123");

    JsonObject section6 = new JsonObject().put("whatIsTheFrequencyOfUpdates2",
      "Description of the data needed");

    JsonArray section6Countries = new JsonArray().add(section6);

    // Section JSONs
    JsonObject form = new JsonObject().put("section6Countries", section6Countries).put("section2",
      new JsonObject().put("purposeForWhichDataWillBeUsed",
        "[{\"key\":\"b\",\"value\":\"Research\"}]")).put("section3",
      new JsonObject().put("applyingForDataOnBehalfOfPublicSector", "")
        .put("legalOrNaturalPerson", "{\"code\":\"legal\",\"value\":\"Legal\"}")).put("section7",
      new JsonObject().put("willDataBeCombinedWithDataObtained",
        "{\"code\":\"combine\",\"value\":\"Yes\"}")).put("section5",
      new JsonObject().put("formatOfTheElectronicHealthData",
        "{\"code\":\"json\",\"value\":\"JSON\"}"));

    JsonObject dbResult = new JsonObject().put("form", form);
    when(context.body()).thenReturn(requestBody);
    when(context.body().asJsonArray()).thenReturn(form.getJsonArray("section6Countries"));

    when(mongoClient.findOne(anyString(), any(), any(), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(dbResult));
      return null;
    });

    validationManager.section6Validation(context);
    // Add assertions on context.next(), error handling etc. using ArgumentCaptor or verify
    verify(context).next();
  }


  @Test
  void testSection6Validation_shouldPassValidation() {
    RoutingContext context = mock(RoutingContext.class);
    RequestBody requestBody = Mockito.mock(RequestBody.class);

    when(context.pathParam("application_id")).thenReturn("app-123");

    // Section JSONs
    JsonObject form = new JsonObject().put("section2",
      new JsonObject().put("purposeForWhichDataWillBeUsed",
        "[{\"code\":\"purpose1\",\"value\":\"Research\"}]")).put("section3",
      new JsonObject().put("applyingForDataOnBehalfOfPublicSector",
          "{\"code\":\"yes\",\"value\":\"Yes\"}")
        .put("legalOrNaturalPerson", "{\"code\":\"legal\",\"value\":\"Legal\"}")).put("section7",
      new JsonObject().put("willDataBeCombinedWithDataObtained",
        "{\"code\":\"combine\",\"value\":\"Yes\"}")).put("section5",
      new JsonObject().put("formatOfTheElectronicHealthData",
        "{\"code\":\"json\",\"value\":\"JSON\"}")).put("test", new JsonArray());

    JsonObject dbResult = new JsonObject().put("form", form);
    when(context.body()).thenReturn(requestBody);
    when(context.body().asJsonArray()).thenReturn(form.getJsonArray("test"));

    when(mongoClient.findOne(anyString(), any(), any(), any())).thenAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(dbResult));
      return null;
    });

    validationManager.section6Validation(context);
    // Add assertions on context.next(), error handling etc. using ArgumentCaptor or verify
    verify(context).next();
  }


  @Test
  void testSection6Validation_shouldNotPassValidation() {
    RoutingContext context = mock(RoutingContext.class);

    when(context.pathParam("section_number")).thenReturn("8");

    validationManager.businessValidateUpdate(context);
    // Add assertions on context.next(), error handling etc. using ArgumentCaptor or verify
    verify(context).next();
  }


  @Test
  void testSection5Validation_withConstraintViolations() throws Exception {
    // Arrange
    RoutingContext context = mock(RoutingContext.class);
    RequestBody requestBody = mock(RequestBody.class);
    when(context.pathParam("section_number")).thenReturn("5");

    when(context.pathParam("application_id")).thenReturn("app-id-123");
    when(context.body()).thenReturn(requestBody);

    // Section 2: Mongo result (form.section2.purposeForWhichDataWillBeUsed)
    List<SelectObjectDTO> purposeList = List.of(new SelectObjectDTO("a", "desc"));
    ObjectMapper mapper = new ObjectMapper();
    String jsonList = mapper.writeValueAsString(purposeList);

    JsonObject dbResult = new JsonObject().put("form", new JsonObject().put("section2",
      new JsonObject().put("purposeForWhichDataWillBeUsed", jsonList)));

    // Section 5: request body to be validated
    Section5BindToOtherSections section5 = new Section5BindToOtherSections();

    String section5Body = mapper.writeValueAsString(section5);
    when(requestBody.asString()).thenReturn(section5Body);

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(dbResult);
      handler.handle(asyncResult);
      return null;
    }).when(mongoClient).findOne(any(), any(), isNull(), any());

    ArgumentCaptor<List> errorsCaptor = ArgumentCaptor.forClass(List.class);
    doNothing().when(context).next();

    // Act
    validationManager.businessValidateUpdate(context);

    // Assert
    verify(context, timeout(1000)).put(eq("errors"), errorsCaptor.capture());
    verify(context, timeout(1000)).next();

    List<FieldError> errors = errorsCaptor.getValue();
    assertNotNull(errors);
    assertFalse(errors.isEmpty());
  }


}

