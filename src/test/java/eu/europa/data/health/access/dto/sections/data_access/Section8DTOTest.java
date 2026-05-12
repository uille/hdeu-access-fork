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
package eu.europa.data.health.access.dto.sections.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.data.health.access.dto.PersonDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Section8DTOTest {

  private final Validator validator;

  public Section8DTOTest() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testValidSection8DTO() {
    Section8DTO dto = createValidSection8DTO();

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for a valid Section8DTO.");
  }

  @Test
  void testTechnicalRequirementsTooLong() {
    Section8DTO dto = createValidSection8DTO();
    dto.setTechnicalRequirementsForEnvironment("a".repeat(2001)); // Exceeds max size
    dto.setWhyWillDataBeTransferredOutsideEUArticle47(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48(false);
    dto.setStartPeriodOfInactiveDataStorage("2024-11-01");
    dto.setEndPeriodOfInactiveDataStorage("3024-12-01");
    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for technicalRequirementsForEnvironment exceeding max size.");
    assertTrue(violations.stream().anyMatch(v ->
        v.getMessage().equals("size must be between 0 and 2000")),
      "Expected error on 'technicalRequirementsForEnvironment' field.");
  }

  @Test
  void testInvalidIfLaterWhenDate() {
    Section8DTO dto = createValidSection8DTO();
    dto.setIfLaterWhen("2023-01-01"); // Past date

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for past 'ifLaterWhen' date.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Date must be in the future")),
      "Expected error message for invalid 'ifLaterWhen' date.");
  }

  @Test
  void testInvalidEndPeriodOfInactiveDataStorage() {
    Section8DTO dto = createValidSection8DTO();
    dto.setStartPeriodOfInactiveDataStorage("2024-01-01");
    dto.setEndPeriodOfInactiveDataStorage("2023-12-31"); // End date before start date

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for 'endPeriodOfInactiveDataStorage' before 'startPeriodOfInactiveDataStorage'.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Date must be higher than starting day")),
      "Expected error message for invalid 'endPeriodOfInactiveDataStorage'.");
  }

  @Test
  void testMandatoryFieldsForDataTransfer() {
    Section8DTO dto = createValidSection8DTO();
    dto.setWillTheDataBeTransferred(createSelectObject("a", "Yes"));
    dto.setWhichCountriesDataBeProcessed(null); // Required when data is transferred
    dto.setLegalBasisForTransferringTheDataOutsideEU(createSelectObject("b", "Legal Basis"));

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for missing mandatory fields when data is transferred.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("whichCountriesDataBeProcessed must be provided")),
      "Expected error message for missing 'whichCountriesDataBeProcessed'.");
  }

  @Test
  void testAssertionsProtectionAndSecurityStatements() {
    Section8DTO dto = createValidSection8DTO();
    dto.setProtectionAndSecurityStatementsA(false); // Should fail @AssertTrue

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for failed @AssertTrue on protectionAndSecurityStatementsA.");
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("protectionAndSecurityStatementsA")),
      "Expected error on 'protectionAndSecurityStatementsA'.");
  }

  @Test
  void testPeopleWhoWillBeProcessingTheDataFieldsNotNull() {
    Section8DTO dto = createValidSection8DTO();
    PersonDTO person = new PersonDTO();
    person.setFullName(null); // Should trigger validation error
    person.setEmailAddress("email@example.com");
    dto.setPeopleWhoWillBeProcessingTheData(Collections.singletonList(person));

    Set<ConstraintViolation<Section8DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation error for null 'fullName' in PersonDTO.");
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be null")),
      "Expected error message for 'fullName' in PersonDTO.");
  }

  @Test
  void testGettersAndSetters() {
    Section8DTO dto = new Section8DTO();

    dto.setTechnicalRequirementsForEnvironment("Environment Info");
    assertEquals("Environment Info", dto.getTechnicalRequirementsForEnvironment());

    dto.setProtectionAndSecurityStatementsA(true);
    assertTrue(dto.isProtectionAndSecurityStatementsA());

    dto.setWillTheDataBeTransferred(createSelectObject("a", "Yes"));
    assertNotNull(dto.getWillTheDataBeTransferred());
  }

  private Section8DTO createValidSection8DTO() {
    Section8DTO dto = new Section8DTO();
    dto.setTechnicalRequirementsForEnvironment("Technical requirements");
    dto.setNameWebsiteAddressForEnvironment("www.environment.com");
    dto.setWhenDataNeeded(createSelectObject("b", "Later"));
    dto.setIfLaterWhen("2094-12-31");
    dto.setStartPeriodOfInactiveDataStorage("3024-01-01");
    dto.setEndPeriodOfInactiveDataStorage("4024-12-31");
    dto.setWillTheDataBeTransferred(createSelectObject("b", "No"));
    dto.setWhichCountriesDataBeProcessed(Collections.singletonList(createSelectObject("1", "Country")));
    dto.setLegalBasisForTransferringTheDataOutsideEU(createSelectObject("b", "Legal Basis"));
    dto.setSafeguardsAreProvidedByReferringGDCP(Collections.singletonList(createSelectObject("a", "GDCP")));
    dto.setLegalBasisForTransferringTheDataOutsideEUOtherOptions(Collections.singletonList(createSelectObject("a", "Other")));
    dto.setWhichOrganizationWillBeTheControllerOfData("Controller Org");
    dto.setProtectionAndSecurityStatementsA(true);
    dto.setProtectionAndSecurityStatementsB(true);
    dto.setProtectionAndSecurityStatementsC(true);
    dto.setProtectionAndSecurityStatementsD(true);
    dto.setProtectionAndSecurityStatementsE(true);
    dto.setWhyWillDataBeTransferredOutsideEUArticle47(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48b(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48a(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48c(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48d(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48e(false);
    SelectObjectDTO selectObject = new SelectObjectDTO("a", "value");
    List<SelectObjectDTO> selectObjectList = new ArrayList<>();
    selectObjectList.add(selectObject);
    dto.setWhyWillDataBeTransferredOutsideEUArticle47Options(selectObjectList);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle49(false);
    PersonDTO person = new PersonDTO();
    person.setFullName("John Doe");
    person.setEmailAddress("email@example.com");
    dto.setPeopleWhoWillBeProcessingTheData(Collections.singletonList(person));
    SelectObjectDTO selectObject8 = new SelectObjectDTO("b", "No");
    dto.setOptOutOfTheMechanismProvidedInTheNationalLaw(selectObject8);
    dto.setHdabToRetain(createSelectObject("b", "No"));
    return dto;
  }

  private SelectObjectDTO createSelectObject(String key, String value) {
    SelectObjectDTO obj = new SelectObjectDTO();
    obj.setKey(key);
    obj.setValue(value);
    return obj;
  }
}
