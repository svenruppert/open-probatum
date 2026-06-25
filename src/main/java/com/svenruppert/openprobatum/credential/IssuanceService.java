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
    Objects.requireNonNull(attempt, "attempt");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    if (!attempt.passed()) {
      logger().debug("No credential issued — attempt {} did not pass", attempt.id());
      return Optional.empty();
    }
    Credential credential = Credential.issue(title, type, attempt.learnerName(),
        issuer.name(), AppClock.now(), expiresAt);
    repository.save(credential);
    logger().info("Issued credential {} to '{}' (evidence: assessment {} v{} passed)",
        credential.id(), attempt.learnerName(),
        attempt.assessmentId(), attempt.assessmentVersion());
    return Optional.of(credential);
  }
}
