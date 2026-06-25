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

package com.svenruppert.openprobatum.progress;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heap-only {@link ProgressRepository} for fast upper-layer tests.
 *
 * @since V00.20.00
 */
public final class InMemoryProgressRepository implements ProgressRepository {

  private final Map<String, LearnerProgress> store = new ConcurrentHashMap<>();

  @Override
  public void save(LearnerProgress progress) {
    Objects.requireNonNull(progress, "progress");
    store.put(progress.key(), progress);
  }

  @Override
  public Optional<LearnerProgress> find(Long userId, UUID offeringId) {
    if (userId == null || offeringId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(store.get(LearnerProgress.key(userId, offeringId)));
  }
}
