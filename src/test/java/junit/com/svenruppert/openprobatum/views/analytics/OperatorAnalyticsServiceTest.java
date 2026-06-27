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

package junit.com.svenruppert.openprobatum.views.analytics;

import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialStatus;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.EffectiveStatus;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.lab.InMemoryLabSubmissionRepository;
import com.svenruppert.openprobatum.lab.LabSubmission;
import com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.model.InMemoryUserDirectoryPersistence;
import com.svenruppert.openprobatum.security.model.PersistentUserDirectory;
import com.svenruppert.openprobatum.security.model.UserDirectoryProvider;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.analytics.OperatorAnalyticsService;
import com.svenruppert.jsentinel.credential.password.bouncycastle.BouncyCastleHashingServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("OperatorAnalyticsService — academy-wide aggregation (P002)")
class OperatorAnalyticsServiceTest {

  private InMemoryCredentialRepository credentials;
  private InMemoryQuestionRepository questions;
  private final OperatorAnalyticsService service = new OperatorAnalyticsService();

  @BeforeEach
  void setUp() {
    credentials = new InMemoryCredentialRepository();
    questions = new InMemoryQuestionRepository();
    CredentialRepositoryProvider.setRepository(credentials);
    QuestionRepositoryProvider.setRepository(questions);
    LabSubmissionRepositoryProvider.setRepository(new InMemoryLabSubmissionRepository());
    UserDirectoryProvider.setDirectory(new PersistentUserDirectory(
        new InMemoryUserDirectoryPersistence(), BouncyCastleHashingServices.modern()));
  }

  @AfterEach
  void tearDown() {
    CredentialRepositoryProvider.reset();
    QuestionRepositoryProvider.reset();
    LabSubmissionRepositoryProvider.reset();
    UserDirectoryProvider.reset();
  }

  private static Credential credential(Evidence evidence) {
    return Credential.issue("C", CredentialType.COMPLETION_CERTIFICATE, 1L, "Ada", "Academy",
        Instant.parse("2026-01-01T00:00:00Z"), null, evidence);
  }

  @Test
  @DisplayName("credential stats count by effective status and by evidence type")
  void credentialStats() {
    credentials.save(credential(Evidence.assessmentPassed(UUID.randomUUID(), 1)));   // VALID
    credentials.save(credential(Evidence.labVerified(UUID.randomUUID(), 1)));        // VALID
    credentials.save(credential(Evidence.manualAward()).withStatus(CredentialStatus.REVOKED));

    var stats = service.credentialStats();
    assertEquals(3, stats.total());
    assertEquals(2L, stats.byStatus().get(EffectiveStatus.VALID));
    assertEquals(1L, stats.byStatus().get(EffectiveStatus.REVOKED));
    assertEquals(1L, stats.byEvidence().get(Evidence.Type.ASSESSMENT_PASSED));
    assertEquals(1L, stats.byEvidence().get(Evidence.Type.PRACTICAL_LAB_VERIFIED));
    assertEquals(1L, stats.byEvidence().get(Evidence.Type.MANUAL_AWARD));
  }

  @Test
  @DisplayName("the content pipeline counts each content type by ContentStatus")
  void contentPipeline() {
    questions.save(Question.singleChoice("q1", List.of("a", "b"), 0, "e"));         // DRAFT
    questions.save(Question.singleChoice("q2", List.of("a", "b"), 0, "e")
        .withStatus(ContentStatus.PUBLISHED));

    var pipelines = service.contentPipelines();
    var qPipe = pipelines.stream().filter(p -> p.type().equals("Questions")).findFirst().orElseThrow();
    assertEquals(2, qPipe.total());
    assertEquals(1L, qPipe.byStatus().get(ContentStatus.DRAFT));
    assertEquals(1L, qPipe.byStatus().get(ContentStatus.PUBLISHED));
    assertEquals(6, pipelines.size(), "one pipeline per content type");
  }

  @Test
  @DisplayName("engagement counts registered users, lab submissions, and bookings")
  void engagement() {
    UserDirectoryProvider.directory().registerWithHashedPassword("ada", "hash",
        new AppUser(1L, "Ada", EnumSet.of(AuthorizationRole.LEARNER)));
    UserDirectoryProvider.directory().registerWithHashedPassword("bob", "hash",
        new AppUser(2L, "Bob", EnumSet.of(AuthorizationRole.LEARNER)));
    LabSubmissionRepositoryProvider.repository().save(
        LabSubmission.submit(UUID.randomUUID(), 1, 1L, "Ada", "did it", null));

    var e = service.engagement();
    assertEquals(2, e.registeredUsers());
    assertEquals(1, e.labSubmissions());
  }

  @Test
  @DisplayName("an empty academy aggregates to zeros (no crash)")
  void emptyAcademy() {
    var stats = service.credentialStats();
    assertEquals(0, stats.total());
    assertNull(stats.byStatus().get(EffectiveStatus.VALID));
    assertEquals(0, service.engagement().registeredUsers());
    assertEquals(0, service.contentPipelines().get(0).total());
  }
}
