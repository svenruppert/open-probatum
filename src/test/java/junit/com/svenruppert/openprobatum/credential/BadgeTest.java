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

package junit.com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.credential.Badge;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Badge — standalone representation referencing the record (P013)")
class BadgeTest {

  private final IssuerIdentity issuer =
      new IssuerIdentity("Open Probatum Academy", "https://verify.example.org/validate");
  private final Credential credential = Credential.issue("Vaadin Certified",
      CredentialType.COMPLETION_CERTIFICATE, "Alice", "Open Probatum Academy",
      Instant.parse("2026-01-01T00:00:00Z"), null);

  @Test
  @DisplayName("a badge references the same record + mirrors its fields")
  void badgeReferencesRecord() {
    Badge badge = Badge.of(credential, issuer);

    assertEquals(credential.id(), badge.credentialId(), "badge points at the credential record");
    assertEquals(credential.title(), badge.title());
    assertEquals(credential.recipientName(), badge.recipientName());
    assertEquals(credential.issuer(), badge.issuer());
  }

  @Test
  @DisplayName("the badge's validation URL targets the credential id")
  void badgeLinksToValidation() {
    Badge badge = Badge.of(credential, issuer);
    assertEquals(issuer.validationUrl(credential.id()), badge.validationUrl());
    assertTrue(badge.validationUrl().contains(credential.id().toString()),
        "the badge links to this credential's validation page");
  }
}
