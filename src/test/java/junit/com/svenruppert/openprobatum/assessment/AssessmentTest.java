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
import com.svenruppert.openprobatum.assessment.AssessmentResult;
import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Assessment — grading + pass threshold (P005)")
class AssessmentTest {

  private final Question q1 = Question.singleChoice("1+1?", List.of("1", "2", "3"), 1);
  private final Question q2 = Question.singleChoice("Sky colour?", List.of("Blue", "Green"), 0);
  private final Question q3 = Question.singleChoice("2*2?", List.of("3", "4"), 1);
  private final Question q4 = Question.multipleChoice("Even numbers?",
      List.of("1", "2", "3", "4"), Set.of(1, 3), "2 and 4 are even.");

  /** 4 questions, pass at 75% (3 of 4). */
  private Assessment assessment() {
    return new Assessment(UUID.randomUUID(), "Basics", 1, List.of(q1, q2, q3, q4), 0.75);
  }

  private static Map<UUID, Set<Integer>> answers(Question q, int... picks) {
    Map<UUID, Set<Integer>> m = new HashMap<>();
    Set<Integer> set = new java.util.HashSet<>();
    for (int p : picks) {
      set.add(p);
    }
    m.put(q.id(), set);
    return m;
  }

  @Test
  @DisplayName("all correct → score 1.0, passed")
  void allCorrect() {
    Map<UUID, Set<Integer>> a = new HashMap<>();
    a.put(q1.id(), Set.of(1));
    a.put(q2.id(), Set.of(0));
    a.put(q3.id(), Set.of(1));
    a.put(q4.id(), Set.of(1, 3));
    AssessmentResult r = assessment().grade(a);
    assertEquals(4, r.correct());
    assertEquals(1.0, r.score());
    assertTrue(r.passed());
  }

  @Test
  @DisplayName("exactly at the threshold (3 of 4) passes")
  void atThresholdPasses() {
    Map<UUID, Set<Integer>> a = new HashMap<>();
    a.put(q1.id(), Set.of(1));
    a.put(q2.id(), Set.of(0));
    a.put(q3.id(), Set.of(1));
    a.put(q4.id(), Set.of(0)); // wrong
    AssessmentResult r = assessment().grade(a);
    assertEquals(3, r.correct());
    assertTrue(r.passed());
  }

  @Test
  @DisplayName("below the threshold (2 of 4) fails")
  void belowThresholdFails() {
    Map<UUID, Set<Integer>> a = new HashMap<>();
    a.put(q1.id(), Set.of(1));
    a.put(q2.id(), Set.of(0));
    // q3, q4 unanswered → wrong
    AssessmentResult r = assessment().grade(a);
    assertEquals(2, r.correct());
    assertFalse(r.passed());
  }

  @Test
  @DisplayName("a multiple-choice question with a missing pick is wrong (exact match required)")
  void multipleChoiceNeedsExactMatch() {
    assertFalse(q4.isCorrect(Set.of(1)));        // missing 3
    assertFalse(q4.isCorrect(Set.of(1, 3, 0)));  // extra 0
    assertTrue(q4.isCorrect(Set.of(1, 3)));
  }

  @Test
  @DisplayName("an unanswered question grades as wrong (null/empty)")
  void unansweredIsWrong() {
    assertFalse(q1.isCorrect(null));
    assertFalse(q1.isCorrect(Set.of()));
  }

  @Test
  @DisplayName("invalid construction is rejected")
  void invalidConstruction() {
    assertThrows(IllegalArgumentException.class,
        () -> Question.singleChoice("x", List.of("a", "b"), 5)); // index out of range
    assertThrows(IllegalArgumentException.class, () -> {
      UUID qid = UUID.randomUUID();
      new Question(qid, qid, 1, com.svenruppert.openprobatum.content.ContentStatus.DRAFT,
          "x", QuestionType.SINGLE_CHOICE, List.of("a", "b"), Set.of(0, 1), "", "", "",
          Difficulty.MEDIUM, Set.of()); // single-choice with two correct
    });
    assertThrows(IllegalArgumentException.class,
        () -> new Assessment(UUID.randomUUID(), "t", 1, List.of(q1), 1.5)); // bad threshold
    assertThrows(IllegalArgumentException.class,
        () -> new Assessment(UUID.randomUUID(), "t", 1, List.of(), 0.5)); // no questions
  }
}
