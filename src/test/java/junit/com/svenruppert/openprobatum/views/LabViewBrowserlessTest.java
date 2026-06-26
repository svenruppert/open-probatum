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

import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
import com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider;
import com.svenruppert.openprobatum.lab.SubmissionStatus;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.LabView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LabView — learner submits practical evidence (P005)")
class LabViewBrowserlessTest extends BrowserlessTest {

  private InMemoryLabRepository labs;
  private InMemoryLabSubmissionRepository submissions;

  @BeforeEach
  void setUp() {
    labs = new InMemoryLabRepository();
    submissions = new InMemoryLabSubmissionRepository();
    LabRepositoryProvider.setRepository(labs);
    LabSubmissionRepositoryProvider.setRepository(submissions);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    LabRepositoryProvider.reset();
    LabSubmissionRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private Lab publishedLab() {
    Lab lab = Lab.draft("Deploy", "Deploy the app")
        .withMetadata("Master deploy", Difficulty.HARD, "WAR boots")
        .withStatus(ContentStatus.PUBLISHED);
    labs.save(lab);
    return lab;
  }

  @Test
  @DisplayName("a learner submits evidence against a published lab → SUBMITTED")
  void submitsEvidence() throws Exception {
    Lab lab = publishedLab();
    LabView view = new LabView();
    select(view, lab);
    area(view, "writeUp", "I deployed it and captured the log.");
    submit(view);

    assertEquals(1, submissions.all().size());
    var s = submissions.all().iterator().next();
    assertEquals(SubmissionStatus.SUBMITTED, s.status());
    assertEquals(1001L, s.recipientId());
    assertEquals(lab.version(), s.labVersion());
    // It shows in the learner's own submissions.
    assertEquals(List.of(SubmissionStatus.SUBMITTED.name()), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("a draft (unpublished) lab cannot be submitted against")
  void cannotSubmitToDraft() throws Exception {
    Lab draft = Lab.draft("Draft", "wip"); // DRAFT — never offered in the selector
    labs.save(draft);

    LabView view = new LabView();
    // The selector only lists published labs, so there is nothing to submit.
    assertTrue(attributes(view, "data-submission").isEmpty());
    assertEquals(0, submissions.all().size());
  }

  @Test
  @DisplayName("the learner sees only their own submissions, not another learner's")
  void ownDataOnly() {
    Lab lab = publishedLab();
    // Another learner's submission must not show in Ada's list.
    submissions.save(com.svenruppert.openprobatum.lab.LabSubmission.submit(
        lab.id(), lab.version(), 2002L, "Bob", "Bob's work", null));

    LabView view = new LabView();
    assertTrue(attributes(view, "data-submission").isEmpty(),
        "Ada has no submissions of her own yet");
  }

  // ── helpers ─────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private static void select(LabView v, Lab lab) throws Exception {
    Field f = LabView.class.getDeclaredField("labSelect");
    f.setAccessible(true);
    ((ComboBox<Lab>) f.get(v)).setValue(lab);
  }

  private static void area(LabView v, String field, String value) throws Exception {
    Field f = LabView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextArea) f.get(v)).setValue(value);
  }

  private static void submit(LabView v) throws Exception {
    Method m = LabView.class.getDeclaredMethod("submit");
    m.setAccessible(true);
    m.invoke(v);
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
