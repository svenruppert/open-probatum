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
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Question — versioned didactic object (P002)")
class QuestionVersioningTest {

  @Test
  @DisplayName("a fresh question is a DRAFT v1 with id == lineageId")
  void freshIsDraftV1() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.");
    assertEquals(1, q.version());
    assertEquals(ContentStatus.DRAFT, q.status());
    assertEquals(q.id(), q.lineageId(), "the first version anchors the lineage");
    assertEquals(Difficulty.MEDIUM, q.difficulty());
  }

  @Test
  @DisplayName("withMetadata sets objective/topic/difficulty; withStatus moves the lifecycle")
  void metadataAndStatus() {
    Question q = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.")
        .withMetadata("Add small integers", "Maths", Difficulty.EASY)
        .withStatus(ContentStatus.IN_REVIEW);
    assertEquals("Add small integers", q.learningObjective());
    assertEquals("Maths", q.topic());
    assertEquals(Difficulty.EASY, q.difficulty());
    assertEquals(ContentStatus.IN_REVIEW, q.status());
  }

  @Test
  @DisplayName("asNewVersion bumps the version + new id + same lineage, leaving the old intact")
  void newVersionPreservesOld() {
    Question v1 = Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.")
        .withStatus(ContentStatus.PUBLISHED);
    Question v2 = v1.asNewVersion();

    assertEquals(2, v2.version());
    assertEquals(v1.lineageId(), v2.lineageId(), "same logical question");
    assertNotEquals(v1.id(), v2.id(), "a new version is a distinct record");
    assertEquals(ContentStatus.DRAFT, v2.status(), "a new version starts as a draft");

    // v1 is untouched (records are immutable) — old attempts referencing it stay truthful.
    assertEquals(1, v1.version());
    assertEquals(ContentStatus.PUBLISHED, v1.status());
  }
}
