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
package eu.europa.data.health.access.validations.validators;

import io.piveau.security.PiveauAuth;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PermissionValidator implements Handler<RoutingContext> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final List<String> scopes;

  public PermissionValidator(List<String> scopes) {
    this.scopes = scopes;
  }

  @Override
  public void handle(RoutingContext context) {
    //it is null
    String resource = context.queryParams().get("catalog");
    if (log.isTraceEnabled()) {
      log.trace("Permission check for {}: {}", resource,
        context.user().principal().encodePrettily());
    }

    if (scopes.stream().allMatch(scope -> PiveauAuth.userHasRole(context.user(), scope))
      || scopes.stream().allMatch(scope -> PiveauAuth.userHasPermission(context.user(), resource, scope))){
      context.next();
    } else {
      context.fail(403);
    }
  }
}


