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

import java.util.Objects;

/**
 * Lazy holder for the application's {@link EntitlementRepository} (IODH + test
 * seam), mirroring the catalog/credential providers.
 *
 * @since V00.20.00
 */
public final class EntitlementRepositoryProvider {

  private static volatile EntitlementRepository override;

  private EntitlementRepositoryProvider() {
  }

  private static final class Holder {
    static final EntitlementRepository INSTANCE = new EclipseStoreEntitlementRepository();
  }

  public static EntitlementRepository repository() {
    EntitlementRepository swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRepository(EntitlementRepository replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  public static void reset() {
    override = null;
  }
}
