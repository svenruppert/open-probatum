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
import com.svenruppert.openprobatum.assessment.AttemptRepositoryProvider;
import com.svenruppert.openprobatum.assessment.InMemoryAssessmentRepository;
import com.svenruppert.openprobatum.assessment.InMemoryAttemptRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.EffectiveStatus;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.security.AppClock;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.CheckView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CheckView — graded completion check + issuance (P011/P012)")
class CheckViewBrowserlessTest extends BrowserlessTest {

  private InMemoryAssessmentRepository assessments;
  private InMemoryCredentialRepository credentials;

  @BeforeEach
  void setUp() {
    assessments = new InMemoryAssessmentRepository();
    credentials = new InMemoryCredentialRepository();
    AssessmentRepositoryProvider.setRepository(assessments);
    AttemptRepositoryProvider.setRepository(new InMemoryAttemptRepository());
    CredentialRepositoryProvider.setRepository(credentials);
    // Keep the issuance audit-trail side effect in memory (no file store).
    com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider.setRepository(
        new com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository());
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1001L, "Alice", EnumSet.of(AuthorizationRole.LEARNER)), AppUser.class);
  }

  @AfterEach
  void tearDown() {
    AssessmentRepositoryProvider.reset();
    AttemptRepositoryProvider.reset();
    CredentialRepositoryProvider.reset();
    com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  private Assessment oneQuestion() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1);
    Assessment a = new Assessment(UUID.randomUUID(), "Quiz", 1, List.of(q), 1.0);
    assessments.save(a);
    return a;
  }

  @Test
  @DisplayName("a correct submission passes and counts as attempt 1")
  void correctSubmissionPasses() {
    Assessment a = oneQuestion();
    CheckView view = new CheckView();
    view.setParameter(null, a.id().toString());

    first(view, CheckboxGroup.class).select(1); // correct
    first(view, Button.class).click();

    assertEquals(List.of("PASSED"), attributes(view, "data-check-result"));
    assertEquals(List.of("1"), attributes(view, "data-attempt"));

    // P012: a passing check mints exactly one VALID credential.
    assertEquals(1, credentials.all().size());
    Credential issued = credentials.all().iterator().next();
    assertEquals("Quiz", issued.title());
    assertEquals(EffectiveStatus.VALID, issued.effectiveStatusAt(AppClock.now()));
    // P008: the credential is bound to the signed-in learner's stable id, not just the name.
    assertEquals(1001L, issued.recipientId(), "the credential carries the learner's id");
    assertTrue(issued.isHeldBy(1001L));
  }

  @Test
  @DisplayName("a wrong submission fails, counts as an attempt, and issues no credential")
  void wrongSubmissionFails() {
    Assessment a = oneQuestion();
    CheckView view = new CheckView();
    view.setParameter(null, a.id().toString());

    first(view, CheckboxGroup.class).select(0); // wrong
    first(view, Button.class).click();

    assertEquals(List.of("FAILED"), attributes(view, "data-check-result"));
    assertEquals(List.of("1"), attributes(view, "data-attempt"));
    assertTrue(credentials.all().isEmpty(), "a failed check issues no credential");
  }

  @Test
  @DisplayName("passing twice does not mint a second credential")
  void secondPassDoesNotDuplicate() {
    Assessment a = oneQuestion();

    CheckView firstView = new CheckView();
    firstView.setParameter(null, a.id().toString());
    first(firstView, CheckboxGroup.class).select(1);
    first(firstView, Button.class).click();

    CheckView secondView = new CheckView();
    secondView.setParameter(null, a.id().toString());
    first(secondView, CheckboxGroup.class).select(1);
    first(secondView, Button.class).click();

    assertEquals(1, credentials.all().size(), "re-passing must not duplicate the credential");
  }

  @Test
  @DisplayName("an unknown assessment id renders the unknown marker")
  void unknownAssessment() {
    CheckView view = new CheckView();
    view.setParameter(null, UUID.randomUUID().toString());
    assertEquals(List.of("UNKNOWN"), attributes(view, "data-check-result"));
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
