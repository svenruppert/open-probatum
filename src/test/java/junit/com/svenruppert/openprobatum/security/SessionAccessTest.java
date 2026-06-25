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

package junit.com.svenruppert.openprobatum.security;

import com.svenruppert.openprobatum.security.SessionAccess;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.jsentinel.authorization.api.tenant.TenantId;
import com.svenruppert.jsentinel.logout.SubjectId;
import com.svenruppert.jsentinel.session.JSentinelVersion;
import com.svenruppert.jsentinel.session.SessionId;
import com.svenruppert.jsentinel.session.SessionRecord;
import com.svenruppert.jsentinel.session.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SessionAccess — row-level own-sessions rule (§3.6/5.4, P014/R07)")
class SessionAccessTest {

  private static SessionRecord sessionFor(String subjectId) {
    return new SessionRecord(
        SessionId.of("s-" + subjectId),
        SubjectId.of(subjectId),
        TenantId.DEFAULT,
        Instant.EPOCH, Instant.EPOCH,
        JSentinelVersion.INITIAL,
        SessionStatus.ACTIVE);
  }

  private static AppUser user(long id) {
    return new AppUser(id, "u" + id, EnumSet.of(AuthorizationRole.LEARNER));
  }

  @Test
  @DisplayName("an admin (canSeeAll) sees every session")
  void adminSeesAll() {
    List<SessionRecord> all = List.of(sessionFor("1"), sessionFor("2"), sessionFor("3"));
    assertEquals(3, SessionAccess.visibleTo(all, user(1), true).size());
  }

  @Test
  @DisplayName("a non-admin sees only their own sessions")
  void nonAdminSeesOnlyOwn() {
    List<SessionRecord> all = List.of(sessionFor("1"), sessionFor("2"), sessionFor("1"));
    List<SessionRecord> own = SessionAccess.visibleTo(all, user(1), false);

    assertEquals(2, own.size());
    assertTrue(own.stream().allMatch(s -> "1".equals(s.subjectId().value())),
        "a non-admin must never see another subject's session");
  }

  @Test
  @DisplayName("a non-admin with no current subject sees nothing")
  void nonAdminAnonymousSeesNothing() {
    assertTrue(SessionAccess.visibleTo(List.of(sessionFor("1")), null, false).isEmpty());
  }

  @Test
  @DisplayName("a non-admin with a null id sees nothing (no NPE)")
  void nonAdminNullIdSeesNothing() {
    AppUser noId = new AppUser(null, "x", EnumSet.of(AuthorizationRole.LEARNER));
    assertTrue(SessionAccess.visibleTo(List.of(sessionFor("1")), noId, false).isEmpty());
  }
}
