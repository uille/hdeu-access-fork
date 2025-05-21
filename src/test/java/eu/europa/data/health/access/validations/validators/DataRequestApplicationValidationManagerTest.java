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

import static eu.europa.data.health.access.Constants.APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR;
import static eu.europa.data.health.access.Constants.CATALOGUE_ID;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY;
import static eu.europa.data.health.access.Constants.DATA_EXTRACTION_FREQUENCY_RELATIVES;
import static eu.europa.data.health.access.Constants.DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES;
import static eu.europa.data.health.access.Constants.DOMIBUS_ID;
import static eu.europa.data.health.access.Constants.FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_2;
import static eu.europa.data.health.access.Constants.HAS_THE_GROUP_RELATIVES_FORMED_3;
import static eu.europa.data.health.access.Constants.HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS;
import static eu.europa.data.health.access.Constants.NEED_FOR_DATA_EXTRACTION_EVERY;
import static eu.europa.data.health.access.Constants.OTHER_PERMIT_APPLICATIONS;
import static eu.europa.data.health.access.Constants.PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION;
import static eu.europa.data.health.access.Constants.THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY;
import static eu.europa.data.health.access.Constants.UPLOAD_RDF;
import static eu.europa.data.health.access.Constants.WHEN_DATA_NEEDED;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48;
import static eu.europa.data.health.access.Constants.WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B;
import static eu.europa.data.health.access.Constants.WILL_CONTROLS_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY;
import static eu.europa.data.health.access.Constants.WILL_RELATIVES_BE_EXTRACTED;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS;
import static eu.europa.data.health.access.Constants.WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES;
import static eu.europa.data.health.access.Constants.WILL_THE_DATA_BE_TRANSFERRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataRequestApplicationValidationManagerTest {

  @InjectMocks
  private DataRequestApplicationValidationManager validationManager;

  @Mock
  private RoutingContext routingContext;


  @Mock
  private HttpServerResponse response;

  @Mock
  private RequestBody requestBody;


  @Test
  void testValidateDeleteDistributionsJsonSchema_invokesValidator() {

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(routingContext.response()).thenReturn(response);

    when(routingContext.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(routingContext.response()).thenReturn(response);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(response);

    when(routingContext.body()).thenReturn(requestBody);
    validationManager.validateDeleteDistributionsJsonSchema(routingContext);
    verify(routingContext, times(2)).response();
  }

  @Test
  void testValidateDataAccessApplicationJsonSchema_invokesValidator() {

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(routingContext.response()).thenReturn(response);

    when(routingContext.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(routingContext.response()).thenReturn(response);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(response);

    when(routingContext.body()).thenReturn(requestBody);
    validationManager.validateDataAccessApplicationJsonSchema(routingContext);
    verify(routingContext, times(2)).response();
  }


  @Test
  void testValidatePutDistributionsApplicationJsonSchema_invokesValidator() {

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(routingContext.response()).thenReturn(response);

    when(routingContext.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(routingContext.response()).thenReturn(response);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(response);

    when(routingContext.body()).thenReturn(requestBody);
    validationManager.validatePutDistributionsApplicationJsonSchema(routingContext);
    verify(routingContext, times(2)).response();
  }


  @Test
  void testValidateDeleteDatasetsJsonSchema_invokesValidator() {

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(routingContext.response()).thenReturn(response);

    when(routingContext.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(routingContext.response()).thenReturn(response);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(response);

    when(routingContext.body()).thenReturn(requestBody);
    validationManager.validateDeleteDatasetsJsonSchema(routingContext);
    verify(routingContext, times(2)).response();

  }

  @Test
  void testValidateChangeTitleJsonSchema_invokesValidator() {

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(routingContext.response()).thenReturn(response);

    when(routingContext.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(routingContext.response()).thenReturn(response);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(response);

    when(routingContext.body()).thenReturn(requestBody);
    validationManager.validateChangeTitleJsonSchema(routingContext);
    verify(routingContext, times(2)).response();
  }

  @Test
  void testValidateChangeStatusJsonSchema_invokesValidator() {

    Buffer buffer = Buffer.buffer("{\"exampleField\":\"exampleValue\"}");
    var body = mock(io.vertx.ext.web.RequestBody.class);

    when(routingContext.response()).thenReturn(response);

    when(routingContext.body()).thenReturn(body);
    when(requestBody.asString()).thenReturn(buffer.toString());

    when(routingContext.response()).thenReturn(response);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(response);

    when(routingContext.body()).thenReturn(requestBody);
    validationManager.validateChangeStatusJsonSchema(routingContext);
    verify(routingContext, times(2)).response();

  }


  @Test
  void validateUpdate_section3_natural_withoutValidationErrors() {
    // given validateUpdate
    when(routingContext.pathParam("section_number")).thenReturn("3");

    JsonObject jsonObjectB = new JsonObject().put("key", "b");
    JsonObject json = new JsonObject().put("legalOrNaturalPerson", jsonObjectB)
      .put(APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR, jsonObjectB);
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }

  @Test
  void validateUpdate_section2_natural_withoutValidationErrors() {
    // given validateUpdate
    when(routingContext.pathParam("section_number")).thenReturn("2");

    JsonObject legalOrNaturalPerson = new JsonObject().put("key", "b");
    JsonObject json = new JsonObject().put("legalOrNaturalPerson", legalOrNaturalPerson);
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }

  @Test
  void validateUpdate_section2_with_summary_withoutValidationErrors() {
    // given validateUpdate
    when(routingContext.pathParam("section_number")).thenReturn("2");

    JsonObject legalOrNaturalPerson = new JsonObject().put("key", "b");
    JsonObject json = new JsonObject().put("legalOrNaturalPerson", legalOrNaturalPerson)
      .put(THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY, true)
      .put(THE_NATURE_OF_THE_DATA_DOES_NOT_LET_YOU_PROVIDE_A_DESCRIPTION, true);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section3_withValidationErrors_shouldPutErrorsInContext() {
    when(routingContext.pathParam("section_number")).thenReturn("3");

    SelectObjectDTO legalOrNaturalPerson = new SelectObjectDTO();
    legalOrNaturalPerson.setKey("a");

    JsonObject requestBodyJson = new JsonObject().put(CATALOGUE_ID, "test-cat-id")
      .put(DOMIBUS_ID, "dom-id").put(UPLOAD_RDF, new JsonObject().put("key", "b"))
      .put("geographicalCoverage", new JsonObject().put("key", "coverage"));

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(requestBodyJson);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section4_natural_withoutValidationErrors() {
    // given validateUpdate
    when(routingContext.pathParam("section_number")).thenReturn("4");

    JsonObject invoiceType = new JsonObject().put("key", "a");
    JsonObject isTheProjectFinanciallyCovered = new JsonObject().put("key", "b");

    JsonObject json = new JsonObject().put("isTheProjectFinanciallyCovered",
      isTheProjectFinanciallyCovered).put("invoiceType", invoiceType);
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }

  @Test
  void validateUpdate_section5_natural_withoutValidationErrors() {
    // given validateUpdate
    when(routingContext.pathParam("section_number")).thenReturn("5");

    JsonObject formatOfTheElectronicHealthData = new JsonObject().put("key", "a");

    JsonObject json = new JsonObject().put(FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA,
      formatOfTheElectronicHealthData);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section6a_withValidationErrors_shouldPutErrorsInContext() {
    when(routingContext.pathParam("section_number")).thenReturn("6");

    // simulate Section3DTO returning "a" for legalOrNaturalPerson
    SelectObjectDTO legalOrNaturalPerson = new SelectObjectDTO();
    legalOrNaturalPerson.setKey("a");

    JsonObject section6 = new JsonObject().put("country_id", 1)
      .put("howIsTheStudyCohortFormed", new JsonObject().put("key", "a"))
      .put(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES, new JsonObject().put("key", "a"))
      .put(WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY, new JsonObject().put("key", "a"));

    JsonArray section6Countries = new JsonArray().add(section6);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(section6Countries);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section6b_withValidationErrors_shouldPutErrorsInContext() {
    when(routingContext.pathParam("section_number")).thenReturn("6");

    // simulate Section3DTO returning "a" for legalOrNaturalPerson
    SelectObjectDTO legalOrNaturalPerson = new SelectObjectDTO();
    legalOrNaturalPerson.setKey("b");

    JsonObject section6 = new JsonObject().put("howIsTheStudyCohortFormed",
        new JsonObject().put("key", "a"))
      .put(WILL_CONTROLS_BE_EXTRACTED, new JsonObject().put("key", "b"))
      .put(WILL_RELATIVES_BE_EXTRACTED, new JsonObject().put("key", "b"))
      .put(HAS_THE_GROUP_RELATIVES_FORMED_3, new JsonObject().put("key", "b"))
      .put(HAS_THE_GROUP_RELATIVES_FORMED_2, new JsonObject().put("key", "b"))
      .put(PROVIDED_INFORMATION_OF_THE_DATA_USE_TO_CORRESPONDING_SUBJECTS,
        new JsonObject().put("key", "b"))
      .put(WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES, new JsonObject().put("key", "b"))
      .put(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY, new JsonObject().put("key", "b"))
      .put(DATA_EXTRACTION_FREQUENCY_RELATIVES, new JsonObject().put("key", "a"));
    JsonArray section6Countries = new JsonArray().add(section6);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(section6Countries);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section6c_withValidationErrors_shouldPutErrorsInContext() {
    when(routingContext.pathParam("section_number")).thenReturn("6");

    SelectObjectDTO legalOrNaturalPerson = new SelectObjectDTO();
    legalOrNaturalPerson.setKey("c");

    JsonObject section6 = new JsonObject().put("howIsTheStudyCohortFormed",
        new JsonObject().put("key", "a"))
      .put(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES, new JsonObject().put("key", "a"))
      .put(DATA_EXTRACTION_FREQUENCY_RELATIVES, new JsonObject().put("key", "a"))
      .put(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY, new JsonObject().put("key", "a"));
    JsonArray section6Countries = new JsonArray().add(section6);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(section6Countries);

    validationManager.validateUpdate(routingContext);

    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section6d_withValidationErrors_shouldPutErrorsInContext() {
    when(routingContext.pathParam("section_number")).thenReturn("6");

    SelectObjectDTO legalOrNaturalPerson = new SelectObjectDTO();
    legalOrNaturalPerson.setKey("a");

    JsonObject section6 = new JsonObject().put("country_id", 1)
      .put("howIsTheStudyCohortFormed", new JsonObject().put("key", "a"))
      .put(DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES, new JsonObject().put("key", "a"))

      .put(WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS, new JsonObject().put("key", "a"))
      .put(WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY, new JsonObject().put("key", "a"))
      .put(DATA_EXTRACTION_FREQUENCY, new JsonObject().put("key", "a"))
      .put(NEED_FOR_DATA_EXTRACTION_EVERY, new JsonObject().put("key", "a"))
      .put("howOftenDoesTheDataNeedToBeExtracted", new JsonObject().put("key", "a"))
      .put("extractionMethod", new JsonObject().put("key", "b"))
      .put("howIsTheStudyCohortFormed", new JsonObject().put("key", "b"))
      .put(HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS,
        new JsonObject().put("key", "b"));

    JsonArray section6Countries = new JsonArray().add(section6);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonArray()).thenReturn(section6Countries);

    validationManager.validateUpdate(routingContext);

    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section7_natural_withoutValidationErrors() {
    when(routingContext.pathParam("section_number")).thenReturn("7");

    JsonObject willDataBeCombinedWithDataObtained = new JsonObject().put("key", "b");
    JsonObject otherPermitApplications = new JsonObject().put("key", "b");

    JsonObject json = new JsonObject().put(WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED,
      willDataBeCombinedWithDataObtained).put(OTHER_PERMIT_APPLICATIONS, otherPermitApplications);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    verify(routingContext).next();
  }


  @Test
  void validateUpdate_section8_natural_withoutValidationErrors() {
    when(routingContext.pathParam("section_number")).thenReturn("8");

    JsonObject whenDataNeeded = new JsonObject().put("key", "a");
    JsonObject willTheDataBeTransferred = new JsonObject().put("key", "b");

    JsonObject json = new JsonObject().put(WHEN_DATA_NEEDED, whenDataNeeded)
      .put(WILL_THE_DATA_BE_TRANSFERRED, willTheDataBeTransferred);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }

  @Test
  void validateUpdate_section8b_natural_withoutValidationErrors() {
    when(routingContext.pathParam("section_number")).thenReturn("8");

    JsonObject whenDataNeeded = new JsonObject().put("key", "b");
    JsonObject willTheDataBeTransferred = new JsonObject().put("key", "a");

    JsonObject json = new JsonObject().put(WHEN_DATA_NEEDED, whenDataNeeded)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, true)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, true)
      .put(WILL_THE_DATA_BE_TRANSFERRED, willTheDataBeTransferred);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }

  @Test
  void validateUpdate_section8b_natural_withoutValidationErrors_willTheDataBeTransferred() {
    when(routingContext.pathParam("section_number")).thenReturn("8");

    JsonObject whenDataNeeded = new JsonObject().put("key", "b");
    JsonObject willTheDataBeTransferred = new JsonObject().put("key", "a");

    JsonObject json = new JsonObject().put(WHEN_DATA_NEEDED, whenDataNeeded)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48, false)
      .put(WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B, true)
      .put(WILL_THE_DATA_BE_TRANSFERRED, willTheDataBeTransferred);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdate(routingContext);

    // then
    verify(routingContext).put(eq("errors"), any());
    verify(routingContext).next();
  }


  @Test
  void validateUpdateTitle_shouldReturn400_whenValidationFails()  {
    JsonObject json = new JsonObject().put("title", "");

    // mock response
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);

    validationManager.validateUpdateTitle(routingContext);

  }

  @Test
  void validateUpdateTitle_shouldCallNext_whenValidationPasses() {
    JsonObject json = new JsonObject().put("title", "Valid title");

    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(json);
    validationManager.validateUpdateTitle(routingContext);
    verify(routingContext).next();
  }

}
