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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * The coach's slot surface (concept §7.x): open bookable time slots under your
 * published coaching offers, and review the slots you have opened. Gated to
 * {@code coaching:provide}.
 *
 * @since V00.60.00
 */
@Route(value = CoachingSlotsView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.REVIEWER, AuthorizationRole.PLATFORM_ADMIN})
public class CoachingSlotsView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "coaching-slots";

  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

  private final ComboBox<CoachingOffer> offer = new ComboBox<>();
  private final DateTimePicker startsAt = new DateTimePicker();
  private final DateTimePicker endsAt = new DateTimePicker();
  private final Span status = new Span();

  public CoachingSlotsView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("slots.heading", "Coaching slots"),
        tr("slots.subtitle", "Open bookable slots under your published offers.")));

    Long me = currentCoachId();
    List<CoachingOffer> mine = new CoachingOfferService().published().stream()
        .filter(o -> me != null && me.equals(o.coachId()))
        .sorted(Comparator.comparing(CoachingOffer::title))
        .toList();
    if (mine.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.COMMENTS,
          tr("slots.empty.title", "No published offers yet"),
          tr("slots.empty.body", "Publish a coaching offer to open slots for it.")));
    } else {
      root.add(buildForm(mine));
    }
    root.add(buildList());
  }

  private Div buildForm(List<CoachingOffer> mine) {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    offer.setLabel(tr("slots.field.offer", "Offer"));
    offer.setItems(mine);
    offer.setItemLabelGenerator(CoachingOffer::title);
    offer.setWidthFull();
    startsAt.setLabel(tr("slots.field.starts", "Starts at"));
    endsAt.setLabel(tr("slots.field.ends", "Ends at"));
    status.setVisible(false);

    Button open = new Button(tr("slots.action.open", "Open slot"), e -> open());
    open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    open.getElement().setAttribute("data-action", "open");

    form.add(offer, startsAt, endsAt, open, status);
    return form;
  }

  void open() {
    CoachingOffer selected = offer.getValue();
    LocalDateTime start = startsAt.getValue();
    LocalDateTime end = endsAt.getValue();
    if (selected == null || start == null || end == null || !end.isAfter(start)) {
      showStatus("INVALID", tr("slots.error.invalid",
          "Select an offer and a valid start/end."));
      return;
    }
    new CoachingSlotService().open(selected.id(),
        start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC));
    showStatus("OPENED", tr("slots.success", "Slot opened."));
    startsAt.clear();
    endsAt.clear();
    render();
  }

  private Div buildList() {
    Div box = new Div();
    box.add(new H3(tr("slots.mine.heading", "My slots")));
    List<CoachingSlot> mine = CoachingSlotRepositoryProvider.repository()
        .forCoach(currentCoachId()).stream()
        .sorted(Comparator.comparing(CoachingSlot::startsAt))
        .toList();
    if (mine.isEmpty()) {
      box.add(new Span(tr("slots.mine.empty", "You have not opened any slots yet.")));
      return box;
    }
    mine.forEach(s -> box.add(slotRow(s)));
    return box;
  }

  private Div slotRow(CoachingSlot slot) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-slot", slot.id().toString());

    Span line = new Span(WHEN.format(slot.startsAt()) + " — ");
    Span statusBadge = new Span(slot.status().name());
    statusBadge.getElement().setAttribute("data-status", slot.status().name());
    statusBadge.getElement().getThemeList().add("badge pill contrast");
    card.add(line, statusBadge);
    return card;
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    status.getElement().getThemeList().add(
        "badge pill " + ("OPENED".equals(marker) ? "success" : "error"));
    status.setVisible(true);
  }

  private static Long currentCoachId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
