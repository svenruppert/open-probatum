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
import com.svenruppert.openprobatum.coaching.CoachingOfferService;
import com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CoachingOfferService — lifecycle + segregation of duties (P003)")
class CoachingOfferServiceTest {

  private InMemoryCoachingOfferRepository repo;
  private InMemoryContentAuthorship authorship;
  private CoachingOfferService service;

  @BeforeEach
  void setUp() {
    repo = new InMemoryCoachingOfferRepository();
    authorship = new InMemoryContentAuthorship();
    ContentAuthorshipProvider.setRegistry(authorship);
    service = new CoachingOfferService(repo);
  }

  @AfterEach
  void tearDown() {
    ContentAuthorshipProvider.reset();
  }

  private CoachingOffer submitted() {
    CoachingOffer o = CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60)
        .withObjective("Grow as a lead");
    service.create(o);
    service.submitForReview(o.id());
    return o;
  }

  @Test
  @DisplayName("submit → approve → publish walks the lifecycle")
  void happyPath() {
    CoachingOffer o = submitted();
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(o.id()).orElseThrow().status());
    service.approve(o.id());
    assertEquals(ContentStatus.APPROVED, repo.findById(o.id()).orElseThrow().status());
    CoachingOffer published = service.publish(o.id()).orElseThrow();
    assertTrue(published.isPublished());
    assertEquals(1, service.published().size());
  }

  @Test
  @DisplayName("approval needs a non-blank learning objective")
  void approvalNeedsObjective() {
    CoachingOffer bare = CoachingOffer.draft("O", "d", "Sven", 7L, 60); // no objective
    service.create(bare);
    service.submitForReview(bare.id());
    assertThrows(IllegalStateException.class, () -> service.approve(bare.id()));
  }

  @Test
  @DisplayName("an illegal transition (DRAFT → PUBLISHED) is refused")
  void illegalTransition() {
    CoachingOffer o = CoachingOffer.draft("O", "d", "Sven", 7L, 60);
    service.create(o);
    assertThrows(IllegalStateException.class, () -> service.publish(o.id()));
  }

  @Test
  @DisplayName("an author cannot approve their own offer; a different reviewer can")
  void selfApprovalRefused() {
    CoachingOffer o = submitted();
    authorship.recordAuthor(o.lineageId(), 1001L);

    assertThrows(IllegalStateException.class, () -> service.approve(o.id(), 1001L));
    assertEquals(ContentStatus.IN_REVIEW, repo.findById(o.id()).orElseThrow().status());

    service.approve(o.id(), 2002L);
    assertEquals(ContentStatus.APPROVED, repo.findById(o.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("pendingReview lists IN_REVIEW + APPROVED; published lists only PUBLISHED")
  void filters() {
    CoachingOffer inReview = submitted();
    service.create(CoachingOffer.draft("D", "x", "Sven", 7L, 30)); // DRAFT

    assertEquals(1, service.pendingReview().size());
    assertEquals(inReview.id(), service.pendingReview().get(0).id());
    assertTrue(service.published().isEmpty());
  }
}
