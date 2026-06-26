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

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.views.WorkshopAuthorView;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WorkshopAuthorView — authoring + submit-for-review (P007)")
class WorkshopAuthorViewBrowserlessTest extends BrowserlessTest {

  private InMemoryWorkshopRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryWorkshopRepository();
    WorkshopRepositoryProvider.setRepository(repo);
  }

  @AfterEach
  void tearDown() {
    WorkshopRepositoryProvider.reset();
  }

  private void fillValid(WorkshopAuthorView view) throws Exception {
    text(view, "title", "Vaadin Day");
    text(view, "instructor", "Sven");
    text(view, "objective", "Master Vaadin");
    dateTime(view, "startsAt", LocalDateTime.of(2026, 9, 1, 9, 0));
    dateTime(view, "endsAt", LocalDateTime.of(2026, 9, 1, 17, 0));
    integer(view, "capacity", 12);
  }

  @Test
  @DisplayName("creating a workshop stores a DRAFT with its schedule + capacity")
  void createsDraft() throws Exception {
    WorkshopAuthorView view = new WorkshopAuthorView();
    fillValid(view);
    create(view);

    assertEquals(1, repo.all().size());
    Workshop w = repo.all().iterator().next();
    assertEquals("Vaadin Day", w.title());
    assertEquals(ContentStatus.DRAFT, w.status());
    assertEquals(12, w.capacity());
    assertEquals("Sven", w.instructor());
    assertEquals(List.of("DRAFT"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("an end before the start saves nothing")
  void invalidScheduleRejected() throws Exception {
    WorkshopAuthorView view = new WorkshopAuthorView();
    fillValid(view);
    dateTime(view, "endsAt", LocalDateTime.of(2026, 9, 1, 8, 0)); // before start
    create(view);
    assertTrue(repo.all().isEmpty());
  }

  @Test
  @DisplayName("submit-for-review moves the workshop to IN_REVIEW")
  void submitForReview() throws Exception {
    WorkshopAuthorView view = new WorkshopAuthorView();
    fillValid(view);
    create(view);

    click(view, "submit");
    assertEquals(ContentStatus.IN_REVIEW, repo.all().iterator().next().status());
  }

  // ── reflection helpers ──────────────────────────────────────────

  private static void text(WorkshopAuthorView v, String field, String value) throws Exception {
    Field f = WorkshopAuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  private static void dateTime(WorkshopAuthorView v, String field, LocalDateTime value)
      throws Exception {
    Field f = WorkshopAuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((DateTimePicker) f.get(v)).setValue(value);
  }

  private static void integer(WorkshopAuthorView v, String field, int value) throws Exception {
    Field f = WorkshopAuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((IntegerField) f.get(v)).setValue(value);
  }

  private static void create(WorkshopAuthorView v) throws Exception {
    Method m = WorkshopAuthorView.class.getDeclaredMethod("create");
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
