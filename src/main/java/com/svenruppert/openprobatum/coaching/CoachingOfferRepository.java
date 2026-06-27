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

package com.svenruppert.openprobatum.coaching;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of authored {@link CoachingOffer}s (concept §7.x). Mirrors the
 * workshop/lab repositories: in-memory for fast tests, Eclipse-Store-backed for
 * production.
 *
 * @since V00.60.00
 */
public interface CoachingOfferRepository {

  /** Inserts or replaces an offer version by its id. */
  void save(CoachingOffer offer);

  /** Looks an offer version up by id. */
  Optional<CoachingOffer> findById(UUID id);

  /** Every offer version in the store. */
  Collection<CoachingOffer> all();

  /** All versions of the logical offer {@code lineageId}, lowest version first. */
  default List<CoachingOffer> versionsOf(UUID lineageId) {
    return all().stream()
        .filter(o -> o.lineageId().equals(lineageId))
        .sorted(Comparator.comparingInt(CoachingOffer::version))
        .toList();
  }

  /** The highest version of the logical offer {@code lineageId}, if any. */
  default Optional<CoachingOffer> latestOf(UUID lineageId) {
    return all().stream()
        .filter(o -> o.lineageId().equals(lineageId))
        .max(Comparator.comparingInt(CoachingOffer::version));
  }
}
