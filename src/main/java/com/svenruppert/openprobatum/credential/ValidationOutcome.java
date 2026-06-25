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

import java.util.Optional;

/**
 * The outcome of a validation lookup: a {@link ValidationResult} and, when the
 * credential was found, the record to display the match fields from. For
 * {@link ValidationResult#UNKNOWN} there is no credential.
 *
 * @param result     the verification result
 * @param credential the matched record, or {@code null} when unknown
 * @since V00.10.00
 */
public record ValidationOutcome(ValidationResult result, Credential credential) {

  /** An unknown-credential outcome (id not found). */
  public static ValidationOutcome unknown() {
    return new ValidationOutcome(ValidationResult.UNKNOWN, null);
  }

  /** A found outcome carrying the record for the match fields. */
  public static ValidationOutcome found(ValidationResult result, Credential credential) {
    return new ValidationOutcome(result, java.util.Objects.requireNonNull(credential, "credential"));
  }

  /** The matched credential, if any. */
  public Optional<Credential> credentialOpt() {
    return Optional.ofNullable(credential);
  }
}
