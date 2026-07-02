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
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The workshop enrolment flow (concept §7.x): a learner enrols in a published
 * workshop up to its capacity, and may cancel before the session. The seat
 * accounting is the exactly-once / no-overbooking edge — the capacity check and
 * the seat creation are serialised on a shared monitor, so concurrent learners
 * never push an enrolment past capacity. Attendance verdicts land in P009.
 *
 * @since V00.50.00
 */
public final class WorkshopEnrolmentService implements HasLogger {

  /** Process-wide lock for the seat edge — see the V00.40.00 mint-once pattern. */
  private static final Object SEAT_LOCK = new Object();

  private final WorkshopRepository workshops;
  private final WorkshopEnrolmentRepository enrolments;

  public WorkshopEnrolmentService(WorkshopRepository workshops,
                                  WorkshopEnrolmentRepository enrolments) {
    this.workshops = Objects.requireNonNull(workshops, "workshops");
    this.enrolments = Objects.requireNonNull(enrolments, "enrolments");
  }

  public WorkshopEnrolmentService() {
    this(WorkshopRepositoryProvider.repository(), WorkshopEnrolmentRepositoryProvider.repository());
  }

  /**
   * Enrols {@code recipientId} in a published workshop, atomically respecting
   * capacity (no overbooking) and refusing a duplicate enrolment. Returns empty
   * (and saves nothing) when the workshop is unknown/unpublished, the learner is
   * already enrolled, or the workshop is full.
   */
  public Optional<WorkshopEnrolment> enrol(UUID workshopId, Long recipientId, String learnerName) {
    Objects.requireNonNull(workshopId, "workshopId");
    if (recipientId == null) {
      return Optional.empty();
    }
    synchronized (SEAT_LOCK) {
      Workshop workshop = workshops.findById(workshopId).filter(Workshop::isPublished).orElse(null);
      if (workshop == null) {
        return Optional.empty();
      }
      // Enrolment is a "before the session" action — refuse once the session has
      // started (and therefore once it is over), so a learner cannot enrol into a
      // running or past workshop, nor re-enrol into a finished one after a NO_SHOW.
      if (!AppClock.now().isBefore(workshop.startsAt())) {
        return Optional.empty();
      }
      boolean already = enrolments.forWorkshop(workshopId).stream()
          .anyMatch(e -> e.isHeldBy(recipientId) && (e.isActive() || e.isAttended()));
      if (already || enrolments.activeCount(workshopId) >= workshop.capacity()) {
        return Optional.empty();
      }
      WorkshopEnrolment enrolment = WorkshopEnrolment.enrol(workshopId, recipientId, learnerName);
      enrolments.save(enrolment);
      logger().info("Enrolment {} by '{}' (id={}) in workshop {}",
          enrolment.id(), learnerName, recipientId, workshopId);
      return Optional.of(enrolment);
    }
  }

  /**
   * Records attendance for an ENROLLED enrolment on behalf of {@code instructorId}
   * — the ENROLLED→ATTENDED edge (idempotent: a non-ENROLLED enrolment yields
   * empty, so re-recording never re-fires and never double-mints the certificate).
   * The view mints the workshop credential only on the non-empty result.
   *
   * <p>{@code instructorId} is recorded for audit but not checked against the
   * workshop's instructor: any holder of {@code workshop:run} may record
   * attendance, consistent with the shared staff-role model used elsewhere.
   */
  public Optional<WorkshopEnrolment> recordAttendance(UUID enrolmentId, Long instructorId) {
    return decide(enrolmentId, WorkshopEnrolment::attended, "attended", instructorId);
  }

  /** Marks an ENROLLED enrolment a NO_SHOW (same idempotent edge as attendance). */
  public Optional<WorkshopEnrolment> markNoShow(UUID enrolmentId, Long instructorId) {
    return decide(enrolmentId, WorkshopEnrolment::noShow, "no-show", instructorId);
  }

  private Optional<WorkshopEnrolment> decide(UUID enrolmentId,
                                             java.util.function.UnaryOperator<WorkshopEnrolment> verdict,
                                             String action, Long instructorId) {
    Objects.requireNonNull(enrolmentId, "enrolmentId");
    synchronized (SEAT_LOCK) {
      return enrolments.findById(enrolmentId)
          .filter(WorkshopEnrolment::isActive)
          // An attendance / no-show verdict is a "session has run" fact — refuse
          // it before the session has started, so it cannot be recorded early.
          .filter(this::sessionHasStarted)
          .map(e -> {
            WorkshopEnrolment decided = verdict.apply(e);
            enrolments.save(decided);
            logger().info("Enrolment {} {} by instructor {}", enrolmentId, action, instructorId);
            return decided;
          });
    }
  }

  /** Cancels the learner's own active seat, freeing it. Empty when not the learner's / not active. */
  public Optional<WorkshopEnrolment> cancel(UUID enrolmentId, Long recipientId) {
    Objects.requireNonNull(enrolmentId, "enrolmentId");
    synchronized (SEAT_LOCK) {
      return enrolments.findById(enrolmentId)
          .filter(e -> e.isHeldBy(recipientId) && e.isActive())
          // Cancellation is only allowed before the session — otherwise a
          // no-show could cancel after the fact to dodge the NO_SHOW record.
          .filter(e -> !sessionHasStarted(e))
          .map(e -> {
            WorkshopEnrolment cancelled = e.cancelled();
            enrolments.save(cancelled);
            logger().info("Enrolment {} cancelled by {}", enrolmentId, recipientId);
            return cancelled;
          });
    }
  }

  /**
   * Whether the enrolment's workshop session has already begun ({@code now} is at
   * or after {@code startsAt}). An unknown workshop is treated as not-started so a
   * verdict is refused rather than recorded against a missing schedule.
   */
  private boolean sessionHasStarted(WorkshopEnrolment enrolment) {
    return workshops.findById(enrolment.workshopId())
        .map(w -> !AppClock.now().isBefore(w.startsAt()))
        .orElse(false);
  }
}
