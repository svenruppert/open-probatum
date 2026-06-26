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
import com.svenruppert.openprobatum.security.AppClock;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Credential-Manager governance actions (concept §10.9, §17.4): revoke,
 * suspend and supersede. Each persists the new stored status through the
 * {@link CredentialRepository}, which the validation page reads — so the change
 * takes effect <em>immediately</em> on the public page (the central advantage
 * of the online-verification model, §10.9). The old record is never deleted.
 *
 * @since V00.10.00
 */
public final class CredentialGovernance implements HasLogger {

  private final CredentialRepository repository;

  public CredentialGovernance(CredentialRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  /** Revokes a credential ({@code VALID/...} → {@code REVOKED}). */
  public Optional<Credential> revoke(UUID id) {
    return transition(id, c -> c.withStatus(CredentialStatus.REVOKED),
        CredentialEvent.Action.REVOKED, "");
  }

  /** Suspends a credential (temporary deactivation). */
  public Optional<Credential> suspend(UUID id) {
    return transition(id, c -> c.withStatus(CredentialStatus.SUSPENDED),
        CredentialEvent.Action.SUSPENDED, "");
  }

  /** Reinstates a suspended credential back to {@code VALID}. */
  public Optional<Credential> reinstate(UUID id) {
    return transition(id, c -> c.withStatus(CredentialStatus.VALID),
        CredentialEvent.Action.REINSTATED, "");
  }

  /** Marks a credential {@code SUPERSEDED}, pointing at the replacing credential. */
  public Optional<Credential> supersede(UUID id, UUID replacementId) {
    Objects.requireNonNull(replacementId, "replacementId");
    return transition(id, c -> c.supersededByCredential(replacementId),
        CredentialEvent.Action.SUPERSEDED, "by " + replacementId);
  }

  /**
   * Re-issues (renews) a credential (concept §10.9): mints a fresh {@code VALID}
   * successor with the same recipient + basis and an optional new expiry, then
   * marks the predecessor {@code SUPERSEDED} pointing at the successor. The
   * predecessor is never deleted — both stay findable. Returns the successor, or
   * empty when the predecessor id is unknown.
   *
   * @param predecessorId the credential being renewed
   * @param newExpiresAt  the successor's optional expiry; {@code null} for none
   */
  public synchronized Optional<Credential> reissue(UUID predecessorId, java.time.Instant newExpiresAt) {
    Objects.requireNonNull(predecessorId, "predecessorId");
    return repository.findById(predecessorId).map(predecessor -> {
      Credential successor = predecessor.renew(AppClock.now(), newExpiresAt);
      repository.save(successor);
      repository.save(predecessor.supersededByCredential(successor.id()));
      logger().info("Credential {} reissued as {}", predecessorId, successor.id());
      appendEvent(predecessorId, CredentialEvent.Action.REISSUED, "as " + successor.id());
      return successor;
    });
  }

  private Optional<Credential> transition(UUID id, UnaryOperator<Credential> change,
                                          CredentialEvent.Action action, String detail) {
    Objects.requireNonNull(id, "id");
    return repository.findById(id).map(current -> {
      Credential updated = change.apply(current);
      repository.save(updated);
      logger().info("Credential {} {}", id, action);
      appendEvent(id, action, detail);
      return updated;
    });
  }

  /** Appends one audit-trail event for a governance action (§17.3). */
  private void appendEvent(UUID credentialId, CredentialEvent.Action action, String detail) {
    CredentialEventRepositoryProvider.repository().append(
        CredentialEvent.of(credentialId, action, ACTOR, detail));
  }

  /** The coarse actor recorded for governance actions in this slice. */
  private static final String ACTOR = "credential-manager";
}
