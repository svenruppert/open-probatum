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

import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import com.svenruppert.openprobatum.lab.InMemoryLabRepository;
import com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabSubmission;
import com.svenruppert.openprobatum.lab.LabSubmissionService;
import com.svenruppert.openprobatum.lab.SubmissionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Lab assessment — verify / reject with SoD + idempotency (P006)")
class LabAssessmentTest {

  private static final Long AUTHOR = 1001L;
  private static final Long ASSESSOR = 2002L;

  private InMemoryLabRepository labs;
  private InMemoryLabSubmissionRepository submissions;
  private InMemoryContentAuthorship authorship;
  private LabSubmissionService service;
  private Lab lab;

  @BeforeEach
  void setUp() {
    labs = new InMemoryLabRepository();
    submissions = new InMemoryLabSubmissionRepository();
    authorship = new InMemoryContentAuthorship();
    ContentAuthorshipProvider.setRegistry(authorship);
    service = new LabSubmissionService(labs, submissions);

    lab = Lab.draft("Deploy", "Deploy the app").withStatus(ContentStatus.PUBLISHED);
    labs.save(lab);
    authorship.recordAuthor(lab.lineageId(), AUTHOR);
  }

  @AfterEach
  void tearDown() {
    ContentAuthorshipProvider.reset();
  }

  private LabSubmission submitted() {
    LabSubmission s = service.submit(lab.id(), 5005L, "Ada", "did it", null).orElseThrow();
    return s;
  }

  @Test
  @DisplayName("an assessor verifies a submitted submission → VERIFIED")
  void verify() {
    LabSubmission s = submitted();
    LabSubmission verified = service.verify(s.id(), ASSESSOR, "Great").orElseThrow();
    assertEquals(SubmissionStatus.VERIFIED, verified.status());
    assertEquals("Great", verified.assessorFeedback());
    assertEquals(SubmissionStatus.VERIFIED, submissions.findById(s.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("reject carries feedback; blank feedback is refused")
  void reject() {
    LabSubmission s = submitted();
    LabSubmission rejected = service.reject(s.id(), ASSESSOR, "Missing the log").orElseThrow();
    assertEquals(SubmissionStatus.REJECTED, rejected.status());
    assertEquals("Missing the log", rejected.assessorFeedback());

    LabSubmission s2 = submitted();
    assertThrows(IllegalArgumentException.class, () -> service.reject(s2.id(), ASSESSOR, "  "));
  }

  @Test
  @DisplayName("the lab's author cannot assess submissions to their own lab (SoD)")
  void selfAssessmentRefused() {
    LabSubmission s = submitted();
    assertThrows(IllegalStateException.class, () -> service.verify(s.id(), AUTHOR, "ok"));
    assertEquals(SubmissionStatus.SUBMITTED, submissions.findById(s.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("re-deciding an already-decided submission is a no-op (idempotent edge)")
  void idempotent() {
    LabSubmission s = submitted();
    service.verify(s.id(), ASSESSOR, "ok");
    // A second verdict on the now-VERIFIED submission does nothing.
    assertTrue(service.verify(s.id(), ASSESSOR, "again").isEmpty());
    assertTrue(service.reject(s.id(), ASSESSOR, "no").isEmpty());
    assertEquals(SubmissionStatus.VERIFIED, submissions.findById(s.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("concurrent assessors verifying the same submission win the edge exactly once (H1)")
  void concurrentVerifyFiresOnce() throws InterruptedException {
    LabSubmission s = submitted();
    int threads = 16;
    var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
    var start = new java.util.concurrent.CountDownLatch(1);
    var done = new java.util.concurrent.CountDownLatch(threads);
    var wins = new java.util.concurrent.atomic.AtomicInteger();
    for (int i = 0; i < threads; i++) {
      pool.execute(() -> {
        try {
          start.await();
          // Each thread uses its own service instance, as the view does per click.
          if (new LabSubmissionService(labs, submissions).verify(s.id(), ASSESSOR, "ok").isPresent()) {
            wins.incrementAndGet();
          }
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      });
    }
    start.countDown();
    assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS), "all threads finished");
    pool.shutdownNow();

    assertEquals(1, wins.get(), "exactly one assessor wins the SUBMITTED→VERIFIED edge");
    assertEquals(SubmissionStatus.VERIFIED, submissions.findById(s.id()).orElseThrow().status());
  }
}
