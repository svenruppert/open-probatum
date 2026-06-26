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
import com.svenruppert.openprobatum.bundle.BundleRepositoryProvider;
import com.svenruppert.openprobatum.bundle.InMemoryBundleRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.views.BundleAuthorView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BundleAuthorView — authoring + submit-for-review (P003)")
class BundleAuthorViewBrowserlessTest extends BrowserlessTest {

  private InMemoryBundleRepository bundles;
  private InMemoryCatalogRepository catalog;
  private Offering offering;

  @BeforeEach
  void setUp() {
    bundles = new InMemoryBundleRepository();
    catalog = new InMemoryCatalogRepository();
    BundleRepositoryProvider.setRepository(bundles);
    CatalogRepositoryProvider.setRepository(catalog);
    offering = Offering.publicPath("Course A", "d",
        new LearningPath("P", List.of(Module.mandatory("M", "c"))));
    catalog.save(offering);
  }

  @AfterEach
  void tearDown() {
    BundleRepositoryProvider.reset();
    CatalogRepositoryProvider.reset();
  }

  @Test
  @DisplayName("creating a bundle stores a DRAFT holding the selected members")
  void createsDraft() throws Exception {
    BundleAuthorView view = new BundleAuthorView();
    text(view, "title", "Vaadin Mastery");
    selectMembers(view, Set.of(offering));
    create(view);

    assertEquals(1, bundles.all().size());
    Bundle b = bundles.all().iterator().next();
    assertEquals("Vaadin Mastery", b.title());
    assertEquals(ContentStatus.DRAFT, b.status());
    assertTrue(b.contains(offering.id()));
    assertEquals(List.of("DRAFT"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("a bundle without members saves nothing")
  void noMembersRejected() throws Exception {
    BundleAuthorView view = new BundleAuthorView();
    text(view, "title", "Empty");
    create(view);
    assertTrue(bundles.all().isEmpty());
  }

  @Test
  @DisplayName("submit-for-review moves the bundle to IN_REVIEW")
  void submitForReview() throws Exception {
    BundleAuthorView view = new BundleAuthorView();
    text(view, "title", "B");
    selectMembers(view, Set.of(offering));
    create(view);

    click(view, "submit");
    assertEquals(ContentStatus.IN_REVIEW, bundles.all().iterator().next().status());
  }

  // ── reflection helpers ──────────────────────────────────────────

  private static void text(BundleAuthorView v, String field, String value) throws Exception {
    Field f = BundleAuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  @SuppressWarnings("unchecked")
  private static void selectMembers(BundleAuthorView v, Set<Offering> value) throws Exception {
    Field f = BundleAuthorView.class.getDeclaredField("members");
    f.setAccessible(true);
    ((MultiSelectComboBox<Offering>) f.get(v)).setValue(value);
  }

  private static void create(BundleAuthorView v) throws Exception {
    Method m = BundleAuthorView.class.getDeclaredMethod("create");
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
