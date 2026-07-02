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

package junit.com.svenruppert.openprobatum.bundle;

import com.svenruppert.openprobatum.access.AccessDecision;
import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.BundleAccessService;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BundleAccessService — granting a bundle entitles every member (P004)")
class BundleAccessServiceTest {

  private static LearningPath path() {
    return new LearningPath("P", List.of(Module.mandatory("M", "c")));
  }

  @Test
  @DisplayName("granting a bundle unlocks each code-gated member offering for the learner")
  void grantEntitlesMembers() {
    InMemoryCatalogRepository catalog = new InMemoryCatalogRepository();
    InMemoryEntitlementRepository entRepo = new InMemoryEntitlementRepository();
    EntitlementService entitlements = new EntitlementService(entRepo);
    BundleAccessService access = new BundleAccessService(catalog, entitlements);

    // Members must be PUBLISHED to be learner-accessible (P004).
    Offering a = Offering.codePath("Gated A", "d", path(), "AAA")
        .withStatus(com.svenruppert.openprobatum.content.ContentStatus.PUBLISHED);
    Offering b = Offering.codePath("Gated B", "d", path(), "BBB")
        .withStatus(com.svenruppert.openprobatum.content.ContentStatus.PUBLISHED);
    catalog.save(a);
    catalog.save(b);
    Bundle bundle = Bundle.draft("Pack", "d", Set.of(a.id(), b.id()));

    AppUser user = new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER));
    assertEquals(AccessDecision.CODE_REQUIRED, entitlements.canAccess(user, a), "gated before");
    assertEquals(AccessDecision.CODE_REQUIRED, entitlements.canAccess(user, b), "gated before");

    access.grant(user, bundle);

    assertEquals(AccessDecision.GRANTED, entitlements.canAccess(user, a), "unlocked by the bundle");
    assertEquals(AccessDecision.GRANTED, entitlements.canAccess(user, b), "unlocked by the bundle");
  }

  @Test
  @DisplayName("members() resolves the bundle's offerings from the catalog")
  void membersResolve() {
    InMemoryCatalogRepository catalog = new InMemoryCatalogRepository();
    BundleAccessService access = new BundleAccessService(catalog,
        new EntitlementService(new InMemoryEntitlementRepository()));
    Offering a = Offering.publicPath("A", "d", path())
        .withStatus(com.svenruppert.openprobatum.content.ContentStatus.PUBLISHED);
    catalog.save(a);
    Bundle bundle = Bundle.draft("Pack", "d", Set.of(a.id(), java.util.UUID.randomUUID()));
    // Only the resolvable, PUBLISHED member is returned (the random id has no
    // offering; a draft member would be filtered out — P004).
    assertEquals(List.of(a), access.members(bundle));
  }
}
