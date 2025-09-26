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

import eu.europa.data.health.access.dto.sections.data_access.Section8DTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.BooleanUtils;

public class Section8DataBeTransferredValidator implements ConstraintValidator<Section8DataBeTransferred, Section8DTO> {

  @Override
  public boolean isValid(Section8DTO value, ConstraintValidatorContext context) {
    if(Objects.isNull(value.getWillTheDataBeTransferred())){
      return true;
    }
    boolean isValid = true;
    if ("a".equalsIgnoreCase(value.getWillTheDataBeTransferred().getKey())) {
      isValid = value.isWhyWillDataBeTransferredOutsideEUArticle47() ||
        value.isWhyWillDataBeTransferredOutsideEUArticle48() ||
        value.isWhyWillDataBeTransferredOutsideEUArticle49();
      if (BooleanUtils.isFalse(isValid)) {
        context.disableDefaultConstraintViolation();
        List<String> fields = Arrays.asList(
          "whyWillDataBeTransferredOutsideEUArticle47",
          "whyWillDataBeTransferredOutsideEUArticle48",
          "whyWillDataBeTransferredOutsideEUArticle49"
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
