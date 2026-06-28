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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The central catalog item (concept §7.2) — the hook of the trust flow. An
 * offering wraps one {@link LearningPath} and declares how learners reach it via
 * its {@link OfferingVisibility} (§7.4).
 *
 * <p>The gate data is constrained to the visibility: a {@code CODE} offering
 * carries a non-blank {@link #accessCode}; a {@code PREREQUISITE} offering
 * carries a non-null {@link #prerequisiteOfferingId}; {@code PUBLIC}/
 * {@code REGISTERED} carry neither.
 *
 * @param id                     random offering id
 * @param title                  the offering title
 * @param description            short human-readable summary (never null; may be empty)
 * @param type                   the offering kind
 * @param path                   the learning path delivered by this offering
 * @param visibility             how the offering is reached
 * @param accessCode             the code for a {@code CODE} offering, else null
 * @param prerequisiteOfferingId the required offering for a {@code PREREQUISITE} offering, else null
 * @since V00.10.00
 */
public record Offering(UUID id, UUID lineageId, int version, ContentStatus status,
                       String title, String description, OfferingType type,
                       LearningPath path, OfferingVisibility visibility,
                       String accessCode, UUID prerequisiteOfferingId) {

  public Offering {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lineageId, "lineageId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(visibility, "visibility");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    if (visibility == OfferingVisibility.CODE && (accessCode == null || accessCode.isBlank())) {
      throw new IllegalArgumentException("a CODE offering needs a non-blank access code");
    }
    if (visibility != OfferingVisibility.CODE && accessCode != null) {
      throw new IllegalArgumentException("only a CODE offering may carry an access code");
    }
    if (visibility == OfferingVisibility.PREREQUISITE && prerequisiteOfferingId == null) {
      throw new IllegalArgumentException("a PREREQUISITE offering needs a prerequisite id");
    }
    if (visibility != OfferingVisibility.PREREQUISITE && prerequisiteOfferingId != null) {
      throw new IllegalArgumentException("only a PREREQUISITE offering may carry a prerequisite id");
    }
  }

  /** @return the access code, when this is a {@code CODE} offering. */
  public Optional<String> accessCodeOpt() {
    return Optional.ofNullable(accessCode);
  }

  /** @return the prerequisite offering id, when this is a {@code PREREQUISITE} offering. */
  public Optional<UUID> prerequisiteOfferingIdOpt() {
    return Optional.ofNullable(prerequisiteOfferingId);
  }

  /** A fresh DRAFT v1 certification-path offering with its own lineage. */
  private static Offering draft(String title, String description, LearningPath path,
                                OfferingVisibility visibility, String code, UUID prereq) {
    UUID id = UUID.randomUUID();
    return new Offering(id, id, 1, ContentStatus.DRAFT, title, description,
        OfferingType.CERTIFICATION_PATH, path, visibility, code, prereq);
  }

  /** A public certification-path offering with a fresh random id. */
  public static Offering certificationPath(String title, LearningPath path) {
    return publicPath(title, "", path);
  }

  /** A publicly visible certification path. */
  public static Offering publicPath(String title, String description, LearningPath path) {
    return draft(title, description, path, OfferingVisibility.PUBLIC, null, null);
  }

  /** A certification path visible to any registered user. */
  public static Offering registeredPath(String title, String description, LearningPath path) {
    return draft(title, description, path, OfferingVisibility.REGISTERED, null, null);
  }

  /** A certification path reachable only with {@code code}. */
  public static Offering codePath(String title, String description, LearningPath path, String code) {
    return draft(title, description, path, OfferingVisibility.CODE, code, null);
  }

  /** A certification path reachable only after {@code prerequisiteOfferingId} is completed. */
  public static Offering prerequisitePath(String title, String description, LearningPath path,
                                          UUID prerequisiteOfferingId) {
    return draft(title, description, path, OfferingVisibility.PREREQUISITE, null, prerequisiteOfferingId);
  }

  /** A copy with the editorial status changed (e.g. publish). */
  public Offering withStatus(ContentStatus newStatus) {
    return new Offering(id, lineageId, version, newStatus, title, description, type, path,
        visibility, accessCode, prerequisiteOfferingId);
  }

  /**
   * A copy with the author-editable details replaced — title, description,
   * visibility (+ its gate data) and the learning path — keeping the identity and
   * editorial state ({@link #id}, {@link #lineageId}, {@link #version},
   * {@link #status}, {@link #type}). The compact constructor re-validates the
   * visibility/access-code/prerequisite invariants. Use this for an in-place DRAFT
   * edit; for a PUBLISHED offering, {@link #asNewVersion()} first so a published
   * record is never rewritten under its own id.
   */
  public Offering withDetails(String newTitle, String newDescription,
                              OfferingVisibility newVisibility, String newAccessCode,
                              UUID newPrerequisiteOfferingId, LearningPath newPath) {
    return new Offering(id, lineageId, version, status, newTitle, newDescription, type, newPath,
        newVisibility, newAccessCode, newPrerequisiteOfferingId);
  }

  /** @return {@code true} when this offering is PUBLISHED (learner-visible). */
  public boolean isPublished() {
    return status == ContentStatus.PUBLISHED;
  }

  /**
   * A fresh DRAFT next version of the same logical offering — a new {@link #id},
   * same {@link #lineageId}, {@code version + 1}. The prior version stays intact.
   */
  public Offering asNewVersion() {
    return new Offering(UUID.randomUUID(), lineageId, version + 1, ContentStatus.DRAFT,
        title, description, type, path, visibility, accessCode, prerequisiteOfferingId);
  }
}
