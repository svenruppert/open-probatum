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

package junit.com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionBankService;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.catalog.CatalogLifecycleService;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.views.ReviewView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReviewView — the reviewer queue: approve / reject / publish (P006)")
class ReviewViewBrowserlessTest extends BrowserlessTest {

  private InMemoryQuestionRepository questions;
  private InMemoryCatalogRepository catalog;
  private com.svenruppert.openprobatum.lab.InMemoryLabRepository labs;
  private com.svenruppert.openprobatum.bundle.InMemoryBundleRepository bundles;
  private com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository workshops;
  private com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository coaching;

  @BeforeEach
  void setUp() {
    questions = new InMemoryQuestionRepository();
    catalog = new InMemoryCatalogRepository();
    labs = new com.svenruppert.openprobatum.lab.InMemoryLabRepository();
    bundles = new com.svenruppert.openprobatum.bundle.InMemoryBundleRepository();
    workshops = new com.svenruppert.openprobatum.workshop.InMemoryWorkshopRepository();
    coaching = new com.svenruppert.openprobatum.coaching.InMemoryCoachingOfferRepository();
    QuestionRepositoryProvider.setRepository(questions);
    CatalogRepositoryProvider.setRepository(catalog);
    com.svenruppert.openprobatum.lab.LabRepositoryProvider.setRepository(labs);
    com.svenruppert.openprobatum.bundle.BundleRepositoryProvider.setRepository(bundles);
    com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider.setRepository(workshops);
    com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider.setRepository(coaching);
  }

  @AfterEach
  void tearDown() {
    QuestionRepositoryProvider.reset();
    CatalogRepositoryProvider.reset();
    com.svenruppert.openprobatum.lab.LabRepositoryProvider.reset();
    com.svenruppert.openprobatum.bundle.BundleRepositoryProvider.reset();
    com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider.reset();
    com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider.reset();
  }

  private static LearningPath path() {
    return new LearningPath("P", List.of(Module.mandatory("M", "c")));
  }

