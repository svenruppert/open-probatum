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

package com.svenruppert.openprobatum.security.roles;

/**
 * Role catalog for the application (concept §5). A subject holds a set of these.
 *
 * <ul>
 *   <li>{@link #LEARNER} — registers, browses the catalog, learns, practises and
 *       owns a credential wallet (§5.1).</li>
 *   <li>{@link #AUTHOR} — creates offerings, paths, modules, resources and
 *       questions (§5.2).</li>
 *   <li>{@link #REVIEWER} — reviews + approves authored content before it is
 *       published, and assesses labs + runs workshops (§5.3).</li>
 *   <li>{@link #COACH} — authors 1:1 coaching offers and delivers them
 *       (opens slots, completes sessions); the coach is the offer's author (§5.4).</li>
 *   <li>{@link #CREDENTIAL_MANAGER} — governs issued credentials, e.g. revoke
 *       (§5.5).</li>
 *   <li>{@link #PLATFORM_ADMIN} — operates the instance; holds every permission
 *       (§5.6).</li>
 *   <li>{@link #VERIFIER} — an authenticated verifier; the public validation page
 *       itself needs no account (§5.7).</li>
 * </ul>
 *
 * <p>The role-to-permission table lives in {@code AppAuthorizationService}. Add a
 * new role here, map it to permissions there, and reference it in
 * {@code @VisibleFor(...)} on views.
 */
public enum AuthorizationRole {
  LEARNER,
  AUTHOR,
  REVIEWER,
  COACH,
  CREDENTIAL_MANAGER,
  PLATFORM_ADMIN,
  VERIFIER
}
