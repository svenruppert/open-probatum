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

package junit.com.svenruppert.openprobatum.lab;

import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.Lab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Lab — versioned practical content object (P002)")
class LabTest {

  @Test
  @DisplayName("a fresh lab is a DRAFT v1 with id == lineageId + sensible defaults")
  void freshLabIsDraftV1() {
    Lab lab = Lab.draft("Deploy a WAR", "Deploy the app to Jetty and capture the boot log.");
    assertEquals(1, lab.version());
    assertEquals(ContentStatus.DRAFT, lab.status());
    assertEquals(lab.id(), lab.lineageId());
    assertEquals(Difficulty.MEDIUM, lab.difficulty());
    assertEquals("", lab.learningObjective());
    assertEquals("", lab.acceptanceCriteria());
    assertTrue(lab.tags().isEmpty());
    assertFalse(lab.isPublished());
  }

  @Test
  @DisplayName("withMetadata + withTags + withStatus set fields without changing identity")
  void withers() {
    Lab lab = Lab.draft("L", "do it")
        .withMetadata("Master deployment", Difficulty.HARD, "WAR boots + serves /")
        .withTags(Set.of("ops", "jetty"))
        .withStatus(ContentStatus.PUBLISHED);

    assertEquals("Master deployment", lab.learningObjective());
    assertEquals(Difficulty.HARD, lab.difficulty());
    assertEquals("WAR boots + serves /", lab.acceptanceCriteria());
    assertEquals(Set.of("ops", "jetty"), lab.tags());
    assertTrue(lab.isPublished());
  }

  @Test
  @DisplayName("asNewVersion keeps the lineage but is a new immutable record; the old stays intact")
  void versioning() {
    Lab v1 = Lab.draft("L", "do it").withStatus(ContentStatus.PUBLISHED);
    Lab v2 = v1.asNewVersion();

    assertEquals(2, v2.version());
    assertEquals(v1.lineageId(), v2.lineageId());
    assertNotEquals(v1.id(), v2.id());
    assertEquals(ContentStatus.DRAFT, v2.status(), "a new version starts as a DRAFT");
    assertTrue(v1.isPublished(), "the old version is untouched");
  }

  @Test
  @DisplayName("tags are defensively copied; blank title/instructions are rejected")
  void validation() {
    java.util.Set<String> mutable = new java.util.HashSet<>(Set.of("a"));
    Lab lab = Lab.draft("L", "do it").withTags(mutable);
    mutable.add("b");
    assertEquals(Set.of("a"), lab.tags(), "the tag set is copied, not aliased");

    assertThrows(IllegalArgumentException.class, () -> Lab.draft("  ", "do it"));
    assertThrows(IllegalArgumentException.class, () -> Lab.draft("L", "  "));
  }

  @Test
  @DisplayName("the repository persists by id + resolves versions by lineage")
  void repository() {
    InMemoryLabRepository repo = new InMemoryLabRepository();
    Lab v1 = Lab.draft("L", "do it");
    Lab v2 = v1.asNewVersion();
    repo.save(v1);
    repo.save(v2);

    assertEquals(v1, repo.findById(v1.id()).orElseThrow());
    assertEquals(2, repo.versionsOf(v1.lineageId()).size());
    assertEquals(v2, repo.latestOf(v1.lineageId()).orElseThrow());
    assertEquals(1, repo.versionsOf(v1.lineageId()).get(0).version(), "lowest version first");
  }
}
