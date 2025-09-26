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
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class AttachmentValidatorTest {

    @Test
    void testIsValid_NullAttachment_ShouldReturnTrue() {
        // Arrange
        AttachmentValidator validator = new AttachmentValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        // Act
        boolean result = validator.isValid(null, context);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsValid_ValidAttachment_ShouldReturnTrue() {
        // Arrange
        AttachmentValidation annotation = mock(AttachmentValidation.class);
        when(annotation.allowedExtensions()).thenReturn(new String[]{"jpg", "png", "pdf"});
        when(annotation.maxSize()).thenReturn(5000L);

        AttachmentValidator validator = new AttachmentValidator();
        validator.initialize(annotation);

        AttachmentDTO attachment = new AttachmentDTO("image.jpg", "1", null, 3000.0);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        // Act
        boolean result = validator.isValid(attachment, context);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsValid_InvalidExtension_ShouldReturnFalse() {
        // Arrange
        AttachmentValidation annotation = mock(AttachmentValidation.class);
        when(annotation.allowedExtensions()).thenReturn(new String[]{"jpg", "png", "pdf"});
        when(annotation.maxSize()).thenReturn(5000L);

        AttachmentValidator validator = new AttachmentValidator();
        validator.initialize(annotation);

        AttachmentDTO attachment = new AttachmentDTO("image.txt", "1", null, 3000.0);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class));

        // Act
        boolean result = validator.isValid(attachment, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Invalid file extension: txt");
    }

    @Test
    void testIsValid_FileSizeExceedsLimit_ShouldReturnFalse() {
        // Arrange
        AttachmentValidation annotation = mock(AttachmentValidation.class);
        when(annotation.allowedExtensions()).thenReturn(new String[]{"jpg", "png", "pdf"});
        when(annotation.maxSize()).thenReturn(5000L);

        AttachmentValidator validator = new AttachmentValidator();
        validator.initialize(annotation);

        AttachmentDTO attachment = new AttachmentDTO("image.jpg", "1", null, 6000.0);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class));

        // Act
        boolean result = validator.isValid(attachment, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("File size exceeds limit: 6000.0");
    }

    @Test
    void testIsValid_MultipleViolations_ShouldReturnFalse() {
        // Arrange
        AttachmentValidation annotation = mock(AttachmentValidation.class);
        when(annotation.allowedExtensions()).thenReturn(new String[]{"jpg", "png", "pdf"});
        when(annotation.maxSize()).thenReturn(5000L);

        AttachmentValidator validator = new AttachmentValidator();
        validator.initialize(annotation);

        AttachmentDTO attachment = new AttachmentDTO("file.exe", "1", null, 6000.0);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class));

        // Act
        boolean result = validator.isValid(attachment, context);

        // Assert
        assertFalse(result);
        verify(context, times(2)).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Invalid file extension: exe");
        verify(context).buildConstraintViolationWithTemplate("File size exceeds limit: 6000.0");
    }
}
