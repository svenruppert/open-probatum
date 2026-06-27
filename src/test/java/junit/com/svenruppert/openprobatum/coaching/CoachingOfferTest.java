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

package junit.com.svenruppert.openprobatum.coaching;

import com.svenruppert.openprobatum.coaching.CoachingOffer;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository;
import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CoachingOffer — versioned 1:1 coaching content object (P002)")
class CoachingOfferTest {

  private static CoachingOffer draft() {
    return CoachingOffer.draft("Career mentoring", "1:1 guidance", "Sven", 7L, 60);
  }

  @Test
  @DisplayName("a fresh offer is a DRAFT v1 with its coach + duration")
  void freshOffer() {
    CoachingOffer o = draft();
    assertEquals(1, o.version());
    assertEquals(ContentStatus.DRAFT, o.status());
    assertEquals(o.id(), o.lineageId());
    assertEquals("Sven", o.coachName());
    assertEquals(7L, o.coachId());
    assertEquals(60, o.durationMinutes());
    assertFalse(o.isPublished());
  }

  @Test
  @DisplayName("withObjective + withTags + withStatus set fields without changing identity")
  void withers() {
    CoachingOffer o = draft().withObjective("Grow as a tech lead")
        .withTags(java.util.Set.of("career"))
        .withStatus(ContentStatus.PUBLISHED);
    assertEquals("Grow as a tech lead", o.learningObjective());
    assertEquals(java.util.Set.of("career"), o.tags());
    assertTrue(o.isPublished());
  }

  @Test
  @DisplayName("asNewVersion keeps the lineage but is a new immutable record")
  void versioning() {
    CoachingOffer v1 = draft().withStatus(ContentStatus.PUBLISHED);
    CoachingOffer v2 = v1.asNewVersion();
    assertEquals(2, v2.version());
    assertEquals(v1.lineageId(), v2.lineageId());
    assertNotEquals(v1.id(), v2.id());
    assertEquals(ContentStatus.DRAFT, v2.status());
    assertTrue(v1.isPublished(), "the old version is untouched");
  }

  @Test
  @DisplayName("duration must be >= 1 minute; a blank title is rejected")
  void validation() {
    assertThrows(IllegalArgumentException.class,
        () -> CoachingOffer.draft("W", "d", "Sven", 7L, 0));
    assertThrows(IllegalArgumentException.class,
        () -> CoachingOffer.draft("  ", "d", "Sven", 7L, 60));
  }

  @Test
  @DisplayName("the repository persists by id + resolves versions by lineage")
  void repository() {
    InMemoryCoachingOfferRepository repo = new InMemoryCoachingOfferRepository();
    CoachingOffer v1 = draft();
    CoachingOffer v2 = v1.asNewVersion();
    repo.save(v1);
    repo.save(v2);

    assertEquals(v1, repo.findById(v1.id()).orElseThrow());
    assertEquals(2, repo.versionsOf(v1.lineageId()).size());
    assertEquals(v2, repo.latestOf(v1.lineageId()).orElseThrow());
  }
}
