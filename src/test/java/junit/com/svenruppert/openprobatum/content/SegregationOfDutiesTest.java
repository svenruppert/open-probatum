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

package junit.com.svenruppert.openprobatum.content;

import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionBankService;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.catalog.CatalogLifecycleService;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Segregation of duties — an author cannot approve their own content (P012, §3.6)")
class SegregationOfDutiesTest {

  private static final Long AUTHOR = 1001L;
  private static final Long REVIEWER = 2002L;

  private InMemoryContentAuthorship authorship;
  private InMemoryQuestionRepository questions;

  @BeforeEach
  void setUp() {
    authorship = new InMemoryContentAuthorship();
    ContentAuthorshipProvider.setRegistry(authorship);
    questions = new InMemoryQuestionRepository();
    QuestionRepositoryProvider.setRepository(questions);
  }

  @AfterEach
  void tearDown() {
    ContentAuthorshipProvider.reset();
    QuestionRepositoryProvider.reset();
  }

  private Question submittedQuestion(Long author) {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.")
        .withMetadata("Add integers", "maths",
            com.svenruppert.openprobatum.assessment.Difficulty.EASY);
    QuestionBankService bank = new QuestionBankService(questions);
    bank.create(q);
    authorship.recordAuthor(q.lineageId(), author);
    bank.submitForReview(q.id());
    return q;
  }

  @Test
  @DisplayName("the author of a question cannot approve it; a different reviewer can")
  void questionSelfApprovalRefused() {
    Question q = submittedQuestion(AUTHOR);
    QuestionBankService bank = new QuestionBankService(questions);

    assertThrows(IllegalStateException.class, () -> bank.approve(q.id(), AUTHOR),
        "the author must not approve their own question");
    assertEquals(ContentStatus.IN_REVIEW, questions.findById(q.id()).orElseThrow().status(),
        "the refused question stays IN_REVIEW");

    bank.approve(q.id(), REVIEWER);
    assertEquals(ContentStatus.APPROVED, questions.findById(q.id()).orElseThrow().status(),
        "a different reviewer approves it");
  }

  @Test
  @DisplayName("the author of an offering cannot approve it; a different reviewer can")
  void offeringSelfApprovalRefused() {
    InMemoryCatalogRepository catalog = new InMemoryCatalogRepository();
    CatalogLifecycleService lifecycle = new CatalogLifecycleService(catalog);
    Offering o = Offering.publicPath("Course", "d",
        new LearningPath("P", List.of(Module.mandatory("M", "c"))));
    catalog.save(o);
    authorship.recordAuthor(o.lineageId(), AUTHOR);
    lifecycle.submitForReview(o.id());

    assertThrows(IllegalStateException.class, () -> lifecycle.approve(o.id(), AUTHOR));
    assertEquals(ContentStatus.IN_REVIEW, catalog.findById(o.id()).orElseThrow().status());

    lifecycle.approve(o.id(), REVIEWER);
    assertEquals(ContentStatus.APPROVED, catalog.findById(o.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("authorship is recorded on the lineage, so a new version keeps the original author")
  void authorshipFollowsLineage() {
    Question v1 = Question.singleChoice("Q", List.of("a", "b"), 0, "e");
    authorship.recordAuthor(v1.lineageId(), AUTHOR);
    Question v2 = v1.asNewVersion();
    assertEquals(v1.lineageId(), v2.lineageId());
    // The author of the lineage is the same across versions.
    assertEquals(AUTHOR, authorship.authorOf(v2.lineageId()).orElseThrow());
  }
}
