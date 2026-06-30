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

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The editorial lifecycle of catalog {@link Offering}s (concept §16.2/16.3):
 * an author submits an offering for review, a reviewer approves or rejects it,
 * and an approved offering is published so learners can reach it. Transitions
 * are validated against {@link ContentStatus}; each transition saves a new
 * immutable {@link Offering} record (the status copy) to the catalog.
 *
 * @since V00.30.00
 */
public final class CatalogLifecycleService implements HasLogger {

  /**
   * Process-wide monitor for catalog mutations — the check-then-act in
   * {@link #transition} and the guarded hard-delete in
   * {@link OfferingAuthoringService#delete(java.util.UUID)}. Views create a fresh
   * service per action, so an instance lock would serialise nothing across
   * callers/threads; this shared static lock does (the mint-once pattern, like
   * {@code CoachingSlotService.SLOT_LOCK}). Package-private so the offering-delete
   * path can serialise against a concurrent transition on the same monitor.
   */
  static final Object LOCK = new Object();

  private final CatalogRepository repository;

  public CatalogLifecycleService(CatalogRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public CatalogLifecycleService() {
    this(CatalogRepositoryProvider.repository());
  }

  /** Applies a lifecycle transition, validated against {@link ContentStatus}. */
  public Optional<Offering> transition(UUID id, ContentStatus target) {
    Objects.requireNonNull(target, "target");
    synchronized (LOCK) {
      return repository.findById(id).map(o -> {
        if (!o.status().canTransitionTo(target)) {
          throw new IllegalStateException(
              "illegal content transition " + o.status() + " → " + target);
        }
        Offering next = o.withStatus(target);
        repository.save(next);
        logger().info("Offering {} → {}", id, target);
        return next;
      });
    }
  }

  public Optional<Offering> submitForReview(UUID id) {
    return transition(id, ContentStatus.IN_REVIEW);
  }

  public Optional<Offering> approve(UUID id) {
    return transition(id, ContentStatus.APPROVED);
  }

  /**
   * Approves an offering on behalf of {@code approverId}, enforcing segregation
   * of duties (§3.6/§17.2): a reviewer may not approve content they authored.
   * Throws {@link IllegalStateException} on a self-approval attempt.
   */
  public Optional<Offering> approve(UUID id, Long approverId) {
    repository.findById(id).ifPresent(o -> {
      if (com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
          .isAuthor(o.lineageId(), approverId)) {
        throw new IllegalStateException("an author cannot approve their own content");
      }
    });
    return approve(id);
  }

  public Optional<Offering> rejectToDraft(UUID id) {
    return transition(id, ContentStatus.DRAFT);
  }

  public Optional<Offering> publish(UUID id) {
    return transition(id, ContentStatus.PUBLISHED);
  }

  public Optional<Offering> deprecate(UUID id) {
    return transition(id, ContentStatus.DEPRECATED);
  }

  public Optional<Offering> archive(UUID id) {
    return transition(id, ContentStatus.ARCHIVED);
  }

  /** Offerings awaiting a reviewer's verdict (IN_REVIEW) or ready to publish (APPROVED). */
  public List<Offering> pendingReview() {
    return repository.all().stream()
        .filter(o -> o.status() == ContentStatus.IN_REVIEW || o.status() == ContentStatus.APPROVED)
        .toList();
  }
}
