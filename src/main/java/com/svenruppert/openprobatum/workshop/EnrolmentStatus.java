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

/**
 * The state of a learner's {@link WorkshopEnrolment} (concept §7.x): holding a
 * seat, attended (the evidence behind a workshop certificate), a no-show, or
 * cancelled (the seat is freed).
 *
 * @since V00.50.00
 */
public enum EnrolmentStatus {
  /** The learner holds a seat (counts towards capacity). */
  ENROLLED,
  /** The learner attended — valid evidence for a workshop certificate. */
  ATTENDED,
  /** The learner did not attend. */
  NO_SHOW,
  /** The learner cancelled their seat (it is freed for others). */
  CANCELLED
}
