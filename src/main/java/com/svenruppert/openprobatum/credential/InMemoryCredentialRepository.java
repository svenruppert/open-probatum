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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heap-backed {@link CredentialRepository} with no I/O — for fast tests of the
 * layers above persistence.
 *
 * @since V00.10.00
 */
public final class InMemoryCredentialRepository implements CredentialRepository {

  private final Map<UUID, Credential> store = new ConcurrentHashMap<>();

  @Override
  public void save(Credential credential) {
    Objects.requireNonNull(credential, "credential");
    store.put(credential.id(), credential);
  }

  @Override
  public Optional<Credential> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Collection<Credential> all() {
    return new ArrayList<>(store.values());
  }
}
