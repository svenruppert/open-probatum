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
import com.svenruppert.openprobatum.security.services.RegistrationResult;
import com.svenruppert.openprobatum.security.services.RegistrationService;
import com.svenruppert.openprobatum.views.ui.BrandMark;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * Public self-registration form (concept §5.1). Reachable WITHOUT login at
 * {@code /register}; a visitor creates an account and is onboarded as a Learner,
 * then forwarded to the login page. Standalone route (not the authenticated
 * {@code MainLayout}), mirroring {@link SetupView}.
 *
 * <p>All validation + persistence live in {@link RegistrationService}; this view
 * only collects input, maps the {@link RegistrationResult} to a message, and
 * navigates on success.
 *
 * @since V00.20.00
 */
@Route(RegistrationView.NAV)
public class RegistrationView extends Composite<Div> implements I18nSupport {

  public static final String NAV = "register";

  private static final String K_HEADING = "register.heading";
  private static final String K_HINT = "register.hint";
  private static final String K_F_USER = "register.field.username";
  private static final String K_F_PWD = "register.field.password";
  private static final String K_F_CONFIRM = "register.field.confirm";
  private static final String K_F_DN = "register.field.displayName";
  private static final String K_SUBMIT = "register.action.submit";
  private static final String K_HAVE_ACCOUNT = "register.haveAccount";
  private static final String K_E_REQUIRED = "register.error.required";
  private static final String K_E_MISMATCH = "register.error.mismatch";
  private static final String K_E_TAKEN = "register.error.usernameTaken";
  private static final String K_E_NAME_TAKEN = "register.error.nameTaken";
  private static final String K_E_WEAK = "register.error.weakPassword";
  private static final String K_SUCCESS = "register.success";

  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private final PasswordField confirmField = new PasswordField();
  private final TextField displayNameField = new TextField();
  private final Span status = new Span();

  public RegistrationView() {
    usernameField.setLabel(tr(K_F_USER, "Username"));
    usernameField.setWidthFull();
    passwordField.setLabel(tr(K_F_PWD, "Password"));
    passwordField.setWidthFull();
    confirmField.setLabel(tr(K_F_CONFIRM, "Repeat password"));
    confirmField.setWidthFull();
    displayNameField.setLabel(tr(K_F_DN, "Display name (optional)"));
    displayNameField.setWidthFull();
    status.setVisible(false);

    Button submit = new Button(tr(K_SUBMIT, "Create account"), e -> submit());
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

    Anchor toLogin = new Anchor(AppLoginView.NAV, tr(K_HAVE_ACCOUNT, "Already have an account? Sign in"));

    VerticalLayout form = new VerticalLayout(
        new BrandMark(),
        new H2(tr(K_HEADING, "Create your account")),
        new Paragraph(tr(K_HINT,
            "Register to browse the catalog, work through learning paths and earn credentials.")),
        usernameField, passwordField, confirmField, displayNameField,
        status, submit, toLogin);
    form.setMaxWidth("520px");
    form.setSpacing(true);
    form.setAlignItems(FlexComponent.Alignment.STRETCH);

    Div hero = new Div(form);
    hero.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    hero.getStyle().set("max-width", "640px");
    hero.getStyle().set("margin", "var(--lumo-space-xl) auto");

    Div root = getContent();
    root.getStyle().set("display", "flex");
    root.getStyle().set("justify-content", "center");
    root.getStyle().set("padding", "var(--lumo-space-m)");
    root.add(hero);
  }

  void submit() {
    clearErrors();
    String username = usernameField.getValue() == null ? "" : usernameField.getValue().trim();
    String password = passwordField.getValue() == null ? "" : passwordField.getValue();
    String confirm = confirmField.getValue() == null ? "" : confirmField.getValue();

    if (username.isBlank() || password.isBlank()) {
      showStatus("INVALID", tr(K_E_REQUIRED, "Username and password are required."));
      return;
    }
    if (!password.equals(confirm)) {
      confirmField.setInvalid(true);
      confirmField.setErrorMessage(tr(K_E_MISMATCH, "The passwords do not match."));
      showStatus("MISMATCH", tr(K_E_MISMATCH, "The passwords do not match."));
      return;
    }

    RegistrationResult result =
        new RegistrationService().register(username, password, displayNameField.getValue());
    switch (result) {
      case RegistrationResult.Success ignored -> {
        showStatus("SUCCESS", tr(K_SUCCESS, "Account created. Please sign in."));
        UI.getCurrent().navigate(AppLoginView.class);
      }
      case RegistrationResult.UsernameTaken ignored -> {
        usernameField.setInvalid(true);
        usernameField.setErrorMessage(tr(K_E_TAKEN, "That username is already taken."));
        showStatus("USERNAME_TAKEN", tr(K_E_TAKEN, "That username is already taken."));
      }
      case RegistrationResult.NameTaken ignored -> {
        displayNameField.setInvalid(true);
        displayNameField.setErrorMessage(tr(K_E_NAME_TAKEN, "That display name is already taken."));
        showStatus("NAME_TAKEN", tr(K_E_NAME_TAKEN, "That display name is already taken."));
      }
      case RegistrationResult.WeakPassword ignored -> {
        passwordField.setInvalid(true);
        passwordField.setErrorMessage(tr(K_E_WEAK, "Please choose a stronger password."));
        showStatus("WEAK_PASSWORD", tr(K_E_WEAK, "Please choose a stronger password."));
      }
      case RegistrationResult.InvalidInput ignored ->
          showStatus("INVALID", tr(K_E_REQUIRED, "Username and password are required."));
    }
  }

  private void clearErrors() {
    usernameField.setInvalid(false);
    displayNameField.setInvalid(false);
    passwordField.setInvalid(false);
    confirmField.setInvalid(false);
    status.setVisible(false);
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    status.getElement().getThemeList().add(
        "badge " + ("SUCCESS".equals(marker) ? "success" : "error") + " pill");
    status.setVisible(true);
  }
}
