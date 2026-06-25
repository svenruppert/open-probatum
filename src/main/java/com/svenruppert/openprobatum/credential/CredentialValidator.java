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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The validation logic behind the public verification portal (concept §11).
 * Looks a credential up by id and maps its read-time {@link EffectiveStatus} to
 * a {@link ValidationResult}; an unknown id yields {@link ValidationResult#UNKNOWN}.
 * The validation page is the sole source of truth (§11.1) — this reads the
 * stored record, never a presented document.
 *
 * @since V00.10.00
 */
public final class CredentialValidator {

  private final CredentialRepository repository;

  public CredentialValidator(CredentialRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  /** Validates the credential with {@code id} as of {@code now}. */
  public ValidationOutcome validate(UUID id, Instant now) {
    Objects.requireNonNull(now, "now");
    if (id == null) {
      return ValidationOutcome.unknown();
    }
    return repository.findById(id)
        .map(c -> ValidationOutcome.found(resultFor(c.effectiveStatusAt(now)), c))
        .orElseGet(ValidationOutcome::unknown);
  }

  /** Maps the computed effective status to a verification result. */
  static ValidationResult resultFor(EffectiveStatus status) {
    return switch (status) {
      case VALID -> ValidationResult.VALID;
      case EXPIRED -> ValidationResult.EXPIRED;
      case REVOKED -> ValidationResult.REVOKED;
      case SUSPENDED -> ValidationResult.SUSPENDED;
      case SUPERSEDED -> ValidationResult.SUPERSEDED;
    };
  }
}
