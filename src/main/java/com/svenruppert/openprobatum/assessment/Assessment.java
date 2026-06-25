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

package com.svenruppert.openprobatum.assessment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A completion check (concept §9.6) — a fixed, versioned set of questions with a
 * defined pass threshold. Minimal for the slice: no randomisation, no time
 * limit. The {@code version} pins the question set a credential is issued
 * against (§16.4).
 *
 * @param id            random assessment id
 * @param title         the check title
 * @param version       the assessment version (>= 1)
 * @param questions     the fixed, defensively-copied question set (non-empty)
 * @param passThreshold the minimum fraction correct to pass, in {@code (0, 1]}
 * @since V00.10.00
 */
public record Assessment(UUID id, String title, int version,
                         List<Question> questions, double passThreshold) {

  public Assessment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(questions, "questions");
    questions = List.copyOf(questions);
    if (questions.isEmpty()) {
      throw new IllegalArgumentException("an assessment needs at least one question");
    }
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    if (passThreshold <= 0.0 || passThreshold > 1.0) {
      throw new IllegalArgumentException("passThreshold must be in (0, 1]");
    }
  }

  /**
   * Grades an attempt. {@code answers} maps a question id to the chosen option
   * indices; a missing entry counts as unanswered (wrong). The full score is
   * computed here but only {@code passed} drives credential issuance.
   */
  public AssessmentResult grade(Map<UUID, Set<Integer>> answers) {
    Objects.requireNonNull(answers, "answers");
    int correct = (int) questions.stream()
        .filter(q -> q.isCorrect(answers.get(q.id())))
        .count();
    double score = (double) correct / questions.size();
    return new AssessmentResult(correct, questions.size(), score, score >= passThreshold);
  }
}
