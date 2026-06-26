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

package com.svenruppert.openprobatum.lab;

import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.content.ContentStatus;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A practical task a learner completes hands-on and submits evidence for
 * (concept §9.x / §16.3). A lab is an authored, versioned, reviewed content
 * object — the practical counterpart of a bank {@code Question}: it carries a
 * stable {@link #lineageId} + {@link #version} so a new version is a new
 * immutable record (existing submissions, which pin the version they were made
 * against, are never falsified), and moves through the {@link ContentStatus}
 * editorial lifecycle before learners can reach it.
 *
 * @param id                 the lab id (distinct per version)
 * @param lineageId          the stable logical-lab id (shared across versions)
 * @param version            the version sequence number ({@code >= 1})
 * @param status             the editorial lifecycle status
 * @param title              the lab title
 * @param instructions       what the learner must do
 * @param learningObjective  the objective the lab evidences
 * @param difficulty         the lab difficulty
 * @param acceptanceCriteria what a satisfactory submission must show
 * @param tags               categorisation tags (defensively copied)
 * @since V00.40.00
 */
public record Lab(UUID id, UUID lineageId, int version, ContentStatus status,
                  String title, String instructions, String learningObjective,
                  Difficulty difficulty, String acceptanceCriteria, Set<String> tags) {

  public Lab {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lineageId, "lineageId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(instructions, "instructions");
    Objects.requireNonNull(learningObjective, "learningObjective");
    Objects.requireNonNull(difficulty, "difficulty");
    Objects.requireNonNull(acceptanceCriteria, "acceptanceCriteria");
    Objects.requireNonNull(tags, "tags");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    if (title.isBlank()) {
      throw new IllegalArgumentException("a lab needs a non-blank title");
    }
    if (instructions.isBlank()) {
      throw new IllegalArgumentException("a lab needs non-blank instructions");
    }
    tags = Set.copyOf(tags);
  }

  /** A fresh DRAFT v1 lab with its own lineage and default metadata. */
  public static Lab draft(String title, String instructions) {
    UUID id = UUID.randomUUID();
    return new Lab(id, id, 1, ContentStatus.DRAFT, title, instructions,
        "", Difficulty.MEDIUM, "", Set.of());
  }

  /** A copy with the editorial status changed (e.g. publish). */
  public Lab withStatus(ContentStatus newStatus) {
    return new Lab(id, lineageId, version, Objects.requireNonNull(newStatus, "newStatus"),
        title, instructions, learningObjective, difficulty, acceptanceCriteria, tags);
  }

  /** A copy with the didactic metadata set (objective, difficulty, acceptance criteria). */
  public Lab withMetadata(String objective, Difficulty newDifficulty, String acceptance) {
    return new Lab(id, lineageId, version, status, title, instructions,
        Objects.requireNonNull(objective, "objective"),
        Objects.requireNonNull(newDifficulty, "difficulty"),
        Objects.requireNonNull(acceptance, "acceptance"), tags);
  }

  /** A copy with the categorisation tags replaced. */
  public Lab withTags(Set<String> newTags) {
    return new Lab(id, lineageId, version, status, title, instructions,
        learningObjective, difficulty, acceptanceCriteria, newTags);
  }

  /** @return {@code true} when this lab is PUBLISHED (learner-visible). */
  public boolean isPublished() {
    return status == ContentStatus.PUBLISHED;
  }

  /**
   * A fresh DRAFT next version of the same logical lab — a new {@link #id}, same
   * {@link #lineageId}, {@code version + 1}. The prior version stays intact.
   */
  public Lab asNewVersion() {
    return new Lab(UUID.randomUUID(), lineageId, version + 1, ContentStatus.DRAFT,
        title, instructions, learningObjective, difficulty, acceptanceCriteria, tags);
  }
}
