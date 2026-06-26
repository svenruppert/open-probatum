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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Runs a graded completion check (concept §9.6): grades the answers, records a
 * counted {@link Attempt}, and returns it. Unlike practice mode this persists
 * the attempt; issuance-on-pass is wired by the issuance flow on top of this.
 *
 * @since V00.20.00
 */
public final class CheckService {

  private final AttemptRepository attempts;

  public CheckService(AttemptRepository attempts) {
    this.attempts = Objects.requireNonNull(attempts, "attempts");
  }

  public CheckService() {
    this(AttemptRepositoryProvider.repository());
  }

  /** Grades {@code answers}, records the attempt for {@code learnerName}, returns it. */
  public synchronized Attempt submit(String learnerName, Assessment assessment,
                                     Map<UUID, Set<Integer>> answers) {
    Objects.requireNonNull(learnerName, "learnerName");
    Objects.requireNonNull(assessment, "assessment");
    AssessmentResult result = assessment.grade(answers);
    Attempt attempt = Attempt.record(learnerName, assessment, result);
    attempts.save(attempt);
    return attempt;
  }

  /** How many times {@code learnerName} has attempted the assessment. */
  public int attemptCount(String learnerName, UUID assessmentId) {
    return attempts.countFor(learnerName, assessmentId);
  }

  /** How many of {@code learnerName}'s attempts at the assessment passed. */
  public long passedAttemptCount(String learnerName, UUID assessmentId) {
    return attempts.forLearner(learnerName, assessmentId).stream()
        .filter(Attempt::passed)
        .count();
  }
}
