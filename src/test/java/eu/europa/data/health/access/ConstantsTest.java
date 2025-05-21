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
package eu.europa.data.health.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class ConstantsTest {

  @Test
  void testConstantsValuesCase1() {
    assertEquals("MONGO_DB_URI", Constants.ENV_MONGO_DB_URI);
    assertEquals("MONGO_DB", Constants.ENV_MONGO_DB);
    assertEquals("HEALTH_DATA_BODY_LIMIT", Constants.HEALTH_DATA_BODY_LIMIT);
    assertEquals(20971520L, Constants.BODY_LIMIT);
    assertEquals("HUB_STORE_UPLOAD_DATA_URL", Constants.HUB_STORE_UPLOAD_DATA_URL);
    assertEquals("PIVEAU_STORE_API_KEY", Constants.PIVEAU_STORE_API_KEY);
    assertEquals("all", Constants.COUNTRIES_ALL);
    assertEquals("noEU", Constants.COUNTRIES_NO_EU);
    assertEquals("euEEA", Constants.COUNTRIES_EU_EEA);
    assertEquals("DATA_ACCESS_DISPATCHER_MAX_RETRIES", Constants.DATA_ACCESS_DISPATCHER_MAX_RETRIES);
    assertEquals("DATA_ACCESS_DISPATCHER_MAX_RETRIES", Constants.DATA_REQUEST_DISPATCHER_MAX_RETRIES);
    assertEquals("DATA_ACCESS_DISPATCHER_INTERVAL", Constants.DATA_ACCESS_DISPATCHER_INTERVAL);
    assertEquals("DATA_REQUEST_DISPATCHER_INTERVAL", Constants.DATA_REQUEST_DISPATCHER_INTERVAL);
    assertEquals(5, Constants.DATA_ACCESS_DISPATCHER_DEFAULT_MAX_RETRIES);
    assertEquals(5, Constants.DATA_REQUEST_DISPATCHER_DEFAULT_MAX_RETRIES);
    assertEquals(300000, Constants.DATA_ACCESS_DISPATCHER_DEFAULT_INTERVAL);
    assertEquals(300000, Constants.DATA_REQUEST_DISPATCHER_DEFAULT_INTERVAL);

  }

  @Test
  void testConstantsValuesCase2() {
    assertEquals("DISPATCHER_URL", Constants.DISPATCHER_URL);
    assertEquals("DISPATCHER_X_API_KEY", Constants.DISPATCHER_X_API_KEY);
    assertEquals("KEYCLOAK_INTROSPECT_PATH", Constants.ENV_KEYCLOAK_INTROSPECT_PATH);
    assertEquals("PIVEAU_ACCESS_AUTH_CONFIG", Constants.ENV_PIVEAU_ACCESS_AUTH_CONFIG);
    assertEquals("HEALTH_DATA_ACCESS_API_KEY", Constants.ENV_HEALTH_DATA_ACCESS_API_KEY);
    assertEquals("BearerAuth", Constants.BEARER_AUTH);
    assertEquals("USER_DELETED", Constants.USER_DELETED);
    assertEquals(50, Constants.PAGINATION_LIMIT);
    assertEquals("HUB_SEARCH_DATA_URL", Constants.HUB_SEARCH_DATA_URL);
    assertEquals("EXPORT_DATA_ACCESS_API_KEY", Constants.EXPORT_DATA_ACCESS_API_KEY);
    assertEquals("EXPORT_DATASET_EXTERNAL_URL", Constants.EXPORT_DATASET_EXTERNAL_URL);
    assertEquals("application_type", Constants.APPLICATION_TYPE);
    assertEquals("DATA_REQUEST_APPLICATION", Constants.DATA_REQUEST_APPLICATION_COLLECTION);
    assertEquals("DATA_ACCESS_APPLICATION", Constants.DATA_ACCESS_APPLICATION_COLLECTION);
  }

  @Test
  void testPrivateConstructor() {
    Constructor<?>[] constructors = Constants.class.getDeclaredConstructors();
    assertEquals(1, constructors.length, "Constants class should only have one constructor");

    Constructor<?> constructor = constructors[0];
    assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()), "Constructor should be private");

    constructor.setAccessible(true);

    Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    assertEquals("This is a utility class and cannot be instantiated", exception.getCause().getMessage());
  }
}
