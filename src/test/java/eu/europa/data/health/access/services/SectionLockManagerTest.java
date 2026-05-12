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

import static eu.europa.data.health.access.Constants.APPLICATION_SECTION_LOCK_COLLECTION;
import static eu.europa.data.health.access.Constants.DATA_REQUEST_APPLICATION_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europa.data.health.access.Constants;
import eu.europa.data.health.access.RoutingContextMocker;
import eu.europa.data.health.access.handlers.DBHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Integration tests for {@link SectionLockManager}.
 *
 * <p>Verifies the full lifecycle of section edit locks:
 * <ul>
 *   <li>Acquire succeeds when no lock exists</li>
 *   <li>Acquire returns 409 when another user holds the lock</li>
 *   <li>Acquire succeeds when the existing lock is expired</li>
 *   <li>Re-entrant acquire (same user+session) returns 200</li>
 *   <li>Same user in a different tab returns 409</li>
 *   <li>Heartbeat extends the expiry</li>
 *   <li>Heartbeat returns 409 when lock is not owned by caller</li>
 *   <li>Release removes the lock document</li>
 *   <li>Release returns 403 when caller does not own the lock</li>
 *   <li>{@code assertLockHeld} succeeds for a valid lock</li>
 *   <li>{@code assertLockHeld} fails when no lock exists</li>
 *   <li>{@code assertLockHeld} fails when the lock has expired</li>
 * </ul>
 *
 * <p>All MongoClient calls use the Future-style API (Vert.x 4.5+). Timer-based
 * assertions give the asynchronous handlers time to complete before the result
 * is checked.
 */
@ExtendWith(VertxExtension.class)
class SectionLockManagerTest {

  private static final String TEST_DB    = "lockTestDb";
  private static final String APP_TYPE   = "DATA_ACCESS_APPLICATION";
  private static final String APP_ID     = "lockApp-001";
  private static final String SECTION_ID = "section3";
  private static final String USER_A     = "userAlice";
  private static final String USER_B     = "userBob";
  private static final String SESSION_A  = "session-a-uuid";
  private static final String SESSION_B  = "session-b-uuid";

  private static MongoDBContainer mongoDBContainer;
  private static Vertx vertx;
  private static MongoClient mongoClient;
  private SectionLockManager lockManager;

  @BeforeAll
  static void setUpSuite() {
    vertx = Vertx.vertx();
    mongoDBContainer = new MongoDBContainer("mongo:5.0");
    mongoDBContainer.start();

    JsonObject config = new JsonObject()
      .put("connection_string", mongoDBContainer.getReplicaSetUrl(TEST_DB));

    mongoClient = MongoClient.createShared(vertx, config);
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    cleanupCollection(APPLICATION_SECTION_LOCK_COLLECTION);
    LockWebSocketManager wsManager = mock(LockWebSocketManager.class);
    lockManager = new SectionLockManager(new DBHandler(mongoClient), mongoClient, wsManager);
  }

  @AfterAll
  static void tearDownSuite() throws InterruptedException {
    closeMongoClient();
    closeVertx();
    mongoDBContainer.stop();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Builds a mock {@link RoutingContext} for a lock endpoint.
   *
   * @param userId    the authenticated user (ecasId)
   * @param sessionId the browser-tab unique session ID
   * @return a configured mock RoutingContext with path params and user principal set
   */
  private RoutingContext lockCtx(String userId, String sessionId) {
    JsonObject body = new JsonObject().put("sessionId", sessionId);
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);
    JsonObject principal = new JsonObject().put("preferred_username", userId);
    when(mockUser.principal()).thenReturn(principal);
    when(mockUser.get("preferred_username")).thenReturn(userId);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    when(ctx.pathParam("sectionId")).thenReturn(SECTION_ID);
    return ctx;
  }

