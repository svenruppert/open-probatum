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
 * The kind of offering in the academy catalog (concept §7.2). The Trust-Core
 * slice exercises {@link #CERTIFICATION_PATH}; the rest are modelled for the
 * later versions that introduce them.
 *
 * @since V00.10.00
 */
public enum OfferingType {

  /** Structured self-study track. */
  LEARNING_PATH,

  /** Learning path with a completion check and a credential. */
  CERTIFICATION_PATH,

  /** Video-based workshop with materials, tasks and completion. */
  ON_DEMAND_WORKSHOP,

  /** Bookable 1:1 offering. */
  COACHING_OFFERING,

  /** Knowledge check with no learning material. */
  ASSESSMENT_ONLY,

  /** A combination of several offerings. */
  BUNDLE,

  /** Refresh of an existing credential. */
  RENEWAL_OFFERING,

  /** A free awareness entry point. */
  FREE_AWARENESS_MODULE
}
