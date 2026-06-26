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

import com.svenruppert.openprobatum.content.ContentStatus;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A curated package grouping several offerings (concept §7.x). A bundle is an
 * authored, versioned, reviewed content object (like a lab or a question): it
 * carries a stable {@link #lineageId} + {@link #version} and moves through the
 * {@link ContentStatus} editorial lifecycle. Granting a bundle entitles the
 * learner to every {@link #offeringIds member offering} at once; completing all
 * member paths earns a bundle completion credential.
 *
 * @param id          the bundle id (distinct per version)
 * @param lineageId   the stable logical-bundle id (shared across versions)
 * @param version     the version sequence number ({@code >= 1})
 * @param status      the editorial lifecycle status
 * @param title       the bundle title
 * @param description short human-readable summary (never null; may be empty)
 * @param offeringIds the member offering ids (defensively copied; non-empty when published)
 * @param tags        categorisation tags (defensively copied)
 * @since V00.50.00
 */
public record Bundle(UUID id, UUID lineageId, int version, ContentStatus status,
                     String title, String description, Set<UUID> offeringIds, Set<String> tags) {

  public Bundle {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lineageId, "lineageId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(offeringIds, "offeringIds");
    Objects.requireNonNull(tags, "tags");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    if (title.isBlank()) {
      throw new IllegalArgumentException("a bundle needs a non-blank title");
    }
    offeringIds = Set.copyOf(offeringIds);
    tags = Set.copyOf(tags);
  }

  /** A fresh DRAFT v1 bundle with its own lineage and the given members. */
  public static Bundle draft(String title, String description, Set<UUID> offeringIds) {
    UUID id = UUID.randomUUID();
    return new Bundle(id, id, 1, ContentStatus.DRAFT, title,
        description == null ? "" : description, offeringIds, Set.of());
  }

  /** A copy with the editorial status changed (e.g. publish). */
  public Bundle withStatus(ContentStatus newStatus) {
    return new Bundle(id, lineageId, version, Objects.requireNonNull(newStatus, "newStatus"),
        title, description, offeringIds, tags);
  }

  /** A copy with the member offering ids replaced. */
  public Bundle withMembers(Set<UUID> newMembers) {
    return new Bundle(id, lineageId, version, status, title, description, newMembers, tags);
  }

  /** A copy with the categorisation tags replaced. */
  public Bundle withTags(Set<String> newTags) {
    return new Bundle(id, lineageId, version, status, title, description, offeringIds, newTags);
  }

  /** @return {@code true} when this bundle is PUBLISHED (learner-visible). */
  public boolean isPublished() {
    return status == ContentStatus.PUBLISHED;
  }

  /** @return {@code true} if {@code offeringId} is a member of this bundle. */
  public boolean contains(UUID offeringId) {
    return offeringIds.contains(offeringId);
  }

  /**
   * A fresh DRAFT next version of the same logical bundle — a new {@link #id},
   * same {@link #lineageId}, {@code version + 1}. The prior version stays intact.
   */
  public Bundle asNewVersion() {
    return new Bundle(UUID.randomUUID(), lineageId, version + 1, ContentStatus.DRAFT,
        title, description, offeringIds, tags);
  }
}
