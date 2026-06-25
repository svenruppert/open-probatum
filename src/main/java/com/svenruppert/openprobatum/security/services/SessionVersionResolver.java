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

import com.svenruppert.openprobatum.security.AppTenant;

import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.jsentinel.authorization.api.JSentinelServiceResolver;
import com.svenruppert.jsentinel.logout.SubjectId;
import com.svenruppert.jsentinel.session.JSentinelVersion;
import com.svenruppert.jsentinel.session.JSentinelVersionKey;
import com.svenruppert.jsentinel.session.JSentinelVersionStore;

/**
 * Resolves the {@code JSentinelVersion} baseline to stamp on a session
 * record at login time. The counterpart to {@link VersionBumper}: where
 * the bumper <em>increments</em> the per-subject version on a role change,
 * this resolver <em>reads</em> the current value so a session opened
 * <em>after</em> a prior bump carries the right baseline.
 *
 * <p>Without this, {@code AppLoginView} stamped every fresh session with
 * {@link JSentinelVersion#INITIAL}: a subject already bumped to version
 * {@code N} (by an earlier role revoke that persisted across the login)
 * would then be seen as drifted by
 * {@code JSentinelVersionEnforcerListener} and bounced straight back to
 * the login view — phantom drift. Reading the live value here closes that.
 *
 * <p>The key is built identically to {@link VersionBumper}
 * ({@code (AppTenant.ID, SubjectId.of(user.id()))}) so read and write
 * address the same counter. Falls back to {@link JSentinelVersion#INITIAL}
 * when the {@link JSentinelVersionStore} SPI is absent (hardening skill
 * reverted) or the user/id is null, so callers can stay unconditional.
 */
public final class SessionVersionResolver {

  private SessionVersionResolver() {
  }

  /**
   * Returns the subject's current drift version, or
   * {@link JSentinelVersion#INITIAL} when no store is registered or the
   * user/id is null.
   */
  public static JSentinelVersion current(AppUser user) {
    if (user == null || user.id() == null) {
      return JSentinelVersion.INITIAL;
    }
    JSentinelVersionKey key = new JSentinelVersionKey(
        AppTenant.ID, SubjectId.of(user.id().toString()));
    return JSentinelServiceResolver.findJSentinelVersionStore()
        .map(store -> store.current(key))
        .orElse(JSentinelVersion.INITIAL);
  }
}
