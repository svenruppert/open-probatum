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

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A learner's progress through one offering (concept §8.4): the set of completed
 * module ids. The natural key is {@code (userId, offeringId)}.
 *
 * @param userId             the learner
 * @param offeringId         the offering being worked
 * @param completedModuleIds ids of the modules the learner has completed
 * @since V00.20.00
 */
public record LearnerProgress(Long userId, UUID offeringId, Set<UUID> completedModuleIds) {

  public LearnerProgress {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(offeringId, "offeringId");
    Objects.requireNonNull(completedModuleIds, "completedModuleIds");
    completedModuleIds = Set.copyOf(completedModuleIds);
  }

  /** An empty progress record for {@code (userId, offeringId)}. */
  public static LearnerProgress empty(Long userId, UUID offeringId) {
    return new LearnerProgress(userId, offeringId, Set.of());
  }

  /** A copy with {@code moduleId} added to the completed set. */
  public LearnerProgress withModuleCompleted(UUID moduleId) {
    Objects.requireNonNull(moduleId, "moduleId");
    Set<UUID> next = new java.util.HashSet<>(completedModuleIds);
    next.add(moduleId);
    return new LearnerProgress(userId, offeringId, next);
  }

  /** The storage key for this record. */
  public String key() {
    return key(userId, offeringId);
  }

  /** The storage key for a {@code (userId, offeringId)} pair. */
  public static String key(Long userId, UUID offeringId) {
    return userId + ":" + offeringId;
  }
}
