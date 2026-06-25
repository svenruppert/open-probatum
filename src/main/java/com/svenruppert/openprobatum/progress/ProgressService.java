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

import com.svenruppert.openprobatum.catalog.Offering;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Records and reports a learner's progress through an offering (concept §8.4).
 * Completion is measured against the path's mandatory modules
 * ({@code LearningPath.isComplete}).
 *
 * @since V00.20.00
 */
public final class ProgressService {

  private final ProgressRepository repository;

  public ProgressService(ProgressRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public ProgressService() {
    this(ProgressRepositoryProvider.repository());
  }

  /** Marks {@code moduleId} complete for {@code userId} on {@code offeringId}. */
  public synchronized void markModuleComplete(Long userId, UUID offeringId, UUID moduleId) {
    LearnerProgress current = repository.find(userId, offeringId)
        .orElseGet(() -> LearnerProgress.empty(userId, offeringId));
    repository.save(current.withModuleCompleted(moduleId));
  }

  /** The set of modules the learner has completed on the offering. */
  public Set<UUID> completedModules(Long userId, UUID offeringId) {
    return repository.find(userId, offeringId)
        .map(LearnerProgress::completedModuleIds)
        .orElseGet(Set::of);
  }

  /** Whether the learner has completed every mandatory module of the offering's path. */
  public boolean isPathComplete(Long userId, Offering offering) {
    Objects.requireNonNull(offering, "offering");
    return offering.path().isComplete(completedModules(userId, offering.id()));
  }

  /**
   * Percent of the path's mandatory modules completed (0–100). A path with no
   * mandatory modules counts as 100 % complete.
   */
  public int percentComplete(Long userId, Offering offering) {
    Objects.requireNonNull(offering, "offering");
    var mandatory = offering.path().mandatoryModules();
    if (mandatory.isEmpty()) {
      return 100;
    }
    Set<UUID> done = completedModules(userId, offering.id());
    long completed = mandatory.stream().map(m -> m.id()).filter(done::contains).count();
    return (int) (completed * 100 / mandatory.size());
  }
}
