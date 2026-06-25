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
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.CredentialValidator;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.credential.ValidationOutcome;
import com.svenruppert.openprobatum.credential.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialValidator — id lookup → the six computed results (P009)")
class CredentialValidatorTest {

  private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

  private final InMemoryCredentialRepository repo = new InMemoryCredentialRepository();
  private final CredentialValidator validator = new CredentialValidator(repo);

  private Credential issue(Instant expiry) {
    return Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Academy", Instant.parse("2026-01-01T00:00:00Z"), expiry);
  }

  private Credential saved(Credential c) {
    repo.save(c);
    return c;
  }

  @Test
  @DisplayName("a stored VALID credential validates as VALID and exposes the record")
  void validIsValid() {
    Credential c = saved(issue(null));
    ValidationOutcome o = validator.validate(c.id(), NOW);
    assertEquals(ValidationResult.VALID, o.result());
    assertEquals(c, o.credentialOpt().orElseThrow());
  }

  @Test
  @DisplayName("a past expiry validates as EXPIRED")
  void pastExpiryIsExpired() {
    Credential c = saved(issue(Instant.parse("2026-03-01T00:00:00Z")));
    assertEquals(ValidationResult.EXPIRED, validator.validate(c.id(), NOW).result());
  }

  @Test
  @DisplayName("revoked / suspended / superseded validate to their results")
  void storedStatusesMap() {
    Credential revoked = saved(issue(null).withStatus(CredentialStatus.REVOKED));
    Credential suspended = saved(issue(null).withStatus(CredentialStatus.SUSPENDED));
    Credential superseded = saved(issue(null).supersededByCredential(UUID.randomUUID()));

    assertEquals(ValidationResult.REVOKED, validator.validate(revoked.id(), NOW).result());
    assertEquals(ValidationResult.SUSPENDED, validator.validate(suspended.id(), NOW).result());
    assertEquals(ValidationResult.SUPERSEDED, validator.validate(superseded.id(), NOW).result());
  }

  @Test
  @DisplayName("an unknown id validates as UNKNOWN with no record")
  void unknownIsUnknown() {
    ValidationOutcome o = validator.validate(UUID.randomUUID(), NOW);
    assertEquals(ValidationResult.UNKNOWN, o.result());
    assertTrue(o.credentialOpt().isEmpty());
  }

  @Test
  @DisplayName("a null id validates as UNKNOWN (no NPE)")
  void nullIdIsUnknown() {
    assertEquals(ValidationResult.UNKNOWN, validator.validate(null, NOW).result());
  }
}
