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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of {@link Offering}s in the academy catalog (concept §7). Mirrors the
 * credential repository: an in-memory implementation for fast upper-layer tests
 * and an Eclipse-Store-backed one for production.
 *
 * @since V00.20.00
 */
public interface CatalogRepository {

  /** Inserts or replaces an offering by its id. */
  void save(Offering offering);

  /** Looks an offering up by id. */
  Optional<Offering> findById(UUID id);

  /** All offerings in the catalog. */
  Collection<Offering> all();

  /**
   * Removes the offering with {@code id} (a no-op if absent). Hard deletion is
   * reserved for unreferenced DRAFTs — callers gate it through
   * {@link CatalogIntegrityService} (published/referenced offerings are deactivated,
   * not deleted).
   */
  void delete(UUID id);
}
