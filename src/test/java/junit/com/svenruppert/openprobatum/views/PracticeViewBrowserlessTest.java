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

import com.svenruppert.openprobatum.assessment.Assessment;
import com.svenruppert.openprobatum.assessment.AssessmentRepositoryProvider;
import com.svenruppert.openprobatum.assessment.InMemoryAssessmentRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.views.PracticeView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PracticeView — practice mode feedback, no credential (P010)")
class PracticeViewBrowserlessTest extends BrowserlessTest {

  private InMemoryAssessmentRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryAssessmentRepository();
    AssessmentRepositoryProvider.setRepository(repo);
  }

  @AfterEach
  void tearDown() {
    AssessmentRepositoryProvider.reset();
  }

  private Assessment singleQuestion() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.");
    Assessment a = new Assessment(UUID.randomUUID(), "Quiz", 1, List.of(q), 0.5);
    repo.save(a);
    return a;
  }

  @Test
  @DisplayName("practice renders the no-credential banner + a block per question")
  void rendersPractice() {
    Assessment a = singleQuestion();
    PracticeView view = new PracticeView();
    view.setParameter(null, a.id().toString());

    assertEquals(List.of("MODE"), attributes(view, "data-practice"));
    assertEquals(1, attributes(view, "data-question").size());
  }

  @Test
  @DisplayName("checking the right answer shows CORRECT feedback (and no credential is issued)")
  void correctAnswerFeedback() {
    Assessment a = singleQuestion();
    PracticeView view = new PracticeView();
    view.setParameter(null, a.id().toString());

    // Single-choice questions render as a radio group (P021).
    @SuppressWarnings("unchecked")
    RadioButtonGroup<Integer> group = first(view, RadioButtonGroup.class);
    group.setValue(1); // the correct option
    first(view, Button.class).click();

    assertEquals(List.of("CORRECT"), attributes(view, "data-feedback"));
  }

  @Test
  @DisplayName("an unknown assessment id renders the unknown marker")
  void unknownAssessment() {
    PracticeView view = new PracticeView();
    view.setParameter(null, UUID.randomUUID().toString());
    assertEquals(List.of("UNKNOWN"), attributes(view, "data-practice-result"));
  }

  @SuppressWarnings("unchecked")
  private static <T extends Component> T first(Component root, Class<T> type) {
    List<Component> hits = new ArrayList<>();
    collectType(root, type, hits);
    return (T) hits.get(0);
  }

  private static void collectType(Component c, Class<?> type, List<Component> out) {
    if (type.isInstance(c)) {
      out.add(c);
    }
    c.getChildren().forEach(child -> collectType(child, type, out));
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
