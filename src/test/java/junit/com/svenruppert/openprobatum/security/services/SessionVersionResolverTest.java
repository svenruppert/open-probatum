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

package junit.com.svenruppert.openprobatum.security.services;

import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.services.SessionVersionResolver;
import com.svenruppert.openprobatum.security.services.VersionBumper;
import com.svenruppert.jsentinel.authorization.api.JSentinelServiceResolver;
import com.svenruppert.jsentinel.authorization.api.tenant.TenantId;
import com.svenruppert.jsentinel.logout.SubjectId;
import com.svenruppert.jsentinel.session.InMemoryJSentinelVersionStore;
import com.svenruppert.jsentinel.session.JSentinelVersion;
import com.svenruppert.jsentinel.session.JSentinelVersionKey;
import com.svenruppert.jsentinel.session.JSentinelVersionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SessionVersionResolver — reads the live drift version, not INITIAL")
class SessionVersionResolverTest {

  private JSentinelVersionStore previousStore;

  @BeforeEach
  void setUp() {
    previousStore = JSentinelServiceResolver.findJSentinelVersionStore().orElse(null);
  }

  @AfterEach
  void tearDown() {
    JSentinelServiceResolver.setJSentinelVersionStore(previousStore);
  }

  private static AppUser user(long id) {
    return new AppUser(id, "u" + id, EnumSet.of(AuthorizationRole.USER));
  }

  @Test
  @DisplayName("after a prior bump, current() returns the bumped value (the R01 regression)")
  void afterBumpReturnsCurrentValue() {
    JSentinelServiceResolver.setJSentinelVersionStore(new InMemoryJSentinelVersionStore());
    AppUser u = user(77L);

    // Two role-revoke bumps persist version 2 for this subject BEFORE a fresh login.
    VersionBumper.bump(u);
    VersionBumper.bump(u);

    // The session baseline must match the live store, not INITIAL.
    assertEquals(2L, SessionVersionResolver.current(u).value(),
        "login must stamp the subject's current drift version, not JSentinelVersion.INITIAL");
  }

  @Test
  @DisplayName("a never-bumped subject still resolves to INITIAL (common path unchanged)")
  void neverBumpedSubjectIsInitial() {
    JSentinelServiceResolver.setJSentinelVersionStore(new InMemoryJSentinelVersionStore());
    assertEquals(JSentinelVersion.INITIAL, SessionVersionResolver.current(user(5L)));
  }

  @Test
  @DisplayName("no version-store SPI → falls back to INITIAL")
  void noStoreFallsBackToInitial() {
    JSentinelServiceResolver.setJSentinelVersionStore(null);
    assertEquals(JSentinelVersion.INITIAL, SessionVersionResolver.current(user(9L)));
  }

  @Test
  @DisplayName("null user → INITIAL (callers stay unconditional)")
  void nullUserIsInitial() {
    JSentinelServiceResolver.setJSentinelVersionStore(new InMemoryJSentinelVersionStore());
    assertEquals(JSentinelVersion.INITIAL, SessionVersionResolver.current(null));
  }

  @Test
  @DisplayName("read key matches the bumper's write key (same TenantId.DEFAULT + SubjectId)")
  void readKeyMatchesBumperKey() {
    InMemoryJSentinelVersionStore store = new InMemoryJSentinelVersionStore();
    JSentinelServiceResolver.setJSentinelVersionStore(store);

    // Increment via the explicit key the bumper uses; the resolver must see it.
    JSentinelVersionKey key = new JSentinelVersionKey(TenantId.DEFAULT, SubjectId.of("42"));
    store.increment(key);

    assertEquals(1L, SessionVersionResolver.current(user(42L)).value());
  }
}
