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

package junit.com.svenruppert.openprobatum.views.analytics;

import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.bundle.BundleRepositoryProvider;
import com.svenruppert.openprobatum.bundle.InMemoryBundleRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider;
import com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingSlotRepository;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository;
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
import com.svenruppert.openprobatum.lab.LabSubmission;
import com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.model.InMemoryUserDirectoryPersistence;
import com.svenruppert.openprobatum.security.model.PersistentUserDirectory;
import com.svenruppert.openprobatum.security.model.UserDirectoryProvider;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.analytics.OperatorDashboardView;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopEnrolmentRepository;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;
import com.svenruppert.jsentinel.credential.password.bouncycastle.BouncyCastleHashingServices;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OperatorDashboardView — academy-wide operator surface (P003)")
class OperatorDashboardViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCredentialRepository credentials;
  private InMemoryQuestionRepository questions;
  private InMemoryLabSubmissionRepository labSubmissions;

  @BeforeEach
  void setUp() {
    credentials = new InMemoryCredentialRepository();
    questions = new InMemoryQuestionRepository();
    labSubmissions = new InMemoryLabSubmissionRepository();
    CredentialRepositoryProvider.setRepository(credentials);
    QuestionRepositoryProvider.setRepository(questions);
    LabSubmissionRepositoryProvider.setRepository(labSubmissions);
    CatalogRepositoryProvider.setRepository(new InMemoryCatalogRepository());
    LabRepositoryProvider.setRepository(new InMemoryLabRepository());
    BundleRepositoryProvider.setRepository(new InMemoryBundleRepository());
    WorkshopRepositoryProvider.setRepository(new InMemoryWorkshopRepository());
    WorkshopEnrolmentRepositoryProvider.setRepository(new InMemoryWorkshopEnrolmentRepository());
    CoachingOfferRepositoryProvider.setRepository(new InMemoryCoachingOfferRepository());
    CoachingSlotRepositoryProvider.setRepository(new InMemoryCoachingSlotRepository());
    UserDirectoryProvider.setDirectory(new PersistentUserDirectory(
        new InMemoryUserDirectoryPersistence(), BouncyCastleHashingServices.modern()));
  }

  @AfterEach
  void tearDown() {
    CredentialRepositoryProvider.reset();
    QuestionRepositoryProvider.reset();
    LabSubmissionRepositoryProvider.reset();
    CatalogRepositoryProvider.reset();
    LabRepositoryProvider.reset();
    BundleRepositoryProvider.reset();
    WorkshopRepositoryProvider.reset();
    WorkshopEnrolmentRepositoryProvider.reset();
    CoachingOfferRepositoryProvider.reset();
    CoachingSlotRepositoryProvider.reset();
    UserDirectoryProvider.reset();
  }

  @Test
  @DisplayName("renders seeded credential, pipeline and engagement figures")
  void rendersSeededFigures() {
    credentials.save(Credential.issue("C", CredentialType.COMPLETION_CERTIFICATE, 1L, "Ada",
        "Academy", Instant.parse("2026-01-01T00:00:00Z"), null,
        Evidence.assessmentPassed(UUID.randomUUID(), 1)));
    questions.save(Question.singleChoice("q1", List.of("a", "b"), 0, "e"));
    questions.save(Question.singleChoice("q2", List.of("a", "b"), 0, "e")
        .withStatus(ContentStatus.PUBLISHED));
    UserDirectoryProvider.directory().registerWithHashedPassword("ada", "h",
        new AppUser(1L, "Ada", EnumSet.of(AuthorizationRole.LEARNER)));
    labSubmissions.save(LabSubmission.submit(UUID.randomUUID(), 1, 1L, "Ada", "did it", null));

    OperatorDashboardView view = new OperatorDashboardView();

    assertEquals(List.of("1"), attributes(view, "data-cred-total"));
    List<String> pipelines = attributes(view, "data-pipeline");
    assertEquals(6, pipelines.size(), "one card per content type");
    assertTrue(pipelines.contains("Questions"));
    assertEquals("2", attributes(view, "data-pipeline-total").get(pipelines.indexOf("Questions")),
        "two question versions");
    assertEquals(List.of("1"), attributes(view, "data-engagement-users"));
    assertEquals(List.of("1"), attributes(view, "data-engagement-submissions"));
  }

  @Test
  @DisplayName("an empty academy renders zeros")
  void emptyAcademyRendersZeros() {
    OperatorDashboardView view = new OperatorDashboardView();

    assertEquals(List.of("0"), attributes(view, "data-cred-total"));
    assertEquals(List.of("0"), attributes(view, "data-engagement-users"));
    assertEquals(List.of("0"), attributes(view, "data-engagement-bookings"));
    assertTrue(attributes(view, "data-cred-status").isEmpty(), "no credentials → no status chips");
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
