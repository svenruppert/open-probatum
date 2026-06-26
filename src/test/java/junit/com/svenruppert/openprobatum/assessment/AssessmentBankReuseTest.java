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
import com.svenruppert.openprobatum.assessment.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Assessment ← question bank: reuse + version safety (P004)")
class AssessmentBankReuseTest {

  private static Question bankQuestion() {
    return Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.");
  }

  @Test
  @DisplayName("the same bank question reused in two assessments shares its lineage")
  void reuseAcrossAssessments() {
    Question shared = bankQuestion();

    Assessment a1 = Assessment.fromBank("Quiz A", 1, List.of(shared), 1.0);
    Assessment a2 = Assessment.fromBank("Quiz B", 1,
        List.of(shared, bankQuestion()), 0.5);

    assertTrue(a1.usesQuestionLineage(shared.lineageId()));
    assertTrue(a2.usesQuestionLineage(shared.lineageId()));
    assertTrue(a1.questionLineages().contains(shared.lineageId()));
    assertEquals(2, a2.questionLineages().size(), "two distinct logical questions");
  }

  @Test
  @DisplayName("a new bank version does not mutate an assessment that captured an earlier one")
  void versioningDoesNotFalsifyAttempts() {
    Question v1 = bankQuestion();
    Assessment assessment = Assessment.fromBank("Quiz", 1, List.of(v1), 1.0);

    // Author publishes a NEW version of the same logical question in the bank.
    Question v2 = v1.asNewVersion();
    assertEquals(2, v2.version());

    // The assessment still references v1 (the captured snapshot), not v2.
    Question embedded = assessment.questions().get(0);
    assertEquals(v1.id(), embedded.id(), "the assessment keeps the captured version");
    assertEquals(1, embedded.version());
    assertTrue(assessment.usesQuestionLineage(v1.lineageId()));

    // Grading against the captured v1 is unaffected by v2's existence.
    var result = assessment.grade(Map.of(v1.id(), Set.of(1)));
    assertTrue(result.passed());
    assertFalse(assessment.usesQuestionLineage(v2.id()), "v2's id is not a lineage of this assessment");
  }
}
