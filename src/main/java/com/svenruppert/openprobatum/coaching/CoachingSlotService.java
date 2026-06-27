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

  /**
   * Books an OPEN slot for {@code recipientId}, atomically — exactly one learner
   * per slot. The OPEN→BOOKED compare-and-set is serialised on {@link #SLOT_LOCK},
   * so two concurrent learners can never both book the same slot (the loser sees a
   * no-longer-OPEN slot → empty). Returns empty when the slot is unknown or not OPEN.
   */
  public Optional<CoachingSlot> book(UUID slotId, Long recipientId, String learnerName) {
    Objects.requireNonNull(slotId, "slotId");
    if (recipientId == null) {
      return Optional.empty();
    }
    synchronized (SLOT_LOCK) {
      return slots.findById(slotId)
          .filter(CoachingSlot::isOpen)
          .map(s -> {
            CoachingSlot booked = s.booked(recipientId, learnerName);
            slots.save(booked);
            logger().info("Coaching slot {} booked by '{}' (id={})", slotId, learnerName, recipientId);
            return booked;
          });
    }
  }

  /** A learner releases their own BOOKED slot, returning it to OPEN for others. */
  public Optional<CoachingSlot> cancelBooking(UUID slotId, Long recipientId) {
    Objects.requireNonNull(slotId, "slotId");
    synchronized (SLOT_LOCK) {
      return slots.findById(slotId)
          .filter(s -> s.isHeldBy(recipientId) && s.isBooked())
          .map(s -> {
            CoachingSlot reopened = s.reopened();
            slots.save(reopened);
            logger().info("Coaching slot {} booking released by {}", slotId, recipientId);
            return reopened;
          });
    }
  }

  /**
   * Completes a BOOKED session on behalf of its coach — the BOOKED→COMPLETED edge
   * (idempotent: a non-BOOKED slot yields empty, so re-completing never re-fires
   * and never double-mints the credential). Only the slot's own coach may complete
   * it. The view mints the coaching credential only on the non-empty result.
   */
  public Optional<CoachingSlot> complete(UUID slotId, Long coachId, String notes) {
    Objects.requireNonNull(slotId, "slotId");
    synchronized (SLOT_LOCK) {
      return slots.findById(slotId)
          .filter(s -> coachId != null && coachId.equals(s.coachId()))
          .filter(CoachingSlot::isBooked)
          .map(s -> {
            CoachingSlot completed = s.completed(notes);
            slots.save(completed);
            logger().info("Coaching slot {} completed by coach {}", slotId, coachId);
            return completed;
          });
    }
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
