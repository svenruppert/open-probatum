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

import com.svenruppert.openprobatum.assessment.Assessment;
import com.svenruppert.openprobatum.assessment.AssessmentRepository;
import com.svenruppert.openprobatum.assessment.AssessmentRepositoryProvider;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionFeedback;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Practice mode (concept §9.5): the learner answers an assessment's questions and
 * gets immediate per-question feedback + explanation. It deliberately issues
 * <strong>no</strong> credential and persists no attempt — it is for learning,
 * not certification.
 *
 * @since V00.20.00
 */
@Route(value = PracticeView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class PracticeView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "practice";

  private final AssessmentRepository assessments = AssessmentRepositoryProvider.repository();

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String id) {
    VerticalLayout root = getContent();
    root.removeAll();

    Optional<Assessment> found = parse(id).flatMap(assessments::findById);
    if (found.isEmpty()) {
      Span unknown = new Span(tr("practice.unknown", "This assessment does not exist."));
      unknown.getElement().setAttribute("data-practice-result", "UNKNOWN");
      root.add(new PageHeader(tr("practice.heading", "Practice")), unknown);
      return;
    }
    render(root, found.get());
  }

  private void render(VerticalLayout root, Assessment assessment) {
    root.add(new PageHeader(assessment.title(), tr("practice.subtitle", "Practice mode")));

    Span banner = new Span(tr("practice.noCredential",
        "Practice mode — immediate feedback, no credential is issued."));
    banner.getElement().setAttribute("data-practice", "MODE");
    banner.getElement().getThemeList().add("badge contrast pill");
    root.add(banner);

    for (Question question : assessment.questions()) {
      root.add(questionBlock(question));
    }
  }

  private Div questionBlock(Question question) {
    Div block = new Div();
    block.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    block.getElement().setAttribute("data-question", question.id().toString());
    block.add(new H4(question.text()));

    CheckboxGroup<Integer> choices = new CheckboxGroup<>();
    choices.setItems(IntStream.range(0, question.options().size()).boxed().toList());
    choices.setItemLabelGenerator(i -> question.options().get(i));

    Span result = new Span();
    result.setVisible(false);

    Button check = new Button(tr("practice.check", "Check"), e -> {
      Set<Integer> chosen = choices.getSelectedItems();
      QuestionFeedback fb = question.feedback(chosen);
      result.getElement().setAttribute("data-feedback", fb.correct() ? "CORRECT" : "INCORRECT");
      result.getElement().getThemeList().clear();
      result.getElement().getThemeList().add("badge pill " + (fb.correct() ? "success" : "error"));
      String label = fb.correct()
          ? tr("practice.correct", "Correct!")
          : tr("practice.incorrect", "Not quite.");
      result.setText(fb.explanation().isBlank() ? label : label + " " + fb.explanation());
      result.setVisible(true);
    });
    check.addThemeVariants(ButtonVariant.LUMO_SMALL);

    block.add(choices, check, result);
    return block;
  }

  private static Optional<UUID> parse(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(id.trim()));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
