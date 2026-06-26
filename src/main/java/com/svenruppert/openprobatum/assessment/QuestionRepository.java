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

package com.svenruppert.openprobatum.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The question bank (concept §9.3) — every question version stored by its
 * own id, with lineage helpers to walk the versions of a logical question.
 *
 * @since V00.30.00
 */
public interface QuestionRepository {

  /** Inserts or replaces a question version by its (version-specific) id. */
  void save(Question question);

  /** Looks a specific question version up by id. */
  Optional<Question> findById(UUID id);

  /** Every question version in the bank. */
  Collection<Question> all();

  /** All versions of the logical question {@code lineageId}, ascending by version. */
  default List<Question> versionsOf(UUID lineageId) {
    return all().stream()
        .filter(q -> q.lineageId().equals(lineageId))
        .sorted(java.util.Comparator.comparingInt(Question::version))
        .toList();
  }

  /** The highest-version record of the logical question {@code lineageId}. */
  default Optional<Question> latestOf(UUID lineageId) {
    return all().stream()
        .filter(q -> q.lineageId().equals(lineageId))
        .max(java.util.Comparator.comparingInt(Question::version));
  }
}
