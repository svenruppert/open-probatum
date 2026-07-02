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

import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.views.AdminRolesView;
import com.svenruppert.openprobatum.views.AppLoginView;
import com.svenruppert.openprobatum.views.AuthorView;
import com.svenruppert.openprobatum.views.CoachingSlotsView;
import com.svenruppert.openprobatum.views.DashboardView;
import com.svenruppert.openprobatum.views.GovernanceView;
import com.svenruppert.openprobatum.views.ReviewView;
import com.svenruppert.openprobatum.views.SetupView;
import com.svenruppert.openprobatum.views.ValidationView;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import junit.com.svenruppert.openprobatum.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("AppLoginView — navigation + route constants")
class AppLoginViewBrowserlessTest extends BrowserlessTest {

  @BeforeEach
  void seedAdminSoBootstrapNotRequired() {
    TestSupport.seedAdminAndResetBootstrap();
  }

  @Test
  @DisplayName("NAV constant is 'login' — guards against route drift")
  void navConstant() {
    assertEquals("login", AppLoginView.NAV);
  }

  @Test
  @DisplayName("navigating to /login lands on AppLoginView when bootstrap not required")
  void navigateLandsOnAppLoginView() {
    Component current = navigate(AppLoginView.class);
    assertNotNull(current);
    assertEquals(AppLoginView.class, current.getClass(),
        "with an admin seeded, /login must NOT forward to /setup");
  }

  @Test
  @DisplayName("SetupView.NAV ('setup') is the forwarder target")
  void setupViewNavConstant() {
    assertEquals("setup", SetupView.NAV);
  }

  @Test
  @DisplayName("post-login landing view matches the subject's roles — no denied-bounce for staff-only users (P008)")
  void landingViewMatchesRoles() throws Exception {
    assertEquals(DashboardView.class, landingFor(AuthorizationRole.LEARNER));
    assertEquals(AuthorView.class, landingFor(AuthorizationRole.AUTHOR));
    assertEquals(ReviewView.class, landingFor(AuthorizationRole.REVIEWER));
    assertEquals(CoachingSlotsView.class, landingFor(AuthorizationRole.COACH));
    assertEquals(GovernanceView.class, landingFor(AuthorizationRole.CREDENTIAL_MANAGER));
    assertEquals(AdminRolesView.class, landingFor(AuthorizationRole.PLATFORM_ADMIN));
    assertEquals(ValidationView.class, landingFor(AuthorizationRole.VERIFIER));
  }

  @Test
  @DisplayName("the bootstrap admin (PLATFORM_ADMIN + LEARNER) still lands on the dashboard")
  void bootstrapAdminLandsOnDashboard() throws Exception {
    assertEquals(DashboardView.class, landingFor(
        AuthorizationRole.PLATFORM_ADMIN, AuthorizationRole.LEARNER));
  }

  private static Class<?> landingFor(AuthorizationRole first, AuthorizationRole... rest)
      throws Exception {
    Method m = AppLoginView.class.getDeclaredMethod("landingViewFor", AppUser.class);
    m.setAccessible(true);
    return (Class<?>) m.invoke(null, new AppUser(99L, "probe", EnumSet.of(first, rest)));
  }
}
