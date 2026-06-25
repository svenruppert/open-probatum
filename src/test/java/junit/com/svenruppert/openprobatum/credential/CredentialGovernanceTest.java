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
  @DisplayName("supersede → SUPERSEDED and records the replacing id")
  void supersede() {
    Credential c = issued();
    UUID replacement = UUID.randomUUID();
    governance.supersede(c.id(), replacement);

    assertEquals(ValidationResult.SUPERSEDED, pageResult(c.id()));
    assertEquals(replacement, repo.findById(c.id()).orElseThrow().superseder().orElseThrow());
  }

  @Test
  @DisplayName("an unknown id is a no-op")
  void unknownIsNoop() {
    assertTrue(governance.revoke(UUID.randomUUID()).isEmpty());
    assertTrue(repo.all().isEmpty());
  }
}
