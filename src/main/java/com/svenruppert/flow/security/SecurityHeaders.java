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

import java.util.function.BiConsumer;

/**
 * Baseline HTTP security headers applied to every response by
 * {@link SecurityHeadersFilter}. Kept as a pure function over a
 * {@code (name, value)} setter so it is unit-testable without a servlet
 * container.
 *
 * <p>The Content-Security-Policy is deliberately conservative: it locks
 * down framing ({@code frame-ancestors 'none'}), plugins
 * ({@code object-src 'none'}) and base-URI hijacking
 * ({@code base-uri 'self'}) but does <strong>not</strong> restrict
 * {@code script-src}/{@code style-src}, because Vaadin Flow injects
 * inline bootstrap script/style and a strict policy there would break
 * the app without a nonce pipeline. Tightening {@code script-src} is a
 * later, separately-verified step.
 *
 * <p>HSTS is emitted only over HTTPS — sending it over plain HTTP is
 * meaningless and a browser ignores it.
 */
public final class SecurityHeaders {

  public static final String CSP = "Content-Security-Policy";
  public static final String CSP_VALUE =
      "frame-ancestors 'none'; object-src 'none'; base-uri 'self'";

  public static final String CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  public static final String CONTENT_TYPE_OPTIONS_VALUE = "nosniff";

  public static final String REFERRER_POLICY = "Referrer-Policy";
  public static final String REFERRER_POLICY_VALUE = "strict-origin-when-cross-origin";

  public static final String FRAME_OPTIONS = "X-Frame-Options";
  public static final String FRAME_OPTIONS_VALUE = "DENY";

  public static final String HSTS = "Strict-Transport-Security";
  public static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";

  private SecurityHeaders() {
  }

  /**
   * Applies the baseline headers via {@code setter}. HSTS is added only
   * when {@code secure} (the request arrived over HTTPS).
   */
  public static void apply(BiConsumer<String, String> setter, boolean secure) {
    setter.accept(CSP, CSP_VALUE);
    setter.accept(CONTENT_TYPE_OPTIONS, CONTENT_TYPE_OPTIONS_VALUE);
    setter.accept(REFERRER_POLICY, REFERRER_POLICY_VALUE);
    setter.accept(FRAME_OPTIONS, FRAME_OPTIONS_VALUE);
    if (secure) {
      setter.accept(HSTS, HSTS_VALUE);
    }
  }
}
