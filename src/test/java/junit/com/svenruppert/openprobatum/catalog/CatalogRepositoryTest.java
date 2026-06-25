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

package junit.com.svenruppert.openprobatum.catalog;

import com.svenruppert.openprobatum.catalog.EclipseStoreCatalogRepository;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogRepository — shared-pair Eclipse-Store + in-memory (P003)")
class CatalogRepositoryTest {

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;
  private EclipseStoreCatalogRepository repo;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
    repo = new EclipseStoreCatalogRepository();
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  private static Offering offering(String title) {
    return Offering.publicPath(title, "desc",
        new LearningPath("Path", List.of(Module.mandatory("M", "content"))));
  }

  @Test
  @DisplayName("save then findById returns the same offering")
  void saveThenFind() {
    Offering o = offering("Vaadin Basics");
    repo.save(o);
    assertEquals(o, repo.findById(o.id()).orElseThrow());
  }

  @Test
  @DisplayName("findById is empty for an unknown id; all() yields every offering")
  void unknownAndAll() {
    assertTrue(repo.findById(UUID.randomUUID()).isEmpty());
    repo.save(offering("A"));
    repo.save(offering("B"));
    assertEquals(2, repo.all().size());
  }

  @Test
  @DisplayName("a code-gated offering survives closing + reopening the storage pair")
  void roundTripSurvivesReopen() {
    Offering coded = Offering.codePath("Gated", "desc",
        new LearningPath("P", List.of(Module.mandatory("M", "c"))), "CODE-42");
    repo.save(coded);

    pair.close();
    JSentinelStoragePair reopened = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(reopened);
    try {
      Offering reloaded = new EclipseStoreCatalogRepository().findById(coded.id()).orElseThrow();
      assertEquals(coded, reloaded);
      assertEquals("CODE-42", reloaded.accessCodeOpt().orElseThrow());
    } finally {
      reopened.close();
    }
  }

  @Test
  @DisplayName("in-memory repository satisfies the same contract")
  void inMemoryContract() {
    InMemoryCatalogRepository memory = new InMemoryCatalogRepository();
    Offering o = offering("Heap");
    memory.save(o);
    assertEquals(o, memory.findById(o.id()).orElseThrow());
    assertTrue(memory.findById(UUID.randomUUID()).isEmpty());
    assertEquals(1, memory.all().size());
  }
}
