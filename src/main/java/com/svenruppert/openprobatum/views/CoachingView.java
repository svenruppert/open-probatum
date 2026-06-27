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
import com.svenruppert.openprobatum.coaching.CoachingOfferService;
import com.svenruppert.openprobatum.coaching.CoachingSlot;
import com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider;
import com.svenruppert.openprobatum.coaching.CoachingSlotService;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * The learner's coaching surface (concept §7.x): browse published coaching offers,
 * book an open 1:1 slot, and track your own bookings (own-data, §3.6), with the
 * option to release a booked slot.
 *
 * @since V00.60.00
 */
@Route(value = CoachingView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class CoachingView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "coaching";

  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

  public CoachingView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("coaching.heading", "Coaching"),
        tr("coaching.subtitle", "Book a 1:1 session and earn a certificate on completion.")));

    List<CoachingOffer> published = new CoachingOfferService().published().stream()
        .sorted(Comparator.comparing(CoachingOffer::title))
        .toList();
    if (published.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.COMMENTS,
          tr("coaching.empty.title", "No coaching yet"),
          tr("coaching.empty.body", "Published coaching offers will appear here.")));
    } else {
      published.forEach(o -> root.add(offerCard(o)));
    }
    root.add(buildMine());
  }

  private Div offerCard(CoachingOffer offer) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("max-width", "440px").set("padding", "var(--lumo-space-m)")
        .set("margin-bottom", "var(--lumo-space-m)");
    card.getElement().setAttribute("data-offer", offer.id().toString());
    card.add(new H4(offer.title() + " · " + offer.coachName()), new Paragraph(offer.description()));

    List<CoachingSlot> open = CoachingSlotRepositoryProvider.repository().openSlotsOf(offer.id());
    if (open.isEmpty()) {
      card.add(new Span(tr("coaching.noslots", "No open slots right now.")));
    }
    for (CoachingSlot slot : open) {
      Div line = new Div();
      line.getElement().setAttribute("data-slot", slot.id().toString());
      line.add(new Span(WHEN.format(slot.startsAt()) + " "));
      Button book = new Button(tr("coaching.action.book", "Book"), e -> {
        Long me = currentLearnerId();
        if (me != null) {
          new CoachingSlotService().book(slot.id(), me, currentLearnerName());
        }
        render();
      });
      book.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      book.getElement().setAttribute("data-action", "book");
      line.add(book);
      card.add(line);
    }
    return card;
  }

  private Div buildMine() {
    Div box = new Div();
    box.add(new H3(tr("coaching.mine.heading", "My bookings")));
    List<CoachingSlot> mine = CoachingSlotRepositoryProvider.repository()
        .forLearner(currentLearnerId());
    if (mine.isEmpty()) {
      box.add(new Paragraph(tr("coaching.mine.empty", "You have not booked any coaching yet.")));
      return box;
    }
    mine.forEach(s -> box.add(bookingRow(s)));
    return box;
  }

  private Div bookingRow(CoachingSlot slot) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-booking", slot.id().toString());

    Span line = new Span(WHEN.format(slot.startsAt()) + " — ");
    Span statusBadge = new Span(slot.status().name());
    statusBadge.getElement().setAttribute("data-status", slot.status().name());
    statusBadge.getElement().getThemeList().add("badge pill contrast");
    card.add(line, statusBadge);

    if (slot.isBooked()) {
      Button cancel = new Button(tr("coaching.action.cancel", "Release"), e -> {
        new CoachingSlotService().cancelBooking(slot.id(), currentLearnerId());
        render();
      });
      cancel.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      cancel.getElement().setAttribute("data-action", "release");
      card.add(cancel);
    }
    return card;
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
