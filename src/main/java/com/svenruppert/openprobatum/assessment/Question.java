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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A single assessment question (concept §9.4). Answered correctly when the
 * chosen option indices match {@link #correctIndices()} exactly — so a
 * multiple-choice question with a missing or extra pick counts as wrong.
 *
 * @param id             random question id
 * @param text           the question prompt
 * @param type           the question form
 * @param options        the answer options (defensively copied, non-empty)
 * @param correctIndices the indices of the correct options (non-empty, in range)
 * @since V00.10.00
 */
public record Question(UUID id, String text, QuestionType type,
                       List<String> options, Set<Integer> correctIndices) {

  public Question {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(correctIndices, "correctIndices");
    options = List.copyOf(options);
    correctIndices = Set.copyOf(correctIndices);
    if (options.isEmpty()) {
      throw new IllegalArgumentException("a question needs at least one option");
    }
    if (correctIndices.isEmpty()) {
      throw new IllegalArgumentException("a question needs at least one correct option");
    }
    for (int i : correctIndices) {
      if (i < 0 || i >= options.size()) {
        throw new IllegalArgumentException("correct index out of range: " + i);
      }
    }
    if (type != QuestionType.MULTIPLE_CHOICE && correctIndices.size() != 1) {
      throw new IllegalArgumentException(type + " must have exactly one correct option");
    }
  }

  /** Creates a single-choice question with one correct option index. */
  public static Question singleChoice(String text, List<String> options, int correct) {
    return new Question(UUID.randomUUID(), text, QuestionType.SINGLE_CHOICE,
        options, Set.of(correct));
  }

  /** {@code true} when {@code chosen} is exactly the set of correct options. */
  public boolean isCorrect(Set<Integer> chosen) {
    return correctIndices.equals(chosen == null ? Set.of() : chosen);
  }
}
