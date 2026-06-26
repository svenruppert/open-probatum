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
import com.svenruppert.openprobatum.credential.CredentialPdf;
import com.svenruppert.openprobatum.credential.CredentialQr;
import com.svenruppert.openprobatum.credential.CredentialRepository;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.EffectiveStatus;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.AppClock;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * The learner's credential wallet (concept §19): every credential they hold, with
 * its effective status, optional expiry, a QR code + PDF certificate download,
 * and the public validation (share) link. The credential record stays the source
 * of truth — the wallet only renders + links to it.
 *
 * @since V00.20.00
 */
@Route(value = WalletView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class WalletView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "wallet";

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  private final CredentialRepository credentials = CredentialRepositoryProvider.repository();

  public WalletView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("wallet.heading", "My credentials"),
        tr("wallet.subtitle", "Your earned credentials — status, certificate and validation link.")));

    String me = currentLearnerName();
    var mine = credentials.all().stream()
        .filter(c -> c.recipientName().equals(me))
        .sorted(Comparator.comparing(Credential::issuedAt).reversed())
        .toList();

    if (mine.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.DIPLOMA,
          tr("wallet.empty.title", "No credentials yet"),
          tr("wallet.empty.body", "Pass a completion check to earn your first credential.")));
      return;
    }
    mine.forEach(c -> root.add(card(c)));
  }

  private Div card(Credential credential) {
    EffectiveStatus status = credential.effectiveStatusAt(AppClock.now());
    String validationUrl = IssuerIdentity.fromConfig().validationUrl(credential.id());

    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("max-width", "360px").set("padding", "var(--lumo-space-m)");
    card.getElement().setAttribute("data-credential", credential.id().toString());

    H4 title = new H4(credential.title());

    Span statusBadge = new Span(status.name());
    statusBadge.getElement().setAttribute("data-status", status.name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (status == EffectiveStatus.VALID ? "success" : "error"));

    card.add(title, statusBadge,
        new Div(new Span(tr("wallet.issued", "Issued") + ": " + DATE.format(credential.issuedAt()))));
    credential.expiry().ifPresent(e -> card.add(
        new Div(new Span(tr("wallet.expires", "Expires") + ": " + DATE.format(e)))));

    // QR + PDF + share link.
    byte[] qrPng = CredentialQr.pngFor(validationUrl);
    Image qr = new Image(new StreamResource("qr-" + credential.id() + ".png",
        () -> new ByteArrayInputStream(qrPng)), "QR");
    qr.getElement().setAttribute("data-qr", "true");
    qr.setWidth("140px");

    Anchor pdf = new Anchor(new StreamResource("credential-" + credential.id() + ".pdf",
        () -> new ByteArrayInputStream(
            CredentialPdf.render(credential, validationUrl, qrPng, AppClock.now()))),
        tr("wallet.download", "Download PDF"));
    pdf.getElement().setAttribute("download", true);
    pdf.getElement().setAttribute("data-pdf", "true");

    Anchor share = new Anchor(validationUrl, tr("wallet.share", "Validation link"));
    share.getElement().setAttribute("data-share", validationUrl);

    card.add(qr, new Div(pdf), new Div(share));
    return card;
  }

  private static String currentLearnerName() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name)
        .orElse("");
  }
}
