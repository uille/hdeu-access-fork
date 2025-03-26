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

import eu.europa.data.health.access.dto.data_access.Section8DTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;

public class Section8DataBeTransferredOpt48Validator implements ConstraintValidator<Section8DataBeTransferredOpt48, Section8DTO> {

  /**
   * Unique case where FE need to get a different error for each field
   */
  @Override
  public boolean isValid(Section8DTO value, ConstraintValidatorContext context) {
    boolean isValid = true;
    if (value.isWhyWillDataBeTransferredOutsideEUArticle48()) {
      isValid = value.isWhyWillDataBeTransferredOutsideEUArticle48a() ||
        value.isWhyWillDataBeTransferredOutsideEUArticle48b() ||
        value.isWhyWillDataBeTransferredOutsideEUArticle48c() ||
        value.isWhyWillDataBeTransferredOutsideEUArticle48d() ||
        value.isWhyWillDataBeTransferredOutsideEUArticle48e();
      if (BooleanUtils.isFalse(isValid)) {
        context.disableDefaultConstraintViolation();
        List<String> fields = Arrays.asList(
          "whyWillDataBeTransferredOutsideEUArticle48a",
          "whyWillDataBeTransferredOutsideEUArticle48b",
          "whyWillDataBeTransferredOutsideEUArticle48c",
          "whyWillDataBeTransferredOutsideEUArticle48d",
          "whyWillDataBeTransferredOutsideEUArticle48e"
        );
        for (String field : fields) {
          context.buildConstraintViolationWithTemplate(
              context.getDefaultConstraintMessageTemplate())
            .addPropertyNode(field)
            .addConstraintViolation();
        }
      }
    }
    return isValid;
  }


}
