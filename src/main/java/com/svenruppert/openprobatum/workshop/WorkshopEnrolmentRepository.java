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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of {@link WorkshopEnrolment}s (concept §7.x). Mirrors the lab-submission
 * repository: in-memory for fast tests, Eclipse-Store-backed for production.
 *
 * @since V00.50.00
 */
public interface WorkshopEnrolmentRepository {

  /** Inserts or replaces an enrolment by its id. */
  void save(WorkshopEnrolment enrolment);

  /** Looks an enrolment up by id. */
  Optional<WorkshopEnrolment> findById(UUID id);

  /** Every enrolment in the store. */
  List<WorkshopEnrolment> all();

  /** A learner's own enrolments (own-data, §3.6), newest first. */
  default List<WorkshopEnrolment> forLearner(Long recipientId) {
    return all().stream()
        .filter(e -> e.recipientId().equals(recipientId))
        .sorted(Comparator.comparing(WorkshopEnrolment::enrolledAt).reversed())
        .toList();
  }

  /** All enrolments for a workshop. */
  default List<WorkshopEnrolment> forWorkshop(UUID workshopId) {
    return all().stream()
        .filter(e -> e.workshopId().equals(workshopId))
        .toList();
  }

  /** How many seats are currently occupied (ENROLLED) for a workshop. */
  default long activeCount(UUID workshopId) {
    return all().stream()
        .filter(e -> e.workshopId().equals(workshopId) && e.isActive())
        .count();
  }
}
