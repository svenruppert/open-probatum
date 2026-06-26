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

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
