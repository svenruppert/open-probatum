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

package junit.com.svenruppert.openprobatum.assessment;

import com.svenruppert.openprobatum.assessment.Difficulty;
import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionBankService;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.content.ContentAuthorshipProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Segregation-of-duties on question approval (concept §3.6/§17.2): the check
 * compares the recorded author against the approver by their numeric user id. A
 * <em>different</em> reviewer may approve; only the author of the content is
 * refused — so the rule blocks self-approval, not cross-user review.
 */
@DisplayName("Question approval — segregation of duties is per user-id")
class QuestionApprovalSodTest {

  private static final Long AUTHOR = 1001L;
  private static final Long REVIEWER = 1002L;

  private QuestionBankService service;

  @BeforeEach
  void setUp() {
    QuestionRepositoryProvider.setRepository(new InMemoryQuestionRepository());
    ContentAuthorshipProvider.setRegistry(new InMemoryContentAuthorship());
    service = new QuestionBankService();
  }

  @AfterEach
  void tearDown() {
    QuestionRepositoryProvider.reset();
    ContentAuthorshipProvider.reset();
  }

  private Question submittedBy(Long authorId) {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.")
        .withMetadata("Add integers", "maths", Difficulty.EASY);
    service.create(q);
    ContentAuthorshipProvider.registry().recordAuthor(q.lineageId(), authorId);
    service.submitForReview(q.id());
    return q;
  }

  @Test
  @DisplayName("a DIFFERENT reviewer may approve a question another user authored")
  void reviewerApprovesOthersQuestion() {
    Question q = submittedBy(AUTHOR);
    assertDoesNotThrow(() -> service.approve(q.id(), REVIEWER));
    assertEquals(ContentStatus.APPROVED,
        QuestionRepositoryProvider.repository().findById(q.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("the author cannot approve their own question (same id → self-approval refused)")
  void authorCannotApproveOwn() {
    Question q = submittedBy(AUTHOR);
    assertThrows(IllegalStateException.class, () -> service.approve(q.id(), AUTHOR));
    assertEquals(ContentStatus.IN_REVIEW,
        QuestionRepositoryProvider.repository().findById(q.id()).orElseThrow().status(),
        "a refused self-approval leaves the question awaiting another reviewer");
  }
}
