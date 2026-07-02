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
 * The outcome of an access check for a learner against an offering (§12).
 *
 * @since V00.20.00
 */
public enum AccessDecision {

  /** The learner may enter the offering. */
  GRANTED,

  /** A registered offering — the visitor must sign in first. */
  LOGIN_REQUIRED,

  /** A code-gated offering — a valid access code must be redeemed. */
  CODE_REQUIRED,

  /** A prerequisite-gated offering — the prerequisite must be completed first. */
  PREREQUISITE_REQUIRED,

  /**
   * The offering is not published (DRAFT / IN_REVIEW / ARCHIVED / REPLACED) and
   * is therefore never accessible to a learner, regardless of visibility or any
   * stored grant — learners only ever see PUBLISHED content (§16.2).
   */
  UNAVAILABLE;

  public boolean isGranted() {
    return this == GRANTED;
  }
}
