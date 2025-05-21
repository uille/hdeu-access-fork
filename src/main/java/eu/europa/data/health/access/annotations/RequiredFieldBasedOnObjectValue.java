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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RequiredFieldBasedOnObjectValueValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RequiredFieldBasedOnObjectValue.List.class)
public @interface RequiredFieldBasedOnObjectValue {

  String message() default "Invalid conditional constraint";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String baseField();

  String baseFieldValue();

  String mandatoryField();

  String mandatoryFieldType();

  String objectName();

  @Target({ ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface List {
    RequiredFieldBasedOnObjectValue[] value();
  }
}
