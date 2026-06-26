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

import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.catalog.OfferingVisibility;
import com.svenruppert.openprobatum.views.AuthorView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthorView — minimal offering authoring (P016)")
class AuthorViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCatalogRepository catalog;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    CatalogRepositoryProvider.setRepository(catalog);
  }

  @AfterEach
  void tearDown() {
    CatalogRepositoryProvider.reset();
  }

  @Test
  @DisplayName("an author creates a public offering with one module")
  void createsPublicOffering() throws Exception {
    AuthorView view = new AuthorView();
    text(view, "title", "Vaadin Basics");
    area(view, "description", "Learn routing");
    text(view, "moduleTitle", "Routing");
    area(view, "moduleContent", "How @Route works");
    create(view);

    assertEquals(1, catalog.all().size());
    Offering o = catalog.all().iterator().next();
    assertEquals("Vaadin Basics", o.title());
    assertEquals(OfferingVisibility.PUBLIC, o.visibility());
    assertEquals("Routing", o.path().modules().get(0).title());
  }

  @Test
  @DisplayName("a code-gated offering carries the access code")
  void createsCodeOffering() throws Exception {
    AuthorView view = new AuthorView();
    text(view, "title", "Members Course");
    text(view, "moduleTitle", "M");
    area(view, "moduleContent", "c");
    combo(view).setValue(OfferingVisibility.CODE);
    text(view, "accessCode", "OPEN-2026");
    create(view);

    Offering o = catalog.all().iterator().next();
    assertEquals(OfferingVisibility.CODE, o.visibility());
    assertEquals("OPEN-2026", o.accessCodeOpt().orElseThrow());
  }

  @Test
  @DisplayName("a missing module is rejected and nothing is saved")
  void rejectsIncomplete() throws Exception {
    AuthorView view = new AuthorView();
    text(view, "title", "Nope");
    create(view);
    assertTrue(catalog.all().isEmpty());
  }

  // ── reflection helpers ──────────────────────────────────────────

  private static void text(AuthorView v, String field, String value) throws Exception {
    Field f = AuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  private static void area(AuthorView v, String field, String value) throws Exception {
    Field f = AuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextArea) f.get(v)).setValue(value);
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<OfferingVisibility> combo(AuthorView v) throws Exception {
    Field f = AuthorView.class.getDeclaredField("visibility");
    f.setAccessible(true);
    return (ComboBox<OfferingVisibility>) f.get(v);
  }

  private static void create(AuthorView v) throws Exception {
    Method m = AuthorView.class.getDeclaredMethod("create");
    m.setAccessible(true);
    m.invoke(v);
  }
}
