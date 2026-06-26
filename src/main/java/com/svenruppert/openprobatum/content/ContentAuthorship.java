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

package com.svenruppert.openprobatum.content;

import java.util.Optional;
import java.util.UUID;

/**
 * Records who authored a piece of content, keyed by its stable {@code lineageId}
 * (concept §17.1/§17.2) — so the registry survives versioning (a new version of
 * the same logical content keeps the original author). It backs two row-level
 * rules: an author surface can filter to its own content, and the review surface
 * can refuse self-approval (segregation of duties, §3.6). Kept off the immutable
 * content records to avoid coupling authorship to every content revision.
 *
 * @since V00.30.00
 */
public interface ContentAuthorship {

  /** Records that {@code authorId} authored the content lineage {@code lineageId}. */
  void recordAuthor(UUID lineageId, Long authorId);

  /** The author of {@code lineageId}, if recorded. */
  Optional<Long> authorOf(UUID lineageId);

  /** Whether {@code userId} authored {@code lineageId}. */
  default boolean isAuthor(UUID lineageId, Long userId) {
    return userId != null && authorOf(lineageId).map(userId::equals).orElse(false);
  }
}
