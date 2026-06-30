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

import com.svenruppert.openprobatum.security.roles.AuthorizationRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Bulk user provisioning for the "create users" wizard (concept §5): turns a list
 * of operator-entered {@link UserSpec}s into real accounts through the validated,
 * role-aware {@link RegistrationService#register(String, String, String, Set)} path
 * — so provisioned users get the same length / uniqueness / breach-preflight checks
 * as a self-registration, and no passwords are ever shipped or hard-coded (the
 * operator types them). Each row's outcome is returned so the UI can report
 * created / skipped / failed per line. The wizard view is the thin layer over this.
 *
 * @since V00.80.00
 */
public final class UserProvisioningService {

  private final RegistrationService registration;

  public UserProvisioningService(RegistrationService registration) {
    this.registration = Objects.requireNonNull(registration, "registration");
  }

  public UserProvisioningService() {
    this(new RegistrationService());
  }

  /**
   * One operator-entered user to create.
   *
   * @param username    the login name (must be unique)
   * @param password    the operator-chosen password
   * @param displayName the display / credential-recipient name (defaults to username if blank)
   * @param roles       the roles to grant (must be non-empty)
   */
  public record UserSpec(String username, String password, String displayName,
                         Set<AuthorizationRole> roles) {
    public UserSpec {
      roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
  }

  /**
   * The result of provisioning one {@link UserSpec}.
   *
   * @param spec   the input row
   * @param result the validated registration outcome
   */
  public record ProvisionOutcome(UserSpec spec, RegistrationResult result) {
    /** @return whether the user was created. */
    public boolean created() {
      return result instanceof RegistrationResult.Success;
    }
  }

  /**
   * Provisions every spec in order, returning one {@link ProvisionOutcome} per input
   * row. A failing row (e.g. a duplicate username or a weak password) does not stop
   * the others — each row's verdict is independent.
   */
  public List<ProvisionOutcome> provision(List<UserSpec> specs) {
    Objects.requireNonNull(specs, "specs");
    List<ProvisionOutcome> outcomes = new ArrayList<>();
    for (UserSpec spec : specs) {
      RegistrationResult result = registration.register(
          spec.username(), spec.password(), spec.displayName(), spec.roles());
      outcomes.add(new ProvisionOutcome(spec, result));
    }
    return outcomes;
  }
}
