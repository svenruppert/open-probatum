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
 * A single question (concept §9.2, §9.4). Supports the three concept types
 * (single / multiple choice, true/false) and carries an {@code explanation}
 * shown as feedback in practice mode (§9.5).
 *
 * @param id             stable question id
 * @param text           the question prompt
 * @param type           the question type
 * @param options        the answer options
 * @param correctIndices the indices of the correct option(s)
 * @param explanation    didactic feedback shown after answering (may be empty)
 * @since V00.10.00
 */
public record Question(UUID id, String text, QuestionType type, List<String> options,
                       Set<Integer> correctIndices, String explanation) {

  public Question {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(correctIndices, "correctIndices");
    Objects.requireNonNull(explanation, "explanation");
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

  /** A single-choice question with one correct option index. */
  public static Question singleChoice(String text, List<String> options, int correct) {
    return singleChoice(text, options, correct, "");
  }

  /** A single-choice question with feedback explanation. */
  public static Question singleChoice(String text, List<String> options, int correct,
                                      String explanation) {
    return new Question(UUID.randomUUID(), text, QuestionType.SINGLE_CHOICE,
        options, Set.of(correct), explanation);
  }

  /** A multiple-choice question — every correct index and only those must be chosen. */
  public static Question multipleChoice(String text, List<String> options,
                                        Set<Integer> correct, String explanation) {
    return new Question(UUID.randomUUID(), text, QuestionType.MULTIPLE_CHOICE,
        options, correct, explanation);
  }

  /** A true/false question; {@code isTrue} selects the correct statement. */
  public static Question trueFalse(String text, boolean isTrue, String explanation) {
    return new Question(UUID.randomUUID(), text, QuestionType.TRUE_FALSE,
        List.of("True", "False"), Set.of(isTrue ? 0 : 1), explanation);
  }

  /** {@code true} when {@code chosen} is exactly the set of correct options. */
  public boolean isCorrect(Set<Integer> chosen) {
    return correctIndices.equals(chosen == null ? Set.of() : chosen);
  }

  /** Per-question practice feedback: whether the answer was correct + the explanation. */
  public QuestionFeedback feedback(Set<Integer> chosen) {
    return new QuestionFeedback(isCorrect(chosen), explanation);
  }
}
