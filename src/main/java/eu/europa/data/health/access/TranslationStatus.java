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

public enum TranslationStatus {

    NOT_STARTED("not_started"),
    FILES_PENDING("files_pending"),
    FILES_COMPLETED("files_completed"),
    FORM_PENDING("form_pending"),
    COMPLETED("completed"),
    PARTIALLY_COMPLETED("partially_completed"),
    FAILED("failed"),
    PENDING("pending");

    private final String status;

    TranslationStatus(final String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static boolean isValidStatus(String statusStr) {
        for (TranslationStatus status : TranslationStatus.values()) {
            if (status.getStatus().equalsIgnoreCase(statusStr)) {
                return true;
            }
        }
        return false;
    }

    public static TranslationStatus fromStatus(String statusStr) {
        for (TranslationStatus status : TranslationStatus.values()) {
            if (status.getStatus().equalsIgnoreCase(statusStr)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No enum constant with status " + statusStr);
    }
}
