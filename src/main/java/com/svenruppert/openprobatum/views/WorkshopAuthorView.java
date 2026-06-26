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

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopService;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The workshop-authoring surface (concept §7.x / §16.3): an author schedules a
 * workshop (title, description, objective, start/end, capacity, instructor) and
 * submits it for review. Approval + publication happen in the reviewer's surface.
 *
 * @since V00.50.00
 */
@Route(value = WorkshopAuthorView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.AUTHOR)
public class WorkshopAuthorView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "workshops-author";

  private final TextField title = new TextField();
  private final TextArea description = new TextArea();
  private final TextField objective = new TextField();
  private final DateTimePicker startsAt = new DateTimePicker();
  private final DateTimePicker endsAt = new DateTimePicker();
  private final IntegerField capacity = new IntegerField();
  private final TextField instructor = new TextField();
  private final TextField tags = new TextField();
  private final Span status = new Span();

  public WorkshopAuthorView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("workshopsauthor.heading", "Workshop authoring"),
        tr("workshopsauthor.subtitle", "Schedule a capacity-limited session.")));
    root.add(buildForm());
    root.add(buildList());
  }

  private Div buildForm() {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    title.setLabel(tr("workshopsauthor.field.title", "Title"));
    title.setWidthFull();
    description.setLabel(tr("workshopsauthor.field.description", "Description"));
    description.setWidthFull();
    objective.setLabel(tr("workshopsauthor.field.objective", "Learning objective"));
    objective.setWidthFull();
    startsAt.setLabel(tr("workshopsauthor.field.starts", "Starts at"));
    endsAt.setLabel(tr("workshopsauthor.field.ends", "Ends at"));
    capacity.setLabel(tr("workshopsauthor.field.capacity", "Capacity"));
    capacity.setValue(12);
    instructor.setLabel(tr("workshopsauthor.field.instructor", "Instructor"));
    instructor.setWidthFull();
    tags.setLabel(tr("workshopsauthor.field.tags", "Tags (comma-separated)"));
    tags.setWidthFull();
    status.setVisible(false);

    Button create = new Button(tr("workshopsauthor.action.create", "Create workshop"), e -> create());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    form.add(title, description, objective, startsAt, endsAt, capacity, instructor, tags, create, status);
    return form;
  }

  void create() {
    String t = value(title);
    LocalDateTime start = startsAt.getValue();
    LocalDateTime end = endsAt.getValue();
    Integer seats = capacity.getValue();
    String runner = value(instructor);
    if (t.isBlank() || start == null || end == null || !end.isAfter(start)
        || seats == null || seats < 1 || runner.isBlank()) {
      showStatus("INVALID", tr("workshopsauthor.error.invalid",
          "A title, instructor, a valid start/end and a capacity of at least one are required."));
      return;
    }
    Workshop workshop = Workshop.draft(t, areaValue(description),
            start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC), seats, runner)
        .withObjective(value(objective))
        .withTags(java.util.Set.copyOf(splitCsv(value(tags))));
    new WorkshopService().create(workshop);
    com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
        .recordAuthor(workshop.lineageId(), currentAuthorId());
    showStatus("CREATED", tr("workshopsauthor.success", "Workshop created as a draft."));
    clear();
    render();
  }

  private Div buildList() {
    Div list = new Div();
    var all = WorkshopRepositoryProvider.repository().all().stream()
        .sorted(Comparator.comparing((Workshop w) -> w.title()).thenComparingInt(Workshop::version))
        .toList();
    if (all.isEmpty()) {
      list.add(new EmptyState(VaadinIcon.CALENDAR_CLOCK,
          tr("workshopsauthor.empty.title", "No workshops yet"),
          tr("workshopsauthor.empty.body", "Schedule your first workshop above.")));
      return list;
    }
    all.forEach(w -> list.add(row(w)));
    return list;
  }

  private Div row(Workshop workshop) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-workshop", workshop.id().toString());

    H4 heading = new H4(workshop.title() + "  (v" + workshop.version() + ", "
        + workshop.capacity() + " seats)");
    Span statusBadge = new Span(workshop.status().name());
    statusBadge.getElement().setAttribute("data-status", workshop.status().name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (workshop.status() == ContentStatus.PUBLISHED ? "success" : "contrast"));

    card.add(heading, statusBadge);
    if (workshop.status() == ContentStatus.DRAFT) {
      Button submit = new Button(tr("workshopsauthor.action.submit", "Submit for review"), e -> {
        new WorkshopService().submitForReview(workshop.id());
        render();
      });
      submit.addThemeVariants(ButtonVariant.LUMO_SMALL);
      submit.getElement().setAttribute("data-action", "submit");
      card.add(submit);
    }
    return card;
  }

  private void clear() {
    title.clear();
    description.clear();
    objective.clear();
    startsAt.clear();
    endsAt.clear();
    capacity.setValue(12);
    instructor.clear();
    tags.clear();
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    status.getElement().getThemeList().add(
        "badge pill " + ("CREATED".equals(marker) ? "success" : "error"));
    status.setVisible(true);
  }

  private static List<String> splitCsv(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private static String value(TextField field) {
    return field.getValue() == null ? "" : field.getValue().trim();
  }

  private static String areaValue(TextArea field) {
    return field.getValue() == null ? "" : field.getValue().trim();
  }

  private static Long currentAuthorId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
