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

package com.svenruppert.openprobatum.coaching;

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Operations on authored {@link CoachingOffer}s (concept §7.x / §16.3): create,
 * tag, move through the editorial lifecycle, list those awaiting review or
 * published. Transitions are validated against {@link ContentStatus}; approval
 * requires a non-blank learning objective, and a reviewer may not approve an
 * offer they authored.
 *
 * @since V00.60.00
 */
public final class CoachingOfferService implements HasLogger {

  private final CoachingOfferRepository repository;

  public CoachingOfferService(CoachingOfferRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public CoachingOfferService() {
    this(CoachingOfferRepositoryProvider.repository());
  }

  /** Stores a new (draft) offer. */
  public CoachingOffer create(CoachingOffer offer) {
    Objects.requireNonNull(offer, "offer");
    repository.save(offer);
    logger().info("Coaching offer created: id={} lineage={} v{}",
        offer.id(), offer.lineageId(), offer.version());
    return offer;
  }

  /** Sets the categorisation tags on an offer. */
  public Optional<CoachingOffer> tag(UUID id, Set<String> tags) {
    return repository.findById(id).map(o -> {
      CoachingOffer next = o.withTags(tags);
      repository.save(next);
      return next;
    });
  }

  /**
   * Applies a lifecycle transition (validated against {@link ContentStatus}).
   * Approval additionally requires a non-blank learning objective.
   */
  public synchronized Optional<CoachingOffer> transition(UUID id, ContentStatus target) {
    Objects.requireNonNull(target, "target");
    return repository.findById(id).map(o -> {
      if (!o.status().canTransitionTo(target)) {
        throw new IllegalStateException(
            "illegal content transition " + o.status() + " → " + target);
      }
      if (target == ContentStatus.APPROVED && o.learningObjective().isBlank()) {
        throw new IllegalStateException(
            "a coaching offer needs a non-blank learning objective before approval");
      }
      CoachingOffer next = o.withStatus(target);
      repository.save(next);
      logger().info("Coaching offer {} → {}", id, target);
      return next;
    });
  }

  public Optional<CoachingOffer> submitForReview(UUID id) {
    return transition(id, ContentStatus.IN_REVIEW);
  }

  public Optional<CoachingOffer> approve(UUID id) {
    return transition(id, ContentStatus.APPROVED);
  }

  /**
   * Approves an offer on behalf of {@code approverId}, enforcing segregation of
   * duties (§3.6/§17.2): a reviewer may not approve an offer they authored. Throws
   * {@link IllegalStateException} on a self-approval attempt. Fail-open when the
   * author is unknown — by necessity; the authoring surface always records the
   * signed-in author at creation.
   */
  public Optional<CoachingOffer> approve(UUID id, Long approverId) {
    repository.findById(id).ifPresent(o -> {
      if (ContentAuthorshipProvider.registry().isAuthor(o.lineageId(), approverId)) {
        throw new IllegalStateException("an author cannot approve their own content");
      }
    });
    return approve(id);
  }

  public Optional<CoachingOffer> rejectToDraft(UUID id) {
    return transition(id, ContentStatus.DRAFT);
  }

  public Optional<CoachingOffer> publish(UUID id) {
    return transition(id, ContentStatus.PUBLISHED);
  }

  public Optional<CoachingOffer> deprecate(UUID id) {
    return transition(id, ContentStatus.DEPRECATED);
  }

  public Optional<CoachingOffer> archive(UUID id) {
    return transition(id, ContentStatus.ARCHIVED);
  }

  /** Offers awaiting a reviewer's verdict (IN_REVIEW) or ready to publish (APPROVED). */
  public List<CoachingOffer> pendingReview() {
    return repository.all().stream()
        .filter(o -> o.status() == ContentStatus.IN_REVIEW || o.status() == ContentStatus.APPROVED)
        .toList();
  }

  /** Offers a coach can open slots under / a learner can book — PUBLISHED only. */
  public List<CoachingOffer> published() {
    return repository.all().stream()
        .filter(CoachingOffer::isPublished)
        .toList();
  }
}
