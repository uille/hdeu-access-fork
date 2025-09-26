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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.FileDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Section9DTOTest {

  private final Validator validator;

  public Section9DTOTest() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testValidSection9DTO() {
    Section9DTO dto = new Section9DTO();
    dto.setAdditionalInformation("This is some valid additional information.");
    dto.setAdditionalAttachment(createValidAttachments());

    Set<ConstraintViolation<Section9DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for a valid Section9DTO.");
  }

  @Test
  void testAdditionalInformationTooLong() {
    String longString = "A".repeat(2001); // Exceeds max length of 2000
    Section9DTO dto = new Section9DTO();
    dto.setAdditionalInformation(longString);
    dto.setAdditionalAttachment(createValidAttachments());

    Set<ConstraintViolation<Section9DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for additionalInformation exceeding max length.");
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("additionalInformation")),
      "Expected validation error on 'additionalInformation' field.");
  }

  @Test
  void testEmptyAdditionalAttachment() {
    Section9DTO dto = new Section9DTO();
    dto.setAdditionalInformation("This is valid information.");
    dto.setAdditionalAttachment(new ArrayList<>()); // Empty list

    Set<ConstraintViolation<Section9DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for an empty additionalAttachment list.");
  }

  @Test
  void testNullAdditionalAttachment() {
    Section9DTO dto = new Section9DTO();
    dto.setAdditionalInformation("This is valid information.");
    dto.setAdditionalAttachment(null);

    Set<ConstraintViolation<Section9DTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "Expected no validation errors for a null additionalAttachment.");
  }

  @Test
  void testInvalidAdditionalAttachment() {
    Section9DTO dto = new Section9DTO();
    dto.setAdditionalInformation("This is valid information.");
    List<AttachmentDTO> invalidAttachments = new ArrayList<>();
    FileDTO fileDTO = new FileDTO();
    fileDTO.setLastModified(1609459200000.0);  // Example timestamp (e.g., 2021-01-01 00:00:00)
    fileDTO.setName("fileName.txt");
    fileDTO.setWebkitRelativePath("/files/fileName.pdf");
    invalidAttachments.add(new AttachmentDTO("name.txt", "123", fileDTO, (double) 1000000L)); // Invalid extension and size
    dto.setAdditionalAttachment(invalidAttachments);

    Set<ConstraintViolation<Section9DTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty(), "Expected validation errors for invalid additionalAttachment.");
  }

  @Test
  void testGettersAndSetters() {
    Section9DTO dto = new Section9DTO();

    dto.setAdditionalInformation("This is a test.");
    assertEquals("This is a test.", dto.getAdditionalInformation(), "Expected additionalInformation to match.");

    List<AttachmentDTO> attachments = createValidAttachments();
    dto.setAdditionalAttachment(attachments);
    assertEquals(attachments, dto.getAdditionalAttachment(), "Expected additionalAttachment to match.");
  }

  @Test
  void testConstructorAndDefaultValues() {
    Section9DTO dto = new Section9DTO();
    assertNull(dto.getAdditionalInformation(), "Expected additionalInformation to be null by default.");
    assertNull(dto.getAdditionalAttachment(), "Expected additionalAttachment to be null by default.");
  }

  private List<AttachmentDTO> createValidAttachments() {
    List<AttachmentDTO> attachments = new ArrayList<>();
    FileDTO fileDTO = new FileDTO();
    fileDTO.setLastModified(1609459200000.0);  // Example timestamp (e.g., 2021-01-01 00:00:00)
    fileDTO.setName("fileName.txt");
    fileDTO.setWebkitRelativePath("/files/fileName.txt");
    attachments.add(new AttachmentDTO("name.pdf", "123", fileDTO, (double) 1000000L));
    attachments.add(new AttachmentDTO("name.pdf", "123", fileDTO, (double) 1000000L));
    return attachments;
  }
}

