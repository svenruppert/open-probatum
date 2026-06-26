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

package com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.openprobatum.workshop.EnrolmentStatus;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolment;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentService;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

/**
 * The instructor's surface (concept §7.x): the queue of held seats awaiting an
 * attendance verdict. Recording attendance mints a workshop certificate for the
 * learner; a no-show mints nothing. Gated to {@code workshop:run}. Minting
 * happens only on the real ENROLLED→ATTENDED edge, so re-recording never
 * double-mints.
 *
 * @since V00.50.00
 */
@Route(value = WorkshopAttendanceView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.REVIEWER, AuthorizationRole.PLATFORM_ADMIN})
public class WorkshopAttendanceView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "attendance";

  public WorkshopAttendanceView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("attendance.heading", "Attendance"),
        tr("attendance.subtitle", "Record attendance for enrolled learners.")));

    List<WorkshopEnrolment> held = WorkshopEnrolmentRepositoryProvider.repository().all().stream()
        .filter(e -> e.status() == EnrolmentStatus.ENROLLED)
        .toList();
    if (held.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.CLIPBOARD_CHECK,
          tr("attendance.empty.title", "Nothing to record"),
          tr("attendance.empty.body", "Held workshop seats appear here.")));
      return;
    }
    held.forEach(e -> root.add(row(e)));
  }

  private Div row(WorkshopEnrolment enrolment) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-enrolment", enrolment.id().toString());

    String workshopTitle = WorkshopRepositoryProvider.repository().findById(enrolment.workshopId())
        .map(w -> w.title() + " (v" + w.version() + ")")
        .orElse(enrolment.workshopId().toString());
    card.add(new H4(workshopTitle + " — " + enrolment.learnerName()));

    Button attend = new Button(tr("attendance.action.attend", "Attended"),
        e -> attendAndIssue(enrolment));
    attend.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
    attend.getElement().setAttribute("data-action", "attend");

    Button noShow = new Button(tr("attendance.action.noshow", "No-show"), e -> {
      new WorkshopEnrolmentService().markNoShow(enrolment.id(), currentInstructorId());
      render();
    });
    noShow.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
    noShow.getElement().setAttribute("data-action", "noshow");

    card.add(new Div(attend, noShow));
    return card;
  }

  /**
   * Records attendance and, on the real ENROLLED→ATTENDED edge only, mints the
   * workshop certificate. Re-recording yields empty, so it never double-mints.
   */
  private void attendAndIssue(WorkshopEnrolment enrolment) {
    new WorkshopEnrolmentService().recordAttendance(enrolment.id(), currentInstructorId())
        .ifPresent(this::issueCredentialFor);
    render();
  }

  private void issueCredentialFor(WorkshopEnrolment attended) {
    Workshop workshop = WorkshopRepositoryProvider.repository()
        .findById(attended.workshopId()).orElse(null);
    int version = workshop == null ? 1 : workshop.version();
    String title = workshop == null
        ? tr("attendance.credential.default", "Workshop") : workshop.title();
    new IssuanceService(CredentialRepositoryProvider.repository(), IssuerIdentity.fromConfig())
        .issueForWorkshop(attended.workshopId(), version, attended.recipientId(),
            attended.learnerName(), title, CredentialType.WORKSHOP_CERTIFICATE, null);
  }

  private static Long currentInstructorId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
