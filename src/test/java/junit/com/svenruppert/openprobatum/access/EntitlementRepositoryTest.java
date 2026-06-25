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

package junit.com.svenruppert.openprobatum.access;

import com.svenruppert.openprobatum.access.EclipseStoreEntitlementRepository;
import com.svenruppert.openprobatum.access.Entitlement;
import com.svenruppert.openprobatum.access.EntitlementReason;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EntitlementRepository — shared-pair Eclipse-Store + in-memory (P004)")
class EntitlementRepositoryTest {

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;
  private EclipseStoreEntitlementRepository repo;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
    repo = new EclipseStoreEntitlementRepository();
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  @Test
  @DisplayName("grant + hasGrant + forUser on the shared store")
  void grantAndQuery() {
    UUID offering = UUID.randomUUID();
    repo.grant(new Entitlement(1001L, offering, EntitlementReason.CODE));

    assertTrue(repo.hasGrant(1001L, offering));
    assertFalse(repo.hasGrant(1002L, offering));
    assertEquals(1, repo.forUser(1001L).size());
  }

  @Test
  @DisplayName("a grant survives closing + reopening the storage pair")
  void roundTripSurvivesReopen() {
    UUID offering = UUID.randomUUID();
    repo.grant(new Entitlement(1001L, offering, EntitlementReason.MANUAL));

    pair.close();
    JSentinelStoragePair reopened = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(reopened);
    try {
      assertTrue(new EclipseStoreEntitlementRepository().hasGrant(1001L, offering));
    } finally {
      reopened.close();
    }
  }

  @Test
  @DisplayName("in-memory repository satisfies the same contract")
  void inMemoryContract() {
    InMemoryEntitlementRepository memory = new InMemoryEntitlementRepository();
    UUID offering = UUID.randomUUID();
    memory.grant(new Entitlement(7L, offering, EntitlementReason.PREREQUISITE));
    assertTrue(memory.hasGrant(7L, offering));
    assertEquals(1, memory.forUser(7L).size());
  }
}
