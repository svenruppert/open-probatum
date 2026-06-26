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
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LabService — lifecycle + segregation of duties (P003)")
class LabServiceTest {

  private InMemoryLabRepository repo;
  private InMemoryContentAuthorship authorship;
  private LabService service;

  @BeforeEach
  void setUp() {
    repo = new InMemoryLabRepository();
    authorship = new InMemoryContentAuthorship();
    ContentAuthorshipProvider.setRegistry(authorship);
    service = new LabService(repo);
  }

  @AfterEach
  void tearDown() {
    ContentAuthorshipProvider.reset();
  }

  private Lab readyToApprove() {
    Lab lab = Lab.draft("Deploy", "Deploy the app").withMetadata("Master deploy",
        Difficulty.HARD, "WAR boots + serves /");
    service.create(lab);
    service.submitForReview(lab.id());
    return lab;
  }

  @Test
  @DisplayName("submit → approve → publish walks the lifecycle")
  void happyPath() {
    Lab lab = readyToApprove();
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(lab.id()).orElseThrow().status());
    service.approve(lab.id());
    assertEquals(ContentStatus.APPROVED, repo.findById(lab.id()).orElseThrow().status());
    Lab published = service.publish(lab.id()).orElseThrow();
    assertTrue(published.isPublished());
    assertEquals(1, service.published().size());
  }

  @Test
  @DisplayName("approval needs a non-blank learning objective + acceptance criteria")
  void approvalNeedsMetadata() {
    Lab bare = Lab.draft("L", "do it"); // no objective / acceptance
    service.create(bare);
    service.submitForReview(bare.id());
    assertThrows(IllegalStateException.class, () -> service.approve(bare.id()));
  }

  @Test
  @DisplayName("an illegal transition (DRAFT → PUBLISHED) is refused")
  void illegalTransition() {
    Lab lab = Lab.draft("L", "do it");
    service.create(lab);
    assertThrows(IllegalStateException.class, () -> service.publish(lab.id()));
  }

  @Test
  @DisplayName("an author cannot approve their own lab; a different reviewer can")
  void selfApprovalRefused() {
    Lab lab = readyToApprove();
    authorship.recordAuthor(lab.lineageId(), 1001L);

    assertThrows(IllegalStateException.class, () -> service.approve(lab.id(), 1001L));
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(lab.id()).orElseThrow().status());

    service.approve(lab.id(), 2002L);
    assertEquals(ContentStatus.APPROVED, repo.findById(lab.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("pendingReview lists IN_REVIEW + APPROVED; published lists only PUBLISHED")
  void filters() {
    Lab inReview = readyToApprove();
    Lab draft = Lab.draft("D", "x");
    service.create(draft); // DRAFT — not pending, not published

    assertEquals(1, service.pendingReview().size());
    assertEquals(inReview.id(), service.pendingReview().get(0).id());
    assertTrue(service.published().isEmpty());

    assertTrue(service.tag(UUID.randomUUID(), java.util.Set.of("x")).isEmpty(),
        "tagging an unknown id is a no-op");
  }
}
