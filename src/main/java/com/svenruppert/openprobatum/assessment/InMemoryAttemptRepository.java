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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heap-only {@link AttemptRepository} for fast upper-layer tests.
 *
 * @since V00.20.00
 */
public final class InMemoryAttemptRepository implements AttemptRepository {

  private final Map<UUID, Attempt> store = new ConcurrentHashMap<>();

  @Override
  public void save(Attempt attempt) {
    Objects.requireNonNull(attempt, "attempt");
    store.put(attempt.id(), attempt);
  }

  @Override
  public List<Attempt> forLearner(String learnerName, UUID assessmentId) {
    List<Attempt> mine = new ArrayList<>();
    for (Attempt a : store.values()) {
      if (a.learnerName().equals(learnerName) && a.assessmentId().equals(assessmentId)) {
        mine.add(a);
      }
    }
    return mine;
  }

  @Override
  public List<Attempt> forAssessment(UUID assessmentId) {
    List<Attempt> all = new ArrayList<>();
    for (Attempt a : store.values()) {
      if (a.assessmentId().equals(assessmentId)) {
        all.add(a);
      }
    }
    return all;
  }
}
