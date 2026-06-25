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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A section within a learning path (concept §8.2). Carries a stable id (so
 * progress can be tracked, §8.4), a title, an intro {@code content} block,
 * whether it is {@link #mandatory} (only mandatory modules gate completion), and
 * an ordered, immutable list of typed {@link LearningResource}s (§8.3).
 *
 * @param id        stable module id
 * @param title     the module heading
 * @param content   the module's intro / article text
 * @param mandatory whether completing this module is required to finish the path
 * @param resources the ordered learning resources (defensively copied)
 * @since V00.10.00
 */
public record Module(UUID id, String title, String content, boolean mandatory,
                     List<LearningResource> resources) {

  public Module {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(resources, "resources");
    resources = List.copyOf(resources);
  }

  /** A mandatory module with a fresh random id and no extra resources. */
  public static Module mandatory(String title, String content) {
    return new Module(UUID.randomUUID(), title, content, true, List.of());
  }

  /** An optional module with a fresh random id and no extra resources. */
  public static Module optional(String title, String content) {
    return new Module(UUID.randomUUID(), title, content, false, List.of());
  }

  /** A mandatory module carrying the given learning resources. */
  public static Module mandatory(String title, String content, List<LearningResource> resources) {
    return new Module(UUID.randomUUID(), title, content, true, resources);
  }

  /** An optional module carrying the given learning resources. */
  public static Module optional(String title, String content, List<LearningResource> resources) {
    return new Module(UUID.randomUUID(), title, content, false, resources);
  }
}
