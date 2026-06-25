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
    c.getChildren().forEach(child -> collect(child, sb));
  }

  @Test
  @DisplayName("NAV constant is 'validate'")
  void navConstant() {
    assertEquals("validate", ValidationView.NAV);
  }

  // The view renders labels through i18n; an unattached component in the test
  // harness has no AppI18NProvider, so labels surface as their keys. We assert
  // on the result KEY (which result rendered) and the field VALUES (verbatim,
  // not translated) — both stable regardless of the active i18n provider.

  @Test
  @DisplayName("a valid credential renders the VALID result + the match fields, no status field/scores")
  void validShowsFields() {
    Credential c = issue();
    repo.save(c);

    String text = renderText(c.id().toString());

    assertTrue(text.contains("validate.result.valid"), "the VALID result");
    assertTrue(text.contains("Alice"), "recipient value");
    assertTrue(text.contains("Vaadin Certified"), "title value");
    assertTrue(text.contains("Open Probatum Academy"), "issuer value");
    assertTrue(text.contains(c.id().toString()), "credential id value");
    assertTrue(text.contains("validate.matchRule"), "the match-rule hint");
    assertFalse(text.contains("Status:"), "must not print a status field (§10.7)");
    assertFalse(text.toLowerCase().contains("score"), "must never expose scores (§11.3)");
  }

  @Test
  @DisplayName("a revoked credential renders the REVOKED result")
  void revokedShowsRevoked() {
    Credential c = issue().withStatus(CredentialStatus.REVOKED);
    repo.save(c);
    assertTrue(renderText(c.id().toString()).contains("validate.result.revoked"));
  }

  @Test
  @DisplayName("an unknown id renders the UNKNOWN result and no record")
  void unknownShowsUnknown() {
    String text = renderText(UUID.randomUUID().toString());
    assertTrue(text.contains("validate.result.unknown"));
    assertFalse(text.contains("Alice"));
  }

  @Test
  @DisplayName("a malformed id is treated as unknown (no crash)")
  void malformedIdIsUnknown() {
    assertTrue(renderText("not-a-uuid").contains("validate.result.unknown"));
  }

  @Test
  @DisplayName("exceeding the per-IP limit throttles the lookup and reveals no record (P010)")
  void throttlesAfterLimit() {
    ValidationRateLimiterProvider.setLimiter(limiter(2));
    Credential c = issue();
    repo.save(c);

    assertTrue(renderText(c.id().toString()).contains("validate.result.valid"), "1st allowed");
    assertTrue(renderText(c.id().toString()).contains("validate.result.valid"), "2nd allowed");
    String third = renderText(c.id().toString());
    assertTrue(third.contains("validate.throttled"), "3rd must be throttled");
    assertFalse(third.contains("Alice"), "a throttled lookup must not reveal the record");
  }
}
