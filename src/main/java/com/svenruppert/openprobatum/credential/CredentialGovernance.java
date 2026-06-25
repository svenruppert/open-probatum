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
    return transition(id, c -> c.withStatus(CredentialStatus.REVOKED), "revoked");
  }

  /** Suspends a credential (temporary deactivation). */
  public Optional<Credential> suspend(UUID id) {
    return transition(id, c -> c.withStatus(CredentialStatus.SUSPENDED), "suspended");
  }

  /** Reinstates a suspended credential back to {@code VALID}. */
  public Optional<Credential> reinstate(UUID id) {
    return transition(id, c -> c.withStatus(CredentialStatus.VALID), "reinstated");
  }

  /** Marks a credential {@code SUPERSEDED}, pointing at the replacing credential. */
  public Optional<Credential> supersede(UUID id, UUID replacementId) {
    Objects.requireNonNull(replacementId, "replacementId");
    return transition(id, c -> c.supersededByCredential(replacementId), "superseded");
  }

  private Optional<Credential> transition(UUID id, UnaryOperator<Credential> change, String action) {
    Objects.requireNonNull(id, "id");
    return repository.findById(id).map(current -> {
      Credential updated = change.apply(current);
      repository.save(updated);
      logger().info("Credential {} {}", id, action);
      return updated;
    });
  }
}
