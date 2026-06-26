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

package com.svenruppert.openprobatum.lab;

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link LabRepository}. Stores lab versions in the single shared
 * application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#labs}.
 *
 * @since V00.40.00
 */
public final class EclipseStoreLabRepository implements LabRepository {

  @Override
  public synchronized void save(Lab lab) {
    Objects.requireNonNull(lab, "lab");
    Map<UUID, Lab> labs = AppStorage.appRoot().labs;
    labs.put(lab.id(), lab);
    AppStorage.app().store(labs);
  }

  @Override
  public synchronized Optional<Lab> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().labs.get(id));
  }

  @Override
  public synchronized Collection<Lab> all() {
    return new ArrayList<>(AppStorage.appRoot().labs.values());
  }
}
