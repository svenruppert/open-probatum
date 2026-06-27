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

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.security.AppClock;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues a credential when an attempt passes (concept §3.7, §10.x). The passed
 * {@link Attempt} is the evidence ("Assessment Passed", §10.6); the credential
 * carries this instance's {@link IssuerIdentity} (§4.3), the learner as
 * recipient, and is persisted through the {@link CredentialRepository} as the
 * sole source of truth — no PDF is produced here.
 *
 * @since V00.10.00
 */
public final class IssuanceService implements HasLogger {

  private final CredentialRepository repository;
  private final IssuerIdentity issuer;

  public IssuanceService(CredentialRepository repository, IssuerIdentity issuer) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.issuer = Objects.requireNonNull(issuer, "issuer");
  }

  /**
   * Issues and persists a {@code VALID} credential for a <em>passed</em>
   * attempt; returns empty (and persists nothing) when the attempt did not pass.
   *
   * @param attempt   the graded attempt (the evidence)
   * @param title     the credential title
   * @param type      the credential type
   * @param expiresAt optional expiry; {@code null} for no expiry
   */
  public Optional<Credential> issueFor(Attempt attempt, String title,
                                       CredentialType type, java.time.Instant expiresAt) {
    return issueFor(attempt, null, title, type, expiresAt);
  }

  /**
   * Issues and persists a {@code VALID} credential for a <em>passed</em>
   * attempt, bound to the recipient's stable {@code recipientId} (the durable
   * wallet/dashboard key, §17.2) and carrying {@link Evidence} that the
   * assessment version was passed (§10.6/§16.4). Returns empty (and persists
   * nothing) when the attempt did not pass.
   *
   * @param attempt     the graded attempt (the evidence)
   * @param recipientId the stable id of the recipient, or {@code null} if unknown
   * @param title       the credential title
   * @param type        the credential type
   * @param expiresAt   optional expiry; {@code null} for no expiry
   */
  public Optional<Credential> issueFor(Attempt attempt, Long recipientId, String title,
                                       CredentialType type, java.time.Instant expiresAt) {
    Objects.requireNonNull(attempt, "attempt");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    if (!attempt.passed()) {
      logger().debug("No credential issued — attempt {} did not pass", attempt.id());
      return Optional.empty();
    }
    Evidence evidence = Evidence.assessmentPassed(attempt.assessmentId(), attempt.assessmentVersion());
    Credential credential = Credential.issue(title, type, recipientId, attempt.learnerName(),
        issuer.name(), AppClock.now(), expiresAt, evidence);
    repository.save(credential);
    // App-side audit trail (§17.3): one ISSUED event per issuance.
    CredentialEventRepositoryProvider.repository().append(CredentialEvent.of(
        credential.id(), CredentialEvent.Action.ISSUED, "system",
        "assessment " + attempt.assessmentId() + " v" + attempt.assessmentVersion() + " passed"));
    logger().info("Issued credential {} to '{}' (id={}, evidence: assessment {} v{} passed)",
        credential.id(), attempt.learnerName(), recipientId,
        attempt.assessmentId(), attempt.assessmentVersion());
    return Optional.of(credential);
  }

  /**
   * Issues and persists a {@code VALID} credential for a <em>verified</em> lab
   * submission (concept §10.6/§16.3), carrying {@link Evidence} that the lab
   * version's practical submission was verified + the submission's stable
   * recipient id. Emits one {@code ISSUED} audit event. Returns empty (and
   * persists nothing) when the submission is not verified.
   *
   * @param submission the verified lab submission (the evidence)
   * @param title      the credential title
   * @param type       the credential type
   * @param expiresAt  optional expiry; {@code null} for no expiry
   */
  public Optional<Credential> issueForLab(com.svenruppert.openprobatum.lab.LabSubmission submission,
                                          String title, CredentialType type,
                                          java.time.Instant expiresAt) {
    Objects.requireNonNull(submission, "submission");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    if (!submission.isVerified()) {
      logger().debug("No credential issued — submission {} is not verified", submission.id());
      return Optional.empty();
    }
    Evidence evidence = Evidence.labVerified(submission.labId(), submission.labVersion());
    Credential credential = Credential.issue(title, type, submission.recipientId(),
        submission.learnerName(), issuer.name(), AppClock.now(), expiresAt, evidence);
    repository.save(credential);
    CredentialEventRepositoryProvider.repository().append(CredentialEvent.of(
        credential.id(), CredentialEvent.Action.ISSUED, "system",
        "lab " + submission.labId() + " v" + submission.labVersion() + " verified"));
    logger().info("Issued credential {} to '{}' (id={}, evidence: lab {} v{} verified)",
        credential.id(), submission.learnerName(), submission.recipientId(),
        submission.labId(), submission.labVersion());
    return Optional.of(credential);
  }

  /**
   * Issues and persists a {@code VALID} credential for a <em>completed</em> bundle
   * (concept §10.6), carrying {@link Evidence} that the bundle version's offerings
   * were all completed + the learner's stable recipient id. Emits one
   * {@code ISSUED} audit event. The caller (BundleCompletionService) guards
   * completion + already-issued + the atomic claim edge.
   *
   * @param bundleId      the completed bundle version
   * @param bundleVersion the bundle version (§16.4)
   * @param recipientId   the learner's stable id
   * @param learnerName   the learner's display name
   * @param title         the credential title
   * @param type          the credential type
   * @param expiresAt     optional expiry; {@code null} for no expiry
   */
  public Optional<Credential> issueForBundle(UUID bundleId, int bundleVersion, Long recipientId,
                                             String learnerName, String title, CredentialType type,
                                             java.time.Instant expiresAt) {
    Objects.requireNonNull(bundleId, "bundleId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    Evidence evidence = Evidence.bundleCompleted(bundleId, bundleVersion);
    Credential credential = Credential.issue(title, type, recipientId, learnerName,
        issuer.name(), AppClock.now(), expiresAt, evidence);
    repository.save(credential);
    CredentialEventRepositoryProvider.repository().append(CredentialEvent.of(
        credential.id(), CredentialEvent.Action.ISSUED, "system",
        "bundle " + bundleId + " v" + bundleVersion + " completed"));
    logger().info("Issued credential {} to '{}' (id={}, evidence: bundle {} v{} completed)",
        credential.id(), learnerName, recipientId, bundleId, bundleVersion);
    return Optional.of(credential);
  }

  /**
   * Issues and persists a {@code VALID} credential for an <em>attended</em>
   * workshop (concept §10.6), carrying {@link Evidence} that the workshop version
   * was attended + the learner's stable recipient id. Emits one {@code ISSUED}
   * audit event. The caller (the attendance flow) guards the atomic attend edge.
   *
   * @param workshopId      the attended workshop version
   * @param workshopVersion the workshop version (§16.4)
   * @param recipientId     the learner's stable id
   * @param learnerName     the learner's display name
   * @param title           the credential title
   * @param type            the credential type
   * @param expiresAt       optional expiry; {@code null} for no expiry
   */
  public Optional<Credential> issueForWorkshop(UUID workshopId, int workshopVersion, Long recipientId,
                                               String learnerName, String title, CredentialType type,
                                               java.time.Instant expiresAt) {
    Objects.requireNonNull(workshopId, "workshopId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    Evidence evidence = Evidence.workshopAttended(workshopId, workshopVersion);
    Credential credential = Credential.issue(title, type, recipientId, learnerName,
        issuer.name(), AppClock.now(), expiresAt, evidence);
    repository.save(credential);
    CredentialEventRepositoryProvider.repository().append(CredentialEvent.of(
        credential.id(), CredentialEvent.Action.ISSUED, "system",
        "workshop " + workshopId + " v" + workshopVersion + " attended"));
    logger().info("Issued credential {} to '{}' (id={}, evidence: workshop {} v{} attended)",
        credential.id(), learnerName, recipientId, workshopId, workshopVersion);
    return Optional.of(credential);
  }

  /**
   * Issues and persists a {@code VALID} credential for a <em>completed</em> 1:1
   * coaching session (concept §10.6), carrying {@link Evidence} that the coaching
   * offer version's session was completed + the learner's stable recipient id.
   * Emits one {@code ISSUED} audit event. The caller (the completion flow) guards
   * the atomic complete edge.
   *
   * @param offerId      the completed coaching offer version
   * @param offerVersion the offer version (§16.4)
   * @param recipientId  the learner's stable id
   * @param learnerName  the learner's display name
   * @param title        the credential title
   * @param type         the credential type
   * @param expiresAt    optional expiry; {@code null} for no expiry
   */
  public Optional<Credential> issueForCoaching(UUID offerId, int offerVersion, Long recipientId,
                                               String learnerName, String title, CredentialType type,
                                               java.time.Instant expiresAt) {
    Objects.requireNonNull(offerId, "offerId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    Evidence evidence = Evidence.coachingCompleted(offerId, offerVersion);
    Credential credential = Credential.issue(title, type, recipientId, learnerName,
        issuer.name(), AppClock.now(), expiresAt, evidence);
    repository.save(credential);
    CredentialEventRepositoryProvider.repository().append(CredentialEvent.of(
        credential.id(), CredentialEvent.Action.ISSUED, "system",
        "coaching " + offerId + " v" + offerVersion + " completed"));
    logger().info("Issued credential {} to '{}' (id={}, evidence: coaching {} v{} completed)",
        credential.id(), learnerName, recipientId, offerId, offerVersion);
    return Optional.of(credential);
  }
}
