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

import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.CONFIGURATION_RETRIES;
import static eu.europa.data.health.access.Constants.CONFIGURATION_STATUS;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_TIME_FORMAT;
import static eu.europa.data.health.access.Constants.DESC;
import static eu.europa.data.health.access.Constants.DISPATCHER_COLLECTION;
import static eu.europa.data.health.access.Constants.FAILED;
import static eu.europa.data.health.access.Constants.LIMIT;
import static eu.europa.data.health.access.Constants.PAGE;
import static eu.europa.data.health.access.Constants.PENDING;
import static eu.europa.data.health.access.Constants.SORT_BY;
import static eu.europa.data.health.access.Constants.SORT_ORDER;
import static eu.europa.data.health.access.Constants.STATUS;

import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdministrationTasksManager extends AbstractManager{

  public static final String DISPATCHER_RECORD_WITH_ID = "Dispatcher record with id: ";
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final MongoClient mongoClient;

  public AdministrationTasksManager(DBHandler dbHandler, MongoClient mongoClient) {
    super(dbHandler);
    this.mongoClient = mongoClient;
  }


  public void retrySubmitAction(@NotNull RoutingContext context){

    String applicationId = context.pathParam(APPLICATION_ID);
    JsonObject query = new JsonObject().put("application._id", applicationId);
    mongoClient.findOne(DISPATCHER_COLLECTION, query, null)
      .onSuccess(result -> {

        if(result == null){
          log.error(
            DISPATCHER_COLLECTION + " Dispatcher record with id: {} not found", applicationId);
          setUpMessageContextResponse(context, HttpResponseStatus.NOT_FOUND,
            DISPATCHER_RECORD_WITH_ID + applicationId + " not found");
        } else {
          JsonObject configuration = result.getJsonObject("configuration");
          String status = configuration.getString("status");
          if(Strings.CI.equals(status, FAILED)){

            String dispatcherRecordId = result.getString("_id");

            JsonObject resultObjectId = new JsonObject().put("_id", dispatcherRecordId);
            JsonObject update = new JsonObject().put("$set", new JsonObject()
              .put(CONFIGURATION_STATUS, PENDING)
              .put(CONFIGURATION_RETRIES, 0)
              .put("configuration.resubmission_date", getCurrentTimestamp()));

            mongoClient.updateCollection(DISPATCHER_COLLECTION, resultObjectId, update, res -> {
              if (res.succeeded()) {
                log.info(DISPATCHER_COLLECTION
                  + " Application dispatcher record : {} resubmitted", dispatcherRecordId);
                setUpMessageContextResponse(context, HttpResponseStatus.OK,
                  DISPATCHER_RECORD_WITH_ID + applicationId + " has been resubmitted");
              } else {
                log.error(DISPATCHER_COLLECTION
                  + " Failed to update application dispatcher record for retry, with error message {}", res.cause().getMessage());
                setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                  "Internal server error occurred with message:" + res.cause().getMessage());
              }
            });

          } else {
            log.info(DISPATCHER_COLLECTION
              + "Provided application {} is not in failed status", applicationId);
            setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
              DISPATCHER_RECORD_WITH_ID + applicationId + " must be in failed status");
          }
        }
      })
      .onFailure(fail -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, fail.getMessage()));
  }

  public void getAllDataAccessApplications(RoutingContext context){
    getAllDataApplications(context, DATA_ACCESS_APPLICATION_COLLECTION);
  }

  public void getAllDataRequestApplications(@NotNull RoutingContext context) {
    getAllDataApplications(context, DATA_REQUEST_APPLICATION_COLLECTION);
  }

  private void getAllDataApplications(@NotNull RoutingContext context, String collection){
    Integer pageSize = null;
    Integer pageLimit = null;
    String pageString = context.request().getParam(PAGE);
    String limitString = context.request().getParam(LIMIT);
    String sortByString = context.request().getParam(SORT_BY);
    String sortOrderString = context.request().getParam(SORT_ORDER);
    String status = context.request().getParam(STATUS);

    if (StringUtils.isNotEmpty(pageString) && StringUtils.isNotEmpty(limitString)) {
      pageSize = Integer.parseInt(pageString);
      pageLimit = Integer.parseInt(limitString);
    }

    super.getAllRecordsByAdmin(context, collection, pageSize, pageLimit,
      StringUtils.isNotEmpty(sortByString) ? sortByString : DATE_CREATED,
      StringUtils.isNotEmpty(sortOrderString) ? sortOrderString : DESC,
      status);
  }

  private String getCurrentTimestamp(){
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    return now.format(formatter);
  }
}
