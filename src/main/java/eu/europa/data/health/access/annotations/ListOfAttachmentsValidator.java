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
package eu.europa.data.health.access.annotations;

import eu.europa.data.health.access.dto.AttachmentDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ListOfAttachmentsValidator implements ConstraintValidator<ListOfAttachmentsValidation, List<AttachmentDTO>> {

  private Set<String> allowedExtensions;
  private long maxSize;
  private String fieldName;

  @Override
  public void initialize(ListOfAttachmentsValidation constraintAnnotation) {
    this.allowedExtensions = Arrays.stream(constraintAnnotation.allowedExtensions())
      .collect(Collectors.toSet());
    this.maxSize = constraintAnnotation.maxSize();
    this.fieldName = constraintAnnotation.fieldName();
  }

  @Override
  public boolean isValid(List<AttachmentDTO> attachments, ConstraintValidatorContext context) {
    if (attachments == null) {
      return true; // Allow null lists, use @NotNull to enforce non-null
    }

    boolean isValid = true;

    for (int i = 0; i < attachments.size(); i++) {
      AttachmentDTO attachment = attachments.get(i);
      if (attachment == null) {
        continue; // Skip null attachments, use @NotNull to enforce non-null attachments
      }

      // Validate file extension
      String fileName = attachment.getName();
      String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
      if (!allowedExtensions.contains(fileExtension)) {
        isValid = false;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Invalid file extension: " + fileExtension)
          .addPropertyNode(Integer.toString(i)).addPropertyNode(fieldName).addConstraintViolation();
      }

      // Validate file size
      if (attachment.getSize() > maxSize) {
        isValid = false;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("File size exceeds limit: " + attachment.getSize())
          .addPropertyNode(Integer.toString(i)).addPropertyNode(fieldName).addConstraintViolation();
      }
    }

    return isValid;
  }
}
