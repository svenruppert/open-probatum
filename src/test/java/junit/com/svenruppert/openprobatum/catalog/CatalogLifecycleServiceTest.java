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

import com.svenruppert.openprobatum.catalog.CatalogLifecycleService;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogLifecycleService — offering review lifecycle (P006)")
class CatalogLifecycleServiceTest {

  private static LearningPath path() {
    return new LearningPath("P", List.of(Module.mandatory("M", "c")));
  }

  private static Offering draftIn(InMemoryCatalogRepository repo) {
    Offering o = Offering.publicPath("Course", "d", path());
    repo.save(o);
    return o;
  }

  @Test
  @DisplayName("submit → approve → publish walks the lifecycle and keeps the same lineage")
  void happyPath() {
    InMemoryCatalogRepository repo = new InMemoryCatalogRepository();
    CatalogLifecycleService service = new CatalogLifecycleService(repo);
    Offering draft = draftIn(repo);

    service.submitForReview(draft.id());
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(draft.id()).orElseThrow().status());

    service.approve(draft.id());
    assertEquals(ContentStatus.APPROVED, repo.findById(draft.id()).orElseThrow().status());

    Offering published = service.publish(draft.id()).orElseThrow();
    assertTrue(published.isPublished());
    assertEquals(draft.lineageId(), published.lineageId());
    assertEquals(draft.version(), published.version(), "a status change is not a new version");
  }

  @Test
  @DisplayName("pendingReview lists IN_REVIEW + APPROVED, never DRAFT or PUBLISHED")
  void pendingReviewFiltering() {
    InMemoryCatalogRepository repo = new InMemoryCatalogRepository();
    CatalogLifecycleService service = new CatalogLifecycleService(repo);
    Offering inReview = draftIn(repo);
    service.submitForReview(inReview.id());
    draftIn(repo); // a DRAFT — must not appear

    List<Offering> pending = service.pendingReview();
    assertEquals(1, pending.size());
    assertEquals(inReview.id(), pending.get(0).id());
  }

  @Test
  @DisplayName("an illegal transition (DRAFT → PUBLISHED) is refused")
  void illegalTransitionRefused() {
    InMemoryCatalogRepository repo = new InMemoryCatalogRepository();
    CatalogLifecycleService service = new CatalogLifecycleService(repo);
    Offering draft = draftIn(repo);
    assertThrows(IllegalStateException.class,
        () -> service.publish(draft.id()));
  }

  @Test
  @DisplayName("transitioning an unknown id yields an empty result, not a crash")
  void unknownId() {
    CatalogLifecycleService service = new CatalogLifecycleService(new InMemoryCatalogRepository());
    assertTrue(service.submitForReview(UUID.randomUUID()).isEmpty());
  }
}
