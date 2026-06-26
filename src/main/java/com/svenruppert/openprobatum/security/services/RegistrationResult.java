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

package com.svenruppert.openprobatum.security.services;

import com.svenruppert.openprobatum.security.model.AppUser;

/**
 * Outcome of a self-registration attempt (concept §5.1). A sealed result so the
 * view exhaustively maps each case to a message without exposing internals.
 *
 * @since V00.20.00
 */
public sealed interface RegistrationResult {

  /** The account was created; {@code user} is the new Learner. */
  record Success(AppUser user) implements RegistrationResult {
  }

  /** The username is already taken. */
  record UsernameTaken() implements RegistrationResult {
  }

  /**
   * The display name is already taken. The display name is the credential
   * recipient key, so it must be unique (otherwise a learner could match another
   * learner's wallet/credentials).
   */
  record NameTaken() implements RegistrationResult {
  }

  /** The password failed the policy / preflight (too short, blocklisted, breached). */
  record WeakPassword(String reason) implements RegistrationResult {
  }

  /** Username or another required field was missing/blank. */
  record InvalidInput(String reason) implements RegistrationResult {
  }
}
