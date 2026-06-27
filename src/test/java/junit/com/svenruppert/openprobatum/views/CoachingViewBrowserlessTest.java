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

import com.svenruppert.openprobatum.coaching.BookingStatus;
import com.svenruppert.openprobatum.coaching.CoachingOffer;
import com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider;
import com.svenruppert.openprobatum.coaching.CoachingSlot;
import com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingSlotRepository;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.CoachingView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CoachingView — learner books a coaching slot (P005)")
class CoachingViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCoachingOfferRepository offers;
  private InMemoryCoachingSlotRepository slots;
  private CoachingSlot slot;

  @BeforeEach
  void setUp() {
    offers = new InMemoryCoachingOfferRepository();
    slots = new InMemoryCoachingSlotRepository();
    CoachingOfferRepositoryProvider.setRepository(offers);
    CoachingSlotRepositoryProvider.setRepository(slots);
    CoachingOffer offer = CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60)
        .withStatus(ContentStatus.PUBLISHED);
    offers.save(offer);
    slot = CoachingSlot.open(offer.id(), 7L,
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T10:00:00Z"));
    slots.save(slot);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CoachingOfferRepositoryProvider.reset();
    CoachingSlotRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  @Test
  @DisplayName("a learner books an open slot → BOOKED and it shows in their bookings")
  void books() {
    CoachingView view = new CoachingView();
    assertEquals(List.of(slot.id().toString()), attributes(view, "data-slot"));

    click(view, "book");

    assertEquals(BookingStatus.BOOKED, slots.findById(slot.id()).orElseThrow().status());
    assertEquals(1001L, slots.findById(slot.id()).orElseThrow().recipientId());
    assertEquals(List.of(BookingStatus.BOOKED.name()), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("a learner sees only their own bookings")
  void ownDataOnly() {
    slots.save(slot.booked(2002L, "Bob")); // another learner's booking
    CoachingView view = new CoachingView();
    assertTrue(attributes(view, "data-booking").isEmpty(), "Ada has no booking of her own yet");
  }

  private static void click(Component root, String action) {
    List<Button> buttons = new ArrayList<>();
    collectButtons(root, action, buttons);
    buttons.get(0).click();
  }

  private static void collectButtons(Component c, String action, List<Button> out) {
    if (c instanceof Button b && action.equals(b.getElement().getAttribute("data-action"))) {
      out.add(b);
    }
    c.getChildren().forEach(child -> collectButtons(child, action, out));
  }

  private static List<String> attributes(Component root, String name) {
    List<String> values = new ArrayList<>();
    collect(root, name, values);
    return values;
  }

  private static void collect(Component c, String name, List<String> out) {
    String v = c.getElement().getAttribute(name);
    if (v != null) {
      out.add(v);
    }
    c.getChildren().forEach(child -> collect(child, name, out));
  }
}
