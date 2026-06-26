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

import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production {@link QuestionRepository}. Stores question versions in the single
 * shared application Eclipse-Store, rooted at {@link AppStorage.AppRoot#questions}.
 *
 * @since V00.30.00
 */
public final class EclipseStoreQuestionRepository implements QuestionRepository {

  @Override
  public synchronized void save(Question question) {
    Objects.requireNonNull(question, "question");
    Map<UUID, Question> store = AppStorage.appRoot().questions;
    store.put(question.id(), question);
    AppStorage.app().store(store);
  }

  @Override
  public synchronized Optional<Question> findById(UUID id) {
    return Optional.ofNullable(AppStorage.appRoot().questions.get(id));
  }

  @Override
  public synchronized Collection<Question> all() {
    return new ArrayList<>(AppStorage.appRoot().questions.values());
  }
}
