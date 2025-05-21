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
package eu.europa.data.health.access.dto.data_request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.data.health.access.annotations.ListContainsSpecificValue;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

  @ListContainsSpecificValue(
    listField = "section2purposeOfUse",
    baseField = "key",
    baseFieldValue = "b",
    mandatoryField = "whatIsTheFrequencyOfUpdates",
    message = "whatIsTheFrequencyOfUpdates must be provided when "
      + "section2purposeOfUse is 2. b. To support public sector bodies or Union institutions, agencies and bodies including regulatory authorities, in the health or care sector to carry out their tasks defined in their mandates"
  )

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section6BindToOtherSections {

  List<SelectObjectDTO> section2purposeOfUse;

  @Size(max = 2000)
  String whatIsTheFrequencyOfUpdates;
}
