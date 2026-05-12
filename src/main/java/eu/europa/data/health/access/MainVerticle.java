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
import static eu.europa.data.health.access.Constants.CATALOG_NOTIFICATIONS_INTERVAL;
import static eu.europa.data.health.access.Constants.CATALOG_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.CLIENT_ID;
import static eu.europa.data.health.access.Constants.CLIENT_SECRET;
import static eu.europa.data.health.access.Constants.CONTENT_TYPE;
import static eu.europa.data.health.access.Constants.DATASET_NOTIFICATIONS_INTERVAL;
import static eu.europa.data.health.access.Constants.DATASET_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_ADMIN;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_USER;
import static eu.europa.data.health.access.Constants.DATE_CREATED;
import static eu.europa.data.health.access.Constants.DATE_MODIFIED;
import static eu.europa.data.health.access.Constants.DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS_LABEL;
import static eu.europa.data.health.access.Constants.DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS_LABEL;
import static eu.europa.data.health.access.Constants.DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS_LABEL;
import static eu.europa.data.health.access.Constants.DEFAULT_DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS;
import static eu.europa.data.health.access.Constants.DEFAULT_DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS;
import static eu.europa.data.health.access.Constants.DEFAULT_DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS;
import static eu.europa.data.health.access.Constants.HEALTH_DATA_BODY_LIMIT;
import static eu.europa.data.health.access.Constants.PERMIT_NOTIFICATIONS_INTERVAL;
import static eu.europa.data.health.access.Constants.PERMIT_NOTIFICATION_TYPE;
import static eu.europa.data.health.access.Constants.USER_ID;
import static eu.europa.data.health.access.Constants.WITHDRAWN_TIME_PERIOD;
import static eu.europa.data.health.access.NotificationCode.SCHEDULED_CATALOG_WITHDRAWN_APPROACHING;
import static eu.europa.data.health.access.NotificationCode.SCHEDULED_DATASET_WITHDRAWN_APPROACHING;
import static eu.europa.data.health.access.NotificationCode.SCHEDULED_PERMIT_EXPIRATION_APPROACHING;
import static eu.europa.data.health.access.SchemaFiles.COUNTRIES_RDF;
import static eu.europa.data.health.access.services.CommonApplicationManager.STATUS_STATUS;

import eu.europa.data.health.access.auth_client.AuthClient;
import eu.europa.data.health.access.dispatcher_client.DispatcherClient;
import eu.europa.data.health.access.handlers.DBHandler;
import eu.europa.data.health.access.handlers.DynamicLogLevelHandler;
import eu.europa.data.health.access.hub_repo_client.HubRepoClient;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.hub_translation_client.HubTranslationClientActivities;
import eu.europa.data.health.access.mail.MailClient;
import eu.europa.data.health.access.manager.SchedulerManager;
import eu.europa.data.health.access.scheduled_jobs.DataAccessPermitJob;
import eu.europa.data.health.access.scheduled_jobs.DispatcherScheduledJob;
import eu.europa.data.health.access.scheduled_jobs.NotificationScheduledJob;
import eu.europa.data.health.access.scheduled_jobs.TranslationRetryScheduledJob;
import eu.europa.data.health.access.services.AdministrationTasksManager;
import eu.europa.data.health.access.services.AuditLogManager;
import eu.europa.data.health.access.services.BasketManager;
import eu.europa.data.health.access.services.BiennialReportManager;
import eu.europa.data.health.access.services.CatalogueManager;
import eu.europa.data.health.access.services.CollaboratorManager;
import eu.europa.data.health.access.services.ControlledVocabularyManager;
import eu.europa.data.health.access.services.CountriesManager;
import eu.europa.data.health.access.services.DataAccessApplicationManager;
import eu.europa.data.health.access.services.DataRequestApplicationManager;
import eu.europa.data.health.access.services.DdaDatasetManager;
import eu.europa.data.health.access.services.DecisionTranslationService;
import eu.europa.data.health.access.services.DispatcherManager;
import eu.europa.data.health.access.services.DocumentExportDataAccessManager;
import eu.europa.data.health.access.services.DocumentExportDataRequestManager;
import eu.europa.data.health.access.services.FavouriteManager;
import eu.europa.data.health.access.services.HubSearchClient;
import eu.europa.data.health.access.services.HubStoreClient;
import eu.europa.data.health.access.services.ITBValidatorClient;
import eu.europa.data.health.access.services.LockWebSocketManager;
import eu.europa.data.health.access.services.NotificationManager;
import eu.europa.data.health.access.services.PayloadManager;
import eu.europa.data.health.access.services.RDFService;
import eu.europa.data.health.access.services.SchedulerService;
import eu.europa.data.health.access.services.SchemaTranslationService;
import eu.europa.data.health.access.services.SectionLockManager;
import eu.europa.data.health.access.services.TranslationCallbackManager;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.mongo.IndexOptions;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MainVerticle class of Vert.x
 */
public class MainVerticle extends AbstractVerticle {

  public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
  private final Logger log = LoggerFactory.getLogger(getClass());
  DBHandler dbHandler;
  MongoClient mongoClient;
  JsonObject config;
  private WebClient client;
  private Model countriesModel;
  private CountriesManager countriesManager;
  private MailClient mailClient;

  /**
   * Singleton WebSocket manager for section lock broadcasting.
   * Created once in {@link #startServer()} and shared with {@link SectionLockManager}.
   * The same instance is also used in {@link #startHttpServer(Router)} to handle
   * incoming WebSocket upgrade requests.
   */
  private LockWebSocketManager lockWsManager;

  /**
   * Singleton section lock manager.
   * Stored as a field so {@link #handleLockWebSocket} can call
   * {@link SectionLockManager#releaseLocksForSession} on WS disconnect.
   */
  private SectionLockManager sectionLockManager;

  /**
   * Short-lived cache of recently-introspected auth tokens mapped to their expiry timestamp.
   *
   * <p>The {@link #createAuthTokenHandler} calls Keycloak's token introspection endpoint on
   * every cookie-authenticated request.  Since the section-locking feature added continuous
   * background requests (heartbeat every 60 s, polling every 120 s), this previously caused
   * one Keycloak HTTP call per heartbeat per active user — a significant and growing load.
   *
   * <p>Tokens that were confirmed active within the last
   * {@value #INTROSPECTION_CACHE_TTL_MS} ms are trusted without re-querying Keycloak.
   * JWT signature validation via {@code piveauAuth.authenticate()} still runs on every
   * request (local, no network); only the remote introspect call is skipped on cache hits.
   *
   * <p>Entries are lazily evicted when accessed after expiry, and a periodic Vert.x timer
   * flushes the entire map every 5 minutes to prevent unbounded growth.
   *
   * <p>Security tradeoff: a revoked token may be accepted for up to
   * {@value #INTROSPECTION_CACHE_TTL_MS} ms after revocation.  This is acceptable for the
   * section-lock endpoints (heartbeat / polling) where the only action is extending a lock
   * lease; critical write endpoints are protected by the lock ownership check in addition.
   */
  private final Map<String, Long> introspectionCache = new ConcurrentHashMap<>();

  /**
   * TTL for token introspection cache entries in milliseconds (60 s).
   * Must be shorter than the minimum Keycloak access-token lifetime.
   */
  private static final long INTROSPECTION_CACHE_TTL_MS = 60_000L;


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

    // Create the WebSocket manager for section lock broadcasting.
    // Shared between SectionLockManager (broadcasts events) and the HTTP server
    // (handles WebSocket upgrade requests from the frontend).
    lockWsManager = new LockWebSocketManager();

    // Flush expired token introspection cache entries every 5 minutes.
    // Prevents unbounded growth while keeping memory usage negligible.
    vertx.setPeriodic(300_000L, timerId -> flushExpiredIntrospectionCacheEntries());

    PiveauAuth.create(vertx, new PiveauAuthConfig(authConfig)).compose(piveauAuth -> {
      client = createWebClient();

      mailClient = new MailClient(
        config.getString(Constants.ENV_MAIL_SERVICE_BASE_URL),
        config.getString(Constants.ENV_MAIL_SERVICE_X_API_KEY),
        client
      );

      return initializeRouter(piveauAuth, authConfig)
        .onSuccess(this::startHttpServer)
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
    Set<String> allowedHeaders = Set.of("x-requested-with", CONTENT_TYPE, "Authorization", "Accept", "X-API-Key", "x-session-id");
    Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS);

    long bodyLimit = config.getLong(HEALTH_DATA_BODY_LIMIT, Constants.BODY_LIMIT);
    builder.rootHandler(CorsHandler.create().allowedHeaders(allowedHeaders).allowedMethods(allowedMethods).allowCredentials(true));
    builder.rootHandler(BodyHandler.create().setBodyLimit(bodyLimit));
    builder.rootHandler(StaticHandler.create());
    builder.rootHandler(createAuthTokenHandler(authConfig, piveauAuth));
  }

