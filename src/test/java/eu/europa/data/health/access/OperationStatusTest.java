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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class OperationStatusTest {

    /**
     * Tests the fromLabel method in the OperationStatus enum.
     * This method retrieves the corresponding OperationStatus enum
     * based on the provided label string, or returns null if no match is found.
     */

    @Test
    void testFromLabelWithValidLabel() {
        // Test for a valid label matching an enum constant
        OperationStatus result = OperationStatus.fromLabel("statusUpdate");
        assertNotNull(result);
        assertEquals(OperationStatus.STATUS_UPDATE, result);
    }

    @Test
    void testFromLabelWithValidLabelWithExtraSpaces() {
        // Test for a valid label with leading and trailing spaces
        OperationStatus result = OperationStatus.fromLabel("   update  ");
        assertNotNull(result);
        assertEquals(OperationStatus.UPDATE, result);
    }

    @Test
    void testFromLabelWithInvalidLabel() {
        // Test for an invalid label that doesn't match any enum constant
        OperationStatus result = OperationStatus.fromLabel("nonexistent");
        assertNull(result);
    }

    @Test
    void testFromLabelWithEmptyLabel() {
        // Test for an empty string as label
        OperationStatus result = OperationStatus.fromLabel("");
        assertNull(result);
    }

    @Test
    void testFromLabelWithNullLabel() {
        // Test for a null label
        OperationStatus result = OperationStatus.fromLabel(null);
        assertNull(result);
    }
}
