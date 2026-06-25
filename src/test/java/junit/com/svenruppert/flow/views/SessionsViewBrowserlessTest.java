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

package junit.com.svenruppert.flow.views;

import com.svenruppert.flow.security.model.AppUser;
import com.svenruppert.flow.security.roles.AuthorizationRole;
import com.svenruppert.flow.security.services.SessionStoreProvider;
import com.svenruppert.flow.views.SessionsView;
import com.svenruppert.jsentinel.audit.AuditEvent;
import com.svenruppert.jsentinel.audit.AuditQuery;
import com.svenruppert.jsentinel.audit.JSentinelAuditService;
import com.svenruppert.jsentinel.audit.SessionInvalidated;
import com.svenruppert.jsentinel.authorization.api.JSentinelServiceResolver;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.jsentinel.authorization.api.tenant.TenantId;
import com.svenruppert.jsentinel.logout.SubjectId;
import com.svenruppert.jsentinel.session.JSentinelVersion;
import com.svenruppert.jsentinel.session.SessionId;
import com.svenruppert.jsentinel.session.SessionRecord;
import com.svenruppert.jsentinel.session.SessionStatus;
import com.vaadin.browserless.BrowserlessTest;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SessionsView — admin wrapper around SessionManagementView")
class SessionsViewBrowserlessTest extends BrowserlessTest {

  private RecordingAudit audit;
  private JSentinelAuditService previousAudit;

  @BeforeEach
  void seedAdmin() {
    TestSupport.seedAdminAndResetBootstrap();
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1L, "Admin",
            EnumSet.of(AuthorizationRole.ADMIN, AuthorizationRole.USER)),
        AppUser.class);
    audit = new RecordingAudit();
    previousAudit = JSentinelServiceResolver.findJSentinelAuditService().orElse(null);
    JSentinelServiceResolver.setJSentinelAuditService(audit);
  }

  @AfterEach
  void restoreAudit() {
    JSentinelServiceResolver.setJSentinelAuditService(previousAudit);
  }

  @Test
  @DisplayName("NAV constant is 'admin/sessions'")
  void navConstant() {
    assertEquals("admin/sessions", SessionsView.NAV);
  }

  @Test
  @DisplayName("view instantiation does not throw — store binding sanity")
  void viewInstantiates() {
    SessionsView view = navigate(SessionsView.class);
    assertNotNull(view);
  }

  @Test
  @DisplayName("revoke callback flips the session's status to REVOKED")
  void revokeFlipsSessionStatus() throws Exception {
    Instant now = Instant.now();
    SessionRecord active = new SessionRecord(
        SessionId.of("test-session-1"),
        SubjectId.of("99"),
        TenantId.DEFAULT,
        now, now,
        JSentinelVersion.INITIAL,
        SessionStatus.ACTIVE);
    SessionStoreProvider.sessionStore().save(active);

    // Invoke the private revoke(SessionRecord) on a fresh view instance
    // via reflection — the grid's per-row Revoke button dispatches there
    // when an admin clicks it.
    SessionsView view = navigate(SessionsView.class);
    java.lang.reflect.Method m = SessionsView.class
        .getDeclaredMethod("revoke", SessionRecord.class);
    m.setAccessible(true);
    m.invoke(view, active);

    SessionRecord updated = SessionStoreProvider.sessionStore()
        .findById(SessionId.of("test-session-1"))
        .orElseThrow();
    assertEquals(SessionStatus.REVOKED, updated.status(),
        "revoke callback must persist a REVOKED status on the SessionRecord");
  }

  @Test
  @DisplayName("revoke emits a SessionInvalidated audit event (R08 — subtitle promise honoured)")
  void revokeEmitsSessionInvalidatedAudit() throws Exception {
    Instant now = Instant.now();
    SessionRecord active = new SessionRecord(
        SessionId.of("test-session-2"),
        SubjectId.of("77"),
        TenantId.DEFAULT,
        now, now,
        JSentinelVersion.INITIAL,
        SessionStatus.ACTIVE);
    SessionStoreProvider.sessionStore().save(active);

    SessionsView view = navigate(SessionsView.class);
    audit.events.clear();
    java.lang.reflect.Method m = SessionsView.class
        .getDeclaredMethod("revoke", SessionRecord.class);
    m.setAccessible(true);
    m.invoke(view, active);

    assertTrue(audit.events.stream().anyMatch(e ->
            e instanceof SessionInvalidated si
                && "77".equals(si.subjectId())
                && "test-session-2".equals(si.sessionId())),
        "revoke must publish a SessionInvalidated for the revoked session");
  }

  // Recording audit sink — a real implementation, not a mock framework.
  private static final class RecordingAudit implements JSentinelAuditService {
    final List<AuditEvent> events = new ArrayList<>();
    @Override public void publish(AuditEvent e) { events.add(e); }
    @Override public List<AuditEvent> query(AuditQuery q) { return List.copyOf(events); }
  }
}
