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
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.LearningResource;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.catalog.OfferingVisibility;
import com.svenruppert.openprobatum.catalog.ResourceType;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.AuthorView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthorView — offering inventory + multi-module editor (P004)")
class AuthorViewBrowserlessTest extends BrowserlessTest {

  private static final Long AUTHOR = 7L;

  private InMemoryCatalogRepository catalog;
  private InMemoryContentAuthorship authorship;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    authorship = new InMemoryContentAuthorship();
    CatalogRepositoryProvider.setRepository(catalog);
    ContentAuthorshipProvider.setRegistry(authorship);
    CredentialRepositoryProvider.setRepository(new InMemoryCredentialRepository());
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(AUTHOR, "Ada", EnumSet.of(AuthorizationRole.AUTHOR)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CatalogRepositoryProvider.reset();
    ContentAuthorshipProvider.reset();
    CredentialRepositoryProvider.reset();
  }

  private static LearningPath path() {
    return new LearningPath("P", List.of(Module.mandatory("M", "c")));
  }

  private void seedOwn(String title, ContentStatus status) {
    Offering o = Offering.publicPath(title, "d", path());
    if (status != ContentStatus.DRAFT) {
      o = o.withStatus(status);
    }
    catalog.save(o);
    authorship.recordAuthor(o.lineageId(), AUTHOR);
  }

  @Test
  @DisplayName("the inventory lists the author's offerings with status + status-specific actions")
  void inventoryRendersStatusAndActions() {
    seedOwn("Draft Course", ContentStatus.DRAFT);
    seedOwn("Live Course", ContentStatus.PUBLISHED);

    AuthorView view = new AuthorView();

    List<String> statuses = attributes(view, "data-offering-status");
    assertTrue(statuses.contains("DRAFT"));
    assertTrue(statuses.contains("PUBLISHED"));
    List<String> actions = attributes(view, "data-action");
    assertTrue(actions.contains("submit"), "a DRAFT offers submit");
    assertTrue(actions.contains("delete"), "a DRAFT offers delete");
    assertTrue(actions.contains("deactivate"), "a PUBLISHED offering offers deactivate");
  }

  @Test
  @DisplayName("another author's offering is not shown")
  void foreignOfferingHidden() {
    Offering foreign = Offering.publicPath("Not mine", "d", path());
    catalog.save(foreign);
    authorship.recordAuthor(foreign.lineageId(), 99L);

    AuthorView view = new AuthorView();
    assertFalse(attributes(view, "data-offering").contains(foreign.id().toString()));
  }

  @Test
  @DisplayName("saving builds a multi-module DRAFT (no auto-submit) owned by the author")
  void savesMultiModuleDraft() throws Exception {
    AuthorView view = new AuthorView();
    setText(view, "title", "Vaadin Basics");
    clearModules(view);
    addModule(view, "Routing", "How @Route works", true);
    addModule(view, "Theming", "Lumo + CSS", false);
    invoke(view, "saveOffering");

    assertEquals(1, catalog.all().size());
    Offering o = catalog.all().iterator().next();
    assertEquals("Vaadin Basics", o.title());
    assertEquals(ContentStatus.DRAFT, o.status(), "created as DRAFT, not submitted");
    assertEquals(List.of("Routing", "Theming"),
        o.path().modules().stream().map(Module::title).toList());
    assertTrue(authorship.isAuthor(o.lineageId(), AUTHOR));
  }

  @Test
  @DisplayName("a code-gated offering carries its access code")
  void savesCodeOffering() throws Exception {
    AuthorView view = new AuthorView();
    setText(view, "title", "Members Course");
    setText(view, "accessCode", "OPEN-2026");
    visibility(view).setValue(OfferingVisibility.CODE);
    clearModules(view);
    addModule(view, "M", "c", true);
    invoke(view, "saveOffering");

    Offering o = catalog.all().iterator().next();
    assertEquals(OfferingVisibility.CODE, o.visibility());
    assertEquals("OPEN-2026", o.accessCodeOpt().orElseThrow());
  }

