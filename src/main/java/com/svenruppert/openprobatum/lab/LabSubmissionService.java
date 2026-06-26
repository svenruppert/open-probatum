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

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The lab-submission flow (concept §16.3): a learner submits practical evidence
 * against a <em>published</em> lab. The submission pins the lab's current
 * {@link Lab#version()} so a later lab version never falsifies it (§16.4).
 * Assessor verdicts (verify / reject) land in V00.40.00 P006.
 *
 * @since V00.40.00
 */
public final class LabSubmissionService implements HasLogger {

  /**
   * A process-wide lock for the verdict edge. The service is created per call, so
   * an instance {@code synchronized} would lock nothing across threads; verifying
   * a submission and minting its credential must be exactly-once even when two
   * assessors work the shared queue concurrently, so the SUBMITTED→decided
   * compare-and-set is serialised on this shared monitor. The view mints only on
   * the non-empty result, so exactly one assessor wins the edge — no double-mint.
   */
  private static final Object DECISION_LOCK = new Object();

  private final LabRepository labs;
  private final LabSubmissionRepository submissions;

  public LabSubmissionService(LabRepository labs, LabSubmissionRepository submissions) {
    this.labs = Objects.requireNonNull(labs, "labs");
    this.submissions = Objects.requireNonNull(submissions, "submissions");
  }

  public LabSubmissionService() {
    this(LabRepositoryProvider.repository(), LabSubmissionRepositoryProvider.repository());
  }

  /**
   * Records a learner's submission against a published lab, pinning the lab
   * version. Returns empty (and saves nothing) when the lab is unknown or not
   * published — a learner may only submit against a published lab.
   */
  public synchronized Optional<LabSubmission> submit(UUID labId, Long recipientId,
                                                     String learnerName, String writeUp,
                                                     String artefactLink) {
    Objects.requireNonNull(labId, "labId");
    return labs.findById(labId)
        .filter(Lab::isPublished)
        .map(lab -> {
          LabSubmission submission = LabSubmission.submit(lab.id(), lab.version(),
              recipientId, learnerName, writeUp, blankToNull(artefactLink));
          submissions.save(submission);
          logger().info("Lab submission {} by '{}' (id={}) against lab {} v{}",
              submission.id(), learnerName, recipientId, lab.id(), lab.version());
          return submission;
        });
  }

  /**
   * Verifies a SUBMITTED submission on behalf of {@code assessorId} — the
   * SUBMITTED→VERIFIED edge (idempotent: a non-SUBMITTED submission yields empty,
   * so re-verifying never re-fires). Enforces segregation of duties (§3.6): the
   * assessor may not have authored the lab. Returns the verified submission, or
   * empty when the id is unknown or already decided.
   */
  public Optional<LabSubmission> verify(UUID id, Long assessorId, String feedback) {
    return decide(id, assessorId, s -> s.verified(feedback), "verified");
  }

  /** Rejects a SUBMITTED submission with feedback (same SoD + idempotency as verify). */
  public Optional<LabSubmission> reject(UUID id, Long assessorId, String feedback) {
    return decide(id, assessorId, s -> s.rejected(feedback), "rejected");
  }

  private Optional<LabSubmission> decide(UUID id, Long assessorId,
                                         java.util.function.UnaryOperator<LabSubmission> decision,
                                         String action) {
    Objects.requireNonNull(id, "id");
    // Serialise the read-decide-write on the shared monitor so the
    // SUBMITTED→decided edge fires for exactly one caller (see DECISION_LOCK).
    synchronized (DECISION_LOCK) {
      return submissions.findById(id)
          .filter(s -> s.status() == SubmissionStatus.SUBMITTED)
          .map(s -> {
            guardSelfAssessment(s, assessorId);
            LabSubmission decided = decision.apply(s);
            submissions.save(decided);
            logger().info("Lab submission {} {} by assessor {}", id, action, assessorId);
            return decided;
          });
    }
  }

  /**
   * Refuses an assessor verifying a submission to a lab they authored (§3.6/§17.2).
   * Fail-open when the lab record is absent or the assessor id is unknown — SoD
   * cannot be enforced against an unknown author; lab versions are immutable and
   * assessors are authenticated, so this is by necessity, not a gap.
   */
  private void guardSelfAssessment(LabSubmission submission, Long assessorId) {
    labs.findById(submission.labId()).ifPresent(lab -> {
      if (ContentAuthorshipProvider.registry().isAuthor(lab.lineageId(), assessorId)) {
        throw new IllegalStateException("an author cannot assess submissions to their own lab");
      }
    });
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

