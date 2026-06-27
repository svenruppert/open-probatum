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

package junit.com.svenruppert.openprobatum.coaching;

import com.svenruppert.openprobatum.coaching.BookingStatus;
import com.svenruppert.openprobatum.coaching.CoachingOffer;
import com.svenruppert.openprobatum.coaching.CoachingSlot;
import com.svenruppert.openprobatum.coaching.CoachingSlotService;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingSlotRepository;
import com.svenruppert.openprobatum.content.ContentStatus;
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

@DisplayName("Coaching booking — one learner per slot, atomic (P005)")
class CoachingBookingTest {

  private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");
  private static final Instant END = Instant.parse("2026-09-01T10:00:00Z");

  private InMemoryCoachingSlotRepository slots;
  private CoachingSlotService service;
  private CoachingSlot slot;

  @BeforeEach
  void setUp() {
    InMemoryCoachingOfferRepository offers = new InMemoryCoachingOfferRepository();
    slots = new InMemoryCoachingSlotRepository();
    service = new CoachingSlotService(offers, slots);
    CoachingOffer offer = CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60)
        .withStatus(ContentStatus.PUBLISHED);
    offers.save(offer);
    slot = service.open(offer.id(), 7L, START, END).orElseThrow();
  }

  @Test
  @DisplayName("a learner books an open slot → BOOKED carrying their id")
  void book() {
    CoachingSlot booked = service.book(slot.id(), 1001L, "Ada").orElseThrow();
    assertEquals(BookingStatus.BOOKED, booked.status());
    assertEquals(1001L, booked.recipientId());
    assertEquals("Ada", booked.learnerName());
  }

  @Test
  @DisplayName("a second learner cannot book an already-booked slot")
  void noDoubleBooking() {
    service.book(slot.id(), 1001L, "Ada");
    assertTrue(service.book(slot.id(), 2002L, "Bob").isEmpty(), "slot no longer open");
    assertEquals(1001L, slots.findById(slot.id()).orElseThrow().recipientId());
  }

  @Test
  @DisplayName("releasing a booking reopens the slot for another learner")
  void releaseReopens() {
    service.book(slot.id(), 1001L, "Ada");
    service.cancelBooking(slot.id(), 1001L);
    assertEquals(BookingStatus.OPEN, slots.findById(slot.id()).orElseThrow().status());
    assertEquals(BookingStatus.BOOKED,
        service.book(slot.id(), 2002L, "Bob").orElseThrow().status(), "now Bob can book");
  }

  @Test
  @DisplayName("concurrent bookers of the same slot — exactly one wins (atomic edge)")
  void concurrentBooking() throws InterruptedException {
    int threads = 16;
    var pool = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(threads);
    var wins = new AtomicInteger();
    for (int i = 0; i < threads; i++) {
      long learner = 1000 + i;
      pool.execute(() -> {
        try {
          start.await();
          if (service.book(slot.id(), learner, "L" + learner).isPresent()) {
            wins.incrementAndGet();
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

    assertEquals(1, wins.get(), "exactly one learner books the slot");
    assertEquals(BookingStatus.BOOKED, slots.findById(slot.id()).orElseThrow().status());
  }
}
