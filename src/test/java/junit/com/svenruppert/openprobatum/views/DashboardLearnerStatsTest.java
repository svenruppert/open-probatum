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
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.progress.InMemoryProgressRepository;
import com.svenruppert.openprobatum.progress.ProgressRepositoryProvider;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.DashboardView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DashboardView — learner stats row (P015)")
class DashboardLearnerStatsTest extends BrowserlessTest {

  private final AppUser alice = new AppUser(1001L, "Alice", EnumSet.of(AuthorizationRole.LEARNER));
  private InMemoryCredentialRepository credentials;
  private InMemoryCatalogRepository catalog;
  private InMemoryProgressRepository progress;

  @BeforeEach
  void setUp() {
    credentials = new InMemoryCredentialRepository();
    catalog = new InMemoryCatalogRepository();
    progress = new InMemoryProgressRepository();
    CredentialRepositoryProvider.setRepository(credentials);
    CatalogRepositoryProvider.setRepository(catalog);
    ProgressRepositoryProvider.setRepository(progress);
    SubjectStores.subjectStore().setCurrentSubject(alice, AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CredentialRepositoryProvider.reset();
    CatalogRepositoryProvider.reset();
    ProgressRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  @Test
  @DisplayName("the dashboard counts the learner's credentials + in-progress paths")
  void countsLearnerStats() {
    credentials.save(Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Open Probatum Academy", Instant.parse("2026-01-01T00:00:00Z"), null));
    credentials.save(Credential.issue("Other", CredentialType.COMPLETION_CERTIFICATE,
        "Bob", "Open Probatum Academy", Instant.parse("2026-01-01T00:00:00Z"), null));

    Module core1 = Module.mandatory("Core 1", "c");
    Module core2 = Module.mandatory("Core 2", "c");
    Offering offering = Offering.publicPath("Course", "d",
        new LearningPath("P", List.of(core1, core2)));
    catalog.save(offering);
    // Alice completed one of two mandatory modules → 50% → "in progress".
    new ProgressService(progress).markModuleComplete(alice.id(), offering.id(), core1.id());

    DashboardView view = new DashboardView();

    assertEquals(List.of("1"), attributes(view, "data-stat-credentials"));
    assertEquals(List.of("1"), attributes(view, "data-stat-inprogress"));
  }

  @Test
  @DisplayName("a fresh learner sees zero credentials and zero paths in progress")
  void freshLearnerStats() {
    DashboardView view = new DashboardView();
    assertEquals(List.of("0"), attributes(view, "data-stat-credentials"));
    assertEquals(List.of("0"), attributes(view, "data-stat-inprogress"));
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
