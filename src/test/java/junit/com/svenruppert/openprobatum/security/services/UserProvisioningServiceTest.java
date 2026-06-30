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
import com.svenruppert.openprobatum.security.model.InMemoryUserDirectoryPersistence;
import com.svenruppert.openprobatum.security.model.PersistentUserDirectory;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.services.RegistrationResult;
import com.svenruppert.openprobatum.security.services.RegistrationService;
import com.svenruppert.openprobatum.security.services.UserProvisioningService;
import com.svenruppert.openprobatum.security.services.UserProvisioningService.ProvisionOutcome;
import com.svenruppert.openprobatum.security.services.UserProvisioningService.UserSpec;
import com.svenruppert.jsentinel.credential.password.bouncycastle.BouncyCastleHashingServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserProvisioningService — bulk user creation with roles (V00.80.00 P001)")
class UserProvisioningServiceTest {

  private PersistentUserDirectory directory;
  private UserProvisioningService service;

  @BeforeEach
  void setUp() {
    directory = new PersistentUserDirectory(
        new InMemoryUserDirectoryPersistence(), BouncyCastleHashingServices.modern());
    service = new UserProvisioningService(new RegistrationService(directory, 8));
  }

  private static UserSpec spec(String name, String password, AuthorizationRole role) {
    return new UserSpec(name, password, name, Set.of(role));
  }

  @Test
  @DisplayName("provisions each user with its role through the validated registration path")
  void provisionsUsersWithRoles() {
    List<ProvisionOutcome> outcomes = service.provision(List.of(
        spec("author", "author-pass", AuthorizationRole.AUTHOR),
        spec("reviewer", "reviewer-pass", AuthorizationRole.REVIEWER),
        spec("coach", "coach-pass", AuthorizationRole.COACH)));

    assertTrue(outcomes.stream().allMatch(ProvisionOutcome::created), "all three created");
    assertTrue(directory.usernameExists("author"));
    AppUser author = ((RegistrationResult.Success) outcomes.get(0).result()).user();
    assertTrue(author.roles().contains(AuthorizationRole.AUTHOR));
    assertFalse(author.roles().contains(AuthorizationRole.LEARNER), "only the requested role");
    AppUser coach = ((RegistrationResult.Success) outcomes.get(2).result()).user();
    assertTrue(coach.roles().contains(AuthorizationRole.COACH));
  }

  @Test
  @DisplayName("a duplicate username is reported as taken; the other rows still succeed")
  void duplicateUsernameReported() {
    List<ProvisionOutcome> outcomes = service.provision(List.of(
        spec("dup", "first-pass", AuthorizationRole.AUTHOR),
        spec("dup", "second-pass", AuthorizationRole.REVIEWER),
        spec("unique", "third-pass", AuthorizationRole.COACH)));

    assertTrue(outcomes.get(0).created());
    assertInstanceOf(RegistrationResult.UsernameTaken.class, outcomes.get(1).result());
    assertTrue(outcomes.get(2).created(), "an independent row still succeeds");
  }

  @Test
  @DisplayName("a weak password and an empty role set are rejected per row")
  void invalidRowsRejected() {
    List<ProvisionOutcome> outcomes = service.provision(List.of(
        spec("weak", "short", AuthorizationRole.AUTHOR),
        new UserSpec("noroles", "good-password", "No Roles", Set.of())));

    assertInstanceOf(RegistrationResult.WeakPassword.class, outcomes.get(0).result());
    assertInstanceOf(RegistrationResult.InvalidInput.class, outcomes.get(1).result());
    assertFalse(directory.usernameExists("weak"));
    assertFalse(directory.usernameExists("noroles"));
  }

  @Test
  @DisplayName("the 3-arg register still onboards a LEARNER (backwards compatible)")
  void learnerRegistrationUnchanged() {
    RegistrationResult result = new RegistrationService(directory, 8)
        .register("ada", "ada-password", "Ada");
    AppUser user = ((RegistrationResult.Success) result).user();
    assertEquals(Set.of(AuthorizationRole.LEARNER), Set.copyOf(user.roles()));
  }
}
