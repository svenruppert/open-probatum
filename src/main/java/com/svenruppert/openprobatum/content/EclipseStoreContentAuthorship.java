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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link ContentAuthorship}. Stores the lineage→author mapping in the
 * single shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#contentAuthors}.
 *
 * @since V00.30.00
 */
public final class EclipseStoreContentAuthorship implements ContentAuthorship {

  @Override
  public synchronized void recordAuthor(UUID lineageId, Long authorId) {
    Objects.requireNonNull(lineageId, "lineageId");
    if (authorId == null) {
      return;
    }
    Map<UUID, Long> authors = AppStorage.appRoot().contentAuthors;
    authors.putIfAbsent(lineageId, authorId);
    AppStorage.app().store(authors);
  }

  @Override
  public synchronized Optional<Long> authorOf(UUID lineageId) {
    return Optional.ofNullable(AppStorage.appRoot().contentAuthors.get(lineageId));
  }
}
