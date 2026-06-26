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
import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QuestionBankService — lifecycle + reuse (P003)")
class QuestionBankServiceTest {

  private InMemoryQuestionRepository repo;
  private QuestionBankService bank;

  @BeforeEach
  void setUp() {
    repo = new InMemoryQuestionRepository();
    bank = new QuestionBankService(repo);
  }

  private Question complete() {
    return bank.create(Question.singleChoice("2+2?", List.of("3", "4"), 1, "Basic arithmetic.")
        .withMetadata("Add small integers", "Maths", Difficulty.EASY));
  }

  @Test
  @DisplayName("the create → tag → submit → approve → publish lifecycle works")
  void happyLifecycle() {
    Question q = complete();
    bank.tag(q.id(), Set.of("maths", "arithmetic"));
    assertEquals(Set.of("maths", "arithmetic"),
        repo.findById(q.id()).orElseThrow().tags());

    assertEquals(ContentStatus.IN_REVIEW, bank.submitForReview(q.id()).orElseThrow().status());
    assertEquals(ContentStatus.APPROVED, bank.approve(q.id()).orElseThrow().status());
    assertEquals(ContentStatus.PUBLISHED, bank.publish(q.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("approval is refused without a non-blank explanation + learning objective")
  void approvalNeedsDidactics() {
    // explanation present but no learning objective
    Question q = bank.create(Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic."));
    bank.submitForReview(q.id());
    assertThrows(IllegalStateException.class, () -> bank.approve(q.id()));
  }

  @Test
  @DisplayName("an illegal transition (Draft → Published) is rejected")
  void illegalTransitionRejected() {
    Question q = complete();
    assertThrows(IllegalStateException.class, () -> bank.publish(q.id()));
  }

  @Test
  @DisplayName("reusable() lists approved/published and excludes draft + archived")
  void reuseFilter() {
    Question draft = complete();                                   // DRAFT
    Question published = complete();
    bank.submitForReview(published.id());
    bank.approve(published.id());
    bank.publish(published.id());                                  // PUBLISHED
    Question archived = complete();
    bank.submitForReview(archived.id());
    bank.approve(archived.id());
    bank.publish(archived.id());
    bank.deprecate(archived.id());
    bank.archive(archived.id());                                   // ARCHIVED

    var reusableIds = bank.reusable().stream().map(Question::id).toList();
    assertTrue(reusableIds.contains(published.id()), "published is reusable");
    assertFalse(reusableIds.contains(draft.id()), "a draft is not reusable");
    assertFalse(reusableIds.contains(archived.id()), "an archived question is excluded");
  }
}
