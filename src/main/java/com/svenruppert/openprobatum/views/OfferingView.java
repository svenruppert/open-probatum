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
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import java.util.Optional;
import java.util.UUID;

/**
 * Offering detail (concept §7.3): the learning path's modules plus the access
 * gate for the current learner — sign in, redeem a code, satisfy a prerequisite,
 * or (when granted) start learning. The actual path-working UI arrives in a later
 * issue; this view resolves and unlocks access.
 *
 * @since V00.20.00
 */
@Route(value = OfferingView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class OfferingView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "offering";

  private final CatalogRepository catalog = CatalogRepositoryProvider.repository();
  private final EntitlementService entitlements = new EntitlementService();

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String id) {
    VerticalLayout root = getContent();
    root.removeAll();

    Optional<Offering> found = parse(id).flatMap(catalog::findById);
    if (found.isEmpty()) {
      Span unknown = new Span(tr("offering.unknown", "This offering does not exist."));
      unknown.getElement().setAttribute("data-offering-result", "UNKNOWN");
      root.add(new PageHeader(tr("offering.heading", "Offering")), unknown);
      return;
    }
    render(root, found.get());
  }

  private void render(VerticalLayout root, Offering offering) {
    root.add(new PageHeader(offering.title(), offering.description()));

    root.add(new H4(tr("offering.modules", "What you will work through")));
    UnorderedList modules = new UnorderedList();
    for (Module m : offering.path().modules()) {
      modules.add(new ListItem(m.title()));
    }
    root.add(modules);

    root.add(gate(root, offering));
  }

  private VerticalLayout gate(VerticalLayout root, Offering offering) {
    AppUser user = currentUser();
    AccessDecision decision = entitlements.canAccess(user, offering);

    VerticalLayout box = new VerticalLayout();
    box.setPadding(false);
    box.getElement().setAttribute("data-access", decision.name());

    switch (decision) {
      case GRANTED -> {
        Span ok = new Span(tr("offering.granted", "You have access — start learning."));
        ok.getElement().getThemeList().add("badge success pill");
        box.add(ok);
      }
      case LOGIN_REQUIRED -> box.add(new Paragraph(
          tr("offering.login", "Please sign in to access this offering.")));
      case PREREQUISITE_REQUIRED -> box.add(new Paragraph(
          tr("offering.prerequisite", "Complete the prerequisite offering to unlock this one.")));
      case CODE_REQUIRED -> box.add(codeGate(root, offering, user));
      default -> throw new IllegalStateException("unhandled access decision: " + decision);
    }
    return box;
  }

  private VerticalLayout codeGate(VerticalLayout root, Offering offering, AppUser user) {
    VerticalLayout box = new VerticalLayout();
    box.setPadding(false);
    box.add(new Paragraph(tr("offering.code.hint",
        "This offering needs an access code.")));

    TextField code = new TextField(tr("offering.code.label", "Access code"));
    Span error = new Span();
    error.setVisible(false);

    Button redeem = new Button(tr("offering.code.redeem", "Unlock"), e -> {
      if (entitlements.redeemCode(user, offering, code.getValue())) {
        setParameter(null, offering.id().toString()); // re-render → GRANTED
      } else {
        error.setText(tr("offering.code.invalid", "That code is not valid."));
        error.getElement().setAttribute("data-result", "CODE_INVALID");
        error.getElement().getThemeList().add("badge error pill");
        error.setVisible(true);
      }
    });
    redeem.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    box.add(code, redeem, error);
    return box;
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

  private static AppUser currentUser() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class).orElse(null);
  }
}
