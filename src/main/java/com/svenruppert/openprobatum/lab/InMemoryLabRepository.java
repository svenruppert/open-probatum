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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Heap-only {@link LabRepository} for fast upper-layer tests.
 *
 * @since V00.40.00
 */
public final class InMemoryLabRepository implements LabRepository {

  private final ConcurrentMap<UUID, Lab> store = new ConcurrentHashMap<>();

  @Override
  public void save(Lab lab) {
    Objects.requireNonNull(lab, "lab");
    store.put(lab.id(), lab);
  }

  @Override
  public Optional<Lab> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Collection<Lab> all() {
    return new ArrayList<>(store.values());
  }
}
