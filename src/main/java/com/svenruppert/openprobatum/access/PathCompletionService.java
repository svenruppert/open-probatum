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

package com.svenruppert.openprobatum.access;

import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;

/**
 * Wires the prerequisite gate (concept §12): when a learner finishes an
 * offering's learning path, every offering that lists it as its
 * {@link Offering#prerequisiteOfferingId() prerequisite} is unlocked by a
 * {@link EntitlementReason#PREREQUISITE} grant. Without this producer no code
 * ever created a PREREQUISITE grant, so a prerequisite-gated offering stayed at
 * {@link AccessDecision#PREREQUISITE_REQUIRED} forever — the gate could never
 * open (V00.80.10 P003).
 *
 * <p>Idempotent: {@link EntitlementService#grant} keys on (user, offering), so
 * re-running after each module completion never accumulates duplicate grants.
 *
 * @since V00.80.10
 */
public final class PathCompletionService implements HasLogger {

  private final CatalogRepository catalog;
  private final ProgressService progress;
  private final EntitlementService entitlements;

  public PathCompletionService(CatalogRepository catalog, ProgressService progress,
                               EntitlementService entitlements) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.progress = Objects.requireNonNull(progress, "progress");
    this.entitlements = Objects.requireNonNull(entitlements, "entitlements");
  }

  public PathCompletionService() {
    this(CatalogRepositoryProvider.repository(), new ProgressService(), new EntitlementService());
  }

  /**
   * If {@code user} has completed {@code completed}'s path, grants a PREREQUISITE
   * entitlement for every offering that requires it and returns those unlocked
   * offerings. A no-op (empty list) when the path is not complete or nobody is
   * signed in. Safe to call after every module completion.
   */
  public List<Offering> unlockDependents(AppUser user, Offering completed) {
    Objects.requireNonNull(completed, "completed");
    if (user == null || !progress.isPathComplete(user.id(), completed)) {
      return List.of();
    }
    List<Offering> unlocked = catalog.all().stream()
        .filter(o -> completed.id().equals(o.prerequisiteOfferingId()))
        .toList();
    for (Offering dependent : unlocked) {
      entitlements.grant(user, dependent, EntitlementReason.PREREQUISITE);
      logger().info("Prerequisite unlocked: user={}, offering={} (completed prerequisite {})",
          user.id(), dependent.id(), completed.id());
    }
    return unlocked;
  }
}
