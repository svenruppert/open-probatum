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

package com.svenruppert.openprobatum.assessment;

import java.util.Objects;

/**
 * Lazy holder for the application's {@link AttemptRepository} (IODH + test seam).
 *
 * @since V00.20.00
 */
public final class AttemptRepositoryProvider {

  private static volatile AttemptRepository override;

  private AttemptRepositoryProvider() {
  }

  private static final class Holder {
    static final AttemptRepository INSTANCE = new EclipseStoreAttemptRepository();
  }

  public static AttemptRepository repository() {
    AttemptRepository swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRepository(AttemptRepository replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  public static void reset() {
    override = null;
  }
}
