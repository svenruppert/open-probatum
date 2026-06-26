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

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Operations on authored {@link Lab}s (concept §9.x / §16.3): create, tag, move
 * through the editorial lifecycle, and list those awaiting review. Transitions
 * are validated against {@link ContentStatus}; approval additionally requires a
 * non-blank learning objective + acceptance criteria (a published lab must tell
 * the learner what to do and how it is judged), and refuses self-approval.
 *
 * @since V00.40.00
 */
public final class LabService implements HasLogger {

  private final LabRepository repository;

  public LabService(LabRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public LabService() {
    this(LabRepositoryProvider.repository());
  }

  /** Stores a new (draft) lab. */
  public Lab create(Lab lab) {
    Objects.requireNonNull(lab, "lab");
    repository.save(lab);
    logger().info("Lab created: id={} lineage={} v{}", lab.id(), lab.lineageId(), lab.version());
    return lab;
  }

  /** Sets the categorisation tags on a lab. */
  public Optional<Lab> tag(UUID id, Set<String> tags) {
    return repository.findById(id).map(l -> {
      Lab next = l.withTags(tags);
      repository.save(next);
      return next;
    });
  }

  /**
   * Applies a lifecycle transition (validated against {@link ContentStatus}).
   * Approval additionally requires a non-blank learning objective + acceptance
   * criteria.
   */
  public synchronized Optional<Lab> transition(UUID id, ContentStatus target) {
    Objects.requireNonNull(target, "target");
    return repository.findById(id).map(l -> {
      if (!l.status().canTransitionTo(target)) {
        throw new IllegalStateException(
            "illegal content transition " + l.status() + " → " + target);
      }
      if (target == ContentStatus.APPROVED
          && (l.learningObjective().isBlank() || l.acceptanceCriteria().isBlank())) {
        throw new IllegalStateException(
            "a lab needs a non-blank learning objective + acceptance criteria before approval");
      }
      Lab next = l.withStatus(target);
      repository.save(next);
      logger().info("Lab {} → {}", id, target);
      return next;
    });
  }

  public Optional<Lab> submitForReview(UUID id) {
    return transition(id, ContentStatus.IN_REVIEW);
  }

  public Optional<Lab> approve(UUID id) {
    return transition(id, ContentStatus.APPROVED);
  }

  /**
   * Approves a lab on behalf of {@code approverId}, enforcing segregation of
   * duties (§3.6/§17.2): a reviewer may not approve a lab they authored. Throws
   * {@link IllegalStateException} on a self-approval attempt. Fail-open when the
   * author is unknown (no authorship recorded / null approver) — by necessity,
   * SoD cannot be enforced against an unknown author; the authoring surface
   * always records the signed-in author at creation.
   */
  public Optional<Lab> approve(UUID id, Long approverId) {
    repository.findById(id).ifPresent(l -> {
      if (ContentAuthorshipProvider.registry().isAuthor(l.lineageId(), approverId)) {
        throw new IllegalStateException("an author cannot approve their own content");
      }
    });
    return approve(id);
  }

  public Optional<Lab> rejectToDraft(UUID id) {
    return transition(id, ContentStatus.DRAFT);
  }

  public Optional<Lab> publish(UUID id) {
    return transition(id, ContentStatus.PUBLISHED);
  }

  public Optional<Lab> deprecate(UUID id) {
    return transition(id, ContentStatus.DEPRECATED);
  }

  public Optional<Lab> archive(UUID id) {
    return transition(id, ContentStatus.ARCHIVED);
  }

  /** Labs awaiting a reviewer's verdict (IN_REVIEW) or ready to publish (APPROVED). */
  public List<Lab> pendingReview() {
    return repository.all().stream()
        .filter(l -> l.status() == ContentStatus.IN_REVIEW || l.status() == ContentStatus.APPROVED)
        .toList();
  }

  /** Labs a learner can submit against — PUBLISHED only. */
  public List<Lab> published() {
    return repository.all().stream()
        .filter(Lab::isPublished)
        .toList();
  }
}
