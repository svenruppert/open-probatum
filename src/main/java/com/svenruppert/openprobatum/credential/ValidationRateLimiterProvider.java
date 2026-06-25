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

import java.util.Objects;

/**
 * Lazy holder for the shared {@link ValidationRateLimiter}, so the in-memory
 * window state is one process-wide limiter and tests can install a small-limit
 * substitute via {@link #setLimiter(ValidationRateLimiter)}.
 *
 * @since V00.10.00
 */
public final class ValidationRateLimiterProvider {

  private static volatile ValidationRateLimiter override;

  private ValidationRateLimiterProvider() {
  }

  private static final class Holder {
    static final ValidationRateLimiter INSTANCE = ValidationRateLimiter.create();
  }

  /** The active limiter — a test override if installed, else the shared singleton. */
  public static ValidationRateLimiter limiter() {
    ValidationRateLimiter swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setLimiter(ValidationRateLimiter replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  /** Clears any test override. */
  public static void reset() {
    override = null;
  }
}
