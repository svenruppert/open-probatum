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

  /**
   * Serialises the find→add→save read-modify-write across ALL callers. The views
   * build a {@code new ProgressService()} per click, so an instance
   * {@code synchronized} would lock a throw-away monitor — two near-simultaneous
   * completions could then each read the same snapshot and the second save would
   * clobber the first, silently dropping a completed module (the same reason
   * {@code WorkshopEnrolmentService.SEAT_LOCK} is static).
   */
  private static final Object PROGRESS_LOCK = new Object();

  private final ProgressRepository repository;

  public ProgressService(ProgressRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public ProgressService() {
    this(ProgressRepositoryProvider.repository());
  }

  /** Marks {@code moduleId} complete for {@code userId} on {@code offeringId}. */
  public void markModuleComplete(Long userId, UUID offeringId, UUID moduleId) {
    synchronized (PROGRESS_LOCK) {
      LearnerProgress current = repository.find(userId, offeringId)
          .orElseGet(() -> LearnerProgress.empty(userId, offeringId));
      repository.save(current.withModuleCompleted(moduleId));
    }
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
    // Round to nearest, not truncate: 2 of 3 mandatory modules is 67 %, not 66 %.
    int percent = (int) Math.round(completed * 100.0 / mandatory.size());
    // ...but never round UP to 100 % while a module is still outstanding (e.g.
    // 199 of 200 → 99.5 → 100): 100 % must mean actually complete.
    if (percent >= 100 && completed < mandatory.size()) {
      return 99;
    }
    return percent;
  }
}
