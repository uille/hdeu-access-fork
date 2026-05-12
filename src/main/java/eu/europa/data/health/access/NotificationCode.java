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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum NotificationCode {

  // 1 Application [Application Name] has been submitted to [HDAB Name]
  APPLICATION_SUBMIT("001"),
  // 2	Status changes to “Submitted” after resubmitting with additional information	Application [Application Name] has been resubmitted with additional information to [HDAB Name]
  APPLICATION_RESUBMIT("002"),
  // 3 Application [Application Name] has been rejected as Incomplete by HDAB [HDAB Name]
  CATALOG_REJECTED("003"),
  // 4 Application [Application Name] has been returned by HDAB [HDAB Name] for additional information
  CATALOG_AWAIT_ADDITIONAL_INFORMATION("004"),
  // 5 Application [Application Name] was accepted and is being processed by HDAB [HDAB Name]
  CATALOG_PROCESSING("005"),
  // 6 Processing of Application [Application Name] has been extended by HDAB [HDAB Name]
  CATALOG_PROCESSING_EXTENDED("006"),
  // 7 Application [Application Name] for HDAB [HDAB Name] has been withdrawn by the system
  CATALOG_WITHDRAW("007"),
  // 8 Positive Decision issued for Dataset [Dataset ID] in Application [Application Name]
  DATASET_POSITIVE_DECISION_ISSUED("008"),
  // 9 Negative Decision issued for Dataset [Dataset ID] in Application [Application Name]
  DATASET_NEGATIVE_DECISION_ISSUED("009"),
  // 11 Appeal rejected for Dataset [Dataset ID] in Application [Application Name]
  DATASET_APPEAL_REJECTED("011"),
  // 12 Data Permit granted for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_GRANTED("012"),
  // 13 Data Permit renewed for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_RENEWED("013"),
  // 14 Data Permit renewal rejected for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_RENEWAL_REJECTED("014"),
  // 15 Data Permit amended for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_AMENDED("015"),
  // 16 Data Permit amendment rejected for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_AMENDMENT_REJECTED("016"),
  // 17 Data Permit has expired for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_EXPIRED("017"),
  // 18 Data Permit  has been revoked for Dataset [Dataset ID] in Application [Application Name]
  PERMIT_REVOKED("018"),
  // 19 Appeal on Revoked Data Permit for Dataset [Dataset ID] in Application [Application Name] has been approved
  PERMIT_APPEAL_APPROVED("019"),
  // 20 Appeal on revoked Data Permit for Dataset [Dataset ID] in Application [Application Name has been rejected
  PERMIT_APPEAL_REJECTED("020"),
  // 21 Data Permit [Data Permit ID] for Application [Application Name] is approaching expiration. Please submit a renewal request if needed.
  SCHEDULED_PERMIT_EXPIRATION_APPROACHING("021"),
  // 10 Data Permit [Data Permit ID] for Application [Application Name] is approaching expiration.
  SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW("010"),
  // 22 A decision is awaiting your action for Application [Application Name]. It will be automatically withdrawn in [X] days if no response is submitted.
  SCHEDULED_DATASET_WITHDRAWN_APPROACHING("022"),
  // 23 Application [Application Name] requires action. It will be automatically withdrawn in [X] days if no information is provided.
  SCHEDULED_CATALOG_WITHDRAWN_APPROACHING("023"),
  // 24 You have been invited to collaborate on Health Data Application [Application Reference Number – Application Name] with the role Editor. Please accept or reject the invitation
  COLLABORATION_INVITE_SENT("024"),
  // 25 You have been removed from Health Data Application [Application Reference Number – Application Name]. You no longer have access to this application.
  COLLABORATOR_REMOVED_BY_OWNER("025"),
  // 26 [User Name] has left Health Data Application [Application Reference Number – Application Name].
  COLLABORATOR_SELF_REMOVED("026"),
  // 27 Ownership of Health Data Application [Application Reference Number – Application Name] has been transferred to you. You are now the Owner of this application.
  COLLABORATION_OWNERSHIP_TRANSFERRED("027"),
  // 28 [User Name] has accepted your invitation to collaborate on Health Data Application [Application Reference Number – Application Name].
  COLLABORATION_INVITE_ACCEPTED("028"),
  // 29 [User Name] has rejected your invitation to collaborate on Health Data Application [Application Reference Number – Application Name].
  COLLABORATION_INVITE_REJECTED("029"),
  // 30 Transfer of the ownership of Health Data Application [Application Reference Number – Application Name] has been requested.
  COLLABORATION_OWNERSHIP_REQUESTED("030"),
  // 31 Transfer of the ownership of Health Data Application [Application Reference Number – Application Name] has been approved by [User Name].
  COLLABORATION_OWNERSHIP_REQUEST_APPROVED("031"),
  // 32 Transfer of the ownership of Health Data Application [Application Reference Number – Application Name] has been rejected by [User Name].
  COLLABORATION_OWNERSHIP_REQUEST_REJECTED("032"),
  // 33 Ownership of Health Data Application [Application Reference Number – Application Name] has been transferred to another collaborator by the administrator. You are now editor of this application.
  COLLABORATION_OWNERSHIP_TRANSFERRED_BY_ADMIN("033"),
  // 34 The Health Data Application [Application Reference Number – Application Name] has been discarded by the owner.
  APPLICATION_DISCARDED_BY_OWNER("034");


  private final String code;

  // 🔹 Fast reverse lookup (very useful in real systems)
  private static final Map<String, NotificationCode> BY_CODE =
    Arrays.stream(values())
      .collect(Collectors.toMap(NotificationCode::getCode, Function.identity()));

  NotificationCode(String code) {
    this.code = code;
  }

  // 🔹 Optional but highly recommended
  public static NotificationCode fromCode(String code) {
    NotificationCode result = BY_CODE.get(code);
    if (result == null) {
      throw new IllegalArgumentException("Unknown NotificationCode: " + code);
    }
    return result;
  }

  // 🔹 Optional: safe lookup (no exception)
  public static NotificationCode fromCodeOrNull(String code) {
    return BY_CODE.get(code);
  }

  /**
   * Notification codes representing scheduled reminder notifications.
   * <p>
   * These notifications are subject to user email preferences under
   * emailNotificationPreferences.reminders. When triggered, the
   * NotificationManager checks whether reminders are enabled and whether
   * the NOTIFICATION_DAY property matches one of the configured offsetDays.
   */
  private static final Set<NotificationCode> REMINDER_CODES = Set.of(
    SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
    SCHEDULED_PERMIT_EXPIRATION_APPROACHING_WITH_RENEW,
    SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
    SCHEDULED_CATALOG_WITHDRAWN_APPROACHING
  );

  public boolean isReminder() {
    return REMINDER_CODES.contains(this);
  }
}
