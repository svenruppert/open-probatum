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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heap-only {@link CatalogRepository} for fast upper-layer tests.
 *
 * @since V00.20.00
 */
public final class InMemoryCatalogRepository implements CatalogRepository {

  private final Map<UUID, Offering> offerings = new ConcurrentHashMap<>();

  @Override
  public void save(Offering offering) {
    Objects.requireNonNull(offering, "offering");
    offerings.put(offering.id(), offering);
  }

  @Override
  public Optional<Offering> findById(UUID id) {
    return Optional.ofNullable(offerings.get(id));
  }

  @Override
  public Collection<Offering> all() {
    return new ArrayList<>(offerings.values());
  }
}
