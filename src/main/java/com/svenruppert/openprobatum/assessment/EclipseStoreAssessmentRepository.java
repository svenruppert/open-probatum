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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link AssessmentRepository}. Stores assessments in the single
 * shared application Eclipse-Store, rooted at {@link AppStorage.AppRoot#assessments}.
 *
 * @since V00.20.00
 */
public final class EclipseStoreAssessmentRepository implements AssessmentRepository {

  @Override
  public synchronized void save(Assessment assessment) {
    Objects.requireNonNull(assessment, "assessment");
    Map<UUID, Assessment> store = AppStorage.appRoot().assessments;
    store.put(assessment.id(), assessment);
    AppStorage.app().store(store);
  }

  @Override
  public synchronized Optional<Assessment> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().assessments.get(id));
  }
}
