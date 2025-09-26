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

import eu.europa.data.health.access.dto.SelectObjectDTO;
import eu.europa.data.health.access.dto.sections.data_access.Section8DTO;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Section8DataBeTransferredValidatorTest {

    @Test
    void testIsValid_DataNotTransferred_ReturnsTrue() {
        Section8DTO section8DTO = new Section8DTO();
        section8DTO.setWillTheDataBeTransferred(null);

        Section8DataBeTransferredValidator validator = new Section8DataBeTransferredValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        boolean result = validator.isValid(section8DTO, context);

        assertTrue(result);
    }


    @Test
    void testIsValid_KeyIsA_ValidConditions_ReturnsTrue() {
        Section8DTO section8DTO = new Section8DTO();
        SelectObjectDTO selectObjectDTO = new SelectObjectDTO();
        selectObjectDTO.setKey("a");
        section8DTO.setWillTheDataBeTransferred(selectObjectDTO);
        section8DTO.setWhyWillDataBeTransferredOutsideEUArticle47(true);

        Section8DataBeTransferredValidator validator = new Section8DataBeTransferredValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        boolean result = validator.isValid(section8DTO, context);

        assertTrue(result);
    }
}
