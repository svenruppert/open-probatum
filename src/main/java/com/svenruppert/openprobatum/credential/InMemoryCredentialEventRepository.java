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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Heap-only {@link CredentialEventRepository} for fast upper-layer tests.
 *
 * @since V00.30.00
 */
public final class InMemoryCredentialEventRepository implements CredentialEventRepository {

  private final ConcurrentMap<UUID, CredentialEvent> store = new ConcurrentHashMap<>();

  @Override
  public void append(CredentialEvent event) {
    Objects.requireNonNull(event, "event");
    store.put(event.id(), event);
  }

  @Override
  public List<CredentialEvent> findByCredential(UUID credentialId) {
    return store.values().stream()
        .filter(e -> e.credentialId().equals(credentialId))
        .sorted(Comparator.comparing(CredentialEvent::timestamp)
            .thenComparing(e -> e.id().toString()).reversed())
        .toList();
  }

  @Override
  public List<CredentialEvent> all() {
    return store.values().stream()
        .sorted(Comparator.comparing(CredentialEvent::timestamp)
            .thenComparing(e -> e.id().toString()).reversed())
        .toList();
  }
}
