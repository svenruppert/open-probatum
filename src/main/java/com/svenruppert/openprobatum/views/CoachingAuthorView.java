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

import com.svenruppert.openprobatum.coaching.CoachingOffer;
import com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider;
import com.svenruppert.openprobatum.coaching.CoachingOfferService;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The coaching-offer authoring surface (concept §7.x / §16.3): a coach describes a
 * 1:1 coaching topic (title, description, objective, session duration) and submits
 * it for review. The author is recorded as the coach. Approval + publication happen
 * in the reviewer's surface; bookable slots are opened in the coach's slot surface.
 *
 * @since V00.60.00
 */
@Route(value = CoachingAuthorView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.AUTHOR)
public class CoachingAuthorView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "coaching-author";

  private final TextField title = new TextField();
  private final TextArea description = new TextArea();
  private final TextField objective = new TextField();
  private final IntegerField duration = new IntegerField();
  private final TextField tags = new TextField();
  private final Span status = new Span();

  public CoachingAuthorView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("coachingauthor.heading", "Coaching authoring"),
        tr("coachingauthor.subtitle", "Describe a 1:1 coaching topic you offer.")));
    root.add(buildForm());
    root.add(buildList());
  }

  private Div buildForm() {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    title.setLabel(tr("coachingauthor.field.title", "Title"));
    title.setWidthFull();
    description.setLabel(tr("coachingauthor.field.description", "Description"));
    description.setWidthFull();
    objective.setLabel(tr("coachingauthor.field.objective", "Learning objective"));
    objective.setWidthFull();
    duration.setLabel(tr("coachingauthor.field.duration", "Session duration (minutes)"));
    duration.setValue(60);
    tags.setLabel(tr("coachingauthor.field.tags", "Tags (comma-separated)"));
    tags.setWidthFull();
    status.setVisible(false);

    Button create = new Button(tr("coachingauthor.action.create", "Create offer"), e -> create());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    form.add(title, description, objective, duration, tags, create, status);
    return form;
  }

  void create() {
    String t = value(title);
    Integer mins = duration.getValue();
    Long coachId = currentId();
    if (t.isBlank() || mins == null || mins < 1 || coachId == null) {
      showStatus("INVALID", tr("coachingauthor.error.invalid",
          "A title and a session duration of at least one minute are required."));
      return;
    }
    CoachingOffer offer = CoachingOffer.draft(t, areaValue(description),
            currentName(), coachId, mins)
        .withObjective(value(objective))
        .withTags(java.util.Set.copyOf(splitCsv(value(tags))));
    new CoachingOfferService().create(offer);
    com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
        .recordAuthor(offer.lineageId(), coachId);
    showStatus("CREATED", tr("coachingauthor.success", "Coaching offer created as a draft."));
    clear();
    render();
  }

  private Div buildList() {
    Div list = new Div();
    var all = CoachingOfferRepositoryProvider.repository().all().stream()
        .sorted(Comparator.comparing((CoachingOffer o) -> o.title()).thenComparingInt(CoachingOffer::version))
        .toList();
    if (all.isEmpty()) {
      list.add(new EmptyState(VaadinIcon.COMMENTS,
          tr("coachingauthor.empty.title", "No coaching offers yet"),
          tr("coachingauthor.empty.body", "Describe your first coaching topic above.")));
      return list;
    }
    all.forEach(o -> list.add(row(o)));
    return list;
  }

  private Div row(CoachingOffer offer) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-offer", offer.id().toString());

    H4 heading = new H4(offer.title() + "  (v" + offer.version() + ", "
        + offer.durationMinutes() + " min)");
    Span statusBadge = new Span(offer.status().name());
    statusBadge.getElement().setAttribute("data-status", offer.status().name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (offer.status() == ContentStatus.PUBLISHED ? "success" : "contrast"));

    card.add(heading, statusBadge);
    if (offer.status() == ContentStatus.DRAFT) {
      Button submit = new Button(tr("coachingauthor.action.submit", "Submit for review"), e -> {
        new CoachingOfferService().submitForReview(offer.id());
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
    duration.setValue(60);
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

  private static String currentName() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("coach");
  }

  private static Long currentId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
