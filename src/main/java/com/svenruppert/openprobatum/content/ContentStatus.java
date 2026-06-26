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

package com.svenruppert.openprobatum.content;

import java.util.Set;

/**
 * The editorial lifecycle of an authored content object — question, offering,
 * path, module, assessment (concept §16.2). Transitions are constrained by
 * {@link #canTransitionTo(ContentStatus)}; learners only ever see
 * {@link #PUBLISHED} content.
 *
 * <pre>
 *   DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED → ARCHIVED
 *             ↑   ↓ (rejected)                    ↓
 *           DRAFT                               REPLACED (terminal)
 * </pre>
 *
 * @since V00.30.00
 */
public enum ContentStatus {

  /** Being authored; not visible to learners. */
  DRAFT,

  /** Submitted for review; awaiting a reviewer decision. */
  IN_REVIEW,

  /** Reviewed + approved; ready to publish. */
  APPROVED,

  /** Live + visible to learners. */
  PUBLISHED,

  /** Superseded but still reachable; being phased out. */
  DEPRECATED,

  /** Retired; not visible, terminal. */
  ARCHIVED,

  /** Replaced by a newer version; terminal. */
  REPLACED;

  private Set<ContentStatus> allowedTargets() {
    return switch (this) {
      case DRAFT -> Set.of(IN_REVIEW);
      case IN_REVIEW -> Set.of(APPROVED, DRAFT);          // approved or rejected back
      case APPROVED -> Set.of(PUBLISHED, DRAFT);          // publish or send back to edit
      case PUBLISHED -> Set.of(DEPRECATED, REPLACED);
      case DEPRECATED -> Set.of(ARCHIVED, REPLACED);
      case ARCHIVED, REPLACED -> Set.of();                // terminal
    };
  }

  /** @return {@code true} if a direct transition to {@code target} is allowed. */
  public boolean canTransitionTo(ContentStatus target) {
    return target != null && allowedTargets().contains(target);
  }

  /** @return {@code true} if this is a terminal state (no further transitions). */
  public boolean isTerminal() {
    return allowedTargets().isEmpty();
  }

  /** @return {@code true} only for {@link #PUBLISHED} — the learner-visible state. */
  public boolean isPublished() {
    return this == PUBLISHED;
  }
}
