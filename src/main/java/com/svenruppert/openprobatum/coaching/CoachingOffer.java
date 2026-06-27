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

package com.svenruppert.openprobatum.coaching;

import com.svenruppert.openprobatum.content.ContentStatus;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A coach's 1:1 coaching topic (concept §7.x). A coaching offer is an authored,
 * versioned, reviewed content object (like a workshop): it carries a stable
 * {@link #lineageId} + {@link #version} and moves through the
 * {@link ContentStatus} lifecycle. Once published, the coach opens concrete
 * bookable {@code CoachingSlot}s under it; a learner books a slot for a 1:1
 * session of {@link #durationMinutes}.
 *
 * @param id                the offer id (distinct per version)
 * @param lineageId         the stable logical-offer id (shared across versions)
 * @param version           the version sequence number ({@code >= 1})
 * @param status            the editorial lifecycle status
 * @param title             the offer title
 * @param description       short human-readable summary (never null; may be empty)
 * @param learningObjective the objective the coaching evidences
 * @param coachName         the coach's display name
 * @param coachId           the coach's stable id
 * @param durationMinutes   the session length in minutes ({@code >= 1})
 * @param tags              categorisation tags (defensively copied)
 * @since V00.60.00
 */
public record CoachingOffer(UUID id, UUID lineageId, int version, ContentStatus status,
                            String title, String description, String learningObjective,
                            String coachName, Long coachId, int durationMinutes, Set<String> tags) {

  public CoachingOffer {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lineageId, "lineageId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(learningObjective, "learningObjective");
    Objects.requireNonNull(coachName, "coachName");
    Objects.requireNonNull(tags, "tags");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    if (title.isBlank()) {
      throw new IllegalArgumentException("a coaching offer needs a non-blank title");
    }
    if (durationMinutes < 1) {
      throw new IllegalArgumentException("a coaching session needs a duration >= 1 minute");
    }
    tags = Set.copyOf(tags);
  }

  /** A fresh DRAFT v1 coaching offer with its own lineage. */
  public static CoachingOffer draft(String title, String description, String coachName,
                                    Long coachId, int durationMinutes) {
    UUID id = UUID.randomUUID();
    return new CoachingOffer(id, id, 1, ContentStatus.DRAFT, title,
        description == null ? "" : description, "", coachName, coachId, durationMinutes, Set.of());
  }

  /** A copy with the editorial status changed (e.g. publish). */
  public CoachingOffer withStatus(ContentStatus newStatus) {
    return new CoachingOffer(id, lineageId, version, Objects.requireNonNull(newStatus, "newStatus"),
        title, description, learningObjective, coachName, coachId, durationMinutes, tags);
  }

  /** A copy with the learning objective set. */
  public CoachingOffer withObjective(String objective) {
    return new CoachingOffer(id, lineageId, version, status, title, description,
        Objects.requireNonNull(objective, "objective"), coachName, coachId, durationMinutes, tags);
  }

  /** A copy with the categorisation tags replaced. */
  public CoachingOffer withTags(Set<String> newTags) {
    return new CoachingOffer(id, lineageId, version, status, title, description, learningObjective,
        coachName, coachId, durationMinutes, newTags);
  }

  /** @return {@code true} when this offer is PUBLISHED (learner-visible). */
  public boolean isPublished() {
    return status == ContentStatus.PUBLISHED;
  }

  /**
   * A fresh DRAFT next version of the same logical offer — a new {@link #id}, same
   * {@link #lineageId}, {@code version + 1}. The prior version stays intact.
   */
  public CoachingOffer asNewVersion() {
    return new CoachingOffer(UUID.randomUUID(), lineageId, version + 1, ContentStatus.DRAFT,
        title, description, learningObjective, coachName, coachId, durationMinutes, tags);
  }
}
