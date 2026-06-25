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

import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.assessment.AssessmentResult;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import com.svenruppert.openprobatum.security.AppClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IssuanceService + IssuerIdentity — issue on pass (P006)")
class IssuanceServiceTest {

  private static final Instant FIXED = Instant.parse("2026-03-04T05:06:07Z");

  private InMemoryCredentialRepository repository;
  private IssuanceService service;

  @BeforeEach
  void setUp() {
    AppClock.setClock(Clock.fixed(FIXED, ZoneOffset.UTC));
    repository = new InMemoryCredentialRepository();
    service = new IssuanceService(repository, new IssuerIdentity("Test Academy", "http://x/validate"));
  }

  @AfterEach
  void tearDown() {
    AppClock.reset();
  }

  private static Attempt attempt(boolean passed) {
    return new Attempt(UUID.randomUUID(), "Alice", UUID.randomUUID(), 1,
        new AssessmentResult(passed ? 4 : 1, 4, passed ? 1.0 : 0.25, passed), FIXED);
  }

  @Test
  @DisplayName("a passed attempt issues + persists a VALID credential with issuer and recipient")
  void passIssuesAndPersists() {
    Optional<Credential> issued = service.issueFor(
        attempt(true), "Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE, null);

    assertTrue(issued.isPresent());
    Credential c = issued.get();
    assertEquals(CredentialStatus.VALID, c.status());
    assertEquals("Alice", c.recipientName());
    assertEquals("Test Academy", c.issuer());
    assertEquals(FIXED, c.issuedAt(), "issuedAt must come from AppClock");
    assertTrue(c.expiry().isEmpty());
    assertEquals(c, repository.findById(c.id()).orElseThrow(), "must be persisted");
  }

  @Test
  @DisplayName("a failed attempt issues nothing and persists nothing")
  void failIssuesNothing() {
    assertTrue(service.issueFor(
        attempt(false), "Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE, null).isEmpty());
    assertTrue(repository.all().isEmpty());
  }

  @Test
  @DisplayName("issuer builds the validation URL from the base + credential id")
  void issuerValidationUrl() {
    IssuerIdentity issuer = new IssuerIdentity("A", "http://host/validate");
    UUID id = UUID.randomUUID();
    assertEquals("http://host/validate/" + id, issuer.validationUrl(id));
  }

  @Test
  @DisplayName("fromConfig falls back to defaults when no properties are set")
  void fromConfigDefaults() {
    IssuerIdentity issuer = IssuerIdentity.fromConfig();
    assertEquals("Open Probatum Academy", issuer.name());
  }
}
