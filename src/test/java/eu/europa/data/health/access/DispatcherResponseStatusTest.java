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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DispatcherResponseStatusTest {

  @Test
  void testEnumValues() {
    // Validate that the enum contains all expected values
    assertEquals("dispatcher_received", DispatcherResponseStatus.DISPATCHER_RECEIVED.getDispatcherResponseStatus());
    assertEquals("domibus_national_received", DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.getDispatcherResponseStatus());
    assertEquals("domibus_received", DispatcherResponseStatus.DOMIBUS_RECEIVED.getDispatcherResponseStatus());
    assertEquals("failed", DispatcherResponseStatus.FAILED.getDispatcherResponseStatus());
  }

  @Test
  void testEnumConsistency() {
    // Validate the enum's toString() matches its defined name
    assertEquals("DISPATCHER_RECEIVED", DispatcherResponseStatus.DISPATCHER_RECEIVED.name());
    assertEquals("DOMIBUS_NATIONAL_RECEIVED", DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.name());
    assertEquals("DOMIBUS_RECEIVED", DispatcherResponseStatus.DOMIBUS_RECEIVED.name());
    assertEquals("FAILED", DispatcherResponseStatus.FAILED.name());
  }

  @Test
  void testEnumOrdinalValues() {
    // Validate the ordinal positions of the enum values
    assertEquals(0, DispatcherResponseStatus.DISPATCHER_RECEIVED.ordinal());
    assertEquals(1, DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED.ordinal());
    assertEquals(2, DispatcherResponseStatus.DOMIBUS_RECEIVED.ordinal());
    assertEquals(3, DispatcherResponseStatus.FAILED.ordinal());
  }

  @Test
  void testEnumValueOf() {
    // Validate the valueOf method for correct name matching
    assertEquals(DispatcherResponseStatus.DISPATCHER_RECEIVED, DispatcherResponseStatus.valueOf("DISPATCHER_RECEIVED"));
    assertEquals(DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED, DispatcherResponseStatus.valueOf("DOMIBUS_NATIONAL_RECEIVED"));
    assertEquals(DispatcherResponseStatus.DOMIBUS_RECEIVED, DispatcherResponseStatus.valueOf("DOMIBUS_RECEIVED"));
    assertEquals(DispatcherResponseStatus.FAILED, DispatcherResponseStatus.valueOf("FAILED"));
  }

  @Test
  void testEnumValuesMethod() {
    // Validate the values() method to ensure all enum constants are returned
    DispatcherResponseStatus[] statuses = DispatcherResponseStatus.values();
    assertEquals(4, statuses.length);
    assertArrayEquals(
      new DispatcherResponseStatus[]{
        DispatcherResponseStatus.DISPATCHER_RECEIVED,
        DispatcherResponseStatus.DOMIBUS_NATIONAL_RECEIVED,
        DispatcherResponseStatus.DOMIBUS_RECEIVED,
        DispatcherResponseStatus.FAILED
      },
      statuses
    );
  }
}

