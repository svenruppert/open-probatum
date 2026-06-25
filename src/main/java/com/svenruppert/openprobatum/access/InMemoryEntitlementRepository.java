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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heap-only {@link EntitlementRepository} for fast upper-layer tests.
 *
 * @since V00.20.00
 */
public final class InMemoryEntitlementRepository implements EntitlementRepository {

  private final Map<String, Entitlement> grants = new ConcurrentHashMap<>();

  @Override
  public void grant(Entitlement entitlement) {
    Objects.requireNonNull(entitlement, "entitlement");
    grants.put(entitlement.key(), entitlement);
  }

  @Override
  public boolean hasGrant(Long userId, UUID offeringId) {
    return userId != null && offeringId != null && grants.containsKey(Entitlement.key(userId, offeringId));
  }

  @Override
  public Collection<Entitlement> forUser(Long userId) {
    Collection<Entitlement> mine = new ArrayList<>();
    for (Entitlement e : grants.values()) {
      if (e.userId().equals(userId)) {
        mine.add(e);
      }
    }
    return mine;
  }
}
