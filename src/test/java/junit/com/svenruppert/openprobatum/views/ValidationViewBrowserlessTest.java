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

package junit.com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.credential.ValidationRateLimiter;
import com.svenruppert.openprobatum.credential.ValidationRateLimiterProvider;
import com.svenruppert.openprobatum.views.ValidationView;
import com.svenruppert.jsentinel.authorization.api.JSentinelServiceResolver;
import com.svenruppert.jsentinel.ratelimiting.InMemoryRateLimitPolicy;
import com.svenruppert.jsentinel.ratelimiting.InMemoryRateLimitStore;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ValidationView — public verification portal renders results + match fields (P009)")
class ValidationViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCredentialRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryCredentialRepository();
    CredentialRepositoryProvider.setRepository(repo);
    // A generous per-test limiter so the rendering tests are not throttled by
    // the process-wide singleton (each renderText() is an "anon"-IP lookup).
    ValidationRateLimiterProvider.setLimiter(limiter(100));
  }

  @AfterEach
  void tearDown() {
    CredentialRepositoryProvider.reset();
    ValidationRateLimiterProvider.reset();
  }

  private static ValidationRateLimiter limiter(int limit) {
    return new ValidationRateLimiter(new InMemoryRateLimitPolicy(
        new InMemoryRateLimitStore(),
        JSentinelServiceResolver.securityAuditService(),
        limit, Duration.ofMinutes(1)));
  }

  private static Credential issue() {
    return Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Open Probatum Academy", Instant.parse("2026-01-01T00:00:00Z"), null);
  }

  private static String renderText(String idParam) {
    ValidationView view = new ValidationView();
    view.setParameter(null, idParam);
    StringBuilder sb = new StringBuilder();
    collect(view.getContent(), sb);
    return sb.toString();
  }

  private static void collect(Component c, StringBuilder sb) {
    if (c instanceof HasText t) {
      sb.append(t.getText()).append(" | ");
    }
    String result = c.getElement().getAttribute("data-result");
    if (result != null) {
      sb.append("[result:").append(result).append("] ");
    }
    c.getChildren().forEach(child -> collect(child, sb));
  }

  @Test
  @DisplayName("NAV constant is 'validate'")
  void navConstant() {
    assertEquals("validate", ValidationView.NAV);
  }

  // Result labels are i18n + locale-dependent, so we assert on the stable
  // data-result marker (the enum name) for "which result" and on the field
  // VALUES (verbatim, not translated) for the record content.

  @Test
  @DisplayName("a valid credential renders the VALID result + the match fields, no status field/scores")
  void validShowsFields() {
    Credential c = issue();
    repo.save(c);

    String text = renderText(c.id().toString());

    assertTrue(text.contains("[result:VALID]"), "the VALID result");
    assertTrue(text.contains("Alice"), "recipient value");
    assertTrue(text.contains("Vaadin Certified"), "title value");
    assertTrue(text.contains("Open Probatum Academy"), "issuer value");
    assertTrue(text.contains(c.id().toString()), "credential id value");
    assertFalse(text.contains("Status:"), "must not print a status field (§10.7)");
    assertFalse(text.toLowerCase().contains("score"), "must never expose scores (§11.3)");
  }

  @Test
  @DisplayName("the issued date renders as the UTC calendar date, matching the PDF (M1)")
  void issuedDateIsUtcCalendarDate() {
    // Issued at exactly 2026-01-01T00:00:00Z: a host in any negative UTC offset
    // would slip to 2025-12-31 if the page used the system zone instead of UTC.
    Credential c = issue();
    repo.save(c);

    String text = renderText(c.id().toString());

    assertTrue(text.contains("2026-01-01"),
        "the public page must show the UTC calendar date (same as the PDF)");
    assertFalse(text.contains("2025-12-31"),
        "the issued date must not drift to the host's local zone");
  }

  @Test
  @DisplayName("a revoked credential renders the REVOKED result")
  void revokedShowsRevoked() {
    Credential c = issue().withStatus(CredentialStatus.REVOKED);
    repo.save(c);
    assertTrue(renderText(c.id().toString()).contains("[result:REVOKED]"));
  }

  @Test
  @DisplayName("an unknown id renders the UNKNOWN result and no record")
  void unknownShowsUnknown() {
    String text = renderText(UUID.randomUUID().toString());
    assertTrue(text.contains("[result:UNKNOWN]"));
    assertFalse(text.contains("Alice"));
  }

  @Test
  @DisplayName("a malformed id is treated as unknown (no crash)")
  void malformedIdIsUnknown() {
    assertTrue(renderText("not-a-uuid").contains("[result:UNKNOWN]"));
  }

  @Test
  @DisplayName("exceeding the per-IP limit throttles the lookup and reveals no record (P010)")
  void throttlesAfterLimit() {
    ValidationRateLimiterProvider.setLimiter(limiter(2));
    Credential c = issue();
    repo.save(c);

    assertTrue(renderText(c.id().toString()).contains("[result:VALID]"), "1st allowed");
    assertTrue(renderText(c.id().toString()).contains("[result:VALID]"), "2nd allowed");
    String third = renderText(c.id().toString());
    assertTrue(third.contains("[result:THROTTLED]"), "3rd must be throttled");
    assertFalse(third.contains("Alice"), "a throttled lookup must not reveal the record");
  }
}
