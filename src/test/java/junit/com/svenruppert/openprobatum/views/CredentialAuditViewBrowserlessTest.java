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

import com.svenruppert.openprobatum.credential.CredentialEvent;
import com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider;
import com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository;
import com.svenruppert.openprobatum.views.CredentialAuditView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialAuditView — the credential audit trail surface (P011)")
class CredentialAuditViewBrowserlessTest extends BrowserlessTest {

  private InMemoryCredentialEventRepository events;

  @BeforeEach
  void setUp() {
    events = new InMemoryCredentialEventRepository();
    CredentialEventRepositoryProvider.setRepository(events);
  }

  @AfterEach
  void tearDown() {
    CredentialEventRepositoryProvider.reset();
  }

  @Test
  @DisplayName("the trail lists each event with its action + credential id")
  void listsEvents() {
    UUID cred = UUID.randomUUID();
    events.append(CredentialEvent.of(cred, CredentialEvent.Action.ISSUED, "system", ""));
    events.append(CredentialEvent.of(cred, CredentialEvent.Action.REVOKED, "credential-manager", ""));

    CredentialAuditView view = new CredentialAuditView();

    assertEquals(2, attributes(view, "data-event").size(), "two rows");
    List<String> actions = attributes(view, "data-action");
    assertTrue(actions.contains("ISSUED") && actions.contains("REVOKED"));
    assertTrue(attributes(view, "data-credential").stream().allMatch(cred.toString()::equals));
  }

  @Test
  @DisplayName("an empty trail shows the empty state")
  void emptyTrail() {
    assertTrue(attributes(new CredentialAuditView(), "data-event").isEmpty());
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
