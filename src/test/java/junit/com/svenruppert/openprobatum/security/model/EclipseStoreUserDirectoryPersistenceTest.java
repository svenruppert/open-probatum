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

package junit.com.svenruppert.openprobatum.security.model;

import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.model.EclipseStoreUserDirectoryPersistence;
import com.svenruppert.openprobatum.security.model.StoredUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EclipseStoreUserDirectoryPersistence — shared app-store round-trip")
class EclipseStoreUserDirectoryPersistenceTest {

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;
  private EclipseStoreUserDirectoryPersistence persistence;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
    persistence = new EclipseStoreUserDirectoryPersistence();
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  private static StoredUser storedUser(long id, String name, AuthorizationRole role, String hash) {
    return new StoredUser(new AppUser(id, name, EnumSet.of(role)), hash);
  }

  @Test
  @DisplayName("a fresh app store returns an empty map on load")
  void freshStorageIsEmpty() {
    Map<String, StoredUser> loaded = persistence.load();
    assertNotNull(loaded);
    assertTrue(loaded.isEmpty());
  }

  @Test
  @DisplayName("save then close + reopen the pair + load yields the same entries")
  void roundTripSurvivesClose() {
    Map<String, StoredUser> snapshot = new HashMap<>();
    snapshot.put("alice", storedUser(1L, "Alice", AuthorizationRole.PLATFORM_ADMIN, "$argon2id$fake$alice"));
    snapshot.put("bob", storedUser(2L, "Bob", AuthorizationRole.LEARNER, "$argon2id$fake$bob"));
    persistence.save(snapshot);

    pair.close(); // close the whole pair
    JSentinelStoragePair reopened = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(reopened);
    try {
      Map<String, StoredUser> reloaded = new EclipseStoreUserDirectoryPersistence().load();
      assertEquals(2, reloaded.size());
      assertEquals("$argon2id$fake$alice", reloaded.get("alice").passwordHash());
      assertEquals("$argon2id$fake$bob", reloaded.get("bob").passwordHash());
      assertEquals(new AppUser(1L, "Alice", EnumSet.of(AuthorizationRole.PLATFORM_ADMIN)),
          reloaded.get("alice").user());
    } finally {
      reopened.close();
    }
  }

  @Test
  @DisplayName("save replaces previous content wholesale (delete by omission)")
  void saveIsReplaceNotMerge() {
    Map<String, StoredUser> first = new HashMap<>();
    first.put("alice", storedUser(1L, "Alice", AuthorizationRole.LEARNER, "$alice"));
    first.put("bob", storedUser(2L, "Bob", AuthorizationRole.LEARNER, "$bob"));
    persistence.save(first);
    assertEquals(2, persistence.load().size());

    Map<String, StoredUser> second = new HashMap<>();
    second.put("carol", storedUser(3L, "Carol", AuthorizationRole.PLATFORM_ADMIN, "$carol"));
    persistence.save(second);

    Map<String, StoredUser> reloaded = persistence.load();
    assertEquals(1, reloaded.size());
    assertEquals("$carol", reloaded.get("carol").passwordHash());
  }

  @Test
  @DisplayName("load returns a defensive copy — caller mutations do not bleed into storage")
  void loadReturnsCopy() {
    Map<String, StoredUser> snapshot = new HashMap<>();
    snapshot.put("alice", storedUser(1L, "Alice", AuthorizationRole.LEARNER, "$h"));
    persistence.save(snapshot);

    Map<String, StoredUser> loaded = persistence.load();
    loaded.clear();

    assertEquals(1, persistence.load().size());
  }
}
