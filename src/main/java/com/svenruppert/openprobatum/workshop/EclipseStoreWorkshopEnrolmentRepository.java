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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link WorkshopEnrolmentRepository}. Stores enrolments in the single
 * shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#workshopEnrolments}.
 *
 * @since V00.50.00
 */
public final class EclipseStoreWorkshopEnrolmentRepository implements WorkshopEnrolmentRepository {

  @Override
  public synchronized void save(WorkshopEnrolment enrolment) {
    Objects.requireNonNull(enrolment, "enrolment");
    Map<UUID, WorkshopEnrolment> enrolments = AppStorage.appRoot().workshopEnrolments;
    enrolments.put(enrolment.id(), enrolment);
    AppStorage.app().store(enrolments);
  }

  @Override
  public synchronized Optional<WorkshopEnrolment> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().workshopEnrolments.get(id));
  }

  @Override
  public synchronized List<WorkshopEnrolment> all() {
    return new ArrayList<>(AppStorage.appRoot().workshopEnrolments.values());
  }
}
