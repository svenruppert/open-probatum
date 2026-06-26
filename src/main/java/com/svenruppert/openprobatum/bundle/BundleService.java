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

package com.svenruppert.openprobatum.bundle;

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Operations on authored {@link Bundle}s (concept §7.x / §16.3): create, tag,
 * move through the editorial lifecycle, and list those awaiting review or
 * published. Transitions are validated against {@link ContentStatus}; a bundle
 * may only be approved once it has at least one member offering (a published
 * bundle must grant something), and a reviewer may not approve a bundle they
 * authored.
 *
 * @since V00.50.00
 */
public final class BundleService implements HasLogger {

  private final BundleRepository repository;

  public BundleService(BundleRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public BundleService() {
    this(BundleRepositoryProvider.repository());
  }

  /** Stores a new (draft) bundle. */
  public Bundle create(Bundle bundle) {
    Objects.requireNonNull(bundle, "bundle");
    repository.save(bundle);
    logger().info("Bundle created: id={} lineage={} v{}",
        bundle.id(), bundle.lineageId(), bundle.version());
    return bundle;
  }

  /** Sets the categorisation tags on a bundle. */
  public Optional<Bundle> tag(UUID id, Set<String> tags) {
    return repository.findById(id).map(b -> {
      Bundle next = b.withTags(tags);
      repository.save(next);
      return next;
    });
  }

  /**
   * Applies a lifecycle transition (validated against {@link ContentStatus}).
   * Approval additionally requires at least one member offering.
   */
  public synchronized Optional<Bundle> transition(UUID id, ContentStatus target) {
    Objects.requireNonNull(target, "target");
    return repository.findById(id).map(b -> {
      if (!b.status().canTransitionTo(target)) {
        throw new IllegalStateException(
            "illegal content transition " + b.status() + " → " + target);
      }
      if (target == ContentStatus.APPROVED && b.offeringIds().isEmpty()) {
        throw new IllegalStateException("a bundle needs at least one member offering before approval");
      }
      Bundle next = b.withStatus(target);
      repository.save(next);
      logger().info("Bundle {} → {}", id, target);
      return next;
    });
  }

  public Optional<Bundle> submitForReview(UUID id) {
    return transition(id, ContentStatus.IN_REVIEW);
  }

  public Optional<Bundle> approve(UUID id) {
    return transition(id, ContentStatus.APPROVED);
  }

  /**
   * Approves a bundle on behalf of {@code approverId}, enforcing segregation of
   * duties (§3.6/§17.2): a reviewer may not approve a bundle they authored. Throws
   * {@link IllegalStateException} on a self-approval attempt. Fail-open when the
   * author is unknown — by necessity; the authoring surface always records the
   * signed-in author at creation.
   */
  public Optional<Bundle> approve(UUID id, Long approverId) {
    repository.findById(id).ifPresent(b -> {
      if (ContentAuthorshipProvider.registry().isAuthor(b.lineageId(), approverId)) {
        throw new IllegalStateException("an author cannot approve their own content");
      }
    });
    return approve(id);
  }

  public Optional<Bundle> rejectToDraft(UUID id) {
    return transition(id, ContentStatus.DRAFT);
  }

  public Optional<Bundle> publish(UUID id) {
    return transition(id, ContentStatus.PUBLISHED);
  }

  public Optional<Bundle> deprecate(UUID id) {
    return transition(id, ContentStatus.DEPRECATED);
  }

  public Optional<Bundle> archive(UUID id) {
    return transition(id, ContentStatus.ARCHIVED);
  }

  /** Bundles awaiting a reviewer's verdict (IN_REVIEW) or ready to publish (APPROVED). */
  public List<Bundle> pendingReview() {
    return repository.all().stream()
        .filter(b -> b.status() == ContentStatus.IN_REVIEW || b.status() == ContentStatus.APPROVED)
        .toList();
  }

  /** Bundles a learner can browse + be granted — PUBLISHED only. */
  public List<Bundle> published() {
    return repository.all().stream()
        .filter(Bundle::isPublished)
        .toList();
  }
}
