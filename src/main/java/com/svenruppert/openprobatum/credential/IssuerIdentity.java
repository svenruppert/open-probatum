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

import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.Objects;
import java.util.UUID;

/**
 * The instance's issuer identity (concept §4.3) — the name shown as the issuer
 * on every credential and the base of its public validation link. Each
 * installation is its own independently branded issuer; the value comes from
 * per-instance config (system properties) with a sensible default.
 *
 * @param name               the issuer name printed on credentials / the page
 * @param validationBaseUrl  the base URL of the public validation page
 * @since V00.10.00
 */
public record IssuerIdentity(String name, String validationBaseUrl) {

  /** Config key for the issuer name. */
  public static final String NAME_PROPERTY = "app.issuer.name";
  /** Config key for the validation base URL. */
  public static final String VALIDATION_URL_PROPERTY = "app.validation.baseUrl";

  private static final String DEFAULT_NAME = "Open Probatum Academy";
  private static final String DEFAULT_VALIDATION_URL = "http://localhost:8080/validate";

  public IssuerIdentity {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(validationBaseUrl, "validationBaseUrl");
    if (name.isBlank()) {
      throw new IllegalArgumentException("issuer name must not be blank");
    }
  }

  /** Resolves the issuer identity from per-instance config, with defaults. */
  public static IssuerIdentity fromConfig() {
    String validationBaseUrl = System.getProperty(VALIDATION_URL_PROPERTY, DEFAULT_VALIDATION_URL);
    if (DEFAULT_VALIDATION_URL.equals(validationBaseUrl)) {
      // The base URL is baked into every QR code on printed certificates and
      // cannot be corrected after the fact — warn loudly so a production
      // instance that forgot to set app.validation.baseUrl is noticed before
      // it ships unverifiable localhost links.
      HasLogger.staticLogger().warn(
          "IssuerIdentity: app.validation.baseUrl is unset — issued credentials will "
              + "point at the localhost default {}. Set -D{}=https://… in production.",
          DEFAULT_VALIDATION_URL, VALIDATION_URL_PROPERTY);
    }
    return new IssuerIdentity(
        System.getProperty(NAME_PROPERTY, DEFAULT_NAME),
        validationBaseUrl);
  }

  /** The full public validation URL for a credential id. */
  public String validationUrl(UUID credentialId) {
    Objects.requireNonNull(credentialId, "credentialId");
    String base = validationBaseUrl.endsWith("/") ? validationBaseUrl : validationBaseUrl + "/";
    return base + credentialId;
  }
}
