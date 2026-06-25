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

package com.svenruppert.openprobatum.credential;

/**
 * Computed validity of a credential — level 2 of the three-layer status
 * model (concept §10.5). Derived at read time from the stored
 * {@link CredentialStatus} plus the optional expiry date; never persisted.
 * {@link #EXPIRED} exists only here, computed from the expiry vs. now.
 *
 * @since V00.10.00
 */
public enum EffectiveStatus {

  /** Stored {@code VALID} and not past its expiry date. */
  VALID,

  /** Stored {@code VALID} but the expiry date has passed (computed). */
  EXPIRED,

  /** Stored {@code REVOKED}. */
  REVOKED,

  /** Stored {@code SUSPENDED}. */
  SUSPENDED,

  /** Stored {@code SUPERSEDED}. */
  SUPERSEDED
}