  @Test
  @DisplayName("a module's learning resources are saved with the module (P005)")
  void savesModuleWithResources() throws Exception {
    AuthorView view = new AuthorView();
    setText(view, "title", "Course");
    clearModules(view);
    addModule(view, "Intro", "start here", true);
    addResourceTo(view, 0, ResourceType.ARTICLE, "Reading", "Some inline text");
    addResourceTo(view, 0, ResourceType.EXTERNAL_LINK, "Docs", "https://vaadin.com");
    invoke(view, "saveOffering");

    Offering o = catalog.all().iterator().next();
    Module m = o.path().modules().get(0);
    assertEquals(2, m.resources().size());
    LearningResource first = m.resources().get(0);
    assertEquals(ResourceType.ARTICLE, first.type());
    assertEquals("Reading", first.title());
    assertEquals(ResourceType.EXTERNAL_LINK, m.resources().get(1).type());
  }

  @Test
  @DisplayName("an invalid resource payload (URL type without a URL) is rejected, nothing saved")
  void rejectsInvalidResource() throws Exception {
    AuthorView view = new AuthorView();
    setText(view, "title", "Course");
    clearModules(view);
    addModule(view, "Intro", "start here", true);
    addResourceTo(view, 0, ResourceType.VIDEO_REFERENCE, "Clip", "not-a-url");
    invoke(view, "saveOffering");

    assertTrue(catalog.all().isEmpty(), "the invalid resource blocks the save");
    assertTrue(attributes(view, "data-result").contains("INVALID"));
  }

  // ── reflection + tree helpers ──────────────────────────────────────

  private static void setText(AuthorView v, String field, String value) throws Exception {
    Field f = AuthorView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  @SuppressWarnings("unchecked")
  private static com.vaadin.flow.component.combobox.ComboBox<OfferingVisibility> visibility(
      AuthorView v) throws Exception {
    Field f = AuthorView.class.getDeclaredField("visibility");
    f.setAccessible(true);
    return (com.vaadin.flow.component.combobox.ComboBox<OfferingVisibility>) f.get(v);
  }

  @SuppressWarnings("unchecked")
  private static List<Object> moduleRows(AuthorView v) throws Exception {
    Field f = AuthorView.class.getDeclaredField("moduleRows");
    f.setAccessible(true);
    return (List<Object>) f.get(v);
  }

  private static void clearModules(AuthorView v) throws Exception {
    moduleRows(v).clear();
  }

  private static void addModule(AuthorView v, String title, String content, boolean mandatory)
      throws Exception {
    Class<?> rowClass = Class.forName("com.svenruppert.openprobatum.views.AuthorView$ModuleRow");
    Constructor<?> ctor = rowClass.getDeclaredConstructor();
    ctor.setAccessible(true);
    Object row = ctor.newInstance();
    setRowField(rowClass, row, "title", title);
    setRowField(rowClass, row, "content", content);
    Field m = rowClass.getDeclaredField("mandatory");
    m.setAccessible(true);
    m.setBoolean(row, mandatory);
    moduleRows(v).add(row);
  }

  private static void setRowField(Class<?> cls, Object row, String name, String value)
      throws Exception {
    Field f = cls.getDeclaredField(name);
    f.setAccessible(true);
    f.set(row, value);
  }

  @SuppressWarnings("unchecked")
  private static void addResourceTo(AuthorView v, int moduleIndex, ResourceType type,
                                    String title, String payload) throws Exception {
    Object moduleRow = moduleRows(v).get(moduleIndex);
    Field resF = moduleRow.getClass().getDeclaredField("resources");
    resF.setAccessible(true);
    List<Object> resources = (List<Object>) resF.get(moduleRow);
    Class<?> rrClass = Class.forName("com.svenruppert.openprobatum.views.AuthorView$ResourceRow");
    Constructor<?> ctor = rrClass.getDeclaredConstructor();
    ctor.setAccessible(true);
    Object rr = ctor.newInstance();
    Field tf = rrClass.getDeclaredField("type");
    tf.setAccessible(true);
    tf.set(rr, type);
    setRowField(rrClass, rr, "title", title);
    setRowField(rrClass, rr, "payload", payload);
    resources.add(rr);
  }

  private static void invoke(AuthorView v, String method) throws Exception {
    Method m = AuthorView.class.getDeclaredMethod(method);
    m.setAccessible(true);
    m.invoke(v);
  }

  private static List<String> attributes(Component root, String name) {
    List<String> out = new ArrayList<>();
    collectAttr(root, name, out);
    return out;
  }

  private static void collectAttr(Component c, String name, List<String> out) {
    String v = c.getElement().getAttribute(name);
    if (v != null) {
      out.add(v);
    }
    c.getChildren().forEach(child -> collectAttr(child, name, out));
  }
}
