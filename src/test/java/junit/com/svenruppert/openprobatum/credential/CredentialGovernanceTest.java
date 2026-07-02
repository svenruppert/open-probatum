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

package junit.com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialGovernance;
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.CredentialValidator;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.credential.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialGovernance — revoke/suspend/supersede reflect immediately (P011)")
class CredentialGovernanceTest {

  private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

  private final InMemoryCredentialRepository repo = new InMemoryCredentialRepository();
  private final CredentialGovernance governance = new CredentialGovernance(repo);
  private final CredentialValidator validator = new CredentialValidator(repo);

  @org.junit.jupiter.api.BeforeEach
  void pinAuditTrail() {
    // Keep the governance audit-trail side effect in memory (no file store).
    com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository());
  }

  @org.junit.jupiter.api.AfterEach
  void resetAuditTrail() {
    com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider.reset();
  }

  private Credential issued() {
    Credential c = Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Academy", Instant.parse("2026-01-01T00:00:00Z"), null);
    repo.save(c);
    return c;
  }

  private ValidationResult pageResult(UUID id) {
    return validator.validate(id, NOW).result();
  }

  @Test
  @DisplayName("revoke flips the page result to REVOKED immediately")
  void revokeIsImmediate() {
    Credential c = issued();
    assertEquals(ValidationResult.VALID, pageResult(c.id()));

    governance.revoke(c.id());

    assertEquals(ValidationResult.REVOKED, pageResult(c.id()));
    assertEquals(CredentialStatus.REVOKED, repo.findById(c.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("suspend → SUSPENDED, reinstate → VALID")
  void suspendThenReinstate() {
    Credential c = issued();
    governance.suspend(c.id());
    assertEquals(ValidationResult.SUSPENDED, pageResult(c.id()));
    governance.reinstate(c.id());
    assertEquals(ValidationResult.VALID, pageResult(c.id()));
  }

  @Test
  @DisplayName("illegal source-state transitions are refused — no resurrecting a terminal credential (P022)")
  void illegalTransitionsAreRefused() {
    Credential c = issued();
    governance.revoke(c.id()); // now REVOKED (terminal)

    // A REVOKED credential must not be reinstatable, re-suspendable or supersedable.
    assertTrue(governance.reinstate(c.id()).isEmpty(), "a revoked credential cannot be reinstated");
    assertTrue(governance.suspend(c.id()).isEmpty(), "a revoked credential cannot be suspended");
    assertTrue(governance.supersede(c.id(), UUID.randomUUID()).isEmpty(),
        "a revoked credential cannot be superseded");
    assertEquals(CredentialStatus.REVOKED, repo.findById(c.id()).orElseThrow().status(),
        "the status stays REVOKED");

    // reinstate is only valid from SUSPENDED: a VALID credential is not reinstatable.
    Credential valid = issued();
    assertTrue(governance.reinstate(valid.id()).isEmpty(), "a VALID credential is already valid");
    assertEquals(CredentialStatus.VALID, repo.findById(valid.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("supersede → SUPERSEDED and records the replacing id")
  void supersede() {
    Credential c = issued();
    UUID replacement = UUID.randomUUID();
    governance.supersede(c.id(), replacement);

    assertEquals(ValidationResult.SUPERSEDED, pageResult(c.id()));
    assertEquals(replacement, repo.findById(c.id()).orElseThrow().superseder().orElseThrow());
  }

  @Test
  @DisplayName("re-issue mints a VALID successor and supersedes the predecessor; both stay findable (P009)")
  void reissueRenews() {
    Credential predecessor = Credential.issue("Vaadin Certified",
        CredentialType.COMPLETION_CERTIFICATE, 1001L, "Alice", "Academy",
        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
        com.svenruppert.openprobatum.credential.Evidence.manualAward());
    repo.save(predecessor);

    Instant newExpiry = Instant.parse("2028-01-01T00:00:00Z");
    Credential successor = governance.reissue(predecessor.id(), newExpiry).orElseThrow();

    // The successor is a fresh VALID credential carrying the same recipient + new expiry.
    assertEquals(CredentialStatus.VALID, successor.status());
    assertTrue(successor.id() != predecessor.id() && !successor.id().equals(predecessor.id()));
    assertEquals(1001L, successor.recipientId(), "the recipient id carries over");
    assertEquals(newExpiry, successor.expiry().orElseThrow());

    // The predecessor is now SUPERSEDED, pointing at the successor — and still findable.
    Credential reloadedPredecessor = repo.findById(predecessor.id()).orElseThrow();
    assertEquals(CredentialStatus.SUPERSEDED, reloadedPredecessor.status());
    assertEquals(successor.id(), reloadedPredecessor.superseder().orElseThrow());
    assertEquals(ValidationResult.SUPERSEDED, pageResult(predecessor.id()));
    assertEquals(ValidationResult.VALID, pageResult(successor.id()), "both are findable");
  }

  @Test
  @DisplayName("an unknown id is a no-op")
  void unknownIsNoop() {
    assertTrue(governance.revoke(UUID.randomUUID()).isEmpty());
    assertTrue(governance.reissue(UUID.randomUUID(), null).isEmpty());
    assertTrue(repo.all().isEmpty());
  }
}
