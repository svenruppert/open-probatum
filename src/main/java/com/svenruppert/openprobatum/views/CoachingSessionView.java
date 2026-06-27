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
import com.svenruppert.openprobatum.coaching.CoachingSlot;
import com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider;
import com.svenruppert.openprobatum.coaching.CoachingSlotService;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Comparator;
import java.util.List;

/**
 * The coach's session surface (concept §7.x): the queue of booked sessions the
 * coach can complete. Completing a session mints a coaching credential for the
 * learner. Gated to {@code coaching:provide}. Minting happens only on the real
 * BOOKED→COMPLETED edge, so re-completing never double-mints.
 *
 * @since V00.60.00
 */
@Route(value = CoachingSessionView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.REVIEWER, AuthorizationRole.PLATFORM_ADMIN})
public class CoachingSessionView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "coaching-sessions";

  public CoachingSessionView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("sessions.heading", "Coaching sessions"),
        tr("sessions.subtitle", "Complete your booked sessions to credential the learner.")));

    Long me = currentCoachId();
    List<CoachingSlot> booked = CoachingSlotRepositoryProvider.repository()
        .forCoach(me).stream()
        .filter(CoachingSlot::isBooked)
        .sorted(Comparator.comparing(CoachingSlot::startsAt))
        .toList();
    if (booked.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.COMMENTS,
          tr("sessions.empty.title", "Nothing to complete"),
          tr("sessions.empty.body", "Your booked sessions appear here.")));
      return;
    }
    booked.forEach(s -> root.add(row(s)));
  }

  private Div row(CoachingSlot slot) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-slot", slot.id().toString());

    String offerTitle = CoachingOfferRepositoryProvider.repository().findById(slot.offerId())
        .map(o -> o.title() + " (v" + o.version() + ")")
        .orElse(slot.offerId().toString());
    card.add(new H4(offerTitle + " — " + slot.learnerName()));

    TextField notes = new TextField(tr("sessions.notes", "Session notes"));
    notes.setWidthFull();
    notes.getElement().setAttribute("data-notes-input", slot.id().toString());

    Button complete = new Button(tr("sessions.action.complete", "Complete"),
        e -> completeAndIssue(slot, notes.getValue()));
    complete.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
    complete.getElement().setAttribute("data-action", "complete");

    card.add(notes, new Div(complete));
    return card;
  }

  /**
   * Completes a session and, on the real BOOKED→COMPLETED edge only, mints the
   * coaching credential. Re-completing yields empty, so it never double-mints.
   */
  private void completeAndIssue(CoachingSlot slot, String notes) {
    new CoachingSlotService().complete(slot.id(), currentCoachId(), notes)
        .ifPresent(this::issueCredentialFor);
    render();
  }

  private void issueCredentialFor(CoachingSlot completed) {
    CoachingOffer offer = CoachingOfferRepositoryProvider.repository()
        .findById(completed.offerId()).orElse(null);
    int version = offer == null ? 1 : offer.version();
    String title = offer == null
        ? tr("sessions.credential.default", "Coaching") : offer.title();
    new IssuanceService(CredentialRepositoryProvider.repository(), IssuerIdentity.fromConfig())
        .issueForCoaching(completed.offerId(), version, completed.recipientId(),
            completed.learnerName(), title, CredentialType.COMPLETION_CERTIFICATE, null);
  }

  private static Long currentCoachId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
