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

import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionFeedback;
import com.svenruppert.openprobatum.assessment.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Question — type factories + practice feedback (P010)")
class QuestionFeedbackTest {

  @Test
  @DisplayName("single-choice feedback is correct only for the one right index")
  void singleChoiceFeedback() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4", "5"), 1, "Basic arithmetic.");
    assertEquals(QuestionType.SINGLE_CHOICE, q.type());

    QuestionFeedback ok = q.feedback(Set.of(1));
    assertTrue(ok.correct());
    assertEquals("Basic arithmetic.", ok.explanation());

    assertFalse(q.feedback(Set.of(0)).correct());
    assertFalse(q.feedback(Set.of()).correct());
  }

  @Test
  @DisplayName("multiple-choice feedback requires exactly the correct set")
  void multipleChoiceFeedback() {
    Question q = Question.multipleChoice("Even numbers?", List.of("1", "2", "3", "4"),
        Set.of(1, 3), "2 and 4 are even.");
    assertEquals(QuestionType.MULTIPLE_CHOICE, q.type());

    assertTrue(q.feedback(Set.of(1, 3)).correct());
    assertFalse(q.feedback(Set.of(1)).correct(), "missing one correct → wrong");
    assertFalse(q.feedback(Set.of(1, 2, 3)).correct(), "an extra pick → wrong");
  }

  @Test
  @DisplayName("true/false feedback maps the boolean to the right option")
  void trueFalseFeedback() {
    Question t = Question.trueFalse("The sky is blue.", true, "Rayleigh scattering.");
    assertEquals(QuestionType.TRUE_FALSE, t.type());
    assertTrue(t.feedback(Set.of(0)).correct());   // index 0 = True
    assertFalse(t.feedback(Set.of(1)).correct());

    Question f = Question.trueFalse("2+2 = 5.", false, "");
    assertTrue(f.feedback(Set.of(1)).correct());   // index 1 = False
  }
}
