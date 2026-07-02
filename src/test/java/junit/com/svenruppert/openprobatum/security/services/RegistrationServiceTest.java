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
import com.svenruppert.openprobatum.security.model.Credentials;
import com.svenruppert.openprobatum.security.model.InMemoryUserDirectoryPersistence;
import com.svenruppert.openprobatum.security.model.PersistentUserDirectory;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.services.RegistrationResult;
import com.svenruppert.openprobatum.security.services.RegistrationService;
import com.svenruppert.jsentinel.credential.password.bouncycastle.BouncyCastleHashingServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RegistrationService — self-registration onboards a Learner (P002)")
class RegistrationServiceTest {

  private static final int MIN_LEN = 12;
  private static final String STRONG = "zR9q!sX5#kP2@dM7L"; // not on the local blocklist

  private PersistentUserDirectory directory;
  private RegistrationService service;

  @BeforeEach
  void setUp() {
    directory = new PersistentUserDirectory(
        new InMemoryUserDirectoryPersistence(), BouncyCastleHashingServices.modern());
    service = new RegistrationService(directory, MIN_LEN);
  }

  @Test
  @DisplayName("a valid registration creates a Learner with a working Argon2id hash")
  void registersLearner() {
    RegistrationResult result = service.register("alice", STRONG, "Alice");

    AppUser user = assertInstanceOf(RegistrationResult.Success.class, result).user();
    assertEquals("Alice", user.name());
    assertEquals(java.util.Set.of(AuthorizationRole.LEARNER), user.roles());

    // The password really verifies through the directory (real hashing, no mock).
    assertTrue(directory.findByCredentials(new Credentials("alice", STRONG)).isPresent());
    assertTrue(directory.usernameExists("alice"));
  }

  @Test
  @DisplayName("display name falls back to the username when blank")
  void displayNameDefaultsToUsername() {
    AppUser user = assertInstanceOf(RegistrationResult.Success.class,
        service.register("bob", STRONG, "  ")).user();
    assertEquals("bob", user.name());
  }

  @Test
  @DisplayName("a duplicate username is rejected and does not overwrite the first user")
  void duplicateUsernameRejected() {
    AppUser carol = assertInstanceOf(RegistrationResult.Success.class,
        service.register("carol", STRONG, "Carol")).user();
    RegistrationResult second = service.register("carol", STRONG + "X", "Impostor");

    assertInstanceOf(RegistrationResult.UsernameTaken.class, second);
    assertEquals("Carol", directory.findById(carol.id()).map(AppUser::name).orElse(null));
  }

  @Test
  @DisplayName("a duplicate DISPLAY name is rejected — the recipient key must be unique (HIGH-1)")
  void duplicateDisplayNameRejected() {
    service.register("alice", STRONG, "Alice Smith");
    // A different username but the same display name must be refused, otherwise
    // the two learners would match each other's credentials/wallet.
    RegistrationResult clash = service.register("alice2", STRONG, "Alice Smith");
    assertInstanceOf(RegistrationResult.NameTaken.class, clash);
    assertFalse(directory.usernameExists("alice2"), "nothing persisted for the clashing name");
  }

  @Test
  @DisplayName("a too-short password is rejected by the length policy")
  void shortPasswordRejected() {
    assertInstanceOf(RegistrationResult.WeakPassword.class,
        service.register("dave", "short", "Dave"));
    assertTrue(directory.all().findAny().isEmpty(), "nothing persisted");
  }

  @Test
  @DisplayName("a long but blocklisted password is rejected by the preflight")
  void blocklistedPasswordRejected() {
    // "password123" is 12 chars (passes length) but on the local blocklist.
    assertInstanceOf(RegistrationResult.WeakPassword.class,
        service.register("erin", "password123", "Erin"));
    assertTrue(directory.all().findAny().isEmpty(), "nothing persisted");
  }

  @Test
  @DisplayName("a blank username is rejected as invalid input")
  void blankUsernameRejected() {
    assertInstanceOf(RegistrationResult.InvalidInput.class,
        service.register("   ", STRONG, "X"));
  }

  @Test
  @DisplayName("ids come from the directory's single monotonic source (floor 1000, distinct)")
  void idsComeFromTheDirectorySequence() {
    AppUser first = assertInstanceOf(RegistrationResult.Success.class,
        service.register("frank", STRONG, "Frank")).user();
    AppUser second = assertInstanceOf(RegistrationResult.Success.class,
        service.register("grace", STRONG, "Grace")).user();

    assertEquals(1000L, first.id(), "empty directory starts at the bootstrap floor");
    assertTrue(second.id() > first.id(), "sequential registrations get distinct, increasing ids");
  }
}
