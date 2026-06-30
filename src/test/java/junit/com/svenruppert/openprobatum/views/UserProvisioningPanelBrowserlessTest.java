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

import com.svenruppert.openprobatum.security.model.InMemoryUserDirectoryPersistence;
import com.svenruppert.openprobatum.security.model.PersistentUserDirectory;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.services.RegistrationService;
import com.svenruppert.openprobatum.security.services.UserProvisioningService;
import com.svenruppert.openprobatum.views.UserProvisioningPanel;
import com.svenruppert.jsentinel.credential.password.bouncycastle.BouncyCastleHashingServices;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserProvisioningPanel — guided user creation (V00.80.00 P002)")
class UserProvisioningPanelBrowserlessTest extends BrowserlessTest {

  private PersistentUserDirectory directory;
  private UserProvisioningService service;

  @BeforeEach
  void setUp() {
    directory = new PersistentUserDirectory(
        new InMemoryUserDirectoryPersistence(), BouncyCastleHashingServices.modern());
    service = new UserProvisioningService(new RegistrationService(directory, 8));
  }

  @Test
  @DisplayName("onboarding renders one section per persona with a pre-filled generic username")
  void rendersPersonaSections() {
    UserProvisioningPanel panel = new UserProvisioningPanel(List.of(
        AuthorizationRole.AUTHOR, AuthorizationRole.REVIEWER, AuthorizationRole.COACH,
        AuthorizationRole.LEARNER, AuthorizationRole.CREDENTIAL_MANAGER,
        AuthorizationRole.VERIFIER), service);

    List<String> personas = attributes(panel, "data-persona");
    assertTrue(personas.contains("AUTHOR") && personas.contains("COACH")
        && personas.contains("VERIFIER"), "a section per persona");
    assertTrue(prefilledUsernames(panel).contains("author1"),
        "the author row is pre-filled with a generic username");
  }

  @Test
  @DisplayName("a persona left blank creates nothing (skippable)")
  void blankPersonaSkipped() throws Exception {
    UserProvisioningPanel panel = new UserProvisioningPanel(
        List.of(AuthorizationRole.AUTHOR), service);
    invokeProvision(panel);

    assertTrue(attributes(panel, "data-provision-result").contains("NONE"));
    assertFalse(directory.usernameExists("author1"), "no password → not created");
  }

  @Test
  @DisplayName("a filled row provisions the user with its role")
  void provisionsFilledRow() throws Exception {
    UserProvisioningPanel panel = new UserProvisioningPanel(
        List.of(AuthorizationRole.AUTHOR), service);
    setFirstDraftPassword(panel, "good-password");
    invokeProvision(panel);

    assertTrue(attributes(panel, "data-provision-result").contains("CREATED"));
    assertTrue(directory.usernameExists("author1"));
    assertTrue(directory.findByCredentials(
            new com.svenruppert.openprobatum.security.model.Credentials("author1", "good-password"))
        .orElseThrow().roles().contains(AuthorizationRole.AUTHOR));
  }

  // ── reflection + tree helpers ──────────────────────────────────────

  @SuppressWarnings("unchecked")
  private static void setFirstDraftPassword(UserProvisioningPanel panel, String password)
      throws Exception {
    Field draftsF = UserProvisioningPanel.class.getDeclaredField("drafts");
    draftsF.setAccessible(true);
    List<Object> drafts = (List<Object>) draftsF.get(panel);
    Object draft = drafts.get(0);
    Field pw = draft.getClass().getDeclaredField("password");
    pw.setAccessible(true);
    pw.set(draft, password);
  }

  private static void invokeProvision(UserProvisioningPanel panel) throws Exception {
    Method m = UserProvisioningPanel.class.getDeclaredMethod("provision");
    m.setAccessible(true);
    m.invoke(panel);
  }

  private static List<String> prefilledUsernames(Component root) {
    List<String> out = new ArrayList<>();
    collectUsernames(root, out);
    return out;
  }

  private static void collectUsernames(Component c, List<String> out) {
    if (c instanceof com.vaadin.flow.component.textfield.TextField field
        && field.getElement().hasAttribute("data-username")) {
      out.add(field.getValue());
    }
    c.getChildren().forEach(child -> collectUsernames(child, out));
  }

  private static List<String> attributes(Component root, String name) {
    List<String> values = new ArrayList<>();
    collect(root, name, values);
    return values;
  }

  private static void collect(Component c, String name, List<String> out) {
    String v = c.getElement().getAttribute(name);
    if (v != null) {
      out.add(v);
    }
    c.getChildren().forEach(child -> collect(child, name, out));
  }
}
