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

import com.svenruppert.openprobatum.security.AppClock;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A learner's seat in a {@link Workshop} (concept §7.x). An {@code ENROLLED}
 * enrolment occupies a seat (counts towards capacity); after the session an
 * instructor moves it to {@code ATTENDED} (the evidence behind a workshop
 * certificate) or {@code NO_SHOW}; the learner may {@code CANCELLED} a seat
 * before the session, freeing it.
 *
 * @param id          the enrolment id
 * @param workshopId  the workshop seat is held for
 * @param recipientId the stable id of the enrolled learner (the wallet key)
 * @param learnerName the learner's display name (shown to the instructor)
 * @param status      the enrolment state
 * @param enrolledAt  when the learner enrolled
 * @param decidedAt   when an instructor recorded attendance, or {@code null} while ENROLLED
 * @since V00.50.00
 */
public record WorkshopEnrolment(UUID id, UUID workshopId, Long recipientId, String learnerName,
                                EnrolmentStatus status, Instant enrolledAt, Instant decidedAt) {

  public WorkshopEnrolment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(workshopId, "workshopId");
    Objects.requireNonNull(recipientId, "recipientId");
    Objects.requireNonNull(learnerName, "learnerName");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(enrolledAt, "enrolledAt");
  }

  /** A fresh {@code ENROLLED} seat, stamped from {@link AppClock}. */
  public static WorkshopEnrolment enrol(UUID workshopId, Long recipientId, String learnerName) {
    return new WorkshopEnrolment(UUID.randomUUID(), workshopId, recipientId, learnerName,
        EnrolmentStatus.ENROLLED, AppClock.now(), null);
  }

  private WorkshopEnrolment withStatus(EnrolmentStatus newStatus, Instant decided) {
    return new WorkshopEnrolment(id, workshopId, recipientId, learnerName, newStatus,
        enrolledAt, decided);
  }

  /** A copy marked {@code ATTENDED}, stamped now. */
  public WorkshopEnrolment attended() {
    return withStatus(EnrolmentStatus.ATTENDED, AppClock.now());
  }

  /** A copy marked {@code NO_SHOW}, stamped now. */
  public WorkshopEnrolment noShow() {
    return withStatus(EnrolmentStatus.NO_SHOW, AppClock.now());
  }

  /** A copy marked {@code CANCELLED} (the seat is freed). */
  public WorkshopEnrolment cancelled() {
    return withStatus(EnrolmentStatus.CANCELLED, AppClock.now());
  }

  /** @return {@code true} when this enrolment occupies a seat (ENROLLED). */
  public boolean isActive() {
    return status == EnrolmentStatus.ENROLLED;
  }

  /** @return {@code true} when the learner attended. */
  public boolean isAttended() {
    return status == EnrolmentStatus.ATTENDED;
  }

  /** Whether this enrolment belongs to the learner with id {@code userId}. */
  public boolean isHeldBy(Long userId) {
    return recipientId.equals(userId);
  }

  /** The optional decision timestamp. */
  public Optional<Instant> decidedAtOpt() {
    return Optional.ofNullable(decidedAt);
  }
}
