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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Heap-only {@link ContentAuthorship} for fast upper-layer tests.
 *
 * @since V00.30.00
 */
public final class InMemoryContentAuthorship implements ContentAuthorship {

  private final ConcurrentMap<UUID, Long> authors = new ConcurrentHashMap<>();

  @Override
  public void recordAuthor(UUID lineageId, Long authorId) {
    Objects.requireNonNull(lineageId, "lineageId");
    if (authorId != null) {
      authors.putIfAbsent(lineageId, authorId);
    }
  }

  @Override
  public Optional<Long> authorOf(UUID lineageId) {
    return Optional.ofNullable(authors.get(lineageId));
  }
}
