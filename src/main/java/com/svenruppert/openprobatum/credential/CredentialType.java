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
 * The kind of proof a credential represents (concept §10.2). The staggering
 * matters so a participation badge does not read like a competence proof.
 * An organisation-scoped type is intentionally absent — there is no group
 * logic (concept §3.5).
 *
 * @since V00.10.00
 */
public enum CredentialType {

  /** Awareness offering completed and a simple check passed. */
  AWARENESS_BADGE,

  /** Product-related knowledge demonstrated. */
  PRODUCT_KNOWLEDGE_BADGE,

  /** A learning offering or workshop completed. */
  COMPLETION_CERTIFICATE,

  /** Participated in a coaching or event. */
  PARTICIPATION_CERTIFICATE,

  /** An on-demand workshop completed. */
  WORKSHOP_CERTIFICATE,

  /** Knowledge and optionally practical tasks completed. */
  PRACTITIONER_CREDENTIAL,

  /** An existing credential renewed. */
  RENEWAL_CREDENTIAL,

  /** Manually awarded. */
  MANUAL_CREDENTIAL
}
