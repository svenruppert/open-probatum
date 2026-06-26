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

package com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.progress.LearnerProgress;

import java.util.Objects;
import java.util.UUID;

/**
 * A rule that declares what earns a credential (concept §3.7, §10.1). A rule is
 * one of two kinds:
 *
 * <ul>
 *   <li>{@link RuleType#ASSESSMENT_PASSED} — a specific assessment (by
 *       {@code targetId}) is passed, optionally above a minimum {@code minScore}
 *       fraction; evaluated against an {@link Attempt}.</li>
 *   <li>{@link RuleType#PATH_COMPLETED} — a specific offering's learning path
 *       (by {@code targetId} = offering id) is complete; evaluated against a
 *       {@link LearnerProgress} plus the offering's {@link LearningPath}.</li>
 * </ul>
 *
 * <p>The rule also declares the credential it awards ({@code credentialTitle} +
 * {@code awards} type), so a satisfied rule fully defines the issuance.
 *
 * @param id              the rule id
 * @param type            the rule kind
 * @param targetId        the assessment id (ASSESSMENT_PASSED) or offering id (PATH_COMPLETED)
 * @param minScore        the minimum score fraction in {@code [0, 1]} (ASSESSMENT_PASSED only)
 * @param credentialTitle the title of the credential awarded when satisfied
 * @param awards          the type of the credential awarded
 * @since V00.30.00
 */
public record CredentialRule(UUID id, RuleType type, UUID targetId, double minScore,
                             String credentialTitle, CredentialType awards) {

  /** The condition under which a credential is earned. */
  public enum RuleType {
    /** A specific assessment is passed (optionally above a minimum score). */
    ASSESSMENT_PASSED,
    /** A specific offering's learning path is completed. */
    PATH_COMPLETED,
    /** A specific lab's practical submission is verified by an assessor. */
    LAB_VERIFIED,
    /** Every member offering of a specific bundle is completed. */
    BUNDLE_COMPLETED
  }

  public CredentialRule {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(targetId, "targetId");
    Objects.requireNonNull(credentialTitle, "credentialTitle");
    Objects.requireNonNull(awards, "awards");
    if (credentialTitle.isBlank()) {
      throw new IllegalArgumentException("a credential rule needs a non-blank title");
    }
    if (minScore < 0.0 || minScore > 1.0) {
      throw new IllegalArgumentException("minScore must be in [0, 1]");
    }
  }

  /** A rule earned by passing {@code assessmentId} at or above {@code minScore}. */
  public static CredentialRule assessmentPassed(UUID assessmentId, double minScore,
                                                String credentialTitle, CredentialType awards) {
    return new CredentialRule(UUID.randomUUID(), RuleType.ASSESSMENT_PASSED,
        assessmentId, minScore, credentialTitle, awards);
  }

  /** A rule earned by completing the learning path of {@code offeringId}. */
  public static CredentialRule pathCompleted(UUID offeringId,
                                             String credentialTitle, CredentialType awards) {
    return new CredentialRule(UUID.randomUUID(), RuleType.PATH_COMPLETED,
        offeringId, 0.0, credentialTitle, awards);
  }

  /** A rule earned by an assessor verifying a submission against {@code labId}. */
  public static CredentialRule labVerified(UUID labId,
                                           String credentialTitle, CredentialType awards) {
    return new CredentialRule(UUID.randomUUID(), RuleType.LAB_VERIFIED,
        labId, 0.0, credentialTitle, awards);
  }

  /**
   * A rule earned by completing every member offering of {@code bundleId}.
   * Satisfaction is an aggregate over the bundle's members, so it is evaluated by
   * {@code BundleCompletionService} rather than a single-object {@code isSatisfiedBy}.
   */
  public static CredentialRule bundleCompleted(UUID bundleId,
                                               String credentialTitle, CredentialType awards) {
    return new CredentialRule(UUID.randomUUID(), RuleType.BUNDLE_COMPLETED,
        bundleId, 0.0, credentialTitle, awards);
  }

  /**
   * Whether this (ASSESSMENT_PASSED) rule is satisfied by {@code attempt}: the
   * attempt is against this rule's assessment, it passed, and its score meets
   * {@code minScore}. A rule of another kind is never satisfied by an attempt.
   */
  public boolean isSatisfiedBy(Attempt attempt) {
    Objects.requireNonNull(attempt, "attempt");
    return type == RuleType.ASSESSMENT_PASSED
        && attempt.assessmentId().equals(targetId)
        && attempt.passed()
        && attempt.result().score() >= minScore;
  }

  /**
   * Whether this (PATH_COMPLETED) rule is satisfied by {@code progress} against
   * {@code path}: the progress is for this rule's offering and every mandatory
   * module is complete. A rule of another kind is never satisfied this way.
   */
  public boolean isSatisfiedBy(LearningPath path, LearnerProgress progress) {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(progress, "progress");
    return type == RuleType.PATH_COMPLETED
        && progress.offeringId().equals(targetId)
        && path.isComplete(progress.completedModuleIds());
  }

  /**
   * Whether this (LAB_VERIFIED) rule is satisfied by {@code submission}: the
   * submission is against this rule's lab and an assessor has verified it. A rule
   * of another kind is never satisfied by a submission.
   */
  public boolean isSatisfiedBy(com.svenruppert.openprobatum.lab.LabSubmission submission) {
    Objects.requireNonNull(submission, "submission");
    return type == RuleType.LAB_VERIFIED
        && submission.labId().equals(targetId)
        && submission.isVerified();
  }
}
