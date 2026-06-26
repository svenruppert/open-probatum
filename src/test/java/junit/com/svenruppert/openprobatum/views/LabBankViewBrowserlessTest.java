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
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
import com.svenruppert.openprobatum.views.LabBankView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
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

@DisplayName("LabBankView — authoring + submit-for-review (P003)")
class LabBankViewBrowserlessTest extends BrowserlessTest {

  private InMemoryLabRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryLabRepository();
    LabRepositoryProvider.setRepository(repo);
  }

  @AfterEach
  void tearDown() {
    LabRepositoryProvider.reset();
  }

  @Test
  @DisplayName("creating a lab stores a DRAFT and lists it")
  void createsDraft() throws Exception {
    LabBankView view = new LabBankView();
    text(view, "title", "Deploy a WAR");
    area(view, "instructions", "Deploy to Jetty and capture the boot log.");
    text(view, "objective", "Master deployment");
    text(view, "acceptance", "The WAR boots and serves /");
    text(view, "tags", "ops, jetty");
    create(view);

    assertEquals(1, repo.all().size());
    Lab lab = repo.all().iterator().next();
    assertEquals("Deploy a WAR", lab.title());
    assertEquals(ContentStatus.DRAFT, lab.status());
    assertEquals(java.util.Set.of("ops", "jetty"), lab.tags());
    assertEquals(List.of("DRAFT"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("an incomplete form (no instructions) saves nothing")
  void incompleteRejected() throws Exception {
    LabBankView view = new LabBankView();
    text(view, "title", "No instructions");
    create(view);
    assertTrue(repo.all().isEmpty());
  }

  @Test
  @DisplayName("submit-for-review moves the lab to IN_REVIEW")
  void submitForReview() throws Exception {
    LabBankView view = new LabBankView();
    text(view, "title", "L");
    area(view, "instructions", "do it");
    create(view);

    click(view, "submit");
    assertEquals(ContentStatus.IN_REVIEW, repo.all().iterator().next().status());
  }

  // ── reflection helpers ──────────────────────────────────────────

  private static void text(LabBankView v, String field, String value) throws Exception {
    Field f = LabBankView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  private static void area(LabBankView v, String field, String value) throws Exception {
    Field f = LabBankView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextArea) f.get(v)).setValue(value);
  }

  private static void create(LabBankView v) throws Exception {
    Method m = LabBankView.class.getDeclaredMethod("create");
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
