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

import com.svenruppert.dependencies.core.logger.HasLogger;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The coaching-slot flow (concept §7.x): a coach opens bookable slots under a
 * <em>published</em> offer; a learner books an OPEN slot (exactly one learner per
 * slot — the booking is serialised on a shared monitor so two learners never book
 * the same slot, even concurrently); the coach completes a booked session (the
 * mint-once edge, V00.60.00 P006). A slot/booking may be cancelled.
 *
 * @since V00.60.00
 */
public final class CoachingSlotService implements HasLogger {

  /** Process-wide lock for the book + complete edges — see the V00.40.00 mint-once pattern. */
  static final Object SLOT_LOCK = new Object();

  private final CoachingOfferRepository offers;
  private final CoachingSlotRepository slots;

  public CoachingSlotService(CoachingOfferRepository offers, CoachingSlotRepository slots) {
    this.offers = Objects.requireNonNull(offers, "offers");
    this.slots = Objects.requireNonNull(slots, "slots");
  }

  public CoachingSlotService() {
    this(CoachingOfferRepositoryProvider.repository(), CoachingSlotRepositoryProvider.repository());
  }

  /**
   * Opens a bookable slot under a <em>published</em> offer (the coach is taken from
   * the offer). Returns empty (and saves nothing) when the offer is unknown or not
   * published.
   */
  public Optional<CoachingSlot> open(UUID offerId, Instant startsAt, Instant endsAt) {
    Objects.requireNonNull(offerId, "offerId");
    return offers.findById(offerId)
        .filter(CoachingOffer::isPublished)
        .map(offer -> {
          CoachingSlot slot = CoachingSlot.open(offer.id(), offer.coachId(), startsAt, endsAt);
          slots.save(slot);
          logger().info("Coaching slot {} opened under offer {} by coach {}",
              slot.id(), offer.id(), offer.coachId());
          return slot;
        });
  }

  /** Cancels an OPEN or BOOKED slot (coach action). Empty when not this coach's / terminal. */
  public Optional<CoachingSlot> cancelSlot(UUID slotId, Long coachId) {
    Objects.requireNonNull(slotId, "slotId");
    synchronized (SLOT_LOCK) {
      return slots.findById(slotId)
          .filter(s -> coachId != null && coachId.equals(s.coachId()))
          .filter(s -> s.isOpen() || s.isBooked())
          .map(s -> {
            CoachingSlot cancelled = s.cancelled();
            slots.save(cancelled);
            logger().info("Coaching slot {} cancelled by coach {}", slotId, coachId);
            return cancelled;
          });
    }
  }
}
