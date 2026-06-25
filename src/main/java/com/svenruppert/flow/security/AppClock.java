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

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Single time source for audit and session timestamps. Replaces the
 * hand-repeated {@code Instant.now(Clock.systemUTC())} scattered across
 * the auth / user-directory / session code, so event ordering can be
 * pinned deterministically in tests via {@link #setClock(Clock)}.
 *
 * <p>Defaults to {@link Clock#systemUTC()}. The {@code setClock} /
 * {@link #reset()} seam mirrors the project's other swappable statics
 * ({@code UserDirectoryProvider}, {@code JSentinelServiceResolver}).
 */
public final class AppClock {

  private static volatile Clock clock = Clock.systemUTC();

  private AppClock() {
  }

  /** The current instant from the active clock. */
  public static Instant now() {
    return Instant.now(clock);
  }

  /** The active clock. */
  public static Clock clock() {
    return clock;
  }

  /** Test seam: install a fixed/offset clock. */
  public static void setClock(Clock replacement) {
    clock = Objects.requireNonNull(replacement, "clock");
  }

  /** Restore the production {@link Clock#systemUTC()}. */
  public static void reset() {
    clock = Clock.systemUTC();
  }
}
