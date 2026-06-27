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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CoachingSlotService — opening slots under a published offer (P004)")
class CoachingSlotServiceTest {

  private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");
  private static final Instant END = Instant.parse("2026-09-01T10:00:00Z");

  private InMemoryCoachingOfferRepository offers;
  private InMemoryCoachingSlotRepository slots;
  private CoachingSlotService service;
  private CoachingOffer published;

  @BeforeEach
  void setUp() {
    offers = new InMemoryCoachingOfferRepository();
    slots = new InMemoryCoachingSlotRepository();
    service = new CoachingSlotService(offers, slots);
    published = CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60)
        .withStatus(ContentStatus.PUBLISHED);
    offers.save(published);
  }

  @Test
  @DisplayName("opening a slot under a published offer creates an OPEN slot carrying the coach")
  void open() {
    CoachingSlot slot = service.open(published.id(), 7L, START, END).orElseThrow();
    assertEquals(BookingStatus.OPEN, slot.status());
    assertEquals(7L, slot.coachId());
    assertEquals(published.id(), slot.offerId());
    assertEquals(1, slots.openSlotsOf(published.id()).size());
  }

  @Test
  @DisplayName("opening under an unpublished/unknown offer saves nothing")
  void unpublishedRefused() {
    CoachingOffer draft = CoachingOffer.draft("Draft", "d", "Sven", 7L, 60); // DRAFT
    offers.save(draft);
    assertTrue(service.open(draft.id(), 7L, START, END).isEmpty(), "unpublished refused");
    assertTrue(service.open(java.util.UUID.randomUUID(), 7L, START, END).isEmpty(), "unknown refused");
    assertTrue(slots.all().isEmpty());
  }

  @Test
  @DisplayName("a slot ending before it starts is rejected")
  void invalidTimes() {
    assertThrows(IllegalArgumentException.class, () -> service.open(published.id(), 7L, END, START));
  }

  @Test
  @DisplayName("only the offer's own coach may open slots under it (exit-review L2)")
  void onlyOwnCoachOpens() {
    assertTrue(service.open(published.id(), 9999L, START, END).isEmpty(),
        "another coach cannot open slots under this offer");
    assertTrue(slots.all().isEmpty());
  }

  @Test
  @DisplayName("complete fires only on the BOOKED→COMPLETED edge, by the owning coach (idempotent)")
  void completeEdge() {
    CoachingSlot slot = service.open(published.id(), 7L, START, END).orElseThrow();
    assertTrue(service.complete(slot.id(), 7L, "notes").isEmpty(), "an OPEN slot cannot be completed");
    service.book(slot.id(), 5005L, "Ada");
    assertTrue(service.complete(slot.id(), 9999L, "n").isEmpty(), "not this coach's slot");

    assertEquals(BookingStatus.COMPLETED,
        service.complete(slot.id(), 7L, "great").orElseThrow().status());
    // Re-completing a COMPLETED slot does nothing (no double-mint upstream).
    assertTrue(service.complete(slot.id(), 7L, "again").isEmpty());
  }

  @Test
  @DisplayName("the coach cancels an open slot; a non-owner cannot")
  void cancel() {
    CoachingSlot slot = service.open(published.id(), 7L, START, END).orElseThrow();
    assertTrue(service.cancelSlot(slot.id(), 9999L).isEmpty(), "not this coach's slot");
    assertEquals(BookingStatus.CANCELLED,
        service.cancelSlot(slot.id(), 7L).orElseThrow().status());
  }
}
