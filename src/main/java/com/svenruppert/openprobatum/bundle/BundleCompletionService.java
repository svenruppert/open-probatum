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

import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepository;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bundle completion + credential claim (concept §10.6 / §16.4). A bundle is
 * complete for a learner when every member offering's learning path is complete
 * ({@link ProgressService#isPathComplete}). The learner then claims a single
 * bundle completion credential carrying {@link Evidence.Type#BUNDLE_COMPLETED}
 * evidence + their stable recipient id. The claim is the exactly-once edge: it is
 * serialised on a shared monitor and refuses a second claim once a credential for
 * this (learner, bundle) already exists — concurrent claims mint at most one.
 *
 * @since V00.50.00
 */
public final class BundleCompletionService implements HasLogger {

  /** Process-wide lock for the claim edge — see the V00.40.00 mint-once pattern. */
  private static final Object CLAIM_LOCK = new Object();

  private final CatalogRepository catalog;
  private final ProgressService progress;
  private final CredentialRepository credentials;
  private final IssuanceService issuance;

  /**
   * For the exactly-once guarantee, {@code credentials} (read by
   * {@link #alreadyClaimed}) and {@code issuance}'s repository (which writes the
   * minted credential) MUST be the same store — the no-arg constructor wires both
   * to {@code CredentialRepositoryProvider.repository()}.
   */
  public BundleCompletionService(CatalogRepository catalog, ProgressService progress,
                                 CredentialRepository credentials, IssuanceService issuance) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.progress = Objects.requireNonNull(progress, "progress");
    this.credentials = Objects.requireNonNull(credentials, "credentials");
    this.issuance = Objects.requireNonNull(issuance, "issuance");
  }

  public BundleCompletionService() {
    this(CatalogRepositoryProvider.repository(), new ProgressService(),
        CredentialRepositoryProvider.repository(),
        new IssuanceService(CredentialRepositoryProvider.repository(), IssuerIdentity.fromConfig()));
  }

  /** Whether {@code learnerId} has completed every member offering's path of {@code bundle}. */
  public boolean isComplete(Long learnerId, Bundle bundle) {
    Objects.requireNonNull(bundle, "bundle");
    if (learnerId == null || bundle.offeringIds().isEmpty()) {
      return false;
    }
    for (UUID offeringId : bundle.offeringIds()) {
      Offering offering = catalog.findById(offeringId).orElse(null);
      if (offering == null || !progress.isPathComplete(learnerId, offering)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Whether a bundle completion credential for this (learner, bundle) already
   * exists. Keyed on the bundle <em>version</em> id ({@code bundle.id()}), matching
   * the version model: a new published version is a new bundle and may be claimed
   * separately, exactly as a new assessment/lab version is a distinct credential.
   */
  public boolean alreadyClaimed(Long learnerId, Bundle bundle) {
    return credentials.all().stream().anyMatch(c ->
        c.isHeldBy(learnerId)
            && c.evidence().type() == Evidence.Type.BUNDLE_COMPLETED
            && bundle.id().equals(c.evidence().sourceId()));
  }

  /**
   * Claims the bundle completion credential for {@code user}, exactly once: mints a
   * {@code COMPLETION_CERTIFICATE} with bundle evidence only when the bundle is
   * complete and not already claimed. Returns the credential, or empty when not
   * eligible or already claimed. The check + mint is atomic on {@link #CLAIM_LOCK}.
   */
  public Optional<Credential> claim(AppUser user, Bundle bundle, String title) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(bundle, "bundle");
    synchronized (CLAIM_LOCK) {
      Long learnerId = user.id();
      if (!isComplete(learnerId, bundle) || alreadyClaimed(learnerId, bundle)) {
        return Optional.empty();
      }
      logger().info("Bundle {} v{} claimed by user {}", bundle.id(), bundle.version(), learnerId);
      return issuance.issueForBundle(bundle.id(), bundle.version(), learnerId, user.name(),
          title, CredentialType.COMPLETION_CERTIFICATE, null);
    }
  }
}
