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

import com.svenruppert.openprobatum.access.AccessDecision;
import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Comparator;

/**
 * Learner-facing catalog (concept §7.4). Lists every published {@link Offering}
 * as a card carrying its visibility and the current learner's access state
 * (resolved by {@link EntitlementService}); each card opens the offering detail.
 *
 * @since V00.20.00
 */
@Route(value = CatalogView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class CatalogView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "catalog";

  private final CatalogRepository catalog = CatalogRepositoryProvider.repository();

  public CatalogView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("catalog.heading", "Academy catalog"),
        tr("catalog.subtitle", "Browse offerings and start learning.")));

    // Learners browse only PUBLISHED offerings (§16.2); drafts/archived are hidden.
    // Deduplicate by lineage, keeping the highest version — publish() already
    // retires predecessors to REPLACED, but this is the same defensive
    // latest-per-lineage collapse that OfferingAuthoringService.myOfferings uses,
    // so a legacy double-published lineage never shows twice.
    var offerings = catalog.all().stream()
        .filter(Offering::isPublished)
        .collect(java.util.stream.Collectors.toMap(
            Offering::lineageId, o -> o,
            (a, b) -> a.version() >= b.version() ? a : b,
            java.util.LinkedHashMap::new))
        .values().stream().toList();
    if (offerings.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.OPEN_BOOK,
          tr("catalog.empty.title", "No offerings yet"),
          tr("catalog.empty.body", "An author has not published anything yet.")));
      return;
    }

    AppUser user = currentUser();
    FlexLayout grid = new FlexLayout();
    grid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    grid.getStyle().set("gap", "var(--lumo-space-l)");
    offerings.stream()
        .sorted(Comparator.comparing(Offering::title))
        .forEach(o -> grid.add(card(o, user)));
    root.add(grid);
  }

  private Component card(Offering offering, AppUser user) {
    AccessDecision decision = new EntitlementService().canAccess(user, offering);

    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("max-width", "320px").set("padding", "var(--lumo-space-m)");
    card.getElement().setAttribute("data-offering", offering.id().toString());

    H3 title = new H3(offering.title());
    Paragraph desc = new Paragraph(offering.description());

    Span visibility = new Span(offering.visibility().name());
    visibility.getElement().getThemeList().add("badge contrast pill");

    Span access = new Span(accessLabel(decision));
    access.getElement().setAttribute("data-access", decision.name());
    access.getElement().getThemeList().add(
        "badge pill " + (decision.isGranted() ? "success" : "contrast"));

    Button open = new Button(tr("catalog.action.view", "View"),
        e -> UI.getCurrent().navigate(OfferingView.class, offering.id().toString()));
    open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    card.add(title, desc, visibility, access, new Div(open));
    return card;
  }

  private String accessLabel(AccessDecision decision) {
    return switch (decision) {
      case GRANTED -> tr("catalog.access.granted", "Open");
      case LOGIN_REQUIRED -> tr("catalog.access.login", "Sign in required");
      case CODE_REQUIRED -> tr("catalog.access.code", "Access code required");
      case PREREQUISITE_REQUIRED -> tr("catalog.access.prerequisite", "Prerequisite required");
      case UNAVAILABLE -> tr("catalog.access.unavailable", "Not available");
    };
  }

  private static AppUser currentUser() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class).orElse(null);
  }
}
