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

package com.svenruppert.flow.security;

import com.svenruppert.flow.security.model.AppUser;
import com.svenruppert.jsentinel.session.SessionRecord;

import java.util.List;

/**
 * The single row-level visibility rule that survives the no-tenant /
 * no-group decision (concept §3.6 / §5.4): a caller who holds
 * {@code admin:sessions} may see every session; everyone else sees only
 * the sessions whose subject id matches their own.
 *
 * <p>This is a deliberate, security-critical construction detail, not an
 * incidental one — the concept requires it to be enforced <em>per query</em>
 * ("je Abfrage durchzusetzen"). Every non-admin session read MUST route
 * through {@link #visibleTo} so a bug in one query cannot leak another
 * subject's sessions. Kept as a pure function so both branches are
 * unit-tested without a UI.
 */
public final class SessionAccess {

  private SessionAccess() {
  }

  /**
   * Filters {@code sessions} to those the {@code viewer} may see.
   *
   * @param sessions  the candidate session records
   * @param viewer    the current subject (may be {@code null} when anonymous)
   * @param canSeeAll {@code true} when the viewer holds {@code admin:sessions}
   * @return all sessions when {@code canSeeAll}; otherwise only the viewer's
   *     own; an empty list when the viewer is null / has no id and is not admin
   */
  public static List<SessionRecord> visibleTo(List<SessionRecord> sessions,
                                              AppUser viewer, boolean canSeeAll) {
    if (canSeeAll) {
      return sessions;
    }
    if (viewer == null || viewer.id() == null) {
      return List.of();
    }
    String ownId = String.valueOf(viewer.id());
    return sessions.stream()
        .filter(s -> ownId.equals(s.subjectId().value()))
        .toList();
  }
}
