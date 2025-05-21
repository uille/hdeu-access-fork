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

import static eu.europa.data.health.access.Constants.APPLICATION_X_WWW_FORM_URLENCODED;
import static eu.europa.data.health.access.Constants.CLIENT_ID;
import static eu.europa.data.health.access.Constants.CLIENT_SECRET;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ADMIN;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_USER;
import static eu.europa.data.health.access.Constants.HEALTH_DATA_BODY_LIMIT;
import static eu.europa.data.health.access.SchemaFiles.COUNTRIES_RDF;

import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.hub_repo_client.HubRepoClient;
import eu.europa.data.health.access.scheduled_jobs.DispatcherScheduledJob;
import eu.europa.data.health.access.services.AdministrationTasksManager;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.BasketManager;
import eu.europa.data.health.access.services.CatalogueManager;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import eu.europa.data.health.access.services.DataRequestApplicationManager;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.DocumentExportDataAccessManager;
import eu.europa.data.health.access.services.DocumentExportDataRequestManager;
import eu.europa.data.health.access.services.HubSearchClient;
import eu.europa.data.health.access.services.HubStoreClient;
import eu.europa.data.health.access.services.TranslationManager;
import eu.europa.data.health.access.services.UserManager;
import eu.europa.data.health.access.validations.business_validators.DataAccessApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.business_validators.DataRequestApplicationBusinessValidationManager;
import eu.europa.data.health.access.validations.validators.AuditLogValidationManager;
import eu.europa.data.health.access.validations.validators.BasketValidationManager;
import eu.europa.data.health.access.validations.validators.CatalogueValidationManager;
import eu.europa.data.health.access.validations.validators.CommonApplicationValidationManager;
import eu.europa.data.health.access.validations.validators.DataAccessApplicationValidationManager;
import eu.europa.data.health.access.validations.validators.DataRequestApplicationValidationManager;
import eu.europa.data.health.access.validations.validators.DispatcherValidationManager;
import eu.europa.data.health.access.validations.validators.PermissionValidator;
import eu.europa.data.health.access.validations.validators.UserValidationManager;
import io.piveau.json.ConfigHelper;
import io.piveau.security.ApiKeyAuthProvider;
import io.piveau.security.PiveauAuth;
import io.piveau.security.PiveauAuthConfig;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.serviceproxy.ServiceException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The MainVerticle class of Vert.x
 */
public class MainVerticle extends AbstractVerticle {

