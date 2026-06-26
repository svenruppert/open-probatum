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
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.assessment.InMemoryAssessmentRepository;
import com.svenruppert.openprobatum.assessment.InMemoryAttemptRepository;
import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QualityMetricsService;
import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("QualityMetricsService — pass rates + bank composition (P013, §20.2)")
class QualityMetricsServiceTest {

  private final InMemoryAttemptRepository attempts = new InMemoryAttemptRepository();
  private final InMemoryAssessmentRepository assessments = new InMemoryAssessmentRepository();
  private final InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
  private final QualityMetricsService metrics =
      new QualityMetricsService(attempts, assessments, questions);

  private static Assessment assessment(String title) {
    Question q = Question.singleChoice("Q", List.of("a", "b"), 1, "e");
    return Assessment.fromBank(title, 1, List.of(q), 0.5);
  }

  private void record(Assessment a, double score, boolean passed) {
    attempts.save(Attempt.record("learner-" + score + "-" + passed, a,
        new AssessmentResult(passed ? 2 : 0, 2, score, passed)));
  }

  @Test
  @DisplayName("pass rate + average score aggregate every attempt at the assessment")
  void aggregatesAttempts() {
    Assessment a = assessment("Quiz");
    assessments.save(a);
    record(a, 1.0, true);
    record(a, 0.8, true);
    record(a, 1.0, true);
    record(a, 0.2, false); // 3 of 4 pass

    var m = metrics.metricsFor(a.id());
    assertEquals(4, m.attempts());
    assertEquals(3, m.passed());
    assertEquals(0.75, m.passRate(), 1e-9, "3/4 passed");
    assertEquals((1.0 + 0.8 + 1.0 + 0.2) / 4, m.averageScore(), 1e-9);
    assertEquals("Quiz", m.title());
  }

  @Test
  @DisplayName("an assessment with no attempts reports a zero pass rate, not a divide-by-zero")
  void noAttemptsIsZero() {
    Assessment a = assessment("Empty");
    assessments.save(a);
    var m = metrics.metricsFor(a.id());
    assertEquals(0, m.attempts());
    assertEquals(0.0, m.passRate(), 1e-9);
    assertEquals(0.0, m.averageScore(), 1e-9);
  }

  @Test
  @DisplayName("allAssessmentMetrics covers every assessment, sorted by title")
  void allMetricsSorted() {
    Assessment b = assessment("Beta");
    Assessment a = assessment("Alpha");
    assessments.save(b);
    assessments.save(a);
    record(a, 1.0, true);

    List<QualityMetricsService.AssessmentMetrics> all = metrics.allAssessmentMetrics();
    assertEquals(2, all.size());
    assertEquals("Alpha", all.get(0).title());
    assertEquals("Beta", all.get(1).title());
  }

  @Test
  @DisplayName("bank composition counts questions by status and difficulty")
  void bankComposition() {
    questions.save(Question.singleChoice("q1", List.of("a", "b"), 0, "e")); // DRAFT, MEDIUM
    questions.save(Question.singleChoice("q2", List.of("a", "b"), 0, "e")
        .withStatus(ContentStatus.PUBLISHED)
        .withMetadata("o", "t", Difficulty.HARD));

    assertEquals(1L, metrics.bankByStatus().get(ContentStatus.DRAFT));
    assertEquals(1L, metrics.bankByStatus().get(ContentStatus.PUBLISHED));
    assertEquals(1L, metrics.bankByDifficulty().get(Difficulty.MEDIUM));
    assertEquals(1L, metrics.bankByDifficulty().get(Difficulty.HARD));
  }
}
