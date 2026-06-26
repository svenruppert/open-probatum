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

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Operations on the question bank (concept §9.3): create, tag, move through the
 * editorial lifecycle, and list reusable questions. Transitions are validated
 * against {@link ContentStatus}; a question may only be approved once it carries
 * the mandatory explanation + learning objective (§9.4).
 *
 * @since V00.30.00
 */
public final class QuestionBankService implements HasLogger {

  private final QuestionRepository repository;

  public QuestionBankService(QuestionRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public QuestionBankService() {
    this(QuestionRepositoryProvider.repository());
  }

  /** Stores a new (draft) question. */
  public Question create(Question question) {
    Objects.requireNonNull(question, "question");
    repository.save(question);
    logger().info("Question created: id={} lineage={} v{}",
        question.id(), question.lineageId(), question.version());
    return question;
  }

  /** Sets the categorisation tags on a question. */
  public Optional<Question> tag(UUID id, Set<String> tags) {
    return repository.findById(id).map(q -> {
      Question next = q.withTags(tags);
      repository.save(next);
      return next;
    });
  }

  /**
   * Applies a lifecycle transition (validated against {@link ContentStatus}).
   * Approval additionally requires a non-blank explanation + learning objective.
   */
  public synchronized Optional<Question> transition(UUID id, ContentStatus target) {
    Objects.requireNonNull(target, "target");
    return repository.findById(id).map(q -> {
      if (!q.status().canTransitionTo(target)) {
        throw new IllegalStateException(
            "illegal content transition " + q.status() + " → " + target);
      }
      if (target == ContentStatus.APPROVED
          && (q.explanation().isBlank() || q.learningObjective().isBlank())) {
        throw new IllegalStateException(
            "a question needs a non-blank explanation + learning objective before approval");
      }
      Question next = q.withStatus(target);
      repository.save(next);
      logger().info("Question {} → {}", id, target);
      return next;
    });
  }

  public Optional<Question> submitForReview(UUID id) {
    return transition(id, ContentStatus.IN_REVIEW);
  }

  public Optional<Question> approve(UUID id) {
    return transition(id, ContentStatus.APPROVED);
  }

  /**
   * Approves a question on behalf of {@code approverId}, enforcing segregation of
   * duties (§3.6/§17.2): a reviewer may not approve content they authored. Throws
   * {@link IllegalStateException} on a self-approval attempt.
   */
  public Optional<Question> approve(UUID id, Long approverId) {
    repository.findById(id).ifPresent(q -> {
      if (ContentAuthorshipProvider.registry().isAuthor(q.lineageId(), approverId)) {
        throw new IllegalStateException("an author cannot approve their own content");
      }
    });
    return approve(id);
  }

  public Optional<Question> rejectToDraft(UUID id) {
    return transition(id, ContentStatus.DRAFT);
  }

  public Optional<Question> publish(UUID id) {
    return transition(id, ContentStatus.PUBLISHED);
  }

  public Optional<Question> deprecate(UUID id) {
    return transition(id, ContentStatus.DEPRECATED);
  }

  public Optional<Question> archive(UUID id) {
    return transition(id, ContentStatus.ARCHIVED);
  }

  /** Questions awaiting a reviewer's verdict (IN_REVIEW) or ready to publish (APPROVED). */
  public List<Question> pendingReview() {
    return repository.all().stream()
        .filter(q -> q.status() == ContentStatus.IN_REVIEW || q.status() == ContentStatus.APPROVED)
        .toList();
  }

  /** Questions usable when authoring an assessment — APPROVED or PUBLISHED, never archived. */
  public List<Question> reusable() {
    return repository.all().stream()
        .filter(q -> q.status() == ContentStatus.APPROVED || q.status() == ContentStatus.PUBLISHED)
        .toList();
  }
}
