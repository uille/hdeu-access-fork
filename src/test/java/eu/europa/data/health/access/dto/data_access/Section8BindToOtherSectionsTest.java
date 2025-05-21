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
package eu.europa.data.health.access.dto.data_access;

import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Section8BindToOtherSectionsTest {

  private Validator validator;

  @BeforeEach
  void setup() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void shouldPassValidation_whenMinimalValidDataIsProvided () {
    Section8BindToOtherSections dto = new Section8BindToOtherSections();
    Set<ConstraintViolation<Section8BindToOtherSections>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation violations but found some");

  }

  @Test
  void shouldFailValidation_whenRequiredFieldsAreMissing() {
    Section8BindToOtherSections dto = new Section8BindToOtherSections();

    // Populate with minimal valid data depending on annotations
    dto.setSection3applyingForDataOnBehalfOfPublicSector(new SelectObjectDTO("b", "No"));
    dto.setEstimatedStartDatesForDataProcessing("2099-01-01");
    dto.setEstimatedEndDatesForDataProcessing("2099-12-31");
    dto.setInformationWhichDataCanAccess("test");
    dto.setFrequencyOfAccessOrDataUpdates("test");
    dto.setComplyWithDataMinimisationPrincipleNotEUMember("test");
    dto.setOtherLegalBasisForProcessingCombinedData("test");
    dto.setOtherLegalBasisForProcessingCombinedApplicationData("test");
    dto.setOtherLegalBasisForProcessingCombinedData("test");

    Set<ConstraintViolation<Section8BindToOtherSections>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation violations but found none");

  }
}
