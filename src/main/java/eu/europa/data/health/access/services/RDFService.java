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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Converts an RDF from one format to another. Responds with:
   * <ul>
   *   <li><code>200 OK</code> if the conversion is successful, with the converted RDF in the response body</li>
   *   <li><code>400 Bad Request</code> if the input RDF is empty or has invalid syntax</li>
   *   <li><code>500 Internal Server Error</code> if an unexpected error occurs during conversion</li>
   * </ul>
   *
   * @param context      The routing context containing the request and response.
   * @param inputFormat  The input RDF format (e.g., Lang.JSONLD).
   * @param outputFormat The desired output RDF format (e.g., Lang.RDFXML).
   *
   */
  public void convertRDF(RoutingContext context, Lang inputFormat, Lang outputFormat) {
    String rdfPayload = context.body().asString();
    if (StringUtils.isBlank(rdfPayload)) {
      context.response()
        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
        .end("Empty Turtle payload");
      return;
    }
    try {
      Model model = ModelFactory.createDefaultModel();
      RDFDataMgr.read(model, new StringReader(rdfPayload), null, inputFormat);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      RDFDataMgr.write(out, model, outputFormat);
      String output = out.toString(StandardCharsets.UTF_8);
      context.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, outputFormat.getContentType().getContentTypeStr())
        .end(output);
    } catch (Exception e) {
      if (e.getClass().getName().contains("Riot")) {
        context.response()
          .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
          .end("Invalid syntax: " + e.getMessage());
      } else {
        log.error("Failed to convert RDF", e);
        context.response()
          .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
          .end("Conversion error");
      }
    }
  }
}
