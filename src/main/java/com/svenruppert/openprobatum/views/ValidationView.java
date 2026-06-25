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
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialValidator;
import com.svenruppert.openprobatum.credential.ValidationOutcome;
import com.svenruppert.openprobatum.credential.ValidationResult;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.AppClock;
import com.svenruppert.openprobatum.views.ui.GridSupport;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import java.util.UUID;

/**
 * The public verification portal (concept §11). Reachable WITHOUT login at
 * {@code /validate/<id>}; it is the sole source of truth for a credential's
 * authenticity, status and validity (§11.1). It shows only the match fields
 * (§11.3) plus the match-rule and a privacy hint (§11.4/§11.6) — never answers,
 * scores or notes. Standalone route (not the authenticated MainLayout).
 *
 * @since V00.10.00
 */
@Route("validate")
public class ValidationView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "validate";

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String id) {
    VerticalLayout root = getContent();
    root.removeAll();
    root.setMaxWidth("640px");
    root.getStyle().set("margin", "var(--lumo-space-xl) auto");

    root.add(new H2(tr("validate.heading", "Credential validation")));
    render(root, validate(id));
    root.add(matchRuleHint(), privacyHint());
  }

  private static ValidationOutcome validate(String idParam) {
    UUID id = parse(idParam);
    return new CredentialValidator(CredentialRepositoryProvider.repository())
        .validate(id, AppClock.now());
  }

  private static UUID parse(String idParam) {
    if (idParam == null || idParam.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(idParam.trim());
    } catch (IllegalArgumentException malformed) {
      return null; // not a valid id → treated as unknown
    }
  }

  private void render(VerticalLayout root, ValidationOutcome outcome) {
    ValidationResult result = outcome.result();
    Span badge = new Span(tr(result.messageKey(), result.englishLabel()));
    badge.getElement().getThemeList().add("badge " + badgeTheme(result) + " pill");
    root.add(badge);

    outcome.credentialOpt().ifPresent(c -> root.add(fields(c)));
  }

  private VerticalLayout fields(Credential c) {
    VerticalLayout box = new VerticalLayout();
    box.setPadding(false);
    box.setSpacing(false);
    box.add(field("validate.field.recipient", "Recipient", c.recipientName()));
    box.add(field("validate.field.title", "Title", c.title()));
    box.add(field("validate.field.type", "Type", c.type().name()));
    box.add(field("validate.field.issuer", "Issuer", c.issuer()));
    box.add(field("validate.field.issued", "Issued on", GridSupport.TIMESTAMP.format(c.issuedAt())));
    c.expiry().ifPresent(e ->
        box.add(field("validate.field.expiry", "Expires on", GridSupport.TIMESTAMP.format(e))));
    box.add(field("validate.field.id", "Credential ID", c.id().toString()));
    c.superseder().ifPresent(ref ->
        box.add(field("validate.field.supersededBy", "Replaced by", ref.toString())));
    return box;
  }

  private Span field(String key, String label, String value) {
    Span span = new Span(tr(key, label) + ": " + value);
    span.getStyle().set("display", "block");
    return span;
  }

  private Paragraph matchRuleHint() {
    return new Paragraph(tr("validate.matchRule",
        "This credential is genuine only if the details above match the document "
            + "presented to you. A valid status alone is not enough."));
  }

  private Paragraph privacyHint() {
    return new Paragraph(tr("validate.privacy",
        "Sharing a credential link reveals these basic fields to whoever opens it."));
  }

  private static String badgeTheme(ValidationResult result) {
    return switch (result) {
      case VALID -> "success";
      case EXPIRED, SUPERSEDED, UNKNOWN -> "contrast";
      case REVOKED, SUSPENDED, MISMATCH -> "error";
    };
  }
}
