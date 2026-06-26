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

import java.util.List;
import java.util.UUID;

/**
 * Append-only store of the credential audit trail (concept §17.3). Mirrors the
 * other repositories: an in-memory implementation for fast upper-layer tests and
 * an Eclipse-Store-backed one for production. The trail is never mutated or
 * deleted — events are only appended.
 *
 * @since V00.30.00
 */
public interface CredentialEventRepository {

  /** Appends an event to the trail. */
  void append(CredentialEvent event);

  /** Every event for {@code credentialId}, newest first. */
  List<CredentialEvent> findByCredential(UUID credentialId);

  /** The whole trail, newest first. */
  List<CredentialEvent> all();
}
