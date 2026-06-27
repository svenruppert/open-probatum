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

import com.svenruppert.openprobatum.security.AppClock;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A concrete bookable time slot under a published {@link CoachingOffer} (concept
 * §7.x). A coach opens slots; exactly one learner books an {@code OPEN} slot
 * (no double-booking); the coach records the session {@code COMPLETED} (the
 * evidence behind a coaching credential) or the slot is {@code CANCELLED}.
 *
 * @param id          the slot id
 * @param offerId     the published coaching offer version this slot belongs to
 * @param coachId     the coach who opened the slot
 * @param startsAt    when the session starts
 * @param endsAt      when the session ends (after {@link #startsAt})
 * @param status      the booking state
 * @param recipientId the booking learner's stable id, or {@code null} while OPEN
 * @param learnerName the booking learner's display name, or {@code null} while OPEN
 * @param notes       the coach's session notes (never null; may be empty)
 * @param bookedAt    when the learner booked, or {@code null} while OPEN
 * @param decidedAt   when the coach completed, or {@code null} until COMPLETED
 * @since V00.60.00
 */
public record CoachingSlot(UUID id, UUID offerId, Long coachId, Instant startsAt, Instant endsAt,
                           BookingStatus status, Long recipientId, String learnerName,
                           String notes, Instant bookedAt, Instant decidedAt) {

  public CoachingSlot {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(offerId, "offerId");
    Objects.requireNonNull(startsAt, "startsAt");
    Objects.requireNonNull(endsAt, "endsAt");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(notes, "notes");
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException("a coaching slot must end after it starts");
    }
  }

  /** A fresh {@code OPEN} slot under {@code offerId}, run by {@code coachId}. */
  public static CoachingSlot open(UUID offerId, Long coachId, Instant startsAt, Instant endsAt) {
    return new CoachingSlot(UUID.randomUUID(), offerId, coachId, startsAt, endsAt,
        BookingStatus.OPEN, null, null, "", null, null);
  }

  /** A copy booked by a learner ({@code OPEN → BOOKED}), stamped now. */
  public CoachingSlot booked(Long bookingLearnerId, String bookingLearnerName) {
    return new CoachingSlot(id, offerId, coachId, startsAt, endsAt, BookingStatus.BOOKED,
        bookingLearnerId, bookingLearnerName, notes, AppClock.now(), decidedAt);
  }

  /** A copy marked {@code COMPLETED} with the coach's notes, stamped now. */
  public CoachingSlot completed(String sessionNotes) {
    return new CoachingSlot(id, offerId, coachId, startsAt, endsAt, BookingStatus.COMPLETED,
        recipientId, learnerName, sessionNotes == null ? "" : sessionNotes, bookedAt, AppClock.now());
  }

  /** A copy returned to {@code OPEN}, clearing the booking (a learner released the slot). */
  public CoachingSlot reopened() {
    return new CoachingSlot(id, offerId, coachId, startsAt, endsAt, BookingStatus.OPEN,
        null, null, notes, null, decidedAt);
  }

  /** A copy marked {@code CANCELLED} (the slot/booking is released). */
  public CoachingSlot cancelled() {
    return new CoachingSlot(id, offerId, coachId, startsAt, endsAt, BookingStatus.CANCELLED,
        recipientId, learnerName, notes, bookedAt, AppClock.now());
  }

  /** @return {@code true} when this slot is open for booking. */
  public boolean isOpen() {
    return status == BookingStatus.OPEN;
  }

  /** @return {@code true} when this slot is booked (awaiting the session). */
  public boolean isBooked() {
    return status == BookingStatus.BOOKED;
  }

  /** @return {@code true} when the session is completed. */
  public boolean isCompleted() {
    return status == BookingStatus.COMPLETED;
  }

  /** Whether this slot's booking belongs to the learner with id {@code userId}. */
  public boolean isHeldBy(Long userId) {
    return recipientId != null && recipientId.equals(userId);
  }

  /** The booking learner's id, when booked. */
  public Optional<Long> recipient() {
    return Optional.ofNullable(recipientId);
  }
}
