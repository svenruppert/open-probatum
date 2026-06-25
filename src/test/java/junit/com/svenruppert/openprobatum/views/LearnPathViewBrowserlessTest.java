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

import com.svenruppert.openprobatum.access.EntitlementRepositoryProvider;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.progress.InMemoryProgressRepository;
import com.svenruppert.openprobatum.progress.ProgressRepositoryProvider;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.LearnPathView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("LearnPathView — work a path + progress (P009)")
class LearnPathViewBrowserlessTest extends BrowserlessTest {

  private final AppUser learner = new AppUser(1001L, "Learner", EnumSet.of(AuthorizationRole.LEARNER));
  private InMemoryCatalogRepository catalog;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    CatalogRepositoryProvider.setRepository(catalog);
    EntitlementRepositoryProvider.setRepository(new InMemoryEntitlementRepository());
    ProgressRepositoryProvider.setRepository(new InMemoryProgressRepository());
    SubjectStores.subjectStore().setCurrentSubject(learner, AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CatalogRepositoryProvider.reset();
    EntitlementRepositoryProvider.reset();
    ProgressRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private Offering twoMandatory() {
    Offering o = Offering.publicPath("Course", "d",
        new LearningPath("P", List.of(
            Module.mandatory("Core 1", "c"),
            Module.mandatory("Core 2", "c"))));
    catalog.save(o);
    return o;
  }

  @Test
  @DisplayName("marking modules advances the progress bar to 100%")
  void markingAdvancesProgress() {
    Offering o = twoMandatory();

    LearnPathView view = new LearnPathView();
    view.setParameter(null, o.id().toString());
    assertEquals("0", percent(view));

    clickFirstOpenModule(view);
    assertEquals("50", percent(view));

    clickFirstOpenModule(view);
    assertEquals("100", percent(view));
  }

  @Test
  @DisplayName("an offering the learner is not entitled to is not workable")
  void deniedOfferingIsNotWorkable() {
    Offering gated = Offering.codePath("Gated", "d",
        new LearningPath("P", List.of(Module.mandatory("M", "c"))), "SECRET");
    catalog.save(gated);

    LearnPathView view = new LearnPathView();
    view.setParameter(null, gated.id().toString());
    assertEquals(List.of("DENIED"), attributes(view, "data-learn-result"));
  }

  @Test
  @DisplayName("an unknown offering id renders the unknown marker")
  void unknownOffering() {
    LearnPathView view = new LearnPathView();
    view.setParameter(null, UUID.randomUUID().toString());
    assertEquals(List.of("UNKNOWN"), attributes(view, "data-learn-result"));
  }

  private static String percent(Component view) {
    return attributes(view, "data-percent").get(0);
  }

  private static void clickFirstOpenModule(LearnPathView view) {
    List<Button> open = new ArrayList<>();
    collectOpenButtons(view, open);
    open.get(0).click();
  }

  private static void collectOpenButtons(Component c, List<Button> out) {
    if (c instanceof Button b && "OPEN".equals(b.getElement().getAttribute("data-module-state"))) {
      out.add(b);
    }
    c.getChildren().forEach(child -> collectOpenButtons(child, out));
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
