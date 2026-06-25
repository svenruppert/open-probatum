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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
