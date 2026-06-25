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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link CredentialRepository}. Stores credential records in the
 * single shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#credentials} — side by side with the user directory
 * in the one app store, no second manager and no own shutdown hook (jSentinel
 * 00.75.20 {@code JSentinelStoragePair}). Only the credential record is stored,
 * never a PDF (concept §3.2/§10.7).
 *
 * @since V00.10.00
 */
public final class EclipseStoreCredentialRepository implements CredentialRepository {

  @Override
  public synchronized void save(Credential credential) {
    Objects.requireNonNull(credential, "credential");
    Map<UUID, Credential> credentials = AppStorage.appRoot().credentials;
    credentials.put(credential.id(), credential);
    AppStorage.app().store(credentials);
  }

  @Override
  public synchronized Optional<Credential> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().credentials.get(id));
  }

  @Override
  public synchronized Collection<Credential> all() {
    return new ArrayList<>(AppStorage.appRoot().credentials.values());
  }
}
