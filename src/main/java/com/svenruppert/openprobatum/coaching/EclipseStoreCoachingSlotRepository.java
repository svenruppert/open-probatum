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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link CoachingSlotRepository}. Stores slots in the single shared
 * application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#coachingSlots}.
 *
 * @since V00.60.00
 */
public final class EclipseStoreCoachingSlotRepository implements CoachingSlotRepository {

  @Override
  public synchronized void save(CoachingSlot slot) {
    Objects.requireNonNull(slot, "slot");
    Map<UUID, CoachingSlot> slots = AppStorage.appRoot().coachingSlots;
    slots.put(slot.id(), slot);
    AppStorage.app().store(slots);
  }

  @Override
  public synchronized Optional<CoachingSlot> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().coachingSlots.get(id));
  }

  @Override
  public synchronized List<CoachingSlot> all() {
    return new ArrayList<>(AppStorage.appRoot().coachingSlots.values());
  }
}
