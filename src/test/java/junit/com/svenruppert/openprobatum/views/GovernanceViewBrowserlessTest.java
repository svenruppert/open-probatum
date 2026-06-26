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

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.views.GovernanceView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GovernanceView — credential-manager revoke/suspend/re-issue (P017/P009)")
class GovernanceViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCredentialRepository credentials;

  @BeforeEach
  void setUp() {
    credentials = new InMemoryCredentialRepository();
    CredentialRepositoryProvider.setRepository(credentials);
  }

  @AfterEach
  void tearDown() {
    CredentialRepositoryProvider.reset();
  }

  private Credential save(CredentialStatus status) {
    Credential c = Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Open Probatum Academy", Instant.parse("2026-01-01T00:00:00Z"), null)
        .withStatus(status);
    credentials.save(c);
    return c;
  }

  @Test
  @DisplayName("revoking flips the credential to REVOKED in the repo and the view")
  void revokeFlipsStatus() {
    Credential c = save(CredentialStatus.VALID);
    GovernanceView view = new GovernanceView();

    click(view, "Revoke");

    assertEquals(CredentialStatus.REVOKED,
        credentials.findById(c.id()).orElseThrow().status());
    assertEquals(List.of("REVOKED"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("reinstating a suspended credential returns it to VALID")
  void reinstateReturnsToValid() {
    Credential c = save(CredentialStatus.SUSPENDED);
    GovernanceView view = new GovernanceView();

    click(view, "Reinstate");

    assertEquals(CredentialStatus.VALID,
        credentials.findById(c.id()).orElseThrow().status());
    assertEquals(List.of("VALID"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("re-issuing supersedes the predecessor and adds a fresh VALID successor (P009)")
  void reissueSupersedesAndRenews() {
    Credential original = save(CredentialStatus.VALID);
    GovernanceView view = new GovernanceView();

    click(view, "Re-issue");

    assertEquals(2, credentials.all().size(), "a successor was minted");
    assertEquals(CredentialStatus.SUPERSEDED,
        credentials.findById(original.id()).orElseThrow().status());
    // The view now shows both: the superseded predecessor + the VALID successor.
    List<String> statuses = attributes(view, "data-status");
    assertEquals(2, statuses.size());
    assertTrue(statuses.contains("SUPERSEDED") && statuses.contains("VALID"));
  }

  @Test
  @DisplayName("the empty registry shows an empty state")
  void emptyRegistry() {
    GovernanceView view = new GovernanceView();
    assertEquals(List.of(), attributes(view, "data-credential"));
  }

  private static void click(Component root, String action) {
    List<Button> buttons = new ArrayList<>();
    collectButtons(root, action, buttons);
    buttons.get(0).click();
  }

  private static void collectButtons(Component c, String action, List<Button> out) {
    if (c instanceof Button b && action.equals(b.getElement().getAttribute("data-action"))) {
      out.add(b);
    }
    c.getChildren().forEach(child -> collectButtons(child, action, out));
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
