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

import com.svenruppert.openprobatum.content.ContentStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A single question as a versioned, reviewable didactic object (concept §9.2,
 * §9.4). Beyond the answer model it carries a {@code lineageId} (the stable
 * logical question across versions), a {@code version}, an editorial
 * {@link ContentStatus}, an explanation shown as practice feedback, a learning
 * objective, a topic, a {@link Difficulty} and free-form {@code tags} for the
 * bank's categorisation (§9.3).
 *
 * <p>Each version is its own immutable record with a distinct {@link #id}, so a
 * new version never overwrites or falsifies an existing one — attempts that
 * referenced an earlier version stay truthful (§16.4).
 *
 * @param id                this version's id (unique per version)
 * @param lineageId         the stable logical-question id shared across versions
 * @param version           the version sequence number (≥ 1)
 * @param status            the editorial lifecycle status
 * @param text              the prompt
 * @param type              the question type
 * @param options           the answer options
 * @param correctIndices    the indices of the correct option(s)
 * @param explanation       didactic feedback (may be empty in DRAFT)
 * @param learningObjective what the question assesses (may be empty in DRAFT)
 * @param topic             the subject area (may be empty)
 * @param difficulty        the question difficulty
 * @param tags              free-form categorisation tags (defensively copied)
 * @since V00.10.00
 */
public record Question(UUID id, UUID lineageId, int version, ContentStatus status,
                       String text, QuestionType type, List<String> options,
                       Set<Integer> correctIndices, String explanation,
                       String learningObjective, String topic, Difficulty difficulty,
                       Set<String> tags) {

  public Question {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lineageId, "lineageId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(correctIndices, "correctIndices");
    Objects.requireNonNull(explanation, "explanation");
    Objects.requireNonNull(learningObjective, "learningObjective");
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(difficulty, "difficulty");
    Objects.requireNonNull(tags, "tags");
    options = List.copyOf(options);
    correctIndices = Set.copyOf(correctIndices);
    tags = Set.copyOf(tags);
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
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

  // ── factories (a fresh DRAFT v1 with its own lineage) ──────────────

  private static Question draft(String text, QuestionType type, List<String> options,
                                Set<Integer> correct, String explanation) {
    UUID id = UUID.randomUUID();
    return new Question(id, id, 1, ContentStatus.DRAFT, text, type, options, correct,
        explanation, "", "", Difficulty.MEDIUM, Set.of());
  }

  /** A single-choice question with one correct option index. */
  public static Question singleChoice(String text, List<String> options, int correct) {
    return singleChoice(text, options, correct, "");
  }

  /** A single-choice question with feedback explanation. */
  public static Question singleChoice(String text, List<String> options, int correct,
                                      String explanation) {
    return draft(text, QuestionType.SINGLE_CHOICE, options, Set.of(correct), explanation);
  }

  /** A multiple-choice question — every correct index and only those must be chosen. */
  public static Question multipleChoice(String text, List<String> options,
                                        Set<Integer> correct, String explanation) {
    return draft(text, QuestionType.MULTIPLE_CHOICE, options, correct, explanation);
  }

  /** A true/false question; {@code isTrue} selects the correct statement. */
  public static Question trueFalse(String text, boolean isTrue, String explanation) {
    return draft(text, QuestionType.TRUE_FALSE, List.of("True", "False"),
        Set.of(isTrue ? 0 : 1), explanation);
  }

  // ── withers (immutable lifecycle/version/metadata changes) ─────────

  /** A copy with the editorial status changed. */
  public Question withStatus(ContentStatus newStatus) {
    return new Question(id, lineageId, version, newStatus, text, type, options, correctIndices,
        explanation, learningObjective, topic, difficulty, tags);
  }

  /** A copy with didactic metadata set. */
  public Question withMetadata(String objective, String aTopic, Difficulty level) {
    return new Question(id, lineageId, version, status, text, type, options, correctIndices,
        explanation, objective, aTopic, level, tags);
  }

  /** A copy with the given categorisation tags. */
  public Question withTags(Set<String> newTags) {
    return new Question(id, lineageId, version, status, text, type, options, correctIndices,
        explanation, learningObjective, topic, difficulty, newTags);
  }

  /**
   * A fresh DRAFT next version of the same logical question — a new {@link #id},
   * same {@link #lineageId}, {@code version + 1}. The prior version stays intact.
   */
  public Question asNewVersion() {
    return new Question(UUID.randomUUID(), lineageId, version + 1, ContentStatus.DRAFT,
        text, type, options, correctIndices, explanation, learningObjective, topic, difficulty,
        tags);
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
