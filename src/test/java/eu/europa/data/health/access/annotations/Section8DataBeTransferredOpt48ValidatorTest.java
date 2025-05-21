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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.dto.data_access.Section8DTO;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Section8DataBeTransferredOpt48ValidatorTest {

  private Section8DataBeTransferredOpt48Validator validator;

  @Mock
  private ConstraintValidatorContext context;

  @BeforeEach
  public void setUp() {
    validator = new Section8DataBeTransferredOpt48Validator();
  }

  @Test
  void testValidWhenArticle48IsFalse() {
    Section8DTO dto = new Section8DTO();
    dto.setWhyWillDataBeTransferredOutsideEUArticle48(false);

    assertTrue(validator.isValid(dto, context));
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  void testValidWhenAtLeastOneArticleIsTrue() {
    Section8DTO dto = new Section8DTO();
    dto.setWhyWillDataBeTransferredOutsideEUArticle48(true);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48b(true);

    assertTrue(validator.isValid(dto, context));
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  void testInvalidWhenAllArticlesAreFalse() {

    ConstraintViolationBuilder builder = Mockito.mock(ConstraintViolationBuilder.class);
    NodeBuilderCustomizableContext nodeBuilder = Mockito.mock(NodeBuilderCustomizableContext.class);

    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(builder);
    when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);


    Section8DTO dto = new Section8DTO();
    dto.setWhyWillDataBeTransferredOutsideEUArticle48(true);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48a(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48b(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48c(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48d(false);
    dto.setWhyWillDataBeTransferredOutsideEUArticle48e(false);

    assertFalse(validator.isValid(dto, context));
    verify(context).disableDefaultConstraintViolation();
  }
}
