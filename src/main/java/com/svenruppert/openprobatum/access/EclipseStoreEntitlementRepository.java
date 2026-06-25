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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Production {@link EntitlementRepository}. Stores grants in the single shared
 * application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#entitlements} (key {@code userId:offeringId}).
 *
 * @since V00.20.00
 */
public final class EclipseStoreEntitlementRepository implements EntitlementRepository {

  @Override
  public synchronized void grant(Entitlement entitlement) {
    Objects.requireNonNull(entitlement, "entitlement");
    Map<String, Entitlement> grants = AppStorage.appRoot().entitlements;
    grants.put(entitlement.key(), entitlement);
    AppStorage.app().store(grants);
  }

  @Override
  public synchronized boolean hasGrant(Long userId, UUID offeringId) {
    return userId != null && offeringId != null
        && AppStorage.appRoot().entitlements.containsKey(Entitlement.key(userId, offeringId));
  }

  @Override
  public synchronized Collection<Entitlement> forUser(Long userId) {
    Collection<Entitlement> mine = new ArrayList<>();
    for (Entitlement e : AppStorage.appRoot().entitlements.values()) {
      if (e.userId().equals(userId)) {
        mine.add(e);
      }
    }
    return mine;
  }
}
