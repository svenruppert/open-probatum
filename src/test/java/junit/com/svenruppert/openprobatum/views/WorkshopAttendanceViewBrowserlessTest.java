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
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.WorkshopAttendanceView;
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

@DisplayName("WorkshopAttendanceView — attendance mints a workshop certificate (P009)")
class WorkshopAttendanceViewBrowserlessTest extends BrowserlessTest {

  private InMemoryWorkshopRepository workshops;
  private InMemoryWorkshopEnrolmentRepository enrolments;
  private InMemoryCredentialRepository credentials;
  private Workshop workshop;

  @BeforeEach
  void setUp() {
    workshops = new InMemoryWorkshopRepository();
    enrolments = new InMemoryWorkshopEnrolmentRepository();
    credentials = new InMemoryCredentialRepository();
    WorkshopRepositoryProvider.setRepository(workshops);
    WorkshopEnrolmentRepositoryProvider.setRepository(enrolments);
    CredentialRepositoryProvider.setRepository(credentials);
    CredentialEventRepositoryProvider.setRepository(new InMemoryCredentialEventRepository());
    workshop = Workshop.draft("Vaadin Day", "d",
        Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"), 10, "Sven")
        .withStatus(ContentStatus.PUBLISHED);
    workshops.save(workshop);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(2002L, "Instructor", EnumSet.of(AuthorizationRole.REVIEWER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    WorkshopRepositoryProvider.reset();
    WorkshopEnrolmentRepositoryProvider.reset();
    CredentialRepositoryProvider.reset();
    CredentialEventRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private WorkshopEnrolment enrolled() {
    WorkshopEnrolment e = WorkshopEnrolment.enrol(workshop.id(), 5005L, "Ada");
    enrolments.save(e);
    return e;
  }

  @Test
  @DisplayName("recording attendance mints exactly one workshop certificate (P009)")
  void attendMints() {
    WorkshopEnrolment e = enrolled();
    WorkshopAttendanceView view = new WorkshopAttendanceView();
    assertEquals(List.of(e.id().toString()), attributes(view, "data-enrolment"));

    click(view, "attend");

    assertEquals(EnrolmentStatus.ATTENDED, enrolments.findById(e.id()).orElseThrow().status());
    assertEquals(1, credentials.all().size(), "exactly one certificate");
    Credential c = credentials.all().iterator().next();
    assertEquals(Evidence.Type.WORKSHOP_ATTENDED, c.evidence().type());
    assertEquals(workshop.id(), c.evidence().sourceId());
    assertEquals(5005L, c.recipientId());
  }

  @Test
  @DisplayName("a no-show mints nothing")
  void noShowMintsNothing() {
    enrolled();
    WorkshopAttendanceView view = new WorkshopAttendanceView();
    click(view, "noshow");
    assertTrue(credentials.all().isEmpty());
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
