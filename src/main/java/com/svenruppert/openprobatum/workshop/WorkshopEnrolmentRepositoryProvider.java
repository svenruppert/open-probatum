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

package com.svenruppert.openprobatum.workshop;

import java.util.Objects;

/**
 * Lazy holder for the application's {@link WorkshopEnrolmentRepository}, mirroring
 * the other repository providers (Initialization-on-Demand Holder idiom; test
 * override via {@link #setRepository(WorkshopEnrolmentRepository)}).
 *
 * @since V00.50.00
 */
public final class WorkshopEnrolmentRepositoryProvider {

  private static volatile WorkshopEnrolmentRepository override;

  private WorkshopEnrolmentRepositoryProvider() {
  }

  private static final class Holder {
    static final WorkshopEnrolmentRepository INSTANCE = new EclipseStoreWorkshopEnrolmentRepository();
  }

  /** The active repository — a test override if installed, else the file-backed singleton. */
  public static WorkshopEnrolmentRepository repository() {
    WorkshopEnrolmentRepository swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRepository(WorkshopEnrolmentRepository replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  /** Clears any test override. */
  public static void reset() {
    override = null;
  }
}
