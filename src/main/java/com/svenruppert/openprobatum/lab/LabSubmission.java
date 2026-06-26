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

package com.svenruppert.openprobatum.lab;

import com.svenruppert.openprobatum.security.AppClock;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A learner's submission of practical evidence against a published {@link Lab}
 * (concept §16.3). The submission pins the {@link #labVersion} it was made
 * against, so a later lab version never falsifies it. An assessor moves it from
 * {@code SUBMITTED} to {@code VERIFIED} (valid practical evidence) or
 * {@code REJECTED} (with feedback). A verified submission is the evidence behind
 * a practical-lab credential (§10.6).
 *
 * @param id              the submission id
 * @param labId           the lab version submitted against
 * @param labVersion      the lab version (>= 1), pinned for reproducibility (§16.4)
 * @param recipientId     the stable id of the submitting learner (the wallet key)
 * @param learnerName     the learner's display name (shown to the assessor)
 * @param writeUp         the learner's account of what they did (non-blank)
 * @param artefactLink    an optional external artefact URL, or {@code null}
 * @param status          the submission state
 * @param assessorFeedback the assessor's feedback (never null; may be empty)
 * @param submittedAt     when the learner submitted
 * @param decidedAt       when an assessor decided, or {@code null} while SUBMITTED
 * @since V00.40.00
 */
public record LabSubmission(UUID id, UUID labId, int labVersion, Long recipientId,
                            String learnerName, String writeUp, String artefactLink,
                            SubmissionStatus status, String assessorFeedback,
                            Instant submittedAt, Instant decidedAt) {

  public LabSubmission {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(labId, "labId");
    Objects.requireNonNull(recipientId, "recipientId");
    Objects.requireNonNull(learnerName, "learnerName");
    Objects.requireNonNull(writeUp, "writeUp");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(assessorFeedback, "assessorFeedback");
    Objects.requireNonNull(submittedAt, "submittedAt");
    if (labVersion < 1) {
      throw new IllegalArgumentException("labVersion must be >= 1");
    }
    if (writeUp.isBlank()) {
      throw new IllegalArgumentException("a submission needs a non-blank write-up");
    }
  }

  /** A fresh {@code SUBMITTED} submission, stamped from {@link AppClock}. */
  public static LabSubmission submit(UUID labId, int labVersion, Long recipientId,
                                     String learnerName, String writeUp, String artefactLink) {
    return new LabSubmission(UUID.randomUUID(), labId, labVersion, recipientId,
        learnerName, writeUp, artefactLink, SubmissionStatus.SUBMITTED, "",
        AppClock.now(), null);
  }

  /** A copy marked {@code VERIFIED}, stamped now; optional assessor feedback. */
  public LabSubmission verified(String feedback) {
    return new LabSubmission(id, labId, labVersion, recipientId, learnerName, writeUp,
        artefactLink, SubmissionStatus.VERIFIED, feedback == null ? "" : feedback,
        submittedAt, AppClock.now());
  }

  /** A copy marked {@code REJECTED} with the assessor's feedback, stamped now. */
  public LabSubmission rejected(String feedback) {
    Objects.requireNonNull(feedback, "feedback");
    if (feedback.isBlank()) {
      throw new IllegalArgumentException("a rejection needs feedback for the learner");
    }
    return new LabSubmission(id, labId, labVersion, recipientId, learnerName, writeUp,
        artefactLink, SubmissionStatus.REJECTED, feedback, submittedAt, AppClock.now());
  }

  /** @return {@code true} once an assessor has decided (no longer SUBMITTED). */
  public boolean isDecided() {
    return status != SubmissionStatus.SUBMITTED;
  }

  /** @return {@code true} when this is verified practical evidence. */
  public boolean isVerified() {
    return status == SubmissionStatus.VERIFIED;
  }

  /** Whether this submission belongs to the learner with id {@code userId}. */
  public boolean isHeldBy(Long userId) {
    return recipientId.equals(userId);
  }

  /** The optional external artefact link. */
  public Optional<String> artefactLinkOpt() {
    return Optional.ofNullable(artefactLink);
  }

  /** The optional decision timestamp. */
  public Optional<Instant> decidedAtOpt() {
    return Optional.ofNullable(decidedAt);
  }
}
