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

package com.svenruppert.openprobatum.catalog;

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link CatalogRepository}. Stores offerings in the single shared
 * application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#offerings} — side by side with users + credentials
 * in the one app store (jSentinel 00.75.20 {@code JSentinelStoragePair}).
 *
 * @since V00.20.00
 */
public final class EclipseStoreCatalogRepository implements CatalogRepository {

  @Override
  public synchronized void save(Offering offering) {
    Objects.requireNonNull(offering, "offering");
    Map<UUID, Offering> offerings = AppStorage.appRoot().offerings;
    offerings.put(offering.id(), offering);
    AppStorage.app().store(offerings);
  }

  @Override
  public synchronized Optional<Offering> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().offerings.get(id));
  }

  @Override
  public synchronized Collection<Offering> all() {
    return new ArrayList<>(AppStorage.appRoot().offerings.values());
  }
}
