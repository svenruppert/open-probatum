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

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
import com.svenruppert.openprobatum.lab.LabSubmission;
import com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider;
import com.svenruppert.openprobatum.lab.SubmissionStatus;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.AssessmentQueueView;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AssessmentQueueView — assessor verify/reject (P006)")
class AssessmentQueueViewBrowserlessTest extends BrowserlessTest {

  private InMemoryLabRepository labs;
  private InMemoryLabSubmissionRepository submissions;
  private InMemoryContentAuthorship authorship;
  private Lab lab;

  @BeforeEach
  void setUp() {
    labs = new InMemoryLabRepository();
    submissions = new InMemoryLabSubmissionRepository();
    authorship = new InMemoryContentAuthorship();
    LabRepositoryProvider.setRepository(labs);
    LabSubmissionRepositoryProvider.setRepository(submissions);
    ContentAuthorshipProvider.setRegistry(authorship);

    lab = Lab.draft("Deploy", "Deploy the app").withStatus(ContentStatus.PUBLISHED);
    labs.save(lab);
    authorship.recordAuthor(lab.lineageId(), 1001L); // authored by user 1001

    assessorIs(2002L);
  }

  @AfterEach
  void tearDown() {
    LabRepositoryProvider.reset();
    LabSubmissionRepositoryProvider.reset();
    ContentAuthorshipProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private void assessorIs(Long id) {
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(id, "Assessor", EnumSet.of(AuthorizationRole.REVIEWER)), AppUser.class);
  }

  private LabSubmission pendingSubmission() {
    LabSubmission s = LabSubmission.submit(lab.id(), lab.version(), 5005L, "Ada", "did it", null);
    submissions.save(s);
    return s;
  }

  @Test
  @DisplayName("a pending submission appears and the assessor verifies it")
  void verifies() {
    LabSubmission s = pendingSubmission();
    AssessmentQueueView view = new AssessmentQueueView();
    assertEquals(List.of(s.id().toString()), attributes(view, "data-submission"));

    click(view, "verify");
    assertEquals(SubmissionStatus.VERIFIED, submissions.findById(s.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("the lab's author cannot assess; an inline self-assess notice is shown")
  void selfAssessmentBlocked() {
    LabSubmission s = pendingSubmission();
    assessorIs(1001L); // the author tries to assess their own lab's submission

    AssessmentQueueView view = new AssessmentQueueView();
    click(view, "verify");

    assertEquals(List.of("SELF_ASSESS"), attributes(view, "data-error"));
    assertEquals(SubmissionStatus.SUBMITTED, submissions.findById(s.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("an empty queue shows the empty state")
  void emptyQueue() {
    assertTrue(attributes(new AssessmentQueueView(), "data-submission").isEmpty());
  }

  // ── helpers ─────────────────────────────────────────────────────

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
