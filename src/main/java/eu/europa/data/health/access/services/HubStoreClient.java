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

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.europa.data.health.access.Constants.PARAMETER_ID;
import static eu.europa.data.health.access.Constants.PARAMETER_TYPE;
import static eu.europa.data.health.access.Constants.DATA;


/**
 * The HubStoreClient class provides methods to interact with a hub store API for uploading files.
 */
public class HubStoreClient {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final WebClient client;
  private final String apiUrl;
  private final String apiToken;

  public HubStoreClient(WebClient client, String apiUrl, String apiToken) {
    this.client = client;
    this.apiUrl = apiUrl;
    this.apiToken = apiToken;
  }

  /**
   * Uploads a file to the hub store API.
   *
   * @param parameterType the name of the parameter to use
   * @param parameterId the ID of the parameter given
   * @param fileName the name of the file
   * @param fileBuffer the buffer containing the file data
   * @param contentType the content type of the file
   * @return a Future representing the result of the file upload, containing a JsonObject on success or a failure message on failure
   */
  public Future<JsonObject> uploadFile(String parameterType, String parameterId, String fileName, Buffer fileBuffer, String contentType) {
    MultipartForm form = MultipartForm.create()
      .binaryFileUpload("data", fileName, fileBuffer, contentType);

    log.info("Uploading file to hub store: parameterType={}, parameterId={}, fileName={}, contentType={}, url={}/data", parameterType, parameterId, fileName, contentType, apiUrl);

    return client.postAbs(apiUrl + "/data")
      .addQueryParam(PARAMETER_TYPE, parameterType)
      .addQueryParam(PARAMETER_ID, parameterId)
      .putHeader("X-API-Key", apiToken)
      .putHeader("Content-Type", "multipart/form-data")
      .sendMultipartForm(form)
      .compose(response -> {
        if (response.statusCode() == 201) {
          log.info("File uploaded successfully: parameterType={}, parameterId={}, fileName={}, contentType={}", parameterType, parameterType, fileName, contentType);
          return Future.succeededFuture(response.bodyAsJsonObject());
        } else {
          log.error("Failed to upload file: parameterType={}, parameterId={}, fileName={}, contentType={}, status={}", parameterType, parameterType, fileName, contentType, response.statusCode());
          return Future.failedFuture("Failed to upload file: " + response.statusMessage());
        }
      });
  }

  /**
   * Get the file by id
   * @param fileId the id of the file
   * @return the document payload
   */
  public Future<HttpResponse<Buffer>> getFile(String fileId, String parameterType, String parameterId) {

    log.info("Downloading file from hub store: fileId={}, url={}{}{}", fileId, apiUrl, DATA, fileId);
    HttpRequest<Buffer> request =  client.getAbs(apiUrl + DATA );

    if(fileId != null){
      request.addQueryParam("id", fileId);
    }
    if(parameterType != null){
      request.addQueryParam(PARAMETER_TYPE, parameterType);
    }
    if(parameterId != null){
      request.addQueryParam(PARAMETER_ID, parameterId);
    }

     return  request.send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          log.info("File downloaded successfully: fileId={}", fileId);
          return Future.succeededFuture(response);
        } else {
          log.error("Failed to download file: fileId={}, status={}, body={}", fileId, response.statusCode(), response.bodyAsString());
          return Future.failedFuture("Failed to download file: " + response.statusMessage());
        }
      });
  }

  /**
   * Delete the file by id
   * @param fileId the id of the file
   */
  public Future<HttpResponse<Buffer>> deleteFile(String fileId, String parameterType, String parameterId) {
    log.info("Deleting file from hub store: fileId={}, url={}{}{}", fileId, apiUrl, DATA, fileId);
    return client.deleteAbs(apiUrl + DATA + fileId)
      .putHeader("X-API-Key", apiToken)
      .addQueryParam(PARAMETER_TYPE, parameterType)
      .addQueryParam(PARAMETER_ID, parameterId)
      .send()
      .compose(response -> {
        if (response.statusCode() == 202) {
          log.info("File deleted successfully: fileId={}", fileId);
          return Future.succeededFuture(response);
        } else {
          log.error("Failed to delete file: fileId={}, status={}, body={}", fileId, response.statusCode(), response.bodyAsString());
          return Future.failedFuture("Failed to delete file: " + response.statusMessage());
        }
      });
  }

  /**
   * Delete the file by parameter
   * @param parameterType Parameter type name
   * @param parameterId Parameter type id
   */

  public Future<HttpResponse<Buffer>> deleteFileByParameter(String parameterType, String parameterId) {
    log.info("Deleting file from hub store: parameterType={}, parameterId{},  url={}{}",parameterType, parameterId, apiUrl, DATA);
    return client.deleteAbs(apiUrl + DATA)
      .putHeader("X-API-Key", apiToken)
      .addQueryParam(PARAMETER_TYPE, parameterType)
      .addQueryParam(PARAMETER_ID, parameterId)
      .send()
      .compose(response -> {
        if (response.statusCode() == 202) {
          log.info("File deleted successfully: parameterType={}, parameterId{}", parameterType, parameterId);
          return Future.succeededFuture(response);
        } else {
          log.error("Failed to delete file: parameterType={}, parameterId{}, status={}, body={}", parameterType, parameterId, response.statusCode(), response.bodyAsString());
          return Future.failedFuture("Failed to delete file: " + response.statusMessage());
        }
      });
  }
}
