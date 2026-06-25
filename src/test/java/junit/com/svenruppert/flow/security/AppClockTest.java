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

package junit.com.svenruppert.flow.security;

import com.svenruppert.flow.security.AppClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("AppClock — injectable time source (R18)")
class AppClockTest {

  @AfterEach
  void reset() {
    AppClock.reset();
  }

  @Test
  @DisplayName("now() reflects an installed fixed clock")
  void nowReflectsSetClock() {
    Instant fixed = Instant.parse("2030-05-06T07:08:09Z");
    AppClock.setClock(Clock.fixed(fixed, ZoneOffset.UTC));
    assertEquals(fixed, AppClock.now());
  }

  @Test
  @DisplayName("reset() restores the system clock")
  void resetRestoresSystemClock() {
    AppClock.setClock(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    AppClock.reset();
    assertNotEquals(Instant.EPOCH, AppClock.now());
  }
}
