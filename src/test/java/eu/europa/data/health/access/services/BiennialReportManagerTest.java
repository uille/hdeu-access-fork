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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import static org.mockito.Mockito.*;

class BiennialReportManagerTest {

  private BiennialReportManager manager;
  private RoutingContext context;
  private HttpServerResponse response;

  @BeforeEach
  void setUp() {
    manager = new BiennialReportManager();
    context = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);

    // Make fluent API work
    when(context.response()).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
  }

  @Test
  void testDownloadBiennialReportTemplateExists() throws Exception {
    // Arrange: ensure the resource exists in test/resources
    InputStream template = getClass().getResourceAsStream("/templates/Biennial Report template_NZP.docx");
    assertNotNull(template, "Test requires Biennial Report template in test/resources/templates/");
    template.close();

    // Act
    manager.downloadBiennialReportDocument(context);

    // Assert
    verify(response).putHeader(eq("Content-Disposition"),
      contains("TEHDAS2_Task6.3_Biennial_report_template.docx"));
    verify(response).putHeader(eq("Content-Type"),
      eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

    // Capture Buffer content
    ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
    verify(response).end(bufferCaptor.capture());

    Buffer writtenBuffer = bufferCaptor.getValue();
    assertNotNull(writtenBuffer);
    assertTrue(writtenBuffer.length() > 0, "Generated document should not be empty");
  }
  @Test
  void testTemplateMissing_returns500WithMessage() throws Exception {

    when(response.setStatusCode(anyInt())).thenReturn(response);

    URL mainClasses = Paths.get("target", "classes").toUri().toURL(); // Gradle: build/classes/java/main
    String blocked = "templates/Biennial Report template_NZP.docx";
    try (BlockingResourceClassLoader cl =
           new BlockingResourceClassLoader(new URL[]{mainClasses}, ClassLoader.getSystemClassLoader(), blocked)) {

      Class<?> mgrClass = cl.loadClass(BiennialReportManager.class.getName());
      Object mgr = mgrClass.getDeclaredConstructor().newInstance();

      Method m = mgrClass.getMethod("downloadBiennialReportDocument", RoutingContext.class);
      m.invoke(mgr, context);

      // Assert 500 + JSON body
      verify(response).setStatusCode(500);
      ArgumentCaptor<String> msgCap = ArgumentCaptor.forClass(String.class);
      verify(response).end(msgCap.capture());
      String body = msgCap.getValue();
      assertNotNull(body);
      assertTrue(body.contains("\"result_message\""));
      assertTrue(body.contains("Biennial Report Template not found"));
    }
  }

  @Test
  void testDocxLoadThrows_returns500() throws Exception {
    // Ensure the resource exists so we pass the null check.
    InputStream template = getClass().getResourceAsStream("/templates/Biennial Report template_NZP.docx");
    assertNotNull(template, "Requires /templates/Biennial Report template_NZP.docx in test resources");
    template.close();

    try (var mocked = mockStatic(WordprocessingMLPackage.class)) {
      mocked.when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
        .thenThrow(new RuntimeException("boom"));

      manager.downloadBiennialReportDocument(context);

      verify(response).setStatusCode(500);
      verify(response).end(); // no body in catch branch
    }
  }

  // ---------------- helper classloader ----------------
  /**
   * A child-first URLClassLoader that looks in its own URLs for classes/resources
   * before delegating to the parent. We intentionally DO NOT delegate resources to parent
   * so test resources (like the template file) are hidden from this loader.
   */
  static class ParentLastURLClassLoader extends URLClassLoader {
    ParentLastURLClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
    }
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      // Always let java.* go first
      if (name.startsWith("java.")) {
        return super.loadClass(name, resolve);
      }
      // Try to find class in child first
      try {
        Class<?> c = findLoadedClass(name);
        if (c == null) c = findClass(name);
        if (resolve) resolveClass(c);
        return c;
      } catch (ClassNotFoundException e) {
        // Delegate to parent if not found in child
        return super.loadClass(name, resolve);
      }
    }
    @Override
    public URL getResource(String name) {
      // Child-first for resources; DO NOT fall back to parent to hide test resources
      URL url = findResource(name);
      return url; // may be null (desired for this test)
    }
    @Override
    public InputStream getResourceAsStream(String name) {
      URL url = getResource(name);
      try {
        return (url != null) ? url.openStream() : null;
      } catch (Exception e) {
        return null;
      }
    }
  }
  static class BlockingResourceClassLoader extends URLClassLoader {
    private final String blocked;

    BlockingResourceClassLoader(URL[] urls, ClassLoader parent, String blockedPath) {
      super(urls, parent);
      // normalize: ClassLoader#getResource gets names without leading slash
      this.blocked = blockedPath.startsWith("/") ? blockedPath.substring(1) : blockedPath;
    }

    @Override
    public URL getResource(String name) {
      String norm = name.startsWith("/") ? name.substring(1) : name;
      if (norm.equals(blocked)) {
        return null; // explicitly hide the template resource
      }
      return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
      URL url = getResource(name);
      try { return url != null ? url.openStream() : null; }
      catch (Exception e) { return null; }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      // Load the manager class in THIS loader so getClass().getResourceAsStream uses our overrides
      if (name.equals(BiennialReportManager.class.getName())) {
        Class<?> c = findLoadedClass(name);
        if (c == null) c = findClass(name);
        if (resolve) resolveClass(c);
        return c;
      }
      // Everything else: delegate to parent normally
      return super.loadClass(name, resolve);
    }
  }
}
