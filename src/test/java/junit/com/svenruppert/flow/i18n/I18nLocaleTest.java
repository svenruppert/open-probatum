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

package junit.com.svenruppert.flow.i18n;

import com.svenruppert.flow.i18n.I18n;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("I18n.currentLocale — does not leak the JVM default locale (R16)")
class I18nLocaleTest {

  @Test
  @DisplayName("with no UI/request, currentLocale() returns ENGLISH even on a German JVM")
  void currentLocaleIgnoresJvmDefault() throws Exception {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.GERMAN);
      Method m = I18n.class.getDeclaredMethod("currentLocale");
      m.setAccessible(true);
      assertEquals(Locale.ENGLISH, m.invoke(null),
          "currentLocale() must not fall back to the JVM default locale");
    } finally {
      Locale.setDefault(previous);
    }
  }
}
