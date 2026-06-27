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

package com.svenruppert.openprobatum.catalog;

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.credential.CredentialRepository;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.Evidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Referential-integrity guard for destructive offering operations (entry-review #1,
 * finding&nbsp;1). An {@code Offering} id is referenced from other aggregates —
 * issued credentials' {@link Evidence#sourceId() evidence} (a
 * {@code LEARNING_PATH_COMPLETED} credential pins the offering id+version), other
 * offerings' {@link Offering#prerequisiteOfferingId() prerequisite}, and per-learner
 * entitlements/progress keyed by {@code (userId, offeringId)} — so removing an
 * offering can leave dangling references (broken prerequisite gates,
 * un-revalidatable credentials).
 *
 * <p>The rule this service enforces:
 * <ul>
 *   <li><b>Deactivate</b> (move to {@link ContentStatus#DEPRECATED}/
 *       {@link ContentStatus#ARCHIVED}) is the reference-preserving default — it
 *       hides the offering from the learner catalogue while keeping every
 *       credential/entitlement valid. Always the right choice for published content.</li>
 *   <li><b>Hard delete</b> is permitted <em>only</em> for a {@link ContentStatus#DRAFT}
 *       offering that nothing references. A DRAFT was never published, so by
 *       construction no learner holds an entitlement/progress against it and no
 *       credential was issued from it; the credential + prerequisite scans below are
 *       defence-in-depth (the two references that are scannable with the current
 *       repository APIs — {@code Entitlement}/{@code Progress} repositories expose no
 *       full scan, and the deactivate path preserves them regardless).</li>
 * </ul>
 *
 * @since V00.70.10
 */
public final class CatalogIntegrityService {

  private final CatalogRepository catalog;
  private final CredentialRepository credentials;

  public CatalogIntegrityService(CatalogRepository catalog, CredentialRepository credentials) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.credentials = Objects.requireNonNull(credentials, "credentials");
  }

  public CatalogIntegrityService() {
    this(CatalogRepositoryProvider.repository(), CredentialRepositoryProvider.repository());
  }

  /**
   * The verdict for a destructive operation: whether it is allowed and, if not, the
   * human-readable references blocking it.
   *
   * @param allowed  whether the operation may proceed
   * @param blockers the blocking references (empty when {@code allowed})
   */
  public record IntegrityVerdict(boolean allowed, List<String> blockers) {
    public IntegrityVerdict {
      blockers = List.copyOf(blockers);
    }
  }

  /**
   * Deactivation ({@code DEPRECATED}/{@code ARCHIVED}) is reference-preserving and
   * always allowed by integrity — the {@link ContentStatus} transition validity is
   * enforced separately by {@link CatalogLifecycleService}.
   */
  public boolean canDeactivate(Offering offering) {
    Objects.requireNonNull(offering, "offering");
    return true;
  }

  /** Convenience: {@code true} when {@link #checkHardDelete(Offering)} permits the delete. */
  public boolean mayHardDelete(Offering offering) {
    return checkHardDelete(offering).allowed();
  }

  /**
   * Decides whether {@code offering} may be hard-deleted, listing every blocking
   * reference. Allowed only for a {@link ContentStatus#DRAFT} that no other offering
   * lists as a prerequisite and that no issued credential references.
   */
  public IntegrityVerdict checkHardDelete(Offering offering) {
    Objects.requireNonNull(offering, "offering");
    List<String> blockers = new ArrayList<>();

    if (offering.status() != ContentStatus.DRAFT) {
      blockers.add("offering is " + offering.status()
          + " — only a DRAFT may be deleted; deactivate (DEPRECATED/ARCHIVED) instead");
    }

    UUID id = offering.id();
    long prerequisiteRefs = catalog.all().stream()
        .filter(o -> id.equals(o.prerequisiteOfferingId()))
        .count();
    if (prerequisiteRefs > 0) {
      blockers.add(prerequisiteRefs + " offering(s) list this as a prerequisite");
    }

    long credentialRefs = credentials.all().stream()
        .filter(c -> c.evidence().type() == Evidence.Type.LEARNING_PATH_COMPLETED)
        .filter(c -> id.equals(c.evidence().sourceId()))
        .count();
    if (credentialRefs > 0) {
      blockers.add(credentialRefs + " issued credential(s) reference this offering");
    }

    return new IntegrityVerdict(blockers.isEmpty(), blockers);
  }
}