  private final Logger log = LoggerFactory.getLogger(getClass());
  DBHandler dbHandler;
  MongoClient mongoClient;
  JsonObject config;
  private WebClient client;
  private Model countriesModel;
  private CountriesManager countriesManager;


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    loadConfig().compose(handler -> setupMongoDBConnection()).compose(handler -> startServer())
      .onSuccess(v -> {
        log.info("Health data access successfully launched");
        startPromise.complete();
      }).onFailure(startPromise::fail);
  }

  /**
   * Initialization of the Server Configuration
   */
  Future<Void> startServer() {
    Promise<Void> promise = Promise.promise();
    JsonObject authConfig = ConfigHelper.forConfig(config).forceJsonObject(Constants.ENV_PIVEAU_ACCESS_AUTH_CONFIG);

    countriesModel = getCountriesModel();
    countriesManager = new CountriesManager();

    PiveauAuth.create(vertx, new PiveauAuthConfig(authConfig)).compose(piveauAuth -> {
      client = createWebClient();

      return initializeRouter(piveauAuth, authConfig)
        .onSuccess(router -> startHttpServer(router))
        .onSuccess(v -> promise.complete())
        .onFailure(promise::fail);
    });

    return promise.future();
  }

  private WebClient createWebClient() {
    WebClientOptions options = new WebClientOptions().setSsl(true).setTrustAll(true);
    return WebClient.create(vertx, options);
  }

  private Future<Router> initializeRouter(PiveauAuth piveauAuth, JsonObject authConfig) {
    Promise<Router> promise = Promise.promise();

    RouterBuilder.create(vertx, "webroot/openapi-data-permit.yml").onSuccess(builder -> {
      setupRootHandlers(builder, authConfig, piveauAuth);
      setupSecurityHandlers(builder, piveauAuth);
      setupApiHandlers(builder);

      Router router = builder.createRouter();
      addHealthCheck(router);
      handleExceptions(router);
      promise.complete(router);
    }).onFailure(promise::fail);

    return promise.future();
  }

  private void setupRootHandlers(RouterBuilder builder, JsonObject authConfig, PiveauAuth piveauAuth) {
    Set<String> allowedHeaders = Set.of("x-requested-with", CONTENT_TYPE, "Authorization", "Accept", "X-API-Key");
    Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS);

    long bodyLimit = config.getLong(HEALTH_DATA_BODY_LIMIT, Constants.BODY_LIMIT);
    builder.rootHandler(CorsHandler.create().allowedHeaders(allowedHeaders).allowedMethods(allowedMethods).allowCredentials(true));
    builder.rootHandler(BodyHandler.create().setBodyLimit(bodyLimit));
    builder.rootHandler(StaticHandler.create());
    builder.rootHandler(createAuthTokenHandler(authConfig, piveauAuth));
  }

  private Handler<RoutingContext> createAuthTokenHandler(JsonObject authConfig, PiveauAuth piveauAuth) {
    return ctx -> {
      Cookie authTokenCookie = ctx.request().getCookie("authToken");
      if (authTokenCookie != null) {
        log.info("Auth token found in cookie for request {}", ctx.request().absoluteURI());
        String token = authTokenCookie.getValue();
        piveauAuth.authenticate(token).onSuccess(user -> {
          log.debug("authConfig: {}", authConfig);
          String clientId = authConfig.getString("clientId");
          String clientSecret = authConfig.getString("clientSecret");
          String serverUrl = authConfig.getJsonObject("tokenServerConfig")
            .getJsonObject("keycloak").getString("serverUrl");

          log.info("Introspecting token {} with client id {} and secret {} to server {}", token, clientId, clientSecret, serverUrl);
          client.postAbs(serverUrl + config.getString(Constants.ENV_KEYCLOAK_INTROSPECT_PATH))
            .putHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .sendForm(new HeadersMultiMap().add(CLIENT_ID, clientId).add(CLIENT_SECRET, clientSecret).add("token", token), ar -> {
              if (ar.succeeded()) {
                var response = ar.result();
                if (response.statusCode() == 200) {
                  JsonObject json = response.bodyAsJsonObject();
                  boolean isActive = json.getBoolean("active");
                  if (isActive) {
                    log.info("Token is active for user {}", user.principal().getString("preferred_username"));
                    ctx.request().headers().set("Authorization", "Bearer " + token);
                    ctx.setUser(user);
                    ctx.next();
                  } else {
                    log.warn("Token is not active for user {}", user.principal().getString("preferred_username"));
                    ctx.fail(401);
                  }
                } else {
                  log.error("Introspection failed: {}", response.bodyAsString());
                  ctx.fail(401);
                }
              } else {
                log.error("Introspection failed", ar.cause());
                ctx.fail(401);
              }
            });
        }).onFailure(err -> {
          log.error("Failed to authenticate user: {}", err.getMessage());
          ctx.fail(401, err);
        });
      } else {
        log.info("No auth token found in cookie for request {}", ctx.request().absoluteURI());
        ctx.next();
      }
    };
  }

  private void setupSecurityHandlers(RouterBuilder builder, PiveauAuth piveauAuth) {
    Map<String, List<String>> apiKeys = getApiKeys(Constants.ENV_HEALTH_DATA_ACCESS_API_KEY);
    builder.securityHandler("ApiKeyAuth", APIKeyHandler.create(new ApiKeyAuthProvider(apiKeys)).header("X-API-Key"));

    Map<String, List<String>> exportApiKeys = getApiKeys(Constants.EXPORT_DATA_ACCESS_API_KEY);
    builder.securityHandler("ApiKeyExportAuth", APIKeyHandler.create(new ApiKeyAuthProvider(exportApiKeys)).header("X-EXPORT-API-Key"));

    builder.securityHandler(Constants.BEARER_AUTH, piveauAuth.authHandler());

    builder.setOptions(new RouterBuilderOptions()
      .setOperationModelKey("operationModel")
      .setMountNotImplementedHandler(true)
      .setMountResponseContentTypeHandler(true));
  }

  private Map<String, List<String>> getApiKeys(String configKey) {
    Map<String, List<String>> apiKeys = new HashMap<>();
    if (config.containsKey(configKey)) {
      apiKeys.put(config.getString(configKey), Collections.singletonList("*"));
    } else {
      log.warn("No API key configured for {}", configKey);
    }
    return apiKeys;
  }

  private void setupApiHandlers(RouterBuilder builder) {
    AuthClient authClient = new AuthClient(config.getString(Constants.AUTH_URL), WebClient.create(vertx));
    HubStoreClient hubStoreClient = new HubStoreClient(WebClient.create(vertx), config.getString(Constants.HUB_STORE_UPLOAD_DATA_URL), config.getString(Constants.PIVEAU_STORE_API_KEY));
    DispatcherClient dispatcherClient = new DispatcherClient(config.getString(Constants.DISPATCHER_URL), config.getString(Constants.DISPATCHER_X_API_KEY), client);
    HubSearchClient hubSearchClient = new HubSearchClient(client, config);
    HubRepoClient hubRepoClient = new HubRepoClient(dbHandler, config.getString(Constants.HUB_REPO_URL), config.getString(Constants.HUB_REPO_X_API_KEY), client, dispatcherClient, hubStoreClient, hubSearchClient, countriesManager);

    AuditLogManager auditLogManager = new AuditLogManager(dbHandler, authClient);
    BasketManager basketManager = new BasketManager(dbHandler, mongoClient);
    CommonApplicationValidationManager commonApplicationValidationManager = new CommonApplicationValidationManager(dbHandler, mongoClient);
    DataAccessApplicationValidationManager daaValidation = new DataAccessApplicationValidationManager();
    DataAccessApplicationBusinessValidationManager daaBusinessValidation = new DataAccessApplicationBusinessValidationManager(mongoClient);
    DataAccessApplicationManager daaManager = new DataAccessApplicationManager(dbHandler, mongoClient, basketManager, hubStoreClient, vertx, daaValidation, daaBusinessValidation, auditLogManager);

    DataRequestApplicationValidationManager draValidation = new DataRequestApplicationValidationManager();
    DataRequestApplicationBusinessValidationManager draBusinessValidation = new DataRequestApplicationBusinessValidationManager(mongoClient);
    DataRequestApplicationManager draManager = new DataRequestApplicationManager(dbHandler, mongoClient, basketManager, hubStoreClient, vertx, draValidation, draBusinessValidation, auditLogManager);

    UserValidationManager userValidation = new UserValidationManager();
    UserManager userManager = new UserManager(dbHandler, mongoClient, dispatcherClient, auditLogManager, authClient);
    AdministrationTasksManager adminTasksManager = new AdministrationTasksManager(dbHandler, mongoClient);
    TranslationManager translationManager = new TranslationManager();

    AuditLogValidationManager auditLogValidation = new AuditLogValidationManager( config,hubSearchClient);
    BasketValidationManager basketValidation = new BasketValidationManager(hubSearchClient);

    DocumentExportDataRequestManager documentExportRequestManager = new DocumentExportDataRequestManager(translationManager, dbHandler, client, config);
    DocumentExportDataAccessManager documentExportAccessManager = new DocumentExportDataAccessManager(translationManager, dbHandler, client, config);

    DispatcherValidationManager dispatcherValidationManager = new DispatcherValidationManager();
    DispatcherManager dispatcherManager = new DispatcherManager(mongoClient, dbHandler);

    CatalogueManager catalogueManager = new CatalogueManager(dbHandler, hubRepoClient, hubStoreClient, dispatcherClient, vertx);
    CatalogueValidationManager catalogueValidationManager = new CatalogueValidationManager(
      hubRepoClient, dispatcherClient, vertx, countriesManager);

    PermissionValidator adminValidator = new PermissionValidator(List.of(DATA_ADMIN));
    PermissionValidator userValidator = new PermissionValidator(List.of(DATA_USER));

    setupHandlers(builder, userValidator, adminValidator, adminTasksManager, catalogueValidationManager, catalogueManager,
      commonApplicationValidationManager, dispatcherManager, dispatcherValidationManager,
      draValidation, draManager, draBusinessValidation,
      daaValidation, daaManager, daaBusinessValidation,
      basketValidation, basketManager, userValidation, userManager,
      translationManager, auditLogValidation, auditLogManager,
      authClient, documentExportRequestManager, documentExportAccessManager);

    scheduleCronJobForDataAccessApplication(dispatcherManager, dispatcherClient);
    scheduleCronJobForDataRequestApplication(dispatcherManager, dispatcherClient);
  }

  private void addHealthCheck(Router router) {
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("health-check", future -> future.complete(Status.OK()));
    router.get("/health").handler(healthCheckHandler);
  }

  private void startHttpServer(Router router) {
    vertx.createHttpServer(new HttpServerOptions().setPort(8080))
      .requestHandler(router)
      .listen()
      .onSuccess(server -> log.info("HTTP server successfully launched."))
      .onFailure(error -> log.error("HTTP server failed to launch: ", error));
  }

  private void setupHandlers(RouterBuilder builder,
    PermissionValidator userPermissionValidator, PermissionValidator adminPermissionValidator,
    AdministrationTasksManager administrationTasksManager, CatalogueValidationManager catalogueValidationManager,
    CatalogueManager catalogueManager, CommonApplicationValidationManager commonApplicationValidationManager,
    DispatcherManager dispatcherManager, DispatcherValidationManager dispatcherValidationManager,
    DataRequestApplicationValidationManager dataRequestApplicationValidationManager,
    DataRequestApplicationManager dataRequestApplicationManager,
    DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager,
    DataAccessApplicationValidationManager dataAccessApplicationValidationManager,
    DataAccessApplicationManager dataAccessApplicationManager,
    DataAccessApplicationBusinessValidationManager dataAccessApplicationBusinessValidationManager,
    BasketValidationManager basketValidationManager, BasketManager basketManager,
    UserValidationManager userValidationManager, UserManager userManager,
    TranslationManager translationManager, AuditLogValidationManager auditLogValidationManager,
    AuditLogManager auditLogManager, AuthClient authClient, DocumentExportDataRequestManager documentExportDataRequestManager,
    DocumentExportDataAccessManager documentExportDataAccessManager){

    setupDataAccessApplicationManagerHandler(builder, userPermissionValidator,
      dataAccessApplicationValidationManager, commonApplicationValidationManager,
      dataAccessApplicationManager, dataAccessApplicationBusinessValidationManager);

    setupDataRequestApplicationManagerHandler(builder, userPermissionValidator,
      dataRequestApplicationValidationManager, commonApplicationValidationManager,
      dataRequestApplicationManager, dataRequestApplicationBusinessValidationManager);

    setupBasketManagerHandler(builder, userPermissionValidator,
      basketValidationManager, basketManager);

    setupCountriesHandler(builder, userPermissionValidator);

    setupCountriesTranslationHandler(builder);

    setupTranslationManagerHandler(builder, userPermissionValidator,
      translationManager);

    setupUserManagerHandler(builder, userPermissionValidator, adminPermissionValidator,
      userValidationManager, userManager);

    setupAuditLogManagerHandler(builder, adminPermissionValidator,
      auditLogValidationManager, auditLogManager);

    setupAuthClientHandler(builder, adminPermissionValidator,
      authClient);

    setupDocumentExportDataRequestManagerHandler(builder, userPermissionValidator,
      documentExportDataRequestManager);

    setupDocumentExportDataAccessManagerHandler(builder, userPermissionValidator, documentExportDataAccessManager);

    setupDispatcherManagerHandler(builder, dispatcherManager,
      dispatcherValidationManager);

    setupCatalogueManagerHandler(builder, adminPermissionValidator,
      catalogueValidationManager, catalogueManager);

    setupAdministrationTasksManagerHandler(builder,
      adminPermissionValidator, administrationTasksManager);
  }

  private void setupCountriesHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator){
    builder.operation("getCountries")
      .handler(userPermissionValidator)
      .handler(this::getCountries);
  }

  private void setupCountriesTranslationHandler(RouterBuilder builder){
    builder.operation("getCountriesTranslation")
      .handler(this::getCountriesTranslation);
  }

  private void setupAdministrationTasksManagerHandler(RouterBuilder builder,
    PermissionValidator adminPermissionValidator, AdministrationTasksManager administrationTasksManager){
    builder.operation("resubmitApplication")
      .handler(adminPermissionValidator)
      .handler(administrationTasksManager::retrySubmitAction);
    builder.operation("getAllDataAccessApplicationsByAdmin")
      .handler(adminPermissionValidator)
      .handler(administrationTasksManager::getAllDataAccessApplications);
    builder.operation("getAllDataRequestApplicationsByAdmin")
      .handler(adminPermissionValidator)
      .handler(administrationTasksManager::getAllDataRequestApplications);
  }

  private void setupCatalogueManagerHandler(RouterBuilder builder, PermissionValidator adminPermissionValidator,
    CatalogueValidationManager catalogueValidationManager, CatalogueManager catalogueManager){
    builder.operation("validateUpdateCatalogue")
      .handler(adminPermissionValidator)
      .handler(catalogueValidationManager::validateUpdate)
      .handler(catalogueManager::validateUpdateCatalogue);
    builder.operation("createCatalogue")
      .handler(adminPermissionValidator)
      .handler(catalogueValidationManager::validateUpdate)
      .handler(catalogueManager::createCatalogue);
    builder.operation("updateCatalogue")
      .handler(adminPermissionValidator)
      .handler(catalogueValidationManager::validateUpdate)
      .handler(catalogueManager::updateCatalogue);
    builder.operation("getCatalogue")
      .handler(adminPermissionValidator)
      .handler(catalogueManager::getCatalogue);
    builder.operation("getAllCatalogue")
      .handler(adminPermissionValidator)
      .handler(catalogueValidationManager::validateSearchCatalogueJsonSchema)
      .handler(catalogueManager::getAllCatalogues);
    builder.operation("deleteCatalog")
      .handler(adminPermissionValidator)
      .handler(catalogueValidationManager::validateCatalogMappingForDeletion)
      .handler(catalogueManager::deleteCatalog);
    builder.operation("postDataCreateCatalogue")
      .handler(adminPermissionValidator)
      .handler(catalogueValidationManager::validateFileCatalogue)
      .handler(catalogueManager::uploadRDFFile);
    builder.operation("getAllGeographicalCoverage")
      .handler(adminPermissionValidator)
      .handler(catalogueManager::getAllGeographicalCoverage);
    builder.operation("getRDFFileData")
      .handler(adminPermissionValidator)
      .handler(catalogueManager::getRDFFileData);
    builder.operation("getRDFFileMetadata")
      .handler(adminPermissionValidator)
      .handler(catalogueManager::getRDFFileMetadata);
  }

  private void setupDispatcherManagerHandler(RouterBuilder builder, DispatcherManager dispatcherManager,
    DispatcherValidationManager dispatcherValidationManager) {
    builder.operation("postStatusFromDispatcher")
      .handler(dispatcherManager::updateApplicationStatusFromDispatcher);
    builder.operation("putStatusFromDispatcher")
      .handler(dispatcherValidationManager::validateDispatcherDecisionJsonSchema)
      .handler(dispatcherManager::updateDatasetStatusFromDispatcher);
  }

  private void setupTranslationManagerHandler(RouterBuilder builder, PermissionValidator userPermissionValidator,
    TranslationManager translationManager) {
    builder.operation("getTranslations")
      .handler(userPermissionValidator)
      .handler(translationManager::getTranslationForSection);
  }

  private void setupAuthClientHandler(RouterBuilder builder, PermissionValidator adminPermissionValidator,
    AuthClient authClient) {
    builder.operation("searchUsers")
      .handler(adminPermissionValidator)
      .handler(authClient::validateUsersSearchFilters)
      .handler(authClient::searchUsers);

    builder.operation("updateUserRole")
      .handler(adminPermissionValidator)
      .handler(authClient::updateUserRole);
  }

  private void setupAuditLogManagerHandler(RouterBuilder builder,
    PermissionValidator adminPermissionValidator, AuditLogValidationManager auditLogValidationManager,
    AuditLogManager auditLogManager) {

    builder.operation("submitDatasetAuditLog")
      .handler(auditLogValidationManager::validateSubmitAuditLogJsonSchema)
      .handler(auditLogValidationManager::validateDatasetExists)
      .handler(auditLogManager::submitDatasetAuditLog);
    builder.operation("searchAuditLogs")
      .handler(adminPermissionValidator)
      .handler(auditLogValidationManager::validateSearchAuditLogJsonSchema)
      .handler(auditLogValidationManager::validateFilters)
      .handler(auditLogManager::searchAuditLogs);
  }

  private void setupDocumentExportDataAccessManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator, DocumentExportDataAccessManager documentExportDataAccessManager) {
    builder.operation("exportDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(documentExportDataAccessManager::exportApplicationDataAccessForm);
    //external export api call
    builder.operation("externalExportDataAccessApplication")
      .handler(documentExportDataAccessManager::externalExportApplicationForm);
    //Download data permit document
    builder.operation("downloadDataPermitDocument")
      .handler(documentExportDataAccessManager::downloadDataPermitDocument);
  }

  private void setupDocumentExportDataRequestManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator, DocumentExportDataRequestManager documentExportDataRequestManager) {

    builder.operation("exportDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(documentExportDataRequestManager::exportApplicationDataRequestForm);
    //external export api call
    builder.operation("externalExportDataRequestApplication")
      .handler(documentExportDataRequestManager::externalExportApplicationForm);
  }

  private void setupUserManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator, PermissionValidator adminPermissionValidator,
    UserValidationManager userValidationManager, UserManager userManager) {
    builder.operation("putUser")
      .handler(userPermissionValidator)
      .handler(userValidationManager::validateCreateOrUpdateUserJsonSchema)
      .handler((userValidationManager::validateCreateOrUpdateUser))
      .handler(userManager::createOrUpdateUser);
    builder.operation("getUser")
      .handler(userPermissionValidator)
      .handler(userManager::getUser);
    builder.operation("deleteUser")
      .handler(userPermissionValidator)
      .handler(userManager::deleteUser);
    builder.operation("adminDeleteUser")
      .handler(adminPermissionValidator)
      .handler(userManager::adminDeleteUser);
  }

  private void setupBasketManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator, BasketValidationManager basketValidationManager,
    BasketManager basketManager) {

    builder.operation("putBasketDataset")
      .handler(userPermissionValidator)
      .handler(basketValidationManager::validateBasketJsonSchema)
      .handler(basketValidationManager::validateDatasetAndDistributionExist)
      .handler(basketManager::createOrUpdateBasketDataset);
    builder.operation("getBasketDatasetByDatasetId")
      .handler(userPermissionValidator)
      .handler(basketManager::getBasketDatasetByDatasetId);
    builder.operation("getBasketDatasets")
      .handler(userPermissionValidator)
      .handler(basketManager::getAllBasketDatasets);
    builder.operation("deleteBasketDatasetByDatasetId")
      .handler(userPermissionValidator)
      .handler(basketManager::deleteBasketDatasetByDatasetId);
    builder.operation("deleteBasketDatasets")
      .handler(userPermissionValidator)
      .handler(basketManager::deleteAllBasketDatasets);
    builder.operation("deleteDistributionByDistributionId")
      .handler(userPermissionValidator)
      .handler(basketValidationManager::validateBasketJsonSchema)
      .handler(basketManager::deleteDistributionByDistributionId);
    builder.operation("getDistributionCount")
      .handler(userPermissionValidator)
      .handler(basketManager::getDistributionCount);
  }

  private void setupDataRequestApplicationManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator,
    DataRequestApplicationValidationManager dataRequestApplicationValidationManager,
    CommonApplicationValidationManager commonApplicationValidationManager,
    DataRequestApplicationManager dataRequestApplicationManager,
    DataRequestApplicationBusinessValidationManager dataRequestApplicationBusinessValidationManager) {

    builder.operation("postDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateDataAccessApplicationJsonSchema)
      .handler(context -> commonApplicationValidationManager.validateCreate(context,
        DATA_REQUEST_APPLICATION_COLLECTION))
      .handler(dataRequestApplicationManager::createApplication);
    builder.operation("getDataRequestApplicationById")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getApplicationById);
    builder.operation("getAllDataRequestApplications")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getAllDataRequestApplications);
    builder.operation("putDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::updateApplication);
    builder.operation("patchDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::patchApplication);
    builder.operation("deleteDataRequestApplicationById")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::deleteDataRequestApplicationById);
    builder.operation("deleteAllDataRequestApplications")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::deleteAllDataRequestApplication);
    builder.operation("putDistributionsDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(
        dataRequestApplicationValidationManager::validatePutDistributionsApplicationJsonSchema)
      .handler(dataRequestApplicationManager::putDistributionsApplication);
    builder.operation("deleteDataRequestDatasets")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateDeleteDatasetsJsonSchema)
      .handler(dataRequestApplicationManager::deleteDatasets);
    builder.operation("deleteDataRequestDistributions")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateDeleteDistributionsJsonSchema)
      .handler(dataRequestApplicationManager::deleteDistributions);
    builder.operation("putSectionDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::validateFileBeforeUpdate)
      .handler(dataRequestApplicationValidationManager::validateUpdate)
      .handler(dataRequestApplicationBusinessValidationManager::businessValidateUpdate)
      .handler(dataRequestApplicationManager::createOrUpdateSection);
    builder.operation("getSectionDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getSection);
    builder.operation("getDataRequestSectionErrors")
      .handler(dataRequestApplicationValidationManager::validateUpdate)
      .handler(dataRequestApplicationBusinessValidationManager::businessValidateUpdate)
      .handler(dataRequestApplicationManager::getSectionErrors);
    builder.operation("postRequestData")
      .handler(userPermissionValidator)
      .handler(commonApplicationValidationManager::validateFile)
      .handler(dataRequestApplicationManager::postData);
    builder.operation("getRequestData")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getData);
    builder.operation("deleteRequestData")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::deleteData);
    builder.operation("putStatusDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataRequestApplicationManager::changeStatus);
    builder.operation("putTitleDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateChangeTitleJsonSchema)
      .handler(dataRequestApplicationValidationManager::validateUpdateTitle)
      .handler(dataRequestApplicationManager::changeTitle);
  }

  private void setupDataAccessApplicationManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator,
    DataAccessApplicationValidationManager dataAccessApplicationValidationManager,
    CommonApplicationValidationManager commonApplicationValidationManager,
    DataAccessApplicationManager dataAccessApplicationManager,
    DataAccessApplicationBusinessValidationManager dataAccessApplicationBusinessValidationManager
  ) {
    builder.operation("postDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateDataAccessApplicationJsonSchema)
      .handler(context -> commonApplicationValidationManager.validateCreate(context,
        DATA_ACCESS_APPLICATION_COLLECTION))
      .handler(dataAccessApplicationManager::createApplication);
    builder.operation("getDataAccessApplicationById")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getApplicationById);
    builder.operation("getAllDataAccessApplications")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getAllDataAccessApplications);
    builder.operation("putDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::updateApplication);
    builder.operation("patchDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::patchApplication);
    builder.operation("deleteDataAccessApplicationById")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::deleteDataAccessApplicationById);
    builder.operation("deleteAllDataAccessApplications")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::deleteAllDataAccessApplication);
    builder.operation("putDistributionsApplication")
      .handler(userPermissionValidator).handler(
        dataAccessApplicationValidationManager::validatePutDistributionsApplicationJsonSchema)
      .handler(dataAccessApplicationManager::putDistributionsApplication);
    builder.operation("deleteDatasets")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateDeleteDatasetsJsonSchema)
      .handler(dataAccessApplicationManager::deleteDatasets);
    builder.operation("deleteDistributions")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateDeleteDistributionsJsonSchema)
      .handler(dataAccessApplicationManager::deleteDistributions);
    builder.operation("putSectionApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::validateFileBeforeUpdate)
      .handler(dataAccessApplicationValidationManager::validateUpdate)
      .handler(dataAccessApplicationBusinessValidationManager::businessValidateUpdate)
      .handler(dataAccessApplicationManager::createOrUpdateSection);
    builder.operation("getSectionApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getSection);
    builder.operation("getSectionErrors")
      .handler(dataAccessApplicationValidationManager::validateUpdate)
      .handler(dataAccessApplicationBusinessValidationManager::businessValidateUpdate)
      .handler(dataAccessApplicationManager::getSectionErrors);
    builder.operation("postData")
      .handler(userPermissionValidator)
      .handler(commonApplicationValidationManager::validateFile)
      .handler(dataAccessApplicationManager::postData);
    builder.operation("getData")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getData);
    builder.operation("deleteData")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::deleteData);
    builder.operation("putStatusApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataAccessApplicationManager::changeStatus);
    builder.operation("putTitleApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeTitleJsonSchema)
      .handler(dataAccessApplicationValidationManager::validateUpdateTitle)
      .handler(dataAccessApplicationManager::changeTitle);
  }

  private void scheduleCronJobForDataAccessApplication(DispatcherManager dispatcherManager,
    DispatcherClient dispatcherClient) {
    int dataAccessDispatcherInterval = Constants.DATA_ACCESS_DISPATCHER_DEFAULT_INTERVAL;
    if (config.getInteger(Constants.DATA_ACCESS_DISPATCHER_INTERVAL) != null) {
      dataAccessDispatcherInterval = config.getInteger(
        Constants.DATA_ACCESS_DISPATCHER_INTERVAL);
    }
    final int dataAccessDispatcherMaxRetries;
    if (config.getInteger(Constants.DATA_ACCESS_DISPATCHER_MAX_RETRIES) != null) {
      dataAccessDispatcherMaxRetries = config.getInteger(
        Constants.DATA_ACCESS_DISPATCHER_MAX_RETRIES);
    } else {
      dataAccessDispatcherMaxRetries = Constants.DATA_ACCESS_DISPATCHER_DEFAULT_MAX_RETRIES;
    }
    DispatcherScheduledJob dataAccessDispatcherScheduledJob = new DispatcherScheduledJob(
      mongoClient,
      dispatcherManager);
    vertx.setPeriodic(dataAccessDispatcherInterval,
      id -> dataAccessDispatcherScheduledJob.sendApplicationsToDispatcher(
        dataAccessDispatcherMaxRetries,
        dispatcherClient, DATA_ACCESS_APPLICATION_COLLECTION));
  }

  private void scheduleCronJobForDataRequestApplication(DispatcherManager dispatcherManager,
    DispatcherClient dispatcherClient) {
    int dataRequestDispatcherInterval = Constants.DATA_REQUEST_DISPATCHER_DEFAULT_INTERVAL;
    if (config.getInteger(Constants.DATA_REQUEST_DISPATCHER_INTERVAL) != null) {
      dataRequestDispatcherInterval = config.getInteger(
        Constants.DATA_REQUEST_DISPATCHER_INTERVAL);
    }
    final int dataRequestDispatcherMaxRetries;
    if (config.getInteger(Constants.DATA_REQUEST_DISPATCHER_MAX_RETRIES) != null) {
      dataRequestDispatcherMaxRetries = config.getInteger(
        Constants.DATA_REQUEST_DISPATCHER_MAX_RETRIES);
    } else {
      dataRequestDispatcherMaxRetries = Constants.DATA_REQUEST_DISPATCHER_DEFAULT_MAX_RETRIES;
    }
    DispatcherScheduledJob dataRequestDispatcherScheduledJob = new DispatcherScheduledJob(
      mongoClient,
      dispatcherManager);
    vertx.setPeriodic(dataRequestDispatcherInterval,
      id -> dataRequestDispatcherScheduledJob.sendApplicationsToDispatcher(
        dataRequestDispatcherMaxRetries,
        dispatcherClient, DATA_REQUEST_APPLICATION_COLLECTION));
  }

  /**
   * Handle http errors
   */
  public void handleExceptions(Router router) {
    router.errorHandler(304, this::handleRedirection).errorHandler(400, this::handleClientError)
      .errorHandler(401, this::handleUnauthorized).errorHandler(403, this::handleForbidden)
      .errorHandler(404, this::handleNotFound).errorHandler(415, this::handleUnsupportedMediaType)
      .errorHandler(422, this::handleUnprocessableEntity)
      .errorHandler(500, this::handleServerError);
  }

  private void handleRedirection(RoutingContext context) {
    context.response().setStatusCode(304);
    Object failure = context.failure();
    if (failure instanceof ServiceException se) {
      context.response().end(se.getMessage());
    } else {
      context.response().end();
    }
  }

  private void handleClientError(RoutingContext context) {
    context.response().setStatusCode(400);
    Object failure = context.failure();
    if (failure instanceof BadRequestException bre) {
      respondWithJson(context, bre.toJson().encodePrettily());
    } else if (failure instanceof ServiceException se) {
      context.response().end(se.getMessage());
    } else {
      context.response().end();
    }
  }

  private void handleUnauthorized(RoutingContext context) {
    context.response().setStatusCode(401);
    if (log.isDebugEnabled()) {
      log.debug("Unauthorized (401) request: {}", context.request().absoluteURI());
    }
    context.response().putHeader("WWW-Authenticate", "Bearer").end();
  }

  private void handleForbidden(RoutingContext context) {
    context.response().setStatusCode(403);
    if (log.isDebugEnabled()) {
      log.debug("Forbidden (403) request to {}", context.request().absoluteURI());
    }
    context.response().end();
  }

  private void handleNotFound(RoutingContext context) {
    context.response().setStatusCode(404);
    Object failure = context.failure();
    if (failure instanceof ServiceException se) {
      if (se.getDebugInfo() != null && !se.getDebugInfo().isEmpty()) {
        respondWithJson(context, se.getDebugInfo().encodePrettily());
      } else {
        context.response().end(se.getMessage());
      }
    } else {
      context.response().end(failure != null ? context.failure().getMessage() : "");
    }
  }

  private void handleUnsupportedMediaType(RoutingContext context) {
    context.response().setStatusCode(415);
    Object failure = context.failure();
    if (failure instanceof ServiceException se) {
      context.response().end(se.getMessage());
    } else {
      context.response().end();
    }
  }

  private void handleUnprocessableEntity(RoutingContext context) {
    context.response().setStatusCode(422);
    Object failure = context.failure();
    if (failure instanceof ServiceException se) {
      context.response().end(se.getMessage());
    } else {
      context.response().end();
    }
  }

  private void handleServerError(RoutingContext context) {
    context.response().setStatusCode(500);
    Object failure = context.failure();
    if (failure instanceof ServiceException se) {
      if (se.getDebugInfo() != null) {
        respondWithJson(context, se.getDebugInfo().encodePrettily());
      } else {
        context.response().end(se.getMessage());
      }
    } else {
      context.response().end();
    }
  }

  private void respondWithJson(RoutingContext context, String jsonResponse) {
    context.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(jsonResponse);
  }

  Future<Void> loadConfig() {
    Promise<Void> promise = Promise.promise();
    ConfigRetrieverOptions options = new ConfigRetrieverOptions();

    // configuration options from properties file
    String configPath = "conf/hd-access-env.properties";
    if (Files.exists(Paths.get(configPath))) {
      ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file")
        .setFormat("properties").setConfig(new JsonObject().put("path", configPath));
      options.addStore(fileStore);
    } else {
      log.warn("Configuration file not found: {}", configPath);
    }

    // configuration options from environment variables
    ConfigStoreOptions envStore = new ConfigStoreOptions().setType("env");
    options.addStore(envStore);

    ConfigRetriever.create(vertx, options).getConfig(handler -> {
      if (handler.succeeded()) {
        config = handler.result();
        promise.complete();
        log.debug("Successfully loaded configuration.");
      } else {
        promise.fail("Failed to load config: " + handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * Initialization of the MongoDB
   */
  Future<DBHandler> setupMongoDBConnection() {
    Promise<DBHandler> promise = Promise.promise();
    JsonObject dbConfig = new JsonObject().put("connection_string",
        config.getString(Constants.ENV_MONGO_DB_URI))
      .put("db_name", config.getString(Constants.ENV_MONGO_DB));

    if (log.isDebugEnabled()) {
      log.debug(dbConfig.encodePrettily());
    }

    mongoClient = createMongoClient(dbConfig);

    mongoClient.runCommand("ping", new JsonObject().put("ping", 1)).onFailure(err -> {
      log.error("Failed connecting to mongodb: {}", err.getMessage());
      mongoClient.close();
      vertx.close();
      promise.fail(err);
    }).onSuccess(list -> {
    dbHandler = new DBHandler(mongoClient);

      if (ObjectUtils.allNotNull(mongoClient)) {
        log.debug("Successfully started mongodb connection.");
      promise.complete(dbHandler);
    } else {
        log.error("Failed starting mongodb connection: {}", promise.future().result());
        promise.fail("No DB-Connection.");
    }

    });
    return promise.future();
  }

  /**
   * Initialize mongoClient
   */
  MongoClient createMongoClient(JsonObject config) {
    return MongoClient.createShared(vertx, config);
  }

  /**
   * Loads from the RDF model for the countries lists used in health data applications at start up
   * by reading the corresponding rdf file (countries-skos.rdf) from the filesystem
   *
   * @return the countries rdf model
   */
  Model getCountriesModel() {
    try (InputStream rdfFile = SchemaFiles.class.getClassLoader()
      .getResourceAsStream(COUNTRIES_RDF.getValue())) {
      Model model = ModelFactory.createDefaultModel();
      RDFDataMgr.read(model, Objects.requireNonNull(rdfFile), Lang.RDFXML);
      return model;
    } catch (
      IOException e) {
      log.error("Countries RDF file not found.", e);
      return null;
    }
  }

  /**
   * API method to return the json array with the required countries to the F/E
   *
   * @param context the routing context
   */
  private void getCountries(RoutingContext context) {
    String lang = context.pathParam("lg");
    String filter = context.pathParam("filter");

    if (Objects.nonNull(countriesModel)) {
      JsonArray countries = countriesManager.getCountriesJsonArray(countriesModel, lang, filter);
      context.response().putHeader("content-type", "application/json");
      context.response().setStatusCode(200).end(countries.encode());
    } else {
      context.response().setStatusCode(400).end("Countries model is null.");
    }
  }

  /**
   * API method to return the json array with the required countries translation to the F/E
   *
   * @param context the routing context
   */
  private void getCountriesTranslation(RoutingContext context) {
    String lang = context.pathParam("lg");

    if (Objects.nonNull(countriesModel)) {
      JsonArray countries = countriesManager.getCountriesJsonArray(countriesModel, lang, "euCountry");
      context.response().putHeader("content-type", "application/json");
      context.response().setStatusCode(200).end(countries.encode());
    } else {
      context.response().setStatusCode(400).end("Countries model is null.");
    }
  }

}
