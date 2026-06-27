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

package junit.com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.InMemoryBundleRepository;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.views.metrics.PackagingMetricsService;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopEnrolmentRepository;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PackagingMetricsService — bundle completions + workshop fill/attendance (P010)")
class PackagingMetricsServiceTest {

  private final InMemoryBundleRepository bundles = new InMemoryBundleRepository();
  private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
  private final InMemoryWorkshopRepository workshops = new InMemoryWorkshopRepository();
  private final InMemoryWorkshopEnrolmentRepository enrolments = new InMemoryWorkshopEnrolmentRepository();
  private final com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository coachingOffers =
      new com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository();
  private final com.svenruppert.openprobatum.coaching.InMemoryCoachingSlotRepository coachingSlots =
      new com.svenruppert.openprobatum.coaching.InMemoryCoachingSlotRepository();
  private final PackagingMetricsService metrics =
      new PackagingMetricsService(bundles, credentials, workshops, enrolments,
          coachingOffers, coachingSlots);

  @Test
  @DisplayName("bundle metrics count the completion credentials issued for each bundle")
  void bundleCompletions() {
    Bundle bundle = Bundle.draft("Pack", "d", Set.of(UUID.randomUUID()));
    bundles.save(bundle);
    credentials.save(Credential.issue("Pack", CredentialType.COMPLETION_CERTIFICATE, 1L,
        "Ada", "Academy", Instant.parse("2026-01-01T00:00:00Z"), null,
        Evidence.bundleCompleted(bundle.id(), bundle.version())));
    credentials.save(Credential.issue("Pack", CredentialType.COMPLETION_CERTIFICATE, 2L,
        "Bob", "Academy", Instant.parse("2026-01-01T00:00:00Z"), null,
        Evidence.bundleCompleted(bundle.id(), bundle.version())));
    // A credential for a different bundle must not be counted.
    credentials.save(Credential.issue("Other", CredentialType.COMPLETION_CERTIFICATE, 3L,
        "Cy", "Academy", Instant.parse("2026-01-01T00:00:00Z"), null,
        Evidence.bundleCompleted(UUID.randomUUID(), 1)));

    var m = metrics.allBundleMetrics();
    assertEquals(1, m.size());
    assertEquals(2, m.get(0).completions());
  }

  @Test
  @DisplayName("workshop metrics report fill (enrolled/capacity) + attendance (attended/enrolled)")
  void workshopFillAndAttendance() {
    Workshop w = Workshop.draft("Day", "d",
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"), 4, "Sven");
    workshops.save(w);
    // 2 enrolled (of 4 → 50% fill); of those 1 attended (→ 50% attendance); 1 cancelled is ignored.
    enrolments.save(WorkshopEnrolment.enrol(w.id(), 1L, "Ada").attended());
    enrolments.save(WorkshopEnrolment.enrol(w.id(), 2L, "Bob"));
    enrolments.save(WorkshopEnrolment.enrol(w.id(), 3L, "Cy").cancelled());

    var m = metrics.allWorkshopMetrics().get(0);
    assertEquals(2, m.enrolled(), "cancelled does not count as enrolled");
    assertEquals(1, m.attended());
    assertEquals(0.5, m.fillRate(), 1e-9);
    assertEquals(0.5, m.attendanceRate(), 1e-9);
  }

  @Test
  @DisplayName("coaching metrics report slots, booked + completed and the completion rate")
  void coachingSlotsAndCompletions() {
    com.svenruppert.openprobatum.coaching.CoachingOffer offer =
        com.svenruppert.openprobatum.coaching.CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60);
    coachingOffers.save(offer);
    java.time.Instant s = java.time.Instant.parse("2026-09-01T09:00:00Z");
    java.time.Instant e = java.time.Instant.parse("2026-09-01T10:00:00Z");
    // 4 non-cancelled slots: 1 completed, 1 booked, 2 open; 1 cancelled is ignored.
    coachingSlots.save(com.svenruppert.openprobatum.coaching.CoachingSlot.open(offer.id(), 7L, s, e)
        .booked(1L, "Ada").completed("ok"));
    coachingSlots.save(com.svenruppert.openprobatum.coaching.CoachingSlot.open(offer.id(), 7L, s, e)
        .booked(2L, "Bob"));
    coachingSlots.save(com.svenruppert.openprobatum.coaching.CoachingSlot.open(offer.id(), 7L, s, e));
    coachingSlots.save(com.svenruppert.openprobatum.coaching.CoachingSlot.open(offer.id(), 7L, s, e));
    coachingSlots.save(com.svenruppert.openprobatum.coaching.CoachingSlot.open(offer.id(), 7L, s, e)
        .cancelled());

    var m = metrics.allCoachingMetrics().get(0);
    assertEquals(4, m.slots(), "cancelled is excluded");
    assertEquals(1, m.booked());
    assertEquals(1, m.completed());
    assertEquals(0.25, m.completionRate(), 1e-9);
  }

  @Test
  @DisplayName("fill rate is clamped to 100% when a freed seat is re-enrolled (exit-review M1)")
  void fillRateClamped() {
    Workshop w = Workshop.draft("Tiny", "d",
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"), 1, "Sven");
    workshops.save(w);
    // capacity 1, but an attended seat + a re-enrolment → 2 non-cancelled enrolments.
    enrolments.save(WorkshopEnrolment.enrol(w.id(), 1L, "Ada").attended());
    enrolments.save(WorkshopEnrolment.enrol(w.id(), 2L, "Bob"));

    var m = metrics.allWorkshopMetrics().get(0);
    assertEquals(2, m.enrolled());
    assertEquals(1.0, m.fillRate(), 1e-9, "rate is capped at 1.0, not 2.0");
  }

  @Test
  @DisplayName("a workshop with no enrolments reports zero rates, not a divide-by-zero")
  void workshopNoEnrolments() {
    Workshop w = Workshop.draft("Empty", "d",
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"), 4, "Sven");
    workshops.save(w);
    var m = metrics.allWorkshopMetrics().get(0);
    assertEquals(0.0, m.fillRate(), 1e-9);
    assertEquals(0.0, m.attendanceRate(), 1e-9);
  }
}
