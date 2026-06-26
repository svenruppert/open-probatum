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
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.WalletView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WalletView — the learner's credentials (P014)")
class WalletViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCredentialRepository credentials;

  @BeforeEach
  void setUp() {
    credentials = new InMemoryCredentialRepository();
    CredentialRepositoryProvider.setRepository(credentials);
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1001L, "Alice", EnumSet.of(AuthorizationRole.LEARNER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CredentialRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private static Credential forRecipient(String name) {
    return Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        name, "Open Probatum Academy", Instant.parse("2026-01-01T00:00:00Z"), null);
  }

  @Test
  @DisplayName("the wallet shows only the current learner's credentials, with status + QR + PDF + share")
  void showsOwnCredentials() {
    credentials.save(forRecipient("Alice"));
    credentials.save(forRecipient("Bob")); // must not appear

    WalletView view = new WalletView();

    assertEquals(1, attributes(view, "data-credential").size(), "only Alice's credential");
    assertEquals(List.of("VALID"), attributes(view, "data-status"));
    assertEquals(List.of("true"), attributes(view, "data-qr"));
    assertEquals(List.of("true"), attributes(view, "data-pdf"));
    assertTrue(attributes(view, "data-share").get(0).contains("/validate"),
        "the share link points at the validation page");
  }

  @Test
  @DisplayName("a learner with no credentials sees the empty state")
  void emptyWallet() {
    credentials.save(forRecipient("Bob")); // someone else's
    WalletView view = new WalletView();
    assertTrue(attributes(view, "data-credential").isEmpty());
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
