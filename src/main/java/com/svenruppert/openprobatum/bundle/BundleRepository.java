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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of authored {@link Bundle}s (concept §7.x). Mirrors the lab/catalog
 * repositories: in-memory for fast tests, Eclipse-Store-backed for production.
 * Each version is its own record keyed by {@link Bundle#id()}.
 *
 * @since V00.50.00
 */
public interface BundleRepository {

  /** Inserts or replaces a bundle version by its id. */
  void save(Bundle bundle);

  /** Looks a bundle version up by id. */
  Optional<Bundle> findById(UUID id);

  /** Every bundle version in the store. */
  Collection<Bundle> all();

  /** All versions of the logical bundle {@code lineageId}, lowest version first. */
  default List<Bundle> versionsOf(UUID lineageId) {
    return all().stream()
        .filter(b -> b.lineageId().equals(lineageId))
        .sorted(Comparator.comparingInt(Bundle::version))
        .toList();
  }

  /** The highest version of the logical bundle {@code lineageId}, if any. */
  default Optional<Bundle> latestOf(UUID lineageId) {
    return all().stream()
        .filter(b -> b.lineageId().equals(lineageId))
        .max(Comparator.comparingInt(Bundle::version));
  }
}
