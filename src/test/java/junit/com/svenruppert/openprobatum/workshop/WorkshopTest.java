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

package junit.com.svenruppert.openprobatum.workshop;

import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.Workshop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Workshop — scheduled, capacity-limited content object (P006)")
class WorkshopTest {

  private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");
  private static final Instant END = Instant.parse("2026-09-01T17:00:00Z");

  private static Workshop draft() {
    return Workshop.draft("Vaadin Day", "Hands-on", START, END, 12, "Sven");
  }

  @Test
  @DisplayName("a fresh workshop is a DRAFT v1 with its schedule, capacity + instructor")
  void freshWorkshop() {
    Workshop w = draft();
    assertEquals(1, w.version());
    assertEquals(ContentStatus.DRAFT, w.status());
    assertEquals(w.id(), w.lineageId());
    assertEquals(12, w.capacity());
    assertEquals("Sven", w.instructor());
    assertEquals(START, w.startsAt());
    assertFalse(w.isPublished());
  }

  @Test
  @DisplayName("withObjective + withTags + withStatus set fields without changing identity")
  void withers() {
    Workshop w = draft().withObjective("Master Vaadin")
        .withTags(java.util.Set.of("vaadin"))
        .withStatus(ContentStatus.PUBLISHED);
    assertEquals("Master Vaadin", w.learningObjective());
    assertEquals(java.util.Set.of("vaadin"), w.tags());
    assertTrue(w.isPublished());
  }

  @Test
  @DisplayName("asNewVersion keeps the lineage but is a new immutable record")
  void versioning() {
    Workshop v1 = draft().withStatus(ContentStatus.PUBLISHED);
    Workshop v2 = v1.asNewVersion();
    assertEquals(2, v2.version());
    assertEquals(v1.lineageId(), v2.lineageId());
    assertNotEquals(v1.id(), v2.id());
    assertEquals(ContentStatus.DRAFT, v2.status());
    assertTrue(v1.isPublished(), "the old version is untouched");
  }

  @Test
  @DisplayName("capacity must be >= 1 and the end must be after the start")
  void validation() {
    assertThrows(IllegalArgumentException.class,
        () -> Workshop.draft("W", "d", START, END, 0, "Sven"));
    assertThrows(IllegalArgumentException.class,
        () -> Workshop.draft("W", "d", END, START, 10, "Sven"));
    assertThrows(IllegalArgumentException.class,
        () -> Workshop.draft("  ", "d", START, END, 10, "Sven"));
  }

  @Test
  @DisplayName("the repository persists by id + resolves versions by lineage")
  void repository() {
    InMemoryWorkshopRepository repo = new InMemoryWorkshopRepository();
    Workshop v1 = draft();
    Workshop v2 = v1.asNewVersion();
    repo.save(v1);
    repo.save(v2);

    assertEquals(v1, repo.findById(v1.id()).orElseThrow());
    assertEquals(2, repo.versionsOf(v1.lineageId()).size());
    assertEquals(v2, repo.latestOf(v1.lineageId()).orElseThrow());
  }
}
