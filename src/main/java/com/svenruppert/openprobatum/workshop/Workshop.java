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

package com.svenruppert.openprobatum.workshop;

import com.svenruppert.openprobatum.content.ContentStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A scheduled, capacity-limited, instructor-run session (concept §7.x). A
 * workshop is an authored, versioned, reviewed content object: it carries a
 * stable {@link #lineageId} + {@link #version}, moves through the
 * {@link ContentStatus} lifecycle, and adds a schedule ({@link #startsAt} /
 * {@link #endsAt}), a seat {@link #capacity} and an {@link #instructor}. A learner
 * enrols up to capacity; attendance earns a workshop certificate.
 *
 * @param id                the workshop id (distinct per version)
 * @param lineageId         the stable logical-workshop id (shared across versions)
 * @param version           the version sequence number ({@code >= 1})
 * @param status            the editorial lifecycle status
 * @param title             the workshop title
 * @param description       short human-readable summary (never null; may be empty)
 * @param learningObjective the objective the workshop evidences
 * @param startsAt          when the session starts
 * @param endsAt            when the session ends (after {@link #startsAt})
 * @param capacity          the maximum number of seats ({@code >= 1})
 * @param instructor        the instructor's name
 * @param tags              categorisation tags (defensively copied)
 * @since V00.50.00
 */
public record Workshop(UUID id, UUID lineageId, int version, ContentStatus status,
                       String title, String description, String learningObjective,
                       Instant startsAt, Instant endsAt, int capacity, String instructor,
                       Set<String> tags) {

  public Workshop {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lineageId, "lineageId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(learningObjective, "learningObjective");
    Objects.requireNonNull(startsAt, "startsAt");
    Objects.requireNonNull(endsAt, "endsAt");
    Objects.requireNonNull(instructor, "instructor");
    Objects.requireNonNull(tags, "tags");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    if (title.isBlank()) {
      throw new IllegalArgumentException("a workshop needs a non-blank title");
    }
    if (capacity < 1) {
      throw new IllegalArgumentException("a workshop needs a capacity >= 1");
    }
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException("a workshop must end after it starts");
    }
    tags = Set.copyOf(tags);
  }

  /** A fresh DRAFT v1 workshop with its own lineage. */
  public static Workshop draft(String title, String description, Instant startsAt,
                               Instant endsAt, int capacity, String instructor) {
    UUID id = UUID.randomUUID();
    return new Workshop(id, id, 1, ContentStatus.DRAFT, title,
        description == null ? "" : description, "", startsAt, endsAt, capacity, instructor, Set.of());
  }

  /** A copy with the editorial status changed (e.g. publish). */
  public Workshop withStatus(ContentStatus newStatus) {
    return new Workshop(id, lineageId, version, Objects.requireNonNull(newStatus, "newStatus"),
        title, description, learningObjective, startsAt, endsAt, capacity, instructor, tags);
  }

  /** A copy with the learning objective set. */
  public Workshop withObjective(String objective) {
    return new Workshop(id, lineageId, version, status, title, description,
        Objects.requireNonNull(objective, "objective"), startsAt, endsAt, capacity, instructor, tags);
  }

  /** A copy with the categorisation tags replaced. */
  public Workshop withTags(Set<String> newTags) {
    return new Workshop(id, lineageId, version, status, title, description, learningObjective,
        startsAt, endsAt, capacity, instructor, newTags);
  }

  /** @return {@code true} when this workshop is PUBLISHED (learner-visible). */
  public boolean isPublished() {
    return status == ContentStatus.PUBLISHED;
  }

  /**
   * A fresh DRAFT next version of the same logical workshop — a new {@link #id},
   * same {@link #lineageId}, {@code version + 1}. The prior version stays intact.
   */
  public Workshop asNewVersion() {
    return new Workshop(UUID.randomUUID(), lineageId, version + 1, ContentStatus.DRAFT,
        title, description, learningObjective, startsAt, endsAt, capacity, instructor, tags);
  }
}
