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

package junit.com.svenruppert.openprobatum.workshop;

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.workshop.EnrolmentStatus;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopEnrolmentRepository;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WorkshopEnrolmentService — capacity-safe enrol (P008)")
class WorkshopEnrolmentServiceTest {

  private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");
  private static final Instant END = Instant.parse("2026-09-01T17:00:00Z");

  private InMemoryWorkshopRepository workshops;
  private InMemoryWorkshopEnrolmentRepository enrolments;
  private WorkshopEnrolmentService service;
  private Workshop workshop;

  @BeforeEach
  void setUp() {
    workshops = new InMemoryWorkshopRepository();
    enrolments = new InMemoryWorkshopEnrolmentRepository();
    service = new WorkshopEnrolmentService(workshops, enrolments);
    workshop = Workshop.draft("Vaadin Day", "d", START, END, 2, "Sven")
        .withStatus(ContentStatus.PUBLISHED);
    workshops.save(workshop);
  }

  @Test
  @DisplayName("a learner enrols in a published workshop → ENROLLED, occupying a seat")
  void enrol() {
    var e = service.enrol(workshop.id(), 1L, "Ada").orElseThrow();
    assertEquals(EnrolmentStatus.ENROLLED, e.status());
    assertEquals(1, enrolments.activeCount(workshop.id()));
  }

  @Test
  @DisplayName("enrolling twice, an unpublished workshop, and a full workshop are all refused")
  void refusals() {
    service.enrol(workshop.id(), 1L, "Ada");
    assertTrue(service.enrol(workshop.id(), 1L, "Ada").isEmpty(), "no double-enrol");

    Workshop draft = Workshop.draft("Draft", "d", START, END, 5, "Sven"); // DRAFT
    workshops.save(draft);
    assertTrue(service.enrol(draft.id(), 2L, "Bob").isEmpty(), "unpublished refused");

    service.enrol(workshop.id(), 3L, "Cy"); // fills the 2nd of 2 seats
    assertTrue(service.enrol(workshop.id(), 4L, "Di").isEmpty(), "full refused");
  }

  @Test
  @DisplayName("cancelling frees the seat for another learner")
  void cancelFreesSeat() {
    var ada = service.enrol(workshop.id(), 1L, "Ada").orElseThrow();
    service.enrol(workshop.id(), 2L, "Bob"); // 2/2 full
    assertTrue(service.enrol(workshop.id(), 3L, "Cy").isEmpty(), "full");

    service.cancel(ada.id(), 1L);
    assertEquals(1, enrolments.activeCount(workshop.id()), "a seat freed");
    assertEquals(EnrolmentStatus.ENROLLED,
        service.enrol(workshop.id(), 3L, "Cy").orElseThrow().status(), "now Cy fits");
  }

  @Test
  @DisplayName("recordAttendance fires only on the ENROLLED→ATTENDED edge (idempotent)")
  void attendanceEdge() {
    var e = service.enrol(workshop.id(), 1L, "Ada").orElseThrow();
    assertEquals(EnrolmentStatus.ATTENDED,
        service.recordAttendance(e.id(), 9L).orElseThrow().status());
    // Re-recording a now-ATTENDED enrolment does nothing (no double-mint upstream).
    assertTrue(service.recordAttendance(e.id(), 9L).isEmpty());
    assertTrue(service.markNoShow(e.id(), 9L).isEmpty());

    var bob = service.enrol(workshop.id(), 2L, "Bob").orElseThrow();
    assertEquals(EnrolmentStatus.NO_SHOW, service.markNoShow(bob.id(), 9L).orElseThrow().status());
  }

  @Test
  @DisplayName("concurrent enrolments never exceed capacity (no overbooking)")
  void noOverbooking() throws InterruptedException {
    Workshop big = Workshop.draft("Big", "d", START, END, 5, "Sven")
        .withStatus(ContentStatus.PUBLISHED);
    workshops.save(big);

    int threads = 24;
    var pool = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(threads);
    var seated = new AtomicInteger();
    for (int i = 0; i < threads; i++) {
      long learner = 1000 + i; // each a distinct learner
      pool.execute(() -> {
        try {
          start.await();
          if (service.enrol(big.id(), learner, "L" + learner).isPresent()) {
            seated.incrementAndGet();
          }
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      });
    }
    start.countDown();
    assertTrue(done.await(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    assertEquals(5, seated.get(), "exactly capacity seats granted");
    assertEquals(5, enrolments.activeCount(big.id()));
  }
}
