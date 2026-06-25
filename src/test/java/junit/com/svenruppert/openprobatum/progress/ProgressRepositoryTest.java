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

package junit.com.svenruppert.openprobatum.progress;

import com.svenruppert.openprobatum.progress.EclipseStoreProgressRepository;
import com.svenruppert.openprobatum.progress.LearnerProgress;
import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProgressRepository — shared-pair Eclipse-Store round-trip (P008)")
class ProgressRepositoryTest {

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;
  private EclipseStoreProgressRepository repo;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
    repo = new EclipseStoreProgressRepository();
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  @Test
  @DisplayName("a learner's completed modules survive a close + reopen")
  void roundTripSurvivesReopen() {
    UUID offering = UUID.randomUUID();
    UUID module = UUID.randomUUID();
    repo.save(new LearnerProgress(1001L, offering, Set.of(module)));

    pair.close();
    JSentinelStoragePair reopened = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(reopened);
    try {
      LearnerProgress reloaded = new EclipseStoreProgressRepository()
          .find(1001L, offering).orElseThrow();
      assertEquals(Set.of(module), reloaded.completedModuleIds());
    } finally {
      reopened.close();
    }
  }

  @Test
  @DisplayName("find is empty for an unknown learner/offering")
  void unknownIsEmpty() {
    assertTrue(repo.find(9L, UUID.randomUUID()).isEmpty());
  }
}
