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

import static eu.europa.data.health.access.Constants.ACQUIRED_AT;
import static eu.europa.data.health.access.Constants.APPLICATION_ID;
import static eu.europa.data.health.access.Constants.APPLICATION_SECTION_LOCK_COLLECTION;
import static eu.europa.data.health.access.Constants.APPLICATION_TYPE_FIELD;
import static eu.europa.data.health.access.Constants.DATA_ACCESS_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static eu.europa.data.health.access.Constants.EXPIRES_AT;
import static eu.europa.data.health.access.Constants.LAST_HEARTBEAT_AT;
import static eu.europa.data.health.access.Constants.LOCK_HEARTBEAT_EXTENSION_SECONDS;
import static eu.europa.data.health.access.Constants.LOCK_LEASE_DURATION_SECONDS;
import static eu.europa.data.health.access.Constants.LOCK_OWNER_SESSION_ID;
import static eu.europa.data.health.access.Constants.LOCK_OWNER_USER_ID;
import static eu.europa.data.health.access.Constants.PRIMARY_SECTION_ID;
import static eu.europa.data.health.access.Constants.SECTION_1;
import static eu.europa.data.health.access.Constants.SECTION_10;
import static eu.europa.data.health.access.Constants.SECTION_2;
import static eu.europa.data.health.access.Constants.SECTION_3;
import static eu.europa.data.health.access.Constants.SECTION_4;
import static eu.europa.data.health.access.Constants.SECTION_5;
import static eu.europa.data.health.access.Constants.SECTION_6;
import static eu.europa.data.health.access.Constants.SECTION_7;
import static eu.europa.data.health.access.Constants.SECTION_8;
import static eu.europa.data.health.access.Constants.SECTION_9;
import static eu.europa.data.health.access.Constants.SECTION_ID;

