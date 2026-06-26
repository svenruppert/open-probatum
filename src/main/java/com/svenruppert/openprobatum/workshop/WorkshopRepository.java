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

package com.svenruppert.openprobatum.workshop;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of authored {@link Workshop}s (concept §7.x). Mirrors the bundle/lab
 * repositories: in-memory for fast tests, Eclipse-Store-backed for production.
 *
 * @since V00.50.00
 */
public interface WorkshopRepository {

  /** Inserts or replaces a workshop version by its id. */
  void save(Workshop workshop);

  /** Looks a workshop version up by id. */
  Optional<Workshop> findById(UUID id);

  /** Every workshop version in the store. */
  Collection<Workshop> all();

  /** All versions of the logical workshop {@code lineageId}, lowest version first. */
  default List<Workshop> versionsOf(UUID lineageId) {
    return all().stream()
        .filter(w -> w.lineageId().equals(lineageId))
        .sorted(Comparator.comparingInt(Workshop::version))
        .toList();
  }

  /** The highest version of the logical workshop {@code lineageId}, if any. */
  default Optional<Workshop> latestOf(UUID lineageId) {
    return all().stream()
        .filter(w -> w.lineageId().equals(lineageId))
        .max(Comparator.comparingInt(Workshop::version));
  }
}
