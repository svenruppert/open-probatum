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
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * The assessor's surface (concept §16.3): the queue of learners' practical lab
 * submissions awaiting a verdict. An assessor verifies a submission (valid
 * practical evidence) or rejects it with feedback. Gated to {@code lab:assess};
 * an assessor may not verify a submission to a lab they authored (segregation of
 * duties, §3.6) — a self-assessment attempt shows an inline notice.
 *
 * @since V00.40.00
 */
@Route(value = AssessmentQueueView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.REVIEWER, AuthorizationRole.PLATFORM_ADMIN})
public class AssessmentQueueView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "assess";

  public AssessmentQueueView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("assess.heading", "Assessment queue"),
        tr("assess.subtitle", "Verify or reject learners' practical lab submissions.")));

    var pending = LabSubmissionRepositoryProvider.repository().pending();
    if (pending.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.CLIPBOARD_CHECK,
          tr("assess.empty.title", "Nothing to assess"),
          tr("assess.empty.body", "Submitted lab evidence will appear here.")));
      return;
    }
    pending.forEach(s -> root.add(row(s)));
  }

  private Div row(LabSubmission submission) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-submission", submission.id().toString());

    String labTitle = LabRepositoryProvider.repository().findById(submission.labId())
        .map(l -> l.title() + " (v" + l.version() + ")")
        .orElse(submission.labId().toString());
    card.add(new H4(labTitle + " — " + submission.learnerName()));
    card.add(new Paragraph(submission.writeUp()));
    submission.artefactLinkOpt().ifPresent(link -> {
      com.vaadin.flow.component.html.Anchor a =
          new com.vaadin.flow.component.html.Anchor(link, link);
      a.getElement().setAttribute("data-artefact", link);
      card.add(new Div(a));
    });

    TextField feedback = new TextField(tr("assess.feedback", "Feedback"));
    feedback.setWidthFull();
    feedback.getElement().setAttribute("data-feedback-input", submission.id().toString());

    Button verify = new Button(tr("assess.action.verify", "Verify"),
        e -> verifyAndIssue(submission, feedback.getValue(), card));
    verify.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
    verify.getElement().setAttribute("data-action", "verify");

    Button reject = new Button(tr("assess.action.reject", "Reject"),
        e -> decide(() -> new LabSubmissionService()
            .reject(submission.id(), currentAssessorId(), feedback.getValue()), card));
    reject.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
    reject.getElement().setAttribute("data-action", "reject");

    card.add(feedback, new Div(verify, reject));
    return card;
  }

  /**
   * Verifies a submission and, on the real SUBMITTED→VERIFIED edge only, mints the
   * practical-lab credential. Because {@code verify} returns the submission only
   * on that edge (empty once decided), re-verifying never double-mints (§10.x).
   * A self-assessment attempt shows an inline notice instead of minting.
   */
  private void verifyAndIssue(LabSubmission submission, String feedback, Div card) {
    try {
      new LabSubmissionService().verify(submission.id(), currentAssessorId(), feedback)
          .ifPresent(this::issueCredentialFor);
      render();
    } catch (IllegalStateException refused) {
      inlineError(card, "assess.error.selfAssess",
          "You cannot assess a submission to a lab you authored.", "SELF_ASSESS");
    }
  }

  /** Mints the practical-lab credential for a freshly verified submission (§10.6). */
  private void issueCredentialFor(LabSubmission verified) {
    String title = LabRepositoryProvider.repository().findById(verified.labId())
        .map(com.svenruppert.openprobatum.lab.Lab::title)
        .orElse(tr("assess.credential.default", "Practical lab"));
    new com.svenruppert.openprobatum.credential.IssuanceService(
        com.svenruppert.openprobatum.credential.CredentialRepositoryProvider.repository(),
        com.svenruppert.openprobatum.credential.IssuerIdentity.fromConfig())
        .issueForLab(verified, title,
            com.svenruppert.openprobatum.credential.CredentialType.PRACTITIONER_CREDENTIAL, null);
  }

  /**
   * Runs a verdict that may be refused by the segregation-of-duties rule (an
   * assessor verifying a submission to their own lab, §3.6) or by an invalid
   * rejection (blank feedback). On refusal the row shows an inline error; on
   * success the queue re-renders.
   */
  private void decide(Runnable verdict, Div card) {
    try {
      verdict.run();
      render();
    } catch (IllegalStateException refused) {
      inlineError(card, "assess.error.selfAssess",
          "You cannot assess a submission to a lab you authored.", "SELF_ASSESS");
    } catch (IllegalArgumentException invalid) {
      inlineError(card, "assess.error.feedback",
          "A rejection needs feedback for the learner.", "FEEDBACK");
    }
  }

  private void inlineError(Div card, String key, String fallback, String marker) {
    Span error = new Span(tr(key, fallback));
    error.getElement().setAttribute("data-error", marker);
    error.getElement().getThemeList().add("badge error pill");
    card.add(error);
  }

  private static Long currentAssessorId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
