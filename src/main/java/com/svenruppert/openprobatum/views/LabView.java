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
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabService;
import com.svenruppert.openprobatum.lab.LabSubmission;
import com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider;
import com.svenruppert.openprobatum.lab.LabSubmissionService;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Comparator;
import java.util.List;

/**
 * The learner's lab surface (concept §16.3): browse published {@link Lab}s,
 * submit practical evidence (a write-up + an optional artefact link) against one,
 * and track your own submissions and their assessor verdicts. A learner sees
 * only their own submissions (own-data, §3.6).
 *
 * @since V00.40.00
 */
@Route(value = LabView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class LabView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "labs";

  private final ComboBox<Lab> labSelect = new ComboBox<>();
  private final TextArea writeUp = new TextArea();
  private final TextField artefactLink = new TextField();
  private final Span status = new Span();

  public LabView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("labs.heading", "Labs"),
        tr("labs.subtitle", "Submit practical evidence and track your verdicts.")));

    List<Lab> published = new LabService().published().stream()
        .sorted(Comparator.comparing(Lab::title))
        .toList();
    if (published.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.FLASK,
          tr("labs.empty.title", "No labs yet"),
          tr("labs.empty.body", "Published labs will appear here to work on.")));
    } else {
      root.add(buildForm(published));
    }
    root.add(buildMySubmissions());
  }

  private Div buildForm(List<Lab> published) {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    labSelect.setLabel(tr("labs.field.lab", "Lab"));
    labSelect.setItems(published);
    labSelect.setItemLabelGenerator(l -> l.title() + " (v" + l.version() + ")");
    labSelect.setWidthFull();
    labSelect.addValueChangeListener(e -> showSelectedDetail(form, e.getValue()));
    writeUp.setLabel(tr("labs.field.writeup", "Your write-up"));
    writeUp.setWidthFull();
    artefactLink.setLabel(tr("labs.field.link", "Artefact link (optional)"));
    artefactLink.setWidthFull();
    status.setVisible(false);

    Button submit = new Button(tr("labs.action.submit", "Submit evidence"), e -> submit());
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submit.getElement().setAttribute("data-action", "submit");

    form.add(labSelect, writeUp, artefactLink, submit, status);
    return form;
  }

  private void showSelectedDetail(Div form, Lab lab) {
    form.getChildren()
        .filter(c -> c.getElement().hasAttribute("data-lab-detail"))
        .toList()
        .forEach(c -> form.getElement().removeChild(c.getElement()));
    if (lab == null) {
      return;
    }
    Div detail = new Div(new Paragraph(lab.instructions()),
        new Paragraph(tr("labs.acceptance", "Acceptance: {0}", lab.acceptanceCriteria())));
    detail.getElement().setAttribute("data-lab-detail", lab.id().toString());
    form.addComponentAtIndex(1, detail);
  }

  void submit() {
    Lab lab = labSelect.getValue();
    String text = writeUp.getValue() == null ? "" : writeUp.getValue().trim();
    if (lab == null || text.isBlank()) {
      showStatus("INVALID", tr("labs.error.invalid",
          "Select a lab and describe what you did."));
      return;
    }
    Long learnerId = currentLearnerId();
    if (learnerId == null) {
      // No identified learner (no subject) — a submission must belong to one.
      showStatus("INVALID", tr("labs.error.unavailable",
          "That lab is no longer available for submission."));
      return;
    }
    var submission = new LabSubmissionService().submit(lab.id(), learnerId,
        currentLearnerName(), text, artefactLink.getValue());
    if (submission.isEmpty()) {
      showStatus("INVALID", tr("labs.error.unavailable",
          "That lab is no longer available for submission."));
      return;
    }
    showStatus("SUBMITTED", tr("labs.success", "Evidence submitted for review."));
    writeUp.clear();
    artefactLink.clear();
    labSelect.clear();
    render();
  }

  private Div buildMySubmissions() {
    Div box = new Div();
    box.add(new H3(tr("labs.mine.heading", "My submissions")));
    List<LabSubmission> mine = LabSubmissionRepositoryProvider.repository()
        .forLearner(currentLearnerId());
    if (mine.isEmpty()) {
      box.add(new Paragraph(tr("labs.mine.empty", "You have not submitted any evidence yet.")));
      return box;
    }
    mine.forEach(s -> box.add(submissionRow(s)));
    return box;
  }

  private Div submissionRow(LabSubmission submission) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-submission", submission.id().toString());

    Span statusBadge = new Span(submission.status().name());
    statusBadge.getElement().setAttribute("data-status", submission.status().name());
    statusBadge.getElement().getThemeList().add("badge pill contrast");
    card.add(new H4(tr("labs.mine.lab", "Lab {0} v{1}",
        submission.labId().toString(), submission.labVersion())), statusBadge);
    if (!submission.assessorFeedback().isBlank()) {
      Span feedback = new Span(submission.assessorFeedback());
      feedback.getElement().setAttribute("data-feedback", submission.assessorFeedback());
      card.add(feedback);
    }
    return card;
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    status.getElement().getThemeList().add(
        "badge pill " + ("SUBMITTED".equals(marker) ? "success" : "error"));
    status.setVisible(true);
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
