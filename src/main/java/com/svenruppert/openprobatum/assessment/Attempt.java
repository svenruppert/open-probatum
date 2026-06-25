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

import com.svenruppert.openprobatum.security.AppClock;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One learner's attempt at an assessment (concept §9.7) — the evidence behind a
 * credential. Records who attempted which assessment version and the graded
 * result, so a credential's basis stays reproducible (§16.4).
 *
 * @param id                random attempt id
 * @param learnerName       the learner (recipient on the resulting credential)
 * @param assessmentId      the assessment attempted
 * @param assessmentVersion the assessment version graded against
 * @param result            the graded outcome
 * @param submittedAt       when the attempt was submitted
 * @since V00.10.00
 */
public record Attempt(UUID id, String learnerName, UUID assessmentId,
                      int assessmentVersion, AssessmentResult result, Instant submittedAt) {

  public Attempt {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(learnerName, "learnerName");
    Objects.requireNonNull(assessmentId, "assessmentId");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(submittedAt, "submittedAt");
  }

  /**
   * Records a fresh attempt of {@code assessment} by {@code learnerName} with
   * the given {@code result}, stamped from {@link AppClock}.
   */
  public static Attempt record(String learnerName, Assessment assessment, AssessmentResult result) {
    return new Attempt(UUID.randomUUID(), learnerName,
        assessment.id(), assessment.version(), result, AppClock.now());
  }

  /** Whether this attempt passed — the gate for credential issuance. */
  public boolean passed() {
    return result.passed();
  }
}
