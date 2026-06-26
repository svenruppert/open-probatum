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

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WorkshopService — lifecycle + segregation of duties (P007)")
class WorkshopServiceTest {

  private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");
  private static final Instant END = Instant.parse("2026-09-01T17:00:00Z");

  private InMemoryWorkshopRepository repo;
  private InMemoryContentAuthorship authorship;
  private WorkshopService service;

  @BeforeEach
  void setUp() {
    repo = new InMemoryWorkshopRepository();
    authorship = new InMemoryContentAuthorship();
    ContentAuthorshipProvider.setRegistry(authorship);
    service = new WorkshopService(repo);
  }

  @AfterEach
  void tearDown() {
    ContentAuthorshipProvider.reset();
  }

  private Workshop submitted() {
    Workshop w = Workshop.draft("Vaadin Day", "d", START, END, 10, "Sven")
        .withObjective("Master Vaadin");
    service.create(w);
    service.submitForReview(w.id());
    return w;
  }

  @Test
  @DisplayName("submit → approve → publish walks the lifecycle")
  void happyPath() {
    Workshop w = submitted();
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(w.id()).orElseThrow().status());
    service.approve(w.id());
    assertEquals(ContentStatus.APPROVED, repo.findById(w.id()).orElseThrow().status());
    Workshop published = service.publish(w.id()).orElseThrow();
    assertTrue(published.isPublished());
    assertEquals(1, service.published().size());
  }

  @Test
  @DisplayName("approval needs a non-blank learning objective")
  void approvalNeedsObjective() {
    Workshop bare = Workshop.draft("W", "d", START, END, 5, "Sven"); // no objective
    service.create(bare);
    service.submitForReview(bare.id());
    assertThrows(IllegalStateException.class, () -> service.approve(bare.id()));
  }

  @Test
  @DisplayName("an illegal transition (DRAFT → PUBLISHED) is refused")
  void illegalTransition() {
    Workshop w = Workshop.draft("W", "d", START, END, 5, "Sven");
    service.create(w);
    assertThrows(IllegalStateException.class, () -> service.publish(w.id()));
  }

  @Test
  @DisplayName("an author cannot approve their own workshop; a different reviewer can")
  void selfApprovalRefused() {
    Workshop w = submitted();
    authorship.recordAuthor(w.lineageId(), 1001L);

    assertThrows(IllegalStateException.class, () -> service.approve(w.id(), 1001L));
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(w.id()).orElseThrow().status());

    service.approve(w.id(), 2002L);
    assertEquals(ContentStatus.APPROVED, repo.findById(w.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("pendingReview lists IN_REVIEW + APPROVED; published lists only PUBLISHED")
  void filters() {
    Workshop inReview = submitted();
    service.create(Workshop.draft("D", "x", START, END, 5, "Sven")); // DRAFT

    assertEquals(1, service.pendingReview().size());
    assertEquals(inReview.id(), service.pendingReview().get(0).id());
    assertTrue(service.published().isEmpty());
  }
}
