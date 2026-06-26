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

package com.svenruppert.openprobatum.workshop;

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Operations on authored {@link Workshop}s (concept §7.x / §16.3): create, tag,
 * move through the editorial lifecycle, list those awaiting review or published.
 * Transitions are validated against {@link ContentStatus}; approval requires a
 * non-blank learning objective, and a reviewer may not approve a workshop they
 * authored.
 *
 * @since V00.50.00
 */
public final class WorkshopService implements HasLogger {

  private final WorkshopRepository repository;

  public WorkshopService(WorkshopRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public WorkshopService() {
    this(WorkshopRepositoryProvider.repository());
  }

  /** Stores a new (draft) workshop. */
  public Workshop create(Workshop workshop) {
    Objects.requireNonNull(workshop, "workshop");
    repository.save(workshop);
    logger().info("Workshop created: id={} lineage={} v{}",
        workshop.id(), workshop.lineageId(), workshop.version());
    return workshop;
  }

  /** Sets the categorisation tags on a workshop. */
  public Optional<Workshop> tag(UUID id, Set<String> tags) {
    return repository.findById(id).map(w -> {
      Workshop next = w.withTags(tags);
      repository.save(next);
      return next;
    });
  }

  /**
   * Applies a lifecycle transition (validated against {@link ContentStatus}).
   * Approval additionally requires a non-blank learning objective.
   */
  public synchronized Optional<Workshop> transition(UUID id, ContentStatus target) {
    Objects.requireNonNull(target, "target");
    return repository.findById(id).map(w -> {
      if (!w.status().canTransitionTo(target)) {
        throw new IllegalStateException(
            "illegal content transition " + w.status() + " → " + target);
      }
      if (target == ContentStatus.APPROVED && w.learningObjective().isBlank()) {
        throw new IllegalStateException("a workshop needs a non-blank learning objective before approval");
      }
      Workshop next = w.withStatus(target);
      repository.save(next);
      logger().info("Workshop {} → {}", id, target);
      return next;
    });
  }

  public Optional<Workshop> submitForReview(UUID id) {
    return transition(id, ContentStatus.IN_REVIEW);
  }

  public Optional<Workshop> approve(UUID id) {
    return transition(id, ContentStatus.APPROVED);
  }

  /**
   * Approves a workshop on behalf of {@code approverId}, enforcing segregation of
   * duties (§3.6/§17.2): a reviewer may not approve a workshop they authored.
   * Throws {@link IllegalStateException} on a self-approval attempt. Fail-open when
   * the author is unknown — by necessity; the authoring surface always records the
   * signed-in author at creation.
   */
  public Optional<Workshop> approve(UUID id, Long approverId) {
    repository.findById(id).ifPresent(w -> {
      if (ContentAuthorshipProvider.registry().isAuthor(w.lineageId(), approverId)) {
        throw new IllegalStateException("an author cannot approve their own content");
      }
    });
    return approve(id);
  }

  public Optional<Workshop> rejectToDraft(UUID id) {
    return transition(id, ContentStatus.DRAFT);
  }

  public Optional<Workshop> publish(UUID id) {
    return transition(id, ContentStatus.PUBLISHED);
  }

  public Optional<Workshop> deprecate(UUID id) {
    return transition(id, ContentStatus.DEPRECATED);
  }

  public Optional<Workshop> archive(UUID id) {
    return transition(id, ContentStatus.ARCHIVED);
  }

  /** Workshops awaiting a reviewer's verdict (IN_REVIEW) or ready to publish (APPROVED). */
  public List<Workshop> pendingReview() {
    return repository.all().stream()
        .filter(w -> w.status() == ContentStatus.IN_REVIEW || w.status() == ContentStatus.APPROVED)
        .toList();
  }

  /** Workshops a learner can enrol in — PUBLISHED only. */
  public List<Workshop> published() {
    return repository.all().stream()
        .filter(Workshop::isPublished)
        .toList();
  }
}
