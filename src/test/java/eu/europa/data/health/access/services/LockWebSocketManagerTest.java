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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockWebSocketManagerTest {

  private static final String APP_TYPE = "DATA_ACCESS_APPLICATION";
  private static final String APP_ID = "app-123";
  private static final String OTHER_APP_ID = "app-456";

  @Mock
  private ServerWebSocket webSocket;

  @Mock
  private ServerWebSocket otherWebSocket;

  @Captor
  private ArgumentCaptor<String> payloadCaptor;

  @InjectMocks
  private LockWebSocketManager lockWebSocketManager;

  @Test
  void register_sendsConnectedAck_andBroadcastsToRegisteredSocket() {
    when(webSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.broadcast(APP_TYPE, APP_ID, new JsonObject().put("event", "custom_event"));

    verify(webSocket, times(2)).writeTextMessage(payloadCaptor.capture());

    List<String> payloads = payloadCaptor.getAllValues();
    JsonObject connectedPayload = new JsonObject(payloads.get(0));
    assertEquals(LockWebSocketManager.EVENT_CONNECTED, connectedPayload.getString("event"));
    assertEquals(APP_TYPE, connectedPayload.getString("applicationType"));
    assertEquals(APP_ID, connectedPayload.getString("applicationId"));

    JsonObject broadcastPayload = new JsonObject(payloads.get(1));
    assertEquals("custom_event", broadcastPayload.getString("event"));
  }

  @Test
  void broadcast_targetsOnlySocketsInRequestedApplicationGroup() {
    when(webSocket.isClosed()).thenReturn(false);
    when(otherWebSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.register(APP_TYPE, OTHER_APP_ID, otherWebSocket);

    lockWebSocketManager.broadcast(
      APP_TYPE,
      APP_ID,
      new JsonObject().put("event", "group_specific")
    );

    verify(webSocket, times(2)).writeTextMessage(payloadCaptor.capture());
    verify(otherWebSocket, times(1)).writeTextMessage(payloadCaptor.capture());

    List<String> payloads = payloadCaptor.getAllValues();
    assertEquals(
      LockWebSocketManager.EVENT_CONNECTED,
      new JsonObject(payloads.get(0)).getString("event")
    );
    assertEquals("group_specific", new JsonObject(payloads.get(1)).getString("event"));
    assertEquals(
      LockWebSocketManager.EVENT_CONNECTED,
      new JsonObject(payloads.get(2)).getString("event")
    );
  }

  @Test
  void unregister_removesSocketFromGroup() {
    when(webSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.unregister(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.broadcast(
      APP_TYPE,
      APP_ID,
      new JsonObject().put("event", "after_unregister")
    );

    verify(webSocket).writeTextMessage(payloadCaptor.capture());

    JsonObject connectedPayload = new JsonObject(payloadCaptor.getValue());
    assertEquals(LockWebSocketManager.EVENT_CONNECTED, connectedPayload.getString("event"));
  }

  @Test
  void broadcast_skipsClosedSockets() {
    when(webSocket.isClosed()).thenReturn(true);
    when(otherWebSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.register(APP_TYPE, APP_ID, otherWebSocket);

    lockWebSocketManager.broadcast(APP_TYPE, APP_ID, new JsonObject().put("event", "custom_event"));

    verify(webSocket, times(1)).writeTextMessage(payloadCaptor.capture());
    verify(otherWebSocket, times(2)).writeTextMessage(payloadCaptor.capture());

    List<String> payloads = payloadCaptor.getAllValues();
    assertEquals(
      LockWebSocketManager.EVENT_CONNECTED,
      new JsonObject(payloads.get(0)).getString("event")
    );
    assertEquals(
      LockWebSocketManager.EVENT_CONNECTED,
      new JsonObject(payloads.get(1)).getString("event")
    );
    assertEquals("custom_event", new JsonObject(payloads.get(2)).getString("event"));
  }

  @Test
  void broadcastLockAcquired_sendsExpectedPayload() {
    when(webSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.broadcastLockAcquired(
      APP_TYPE,
      APP_ID,
      "section3",
      "john.doe",
      "session-xyz",
      "2026-03-03T12:30:00Z",
      "section3"
    );

    verify(webSocket, times(2)).writeTextMessage(payloadCaptor.capture());

    JsonObject payload = new JsonObject(payloadCaptor.getAllValues().get(1));
    assertEquals(LockWebSocketManager.EVENT_LOCK_ACQUIRED, payload.getString("event"));
    assertEquals(APP_TYPE, payload.getString("applicationType"));
    assertEquals(APP_ID, payload.getString("applicationId"));
    assertEquals("section3", payload.getString("sectionId"));
    assertEquals("john.doe", payload.getString("lockOwnerUserId"));
    assertEquals("session-xyz", payload.getString("lockOwnerSessionId"));
    assertEquals("2026-03-03T12:30:00Z", payload.getString("expiresAt"));
    assertEquals("section3", payload.getString("primarySectionId"));
  }

  @Test
  void broadcastLockReleased_sendsExpectedPayload() {
    when(webSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.broadcastLockReleased(APP_TYPE, APP_ID, "section4", "session-xyz");

    verify(webSocket, times(2)).writeTextMessage(payloadCaptor.capture());

    JsonObject payload = new JsonObject(payloadCaptor.getAllValues().get(1));
    assertEquals(LockWebSocketManager.EVENT_LOCK_RELEASED, payload.getString("event"));
    assertEquals(APP_TYPE, payload.getString("applicationType"));
    assertEquals(APP_ID, payload.getString("applicationId"));
    assertEquals("section4", payload.getString("sectionId"));
    assertEquals("session-xyz", payload.getString("lockOwnerSessionId"));
  }

  @Test
  void broadcastSectionSaved_includesSessionIdAndTimestampWhenProvided() {
    when(webSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.broadcastSectionSaved(
      APP_TYPE,
      APP_ID,
      "section5",
      "john.doe",
      "session-1"
    );

    verify(webSocket, times(2)).writeTextMessage(payloadCaptor.capture());

    JsonObject payload = new JsonObject(payloadCaptor.getAllValues().get(1));
    assertEquals(LockWebSocketManager.EVENT_SECTION_SAVED, payload.getString("event"));
    assertEquals(APP_TYPE, payload.getString("applicationType"));
    assertEquals(APP_ID, payload.getString("applicationId"));
    assertEquals("section5", payload.getString("sectionId"));
    assertEquals("john.doe", payload.getString("savedByUserId"));
    assertEquals("session-1", payload.getString("savedBySessionId"));
    assertNotNull(payload.getString("timestamp"));
  }

  @Test
  void broadcastSectionSaved_omitsSessionIdWhenNotProvided() {
    when(webSocket.isClosed()).thenReturn(false);

    lockWebSocketManager.register(APP_TYPE, APP_ID, webSocket);
    lockWebSocketManager.broadcastSectionSaved(APP_TYPE, APP_ID, "section5", "john.doe", null);

    verify(webSocket, times(2)).writeTextMessage(payloadCaptor.capture());

    JsonObject payload = new JsonObject(payloadCaptor.getAllValues().get(1));
    assertEquals(LockWebSocketManager.EVENT_SECTION_SAVED, payload.getString("event"));
    assertFalse(payload.containsKey("savedBySessionId"));
    assertTrue(payload.containsKey("timestamp"));
  }
}
