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
    logger().info("Issued credential {} to '{}' (id={}, evidence: assessment {} v{} passed)",
        credential.id(), attempt.learnerName(), recipientId,
        attempt.assessmentId(), attempt.assessmentVersion());
    return Optional.of(credential);
  }
}
