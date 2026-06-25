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

package com.svenruppert.openprobatum.catalog;

/**
 * How an offering becomes accessible to a learner (concept §7.4). The
 * entitlement check (§12) resolves the actual access; this declares the gate.
 *
 * @since V00.20.00
 */
public enum OfferingVisibility {

  /** Visible and enterable by anyone, including anonymous visitors. */
  PUBLIC,

  /** Visible to any registered (logged-in) user. */
  REGISTERED,

  /** Enterable only with a valid access code. */
  CODE,

  /** Enterable only after a prerequisite offering has been completed. */
  PREREQUISITE
}
