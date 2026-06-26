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
import com.svenruppert.openprobatum.assessment.Attempt;
import com.svenruppert.openprobatum.assessment.CheckService;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The graded completion check (concept §9.6). The learner answers the whole
 * assessment, submits once, and sees pass/fail + score + attempt count; the
 * attempt is recorded (and counted). Issuance-on-pass is layered on by the
 * issuance flow.
 *
 * @since V00.20.00
 */
@Route(value = CheckView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class CheckView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "check";

  private final AssessmentRepository assessments = AssessmentRepositoryProvider.repository();
  private final CheckService checkService = new CheckService();
  private final Map<UUID, CheckboxGroup<Integer>> answers = new LinkedHashMap<>();

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String id) {
    VerticalLayout root = getContent();
    root.removeAll();
    answers.clear();

    Optional<Assessment> found = parse(id).flatMap(assessments::findById);
    if (found.isEmpty()) {
      Span unknown = new Span(tr("check.unknown", "This assessment does not exist."));
      unknown.getElement().setAttribute("data-check-result", "UNKNOWN");
      root.add(new PageHeader(tr("check.heading", "Completion check")), unknown);
      return;
    }
    render(root, found.get());
  }

  private void render(VerticalLayout root, Assessment assessment) {
    root.add(new PageHeader(assessment.title(),
        tr("check.subtitle", "Pass to earn your credential.")));

    for (Question question : assessment.questions()) {
      root.add(questionBlock(question));
    }

    Span result = new Span();
    result.setVisible(false);

    Button submit = new Button(tr("check.submit", "Submit"), e -> grade(assessment, result));
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

    root.add(submit, result);
  }

  private Div questionBlock(Question question) {
    Div block = new Div();
    block.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    block.add(new H4(question.text()));

    CheckboxGroup<Integer> choices = new CheckboxGroup<>();
    choices.setItems(IntStream.range(0, question.options().size()).boxed().toList());
    choices.setItemLabelGenerator(i -> question.options().get(i));
    answers.put(question.id(), choices);

    block.add(choices);
    return block;
  }

  private void grade(Assessment assessment, Span result) {
    Map<UUID, Set<Integer>> given = new HashMap<>();
    answers.forEach((qid, group) -> given.put(qid, group.getSelectedItems()));

    String learner = currentLearnerName();
    CheckService.SubmitOutcome outcome = checkService.submit(learner, assessment, given);
    Attempt attempt = outcome.attempt();
    int count = checkService.attemptCount(learner, assessment.id());

    boolean passed = attempt.passed();
    // Mint the credential only on the FIRST passing attempt — the decision is made
    // atomically inside CheckService.submit, so re-passing never duplicates and
    // there is no double-mint race. issueFor itself no-ops on a failed attempt.
    if (outcome.firstPass()) {
      new IssuanceService(CredentialRepositoryProvider.repository(), IssuerIdentity.fromConfig())
          .issueFor(attempt, assessment.title(), CredentialType.COMPLETION_CERTIFICATE, null);
    }
    result.getElement().setAttribute("data-check-result", passed ? "PASSED" : "FAILED");
    result.getElement().setAttribute("data-attempt", Integer.toString(count));
    result.getElement().getThemeList().clear();
    result.getElement().getThemeList().add("badge pill " + (passed ? "success" : "error"));
    String label = passed
        ? tr("check.passed", "Passed!")
        : tr("check.failed", "Not passed yet.");
    result.setText(label + " " + tr("check.score", "Score: {0}/{1}. Attempt {2}.",
        attempt.result().correct(), attempt.result().total(), count));
    result.setVisible(true);
  }

  private static String currentLearnerName() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name)
        .orElse("anonymous");
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
