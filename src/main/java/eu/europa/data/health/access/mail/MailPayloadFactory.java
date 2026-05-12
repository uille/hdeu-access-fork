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
package eu.europa.data.health.access.mail;

import io.vertx.core.json.JsonObject;
import java.util.Map;

public class MailPayloadFactory {

  private static final String PLACEHOLDER_BODY = "This is an automated notification.";

  private MailPayloadFactory() {
  }

  public static JsonObject applicationNotification(
      String to,
      ApplicationNotificationModel model,
      String subject
  ) {
    return buildPayload(
        to,
        MailTemplateType.APPLICATION_NOTIFICATION,
        model.toMap(),
        subject
    );
  }

  public static JsonObject dataPermitNotification(
      String to,
      DataPermitNotificationModel model,
      String subject
  ) {
    return buildPayload(
        to,
        MailTemplateType.DATA_PERMIT_NOTIFICATION,
        model.toMap(),
        subject
    );
  }

  public static JsonObject collaborationNotification(
      String to,
      CollaborationNotificationModel model,
      String subject
  ) {
    return buildPayload(
        to,
        MailTemplateType.COLLABORATION_NOTIFICATION,
        model.toMap(),
        subject
    );
  }

  public static JsonObject reminderNotification(
      String to,
      ReminderNotificationModel model,
      String subject
  ) {
    return buildPayload(
        to,
        MailTemplateType.REMINDER_NOTIFICATION,
        model.toMap(),
        subject
    );
  }

  private static JsonObject buildPayload(
      String to,
      MailTemplateType templateType,
      Map<String, Object> templateModel,
      String subject
  ) {
    return new JsonObject()
        .put("to", to)
        .put("subject", subject != null ? subject : templateType.getDefaultSubject())
        .put("body", PLACEHOLDER_BODY)
        .put("templateName", templateType.getTemplateName())
        .put("templateModel", templateModel);
  }
}
