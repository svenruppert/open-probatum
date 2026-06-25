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

package com.svenruppert.openprobatum.access;

import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.Objects;

/**
 * Resolves whether a learner may enter an offering (concept §12). PUBLIC /
 * REGISTERED access is derived from the offering visibility; CODE / PREREQUISITE
 * / MANUAL access requires a stored {@link Entitlement} grant.
 *
 * <p>A prerequisite grant is created once the learner completes the prerequisite
 * offering (wired by the issuance/progress flow) or manually by an author; until
 * then a PREREQUISITE offering reports {@link AccessDecision#PREREQUISITE_REQUIRED}.
 *
 * @since V00.20.00
 */
public final class EntitlementService implements HasLogger {

  private final EntitlementRepository repository;

  public EntitlementService(EntitlementRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public EntitlementService() {
    this(EntitlementRepositoryProvider.repository());
  }

  /** Resolves the access decision for {@code user} (may be {@code null} = anonymous). */
  public AccessDecision canAccess(AppUser user, Offering offering) {
    Objects.requireNonNull(offering, "offering");
    return switch (offering.visibility()) {
      case PUBLIC -> AccessDecision.GRANTED;
      case REGISTERED -> user != null ? AccessDecision.GRANTED : AccessDecision.LOGIN_REQUIRED;
      case CODE -> hasGrant(user, offering)
          ? AccessDecision.GRANTED : AccessDecision.CODE_REQUIRED;
      case PREREQUISITE -> hasGrant(user, offering)
          ? AccessDecision.GRANTED : AccessDecision.PREREQUISITE_REQUIRED;
    };
  }

  /**
   * Redeems {@code code} against a CODE offering; on a match, stores a CODE grant
   * and returns {@code true}. Wrong code, non-CODE offering or anonymous user
   * return {@code false} and grant nothing.
   */
  public boolean redeemCode(AppUser user, Offering offering, String code) {
    Objects.requireNonNull(offering, "offering");
    if (user == null || code == null) {
      return false;
    }
    boolean matches = offering.accessCodeOpt().filter(c -> c.equals(code)).isPresent();
    if (!matches) {
      return false;
    }
    repository.grant(new Entitlement(user.id(), offering.id(), EntitlementReason.CODE));
    logger().info("Entitlement granted by code: user={}, offering={}", user.id(), offering.id());
    return true;
  }

  /** Grants access to {@code user} for {@code offering} for the given reason. */
  public void grant(AppUser user, Offering offering, EntitlementReason reason) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(offering, "offering");
    repository.grant(new Entitlement(user.id(), offering.id(), reason));
    logger().info("Entitlement granted ({}): user={}, offering={}", reason, user.id(), offering.id());
  }

  private boolean hasGrant(AppUser user, Offering offering) {
    return user != null && repository.hasGrant(user.id(), offering.id());
  }
}
