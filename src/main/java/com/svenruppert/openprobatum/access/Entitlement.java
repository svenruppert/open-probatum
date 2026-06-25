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

import java.util.Objects;
import java.util.UUID;

/**
 * A stored grant linking a learner to an offering (concept §12). The natural key
 * is {@code (userId, offeringId)}; PUBLIC/REGISTERED access is derived from the
 * offering visibility and needs no stored entitlement.
 *
 * @param userId     the learner's id
 * @param offeringId the offering the learner is entitled to
 * @param reason     why the entitlement was granted
 * @since V00.20.00
 */
public record Entitlement(Long userId, UUID offeringId, EntitlementReason reason) {

  public Entitlement {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(offeringId, "offeringId");
    Objects.requireNonNull(reason, "reason");
  }

  /** The storage key for this grant. */
  public String key() {
    return key(userId, offeringId);
  }

  /** The storage key for a {@code (userId, offeringId)} pair. */
  public static String key(Long userId, UUID offeringId) {
    return userId + ":" + offeringId;
  }
}
