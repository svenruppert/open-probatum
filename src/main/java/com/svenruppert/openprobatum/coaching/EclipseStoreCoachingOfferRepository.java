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

package com.svenruppert.openprobatum.coaching;

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link CoachingOfferRepository}. Stores offer versions in the single
 * shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#coachingOffers}.
 *
 * @since V00.60.00
 */
public final class EclipseStoreCoachingOfferRepository implements CoachingOfferRepository {

  @Override
  public synchronized void save(CoachingOffer offer) {
    Objects.requireNonNull(offer, "offer");
    Map<UUID, CoachingOffer> offers = AppStorage.appRoot().coachingOffers;
    offers.put(offer.id(), offer);
    AppStorage.app().store(offers);
  }

  @Override
  public synchronized Optional<CoachingOffer> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().coachingOffers.get(id));
  }

  @Override
  public synchronized Collection<CoachingOffer> all() {
    return new ArrayList<>(AppStorage.appRoot().coachingOffers.values());
  }
}
