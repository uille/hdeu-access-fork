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

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages WebSocket connections for real-time section lock event broadcasting.
 *
 * <h2>Purpose</h2>
 * <p>When a user acquires or releases a section lock, all other users currently
 * viewing the same application need to know immediately so that the UI can update
 * lock icons without waiting for a polling cycle.  This class maintains an in-memory
 * registry of open WebSocket connections keyed by
 * {@code applicationType:applicationId} and broadcasts events to all connected
 * clients in that group.
 *
 * <h2>Connection lifecycle</h2>
 * <ol>
 *   <li>Client opens {@code ws://.../api/ws/applications/:type/:id/locks}.</li>
 *   <li>{@link #register(String, String, ServerWebSocket)} is called — the socket
 *       is added to the group map.</li>
 *   <li>When the socket closes (normally or abnormally),
 *       {@link #unregister(String, String, ServerWebSocket)} removes it.</li>
 *   <li>{@link #broadcast(String, String, JsonObject)} sends a message to every
 *       socket in the group (skipping closed sockets).</li>
 * </ol>
 *
 * <h2>Thread-safety</h2>
 * <p>Vert.x is single-threaded per event-loop thread; however the ConcurrentHashMap
 * here is used defensively in case multiple Vert.x verticle instances are deployed.
 *
 * <h2>Message format</h2>
 * <p>Every broadcast message is a JSON object:
 * <pre>
 * {
 *   "event":            "lock_acquired" | "lock_released",
 *   "applicationType":  "DATA_ACCESS_APPLICATION",
 *   "applicationId":    "...",
 *   "sectionId":        "section3",
 *   "lockOwnerUserId":  "john.doe",
 *   "expiresAt":        "2026-03-03T12:30:00Z"   // only on lock_acquired
 * }
 * </pre>
 */
public class LockWebSocketManager {

  private static final Logger log = LoggerFactory.getLogger(LockWebSocketManager.class);

  /**
   * WebSocket event type fired when a lock is successfully acquired.
   * Frontend uses this to immediately mark a section as locked.
   */
  public static final String EVENT_LOCK_ACQUIRED = "lock_acquired";

  /**
   * WebSocket event type fired when a lock is released (explicit release or
   * expiry detection at the moment of an acquire attempt).
   * Frontend uses this to immediately clear the lock icon.
   */
  public static final String EVENT_LOCK_RELEASED = "lock_released";

  /**
   * WebSocket event type fired when a section is successfully saved.
   * Frontend uses this to refresh the application data from REST and merge
   * non-locked sections into the in-memory state.
   */
  public static final String EVENT_SECTION_SAVED = "section_saved";

  /**
   * WebSocket event type sent to a newly connected client to confirm the
   * connection is alive and the client can start receiving events.
   */
  public static final String EVENT_CONNECTED = "connected";

  /**
   * Map from {@code "applicationType:applicationId"} to the set of active
   * WebSocket connections subscribed to lock events for that application.
   */
  private final Map<String, Set<ServerWebSocket>> connections = new ConcurrentHashMap<>();

  /**
   * Builds the group key used as the map key from applicationType and applicationId.
   *
   * @param applicationType the application type constant (e.g. "DATA_ACCESS_APPLICATION")
   * @param applicationId   the application document {@code _id}
   * @return composite group key string
   */
  private String groupKey(String applicationType, String applicationId) {
    return applicationType + ":" + applicationId;
  }

  /**
   * Registers a new WebSocket connection to receive lock events for the given application.
   *
   * <p>Also sends a {@code connected} event back to the client so it knows the subscription
   * is active and can display an "online" indicator if desired.
   *
   * @param applicationType the application type constant
   * @param applicationId   the application document {@code _id}
   * @param ws              the newly opened WebSocket connection
   */
  public void register(String applicationType, String applicationId, ServerWebSocket ws) {
    String key = groupKey(applicationType, applicationId);
    connections.computeIfAbsent(key, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
               .add(ws);
    log.info("LockWS: client registered for app '{}:{}' — total connections in group: {}",
      applicationType, applicationId, connections.get(key).size());

    // Confirm subscription to the client
    try {
      ws.writeTextMessage(new JsonObject()
          .put("event", EVENT_CONNECTED)
          .put("applicationType", applicationType)
          .put("applicationId", applicationId)
          .encode());
    } catch (Exception e) {
      log.warn("LockWS: failed to send 'connected' ack to client: {}", e.getMessage());
    }
  }

  /**
   * Removes a WebSocket connection from the group registry.
   *
   * <p>Called when the socket closes (either by the client or due to network error).
   * If the group becomes empty after removal, the group entry is cleaned up to avoid
   * memory leaks.
   *
   * @param applicationType the application type constant
   * @param applicationId   the application document {@code _id}
   * @param ws              the WebSocket connection to remove
   */
  public void unregister(String applicationType, String applicationId, ServerWebSocket ws) {
    String key = groupKey(applicationType, applicationId);
    Set<ServerWebSocket> group = connections.get(key);
    if (group != null) {
      group.remove(ws);
      log.info("LockWS: client unregistered from app '{}:{}' — remaining: {}",
        applicationType, applicationId, group.size());
      if (group.isEmpty()) {
        connections.remove(key);
        log.debug("LockWS: empty group '{}' removed from registry", key);
      }
    }
  }

  /**
   * Broadcasts a {@code lock_acquired} event to all subscribers for the given application.
   *
   * @param applicationType     the application type constant
   * @param applicationId       the application document {@code _id}
   * @param sectionId           the section that was locked (e.g. "section3")
   * @param lockOwnerUserId     the ECAS ID of the user who acquired the lock
   * @param lockOwnerSessionId  the per-tab session UUID of the acquirer (used by
   *                            subscribers to suppress own-session echo events)
   * @param expiresAt           ISO-8601 string when the lock expires
   * @param primarySectionId    the primary section that triggered the lock group
   */
  public void broadcastLockAcquired(
    String applicationType,
    String applicationId,
    String sectionId,
    String lockOwnerUserId,
    String lockOwnerSessionId,
    String expiresAt,
    String primarySectionId
  ) {
    JsonObject message = new JsonObject()
      .put("event", EVENT_LOCK_ACQUIRED)
      .put("applicationType", applicationType)
      .put("applicationId", applicationId)
      .put("sectionId", sectionId)
      .put("lockOwnerUserId", lockOwnerUserId)
      .put("lockOwnerSessionId", lockOwnerSessionId)
      .put("expiresAt", expiresAt)
      .put("primarySectionId", primarySectionId);
    broadcast(applicationType, applicationId, message);
  }

  /**
   * Broadcasts a {@code lock_released} event to all subscribers for the given application.
   *
   * @param applicationType     the application type constant
   * @param applicationId       the application document {@code _id}
   * @param sectionId           the section whose lock was released (e.g. "section3")
   * @param lockOwnerSessionId  the per-tab session UUID of the releaser (used by
   *                            subscribers to suppress own-session echo events and
   *                            to avoid clearing a section still locked by another
   *                            session through a dependent lock)
   */
  public void broadcastLockReleased(
    String applicationType,
    String applicationId,
    String sectionId,
    String lockOwnerSessionId
  ) {
    JsonObject message = new JsonObject()
      .put("event", EVENT_LOCK_RELEASED)
      .put("applicationType", applicationType)
      .put("applicationId", applicationId)
      .put("sectionId", sectionId)
      .put("lockOwnerSessionId", lockOwnerSessionId);
    broadcast(applicationType, applicationId, message);
  }

  /**
   * Broadcasts a {@code section_saved} event to all subscribers for the given application.
   *
   * <p>Sent after a section is successfully persisted to MongoDB. Receiving clients
   * fetch fresh application data via REST and merge it into their in-memory state,
   * skipping sections they currently hold a lock on (to preserve unsaved edits).
   *
   * @param applicationType   the application type constant
   * @param applicationId     the application document {@code _id}
   * @param sectionId         the section that was saved (e.g. "section3")
   * @param savedByUserId     the ECAS ID of the user who saved the section
   * @param savedBySessionId  the per-tab session UUID (may be null for admin/legacy saves)
   */
  public void broadcastSectionSaved(String applicationType, String applicationId,
      String sectionId, String savedByUserId, String savedBySessionId) {
    JsonObject message = new JsonObject()
        .put("event", EVENT_SECTION_SAVED)
        .put("applicationType", applicationType)
        .put("applicationId", applicationId)
        .put("sectionId", sectionId)
        .put("savedByUserId", savedByUserId);
    if (savedBySessionId != null) {
      message.put("savedBySessionId", savedBySessionId);
    }
    message.put("timestamp", java.time.Instant.now().toString());
    broadcast(applicationType, applicationId, message);
  }

  /**
   * Broadcasts a raw JSON message to all WebSocket clients subscribed to
   * the given application. Any closed sockets are silently skipped.
   *
   * @param applicationType the application type constant
   * @param applicationId   the application document {@code _id}
   * @param message         the JSON message payload to broadcast
   */
  public void broadcast(String applicationType, String applicationId, JsonObject message) {
    String key = groupKey(applicationType, applicationId);
    Set<ServerWebSocket> group = connections.get(key);
    if (group == null || group.isEmpty()) {
      log.debug("LockWS: broadcast to '{}' — no subscribers, skipping", key);
      return;
    }

    String payload = message.encode();
    int sent = 0;
    for (ServerWebSocket ws : group) {
      if (!ws.isClosed()) {
        try {
          ws.writeTextMessage(payload);
          sent++;
        } catch (Exception e) {
          log.warn("LockWS: failed to send to a subscriber in group '{}': {}", key, e.getMessage());
        }
      }
    }
    log.debug("LockWS: broadcast to '{}' — event='{}' sent to {} subscriber(s)",
      key, message.getString("event"), sent);
  }

}
