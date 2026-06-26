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

package com.svenruppert.openprobatum.assessment;

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Production {@link AttemptRepository}. Stores attempts in the single shared
 * application Eclipse-Store, rooted at {@link AppStorage.AppRoot#attempts}.
 *
 * @since V00.20.00
 */
public final class EclipseStoreAttemptRepository implements AttemptRepository {

  @Override
  public synchronized void save(Attempt attempt) {
    Objects.requireNonNull(attempt, "attempt");
    Map<UUID, Attempt> store = AppStorage.appRoot().attempts;
    store.put(attempt.id(), attempt);
    AppStorage.app().store(store);
  }

  @Override
  public synchronized List<Attempt> forLearner(String learnerName, UUID assessmentId) {
    List<Attempt> mine = new ArrayList<>();
    for (Attempt a : AppStorage.appRoot().attempts.values()) {
      if (a.learnerName().equals(learnerName) && a.assessmentId().equals(assessmentId)) {
        mine.add(a);
      }
    }
    return mine;
  }
}
