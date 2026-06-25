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
 * Stored lifecycle status of a credential — level 1 of the three-layer
 * status model (concept §10.5). These are the values the operator sets and
 * the platform persists. The computed validity ({@link EffectiveStatus},
 * level 2) and the verification result (level 3) are derived at read time,
 * never stored.
 *
 * @since V00.10.00
 */
public enum CredentialStatus {

  /** Issued and currently valid, unless an expiry date has been passed. */
  VALID,

  /** Withdrawn — takes effect immediately on the validation page. */
  REVOKED,

  /** Temporarily deactivated. */
  SUSPENDED,

  /** Replaced by a newer credential (see {@code supersededBy}). */
  SUPERSEDED
}
