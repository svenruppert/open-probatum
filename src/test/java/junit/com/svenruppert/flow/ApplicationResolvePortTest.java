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

package junit.com.svenruppert.flow;

import com.svenruppert.flow.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Application.resolvePort — valid parse + safe fallback (R26)")
class ApplicationResolvePortTest {

  private String previous;

  @AfterEach
  void restore() {
    if (previous == null) {
      System.clearProperty("app.port");
    } else {
      System.setProperty("app.port", previous);
    }
  }

  private static int resolvePort(int fallback) throws Exception {
    Method m = Application.class.getDeclaredMethod("resolvePort", int.class);
    m.setAccessible(true);
    return (int) m.invoke(null, fallback);
  }

  @Test
  @DisplayName("a valid numeric app.port is parsed")
  void validPortParsed() throws Exception {
    previous = System.getProperty("app.port");
    System.setProperty("app.port", "9090");
    assertEquals(9090, resolvePort(8080));
  }

  @Test
  @DisplayName("a malformed app.port falls back (and warns) instead of throwing")
  void malformedPortFallsBack() throws Exception {
    previous = System.getProperty("app.port");
    System.setProperty("app.port", "80O"); // letter O — not a number
    assertEquals(8080, resolvePort(8080));
  }
}
