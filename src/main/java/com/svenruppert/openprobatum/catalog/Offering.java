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
public record Offering(UUID id, String title, String description, OfferingType type,
                       LearningPath path, OfferingVisibility visibility,
                       String accessCode, UUID prerequisiteOfferingId) {

  public Offering {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(visibility, "visibility");
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

  /** A public certification-path offering with a fresh random id. */
  public static Offering certificationPath(String title, LearningPath path) {
    return publicPath(title, "", path);
  }

  /** A publicly visible certification path. */
  public static Offering publicPath(String title, String description, LearningPath path) {
    return new Offering(UUID.randomUUID(), title, description, OfferingType.CERTIFICATION_PATH,
        path, OfferingVisibility.PUBLIC, null, null);
  }

  /** A certification path visible to any registered user. */
  public static Offering registeredPath(String title, String description, LearningPath path) {
    return new Offering(UUID.randomUUID(), title, description, OfferingType.CERTIFICATION_PATH,
        path, OfferingVisibility.REGISTERED, null, null);
  }

  /** A certification path reachable only with {@code code}. */
  public static Offering codePath(String title, String description, LearningPath path, String code) {
    return new Offering(UUID.randomUUID(), title, description, OfferingType.CERTIFICATION_PATH,
        path, OfferingVisibility.CODE, code, null);
  }

  /** A certification path reachable only after {@code prerequisiteOfferingId} is completed. */
  public static Offering prerequisitePath(String title, String description, LearningPath path,
                                          UUID prerequisiteOfferingId) {
    return new Offering(UUID.randomUUID(), title, description, OfferingType.CERTIFICATION_PATH,
        path, OfferingVisibility.PREREQUISITE, null, prerequisiteOfferingId);
  }
}
