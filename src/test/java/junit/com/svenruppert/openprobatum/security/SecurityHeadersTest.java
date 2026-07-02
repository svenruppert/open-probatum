/*
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the EUPL, Version 1.2 (the "Licence");
 * you may not use this file except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package junit.com.svenruppert.openprobatum.security;

import com.svenruppert.openprobatum.security.SecurityHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SecurityHeaders — baseline response hardening")
class SecurityHeadersTest {

  @Test
  @DisplayName("applies CSP, nosniff, Referrer-Policy and X-Frame-Options")
  void appliesBaselineHeaders() {
    Map<String, String> h = new HashMap<>();
    SecurityHeaders.apply(h::put, false);

    assertEquals("frame-ancestors 'none'; object-src 'none'; base-uri 'self'",
        h.get(SecurityHeaders.CSP));
    assertEquals("nosniff", h.get(SecurityHeaders.CONTENT_TYPE_OPTIONS));
    assertEquals("strict-origin-when-cross-origin", h.get(SecurityHeaders.REFERRER_POLICY));
    assertEquals("DENY", h.get(SecurityHeaders.FRAME_OPTIONS));
  }

  @Test
  @DisplayName("the SecurityHeadersFilter declares async-supported so @Push long-polling works (P017)")
  void filterIsAsyncSupported() throws Exception {
    // The filter sits in front of the async Vaadin servlet (@Push). Without an
    // async-supported declaration, startAsync() throws once push falls back to
    // long-polling. web.xml filters default to async-unsupported, so it must be
    // explicit.
    Path webXml = Path.of("src/main/webapp/WEB-INF/web.xml");
    assertTrue(Files.exists(webXml), "web.xml must exist at " + webXml.toAbsolutePath());
    String xml = Files.readString(webXml).replaceAll("\\s+", " ");
    int filterStart = xml.indexOf("<filter>");
    int filterEnd = xml.indexOf("</filter>");
    assertTrue(filterStart >= 0 && filterEnd > filterStart, "a <filter> block must exist");
    String filterBlock = xml.substring(filterStart, filterEnd);
    assertTrue(filterBlock.contains("SecurityHeadersFilter"),
        "the first filter block is the SecurityHeadersFilter");
    assertTrue(filterBlock.contains("<async-supported>true</async-supported>"),
        "the SecurityHeadersFilter must declare <async-supported>true</async-supported>");
  }

  @Test
  @DisplayName("no HSTS over plain HTTP")
  void noHstsOverPlainHttp() {
    Map<String, String> h = new HashMap<>();
    SecurityHeaders.apply(h::put, false);
    assertFalse(h.containsKey(SecurityHeaders.HSTS), "HSTS is meaningless over HTTP");
  }

  @Test
  @DisplayName("HSTS is set only over HTTPS")
  void hstsOnlyWhenSecure() {
    Map<String, String> h = new HashMap<>();
    SecurityHeaders.apply(h::put, true);
    assertEquals("max-age=31536000; includeSubDomains", h.get(SecurityHeaders.HSTS));
  }

  @Test
  @DisplayName("the CSP does not constrain script-src/style-src (Vaadin compatibility)")
  void cspDoesNotBreakVaadin() {
    Map<String, String> h = new HashMap<>();
    SecurityHeaders.apply(h::put, true);
    String csp = h.get(SecurityHeaders.CSP);
    assertFalse(csp.contains("script-src"), "a script-src would break Vaadin's inline bootstrap");
    assertFalse(csp.contains("style-src"), "a style-src would break Vaadin's inline styles");
  }
}
