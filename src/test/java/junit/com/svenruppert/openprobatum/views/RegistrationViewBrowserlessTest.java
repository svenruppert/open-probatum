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

package junit.com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.model.Credentials;
import com.svenruppert.openprobatum.security.model.UserDirectory;
import com.svenruppert.openprobatum.security.model.UserDirectoryProvider;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.RegistrationView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import junit.com.svenruppert.openprobatum.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RegistrationView — public self-registration form wiring (P002)")
class RegistrationViewBrowserlessTest extends BrowserlessTest {

  private static final String STRONG = "zR9q!sX5#kP2@dM7L";

  @BeforeEach
  void seed() {
    // Installs a directory holding one admin ("admin", id 1000) + wires bootstrap
    // so RegistrationView's no-arg RegistrationService() resolves the policy.
    TestSupport.seedAdminAndResetBootstrap();
  }

  @AfterEach
  void reset() {
    UserDirectoryProvider.reset();
  }

  @Test
  @DisplayName("NAV constant is 'register'")
  void navConstant() {
    assertEquals("register", RegistrationView.NAV);
  }

  @Test
  @DisplayName("a valid submission registers a Learner that can authenticate")
  void validSubmissionRegistersLearner() throws Exception {
    RegistrationView view = new RegistrationView();
    setText(view, "usernameField", "newbie");
    setPwd(view, "passwordField", STRONG);
    setPwd(view, "confirmField", STRONG);

    submit(view);

    UserDirectory dir = UserDirectoryProvider.directory();
    assertTrue(dir.usernameExists("newbie"));
    AppUser created = dir.findByCredentials(new Credentials("newbie", STRONG)).orElseThrow();
    assertEquals(Set.of(AuthorizationRole.LEARNER), created.roles());
  }

  @Test
  @DisplayName("mismatched confirmation shows MISMATCH and persists nothing")
  void mismatchIsRejected() throws Exception {
    RegistrationView view = new RegistrationView();
    setText(view, "usernameField", "mike");
    setPwd(view, "passwordField", STRONG);
    setPwd(view, "confirmField", "different-pass-99");

    submit(view);

    assertEquals("MISMATCH", statusMarker(view));
    assertFalse(UserDirectoryProvider.directory().usernameExists("mike"));
  }

  @Test
  @DisplayName("an already-taken username shows USERNAME_TAKEN")
  void duplicateUsernameRejected() throws Exception {
    RegistrationView view = new RegistrationView();
    setText(view, "usernameField", "admin"); // seeded by TestSupport
    setPwd(view, "passwordField", STRONG);
    setPwd(view, "confirmField", STRONG);

    submit(view);

    assertEquals("USERNAME_TAKEN", statusMarker(view));
  }

  // ── reflection helpers (mirror SetupViewBrowserlessTest) ────────

  private static void setText(RegistrationView v, String field, String value) throws Exception {
    Field f = RegistrationView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  private static void setPwd(RegistrationView v, String field, String value) throws Exception {
    Field f = RegistrationView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((PasswordField) f.get(v)).setValue(value);
  }

  private static void submit(RegistrationView v) throws Exception {
    Method m = RegistrationView.class.getDeclaredMethod("submit");
    m.setAccessible(true);
    m.invoke(v);
  }

  private static String statusMarker(RegistrationView v) throws Exception {
    Field f = RegistrationView.class.getDeclaredField("status");
    f.setAccessible(true);
    return ((Span) f.get(v)).getElement().getAttribute("data-result");
  }
}
