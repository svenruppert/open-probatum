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

package com.svenruppert.openprobatum.security.model;

import com.svenruppert.openprobatum.security.roles.AuthorizationRole;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Application user store. Decoupled from the concrete implementation
 * so a future backend swap (DB, LDAP, IAM) does not touch consumers.
 *
 * <p>{@link com.svenruppert.openprobatum.security.bootstrap.AdministratorAccountStoreImpl}
 * adapts the {@link com.svenruppert.jsentinel.bootstrap.AdministratorAccountStore}
 * SPI required by the first-admin bootstrap pipeline onto this
 * directory — {@link #hasAnyAdministrator()} and
 * {@link #registerWithHashedPassword(String, String, AppUser)} are
 * the two methods the adapter consumes.
 */
public interface UserDirectory {

  /** The id handed to the very first user (the bootstrap administrator). */
  long FIRST_USER_ID = 1000L;

  Optional<AppUser> findByCredentials(Credentials credentials);

  /**
   * Allocates the next user id. The default implementation derives it from
   * the current maximum (floor {@link #FIRST_USER_ID}). Durable
   * implementations SHOULD override this with a monotonic, non-reusing
   * source: a {@code max+1} scheme hands the id of a deleted user to the
   * next account, so historic authorship / audit records suddenly point at
   * the wrong present-day user (e.g. a false segregation-of-duties block).
   */
  default long nextUserId() {
    return all().mapToLong(AppUser::id).max().orElse(FIRST_USER_ID - 1) + 1;
  }

  default boolean checkCredentials(Credentials credentials) {
    return findByCredentials(credentials).isPresent();
  }

  Optional<AppUser> findById(Long id);

  Stream<AppUser> all();

  /**
   * @return {@code true} when at least one user with the
   *         {@link AuthorizationRole#PLATFORM_ADMIN} role exists. Used by the
   *         bootstrap pipeline to decide whether the system is
   *         uninitialised.
   */
  boolean hasAnyAdministrator();

  /**
   * @return {@code true} when the user {@code id} holds
   *         {@link AuthorizationRole#PLATFORM_ADMIN} and <em>no other</em> user
   *         does — i.e. removing that role or deleting the account would leave
   *         the instance with zero administrators. Used to refuse the mutation
   *         that would otherwise force the whole instance back into the
   *         (token-gated, unreachable) bootstrap flow.
   */
  default boolean isLastAdministrator(Long id) {
    if (id == null) {
      return false;
    }
    AppUser target = findById(id).orElse(null);
    if (target == null || !target.roles().contains(AuthorizationRole.PLATFORM_ADMIN)) {
      return false;
    }
    return all().noneMatch(u -> !u.id().equals(id)
        && u.roles().contains(AuthorizationRole.PLATFORM_ADMIN));
  }

  /** @return {@code true} if a user with this username already exists. */
  boolean usernameExists(String username);

  /**
   * @return {@code true} if a user with this display name already exists. The
   *         display name is the credential recipient key, so it must be unique to
   *         keep one learner's wallet / credentials from matching another's.
   */
  boolean displayNameExists(String displayName);

  void addUser(String username, String plaintextPassword, AppUser user);

  /**
   * Stores a user with a pre-hashed password — bypasses the directory's
   * own hashing. Used by the bootstrap flow, which receives the
   * already-hashed password from
   * {@link com.svenruppert.jsentinel.bootstrap.InitialAdminBootstrapService}.
   *
   * @throws IllegalStateException if {@code username} already exists
   */
  void registerWithHashedPassword(String username, String passwordHash, AppUser user);

  void deleteUser(Long id);

  void assignRole(Long id, AuthorizationRole role);

  void revokeRole(Long id, AuthorizationRole role);
}
