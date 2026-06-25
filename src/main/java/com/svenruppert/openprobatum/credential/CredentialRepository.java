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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores credential <em>records</em> — never a PDF (concept §3.2, §10.7). The
 * record is the sole source of truth the validation page reads. Decoupled from
 * the backend so a later swap (DB / IAM) does not touch consumers.
 *
 * @since V00.10.00
 */
public interface CredentialRepository {

  /** Persists a new credential or replaces an existing one with the same id. */
  void save(Credential credential);

  /** Looks a credential up by its random id; empty when unknown. */
  Optional<Credential> findById(UUID id);

  /** All stored credentials (defensive copy). */
  Collection<Credential> all();

  /** Releases any underlying resources. No-op for in-memory implementations. */
  default void close() {
  }
}
