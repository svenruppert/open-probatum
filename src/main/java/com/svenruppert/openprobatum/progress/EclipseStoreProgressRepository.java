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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link ProgressRepository}. Stores progress records in the single
 * shared application Eclipse-Store, rooted at {@link AppStorage.AppRoot#progress}.
 *
 * @since V00.20.00
 */
public final class EclipseStoreProgressRepository implements ProgressRepository {

  @Override
  public synchronized void save(LearnerProgress progress) {
    Objects.requireNonNull(progress, "progress");
    Map<String, LearnerProgress> store = AppStorage.appRoot().progress;
    store.put(progress.key(), progress);
    AppStorage.app().store(store);
  }

  @Override
  public synchronized Optional<LearnerProgress> find(Long userId, UUID offeringId) {
    if (userId == null || offeringId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(AppStorage.appRoot().progress.get(LearnerProgress.key(userId, offeringId)));
  }
}