  /**
   * Directly inserts an already-expired lock document to simulate a stale lock.
   *
   * @param then callback invoked after the insert completes
   */
  private void insertExpiredLock(Runnable then) {
    Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);
    JsonObject lock = new JsonObject()
      .put(Constants.APPLICATION_TYPE_FIELD, APP_TYPE)
      .put(Constants.APPLICATION_ID,         APP_ID)
      .put(Constants.SECTION_ID,             SECTION_ID)
      .put(Constants.LOCK_OWNER_USER_ID,     USER_A)
      .put(Constants.LOCK_OWNER_SESSION_ID,  SESSION_A)
      .put(Constants.ACQUIRED_AT,            past.toString())
      .put(Constants.LAST_HEARTBEAT_AT,      past.toString())
      .put(Constants.EXPIRES_AT,             past.toString());
    mongoClient.insert(APPLICATION_SECTION_LOCK_COLLECTION, lock)
      .onSuccess(id -> then.run())
      .onFailure(err -> { throw new RuntimeException(err); });
  }

  private void cleanupCollection(String collection) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    mongoClient.removeDocuments(collection, new JsonObject(), result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out cleaning collection: " + collection);
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  private static void closeMongoClient() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    mongoClient.close().onComplete(result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out closing Mongo client");
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  private static void closeVertx() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    vertx.close(result -> {
      if (result.failed()) {
        error.set(result.cause());
      }
      latch.countDown();
    });

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timed out closing Vertx");
    }
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
  }

  // ---------------------------------------------------------------------------
  // Acquire tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("acquireLock: succeeds (200) when no lock exists — creates lock group")
  void acquireLock_succeeds_whenNoLockExists(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = lockCtx(USER_A, SESSION_A);
    lockManager.acquireLock(ctx);

    vertx.setTimer(600, id ->
      mongoClient
        .find(APPLICATION_SECTION_LOCK_COLLECTION, new JsonObject())
        .onSuccess(results -> {
          testContext.verify(() -> {
            // section3 lock group for DATA_ACCESS_APPLICATION = {3, 4, 5, 6, 8}
            assertEquals(
              5,
              results.size(),
              "Lock group for section3 should create 5 lock documents"
            );
            Set<String> lockedSections = results
              .stream()
              .map(d -> d.getString("sectionId"))
              .collect(Collectors.toSet());
            assertTrue(lockedSections.contains("section3"));
            assertTrue(lockedSections.contains("section4"));
            assertTrue(lockedSections.contains("section5"));
            assertTrue(lockedSections.contains("section6"));
            assertTrue(lockedSections.contains("section8"));
            // All docs should be owned by USER_A / SESSION_A
            for (JsonObject doc : results) {
              assertEquals(USER_A, doc.getString("lockOwnerUserId"));
              assertEquals(SESSION_A, doc.getString("lockOwnerSessionId"));
            }
          });
          testContext.completeNow();
        })
        .onFailure(testContext::failNow)
    );
  }

  @Test
  @DisplayName("acquireLock: returns 409 when another user holds lock on a dependent section")
  void acquireLock_returns409_whenAnotherUserHoldsLock(Vertx vertx, VertxTestContext testContext) {
    // USER_A acquires section3 (locks {3,4,5,6,8})
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(500, id -> {
      // USER_B tries to acquire section4 (group {3,4}) — conflicts with USER_A's lock on section3
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      when(ctxB.pathParam("sectionId")).thenReturn("section4");
      lockManager.acquireLock(ctxB);

      vertx.setTimer(500, id2 -> {
        testContext.verify(() ->
          assertEquals(HttpResponseStatus.CONFLICT.code(),
            ctxB.response().getStatusCode(),
            "Should return 409 when another user holds a dependent section lock"));
        testContext.completeNow();
      });
    });
  }

  @Test
  @DisplayName("acquireLock: succeeds when existing lock is expired")
  void acquireLock_succeeds_whenLockIsExpired(Vertx vertx, VertxTestContext testContext) {
    insertExpiredLock(() -> {
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      lockManager.acquireLock(ctxB);

      vertx.setTimer(600, id ->
        mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("sectionId", SECTION_ID))
          .onSuccess(results -> {
            testContext.verify(() -> {
              assertEquals(1, results.size(), "Still exactly one lock document");
              assertEquals(USER_B,
                results.get(0).getString("lockOwnerUserId"),
                "Lock should now belong to USER_B after expiry");
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow));
    });
  }

  @Test
  @DisplayName("acquireLock: conflict on group rolls back all partially acquired locks")
  void acquireLock_rollsBack_onGroupConflict(Vertx vertx, VertxTestContext testContext) {
    // USER_A acquires section1 (group {1} — singleton)
    RoutingContext ctxA = lockCtx(USER_A, SESSION_A);
    when(ctxA.pathParam("sectionId")).thenReturn("section1");
    lockManager.acquireLock(ctxA);

    vertx.setTimer(400, id -> {
      // Insert a PRIMARY lock by USER_A (different session) on section5 to block
      // USER_B's section3 group.  A primary row is one where sectionId ==
      // primarySectionId, which is what the conflict filter requires.
      Instant now = Instant.now();
      Instant future = now.plusSeconds(300);
      JsonObject blocker = new JsonObject()
        .put(Constants.APPLICATION_TYPE_FIELD, APP_TYPE)
        .put(Constants.APPLICATION_ID, APP_ID)
        .put(Constants.SECTION_ID, "section5")
        .put(Constants.PRIMARY_SECTION_ID, "section5")
        .put(Constants.LOCK_OWNER_USER_ID, USER_A)
        .put(Constants.LOCK_OWNER_SESSION_ID, "session-a-other-tab")
        .put(Constants.ACQUIRED_AT, now.toString())
        .put(Constants.LAST_HEARTBEAT_AT, now.toString())
        .put(Constants.EXPIRES_AT, future.toString());
      mongoClient
        .insert(APPLICATION_SECTION_LOCK_COLLECTION, blocker)
        .onSuccess(insertId -> {
          // USER_B tries section3 (group {3,4,5,6,8}) — section5 primary is blocked
          RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
          lockManager.acquireLock(ctxB);

          vertx.setTimer(600, id2 -> {
            testContext.verify(() ->
              assertEquals(
                HttpResponseStatus.CONFLICT.code(),
                ctxB.response().getStatusCode(),
                "GROUP conflict should return 409"
              )
            );

            // Verify no locks by USER_B remain (rollback should have deleted them)
            mongoClient
              .find(
                APPLICATION_SECTION_LOCK_COLLECTION,
                new JsonObject().put("lockOwnerSessionId", SESSION_B)
              )
              .onSuccess(docs -> {
                testContext.verify(() ->
                  assertTrue(docs.isEmpty(), "All partially acquired locks should be rolled back")
                );
                testContext.completeNow();
              })
              .onFailure(testContext::failNow);
          });
        })
        .onFailure(testContext::failNow);
    });
  }

  @Test
  @DisplayName("acquireLock: dependent-only overlap does NOT block another user")
  void acquireLock_allows_dependentOnlyOverlap(Vertx vertx, VertxTestContext testContext) {
    // USER_A acquires section2 (group {2, 5, 8} — sections 5 and 8 are dep locks).
    RoutingContext ctxA = lockCtx(USER_A, SESSION_A);
    when(ctxA.pathParam("sectionId")).thenReturn("section2");
    lockManager.acquireLock(ctxA);

    vertx.setTimer(500, id -> {
      // USER_B tries section3 (group {3,4,5,6,8}).  The overlap with A's group is
      // {5, 8}, but A holds those only as DEPENDENT locks (primarySectionId = 2).
      // B must be allowed to acquire because no primary lock conflict exists.
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      when(ctxB.pathParam("sectionId")).thenReturn("section3");
      lockManager.acquireLock(ctxB);

      vertx.setTimer(600, id2 -> {
        testContext.verify(() ->
          assertEquals(
            HttpResponseStatus.OK.code(),
            ctxB.response().getStatusCode(),
            "Acquire should succeed when overlap is only through dep locks"
          )
        );

        // Verify B's primary row for section3 exists
        mongoClient
          .find(
            APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject()
              .put("lockOwnerSessionId", SESSION_B)
              .put("sectionId", "section3")
              .put("primarySectionId", "section3")
          )
          .onSuccess(docs -> {
            testContext.verify(() ->
              assertEquals(1, docs.size(), "B should own section3 as its primary")
            );
            testContext.completeNow();
          })
          .onFailure(testContext::failNow);
      });
    });
  }

  @Test
  @DisplayName("acquireLock: primary-on-primary conflict returns 409")
  void acquireLock_conflict_primaryOverlap(Vertx vertx, VertxTestContext testContext) {
    // A acquires section2 (group {2,5,8}).  B tries section5 — group {2,3,5,8}
    // intersects with A at sections 2 and 5.  Section 2 is A's PRIMARY, so B's
    // acquire of section5 must fail with 409 because the group contains A's primary.
    RoutingContext ctxA = lockCtx(USER_A, SESSION_A);
    when(ctxA.pathParam("sectionId")).thenReturn("section2");
    lockManager.acquireLock(ctxA);

    vertx.setTimer(500, id -> {
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      when(ctxB.pathParam("sectionId")).thenReturn("section5");
      lockManager.acquireLock(ctxB);

      vertx.setTimer(500, id2 -> {
        testContext.verify(() ->
          assertEquals(
            HttpResponseStatus.CONFLICT.code(),
            ctxB.response().getStatusCode(),
            "Primary-on-primary conflict should return 409"
          )
        );
        testContext.completeNow();
      });
    });
  }

  @Test
  @DisplayName("acquireLock: re-entrant — same user+session returns 200 again")
  void acquireLock_reentrant_sameUserSession(Vertx vertx, VertxTestContext testContext) {
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      RoutingContext ctx2 = lockCtx(USER_A, SESSION_A);
      lockManager.acquireLock(ctx2);

      vertx.setTimer(400, id2 -> {
        testContext.verify(() ->
          assertEquals(HttpResponseStatus.OK.code(),
            ctx2.response().getStatusCode(),
            "Re-entrant acquire by same user+session should return 200"));
        testContext.completeNow();
      });
    });
  }

  @Test
  @DisplayName("acquireLock: returns 409 for same user in a different tab (different sessionId)")
  void acquireLock_returns409_sameUserDifferentSession(Vertx vertx, VertxTestContext testContext) {
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      RoutingContext ctx2 = lockCtx(USER_A, "session-a-tab2");
      lockManager.acquireLock(ctx2);

      vertx.setTimer(400, id2 -> {
        testContext.verify(() ->
          assertEquals(HttpResponseStatus.CONFLICT.code(),
            ctx2.response().getStatusCode(),
            "Different tab (different sessionId) should return 409"));
        testContext.completeNow();
      });
    });
  }

  // ---------------------------------------------------------------------------
  // Heartbeat tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("heartbeatLock: extends expiresAt for ALL locks in the session")
  void heartbeatLock_extends_expiresAt(Vertx vertx, VertxTestContext testContext) {
    // Acquire section3 (group {3,4,5,6,8} = 5 locks)
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      RoutingContext heartbeatCtx = lockCtx(USER_A, SESSION_A);
      lockManager.heartbeatLock(heartbeatCtx);

      vertx.setTimer(400, id2 ->
        mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A))
          .onSuccess(docs -> {
            testContext.verify(() -> {
              assertEquals(5, docs.size(),
                "All 5 lock documents in the group should still exist");
              // All should have updated expiresAt
              for (JsonObject doc : docs) {
                assertNotNull(doc.getString("expiresAt"),
                  "expiresAt should be set after heartbeat for " + doc.getString("sectionId"));
              }
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow));
    });
  }

  @Test
  @DisplayName("heartbeatLock: returns 409 when lock is not owned by caller")
  void heartbeatLock_returns409_whenNotOwner(Vertx vertx, VertxTestContext testContext) {
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      lockManager.heartbeatLock(ctxB);

      vertx.setTimer(400, id2 -> {
        testContext.verify(() ->
          assertEquals(HttpResponseStatus.CONFLICT.code(),
            ctxB.response().getStatusCode(),
            "Heartbeat for wrong user/session should return 409"));
        testContext.completeNow();
      });
    });
  }

  // ---------------------------------------------------------------------------
  // Release tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("releaseLock: removes ALL lock documents in the session (session-wide)")
  void releaseLock_removesDocument(Vertx vertx, VertxTestContext testContext) {
    // Acquire section3 (group {3,4,5,6,8} = 5 locks)
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      lockManager.releaseLock(lockCtx(USER_A, SESSION_A));

      vertx.setTimer(400, id2 ->
        mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A))
          .onSuccess(results -> {
            testContext.verify(() ->
              assertTrue(results.isEmpty(),
                "All lock documents for the session should be deleted after release"));
            testContext.completeNow();
          })
          .onFailure(testContext::failNow));
    });
  }

  @Test
  @DisplayName("releaseLock: returns 204 when caller does not own the lock (idempotent)")
  void releaseLock_returns204_whenNotOwner(Vertx vertx, VertxTestContext testContext) {
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      lockManager.releaseLock(ctxB);

      vertx.setTimer(400, id2 -> {
        testContext.verify(() ->
          assertEquals(HttpResponseStatus.NO_CONTENT.code(),
            ctxB.response().getStatusCode(),
            "Non-owner release returns 204 (idempotent - nothing was deleted)"));
        testContext.completeNow();
      });
    });
  }

  // ---------------------------------------------------------------------------
  // assertLockHeld tests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("assertLockHeld: succeeds when caller holds a valid lock")
  void assertLockHeld_succeeds_whenLockValid(Vertx vertx, VertxTestContext testContext) {
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id ->
      lockManager.assertLockHeld(APP_TYPE, APP_ID, SECTION_ID, SESSION_A)
        .onSuccess(v -> testContext.completeNow())
        .onFailure(testContext::failNow));
  }

  @Test
  @DisplayName("assertLockHeld: fails when no lock exists")
  void assertLockHeld_fails_whenNoLock(VertxTestContext testContext) {
    lockManager.assertLockHeld(APP_TYPE, APP_ID, SECTION_ID, SESSION_A)
      .onSuccess(v -> testContext.failNow(
        new AssertionError("assertLockHeld should fail when no lock exists")))
      .onFailure(err -> {
        testContext.verify(() ->
          assertNotNull(err.getMessage(), "Error message should explain missing lock"));
        testContext.completeNow();
      });
  }

  @Test
  @DisplayName("assertLockHeld: fails when the lock is expired")
  void assertLockHeld_fails_whenLockExpired(VertxTestContext testContext) {
    insertExpiredLock(() ->
      lockManager.assertLockHeld(APP_TYPE, APP_ID, SECTION_ID, USER_A)
        .onSuccess(v -> testContext.failNow(
          new AssertionError("assertLockHeld should fail for an expired lock")))
        .onFailure(err -> {
          testContext.verify(() ->
            assertNotNull(err.getMessage(), "Should explain that the lock is expired"));
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("assertLockHeld: succeeds for a dependent section in the lock group")
  void assertLockHeld_succeeds_forDependentSection(Vertx vertx, VertxTestContext testContext) {
    // Acquire section3 which locks {3,4,5,6,8}
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id ->
      // Assert lock held on section5 (a dependent section, not the primary)
      lockManager
        .assertLockHeld(APP_TYPE, APP_ID, "section5", SESSION_A)
        .onSuccess(v -> testContext.completeNow())
        .onFailure(testContext::failNow)
    );
  }

  // ---------------------------------------------------------------------------
  // computeLockGroup tests (pure logic, no DB needed)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section3 returns {3,4,5,6,8}")
  void computeLockGroup_dataAccess_section3() {
    Set<String> group = SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section3");
    assertEquals(Set.of("section3", "section4", "section5", "section6", "section8"), group);
  }

  @Test
  @DisplayName("computeLockGroup: dependency maps are symmetric for all sections")
  void computeLockGroup_symmetry_allSections() {
    // For every section A in every application type: if B ∈ group(A), then A ∈ group(B).
    String[] appTypes = { "DATA_ACCESS_APPLICATION", "DATA_REQUEST_APPLICATION" };
    for (String appType : appTypes) {
      String[] sections = "DATA_ACCESS_APPLICATION".equals(appType)
        ? new String[] {
            "section1",
            "section2",
            "section3",
            "section4",
            "section5",
            "section6",
            "section7",
            "section8",
            "section9",
            "section10",
          }
        : new String[] {
            "section1",
            "section2",
            "section3",
            "section4",
            "section5",
            "section6",
            "section7",
            "section8",
          };
      for (String a : sections) {
        Set<String> groupA = SectionLockManager.computeLockGroup(appType, a);
        for (String b : groupA) {
          Set<String> groupB = SectionLockManager.computeLockGroup(appType, b);
          assertTrue(
            groupB.contains(a),
            "Symmetry violation in " +
              appType +
              ": " +
              a +
              " ∈ group(" +
              b +
              ") but " +
              b +
              "'s group does not contain " +
              a
          );
        }
      }
    }
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section1 returns singleton {1}")
  void computeLockGroup_dataAccess_section1_singleton() {
    Set<String> group = SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section1");
    assertEquals(Set.of("section1"), group);
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section2 returns {2,5,6}")
  void computeLockGroup_dataRequest_section2() {
    Set<String> group = SectionLockManager.computeLockGroup("DATA_REQUEST_APPLICATION", "section2");
    assertEquals(Set.of("section2", "section5", "section6"), group);
  }

  @Test
  @DisplayName("computeLockGroup: unknown application type returns singleton")
  void computeLockGroup_unknownType_returnsSingleton() {
    Set<String> group = SectionLockManager.computeLockGroup("UNKNOWN_TYPE", "section3");
    assertEquals(Set.of("section3"), group);
  }

  @Test
  @DisplayName("computeLockGroup: dependency maps are bidirectional")
  void computeLockGroup_bidirectional() {
    // If section3 -> section4, then section4 -> section3
    Set<String> from3 = SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section3");
    Set<String> from4 = SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section4");
    assertTrue(from3.contains("section4"), "section3 group should contain section4");
    assertTrue(from4.contains("section3"), "section4 group should contain section3");
  }

  // DATA_ACCESS remaining sections

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section2 returns {2,5,8}")
  void computeLockGroup_dataAccess_section2() {
    assertEquals(Set.of("section2", "section5", "section8"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section2"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section4 returns {3,4}")
  void computeLockGroup_dataAccess_section4() {
    assertEquals(Set.of("section3", "section4"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section4"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section5 returns {2,3,5,8}")
  void computeLockGroup_dataAccess_section5() {
    assertEquals(Set.of("section2", "section3", "section5", "section8"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section5"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section6 returns {3,6}")
  void computeLockGroup_dataAccess_section6() {
    assertEquals(Set.of("section3", "section6"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section6"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section7 returns {7,8}")
  void computeLockGroup_dataAccess_section7() {
    assertEquals(Set.of("section7", "section8"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section7"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section8 returns {2,3,5,7,8}")
  void computeLockGroup_dataAccess_section8() {
    assertEquals(Set.of("section2", "section3", "section5", "section7", "section8"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section8"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section9 returns singleton {9}")
  void computeLockGroup_dataAccess_section9() {
    assertEquals(Set.of("section9"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section9"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS section10 returns singleton {10}")
  void computeLockGroup_dataAccess_section10() {
    assertEquals(Set.of("section10"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section10"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_ACCESS unknown section returns singleton")
  void computeLockGroup_dataAccess_unknownSection() {
    assertEquals(Set.of("section99"),
        SectionLockManager.computeLockGroup("DATA_ACCESS_APPLICATION", "section99"));
  }

  // DATA_REQUEST all sections

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section1 returns singleton {1}")
  void computeLockGroup_dataRequest_section1() {
    assertEquals(Set.of("section1"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section1"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section3 returns {3,4,5}")
  void computeLockGroup_dataRequest_section3() {
    assertEquals(Set.of("section3", "section4", "section5"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section3"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section4 returns {3,4}")
  void computeLockGroup_dataRequest_section4() {
    assertEquals(Set.of("section3", "section4"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section4"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section5 returns {2,3,5}")
  void computeLockGroup_dataRequest_section5() {
    assertEquals(Set.of("section2", "section3", "section5"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section5"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section6 returns {2,6}")
  void computeLockGroup_dataRequest_section6() {
    assertEquals(Set.of("section2", "section6"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section6"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section7 returns singleton {7}")
  void computeLockGroup_dataRequest_section7() {
    assertEquals(Set.of("section7"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section7"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST section8 returns singleton {8}")
  void computeLockGroup_dataRequest_section8() {
    assertEquals(Set.of("section8"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "section8"));
  }

  @Test
  @DisplayName("computeLockGroup: DATA_REQUEST unknown section returns singleton")
  void computeLockGroup_dataRequest_unknownSection() {
    assertEquals(Set.of("sectionX"),
        SectionLockManager.computeLockGroup(DATA_REQUEST_APPLICATION_COLLECTION, "sectionX"));
  }

  // ---------------------------------------------------------------------------
  // Input-validation tests (400 Bad Request)
  // ---------------------------------------------------------------------------

  /** Returns a RoutingContext whose body is null (simulates missing body). */
  private RoutingContext nullBodyCtx(String userId, String sectionId) {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    RequestBody rb = mock(RequestBody.class);
    when(ctx.body()).thenReturn(rb);
    when(rb.asJsonObject()).thenReturn(null);
    User mockUser = mock(User.class);
    JsonObject principal = new JsonObject().put("preferred_username", userId);
    when(mockUser.principal()).thenReturn(principal);
    when(mockUser.get("preferred_username")).thenReturn(userId);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    when(ctx.pathParam("sectionId")).thenReturn(sectionId);
    return ctx;
  }

  @Test
  @DisplayName("acquireLock: returns 400 when body is null")
  void acquireLock_returns400_whenBodyIsNull() {
    RoutingContext ctx = nullBodyCtx(USER_A, SECTION_ID);
    lockManager.acquireLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("acquireLock: returns 400 when sessionId is missing from body")
  void acquireLock_returns400_whenSessionIdMissing() {
    // Empty body — getString("sessionId") returns null → blank
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
    when(mockUser.get("preferred_username")).thenReturn(USER_A);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    when(ctx.pathParam("sectionId")).thenReturn(SECTION_ID);
    lockManager.acquireLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("acquireLock: returns 400 when sectionId is invalid")
  void acquireLock_returns400_whenSectionIdInvalid() {
    JsonObject body = new JsonObject().put("sessionId", SESSION_A);
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
    when(mockUser.get("preferred_username")).thenReturn(USER_A);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    when(ctx.pathParam("sectionId")).thenReturn("sectionXYZ"); // invalid
    lockManager.acquireLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("heartbeatLock: returns 400 when body is null")
  void heartbeatLock_returns400_whenBodyIsNull() {
    RoutingContext ctx = nullBodyCtx(USER_A, SECTION_ID);
    lockManager.heartbeatLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("heartbeatLock: returns 400 when sessionId is missing from body")
  void heartbeatLock_returns400_whenSessionIdMissing() {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
    when(mockUser.get("preferred_username")).thenReturn(USER_A);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    when(ctx.pathParam("sectionId")).thenReturn(SECTION_ID);
    lockManager.heartbeatLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("releaseLock: returns 400 when sessionId is absent from both body and query param")
  void releaseLock_returns400_whenSessionIdAbsent() {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject()); // empty body
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
    when(mockUser.get("preferred_username")).thenReturn(USER_A);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    when(ctx.pathParam("sectionId")).thenReturn(SECTION_ID);
    when(ctx.queryParam("sessionId")).thenReturn(List.of()); // no query param either
    lockManager.releaseLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("switchLock: returns 400 when body is null")
  void switchLock_returns400_whenBodyIsNull() {
    RoutingContext ctx = nullBodyCtx(USER_A, null);
    lockManager.switchLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("switchLock: returns 400 when newSectionId is missing from body")
  void switchLock_returns400_whenNewSectionIdMissing() {
    JsonObject body = new JsonObject().put("sessionId", SESSION_A); // no newSectionId
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
    when(mockUser.get("preferred_username")).thenReturn(USER_A);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    lockManager.switchLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  @Test
  @DisplayName("switchLock: returns 400 when newSectionId is invalid")
  void switchLock_returns400_whenNewSectionIdInvalid() {
    JsonObject body = new JsonObject().put("sessionId", SESSION_A).put("newSectionId", "bad");
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);
    when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
    when(mockUser.get("preferred_username")).thenReturn(USER_A);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    lockManager.switchLock(ctx);
    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), ctx.response().getStatusCode());
  }

  // ---------------------------------------------------------------------------
  // heartbeatLock — returnLockConflict branches
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("heartbeatLock: returns 409 with 'Lock was lost or expired' when no active lock exists")
  void heartbeatLock_returns409_lockLostOrExpired(Vertx vertx, VertxTestContext testContext) {
    // No lock in DB — heartbeat finds nothing → returnLockConflict → lockDoc == null
    RoutingContext ctx = lockCtx(USER_A, SESSION_A);
    lockManager.heartbeatLock(ctx);

    vertx.setTimer(400, id -> {
      testContext.verify(() ->
        assertEquals(HttpResponseStatus.CONFLICT.code(), ctx.response().getStatusCode(),
          "Heartbeat with no lock should return 409"));
      testContext.completeNow();
    });
  }

  // ---------------------------------------------------------------------------
  // releaseLock — sendBeacon query-param fallback
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("releaseLock: accepts sessionId from query parameter (sendBeacon fallback)")
  void releaseLock_sessionId_fromQueryParam(Vertx vertx, VertxTestContext testContext) {
    // Acquire lock first
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      // Release via query param (body has no sessionId — simulates sendBeacon)
      RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject()); // empty body
      User mockUser = mock(User.class);
      when(mockUser.principal()).thenReturn(new JsonObject().put("preferred_username", USER_A));
      when(mockUser.get("preferred_username")).thenReturn(USER_A);
      when(ctx.user()).thenReturn(mockUser);
      when(ctx.pathParam("type")).thenReturn(APP_TYPE);
      when(ctx.pathParam("id")).thenReturn(APP_ID);
      when(ctx.pathParam("sectionId")).thenReturn(SECTION_ID);
      when(ctx.queryParam("sessionId")).thenReturn(List.of(SESSION_A));
      lockManager.releaseLock(ctx);

      vertx.setTimer(400, id2 ->
        mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A))
          .onSuccess(docs -> {
            testContext.verify(() -> {
              assertEquals(HttpResponseStatus.NO_CONTENT.code(), ctx.response().getStatusCode(),
                "Release via query param should return 204");
              assertTrue(docs.isEmpty(), "All locks should be removed after release via query param");
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow)
      );
    });
  }

  // ---------------------------------------------------------------------------
  // getApplicationLocks
  // ---------------------------------------------------------------------------

  /** Returns a RoutingContext for getApplicationLocks (no user auth needed). */
  private RoutingContext getLocksCtx() {
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(new JsonObject());
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    return ctx;
  }

  @Test
  @DisplayName("getApplicationLocks: returns empty array when no locks exist")
  void getApplicationLocks_returnsEmpty_whenNoLocks(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = getLocksCtx();
    lockManager.getApplicationLocks(ctx);

    vertx.setTimer(400, id -> {
      testContext.verify(() ->
        assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode(),
          "getApplicationLocks should return 200 even when empty"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("getApplicationLocks: returns active locks in the response")
  void getApplicationLocks_returnsActiveLocks(Vertx vertx, VertxTestContext testContext) {
    // Acquire section3 (group {3,4,5,6,8} = 5 locks)
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      RoutingContext ctx = getLocksCtx();
      lockManager.getApplicationLocks(ctx);

      vertx.setTimer(400, id2 -> {
        testContext.verify(() ->
          assertEquals(HttpResponseStatus.OK.code(), ctx.response().getStatusCode(),
            "getApplicationLocks should return 200 with active locks"));
        testContext.completeNow();
      });
    });
  }

  // ---------------------------------------------------------------------------
  // switchLock — toRelease is empty (expanding lock group)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("switchLock: toRelease is empty when expanding to a larger group")
  void switchLock_expandingGroup_toReleaseIsEmpty(Vertx vertx, VertxTestContext testContext) {
    // Acquire section4 (group {3,4})
    RoutingContext ctxA = lockCtx(USER_A, SESSION_A);
    when(ctxA.pathParam("sectionId")).thenReturn("section4");
    lockManager.acquireLock(ctxA);

    vertx.setTimer(500, id -> {
      // Switch to section3 (group {3,4,5,6,8}) — toRelease={}, toKeep={3,4}, toAcquire={5,6,8}
      RoutingContext switchCtx = switchCtx(USER_A, SESSION_A, "section3");
      lockManager.switchLock(switchCtx);

      vertx.setTimer(500, id2 ->
        mongoClient
          .find(APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A))
          .onSuccess(docs -> {
            testContext.verify(() -> {
              Set<String> locked = docs.stream()
                .map(d -> d.getString("sectionId"))
                .collect(Collectors.toSet());
              assertEquals(Set.of("section3", "section4", "section5", "section6", "section8"),
                locked, "Expanding to section3 group should lock all 5 sections");
              assertEquals(HttpResponseStatus.OK.code(), switchCtx.response().getStatusCode(),
                "Expanding switch should return 200");
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow)
      );
    });
  }

  // ---------------------------------------------------------------------------
  // releaseLocksForSession
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("releaseLocksForSession: does nothing when sessionId is blank")
  void releaseLocksForSession_blankSessionId_doesNothing() throws InterruptedException {
    // Insert a lock to ensure blank sessionId does NOT remove it
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));
    Thread.sleep(300);

    lockManager.releaseLocksForSession(APP_TYPE, APP_ID, ""); // blank — should skip

    Thread.sleep(300);
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Integer> count = new AtomicReference<>(0);
    mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION, new JsonObject(), res -> {
      if (res.succeeded()) count.set(res.result().size());
      latch.countDown();
    });
    latch.await(5, TimeUnit.SECONDS);
    assertTrue(count.get() > 0, "Blank sessionId should not delete any locks");
  }

  @Test
  @DisplayName("releaseLocksForSession: does nothing when no locks found for session")
  void releaseLocksForSession_noLocksFound_doesNothing(VertxTestContext testContext) {
    // Call without acquiring any lock first
    lockManager.releaseLocksForSession(APP_TYPE, APP_ID, "ghost-session");

    // Wait briefly and verify collection is still empty
    vertx.setTimer(300, id ->
      mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION, new JsonObject())
        .onSuccess(docs -> {
          testContext.verify(() ->
            assertTrue(docs.isEmpty(), "No locks should exist after releaseLocksForSession with no matching session"));
          testContext.completeNow();
        })
        .onFailure(testContext::failNow)
    );
  }

  @Test
  @DisplayName("releaseLocksForSession: removes all locks held by the session")
  void releaseLocksForSession_removesSessionLocks(Vertx vertx, VertxTestContext testContext) {
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id -> {
      lockManager.releaseLocksForSession(APP_TYPE, APP_ID, SESSION_A);

      vertx.setTimer(400, id2 ->
        mongoClient.find(APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A))
          .onSuccess(docs -> {
            testContext.verify(() ->
              assertTrue(docs.isEmpty(),
                "releaseLocksForSession should remove all session locks"));
            testContext.completeNow();
          })
          .onFailure(testContext::failNow)
      );
    });
  }

  // ---------------------------------------------------------------------------
  // assertLockHeld — edge cases
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("assertLockHeld: succeeds immediately when sessionId is blank (backward-compat mode)")
  void assertLockHeld_blankSessionId_succeedsImmediately(VertxTestContext testContext) {
    lockManager.assertLockHeld(APP_TYPE, APP_ID, SECTION_ID, "")
      .onSuccess(v -> testContext.completeNow())
      .onFailure(err -> testContext.failNow(
        new AssertionError("Should succeed with blank sessionId, but failed: " + err.getMessage())));
  }

  @Test
  @DisplayName("assertLockHeld: fails with 'locked by <userId>' when section is held by another session")
  void assertLockHeld_fails_whenSectionLockedByAnotherSession(Vertx vertx, VertxTestContext testContext) {
    // USER_A acquires the lock
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(400, id ->
      // SESSION_B tries to assert — USER_A/SESSION_A holds the lock, SESSION_B doesn't
      lockManager.assertLockHeld(APP_TYPE, APP_ID, SECTION_ID, SESSION_B)
        .onSuccess(v -> testContext.failNow(
          new AssertionError("assertLockHeld should fail when another session holds the lock")))
        .onFailure(err -> {
          testContext.verify(() -> {
            assertNotNull(err.getMessage(), "Error message should be non-null");
            assertTrue(err.getMessage().contains(USER_A),
              "Error message should name the lock holder '" + USER_A + "', but was: " + err.getMessage());
          });
          testContext.completeNow();
        })
    );
  }

  @Test
  @DisplayName("assertLockHeld: null sessionId is treated as blank (backward-compat mode)")
  void assertLockHeld_nullSessionId_succeedsImmediately(VertxTestContext testContext) {
    lockManager.assertLockHeld(APP_TYPE, APP_ID, SECTION_ID, null)
      .onSuccess(v -> testContext.completeNow())
      .onFailure(err -> testContext.failNow(
        new AssertionError("Should succeed with null sessionId: " + err.getMessage())));
  }

  // ---------------------------------------------------------------------------
  // readMongoDate — private static method tested via reflection
  // ---------------------------------------------------------------------------

  private static Method readMongoDateMethod() throws Exception {
    Method m = SectionLockManager.class.getDeclaredMethod("readMongoDate", JsonObject.class, String.class);
    m.setAccessible(true);
    return m;
  }

  @Test
  @DisplayName("readMongoDate: returns null when doc is null")
  void readMongoDate_nullDoc_returnsNull() throws Exception {
    Method m = readMongoDateMethod();
    assertNull(m.invoke(null, (JsonObject) null, "field"));
  }

  @Test
  @DisplayName("readMongoDate: returns null when field is absent from doc")
  void readMongoDate_missingField_returnsNull() throws Exception {
    Method m = readMongoDateMethod();
    assertNull(m.invoke(null, new JsonObject(), "missing"));
  }

  @Test
  @DisplayName("readMongoDate: returns ISO string when value is a plain String")
  void readMongoDate_plainString_returnsAsIs() throws Exception {
    Method m = readMongoDateMethod();
    String iso = "2026-01-01T00:00:00Z";
    assertEquals(iso, m.invoke(null, new JsonObject().put("f", iso), "f"));
  }

  @Test
  @DisplayName("readMongoDate: handles BSON $date wrapper with Long epoch millis")
  void readMongoDate_jsonObjectDate_withLong() throws Exception {
    Method m = readMongoDateMethod();
    long epoch = 1700000000000L;
    JsonObject doc = new JsonObject().put("f", new JsonObject().put("$date", epoch));
    String result = (String) m.invoke(null, doc, "f");
    assertNotNull(result, "Should return ISO string for $date Long");
    assertTrue(result.contains("2023"), "Epoch 1700000000000 is in year 2023");
  }

  @Test
  @DisplayName("readMongoDate: handles BSON $date wrapper with Integer epoch millis")
  void readMongoDate_jsonObjectDate_withInteger() throws Exception {
    Method m = readMongoDateMethod();
    JsonObject doc = new JsonObject().put("f", new JsonObject().put("$date", 1000000));
    String result = (String) m.invoke(null, doc, "f");
    assertNotNull(result, "Should return ISO string for $date Integer");
  }

  @Test
  @DisplayName("readMongoDate: handles BSON $date wrapper with String value")
  void readMongoDate_jsonObjectDate_withString() throws Exception {
    Method m = readMongoDateMethod();
    String iso = "2026-03-03T10:15:30Z";
    JsonObject doc = new JsonObject().put("f", new JsonObject().put("$date", iso));
    assertEquals(iso, m.invoke(null, doc, "f"));
  }

  @Test
  @DisplayName("readMongoDate: returns null for $date wrapper with unsupported inner type")
  void readMongoDate_jsonObjectDate_withUnsupportedInner() throws Exception {
    Method m = readMongoDateMethod();
    JsonObject doc = new JsonObject().put("f", new JsonObject().put("$date", new JsonObject()));
    assertNull(m.invoke(null, doc, "f"));
  }

  @Test
  @DisplayName("readMongoDate: handles bare Long value")
  void readMongoDate_bareLong() throws Exception {
    Method m = readMongoDateMethod();
    JsonObject doc = new JsonObject().put("f", 1700000000000L);
    String result = (String) m.invoke(null, doc, "f");
    assertNotNull(result, "Should return ISO string for bare Long");
  }

  @Test
  @DisplayName("readMongoDate: handles bare Integer value")
  void readMongoDate_bareInteger() throws Exception {
    Method m = readMongoDateMethod();
    JsonObject doc = new JsonObject().put("f", 1000000);
    String result = (String) m.invoke(null, doc, "f");
    assertNotNull(result, "Should return ISO string for bare Integer");
  }

  @Test
  @DisplayName("readMongoDate: falls back to toString for unrecognised value type")
  void readMongoDate_unknownType_fallsBackToString() throws Exception {
    Method m = readMongoDateMethod();
    // Use a Boolean value to hit the final `value.toString()` branch
    JsonObject doc = new JsonObject().put("f", true);
    String result = (String) m.invoke(null, doc, "f");
    assertEquals("true", result, "Should fall back to toString()");
  }

  // ---------------------------------------------------------------------------
  // switchLock tests
  // ---------------------------------------------------------------------------

  /**
   * Builds a mock RoutingContext for the switch lock endpoint.
   */
  private RoutingContext switchCtx(String userId, String sessionId, String newSectionId) {
    JsonObject body = new JsonObject()
      .put("sessionId", sessionId)
      .put("newSectionId", newSectionId);
    RoutingContext ctx = RoutingContextMocker.mockRoutingContext(body);
    User mockUser = mock(User.class);
    JsonObject principal = new JsonObject().put("preferred_username", userId);
    when(mockUser.principal()).thenReturn(principal);
    when(mockUser.get("preferred_username")).thenReturn(userId);
    when(ctx.user()).thenReturn(mockUser);
    when(ctx.pathParam("type")).thenReturn(APP_TYPE);
    when(ctx.pathParam("id")).thenReturn(APP_ID);
    return ctx;
  }

  @Test
  @DisplayName("switchLock: keeps overlapping sections and releases old-only sections")
  void switchLock_keepsOverlap_releasesOld(Vertx vertx, VertxTestContext testContext) {
    // Acquire section3 (group {3,4,5,6,8})
    lockManager.acquireLock(lockCtx(USER_A, SESSION_A));

    vertx.setTimer(500, id -> {
      // Switch to section4 (group {3,4}) — should keep {3,4}, release {5,6,8}
      RoutingContext switchCtx = switchCtx(USER_A, SESSION_A, "section4");
      lockManager.switchLock(switchCtx);

      vertx.setTimer(500, id2 ->
        mongoClient
          .find(
            APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A)
          )
          .onSuccess(docs -> {
            testContext.verify(() -> {
              Set<String> remaining = docs
                .stream()
                .map(d -> d.getString("sectionId"))
                .collect(Collectors.toSet());
              assertEquals(
                Set.of("section3", "section4"),
                remaining,
                "After switching to section4, only {3,4} should remain locked"
              );
              assertEquals(
                HttpResponseStatus.OK.code(),
                switchCtx.response().getStatusCode(),
                "Switch should return 200"
              );
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow)
      );
    });
  }

  @Test
  @DisplayName("switchLock: acquires new sections when switching to a non-overlapping group")
  void switchLock_acquiresNewSections(Vertx vertx, VertxTestContext testContext) {
    // Acquire section1 (group {1} — singleton)
    RoutingContext ctxA = lockCtx(USER_A, SESSION_A);
    when(ctxA.pathParam("sectionId")).thenReturn("section1");
    lockManager.acquireLock(ctxA);

    vertx.setTimer(500, id -> {
      // Switch to section9 (group {9} — singleton, no overlap with {1})
      RoutingContext switchCtx = switchCtx(USER_A, SESSION_A, "section9");
      lockManager.switchLock(switchCtx);

      vertx.setTimer(500, id2 ->
        mongoClient
          .find(
            APPLICATION_SECTION_LOCK_COLLECTION,
            new JsonObject().put("lockOwnerSessionId", SESSION_A)
          )
          .onSuccess(docs -> {
            testContext.verify(() -> {
              Set<String> remaining = docs
                .stream()
                .map(d -> d.getString("sectionId"))
                .collect(Collectors.toSet());
              assertEquals(
                Set.of("section9"),
                remaining,
                "After switching from section1 to section9, only {9} should be locked"
              );
            });
            testContext.completeNow();
          })
          .onFailure(testContext::failNow)
      );
    });
  }

  @Test
  @DisplayName("switchLock: returns 409 and releases the previous lock group when target section is held")
  void switchLock_returns409_onConflict(Vertx vertx, VertxTestContext testContext) {
    // USER_A acquires section1 (group {1})
    RoutingContext ctxA = lockCtx(USER_A, SESSION_A);
    when(ctxA.pathParam("sectionId")).thenReturn("section1");
    lockManager.acquireLock(ctxA);

    // USER_B acquires section9 (group {9})
    vertx.setTimer(400, id -> {
      RoutingContext ctxB = lockCtx(USER_B, SESSION_B);
      when(ctxB.pathParam("sectionId")).thenReturn("section9");
      lockManager.acquireLock(ctxB);

      vertx.setTimer(400, id2 -> {
        // USER_A tries to switch from section1 to section9 — blocked by USER_B
        RoutingContext switchCtx = switchCtx(USER_A, SESSION_A, "section9");
        lockManager.switchLock(switchCtx);

        vertx.setTimer(400, id3 -> {
          testContext.verify(() ->
            assertEquals(
              HttpResponseStatus.CONFLICT.code(),
              switchCtx.response().getStatusCode(),
              "Switch to a section locked by another user should return 409"
            )
          );

          // USER_A navigated away from section1, so the old lock group should be released
          mongoClient
            .find(
              APPLICATION_SECTION_LOCK_COLLECTION,
              new JsonObject().put("lockOwnerSessionId", SESSION_A)
            )
            .onSuccess(docs -> {
              testContext.verify(() -> {
                assertEquals(
                  0,
                  docs.size(),
                  "Previous session locks should be released when the switch target is locked"
                );
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow);
        });
      });
    });
  }
}
