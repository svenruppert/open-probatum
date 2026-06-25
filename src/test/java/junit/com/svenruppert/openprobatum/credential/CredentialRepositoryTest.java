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

package junit.com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.EclipseStoreCredentialRepository;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialRepository — shared-pair Eclipse-Store + in-memory (P003)")
class CredentialRepositoryTest {

  private static final Instant ISSUED = Instant.parse("2026-01-01T00:00:00Z");

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;
  private EclipseStoreCredentialRepository repo;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
    repo = new EclipseStoreCredentialRepository();
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  private static Credential issue(String recipient) {
    return Credential.issue("Vaadin Basics", CredentialType.COMPLETION_CERTIFICATE,
        recipient, "Open Probatum Academy", ISSUED, null);
  }

  @Test
  @DisplayName("a fresh app store is empty")
  void freshIsEmpty() {
    assertTrue(repo.all().isEmpty());
  }

  @Test
  @DisplayName("save then findById returns the same credential")
  void saveThenFind() {
    Credential c = issue("Alice");
    repo.save(c);
    assertEquals(c, repo.findById(c.id()).orElseThrow());
  }

  @Test
  @DisplayName("findById is empty for an unknown id")
  void unknownIsEmpty() {
    assertTrue(repo.findById(UUID.randomUUID()).isEmpty());
  }

  @Test
  @DisplayName("all() yields every stored credential")
  void allYieldsStored() {
    repo.save(issue("Alice"));
    repo.save(issue("Bob"));
    assertEquals(2, repo.all().size());
  }

  @Test
  @DisplayName("a saved credential survives closing + reopening the storage pair")
  void roundTripSurvivesReopen() {
    Credential c = issue("Carol");
    repo.save(c);

    pair.close(); // close the whole pair
    JSentinelStoragePair reopened = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(reopened);
    try {
      assertEquals(c, new EclipseStoreCredentialRepository().findById(c.id()).orElseThrow(),
          "the credential record must reload from a fresh pair on the same directory");
    } finally {
      reopened.close();
    }
  }

  @Test
  @DisplayName("in-memory repository satisfies the same contract")
  void inMemoryContract() {
    InMemoryCredentialRepository memory = new InMemoryCredentialRepository();
    Credential c = issue("Dora");
    memory.save(c);
    assertEquals(c, memory.findById(c.id()).orElseThrow());
    assertTrue(memory.findById(UUID.randomUUID()).isEmpty());
    assertEquals(1, memory.all().size());
  }
}
