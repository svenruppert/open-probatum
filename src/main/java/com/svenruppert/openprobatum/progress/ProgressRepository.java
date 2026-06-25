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

package com.svenruppert.openprobatum.progress;

import java.util.Optional;
import java.util.UUID;

/**
 * Store of {@link LearnerProgress} records (concept §8.4).
 *
 * @since V00.20.00
 */
public interface ProgressRepository {

  /** Inserts or replaces the progress for {@code (userId, offeringId)}. */
  void save(LearnerProgress progress);

  /** Looks up a learner's progress for an offering. */
  Optional<LearnerProgress> find(Long userId, UUID offeringId);
}
