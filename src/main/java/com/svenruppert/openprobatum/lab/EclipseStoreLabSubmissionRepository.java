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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link LabSubmissionRepository}. Stores submissions in the single
 * shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#labSubmissions}.
 *
 * @since V00.40.00
 */
public final class EclipseStoreLabSubmissionRepository implements LabSubmissionRepository {

  @Override
  public synchronized void save(LabSubmission submission) {
    Objects.requireNonNull(submission, "submission");
    Map<UUID, LabSubmission> submissions = AppStorage.appRoot().labSubmissions;
    submissions.put(submission.id(), submission);
    AppStorage.app().store(submissions);
  }

  @Override
  public synchronized Optional<LabSubmission> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().labSubmissions.get(id));
  }

  @Override
  public synchronized List<LabSubmission> all() {
    return new ArrayList<>(AppStorage.appRoot().labSubmissions.values());
  }
}
