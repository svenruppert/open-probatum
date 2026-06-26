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
import com.svenruppert.openprobatum.security.services.AppAuthorizationService;
import com.svenruppert.jsentinel.authorization.api.permissions.PermissionName;
import com.svenruppert.jsentinel.authorization.api.roles.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AppAuthorizationService — role → permission mapping")
class AppAuthorizationServiceTest {

  private final AppAuthorizationService service = new AppAuthorizationService();

  // ── permissionsFor ─────────────────────────────────────────────

  private Set<String> permissionsOf(AuthorizationRole... roles) {
    AppUser u = new AppUser(1L, "u", EnumSet.copyOf(Set.of(roles)));
    return service.permissionsFor(u).permissionNames().stream()
        .map(PermissionName::value)
        .collect(Collectors.toSet());
  }

  @Test
  @DisplayName("PLATFORM_ADMIN holds every permission (the superuser)")
  void platformAdminGetsEveryPermission() {
    assertEquals(
        Set.of("app:view", "audit:read", "admin:sessions", "admin:roles",
            "author:content", "author:review", "credential:manage"),
        permissionsOf(AuthorizationRole.PLATFORM_ADMIN));
  }

  @Test
  @DisplayName("REVIEWER gets app:view + author:review (and nothing more)")
  void reviewerGetsReviewPermission() {
    assertEquals(Set.of("app:view", "author:review"),
        permissionsOf(AuthorizationRole.REVIEWER));
  }

  @Test
  @DisplayName("LEARNER gets only app:view")
  void learnerGetsOnlyAppView() {
    assertEquals(Set.of("app:view"), permissionsOf(AuthorizationRole.LEARNER));
  }

  @Test
  @DisplayName("AUTHOR gets app:view + author:content")
  void authorGetsContentPermission() {
    assertEquals(Set.of("app:view", "author:content"),
        permissionsOf(AuthorizationRole.AUTHOR));
  }

  @Test
  @DisplayName("CREDENTIAL_MANAGER gets app:view + credential:manage")
  void credentialManagerGetsManagePermission() {
    assertEquals(Set.of("app:view", "credential:manage"),
        permissionsOf(AuthorizationRole.CREDENTIAL_MANAGER));
  }

  @Test
  @DisplayName("VERIFIER gets only app:view")
  void verifierGetsOnlyAppView() {
    assertEquals(Set.of("app:view"), permissionsOf(AuthorizationRole.VERIFIER));
  }

  @Test
  @DisplayName("permissions are the union across a multi-role subject")
  void multiRoleUnionsPermissions() {
    assertEquals(Set.of("app:view", "author:content", "credential:manage"),
        permissionsOf(AuthorizationRole.AUTHOR, AuthorizationRole.CREDENTIAL_MANAGER));
  }

  @Test
  @DisplayName("subject without roles gets empty permissions")
  void rolelessSubjectGetsNothing() {
    AppUser nobody = new AppUser(4L, "nobody",
        EnumSet.noneOf(AuthorizationRole.class));

    assertTrue(service.permissionsFor(nobody).permissionNames().isEmpty());
  }

  // ── rolesFor ────────────────────────────────────────────────────

  @Test
  @DisplayName("rolesFor mirrors the subject's role enum names exactly")
  void rolesForReturnsRoleNames() {
    AppUser admin = new AppUser(5L, "admin",
        EnumSet.of(AuthorizationRole.PLATFORM_ADMIN, AuthorizationRole.LEARNER));

    Set<String> names = admin.roles().stream()
        .map(Enum::name)
        .collect(Collectors.toSet());

    Set<String> reported = service.rolesFor(admin).roleNames().stream()
        .map(RoleName::value)
        .collect(Collectors.toSet());

    assertEquals(names, reported);
    assertEquals(Set.of("PLATFORM_ADMIN", "LEARNER"), reported);
  }

  @Test
  @DisplayName("rolesFor of a roleless subject is empty")
  void rolesForRolelessSubjectIsEmpty() {
    AppUser nobody = new AppUser(6L, "nobody",
        EnumSet.noneOf(AuthorizationRole.class));

    assertTrue(service.rolesFor(nobody).roleNames().isEmpty());
  }

  // ── null defence ───────────────────────────────────────────────

  @Test
  @DisplayName("null subject is rejected on both lookups")
  void nullSubjectThrows() {
    assertThrows(NullPointerException.class, () -> service.rolesFor(null));
    assertThrows(NullPointerException.class, () -> service.permissionsFor(null));
  }
}
