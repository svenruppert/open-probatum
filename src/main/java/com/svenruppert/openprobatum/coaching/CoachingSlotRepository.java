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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of {@link CoachingSlot}s (concept §7.x). Mirrors the workshop-enrolment
 * repository: in-memory for fast tests, Eclipse-Store-backed for production.
 *
 * @since V00.60.00
 */
public interface CoachingSlotRepository {

  /** Inserts or replaces a slot by its id. */
  void save(CoachingSlot slot);

  /** Looks a slot up by id. */
  Optional<CoachingSlot> findById(UUID id);

  /** Every slot in the store. */
  List<CoachingSlot> all();

  /** All slots under an offer, soonest first. */
  default List<CoachingSlot> forOffer(UUID offerId) {
    return all().stream()
        .filter(s -> s.offerId().equals(offerId))
        .sorted(Comparator.comparing(CoachingSlot::startsAt))
        .toList();
  }

  /** A coach's slots. */
  default List<CoachingSlot> forCoach(Long coachId) {
    return all().stream()
        .filter(s -> coachId != null && coachId.equals(s.coachId()))
        .toList();
  }

  /** A learner's own bookings (own-data, §3.6), soonest first. */
  default List<CoachingSlot> forLearner(Long recipientId) {
    return all().stream()
        .filter(s -> s.isHeldBy(recipientId))
        .sorted(Comparator.comparing(CoachingSlot::startsAt))
        .toList();
  }

  /** All OPEN slots under an offer (bookable), soonest first. */
  default List<CoachingSlot> openSlotsOf(UUID offerId) {
    return forOffer(offerId).stream().filter(CoachingSlot::isOpen).toList();
  }
}
