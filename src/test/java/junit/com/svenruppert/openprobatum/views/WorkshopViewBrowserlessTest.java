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
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.WorkshopView;
import com.svenruppert.openprobatum.workshop.EnrolmentStatus;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopEnrolmentRepository;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolment;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WorkshopView — learner enrols in a workshop (P008)")
class WorkshopViewBrowserlessTest extends BrowserlessTest {

  private InMemoryWorkshopRepository workshops;
  private InMemoryWorkshopEnrolmentRepository enrolments;
  private Workshop workshop;

  @BeforeEach
  void setUp() {
    workshops = new InMemoryWorkshopRepository();
    enrolments = new InMemoryWorkshopEnrolmentRepository();
    WorkshopRepositoryProvider.setRepository(workshops);
    WorkshopEnrolmentRepositoryProvider.setRepository(enrolments);
    workshop = Workshop.draft("Vaadin Day", "d",
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"), 10, "Sven")
        .withStatus(ContentStatus.PUBLISHED);
    workshops.save(workshop);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    WorkshopRepositoryProvider.reset();
    WorkshopEnrolmentRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  @Test
  @DisplayName("a learner enrols in a published workshop → ENROLLED and it shows in their list")
  void enrols() {
    WorkshopView view = new WorkshopView();
    assertEquals(List.of(workshop.id().toString()), attributes(view, "data-workshop"));

    click(view, "enrol");

    assertEquals(1, enrolments.all().size());
    WorkshopEnrolment e = enrolments.all().iterator().next();
    assertEquals(EnrolmentStatus.ENROLLED, e.status());
    assertEquals(1001L, e.recipientId());
    assertEquals(List.of(EnrolmentStatus.ENROLLED.name()), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("a learner sees only their own enrolments")
  void ownDataOnly() {
    // Another learner already enrolled.
    enrolments.save(WorkshopEnrolment.enrol(workshop.id(), 2002L, "Bob"));
    WorkshopView view = new WorkshopView();
    assertTrue(attributes(view, "data-enrolment").isEmpty(), "Ada has no enrolment of her own yet");
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
