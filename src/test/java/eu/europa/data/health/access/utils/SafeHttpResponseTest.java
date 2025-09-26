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
package eu.europa.data.health.access.utils;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SafeHttpResponseTest {

  @Test
  void nullBody_normalizesToEmptyBuffer_andReturnsNullJsonRepresentations() {
    // delegate with null body
    @SuppressWarnings("unchecked")
    HttpResponse<Buffer> delegate = mock(HttpResponse.class);
    when(delegate.bodyAsBuffer()).thenReturn(null);

    SafeHttpResponse safe = new SafeHttpResponse(delegate);

    assertNotNull(safe.bodyAsBuffer());
    assertEquals(0, safe.bodyAsBuffer().length());
    assertSame(safe.bodyAsBuffer(), safe.body(), "body() should return the same cached buffer");

    assertEquals("", safe.bodyAsString());
    assertEquals("", safe.bodyAsString("ISO-8859-1"));

    assertNull(safe.bodyAsJsonObject());
    assertNull(safe.bodyAsJsonArray());

    // constructor should have called delegate.bodyAsBuffer() exactly once
    verify(delegate, times(1)).bodyAsBuffer();
    verifyNoMoreInteractions(delegate); // none of the delegated methods were called in this test
  }

  @Test
  void nonEmptyObjectBody_isReused_andParsesToJsonObject() {
    @SuppressWarnings("unchecked")
    HttpResponse<Buffer> delegate = mock(HttpResponse.class);

    String json = "{\"a\":1}";
    Buffer buf = Buffer.buffer(json); // UTF-8 by default
    when(delegate.bodyAsBuffer()).thenReturn(buf);

    SafeHttpResponse safe = new SafeHttpResponse(delegate);

    // reuses the same buffer instance
    assertSame(buf, safe.bodyAsBuffer());
    assertEquals(json, safe.bodyAsString());

    JsonObject obj = safe.bodyAsJsonObject();
    assertNotNull(obj);
    assertEquals(1, obj.getInteger("a"));

    verify(delegate, times(2)).bodyAsBuffer();
  }

  @Test
  void nonEmptyArrayBody_parsesToJsonArray() {
    @SuppressWarnings("unchecked")
    HttpResponse<Buffer> delegate = mock(HttpResponse.class);

    String json = "[1,2,3]";
    when(delegate.bodyAsBuffer()).thenReturn(Buffer.buffer(json)); // UTF-8

    SafeHttpResponse safe = new SafeHttpResponse(delegate);

    // OK path: array parsing works
    JsonArray arr = safe.bodyAsJsonArray();
    assertNotNull(arr);
    assertEquals(List.of(1, 2, 3), arr.getList());

    verify(delegate, times(2)).bodyAsBuffer();
  }

  @Test
  void bodyAsString_withEncodingParam_isStillUtf8() {
    @SuppressWarnings("unchecked")
    HttpResponse<Buffer> delegate = mock(HttpResponse.class);

    String text = "Δelta–π"; // non-ASCII to prove UTF-8 handling
    Buffer buf = Buffer.buffer(text, String.valueOf(StandardCharsets.UTF_8));
    when(delegate.bodyAsBuffer()).thenReturn(buf);

    SafeHttpResponse safe = new SafeHttpResponse(delegate);

    // Ignores provided encoding and uses UTF-8
    assertEquals(text, safe.bodyAsString("ISO-8859-1"));
    assertEquals(text, safe.bodyAsString());

    verify(delegate, times(2)).bodyAsBuffer();
  }

  @Test
  void delegates_headers_status_version_trailers_and_more() {
    @SuppressWarnings("unchecked")
    HttpResponse<Buffer> delegate = mock(HttpResponse.class);

    // body must be stubbed for constructor
    when(delegate.bodyAsBuffer()).thenReturn(Buffer.buffer());

    // stubs for delegation
    when(delegate.version()).thenReturn(HttpVersion.HTTP_2);
    when(delegate.statusCode()).thenReturn(207);
    when(delegate.statusMessage()).thenReturn("Multi-Status");

    MultiMap headers = mock(MultiMap.class);
    when(delegate.headers()).thenReturn(headers);
    when(delegate.getHeader("X-Test")).thenReturn("v1");
    when(delegate.getHeader((CharSequence) "X-Seq")).thenReturn("v2");

    MultiMap trailers = mock(MultiMap.class);
    when(delegate.trailers()).thenReturn(trailers);
    when(delegate.getTrailer("T")).thenReturn("trail");

    List<String> cookies = List.of("a=b", "c=d");
    when(delegate.cookies()).thenReturn(cookies);

    List<String> redirects = List.of("http://r1", "http://r2");
    when(delegate.followedRedirects()).thenReturn(redirects);

    SafeHttpResponse safe = new SafeHttpResponse(delegate);

    // verify delegations
    assertEquals(HttpVersion.HTTP_2, safe.version());
    assertEquals(207, safe.statusCode());
    assertEquals("Multi-Status", safe.statusMessage());
    assertSame(headers, safe.headers());
    assertEquals("v1", safe.getHeader("X-Test"));
    assertEquals("v2", safe.getHeader((CharSequence) "X-Seq"));
    assertSame(trailers, safe.trailers());
    assertEquals("trail", safe.getTrailer("T"));
    assertEquals(cookies, safe.cookies());
    assertEquals(redirects, safe.followedRedirects());

    // called once in constructor + the above delegated calls
    verify(delegate, times(2)).bodyAsBuffer();
    verify(delegate).version();
    verify(delegate).statusCode();
    verify(delegate).statusMessage();
    verify(delegate).headers();
    verify(delegate).getHeader("X-Test");
    verify(delegate).getHeader((CharSequence) "X-Seq");
    verify(delegate).trailers();
    verify(delegate).getTrailer("T");
    verify(delegate).cookies();
    verify(delegate).followedRedirects();
    verifyNoMoreInteractions(delegate);
  }
}
