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

package com.svenruppert.openprobatum.bundle;

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link BundleRepository}. Stores bundle versions in the single shared
 * application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#bundles}.
 *
 * @since V00.50.00
 */
public final class EclipseStoreBundleRepository implements BundleRepository {

  @Override
  public synchronized void save(Bundle bundle) {
    Objects.requireNonNull(bundle, "bundle");
    Map<UUID, Bundle> bundles = AppStorage.appRoot().bundles;
    bundles.put(bundle.id(), bundle);
    AppStorage.app().store(bundles);
  }

  @Override
  public synchronized Optional<Bundle> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().bundles.get(id));
  }

  @Override
  public synchronized Collection<Bundle> all() {
    return new ArrayList<>(AppStorage.appRoot().bundles.values());
  }
}
