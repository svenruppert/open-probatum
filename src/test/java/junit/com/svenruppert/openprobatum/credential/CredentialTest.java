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
import com.svenruppert.openprobatum.credential.EffectiveStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Credential — record, random id, three-layer status (P002)")
class CredentialTest {

  private static final Instant ISSUED = Instant.parse("2026-01-01T00:00:00Z");

  private static Credential issue(Instant expiresAt) {
    return Credential.issue("Vaadin Basics", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Open Probatum Academy", ISSUED, expiresAt);
  }

  @Test
  @DisplayName("issue() creates a VALID credential with a random UUIDv4 id")
  void issueIsValidWithV4Id() {
    Credential c = issue(null);
    assertEquals(CredentialStatus.VALID, c.status());
    assertEquals(4, c.id().version(), "the id must be a UUID version 4 (random, non-enumerable)");
    assertNull(c.supersededBy());
    assertTrue(c.expiry().isEmpty());
  }

  @Test
  @DisplayName("ids are unique across issues — not enumerable")
  void idsAreUnique() {
    assertNotEquals(issue(null).id(), issue(null).id());
  }

  @Test
  @DisplayName("a VALID credential with no expiry is effectively VALID forever")
  void noExpiryStaysValid() {
    assertEquals(EffectiveStatus.VALID,
        issue(null).effectiveStatusAt(Instant.parse("2099-12-31T00:00:00Z")));
  }

  @Test
  @DisplayName("a future expiry is still effectively VALID before it passes")
  void futureExpiryIsValid() {
    Credential c = issue(Instant.parse("2030-01-01T00:00:00Z"));
    assertEquals(EffectiveStatus.VALID, c.effectiveStatusAt(Instant.parse("2027-06-01T00:00:00Z")));
  }

  @Test
  @DisplayName("a passed expiry computes EXPIRED (level 2, never stored)")
  void pastExpiryIsExpired() {
    Credential c = issue(Instant.parse("2026-06-01T00:00:00Z"));
    assertEquals(EffectiveStatus.EXPIRED, c.effectiveStatusAt(Instant.parse("2026-07-01T00:00:00Z")));
    // The stored status is unchanged — EXPIRED is computed, not persisted.
    assertEquals(CredentialStatus.VALID, c.status());
  }

  @Test
  @DisplayName("REVOKED / SUSPENDED outrank an expiry date")
  void storedStatusOutranksExpiry() {
    Instant past = Instant.parse("2026-06-01T00:00:00Z");
    Instant after = Instant.parse("2026-07-01T00:00:00Z");
    assertEquals(EffectiveStatus.REVOKED,
        issue(past).withStatus(CredentialStatus.REVOKED).effectiveStatusAt(after));
    assertEquals(EffectiveStatus.SUSPENDED,
        issue(past).withStatus(CredentialStatus.SUSPENDED).effectiveStatusAt(after));
  }

  @Test
  @DisplayName("supersededByCredential marks SUPERSEDED and records the replacing id")
  void supersedeMarksAndLinks() {
    UUID replacement = UUID.randomUUID();
    Credential c = issue(null).supersededByCredential(replacement);
    assertEquals(CredentialStatus.SUPERSEDED, c.status());
    assertEquals(EffectiveStatus.SUPERSEDED, c.effectiveStatusAt(ISSUED));
    assertEquals(replacement, c.superseder().orElseThrow());
  }

  @Test
  @DisplayName("withStatus produces an updated copy, leaving the id intact")
  void withStatusKeepsId() {
    Credential c = issue(null);
    Credential revoked = c.withStatus(CredentialStatus.REVOKED);
    assertEquals(c.id(), revoked.id());
    assertEquals(CredentialStatus.REVOKED, revoked.status());
    assertEquals(CredentialStatus.VALID, c.status(), "the original record is immutable");
  }

  @Test
  @DisplayName("a null required field is rejected at construction")
  void nullFieldRejected() {
    assertThrows(NullPointerException.class, () -> Credential.issue(
        null, CredentialType.MANUAL_CREDENTIAL, "Bob", "Academy", ISSUED, null));
  }
}
