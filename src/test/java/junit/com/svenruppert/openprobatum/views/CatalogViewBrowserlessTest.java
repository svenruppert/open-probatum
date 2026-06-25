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

import com.svenruppert.openprobatum.access.EntitlementReason;
import com.svenruppert.openprobatum.access.EntitlementRepositoryProvider;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.CatalogView;
import com.svenruppert.openprobatum.views.OfferingView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogView / OfferingView — browse + access gate (P005)")
class CatalogViewBrowserlessTest extends BrowserlessTest {

  private final AppUser learner = new AppUser(1001L, "Learner", java.util.EnumSet.of(AuthorizationRole.LEARNER));
  private InMemoryCatalogRepository catalog;
  private InMemoryEntitlementRepository entitlements;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    entitlements = new InMemoryEntitlementRepository();
    CatalogRepositoryProvider.setRepository(catalog);
    EntitlementRepositoryProvider.setRepository(entitlements);
    SubjectStores.subjectStore().setCurrentSubject(learner, AppUser.class);
  }

  @AfterEach
  void tearDown() {
    CatalogRepositoryProvider.reset();
    EntitlementRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private static LearningPath path() {
    return new LearningPath("P", List.of(new Module("Routing", "How @Route works.")));
  }

  @Test
  @DisplayName("the catalog lists each offering with its access state")
  void listsOfferingsWithAccess() {
    catalog.save(Offering.publicPath("Open Course", "free for all", path()));
    catalog.save(Offering.codePath("Gated Course", "needs a code", path(), "SECRET"));

    List<String> access = attributes(new CatalogView(), "data-access");
    assertEquals(2, attributes(new CatalogView(), "data-offering").size(), "two cards");
    assertTrue(access.contains("GRANTED"), "the public offering is open");
    assertTrue(access.contains("CODE_REQUIRED"), "the code offering is gated");
  }

  @Test
  @DisplayName("the empty catalog shows an empty state, not a crash")
  void emptyCatalog() {
    assertTrue(attributes(new CatalogView(), "data-offering").isEmpty());
  }

  @Test
  @DisplayName("an offering detail reflects the learner's entitlement state")
  void offeringGateReflectsEntitlement() {
    Offering gated = Offering.codePath("Gated", "d", path(), "SECRET");
    catalog.save(gated);

    OfferingView locked = new OfferingView();
    locked.setParameter(null, gated.id().toString());
    assertEquals(List.of("CODE_REQUIRED"), attributes(locked, "data-access"));

    // Grant the entitlement (as redeemCode would) → the gate opens.
    entitlements.grant(new com.svenruppert.openprobatum.access.Entitlement(
        learner.id(), gated.id(), EntitlementReason.CODE));
    OfferingView unlocked = new OfferingView();
    unlocked.setParameter(null, gated.id().toString());
    assertEquals(List.of("GRANTED"), attributes(unlocked, "data-access"));
  }

  @Test
  @DisplayName("an unknown offering id renders the unknown marker")
  void unknownOffering() {
    OfferingView view = new OfferingView();
    view.setParameter(null, java.util.UUID.randomUUID().toString());
    assertEquals(List.of("UNKNOWN"), attributes(view, "data-offering-result"));
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
