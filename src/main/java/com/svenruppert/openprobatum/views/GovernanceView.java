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

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialGovernance;
import com.svenruppert.openprobatum.credential.CredentialRepository;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.EffectiveStatus;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.AppClock;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Comparator;

/**
 * Credential-Manager governance surface (concept §10.9, §17.4): every issued
 * credential with its effective status and revoke / suspend / reinstate actions.
 * Each action goes through {@link CredentialGovernance} and takes effect
 * immediately — the public validation page reflects it on the next lookup.
 *
 * @since V00.20.00
 */
@Route(value = GovernanceView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.CREDENTIAL_MANAGER)
public class GovernanceView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "governance";

  private final CredentialRepository repository = CredentialRepositoryProvider.repository();
  private final CredentialGovernance governance = new CredentialGovernance(repository);

  public GovernanceView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("governance.heading", "Credential governance"),
        tr("governance.subtitle", "Revoke, suspend or reinstate issued credentials.")));

    var all = repository.all().stream()
        .sorted(Comparator.comparing(Credential::issuedAt).reversed())
        .toList();
    if (all.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.DIPLOMA,
          tr("governance.empty.title", "No credentials issued yet"),
          tr("governance.empty.body", "Issued credentials appear here for governance.")));
      return;
    }
    all.forEach(c -> root.add(row(c)));
  }

  private Div row(Credential credential) {
    EffectiveStatus status = credential.effectiveStatusAt(AppClock.now());
    CredentialStatus stored = credential.status();

    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-credential", credential.id().toString());

    H4 title = new H4(credential.title() + " — " + credential.recipientName());
    Span statusBadge = new Span(status.name());
    statusBadge.getElement().setAttribute("data-status", status.name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (status == EffectiveStatus.VALID ? "success" : "error"));

    HorizontalLayout actions = new HorizontalLayout();
    if (stored == CredentialStatus.SUSPENDED) {
      actions.add(action(tr("governance.reinstate", "Reinstate"),
          () -> governance.reinstate(credential.id()), false));
    }
    if (stored == CredentialStatus.VALID) {
      actions.add(action(tr("governance.suspend", "Suspend"),
          () -> governance.suspend(credential.id()), false));
    }
    if (stored != CredentialStatus.REVOKED) {
      actions.add(action(tr("governance.revoke", "Revoke"),
          () -> governance.revoke(credential.id()), true));
    }

    card.add(title, statusBadge, actions);
    return card;
  }

  private Button action(String label, Runnable op, boolean danger) {
    Button button = new Button(label, e -> {
      op.run();
      render(); // immediate effect
    });
    button.addThemeVariants(danger ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_TERTIARY,
        ButtonVariant.LUMO_SMALL);
    button.getElement().setAttribute("data-action", label);
    return button;
  }
}
