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

import com.svenruppert.openprobatum.security.AppClock;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One entry in the app-side credential audit trail (concept §17.3). jSentinel's
 * audit service is closed to application-defined event types, so the credential
 * lifecycle is recorded in this self-contained, persisted log instead — a plain
 * domain record following the repository pattern. Every issuance and governance
 * action appends exactly one event, so the trail is a complete, ordered history
 * of what happened to a credential, by whom and when.
 *
 * @param id           the event id
 * @param timestamp    when the action happened
 * @param credentialId the credential the action applied to
 * @param action       the kind of action
 * @param actor        who performed it (a role label or subject name)
 * @param detail       optional free-text context (never null; may be empty)
 * @since V00.30.00
 */
public record CredentialEvent(UUID id, Instant timestamp, UUID credentialId,
                              Action action, String actor, String detail) {

  /** The credential lifecycle actions recorded in the trail (§17.3). */
  public enum Action {
    ISSUED, REVOKED, SUSPENDED, REINSTATED, SUPERSEDED, REISSUED
  }

  public CredentialEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(credentialId, "credentialId");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(detail, "detail");
  }

  /** A fresh event stamped from {@link AppClock}, with a random id. */
  public static CredentialEvent of(UUID credentialId, Action action, String actor, String detail) {
    return new CredentialEvent(UUID.randomUUID(), AppClock.now(), credentialId,
        action, actor, detail == null ? "" : detail);
  }
}
