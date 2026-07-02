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

import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolment;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentService;
import com.svenruppert.openprobatum.workshop.WorkshopService;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * The learner's workshop surface (concept §7.x): browse published {@link Workshop}s
 * and enrol while seats remain, and track your own enrolments (own-data, §3.6),
 * with the option to cancel a seat before the session.
 *
 * @since V00.50.00
 */
@Route(value = WorkshopView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class WorkshopView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "workshops";

  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

  public WorkshopView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("workshops.heading", "Workshops"),
        tr("workshops.subtitle", "Enrol in a session and earn a certificate by attending.")));

    List<Workshop> published = new WorkshopService().published().stream()
        .sorted(Comparator.comparing(Workshop::startsAt))
        .toList();
    if (published.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.CALENDAR_CLOCK,
          tr("workshops.empty.title", "No workshops yet"),
          tr("workshops.empty.body", "Published workshops will appear here.")));
    } else {
      published.forEach(w -> root.add(card(w)));
    }
    root.add(buildMine());
  }

  private Div card(Workshop workshop) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("max-width", "420px").set("padding", "var(--lumo-space-m)")
        .set("margin-bottom", "var(--lumo-space-m)");
    card.getElement().setAttribute("data-workshop", workshop.id().toString());

    long taken = WorkshopEnrolmentRepositoryProvider.repository().activeCount(workshop.id());
    long free = workshop.capacity() - taken;
    card.add(new H4(workshop.title()), new Paragraph(workshop.description()));
    Span when = new Span(WHEN.format(workshop.startsAt()) + " · "
        + tr("workshops.seats", "{0} of {1} seats free", free, workshop.capacity()));
    when.getElement().setAttribute("data-seats-free", String.valueOf(free));
    card.add(when);

    Button enrol = new Button(tr("workshops.action.enrol", "Enrol"), e -> {
      Long me = currentLearnerId();
      if (me != null) {
        boolean enrolled = new WorkshopEnrolmentService()
            .enrol(workshop.id(), me, currentLearnerName()).isPresent();
        Notification.show(enrolled
            ? tr("workshops.enrol.success", "Enrolled — see you there.")
            : tr("workshops.enrol.refused",
                "Could not enrol — the workshop is full, has started, or you are "
                    + "already enrolled."));
      }
      render();
    });
    enrol.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    enrol.getElement().setAttribute("data-action", "enrol");
    enrol.setEnabled(free > 0);
    card.add(enrol);
    return card;
  }

  private Div buildMine() {
    Div box = new Div();
    box.add(new H3(tr("workshops.mine.heading", "My enrolments")));
    List<WorkshopEnrolment> mine = WorkshopEnrolmentRepositoryProvider.repository()
        .forLearner(currentLearnerId());
    if (mine.isEmpty()) {
      box.add(new Paragraph(tr("workshops.mine.empty", "You have not enrolled in any workshop yet.")));
      return box;
    }
    mine.forEach(e -> box.add(enrolmentRow(e)));
    return box;
  }

  private Div enrolmentRow(WorkshopEnrolment enrolment) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-enrolment", enrolment.id().toString());

    Span statusBadge = new Span(enrolment.status().name());
    statusBadge.getElement().setAttribute("data-status", enrolment.status().name());
    statusBadge.getElement().getThemeList().add("badge pill contrast");
    card.add(statusBadge);

    if (enrolment.isActive()) {
      Button cancel = new Button(tr("workshops.action.cancel", "Cancel"), e -> {
        new WorkshopEnrolmentService().cancel(enrolment.id(), currentLearnerId());
        render();
      });
      cancel.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      cancel.getElement().setAttribute("data-action", "cancel");
      card.add(cancel);
    }
    return card;
  }

  private static Long currentLearnerId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }

  private static String currentLearnerName() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("anonymous");
  }
}
