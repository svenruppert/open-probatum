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

package junit.com.svenruppert.openprobatum.i18n;

import com.svenruppert.openprobatum.i18n.AppI18NProvider;
import com.vaadin.flow.i18n.DefaultI18NProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("i18n bundle loading — sanity checks")
class I18nBundleLoadingTest {

  @Test
  @DisplayName("German properties file is on the classpath and UTF-8 readable")
  void germanBundleOnClasspath() throws Exception {
    ClassLoader cl = getClass().getClassLoader();
    try (InputStream in = cl.getResourceAsStream(
        "vaadin-i18n/translations_de.properties")) {
      assertNotNull(in,
          "translations_de.properties must be on the classpath");
      Properties props = new Properties();
      props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
      assertEquals("Start", props.getProperty("nav.home"),
          "nav.home in German must be 'Start' — wrong = file is "
              + "either truncated, badly encoded or out of sync");
      assertEquals("Anmelden", props.getProperty("common.signIn"));
    }
  }

  @Test
  @DisplayName("AppI18NProvider with Locale.GERMAN returns German translation")
  void appProviderReturnsGerman() {
    AppI18NProvider provider = new AppI18NProvider();
    assertEquals("Start", provider.getTranslation("nav.home", Locale.GERMAN));
    assertEquals("Anmelden",
        provider.getTranslation("common.signIn", Locale.GERMAN));
  }

  @Test
  @DisplayName("AppI18NProvider with Locale.ENGLISH returns English translation")
  void appProviderReturnsEnglish() {
    AppI18NProvider provider = new AppI18NProvider();
    assertEquals("Home", provider.getTranslation("nav.home", Locale.ENGLISH),
        "AppI18NProvider must NOT fall back to the JVM default locale when "
            + "the requested locale has no explicit bundle file. The whole "
            + "point of this provider is to disable that fallback.");
    assertEquals("Sign in",
        provider.getTranslation("common.signIn", Locale.ENGLISH));
  }

  @Test
  @DisplayName("AppI18NProvider with unknown locale falls back to the default English bundle")
  void appProviderUnknownLocaleFallsBackToDefault() {
    AppI18NProvider provider = new AppI18NProvider();
    // French has no bundle → falls through to translations.properties (English)
    assertEquals("Home",
        provider.getTranslation("nav.home", Locale.FRENCH));
  }

  @Test
  @DisplayName("neither bundle defines a key twice — duplicates silently shadow each other (P009)")
  void noDuplicateKeys() throws Exception {
    for (String bundle : List.of(
        "vaadin-i18n/translations.properties",
        "vaadin-i18n/translations_de.properties")) {
      List<String> duplicates = duplicateKeys(bundle);
      assertEquals(List.of(), duplicates,
          bundle + " defines these keys more than once — java.util.Properties keeps "
              + "only the LAST value, so one view silently shows another's text "
              + "(e.g. the admin sessions view showing coaching text): " + duplicates);
    }
  }

  private List<String> duplicateKeys(String resource) throws Exception {
    ClassLoader cl = getClass().getClassLoader();
    java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
    try (InputStream in = cl.getResourceAsStream(resource);
         java.io.BufferedReader r = new java.io.BufferedReader(
             new java.io.InputStreamReader(java.util.Objects.requireNonNull(in), StandardCharsets.UTF_8))) {
      String line;
      while ((line = r.readLine()) != null) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
          continue;
        }
        int eq = trimmed.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        String key = trimmed.substring(0, eq).strip();
        counts.merge(key, 1, Integer::sum);
      }
    }
    return counts.entrySet().stream()
        .filter(e -> e.getValue() > 1)
        .map(java.util.Map.Entry::getKey)
        .toList();
  }

  @Test
  @DisplayName("Vaadin's DefaultI18NProvider has the JVM-fallback bug — diagnostic only")
  void defaultProviderHasJvmFallbackBug() {
    // This test DOCUMENTS the bug we're fixing. On a German JVM,
    // DefaultI18NProvider answers Locale.ENGLISH requests with German text
    // because ResourceBundle's standard fallback includes the JVM default.
    // We don't assert the exact behaviour — it depends on the developer
    // machine — but we keep the test as a reference for future readers.
    DefaultI18NProvider provider =
        new DefaultI18NProvider(List.of(Locale.ENGLISH, Locale.GERMAN));
    String englishLookup = provider.getTranslation("nav.home", Locale.ENGLISH);
    assertNotNull(englishLookup,
        "DefaultI18NProvider should at least return SOMETHING for any key");
  }
}
