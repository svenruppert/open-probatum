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

package com.svenruppert.openprobatum.lab;

import java.util.Objects;

/**
 * Lazy holder for the application's {@link LabRepository}, mirroring the other
 * repository providers. The Initialization-on-Demand Holder idiom keeps
 * Eclipse-Store untouched at class-load until {@link #repository()} is first
 * reached and no test override was installed via
 * {@link #setRepository(LabRepository)}.
 *
 * @since V00.40.00
 */
public final class LabRepositoryProvider {

  private static volatile LabRepository override;

  private LabRepositoryProvider() {
  }

  private static final class Holder {
    static final LabRepository INSTANCE = new EclipseStoreLabRepository();
  }

  /** The active repository — a test override if installed, else the file-backed singleton. */
  public static LabRepository repository() {
    LabRepository swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRepository(LabRepository replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  /** Clears any test override. */
  public static void reset() {
    override = null;
  }
}
