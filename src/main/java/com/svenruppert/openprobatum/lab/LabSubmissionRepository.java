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

package com.svenruppert.openprobatum.lab;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store of {@link LabSubmission}s (concept §16.3). Mirrors the other
 * repositories: in-memory for fast tests, Eclipse-Store-backed for production.
 *
 * @since V00.40.00
 */
public interface LabSubmissionRepository {

  /** Inserts or replaces a submission by its id. */
  void save(LabSubmission submission);

  /** Looks a submission up by id. */
  Optional<LabSubmission> findById(UUID id);

  /** Every submission in the store. */
  List<LabSubmission> all();

  /** A learner's own submissions (own-data, §3.6), newest first. */
  default List<LabSubmission> forLearner(Long recipientId) {
    return all().stream()
        .filter(s -> s.recipientId().equals(recipientId))
        .sorted(Comparator.comparing(LabSubmission::submittedAt).reversed())
        .toList();
  }

  /** Submissions awaiting an assessor's verdict (SUBMITTED), oldest first. */
  default List<LabSubmission> pending() {
    return all().stream()
        .filter(s -> s.status() == SubmissionStatus.SUBMITTED)
        .sorted(Comparator.comparing(LabSubmission::submittedAt))
        .toList();
  }

  /** All submissions against any version of a given lab id. */
  default List<LabSubmission> forLab(UUID labId) {
    return all().stream()
        .filter(s -> s.labId().equals(labId))
        .toList();
  }
}
