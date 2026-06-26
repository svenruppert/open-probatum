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

package junit.com.svenruppert.openprobatum.lab;

import com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository;
import com.svenruppert.openprobatum.lab.LabSubmission;
import com.svenruppert.openprobatum.lab.SubmissionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LabSubmission — practical evidence record + repository (P004)")
class LabSubmissionTest {

  private static LabSubmission submitted(Long learner) {
    return LabSubmission.submit(UUID.randomUUID(), 2, learner, "Ada",
        "I deployed the WAR and captured the boot log.", "https://example.test/log");
  }

  @Test
  @DisplayName("a fresh submission is SUBMITTED, pins the lab version, and is undecided")
  void freshSubmission() {
    LabSubmission s = submitted(1001L);
    assertEquals(SubmissionStatus.SUBMITTED, s.status());
    assertEquals(2, s.labVersion());
    assertEquals(1001L, s.recipientId());
    assertFalse(s.isDecided());
    assertFalse(s.isVerified());
    assertTrue(s.decidedAtOpt().isEmpty());
    assertEquals("https://example.test/log", s.artefactLinkOpt().orElseThrow());
    assertTrue(s.isHeldBy(1001L));
  }

  @Test
  @DisplayName("verified() marks VERIFIED + stamps a decision time; rejected() needs feedback")
  void decisions() {
    LabSubmission verified = submitted(1L).verified("Nice work");
    assertEquals(SubmissionStatus.VERIFIED, verified.status());
    assertTrue(verified.isVerified());
    assertTrue(verified.isDecided());
    assertTrue(verified.decidedAtOpt().isPresent());
    assertEquals("Nice work", verified.assessorFeedback());

    LabSubmission rejected = submitted(1L).rejected("Missing the boot log");
    assertEquals(SubmissionStatus.REJECTED, rejected.status());
    assertEquals("Missing the boot log", rejected.assessorFeedback());
    assertThrows(IllegalArgumentException.class, () -> submitted(1L).rejected("  "));
  }

  @Test
  @DisplayName("a blank write-up and a sub-1 lab version are rejected")
  void validation() {
    assertThrows(IllegalArgumentException.class,
        () -> LabSubmission.submit(UUID.randomUUID(), 1, 1L, "Ada", "  ", null));
    assertThrows(IllegalArgumentException.class,
        () -> LabSubmission.submit(UUID.randomUUID(), 0, 1L, "Ada", "did it", null));
  }

  @Test
  @DisplayName("the repository filters by learner (own-data), pending and lab")
  void repository() {
    InMemoryLabSubmissionRepository repo = new InMemoryLabSubmissionRepository();
    UUID labId = UUID.randomUUID();
    LabSubmission mine = LabSubmission.submit(labId, 1, 1001L, "Ada", "did it", null);
    LabSubmission other = LabSubmission.submit(labId, 1, 2002L, "Bob", "did it", null);
    LabSubmission decided = mine.verified("");
    repo.save(other);
    repo.save(decided);

    assertEquals(1, repo.forLearner(1001L).size(), "only my submissions");
    assertEquals(1001L, repo.forLearner(1001L).get(0).recipientId());
    assertEquals(1, repo.pending().size(), "only Bob's is still SUBMITTED");
    assertEquals(2002L, repo.pending().get(0).recipientId());
    assertEquals(2, repo.forLab(labId).size(), "both target the lab");
  }
}
