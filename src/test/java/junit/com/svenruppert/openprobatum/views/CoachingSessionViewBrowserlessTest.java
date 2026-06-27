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
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.CoachingSessionView;
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

@DisplayName("CoachingSessionView — completing a session mints a coaching credential (P006)")
class CoachingSessionViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCoachingOfferRepository offers;
  private InMemoryCoachingSlotRepository slots;
  private InMemoryCredentialRepository credentials;
  private CoachingOffer offer;

  @BeforeEach
  void setUp() {
    offers = new InMemoryCoachingOfferRepository();
    slots = new InMemoryCoachingSlotRepository();
    credentials = new InMemoryCredentialRepository();
    CoachingOfferRepositoryProvider.setRepository(offers);
    CoachingSlotRepositoryProvider.setRepository(slots);
    CredentialRepositoryProvider.setRepository(credentials);
    CredentialEventRepositoryProvider.setRepository(new InMemoryCredentialEventRepository());
    offer = CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60)
        .withStatus(ContentStatus.PUBLISHED);
    offers.save(offer);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(7L, "Sven", EnumSet.of(AuthorizationRole.REVIEWER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CoachingOfferRepositoryProvider.reset();
    CoachingSlotRepositoryProvider.reset();
    CredentialRepositoryProvider.reset();
    CredentialEventRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private CoachingSlot bookedSlot() {
    CoachingSlot s = CoachingSlot.open(offer.id(), 7L,
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T10:00:00Z"))
        .booked(5005L, "Ada");
    slots.save(s);
    return s;
  }

  @Test
  @DisplayName("completing a booked session mints exactly one coaching credential (P006)")
  void completeMints() {
    CoachingSlot s = bookedSlot();
    CoachingSessionView view = new CoachingSessionView();
    assertEquals(List.of(s.id().toString()), attributes(view, "data-slot"));

    click(view, "complete");

    assertEquals(BookingStatus.COMPLETED, slots.findById(s.id()).orElseThrow().status());
    assertEquals(1, credentials.all().size(), "exactly one credential");
    Credential c = credentials.all().iterator().next();
    assertEquals(Evidence.Type.COACHING_COMPLETED, c.evidence().type());
    assertEquals(offer.id(), c.evidence().sourceId());
    assertEquals(5005L, c.recipientId());
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
