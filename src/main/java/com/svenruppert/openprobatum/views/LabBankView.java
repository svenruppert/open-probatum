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

import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
import com.svenruppert.openprobatum.lab.LabService;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The lab-authoring surface (concept §9.x / §16.3): an author creates a practical
 * lab (title, instructions, learning objective, difficulty, acceptance criteria)
 * and submits it for review. Approval + publication happen in the reviewer's
 * surface. Every version's status is shown.
 *
 * @since V00.40.00
 */
@Route(value = LabBankView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.AUTHOR)
public class LabBankView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "labs-author";

  private final TextField title = new TextField();
  private final TextArea instructions = new TextArea();
  private final TextField objective = new TextField();
  private final ComboBox<Difficulty> difficulty = new ComboBox<>();
  private final TextField acceptance = new TextField();
  private final TextField tags = new TextField();
  private final Span status = new Span();

  public LabBankView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("labsauthor.heading", "Lab authoring"),
        tr("labsauthor.subtitle", "Author reviewable, practical labs.")));
    root.add(buildForm());
    root.add(buildList());
  }

  private Div buildForm() {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    title.setLabel(tr("labsauthor.field.title", "Title"));
    title.setWidthFull();
    instructions.setLabel(tr("labsauthor.field.instructions", "Instructions"));
    instructions.setWidthFull();
    objective.setLabel(tr("labsauthor.field.objective", "Learning objective"));
    objective.setWidthFull();
    difficulty.setLabel(tr("labsauthor.field.difficulty", "Difficulty"));
    difficulty.setItems(Difficulty.values());
    difficulty.setValue(Difficulty.MEDIUM);
    acceptance.setLabel(tr("labsauthor.field.acceptance", "Acceptance criteria"));
    acceptance.setWidthFull();
    tags.setLabel(tr("labsauthor.field.tags", "Tags (comma-separated)"));
    tags.setWidthFull();
    status.setVisible(false);

    Button create = new Button(tr("labsauthor.action.create", "Create lab"), e -> create());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    form.add(title, instructions, objective, difficulty, acceptance, tags, create, status);
    return form;
  }

  void create() {
    String t = value(title);
    String instr = areaValue(instructions);
    if (t.isBlank() || instr.isBlank()) {
      showStatus("INVALID", tr("labsauthor.error.invalid",
          "A title and instructions are required."));
      return;
    }
    Difficulty diff = difficulty.getValue() == null ? Difficulty.MEDIUM : difficulty.getValue();
    Lab lab = Lab.draft(t, instr)
        .withMetadata(value(objective), diff, value(acceptance))
        .withTags(java.util.Set.copyOf(splitCsv(value(tags))));
    new LabService().create(lab);
    // Record authorship (§17.2) for the segregation-of-duties rule on review.
    com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
        .recordAuthor(lab.lineageId(), currentAuthorId());
    showStatus("CREATED", tr("labsauthor.success", "Lab created as a draft."));
    clear();
    render();
  }

  private Div buildList() {
    Div list = new Div();
    var all = LabRepositoryProvider.repository().all().stream()
        .sorted(Comparator.comparing((Lab l) -> l.title()).thenComparingInt(Lab::version))
        .toList();
    if (all.isEmpty()) {
      list.add(new EmptyState(VaadinIcon.FLASK,
          tr("labsauthor.empty.title", "No labs yet"),
          tr("labsauthor.empty.body", "Create your first lab above.")));
      return list;
    }
    all.forEach(l -> list.add(row(l)));
    return list;
  }

  private Div row(Lab lab) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-lab", lab.id().toString());

    H4 heading = new H4(lab.title() + "  (v" + lab.version() + ")");
    Span statusBadge = new Span(lab.status().name());
    statusBadge.getElement().setAttribute("data-status", lab.status().name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (lab.status() == ContentStatus.PUBLISHED ? "success" : "contrast"));

    card.add(heading, statusBadge);
    if (lab.status() == ContentStatus.DRAFT) {
      Button submit = new Button(tr("labsauthor.action.submit", "Submit for review"), e -> {
        new LabService().submitForReview(lab.id());
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
    instructions.clear();
    objective.clear();
    acceptance.clear();
    tags.clear();
    difficulty.setValue(Difficulty.MEDIUM);
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
        .map(AppUser::id)
        .orElse(null);
  }
}
