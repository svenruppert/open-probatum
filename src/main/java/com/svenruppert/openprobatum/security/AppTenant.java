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

package com.svenruppert.openprobatum.security;

import com.svenruppert.jsentinel.authorization.api.tenant.TenantId;

/**
 * The single, fixed tenant for this instance. Multi-tenancy inherited from
 * the template is stilllegt (ADR TR-08): the platform runs one independently
 * branded instance per installation (concept §3.5 / §4), so every
 * tenant-scoped key is formed from this one constant — no call site can mint
 * a different value. Re-enabling tenancy is a deliberate future decision, not
 * a configuration change.
 */
public final class AppTenant {

  /** The fixed {@code TenantId} every instance-scoped lookup must use. */
  public static final TenantId ID = TenantId.DEFAULT;

  private AppTenant() {
  }
}
