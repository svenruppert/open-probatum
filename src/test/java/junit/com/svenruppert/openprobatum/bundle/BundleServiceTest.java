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
import com.svenruppert.openprobatum.bundle.BundleService;
import com.svenruppert.openprobatum.bundle.InMemoryBundleRepository;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BundleService — lifecycle + segregation of duties (P003)")
class BundleServiceTest {

  private InMemoryBundleRepository repo;
  private InMemoryContentAuthorship authorship;
  private BundleService service;

  @BeforeEach
  void setUp() {
    repo = new InMemoryBundleRepository();
    authorship = new InMemoryContentAuthorship();
    ContentAuthorshipProvider.setRegistry(authorship);
    service = new BundleService(repo);
  }

  @AfterEach
  void tearDown() {
    ContentAuthorshipProvider.reset();
  }

  private Bundle submitted() {
    Bundle b = Bundle.draft("Mastery", "two courses", Set.of(UUID.randomUUID()));
    service.create(b);
    service.submitForReview(b.id());
    return b;
  }

  @Test
  @DisplayName("submit → approve → publish walks the lifecycle")
  void happyPath() {
    Bundle b = submitted();
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(b.id()).orElseThrow().status());
    service.approve(b.id());
    assertEquals(ContentStatus.APPROVED, repo.findById(b.id()).orElseThrow().status());
    Bundle published = service.publish(b.id()).orElseThrow();
    assertTrue(published.isPublished());
    assertEquals(1, service.published().size());
  }

  @Test
  @DisplayName("approval needs at least one member offering")
  void approvalNeedsMember() {
    Bundle empty = new Bundle(UUID.randomUUID(), UUID.randomUUID(), 1, ContentStatus.DRAFT,
        "Empty", "", Set.of(), Set.of());
    service.create(empty);
    service.submitForReview(empty.id());
    assertThrows(IllegalStateException.class, () -> service.approve(empty.id()));
  }

  @Test
  @DisplayName("an illegal transition (DRAFT → PUBLISHED) is refused")
  void illegalTransition() {
    Bundle b = Bundle.draft("B", "d", Set.of(UUID.randomUUID()));
    service.create(b);
    assertThrows(IllegalStateException.class, () -> service.publish(b.id()));
  }

  @Test
  @DisplayName("an author cannot approve their own bundle; a different reviewer can")
  void selfApprovalRefused() {
    Bundle b = submitted();
    authorship.recordAuthor(b.lineageId(), 1001L);

    assertThrows(IllegalStateException.class, () -> service.approve(b.id(), 1001L));
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(b.id()).orElseThrow().status());

    service.approve(b.id(), 2002L);
    assertEquals(ContentStatus.APPROVED, repo.findById(b.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("pendingReview lists IN_REVIEW + APPROVED; published lists only PUBLISHED")
  void filters() {
    Bundle inReview = submitted();
    service.create(Bundle.draft("D", "x", Set.of(UUID.randomUUID()))); // DRAFT

    assertEquals(1, service.pendingReview().size());
    assertEquals(inReview.id(), service.pendingReview().get(0).id());
    assertTrue(service.published().isEmpty());
  }
}
