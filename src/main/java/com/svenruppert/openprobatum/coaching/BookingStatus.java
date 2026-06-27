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

/**
 * The state of a {@link CoachingSlot} (concept §7.x): open for booking, booked by
 * one learner, completed by the coach (the evidence behind a coaching credential),
 * or cancelled.
 *
 * @since V00.60.00
 */
public enum BookingStatus {
  /** Open — bookable by a learner. */
  OPEN,
  /** Booked by exactly one learner, awaiting the session. */
  BOOKED,
  /** Completed by the coach — valid evidence for a coaching credential. */
  COMPLETED,
  /** Cancelled (by the coach, or a learner releasing their booking). */
  CANCELLED
}
