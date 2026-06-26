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

package junit.com.svenruppert.openprobatum.assessment;

import com.svenruppert.openprobatum.assessment.Assessment;
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.assessment.CheckService;
import com.svenruppert.openprobatum.assessment.InMemoryAttemptRepository;
import com.svenruppert.openprobatum.assessment.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CheckService — graded check + counted attempts (P011)")
class CheckServiceTest {

  private final Question q1 = Question.singleChoice("1+1?", List.of("1", "2"), 1);
  private final Question q2 = Question.singleChoice("2+2?", List.of("3", "4"), 1);
  // pass threshold 1.0 → both must be right
  private final Assessment assessment =
      new Assessment(UUID.randomUUID(), "Quiz", 1, List.of(q1, q2), 1.0);

  private InMemoryAttemptRepository repo;
  private CheckService service;

  @BeforeEach
  void setUp() {
    repo = new InMemoryAttemptRepository();
    service = new CheckService(repo);
  }

  private Map<UUID, Set<Integer>> answers(int a1, int a2) {
    return Map.of(q1.id(), Set.of(a1), q2.id(), Set.of(a2));
  }

  @Test
  @DisplayName("all-correct answers pass; a wrong answer fails the threshold")
  void gradingReflectsThreshold() {
    Attempt pass = service.submit("alice", assessment, answers(1, 1));
    assertTrue(pass.passed());
    assertEquals(2, pass.result().correct());

    Attempt fail = service.submit("alice", assessment, answers(1, 0));
    assertFalse(fail.passed());
    assertEquals(1, fail.result().correct());
  }

  @Test
  @DisplayName("attempts accrue and are counted per learner")
  void attemptsAreCounted() {
    service.submit("alice", assessment, answers(0, 0));
    service.submit("alice", assessment, answers(1, 0));
    service.submit("alice", assessment, answers(1, 1));
    assertEquals(3, service.attemptCount("alice", assessment.id()));

    // a different learner's count is independent
    assertEquals(0, service.attemptCount("bob", assessment.id()));
    service.submit("bob", assessment, answers(1, 1));
    assertEquals(1, service.attemptCount("bob", assessment.id()));
  }

  @Test
  @DisplayName("the recorded attempt carries the assessment id + version")
  void attemptCarriesAssessmentRef() {
    Attempt a = service.submit("alice", assessment, answers(1, 1));
    assertEquals(assessment.id(), a.assessmentId());
    assertEquals(assessment.version(), a.assessmentVersion());
    assertEquals(1, repo.forLearner("alice", assessment.id()).size());
  }
}
