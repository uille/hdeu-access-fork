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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailClient {

  private static final Logger log = LoggerFactory.getLogger(MailClient.class);

  public static final String MAIL_DATA_ENDPOINT = "/mail-data";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_DETAILS = "errorDetails";
  public static final int ERROR_CODE_REQUEST_FAILURE = -1;

  private final String mailServiceUrl;
  private final String xApiKey;
  private final WebClient vertxClient;

  public MailClient(String mailServiceUrl,
    String xApiKey,
    WebClient vertxClient) {
    this.mailServiceUrl = mailServiceUrl;
    this.xApiKey = xApiKey;
    this.vertxClient = vertxClient;
  }

  private MultiMap createHeaders(Buffer buffer) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("content-type", "multipart/form-data");
    headers.add("content-length", String.valueOf(buffer.length()));
    if (StringUtils.isNotEmpty(xApiKey)) {
      headers.add("X-API-Key", xApiKey);
    }
    return headers;
  }

  public Future<Void> sendMail(JsonObject mailPayload) {
    return sendMail(mailPayload, null);
  }

  public Future<Void> sendMail(JsonObject mailPayload, String correlationId) {
    Promise<Void> promise = Promise.promise();

    MultipartForm form = MultipartForm.create()
      .attribute("data", mailPayload.encode());

    Buffer buffer = Buffer.buffer(mailPayload.encode());
    MultiMap headers = createHeaders(buffer);

    vertxClient.postAbs(mailServiceUrl + MAIL_DATA_ENDPOINT)
      .putHeaders(headers)
      .sendMultipartForm(form, ar -> {
        if (ar.succeeded()) {
          int statusCode = ar.result().statusCode();
          if (statusCode == HttpResponseStatus.OK.code()
            || statusCode == HttpResponseStatus.NO_CONTENT.code()
            || statusCode == HttpResponseStatus.CREATED.code()) {

            log.info("[MAILCLIENT-SUCCESS] correlationId={} status={}",
              correlationId, statusCode);
            promise.complete();

          } else {
            log.error("[MAILCLIENT-FAILURE] correlationId={} status={} message={}",
              correlationId, statusCode, ar.result().bodyAsString());

            promise.fail(new JsonObject()
              .put(ERROR_CODE, statusCode)
              .put(ERROR_DETAILS, ar.result().bodyAsString())
              .encode());
          }
        } else {
          log.error("[MAILCLIENT-FAILURE] correlationId={} status={} message={}",
            correlationId, ERROR_CODE_REQUEST_FAILURE, ar.cause().getMessage());

          promise.fail(new JsonObject()
            .put(ERROR_CODE, ERROR_CODE_REQUEST_FAILURE)
            .put(ERROR_DETAILS, ar.cause().getMessage())
            .encode());
        }
      });

    return promise.future();
  }

}
