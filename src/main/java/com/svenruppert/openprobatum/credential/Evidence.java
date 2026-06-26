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

package com.svenruppert.openprobatum.credential;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The basis on which a credential was issued (concept §10.6): what the holder
 * did to earn it, captured so the credential stays reproducible (§16.4).
 *
 * <ul>
 *   <li>{@link Type#ASSESSMENT_PASSED} — {@code sourceId} is the assessment id,
 *       {@code sourceVersion} the assessment version graded against.</li>
 *   <li>{@link Type#LEARNING_PATH_COMPLETED} — {@code sourceId} is the offering
 *       id, {@code sourceVersion} the offering version that was completed.</li>
 *   <li>{@link Type#MANUAL_AWARD} — a credential-manager award with no machine
 *       source ({@code sourceId} is {@code null}, {@code sourceVersion} 0).</li>
 * </ul>
 *
 * @param type          the kind of evidence
 * @param sourceId      the assessment / offering id, or {@code null} for a manual award
 * @param sourceVersion the version of the source content (§16.4); 0 for a manual award
 * @since V00.30.00
 */
public record Evidence(Type type, UUID sourceId, int sourceVersion) {

  /** The kind of basis behind a credential (§10.6). */
  public enum Type {
    /** A specific assessment version was passed. */
    ASSESSMENT_PASSED,
    /** A specific offering version's learning path was completed. */
    LEARNING_PATH_COMPLETED,
    /** A manual award by a credential manager, with no machine source. */
    MANUAL_AWARD
  }

  public Evidence {
    Objects.requireNonNull(type, "type");
    if (type != Type.MANUAL_AWARD) {
      Objects.requireNonNull(sourceId, "a non-manual evidence needs a source id");
      if (sourceVersion < 1) {
        throw new IllegalArgumentException("a non-manual evidence needs a source version >= 1");
      }
    }
  }

  /** Evidence that {@code assessmentId} (at {@code version}) was passed. */
  public static Evidence assessmentPassed(UUID assessmentId, int version) {
    return new Evidence(Type.ASSESSMENT_PASSED, assessmentId, version);
  }

  /** Evidence that {@code offeringId}'s learning path (at {@code version}) was completed. */
  public static Evidence pathCompleted(UUID offeringId, int version) {
    return new Evidence(Type.LEARNING_PATH_COMPLETED, offeringId, version);
  }

  /** Evidence for a manual credential-manager award (no machine source). */
  public static Evidence manualAward() {
    return new Evidence(Type.MANUAL_AWARD, null, 0);
  }

  /** The source content id, when there is a machine source. */
  public Optional<UUID> source() {
    return Optional.ofNullable(sourceId);
  }
}
