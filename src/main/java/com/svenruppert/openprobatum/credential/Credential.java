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

package com.svenruppert.openprobatum.credential;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The credential record — the sole source of truth for a proof (concept
 * §3.2, §10.3). A PDF or badge is only a rendering of this record; the
 * validation page reads this record, never a document.
 *
 * <p>The identifier is a random, non-enumerable UUIDv4 (concept §10.4), so
 * the public validation page cannot be harvested by counting upwards. The
 * stored {@link CredentialStatus} is level 1 of the three-layer status
 * model; {@link #effectiveStatusAt(Instant)} computes level 2 (it derives
 * {@link EffectiveStatus#EXPIRED} from the optional expiry, which is never
 * stored — concept §10.5).
 *
 * @param id           random non-enumerable identifier (UUIDv4)
 * @param title        human-readable credential title
 * @param type         the kind of proof
 * @param recipientName the recipient shown on the validation page
 * @param issuer       the instance's issuer identity (concept §4.3)
 * @param issuedAt     the business issue date
 * @param expiresAt    optional expiry; {@code null} means it never expires
 * @param status       stored lifecycle status (level 1)
 * @param supersededBy the replacing credential's id when {@code SUPERSEDED},
 *                     otherwise {@code null}
 * @since V00.10.00
 */
public record Credential(
    UUID id,
    String title,
    CredentialType type,
    String recipientName,
    String issuer,
    Instant issuedAt,
    Instant expiresAt,
    CredentialStatus status,
    UUID supersededBy) {

  public Credential {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(recipientName, "recipientName");
    Objects.requireNonNull(issuer, "issuer");
    Objects.requireNonNull(issuedAt, "issuedAt");
    Objects.requireNonNull(status, "status");
  }

  /**
   * Issues a new {@code VALID} credential with a random, non-enumerable
   * UUIDv4 identifier (concept §10.4).
   *
   * @param expiresAt optional expiry; {@code null} for no expiry
   */
  public static Credential issue(String title, CredentialType type,
                                 String recipientName, String issuer,
                                 Instant issuedAt, Instant expiresAt) {
    return new Credential(UUID.randomUUID(), title, type, recipientName,
        issuer, issuedAt, expiresAt, CredentialStatus.VALID, null);
  }

  /** The optional expiry date. */
  public Optional<Instant> expiry() {
    return Optional.ofNullable(expiresAt);
  }

  /** The optional replacing-credential id (present only when superseded). */
  public Optional<UUID> superseder() {
    return Optional.ofNullable(supersededBy);
  }

  /**
   * The computed effective status (level 2). {@code EXPIRED} is derived from
   * the expiry vs. {@code now} and only when the stored status is
   * {@code VALID}; {@code REVOKED}/{@code SUSPENDED}/{@code SUPERSEDED} come
   * straight from the stored status and outrank an expiry.
   */
  public EffectiveStatus effectiveStatusAt(Instant now) {
    Objects.requireNonNull(now, "now");
    return switch (status) {
      case REVOKED -> EffectiveStatus.REVOKED;
      case SUSPENDED -> EffectiveStatus.SUSPENDED;
      case SUPERSEDED -> EffectiveStatus.SUPERSEDED;
      case VALID -> (expiresAt != null && now.isAfter(expiresAt))
          ? EffectiveStatus.EXPIRED
          : EffectiveStatus.VALID;
    };
  }

  /** Returns a copy with a new stored status (e.g. revoke / suspend). */
  public Credential withStatus(CredentialStatus newStatus) {
    return new Credential(id, title, type, recipientName, issuer,
        issuedAt, expiresAt, Objects.requireNonNull(newStatus, "newStatus"),
        supersededBy);
  }

  /**
   * Returns a copy marked {@code SUPERSEDED}, pointing at the replacing
   * credential's id. The old record is never deleted (concept §10.9).
   */
  public Credential supersededByCredential(UUID replacement) {
    return new Credential(id, title, type, recipientName, issuer,
        issuedAt, expiresAt, CredentialStatus.SUPERSEDED,
        Objects.requireNonNull(replacement, "replacement"));
  }
}