  /** A question an author has created + submitted for review. */
  private Question submittedQuestion() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.")
        .withMetadata("Add integers", "maths", com.svenruppert.openprobatum.assessment.Difficulty.EASY);
    QuestionBankService bank = new QuestionBankService();
    bank.create(q);
    bank.submitForReview(q.id());
    return q;
  }

  @Test
  @DisplayName("an empty queue shows the empty state")
  void emptyQueue() {
    assertTrue(attributes(new ReviewView(), "data-question").isEmpty());
    assertTrue(attributes(new ReviewView(), "data-offering").isEmpty());
  }

  @Test
  @DisplayName("a reviewer approves a submitted question, then publishes it")
  void approveThenPublishQuestion() {
    Question q = submittedQuestion();

    // The IN_REVIEW question is in the queue with an approve action.
    ReviewView view = new ReviewView();
    assertEquals(List.of(q.id().toString()), attributes(view, "data-question"));
    click(view, "approve");
    assertEquals(ContentStatus.APPROVED, questions.findById(q.id()).orElseThrow().status());

    // Re-render: an approved item offers publish.
    ReviewView afterApprove = new ReviewView();
    click(afterApprove, "publish");
    assertEquals(ContentStatus.PUBLISHED, questions.findById(q.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a reviewer rejects a submitted question back to the author (DRAFT)")
  void rejectQuestion() {
    Question q = submittedQuestion();
    click(new ReviewView(), "reject");
    assertEquals(ContentStatus.DRAFT, questions.findById(q.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a stale publish (already moved on by another reviewer) shows a notice, not a crash (M-2)")
  void stalePublishIsGuarded() {
    Question q = submittedQuestion();
    QuestionBankService bank = new QuestionBankService();
    bank.approve(q.id());

    // The reviewer's queue is rendered while the item is APPROVED (publish shown).
    ReviewView view = new ReviewView();
    // Another reviewer publishes it first → the card is now stale.
    bank.publish(q.id());

    // Clicking the stale Publish button must not throw; it shows a STALE notice.
    click(view, "publish");
    assertEquals(List.of("STALE"), attributes(view, "data-error"));
    assertEquals(ContentStatus.PUBLISHED, questions.findById(q.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a submitted lab appears in the queue and a reviewer approves it (P003)")
  void approveSubmittedLab() {
    com.svenruppert.openprobatum.lab.Lab lab =
        com.svenruppert.openprobatum.lab.Lab.draft("Deploy", "Deploy the app")
            .withMetadata("Master deploy", com.svenruppert.openprobatum.assessment.Difficulty.HARD,
                "WAR boots");
    com.svenruppert.openprobatum.lab.LabService labService =
        new com.svenruppert.openprobatum.lab.LabService();
    labService.create(lab);
    labService.submitForReview(lab.id());

    ReviewView view = new ReviewView();
    assertEquals(List.of(lab.id().toString()), attributes(view, "data-lab"));
    click(view, "approve");
    assertEquals(ContentStatus.APPROVED, labs.findById(lab.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a submitted bundle appears in the queue and a reviewer approves it (P003)")
  void approveSubmittedBundle() {
    com.svenruppert.openprobatum.bundle.Bundle bundle =
        com.svenruppert.openprobatum.bundle.Bundle.draft("Pack", "d",
            java.util.Set.of(java.util.UUID.randomUUID()));
    com.svenruppert.openprobatum.bundle.BundleService bundleService =
        new com.svenruppert.openprobatum.bundle.BundleService();
    bundleService.create(bundle);
    bundleService.submitForReview(bundle.id());

    ReviewView view = new ReviewView();
    assertEquals(List.of(bundle.id().toString()), attributes(view, "data-bundle"));
    click(view, "approve");
    assertEquals(ContentStatus.APPROVED, bundles.findById(bundle.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a submitted workshop appears in the queue and a reviewer approves it (P007)")
  void approveSubmittedWorkshop() {
    com.svenruppert.openprobatum.workshop.Workshop workshop =
        com.svenruppert.openprobatum.workshop.Workshop.draft("Vaadin Day", "d",
            java.time.Instant.parse("2026-09-01T09:00:00Z"),
            java.time.Instant.parse("2026-09-01T17:00:00Z"), 10, "Sven")
            .withObjective("Master Vaadin");
    com.svenruppert.openprobatum.workshop.WorkshopService workshopService =
        new com.svenruppert.openprobatum.workshop.WorkshopService();
    workshopService.create(workshop);
    workshopService.submitForReview(workshop.id());

    ReviewView view = new ReviewView();
    assertEquals(List.of(workshop.id().toString()), attributes(view, "data-workshop"));
    click(view, "approve");
    assertEquals(ContentStatus.APPROVED, workshops.findById(workshop.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a submitted coaching offer appears in the queue and a reviewer approves it (P003)")
  void approveSubmittedCoachingOffer() {
    com.svenruppert.openprobatum.coaching.CoachingOffer offer =
        com.svenruppert.openprobatum.coaching.CoachingOffer.draft("Mentoring", "d", "Sven", 7L, 60)
            .withObjective("Grow as a lead");
    com.svenruppert.openprobatum.coaching.CoachingOfferService offerService =
        new com.svenruppert.openprobatum.coaching.CoachingOfferService();
    offerService.create(offer);
    offerService.submitForReview(offer.id());

    ReviewView view = new ReviewView();
    assertEquals(List.of(offer.id().toString()), attributes(view, "data-offer"));
    click(view, "approve");
    assertEquals(ContentStatus.APPROVED, coaching.findById(offer.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("a reviewer approves + publishes a submitted offering; it becomes learner-visible")
  void publishOffering() {
    Offering o = Offering.publicPath("Course", "d", path());
    catalog.save(o);
    new CatalogLifecycleService().submitForReview(o.id());

    ReviewView view = new ReviewView();
    assertEquals(List.of(o.id().toString()), attributes(view, "data-offering"));
    click(view, "approve");
    click(new ReviewView(), "publish");

    Offering published = catalog.findById(o.id()).orElseThrow();
    assertTrue(published.isPublished(), "the offering is now PUBLISHED");
    assertFalse(published.id().equals(null));
  }

  @Test
  @DisplayName("each card shows the reviewable content the reviewer judges (P009a)")
  void rendersReviewableContent() {
    // Question: options + the correct answer marked.
    submittedQuestion();
    // Offering with a named module.
    Offering offering = Offering.publicPath("Course", "d",
        new LearningPath("P", List.of(Module.mandatory("Routing", "how @Route works"))));
    catalog.save(offering);
    new CatalogLifecycleService().submitForReview(offering.id());
    // Lab with acceptance criteria.
    com.svenruppert.openprobatum.lab.Lab lab =
        com.svenruppert.openprobatum.lab.Lab.draft("Deploy", "Deploy the app")
            .withMetadata("Master deploy", com.svenruppert.openprobatum.assessment.Difficulty.HARD,
                "WAR boots cleanly");
    com.svenruppert.openprobatum.lab.LabService labService =
        new com.svenruppert.openprobatum.lab.LabService();
    labService.create(lab);
    labService.submitForReview(lab.id());
    // Bundle referencing a member offering (only saved, not itself in review).
    Offering member = Offering.publicPath("MemberCourse", "d", path());
    catalog.save(member);
    com.svenruppert.openprobatum.bundle.Bundle bundle =
        com.svenruppert.openprobatum.bundle.Bundle.draft("Pack", "a pack", java.util.Set.of(member.id()));
    com.svenruppert.openprobatum.bundle.BundleService bundleService =
        new com.svenruppert.openprobatum.bundle.BundleService();
    bundleService.create(bundle);
    bundleService.submitForReview(bundle.id());

    ReviewView view = new ReviewView();

    List<String> options = attributes(view, "data-detail-option");
    assertTrue(options.contains("3") && options.contains("4"), "question options shown");
    assertEquals(List.of("4"), attributes(view, "data-detail-correct"), "correct answer marked");
    assertTrue(attributes(view, "data-detail-module").contains("Routing"), "offering modules shown");
    assertTrue(attributes(view, "data-detail-acceptance").contains("WAR boots cleanly"),
        "lab acceptance criteria shown");
    assertTrue(attributes(view, "data-detail-member").contains("MemberCourse"),
        "bundle member title resolved + shown");
  }

  // ── tree-walk helpers ───────────────────────────────────────────

  private static void click(Component root, String action) {
    List<Button> buttons = new ArrayList<>();
    collectButtons(root, action, buttons);
    assertFalse(buttons.isEmpty(), "expected a '" + action + "' button in the queue");
    buttons.get(0).click();
  }

  private static void collectButtons(Component c, String action, List<Button> out) {
    if (c instanceof Button b && action.equals(b.getElement().getAttribute("data-action"))) {
      out.add(b);
    }
    c.getChildren().forEach(child -> collectButtons(child, action, out));
  }

  private static List<String> attributes(Component root, String name) {
    List<String> values = new ArrayList<>();
    collect(root, name, values);
    return values;
  }

  private static void collect(Component c, String name, List<String> out) {
    String v = c.getElement().getAttribute(name);
    if (v != null) {
      out.add(v);
    }
    c.getChildren().forEach(child -> collect(child, name, out));
  }
}
