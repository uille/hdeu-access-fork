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
package eu.europa.data.health.access.services;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiennialReportManager {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public void downloadBiennialReportDocument(RoutingContext context) {

    log.info("Will download Biennial Report Document");

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      InputStream templateInputStream = getClass().getResourceAsStream(
        "/templates/Biennial Report template_NZP.docx")) {

      if (templateInputStream == null) {
        log.error("Biennial Report Template not found");
        context.response().setStatusCode(500)
          .end(new JsonObject().put("result_message", "Biennial Report Template not found").encode());
        return;
      }

      WordprocessingMLPackage document = WordprocessingMLPackage.load(templateInputStream);
      document.save(outputStream);
      context.response().putHeader("Content-Length", String.valueOf(outputStream.size()))
        .putHeader("Content-Disposition",
          "attachment; filename=TEHDAS2_Task6.3_Biennial_report_template.docx")
        .putHeader("Content-Type",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        .end(Buffer.buffer(outputStream.toByteArray()));
      log.info("Biennial Report Document downloaded");

    } catch (Exception e) {
      log.error("Error was thrown during download of Biennial Report Document, with error message: {}",
        e.getMessage());
      context.response().setStatusCode(500).end();
    }
  }

}
