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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Production {@link CredentialEventRepository}. Appends events to the single
 * shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#credentialEvents} — side by side with the credential
 * records in the one app store (jSentinel 00.75.20 {@code JSentinelStoragePair}).
 *
 * @since V00.30.00
 */
public final class EclipseStoreCredentialEventRepository implements CredentialEventRepository {

  @Override
  public synchronized void append(CredentialEvent event) {
    Objects.requireNonNull(event, "event");
    Map<UUID, CredentialEvent> events = AppStorage.appRoot().credentialEvents;
    events.put(event.id(), event);
    AppStorage.app().store(events);
  }

  @Override
  public synchronized List<CredentialEvent> findByCredential(UUID credentialId) {
    return AppStorage.appRoot().credentialEvents.values().stream()
        .filter(e -> e.credentialId().equals(credentialId))
        .sorted(Comparator.comparing(CredentialEvent::timestamp).reversed())
        .toList();
  }

  @Override
  public synchronized List<CredentialEvent> all() {
    return AppStorage.appRoot().credentialEvents.values().stream()
        .sorted(Comparator.comparing(CredentialEvent::timestamp).reversed())
        .toList();
  }
}
