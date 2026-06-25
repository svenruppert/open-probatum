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

/**
 * A structured learning track (concept §8.1) — a title and an ordered,
 * immutable list of {@link Module}s. For the Trust-Core slice a path holds at
 * least one module.
 *
 * @param title   the path title
 * @param modules the ordered modules (defensively copied, must be non-empty)
 * @since V00.10.00
 */
public record LearningPath(String title, List<Module> modules) {

  public LearningPath {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(modules, "modules");
    modules = List.copyOf(modules);
    if (modules.isEmpty()) {
      throw new IllegalArgumentException("a learning path needs at least one module");
    }
  }
}
