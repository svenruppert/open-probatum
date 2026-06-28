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

package junit.com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.AuthorView;
import com.svenruppert.openprobatum.views.BundleAuthorView;
import com.svenruppert.openprobatum.views.CoachingAuthorView;
import com.svenruppert.openprobatum.views.CoachingSessionView;
import com.svenruppert.openprobatum.views.CoachingSlotsView;
import com.svenruppert.openprobatum.views.CredentialAuditView;
import com.svenruppert.openprobatum.views.GovernanceView;
import com.svenruppert.openprobatum.views.LabBankView;
import com.svenruppert.openprobatum.views.MetricsView;
import com.svenruppert.openprobatum.views.QuestionBankView;
import com.svenruppert.openprobatum.views.WorkshopAuthorView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the nav↔route gate-consistency invariant (entry-review #1, finding 3;
 * P002): a {@code MainLayout} nav item is shown when the subject holds the item's
 * <em>permission</em>, but a route is reached only by exact <em>role</em>
 * membership ({@code @VisibleFor}, no admin bypass). Since PLATFORM_ADMIN holds
 * every permission, every permission-gated authoring/governance view must list
 * PLATFORM_ADMIN in {@code @VisibleFor} — otherwise an admin sees a dead nav link.
 */
@DisplayName("Authorization gate consistency — nav permission ↔ route @VisibleFor (P002)")
class GateConsistencyTest {

  private static Set<AuthorizationRole> rolesOf(Class<?> view) {
    VisibleFor annotation = view.getAnnotation(VisibleFor.class);
    assertTrue(annotation != null, view.getSimpleName() + " must carry @VisibleFor");
    return Set.of(annotation.value());
  }

  @Test
  @DisplayName("every permission-gated authoring/governance view admits PLATFORM_ADMIN")
  void adminReachesEveryPermissionGatedView() {
    List<Class<?>> views = List.of(
        AuthorView.class, QuestionBankView.class, LabBankView.class,
        BundleAuthorView.class, WorkshopAuthorView.class, MetricsView.class,
        GovernanceView.class, CredentialAuditView.class,
        CoachingAuthorView.class, CoachingSlotsView.class, CoachingSessionView.class);
    for (Class<?> view : views) {
      assertTrue(rolesOf(view).contains(AuthorizationRole.PLATFORM_ADMIN),
          view.getSimpleName() + " nav is permission-gated but its route excludes PLATFORM_ADMIN"
              + " — admin would get a dead link");
    }
  }

  @Test
  @DisplayName("MetricsView route matches its metrics:read nav gate (AUTHOR + REVIEWER + admin)")
  void metricsRouteMatchesNav() {
    Set<AuthorizationRole> roles = rolesOf(MetricsView.class);
    assertTrue(roles.contains(AuthorizationRole.AUTHOR));
    assertTrue(roles.contains(AuthorizationRole.REVIEWER),
        "REVIEWER holds metrics:read and must reach Quality metrics");
    assertTrue(roles.contains(AuthorizationRole.PLATFORM_ADMIN));
  }

  @Test
  @DisplayName("coaching delivery views are gated to COACH + admin (post-COACH-role)")
  void coachingViewsGatedToCoach() {
    for (Class<?> view : List.of(CoachingAuthorView.class, CoachingSlotsView.class,
        CoachingSessionView.class)) {
      Set<AuthorizationRole> roles = rolesOf(view);
      assertTrue(roles.contains(AuthorizationRole.COACH), view.getSimpleName());
      assertFalse(roles.contains(AuthorizationRole.REVIEWER),
          view.getSimpleName() + " no longer admits REVIEWER");
    }
  }
}
