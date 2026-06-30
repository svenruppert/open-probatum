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

import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.services.RegistrationResult;
import com.svenruppert.openprobatum.security.services.UserProvisioningService;
import com.svenruppert.openprobatum.security.services.UserProvisioningService.ProvisionOutcome;
import com.svenruppert.openprobatum.security.services.UserProvisioningService.UserSpec;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The reusable "create users" wizard panel (concept §5): for each offered persona
 * it explains the role and offers rows with a <em>pre-filled generic username and
 * an empty password</em>, so the operator only has to type passwords (or skip a
 * persona by leaving it blank). "Create users" provisions every filled row through
 * the validated {@link UserProvisioningService} (operator-chosen passwords — nothing
 * is shipped) and reports the outcome per row. Used both in the bootstrap onboarding
 * (all personas) and as an admin action scoped to a single role.
 *
 * @since V00.80.00
 */
public final class UserProvisioningPanel extends Composite<VerticalLayout>
    implements I18nSupport {

  /** A mutable editor row backing one user to provision. */
  static final class UserDraft {
    private final AuthorizationRole role;
    private String username;
    private String password = "";

    UserDraft(AuthorizationRole role, String username) {
      this.role = role;
      this.username = username;
    }
  }

  private final UserProvisioningService service;
  private final List<AuthorizationRole> personas;
  private final List<UserDraft> drafts = new ArrayList<>();
  private final Div results = new Div();

  public UserProvisioningPanel(List<AuthorizationRole> personas) {
    this(personas, new UserProvisioningService());
  }

  public UserProvisioningPanel(List<AuthorizationRole> personas, UserProvisioningService service) {
    this.personas = List.copyOf(personas);
    this.service = service;
    build();
  }

  /** The default onboarding personas (every functional role; the admin is the bootstrap account). */
  public static UserProvisioningPanel forOnboarding() {
    return new UserProvisioningPanel(List.of(
        AuthorizationRole.AUTHOR, AuthorizationRole.REVIEWER, AuthorizationRole.COACH,
        AuthorizationRole.LEARNER, AuthorizationRole.CREDENTIAL_MANAGER,
        AuthorizationRole.VERIFIER));
  }

  /** A panel scoped to a single role (the admin "add a user for this role" action). */
  public static UserProvisioningPanel forRole(AuthorizationRole role) {
    return new UserProvisioningPanel(List.of(role));
  }

  private void build() {
    VerticalLayout root = getContent();
    root.setPadding(false);
    for (AuthorizationRole persona : personas) {
      root.add(personaSection(persona));
    }
    Button create = new Button(tr("provision.action.create", "Create users"), e -> provision());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    create.getElement().setAttribute("data-action", "provision");
    root.add(create, results);
  }

  private Div personaSection(AuthorizationRole persona) {
    Div section = new Div();
    section.getElement().setAttribute("data-persona", persona.name());
    section.getStyle().set("margin-bottom", "var(--lumo-space-m)")
        .set("padding", "var(--lumo-space-s)")
        .set("border", "1px solid var(--lumo-contrast-10pct)")
        .set("border-radius", "var(--lumo-border-radius-m)");
    section.add(new H4(roleTitle(persona)));
    section.add(new Paragraph(roleDescription(persona)));

    VerticalLayout rows = new VerticalLayout();
    rows.setPadding(false);
    rows.setSpacing(false);
    section.add(rows);
    addRow(persona, rows);

    Button add = new Button(tr("provision.action.add", "Add another user"),
        e -> addRow(persona, rows));
    add.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    add.getElement().setAttribute("data-action", "add");
    section.add(add);
    return section;
  }

  private void addRow(AuthorizationRole persona, VerticalLayout rows) {
    long sameRole = drafts.stream().filter(d -> d.role == persona).count();
    String generic = persona.name().toLowerCase(java.util.Locale.ROOT) + (sameRole + 1);
    UserDraft draft = new UserDraft(persona, generic);
    drafts.add(draft);

    TextField username = new TextField();
    username.setValue(draft.username);
    username.setPlaceholder(tr("provision.username", "Username"));
    username.addValueChangeListener(e -> draft.username = e.getValue() == null ? "" : e.getValue());
    username.getElement().setAttribute("data-username", persona.name());

    PasswordField password = new PasswordField();
    password.setPlaceholder(tr("provision.password", "Password"));
    password.addValueChangeListener(e -> draft.password = e.getValue() == null ? "" : e.getValue());
    password.getElement().setAttribute("data-password", persona.name());

    Button remove = new Button(tr("provision.action.remove", "Remove"), e -> {
      drafts.remove(draft);
      rows.remove(rows.getComponentAt(rows.getComponentCount() - 1));
    });
    remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR,
        ButtonVariant.LUMO_SMALL);

    HorizontalLayout row = new HorizontalLayout(username, password, remove);
    row.setWidthFull();
    rows.add(row);
  }

  /** Provisions every filled row (username + password); blank rows are skipped. */
  void provision() {
    List<UserSpec> specs = drafts.stream()
        .filter(d -> d.username != null && !d.username.isBlank() && !d.password.isBlank())
        .map(d -> new UserSpec(d.username.trim(), d.password, d.username.trim(), Set.of(d.role)))
        .toList();
    results.removeAll();
    if (specs.isEmpty()) {
      results.add(resultLine("NONE",
          tr("provision.none", "Nothing to create — enter at least one password."), false));
      return;
    }
    for (ProvisionOutcome outcome : service.provision(specs)) {
      String user = outcome.spec().username();
      if (outcome.created()) {
        results.add(resultLine("CREATED",
            tr("provision.result.created", "Created: {0}", user), true));
      } else {
        results.add(resultLine("FAILED",
            tr("provision.result.failed", "Skipped {0}: {1}", user, reason(outcome.result())),
            false));
      }
    }
  }

  private Div resultLine(String marker, String text, boolean ok) {
    Div line = new Div(new Span(text));
    line.getElement().setAttribute("data-provision-result", marker);
    line.getElement().getThemeList().add("badge pill " + (ok ? "success" : "error"));
    line.getStyle().set("margin-top", "var(--lumo-space-xs)");
    return line;
  }

  private String reason(RegistrationResult result) {
    return switch (result) {
      case RegistrationResult.UsernameTaken ignored ->
          tr("provision.reason.username", "username already taken");
      case RegistrationResult.NameTaken ignored ->
          tr("provision.reason.name", "display name already taken");
      case RegistrationResult.WeakPassword weak -> weak.reason();
      case RegistrationResult.InvalidInput invalid -> invalid.reason();
      case RegistrationResult.Success ignored -> "";
    };
  }

  private String roleTitle(AuthorizationRole role) {
    return tr("provision.role." + role.name().toLowerCase(java.util.Locale.ROOT) + ".title",
        role.name());
  }

  private String roleDescription(AuthorizationRole role) {
    return tr("provision.role." + role.name().toLowerCase(java.util.Locale.ROOT) + ".desc",
        roleDescriptionFallback(role));
  }

  private static String roleDescriptionFallback(AuthorizationRole role) {
    return switch (role) {
      case AUTHOR -> "Creates catalogue content: offerings, questions, labs, bundles, workshops.";
      case REVIEWER -> "Reviews + approves content, assesses labs, runs workshops.";
      case COACH -> "Authors and delivers 1:1 coaching offers.";
      case LEARNER -> "Learns, practises and earns credentials.";
      case CREDENTIAL_MANAGER -> "Governs issued credentials (revoke / reissue).";
      case VERIFIER -> "Authenticated verification of credentials.";
      case PLATFORM_ADMIN -> "Operates the instance; holds every permission.";
    };
  }
}
