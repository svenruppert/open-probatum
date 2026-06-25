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
import java.util.UUID;

/**
 * The central catalog item (concept §7.2) — the hook of the trust flow. For the
 * Trust-Core slice an offering is a single {@link OfferingType#CERTIFICATION_PATH}
 * wrapping one {@link LearningPath}.
 *
 * @param id    random offering id
 * @param title the offering title
 * @param type  the offering kind
 * @param path  the learning path delivered by this offering
 * @since V00.10.00
 */
public record Offering(UUID id, String title, OfferingType type, LearningPath path) {

  public Offering {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(path, "path");
  }

  /** Creates a certification-path offering with a fresh random id. */
  public static Offering certificationPath(String title, LearningPath path) {
    return new Offering(UUID.randomUUID(), title, OfferingType.CERTIFICATION_PATH, path);
  }
}
