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

import com.svenruppert.openprobatum.assessment.Assessment;
import com.svenruppert.openprobatum.assessment.AssessmentResult;
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.credential.CredentialRule;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.progress.LearnerProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialRule — what earns a credential (P007)")
class CredentialRuleTest {

  private static Assessment assessment() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.");
    return Assessment.fromBank("Quiz", 1, List.of(q), 0.5);
  }

  private static Attempt attemptWith(Assessment a, double score, boolean passed) {
    return Attempt.record("Ada", a, new AssessmentResult(1, 2, score, passed));
  }

  @Test
  @DisplayName("an ASSESSMENT_PASSED rule is satisfied by a passing attempt at or above its minimum score")
  void assessmentRuleHappyPath() {
    Assessment a = assessment();
    CredentialRule rule = CredentialRule.assessmentPassed(a.id(), 0.8,
        "Vaadin Basics", CredentialType.COMPLETION_CERTIFICATE);

    assertTrue(rule.isSatisfiedBy(attemptWith(a, 0.9, true)), "passed, score above the floor");
    assertFalse(rule.isSatisfiedBy(attemptWith(a, 0.7, true)), "passed but below the score floor");
    assertFalse(rule.isSatisfiedBy(attemptWith(a, 0.9, false)), "score high but not a pass");
  }

  @Test
  @DisplayName("an ASSESSMENT_PASSED rule ignores an attempt at a different assessment")
  void assessmentRuleWrongTarget() {
    CredentialRule rule = CredentialRule.assessmentPassed(UUID.randomUUID(), 0.0,
        "X", CredentialType.COMPLETION_CERTIFICATE);
    assertFalse(rule.isSatisfiedBy(attemptWith(assessment(), 1.0, true)));
  }

  @Test
  @DisplayName("a PATH_COMPLETED rule is satisfied only when every mandatory module is done")
  void pathRuleHappyPath() {
    Module core = Module.mandatory("Core", "c");
    Module bonus = Module.optional("Bonus", "c");
    LearningPath path = new LearningPath("P", List.of(core, bonus));
    UUID offeringId = UUID.randomUUID();
    CredentialRule rule = CredentialRule.pathCompleted(offeringId,
        "Path Done", CredentialType.COMPLETION_CERTIFICATE);

    LearnerProgress none = LearnerProgress.empty(1L, offeringId);
    LearnerProgress done = none.withModuleCompleted(core.id());

    assertFalse(rule.isSatisfiedBy(path, none), "nothing done");
    assertTrue(rule.isSatisfiedBy(path, done), "the mandatory module done");
  }

  @Test
  @DisplayName("a PATH_COMPLETED rule ignores progress for another offering")
  void pathRuleWrongOffering() {
    Module core = Module.mandatory("Core", "c");
    LearningPath path = new LearningPath("P", List.of(core));
    CredentialRule rule = CredentialRule.pathCompleted(UUID.randomUUID(),
        "X", CredentialType.COMPLETION_CERTIFICATE);

    LearnerProgress otherOffering =
        LearnerProgress.empty(1L, UUID.randomUUID()).withModuleCompleted(core.id());
    assertFalse(rule.isSatisfiedBy(path, otherOffering));
  }

  @Test
  @DisplayName("the rule kinds do not cross-evaluate; bad data is rejected")
  void kindIsolationAndValidation() {
    Assessment a = assessment();
    CredentialRule assessmentRule = CredentialRule.assessmentPassed(a.id(), 0.0,
        "X", CredentialType.COMPLETION_CERTIFICATE);
    // An assessment rule is never satisfied by a path check.
    Module core = Module.mandatory("Core", "c");
    assertFalse(assessmentRule.isSatisfiedBy(new LearningPath("P", List.of(core)),
        LearnerProgress.empty(1L, a.id()).withModuleCompleted(core.id())));

    assertThrows(IllegalArgumentException.class, () ->
        CredentialRule.assessmentPassed(a.id(), 1.5, "X", CredentialType.COMPLETION_CERTIFICATE));
    assertThrows(IllegalArgumentException.class, () ->
        CredentialRule.pathCompleted(a.id(), "  ", CredentialType.COMPLETION_CERTIFICATE));
  }
}
