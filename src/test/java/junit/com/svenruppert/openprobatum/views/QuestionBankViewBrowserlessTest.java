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
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.views.QuestionBankView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QuestionBankView — authoring + submit-for-review (P003)")
class QuestionBankViewBrowserlessTest extends BrowserlessTest {

  private InMemoryQuestionRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryQuestionRepository();
    QuestionRepositoryProvider.setRepository(repo);
  }

  @AfterEach
  void tearDown() {
    QuestionRepositoryProvider.reset();
  }

  @Test
  @DisplayName("creating a question stores a DRAFT and lists it")
  void createsDraft() throws Exception {
    QuestionBankView view = new QuestionBankView();
    text(view, "text", "What is 2+2?");
    text(view, "options", "3, 4, 5");
    integer(view, "correct", 1);
    text(view, "explanation", "Basic arithmetic.");
    text(view, "objective", "Add small integers");
    text(view, "tags", "maths, arithmetic");
    create(view);

    assertEquals(1, repo.all().size());
    Question q = repo.all().iterator().next();
    assertEquals("What is 2+2?", q.text());
    assertEquals(ContentStatus.DRAFT, q.status());
    assertEquals(java.util.Set.of("maths", "arithmetic"), q.tags());
    assertEquals(List.of("DRAFT"), attributes(view, "data-status"));
  }

  @Test
  @DisplayName("an incomplete form saves nothing")
  void incompleteRejected() throws Exception {
    QuestionBankView view = new QuestionBankView();
    text(view, "text", "No options");
    create(view);
    assertTrue(repo.all().isEmpty());
  }

  @Test
  @DisplayName("submit-for-review moves the question to IN_REVIEW")
  void submitForReview() throws Exception {
    QuestionBankView view = new QuestionBankView();
    text(view, "text", "Q");
    text(view, "options", "a, b");
    integer(view, "correct", 0);
    text(view, "explanation", "e");
    text(view, "objective", "o");
    create(view);

    click(view, "submit");
    assertEquals(ContentStatus.IN_REVIEW, repo.all().iterator().next().status());
  }

  // ── reflection helpers ──────────────────────────────────────────

  private static void text(QuestionBankView v, String field, String value) throws Exception {
    Field f = QuestionBankView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((TextField) f.get(v)).setValue(value);
  }

  private static void integer(QuestionBankView v, String field, int value) throws Exception {
    Field f = QuestionBankView.class.getDeclaredField(field);
    f.setAccessible(true);
    ((IntegerField) f.get(v)).setValue(value);
  }

  private static void create(QuestionBankView v) throws Exception {
    Method m = QuestionBankView.class.getDeclaredMethod("create");
    m.setAccessible(true);
    m.invoke(v);
  }

  private static void click(Component root, String action) {
    List<Button> buttons = new ArrayList<>();
    collectButtons(root, action, buttons);
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
