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

package junit.com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.assessment.Assessment;
import com.svenruppert.openprobatum.assessment.AssessmentRepositoryProvider;
import com.svenruppert.openprobatum.assessment.AssessmentResult;
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.assessment.AttemptRepositoryProvider;
import com.svenruppert.openprobatum.assessment.InMemoryAssessmentRepository;
import com.svenruppert.openprobatum.assessment.InMemoryAttemptRepository;
import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.views.MetricsView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MetricsView — quality metrics surface (P013)")
class MetricsViewBrowserlessTest extends BrowserlessTest {

  private InMemoryAttemptRepository attempts;
  private InMemoryAssessmentRepository assessments;
  private InMemoryQuestionRepository questions;
  private com.svenruppert.openprobatum.lab.InMemoryLabRepository labs;
  private com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository labSubmissions;

  @BeforeEach
  void setUp() {
    attempts = new InMemoryAttemptRepository();
    assessments = new InMemoryAssessmentRepository();
    questions = new InMemoryQuestionRepository();
    labs = new com.svenruppert.openprobatum.lab.InMemoryLabRepository();
    labSubmissions = new com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository();
    AttemptRepositoryProvider.setRepository(attempts);
    AssessmentRepositoryProvider.setRepository(assessments);
    QuestionRepositoryProvider.setRepository(questions);
    com.svenruppert.openprobatum.lab.LabRepositoryProvider.setRepository(labs);
    com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider.setRepository(labSubmissions);
    // Packaging metrics read bundles/workshops/enrolments/credentials — keep in memory.
    com.svenruppert.openprobatum.bundle.BundleRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.bundle.InMemoryBundleRepository());
    com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository());
    com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.workshop.InMemoryWorkshopEnrolmentRepository());
    com.svenruppert.openprobatum.credential.CredentialRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.credential.InMemoryCredentialRepository());
    com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository());
    com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.coaching.InMemoryCoachingSlotRepository());
  }

  @AfterEach
  void tearDown() {
    AttemptRepositoryProvider.reset();
    AssessmentRepositoryProvider.reset();
    QuestionRepositoryProvider.reset();
    com.svenruppert.openprobatum.lab.LabRepositoryProvider.reset();
    com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider.reset();
    com.svenruppert.openprobatum.bundle.BundleRepositoryProvider.reset();
    com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider.reset();
    com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider.reset();
    com.svenruppert.openprobatum.credential.CredentialRepositoryProvider.reset();
    com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider.reset();
    com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider.reset();
  }

  @Test
  @DisplayName("the view shows an assessment's pass rate and the bank size")
  void showsPassRateAndBank() {
    Question q = Question.singleChoice("Q", List.of("a", "b"), 1, "e");
    Assessment a = Assessment.fromBank("Quiz", 1, List.of(q), 0.5);
    assessments.save(a);
    questions.save(q);
    attempts.save(Attempt.record("x", a, new AssessmentResult(2, 2, 1.0, true)));
    attempts.save(Attempt.record("y", a, new AssessmentResult(0, 2, 0.0, false)));

    MetricsView view = new MetricsView();

    assertEquals(List.of(a.id().toString()), attributes(view, "data-assessment"));
    assertEquals(List.of("50"), attributes(view, "data-pass-rate"), "1 of 2 passed → 50%");
    assertEquals(List.of("2"), attributes(view, "data-attempts"));
    assertEquals(List.of("1"), attributes(view, "data-bank-total"));
  }

  @Test
  @DisplayName("the view shows a lab's verify rate (P009)")
  void showsLabVerifyRate() {
    com.svenruppert.openprobatum.lab.Lab lab =
        com.svenruppert.openprobatum.lab.Lab.draft("Deploy", "do it");
    labs.save(lab);
    labSubmissions.save(com.svenruppert.openprobatum.lab.LabSubmission
        .submit(lab.id(), lab.version(), 1L, "Ada", "did it", null).verified("ok"));
    labSubmissions.save(com.svenruppert.openprobatum.lab.LabSubmission
        .submit(lab.id(), lab.version(), 2L, "Bob", "did it", null).rejected("no"));

    MetricsView view = new MetricsView();
    assertEquals(List.of(lab.id().toString()), attributes(view, "data-lab"));
    assertEquals(List.of("50"), attributes(view, "data-verify-rate"), "1 of 2 verified → 50%");
    assertEquals(List.of("2"), attributes(view, "data-submissions"));
  }

  @Test
  @DisplayName("with no assessments the view still renders the bank section")
  void emptyAssessments() {
    MetricsView view = new MetricsView();
    assertTrue(attributes(view, "data-assessment").isEmpty());
    assertEquals(List.of("0"), attributes(view, "data-bank-total"));
  }

  private static List<String> attributes(Component root, String name) {
    List<String> values = new ArrayList<>();
    collect(root, name, values);
    return values;
  }

  private static void collect(Component c, String name, List<String> out) {
    String v = c.getElement().getAttribute(name);
    if (v != null) {
      out.add(v);
    }
    c.getChildren().forEach(child -> collect(child, name, out));
  }
}
