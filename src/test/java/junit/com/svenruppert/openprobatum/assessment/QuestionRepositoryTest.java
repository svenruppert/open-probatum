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

import com.svenruppert.openprobatum.assessment.EclipseStoreQuestionRepository;
import com.svenruppert.openprobatum.assessment.InMemoryQuestionRepository;
import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QuestionRepository — bank: shared-store round-trip + lineage (P002)")
class QuestionRepositoryTest {

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;
  private EclipseStoreQuestionRepository repo;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
    repo = new EclipseStoreQuestionRepository();
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  private static Question q() {
    return Question.singleChoice("2+2?", List.of("3", "4"), 1, "Arithmetic.");
  }

  @Test
  @DisplayName("save then findById returns the same question version")
  void saveThenFind() {
    Question q = q();
    repo.save(q);
    assertEquals(q, repo.findById(q.id()).orElseThrow());
  }

  @Test
  @DisplayName("versionsOf + latestOf walk a lineage; old versions persist")
  void lineageHelpers() {
    Question v1 = q();
    Question v2 = v1.asNewVersion();
    repo.save(v1);
    repo.save(v2);

    assertEquals(List.of(v1, v2), repo.versionsOf(v1.lineageId()));
    assertEquals(v2, repo.latestOf(v1.lineageId()).orElseThrow());
    assertTrue(repo.findById(v1.id()).isPresent(), "the old version is still stored");
  }

  @Test
  @DisplayName("a saved question survives closing + reopening the storage pair")
  void roundTripSurvivesReopen() {
    Question q = q().withStatus(com.svenruppert.openprobatum.content.ContentStatus.PUBLISHED);
    repo.save(q);

    pair.close();
    JSentinelStoragePair reopened = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(reopened);
    try {
      assertEquals(q, new EclipseStoreQuestionRepository().findById(q.id()).orElseThrow());
    } finally {
      reopened.close();
    }
  }

  @Test
  @DisplayName("in-memory repository satisfies the same contract")
  void inMemoryContract() {
    InMemoryQuestionRepository memory = new InMemoryQuestionRepository();
    Question q = q();
    memory.save(q);
    assertEquals(q, memory.findById(q.id()).orElseThrow());
    assertEquals(1, memory.all().size());
    assertEquals(q, memory.latestOf(q.lineageId()).orElseThrow());
  }
}
