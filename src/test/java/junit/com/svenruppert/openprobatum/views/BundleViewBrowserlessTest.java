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

import com.svenruppert.openprobatum.access.AccessDecision;
import com.svenruppert.openprobatum.access.EntitlementRepositoryProvider;
import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.BundleRepositoryProvider;
import com.svenruppert.openprobatum.bundle.InMemoryBundleRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.BundleView;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BundleView — learner joins a bundle (P004)")
class BundleViewBrowserlessTest extends BrowserlessTest {

  private InMemoryBundleRepository bundles;
  private InMemoryCatalogRepository catalog;
  private Offering gated;

  @BeforeEach
  void setUp() {
    bundles = new InMemoryBundleRepository();
    catalog = new InMemoryCatalogRepository();
    BundleRepositoryProvider.setRepository(bundles);
    CatalogRepositoryProvider.setRepository(catalog);
    EntitlementRepositoryProvider.setRepository(new InMemoryEntitlementRepository());
    // The claim-eligibility check reads progress + credentials — keep both in memory.
    com.svenruppert.openprobatum.progress.ProgressRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.progress.InMemoryProgressRepository());
    com.svenruppert.openprobatum.credential.CredentialRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.credential.InMemoryCredentialRepository());
    com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository());
    gated = Offering.codePath("Gated", "d",
        new LearningPath("P", List.of(Module.mandatory("M", "c"))), "SECRET");
    catalog.save(gated);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    BundleRepositoryProvider.reset();
    CatalogRepositoryProvider.reset();
    EntitlementRepositoryProvider.reset();
    com.svenruppert.openprobatum.progress.ProgressRepositoryProvider.reset();
    com.svenruppert.openprobatum.credential.CredentialRepositoryProvider.reset();
    com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private Bundle publishedBundle() {
    Bundle b = Bundle.draft("Pack", "d", Set.of(gated.id())).withStatus(ContentStatus.PUBLISHED);
    bundles.save(b);
    return b;
  }

  @Test
  @DisplayName("joining a published bundle entitles the learner to its members")
  void joinEntitlesMembers() {
    publishedBundle();
    EntitlementService entitlements = new EntitlementService();
    AppUser ada = new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER));
    assertEquals(AccessDecision.CODE_REQUIRED, entitlements.canAccess(ada, gated));

    BundleView view = new BundleView();
    click(view, "join");

    assertEquals(AccessDecision.GRANTED, entitlements.canAccess(ada, gated),
        "the gated member is unlocked by joining the bundle");
  }

  @Test
  @DisplayName("a draft bundle is not shown to learners")
  void draftHidden() {
    bundles.save(Bundle.draft("Draft", "d", Set.of(gated.id()))); // DRAFT
    BundleView view = new BundleView();
    assertTrue(attributes(view, "data-bundle").isEmpty());
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
