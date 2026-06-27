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

import com.svenruppert.openprobatum.coaching.CoachingOffer;
import com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.views.CoachingAuthorView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CoachingAuthorView — authoring + submit-for-review (P003)")
class CoachingAuthorViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCoachingOfferRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryCoachingOfferRepository();
    CoachingOfferRepositoryProvider.setRepository(repo);
    // The author is the coach — create() now requires an identified author.
    com.svenruppert.jsentinel.authorization.api.SubjectStores.subjectStore().setCurrentSubject(
        new com.svenruppert.openprobatum.security.model.AppUser(7L, "Sven",
            java.util.EnumSet.of(com.svenruppert.openprobatum.security.roles.AuthorizationRole.COACH)),
        com.svenruppert.openprobatum.security.model.AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CoachingOfferRepositoryProvider.reset();
    com.svenruppert.jsentinel.authorization.api.SubjectStores.subjectStore()
        .deleteCurrentSubject(com.svenruppert.openprobatum.security.model.AppUser.class);
  }

  @Test
  @DisplayName("creating an offer stores a DRAFT with its duration")
  void createsDraft() throws Exception {
    CoachingAuthorView view = new CoachingAuthorView();
    text(view, "title", "Career mentoring");
    text(view, "objective", "Grow as a lead");
    integer(view, "duration", 45);
    create(view);

    assertEquals(1, repo.all().size());
    CoachingOffer o = repo.all().iterator().next();
    assertEquals("Career mentoring", o.title());
    assertEquals(ContentStatus.DRAFT, o.status());
    assertEquals(45, o.durationMinutes());
    assertEquals(List.of("DRAFT"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("an offer without a title saves nothing")
  void incompleteRejected() throws Exception {
    CoachingAuthorView view = new CoachingAuthorView();
    integer(view, "duration", 45);
    create(view);
    assertTrue(repo.all().isEmpty());
  }

  @Test
  @DisplayName("submit-for-review moves the offer to IN_REVIEW")
  void submitForReview() throws Exception {
    CoachingAuthorView view = new CoachingAuthorView();
    text(view, "title", "O");
    integer(view, "duration", 30);
    create(view);

    click(view, "submit");
    assertEquals(ContentStatus.IN_REVIEW, repo.all().iterator().next().status());
  }

  // ── reflection helpers ──────────────────────────────────────────

  private static void text(CoachingAuthorView v, String field, String value) throws Exception {
    Field f = CoachingAuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  private static void integer(CoachingAuthorView v, String field, int value) throws Exception {
    Field f = CoachingAuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((IntegerField) f.get(v)).setValue(value);
  }

  private static void create(CoachingAuthorView v) throws Exception {
    Method m = CoachingAuthorView.class.getDeclaredMethod("create");
    m.setAccessible(true);
    m.invoke(v);
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
