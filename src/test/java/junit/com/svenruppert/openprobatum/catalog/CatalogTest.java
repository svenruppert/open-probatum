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

package junit.com.svenruppert.openprobatum.catalog;

import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.catalog.OfferingType;
import com.svenruppert.openprobatum.catalog.OfferingVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("Catalog — Offering / LearningPath / Module (P004)")
class CatalogTest {

  private static LearningPath path() {
    return new LearningPath("Vaadin Basics",
        List.of(Module.mandatory("Routing", "How @Route works.")));
  }

  @Test
  @DisplayName("certificationPath() wraps the path with type CERTIFICATION_PATH + a random id")
  void certificationPathOffering() {
    Offering o = Offering.certificationPath("Vaadin Certified", path());
    assertEquals(OfferingType.CERTIFICATION_PATH, o.type());
    assertEquals(1, o.path().modules().size());
    assertEquals("Routing", o.path().modules().get(0).title());
    assertNotEquals(o.id(), Offering.certificationPath("X", path()).id());
  }

  @Test
  @DisplayName("a learning path defensively copies its modules (immutable)")
  void modulesAreImmutable() {
    List<Module> mutable = new ArrayList<>();
    mutable.add(Module.mandatory("M1", "c1"));
    LearningPath p = new LearningPath("P", mutable);
    mutable.add(Module.mandatory("M2", "c2")); // must not bleed into the path
    assertEquals(1, p.modules().size());
    assertThrows(UnsupportedOperationException.class,
        () -> p.modules().add(Module.mandatory("M3", "c3")));
  }

  @Test
  @DisplayName("a learning path needs at least one module")
  void emptyPathRejected() {
    assertThrows(IllegalArgumentException.class, () -> new LearningPath("P", List.of()));
  }

  @Test
  @DisplayName("null fields are rejected")
  void nullsRejected() {
    assertThrows(NullPointerException.class, () -> Module.mandatory(null, "c"));
    assertThrows(NullPointerException.class, () -> Offering.certificationPath(null, path()));
  }

  @Test
  @DisplayName("certificationPath() is PUBLIC with no gate data")
  void certificationPathIsPublic() {
    Offering o = Offering.certificationPath("Vaadin Certified", path());
    assertEquals(OfferingVisibility.PUBLIC, o.visibility());
    assertTrue(o.accessCodeOpt().isEmpty());
    assertTrue(o.prerequisiteOfferingIdOpt().isEmpty());
  }

  @Test
  @DisplayName("the four visibility factories set the right visibility + gate data")
  void visibilityFactories() {
    assertEquals(OfferingVisibility.REGISTERED,
        Offering.registeredPath("R", "d", path()).visibility());

    Offering coded = Offering.codePath("C", "d", path(), "SECRET-2026");
    assertEquals(OfferingVisibility.CODE, coded.visibility());
    assertEquals("SECRET-2026", coded.accessCodeOpt().orElseThrow());

    UUID prereq = UUID.randomUUID();
    Offering gated = Offering.prerequisitePath("P", "d", path(), prereq);
    assertEquals(OfferingVisibility.PREREQUISITE, gated.visibility());
    assertEquals(prereq, gated.prerequisiteOfferingIdOpt().orElseThrow());
  }

  @Test
  @DisplayName("a CODE offering needs a non-blank code; PREREQUISITE needs a prerequisite id")
  void gateDataIsValidated() {
    assertThrows(IllegalArgumentException.class,
        () -> Offering.codePath("C", "d", path(), "  "));
    assertThrows(IllegalArgumentException.class,
        () -> Offering.prerequisitePath("P", "d", path(), null));
  }

  @Test
  @DisplayName("module ids are stable per instance and distinct across modules (P006)")
  void moduleIdsAreStableAndDistinct() {
    Module m = Module.mandatory("Routing", "c");
    assertEquals(m.id(), m.id());
    assertNotEquals(m.id(), Module.mandatory("Routing", "c").id());
    assertTrue(m.mandatory());
    assertFalse(Module.optional("Extra", "c").mandatory());
  }

  @Test
  @DisplayName("a fresh offering is a DRAFT v1; withStatus publishes; asNewVersion preserves the old (P005)")
  void offeringLifecycleAndVersion() {
    Offering draft = Offering.publicPath("Course", "d", path());
    assertEquals(1, draft.version());
    assertEquals(com.svenruppert.openprobatum.content.ContentStatus.DRAFT, draft.status());
    assertEquals(draft.id(), draft.lineageId());
    assertFalse(draft.isPublished());

    Offering published = draft.withStatus(com.svenruppert.openprobatum.content.ContentStatus.PUBLISHED);
    assertTrue(published.isPublished());

    Offering v2 = published.asNewVersion();
    assertEquals(2, v2.version());
    assertEquals(published.lineageId(), v2.lineageId());
    assertNotEquals(published.id(), v2.id());
    // the published v1 is untouched (immutable)
    assertTrue(published.isPublished());
  }

  @Test
  @DisplayName("a path completes only when every mandatory module is done (optional never blocks)")
  void completionCriterion() {
    Module core = Module.mandatory("Core", "c");
    Module bonus = Module.optional("Bonus", "c");
    LearningPath p = new LearningPath("P", List.of(core, bonus));

    assertEquals(List.of(core), p.mandatoryModules());
    assertFalse(p.isComplete(Set.of()), "nothing done → not complete");
    assertFalse(p.isComplete(Set.of(bonus.id())), "only the optional done → not complete");
    assertTrue(p.isComplete(Set.of(core.id())), "the mandatory done → complete");
    assertTrue(p.isComplete(Set.of(core.id(), bonus.id())), "all done → complete");
  }
}
