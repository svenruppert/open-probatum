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

  /**
   * Serialises the grade→save→first-pass decision across ALL callers. The
   * service is created per request (the views do {@code new CheckService()}),
   * so an instance {@code synchronized} would lock a throw-away monitor and
   * protect nothing — the same reason {@code WorkshopEnrolmentService.SEAT_LOCK}
   * and {@code LabSubmissionService.DECISION_LOCK} are static. Without a shared
   * lock, two concurrent passing submits from the same learner can interleave
   * as save(A)→save(B)→count(A)=2→count(B)=2, so BOTH report firstPass=false
   * and the credential is never minted (and can never be, since passedAttemptCount
   * is then permanently &gt; 1).
   */
  private static final Object DECISION_LOCK = new Object();

  private final AttemptRepository attempts;

  public CheckService(AttemptRepository attempts) {
    this.attempts = Objects.requireNonNull(attempts, "attempts");
  }

  public CheckService() {
    this(AttemptRepositoryProvider.repository());
  }

  /**
   * The outcome of a submitted check: the recorded {@code attempt}, and whether
   * it is the learner's <em>first</em> passing attempt — the atomic signal the
   * caller uses to issue a credential exactly once.
   */
  public record SubmitOutcome(Attempt attempt, boolean firstPass) {
  }

  /**
   * Grades {@code answers}, records the attempt for {@code learnerName}, and
   * decides — atomically, under {@link #DECISION_LOCK} — whether it is the first
   * passing attempt. Computing {@code firstPass} inside the same locked block as
   * the save closes the issuance double-mint race (no TOCTOU between "did it pass
   * first?" and recording the attempt), across concurrent per-request instances.
   */
  public SubmitOutcome submit(String learnerName, Assessment assessment,
                              Map<UUID, Set<Integer>> answers) {
    Objects.requireNonNull(learnerName, "learnerName");
    Objects.requireNonNull(assessment, "assessment");
    AssessmentResult result = assessment.grade(answers);
    synchronized (DECISION_LOCK) {
      Attempt attempt = Attempt.record(learnerName, assessment, result);
      attempts.save(attempt);
      boolean firstPass = attempt.passed()
          && passedAttemptCount(learnerName, assessment.id()) == 1;
      return new SubmitOutcome(attempt, firstPass);
    }
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
