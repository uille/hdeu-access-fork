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

public final class NotificationKeys {

  private NotificationKeys() {
  }

  // Notification property keys (used in properties map)
  public static final String APPLICATION_NAME = "APPLICATION_NAME";
  public static final String HDAB_NAME = "HDAB_NAME";
  public static final String DATASET_ID = "DATASET_ID";
  public static final String DATASET_NAME = "DATASET_NAME";
  public static final String NOTIFICATION_DAY = "NOTIFICATION_DAY";
  public static final String USER_NAME = "USER_NAME";

  // Email preferences keys
  public static final String EMAIL_NOTIFICATION_PREFERENCES = "emailNotificationPreferences";
  public static final String ENABLED = "enabled";
  public static final String REMINDERS = "reminders";
  public static final String OFFSET_DAYS = "offsetDays";
  public static final String USER_NAME_FIELD = "userName";
}
