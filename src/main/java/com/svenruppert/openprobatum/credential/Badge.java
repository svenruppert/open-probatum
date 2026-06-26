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
import java.util.UUID;

/**
 * A standalone, shareable representation of a credential (concept §10.8). A badge
 * is <em>not</em> the source of truth — it merely points at the credential record
 * by {@link #credentialId} and links to its public validation page, exactly like
 * the PDF certificate. Badge and certificate therefore always reference the same
 * record.
 *
 * @param credentialId  the id of the credential this badge represents
 * @param title         the credential title
 * @param recipientName the holder's name
 * @param issuer        the issuing instance
 * @param validationUrl the public validation link for {@link #credentialId}
 * @since V00.20.00
 */
public record Badge(UUID credentialId, String title, String recipientName, String issuer,
                    String validationUrl) {

  public Badge {
    Objects.requireNonNull(credentialId, "credentialId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(recipientName, "recipientName");
    Objects.requireNonNull(issuer, "issuer");
    Objects.requireNonNull(validationUrl, "validationUrl");
  }

  /** Derives the badge for {@code credential}, linking to its validation page. */
  public static Badge of(Credential credential, IssuerIdentity issuer) {
    Objects.requireNonNull(credential, "credential");
    Objects.requireNonNull(issuer, "issuer");
    return new Badge(credential.id(), credential.title(), credential.recipientName(),
        credential.issuer(), issuer.validationUrl(credential.id()));
  }
}
