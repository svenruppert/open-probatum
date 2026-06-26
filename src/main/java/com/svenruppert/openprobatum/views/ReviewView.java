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
import com.svenruppert.openprobatum.catalog.CatalogLifecycleService;
import com.svenruppert.openprobatum.catalog.Offering;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The reviewer's surface (concept §16.3): a single queue of content awaiting a
 * verdict — questions and offerings that an author submitted for review. A
 * reviewer approves (IN_REVIEW → APPROVED), rejects back to the author
 * (IN_REVIEW → DRAFT) or publishes an approved item (APPROVED → PUBLISHED).
 * The route is gated to {@code author:review}; an author without the Reviewer
 * role cannot reach it, so no one approves content they cannot review.
 *
 * @since V00.30.00
 */
@Route(value = ReviewView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.REVIEWER, AuthorizationRole.PLATFORM_ADMIN})
public class ReviewView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "review";

  public ReviewView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("review.heading", "Review queue"),
        tr("review.subtitle", "Approve, reject or publish content submitted for review.")));

    var questions = new QuestionBankService().pendingReview();
    var offerings = new CatalogLifecycleService().pendingReview();
    if (questions.isEmpty() && offerings.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.CHECK_SQUARE_O,
          tr("review.empty.title", "Nothing to review"),
          tr("review.empty.body", "There is no content awaiting a verdict.")));
      return;
    }

    if (!questions.isEmpty()) {
      root.add(new H3(tr("review.section.questions", "Questions")));
      questions.forEach(q -> root.add(questionRow(q)));
    }
    if (!offerings.isEmpty()) {
      root.add(new H3(tr("review.section.offerings", "Offerings")));
      offerings.forEach(o -> root.add(offeringRow(o)));
    }
  }

  private Div questionRow(Question q) {
    QuestionBankService service = new QuestionBankService();
    Div card = card("data-question", q.id(), q.text() + "  (v" + q.version() + ")", q.status());
    card.add(actions(q.status(),
        () -> guardedApprove(() -> service.approve(q.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(q.id()), card),
        () -> guardedTransition(() -> service.publish(q.id()), card)));
    return card;
  }

  private Div offeringRow(Offering o) {
    CatalogLifecycleService service = new CatalogLifecycleService();
    Div card = card("data-offering", o.id(), o.title() + "  (v" + o.version() + ")", o.status());
    card.add(actions(o.status(),
        () -> guardedApprove(() -> service.approve(o.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(o.id()), card),
        () -> guardedTransition(() -> service.publish(o.id()), card)));
    return card;
  }

  /**
   * Runs an approval that may be refused by the segregation-of-duties rule
   * (a reviewer approving their own content, §3.6). On refusal the row shows an
   * inline error instead of crashing; on success the queue re-renders.
   */
  private void guardedApprove(Runnable approve, Div card) {
    try {
      approve.run();
      render();
    } catch (IllegalStateException refused) {
      inlineError(card, "review.error.selfApprove",
          "You cannot approve content you authored.", "SELF_APPROVAL");
    }
  }

  /**
   * Runs a reject/publish transition that another reviewer may have already moved
   * past (a stale card in a concurrent queue, §16.3). An illegal transition shows
   * an inline notice and re-renders the now-current queue instead of crashing.
   */
  private void guardedTransition(Runnable transition, Div card) {
    try {
      transition.run();
      render();
    } catch (IllegalStateException stale) {
      inlineError(card, "review.error.stale",
          "This item was already moved on by another reviewer.", "STALE");
    }
  }

  private void inlineError(Div card, String key, String fallback, String marker) {
    Span error = new Span(tr(key, fallback));
    error.getElement().setAttribute("data-error", marker);
    error.getElement().getThemeList().add("badge error pill");
    card.add(error);
  }

  private static Long currentReviewerId() {
    return com.svenruppert.jsentinel.authorization.api.SubjectStores.subjectStore()
        .currentSubject(com.svenruppert.openprobatum.security.model.AppUser.class)
        .map(com.svenruppert.openprobatum.security.model.AppUser::id)
        .orElse(null);
  }

  private Div card(String idAttr, UUID id, String heading, ContentStatus status) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute(idAttr, id.toString());

    card.add(new H4(heading));
    Span badge = new Span(status.name());
    badge.getElement().setAttribute("data-status", status.name());
    badge.getElement().getThemeList().add("badge pill contrast");
    card.add(badge);
    return card;
  }

  /** Approve/reject when IN_REVIEW; publish when APPROVED. */
  private Div actions(ContentStatus status, Runnable approve, Runnable reject, Runnable publish) {
    Div bar = new Div();
    bar.getStyle().set("margin-top", "var(--lumo-space-xs)");
    if (status == ContentStatus.IN_REVIEW) {
      bar.add(button(tr("review.action.approve", "Approve"), "approve",
          ButtonVariant.LUMO_PRIMARY, e -> approve.run()));
      bar.add(button(tr("review.action.reject", "Reject"), "reject",
          ButtonVariant.LUMO_ERROR, e -> reject.run()));
    } else if (status == ContentStatus.APPROVED) {
      bar.add(button(tr("review.action.publish", "Publish"), "publish",
          ButtonVariant.LUMO_SUCCESS, e -> publish.run()));
    }
    return bar;
  }

  private Button button(String label, String action, ButtonVariant variant,
                        Consumer<com.vaadin.flow.component.ClickEvent<Button>> onClick) {
    Button b = new Button(label, onClick::accept);
    b.addThemeVariants(ButtonVariant.LUMO_SMALL, variant);
    b.getElement().setAttribute("data-action", action);
    b.getStyle().set("margin-right", "var(--lumo-space-xs)");
    return b;
  }
}
