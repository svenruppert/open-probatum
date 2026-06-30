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

import com.svenruppert.openprobatum.content.ContentAuthorship;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.credential.CredentialRepository;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The author's offering-management orchestrator (concept §16.1): the single
 * collaborator {@code AuthorView} drives, so the CRUD + path-assembly logic is
 * unit-testable independently of the Vaadin UI. It composes the existing pieces —
 * {@link CatalogLifecycleService} (status transitions), {@link CatalogIntegrityService}
 * (the delete/deactivate guard, P006) and the {@link ContentAuthorship} registry —
 * over a {@link CatalogRepository}.
 *
 * <p>Editing rule: a DRAFT is edited in place; a non-DRAFT (PUBLISHED) offering is
 * branched via {@link Offering#asNewVersion()} into a fresh DRAFT version, so a
 * published record — whose {@code (offeringId, version)} an issued credential's
 * evidence may pin — is never rewritten under its own id.
 *
 * @since V00.70.10
 */
public final class OfferingAuthoringService {

  private final CatalogRepository catalog;
  private final ContentAuthorship authorship;
  private final CatalogLifecycleService lifecycle;
  private final CatalogIntegrityService integrity;

  public OfferingAuthoringService(CatalogRepository catalog, ContentAuthorship authorship,
                                  CredentialRepository credentials) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.authorship = Objects.requireNonNull(authorship, "authorship");
    this.lifecycle = new CatalogLifecycleService(catalog);
    this.integrity = new CatalogIntegrityService(catalog,
        Objects.requireNonNull(credentials, "credentials"));
  }

  public OfferingAuthoringService() {
    this(CatalogRepositoryProvider.repository(), ContentAuthorshipProvider.registry(),
        CredentialRepositoryProvider.repository());
  }

  /**
   * Creates a fresh DRAFT offering from the ordered modules and records authorship.
   * The offering is <em>not</em> auto-submitted — the author submits it explicitly
   * via {@link #submitForReview(UUID)}.
   */
  public Offering createDraft(String title, String description, OfferingVisibility visibility,
                              String accessCode, UUID prerequisiteOfferingId,
                              List<Module> modules, Long authorId) {
    LearningPath path = new LearningPath(title, modules);
    Offering offering = build(title, description, visibility, accessCode,
        prerequisiteOfferingId, path);
    catalog.save(offering);
    if (authorId != null) {
      authorship.recordAuthor(offering.lineageId(), authorId);
    }
    return offering;
  }

  /**
   * Saves edited details: in place when {@code existing} is a DRAFT, otherwise as a
   * fresh DRAFT version (same lineage) so a published offering is never rewritten.
   * Returns the saved offering.
   */
  public Offering saveEdit(Offering existing, String title, String description,
                           OfferingVisibility visibility, String accessCode,
                           UUID prerequisiteOfferingId, List<Module> modules) {
    Objects.requireNonNull(existing, "existing");
    LearningPath path = new LearningPath(title, modules);
    Offering base = existing.status() == ContentStatus.DRAFT ? existing : existing.asNewVersion();
    Offering edited = base.withDetails(title, description, visibility, accessCode,
        prerequisiteOfferingId, path);
    catalog.save(edited);
    return edited;
  }

  /** Submits a DRAFT offering for review (DRAFT → IN_REVIEW). */
  public Optional<Offering> submitForReview(UUID id) {
    return lifecycle.submitForReview(id);
  }

  /** Deactivates a published offering (→ DEPRECATED) — reference-preserving. */
  public Optional<Offering> deactivate(UUID id) {
    return lifecycle.deprecate(id);
  }

  /**
   * Hard-deletes the offering when {@link CatalogIntegrityService} clears it
   * (a DRAFT with no prerequisite/credential references). Returns the verdict; on a
   * blocked delete nothing is removed and the verdict carries the blocking references.
   */
  public CatalogIntegrityService.IntegrityVerdict delete(UUID id) {
    // Serialise the find→check→delete against a concurrent lifecycle transition on
    // the shared catalog-mutation monitor, so the integrity verdict cannot be made
    // stale by a status change between the check and the remove (exit-review #2).
    synchronized (CatalogLifecycleService.LOCK) {
      Optional<Offering> offering = catalog.findById(id);
      if (offering.isEmpty()) {
        return new CatalogIntegrityService.IntegrityVerdict(false, List.of("offering not found"));
      }
      CatalogIntegrityService.IntegrityVerdict verdict = integrity.checkHardDelete(offering.get());
      if (verdict.allowed()) {
        catalog.delete(id);
      }
      return verdict;
    }
  }

  /** The author's own offerings, latest version per lineage, sorted by title. */
  public List<Offering> myOfferings(Long authorId) {
    Map<UUID, Offering> latest = new LinkedHashMap<>();
    for (Offering o : catalog.all()) {
      if (!authorship.isAuthor(o.lineageId(), authorId)) {
        continue;
      }
      latest.merge(o.lineageId(), o, (a, b) -> a.version() >= b.version() ? a : b);
    }
    List<Offering> result = new ArrayList<>(latest.values());
    result.sort(Comparator.comparing(Offering::title, String.CASE_INSENSITIVE_ORDER));
    return result;
  }

  private static Offering build(String title, String description, OfferingVisibility visibility,
                                String accessCode, UUID prerequisiteOfferingId, LearningPath path) {
    return switch (visibility) {
      case PUBLIC -> Offering.publicPath(title, description, path);
      case REGISTERED -> Offering.registeredPath(title, description, path);
      case CODE -> Offering.codePath(title, description, path, accessCode);
      case PREREQUISITE -> Offering.prerequisitePath(title, description, path,
          prerequisiteOfferingId);
    };
  }
}
