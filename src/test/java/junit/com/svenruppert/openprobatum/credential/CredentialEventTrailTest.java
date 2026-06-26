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

import com.svenruppert.openprobatum.assessment.AssessmentResult;
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialEvent;
import com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialGovernance;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Credential audit trail — one event per operation (P011, §17.3)")
class CredentialEventTrailTest {

  private InMemoryCredentialRepository credentials;
  private InMemoryCredentialEventRepository events;
  private CredentialGovernance governance;
  private IssuanceService issuance;

  @BeforeEach
  void setUp() {
    credentials = new InMemoryCredentialRepository();
    events = new InMemoryCredentialEventRepository();
    CredentialEventRepositoryProvider.setRepository(events);
    governance = new CredentialGovernance(credentials);
    issuance = new IssuanceService(credentials, new IssuerIdentity("Academy", "http://h/validate"));
  }

  @AfterEach
  void tearDown() {
    CredentialEventRepositoryProvider.reset();
  }

  private static Attempt passed() {
    return new Attempt(UUID.randomUUID(), "Alice", UUID.randomUUID(), 1,
        new AssessmentResult(4, 4, 1.0, true), java.time.Instant.parse("2026-06-01T00:00:00Z"));
  }

  private Credential issue() {
    return issuance.issueFor(passed(), 1001L, "Vaadin Certified",
        CredentialType.COMPLETION_CERTIFICATE, null).orElseThrow();
  }

  @Test
  @DisplayName("issuance appends exactly one ISSUED event for the credential")
  void issuanceLogsOne() {
    Credential c = issue();
    List<CredentialEvent> trail = events.findByCredential(c.id());
    assertEquals(1, trail.size());
    assertEquals(CredentialEvent.Action.ISSUED, trail.get(0).action());
    assertEquals(c.id(), trail.get(0).credentialId());
  }

  @Test
  @DisplayName("each governance action appends exactly one matching event")
  void governanceLogsOnePerAction() {
    Credential c = issue();                          // ISSUED
    governance.suspend(c.id());                       // SUSPENDED
    governance.reinstate(c.id());                     // REINSTATED
    governance.revoke(c.id());                        // REVOKED

    List<CredentialEvent> trail = events.findByCredential(c.id());
    assertEquals(4, trail.size(), "one event per operation, none missing or doubled");
    // Newest first.
    assertEquals(CredentialEvent.Action.REVOKED, trail.get(0).action());
    assertEquals(CredentialEvent.Action.REINSTATED, trail.get(1).action());
    assertEquals(CredentialEvent.Action.SUSPENDED, trail.get(2).action());
    assertEquals(CredentialEvent.Action.ISSUED, trail.get(3).action());
    assertTrue(trail.stream().allMatch(e -> "credential-manager".equals(e.actor())
        || "system".equals(e.actor())));
  }

  @Test
  @DisplayName("supersede records SUPERSEDED; re-issue records REISSUED on the predecessor")
  void supersedeAndReissueLog() {
    Credential c = issue();
    UUID replacement = UUID.randomUUID();
    governance.supersede(c.id(), replacement);
    assertEquals(CredentialEvent.Action.SUPERSEDED,
        events.findByCredential(c.id()).get(0).action());

    Credential predecessor = issue();
    governance.reissue(predecessor.id(), null);
    assertEquals(CredentialEvent.Action.REISSUED,
        events.findByCredential(predecessor.id()).get(0).action());
  }

  @Test
  @DisplayName("a no-op governance call on an unknown id appends nothing")
  void noopLogsNothing() {
    governance.revoke(UUID.randomUUID());
    assertTrue(events.all().isEmpty());
  }
}
