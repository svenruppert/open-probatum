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

import com.svenruppert.openprobatum.credential.CredentialEvent;
import com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * The credential audit trail surface (concept §17.3): the complete, ordered
 * history of every credential lifecycle action — issuance, revoke, suspend,
 * reinstate, supersede, re-issue — read from the app-side
 * {@link CredentialEvent} log. Gated to {@code credential:manage}. Read-only:
 * the trail is append-only and shown newest first.
 *
 * @since V00.30.00
 */
@Route(value = CredentialAuditView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.CREDENTIAL_MANAGER, AuthorizationRole.PLATFORM_ADMIN})
public class CredentialAuditView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "credential-audit";

  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  public CredentialAuditView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("credaudit.heading", "Credential audit trail"),
        tr("credaudit.subtitle", "Every issuance and governance action, newest first.")));

    var events = CredentialEventRepositoryProvider.repository().all();
    if (events.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.RECORDS,
          tr("credaudit.empty.title", "No credential events yet"),
          tr("credaudit.empty.body", "Issuance and governance actions are recorded here.")));
      return;
    }
    events.forEach(e -> root.add(row(e)));
  }

  private Div row(CredentialEvent event) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-event", event.id().toString());
    card.getElement().setAttribute("data-credential", event.credentialId().toString());

    Span action = new Span(event.action().name());
    action.getElement().setAttribute("data-action", event.action().name());
    action.getElement().getThemeList().add("badge pill contrast");

    Span line = new Span(" " + STAMP.format(event.timestamp())
        + " · " + event.actor()
        + " · " + event.credentialId()
        + (event.detail().isBlank() ? "" : " · " + event.detail()));
    card.add(action, line);
    return card;
  }
}
