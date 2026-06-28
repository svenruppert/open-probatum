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
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Module;
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

import java.util.List;
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
    var labs = new com.svenruppert.openprobatum.lab.LabService().pendingReview();
    var bundles = new com.svenruppert.openprobatum.bundle.BundleService().pendingReview();
    var workshops = new com.svenruppert.openprobatum.workshop.WorkshopService().pendingReview();
    var coachingOffers = new com.svenruppert.openprobatum.coaching.CoachingOfferService().pendingReview();
    if (questions.isEmpty() && offerings.isEmpty() && labs.isEmpty()
        && bundles.isEmpty() && workshops.isEmpty() && coachingOffers.isEmpty()) {
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
    if (!labs.isEmpty()) {
      root.add(new H3(tr("review.section.labs", "Labs")));
      labs.forEach(l -> root.add(labRow(l)));
    }
    if (!bundles.isEmpty()) {
      root.add(new H3(tr("review.section.bundles", "Bundles")));
      bundles.forEach(b -> root.add(bundleRow(b)));
    }
    if (!workshops.isEmpty()) {
      root.add(new H3(tr("review.section.workshops", "Workshops")));
      workshops.forEach(w -> root.add(workshopRow(w)));
    }
    if (!coachingOffers.isEmpty()) {
      root.add(new H3(tr("review.section.coaching", "Coaching")));
      coachingOffers.forEach(o -> root.add(coachingRow(o)));
    }
  }

  private Div coachingRow(com.svenruppert.openprobatum.coaching.CoachingOffer offer) {
    com.svenruppert.openprobatum.coaching.CoachingOfferService service =
        new com.svenruppert.openprobatum.coaching.CoachingOfferService();
    Div card = card("data-offer", offer.id(),
        offer.title() + "  (v" + offer.version() + ")", offer.status());
    card.add(coachingDetail(offer));
    card.add(actions(offer.status(),
        () -> guardedApprove(() -> service.approve(offer.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(offer.id()), card),
        () -> guardedTransition(() -> service.publish(offer.id()), card)));
    return card;
  }

  private Div workshopRow(com.svenruppert.openprobatum.workshop.Workshop workshop) {
    com.svenruppert.openprobatum.workshop.WorkshopService service =
        new com.svenruppert.openprobatum.workshop.WorkshopService();
    Div card = card("data-workshop", workshop.id(),
        workshop.title() + "  (v" + workshop.version() + ")", workshop.status());
    card.add(workshopDetail(workshop));
    card.add(actions(workshop.status(),
        () -> guardedApprove(() -> service.approve(workshop.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(workshop.id()), card),
        () -> guardedTransition(() -> service.publish(workshop.id()), card)));
    return card;
  }

  private Div labRow(com.svenruppert.openprobatum.lab.Lab lab) {
    com.svenruppert.openprobatum.lab.LabService service =
        new com.svenruppert.openprobatum.lab.LabService();
    Div card = card("data-lab", lab.id(), lab.title() + "  (v" + lab.version() + ")", lab.status());
    card.add(labDetail(lab));
    card.add(actions(lab.status(),
        () -> guardedApprove(() -> service.approve(lab.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(lab.id()), card),
        () -> guardedTransition(() -> service.publish(lab.id()), card)));
    return card;
  }

  private Div bundleRow(com.svenruppert.openprobatum.bundle.Bundle bundle) {
    com.svenruppert.openprobatum.bundle.BundleService service =
        new com.svenruppert.openprobatum.bundle.BundleService();
    Div card = card("data-bundle", bundle.id(),
        bundle.title() + "  (v" + bundle.version() + ")", bundle.status());
    card.add(bundleDetail(bundle));
    card.add(actions(bundle.status(),
        () -> guardedApprove(() -> service.approve(bundle.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(bundle.id()), card),
        () -> guardedTransition(() -> service.publish(bundle.id()), card)));
    return card;
  }

  private Div questionRow(Question q) {
    QuestionBankService service = new QuestionBankService();
    Div card = card("data-question", q.id(), q.text() + "  (v" + q.version() + ")", q.status());
    card.add(questionDetail(q));
    card.add(actions(q.status(),
        () -> guardedApprove(() -> service.approve(q.id(), currentReviewerId()), card),
        () -> guardedTransition(() -> service.rejectToDraft(q.id()), card),
        () -> guardedTransition(() -> service.publish(q.id()), card)));
    return card;
  }

  private Div offeringRow(Offering o) {
    CatalogLifecycleService service = new CatalogLifecycleService();
    Div card = card("data-offering", o.id(), o.title() + "  (v" + o.version() + ")", o.status());
    card.add(offeringDetail(o));
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

  // ── read-only content the reviewer judges (P009a) ─────────────────

  private Div questionDetail(Question q) {
    Div box = detailBox();
    for (int i = 0; i < q.options().size(); i++) {
      String option = q.options().get(i);
      boolean correct = q.correctIndices().contains(i);
      Span line = new Span((correct ? "✓ " : "• ") + option);
      line.getElement().setAttribute("data-detail-option", option);
      if (correct) {
        line.getElement().setAttribute("data-detail-correct", option);
        line.getStyle().set("font-weight", "600");
      }
      box.add(new Div(line));
    }
    box.add(labelled("review.detail.explanation", "Explanation", q.explanation()));
    box.add(labelled("review.detail.objective", "Objective", q.learningObjective()));
    box.add(labelled("review.detail.topic", "Topic", q.topic()));
    box.add(labelled("review.detail.difficulty", "Difficulty", q.difficulty().name()));
    return box;
  }

  private Div offeringDetail(Offering offering) {
    Div box = detailBox();
    List<Module> modules = offering.path().modules();
    for (int i = 0; i < modules.size(); i++) {
      Module m = modules.get(i);
      String suffix = m.mandatory() ? "" : " (" + tr("review.detail.optional", "optional") + ")";
      Span line = new Span((i + 1) + ". " + m.title() + suffix);
      line.getElement().setAttribute("data-detail-module", m.title());
      line.getStyle().set("font-weight", "600");
      box.add(new Div(line));
      if (m.content() != null && !m.content().isBlank()) {
        box.add(new Div(new Span(m.content())));
      }
    }
    return box;
  }

  private Div labDetail(com.svenruppert.openprobatum.lab.Lab lab) {
    Div box = detailBox();
    box.add(labelledMarked("review.detail.instructions", "Instructions", lab.instructions(),
        "data-detail-instructions"));
    box.add(labelledMarked("review.detail.acceptance", "Acceptance criteria",
        lab.acceptanceCriteria(), "data-detail-acceptance"));
    box.add(labelled("review.detail.objective", "Objective", lab.learningObjective()));
    box.add(labelled("review.detail.difficulty", "Difficulty", lab.difficulty().name()));
    return box;
  }

  private Div bundleDetail(com.svenruppert.openprobatum.bundle.Bundle bundle) {
    Div box = detailBox();
    box.add(labelled("review.detail.description", "Description", bundle.description()));
    bundle.offeringIds().forEach(id -> {
      String title = CatalogRepositoryProvider.repository().findById(id)
          .map(Offering::title).orElse(id.toString());
      Span line = new Span("• " + title);
      line.getElement().setAttribute("data-detail-member", title);
      box.add(new Div(line));
    });
    return box;
  }

  private Div workshopDetail(com.svenruppert.openprobatum.workshop.Workshop workshop) {
    Div box = detailBox();
    box.add(labelled("review.detail.description", "Description", workshop.description()));
    box.add(labelled("review.detail.objective", "Objective", workshop.learningObjective()));
    box.add(labelled("review.detail.schedule", "Schedule",
        workshop.startsAt() + " – " + workshop.endsAt()));
    box.add(labelled("review.detail.capacity", "Capacity", String.valueOf(workshop.capacity())));
    box.add(labelled("review.detail.instructor", "Instructor", workshop.instructor()));
    return box;
  }

  private Div coachingDetail(com.svenruppert.openprobatum.coaching.CoachingOffer offer) {
    Div box = detailBox();
    box.add(labelled("review.detail.description", "Description", offer.description()));
    box.add(labelled("review.detail.objective", "Objective", offer.learningObjective()));
    box.add(labelled("review.detail.coach", "Coach", offer.coachName()));
    box.add(labelled("review.detail.duration", "Duration (min)",
        String.valueOf(offer.durationMinutes())));
    return box;
  }

  private Div detailBox() {
    Div box = new Div();
    box.getElement().setAttribute("data-detail", "");
    box.getStyle().set("margin", "var(--lumo-space-xs) 0")
        .set("padding-left", "var(--lumo-space-s)")
        .set("border-left", "2px solid var(--lumo-contrast-20pct)")
        .set("font-size", "var(--lumo-font-size-s)");
    return box;
  }

  private Div labelled(String key, String fallback, String value) {
    Div line = new Div();
    Span label = new Span(tr(key, fallback) + ": ");
    label.getStyle().set("font-weight", "600");
    line.add(label, new Span(value == null ? "" : value));
    return line;
  }

  private Div labelledMarked(String key, String fallback, String value, String marker) {
    Div line = labelled(key, fallback, value);
    line.getElement().setAttribute(marker, value == null ? "" : value);
    return line;
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
