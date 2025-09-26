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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wrapper around Vert.x HttpResponse<Buffer> that guarantees: - bodyAsBuffer() never returns null
 * (empty file -> Buffer.buffer()). - All other methods delegate to the original HttpResponse.
 */
public class SafeHttpResponse implements HttpResponse<Buffer> {

  private final HttpResponse<Buffer> delegate; // the original Vert.x response
  private final Buffer safeBody;               // normalized non-null body

  public SafeHttpResponse(HttpResponse<Buffer> delegate) {
    this.delegate = delegate;
    // Normalize: if the upstream body is null (0-byte file), replace with an empty buffer
    this.safeBody = delegate.bodyAsBuffer() != null ? delegate.bodyAsBuffer() : Buffer.buffer();
  }

  /**
   * Always safe: never null
   */
  @Override
  public Buffer bodyAsBuffer() {
    return safeBody;
  }

  /**
   * Consistent with bodyAsBuffer()
   */
  @Override
  public Buffer body() {
    return safeBody;
  }

  // ===== Alternate body representations =====

  @Override
  public String bodyAsString() {
    return safeBody.toString();
  }

  @Override
  public String bodyAsString(String encoding) {
    // Vert.x itself ignores encoding and always uses UTF-8 internally
    return safeBody.toString(StandardCharsets.UTF_8);
  }

  @Override
  public io.vertx.core.json.JsonObject bodyAsJsonObject() {
    return safeBody.length() > 0 ? new io.vertx.core.json.JsonObject(safeBody) : null;
  }

  @Override
  public io.vertx.core.json.JsonArray bodyAsJsonArray() {
    return safeBody.length() > 0 ? new io.vertx.core.json.JsonArray(safeBody) : null;
  }

  // ===== Delegate everything else to the original response =====

  @Override
  public HttpVersion version() {
    return delegate.version();
  }

  @Override
  public int statusCode() {
    return delegate.statusCode();
  }

  @Override
  public String statusMessage() {
    return delegate.statusMessage();
  }

  @Override
  public MultiMap headers() {
    return delegate.headers();
  }

  @Override
  public String getHeader(String headerName) {
    return delegate.getHeader(headerName);
  }

  @Override
  public String getHeader(CharSequence name) {
    return delegate.getHeader(name);
  }

  @Override
  public List<String> cookies() {
    return delegate.cookies();
  }

  @Override
  public MultiMap trailers() {
    return delegate.trailers();
  }

  @Override
  public @Nullable String getTrailer(String name) {
    return delegate.getTrailer(name);
  }

  @Override
  public List<String> followedRedirects() {
    return delegate.followedRedirects();
  }
}
