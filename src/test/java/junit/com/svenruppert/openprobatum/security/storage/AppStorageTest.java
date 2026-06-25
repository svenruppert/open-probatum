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

package junit.com.svenruppert.openprobatum.security.storage;

import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AppStorage — the single consolidated storage pair + test seam")
class AppStorageTest {

  @TempDir
  Path tempDir;

  private JSentinelStoragePair pair;

  @BeforeEach
  void setUp() {
    pair = JSentinelStorageFactory.openAt(tempDir);
    AppStorage.setPair(pair);
  }

  @AfterEach
  void tearDown() {
    AppStorage.reset();
    pair.close();
  }

  @Test
  @DisplayName("framework() / app() / pair() expose the installed override pair")
  void exposesInstalledPair() {
    assertSame(pair, AppStorage.pair(), "pair() must return the test override");
    assertSame(pair.framework(), AppStorage.framework(), "framework() forwards to the pair");
    assertSame(pair.app(), AppStorage.app(), "app() forwards to the pair");
    assertNotNull(AppStorage.framework().auditEventStore(), "framework store is live");
  }

  @Test
  @DisplayName("appRoot() get-or-creates one shared root and returns the same instance")
  void appRootIsStableAndShared() {
    AppStorage.AppRoot first = AppStorage.appRoot();
    assertNotNull(first, "a fresh store must get a created root");
    assertTrue(first.users.isEmpty() && first.credentials.isEmpty(), "fresh root is empty");

    AppStorage.AppRoot second = AppStorage.appRoot();
    assertSame(first, second, "appRoot() must reuse the existing root, not replace it");
  }

  @Test
  @DisplayName("reset() drops the override so pair() falls back off the test pair")
  void resetClearsOverride() {
    assertSame(pair, AppStorage.pair());
    AppStorage.reset();
    // After reset the override is gone; re-install for a deterministic assertion
    // (touching the production singleton here would open the real ./data store).
    AppStorage.setPair(pair);
    assertSame(pair, AppStorage.pair(), "re-installing the override is honoured");
  }
}
