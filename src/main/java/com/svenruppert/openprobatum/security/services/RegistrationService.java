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

package com.svenruppert.openprobatum.security.services;

import com.svenruppert.openprobatum.security.bootstrap.BootstrapWiring;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.model.UserDirectory;
import com.svenruppert.openprobatum.security.model.UserDirectoryProvider;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Public self-registration (concept §5.1): a visitor creates an account and is
 * onboarded as a {@link AuthorizationRole#LEARNER}. Reuses the same
 * {@link UserDirectory} + Argon2id hashing the rest of the platform uses, and
 * the same {@link PasswordPreflight} as the setup/change-password flows.
 *
 * <p>The view stays thin; all validation + persistence live here so they can be
 * tested against a real directory (no mocks).
 *
 * @since V00.20.00
 */
public final class RegistrationService implements HasLogger {

  private final UserDirectory directory;
  private final int minPasswordLength;

  public RegistrationService(UserDirectory directory, int minPasswordLength) {
    this.directory = Objects.requireNonNull(directory, "directory");
    this.minPasswordLength = minPasswordLength;
  }

  /** Production wiring — the shared directory + the server-side password policy. */
  public RegistrationService() {
    this(UserDirectoryProvider.directory(),
        BootstrapWiring.instance().policy().minLength().orElse(1));
  }

  /**
   * Validates and creates a Learner account. Order matters: cheap structural and
   * length checks first, the username-taken check next, then the (possibly
   * network-bound) compromised-password preflight last.
   */
  public RegistrationResult register(String username, String password, String displayName) {
    return register(username, password, displayName, EnumSet.of(AuthorizationRole.LEARNER));
  }

  /**
   * Validates and creates an account with the given {@code roles} — the
   * role-aware path used by the user-provisioning wizard (§5). Same validation as
   * the learner self-registration (length → username → display-name → breach
   * preflight); an empty role set is rejected as {@link RegistrationResult.InvalidInput}.
   *
   * @since V00.80.00
   */
  public synchronized RegistrationResult register(String username, String password,
                                                  String displayName,
                                                  Set<AuthorizationRole> roles) {
    if (username == null || username.isBlank()) {
      return new RegistrationResult.InvalidInput("username must not be blank");
    }
    if (roles == null || roles.isEmpty()) {
      return new RegistrationResult.InvalidInput("at least one role is required");
    }
    if (password == null || password.length() < minPasswordLength) {
      return new RegistrationResult.WeakPassword(
          "password must be at least " + minPasswordLength + " characters");
    }
    if (directory.usernameExists(username)) {
      return new RegistrationResult.UsernameTaken();
    }
    // The display name is the credential recipient key — it must be unique so one
    // user's wallet/credentials can never match another's (exit-review HIGH-1).
    String name = (displayName == null || displayName.isBlank()) ? username : displayName;
    if (directory.displayNameExists(name)) {
      return new RegistrationResult.NameTaken();
    }
    if (!PasswordPreflight.isAcceptable(password)) {
      return new RegistrationResult.WeakPassword(
          "password appears on a breach/blocklist");
    }
    // Single id source (directory high-water) — never reuses a deleted user's id.
    long id = directory.nextUserId();
    AppUser user = new AppUser(id, name, EnumSet.copyOf(roles));
    directory.addUser(username, password, user);
    logger().info("Registered new user: id={}, username={}, roles={}", id, username, roles);
    return new RegistrationResult.Success(user);
  }
}
