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

package com.svenruppert.openprobatum.workshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Heap-only {@link WorkshopEnrolmentRepository} for fast upper-layer tests.
 *
 * @since V00.50.00
 */
public final class InMemoryWorkshopEnrolmentRepository implements WorkshopEnrolmentRepository {

  private final ConcurrentMap<UUID, WorkshopEnrolment> store = new ConcurrentHashMap<>();

  @Override
  public void save(WorkshopEnrolment enrolment) {
    Objects.requireNonNull(enrolment, "enrolment");
    store.put(enrolment.id(), enrolment);
  }

  @Override
  public Optional<WorkshopEnrolment> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<WorkshopEnrolment> all() {
    return new ArrayList<>(store.values());
  }
}
