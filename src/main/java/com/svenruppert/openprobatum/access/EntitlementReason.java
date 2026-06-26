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

package com.svenruppert.openprobatum.access;

/**
 * Why a learner is entitled to an offering (concept §12.1–12.3).
 *
 * @since V00.20.00
 */
public enum EntitlementReason {

  /** Open to everyone — derived from a PUBLIC offering, no stored grant. */
  FREE,

  /** Open to any registered user — derived from a REGISTERED offering. */
  REGISTRATION,

  /** Unlocked by redeeming a valid access code. */
  CODE,

  /** Unlocked by completing a prerequisite offering. */
  PREREQUISITE,

  /** Granted manually by an author / platform admin. */
  MANUAL,

  /** Unlocked as a member offering of a granted bundle (§7.x). */
  BUNDLE
}
