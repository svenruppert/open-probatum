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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heap-only {@link AssessmentRepository} for fast upper-layer tests.
 *
 * @since V00.20.00
 */
public final class InMemoryAssessmentRepository implements AssessmentRepository {

  private final Map<UUID, Assessment> store = new ConcurrentHashMap<>();

  @Override
  public void save(Assessment assessment) {
    Objects.requireNonNull(assessment, "assessment");
    store.put(assessment.id(), assessment);
  }

  @Override
  public Optional<Assessment> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public java.util.Collection<Assessment> all() {
    return new java.util.ArrayList<>(store.values());
  }
}