import eu.europa.data.health.access.Context;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service class implementing backend-authoritative section-level edit locking with
 * real-time WebSocket broadcasting.
 *
 * <h3>Overview</h3>
 * <p>Applications are divided into sections (section1 … section10).  When a user
 * opens a section for editing, they acquire a "lease lock" that expires after
 * {@value eu.europa.data.health.access.Constants#LOCK_LEASE_DURATION_SECONDS} seconds.
 * The frontend must send heartbeats every 30 seconds to keep the lock alive.
 *
 * <h3>Key design decisions</h3>
 * <ul>
 *   <li><strong>Atomic acquisition</strong>: uses a two-phase approach (update-only,
 *       then insert-if-absent) to avoid E11000 duplicate-key errors.</li>
 *   <li><strong>No infinite locks</strong>: {@code expiresAt} is a hard deadline.</li>
 *   <li><strong>TTL index</strong>: MongoDB auto-removes expired lock documents.</li>
 *   <li><strong>Session isolation</strong>: locks are scoped to (userId + sessionId).</li>
 *   <li><strong>WebSocket broadcasting</strong>: on acquire and release,
 *       {@link LockWebSocketManager#broadcast} is called so all connected clients
 *       update their lock indicators immediately — no polling needed.</li>
 * </ul>
 *
 * <h3>Write-time enforcement</h3>
 * <p>Any section save endpoint should call {@link #assertLockHeld(String, String, String, String)}
 * before persisting data.
 */
public class SectionLockManager extends AbstractManager {

  private static final Logger log = LoggerFactory.getLogger(SectionLockManager.class);

  // -------------------------------------------------------------------------
  // Section dependency maps (dependent section locking)
  // -------------------------------------------------------------------------
  //
  // When a user edits section X, all sections in its lock group must also be
  // locked to prevent concurrent editing of interdependent data (data copies
  // and visibility-control dependencies).  The maps below encode the direct
  // bidirectional dependencies for each application type.  The lock group for
  // a section is: {section itself} ∪ {all sections it is directly connected to}.
  //
  // Dependencies included:
  //   - "Same as contact person" data-copy checkboxes
  //   - Field-visibility controls (v-if bound to another section's field)
  // Dependencies excluded:
  //   - Read-only display dependencies (e.g. S1→S6 datasets)
  //
  // Directionality: bidirectional (editing source OR consumer locks both).
  // Transitivity: 1-hop only — no transitive closure.
  // -------------------------------------------------------------------------

  /**
   * Data Access Application — section dependency map.
   *
   * <p>Direct dependencies:
   * <ul>
   *   <li>S3 → S4 : "Same as contact person" copies + legalOrNaturalPerson visibility</li>
   *   <li>S3 → S5 : "Same as contact person" copies person name</li>
   *   <li>S3 → S6 : "Same as contact person" copies info provider fields</li>
   *   <li>S3 → S8 : "Same as contact person" copies + applyingForDataOnBehalfOfPublicSector visibility</li>
   *   <li>S2 → S5 : purposeForWhichDataWillBeUsed controls field visibility</li>
   *   <li>S2 → S8 : purposeForWhichDataWillBeUsed controls isPurposeForE computed
   *       property and attachResearchPermitIfRequired cleanup</li>
   *   <li>S5 → S8 : formatOfTheElectronicHealthData controls visibility</li>
   *   <li>S7 → S8 : willDataBeCombinedWithDataObtained controls visibility</li>
   * </ul>
   *
   * <p>Lock groups (bidirectional, 1-hop):
   * <table>
   *   <tr><td>section1</td><td>{1}</td></tr>
   *   <tr><td>section2</td><td>{2, 5, 8}</td></tr>
   *   <tr><td>section3</td><td>{3, 4, 5, 6, 8}</td></tr>
   *   <tr><td>section4</td><td>{3, 4}</td></tr>
   *   <tr><td>section5</td><td>{2, 3, 5, 8}</td></tr>
   *   <tr><td>section6</td><td>{3, 6}</td></tr>
   *   <tr><td>section7</td><td>{7, 8}</td></tr>
   *   <tr><td>section8</td><td>{2, 3, 5, 7, 8}</td></tr>
   *   <tr><td>section9</td><td>{9}</td></tr>
   *   <tr><td>section10</td><td>{10}</td></tr>
   * </table>
   */
  static final Map<String, Set<String>> DATA_ACCESS_SECTION_DEPENDENCIES;

  /**
   * Data Request Application — section dependency map.
   *
   * <p>Direct dependencies:
   * <ul>
   *   <li>S3 → S4 : "Same as contact person" copies + legalOrNaturalPerson visibility</li>
   *   <li>S3 → S5 : "Same as contact person" copies person name</li>
   *   <li>S2 → S5 : purposeForWhichDataWillBeUsed controls field visibility</li>
   *   <li>S2 → S6 : purposeForWhichDataWillBeUsed controls whatIsTheFrequencyOfUpdates</li>
   * </ul>
   *
   * <p>Lock groups (bidirectional, 1-hop):
   * <table>
   *   <tr><td>section1</td><td>{1}</td></tr>
   *   <tr><td>section2</td><td>{2, 5, 6}</td></tr>
   *   <tr><td>section3</td><td>{3, 4, 5}</td></tr>
   *   <tr><td>section4</td><td>{3, 4}</td></tr>
   *   <tr><td>section5</td><td>{2, 3, 5}</td></tr>
   *   <tr><td>section6</td><td>{2, 6}</td></tr>
   *   <tr><td>section7</td><td>{7}</td></tr>
   *   <tr><td>section8</td><td>{8}</td></tr>
   * </table>
   */
  static final Map<String, Set<String>> DATA_REQUEST_SECTION_DEPENDENCIES;

  static {
    // --- Data Access Application dependencies (bidirectional edges) ---
    Map<String, Set<String>> daa = new HashMap<>();
    daa.put(SECTION_1,  Set.of(SECTION_1));
    daa.put(SECTION_2,  Set.of(SECTION_2, SECTION_5, SECTION_8));
    daa.put(SECTION_3,  Set.of(SECTION_3, SECTION_4, SECTION_5, SECTION_6, SECTION_8));
    daa.put(SECTION_4,  Set.of(SECTION_3, SECTION_4));
    daa.put(SECTION_5,  Set.of(SECTION_2, SECTION_3, SECTION_5, SECTION_8));
    daa.put(SECTION_6,  Set.of(SECTION_3, SECTION_6));
    daa.put(SECTION_7,  Set.of(SECTION_7, SECTION_8));
    daa.put(SECTION_8,  Set.of(SECTION_2, SECTION_3, SECTION_5, SECTION_7, SECTION_8));
    daa.put(SECTION_9,  Set.of(SECTION_9));
    daa.put(SECTION_10, Set.of(SECTION_10));
    DATA_ACCESS_SECTION_DEPENDENCIES = Collections.unmodifiableMap(daa);

    // --- Data Request Application dependencies (bidirectional edges) ---
    Map<String, Set<String>> dra = new HashMap<>();
    dra.put(SECTION_1, Set.of(SECTION_1));
    dra.put(SECTION_2, Set.of(SECTION_2, SECTION_5, SECTION_6));
    dra.put(SECTION_3, Set.of(SECTION_3, SECTION_4, SECTION_5));
    dra.put(SECTION_4, Set.of(SECTION_3, SECTION_4));
    dra.put(SECTION_5, Set.of(SECTION_2, SECTION_3, SECTION_5));
    dra.put(SECTION_6, Set.of(SECTION_2, SECTION_6));
    dra.put(SECTION_7, Set.of(SECTION_7));
    dra.put(SECTION_8, Set.of(SECTION_8));
    DATA_REQUEST_SECTION_DEPENDENCIES = Collections.unmodifiableMap(dra);
  }

  /**
   * Returns the lock group for a given section in the specified application type.
   * The lock group includes the section itself plus all directly connected sections.
   * Returns a singleton set of just the section if no dependency map is found.
   *
   * @param applicationType the application type constant
   * @param sectionId       the section identifier (e.g. SECTION_3)
   * @return unmodifiable set of section IDs that must be locked together
   */
  static Set<String> computeLockGroup(String applicationType, String sectionId) {
    Map<String, Set<String>> deps;
    if (DATA_ACCESS_APPLICATION_COLLECTION.equals(applicationType)) {
      deps = DATA_ACCESS_SECTION_DEPENDENCIES;
    } else if (DATA_REQUEST_APPLICATION_COLLECTION.equals(applicationType)) {
      deps = DATA_REQUEST_SECTION_DEPENDENCIES;
    } else {
      return Set.of(sectionId);
    }
    return deps.getOrDefault(sectionId, Set.of(sectionId));
  }

  /** HTTP 409 message used when a lock is already held by another user/session. */
  public static final String LOCK_CONFLICT_MSG = "Section is currently locked by another user.";

  /** HTTP 409 message used when the same user tries to lock from a different tab. */
  public static final String SAME_USER_DIFFERENT_TAB_MSG =
      "You already hold this lock in another tab.";

  /** HTTP 409 message returned when a section write is attempted without a valid lock. */
  public static final String WRITE_REJECTED_NO_LOCK_MSG =
      "You must hold the section lock before saving. Acquire the lock first.";

  private final MongoClient mongoClient;

  /**
   * WebSocket broadcast manager — notifies all connected clients when a lock
   * is acquired or released so the UI updates in real time without polling.
   */
  private final LockWebSocketManager wsManager;

  /**
   * Constructs a new {@code SectionLockManager}.
   *
   * @param dbHandler   shared DB handler for low-level operations
   * @param mongoClient raw Mongo client for atomic lock operations
   * @param wsManager   WebSocket manager for real-time lock event broadcasting
   */
  public SectionLockManager(DBHandler dbHandler, MongoClient mongoClient,
      LockWebSocketManager wsManager) {
    super(dbHandler);
    this.mongoClient = mongoClient;
    this.wsManager = wsManager;
  }

  // ---------------------------------------------------------------------------
  // POST /api/applications/:type/:id/sections/:sectionId/lock/acquire
  // ---------------------------------------------------------------------------

  /**
   * Acquires section locks for the authenticated user on the primary section
   * and all its dependent sections (the "lock group").
   *
   * <p>The lock group is determined by
   * {@link #computeLockGroup(String, String)}.  All sections in the
   * group must be available (no active lock by another user).  If ANY section
   * in the group is held by another user, the entire acquisition is rolled back
   * and HTTP 409 is returned.
   *
   * <p>Request body: {@code { "sessionId": "<UUID per browser tab>" }}
   *
   * @param context Vert.x routing context; path params:
   *                {@code type}, {@code id}, {@code sectionId}
   */
  public void acquireLock(@NotNull RoutingContext context) {
    String applicationType = context.pathParam("type");
    String applicationId   = context.pathParam("id");
    String sectionId       = context.pathParam("sectionId");
    String ecasId          = Context.getEcasId(context);
    JsonObject body        = context.body().asJsonObject();

    if (body == null || StringUtils.isBlank(body.getString("sessionId"))) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Request body must contain 'sessionId'");
      return;
    }

    String sessionId = body.getString("sessionId");

    if (!isValidSectionId(sectionId)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Invalid sectionId. Must be section1 through section10.");
      return;
    }

    Set<String> lockGroup = computeLockGroup(applicationType, sectionId);

    acquireLockGroup(applicationType, applicationId, sectionId, lockGroup, ecasId, sessionId)
      .onSuccess(lockedDocs -> {
        Instant expiresAt = Instant.parse(lockedDocs.get(0).getString(EXPIRES_AT));
        // Build response with lockedSections array
        JsonArray lockedSections = new JsonArray();
        for (JsonObject doc : lockedDocs) {
          lockedSections.add(normalizeLockDoc(doc));
        }
        JsonObject successBody = new JsonObject()
          .put("locked", true)
          .put(LOCK_OWNER_USER_ID, ecasId)
          .put(LOCK_OWNER_SESSION_ID, sessionId)
          .put(PRIMARY_SECTION_ID, sectionId)
          .put(ACQUIRED_AT, lockedDocs.get(0).getString(ACQUIRED_AT))
          .put(EXPIRES_AT, expiresAt.toString())
          .put("lockedSections", lockedSections);
        setUpContextResponse(context, HttpResponseStatus.OK, successBody.encode());

        // Broadcast lock_acquired for each section in the group
        for (JsonObject doc : lockedDocs) {
          wsManager.broadcastLockAcquired(
            applicationType,
            applicationId,
            doc.getString(SECTION_ID),
            ecasId,
            sessionId,
            expiresAt.toString(),
            sectionId
          );
        }
      })
      .onFailure(err -> {
        if (err instanceof LockConflictException conflictErr) {
          JsonObject conflictBody = new JsonObject()
            .put("locked", false)
            .put("reason", conflictErr.getMessage())
            .put(LOCK_OWNER_USER_ID, conflictErr.lockOwnerUserId)
            .put(PRIMARY_SECTION_ID, conflictErr.conflictSectionId)
            .put(EXPIRES_AT, conflictErr.expiresAt);
          setUpContextResponse(context, HttpResponseStatus.CONFLICT, conflictBody.encode());
        } else {
          log.error("acquireLock failed: {}", err.getMessage(), err);
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Failed to acquire lock"
          );
        }
      });
  }

  /**
   * Acquires locks for all sections in a lock group.
   *
   * <p>For each section, uses the two-phase approach (update-only → insert-if-absent).
   * If any section is held by another user, rolls back all newly acquired locks
   * and fails with a {@link LockConflictException}.
   *
   * @return a succeeded future with the list of lock documents on success
   */
  private Future<List<JsonObject>> acquireLockGroup(
    String applicationType,
    String applicationId,
    String primarySectionId,
    Set<String> lockGroup,
    String ecasId,
    String sessionId
  ) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(LOCK_LEASE_DURATION_SECONDS);

    // First: check for same-user different-tab conflict on ANY section in group
    return checkSameUserDifferentTab(applicationType, applicationId, lockGroup, ecasId, sessionId)
      .compose(v -> {
        // Sequentially acquire each section in the group
        Future<List<JsonObject>> chain = Future.succeededFuture(new ArrayList<>());
        for (String sid : lockGroup) {
          chain = chain.compose(acquiredDocs ->
            acquireSingleSection(
              applicationType,
              applicationId,
              sid,
              primarySectionId,
              ecasId,
              sessionId,
              now,
              expiresAt
            ).map(doc -> {
              acquiredDocs.add(doc);
              return acquiredDocs;
            })
          );
        }
        return chain;
      })
      .recover(err -> {
        // On any conflict, rollback all locks acquired in this call by this session
        if (err instanceof LockConflictException) {
          return rollbackSessionLocks(applicationType, applicationId, sessionId, lockGroup).compose(
            v -> Future.failedFuture(err)
          );
        }
        return Future.failedFuture(err);
      });
  }

  /**
   * Checks whether the same user holds an active lock on any section in the group
   * from a different browser tab.
   */
  private Future<Void> checkSameUserDifferentTab(
    String applicationType,
    String applicationId,
    Set<String> lockGroup,
    String ecasId,
    String sessionId
  ) {
    Instant now = Instant.now();
    JsonObject filter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(SECTION_ID, new JsonObject().put("$in", new JsonArray(new ArrayList<>(lockGroup))))
      .put(LOCK_OWNER_USER_ID, ecasId)
      .put(LOCK_OWNER_SESSION_ID, new JsonObject().put("$ne", sessionId))
      .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

    return mongoClient
      .findOne(APPLICATION_SECTION_LOCK_COLLECTION, filter, null)
      .compose(doc -> {
        if (doc != null) {
          return Future.failedFuture(
            new LockConflictException(
              SAME_USER_DIFFERENT_TAB_MSG,
              ecasId,
              doc.getString(SECTION_ID),
              readMongoDate(doc, EXPIRES_AT)
            )
          );
        }
        return Future.succeededFuture();
      });
  }

  /**
   * Acquires a lock on a single section using the two-phase approach.
   * Returns the lock document on success, fails with LockConflictException on conflict.
   */
  private Future<JsonObject> acquireSingleSection(
    String applicationType,
    String applicationId,
    String sectionId,
    String primarySectionId,
    String ecasId,
    String sessionId,
    Instant now,
    Instant expiresAt
  ) {
    JsonObject lockDoc = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(SECTION_ID, sectionId)
      .put(PRIMARY_SECTION_ID, primarySectionId)
      .put(LOCK_OWNER_USER_ID, ecasId)
      .put(LOCK_OWNER_SESSION_ID, sessionId)
      .put(ACQUIRED_AT, toMongoDate(now))
      .put(LAST_HEARTBEAT_AT, toMongoDate(now))
      .put(EXPIRES_AT, toMongoDate(expiresAt));

    // Phase 1: update existing doc if ALREADY owned by this same user+session
    // (re-entrant acquire / lease refresh).  We deliberately do NOT steal an
    // expired doc belonging to another session here: with per-session lock
    // rows, another session's expired doc is left for TTL cleanup and a fresh
    // row for this session is created in Phase 2 below.
    JsonObject canAcquireFilter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(SECTION_ID, sectionId)
      .put(LOCK_OWNER_USER_ID, ecasId)
      .put(LOCK_OWNER_SESSION_ID, sessionId);

    JsonObject updateDoc = new JsonObject().put("$set", lockDoc.copy());

    return mongoClient
      .updateCollection(APPLICATION_SECTION_LOCK_COLLECTION, canAcquireFilter, updateDoc)
      .compose(res -> {
        if (res.getDocModified() > 0 || res.getDocMatched() > 0) {
          log.info(
            "acquireLock (update): lock acquired for ecasId='{}' sectionId='{}'",
            ecasId,
            sectionId
          );
          return Future.succeededFuture(lockDoc);
        }
        // Phase 2: check for active lock, insert if absent
        return insertIfAbsent(
          applicationType,
          applicationId,
          sectionId,
          ecasId,
          sessionId,
          lockDoc,
          now
        );
      });
  }

  /**
   * Phase 2: checks whether another session holds a <strong>primary</strong> lock
   * on this section (a primary lock is a row where
   * {@code sectionId == primarySectionId}).  Dependent-lock rows held by other
   * sessions do NOT cause a conflict — they are advisory reservations only and
   * may coexist with this session's own dependent row on the same section.  If
   * no primary conflict exists, inserts a fresh lock document for this session.
   */
  private Future<JsonObject> insertIfAbsent(
    String applicationType,
    String applicationId,
    String sectionId,
    String ecasId,
    String sessionId,
    JsonObject lockDoc,
    Instant now
  ) {
    // Only another session's PRIMARY lock on this section conflicts.
    // A primary row is identified by: sectionId == primarySectionId.
    JsonObject activeLockFilter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(SECTION_ID, sectionId)
      .put(PRIMARY_SECTION_ID, sectionId)
      .put(LOCK_OWNER_SESSION_ID, new JsonObject().put("$ne", sessionId))
      .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

    // Opportunistically sweep any expired rows for this section before the
    // primary-conflict check + insert.  The (sectionId, sessionId) unique index
    // would otherwise keep a stale expired row in place until TTL cleanup.
    JsonObject expiredFilter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(SECTION_ID, sectionId)
      .put(EXPIRES_AT, new JsonObject().put("$lt", toMongoDate(now)));

    return mongoClient
      .removeDocuments(APPLICATION_SECTION_LOCK_COLLECTION, expiredFilter)
      .compose(sweep ->
        mongoClient.findOne(APPLICATION_SECTION_LOCK_COLLECTION, activeLockFilter, null)
      )
      .compose(existingDoc -> {
        if (existingDoc != null) {
          String ownerUserId = existingDoc.getString(LOCK_OWNER_USER_ID);
          log.info(
            "acquireLock: active lock by '{}' on sectionId='{}'; conflict",
            ownerUserId,
            sectionId
          );
          return Future.failedFuture(
            new LockConflictException(
              LOCK_CONFLICT_MSG,
              ownerUserId,
              sectionId,
              readMongoDate(existingDoc, EXPIRES_AT)
            )
          );
        }
        return mongoClient
          .insert(APPLICATION_SECTION_LOCK_COLLECTION, lockDoc.copy())
          .map(id -> {
            log.info(
              "acquireLock (insert): new lock for ecasId='{}' sectionId='{}'",
              ecasId,
              sectionId
            );
            return lockDoc;
          })
          .recover(err -> {
            if (err.getMessage() != null && err.getMessage().contains("E11000")) {
              log.info("acquireLock (insert race): duplicate key for sectionId='{}'", sectionId);
              return Future.failedFuture(
                new LockConflictException(LOCK_CONFLICT_MSG, "unknown", sectionId, null)
              );
            }
            return Future.failedFuture(err);
          });
      });
  }

  /**
   * Rolls back (deletes) all locks held by this session for the given sections.
   * Called when acquiring a lock group partially fails due to a conflict.
   */
  private Future<Void> rollbackSessionLocks(
    String applicationType,
    String applicationId,
    String sessionId,
    Set<String> lockGroup
  ) {
    JsonObject filter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(LOCK_OWNER_SESSION_ID, sessionId)
      .put(SECTION_ID, new JsonObject().put("$in", new JsonArray(new ArrayList<>(lockGroup))));

    return mongoClient
      .removeDocuments(APPLICATION_SECTION_LOCK_COLLECTION, filter)
      .map(res -> {
        log.info(
          "rollbackSessionLocks: removed {} lock(s) for sessionId='{}' applicationId='{}'",
          res.getRemovedCount(),
          sessionId,
          applicationId
        );
        return (Void) null;
      });
  }

  // ---------------------------------------------------------------------------
  // POST /api/applications/:type/:id/sections/:sectionId/lock/heartbeat
  // ---------------------------------------------------------------------------

  /**
   * Renews the expiry of ALL active section locks held by this session.
   *
   * <p>With dependent section locking, a session may hold multiple locks (the lock group).
   * This endpoint extends all locks for the session in one call.  The {@code sectionId}
   * path parameter is kept for backward API compatibility but is not used for filtering.
   *
   * <p>Request body: {@code { "sessionId": "<UUID>" }}
   *
   * @param context Vert.x routing context
   */
  public void heartbeatLock(@NotNull RoutingContext context) {
    String applicationType = context.pathParam("type");
    String applicationId   = context.pathParam("id");
    String ecasId          = Context.getEcasId(context);
    JsonObject body        = context.body().asJsonObject();

    if (body == null || StringUtils.isBlank(body.getString("sessionId"))) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Request body must contain 'sessionId'");
      return;
    }

    String sessionId = body.getString("sessionId");
    Instant now      = Instant.now();
    Instant newExpiry = now.plusSeconds(LOCK_HEARTBEAT_EXTENSION_SECONDS);

    // Filter: update ALL locks belonging to this user+session that are not yet expired
    JsonObject filter = new JsonObject()
        .put(APPLICATION_TYPE_FIELD, applicationType)
        .put(APPLICATION_ID, applicationId)
        .put(LOCK_OWNER_USER_ID, ecasId)
        .put(LOCK_OWNER_SESSION_ID, sessionId)
        .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

    JsonObject update = new JsonObject()
        .put("$set", new JsonObject()
            .put(LAST_HEARTBEAT_AT, toMongoDate(now))
            .put(EXPIRES_AT,        toMongoDate(newExpiry)));

    mongoClient
      .updateCollectionWithOptions(
        APPLICATION_SECTION_LOCK_COLLECTION,
        filter,
        update,
        new UpdateOptions().setMulti(true)
      )
      .onSuccess(res -> {
        if (res.getDocModified() > 0) {
          log.debug(
            "heartbeatLock: {} lock(s) extended for ecasId='{}' applicationId='{}'",
            res.getDocModified(),
            ecasId,
            applicationId
          );
          setUpContextResponse(context, HttpResponseStatus.OK,
            new JsonObject().put(EXPIRES_AT, newExpiry.toString()).encode());
        } else {
          // All locks expired or no longer belong to this user/session
          String sectionId = context.pathParam("sectionId");
          returnLockConflict(context, applicationType, applicationId, sectionId);
        }
      })
      .onFailure(err -> {
        log.error("heartbeatLock failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to send heartbeat");
      });
  }

  // ---------------------------------------------------------------------------
  // POST /api/applications/:type/:id/sections/:sectionId/lock/release
  // ---------------------------------------------------------------------------

  /**
   * Releases ALL section locks held by this session for the given application.
   *
   * <p>With dependent section locking, a session holds multiple locks (the lock group).
   * This endpoint releases all of them.  The {@code sectionId} path parameter is
   * kept for backward API compatibility but is not used for filtering.
   *
   * <p>Uses POST (not DELETE) so that the {@code sessionId} in the request body is
   * reliably transmitted through all HTTP proxies.
   *
   * <p>Request body: {@code { "sessionId": "<UUID>" }}
   *
   * @param context Vert.x routing context
   */
  public void releaseLock(@NotNull RoutingContext context) {
    String applicationType = context.pathParam("type");
    String applicationId   = context.pathParam("id");
    String ecasId          = Context.getEcasId(context);
    JsonObject body        = context.body().asJsonObject();

    // Accept sessionId from the JSON body (normal path) OR from the query parameter
    // (sendBeacon fallback — browsers fire-and-forget on tab/window close and the
    // body Content-Type is sometimes mangled so Vert.x returns null from asJsonObject()).
    String sessionId = null;
    if (body != null) {
      sessionId = body.getString("sessionId");
    }
    if (StringUtils.isBlank(sessionId)) {
      sessionId = context.queryParam("sessionId").stream().findFirst().orElse(null);
    }

    if (StringUtils.isBlank(sessionId)) {
      setUpMessageContextResponse(context, HttpResponseStatus.BAD_REQUEST,
        "Request body or query parameter must contain 'sessionId'");
      return;
    }

    // Find all locks for this session first (to broadcast releases)
    final String finalSessionId = sessionId;
    JsonObject filter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(LOCK_OWNER_USER_ID, ecasId)
      .put(LOCK_OWNER_SESSION_ID, finalSessionId);

    mongoClient
      .find(APPLICATION_SECTION_LOCK_COLLECTION, filter)
      .compose(docs -> {
        // Broadcast lock_released for each locked section
        for (JsonObject doc : docs) {
          wsManager.broadcastLockReleased(
            applicationType,
            applicationId,
            doc.getString(SECTION_ID),
            finalSessionId
          );
        }
        return mongoClient.removeDocuments(APPLICATION_SECTION_LOCK_COLLECTION, filter);
      })
      .onSuccess(res -> {
        log.info(
          "releaseLock: released {} lock(s) for ecasId='{}' applicationId='{}'",
          res.getRemovedCount(),
          ecasId,
          applicationId
        );
        setUpContextResponse(context, HttpResponseStatus.NO_CONTENT);
      })
      .onFailure(err -> {
        log.error("releaseLock failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to release lock");
      });
  }

  // ---------------------------------------------------------------------------
  // GET /api/applications/:type/:id/sections/locks
  // ---------------------------------------------------------------------------

  /**
   * Returns all active (non-expired) locks for a given application.
   *
   * <p>The frontend uses this for the polling fallback (every 60-120 seconds)
   * to synchronise lock state even if a WebSocket event was missed.
   *
   * <p>Only locks where {@code expiresAt > now} are returned.
   *
   * @param context Vert.x routing context; path params: {@code type}, {@code id}
   */
  public void getApplicationLocks(@NotNull RoutingContext context) {
    String applicationType = context.pathParam("type");
    String applicationId   = context.pathParam("id");
    Instant now            = Instant.now();

    JsonObject filter = new JsonObject()
        .put(APPLICATION_TYPE_FIELD, applicationType)
        .put(APPLICATION_ID, applicationId)
        // Return only locks that have not yet expired
        .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

    mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION, filter)
      .onSuccess(locks -> {
        // Convert BSON Date fields to ISO strings for the frontend
        JsonArray result = new JsonArray();
        for (JsonObject doc : locks) {
          result.add(normalizeLockDoc(doc));
        }
        setUpContextResponse(context, HttpResponseStatus.OK, result.encode());
      })
      .onFailure(err -> {
        log.error("getApplicationLocks failed: {}", err.getMessage(), err);
        setUpMessageContextResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to retrieve application locks");
      });
  }

  // ---------------------------------------------------------------------------
  // POST /api/applications/:type/:id/lock/switch
  // ---------------------------------------------------------------------------

  /**
   * Atomically switches the lock group when the user navigates to a different section.
   *
   * <p>Computes the new lock group, diffs it against the current session's locks,
   * then releases sections no longer needed, keeps overlapping sections (refreshing
    * their expiry), and acquires sections newly required. If any newly required
    * section is held by another user, the current session's existing locks are
    * released because the caller is navigating away from the previously edited
    * section and should not continue blocking collaborators there.
   *
   * <p>Request body: {@code { "newSectionId": SECTION_4, "sessionId": "<UUID>" }}
   *
   * @param context Vert.x routing context; path params: {@code type}, {@code id}
   */
  public void switchLock(@NotNull RoutingContext context) {
    String applicationType = context.pathParam("type");
    String applicationId = context.pathParam("id");
    String ecasId = Context.getEcasId(context);
    JsonObject body = context.body().asJsonObject();

    if (
      body == null ||
      StringUtils.isBlank(body.getString("sessionId")) ||
      StringUtils.isBlank(body.getString("newSectionId"))
    ) {
      setUpMessageContextResponse(
        context,
        HttpResponseStatus.BAD_REQUEST,
        "Request body must contain 'sessionId' and 'newSectionId'"
      );
      return;
    }

    String sessionId = body.getString("sessionId");
    String newSectionId = body.getString("newSectionId");

    if (!isValidSectionId(newSectionId)) {
      setUpMessageContextResponse(
        context,
        HttpResponseStatus.BAD_REQUEST,
        "Invalid newSectionId. Must be section1 through section10."
      );
      return;
    }

    Set<String> newGroup = computeLockGroup(applicationType, newSectionId);
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(LOCK_LEASE_DURATION_SECONDS);

    // Step 1: fetch current locks held by this session
    JsonObject sessionFilter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(LOCK_OWNER_SESSION_ID, sessionId);

    mongoClient
      .find(APPLICATION_SECTION_LOCK_COLLECTION, sessionFilter)
      .compose(currentDocs -> {
        Set<String> currentSections = currentDocs
          .stream()
          .map(d -> d.getString(SECTION_ID))
          .collect(Collectors.toSet());

        Set<String> toRelease = new HashSet<>(currentSections);
        toRelease.removeAll(newGroup);

        Set<String> toKeep = new HashSet<>(currentSections);
        toKeep.retainAll(newGroup);

        Set<String> toAcquire = new HashSet<>(newGroup);
        toAcquire.removeAll(currentSections);

        log.info(
          "switchLock: newSection='{}' toRelease={} toKeep={} toAcquire={}",
          newSectionId,
          toRelease,
          toKeep,
          toAcquire
        );

        // Step 2: check that all toAcquire sections are available
        return checkSectionsAvailable(
          applicationType,
          applicationId,
          toAcquire,
          ecasId,
          sessionId,
          now
        )
          .recover(err -> {
            if (err instanceof LockConflictException conflictErr) {
              return releaseSectionsAndBroadcast(
                applicationType,
                applicationId,
                sessionFilter,
                currentSections
              ).compose(v ->
                Future.failedFuture(new SwitchLockConflictException(conflictErr, currentSections))
              );
            }
            return Future.failedFuture(err);
          })
          .compose(v -> {
            // Step 3: release stale sections
            Future<Void> releaseFut = toRelease.isEmpty()
              ? Future.succeededFuture()
              : releaseSections(applicationType, applicationId, sessionFilter, toRelease);

            return releaseFut.compose(v2 -> {
              // Step 4: refresh kept sections
              Future<Void> refreshFut = toKeep.isEmpty()
                ? Future.succeededFuture()
                : refreshSections(
                    applicationType,
                    applicationId,
                    sessionId,
                    toKeep,
                    newSectionId,
                    now,
                    expiresAt
                  );

              return refreshFut.compose(v3 -> {
                // Step 5: acquire new sections
                if (toAcquire.isEmpty()) {
                  return Future.succeededFuture(new ArrayList<JsonObject>());
                }
                Future<List<JsonObject>> acquireChain = Future.succeededFuture(new ArrayList<>());
                for (String sid : toAcquire) {
                  acquireChain = acquireChain.compose(docs ->
                    acquireSingleSection(
                      applicationType,
                      applicationId,
                      sid,
                      newSectionId,
                      ecasId,
                      sessionId,
                      now,
                      expiresAt
                    ).map(doc -> {
                      docs.add(doc);
                      return docs;
                    })
                  );
                }
                return acquireChain;
              });
            });
          })
          .map(newDocs -> {
            // Build response
            JsonArray lockedSections = new JsonArray();
            JsonArray releasedSections = new JsonArray();
            for (String sid : toRelease) releasedSections.add(sid);

            // Re-fetch all current locks for response
            for (String sid : toKeep) {
              lockedSections.add(
                new JsonObject()
                  .put(SECTION_ID, sid)
                  .put(PRIMARY_SECTION_ID, newSectionId)
                  .put(LOCK_OWNER_USER_ID, ecasId)
                  .put(EXPIRES_AT, expiresAt.toString())
              );
            }
            for (JsonObject doc : newDocs) {
              lockedSections.add(normalizeLockDoc(doc));
            }

            // Broadcast releases
            for (String sid : toRelease) {
              wsManager.broadcastLockReleased(applicationType, applicationId, sid, sessionId);
            }
            // Broadcast acquires (newly acquired sections)
            for (JsonObject doc : newDocs) {
              wsManager.broadcastLockAcquired(
                applicationType,
                applicationId,
                doc.getString(SECTION_ID),
                ecasId,
                sessionId,
                expiresAt.toString(),
                newSectionId
              );
            }
            // Broadcast acquires for kept sections (primarySectionId may have changed)
            for (String sid : toKeep) {
              wsManager.broadcastLockAcquired(
                applicationType,
                applicationId,
                sid,
                ecasId,
                sessionId,
                expiresAt.toString(),
                newSectionId
              );
            }

            return new JsonObject()
              .put("locked", true)
              .put(PRIMARY_SECTION_ID, newSectionId)
              .put(LOCK_OWNER_USER_ID, ecasId)
              .put(EXPIRES_AT, expiresAt.toString())
              .put("lockedSections", lockedSections)
              .put("releasedSections", releasedSections);
          });
      })
      .onSuccess(result -> setUpContextResponse(context, HttpResponseStatus.OK, result.encode()))
      .onFailure(err -> {
        if (err instanceof SwitchLockConflictException conflictErr) {
          JsonArray releasedSections = new JsonArray();
          for (String sid : conflictErr.releasedSections) {
            releasedSections.add(sid);
          }
          JsonObject conflictBody = new JsonObject()
            .put("locked", false)
            .put("reason", conflictErr.getMessage())
            .put(LOCK_OWNER_USER_ID, conflictErr.lockOwnerUserId)
            .put(PRIMARY_SECTION_ID, conflictErr.conflictSectionId)
            .put(EXPIRES_AT, conflictErr.expiresAt)
            .put("releasedSections", releasedSections);
          setUpContextResponse(context, HttpResponseStatus.CONFLICT, conflictBody.encode());
        } else if (err instanceof LockConflictException conflictErr) {
          JsonObject conflictBody = new JsonObject()
            .put("locked", false)
            .put("reason", conflictErr.getMessage())
            .put(LOCK_OWNER_USER_ID, conflictErr.lockOwnerUserId)
            .put(PRIMARY_SECTION_ID, conflictErr.conflictSectionId)
            .put(EXPIRES_AT, conflictErr.expiresAt);
          setUpContextResponse(context, HttpResponseStatus.CONFLICT, conflictBody.encode());
        } else {
          log.error("switchLock failed: {}", err.getMessage(), err);
          setUpMessageContextResponse(
            context,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Failed to switch lock"
          );
        }
      });
  }

  /**
   * Checks that no <strong>primary</strong> lock by another session exists on any
   * section in {@code toAcquire}.  Dependent-lock rows held by other sessions are
   * ignored (they are advisory reservations and may coexist with this session's
   * own dependent rows).  Fails with {@link LockConflictException} on the first
   * primary conflict found.
   */
  private Future<Void> checkSectionsAvailable(
    String applicationType,
    String applicationId,
    Set<String> toAcquire,
    String ecasId,
    String sessionId,
    Instant now
  ) {
    if (toAcquire.isEmpty()) return Future.succeededFuture();

    // A primary lock row has sectionId == primarySectionId.  Using $expr so the
    // comparison is between two fields of the same document.
    JsonObject filter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(SECTION_ID, new JsonObject().put("$in", new JsonArray(new ArrayList<>(toAcquire))))
      .put(LOCK_OWNER_SESSION_ID, new JsonObject().put("$ne", sessionId))
      .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)))
      .put(
        "$expr",
        new JsonObject().put(
          "$eq",
          new JsonArray().add("$" + SECTION_ID).add("$" + PRIMARY_SECTION_ID)
        )
      );

    return mongoClient
      .findOne(APPLICATION_SECTION_LOCK_COLLECTION, filter, null)
      .compose(doc -> {
        if (doc != null) {
          return Future.failedFuture(
            new LockConflictException(
              LOCK_CONFLICT_MSG,
              doc.getString(LOCK_OWNER_USER_ID),
              doc.getString(SECTION_ID),
              readMongoDate(doc, EXPIRES_AT)
            )
          );
        }
        return Future.succeededFuture();
      });
  }

  /**
   * Releases (deletes) specific sections from the session's locks and broadcasts events.
   */
  private Future<Void> releaseSections(
    String applicationType,
    String applicationId,
    JsonObject sessionFilter,
    Set<String> sections
  ) {
    JsonObject filter = sessionFilter
      .copy()
      .put(SECTION_ID, new JsonObject().put("$in", new JsonArray(new ArrayList<>(sections))));
    return mongoClient
      .removeDocuments(APPLICATION_SECTION_LOCK_COLLECTION, filter)
      .map(res -> (Void) null);
  }

  /**
   * Releases specific sections from the session's locks and broadcasts release events.
   */
  private Future<Void> releaseSectionsAndBroadcast(
    String applicationType,
    String applicationId,
    JsonObject sessionFilter,
    Set<String> sections
  ) {
    if (sections.isEmpty()) {
      return Future.succeededFuture();
    }
    String sessionId = sessionFilter.getString(LOCK_OWNER_SESSION_ID);
    for (String sid : sections) {
      wsManager.broadcastLockReleased(applicationType, applicationId, sid, sessionId);
    }
    return releaseSections(applicationType, applicationId, sessionFilter, sections);
  }

  /**
   * Refreshes the expiry of sections that are kept across a lock switch.
   */
  private Future<Void> refreshSections(
    String applicationType,
    String applicationId,
    String sessionId,
    Set<String> sections,
    String primarySectionId,
    Instant now,
    Instant expiresAt
  ) {
    JsonObject filter = new JsonObject()
      .put(APPLICATION_TYPE_FIELD, applicationType)
      .put(APPLICATION_ID, applicationId)
      .put(LOCK_OWNER_SESSION_ID, sessionId)
      .put(SECTION_ID, new JsonObject().put("$in", new JsonArray(new ArrayList<>(sections))));

    JsonObject update = new JsonObject().put(
      "$set",
      new JsonObject()
        .put(PRIMARY_SECTION_ID, primarySectionId)
        .put(LAST_HEARTBEAT_AT, toMongoDate(now))
        .put(EXPIRES_AT, toMongoDate(expiresAt))
    );

    return mongoClient
      .updateCollectionWithOptions(
        APPLICATION_SECTION_LOCK_COLLECTION,
        filter,
        update,
        new io.vertx.ext.mongo.UpdateOptions().setMulti(true)
      )
      .map(res -> (Void) null);
  }

  // ---------------------------------------------------------------------------
  // WS-disconnect release (called from MainVerticle when a WebSocket closes)
  // ---------------------------------------------------------------------------

  /**
   * Releases ALL active section locks held by a specific browser-tab session
   * for a given application.
   *
   * <p>This is called by {@link eu.europa.data.health.access.MainVerticle} from the
   * WebSocket {@code closeHandler} and {@code exceptionHandler} when a client
   * connection is lost.  It ensures that locks are always released when the browser
   * is force-closed, even when the {@code sendBeacon} / {@code pagehide} HTTP request
   * from the frontend is suppressed or never arrives.
   *
   * <p>A single browser tab may hold multiple locks (the lock group), so this call
   * may remove several documents.
   *
   * @param applicationType the application type (e.g. "DATA_ACCESS_APPLICATION")
   * @param applicationId   the application document {@code _id}
   * @param sessionId       the browser-tab session UUID (from the WebSocket query param)
   */
  public void releaseLocksForSession(String applicationType, String applicationId,
      String sessionId) {
    if (StringUtils.isBlank(sessionId)) {
      log.debug("releaseLocksForSession: no sessionId — skipping (connection had no session)");
      return;
    }

    JsonObject filter = new JsonObject()
        .put(APPLICATION_TYPE_FIELD, applicationType)
        .put(APPLICATION_ID, applicationId)
        .put(LOCK_OWNER_SESSION_ID, sessionId);

    mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION, filter)
      .compose(docs -> {
        if (docs == null || docs.isEmpty()) {
          log.debug("releaseLocksForSession: no locks found for sessionId='{}' applicationId='{}'",
            sessionId, applicationId);
          return Future.succeededFuture(docs);
        }
        // Broadcast lock_released for each lock we are about to delete
        for (JsonObject doc : docs) {
          String sectionId = doc.getString(SECTION_ID);
          String ecasId    = doc.getString(LOCK_OWNER_USER_ID);
          log.info("releaseLocksForSession: releasing lock for ecasId='{}' sectionId='{}' " +
              "applicationId='{}' (WS disconnect)", ecasId, sectionId, applicationId);
          wsManager.broadcastLockReleased(applicationType, applicationId, sectionId, sessionId);
        }
        return Future.succeededFuture(docs);
      })
      .compose(docs -> mongoClient.removeDocuments(APPLICATION_SECTION_LOCK_COLLECTION, filter))
      .onSuccess(res -> log.info(
          "releaseLocksForSession: removed {} lock(s) for sessionId='{}' applicationId='{}'",
          res.getRemovedCount(), sessionId, applicationId))
      .onFailure(err -> log.error(
          "releaseLocksForSession: failed for sessionId='{}' applicationId='{}': {}",
          sessionId, applicationId, err.getMessage(), err));
  }

  // ---------------------------------------------------------------------------
  // Write-time enforcement (called from DataAccessApplicationManager /
  // DataRequestApplicationManager before persisting section data)
  // ---------------------------------------------------------------------------

  /**
   * Asserts that the caller holds a valid, non-expired lock for the given section.
   *
   * <p>This method MUST be called by the section-save handlers
   * ({@code putSectionApplication} and {@code putSectionDataRequestApplication})
   * before persisting section data.  If the caller does not hold the lock, the
   * returned {@code Future} fails and the handler should respond with HTTP 409.
   *
   * <h3>Why this is necessary</h3>
   * <p>The UI enforces locking through the frontend, but a determined client could
   * bypass the frontend and call the save endpoint directly.  Backend enforcement
   * prevents this.
   *
   * @param applicationType the application type string
   * @param applicationId   the application document {@code _id}
   * @param sectionId       the section identifier (e.g. SECTION_3)
   * @param sessionId       the caller's session ID (from request header
   *                        {@code X-Session-Id} or request body)
   * @return a succeeded {@code Future<Void>} if the lock is held, or a failed
   *         {@code Future} with a human-readable error message if not
   */
  public Future<Void> assertLockHeld(String applicationType, String applicationId,
      String sectionId, String sessionId) {
    // sessionId is optional for backward-compatibility (e.g., admin saves)
    if (StringUtils.isBlank(sessionId)) {
      log.debug("assertLockHeld: sessionId absent — skipping lock check (backward-compat mode)");
      return Future.succeededFuture();
    }

    Instant now = Instant.now();

    JsonObject filter = new JsonObject()
        .put(APPLICATION_TYPE_FIELD, applicationType)
        .put(APPLICATION_ID, applicationId)
        .put(SECTION_ID, sectionId)
        .put(LOCK_OWNER_SESSION_ID, sessionId)
        .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

    return mongoClient.findOne(APPLICATION_SECTION_LOCK_COLLECTION, filter, null)
      .compose(doc -> {
        if (doc != null) {
          return Future.succeededFuture();
        }
        // Fetch lock owner info to include in the error message
        JsonObject anyLockFilter = new JsonObject()
            .put(APPLICATION_TYPE_FIELD, applicationType)
            .put(APPLICATION_ID, applicationId)
            .put(SECTION_ID, sectionId)
            .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

        return mongoClient.findOne(APPLICATION_SECTION_LOCK_COLLECTION, anyLockFilter, null)
          .compose(lockDoc -> {
            String reason = lockDoc != null
                ? "locked by " + lockDoc.getString(LOCK_OWNER_USER_ID)
                :   WRITE_REJECTED_NO_LOCK_MSG;
            return Future.failedFuture(reason);
          });
      });
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  /**
   * Fetches the current active lock for a section and returns a 409 response with
   * lock owner information.  Called when an acquisition or heartbeat fails.
   */
  private void returnLockConflict(RoutingContext context, String applicationType,
      String applicationId, String sectionId) {
    Instant now = Instant.now();

    JsonObject filter = new JsonObject()
        .put(APPLICATION_TYPE_FIELD, applicationType)
        .put(APPLICATION_ID, applicationId)
        .put(SECTION_ID, sectionId)
        .put(EXPIRES_AT, new JsonObject().put("$gt", toMongoDate(now)));

    mongoClient.findOne(APPLICATION_SECTION_LOCK_COLLECTION, filter, null)
      .onSuccess(lockDoc -> {
        JsonObject body = new JsonObject().put("locked", false);
        if (lockDoc != null) {
          body.put("reason", LOCK_CONFLICT_MSG)
              .put(LOCK_OWNER_USER_ID, lockDoc.getString(LOCK_OWNER_USER_ID))
              .put(EXPIRES_AT, readMongoDate(lockDoc, EXPIRES_AT));
        } else {
          body.put("reason", "Lock was lost or expired.");
        }
        setUpContextResponse(context, HttpResponseStatus.CONFLICT, body.encode());
      })
      .onFailure(err -> setUpMessageContextResponse(context,
        HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve lock info"));
  }

  /**
   * Converts an {@link Instant} to a plain ISO-8601 string for use in MongoDB
   * query filters and update documents via Vert.x's MongoDB client.
   *
   * <h3>Why plain strings instead of BSON {@code {"$date": ...}}?</h3>
   * <p>Vert.x 4's {@code JsonObjectCodec.writeDateTime} is invoked whenever it
   * encounters a {@code {"$date": value}} wrapper in a {@code JsonObject} being
   * encoded for a MongoDB operation.  Depending on the Vert.x/driver version,
   * it may call {@code OffsetDateTime.parse(value.toString())}, which:
   * <ul>
   *   <li>fails if {@code value} is a {@code Long} (epoch millis) — the
   *       {@code DateTimeParseException: Text '1772503365954' could not be parsed}
   *       error seen in production.</li>
   *   <li>may also fail for ISO strings ending in {@code Z} in some JDK builds.</li>
   * </ul>
   * <p>Storing dates as <strong>plain ISO-8601 strings</strong> sidesteps the codec
   * entirely: Vert.x serialises them as ordinary strings and MongoDB's
   * {@code $gt}/{@code $lt} operators compare ISO strings correctly because the
   * format is lexicographically monotonic.
   *
   * <p>The TTL index on {@code expiresAt} must therefore be created as a
   * <em>string-type</em> field index; alternatively, the application-level expiry
   * check (expiresAt < now) is the primary mechanism and TTL cleanup is secondary.
   *
   * @param instant the point in time to encode
   * @return ISO-8601 string, e.g. {@code "2026-03-03T10:15:30Z"}
   */
  private static String toMongoDate(Instant instant) {
    return instant.toString();  // e.g. "2026-03-03T10:15:30Z" — safe for Vert.x codec
  }

  /**
   * Reads a date field from a MongoDB document returned by Vert.x and returns
   * its value as an ISO-8601 string suitable for JSON API responses.
   *
   * <p>Handles all forms a date value may arrive in from Vert.x:
   * <ul>
   *   <li>Plain {@code String} — stored by this code as ISO-8601 (current format).</li>
   *   <li>{@code {"$date": Long}} — legacy BSON Date as epoch millis (old documents).</li>
   *   <li>{@code {"$date": String}} — BSON Date encoded as ISO string (transition).</li>
   *   <li>Bare {@code Long}/{@code Integer} — should not occur but handled defensively.</li>
   * </ul>
   *
   * @param doc       the MongoDB document
   * @param fieldName the field name to read
   * @return ISO-8601 string representation, or {@code null} if absent
   */
  private static String readMongoDate(JsonObject doc, String fieldName) {
    if (doc == null || !doc.containsKey(fieldName)) return null;
    Object value = doc.getValue(fieldName);
    // Plain string — current format (stored by toMongoDate as ISO-8601 string)
    if (value instanceof String) return (String) value;
    // BSON extended-JSON wrapper {"$date": ...} — from legacy documents
    if (value instanceof JsonObject) {
      Object inner = ((JsonObject) value).getValue("$date");
      if (inner instanceof Long)    return Instant.ofEpochMilli((Long) inner).toString();
      if (inner instanceof Integer) return Instant.ofEpochMilli(((Integer) inner).longValue()).toString();
      if (inner instanceof String)  return (String) inner;
      return null;
    }
    // Bare numeric — defensive fallback
    if (value instanceof Long)    return Instant.ofEpochMilli((Long) value).toString();
    if (value instanceof Integer) return Instant.ofEpochMilli(((Integer) value).longValue()).toString();
    return value.toString();
  }

  /**
   * Normalises a raw MongoDB lock document for JSON API consumption by
   * converting all BSON Date fields to ISO-8601 strings.
   *
   * @param doc raw document from {@code mongoClient.find()}
   * @return a new {@link JsonObject} safe to serialise as API response JSON
   */
  private static JsonObject normalizeLockDoc(JsonObject doc) {
    JsonObject out = doc.copy();
    for (String field : new String[]{EXPIRES_AT, ACQUIRED_AT, LAST_HEARTBEAT_AT}) {
      String iso = readMongoDate(doc, field);
      if (iso != null) out.put(field, iso);
    }
    return out;
  }

  /**
   * Validates that the given sectionId is one of the allowed values (section1 … section10).
   *
   * @param sectionId the section identifier to validate
   * @return {@code true} if valid
   */
  private boolean isValidSectionId(String sectionId) {
    if (StringUtils.isBlank(sectionId)) return false;
    return sectionId.matches("^section([1-9]|10)$");
  }

  // ---------------------------------------------------------------------------
  // Lock conflict exception (used for internal control flow in group acquisition)
  // ---------------------------------------------------------------------------

  /**
   * Typed exception carrying structured conflict info so that callers can build
   * a proper 409 response body without re-querying MongoDB.
   */
  static class LockConflictException extends RuntimeException {

    final String lockOwnerUserId;
    final String conflictSectionId;
    final String expiresAt;

    LockConflictException(
      String message,
      String lockOwnerUserId,
      String conflictSectionId,
      String expiresAt
    ) {
      super(message);
      this.lockOwnerUserId = lockOwnerUserId;
      this.conflictSectionId = conflictSectionId;
      this.expiresAt = expiresAt;
    }
  }

  /**
   * Conflict raised during switchLock after the current session's old lock group
   * has already been released.
   */
  static final class SwitchLockConflictException extends LockConflictException {

    final Set<String> releasedSections;

    SwitchLockConflictException(LockConflictException cause, Set<String> releasedSections) {
      super(cause.getMessage(), cause.lockOwnerUserId, cause.conflictSectionId, cause.expiresAt);
      this.releasedSections = Set.copyOf(releasedSections);
    }
  }
}
