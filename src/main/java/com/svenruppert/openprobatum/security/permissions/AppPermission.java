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

package com.svenruppert.openprobatum.security.permissions;

import com.svenruppert.jsentinel.authorization.api.permissions.PermissionName;

/**
 * Permission catalog. Each entry binds a string id (used in
 * {@code @RequiresPermission("...")} annotations) to a typed
 * {@link PermissionName} the AuthorizationService produces from the
 * subject's roles.
 *
 * <p>Add new permissions here, map them to roles in the
 * {@code AppAuthorizationService} ROLE_PERMISSIONS table.
 */
public enum AppPermission {
  APP_VIEW("app:view"),
  AUDIT_READ("audit:read"),
  ADMIN_SESSIONS("admin:sessions"),
  ADMIN_ROLES("admin:roles"),
  /** Author: create/edit catalog content (offerings, paths, modules, resources, questions). */
  AUTHOR_CONTENT("author:content"),
  /** Reviewer: review + approve authored content before publication. */
  AUTHOR_REVIEW("author:review"),
  /** Assessor: verify or reject learners' practical lab submissions (§16.3). */
  LAB_ASSESS("lab:assess"),
  /** Instructor: run workshops and record learner attendance (§7.x). */
  WORKSHOP_RUN("workshop:run"),
  /** Coach: open coaching slots and complete 1:1 sessions (§7.x). */
  COACHING_PROVIDE("coaching:provide"),
  /** Credential Manager: govern issued credentials (e.g. revoke). */
  CREDENTIAL_MANAGE("credential:manage");

  private final PermissionName permissionName;

  AppPermission(String value) {
    this.permissionName = new PermissionName(value);
  }

  public PermissionName permissionName() {
    return permissionName;
  }
}
