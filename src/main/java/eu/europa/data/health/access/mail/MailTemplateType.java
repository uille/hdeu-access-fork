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

import lombok.Getter;

@Getter
public enum MailTemplateType {

  APPLICATION_NOTIFICATION(MailTemplates.APPLICATION_NOTIFICATION, "Application Update"),
  DATA_PERMIT_NOTIFICATION(MailTemplates.DATA_PERMIT_NOTIFICATION, "Data Permit Update"),
  COLLABORATION_NOTIFICATION(MailTemplates.COLLABORATION_NOTIFICATION, "Collaboration Update"),
  REMINDER_NOTIFICATION(MailTemplates.REMINDER_NOTIFICATION, "Reminder");

  public static final String DATASET_DECISION_SUBJECT = "Dataset Decision Update";

  private final String templateName;
  private final String defaultSubject;

  MailTemplateType(String templateName, String defaultSubject) {
    this.templateName = templateName;
    this.defaultSubject = defaultSubject;
  }

}
