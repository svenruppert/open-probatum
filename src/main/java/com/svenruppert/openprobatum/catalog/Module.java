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
 * A section within a learning path (concept §8.2). Carries a stable id (so
 * progress can be tracked, §8.4), a title, a block of human-readable content and
 * whether it is {@link #mandatory} — only mandatory modules gate path completion.
 *
 * @param id        stable module id
 * @param title     the module heading
 * @param content   the learning material (article text for the slice)
 * @param mandatory whether completing this module is required to finish the path
 * @since V00.10.00
 */
public record Module(UUID id, String title, String content, boolean mandatory) {

  public Module {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(content, "content");
  }

  /** A mandatory module with a fresh random id. */
  public static Module mandatory(String title, String content) {
    return new Module(UUID.randomUUID(), title, content, true);
  }

  /** An optional module with a fresh random id. */
  public static Module optional(String title, String content) {
    return new Module(UUID.randomUUID(), title, content, false);
  }
}
