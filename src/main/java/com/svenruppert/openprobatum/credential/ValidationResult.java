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
 * The result of a verification (concept §11.7) — level 3 of the status model.
 * This is the outcome of the <em>check</em>, not a stored value. The first five
 * derive from a credential's {@link EffectiveStatus}; {@link #UNKNOWN} means the
 * id was not found; {@link #MISMATCH} ("ungültig") is the verifier's verdict
 * when a presented document's fields differ from the official record (§11.6) —
 * the page shows the match-rule so the verifier can reach it.
 *
 * @since V00.10.00
 */
public enum ValidationResult {

  VALID("validate.result.valid", "Valid"),
  EXPIRED("validate.result.expired", "Expired"),
  REVOKED("validate.result.revoked", "Revoked"),
  SUSPENDED("validate.result.suspended", "Suspended"),
  SUPERSEDED("validate.result.superseded", "Superseded"),
  UNKNOWN("validate.result.unknown", "Unknown credential"),
  MISMATCH("validate.result.mismatch", "Invalid — details do not match");

  private final String messageKey;
  private final String englishLabel;

  ValidationResult(String messageKey, String englishLabel) {
    this.messageKey = messageKey;
    this.englishLabel = englishLabel;
  }

  /** i18n key for the result label. */
  public String messageKey() {
    return messageKey;
  }

  /** English fallback label. */
  public String englishLabel() {
    return englishLabel;
  }
}
