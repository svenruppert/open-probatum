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

import java.util.Collection;
import java.util.UUID;

/**
 * Store of {@link Entitlement} grants (concept §12). Only CODE / PREREQUISITE /
 * MANUAL grants are stored; FREE / REGISTRATION access is derived from the
 * offering visibility.
 *
 * @since V00.20.00
 */
public interface EntitlementRepository {

  /** Inserts or replaces the grant for {@code (userId, offeringId)}. */
  void grant(Entitlement entitlement);

  /** @return {@code true} if {@code userId} holds a grant for {@code offeringId}. */
  boolean hasGrant(Long userId, UUID offeringId);

  /** All grants held by {@code userId}. */
  Collection<Entitlement> forUser(Long userId);
}
