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

package junit.com.svenruppert.openprobatum.access;

import com.svenruppert.openprobatum.access.AccessDecision;
import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.access.PathCompletionService;
import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.progress.InMemoryProgressRepository;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PathCompletionService — completing a prerequisite unlocks its dependents (P003)")
class PathCompletionServiceTest {

  private final AppUser learner = new AppUser(1001L, "Learner", EnumSet.of(AuthorizationRole.LEARNER));

  private CatalogRepository catalog;
  private ProgressService progress;
  private EntitlementService entitlements;
  private PathCompletionService completion;

  private Offering prerequisite;
  private Offering advanced;
  private Module prereqModule;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    progress = new ProgressService(new InMemoryProgressRepository());
    entitlements = new EntitlementService(new InMemoryEntitlementRepository());
    completion = new PathCompletionService(catalog, progress, entitlements);

    prereqModule = Module.mandatory("Basics", "c");
    prerequisite = Offering.publicPath("Foundation", "d", new LearningPath("P", List.of(prereqModule)))
        .withStatus(ContentStatus.PUBLISHED);
    // A PREREQUISITE-gated offering that requires `prerequisite`.
    advanced = Offering.prerequisitePath("Advanced", "d",
            new LearningPath("A", List.of(Module.mandatory("Deep", "c"))), prerequisite.id())
        .withStatus(ContentStatus.PUBLISHED);
    catalog.save(prerequisite);
    catalog.save(advanced);
  }

  @Test
  @DisplayName("the gated offering stays locked until the prerequisite path is actually complete")
  void lockedUntilPrerequisiteComplete() {
    assertEquals(AccessDecision.PREREQUISITE_REQUIRED, entitlements.canAccess(learner, advanced));

    // Running the hook before completion grants nothing (real flow, not a hand-made grant).
    assertTrue(completion.unlockDependents(learner, prerequisite).isEmpty(),
        "no unlock while the prerequisite is incomplete");
    assertEquals(AccessDecision.PREREQUISITE_REQUIRED, entitlements.canAccess(learner, advanced));
  }

  @Test
  @DisplayName("completing the prerequisite path unlocks the dependent offering")
  void completingPrerequisiteUnlocksDependent() {
    // Complete the prerequisite's only mandatory module, then run the completion hook.
    progress.markModuleComplete(learner.id(), prerequisite.id(), prereqModule.id());
    assertTrue(progress.isPathComplete(learner.id(), prerequisite));

    List<Offering> unlocked = completion.unlockDependents(learner, prerequisite);

    assertEquals(List.of(advanced), unlocked, "the dependent is unlocked");
    assertEquals(AccessDecision.GRANTED, entitlements.canAccess(learner, advanced),
        "the prerequisite gate now opens");
  }

  @Test
  @DisplayName("re-running the hook is idempotent — no duplicate grants, still granted")
  void hookIsIdempotent() {
    progress.markModuleComplete(learner.id(), prerequisite.id(), prereqModule.id());
    completion.unlockDependents(learner, prerequisite);
    completion.unlockDependents(learner, prerequisite);
    assertEquals(AccessDecision.GRANTED, entitlements.canAccess(learner, advanced));
  }
}
