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
package eu.europa.data.health.access.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class GeneralExceptionTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testGeneralExceptionGetter() {
    FieldError fieldError = new FieldError(
      "E001",
      "Invalid input",
      "fieldName",
      "1",
      List.of("param1", "param2"),
      "US",
      "section6SubSectionValue"
    );

    GeneralException generalException = new GeneralException(List.of(fieldError));

    assertNotNull(generalException.getErrors());
    assertEquals(1, generalException.getErrors().size());
    assertEquals("E001", generalException.getErrors().get(0).getCode());
  }

  @Test
  void testGeneralExceptionSerialization() throws JsonProcessingException {
    FieldError fieldError1 = new FieldError(
      "E002",
      "Field is required",
      "fieldName2",
      null,
      List.of("paramA"),
      "UK",
      null
    );

    FieldError fieldError2 = new FieldError(
      "E003",
      "Invalid format",
      "fieldName3",
      "5",
      List.of("paramX", "paramY"),
      null,
      "section6Value"
    );

    GeneralException generalException = new GeneralException(List.of(fieldError1, fieldError2));
    String json = objectMapper.writeValueAsString(generalException);

    assertTrue(json.contains("\"errors\""));
    assertTrue(json.contains("\"code\":\"E002\""));
    assertTrue(json.contains("\"code\":\"E003\""));
    assertTrue(json.contains("\"field\":\"fieldName2\""));
    assertTrue(json.contains("\"section6SubSection\":\"section6Value\""));
  }


}
