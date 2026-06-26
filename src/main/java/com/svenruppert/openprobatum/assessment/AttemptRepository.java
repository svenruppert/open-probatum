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

import java.util.List;
import java.util.UUID;

/**
 * Store of completion-check {@link Attempt}s (concept §9.6, §9.7) — attempts are
 * counted per learner + assessment.
 *
 * @since V00.20.00
 */
public interface AttemptRepository {

  void save(Attempt attempt);

  /** All attempts a learner has made at an assessment, in no guaranteed order. */
  List<Attempt> forLearner(String learnerName, UUID assessmentId);

  /** All attempts at an assessment across every learner (for quality metrics, §20.2). */
  List<Attempt> forAssessment(UUID assessmentId);

  /** How many attempts a learner has made at an assessment. */
  default int countFor(String learnerName, UUID assessmentId) {
    return forLearner(learnerName, assessmentId).size();
  }
}
