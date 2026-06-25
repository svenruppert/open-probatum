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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialRepository — Eclipse-Store round-trip + in-memory (P003)")
class CredentialRepositoryTest {

  private static final Instant ISSUED = Instant.parse("2026-01-01T00:00:00Z");

  private static Credential issue(String recipient) {
    return Credential.issue("Vaadin Basics", CredentialType.COMPLETION_CERTIFICATE,
        recipient, "Open Probatum Academy", ISSUED, null);
  }

  @Test
  @DisplayName("fresh Eclipse-Store storage is empty")
  void freshIsEmpty(@TempDir Path dir) {
    EclipseStoreCredentialRepository repo =
        new EclipseStoreCredentialRepository(dir.resolve("creds"));
    try {
      assertTrue(repo.all().isEmpty());
    } finally {
      repo.close();
    }
  }

  @Test
  @DisplayName("save then findById returns the same credential")
  void saveThenFind(@TempDir Path dir) {
    EclipseStoreCredentialRepository repo =
        new EclipseStoreCredentialRepository(dir.resolve("creds"));
    try {
      Credential c = issue("Alice");
      repo.save(c);
      assertEquals(c, repo.findById(c.id()).orElseThrow());
    } finally {
      repo.close();
    }
  }

  @Test
  @DisplayName("findById is empty for an unknown id")
  void unknownIsEmpty(@TempDir Path dir) {
    EclipseStoreCredentialRepository repo =
        new EclipseStoreCredentialRepository(dir.resolve("creds"));
    try {
      assertTrue(repo.findById(UUID.randomUUID()).isEmpty());
    } finally {
      repo.close();
    }
  }

  @Test
  @DisplayName("a saved credential survives close + reopen on the same directory")
  void roundTripSurvivesReopen(@TempDir Path dir) {
    Path store = dir.resolve("creds-rt");
    Credential c = issue("Carol");

    EclipseStoreCredentialRepository r1 = new EclipseStoreCredentialRepository(store);
    try {
      r1.save(c);
    } finally {
      r1.close();
    }

    EclipseStoreCredentialRepository r2 = new EclipseStoreCredentialRepository(store);
    try {
      assertEquals(c, r2.findById(c.id()).orElseThrow(),
          "the credential record must reload byte-for-byte from a fresh instance");
    } finally {
      r2.close();
    }
  }

  @Test
  @DisplayName("all() yields every stored credential")
  void allYieldsStored(@TempDir Path dir) {
    EclipseStoreCredentialRepository repo =
        new EclipseStoreCredentialRepository(dir.resolve("creds"));
    try {
      repo.save(issue("Alice"));
      repo.save(issue("Bob"));
      assertEquals(2, repo.all().size());
    } finally {
      repo.close();
    }
  }

  @Test
  @DisplayName("in-memory repository satisfies the same contract")
  void inMemoryContract() {
    InMemoryCredentialRepository repo = new InMemoryCredentialRepository();
    Credential c = issue("Dora");
    repo.save(c);
    assertEquals(c, repo.findById(c.id()).orElseThrow());
    assertTrue(repo.findById(UUID.randomUUID()).isEmpty());
    assertEquals(1, repo.all().size());
  }
}
