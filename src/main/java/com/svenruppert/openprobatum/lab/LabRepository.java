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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of authored {@link Lab}s (concept §9.x). Mirrors the question/catalog
 * repositories: an in-memory implementation for fast upper-layer tests and an
 * Eclipse-Store-backed one for production. Each version is its own record keyed
 * by {@link Lab#id()}; {@link #versionsOf} / {@link #latestOf} resolve a logical
 * lab by its {@code lineageId}.
 *
 * @since V00.40.00
 */
public interface LabRepository {

  /** Inserts or replaces a lab version by its id. */
  void save(Lab lab);

  /** Looks a lab version up by id. */
  Optional<Lab> findById(UUID id);

  /** Every lab version in the store. */
  Collection<Lab> all();

  /** All versions of the logical lab {@code lineageId}, lowest version first. */
  default List<Lab> versionsOf(UUID lineageId) {
    return all().stream()
        .filter(l -> l.lineageId().equals(lineageId))
        .sorted(Comparator.comparingInt(Lab::version))
        .toList();
  }

  /** The highest version of the logical lab {@code lineageId}, if any. */
  default Optional<Lab> latestOf(UUID lineageId) {
    return all().stream()
        .filter(l -> l.lineageId().equals(lineageId))
        .max(Comparator.comparingInt(Lab::version));
  }
}
