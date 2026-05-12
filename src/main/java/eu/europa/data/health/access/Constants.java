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

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class Constants {

  private Constants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static final String ENV_MONGO_DB_URI = "MONGO_DB_URI";
  public static final String ENV_MONGO_DB = "MONGO_DB";
  public static final String HEALTH_DATA_BODY_LIMIT = "HEALTH_DATA_BODY_LIMIT";
  public static final long BODY_LIMIT = 20971520L;
  public static final String HUB_STORE_UPLOAD_DATA_URL = "HUB_STORE_UPLOAD_DATA_URL";
  public static final String PIVEAU_STORE_API_KEY = "PIVEAU_STORE_API_KEY";
  public static final String COUNTRIES_ALL = "all";
  public static final String COUNTRIES_NO_EU = "noEU";
  public static final String COUNTRIES_EU_EEA = "euEEA";
  public static final String COUNTRY_EU = "euCountry";
  public static final String DATA_ACCESS_DISPATCHER_MAX_RETRIES = "DATA_ACCESS_DISPATCHER_MAX_RETRIES";
  public static final String DATA_REQUEST_DISPATCHER_MAX_RETRIES = "DATA_ACCESS_DISPATCHER_MAX_RETRIES";
  public static final String DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES = "DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES";
  public static final String DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES = "DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_MAX_RETRIES";
  public static final String WITHDRAW_DISPATCHER_MAX_RETRIES = "WITHDRAW_DISPATCHER_MAX_RETRIES";
  public static final String DATA_ACCESS_DISPATCHER_INTERVAL = "DATA_ACCESS_DISPATCHER_INTERVAL";
  public static final String DATA_REQUEST_DISPATCHER_INTERVAL = "DATA_REQUEST_DISPATCHER_INTERVAL";
  public static final String DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL = "DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL";
  public static final String DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL = "DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_INTERVAL";
  public static final String WITHDRAW_DISPATCHER_INTERVAL = "WITHDRAW_DISPATCHER_INTERVAL";
  public static final String WITHDRAW_RUN_TIME = "WITHDRAW_RUN_TIME";
  public static final String WITHDRAW_THRESHOLD_DAYS = "WITHDRAW_THRESHOLD_DAYS";
  public static final String ADDITIONAL_INFO_WITHDRAW_THRESHOLD_DAYS = "ADDITIONAL_INFO_WITHDRAW_THRESHOLD_DAYS";

  public static final String EXPIRATION_DAYS = "EXPIRATION_DAYS";

  public static final String OUTGOING_APPLICATION_ACTIVITIES_INTERVAL = "OUTGOING_APPLICATION_ACTIVITIES_INTERVAL";
  public static final String OUTGOING_APPLICATION_ACTIVITIES_ENABLE = "OUTGOING_APPLICATION_ACTIVITIES_ENABLE";
  public static final String SCHEDULER_ENABLED = "1";
  public static final String COLLECTION_SCHEDULERS_STATUS = "SCHEDULERS_STATUS";
  public static final String FIELD_SCHEDULER_NAME = "scheduler_name";
  public static final String WITHDRAW_ACCESS_SCHEDULER = "WITHDRAW_ACCESS_SCHEDULER";
  public static final String WITHDRAW_REQUEST_SCHEDULER = "WITHDRAW_REQUEST_SCHEDULER";
  public static final String WITHDRAW_SYNCHRONIZATION = "WITHDRAW_SYNCHRONIZATION-";
  public static final String ADDITIONAL_INFORMATION_ACCESS_SCHEDULER = "ADDITIONAL_INFORMATION_ACCESS_SCHEDULER";
  public static final String ADDITIONAL_INFORMATION_REQUEST_SCHEDULER = "ADDITIONAL_INFORMATION_REQUEST_SCHEDULER";
  public static final String ADDITIONAL_INFORMATION_SYNCHRONIZATION = "ADDITIONAL_INFORMATION_SYNCHRONIZATION-";

  public static final int DATA_ACCESS_DISPATCHER_DEFAULT_MAX_RETRIES = 5;
  public static final int DATA_REQUEST_DISPATCHER_DEFAULT_MAX_RETRIES = 5;
  public static final int DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_MAX_RETRIES = 10;
  public static final int DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_MAX_RETRIES = 10;
  public static final int DATA_ACCESS_DISPATCHER_DEFAULT_INTERVAL = 300000;
  public static final long DATA_ACCESS_PERMIT_DEFAULT_INTERVAL = 24 * 60 * 60 * 1000L;
  public static final String DATA_ACCESS_PERMIT_INTERVAL = "DATA_ACCESS_PERMIT_INTERVAL";
  public static final String WITHDRAW_RUN_TIME_DEFAULT = "06:00";
  public static final int EXPIRATION_DAYS_DEFAULT = 90;
  public static final String PERMIT_STATUSES_THAT_EXPIRE = "GRANTED,RENEWED,RENEWAL_REJECTED,AMENDED,AMENDMENT_REJECTED";
  public static final String DATASET_STATUSES_THAT_WITHDRAW = "POSITIVE_DECISION_ISSUED,NEGATIVE_DECISION_ISSUED";
  public static final String CATALOG_STATUSES_THAT_WITHDRAW = "ADDITIONAL_INFORMATION";
  public static final String PERMIT_STATUSES_THAT_EXPIRE_LABEL = "PERMIT_STATUSES_THAT_EXPIRE";
  public static final String DATASET_STATUSES_THAT_WITHDRAW_LABEL = "DATASET_STATUSES_THAT_WITHDRAW";
  public static final String CATALOG_STATUSES_THAT_WITHDRAW_LABEL = "CATALOG_STATUSES_THAT_WITHDRAW";
  public static final String PERMIT_NOTIFICATION_TYPE = "PERMIT";
  public static final String DATASET_NOTIFICATION_TYPE = "DATASET";
  public static final String CATALOG_NOTIFICATION_TYPE = "CATALOG";
  public static final int DATA_REQUEST_DISPATCHER_DEFAULT_INTERVAL = 300000;
  public static final int DATA_ACCESS_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_INTERVAL = 120000;
  public static final int DATA_REQUEST_DISPATCHER_FOR_SENT_APPLICATIONS_DEFAULT_INTERVAL = 120000;


  public static final String FIELD_TRANSLATIONS = "translations";
  public static final String TRANSLATION_STATUS = "translationStatus";
  public static final String FILE_RETRIES = "fileRetries";
  public static final String TEXT_RETRIES = "textRetries";
  public static final int MAX_RETRIES = 5;
  public static final String SYNC_PENDING = "syncPending";

  public static final String ENABLE_TRANSLATION = "ENABLE_TRANSLATION";
  public static final String TRANSLATION_RETRY_INTERVAL = "TRANSLATION_RETRY_INTERVAL";
  public static final String TRANSLATION_RETRY_MAX_ATTEMPTS = "TRANSLATION_RETRY_MAX_ATTEMPTS";
  public static final int TRANSLATION_RETRY_DEFAULT_INTERVAL = 18_000_000; // 5 hours
  public static final int TRANSLATION_RETRY_DEFAULT_MAX_ATTEMPTS = 3;
  public static final String CONTROLLED_VOCABULARY_REFRESH_INTERVAL = "CONTROLLED_VOCABULARY_REFRESH_INTERVAL";
  public static final int PAGINATION_LIMIT = 50;
  public static final String DISPATCHER_URL = "DISPATCHER_URL";
  public static final String DISPATCHER_X_API_KEY = "DISPATCHER_X_API_KEY";
  public static final String HUB_REPO_URL = "HUB_REPO_URL";
  public static final String AUTH_URL = "AUTH_URL";
  public static final String HUB_REPO_X_API_KEY = "HUB_REPO_X_API_KEY";
  public static final String ENV_TRANSLATION_SERVICE_URL = "TRANSLATION_SERVICE_URL";
  public static final String ENV_TRANSLATION_SERVICE_FILE_URL = "TRANSLATION_SERVICE_FILE_URL";
  public static final String ENV_TRANSLATION_SERVICE_CALLBACK_URL = "TRANSLATION_SERVICE_CALLBACK_URL";
  public static final String ENV_TRANSLATION_SERVICE_X_API_KEY = "TRANSLATION_SERVICE_X_API_KEY";
  public static final String ENV_DECISION_TRANSLATION_INTERVAL = "DECISION_TRANSLATION_INTERVAL";
  public static final String ENV_DECISION_SYNC_INTERVAL = "DECISION_SYNC_INTERVAL";
  public static final String ENV_MAIL_SERVICE_X_API_KEY = "MAIL_SERVICE_X_API_KEY";
  public static final String ENV_MAIL_SERVICE_BASE_URL = "MAIL_SERVICE_BASE_URL";
  public static final String MAIL_APPLICATIONS_BASE_URL = "MAIL_APPLICATIONS_BASE_URL";
  public static final String ENV_KEYCLOAK_INTROSPECT_PATH = "KEYCLOAK_INTROSPECT_PATH";
  public static final String ENV_PIVEAU_ACCESS_AUTH_CONFIG = "PIVEAU_ACCESS_AUTH_CONFIG";
  public static final String ENV_HEALTH_DATA_ACCESS_API_KEY = "HEALTH_DATA_ACCESS_API_KEY";
  public static final String ENV_ITB_VALIDATOR_ENDPOINT = "ITB_VALIDATOR_ENDPOINT";
  public static final String BEARER_AUTH = "BearerAuth";
  public static final String USER_DELETED = "USER_DELETED";
  public static final String HUB_SEARCH_DATA_URL = "HUB_SEARCH_DATA_URL";
  public static final String EXPORT_DATA_ACCESS_API_KEY = "EXPORT_DATA_ACCESS_API_KEY";
  public static final String EXPORT_DATASET_EXTERNAL_URL = "EXPORT_DATASET_EXTERNAL_URL";
  public static final String APPLICATION_TYPE = "application_type";
  public static final String DATA_REQUEST_APPLICATION_COLLECTION = "DATA_REQUEST_APPLICATION";
  public static final String DATA_ACCESS_APPLICATION_COLLECTION = "DATA_ACCESS_APPLICATION";
  public static final String AUDIT_LOG_COLLECTION = "APPLICATION_AUDIT_LOG";
  public static final String NOTIFICATION_COLLECTION = "NOTIFICATION";
  public static final String NOTIFICATION_HISTORY_COLLECTION = "NOTIFICATION_HISTORY";

  // -------------------------------------------------------------------------
  // Collaboration feature collections (Phase 2)
  // -------------------------------------------------------------------------

  /**
   * MongoDB collection that stores collaborator relationships between users and applications.
   * Documents represent either a confirmed collaboration or a pending email invite.
   * Unique compound index: (applicationType, applicationId, collaboratorUserId) when userId present,
   * and (applicationType, applicationId, invitedEmail) where userId is null (pending email invites).
   */
  public static final String APPLICATION_COLLABORATOR_COLLECTION = "APPLICATION_COLLABORATOR";

  // -------------------------------------------------------------------------
  // Section-level edit locking collections (Phase 3)
  // -------------------------------------------------------------------------

  /**
   * MongoDB collection that stores per-section edit locks.
   * Each document represents a lease held by a specific user/session on a particular section.
   * TTL index on {@code expiresAt} allows MongoDB to auto-clean stale locks.
   * Unique compound index: (applicationType, applicationId, sectionId).
   */
  public static final String APPLICATION_SECTION_LOCK_COLLECTION = "APPLICATION_SECTION_LOCK";

  // -------------------------------------------------------------------------
  // Collaboration field names
  // -------------------------------------------------------------------------

  /** Field holding the application type value in collaboration/lock documents. */
  public static final String APPLICATION_TYPE_FIELD = "applicationType";

  /** Field holding the invited collaborator's email address (used for pending invites). */
  public static final String INVITED_EMAIL = "invitedEmail";

  /** Field holding the collaborator's userId (= preferred_username = ECAS ID). */
  public static final String COLLABORATOR_USER_ID = "collaboratorUserId";

  /** Field holding the collaborator's persisted display/user name for search and sorting. */
  public static final String COLLABORATOR_USER_NAME = "collaboratorUserName";

  /** Field holding the ecasId of the user who sent the invitation. */
  public static final String INVITED_BY = "invitedBy";

  /** Collaboration role: application owner. */
  public static final String ROLE_OWNER = "OWNER";

  /** Collaboration role: editor (collaborator). */
  public static final String ROLE_EDITOR = "EDITOR";

  /** Invitation status: invite sent but not yet actioned. */
  public static final String INVITATION_PENDING = "PENDING";

  /** Invitation status: collaborator accepted the invitation. */
  public static final String INVITATION_ACCEPTED = "ACCEPTED";

  /** Invitation status: collaborator rejected the invitation. */
  public static final String INVITATION_REJECTED = "REJECTED";

  /**
   * Error message returned when trying to invite an already existing collaborator.
   * This exact string is a business requirement — do not change.
   */
  public static final String ALREADY_COLLABORATOR_MESSAGE =
      "The selected user is already a collaborator of this application.";

  /** Field name for the collaboration role. */
  public static final String ROLE = "role";

  /** Field name for the invitation status. */
  public static final String INVITATION_STATUS = "invitationStatus";

  // -------------------------------------------------------------------------
  // Section lock field names
  // -------------------------------------------------------------------------

  /** Section identifier within an application (e.g. "section1" … "section10"). */
  public static final String SECTION_ID = "sectionId";

  /** UserId (ecasId) of the user currently holding the section lock. */
  public static final String LOCK_OWNER_USER_ID = "lockOwnerUserId";

  /** Browser-tab unique session ID provided by the frontend (UUID per tab). */
  public static final String LOCK_OWNER_SESSION_ID = "lockOwnerSessionId";

  /** Timestamp when the lock was first acquired. */
  public static final String ACQUIRED_AT = "acquiredAt";

  /** Timestamp of the most recent heartbeat that extended the lock. */
  public static final String LAST_HEARTBEAT_AT = "lastHeartbeatAt";

  /** Timestamp after which this lock must be treated as expired. */
  public static final String EXPIRES_AT = "expiresAt";

  /**
   * Lock lease duration in seconds: 3 minutes (180 s).
   *
   * <p>The frontend sends a heartbeat every 60 s (HEARTBEAT_INTERVAL_MS). With a 180 s lease:
   * <ul>
   *   <li>Heartbeats fire at t+60, t+120, t+180, … — 1/3 of the lease period.</li>
   *   <li>A <strong>single missed heartbeat</strong> still leaves ~60 s before expiry,
   *       giving the next heartbeat time to arrive and extend the lock.</li>
   * </ul>
   *
   * <p>Previously 120 s with a 30 s heartbeat (1/4 ratio). The ratio is now 1/3, which
   * provides the same 60 s recovery margin while halving the heartbeat frequency.
   *
   * <p>The longer lease slightly increases the time a zombie lock persists if the
   * WS-disconnect release also fails (max 180 s vs 120 s), which is acceptable.
   */
  public static final long LOCK_LEASE_DURATION_SECONDS = 180L;

  /**
   * Each heartbeat resets the expiry to: now + LOCK_HEARTBEAT_EXTENSION_SECONDS (180 s).
   */
  public static final long LOCK_HEARTBEAT_EXTENSION_SECONDS = 180L;
  public static final String FILE_UPLOAD_VALID_TYPES = ".*\\.(pdf|doc|docx|xls|xlsx|odt)$";
  public static final String APPLICATION = "application";
  public static final String STATUS = "status";
  public static final String ERROR = "error";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String APPLICATION_JSON = "application/json";
  public static final String CLIENT_ID = "client_id";
  public static final String CLIENT_SECRET = "client_secret";
  public static final String DATA_USER = "data-user";
  public static final String DATA_ADMIN = "data-admin";
  public static final String USER_ID_PARAM = "userId";
  public static final String RESULT_MESSAGE_PARAM = "result_message";
  public static final String ERRORS_PARAM = "errors";
  public static final String BASKET_COLLECTION = "BASKET";
  public static final String USER_ID = "userId";
  public static final String DISTRIBUTION_ID = "distribution_id";
  public static final String DISTRIBUTIONS = "distributions";
  public static final String DATASET_ID = "dataset_id";
  public static final String STATUS_HISTORY = "status_history";
  public static final String DATE = "date";
  public static final String DATE_SUBMITTED = "date_submitted";
  public static final String DATE_CREATED = "date_created";
  public static final String DATE_MODIFIED = "date_modified";
  public static final String DATE_DISPATCHED = "date_dispatched";
  public static final String DATE_DEADLINE = "date_deadline";
  public static final String DATE_EXTENSION_DEADLINE = "date_extension_deadline";

  public static final String RENEW_AMEND_FILE = "renew_amend_file";
  public static final String REJECTION_REASON = "rejection_reason";
  public static final String REJECTION_FILE = "rejection_file";
  public static final String REVOCATION_FILE = "revocation_file";

  public static final String VERSION = "version";
  public static final String DATASETS = "datasets";
  public static final String DATE_ADDED = "date_added";
  public static final String ID = "_id";
  public static final String APPLICATION_ID = "application_id";
  public static final String APPLICATION_WITH_ID = "Application with id: ";
  public static final String WAS_NOT_FOUND = " was not found";
  public static final String SECTION_NUMBER = "section_number";
  public static final String NOT_FOUND_FOR_USER_WITH_ID = " not found for user with id ";
  public static final String ERROR_CHECKING_DOCUMENT_EXISTENCE = "Error checking document existence: ";
  public static final String INTERNAL_SERVER_ERROR_WHILE_CHECKING_APPLICATION_EXISTENCE = "Internal server error while checking application existence";
  public static final String DELETION_CAN_NOT_BE_EXECUTED = "Deletion can be executed only in a draft application";
  public static final String PROVIDED_APPLICATION_ID = "Provided application id: ";
  public static final String WAS_NOT_FOUND_IN_THE_DATABASE = " was not found in the database";
  public static final String ERRORS = "errors";
  public static final String SECTION = "section";
  public static final String FORM = "form.";
  public static final String LEGAL_PERSON_ADDRESS = "legalPersonAddress";
  public static final String CONTACT_PERSON_ORGANISATION_NAME = "contactPersonOrganisationName";
  public static final String CONTACT_PERSON_BUSINESS_ID = "contactPersonBusinessID";
  public static final String NATURAL_PERSON_NAME = "naturalPersonName";
  public static final String NATURAL_PERSON_EMAIL = "naturalPersonEmail";
  public static final String PERSON_RESPONSIBLE_SAME_AS_CONTACT_PERSON = "personResponsibleSameAsContactPerson";
  public static final String PERSON_RESPONSIBLE_NAME = "personResponsibleName";
  public static final String PERSON_RESEARCH_SAME_AS_CONTACT_PERSON = "personResearchSameAsContactPerson";
  public static final String PERSON_RESEARCH_NAME = "personResearchName";
  public static final String INFORMATION_PROVIDER_SAME_AS_CONTACT_PERSON = "informationProviderSameAsContactPerson";
  public static final String SAME_AS_CONTACT_PERSON = "sameAsContactPerson";
  public static final String LEGAL_OR_NATURAL_PERSON = "legalOrNaturalPerson";
  public static final String LEGAL_PERSON_NAME = "legalPersonName";
  public static final String CONTACT_PERSON_EMAIL = "contactPersonEmail";
  public static final String CONTACT_PERSON_PHONE = "contactPersonPhone";
  public static final String NATURAL_PERSON_PHONE = "naturalPersonPhone";
  public static final String NATURAL_PERSON_ADDRESS = "naturalPersonAddress";
  public static final String PREFIX_SKOS_HTTP_WWW_W_3_ORG_2004_02_SKOS_CORE = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ";
  public static final String PREFIX_AT_HTTP_PUBLICATIONS_EUROPA_EU_ONTOLOGY_AUTHORITY = "PREFIX at: <http://publications.europa.eu/ontology/authority/> ";
  public static final String SELECT_COUNTRY_NAME_COUNTRY_WHERE = "SELECT ?countryName ?country WHERE { ";
  public static final String SELECT_COUNTRY_NAME_COUNTRY_NOTATION_WHERE = "SELECT ?countryName ?country ?notation WHERE { ";
  public static final String COUNTRY_SKOS_PREFLABEL_COUNTRY_NAME = "  ?country skos:prefLabel ?countryName . ";
  public static final String OPTIONAL = "  OPTIONAL { ";
  public static final String COUNTRY_AT_DEPRECATED = "    ?country at:deprecated ?deprecated . ";
  public static final String COUNTRY_SKOS_NOTATION =
    "    ?country skos:notation ?notation . " +
      "    FILTER (datatype(?notation) = <http://publications.europa.eu/ontology/euvoc#ISO_3166_1_ALPHA_2>) ";
  public static final String FILTER_LANG_COUNTRY_NAME = "  FILTER (lang(?countryName) = '";
  public static final String FILTER_BOUND_DEPRECATED = "  FILTER (!bound(?deprecated) || ?deprecated = 'false') ";
  public static final String DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES = "dataExtractionFrequencyMultipleTimesInformationRelatives";
  public static final String DATA_NEEDS_TO_BE_EXTRACTED_EVERY_RELATIVES = "dataNeedsToBeExtractedEveryRelatives";
  public static final String SPECIFY_OTHER_RELATIVES = "specifyOtherRelatives";
  public static final String DATA_EXTRACTION_FREQUENCY_RELATIVES = "dataExtractionFrequencyRelatives";
  public static final String DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION_RELATIVES1 = "dataExtractionFrequencyMultipleTimesInformationRelatives";
  public static final String DATA_EXTRACTION_FREQUENCY_MULTIPLE_TIMES_INFORMATION = "dataExtractionFrequencyMultipleTimesInformation";
  public static final String SPECIFY_OTHER = "specifyOther";
  public static final String LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU = "legalBasisForTransferringTheDataOutsideEU";
  public static final String TITLE = "title";

  /** Field name used to store the application title inside an APPLICATION_COLLABORATOR document. */
  public static final String APPLICATION_TITLE = "applicationTitle";
  public static final String INPUT_LANGUAGE = "inputLanguage";
  public static final String SECTION_1 = "section1";
  public static final String SECTION_2 = "section2";
  public static final String SECTION_3 = "section3";
  public static final String SECTION_4 = "section4";
  public static final String SECTION_5 = "section5";
  public static final String SECTION_6 = "section6";
  public static final String SECTION_7 = "section7";
  public static final String SECTION_8 = "section8";
  public static final String SECTION_9 = "section9";
  public static final String SECTION_10 = "section10";
  public static final String PURPOSE_FOR_WHICH_DATA_WILL_BE_USED = "purposeForWhichDataWillBeUsed";
  public static final String LEGAL_PERSON_COUNTRY = "legalPersonCountry";
  public static final String FORMAT_OF_THE_ELECTRONIC_HEALTH_DATA = "formatOfTheElectronicHealthData";
  public static final String IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_A = "inOrderToApplyPseudonymisedDataA";
  public static final String IN_ORDER_TO_APPLY_PSEUDONYMISED_DATA_B = "inOrderToApplyPseudonymisedDataB";
  public static final String WILL_DATA_BE_COMBINED_WITH_DATA_OBTAINED = "willDataBeCombinedWithDataObtained";
  public static final String OTHER_PERMIT_APPLICATIONS = "otherPermitApplications";
  public static final String WHEN_DATA_NEEDED = "whenDataNeeded";
  public static final String WILL_THE_DATA_BE_TRANSFERRED = "willTheDataBeTransferred";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_47 = "whyWillDataBeTransferredOutsideEUArticle47";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48 = "whyWillDataBeTransferredOutsideEUArticle48";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B = "whyWillDataBeTransferredOutsideEUArticle48b";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C = "whyWillDataBeTransferredOutsideEUArticle48c";
  public static final String COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE = "complyWithDataMinimisationPrinciple";
  public static final String COMPLY_WITH_DATA_MINIMISATION_PRINCIPLE_NOT_EU_MEMBER = "complyWithDataMinimisationPrincipleNotEUMember";
  public static final String LEGAL_BASIS_FOR_PROCESSING_PERSONAL_DATA = "legalBasisForProcessingPersonalData";
  public static final String LEGAL_BASIS_FOR_PROCESSING_COMBINED_DATA = "legalBasisForProcessingCombinedData";
  public static final String LEGAL_BASIS_FOR_PROCESSING_APPLICATION_DATA = "legalBasisForProcessingApplicationData";
  public static final String LEGAL_BASIS_FOR_PROCESSING_COMBINED_APPLICATION_DATA = "legalBasisForProcessingCombinedApplicationData";
  public static final String APPLYING_FOR_DATA_ON_BEHALF_OF_PUBLIC_SECTOR = "applyingForDataOnBehalfOfPublicSector";
  public static final String HAS_THE_STUDY_COHORT_BEEN_FORMED_BASED_ON_INFORMATION_OF_STUDY_PARTICIPANTS = "hasTheStudyCohortBeenFormedBasedOnInformationOfStudyParticipants";
  public static final String THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY = "theNatureOfTheProjectDoesNotLetYouProvideASummary";
  public static final String THE_NATURE_OF_THE_PROJECT_DOES_NOT_LET_YOU_PROVIDE_A_SUMMARY_REASON = "theNatureOfTheProjectDoesNotLetYouProvideASummaryReason";
  public static final String APPLYING_FOR_DATA_FOR_CARRYING_OUT_TASKS = "applyingForDataForCarryingOutTasks";
  public static final String NEED_FOR_DATA_EXTRACTION_EVERY = "needForDataExtractionEvery";
  public static final String WILL_CONTROLS_BE_EXTRACTED = "willControlsBeExtracted";
  public static final String WILL_SAME_DATA_BE_EXTRACTED_FOR_CONTROLS = "willSameDataBeExtractedForControls";
  public static final String WILL_DATA_FOR_CONTROLS_BE_EXTRACTED_SIMULTANEOUSLY = "willDataForControlsBeExtractedSimultaneously";
  public static final String DATA_EXTRACTION_FREQUENCY = "dataExtractionFrequency";
  public static final String WILL_RELATIVES_BE_EXTRACTED = "willRelativesBeExtracted";
  public static final String HAS_THE_GROUP_RELATIVES_FORMED_3= "hasTheGroupRelativesFormed3";
  public static final String HAS_THE_GROUP_RELATIVES_FORMED_2= "hasTheGroupRelativesFormed2";
  public static final String DATA_NEEDS_TO_BE_EXTRACTED_EVERY = "dataNeedsToBeExtractedEvery";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_C_OPT = "whyWillDataBeTransferredOutsideEUArticle48cOpt";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_B_OPT = "whyWillDataBeTransferredOutsideEUArticle48bOptions";
  public static final String DISPATCHER_COLLECTION = "DISPATCHER";
  public static final String OUTGOING_APPLICATION_ACTIVITIES_COLLECTION = "OUTGOING_APPLICATION_ACTIVITIES";
  public static final String TRANSLATIONS_STATE_COLLECTION = "TRANSLATIONS_STATE";
  public static final String DATA_PERMIT_ID = "data_permit_id";
  public static final String CATALOG_IDS = "catalog_ids";
  public static final String LABEL = "label";
  public static final String VALUE = "value";
  public static final String TOOLTIP = "tooltip";
  public static final String PUBLISHER = "publisher";
  public static final String PROVENANCE = "provenance";
  public static final String COUNTRY_OF_PROJECT_LEADER = "countryOfProjectLeader";
  public static final String COUNTRY_CODE = "countryCode";
  public static final String NUMBER = "number";
  public static final String NAME = "name";
  public static final String FULL_NAME = "fullName";
  public static final String ADDRESS = "address";
  public static final String PHONE = "phone";
  public static final String EMAIL = "email";
  public static final String SUMMARY_OF_PLAN_FOR_USING_THE_DATA = "summaryOfPlanForUsingTheData";
  public static final String SUMMARY_OF_RESEARCH_PLAN = "summaryOfResearchPlan";
  public static final String HEADING_3 = "Heading3";
  public static final String PERMIT_DECISION_ATTACHMENT = "permitDecisionAttachment";
  public static final String INFORMATION_PROVIDER_PHONE = "informationProviderPhone";
  public static final String WHY_NEED_DATA_OF_A_WHOLE_POPULATION = "whyNeedDataOfaWholePopulation";
  public static final String SAMPLE_SIZE = "sampleSize";
  public static final String INCLUSION_CRITERIA = "inclusionCriteria";
  public static final String MORE_INFO_ON_EXTRACTION_PERIODS = "moreInfoOnExtractionPeriods";
  public static final String MORE_INFO_ON_EXTRACTION_PERIODS_RELATIVES = "moreInfoOnExtractionPeriodsRelatives";
  public static final String ORDER_OF_CONTROLS_DATA_EXTRACTION = "orderOfControlsDataExtraction";
  public static final String ORDER_OF_RELATIVES_DATA_EXTRACTION = "orderOfRelativesDataExtraction";
  public static final String WILL_SAME_DATA_BE_EXTRACTED_FOR_RELATIVES = "willSameDataBeExtractedForRelatives";
  public static final String WILL_DATA_FOR_RELATIVES_BE_EXTRACTED_SIMULTANEOUSLY = "willDataForRelativesBeExtractedSimultaneously";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_A = "whyWillDataBeTransferredOutsideEUArticle48a";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_D = "whyWillDataBeTransferredOutsideEUArticle48d";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_48_E = "whyWillDataBeTransferredOutsideEUArticle48e";
  public static final String WHY_WILL_DATA_BE_TRANSFERRED_OUTSIDE_EU_ARTICLE_49 = "whyWillDataBeTransferredOutsideEUArticle49";
  public static final String WHICH_ORGANIZATION_WILL_BE_THE_CONTROLLER_OF_DATA = "whichOrganizationWillBeTheControllerOfData";
  public static final String PROTECTION_AND_SECURITY_STATEMENTS_A = "protectionAndSecurityStatementsA";
  public static final String PROTECTION_AND_SECURITY_STATEMENTS_B = "protectionAndSecurityStatementsB";
  public static final String PROTECTION_AND_SECURITY_STATEMENTS_C = "protectionAndSecurityStatementsC";
  public static final String PROTECTION_AND_SECURITY_STATEMENTS_D = "protectionAndSecurityStatementsD";
  public static final String PROTECTION_AND_SECURITY_STATEMENTS_E = "protectionAndSecurityStatementsE";
  public static final String ADDITIONAL_INFORMATION = "additionalInformation";
  public static final String ADDITIONAL_ATTACHMENT = "additionalAttachment";
  public static final String AWARE_PROCESSING_FEE = "awareProcessingFee";
  public static final String AWARE_CHARGE_FEE = "awareChargeFee";
  public static final String AWARE_INFORMATION_CORRECT = "awareInformationCorrect";
  public static final String CONFIRM_THAT_DATA_PERMIT_HAS_BEEN_GRANTED_FOR_THE_RESEARCH_PROJECT = "confirmThatDataPermitHasBeenGrantedForTheResearchProject";
  public static final String TABULATION_PLAN = "tabulationPlan";
  public static final String TABULATION_PLAN_ARRAY = "tabulationPlanArray";
  public static final String EXTRACTION_DESCRIPTION_AND_COMPILATION = "extractionDescriptionAndCompilation";
  public static final String NATURAL_PERSON_JOB_TITLE = "naturalPersonJobTitle";
  public static final String NATURAL_PERSON_AFFILIATION = "naturalPersonAffiliation";
  public static final String ACCEPT_HEALTH_DATA_BODY = "acceptHealthDataBody";
  public static final String HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED_ONCE_OR_MULTIPLE = "howOftenDoesTheDataNeedToBeExtractedOnceOrMultiple";
  public static final String OPT_OUT_OF_THE_MECHANISM_PROVIDED_IN_THE_NATIONAL_LAW= "optOutOfTheMechanismProvidedInTheNationalLaw";
  public static final String PROVIDED_JUSTIFICATION_EXCEPTION= "providedJustificationException";
  public static final String EXTERNAL_DATASET_ID = "external_dataset_id";
  public static final String RESULT = "result";
  public static final String DATA = "/data/";
  public static final String APPLICATIONID = "applicationId";
  public static final String JSON = ".json";
  public static final String TRANSLATIONS = "/translations/";
  public static final String NOT_FOUND = " not found.";
  public static final String JSON_FILE_FOR = "Json file for ";
  public static final String TRIPLE_LOG_ABBREVIATION = "{}{}{}";
  public static final String USER_COLLECTION = "USER";
  public static final String RESULT_MESSAGE = "result_message";
  public static final String THE_PARSING_OF_JSON_FAILED_WITH_ERROR = "The parsing of json failed with error: ";
  public static final String CATALOG_ID = "catalog_id";
  public static final String MESSAGE = "message";
  public static final String MANDATORY_FIELD = "mandatoryField";
  public static final String ERROR_CODE = "errorCode";
  public static final String VARIABLES_FOR_DATA_EXTRACTION_ATTACHMENTS = "variablesForDataExtractionAttachments";
  public static final String TIME_PERIOD_FOR_DATA_EXTRACTION = "timePeriodForDataExtraction";
  public static final String EXTRACTION_CRITERIA_FOR_CONTROLS = "extractionCriteriaForControls";
  public static final String PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM = "previouslyIssuedPermitValidityPeriodFrom2";
  public static final String VARIABLES_FOR_DATA_EXTRACTION_RELATIVES_ATTACHMENTS= "variablesForDataExtractionRelativesAttachments";
  public static final String TIME_PERIOD_FOR_DATA_EXTRACTION_RELATIVES = "timePeriodForDataExtractionRelatives";
  public static final String RELATIONSHIP_TO_STUDY_COHORT = "relationshipToStudyCohort";
  public static final String LEGAL_BASIS_FOR_TRANSFERRING_THE_DATA_OUTSIDE_EU_OTHER_OPTIONS = "legalBasisForTransferringTheDataOutsideEUOtherOptions";
  public static final String PARSING_FAILED = "The parsing of json failed with error: ";
  public static final String INFORMATION_PROVIDER_NAME = "informationProviderName";
  public static final String INFORMATION_PROVIDER_EMAIL = "informationProviderEmail";
  public static final String PENDING = "pending";
  public static final String FAILED = "failed";
  public static final String COMPLETED = "completed";
  public static final String IN_PROGRESS = "in_progress";
  public static final String CONFIGURATION_STATUS = "configuration.status";
  public static final String CONFIGURATION_RETRIES = "configuration.retries";
  public static final String CONFIGURATION_APPLICATION_TYPE = "configuration.application_type";
  public static final String CATALOGUE_NAME = "catalogueName";
  public static final String DESCRIPTION = "description";
  public static final String PUBLISHER_HOMEPAGE_URL = "publisherHomepageURL";
  public static final String PUBLISHER_NAME = "publisherName";
  public static final String CATALOGUE_ID = "catalogueId";
  public static final String FILENAME =  "filename";
  public static final String UPLOAD_RDF= "uploadRDF";
  public static final String DOMIBUS_ID = "domibusId";
  public static final String GEOGRAPHICAL_COVERAGE ="geographicalCoverage";
  public static final String CATALOGUE_LANGUAGE ="language";
  public static final String RDF_TEMPLATE ="rdfTemplate";
  public static final String FILE_UPLOAD_CATALOGUE_VALID_TYPES = ".*\\.(rdf|xml|jsonld|json|ttl|nt|n3)$";
  public static final String PAGE = "page";
  public static final String LIMIT = "limit";
  public static final String SORT_BY = "sortBy";
  public static final String SORT_ORDER = "sortOrder";
  public static final String DESC = "desc";
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String SUBMITTED = "SUBMITTED";
  public static final String SENDING = "SENDING";
  public static final String PARAMETER_TYPE ="parameterType";
  public static final String PARAMETER_ID ="parameterId";
  public static final String AUDIT_LOG_DATASET_SEARCH_INTERVAL = "AUDIT_LOG_SEARCH_INTERVAL";
  public static final long DEFAULT_AUDIT_LOG_DATASET_SEARCH_INTERVAL = 60_000;
  public static final String AUDIT_LOG_DATASET_MAX_RETRIES = "AUDIT_LOG_MAX_RETRIES";
  public static final int DEFAULT_AUDIT_LOG_DATASET_MAX_RETRIES = 10;
  public static final int MAX_ALLOWED_CHARS_50 = 50;
  public static final int MAX_ALLOWED_CHARS_36 = 36;
  public static final String OBJECT = "object";
  public static final String OBJECT_ID = "objectId";
  public static final String PARENT_ID = "parentId";
  public static final String CONTENT = "content";
  public static final String FORM_TRANSLATIONS = "form_translations";
  public static final String ACTION_DESCRIPTION = "actionDescription";
  public static final String ECAS_ID = "ecasId";
  public static final String REGEX = "$regex";
  public static final String FILENAME_ASTERISK_EQUALS = "filename*=";
  public static final String OPTIONS = "$options";
  public static final String ACTION = "action";
  public static final String FILE_ID = "fileId";
  public static final String FILE_NAME = "fileName";
  public static final String NOTATION = "notation";
  public static final String TRANSLATION = "translation";
  public static final String PENDING_FILE_TRANSLATIONS = "pendingFileTranslations";
  public static final String COUNTRY = "country";
  public static final String TARGET_LANGUAGES = "targetLanguages";
  public static final String SOURCE_LANGUAGE = "sourceLanguage";
  public static final String CONFIGURATION_COLLECTION = "CONFIGURATION";
  public static final String CONTROLLED_VOCABULARY_COLLECTION = "CONTROLLED_VOCABULARY";
  public static final String DATASET_ASSISTANT_COLLECTION = "DDA_DATASET";
  public static final String FAVOURITES_COLLECTION = "FAVOURITES";
  public static final String TYPE = "type";
  public static final String CREATED_AT = "createdAt";
  public static final String UPDATED_AT = "updatedAt";
  public static final String LEGAL_BASIS = "legalBasis";
  public static final String NEED_FOR_DATA_EXTRACTION_EVERY_DESCRIPTION = "needForDataExtractionEveryDescription";
  public static final String PAYLOAD = "payload";
  public static final String USER_NAME = "userName";
  public static final String HOW_IS_THE_STUDY_COHORT_FORMED = "howIsTheStudyCohortFormed";
  public static final String HOW_OFTEN_DOES_THE_DATA_NEED_TO_BE_EXTRACTED = "howOftenDoesTheDataNeedToBeExtracted";
  public static final String PROVIDE_INFORMATION_LIST = "provideInformationList";
  public static final String PROJECT_LEADER = "projectLeader";
  public static final String VARIABLES = "variables";
  public static final String IS_THE_PROJECT_FINANCIALLY_COVERED = "isTheProjectFinanciallyCovered";
  public static final String INVOICE_TYPE = "invoiceType";
  public static final String X_API_KEY = "X-API-Key";
  public static final String UNAUTHORIZED = "Unauthorized";
  public static final String OPERATION = "operation";
  public static final String UNKNOWN_APPLICATION_TYPE = "Unknown application type";
  public static final String PREVIOUSLY_ISSUED_PERMIT_DATE_2 = "previouslyIssuedPermitDate2";
  public static final String PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM_2 = "previouslyIssuedPermitValidityPeriodFrom2";
  public static final String PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_TO_2 = "previouslyIssuedPermitValidityPeriodTo2";
  public static final String PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_TO_3 = "previouslyIssuedPermitValidityPeriodTo3";
  public static final String PREVIOUSLY_ISSUED_PERMIT_NUMBER_2 = "previouslyIssuedPermitNumber2";
  public static final String PREVIOUSLY_ISSUED_PERMIT_NUMBER_3 = "previouslyIssuedPermitNumber3";
  public static final String PREVIOUSLY_ISSUED_PERMIT_VALIDITY_PERIOD_FROM_3 = "previouslyIssuedPermitValidityPeriodFrom3";
  public static final String PREVIOUSLY_ISSUED_PERMIT_ISSUER_2 = "previouslyIssuedPermitIssuer2";
  public static final String PREVIOUSLY_ISSUED_PERMIT_ISSUER_3 = "previouslyIssuedPermitIssuer3";
  public static final String PREVIOUSLY_ISSUED_PERMIT_DATE_3 = "previouslyIssuedPermitDate3";
  public static final Set<String> SUPPORTED_LANGUAGES = Set.of(
    "bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "ga", "hr", "hu", "is", "it", "lt",
    "lv", "mt", "nb", "nl", "nn", "pl", "pt", "ro", "sk", "sl", "sv"
  );
  public static final Set<String> SUPPORTED_LANGUAGES_UPPER = SUPPORTED_LANGUAGES.stream()
    .map(str -> str.toUpperCase(Locale.ENGLISH))
    .collect(Collectors.toUnmodifiableSet());
  public static final Set<Integer> DEFAULT_DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS = Set.of(1, 10);
  public static final Set<Integer> DEFAULT_DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS = Set.of(1, 10);
  public static final Set<Integer> DEFAULT_DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS = Set.of(1, 10);
  public static final String DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS_LABEL = "DAYS_TO_DISPLAY_PERMIT_NOTIFICATIONS";
  public static final String DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS_LABEL = "DAYS_TO_DISPLAY_DATASET_NOTIFICATIONS";
  public static final String DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS_LABEL = "DAYS_TO_DISPLAY_CATALOG_NOTIFICATIONS";
  public static final long PERMIT_NOTIFICATIONS_DEFAULT_INTERVAL = 24 * 60 * 60 * 1000L;
  public static final long DATASET_NOTIFICATIONS_DEFAULT_INTERVAL = 24 * 60 * 60 * 1000L;
  public static final long CATALOG_NOTIFICATIONS_DEFAULT_INTERVAL = 24 * 60 * 60 * 1000L;
  public static final String WITHDRAWN_TIME_PERIOD = "WITHDRAWN_TIME_PERIOD";
  public static final Integer WITHDRAWN_TIME_DEFAULT_PERIOD = 28;
  //public static final long ONE_MINUTE_INTERVAL = 60 * 1000L;
  public static final String PERMIT_NOTIFICATIONS_INTERVAL = "PERMIT_NOTIFICATIONS_INTERVAL";
  public static final String DATASET_NOTIFICATIONS_INTERVAL = "DATASET_NOTIFICATIONS_INTERVAL";
  public static final String CATALOG_NOTIFICATIONS_INTERVAL = "CATALOG_NOTIFICATIONS_INTERVAL";
  public static final String LANGUAGE_SCHEME = "http://publications.europa.eu/resource/authority/language";
  public static final Set<String> SUPPORTED_LANGUAGE_CONCEPTS = Set.of(
    LANGUAGE_SCHEME + "/BUL", // Bulgarian
    LANGUAGE_SCHEME + "/CES", // Czech
    LANGUAGE_SCHEME + "/DAN", // Danish
    LANGUAGE_SCHEME + "/DEU", // German
    LANGUAGE_SCHEME + "/ELL", // Greek
    LANGUAGE_SCHEME + "/ENG", // English
    LANGUAGE_SCHEME + "/EST", // Estonian
    LANGUAGE_SCHEME + "/FIN", // Finnish
    LANGUAGE_SCHEME + "/FRA", // French
    LANGUAGE_SCHEME + "/GLE", // Irish
    LANGUAGE_SCHEME + "/HRV", // Croatian
    LANGUAGE_SCHEME + "/HUN", // Hungarian
    LANGUAGE_SCHEME + "/ITA", // Italian
    LANGUAGE_SCHEME + "/ISL", // Icelandic
    LANGUAGE_SCHEME + "/LAV", // Latvian
    LANGUAGE_SCHEME + "/LIT", // Lithuanian
    LANGUAGE_SCHEME + "/MLT", // Maltese
    LANGUAGE_SCHEME + "/NLD", // Dutch
    LANGUAGE_SCHEME + "/NNO", // Norwegian Nynorsk
    LANGUAGE_SCHEME + "/NOB", // Norwegian Bokmål
    LANGUAGE_SCHEME + "/POL", // Polish
    LANGUAGE_SCHEME + "/POR", // Portuguese
    LANGUAGE_SCHEME + "/RON", // Romanian
    LANGUAGE_SCHEME + "/SLK", // Slovak
    LANGUAGE_SCHEME + "/SLV", // Slovenian
    LANGUAGE_SCHEME + "/SPA", // Spanish
    LANGUAGE_SCHEME + "/SWE" // Swedish
  );
  public static final String COUNTRY_SCHEME = "http://publications.europa.eu/resource/authority/country";
  public static final Set<String> SUPPORTED_COUNTRY_CONCEPTS = Set.of(
    COUNTRY_SCHEME + "/AUT", // Austria
    COUNTRY_SCHEME + "/BEL", // Belgium
    COUNTRY_SCHEME + "/BGR", // Bulgaria
    COUNTRY_SCHEME + "/HRV", // Croatia
    COUNTRY_SCHEME + "/CYP", // Cyprus
    COUNTRY_SCHEME + "/CZE", // Czechia
    COUNTRY_SCHEME + "/DNK", // Denmark
    COUNTRY_SCHEME + "/EST", // Estonia
    COUNTRY_SCHEME + "/FIN", // Finland
    COUNTRY_SCHEME + "/FRA", // France
    COUNTRY_SCHEME + "/DEU", // Germany
    COUNTRY_SCHEME + "/GRC", // Greece
    COUNTRY_SCHEME + "/HUN", // Hungary
    COUNTRY_SCHEME + "/IRL", // Ireland
    COUNTRY_SCHEME + "/ITA", // Italy
    COUNTRY_SCHEME + "/LVA", // Latvia
    COUNTRY_SCHEME + "/LTU", // Lithuania
    COUNTRY_SCHEME + "/LUX", // Luxembourg
    COUNTRY_SCHEME + "/MLT", // Malta
    COUNTRY_SCHEME + "/NLD", // Netherlands
    COUNTRY_SCHEME + "/POL", // Poland
    COUNTRY_SCHEME + "/PRT", // Portugal
    COUNTRY_SCHEME + "/ROU", // Romania
    COUNTRY_SCHEME + "/SVK", // Slovakia
    COUNTRY_SCHEME + "/SVN", // Slovenia
    COUNTRY_SCHEME + "/ESP", // Spain
    COUNTRY_SCHEME + "/SWE"  // Sweden
  );

  /** MongoDB collection that stores pending ownership transfer requests from editors. */
  public static final String OWNERSHIP_REQUEST_COLLECTION = "OWNERSHIP_REQUESTS";

  /** Field holding the ownership transfer request justification. */
  public static final String JUSTIFICATION = "justification";

  /** Field holding the primary section that triggered a dependent lock group. */
  public static final String PRIMARY_SECTION_ID = "primarySectionId";
}
