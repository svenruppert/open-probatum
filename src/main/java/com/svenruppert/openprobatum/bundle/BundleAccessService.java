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

package com.svenruppert.openprobatum.bundle;

import com.svenruppert.openprobatum.access.EntitlementReason;
import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bundle access (concept §7.x / §12): granting a learner a bundle entitles them
 * to every member offering at once (an {@link EntitlementReason#BUNDLE} grant per
 * member). Resolves the member offerings from the catalog for display.
 *
 * @since V00.50.00
 */
public final class BundleAccessService implements HasLogger {

  private final CatalogRepository catalog;
  private final EntitlementService entitlements;

  public BundleAccessService(CatalogRepository catalog, EntitlementService entitlements) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.entitlements = Objects.requireNonNull(entitlements, "entitlements");
  }

  public BundleAccessService() {
    this(CatalogRepositoryProvider.repository(), new EntitlementService());
  }

  /** Entitles {@code user} to every member offering of {@code bundle} (reason BUNDLE). */
  public void grant(AppUser user, Bundle bundle) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(bundle, "bundle");
    for (UUID offeringId : bundle.offeringIds()) {
      catalog.findById(offeringId)
          .ifPresent(offering -> entitlements.grant(user, offering, EntitlementReason.BUNDLE));
    }
    logger().info("Bundle {} granted to user {} ({} members)",
        bundle.id(), user.id(), bundle.offeringIds().size());
  }

  /** The member offerings of {@code bundle} that resolve in the catalog. */
  public List<Offering> members(Bundle bundle) {
    Objects.requireNonNull(bundle, "bundle");
    return bundle.offeringIds().stream()
        .map(catalog::findById)
        .flatMap(Optional::stream)
        .toList();
  }
}