  Handler<RoutingContext> createAuthTokenHandler(JsonObject authConfig, PiveauAuth piveauAuth) {
    return ctx -> {
      Cookie authTokenCookie = ctx.request().getCookie("authToken");
      if (authTokenCookie != null) {
        log.debug("Auth token found in cookie for request {}", ctx.request().absoluteURI());
        String token = authTokenCookie.getValue();
        piveauAuth.authenticate(token).onSuccess(user -> {
          log.debug("authConfig: {}", authConfig);

          // --- Introspection cache check -------------------------------------------
          // Heartbeat (every 60 s) and polling (every 120 s) requests from the lock
          // feature arrive continuously for every active user. Without this cache,
          // each request triggers one Keycloak HTTP introspect call — significantly
          // increasing load at scale. JWT signature is still validated locally by
          // piveauAuth.authenticate() above; only the remote round-trip is skipped.
          Long cachedExpiry = introspectionCache.get(token);
          if (cachedExpiry != null && System.currentTimeMillis() < cachedExpiry) {
            log.debug("Auth: introspection cache hit for '{}' — skipping Keycloak call",
              user.principal().getString("preferred_username"));
            ctx.request().headers().set("Authorization", "Bearer " + token);
            ctx.setUser(user);
            ctx.next();
            return;
          }
          // -------------------------------------------------------------------------

          String clientId = authConfig.getString("clientId");
          String clientSecret = authConfig.getString("clientSecret");
          String serverUrl = authConfig.getJsonObject("tokenServerConfig")
            .getJsonObject("keycloak").getString("serverUrl");

          log.debug("Introspecting token {} with client id {} and secret {} to server {}", token, clientId, clientSecret, serverUrl);
          client.postAbs(serverUrl + config.getString(Constants.ENV_KEYCLOAK_INTROSPECT_PATH))
            .putHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .sendForm(new HeadersMultiMap().add(CLIENT_ID, clientId).add(CLIENT_SECRET, clientSecret).add("token", token), ar -> {
              if (ar.succeeded()) {
                var response = ar.result();
                if (response.statusCode() == 200) {
                  JsonObject json = response.bodyAsJsonObject();
                  boolean isActive = json.getBoolean("active");
                  if (isActive) {
                    log.debug("Token is active for user {}", user.principal().getString("preferred_username"));
                    // Cache confirmed-active token to avoid repeated Keycloak calls
                    introspectionCache.put(token, System.currentTimeMillis() + INTROSPECTION_CACHE_TTL_MS);
                    ctx.request().headers().set("Authorization", "Bearer " + token);
                    ctx.setUser(user);
                    ctx.next();
                  } else {
                    log.warn("Token is not active for user {}", user.principal().getString("preferred_username"));
                    introspectionCache.remove(token); // evict any stale entry
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
        log.debug("No auth token found in cookie for request {}", ctx.request().absoluteURI());
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

  Map<String, List<String>> getApiKeys(String configKey) {
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
    AuditLogManager auditLogManager = new AuditLogManager(dbHandler, authClient);

    String applicationsBaseUrl = config.getString(Constants.MAIL_APPLICATIONS_BASE_URL);
    NotificationManager notificationManager = new NotificationManager(dbHandler, mailClient, applicationsBaseUrl);

    TranslationManager translationManager = new TranslationManager(dbHandler, mongoClient);
    HubTranslationClient hubTranslationClient = new HubTranslationClient(
      config.getString(Constants.ENV_TRANSLATION_SERVICE_URL),
      config.getString(Constants.ENV_TRANSLATION_SERVICE_FILE_URL),
      config.getString(Constants.ENV_TRANSLATION_SERVICE_CALLBACK_URL),
      config.getString(Constants.ENV_TRANSLATION_SERVICE_X_API_KEY),
      config.getString(Constants.ENV_HEALTH_DATA_ACCESS_API_KEY),
      client,
      translationManager
    );
    authClient.setAuditLogManager(auditLogManager);
    HubRepoClient hubRepoClient = new HubRepoClient(dbHandler, config.getString(Constants.HUB_REPO_URL), config.getString(Constants.HUB_REPO_X_API_KEY), client, dispatcherClient, hubStoreClient, hubSearchClient, countriesManager, auditLogManager);
    ITBValidatorClient itbValidatorClient =
      new ITBValidatorClient(client, config.getString(Constants.ENV_ITB_VALIDATOR_ENDPOINT, ""));

    BasketManager basketManager = new BasketManager(dbHandler, mongoClient);
    CommonApplicationValidationManager commonApplicationValidationManager = new CommonApplicationValidationManager(dbHandler, mongoClient);
    DataAccessApplicationValidationManager daaValidation = new DataAccessApplicationValidationManager();
    DataAccessApplicationBusinessValidationManager daaBusinessValidation = new DataAccessApplicationBusinessValidationManager(mongoClient);
    DataAccessApplicationManager daaManager = new DataAccessApplicationManager(dbHandler, mongoClient, basketManager, hubStoreClient, vertx, daaValidation, daaBusinessValidation, auditLogManager, notificationManager, hubTranslationClient, hubSearchClient, authClient);

    DataRequestApplicationValidationManager draValidation = new DataRequestApplicationValidationManager();
    DataRequestApplicationBusinessValidationManager draBusinessValidation = new DataRequestApplicationBusinessValidationManager(mongoClient);
    DataRequestApplicationManager draManager = new DataRequestApplicationManager(dbHandler, mongoClient, basketManager, hubStoreClient, vertx, draValidation, draBusinessValidation, auditLogManager, notificationManager, hubTranslationClient, hubSearchClient, authClient);

    UserValidationManager userValidation = new UserValidationManager();
    UserManager userManager = new UserManager(dbHandler, mongoClient, dispatcherClient, auditLogManager, authClient, notificationManager);
    FavouriteManager favouriteManager = new FavouriteManager(dbHandler, mongoClient, hubSearchClient);
    AdministrationTasksManager adminTasksManager = new AdministrationTasksManager(dbHandler, mongoClient);

    // Collaboration and section locking managers
    CollaboratorManager collaboratorManager = new CollaboratorManager(
      dbHandler, mongoClient, auditLogManager, notificationManager);
    sectionLockManager = new SectionLockManager(dbHandler, mongoClient, lockWsManager);

    // Inject section lock manager into application managers for write-time lock enforcement
    daaManager.setSectionLockManager(sectionLockManager);
    draManager.setSectionLockManager(sectionLockManager);

    // Inject WebSocket manager into application managers for section_saved broadcasting
    daaManager.setLockWebSocketManager(lockWsManager);
    draManager.setLockWebSocketManager(lockWsManager);

    // Create MongoDB indexes for collaboration and locking collections at startup
    createCollaborationIndexes();
    createOwnershipRequestIndexes();
    createSectionLockIndexes();

    ControlledVocabularyManager controlledVocabularyManager = new ControlledVocabularyManager(
      mongoClient, client, hubTranslationClient);
    SchemaTranslationService schemaTranslationService = new SchemaTranslationService();
    PayloadManager payloadManager = new PayloadManager(dbHandler, mongoClient,
      controlledVocabularyManager, hubTranslationClient, schemaTranslationService, config);

    HubTranslationClientActivities hubTranslationClientActivities = new HubTranslationClientActivities(
      client,
      config.getString(Constants.ENV_TRANSLATION_SERVICE_X_API_KEY, ""),
      config.getString(Constants.ENV_TRANSLATION_SERVICE_URL, "http://localhost:8084"),
      config.getString(Constants.ENV_HEALTH_DATA_ACCESS_API_KEY, ""),
      config.getString(Constants.ENV_TRANSLATION_SERVICE_CALLBACK_URL, "http://localhost:8089")
    );

    boolean enableTranslation = Boolean.parseBoolean(config.getString(Constants.ENABLE_TRANSLATION, "false"));
    DecisionTranslationService decisionTranslationService =
      new DecisionTranslationService(mongoClient, hubTranslationClientActivities, dispatcherClient, hubStoreClient, enableTranslation);

    daaManager.setDecisionTranslationService(decisionTranslationService);
    draManager.setDecisionTranslationService(decisionTranslationService);

    TranslationCallbackManager translationCallbackManager = new TranslationCallbackManager(
      draManager, daaManager, controlledVocabularyManager, payloadManager, hubStoreClient,
      decisionTranslationService);

    AuditLogValidationManager auditLogValidation = new AuditLogValidationManager( config,hubSearchClient);
    BasketValidationManager basketValidation = new BasketValidationManager(hubSearchClient);

    DocumentExportDataRequestManager documentExportRequestManager = new DocumentExportDataRequestManager(translationManager, dbHandler, client, config);
    DocumentExportDataAccessManager documentExportAccessManager = new DocumentExportDataAccessManager(translationManager, dbHandler, client, config);

    DispatcherValidationManager dispatcherValidationManager = new DispatcherValidationManager();
    DispatcherManager dispatcherManager = new DispatcherManager(dbHandler, mongoClient,
      basketManager, hubStoreClient, vertx, auditLogManager, notificationManager,
      hubTranslationClient, hubSearchClient, authClient,config);
    dispatcherManager.setDecisionTranslationService(decisionTranslationService);

    CatalogueManager catalogueManager = new CatalogueManager(dbHandler, hubRepoClient, hubStoreClient, dispatcherClient, vertx, auditLogManager);
    CatalogueValidationManager catalogueValidationManager = new CatalogueValidationManager(
      hubRepoClient, dispatcherClient, vertx, countriesManager);

    DdaDatasetManager  ddaDatasetManager = new DdaDatasetManager(dbHandler, mongoClient);

    PermissionValidator adminValidator = new PermissionValidator(List.of(DATA_ADMIN));
    PermissionValidator userValidator = new PermissionValidator(List.of(DATA_USER));

    BiennialReportManager biennialReportManager = new BiennialReportManager();

    RDFService rdfService = new RDFService();

    setupHandlers(builder, userValidator, adminValidator,
      adminTasksManager, catalogueValidationManager, catalogueManager,
      commonApplicationValidationManager, dispatcherManager, dispatcherValidationManager,
      draValidation, draManager, draBusinessValidation,
      daaValidation, daaManager, daaBusinessValidation,
      basketValidation, basketManager, userValidation, userManager, favouriteManager,
      translationManager, auditLogValidation, auditLogManager,
      authClient, documentExportRequestManager, documentExportAccessManager,
      translationCallbackManager, biennialReportManager, payloadManager, ddaDatasetManager,
      itbValidatorClient, rdfService, collaboratorManager, sectionLockManager, notificationManager);

    SchedulerManager schedulerManager = new SchedulerManager(mongoClient);
    SchedulerService schedulerService = new SchedulerService(dbHandler);

    scheduleJobForDataAccessPermitApplication(mongoClient, daaManager);
    scheduleTranslationRetryJob(hubTranslationClient, hubStoreClient, daaManager, draManager);
    scheduleControlledVocabularyRefreshJob(controlledVocabularyManager);
    schedulerManager.scheduleWithdrawApplicationJob(dispatcherManager, auditLogManager, schedulerService, config, vertx, dbHandler, notificationManager);
    schedulerManager.scheduleDispatcherJobs(dispatcherManager, dispatcherClient, auditLogManager, notificationManager, schedulerService, config, vertx, dbHandler);
    setUpSchedulersOutgoingApplicationActivities(dispatcherManager, dispatcherClient, auditLogManager, notificationManager);
    scheduleCronJobForNotifications(notificationManager);
    scheduleDecisionTranslationJob(decisionTranslationService);

  }

  private void addHealthCheck(Router router) {
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("health-check", future -> future.complete(Status.OK()));
    router.get("/health").handler(healthCheckHandler);
  }

  private void startHttpServer(Router router) {
    vertx.createHttpServer(new HttpServerOptions().setPort(8080))
      .requestHandler(router)
      .webSocketHandler(ws -> handleLockWebSocket(ws, lockWsManager, sectionLockManager))
      .listen()
      .onSuccess(server -> log.info("HTTP server successfully launched."))
      .onFailure(error -> log.error("HTTP server failed to launch: ", error));
  }

  /**
   * Handles incoming WebSocket upgrade requests for section lock events.
   *
   * <p>Expected WebSocket path:
   * <pre>  /api/ws/applications/{applicationType}/{applicationId}/locks?sessionId={uuid}</pre>
   *
   * <p>The {@code sessionId} query parameter is the same per-tab UUID used for REST
   * lock operations.  It is stored here and passed to
   * {@link SectionLockManager#releaseLocksForSession} when the connection closes, so
   * that any lock held by this browser tab is automatically released even if the
   * frontend's {@code sendBeacon} / {@code pagehide} HTTP request never arrives
   * (e.g. browser force-killed, network drop, OS shutdown).
   *
   * <p>When the frontend opens a WebSocket to this path, the server:
   * <ol>
   *   <li>Parses {@code applicationType}, {@code applicationId}, and {@code sessionId}.</li>
   *   <li>Registers the socket with {@link LockWebSocketManager}.</li>
   *   <li>On close/error: calls {@link SectionLockManager#releaseLocksForSession} and
   *       unregisters the socket.</li>
   * </ol>
   *
   * @param ws                 the incoming server-side WebSocket connection
   * @param wsManager          the WebSocket broadcast manager
   * @param lockManager        the section lock manager (for WS-disconnect release)
   */
  void handleLockWebSocket(
    ServerWebSocket ws,
    LockWebSocketManager wsManager,
    SectionLockManager lockManager
  ) {
    // Expected path: /api/ws/applications/{type}/{id}/locks
    String path = ws.path();
    log.debug("LockWS: incoming WebSocket upgrade request for path '{}'", path);

    // Parse the path: /api/ws/applications/{type}/{id}/locks
    String[] parts = path.split("/");
    // Expected: ["", "api", "ws", "applications", "{type}", "{id}", "locks"]  → length 7
    if (parts.length != 7
        || !"api".equals(parts[1])
        || !"ws".equals(parts[2])
        || !"applications".equals(parts[3])
        || !"locks".equals(parts[6])) {
      log.warn("LockWS: unexpected WebSocket path '{}' — rejecting", path);
      ws.reject(404);
      return;
    }

    String applicationType = parts[4];
    String applicationId   = parts[5];

    // Validate applicationType
    if (!Constants.DATA_ACCESS_APPLICATION_COLLECTION.equals(applicationType)
        && !Constants.DATA_REQUEST_APPLICATION_COLLECTION.equals(applicationType)) {
      log.warn("LockWS: unknown applicationType '{}' — rejecting", applicationType);
      ws.reject(400);
      return;
    }

    // Parse the sessionId query parameter — used to release locks on disconnect.
    // The frontend appends ?sessionId=<uuid> to the WebSocket URL.
    // If absent (older clients), lock release on disconnect is skipped gracefully.
    String sessionId = ws.query() != null
        ? java.util.Arrays.stream(ws.query().split("&"))
              .filter(p -> p.startsWith("sessionId="))
              .map(p -> p.substring("sessionId=".length()))
              .findFirst().orElse(null)
        : null;
    log.debug("LockWS: sessionId from query param: '{}'", sessionId);

    // Accept the WebSocket upgrade
    ws.accept();
    log.info("LockWS: accepted connection for '{}:{}' from {} sessionId='{}'",
      applicationType, applicationId, ws.remoteAddress(), sessionId);

    // Register with the manager so broadcasts reach this socket
    wsManager.register(applicationType, applicationId, ws);

    final String finalType      = applicationType;
    final String finalId        = applicationId;
    final String finalSessionId = sessionId;

    // Send a WebSocket ping frame every 20 s to prevent intermediary idle-timeout
    // from silently dropping the connection (e.g. AWS ALB default is 60 s).
    // The browser handles the pong automatically — no application code required.
    // The timer ID is stored in a single-element array so the cleanup lambda can
    // cancel it even though the ID is not known at lambda-capture time.
    final long[] pingTimerId = { -1L };
    pingTimerId[0] = vertx.setPeriodic(20_000L, id ->
      handleLockWebSocketPing(ws, finalType, finalId, id)
    );

    // Guard against double-cleanup: both exceptionHandler and closeHandler can fire
    // for the same connection on abnormal disconnect (confirmed in production logs).
    // AtomicBoolean ensures unregister + releaseLocksForSession runs exactly once,
    // preventing redundant MongoDB find + removeDocuments operations.
    AtomicBoolean cleanedUp = new AtomicBoolean(false);
    Runnable cleanup = () -> {
      if (cleanedUp.compareAndSet(false, true)) {
        vertx.cancelTimer(pingTimerId[0]);
        wsManager.unregister(finalType, finalId, ws);
        lockManager.releaseLocksForSession(finalType, finalId, finalSessionId);
      } else {
        log.debug("LockWS: cleanup already done for '{}:{}' — skipping duplicate call",
          finalType, finalId);
      }
    };

    ws.closeHandler(v -> {
      log.info("LockWS: connection closed for '{}:{}'", finalType, finalId);
      cleanup.run();
    });

    ws.exceptionHandler(err -> {
      log.warn("LockWS: connection error for '{}:{}': {}", finalType, finalId, err.getMessage());
      cleanup.run();
    });

    // Currently we only support server-to-client events; ignore client messages
    ws.textMessageHandler(msg ->
      log.debug("LockWS: received message from client for '{}:{}': {}", finalType, finalId, msg));
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
    FavouriteManager favouriteManager,
    TranslationManager translationManager, AuditLogValidationManager auditLogValidationManager,
    AuditLogManager auditLogManager, AuthClient authClient, DocumentExportDataRequestManager documentExportDataRequestManager,
    DocumentExportDataAccessManager documentExportDataAccessManager,
    TranslationCallbackManager translationCallbackManager,
    BiennialReportManager biennialReportManager, PayloadManager payloadManager, DdaDatasetManager ddaDatasetManager,
    ITBValidatorClient itbValidatorClient, RDFService rdfService,
    CollaboratorManager collaboratorManager, SectionLockManager sectionLockManager, NotificationManager notificationManager) {

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

    setupTranslationCallbackHandler(builder, translationCallbackManager);

    setupUserManagerHandler(builder, userPermissionValidator, adminPermissionValidator,
      userValidationManager, userManager);

    setupFavouriteManagerHandler(builder, userPermissionValidator, favouriteManager);

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

    setUpBiennialReportManagerHandler(builder,
      biennialReportManager);

    setupPayloadManagerHandler(builder, payloadManager);

    setupDdaDatasetManagerHandler(builder, ddaDatasetManager);

    setupITBValidatorHandler(builder, itbValidatorClient);

    setupRdfConversionHandler(builder, rdfService);

    setupLogLevelHandler(builder);

    // New feature: collaboration and section locking
    setupCollaboratorManagerHandler(builder, collaboratorManager);
    setupSectionLockManagerHandler(builder, userPermissionValidator, sectionLockManager);

    setupNotificationHandler(builder, notificationManager);
  }

  private void setupCountriesHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator){
    builder.operation("getCountries")
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
    builder.operation("searchAllApplicationsByAdmin")
      .handler(adminPermissionValidator)
      .handler(administrationTasksManager::searchAllApplications);
  }

  private void setUpBiennialReportManagerHandler(RouterBuilder builder, BiennialReportManager biennialReportManager){
    builder.operation("downloadBiennialReportDocument")
      .handler(biennialReportManager::downloadBiennialReportDocument);
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

    builder.operation("putDecisionFromDispatcher")
      .handler(dispatcherValidationManager::validateDispatcherDecisionJsonSchema)
      .handler(dispatcherManager::updateDatasetDecisionFromDispatcher);

    builder.operation("putStatusFromDispatcher")
      .handler(dispatcherValidationManager::validateDispatcherStatusJsonSchema)
      .handler(dispatcherManager::updateStatusFromDispatcher);

    builder.operation("putPermitFromDispatcher")
      .handler(dispatcherValidationManager::validateDispatcherPermitJsonSchema)
      .handler(dispatcherManager::updatePermitFromDispatcher);
    builder.operation("getDispatcherApplicationByIdAndVersion")
      .handler(dispatcherManager::getDispatcherApplicationByIdAndVersion);
    builder.operation("getDispatcherApplicationById")
      .handler(dispatcherManager::getDispatcherApplicationById);
  }

  private void setupTranslationManagerHandler(RouterBuilder builder, PermissionValidator userPermissionValidator,
    TranslationManager translationManager) {
    builder.operation("getTranslations")
      .handler(translationManager::getTranslationForSection);
    builder.operation("getApplicationTranslation")
      .handler(userPermissionValidator)
      .handler(translationManager::getApplicationTranslation);
  }

  private void setupTranslationCallbackHandler(RouterBuilder builder, TranslationCallbackManager translationCallbackManager) {
    builder.operation("postTranslationCallback")
      .handler(translationCallbackManager::handleTranslationCallback);
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
    builder.operation("getUserById")
      .handler(adminPermissionValidator)
      .handler(userManager::getUserById);
    builder.operation("putUserEmailPreferences")
      .handler(userPermissionValidator)
      .handler(userValidationManager::validateEmailPreferencesJsonSchema)
      .handler(userManager::updateUserEmailPreferences);

    // Bootstrap endpoint: called once per login from the frontend
    builder.operation("bootstrapUser")
      .handler(userPermissionValidator)
      .handler(userManager::bootstrapUser);
  }

  /**
   * Registers all collaboration-related API operation handlers.
   *
   * <p>Endpoints are already protected by Bearer authentication at the OpenAPI level.
   * Owner/collaborator checks and administrator bypasses are enforced inside the
   * collaboration handlers themselves.
   *
   * @param builder              the OpenAPI router builder
   * @param collaboratorManager  the collaboration service instance
   */
  private void setupCollaboratorManagerHandler(RouterBuilder builder,
      eu.europa.data.health.access.services.CollaboratorManager collaboratorManager) {

    builder.operation("getCollaborators")
      .handler(collaboratorManager::getCollaborators);

    builder.operation("addCollaborator")
      .handler(collaboratorManager::addCollaborator);

    builder.operation("removeCollaborator")
      .handler(collaboratorManager::removeCollaborator);

    builder.operation("acceptCollaboratorInvite")
      .handler(collaboratorManager::acceptCollaboratorInvite);

    builder.operation("rejectCollaboratorInvite")
      .handler(collaboratorManager::rejectCollaboratorInvite);

    builder.operation("transferOwnership")
      .handler(collaboratorManager::transferOwnership);

    builder.operation("getMyCollaborativeApplications")
      .handler(collaboratorManager::getMyCollaborativeApplications);

    builder.operation("requestOwnership")
      .handler(collaboratorManager::requestOwnership);

    builder.operation("getOwnershipRequests")
      .handler(collaboratorManager::getOwnershipRequests);

    builder.operation("acceptOwnershipRequest")
      .handler(collaboratorManager::acceptOwnershipRequest);

    builder.operation("rejectOwnershipRequest")
      .handler(collaboratorManager::rejectOwnershipRequest);
  }

  /**
   * Registers all section edit locking API operation handlers.
   *
   * <p>All lock endpoints require the user to be authenticated (userPermissionValidator).
   * Lock ownership enforcement is done inside {@link SectionLockManager}.
   *
   * @param builder              the OpenAPI router builder
   * @param userPermissionValidator validator requiring {@code data-user} role
   * @param sectionLockManager   the section lock service instance
   */
  private void setupSectionLockManagerHandler(RouterBuilder builder,
      PermissionValidator userPermissionValidator,
      eu.europa.data.health.access.services.SectionLockManager sectionLockManager) {

    builder.operation("acquireSectionLock")
      .handler(userPermissionValidator)
      .handler(sectionLockManager::acquireLock);

    builder.operation("heartbeatSectionLock")
      .handler(userPermissionValidator)
      .handler(sectionLockManager::heartbeatLock);

    builder.operation("releaseSectionLock")
      .handler(userPermissionValidator)
      .handler(sectionLockManager::releaseLock);

    builder.operation("getApplicationSectionLocks")
      .handler(userPermissionValidator)
      .handler(sectionLockManager::getApplicationLocks);

    builder.operation("switchSectionLock")
      .handler(userPermissionValidator)
      .handler(sectionLockManager::switchLock);
  }

  private void setupFavouriteManagerHandler(RouterBuilder builder,
    PermissionValidator userPermissionValidator, FavouriteManager favouriteManager) {

    builder.operation("getFavourites")
      .handler(userPermissionValidator)
      .handler(favouriteManager::getFavourites);

    builder.operation("getStaleFavourites")
      .handler(userPermissionValidator)
      .handler(favouriteManager::getStaleFavourites);

    builder.operation("createFavourite")
      .handler(userPermissionValidator)
      .handler(favouriteManager::createFavourite);

    builder.operation("updateFavourite")
      .handler(userPermissionValidator)
      .handler(favouriteManager::updateFavourite);

    builder.operation("deleteFavourite")
      .handler(userPermissionValidator)
      .handler(favouriteManager::deleteFavourite);
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
      .handler(dataRequestApplicationManager::postData)
      // ⬇️ this catches ANY failure for this operation, even before your handlers run
      .failureHandler(this::handlePostRequestDataFailure);
    builder.operation("getRequestData")
      .handler(dataRequestApplicationManager::getData);
    builder.operation("deleteRequestData")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::deleteData);
    builder.operation("putStatusDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataRequestApplicationManager::changeStatus);
    builder.operation("putCatalogStatusDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataRequestApplicationManager::changeCatalogStatus);
    builder.operation("putDatasetStatusDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataRequestApplicationManager::changeDatasetStatus);
    builder.operation("putTitleDataRequestApplication")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationValidationManager::validateChangeTitleJsonSchema)
      .handler(dataRequestApplicationValidationManager::validateUpdateTitle)
      .handler(dataRequestApplicationManager::changeTitle);
    builder.operation("getDataRequestApplicationsHbad")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getDataRequestApplicationsHbad);
    builder.operation("getDataRequestApplicationsCountries")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getDataRequestApplicationsCountries);
    builder.operation("getAllDataRequestDatasets")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getAllDataRequestDatasets);
    builder.operation("getAllOnGoingDataRequestApplications")
      .handler(dataRequestApplicationManager::getAllOnGoingDataRequestApplications);
    builder.operation("getAllOnGoingDataRequestApplications")
      .handler(dataRequestApplicationManager::getAllOnGoingDataRequestApplications);
    builder.operation("getAllDataPermitIssuedDataRequestApplications")
      .handler(dataRequestApplicationManager::getAllDataPermitIssuedApplications);
    builder.operation("getAllDecisionIssuedDataRequestApplications")
      .handler(dataRequestApplicationManager::getAllDecisionIssuedDataRequestApplications);
    builder.operation("getEveryDataRequestApplicationById")
      .handler(dataRequestApplicationManager::getEveryDataRequestApplicationById);
    builder.operation("getDataRequestApplicationTitles")
      .handler(userPermissionValidator)
      .handler(dataRequestApplicationManager::getDataRequestApplicationTitles);

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
      .handler(dataAccessApplicationManager::getData);
    builder.operation("deleteData")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::deleteData);
    builder.operation("putStatusApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataAccessApplicationManager::changeStatus);
    builder.operation("putCatalogStatusDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataAccessApplicationManager::changeCatalogStatus);
    builder.operation("putDatasetStatusDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataAccessApplicationManager::changeDatasetStatus);
    builder.operation("putDatasetPermitStatusDataAccessApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeStatusJsonSchema)
      .handler(dataAccessApplicationManager::changePermitStatus);
    builder.operation("putTitleApplication")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationValidationManager::validateChangeTitleJsonSchema)
      .handler(dataAccessApplicationValidationManager::validateUpdateTitle)
      .handler(dataAccessApplicationManager::changeTitle);
    builder.operation("getDataAccessApplicationsHbad")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getDataAccessApplicationsHbad);
    builder.operation("getDataAccessApplicationsCountries")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getDataAccessApplicationsCountries);
    builder.operation("getAllDataAccessDatasets")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getAllDataAccessDatasets);
    builder.operation("getAllOnGoingDataAccessApplications")
      .handler(dataAccessApplicationManager::getAllOnGoingDataAccessApplications);
    builder.operation("getAllDataPermitIssuedDataAccessApplications")
      .handler(dataAccessApplicationManager::getAllDataPermitIssuedApplications);
    builder.operation("getAllDecisionIssuedDataAccessApplications")
      .handler(dataAccessApplicationManager::getAllDecisionIssuedDataAccessApplications);
    builder.operation("getEveryDataAccessApplicationById")
      .handler(dataAccessApplicationManager::getEveryDataAccessApplicationById);
    builder.operation("getDataAccessApplicationTitles")
      .handler(userPermissionValidator)
      .handler(dataAccessApplicationManager::getDataAccessApplicationTitles);
  }

  private void setupPayloadManagerHandler(RouterBuilder builder, PayloadManager payloadManager) {
    builder.operation("putPayload").handler(payloadManager::createOrUpdatePayload);
    builder.operation("getPayload").handler(payloadManager::retrievePayload);
  }

  /**
   * Setup DDA Dataset Manager Handler
   * @param builder : RouterBuilder
   * @param ddaDatasetManager : DdaDatasetManager
   */
  private void setupDdaDatasetManagerHandler(RouterBuilder builder, DdaDatasetManager ddaDatasetManager) {
    builder.operation("postDda").handler(ddaDatasetManager::createOrUpdateDatasetDescription);
    builder.operation("getDda").handler(ddaDatasetManager::getDatasetDescription);
  }

  private void setupITBValidatorHandler(RouterBuilder builder,
    ITBValidatorClient itbValidatorClient) {
    builder.operation("validateDataset").handler(itbValidatorClient::validateDataset);
  }

  private void setupRdfConversionHandler(RouterBuilder builder, RDFService rdfService) {
    builder.operation("convertJsonLdToRdfXml")
      .handler(ctx -> rdfService.convertRDF(ctx, Lang.JSONLD, Lang.RDFXML));
  }

  private void setupLogLevelHandler(RouterBuilder builder) {
    // for example GET http://localhost:8080/log-level?logger=root&level=DEBUG
    builder.operation("updateLogLevel").handler(new DynamicLogLevelHandler());
  }

  private void setupNotificationHandler(RouterBuilder builder, NotificationManager notificationManager) {
    builder.operation("getAllNotifications")
      .handler(notificationManager::getAllNotifications);
    builder.operation("readNotifications")
      .handler(notificationManager::readNotifications);
    builder.operation("getUnreadNotificationCount")
      .handler(notificationManager::getUnreadNotificationCount);
    builder.operation("deleteNotifications")
      .handler(notificationManager::deleteNotifications);
  }

  void setUpSchedulersOutgoingApplicationActivities(
    DispatcherManager dispatcherManager,
    DispatcherClient dispatcherClient,
    AuditLogManager auditLogManager,
    NotificationManager notificationManager
  ) {
    if (Constants.SCHEDULER_ENABLED.equals(
      config.getString(Constants.OUTGOING_APPLICATION_ACTIVITIES_ENABLE))) {
      Integer dispatcherInterval = config.getInteger(
        Constants.OUTGOING_APPLICATION_ACTIVITIES_INTERVAL);
      DispatcherScheduledJob dispatcherScheduledJob = new DispatcherScheduledJob(dbHandler,
        mongoClient, dispatcherManager, auditLogManager, notificationManager, config);
      vertx.setPeriodic(dispatcherInterval, id ->
        sendOutgoingApplicationActivities(
          dispatcherScheduledJob,
          dispatcherClient,
          dispatcherInterval
        )
      );
    } else {
      log.info("Create Message Scheduler is disabled");
    }
  }

  void scheduleJobForDataAccessPermitApplication(
    MongoClient mongoClient,
    DataAccessApplicationManager daaManager
  ) {
    final List<String> permitStatusesThatExpire;

    String permitStatusesThatExpireFromConfig = config.getString(Constants.PERMIT_STATUSES_THAT_EXPIRE_LABEL);

    if (permitStatusesThatExpireFromConfig != null && !permitStatusesThatExpireFromConfig.isBlank()) {
      permitStatusesThatExpire = Arrays.stream(permitStatusesThatExpireFromConfig.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    } else {
      permitStatusesThatExpire = Arrays.stream(Constants.PERMIT_STATUSES_THAT_EXPIRE.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    }

    long dataAccessPermitInterval = config.getLong(Constants.DATA_ACCESS_PERMIT_INTERVAL) != null
      ? config.getLong(Constants.DATA_ACCESS_PERMIT_INTERVAL)
      : Constants.DATA_ACCESS_PERMIT_DEFAULT_INTERVAL;

    DataAccessPermitJob dataAccessPermitJob = new DataAccessPermitJob(mongoClient, daaManager);

    long delayToMidnight = millisUntilNextMidnight();
    vertx.setTimer(delayToMidnight, id ->
      runInitialPermitExpirationAndScheduleRepeats(
        dataAccessPermitJob,
        permitStatusesThatExpire,
        dataAccessPermitInterval
      )
    );
  }

  long millisUntilNextMidnight() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
    return Duration.between(now, nextMidnight).toMillis();
  }

  private void scheduleTranslationRetryJob(
    HubTranslationClient hubTranslationClient,
    HubStoreClient hubStoreClient,
    DataAccessApplicationManager daaManager,
    DataRequestApplicationManager draManager) {
    int retryInterval = config.getInteger(Constants.TRANSLATION_RETRY_INTERVAL) != null
      ? config.getInteger(Constants.TRANSLATION_RETRY_INTERVAL)
      : Constants.TRANSLATION_RETRY_DEFAULT_INTERVAL;

    int maxAttempts = config.getInteger(Constants.TRANSLATION_RETRY_MAX_ATTEMPTS) != null
      ? config.getInteger(Constants.TRANSLATION_RETRY_MAX_ATTEMPTS)
      : Constants.TRANSLATION_RETRY_DEFAULT_MAX_ATTEMPTS;

    TranslationRetryScheduledJob retryJob = new TranslationRetryScheduledJob(mongoClient, hubTranslationClient, hubStoreClient, daaManager, draManager);

    vertx.setPeriodic(retryInterval, id ->
      retryPendingTranslations(retryJob, maxAttempts, retryInterval)
    );
  }

  /**
   * Periodically re-fetches every controlled vocabulary document in Mongo by its source URL.
   * Interval (ms) can be configured via CONTROLLED_VOCABULARY_REFRESH_INTERVAL; defaults to
   * midnight Brussels time if not set. Minimum interval is 1 minute.
   */
  private void scheduleControlledVocabularyRefreshJob(ControlledVocabularyManager cvManager) {
    Object raw = config.getValue(Constants.CONTROLLED_VOCABULARY_REFRESH_INTERVAL);
    final long configuredDelay = raw instanceof Number n ? n.longValue() : millisToMidnight();
    final long refreshDelayMs = Math.max(configuredDelay, 60_000L); // Force minimum 1 minute

    vertx.setTimer(refreshDelayMs, id -> executeControlledVocabularyRefresh(cvManager));
  }

  private void scheduleCronJobForNotifications(NotificationManager notificationManager) {
    Integer withdrawnTimePeriod = config.getInteger(WITHDRAWN_TIME_PERIOD) != null
      ? config.getInteger(WITHDRAWN_TIME_PERIOD)
      : Constants.WITHDRAWN_TIME_DEFAULT_PERIOD;

    NotificationScheduledJob notificationScheduledJob = new NotificationScheduledJob(mongoClient, notificationManager, withdrawnTimePeriod);

    // Permit Scheduler

    //if need to override do this: DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS=1,2,15
    String daysToDisplayPermitNotificationsFromConfig = config.getString(DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS_LABEL);

    Set<Integer> daysToDisplayNotificationsForPermitsThatWillExpire =
      daysToDisplayPermitNotificationsFromConfig != null && !daysToDisplayPermitNotificationsFromConfig.isEmpty()
        ? parseDaysToSet(daysToDisplayPermitNotificationsFromConfig)
        : DEFAULT_DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS;
    log.info("Days to display notifications for permits that will expire are: {}", daysToDisplayNotificationsForPermitsThatWillExpire);

    long permitNotificationsInterval = config.getLong(PERMIT_NOTIFICATIONS_INTERVAL) != null
      ? config.getLong(PERMIT_NOTIFICATIONS_INTERVAL)
      : Constants.PERMIT_NOTIFICATIONS_DEFAULT_INTERVAL;

    // if need to override do this: PERMIT_STATUSES_THAT_EXPIRE=GRANTED,RENEWED,RENEWAL_REJECTED,AMENDED,AMENDMENT_REJECTED
    final List<String> permitStatusesThatExpire = parseArrayOfStringsFromConstantsOrConfig(Constants.PERMIT_STATUSES_THAT_EXPIRE_LABEL, Constants.PERMIT_STATUSES_THAT_EXPIRE);
    log.info("Permit statuses that can expire are: {}", permitStatusesThatExpire);

    long permitDelayUntilNextTime = millisUntilNextTime(LocalTime.of(3, 0));
    vertx.setTimer(permitDelayUntilNextTime, id ->
      scheduleRecurringNotifications(
        notificationScheduledJob,
        daysToDisplayNotificationsForPermitsThatWillExpire,
        permitStatusesThatExpire,
        SCHEDULED_PERMIT_EXPIRATION_APPROACHING,
        PERMIT_NOTIFICATION_TYPE,
        permitNotificationsInterval
      )
    );

    // Dataset Scheduler

    //if need to override do this: DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS=1,2,15
    String daysToDisplayDatasetNotificationsFromConfig = config.getString(DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS_LABEL);

    Set<Integer> daysToDisplayNotificationsForDatasetsThatWillWithdraw =
      daysToDisplayDatasetNotificationsFromConfig != null && !daysToDisplayDatasetNotificationsFromConfig.isEmpty()
        ? parseDaysToSet(daysToDisplayDatasetNotificationsFromConfig)
        : DEFAULT_DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS;
    log.info("Days to display notifications for datasets that will withdraw are: {}", daysToDisplayNotificationsForDatasetsThatWillWithdraw);

    long datasetNotificationsInterval = config.getLong(DATASET_NOTIFICATIONS_INTERVAL) != null
      ? config.getLong(DATASET_NOTIFICATIONS_INTERVAL)
      : Constants.DATASET_NOTIFICATIONS_DEFAULT_INTERVAL;

    // if need to override do this: DATASET_STATUSES_THAT_WITHDRAW=POSITIVE_DECISION_ISSUED,NEGATIVE_DECISION_ISSUED
    final List<String> datasetStatusesThatWithdraw = parseArrayOfStringsFromConstantsOrConfig(Constants.DATASET_STATUSES_THAT_WITHDRAW_LABEL, Constants.DATASET_STATUSES_THAT_WITHDRAW);
    log.info("Dataset statuses that can withdraw are: {}", datasetStatusesThatWithdraw);

    long datasetDelayUntilNextTime = millisUntilNextTime(LocalTime.of(4, 0));
    vertx.setTimer(datasetDelayUntilNextTime, id ->
      scheduleRecurringNotifications(
        notificationScheduledJob,
        daysToDisplayNotificationsForDatasetsThatWillWithdraw,
        datasetStatusesThatWithdraw,
        SCHEDULED_DATASET_WITHDRAWN_APPROACHING,
        DATASET_NOTIFICATION_TYPE,
        datasetNotificationsInterval
      )
    );

    // Catalog Scheduler

    //if need to override do this: DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS=1,2,15
    String daysToDisplayCatalogNotificationsFromConfig = config.getString(DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS_LABEL);

    Set<Integer> daysToDisplayNotificationsForCatalogsThatWillWithdraw =
      daysToDisplayCatalogNotificationsFromConfig != null && !daysToDisplayCatalogNotificationsFromConfig.isEmpty()
        ? parseDaysToSet(daysToDisplayCatalogNotificationsFromConfig)
        : DEFAULT_DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS;
    log.info("Days to display notifications for catalogs that will withdraw are: {}", daysToDisplayNotificationsForCatalogsThatWillWithdraw);

    long catalogNotificationsInterval = config.getLong(CATALOG_NOTIFICATIONS_INTERVAL) != null
      ? config.getLong(CATALOG_NOTIFICATIONS_INTERVAL)
      : Constants.CATALOG_NOTIFICATIONS_DEFAULT_INTERVAL;

    // if need to override do this: CATALOG_STATUSES_THAT_WITHDRAW="SUBMITTED,PRE_SCREEN,PROCESSING,PROCESSING_EXTENDED,ADDITIONAL_INFORMATION"
    final List<String> catalogStatusesThatWithdraw = parseArrayOfStringsFromConstantsOrConfig(Constants.CATALOG_STATUSES_THAT_WITHDRAW_LABEL, Constants.CATALOG_STATUSES_THAT_WITHDRAW);
    log.info("Catalog statuses that can withdraw are: {}", catalogStatusesThatWithdraw);

    long catalogDelayUntilNextTime = millisUntilNextTime(LocalTime.of(5, 0));
    vertx.setTimer(catalogDelayUntilNextTime, id ->
      scheduleRecurringNotifications(
        notificationScheduledJob,
        daysToDisplayNotificationsForCatalogsThatWillWithdraw,
        catalogStatusesThatWithdraw,
        SCHEDULED_CATALOG_WITHDRAWN_APPROACHING,
        CATALOG_NOTIFICATION_TYPE,
        catalogNotificationsInterval
      )
    );
  }

  Set<Integer> parseDaysToSet(String daysConfig) {
    if (daysConfig == null || daysConfig.isBlank()) {
      return Set.of();
    }

    // Remove brackets if present
    daysConfig = daysConfig.replace("[", "").replace("]", "");

    try {
      return Arrays.stream(daysConfig.split(","))
        .map(String::trim)
        .map(Integer::parseInt)
        .collect(Collectors.toSet());

    } catch (NumberFormatException e) {
      log.error("Invalid value: {}", daysConfig, e);
      return Set.of();
    }
  }

  List<String> parseArrayOfStringsFromConstantsOrConfig(
    String configurationLabel,
    String constantValuesAsString
  ) {
    final List<String> arrayOfStrings;

    String valuesFromConfig = config.getString(configurationLabel);
    if (valuesFromConfig != null && !valuesFromConfig.isBlank()) {
      arrayOfStrings = Arrays.stream(valuesFromConfig.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    } else {
      arrayOfStrings = Arrays.stream(constantValuesAsString.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    }
    return arrayOfStrings;
  }

  private long millisUntilNextTime(LocalTime targetTime) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextRun = now.toLocalDate().atTime(targetTime);

    // If time already passed today → schedule tomorrow
    if (nextRun.isBefore(now)) {
      nextRun = nextRun.plusDays(1);
    }

    return Duration.between(now, nextRun).toMillis();
  }


  /**
   * Calculates milliseconds until next midnight in Brussels timezone.
   */
  private long millisToMidnight() {
    ZoneId zone = ZoneId.of("Europe/Brussels");
    ZonedDateTime now = ZonedDateTime.now(zone);
    ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone);
    return Duration.between(now, nextMidnight).toMillis();
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
    context.response().putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON).end(jsonResponse);
  }

  Future<Void> loadConfig() {
    Promise<Void> promise = Promise.promise();
    ConfigRetrieverOptions options = new ConfigRetrieverOptions();

    // configuration options from properties file
    String configPath = getConfigPath();
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

    createConfigRetriever(options).getConfig(handler -> {
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
        createCollaborationIndexes()
          .compose(v -> createOwnershipRequestIndexes())
          .compose(v -> createSectionLockIndexes())
          .compose(v -> createTranslationStateIndexes())
          .compose(v -> createApplicationIndexes(DATA_ACCESS_APPLICATION_COLLECTION))
          .compose(v -> createApplicationIndexes(DATA_REQUEST_APPLICATION_COLLECTION))
          .onComplete(ar -> promise.complete(dbHandler));
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
    try (InputStream rdfFile = openCountriesRdfStream()) {
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
  void getCountries(RoutingContext context) {
    String lang = context.pathParam("lg");
    String filter = context.pathParam("filter");

    if (Objects.nonNull(countriesModel)) {
      JsonArray countries = countriesManager.getCountriesJsonArray(countriesModel, lang, filter);
      context.response().putHeader("content-type", CONTENT_TYPE_APPLICATION_JSON);
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
  void getCountriesTranslation(RoutingContext context) {
    String lang = context.pathParam("lg");

    if (Objects.nonNull(countriesModel)) {
      JsonArray countries = countriesManager.getCountriesJsonArray(countriesModel, lang, "euCountry");
      context.response().putHeader("content-type", CONTENT_TYPE_APPLICATION_JSON);
      context.response().setStatusCode(200).end(countries.encode());
    } else {
      context.response().setStatusCode(400).end("Countries model is null.");
    }
  }

  /**
   * Creates MongoDB indexes for the {@code APPLICATION_COLLABORATOR} collection.
   *
   * <p>Two partial-unique indexes are created:
   * <ol>
   *   <li>Unique on (applicationType, applicationId, collaboratorUserId) when userId is present
   *       — prevents duplicate confirmed collaborations.</li>
   *   <li>Unique on (applicationType, applicationId, invitedEmail) when invitationStatus is PENDING
   *       — prevents duplicate pending email invites.</li>
   * </ol>
   *
   * <p>Index creation is idempotent; if the index already exists, Mongo will not recreate it.
   *
   * @return a {@link Future} that always succeeds (failures are logged as warnings)
   */
  Future<Void> createCollaborationIndexes() {
    // Drop existing index (migrating from sparse to partial)
    return mongoClient
      .dropIndex(Constants.APPLICATION_COLLABORATOR_COLLECTION,
        "idx_collaborator_unique_user")
      .onSuccess(v ->
        log.info("Old index 'idx_collaborator_unique_user' dropped"))
      .onFailure(err ->
        log.warn("Old index 'idx_collaborator_unique_user' could not be dropped: {}",
          err.getMessage()))
      .recover(err -> Future.succeededFuture())

      //  Create new partial index for collaboratorUserId
      .compose(v -> {
        JsonObject collaboratorUniqueKey = new JsonObject()
          .put(Constants.APPLICATION_TYPE_FIELD, 1)
          .put(Constants.APPLICATION_ID, 1)
          .put(Constants.COLLABORATOR_USER_ID, 1);

        JsonObject collaboratorUniqueOptions = new JsonObject()
          .put("name", "idx_collaborator_unique_user")
          .put("unique", true)
          .put("partialFilterExpression", new JsonObject()
            .put(Constants.COLLABORATOR_USER_ID, new JsonObject().put("$exists", true)));

        return mongoClient.createIndexWithOptions(
            Constants.APPLICATION_COLLABORATOR_COLLECTION,
            collaboratorUniqueKey,
            new IndexOptions(collaboratorUniqueOptions)
          )
          .onSuccess(v2 ->
            log.debug("Collaboration index 'idx_collaborator_unique_user' ensured"))
          .onFailure(err ->
            log.warn("Failed to create collaboration user index: {}", err.getMessage()))
          .recover(err -> Future.succeededFuture());
      })

      // Unique partial index on (applicationType, applicationId, invitedEmail) for PENDING invites
      .compose(v -> {
        JsonObject emailUniqueKey = new JsonObject()
          .put(Constants.APPLICATION_TYPE_FIELD, 1)
          .put(Constants.APPLICATION_ID, 1)
          .put(Constants.INVITED_EMAIL, 1);

        JsonObject emailUniqueOptions = new JsonObject()
          .put("name", "idx_collaborator_unique_email_pending")
          .put("unique", true)
          .put("partialFilterExpression", new JsonObject()
            .put(Constants.INVITATION_STATUS, Constants.INVITATION_PENDING));

        return mongoClient.createIndexWithOptions(
            Constants.APPLICATION_COLLABORATOR_COLLECTION,
            emailUniqueKey,
            new IndexOptions(emailUniqueOptions)
          )
          .onSuccess(v3 ->
            log.debug("Collaboration index 'idx_collaborator_unique_email_pending' ensured"))
          .onFailure(err ->
            log.warn("Failed to create collaboration email index: {}", err.getMessage()))
          .recover(err -> Future.succeededFuture());
      });
  }

  /**
   * Creates MongoDB indexes for the {@code OWNERSHIP_REQUESTS} collection.
   *
   * <p>A unique compound index is created on
   * {@code (applicationType, application_id, userId)} to prevent duplicate pending
   * ownership requests by the same editor on the same application.
   *
   * @return a {@link Future} that always succeeds (failures are logged as warnings)
   */
  Future<Void> createOwnershipRequestIndexes() {
    JsonObject requestUniqueKey = new JsonObject()
      .put(Constants.APPLICATION_TYPE_FIELD, 1)
      .put(Constants.APPLICATION_ID, 1)
      .put(Constants.USER_ID, 1);

    JsonObject requestUniqueOptions = new JsonObject()
      .put("name", "idx_ownership_request_unique_user")
      .put("unique", true);

    return mongoClient.createIndexWithOptions(
        Constants.OWNERSHIP_REQUEST_COLLECTION,
        requestUniqueKey,
        new IndexOptions(requestUniqueOptions)
      )
      .onSuccess(v -> log.debug("Ownership request index 'idx_ownership_request_unique_user' ensured"))
      .onFailure(err ->
        log.warn("Failed to create ownership request index: {}", err.getMessage()))
      .recover(err -> Future.succeededFuture());
  }

  /**
   * Creates MongoDB indexes for the {@code APPLICATION_SECTION_LOCK} collection.
   *
   * <p>Two indexes are created:
   * <ol>
   *   <li>Unique compound index on (applicationType, applicationId, sectionId) — enforces
   *       at most one lock document per section per application.</li>
   *   <li>TTL index on {@code expiresAt} — MongoDB automatically removes expired lock
   *       documents. Note: TTL deletion is not instant; always check {@code expiresAt < now}
   *       in code for correctness.</li>
   * </ol>
   *
   * @return a {@link Future} that always succeeds (failures are logged as warnings)
   */
  Future<Void> createSectionLockIndexes() {
    // Unique compound index: at most ONE lock document per (section, session) —
    // i.e. each browser-tab session owns at most one row per section, but two
    // different sessions may each hold a dependent-lock row on the same section
    // (e.g. user A editing section2 — locks {2,5,8} as deps — does not prevent
    // user B editing section3 — which also locks {5,8} as deps).  Exclusive
    // primary-lock semantics are enforced in SectionLockManager's conflict check,
    // which rejects acquires when another session holds a primary lock on any
    // section in the requested lock group.
    JsonObject lockUniqueKey = new JsonObject()
        .put(Constants.APPLICATION_TYPE_FIELD, 1)
        .put(Constants.APPLICATION_ID, 1)
        .put(Constants.SECTION_ID, 1)
        .put(Constants.LOCK_OWNER_SESSION_ID, 1);

    JsonObject lockUniqueOptions = new JsonObject()
        .put("name", "idx_section_lock_unique")
        .put("unique", true);

    // TTL index on expiresAt for automatic cleanup of stale locks
    // expireAfterSeconds=0 means: delete the document when current time >= expiresAt
    JsonObject ttlKey = new JsonObject().put(Constants.EXPIRES_AT, 1);
    JsonObject ttlOptions = new JsonObject()
        .put("name", "idx_section_lock_ttl")
        .put("expireAfterSeconds", 0);

    return mongoClient
      .createIndexWithOptions(
        Constants.APPLICATION_SECTION_LOCK_COLLECTION,
        lockUniqueKey,
        new IndexOptions(lockUniqueOptions)
      )
      .onSuccess(v -> log.debug("Section lock index 'idx_section_lock_unique' ensured"))
      .onFailure(err ->
        log.warn("Failed to create section lock unique index: {}", err.getMessage())
      )
      .recover(err -> Future.succeededFuture())
      .compose(v ->
        mongoClient
          .createIndexWithOptions(
            Constants.APPLICATION_SECTION_LOCK_COLLECTION,
            ttlKey,
            new IndexOptions(ttlOptions)
          )
          .onSuccess(v2 -> log.debug("Section lock TTL index 'idx_section_lock_ttl' ensured"))
          .onFailure(err ->
            log.warn("Failed to create section lock TTL index: {}", err.getMessage())
          )
          .recover(err -> Future.succeededFuture())
      );
  }

  /**
   * Creates MongoDB indexes for the given application {@code collection} to optimise
   * list queries.
   *
   * <p>Three compound indexes are created:
   * <ol>
   *   <li><strong>idx_user_date_created</strong> —
   *       {@code {userId: 1, date_created: -1}}.
   *       Optimises queries that filter by user and sort by creation date.</li>
   *   <li><strong>idx_user_date_modified</strong> —
   *       {@code {userId: 1, date_modified: -1}}.
   *       Optimises queries that filter by user and sort by modification date.</li>
   *   <li><strong>idx_status_date_created</strong> —
   *       {@code {status.status: 1, date_created: -1}}.
   *       Optimises queries that filter by status and sort by creation date.</li>
   * </ol>
   *
   * @param collection the MongoDB collection name to create indexes for
   * @return a {@link Future} that always succeeds (failures are logged as warnings)
   */
  Future<Void> createApplicationIndexes(String collection) {
    return mongoClient
      .createIndexWithOptions(
        collection,
        new JsonObject().put(USER_ID, 1).put(DATE_CREATED, -1),
        new IndexOptions(new JsonObject().put("name", "idx_user_date_created"))
      )
      .onSuccess(v -> log.debug("{} index 'idx_user_date_created' ensured", collection))
      .onFailure(err -> log.warn("Failed to create {} date_created index:", collection, err))
      .recover(err -> Future.succeededFuture())
      .compose(v ->
        mongoClient
          .createIndexWithOptions(
            collection,
            new JsonObject().put(USER_ID, 1).put(DATE_MODIFIED, -1),
            new IndexOptions(new JsonObject().put("name", "idx_user_date_modified"))
          )
          .onSuccess(v2 -> log.debug("Index 'idx_user_date_modified' in {} ensured", collection))
          .onFailure(err ->
            log.warn("Failed to create date_modified index in {}:", collection, err)
          )
          .recover(err -> Future.succeededFuture())
      )
      .compose(v ->
        // Index for registry
        mongoClient
          .createIndexWithOptions(
            collection,
            new JsonObject().put(STATUS_STATUS, 1).put(DATE_CREATED, -1),
            new IndexOptions(new JsonObject().put("name", "idx_status_date_created"))
          )
          .onSuccess(v2 -> log.debug("Index 'idx_status_date_created' in {} ensured", collection))
          .onFailure(err ->
            log.warn("Failed to create status_date_created index in {}:", collection, err)
          )
          .recover(err -> Future.succeededFuture())
      );
  }

  private Future<Void> createTranslationStateIndexes() {
    return mongoClient
      .createIndexWithOptions(
        Constants.TRANSLATIONS_STATE_COLLECTION,
        new JsonObject().put(Constants.APPLICATION_TYPE, 1).put(Constants.TRANSLATION_STATUS, 1),
        new IndexOptions(new JsonObject().put("name", "idx_translation_state_app_status"))
      )
      .onSuccess(v -> log.debug("{} index 'idx_translation_state_app_status' ensured",
        Constants.TRANSLATIONS_STATE_COLLECTION))
      .onFailure(err -> log.warn("Failed to create translation state app/status index:", err))
      .recover(err -> Future.succeededFuture())
      .compose(v -> mongoClient.createIndexWithOptions(
          Constants.TRANSLATIONS_STATE_COLLECTION,
          new JsonObject().put("configuration.syncPending", 1).put(Constants.APPLICATION_TYPE, 1),
          new IndexOptions(new JsonObject().put("name", "idx_translation_state_sync_pending"))
        )
        .onSuccess(v2 -> log.debug("{} index 'idx_translation_state_sync_pending' ensured",
          Constants.TRANSLATIONS_STATE_COLLECTION))
        .onFailure(err -> log.warn("Failed to create translation state sync_pending index:", err))
        .recover(err -> Future.succeededFuture()))
      .compose(v -> mongoClient.createIndexWithOptions(
          Constants.TRANSLATIONS_STATE_COLLECTION,
          new JsonObject().put(Constants.OPERATION, 1),
          new IndexOptions(new JsonObject().put("name", "idx_translation_state_operation"))
        )
        .onSuccess(v3 -> log.debug("{} index 'idx_translation_state_operation' ensured",
          Constants.TRANSLATIONS_STATE_COLLECTION))
        .onFailure(err -> log.warn("Failed to create translation state operation index:", err))
        .recover(err -> Future.succeededFuture()))
      .mapEmpty();
  }

  private void scheduleDecisionTranslationJob(
    DecisionTranslationService decisionTranslationService
  ) {
    final AtomicBoolean isRunning = new AtomicBoolean(false);
    long interval = config.getLong(Constants.ENV_DECISION_TRANSLATION_INTERVAL, 30_000L);
    vertx.setPeriodic(interval, id ->
      triggerDecisionTranslationJob(decisionTranslationService, isRunning)
    );
  }

  void flushExpiredIntrospectionCacheEntries() {
    long now = System.currentTimeMillis();
    int before = introspectionCache.size();
    introspectionCache.entrySet().removeIf(e -> e.getValue() < now);
    log.debug(
      "Auth: introspection cache flush — removed {} expired entries, {} remaining",
      before - introspectionCache.size(),
      introspectionCache.size()
    );
  }

  void handleLockWebSocketPing(
    ServerWebSocket ws,
    String applicationType,
    String applicationId,
    long timerId
  ) {
    if (ws.isClosed()) {
      vertx.cancelTimer(timerId);
    } else {
      ws.writePing(Buffer.buffer());
      log.trace("LockWS: sent ping to '{}:{}'", applicationType, applicationId);
    }
  }

  void sendOutgoingApplicationActivities(
    DispatcherScheduledJob dispatcherScheduledJob,
    DispatcherClient dispatcherClient,
    long dispatcherInterval
  ) {
    log.debug("Create Message Scheduler successfully launched after {}", dispatcherInterval);
    dispatcherScheduledJob.sendOutgoingApplicationActivitiesToDispatcher(
      Constants.OUTGOING_APPLICATION_ACTIVITIES_COLLECTION,
      dispatcherClient
    );
  }

  void runInitialPermitExpirationAndScheduleRepeats(
    DataAccessPermitJob dataAccessPermitJob,
    List<String> permitStatusesThatExpire,
    long dataAccessPermitInterval
  ) {
    dataAccessPermitJob.expireOldPermits(permitStatusesThatExpire);
    vertx.setPeriodic(dataAccessPermitInterval, pid ->
      dataAccessPermitJob.expireOldPermits(permitStatusesThatExpire)
    );
  }

  void retryPendingTranslations(
    TranslationRetryScheduledJob retryJob,
    int maxAttempts,
    int retryInterval
  ) {
    retryJob.retryPendingTranslations(
      DATA_REQUEST_APPLICATION_COLLECTION,
      maxAttempts,
      retryInterval
    );
    retryJob.retryPendingTranslations(
      DATA_ACCESS_APPLICATION_COLLECTION,
      maxAttempts,
      retryInterval
    );
  }

  void executeControlledVocabularyRefresh(ControlledVocabularyManager cvManager) {
    cvManager
      .getAll()
      .onSuccess(docs -> handleControlledVocabularyDocuments(cvManager, docs))
      .onFailure(err -> log.error("Failed to find controlled vocabulary documents", err))
      .onComplete(v -> scheduleControlledVocabularyRefreshJob(cvManager));
  }

  void handleControlledVocabularyDocuments(
    ControlledVocabularyManager cvManager,
    List<JsonObject> docs
  ) {
    if (docs == null || docs.isEmpty()) {
      log.info("Controlled vocabulary refresh: no documents found.");
      return;
    }
    log.info("Controlled vocabulary refresh: re-fetching {} document(s).", docs.size());

    for (JsonObject doc : docs) {
      refreshControlledVocabularyDocument(cvManager, doc);
    }
  }

  void refreshControlledVocabularyDocument(ControlledVocabularyManager cvManager, JsonObject doc) {
    final String src = doc.getString("source");
    if (StringUtils.isNotBlank(src)) {
      cvManager
        .refreshBySource(src, doc)
        .onSuccess(v -> log.debug("Refreshed controlled vocabulary '{}'", src))
        .onFailure(err -> log.error("Failed to refresh controlled vocabulary '{}'", src, err));
    }
  }

  void scheduleRecurringNotifications(
    NotificationScheduledJob notificationScheduledJob,
    Set<Integer> daysToDisplayNotifications,
    List<String> statuses,
    NotificationCode notificationCode,
    String notificationType,
    long repeatInterval
  ) {
    triggerScheduledNotifications(
      notificationScheduledJob,
      daysToDisplayNotifications,
      statuses,
      notificationCode,
      notificationType
    );
    vertx.setPeriodic(repeatInterval, pid ->
      triggerScheduledNotifications(
        notificationScheduledJob,
        daysToDisplayNotifications,
        statuses,
        notificationCode,
        notificationType
      )
    );
  }

  void triggerScheduledNotifications(
    NotificationScheduledJob notificationScheduledJob,
    Set<Integer> daysToDisplayNotifications,
    List<String> statuses,
    NotificationCode notificationCode,
    String notificationType
  ) {
    notificationScheduledJob.createScheduledNotifications(
      daysToDisplayNotifications,
      statuses,
      notificationCode,
      notificationType
    );
  }

  void triggerDecisionTranslationJob(
    DecisionTranslationService decisionTranslationService,
    AtomicBoolean isRunning
  ) {
    if (!isRunning.compareAndSet(false, true)) {
      log.warn("Previous decision translation job still running, skipping this interval.");
      return;
    }
    log.debug("Triggering decision translation job");
    decisionTranslationService
      .triggerPendingTranslations()
      .onSuccess(v -> log.debug("Decision translation job completed successfully"))
      .onFailure(err -> log.error("Decision translation job failed", err))
      .onComplete(v -> isRunning.set(false));
  }

  void handlePostRequestDataFailure(RoutingContext ctx) {
    int status = ctx.statusCode() > 0 ? ctx.statusCode() : 500;
    Throwable failure = ctx.failure();

    if (failure != null) {
      log.error("[postRequestData] Failure (status={}): {}", status, failure.getMessage(), failure);
    } else {
      log.error("[postRequestData] Failure (status={}): no exception object", status);
    }

    if (!ctx.response().ended()) {
      ctx.response().setStatusCode(status).end("Upload failed");
    }
  }

  void setCountriesManager(CountriesManager countriesManager) {
    this.countriesManager = countriesManager;
  }

  void setCountriesModel(Model countriesModel) {
    this.countriesModel = countriesModel;
  }

  void setWebClient(WebClient client) {
    this.client = client;
  }

  Map<String, Long> introspectionCache() {
    return introspectionCache;
  }

  String getConfigPath() {
    return "conf/hd-access-env.properties";
  }

  ConfigRetriever createConfigRetriever(ConfigRetrieverOptions options) {
    return ConfigRetriever.create(vertx, options);
  }

  InputStream openCountriesRdfStream() throws IOException {
    return SchemaFiles.class.getClassLoader().getResourceAsStream(COUNTRIES_RDF.getValue());
  }
}
