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

import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionBankService;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The question-bank authoring surface (concept §9.3): an author creates a
 * single-choice question (text, options, correct index, explanation, learning
 * objective) and manages its lifecycle — tag + submit for review. Approval +
 * publication happen in the reviewer's surface. Every version's status is shown.
 *
 * @since V00.30.00
 */
@Route(value = QuestionBankView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.AUTHOR)
public class QuestionBankView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "questions";

  private final TextField text = new TextField();
  private final TextField options = new TextField();
  private final IntegerField correct = new IntegerField();
  private final TextField explanation = new TextField();
  private final TextField objective = new TextField();
  private final TextField tags = new TextField();
  private final Span status = new Span();

  public QuestionBankView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("questions.heading", "Question bank"),
        tr("questions.subtitle", "Author reusable, reviewable questions.")));
    root.add(buildForm());
    root.add(buildList());
  }

  private Div buildForm() {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    text.setLabel(tr("questions.field.text", "Question"));
    text.setWidthFull();
    options.setLabel(tr("questions.field.options", "Options (comma-separated)"));
    options.setWidthFull();
    correct.setLabel(tr("questions.field.correct", "Correct option (0-based index)"));
    correct.setValue(0);
    explanation.setLabel(tr("questions.field.explanation", "Explanation"));
    explanation.setWidthFull();
    objective.setLabel(tr("questions.field.objective", "Learning objective"));
    objective.setWidthFull();
    tags.setLabel(tr("questions.field.tags", "Tags (comma-separated)"));
    tags.setWidthFull();
    status.setVisible(false);

    Button create = new Button(tr("questions.action.create", "Create question"), e -> create());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    form.add(text, options, correct, explanation, objective, tags, create, status);
    return form;
  }

  void create() {
    String prompt = value(text);
    List<String> opts = splitCsv(value(options));
    Integer idx = correct.getValue();
    if (prompt.isBlank() || opts.size() < 2 || idx == null || idx < 0 || idx >= opts.size()) {
      showStatus("INVALID", tr("questions.error.invalid",
          "A question, at least two options and a valid correct index are required."));
      return;
    }
    Question q = Question.singleChoice(prompt, opts, idx, value(explanation))
        .withMetadata(value(objective), "", com.svenruppert.openprobatum.assessment.Difficulty.MEDIUM)
        .withTags(java.util.Set.copyOf(splitCsv(value(tags))));
    new QuestionBankService().create(q);
    // Record authorship (§17.2) so the review surface can enforce segregation of
    // duties — a reviewer may not approve content they authored.
    com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
        .recordAuthor(q.lineageId(), currentAuthorId());
    showStatus("CREATED", tr("questions.success", "Question created as a draft."));
    clear();
    render();
  }

  private Div buildList() {
    Div list = new Div();
    var all = QuestionRepositoryProvider.repository().all().stream()
        .sorted(Comparator.comparing((Question q) -> q.text()).thenComparingInt(Question::version))
        .toList();
    if (all.isEmpty()) {
      list.add(new EmptyState(VaadinIcon.QUESTION,
          tr("questions.empty.title", "No questions yet"),
          tr("questions.empty.body", "Create your first question above.")));
      return list;
    }
    all.forEach(q -> list.add(row(q)));
    return list;
  }

  private Div row(Question q) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-question", q.id().toString());

    H4 heading = new H4(q.text() + "  (v" + q.version() + ")");
    Span statusBadge = new Span(q.status().name());
    statusBadge.getElement().setAttribute("data-status", q.status().name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (q.status() == ContentStatus.PUBLISHED ? "success" : "contrast"));

    card.add(heading, statusBadge);
    if (!q.tags().isEmpty()) {
      Span tagSpan = new Span(String.join(", ", q.tags()));
      tagSpan.getElement().setAttribute("data-tags", String.join(",", q.tags()));
      card.add(tagSpan);
    }
    if (q.status() == ContentStatus.DRAFT) {
      Button submit = new Button(tr("questions.action.submit", "Submit for review"), e -> {
        new QuestionBankService().submitForReview(q.id());
        render();
      });
      submit.addThemeVariants(ButtonVariant.LUMO_SMALL);
      submit.getElement().setAttribute("data-action", "submit");
      card.add(submit);
    }
    return card;
  }

  private void clear() {
    text.clear();
    options.clear();
    correct.setValue(0);
    explanation.clear();
    objective.clear();
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

  private static Long currentAuthorId() {
    return com.svenruppert.jsentinel.authorization.api.SubjectStores.subjectStore()
        .currentSubject(com.svenruppert.openprobatum.security.model.AppUser.class)
        .map(com.svenruppert.openprobatum.security.model.AppUser::id)
        .orElse(null);
  }
}
