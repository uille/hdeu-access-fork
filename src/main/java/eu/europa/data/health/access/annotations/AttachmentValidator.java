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
import java.util.Set;
import java.util.stream.Collectors;

public class AttachmentValidator implements ConstraintValidator<AttachmentValidation, AttachmentDTO> {

  private Set<String> allowedExtensions;
  private long maxSize;

  @Override
  public void initialize(AttachmentValidation constraintAnnotation) {
    this.allowedExtensions = Arrays.stream(constraintAnnotation.allowedExtensions())
      .collect(Collectors.toSet());
    this.maxSize = constraintAnnotation.maxSize();
  }

  @Override
  public boolean isValid(AttachmentDTO attachment, ConstraintValidatorContext context) {
    if (attachment == null) {
      return true; // Allow null values, use @NotNull to enforce non-null
    }

    boolean isValid = true;

    // Validate file extension
    String fileName = attachment.getName();
    String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    if (!allowedExtensions.contains(fileExtension)) {
      isValid = false;
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Invalid file extension: " + fileExtension)
        .addPropertyNode("name").addConstraintViolation();
    }

    // Validate file size
    if (attachment.getSize() > maxSize) {
      isValid = false;
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("File size exceeds limit: " + attachment.getSize())
        .addPropertyNode("size").addConstraintViolation();
    }

    return isValid;
  }
}
