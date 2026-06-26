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

import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.InMemoryBundleRepository;
import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Bundle — versioned package of offerings (P002)")
class BundleTest {

  private final UUID o1 = UUID.randomUUID();
  private final UUID o2 = UUID.randomUUID();

  @Test
  @DisplayName("a fresh bundle is a DRAFT v1 holding its member offerings")
  void freshBundle() {
    Bundle b = Bundle.draft("Vaadin Mastery", "Two courses", Set.of(o1, o2));
    assertEquals(1, b.version());
    assertEquals(ContentStatus.DRAFT, b.status());
    assertEquals(b.id(), b.lineageId());
    assertEquals(Set.of(o1, o2), b.offeringIds());
    assertTrue(b.contains(o1));
    assertFalse(b.isPublished());
  }

  @Test
  @DisplayName("withMembers + withTags + withStatus set fields without changing identity")
  void withers() {
    Bundle b = Bundle.draft("B", "d", Set.of(o1))
        .withMembers(Set.of(o1, o2))
        .withTags(Set.of("vaadin"))
        .withStatus(ContentStatus.PUBLISHED);
    assertEquals(Set.of(o1, o2), b.offeringIds());
    assertEquals(Set.of("vaadin"), b.tags());
    assertTrue(b.isPublished());
  }

  @Test
  @DisplayName("asNewVersion keeps the lineage but is a new immutable record")
  void versioning() {
    Bundle v1 = Bundle.draft("B", "d", Set.of(o1)).withStatus(ContentStatus.PUBLISHED);
    Bundle v2 = v1.asNewVersion();
    assertEquals(2, v2.version());
    assertEquals(v1.lineageId(), v2.lineageId());
    assertNotEquals(v1.id(), v2.id());
    assertEquals(ContentStatus.DRAFT, v2.status());
    assertTrue(v1.isPublished(), "the old version is untouched");
  }

  @Test
  @DisplayName("members + tags are defensively copied; a blank title is rejected")
  void validation() {
    java.util.Set<UUID> mutable = new java.util.HashSet<>(Set.of(o1));
    Bundle b = Bundle.draft("B", "d", mutable);
    mutable.add(o2);
    assertEquals(Set.of(o1), b.offeringIds(), "the member set is copied, not aliased");
    assertThrows(IllegalArgumentException.class, () -> Bundle.draft("  ", "d", Set.of(o1)));
  }

  @Test
  @DisplayName("the repository persists by id + resolves versions by lineage")
  void repository() {
    InMemoryBundleRepository repo = new InMemoryBundleRepository();
    Bundle v1 = Bundle.draft("B", "d", Set.of(o1));
    Bundle v2 = v1.asNewVersion();
    repo.save(v1);
    repo.save(v2);

    assertEquals(v1, repo.findById(v1.id()).orElseThrow());
    assertEquals(2, repo.versionsOf(v1.lineageId()).size());
    assertEquals(v2, repo.latestOf(v1.lineageId()).orElseThrow());
  }
}
