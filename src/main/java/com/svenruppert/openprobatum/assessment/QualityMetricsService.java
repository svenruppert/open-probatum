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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quality metrics for authors and reviewers (concept §20.2): per-assessment pass
 * rates + average scores aggregated over {@link Attempt}s, and the composition of
 * the question bank by lifecycle status + difficulty. Attempts record only the
 * overall result (not per-question answers), so per-question quality is surfaced
 * as the bank's difficulty/status mix rather than item-level difficulty indices.
 *
 * @since V00.30.00
 */
public final class QualityMetricsService {

  private final AttemptRepository attempts;
  private final AssessmentRepository assessments;
  private final QuestionRepository questions;

  public QualityMetricsService(AttemptRepository attempts, AssessmentRepository assessments,
                               QuestionRepository questions) {
    this.attempts = Objects.requireNonNull(attempts, "attempts");
    this.assessments = Objects.requireNonNull(assessments, "assessments");
    this.questions = Objects.requireNonNull(questions, "questions");
  }

  public QualityMetricsService() {
    this(AttemptRepositoryProvider.repository(),
        AssessmentRepositoryProvider.repository(),
        QuestionRepositoryProvider.repository());
  }

  /**
   * Aggregated quality of one assessment.
   *
   * @param assessmentId the assessment
   * @param title        the assessment title (or its id when unknown)
   * @param attempts     the number of attempts made
   * @param passed       how many of them passed
   * @param passRate     {@code passed / attempts} in {@code [0, 1]} (0 when no attempts)
   * @param averageScore the mean attempt score in {@code [0, 1]} (0 when no attempts)
   */
  public record AssessmentMetrics(UUID assessmentId, String title, int attempts,
                                  int passed, double passRate, double averageScore) {
  }

  /** Metrics for a single assessment. */
  public AssessmentMetrics metricsFor(UUID assessmentId) {
    Objects.requireNonNull(assessmentId, "assessmentId");
    List<Attempt> all = attempts.forAssessment(assessmentId);
    int total = all.size();
    int passed = (int) all.stream().filter(Attempt::passed).count();
    double passRate = total == 0 ? 0.0 : (double) passed / total;
    double avgScore = total == 0 ? 0.0
        : all.stream().mapToDouble(a -> a.result().score()).average().orElse(0.0);
    String title = assessments.findById(assessmentId)
        .map(Assessment::title).orElse(assessmentId.toString());
    return new AssessmentMetrics(assessmentId, title, total, passed, passRate, avgScore);
  }

  /** Metrics for every known assessment, by title. */
  public List<AssessmentMetrics> allAssessmentMetrics() {
    return assessments.all().stream()
        .map(a -> metricsFor(a.id()))
        .sorted((x, y) -> x.title().compareToIgnoreCase(y.title()))
        .toList();
  }

  /** How many bank questions sit in each lifecycle status. */
  public Map<ContentStatus, Long> bankByStatus() {
    return questions.all().stream()
        .collect(Collectors.groupingBy(Question::status, Collectors.counting()));
  }

  /** How many bank questions sit at each difficulty. */
  public Map<Difficulty, Long> bankByDifficulty() {
    return questions.all().stream()
        .collect(Collectors.groupingBy(Question::difficulty, Collectors.counting()));
  }
}
